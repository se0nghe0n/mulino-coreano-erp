package org.mulino.verification.actual;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ObserverPostgresTestDatabaseConfigurationTest {
    private static final String TEST_PASSWORD = "only-a-unit-test-value";

    @Test
    void requiresSeparateTestOptInAndDisposableDatabaseApprovalBeforeDatabaseUse() {
        Map<String, String> valid = validEnvironment("localhost");

        Map<String, String> missingOptIn = new HashMap<>(valid);
        missingOptIn.remove(ObserverPostgresTestDatabaseConfiguration.OPT_IN);
        assertRejected(missingOptIn);

        Map<String, String> falseOptIn = new HashMap<>(valid);
        falseOptIn.put(ObserverPostgresTestDatabaseConfiguration.OPT_IN, "false");
        assertRejected(falseOptIn);

        Map<String, String> missingDisposableMarker = new HashMap<>(valid);
        missingDisposableMarker.remove(ObserverPostgresTestDatabaseConfiguration.DISPOSABLE);
        assertRejected(missingDisposableMarker);

        Map<String, String> falseDisposableMarker = new HashMap<>(valid);
        falseDisposableMarker.put(ObserverPostgresTestDatabaseConfiguration.DISPOSABLE, "false");
        assertRejected(falseDisposableMarker);

        Map<String, String> missingUrl = new HashMap<>(valid);
        missingUrl.remove(ObserverPostgresTestDatabaseConfiguration.JDBC_URL);
        assertRejected(missingUrl);

        Map<String, String> missingUser = new HashMap<>(valid);
        missingUser.remove(ObserverPostgresTestDatabaseConfiguration.DB_USER);
        assertRejected(missingUser);

        Map<String, String> blankUser = new HashMap<>(valid);
        blankUser.put(ObserverPostgresTestDatabaseConfiguration.DB_USER, "  ");
        assertRejected(blankUser);

        Map<String, String> missingPassword = new HashMap<>(valid);
        missingPassword.remove(ObserverPostgresTestDatabaseConfiguration.DB_PASSWORD);
        assertRejected(missingPassword);

        Map<String, String> blankPassword = new HashMap<>(valid);
        blankPassword.put(ObserverPostgresTestDatabaseConfiguration.DB_PASSWORD, " \t ");
        assertRejected(blankPassword);

        Map<String, String> mismatchedRole = new HashMap<>(valid);
        mismatchedRole.put(ObserverPostgresTestDatabaseConfiguration.DB_USER, "observer_test_fedcba9876543210");
        assertRejected(mismatchedRole);
    }

    @Test
    void acceptsOnlyLoopbackDisposableDatabaseWithMatchingTestRoleAndNonDefaultPort() {
        for (String host : new String[]{"localhost", "127.0.0.1", "[::1]"}) {
            var configuration = ObserverPostgresTestDatabaseConfiguration.fromEnvironment(validEnvironment(host));
            assertNotNull(configuration);
            assertEquals("observer_test_0123456789abcdef", configuration.username());
            assertTrue(configuration.schemaJdbcUrl("obs_0123456789abcdef0123456789abcdef")
                    .endsWith("?currentSchema=obs_0123456789abcdef0123456789abcdef"));
        }

        assertThrows(IllegalArgumentException.class, () ->
                ObserverPostgresTestDatabaseConfiguration.fromEnvironment(validEnvironment("localhost"))
                        .schemaJdbcUrl("other_schema"));

        Map<String, String> missingPort = validEnvironment("localhost");
        missingPort.put(ObserverPostgresTestDatabaseConfiguration.JDBC_URL,
                "jdbc:postgresql://localhost/mulino_observer_test_0123456789abcdef");
        assertRejected(missingPort);

        Map<String, String> defaultPort = validEnvironment("localhost");
        defaultPort.put(ObserverPostgresTestDatabaseConfiguration.JDBC_URL,
                "jdbc:postgresql://localhost:5432/mulino_observer_test_0123456789abcdef");
        assertRejected(defaultPort);
    }

    @Test
    void rejectsRemoteMultiHostCredentialsOptionsWrongDatabaseAndMalformedTargets() {
        String database = "mulino_observer_test_0123456789abcdef";
        String[] unsafeUrls = {
                "jdbc:postgresql://db.example.test:55432/" + database,
                "jdbc:postgresql://192.168.1.20:55432/" + database,
                "jdbc:postgresql://localhost.evil.test:55432/" + database,
                "jdbc:postgresql://localhost:55432,127.0.0.1:55433/" + database,
                "jdbc:postgresql://localhost:55432/" + database + "?user=postgres",
                "jdbc:postgresql://localhost:55432/" + database + "?password=" + TEST_PASSWORD,
                "jdbc:postgresql://test-user:" + TEST_PASSWORD + "@localhost:55432/" + database,
                "jdbc:postgresql://localhost:55432/mulino_production",
                "jdbc:postgresql://localhost:55432/mulino_observer_test_0123456789abcdef/other",
                "jdbc:postgresql://localhost:55432/" + database + "#" + TEST_PASSWORD,
                "jdbc:postgresql://[::1/" + database,
                "jdbc:postgresql://localhost:70000/" + database,
                "jdbc:postgresql://localhost:55432/%6dulino_observer_test_0123456789abcdef"
        };
        for (String unsafeUrl : unsafeUrls) {
            Map<String, String> environment = validEnvironment("localhost");
            environment.put(ObserverPostgresTestDatabaseConfiguration.JDBC_URL, unsafeUrl);
            IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                    () -> ObserverPostgresTestDatabaseConfiguration.fromEnvironment(environment));
            assertFalse(failure.getMessage().contains(TEST_PASSWORD));
            assertFalse(failure.getMessage().contains(unsafeUrl));
        }
    }

    @Test
    void redactsTargetAndCredentialValuesFromConfigurationAndSqlErrors() {
        var configuration = ObserverPostgresTestDatabaseConfiguration.fromEnvironment(validEnvironment("localhost"));
        assertFalse(configuration.toString().contains(TEST_PASSWORD));
        assertFalse(configuration.toString().contains("localhost"));

        SQLException source = new SQLException("driver details contain " + TEST_PASSWORD, "28000",
                new SQLException("nested details contain " + TEST_PASSWORD, "28000"));
        SQLException redacted = ObserverPostgresTestDatabaseConfiguration.redacted(source);
        assertFalse(redacted.toString().contains(TEST_PASSWORD));
        assertNull(redacted.getCause());
        assertNull(redacted.getNextException());
        assertEquals(0, redacted.getSuppressed().length);
    }

    @Test
    void baseAndSharedCaptureObserverSuitesBothDeclareTheOptInGuard() {
        assertGuarded(ScenarioObservationPostgresTest.class);
        assertGuarded(SharedCaptureObservationPostgresTest.class);
    }

    private static void assertGuarded(Class<?> testClass) {
        EnabledIfEnvironmentVariable guard = testClass.getDeclaredAnnotation(EnabledIfEnvironmentVariable.class);
        assertNotNull(guard, testClass.getSimpleName() + " must declare its own opt-in guard");
        assertEquals(ObserverPostgresTestDatabaseConfiguration.OPT_IN, guard.named());
        assertEquals("true", guard.matches());
    }

    private static void assertRejected(Map<String, String> environment) {
        assertThrows(IllegalArgumentException.class,
                () -> ObserverPostgresTestDatabaseConfiguration.fromEnvironment(environment));
    }

    private static Map<String, String> validEnvironment(String host) {
        Map<String, String> environment = new HashMap<>();
        environment.put(ObserverPostgresTestDatabaseConfiguration.OPT_IN, "true");
        environment.put(ObserverPostgresTestDatabaseConfiguration.DISPOSABLE, "true");
        environment.put(ObserverPostgresTestDatabaseConfiguration.JDBC_URL,
                "jdbc:postgresql://" + host + ":55432/mulino_observer_test_0123456789abcdef");
        environment.put(ObserverPostgresTestDatabaseConfiguration.DB_USER, "observer_test_0123456789abcdef");
        environment.put(ObserverPostgresTestDatabaseConfiguration.DB_PASSWORD, TEST_PASSWORD);
        return environment;
    }
}
