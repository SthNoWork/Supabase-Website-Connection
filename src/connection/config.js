// ⚠️ DO NOT COMMIT THIS FILE - Contains database credentials

// ─── Easy Config ─────────────────────────────────────────────────────────────
// Paste your Session Pooler connection string from Supabase Dashboard → Settings → Database
const CONNECTION_STRING = 'postgresql://postgres.xvlilgsawbqpedmrbdkv:[YOUR-PASSWORD]@aws-1-ap-south-1.pooler.supabase.com:5432/postgres';

// Override password here (replace [YOUR-PASSWORD] above OR set it here)
const PASSWORD = 'restricted';

// Override username if using a custom database user (e.g., 'restricted' instead of 'postgres')
const USERNAME_OVERRIDE = 'restricted'; // Set to null to use username from connection string

// Default table name for database operations
const DEFAULT_TABLE_NAME = 'hospital_records';

// Schema name (use 'public' for default, or your custom schema name)
const SCHEMA_NAME = 'public';

// Supabase anon key (get from Dashboard → Settings → API → anon/public key)
// ⚠️ Must be the JWT key starting with "eyJ..." NOT the publishable key
const SUPABASE_ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inh2bGlsZ3Nhd2JxcGVkbXJiZGt2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjgxMjExMDYsImV4cCI6MjA4MzY5NzEwNn0.6_k63XlmfDIJ2jN0txMjcY-SKwYH7H_HF4b-3NrDKbA';

// ─── Auto-parse connection string ────────────────────────────────────────────
const url = new URL(CONNECTION_STRING);
const [baseUser, projectRef] = url.username.split('.');

// Build Supabase REST API URL from project ref
const SUPABASE_URL = `https://${projectRef}.supabase.co`;

export { SUPABASE_URL, SUPABASE_ANON_KEY, DEFAULT_TABLE_NAME, SCHEMA_NAME };