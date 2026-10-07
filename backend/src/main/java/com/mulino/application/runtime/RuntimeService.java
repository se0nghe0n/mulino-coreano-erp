package com.mulino.application.runtime;

import com.mulino.application.core.*;
import com.mulino.application.identity.IdentityAuthorization;
import com.mulino.domain.runtime.RuntimeRepository;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.mulino.domain.runtime.RuntimeRepository.*;

/** Trusted worker primitives; never elevates the persisted original actor. */
@Service
public class RuntimeService {
 private final RuntimeRepository repository;private final ExecutionClock clock;private final IdentityAuthorization auth;private final RuntimeDutyPort duties;private final com.mulino.application.evidence.ExternalResultEvidenceGuard externalEvidence;
 private final org.springframework.beans.factory.ObjectProvider<ExternalDeliveryAuthorization> deliveryAuthorization;
 private final tools.jackson.databind.ObjectMapper json=new tools.jackson.databind.ObjectMapper();
 public RuntimeService(RuntimeRepository r,ExecutionClock c,IdentityAuthorization a,RuntimeDutyPort duties,com.mulino.application.evidence.ExternalResultEvidenceGuard externalEvidence,org.springframework.beans.factory.ObjectProvider<ExternalDeliveryAuthorization> deliveryAuthorization){repository=r;clock=c;auth=a;this.duties=duties;this.externalEvidence=externalEvidence;this.deliveryAuthorization=deliveryAuthorization;}
 @Transactional public Optional<Map<String,Object>> claim(DomainContext c,String work,String capability,String command,String worker,Duration ttl){auth.fence(c,List.of());auth.authorizeScope(c,capability,"WORK",work);return repository.claim(c,work,capability,command,worker,ttl);}
 @Transactional public void heartbeat(DomainContext c,Map<String,Object> claim,Duration ttl){repository.heartbeat(c,claim,ttl);}
 @Transactional public void finish(DomainContext c,Map<String,Object> claim,String status,String code){repository.finish(c,claim,status,code);}
 /** Locks a deliverable row, records capability support and intent before network IO. */
 @Transactional public Optional<Map<String,Object>> claimDelivery(DomainContext c,String worker,String operation,ExternalDeliveryAdapter.Support support,int maxAttempts,Duration ttl){
  if(maxAttempts<1||maxAttempts>20||ttl.isNegative()||ttl.isZero()||ttl.compareTo(Duration.ofMinutes(5))>0)throw DomainError.invalid("Invalid delivery policy");
  auth.fence(c,List.of());Instant now=clock.instant();
  var rows=repository.db().queryForList("SELECT * FROM mulino_runtime_Outbox WHERE organizationId=? AND actorId=? AND stableRequestOwner=? AND operation=? AND status='PENDING' AND nextCheckAt<=? ORDER BY nextCheckAt,ID FOR UPDATE SKIP LOCKED LIMIT 1",c.organizationId(),c.actorId(),c.stableRequestOwner(),operation,at(now));
  if(rows.isEmpty())return Optional.empty();var row=rows.getFirst();
  var payload=json.readValue((String)row.get("payloadjson"),Map.class);
  if(!(payload.get("workId") instanceof String workId))throw DomainError.invalid("External delivery requires canonical work scope");
  auth.authorizeScope(c,(String)row.get("operation"),"WORK",workId);
  int attempts=((Number)row.get("deliveryattempts")).intValue();
  if(attempts>=maxAttempts){repository.db().update("UPDATE mulino_runtime_Outbox SET status='EXHAUSTED',nextAction='Human must review exhausted delivery and retain responsibility' WHERE organizationId=? AND ID=?",c.organizationId(),row.get("id"));retainDuty(c,row,"Review exhausted external delivery");return Optional.empty();}
  long token=((Number)row.get("fencingtoken")).longValue()+1;
  repository.db().update("UPDATE mulino_runtime_Outbox SET status='IN_FLIGHT',deliveryAttempts=deliveryAttempts+1,fencingToken=?,leaseOwner=?,leaseExpiresAt=?,idempotencySupported=?,lookupSupported=?,nextAction='Reconcile external result; local cancellation does not undo it' WHERE organizationId=? AND ID=?",token,worker,at(now.plus(ttl)),support.idempotency(),support.authoritativeLookup(),c.organizationId(),row.get("id"));
  var result=new LinkedHashMap<String,Object>(row);result.put("fencingtoken",token);result.put("leaseowner",worker);return Optional.of(result);
 }
 @Transactional public void deliveryResult(DomainContext c,String id,long token,String worker,ExternalDeliveryAdapter.Result result,String evidence){
  if(evidence!=null&&!evidence.matches("[A-Za-z0-9._:/-]{1,160}"))throw DomainError.invalid("Invalid evidence reference");
  var rows=repository.db().queryForList("SELECT * FROM mulino_runtime_Outbox WHERE organizationId=? AND ID=? FOR UPDATE",c.organizationId(),id);if(rows.size()!=1)throw DomainError.forbidden();var row=rows.getFirst();
  if(!"IN_FLIGHT".equals(row.get("status"))||((Number)row.get("fencingtoken")).longValue()!=token||!Objects.equals(row.get("leaseowner"),worker)||!c.actorId().equals(row.get("actorid"))||!instant(row.get("leaseexpiresat")).isAfter(clock.instant()))throw conflict();
  if(result==ExternalDeliveryAdapter.Result.CONFIRMED_SUCCESS&&evidence!=null)externalEvidence.requireExternalResult(c,evidence,(String)row.get("externaloperationid"),"CONFIRMED_SUCCESS");
  String status=result==ExternalDeliveryAdapter.Result.CONFIRMED_SUCCESS&&evidence!=null?"SUCCEEDED":"UNKNOWN_EXTERNAL";
  // Even a synchronous failure needs explicit authoritative reconciliation before retry.
  repository.db().update("UPDATE mulino_runtime_Outbox SET status=?,resultEvidenceId=?,leaseOwner=NULL,leaseExpiresAt=NULL,nextCheckAt=?,nextAction=? WHERE organizationId=? AND ID=?",status,evidence,at(clock.instant().plusSeconds(1)),status.equals("SUCCEEDED")?"External success retained; link evidence to original work":"Authoritative lookup or human reconciliation required; do not redeliver",c.organizationId(),id);
  if(status.equals("UNKNOWN_EXTERNAL"))retainDuty(c,row,"Authoritative external reconciliation required");
 }
 @Transactional public void recordExternalReconciliation(DomainContext c,String id,ExternalDeliveryAdapter.Result result,String evidence){
  if(evidence==null||!evidence.matches("[A-Za-z0-9._:/-]{1,160}"))throw DomainError.invalid("Authoritative evidence reference required");
  auth.fence(c,List.of());
  var rows=repository.db().queryForList("SELECT * FROM mulino_runtime_Outbox WHERE organizationId=? AND ID=? FOR UPDATE",c.organizationId(),id);if(rows.size()!=1)throw DomainError.forbidden();var row=rows.getFirst();
  var scope=json.readValue((String)row.get("payloadjson"),Map.class);if(!(scope.get("workId") instanceof String work))throw DomainError.invalid("Canonical Work required");auth.authorizeScope(c,"recordExternalReconciliation","WORK",work);
  if(!Set.of("UNKNOWN_EXTERNAL","EXHAUSTED").contains(row.get("status")))throw conflict();
  String decision=result==ExternalDeliveryAdapter.Result.CONFIRMED_SUCCESS?"CONFIRMED_SUCCESS":result==ExternalDeliveryAdapter.Result.CONFIRMED_FAILURE?"CONFIRMED_FAILURE":"UNRESOLVED";
  if(result!=ExternalDeliveryAdapter.Result.UNKNOWN)externalEvidence.requireExternalResult(c,evidence,(String)row.get("externaloperationid"),decision);
  repository.db().update("INSERT INTO mulino_runtime_ExternalReconciliations VALUES(?,?,?,?,?,?,?)",c.organizationId(),UUID.randomUUID().toString(),id,decision,c.actorId(),evidence,at(clock.instant()));
  String status=result==ExternalDeliveryAdapter.Result.CONFIRMED_SUCCESS?"SUCCEEDED":result==ExternalDeliveryAdapter.Result.CONFIRMED_FAILURE?"PENDING":"UNKNOWN_EXTERNAL";
  repository.db().update("UPDATE mulino_runtime_Outbox SET status=?,resultEvidenceId=?,nextCheckAt=?,nextAction=? WHERE organizationId=? AND ID=?",status,evidence,at(clock.instant().plusSeconds(1)),status.equals("PENDING")?"Retry original externalOperationId after current authority check":"Retain original human duty and reconcile external result",c.organizationId(),id);
 }
 /** Crash after intent commit is ambiguous, including adapters with idempotency support. */
 @Transactional public int recoverExpiredDeliveries(){
  var rows=repository.db().queryForList("SELECT * FROM mulino_runtime_Outbox WHERE status='IN_FLIGHT' AND leaseExpiresAt<=? ORDER BY ID FOR UPDATE SKIP LOCKED",at(clock.instant()));
  for(var row:rows){
   repository.db().update("UPDATE mulino_runtime_Outbox SET status='UNKNOWN_EXTERNAL',leaseOwner=NULL,leaseExpiresAt=NULL,nextCheckAt=?,nextAction='Worker interrupted: authoritative lookup or human reconciliation; never automatic redelivery' WHERE organizationId=? AND ID=?",at(clock.instant()),row.get("organizationid"),row.get("id"));
   var c=new DomainContext((String)row.get("organizationid"),(String)row.get("actorid"),(String)row.get("stablerequestowner"),clock.instant(),clock.instant());retainDuty(c,row,"Worker interrupted: reconcile external result");
  }
  return rows.size();
 }
 private void retainDuty(DomainContext c,Map<String,Object> row,String action){var payload=json.readValue((String)row.get("payloadjson"),Map.class);if(!(payload.get("workId") instanceof String workId))throw DomainError.invalid("External delivery requires canonical work scope");duties.ensureRuntimeDuty(c,workId,(String)row.get("commandid"),"EXTERNAL_RECONCILIATION",action,clock.instant().plusSeconds(1));}


 @Transactional public void schedule(DomainContext c,String work,String kind,Instant nextCheck,Instant boundary,boolean pending,String owner,String supervisor,String action){
  if(nextCheck==null||owner==null||supervisor==null||action==null||action.isBlank())throw DomainError.invalid("Recovery duty requires owner, supervisor and next check");
  repository.db().update("INSERT INTO mulino_runtime_RecoverySchedules(organizationId,ID,workId,kind,nextCheckAt,nextValidityBoundary,pendingAssessment,ownerId,supervisorId,nextAction) VALUES(?,?,?,?,?,?,?,?,?,?) ON CONFLICT(organizationId,workId,kind) DO UPDATE SET recoveryStatus='READY',attemptCount=0,nextCheckAt=EXCLUDED.nextCheckAt,nextValidityBoundary=EXCLUDED.nextValidityBoundary,pendingAssessment=EXCLUDED.pendingAssessment,ownerId=EXCLUDED.ownerId,supervisorId=EXCLUDED.supervisorId,nextAction=EXCLUDED.nextAction,revision=mulino_runtime_RecoverySchedules.revision+1",c.organizationId(),UUID.randomUUID().toString(),work,kind,at(nextCheck),at(boundary),pending,owner,supervisor,action);
 }
 /** Discovery only: predicates/current grants must be rechecked by the original command. */
 @Transactional public List<Map<String,Object>> dueSchedules(int limit){
  if(limit<1||limit>1000)throw DomainError.invalid("Invalid sweep bound");
  return repository.db().queryForList("SELECT * FROM mulino_runtime_RecoverySchedules WHERE recoveryStatus='READY' AND (nextCheckAt<=? OR nextValidityBoundary<=? OR pendingAssessment=true) ORDER BY nextCheckAt,ID FOR UPDATE SKIP LOCKED LIMIT ?",at(clock.instant()),at(clock.instant()),limit);
 }
 @Transactional public void removeSchedule(DomainContext c,String work,String kind){repository.db().update("DELETE FROM mulino_runtime_RecoverySchedules WHERE organizationId=? AND workId=? AND kind=?",c.organizationId(),work,kind);}
 public Instant nextAuthorityBoundary(DomainContext c,String owner){
  var rows=repository.db().queryForList("SELECT min(boundary) AS boundary FROM (SELECT validUntil AS boundary FROM mulino_identity_Grants WHERE organizationId=? AND actorId IN (?,?) AND revokedAt IS NULL UNION ALL SELECT validUntil FROM mulino_identity_CapabilityAssignments WHERE organizationId=? AND actorId IN (?,?) AND revokedAt IS NULL UNION ALL SELECT validUntil FROM mulino_identity_Memberships WHERE organizationId=? AND actorId IN (?,?) AND revokedAt IS NULL) b WHERE boundary>?",c.organizationId(),c.actorId(),owner,c.organizationId(),c.actorId(),owner,c.organizationId(),c.actorId(),owner,at(clock.instant()));
  return rows.getFirst().get("boundary")==null?null:instant(rows.getFirst().get("boundary"));
 }
 @Transactional public void completedRecovery(Map<String,Object> schedule,String outcome){
  repository.db().update("UPDATE mulino_runtime_RecoverySchedules SET recoveryStatus='CHECKED',pendingAssessment=false,lastOutcome=?,lastCheckedAt=?,attemptCount=attemptCount+1 WHERE organizationId=? AND ID=?",outcome,at(clock.instant()),schedule.get("organizationid"),schedule.get("id"));
 }
 @Transactional public void manualRecovery(Map<String,Object> schedule,String outcome,String action,DomainContext c){
  repository.db().update("UPDATE mulino_runtime_RecoverySchedules SET recoveryStatus='HELD_MANUAL',pendingAssessment=false,nextValidityBoundary=NULL,nextAction=?,nextCheckAt=?,lastOutcome=?,lastCheckedAt=?,attemptCount=attemptCount+1 WHERE organizationId=? AND ID=?",action,at(clock.instant().plusSeconds(30)),outcome,at(clock.instant()),schedule.get("organizationid"),schedule.get("id"));
 }
 @Transactional public int recoverExpiredExecutions(){
  var rows=repository.db().queryForList("SELECT a.*,w.ownerId,w.supervisorId FROM mulino_runtime_ExecutionScopes s JOIN mulino_runtime_ExecutionAttempts a ON a.organizationId=s.organizationId AND a.ID=s.attemptId JOIN mulino_work_read_Works w ON w.organizationId=a.organizationId AND w.ID=a.workId WHERE a.status='ACTIVE' AND a.leaseExpiresAt<=? ORDER BY a.ID FOR UPDATE OF s,a SKIP LOCKED",at(clock.instant()));
  for(var row:rows){repository.db().update("UPDATE mulino_runtime_ExecutionAttempts SET status='EXPIRED',finishedAt=? WHERE organizationId=? AND ID=?",at(clock.instant()),row.get("organizationid"),row.get("id"));var c=new DomainContext((String)row.get("organizationid"),(String)row.get("actorid"),(String)row.get("stablerequestowner"),clock.instant(),clock.instant());schedule(c,(String)row.get("workid"),"WORK_RECHECK",clock.instant(),null,true,(String)row.get("ownerid"),(String)row.get("supervisorid"),"Recheck interrupted execution with current authority and original command key");}
  return rows.size();
 }
 @Transactional public int captureControlInvalidations(){
  var rows=repository.db().queryForList("SELECT DISTINCT w.organizationId,w.ID,w.ownerId,w.supervisorId,actor.stableRequestOwner FROM mulino_work_read_Works w JOIN mulino_identity_Actors actor ON actor.organizationId=w.organizationId AND actor.ID=w.ownerId WHERE w.status IN ('ACTIVE','WAITING') AND (EXISTS(SELECT 1 FROM mulino_identity_AuthorityInvalidations i WHERE i.organizationId=w.organizationId AND i.nextCheckAt<=? AND (i.actorId=w.ownerId OR EXISTS(SELECT 1 FROM mulino_runtime_ExecutionAttempts a WHERE a.organizationId=w.organizationId AND a.workId=w.ID AND a.actorId=i.actorId))) OR EXISTS(SELECT 1 FROM mulino_governance_PolicyBoundaries p WHERE p.organizationId=w.organizationId AND p.nextCheckAt<=?))",at(clock.instant()),at(clock.instant()));
  for(var row:rows){var c=new DomainContext((String)row.get("organizationid"),(String)row.get("ownerid"),(String)row.get("stablerequestowner"),clock.instant(),clock.instant());schedule(c,(String)row.get("id"),"WORK_RECHECK",clock.instant(),null,true,(String)row.get("ownerid"),(String)row.get("supervisorid"),"Recheck changed grant or current policy; require evidence before resume");}
  repository.db().update("DELETE FROM mulino_identity_AuthorityInvalidations WHERE nextCheckAt<=?",at(clock.instant()));repository.db().update("DELETE FROM mulino_governance_PolicyBoundaries WHERE nextCheckAt<=?",at(clock.instant()));return rows.size();
 }
 @Transactional public boolean linkRecoveryDuty(DomainContext c,String work,String action){
  var source=repository.db().queryForList("SELECT ID FROM mulino_commands_CommandRecords WHERE organizationId=? AND state='COMMITTED' AND resultJson::jsonb->'effects'->>'workId'=? ORDER BY completedAt DESC,ID LIMIT 1",c.organizationId(),work);
  if(source.isEmpty())return false;
  duties.ensureRuntimeDuty(c,work,(String)source.getFirst().get("id"),"RUNTIME_RECOVERY",action,clock.instant().plusSeconds(30));return true;
 }
 /** IN_FLIGHT was committed separately. Current authority locks cover actual bounded IO. */
 @Transactional public void deliverUnderFence(DomainContext c,String id,long token,String worker,ExternalDeliveryAdapter adapter){
  var rows=repository.db().queryForList("SELECT * FROM mulino_runtime_Outbox WHERE organizationId=? AND ID=?",c.organizationId(),id);if(rows.size()!=1)throw DomainError.forbidden();var row=rows.getFirst();
  if(!"IN_FLIGHT".equals(row.get("status"))||((Number)row.get("fencingtoken")).longValue()!=token||!worker.equals(row.get("leaseowner"))||!c.actorId().equals(row.get("actorid"))||!c.stableRequestOwner().equals(row.get("stablerequestowner"))||!instant(row.get("leaseexpiresat")).isAfter(clock.instant()))throw conflict();
  var payload=json.readValue((String)row.get("payloadjson"),Map.class);if(!(payload.get("workId") instanceof String work))throw DomainError.invalid("Canonical work required");
  var guard=deliveryAuthorization.getIfAvailable();if(guard==null)throw DomainError.unsupported();guard.require(c,(String)row.get("commandid"),(String)row.get("operation"),payload);
  auth.fence(c,List.of());auth.authorizeScope(c,(String)row.get("operation"),"WORK",work);
  var locked=repository.db().queryForMap("SELECT * FROM mulino_runtime_Outbox WHERE organizationId=? AND ID=? FOR UPDATE",c.organizationId(),id);
  if(!"IN_FLIGHT".equals(locked.get("status"))||((Number)locked.get("fencingtoken")).longValue()!=token||!worker.equals(locked.get("leaseowner"))||!instant(locked.get("leaseexpiresat")).isAfter(clock.instant()))throw conflict();
  ExternalDeliveryAdapter.Result result;try{result=adapter.deliver((String)row.get("operation"),(String)row.get("externaloperationid"),payload);}catch(RuntimeException timeout){result=ExternalDeliveryAdapter.Result.UNKNOWN;}
  // Adapter receipt without immutable verified evidence is held for canonical reconciliation.
  deliveryResult(c,id,token,worker,result,null);
 }
}
