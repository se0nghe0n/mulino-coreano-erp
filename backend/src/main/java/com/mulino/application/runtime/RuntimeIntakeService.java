package com.mulino.application.runtime;

import com.mulino.application.core.*;
import com.mulino.domain.runtime.RuntimeRepository;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.mulino.domain.runtime.RuntimeRepository.*;

/** Mutable recovery index references immutable intake and canonical responsibility. */
@Service
public class RuntimeIntakeService {
 private final RuntimeRepository repository;private final ExecutionClock clock;
 public RuntimeIntakeService(RuntimeRepository repository,ExecutionClock clock){this.repository=repository;this.clock=clock;}
 @Transactional public List<Map<String,Object>> due(){
  repository.db().update("INSERT INTO mulino_runtime_IntakeRecoveries(organizationId,ID,eventId,sourceProfileId,intakeOwnerId,supervisorId,nextAction,nextCheckAt) SELECT organizationId,ID,eventId,sourceProfileId,intakeOwnerId,supervisorId,nextAction,nextCheckAt FROM mulino_evidence_InboxRecords WHERE eventId IS NOT NULL AND intakeOwnerId IS NOT NULL AND supervisorId IS NOT NULL ON CONFLICT DO NOTHING");
  return repository.db().queryForList("SELECT * FROM mulino_runtime_IntakeRecoveries WHERE state IN ('PENDING','HELD_POLICY','HELD_LINK') AND nextCheckAt<=? AND attempts<3 ORDER BY nextCheckAt,ID FOR UPDATE SKIP LOCKED LIMIT 100",at(clock.instant()));
 }
 @Transactional public void hold(Map<String,Object> index,String code,String action){
  String status=code.equals("POLICY_UNRESOLVED")?"HELD_POLICY":"HELD_LINK";
  repository.db().update("UPDATE mulino_runtime_IntakeRecoveries SET state=CASE WHEN attempts+1>=3 THEN 'HELD_MANUAL' ELSE ? END,lastCode=?,nextAction=?,nextCheckAt=?,attempts=attempts+1,revision=revision+1 WHERE organizationId=? AND ID=? AND state NOT IN ('LINKED','NO_RESPONSE')",status,code,action,at(clock.instant().plusSeconds(1)),index.get("organizationid"),index.get("id"));
 }
 @Transactional public Optional<Map<String,Object>> claimCreate(DomainContext c,Map<String,Object> index,Map<String,Object> intent){
  RuntimeRepository.transaction();RuntimeRepository.requireQueueSafe(intent);var rows=repository.db().queryForList("SELECT * FROM mulino_runtime_IntakeRecoveries WHERE organizationId=? AND ID=? FOR UPDATE SKIP LOCKED",c.organizationId(),index.get("id"));if(rows.isEmpty())return Optional.empty();var row=rows.getFirst();
  if(row.get("leaseexpiresat")!=null&&instant(row.get("leaseexpiresat")).isAfter(clock.instant()))return Optional.empty();String hash=CommandRequests.hash(intent);
  if(row.get("canonicalintenthash")!=null&&!hash.equals(row.get("canonicalintenthash")))throw conflict();
  long token=((Number)row.get("fencingtoken")).longValue()+1;
  repository.db().update("UPDATE mulino_runtime_IntakeRecoveries SET createIntentJson=?,canonicalIntentHash=?,leaseOwner='intake-recovery',fencingToken=?,leaseExpiresAt=? WHERE organizationId=? AND ID=?",new tools.jackson.databind.ObjectMapper().writeValueAsString(intent),hash,token,at(clock.instant().plusSeconds(5)),c.organizationId(),index.get("id"));
  return Optional.of(Map.of("organizationId",c.organizationId(),"scopeKind","INTAKE","intakeId",index.get("id"),"capabilityId","createDraft","commandId",intent.get("commandIdempotencyKey"),"canonicalIntentHash",hash,"leaseToken",token,"leaseOwner","intake-recovery"));
 }
 @Transactional public void created(Map<String,Object> index,String work){repository.db().update("UPDATE mulino_runtime_IntakeRecoveries SET createdWorkId=?,leaseExpiresAt=NULL WHERE organizationId=? AND ID=?",work,index.get("organizationid"),index.get("id"));}
 @Transactional public void policy(Map<String,Object> index,String policy,String hash){repository.db().update("UPDATE mulino_runtime_IntakeRecoveries SET policyId=?,policyHash=? WHERE organizationId=? AND ID=?",policy,hash,index.get("organizationid"),index.get("id"));}
}
