package com.mulino.application.core;
import java.util.Map;
/** Runtime checks the server claim owner/token/grant and parent transaction fence. */
public interface CommandLeasePort {
  void fenceAndVerify(DomainContext context,Map<String,Object> executionClaim);
}
