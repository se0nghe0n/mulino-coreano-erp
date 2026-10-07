package com.mulino.domain.inventory;

import com.mulino.application.core.*;
import com.sap.cds.ql.*;
import com.sap.cds.services.persistence.PersistenceService;
import java.util.*;
import org.springframework.stereotype.Repository;

/** Org predicates are applied in CQN, including every lineage and object lookup. */
@Repository
public class InventoryRepository {
  public static final Set<String> ENTITIES = Set.of("Products", "TradeItems", "SpecificationVersions", "PackagingVersions", "UnitConversions", "ExternalIdentifiers", "Manufacturers", "ManufacturingLots", "Places", "QuantitySegments", "LogisticsUnits", "LogisticsMemberships", "GenealogyEdges", "QuantityMovements", "ObjectRelations", "Stocktakes", "IdentifierConflicts", "SegmentAllocations", "Restrictions", "DispositionBases");
  private final PersistenceService db;
  private final org.springframework.jdbc.core.JdbcTemplate jdbc;
  public InventoryRepository(PersistenceService db,org.springframework.jdbc.core.JdbcTemplate jdbc) { this.db = db; this.jdbc=jdbc; }
  public void fence(DomainContext c,Collection<String> keys) {
    if(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Inventory effect requires gateway transaction");
    jdbc.queryForList("SELECT set_config('lock_timeout', ?, true)","2000ms");
    for(String key:new TreeSet<>(keys))jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",c.organizationId()+":"+key);
  }
  public List<Map<String,Object>> rows(DomainContext c, String entity) {
    check(entity);
    return db.run(Select.from("mulino.inventory."+entity).where(r -> r.get("organizationId").eq(c.organizationId()).and(r.get("recordedAt").le(c.knownAt())))).listOf(Map.class).stream().map(r -> (Map<String,Object>) r).toList();
  }
  public Map<String,Object> object(DomainContext c, String entity, String id) {
    check(entity);
    return db.run(Select.from("mulino.inventory."+entity).where(r -> r.get("organizationId").eq(c.organizationId()).and(r.get("ID").eq(id)).and(r.get("recordedAt").le(c.knownAt())))).first().map(r -> (Map<String,Object>)r).orElseThrow(DomainError::forbidden);
  }
  public void register(String entity, Map<String,Object> row) {
    if (!Set.of("Products","TradeItems","SpecificationVersions","PackagingVersions","ExternalIdentifiers").contains(entity)) throw DomainError.unsupported();
    db.run(Insert.into("mulino.inventory."+entity).entry(row));
  }
  public void recordIdentifierConflict(Map<String,Object> conflict) {
    db.run(Insert.into("mulino.inventory.IdentifierConflicts").entry(conflict));
  }
  // Package-private mutation surface. Only inventory primitives may write physical records.
  void insert(String entity, Map<String,Object> row) {
    check(entity); db.run(Insert.into("mulino.inventory."+entity).entry(row));
  }
  void update(DomainContext c,String entity,String id,Map<String,Object> values) {
    check(entity); db.run(Update.entity("mulino.inventory."+entity).data(values).where(r -> r.get("organizationId").eq(c.organizationId()).and(r.get("ID").eq(id))));
  }
  public Map<String,Object> current(DomainContext c,String entity,String id) {
    check(entity);
    return db.run(Select.from("mulino.inventory."+entity).where(r -> r.get("organizationId").eq(c.organizationId()).and(r.get("ID").eq(id))))
      .first().map(r -> (Map<String,Object>)r).orElseThrow(DomainError::forbidden);
  }
  public List<Map<String,Object>> currentRows(DomainContext c,String entity) {
    check(entity); return db.run(Select.from("mulino.inventory."+entity).where(r -> r.get("organizationId").eq(c.organizationId())))
      .listOf(Map.class).stream().map(r -> (Map<String,Object>)r).toList();
  }
  private void check(String entity) { if (!ENTITIES.contains(entity)) throw DomainError.unsupported(); }
}
