package com.mulino.application.inventory;

import com.mulino.application.core.*;
import com.mulino.domain.inventory.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class InventoryQueries implements QueryHandler {
  private static final Map<String,String> TYPES = Map.ofEntries(Map.entry("Product","Products"),Map.entry("TradeItem","TradeItems"),Map.entry("ManufacturingLot","ManufacturingLots"),Map.entry("QuantitySegment","QuantitySegments"),Map.entry("LogisticsUnit","LogisticsUnits"),Map.entry("Place","Places"),Map.entry("Manufacturer","Manufacturers"));
  private final InventoryRepository repository;
  private final ReadAuthorizer authorizer;
  public InventoryQueries(InventoryRepository repository, ReadAuthorizer authorizer) { this.repository=repository; this.authorizer=authorizer; }
  public Set<String> operations() { return Set.of("getObject","searchObjects","getInventory","getTrace"); }
  public QueryResult query(DomainContext c, QueryRequest q) {
    if (!q.filters().isEmpty()) throw DomainError.invalid("Unsupported inventory filter");
    return switch(q.operation()) {
      case "getObject" -> object(c,q);
      case "searchObjects" -> search(c,q);
      case "getInventory" -> inventory(c,q);
      case "getTrace" -> trace(c,q);
      default -> throw DomainError.unsupported();
    };
  }
  private String entity(QueryRequest q) {
    String type = (String)q.scope().get("objectType");
    if (type == null) type="TradeItem";
    String entity=TYPES.get(type);
    if (entity==null) throw DomainError.invalid("Unsupported object type");
    return entity;
  }
  private QueryResult object(DomainContext c, QueryRequest q) {
    if (q.id()==null) throw DomainError.invalid("Object ID required");
    Map<String,Object> row=repository.object(c,entity(q),q.id());
    authorize(c,q.operation(),row);
    Map<String,Object> data=dto(row);
    if ("TradeItems".equals(entity(q))) {
      data.put("itemId",row.get("ID"));
      data.put("externalIdentifiers",repository.rows(c,"ExternalIdentifiers").stream().filter(r -> row.get("ID").equals(r.get("itemId")) && interval(r,c)).map(this::dto).toList());
      data.put("unitConversions",repository.rows(c,"UnitConversions").stream().filter(r -> row.get("ID").equals(r.get("itemId")) && interval(r,c)).map(this::dto).toList());
      data.put("specificationVersion",dto(repository.object(c,"SpecificationVersions",(String)row.get("specificationVersionId"))));
      data.put("packagingVersion",dto(repository.object(c,"PackagingVersions",(String)row.get("packagingVersionId"))));
    }
    if ("LogisticsUnits".equals(entity(q))) {
      var members=repository.rows(c,"LogisticsMemberships").stream().filter(r -> q.id().equals(r.get("logisticsUnitId")) && interval(r,c)).toList();
      data.put("memberships",members.stream().filter(r -> permitted(c,q.operation(),repository.object(c,"QuantitySegments",(String)r.get("segmentId")))).map(this::dto).toList());
    }
    return result(data,q,List.of());
  }
  private QueryResult search(DomainContext c, QueryRequest q) {
    List<Map<String,Object>> available=repository.rows(c,entity(q)).stream().filter(r -> permitted(c,q.operation(),r)).sorted(Comparator.comparing(r -> (String)r.get("ID"))).toList();
    String cursor=ReadCursor.decode(q.cursor(),c,q);
    List<Map<String,Object>> after=available.stream().filter(r -> cursor==null || ((String)r.get("ID")).compareTo(cursor)>0).toList();
    List<Map<String,Object>> page=after.stream().limit(q.limit()).map(this::dto).toList();
    String next=after.size()>q.limit() ? ReadCursor.encode((String)page.getLast().get("ID"),c,q) : null;
    return new QueryResult(page,q.scope(),List.of(),List.of(),List.of(),next);
  }
  private QueryResult inventory(DomainContext c, QueryRequest q) {
    Set<String> allowed=Set.of("itemId","placeId","lotId","objectType");
    if (!allowed.containsAll(q.scope().keySet())) throw DomainError.invalid("Unsupported inventory scope");
    String item=(String)q.scope().get("itemId");
    if (item==null) item=q.id();
    if (item==null) throw DomainError.invalid("Item scope required");
    var itemRow=repository.object(c,"TradeItems",item);
    String chosen=item;
    var rows=repository.rows(c,"QuantitySegments").stream().filter(r -> chosen.equals(r.get("itemId")) && active(r,c) && matches(r,q.scope()) && permitted(c,q.operation(),r)).toList();
    if(rows.isEmpty()) authorize(c,q.operation(),itemRow);
    BigDecimal held=rows.stream().map(r -> (BigDecimal)r.get("quantity")).reduce(BigDecimal.ZERO,BigDecimal::add);
    Map<String,Object> data=new LinkedHashMap<>();
    data.put("itemId",item); data.put("heldQuantity",held.stripTrailingZeros().toPlainString()); data.put("unit",itemRow.get("baseUnit"));
    data.put("eligibleQuantity",null); data.put("reservedQuantity",null); data.put("unreservedEligibleQuantity",null); data.put("cumulativeArrival",null);
    data.put("eligibilityStatus","UNKNOWN"); data.put("segmentIds",rows.stream().map(r -> r.get("ID")).toList()); data.put("segments",rows.stream().map(this::dto).toList());
    return result(data,q,List.of("ELIGIBILITY_NOT_IMPLEMENTED_S1","ALLOCATION_NOT_IMPLEMENTED_S1","CUMULATIVE_ARRIVAL_REQUIRES_CONFIRMED_RECEIPT_S3"));
  }
  private QueryResult trace(DomainContext c, QueryRequest q) {
    if (q.id()==null) throw DomainError.invalid("Trace ID required");
    String type=(String)q.scope().getOrDefault("objectType","QuantitySegment");
    Set<String> frontier=new LinkedHashSet<>();
    if ("ManufacturingLot".equals(type)) {
      var lot=repository.object(c,"ManufacturingLots",q.id()); authorize(c,q.operation(),lot);
      repository.rows(c,"QuantitySegments").stream().filter(r -> q.id().equals(r.get("lotId")) && permitted(c,q.operation(),r)).forEach(r -> frontier.add((String)r.get("ID")));
    } else if ("QuantitySegment".equals(type)) {var s=repository.object(c,"QuantitySegments",q.id()); authorize(c,q.operation(),s); frontier.add(q.id());}
    else throw DomainError.invalid("Trace requires segment or manufacturing lot");
    List<Map<String,Object>> edges=repository.rows(c,"GenealogyEdges").stream().filter(r -> !time(r.get("occurredAt")).isAfter(c.asOf())).toList();
    Map<String,Map<String,Object>> segments=new HashMap<>();
    repository.rows(c,"QuantitySegments").stream().filter(r -> permitted(c,q.operation(),r)).forEach(r -> segments.put((String)r.get("ID"),r));
    Set<String> all=new LinkedHashSet<>(frontier); boolean changed;
    do { changed=false; for(var edge:edges) {String from=(String)edge.get("sourceId"),to=(String)edge.get("targetId"); if (segments.containsKey(from)&&segments.containsKey(to)&&(all.contains(from)||all.contains(to))) {changed|=all.add(from);changed|=all.add(to);}} } while(changed);
    List<Map<String,Object>> visible=edges.stream().filter(r -> all.contains(r.get("sourceId"))&&all.contains(r.get("targetId"))).toList();
    boolean uncertain=visible.stream().anyMatch(r -> Boolean.TRUE.equals(r.get("uncertain"))) || all.stream().map(segments::get).anyMatch(r -> "UNCERTAIN_MIXTURE".equals(r.get("mixtureStatus")));
    var leaves=all.stream().map(segments::get).filter(r -> active(r,c)).toList();
    Map<String,Object> data=new LinkedHashMap<>(); data.put("rootId",q.id()); data.put("segmentIds",List.copyOf(all)); data.put("segments",all.stream().map(segments::get).map(this::dto).toList()); data.put("edges",visible.stream().map(this::dto).toList()); data.put("activeLeafIds",leaves.stream().map(r -> r.get("ID")).toList());
    data.put("impactStatus",uncertain ? "CANDIDATE_UNCERTAIN_MIXTURE" : "LINEAGE_ONLY"); data.put("confirmedDefectImpact",null); data.put("cleanSubsetSelectable",false);
    return result(data,q,uncertain ? List.of("PHYSICAL_SUBSET_NOT_IDENTIFIABLE","DEFECT_IMPACT_REQUIRES_EVIDENCE") : List.of("DEFECT_IMPACT_REQUIRES_EVIDENCE"));
  }
  private boolean matches(Map<String,Object> row,Map<String,Object> scope) {return (!scope.containsKey("placeId")||Objects.equals(scope.get("placeId"),row.get("placeId"))) && (!scope.containsKey("lotId")||Objects.equals(scope.get("lotId"),row.get("lotId")));}
  private boolean active(Map<String,Object> row,DomainContext c) {
    if (time(row.get("validFrom")).isAfter(c.asOf())) return false;
    return row.get("retiredAt")==null || time(row.get("retiredAt")).isAfter(c.asOf()) || time(row.get("retirementRecordedAt")).isAfter(c.knownAt());
  }
  private boolean interval(Map<String,Object> row,DomainContext c) {return !time(row.get("validFrom")).isAfter(c.asOf())&&(row.get("validUntil")==null||time(row.get("validUntil")).isAfter(c.asOf()));}
  private Instant time(Object value) {return value instanceof Instant i ? i : value instanceof java.sql.Timestamp t ? t.toInstant() : Instant.parse(value.toString());}
  private Map<String,Collection<String>> scopes(Map<String,Object> row) {
    Map<String,Collection<String>> candidates=new LinkedHashMap<>();
    candidates.put("TARGET",List.of((String)row.get("ID")));
    // TradeItems carry baseUnit, rather than an itemId foreign key.
    if(row.containsKey("baseUnit")) candidates.put("ITEM",List.of((String)row.get("ID")));
    if(row.get("itemId")!=null) candidates.put("ITEM",List.of((String)row.get("itemId")));
    if(row.get("placeId")!=null) candidates.put("PLACE",List.of((String)row.get("placeId")));
    if(row.containsKey("parentId")&&row.containsKey("kind")) candidates.put("PLACE",List.of((String)row.get("ID")));
    return candidates;
  }
  private void authorize(DomainContext c,String operation,Map<String,Object> row) {authorizer.authorizeScopes(c,operation,scopes(row));}
  private boolean permitted(DomainContext c,String operation,Map<String,Object> row) {return authorizer.permittedScopes(c,operation,scopes(row));}
  private Map<String,Object> dto(Map<String,Object> row) {Map<String,Object> result=new LinkedHashMap<>(); row.forEach((k,v)->result.put(k,v instanceof BigDecimal ? InventoryQuantity.text(v) : v)); return result;}
  private QueryResult result(Object data,QueryRequest q,List<String> unknowns) {return new QueryResult(data,q.scope(),unknowns,List.of(),List.of(),null);}
}
