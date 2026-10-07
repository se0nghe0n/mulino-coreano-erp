package com.mulino.application.runtime;
import com.mulino.application.core.DomainContext;
import java.time.Instant;
import java.util.Map;
/** Responsibility owner validates immutable evidence/source and returns canonical linkage. */
public interface IntakeDutyPort {
 Map<String,Object> ensureEvidenceCorrectionDuty(DomainContext context,String workId,
   String evidenceId,String evidenceKind,String nextAction,Instant nextCheckAt);
}
