package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mulino.verification.Json;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real HTTP/Spring/PostgreSQL probe. No model-client acceptance claim. */
@EnabledIfEnvironmentVariable(named="T13_PG_TEST",matches="true")
class T13SupplierSourceFactsPostgresTest {
 final Path root=Path.of(System.getProperty("repo.root"));T13SupplierSourceFacts f;Connection db;String org,name;
 void install(String sid)throws Exception {f=new T13SupplierSourceFacts(root,ActualConfiguration.environment(System.getenv()),sid);org=f.aliases.path("ORG").asText();db=DriverManager.getConnection(f.config.jdbcUrl(),f.config.username(),f.config.password());}
 @BeforeEach void name(TestInfo info){name=info.getTestMethod().orElseThrow().getName();}
 @AfterEach void close()throws Exception {if(f!=null)f.save(Path.of(System.getenv("T13_TEST_EVIDENCE")).resolve(name+"-http.json"));if(db!=null)db.close();}
 List<Map<String,Object>> rows(String table)throws Exception {try(var s=db.prepareStatement("SELECT * FROM "+table+" WHERE organizationId=?")){s.setString(1,org);try(var r=s.executeQuery()){var out=new ArrayList<Map<String,Object>>();while(r.next()){var row=new LinkedHashMap<String,Object>();for(int i=1;i<=r.getMetaData().getColumnCount();i++)row.put(r.getMetaData().getColumnLabel(i),r.getObject(i));out.add(row);}return out;}}}
 void zeroPhysical()throws Exception {assertTrue(rows("mulino_inventory_QuantitySegments").isEmpty());assertTrue(rows("mulino_trade_purchase_ReceiptCredits").isEmpty());}
 void positive(String sid,String reply,String quantity,String status)throws Exception {
  install(sid);f.executeThrough("supplier-reply");zeroPhysical();
  var commitments=rows("mulino_trade_purchase_SupplierReplies");assertEquals(1,commitments.size());var c=commitments.getFirst();assertEquals(reply,c.get("reply"));assertEquals(0,new java.math.BigDecimal(quantity).compareTo((java.math.BigDecimal)c.get("proposedquantity")));assertEquals("BOX",c.get("unit"));assertEquals(f.results.get("dispatch").at("/response/orderId").asText(),c.get("orderid"));
  var orders=rows("mulino_trade_purchase_OrderLines");assertEquals(1,orders.size());assertEquals(status,orders.getFirst().get("status"));assertEquals(0,new java.math.BigDecimal("100").compareTo((java.math.BigDecimal)orders.getFirst().get("quantity")));
  var out=rows("mulino_runtime_Outbox");assertEquals(1,out.size());assertEquals("SUCCEEDED",out.getFirst().get("status"));assertEquals(f.results.get("dispatch").at("/response/outboxId").asText(),out.getFirst().get("id"));
  assertEquals(2,rows("mulino_evidence_CanonicalOccurrences").size());assertEquals(2,rows("mulino_evidence_Verifications").size());
  long reviews=rows("mulino_work_read_ObligationReferences").stream().filter(x->"SUPPLIER_REPLY_REVIEW".equals(x.get("kind"))).count();assertEquals(reply.equals("ACCEPT")?0:1,reviews);
  JsonNode replay=f.call("recordSupplierReply",f.replyRequest(),"source");assertEquals("APPLIED",replay.at("/response/outcome").asText(),replay.toString());assertEquals(1,rows("mulino_trade_purchase_SupplierReplies").size());
  var duplicate=f.replyRequest().put("commandIdempotencyKey",UUID.randomUUID().toString()).put("expectedRevision",2);var denied=f.call("recordSupplierReply",duplicate,"source");assertEquals("SUPPLIER_REPLY_ALREADY_RECORDED",denied.at("/response/error/code").asText(),denied.toString());assertEquals(1,rows("mulino_trade_purchase_SupplierReplies").size());zeroPhysical();
 }
 @Test void accepts100WithoutArrival()throws Exception{positive("supplier-accept","ACCEPT","100","SUPPLIER_ACCEPTED");}
 @Test void rejects100WithResponsibility()throws Exception{positive("supplier-reject","REJECT","100","SUPPLIER_REJECTED");}
 @Test void changes80PreservesOrdered100()throws Exception{positive("supplier-change","CHANGE","80","SUPPLIER_CHANGE_PENDING");}
 @Test void unverifiedDocumentAndWrongQuantityCannotCommit()throws Exception {
  install("supplier-accept");f.executeThrough("source-reply-document",Set.of("transmission"));
  var request=(ObjectNode)f.sub.path("actions").get(0).deepCopy(); // overwritten below from authored reply
  for(var a:f.sub.path("actions"))if(a.path("id").asText().equals("supplier-reply"))request=(ObjectNode)a.path("request").deepCopy();
  ((ObjectNode)request.path("slots")).remove("canonicalOccurrenceId");request=(ObjectNode)new org.mulino.verification.ReferenceResolver(f.results,f.aliases).resolve(request);
  var unverified=f.call("recordSupplierReply",request,"source");assertEquals("EVIDENCE_UNVERIFIED",unverified.at("/response/error/code").asText(),unverified.toString());
  // Continue only the remaining source commands, without replaying setup/business commands.
  for(var a:f.sub.path("actions"))if(Set.of("source-reply-event","source-reply-match","source-reply-canonical").contains(a.path("id").asText())){var r=(ObjectNode)new org.mulino.verification.ReferenceResolver(f.results,f.aliases).resolve(a.path("request"));var result=f.call(a.path("capabilityId").asText(),r,"t13Source");f.results.put(a.path("id").asText(),result);assertEquals("APPLIED",result.at("/response/outcome").asText(),result.toString());}
  var bad=f.replyRequest().put("commandIdempotencyKey",UUID.randomUUID().toString());((ObjectNode)bad.path("slots").path("proposedQuantity")).put("unit","EA");var denied=f.call("recordSupplierReply",bad,"source");assertNotEquals("APPLIED",denied.at("/response/outcome").asText());assertTrue(rows("mulino_trade_purchase_SupplierReplies").isEmpty());zeroPhysical();
 }

 @Test void missingTransmissionConfirmationCannotCreateCommitment()throws Exception {
  install("supplier-accept");f.executeThrough("source-reply-canonical",Set.of("transmission"));
  var result=f.call("recordSupplierReply",f.replyRequest(),"source");f.results.put("missing-transmission-probe",result);
  assertNotEquals("APPLIED",result.at("/response/outcome").asText(),"Product accepted supplier commitment without confirmed transmission: "+result);
  assertTrue(rows("mulino_trade_purchase_SupplierReplies").isEmpty());zeroPhysical();
 }
 @Test void wrongScopeAndExpiredSourceCannotPublish()throws Exception {
  for(String mode:List.of("wrong-item","wrong-work","expired")) {
   f=new T13SupplierSourceFacts(root,ActualConfiguration.environment(System.getenv()),"supplier-accept",fixture->{
    var grant=(ObjectNode)fixture.path("actors").path("t13Source").path("grant");
    if(mode.equals("expired"))grant.put("validUntil","2026-10-07T09:00:00Z");
    else if(mode.equals("wrong-item"))((ObjectNode)grant.path("scope")).set("items",Json.array().add("P-alt"));
    else ((ObjectNode)grant.path("scope")).set("works",Json.array().add("W"));
   });
   org=f.aliases.path("ORG").asText();db=DriverManager.getConnection(f.config.jdbcUrl(),f.config.username(),f.config.password());
   var failure=assertThrows(IllegalStateException.class,()->f.executeThrough("source-transmission-document"));assertTrue(failure.getMessage().contains("FORBIDDEN"),failure.getMessage());
   assertTrue(rows("mulino_trade_purchase_SupplierReplies").isEmpty());assertTrue(rows("mulino_evidence_CanonicalOccurrences").isEmpty());zeroPhysical();
   f.save(Path.of(System.getenv("T13_TEST_EVIDENCE")).resolve(name+"-"+mode+".json"));db.close();db=null;
  }
 }

 @Test void mismatchedSourceIdentityQuantityTimeAndOrderDoNotVerify()throws Exception {
  install("supplier-accept");f.executeThrough("source-reply-event",Set.of("transmission"));
  ObjectNode authored=null;for(var a:f.sub.path("actions"))if(a.path("id").asText().equals("source-reply-match"))authored=(ObjectNode)new org.mulino.verification.ReferenceResolver(f.results,f.aliases).resolve(a.path("request"));
  for(String field:List.of("sourceIdentity","quantity","unit","effectiveFrom","physicalScopeId")) {
   var request=authored.deepCopy().put("commandIdempotencyKey",UUID.randomUUID().toString());
   String value=switch(field){case "sourceIdentity"->"other-source:other-event:1";case "quantity"->"99";case "unit"->"EA";case "effectiveFrom"->"2026-10-07T09:00:01Z";default->UUID.randomUUID().toString();};
   ((ObjectNode)request.path("slots")).put(field,value);var result=f.call("matchSourceIdentity",request,"t13Source");f.results.put("mismatch-"+field,result);
   assertNotEquals("MATCHED",result.at("/response/evidenceStatus").asText(),result.toString());
   assertEquals(1,rows("mulino_evidence_CanonicalOccurrences").size());assertTrue(rows("mulino_trade_purchase_SupplierReplies").isEmpty());zeroPhysical();
  }
 }

 @Test void supplierCanonicalRepliesAreDistinctIndependentOfTransmission()throws Exception {
  for(String sid:List.of("supplier-accept","supplier-reject","supplier-change")) {
   install(sid);f.executeThrough("supplier-reply",Set.of("transmission"));
   var rows=rows("mulino_trade_purchase_SupplierReplies");assertEquals(1,rows.size());
   var row=rows.getFirst();String reply=sid.substring("supplier-".length()).toUpperCase();
   assertEquals(reply,row.get("reply"));assertEquals("BOX",row.get("unit"));
   assertEquals(0,new java.math.BigDecimal(reply.equals("CHANGE")?"80":"100").compareTo((java.math.BigDecimal)row.get("proposedquantity")));
   assertEquals(f.results.get("dispatch").at("/response/orderId").asText(),row.get("orderid"));
   assertEquals(1,rows("mulino_runtime_Outbox").size());zeroPhysical();
   var duties=rows("mulino_work_read_ObligationReferences");
   var review=duties.stream().filter(x->"SUPPLIER_REPLY_REVIEW".equals(x.get("kind"))).toList();assertEquals(reply.equals("ACCEPT")?0:1,review.size());
   for(var duty:review){assertEquals("OPEN",duty.get("status"));assertEquals(f.aliases.path("procurement").asText(),duty.get("ownerid"));assertNotNull(duty.get("nextaction"));assertNotNull(duty.get("nextcheckat"));}
   var replay=f.call("recordSupplierReply",f.replyRequest(),"source");assertEquals("APPLIED",replay.at("/response/outcome").asText());
   var duplicate=f.replyRequest().put("commandIdempotencyKey",UUID.randomUUID().toString()).put("expectedRevision",2);var duplicateResult=f.call("recordSupplierReply",duplicate,"source");f.results.put("duplicate-probe",duplicateResult);assertEquals("SUPPLIER_REPLY_ALREADY_RECORDED",duplicateResult.at("/response/error/code").asText());
   assertEquals(1,rows("mulino_trade_purchase_SupplierReplies").size());
   f.save(Path.of(System.getenv("T13_TEST_EVIDENCE")).resolve("supplier-only-"+sid+".json"));db.close();db=null;
  }
 }

 @Test void anotherOrganizationCannotUseOriginalOrderOrCanonical()throws Exception {
  install("supplier-accept");f.executeThrough("source-reply-canonical",Set.of("transmission"));
  var other=new T13SupplierSourceFacts(root,f.config,"supplier-accept");
  var result=other.call("recordSupplierReply",f.replyRequest(),"source");other.results.put("cross-org-probe",result);
  assertEquals("FORBIDDEN",result.at("/response/error/code").asText(),result.toString());assertTrue(rows("mulino_trade_purchase_SupplierReplies").isEmpty());zeroPhysical();
  other.save(Path.of(System.getenv("T13_TEST_EVIDENCE")).resolve(name+"-other-org.json"));
 }

 @Test void originalReplyAssertionUsesActualObserverFields()throws Exception {
  install("supplier-accept");f.executeThrough("supplier-reply",Set.of("transmission"));
  var scope=Json.object().put("organizationId",org).put("itemId",f.aliases.path("P").asText()).put("caseId","T13").put("subcaseId","supplier-accept");
  var request=Json.object().put("asOf","2026-10-07T09:00:00Z").put("knownAt","2026-10-07T09:00:01Z").put("snapshotRef","CURRENT_COMMITTED");request.set("scope",scope);request.set("sources",Json.array().add("supplierCommitments"));
  var observed=ScenarioJdbcObservation.capture(f.config,root,request);var result=Json.object().put("driverStatus","EXECUTED");result.set("data",observed);result.set("provenance",Json.object().put("scopeComplete",true));f.results.put("reply-db",result);
  for(var assertion:f.sub.path("assertions"))if(assertion.path("id").asText().equals("reply-distinct"))new org.mulino.verification.AssertionEngine().check(assertion,f.results,f.aliases);
 }
}
