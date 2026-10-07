package org.mulino.verification.cases.definitions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mulino.verification.AssertionEngine;
import org.mulino.verification.ContractValidator;
import org.mulino.verification.Json;
import static org.junit.jupiter.api.Assertions.*;

/** 고정 관찰 표본으로 실제 assertion을 검사한다. 제품 adapter나 업무 evaluator가 아니다. */
class DefinitionsAssertionTest {
    static final Path ROOT = Path.of(System.getProperty("repo.root", "../.."));
    static final AssertionEngine ENGINE = new AssertionEngine();
    static void check(JsonNode a, Map<String,JsonNode> r) { ENGINE.check(a, r, Json.object().put("ORG", "org-uuid")); }
    static void check(JsonNode a, Map<String,JsonNode> r, JsonNode aliases) { ENGINE.check(a, r, aliases); }
    static JsonNode json(String value) throws Exception { return Json.MAPPER.readTree(value); }
    static JsonNode declaration(String cid, String sid, String aid) throws Exception {
        var c = Json.read(ROOT.resolve("verification/cases/" + cid + "/case.json"));
        for (var sub : c.path("subcases")) if (sub.path("id").asText().equals(sid)) {
            for (var a : sub.path("assertions")) if (a.path("id").asText().equals(aid)) return a;
        }
        throw new AssertionError("실제 case assertion이 없다: " + cid + "/" + sid + "/" + aid);
    }
    static ObjectNode observed(String body) throws Exception {
        var r = Json.object();
        r.put("driverStatus", "EXECUTED");
        r.set("provenance", json("{\"scopeComplete\":true,\"source\":\"CAPTURED_ASSERTION_SELFTEST\"}"));
        r.set("response", json(body));
        r.set("data", json(body));
        return r;
    }
    static Stream<String> files() { return Stream.of("T02", "T07", "T21", "V1"); }
    @ParameterizedTest @MethodSource("files")
    void declaredCasesAndFixturesMatchActualSchemas(String id) throws Exception {
        new ContractValidator(ROOT).caseFile(ROOT.resolve("verification/cases/" + id + "/case.json"));
    }
    @Test void receiptSixtyRejectsWrongQuantityAndUnit() throws Exception {
        var a = declaration("T07", "two-documents-one-receipt", "db-held-60");
        var correct = observed("{\"rawRows\":{\"segments\":[{\"quantity\":\"60\",\"unit\":\"BOX\"}]}}");
        assertDoesNotThrow(() -> check(a, Map.of("db-after", correct)));
        for (String wrong : new String[] {"{\"rawRows\":{\"segments\":[{\"quantity\":\"120\",\"unit\":\"BOX\"}]}}",
                "{\"rawRows\":{\"segments\":[{\"quantity\":\"60\",\"unit\":\"EA\"}]}}"}) {
            var mutant = observed(wrong);
            assertThrows(AssertionError.class, () -> check(a, Map.of("db-after", mutant)));
        }
    }
    @Test void duplicatePhysicalScopeIsRejectedEvenWhenSumIsSixty() throws Exception {
        var a = declaration("T07", "two-documents-one-receipt", "physical-identities-unique");
        var correct = observed("{\"rawRows\":{\"segments\":[{\"physicalScope\":\"physical60\",\"quantity\":\"60\"}]}}");
        var duplicate = observed("{\"rawRows\":{\"segments\":[{\"physicalScope\":\"physical60\",\"quantity\":\"30\"},{\"physicalScope\":\"physical60\",\"quantity\":\"30\"}]}}");
        assertDoesNotThrow(() -> check(a, Map.of("db-after", correct)));
        assertThrows(AssertionError.class, () -> check(a, Map.of("db-after", duplicate)));
    }
    @Test void unknownNotCannotBecomeSatisfiedAndConflictCannotDisappear() throws Exception {
        var notUnknown = declaration("T07", "predicate-truth-and-conflict", "not-unknown-api-result");
        assertDoesNotThrow(() -> check(notUnknown, Map.of("not-unknown", observed("{\"data\":{\"result\":\"UNVERIFIED\"}}"))));
        assertThrows(AssertionError.class, () -> check(notUnknown, Map.of("not-unknown", observed("{\"data\":{\"result\":\"SATISFIED\"}}"))));
        var conflict = declaration("T07", "predicate-truth-and-conflict", "not-conflict-api-conflict");
        assertDoesNotThrow(() -> check(conflict, Map.of("not-conflict", observed("{\"data\":{\"conflict\":true}}"))));
        assertThrows(AssertionError.class, () -> check(conflict, Map.of("not-conflict", observed("{\"data\":{\"conflict\":false}}"))));
    }
    @Test void repeatedTruthLeavesUseDistinctConditionIdentity() throws Exception {
        var a = declaration("T07", "predicate-truth-and-conflict", "all-true-input-state");
        var correct = observed("{\"rawRows\":{\"conditionResults\":[{\"conditionId\":\"all-true-operand-0\",\"inputState\":\"TRUE\"},{\"conditionId\":\"all-true-operand-1\",\"inputState\":\"TRUE\"}]}}");
        var duplicate = observed("{\"rawRows\":{\"conditionResults\":[{\"conditionId\":\"all-true-operand-0\",\"inputState\":\"TRUE\"},{\"conditionId\":\"all-true-operand-0\",\"inputState\":\"TRUE\"}]}}");
        assertDoesNotThrow(() -> check(a, Map.of("db-all-true", correct)));
        assertThrows(AssertionError.class, () -> check(a, Map.of("db-all-true", duplicate)));
    }
    @Test void oldEndpointAndEvaluatorCannotSilentlyUseVersionTwo() throws Exception {
        for (String cid : new String[] {"T21", "V1"}) {
            var endpoint = declaration(cid, "pinned-meaning-current-policy", "old-endpoint");
            var evaluator = declaration(cid, "pinned-meaning-current-policy", "old-evaluator");
            var good = observed("{\"data\":{\"goal\":{\"endpoint\":\"ARRIVED\"},\"evaluatorVersion\":\"arrival-v1\"}}");
            var wrong = observed("{\"data\":{\"goal\":{\"endpoint\":\"SELL_ELIGIBLE\"},\"evaluatorVersion\":\"eligibility-v2\"}}");
            assertDoesNotThrow(() -> check(endpoint, Map.of("old-assessment", good)));
            assertDoesNotThrow(() -> check(evaluator, Map.of("old-assessment", good)));
            assertThrows(AssertionError.class, () -> check(endpoint, Map.of("old-assessment", wrong)));
            assertThrows(AssertionError.class, () -> check(evaluator, Map.of("old-assessment", wrong)));
        }
    }
    @Test void unsupportedDutyIsScopedAndMissingOrWrongHumanOwnerFails() throws Exception {
        var owner = declaration("V1", "unsupported-v1-held", "current-duty-owner");
        var count = declaration("V1", "unsupported-v1-held", "current-duty-count");
        var aliases = json("{\"ORG\":\"org-uuid\",\"owner\":\"human-owner-uuid\"}");
        var good = observed("{\"rawRows\":{\"obligations\":[{\"kind\":\"ARRIVAL_REMAINDER\",\"current\":true,\"ownerId\":\"human-owner-uuid\"},{\"kind\":\"VERSION_UNSUPPORTED\",\"current\":true,\"ownerId\":\"human-owner-uuid\"}]}}");
        assertDoesNotThrow(() -> check(owner, Map.of("db-after", good), aliases));
        assertDoesNotThrow(() -> check(count, Map.of("db-after", good), aliases));
        for (String bad : new String[] {"{\"rawRows\":{\"obligations\":[{\"kind\":\"VERSION_UNSUPPORTED\",\"current\":true}]}}",
                "{\"rawRows\":{\"obligations\":[{\"kind\":\"VERSION_UNSUPPORTED\",\"current\":true,\"ownerId\":\"agent-uuid\"}]}}"}) {
            var mutant = observed(bad);
            assertThrows(AssertionError.class, () -> check(owner, Map.of("db-after", mutant), aliases));
        }
        var two = observed("{\"rawRows\":{\"obligations\":[{\"kind\":\"VERSION_UNSUPPORTED\",\"current\":true},{\"kind\":\"VERSION_UNSUPPORTED\",\"current\":true}]}}");
        assertThrows(AssertionError.class, () -> check(count, Map.of("db-after", two), aliases));
    }
    @Test void unchangedEffectRequiresBothSnapshotsAndRejectsNewAllocation() throws Exception {
        var a = declaration("V1", "unsupported-v1-held", "unchanged-allocations");
        var before = observed("{\"rawRows\":{\"allocations\":[]}}");
        var after = observed("{\"rawRows\":{\"allocations\":[]}}");
        assertDoesNotThrow(() -> check(a, Map.of("db-before", before, "db-after", after)));
        var changed = observed("{\"rawRows\":{\"allocations\":[{\"id\":\"new-allocation\",\"quantity\":\"1\",\"unit\":\"BOX\"}]}}");
        assertThrows(AssertionError.class, () -> check(a, Map.of("db-before", before, "db-after", changed)));
        assertThrows(AssertionError.class, () -> check(a, Map.of("db-after", after)));
    }
    @Test void unitDeltaChecksBaselineUnitIndependently() throws Exception {
        var a = declaration("T02", "conversion-no-substitution", "held-bundle-delta");
        var good = observed("{\"data\":{\"heldQuantity\":\"1\",\"unit\":\"BUNDLE\"}}");
        var wrongBefore = observed("{\"data\":{\"heldQuantity\":\"1\",\"unit\":\"EA\"}}");
        assertDoesNotThrow(() -> check(a, Map.of("before", good, "after", good)));
        assertThrows(AssertionError.class, () -> check(a, Map.of("before", wrongBefore, "after", good)));
    }
    @Test void publishedHashIsBoundToActualReviewedHash() throws Exception {
        var a = declaration("T21", "approved-lifecycle", "published-hash-is-reviewed");
        String h = "a".repeat(64), wrong = "b".repeat(64);
        var draft = observed("{\"hash\":\"" + h + "\"}");
        var good = observed("{\"hash\":\"" + h + "\"}");
        var bad = observed("{\"hash\":\"" + wrong + "\"}");
        assertDoesNotThrow(() -> check(a, Map.of("draft", draft, "publish", good)));
        assertThrows(AssertionError.class, () -> check(a, Map.of("draft", draft, "publish", bad)));
    }
    @Test void issuerItemsCannotReuseOneInternalId() throws Exception {
        var a = declaration("T02", "issuer-and-lot-context", "api-distinct-item-ids");
        var first = observed("{\"itemId\":\"item-a-uuid\"}");
        var second = observed("{\"itemId\":\"item-b-uuid\"}");
        assertDoesNotThrow(() -> check(a, Map.of("register-a", first, "register-b", second)));
        assertThrows(AssertionError.class, () -> check(a, Map.of("register-a", first, "register-b", first)));
    }
    @Test void receiptRowsDoNotTreatUnobservedQuantityAsZero() throws Exception {
        var a = declaration("T07", "two-documents-one-receipt", "db-held-60");
        for (String wrong : new String[] {"{\"rawRows\":{\"segments\":[]}}",
                "{\"rawRows\":{\"segments\":[{\"quantity\":\"UNKNOWN\",\"unit\":\"BOX\"}]}}"}) {
            var r = observed(wrong);
            assertThrows(AssertionError.class, () -> check(a, Map.of("db-after", r)));
        }
    }
}
