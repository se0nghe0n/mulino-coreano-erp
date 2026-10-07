package com.mulino;

import static org.junit.jupiter.api.Assertions.*;

import com.mulino.application.PlatformCommands;
import com.sap.cds.services.runtime.CdsRuntime;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("local")
class PlatformIntegrationTest {
  static final PostgreSQLContainer PG =
      new PostgreSQLContainer(
          "postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280");

  static {
    PG.start();
  }

  static final String ID = "00000000-0000-0000-0000-000000000001";

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", PG::getJdbcUrl);
    r.add("spring.datasource.username", PG::getUsername);
    r.add("spring.datasource.password", PG::getPassword);
    r.add("JWT_PUBLIC_KEY", () -> System.getenv("JWT_PUBLIC_KEY"));
    r.add("JWT_ISSUER", () -> "https://mulino.local.invalid");
    r.add("JWT_AUDIENCE", () -> "mulino-platform");
  }

  @Autowired PlatformCommands commands;
  @Autowired JdbcTemplate jdbc;
  @Autowired CdsRuntime runtime;

  void login() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new JwtAuthenticationToken(
                Jwt.withTokenValue("local-unit-context")
                    .header("alg", "RS256")
                    .subject("writer-a")
                    .claim("organizationId", "org-a")
                    .claim("stableRequestOwner", "owner-a")
                    .build()));
  }

  @BeforeEach
  void seed() {
    jdbc.execute("DROP TRIGGER IF EXISTS audit_failure ON mulino_platform_audit");
    for (String t :
        List.of("idempotency", "outbox", "audit", "restrictions", "policies", "grants", "scopes"))
      jdbc.execute("DELETE FROM mulino_platform_" + t);
    jdbc.update("INSERT INTO mulino_platform_scopes VALUES(?, 'org-a',60,0,0,false)", ID);
    jdbc.update(
        "INSERT INTO mulino_platform_grants"
            + " VALUES('00000000-0000-0000-0000-000000000011',?,'writer-a','org-a',true,0)",
        ID);
    jdbc.update(
        "INSERT INTO mulino_platform_policies"
            + " VALUES('00000000-0000-0000-0000-000000000021',?,'ALLOWED',0)",
        ID);
    login();
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  <T> T request(Callable<T> work) {
    login();
    return runtime
        .requestContext()
        .run(
            ctx -> {
              try {
                return work.call();
              } catch (RuntimeException e) {
                throw e;
              } catch (Exception e) {
                throw new IllegalStateException(e);
              }
            });
  }

  long count(String t) {
    return jdbc.queryForObject("SELECT count(*) FROM mulino_platform_" + t, Long.class);
  }

  @Test
  void cqnActionReplayAndConflict() {
    var a = request(() -> commands.reserve(ID, "20", 0, "k"));
    var b = request(() -> commands.reserve(ID, "20.0", 0, "k"));
    assertEquals(a, b);
    assertEquals(1, count("audit"));
    assertEquals(1, count("outbox"));
    assertEquals(1, count("idempotency"));
    assertThrows(
        PlatformCommands.Conflict.class, () -> request(() -> commands.reserve(ID, "21", 0, "k")));
  }

  @Test
  void auditFailureRollsBackAllWrites() {
    jdbc.execute(
        "CREATE OR REPLACE FUNCTION reject_audit() RETURNS trigger LANGUAGE plpgsql AS $$BEGIN"
            + " RAISE EXCEPTION 'audit unavailable';END$$");
    jdbc.execute(
        "CREATE TRIGGER audit_failure BEFORE INSERT ON mulino_platform_audit FOR EACH ROW EXECUTE"
            + " FUNCTION reject_audit()");
    assertThrows(
        RuntimeException.class, () -> request(() -> commands.reserve(ID, "20", 0, "audit-fails")));
    assertEquals(
        0,
        jdbc.queryForObject(
                "SELECT reserved FROM mulino_platform_scopes WHERE id=?",
                java.math.BigDecimal.class,
                ID)
            .intValueExact());
    assertEquals(0, count("audit"));
    assertEquals(0, count("outbox"));
    assertEquals(0, count("idempotency"));
  }

  @Test
  void reservationRaceHasOneEffect() throws Exception {
    var pool = Executors.newFixedThreadPool(2);
    var start = new CyclicBarrier(2);
    try {
      Callable<String> op =
          () -> {
            start.await();
            return request(
                () -> {
                  try {
                    commands.reserve(ID, "40", 0, UUID.randomUUID().toString());
                    return "ACCEPTED";
                  } catch (PlatformCommands.Conflict c) {
                    return c.getMessage();
                  }
                });
          };
      var a = pool.submit(op);
      var b = pool.submit(op);
      assertEquals(
          Set.of("ACCEPTED", "REVISION_CONFLICT"),
          Set.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS)));
      assertEquals(
          40,
          jdbc.queryForObject(
                  "SELECT reserved FROM mulino_platform_scopes WHERE id=?",
                  java.math.BigDecimal.class,
                  ID)
              .intValueExact());
      assertEquals(1, count("outbox"));
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void restrictionPhantomCommitPrecedesAction() throws Exception {
    raceFence(
        "INSERT INTO mulino_platform_restrictions VALUES('00000000-0000-0000-0000-000000000099','"
            + ID
            + "',true)",
        PlatformCommands.Conflict.class);
  }

  @Test
  void grantRevocationCommitPrecedesAction() throws Exception {
    raceFence(
        "UPDATE mulino_platform_grants SET allowed=false,revision=1 WHERE scopeId='" + ID + "'",
        org.springframework.security.access.AccessDeniedException.class);
  }

  @Test
  void policyUnknownCommitPrecedesAction() throws Exception {
    raceFence(
        "UPDATE mulino_platform_policies SET state='UNKNOWN',revision=1 WHERE scopeId='" + ID + "'",
        org.springframework.security.access.AccessDeniedException.class);
  }

  void raceFence(String sql, Class<? extends Throwable> expected) throws Exception {
    var pool = Executors.newSingleThreadExecutor();
    try (Connection c =
        DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())) {
      c.setAutoCommit(false);
      try (var st =
          c.prepareStatement("SELECT id FROM mulino_platform_scopes WHERE id=? FOR UPDATE")) {
        st.setString(1, ID);
        st.executeQuery();
      }
      CountDownLatch started = new CountDownLatch(1);
      var future =
          pool.submit(
              () -> {
                started.countDown();
                return request(() -> commands.reserve(ID, "20", 0, "fenced"));
              });
      assertTrue(started.await(5, TimeUnit.SECONDS));
      int holder;
      try (var pid = c.createStatement().executeQuery("SELECT pg_backend_pid()")) {
        pid.next();
        holder = pid.getInt(1);
      }
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
      boolean observed = false;
      while (System.nanoTime() < deadline) {
        observed =
            jdbc.queryForObject(
                    "SELECT count(*) FROM pg_stat_activity WHERE ? = ANY(pg_blocking_pids(pid)) AND"
                        + " wait_event_type='Lock' AND query LIKE '%mulino_platform_scopes%FOR"
                        + " UPDATE%'",
                    Integer.class, holder)
                > 0;
        if (observed) break;
        if (future.isDone()) fail("command completed before PostgreSQL lock wait was observed");
        Thread.sleep(20);
      }
      assertTrue(observed, "PostgreSQL waiter must be blocked by the held transaction");
      c.createStatement().execute(sql);
      c.commit();
      var error = assertThrows(ExecutionException.class, () -> future.get(10, TimeUnit.SECONDS));
      assertTrue(expected.isInstance(error.getCause()), error.toString());
      assertEquals(0, count("audit"));
      assertEquals(0, count("outbox"));
      assertEquals(0, count("idempotency"));
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void effectBeforeRestrictionPreservesHistory() {
    request(() -> commands.reserve(ID, "20", 0, "first"));
    request(
        () -> {
          commands.restrict(ID);
          return null;
        });
    assertEquals(
        20,
        jdbc.queryForObject(
                "SELECT reserved FROM mulino_platform_scopes WHERE id=?",
                java.math.BigDecimal.class,
                ID)
            .intValueExact());
    assertEquals(1, count("audit"));
    assertThrows(
        PlatformCommands.Conflict.class,
        () -> request(() -> commands.reserve(ID, "10", 1, "later")));
  }

  @Test
  void ontologyV1ToV2PreservesProgressAndDefinition() throws Exception {
    try (var c = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())) {
      c.createStatement().execute("CREATE DATABASE upgrade_proof");
    }
    String url = PG.getJdbcUrl().replace(PG.getDatabaseName(), "upgrade_proof");
    var v1 =
        Flyway.configure().dataSource(url, PG.getUsername(), PG.getPassword()).target("1").load();
    v1.migrate();
    try (var c = DriverManager.getConnection(url, PG.getUsername(), PG.getPassword())) {
      c.createStatement()
          .execute(
              "INSERT INTO mulino_platform_preserved"
                  + " VALUES('00000000-0000-0000-0000-000000000088','WAITING','definition-v1','hash-v1')");
      c.createStatement()
          .execute(
              "INSERT INTO mulino_platform_idempotency"
                  + " VALUES('00000000-0000-0000-0000-000000000077','org-a','owner-a','platform.reserve','past','hash','accepted')");
    }
    try (var c = DriverManager.getConnection(url, PG.getUsername(), PG.getPassword())) {
      c.createStatement()
          .execute(
              "INSERT INTO mulino_platform_outbox"
                  + " VALUES('00000000-0000-0000-0000-000000000076','00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000076','PENDING')");
    }
    var v2 = Flyway.configure().dataSource(url, PG.getUsername(), PG.getPassword()).load();
    v2.migrate();
    v2.validate();
    try (var c = DriverManager.getConnection(url, PG.getUsername(), PG.getPassword());
        var r =
            c.createStatement()
                .executeQuery(
                    "SELECT workState,definitionVersion,evidenceHash,upgradeMarker FROM"
                        + " mulino_platform_preserved")) {
      assertTrue(r.next());
      assertEquals("WAITING", r.getString(1));
      assertEquals("definition-v1", r.getString(2));
      assertEquals("hash-v1", r.getString(3));
      assertEquals("ontology-v2", r.getString(4));
    }
    try (var c = DriverManager.getConnection(url, PG.getUsername(), PG.getPassword());
        var r =
            c.createStatement().executeQuery("SELECT count(*) FROM mulino_platform_idempotency")) {
      r.next();
      assertEquals(1, r.getInt(1));
    }
  }

  @Test
  void scopeLockTimeoutReturnsStructuredConflictWithoutEffects() throws Exception {
    var pool = Executors.newSingleThreadExecutor();
    try (var c = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())) {
      c.setAutoCommit(false);
      try (var st =
          c.prepareStatement("SELECT id FROM mulino_platform_scopes WHERE id=? FOR UPDATE")) {
        st.setString(1, ID);
        st.executeQuery();
      }
      var command = pool.submit(() -> request(() -> commands.reserve(ID, "20", 0, "timeout")));
      var error = assertThrows(ExecutionException.class, () -> command.get(10, TimeUnit.SECONDS));
      assertInstanceOf(PlatformCommands.Conflict.class, error.getCause());
      assertEquals("LOCK_CONFLICT", error.getCause().getMessage());
      assertEquals(0, count("audit"));
      assertEquals(0, count("outbox"));
      assertEquals(0, count("idempotency"));
      c.rollback();
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void compilerColumnsMatchFlywaySchema() throws Exception {
    try (var c = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())) {
      c.createStatement().execute("CREATE SCHEMA expected_compiler");
      c.createStatement().execute("SET search_path TO expected_compiler");
      var generated = java.nio.file.Path.of("target/expected-v2-fresh.sql");
      String node = System.getenv().getOrDefault("NODE24_BIN", "node");
      var compiler =
          new ProcessBuilder(
                  node,
                  "node_modules/@sap/cds-dk/bin/cds.js",
                  "compile",
                  "db/schema.cds",
                  "--to",
                  "sql",
                  "--dialect",
                  "postgres")
              .redirectOutput(generated.toFile())
              .redirectError(ProcessBuilder.Redirect.INHERIT)
              .start();
      assertTrue(compiler.waitFor(30, TimeUnit.SECONDS));
      assertEquals(0, compiler.exitValue());
      assertEquals(
          -1,
          java.nio.file.Files.mismatch(
              generated, java.nio.file.Path.of("../verification/platform/spike/expected-v2.sql")),
          "Current pinned compiler output must match versioned expected DDL");
      c.createStatement().execute(java.nio.file.Files.readString(generated));
      var q =
          "SELECT"
              + " table_name,column_name,data_type,COALESCE(character_maximum_length,0),COALESCE(numeric_precision,0),COALESCE(numeric_scale,0)"
              + " FROM information_schema.columns WHERE table_schema=? AND table_name LIKE"
              + " 'mulino_platform_%' ORDER BY table_name,column_name";
      var a = columns(c, q, "public");
      var b = columns(c, q, "expected_compiler");
      assertEquals(b, a);
      assertTrue(
          jdbc.queryForObject(
                  "SELECT count(*) FROM pg_constraint WHERE conname IN"
                      + " ('scope_quantity','grant_scope_fk','policy_scope_fk','restriction_scope_fk','idem_owner_key')",
                  Integer.class)
              >= 5);
    }
  }

  List<String> columns(Connection c, String q, String schema) throws Exception {
    try (var s = c.prepareStatement(q)) {
      s.setString(1, schema);
      try (var r = s.executeQuery()) {
        var result = new ArrayList<String>();
        while (r.next()) {
          result.add(
              r.getString(1)
                  + "|"
                  + r.getString(2)
                  + "|"
                  + r.getString(3)
                  + "|"
                  + r.getInt(4)
                  + "|"
                  + r.getInt(5)
                  + "|"
                  + r.getInt(6));
        }
        return result;
      }
    }
  }

  @Test
  void postgresBackupRestorePreservesAtomicEffects() throws Exception {
    request(() -> commands.reserve(ID, "20", 0, "backup"));
    assertEquals(
        0,
        PG.execInContainer(
                "pg_dump",
                "-U",
                PG.getUsername(),
                "-d",
                PG.getDatabaseName(),
                "-Fc",
                "-f",
                "/tmp/platform.dump")
            .getExitCode());
    assertEquals(
        0, PG.execInContainer("createdb", "-U", PG.getUsername(), "restore_proof").getExitCode());
    assertEquals(
        0,
        PG.execInContainer(
                "pg_restore", "-U", PG.getUsername(), "-d", "restore_proof", "/tmp/platform.dump")
            .getExitCode());
    var url = PG.getJdbcUrl().replace(PG.getDatabaseName(), "restore_proof");
    try (var c = DriverManager.getConnection(url, PG.getUsername(), PG.getPassword());
        var r =
            c.createStatement()
                .executeQuery(
                    "SELECT reserved,revision,(SELECT count(*) FROM mulino_platform_audit),(SELECT"
                        + " count(*) FROM mulino_platform_outbox),(SELECT count(*) FROM"
                        + " mulino_platform_idempotency) FROM mulino_platform_scopes")) {
      assertTrue(r.next());
      assertEquals(20, r.getBigDecimal(1).intValueExact());
      assertEquals(1, r.getInt(2));
      assertEquals(1, r.getInt(3));
      assertEquals(1, r.getInt(4));
      assertEquals(1, r.getInt(5));
    }
  }
}
