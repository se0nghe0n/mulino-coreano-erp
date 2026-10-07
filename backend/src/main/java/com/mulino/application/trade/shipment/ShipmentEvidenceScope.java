package com.mulino.application.trade.shipment;

import com.mulino.application.core.*;
import com.mulino.application.trade.TradeEvidenceScopePort;
import com.mulino.domain.trade.shipment.ShipmentRepository;
import com.mulino.domain.inventory.*;
import com.fasterxml.jackson.databind.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;

/** Reconciles original source bytes against persisted cargo and route before canonical insertion. */
@Component
public class ShipmentEvidenceScope implements TradeEvidenceScopePort {
 private final ShipmentRepository r;private final InventoryRepository inventory;
 public ShipmentEvidenceScope(ShipmentRepository r,InventoryRepository inventory){this.r=r;this.inventory=inventory;}
 public Set<String> eventKinds(){return Set.of("SHIPMENT_DEPARTURE","SHIPMENT_ARRIVAL","SHIPMENT_IN_TRANSIT","SHIPMENT_DELAY","SHIPMENT_TEMPERATURE_ANOMALY","CUSTODY_HANDOVER");}
 public Scope require(DomainContext c,String physical,Map<String,Object>claim,Map<String,Object>event,Map<String,Object>document,byte[]original){
  try {var mapper=new ObjectMapper();var source=mapper.readTree(original);var report=mapper.readTree(event.get("payload").toString());
   String shipment=id(source,"shipmentId"),cargo=id(source,"cargoId"),leg=id(source,"legId"),place=id(source,"placeId");var sh=r.currentShipment(c,shipment);var ca=r.requireCargo(c,cargo);var route=r.require(c,"ShipmentLegs",leg);inventory.current(c,"Places",place);
   if(!shipment.equals(ca.get("shipmentId"))||!shipment.equals(route.get("shipmentId"))||!physical.equals(id(source,"physicalScopeId")))throw held();
   String kind=event.get("kind").toString();for(String key:List.of("shipmentId","cargoId","legId","placeId","physicalScopeId","quantity","unit","occurredAt"))if(!source.path(key).equals(report.path(key)))throw held();
   if(!ca.get("itemId").equals(claim.get("itemId"))||!ca.get("itemId").equals(event.get("itemId")))throw held();
   BigDecimal q=InventoryQuantity.parse(source.path("quantity").asText(),12);if(!(claim.get("quantity") instanceof BigDecimal quantity)||q.compareTo(quantity)!=0||q.compareTo((BigDecimal)ca.get("plannedQuantity"))>0||!ca.get("unit").equals(source.path("unit").asText())||!ca.get("unit").equals(claim.get("unit")))throw held();
   Instant at=Instant.parse(source.path("occurredAt").asText());if(!at.equals(StockPrimitives.instant(claim.get("effectiveFrom")))||!at.equals(StockPrimitives.instant(event.get("effectiveFrom")))||at.isAfter(c.knownAt()))throw held();
   if("SHIPMENT_DEPARTURE".equals(kind)&&!place.equals(route.get("originId"))||"SHIPMENT_ARRIVAL".equals(kind)&&!place.equals(route.get("destinationId")))throw held();
   if(kind.equals("CUSTODY_HANDOVER")){String from=id(source,"fromCustodianId"),to=id(source,"toCustodianId");if(from.equals(to)||!source.path("fromCustodianId").equals(report.path("fromCustodianId"))||!source.path("toCustodianId").equals(report.path("toCustodianId")))throw held();}
   else if(!kind.equals("SHIPMENT_"+source.path("kind").asText())||!source.path("kind").equals(report.path("kind")))throw held();
   var fields=new LinkedHashMap<String,Object>();fields.put("itemId",ca.get("itemId"));fields.put("placeId",place);var scopes=new LinkedHashMap<String,List<String>>();var targets=new ArrayList<String>(List.of(shipment,cargo,physical));for(var a:r.cargoAllocations(c,cargo))targets.add(a.get("poLineId").toString());scopes.put("TARGET",targets);scopes.put("ITEM",List.of(ca.get("itemId").toString()));scopes.put("PLACE",List.of(place));if(sh.get("workId")!=null){fields.put("workId",sh.get("workId"));scopes.put("WORK",List.of(sh.get("workId").toString()));}return new Scope(fields,scopes);
  }catch(java.io.IOException|java.time.format.DateTimeParseException malformed){throw held();}
 }
 private static String id(JsonNode node,String key){return CommandRequests.uuid(node.path(key).asText());}
 private static DomainError held(){return new DomainError("HELD","EVIDENCE_UNVERIFIED","Original shipment bytes and exact cargo scope differ");}
}
