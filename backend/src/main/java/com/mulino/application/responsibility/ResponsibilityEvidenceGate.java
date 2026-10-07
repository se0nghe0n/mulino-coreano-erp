package com.mulino.application.responsibility;
import com.mulino.application.core.*;
import org.springframework.stereotype.Component;
/** Fail closed until a verified kind-specific resolver is installed. */
@Component
public class ResponsibilityEvidenceGate implements ResponsibilityEvidence {
 public void requireResolution(DomainContext c,String kind,String root,String scope,String evidence){throw DomainError.unsupported();}
 public void requireWaiver(DomainContext c,String kind,String root,String evidence,String reason){throw DomainError.unsupported();}
}
