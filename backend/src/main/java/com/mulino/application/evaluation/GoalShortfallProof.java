package com.mulino.application.evaluation;

import com.mulino.application.core.DomainContext;
import java.math.BigDecimal;
import java.util.*;

/** Internal proof of an explicit current quantity duty; never an assessment or fact writer. */
public interface GoalShortfallProof {
  Map<String,List<String>> scopes(DomainContext context,String workId);
  Proof require(DomainContext context,String workId,Map<String,Object> committedCommand);
  /** Only the currently executing assessGoal transaction may use its just-saved immutable assessment. */
  default Proof requireInAssessment(DomainContext context,String workId,Map<String,Object> command){throw com.mulino.application.core.DomainError.unsupported();}
  record Proof(String workId,String goalId,String conditionId,BigDecimal target,BigDecimal recognized,
      BigDecimal shortfall,String unit,Map<String,Object> scope,Map<String,Object> provenance) {}
}
