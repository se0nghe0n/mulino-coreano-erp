package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.mulino.verification.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import static org.mulino.verification.nativeclient.NativePermittedContext.require;

/** Consumes the transport's validated SERVER_AUTHENTICATED_MCP_CAPTURE_V1 artifact API.
 * Never promotes a client-signed JWT or declared grant to server authentication evidence. */
final class NativePermittedContextCapture {
    static StepResult result(String id,JsonNode actor,JsonNode scope,NativePermittedContext.Collected collected,
                             NativeIntegrationEvidence store) throws Exception {
        var refs=new ArrayList<String>();var wires=new ArrayList<ObjectNode>();var unattributed=Json.array();ObjectNode anchorWire=null,anchorAuth=null;
        String unavailable=null;JsonNode principal=null;Instant authorityAt=null;
        for(var capture:collected.captures()) {
            refs.add(Json.required(capture.artifact(),"path"));
            try {
                var receipt=receipt(capture,store);var server=receipt.path("serverAuthenticatedCapture");
                principal(server,actor,scope);
                // All constituent reads must use the same actual server identity.
                var current=server.path("identity");
                if(principal==null){principal=current;authorityAt=Instant.parse(Json.required(server,"authorityAsOf"));}
                require(principal.equals(current),"PERMITTED_CONTEXT_PRINCIPAL_CHANGED");
                String operation=capture.request().path("params").path("name").asText();
                if(!NativePermittedContext.success(capture)||NativePermittedContextGrants.emptyRead(operation,capture.request().path("params").path("arguments"),capture.businessResponse())) {
                    // Empty/denied reads expose no business target. Preserve raw authentication without inventing a used grant.
                    unattributed.add(Json.object().put("rawWireArtifactRef",capture.artifact().path("path").asText()).put("grantAttribution","NO_BUSINESS_TARGET_ASSERTED"));continue;
                }
                var auth=identity(server,actor,scope,operation,capture.request().path("params").path("arguments"),capture.businessResponse());
                // Capture each current authority observation; no assumption of a shared DB transaction.
                var wire=wire(id,scope,capture,receipt,auth);wires.add(wire);
                if(capture==collected.projection()){anchorWire=wire;anchorAuth=auth;}
            }catch(Exception invalid){unavailable=safe(invalid);}
        }
        if(unavailable!=null||anchorWire==null) {
            var diagnostic=store.base("ACTUAL_PERMITTED_CONTEXT_DIAGNOSTIC",scope).put("actionId",id)
                .put("status","NOT_RUN").put("missingObservation",unavailable==null?"SERVER_AUTHENTICATED_CAPTURE_MISSING":unavailable)
                .put("actualModelCalls",0).put("productReadCount",collected.captures().size());
            diagnostic.set("rawReadArtifacts",Json.MAPPER.valueToTree(refs));
            refs.add(store.save("permitted-context-unavailable",diagnostic).path("path").asText());
            var absent=StepResult.missing(id,unavailable==null?"SERVER_AUTHENTICATED_CAPTURE_MISSING":unavailable);
            return new StepResult(id,absent.driverStatus(),null,null,absent.reason(),absent.provenance(),refs);
        }
        String projectionRef=null;
        for(var wire:wires){var artifact=store.save("permitted-context-api",wire);String ref=Json.required(artifact,"path");refs.add(ref);if(wire==anchorWire)projectionRef=ref;}
        var manifest=store.base("ACTUAL_PERMITTED_CONTEXT_READ_PLAN",scope).put("actionId",id)
            .put("projectionReadRef",projectionRef).put("actualModelCalls",0).put("authorityAsOf",authorityAt.toString());
        manifest.set("readArtifacts",Json.MAPPER.valueToTree(refs));manifest.set("unattributedEmptyOrDeniedReads",unattributed);
        refs.add(store.save("permitted-context-plan",manifest).path("path").asText());
        var response=collected.projection().businessResponse();
        var data=Json.object().put("projectionReadRef",projectionRef);data.set("snapshotRevision",response.path("snapshotRevision"));
        data.set("permittedContext",collected.context());
        return new StepResult(id,StepResult.DriverStatus.EXECUTED,data,response,null,provenance(anchorAuth,response,anchorWire),refs);
    }
    static StepResult ordinary(String id,JsonNode actor,JsonNode scope,NativeMcpTransport.Capture capture,NativeIntegrationEvidence store) throws Exception {
        var receipt=receipt(capture,store);var auth=identity(receipt.path("serverAuthenticatedCapture"),actor,scope,capture.request().path("params").path("name").asText(),capture.request().path("params").path("arguments"),capture.businessResponse());
        var wire=wire(id,scope,capture,receipt,auth);var artifact=store.save("authenticated-api",wire);
        var data=Json.object().put("httpStatus",capture.status()).put("projectionReadRef",artifact.path("path").asText());
        if(capture.businessResponse().hasNonNull("snapshotRevision"))data.set("snapshotRevision",capture.businessResponse().path("snapshotRevision"));
        return new StepResult(id,StepResult.DriverStatus.EXECUTED,data,capture.businessResponse(),null,provenance(auth,capture.businessResponse(),wire),
            List.of(capture.artifact().path("path").asText(),artifact.path("path").asText()));
    }
    private static ObjectNode provenance(JsonNode auth,JsonNode response,JsonNode wire) {
        var p=Json.object().put("adapter","native-permitted-context").put("adapterVersion","1.0.0")
            .put("buildVersion","source-captured").put("source",wire.path("evidenceClass").asText().equals("ACTUAL_API_CAPTURE")
                ?"ACTUAL_AUTHENTICATED_TRANSPORT":"CAPTURED_CONTRACT_SELFTEST").put("independent",false).put("scopeComplete",true);
        p.set("authenticatedActor",auth);p.putNull("sourceQuery");p.set("snapshot",response.hasNonNull("snapshotRevision")
            ?Json.object().set("revision",response.path("snapshotRevision")):Json.MAPPER.nullNode());return p;
    }
    static JsonNode receipt(NativeMcpTransport.Capture capture,NativeIntegrationEvidence store) throws Exception {
        Path path=store.root.resolve(Json.required(capture.artifact(),"path")).toRealPath();
        require(path.startsWith(store.root.toRealPath())&&Json.sha256(path).equals(Json.required(capture.artifact(),"sha256")),"PERMITTED_CONTEXT_CAPTURE_HASH_MISMATCH");
        var receipt=Json.read(path);var server=receipt.path("serverAuthenticatedCapture");
        require(Set.of("ACTUAL_MCP_TRANSPORT","CAPTURED_CONTRACT_SELFTEST").contains(receipt.path("evidenceClass").asText()),"PERMITTED_CONTEXT_CAPTURE_CLASS_INVALID");
        require(receipt.path("authenticatedActorStatus").asText().equals("CAPTURED")
            &&server.path("recordType").asText().equals("SERVER_AUTHENTICATED_MCP_CAPTURE_V1"),"SERVER_AUTHENTICATED_CAPTURE_MISSING");
        require(receipt.path("request").equals(capture.request())&&receipt.path("response").equals(capture.response())
            &&receipt.path("httpStatus").asInt(-1)==capture.status()&&server.path("httpStatus").asInt(-1)==capture.status()
            &&server.path("rpcId").equals(capture.request().path("id"))
            &&server.path("requestSha256").asText().equals(NativeClientPolicy.sha(capture.request().toString()))
            &&server.path("rpcMethod").equals(capture.request().path("method"))
            &&server.path("toolName").equals(capture.request().path("params").path("name")),"PERMITTED_CONTEXT_CAPTURE_WIRE_MISMATCH");
        require(server.path("observationPhase").asText().equals("BEFORE_DISPATCH_READ_SNAPSHOT")
            &&server.path("actionAuthorization").asText().equals("NOT_ASSERTED"),"PERMITTED_CONTEXT_CAPTURE_PHASE_INVALID");
        return receipt;
    }
    private static void principal(JsonNode server,JsonNode requested,JsonNode scope) {
        var identity=server.path("identity");
        for(String field:List.of("issuer","subject"))require(identity.path(field).equals(requested.path(field)),"PERMITTED_CONTEXT_IDENTITY_MISMATCH");
        require(identity.path("organizationId").equals(scope.path("organizationId")),"PERMITTED_CONTEXT_IDENTITY_MISMATCH");
        require(identity.path("audiences").isArray(),"PERMITTED_CONTEXT_IDENTITY_MISSING");
        boolean audience=false;for(JsonNode a:identity.path("audiences"))audience|=a.equals(requested.path("audience"));
        require(audience,"PERMITTED_CONTEXT_IDENTITY_MISMATCH");UUID.fromString(Json.required(identity,"actorId"));
    }
    static ObjectNode identity(JsonNode server,JsonNode requested,JsonNode scope,String operation,JsonNode request,JsonNode response) {
        principal(server,requested,scope);var identity=server.path("identity");
        Instant now=Instant.parse(Json.required(server,"authorityAsOf"));
        var grants=new ArrayList<JsonNode>();require(server.path("grants").isArray(),"PERMITTED_CONTEXT_GRANTS_MISSING");
        if(NativePermittedContextGrants.READS.contains(operation))grants.addAll(NativePermittedContextGrants.applicable(server,operation,NativePermittedContextGrants.targets(operation,request,response)));
        else for(JsonNode grant:server.path("grants")) {
            require(grant.path("actorId").equals(identity.path("actorId")),"PERMITTED_CONTEXT_GRANT_ACTOR_MISMATCH");
            boolean action=false;for(JsonNode a:grant.path("actions"))action|=operation.equals(a.path("capabilityId").asText());
            if(action&&NativePermittedContextGrants.active(grant,now))grants.add(grant);
        }
        // The hook observes candidates, not which of several grants authorized a row. Do not guess from actor payload.
        require(grants.size()==1,"PERMITTED_CONTEXT_GRANT_ATTRIBUTION_UNAVAILABLE");
        String grant=Json.required(grants.getFirst(),"ID");UUID.fromString(grant);
        var auth=Json.object().put("issuer",Json.required(identity,"issuer")).put("subject",Json.required(identity,"subject"))
            .put("audience",requested.path("audience").asText()).put("organizationId",scope.path("organizationId").asText())
            .put("organizationRef",scope.path("organizationId").asText()).put("grantId",grant).put("authorizationGrantRef",grant)
            .put("actorId",Json.required(identity,"actorId"));
        if(grants.getFirst().hasNonNull("delegatorId"))auth.set("delegatorId",grants.getFirst().path("delegatorId"));return auth;
    }
    private static ObjectNode wire(String id,JsonNode scope,NativeMcpTransport.Capture c,JsonNode receipt,JsonNode auth) {
        var wire=Json.object().put("evidenceClass",receipt.path("evidenceClass").asText().equals("ACTUAL_MCP_TRANSPORT")
            ?"ACTUAL_API_CAPTURE":"CAPTURED_CONTRACT_SELFTEST").put("actionId",id);
        wire.set("scope",scope.deepCopy());for(String t:List.of("requestSentAt","responseReceivedAt"))wire.set(t,receipt.path(t));
        wire.set("request",c.request());wire.set("response",c.response());wire.set("authenticatedActor",auth);
        var call=Json.object().put("callId",c.request().path("id").asText()).put("capabilityId",c.request().path("params").path("name").asText())
            .put("wireArtifactRef",c.artifact().path("path").asText()).put("businessResponsePointer","/response/result/structuredContent");
        for(String k:List.of("issuer","subject","audience","organizationRef","authorizationGrantRef"))call.set(k,auth.path(k));
        wire.set("toolCall",call);return wire;
    }
    private static String safe(Exception e){return e.getMessage()!=null&&e.getMessage().matches("[A-Z0-9_]+")?e.getMessage():"PERMITTED_CONTEXT_CAPTURE_INVALID";}
    private NativePermittedContextCapture() {}
}
