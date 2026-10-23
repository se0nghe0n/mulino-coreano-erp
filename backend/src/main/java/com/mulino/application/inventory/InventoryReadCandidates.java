package com.mulino.application.inventory;

import com.mulino.application.core.*;
import java.time.Instant;
import java.util.*;

/** Shared physical candidate predicates. Does not infer warehouse custody or rewrite history. */
public final class InventoryReadCandidates {
  private InventoryReadCandidates() {}

  public static boolean active(Map<String,Object> row, DomainContext c) {
    if (time(row.get("validFrom")).isAfter(c.asOf())) return false;
    return row.get("retiredAt")==null || time(row.get("retiredAt")).isAfter(c.asOf())
        || (row.get("retirementRecordedAt")!=null && time(row.get("retirementRecordedAt")).isAfter(c.knownAt()));
  }

  public static boolean matches(Map<String,Object> row, Map<String,Object> scope) {
    return (!scope.containsKey("placeId") || Objects.equals(scope.get("placeId"),row.get("placeId")))
        && (!scope.containsKey("lotId") || Objects.equals(scope.get("lotId"),row.get("lotId")));
  }

  public static Map<String,Collection<String>> segmentScopes(Map<String,Object> row, String workId) {
    Map<String,Collection<String>> scopes=new LinkedHashMap<>();
    scopes.put("TARGET",List.of((String)row.get("ID")));
    scopes.put("ITEM",List.of((String)row.get("itemId")));
    scopes.put("PLACE",List.of((String)row.get("placeId")));
    if(workId!=null) scopes.put("WORK",List.of(workId));
    return scopes;
  }

  private static Instant time(Object value) {
    return value instanceof Instant i ? i : value instanceof java.sql.Timestamp t ? t.toInstant() : Instant.parse(value.toString());
  }
}
