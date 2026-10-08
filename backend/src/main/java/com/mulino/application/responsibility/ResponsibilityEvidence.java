package com.mulino.application.responsibility;
import com.mulino.application.core.DomainContext;
/** Kind-specific business resolution/waiver authority; no document-presence shortcut. */
public interface ResponsibilityEvidence {
  void requireResolution(DomainContext context,String kind,String rootId,String scopeId,String evidenceId);
  void requireWaiver(DomainContext context,String kind,String rootId,String assignmentId,int assignmentRevision,String approvalId,String reason);
  /** What the waiver covers at its execution, recorded in the waived assignment basis; null when the kind records nothing. */
  default String waiverCoverage(DomainContext context,String kind,String rootId){return null;}
}
