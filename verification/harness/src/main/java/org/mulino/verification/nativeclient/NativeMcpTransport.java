package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.mulino.verification.Json;
import org.mulino.verification.actual.*;
import java.io.IOException;
import java.net.http.*;
import java.time.*;
import java.util.*;

/** Actual loopback MCP. Captured credentials are deliberately absent from every artifact.
 * A signed request is not evidence of the server-resolved actor/grant. That missing hook is explicit. */
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
        var builder=HttpRequest.newBuilder(configuration.baseUri().resolve("/mcp/ontology"))
            .timeout(Duration.ofSeconds(60)).header("Content-Type","application/json")
            .header("Accept","application/json, text/event-stream").header("Mcp-Method",method)
            .header("MCP-Protocol-Version",VERSION).header("Authorization","Bearer "+signer.sign(actor));
        if(name!=null)builder.header("Mcp-Name",name);
        NativeClientPolicy.rejectSecrets(rpc);
        String sent=Instant.now().toString();
        var received=client.send(builder.POST(HttpRequest.BodyPublishers.ofString(rpc.toString())).build(),HttpResponse.BodyHandlers.ofString());
        String completed=Instant.now().toString();
        JsonNode response;
        try { response=Json.parse(received.body()); }
        catch(Exception invalid) { throw new IOException("NON_JSON_MCP_RESPONSE"); }
        NativeClientPolicy.rejectSecrets(response);
        var receipt=evidence.base("ACTUAL_MCP_TRANSPORT",scope).put("actionId",action)
            .put("requestSentAt",sent).put("responseReceivedAt",completed).put("httpStatus",received.statusCode())
            .put("endpointPath","/mcp/ontology").put("authenticatedActorStatus","NOT_IMPLEMENTED")
            .put("missingObservation","Server-resolved issuer/subject/audience/organization/grant capture hook absent");
        receipt.set("request",rpc);receipt.set("response",response);
        receipt.set("implementationSource",evidence.source("verification/harness/src/main/java/org/mulino/verification/nativeclient/NativeMcpTransport.java"));
        receipt.set("productSource",evidence.source("backend/src/main/java/com/mulino/adapters/OntologyMcp.java"));
        JsonNode artifact=evidence.save("mcp",receipt);
        NativeClientPolicy.require(response.path("jsonrpc").asText().equals("2.0")&&response.path("id").equals(rpc.path("id")),"MCP_CORRELATION_MISMATCH");
        return new Capture(rpc,response,received.statusCode(),artifact);
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
