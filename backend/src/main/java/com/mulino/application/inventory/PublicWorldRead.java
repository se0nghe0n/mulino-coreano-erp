package com.mulino.application.inventory;

import com.mulino.application.core.*;
import com.mulino.application.trade.sales.SalesQueries;
import com.mulino.domain.inventory.InventoryRepository;
import com.mulino.domain.inventory.PhysicalRanges;
import com.mulino.domain.inventory.QualityRanges;
import com.mulino.domain.trade.sales.SalesRepository;
import com.mulino.domain.trade.purchase.PurchaseRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;

/** Read-only composition of visible physical facts and recorded execution relations.
 * No eligibility or goal judgment is inferred from a physical delivery. */
@Component
public class PublicWorldRead {
  private final InventoryRepository inventory;
  private final SalesRepository sales;
  private final PurchaseRepository purchase;
  private final SalesQueries salesRead;
  private final ReadAuthorizer auth;
  public PublicWorldRead(InventoryRepository inventory,SalesRepository sales,PurchaseRepository purchase,
      SalesQueries salesRead,ReadAuthorizer auth){
    this.inventory=inventory;this.sales=sales;this.purchase=purchase;this.salesRead=salesRead;this.auth=auth;
  }

  @SuppressWarnings("unchecked")
  public Map<String,Object> read(DomainContext c,Map<String,Object> scope,Map<String,Object> world,Instant readTime){
    String item=Objects.toString(scope.get("itemId"),null);
    if(item==null)throw DomainError.invalid("Public world requires an item scope");
    if(scope.get("organizationId")!=null&&!c.organizationId().equals(scope.get("organizationId")))throw DomainError.forbidden();
    var gaps=new TreeSet<String>();
    var works=(List<Map<String,Object>>)world.get("workReferences");
    if(works==null)throw new IllegalStateException("Authorized Work world required");
    var workIds=new HashSet<Object>();works.forEach(w->workIds.add(w.get("ID")));
    if(scope.get("workId")!=null&&!workIds.contains(scope.get("workId")))throw DomainError.forbidden();
    var allSegments=index(inventory.rows(c,"QuantitySegments").stream()
        .filter(r->item.equals(r.get("itemId"))&&at(r,"validFrom",c.asOf())).toList());
    var rawAllocations=inventory.rows(c,"SegmentAllocations").stream().filter(a->at(a,"createdAt",c.asOf())).toList();
    var contracts=salesRead.worldLines(c,scope);
    var lines=contracts.rows().stream().filter(l->workIds.contains(l.get("workId")))
        .filter(l->scope.get("placeId")==null||Objects.equals(scope.get("placeId"),l.get("destinationId"))
            ||rawAllocations.stream().anyMatch(a->Objects.equals(l.get("ID"),a.get("orderLineId"))
                &&allSegments.containsKey(a.get("segmentId"))&&Objects.equals(scope.get("placeId"),allSegments.get(a.get("segmentId")).get("placeId")))).toList();
    if(lines.size()!=contracts.rows().size())gaps.add("ORDER_CONTRACT_REQUIRES_VISIBLE_WORK_AND_PLACE_BINDING");
    if(!contracts.complete())gaps.add("PARTIAL_ORDER_LINE_READ_AUTHORITY");
    if((scope.containsKey("orderId")||scope.containsKey("salesLineId"))&&lines.isEmpty())throw DomainError.forbidden();
    var lineIds=new HashSet<Object>();lines.forEach(l->lineIds.add(l.get("ID")));
    if(scope.containsKey("orderId")||scope.containsKey("salesLineId")||scope.containsKey("customerId")){
      var lineWorks=new HashSet<Object>();lines.forEach(l->lineWorks.add(l.get("workId")));workIds.retainAll(lineWorks);
    }
    var allocations=rawAllocations.stream()
        .filter(r->at(r,"createdAt",c.asOf())&&allSegments.containsKey(r.get("segmentId"))&&matches(r,scope,"workId","customerId")
            &&(scope.get("salesLineId")==null||scope.get("salesLineId").equals(r.get("orderLineId"))))
        .filter(r->scope.get("orderId")==null||lineIds.contains(r.get("orderLineId"))).toList();
    boolean relationScoped=List.of("workId","customerId","orderId","salesLineId").stream().anyMatch(scope::containsKey);
    var related=new HashSet<Object>();allocations.forEach(a->related.add(a.get("segmentId")));
    var dispatches=inventory.rows(c,"Dispatches").stream().filter(r->item.equals(r.get("itemId"))&&at(r,"occurredAt",c.asOf())
        &&matches(r,scope,"workId","customerId","salesLineId")
        &&(scope.get("orderId")==null||lineIds.contains(r.get("salesLineId")))).toList();
    for(var d:dispatches){related.add(d.get("rangeRootId"));related.add(d.get("transitSegmentId"));}
    var delivered=sales.rows(c,"Deliveries").stream().filter(r->item.equals(r.get("itemId"))&&visibleTime(r,c)
        &&matches(r,scope,"workId","customerId","salesLineId")
        &&(scope.get("orderId")==null||lineIds.contains(r.get("salesLineId")))).toList();
    delivered.forEach(d->related.add(d.get("segmentId")));
    var edges=inventory.rows(c,"GenealogyEdges");
    if(relationScoped){
      // A shared ancestor is not an exclusive physical range. Use stored edge coordinates.
      for(var segment:allSegments.values()){
        var ranges=new ArrayList<QualityRanges.Range>();
        for(var a:allocations)ranges.addAll(project(edges,a,"segmentId",segment));
        for(var d:dispatches)ranges.addAll(project(edges,d,"rangeRootId",segment));
        if(!QualityRanges.union(ranges).isEmpty())related.add(segment.get("ID"));
      }
    }
    var candidates=allSegments.values().stream().filter(r->InventoryReadCandidates.matches(r,scope)&&(!relationScoped||related.contains(r.get("ID")))).toList();
    var segments=candidates.stream().filter(r->segmentAllowed(c,r,Objects.toString(scope.get("workId"),null))).sorted(order()).toList();
    var segmentIds=new HashSet<Object>();segments.forEach(s->segmentIds.add(s.get("ID")));
    boolean stockComplete=segments.size()==candidates.size();
    if(!stockComplete)gaps.add("PARTIAL_PHYSICAL_READ_AUTHORITY");
    if(relationScoped&&related.isEmpty())gaps.add("NO_RECORDED_PHYSICAL_EXECUTION_RELATION");
    var visibleAllocations=allocations.stream().filter(r->segmentIds.contains(r.get("segmentId"))&&workIds.contains(r.get("workId"))
        &&auth.permittedScopes(c,"getInventory",allocationScopes(r,allSegments.get(r.get("segmentId"))))).sorted(order()).toList();
    boolean allocationComplete=visibleAllocations.size()==allocations.stream().filter(a->segmentIds.contains(a.get("segmentId"))).count();
    if(!allocationComplete)gaps.add("PARTIAL_ALLOCATION_READ_AUTHORITY");
    var movements=new ArrayList<Map<String,Object>>();boolean movementComplete=true;
    var dispatchIds=new HashSet<Object>();var executionCommands=new HashSet<Object>();
    dispatches.forEach(d->{dispatchIds.add(d.get("ID"));executionCommands.add(d.get("commandId"));});
    for(var transfer:inventory.rows(c,"DeliveryTransfers"))if(dispatchIds.contains(transfer.get("dispatchId"))&&at(transfer,"occurredAt",c.asOf()))executionCommands.add(transfer.get("commandId"));
    for(var movement:inventory.rows(c,"QuantityMovements")){
      if(!at(movement,"occurredAt",c.asOf())||!segmentIds.contains(movement.get("sourceId"))&&!segmentIds.contains(movement.get("targetId")))continue;
      if(relationScoped&&Set.of("DISPATCH","DELIVERY").contains(movement.get("kind"))&&!executionCommands.contains(movement.get("commandId")))continue;
      // Both endpoints must be visible and authorized even for a place-scoped boundary crossing.
      boolean allowed=true;
      for(String endpoint:List.of("sourceId","targetId"))if(movement.get(endpoint)!=null){var s=allSegments.get(movement.get(endpoint));allowed&=s!=null&&segmentAllowed(c,s,Objects.toString(scope.get("workId"),null));}
      if(allowed)movements.add(movement);else movementComplete=false;
    }
    if(!movementComplete)gaps.add("PARTIAL_MOVEMENT_ENDPOINT_AUTHORITY");
    var visibleDeliveries=delivered.stream().filter(r->dispatches.stream().anyMatch(d->Objects.equals(d.get("ID"),r.get("dispatchId"))
        &&Objects.equals(d.get("workId"),r.get("workId"))&&Objects.equals(d.get("salesLineId"),r.get("salesLineId"))
        &&visibleAllocations.stream().anyMatch(a->Objects.equals(a.get("ID"),d.get("allocationId")))))
        .filter(r->workIds.contains(r.get("workId"))&&segmentIds.contains(r.get("segmentId"))
        &&matches(r,scope,"lotId","placeId")&&auth.permittedScopes(c,"getObject",salesScopes(r))).sorted(order()).toList();
    boolean deliveryComplete=visibleDeliveries.size()==delivered.stream().filter(r->matches(r,scope,"lotId","placeId")).count();
    if(!deliveryComplete)gaps.add("PARTIAL_DELIVERY_READ_AUTHORITY");
    var credits=new ArrayList<Map<String,Object>>();
    for(var credit:purchase.rows(c,"ReceiptCredits")){
      if(!visibleTime(credit,c))continue;
      var matching=purchase.rows(c,"OrderLines").stream().filter(l->credit.get("lineId").equals(l.get("ID"))&&item.equals(l.get("itemId"))&&visibleTime(l,c)
          &&matches(l,scope,"workId")&&(scope.get("placeId")==null||scope.get("placeId").equals(l.get("destinationId")))).toList();
      if(matching.size()!=1)continue;var line=matching.getFirst();
      if(scope.get("customerId")!=null||scope.get("salesLineId")!=null||scope.get("orderId")!=null)continue;
      // A purchase credit has no LOT binding; do not invent one from current inventory.
      if(scope.get("lotId")!=null){gaps.add("PURCHASE_CREDIT_LOT_BINDING_UNAVAILABLE");continue;}
      if(workIds.contains(line.get("workId"))&&auth.permittedScopes(c,"getObject",salesScopes(line)))credits.add(credit);
      else gaps.add("PARTIAL_PURCHASE_CREDIT_READ_AUTHORITY");
    }
    var ledger=new LinkedHashMap<String,Object>();ledger.put("segments",segments.stream().map(s->segmentDto(s,c)).toList());
    // Allocation state is mutable without a recorded-at revision history. Earlier cutoffs
    // cannot safely interpolate its current state; leave that section explicitly unknown.
    boolean historical=c.asOf().isBefore(readTime)||c.knownAt().isBefore(readTime);
    if(!historical)ledger.put("allocations",visibleAllocations.stream().map(PublicWorldRead::dto).toList());
    else gaps.add("HISTORICAL_ALLOCATION_STATE_UNAVAILABLE");
    ledger.put("movements",sorted(movements));ledger.put("deliveries",visibleDeliveries.stream().map(PublicWorldRead::dto).toList());
    ledger.put("receiptContributions",sorted(credits));
    var assessments=(List<Map<String,Object>>)world.get("assessmentReferences");
    if(assessments==null)throw new IllegalStateException("Recorded assessment world required");
    var boundAssessments=assessments.stream().filter(a->workIds.contains(a.get("workId"))).toList();
    var visibleAssessments=boundAssessments.stream().filter(a->auth.permittedScopes(c,"getAssessment",
        Map.of("TARGET",List.of(a.get("workId").toString()),"WORK",List.of(a.get("workId").toString()),"ITEM",List.of(item)))).sorted(order()).toList();
    if(visibleAssessments.size()!=boundAssessments.size())gaps.add("PARTIAL_ASSESSMENT_READ_AUTHORITY");
    ledger.put("assessments",visibleAssessments);
    var result=new LinkedHashMap<String,Object>();result.put("effectLedger",ledger);
    result.put("deliveryObservations",salesRead.worldObservations(c,scope).stream().filter(row->workIds.contains(row.get("workId"))).toList());
    var unit=inventory.object(c,"TradeItems",item).get("baseUnit").toString();
    var places=index(inventory.rows(c,"Places"));
    boolean placeKindsKnown=segments.stream().allMatch(s->places.containsKey(s.get("placeId"))&&places.get(s.get("placeId")).get("kind")!=null);
    if(!placeKindsKnown)gaps.add("PHYSICAL_PLACE_KIND_UNAVAILABLE");
    var transit=segments.stream().filter(s->InventoryReadCandidates.active(s,c)&&"TRANSIT".equals(places.getOrDefault(s.get("placeId"),Map.of()).get("kind"))).toList();
    if(stockComplete&&placeKindsKnown){
      quantity(result,"warehouseHeldQuantity",segments.stream().filter(s->InventoryReadCandidates.active(s,c)&&"INTERNAL_STORAGE".equals(places.getOrDefault(s.get("placeId"),Map.of()).get("kind"))).toList(),"quantity",unit,gaps);
      if(!relationScoped)quantity(result,"inTransitQuantity",transit,"quantity",unit,gaps);
      else if(transit.stream().allMatch(s->unit.equals(s.get("unit")))&&dispatches.stream().allMatch(d->completeRange(edges,d,allSegments.values(),unit,c))){
        BigDecimal total=BigDecimal.ZERO;for(var segment:transit){var ranges=new ArrayList<QualityRanges.Range>();for(var d:dispatches)ranges.addAll(project(edges,d,"rangeRootId",segment));total=total.add(QualityRanges.quantity(QualityRanges.union(ranges)));}result.put("inTransitQuantity",total);
      }else gaps.add("SCOPED_TRANSIT_RANGE_UNAVAILABLE");
    }
    if(deliveryComplete)quantity(result,"deliveredQuantity",visibleDeliveries,"quantity",unit,gaps);
    if(stockComplete&&placeKindsKnown&&movementComplete)quantity(result,"warehouseDispatchQuantity",movements.stream().filter(m->"DISPATCH".equals(m.get("kind")))
        .filter(m->{var source=allSegments.get(m.get("sourceId"));return source!=null&&"INTERNAL_STORAGE".equals(places.getOrDefault(source.get("placeId"),Map.of()).get("kind"));}).toList(),"quantity",unit,gaps);
    result.put("allocationRelations",allocationRelations(visibleAllocations));
    if(!historical&&allocationComplete){
      quantity(result,"executableReservedQuantity",visibleAllocations.stream().filter(a->"EXECUTABLE".equals(a.get("state"))).toList(),"quantity",unit,gaps);
      allocationSummary(result,visibleAllocations,gaps);
    }
    result.put("orderContracts",lines);
    if(contracts.complete()&&lines.size()==1)result.put("deliveryEndpoint",lines.getFirst().get("deliveryEndpoint"));
    else if(!lines.isEmpty())gaps.add("ORDER_ENDPOINT_REQUIRES_UNIQUE_LINE");
    deliveryAssessments(result,world,works,visibleAssessments,workIds,gaps);
    gaps.add("NORMAL_SALE_AUTHORIZATION_REQUIRES_EXECUTION_ASSESSMENT");
    result.put("publicReadGaps",List.copyOf(gaps));return result;
  }

  /** Only an immutable assessment of the current delivery GoalVersion can supply a summary.
   * A physical delivery, its recognized quantity, and a future sale authorization remain separate. */
  @SuppressWarnings("unchecked")
  private static void deliveryAssessments(Map<String,Object> result,Map<String,Object> world,List<Map<String,Object>> works,List<Map<String,Object>> assessments,Set<Object> workIds,Set<String> gaps){
    var goals=(List<Map<String,Object>>)world.getOrDefault("goalReferences",List.of());var selected=new ArrayList<Map<String,Object>>();
    for(var work:works){
      if(!workIds.contains(work.get("ID")))continue;
      var goal=goals.stream().filter(g->Objects.equals(work.get("currentGoalVersionId"),g.get("ID"))&&Objects.equals(work.get("ID"),g.get("workId"))&&"DELIVERED".equals(g.get("endpoint"))).findFirst();
      if(goal.isEmpty())continue;
      var candidates=assessments.stream().filter(a->Objects.equals(work.get("ID"),a.get("workId"))&&Objects.equals(goal.get().get("ID"),a.get("goalId"))).toList();
      var heads=candidates.stream().filter(a->candidates.stream().noneMatch(next->Objects.equals(a.get("ID"),next.get("previousAssessmentId")))).toList();
      if(heads.size()!=1||Boolean.TRUE.equals(work.get("pendingInvalidation"))){gaps.add("DELIVERY_ASSESSMENT_PENDING_OR_UNAVAILABLE");continue;}
      var row=new LinkedHashMap<String,Object>(heads.getFirst());row.put("goalKind","DELIVERY");row.put("current",true);row.put("result",row.get("outcome"));selected.add(row);
    }
    selected.sort(order());result.put("deliveryAssessments",List.copyOf(selected));
    if(selected.size()==1)result.put("deliveryAssessment",selected.getFirst().get("result"));
    else if(selected.size()>1)gaps.add("DELIVERY_ASSESSMENT_REQUIRES_UNIQUE_GOAL");
  }

  private static List<QualityRanges.Range> project(List<Map<String,Object>> edges,Map<String,Object> binding,String root,Map<String,Object> segment){
    if(binding.get(root)==null||binding.get("startQuantity")==null||binding.get("quantity")==null)return List.of();
    if(segment.get("quantity")==null)return List.of();
    return QualityRanges.intersect(PhysicalRanges.project(edges,binding.get(root).toString(),segment.get("ID").toString(),new BigDecimal(binding.get("startQuantity").toString()),new BigDecimal(binding.get("quantity").toString())),
        List.of(new QualityRanges.Range(BigDecimal.ZERO,new BigDecimal(segment.get("quantity").toString()))));
  }
  private static boolean completeRange(List<Map<String,Object>> edges,Map<String,Object> dispatch,Collection<Map<String,Object>> segments,String unit,DomainContext c){
    if(dispatch.get("startQuantity")==null||dispatch.get("quantity")==null||!unit.equals(dispatch.get("unit")))return false;
    BigDecimal mapped=BigDecimal.ZERO;
    for(var segment:segments)if(InventoryReadCandidates.active(segment,c)){
      var ranges=project(edges,dispatch,"rangeRootId",segment);
      if(!ranges.isEmpty()&&!unit.equals(segment.get("unit")))return false;
      mapped=mapped.add(QualityRanges.quantity(ranges));
    }
    return mapped.compareTo(new BigDecimal(dispatch.get("quantity").toString()))==0;
  }
  private boolean segmentAllowed(DomainContext c,Map<String,Object> segment,String work){return auth.permittedScopes(c,"getInventory",InventoryReadCandidates.segmentScopes(segment,work));}
  private Map<String,Collection<String>> allocationScopes(Map<String,Object> a,Map<String,Object> segment){return InventoryReadCandidates.segmentScopes(segment,Objects.toString(a.get("workId"),null));}
  private Map<String,List<String>> salesScopes(Map<String,Object> r){var s=new LinkedHashMap<String,List<String>>();s.put("TARGET",List.of(r.get("ID").toString()));for(var pair:Map.of("workId","WORK","itemId","ITEM","placeId","PLACE","destinationId","PLACE").entrySet())if(r.get(pair.getKey())!=null)s.put(pair.getValue(),List.of(r.get(pair.getKey()).toString()));return s;}
  private static List<Map<String,Object>> allocationRelations(List<Map<String,Object>> rows){return rows.stream().map(a->{var r=new LinkedHashMap<String,Object>();for(String k:List.of("ID","predecessorId","orderLineId","workId","customerId","segmentId"))if(a.get(k)!=null)r.put(k,a.get(k));return (Map<String,Object>)r;}).toList();}
  private static void allocationSummary(Map<String,Object> result,List<Map<String,Object>> rows,Set<String> gaps){
    if(rows.size()==1){result.put("allocationStatus",rows.getFirst().get("state"));return;}
    if(rows.size()==2){var successors=rows.stream().filter(r->r.get("predecessorId")!=null&&rows.stream().anyMatch(p->Objects.equals(p.get("ID"),r.get("predecessorId"))&&Objects.equals(p.get("orderLineId"),r.get("orderLineId")))).toList();
      if(successors.size()==1){var next=successors.getFirst();var old=rows.stream().filter(r->Objects.equals(r.get("ID"),next.get("predecessorId"))).findFirst().orElseThrow();if("REPLACED".equals(old.get("state"))){result.put("oldAllocationStatus",old.get("state"));result.put("newAllocationStatus",next.get("state"));return;}}
    }
    if(!rows.isEmpty())gaps.add("ALLOCATION_STATUS_REQUIRES_UNIQUE_LINEAGE");
  }
  private static void quantity(Map<String,Object> out,String name,List<Map<String,Object>> rows,String field,String unit,Set<String> gaps){
    if(rows.stream().anyMatch(r->r.get(field)==null||!unit.equals(r.get("unit")))){gaps.add(name+":QUANTITY_OR_UNIT_UNAVAILABLE");return;}
    out.put(name,rows.stream().map(r->new BigDecimal(r.get(field).toString())).reduce(BigDecimal.ZERO,BigDecimal::add));
  }
  private static Map<String,Object> segmentDto(Map<String,Object> r,DomainContext c){var dto=new LinkedHashMap<String,Object>();for(String k:List.of("organizationId","ID","itemId","lotId","quantity","unit","placeId","custodianId","ownerId","controlScope","validFrom","recordedAt"))dto.put(k,r.get(k));
    if(r.get("retirementRecordedAt")!=null&&at(r,"retirementRecordedAt",c.knownAt())){dto.put("retiredAt",r.get("retiredAt"));dto.put("retirementRecordedAt",r.get("retirementRecordedAt"));}return dto;}
  private static boolean matches(Map<String,Object> r,Map<String,Object> scope,String...keys){for(String k:keys)if(scope.get(k)!=null&&!Objects.equals(scope.get(k),r.get(k)))return false;return true;}
  private static boolean visibleTime(Map<String,Object> r,DomainContext c){return at(r,"recordedAt",c.knownAt())&&at(r,"effectiveAt",c.asOf())&&at(r,"occurredAt",c.asOf());}
  private static boolean at(Map<String,Object> r,String key,Instant cutoff){Object v=r.get(key);return v==null||!StockTime.instant(v).isAfter(cutoff);}
  private static Map<Object,Map<String,Object>> index(List<Map<String,Object>> rows){var out=new LinkedHashMap<Object,Map<String,Object>>();rows.stream().sorted(order()).forEach(r->out.put(r.get("ID"),r));return out;}
  private static Comparator<Map<String,Object>> order(){return Comparator.comparing(r->r.get("ID").toString());}
  private static Map<String,Object> dto(Map<String,Object> row){return new LinkedHashMap<>(row);}
  private static List<Map<String,Object>> sorted(List<Map<String,Object>> rows){return rows.stream().sorted(order()).map(PublicWorldRead::dto).toList();}
  private static final class StockTime {static Instant instant(Object v){return v instanceof Instant i?i:v instanceof java.sql.Timestamp t?t.toInstant():Instant.parse(v.toString());}}
}
