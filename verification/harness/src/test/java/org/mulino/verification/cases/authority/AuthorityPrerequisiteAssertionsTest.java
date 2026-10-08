package org.mulino.verification.cases.authority;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.mulino.verification.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Authored prerequisites and captured assertion mutations; never a product implementation. */
public class AuthorityPrerequisiteAssertionsTest {
    private final Path root=Path.of(System.getProperty("repo.root"));
    private final AssertionEngine engine=new AssertionEngine();
    private JsonNode scenario(String route,String cap) throws Exception {
        for(JsonNode sub:Json.read(root.resolve("verification/cases/C3/case.json")).path("subcases"))
            if(sub.path("id").asText().equals(route+"-"+cap)) return sub;
        throw new AssertionError("Missing C3 case");
    }
    private JsonNode action(JsonNode sub,String id) {
        for(JsonNode a:sub.path("actions")) if(a.path("id").asText().equals(id)) return a;
        throw new AssertionError("Missing action "+id);
    }
    private JsonNode assertion(JsonNode sub,String id) {
        for(JsonNode a:sub.path("assertions")) if(a.path("id").asText().equals(id)) return a;
        throw new AssertionError("Missing assertion "+id);
    }
    private int index(JsonNode sub,String id) {
        for(int i=0;i<sub.path("actions").size();i++) if(sub.path("actions").get(i).path("id").asText().equals(id))return i;
        throw new AssertionError("Missing action "+id);
    }
    private ObjectNode capture(String rows,String response) throws Exception {
        ObjectNode n=Json.object();n.put("driverStatus","EXECUTED");n.set("provenance",Json.parse("{\"scopeComplete\":true,\"source\":\"CAPTURED_ASSERTION_SELFTEST\"}"));
        n.set("data",Json.object().set("rawRows",Json.parse(rows)));n.set("response",Json.parse(response));return n;
    }
    private JsonNode aliases() throws Exception {
        return Json.parse("{\"ORG-A\":\"org-a\",\"P\":\"item\",\"A20\":\"segment20\",\"PROPOSAL\":\"proposal\",\"EXTERNAL-OP\":\"external-op\",\"manager\":\"manager\",\"WORK\":\"work\",\"WORK2\":\"source-work\",\"DUTY\":\"duty\",\"warehouse\":\"warehouse\",\"RECIPIENT-ACCEPTANCE\":\"recipient-decision\"}");
    }
    @Test void migrationReviewIdentityRequiresAnActualNonemptyString() throws Exception {
        JsonNode declaration=Json.parse("{\"$result\":{\"actionId\":\"review\",\"pointer\":\"/response/reviewId\"}}");
        var actual=capture("{}","{\"reviewId\":\"review-for-work100\"}");
        var resolver=new ReferenceResolver(Map.of("review",actual),aliases());
        assertEquals("review-for-work100",resolver.identity(declaration).asText());
        for(JsonNode invalid:List.of(Json.parse("17"),Json.parse("false"),Json.parse("{}"),Json.parse("\"\""))) {
            ((ObjectNode)actual.path("response")).set("reviewId",invalid);
            assertThrows(IllegalArgumentException.class,()->resolver.identity(declaration));
        }
    }
    @Test void schemaAndKoreanFeatureLinkEveryAuthoredPrerequisite() throws Exception {
        Path path=root.resolve("verification/cases/C3/case.json");
        JsonNode c=new ContractValidator(root).caseFile(path);
        assertEquals(List.of(),new PreparationValidator(root).feature(path,c));
        assertEquals(309,c.path("subcases").size());
    }
    @Test void allThreeRoutesUseTheIdenticalValidPayloadWithDifferentCurrentAuthority() throws Exception {
        for(String cap:List.of("dispatchPurchaseOrder","migrateWorkDefinition","transferObligation")) for(String route:List.of("api","mcp","worker")) {
            JsonNode sub=scenario(route,cap),denied=action(sub,"attempt"),allowed=action(sub,"authorized-same-input");
            ObjectNode deniedPayload=denied.path("request").deepCopy(),allowedPayload=allowed.path("request").deepCopy();
            assertEquals("C3-"+route+"-"+cap+"-attempt",deniedPayload.remove("commandIdempotencyKey").asText());assertEquals("C3-"+route+"-"+cap+"-authorized",allowedPayload.remove("commandIdempotencyKey").asText());
            assertEquals(deniedPayload,allowedPayload);assertEquals(route,allowed.path("route").asText());
            assertEquals("reader",denied.path("actorRef").asText());assertEquals("delegator",allowed.path("actorRef").asText());
            assertTrue(index(sub,"before")<index(sub,"attempt"));assertTrue(index(sub,"after")<index(sub,"authorized-same-input"));
            assertEquals("FORBIDDEN",assertion(sub,"attempt-code").path("expected").asText());
        }
    }
    @Test void managerApprovalIsExecutedBeforeBaselineAndCannotBeReplacedByClientDecision() throws Exception {
        for(String route:List.of("api","mcp","worker")) {
            JsonNode sub=scenario(route,"dispatchPurchaseOrder"),approve=action(sub,"precondition-approval"),attempt=action(sub,"attempt");
            assertEquals("manager",approve.path("actorRef").asText());assertEquals("approvePurchase",approve.path("capabilityId").asText());
            assertTrue(index(sub,"precondition-proposal")<index(sub,"precondition-approval"));assertTrue(index(sub,"precondition-approval")<index(sub,"before"));
            assertEquals("APPROVE",approve.at("/request/slots/decision/value").asText());assertFalse(attempt.at("/request/slots").has("decision"));
            assertEquals("precondition-approval",attempt.at("/request/slots/approvalId/value/$result/actionId").asText());
            assertEquals("PROPOSAL",action(sub,"target-before").at("/request/objectId/$alias").asText());
            assertEquals("TradeItem",attempt.at("/request/subjectRefs/0/type").asText());
        }
    }
    @Test void outboxMustBindTheActualApprovalHashAndExternalOperationExactlyOnce() throws Exception {
        JsonNode sub=scenario("api","dispatchPurchaseOrder"),a=assertion(sub,"authorized-business-outbox-once");
        String hash="a".repeat(64),key="C3-api-dispatchPurchaseOrder-authorized";
        var observed=capture("{\"outbox\":[{\"capabilityId\":\"dispatchPurchaseOrder\",\"proposalId\":\"proposal\",\"proposalHash\":\""+hash+"\",\"approvalId\":\"approval\",\"externalOperationId\":\"external-op\",\"commandIdempotencyKey\":\""+key+"\"}]}","{}");
        var results=new HashMap<String,JsonNode>();results.put("authorized-after",observed);results.put("precondition-proposal",capture("{}","{\"data\":{\"proposalHash\":\""+hash+"\"}}"));results.put("precondition-approval",capture("{}","{\"approvalId\":\"approval\"}"));
        engine.check(a,results,aliases());ObjectNode row=(ObjectNode)observed.at("/data/rawRows/outbox/0");
        row.put("approvalId","unverified-approval");assertThrows(AssertionError.class,()->engine.check(a,results,aliases()));row.put("approvalId","approval");
        row.put("externalOperationId","other-external-op");assertThrows(AssertionError.class,()->engine.check(a,results,aliases()));row.put("externalOperationId","external-op");
        ((com.fasterxml.jackson.databind.node.ArrayNode)observed.at("/data/rawRows/outbox")).add(row.deepCopy());assertThrows(AssertionError.class,()->engine.check(a,results,aliases()));
        results.put("precondition-approval",StepResult.missing("precondition-approval","no actual manager decision").toJson());assertThrows(AssertionError.class,()->engine.check(a,results,aliases()));
    }
    @Test void recallClosureRequiresAdminApprovalNoticeRecoveryDisposalBeforeTheCounterCall() throws Exception {
        for(String route:List.of("api","mcp","worker")) {
            JsonNode sub=scenario(route,"closeRecall");String previous="setup";
            for(String next:List.of("precondition-recall","precondition-approval","precondition-notice","precondition-recovery","precondition-disposal","before","attempt","authorized-same-input")) {
                assertTrue(index(sub,previous)<index(sub,next),previous+" must precede "+next);previous=next;
            }
            assertEquals("admin",action(sub,"precondition-approval").path("actorRef").asText());assertEquals("admin",action(sub,"authorized-same-input").path("actorRef").asText());
            assertEquals("DISPOSED",action(sub,"precondition-disposal").at("/request/slots/outcome").asText());
            assertEquals("precondition-disposal",action(sub,"attempt").at("/request/slots/partitionHash/$result/actionId").asText());
            assertFalse(action(sub,"attempt").at("/request/slots").has("decision"),"closeRecall must not reuse approveRecall's payload");
        }
        JsonNode closure=assertion(scenario("api","closeRecall"),"authorized-business-recall-closure");
        var rows=capture("{\"recallClosures\":[{\"recallId\":\"recall\",\"approvalId\":\"approval\",\"closedBy\":\"admin\",\"recoveredQuantity\":\"20\",\"disposedQuantity\":\"20\",\"exceptionQuantity\":\"0\",\"unknownQuantity\":\"0\",\"unit\":\"BOX\"}]}","{}");
        var results=new HashMap<String,JsonNode>();results.put("authorized-after",rows);results.put("precondition-approval",capture("{}","{\"approvalId\":\"approval\"}"));
        ObjectNode aliases=(ObjectNode)aliases();aliases.put("RECALL","recall").put("admin","admin");
        engine.check(closure,results,aliases);
        ((ObjectNode)rows.at("/data/rawRows/recallClosures/0")).put("unknownQuantity","20").put("disposedQuantity","0");assertThrows(AssertionError.class,()->engine.check(closure,results,aliases));
        ((ObjectNode)rows.at("/data/rawRows/recallClosures/0")).put("unknownQuantity","0").put("disposedQuantity","20").put("closedBy","delegator");assertThrows(AssertionError.class,()->engine.check(closure,results,aliases));
    }
    @Test void emergencyReassignUsesItsOwnAdminPayloadNotTheHandoverPayload() throws Exception {
        JsonNode sub=scenario("mcp","emergencyReassign");JsonNode slots=action(sub,"attempt").at("/request/slots");
        assertEquals("DUTY",slots.at("/obligationId/$alias").asText());assertEquals("delegator",slots.at("/newOwnerId/$alias").asText());assertFalse(slots.has("handoverId"));
        assertEquals("admin",action(sub,"authorized-same-input").path("actorRef").asText());
        assertEquals("admin",assertion(sub,"authorized-committed-once").at("/source/where/stableRequestOwnerId/$alias").asText());
    }
    @Test void migrationPositiveActorIsTheCurrentHumanWorkOwner() throws Exception {
        JsonNode fixture=Json.read(root.resolve("verification/cases/C3/fixture-migrateWorkDefinition.json"));
        assertEquals("delegator",fixture.at("/baseline/work/ownerAlias").asText());
        assertTrue(fixture.at("/actors/delegator/grant/actions").toString().contains("migrateWorkDefinition"));
        assertFalse(fixture.at("/actors/reader/grant/actions").toString().contains("migrateWorkDefinition"));
    }
    @Test void migrationRequiresPublishedTargetAndDistinctScopedTransitionApprovalBeforePrivilegeTest() throws Exception {
        for(String route:List.of("api","mcp","worker")) {
            JsonNode sub=scenario(route,"migrateWorkDefinition");
            String previous="setup";
            for(String next:List.of("precondition-draft","precondition-validate","precondition-review","precondition-publish-approval","precondition-publish","precondition-migration-review","precondition-migration-approval","before","attempt")) {
                assertTrue(index(sub,previous)<index(sub,next),previous+" must precede "+next);previous=next;
            }
            JsonNode review=action(sub,"precondition-migration-review").at("/request/slots");
            assertEquals("WORK_MIGRATION",review.path("reviewKind").asText());assertEquals("WORK",review.at("/affectedWorkIds/0/$alias").asText());assertEquals("GOAL-V1",review.at("/affectedDataIds/0/$alias").asText());
            assertEquals("100",review.at("/mapping/quantity/from").asText());assertEquals("100",review.at("/mapping/quantity/to").asText());assertEquals("ARRIVED",review.at("/mapping/endpoint/to").asText());assertTrue(review.path("unsupportedValues").isEmpty());
            assertEquals("configApprover",action(sub,"precondition-migration-approval").path("actorRef").asText());
            assertEquals("precondition-migration-approval",action(sub,"attempt").at("/request/slots/approvalId/$result/actionId").asText());
            assertEquals("Work",action(sub,"attempt").at("/request/subjectRefs/0/type").asText());
        }
    }
    @Test void appliedResponseCannotPassWhenWorkStillUsesV1OrUnpublishedTarget() throws Exception {
        JsonNode sub=scenario("worker","migrateWorkDefinition");var r=capture("{\"works\":[{\"id\":\"work\",\"definitionVersion\":\"definition-v2\",\"definitionId\":\"published-target\"}],\"definitions\":[{\"id\":\"published-target\",\"version\":\"definition-v2\",\"state\":\"PUBLISHED\",\"hash\":\""+"b".repeat(64)+"\"}]}","{\"outcome\":\"APPLIED\"}");
        var results=new HashMap<String,JsonNode>();results.put("before",r);results.put("authorized-after",r);results.put("precondition-draft",capture("{}","{\"definitionId\":\"published-target\"}"));results.put("precondition-publish",capture("{}","{\"hash\":\""+"b".repeat(64)+"\"}"));
        var version=assertion(sub,"authorized-business-work-version");var published=assertion(sub,"precondition-published");engine.check(version,results,aliases());engine.check(published,results,aliases());
        ((ObjectNode)r.at("/data/rawRows/works/0")).put("definitionVersion","definition-v1");assertThrows(AssertionError.class,()->engine.check(version,results,aliases()));
        ((ObjectNode)r.at("/data/rawRows/definitions/0")).put("state","IN_REVIEW");assertThrows(AssertionError.class,()->engine.check(published,results,aliases()));
    }
    @Test void transferUsesVerifiedRecipientDecisionForTheActualSourceTargetAndCurrentRevision() throws Exception {
        JsonNode f=Json.read(root.resolve("verification/cases/C3/fixture-transferObligation.json")),a=f.at("/baseline/recipientAcceptances/0");
        assertEquals("warehouse",a.path("issuedByAlias").asText());assertEquals("fixture-warehouse",a.at("/verifiedIdentity/subject").asText());
        assertEquals("WORK2",a.path("sourceWorkAlias").asText());assertEquals("WORK",a.path("targetWorkAlias").asText());
        assertEquals("20",a.path("quantity").asText());assertEquals("BOX",a.path("unit").asText());assertEquals("VERIFIED",a.path("verificationStatus").asText());assertFalse(a.path("consumed").asBoolean());
        Instant now=Instant.parse(f.at("/clock/asOf").asText());assertFalse(now.isBefore(Instant.parse(a.path("validFrom").asText())));assertTrue(now.isBefore(Instant.parse(a.path("validUntil").asText())));
        assertEquals(f.at("/baseline/priorEntities/DUTY/revision"),a.path("expectedSourceRevision"));assertEquals("delegator",f.at("/baseline/priorEntities/DUTY/ownerAlias").asText());
        assertEquals(Json.sha256(root.resolve("verification/cases/C3/recipient-acceptance.json")),a.path("sha256").asText());
        JsonNode sub=scenario("api","transferObligation");assertEquals("RECIPIENT-ACCEPTANCE",action(sub,"attempt").at("/request/slots/acceptanceEvidenceId/$alias").asText());assertFalse(action(sub,"attempt").at("/request/slots").has("resolutionEvidenceId"));
        var observed=capture("{\"recipientAcceptances\":[{\"id\":\"recipient-decision\",\"issuedById\":\"warehouse\",\"sourceObligationId\":\"duty\",\"sourceWorkId\":\"source-work\",\"targetWorkId\":\"work\",\"quantity\":\"20\",\"unit\":\"BOX\",\"verificationStatus\":\"VERIFIED\",\"decision\":\"ACCEPTED\",\"validUntil\":\"2026-10-08T00:00:00Z\"}]}","{}");
        var assertion=assertion(sub,"precondition-recipient-acceptance");engine.check(assertion,Map.of("before",observed),aliases());
        ((ObjectNode)observed.at("/data/rawRows/recipientAcceptances/0")).put("issuedById","delegator");assertThrows(AssertionError.class,()->engine.check(assertion,Map.of("before",observed),aliases()));
        ((ObjectNode)observed.at("/data/rawRows/recipientAcceptances/0")).put("issuedById","warehouse");((ObjectNode)observed.at("/data/rawRows/recipientAcceptances/0")).put("validUntil","2026-10-06T00:00:00Z");assertThrows(AssertionError.class,()->engine.check(assertion,Map.of("before",observed),aliases()));
    }
}
