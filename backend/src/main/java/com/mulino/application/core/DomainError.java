package com.mulino.application.core;

import java.util.Map;

/** Safe domain outcomes are preserved across all presentation adapters. */
public class DomainError extends RuntimeException {
  private final String outcome;
  private final String code;
  private final Map<String,Object> details;
  public DomainError(String outcome, String code, String message) {
    this(outcome,code,message,Map.of());
  }
  private DomainError(String outcome,String code,String message,Map<String,Object> details) {
    super(message); this.outcome=outcome; this.code=code; this.details=Map.copyOf(details);
  }
  public String outcome() { return outcome; }
  public String code() { return code; }
  public Map<String,Object> response() {
    var error=new java.util.LinkedHashMap<String,Object>(details);
    error.put("code",code);error.put("message",getMessage());
    return Map.of("outcome",outcome,"error",Map.copyOf(error),"effects",Map.of());
  }
  public static DomainError invalid(String message) { return new DomainError("REJECTED","TYPE_INVALID",message); }
  public static DomainError forbidden() { return new DomainError("REJECTED","FORBIDDEN","Unavailable scope"); }
  /**
   * Report only a recognized excluded business effect from a parsed structureIntent.
   * This is the requested category, never an inferred effect or an authorization result.
   * Arbitrary text, identifiers and nested values are deliberately not reflected.
   */
  public static DomainError unsupportedIntent(Map<String,Object> intent) {
    if ("structureIntent".equals(intent.get("capabilityId"))
        && intent.get("slots") instanceof Map<?,?> slots
        && slots.get("requestedEffect") instanceof String effect
        && java.util.Set.of("manufacture","BOM","B2C","generalLedger","taxSubmit","bankTransfer").contains(effect)) {
      return new DomainError("HELD","VERSION_UNSUPPORTED","Unsupported business effect",Map.of("requestedEffect",effect));
    }
    return unsupported();
  }
  public static DomainError unsupported() { return new DomainError("HELD","VERSION_UNSUPPORTED","Unsupported capability or definition"); }
}
