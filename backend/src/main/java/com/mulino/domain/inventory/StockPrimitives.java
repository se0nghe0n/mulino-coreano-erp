package com.mulino.domain.inventory;

import com.mulino.application.core.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;

/** Bounded physical effects. Call only inside the common gateway transaction after fences/guard. */
@Component
public final class StockPrimitives {
  private final InventoryRepository repository;
  public StockPrimitives(InventoryRepository repository) {this.repository=repository;}
  public Map<String,Object> leaf(DomainContext c,String id,Instant at) {
    if(at.isAfter(c.knownAt()))throw DomainError.invalid("Future actual occurrence rejected");
    var s=repository.current(c,"QuantitySegments",id);
    if(s.get("retiredAt")!=null || instant(s.get("validFrom")).isAfter(at)) throw conflict("Physical parent already consumed or not effective");
    return s;
  }
  public List<String> fences(Map<String,Object> s) {
    return List.of("inventory/segment/"+s.get("ID"),"inventory/control/"+s.get("controlScope"),"inventory/item/"+s.get("itemId"),"inventory/place/"+s.get("placeId"));
  }
  public BigDecimal quantity(DomainContext c,Map<String,Object> s,Object value,String unit) {
    var item=repository.current(c,"TradeItems",(String)s.get("itemId"));
    if(!Objects.equals(unit,s.get("unit"))||!Objects.equals(unit,item.get("baseUnit"))) throw DomainError.invalid("Base unit mismatch");
    return InventoryQuantity.parse(value,((Number)item.get("decimalPlaces")).intValue());
  }
  public List<String> split(DomainContext c,String parent,List<String> values,String unit,Instant at,String evidence,String command) {
    lockSources(c,List.of(parent));var s=leaf(c,parent,at);
    if(values.size()<2||values.size()>100)throw DomainError.invalid("Split requires 2..100 children");
    var amounts=values.stream().map(v->quantity(c,s,v,unit)).toList();
    if(amounts.stream().reduce(BigDecimal.ZERO,BigDecimal::add).compareTo(amount(s))!=0)throw DomainError.invalid("Split must conserve full parent");
    return replace(c,List.of(s),amounts,(String)s.get("placeId"),at,evidence,command,"SPLIT");
  }
  public String merge(DomainContext c,List<String> parents,Instant at,String evidence,String command) {
    if(parents.size()<2||parents.size()>100||new HashSet<>(parents).size()!=parents.size()) throw DomainError.invalid("Distinct merge parents required");
    lockSources(c,parents);var sources=parents.stream().sorted().map(id->leaf(c,id,at)).toList();var first=sources.getFirst();
    for(var s:sources) for(String key:List.of("itemId","lotId","unit","placeId","controlScope","custodianId","ownerId","identificationStatus"))
      if(!Objects.equals(first.get(key),s.get(key)))throw DomainError.invalid("Incompatible merge "+key);
    var memberships=repository.currentRows(c,"LogisticsMemberships");
    var currentContainers=new HashSet<String>();
    for(var source:sources) {
      var active=memberships.stream().filter(m->source.get("ID").equals(m.get("segmentId"))&&!instant(m.get("validFrom")).isAfter(at)&&(m.get("validUntil")==null||at.isBefore(instant(m.get("validUntil"))))).map(m->(String)m.get("logisticsUnitId")).toList();
      currentContainers.add(active.isEmpty()?"UNCONTAINED":active.getFirst());
    }
    if(currentContainers.size()>1)throw DomainError.invalid("Resolve incompatible logistics membership before merge");
    BigDecimal total=sources.stream().map(StockPrimitives::amount).reduce(BigDecimal.ZERO,BigDecimal::add);
    quantity(c,first,total.toPlainString(),(String)first.get("unit"));
    return replace(c,sources,List.of(total),(String)first.get("placeId"),at,evidence,command,"MERGE").getFirst();
  }
  public String moveInternal(DomainContext c,String parent,String destination,Instant at,String evidence,String command) {
    lockSources(c,List.of(parent));var s=leaf(c,parent,at);
    var from=repository.current(c,"Places",(String)s.get("placeId"));var to=repository.current(c,"Places",destination);
    if(!"INTERNAL_STORAGE".equals(from.get("kind"))||!"INTERNAL_STORAGE".equals(to.get("kind")) || Objects.equals(from.get("ID"),to.get("ID")))throw DomainError.invalid("Configured distinct internal storage places required");
    if(!repository.internalCustodian(c,(String)s.get("custodianId")))throw DomainError.invalid("Confirmed internal custody required");
    return replace(c,List.of(s),List.of(amount(s)),destination,at,evidence,command,"INTERNAL_MOVE").getFirst();
  }
  public String stocktake(DomainContext c,String parent,Object value,String unit,Instant at,String evidence,String command) {
    lockSources(c,List.of(parent));var s=leaf(c,parent,at);BigDecimal observed=nonNegative(c,s,value,unit);
    var r=row(c,id(),at);r.put("segmentId",parent);r.put("observedQuantity",observed);r.put("unit",unit);r.put("occurredAt",at);r.put("evidenceRef",evidence);r.put("commandId",command);
    repository.insert("Stocktakes",r);return (String)r.get("ID");
  }
  /** Approved decreases consume the leaf. Observation alone never calls this. */
  public List<String> decrease(DomainContext c,String parent,Object value,String unit,Instant at,String evidence,String command,String kind) {
    if(!Set.of("DISPOSE","ADJUST_DECREASE").contains(kind))throw DomainError.invalid("Unsupported decrease");
    lockSources(c,List.of(parent));var s=leaf(c,parent,at);BigDecimal decrease=quantity(c,s,value,unit),remaining=amount(s).subtract(decrease);
    if(remaining.signum()<0)throw DomainError.invalid("Decrease exceeds physical leaf");
    if(kind.equals("DISPOSE")&&!repository.currentRows(c,"SegmentAllocations").stream().filter(a->parent.equals(a.get("segmentId"))).noneMatch(a->Set.of("EXECUTABLE","SUSPENDED").contains(a.get("state"))))throw conflict("Resolve allocation responsibility before disposing stock");
    List<String> children=replace(c,List.of(s),remaining.signum()==0?List.of():List.of(remaining),(String)s.get("placeId"),at,evidence,command,kind);
    movement(c,s,null,decrease,kind,at,evidence,command);return children;
  }
  public List<String> adjust(DomainContext c,String parent,String stocktakeId,Object value,String unit,String direction,Instant at,String evidence,String command,String reason) {
    var keys=new TreeSet<>(fences(repository.current(c,"QuantitySegments",parent)));keys.add("inventory/stocktake/"+stocktakeId);repository.fence(c,keys);
    var s=leaf(c,parent,at);var count=repository.current(c,"Stocktakes",stocktakeId);var delta=quantity(c,s,value,unit);
    if(repository.currentRows(c,"StockAdjustments").stream().anyMatch(a->stocktakeId.equals(a.get("stocktakeId"))))throw conflict("Stocktake difference already applied");
    if(at.isBefore(instant(count.get("occurredAt"))))throw DomainError.invalid("Adjustment precedes stocktake occurrence");
    BigDecimal difference=((BigDecimal)count.get("observedQuantity")).subtract(amount(s));
    if(!parent.equals(count.get("segmentId"))||!unit.equals(count.get("unit"))||difference.abs().compareTo(delta)!=0||difference.signum()!=(direction.equals("INCREASE")?1:-1))throw DomainError.invalid("Adjustment must reconcile this stocktake difference");
    var r=row(c,id(),at);r.put("stocktakeId",stocktakeId);r.put("segmentId",parent);r.put("direction",direction);r.put("quantity",delta);r.put("unit",unit);r.put("occurredAt",at);r.put("reason",reason);r.put("evidenceRef",evidence);r.put("commandId",command);repository.insert("StockAdjustments",r);
    return direction.equals("INCREASE")?List.of(increase(c,parent,value,unit,at,evidence,command)):decrease(c,parent,value,unit,at,evidence,command,"ADJUST_DECREASE");
  }
  /** Explicit approved upward correction keeps old scope and identifies the excess as a new leaf. */
  private String increase(DomainContext c,String parent,Object value,String unit,Instant at,String evidence,String command) {
    lockSources(c,List.of(parent));var s=leaf(c,parent,at);var delta=quantity(c,s,value,unit);
    BigDecimal corrected=amount(s).add(delta);quantity(c,s,corrected.toPlainString(),unit);
    var extra=child(c,s,corrected,(String)s.get("placeId"),at,evidence);repository.insert("QuantitySegments",extra);
    repository.update(c,"QuantitySegments",parent,Map.of("retiredAt",at,"retirementRecordedAt",c.knownAt(),"revision",((Number)s.get("revision")).intValue()+1));
    edge(c,s,extra,amount(s),BigDecimal.ZERO,BigDecimal.ZERO,"ADJUST_RETAINED","UNCERTAIN_MIXTURE".equals(s.get("mixtureStatus")),at,evidence,command);
    movement(c,null,extra,delta,"ADJUST_INCREASE",at,evidence,command);transferAllocations(c,s,List.of(extra),at,command);closeMembership(c,s,List.of(extra),at);return (String)extra.get("ID");
  }
  /** Relocates an identified interval and retains exact prefix/suffix identities. */
  public String transferRange(DomainContext c,String segmentId,BigDecimal start,BigDecimal q,String destination,Instant at,String evidence,String command,String kind){
    return replaceRange(c,segmentId,start,q,destination,at,evidence,command,kind,false);
  }
  public void disposeRange(DomainContext c,String segmentId,BigDecimal start,BigDecimal q,Instant at,String evidence,String command){
    replaceRange(c,segmentId,start,q,null,at,evidence,command,"DISPOSE",true);
  }
  private String replaceRange(DomainContext c,String segmentId,BigDecimal start,BigDecimal q,String destination,Instant at,String evidence,String command,String kind,boolean discard){
    lockSources(c,List.of(segmentId));var source=leaf(c,segmentId,at);quantity(c,source,q.toPlainString(),(String)source.get("unit"));
    if(start.signum()<0||start.add(q).compareTo(amount(source))>0||"UNCERTAIN_MIXTURE".equals(source.get("mixtureStatus")))throw DomainError.invalid("Identified exact physical interval required");
    if(destination!=null)repository.current(c,"Places",destination);
    var children=new ArrayList<Map<String,Object>>();String selected=null;
    BigDecimal end=start.add(q);var intervals=new ArrayList<QualityRanges.Range>();if(start.signum()>0)intervals.add(new QualityRanges.Range(BigDecimal.ZERO,start));if(!discard)intervals.add(new QualityRanges.Range(start,end));if(end.compareTo(amount(source))<0)intervals.add(new QualityRanges.Range(end,amount(source)));
    for(var range:intervals){boolean chosen=!discard&&range.start().compareTo(start)==0&&range.end().compareTo(end)==0;var child=child(c,source,range.end().subtract(range.start()),chosen?destination:(String)source.get("placeId"),at,evidence);repository.insert("QuantitySegments",child);children.add(child);edge(c,source,child,amount(child),range.start(),BigDecimal.ZERO,kind,false,at,evidence,command);if(chosen)selected=(String)child.get("ID");}
    repository.update(c,"QuantitySegments",segmentId,Map.of("retiredAt",at,"retirementRecordedAt",c.knownAt(),"revision",((Number)source.get("revision")).intValue()+1));
    transferAllocations(c,source,children,at,command);closeMembership(c,source,children,at);if(discard)movement(c,source,null,q,kind,at,evidence,command);return selected;
  }
  private void lockSources(DomainContext c,List<String> ids) {
    var keys=new TreeSet<String>();
    ids.forEach(id->keys.addAll(fences(repository.current(c,"QuantitySegments",id))));
    repository.fence(c,keys);
  }
  private BigDecimal nonNegative(DomainContext c,Map<String,Object> s,Object value,String unit) {
    if(value instanceof String text&&text.matches("0(?:\\.0{1,12})?")) {
      // Validate item scale even for observed zero.
      quantity(c,s,text.replaceFirst("^0","1"),unit);return new BigDecimal(text);
    }
    return quantity(c,s,value,unit);
  }
  private List<String> replace(DomainContext c,List<Map<String,Object>> sources,List<BigDecimal> amounts,String place,Instant at,String evidence,String command,String kind) {
    var first=sources.getFirst();boolean uncertain=sources.stream().anyMatch(s->"UNCERTAIN_MIXTURE".equals(s.get("mixtureStatus")));
    var children=new ArrayList<Map<String,Object>>();
    for(BigDecimal q:amounts) {var child=child(c,first,q,place,at,evidence);if(uncertain)child.put("mixtureStatus","UNCERTAIN_MIXTURE");children.add(child);repository.insert("QuantitySegments",child);}
    for(var source:sources) {
      repository.update(c,"QuantitySegments",(String)source.get("ID"),Map.of("retiredAt",at,"retirementRecordedAt",c.knownAt(),"revision",((Number)source.get("revision")).intValue()+1));
      if(sources.size()==1) {BigDecimal offset=BigDecimal.ZERO;for(var target:children){edge(c,source,target,amount(target),offset,BigDecimal.ZERO,kind,uncertain,at,evidence,command);offset=offset.add(amount(target));}}
      else {BigDecimal offset=BigDecimal.ZERO;for(var prior:sources){if(prior==source)break;offset=offset.add(amount(prior));}edge(c,source,children.getFirst(),amount(source),BigDecimal.ZERO,offset,kind,uncertain,at,evidence,command);}
      transferAllocations(c,source,children,at,command);
      closeMembership(c,source,children,at);
    }
    return children.stream().map(s->(String)s.get("ID")).toList();
  }
  private void transferAllocations(DomainContext c,Map<String,Object> source,List<Map<String,Object>> children,Instant at,String command) {
    var old=repository.currentRows(c,"SegmentAllocations").stream().filter(a->source.get("ID").equals(a.get("segmentId"))&&Set.of("EXECUTABLE","SUSPENDED").contains(a.get("state"))).sorted(Comparator.comparing(a->(String)a.get("ID"))).toList();
    BigDecimal legacyCursor=BigDecimal.ZERO;
    for(var a:old) {
      BigDecimal start=a.get("startQuantity") instanceof BigDecimal x?x:legacyCursor,q=(BigDecimal)a.get("quantity");legacyCursor=start.add(q);
      BigDecimal retained=BigDecimal.ZERO;
      for(var child:children) for(var range:PhysicalRanges.project(repository.currentRows(c,"GenealogyEdges"),(String)source.get("ID"),(String)child.get("ID"),start,q)) {
        var next=row(c,id(),at);for(String key:List.of("rootId","orderLineId","unit","state","workId","authorizationActorId","action","customerId","nextValidityBoundary","suspendedAt","suspensionReason"))if(a.get(key)!=null)next.put(key,a.get(key));
        BigDecimal count=range.end().subtract(range.start());next.put("segmentId",child.get("ID"));next.put("startQuantity",range.start());next.put("quantity",count);next.put("predecessorId",a.get("ID"));next.put("commandId",command);repository.insert("SegmentAllocations",next);retained=retained.add(count);
      }
      if(retained.compareTo(q)<0){
        // The missing physical part remains a suspended obligation; it cannot execute on a retired parent.
        var shortage=new LinkedHashMap<String,Object>(a);shortage.putAll(row(c,id(),at));shortage.put("quantity",q.subtract(retained));shortage.put("startQuantity",start.add(retained));shortage.put("state","SUSPENDED");shortage.put("suspendedAt",c.knownAt());shortage.put("suspensionReason","PHYSICAL_SHORTAGE");shortage.put("predecessorId",a.get("ID"));shortage.put("commandId",command);repository.insert("SegmentAllocations",shortage);
      }
      repository.update(c,"SegmentAllocations",(String)a.get("ID"),Map.of("state","REPLACED","revision",((Number)a.get("revision")).intValue()+1));
    }
  }

  private void closeMembership(DomainContext c,Map<String,Object> source,List<Map<String,Object>> children,Instant at) {
    for(var membership:repository.currentRows(c,"LogisticsMemberships"))if(source.get("ID").equals(membership.get("segmentId"))&&!instant(membership.get("validFrom")).isAfter(at)&&(membership.get("validUntil")==null||at.isBefore(instant(membership.get("validUntil"))))) {
      if(!at.isAfter(instant(membership.get("validFrom"))))throw DomainError.invalid("Membership transition must follow its start");
      repository.update(c,"LogisticsMemberships",(String)membership.get("ID"),Map.of("validUntil",at,"revision",((Number)membership.get("revision")).intValue()+1));
      for(var child:children) {
        boolean exists=repository.currentRows(c,"LogisticsMemberships").stream().anyMatch(m->child.get("ID").equals(m.get("segmentId"))&&m.get("validUntil")==null);
        if(!exists) {var r=row(c,id(),at);r.put("segmentId",child.get("ID"));r.put("logisticsUnitId",membership.get("logisticsUnitId"));r.put("validFrom",at);r.put("validUntil",membership.get("validUntil"));repository.insert("LogisticsMemberships",r);}
      }
    }
  }
  private Map<String,Object> child(DomainContext c,Map<String,Object> source,BigDecimal q,String place,Instant at,String evidence) {
    var r=row(c,id(),at);for(String key:List.of("itemId","lotId","identificationStatus","unit","custodianId","ownerId","controlScope","mixtureStatus"))r.put(key,source.get(key));
    r.put("quantity",q);r.put("placeId",place);r.put("validFrom",at);r.put("evidenceRef",evidence);return r;
  }
  private void edge(DomainContext c,Map<String,Object> source,Map<String,Object> target,BigDecimal q,BigDecimal sourceStart,BigDecimal targetStart,String kind,boolean uncertain,Instant at,String evidence,String command) {
    var r=row(c,id(),at);r.put("sourceId",source.get("ID"));r.put("targetId",target.get("ID"));r.put("quantity",q);r.put("unit",source.get("unit"));r.put("kind",kind);r.put("sourceStartQuantity",sourceStart);r.put("targetStartQuantity",targetStart);r.put("uncertain",uncertain);r.put("occurredAt",at);r.put("evidenceRef",evidence);repository.insert("GenealogyEdges",r);
    movement(c,source,target,q,kind,at,evidence,command);
  }
  private void movement(DomainContext c,Map<String,Object> source,Map<String,Object> target,BigDecimal q,String kind,Instant at,String evidence,String command) {
    var r=row(c,id(),at);r.put("sourceId",source==null?null:source.get("ID"));r.put("targetId",target==null?null:target.get("ID"));r.put("quantity",q);r.put("unit",source==null?target.get("unit"):source.get("unit"));r.put("kind",kind);r.put("occurredAt",at);r.put("evidenceRef",evidence);r.put("commandId",command);repository.insert("QuantityMovements",r);
  }
  public static Map<String,Object> row(DomainContext c,String id,Instant at) {
    var r=new LinkedHashMap<String,Object>();r.put("organizationId",c.organizationId());r.put("ID",id);r.put("revision",0);r.put("createdAt",c.knownAt());r.put("recordedAt",c.knownAt());return r;
  }
  public static Instant instant(Object v){return v instanceof Instant i?i:Instant.parse(v.toString());}
  public static BigDecimal amount(Map<String,Object> s){return (BigDecimal)s.get("quantity");}
  public static String id(){return UUID.randomUUID().toString();}
  private static DomainError conflict(String message){return new DomainError("REJECTED","REVISION_CONFLICT",message);}
}
