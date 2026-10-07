package com.mulino.domain.runtime;

import com.mulino.application.core.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

/** Fixed PostgreSQL lock primitives share the parent business transaction. */
@Repository
public class RuntimeRepository implements CommandLeasePort,TransactionalOutboxPort {
  private final JdbcTemplate db;
  private final ExecutionClock clock;
  private final ObjectMapper json=new ObjectMapper();
  public RuntimeRepository(JdbcTemplate db,ExecutionClock clock){this.db=db;this.clock=clock;}
  public JdbcTemplate db(){return db;}
  public static Timestamp at(Instant i){return i==null?null:Timestamp.from(i);}
  public static Instant instant(Object value){return value instanceof Timestamp t?t.toInstant():value instanceof java.time.OffsetDateTime t?t.toInstant():Instant.parse(value.toString());}
  public static void transaction(){if(!TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Runtime persistence requires parent transaction");}
  public static DomainError conflict(){return new DomainError("CONFLICT","STALE_EXECUTION","Execution claim is no longer current");}
  public Optional<Map<String,Object>> claim(DomainContext c,String work,String capability,String command,String worker,Duration ttl){
    transaction();validateTtl(ttl);Instant now=clock.instant();
    db.update("INSERT INTO mulino_runtime_ExecutionScopes(organizationId,workId,capabilityId,commandId) VALUES(?,?,?,?) ON CONFLICT DO NOTHING",c.organizationId(),work,capability,command);
    var rows=db.queryForList("SELECT * FROM mulino_runtime_ExecutionScopes WHERE organizationId=? AND workId=? AND capabilityId=? AND commandId=? FOR UPDATE SKIP LOCKED",c.organizationId(),work,capability,command);
    if(rows.isEmpty())return Optional.empty();var scope=rows.getFirst();
    if(scope.get("attemptid")!=null){var a=db.queryForMap("SELECT * FROM mulino_runtime_ExecutionAttempts WHERE organizationId=? AND ID=?",c.organizationId(),scope.get("attemptid"));
      if("SUCCEEDED".equals(a.get("status")))return Optional.empty();
      if("ACTIVE".equals(a.get("status"))&&instant(a.get("leaseexpiresat")).isAfter(now))return Optional.empty();
      db.update("UPDATE mulino_runtime_ExecutionAttempts SET status='EXPIRED',finishedAt=? WHERE organizationId=? AND ID=? AND status='ACTIVE'",at(now),c.organizationId(),a.get("id"));
    }
    long token=((Number)scope.get("fencingtoken")).longValue()+1;String id=UUID.randomUUID().toString();
    db.update("INSERT INTO mulino_runtime_ExecutionAttempts(organizationId,ID,workId,capabilityId,commandId,actorId,stableRequestOwner,leaseOwner,fencingToken,startedAt,heartbeatAt,leaseExpiresAt,status) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,'ACTIVE')",c.organizationId(),id,work,capability,command,c.actorId(),c.stableRequestOwner(),worker,token,at(now),at(now),at(now.plus(ttl)));
    db.update("UPDATE mulino_runtime_ExecutionScopes SET fencingToken=?,attemptId=? WHERE organizationId=? AND workId=? AND capabilityId=? AND commandId=?",token,id,c.organizationId(),work,capability,command);
    return Optional.of(Map.of("attemptId",id,"leaseToken",token,"leaseOwner",worker,"workId",work,"capabilityId",capability,"commandId",command));
  }
  @Override public void fenceAndVerify(DomainContext c,Map<String,Object> claim){
    transaction();if(claim==null||claim.isEmpty())return;
    var rows=db.queryForList("SELECT a.* FROM mulino_runtime_ExecutionScopes s JOIN mulino_runtime_ExecutionAttempts a ON a.organizationId=s.organizationId AND a.ID=s.attemptId WHERE s.organizationId=? AND s.workId=? AND s.capabilityId=? AND s.commandId=? FOR UPDATE OF s,a",c.organizationId(),claim.get("workId"),claim.get("capabilityId"),claim.get("commandId"));
    if(rows.size()!=1)throw conflict();var a=rows.getFirst();
    if(!Objects.equals(a.get("id"),claim.get("attemptId"))||!Objects.equals(a.get("leaseowner"),claim.get("leaseOwner"))||!(claim.get("leaseToken") instanceof Number token)||((Number)a.get("fencingtoken")).longValue()!=token.longValue()||!"ACTIVE".equals(a.get("status"))||!instant(a.get("leaseexpiresat")).isAfter(clock.instant())||!c.actorId().equals(a.get("actorid"))||!c.stableRequestOwner().equals(a.get("stablerequestowner")))throw conflict();
  }
  public void heartbeat(DomainContext c,Map<String,Object> claim,Duration ttl){validateTtl(ttl);fenceAndVerify(c,claim);Instant now=clock.instant();db.update("UPDATE mulino_runtime_ExecutionAttempts SET heartbeatAt=?,leaseExpiresAt=? WHERE organizationId=? AND ID=?",at(now),at(now.plus(ttl)),c.organizationId(),claim.get("attemptId"));}
  public void finish(DomainContext c,Map<String,Object> claim,String status,String code){
    if(!Set.of("SUCCEEDED","FAILED","HELD","CANCELLED").contains(status))throw DomainError.invalid("Invalid technical status");
    if(code!=null&&!code.matches("[A-Z0-9_]{1,80}"))throw DomainError.invalid("Technical code must be secret-safe");
    fenceAndVerify(c,claim);db.update("UPDATE mulino_runtime_ExecutionAttempts SET status=?,technicalCode=?,finishedAt=? WHERE organizationId=? AND ID=?",status,code,at(clock.instant()),c.organizationId(),claim.get("attemptId"));
  }
  @Override public String enqueue(DomainContext c,String command,String externalId,String operation,Map<String,Object> payload){
    transaction();safePayload(payload);String serialized=json.writeValueAsString(payload);
    var existing=db.queryForList("SELECT * FROM mulino_runtime_Outbox WHERE organizationId=? AND externalOperationId=? FOR UPDATE",c.organizationId(),externalId);
    if(!existing.isEmpty()){var e=existing.getFirst();if(!command.equals(e.get("commandid"))||!operation.equals(e.get("operation"))||!serialized.equals(e.get("payloadjson"))||!c.actorId().equals(e.get("actorid"))||!c.stableRequestOwner().equals(e.get("stablerequestowner")))throw conflict();return (String)e.get("id");}
    String id=UUID.randomUUID().toString();Instant now=clock.instant();
    db.update("INSERT INTO mulino_runtime_Outbox(organizationId,ID,commandId,externalOperationId,operation,payloadJson,actorId,stableRequestOwner,status,createdAt,nextCheckAt,nextAction) VALUES(?,?,?,?,?,?,?,?,'PENDING',?,?,?)",c.organizationId(),id,command,externalId,operation,serialized,c.actorId(),c.stableRequestOwner(),at(now),at(now),"Verify current authority before external delivery");return id;
  }
  private static void validateTtl(Duration ttl){if(ttl.isNegative()||ttl.isZero()||ttl.compareTo(Duration.ofMinutes(5))>0)throw DomainError.invalid("Lease duration outside bounded policy");}
  private static void safePayload(Object value){
    if(value instanceof Map<?,?> m){for(var e:m.entrySet()){String key=e.getKey().toString().toLowerCase(Locale.ROOT);if(key.matches(".*(token|secret|password|authorization|privateprofile|credential).*"))throw DomainError.invalid("Secret-bearing payload is not queueable");safePayload(e.getValue());}}
    else if(value instanceof Collection<?> list)list.forEach(RuntimeRepository::safePayload);
    else if(value instanceof String s && (s.toLowerCase(Locale.ROOT).contains("bearer ")||s.contains("-----BEGIN PRIVATE KEY")))throw DomainError.invalid("Secret-bearing payload is not queueable");
  }
}
