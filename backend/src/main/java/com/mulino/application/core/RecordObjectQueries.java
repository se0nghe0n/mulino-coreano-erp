package com.mulino.application.core;

import com.sap.cds.ql.Select;
import com.sap.cds.services.persistence.PersistenceService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;

/**
 * S3-3: getObject/searchObjects for the plan's record nouns that had no noun read (plan §3.4 명사 조회, §4.1 catalog
 * Allocation·Restriction·DispositionBasis, §5.3 Obligation·Handover, §6 Dispatch·ReturnAuthorization, §7.3 command record).
 * Read-only: the same organization/knownAt visibility and the same all-of scope authorization as the other noun providers.
 * A row outside the caller's scope, another organization's row and an unknown ID all answer FORBIDDEN.
 */
@Component
public class RecordObjectQueries implements ObjectReadProvider {
  /** noun -> CDS entity. */
  static final Map<String,String> ENTITIES=Map.of(
      "Allocation","mulino.inventory.SegmentAllocations","Restriction","mulino.inventory.Restrictions",
      "DispositionBasis","mulino.inventory.DispositionBases","Dispatch","mulino.inventory.Dispatches",
      "ReturnAuthorization","mulino.trade.returns.Authorizations","Obligation","mulino.responsibility.Roots",
      "Handover","mulino.responsibility.Handovers","CommandRecord","mulino.commands.CommandRecords");
  private final PersistenceService db;private final ReadAuthorizer auth;
  public RecordObjectQueries(PersistenceService db,ReadAuthorizer auth){this.db=db;this.auth=auth;}
  public Set<String> objectTypes(){return ENTITIES.keySet();}

  public QueryResult query(DomainContext c,QueryRequest q){
    Object type=q.scope().getOrDefault("objectType",q.filters().get("type"));
    if(!(type instanceof String noun)||!ENTITIES.containsKey(noun))throw DomainError.invalid("Invalid typed selector");
    if(!Set.of("objectType","organizationId","itemId","workId").containsAll(q.scope().keySet())||!Set.of("type","sort").containsAll(q.filters().keySet()))throw DomainError.invalid("Unsupported record query selector");
    if(q.filters().containsKey("sort")&&!"ID".equals(q.filters().get("sort")))throw DomainError.invalid("Unsupported sort");
    boolean single=q.operation().equals("getObject");if(single&&q.id()==null)throw DomainError.invalid("Object ID required");
    String after=ReadCursor.decode(q.cursor(),c,q);var out=new ArrayList<Map<String,Object>>();
    for(var row:rows(c,noun)){
      String id=row.get("ID").toString();
      if(q.id()!=null&&!q.id().equals(id)||after!=null&&id.compareTo(after)<=0)continue;
      var data=view(c,noun,row);
      if(q.scope().get("itemId")!=null&&!Objects.equals(q.scope().get("itemId"),data.get("itemId")))continue;
      if(q.scope().get("workId")!=null&&!workIds(data).contains(q.scope().get("workId")))continue;
      if(!visibleTo(c,noun,row)||!auth.permittedScopes(c,q.operation(),scopes(noun,data)))continue;
      out.add(data);
    }
    if(single&&out.isEmpty())throw DomainError.forbidden();
    out.sort(Comparator.comparing(x->x.get("ID").toString()));
    boolean more=out.size()>q.limit();var page=out.stream().limit(q.limit()).map(x->(Map<String,Object>)TransportValues.normalize(x)).toList();
    String next=more?ReadCursor.encode(out.get(q.limit()-1).get("ID").toString(),c,q):null;
    var scope=new LinkedHashMap<String,Object>(q.scope());scope.put("organizationId",c.organizationId());
    if(single&&page.getFirst().get("itemId")!=null)scope.put("itemId",page.getFirst().get("itemId"));
    return new QueryResult(single?page.getFirst():page,scope,List.of(),List.of(),List.of(),next);
  }

  @SuppressWarnings({"unchecked","rawtypes"})
  private List<Map<String,Object>> rows(DomainContext c,String noun){
    String entity=ENTITIES.get(noun);String recorded=noun.equals("CommandRecord")?"createdAt":"recordedAt";
    return (List)db.run(Select.from(entity).where(r->r.get("organizationId").eq(c.organizationId()).and(r.get(recorded).le(c.knownAt())))).listOf(Map.class).stream().map(r->new LinkedHashMap<String,Object>((Map<String,Object>)r)).toList();
  }
  @SuppressWarnings({"unchecked","rawtypes"})
  private Optional<Map<String,Object>> one(DomainContext c,String entity,Object id){
    if(id==null)return Optional.empty();
    return db.run(Select.from(entity).where(r->r.get("organizationId").eq(c.organizationId()).and(r.get("ID").eq(id.toString())))).first().map(r->(Map<String,Object>)new LinkedHashMap<String,Object>((Map)r));
  }
  /** Row plus the identifiers the scope check and the noun/verb views share (item, place, works). */
  private Map<String,Object> view(DomainContext c,String noun,Map<String,Object> row){
    var data=new LinkedHashMap<String,Object>(row);data.put("type",noun);
    switch(noun){
      case "Allocation","Restriction","DispositionBasis"->one(c,"mulino.inventory.QuantitySegments",row.get("segmentId")).ifPresent(s->{data.put("itemId",s.get("itemId"));data.put("placeId",s.get("placeId"));});
      case "Dispatch"->data.put("placeId",row.get("destinationId"));
      case "ReturnAuthorization"->data.put("placeId",row.get("destinationId"));
      case "Obligation"->{
        var assignments=new ArrayList<Map<String,Object>>();
        for(var a:workRows(c,"mulino.work.read.ObligationReferences"))if(row.get("ID").equals(a.get("rootId"))&&!Instant.parse(a.get("recordedAt").toString()).isAfter(c.knownAt()))assignments.add(a);
        assignments.sort(Comparator.comparing(x->x.get("ID").toString()));data.put("assignments",assignments);
        var works=new TreeSet<String>();for(var a:assignments)if(a.get("workId")!=null)works.add(a.get("workId").toString());data.put("workIds",List.copyOf(works));
        var items=new TreeSet<String>();for(String w:works)one(c,"mulino.work.read.Works",w).ifPresent(x->items.add(x.get("itemId").toString()));if(items.size()==1)data.put("itemId",items.first());data.put("itemIds",List.copyOf(items));
        assignments.stream().filter(a->"OPEN".equals(a.get("status"))&&!Boolean.FALSE.equals(a.get("valid"))).findFirst().ifPresent(a->{data.put("status","OPEN");data.put("responsibleWorkId",a.get("workId"));data.put("ownerId",a.get("ownerId"));data.put("nextAction",a.get("nextAction"));data.put("nextCheckAt",a.get("nextCheckAt"));});
      }
      case "Handover"->one(c,"mulino.work.read.Works",row.get("workId")).ifPresent(w->data.put("itemId",w.get("itemId")));
      case "CommandRecord"->{data.remove("canonicalIntentJson");data.remove("authorizationScopeJson");data.put("revision",1);}
      default->{}
    }
    data.replaceAll((k,v)->v instanceof BigDecimal n?n.stripTrailingZeros().toPlainString():v);
    return data;
  }
  @SuppressWarnings({"unchecked","rawtypes"})
  private List<Map<String,Object>> workRows(DomainContext c,String entity){return (List)db.run(Select.from(entity).where(r->r.get("organizationId").eq(c.organizationId()))).listOf(Map.class);}
  /** Plan §7.3: a command record is visible only to its own stable request owner; another principal's result is not disclosed. */
  private boolean visibleTo(DomainContext c,String noun,Map<String,Object> row){return !noun.equals("CommandRecord")||Objects.equals(c.stableRequestOwner(),row.get("stableRequestOwner"));}
  @SuppressWarnings("unchecked")
  private static Set<Object> workIds(Map<String,Object> data){var out=new HashSet<Object>();if(data.get("workId")!=null)out.add(data.get("workId"));if(data.get("targetWorkId")!=null)out.add(data.get("targetWorkId"));if(data.get("workIds") instanceof List<?> l)out.addAll(l);return out;}
  @SuppressWarnings("unchecked")
  static Map<String,List<String>> scopes(String noun,Map<String,Object> data){
    var s=new LinkedHashMap<String,List<String>>();var targets=new ArrayList<String>(List.of(data.get("ID").toString()));
    for(String k:List.of("segmentId","rootId","transitSegmentId","rangeRootId","allocationId"))if(data.get(k)!=null&&!targets.contains(data.get(k).toString()))targets.add(data.get(k).toString());
    s.put("TARGET",targets);
    if(data.get("itemIds") instanceof List<?> items&&!items.isEmpty())s.put("ITEM",items.stream().map(Object::toString).toList());else if(data.get("itemId")!=null)s.put("ITEM",List.of(data.get("itemId").toString()));
    var works=workIds(data).stream().map(Object::toString).sorted().toList();if(!works.isEmpty())s.put("WORK",works);
    if(data.get("placeId")!=null)s.put("PLACE",List.of(data.get("placeId").toString()));
    if(data.get("customerId")!=null)s.put("CUSTOMER",List.of(data.get("customerId").toString()));
    return s;
  }
}
