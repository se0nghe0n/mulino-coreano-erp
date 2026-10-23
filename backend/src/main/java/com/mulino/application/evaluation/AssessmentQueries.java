package com.mulino.application.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mulino.application.core.*;
import com.mulino.domain.evaluation.AssessmentRepository;
import com.mulino.domain.work.read.WorkReadRepository;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Immutable assessment reads. Evaluating a goal is exclusively assessGoal's job. */
@Service
public class AssessmentQueries implements com.mulino.application.work.WorkAssessmentRead {
 private final AssessmentRepository repository;
 private final WorkReadRepository works;
 private final ReadAuthorizer auth;
 private final ObjectMapper json=new ObjectMapper();
 public AssessmentQueries(AssessmentRepository repository,WorkReadRepository works,ReadAuthorizer auth){this.repository=repository;this.works=works;this.auth=auth;}
 /** Optional getWork sub-read: a denied read must not poison the shared read transaction. */
 @Transactional(readOnly=true,noRollbackFor=DomainError.class)
 public List<Map<String,Object>> snapshots(DomainContext c,String workId){
  var work=requireWork(c,workId);
  authorize(c,work);
  return repository.rows(c,"mulino.evaluation.InputSnapshots").stream().filter(x->workId.equals(x.get("workId"))&&visible(c,x)).toList();
 }
 @Transactional(readOnly=true)
 public QueryResult query(DomainContext c,QueryRequest q){
  if(!Set.of("organizationId","workId","itemId","lotId").containsAll(q.scope().keySet())||
     !Set.of("workId","itemId","lotId","status","sort").containsAll(q.filters().keySet()))throw DomainError.invalid("Unsupported assessment selector");
  if(q.filters().containsKey("sort")&&!"ID".equals(q.filters().get("sort")))throw DomainError.invalid("Only stable ID sort supported");
  if(q.scope().containsKey("organizationId")&&!c.organizationId().equals(q.scope().get("organizationId")))throw DomainError.forbidden();
  var rows=repository.rows(c,"mulino.work.read.AssessmentReferences").stream().filter(a->visible(c,a)).toList();
  var explicit=q.id()==null?Optional.<Map<String,Object>>empty():rows.stream().filter(a->q.id().equals(a.get("ID"))).findFirst();
  String workId=explicit.map(a->String.valueOf(a.get("workId"))).orElseGet(()->q.id()!=null?q.id():Objects.toString(q.scope().getOrDefault("workId",q.filters().get("workId")),null));
  if(workId==null)throw DomainError.invalid("Assessment or Work identifier required");
  var work=requireWork(c,workId);
  for(var selectors:List.of(q.scope(),q.filters()))for(String key:List.of("workId","itemId","lotId","status")){
   Object actual=key.equals("workId")?work.get("ID"):work.get(key);
   boolean linkedLot=key.equals("lotId")&&works.rows("SubjectLinks",c).stream().anyMatch(link->workId.equals(link.get("workId"))&&Objects.equals(work.get("itemId"),link.get("itemId"))&&Objects.equals(selectors.get(key),link.get("lotId")));
   if(selectors.containsKey(key)&&!Objects.equals(actual,selectors.get(key))&&!linkedLot)throw DomainError.forbidden();
  }
  authorize(c,work);
  Map<String,Object> assessment=explicit.orElse(null);
  if(assessment==null){
   var candidates=rows.stream().filter(a->workId.equals(a.get("workId"))&&Objects.equals(work.get("currentGoalVersionId"),a.get("goalId"))).toList();
   var previous=new HashSet<Object>();candidates.forEach(a->{if(a.get("previousAssessmentId")!=null)previous.add(a.get("previousAssessmentId"));});
   var heads=candidates.stream().filter(a->!previous.contains(a.get("ID"))).toList();
   if(heads.size()>1)throw new DomainError("HELD","ASSESSMENT_CONFLICT","Immutable assessment chain has multiple visible heads");
   if(!heads.isEmpty())assessment=heads.getFirst();
  }
  var scope=new LinkedHashMap<String,Object>(q.scope());scope.put("organizationId",c.organizationId());scope.put("workId",workId);scope.put("itemId",work.get("itemId"));
  var unknowns=new ArrayList<String>();var conflicts=new ArrayList<String>();
  var data=new LinkedHashMap<String,Object>();
  if(assessment==null){
   data.put("workId",workId);data.put("goalId",work.get("currentGoalVersionId"));data.put("pending",true);unknowns.add("ASSESSMENT_PENDING");
  }else{
   data.putAll(assessment);
   try{data.put("conditionResults",json.readValue(Objects.toString(data.remove("conditionsJson"),"[]"),Object.class));}
   catch(Exception failure){throw new IllegalStateException("Invalid stored assessment conditions",failure);}
   data.put("inputSnapshots",repository.rows(c,"mulino.evaluation.InputSnapshots").stream().filter(s->Objects.equals(data.get("ID"),s.get("assessmentId"))&&visible(c,s)).toList());
   boolean pending=explicit.isEmpty()&&Boolean.TRUE.equals(work.get("pendingInvalidation"));data.put("pending",pending);
   if(pending)unknowns.add("ASSESSMENT_PENDING");
   if(Boolean.TRUE.equals(assessment.get("held")))unknowns.add("PINNED_CONTRACT_UNAVAILABLE");
   if(Boolean.TRUE.equals(assessment.get("conflict")))conflicts.add("EVIDENCE_CONFLICT");
  }
  var evidence=new TreeSet<String>();
  if(data.get("conditionResults") instanceof List<?> conditions)for(Object condition:conditions)
   if(condition instanceof Map<?,?> entry&&entry.get("evidenceRefs") instanceof List<?> refs)
    refs.forEach(ref->evidence.add(String.valueOf(ref)));
  return new QueryResult(data,scope,unknowns,conflicts,List.copyOf(evidence),null);
 }
 private Map<String,Object> requireWork(DomainContext c,String id){return works.rows("Works",c).stream().filter(w->id.equals(w.get("ID"))).findFirst().orElseThrow(DomainError::forbidden);}
 private void authorize(DomainContext c,Map<String,Object> work){auth.authorizeScopes(c,"getAssessment",Map.of("TARGET",List.of(work.get("ID").toString()),"WORK",List.of(work.get("ID").toString()),"ITEM",List.of(work.get("itemId").toString())));}
 private boolean visible(DomainContext c,Map<String,Object> row){return atOrBefore(row.get("effectiveAt"),c.asOf())&&atOrBefore(row.get("asOf"),c.asOf())&&atOrBefore(row.get("knownAt"),c.knownAt());}
 private boolean atOrBefore(Object value,Instant boundary){return value==null||!(value instanceof Instant i?i:Instant.parse(value.toString())).isAfter(boundary);}
}
