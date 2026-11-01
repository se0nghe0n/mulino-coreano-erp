package org.mulino.verification.nativeclient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModelFixtureTestDatabaseConfigurationTest {
    private static final String PASSWORD = "probe-password-never-log-this";
    @TempDir Path temporaryDirectory;

    @Test
    void requiresExplicitDisposableMarkerAndDedicatedNonblankCredentials() {
        Map<String, String> environment = validEnvironment("localhost");

        Map<String, String> withoutMarker = new HashMap<>(environment);
        withoutMarker.remove(ModelFixtureTestDatabaseConfiguration.DISPOSABLE);
        assertRejected(withoutMarker);

        Map<String, String> falseMarker = new HashMap<>(environment);
        falseMarker.put(ModelFixtureTestDatabaseConfiguration.DISPOSABLE, "false");
        assertRejected(falseMarker);

        Map<String, String> withoutUser = new HashMap<>(environment);
        withoutUser.remove(ModelFixtureTestDatabaseConfiguration.DB_USER);
        assertRejected(withoutUser);

        Map<String, String> blankUser = new HashMap<>(environment);
        blankUser.put(ModelFixtureTestDatabaseConfiguration.DB_USER, "  ");
        assertRejected(blankUser);

        Map<String, String> wrongUser = new HashMap<>(environment);
        wrongUser.put(ModelFixtureTestDatabaseConfiguration.DB_USER, "model_fixture_0123456789abcdee");
        assertRejected(wrongUser);

        Map<String, String> withoutPassword = new HashMap<>(environment);
        withoutPassword.remove(ModelFixtureTestDatabaseConfiguration.DB_PASSWORD);
        assertRejected(withoutPassword);

        Map<String, String> blankPassword = new HashMap<>(environment);
        blankPassword.put(ModelFixtureTestDatabaseConfiguration.DB_PASSWORD, " \t ");
        assertRejected(blankPassword);

        Map<String, String> withoutUrl = new HashMap<>(environment);
        withoutUrl.remove(ModelFixtureTestDatabaseConfiguration.JDBC_URL);
        assertRejected(withoutUrl);
    }

    @Test
    void acceptsOnlyLocalhostAndNumericLoopbackForTheExactDedicatedDatabase() {
        for (String host : new String[]{"localhost", "127.0.0.1", "[::1]"}) {
            ModelFixtureTestDatabaseConfiguration configuration =
                    ModelFixtureTestDatabaseConfiguration.fromEnvironment(validEnvironment(host));
            assertNotNull(configuration);
        }

        Map<String, String> noPort = validEnvironment("localhost");
        noPort.put(ModelFixtureTestDatabaseConfiguration.JDBC_URL,
                "jdbc:postgresql://localhost/" + databaseName());
        assertRejected(noPort);

        Map<String, String> defaultPort = validEnvironment("localhost");
        defaultPort.put(ModelFixtureTestDatabaseConfiguration.JDBC_URL,
                "jdbc:postgresql://localhost:5432/" + databaseName());
        assertRejected(defaultPort);

        Map<String, String> wrongDatabase = validEnvironment("localhost");
        wrongDatabase.put(ModelFixtureTestDatabaseConfiguration.JDBC_URL, "jdbc:postgresql://localhost:55432/production");
        assertRejected(wrongDatabase);
    }

    @Test
    void rejectsRemoteMultiHostCredentialOptionAndMalformedTargets() {
        String identity = databaseName();
        String[] unsafeUrls = {
                "jdbc:postgresql://db.example.test:5432/" + identity,
                "jdbc:postgresql://192.168.1.20:5432/" + identity,
                "jdbc:postgresql://localhost.evil.test:5432/" + identity,
                "jdbc:postgresql://localhost:5432,127.0.0.1:5433/" + identity,
                "jdbc:postgresql://localhost:55432,127.0.0.1:55433/" + identity,
                "jdbc:postgresql://localhost:55432/" + identity + "?user=postgres",
                "jdbc:postgresql://localhost:55432/" + identity + "?password=" + PASSWORD,
                "jdbc:postgresql://test-user:" + PASSWORD + "@localhost:55432/" + identity,
                "jdbc:postgresql://localhost:55432",
                "jdbc:postgresql://localhost:55432/",
                "jdbc:postgresql://localhost:55432/" + identity + "#" + PASSWORD,
                "jdbc:postgresql://[::1/" + identity,
                "jdbc:postgresql://localhost:70000/" + identity,
                "jdbc:postgresql://localhost:5432/%6dulino_model_installer_test"
        };
        for (String url : unsafeUrls) {
            Map<String, String> environment = validEnvironment("localhost");
            environment.put(ModelFixtureTestDatabaseConfiguration.JDBC_URL, url);
            IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                    () -> ModelFixtureTestDatabaseConfiguration.fromEnvironment(environment),
                    "Unsafe JDBC target should be rejected without displaying its value");
            assertFalse(failure.getMessage().contains(PASSWORD));
            assertFalse(failure.getMessage().contains(url));
        }
    }

    @Test
    void redactsCredentialsFromConfigurationAndValidationErrors() {
        Map<String, String> environment = validEnvironment("localhost");
        String url = "jdbc:postgresql://user:" + PASSWORD + "@remote.example.test/" +
                databaseName();
        environment.put(ModelFixtureTestDatabaseConfiguration.JDBC_URL, url);
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> ModelFixtureTestDatabaseConfiguration.fromEnvironment(environment));

        assertFalse(failure.toString().contains(PASSWORD));
        assertFalse(failure.toString().contains(url));
        ModelFixtureTestDatabaseConfiguration safe = ModelFixtureTestDatabaseConfiguration.fromEnvironment(validEnvironment("localhost"));
        assertFalse(safe.toString().contains(PASSWORD));
        assertFalse(safe.toString().contains("localhost"));

        SQLException source = new SQLException("driver message containing " + PASSWORD, "28000",
                new SQLException("nested cause containing " + PASSWORD, "28000"));
        SQLException redacted = safe.redacted(source);
        assertFalse(redacted.toString().contains(PASSWORD));
        assertTrue(redacted.getCause() == null, "redacted exception must not retain its cause");
        assertTrue(redacted.getNextException() == null, "redacted exception must not retain its chained exception");
        assertEquals(0, redacted.getSuppressed().length);
    }

    @Test
    void migrationInventoryUsesNumericOrderAndRejectsGapsDuplicatesAndMalformedNames() throws Exception {
        Path migrations = temporaryDirectory.resolve("migrations");
        Files.createDirectories(migrations);
        for (int version = 10; version >= 1; version--) {
            Files.writeString(migrations.resolve("V" + version + "__step.sql"), "SELECT " + version + ";");
        }
        Path two = migrations.resolve("V2__step.sql");
        Path one = migrations.resolve("V1__step.sql");

        var inventory = ModelFixtureTestDatabaseConfiguration.migrationInventory(migrations);
        assertEquals("V1__step.sql", inventory.get(0).fileName());
        assertEquals("V2__step.sql", inventory.get(1).fileName());
        assertEquals("V10__step.sql", inventory.get(9).fileName());
        assertEquals(org.mulino.verification.Json.sha256(one), inventory.get(0).sha256());

        Files.delete(two);
        assertThrows(IllegalArgumentException.class,
                () -> ModelFixtureTestDatabaseConfiguration.migrationInventory(migrations));

        Path duplicate = migrations.resolve("V1__duplicate.sql");
        Files.writeString(two, "SELECT 2;");
        Files.writeString(duplicate, "SELECT 1;");
        assertThrows(IllegalArgumentException.class,
                () -> ModelFixtureTestDatabaseConfiguration.migrationInventory(migrations));

        Files.delete(duplicate);
        Files.writeString(migrations.resolve("V03-bad.sql"), "SELECT 3;");
        assertThrows(IllegalArgumentException.class,
                () -> ModelFixtureTestDatabaseConfiguration.migrationInventory(migrations));
    }

    private static void assertRejected(Map<String, String> environment) {
        assertThrows(IllegalArgumentException.class,
                () -> ModelFixtureTestDatabaseConfiguration.fromEnvironment(environment));
    }

    private static Map<String, String> validEnvironment(String host) {
        Map<String, String> environment = new HashMap<>();
        environment.put(ModelFixtureTestDatabaseConfiguration.DISPOSABLE, "true");
        environment.put(ModelFixtureTestDatabaseConfiguration.JDBC_URL, "jdbc:postgresql://" + host +
                ":55432/" + databaseName());
        environment.put(ModelFixtureTestDatabaseConfiguration.DB_USER, databaseUser());
        environment.put(ModelFixtureTestDatabaseConfiguration.DB_PASSWORD, PASSWORD);
        return environment;
    }

    private static String databaseName() {
        return "mulino_model_fixture_0123456789abcdef";
    }

    private static String databaseUser() {
        return "model_fixture_0123456789abcdef";
    }
}
