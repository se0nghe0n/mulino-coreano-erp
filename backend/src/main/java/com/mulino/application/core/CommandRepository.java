package com.mulino.application.core;
import com.sap.cds.ql.*;
import com.sap.cds.services.persistence.PersistenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronizationManager;
@Repository
public class CommandRepository {
  private final PersistenceService db;private final JdbcTemplate jdbc;private final ObjectMapper json=new ObjectMapper();
  public CommandRepository(PersistenceService db,JdbcTemplate jdbc){this.db=db;this.jdbc=jdbc;}
  public void fence(DomainContext c,Collection<String> keys){
    if(!TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Command fence requires transaction");
    jdbc.queryForList("SELECT set_config('lock_timeout', ?, true)","2000ms");
    for(String key:new TreeSet<>(keys))jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))",c.organizationId()+":"+key);
  }
  public Optional<Map<String,Object>> find(DomainContext c,String cap,String key){
    return db.run(Select.from("mulino.commands.CommandRecords").where(b->b.get("organizationId").eq(c.organizationId()).and(b.get("stableRequestOwner").eq(c.stableRequestOwner())).and(b.get("capabilityId").eq(cap)).and(b.get("commandIdempotencyKey").eq(key)))).first().map(r->(Map<String,Object>)r);
  }
  public Optional<Map<String,Object>> owned(DomainContext c,String id){
    return db.run(Select.from("mulino.commands.CommandRecords").where(b->b.get("organizationId").eq(c.organizationId()).and(b.get("ID").eq(id)).and(b.get("stableRequestOwner").eq(c.stableRequestOwner())).and(b.get("actorId").eq(c.actorId())))).first().map(r->(Map<String,Object>)r);
  }
  public Map<String,List<String>> scopes(Map<String,Object> row){try{return json.readValue((String)row.get("authorizationScopeJson"),Map.class);}catch(Exception failure){throw DomainError.forbidden();}}
  public Map<String,Object> original(Map<String,Object> row){try{return json.readValue((String)row.get("canonicalIntentJson"),Map.class);}catch(Exception failure){throw new DomainError("REJECTED","COMMAND_UNAVAILABLE","Canonical request unavailable");}}
  public String begin(DomainContext c,Map<String,Object> intent,String hash,java.time.Instant now,CommandPreparation validated){
    String id=UUID.randomUUID().toString();Map<String,Object> row=new HashMap<>();row.put("ID",id);row.put("organizationId",c.organizationId());row.put("actorId",c.actorId());if(validated!=null){row.put("canonicalIntentJson",encode(intent));row.put("authorizationScopeJson",encode(validated.scopes()));row.put("effectClass",validated.effectClass());}row.put("stableRequestOwner",c.stableRequestOwner());row.put("capabilityId",intent.get("capabilityId"));row.put("commandIdempotencyKey",intent.get("commandIdempotencyKey"));row.put("canonicalHash",hash);row.put("definitionVersion",intent.get("definitionVersion"));row.put("capabilityVersion",intent.get("capabilityVersion"));row.put("state","IN_PROGRESS");row.put("createdAt",now);
    db.run(Insert.into("mulino.commands.CommandRecords").entry(row));return id;
  }
  public void finish(DomainContext c,String id,Map<String,Object> intent,String hash,Map<String,Object> result,java.time.Instant now){
    String outcome=(String)result.get("outcome");String state=Set.of("REJECTED","CONFLICT","NEEDS_INPUT","WAITING_APPROVAL","HELD").contains(outcome)?"REJECTED":"COMMITTED";
    db.run(Update.entity("mulino.commands.CommandRecords").byId(id).data(Map.of("state",state,"resultJson",encode(result),"completedAt",now)));
    // Only server identifiers/results, never raw intent, bearer token, or user text.
    db.run(Insert.into("mulino.commands.CommandAudits").entry(Map.of("ID",UUID.randomUUID().toString(),"organizationId",c.organizationId(),"actorId",c.actorId(),"commandId",id,"capabilityId",intent.get("capabilityId"),"canonicalHash",hash,"outcome",outcome,"effectRefs",encode(safeRefs(result)),"createdAt",now)));
  }
  private Map<String,Object> safeRefs(Map<String,Object> result){Map<String,Object> refs=new TreeMap<>();for(String key:List.of("workId","activityId","obligationId","revision","commandId")){Object value=result.get(key);if(value instanceof Number)refs.put(key,value);else if(value instanceof String s&&s.matches("[a-fA-F0-9-]{36}"))refs.put(key,s);}return refs;}
  public String encode(Object value){try{return json.writeValueAsString(TransportValues.normalize(value));}catch(Exception failure){throw new IllegalStateException("Cannot persist command result",failure);}}
  public Map<String,Object> result(Map<String,Object> row){try{return json.readValue((String)row.get("resultJson"),Map.class);}catch(Exception failure){throw new IllegalStateException("Invalid persisted result",failure);}}
}
