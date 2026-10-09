package com.mulino.application.inventory;

import com.mulino.application.core.*;
import com.mulino.domain.inventory.*;
import com.mulino.application.trade.InventoryReadFacts;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class InventoryQueries implements QueryHandler {
  private static final Map<String,String> TYPES = Map.ofEntries(Map.entry("Product","Products"),Map.entry("TradeItem","TradeItems"),Map.entry("ManufacturingLot","ManufacturingLots"),Map.entry("QuantitySegment","QuantitySegments"),Map.entry("LogisticsUnit","LogisticsUnits"),Map.entry("Place","Places"),Map.entry("Manufacturer","Manufacturers"));
  private final InventoryRepository repository;
  private final ReadAuthorizer authorizer;
  private final List<InventoryReadFacts> facts;
  private final Map<String,ObjectReadProvider> objects=new HashMap<>();
  public InventoryQueries(InventoryRepository repository, ReadAuthorizer authorizer) { this(repository,authorizer,List.of(),List.of()); }
  @org.springframework.beans.factory.annotation.Autowired
  public InventoryQueries(InventoryRepository repository, ReadAuthorizer authorizer,List<InventoryReadFacts> facts,List<ObjectReadProvider> providers) {
    this.repository=repository; this.authorizer=authorizer;this.facts=List.copyOf(facts);
    for(var provider:providers)for(String type:provider.objectTypes())if(TYPES.containsKey(type)||objects.put(type,provider)!=null)throw new IllegalStateException("Duplicate noun read provider "+type);
    var owned=new HashSet<String>();for(var provider:facts)for(String metric:provider.metrics())if(!Set.of("eligibleQuantity","reservedQuantity","unreservedEligibleQuantity","cumulativeArrival","eligibilityStatus","allocationShortageQuantity","reservationResponsibilityQuantity").contains(metric)||!owned.add(metric))throw new IllegalStateException("Invalid or duplicate inventory metric "+metric);
  }
  public Set<String> operations() { return Set.of("getObject","searchObjects","getInventory","getTrace","traceLot"); }
  public QueryResult query(DomainContext c, QueryRequest q) {
    if(q.scope().containsKey("organizationId")&&!c.organizationId().equals(q.scope().get("organizationId")))throw DomainError.forbidden();
    if(q.scope().containsKey("objectType")&&q.filters().containsKey("type")&&!Objects.equals(q.scope().get("objectType"),q.filters().get("type")))throw DomainError.invalid("Conflicting object types");
    if(Set.of("getObject","searchObjects").contains(q.operation())){if(!Set.of("type","sort").containsAll(q.filters().keySet()))throw DomainError.invalid("Unsupported object filter");if(q.filters().containsKey("sort")&&!"ID".equals(q.filters().get("sort")))throw DomainError.invalid("Unsupported object sort");Object type=q.scope().getOrDefault("objectType",q.filters().get("type"));var provider=objects.get(type);if(provider!=null)return provider.query(c,q);}
    // Plan §122 행동별 적격량: getInventory reads eligibility for one action (default SELL) and optionally one customer.
    if (!(q.operation().equals("getInventory")?Set.of("type","sort","action","customerId"):Set.of("type","sort")).containsAll(q.filters().keySet())) throw DomainError.invalid("Unsupported inventory filter");
    if (q.filters().containsKey("sort")&&!"ID".equals(q.filters().get("sort"))) throw DomainError.invalid("Unsupported inventory sort");
    if(q.scope().containsKey("organizationId")&&!c.organizationId().equals(q.scope().get("organizationId"))) throw DomainError.forbidden();
    return switch(q.operation()) {
      case "getObject" -> object(c,q);
      case "searchObjects" -> search(c,q);
      case "getInventory" -> inventory(c,q);
      case "getTrace" -> trace(c,q);
      case "traceLot" -> {var scope=new LinkedHashMap<String,Object>(q.scope()); scope.put("objectType","ManufacturingLot"); yield trace(c,new QueryRequest(q.operation(),q.id()==null?(String)scope.get("lotId"):q.id(),scope,q.filters(),q.limit(),q.cursor(),q.definitionVersion(),q.asOf(),q.knownAt(),q.snapshotRef()));}
      default -> throw DomainError.unsupported();
    };
  }
  private String entity(QueryRequest q) {
    String type = (String)q.scope().get("objectType");
    if(type==null) type=(String)q.filters().get("type");
    else if(q.filters().containsKey("type")&&!type.equals(q.filters().get("type"))) throw DomainError.invalid("Conflicting object types");
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
    data.put("relations",repository.rows(c,"ObjectRelations").stream().filter(r -> q.id().equals(r.get("sourceId")) && interval(r,c)).filter(r -> permitted(c,q.operation(),repository.object(c,TYPES.get((String)r.get("targetType")),(String)r.get("targetId")))).map(this::dto).toList());
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
    Set<String> allowed=Set.of("itemId","placeId","lotId","objectType","organizationId","customerId");
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
    var unknowns=new ArrayList<String>();var conflicts=new ArrayList<String>();var evidence=new ArrayList<String>();var implemented=new HashSet<String>();
    var factScope=eligibilityScope(q);
    for(var provider:facts){var extra=provider.read(c,q.operation(),item,factScope,rows);if(!extra.values().keySet().equals(provider.metrics()))throw new IllegalStateException("Inventory metric contract differs from installed provider");data.putAll(extra.values());implemented.addAll(provider.metrics());unknowns.addAll(extra.unknowns());conflicts.addAll(extra.conflicts());evidence.addAll(extra.evidenceRefs());}
    if(!implemented.contains("eligibleQuantity"))unknowns.add("ELIGIBILITY_NOT_IMPLEMENTED_S1");
    if(!implemented.containsAll(Set.of("reservedQuantity","unreservedEligibleQuantity")))unknowns.add("ALLOCATION_NOT_IMPLEMENTED_S1");
    if(!implemented.contains("cumulativeArrival"))unknowns.add("CUMULATIVE_ARRIVAL_REQUIRES_CONFIRMED_RECEIPT_S3");
    var scope=new LinkedHashMap<String,Object>(q.scope());scope.put("itemId",item);if(factScope.get("customerId")!=null)scope.put("customerId",factScope.get("customerId"));
    return new QueryResult(data,scope,unknowns,conflicts,evidence,null);
  }
  /** Eligibility inputs of getInventory: action filter (an upper-case action token, default SELL) and customer (filter or scope, not conflicting). */
  private static Map<String,Object> eligibilityScope(QueryRequest q) {
    var out=new LinkedHashMap<String,Object>(q.scope());
    Object action=q.filters().getOrDefault("action","SELL");
    if(!(action instanceof String a)||!a.matches("[A-Z][A-Z_]{0,39}"))throw DomainError.invalid("Invalid eligibility action");
    out.put("action",action);
    Object customer=q.filters().get("customerId");
    if(customer!=null){
      if(!(customer instanceof String id))throw DomainError.invalid("Typed customer ID required");
      try{UUID.fromString(id);}catch(IllegalArgumentException invalid){throw DomainError.invalid("UUID customer ID required");}
      if(out.containsKey("customerId")&&!customer.equals(out.get("customerId")))throw DomainError.invalid("Conflicting customer scope");
      out.put("customerId",customer);
    }
    return out;
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
  private QueryResult result(Object data,QueryRequest q,List<String> unknowns) {var scope=new LinkedHashMap<String,Object>(q.scope());if(data instanceof Map<?,?> map&&map.get("itemId")!=null)scope.put("itemId",map.get("itemId"));return new QueryResult(data,scope,unknowns,List.of(),List.of(),null);}
}
