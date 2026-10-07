package com.mulino.application.trade.receipt;
import com.mulino.application.core.*;
import com.mulino.application.trade.InventoryReadFacts;
import com.mulino.application.evidence.EvidenceQueries;
import com.mulino.domain.inventory.*;
import com.mulino.domain.trade.receipt.ReceiptRepository;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Component;
@Component
public final class ReceiptQueries implements QueryHandler,ObjectReadProvider,InventoryReadFacts {
 private final ReceiptRepository r;private final ReadAuthorizer auth;private final EvidenceQueries evidence;
 public ReceiptQueries(ReceiptRepository r,ReadAuthorizer auth,EvidenceQueries evidence){this.r=r;this.auth=auth;this.evidence=evidence;}
 public Set<String> operations(){return Set.of("getReceipt","getReceipts");}
 public Set<String> objectTypes(){return Set.of("Receipt","ReceiptObservation");}
 public Set<String> metrics(){return Set.of("cumulativeArrival");}
 private Map<String,List<String>> scopes(Map<String,Object> row){return Map.of("TARGET",List.of(row.get("ID").toString()),"ITEM",List.of(row.get("itemId").toString()),"PLACE",List.of(row.get("placeId").toString()),"WORK",List.of(row.get("workId").toString()));}
 private boolean visible(DomainContext c,Map<String,Object> row,Map<String,Object> scope,String cap){return !StockPrimitives.instant(row.get("occurredAt")).isAfter(c.asOf())&&List.of("itemId","lotId","placeId","workId","purchaseLineId","rangeRootId").stream().allMatch(k->scope.get(k)==null||Objects.equals(scope.get(k),row.get(k)))&&auth.permittedScopes(c,cap,scopes(row));}
 public QueryResult query(DomainContext c,QueryRequest q){if(!Set.of("objectType","organizationId","itemId","lotId","placeId","workId","purchaseLineId","rangeRootId").containsAll(q.scope().keySet()))throw DomainError.invalid("Unsupported receipt scope");if(q.scope().get("organizationId")!=null&&!c.organizationId().equals(q.scope().get("organizationId")))throw DomainError.forbidden();String type=Objects.toString(q.scope().getOrDefault("objectType",q.filters().get("type")),"Receipt");String entity=type.equals("ReceiptObservation")?"Observations":"Receipts";if(!Set.of("Receipt","ReceiptObservation").contains(type))throw DomainError.invalid("Unsupported receipt noun");
  String after=ReadCursor.decode(q.cursor(),c,q);var rows=r.rows(c,entity).stream().filter(x->visible(c,x,q.scope(),q.operation())).filter(x->q.id()==null||q.id().equals(x.get("ID"))).sorted(Comparator.comparing(x->x.get("ID").toString())).filter(x->after==null||x.get("ID").toString().compareTo(after)>0).toList();if(q.id()!=null&&rows.isEmpty())throw DomainError.forbidden();boolean more=rows.size()>q.limit();var selected=rows.stream().limit(q.limit()).map(TransportValues::normalize).toList();return new QueryResult(q.id()==null?selected:selected.getFirst(),q.scope(),entity.equals("Observations")?rows.stream().filter(x->!"CONFIRMED".equals(x.get("state"))).map(x->x.get("ID")+":PROVISIONAL_INELIGIBLE").toList():List.of(),List.of(),rows.stream().map(x->Objects.toString(x.get("canonicalOccurrenceId"),Objects.toString(x.get("eventId"),""))).toList(),more?ReadCursor.encode(rows.get(q.limit()-1).get("ID").toString(),c,q):null);
 }
 public Facts read(DomainContext c,String operation,String item,Map<String,Object> scope,List<Map<String,Object>> segments){
  var receipts=r.rows(c,"Receipts").stream().filter(x->item.equals(x.get("itemId"))&&visible(c,x,scope,operation)).toList();var refs=new ArrayList<String>();var unknown=new ArrayList<String>();BigDecimal sum=BigDecimal.ZERO;for(var row:receipts){String id=row.get("canonicalOccurrenceId").toString();refs.add(id);if(evidence.verifiedAt(c,id))sum=sum.add((BigDecimal)row.get("quantity"));else unknown.add(id+":RECEIPT_EVIDENCE_RETRACTED_REASSESSMENT_REQUIRED");}var values=new LinkedHashMap<String,Object>();values.put("cumulativeArrival",unknown.isEmpty()?InventoryQuantity.text(sum):null);return new Facts(values,unknown,List.of(),refs);
 }
}
