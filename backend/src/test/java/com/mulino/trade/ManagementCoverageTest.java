package com.mulino.trade;

import com.mulino.application.trade.recall.ManagementCoverage;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 2026-10-09 user decision (AGENTS.md): authority that names several scope dimensions applies only when every named
 * dimension covers the target (all-of). One covered dimension is not authority over a target whose other named
 * dimension is not covered.
 */
class ManagementCoverageTest {
  private static final String ORG = "org", ACTOR = "manager", CAP = "decideRecall";
  private static Map<String, Object> row(String kind, String id) { return Map.of("actorId", ACTOR, "capabilityId", CAP, "scopeKind", kind, "scopeId", id); }
  private static boolean covers(List<Map<String, Object>> rows, Map<String, List<String>> target) { return ManagementCoverage.covers(rows, ORG, ACTOR, CAP, target, x -> true); }

  @Test void everyNamedDimensionMustCoverTheTarget() {
    var placeAndItem = List.of(row("PLACE", "C"), row("ITEM", "P"));
    assertFalse(covers(placeAndItem, Map.of("PLACE", List.of("C"), "ITEM", List.of("Q"))), "covered place does not authorize an uncovered item");
    assertTrue(covers(placeAndItem, Map.of("PLACE", List.of("C"), "ITEM", List.of("P"))));
    assertFalse(covers(placeAndItem, Map.of("PLACE", List.of("C"))), "a named dimension absent from the target is not covered");
  }

  @Test void everyValueOfADimensionMustBeCovered() {
    assertFalse(covers(List.of(row("PLACE", "C")), Map.of("PLACE", List.of("C", "W"))));
    assertTrue(covers(List.of(row("PLACE", "C"), row("PLACE", "W")), Map.of("PLACE", List.of("C", "W"))));
  }

  @Test void dimensionsTheAuthorityDoesNotNameStayUnconstrained() {
    assertTrue(covers(List.of(row("PLACE", "C")), Map.of("PLACE", List.of("C"), "ITEM", List.of("P"))));
  }

  @Test void organizationAuthorityCoversAndNoAuthorityDoesNot() {
    assertTrue(covers(List.of(row("ORGANIZATION", ORG)), Map.of("PLACE", List.of("C"))));
    assertFalse(covers(List.of(), Map.of("PLACE", List.of("C"))));
    assertFalse(covers(List.of(row("PLACE", "C")), Map.of()), "an empty target is never covered by a scoped authority");
  }

  @Test void otherActorsCapabilitiesAndInactiveRowsDoNotCount() {
    var other = new HashMap<String, Object>(row("PLACE", "C")); other.put("actorId", "someone");
    assertFalse(covers(List.of(other), Map.of("PLACE", List.of("C"))));
    assertFalse(ManagementCoverage.covers(List.of(row("PLACE", "C")), ORG, ACTOR, CAP, Map.of("PLACE", List.of("C")), x -> false));
  }
}
