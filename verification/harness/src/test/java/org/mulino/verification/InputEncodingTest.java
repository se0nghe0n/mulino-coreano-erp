package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Only deterministic input serialization; these are not actual-scenario results. */
final class InputEncodingTest {
    private final JsonNode aliases=Json.parse("{\"P\":\"00000000-0000-4000-8000-000000000001\"}");
    private Map<String,JsonNode> results() {
        return Map.of("work",Json.parse("{\"driverStatus\":\"EXECUTED\",\"provenance\":{\"scopeComplete\":true},\"response\":{\"workId\":\"00000000-0000-4000-8000-000000000002\",\"quantity\":\"999\"}}"));
    }
    private ObjectNode encoding(JsonNode source,String mode) {
        return Json.object().set("$encode",Json.object().put("encoding",mode).set("source",source));
    }
    private JsonNode source() {
        return Json.parse("{\"workId\":{\"$result\":{\"actionId\":\"work\",\"pointer\":\"/response/workId\"}},\"quantity\":\"60\",\"itemId\":{\"$alias\":\"P\"},\"nested\":{\"z\":[true,null,1,\"창고\"],\"a\":\"\\n\"}}");
    }
    @Test void resolvesIdentityThenProducesIdenticalCanonicalUtf8BytesAndHash() throws Exception {
        var resolver=new ReferenceResolver(results(),aliases);
        var original=source();
        String text=resolver.resolve(encoding(original,"json")).asText();
        assertEquals("{\"itemId\":\"00000000-0000-4000-8000-000000000001\",\"nested\":{\"a\":\"\\n\",\"z\":[true,null,1,\"창고\"]},\"quantity\":\"60\",\"workId\":\"00000000-0000-4000-8000-000000000002\"}",text);
        byte[] bytes=text.getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(bytes,Base64.getDecoder().decode(resolver.resolve(encoding(original,"base64")).asText()));
        assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),resolver.resolve(encoding(original,"sha256")).asText());
        assertTrue(original.path("itemId").has("$alias"));
        var reverse=Json.object();var keys=new ArrayList<String>();original.fieldNames().forEachRemaining(keys::add);Collections.reverse(keys);keys.forEach(k->reverse.set(k,original.get(k)));
        assertEquals(text,resolver.resolve(encoding(reverse,"json")).asText());
    }
    @Test void unresolvedMalformedAndBusinessValueReferencesFailClosed() {
        var resolver=new ReferenceResolver(results(),aliases);
        for(String bad:List.of("{\"id\":{\"$alias\":\"missing\"}}", "{\"id\":{\"$alias\":\"P\",\"extra\":1}}",
                "{\"quantity\":{\"$result\":{\"actionId\":\"work\",\"pointer\":\"/response/quantity\"}}}",
                "{\"id\":{\"$result\":{\"actionId\":\"work\",\"pointer\":\"/response/missingId\"}}}"))
            assertThrows(Throwable.class,()->resolver.resolve(encoding(Json.parse(bad),"json")));
        var unavailable=new ReferenceResolver(Map.of("work",StepResult.missing("work","NOT_IMPLEMENTED").toJson()),aliases);
        assertThrows(AssertionError.class,()->unavailable.resolve(encoding(source(),"json")));
    }
    @Test void rejectsExtraKeysInvalidEncodingNestingAndBounds() {
        var resolver=new ReferenceResolver(results(),aliases);
        for(String mode:List.of("eval","utf16","BASE64","json ")) assertThrows(IllegalArgumentException.class,()->resolver.resolve(encoding(source(),mode)));
        var extra=encoding(source(),"json");extra.put("other",true);assertThrows(IllegalArgumentException.class,()->resolver.resolve(extra));
        var inner=encoding(source(),"json");((ObjectNode)inner.path("$encode")).put("other",true);assertThrows(IllegalArgumentException.class,()->resolver.resolve(inner));
        assertThrows(IllegalArgumentException.class,()->resolver.resolve(encoding(Json.array(),"json")));
        assertThrows(IllegalArgumentException.class,()->resolver.resolve(encoding(encoding(source(),"json"),"json")));
        JsonNode deep=Json.object();for(int i=0;i<34;i++) deep=Json.object().set("next",deep);final JsonNode overDepth=deep;
        assertThrows(IllegalArgumentException.class,()->resolver.resolve(encoding(overDepth,"json")));
        assertThrows(IllegalArgumentException.class,()->resolver.resolve(encoding(Json.parse("{\"$alias\":\"P\"}"),"json")));
        var wide=Json.object();for(int i=0;i<4096;i++)wide.put("k"+i,i);
        assertThrows(IllegalArgumentException.class,()->resolver.resolve(encoding(wide,"json")));
        // UTF-8 size is bounded, not just Java's UTF-16 string length.
        assertThrows(IllegalArgumentException.class,()->resolver.resolve(encoding(Json.object().put("text","가".repeat(22000)),"json")));
    }
    @Test void cannotEncodeExpectedIdentityOrSneakIntoTransformLiteral() {
        var resolver=new ReferenceResolver(results(),aliases);
        assertThrows(IllegalArgumentException.class,()->resolver.identity(encoding(source(),"json")));
        var assertion=Json.parse("{\"op\":\"equals\",\"source\":{\"actionId\":\"work\",\"pointer\":\"/response/workId\"},\"expected\":null}");
        ((ObjectNode)assertion).set("expected",encoding(source(),"json"));
        assertThrows(IllegalArgumentException.class,()->new AssertionEngine().check(assertion,results(),aliases));
        var mutation=Json.object().set("$transform",Json.object().put("operation","jsonPointerReplace").put("pointer","/workId")
            .set("source",Json.parse("{\"$result\":{\"actionId\":\"work\",\"pointer\":\"/response\"}}")));
        ((ObjectNode)mutation.path("$transform")).set("value",encoding(source(),"json"));
        assertThrows(IllegalArgumentException.class,()->resolver.resolve(mutation));
    }
    @Test void allAuthoredSourceGraphsResolveFreshIdentitiesInOrderWithoutChangingBytes() throws Exception {
        var root=java.nio.file.Path.of(System.getProperty("repo.root"));int receipts=0,events=0;
        for(String name:List.of("T09","T11","T12")) {
            var file=Json.read(root.resolve("verification/cases/"+name+"/case.json"));
            for(var sub:file.path("subcases")) {
                var fixture=Json.read(root.resolve(sub.path("fixtureRef").asText()));var ids=Json.object();
                fixture.path("aliases").fieldNames().forEachRemaining(k->ids.put(k,UUID.randomUUID().toString()));
                var captured=new HashMap<String,JsonNode>();var payloads=new HashMap<String,byte[]>();
                for(var action:sub.path("actions")) {
                    String cap=action.path("capabilityId").asText(),aid=action.path("id").asText();
                    if(Set.of("attachEvidence","recordActivity","receiveProvisional","matchSourceIdentity","linkCanonicalOccurrence","confirmReceipt").contains(cap)) {
                        // Unit responses contain only issued identity/revision fields; no observed
                        // business quantity is available to the encoder as an expected source.
                        var request=new ReferenceResolver(captured,ids).resolve(action.path("request"));
                        var slots=request.path("slots");assertFalse(request.toString().contains("$encode"));
                        if(cap.equals("attachEvidence")) {
                            byte[] bytes=Base64.getDecoder().decode(slots.path("contentBase64").asText());payloads.put(aid,bytes);
                            assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),slots.path("expectedHash").asText());
                        }
                        if(cap.equals("recordActivity")) {
                            events++;assertFalse(slots.path("payload").asText().contains("fixtureAlias"));
                            assertFalse(slots.path("payload").asText().contains("priorSourceAction"));
                            if(payloads.containsKey(aid+"-document"))assertArrayEquals(payloads.get(aid+"-document"),slots.path("payload").asText().getBytes(StandardCharsets.UTF_8));
                        }
                        if(cap.equals("confirmReceipt"))receipts++;
                    }
                    var response=Json.object().put("id",UUID.randomUUID().toString()).put("workId",UUID.randomUUID().toString())
                        .put("claimId",UUID.randomUUID().toString()).put("revision",1);
                    if(cap.equals("receiveProvisional")) {response.put("revision",0);response.set("effects",Json.object().put("receiptId",UUID.randomUUID().toString()));}
                    if(cap.equals("confirmReceipt")) response.set("effects",Json.object()
                        .put("receiptId",UUID.randomUUID().toString()).put("segmentId",UUID.randomUUID().toString()));
                    if(Set.of("prepareRegulatoryProcedure","recordSubmission").contains(cap)) response.set("effects",Json.object()
                        .put("procedureId",UUID.randomUUID().toString()).put("procedureVersionId",UUID.randomUUID().toString()));
                    captured.put(aid,Json.object().put("driverStatus","EXECUTED").set("response",response));
                    ((ObjectNode)captured.get(aid)).set("provenance",Json.object().put("scopeComplete",true));
                }
            }
        }
        assertEquals(10,receipts);assertEquals(39,events);
    }
    @Test void schemaRestrictsEncoderToInputAndChecksNestedDeclarations() throws Exception {
        var root=java.nio.file.Path.of(System.getProperty("repo.root"));
        var caseFile=root.resolve("verification/cases/T09/case.json");
        ObjectNode c=(ObjectNode)Json.read(caseFile);
        var validator=new ContractValidator(root);
        validator.schema("contracts/acceptance-case.schema.json",c);
        var first=(ObjectNode)c.path("subcases").get(0).path("assertions").get(0);
        first.set("expected",encoding(source(),"sha256"));
        final ObjectNode badAssertion=c;
        assertThrows(IllegalArgumentException.class,()->validator.schema("contracts/acceptance-case.schema.json",badAssertion));
        c=(ObjectNode)Json.read(caseFile);
        var request=(ObjectNode)c.path("subcases").get(0).path("actions").get(1).path("request");
        request.set("invalid",encoding(source(),"eval"));final ObjectNode bad=c;
        assertThrows(IllegalArgumentException.class,()->validator.schema("contracts/acceptance-case.schema.json",bad));
    }
}
