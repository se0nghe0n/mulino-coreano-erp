package com.mulino.application.evaluation;
import com.mulino.application.core.*;
import com.mulino.domain.evaluation.AssessmentRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class AssessmentQueries implements QueryHandler {
 private final AssessmentRepository repository;private final ReadAuthorizer auth;
 public AssessmentQueries(AssessmentRepository repository,ReadAuthorizer auth){this.repository=repository;this.auth=auth;}
 public Set<String> operations(){return Set.of("getAssessment");}
 @Transactional(readOnly=true) public QueryResult query(DomainContext c,QueryRequest q){
  if(q.id()==null||!q.filters().isEmpty()||!Set.of("organizationId","workId").containsAll(q.scope().keySet()))throw DomainError.invalid("Assessment identifier required");
  var a=repository.rows(c,"mulino.work.read.AssessmentReferences").stream().filter(x->q.id().equals(x.get("ID"))).findFirst().orElseThrow(DomainError::forbidden);
  if(q.scope().get("workId")!=null&&!q.scope().get("workId").equals(a.get("workId")))throw DomainError.forbidden();
  auth.authorizeScopes(c,"getAssessment",Map.of("WORK",List.of(a.get("workId").toString())));
  var output=new LinkedHashMap<>(a);var snapshots=repository.rows(c,"mulino.evaluation.InputSnapshots").stream().filter(x->a.get("ID").equals(x.get("assessmentId"))).toList();output.put("inputSnapshots",snapshots);
  return new QueryResult(output,Map.of("workId",a.get("workId")),Boolean.TRUE.equals(a.get("held"))?List.of("PINNED_CONTRACT_UNAVAILABLE"):List.of(),Boolean.TRUE.equals(a.get("conflict"))?List.of("EVIDENCE_CONFLICT"):List.of(),List.of(),null);
 }
}
