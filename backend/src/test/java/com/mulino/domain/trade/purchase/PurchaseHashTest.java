package com.mulino.domain.trade.purchase;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class PurchaseHashTest {
  Map<String,Object> revision() {
    return new LinkedHashMap<>(Map.ofEntries(Map.entry("proposalId", "proposal"), Map.entry("revision", 1),
        Map.entry("workId", "work"), Map.entry("goalVersionId", "immutable-goal"), Map.entry("itemId", "item"),
        Map.entry("supplierId", "supplier"), Map.entry("destinationId", "destination"), Map.entry("quantity", new BigDecimal("100")),
        Map.entry("unit", "BOX"), Map.entry("price", new BigDecimal("1.00")), Map.entry("currency", "EUR"),
        Map.entry("dueAt", Instant.parse("2026-10-31T00:00:00Z")), Map.entry("endpoint", "ARRIVED"), Map.entry("quantityMode", "CUMULATIVE_EVENT")));
  }
  @Test void databaseDecimalScaleAndInstantTransportRetainHash() {
    var input = revision(); var saved = revision();
    saved.put("quantity", new BigDecimal("100.000000000000")); saved.put("price", new BigDecimal("1.000000000000"));
    saved.put("dueAt", "2026-10-31T00:00:00.000000000Z");
    assertEquals(PurchaseHash.of(input), PurchaseHash.of(saved));
  }
  @Test void everyApprovedSemanticOperandIncludingGoalIdentityChangesHash() {
    var original = revision(); String hash = PurchaseHash.of(original);
    for (String key : original.keySet()) {
      var changed = new LinkedHashMap<>(original);
      changed.put(key, switch(key) {
        case "quantity", "price" -> new BigDecimal("123.25");
        case "dueAt" -> Instant.parse("2026-11-01T00:00:00Z");
        case "revision" -> 2;
        default -> "changed";
      });
      assertNotEquals(hash, PurchaseHash.of(changed), key);
    }
  }
}
