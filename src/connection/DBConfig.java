package connection;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * DBConfig holds connection parameters and provides helpers similar to the JS `config.js`.
 *
 * At the top of this file you can paste your session pooler values directly for quick
 * testing (similar to `config.js`). Edit the four variables below:
 *
 *  - `SESSION_POOLER` : full pooler connection string from Supabase
 *  - `USERNAME`       : pooler username (without project ref), optional
 *  - `PASSWORD`       : password override, optional
 *  - `CERT_PATH`      : path to CA certificate file, optional
 */
public class DBConfig {

    // ----------------- Editable values (paste here) -----------------
    // Example: postgresql://postgres.xref:password@aws-1-ap-south-1.pooler.supabase.com:5432/postgres
    public static String SESSION_POOLER = "postgresql://postgres.xvlilgsawbqpedmrbdkv:[YOUR-PASSWORD]@aws-1-ap-south-1.pooler.supabase.com:5432/postgres";

    // Username WITHOUT project ref. If empty, username from SESSION_POOLER is used.
    public static String USERNAME = "restricted";

    // Password override. If empty, password from SESSION_POOLER is used.
    public static String PASSWORD = "restricted";

    // CA certificate path (relative to project root or absolute). Leave empty if not using.
    public static String CERT_PATH = "src/connection/prod-ca-2021.crt";

    // Convenience: build DBConfig from the static values above
    public static DBConfig fromStatics() {
        // Prefer environment variable to avoid committing secrets to source code
        String envConn = System.getenv("SUPABASE_POOLER");
        String conn = (envConn != null && !envConn.isEmpty()) ? envConn : ((SESSION_POOLER != null && !SESSION_POOLER.isEmpty()) ? SESSION_POOLER : null);
        if (conn == null) throw new IllegalStateException("Connection string not set. Set the SUPABASE_POOLER env var or DBConfig.SESSION_POOLER before calling fromStatics().");

        String userOverride = (USERNAME != null && !USERNAME.isEmpty()) ? USERNAME : null;
        String passOverride = (PASSWORD != null && !PASSWORD.isEmpty()) ? PASSWORD : null;
        String cert = (CERT_PATH != null && !CERT_PATH.isEmpty()) ? CERT_PATH : null;

        // Basic sanity check: connection string should start with postgresql://
        if (!conn.startsWith("postgresql://") && !conn.startsWith("postgres://")) {
            throw new IllegalArgumentException("Invalid connection string: " + conn);
        }

        return fromConnectionString(conn, userOverride, passOverride, cert);
    }

    public final String host;
    public final int port;
    public final String database;
    public final String username; // full username including project ref (e.g. restricted.xref)
    public final String password;
    /** Path to CA certificate file (optional). Example: "src/connection/prod-ca-2021.crt" */
    public final String sslRootCertPath;

    public DBConfig(String host, int port, String database, String username, String password, String sslRootCertPath) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.sslRootCertPath = sslRootCertPath;
    }

    /**
     * Create a DBConfig from a PostgreSQL connection string. The connection string
     * should have the form: postgresql://user[.projectRef]:password@host:port/database
     *
     * @param connectionString full pooler connection string (paste from Supabase)
     * @param usernameOverride optional username (without project ref). If provided, the projectRef
     *                         part from the connection string will be appended.
     *                         Example: usernameOverride="restricted" -> "restricted.<projectRef>"
     *                         Pass null or empty to use username from the connection string.
     * @param passwordOverride optional password to override the one in the connection string
     * @param sslRootCertPath  optional path to CA cert file (relative or absolute)
     */
    public static DBConfig fromConnectionString(String connectionString, String usernameOverride, String passwordOverride, String sslRootCertPath) {
        try {
            URI uri = new URI(connectionString);
            String host = uri.getHost();
            int port = uri.getPort() == -1 ? 5432 : uri.getPort();
            String path = uri.getPath();
            String database = (path != null && path.length() > 1) ? path.substring(1) : "postgres";

            String userInfo = uri.getUserInfo();
            String usernameFromConn = null;
            String passwordFromConn = null;
            if (userInfo != null) {
                String[] ui = userInfo.split(":", 2);
                usernameFromConn = ui.length > 0 ? ui[0] : null;
                passwordFromConn = ui.length > 1 ? ui[1] : null;
            }

            String projectRef = null;
            if (usernameFromConn != null && usernameFromConn.contains(".")) {
                String[] parts = usernameFromConn.split("\\.", 2);
                if (parts.length > 1) projectRef = parts[1];
            }

            String finalUsername;
            if (usernameOverride != null && !usernameOverride.isEmpty()) {
                if (projectRef != null && !projectRef.isEmpty()) finalUsername = usernameOverride + "." + projectRef;
                else finalUsername = usernameOverride;
            } else {
                finalUsername = usernameFromConn;
            }

            String finalPassword = (passwordOverride != null && !passwordOverride.isEmpty()) ? passwordOverride : passwordFromConn;

            return new DBConfig(host, port, database, finalUsername, finalPassword, sslRootCertPath);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid connection string: " + connectionString, e);
        }
    }

    public String getJdbcUrl() {
        return String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
    }

    public Properties toProperties() {
        Properties props = new Properties();
        if (username != null) props.setProperty("user", username);
        if (password != null) props.setProperty("password", password);
        // enable SSL
        props.setProperty("ssl", "true");
        // If you provide a CA cert path, use verify-ca to validate server cert
        if (sslRootCertPath != null && !sslRootCertPath.isEmpty()) {
            props.setProperty("sslmode", "verify-ca");
            props.setProperty("sslrootcert", sslRootCertPath);
        } else {
            // fallback: request SSL but don't verify root (not recommended for production)
            props.setProperty("sslmode", "require");
        }
        return props;
    }
}
