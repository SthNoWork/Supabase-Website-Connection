package connection;

import java.sql.*;
import java.util.*;

/**
 * Lightweight JDBC helper for PostgreSQL (Supabase) using the settings in DBConfig.
 * Requires the PostgreSQL JDBC driver on the classpath (org.postgresql:postgresql).
 */
public class DatabaseConnector {
    private final DBConfig config;

    public DatabaseConnector(DBConfig config) {
        this.config = config;
    }

    public Connection getConnection() throws SQLException {
        // Load driver (optional with modern JVMs if driver is on classpath)
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            // If driver isn't on classpath, let DriverManager attempt autodiscovery later.
        }
        return DriverManager.getConnection(config.getJdbcUrl(), config.toProperties());
    }

    public List<Map<String, Object>> fetchAll(String schema, String table) throws SQLException {
        String q = String.format("SELECT * FROM %s.%s", quoteIdent(schema), quoteIdent(table));
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(q); ResultSet rs = ps.executeQuery()) {
            return resultSetToList(rs);
        }
    }

    public int insert(String schema, String table, Map<String, Object> values) throws SQLException {
        if (values == null || values.isEmpty()) return 0;
        StringBuilder cols = new StringBuilder();
        StringBuilder ph = new StringBuilder();
        List<Object> vals = new ArrayList<>();
        int i = 0;
        for (String k : values.keySet()) {
            if (i > 0) {
                cols.append(",");
                ph.append(",");
            }
            cols.append(quoteIdent(k));
            ph.append("?");
            vals.add(values.get(k));
            i++;
        }
        String q = String.format("INSERT INTO %s.%s (%s) VALUES (%s)", quoteIdent(schema), quoteIdent(table), cols.toString(), ph.toString());
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(q)) {
            for (int j = 0; j < vals.size(); j++) ps.setObject(j + 1, vals.get(j));
            return ps.executeUpdate();
        }
    }

    private List<Map<String, Object>> resultSetToList(ResultSet rs) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= cols; i++) {
                row.put(md.getColumnLabel(i), rs.getObject(i));
            }
            list.add(row);
        }
        return list;
    }

    private String quoteIdent(String id) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("Identifier is empty");
        return '"' + id.replace("\"", "\"\"") + '"';
    }
}
