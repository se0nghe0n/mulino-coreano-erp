package com.mulino.domain.trade.settlement;
import com.mulino.application.core.*;
import com.sap.cds.ql.*;
import com.sap.cds.services.persistence.PersistenceService;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronizationManager;
@Repository
public class SettlementRepository {
 private final PersistenceService db;private final JdbcTemplate jdbc;private final ExecutionClock clock;
 public SettlementRepository(PersistenceService db,JdbcTemplate jdbc,ExecutionClock clock){this.db=db;this.jdbc=jdbc;this.clock=clock;}
 public List<Map<String,Object>> rows(DomainContext c,String table){return db.run(Select.from("mulino.trade.settlement."+table).where(x->x.get("organizationId").eq(c.organizationId()))).listOf(Map.class).stream().map(x->(Map<String,Object>)new LinkedHashMap<String,Object>(x)).toList();}
 public Map<String,Object> require(DomainContext c,String table,String id){return rows(c,table).stream().filter(x->id.equals(x.get("ID"))).findFirst().orElseThrow(DomainError::forbidden);}
 public void insert(String table,Map<String,Object> row){transaction();row=new LinkedHashMap<>(row);row.putIfAbsent("revision",1);row.putIfAbsent("createdAt",clock.instant());row.putIfAbsent("recordedAt",clock.instant());row.putIfAbsent("effectiveAt",clock.instant());db.run(Insert.into("mulino.trade.settlement."+table).entry(row));}
 public void update(DomainContext c,String table,String id,Map<String,Object> values){transaction();db.run(Update.entity("mulino.trade.settlement."+table).data(values).where(x->x.get("organizationId").eq(c.organizationId()).and(x.get("ID").eq(id))));}
 public Map<String,Object> external(DomainContext c,String entity,String id){return db.run(Select.from(entity).where(x->x.get("organizationId").eq(c.organizationId()).and(x.get("ID").eq(id)))).first().map(x->new LinkedHashMap<String,Object>(x)).orElseThrow(DomainError::forbidden);}
 public List<Map<String,Object>> externalRows(DomainContext c,String entity){return db.run(Select.from(entity).where(x->x.get("organizationId").eq(c.organizationId()))).listOf(Map.class).stream().map(x->(Map<String,Object>)new LinkedHashMap<String,Object>(x)).toList();}
 public void fence(DomainContext c,String key){transaction();jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",c.organizationId()+":"+key);}
 private void transaction(){if(!TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Settlement requires common command transaction");}
}
