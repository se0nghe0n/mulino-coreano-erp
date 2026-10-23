package com.mulino.domain.evaluation;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Goal vocabulary aliases select source kinds; domain admission still proves the applied effect. */
public final class CanonicalEventSemantics {
  private CanonicalEventSemantics() {}

  public static boolean matches(Object goalKind, Object sourceKind) {
    // CONFIRMED_RECEIPT describes an admitted goal fact, never a source occurrence type.
    if ("CONFIRMED_RECEIPT".equals(sourceKind)) return false;
    return goalKind == null || Objects.equals(goalKind, sourceKind)
        || "CONFIRMED_RECEIPT".equals(goalKind) && "PHYSICAL_RECEIPT".equals(sourceKind);
  }
  public static boolean affects(Map<String,Object> goal, Object sourceKind) {
    if ("THROUGHOUT".equals(goal.get("quantityMode"))
        && "complete-initial-and-final-ledger".equals(goal.get("observationPolicy"))
        && Set.of("PHYSICAL", "PHYSICAL_HELD").contains(Objects.toString(goal.get("action")))
        && "STATE_OBSERVATION".equals(sourceKind)) return true;
    return matches(goal.get("eventKind"), sourceKind);
  }

}
