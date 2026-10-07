package org.mulino.verification.cases.supply;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mulino.verification.*;
import static org.junit.jupiter.api.Assertions.*;

/** Captured assertion inputs only: no driver, service, state transition or model calls. */
public class SupplyAssertionTest {
    private final AssertionEngine engine = new AssertionEngine();
    private static final Path ROOT = Path.of(System.getProperty("repo.root"));
    private JsonNode assertion(String caseId, String subcase, String id) throws Exception {
        for (JsonNode s : Json.read(ROOT.resolve("verification/cases/"+caseId+"/case.json")).path("subcases"))
            if (s.path("id").asText().equals(subcase))
                for (JsonNode a : s.path("assertions")) if (a.path("id").asText().equals(id)) return a;
        throw new AssertionError("Missing declared assertion "+caseId+"/"+subcase+"/"+id);
    }
    private ObjectNode captured(String response, String rows) throws Exception {
        ObjectNode result = Json.object();
        result.put("driverStatus","EXECUTED");
        result.set("response", Json.parse(response));
        ObjectNode data = Json.object(); data.set("rawRows", Json.parse(rows)); result.set("data",data);
        ObjectNode p = Json.object();p.put("scopeComplete",true).put("source","CANNED_ASSERTION_SELFTEST").put("independent",true);
        result.set("provenance",p);return result;
    }
    private ObjectNode aliases(String caseId,String subcase) throws Exception {
        JsonNode f=Json.read(ROOT.resolve("verification/cases/"+caseId+"/fixtures/"+subcase+".json"));
        ObjectNode ids=Json.object();f.path("aliases").fieldNames().forEachRemaining(x->ids.put(x,"captured-"+x));return ids;
    }
    private void check(JsonNode a,Map<String,JsonNode> inputs,String c,String s) throws Exception { engine.check(a,inputs,aliases(c,s)); }

    @Test void purchase60Plus40PassesBut120DoesNot() throws Exception {
        JsonNode a=assertion("T13","partial-excess-return-relocation","arrival-db100");
        ObjectNode r=captured("{}","{\"receiptContributions\":[{\"quantity\":\"60\",\"unit\":\"BOX\"},{\"quantity\":\"40\",\"unit\":\"BOX\"}]}");
        Map<String,JsonNode> m=Map.of("receipt-db",r);check(a,m,"T13","partial-excess-return-relocation");
        ((ObjectNode)r.at("/data/rawRows/receiptContributions/1")).put("quantity","60");
        assertThrows(AssertionError.class,()->check(a,m,"T13","partial-excess-return-relocation"));
    }
    @Test void receiptUnitMismatchCannotPassEvenWhenSumIs100() throws Exception {
        JsonNode a=assertion("T13","partial-excess-return-relocation","arrival-db100");
        ObjectNode r=captured("{}","{\"receiptContributions\":[{\"quantity\":\"60\",\"unit\":\"BOX\"},{\"quantity\":\"40\",\"unit\":\"KG\"}]}");
        assertThrows(AssertionError.class,()->check(a,Map.of("receipt-db",r),"T13","partial-excess-return-relocation"));
    }
    @Test void excess5IsIndependentFromAuthorized100() throws Exception {
        JsonNode a=assertion("T13","partial-excess-return-relocation","excess5");
        ObjectNode r=captured("{}","{\"excessReconciliations\":[{\"quantity\":\"5\",\"unit\":\"BOX\"}]}");
        Map<String,JsonNode> m=Map.of("receipt-db",r);check(a,m,"T13","partial-excess-return-relocation");
        ((ObjectNode)r.at("/data/rawRows/excessReconciliations/0")).put("quantity","0");
        assertThrows(AssertionError.class,()->check(a,m,"T13","partial-excess-return-relocation"));
    }
    @Test void purchaseChangesCannotReuseApprovalHash() throws Exception {
        JsonNode a=assertion("T13","changed-quantity","approval-binding");
        String hash="a".repeat(64);
        ObjectNode proposal=captured("{\"proposalId\":\"proposal-1\",\"proposalHash\":\""+hash+"\",\"proposalRevision\":1}","{}");
        ObjectNode r=captured("{}","{\"approvals\":[{\"proposalId\":\"proposal-1\",\"proposalHash\":\""+hash+"\",\"proposalRevision\":1,\"approverId\":\"captured-manager\",\"decidedAt\":\"2026-10-07T09:00:00Z\",\"validUntil\":\"2026-10-08T09:00:00Z\",\"consumptionPolicy\":\"SINGLE_ORDER_REVISION\",\"decision\":\"APPROVE\"}]}");
        Map<String,JsonNode> m=Map.of("proposal",proposal,"after-db",r);check(a,m,"T13","changed-quantity");
        ((ObjectNode)r.at("/data/rawRows/approvals/0")).put("proposalHash","b".repeat(64));
        assertThrows(AssertionError.class,()->check(a,m,"T13","changed-quantity"));
    }
    @Test void duplicatePhysicalScopesCannotHideBehindSum100() throws Exception {
        JsonNode a=assertion("T14","many-to-many","physical-unique");
        ObjectNode r=captured("{}","{\"segments\":[{\"physicalScope\":\"A40\"},{\"physicalScope\":\"B10\"},{\"physicalScope\":\"A20\"},{\"physicalScope\":\"B30\"}]}");
        Map<String,JsonNode> m=Map.of("port-db",r);check(a,m,"T14","many-to-many");
        ((ObjectNode)r.at("/data/rawRows/segments/3")).put("physicalScope","A40");
        assertThrows(AssertionError.class,()->check(a,m,"T14","many-to-many"));
    }
    @Test void receipt98AndTransit2DoNotAuthorizeLoss2() throws Exception {
        JsonNode a=assertion("T14","discrepancy-transit2","loss-api0");
        ObjectNode r=captured("{\"data\":{\"lossQuantity\":\"0\",\"unit\":\"BOX\"}}","{}");
        Map<String,JsonNode> m=Map.of("view",r);check(a,m,"T14","discrepancy-transit2");
        ((ObjectNode)r.path("response").path("data")).put("lossQuantity","2");
        assertThrows(AssertionError.class,()->check(a,m,"T14","discrepancy-transit2"));
    }
    @Test void unavailableLossObservationCannotBecomeZero() throws Exception {
        JsonNode a=assertion("T14","discrepancy-transit2","loss-api0");
        assertThrows(AssertionError.class,()->check(a,Map.of("view",StepResult.missing("view","NOT_IMPLEMENTED").toJson()),"T14","discrepancy-transit2"));
    }
    @Test void lossBeforeAfterReadsBothScopedRawArrays() throws Exception {
        JsonNode a=assertion("T14","discrepancy-transit2","loss-before-after");
        ObjectNode before=captured("{}","{\"movements\":[]}");ObjectNode after=captured("{}","{\"movements\":[{\"kind\":\"RECEIPT\",\"quantity\":\"98\"}]}");
        Map<String,JsonNode> m=Map.of("before-db",before,"db",after);check(a,m,"T14","discrepancy-transit2");
        ((com.fasterxml.jackson.databind.node.ArrayNode)after.at("/data/rawRows/movements")).add(Json.parse("{\"kind\":\"LOSS\",\"quantity\":\"2\"}"));
        assertThrows(AssertionError.class,()->check(a,m,"T14","discrepancy-transit2"));
    }
    @Test void agency30CannotExpandTo100FromQc100() throws Exception {
        JsonNode a=assertion("T15","agency30-qc100","eligible-api30");
        ObjectNode r=captured("{\"data\":{\"confirmedEligibleQuantity\":\"30\",\"unit\":\"BOX\"}}","{}");
        Map<String,JsonNode> m=Map.of("eligibility",r);check(a,m,"T15","agency30-qc100");
        ((ObjectNode)r.path("response").path("data")).put("confirmedEligibleQuantity","100");
        assertThrows(AssertionError.class,()->check(a,m,"T15","agency30-qc100"));
    }
    @Test void regulatoryDecisionVersionAndPhysicalScopeAreExact() throws Exception {
        JsonNode a=assertion("T15","agency30-qc100","decision-scope-raw");
        String hash=a.path("expected").get(0).get(7).asText();
        ObjectNode procedure=captured("{\"objectId\":\"procedure-1\"}","{}");ObjectNode split=captured("{\"children\":{\"ALLOW30\":{\"segmentId\":\"allow30\"}}}","{}");
        ObjectNode r=captured("{}","{\"regulatoryDecisions\":[{\"procedureId\":\"procedure-1\",\"itemId\":\"captured-P\",\"lotId\":\"captured-L\",\"segmentId\":\"allow30\",\"quantity\":\"30\",\"unit\":\"BOX\",\"decisionVersion\":\"agency-v1\",\"evidenceHash\":\""+hash+"\"}]}");
        Map<String,JsonNode> m=Map.of("procedure",procedure,"split",split,"db",r);check(a,m,"T15","agency30-qc100");
        ((ObjectNode)r.at("/data/rawRows/regulatoryDecisions/0")).put("decisionVersion","agency-v2");
        assertThrows(AssertionError.class,()->check(a,m,"T15","agency30-qc100"));
    }
    @Test void localPreparedDocumentCannotBecomeSubmitted() throws Exception {
        JsonNode a=assertion("T15","procedure-lifecycle","local-PREPARED");
        ObjectNode r=captured("{\"data\":{\"status\":\"PREPARED\"}}","{}");Map<String,JsonNode> m=Map.of("local",r);check(a,m,"T15","procedure-lifecycle");
        ((ObjectNode)r.path("response").path("data")).put("status","SUBMITTED");
        assertThrows(AssertionError.class,()->check(a,m,"T15","procedure-lifecycle"));
    }
    @Test void agencySubmittedRequiresExternalReceiptAndHash() throws Exception {
        JsonNode a=assertion("T15","procedure-lifecycle","submitted-requires-receipt");String hash=a.path("expected").get(0).get(2).asText();
        ObjectNode r=captured("{}","{\"submissions\":[{\"status\":\"SUBMITTED\",\"externalReceiptId\":\"EXT-AGENCY-1\",\"receiptEvidenceHash\":\""+hash+"\"}]}");
        Map<String,JsonNode> m=Map.of("submitted-db",r);check(a,m,"T15","procedure-lifecycle");
        ((ObjectNode)r.at("/data/rawRows/submissions/0")).remove("externalReceiptId");
        assertThrows(AssertionError.class,()->check(a,m,"T15","procedure-lifecycle"));
    }
    @Test void invoiceFxPairRateDateSourcePolicyAreAllRequired() throws Exception {
        JsonNode a=assertion("T19","fx-commercial","fx-exact-snapshot");ObjectNode invoice=captured("{\"invoiceId\":\"invoice-1\"}","{}");
        ObjectNode r=captured("{}","{\"fxSnapshots\":[{\"invoiceId\":\"invoice-1\",\"pair\":\"EUR/KRW\",\"rate\":\"1500\",\"date\":\"2026-10-07\",\"source\":\"synthetic-fx\",\"policyVersion\":\"fx-v1\",\"rounding\":\"HALF_UP_SCALE_0\"}]}");
        Map<String,JsonNode> m=Map.of("invoice",invoice,"db",r);check(a,m,"T19","fx-commercial");
        ((ObjectNode)r.at("/data/rawRows/fxSnapshots/0")).put("policyVersion","fx-v2");
        assertThrows(AssertionError.class,()->check(a,m,"T19","fx-commercial"));
    }
    @Test void converted150000RequiresKrwUnit() throws Exception {
        JsonNode a=assertion("T19","fx-commercial","converted150000KRW");
        ObjectNode r=captured("{\"data\":{\"convertedAmount\":\"150000\",\"convertedCurrency\":\"KRW\"}}","{}");Map<String,JsonNode> m=Map.of("after-payment",r);check(a,m,"T19","fx-commercial");
        ((ObjectNode)r.path("response").path("data")).put("convertedCurrency","EUR");
        assertThrows(AssertionError.class,()->check(a,m,"T19","fx-commercial"));
    }
    @Test void settlementDutyMustRetainOneHumanOwnerActionAndCheck() throws Exception {
        JsonNode a=assertion("T19","invoice-match-difference","settlement-owner-owner");
        ObjectNode r=captured("{}","{\"obligations\":[{\"status\":\"OPEN\",\"kind\":\"SETTLEMENT_DIFFERENCE\",\"ownerId\":\"captured-intake\",\"nextAction\":\"원천 물량과 근거를 대조한다\",\"nextCheckAt\":\"2026-10-07T10:00:00Z\"}]}");
        Map<String,JsonNode> m=Map.of("db",r);check(a,m,"T19","invoice-match-difference");
        ((ObjectNode)r.at("/data/rawRows/obligations/0")).remove("ownerId");
        assertThrows(AssertionError.class,()->check(a,m,"T19","invoice-match-difference"));
    }
    @Test void originalDifference5CannotDisappearAfterConfirmation() throws Exception {
        JsonNode a=assertion("T19","settlement-valid","original5-preserved");
        ObjectNode r=captured("{}","{\"invoiceDifferences\":[{\"amount\":\"5\",\"currency\":\"EUR\",\"kind\":\"ORIGINAL\"}]}");Map<String,JsonNode> m=Map.of("db",r);check(a,m,"T19","settlement-valid");
        ((ObjectNode)r.at("/data/rawRows/invoiceDifferences/0")).put("amount","0");
        assertThrows(AssertionError.class,()->check(a,m,"T19","settlement-valid"));
    }
    @Test void oneManagerConfirmationCannotBecomeTwo() throws Exception {
        JsonNode a=assertion("T19","settlement-valid","one-manager-confirmation");
        ObjectNode r=captured("{}","{\"settlementConfirmations\":[{\"id\":\"confirmation-1\"}]}");Map<String,JsonNode> m=Map.of("db",r);check(a,m,"T19","settlement-valid");
        ((com.fasterxml.jackson.databind.node.ArrayNode)r.at("/data/rawRows/settlementConfirmations")).add(Json.parse("{\"id\":\"confirmation-2\"}"));
        assertThrows(AssertionError.class,()->check(a,m,"T19","settlement-valid"));
    }
    @Test void paymentReferenceAndManagerConfirmationCannotCreateBankTransfer() throws Exception {
        JsonNode a=assertion("T19","settlement-valid","no-bank0");
        ObjectNode r=captured("{}","{\"bankTransfers\":[]}");Map<String,JsonNode> m=Map.of("db",r);check(a,m,"T19","settlement-valid");
        ((com.fasterxml.jackson.databind.node.ArrayNode)r.at("/data/rawRows/bankTransfers")).add(Json.parse("{\"id\":\"transfer-1\",\"amount\":\"5\",\"currency\":\"EUR\"}"));
        assertThrows(AssertionError.class,()->check(a,m,"T19","settlement-valid"));
    }
}
