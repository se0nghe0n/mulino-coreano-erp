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
  /**
   * The same request identity whose knowledge time also covers rows this command transaction has just written or
   * re-read under its lock (plan §4.2 lock-then-reread). Never moves knowledge time backwards, and never moves asOf:
   * effective-time reads are unchanged, so a historical read is not widened.
   */
  public DomainContext knownThrough(Instant at) {
    return at==null||!at.isAfter(knownAt)?this:new DomainContext(organizationId,actorId,stableRequestOwner,asOf,at);
  }
}
