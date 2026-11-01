package org.mulino.verification.nativeclient;

import static org.junit.jupiter.api.Assertions.*;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class NativeMcpTransportFixtureClockTest {
  @Test void frozenAuthorityAndPersistedRevocationUseTheSameMicrosecondBoundary() {
    Instant observedFailureClock=Instant.parse("2026-10-24T08:30:30.315385513Z");
    assertEquals(Instant.parse("2026-10-24T08:30:30.315385Z"),
        NativeMcpTransportActualTest.databaseFixtureInstant(observedFailureClock));
    for(Instant start:new Instant[]{observedFailureClock,
        Instant.parse("2026-10-24T08:30:30.999999000Z"),
        Instant.parse("1969-12-31T23:59:59.999999000Z")}) {
      for(int nanos=0;nanos<1000;nanos++) {
        Instant wall=start.plusNanos(nanos);
        Instant fixed=NativeMcpTransportActualTest.databaseFixtureInstant(wall);
        assertEquals(0,fixed.getNano()%1000);
        assertEquals(fixed,Timestamp.from(fixed).toInstant());
        assertEquals(fixed,NativeMcpTransportActualTest.databaseFixtureInstant(fixed));
        assertTrue(Duration.between(fixed,wall).toNanos()>=0);
        assertTrue(Duration.between(fixed,wall).toNanos()<1000);
      }
    }
  }
}
