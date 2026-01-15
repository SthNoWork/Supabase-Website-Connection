// ============================================================================
// Database Records Manager - Browser-compatible (GitHub Pages ready!)
// Uses Supabase REST API directly - no server required
// ============================================================================

import Database from './connection/database.js';

// Initialize database connection
const db = new Database();

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
        const rows = await db.selectAll();
        renderTable(rows, recordsTable);
    } catch (err) {
        recordsTable.innerHTML = `<p class="error">Error: ${err.message}</p>`;
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
        await db.insert(data);
        showStatus('Record inserted successfully!', 'success');
        loadRecords();
    } catch (err) {
        showStatus(`Insert failed: ${err.message}`, 'error');
    }
});

// ─── Filter Records ──────────────────────────────────────────────────────────
filterForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const filters = {};
    const rows = filterFields.querySelectorAll('.field-row');
    rows.forEach(row => {
        const col = row.querySelector('.filter-col').value.trim();
        const val = row.querySelector('.filter-val').value.trim();
        if (col && val) filters[col] = val;
    });

    if (Object.keys(filters).length === 0) {
        filteredTable.innerHTML = '<p class="error">Please add at least one filter</p>';
        return;
    }

    filteredTable.innerHTML = '<p>Loading...</p>';
    try {
        const rows = await db.select(filters);
        renderTable(rows, filteredTable);
    } catch (err) {
        filteredTable.innerHTML = `<p class="error">Error: ${err.message}</p>`;
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
