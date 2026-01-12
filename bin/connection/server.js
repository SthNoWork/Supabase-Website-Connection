/**
 * Express HTTP Server - Uses Database class for all DB operations
 * Run: npm start
 */

import express from 'express';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';
import Database from './database.js';

const __dirname = dirname(fileURLToPath(import.meta.url));
const app = express();
const PORT = process.env.PORT || 3000;

// Shared database instance
const db = new Database();

// Middleware
app.use(express.json());
app.use(express.static(join(__dirname, '..'))); // Serve from src/

// Connect to DB on startup
await db.connect();

// ─── API Endpoints ───────────────────────────────────────────────────────────

// GET all records
app.get('/api/records', async (req, res) => {
    try {
        const rows = await db.selectAll();
        res.json({ data: rows, error: null });
    } catch (err) {
        res.json({ data: null, error: err.message });
    }
});

// GET filtered records
app.get('/api/records/filter', async (req, res) => {
    try {
        if (Object.keys(req.query).length === 0) {
            return res.json({ data: null, error: 'At least one filter required' });
        }
        const rows = await db.select(req.query);
        res.json({ data: rows, error: null });
    } catch (err) {
        res.json({ data: null, error: err.message });
    }
});

// INSERT a record
app.post('/api/records', async (req, res) => {
    try {
        const row = await db.insert(req.body);
        res.json({ data: row, error: null });
    } catch (err) {
        res.json({ data: null, error: err.message });
    }
});

// UPDATE records
app.put('/api/records', async (req, res) => {
    try {
        const { data, filters } = req.body;
        const result = await db.update(data, filters);
        res.json({ data: result.rows, rowsAffected: result.rowsAffected, error: null });
    } catch (err) {
        res.json({ data: null, error: err.message });
    }
});

// DELETE records
app.delete('/api/records', async (req, res) => {
    try {
        const { filters } = req.body;
        const result = await db.delete(filters);
        res.json({ data: result.rows, rowsAffected: result.rowsAffected, error: null });
    } catch (err) {
        res.json({ data: null, error: err.message });
    }
});

// ─── Start Server ────────────────────────────────────────────────────────────
app.listen(PORT, () => {
    console.log(`🚀 Server running at http://localhost:${PORT}`);
});
