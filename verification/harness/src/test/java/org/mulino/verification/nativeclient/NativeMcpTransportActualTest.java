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

/** Opt-in bounded actual transport probe. Owns its packaged backend, database and ephemeral keys; no model calls. */
@EnabledIfEnvironmentVariable(named="S5_AUTH_CAPTURE_ACTUAL",matches="true")
class NativeMcpTransportActualTest {
  static final String IMAGE="postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280";
  final Path root=Path.of(System.getProperty("repo.root")).toAbsolutePath().normalize();
  final Path jar=root.resolve("backend/target/ontology-0.1.0-SNAPSHOT.jar");
  final String container="mulino-auth-capture-"+UUID.randomUUID();
  Process backend;
  Path ephemeral;
  String databasePort,password;
  ObjectNode proof=Json.object().put("evidenceClass","FOCUSED_ACTUAL_HTTP_POSTGRESQL_PROBE");
  @Test void realPackagedBackendReceiptIsConsumedAndDisabledModeFailsClosed()throws Exception {
    Path output=root.resolve("verification/harness/target/auth-capture-actual");Files.createDirectories(output);
    assertTrue(Files.isRegularFile(jar),"Build this isolated worktree's backend before the opt-in probe");
    proof.put("baselineCommit",git("rev-parse","HEAD")).put("backendJarSha256",Json.sha256(jar)).put("postgresImage",IMAGE);
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
    ephemeral=Files.createTempDirectory("mulino-auth-transport-",java.nio.file.attribute.PosixFilePermissions.asFileAttribute(java.nio.file.attribute.PosixFilePermissions.fromString("rwx------")));
    password=UUID.randomUUID().toString();boolean created=false;
    try {
      var generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);var keys=generator.generateKeyPair();
      writeKey("public.pem","PUBLIC KEY",keys.getPublic().getEncoded());writeKey("private.pem","PRIVATE KEY",keys.getPrivate().getEncoded());
      docker(Map.of("POSTGRES_PASSWORD",password),"run","-d","--name",container,"-e","POSTGRES_PASSWORD","-e","POSTGRES_DB=ontology","-p","127.0.0.1::5432",IMAGE);created=true;
      databasePort=docker(Map.of(),"port",container,"5432/tcp").strip().replaceAll(".*:","");
      awaitDatabase();
      Instant time=Instant.now();int port=startBackend(true,time,output.resolve("backend-enabled.log"));
      String org=UUID.randomUUID().toString(),actorId=UUID.randomUUID().toString(),owner=UUID.randomUUID().toString(),grant=UUID.randomUUID().toString();
      String issuer="https://capture-actual.invalid",audience="mulino-auth-capture-actual",alias="capture-probe-org";
      try(var db=DriverManager.getConnection(jdbc(),"postgres",password)) {
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
      stopBackend();
      if(created)docker(Map.of(),"rm","-f","-v",container);
      if(ephemeral!=null)try(var paths=Files.walk(ephemeral)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}
      proof.put("backendStopped",backend==null||!backend.isAlive()).put("ephemeralFilesRemoved",!Files.exists(ephemeral)).put("completedAt",Instant.now().toString());
      proof.put("containerRemoved",docker(Map.of(),"ps","-aq","--filter","name=^/"+container+"$").strip().isEmpty());
      Json.write(output.resolve("probe.json"),proof);
    }
  }
  ActualConfiguration configuration(int port,String issuer,String audience) {
    return new ActualConfiguration(URI.create("http://127.0.0.1:"+port),jdbc(),"postgres",password,ephemeral.resolve("private.pem"),issuer,audience,"FOCUSED_ACTUAL_PROBE");
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
    var process=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin/java").toString(),"-jar",jar.toString(),"--server.port="+port);
    process.directory(root.toFile());var env=process.environment();
    env.put("SPRING_PROFILES_ACTIVE",enabled?"local,verification":"local");env.put("DB_URL",jdbc());env.put("DB_USERNAME","postgres");env.put("DB_PASSWORD",password);
    env.put("JWT_PUBLIC_KEY",ephemeral.resolve("public.pem").toString());env.put("JWT_ISSUER","https://capture-actual.invalid");env.put("JWT_AUDIENCE","mulino-auth-capture-actual");
    env.put("MULINO_VERIFICATION_MCP_CAPTURE_ENABLED","true");env.put("MULINO_VERIFICATION_INSTANT",time.toString());env.put("MULINO_EVIDENCE_BLOB_ROOT",ephemeral.resolve("blobs").toString());
    backend=process.redirectErrorStream(true).redirectOutput(log.toFile()).start();
    var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(40);
    while(System.nanoTime()<deadline) {
      if(!backend.isAlive())throw new IllegalStateException("Packaged backend exited; inspect safe backend log");
      try {var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/mcp/ontology")).timeout(Duration.ofSeconds(1)).GET().build();
        if(client.send(request,HttpResponse.BodyHandlers.discarding()).statusCode()==401)return port;
      }catch(java.io.IOException unavailable){}Thread.sleep(100);
    }
    throw new IllegalStateException("Packaged backend startup timed out");
  }
  void stopBackend()throws Exception {if(backend!=null&&backend.isAlive()){backend.destroy();if(!backend.waitFor(10,TimeUnit.SECONDS)){backend.destroyForcibly();assertTrue(backend.waitFor(5,TimeUnit.SECONDS));}}}
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
