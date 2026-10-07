package com.mulino.application.responsibility;
import com.mulino.application.core.DomainContext;
/** Domain owner validates actual action-specific resolution in the parent transaction. */
public interface ResponsibilityKindEvidence {
 String kind();
 void requireResolution(DomainContext context,String rootId,String scopeId,String evidenceId);
}
