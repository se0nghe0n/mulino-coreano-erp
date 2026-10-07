package com.mulino.application.core;

import java.math.BigDecimal;
import java.util.Map;

/** Decimal transport rejects rounding, exponents, and binary floating point. */
public record DecimalValue(String value, String unit) {
  public DecimalValue {
    if (value == null || !value.matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?")
        || unit == null || unit.isBlank()) throw DomainError.invalid("Decimal string and unit required");
    BigDecimal decimal = new BigDecimal(value);
    if (decimal.scale() > 12 || decimal.precision() > 38 || decimal.precision()-decimal.scale() > 26)
      throw DomainError.invalid("Decimal precision exceeds numeric(38,12)");
  }
  public static DecimalValue of(String value,String unit) { return new DecimalValue(value,unit); }
  public Map<String,Object> transport() { return Map.of("value",value,"unit",unit); }
}
