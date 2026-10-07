package com.mulino.application.core;
import java.time.*;
import org.springframework.stereotype.Component;
/** Injectable server time. Public requests cannot change authority time. */
@Component
public class ExecutionClock {
  private final Clock clock;
  public ExecutionClock(){this(Clock.systemUTC());}
  public ExecutionClock(Clock clock){this.clock=clock;}
  public Instant instant(){return clock.instant();}
}
