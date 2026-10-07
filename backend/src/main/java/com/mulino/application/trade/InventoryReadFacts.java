package com.mulino.application.trade;

import com.mulino.application.core.DomainContext;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Snapshot read extension only. Providers never grant authority or mutate stock. */
public interface InventoryReadFacts {
  /** Owned fields; exactly one installed provider must own each metric. */
  Set<String> metrics();

  /**
   * All inputs use the caller's organization/asOf/knownAt snapshot. Segments are
   * already read-authorized active physical leaves. Historical receipts and
   * other referenced rows still require their own read authorization. Return
   * decimal quantities as strings; null is UNKNOWN, never zero by default.
   */
  Facts read(DomainContext context, String operation, String itemId,
      Map<String,Object> scope, List<Map<String,Object>> visibleSegments);

  record Facts(Map<String,Object> values, List<String> unknowns,
      List<String> conflicts, List<String> evidenceRefs) {
    public Facts {
      values = java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(values));
      unknowns = List.copyOf(unknowns);
      conflicts = List.copyOf(conflicts);
      evidenceRefs = List.copyOf(evidenceRefs);
    }
  }
}
