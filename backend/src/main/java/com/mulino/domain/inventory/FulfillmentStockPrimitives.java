package com.mulino.domain.inventory;

import com.mulino.application.core.*;
import com.mulino.application.trade.DispatchCargoPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;
import static com.mulino.domain.inventory.StockPrimitives.*;

/** All sales effects join the common gateway transaction and its exact stock fences. */
@Component
public final class FulfillmentStockPrimitives implements DispatchCargoPort {
 private final InventoryRepository r;private final StockPrimitives stock;private final PhysicalRanges ranges;private final com.mulino.application.quality.QualityEligibility quality;
 public FulfillmentStockPrimitives(InventoryRepository r,StockPrimitives stock,PhysicalRanges ranges,com.mulino.application.quality.QualityEligibility quality){this.r=r;this.stock=stock;this.ranges=ranges;this.quality=quality;}
 public Map<String,Object> reserve(DomainContext c,Map<String,Object> s,String line,String customer,String work,BigDecimal start,BigDecimal q,Instant boundary,String command,String predecessor){
  var row=row(c,id(),c.knownAt());row.putAll(Map.of("rootId",s.get("ID"),"segmentId",s.get("ID"),"orderLineId",line,"customerId",customer,"workId",work,"startQuantity",start,"quantity",q,"unit",s.get("unit"),"state","EXECUTABLE"));row.put("authorizationActorId",c.actorId());row.put("action","SELL");row.put("commandId",command);if(boundary!=null)row.put("nextValidityBoundary",boundary);if(predecessor!=null)row.put("predecessorId",predecessor);r.insert("SegmentAllocations",row);return row;
 }
 public void transition(DomainContext c,String allocation,String state){var a=r.current(c,"SegmentAllocations",allocation);if(!Set.of("EXECUTABLE","SUSPENDED").contains(a.get("state")))throw DomainError.invalid("Terminal allocation cannot transition");r.update(c,"SegmentAllocations",allocation,Map.of("state",state,"revision",((Number)a.get("revision")).intValue()+1));}
 public void pick(DomainContext c,String allocation){var a=r.current(c,"SegmentAllocations",allocation);if(a.get("pickedAt")!=null)throw DomainError.invalid("Allocation already picked");r.update(c,"SegmentAllocations",allocation,Map.of("pickedAt",c.knownAt(),"revision",((Number)a.get("revision")).intValue()+1));}
 public Map<String,Object> dispatch(DomainContext c,String allocationId,String transit,String destination,Instant at,String evidence,String command){
  var a=r.current(c,"SegmentAllocations",allocationId);var s=stock.leaf(c,(String)a.get("segmentId"),at);if(!"EXECUTABLE".equals(a.get("state"))||a.get("pickedAt")==null)throw DomainError.invalid("Picked executable allocation required");
  if(!"TRANSIT".equals(r.current(c,"Places",transit).get("kind")))throw DomainError.invalid("Transit place required");
  transition(c,allocationId,"CONSUMED");BigDecimal start=(BigDecimal)a.get("startQuantity"),q=(BigDecimal)a.get("quantity");
  String leaf=stock.transferRange(c,(String)s.get("ID"),start,q,transit,at,evidence,command,"DISPATCH");
  var d=row(c,id(),at);String cargo=id();d.putAll(Map.of("cargoScopeId",cargo,"allocationId",allocationId,"salesLineId",a.get("orderLineId"),"customerId",a.get("customerId"),"workId",a.get("workId"),"itemId",s.get("itemId"),"lotId",s.get("lotId"),"rangeRootId",s.get("ID"),"startQuantity",start,"quantity",q));d.putAll(Map.of("unit",s.get("unit"),"transitSegmentId",leaf,"destinationId",destination,"occurredAt",at,"commandId",command,"evidenceRef",evidence));r.insert("Dispatches",d);
  var scope=row(c,cargo,at);scope.putAll(Map.of("dispatchId",d.get("ID"),"rangeRootId",s.get("ID"),"startQuantity",start,"quantity",q,"unit",s.get("unit"),"segmentId",leaf,"customerId",a.get("customerId"),"workId",a.get("workId"),"commandId",command));r.insert("CargoScopes",scope);return d;
 }
 public Map<String,Object> requireDispatch(DomainContext c,String id){var d=r.object(c,"Dispatches",id);if(!"CONSUMED".equals(r.current(c,"SegmentAllocations",(String)d.get("allocationId")).get("state")))throw DomainError.invalid("Dispatch allocation history inconsistent");return d;}
 public Map<String,Object> requireCargoScope(DomainContext c,String id){return r.object(c,"CargoScopes",id);}
 /** Observed physical arrival consumes transit once, even if sale authority was subsequently revoked. */
 public Map<String,Object> observeDelivery(DomainContext c,String dispatchId,String canonical,BigDecimal start,BigDecimal q,Instant at,String command){
  var d=requireDispatch(c,dispatchId);r.fence(c,List.of("inventory/dispatch/"+dispatchId,"inventory/control/"+r.current(c,"QuantitySegments",(String)d.get("transitSegmentId")).get("controlScope")));d=requireDispatch(c,dispatchId);
  BigDecimal from=(BigDecimal)d.get("startQuantity"),end=from.add((BigDecimal)d.get("quantity"));if(start.compareTo(from)<0||q.signum()<=0||start.add(q).compareTo(end)>0||at.isBefore(instant(d.get("occurredAt")))||at.isAfter(c.knownAt()))throw DomainError.invalid("Delivery outside dispatched exact range or time");
  var wanted=new QualityRanges.Range(start,start.add(q));
  for(var old:r.currentRows(c,"DeliveryTransfers"))if(dispatchId.equals(old.get("dispatchId"))){if(canonical.equals(old.get("canonicalId"))){if(start.compareTo((BigDecimal)old.get("startQuantity"))!=0||q.compareTo((BigDecimal)old.get("quantity"))!=0||!at.equals(instant(old.get("occurredAt"))))throw DomainError.invalid("Canonical delivery scope changed");return old;}if(!QualityRanges.intersect(List.of(wanted),List.of(new QualityRanges.Range((BigDecimal)old.get("startQuantity"),((BigDecimal)old.get("startQuantity")).add((BigDecimal)old.get("quantity"))))).isEmpty())throw DomainError.invalid("Delivery range already observed");}
  String root=(String)d.get("rangeRootId");Map<String,Object> selected=null;QualityRanges.Range local=null;
  for(var leaf:r.currentRows(c,"QuantitySegments"))if(leaf.get("retiredAt")==null&&"TRANSIT".equals(r.current(c,"Places",(String)leaf.get("placeId")).get("kind"))){var mapped=ranges.project(c,root,(String)leaf.get("ID"),start,q);if(QualityRanges.quantity(mapped).compareTo(q)==0&&mapped.size()==1){if(selected!=null)throw DomainError.invalid("Ambiguous transit identity");selected=leaf;local=mapped.getFirst();}}
  if(selected==null)throw new DomainError("HELD","PHYSICAL_SCOPE_UNCERTAIN","Dispatched physical range requires reconciliation");
  // Legitimacy is judged at the actual delivery instant on the transit leaf that held the range then. A later partial delivery
  // confirmed first may already have split the transit leaf after `at` (plan §6, D06: late facts are kept).
  Map<String,Object> then=selected;QualityRanges.Range thenLocal=local;
  if(instant(selected.get("validFrom")).isAfter(at))for(var leaf:r.currentRows(c,"QuantitySegments"))if(!instant(leaf.get("validFrom")).isAfter(at)&&(leaf.get("retiredAt")==null||instant(leaf.get("retiredAt")).isAfter(at))&&"TRANSIT".equals(r.current(c,"Places",(String)leaf.get("placeId")).get("kind"))){var mapped=ranges.project(c,root,(String)leaf.get("ID"),start,q);if(QualityRanges.quantity(mapped).compareTo(q)==0&&mapped.size()==1){then=leaf;thenLocal=mapped.getFirst();}}
  // A sale delivery is legitimate only where SELL and DISPATCH both still held for this customer (plan §6 사실 기록과 실행 권한:
  // a delivery after a recall, a SELL withdrawal or a permission expiry keeps fact, violation and response duty).
  var historical=new DomainContext(c.organizationId(),c.actorId(),c.stableRequestOwner(),at,c.knownAt());var sale=quality.assess(historical,then,"SELL",(String)d.get("customerId"));var dispatch=quality.assess(historical,then,"DISPATCH",(String)d.get("customerId"));
  var legitimateLocal=QualityRanges.intersect(QualityRanges.intersect(sale.ranges(),dispatch.ranges()),List.of(thenLocal));BigDecimal legitimate=QualityRanges.quantity(legitimateLocal);
  BigDecimal offset=start.subtract(thenLocal.start());var legitimateRanges=QualityRanges.union(legitimateLocal).stream().map(x->new QualityRanges.Range(x.start().add(offset),x.end().add(offset))).toList();
  var reasons=new TreeSet<String>(sale.unknowns());reasons.addAll(dispatch.unknowns());
  // Delivery hands the goods to the customer: our warehouse custody ends, ownership is not transferred (plan §4.2). The ledger
  // cannot date a child before its parent leaf, so an out-of-order confirmation transfers at the leaf's start while the Delivery
  // keeps the true occurrence time.
  Instant transferAt=instant(selected.get("validFrom")).isAfter(at)?instant(selected.get("validFrom")):at;
  String leaf=stock.transferRangeReleasingCustody(c,(String)selected.get("ID"),local.start(),q,(String)d.get("destinationId"),transferAt,canonical,command,"DELIVERY");
  var moved=row(c,id(),at);moved.putAll(Map.of("dispatchId",dispatchId,"canonicalId",canonical,"startQuantity",start,"quantity",q,"unit",d.get("unit"),"segmentId",leaf,"occurredAt",at,"commandId",command));moved.put("legitimateQuantity",legitimate);r.insert("DeliveryTransfers",moved);var result=new LinkedHashMap<String,Object>(moved);result.put("legitimateQuantity",legitimate);result.put("legitimateRanges",legitimateRanges);result.put("restrictionReasons",List.copyOf(reasons));return result;
 }
}
