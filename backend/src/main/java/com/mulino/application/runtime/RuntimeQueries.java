package com.mulino.application.runtime;

import com.mulino.application.core.*;
import com.mulino.domain.runtime.RuntimeRepository;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Component
public class RuntimeQueries implements QueryHandler {
 private final RuntimeRepository repository;private final ReadAuthorizer auth;private final ObjectMapper json=new ObjectMapper();
 public RuntimeQueries(RuntimeRepository repository,ReadAuthorizer auth){this.repository=repository;this.auth=auth;}
 public Set<String> operations(){return Set.of("searchOperationalIssues");}
 @Transactional(readOnly=true) public QueryResult query(DomainContext c,QueryRequest request){
  if(!Set.of("workId","organizationId").containsAll(request.scope().keySet())||!request.filters().isEmpty()||request.cursor()!=null)throw DomainError.invalid("Unsupported runtime issue selector");
  if(request.scope().containsKey("organizationId")&&!c.organizationId().equals(request.scope().get("organizationId")))throw DomainError.forbidden();
  var issues=new ArrayList<Map<String,Object>>();
  for(var row:repository.db().queryForList("SELECT * FROM mulino_runtime_Outbox WHERE organizationId=? AND status IN ('UNKNOWN_EXTERNAL','EXHAUSTED') ORDER BY nextCheckAt,ID",c.organizationId())){
   var payload=json.readValue((String)row.get("payloadjson"),Map.class);String work=(String)payload.get("workId");
   if(work==null||request.scope().containsKey("workId")&&!work.equals(request.scope().get("workId"))||!auth.permittedScopes(c,"searchOperationalIssues",Map.of("WORK",List.of(work))))continue;
   var duty=repository.db().queryForList("SELECT a.ownerId,a.supervisorId,a.nextAction,a.nextCheckAt FROM mulino_responsibility_Roots r JOIN mulino_work_read_ObligationReferences a ON a.organizationId=r.organizationId AND a.rootId=r.ID WHERE r.organizationId=? AND r.sourceId=? AND r.kind='EXTERNAL_RECONCILIATION' AND a.valid AND a.status='OPEN'",c.organizationId(),row.get("commandid"));
   var issue=new LinkedHashMap<String,Object>();issue.put("ID",row.get("id"));issue.put("workId",work);issue.put("status",row.get("status"));issue.put("externalOperationId",row.get("externaloperationid"));issue.put("nextAction",row.get("nextaction"));issue.put("nextCheckAt",row.get("nextcheckat"));issue.put("responsibility",duty);issues.add(issue);if(issues.size()>=request.limit())break;
  }
  for(var row:repository.db().queryForList("SELECT * FROM mulino_runtime_RecoverySchedules WHERE organizationId=? AND (nextCheckAt<=? OR pendingAssessment=true OR recoveryStatus='HELD_MANUAL') ORDER BY nextCheckAt,ID",c.organizationId(),RuntimeRepository.at(c.knownAt()))){String work=(String)row.get("workid");if(request.scope().containsKey("workId")&&!work.equals(request.scope().get("workId"))||!auth.permittedScopes(c,"searchOperationalIssues",Map.of("WORK",List.of(work))))continue;if(issues.size()>=request.limit())break;issues.add(Map.of("ID",row.get("id"),"workId",work,"status",row.get("recoverystatus"),"ownerId",row.get("ownerid"),"supervisorId",row.get("supervisorid"),"nextAction",row.get("nextaction"),"nextCheckAt",row.get("nextcheckat")));}
  for(var row:repository.db().queryForList("SELECT * FROM mulino_runtime_IntakeRecoveries WHERE organizationId=? AND state NOT IN ('LINKED','NO_RESPONSE') ORDER BY nextCheckAt,ID",c.organizationId())){if(issues.size()>=request.limit())break;if(!auth.permittedScopes(c,"searchOperationalIssues",Map.of("SOURCE",List.of(row.get("sourceprofileid").toString()))))continue;issues.add(Map.of("ID",row.get("id"),"kind","UNLINKED_INTAKE","status",row.get("state"),"ownerId",row.get("intakeownerid"),"supervisorId",row.get("supervisorid"),"nextAction",row.get("nextaction"),"nextCheckAt",row.get("nextcheckat")));}
  return QueryResult.of(issues,Map.of("organizationId",c.organizationId()));
 }
}
