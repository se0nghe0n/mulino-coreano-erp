package com.mulino.adapters.verification;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Guard for the explicitly disposable PostgreSQL target of one real HTTP test. */
final class McpCaptureHttpPostgresTarget {
    static final String RUN = "MCP_CAPTURE_HTTP_PG_TEST";
    static final String DISPOSABLE = "MCP_CAPTURE_HTTP_PG_DISPOSABLE";
    static final String JDBC_URL = "MCP_CAPTURE_HTTP_PG_URL";
    static final String USER = "MCP_CAPTURE_HTTP_PG_USER";
    static final String PASSWORD = "MCP_CAPTURE_HTTP_PG_PASSWORD";

    private static final List<String> CONFIGURATION_KEYS = List.of(
            RUN, DISPOSABLE, JDBC_URL, USER, PASSWORD);
    private static final String JDBC_PREFIX = "jdbc:postgresql://";
    private static final Pattern DATABASE_NAME =
            Pattern.compile("mulino_mcp_capture_http_test_([0-9a-f]{16})");
    private static final Pattern ROLE_NAME =
            Pattern.compile("mcp_capture_http_test_([0-9a-f]{16})");
    private static final String PUBLIC_SCHEMA_INVENTORY_SQL = """
            SELECT EXISTS (
                       SELECT 1 FROM pg_catalog.pg_namespace WHERE nspname = 'public'
                   ),
                   (SELECT count(*) FROM pg_catalog.pg_class c
                    JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace WHERE n.nspname = 'public') +
                   (SELECT count(*) FROM pg_catalog.pg_proc p
                    JOIN pg_catalog.pg_namespace n ON n.oid = p.pronamespace WHERE n.nspname = 'public') +
                   (SELECT count(*) FROM pg_catalog.pg_type t
                    JOIN pg_catalog.pg_namespace n ON n.oid = t.typnamespace WHERE n.nspname = 'public') +
                   (SELECT count(*) FROM pg_catalog.pg_operator o
                    JOIN pg_catalog.pg_namespace n ON n.oid = o.oprnamespace WHERE n.nspname = 'public') +
                   (SELECT count(*) FROM pg_catalog.pg_opclass c
                    JOIN pg_catalog.pg_namespace n ON n.oid = c.opcnamespace WHERE n.nspname = 'public') +
                   (SELECT count(*) FROM pg_catalog.pg_opfamily f
                    JOIN pg_catalog.pg_namespace n ON n.oid = f.opfnamespace WHERE n.nspname = 'public') +
                   (SELECT count(*) FROM pg_catalog.pg_conversion c
                    JOIN pg_catalog.pg_namespace n ON n.oid = c.connamespace WHERE n.nspname = 'public') +
                   (SELECT count(*) FROM pg_catalog.pg_collation c
                    JOIN pg_catalog.pg_namespace n ON n.oid = c.collnamespace WHERE n.nspname = 'public') +
                   (SELECT count(*) FROM pg_catalog.pg_statistic_ext s
                    JOIN pg_catalog.pg_namespace n ON n.oid = s.stxnamespace WHERE n.nspname = 'public') +
                   (SELECT count(*) FROM pg_catalog.pg_ts_config c
                    JOIN pg_catalog.pg_namespace n ON n.oid = c.cfgnamespace WHERE n.nspname = 'public') +
                   (SELECT count(*) FROM pg_catalog.pg_ts_dict d
                    JOIN pg_catalog.pg_namespace n ON n.oid = d.dictnamespace WHERE n.nspname = 'public') +
                   (SELECT count(*) FROM pg_catalog.pg_ts_parser p
                    JOIN pg_catalog.pg_namespace n ON n.oid = p.prsnamespace WHERE n.nspname = 'public') +
                   (SELECT count(*) FROM pg_catalog.pg_ts_template t
                    JOIN pg_catalog.pg_namespace n ON n.oid = t.tmplnamespace WHERE n.nspname = 'public') +
                   (SELECT count(*) FROM pg_catalog.pg_extension e
                    JOIN pg_catalog.pg_namespace n ON n.oid = e.extnamespace WHERE n.nspname = 'public') +
                   (SELECT count(*) FROM pg_catalog.pg_default_acl a
                    JOIN pg_catalog.pg_namespace n ON n.oid = a.defaclnamespace WHERE n.nspname = 'public') +
                   (SELECT count(*) FROM pg_catalog.pg_publication_namespace p
                    JOIN pg_catalog.pg_namespace n ON n.oid = p.pnnspid WHERE n.nspname = 'public')
            """;

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final String databaseName;

    private McpCaptureHttpPostgresTarget(String jdbcUrl, String username,
                                          String password, String databaseName) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.databaseName = databaseName;
    }

    /** Empty only for a wholly absent namespace; partial opt-in never falls back. */
    static Optional<McpCaptureHttpPostgresTarget> fromEnvironment(
            Map<String, String> environment) {
        Objects.requireNonNull(environment, "environment");
        boolean configured = CONFIGURATION_KEYS.stream().anyMatch(environment::containsKey);
        if (!configured) return Optional.empty();

        if (!"true".equals(environment.get(RUN))) {
            throw new IllegalArgumentException(RUN + "=true is required for the external test database");
        }
        if (!"true".equals(environment.get(DISPOSABLE))) {
            throw new IllegalArgumentException(DISPOSABLE + "=true is required for the external test database");
        }

        String jdbcUrl = required(environment, JDBC_URL);
        String username = required(environment, USER);
        String password = required(environment, PASSWORD);
        if (!isSafeJdbcUrl(jdbcUrl, username)) {
            throw new IllegalArgumentException(JDBC_URL + " and " + USER
                    + " must identify the same disposable loopback PostgreSQL target, with an explicit non-5432 port and no URL options or userinfo");
        }
        return Optional.of(new McpCaptureHttpPostgresTarget(
                jdbcUrl, username, password, databaseNameFromUrl(jdbcUrl)));
    }

    String jdbcUrl() { return jdbcUrl; }
    String username() { return username; }
    String password() { return password; }

    /** Must run before Spring/Flyway so a populated public schema is not touched. */
    void verifyFreshDatabase() {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            try (Statement statement = connection.createStatement();
                 ResultSet identity = statement.executeQuery(
                         "SELECT current_database(), current_user, "
                                 + "(SELECT rolsuper FROM pg_catalog.pg_roles WHERE rolname = current_user), "
                                 + "(SELECT datdba = (SELECT oid FROM pg_catalog.pg_roles "
                                 + "WHERE rolname = current_user) FROM pg_catalog.pg_database "
                                 + "WHERE datname = current_database())")) {
                if (!identity.next()) throw new SQLException("Database identity is unavailable", "28000");
                try {
                    verifyConnectedIdentity(identity.getString(1), identity.getString(2),
                            databaseName, username, identity.getBoolean(3), identity.getBoolean(4));
                } catch (IllegalArgumentException mismatch) {
                    throw new SQLException("Database identity mismatch", "28000");
                }
            }

            try (Statement statement = connection.createStatement();
                 ResultSet schema = statement.executeQuery(PUBLIC_SCHEMA_INVENTORY_SQL)) {
                if (!schema.next() || !isFreshPublicSchema(schema.getBoolean(1), schema.getLong(2))) {
                    throw new SQLException("Public schema is not empty", "55000");
                }
            }
        } catch (SQLException failure) {
            throw sanitizedPreflightFailure(failure);
        }
    }

    static void verifyConnectedIdentity(String actualDatabase, String actualRole,
                                        String expectedDatabase, String expectedRole,
                                        boolean superuser, boolean ownsDatabase) {
        if (!Objects.equals(actualDatabase, expectedDatabase)
                || !Objects.equals(actualRole, expectedRole) || superuser || !ownsDatabase) {
            throw new IllegalArgumentException(
                    "Connected PostgreSQL role must be the non-superuser owner of the disposable test database");
        }
    }

    static boolean isFreshPublicSchema(boolean publicSchemaExists, long objectCount) {
        return publicSchemaExists && objectCount == 0L;
    }

    static IllegalStateException sanitizedPreflightFailure(SQLException failure) {
        String state = failure.getSQLState();
        if (state == null || !state.matches("[0-9A-Z]{5}")) state = "unknown";
        return new IllegalStateException(
                "MCP capture HTTP disposable PostgreSQL preflight failed (SQLSTATE " + state + ").");
    }

    static boolean isSafeJdbcUrl(String jdbcUrl, String username) {
        if (username == null || !isSafeJdbcTarget(jdbcUrl)) return false;
        String databaseName = databaseNameFromUrl(jdbcUrl);
        Matcher database = DATABASE_NAME.matcher(databaseName);
        Matcher role = ROLE_NAME.matcher(username);
        return database.matches() && role.matches()
                && database.group(1).equals(role.group(1));
    }

    private static boolean isSafeJdbcTarget(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith(JDBC_PREFIX)) return false;
        try {
            URI target = URI.create(jdbcUrl.substring("jdbc:".length()));
            String host = target.getHost();
            String authority = target.getRawAuthority();
            String rawPath = target.getRawPath();
            if (rawPath == null || rawPath.length() <= 1 || !rawPath.startsWith("/")) return false;
            String databaseName = rawPath.substring(1);
            Matcher database = DATABASE_NAME.matcher(databaseName);

            return "postgresql".equalsIgnoreCase(target.getScheme())
                    && authority != null && !authority.isBlank()
                    && !authority.contains("@") && !authority.contains(",")
                    && target.getRawUserInfo() == null
                    && target.getRawQuery() == null && target.getRawFragment() == null
                    && "127.0.0.1".equals(host)
                    && target.getPort() >= 1 && target.getPort() <= 65535
                    && target.getPort() != 5432
                    && database.matches();
        } catch (IllegalArgumentException malformed) {
            // URI diagnostics can echo supplied credentials or URL, so discard them.
            return false;
        }
    }

    private static String databaseNameFromUrl(String jdbcUrl) {
        URI target = URI.create(jdbcUrl.substring("jdbc:".length()));
        return target.getRawPath().substring(1);
    }

    private static String required(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing nonblank environment variable " + name);
        }
        return value;
    }

    @Override public String toString() {
        return "McpCaptureHttpPostgresTarget[jdbcUrl=<redacted>, username=<redacted>, password=<redacted>]";
    }
}
