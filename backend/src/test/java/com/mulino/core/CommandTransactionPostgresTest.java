package com.mulino.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.mulino.application.core.*;
import java.nio.file.*;
import java.sql.Timestamp;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Own host, composed stateless fixture authoring, real identity/policy/effects/PG. */
@SpringBootTest @ActiveProfiles("local") @SuppressWarnings("unchecked")
class CommandTransactionPostgresTest {
 private static final WorkPostgresTestResources HOST=new WorkPostgresTestResources("command-transaction");
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r){HOST.properties(r);}
 @Autowired AutowireCapableBeanFactory factory;
 @Autowired PlatformTransactionManager manager;
 @MockitoBean ExecutionClock clock;
 @MockitoBean CommandFaults faults;
 RegulatoryConflictPostgresTest.Fixture f;
 String first,second;

 @BeforeEach void setup(TestInfo info)throws Exception{
  // Initializing the static nested authoring class does not initialize its enclosing PG host.
  f=new RegulatoryConflictPostgresTest.Fixture();factory.autowireBean(f);
  f.jdbc.execute("TRUNCATE mulino_identity_Organizations CASCADE");f.seed(info);f.saleFixture();
  regulatory("100");for(String category:List.of("QC","CUSTOMER","COMMERCIAL"))f.basis(category,"100");
  var split=f.run("splitQuantity",f.map("segmentId",f.segment,"quantities",List.of("50","50"),"unit","EA","occurredAt",f.now.toString(),"evidenceRef","Independent synthetic partition"),0);
  var children=(List<String>)f.effects(split).get("segmentIds");first=children.getFirst();second=children.getLast();f.segment=first;
 }
 @AfterAll static void stop()throws Exception{HOST.close();}
 @AfterEach void evidence(TestInfo info)throws Exception{
  try{
   String path=System.getProperty("command.transaction.evidence");if(path!=null){
    var proof=new LinkedHashMap<String,Object>();proof.put("containerId",HOST.pg.getContainerId());proof.put("publicKeyPath",HOST.publicKeyPath.toString());proof.put("blobRoot",HOST.blobRoot.toString());
    for(String table:List.of("mulino_inventory_SegmentAllocations","mulino_inventory_QuantityMovements","mulino_commands_CommandRecords","mulino_commands_CommandAudits"))proof.put(table,f.jdbc.queryForList("SELECT row_to_json(r)::text, xmin::text AS tuple_xmin FROM "+table+" r ORDER BY organizationId,ID"));
    Files.createDirectories(Path.of(path));Files.writeString(Path.of(path,info.getTestMethod().orElseThrow().getName()+".json"),f.json.writerWithDefaultPrettyPrinter().writeValueAsString(proof));
   }
  }finally{SecurityContextHolder.clearContext();}
 }
 Map<String,Object> reserve(String segment){return f.envelope("reserveQuantity",f.map("segmentId",segment,"salesLineId",f.salesLine,"startQuantity","0","quantity","20","unit","EA"),0);}
 Map<String,Object> applied(Map<String,Object> intent){var r=f.apply(intent);assertEquals("APPLIED",r.get("outcome"),r.toString());return (Map<String,Object>)TransportValues.normalize(r);}
 String allocation(Map<String,Object> r){return f.effects(r).get("allocationId").toString();}
 String xmin(String table,String id){return f.jdbc.queryForObject("SELECT xmin::text FROM "+table+" WHERE organizationId=? AND ID=?",String.class,f.org,id);}
 Map<String,Object> row(String id){return f.jdbc.queryForMap("SELECT *,xmin::text AS tuple_xmin FROM mulino_inventory_SegmentAllocations WHERE organizationId=? AND ID=?",f.org,id);}
 String assertTransaction(Map<String,Object> r,String... allocations)throws Exception{
  assertInstanceOf(String.class,r.get("transactionId"));String xid=r.get("transactionId").toString();assertTrue(Long.parseLong(xid)>=3);assertEquals(xid,xmin("mulino_commands_CommandRecords",r.get("commandId").toString()));
  assertEquals(TransportValues.normalize(r),f.json.readValue(f.jdbc.queryForObject("SELECT resultJson FROM mulino_commands_CommandRecords WHERE organizationId=? AND ID=?",String.class,f.org,r.get("commandId")),Map.class));
  for(String id:allocations)assertEquals(xid,xmin("mulino_inventory_SegmentAllocations",id));return xid;
 }
 String replacementProof()throws Exception{
  String bytes=f.json.writeValueAsString(f.map("witnessKind","replacement-proof","reason","Replace current allocation"));
  return f.run("attachEvidence",f.map("subject",f.map("kind","SEGMENT","id",second),"sourceNamespace","quality-source","sourceReference",f.id(),"mediaType","application/json","expectedHash",com.mulino.adapters.blob.LocalBlobStore.hash(bytes.getBytes()),"provenance","SYNTHETIC","contentBase64",Base64.getEncoder().encodeToString(bytes.getBytes())),null).get("id").toString();
 }
 @Test void reserveReplaceReleaseAndReplayRetainActualTupleTransactions()throws Exception{
  var input=reserve(first);var reserved=applied(input);String old=allocation(reserved),reserveXid=assertTransaction(reserved,old);
  var originalCommand=f.jdbc.queryForMap("SELECT *,xmin::text AS tuple_xmin FROM mulino_commands_CommandRecords WHERE ID=?",reserved.get("commandId"));
  var replace=f.envelope("replaceAllocation",f.map("allocationId",old,"segmentId",second,"startQuantity","0","quantity","20","unit","EA","evidenceId",replacementProof()),0);
  var replaced=applied(replace);String next=allocation(replaced),replaceXid=assertTransaction(replaced,old,next);assertNotEquals(reserveXid,replaceXid);
  assertEquals("REPLACED",row(old).get("state"));assertEquals("EXECUTABLE",row(next).get("state"));assertEquals(reserved.get("commandId"),row(old).get("commandid"));assertEquals(replaced.get("commandId"),row(next).get("commandid"));
  var oldAfter=row(old);var nextAfter=row(next);assertEquals(replaced,applied(replace));assertEquals(oldAfter,row(old));assertEquals(nextAfter,row(next));
  var release=f.envelope("releaseAllocation",f.map("allocationId",next,"reason","Explicit synthetic cancellation"),0);var released=applied(release);String releaseXid=assertTransaction(released,next);assertNotEquals(replaceXid,releaseXid);assertEquals(replaceXid,xmin("mulino_inventory_SegmentAllocations",old));
  assertEquals("RELEASED",row(next).get("state"));assertEquals(replaced.get("commandId"),row(next).get("commandid"));var frozen=row(next);assertEquals(released,applied(release));assertEquals(frozen,row(next));assertEquals(reserved,applied(input));
  assertEquals(originalCommand,f.jdbc.queryForMap("SELECT *,xmin::text AS tuple_xmin FROM mulino_commands_CommandRecords WHERE ID=?",reserved.get("commandId")));assertEquals(2,f.jdbc.queryForObject("SELECT count(*) FROM mulino_inventory_SegmentAllocations",Integer.class));
  assertThrows(org.springframework.dao.DataAccessException.class,()->f.jdbc.update("UPDATE mulino_commands_CommandRecords SET resultJson='{}' WHERE ID=?",released.get("commandId")));
 }
 @Test void denialAndRevokedReplayHaveNoEffectTransaction()throws Exception{
  var input=reserve(first);var result=applied(input);assertTransaction(result,allocation(result));var frozen=row(allocation(result));
  var denied=f.apply(reserve(first));assertTrue(CommandOutcomes.NOT_APPLIED.contains(denied.get("outcome")));assertFalse(denied.containsKey("transactionId"));
  f.jdbc.update("UPDATE mulino_identity_CapabilityAssignments SET revokedAt=? WHERE organizationId=? AND capabilityId='reserveQuantity'",Timestamp.from(f.now),f.org);
  var replay=f.apply(input);assertEquals("REJECTED",replay.get("outcome"));assertEquals("FORBIDDEN",((Map<?,?>)replay.get("error")).get("code"));assertFalse(replay.containsKey("transactionId"));assertEquals(frozen,row(allocation(result)));
  assertEquals(0,f.jdbc.queryForObject("SELECT count(*) FROM mulino_commands_CommandRecords WHERE state='REJECTED' AND resultJson::jsonb ? 'transactionId'",Integer.class));
 }
 @Test void failureAfterEffectRollsBackRowsAndTransactionResult(){
  var input=reserve(first);int audits=f.jdbc.queryForObject("SELECT count(*) FROM mulino_commands_CommandAudits",Integer.class);
  doThrow(new IllegalStateException("Synthetic command rollback after allocation")).when(faults).check(eq(f.org),eq("reserveQuantity"),eq(input.get("commandIdempotencyKey").toString()),any(String[].class));
  assertThrows(IllegalStateException.class,()->f.apply(input));assertEquals(0,f.jdbc.queryForObject("SELECT count(*) FROM mulino_inventory_SegmentAllocations",Integer.class));assertEquals(0,f.jdbc.queryForObject("SELECT count(*) FROM mulino_commands_CommandRecords WHERE commandIdempotencyKey=?",Integer.class,input.get("commandIdempotencyKey")));assertEquals(audits,f.jdbc.queryForObject("SELECT count(*) FROM mulino_commands_CommandAudits",Integer.class));
 }
 @Test void foreignOrganizationCannotReplayOrReceiveOwnedTransaction(TestInfo info)throws Exception{
  var input=reserve(first);var result=applied(input);var frozen=row(allocation(result));
  var other=new RegulatoryConflictPostgresTest.Fixture();factory.autowireBean(other);other.seed(info);
  var denied=other.apply(input);assertEquals("REJECTED",denied.get("outcome"));assertEquals("FORBIDDEN",((Map<?,?>)denied.get("error")).get("code"));assertFalse(denied.containsKey("transactionId"));assertFalse(denied.containsKey("commandId"));assertEquals(frozen,row(allocation(result)));assertEquals(1,f.jdbc.queryForObject("SELECT count(*) FROM mulino_inventory_SegmentAllocations",Integer.class));
 }
 @Test void multipleCommandsInOneChangeSetShareXidButNotCommandIdentity()throws Exception{
  var results=new TransactionTemplate(manager).execute(tx->f.runtime.requestContext().run(ctx->{var a=f.gateway.executeInChangeSet(reserve(first),r->{throw new AssertionError("Unexpected denial");});var b=f.gateway.executeInChangeSet(reserve(second),r->{throw new AssertionError("Unexpected denial");});assertEquals(f.jdbc.queryForObject("SELECT pg_current_xact_id()::xid::text",String.class),a.get("transactionId"));return List.of(a,b);}));
  String a=assertTransaction(results.getFirst(),allocation(results.getFirst())),b=assertTransaction(results.getLast(),allocation(results.getLast()));assertEquals(a,b);assertNotEquals(results.getFirst().get("commandId"),results.getLast().get("commandId"));
 }
 @Test void callerChangesetRollbackDoesNotPersistOrReturnACommittedResult(){
  var input=reserve(first);assertThrows(IllegalStateException.class,()->new TransactionTemplate(manager).executeWithoutResult(tx->f.runtime.requestContext().run((java.util.function.Consumer<com.sap.cds.services.request.RequestContext>)ctx->{var tentative=f.gateway.executeInChangeSet(input,r->{throw new AssertionError("Unexpected denial");});assertEquals("APPLIED",tentative.get("outcome"));throw new IllegalStateException("Synthetic outer changeset rollback");})));
  assertEquals(0,f.jdbc.queryForObject("SELECT count(*) FROM mulino_inventory_SegmentAllocations",Integer.class));assertEquals(0,f.jdbc.queryForObject("SELECT count(*) FROM mulino_commands_CommandRecords WHERE commandIdempotencyKey=?",Integer.class,input.get("commandIdempotencyKey")));
 }
 void regulatory(String q){String preparation=f.evidence(f.map("purpose","fixture procedure"),"REG_PREPARATION");var prep=f.run("prepareRegulatoryProcedure",f.map("itemId",f.item,"lotId",f.lot,"physicalScopeId",f.segment,"workId",f.work,"policyId",f.regPolicy,"documentVersionId",preparation),null);String proc=f.effects(prep).get("procedureId").toString();var payload=f.map("procedureId",proc,"procedureVersionId",f.effects(prep).get("procedureVersionId"),"occurredAt",f.now.toString(),"itemId",f.item,"lotId",f.lot,"physicalScopeId",f.segment,"authority","FICTIONAL_TEST_AUTHORITY","policyVersion","fixture-v1");String submit=f.evidence(payload,"REGULATORY_SUBMISSION");var submitted=f.run("recordSubmission",f.map("procedureId",proc,"occurredAt",f.now.toString(),"occurrenceId",f.occurrence(submit)),1);var decision=f.map("procedureId",proc,"procedureVersionId",f.effects(submitted).get("procedureVersionId"),"decision","ALLOWED","action","SELL","startQuantity","0","quantity",q,"unit","EA","validFrom",f.now.toString(),"validUntil",f.now.plusSeconds(1000).toString(),"itemId",f.item,"lotId",f.lot,"physicalScopeId",f.segment,"authority","FICTIONAL_TEST_AUTHORITY","policyVersion","fixture-v1");String doc=f.evidence(decision,"REGULATORY_DECISION");for(String key:List.of("procedureVersionId","itemId","lotId","physicalScopeId","authority","policyVersion","regulatoryEventId"))decision.remove(key);decision.put("occurrenceId",f.occurrence(doc));f.run("recordRegulatoryDecision",decision,2);}
}
