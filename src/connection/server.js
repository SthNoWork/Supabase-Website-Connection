import express from 'express';
import pg from 'pg';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';
import { readFileSync } from 'fs';
import { DB_CONFIG } from './config.js';

const __dirname = dirname(fileURLToPath(import.meta.url));
const app = express();
const PORT = process.env.PORT || 3000;

// Read SSL certificate
const sslConfig = DB_CONFIG.ssl.ca 
    ? { rejectUnauthorized: true, ca: readFileSync(join(__dirname, DB_CONFIG.ssl.ca)).toString() }
    : DB_CONFIG.ssl;

// PostgreSQL connection pool
const pool = new pg.Pool({
    host: DB_CONFIG.host,
    port: DB_CONFIG.port,
    database: DB_CONFIG.database,
    user: DB_CONFIG.username,
    password: DB_CONFIG.password,
    ssl: sslConfig
});

// Middleware
app.use(express.json());
app.use(express.static(join(__dirname, '..'))); // Serve from src/

// Test connection on startup
pool.query('SELECT NOW()')
    .then(() => console.log('✅ Connected to PostgreSQL'))
    .catch(err => console.error('❌ Database connection error:', err.message));

// GET all records
app.get('/api/records', async (req, res) => {
    try {
        const result = await pool.query(
            `SELECT record_index, patient_name, patient_dob, doctor_name, nurse_name, check_in_date 
             FROM hospital_records 
             ORDER BY record_index DESC`
        );
        res.json({ data: result.rows, error: null });
    } catch (err) {
        res.json({ data: null, error: err.message });
    }
});

// INSERT a record
app.post('/api/records', async (req, res) => {
    try {
        const { patient_id_hash, patient_name, patient_dob, doctor_name, nurse_name } = req.body;
        
        if (!patient_id_hash || patient_id_hash.length !== 64) {
            return res.json({ data: null, error: 'Patient ID Hash must be exactly 64 characters' });
        }

        const result = await pool.query(
            `INSERT INTO hospital_records (patient_id_hash, patient_name, patient_dob, doctor_name, nurse_name)
             VALUES ($1, $2, $3, $4, $5)
             RETURNING record_index`,
            [patient_id_hash, patient_name || null, patient_dob || null, doctor_name || null, nurse_name || null]
        );
        
        res.json({ data: result.rows[0], error: null });
    } catch (err) {
        res.json({ data: null, error: err.message });
    }
});

app.listen(PORT, () => {
    console.log(`🚀 Server running at http://localhost:${PORT}`);
});
