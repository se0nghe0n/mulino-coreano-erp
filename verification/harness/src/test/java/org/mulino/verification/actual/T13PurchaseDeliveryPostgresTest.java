package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mulino.verification.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Current authority and actual bounded IO; the receiver is explicitly synthetic. */
@EnabledIfEnvironmentVariable(named="T13_PG_TEST",matches="true")
class T13PurchaseDeliveryPostgresTest {
 final Path root=Path.of(System.getProperty("repo.root"));T13SupplierSourceFacts f;Connection db;String org,name;
 @BeforeEach void setup(TestInfo info)throws Exception {
  name=info.getTestMethod().orElseThrow().getName();install();
 }
 void install()throws Exception {
  f=new T13SupplierSourceFacts(root,ActualConfiguration.environment(System.getenv()),"supplier-accept");
  String external=UUID.randomUUID().toString();
  for(var action:f.sub.path("actions")) {
   if(action.path("id").asText().equals("dispatch"))((ObjectNode)action.path("request").path("slots")).put("externalOperationId",external);
   if(action.path("id").asText().equals("delivery-ack"))((ObjectNode)action.path("control").path("parameters")).put("externalOperationId",external);
  }
  org=f.aliases.path("ORG").asText();db=DriverManager.getConnection(f.config.jdbcUrl(),f.config.username(),f.config.password());
  // Shared test server scans all organizations. Defer older fixtures without changing their outcome.
  try(var q=db.prepareStatement("UPDATE mulino_runtime_Outbox SET nextCheckAt=? WHERE organizationId<>? AND status='PENDING'")){q.setTimestamp(1,Timestamp.from(java.time.Instant.parse("2100-01-01T00:00:00Z")));q.setString(2,org);int deferred=q.executeUpdate();f.results.put("isolated-prior-fixtures",Json.object().put("deferredPendingOutboxes",deferred));}
  f.executeThrough("dispatch");f.executeThrough("delivery-ack",Set.of("work","proposal","approval","dispatch"));
 }
 @AfterEach void cleanup()throws Exception {if(f!=null)f.save(Path.of(System.getenv("T13_TEST_EVIDENCE")).resolve(name+"-delivery.json"));if(db!=null)db.close();}
 void sql(String statement,Object...args)throws Exception {try(var q=db.prepareStatement(statement)){for(int i=0;i<args.length;i++)q.setObject(i+1,args[i]);q.executeUpdate();}}
 String value(String statement,Object...args)throws Exception {try(var q=db.prepareStatement(statement)){for(int i=0;i<args.length;i++)q.setObject(i+1,args[i]);try(var r=q.executeQuery()){assertTrue(r.next());return r.getString(1);}}}
 String outbox(){return f.results.get("dispatch").at("/response/outboxId").asText();}
 String proposal(){return f.results.get("proposal").at("/response/proposalId").asText();}
 String grant()throws Exception {return value("SELECT ID FROM mulino_identity_Grants WHERE organizationId=? AND actorId=?",org,f.aliases.path("procurement").asText());}
 String status()throws Exception {return value("SELECT status FROM mulino_runtime_Outbox WHERE organizationId=? AND ID=?",org,outbox());}
 JsonNode tick(ObjectNode barrier)throws Exception {return f.request("/__verification/runtime/scheduler/tick","POST",Json.object().put("schedulerId","t13-delivery-test"),"source",barrier);}
 int deliveries()throws Exception {String external=value("SELECT externalOperationId FROM mulino_runtime_Outbox WHERE organizationId=? AND ID=?",org,outbox());
  var r=f.request("/__verification/runtime/responder/requests?externalOperationId="+external,"GET",null,"source",null);f.results.put("receiver-requests",r);return r.at("/response/requests").size();}
 ObjectNode command(String cap,ObjectNode slots){var r=Json.object().put("intentKind","COMMAND").put("definitionVersion","definition-v1").put("capabilityId",cap).put("commandIdempotencyKey",UUID.randomUUID().toString()).put("expectedRevision",1);r.set("subjectRefs",Json.array());r.set("slots",slots);r.set("provenance",Json.object());return r;}
 ObjectNode barrier(String id,String point){return Json.object().put("barrierId",name).put("participantId",id).put("transactionId",id+"-tx").put("point",point);}
 void await(String id)throws Exception {var r=f.post("/__verification/runtime/barrier",Json.object().put("operation","waitReached").put("barrierId",name).put("participantId",id).put("timeoutSeconds",15),"source");assertEquals(200,r.path("httpStatus").asInt(),r.toString());}
 void resume(String id)throws Exception {f.post("/__verification/runtime/barrier",Json.object().put("operation","resume").put("barrierId",name).put("participantId",id),"source");}
 void revokeGrant(java.util.concurrent.atomic.AtomicInteger pid)throws Exception {
  // Real SQL revocation takes the same policy/authority fences as IdentityCommands.
  try(var c=DriverManager.getConnection(f.config.jdbcUrl(),f.config.username(),f.config.password())) {
   c.setAutoCommit(false);
   try(var q=c.createStatement();var r=q.executeQuery("SELECT pg_backend_pid()")){r.next();pid.set(r.getInt(1));}
   try(var q=c.prepareStatement("SELECT ID FROM mulino_identity_Organizations WHERE ID=? FOR UPDATE")){q.setString(1,org);q.executeQuery().close();}
   var actors=new TreeSet<String>(List.of(f.aliases.path("procurement").asText(),f.aliases.path("supervisor").asText()));
   for(String actor:actors)try(var q=c.prepareStatement("SELECT actorId FROM mulino_identity_AuthorityFences WHERE organizationId=? AND actorId=? FOR UPDATE")){q.setString(1,org);q.setString(2,actor);q.executeQuery().close();}
   try(var q=c.prepareStatement("UPDATE mulino_identity_Grants SET revokedAt=?,revision=revision+1 WHERE organizationId=? AND ID=?")){q.setTimestamp(1,Timestamp.from(java.time.Instant.parse("2026-10-07T09:00:00Z")));q.setString(2,org);q.setString(3,grant());assertEquals(1,q.executeUpdate());}
   c.commit();
  }
  assertNotNull(value("SELECT revokedAt FROM mulino_identity_Grants WHERE organizationId=? AND ID=?",org,grant()));
 }
 void awaitLock(java.util.concurrent.atomic.AtomicInteger pid)throws Exception {
  long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
  while(System.nanoTime()<end){if(pid.get()!=0&&"Lock".equals(value("SELECT coalesce(wait_event_type,'NONE') FROM pg_stat_activity WHERE pid=?",pid.get())))return;Thread.sleep(5);}
  fail("Revocation did not reach actual database lock wait");
 }
 @Test void answerWithoutImmutableEvidenceStaysUnknownAndIsNeverRedelivered()throws Exception {
  tick(null);assertEquals("UNKNOWN_EXTERNAL",status());assertEquals(1,deliveries());
  tick(null);assertEquals(1,deliveries());assertEquals("UNKNOWN_EXTERNAL",status());
  assertEquals("1",value("SELECT deliveryAttempts FROM mulino_runtime_Outbox WHERE organizationId=? AND ID=?",org,outbox()));
 }
 @Test void revokedOrExpiredOrMismatchedGrantProducesNoIO()throws Exception {
  for(String mode:List.of("revoked","expired","expired-membership","expired-approver","wrong-item","wrong-work","wrong-target")){
   if(!mode.equals("revoked")){f.save(Path.of(System.getenv("T13_TEST_EVIDENCE")).resolve(name+"-previous-"+mode+".json"));db.close();install();}
   String grant=grant();
   switch(mode){
    case "revoked"->revokeGrant(new java.util.concurrent.atomic.AtomicInteger());
    case "expired"->sql("UPDATE mulino_identity_Grants SET validUntil=? WHERE organizationId=? AND ID=?",Timestamp.from(java.time.Instant.parse("2026-10-07T09:00:00Z")),org,grant);
    case "expired-membership"->sql("UPDATE mulino_identity_Memberships SET validUntil=? WHERE organizationId=? AND actorId=?",Timestamp.from(java.time.Instant.parse("2026-10-07T09:00:00Z")),org,f.aliases.path("procurement").asText());
    case "expired-approver"->sql("UPDATE mulino_identity_Memberships SET validUntil=? WHERE organizationId=? AND actorId=?",Timestamp.from(java.time.Instant.parse("2026-10-07T09:00:00Z")),org,f.aliases.path("supervisor").asText());
    case "wrong-item"->sql("UPDATE mulino_identity_GrantScopes SET scopeId=? WHERE organizationId=? AND grantId=? AND scopeKind='ITEM' AND scopeId=?",UUID.randomUUID().toString(),org,grant,f.aliases.path("P").asText());
    default->sql("INSERT INTO mulino_identity_GrantScopes(organizationId,grantId,scopeKind,scopeId) VALUES(?,?,?,?)",org,grant,mode.equals("wrong-work")?"WORK":"TARGET",UUID.randomUUID().toString());
   }
   tick(null);assertEquals(0,deliveries(),mode);assertEquals("PENDING",status(),mode);
  }
 }
 @Test void commercialRevisionAndPolicyChangeProduceNoIO()throws Exception {
  for(String field:List.of("quantity","price","itemId","supplierId","dueAt","destinationId","endpoint","policy")){
   if(!field.equals("quantity")){f.save(Path.of(System.getenv("T13_TEST_EVIDENCE")).resolve(name+"-previous-"+field+".json"));db.close();install();}
   if(field.equals("policy")){
    var content=(ObjectNode)Json.parse(value("SELECT content FROM mulino_governance_PolicyVersions WHERE organizationId=? AND kind='COMMAND'",org));content.put("testPolicyRevision","new-current-policy");
    String text=content.toString(),id=UUID.randomUUID().toString(),hash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    sql("INSERT INTO mulino_governance_PolicyVersions(organizationId,ID,version,kind,content,contentHash,createdAt,effectiveFrom) VALUES(?,?,?,'COMMAND',?,?,?,?)",org,id,"t13-new-current-policy",text,hash,Timestamp.from(java.time.Instant.parse("2026-10-07T09:00:00Z")),Timestamp.from(java.time.Instant.parse("2026-10-07T09:00:00Z")));
    sql("UPDATE mulino_governance_ActivePolicies SET policyId=? WHERE organizationId=? AND kind='COMMAND'",id,org);
   }
   else {
    var change=Json.object();switch(field){
     case "quantity"->change.set(field,Json.object().put("value","120").put("unit","BOX"));
     case "price"->change.set(field,Json.object().put("value","2").put("currency","EUR"));
     case "itemId"->change.put(field,f.aliases.path("P-alt").asText());
     case "supplierId"->change.put(field,f.aliases.path("SUP-alt").asText());
     case "destinationId"->change.put(field,f.aliases.path("W-alt").asText());
     case "dueAt"->change.put(field,"2026-10-10T09:00:00Z");
     default->change.put(field,"RECEIVED");
    }
    var slots=Json.object().put("proposalId",proposal()).put("reason","Explicit negative revision");slots.set("changes",change);
    var r=f.call("revisePurchase",command("revisePurchase",slots),"procurement");f.results.put("revise-"+field,r);assertEquals("APPLIED",r.at("/response/outcome").asText(),r.toString());
   }
   var probe=((ObjectNode)f.results.get("dispatch").path("request")).deepCopy().put("commandIdempotencyKey",UUID.randomUUID().toString());probe.set("subjectRefs",Json.array());
   if(!field.equals("policy")){
    probe.put("expectedRevision",2);((ObjectNode)probe.path("slots")).put("proposalHash",value("SELECT proposalHash FROM mulino_trade_purchase_ProposalRevisions WHERE organizationId=? AND proposalId=? AND revision=2",org,proposal()));
   }
   var rejected=f.call("dispatchPurchaseOrder",probe,"procurement");f.results.put("approval-cause-"+field,rejected);
   assertEquals(field.equals("policy")?"APPROVAL_POLICY_CHANGED":"APPROVAL_NOT_CURRENT",rejected.at("/response/error/code").asText(),rejected.toString());
   tick(null);assertEquals(0,deliveries(),field);assertEquals("PENDING",status(),field);
  }
 }
 @Test void corruptedCommandIdentityOrPayloadCannotReachReceiver()throws Exception {
  for(String mode:List.of("command","payload","owner","actor","external","identity-owner")){
   if(!mode.equals("command")){f.save(Path.of(System.getenv("T13_TEST_EVIDENCE")).resolve(name+"-previous-"+mode+".json"));db.close();install();}
   switch(mode){
    case "command"->sql("UPDATE mulino_runtime_Outbox SET commandId=? WHERE organizationId=? AND ID=?",UUID.randomUUID().toString(),org,outbox());
    case "payload"->sql("UPDATE mulino_runtime_Outbox SET payloadJson=jsonb_set(payloadJson::jsonb,'{quantity}','\"99\"')::text WHERE organizationId=? AND ID=?",org,outbox());
    case "owner"->sql("UPDATE mulino_runtime_Outbox SET stableRequestOwner=? WHERE organizationId=? AND ID=?",UUID.randomUUID().toString(),org,outbox());
    case "identity-owner"->sql("UPDATE mulino_identity_Actors SET stableRequestOwner=? WHERE organizationId=? AND ID=?",UUID.randomUUID().toString(),org,f.aliases.path("procurement").asText());
    case "external"->sql("UPDATE mulino_runtime_Outbox SET externalOperationId=? WHERE organizationId=? AND ID=?",UUID.randomUUID().toString(),org,outbox());
    default->sql("UPDATE mulino_runtime_Outbox SET actorId=? WHERE organizationId=? AND ID=?",f.aliases.path("source").asText(),org,outbox());
   }
   tick(null);assertEquals(0,deliveries(),mode);assertEquals("PENDING",status(),mode);
  }
 }
 @Test void revocationCommittedBeforeIOFencePreventsActualSend()throws Exception {
  var pool=Executors.newSingleThreadExecutor();
  try {var send=pool.submit(()->tick(barrier("send","AFTER_INITIAL_READ_BEFORE_SCOPE_FENCE")));await("send");revokeGrant(new java.util.concurrent.atomic.AtomicInteger());resume("send");send.get(20,TimeUnit.SECONDS);assertEquals(0,deliveries());assertEquals("IN_FLIGHT",status());}
  finally{resume("send");pool.shutdownNow();}
 }
 @Test void revocationWaitsForAlreadyFencedSendAndCannotCauseDuplicate()throws Exception {
  var pool=Executors.newFixedThreadPool(2);
  try {var send=pool.submit(()->tick(barrier("send","AFTER_AUTH_SCOPE_FENCE_ACQUIRED_BEFORE_EFFECT_COMMIT")));await("send");
   var pid=new java.util.concurrent.atomic.AtomicInteger();var revoke=pool.submit(()->{revokeGrant(pid);return null;});awaitLock(pid);assertFalse(revoke.isDone());resume("send");send.get(20,TimeUnit.SECONDS);
   revoke.get(20,TimeUnit.SECONDS);assertEquals(1,deliveries());assertEquals("UNKNOWN_EXTERNAL",status());tick(null);assertEquals(1,deliveries());
  }finally{resume("send");pool.shutdownNow();}
 }

 @Test void expiredApprovalStopsDeliveryAtExactBoundary()throws Exception {
  f.post("/__verification/runtime/clock",Json.object().put("instant","2026-10-08T09:00:00Z"),"source");
  var probe=((ObjectNode)f.results.get("dispatch").path("request")).deepCopy().put("commandIdempotencyKey",UUID.randomUUID().toString());
  var rejected=f.call("dispatchPurchaseOrder",probe,"procurement");f.results.put("expired-approval",rejected);
  assertEquals("APPROVAL_EXPIRED",rejected.at("/response/error/code").asText(),rejected.toString());tick(null);assertEquals(0,deliveries());assertEquals("PENDING",status());
 }
 @Test void immutableCommandAndApprovalRejectMutation()throws Exception {
  assertThrows(SQLException.class,()->sql("UPDATE mulino_commands_CommandRecords SET canonicalHash=? WHERE organizationId=? AND ID=?","f".repeat(64),org,f.results.get("dispatch").at("/response/commandId").asText()));
  assertThrows(SQLException.class,()->sql("UPDATE mulino_trade_purchase_Approvals SET decision='REJECT' WHERE organizationId=? AND ID=?",org,f.results.get("approval").at("/response/approvalId").asText()));
  assertEquals("PENDING",status());assertEquals(0,deliveries());
 }
 @Test void expiredLeaseAfterPauseCannotPerformIOOrRedeliver()throws Exception {
  var pool=Executors.newSingleThreadExecutor();
  try {var send=pool.submit(()->tick(barrier("send","AFTER_INITIAL_READ_BEFORE_SCOPE_FENCE")));await("send");
   f.post("/__verification/runtime/clock",Json.object().put("instant","2026-10-07T09:00:06Z"),"source");resume("send");send.get(20,TimeUnit.SECONDS);
   assertEquals(0,deliveries());tick(null);assertEquals("UNKNOWN_EXTERNAL",status());assertEquals(0,deliveries());
  }finally{resume("send");pool.shutdownNow();}
 }

 @Test void outboxCannotBorrowCommittedCommandFromAnotherOrganization()throws Exception {
  String external=value("SELECT externalOperationId FROM mulino_runtime_Outbox WHERE organizationId=? AND ID=?",org,outbox());
  // Corruption probe only: production has no API to move an outbox row between organizations.
  sql("UPDATE mulino_runtime_Outbox SET organizationId=? WHERE organizationId=? AND ID=?",UUID.randomUUID().toString(),org,outbox());
  tick(null);
  var observed=f.request("/__verification/runtime/responder/requests?externalOperationId="+external,"GET",null,"source",null);f.results.put("cross-org-delivery",observed);
  assertEquals(0,observed.at("/response/requests").size());
  assertEquals("PENDING",value("SELECT status FROM mulino_runtime_Outbox WHERE ID=?",outbox()));
  assertEquals("0",value("SELECT deliveryAttempts FROM mulino_runtime_Outbox WHERE ID=?",outbox()));
 }
}
