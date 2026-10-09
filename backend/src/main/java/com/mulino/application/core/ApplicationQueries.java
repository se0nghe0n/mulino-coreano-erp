package com.mulino.application.core;

import com.mulino.domain.work.read.WorkReadHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class ApplicationQueries {
  private static final Set<String> WORLD=Set.of("getObject","getWork","getInventory","getObligations","getAssessment","traceLot","getTrace");
  private final Map<String,QueryHandler> handlers=new HashMap<>();
  private final Set<String> worldOperations=new HashSet<>(WORLD);
  private final Set<String> objectTypes=new HashSet<>(Set.of("Product","TradeItem","ManufacturingLot","QuantitySegment","LogisticsUnit","Place","Manufacturer"));
  private final ReadAuthorizer auth;
  private final ExecutionClock clock;
  private final WorkReadHandler work;
  private final ObjectMapper json=new ObjectMapper();
  public ApplicationQueries(List<QueryHandler> handlers,ReadAuthorizer auth,WorkReadHandler work,ExecutionClock clock){this(handlers,auth,work,clock,List.of());}
  @org.springframework.beans.factory.annotation.Autowired
  public ApplicationQueries(List<QueryHandler> handlers,ReadAuthorizer auth,WorkReadHandler work,ExecutionClock clock,List<ObjectReadProvider> objects){
    this.auth=auth;this.work=work;this.clock=clock;
    for(var provider:objects){if(provider instanceof QueryHandler query)worldOperations.addAll(query.operations());}
    for(var provider:objects)for(String type:provider.objectTypes())if(!objectTypes.add(type))throw new IllegalStateException("Duplicate noun read provider "+type);
    for(QueryHandler handler:handlers)for(String operation:handler.operations())if(this.handlers.put(operation,handler)!=null)throw new IllegalStateException("Duplicate query capability "+operation);
  }
  public Set<String> operations(){return Set.copyOf(handlers.keySet());}
  @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
  @SuppressWarnings("unchecked")
  public Map<String,Object> query(QueryRequest request){
    try { return queryInternal(request); }
    catch(org.springframework.security.access.AccessDeniedException denied) { throw DomainError.forbidden(); }
    catch(IllegalArgumentException invalid) { throw DomainError.invalid("Invalid typed query"); }
  }
  @SuppressWarnings("unchecked")
  private Map<String,Object> queryInternal(QueryRequest request){
    QueryHandler handler=handlers.get(request.operation());
    if(handler==null)throw DomainError.unsupported();
    Instant now=clock.instant();
    Instant asOf=request.asOf()==null?now:request.asOf();
    Instant knownAt=request.knownAt()==null?now:request.knownAt();
    DomainContext context=auth.context(asOf,knownAt);
    if(request.scope().containsKey("organizationId")&&!context.organizationId().equals(request.scope().get("organizationId")))throw DomainError.forbidden();
    boolean evidenceOperation=Set.of("getEvidence","getInbox").contains(request.operation());
    Set<String> allowedScope=evidenceOperation?Set.of("organizationId","kind","subjectKind","subjectId"):Set.of("organizationId","itemId","lotId","workId","placeId","customerId","objectType");
    if(!allowedScope.containsAll(request.scope().keySet()))throw DomainError.invalid("Unsupported scope key");
    request.scope().forEach((key,value)->{
      Set<String> enumValues=switch(key){case "objectType"->objectTypes;case "kind"->Set.of("DOCUMENT","EVENT","CLAIM","CANONICAL");case "subjectKind"->Set.of("ITEM","LOT","SEGMENT","WORK","PLACE","PURCHASE_ORDER");default->null;};
      if(enumValues!=null){if(!(value instanceof String)||!enumValues.contains(value))throw DomainError.invalid("Invalid typed selector");return;}
      if(!(value instanceof String))throw DomainError.invalid("Typed scope ID required");try{UUID.fromString((String)value);}catch(IllegalArgumentException invalid){throw DomainError.invalid("UUID scope ID required");}
    });
    auth.authorize(context,request.operation(),null);
    QueryResult result=handler.query(context,request);
    Map<String,Object> scope=new LinkedHashMap<>(result.scope());
    if(scope.containsKey("organizationId")&&!Objects.equals(scope.get("organizationId"),context.organizationId()))throw DomainError.forbidden();
    scope.put("organizationId",context.organizationId());
    Object data=result.data();
    if(data instanceof Map<?,?> object&&object.get("itemId")!=null)scope.putIfAbsent("itemId",object.get("itemId"));
    TreeSet<String> evidence=new TreeSet<>(result.evidenceRefs());
    TreeSet<String> unknowns=new TreeSet<>(result.unknowns());
    TreeSet<String> conflicts=new TreeSet<>(result.conflicts());
    Object shared=data;
    Map<String,Object> snapshotScope=scope;
    if(worldOperations.contains(request.operation())&&data instanceof Map<?,?>&&scope.get("itemId")!=null){
      var worldScope=new LinkedHashMap<String,Object>();for(String key:List.of("organizationId","itemId","lotId","placeId","customerId"))if(scope.containsKey(key))worldScope.put(key,scope.get(key));
      snapshotScope=worldScope;
      Map<String,Object> world=work.world(context,worldScope);
      if(handlers.containsKey("getInventory")){
        var inventoryScope=new LinkedHashMap<String,Object>();for(String key:List.of("organizationId","itemId","lotId","placeId","customerId"))if(scope.containsKey(key))inventoryScope.put(key,scope.get(key));
        // The world projection of getInventory itself keeps the request's eligibility action (plan §122 행동별 적격량).
        var eligibilityFilters=new LinkedHashMap<String,Object>();if(request.operation().equals("getInventory")&&request.filters().containsKey("action"))eligibilityFilters.put("action",request.filters().get("action"));
        QueryRequest inventory=new QueryRequest("getInventory",null,inventoryScope,eligibilityFilters,200,null,request.definitionVersion(),asOf,knownAt,null);
        auth.authorize(context,"getInventory",null);
        QueryResult inventoryResult=handlers.get("getInventory").query(context,inventory);
        if(inventoryResult.data() instanceof Map<?,?> inventoryData)world.putAll((Map<String,Object>)inventoryData);
        evidence.addAll(inventoryResult.evidenceRefs());unknowns.addAll(inventoryResult.unknowns());conflicts.addAll(inventoryResult.conflicts());
      }
      if(scope.get("itemId")!=null)world.put("itemId",scope.get("itemId"));
      evidence.addAll((List<String>)world.getOrDefault("evidenceRefs",List.of()));
      world.put("evidenceRefs",List.copyOf(evidence));
      Map<String,Object> merged=new LinkedHashMap<>((Map<String,Object>)data);merged.putAll(world);data=merged;shared=world;
    }
    String snapshot=snapshot(context,snapshotScope,shared);
    if(request.snapshotRef()!=null&&!request.snapshotRef().equals(snapshot))throw new DomainError("CONFLICT","SNAPSHOT_CHANGED","Read snapshot changed; repeat the query");
    Map<String,Object> envelope=new LinkedHashMap<>();
    envelope.put("data",TransportValues.normalize(data));envelope.put("snapshotRevision",snapshot);envelope.put("asOf",asOf.toString());envelope.put("knownAt",knownAt.toString());envelope.put("scope",scope);
    envelope.put("unknowns",List.copyOf(unknowns));envelope.put("conflicts",List.copyOf(conflicts));envelope.put("evidenceRefs",List.copyOf(evidence));envelope.put("nextCursor",result.nextCursor());
    return envelope;
  }
  private String snapshot(DomainContext c,Map<String,Object> scope,Object state){
    try{
      String raw=c.organizationId()+"|"+c.actorId()+"|"+c.asOf()+"|"+c.knownAt()+"|"+json.writeValueAsString(TransportValues.normalize(scope))+"|"+json.writeValueAsString(TransportValues.normalize(state));
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
    }catch(Exception failure){throw new IllegalStateException(failure);}
  }
}
