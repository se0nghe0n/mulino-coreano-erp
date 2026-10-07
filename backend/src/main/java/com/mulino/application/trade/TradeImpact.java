package com.mulino.application.trade;

import com.mulino.application.core.*;
import com.mulino.application.work.WorkAccess;
import com.mulino.application.responsibility.ResponsibilityService;
import com.mulino.domain.responsibility.ResponsibilityRepository;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;

/** Trusted domain hook, inside the gateway/sweeper transaction, never a public command. */
@Component
public class TradeImpact {
  private final WorkAccess work;
  private final ResponsibilityService duties;
  private final ResponsibilityRepository repository;
  public TradeImpact(WorkAccess work,ResponsibilityService duties,ResponsibilityRepository repository){this.work=work;this.duties=duties;this.repository=repository;}

  public Map<String,Object> recorded(DomainContext context,String workId,String sourceId,
      String kind,String physicalScopeId,String nextAction,Instant nextCheckAt){
    return recorded(context,workId,sourceId,kind,physicalScopeId,nextAction,nextCheckAt,CommandExecution.commandId());
  }

  /** sourceId must include immutable decision version/expiry boundary for sweeper retries. */
  public Map<String,Object> recorded(DomainContext context,String workId,String sourceId,
      String kind,String physicalScopeId,String nextAction,Instant nextCheckAt,String sourceCommandId){
    if(sourceId==null||sourceId.isBlank()||kind==null||kind.isBlank()||nextAction==null||nextAction.isBlank()||nextCheckAt==null)throw DomainError.invalid("Exact trade impact and next check required");
    repository.fence(context.organizationId(),workId);
    var source=repository.requireSource(context.organizationId(),"DECISION",sourceCommandId);
    var target=work.require(context,workId,true);
    if("CLOSED".equals(target.get("status")))target=work.ensureFollowup(context,workId,sourceId,kind,nextAction,nextCheckAt);
    var scope=new TreeMap<String,Object>();scope.put("originalWorkId",workId);scope.put("domainSourceId",sourceId);
    if(physicalScopeId!=null)scope.put("physicalScopeId",physicalScopeId);
    String scopeJson;try{scopeJson=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(scope);}catch(Exception e){throw new IllegalStateException(e);}
    String version=source.get("canonicalHash")!=null?source.get("canonicalHash").toString():String.valueOf(source.getOrDefault("revision",0));
    var result=duties.openDuty(context,Map.of("workId",target.get("ID"),"sourceId",sourceCommandId,"sourceKind","DECISION","sourceVersion",version,"kind",kind,"scopeJson",scopeJson,"nextAction",nextAction,"nextCheckAt",nextCheckAt));
    if(!result.containsKey("assignments"))work.markInvalidation(context,target.get("ID").toString(),true);
    return result;
  }

  /** Confirmed facts change the assessment input without inventing a response duty. */
  public void invalidate(DomainContext context,String workId){work.require(context,workId,true);work.markInvalidation(context,workId,true);}
}
