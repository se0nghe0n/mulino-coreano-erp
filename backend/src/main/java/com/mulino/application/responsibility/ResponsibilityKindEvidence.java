package com.mulino.application.responsibility;
import com.mulino.application.core.DomainContext;
/** Domain owner validates actual action-specific resolution in the parent transaction. */
public interface ResponsibilityKindEvidence {
 String kind();
 void requireResolution(DomainContext context,String rootId,String scopeId,String evidenceId);
 /** The residual a waiver of this root covers when it executes (plan §4.3 134행, §5.3); null when the kind records nothing. */
 default String waiverCoverage(DomainContext context,String rootId){return null;}
}
