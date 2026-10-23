package com.mulino.domain.trade.purchase;

import com.mulino.application.core.CommandRequests;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Immutable commercial terms and the exact governing goal identity share one approval hash. */
public final class PurchaseHash {
  private PurchaseHash() {}

  public static String of(Map<String,Object> revision) {
    var input = new TreeMap<String,Object>();
    for (String key : List.of("proposalId", "revision", "workId", "goalVersionId", "itemId",
        "supplierId", "destinationId", "quantity", "unit", "price", "currency", "dueAt",
        "endpoint", "quantityMode")) {
      String value = revision.get(key).toString();
      if (key.equals("quantity") || key.equals("price")) {
        value = new BigDecimal(value).stripTrailingZeros().toPlainString();
      } else if (key.equals("dueAt")) {
        value = Instant.parse(value).toString();
      }
      input.put(key, value);
    }
    return CommandRequests.hash(input);
  }
}
