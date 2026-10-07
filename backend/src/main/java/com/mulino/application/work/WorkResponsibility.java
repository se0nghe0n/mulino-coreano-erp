package com.mulino.application.work;

import com.mulino.application.core.DomainContext;
import java.util.Map;

/** Duty module owns roots/assignments; hooks execute in the caller transaction. */
public interface WorkResponsibility {
  void activated(DomainContext context,Map<String,Object> work,Map<String,Object> goal);
  void requireSettled(DomainContext context,String workId);
  void followup(DomainContext context,String originalWorkId,String followupWorkId,Map<String,Object> parameters);
}
