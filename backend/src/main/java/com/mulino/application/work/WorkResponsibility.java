package com.mulino.application.work;

import com.mulino.application.core.DomainContext;
import java.util.Map;

/** Duty module owns roots/assignments; hooks execute in the caller transaction. */
public interface WorkResponsibility {
  void activated(DomainContext context,Map<String,Object> work,Map<String,Object> goal);
  void requireSettled(DomainContext context,String workId);
  void followup(DomainContext context,String originalWorkId,String followupWorkId,Map<String,Object> parameters);
  /** UD-2 (plan §5.2): opens the Work-sourced initial duty when the Work states nextAction/nextCheckAt; idempotent per Work. */
  default void initialDuty(DomainContext context,Map<String,Object> work){if(work.get("nextAction")!=null)throw com.mulino.application.core.DomainError.unsupported();}
  /** Closing the Work discharges its own still-open initial duty; other duties stay for requireSettled. */
  default void workClosed(DomainContext context,String workId,String reason){}
  default Map<String,Object> assessed(DomainContext context,String workId){return Map.of();}
  default void contributed(DomainContext context,String occurrenceId,String targetWorkId){}
}
