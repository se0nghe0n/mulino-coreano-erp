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
 private final PersistenceService db; private final JdbcTemplate jdbc;
 public WorkRepository(PersistenceService db,JdbcTemplate jdbc){this.db=db;this.jdbc=jdbc;}
 public Map<String,Object> require(DomainContext c,String id,boolean lock){
  if(lock){if(!TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Work lock requires transaction");jdbc.queryForList("SELECT ID FROM mulino_work_read_Works WHERE organizationId=? AND ID=? FOR UPDATE",c.organizationId(),id);}
  return db.run(Select.from("mulino.work.read.Works").where(r->r.get("organizationId").eq(c.organizationId()).and(r.get("ID").eq(id)))).first().map(r->new LinkedHashMap<String,Object>(r)).orElseThrow(DomainError::forbidden);
 }
 public Map<String,Object> currentGoal(DomainContext c,String id){var w=require(c,id,false);Object goal=w.get("currentGoalVersionId");if(goal==null)throw new DomainError("NEEDS_INPUT","GOAL_MISSING","Imported work has no current lifecycle goal");return db.run(Select.from("mulino.work.read.GoalReferences").where(r->r.get("organizationId").eq(c.organizationId()).and(r.get("ID").eq(goal)))).first().map(r->new LinkedHashMap<String,Object>(r)).orElseThrow(DomainError::forbidden);}
 public void insert(String entity,Map<String,Object> row){db.run(Insert.into(entity).entry(row));}
 public void update(DomainContext c,String id,Map<String,Object> changes){db.run(Update.entity("mulino.work.read.Works").data(changes).where(r->r.get("organizationId").eq(c.organizationId()).and(r.get("ID").eq(id))));}
 public List<Map<String,Object>> links(DomainContext c){return db.run(Select.from("mulino.work.WorkLinks").where(r->r.get("organizationId").eq(c.organizationId()))).listOf(Map.class).stream().map(r->(Map<String,Object>)r).toList();}
 public void replaceOwner(DomainContext c,String id,String previous,String next){var w=require(c,id,true);if(!Objects.equals(previous,w.get("ownerId")))throw new DomainError("CONFLICT","OWNER_CHANGED","Owner changed");if(!Set.of("ACTIVE","WAITING").contains(w.get("status")))throw DomainError.invalid("Only active work accepts owner handover");update(c,id,Map.of("ownerId",next,"revision",((Number)w.get("revision")).intValue()+1));}
 public void markInvalidation(DomainContext c,String id,boolean pending){var w=require(c,id,true);update(c,id,Map.of("pendingInvalidation",pending,"revision",((Number)w.get("revision")).intValue()+1));}
}
