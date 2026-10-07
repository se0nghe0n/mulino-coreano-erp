package com.mulino.application.core;

import java.time.Instant;
import java.util.Objects;

/** Trusted server identity and explicit effective/knowledge time. */
public record DomainContext(String organizationId, String actorId, String stableRequestOwner,
    Instant asOf, Instant knownAt) {
  public DomainContext {
    Objects.requireNonNull(organizationId);
    Objects.requireNonNull(actorId);
    Objects.requireNonNull(stableRequestOwner);
    Objects.requireNonNull(asOf);
    Objects.requireNonNull(knownAt);
  }
}
