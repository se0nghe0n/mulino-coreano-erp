package com.mulino.application.work;

import com.mulino.application.core.DomainContext;
import java.util.Map;

/** Shared transaction port. Canonical Work is mulino.work.read.Works. */
public interface WorkAccess {
  Map<String,Object> require(DomainContext context, String workId, boolean lock);
  Map<String,Object> currentGoal(DomainContext context, String workId);
  void replaceOwner(DomainContext context,String workId,String previousOwner,String newOwner);
  default Map<String,Object> ensureFollowup(DomainContext context,String originalWorkId,String sourceId,String reason,String nextAction,java.time.Instant nextCheckAt){throw com.mulino.application.core.DomainError.unsupported();}
  void markInvalidation(DomainContext context,String workId,boolean pending);
}
