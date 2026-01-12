/**
 * Generic Database utility class for handling PostgreSQL operations
 * Works with any table specified in config.js
 * 
 * Usage:
 *   import Database from './connection/database.js';
 *   const db = new Database();
 *   await db.connect();
 *   const rows = await db.selectAll();
 *   await db.close();
 */

import pg from 'pg';
import { readFileSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';
import { DB_CONFIG } from './config.js';

const __dirname = dirname(fileURLToPath(import.meta.url));

export default class Database {
    
    constructor(tableName = null) {
        this.pool = null;
        this.tableName = tableName || DB_CONFIG.tableName;
    }

    // =========================================================================
    // CONNECTION MANAGEMENT
    // =========================================================================

    /**
     * Establish connection pool to the database
     */
    async connect() {
        const sslConfig = DB_CONFIG.ssl.ca
            ? { rejectUnauthorized: true, ca: readFileSync(join(__dirname, DB_CONFIG.ssl.ca)).toString() }
            : DB_CONFIG.ssl;

        this.pool = new pg.Pool({
            host: DB_CONFIG.host,
            port: DB_CONFIG.port,
            database: DB_CONFIG.database,
            user: DB_CONFIG.username,
            password: DB_CONFIG.password,
            ssl: sslConfig
        });

        // Test the connection
        await this.testConnection();
        console.log('✅ Connected to PostgreSQL (SSL + CA verified)');
    }

    /**
     * Close the database connection pool
     */
    async close() {
        if (this.pool) {
            await this.pool.end();
            console.log('🔌 Connection closed');
        }
    }

    /**
     * Test the connection
     */
    async testConnection() {
        const result = await this.pool.query('SELECT NOW()');
        return result.rows[0];
    }

    // =========================================================================
    // CRUD OPERATIONS
    // =========================================================================

    /**
     * SELECT * FROM table
     * No parameters - gets everything from the table
     * 
     * @returns {Array} Array of row objects
     */
    async selectAll() {
        const sql = `SELECT * FROM ${this.tableName}`;
        const result = await this.pool.query(sql);
        return result.rows;
    }

    /**
     * SELECT * FROM table WHERE column = value
     * 
     * @param {Object} filters - Map of column names and their values for WHERE clause
     * @example
     *   await db.select({ doctor_name: 'Dr. Strange', patient_name: 'John' })
     *   // Generates: WHERE doctor_name = $1 AND patient_name = $2
     * @returns {Array} Array of matching row objects
     */
    async select(filters = {}) {
        if (!filters || Object.keys(filters).length === 0) {
            return this.selectAll();
        }

        const columns = Object.keys(filters);
        const values = Object.values(filters);
        const whereClause = columns.map((col, i) => `${col} = $${i + 1}`).join(' AND ');

        const sql = `SELECT * FROM ${this.tableName} WHERE ${whereClause}`;
        const result = await this.pool.query(sql, values);
        return result.rows;
    }

    /**
     * INSERT INTO table (columns...) VALUES (values...) RETURNING *
     * 
     * @param {Object} data - Map of column names and their values
     * @example
     *   await db.insert({ patient_name: 'John', doctor_name: 'Dr. Smith' })
     * @returns {Object} The inserted row
     */
    async insert(data) {
        if (!data || Object.keys(data).length === 0) {
            throw new Error('Data cannot be empty');
        }

        const columns = Object.keys(data);
        const values = Object.values(data);
        const placeholders = columns.map((_, i) => `$${i + 1}`).join(', ');

        const sql = `INSERT INTO ${this.tableName} (${columns.join(', ')}) VALUES (${placeholders}) RETURNING *`;
        const result = await this.pool.query(sql, values);
        return result.rows[0];
    }

    /**
     * UPDATE table SET column = value WHERE filter_column = filter_value
     * 
     * @param {Object} data - Map of columns to update with new values
     * @param {Object} filters - Map of columns for WHERE clause
     * @example
     *   await db.update({ patient_name: 'Jane' }, { record_index: 5 })
     * @returns {Object} { rowsAffected, rows }
     */
    async update(data, filters) {
        if (!data || Object.keys(data).length === 0) {
            throw new Error('Data cannot be empty');
        }
        if (!filters || Object.keys(filters).length === 0) {
            throw new Error('Filters required for UPDATE (safety check)');
        }

        const dataColumns = Object.keys(data);
        const dataValues = Object.values(data);
        const filterColumns = Object.keys(filters);
        const filterValues = Object.values(filters);

        let paramIndex = 1;
        const setClause = dataColumns.map(col => `${col} = $${paramIndex++}`).join(', ');
        const whereClause = filterColumns.map(col => `${col} = $${paramIndex++}`).join(' AND ');

        const sql = `UPDATE ${this.tableName} SET ${setClause} WHERE ${whereClause} RETURNING *`;
        const result = await this.pool.query(sql, [...dataValues, ...filterValues]);
        return { rowsAffected: result.rowCount, rows: result.rows };
    }

    /**
     * DELETE FROM table WHERE column = value
     * 
     * @param {Object} filters - Map of columns for WHERE clause (REQUIRED for safety)
     * @example
     *   await db.delete({ record_index: 5 })
     * @returns {Object} { rowsAffected, rows }
     */
    async delete(filters) {
        if (!filters || Object.keys(filters).length === 0) {
            throw new Error('Filters required for DELETE (safety check)');
        }

        const columns = Object.keys(filters);
        const values = Object.values(filters);
        const whereClause = columns.map((col, i) => `${col} = $${i + 1}`).join(' AND ');

        const sql = `DELETE FROM ${this.tableName} WHERE ${whereClause} RETURNING *`;
        const result = await this.pool.query(sql, values);
        return { rowsAffected: result.rowCount, rows: result.rows };
    }

    // =========================================================================
    // UTILITY METHODS
    // =========================================================================

    /**
     * Print all records from table in a formatted view
     */
    async printTable() {
        const rows = await this.selectAll();
        this._printRows(rows, `Table: ${this.tableName}`);
    }

    /**
     * Print filtered records from table
     * 
     * @param {Object} filters - Map of columns for WHERE clause
     */
    async printFiltered(filters) {
        const rows = await this.select(filters);
        this._printRows(rows, `Table: ${this.tableName} (Filtered)`);
    }

    /**
     * Execute a raw SQL query (for advanced use cases)
     * 
     * @param {string} sql - The SQL query
     * @param {Array} params - Query parameters
     * @returns {Object} { rows, rowCount }
     */
    async query(sql, params = []) {
        const result = await this.pool.query(sql, params);
        return { rows: result.rows, rowCount: result.rowCount };
    }

    /**
     * Change the target table for subsequent operations
     * 
     * @param {string} tableName - New table name
     */
    setTable(tableName) {
        this.tableName = tableName;
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    _printRows(rows, title) {
        console.log(`\n=== ${title} ===`);
        
        if (rows.length === 0) {
            console.log('No records found.\n');
            return;
        }

        const columns = Object.keys(rows[0]);
        
        // Header
        console.log(columns.map(c => c.padEnd(20)).join(' '));
        console.log('─'.repeat(columns.length * 21));

        // Rows
        for (const row of rows) {
            const line = columns.map(col => {
                const val = row[col];
                const str = val === null ? 'NULL' : String(val);
                return str.substring(0, 19).padEnd(20);
            }).join(' ');
            console.log(line);
        }

        console.log('─'.repeat(columns.length * 21));
        console.log(`Total records: ${rows.length}\n`);
    }
}

// ─── Convenience: Single-query functions (no need to manage connection) ─────

/**
 * Quick helper for one-off queries without managing connection lifecycle
 * Automatically connects, runs query, and disconnects
 * 
 * @example
 *   import { quickQuery } from './connection/database.js';
 *   const rows = await quickQuery(db => db.selectAll());
 */
export async function quickQuery(callback, tableName = null) {
    const db = new Database(tableName);
    try {
        await db.connect();
        return await callback(db);
    } finally {
        await db.close();
    }
}
