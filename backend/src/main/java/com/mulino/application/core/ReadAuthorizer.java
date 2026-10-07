package com.mulino.application.core;

import java.time.Instant;

/** Identity module maps verified issuer/subject to server identity and current grants. */
public interface ReadAuthorizer {
  DomainContext context(Instant asOf, Instant knownAt);
  void authorize(DomainContext context, String capabilityId, String targetId);
}
