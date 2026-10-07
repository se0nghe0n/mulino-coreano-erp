package com.mulino.application.runtime;

import com.mulino.application.core.*;
import com.mulino.application.identity.IdentityAuthorization;
import com.mulino.application.work.WorkAccess;
import com.mulino.domain.governance.PolicyRepository;
import com.mulino.domain.runtime.RuntimeRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import static com.mulino.domain.runtime.RuntimeRepository.*;

/** Link effect and receipt completion are one transaction; failures retain raw intake. */
@Service
public class RuntimeIntakeLinker {
 private final RuntimeRepository repository;private final PolicyRepository policies;private final IntakeDutyPort duties;private final WorkAccess works;private final IdentityAuthorization auth;private final CommandGuard guard;private final ExecutionClock clock;
 private final ObjectMapper json=new ObjectMapper();
 public RuntimeIntakeLinker(RuntimeRepository r,PolicyRepository p,IntakeDutyPort duties,WorkAccess works,IdentityAuthorization auth,CommandGuard guard,ExecutionClock clock){repository=r;policies=p;this.duties=duties;this.works=works;this.auth=auth;this.guard=guard;this.clock=clock;}
 public record Rule(Map<String,Object> policy,Map<String,Object> rule,Map<String,Object> event,Map<String,Object> profile){}
 public Rule rule(DomainContext c,Map<String,Object> index){
  var rows=policies.current(c.organizationId(),"EVIDENCE",clock.instant());if(rows.size()!=1)throw new DomainError("HELD","POLICY_UNRESOLVED","Current intake policy required");var policy=rows.getFirst();
  if(repository.db().queryForObject("SELECT count(*) FROM mulino_evidence_InboxRecords WHERE organizationId=? AND ID=? AND state='RECEIVED'",Integer.class,c.organizationId(),index.get("id"))!=1)throw new DomainError("HELD","SOURCE_CONFLICT","Conflicting source intake requires human reconciliation");
  var events=repository.db().queryForList("SELECT * FROM mulino_evidence_Events WHERE organizationId=? AND ID=?",c.organizationId(),index.get("eventid"));if(events.size()!=1)throw DomainError.forbidden();var event=events.getFirst();
  var profile=repository.db().queryForMap("SELECT * FROM mulino_evidence_SourceProfiles WHERE organizationId=? AND ID=?",c.organizationId(),index.get("sourceprofileid"));
  if(!Objects.equals(profile.get("policyversion"),policy.get("version")))throw new DomainError("HELD","POLICY_UNRESOLVED","Source intake policy version mismatch");
  var content=json.readValue(policy.get("content").toString(),Map.class);Object rule=content.get("intakeRules") instanceof Map<?,?> rules?rules.get(event.get("kind")):null;
  if(!(rule instanceof Map<?,?> r)||!(r.get("requiresResponse") instanceof Boolean))throw new DomainError("HELD","POLICY_UNRESOLVED","Typed intake rule required");
  return new Rule(policy,(Map<String,Object>)r,event,profile);
 }
 @Transactional public void noResponse(DomainContext c,Map<String,Object> index){
  policies.fence(c.organizationId());if(index.get("createdworkid")!=null)throw new DomainError("HELD","POLICY_UNRESOLVED","Existing response draft requires explicit authorized disposition");var proof=rule(c,index);if(!Boolean.FALSE.equals(proof.rule.get("requiresResponse")))throw conflict();
  repository.db().update("UPDATE mulino_runtime_IntakeRecoveries SET state='NO_RESPONSE',policyId=?,policyHash=?,decisionReason=?,lastCode='POLICY_NO_RESPONSE',revision=revision+1 WHERE organizationId=? AND ID=? AND state NOT IN ('LINKED','NO_RESPONSE')",proof.policy.get("ID"),proof.policy.get("contentHash"),"Current "+proof.policy.get("version")+" intake rule for "+proof.event.get("kind")+" requires no obligation",c.organizationId(),index.get("id"));
 }
 @Transactional public void link(DomainContext c,Map<String,Object> index,String workId){
  var prep=CommandPreparation.ordinary(Map.of("WORK",List.of(workId),"SOURCE",List.of(index.get("sourceprofileid").toString())),List.of("work:"+workId),"RESPONSIBILITY",workId,null);
  guard.fence(c,prep);var proof=rule(c,index);if(!Boolean.TRUE.equals(proof.rule.get("requiresResponse")))throw conflict();
  auth.authorizeScopes(c,"createObligation",prep.scopes());guard.verify(c,"createObligation",CommandRequests.hash(Map.of("intakeId",index.get("id"),"workId",workId)),prep,Map.of());
  var rows=repository.db().queryForList("SELECT * FROM mulino_runtime_IntakeRecoveries WHERE organizationId=? AND ID=? FOR UPDATE",c.organizationId(),index.get("id"));if(rows.size()!=1)throw DomainError.forbidden();if("LINKED".equals(rows.getFirst().get("state")))return;
  var source=works.require(c,workId,true);if("CLOSED".equals(source.get("status")))auth.authorizeScope(c,"createFollowup","WORK",workId);
  duties.ensureEvidenceCorrectionDuty(c,workId,index.get("eventid").toString(),"EVENT",proof.profile.get("nextaction").toString(),instant(proof.profile.get("nextcheckat")));
  var linked=repository.db().queryForList("SELECT a.ID,a.workId,w.ownerId,w.supervisorId FROM mulino_work_read_ObligationReferences a JOIN mulino_responsibility_Roots r ON r.organizationId=a.organizationId AND r.ID=a.rootId JOIN mulino_work_read_Works w ON w.organizationId=a.organizationId AND w.ID=a.workId WHERE a.organizationId=? AND r.sourceId=? AND r.sourceKind='EVENT' AND a.status='OPEN' AND a.valid AND w.status IN ('ACTIVE','WAITING')",c.organizationId(),index.get("eventid"));
  if(linked.size()!=1)throw DomainError.invalid("One actual active work and open response assignment required");var actual=linked.getFirst();
  for(String actor:List.of(actual.get("ownerid").toString(),actual.get("supervisorid").toString())){
   if(repository.db().queryForObject("SELECT count(*) FROM mulino_identity_Actors a JOIN mulino_identity_Memberships m ON m.organizationId=a.organizationId AND m.actorId=a.ID WHERE a.organizationId=? AND a.ID=? AND a.kind='HUMAN' AND m.validFrom<=? AND m.validUntil>? AND (m.revokedAt IS NULL OR m.revokedAt>?)",Integer.class,c.organizationId(),actor,at(clock.instant()),at(clock.instant()),at(clock.instant()))<1)throw DomainError.invalid("Current human responsibility required");
  }
  repository.db().update("UPDATE mulino_runtime_IntakeRecoveries SET state='LINKED',linkedWorkId=?,linkedAssignmentId=?,policyId=?,policyHash=?,decisionReason='Policy-required response has actual human work and OPEN assignment',lastCode='LINKED',revision=revision+1 WHERE organizationId=? AND ID=?",actual.get("workid"),actual.get("id"),proof.policy.get("ID"),proof.policy.get("contentHash"),c.organizationId(),index.get("id"));
 }
}
