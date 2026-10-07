package com.mulino.application.core;
import java.util.Map;
/** Runtime checks the server claim owner/token/grant and parent transaction fence. */
public interface CommandLeasePort {
  /** Runtime resolves persisted actor/organization/continuity and verifies the server claim. */
  default DomainContext resolveContext(Map<String,Object> executionClaim,java.time.Instant now){throw DomainError.forbidden();}
  void fenceAndVerify(DomainContext context,Map<String,Object> executionClaim);
}
