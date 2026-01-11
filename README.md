# Supabase Connector

A Node.js + Express server that connects directly to PostgreSQL (Supabase) with SSL.

---

## Quick Start

```bash
npm install
npm start
```

Server runs at **http://localhost:3000**

---

## Configuration

Edit `src/connection/config.js` with your Supabase credentials:

```js
export const DB_CONFIG = {
    host: 'aws-1-ap-south-1.pooler.supabase.com',
    port: 5432,
    database: 'postgres',
    
    // Username format for pooler: USERNAME.PROJECT_REF
    username: 'your_user.your_project_ref',
    password: 'your_password',

    ssl: {
        rejectUnauthorized: true,
        ca: './prod-ca-2021.crt'
    }
};

//add user 
//copy this to ai and ask i i wanna create a user with the password and username (of ur choice) with acess to the table of a schema and it should work
//run this in the sql editor in the postgres
-- 1️⃣ Grant access to xxxSchemaNamexxx schema (skip CREATE ROLE)
GRANT USAGE ON SCHEMA xxxSchemaNamexxx TO restricted;

-- 2️⃣ Grant privileges on the table
GRANT SELECT, INSERT, UPDATE ON xxxSchemaNamexxx.xxxTable_Namexxx TO restricted;

-- 3️⃣ Grant privileges on the sequence (for auto-increment)
GRANT USAGE, SELECT, UPDATE ON SEQUENCE xxxSchemaNamexxx.xxxTable_Namexxx_record_index_seq TO restricted;

-- 4️⃣ Enable RLS on the table (if not already)
ALTER TABLE xxxSchemaNamexxx.xxxTable_Namexxx ENABLE ROW LEVEL SECURITY;

-- 5️⃣ Add RLS policies for restricted user
CREATE POLICY "xUSERNAMEx_select" ON xxxSchemaNamexxx.xxxTable_Namexxx
FOR SELECT
TO restricted
USING (true);

CREATE POLICY "xUSERNAMEx_insert" ON xxxSchemaNamexxx.xxxTable_Namexxx
FOR INSERT
TO restricted
WITH CHECK (true);

CREATE POLICY "xUSERNAMEx_update" ON xxxSchemaNamexxx.xxxTable_Namexxx
FOR UPDATE
TO restricted
USING (true)
WITH CHECK (true);

//
```

### How to get your credentials:

1. Go to **Supabase Dashboard → Settings → Database**
2. Select **Session pooler** (recommended for servers)
3. Copy:
   - **Host**: e.g. `aws-1-ap-south-1.pooler.supabase.com`
   - **Username**: `postgres.YOUR_PROJECT_REF` (or custom user)
   - **Password**: Your database password

### Username format for pooler:
- Default user: `postgres.YOUR_PROJECT_REF`
- Custom user: `YOUR_USERNAME.YOUR_PROJECT_REF`

---

## SSL Certificate

The SSL certificate (`prod-ca-2021.crt`) is in `src/connection/`. Download the latest from Supabase if needed.

---

## Project Structure

```
├── src/
│   ├── connection/
│   │   ├── server.js         # Express server with API routes
│   │   ├── config.js         # Database credentials (DO NOT COMMIT)
│   │   └── prod-ca-2021.crt  # SSL certificate
│   └── index.html            # Frontend form
└── package.json
```

---

## API Endpoints

| Method | Endpoint       | Description          |
|--------|----------------|----------------------|
| GET    | `/api/records` | Fetch all records    |
| POST   | `/api/records` | Insert a new record  |

---

## For New Projects

1. Clone this repo
2. Update `src/connection/config.js` with your new Supabase project credentials
3. Modify `src/connection/server.js` to match your table schema
4. Update `src/index.html` form fields as needed
5. Run `npm start`

---

## Security Notes

- **Never commit `src/connection/config.js`** - it's in `.gitignore`
- Use the **Session pooler** for persistent connections
- Use a **restricted database user** in production, not the postgres admin