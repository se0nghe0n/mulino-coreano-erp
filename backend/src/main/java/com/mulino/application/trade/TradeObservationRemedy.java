package com.mulino.application.trade;
import com.mulino.application.core.*;
import com.mulino.application.responsibility.ResponsibilityService;
import com.mulino.domain.responsibility.ResponsibilityRepository;
import com.mulino.domain.inventory.*;
import com.mulino.domain.trade.sales.SalesRepository;
import com.mulino.domain.trade.returns.ReturnRepository;
import java.math.BigDecimal;import java.util.*;
import org.springframework.stereotype.Component;
/** Only applied exact domain facts discharge provisional observation responsibility. */
@Component
public final class TradeObservationRemedy {
 private final SalesRepository sales;private final ReturnRepository returns;private final InventoryRepository inventory;
 private final TradeEvidence evidence;private final ResponsibilityRepository duties;private final ResponsibilityService service;
 public TradeObservationRemedy(SalesRepository sales,ReturnRepository returns,InventoryRepository inventory,TradeEvidence evidence,ResponsibilityRepository duties,ResponsibilityService service){this.sales=sales;this.returns=returns;this.inventory=inventory;this.evidence=evidence;this.duties=duties;this.service=service;}
 public void reconcileDeliveryObservation(DomainContext c,String observationId,String deliveryId){apply(c,sales.require(c,"Observations",observationId),sales.require(c,"Deliveries",deliveryId),"DELIVERY_RECONCILIATION","PHYSICAL_DELIVERY","DELIVERY");}
 public void reconcileReturnObservation(DomainContext c,String observationId,String receiptId){apply(c,returns.require(c,"Observations",observationId),returns.require(c,"Receipts",receiptId),"RETURN_RECONCILIATION","RETURN_RECEIPT","RETURN_MOVE");}
 private void apply(DomainContext c,Map<String,Object> observation,Map<String,Object> actual,String dutyKind,String eventKind,String movementKind){
  if(!"CONFIRMED".equals(observation.get("state"))||!Objects.equals(observation.get("ID"),actual.get("observationId")))throw DomainError.invalid("Applied confirmed exact trade observation required");
  for(String key:List.of("workId","itemId","lotId","rangeRootId","unit","placeId","customerId"))if(!Objects.equals(observation.get(key),actual.get(key)))throw DomainError.invalid("Applied trade observation subject differs");
  for(String key:List.of("startQuantity","quantity"))if(decimal(observation,key).compareTo(decimal(actual,key))!=0)throw DomainError.invalid("Applied trade observation physical range differs");
  if(!StockPrimitives.instant(observation.get("occurredAt")).equals(StockPrimitives.instant(actual.get("occurredAt"))))throw DomainError.invalid("Applied trade observation time differs");
  String canonical=actual.get("canonicalOccurrenceId").toString();evidence.requireCanonical(c,canonical,eventKind,actual.get("itemId").toString(),observation.get("ID").toString(),decimal(actual,"quantity"),actual.get("unit").toString());
  var movements=inventory.rows(c,"QuantityMovements").stream().filter(x->Objects.equals(actual.get("segmentId"),x.get("targetId"))&&canonical.equals(x.get("evidenceRef"))&&movementKind.equals(x.get("kind"))).toList();
  if(movements.size()!=1||decimal(movements.getFirst(),"quantity").compareTo(decimal(actual,"quantity"))!=0)throw DomainError.invalid("Applied physical movement required for reconciliation completion");
  duties.fence(c.organizationId(),actual.get("workId").toString());
  for(var root:duties.rows("Roots",c.organizationId()))if(dutyKind.equals(root.get("kind")))try{
   var scope=new com.fasterxml.jackson.databind.ObjectMapper().readTree(root.get("scopeJson").toString());
   if(observation.get("ID").equals(scope.path("domainSourceId").asText())&&actual.get("workId").equals(scope.path("originalWorkId").asText())&&Objects.equals("RETURN_RECONCILIATION".equals(dutyKind)?observation.get("rangeRootId"):observation.get("ID"),scope.path("physicalScopeId").asText()))service.applyTradeObservation(c,new Completion(root.get("ID").toString(),dutyKind,canonical,actual.get("ID").toString()));
  }catch(java.io.IOException e){throw DomainError.invalid("Invalid trade reconciliation scope");}
 }
 private static BigDecimal decimal(Map<String,Object> m,String key){return new BigDecimal(m.get(key).toString());}
 public static final class Completion {
  private final String rootId,kind,canonicalId,actualId;
  private Completion(String rootId,String kind,String canonicalId,String actualId){this.rootId=rootId;this.kind=kind;this.canonicalId=canonicalId;this.actualId=actualId;}
  public String rootId(){return rootId;}public String kind(){return kind;}public String canonicalId(){return canonicalId;}public String actualId(){return actualId;}
 }
}
