package com.mulino.application.core;
import java.util.*;
/** Exact shared wire envelope. Each registered handler validates its bounded typed slots. */
public final class CommandSchemas {
 private CommandSchemas(){}
 public static Map<String,Object> input(String operation){
   Map<String,Object> fields=new LinkedHashMap<>();
   fields.put("capabilityId",Map.of("type","string","const",operation));
   fields.put("intentKind",Map.of("type","string","enum",List.of("COMMAND","RECORD")));
   for(String key:List.of("definitionVersion","capabilityVersion","commandIdempotencyKey","conversationRequestId","canonicalIntentHash","approvalId"))fields.put(key,Map.of("type","string"));
   for(String key:List.of("expectedRevision","proposalRevision"))fields.put(key,Map.of("type","integer","minimum",0));
   fields.put("slots",Map.of("type","object"));fields.put("provenance",Map.of("type","object","additionalProperties",Map.of("enum",List.of("USER","CONTEXT","APPROVED_DEFAULT"))));
   fields.put("subjectRefs",Map.of("type","array","items",Map.of("type","object","required",List.of("type","id"),"properties",Map.of("type",Map.of("type","string"),"id",Map.of("type","string","format","uuid")),"additionalProperties",false)));
   for(String key:List.of("conditions","evidenceRefs","sourceRefs","contextRefs"))fields.put(key,Map.of("type","array"));
   return Map.of("type","object","required",List.of("intentKind","capabilityId","definitionVersion","commandIdempotencyKey","subjectRefs","slots","provenance"),"properties",fields,"additionalProperties",false);
 }
}
