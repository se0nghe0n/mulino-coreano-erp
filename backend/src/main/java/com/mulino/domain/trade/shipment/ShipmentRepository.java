package com.mulino.domain.trade.shipment;

import com.mulino.application.core.*;
import com.sap.cds.ql.*;
import com.sap.cds.services.persistence.PersistenceService;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class ShipmentRepository {
 private final PersistenceService db;
 public ShipmentRepository(PersistenceService db){this.db=db;}
 public List<Map<String,Object>> rows(DomainContext c,String entity){return db.run(Select.from("mulino.trade.shipment."+entity).where(x->x.get("organizationId").eq(c.organizationId())).orderBy("ID")).listOf(Map.class).stream().map(x->(Map<String,Object>)new LinkedHashMap<String,Object>(x)).toList();}
 public Map<String,Object> require(DomainContext c,String entity,String id){return rows(c,entity).stream().filter(r->id.equals(r.get("ID"))).findFirst().orElseThrow(DomainError::forbidden);}
 public Map<String,Object> currentShipment(DomainContext c,String id){return require(c,"Shipments",id);}
 public Map<String,Object> requireCargo(DomainContext c,String id){return require(c,"ShipmentCargo",id);}
 public List<Map<String,Object>> cargoRows(DomainContext c,String shipment){return rows(c,"ShipmentCargo").stream().filter(r->shipment.equals(r.get("shipmentId"))).toList();}
 public List<Map<String,Object>> cargoAllocations(DomainContext c,String cargo){return rows(c,"CargoAllocations").stream().filter(r->cargo.equals(r.get("cargoId"))).toList();}
 public void insert(String entity,Map<String,Object> row){db.run(Insert.into("mulino.trade.shipment."+entity).entry(row));}
 public void advance(DomainContext c,String shipment){var r=currentShipment(c,shipment);db.run(Update.entity("mulino.trade.shipment.Shipments").data(Map.of("revision",((Number)r.get("revision")).intValue()+1)).where(x->x.get("organizationId").eq(c.organizationId()).and(x.get("ID").eq(shipment))));}
 public static Map<String,Object> row(DomainContext c){var r=new LinkedHashMap<String,Object>();r.put("organizationId",c.organizationId());r.put("ID",UUID.randomUUID().toString());r.put("revision",1);r.put("createdAt",c.knownAt());r.put("recordedAt",c.knownAt());r.put("recordedBy",c.actorId());return r;}
}
