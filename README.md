# Supabase Website Connection

A static HTML/CSS/JS website that connects directly to Supabase PostgreSQL.  
**No server required** - works on GitHub Pages! 🚀

---

## Quick Start (GitHub Pages)

1. **Configure Supabase credentials** in `src/connection/config.js`:

```js
const SUPABASE_URL = 'https://YOUR_PROJECT.supabase.co';
const SUPABASE_ANON_KEY = 'YOUR_ANON_KEY_HERE';
const DEFAULT_TABLE_NAME = 'your_table_name';
```

2. **Push to GitHub** and enable GitHub Pages:
   - Go to repo **Settings → Pages**
   - Set source to `main` branch, folder `/src`
   - Your site will be live at `https://username.github.io/repo-name/`

---

## Local Development

Just open `src/index.html` in your browser, or use a local server:

```bash
# Using Python
cd src && python -m http.server 8000

# Using Node.js (npx)
npx serve src
```

---

## Project Structure

```
src/
├── index.html          # Main page
├── script.js           # App logic (uses Database class)
├── styles.css          # Styling
└── connection/
    ├── config.js       # Supabase URL + anon key
    └── database.js     # Database class (REST API wrapper)
```

---

## How It Works

This uses **Supabase's REST API** directly from the browser:
- No Express server needed
- No npm dependencies
- Works on any static hosting (GitHub Pages, Netlify, Vercel, etc.)

The `database.js` class provides:
- `selectAll()` - Get all records
- `select(filters)` - Get filtered records
- `insert(data)` - Insert a new record
- `update(data, filters)` - Update records
- `delete(filters)` - Delete records

---

## Security Notes

⚠️ **Don't commit your anon key to public repos!**

For production:
1. Enable **Row Level Security (RLS)** on your Supabase tables
2. The anon key is safe to expose IF RLS is properly configured
3. Consider using `.gitignore` for `config.js` and providing a `config.example.js`

---

## Supabase Setup

1. Create a table in Supabase SQL Editor
2. Enable RLS and create policies for your use case
3. Get your credentials from **Settings → API**:
   - Project URL → `SUPABASE_URL`
   - anon/public key → `SUPABASE_ANON_KEY`
