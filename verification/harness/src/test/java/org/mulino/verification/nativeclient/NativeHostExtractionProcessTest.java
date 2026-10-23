package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.Test;
import org.mulino.verification.Json;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/** Executes the real read-only Java extractor process; the wire fixture is never model acceptance. */
class NativeHostExtractionProcessTest {
    private final Path root=Path.of(System.getProperty("repo.root"));
    private ObjectNode capture(String response) {
        var c=Json.object().put("evidenceClass","CAPTURED_CONTRACT_SELFTEST")
            .put("startedAt","2026-10-22T08:00:00Z").put("finishedAt","2026-10-22T08:00:10Z").put("processId",101);
        c.set("command",Json.array().add("contract-client").add("app-server"));c.set("result",Json.object().put("finalResponse",response));
        var events=Json.array();c.set("events",events);
        add(events,"sent",Json.parse("{\"id\":1,\"method\":\"thread/start\",\"params\":{}}"));
        add(events,"received",Json.parse("{\"id\":1,\"result\":{\"thread\":{\"id\":\"t\"},\"model\":\"contract-model\",\"serviceTier\":\"contract-tier\"}}"));
        add(events,"sent",Json.parse("{\"id\":2,\"method\":\"turn/start\",\"params\":{\"threadId\":\"t\"}}"));
        add(events,"received",Json.parse("{\"id\":2,\"result\":{\"turn\":{\"id\":\"turn\"}}}"));
        var finalItem=Json.object().put("type","agentMessage").put("phase","final_answer").put("text",response);
        var params=Json.object().put("threadId","t").put("turnId","turn");params.set("item",finalItem);
        add(events,"received",Json.object().put("method","item/completed").set("params",params));
        add(events,"received",Json.parse("{\"method\":\"turn/completed\",\"params\":{\"threadId\":\"t\",\"turn\":{\"id\":\"turn\",\"status\":\"completed\"}}}"));
        add(events,"lifecycle",Json.parse("{\"processId\":101,\"termination\":\"HOST_CLOSED_DEDICATED_PROCESS\",\"exitCode\":0}"));return c;
    }
    private void add(ArrayNode events,String direction,com.fasterxml.jackson.databind.JsonNode message) {
        events.add(Json.object().put("direction",direction).put("at","2026-10-22T08:00:01Z").set("message",message));
    }
    private NativeHostExtractionProcess.Result extract(ObjectNode capture) throws Exception {
        var store=new NativeIntegrationEvidence(root);var file=store.save("extractor-contract-input",capture);
        return NativeHostExtractionProcess.extract(root.resolve(file.path("path").asText()),store);
    }
    @Test void processReadsCompleteWireAndPreservesExactResponseAndIndependentCustody() throws Exception {
        String text="  수량과 목적지를 알려 주세요.\n";var result=extract(capture(text));var rows=result.observation();
        assertTrue(rows.path("host").path("complete").asBoolean());
        assertTrue(rows.path("semantics").path("complete").asBoolean());
        assertEquals(text,rows.path("semantics").path("sourceResponse").asText());
        assertEquals(Json.parse("[\"destinationRef\",\"quantity\"]"),rows.path("semantics").path("claims").path("missingSlots"));
        var command=result.custody().path("command");
        assertNotEquals(ProcessHandle.current().pid(),command.path("processId").asLong());
        assertEquals(0,command.path("exitCode").asInt(-1));assertFalse(command.path("timedOut").asBoolean());
        assertEquals("EMPTY_NO_PRODUCT_CREDENTIALS",command.path("environmentPolicy").asText());
        assertEquals(5,command.path("implementationClasses").size());
        assertEquals(rows,Json.read(root.resolve(command.path("stdout").path("path").asText())));
        for(String stream:List.of("stdout","stderr"))assertEquals(command.path(stream).path("sha256").asText(),Json.sha256(root.resolve(command.path(stream).path("path").asText())));
        assertFalse(rows.has("evidenceClass"),"A contract fixture must not acquire actual acceptance class from this process");
    }
    @Test void arbitraryTextAndModelSelfLabelsRemainUnknown() throws Exception {
        for(String text:List.of("수령 60 BOX가 완료되었습니다.","{\"execution\":\"EXECUTED\"}")) {
            var result=extract(capture(text));assertFalse(result.observation().path("semantics").path("complete").asBoolean());
            assertEquals(text,result.observation().path("semantics").path("sourceResponse").asText());
        }
    }
    @Test void contradictionBetweenFinalSummaryAndRawEventCannotBecomeHostEvidence() throws Exception {
        var raw=capture("조회했습니다.");((ObjectNode)raw.path("result")).put("finalResponse","요청을 실행했습니다.");
        var result=extract(raw);assertFalse(result.observation().path("host").path("complete").asBoolean());
        assertEquals("FINAL_RESPONSE_DIFFERS_FROM_RAW_EVENT",result.observation().path("host").path("reason").asText());
    }
    @Test void changedHashAndExtraArgumentsAreRejectedByTheActualEntrypoint() throws Exception {
        var successful=extract(capture("조회했습니다."));var base=new ArrayList<String>();
        successful.custody().path("command").path("argv").forEach(x->base.add(x.asText()));
        for(String mutation:List.of("hash","extra")) {
            var args=new ArrayList<>(base);if(mutation.equals("hash"))args.set(args.size()-1,"0".repeat(64));else args.add("ignored");
            var process=new ProcessBuilder(args).redirectErrorStream(true).start();assertTrue(process.waitFor(10,TimeUnit.SECONDS));
            String output=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
            assertEquals(2,process.exitValue());assertEquals(mutation.equals("hash")?"HOST_EXTRACTION_INPUT_CHANGED":"HOST_EXTRACTION_ARGUMENTS_INVALID",output.strip());
        }
    }
    @Test void captureOutsideRepositoryIsRejectedBeforeLaunching() throws Exception {
        Path external=Files.createTempFile("foreign-native-capture-",".json");
        try {Files.writeString(external,capture("조회했습니다.").toString());
            var failure=assertThrows(IllegalStateException.class,()->NativeHostExtractionProcess.extract(external,new NativeIntegrationEvidence(root)));
            assertEquals("EVIDENCE_PATH_ESCAPE",failure.getMessage());
        }finally{Files.deleteIfExists(external);}
    }
}
