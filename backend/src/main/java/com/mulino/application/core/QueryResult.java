package com.mulino.application.core;

import java.util.List;
import java.util.Map;

/** Handler data; application assigns common envelope and snapshot. */
public record QueryResult(Object data, Map<String,Object> scope, List<String> unknowns,
    List<String> conflicts, List<String> evidenceRefs, String nextCursor) {
  public QueryResult {
    scope = scope == null ? Map.of() : Map.copyOf(scope);
    unknowns = unknowns == null ? List.of() : List.copyOf(unknowns);
    conflicts = conflicts == null ? List.of() : List.copyOf(conflicts);
    evidenceRefs = evidenceRefs == null ? List.of() : List.copyOf(evidenceRefs);
  }
  public static QueryResult of(Object data, Map<String,Object> scope) {
    return new QueryResult(data, scope, List.of(), List.of(), List.of(), null);
  }
}
