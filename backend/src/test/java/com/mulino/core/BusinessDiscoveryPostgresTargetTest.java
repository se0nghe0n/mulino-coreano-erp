package com.mulino.core;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BusinessDiscoveryPostgresTargetTest {
    private static final String SUFFIX = "0123456789abcdef";
    private static final String DATABASE = "mulino_business_discovery_" + SUFFIX;
    private static final String USER = "business_discovery_" + SUFFIX;
    private static final String PASSWORD = "disposable-test-password-never-log";

    @Test
    void noDiscoveryOptInKeepsTheExistingContainerDefaultAndIgnoresGenericDatabaseVariables() {
        assertTrue(BusinessDiscoveryPostgresTarget.fromEnvironment(Map.of()).isEmpty());
        assertTrue(BusinessDiscoveryPostgresTarget.fromEnvironment(Map.of(
                "DB_URL", "jdbc:postgresql://db.invalid:5432/production",
                "DB_USERNAME", "production-role",
                "DB_PASSWORD", PASSWORD)).isEmpty());
    }

    @Test
    void requiresBothSeparateMarkersAndEveryDedicatedCredential() {
        Map<String, String> valid = validEnvironment("localhost");
        assertTrue(BusinessDiscoveryPostgresTarget.fromEnvironment(valid).isPresent());

        for (String key : new String[]{BusinessDiscoveryPostgresTarget.RUN,
                BusinessDiscoveryPostgresTarget.DISPOSABLE,
                BusinessDiscoveryPostgresTarget.JDBC_URL,
                BusinessDiscoveryPostgresTarget.USER,
                BusinessDiscoveryPostgresTarget.PASSWORD}) {
            Map<String, String> missing = new HashMap<>(valid);
            missing.remove(key);
            assertRejected(missing);
        }

        Map<String, String> falseRun = new HashMap<>(valid);
        falseRun.put(BusinessDiscoveryPostgresTarget.RUN, "false");
        assertRejected(falseRun);
        Map<String, String> falseDisposable = new HashMap<>(valid);
        falseDisposable.put(BusinessDiscoveryPostgresTarget.DISPOSABLE, "false");
        assertRejected(falseDisposable);
        Map<String, String> blankPassword = new HashMap<>(valid);
        blankPassword.put(BusinessDiscoveryPostgresTarget.PASSWORD, " \t ");
        assertRejected(blankPassword);
    }

    @Test
    void acceptsOnlyLoopbackWithExplicitNonDefaultPortAndTheMatchingDisposableIdentity() {
        for (String host : new String[]{"localhost", "127.0.0.1", "[::1]"}) {
            var target = BusinessDiscoveryPostgresTarget.fromEnvironment(validEnvironment(host)).orElseThrow();
            assertEquals("business_discovery_" + SUFFIX, target.username());
            assertEquals("BusinessDiscoveryPostgresTarget[jdbcUrl=<redacted>, username=<redacted>, password=<redacted>]",
                    target.toString());
            assertFalse(target.toString().contains(PASSWORD));
            assertFalse(target.toString().contains(DATABASE));
        }

        Map<String, String> wrongUserPrefix = validEnvironment("localhost");
        wrongUserPrefix.put(BusinessDiscoveryPostgresTarget.USER, "postgres_" + SUFFIX);
        assertRejected(wrongUserPrefix);

        Map<String, String> mismatchedSuffix = validEnvironment("localhost");
        mismatchedSuffix.put(BusinessDiscoveryPostgresTarget.USER,
                "business_discovery_0000000000000001");
        assertRejected(mismatchedSuffix);
    }

    @Test
    void rejectsRemoteMultiHostOptionCredentialAndMalformedJdbcUrlsWithoutEchoingThem() {
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
                "jdbc:postgresql://localhost:55432/%6dulino_business_discovery_" + SUFFIX,
                "jdbc:postgresql://localhost:55432/" + DATABASE + "/other",
                "postgresql://localhost:55432/" + DATABASE
        };

        for (String url : unsafeUrls) {
            Map<String, String> environment = validEnvironment("localhost");
            environment.put(BusinessDiscoveryPostgresTarget.JDBC_URL, url);
            IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                    () -> BusinessDiscoveryPostgresTarget.fromEnvironment(environment));
            assertFalse(failure.toString().contains(PASSWORD));
            assertFalse(failure.toString().contains(url));
        }
    }

    @Test
    void checksTheLiveDatabaseAndRoleIdentityWithoutExposingConnectionDetails() {
        assertDoesNotThrow(() -> BusinessDiscoveryPostgresTarget.verifyConnectedIdentity(
                DATABASE, USER, DATABASE, USER));

        IllegalArgumentException mismatch = assertThrows(IllegalArgumentException.class,
                () -> BusinessDiscoveryPostgresTarget.verifyConnectedIdentity(
                        "other_database", "other_role", DATABASE, USER));
        assertFalse(mismatch.toString().contains(DATABASE));
        assertFalse(mismatch.toString().contains(USER));

        SQLException driverFailure = new SQLException("driver message leaked " + PASSWORD,
                "08001", new SQLException("nested " + DATABASE, "08001"));
        IllegalStateException sanitized = BusinessDiscoveryPostgresTarget.sanitizedPreflightFailure(driverFailure);
        assertTrue(sanitized.getMessage().contains("08001"));
        assertFalse(sanitized.toString().contains(PASSWORD));
        assertFalse(sanitized.toString().contains(DATABASE));
        assertNull(sanitized.getCause());
        assertEquals(0, sanitized.getSuppressed().length);
    }

    @Test
    void freshSchemaGuardRequiresAnExistingPublicSchemaWithNoObjects() {
        assertTrue(BusinessDiscoveryPostgresTarget.isFreshPublicSchema(true, 0));
        assertFalse(BusinessDiscoveryPostgresTarget.isFreshPublicSchema(false, 0));
        assertFalse(BusinessDiscoveryPostgresTarget.isFreshPublicSchema(true, 1));
        assertFalse(BusinessDiscoveryPostgresTarget.isFreshPublicSchema(true, -1));
    }

    private static void assertRejected(Map<String, String> environment) {
        assertThrows(IllegalArgumentException.class,
                () -> BusinessDiscoveryPostgresTarget.fromEnvironment(environment));
    }

    private static Map<String, String> validEnvironment(String host) {
        Map<String, String> environment = new HashMap<>();
        environment.put(BusinessDiscoveryPostgresTarget.RUN, "true");
        environment.put(BusinessDiscoveryPostgresTarget.DISPOSABLE, "true");
        environment.put(BusinessDiscoveryPostgresTarget.JDBC_URL,
                "jdbc:postgresql://" + host + ":55432/" + DATABASE);
        environment.put(BusinessDiscoveryPostgresTarget.USER, USER);
        environment.put(BusinessDiscoveryPostgresTarget.PASSWORD, PASSWORD);
        return environment;
    }
}
