package com.mulino.runtime;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkerCommandsPostgresTargetTest {
    private static final String SUFFIX = "0123456789abcdef";
    private static final String DATABASE = "mulino_worker_commands_test_" + SUFFIX;
    private static final String USER = "worker_commands_test_" + SUFFIX;
    private static final String PASSWORD = "disposable-worker-password-never-log";

    @Test
    void absentWorkerOptInKeepsTheContainerDefaultAndIgnoresOtherDatabaseVariables() {
        assertTrue(WorkerCommandsPostgresTarget.fromEnvironment(Map.of()).isEmpty());
        assertTrue(WorkerCommandsPostgresTarget.fromEnvironment(Map.of(
                "DB_URL", "jdbc:postgresql://db.invalid:5432/production",
                "DB_USERNAME", "production-role",
                "DB_PASSWORD", PASSWORD,
                "BUSINESS_DISCOVERY_PG_TEST", "true")).isEmpty());
    }

    @Test
    void requiresBothMarkersAndEveryDedicatedCredentialWithoutPartialFallback() {
        Map<String, String> valid = validEnvironment("localhost");
        assertTrue(WorkerCommandsPostgresTarget.fromEnvironment(valid).isPresent());

        for (String key : new String[]{WorkerCommandsPostgresTarget.RUN,
                WorkerCommandsPostgresTarget.DISPOSABLE,
                WorkerCommandsPostgresTarget.JDBC_URL,
                WorkerCommandsPostgresTarget.USER,
                WorkerCommandsPostgresTarget.PASSWORD}) {
            Map<String, String> missing = new HashMap<>(valid);
            missing.remove(key);
            assertRejected(missing);
        }

        Map<String, String> falseRun = new HashMap<>(valid);
        falseRun.put(WorkerCommandsPostgresTarget.RUN, "false");
        assertRejected(falseRun);
        Map<String, String> falseDisposable = new HashMap<>(valid);
        falseDisposable.put(WorkerCommandsPostgresTarget.DISPOSABLE, "false");
        assertRejected(falseDisposable);
        Map<String, String> blankPassword = new HashMap<>(valid);
        blankPassword.put(WorkerCommandsPostgresTarget.PASSWORD, " \t ");
        assertRejected(blankPassword);
    }

    @Test
    void acceptsLoopbackOnlyWithExplicitPortAndMatchingGeneratedDatabaseRole() {
        for (String host : new String[]{"localhost", "127.0.0.1", "[::1]"}) {
            var target = WorkerCommandsPostgresTarget.fromEnvironment(validEnvironment(host)).orElseThrow();
            assertEquals(USER, target.username());
            assertEquals("WorkerCommandsPostgresTarget[jdbcUrl=<redacted>, username=<redacted>, password=<redacted>]",
                    target.toString());
            assertFalse(target.toString().contains(PASSWORD));
            assertFalse(target.toString().contains(DATABASE));
        }

        Map<String, String> wrongPrefix = validEnvironment("localhost");
        wrongPrefix.put(WorkerCommandsPostgresTarget.USER, "postgres_" + SUFFIX);
        assertRejected(wrongPrefix);
        Map<String, String> wrongSuffix = validEnvironment("localhost");
        wrongSuffix.put(WorkerCommandsPostgresTarget.USER, "worker_commands_test_0000000000000001");
        assertRejected(wrongSuffix);
        Map<String, String> wrongDatabasePrefix = validEnvironment("localhost");
        wrongDatabasePrefix.put(WorkerCommandsPostgresTarget.JDBC_URL,
                "jdbc:postgresql://localhost:55432/mulino_business_discovery_" + SUFFIX);
        assertRejected(wrongDatabasePrefix);
    }

    @Test
    void rejectsRemoteMultihostOptionCredentialAndMalformedUrlsWithoutEchoingThem() {
        String[] unsafeUrls = {
                "jdbc:postgresql://db.example.test:55432/" + DATABASE,
                "jdbc:postgresql://192.168.1.20:55432/" + DATABASE,
                "jdbc:postgresql://localhost.evil.test:55432/" + DATABASE,
                "jdbc:postgresql://localhost:5432/" + DATABASE,
                "jdbc:postgresql://localhost/" + DATABASE,
                "jdbc:postgresql://localhost:55432",
                "jdbc:postgresql://localhost:55432/",
                "jdbc:postgresql://localhost:55432,127.0.0.1:55433/" + DATABASE,
                "jdbc:postgresql://localhost:55432/" + DATABASE + "?sslmode=require",
                "jdbc:postgresql://localhost:55432/" + DATABASE + "?password=" + PASSWORD,
                "jdbc:postgresql://test-user:" + PASSWORD + "@localhost:55432/" + DATABASE,
                "jdbc:postgresql://localhost:55432/" + DATABASE + "#" + PASSWORD,
                "jdbc:postgresql://localhost:70000/" + DATABASE,
                "jdbc:postgresql://localhost:0/" + DATABASE,
                "jdbc:postgresql://localhost:55432/%6dulino_worker_commands_test_" + SUFFIX,
                "jdbc:postgresql://localhost:55432/" + DATABASE + "/other",
                "postgresql://localhost:55432/" + DATABASE
        };

        for (String url : unsafeUrls) {
            Map<String, String> environment = validEnvironment("localhost");
            environment.put(WorkerCommandsPostgresTarget.JDBC_URL, url);
            IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                    () -> WorkerCommandsPostgresTarget.fromEnvironment(environment));
            assertFalse(failure.toString().contains(PASSWORD));
            assertFalse(failure.toString().contains(url));
        }
    }

    @Test
    void checksConnectedDatabaseIdentityAndSanitizesJdbcFailures() {
        assertDoesNotThrow(() -> WorkerCommandsPostgresTarget.verifyConnectedIdentity(
                DATABASE, USER, DATABASE, USER));
        IllegalArgumentException mismatch = assertThrows(IllegalArgumentException.class,
                () -> WorkerCommandsPostgresTarget.verifyConnectedIdentity(
                        "other_database", "other_role", DATABASE, USER));
        assertFalse(mismatch.toString().contains(DATABASE));
        assertFalse(mismatch.toString().contains(USER));

        SQLException driverFailure = new SQLException("driver leaked " + PASSWORD, "28P01",
                new SQLException("nested " + DATABASE, "08001"));
        IllegalStateException sanitized = WorkerCommandsPostgresTarget.sanitizedPreflightFailure(driverFailure);
        assertTrue(sanitized.getMessage().contains("28P01"));
        assertFalse(sanitized.toString().contains(PASSWORD));
        assertFalse(sanitized.toString().contains(DATABASE));
        assertNull(sanitized.getCause());
        assertEquals(0, sanitized.getSuppressed().length);
    }

    @Test
    void freshSchemaGuardRequiresExistingPublicSchemaWithNoObjects() {
        assertTrue(WorkerCommandsPostgresTarget.isFreshPublicSchema(true, 0));
        assertFalse(WorkerCommandsPostgresTarget.isFreshPublicSchema(false, 0));
        assertFalse(WorkerCommandsPostgresTarget.isFreshPublicSchema(true, 1));
        assertFalse(WorkerCommandsPostgresTarget.isFreshPublicSchema(true, -1));
    }

    private static void assertRejected(Map<String, String> environment) {
        assertThrows(IllegalArgumentException.class,
                () -> WorkerCommandsPostgresTarget.fromEnvironment(environment));
    }

    private static Map<String, String> validEnvironment(String host) {
        Map<String, String> environment = new HashMap<>();
        environment.put(WorkerCommandsPostgresTarget.RUN, "true");
        environment.put(WorkerCommandsPostgresTarget.DISPOSABLE, "true");
        environment.put(WorkerCommandsPostgresTarget.JDBC_URL,
                "jdbc:postgresql://" + host + ":55432/" + DATABASE);
        environment.put(WorkerCommandsPostgresTarget.USER, USER);
        environment.put(WorkerCommandsPostgresTarget.PASSWORD, PASSWORD);
        return environment;
    }
}
