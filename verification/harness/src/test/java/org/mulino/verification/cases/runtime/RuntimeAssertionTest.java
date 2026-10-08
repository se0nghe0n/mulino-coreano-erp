package org.mulino.verification.cases.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.mulino.verification.AssertionEngine;
import org.mulino.verification.ContractValidator;
import org.mulino.verification.Json;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Fixed input samples for the actual case assertions. No worker, clock or product driver. */
public final class RuntimeAssertionTest {
    private final Path root = Path.of(System.getProperty("repo.root"));
    private final AssertionEngine engine = new AssertionEngine();
    private final JsonNode aliases = Json.parse("{\"ORG\":\"org-runtime-sample\",\"owner\":\"human-owner\",\"Q20\":\"actual-Q20\",\"DOC-TEMP\":\"actual-temp-doc\"}");

    private JsonNode assertion(String caseId, String subcaseId, String assertionId) throws Exception {
        for (JsonNode sub : Json.read(root.resolve("verification/cases/" + caseId + "/case.json")).path("subcases")) {
            if (sub.path("id").asText().equals(subcaseId)) {
                for (JsonNode a : sub.path("assertions")) if (a.path("id").asText().equals(assertionId)) return a;
            }
        }
        throw new IllegalArgumentException("Missing real case assertion: " + assertionId);
    }
    private ObjectNode capture(String rawRows, String data, String response) {
        ObjectNode result = Json.object();
        result.put("driverStatus", "EXECUTED");
        result.set("provenance", Json.parse("{\"scopeComplete\":true,\"source\":\"CANNED_ASSERTION_SELFTEST\"}"));
        ObjectNode body = Json.object(); body.set("rawRows", Json.parse(rawRows)); body.set("data", Json.parse(data));
        result.set("data", body); result.set("response", Json.parse(response));
        return result;
    }
    private Map<String, JsonNode> sample(String rawRows) {
        return new HashMap<>(Map.of("db", capture(rawRows, "{}", "{}")));
    }
    private void accepts(JsonNode a, Map<String, JsonNode> sample) {
        assertDoesNotThrow(() -> engine.check(a, sample, aliases));
    }
    private void rejects(JsonNode a, Map<String, JsonNode> sample) {
        assertThrows(AssertionError.class, () -> engine.check(a, sample, aliases));
    }
    @Test void allRuntimeContractsValidateAndEveryAssignedObservationIsLinked() throws Exception {
        var validator = new ContractValidator(root);
        Set<String> actual = new HashSet<>(), required = new HashSet<>();
        int subcases = 0;
        for (String id : List.of("T26", "V5")) {
            JsonNode c = validator.caseFile(root.resolve("verification/cases/" + id + "/case.json"));
            subcases += c.path("subcases").size();
            for (JsonNode sub : c.path("subcases")) for (JsonNode a : sub.path("assertions"))
                for (JsonNode name : a.path("oracleRef").path("observationNames"))
                    actual.add(a.path("oracleRef").path("oracleId").asText() + ":" + name.asText());
        }
        for (JsonNode oracle : Json.read(root.resolve("verification/requirements/mandatory-oracles.json")).path("oracles")) {
            if (Set.of("T26", "V5").contains(oracle.path("caseId").asText()))
                for (JsonNode observation : oracle.path("expectedObservations"))
                    required.add(oracle.path("oracleId").asText() + ":" + observation.path("name").asText());
        }
        assertEquals(29, subcases); assertEquals(14, required.size()); assertEquals(required, actual);
    }
    @Test void retryCarriesNoCallerIdentityAndAutonomousLoopsHaveNoHarnessTrigger() throws Exception {
        boolean forgedActor = false, forgedHash = false; int autonomous = 0;
        for (JsonNode sub : Json.read(root.resolve("verification/cases/T26/case.json")).path("subcases")) {
            String id = sub.path("id").asText();
            List<JsonNode> flat = new ArrayList<>();
            for (JsonNode a : sub.path("actions")) { flat.add(a); for (JsonNode b : a.path("branches")) b.path("actions").forEach(flat::add); }
            if (id.endsWith("-autonomous-loop")) {
                // The passive watcher and the loop's process starts run in one parallel action, watcher branch first.
                JsonNode group = null; for (JsonNode a : sub.path("actions")) if (a.path("kind").asText().equals("parallel")) group = a;
                assertNotNull(group, id);
                assertTrue(Set.of("tickScheduler", "sweepDue").contains(group.at("/branches/0/actions/0/control/operation").asText()), id);
                for (JsonNode a : group.at("/branches/1/actions")) assertEquals("start", a.path("control").path("operation").asText(), id);
            }
            for (JsonNode a : flat) {
                if (a.path("capabilityId").asText().equals("retrySafeCommand")) {
                    JsonNode r = a.path("request");
                    assertTrue(r.path("slots").has("commandId"), id);
                    assertEquals(id.equals("safe-retry-forged-original-actor"), r.has("originalActorId"), id);
                    assertEquals(id.equals("safe-retry-forged-request-hash"), r.has("canonicalRequestHash"), id);
                    forgedActor |= r.has("originalActorId"); forgedHash |= r.has("canonicalRequestHash");
                }
                String op = a.path("control").path("operation").asText();
                if (id.endsWith("-autonomous-loop") && Set.of("tickScheduler", "sweepDue").contains(op)) {
                    assertEquals("OBSERVE_NEXT_NATURAL_TICK", a.path("control").path("parameters").path("trigger").asText(), id);
                    assertFalse(a.path("control").path("parameters").has("clockInstant"), id);
                }
            }
            if (id.endsWith("-autonomous-loop")) { autonomous++; assertNotNull(assertion("T26", id, "autonomous-within-30s")); }
        }
        assertTrue(forgedActor && forgedHash); assertEquals(3, autonomous);
        JsonNode within = assertion("T26", "due-wait-autonomous-loop", "autonomous-within-30s");
        ObjectNode tick = Json.object(); tick.put("driverStatus", "EXECUTED"); tick.set("provenance", Json.parse("{\"scopeComplete\":true}"));
        tick.set("data", Json.parse("{\"hostObservation\":{\"operationEvidence\":{\"submittedAt\":\"2026-10-08T00:00:20Z\"}}}"));
        ObjectNode start = tick.deepCopy(); start.set("data", Json.parse("{\"hostObservation\":{\"command\":{\"startedAt\":\"2026-10-08T00:00:00Z\"}}}"));
        var results = new java.util.HashMap<String, JsonNode>(); results.put("tick", tick); results.put("start-again-scheduler", start);
        engine.check(within, results, aliases);
        ((ObjectNode) tick.at("/data/hostObservation/operationEvidence")).put("submittedAt", "2026-10-08T00:00:31Z");
        assertThrows(AssertionError.class, () -> engine.check(within, results, aliases));
        // A submission before the scheduler start command began cannot be the restarted loop's own tick.
        ((ObjectNode) tick.at("/data/hostObservation/operationEvidence")).put("submittedAt", "2026-10-07T23:59:59Z");
        assertThrows(AssertionError.class, () -> engine.check(within, results, aliases));
    }
    @Test void everyFixtureArtifactMatchesActualBytesAndDigest() throws Exception {
        for (String id : List.of("T26", "V5")) {
            for (JsonNode sub : Json.read(root.resolve("verification/cases/" + id + "/case.json")).path("subcases")) {
                JsonNode fixture = Json.read(root.resolve(sub.path("fixtureRef").asText()));
                for (JsonNode artifact : fixture.path("baseline").path("fixtureArtifacts")) {
                    Path file = root.resolve(artifact.path("path").asText());
                    assertEquals(artifact.path("sha256").asText(), Json.sha256(file));
                    assertEquals(artifact.path("sizeBytes").asLong(), java.nio.file.Files.size(file));
                }
                assertFalse(fixture.path("baseline").path("uncommittedFailureOrRecoveredStateSeeded").asBoolean());
                assertFalse(fixture.path("baseline").path("installationRules").path("createPendingRecoveryEffects").asBoolean());
            }
        }
    }
    @Test void expiryQuantityRejectsWrongTotalAndWrongUnit() throws Exception {
        JsonNode a = assertion("T26", "lot-expiry-no-event", "held-20");
        var rows = sample("{\"segments\":[{\"quantity\":\"8\",\"unit\":\"BOX\"},{\"quantity\":\"12\",\"unit\":\"BOX\"}]}");
        accepts(a, rows);
        ObjectNode row = (ObjectNode) rows.get("db").at("/data/rawRows/segments/1");
        row.put("quantity", "13"); rejects(a, rows); row.put("quantity", "12");
        row.put("unit", "KG"); rejects(a, rows);
    }
    @Test void zeroEffectRequiresBothActualUnitsAndBothSnapshots() throws Exception {
        JsonNode a = assertion("T26", "lot-expiry-no-event", "guard-db-quantity-delta-zero");
        var rows = new HashMap<String, JsonNode>();
        rows.put("before-db", capture("{}", "{\"heldQuantity\":\"20\",\"unit\":\"BOX\"}", "{}"));
        rows.put("guard-db", capture("{}", "{\"heldQuantity\":\"20\",\"unit\":\"BOX\"}", "{}"));
        accepts(a, rows);
        ((ObjectNode) rows.get("before-db").at("/data/data")).put("unit", "KG"); rejects(a, rows);
        ((ObjectNode) rows.get("before-db").at("/data/data")).put("unit", "BOX");
        rows.remove("before-db"); rejects(a, rows);
    }
    @Test void staleWorkerCommittedRowIsRejectedEvenWhenWinnerCommittedOnce() throws Exception {
        JsonNode a = assertion("V5", "two-workers-expired-fence", "no-stale-commit");
        var rows = sample("{\"commands\":[{\"committingWorkerId\":\"worker-b\",\"status\":\"COMMITTED\",\"effectClass\":\"DUE_WAIT_CONTINUATION\"}]}");
        accepts(a, rows);
        ((ArrayNode) rows.get("db").at("/data/rawRows/commands")).add(Json.parse("{\"committingWorkerId\":\"worker-a\",\"status\":\"COMMITTED\",\"effectClass\":\"DUE_WAIT_CONTINUATION\"}"));
        rejects(a, rows);
    }
    @Test void duplicateContinuationIsRejected() throws Exception {
        JsonNode a = assertion("V5", "repeated-crash-no-duplicate", "exactly-one-commit");
        var rows = sample("{\"commands\":[{\"status\":\"COMMITTED\",\"effectClass\":\"DUE_WAIT_CONTINUATION\"}]}");
        accepts(a, rows);
        ArrayNode commands = (ArrayNode) rows.get("db").at("/data/rawRows/commands");
        commands.add(commands.get(0).deepCopy()); rejects(a, rows);
    }
    @Test void duplicatePhysicalScopeFailsWithoutRelyingOnQuantitySum() throws Exception {
        JsonNode a = assertion("V5", "repeated-crash-no-duplicate", "no-physical-duplication");
        var rows = sample("{\"segments\":[{\"physicalScope\":\"q8\"},{\"physicalScope\":\"q12\"}]}");
        accepts(a, rows);
        ((ObjectNode) rows.get("db").at("/data/rawRows/segments/1")).put("physicalScope", "q8"); rejects(a, rows);
    }
    @Test void ownerIdentityMustBeTheAssignedHumanAndCannotBeMissing() throws Exception {
        JsonNode a = assertion("V5", "bounded-retry-exhausted-owner", "db-owner");
        var rows = sample("{\"assignments\":[{\"current\":true,\"ownerId\":\"human-owner\"}]}");
        accepts(a, rows);
        ObjectNode owner = (ObjectNode) rows.get("db").at("/data/rawRows/assignments/0");
        owner.put("ownerId", "worker-a"); rejects(a, rows); owner.remove("ownerId"); rejects(a, rows);
    }
    @Test void multipleCurrentAssignmentsCannotPassAsOneResponsibility() throws Exception {
        JsonNode a = assertion("V5", "bounded-retry-exhausted-owner", "db-current-assignment-one");
        var rows = sample("{\"assignments\":[{\"current\":true}]}"); accepts(a, rows);
        ((ArrayNode) rows.get("db").at("/data/rawRows/assignments")).add(Json.parse("{\"current\":true}")); rejects(a, rows);
    }
    @Test void causalRevisionDefinitionAndAttemptRemainDistinct() throws Exception {
        JsonNode a = assertion("V5", "two-workers-expired-fence", "distinct-version-attempt-tuple");
        var rows = sample("{\"attempts\":[{\"deliveryAttempt\":1,\"eventRevision\":1,\"definitionVersion\":\"definition-v1\"},{\"deliveryAttempt\":2,\"eventRevision\":1,\"definitionVersion\":\"definition-v1\"}]}");
        accepts(a, rows);
        ObjectNode attempt = (ObjectNode) rows.get("db").at("/data/rawRows/attempts/1");
        attempt.put("eventRevision", 2); rejects(a, rows); attempt.put("eventRevision", 1);
        attempt.put("definitionVersion", "definition-v2"); rejects(a, rows);
    }
    @Test void commitFenceMustMatchCurrentClaimIdentity() throws Exception {
        JsonNode a = assertion("V5", "two-workers-expired-fence", "new-fence-used-at-commit");
        var rows = sample("{\"attempts\":[{}, {\"fencingToken\":\"fence-b\"}],\"claims\":[{}, {\"fencingToken\":\"fence-b\"}]}");
        accepts(a, rows);
        ((ObjectNode) rows.get("db").at("/data/rawRows/attempts/1")).put("fencingToken", "stale-fence-a"); rejects(a, rows);
    }
    @Test void thirtySecondLimitUsesTerminalTimeRatherThanSubmissionAck() throws Exception {
        JsonNode a = assertion("V5", "waiting-full-restart", "restart-within30");
        var rows = new HashMap<String, JsonNode>();
        rows.put("restart-all-worker-b", capture("{}", "{}", "{}"));
        ((ObjectNode) rows.get("restart-all-worker-b").path("data")).set("hostObservation", Json.parse("{\"processObservation\":{\"completedAt\":\"2026-10-07T09:00:00Z\"}}"));
        rows.put("terminal", capture("{}", "{}", "{}"));
        ((ObjectNode) rows.get("terminal").path("data")).set("hostObservation", Json.parse("{\"runtimeTask\":{\"completedAt\":\"2026-10-07T09:00:30Z\"}}"));
        accepts(a, rows);
        ((ObjectNode) rows.get("terminal").at("/data/hostObservation/runtimeTask")).put("completedAt", "2026-10-07T09:00:31Z"); rejects(a, rows);
    }
    @Test void queueSuccessCannotManufactureSatisfiedAssessment() throws Exception {
        JsonNode a = assertion("V5", "waiting-full-restart", "no-satisfied-assessment");
        var rows = sample("{\"assessments\":[{\"result\":\"UNVERIFIED\"}]}"); accepts(a, rows);
        ((ObjectNode) rows.get("db").at("/data/rawRows/assessments/0")).put("result", "SATISFIED"); rejects(a, rows);
    }
    @Test void missingAndUnknownQuantitiesAreNeverZeroEffects() throws Exception {
        JsonNode a = assertion("T26", "lot-expiry-no-event", "held-20");
        var rows = sample("{\"segments\":[{\"quantity\":\"20\",\"unit\":\"BOX\"}]}"); accepts(a, rows);
        ObjectNode row = (ObjectNode) rows.get("db").at("/data/rawRows/segments/0");
        row.put("quantity", "UNKNOWN"); rejects(a, rows); row.remove("quantity"); rejects(a, rows);
    }
    @Test void absentRawSourceCannotPassZeroStaleCommit() throws Exception {
        JsonNode a = assertion("V5", "two-workers-expired-fence", "no-stale-commit");
        var rows = sample("{\"commands\":[]}"); accepts(a, rows);
        ((ObjectNode) rows.get("db").at("/data/rawRows")).remove("commands"); rejects(a, rows);
        ((ObjectNode) rows.get("db").path("provenance")).put("scopeComplete", false); rejects(a, rows);
    }
    @Test void missingEvaluatorMustKeepRestoreIncomplete() throws Exception {
        JsonNode a = assertion("T26", "restore-missing-v1-evaluator", "restore-completeness");
        ObjectNode capture = capture("{}", "{}", "{}");
        ((ObjectNode) capture.path("data")).set("hostObservation", Json.read(root.resolve("verification/cases/T26/selftest/host-shaped-results.json")).path("restore").path("data").path("hostObservation").deepCopy());
        ((ObjectNode) capture.at("/data/hostObservation/extractor/rawRows/restoreResult")).put("status", "INCOMPLETE");
        var rows = new HashMap<String, JsonNode>(Map.of("restore", capture)); accepts(a, rows);
        ((ObjectNode) capture.at("/data/hostObservation/extractor/rawRows/restoreResult")).put("status", "COMPLETE"); rejects(a, rows);
    }
    @Test void v1EvaluatorAndDefinitionCannotBeSilentlyReplaced() throws Exception {
        JsonNode a = assertion("T26", "restore-complete", "v1-evaluator");
        var rows = sample("{\"assessments\":[{\"evaluatorVersion\":\"evaluator-v1\"}]}"); accepts(a, rows);
        ((ObjectNode) rows.get("db").at("/data/rawRows/assessments/0")).put("evaluatorVersion", "evaluator-v2"); rejects(a, rows);
    }
    @Test void realBackupBlobHashIsRequiredRatherThanPresence() throws Exception {
        JsonNode a = assertion("T26", "restore-complete", "blob-hash");
        String fixedHash = Json.sha256(root.resolve("verification/cases/T26/fixtures/artifacts/DOC-TEMP.json"));
        ObjectNode capture = capture("{}", "{}", "{}");
        ((ObjectNode) capture.path("data")).set("hostObservation", Json.read(root.resolve("verification/cases/T26/selftest/host-shaped-results.json")).path("restore").path("data").path("hostObservation").deepCopy());
        ((ObjectNode) capture.at("/data/hostObservation/extractor/rawRows/evidenceBlobs/0")).put("evidenceId", "actual-temp-doc").put("sha256", fixedHash);
        var rows = new HashMap<String, JsonNode>(Map.of("restore", capture)); accepts(a, rows);
        ((ObjectNode) capture.at("/data/hostObservation/extractor/rawRows/evidenceBlobs/0")).put("sha256", "0".repeat(64)); rejects(a, rows);
    }
    @Test void emergencyRepairDiffMustMatchTheActualBeforeAfterValues() throws Exception {
        JsonNode a = assertion("T26", "emergency-repair-dryrun-apply", "dryrun-diff");
        var rows = new HashMap<String, JsonNode>(Map.of("dry-run", capture("{}", "{}", "{\"data\":{\"diff\":[{\"table\":\"inventoryProjection\",\"targetId\":\"actual-Q20\",\"field\":\"sourceRevision\",\"before\":0,\"after\":1}]}}")));
        accepts(a, rows);
        ((ObjectNode) rows.get("dry-run").at("/response/data/diff/0")).put("after", 2); rejects(a, rows);
    }
}
