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
    BigDecimal total=sources.stream().map(StockPrimitives::amount).reduce(BigDecimal.ZERO,BigDecimal::add);
    quantity(c,first,total.toPlainString(),(String)first.get("unit"));
    return replace(c,sources,List.of(total),(String)first.get("placeId"),at,evidence,command,"MERGE").getFirst();
  }
  public String moveInternal(DomainContext c,String parent,String destination,Instant at,String evidence,String command) {
    lockSources(c,List.of(parent));var s=leaf(c,parent,at);
    var from=repository.current(c,"Places",(String)s.get("placeId"));var to=repository.current(c,"Places",destination);
    if(!"INTERNAL_STORAGE".equals(from.get("kind"))||!"INTERNAL_STORAGE".equals(to.get("kind")) || Objects.equals(from.get("ID"),to.get("ID")))throw DomainError.invalid("Configured distinct internal storage places required");
    if(s.get("custodianId")==null)throw DomainError.invalid("Confirmed internal custody required");
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
    if(!repository.currentRows(c,"SegmentAllocations").stream().filter(a->parent.equals(a.get("segmentId"))).noneMatch(a->Set.of("EXECUTABLE","SUSPENDED").contains(a.get("state"))))throw conflict("Resolve allocation responsibility before decreasing stock");
    List<String> children=replace(c,List.of(s),remaining.signum()==0?List.of():List.of(remaining),(String)s.get("placeId"),at,evidence,command,kind);
    movement(c,s,null,decrease,kind,at,evidence,command);return children;
  }
  /** Explicit approved upward correction keeps old scope and identifies the excess as a new leaf. */
  public String increase(DomainContext c,String parent,Object value,String unit,Instant at,String evidence,String command) {
    lockSources(c,List.of(parent));var s=leaf(c,parent,at);var delta=quantity(c,s,value,unit);
    var extra=child(c,s,delta,(String)s.get("placeId"),at,evidence);
    repository.insert("QuantitySegments",extra);movement(c,null,extra,delta,"ADJUST_INCREASE",at,evidence,command);return (String)extra.get("ID");
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
      if(sources.size()==1) for(var target:children)edge(c,source,target,amount(target),kind,uncertain,at,evidence,command);
      else edge(c,source,children.getFirst(),amount(source),kind,uncertain,at,evidence,command);
      transferAllocations(c,source,children,at,command);
      closeMembership(c,source,children,at);
    }
    return children.stream().map(s->(String)s.get("ID")).toList();
  }
  private void transferAllocations(DomainContext c,Map<String,Object> source,List<Map<String,Object>> children,Instant at,String command) {
    Map<String,BigDecimal> available=new LinkedHashMap<>();children.forEach(child->available.put((String)child.get("ID"),amount(child)));
    // Existing allocations of other merge parents already moved into the same child.
    repository.currentRows(c,"SegmentAllocations").stream().filter(a->available.containsKey(a.get("segmentId"))&&Set.of("EXECUTABLE","SUSPENDED").contains(a.get("state"))).forEach(a->available.compute((String)a.get("segmentId"),(k,v)->v.subtract((BigDecimal)a.get("quantity"))));
    var old=repository.currentRows(c,"SegmentAllocations").stream().filter(a->source.get("ID").equals(a.get("segmentId"))&&Set.of("EXECUTABLE","SUSPENDED").contains(a.get("state"))).sorted(Comparator.comparing(a->(String)a.get("ID"))).toList();
    for(var a:old) {
      BigDecimal remaining=(BigDecimal)a.get("quantity");
      for(var child:children) {
        String sid=(String)child.get("ID");BigDecimal q=remaining.min(available.get(sid));if(q.signum()<=0)continue;
        var next=row(c,id(),at);for(String key:List.of("rootId","orderLineId","unit","state"))next.put(key,a.get(key));
        next.put("segmentId",sid);next.put("quantity",q);next.put("predecessorId",a.get("ID"));next.put("commandId",command);
        repository.insert("SegmentAllocations",next);remaining=remaining.subtract(q);available.put(sid,available.get(sid).subtract(q));
      }
      if(remaining.signum()!=0)throw conflict("Existing allocation exceeds replacement stock");
      repository.update(c,"SegmentAllocations",(String)a.get("ID"),Map.of("state","REPLACED","revision",((Number)a.get("revision")).intValue()+1));
    }
  }
  private void closeMembership(DomainContext c,Map<String,Object> source,List<Map<String,Object>> children,Instant at) {
    for(var membership:repository.currentRows(c,"LogisticsMemberships"))if(source.get("ID").equals(membership.get("segmentId"))&&membership.get("validUntil")==null) {
      if(!at.isAfter(instant(membership.get("validFrom"))))throw DomainError.invalid("Membership transition must follow its start");
      repository.update(c,"LogisticsMemberships",(String)membership.get("ID"),Map.of("validUntil",at));
      for(var child:children) {
        boolean exists=repository.currentRows(c,"LogisticsMemberships").stream().anyMatch(m->child.get("ID").equals(m.get("segmentId"))&&m.get("validUntil")==null);
        if(!exists) {var r=row(c,id(),at);r.put("segmentId",child.get("ID"));r.put("logisticsUnitId",membership.get("logisticsUnitId"));r.put("validFrom",at);repository.insert("LogisticsMemberships",r);}
      }
    }
  }
  private Map<String,Object> child(DomainContext c,Map<String,Object> source,BigDecimal q,String place,Instant at,String evidence) {
    var r=row(c,id(),at);for(String key:List.of("itemId","lotId","identificationStatus","unit","custodianId","ownerId","controlScope","mixtureStatus"))r.put(key,source.get(key));
    r.put("quantity",q);r.put("placeId",place);r.put("validFrom",at);r.put("evidenceRef",evidence);return r;
  }
  private void edge(DomainContext c,Map<String,Object> source,Map<String,Object> target,BigDecimal q,String kind,boolean uncertain,Instant at,String evidence,String command) {
    var r=row(c,id(),at);r.put("sourceId",source.get("ID"));r.put("targetId",target.get("ID"));r.put("quantity",q);r.put("unit",source.get("unit"));r.put("kind",kind);r.put("uncertain",uncertain);r.put("occurredAt",at);r.put("evidenceRef",evidence);repository.insert("GenealogyEdges",r);
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
