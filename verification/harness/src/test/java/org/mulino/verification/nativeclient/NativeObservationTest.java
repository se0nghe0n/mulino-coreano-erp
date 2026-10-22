package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.Test;
import org.mulino.verification.*;
import org.mulino.verification.modelbinding.*;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Adversarial observer contracts only; no model invocation and no business oracle simulation. */
class NativeObservationTest {
    private final Path root=Path.of(System.getProperty("repo.root"));
    private ObjectNode physical() {
        var o=Json.object();o.set("rawRows",Json.parse("{\"t\":[{\"ID\":\"r\",\"quantity\":\"60\",\"unit\":\"BOX\"}]}"));
        o.set("physicalCells",Json.parse("[{\"dataset\":\"receipt\",\"attribute\":\"quantity\",\"table\":\"receipts\",\"column\":\"quantity\",\"rowId\":\"r\",\"value\":\"60\",\"unit\":\"BOX\",\"rawRowPointer\":\"/rawRows/t/0/quantity\"}]"));
        o.set("sourceEvidence",Json.parse("{\"t\":{\"rowPointer\":\"/rawRows/t\",\"sourceQuery\":{\"sql\":\"SELECT ID,quantity,unit FROM receipts WHERE organizationId=?\"}}}"));return o;
    }
    @Test void rawRowObjectAndSeparateCellArrayAreBothRequired() throws Exception {
        var o=physical();PhysicalObservation.validate(new ContractValidator(root),o);
        o.set("rawRows",o.path("physicalCells"));assertThrows(IllegalArgumentException.class,()->PhysicalObservation.validateLineage(o));
        assertEquals("object",Json.read(root.resolve("contracts/acceptance-observation.schema.json")).path("properties").path("rawRows").path("type").asText());
    }
    @Test void cellValueIdentityTableColumnUnitAndLineageMutantsFail() {
        for(String field:List.of("value","rowId","table","column","unit","rawRowPointer")) {
            var o=physical();((ObjectNode)o.path("physicalCells").get(0)).put(field,"forged");
            assertThrows(IllegalArgumentException.class,()->PhysicalObservation.validateLineage(o),field);
        }
    }
    @Test void duplicateSemanticCellsAndFakeCanonicalPhysicalIdentityFail() {
        var o=physical();((ArrayNode)o.path("physicalCells")).add(o.path("physicalCells").get(0).deepCopy());
        var duplicate=o;assertThrows(IllegalArgumentException.class,()->PhysicalObservation.validateLineage(duplicate));
        o=physical();((ObjectNode)o.path("physicalCells").get(0)).put("physicalScopeId","made-up");var forged=o;
        assertThrows(IllegalArgumentException.class,()->PhysicalObservation.validateLineage(forged));
    }
    @Test void missingMappingsCannotTurnEmptyCellsIntoZero() throws Exception {
        var evaluator=new BindingEvaluator(new BindingContract(root));
        var result=Json.object();var data=Json.object();data.set("physicalCells",Json.array());data.set("rawRows",Json.object().set("receipt",Json.array()));result.set("data",data);
        var assertion=Json.parse("{\"path\":\"state.receipt.cumulative.value\",\"expected\":\"0\"}");
        assertThrows(IllegalArgumentException.class,()->evaluator.assertSemantic(assertion,Json.object(),result,List.of(),Json.object()));
        data.set("sourceEvidence",Json.object().set("receiptOccurrence.quantity",Json.object().put("complete",true).put("rowPointer","/rawRows/receipt")));
        assertDoesNotThrow(()->evaluator.assertSemantic(assertion,Json.object(),result,List.of(),Json.object()));
        ((ArrayNode)data.path("rawRows").path("receipt")).add(Json.object().put("ID","unmapped"));
        assertThrows(IllegalArgumentException.class,()->evaluator.assertSemantic(assertion,Json.object(),result,List.of(),Json.object()));
    }
    private ObjectNode projection() {
        return (ObjectNode)Json.parse("{\"recipe\":\"APPLICATION_QUERIES_V1_SIMPLE_OBJECT\",\"actorId\":\"actor\",\"asOf\":\"2026-10-07T09:00:00Z\",\"knownAt\":\"2026-10-07T09:00:01Z\",\"request\":{\"operation\":\"getObject\",\"id\":\"place\",\"scope\":{\"organizationId\":\"org\",\"objectType\":\"Place\"}},\"rows\":{\"object\":{\"ID\":\"place\",\"organizationId\":\"org\",\"name\":\"창고\"},\"relations\":[]}}");
    }
    @Test void projectionHashUsesNormalizedOrderedRowsAndActorTimeScope() {
        var p=projection();String expected=Json.sha256Text("org|actor|2026-10-07T09:00:00Z|2026-10-07T09:00:01Z|{\"objectType\":\"Place\",\"organizationId\":\"org\"}|{\"ID\":\"place\",\"name\":\"창고\",\"organizationId\":\"org\",\"relations\":[]}");
        assertEquals(expected,ProjectionRevisionObservation.recompute(p));
        p.put("actorId","other");assertNotEquals(expected,ProjectionRevisionObservation.recompute(p));
        p=projection();p.put("knownAt","2026-10-07T09:00:02Z");assertNotEquals(expected,ProjectionRevisionObservation.recompute(p));
        p=projection();((ObjectNode)p.path("rows").path("object")).put("name","다른 창고");assertNotEquals(expected,ProjectionRevisionObservation.recompute(p));
    }
    @Test void worldProjectionAndRelatedVisibilityAreRefused() {
        var p=projection();((ObjectNode)p.path("request").path("scope")).put("itemId","item");
        assertThrows(IllegalArgumentException.class,()->ProjectionRevisionObservation.recompute(p));
        var relation=projection();((ArrayNode)relation.path("rows").path("relations")).add(Json.object().put("ID","relation"));
        assertThrows(IllegalArgumentException.class,()->ProjectionRevisionObservation.recompute(relation));
    }
    @Test void missingSlotSetComesFromKoreanQuestionAndOriginalStringIsRetained() {
        String original="  수량과 목적지를 알려 주세요.\n";var result=NativeResponseSemantics.extract(original);
        assertTrue(result.path("complete").asBoolean());assertEquals(original,result.path("sourceResponse").asText());
        assertEquals(Json.parse("[\"destinationRef\",\"quantity\"]"),result.path("claims").path("missingSlots"));
        assertEquals("NEEDS_INPUT",result.path("claims").path("execution").asText());
        assertTrue(NativeResponseSemantics.extract(Normalizer.normalize("수량을 알려 주세요.",Normalizer.Form.NFD)).path("complete").asBoolean());
    }
    @Test void explicitUncertaintyAndCompletionAreClaimsNeverProof() {
        var result=NativeResponseSemantics.extract("요청을 실행했습니다. 업무가 완료되었습니다. 결과는 미확인입니다.");
        assertTrue(result.path("complete").asBoolean());assertEquals("FULFILLED",result.path("claims").path("completion").asText());
        assertEquals("EXPLICIT_UNKNOWN",result.path("claims").path("uncertainty").asText());
        assertTrue(result.path("claims").path("assertions").isEmpty());
    }
    @Test void arbitraryNegationConditionalQuotedOrSelfLabeledProseIsUnknown() {
        for(String text:List.of("업무가 완료되지 않았습니다.","요청을 실행했습니다. 수량 60입니다.","완료되면 책임이 해제됩니다.","\"요청을 실행했습니다.\"", "{\"execution\":\"EXECUTED\"}","수량을 알려 주세요. 요청을 실행했습니다.","수량과 수량을 알려 주세요."))
            assertFalse(NativeResponseSemantics.extract(text).path("complete").asBoolean(),text);
    }
    @Test void clarificationRequiresExactIndependentSlotSet() {
        var o=Json.object();o.set("claims",NativeResponseSemantics.extract("수량과 목적지를 알려 주세요.").path("claims"));
        assertDoesNotThrow(()->FinalResponseClarification.checkSlots(o,Json.parse("[\"quantity\",\"destinationRef\"]")));
        assertThrows(IllegalArgumentException.class,()->FinalResponseClarification.checkSlots(o,Json.parse("[\"quantity\"]")));
        ((ObjectNode)o.path("claims")).remove("missingSlots");assertThrows(IllegalArgumentException.class,()->FinalResponseClarification.checkSlots(o,Json.array()));
    }
    private ObjectNode host() {
        var c=Json.object().put("startedAt","2026-10-22T08:00:00Z").put("finishedAt","2026-10-22T08:00:10Z").put("processId",101);
        c.set("command",Json.array().add("actual-client").add("app-server"));c.set("config",Json.object().put("model","fake"));
        c.set("result",Json.object().put("finalResponse","수량을 알려 주세요."));var events=Json.array();c.set("events",events);
        add(events,"sent",Json.parse("{\"id\":1,\"method\":\"thread/start\",\"params\":{}}"));
        add(events,"received",Json.parse("{\"id\":1,\"result\":{\"thread\":{\"id\":\"t\"},\"model\":\"actual-model\",\"serviceTier\":\"actual-tier\"}}"));
        add(events,"sent",Json.parse("{\"id\":2,\"method\":\"turn/start\",\"params\":{\"threadId\":\"t\"}}"));
        add(events,"received",Json.parse("{\"id\":2,\"result\":{\"turn\":{\"id\":\"turn\"}}}"));
        add(events,"received",Json.parse("{\"method\":\"item/completed\",\"params\":{\"threadId\":\"t\",\"turnId\":\"turn\",\"item\":{\"type\":\"agentMessage\",\"phase\":\"final_answer\",\"text\":\"수량을 알려 주세요.\"}}}"));
        add(events,"received",Json.parse("{\"method\":\"turn/completed\",\"params\":{\"threadId\":\"t\",\"turn\":{\"id\":\"turn\",\"status\":\"completed\"}}}"));
        add(events,"lifecycle",Json.parse("{\"processId\":101,\"termination\":\"HOST_CLOSED_DEDICATED_PROCESS\",\"exitCode\":0}"));return c;
    }
    private void add(ArrayNode events,String direction,JsonNode message) {events.add(Json.object().put("direction",direction).put("at","2026-10-22T08:00:01Z").set("message",message));}
    @Test void hostIdentityComesFromRawResponseNotConfig() {var o=NativeHostObserver.extract(host());assertTrue(o.path("complete").asBoolean(),o.toString());assertEquals("actual-model",o.path("model").asText());}
    @Test void forgedFinalResponseMissingTerminationForeignScopeAndBadTimesFail() {
        for(String mutation:List.of("final","termination","scope","time")) {
            var c=host();switch(mutation){
                case "final"->((ObjectNode)c.path("result")).put("finalResponse","fake");
                case "termination"->((ArrayNode)c.path("events")).remove(6);
                case "scope"->((ObjectNode)c.path("events").get(4).path("message").path("params")).put("turnId","foreign");
                case "time"->((ObjectNode)c.path("events").get(4)).put("at","2027-01-01T00:00:00Z");
            }assertFalse(NativeHostObserver.extract(c).path("complete").asBoolean(),mutation);
        }
    }
    @Test void builtInExecutionAndOrphanRpcCannotHideInRawEvents() {
        var c=host();var message=Json.parse("{\"method\":\"item/started\",\"params\":{\"threadId\":\"t\",\"turnId\":\"turn\",\"item\":{\"type\":\"commandExecution\"}}}");
        ((ArrayNode)c.path("events")).insert(4,Json.object().put("direction","received").put("at","2026-10-22T08:00:01Z").set("message",message));
        assertFalse(NativeHostObserver.extract(c).path("complete").asBoolean());
        var orphan=host();((ObjectNode)orphan.path("events").get(4).path("message")).put("id",999);
        assertFalse(NativeHostObserver.extract(orphan).path("complete").asBoolean());
    }
    @Test void unknownSemanticCaptureRemainsNotImplementedEvenWithAnExecutionClaim() throws Exception {
        var store=new NativeIntegrationEvidence(root);var capture=host();capture.put("evidenceClass","ACTUAL_NATIVE_CLIENT");
        var file=store.save("contract-host",capture);var result=NativeFinalResponseObserver.observe("turn",Json.object(),root.resolve(file.path("path").asText()),capture,store);
        assertEquals(StepResult.DriverStatus.NOT_IMPLEMENTED,result.driverStatus());
        assertEquals(capture.path("result").path("finalResponse"),Json.read(root.resolve(result.artifactRefs().get(1))).path("sourceResponse"));
    }
}
