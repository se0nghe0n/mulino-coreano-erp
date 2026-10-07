package org.mulino.verification.cases.reviewbusiness;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mulino.verification.*;
import static org.junit.jupiter.api.Assertions.*;

/** SELFTEST of actual case assertions with fixed observations; never runtime acceptance evidence. */
final class BusinessReviewAssertionTest {
    private final Path root=Path.of(System.getProperty("repo.root"));
    private final AssertionEngine engine=new AssertionEngine();
    private final ObjectNode aliases=Json.parse("{\"ORG\":\"org\",\"P\":\"item\",\"L\":\"lot\",\"A20\":\"physical20\",\"manager\":\"manager-human\"}").deepCopy();
    private JsonNode sub(String cid,String sid) throws Exception {
        for(JsonNode s:Json.read(root.resolve("verification/cases/"+cid+"/case.json")).path("subcases"))
            if(s.path("id").asText().equals(sid))return s;
        throw new AssertionError("Missing subcase "+cid+"/"+sid);
    }
    private JsonNode declared(String cid,String sid,String id) throws Exception {
        for(JsonNode a:sub(cid,sid).path("assertions"))if(a.path("id").asText().equals(id))return a;
        throw new AssertionError("Missing assertion "+id);
    }
    private JsonNode action(JsonNode sub,String id) {
        for(JsonNode a:sub.path("actions"))if(a.path("id").asText().equals(id))return a;
        throw new AssertionError("Missing action "+id);
    }
    private ObjectNode sample(String payload) {
        ObjectNode result=Json.parse(payload).deepCopy();result.put("driverStatus","EXECUTED");
        result.set("provenance",Json.parse("{\"scopeComplete\":true,\"source\":\"BUSINESS_REVIEW_ASSERTION_SELFTEST\",\"independent\":false}"));
        result.set("artifactRefs",Json.parse("[\"SELFTEST_NOT_RUNTIME_PROOF\"]"));return result;
    }
    private void check(JsonNode a,Map<String,JsonNode> captures){engine.check(a,captures,aliases);}
    private void rejects(JsonNode a,Map<String,JsonNode> captures){assertThrows(AssertionError.class,()->check(a,captures));}

    @Test void v3QuantityBaselineAcceptsPhysicalSegmentWithoutLockKindAndRejectsWrongIdentityQuantityUnit() throws Exception {
        JsonNode a=declared("V3","hold-first","actual-baseline-physical-rows");
        ObjectNode db=sample("{\"data\":{\"rawRows\":{\"segments\":[{\"id\":\"physical20\",\"quantity\":\"20\",\"unit\":\"BOX\",\"active\":true}]}}}");
        var results=Map.<String,JsonNode>of("before-db",db);check(a,results);
        ObjectNode row=(ObjectNode)db.at("/data/rawRows/segments/0");
        row.put("quantity","21");rejects(a,results);row.put("quantity","20");
        row.put("unit","KG");rejects(a,results);row.put("unit","BOX");
        row.put("id","different20");rejects(a,results);row.put("id","physical20");
        ((ArrayNode)db.at("/data/rawRows/segments")).add(row.deepCopy());rejects(a,results);
    }
    @Test void v3ActualLockMustStillBeAScopeFence() throws Exception {
        JsonNode a=declared("V3","hold-first","scope-lock-14-source-nonempty");
        ObjectNode db=sample("{\"data\":{\"rawRows\":{\"locks\":[{\"kind\":\"SCOPE_FENCE\",\"capabilityId\":\"dispatchQuantity\"}]}}}");
        var results=Map.<String,JsonNode>of("after-db",db);check(a,results);
        ((ObjectNode)db.at("/data/rawRows/locks/0")).put("kind","QUANTITY_SEGMENT");rejects(a,results);
    }
    @Test void t20BypassCallsHaveValidSameProposalDispatchAndAnApprovedCountercall() throws Exception {
        assertEquals("MANAGER",Json.read(root.resolve("verification/cases/T20/fixture.json")).at("/baseline/policy/purchaseDecisionRole").asText());
        for(String sid:List.of("mrtr-state-as-approval","mrtr-accept-as-approval")) {
            JsonNode s=sub("T20",sid);assertEquals("proposePurchase",action(s,"proposal").path("capabilityId").asText());
            JsonNode continued=action(s,"continued").at("/request/body/params");
            assertEquals("dispatchPurchaseOrder",continued.path("name").asText());
            JsonNode negative=continued.path("arguments");
            assertEquals("dispatchPurchaseOrder",negative.path("capabilityId").asText());
            assertEquals("COMMAND",negative.path("intentKind").asText());
            assertTrue(negative.has("slots"),"The shared business envelope is required");
            assertFalse(negative.path("slots").has("approvalId"));assertEquals("SYNTHETIC_SUPPLIER",negative.at("/slots/channel/value").asText());
            for(String f:List.of("proposalId","proposalHash")) {
                assertEquals("proposal",negative.at("/slots/"+f+"/value/$result/actionId").asText());
                assertEquals(negative.at("/slots/"+f),action(s,"approved-dispatch").at("/request/body/params/arguments/slots/"+f));
            }
            assertEquals("/response/proposalRevision",negative.at("/expectedRevision/$result/pointer").asText());
            ObjectNode approved=action(s,"approved-dispatch").at("/request/body/params/arguments").deepCopy();
            assertEquals("approval",approved.at("/slots/approvalId/value/$result/actionId").asText());
            ((ObjectNode)approved.path("slots")).remove("approvalId");approved.set("commandIdempotencyKey",negative.path("commandIdempotencyKey"));
            assertEquals(negative,approved,"Only manager approval and a fresh effect key differ");
            assertEquals("manager",action(s,"approval").path("actorRef").asText());
            assertFalse(action(s,"issued").at("/request/body/params/arguments/slots").has("channel"));
            assertEquals("dispatchPurchaseOrder",action(s,"issued").at("/request/body/params/name").asText());
            assertTrue(action(s,"db-before").at("/observation/sources").toString().contains("purchaseOrders"));
            if(sid.contains("accept"))assertEquals("accept",continued.at("/inputResponses/0/action").asText());
        }
    }
    @Test void t20RoutesWrapTheIdenticalCanonicalBusinessCommandAndApprovalDecision() throws Exception {
        for(String sid:List.of("domain-waiting_approval","domain-accepted_pending_external")) {
            JsonNode s=sub("T20",sid),api=action(s,"api-result").path("request"),wire=action(s,"wire-result").at("/request/body/params/arguments");
            assertEquals(api,wire,"Transport wrapping cannot change command semantics");
            assertEquals("dispatchPurchaseOrder",api.path("capabilityId").asText());
            assertEquals("/response/proposalRevision",api.at("/expectedRevision/$result/pointer").asText());
            if(sid.contains("accepted")) {
                JsonNode approval=action(s,"approval").path("request");
                assertEquals("APPROVE",approval.at("/slots/decision/value").asText());
                assertEquals("SINGLE_ORDER_REVISION",approval.at("/slots/consumptionPolicy/value").asText());
            }
        }
        JsonNode concurrent=sub("T20","mrtr-concurrent-approval-consumption");
        assertFalse(action(concurrent,"approval-issued").at("/request/body/params/arguments/slots").has("decision"));
        for(String side:List.of("left","right")) {
            JsonNode call=action(concurrent,"start-"+side).path("call");
            assertEquals("APPROVE",call.at("/request/body/params/arguments/slots/decision/value").asText());
            assertEquals("/response/proposalRevision",call.at("/request/body/params/arguments/expectedRevision/$result/pointer").asText());
        }
    }
    @Test void t20ApprovalNegativesRejectGenericValidationFailuresAndUnauthorizedApproval() throws Exception {
        for(String sid:List.of("mrtr-state-as-approval","mrtr-accept-as-approval")) {
            JsonNode outcome=declared("T20",sid,"continued-outcome"),code=declared("T20",sid,"specific-manager-approval-missing"),role=declared("T20",sid,"required-decision-role");
            ObjectNode response=sample("{\"response\":{\"body\":{\"result\":{\"structuredContent\":{\"outcome\":\"WAITING_APPROVAL\",\"error\":{\"code\":\"APPROVAL_REQUIRED\",\"requiredRole\":\"MANAGER\"}}}}}}");
            var results=Map.<String,JsonNode>of("continued",response);check(outcome,results);check(code,results);check(role,results);
            ObjectNode domain=(ObjectNode)response.at("/response/body/result/structuredContent");
            domain.put("outcome","REJECTED");rejects(outcome,results);domain.put("outcome","WAITING_APPROVAL");
            ((ObjectNode)domain.path("error")).put("code","TYPE_INVALID");rejects(code,results);
            ((ObjectNode)domain.path("error")).put("code","APPROVAL_REQUIRED").put("requiredRole","WRITER");rejects(role,results);
            domain.put("outcome","APPLIED");rejects(outcome,results);
        }
    }
    @Test void t20NoApprovalCannotCreateAnOrderEvenIfOutboxStaysEmpty() throws Exception {
        JsonNode a=declared("T20","mrtr-state-as-approval","unchanged-purchase-orders");
        ObjectNode before=sample("{\"data\":{\"rawRows\":{\"purchaseOrders\":[]}}}"),after=before.deepCopy();
        var results=Map.<String,JsonNode>of("db-before",before,"db-after",after);check(a,results);
        ((ArrayNode)after.at("/data/rawRows/purchaseOrders")).add(Json.parse("{\"id\":\"unauthorized-order\",\"proposalId\":\"proposal-one\"}"));rejects(a,results);
        ((ObjectNode)after.path("provenance")).put("scopeComplete",false);rejects(a,results);
    }
    @Test void t20CountercallRequiresActualManagerIdentityHashAndAppliedOrder() throws Exception {
        String sid="mrtr-state-as-approval";
        ObjectNode proposal=sample("{\"response\":{\"proposalId\":\"proposal-one\",\"proposalHash\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\",\"proposalRevision\":1}}");
        ObjectNode approval=sample("{\"response\":{\"approvalId\":\"approval-one\",\"outcome\":\"APPLIED\"}}");
        ObjectNode dispatch=sample("{\"response\":{\"body\":{\"result\":{\"structuredContent\":{\"outcome\":\"ACCEPTED_PENDING_EXTERNAL\",\"purchaseOrderId\":\"order-one\"}}}}}");
        ObjectNode db=sample("{\"data\":{\"rawRows\":{\"approvals\":[{\"id\":\"approval-one\",\"proposalId\":\"proposal-one\",\"proposalHash\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\",\"proposalRevision\":1,\"approverId\":\"manager-human\",\"decidedAt\":\"2026-10-07T09:00:00Z\",\"validUntil\":\"2026-10-08T09:00:00Z\",\"consumptionPolicy\":\"SINGLE_ORDER_REVISION\",\"decision\":\"APPROVE\"}],\"purchaseOrders\":[{\"id\":\"order-one\",\"proposalId\":\"proposal-one\",\"proposalHash\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\",\"approvalId\":\"approval-one\"}]}}}");
        var results=Map.<String,JsonNode>of("proposal",proposal,"approval",approval,"approved-dispatch",dispatch,"db-positive",db);
        JsonNode approved=declared("T20",sid,"approved-countercall-applied"),manager=declared("T20",sid,"real-manager-decision"),order=declared("T20",sid,"approved-countercall-order");
        check(approved,results);check(manager,results);check(order,results);
        ObjectNode decision=(ObjectNode)db.at("/data/rawRows/approvals/0");decision.put("approverId","writer-human");rejects(manager,results);decision.put("approverId","manager-human");
        decision.put("proposalHash","other-hash");rejects(manager,results);decision.put("proposalHash","aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        ((ArrayNode)db.at("/data/rawRows/purchaseOrders")).removeAll();rejects(order,results);
        ((ObjectNode)dispatch.at("/response/body/result/structuredContent")).put("outcome","REJECTED");rejects(approved,results);
    }
    @Test void t20OtherPrincipalHasIndependentIdentityAndSameCapabilityBeforeStateBindingIsTested() throws Exception {
        JsonNode fixture=Json.read(root.resolve("verification/cases/T20/fixture.json"));
        JsonNode writer=fixture.at("/actors/writer"),other=fixture.at("/actors/otherPrincipal");
        assertNotEquals(writer.path("subject"),other.path("subject"));
        for(JsonNode actor:List.of(writer,other)) {
            assertTrue(actor.path("roleCapabilities").toString().contains("\"structureIntent\""));
            assertTrue(actor.at("/grant/actions").toString().contains("\"structureIntent\""));
        }
        assertEquals(writer.path("roleCapabilities"),other.path("roleCapabilities"));
        assertEquals(writer.at("/grant/actions"),other.at("/grant/actions"));
        assertEquals(writer.at("/grant/scope"),other.at("/grant/scope"));
        JsonNode s=sub("T20","mrtr-other-principal");
        for(String id:List.of("continued","own-issued","own-continued"))assertEquals("otherPrincipal",action(s,id).path("actorRef").asText());
        assertEquals("own-issued",action(s,"own-continued").at("/request/body/params/requestState/$result/actionId").asText());
        JsonNode code=declared("T20","mrtr-other-principal","wrong-principal-code"),positive=declared("T20","mrtr-other-principal","own-state-countercall-structured");
        ObjectNode negative=sample("{\"response\":{\"body\":{\"result\":{\"structuredContent\":{\"error\":{\"code\":\"REQUEST_STATE_PRINCIPAL_MISMATCH\"}}}}}}");
        ObjectNode own=sample("{\"response\":{\"body\":{\"result\":{\"structuredContent\":{\"outcome\":\"STRUCTURED\"}}}}}");
        var results=Map.<String,JsonNode>of("continued",negative,"own-continued",own);check(code,results);check(positive,results);
        ((ObjectNode)negative.at("/response/body/result/structuredContent/error")).put("code","FORBIDDEN");rejects(code,results);
        ((ObjectNode)own.at("/response/body/result/structuredContent")).put("outcome","REJECTED");rejects(positive,results);
    }
    private Map<String,JsonNode> overlap() {
        ObjectNode db=sample("{\"data\":{\"rawRows\":{\"restrictions\":[{\"id\":\"qc20\",\"segmentId\":\"physical20\",\"reason\":\"QC_REVIEW\",\"status\":\"RELEASED\",\"quantity\":\"20\",\"unit\":\"BOX\"},{\"id\":\"recall60\",\"lotId\":\"lot\",\"reason\":\"RECALL_INVESTIGATION\",\"status\":\"ACTIVE\",\"quantity\":\"60\",\"unit\":\"BOX\"}]}}}");
        var results=new HashMap<String,JsonNode>();results.put("before-dispatch-db",db.deepCopy());results.put("after-dispatch-db",db);
        ObjectNode active=db.deepCopy();((ObjectNode)active.at("/data/rawRows/restrictions/0")).put("status","ACTIVE");results.put("before-release-db",active);
        results.put("qc-hold20",sample("{\"response\":{\"restrictionId\":\"qc20\"}}"));results.put("recall-hold60",sample("{\"response\":{\"restrictionId\":\"recall60\"}}"));
        results.put("split",sample("{\"response\":{\"children\":[{\"id\":\"physical20\"}]}}"));results.put("qc-release20",sample("{\"response\":{\"outcome\":\"APPLIED\",\"restrictionId\":\"qc20\"}}"));return results;
    }
    @Test void e2QcReleaseCannotBeANoopOrReleaseTheWrongRestrictionScopeQuantityOrUnit() throws Exception {
        var results=overlap();String sid="overlapping-holds";
        JsonNode applied=declared("E2",sid,"qc-release-applied"),identity=declared("E2",sid,"qc-release-same-restriction"),before=declared("E2",sid,"qc20-released-before-dispatch"),after=declared("E2",sid,"qc20-remains-released-after-dispatch");
        for(JsonNode a:List.of(applied,identity,before,after))check(a,results);
        JsonNode active=declared("E2",sid,"qc20-active-before-release");check(active,results);
        ((ObjectNode)results.get("before-release-db").at("/data/rawRows/restrictions/0")).put("status","RELEASED");rejects(active,results);
        ObjectNode row=(ObjectNode)results.get("after-dispatch-db").at("/data/rawRows/restrictions/0");
        for(String[] mutation:List.of(new String[]{"status","ACTIVE"},new String[]{"quantity","19"},new String[]{"unit","KG"},new String[]{"segmentId","different20"},new String[]{"id","different-qc"})) {
            JsonNode original=row.get(mutation[0]);row.put(mutation[0],mutation[1]);rejects(after,results);row.set(mutation[0],original);
        }
        ObjectNode release=(ObjectNode)results.get("qc-release20").path("response");release.put("outcome","NOOP");rejects(applied,results);release.put("outcome","APPLIED");release.put("restrictionId","recall60");rejects(identity,results);
        ((ObjectNode)results.get("before-dispatch-db").at("/data/rawRows/restrictions/0")).put("status","ACTIVE");rejects(before,results);
    }
    @Test void e2ReleasedQc20MustNotReleaseOrShrinkRecall60() throws Exception {
        var results=overlap();JsonNode a=declared("E2","overlapping-holds","recall60-scope-quantity-remains-active");check(a,results);
        ObjectNode row=(ObjectNode)results.get("after-dispatch-db").at("/data/rawRows/restrictions/1");
        for(String[] mutation:List.of(new String[]{"status","RELEASED"},new String[]{"quantity","40"},new String[]{"unit","KG"},new String[]{"lotId","other-lot"})) {
            JsonNode original=row.get(mutation[0]);row.put(mutation[0],mutation[1]);rejects(a,results);row.set(mutation[0],original);
        }
    }
    @Test void e2BothReleaseChecksPassButAnyNewWarehouseDispatchStillFails() throws Exception {
        var results=overlap();ObjectNode before=sample("{\"response\":{\"data\":{\"warehouseDispatchQuantity\":\"0\",\"unit\":\"BOX\"}}}");
        ObjectNode after=before.deepCopy();results.put("before-dispatch",before);results.put("after-dispatch",after);
        JsonNode a=declared("E2","overlapping-holds","dispatched-after-QC-only-release-3");check(a,results);
        check(declared("E2","overlapping-holds","qc20-remains-released-after-dispatch"),results);
        check(declared("E2","overlapping-holds","recall60-scope-quantity-remains-active"),results);
        ((ObjectNode)after.at("/response/data")).put("warehouseDispatchQuantity","60");rejects(a,results);
    }
    @Test void ownedSchemasAndAllFeatureSelectorsRemainValid() throws Exception {
        ContractValidator validator=new ContractValidator(root);
        for(String cid:List.of("V3","T20","E2")) {
            Path path=root.resolve("verification/cases/"+cid+"/case.json");JsonNode c=validator.caseFile(path);
            assertEquals(List.of(),new PreparationValidator(root).feature(path,c));
        }
    }
}
