package org.mulino.verification.nativeclient;

import java.sql.SQLException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NativeMcpTransportPostgresTargetTest {
  private static final String SUFFIX = "0123456789abcdef";
  private static final String DATABASE = "mulino_s5_auth_capture_test_" + SUFFIX;
  private static final String USER = "s5_auth_capture_test_" + SUFFIX;
  private static final String PASSWORD = "disposable-native-mcp-password-never-log";

  @Test
  void absentDedicatedNamespaceKeepsDockerDefaultAndIgnoresGenericDatabaseSettings() {
    assertTrue(NativeMcpTransportPostgresTarget.fromEnvironment(Map.of()).isEmpty());
    assertTrue(NativeMcpTransportPostgresTarget.fromEnvironment(Map.of(
        "DB_URL", "jdbc:postgresql://db.invalid:5432/production",
        "DB_USERNAME", "production-role",
        "DB_PASSWORD", PASSWORD,
        "WORKER_COMMANDS_PG_TEST", "true",
        "S5_AUTH_CAPTURE_ACTUAL", "true")).isEmpty());
  }

  @Test
  void requiresBothOptInsAndEveryDedicatedCredentialWithoutPartialFallback() {
    Map<String, String> valid = validEnvironment();
    var target = NativeMcpTransportPostgresTarget.fromEnvironment(valid).orElseThrow();
    assertEquals("jdbc:postgresql://127.0.0.1:55432/" + DATABASE, target.jdbcUrl());
    assertEquals(USER, target.username());
    assertEquals(PASSWORD, target.password());
    assertEquals("NativeMcpTransportPostgresTarget[jdbcUrl=<redacted>, username=<redacted>, password=<redacted>]",
        target.toString());
    assertFalse(target.toString().contains(PASSWORD));
    assertFalse(target.toString().contains(DATABASE));

    for (String key : new String[]{NativeMcpTransportPostgresTarget.RUN,
        NativeMcpTransportPostgresTarget.DISPOSABLE,
        NativeMcpTransportPostgresTarget.JDBC_URL,
        NativeMcpTransportPostgresTarget.USER,
        NativeMcpTransportPostgresTarget.PASSWORD}) {
      Map<String, String> missing = new HashMap<>(valid);
      missing.remove(key);
      assertRejected(missing);
    }
    Map<String, String> falseRun = new HashMap<>(valid);
    falseRun.put(NativeMcpTransportPostgresTarget.RUN, "false");
    assertRejected(falseRun);
    Map<String, String> falseDisposable = new HashMap<>(valid);
    falseDisposable.put(NativeMcpTransportPostgresTarget.DISPOSABLE, "false");
    assertRejected(falseDisposable);
    Map<String, String> blankPassword = new HashMap<>(valid);
    blankPassword.put(NativeMcpTransportPostgresTarget.PASSWORD, " \t ");
    assertRejected(blankPassword);
    Map<String, String> unknownOptIn = new HashMap<>(valid);
    unknownOptIn.put("S5_AUTH_CAPTURE_PG_HOST", "127.0.0.1");
    assertThrows(IllegalArgumentException.class,
        () -> NativeMcpTransportPostgresTarget.fromEnvironment(unknownOptIn));
  }

  @Test
  void acceptsOnlyTheExactLoopbackAndMatchingGeneratedDatabaseRoleIdentity() {
    assertTrue(NativeMcpTransportPostgresTarget.fromEnvironment(validEnvironment()).isPresent());

    Map<String, String> wrongPrefix = validEnvironment();
    wrongPrefix.put(NativeMcpTransportPostgresTarget.USER, "postgres_" + SUFFIX);
    assertRejected(wrongPrefix);
    Map<String, String> wrongSuffix = validEnvironment();
    wrongSuffix.put(NativeMcpTransportPostgresTarget.USER,
        "s5_auth_capture_test_0000000000000001");
    assertRejected(wrongSuffix);
    Map<String, String> wrongDatabasePrefix = validEnvironment();
    wrongDatabasePrefix.put(NativeMcpTransportPostgresTarget.JDBC_URL,
        "jdbc:postgresql://127.0.0.1:55432/mulino_business_discovery_" + SUFFIX);
    assertRejected(wrongDatabasePrefix);
  }

  @Test
  void rejectsRemoteDecoratedMultihostAndMalformedUrlsWithoutEchoingCredentials() {
    String[] unsafeUrls = {
        "jdbc:postgresql://db.example.test:55432/" + DATABASE,
        "jdbc:postgresql://192.168.1.20:55432/" + DATABASE,
        "jdbc:postgresql://127.0.0.2:55432/" + DATABASE,
        "jdbc:postgresql://localhost:55432/" + DATABASE,
        "jdbc:postgresql://127.0.0.1:5432/" + DATABASE,
        "jdbc:postgresql://127.0.0.1/" + DATABASE,
        "jdbc:postgresql://127.0.0.1:55432",
        "jdbc:postgresql://127.0.0.1:55432/",
        "jdbc:postgresql://127.0.0.1:55432,127.0.0.1:55433/" + DATABASE,
        "jdbc:postgresql://127.0.0.1:55432/" + DATABASE + "?sslmode=require",
        "jdbc:postgresql://127.0.0.1:55432/" + DATABASE + "?password=" + PASSWORD,
        "jdbc:postgresql://test-user:" + PASSWORD + "@127.0.0.1:55432/" + DATABASE,
        "jdbc:postgresql://127.0.0.1:55432/" + DATABASE + "#" + PASSWORD,
        "jdbc:postgresql://127.0.0.1:70000/" + DATABASE,
        "jdbc:postgresql://127.0.0.1:0/" + DATABASE,
        "jdbc:postgresql://127.0.0.1:055432/" + DATABASE,
        "jdbc:postgresql://127.0.0.1:55432/%6dulino_s5_auth_capture_test_" + SUFFIX,
        "jdbc:postgresql://127.0.0.1:55432/" + DATABASE + "/other",
        "postgresql://127.0.0.1:55432/" + DATABASE
    };

    for (String url : unsafeUrls) {
      Map<String, String> environment = validEnvironment();
      environment.put(NativeMcpTransportPostgresTarget.JDBC_URL, url);
      IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
          () -> NativeMcpTransportPostgresTarget.fromEnvironment(environment));
      assertFalse(failure.toString().contains(PASSWORD));
      assertFalse(failure.toString().contains(url));
    }
  }

  @Test
  void verifiesConnectedIdentityAndSanitizesJdbcFailures() {
    assertDoesNotThrow(() -> NativeMcpTransportPostgresTarget.verifyConnectedIdentity(
        DATABASE, USER, DATABASE, USER, false, USER));
    IllegalArgumentException mismatch = assertThrows(IllegalArgumentException.class,
        () -> NativeMcpTransportPostgresTarget.verifyConnectedIdentity(
            "other_database", "other_role", DATABASE, USER, false, USER));
    assertFalse(mismatch.toString().contains(DATABASE));
    assertFalse(mismatch.toString().contains(USER));
    assertThrows(IllegalArgumentException.class,
        () -> NativeMcpTransportPostgresTarget.verifyConnectedIdentity(
            DATABASE, USER, DATABASE, USER, true, USER));
    assertThrows(IllegalArgumentException.class,
        () -> NativeMcpTransportPostgresTarget.verifyConnectedIdentity(
            DATABASE, USER, DATABASE, USER, false, "other_owner"));

    SQLException driverFailure = new SQLException("driver leaked " + PASSWORD, "28P01",
        new SQLException("nested " + DATABASE, "08001"));
    IllegalStateException sanitized =
        NativeMcpTransportPostgresTarget.sanitizedPreflightFailure(driverFailure);
    assertTrue(sanitized.getMessage().contains("28P01"));
    assertFalse(sanitized.toString().contains(PASSWORD));
    assertFalse(sanitized.toString().contains(DATABASE));
    assertNull(sanitized.getCause());
    assertEquals(0, sanitized.getSuppressed().length);
  }

  @Test
  void freshSchemaGuardRequiresExistingPublicSchemaAndNoObjects() {
    assertTrue(NativeMcpTransportPostgresTarget.isFreshPublicSchema(true, 0));
    assertFalse(NativeMcpTransportPostgresTarget.isFreshPublicSchema(false, 0));
    assertFalse(NativeMcpTransportPostgresTarget.isFreshPublicSchema(true, 1));
    assertFalse(NativeMcpTransportPostgresTarget.isFreshPublicSchema(true, -1));
  }

  @Test
  void backendProcessEnvironmentDiscardsInheritedSpringAndJvmDatabaseOverrides() {
    String externalUrl = "jdbc:postgresql://127.0.0.1:55432/" + DATABASE;
    Map<String, String> inherited = Map.ofEntries(
        Map.entry("PATH", "/usr/bin:/bin"),
        Map.entry("HOME", "/runner-home"),
        Map.entry("TMPDIR", "/unowned/tmp"),
        Map.entry("TMP", "/unowned/tmp"),
        Map.entry("TEMP", "/unowned/tmp"),
        Map.entry("SPRING_DATASOURCE_URL", "jdbc:postgresql://remote.example/production"),
        Map.entry("SPRING_APPLICATION_JSON", "{\"spring\":{\"datasource\":{\"url\":\"jdbc:postgresql://evil/production\"}}}"),
        Map.entry("SPRING_CONFIG_LOCATION", "file:/tmp/untrusted-application.yaml"),
        Map.entry("JAVA_TOOL_OPTIONS", "-Dspring.datasource.url=jdbc:postgresql://evil/production"),
        Map.entry("JDK_JAVA_OPTIONS", "-Dspring.datasource.username=production-role"),
        Map.entry("_JAVA_OPTIONS", "-Dspring.datasource.password=production-password"),
        Map.entry("DB_URL", "jdbc:postgresql://remote.example/production"),
        Map.entry("DB_USERNAME", "production-role"),
        Map.entry("DB_PASSWORD", "production-password"),
        Map.entry("S5_AUTH_CAPTURE_PG_PASSWORD", "outer-runner-secret"));

    Map<String, String> backend = NativeMcpTransportBackendEnvironment.create(
        inherited, true, externalUrl, USER, PASSWORD, Path.of("/runner/tmp/public.pem"),
        "https://capture-actual.invalid", "mulino-auth-capture-actual",
        Instant.parse("2026-10-24T00:00:00Z"), Path.of("/runner/tmp/blobs"));

    assertEquals(externalUrl, backend.get("DB_URL"));
    assertEquals(USER, backend.get("DB_USERNAME"));
    assertEquals(PASSWORD, backend.get("DB_PASSWORD"));
    assertEquals("local,verification", backend.get("SPRING_PROFILES_ACTIVE"));
    assertEquals("/usr/bin:/bin", backend.get("PATH"));
    assertEquals("/runner/tmp", backend.get("HOME"));
    assertEquals("/runner/tmp", backend.get("TMPDIR"));
    assertEquals("/runner/tmp", backend.get("TMP"));
    assertEquals("/runner/tmp", backend.get("TEMP"));
    for (String disallowed : new String[]{"SPRING_DATASOURCE_URL", "SPRING_APPLICATION_JSON",
        "SPRING_CONFIG_LOCATION", "JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS",
        "S5_AUTH_CAPTURE_PG_PASSWORD"}) {
      assertFalse(backend.containsKey(disallowed), disallowed + " must not reach the backend process");
    }
    assertFalse(backend.get("DB_URL").contains("production"));
  }

  private static void assertRejected(Map<String, String> environment) {
    assertThrows(IllegalArgumentException.class,
        () -> NativeMcpTransportPostgresTarget.fromEnvironment(environment));
  }

  private static Map<String, String> validEnvironment() {
    Map<String, String> environment = new HashMap<>();
    environment.put(NativeMcpTransportPostgresTarget.RUN, "true");
    environment.put(NativeMcpTransportPostgresTarget.DISPOSABLE, "true");
    environment.put(NativeMcpTransportPostgresTarget.JDBC_URL,
        "jdbc:postgresql://127.0.0.1:55432/" + DATABASE);
    environment.put(NativeMcpTransportPostgresTarget.USER, USER);
    environment.put(NativeMcpTransportPostgresTarget.PASSWORD, PASSWORD);
    return environment;
  }
}
