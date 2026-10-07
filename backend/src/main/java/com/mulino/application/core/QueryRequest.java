package com.mulino.application.core;

import java.time.Instant;
import java.util.Map;

/** Public read contract. Scope contains typed IDs, never SQL expressions. */
public record QueryRequest(String operation, String id, Map<String,Object> scope,
    Map<String,Object> filters, int limit, String cursor, String definitionVersion,
    Instant asOf, Instant knownAt, String snapshotRef) {
  public QueryRequest {
    scope = scope == null ? Map.of() : Map.copyOf(scope);
    filters = filters == null ? Map.of() : Map.copyOf(filters);
    if (limit == 0) limit = 50;
    if (limit < 1 || limit > 200) throw DomainError.invalid("Invalid page size");
  }
}
