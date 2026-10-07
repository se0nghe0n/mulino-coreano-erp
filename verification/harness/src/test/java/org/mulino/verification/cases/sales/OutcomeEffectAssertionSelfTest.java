package org.mulino.verification.cases.sales;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mulino.verification.*;
import static org.junit.jupiter.api.Assertions.*;

/** SELFTEST: fixed captures test the real declared AssertionEngine contracts only.
 * No product, driver, evaluator, transaction or state transition is implemented. */
public final class OutcomeEffectAssertionSelfTest {
    private final Path root=Path.of(System.getProperty("repo.root"));
    private final AssertionEngine engine=new AssertionEngine();
    private final ObjectNode aliases=Json.parse("{\"ORG\":\"org-test\",\"P\":\"item-test\",\"A\":\"segment-a\",\"B\":\"segment-b\",\"correction-98-v2\":\"proof-v2\"}").deepCopy();

    private JsonNode subcase(String cid,String sub) throws Exception {
        for(JsonNode s:Json.read(root.resolve("verification/cases/"+cid+"/case.json")).path("subcases"))
            if(s.path("id").asText().equals(sub)) return s;
        throw new AssertionError("Missing actual case "+cid+"/"+sub);
    }
    private ObjectNode capture(String response,String data) {
        ObjectNode n=Json.object();n.put("driverStatus","EXECUTED");n.set("response",Json.parse(response));n.set("data",Json.parse(data));
        n.set("provenance",Json.parse("{\"scopeComplete\":true,\"independent\":true,\"source\":\"FIXED_ASSERTION_SELFTEST_NOT_PRODUCT\"}"));
        n.set("artifactRefs",Json.parse("[\"SELFTEST_CAPTURE_NOT_RUNTIME_EVIDENCE\"]"));return n;
    }
    private void checkAll(JsonNode sub,Map<String,JsonNode> rs) {
        for(JsonNode a:sub.path("assertions")) engine.check(a,rs,aliases);
    }
    private void checkOriginal(JsonNode sub,Map<String,JsonNode> rs,int originalCount) {
        for(int i=0;i<originalCount;i++) engine.check(sub.path("assertions").get(i),rs,aliases);
    }
    private void fails(JsonNode sub,Map<String,JsonNode> rs) {
        assertThrows(AssertionError.class,()->checkAll(sub,rs));
    }
    private Map<String,JsonNode> cloneResults(Map<String,JsonNode> rs) {
        var out=new HashMap<String,JsonNode>();rs.forEach((k,v)->out.put(k,v.deepCopy()));return out;
    }
    private ObjectNode row(Map<String,JsonNode> rs,String action,String source,int index) {
        return (ObjectNode)rs.get(action).path("data").path("rawRows").path(source).get(index);
    }
    private ObjectNode response(Map<String,JsonNode> rs,String action) {return (ObjectNode)rs.get(action).path("response");}
    private Map<String,JsonNode> c4(String status) {
        var rs=new HashMap<String,JsonNode>();
        rs.put("delivery",capture("{\"occurrenceId\":\"delivery-100\"}","{}"));
        rs.put("correct98",capture("{\"occurrenceId\":\"correct-98\"}","{}"));
        rs.put("resolution",capture("{\"revision\":9}","{}"));
        rs.put("reconfirm98",capture("{\"outcome\":\"APPLIED\",\"occurrenceId\":\"reconfirmed-98\",\"revision\":10,\"commandId\":\"reconfirm-command\",\"transactionId\":\"reconfirm-tx\"}","{}"));
        rs.put("after-reprocess",capture("{\"data\":{\"unresolvedDeficitQuantity\":\"0\",\"supportedDeliveredQuantity\":\"98\",\"unit\":\"BOX\"}}","{}"));
        ObjectNode before=capture("{}","{\"data\":{\"unit\":\"BOX\"},\"rawRows\":{\"deliveries\":[{\"id\":\"correct-98\",\"originalOccurrenceId\":\"delivery-100\",\"current\":true,\"canonical\":true,\"quantity\":\"98\",\"unit\":\"BOX\",\"sourceVersion\":\"1\"}],\"obligations\":[{\"id\":\"deficit-two\",\"rootId\":\"root-two\",\"ownerId\":\"sales-human\",\"status\":\"RESOLVED\",\"resolutionDecisionId\":\"decision-one\",\"kind\":\"DELIVERY_DEFICIT\",\"current\":true,\"quantity\":\"2\",\"unit\":\"BOX\"}]}}");
        ((ObjectNode)before.path("data").path("rawRows").path("obligations").get(0)).put("status",status);
        rs.put("before-reprocess-db",before);
        ObjectNode after=capture("{}","{\"data\":{\"unit\":\"BOX\"},\"rawRows\":{\"deliveries\":[{\"id\":\"correct-98\",\"originalOccurrenceId\":\"delivery-100\",\"current\":false,\"canonical\":true,\"quantity\":\"98\",\"unit\":\"BOX\",\"sourceVersion\":\"1\"},{\"id\":\"reconfirmed-98\",\"originalOccurrenceId\":\"delivery-100\",\"supersedesOccurrenceId\":\"correct-98\",\"current\":true,\"canonical\":true,\"quantity\":\"98\",\"unit\":\"BOX\",\"sourceEvidenceId\":\"proof-v2\",\"sourceNamespace\":\"SYNTHETIC-correction-98\",\"sourceVersion\":\"2\",\"lastCommandId\":\"reconfirm-command\",\"transactionId\":\"reconfirm-tx\"}],\"obligations\":[],\"decisions\":[{\"id\":\"decision-one\",\"deciderId\":\"sales-human\",\"evidenceId\":\"resolution-proof\",\"scopeId\":\"scope-two\",\"reason\":\"CUSTOMER_ACCEPTED_FINAL_SCOPE\",\"authorizedAt\":\"2026-10-07T09:00:01Z\"}],\"assessments\":[{\"id\":\"assessment-100\",\"current\":false,\"result\":\"SATISFIED\",\"inputSnapshotId\":\"snapshot-100\",\"evaluatorVersion\":\"evaluator-v1\"}]}}");
        ((ObjectNode)after.path("data").path("rawRows")).set("obligations",before.path("data").path("rawRows").path("obligations").deepCopy());
        rs.put("after-reprocess-db",after);
        rs.put("historical-db",capture("{}","{\"rawRows\":{\"assessments\":[{\"id\":\"assessment-100\",\"current\":true,\"result\":\"SATISFIED\",\"inputSnapshotId\":\"snapshot-100\",\"evaluatorVersion\":\"evaluator-v1\"}]}}"));
        return rs;
    }
    private Map<String,JsonNode> t17() {
        var rs=new HashMap<String,JsonNode>();
        rs.put("reserve",capture("{\"allocationId\":\"old-allocation\"}","{}"));
        rs.put("hold",capture("{\"restrictionId\":\"qc-hold\"}","{}"));
        rs.put("replace",capture("{\"outcome\":\"APPLIED\",\"allocationId\":\"new-allocation\",\"commandId\":\"replace-command\",\"transactionId\":\"replace-tx\"}","{}"));
        rs.put("release-hold",capture("{\"outcome\":\"APPLIED\",\"restrictionId\":\"qc-hold\"}","{}"));
        rs.put("allocation-release",capture("{\"outcome\":\"APPLIED\",\"commandId\":\"release-command\",\"transactionId\":\"release-tx\"}","{}"));
        ObjectNode suspended=capture("{}","{\"rawRows\":{\"allocations\":[{\"id\":\"old-allocation\",\"status\":\"SUSPENDED\",\"quantity\":\"20\",\"unit\":\"BOX\"}],\"obligations\":[{\"id\":\"duty-one\",\"rootId\":\"root-one\",\"scopeId\":\"scope-one\",\"ownerId\":\"sales-human\"}]}}");
        rs.put("suspended-db",suspended);
        ObjectNode replaced=capture("{}","{\"rawRows\":{\"allocations\":[{\"id\":\"old-allocation\",\"segmentId\":\"segment-a\",\"status\":\"REPLACED\",\"quantity\":\"20\",\"unit\":\"BOX\",\"lastCommandId\":\"replace-command\",\"transactionId\":\"replace-tx\"},{\"id\":\"new-allocation\",\"segmentId\":\"segment-b\",\"status\":\"EXECUTABLE\",\"quantity\":\"20\",\"unit\":\"BOX\",\"lastCommandId\":\"replace-command\",\"transactionId\":\"replace-tx\"}],\"restrictions\":[{\"id\":\"qc-hold\",\"segmentId\":\"segment-a\",\"reason\":\"QC_REVIEW\",\"status\":\"ACTIVE\",\"quantity\":\"20\",\"unit\":\"BOX\"}],\"obligations\":[]}}");
        ((ObjectNode)replaced.path("data").path("rawRows")).set("obligations",suspended.path("data").path("rawRows").path("obligations").deepCopy());
        rs.put("replaced-db",replaced);
        rs.put("released-db",replaced.deepCopy());row(rs,"released-db","restrictions",0).put("status","RELEASED");
        rs.put("replaced",capture("{\"data\":{\"effectLedger\":{\"allocations\":[\"old-allocation\",\"new-allocation\"]}}}","{}"));
        rs.put("released",capture("{\"data\":{\"oldAllocationStatus\":\"REPLACED\",\"newAllocationStatus\":\"EXECUTABLE\",\"executableReservedQuantity\":\"20\",\"unit\":\"BOX\",\"effectLedger\":{\"allocations\":[\"old-allocation\",\"new-allocation\"]}}}","{}"));
        rs.put("allocation-released-db",rs.get("released-db").deepCopy());
        row(rs,"allocation-released-db","allocations",1).put("status","RELEASED").put("lastCommandId","release-command").put("transactionId","release-tx");
        rs.put("allocation-released",capture("{\"data\":{\"executableReservedQuantity\":\"0\",\"unit\":\"BOX\"}}","{}"));
        return rs;
    }

    @Test void actualCaseSchemasAndFixtureVersionTwoAreValid() throws Exception {
        ContractValidator validator=new ContractValidator(root);
        for(String cid:List.of("C4","T17")) validator.caseFile(root.resolve("verification/cases/"+cid+"/case.json"));
        for(String suffix:List.of("resolved","waived")) {
            JsonNode fx=Json.read(root.resolve("verification/cases/C4/fixtures/resolved-no-resurrection-"+suffix+".json"));
            JsonNode old=null,current=null;
            for(JsonNode e:fx.path("evidence")) {
                if(e.path("alias").asText().equals("correction-98")) old=e;
                if(e.path("alias").asText().equals("correction-98-v2")) current=e;
            }
            assertNotNull(old);assertNotNull(current);assertEquals("2",current.path("sourceVersion").asText());
            assertNotEquals(old.path("sha256"),current.path("sha256"));
            assertEquals(old.path("externalEventId"),current.path("externalEventId"));
        }
    }
    @Test void resolvedAndWaivedRejectReconfirmRejectionAndAppliedNoop() throws Exception {
        for(String status:List.of("RESOLVED","WAIVED")) {
            JsonNode sub=subcase("C4","resolved-no-resurrection-"+status.toLowerCase(Locale.ROOT));
            var valid=c4(status);checkAll(sub,valid);
            var rejected=cloneResults(valid);response(rejected,"reconfirm98").put("outcome","REJECTED");
            ((ObjectNode)rejected.get("after-reprocess-db").path("data").path("rawRows")).set("deliveries",rejected.get("before-reprocess-db").path("data").path("rawRows").path("deliveries").deepCopy());
            checkOriginal(sub,rejected,6);fails(sub,rejected);
            var noop=cloneResults(valid);((ObjectNode)noop.get("after-reprocess-db").path("data").path("rawRows")).set("deliveries",noop.get("before-reprocess-db").path("data").path("rawRows").path("deliveries").deepCopy());checkOriginal(sub,noop,6);fails(sub,noop);
            var duplicateIdentity=cloneResults(valid);response(duplicateIdentity,"reconfirm98").put("occurrenceId","correct-98");fails(sub,duplicateIdentity);
            var unchangedRevision=cloneResults(valid);response(unchangedRevision,"reconfirm98").put("revision",9);fails(sub,unchangedRevision);
        }
    }
    @Test void reconfirmNeedsExactEffectiveQuantityAndProvenanceInItsTransaction() throws Exception {
        JsonNode sub=subcase("C4","resolved-no-resurrection-resolved");var valid=c4("RESOLVED");checkAll(sub,valid);
        for(var mutation:Map.of("quantity","100","unit","KG","sourceVersion","1","sourceEvidenceId","old-proof","originalOccurrenceId","other-delivery","supersedesOccurrenceId","delivery-100","lastCommandId","other-command","transactionId","other-tx").entrySet()) {
            var mutant=cloneResults(valid);row(mutant,"after-reprocess-db","deliveries",1).put(mutation.getKey(),mutation.getValue());fails(sub,mutant);
        }
        var doubleCurrent=cloneResults(valid);row(doubleCurrent,"after-reprocess-db","deliveries",0).put("current",true);fails(sub,doubleCurrent);
        var missing=cloneResults(valid);row(missing,"after-reprocess-db","deliveries",1).remove("sourceEvidenceId");fails(sub,missing);
        var apiMismatch=cloneResults(valid);((ObjectNode)response(apiMismatch,"after-reprocess").path("data")).put("supportedDeliveredQuantity","100");fails(sub,apiMismatch);
    }
    @Test void actualReconfirmRetainsOnlyExistingResolvedOrWaivedResponsibility() throws Exception {
        for(String status:List.of("RESOLVED","WAIVED")) {
            JsonNode sub=subcase("C4","resolved-no-resurrection-"+status.toLowerCase(Locale.ROOT));var valid=c4(status);checkAll(sub,valid);
            for(var mutation:Map.of("id","new-deficit","rootId","new-root","status","OPEN","ownerId","other-human","resolutionDecisionId","new-decision").entrySet()) {
                var mutant=cloneResults(valid);row(mutant,"after-reprocess-db","obligations",0).put(mutation.getKey(),mutation.getValue());fails(sub,mutant);
            }
        }
    }
    @Test void replacementMustApplyBeforeTheNoRevivalChecks() throws Exception {
        JsonNode sub=subcase("T17","replacement-no-revival");var valid=t17();checkAll(sub,valid);
        var rejected=cloneResults(valid);response(rejected,"replace").put("outcome","REJECTED");fails(sub,rejected);
        var sameUnrelatedCommand=cloneResults(valid);
        row(sameUnrelatedCommand,"replaced-db","allocations",0).put("lastCommandId","old-command");
        row(sameUnrelatedCommand,"replaced-db","allocations",1).put("lastCommandId","old-command");fails(sub,sameUnrelatedCommand);
        var wrongReplacement=cloneResults(valid);row(wrongReplacement,"replaced-db","allocations",1).put("segmentId","segment-a");fails(sub,wrongReplacement);
    }
    @Test void qcReleaseRejectedNoopAndWrongRestrictionFailWithUnchangedAllocations() throws Exception {
        JsonNode sub=subcase("T17","replacement-no-revival");var valid=t17();checkAll(sub,valid);
        var rejected=cloneResults(valid);response(rejected,"release-hold").put("outcome","REJECTED");row(rejected,"released-db","restrictions",0).put("status","ACTIVE");checkOriginal(sub,rejected,14);fails(sub,rejected);
        var noop=cloneResults(valid);row(noop,"released-db","restrictions",0).put("status","ACTIVE");checkOriginal(sub,noop,14);fails(sub,noop);
        var wrongTarget=cloneResults(valid);response(wrongTarget,"release-hold").put("restrictionId","other-hold");fails(sub,wrongTarget);
        var wrongScope=cloneResults(valid);row(wrongScope,"released-db","restrictions",0).put("segmentId","segment-b");fails(sub,wrongScope);
        var oldRevival=cloneResults(valid);row(oldRevival,"released-db","allocations",0).put("status","EXECUTABLE");fails(sub,oldRevival);
    }
    @Test void replacementAllocationReleaseMustHaveAnActualEffectAndZeroExecutableTotal() throws Exception {
        JsonNode sub=subcase("T17","replacement-no-revival");var valid=t17();checkAll(sub,valid);
        var rejected=cloneResults(valid);response(rejected,"allocation-release").put("outcome","REJECTED");fails(sub,rejected);
        var noop=cloneResults(valid);row(noop,"allocation-released-db","allocations",1).put("status","EXECUTABLE");checkOriginal(sub,noop,14);fails(sub,noop);
        var unrelated=cloneResults(valid);row(unrelated,"allocation-released-db","allocations",1).put("transactionId","other-tx");fails(sub,unrelated);
        var revived=cloneResults(valid);row(revived,"allocation-released-db","allocations",0).put("status","EXECUTABLE");fails(sub,revived);
        var total=cloneResults(valid);((ObjectNode)response(total,"allocation-released").path("data")).put("executableReservedQuantity","20");fails(sub,total);
    }
}
