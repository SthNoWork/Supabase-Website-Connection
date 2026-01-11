// ⚠️ DO NOT COMMIT THIS FILE - Contains database credentials

// ─── Easy Config ─────────────────────────────────────────────────────────────
// Paste your Session Pooler connection string from Supabase Dashboard → Settings → Database
const CONNECTION_STRING = 'postgresql://postgres.xvlilgsawbqpedmrbdkv:[YOUR-PASSWORD]@aws-1-ap-south-1.pooler.supabase.com:5432/postgres';

// Override password here (replace [YOUR-PASSWORD] above OR set it here)
const PASSWORD = 'restricted';

// Override username if using a custom database user (e.g., 'restricted' instead of 'postgres')
const USERNAME_OVERRIDE = 'restricted'; // Set to null to use username from connection string

// ─── Auto-parse connection string ────────────────────────────────────────────
const url = new URL(CONNECTION_STRING);
const [baseUser, projectRef] = url.username.split('.');

export const DB_CONFIG = {
    host: url.hostname,
    port: parseInt(url.port) || 5432,
    database: url.pathname.slice(1) || 'postgres',
    username: USERNAME_OVERRIDE 
        ? `${USERNAME_OVERRIDE}.${projectRef}` 
        : url.username,
    password: PASSWORD || decodeURIComponent(url.password),

    // ─── SSL ─────────────────────────────────────────────────────────────────
    ssl: {
        rejectUnauthorized: true,
        ca: './prod-ca-2021.crt'
    }
};

