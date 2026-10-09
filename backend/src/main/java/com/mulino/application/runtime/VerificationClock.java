package com.mulino.application.runtime;

import java.time.*;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Explicit isolated verification profile only. Authority uses this server clock. */
@Component
@Profile("verification")
public class VerificationClock extends Clock {
 private final AtomicReference<Instant> now;
 public VerificationClock(@Value("${mulino.verification.instant:}") String instant){
  if(instant.isBlank())throw new IllegalStateException("Verification profile requires explicit fixture instant");
  now=new AtomicReference<>(Instant.parse(instant));
 }
 @Override public ZoneId getZone(){return ZoneOffset.UTC;}
 @Override public Clock withZone(ZoneId zone){if(!ZoneOffset.UTC.equals(zone))throw new IllegalArgumentException("Verification clock is UTC");return this;}
 @Override public Instant instant(){return now.get();}
 public Instant advance(Instant next){return now.updateAndGet(previous->{if(next.isBefore(previous))throw new IllegalArgumentException("Clock cannot go backwards");return next;});}
 /**
  * Scenario isolation boundary only: a suite installs one fresh synthetic organization per subcase and starts that
  * subcase at its own fixture asOf, which may precede the previous subcase's last instant. Within a subcase the
  * clock still only moves forward through advance().
  */
 public Instant resetForFixture(Instant start){now.set(start);return start;}
}
