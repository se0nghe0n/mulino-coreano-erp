package org.mulino.verification.modelbinding;

import com.fasterxml.jackson.databind.JsonNode;
import org.mulino.verification.Json;
import java.util.*;
import java.time.Instant;

/** Genuine API response sources, distinct from the client's final answer. No server behavior. */
public final class CapturedApiObservation {
    public record Call(String id,String capabilityId,String wireArtifactRef,String responsePointer,
                       JsonNode request,JsonNode response) {}
    private final Map<String,Call> calls;
    private final Map<String,String> assertionSources;
    public CapturedApiObservation(Collection<Call> calls,JsonNode sources) {
        this.calls=new LinkedHashMap<>();assertionSources=new LinkedHashMap<>();
        for(Call c:calls) BindingContract.require(this.calls.put(c.id(),c)==null,"Duplicate captured API call ID");
        BindingContract.require(sources.isArray(),"API assertion sources unobserved");
        for(JsonNode s:sources) {
            String path=Json.required(s,"semanticPath"),id=Json.required(s,"capturedCallId");
            BindingContract.require(this.calls.containsKey(id),"API assertion refers to uncaptured call "+id);
            BindingContract.require(assertionSources.put(path,id)==null,"Ambiguous API assertion source "+path);
        }
    }
    public JsonNode response(String semanticPath) {
        String id=assertionSources.get(semanticPath);
        BindingContract.require(id!=null,"No authenticated captured API source for "+semanticPath);
        return calls.get(id).response();
    }
    public Collection<Call> calls(){return calls.values();}
    public List<String> ids(){return List.copyOf(calls.keySet());}
    public Call source(String path){response(path);return calls.get(assertionSources.get(path));}
    /** Captured raw MCP envelope must match identity and the recorded capability/response. */
    public static Call decode(JsonNode call,JsonNode wire) {
        String id=Json.required(call,"callId"),cap=Json.required(call,"capabilityId");
        BindingContract.require(wire.path("toolCall").equals(call),"Wire metadata differs from recorded call");
        JsonNode request=wire.path("request"),response=wire.path("response");
        BindingContract.require(request.isObject()&&response.isObject(),"Captured request/response body missing");
        BindingContract.require(request.path("jsonrpc").asText().equals("2.0")&&response.path("jsonrpc").asText().equals("2.0"),"Captured RPC envelope missing");
        BindingContract.require(request.hasNonNull("id")&&request.path("id").equals(response.path("id")),"Captured request/response RPC IDs differ");
        BindingContract.require(request.path("method").asText().equals("tools/call")&&request.path("params").path("name").asText().equals(cap),"Captured request capability differs");
        BindingContract.require(request.path("params").path("arguments").isObject(),"Captured API request arguments missing");
        JsonNode auth=wire.path("authenticatedActor");
        for(String key:List.of("issuer","subject","audience","organizationRef","authorizationGrantRef"))
            BindingContract.require(auth.hasNonNull(key)&&auth.path(key).equals(call.path(key)),"Captured server authentication differs: "+key);
        String pointer=Json.required(call,"businessResponsePointer");
        BindingContract.require(pointer.startsWith("/response/")&&!pointer.matches(".*~([^01]|$).*"),"Invalid captured business-response pointer");
        JsonNode business=wire.at(pointer);
        BindingContract.require(business.isObject(),"Captured business response missing");
        if(call.has("outcome")) BindingContract.require(call.path("outcome").equals(business.path("outcome")),"Call outcome differs from actual captured server response");
        return new Call(id,cap,Json.required(call,"wireArtifactRef"),pointer,request,business);
    }
    public static void checkCaptureContext(JsonNode wire,JsonNode scope,JsonNode actionId,JsonNode command) {
        BindingContract.require(wire.path("scope").equals(scope)&&wire.path("actionId").equals(actionId),"Captured API belongs to a different installation/turn/action");
        Instant sent=Instant.parse(Json.required(wire,"requestSentAt")),received=Instant.parse(Json.required(wire,"responseReceivedAt"));
        BindingContract.require(!received.isBefore(sent),"Captured response precedes request");
        if(command!=null)BindingContract.require(!sent.isBefore(Instant.parse(Json.required(command,"startedAt")))&&!received.isAfter(Instant.parse(Json.required(command,"completedAt"))),"Captured RPC falls outside actual client invocation");
    }
    /** Commands with APPLIED/replay claims need a real matching call, even when effects are zero. */
    public void requireExecutionCall(JsonNode intent,String publicCapability,String selectedPath) {
        if(selectedPath.equals("EVIDENCED_PREFLIGHT_STOP")||intent.path("status").asText().equals("NEEDS_INPUT")||intent.path("intentKind").asText().equals("QUERY")) return;
        String original=intent.path("slots").path("originalCapability").path("value").asText();
        BindingContract.require(calls.values().stream().anyMatch(c->c.capabilityId().equals(publicCapability)||!original.isBlank()&&c.capabilityId().equals(original)),"Executed command has no actual matching captured RPC");
    }
    public static JsonNode businessArguments(JsonNode request) {
        JsonNode arguments=request.path("params").path("arguments");
        if(!arguments.has("slots"))return arguments;
        BindingContract.require(arguments.path("slots").isObject(),"Captured typed slots are not an object");
        Set<String> metadata=Set.of("status","intentKind","capabilityId","definitionVersion","missingSlots","slots");arguments.fieldNames().forEachRemaining(k->BindingContract.require(metadata.contains(k),"Hidden business payload outside typed slots: "+k));
        var result=Json.object();arguments.path("slots").fields().forEachRemaining(e->{BindingContract.require(e.getValue().hasNonNull("value"),"Captured typed slot value missing");result.set(e.getKey(),e.getValue().path("value"));});return result;
    }
    public JsonNode commandResponse(String capability,String original){
        List<Call> matching=calls.values().stream().filter(c->c.capabilityId().equals(capability)||!original.isBlank()&&c.capabilityId().equals(original)).toList();
        BindingContract.require(!matching.isEmpty(),"No captured command response");return matching.get(matching.size()-1).response();
    }
    public JsonNode sources(){var result=Json.array();assertionSources.forEach((path,id)->{Call c=calls.get(id);var entry=Json.object();entry.put("semanticPath",path).put("capturedCallId",id).put("wireArtifactRef",c.wireArtifactRef()).put("businessResponsePointer",c.responsePointer());result.add(entry);});return result;}
    public void requireReplay(JsonNode canonicalPayload,JsonNode aliases,String capability) {
        JsonNode expected=BindingEvaluator.aliases(canonicalPayload,aliases);
        BindingContract.require(expected.isObject(),"Original committed replay payload absent");
        boolean found=false;
        for(Call c:calls.values()) if(c.capabilityId().equals(capability)) {
            JsonNode payload=businessArguments(c.request());
            BindingContract.require(payload.equals(expected),"Replay key/owner/full payload differs from committed payload");
            BindingContract.require(!c.request().path("id").equals(payload.path("commandIdempotencyKey")),"RPC ID is not the command idempotency key");
            if(c.response().path("reusedCommittedResult").asBoolean(false)) {
                BindingContract.require(c.response().path("outcome").asText().equals("APPLIED"),"Replay server response is not the committed result");
                found=true;
                if(assertionSources.containsKey("response.reusedCommittedResult")) BindingContract.require(assertionSources.get("response.reusedCommittedResult").equals(c.id()),"Replay assertion refers to a different call");
            }
        }
        BindingContract.require(found,"No actual same-key/full-payload replay RPC and committed response");
    }
    /** Receipt identity and effect references come from raw server bytes and independent ledgers. */
    public void requireReplayReferences(JsonNode before,JsonNode after,JsonNode payload,String capability) {
        Set<String> receiptIds=new LinkedHashSet<>(),physicalScopes=new LinkedHashSet<>(),effectIds=new LinkedHashSet<>();
        for(JsonNode row:before.path("data").path("rawRows"))if(row.path("dataset").asText().equals("receiptOccurrence")&&row.path("attribute").asText().equals("quantity")) {
            String id=Json.required(row,"rowId");receiptIds.add(id);physicalScopes.add(Json.required(row,"physicalScopeId"));
            for(String attribute:List.of("commandIdempotencyKey","stableRequestOwner")){int matches=0;for(JsonNode column:before.path("data").path("rawRows"))if(column.path("dataset").asText().equals("receiptOccurrence")&&column.path("rowId").asText().equals(id)&&column.path("attribute").asText().equals(attribute)&&column.path("value").equals(payload.path(attribute))){BindingContract.require(containsRow(after.path("data").path("rawRows"),column),"Replay changed original receipt key/owner column");matches++;}BindingContract.require(matches==1,"Committed occurrence lacks independently observed original key/owner");}
            BindingContract.require(containsRow(after.path("data").path("rawRows"),row),"Replay changed original receipt occurrence");
        }
        BindingContract.require(receiptIds.size()==1&&physicalScopes.size()==1,"Replay original canonical receipt identity is unobserved/ambiguous");
        for(JsonNode effect:before.path("data").path("data").path("effects"))if(effect.path("commandIdempotencyKey").equals(payload.path("commandIdempotencyKey"))&&effect.path("stableRequestOwner").equals(payload.path("stableRequestOwner"))&&BindingEvaluator.business(effect)) {effectIds.add(Json.required(effect,"id"));BindingContract.require(containsRow(after.path("data").path("data").path("effects"),effect),"Replay changed original committed effect");}
        BindingContract.require(!effectIds.isEmpty(),"Replay original scoped receipt/movement/outbox effect IDs unobserved");
        boolean matched=false;for(Call c:calls.values())if(c.capabilityId().equals(capability)&&c.response().path("reusedCommittedResult").asBoolean(false)) {
            BindingContract.require(receiptIds.contains(Json.required(c.response(),"committedReceiptOccurrenceRef")),"Replay server receipt reference differs from original ledger occurrence ID");
            BindingContract.require(physicalScopes.contains(Json.required(c.response(),"committedPhysicalScopeRef")),"Replay server physical-scope reference differs from original ledger");
            Set<String> refs=new LinkedHashSet<>();BindingContract.require(c.response().path("committedEffectRefs").isArray(),"Replay actual server effect references absent");for(JsonNode ref:c.response().path("committedEffectRefs"))BindingContract.require(ref.isTextual()&&!ref.asText().isBlank()&&refs.add(ref.asText()),"Duplicate/invalid replay effect reference");
            BindingContract.require(refs.equals(effectIds),"Replay server effect references differ from original committed effect IDs");matched=true;
        }
        BindingContract.require(matched,"Actual replay response with original committed references missing");
    }
    private static boolean containsRow(JsonNode rows,JsonNode expected){for(JsonNode row:rows)if(row.equals(expected))return true;return false;}

}
