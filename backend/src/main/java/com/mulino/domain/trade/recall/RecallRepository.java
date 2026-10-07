package com.mulino.domain.trade.recall;
import com.mulino.application.core.*;
import com.sap.cds.ql.*;
import com.sap.cds.services.persistence.PersistenceService;
import java.util.*;
import org.springframework.stereotype.Repository;
@Repository
public class RecallRepository {
 private final PersistenceService db;
 public RecallRepository(PersistenceService db){this.db=db;}
 private String entity(String e){if(!Set.of("Investigations","Scopes","Approvals","Actions","Closures").contains(e))throw DomainError.unsupported();return "mulino.trade.recall."+e;}
 public List<Map<String,Object>> rows(DomainContext c,String e){return db.run(Select.from(entity(e)).where(x->x.get("organizationId").eq(c.organizationId()).and(x.get("recordedAt").le(c.knownAt())))).listOf(Map.class).stream().map(x->(Map<String,Object>)x).toList();}
 public Map<String,Object> require(DomainContext c,String e,String id){return rows(c,e).stream().filter(x->id.equals(x.get("ID"))).findFirst().orElseThrow(DomainError::forbidden);}
 public Map<String,Object> current(String org,String e,String id){return db.run(Select.from(entity(e)).where(x->x.get("organizationId").eq(org).and(x.get("ID").eq(id)))).first().map(x->(Map<String,Object>)x).orElseThrow(DomainError::forbidden);}
 public List<Map<String,Object>> externalRows(DomainContext c,String entity){if(!Set.of("mulino.trade.returns.Receipts","mulino.trade.sales.Dispatches","mulino.trade.sales.CargoScopes","mulino.trade.sales.Deliveries","mulino.trade.shipment.Shipments","mulino.trade.shipment.CargoScopes").contains(entity))throw DomainError.unsupported();return db.run(Select.from(entity).where(x->x.get("organizationId").eq(c.organizationId()).and(x.get("recordedAt").le(c.knownAt())))).listOf(Map.class).stream().map(x->(Map<String,Object>)x).toList();}
 public void insert(String e,Map<String,Object> row){db.run(Insert.into(entity(e)).entry(row));}
 public void update(DomainContext c,String e,String id,Map<String,Object> changes){db.run(Update.entity(entity(e)).data(changes).where(x->x.get("organizationId").eq(c.organizationId()).and(x.get("ID").eq(id))));}
}
