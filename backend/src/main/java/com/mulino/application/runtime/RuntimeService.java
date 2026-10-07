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
 private final RuntimeRepository repository;private final ExecutionClock clock;private final IdentityAuthorization auth;
 public RuntimeService(RuntimeRepository r,ExecutionClock c,IdentityAuthorization a){repository=r;clock=c;auth=a;}
 @Transactional public Optional<Map<String,Object>> claim(DomainContext c,String work,String capability,String command,String worker,Duration ttl){auth.fence(c,List.of());auth.authorizeScope(c,capability,"WORK",work);return repository.claim(c,work,capability,command,worker,ttl);}
 @Transactional public void heartbeat(DomainContext c,Map<String,Object> claim,Duration ttl){repository.heartbeat(c,claim,ttl);}
 @Transactional public void finish(DomainContext c,Map<String,Object> claim,String status,String code){repository.finish(c,claim,status,code);}
 /** Locks a deliverable row, records capability support and intent before network IO. */
 @Transactional public Optional<Map<String,Object>> claimDelivery(DomainContext c,String worker,ExternalDeliveryAdapter.Support support,int maxAttempts,Duration ttl){
  if(maxAttempts<1||maxAttempts>20||ttl.isNegative()||ttl.isZero()||ttl.compareTo(Duration.ofMinutes(5))>0)throw DomainError.invalid("Invalid delivery policy");
  auth.fence(c,List.of());Instant now=clock.instant();
  var rows=repository.db().queryForList("SELECT * FROM mulino_runtime_Outbox WHERE organizationId=? AND actorId=? AND stableRequestOwner=? AND status='PENDING' AND nextCheckAt<=? ORDER BY nextCheckAt,ID FOR UPDATE SKIP LOCKED LIMIT 1",c.organizationId(),c.actorId(),c.stableRequestOwner(),at(now));
  if(rows.isEmpty())return Optional.empty();var row=rows.getFirst();
  auth.authorize(c,(String)row.get("operation"),null);
  int attempts=((Number)row.get("deliveryattempts")).intValue();
  if(attempts>=maxAttempts){repository.db().update("UPDATE mulino_runtime_Outbox SET status='EXHAUSTED',nextAction='Human must review exhausted delivery and retain responsibility' WHERE organizationId=? AND ID=?",c.organizationId(),row.get("id"));return Optional.empty();}
  long token=((Number)row.get("fencingtoken")).longValue()+1;
  repository.db().update("UPDATE mulino_runtime_Outbox SET status='IN_FLIGHT',deliveryAttempts=deliveryAttempts+1,fencingToken=?,leaseOwner=?,leaseExpiresAt=?,idempotencySupported=?,lookupSupported=?,nextAction='Reconcile external result; local cancellation does not undo it' WHERE organizationId=? AND ID=?",token,worker,at(now.plus(ttl)),support.idempotency(),support.authoritativeLookup(),c.organizationId(),row.get("id"));
  var result=new LinkedHashMap<String,Object>(row);result.put("fencingtoken",token);result.put("leaseowner",worker);return Optional.of(result);
 }
 @Transactional public void deliveryResult(DomainContext c,String id,long token,String worker,ExternalDeliveryAdapter.Result result,String evidence){
  var rows=repository.db().queryForList("SELECT * FROM mulino_runtime_Outbox WHERE organizationId=? AND ID=? FOR UPDATE",c.organizationId(),id);if(rows.size()!=1)throw DomainError.forbidden();var row=rows.getFirst();
  if(!"IN_FLIGHT".equals(row.get("status"))||((Number)row.get("fencingtoken")).longValue()!=token||!Objects.equals(row.get("leaseowner"),worker)||!c.actorId().equals(row.get("actorid"))||!instant(row.get("leaseexpiresat")).isAfter(clock.instant()))throw conflict();
  String status=result==ExternalDeliveryAdapter.Result.CONFIRMED_SUCCESS?"SUCCEEDED":"UNKNOWN_EXTERNAL";
  // Even a synchronous failure needs explicit authoritative reconciliation before retry.
  repository.db().update("UPDATE mulino_runtime_Outbox SET status=?,resultEvidenceId=?,leaseOwner=NULL,leaseExpiresAt=NULL,nextCheckAt=?,nextAction=? WHERE organizationId=? AND ID=?",status,evidence,at(clock.instant().plusSeconds(1)),status.equals("SUCCEEDED")?"External success retained; link evidence to original work":"Authoritative lookup or human reconciliation required; do not redeliver",c.organizationId(),id);
 }
 @Transactional public void recordExternalReconciliation(DomainContext c,String id,ExternalDeliveryAdapter.Result result,String evidence){
  if(evidence==null||!evidence.matches("[A-Za-z0-9._:/-]{1,160}"))throw DomainError.invalid("Authoritative evidence reference required");
  auth.fence(c,List.of());auth.authorize(c,"recordExternalReconciliation",id);
  var rows=repository.db().queryForList("SELECT * FROM mulino_runtime_Outbox WHERE organizationId=? AND ID=? FOR UPDATE",c.organizationId(),id);if(rows.size()!=1)throw DomainError.forbidden();var row=rows.getFirst();
  if(!Set.of("UNKNOWN_EXTERNAL","EXHAUSTED").contains(row.get("status")))throw conflict();
  String decision=result==ExternalDeliveryAdapter.Result.CONFIRMED_SUCCESS?"CONFIRMED_SUCCESS":result==ExternalDeliveryAdapter.Result.CONFIRMED_FAILURE?"CONFIRMED_FAILURE":"UNRESOLVED";
  repository.db().update("INSERT INTO mulino_runtime_ExternalReconciliations VALUES(?,?,?,?,?,?,?)",c.organizationId(),UUID.randomUUID().toString(),id,decision,c.actorId(),evidence,at(clock.instant()));
  String status=result==ExternalDeliveryAdapter.Result.CONFIRMED_SUCCESS?"SUCCEEDED":result==ExternalDeliveryAdapter.Result.CONFIRMED_FAILURE?"PENDING":"UNKNOWN_EXTERNAL";
  repository.db().update("UPDATE mulino_runtime_Outbox SET status=?,resultEvidenceId=?,nextCheckAt=?,nextAction=? WHERE organizationId=? AND ID=?",status,evidence,at(clock.instant().plusSeconds(1)),status.equals("PENDING")?"Retry original externalOperationId after current authority check":"Retain original human duty and reconcile external result",c.organizationId(),id);
 }
 /** Crash after intent commit is ambiguous, including adapters with idempotency support. */
 @Transactional public int recoverExpiredDeliveries(){return repository.db().update("UPDATE mulino_runtime_Outbox SET status='UNKNOWN_EXTERNAL',leaseOwner=NULL,leaseExpiresAt=NULL,nextCheckAt=?,nextAction='Worker interrupted: authoritative lookup or human reconciliation; never automatic redelivery' WHERE status='IN_FLIGHT' AND leaseExpiresAt<=?",at(clock.instant()),at(clock.instant()));}
 @Transactional public List<Map<String,Object>> searchOperationalIssues(DomainContext c){auth.authorize(c,"searchOperationalIssues",null);return repository.db().queryForList("SELECT ID,commandId,externalOperationId,status,nextAction,nextCheckAt FROM mulino_runtime_Outbox WHERE organizationId=? AND status IN ('UNKNOWN_EXTERNAL','EXHAUSTED') ORDER BY nextCheckAt,ID",c.organizationId());}
 @Transactional public void schedule(DomainContext c,String work,String kind,Instant nextCheck,Instant boundary,boolean pending,String owner,String supervisor,String action){
  if(nextCheck==null||owner==null||supervisor==null||action==null||action.isBlank())throw DomainError.invalid("Recovery duty requires owner, supervisor and next check");
  repository.db().update("INSERT INTO mulino_runtime_RecoverySchedules(organizationId,ID,workId,kind,nextCheckAt,nextValidityBoundary,pendingAssessment,ownerId,supervisorId,nextAction) VALUES(?,?,?,?,?,?,?,?,?,?) ON CONFLICT(organizationId,workId,kind) DO UPDATE SET nextCheckAt=EXCLUDED.nextCheckAt,nextValidityBoundary=EXCLUDED.nextValidityBoundary,pendingAssessment=EXCLUDED.pendingAssessment,ownerId=EXCLUDED.ownerId,supervisorId=EXCLUDED.supervisorId,nextAction=EXCLUDED.nextAction,revision=mulino_runtime_RecoverySchedules.revision+1",c.organizationId(),UUID.randomUUID().toString(),work,kind,at(nextCheck),at(boundary),pending,owner,supervisor,action);
 }
 /** Discovery only: predicates/current grants must be rechecked by the original command. */
 @Transactional public List<Map<String,Object>> dueSchedules(int limit){
  if(limit<1||limit>1000)throw DomainError.invalid("Invalid sweep bound");
  return repository.db().queryForList("SELECT * FROM mulino_runtime_RecoverySchedules WHERE nextCheckAt<=? OR nextValidityBoundary<=? OR pendingAssessment=true ORDER BY nextCheckAt,ID FOR UPDATE SKIP LOCKED LIMIT ?",at(clock.instant()),at(clock.instant()),limit);
 }
}
