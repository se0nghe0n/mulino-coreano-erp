package com.mulino.domain.inventory;

import com.mulino.application.core.DomainError;
import java.math.BigDecimal;

/** Quantity conversion never rounds a physical amount into existence. */
public final class InventoryQuantity {
  private InventoryQuantity() {}
  public static BigDecimal parse(Object value, int decimalPlaces) {
    if (!(value instanceof String text) || !text.matches("[0-9]+(?:\\.[0-9]+)?")) throw DomainError.invalid("Quantity must be a decimal string");
    BigDecimal q = new BigDecimal(text);
    BigDecimal normalized = q.stripTrailingZeros();
    if (q.signum() <= 0 || decimalPlaces < 0 || decimalPlaces > 12 || q.scale() > decimalPlaces || q.scale() > 12 || Math.max(0, normalized.precision()-normalized.scale()) > 26) throw DomainError.invalid("Quantity exceeds unit precision or numeric range");
    return q;
  }
  public static BigDecimal convert(String value, BigDecimal factor, int decimalPlaces) {
    BigDecimal q = parse(value,12).multiply(factor);
    return parse(q.toPlainString(),decimalPlaces);
  }
  public static String text(Object value) { return ((BigDecimal)value).stripTrailingZeros().toPlainString(); }
}
