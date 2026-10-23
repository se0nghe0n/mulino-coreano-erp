package org.mulino.verification.actual;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mulino.verification.Json;

/** Focused real HTTP/PG lifecycle checks; does not invoke the scenario runner or independent observer. */
@EnabledIfEnvironmentVariable(named="T11_PG_TEST",matches="true")
class T11WorkLifecyclePostgresTest {
 @ParameterizedTest @ValueSource(strings={"parent90-cancelled","parent90-impossible","parent90-superseded"})
 void actualAssessmentCreatesTenAndRecipientTransferNeverFulfillsHundred(String subcase)throws Exception{
  try(var w=new T11ParentHttp(subcase,"lifecycle")){
   w.through("parent-assessment");
   var root=T11GoalShortfallPostgresTest.roots(w).getFirst();assertEquals(new BigDecimal("10.000000000000"),root.get("quantity"));
   String source=w.results.get("parent").path("response").path("workId").asText();
   var proof=Json.parse(root.get("scopejson").toString()).path("proof");assertEquals("100",proof.path("targetQuantity").asText());assertEquals("90",proof.path("recognizedQuantity").asText());
   String rootId=root.get("id").toString(),assignment=w.results.get("parent-assessment-evaluate").path("response").path("effects").path("responsibility").path("assignments").get(0).path("ID").asText();
   assertFalse(assignment.isBlank());assertEquals("assessGoal",w.rows("mulino_commands_CommandRecords").stream().filter(x->root.get("sourceid").equals(x.get("id"))).findFirst().orElseThrow().get("capabilityid"));
   try(var q=w.db.prepareStatement("SELECT count(*) FROM mulino_identity_GrantActions WHERE organizationId=? AND capabilityId='openObligation'")){q.setString(1,w.org);try(var rs=q.executeQuery()){assertTrue(rs.next());assertEquals(0,rs.getInt(1));}}
   w.execute("followup");w.execute("deficit-transfer-current-work");w.execute("deficit-transfer");
   assertEquals("OPEN",w.rows("mulino_work_read_ObligationReferences").stream().filter(x->assignment.equals(x.get("id"))).findFirst().orElseThrow().get("status"));
   assertEquals(source,w.rows("mulino_work_read_ObligationReferences").stream().filter(x->rootId.equals(x.get("rootid"))&&Boolean.TRUE.equals(x.get("valid"))).findFirst().orElseThrow().get("workid"));
   var wrongRecipient=(com.fasterxml.jackson.databind.node.ObjectNode)new org.mulino.verification.ReferenceResolver(w.results,w.aliases).resolve(w.action("deficit-accept").path("request"));wrongRecipient.put("commandIdempotencyKey",subcase+"-wrong-recipient");
   var denied=w.send("wrong-recipient","B","/api/ontology/commands/acceptHandover",wrongRecipient);assertEquals("FORBIDDEN",denied.path("error").path("code").asText());assertEquals("OPEN",w.rows("mulino_work_read_ObligationReferences").stream().filter(x->assignment.equals(x.get("id"))).findFirst().orElseThrow().get("status"));
   assertEquals("APPLIED",w.execute("deficit-accept").path("outcome").asText());
   T11GoalShortfallPostgresTest.assess(w);assertEquals(root,T11GoalShortfallPostgresTest.roots(w).getFirst());
   String target=w.results.get("followup").path("response").path("workId").asText();
   var current=w.rows("mulino_work_read_ObligationReferences").stream().filter(x->rootId.equals(x.get("rootid"))&&Boolean.TRUE.equals(x.get("valid"))).toList();assertEquals(1,current.size());assertEquals(target,current.getFirst().get("workid"));assertEquals(assignment,current.getFirst().get("predecessorid"));
   w.execute("false-close-current-work");assertEquals("REJECTED",w.execute("false-close").path("outcome").asText());
   w.execute("alternative-close-current-work");var close=w.execute("alternative-close");assertEquals("APPLIED",close.path("outcome").asText(),close.toPrettyString());
   var original=w.rows("mulino_work_read_Works").stream().filter(x->source.equals(x.get("id"))).findFirst().orElseThrow();assertEquals("CLOSED",original.get("status"));
   assertEquals(root,T11GoalShortfallPostgresTest.roots(w).getFirst());
   assertEquals(1,w.rows("mulino_work_read_ObligationReferences").stream().filter(x->target.equals(x.get("workid"))&&"OPEN".equals(x.get("status"))).count());
   assertEquals(1,w.rows("mulino_inventory_QuantityMovements").size());assertEquals(1,w.rows("mulino_evidence_CanonicalOccurrences").size());
  }
 }
 @Test void partialAllocationRetainsNinetyAndFailedAssessmentRollsBackItsTen()throws Exception{
  try(var w=new T11ParentHttp("parent90-cancelled","lifecycle-rollback")){
   w.through("child30-close");
   assertEquals(1,w.rows("mulino_work_read_ObligationReferences").stream().filter(x->"RECEIPT_UNALLOCATED".equals(x.get("kind"))&&"OPEN".equals(x.get("status"))).count());
   for(String action:List.of("child60-link-current-work","child60-link","child60-assessment-evaluation-work","child60-assessment-evaluate","child60-assessment","child60-close-current-work","child60-close","parent-assessment-evaluation-work"))w.execute(action);
   assertEquals(1,w.rows("mulino_work_read_ObligationReferences").stream().filter(x->"RECEIPT_UNALLOCATED".equals(x.get("kind"))&&"RESOLVED".equals(x.get("status"))&&"VERIFIED_WORK_CONTRIBUTION_ALLOCATION".equals(x.get("basis"))).count());
   var before=w.rows("mulino_work_read_AssessmentReferences");var assignments=w.rows("mulino_work_read_ObligationReferences");
   var armed=w.send("arm-assessment-rollback","A","/__verification/runtime/fault",Json.object().put("operation","arm").put("organizationId",w.org).put("point","command.beforeCommit").put("capabilityId","assessGoal").put("commandKey","parent-assessment-evaluate").put("occurrences",1));assertEquals("ARMED",armed.path("state").asText());
   assertNotEquals("APPLIED",w.execute("parent-assessment-evaluate").path("outcome").asText());
   assertEquals(before,w.rows("mulino_work_read_AssessmentReferences"));assertEquals(assignments,w.rows("mulino_work_read_ObligationReferences"));assertTrue(T11GoalShortfallPostgresTest.roots(w).isEmpty());
   var fired=w.send("disarm-assessment-rollback","A","/__verification/runtime/fault",Json.object().put("operation","disarm").put("faultId",armed.path("faultId").asText()));assertEquals("FIRED",fired.path("state").asText());
   var retried=w.execute("parent-assessment-evaluate");assertEquals("APPLIED",retried.path("outcome").asText(),retried.toPrettyString());assertEquals(1,T11GoalShortfallPostgresTest.roots(w).size());
  }
 }
 @Test void rawTemperatureCreatesOneReviewDutyAndPreservesClosedWorld()throws Exception{
  try(var w=new T11ParentHttp("closed-followup","lifecycle")){
   var occurrences=w.rows("mulino_evidence_CanonicalOccurrences");var movements=w.rows("mulino_inventory_QuantityMovements");var before=w.rows("mulino_work_read_Works");var assessments=w.rows("mulino_work_read_AssessmentReferences");
   w.through("followup");var response=w.results.get("followup").path("response");assertEquals("APPLIED",response.path("outcome").asText(),response.toPrettyString());
   String child=response.path("workId").asText();
   var replay=(com.fasterxml.jackson.databind.node.ObjectNode)w.results.get("followup").path("request").deepCopy();replay.put("commandIdempotencyKey","same-source-second-key");
   var repeated=w.send("followup-second-key","A","/api/ontology/commands/createFollowup",replay);assertEquals("APPLIED",repeated.path("outcome").asText(),repeated.toPrettyString());assertEquals(child,repeated.path("workId").asText());
   replay.put("commandIdempotencyKey","different-response-second-key");((com.fasterxml.jackson.databind.node.ObjectNode)replay.path("slots").path("draft").path("goal")).put("targetQuantity","99");var conflicting=w.send("followup-different-goal","A","/api/ontology/commands/createFollowup",replay);assertEquals("IDEMPOTENCY_CONFLICT",conflicting.path("error").path("code").asText());assertEquals(before.size()+1,w.rows("mulino_work_read_Works").size());
   assertEquals(before.getFirst(),w.rows("mulino_work_read_Works").stream().filter(x->Objects.equals(before.getFirst().get("id"),x.get("id"))).findFirst().orElseThrow());assertEquals(assessments,w.rows("mulino_work_read_AssessmentReferences"));
   var assignments=w.rows("mulino_work_read_ObligationReferences");assertEquals(1,assignments.size());assertEquals("UNVERIFIED_SOURCE_REVIEW",assignments.getFirst().get("kind"));assertEquals(child,assignments.getFirst().get("workid"));
   var roots=w.rows("mulino_responsibility_Roots");assertEquals(1,roots.size());assertEquals("EVENT",roots.getFirst().get("sourcekind"));assertEquals(w.results.get("temperature").path("response").path("id").asText(),roots.getFirst().get("sourceid"));
   assertEquals(occurrences,w.rows("mulino_evidence_CanonicalOccurrences"));assertEquals(movements,w.rows("mulino_inventory_QuantityMovements"));
  }
 }
 @Test void missingResumeProofIsHeldForTheRealPredicate()throws Exception{
  try(var w=new T11ParentHttp("wait-timeout","lifecycle")){
   w.through("resume-without-proof");var response=w.results.get("resume-without-proof").path("response");assertEquals("HELD",response.path("outcome").asText(),response.toPrettyString());assertEquals("WAIT_UNVERIFIED",response.path("error").path("code").asText());
   assertEquals("WAITING",w.rows("mulino_work_read_Works").stream().filter(x->w.aliases.path("SOURCE").asText().equals(x.get("id"))).findFirst().orElseThrow().get("status"));assertTrue(w.rows("mulino_inventory_QuantityMovements").isEmpty());
  }
 }
 @Test void unsupportedPinnedEvaluatorIsHeldWithNoActivationAndAnAudit()throws Exception{
  try(var w=new T11ParentHttp("unsupported-evaluator","lifecycle")){
   w.through("activate");var response=w.results.get("activate").path("response");assertEquals("HELD",response.path("outcome").asText());assertEquals("VERSION_UNSUPPORTED",response.path("error").path("code").asText());assertEquals("DRAFT",w.rows("mulino_work_read_Works").getFirst().get("status"));
   assertEquals(1,w.rows("mulino_commands_CommandAudits").stream().filter(x->"activateWork".equals(x.get("capabilityid"))&&"HELD".equals(x.get("outcome"))).count());
  }
 }
}
