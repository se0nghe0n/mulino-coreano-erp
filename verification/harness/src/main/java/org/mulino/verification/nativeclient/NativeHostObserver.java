package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.mulino.verification.Json;
import java.time.Instant;
import java.util.*;

/** Reads the app-server wire and OS lifecycle capture, not declared config/resolved summary fields. */
final class NativeHostObserver {
    static ObjectNode extract(JsonNode capture) {
        var out=Json.object().put("status","UNKNOWN").put("complete",false);var calls=Json.array();out.set("toolCalls",calls);
        try {
            JsonNode events=capture.path("events");require(events.isArray()&&!events.isEmpty(),"RAW_EVENTS_MISSING");
            // Notifications may precede the turn/start reply; the native transport buffers them.
            // Bind only replies present in this same complete capture, then validate event order below.
            Map<String,JsonNode> replies=new HashMap<>();
            for(JsonNode event:events) {
                JsonNode message=event.path("message");
                if(event.path("direction").asText().equals("received")&&!message.has("method")&&message.hasNonNull("id"))
                    require(replies.putIfAbsent(message.path("id").toString(),message)==null,"DUPLICATE_RPC_REPLY");
            }
            Instant began=Instant.parse(Json.required(capture,"startedAt")),end=Instant.parse(Json.required(capture,"finishedAt"));
            Instant previous=began;JsonNode start=null,turn=null,last=null,lifecycle=null;String startId=null,turnId=null,thread=null,model=null,tier=null;
            Map<String,JsonNode> pending=new HashMap<>();Set<String> callIds=new HashSet<>();boolean terminal=false;
            for(int i=0;i<events.size();i++) {
                JsonNode event=events.get(i),message=event.path("message"),params=message.path("params");
                Instant at=Instant.parse(Json.required(event,"at"));require(!at.isBefore(previous)&&!at.isAfter(end),"EVENT_TIME_OUTSIDE_CUSTODY");previous=at;
                require(!event.path("redacted").asBoolean()&&message.isObject(),"RAW_EVENT_REDACTED_OR_MISSING");
                String direction=Json.required(event,"direction"),method=message.path("method").asText();
                require(Set.of("sent","received","lifecycle").contains(direction),"EVENT_DIRECTION_UNKNOWN");
                if(direction.equals("received")&&!method.isEmpty()&&message.hasNonNull("id"))
                    require(method.equals("item/tool/call"),"UNEXPECTED_CLIENT_REQUEST");
                if(direction.equals("lifecycle")){require(terminal&&lifecycle==null,"DUPLICATE_PROCESS_TERMINATION");lifecycle=message;continue;}
                if(direction.equals("sent")&&method.equals("thread/start")){require(message.hasNonNull("id"),"THREAD_REQUEST_ID_MISSING");require(startId==null,"DUPLICATE_THREAD_START");startId=message.path("id").toString();}
                else if(direction.equals("received")&&startId!=null&&!message.has("method")&&message.path("id").toString().equals(startId)) {
                    start=message.path("result");thread=Json.required(start.path("thread"),"id");model=Json.required(start,"model");tier=Json.required(start,"serviceTier");
                } else if(direction.equals("sent")&&method.equals("turn/start")) {
                    require(message.hasNonNull("id"),"TURN_REQUEST_ID_MISSING");require(start!=null&&params.path("threadId").asText().equals(thread)&&turnId==null,"TURN_START_SCOPE");turnId=message.path("id").toString();
                    JsonNode reply=replies.get(turnId);require(reply!=null&&!reply.has("error"),"TURN_REPLY_MISSING");
                    turn=reply.path("result").path("turn");require(turn.hasNonNull("id"),"TURN_REPLY_ID_MISSING");
                } else if(direction.equals("received")&&turnId!=null&&!message.has("method")&&message.path("id").toString().equals(turnId)) {turn=message.path("result").path("turn");}
                else if(direction.equals("received")&&method.equals("item/tool/call")) {
                    require(turn!=null&&!terminal&&params.path("threadId").asText().equals(thread)&&params.path("turnId").equals(turn.path("id")),"TOOL_EVENT_SCOPE");
                    require(message.hasNonNull("id")&&(!params.hasNonNull("namespace")||params.path("namespace").asText().isEmpty()),"UNEXPECTED_TOOL_NAMESPACE");
                    require(callIds.add(Json.required(params,"callId")),"DUPLICATE_TOOL_CALL");String id=message.path("id").toString();require(pending.putIfAbsent(id,params)==null,"DUPLICATE_RPC_ID");
                } else if(direction.equals("sent")&&pending.containsKey(message.path("id").toString())) {
                    JsonNode request=pending.remove(message.path("id").toString());var call=Json.object();call.set("request",request);call.set("response",message.path("result"));call.put("eventIndex",i);calls.add(call);
                } else if(direction.equals("received")&&method.equals("item/completed")&&params.path("item").path("type").asText().equals("agentMessage")&&params.path("item").path("phase").asText().equals("final_answer")) {
                    require(!terminal&&params.path("threadId").asText().equals(thread)&&turn!=null&&params.path("turnId").equals(turn.path("id")),"FINAL_EVENT_SCOPE");
                    require(params.path("item").path("text").isTextual(),"FINAL_EVENT_TEXT_MISSING");last=params.path("item").path("text");
                } else if(direction.equals("received")&&method.equals("turn/completed")) {
                    require(!terminal&&last!=null&&turn!=null&&params.path("threadId").asText().equals(thread)&&params.path("turn").path("id").equals(turn.path("id"))&&params.path("turn").path("status").asText().equals("completed"),"TERMINAL_EVENT_SCOPE");terminal=true;
                } else if(direction.equals("received")&&(method.equals("item/started")||method.equals("item/completed"))) {
                    require(!terminal&&turn!=null&&params.path("threadId").asText().equals(thread)&&params.path("turnId").equals(turn.path("id")),"ITEM_EVENT_SCOPE");
                    require(Set.of("agentMessage","userMessage","reasoning","dynamicToolCall","plan").contains(params.path("item").path("type").asText()),"UNEXPECTED_BUILTIN_TOOL");
                } else if(direction.equals("received")&&method.equals("thread/settings/updated")) {
                    require(start!=null&&(!params.hasNonNull("model")||params.path("model").asText().equals(model))&&(!params.hasNonNull("serviceTier")||params.path("serviceTier").asText().equals(tier)),"RUNTIME_SETTINGS_CHANGED");
                } else if(message.hasNonNull("id")) {
                    throw new IllegalStateException("UNMATCHED_RPC_EVENT");
                }
            }
            require(start!=null&&turn!=null&&terminal&&pending.isEmpty(),"INCOMPLETE_RAW_TURN");
            require(last.equals(capture.path("result").path("finalResponse")),"FINAL_RESPONSE_DIFFERS_FROM_RAW_EVENT");
            require(lifecycle!=null&&lifecycle.path("processId").equals(capture.path("processId"))&&capture.path("processId").asLong()>0
                &&lifecycle.path("termination").asText().equals("HOST_CLOSED_DEDICATED_PROCESS")&&lifecycle.path("exitCode").isIntegralNumber(),"PROCESS_CUSTODY_INCOMPLETE");
            require(capture.path("command").isArray()&&!capture.path("command").isEmpty(),"PROCESS_COMMAND_UNOBSERVED");
            out.put("status","EXTRACTED").put("complete",true).put("model",model).put("serviceTier",tier).put("threadId",thread);
            out.set("turnId",turn.path("id"));out.set("processId",lifecycle.path("processId"));out.set("exitCode",lifecycle.path("exitCode"));out.set("sourceResponse",last);
        } catch(IllegalArgumentException|IllegalStateException error) {out.put("reason",error.getMessage()!=null&&error.getMessage().matches("[A-Z_]+")?error.getMessage():"INVALID_CUSTODY");}
        return out;
    }
    private static void require(boolean b,String code){NativeClientPolicy.require(b,code);}
    private NativeHostObserver() {}
}
