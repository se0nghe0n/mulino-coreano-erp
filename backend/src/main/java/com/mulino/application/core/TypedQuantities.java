package com.mulino.application.core;

import java.util.*;

/**
 * contracts/intent.schema.json types a quantity slot value as {value: decimal string, unit}. Handlers that read a
 * decimal string plus one shared {@code unit} slot see the same intent through this view: each typed quantity slot
 * becomes its decimal string and names the unit. A typed unit that differs from the unit slot, or two typed slots with
 * different units, are TYPE_INVALID; nothing is converted or rounded here (plan section 3.1). The canonical intent,
 * its hash and the stored request keep the authored form.
 */
public final class TypedQuantities {
  /** Slots whose value is a quantity in the handler's single {@code unit}. */
  public static final Set<String> QUANTITY_SLOTS = Set.of("quantity", "startQuantity", "observedQuantity");
  private TypedQuantities() {}
  public static Map<String, Object> expand(Map<String, Object> slots) {
    if (QUANTITY_SLOTS.stream().noneMatch(k -> typed(slots.get(k)))) return slots;
    var out = new LinkedHashMap<String, Object>(slots);
    String unit = slots.get("unit") instanceof String u ? u : null;
    if (slots.containsKey("unit") && unit == null) throw DomainError.invalid("Invalid unit");
    for (String key : QUANTITY_SLOTS) {
      if (!typed(slots.get(key))) continue;
      var q = (Map<?, ?>) slots.get(key);
      if (!(q.get("value") instanceof String value) || !(q.get("unit") instanceof String typedUnit) || typedUnit.isBlank())
        throw DomainError.invalid("Typed quantity requires decimal value and unit");
      if (unit != null && !unit.equals(typedUnit)) throw DomainError.invalid("Quantity unit differs from unit slot");
      unit = typedUnit;
      out.put(key, value);
    }
    out.put("unit", unit);
    return Collections.unmodifiableMap(out);
  }
  private static boolean typed(Object value) { return value instanceof Map<?, ?> m && m.keySet().equals(Set.of("value", "unit")); }
}
