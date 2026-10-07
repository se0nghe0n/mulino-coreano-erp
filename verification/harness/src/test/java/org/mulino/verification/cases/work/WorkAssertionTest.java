package org.mulino.verification.cases.work;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mulino.verification.AssertionEngine;
import org.mulino.verification.Json;
import static org.junit.jupiter.api.Assertions.*;

/** Captured assertion samples only; no state machine, service, DB or process fake. */
public class WorkAssertionTest {
    private final AssertionEngine engine = new AssertionEngine();
    private final JsonNode aliases = Json.parse("""
        {"ORG":"org-id","P":"item-id","W":"warehouse-id","ROOT":"root-id",
         "SOURCE":"source-id","TARGET":"target-id","A":"human-a","B":"human-b","intake":"human-intake"}
        """);
    private JsonNode assertion(String caseId, String subcase, String id) throws Exception {
        Path path = Path.of(System.getProperty("repo.root"), "verification/cases", caseId, "case.json");
        for (JsonNode s : Json.read(path).path("subcases"))
            if (s.path("id").asText().equals(subcase))
                for (JsonNode a : s.path("assertions"))
                    if (a.path("id").asText().equals(id)) return a;
        throw new AssertionError("Missing actual authored assertion " + caseId + "/" + subcase + "/" + id);
    }
    private JsonNode captured(String json) {
        ObjectNode r = Json.object();
        r.put("driverStatus", "EXECUTED");
        r.set("provenance", Json.parse("{\"scopeComplete\":true,\"source\":\"CANNED_CONTRACT_SELFTEST\"}"));
        r.set("data", Json.parse(json));
        return r;
    }
    private JsonNode identity(String field, String value) {
        ObjectNode r = (ObjectNode) captured("{}");
        ObjectNode response = Json.object(); response.put(field, value); r.set("response", response);
        return r;
    }
    private Map<String, JsonNode> cumulativeSample() {
        Map<String, JsonNode> rows = new HashMap<>();
        rows.put("cumulative", identity("workId", "cumulative-id"));
        rows.put("receive60", identity("occurrenceId", "receipt60-id"));
        rows.put("receive40", identity("occurrenceId", "receipt40-id"));
        rows.put("after-db", captured("""
            {"rawRows":{"contributions":[
              {"workId":"cumulative-id","occurrenceId":"receipt60-id","quantity":"60","unit":"BOX"},
              {"workId":"cumulative-id","occurrenceId":"receipt40-id","quantity":"40","unit":"BOX"}]}}
            """));
        return rows;
    }
    @Test void fixedReceipt60Plus40MatchesAuthoredQuantityAndIdentity() throws Exception {
        var rows = cumulativeSample();
        engine.check(assertion("T09","cumulative-versus-state","raw-receipt-sum100"), rows, aliases);
        engine.check(assertion("T09","cumulative-versus-state","arrival-distinct-identities"), rows, aliases);
    }
    @Test void quantityMutationFailsRatherThanReadingExpectedOutput() throws Exception {
        var rows = cumulativeSample();
        ((ObjectNode) rows.get("after-db").at("/data/rawRows/contributions/1")).put("quantity","39");
        assertThrows(AssertionError.class, () -> engine.check(assertion("T09","cumulative-versus-state","raw-receipt-sum100"), rows, aliases));
    }
    @Test void unitMutationFailsEvenWithUnchanged100() throws Exception {
        var rows = cumulativeSample();
        ((ObjectNode) rows.get("after-db").at("/data/rawRows/contributions/1")).put("unit","KG");
        assertThrows(AssertionError.class, () -> engine.check(assertion("T09","cumulative-versus-state","raw-receipt-sum100"), rows, aliases));
    }
    @Test void duplicatePhysicalOccurrenceFailsEvenWhenSumStill100() throws Exception {
        var rows = cumulativeSample();
        ((ObjectNode) rows.get("after-db").at("/data/rawRows/contributions/1")).put("occurrenceId","receipt60-id");
        engine.check(assertion("T09","cumulative-versus-state","raw-receipt-sum100"), rows, aliases);
        assertThrows(AssertionError.class, () -> engine.check(assertion("T09","cumulative-versus-state","arrival-distinct-identities"), rows, aliases));
    }
    @Test void unknownAndMissingQuantityCannotPassAsZero() throws Exception {
        for (String mutation : new String[]{"UNKNOWN","CONFLICT","MISSING"}) {
            var rows = cumulativeSample();
            var row = (ObjectNode) rows.get("after-db").at("/data/rawRows/contributions/1");
            if (mutation.equals("MISSING")) row.remove("quantity"); else row.put("quantity",mutation);
            assertThrows(AssertionError.class, () -> engine.check(assertion("T09","cumulative-versus-state","raw-receipt-sum100"), rows, aliases));
        }
    }
    private Map<String, JsonNode> assignmentSample() {
        return new HashMap<>(Map.of("after-db", captured("""
            {"rawRows":{"assignments":[{"workId":"source-id","status":"OPEN","ownerId":"human-a",
             "nextAction":"남은10상자 수령 확인","nextCheckAt":"2026-10-08T09:00:00Z"}]}}
            """)));
    }
    @Test void missingHumanOwnerFailsAuthoredResponsibilityAssertion() throws Exception {
        var rows = assignmentSample();
        var a = assertion("T10","link-failure","source-responsibility-owner");
        engine.check(a,rows,aliases);
        ((ObjectNode) rows.get("after-db").at("/data/rawRows/assignments/0")).remove("ownerId");
        assertThrows(AssertionError.class, () -> engine.check(a,rows,aliases));
    }
    @Test void duplicateCurrentAssignmentIsRejected() throws Exception {
        var rows = assignmentSample();
        var a = assertion("T10","link-failure","source-responsibility-one");
        engine.check(a,rows,aliases);
        var array = (com.fasterxml.jackson.databind.node.ArrayNode) rows.get("after-db").at("/data/rawRows/assignments");
        array.add(array.get(0).deepCopy());
        assertThrows(AssertionError.class, () -> engine.check(a,rows,aliases));
    }
    @Test void falseNextCheckCannotPassResponsibilityConjunction() throws Exception {
        var rows = assignmentSample();
        var a = assertion("T10","link-failure","source-responsibility-next-check");
        engine.check(a,rows,aliases);
        ((ObjectNode) rows.get("after-db").at("/data/rawRows/assignments/0")).put("nextCheckAt","2026-10-09T09:00:00Z");
        assertThrows(AssertionError.class, () -> engine.check(a,rows,aliases));
    }
    @Test void partialScopeOverlapFailsDespiteFourPlusSixEqualsTen() throws Exception {
        var rows = new HashMap<String, JsonNode>();
        rows.put("after-db",captured("""
          {"rawRows":{"obligations":[
           {"rootId":"root-id","scopeKind":"LEAF","responsibleWorkId":"target-id","quantity":"4","unit":"BOX","physicalScope":["b01","b02","b03","b04"]},
           {"rootId":"root-id","scopeKind":"LEAF","responsibleWorkId":"source-id","quantity":"6","unit":"BOX","physicalScope":["b05","b06","b07","b08","b09","b10"]}]}}
          """));
        var sum = assertion("T10","partial-commit","unresolved-root-sum10");
        var scope = assertion("T10","partial-commit","disjoint-leaf-scopes");
        engine.check(sum,rows,aliases);engine.check(scope,rows,aliases);
        ((com.fasterxml.jackson.databind.node.ArrayNode)rows.get("after-db").at("/data/rawRows/obligations/1/physicalScope")).set(0,Json.MAPPER.valueToTree("b04"));
        engine.check(sum,rows,aliases);
        assertThrows(AssertionError.class, () -> engine.check(scope,rows,aliases));
    }
    @Test void contaminatedEvaluatorVersionFailsAuthoredSnapshotTuple() throws Exception {
        var rows = new HashMap<String, JsonNode>();rows.put("work",identity("workId","work-id"));rows.put("assessment",identity("id","assessment-id"));
        rows.put("after-db",captured("""
          {"rawRows":{"assessments":[{"id":"assessment-id","definitionVersion":"definition-v1","policyVersion":"SYNTHETIC-policy-v1","evaluatorVersion":"evaluator-v1"}]}}
          """));
        var a=assertion("T12","conflicting-claims","versions-pinned");engine.check(a,rows,aliases);
        ((ObjectNode)rows.get("after-db").at("/data/rawRows/assessments/0")).put("evaluatorVersion","evaluator-v2");
        assertThrows(AssertionError.class, () -> engine.check(a,rows,aliases));
    }
    @Test void failedLinkCannotHideOtherOwnerAssignment() throws Exception {
        var rows = new HashMap<String, JsonNode>();
        rows.put("anomaly", identity("occurrenceId", "anomaly-id"));
        rows.put("after-db", captured("""
          {"rawRows":{"assignments":[{"sourceOccurrenceId":"anomaly-id","status":"OPEN",
          "ownerId":"human-intake","nextAction":"온도 이상 원자료 확인","nextCheckAt":"2026-10-08T09:00:00Z"}]}}
          """));
        var count = assertion("C5","failed-link","intake-current-assignment1");
        var owner = assertion("C5","failed-link","intake-assignment-owner-next-check");
        engine.check(count,rows,aliases);engine.check(owner,rows,aliases);
        var assignments = (com.fasterxml.jackson.databind.node.ArrayNode) rows.get("after-db").at("/data/rawRows/assignments");
        assignments.add(assignments.get(0).deepCopy());
        ((ObjectNode)assignments.get(1)).put("ownerId","human-b");
        assertThrows(AssertionError.class, () -> engine.check(count,rows,aliases));
        assignments.remove(1);
        ((ObjectNode)assignments.get(0)).put("ownerId","human-b");
        engine.check(count,rows,aliases);
        assertThrows(AssertionError.class, () -> engine.check(owner,rows,aliases));
    }
    @Test void unavailableAndIncompleteScopeNeverBecomeZeroEffectPass() throws Exception {
        var rows=assignmentSample();var a=assertion("T10","link-failure","no-target-assignment");engine.check(a,rows,aliases);
        ((ObjectNode)rows.get("after-db").path("provenance")).put("scopeComplete",false);
        assertThrows(AssertionError.class, () -> engine.check(a,rows,aliases));
        ((ObjectNode)rows.get("after-db")).put("driverStatus","NOT_IMPLEMENTED");
        assertThrows(AssertionError.class, () -> engine.check(a,rows,aliases));
    }
}
