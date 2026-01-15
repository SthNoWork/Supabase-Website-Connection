/**
 * Browser-compatible Database utility class using Supabase REST API
 * Works directly from static HTML/JS - no server required!
 * 
 * Usage:
 *   import Database from './connection/database.js';
 *   const db = new Database();
 *   const rows = await db.selectAll();
 */

import { SUPABASE_URL, SUPABASE_ANON_KEY, DEFAULT_TABLE_NAME, SCHEMA_NAME } from './config.js';

export default class Database {
    
    constructor(tableName = null) {
        this.tableName = tableName || DEFAULT_TABLE_NAME;
        this.baseUrl = `${SUPABASE_URL}/rest/v1`;
        this.headers = {
            'apikey': SUPABASE_ANON_KEY,
            'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
            'Content-Type': 'application/json',
            'Prefer': 'return=representation'
        };
        // Add schema header if not using public schema
        if (SCHEMA_NAME && SCHEMA_NAME !== 'public') {
            this.headers['Accept-Profile'] = SCHEMA_NAME;
            this.headers['Content-Profile'] = SCHEMA_NAME;
        }
    }

    // =========================================================================
    // CRUD OPERATIONS
    // =========================================================================

    /**
     * SELECT * FROM table
     * @returns {Promise<Array>} Array of row objects
     */
    async selectAll() {
        const response = await fetch(`${this.baseUrl}/${this.tableName}?select=*`, {
            method: 'GET',
            headers: this.headers
        });
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to fetch records');
        }
        
        return await response.json();
    }

    /**
     * SELECT * FROM table WHERE column = value
     * @param {Object} filters - Map of column names and their values for WHERE clause
     * @returns {Promise<Array>} Array of matching row objects
     */
    async select(filters = {}) {
        if (!filters || Object.keys(filters).length === 0) {
            return this.selectAll();
        }

        // Build query string with filters
        const queryParams = Object.entries(filters)
            .map(([col, val]) => `${encodeURIComponent(col)}=eq.${encodeURIComponent(val)}`)
            .join('&');

        const response = await fetch(`${this.baseUrl}/${this.tableName}?select=*&${queryParams}`, {
            method: 'GET',
            headers: this.headers
        });
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to fetch records');
        }
        
        return await response.json();
    }

    /**
     * INSERT INTO table
     * @param {Object} data - Object with column-value pairs to insert
     * @returns {Promise<Object>} The inserted row
     */
    async insert(data) {
        const response = await fetch(`${this.baseUrl}/${this.tableName}`, {
            method: 'POST',
            headers: this.headers,
            body: JSON.stringify(data)
        });
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to insert record');
        }
        
        const rows = await response.json();
        return rows[0];
    }

    /**
     * UPDATE table SET ... WHERE ...
     * @param {Object} data - Object with column-value pairs to update
     * @param {Object} filters - WHERE conditions
     * @returns {Promise<Object>} Result with updated rows
     */
    async update(data, filters = {}) {
        if (!filters || Object.keys(filters).length === 0) {
            throw new Error('Update requires at least one filter to prevent accidental mass updates');
        }

        const queryParams = Object.entries(filters)
            .map(([col, val]) => `${encodeURIComponent(col)}=eq.${encodeURIComponent(val)}`)
            .join('&');

        const response = await fetch(`${this.baseUrl}/${this.tableName}?${queryParams}`, {
            method: 'PATCH',
            headers: this.headers,
            body: JSON.stringify(data)
        });
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to update records');
        }
        
        const rows = await response.json();
        return { rows, rowsAffected: rows.length };
    }

    /**
     * DELETE FROM table WHERE ...
     * @param {Object} filters - WHERE conditions (required to prevent accidental deletion)
     * @returns {Promise<Object>} Result with deleted rows
     */
    async delete(filters = {}) {
        if (!filters || Object.keys(filters).length === 0) {
            throw new Error('Delete requires at least one filter to prevent accidental mass deletion');
        }

        const queryParams = Object.entries(filters)
            .map(([col, val]) => `${encodeURIComponent(col)}=eq.${encodeURIComponent(val)}`)
            .join('&');

        const response = await fetch(`${this.baseUrl}/${this.tableName}?${queryParams}`, {
            method: 'DELETE',
            headers: this.headers
        });
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to delete records');
        }
        
        const rows = await response.json();
        return { rows, rowsAffected: rows.length };
    }

    /**
     * Test the connection by fetching table info
     * @returns {Promise<boolean>} True if connection successful
     */
    async testConnection() {
        try {
            const response = await fetch(`${this.baseUrl}/${this.tableName}?select=*&limit=1`, {
                method: 'GET',
                headers: this.headers
            });
            return response.ok;
        } catch (err) {
            console.error('Connection test failed:', err);
            return false;
        }
    }
}
