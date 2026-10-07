package com.mulino.application.work;

import com.mulino.application.core.DomainContext;
import java.util.Map;

/** Evaluator owns current input revision/policy checks and immutable assessments. */
public interface WorkAssessmentGuard {
  default void requireSupportedGoal(DomainContext context,Map<String,Object> goal){throw com.mulino.application.core.DomainError.unsupported();}
  void requireFulfilled(DomainContext context,Map<String,Object> work,Map<String,Object> goal);
  void requireResume(DomainContext context,Map<String,Object> work,Map<String,Object> wait,Map<String,Object> evidence);
  void goalChanged(DomainContext context,String workId,String previousGoalId,String newGoalId);
}
