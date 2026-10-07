package com.mulino.application.trade.returns;
import com.mulino.application.core.*;
import com.mulino.domain.trade.returns.ReturnRepository;
import java.util.*;
import org.springframework.stereotype.Component;
@Component
public class ReturnQueries implements QueryHandler,ObjectReadProvider {
 private final ReturnRepository r;private final ReadAuthorizer auth;
 public ReturnQueries(ReturnRepository r,ReadAuthorizer auth){this.r=r;this.auth=auth;}
 public Set<String> operations(){return Set.of("getReturn","getReturns");}
 public Set<String> objectTypes(){return Set.of("Return");}
 public QueryResult query(DomainContext c,QueryRequest q){if(!Set.of("objectType","organizationId","itemId","workId","deliveryId","customerId","rangeRootId").containsAll(q.scope().keySet()))throw DomainError.invalid("Unsupported return query scope");if(q.scope().get("organizationId")!=null&&!c.organizationId().equals(q.scope().get("organizationId")))throw DomainError.forbidden();String after=ReadCursor.decode(q.cursor(),c,q);var rows=r.rows(c,"Observations").stream().filter(x->q.id()==null||q.id().equals(x.get("ID"))).filter(x->q.scope().entrySet().stream().allMatch(e->Set.of("objectType","organizationId").contains(e.getKey())||Objects.equals(e.getValue(),x.get(e.getKey())))).filter(x->auth.permittedScopes(c,q.operation(),ReturnCommands.scopes(x))).sorted(Comparator.comparing(x->x.get("ID").toString())).filter(x->after==null||x.get("ID").toString().compareTo(after)>0).toList();if(q.id()!=null&&rows.isEmpty())throw DomainError.forbidden();var results=rows.stream().limit(q.limit()).map(x->{var result=new LinkedHashMap<>(x);result.put("receipts",r.rows(c,"Receipts").stream().filter(y->x.get("ID").equals(y.get("observationId"))).toList());result.put("originalDeliveryPreserved",true);return TransportValues.normalize(result);}).toList();return new QueryResult(q.id()==null?results:results.getFirst(),q.scope(),rows.stream().filter(x->!"CONFIRMED".equals(x.get("state"))).map(x->x.get("ID")+":PROVISIONAL_UNAVAILABLE").toList(),List.of(),List.of(),rows.size()>q.limit()?ReadCursor.encode(rows.get(q.limit()-1).get("ID").toString(),c,q):null);}
}
