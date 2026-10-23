package org.mulino.verification.actual;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mulino.verification.*;

/** Focused HTTP plus read-only PG capture; never invokes the native scenario workflow. */
@EnabledIfEnvironmentVariable(named="T11_PG_TEST",matches="true")
class T11NativeInputsPostgresTest {
 static void observe(T11ParentHttp w,String id)throws Exception {
  var request=(ObjectNode)new ReferenceResolver(w.results,w.aliases).resolve(w.action(id).path("observation"));
  // Serial focused capture has no issuing-query digest binding. Preserve this distinction in the raw artifact.
  request.put("snapshotRef","CURRENT_COMMITTED");
  var data=ScenarioJdbcObservation.capture(w.config,w.root,request);
  var result=Json.object().put("driverStatus","EXECUTED");result.set("data",data);
  result.set("provenance",Json.object().put("source","POSTGRESQL_JDBC").put("scopeComplete",true).put("independent",true));
  w.results.put(id,result);Json.write(w.evidence.resolve(id+".json"),result);
 }
 @Test void cancelAfterShipmentRetainsAllFourteenOriginalOracles()throws Exception {
  try(var w=new T11ParentHttp("cancel-after-shipment","native-r2")) {
   dutyDocumentProof(w);
   for(var a:w.subcase.path("actions")) {
    String id=a.path("id").asText();
    if(Set.of("invoke","query").contains(a.path("kind").asText()))w.execute(id);
    else if(a.path("kind").asText().equals("observe"))observe(w,id);
   }
   transactionProof(w);
   var checks=Json.array();var failures=new ArrayList<org.junit.jupiter.api.function.Executable>();
   for(var a:w.subcase.path("assertions")) {
    var engine=new AssertionEngine();var check=Json.object().put("assertionId",a.path("id").asText());
    check.set("declared",a);checks.add(check);
    try{engine.check(a,w.results,w.aliases);check.put("status","PASS");}
    catch(AssertionError failure){check.put("status","FAIL").put("reason",failure.getMessage());failures.add(()->fail(a.path("id").asText()+": "+failure.getMessage()));}
    check.set("observed",engine.observedValue());
   }
   Json.write(w.evidence.resolve("original-oracles.json"),checks);
   assertEquals(14,checks.size());assertAll(failures);
  }
 }
 static void dutyDocumentProof(T11ParentHttp w)throws Exception {
  var roots=w.rows("mulino_responsibility_Roots");var root=roots.stream().filter(r->w.aliases.path("ROOT").asText().equals(r.get("id"))).findFirst().orElseThrow();
  assertEquals("DOCUMENT",root.get("sourcekind"));assertEquals("1",root.get("sourceversion"));
  var document=w.rows("mulino_evidence_DocumentVersions").stream().filter(d->root.get("sourceid").equals(d.get("id"))).findFirst().orElseThrow();assertEquals(w.org,document.get("organizationid"));assertEquals("AVAILABLE",document.get("availability"));
  var blob=java.nio.file.Path.of(System.getProperty("verification.actual.blobRoot"),"objects",document.get("blobid").toString());var bytes=java.nio.file.Files.readString(blob);
  assertEquals(document.get("sha256"),Json.sha256Text(bytes));var original=Json.parse(bytes);assertEquals("baseline-shortfall-10",original.path("sourceOccurrence").asText());assertEquals("1",original.path("sourceVersion").asText());assertEquals(w.aliases.path("SOURCE").asText(),original.path("originalAssignment").path("workId").asText());
  var artifact=Json.object().put("documentId",document.get("id").toString()).put("sha256",document.get("sha256").toString()).put("organizationId",w.org).put("recordedAt",document.get("recordedat").toString()).put("rootSourceKind","DOCUMENT").put("rootSourceVersion","1").put("blobBytes",bytes);
  artifact.set("installation",w.installed.path("lifecycleBaselineInstallation"));artifact.set("httpSourceCalls",w.installed.path("sourceFactEvidence").path("commands"));Json.write(w.evidence.resolve("duty-document-source-proof.json"),artifact);
 }
 static void transactionProof(T11ParentHttp w)throws Exception {
  var response=w.results.get("transfer").path("response");var proof=Json.object().put("recordType","FOCUSED_SUCCESSOR_TRANSACTION_SQL").put("observerProjectionChanged",false);
  proof.set("acceptResponse",response);var statements=Json.array();proof.set("statements",statements);var actual=new HashMap<String,String>();
  w.db.setAutoCommit(false);w.db.setReadOnly(true);w.db.setTransactionIsolation(java.sql.Connection.TRANSACTION_REPEATABLE_READ);
  try {
   proof.put("snapshot",ObserverSnapshot.token(w.db));
   for(String table:List.of("mulino_commands_CommandRecords","mulino_commands_CommandAudits","mulino_work_read_ObligationReferences")) {
    String filter=table.endsWith("CommandRecords")?"commandIdempotencyKey=?":table.endsWith("CommandAudits")?"commandId=?":"workId=? AND status='OPEN' AND valid=true";
    String parameter=table.endsWith("CommandRecords")?"T11-cancel-after-shipment-transfer":table.endsWith("CommandAudits")?response.path("commandId").asText():w.aliases.path("TARGET").asText();
    String sql="SELECT row_to_json(r)::text, r.xmin::text FROM "+table+" r WHERE organizationId=? AND "+filter+" ORDER BY ID";
    var statement=Json.object().put("sql",sql);statement.set("parameters",Json.array().add(w.org).add(parameter));var rows=Json.array();statement.set("rows",rows);statements.add(statement);
    try(var q=w.db.prepareStatement(sql)) {
     q.setString(1,w.org);q.setString(2,parameter);
     try(var r=q.executeQuery()) {
      while(r.next()) {
       var row=Json.object().put("rawRowBytes",r.getString(1)).put("tupleWriteTransactionId",r.getString(2));rows.add(row);actual.put(table,r.getString(2));
       if(table.endsWith("ObligationReferences")) {
        var assignment=Json.parse(r.getString(1));assertEquals(0,assignment.path("revision").asInt(-1));assertFalse(w.results.get("before-db").path("data").path("rawRows").path("assignments").toString().contains(assignment.path("id").asText()));
       }
      }
     }
    }
    assertEquals(1,rows.size());
   }
  }finally{w.db.rollback();w.db.setReadOnly(false);w.db.setAutoCommit(true);Json.write(w.evidence.resolve("successor-transaction-sql.json"),proof);}
  for(var transaction:actual.values())assertEquals(response.path("transactionId").asText(),transaction);
 }
 @Test void unknownOriginalExpiryRemainsUnknownWithZeroEligibleQuantity()throws Exception {
  try(var w=new T11ParentHttp("cancel-after-shipment","native-r2-unknown-expiry",fixture->{
   ((ObjectNode)fixture.path("aliases").path("LOT")).remove("expiresAt");
   ((ObjectNode)fixture.path("baseline").path("nativeR2LifecycleCompletion").path("lotExpiryCompletion")).put("applied",false);
  })) {
   var movements=w.rows("mulino_inventory_QuantityMovements");w.through("reserve");
   var inventory=w.results.get("before-db-snapshot").path("response");assertTrue(inventory.path("unknowns").toString().contains("LOT_EXPIRY_UNKNOWN"));assertEquals("0",inventory.path("data").path("eligibleQuantity").asText());
   var reserve=w.results.get("reserve").path("response");assertEquals("REJECTED",reserve.path("outcome").asText());assertEquals("INSUFFICIENT_ELIGIBLE_QUANTITY",reserve.path("error").path("code").asText());assertTrue(w.rows("mulino_inventory_SegmentAllocations").isEmpty());assertEquals(movements,w.rows("mulino_inventory_QuantityMovements"));
  }
 }
 @Test void historicalGivenExistsOnceAndRemainsByteIdenticalAfterTemperature()throws Exception {
  try(var w=new T11ParentHttp("closed-followup","native-r2")) {
   var initialMovements=w.rows("mulino_inventory_QuantityMovements");var initialOccurrences=w.rows("mulino_evidence_CanonicalOccurrences");
   var rows=w.rows("mulino_work_read_AssessmentReferences");assertEquals(1,rows.size(),"Empty historical equality is not an oracle");var a=rows.getFirst();
   assertEquals(w.aliases.path("OLDASSESS").asText(),a.get("id"));assertEquals(w.aliases.path("PARENT").asText(),a.get("workid"));assertEquals(w.org,a.get("organizationid"));
   assertEquals("SATISFIED",a.get("outcome"));assertEquals("evaluator-v1",a.get("evaluatorversion"));assertEquals(java.time.Instant.parse("2026-10-07T00:00:00Z"),((java.sql.Timestamp)a.get("assessedat")).toInstant());
   var snapshots=w.rows("mulino_evaluation_InputSnapshots");assertEquals(1,snapshots.size());var s=snapshots.getFirst();var content=Json.parse(s.get("contentjson").toString());
   assertEquals(Json.sha256Text(s.get("contentjson").toString()),a.get("inputsnapshothash"));assertEquals(a.get("inputsnapshothash"),s.get("contenthash"));assertEquals(a.get("id"),s.get("assessmentid"));assertEquals(a.get("goalid"),s.get("goalid"));assertEquals(a.get("workid"),s.get("workid"));assertEquals(w.fixture.path("baseline").path("assessments").get(0),content.path("originalAssessment"));
   assertEquals("goal-closed-v1",content.path("syntheticCompletion").path("originalGoalLabel").asText());assertFalse(content.path("runtimeEvaluationClaimed").asBoolean(true));
   var parent=w.rows("mulino_work_read_Works").getFirst();assertNull(parent.get("currentgoalversionid"));var goal=w.rows("mulino_work_read_GoalReferences").getFirst();assertEquals(a.get("goalid"),goal.get("id"));assertEquals(a.get("workid"),goal.get("workid"));assertEquals("STATE_AT",goal.get("quantitymode"));
   String before=historyBytes(w);Json.write(w.evidence.resolve("historical-given-before.json"),Json.parse(before));
   w.execute("before-db-snapshot");observe(w,"before-db");
   for(var assertion:w.subcase.path("assertions"))if(assertion.path("id").asText().endsWith("baseline-one"))new AssertionEngine().check(assertion,w.results,w.aliases);
   w.through("followup");assertEquals("APPLIED",w.results.get("followup").path("response").path("outcome").asText());w.execute("after-db-snapshot");observe(w,"after-db");
   for(var assertion:w.subcase.path("assertions"))new AssertionEngine().check(assertion,w.results,w.aliases);
   assertEquals(parent,w.rows("mulino_work_read_Works").stream().filter(row->a.get("workid").equals(row.get("id"))).findFirst().orElseThrow());
   assertEquals(before,historyBytes(w));Json.write(w.evidence.resolve("historical-given-after.json"),Json.parse(historyBytes(w)));
   String child=w.results.get("followup").path("response").path("workId").asText();assertTrue(w.rows("mulino_work_read_AssessmentReferences").stream().noneMatch(row->child.equals(row.get("workid"))));
   assertThrows(SQLException.class,()->{try(var q=w.db.prepareStatement("UPDATE mulino_work_read_AssessmentReferences SET outcome='UNSATISFIED' WHERE organizationId=? AND ID=?")){q.setString(1,w.org);q.setString(2,a.get("id").toString());q.executeUpdate();}});
   assertThrows(SQLException.class,()->{try(var q=w.db.prepareStatement("DELETE FROM mulino_evaluation_InputSnapshots WHERE organizationId=? AND assessmentId=?")){q.setString(1,w.org);q.setString(2,a.get("id").toString());q.executeUpdate();}});
   assertEquals(before,historyBytes(w));assertEquals(initialMovements,w.rows("mulino_inventory_QuantityMovements"));assertEquals(initialOccurrences,w.rows("mulino_evidence_CanonicalOccurrences"));
  }
 }
 static String historyBytes(T11ParentHttp w)throws Exception {
  var result=Json.object();w.db.setAutoCommit(false);w.db.setReadOnly(true);w.db.setTransactionIsolation(java.sql.Connection.TRANSACTION_REPEATABLE_READ);
  try{for(String table:List.of("mulino_work_read_GoalReferences","mulino_work_read_AssessmentReferences","mulino_evaluation_InputSnapshots")){var rows=Json.array();try(var q=w.db.prepareStatement("SELECT row_to_json(r)::text FROM "+table+" r WHERE organizationId=? AND workId=? ORDER BY ID")){q.setString(1,w.org);q.setString(2,w.aliases.path("PARENT").asText());try(var r=q.executeQuery()){while(r.next())rows.add(r.getString(1));}}result.set(table,rows);}return result.toString();}
  finally{w.db.rollback();w.db.setReadOnly(false);w.db.setAutoCommit(true);}
 }
}
