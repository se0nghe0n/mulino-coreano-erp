package com.mulino.application.runtime;

import com.mulino.application.core.*;
import com.mulino.application.work.WorkAccess;
import com.mulino.domain.identity.IdentityRepository;
import java.util.*;
import org.springframework.stereotype.Service;

/** Independent receipts recover from DB, retaining original intake responsibility. */
@Service
public class RuntimeIntakeProcessor {
 private final RuntimeIntakeService service;private final RuntimeIntakeLinker linker;private final IdentityRepository identities;private final ExecutionClock clock;private final ApplicationCommands commands;private final RuntimeRecoveryExecution execution;private final WorkAccess works;
 public RuntimeIntakeProcessor(RuntimeIntakeService service,RuntimeIntakeLinker linker,IdentityRepository identities,ExecutionClock clock,ApplicationCommands commands,RuntimeRecoveryExecution execution,WorkAccess works){this.service=service;this.linker=linker;this.identities=identities;this.clock=clock;this.commands=commands;this.execution=execution;this.works=works;}
 public int recoverDue(){int processed=0;
  for(var index:service.due()){
   String owner=(String)index.get("intakeownerid"),org=(String)index.get("organizationid");var actor=identities.actor(org,owner).orElse(null);
   if(actor==null){service.hold(index,"OWNER_UNAVAILABLE","Supervisor must restore the designated intake owner");continue;}
   var c=new DomainContext(org,owner,(String)actor.get("stableRequestOwner"),clock.instant(),clock.instant());
   try{
    var proof=linker.rule(c,index);service.policy(index,proof.policy().get("ID").toString(),proof.policy().get("contentHash").toString());
    if(Boolean.FALSE.equals(proof.rule().get("requiresResponse"))){linker.noResponse(c,index);processed++;continue;}
    String work=index.get("createdworkid")==null?proof.event().get("workid")==null?"WORK".equals(proof.event().get("subjectkind"))?Objects.toString(proof.event().get("subjectid"),null):null:proof.event().get("workid").toString():index.get("createdworkid").toString();
    if(work==null)work=createConfiguredWork(c,index,proof);
    if(work==null)continue;
    var actual=works.require(c,work,false);
    if("DRAFT".equals(actual.get("status"))){var result=execution.execute(c,actual,"activateWork","intake:"+index.get("id")+":activate");if(!"APPLIED".equals(result.get("outcome")))throw new DomainError("HELD","INTAKE_ACTIVATION_HELD","Configured response work cannot activate");}
    linker.link(c,index,work);processed++;
   }catch(org.springframework.security.access.AccessDeniedException denied){service.hold(index,"CURRENT_AUTHORITY_DENIED","Intake owner/supervisor must restore current scope authority before linking response");}
   catch(DomainError held){service.hold(index,held.code(),"Retain intake owner; verify current policy, published goal slots and actual response linkage");}
  }
  return processed;
 }
 private String createConfiguredWork(DomainContext c,Map<String,Object> index,RuntimeIntakeLinker.Rule proof){
  if(!(proof.rule().get("workTemplate") instanceof Map<?,?> template)||!(template.get("goal") instanceof Map<?,?> goal)||!(template.get("definitionVersionId") instanceof String definition)||!(template.get("kind") instanceof String kind))throw new DomainError("HELD","POLICY_UNRESOLVED","Configured published goal template required");
  Object item=proof.event().get("itemid");if(item==null&&"ITEM".equals(proof.event().get("subjectkind")))item=proof.event().get("subjectid");if(item==null)throw DomainError.invalid("Actual intake item required");
  var slots=new LinkedHashMap<String,Object>();slots.put("itemId",Map.of("type","ITEM","id",item));if(proof.event().get("subjectkind").equals("LOT"))slots.put("lotId",Map.of("type","LOT","id",proof.event().get("subjectid")));
  slots.put("definitionVersionId",Map.of("type","DEFINITION","id",definition));slots.put("kind",Map.of("type","STRING","value",kind));slots.put("ownerId",Map.of("type","ACTOR","id",index.get("intakeownerid")));slots.put("supervisorId",Map.of("type","ACTOR","id",index.get("supervisorid")));slots.put("goal",goal);
  var intent=new LinkedHashMap<String,Object>();intent.put("intentKind","COMMAND");intent.put("capabilityId","createDraft");intent.put("capabilityVersion","1.0.0");intent.put("definitionVersion",definition);intent.put("commandIdempotencyKey","intake:"+index.get("id")+":create");intent.put("subjectRefs",List.of());intent.put("slots",slots);var provenance=new LinkedHashMap<String,String>();slots.keySet().forEach(k->provenance.put(k,"APPROVED_DEFAULT"));intent.put("provenance",provenance);
  var claim=service.claimCreate(c,index,intent);if(claim.isEmpty())return null;
  var result=commands.executeClaimed(intent,claim.get());if(!"APPLIED".equals(result.get("outcome")))throw new DomainError("HELD","INTAKE_CREATION_HELD","Configured response work cannot be created");
  if(!(result.get("effects") instanceof Map<?,?> effects)||!(effects.get("workId") instanceof String work))throw DomainError.invalid("Actual created Work reference required");service.created(index,work);return work;
 }
}
