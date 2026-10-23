package com.mulino.domain.work.read;

import com.mulino.application.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class WorkReadHandler implements QueryHandler {
  private final WorkReadRepository repository;
  private final ReadAuthorizer auth;
  private final ObjectMapper json=new ObjectMapper();
  private final org.springframework.beans.factory.ObjectProvider<com.mulino.application.work.WorkAssessmentRead> assessmentRead;
  public WorkReadHandler(WorkReadRepository repository,ReadAuthorizer auth,org.springframework.beans.factory.ObjectProvider<com.mulino.application.work.WorkAssessmentRead> assessmentRead) {this.repository=repository;this.auth=auth;this.assessmentRead=assessmentRead;}
  @org.springframework.beans.factory.annotation.Autowired
  private org.springframework.beans.factory.ObjectProvider<com.mulino.application.inventory.InventoryQueries> inventoryRead;
  @org.springframework.beans.factory.annotation.Autowired
  private org.springframework.beans.factory.ObjectProvider<com.mulino.application.inventory.WorkSupplyRead> supplyRead;
  public Set<String> operations(){return Set.of("getWork","searchWorks","getObligations","getAssessment");}
  public QueryResult query(DomainContext c,QueryRequest q){
    // Action-scoped supply assessment keeps the shared noun/Work world snapshot.
    // Only immutable goal assessments use the dedicated read port.
    if(q.operation().equals("getAssessment")&&!q.filters().containsKey("action")){
      var reader=assessmentRead.getIfAvailable();if(reader==null)throw DomainError.unsupported();
      return reader.query(c,q);
    }
    if(!Set.of("itemId","lotId","workId","status","sort","action","customerId").containsAll(q.filters().keySet()))throw DomainError.invalid("Unsupported work filter");
    if(q.filters().containsKey("sort")&&!"ID".equals(q.filters().get("sort")))throw DomainError.invalid("Only stable ID sort supported");
    var works=authorizedWorks(c,q.scope(),q.filters(),q.operation());
    if(!q.operation().equals("searchWorks")) {
      if(q.id()==null)throw DomainError.invalid("Work ID required");
      var work=works.stream().filter(w->q.id().equals(w.get("ID"))).findFirst().orElseThrow(DomainError::forbidden);
      auth.authorizeScopes(c,q.operation(),workScopes(work));
      Map<String,Object> data=new LinkedHashMap<>(work);
      data.put("workId",q.id());
      var goals=linked(c,"GoalReferences",Set.of(q.id()));data.put("goals",goals);goals.stream().filter(g->Objects.equals(g.get("ID"),work.get("currentGoalVersionId"))).findFirst().ifPresent(g->{var current=new LinkedHashMap<String,Object>(g);current.put("version",g.get("goalVersion"));data.put("currentGoal",current);});
      data.put("assessments",linked(c,"AssessmentReferences",Set.of(q.id())));
      data.put("obligations",obligations(c,Set.of(q.id())));
      data.put("workLinks",workLinks(c,Set.of(q.id())));data.put("contributions",contributions(c,Set.of(q.id())));
      if(assessmentRead.getIfAvailable()!=null)data.put("assessmentInputSnapshots",assessmentSnapshots(c,q.id()));
      if(q.operation().equals("getAssessment")&&q.filters().containsKey("action")&&supplyRead!=null&&supplyRead.getIfAvailable()!=null)data.putAll(supplyRead.getObject().read(c,scope(c,q.scope(),work),q.filters()));
      if(work.get("waitJson")!=null)data.put("wait",decode(work.get("waitJson")));
      data.remove("waitJson");
      return QueryResult.of(data,scope(c,q.scope(),work));
    }
    String after=ReadCursor.decode(q.cursor(),c,q);
    var page=works.stream().filter(w->after==null||String.valueOf(w.get("ID")).compareTo(after)>0).limit(q.limit()+1L).toList();
    boolean more=page.size()>q.limit();
    var rows=more?page.subList(0,q.limit()):page;
    String cursor=more?ReadCursor.encode(String.valueOf(rows.getLast().get("ID")),c,q):null;
    return new QueryResult(rows,scope(c,q.scope(),null),List.of(),List.of(),List.of(),cursor);
  }
  public Map<String,Object> world(DomainContext c,Map<String,Object> scope){
    var works=authorizedWorks(c,scope,Map.of(),"getWork");
    Set<String> ids=new TreeSet<>(); works.forEach(w->ids.add(String.valueOf(w.get("ID"))));
    var obligations=obligations(c,ids);
    TreeSet<String> owners=new TreeSet<>(),next=new TreeSet<>(),evidence=new TreeSet<>();
    works.forEach(w->owners.add(String.valueOf(w.get("ownerId"))));
    obligations.stream().filter(o->"OPEN".equals(o.get("status"))&&!Boolean.FALSE.equals(o.get("valid"))).forEach(o->{owners.add(String.valueOf(o.get("ownerId")));next.add(String.valueOf(o.get("nextAction")));});
    for(var ref:linked(c,"EvidenceReferences",ids)){
      if(!repository.documentKnown(c,String.valueOf(ref.get("documentVersionId"))))continue;
      try{var parent=works.stream().filter(w->Objects.equals(w.get("ID"),ref.get("workId"))).findFirst().orElseThrow();auth.authorizeScopes(c,"getEvidence",Map.of("TARGET",List.of(String.valueOf(ref.get("documentVersionId"))),"WORK",List.of(String.valueOf(ref.get("workId"))),"ITEM",List.of(String.valueOf(parent.get("itemId")))));evidence.add(String.valueOf(ref.get("documentVersionId")));}catch(DomainError denied){if(!denied.code().equals("FORBIDDEN"))throw denied;}
    }
    Map<String,Object> data=new LinkedHashMap<>();
    data.put("workIds",List.copyOf(ids));data.put("ownerIds",List.copyOf(owners));data.put("nextActions",List.copyOf(next));data.put("evidenceRefs",List.copyOf(evidence));data.put("obligations",obligations);
    data.put("workLinks",workLinks(c,ids));data.put("contributions",contributions(c,ids));
    if(inventoryRead!=null&&inventoryRead.getIfAvailable()!=null&&scope.containsKey("itemId")){
      var inventory=inventoryRead.getObject().query(c,new QueryRequest("getInventory",null,scope,Map.of(),200,null,null,c.asOf(),c.knownAt(),null));
      evidence.addAll(inventory.evidenceRefs());data.put("evidenceRefs",List.copyOf(evidence));
    }
    data.put("workReferences",works);data.put("goalReferences",linked(c,"GoalReferences",ids));data.put("assessmentReferences",linked(c,"AssessmentReferences",ids));
    if(assessmentRead.getIfAvailable()!=null)data.put("assessmentInputSnapshots",ids.stream().flatMap(id->assessmentSnapshots(c,id).stream()).toList());
    return data;
  }
  private List<Map<String,Object>> authorizedWorks(DomainContext c,Map<String,Object> scope,Map<String,Object> filters,String capability){
    return repository.rows("Works",c).stream().filter(w->matches(c,w,scope)&&matches(c,w,filters)).filter(w->{
      try{auth.authorizeScopes(c,capability,workScopes(w));return true;}catch(DomainError denied){if(!denied.code().equals("FORBIDDEN"))throw denied;return false;}
    }).toList();
  }
  private Map<String,List<String>> workScopes(Map<String,Object> w){return Map.of("TARGET",List.of(String.valueOf(w.get("ID"))),"WORK",List.of(String.valueOf(w.get("ID"))),"ITEM",List.of(String.valueOf(w.get("itemId"))));}
  private boolean matches(DomainContext c,Map<String,Object> w,Map<String,Object> filter){
    if(filter.containsKey("lotId")&&!Objects.equals(w.get("lotId"),filter.get("lotId"))&&!repository.rows("SubjectLinks",c).stream().anyMatch(link->Objects.equals(w.get("ID"),link.get("workId"))&&Objects.equals(w.get("itemId"),link.get("itemId"))&&Objects.equals(filter.get("lotId"),link.get("lotId"))))return false;
    for(String key:List.of("itemId","status"))if(filter.containsKey(key)&&!Objects.equals(w.get(key),filter.get(key)))return false;
    if(filter.containsKey("workId")&&!Objects.equals(w.get("ID"),filter.get("workId")))return false;
    return true;
  }
  private List<Map<String,Object>> workLinks(DomainContext c,Set<String> ids){var available=new HashSet<String>();authorizedWorks(c,Map.of(),Map.of(),"getWork").forEach(w->available.add(String.valueOf(w.get("ID"))));return repository.rows("WorkLinks",c).stream().filter(r->ids.contains(String.valueOf(r.get("sourceWorkId")))||ids.contains(String.valueOf(r.get("targetWorkId")))).filter(r->available.contains(String.valueOf(r.get("targetWorkId")))&&(r.get("sourceWorkId")==null||available.contains(String.valueOf(r.get("sourceWorkId"))))).toList();}
  private List<Map<String,Object>> contributions(DomainContext c,Set<String> ids){
  var available=new HashSet<String>();authorizedWorks(c,Map.of(),Map.of(),"getWork").forEach(w->available.add(w.get("ID").toString()));
  var links=repository.rows("WorkLinks",c);var out=new ArrayList<Map<String,Object>>();
  for(var raw:repository.rows("WorkContributions",c)){
   var link=links.stream().filter(l->Objects.equals(raw.get("linkId"),l.get("ID"))).findFirst();
   if(link.isEmpty()||!available.contains(Objects.toString(raw.get("targetWorkId")))||link.get().get("sourceWorkId")!=null&&!available.contains(link.get().get("sourceWorkId").toString()))continue;
   if(ids.contains(Objects.toString(raw.get("targetWorkId")))){var row=new LinkedHashMap<>(raw);row.put("attributionRole","TARGET");row.put("boundGoalId",raw.get("goalId"));row.put("boundConditionId",raw.get("conditionId"));out.add(row);}
   if("CONTRIBUTES_TO".equals(link.get().get("kind"))&&ids.contains(Objects.toString(link.get().get("sourceWorkId")))){var row=new LinkedHashMap<>(raw);row.put("attributionRole","SOURCE");row.put("attributionState",raw.get("sourceGoalVersionId")==null?"UNKNOWN":"PINNED");row.put("boundGoalId",raw.get("sourceGoalVersionId"));row.put("boundConditionId",raw.get("sourceConditionId"));out.add(row);}
  }
  return out;
 }
 private List<Map<String,Object>> linked(DomainContext c,String entity,Set<String> ids){return repository.rows(entity,c).stream().filter(r->ids.contains(String.valueOf(r.get("workId")))).map(r->{var data=new LinkedHashMap<String,Object>(r);if(entity.equals("GoalReferences")){if(data.get("slotsJson")!=null)data.put("slots",decode(data.remove("slotsJson")));if(data.get("provenanceJson")!=null)data.put("provenance",decode(data.remove("provenanceJson")));data.put("scope",decode(data.remove("scopeJson")));}if(entity.equals("AssessmentReferences")&&data.get("conditionsJson")!=null)data.put("conditionResults",decodeAny(data.remove("conditionsJson")));return (Map<String,Object>)data;}).toList();}
  private List<Map<String,Object>> assessmentSnapshots(DomainContext c,String workId){try{return assessmentRead.getIfAvailable().snapshots(c,workId);}catch(DomainError denied){if(!denied.code().equals("FORBIDDEN"))throw denied;return List.of();}}
  private Object decodeAny(Object value){try{return json.readValue(String.valueOf(value),Object.class);}catch(Exception failure){throw new IllegalStateException("Invalid assessment condition results",failure);}}
  private List<Map<String,Object>> obligations(DomainContext c,Set<String> ids){return linked(c,"ObligationReferences",ids).stream().map(r->{Map<String,Object> result=new LinkedHashMap<>(r);result.put("scope",decode(result.remove("scopeJson")));result.put("responsibleWorkId",result.get("workId"));return result;}).toList();}
  private Object decode(Object value){try{return json.readValue(String.valueOf(value),Map.class);}catch(Exception failure){throw new IllegalStateException("Invalid stored read reference",failure);}}
  private Map<String,Object> scope(DomainContext c,Map<String,Object> input,Map<String,Object> work){Map<String,Object> scope=new LinkedHashMap<>(input);scope.put("organizationId",c.organizationId());if(work!=null){scope.putIfAbsent("itemId",work.get("itemId"));if(work.get("lotId")!=null)scope.putIfAbsent("lotId",work.get("lotId"));}return scope;}
}
