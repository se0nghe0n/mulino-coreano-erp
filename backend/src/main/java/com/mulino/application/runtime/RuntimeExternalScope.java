package com.mulino.application.runtime;

import com.mulino.application.core.*;
import com.mulino.application.identity.IdentityAuthorization;
import com.mulino.domain.evidence.ExternalOperationScopePort;
import com.mulino.domain.runtime.RuntimeRepository;
import java.util.*;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class RuntimeExternalScope implements ExternalOperationScopePort {
 private final RuntimeRepository repository;private final IdentityAuthorization auth;private final ObjectMapper json=new ObjectMapper();
 public RuntimeExternalScope(RuntimeRepository repository,IdentityAuthorization auth){this.repository=repository;this.auth=auth;}
 @Override public Map<String,Object> require(DomainContext c,String operationId){
  try{UUID.fromString(operationId);}catch(RuntimeException invalid){throw DomainError.invalid("External operation UUID required");}
  var rows=repository.db().queryForList("SELECT * FROM mulino_runtime_Outbox WHERE organizationId=? AND externalOperationId=?",c.organizationId(),operationId);
  if(rows.size()!=1)throw DomainError.forbidden();var row=rows.getFirst();var payload=json.readValue((String)row.get("payloadjson"),Map.class);
  if(!(payload.get("workId") instanceof String work))throw DomainError.invalid("External operation lacks canonical Work");
  // The review command already fences and authorizes WORK and SOURCE; no write elevation here.

  return Map.of("ID",operationId,"workId",work,"commandId",row.get("commandid"),"operation",row.get("operation"),"status",row.get("status"),"actorId",row.get("actorid"));
 }
}
