package org.mulino.verification.nativeclient;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.node.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.security.KeyPairGenerator;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mulino.verification.Json;
import org.mulino.verification.actual.ActualConfiguration;

/** Opt-in bounded actual transport probe. Owns the Docker default; an outer runner owns local PostgreSQL. */
@EnabledIfEnvironmentVariable(named="S5_AUTH_CAPTURE_ACTUAL",matches="true")
class NativeMcpTransportActualTest {
  static final String IMAGE="postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280";
  final Path root=Path.of(System.getProperty("repo.root")).toAbsolutePath().normalize();
  final Path jar=root.resolve("backend/target/ontology-0.1.0-SNAPSHOT.jar");
  final String container="mulino-auth-capture-"+UUID.randomUUID();
  Process backend;
  Path ephemeral;
  String databasePort,password,databaseUrl,databaseUser;
  int backendPort;
  ObjectNode activeBackendProof;
  ObjectNode proof=Json.object().put("evidenceClass","FOCUSED_ACTUAL_HTTP_POSTGRESQL_PROBE");
  @Test void realPackagedBackendReceiptIsConsumedAndDisabledModeFailsClosed()throws Exception {
    var externalDatabase=NativeMcpTransportPostgresTarget.fromEnvironment(System.getenv()).orElse(null);
    Path output=root.resolve("verification/harness/target/auth-capture-actual");Files.createDirectories(output);
    assertTrue(Files.isRegularFile(jar),"Build this isolated worktree's backend before the opt-in probe");
    proof.put("baselineCommit",git("rev-parse","HEAD")).put("backendJarSha256",Json.sha256(jar));
    if(externalDatabase==null)proof.put("postgresImage",IMAGE);
    proof.put("javaVersion",System.getProperty("java.version")).put("startedAt",Instant.now().toString());
    var inputs=proof.putObject("sourceInputs");
    for(String relative:List.of("backend/pom.xml","backend/package-lock.json","backend/src/main/resources/edmx/csn.json","contracts/capability-slots.json",
      "verification/harness/src/main/java/org/mulino/verification/nativeclient/NativeMcpTransport.java",
      "verification/harness/target/classes/org/mulino/verification/nativeclient/NativeMcpTransport.class"))inputs.put(relative,Json.sha256(root.resolve(relative)));
    var classes=proof.putObject("backendClassHashes");
    try(var zip=new java.util.zip.ZipFile(jar.toFile())) {
      for(String name:List.of("McpCaptureConfiguration","McpIdentitySnapshot","McpCaptureStore","McpCaptureFilter","McpCaptureController")) {
        String relative="backend/src/main/java/com/mulino/adapters/verification/"+name+".java";inputs.put(relative,Json.sha256(root.resolve(relative)));
        try(var stream=zip.getInputStream(zip.getEntry("BOOT-INF/classes/com/mulino/adapters/verification/"+name+".class"))) {
          classes.put(name,HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(stream.readAllBytes())));
        }
      }
    }
    boolean created=false;
    try {
      if(externalDatabase==null) {
        password=UUID.randomUUID().toString();databaseUser="postgres";
        docker(Map.of("POSTGRES_PASSWORD",password),"run","-d","--name",container,"-e","POSTGRES_PASSWORD","-e","POSTGRES_DB=ontology","-p","127.0.0.1::5432",IMAGE);created=true;
        databasePort=docker(Map.of(),"port",container,"5432/tcp").strip().replaceAll(".*:","");
        databaseUrl=jdbc();awaitDatabase();proof.put("databaseLifecycleOwner","actual-test-docker-container");
      } else {
        externalDatabase.verifyFreshDatabase();
        databaseUrl=externalDatabase.jdbcUrl();databaseUser=externalDatabase.username();password=externalDatabase.password();
        proof.put("databaseLifecycleOwner","outer-local-runner").put("databasePreflight","FRESH_EMPTY_PUBLIC_SCHEMA_PASS");
      }
      Path javaTemp=Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize();
      assertTrue(Files.isDirectory(javaTemp)&&Files.isWritable(javaTemp),"Runner-owned java.io.tmpdir must exist and be writable");
      ephemeral=Files.createTempDirectory(javaTemp,"mulino-auth-transport-",java.nio.file.attribute.PosixFilePermissions.asFileAttribute(java.nio.file.attribute.PosixFilePermissions.fromString("rwx------")));
      proof.put("javaTempRoot",javaTemp.toString()).put("keyBlobDirectory",ephemeral.toString());
      Path backendTemp=Files.createDirectory(ephemeral.resolve("backend-java-tmp"),java.nio.file.attribute.PosixFilePermissions.asFileAttribute(java.nio.file.attribute.PosixFilePermissions.fromString("rwx------")));
      proof.put("backendJavaTempDirectory",backendTemp.toString());
      var generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);var keys=generator.generateKeyPair();
      writeKey("public.pem","PUBLIC KEY",keys.getPublic().getEncoded());writeKey("private.pem","PRIVATE KEY",keys.getPrivate().getEncoded());
      Instant time=databaseFixtureInstant(Instant.now());
      proof.put("fixtureClock",time.toString()).put("fixtureClockPrecision","MICROSECOND");
      int port=startBackend(true,time,output.resolve("backend-enabled.log"));
      String org=UUID.randomUUID().toString(),actorId=UUID.randomUUID().toString(),owner=UUID.randomUUID().toString(),grant=UUID.randomUUID().toString();
      String issuer="https://capture-actual.invalid",audience="mulino-auth-capture-actual",alias="capture-probe-org";
      try(var db=DriverManager.getConnection(databaseUrl,databaseUser,password)) {
        try(var statement=db.createStatement();var version=statement.executeQuery("SELECT version()")){version.next();proof.put("postgresVersion",version.getString(1));}
        execute(db,"INSERT INTO mulino_identity_Organizations(ID,externalAlias,revision) VALUES(?,?,1)",org,alias);
        execute(db,"INSERT INTO mulino_identity_Actors(organizationId,ID,kind,stableRequestOwner,revision) VALUES(?,?,'AGENT',?,1)",org,actorId,owner);
        execute(db,"INSERT INTO mulino_identity_ExternalIdentities(organizationId,ID,actorId,issuer,subject,organizationAlias,revision) VALUES(?,?,?,?,?,?,1)",org,UUID.randomUUID().toString(),actorId,issuer,"actual-subject",alias);
        execute(db,"INSERT INTO mulino_identity_Memberships(organizationId,ID,actorId,validFrom,validUntil,revision) VALUES(?,?,?,?,?,1)",org,UUID.randomUUID().toString(),actorId,Timestamp.from(time.minusSeconds(60)),Timestamp.from(time.plusSeconds(3600)));
        execute(db,"INSERT INTO mulino_identity_AuthorityFences VALUES(?,?,1)",org,actorId);
        execute(db,"INSERT INTO mulino_identity_Grants(organizationId,ID,actorId,delegatorId,validFrom,validUntil,revision) VALUES(?,?,?,?,?,?,1)",org,grant,actorId,actorId,Timestamp.from(time.minusSeconds(60)),Timestamp.from(time.plusSeconds(3600)));
        execute(db,"INSERT INTO mulino_identity_GrantActions VALUES(?,?,'getAccessContext')",org,grant);
        execute(db,"INSERT INTO mulino_identity_GrantScopes VALUES(?,?,'ORGANIZATION',?)",org,grant,org);
        execute(db,"INSERT INTO mulino_identity_CapabilityAssignments(organizationId,ID,actorId,capabilityId,scopeKind,scopeId,validFrom,validUntil,revision) VALUES(?,?,?,'getAccessContext','ORGANIZATION',?,?,?,1)",org,UUID.randomUUID().toString(),actorId,org,Timestamp.from(time.minusSeconds(60)),Timestamp.from(time.plusSeconds(3600)));
        var actor=Json.object().put("issuer",issuer).put("audience",audience).put("subject","actual-subject").put("organizationAlias",alias).put("stableRequestOwner","forged-caller-owner");
        var scope=Json.object().put("organizationId",org);
        var evidence=new NativeIntegrationEvidence(root);var transport=new NativeMcpTransport(configuration(port,issuer,audience),evidence);var artifacts=proof.putArray("transportArtifacts");
        var capture=transport.send("actual-server-owner","tools/call","getAccessContext",Json.object(),actor,scope);artifacts.add(capture.artifact());
        var receipt=Json.read(root.resolve(capture.artifact().path("path").asText()));assertEquals("CAPTURED",receipt.path("authenticatedActorStatus").asText());
        assertEquals(actorId,receipt.path("serverAuthenticatedCapture").path("identity").path("actorId").asText());
        assertEquals(owner,receipt.path("serverAuthenticatedCapture").path("identity").path("stableRequestOwner").asText());
        assertEquals(owner,capture.businessResponse().path("data").path("stableRequestOwner").asText());
        assertEquals(grant,receipt.path("serverAuthenticatedCapture").path("grants").get(0).path("ID").asText());
        execute(db,"UPDATE mulino_identity_Grants SET revokedAt=?,revision=2 WHERE ID=?",Timestamp.from(time),grant);
        try(var statement=db.prepareStatement("SELECT revokedAt FROM mulino_identity_Grants WHERE organizationId=? AND ID=?")) {
          statement.setString(1,org);statement.setString(2,grant);
          try(var row=statement.executeQuery()) {
            assertTrue(row.next());assertEquals(time,row.getTimestamp(1).toInstant(),"Stored revocation must equal the fixed authority clock");assertFalse(row.next());
          }
        }
        var denied=transport.send("actual-revoked-action","tools/call","getAccessContext",Json.object(),actor,scope);artifacts.add(denied.artifact());
        assertEquals("FORBIDDEN",denied.businessResponse().path("error").path("code").asText());
        var revoked=Json.read(root.resolve(denied.artifact().path("path").asText()));assertEquals("CAPTURED",revoked.path("authenticatedActorStatus").asText());
        assertEquals(2,revoked.path("serverAuthenticatedCapture").path("grants").get(0).path("revision").asInt());
        assertEquals("NOT_ASSERTED",revoked.path("serverAuthenticatedCapture").path("actionAuthorization").asText());
        stopBackend();port=startBackend(false,time,output.resolve("backend-disabled.log"));
        var disabled=new NativeMcpTransport(configuration(port,issuer,audience),evidence);
        assertEquals("SERVER_CAPTURE_ABSENT",assertThrows(IllegalStateException.class,()->disabled.send("actual-disabled","tools/list",null,Json.object(),actor,scope)).getMessage());
        proof.put("enabledCapture","PASS").put("serverOwnerMapping","PASS").put("revokedGrantDeniedAction","PASS").put("localWithoutVerificationDisabled","PASS");
        NativeClientPolicy.rejectSecrets(proof);
      }
    } finally {
      var cleanupFailures=proof.putArray("cleanupFailures");
      try {stopBackend();} catch(Throwable failure) {cleanupFailures.add(cleanupEntry("backend-stop",failure));}
      if(created)try {docker(Map.of(),"rm","-f","-v",container);} catch(Throwable failure) {cleanupFailures.add(cleanupEntry("docker-container-remove",failure));}
      if(ephemeral!=null)try(var paths=Files.walk(ephemeral)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}
      catch(Throwable failure) {cleanupFailures.add(cleanupEntry("ephemeral-key-blob-delete",failure));}
      proof.put("backendStopped",backendInstancesStopped()).put("backendPortsClosed",backendInstancePortsClosed())
          .put("ephemeralFilesRemoved",ephemeral==null||!Files.exists(ephemeral)).put("completedAt",Instant.now().toString());
      if(externalDatabase==null)try {proof.put("containerRemoved",docker(Map.of(),"ps","-aq","--filter","name=^/"+container+"$").strip().isEmpty());}
      catch(Throwable failure) {cleanupFailures.add(cleanupEntry("docker-container-inspection",failure));}
      else proof.put("externalDatabaseCleanupOwner","outer-local-runner");
      try {Json.write(output.resolve("probe.json"),proof);} catch(Throwable failure) {cleanupFailures.add(cleanupEntry("probe-write",failure));}
      if(!cleanupFailures.isEmpty())throw new IllegalStateException("Actual probe cleanup failed; inspect sanitized cleanupFailures in probe.json");
    }
  }
  ActualConfiguration configuration(int port,String issuer,String audience) {
    return new ActualConfiguration(URI.create("http://127.0.0.1:"+port),databaseUrl,databaseUser,password,ephemeral.resolve("private.pem"),issuer,audience,"FOCUSED_ACTUAL_PROBE");
  }
  /** PostgreSQL timestamp precision must not round revocation past the frozen clock. */
  static Instant databaseFixtureInstant(Instant wallClock) {
    return wallClock.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
  }
  String jdbc(){return "jdbc:postgresql://127.0.0.1:"+databasePort+"/ontology";}
  void awaitDatabase()throws Exception {
    long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);
    while(System.nanoTime()<deadline) {
      try(var db=DriverManager.getConnection(jdbc()+"?connectTimeout=1","postgres",password)){return;}
      catch(SQLException unavailable){Thread.sleep(100);}
    }
    throw new IllegalStateException("Owned PostgreSQL startup timed out");
  }
  int startBackend(boolean enabled,Instant time,Path log)throws Exception {
    int port;try(var socket=new java.net.ServerSocket(0,0,InetAddress.getByName("127.0.0.1"))){port=socket.getLocalPort();}
    backendPort=port;
    activeBackendProof=proof.putObject(enabled?"enabledBackendInstance":"disabledBackendInstance").put("port",port);
    Path javaTemp=ephemeral.resolve("backend-java-tmp").toAbsolutePath().normalize();
    var process=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin/java").toString(),"-Djava.io.tmpdir="+javaTemp,"-jar",jar.toString(),"--spring.config.location=classpath:/","--server.address=127.0.0.1","--server.port="+port);
    process.directory(root.toFile());var env=process.environment();env.clear();
    env.putAll(NativeMcpTransportBackendEnvironment.create(System.getenv(),enabled,databaseUrl,databaseUser,password,
        ephemeral.resolve("public.pem"),"https://capture-actual.invalid","mulino-auth-capture-actual",time,ephemeral.resolve("blobs")));
    backend=process.redirectErrorStream(true).redirectOutput(log.toFile()).start();
    activeBackendProof.put("pid",backend.pid());
    var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(40);
    while(System.nanoTime()<deadline) {
      if(!backend.isAlive())throw new IllegalStateException("Packaged backend exited; inspect safe backend log");
      try {var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/mcp/ontology")).timeout(Duration.ofSeconds(1)).GET().build();
        if(client.send(request,HttpResponse.BodyHandlers.discarding()).statusCode()==401)return port;
      }catch(java.io.IOException unavailable){}Thread.sleep(100);
    }
    throw new IllegalStateException("Packaged backend startup timed out");
  }
  void stopBackend()throws Exception {
    Process stopping=backend;
    if(stopping!=null&&stopping.isAlive()) {
      stopping.destroy();if(!stopping.waitFor(10,TimeUnit.SECONDS)){stopping.destroyForcibly();assertTrue(stopping.waitFor(5,TimeUnit.SECONDS));}
    }
    boolean stopped=stopping==null||!stopping.isAlive();
    boolean portClosed=backendPort==0||awaitPortClosed(backendPort);
    proof.put("lastBackendStopped",stopped).put("lastBackendPortClosed",portClosed);
    if(activeBackendProof!=null)activeBackendProof.put("stopped",stopped).put("portClosedAfterStop",portClosed);
    if(!stopped||!portClosed)throw new IllegalStateException("Backend process or loopback port did not close");
    backend=null;backendPort=0;activeBackendProof=null;
  }
  boolean backendInstancesStopped() {
    return (backend==null||!backend.isAlive())
        && proof.has("enabledBackendInstance")&&proof.path("enabledBackendInstance").path("stopped").asBoolean(false)
        && proof.has("disabledBackendInstance")&&proof.path("disabledBackendInstance").path("stopped").asBoolean(false);
  }
  boolean backendInstancePortsClosed() {
    return proof.has("enabledBackendInstance")&&proof.path("enabledBackendInstance").path("portClosedAfterStop").asBoolean(false)
        && proof.has("disabledBackendInstance")&&proof.path("disabledBackendInstance").path("portClosedAfterStop").asBoolean(false);
  }
  boolean awaitPortClosed(int port)throws InterruptedException {
    long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
    while(System.nanoTime()<deadline) {
      try(var socket=new java.net.Socket()) {socket.connect(new InetSocketAddress("127.0.0.1",port),100);}
      catch(java.io.IOException closed){return true;}
      Thread.sleep(50);
    }
    return false;
  }
  static ObjectNode cleanupEntry(String step,Throwable failure) {
    return Json.object().put("step",step).put("failureType",failure.getClass().getSimpleName());
  }
  void writeKey(String file,String kind,byte[] value)throws Exception {
    Files.writeString(ephemeral.resolve(file),"-----BEGIN "+kind+"-----\n"+Base64.getMimeEncoder(64,new byte[]{10}).encodeToString(value)+"\n-----END "+kind+"-----\n");
    Files.setPosixFilePermissions(ephemeral.resolve(file),java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
  }
  static void execute(Connection db,String sql,Object... args)throws SQLException {
    try(var statement=db.prepareStatement(sql)){for(int i=0;i<args.length;i++)statement.setObject(i+1,args[i]);statement.executeUpdate();}
  }
  String docker(Map<String,String> env,String... args)throws Exception {
    var command=new ArrayList<>(List.of("docker"));command.addAll(List.of(args));var builder=new ProcessBuilder(command);builder.environment().putAll(env);
    var p=builder.redirectErrorStream(true).start();assertTrue(p.waitFor(30,TimeUnit.SECONDS),"Owned Docker command timed out");
    String result=new String(p.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);assertEquals(0,p.exitValue(),"Owned Docker command failed");return result;
  }
  String git(String... args)throws Exception {var command=new ArrayList<>(List.of("git"));command.addAll(List.of(args));var p=new ProcessBuilder(command).directory(root.toFile()).start();assertTrue(p.waitFor(5,TimeUnit.SECONDS));return new String(p.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).strip();}
}
