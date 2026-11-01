package com.mulino.adapters.verification;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/** Fixture time uses PostgreSQL timestamp precision so boundary equality is stable. */
final class McpCaptureHttpFixtureClock {
  private McpCaptureHttpFixtureClock() {}

  static Instant now() {
    return atPostgresPrecision(Instant.now());
  }

  static Instant atPostgresPrecision(Instant instant) {
    return Objects.requireNonNull(instant, "instant").truncatedTo(ChronoUnit.MICROS);
  }
}
