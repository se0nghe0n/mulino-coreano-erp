package com.mulino.application.core;
import com.fasterxml.jackson.databind.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
/** Strict shared envelope. Business slot validation belongs to each typed handler. */
public final class CommandRequests {
  private static final Set<String> ALLOWED=Set.of("intentKind","definitionVersion","capabilityVersion","capabilityId","subjectRefs","slots","conditions","evidenceRefs","sourceRefs","contextRefs","provenance","conversationRequestId","proposalRevision","canonicalIntentHash","commandIdempotencyKey","expectedRevision","approvalId");
  private static final ObjectMapper JSON=new ObjectMapper();
  private CommandRequests(){}
  public static Map<String,Object> parse(Map<String,Object> input,boolean executing){
    if(!ALLOWED.containsAll(input.keySet()))throw DomainError.invalid("Unsupported command field");
    for(String key:List.of("intentKind","definitionVersion","capabilityId"))text(input,key,120);
    if(!Set.of("RECORD","COMMAND").contains(input.get("intentKind")))throw DomainError.invalid("Explicit write intent required");
    if(!(input.get("slots") instanceof Map<?,?>)||!(input.get("provenance") instanceof Map<?,?>)||!(input.get("subjectRefs") instanceof List<?> refs))throw DomainError.invalid("Typed slots, provenance and subjects required");
    for(Object ref:refs){if(!(ref instanceof Map<?,?> r)||!r.keySet().equals(Set.of("type","id"))||!(r.get("type") instanceof String)||!(r.get("id") instanceof String id))throw DomainError.invalid("Typed subject required");uuid(id);}
    ((Map<?,?>)input.get("provenance")).forEach((k,v)->{if(!(k instanceof String)||!Set.of("USER","CONTEXT","APPROVED_DEFAULT").contains(v))throw DomainError.invalid("Invalid provenance");});
    for(String field:List.of("expectedRevision","proposalRevision"))if(input.containsKey(field)&&(!(input.get(field) instanceof Number n)||n.longValue()<0||n.doubleValue()!=n.longValue()||n.longValue()>Integer.MAX_VALUE))throw DomainError.invalid("Integer revision required");
    if(input.containsKey("canonicalIntentHash")&&(!(input.get("canonicalIntentHash") instanceof String hash)||!hash.matches("[a-f0-9]{64}")))throw DomainError.invalid("Invalid canonical hash");
    for(String field:List.of("conditions","evidenceRefs","sourceRefs","contextRefs"))if(input.containsKey(field)&&!(input.get(field) instanceof List<?>))throw DomainError.invalid("Array required for "+field);
    if(input.containsKey("evidenceRefs"))for(Object value:(List<?>)input.get("evidenceRefs")){if(!(value instanceof String))throw DomainError.invalid("Typed evidence ID required");uuid((String)value);}
    for(String field:List.of("sourceRefs","contextRefs"))if(input.containsKey(field))for(Object value:(List<?>)input.get(field))if(!(value instanceof String))throw DomainError.invalid("String reference required");
    if(input.containsKey("approvalId")){text(input,"approvalId",36);uuid((String)input.get("approvalId"));}
    if(input.containsKey("conversationRequestId"))text(input,"conversationRequestId",160);
    if(input.containsKey("capabilityVersion"))text(input,"capabilityVersion",80);
    if(executing)text(input,"commandIdempotencyKey",160);
    return Map.copyOf(input);
  }
  public static void uuid(String id){try{UUID.fromString(id);}catch(Exception failure){throw DomainError.invalid("UUID required");}}
  private static void text(Map<String,Object> in,String key,int limit){if(!(in.get(key) instanceof String s)||s.isBlank()||s.length()>limit)throw DomainError.invalid("Invalid "+key);}
  public static String hash(Map<String,Object> intent){
    TreeMap<String,Object> canonical=new TreeMap<>(intent);
    for(String key:List.of("commandIdempotencyKey","conversationRequestId","canonicalIntentHash","proposalRevision","approvalId"))canonical.remove(key);
    try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(JSON.writeValueAsBytes(sort(canonical))));}catch(Exception e){throw DomainError.invalid("Noncanonical intent");}
  }
  private static Object sort(Object value){if(value instanceof Map<?,?> m){TreeMap<String,Object> out=new TreeMap<>();m.forEach((k,v)->{if(!(k instanceof String))throw DomainError.invalid("String keys required");out.put((String)k,sort(v));});return out;}if(value instanceof List<?> l)return l.stream().map(CommandRequests::sort).toList();return value;}
}
