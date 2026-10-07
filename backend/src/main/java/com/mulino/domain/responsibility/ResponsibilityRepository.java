package com.mulino.domain.responsibility;

import com.mulino.application.core.*;
import com.sap.cds.ql.*;
import com.sap.cds.services.persistence.PersistenceService;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Repository
public class ResponsibilityRepository {
  private final PersistenceService db; private final JdbcTemplate jdbc;
  public ResponsibilityRepository(PersistenceService db,JdbcTemplate jdbc){this.db=db;this.jdbc=jdbc;}
  private String entity(String name){return "Assignments".equals(name)?"mulino.work.read.ObligationReferences":"mulino.responsibility."+name;}
  public List<Map<String,Object>> rows(String name,String org){return db.run(Select.from(entity(name)).where(x->x.get("organizationId").eq(org))).listOf(Map.class).stream().map(x->(Map<String,Object>)new LinkedHashMap<String,Object>(x)).toList();}
  public Map<String,Object> require(String name,String org,String id){return rows(name,org).stream().filter(x->id.equals(x.get("ID"))).findFirst().orElseThrow(DomainError::forbidden);}
  public void insert(String name,Map<String,Object> row){db.run(Insert.into(entity(name)).entry(row));}
  public void update(String name,String org,String id,Map<String,Object> values){db.run(Update.entity(entity(name)).data(values).where(x->x.get("organizationId").eq(org).and(x.get("ID").eq(id))));}
  public void fence(String org,String key){if(!TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Responsibility requires parent transaction");jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",org+"|responsibility|"+key);}
}
