package com.mulino.application.evidence;
import com.mulino.application.core.*;
import java.util.*;
import org.springframework.stereotype.Service;
import static com.mulino.application.evidence.EvidenceCommandInputs.*;
import static com.mulino.domain.evidence.EvidenceTypes.*;
@Service
public class EvidenceReconciliationCommands implements CommandHandler {
  private static final Set<String> REVIEW=Set.of("claimId","basisDocumentId","physicalScopeId","existingCanonicalId","policyVersion","sourceIdentity","quantity","unit","effectiveFrom","reason");
  private final EvidenceReconciliation service;
  public EvidenceReconciliationCommands(EvidenceReconciliation service){this.service=service;}
  public Set<String> capabilities(){return Set.of("matchSourceIdentity","linkCanonicalOccurrence","resolveEvidenceConflict");}
  public List<SubjectBinding> subjectBindings(DomainContext c,Map<String,Object> intent,CommandPreparation prep){String cap=intent.get("capabilityId").toString();var p=slots(intent,"linkCanonicalOccurrence".equals(cap)?Set.of("reconciliationId"):REVIEW);var claim="linkCanonicalOccurrence".equals(cap)?service.claim(c,service.review(c,required(p,"reconciliationId")).get("claimId").toString()):service.claim(c,required(p,"claimId"));return List.of(SubjectBinding.optional(EvidenceRecordCommands.noun(claim.get("subjectKind").toString()),Set.of(claim.get("subjectId").toString())));}
  public CommandPreparation prepare(DomainContext c,Map<String,Object> intent){
    String capability=intent.get("capabilityId").toString();var s=slots(intent,"linkCanonicalOccurrence".equals(capability)?Set.of("reconciliationId"):REVIEW);
    Map<String,Object> target="linkCanonicalOccurrence".equals(capability)?service.review(c,required(s,"reconciliationId")):service.claim(c,required(s,"claimId"));
    Map<String,Object> claim=target.get("claimId")!=null?service.claim(c,target.get("claimId").toString()):target;
    service.authorizeReviewer(c,capability,claim);var profile=service.profile(c,claim);
    Map<String,List<String>> dimensions=new LinkedHashMap<>();scopes(claim).forEach((k,v)->dimensions.put(k,List.copyOf(v)));
    if(!"linkCanonicalOccurrence".equals(capability))review(s);
    return new CommandPreparation(dimensions,List.of("evidence-claim:"+claim.get("ID"),"source:"+profile.get("ID")),new HashSet<>(List.of(profile.get("intakeOwnerId").toString(),profile.get("supervisorId").toString())),"RECONCILIATION",null,null,0,target.get("ID").toString(),((Number)target.get("revision")).intValue());
  }
  public Map<String,Object> execute(DomainContext c,Map<String,Object> intent){
    String capability=intent.get("capabilityId").toString();var s=slots(intent,"linkCanonicalOccurrence".equals(capability)?Set.of("reconciliationId"):REVIEW);
    if("linkCanonicalOccurrence".equals(capability))return EvidenceCommandOutcomes.applied(service.link(c,required(s,"reconciliationId")));
    // Conflict resolution records the authorized investigation result. It never selects a source by priority.
    return EvidenceCommandOutcomes.applied(service.match(c,review(s),capability));
  }
}
