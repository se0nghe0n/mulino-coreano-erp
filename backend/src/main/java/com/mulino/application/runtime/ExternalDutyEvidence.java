package com.mulino.application.runtime;

import com.mulino.application.core.*;
import com.mulino.application.evidence.ExternalResultEvidenceGuard;
import com.mulino.application.responsibility.ResponsibilityKindEvidence;
import com.mulino.domain.runtime.RuntimeRepository;
import java.util.*;
import org.springframework.stereotype.Component;

/** All external effects under the canonical decision must have verified success. */
@Component
public class ExternalDutyEvidence implements ResponsibilityKindEvidence {
 private final RuntimeRepository repository;private final ExternalResultEvidenceGuard evidence;
 public ExternalDutyEvidence(RuntimeRepository repository,ExternalResultEvidenceGuard evidence){this.repository=repository;this.evidence=evidence;}
 @Override public String kind(){return "EXTERNAL_RECONCILIATION";}
 @Override public void requireResolution(DomainContext c,String rootId,String scopeId,String document){
  RuntimeRepository.transaction();
  var roots=repository.db().queryForList("SELECT r.sourceId FROM mulino_responsibility_Roots r JOIN mulino_responsibility_Scopes s ON s.organizationId=r.organizationId AND s.rootId=r.ID WHERE r.organizationId=? AND r.ID=? AND s.ID=? AND r.kind='EXTERNAL_RECONCILIATION'",c.organizationId(),rootId,scopeId);
  if(roots.size()!=1)throw DomainError.forbidden();
  var rows=repository.db().queryForList("SELECT * FROM mulino_runtime_Outbox WHERE organizationId=? AND commandId=? ORDER BY ID FOR UPDATE",c.organizationId(),roots.getFirst().get("sourceid"));
  if(rows.isEmpty()||rows.stream().anyMatch(r->!"SUCCEEDED".equals(r.get("status"))||r.get("resultevidenceid")==null)||rows.stream().noneMatch(r->document.equals(r.get("resultevidenceid"))))throw DomainError.invalid("External effects are not authoritatively reconciled");
  for(var row:rows)evidence.requireExternalResult(c,(String)row.get("resultevidenceid"),(String)row.get("externaloperationid"),"CONFIRMED_SUCCESS");
 }
}
