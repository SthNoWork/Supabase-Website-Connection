// ============================================================================
// Database Records Manager - Client-side JavaScript
// ============================================================================

const API_BASE = '/api/records';

// ─── DOM Elements ────────────────────────────────────────────────────────────
const recordForm = document.getElementById('recordForm');
const dynamicFields = document.getElementById('dynamicFields');
const addFieldBtn = document.getElementById('addFieldBtn');
const refreshBtn = document.getElementById('refreshBtn');
const statusDiv = document.getElementById('status');
const recordsTable = document.getElementById('recordsTable');

const filterForm = document.getElementById('filterForm');
const filterFields = document.getElementById('filterFields');
const addFilterBtn = document.getElementById('addFilterBtn');
const clearFilterBtn = document.getElementById('clearFilterBtn');
const filteredTable = document.getElementById('filteredTable');

// ─── Initialize ──────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    addField();       // Add one empty field for insert form
    addFilterField(); // Add one empty filter field
    loadRecords();    // Load all records on page load
});

// ─── Dynamic Field Management ────────────────────────────────────────────────
function addField() {
    const fieldRow = document.createElement('div');
    fieldRow.className = 'field-row';
    fieldRow.innerHTML = `
        <input type="text" placeholder="Column name" class="col-name" required>
        <input type="text" placeholder="Value" class="col-value">
        <button type="button" class="remove-btn" onclick="this.parentElement.remove()">✕</button>
    `;
    dynamicFields.appendChild(fieldRow);
}

function addFilterField() {
    const fieldRow = document.createElement('div');
    fieldRow.className = 'field-row';
    fieldRow.innerHTML = `
        <input type="text" placeholder="Column name" class="filter-col" required>
        <input type="text" placeholder="Value" class="filter-val">
        <button type="button" class="remove-btn" onclick="this.parentElement.remove()">✕</button>
    `;
    filterFields.appendChild(fieldRow);
}

addFieldBtn.addEventListener('click', addField);
addFilterBtn.addEventListener('click', addFilterField);

// ─── Load All Records ────────────────────────────────────────────────────────
async function loadRecords() {
    recordsTable.innerHTML = '<p>Loading...</p>';
    try {
        const res = await fetch(API_BASE);
        const json = await res.json();
        if (json.error) {
            recordsTable.innerHTML = `<p class="error">Error: ${json.error}</p>`;
            return;
        }
        renderTable(json.data, recordsTable);
    } catch (err) {
        recordsTable.innerHTML = `<p class="error">Fetch error: ${err.message}</p>`;
    }
}

refreshBtn.addEventListener('click', loadRecords);

// ─── Insert Record ───────────────────────────────────────────────────────────
recordForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const data = {};
    const rows = dynamicFields.querySelectorAll('.field-row');
    rows.forEach(row => {
        const col = row.querySelector('.col-name').value.trim();
        const val = row.querySelector('.col-value').value;
        if (col) data[col] = val || null;
    });

    if (Object.keys(data).length === 0) {
        showStatus('Please add at least one field', 'error');
        return;
    }

    try {
        const res = await fetch(API_BASE, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        const json = await res.json();
        if (json.error) {
            showStatus(`Insert failed: ${json.error}`, 'error');
        } else {
            showStatus('Record inserted successfully!', 'success');
            loadRecords();
        }
    } catch (err) {
        showStatus(`Fetch error: ${err.message}`, 'error');
    }
});

// ─── Filter Records ──────────────────────────────────────────────────────────
filterForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const params = new URLSearchParams();
    const rows = filterFields.querySelectorAll('.field-row');
    rows.forEach(row => {
        const col = row.querySelector('.filter-col').value.trim();
        const val = row.querySelector('.filter-val').value.trim();
        if (col && val) params.append(col, val);
    });

    if ([...params].length === 0) {
        filteredTable.innerHTML = '<p class="error">Please add at least one filter</p>';
        return;
    }

    filteredTable.innerHTML = '<p>Loading...</p>';
    try {
        const res = await fetch(`${API_BASE}/filter?${params.toString()}`);
        const json = await res.json();
        if (json.error) {
            filteredTable.innerHTML = `<p class="error">Error: ${json.error}</p>`;
            return;
        }
        renderTable(json.data, filteredTable);
    } catch (err) {
        filteredTable.innerHTML = `<p class="error">Fetch error: ${err.message}</p>`;
    }
});

clearFilterBtn.addEventListener('click', () => {
    filterFields.innerHTML = '';
    addFilterField();
    filteredTable.innerHTML = '';
});

// ─── Helpers ─────────────────────────────────────────────────────────────────
function renderTable(rows, container) {
    if (!rows || rows.length === 0) {
        container.innerHTML = '<p>No records found.</p>';
        return;
    }

    const cols = Object.keys(rows[0]);
    let html = '<table><thead><tr>';
    cols.forEach(c => html += `<th>${escapeHtml(c)}</th>`);
    html += '</tr></thead><tbody>';
    rows.forEach(row => {
        html += '<tr>';
        cols.forEach(c => {
            const v = row[c];
            html += `<td>${v === null ? '<em>null</em>' : escapeHtml(String(v))}</td>`;
        });
        html += '</tr>';
    });
    html += '</tbody></table>';
    container.innerHTML = html;
}

function escapeHtml(str) {
    return str.replace(/[&<>"']/g, m => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    })[m]);
}

function showStatus(msg, type) {
    statusDiv.textContent = msg;
    statusDiv.className = type;
    setTimeout(() => { statusDiv.textContent = ''; statusDiv.className = ''; }, 4000);
}
