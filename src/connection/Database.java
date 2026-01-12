package connection;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Generic Database utility class for handling PostgreSQL operations
 * Works with any table specified in Config.java
 */
public class Database {

    private Connection conn;
    private Config config;

    public Database() {
        this.config = Config.getInstance();
    }

    /**
     * Establish connection to the database
     */
    public void connect() throws SQLException, ClassNotFoundException {
        Properties props = new Properties();
        props.setProperty("user", config.getUsername());
        props.setProperty("password", config.getPassword());
        props.setProperty("ssl", "true");
        props.setProperty("sslmode", "verify-full"); // Verify server identity

        Class.forName("org.postgresql.Driver");

        // Load SSL certificate (REQUIRED for secure connection)
        File certFile = new File(config.getSslCertPath());
        if (!certFile.exists()) {
            throw new RuntimeException("SSL certificate not found at: " + config.getSslCertPath() +
                    "\nPlease ensure prod-ca-2021.crt is in the connection folder");
        }
        props.setProperty("sslrootcert", config.getSslCertPath());

        String jdbcUrl = config.getJdbcUrl();
        this.conn = DriverManager.getConnection(jdbcUrl, props);
        System.out.println("✅ Connected to PostgreSQL (SSL + CA verified)");
    }

    /**
     * Close the database connection
     */
    public void close() {
        if (conn != null) {
            try {
                conn.close();
                System.out.println("🔌 Connection closed");
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    /**
     * Test the connection
     */
    public void testConnection() throws SQLException {
        String sql = "SELECT NOW()";
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                System.out.println("Current database time: " + rs.getTimestamp(1));
            }
        }
    }

    /**
     * SELECT * FROM default_table
     * No parameters - gets everything from the table
     */
    public ResultSet selectAll() throws SQLException {
        String sql = "SELECT * FROM " + config.getTableName();
        Statement stmt = conn.createStatement();
        return stmt.executeQuery(sql);
    }

    /**
     * SELECT * FROM default_table WHERE column = value
     * 
     * @param filters Map of column names and their values for WHERE clause
     *                Example: Map.of("doctor_name", "Dr. Strange", "patient_name",
     *                "John")
     *                Generates: WHERE doctor_name = 'Dr. Strange' AND patient_name
     *                = 'John'
     */
    public ResultSet select(Map<String, Object> filters) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM " + config.getTableName());
        List<Object> params = new ArrayList<>();

        if (filters != null && !filters.isEmpty()) {
            sql.append(" WHERE ");
            int count = 0;
            for (Map.Entry<String, Object> entry : filters.entrySet()) {
                if (count > 0)
                    sql.append(" AND ");
                sql.append(entry.getKey()).append(" = ?");
                params.add(entry.getValue());
                count++;
            }
        }

        PreparedStatement pstmt = conn.prepareStatement(sql.toString());
        for (int i = 0; i < params.size(); i++) {
            pstmt.setObject(i + 1, params.get(i));
        }

        return pstmt.executeQuery();
    }

    /**
     * INSERT INTO default_table (columns...) VALUES (values...)
     * 
     * @param data Map of column names and their values
     *             Example: Map.of("patient_name", "John", "doctor_name", "Dr.
     *             Smith")
     * @return ResultSet with inserted row data (RETURNING *)
     */
    public ResultSet insert(Map<String, Object> data) throws SQLException {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Data cannot be empty");
        }

        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        List<Object> params = new ArrayList<>();

        int count = 0;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (count > 0) {
                columns.append(", ");
                values.append(", ");
            }
            columns.append(entry.getKey());
            values.append("?");
            params.add(entry.getValue());
            count++;
        }

        String sql = String.format(
                "INSERT INTO %s (%s) VALUES (%s) RETURNING *",
                config.getTableName(), columns, values);

        PreparedStatement pstmt = conn.prepareStatement(sql);
        for (int i = 0; i < params.size(); i++) {
            pstmt.setObject(i + 1, params.get(i));
        }

        return pstmt.executeQuery();
    }

    /**
     * UPDATE default_table SET column = value WHERE filter_column = filter_value
     * 
     * @param data    Map of columns to update with new values
     *                Example: Map.of("patient_name", "Jane", "doctor_name", "Dr.
     *                Williams")
     * @param filters Map of columns for WHERE clause
     *                Example: Map.of("record_index", 5)
     * @return Number of rows affected
     */
    public int update(Map<String, Object> data, Map<String, Object> filters) throws SQLException {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Data cannot be empty");
        }

        StringBuilder sql = new StringBuilder("UPDATE " + config.getTableName() + " SET ");
        List<Object> params = new ArrayList<>();

        int count = 0;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (count > 0)
                sql.append(", ");
            sql.append(entry.getKey()).append(" = ?");
            params.add(entry.getValue());
            count++;
        }

        if (filters != null && !filters.isEmpty()) {
            sql.append(" WHERE ");
            count = 0;
            for (Map.Entry<String, Object> entry : filters.entrySet()) {
                if (count > 0)
                    sql.append(" AND ");
                sql.append(entry.getKey()).append(" = ?");
                params.add(entry.getValue());
                count++;
            }
        }

        PreparedStatement pstmt = conn.prepareStatement(sql.toString());
        for (int i = 0; i < params.size(); i++) {
            pstmt.setObject(i + 1, params.get(i));
        }

        return pstmt.executeUpdate();
    }

    /**
     * DELETE FROM default_table WHERE column = value
     * 
     * @param filters Map of columns for WHERE clause (REQUIRED for safety)
     *                Example: Map.of("record_index", 5)
     * @return Number of rows affected
     */
    public int delete(Map<String, Object> filters) throws SQLException {
        if (filters == null || filters.isEmpty()) {
            throw new IllegalArgumentException("Filters required for DELETE (safety check)");
        }

        StringBuilder sql = new StringBuilder("DELETE FROM " + config.getTableName() + " WHERE ");
        List<Object> params = new ArrayList<>();

        int count = 0;
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            if (count > 0)
                sql.append(" AND ");
            sql.append(entry.getKey()).append(" = ?");
            params.add(entry.getValue());
            count++;
        }

        PreparedStatement pstmt = conn.prepareStatement(sql.toString());
        for (int i = 0; i < params.size(); i++) {
            pstmt.setObject(i + 1, params.get(i));
        }

        return pstmt.executeUpdate();
    }

    /**
     * Print all records from default table in a formatted view
     */
    public void printTable() throws SQLException {
        ResultSet rs = selectAll();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        System.out.println("\n=== Table: " + config.getTableName() + " ===");
        for (int i = 1; i <= columnCount; i++) {
            System.out.printf("%-20s ", metaData.getColumnName(i));
        }
        System.out.println("\n" + "─".repeat(columnCount * 21));

        int count = 0;
        while (rs.next()) {
            for (int i = 1; i <= columnCount; i++) {
                String value = rs.getString(i);
                System.out.printf("%-20s ", value != null ? value : "NULL");
            }
            System.out.println();
            count++;
        }

        System.out.println("─".repeat(columnCount * 21));
        System.out.println("Total records: " + count + "\n");

        rs.close();
    }

    /**
     * Print filtered records from default table
     * 
     * @param filters Map of columns for WHERE clause
     */
    public void printFiltered(Map<String, Object> filters) throws SQLException {
        ResultSet rs = select(filters);
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        System.out.println("\n=== Table: " + config.getTableName() + " (Filtered) ===");
        for (int i = 1; i <= columnCount; i++) {
            System.out.printf("%-20s ", metaData.getColumnName(i));
        }
        System.out.println("\n" + "─".repeat(columnCount * 21));

        int count = 0;
        while (rs.next()) {
            for (int i = 1; i <= columnCount; i++) {
                String value = rs.getString(i);
                System.out.printf("%-20s ", value != null ? value : "NULL");
            }
            System.out.println();
            count++;
        }

        System.out.println("─".repeat(columnCount * 21));
        System.out.println("Total records: " + count + "\n");

        rs.close();
    }
}