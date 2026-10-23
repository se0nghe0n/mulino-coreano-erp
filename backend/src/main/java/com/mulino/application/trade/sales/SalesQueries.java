package com.mulino.application.trade.sales;
import com.mulino.application.core.*;import com.mulino.domain.trade.sales.SalesRepository;import com.mulino.domain.inventory.*;import com.mulino.domain.evidence.EvidenceSubjectPort;
import com.mulino.application.trade.DispatchCargoPort;
import java.math.BigDecimal;
import java.util.*;import org.springframework.stereotype.Component;
@Component
public class SalesQueries implements QueryHandler,ObjectReadProvider,EvidenceSubjectPort {
 private final SalesRepository r;private final ReadAuthorizer auth;private final DispatchCargoPort dispatch;
 public SalesQueries(SalesRepository r,ReadAuthorizer auth,@org.springframework.context.annotation.Lazy DispatchCargoPort dispatch){this.r=r;this.auth=auth;this.dispatch=dispatch;}
 public Set<String> operations(){return Set.of("getSalesOrder","getSalesOrders","getDelivery","getDeliveries");}
 public Set<String> objectTypes(){return Set.of("SalesOrder","SalesOrderRevision","SalesOrderLine","Delivery","DeliveryObservation","Customer");}
 public Set<String> subjectKinds(){return Set.of("SALES_ORDER","SALES_ORDER_LINE","DELIVERY","DELIVERY_OBSERVATION");}
 private String entity(String type){return switch(type){case "Customer"->"Customers";case "SalesOrder"->"Orders";case "SalesOrderRevision"->"OrderRevisions";case "SalesOrderLine"->"OrderLines";case "Delivery"->"Deliveries";case "DeliveryObservation"->"Observations";default->throw DomainError.invalid("Unsupported sales noun");};}
 public Map<String,Object> require(String org,String kind,String id){return r.subject(org,switch(kind){case "SALES_ORDER"->"Orders";case "SALES_ORDER_LINE"->"OrderLines";case "DELIVERY"->"Deliveries";case "DELIVERY_OBSERVATION"->"Observations";default->throw DomainError.unsupported();},id);}
 private Map<String,List<String>> scopes(Map<String,Object>x){var scopes=new LinkedHashMap<String,List<String>>();scopes.put("TARGET",List.of(x.get("ID").toString()));if(x.get("workId")!=null)scopes.put("WORK",List.of(x.get("workId").toString()));if(x.get("itemId")!=null)scopes.put("ITEM",List.of(x.get("itemId").toString()));if(x.get("placeId")!=null||x.get("destinationId")!=null)scopes.put("PLACE",List.of(Objects.toString(x.get("placeId"),x.get("destinationId")==null?null:x.get("destinationId").toString())));return scopes;}
 public QueryResult query(DomainContext c,QueryRequest q){if(!Set.of("type").containsAll(q.filters().keySet()))throw DomainError.invalid("Unsupported sales filter");if(q.scope().get("objectType")!=null&&q.filters().get("type")!=null&&!Objects.equals(q.scope().get("objectType"),q.filters().get("type")))throw DomainError.invalid("Conflicting sales noun");if(!Set.of("objectType","organizationId","workId","itemId","customerId","orderId","salesLineId","dispatchId","rangeRootId","lotId","placeId").containsAll(q.scope().keySet()))throw DomainError.invalid("Unsupported sales query scope");if(q.scope().get("organizationId")!=null&&!c.organizationId().equals(q.scope().get("organizationId")))throw DomainError.forbidden();String type=Objects.toString(q.scope().getOrDefault("objectType",q.filters().get("type")),q.operation().contains("Delivery")||q.operation().contains("Deliveries")?"Delivery":"SalesOrder");String table=entity(type),after=ReadCursor.decode(q.cursor(),c,q);var rows=(table.equals("Observations")?observations(c):r.rows(c,table)).stream().filter(x->!StockPrimitives.instant(x.get("effectiveAt")).isAfter(c.asOf())&&(x.get("occurredAt")==null||!StockPrimitives.instant(x.get("occurredAt")).isAfter(c.asOf()))).filter(x->q.id()==null||q.id().equals(x.get("ID"))).filter(x->List.of("workId","itemId","customerId","orderId","salesLineId","dispatchId","rangeRootId","lotId","placeId").stream().allMatch(k->q.scope().get(k)==null||Objects.equals(q.scope().get(k),x.get(k)))).filter(x->auth.permittedScopes(c,q.operation(),scopes(x))).sorted(Comparator.comparing(x->x.get("ID").toString())).filter(x->after==null||x.get("ID").toString().compareTo(after)>0).toList();if(q.id()!=null&&rows.isEmpty())throw DomainError.forbidden();var values=rows.stream().limit(q.limit()).map(x->project(c,q.operation(),table,x)).map(TransportValues::normalize).toList();return new QueryResult(q.id()==null?values:values.getFirst(),q.scope(),table.equals("Observations")?rows.stream().filter(x->!"CONFIRMED".equals(x.get("state"))).map(x->x.get("ID")+":DELIVERY_UNCONFIRMED").toList():List.of(),List.of(),rows.stream().map(x->Objects.toString(x.get("canonicalOccurrenceId"),Objects.toString(x.get("eventId"),""))).filter(x->!x.isEmpty()).toList(),rows.size()>q.limit()?ReadCursor.encode(rows.get(q.limit()-1).get("ID").toString(),c,q):null);}
 /**
  * Confirmation is recorded as an immutable Delivery; the Observations row only carries the latest state. A read at an earlier
  * knownAt must not see a confirmation that was not yet recorded (plan §3.1, §4.3, D06), so the state is projected from the
  * Deliveries visible at that knownAt.
  */
 private List<Map<String,Object>> observations(DomainContext c){var confirmed=new HashSet<Object>();for(var d:r.rows(c,"Deliveries"))confirmed.add(d.get("observationId"));return r.rows(c,"Observations").stream().map(x->{var row=new LinkedHashMap<String,Object>(x);boolean known=confirmed.contains(x.get("ID"));if("CONFIRMED".equals(x.get("state"))&&!known){row.put("state","PROVISIONAL");row.put("revision",1);if(row.get("claimedRangeRootId")!=null)row.put("rangeRootId",null);}return (Map<String,Object>)row;}).toList();}
 /** Delivery reports are distinct from quality/regulatory evidence claims in the same item scope. */
 public List<Map<String,Object>> worldObservations(DomainContext c,Map<String,Object> scope){
  return observations(c).stream().filter(row->at(c,row)&&!StockPrimitives.instant(row.get("occurredAt")).isAfter(c.asOf()))
   .filter(row->List.of("itemId","workId","customerId","salesLineId","lotId","placeId").stream().allMatch(key->scope.get(key)==null||Objects.equals(scope.get(key),row.get(key))))
   .filter(row->scope.get("orderId")==null||row.get("salesLineId")!=null&&Objects.equals(scope.get("orderId"),r.require(c,"OrderLines",row.get("salesLineId").toString()).get("orderId")))
   .filter(row->auth.permittedScopes(c,"getObject",scopes(row))).sorted(Comparator.comparing(row->row.get("ID").toString())).map(row->{
    var value=new LinkedHashMap<String,Object>(row);boolean confirmed="CONFIRMED".equals(row.get("state"));value.put("canonical",confirmed);value.put("status",confirmed?"CONFIRMED":"PENDING_RECONCILIATION");
    value.put("saleExecutionAuthorized",false); // RECORD intake/confirmation never grants a future execution.
    return (Map<String,Object>)value;
   }).toList();
 }
 private boolean at(DomainContext c,Map<String,Object>x){return !StockPrimitives.instant(x.get("effectiveAt")).isAfter(c.asOf());}
 private Map<String,Object> project(DomainContext c,String operation,String entity,Map<String,Object>x){var result=new LinkedHashMap<>(x);if(entity.equals("Orders")){var revisions=r.rows(c,"OrderRevisions").stream().filter(a->x.get("ID").equals(a.get("orderId"))&&at(c,a)&&auth.permittedScopes(c,"getSalesOrder",scopes(a))).toList();result.put("revisions",revisions);int visible=revisions.stream().mapToInt(a->((Number)a.get("revisionNumber")).intValue()).max().orElse(0);result.put("currentRevision",visible);result.put("revision",visible);result.put("lines",r.rows(c,"OrderLines").stream().filter(a->x.get("ID").equals(a.get("orderId"))&&at(c,a)&&auth.permittedScopes(c,"getSalesOrder",scopes(a))).toList());}if(entity.equals("Deliveries"))result.put("corrections",r.rows(c,"DeliveryCorrections").stream().filter(a->x.get("ID").equals(a.get("deliveryId"))&&at(c,a)).sorted(Comparator.comparingInt(a->((Number)a.get("revision")).intValue())).toList());if(entity.equals("Observations"))result.put("priorDispatchCheck",priorDispatchCheck(c,operation,x));return result;}
 /** An observation-specific prerequisite, never a sale permission or a promise of confirmation.
  * Missing reference is known DENIED. An unavailable/corrupt history is UNKNOWN; a readable
  * contradictory binding is DENIED. ALLOWED only means the prior dispatch matches this report.
  * Link IDs below are the caller's stored report, not identifiers learned from hidden rows. */
 private Map<String,Object> priorDispatchCheck(DomainContext c,String operation,Map<String,Object> observation){
  var check=new LinkedHashMap<String,Object>();check.put("required",true);
  check.put("linkedDispatchId",observation.get("dispatchId"));check.put("salesLineId",observation.get("salesLineId"));
  String status="UNKNOWN",reason="DISPATCH_LINK_INCOMPLETE";
  if(observation.get("dispatchId")==null){status="DENIED";reason="PRIOR_DISPATCH_MISSING";}
  else try{
   var actual=dispatch.requireDispatch(c,observation.get("dispatchId").toString());
   var required=List.of("ID","organizationId","workId","itemId","customerId","salesLineId","cargoScopeId","rangeRootId","lotId","unit","startQuantity","quantity","occurredAt","destinationId");
   if(required.stream().allMatch(key->actual.get(key)!=null)){
    if(!Objects.equals(c.organizationId(),actual.get("organizationId")))reason="DISPATCH_UNAVAILABLE";
    else if(!auth.permittedScopes(c,operation,Map.of("TARGET",List.of(actual.get("ID").toString()),"WORK",List.of(actual.get("workId").toString()),"ITEM",List.of(actual.get("itemId").toString()),"PLACE",List.of(actual.get("destinationId").toString()),"CUSTOMER",List.of(actual.get("customerId").toString()))))reason="DISPATCH_READ_SCOPE_UNAVAILABLE";
    else if(StockPrimitives.instant(actual.get("occurredAt")).isAfter(c.asOf()))reason="DISPATCH_NOT_YET_OBSERVED";
    else if(List.of("workId","itemId","customerId","salesLineId","cargoScopeId","lotId","unit","startQuantity","quantity","occurredAt").stream().anyMatch(key->observation.get(key)==null))reason="DISPATCH_LINK_INCOMPLETE";
    else{
     boolean matches=Objects.equals(observation.get("dispatchId"),actual.get("ID"))&&Objects.equals(observation.get("placeId"),actual.get("destinationId"))&&List.of("workId","itemId","customerId","salesLineId","cargoScopeId","lotId","unit").stream().allMatch(key->Objects.equals(observation.get(key),actual.get(key)))
       &&Objects.equals(DeliveryObservationClaim.root(observation),actual.get("rangeRootId"));
     BigDecimal start=new BigDecimal(observation.get("startQuantity").toString()),quantity=new BigDecimal(observation.get("quantity").toString()),from=new BigDecimal(actual.get("startQuantity").toString()),amount=new BigDecimal(actual.get("quantity").toString());
     matches&=quantity.signum()>0&&amount.signum()>0&&start.signum()>=0&&from.signum()>=0&&start.compareTo(from)>=0&&start.add(quantity).compareTo(from.add(amount))<=0
       &&!StockPrimitives.instant(observation.get("occurredAt")).isBefore(StockPrimitives.instant(actual.get("occurredAt")));
     status=matches?"ALLOWED":"DENIED";reason=matches?"PRIOR_DISPATCH_MATCHED":"DISPATCH_SCOPE_MISMATCH";
    }
   }
  }catch(DomainError unavailable){reason="FORBIDDEN".equals(unavailable.code())?"DISPATCH_UNAVAILABLE":"DISPATCH_HISTORY_INCONSISTENT";}
   catch(IllegalArgumentException corrupt){reason="DISPATCH_HISTORY_INCONSISTENT";}
  check.put("status",status);check.put("reasonCode",reason);return check;
 }
 /** Current line contracts are selected through immutable visible revisions, not a mutable header counter. */
 public record WorldLines(List<Map<String,Object>> rows,boolean complete){}
 public WorldLines worldLines(DomainContext c,Map<String,Object> scope){
  var revisions=r.rows(c,"OrderRevisions").stream().filter(x->at(c,x)).toList();
  var orders=r.rows(c,"Orders").stream().filter(x->at(c,x)).map(x->x.get("ID")).collect(java.util.stream.Collectors.toSet());
  var heads=new HashMap<Object,Integer>();for(var revision:revisions)if(orders.contains(revision.get("orderId")))heads.merge(revision.get("orderId"),((Number)revision.get("revisionNumber")).intValue(),Math::max);
  var candidates=r.rows(c,"OrderLines").stream().filter(x->at(c,x)&&Objects.equals(scope.get("itemId"),x.get("itemId")))
   .filter(x->List.of("workId","customerId","orderId").stream().allMatch(k->scope.get(k)==null||Objects.equals(scope.get(k),x.get(k))))
   .filter(x->scope.get("salesLineId")==null||Objects.equals(scope.get("salesLineId"),x.get("ID")))
   .filter(x->revisions.stream().anyMatch(v->Objects.equals(v.get("ID"),x.get("revisionId"))&&Objects.equals(v.get("orderId"),x.get("orderId"))
     &&(scope.get("salesLineId")!=null||Objects.equals(heads.get(v.get("orderId")),((Number)v.get("revisionNumber")).intValue()))))
   .sorted(Comparator.comparing(x->x.get("ID").toString())).toList();
  var visible=candidates.stream().filter(x->auth.permittedScopes(c,"getObject",scopes(x))).map(x->(Map<String,Object>)new LinkedHashMap<String,Object>(x)).toList();
  return new WorldLines(visible,visible.size()==candidates.size());
 }

}
