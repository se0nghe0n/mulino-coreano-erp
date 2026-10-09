package com.mulino.commands;

import com.mulino.application.core.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** intent.schema.json {value,unit} quantities reach string+unit handlers unchanged in meaning. */
class TypedQuantitiesTest {
  @Test void typedQuantityBecomesDecimalAndUnit() {
    var out = TypedQuantities.expand(Map.of("quantity", Map.of("value", "60", "unit", "BOX"), "segmentId", "s"));
    assertEquals("60", out.get("quantity"));
    assertEquals("BOX", out.get("unit"));
    assertEquals("s", out.get("segmentId"));
  }
  @Test void untypedSlotsAreReturnedUnchanged() {
    var slots = Map.<String, Object>of("quantity", "60", "unit", "BOX");
    assertSame(slots, TypedQuantities.expand(slots));
  }
  @Test void matchingUnitSlotIsKeptAndMismatchRejects() {
    assertEquals("BOX", TypedQuantities.expand(Map.of("quantity", Map.of("value", "1", "unit", "BOX"), "unit", "BOX")).get("unit"));
    var error = assertThrows(DomainError.class, () -> TypedQuantities.expand(Map.of("quantity", Map.of("value", "1", "unit", "BOX"), "unit", "EA")));
    assertEquals("TYPE_INVALID", error.code());
    assertThrows(DomainError.class, () -> TypedQuantities.expand(Map.of("quantity", Map.of("value", "1", "unit", "BOX"), "startQuantity", Map.of("value", "0", "unit", "EA"))));
  }
  @Test void typedQuantityNeedsDecimalTextAndUnitText() {
    assertThrows(DomainError.class, () -> TypedQuantities.expand(Map.of("quantity", Map.of("value", 60, "unit", "BOX"))));
    assertThrows(DomainError.class, () -> TypedQuantities.expand(Map.of("quantity", Map.of("value", "60", "unit", ""))));
  }
}
