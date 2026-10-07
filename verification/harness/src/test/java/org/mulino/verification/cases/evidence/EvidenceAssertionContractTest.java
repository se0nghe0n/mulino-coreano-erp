package org.mulino.verification.cases.evidence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mulino.verification.*;
import static org.junit.jupiter.api.Assertions.*;

/** Captured assertion inputs only. No product driver, evaluator, DB or state transitions. */
public final class EvidenceAssertionContractTest {
    private final Path root=Path.of(System.getProperty("repo.root"));
    private final AssertionEngine engine=new AssertionEngine();

    private JsonNode assertion(String caseId,String sub,String id) throws Exception {
        for(JsonNode s:Json.read(root.resolve("verification/cases/"+caseId+"/case.json")).path("subcases"))
            if(sub.equals(s.path("id").asText()))
                for(JsonNode a:s.path("assertions")) if(id.equals(a.path("id").asText())) return a;
        throw new AssertionError("No real case assertion "+caseId+"/"+sub+"/"+id);
    }
    private JsonNode aliases() {
        ObjectNode a=Json.object();
        for(String id:List.of("ORG-A","ORG-B","P","intake","supervisor","warehouse","readAgent","doc")) a.put(id,"captured-"+id);
        return a;
    }
    private JsonNode captured(String payload) {
        ObjectNode n=(ObjectNode)Json.parse(payload);
        n.put("driverStatus","EXECUTED");
        n.set("provenance",Json.parse("{\"scopeComplete\":true,\"source\":\"CANNED_CONTRACT_SELFTEST\"}"));
        return n;
    }
    private void accepts(JsonNode a,Map<String,JsonNode> rows) { engine.check(a,rows,aliases()); }
    private void rejects(JsonNode a,Map<String,JsonNode> rows) { assertThrows(AssertionError.class,()->accepts(a,rows)); }

    @Test void thenKnownRejectsChangedQuantityAndUnit() throws Exception {
        JsonNode a=assertion("T06","known-at-correction","then-again-quantity");
        accepts(a,Map.of("then-again",captured("{\"response\":{\"data\":{\"quantity\":\"100\",\"unit\":\"BOX\"}}}")));
        rejects(a,Map.of("then-again",captured("{\"response\":{\"data\":{\"quantity\":\"98\",\"unit\":\"BOX\"}}}")));
        rejects(a,Map.of("then-again",captured("{\"response\":{\"data\":{\"quantity\":\"100\",\"unit\":\"EA\"}}}")));
    }
    @Test void originalRevisionCannotBeOverwritten() throws Exception {
        JsonNode a=assertion("T06","known-at-correction","original-immutable");
        JsonNode original=captured("{\"response\":{\"data\":{\"quantity\":\"100\",\"revision\":1,\"unit\":\"BOX\"}}}");
        accepts(a,Map.of("then",original,"then-again",original.deepCopy()));
        JsonNode changed=captured("{\"response\":{\"data\":{\"quantity\":\"98\",\"revision\":1,\"unit\":\"BOX\"}}}");
        rejects(a,Map.of("then",original,"then-again",changed));
    }
    @Test void utcEqualityDoesNotEraseOriginalOffset() throws Exception {
        JsonNode instant=assertion("T06","known-at-correction","utc-instant");
        JsonNode offset=assertion("T06","known-at-correction","preserve-originalOffset");
        JsonNode input=captured("{\"response\":{\"data\":{\"occurredAt\":\"2026-10-05T09:00:00+09:00\",\"originalOffset\":\"+09:00\"}}}");
        accepts(instant,Map.of("current",input));accepts(offset,Map.of("current",input));
        ((ObjectNode)input.path("response").path("data")).put("originalOffset","+00:00");
        accepts(instant,Map.of("current",input));rejects(offset,Map.of("current",input));
    }
    @Test void missingStateMustNotAcquireKnownZero() throws Exception {
        JsonNode a=assertion("T06","four-states","state-0-not-known-zero");
        accepts(a,Map.of("states",captured("{\"response\":{\"data\":{\"observedAttributes\":[{\"state\":\"MISSING\"}]}}}")));
        rejects(a,Map.of("states",captured("{\"response\":{\"data\":{\"observedAttributes\":[{\"state\":\"MISSING\",\"knownQuantity\":\"0\"}]}}}")));
        rejects(a,Map.of("states",captured("{\"response\":{\"data\":{\"observedAttributes\":[{\"state\":\"MISSING\",\"knownQuantity\":null}]}}}")));
    }
    @Test void conflictCannotDisappearAlongsideSatisfiedAny() throws Exception {
        JsonNode a=assertion("T06","any-true-conflict","predicate-conflict");
        accepts(a,Map.of("assessment",captured("{\"response\":{\"data\":{\"result\":\"SATISFIED\",\"conflict\":true}}}")));
        rejects(a,Map.of("assessment",captured("{\"response\":{\"data\":{\"result\":\"SATISFIED\",\"conflict\":false}}}")));
    }
    @Test void canonicalQuantityAndPhysicalCountAreIndependent() throws Exception {
        JsonNode sum=assertion("T22","two-sources-one-occurrence","receipt-sum");
        JsonNode count=assertion("T22","two-sources-one-occurrence","one-physical-receipt");
        JsonNode one=captured("{\"data\":{\"rawRows\":{\"movements\":[{\"kind\":\"RECEIPT\",\"quantity\":\"60\",\"unit\":\"BOX\"}]}}}");
        accepts(sum,Map.of("db-after",one));accepts(count,Map.of("db-after",one));
        JsonNode duplicate=captured("{\"data\":{\"rawRows\":{\"movements\":[{\"kind\":\"RECEIPT\",\"quantity\":\"30\",\"unit\":\"BOX\"},{\"kind\":\"RECEIPT\",\"quantity\":\"30\",\"unit\":\"BOX\"}]}}}");
        accepts(sum,Map.of("db-after",duplicate));rejects(count,Map.of("db-after",duplicate));
        ((ObjectNode)one.at("/data/rawRows/movements/0")).put("quantity","120");rejects(sum,Map.of("db-after",one));
    }
    @Test void conflictOwnerDeadlineAndAssignmentCountMustAllHold() throws Exception {
        JsonNode owner=assertion("T22","source-key-hash-conflict","EVIDENCE_CONFLICT-responsibility");
        JsonNode count=assertion("T22","source-key-hash-conflict","EVIDENCE_CONFLICT-one-assignment");
        JsonNode rows=captured("{\"data\":{\"rawRows\":{\"assignments\":[{\"kind\":\"EVIDENCE_CONFLICT\",\"status\":\"OPEN\",\"ownerId\":\"captured-intake\",\"nextAction\":\"원천·실물 범위 대조\",\"nextCheckAt\":\"2026-10-07T02:00:00Z\"}]}}}");
        accepts(owner,Map.of("db-after",rows));accepts(count,Map.of("db-after",rows));
        ObjectNode row=(ObjectNode)rows.at("/data/rawRows/assignments/0");
        row.remove("ownerId");rejects(owner,Map.of("db-after",rows));
        row.put("ownerId","captured-intake").put("nextCheckAt","2026-10-07T03:00:00Z");rejects(owner,Map.of("db-after",rows));
    }
    @Test void unknownExternalCannotProduceSecondRemoteRequest() throws Exception {
        JsonNode a=assertion("T22","external-unknown","remote-request-count");
        accepts(a,Map.of("remote-after",captured("{\"data\":{\"requests\":[{\"externalOperationId\":\"captured-EXT-1\"}]}}")));
        rejects(a,Map.of("remote-after",captured("{\"data\":{\"requests\":[{\"externalOperationId\":\"captured-EXT-1\"},{\"externalOperationId\":\"captured-EXT-1\"}]}}")));
    }
    @ParameterizedTest @ValueSource(strings={"domain_records","movements","allocations","obligations","assignments","outbox","command_records"})
    void everyAuditRollbackAxisRejectsOneCommittedRow(String table) throws Exception {
        JsonNode a=assertion("T24","audit-rollback-and-retry",table+"-unchanged");
        ObjectNode original=(ObjectNode)captured("{\"data\":{\"rawRows\":{}}}");
        ((ObjectNode)original.at("/data/rawRows")).set(table,Json.parse("[{\"id\":\"before\",\"revision\":1}]"));
        JsonNode after=original.deepCopy();accepts(a,Map.of("db-before",original,"db-after",after));
        ((com.fasterxml.jackson.databind.node.ArrayNode)after.at("/data/rawRows/"+table)).add(Json.parse("{\"id\":\"forbidden-new-effect\",\"revision\":1}"));
        rejects(a,Map.of("db-before",original,"db-after",after));
    }
    @Test void aDeletedBlobMustNotReturnAfterRestore() throws Exception {
        JsonNode a=assertion("T24","restore-deleted","restored-blob-not-resurrected");
        JsonNode host=captured("{\"data\":{\"hostObservation\":{\"extractor\":{\"rawRows\":{\"blobs\":[{\"documentId\":\"captured-doc\",\"state\":\"NOT_PRESENT\"}]}}}}}");
        accepts(a,Map.of("inspect-restore",host));
        ((ObjectNode)host.at("/data/hostObservation/extractor/rawRows/blobs/0")).put("state","PRESENT");rejects(a,Map.of("inspect-restore",host));
    }
    @Test void legalHoldRejectsADeletionTombstone() throws Exception {
        JsonNode a=assertion("T24","legal-hold","tombstones-unchanged");
        JsonNode before=captured("{\"data\":{\"rawRows\":{\"tombstones\":[]}}}");
        accepts(a,Map.of("db-before",before,"db-after",before.deepCopy()));
        JsonNode deleted=captured("{\"data\":{\"rawRows\":{\"tombstones\":[{\"documentId\":\"captured-doc\",\"state\":\"DELETED\"}]}}}");
        rejects(a,Map.of("db-before",before,"db-after",deleted));
    }
    @Test void sentinelFindingRejectsNoLeakAssertion() throws Exception {
        JsonNode a=assertion("T24","sentinel-artifact-scan","surface-0-no-sentinel");
        JsonNode host=captured("{\"data\":{\"hostObservation\":{\"reads\":[{\"findings\":[]}]}}}");
        accepts(a,Map.of("scan",host));
        ((ObjectNode)host.at("/data/hostObservation/reads/0")).set("findings",Json.parse("[{\"patternId\":\"synthetic-secret\",\"byteOffset\":17,\"lengthBytes\":21}]"));
        rejects(a,Map.of("scan",host));
    }
    @Test void allNormativeObservationNamesAndPayloadHashesAreAccountedFor() throws Exception {
        Map<String,Set<String>> expected=new HashMap<>(), actual=new HashMap<>();
        for(JsonNode o:Json.read(root.resolve("verification/requirements/mandatory-oracles.json")).path("oracles"))
            if(Set.of("T06","T22","T24").contains(o.path("caseId").asText())) {
                Set<String> names=new HashSet<>();o.path("expectedObservations").forEach(n->names.add(n.path("name").asText()));expected.put(o.path("oracleId").asText(),names);
            }
        ContractValidator validator=new ContractValidator(root);
        for(String id:List.of("T06","T22","T24"))
            for(JsonNode sub:validator.caseFile(root.resolve("verification/cases/"+id+"/case.json")).path("subcases")) {
                for(JsonNode a:sub.path("assertions")) {
                    String oracle=a.path("oracleRef").path("oracleId").asText();
                    Set<String> names=actual.computeIfAbsent(oracle,k->new HashSet<>());
                    a.path("oracleRef").path("observationNames").forEach(n->names.add(n.asText()));
                }
                for(JsonNode doc:Json.read(root.resolve(sub.path("fixtureRef").asText())).path("baseline").path("documentInputs"))
                    assertEquals(doc.path("sha256").asText(),Json.sha256(root.resolve(doc.path("path").asText())));
            }
        assertEquals(expected,actual);
        assertEquals(38,actual.values().stream().mapToInt(Set::size).sum());
    }
}
