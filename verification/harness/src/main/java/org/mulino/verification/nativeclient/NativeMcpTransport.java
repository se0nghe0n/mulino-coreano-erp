package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.mulino.verification.Json;
import org.mulino.verification.actual.*;
import java.io.IOException;
import java.net.http.*;
import java.time.*;
import java.util.*;

/** Actual loopback MCP with a single-use server observation bound to the exact HTTP bytes.
 * Authentication/grant observations deliberately make no action authorization assertion. */
final class NativeMcpTransport {
    private static final String VERSION="2026-07-28";
    private final ActualConfiguration configuration;
    private final NativeIntegrationEvidence evidence;
    private final JwtSigner signer;
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NEVER).build();
    record Capture(JsonNode request,JsonNode response,int status,JsonNode artifact) {
        JsonNode businessResponse() { return response.path("result").path("structuredContent"); }
    }
    NativeMcpTransport(ActualConfiguration configuration,NativeIntegrationEvidence evidence) {
        this.configuration=configuration;this.evidence=evidence;signer=new JwtSigner(configuration);
    }
    Capture send(String action,String method,String name,JsonNode arguments,JsonNode actor,JsonNode scope) throws IOException,InterruptedException {
        NativeClientPolicy.require(Set.of("tools/list","tools/call").contains(method),"UNSUPPORTED_MCP_METHOD");
        NativeClientPolicy.require(actor.path("issuer").asText().equals(configuration.issuer())&&
            actor.path("audience").asText().equals(configuration.audience()),"EXPLICIT_IDENTITY_BINDING_REQUIRED");
        var rpc=Json.object().put("jsonrpc","2.0").put("id",UUID.randomUUID().toString()).put("method",method);
        var params=rpc.putObject("params");
        if(method.equals("tools/call")) {
            NativeClientPolicy.require(name!=null&&name.matches("[A-Za-z][A-Za-z0-9]*")&&arguments.isObject(),"INVALID_TOOL_REQUEST");
            params.put("name",name).set("arguments",arguments.deepCopy());
        }
        var meta=params.putObject("_meta");meta.put("io.modelcontextprotocol/protocolVersion",VERSION);
        meta.set("io.modelcontextprotocol/clientCapabilities",Json.object());
        String nonce=UUID.randomUUID().toString(),credential=signer.sign(actor);
        var builder=HttpRequest.newBuilder(configuration.baseUri().resolve("/mcp/ontology"))
            .timeout(Duration.ofSeconds(60)).header("Content-Type","application/json")
            .header("Accept","application/json, text/event-stream").header("Mcp-Method",method)
            .header("MCP-Protocol-Version",VERSION).header("Authorization","Bearer "+credential)
            .header("X-Mulino-Verification-Capture",nonce);
        if(name!=null)builder.header("Mcp-Name",name);
        NativeClientPolicy.rejectSecrets(rpc);
        String sent=Instant.now().toString();
        var received=client.send(builder.POST(HttpRequest.BodyPublishers.ofString(rpc.toString())).build(),HttpResponse.BodyHandlers.ofString());
        String completed=Instant.now().toString();
        JsonNode response;
        try { response=Json.parse(received.body()); }
        catch(Exception invalid) { response=Json.object().put("transportError","NON_JSON_MCP_RESPONSE"); }
        NativeClientPolicy.rejectSecrets(response);
        var receipt=evidence.base("ACTUAL_MCP_TRANSPORT",scope).put("actionId",action)
            .put("requestSentAt",sent).put("responseReceivedAt",completed).put("httpStatus",received.statusCode())
            .put("endpointPath","/mcp/ontology").put("authenticatedActorStatus","UNAVAILABLE");
        receipt.set("request",rpc);receipt.set("response",response);
        receipt.set("implementationSource",evidence.source("verification/harness/src/main/java/org/mulino/verification/nativeclient/NativeMcpTransport.java"));
        receipt.set("productSource",evidence.source("backend/src/main/java/com/mulino/adapters/OntologyMcp.java"));
        String failure=null;
        try {
            String captureId=received.headers().firstValue("X-Mulino-Verification-Receipt").orElseThrow(()->new IllegalStateException("SERVER_CAPTURE_ABSENT"));
            NativeClientPolicy.require(captureId.matches("[a-f0-9-]{36}"),"SERVER_CAPTURE_ID_INVALID");
            UUID.fromString(captureId);
            var read=HttpRequest.newBuilder(configuration.baseUri().resolve("/internal/verification/mcp-captures/"+captureId))
                .timeout(Duration.ofSeconds(10)).header("Authorization","Bearer "+credential).header("Accept","application/json").GET().build();
            var snapshot=client.send(read,HttpResponse.BodyHandlers.ofString());
            NativeClientPolicy.require(snapshot.statusCode()==200,"SERVER_CAPTURE_UNAVAILABLE");
            JsonNode observation=Json.parse(snapshot.body());NativeClientPolicy.rejectSecrets(observation);
            validateServerCapture(observation,captureId,nonce,rpc,received.body(),received.statusCode(),actor,scope,sent,completed);
            receipt.put("authenticatedActorStatus","CAPTURED");receipt.set("serverAuthenticatedCapture",observation);
            var sources=receipt.putArray("serverCaptureSources");
            for(String file:List.of("McpCaptureConfiguration","McpIdentitySnapshot","McpCaptureStore","McpCaptureFilter","McpCaptureController"))
                sources.add(evidence.source("backend/src/main/java/com/mulino/adapters/verification/"+file+".java"));
        } catch(InterruptedException interrupted) { Thread.currentThread().interrupt();failure="SERVER_CAPTURE_INTERRUPTED"; }
        catch(Exception unavailable) {
            // Exception messages/HTTP error bodies may contain credentials or arbitrary server text.
            failure=unavailable instanceof IllegalStateException&&unavailable.getMessage()!=null&&unavailable.getMessage().matches("[A-Z0-9_]{1,100}")
                ?unavailable.getMessage():"SERVER_CAPTURE_INVALID";
        }
        if(failure!=null) {
            receipt.put("authenticatedActorStatus","UNAVAILABLE").put("missingObservation",failure);
            receipt.remove("serverAuthenticatedCapture");
        }
        JsonNode artifact=evidence.save("mcp",receipt);
        NativeClientPolicy.require(failure==null,failure==null?"SERVER_CAPTURE_INVALID":failure);
        NativeClientPolicy.require(response.path("jsonrpc").asText().equals("2.0")&&response.path("id").equals(rpc.path("id")),"MCP_CORRELATION_MISMATCH");
        return new Capture(rpc,response,received.statusCode(),artifact);
    }
    static void validateServerCapture(JsonNode capture,String id,String nonce,JsonNode rpc,String response,int status,
                                      JsonNode actor,JsonNode scope,String sent,String completed) {
        NativeClientPolicy.require(capture.path("recordType").asText().equals("SERVER_AUTHENTICATED_MCP_CAPTURE_V1")
            &&capture.path("captureId").asText().equals(id)&&capture.path("requestNonce").asText().equals(nonce)
            &&capture.path("endpointPath").asText().equals("/mcp/ontology")
            &&capture.path("requestSha256").asText().equals(NativeClientPolicy.sha(rpc.toString()))
            &&capture.path("responseSha256").asText().equals(NativeClientPolicy.sha(response))
            &&capture.path("httpStatus").isIntegralNumber()&&capture.path("httpStatus").asInt(-1)==status&&capture.path("rpcId").equals(rpc.path("id"))
            &&capture.path("rpcMethod").equals(rpc.path("method"))
            &&capture.path("toolName").asText().equals(rpc.path("params").path("name").asText("")),"SERVER_CAPTURE_CORRELATION_MISMATCH");
        var identity=capture.path("identity");
        NativeClientPolicy.require(identity.isObject()&&identity.path("issuer").equals(actor.path("issuer"))
            &&identity.path("subject").equals(actor.path("subject"))&&identity.path("organizationAlias").equals(actor.path("organizationAlias"))
            &&identity.path("organizationId").equals(scope.path("organizationId"))
            &&identity.path("audiences").isArray()&&!identity.path("audiences").isEmpty(),"SERVER_CAPTURE_IDENTITY_MISMATCH");
        boolean audience=false;
        for(JsonNode a:identity.path("audiences")){NativeClientPolicy.require(a.isTextual()&&!a.asText().isBlank(),"SERVER_CAPTURE_INCOMPLETE");audience|=a.equals(actor.path("audience"));}
        NativeClientPolicy.require(audience,"SERVER_CAPTURE_IDENTITY_MISMATCH");
        for(String field:List.of("organizationId","actorId","stableRequestOwner"))UUID.fromString(Json.required(identity,field));
        NativeClientPolicy.require(capture.path("actionAuthorization").asText().equals("NOT_ASSERTED")
            &&capture.path("observationPhase").asText().equals("BEFORE_DISPATCH_READ_SNAPSHOT"),"SERVER_CAPTURE_INCOMPLETE");
        Instant.parse(Json.required(capture,"authorityAsOf")); // Product authority time may intentionally differ from wall time.
        Instant observed=Instant.parse(Json.required(capture,"observedAt")),finished=Instant.parse(Json.required(capture,"completedAt"));
        NativeClientPolicy.require(!observed.isBefore(Instant.parse(sent))&&!finished.isBefore(observed)
            &&!finished.isAfter(Instant.parse(completed))&&Instant.parse(Json.required(capture,"expiresAt")).isAfter(finished),"SERVER_CAPTURE_TIME_MISMATCH");
        for(String table:List.of("grants","memberships","assignments")) {
            NativeClientPolicy.require(capture.path(table).isArray(),"SERVER_CAPTURE_INCOMPLETE");
            if(table.equals("memberships"))NativeClientPolicy.require(!capture.path(table).isEmpty(),"SERVER_CAPTURE_INCOMPLETE");
            for(JsonNode row:capture.path(table)) {
                UUID.fromString(Json.required(row,"ID"));
                NativeClientPolicy.require(row.path("revision").isIntegralNumber()&&row.path("revision").asLong()>=0,"SERVER_CAPTURE_INCOMPLETE");
                Instant from=Instant.parse(Json.required(row,"validFrom")),until=Instant.parse(Json.required(row,"validUntil"));
                NativeClientPolicy.require(until.isAfter(from),"SERVER_CAPTURE_INCOMPLETE");
                if(row.hasNonNull("revokedAt"))Instant.parse(row.path("revokedAt").asText());
                if(table.equals("grants")) {
                    NativeClientPolicy.require(row.path("actorId").equals(identity.path("actorId"))&&row.path("actions").isArray()
                        &&row.path("scopes").isArray(),"SERVER_CAPTURE_INCOMPLETE");
                    if(row.hasNonNull("delegatorId"))UUID.fromString(row.path("delegatorId").asText());
                    for(JsonNode a:row.path("actions"))Json.required(a,"capabilityId");
                    for(JsonNode s:row.path("scopes")){Json.required(s,"scopeKind");UUID.fromString(Json.required(s,"scopeId"));}
                } else if(table.equals("assignments")) {
                    Json.required(row,"capabilityId");Json.required(row,"scopeKind");UUID.fromString(Json.required(row,"scopeId"));
                }
            }
        }
    }
    ArrayNode tools(Capture capture) {
        NativeClientPolicy.require(capture.status()==200&&capture.response().path("result").path("tools").isArray(),"PRODUCT_TOOL_DISCOVERY_FAILED");
        var tools=Json.array();Set<String> names=new HashSet<>();
        for(JsonNode tool:capture.response().path("result").path("tools")) {
            String name=Json.required(tool,"name");
            NativeClientPolicy.require(names.add(name)&&tool.path("inputSchema").isObject(),"INVALID_PRODUCT_TOOL_SCHEMA");
            var nativeTool=Json.object().put("type","function").put("name",name)
                .put("description",tool.path("description").asText());
            nativeTool.set("inputSchema",tool.path("inputSchema").deepCopy());tools.add(nativeTool);
        }
        NativeCodexClient.ModelInputGuard.tools(tools);return tools;
    }
    static JsonNode nativeReply(Capture capture) {
        boolean success=capture.status()==200&&!capture.response().has("error")&&!capture.response().path("result").path("isError").asBoolean();
        var content=Json.array();
        for(JsonNode part:capture.response().path("result").path("content")) {
            // Current product only exposes text. Never silently discard an unsupported modality.
            NativeClientPolicy.require(part.path("type").asText().equals("text")&&part.path("text").isTextual(),"UNSUPPORTED_PRODUCT_CONTENT");
            content.add(Json.object().put("type","inputText").put("text",part.path("text").asText()));
        }
        if(content.isEmpty())content.add(Json.object().put("type","inputText").put("text",capture.response().toString()));
        return Json.object().put("success",success).set("contentItems",content);
    }
}
