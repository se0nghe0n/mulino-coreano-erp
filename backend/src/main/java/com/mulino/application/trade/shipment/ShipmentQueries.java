package com.mulino.application.trade.shipment;

import com.mulino.application.core.*;
import com.mulino.domain.trade.shipment.ShipmentRepository;
import com.mulino.domain.inventory.StockPrimitives;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class ShipmentQueries implements QueryHandler,ObjectReadProvider {
 private final ShipmentRepository r;private final ReadAuthorizer auth;private final ShipmentEvidence evidence;
 public ShipmentQueries(ShipmentRepository r,ReadAuthorizer auth,ShipmentEvidence evidence){this.r=r;this.auth=auth;this.evidence=evidence;}
 public Set<String> operations(){return Set.of("getShipment","searchShipments");}
 public Set<String> objectTypes(){return Set.of("Shipment");}
 public QueryResult query(DomainContext c,QueryRequest q){
  if(!Set.of("organizationId","objectType","itemId","workId").containsAll(q.scope().keySet())||!Set.of("type","sort").containsAll(q.filters().keySet())||q.scope().containsKey("organizationId")&&!c.organizationId().equals(q.scope().get("organizationId")))throw DomainError.invalid("Unsupported shipment query selector");
  if(q.filters().containsKey("sort")&&!"ID".equals(q.filters().get("sort")))throw DomainError.invalid("Unsupported sort");
  if(q.scope().containsKey("objectType")&&!"Shipment".equals(q.scope().get("objectType"))||q.filters().containsKey("type")&&!"Shipment".equals(q.filters().get("type")))throw DomainError.invalid("Shipment type required");
  boolean single=Set.of("getShipment","getObject").contains(q.operation());if(single&&q.id()==null)throw DomainError.invalid("Shipment id required");String after=ReadCursor.decode(q.cursor(),c,q);var output=new ArrayList<Map<String,Object>>();var refs=new ArrayList<String>();
  for(var sh:r.rows(c,"Shipments")){String id=sh.get("ID").toString();if(q.id()!=null&&!q.id().equals(id)||after!=null&&id.compareTo(after)<=0||!visible(c,sh)||q.scope().containsKey("workId")&&!q.scope().get("workId").equals(sh.get("workId")))continue;var cargo=r.cargoRows(c,id);if(q.scope().containsKey("itemId")&&cargo.stream().noneMatch(x->q.scope().get("itemId").equals(x.get("itemId"))))continue;var scope=scopes(c,sh);if(!auth.permittedScopes(c,q.operation(),scope))continue;
   var data=new LinkedHashMap<>(sh);data.put("planStatus","PLANNED");data.put("physicalInventoryEffects","NONE");data.put("legs",r.rows(c,"ShipmentLegs").stream().filter(x->id.equals(x.get("shipmentId"))).toList());var cargoRead=new ArrayList<Map<String,Object>>();
   for(var ca:cargo){var cr=new LinkedHashMap<>(ca);String ci=ca.get("ID").toString();cr.put("allocations",r.cargoAllocations(c,ci));var events=observations(c,ci);cr.put("observations",events);BigDecimal departure=latestQuantity(events,"DEPARTURE"),arrival=latestQuantity(events,"ARRIVAL"),transit=latestQuantity(events,"IN_TRANSIT");cr.put("departureQuantity",departure==null?"UNKNOWN":departure.toPlainString());cr.put("arrivalQuantity",arrival==null?"UNKNOWN":arrival.toPlainString());cr.put("inTransitQuantity",transit==null?"UNKNOWN":transit.toPlainString());cr.put("lossQuantity","UNVERIFIED");cr.put("differenceState",departure!=null&&arrival!=null&&transit!=null&&departure.compareTo(arrival.add(transit))==0?"ACCOUNTED_IN_TRANSIT":"RECONCILIATION_REQUIRED");cr.put("lastConfirmedObservation",events.isEmpty()?Map.of("valueState","UNKNOWN"):events.getLast());for(var event:events)refs.add(event.get("occurrenceId").toString());cargoRead.add(cr);}
   data.put("cargo",cargoRead);data.put("scope",scope);output.add(data);
  }
  if(single&&output.isEmpty())throw DomainError.forbidden();boolean more=output.size()>q.limit();var page=output.stream().limit(q.limit()).map(TransportValues::normalize).toList();String next=more?ReadCursor.encode(output.get(q.limit()-1).get("ID").toString(),c,q):null;
  return new QueryResult(single?page.getFirst():page,q.scope(),List.of(),List.of(),refs.stream().distinct().toList(),next);
 }
 public List<Map<String,Object>> verifiedObservations(DomainContext c,String cargo){var ca=r.requireCargo(c,cargo);var sh=r.currentShipment(c,ca.get("shipmentId").toString());auth.authorizeScopes(c,"getShipment",scopes(c,sh));return observations(c,cargo);}
 private List<Map<String,Object>> observations(DomainContext c,String cargo){var all=new ArrayList<Map<String,Object>>();for(String entity:List.of("LegEvents","CustodyHandovers"))for(var event:r.rows(c,entity)){if(!cargo.equals(event.get("cargoId"))||!visible(c,event)||StockPrimitives.instant(event.get("occurredAt")).isAfter(c.asOf()))continue;var ca=r.requireCargo(c,cargo);String kind=entity.equals("CustodyHandovers")?"CUSTODY_HANDOVER":"SHIPMENT_"+event.get("kind");try{evidence.require(c,event.get("occurrenceId").toString(),kind,ca.get("itemId").toString(),event.get("physicalScopeId").toString(),event.get("placeId").toString(),(BigDecimal)event.get("quantity"),event.get("unit").toString(),StockPrimitives.instant(event.get("occurredAt")));all.add(event);}catch(DomainError e){if(!Set.of("EVIDENCE_UNVERIFIED","FORBIDDEN").contains(e.code()))throw e;}}
  all.sort(Comparator.comparing((Map<String,Object>x)->StockPrimitives.instant(x.get("occurredAt"))).thenComparing(x->x.get("ID").toString()));return all;
 }
 private Map<String,List<String>> scopes(DomainContext c,Map<String,Object>sh){var result=new LinkedHashMap<String,List<String>>();result.put("TARGET",List.of(sh.get("ID").toString()));result.put("ITEM",r.cargoRows(c,sh.get("ID").toString()).stream().map(x->x.get("itemId").toString()).distinct().toList());var places=new TreeSet<String>();places.add(sh.get("originId").toString());places.add(sh.get("destinationId").toString());for(var leg:r.rows(c,"ShipmentLegs"))if(sh.get("ID").equals(leg.get("shipmentId"))){places.add(leg.get("originId").toString());places.add(leg.get("destinationId").toString());}result.put("PLACE",List.copyOf(places));if(sh.get("workId")!=null)result.put("WORK",List.of(sh.get("workId").toString()));return result;}
 private static BigDecimal latestQuantity(List<Map<String,Object>>events,String kind){var current=new HashMap<String,BigDecimal>();for(var e:events)if(kind.equals(e.get("kind")))current.put(e.get("physicalScopeId").toString(),(BigDecimal)e.get("quantity"));return current.isEmpty()?null:current.values().stream().reduce(BigDecimal.ZERO,BigDecimal::add);}
 private static boolean visible(DomainContext c,Map<String,Object>r){return !StockPrimitives.instant(r.get("recordedAt")).isAfter(c.knownAt());}
}
