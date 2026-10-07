package com.mulino.application.trade.purchase;
import com.mulino.application.core.*;
import com.mulino.domain.trade.purchase.PurchaseRepository;
import java.util.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Component;
/** Immutable revisions and event histories support both noun and verb snapshots. */
@Component
public class PurchaseQueries implements QueryHandler,ObjectReadProvider {
 private final PurchaseRepository r;private final ReadAuthorizer auth;
 public PurchaseQueries(PurchaseRepository r,ReadAuthorizer auth){this.r=r;this.auth=auth;}
 public Set<String> operations(){return Set.of("getPurchase");}
 public Set<String> objectTypes(){return Set.of("PurchaseProposal","PurchaseOrder");}
 private boolean visible(DomainContext c,Map<String,Object> row){Object at=row.get("recordedAt");return at!=null&&!Instant.parse(at.toString()).isAfter(c.knownAt());}
 public QueryResult query(DomainContext c,QueryRequest q){if(q.id()==null)throw DomainError.invalid("Purchase ID required");String type=Objects.toString(q.filters().get("type"),r.rows(c,"Orders").stream().anyMatch(x->q.id().equals(x.get("ID")))?"PurchaseOrder":"PurchaseProposal");var data=new LinkedHashMap<String,Object>();String item,work;
 if(type.equals("PurchaseProposal")){var root=r.require(c,"Proposals",q.id());if(!visible(c,root))throw DomainError.forbidden();var versions=r.rows(c,"ProposalRevisions").stream().filter(x->q.id().equals(x.get("proposalId"))&&visible(c,x)).sorted(Comparator.comparingInt(x->((Number)x.get("revision")).intValue())).toList();if(versions.isEmpty())throw DomainError.forbidden();var current=versions.getLast();item=current.get("itemId").toString();work=current.get("workId").toString();data.putAll(current);data.put("ID",q.id());data.put("revisions",versions);data.put("approvals",r.rows(c,"Approvals").stream().filter(x->q.id().equals(x.get("proposalId"))&&visible(c,x)).toList());}
 else if(type.equals("PurchaseOrder")){var order=r.require(c,"Orders",q.id());if(!visible(c,order))throw DomainError.forbidden();var line=r.rows(c,"OrderLines").stream().filter(x->q.id().equals(x.get("orderId"))).findFirst().orElseThrow(DomainError::forbidden);item=line.get("itemId").toString();work=line.get("workId").toString();data.putAll(order);var replies=r.rows(c,"SupplierReplies").stream().filter(x->q.id().equals(x.get("orderId"))&&visible(c,x)).toList();var cancellations=r.rows(c,"Cancellations").stream().filter(x->q.id().equals(x.get("orderId"))&&visible(c,x)).toList();var credits=r.rows(c,"ReceiptCredits").stream().filter(x->line.get("ID").equals(x.get("lineId"))&&visible(c,x)).toList();data.put("line",Map.of("ID",line.get("ID"),"quantity",line.get("quantity"),"unit",line.get("unit"),"itemId",item,"workId",work,"destinationId",line.get("destinationId")));data.put("supplierReplies",replies);data.put("cancellations",cancellations);data.put("receiptCredits",credits);data.put("arrivedQuantity",credits.stream().map(x->new BigDecimal(x.get("contributedQuantity").toString())).reduce(BigDecimal.ZERO,BigDecimal::add));data.put("supplierAcceptance",replies.isEmpty()?"UNKNOWN":replies.getLast().get("reply"));data.put("dispatchRequested",true);data.put("deliveryState","UNKNOWN");}
 else throw DomainError.invalid("Purchase noun unsupported");
 var scopes=Map.of("ITEM",List.of(item),"WORK",List.of(work));auth.authorizeScopes(c,q.operation().equals("getPurchase")?"getPurchase":"getObject",scopes);data.put("type",type);data.put("itemId",item);data.put("workId",work);return QueryResult.of(data,Map.of("organizationId",c.organizationId(),"itemId",item,"workId",work));
 }
}
