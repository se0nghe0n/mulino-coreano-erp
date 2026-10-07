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
  public Set<String> operations(){return Set.of("getWork","searchWorks","getObligations","getAssessment");}
  public QueryResult query(DomainContext c,QueryRequest q){
    if(!Set.of("itemId","lotId","workId","status","sort","action","customerId").containsAll(q.filters().keySet()))throw DomainError.invalid("Unsupported work filter");
    if(q.filters().containsKey("sort")&&!"ID".equals(q.filters().get("sort")))throw DomainError.invalid("Only stable ID sort supported");
    var works=authorizedWorks(c,q.scope(),q.filters(),q.operation());
    if(!q.operation().equals("searchWorks")) {
      if(q.id()==null)throw DomainError.invalid("Work ID required");
      var work=works.stream().filter(w->q.id().equals(w.get("ID"))).findFirst().orElseThrow(DomainError::forbidden);
      auth.authorizeScopes(c,q.operation(),workScopes(work));
      Map<String,Object> data=new LinkedHashMap<>(work);
      data.put("workId",q.id());
      data.put("goals",linked(c,"GoalReferences",Set.of(q.id())));
      data.put("assessments",linked(c,"AssessmentReferences",Set.of(q.id())));
      data.put("obligations",obligations(c,Set.of(q.id())));
      if(assessmentRead.getIfAvailable()!=null)data.put("assessmentInputSnapshots",assessmentRead.getIfAvailable().snapshots(c,q.id()));
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
    obligations.forEach(o->{owners.add(String.valueOf(o.get("ownerId")));next.add(String.valueOf(o.get("nextAction")));});
    for(var ref:linked(c,"EvidenceReferences",ids)){
      if(!repository.documentKnown(c,String.valueOf(ref.get("documentVersionId"))))continue;
      try{var parent=works.stream().filter(w->Objects.equals(w.get("ID"),ref.get("workId"))).findFirst().orElseThrow();auth.authorizeScopes(c,"getEvidence",Map.of("TARGET",List.of(String.valueOf(ref.get("documentVersionId"))),"WORK",List.of(String.valueOf(ref.get("workId"))),"ITEM",List.of(String.valueOf(parent.get("itemId")))));evidence.add(String.valueOf(ref.get("documentVersionId")));}catch(DomainError denied){if(!denied.code().equals("FORBIDDEN"))throw denied;}
    }
    Map<String,Object> data=new LinkedHashMap<>();
    data.put("workIds",List.copyOf(ids));data.put("ownerIds",List.copyOf(owners));data.put("nextActions",List.copyOf(next));data.put("evidenceRefs",List.copyOf(evidence));data.put("obligations",obligations);
    data.put("workReferences",works);data.put("goalReferences",linked(c,"GoalReferences",ids));data.put("assessmentReferences",linked(c,"AssessmentReferences",ids));
    if(assessmentRead.getIfAvailable()!=null)data.put("assessmentInputSnapshots",ids.stream().flatMap(id->assessmentRead.getIfAvailable().snapshots(c,id).stream()).toList());
    return data;
  }
  private List<Map<String,Object>> authorizedWorks(DomainContext c,Map<String,Object> scope,Map<String,Object> filters,String capability){
    return repository.rows("Works",c).stream().filter(w->matches(w,scope)&&matches(w,filters)).filter(w->{
      try{auth.authorizeScopes(c,capability,workScopes(w));return true;}catch(DomainError denied){if(!denied.code().equals("FORBIDDEN"))throw denied;return false;}
    }).toList();
  }
  private Map<String,List<String>> workScopes(Map<String,Object> w){return Map.of("TARGET",List.of(String.valueOf(w.get("ID"))),"WORK",List.of(String.valueOf(w.get("ID"))),"ITEM",List.of(String.valueOf(w.get("itemId"))));}
  private boolean matches(Map<String,Object> w,Map<String,Object> filter){
    for(String key:List.of("itemId","lotId","status"))if(filter.containsKey(key)&&!Objects.equals(w.get(key),filter.get(key)))return false;
    if(filter.containsKey("workId")&&!Objects.equals(w.get("ID"),filter.get("workId")))return false;
    return true;
  }
  private List<Map<String,Object>> linked(DomainContext c,String entity,Set<String> ids){return repository.rows(entity,c).stream().filter(r->ids.contains(String.valueOf(r.get("workId")))).toList();}
  private List<Map<String,Object>> obligations(DomainContext c,Set<String> ids){return linked(c,"ObligationReferences",ids).stream().map(r->{Map<String,Object> result=new LinkedHashMap<>(r);result.put("scope",decode(result.remove("scopeJson")));return result;}).toList();}
  private Object decode(Object value){try{return json.readValue(String.valueOf(value),Map.class);}catch(Exception failure){throw new IllegalStateException("Invalid stored read reference",failure);}}
  private Map<String,Object> scope(DomainContext c,Map<String,Object> input,Map<String,Object> work){Map<String,Object> scope=new LinkedHashMap<>(input);scope.put("organizationId",c.organizationId());if(work!=null){scope.putIfAbsent("itemId",work.get("itemId"));if(work.get("lotId")!=null)scope.putIfAbsent("lotId",work.get("lotId"));}return scope;}
}
