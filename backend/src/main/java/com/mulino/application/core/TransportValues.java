package com.mulino.application.core;

import java.math.BigDecimal;
import java.time.temporal.TemporalAccessor;
import java.util.*;

/** Shared output normalization; quantities retain their companion unit field. */
public final class TransportValues {
  private TransportValues() {}
  public static Object normalize(Object value) {
    if (value instanceof BigDecimal n) return n.stripTrailingZeros().toPlainString();
    if (value instanceof Float || value instanceof Double) throw DomainError.invalid("Binary decimal transport rejected");
    if (value instanceof TemporalAccessor t) return t.toString();
    if (value instanceof Map<?,?> map) {
      Map<String,Object> result = new TreeMap<>();
      map.forEach((k,v)->result.put(String.valueOf(k),normalize(v)));
      return result;
    }
    if (value instanceof Collection<?> list) return list.stream().map(TransportValues::normalize).toList();
    return value;
  }
}
