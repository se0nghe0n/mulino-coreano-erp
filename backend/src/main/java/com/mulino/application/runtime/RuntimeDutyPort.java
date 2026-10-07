package com.mulino.application.runtime;

import com.mulino.application.core.DomainContext;
import java.time.Instant;
import java.util.Map;
/** Canonical responsibility module implements; all writes join caller transaction. */
public interface RuntimeDutyPort {
 Map<String,Object> ensureRuntimeDuty(DomainContext context,String workId,String sourceDecisionId,
     String kind,String nextAction,Instant nextCheckAt);
}
