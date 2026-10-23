package com.mulino.application.core;

import java.util.Set;

/** Optional verification observer. No production bean and no business facts are supplied by this port. */
public interface ReassessmentDeferral {
  /** Called inside the real correcting command transaction after invalidation and retained duties. */
  void checkpoint(String organizationId,String capabilityId,String commandKey,String commandRecordId,Set<String> workIds);
  /** Gate only ordinary recovery execution of the exact affected Work, never manufacture an assessment. */
  boolean held(String organizationId,String workId);
}
