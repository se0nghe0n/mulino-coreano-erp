package com.mulino.domain.inventory;

import com.mulino.application.core.*;
import com.sap.cds.ql.*;
import com.sap.cds.services.persistence.PersistenceService;
import java.util.*;
import org.springframework.stereotype.Repository;

/** Org predicates are applied in CQN, including every lineage and object lookup. */
@Repository
public class InventoryRepository {
  public static final Set<String> ENTITIES = Set.of("Products", "TradeItems", "SpecificationVersions", "PackagingVersions", "UnitConversions", "ExternalIdentifiers", "Manufacturers", "ManufacturingLots", "Places", "QuantitySegments", "LogisticsUnits", "LogisticsMemberships", "GenealogyEdges", "QuantityMovements", "ObjectRelations");
  private final PersistenceService db;
  public InventoryRepository(PersistenceService db) { this.db = db; }
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
  private void check(String entity) { if (!ENTITIES.contains(entity)) throw DomainError.unsupported(); }
}
