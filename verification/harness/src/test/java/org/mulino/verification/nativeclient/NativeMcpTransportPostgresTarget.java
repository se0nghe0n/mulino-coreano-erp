package org.mulino.verification.nativeclient;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Guard for a loopback PostgreSQL database created and removed by an outer local runner. */
final class NativeMcpTransportPostgresTarget {
  static final String RUN = "S5_AUTH_CAPTURE_PG_RUN";
  static final String DISPOSABLE = "S5_AUTH_CAPTURE_PG_DISPOSABLE";
  static final String JDBC_URL = "S5_AUTH_CAPTURE_PG_URL";
  static final String USER = "S5_AUTH_CAPTURE_PG_USER";
  static final String PASSWORD = "S5_AUTH_CAPTURE_PG_PASSWORD";

  private static final Set<String> CONFIGURATION_KEYS = Set.of(
      RUN, DISPOSABLE, JDBC_URL, USER, PASSWORD);
  private static final Pattern TARGET = Pattern.compile(
      "jdbc:postgresql://127\\.0\\.0\\.1:([1-9][0-9]{0,4})/mulino_s5_auth_capture_test_([0-9a-f]{16})");
  private static final Pattern ROLE = Pattern.compile("s5_auth_capture_test_([0-9a-f]{16})");
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

  private NativeMcpTransportPostgresTarget(String jdbcUrl, String username,
                                           String password, String databaseName) {
    this.jdbcUrl = jdbcUrl;
    this.username = username;
    this.password = password;
    this.databaseName = databaseName;
  }

  /** Empty only when this test's complete opt-in namespace is absent. */
  static Optional<NativeMcpTransportPostgresTarget> fromEnvironment(
      Map<String, String> environment) {
    Objects.requireNonNull(environment, "environment");
    boolean configured = environment.keySet().stream()
        .anyMatch(key -> key.startsWith("S5_AUTH_CAPTURE_PG_"));
    if (!configured) return Optional.empty();

    for (String key : environment.keySet()) {
      if (key.startsWith("S5_AUTH_CAPTURE_PG_") && !CONFIGURATION_KEYS.contains(key)) {
        throw new IllegalArgumentException("Unexpected local PostgreSQL opt-in variable " + key);
      }
    }
    if (!"true".equals(environment.get(RUN))) {
      throw new IllegalArgumentException(RUN + "=true is required for the external test database");
    }
    if (!"true".equals(environment.get(DISPOSABLE))) {
      throw new IllegalArgumentException(DISPOSABLE + "=true is required for the external test database");
    }

    String jdbcUrl = required(environment, JDBC_URL);
    String username = required(environment, USER);
    String password = required(environment, PASSWORD);
    Matcher target = TARGET.matcher(jdbcUrl);
    Matcher role = ROLE.matcher(username);
    if (!target.matches()
        || !validPort(target.group(1))
        || !role.matches()
        || !target.group(2).equals(role.group(1))) {
      throw new IllegalArgumentException(JDBC_URL + " and " + USER
          + " must identify a matching generated database and role on 127.0.0.1 with an explicit non-5432 port and no URL options");
    }
    return Optional.of(new NativeMcpTransportPostgresTarget(
        jdbcUrl, username, password, "mulino_s5_auth_capture_test_" + target.group(2)));
  }

  String jdbcUrl() { return jdbcUrl; }
  String username() { return username; }
  String password() { return password; }

  /** Must run before starting the packaged backend, which may run Flyway migrations. */
  void verifyFreshDatabase() {
    Properties properties = new Properties();
    properties.setProperty("user", username);
    properties.setProperty("password", password);
    properties.setProperty("connectTimeout", "3");
    try (Connection connection = DriverManager.getConnection(jdbcUrl, properties)) {
      try (Statement statement = connection.createStatement();
           ResultSet identity = statement.executeQuery("""
               SELECT current_database(), current_user,
                      (SELECT rolsuper FROM pg_catalog.pg_roles WHERE rolname = current_user),
                      (SELECT pg_catalog.pg_get_userbyid(datdba)
                       FROM pg_catalog.pg_database WHERE datname = current_database())
               """)) {
        if (!identity.next()) throw new SQLException("Database identity is unavailable", "28000");
        try {
          verifyConnectedIdentity(identity.getString(1), identity.getString(2),
              databaseName, username, identity.getBoolean(3), identity.getString(4));
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
                                      boolean roleIsSuperuser, String databaseOwner) {
    if (!Objects.equals(actualDatabase, expectedDatabase)
        || !Objects.equals(actualRole, expectedRole)
        || !Objects.equals(databaseOwner, expectedRole)
        || roleIsSuperuser) {
      throw new IllegalArgumentException(
          "Connected PostgreSQL database and role do not match the non-superuser disposable test identity");
    }
  }

  static boolean isFreshPublicSchema(boolean publicSchemaExists, long objectCount) {
    return publicSchemaExists && objectCount == 0L;
  }

  static IllegalStateException sanitizedPreflightFailure(SQLException failure) {
    String state = failure.getSQLState();
    if (state == null || !state.matches("[0-9A-Z]{5}")) state = "unknown";
    return new IllegalStateException(
        "Native MCP disposable PostgreSQL preflight failed (SQLSTATE " + state + ").");
  }

  private static boolean validPort(String value) {
    try {
      int port = Integer.parseInt(value);
      return port >= 1 && port <= 65535 && port != 5432;
    } catch (NumberFormatException malformed) {
      return false;
    }
  }

  private static String required(Map<String, String> environment, String name) {
    String value = environment.get(name);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Missing nonblank environment variable " + name);
    }
    return value;
  }

  @Override public String toString() {
    return "NativeMcpTransportPostgresTarget[jdbcUrl=<redacted>, username=<redacted>, password=<redacted>]";
  }
}
