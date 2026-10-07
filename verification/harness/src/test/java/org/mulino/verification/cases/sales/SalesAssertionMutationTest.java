package org.mulino.verification.cases.sales;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mulino.verification.*;
import static org.junit.jupiter.api.Assertions.*;

/** Fixed assertion captures only. There is no driver, state machine or product implementation here. */
public final class SalesAssertionMutationTest {
    private final Path root=Path.of(System.getProperty("repo.root"));
    private final AssertionEngine engine=new AssertionEngine();
    private final ObjectNode aliases=Json.parse("{\"ORG\":\"org-test\",\"P\":\"item-test\",\"W\":\"warehouse-test\",\"sales\":\"sales-human\",\"admin\":\"admin-human\",\"qc\":\"qc-human\"}").deepCopy();
    private JsonNode assertion(String cid,String sub,String observation,String op,boolean db) throws Exception {
        JsonNode c=Json.read(root.resolve("verification/cases/"+cid+"/case.json"));
        for(JsonNode s:c.path("subcases")) if(s.path("id").asText().equals(sub))
            for(JsonNode a:s.path("assertions")) if(a.path("oracleRef").path("observationNames").get(0).asText().equals(observation)
                && a.path("op").asText().equals(op) && a.path("source").path("actionId").asText().endsWith("-db")==db) return a;
        throw new AssertionError("Missing actual case assertion: "+cid+"/"+sub+"/"+observation+"/"+op);
    }
    private ObjectNode capture(String json) {
        ObjectNode n=Json.object();n.put("driverStatus","EXECUTED");n.set("data",Json.parse(json));
        n.set("provenance",Json.parse("{\"scopeComplete\":true,\"source\":\"CANNED_ASSERTION_SELFTEST\",\"independent\":true}"));
        n.set("artifactRefs",Json.parse("[\"FIXED_ASSERTION_CAPTURE_NOT_RUNTIME_EVIDENCE\"]"));return n;
    }
    private Map<String,JsonNode> one(JsonNode a,ObjectNode n){return new HashMap<>(Map.of(a.path("source").path("actionId").asText(),n));}
    private void check(JsonNode a,Map<String,JsonNode> results){engine.check(a,results,aliases);}
    private void wrong(JsonNode a,Map<String,JsonNode> results){assertThrows(AssertionError.class,()->check(a,results));}

    @Test void warehouse80MustBeIndependentExactQuantityAndActualBoxUnit() throws Exception {
        JsonNode a=assertion("E1","full-flow-quantities","warehouse-current-held","decimalEquals",false);
        ObjectNode n=capture("{}");n.set("response",Json.parse("{\"data\":{\"heldQuantity\":\"80\",\"unit\":\"BOX\"}}"));var rs=one(a,n);check(a,rs);
        ((ObjectNode)n.path("response").path("data")).put("heldQuantity","79");wrong(a,rs);
        ((ObjectNode)n.path("response").path("data")).put("heldQuantity","80").put("unit","KG");wrong(a,rs);
        ((ObjectNode)n.path("response").path("data")).put("unit","BOX").put("heldQuantity","UNKNOWN");wrong(a,rs);
    }
    @Test void warehouse80IsAlsoCheckedFromThreeIndependentRawScopes() throws Exception {
        JsonNode a=assertion("E1","full-flow-quantities","warehouse-current-held","sumEquals",true);
        ObjectNode n=capture("{\"data\":{\"unit\":\"BOX\"},\"rawRows\":{\"segments\":[{\"id\":\"agency30\",\"quantity\":\"30\",\"unit\":\"BOX\",\"locationId\":\"warehouse-test\",\"active\":true},{\"id\":\"qc40\",\"quantity\":\"40\",\"unit\":\"BOX\",\"locationId\":\"warehouse-test\",\"active\":true},{\"id\":\"return10\",\"quantity\":\"10\",\"unit\":\"BOX\",\"locationId\":\"warehouse-test\",\"active\":true}]}}");var rs=one(a,n);check(a,rs);
        ((ObjectNode)n.path("data").path("rawRows").path("segments").get(0)).put("unit","KG");wrong(a,rs);
        ((ObjectNode)n.path("data").path("rawRows").path("segments").get(0)).put("unit","BOX");
        ((ObjectNode)n.path("data").path("rawRows").path("segments").get(2)).put("quantity","9");wrong(a,rs);
        ((ObjectNode)n.path("data").path("rawRows").path("segments").get(2)).remove("locationId");wrong(a,rs);
    }
    @Test void consumedAllocationCannotBecomeExecutableForNormalDelivery() throws Exception {
        JsonNode a=assertion("T17","normal-consumed-delivery","allocation-still","exactSet",true);
        ObjectNode n=capture("{\"rawRows\":{\"allocations\":[{\"id\":\"allocation-one\",\"status\":\"CONSUMED\"}]}}");var rs=one(a,n);
        ObjectNode reserve=capture("{}");reserve.set("response",Json.parse("{\"allocationId\":\"allocation-one\"}"));rs.put("reserve",reserve);check(a,rs);
        ((ObjectNode)n.path("data").path("rawRows").path("allocations").get(0)).put("status","EXECUTABLE");wrong(a,rs);
    }
    @Test void delivery20DoesNotEraseTransit10AfterSellIsDenied() throws Exception {
        JsonNode a=assertion("T17","late-restriction-actual-delivery","remaining-transit","decimalEquals",false);
        ObjectNode n=capture("{}");n.set("response",Json.parse("{\"data\":{\"inTransitQuantity\":\"10\",\"unit\":\"BOX\"}}"));var rs=one(a,n);check(a,rs);
        ((ObjectNode)n.path("response").path("data")).put("inTransitQuantity","0");wrong(a,rs);
    }
    @Test void zeroDeltaRequiresBaselineUnitAndDoesNotAcceptChangedWarehouseDispatch() throws Exception {
        JsonNode a=assertion("T17","late-restriction-actual-delivery","new-warehouse-dispatch","decimalDelta",false);
        ObjectNode after=capture("{}");after.set("response",Json.parse("{\"data\":{\"warehouseDispatchQuantity\":\"30\",\"unit\":\"BOX\"}}"));
        ObjectNode before=after.deepCopy();var rs=one(a,after);rs.put(a.path("baseline").path("actionId").asText(),before);check(a,rs);
        ((ObjectNode)before.path("response").path("data")).put("unit","KG");wrong(a,rs);
        ((ObjectNode)before.path("response").path("data")).put("unit","BOX");((ObjectNode)after.path("response").path("data")).put("warehouseDispatchQuantity","50");wrong(a,rs);
    }
    @Test void factsDoNotMakeLateNoncompliantDeliveryAssessmentSatisfied() throws Exception {
        JsonNode a=assertion("T17","late-restriction-actual-delivery","fact-not-permission","exactSet",true);
        // Pick the assessment conjunction, not the separate canonical fact or obligation-kind check.
        for(JsonNode x:Json.read(root.resolve("verification/cases/T17/case.json")).path("subcases")) if(x.path("id").asText().equals("late-restriction-actual-delivery"))
            for(JsonNode b:x.path("assertions")) if(b.path("source").path("field").asText().equals("result") && b.path("oracleRef").path("observationNames").get(0).asText().equals("fact-not-permission")) a=b;
        ObjectNode n=capture("{\"rawRows\":{\"assessments\":[{\"goalKind\":\"DELIVERY\",\"current\":true,\"result\":\"UNSATISFIED\"}]}}");var rs=one(a,n);check(a,rs);
        ((ObjectNode)n.path("data").path("rawRows").path("assessments").get(0)).put("result","SATISFIED");wrong(a,rs);
    }
    @Test void deficit2IsNotZeroAndMissingHumanResponsibilityCannotPass() throws Exception {
        JsonNode quantity=assertion("C4","corrected-delivery-98","current-unresolved-deficit","sumEquals",true);
        ObjectNode n=capture("{\"data\":{\"unit\":\"BOX\"},\"rawRows\":{\"obligations\":[{\"id\":\"deficit-two\",\"rootId\":\"root-two\",\"kind\":\"DELIVERY_DEFICIT\",\"status\":\"OPEN\",\"current\":true,\"quantity\":\"2\",\"unit\":\"BOX\",\"ownerId\":\"sales-human\",\"responsibleWorkId\":\"work-one\",\"nextAction\":\"인도 부족과 제한 대응을 확인한다\",\"nextCheckAt\":\"2026-10-07T10:00:00Z\",\"sourceOccurrenceId\":\"corrected-one\"}]}}");var rs=one(quantity,n);check(quantity,rs);
        JsonNode owner=assertion("C4","corrected-delivery-98","deficit-owner","fieldsPresent",true);check(owner,rs);
        ((ObjectNode)n.path("data").path("rawRows").path("obligations").get(0)).remove("ownerId");wrong(owner,rs);
        ((ObjectNode)n.path("data").path("rawRows").path("obligations").get(0)).put("quantity","0");wrong(quantity,rs);
    }
    @Test void alreadyResolvedDeficitCannotBeResurrectedAsOpenAssignment() throws Exception {
        JsonNode a=assertion("C4","resolved-no-resurrection-resolved","resolved-debt-resurrection","sameAs",true);
        ObjectNode after=capture("{\"rawRows\":{\"obligations\":[{\"rootId\":\"root-two\",\"ownerId\":\"sales-human\",\"status\":\"RESOLVED\",\"resolutionDecisionId\":\"decision-one\"}]}}");var rs=one(a,after);rs.put(a.path("baseline").path("actionId").asText(),after.deepCopy());check(a,rs);
        ((ObjectNode)after.path("data").path("rawRows").path("obligations").get(0)).put("status","OPEN");wrong(a,rs);
    }
    @Test void recovery25PlusSameDisposed25MustNeverBecome50() throws Exception {
        JsonNode a=assertion("E2","same-25-not-50","unique-finally-processed","decimalEquals",false);
        ObjectNode n=capture("{}");n.set("response",Json.parse("{\"data\":{\"processedQuantity\":\"25\",\"unit\":\"BOX\"}}"));var rs=one(a,n);check(a,rs);
        ((ObjectNode)n.path("response").path("data")).put("processedQuantity","50");wrong(a,rs);
    }
    @Test void uniqueRecoveryMembershipRejectsDuplicateAndDifferentSameQuantityPhysicalScope() throws Exception {
        JsonNode a=assertion("E2","same-25-not-50","status-axes","exactSet",true);
        // Pick the actual serial-set assertion added for the 25 identified recovered boxes.
        for(JsonNode s:Json.read(root.resolve("verification/cases/E2/case.json")).path("subcases")) if(s.path("id").asText().equals("same-25-not-50"))
            for(JsonNode b:s.path("assertions")) if(b.path("source").path("pointer").asText().equals("/data/rawRows/recoveredMembers") && b.path("op").asText().equals("exactSet")) a=b;
        ObjectNode n=capture("{\"rawRows\":{\"recoveredMembers\":[]}}");ArrayNode rows=(ArrayNode)n.path("data").path("rawRows").path("recoveredMembers");
        for(int i=1;i<=25;i++){ObjectNode row=Json.object();row.put("boxSerial",String.format("same-25-not-50-A-BOX-%03d",i));rows.add(row);}
        var rs=one(a,n);check(a,rs);rows.add(rows.get(0).deepCopy());wrong(a,rs);rows.remove(25);
        ((ObjectNode)rows.get(24)).put("boxSerial","same-25-not-50-A-BOX-026");wrong(a,rs);
    }
    @Test void ownerPresenceDoesNotReplaceCorrectOneCurrentHumanAssignment() throws Exception {
        JsonNode a=assertion("E2","exception-responsibility","exception-residual-duty","count",true);
        ObjectNode n=capture("{\"rawRows\":{\"obligations\":[{\"kind\":\"RECALL_RESPONSE\",\"current\":true}]}}");var rs=one(a,n);check(a,rs);
        ((ArrayNode)n.path("data").path("rawRows").path("obligations")).add(n.path("data").path("rawRows").path("obligations").get(0).deepCopy());wrong(a,rs);
        ((ObjectNode)n.path("provenance")).put("scopeComplete",false);wrong(a,rs);
    }
    @Test void currentReassessmentCannotRewriteHistoricalEvaluatorVersion() throws Exception {
        JsonNode a=null;
        for(JsonNode sub:Json.read(root.resolve("verification/cases/C4/case.json")).path("subcases")) if(sub.path("id").asText().equals("resolved-no-resurrection-resolved"))
            for(JsonNode b:sub.path("assertions")) if(b.path("oracleRef").path("observationNames").get(0).asText().equals("valid-resolution") && b.path("op").asText().equals("sameAs")) a=b;
        assertNotNull(a);
        ObjectNode after=capture("{\"rawRows\":{\"assessments\":[{\"id\":\"old-assessment\",\"current\":false,\"result\":\"SATISFIED\",\"inputSnapshotId\":\"original-100\",\"evaluatorVersion\":\"evaluator-v1\"}]}}");
        ObjectNode before=capture("{\"rawRows\":{\"assessments\":[{\"id\":\"old-assessment\",\"current\":true,\"result\":\"SATISFIED\",\"inputSnapshotId\":\"original-100\",\"evaluatorVersion\":\"evaluator-v1\"}]}}");
        var rs=one(a,after);rs.put(a.path("baseline").path("actionId").asText(),before);check(a,rs);
        ((ObjectNode)after.path("data").path("rawRows").path("assessments").get(0)).put("evaluatorVersion","evaluator-v2");wrong(a,rs);
    }
    @Test void allNamedObservationsHaveActualDeclaredAssertionsAndNoAvailabilityOracle() throws Exception {
        JsonNode catalog=Json.read(root.resolve("verification/requirements/mandatory-oracles.json"));Set<String> expected=new HashSet<>(),actual=new HashSet<>();
        Set<String> assigned=Set.of("T17","T18","C4","E1","E2");
        for(JsonNode o:catalog.path("oracles")) if(assigned.contains(o.path("caseId").asText())) for(JsonNode n:o.path("expectedObservations")) expected.add(o.path("oracleId").asText()+":"+n.path("name").asText());
        for(String cid:assigned) for(JsonNode s:Json.read(root.resolve("verification/cases/"+cid+"/case.json")).path("subcases")) for(JsonNode a:s.path("assertions")) {
            assertFalse(a.path("source").path("pointer").asText().contains("driverStatus"));
            for(JsonNode n:a.path("oracleRef").path("observationNames")) actual.add(a.path("oracleRef").path("oracleId").asText()+":"+n.asText());
        }
        assertEquals(94,expected.size());assertEquals(expected,actual);
    }
}
