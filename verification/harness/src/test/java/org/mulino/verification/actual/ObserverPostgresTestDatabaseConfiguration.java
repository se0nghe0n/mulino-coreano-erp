package org.mulino.verification.actual;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Fail-closed target and credential guard for the isolated observer PostgreSQL tests. */
final class ObserverPostgresTestDatabaseConfiguration {
    static final String OPT_IN = "OBSERVER_PG_TEST";
    static final String DISPOSABLE = "OBSERVER_PG_DISPOSABLE";
    static final String JDBC_URL = "OBSERVER_PG_URL";
    static final String DB_USER = "OBSERVER_PG_USER";
    static final String DB_PASSWORD = "OBSERVER_PG_PASSWORD";

    private static final String JDBC_PREFIX = "jdbc:postgresql://";
    private static final Pattern DATABASE_IDENTITY =
            Pattern.compile("mulino_observer_test_([0-9a-f]{16})");
    private static final Pattern SCHEMA = Pattern.compile("obs_[0-9a-f]{32}");
    private static final Pattern AUTHORITY =
            Pattern.compile("(?i:localhost):[0-9]{1,5}|127\\.0\\.0\\.1:[0-9]{1,5}|\\[::1\\]:[0-9]{1,5}");

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final String databaseName;

    private ObserverPostgresTestDatabaseConfiguration(
            String jdbcUrl, String username, String password, String databaseName) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.databaseName = databaseName;
    }

    static ObserverPostgresTestDatabaseConfiguration fromEnvironment(Map<String, String> environment) {
        if (!"true".equals(environment.get(OPT_IN)) || !"true".equals(environment.get(DISPOSABLE))) {
            throw new IllegalArgumentException(OPT_IN + "=true and " + DISPOSABLE + "=true are required for observer PostgreSQL tests");
        }

        String jdbcUrl = required(environment, JDBC_URL);
        String username = required(environment, DB_USER);
        String password = required(environment, DB_PASSWORD);
        String databaseName = safeDatabaseName(jdbcUrl, username);
        if (databaseName == null) {
            throw new IllegalArgumentException(JDBC_URL + " must target a dedicated loopback observer test database without URL credentials or options");
        }
        return new ObserverPostgresTestDatabaseConfiguration(jdbcUrl, username, password, databaseName);
    }

    String username() {
        return username;
    }

    String password() {
        return password;
    }

    String schemaJdbcUrl(String schema) {
        if (schema == null || !SCHEMA.matcher(schema).matches()) {
            throw new IllegalArgumentException("Observer test schema must be a generated isolated schema name");
        }
        return jdbcUrl + "?currentSchema=" + schema;
    }

    Connection openVerifiedConnection() throws SQLException {
        return openVerifiedConnection(jdbcUrl);
    }

    Connection openSchemaConnection(String schema) throws SQLException {
        return openVerifiedConnection(schemaJdbcUrl(schema));
    }

    private Connection openVerifiedConnection(String targetUrl) throws SQLException {
        Connection connection;
        try {
            connection = DriverManager.getConnection(targetUrl, username, password);
        } catch (SQLException failure) {
            throw redacted(failure);
        }
        try (Statement statement = connection.createStatement();
             ResultSet identity = statement.executeQuery("SELECT current_database(), current_user")) {
            if (!identity.next()
                    || !databaseName.equals(identity.getString(1))
                    || !username.equals(identity.getString(2))) {
                throw new SQLException("Observer test database identity mismatch", "28000");
            }
            return connection;
        } catch (SQLException failure) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // The original failure is replaced by a redacted exception below.
            }
            throw redacted(failure);
        }
    }

    private static String required(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing nonblank environment variable " + name);
        }
        return value;
    }

    private static String safeDatabaseName(String jdbcUrl, String username) {
        if (!jdbcUrl.startsWith(JDBC_PREFIX)) return null;
        try {
            URI target = URI.create(jdbcUrl.substring("jdbc:".length()));
            String authority = target.getRawAuthority();
            if (!"postgresql".equalsIgnoreCase(target.getScheme())
                    || authority == null
                    || !AUTHORITY.matcher(authority).matches()
                    || target.getRawUserInfo() != null
                    || target.getRawQuery() != null
                    || target.getRawFragment() != null
                    || target.getPort() < 1
                    || target.getPort() > 65535
                    || target.getPort() == 5432) {
                return null;
            }

            String host = target.getHost();
            boolean loopback = "localhost".equalsIgnoreCase(host)
                    || "127.0.0.1".equals(host)
                    || "::1".equals(host)
                    || "[::1]".equals(host);
            if (!loopback) return null;

            String rawPath = target.getRawPath();
            if (rawPath == null || !rawPath.startsWith("/") || rawPath.length() <= 1) return null;
            String databaseName = rawPath.substring(1);
            Matcher identity = DATABASE_IDENTITY.matcher(databaseName);
            if (!identity.matches()
                    || !("observer_test_" + identity.group(1)).equals(username)) {
                return null;
            }
            return databaseName;
        } catch (IllegalArgumentException malformed) {
            // URI parser diagnostics can contain the original URL; keep it out of test output.
            return null;
        }
    }

    static SQLException redacted(SQLException failure) {
        return new SQLException("Observer PostgreSQL test connection failed; database details are redacted.",
                failure.getSQLState());
    }

    @Override
    public String toString() {
        return "ObserverPostgresTestDatabaseConfiguration[jdbcUrl=<redacted>, username=<redacted>, password=<redacted>]";
    }
}
