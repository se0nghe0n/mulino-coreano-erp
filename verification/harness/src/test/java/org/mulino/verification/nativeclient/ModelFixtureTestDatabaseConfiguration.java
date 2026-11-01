package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.mulino.verification.Json;
import org.mulino.verification.actual.ActualConfiguration;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Fail-closed credentials and target guard for the focused installer PostgreSQL probe. */
final class ModelFixtureTestDatabaseConfiguration {
    static final String JDBC_URL = "MODEL_INSTALLER_TEST_JDBC_URL";
    static final String DISPOSABLE = "MODEL_INSTALLER_TEST_DISPOSABLE";
    static final String DB_USER = "MODEL_INSTALLER_TEST_DB_USER";
    static final String DB_PASSWORD = "MODEL_INSTALLER_TEST_DB_PASSWORD";
    private static final Pattern DATABASE_IDENTITY = Pattern.compile("mulino_model_fixture_([0-9a-f]{16})");

    private static final String JDBC_PREFIX = "jdbc:postgresql://";
    private static final Pattern MIGRATION_FILE = Pattern.compile("V([1-9][0-9]*)__([a-z0-9_]+)\\.sql");
    private static final Pattern POSTGRESQL_18_6 = Pattern.compile("18\\.6(?:\\s.*)?");

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final String databaseName;

    private ModelFixtureTestDatabaseConfiguration(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.databaseName = URI.create(jdbcUrl.substring("jdbc:".length())).getRawPath().substring(1);
    }

    static ModelFixtureTestDatabaseConfiguration fromEnvironment(Map<String, String> environment) {
        if (!"true".equals(environment.get(DISPOSABLE))) {
            throw new IllegalArgumentException(DISPOSABLE + "=true is required for the focused PostgreSQL probe");
        }

        String jdbcUrl = required(environment, JDBC_URL);
        String username = required(environment, DB_USER);
        String password = required(environment, DB_PASSWORD);
        if (!isSafeJdbcUrl(jdbcUrl, username)) {
            throw new IllegalArgumentException(JDBC_URL + " must target the dedicated loopback test database without URL credentials or options");
        }
        return new ModelFixtureTestDatabaseConfiguration(jdbcUrl, username, password);
    }

    ActualConfiguration actualConfiguration(Path signingKey) {
        return new ActualConfiguration(URI.create("http://127.0.0.1:1"), jdbcUrl, username, password,
                signingKey, "fixture-issuer", "fixture-audience", "focused-installer-test");
    }

    Connection openConnection() throws SQLException {
        try {
            return DriverManager.getConnection(jdbcUrl, username, password);
        } catch (SQLException failure) {
            throw redacted(failure);
        }
    }

    ObjectNode prepareFreshDatabase(Path repositoryRoot) throws SQLException, java.io.IOException {
        var migrations = migrationInventory(repositoryRoot.resolve("database/migrations"));
        try (Connection connection = openMigrationConnection()) {
            try {
                connection.setAutoCommit(false);
                try (Statement statement = connection.createStatement(); ResultSet identity = statement.executeQuery("SELECT current_database(), current_user")) {
                    if (!identity.next() || !databaseName.equals(identity.getString(1)) || !username.equals(identity.getString(2))) {
                        throw new SQLException("Focused database identity mismatch", "28000");
                    }
                }

                try (Statement statement = connection.createStatement();
                     ResultSet objects = statement.executeQuery("SELECT " +
                             "(SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE n.nspname = 'public') + " +
                             "(SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace WHERE n.nspname = 'public') + " +
                             "(SELECT count(*) FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE n.nspname = 'public')")) {
                    if (!objects.next() || objects.getLong(1) != 0L) {
                        throw new SQLException("Focused public schema contains existing objects", "55000");
                    }
                }
                String serverVersion;
                try (Statement statement = connection.createStatement(); ResultSet server = statement.executeQuery("SELECT current_setting('server_version')")) {
                    if (!server.next()) throw new SQLException("PostgreSQL version unavailable", "55000");
                    serverVersion = server.getString(1);
                }
                if (!POSTGRESQL_18_6.matcher(serverVersion).matches()) {
                    throw new SQLException("Focused installer probe requires PostgreSQL 18.6", "55000");
                }
                connection.commit();

                for (Migration migration : migrations) {
                    try {
                        connection.setAutoCommit(false);
                        try (Statement statement = connection.createStatement()) {
                            statement.execute(migration.sql());
                        }
                        connection.commit();
                    } catch (SQLException failure) {
                        try { connection.rollback(); } catch (SQLException ignored) { /* disposable DB is discarded by the runner */ }
                        throw failure;
                    }
                }

                String extensionVersion;
                try (Statement statement = connection.createStatement(); ResultSet extension = statement.executeQuery("SELECT extversion FROM pg_extension WHERE extname = 'btree_gist'")) {
                    if (!extension.next()) throw new SQLException("btree_gist extension is not installed", "55000");
                    extensionVersion = extension.getString(1);
                }
                connection.commit();

                ObjectNode result = Json.object();
                result.put("database", databaseName);
                result.put("serverVersion", serverVersion);
                result.put("btreeGistVersion", extensionVersion);
                result.put("migrationCount", migrations.size());
                var manifest = Json.array();
                for (Migration migration : migrations) {
                    manifest.add(Json.object().put("file", migration.fileName()).put("sha256", migration.sha256()));
                }
                result.set("migrations", manifest);
                return result;
            } catch (SQLException failure) {
                try { connection.rollback(); } catch (SQLException ignored) { /* disposable DB is discarded by the runner */ }
                throw redacted(failure);
            }
        } catch (SQLException failure) {
            throw redacted(failure);
        }
    }

    static java.util.List<Migration> migrationInventory(Path migrationsDirectory) throws java.io.IOException {
        TreeMap<Integer, Migration> byVersion = new TreeMap<>();
        try (var files = Files.list(migrationsDirectory)) {
            for (Path file : files.filter(Files::isRegularFile).sorted(Comparator.comparing(path -> path.getFileName().toString())).toList()) {
                String name = file.getFileName().toString();
                if (!name.startsWith("V")) continue;
                Matcher matcher = MIGRATION_FILE.matcher(name);
                if (!matcher.matches()) throw new IllegalArgumentException("Malformed database migration filename: " + name);
                int version;
                try { version = Integer.parseInt(matcher.group(1)); }
                catch (NumberFormatException invalidVersion) { throw new IllegalArgumentException("Database migration version is out of range: " + name); }
                String sql = Files.readString(file, StandardCharsets.UTF_8);
                Migration migration = new Migration(name, Json.sha256(file), sql);
                if (byVersion.putIfAbsent(version, migration) != null) {
                    throw new IllegalArgumentException("Duplicate database migration version: " + version);
                }
            }
        }
        if (byVersion.isEmpty()) throw new IllegalArgumentException("No database migrations found");
        int expected = 1;
        for (int version : byVersion.keySet()) {
            if (version != expected) throw new IllegalArgumentException("Database migration inventory is not contiguous at version " + expected);
            expected++;
        }
        return new ArrayList<>(byVersion.values());
    }

    private Connection openMigrationConnection() throws SQLException {
        try {
            Properties properties = new Properties();
            properties.setProperty("user", username);
            properties.setProperty("password", password);
            // Migration files are trusted local scripts containing multiple SQL statements.
            // The URL itself stays query-free; simple mode executes each whole file as one transaction.
            properties.setProperty("preferQueryMode", "simple");
            return DriverManager.getConnection(jdbcUrl, properties);
        } catch (SQLException failure) {
            throw redacted(failure);
        }
    }

    static record Migration(String fileName, String sha256, String sql) {}

    SQLException redacted(SQLException failure) {
        return new SQLException("Focused PostgreSQL installer probe failed; database details are redacted.",
                failure.getSQLState());
    }

    private static String required(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing nonblank environment variable " + name);
        }
        return value;
    }

    private static boolean isSafeJdbcUrl(String jdbcUrl, String username) {
        if (!jdbcUrl.startsWith(JDBC_PREFIX)) return false;
        try {
            URI target = URI.create(jdbcUrl.substring("jdbc:".length()));
            String authority = target.getRawAuthority();
            String host = target.getHost();
            String rawPath = target.getRawPath();
            if (rawPath == null || !rawPath.startsWith("/") || rawPath.length() <= 1) return false;
            String database = rawPath.substring(1);
            Matcher identity = DATABASE_IDENTITY.matcher(database);
            boolean matchingIdentity = identity.matches()
                    && ("model_fixture_" + identity.group(1)).equals(username);
            boolean loopback = "localhost".equalsIgnoreCase(host)
                    || "127.0.0.1".equals(host)
                    || "::1".equals(host)
                    || "[::1]".equals(host);
            return "postgresql".equalsIgnoreCase(target.getScheme())
                    && authority != null
                    && !authority.isBlank()
                    && !authority.contains("@")
                    && !authority.contains(",")
                    && target.getRawUserInfo() == null
                    && target.getRawQuery() == null
                    && target.getRawFragment() == null
                    && loopback
                    && target.getPort() >= 1
                    && target.getPort() <= 65535
                    && target.getPort() != 5432
                    && matchingIdentity;
        } catch (IllegalArgumentException malformed) {
            // URI parser messages include the original URL; never expose them.
            return false;
        }
    }

    @Override
    public String toString() {
        return "ModelFixtureTestDatabaseConfiguration[jdbcUrl=<redacted>, username=<redacted>, password=<redacted>]";
    }
}
