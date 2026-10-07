package com.mulino.application.core;

import java.util.Map;

/** Safe domain outcomes are preserved across all presentation adapters. */
public class DomainError extends RuntimeException {
  private final String outcome;
  private final String code;
  public DomainError(String outcome, String code, String message) {
    super(message); this.outcome = outcome; this.code = code;
  }
  public String outcome() { return outcome; }
  public String code() { return code; }
  public Map<String,Object> response() {
    return Map.of("outcome",outcome,"error",Map.of("code",code,"message",getMessage()),"effects",Map.of());
  }
  public static DomainError invalid(String message) { return new DomainError("REJECTED","TYPE_INVALID",message); }
  public static DomainError forbidden() { return new DomainError("REJECTED","FORBIDDEN","Unavailable scope"); }
  public static DomainError unsupported() { return new DomainError("REJECTED","VERSION_UNSUPPORTED","Unsupported capability or definition"); }
}
