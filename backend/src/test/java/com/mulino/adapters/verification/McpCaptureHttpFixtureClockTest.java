package com.mulino.adapters.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class McpCaptureHttpFixtureClockTest {
  @Test
  void truncatesFixtureTimesToPostgreSqlMicroseconds() {
    Instant input = Instant.parse("2026-10-24T10:11:12.123456789Z");
    assertEquals(Instant.parse("2026-10-24T10:11:12.123456Z"),
        McpCaptureHttpFixtureClock.atPostgresPrecision(input));
    assertEquals(0, McpCaptureHttpFixtureClock.now().getNano() % 1_000);
  }
}
