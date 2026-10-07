package com.mulino.domain.work;

import com.mulino.application.core.*;
import com.mulino.application.work.WorkAccess;
import com.sap.cds.ql.*;
import com.sap.cds.services.persistence.PersistenceService;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Repository
public class WorkRepository implements WorkAccess {
 private final PersistenceService db; private final JdbcTemplate jdbc; private final org.springframework.beans.factory.ObjectProvider<com.mulino.application.work.WorkLifecycleObserver> observers; private final org.springframework.beans.factory.ObjectProvider<com.mulino.application.work.WorkFollowup> followups;
 public WorkRepository(PersistenceService db,JdbcTemplate jdbc,org.springframework.beans.factory.ObjectProvider<com.mulino.application.work.WorkLifecycleObserver> observers,org.springframework.beans.factory.ObjectProvider<com.mulino.application.work.WorkFollowup> followups){this.db=db;this.jdbc=jdbc;this.observers=observers;this.followups=followups;}
 public Map<String,Object> require(DomainContext c,String id,boolean lock){
  if(lock){if(!TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Work lock requires transaction");jdbc.queryForList("SELECT ID FROM mulino_work_read_Works WHERE organizationId=? AND ID=? FOR UPDATE",c.organizationId(),id);}
  return db.run(Select.from("mulino.work.read.Works").where(r->r.get("organizationId").eq(c.organizationId()).and(r.get("ID").eq(id)))).first().map(r->new LinkedHashMap<String,Object>(r)).orElseThrow(DomainError::forbidden);
 }
 public Map<String,Object> currentGoal(DomainContext c,String id){var w=require(c,id,false);Object goal=w.get("currentGoalVersionId");if(goal==null)throw new DomainError("NEEDS_INPUT","GOAL_MISSING","Imported work has no current lifecycle goal");return db.run(Select.from("mulino.work.read.GoalReferences").where(r->r.get("organizationId").eq(c.organizationId()).and(r.get("ID").eq(goal)))).first().map(r->new LinkedHashMap<String,Object>(r)).orElseThrow(DomainError::forbidden);}
 public void insert(String entity,Map<String,Object> row){db.run(Insert.into(entity).entry(row));}
 public void update(DomainContext c,String id,Map<String,Object> changes){db.run(Update.entity("mulino.work.read.Works").data(changes).where(r->r.get("organizationId").eq(c.organizationId()).and(r.get("ID").eq(id))));}
 public String definitionId(DomainContext c,String version){return db.run(Select.from("mulino.definitions.DefinitionVersions").where(r->r.get("organizationId").eq(c.organizationId()).and(r.get("state").eq("PUBLISHED")).and(r.get("version").eq(version)))).first().map(r->r.get("ID").toString()).orElseThrow(DomainError::unsupported);}
 public List<Map<String,Object>> links(DomainContext c){return db.run(Select.from("mulino.work.WorkLinks").where(r->r.get("organizationId").eq(c.organizationId()))).listOf(Map.class).stream().map(r->(Map<String,Object>)r).toList();}
 public void replaceOwner(DomainContext c,String id,String previous,String next){var w=require(c,id,true);if(!Objects.equals(previous,w.get("ownerId")))throw new DomainError("CONFLICT","OWNER_CHANGED","Owner changed");if(!Set.of("ACTIVE","WAITING").contains(w.get("status")))throw DomainError.invalid("Only active work accepts owner handover");mutate(c,w,Map.of("ownerId",next),"ownerHandover");}
 public Map<String,Object> ensureFollowup(DomainContext c,String original,String source,String reason,String nextAction,java.time.Instant nextCheck){return followups.getObject().ensure(c,original,source,reason,nextAction,nextCheck);}
 public void markInvalidation(DomainContext c,String id,boolean pending){var w=require(c,id,true);mutate(c,w,Map.of("pendingInvalidation",pending),"assessmentInvalidation");}
 public java.util.Optional<Map<String,Object>> find(DomainContext c,String id){return db.run(Select.from("mulino.work.read.Works").where(r->r.get("organizationId").eq(c.organizationId()).and(r.get("ID").eq(id)))).first().map(r->new LinkedHashMap<String,Object>(r));}
 private void mutate(DomainContext c,Map<String,Object> w,Map<String,Object> values,String operation){
  var changes=new LinkedHashMap<String,Object>(values);changes.put("revision",((Number)w.get("revision")).intValue()+1);changes.put("recordedAt",java.time.Instant.now());changes.put("effectiveAt",java.time.Instant.now());update(c,String.valueOf(w.get("ID")),changes);var snapshot=new LinkedHashMap<>(w);snapshot.putAll(changes);var transition=new LinkedHashMap<String,Object>();transition.put("organizationId",c.organizationId());transition.put("ID",UUID.randomUUID().toString());transition.put("workId",w.get("ID"));transition.put("revision",changes.get("revision"));transition.put("operation",operation);transition.put("previousState",w.get("status"));transition.put("state",w.get("status"));transition.put("actorId",c.actorId());transition.put("recordedAt",changes.get("recordedAt"));try{transition.put("snapshotJson",new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(TransportValues.normalize(snapshot)));}catch(Exception e){throw new IllegalStateException(e);}insert("mulino.work.WorkTransitions",transition);observers.orderedStream().forEach(o->o.changed(c,snapshot));
 }
 public void observed(DomainContext c,Map<String,Object> work){observers.orderedStream().forEach(o->o.changed(c,work));}
}
