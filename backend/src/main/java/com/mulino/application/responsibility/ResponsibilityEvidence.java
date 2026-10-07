package com.mulino.application.responsibility;
import com.mulino.application.core.DomainContext;
/** Kind-specific business resolution/waiver authority; no document-presence shortcut. */
public interface ResponsibilityEvidence {
  void requireResolution(DomainContext context,String kind,String rootId,String scopeId,String evidenceId);
  void requireWaiver(DomainContext context,String kind,String rootId,String evidenceId,String reason);
}
