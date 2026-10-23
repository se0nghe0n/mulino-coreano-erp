package com.mulino.application.inventory;

import com.mulino.application.core.*;
import com.mulino.domain.inventory.InventoryRepository;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;

/** An immutable measurement and its distinct book comparison, with complete member visibility. */
@Component
public final class PlaceStocktakeQueries implements ObjectReadProvider {
  private final InventoryRepository repository; private final ReadAuthorizer auth;
  public PlaceStocktakeQueries(InventoryRepository repository,ReadAuthorizer auth){this.repository=repository;this.auth=auth;}
  public Set<String> objectTypes(){return Set.of("PlaceStocktake");}
  public QueryResult query(DomainContext c,QueryRequest q) {
    if(!Set.of("getObject","searchObjects").contains(q.operation())
      ||!Set.of("objectType","organizationId","itemId","placeId","lotId").containsAll(q.scope().keySet())
      ||!Set.of("type","sort").containsAll(q.filters().keySet())
      ||q.filters().containsKey("sort")&&!"ID".equals(q.filters().get("sort"))
      ||q.scope().get("organizationId")!=null&&!c.organizationId().equals(q.scope().get("organizationId")))throw DomainError.invalid("Invalid stocktake selector");
    boolean single=q.operation().equals("getObject");
    if(single&&q.id()==null)throw DomainError.invalid("Stocktake ID required");
    String after=ReadCursor.decode(q.cursor(),c,q);var out=new ArrayList<Map<String,Object>>();
    var members=repository.currentRows(c,"PlaceStocktakeMembers");
    for(var row:repository.currentRows(c,"PlaceStocktakes")) {
      String id=row.get("ID").toString();
      if(q.id()!=null&&!id.equals(q.id())||after!=null&&id.compareTo(after)<=0
        ||Instant.parse(row.get("recordedAt").toString()).isAfter(c.knownAt())
        ||Instant.parse(row.get("occurredAt").toString()).isAfter(c.asOf()))continue;
      if(List.of("itemId","placeId","lotId").stream().anyMatch(k->q.scope().get(k)!=null&&!Objects.equals(q.scope().get(k),row.get(k))))continue;
      var base=new LinkedHashMap<String,List<String>>();
      base.put("TARGET",List.of(row.get("placeId").toString()));base.put("ITEM",List.of(row.get("itemId").toString()));base.put("PLACE",List.of(row.get("placeId").toString()));
      if(row.get("lotId")!=null)base.put("LOT",List.of(row.get("lotId").toString()));
      if(!auth.permittedScopes(c,q.operation(),base))continue;
      var actual=members.stream().filter(m->id.equals(m.get("stocktakeId"))).sorted(Comparator.comparing(m->m.get("segmentId").toString())).toList();
      if(actual.stream().anyMatch(m->{var exact=new LinkedHashMap<>(base);exact.put("TARGET",List.of(m.get("segmentId").toString()));return !auth.permittedScopes(c,q.operation(),exact);}))continue;
      var view=new LinkedHashMap<>(row);view.remove("sourceMembersJson");view.put("type","PlaceStocktake");view.put("bookMembers",actual);
      out.add(view);
    }
    out.sort(Comparator.comparing(r->r.get("ID").toString()));
    if(single&&out.isEmpty())throw DomainError.forbidden();
    boolean more=out.size()>q.limit();var page=out.stream().limit(q.limit()).map(r->(Map<String,Object>)TransportValues.normalize(r)).toList();
    var scope=new LinkedHashMap<>(q.scope());scope.put("organizationId",c.organizationId());
    if(single)for(String key:List.of("itemId","placeId","lotId"))if(page.getFirst().get(key)!=null)scope.put(key,page.getFirst().get(key));
    return new QueryResult(single?page.getFirst():page,scope,List.of(),List.of(),List.of(),
      more?ReadCursor.encode(page.getLast().get("ID").toString(),c,q):null);
  }
}
