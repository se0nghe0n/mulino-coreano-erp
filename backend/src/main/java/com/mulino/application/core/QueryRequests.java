package com.mulino.application.core;

import java.time.Instant;
import java.util.*;

/** Typed HTTP/action/MCP read decoding. Aliases are resolved by clients, never server fixture IDs. */
public final class QueryRequests {
  private static final Set<String> FIELDS=Set.of("operation","id","scope","filters","limit","cursor","definitionVersion","asOf","knownAt","snapshotRef","workId","itemId","lotId","action","customerId","type","sort","name","status");
  private QueryRequests() {}
  public static QueryRequest parse(String operation,Map<String,Object> input) {
    if (!FIELDS.containsAll(input.keySet())) throw DomainError.invalid("Unsupported query field");
    if(input.containsKey("operation")&&!operation.equals(input.get("operation"))) throw DomainError.invalid("Operation mismatch");
    Map<String,Object> scope = object(input.get("scope"));
    Map<String,Object> filters = new LinkedHashMap<>(object(input.get("filters")));
    for(String name:List.of("workId","itemId","lotId","action","customerId","type","sort","name","status")) {
      if(input.containsKey(name)) filters.put(name,input.get(name));
    }
    String id = string(input.get("id"));
    if(id==null && (operation.equals("getWork")||operation.equals("getAssessment")||operation.equals("getObligations"))) id=string(input.get("workId"));
    int limit=0;
    if(input.containsKey("limit")) {
      Object n=input.get("limit");
      if(!(n instanceof Number)||!(n instanceof Integer||n instanceof Long)) throw DomainError.invalid("Integer page size required");
      long raw=((Number)n).longValue();
      if(raw<1||raw>200) throw DomainError.invalid("Invalid page size");
      limit=(int)raw;
    }
    return new QueryRequest(operation,id,scope,filters,limit,string(input.get("cursor")),string(input.get("definitionVersion")),instant(input.get("asOf")),instant(input.get("knownAt")),string(input.get("snapshotRef")));
  }
  private static Instant instant(Object value) {
    if(value==null) return null;
    try {return Instant.parse(string(value));} catch(RuntimeException failure){throw DomainError.invalid("UTC instant required");}
  }
  private static String string(Object value){if(value==null)return null;if(!(value instanceof String s)||s.isBlank())throw DomainError.invalid("String value required");return s;}
  @SuppressWarnings("unchecked") private static Map<String,Object> object(Object value){if(value==null)return Map.of();if(!(value instanceof Map<?,?> map)||map.keySet().stream().anyMatch(k->!(k instanceof String)))throw DomainError.invalid("Object value required");return (Map<String,Object>)map;}
}
