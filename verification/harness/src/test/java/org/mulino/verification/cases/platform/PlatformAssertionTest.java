package org.mulino.verification.cases.platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.mulino.verification.AssertionEngine;
import org.mulino.verification.Json;
import org.mulino.verification.PreparationValidator;
import org.mulino.verification.ContractValidator;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Fixed observations and mutations only. No product adapter, DB, worker or deploy is simulated. */
public final class PlatformAssertionTest {
    private final AssertionEngine engine=new AssertionEngine();
    private final Path root=Path.of(System.getProperty("repo.root"));
    private final Map<String,JsonNode> results=new HashMap<>();
    private final JsonNode aliases=Json.parse("{\"ORG\":\"org-id\",\"P\":\"item-id\",\"Q100\":\"parent-id\",\"A60\":\"segment-a\",\"B40\":\"segment-b\",\"LOT\":\"lot-id\",\"W\":\"warehouse-id\",\"TRANSIT\":\"transit-id\",\"DUTY\":\"duty-id\",\"procurement\":\"human-owner-id\",\"R60\":\"receipt-id\"}");
    private JsonNode assertion(String caseId,String sub,String id) throws Exception {
        for(JsonNode s:Json.read(root.resolve("verification/cases/"+caseId+"/case.json")).path("subcases"))
            if(s.path("id").asText().equals(sub)) for(JsonNode a:s.path("assertions")) if(a.path("id").asText().equals(id)) return a;
        throw new IllegalArgumentException("Missing authored assertion "+caseId+"/"+sub+"/"+id);
    }
    private ObjectNode observed(String action) {
        ObjectNode r=Json.object();r.put("driverStatus","EXECUTED");r.set("provenance",Json.parse("{\"scopeComplete\":true,\"source\":\"CANNED_ASSERTION_SELFTEST\"}"));
        r.set("data",Json.object());r.set("response",Json.object());results.put(action,r);return r;
    }
    private void put(String action,String pointer,JsonNode value) {
        ObjectNode r=(ObjectNode)results.get(action);if(r==null)r=observed(action);
        String[] pieces=pointer.substring(1).split("/");ObjectNode parent=r;
        for(int i=0;i<pieces.length-1;i++) {JsonNode child=parent.get(pieces[i]);if(child==null) {child=Json.object();parent.set(pieces[i],child);}parent=(ObjectNode)child;}
        parent.set(pieces[pieces.length-1],value);
    }
    private void scalar(String action,String pointer,String value) {put(action,pointer,Json.MAPPER.valueToTree(value));}
    private void check(JsonNode a) {engine.check(a,results,aliases);}
    private void fact(String action,String key,JsonNode value) {put(action,"/data/hostObservation/extractor/rawRows/facts/"+key,value);}
    @Test void everyAuthoredCaseHasExactKoreanStepsAndValidSchema() throws Exception {
        var v=new ContractValidator(root);var p=new PreparationValidator(root);
        for(String id:List.of("T23","V8")) {Path file=root.resolve("verification/cases/"+id+"/case.json");JsonNode c=v.caseFile(file);assertEquals(List.of(),p.feature(file,c));}
    }
    @Test void retiredParentCannotBeDoubleCountedAndWrongQuantityOrUnitFails() throws Exception {
        JsonNode a=assertion("V8","ontology-v1-v2-preserves","식별된-실물-100");
        JsonNode rows=Json.parse("[{\"active\":false,\"quantity\":\"100\",\"unit\":\"BOX\"},{\"active\":true,\"quantity\":\"60\",\"unit\":\"BOX\"},{\"active\":true,\"quantity\":\"40\",\"unit\":\"BOX\"}]");
        put("after","/data/rawRows/segments",rows);assertDoesNotThrow(()->check(a));
        ((ObjectNode)rows.get(0)).put("active",true);assertThrows(AssertionError.class,()->check(a));
        ((ObjectNode)rows.get(0)).put("active",false);((ObjectNode)rows.get(2)).put("quantity","39");assertThrows(AssertionError.class,()->check(a));
        ((ObjectNode)rows.get(2)).put("quantity","40").put("unit","KG");assertThrows(AssertionError.class,()->check(a));
    }
    @Test void duplicatePhysicalScopeFailsEvenWithCorrectTotal() throws Exception {
        JsonNode a=assertion("V8","ontology-v1-v2-preserves","실물-identity-중복0");
        JsonNode rows=Json.parse("[{\"active\":true,\"physicalScopeId\":\"physical-A\"},{\"active\":true,\"physicalScopeId\":\"physical-B\"}]");
        put("after","/data/rawRows/segments",rows);check(a);((ObjectNode)rows.get(1)).put("physicalScopeId","physical-A");assertThrows(AssertionError.class,()->check(a));
    }
    @Test void zeroRollbackDeltaStillRequiresActualBaselineUnit() throws Exception {
        JsonNode a=assertion("V8","transaction-rollback-all-effects","rollback-수량변화0");
        for(String action:List.of("before","after")) {
            scalar(action,"/data/data/inventory/heldQuantity","60");
            scalar(action,"/data/data/inventory/unit","BOX");
        }
        check(a);scalar("before","/data/data/inventory/unit","KG");
        assertThrows(AssertionError.class,()->check(a));
    }
    @Test void missingDutyOwnerAndChangedQuantityOrDeadlineFail() throws Exception {
        JsonNode a=assertion("V8","ontology-v1-v2-preserves","남은40-인간-owner");
        JsonNode rows=Json.parse("[{\"id\":\"duty-id\",\"ownerId\":\"human-owner-id\",\"status\":\"OPEN\",\"quantity\":\"40\",\"unit\":\"BOX\",\"nextAction\":\"나머지 수령 확인\",\"nextCheckAt\":\"2026-10-08T09:00:00Z\"}]");
        put("after","/data/rawRows/obligations",rows);check(a);ObjectNode row=(ObjectNode)rows.get(0);row.remove("ownerId");assertThrows(AssertionError.class,()->check(a));
        row.put("ownerId","human-owner-id").put("quantity","0");assertThrows(AssertionError.class,()->check(a));row.put("quantity","40").put("nextCheckAt","2026-10-09T09:00:00Z");assertThrows(AssertionError.class,()->check(a));
    }
    @Test void preservedDefinitionMeaningCannotBecomeV2() throws Exception {
        JsonNode a=assertion("V8","ontology-v1-v2-preserves","v1-정의");put("after","/data/rawRows/works",Json.parse("[{\"definitionVersion\":\"definition-v1\"}]"));check(a);
        ((ObjectNode)results.get("after").at("/data/rawRows/works/0")).put("definitionVersion","definition-v2");assertThrows(AssertionError.class,()->check(a));
    }
    @Test void changedIdempotencyResultFailsPreservation() throws Exception {
        JsonNode a=assertion("V8","ontology-v1-v2-preserves","보존-idempotency");JsonNode rows=Json.parse("[{\"resultId\":\"receipt-id\",\"status\":\"COMMITTED\"}]");
        put("before","/data/rawRows/idempotency",rows.deepCopy());put("after","/data/rawRows/idempotency",rows.deepCopy());check(a);
        ((ObjectNode)results.get("after").at("/data/rawRows/idempotency/0")).put("resultId","new-receipt");assertThrows(AssertionError.class,()->check(a));
    }
    @Test void observedSchemaDriftCannotBeTreatedAsAllowedDelta() throws Exception {
        JsonNode a=assertion("T23","schema-fresh","비허용-delta");fact("compare","unapprovedDeltaKinds",Json.array());check(a);
        fact("compare","unapprovedDeltaKinds",Json.parse("[\"REMOVED_NONNEGATIVE_QUANTITY\"]"));assertThrows(AssertionError.class,()->check(a));
    }
    @Test void archiveTreeHashMismatchFails() throws Exception {
        JsonNode a=assertion("T23","archive-inventory-roundtrip","원-tree-hash-복원");fact("inventory","preservedTreeHash",Json.MAPPER.valueToTree("a".repeat(64)));fact("restore-archive","restoredTreeHash",Json.MAPPER.valueToTree("a".repeat(64)));check(a);
        fact("restore-archive","restoredTreeHash",Json.MAPPER.valueToTree("b".repeat(64)));assertThrows(AssertionError.class,()->check(a));
    }
    @Test void manufacturingReclassificationCannotPassZeroEffect() throws Exception {
        JsonNode a=assertion("T23","data-live","원-제조-import-relabel0");fact("data-inventory","manufactureAsImportRows",Json.array());check(a);
        fact("data-inventory","manufactureAsImportRows",Json.parse("[{\"sourceId\":\"manufacturing-unmapped\"}]"));assertThrows(AssertionError.class,()->check(a));
    }
    @Test void localProbeCannotSatisfyBtpObservation() throws Exception {
        JsonNode a=assertion("V8","btp-auth-binding-tls-wire","실제-BTP-profile");fact("btp","profile",Json.MAPPER.valueToTree("BTP"));check(a);
        fact("btp","profile",Json.MAPPER.valueToTree("LOCAL"));assertThrows(AssertionError.class,()->check(a));
    }
    @Test void missingBlobAndEvaluatorCannotProduceCompletionRecord() throws Exception {
        for(String kind:List.of("blob","evaluator")) {JsonNode a=assertion("V8","restore-"+kind,"허위-완료-record0");fact("restore","completionRecords",Json.array());check(a);fact("restore","completionRecords",Json.parse("[{\"status\":\"COMPLETE\"}]"));assertThrows(AssertionError.class,()->check(a));}
    }
    @Test void deletedEvidenceMustNotReappearFromOldBackup() throws Exception {
        JsonNode a=assertion("V8","restore-none","deleted-원문-복활0");fact("restore","readableDeletedBlobs",Json.array());check(a);
        fact("restore","readableDeletedBlobs",Json.parse("[\"deleted-original\"]"));assertThrows(AssertionError.class,()->check(a));
    }
    @Test void cutoverStageOrderCannotSkipAuthSmoke() throws Exception {
        JsonNode a=assertion("T23","cutover-open-order","쓰기개방-선행-stage");JsonNode good=a.path("expected");fact("open_writes","completedStageOrder",good);check(a);
        fact("open_writes","completedStageOrder",Json.parse("[\"WRITE_FREEZE\",\"FINAL_SNAPSHOT\",\"RECONCILE\",\"APPLY_VERSION\",\"OPEN_WRITES\"]"));assertThrows(AssertionError.class,()->check(a));
    }
    @Test void secondExternalIssueAfterRecoveryFails() throws Exception {
        JsonNode a=assertion("T23","failure-after-open-forward","외부결과-대조");fact("repair","externalEffects",Json.parse("[{\"externalOperationId\":\"T23-failure-after-open-forward-external-po\",\"status\":\"CONFIRMED_SUCCESS\",\"issueCount\":1}]"));check(a);
        ((ObjectNode)results.get("repair").at("/data/hostObservation/extractor/rawRows/facts/externalEffects/0")).put("issueCount",2);assertThrows(AssertionError.class,()->check(a));
    }
    @Test void sqlConstraintWrongStateFails() throws Exception {
        JsonNode a=assertion("V8","fresh-constraints-auth","조직-FK-23503");JsonNode rows=Json.parse("[{\"case\":\"ORG_FK\",\"sqlState\":\"23503\"},{\"case\":\"NEGATIVE_QUANTITY\",\"sqlState\":\"23514\"},{\"case\":\"PRECISION_OVERFLOW\",\"sqlState\":\"22003\"},{\"case\":\"OUTBOX_FK\",\"sqlState\":\"23503\"}]");fact("constraints","constraintAttempts",rows);check(a);
        ((ObjectNode)rows.get(1)).put("sqlState","00000");assertThrows(AssertionError.class,()->check(a));
    }
    @Test void missingObservationAndIncompleteScopeNeverBecomeZeroPass() throws Exception {
        JsonNode a=assertion("V8","restore-blob","허위-완료-record0");observed("restore");assertThrows(AssertionError.class,()->check(a));fact("restore","completionRecords",Json.array());check(a);
        ((ObjectNode)results.get("restore").path("provenance")).put("scopeComplete",false);assertThrows(AssertionError.class,()->check(a));
    }
    @Test void schemaUpgradeMustNotUseLegacySchema() throws Exception {
        JsonNode a=assertion("V8","ontology-v1-v2-preserves","옛-migration-source0");fact("upgrade","legacySchemaSources",Json.array());check(a);fact("upgrade","legacySchemaSources",Json.parse("[\"old-erp-ddl\"]"));assertThrows(AssertionError.class,()->check(a));
    }
    @Test void exactManifestCannotOmitMcpSdk() throws Exception {
        JsonNode a=assertion("V8","fresh-install-manifest-cqn","정확-manifest-component-set");var rows=Json.array();for(JsonNode component:a.path("expected")) {ObjectNode row=Json.object();row.set("component",component);rows.add(row);}fact("install","versionManifest/components",rows);check(a);
        for(int i=0;i<rows.size();i++)if(rows.get(i).path("component").asText().equals("MCP_SDK")) {rows.remove(i);break;}assertThrows(AssertionError.class,()->check(a));
    }
}
