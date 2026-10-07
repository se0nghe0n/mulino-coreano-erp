package com.mulino.application.core;
import java.time.*;
import org.springframework.stereotype.Component;
/** Injectable server time. Public requests cannot change authority time. */
@Component
public class ExecutionClock {
  private final Clock clock;
  @org.springframework.beans.factory.annotation.Autowired
  public ExecutionClock(org.springframework.beans.factory.ObjectProvider<Clock> clocks){this(clocks.getIfAvailable(Clock::systemUTC));}
  public ExecutionClock(){this(Clock.systemUTC());}
  public ExecutionClock(Clock clock){this.clock=clock;}
  public Instant instant(){return clock.instant();}
}
