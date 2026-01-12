package connection;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ⚠️ DO NOT COMMIT THIS FILE - Contains database credentials
 * 
 * Database configuration for Supabase connection
 */
public class Config {

    // ─── Easy Config ─────────────────────────────────────────────────────────────
    // Paste your Session Pooler connection string from Supabase Dashboard →
    // Settings → Database
    private static final String CONNECTION_STRING = "postgresql://postgres.xvlilgsawbqpedmrbdkv:[YOUR-PASSWORD]@aws-1-ap-south-1.pooler.supabase.com:5432/postgres";

    // Override password here (replace [YOUR-PASSWORD] above OR set it here)
    private static final String PASSWORD = "restricted";

    // Override username if using a custom database user (e.g., 'restricted' instead
    // of 'postgres')
    private static final String USERNAME_OVERRIDE = "restricted"; // Set to null to use username from connection string

    // SSL certificate path (relative to where you run the program)
    private static final String SSL_CERT_PATH = "src/connection/prod-ca-2021.crt";

    // Default table name for operations
    private static final String DEFAULT_TABLE_NAME = "hospital_records";

    // ─── Parsed Configuration ────────────────────────────────────────────────────
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final String sslCertPath;
    private final String tableName;

    private static Config instance;

    private Config() {
        try {
            // Parse connection string manually using regex
            // Format: postgresql://username:password@host:port/database
            Pattern pattern = Pattern.compile(
                    "postgresql://([^:@]+)(?::([^@]+))?@([^:]+):(\\d+)/(.+)");
            Matcher matcher = pattern.matcher(CONNECTION_STRING);

            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid connection string format");
            }

            String originalUsername = matcher.group(1);
            String urlPassword = matcher.group(2); // This will be [YOUR-PASSWORD] or actual password
            this.host = matcher.group(3);
            this.port = Integer.parseInt(matcher.group(4));
            this.database = matcher.group(5);

            // Extract project reference from username (e.g., postgres.xvlilgsawbqpedmrbdkv)
            String[] userParts = originalUsername.split("\\.", 2);
            String baseUser = userParts[0];
            String projectRef = userParts.length > 1 ? userParts[1] : "";

            // Apply username override if specified
            if (USERNAME_OVERRIDE != null && !USERNAME_OVERRIDE.isEmpty()) {
                this.username = USERNAME_OVERRIDE + "." + projectRef;
            } else {
                this.username = originalUsername;
            }

            // Use override password if set, otherwise try to parse from connection string
            if (PASSWORD != null && !PASSWORD.isEmpty()) {
                this.password = PASSWORD;
            } else if (urlPassword != null && !urlPassword.equals("[YOUR-PASSWORD]")) {
                this.password = URLDecoder.decode(urlPassword, StandardCharsets.UTF_8);
            } else {
                throw new IllegalArgumentException(
                        "Password not set. Please set PASSWORD constant or replace [YOUR-PASSWORD] in CONNECTION_STRING");
            }

            this.sslCertPath = SSL_CERT_PATH;
            this.tableName = DEFAULT_TABLE_NAME;

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse database configuration: " + e.getMessage(), e);
        }
    }

    public static synchronized Config getInstance() {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getSslCertPath() {
        return sslCertPath;
    }

    public String getJdbcUrl() {
        return String.format("jdbc:postgresql://%s:%d/%s?ssl=true&sslmode=require",
                host, port, database);
    }

    public String getTableName() {
        return tableName;
    }

    // Debug method to print parsed configuration (without password)
    public void printConfig() {
        System.out.println("=== Database Configuration ===");
        System.out.println("Host: " + host);
        System.out.println("Port: " + port);
        System.out.println("Database: " + database);
        System.out.println("Username: " + username);
        System.out.println("Password: " + (password != null ? "***" : "NOT SET"));
        System.out.println("Default Table: " + tableName);
        System.out.println("JDBC URL: " + getJdbcUrl());
        System.out.println("============================");
    }
}