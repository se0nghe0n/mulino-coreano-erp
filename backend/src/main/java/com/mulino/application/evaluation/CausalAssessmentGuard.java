package com.mulino.application.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mulino.application.core.*;
import com.mulino.application.work.WorkAccess;
import com.mulino.domain.evaluation.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** Server-only purpose selection. A RECORD cannot turn assessCreated into a public assessment grant. */
@Component
public class CausalAssessmentGuard {
 private final AssessmentRepository repository;private final WorkAccess works;private final ReadAuthorizer authority;
 public CausalAssessmentGuard(AssessmentRepository repository,WorkAccess works,ReadAuthorizer authority){this.repository=repository;this.works=works;this.authority=authority;}
 public CanonicalFactAdmission.Purpose purpose(DomainContext c,String workId){
  String commandId;try{commandId=CommandExecution.commandId();}catch(IllegalStateException absent){return CanonicalFactAdmission.Purpose.ASSESS_GOAL;}
  var command=repository.currentCommand(c,commandId);var intent=decode(command.get("canonicalIntentJson"));
  if(!"RECORD".equals(intent.get("intentKind")))return CanonicalFactAdmission.Purpose.ASSESS_GOAL;
  String capability=Objects.toString(command.get("capabilityId"),"");
  if(!Set.of("recordDelivery","recordObservedMovement").contains(capability)||!"IN_PROGRESS".equals(command.get("state"))||!"RECORD".equals(command.get("effectClass"))||!commandId.equals(command.get("ID"))||!c.actorId().equals(command.get("actorId"))||!c.stableRequestOwner().equals(command.get("stableRequestOwner"))||!c.organizationId().equals(command.get("organizationId"))||!capability.equals(intent.get("capabilityId"))||!Objects.equals(CommandRequests.hash(intent),command.get("canonicalHash")))throw DomainError.forbidden();
  var scopes=decode(command.get("authorizationScopeJson"));var work=works.require(c,workId,true);
  if(!"COMMAND".equals(work.get("lifecycleMode"))||!"DELIVERY".equals(work.get("kind"))||!Set.of("ACTIVE","WAITING").contains(work.get("status"))||!(scopes.get("WORK") instanceof List<?> workScope)||!workScope.contains(workId)||!(scopes.get("ITEM") instanceof List<?> itemScope)||!itemScope.contains(work.get("itemId")))throw DomainError.forbidden();
  var goal=works.currentGoal(c,workId);var goalSlots=decode(goal.get("slotsJson"));
  if(!Objects.equals(work.get("currentGoalVersionId"),goal.get("ID"))||!workId.equals(goal.get("workId"))||!Objects.equals(work.get("definitionVersionId"),goal.get("definitionVersionId"))||!"DELIVERED".equals(goal.get("endpoint"))||!"DELIVERED".equals(goalSlots.get("endpoint"))||!"PHYSICAL_DELIVERY".equals(goalSlots.get("eventKind")))throw DomainError.forbidden();
  if(!(intent.get("slots") instanceof Map<?,?> slots)||!repository.hasCausalDelivery(c,workId,commandId,slots))throw DomainError.forbidden();
  var currentScopes=new LinkedHashMap<String,List<String>>();for(var entry:scopes.entrySet()){if(!(entry.getValue() instanceof List<?> values)||values.stream().anyMatch(v->!(v instanceof String)))throw DomainError.forbidden();currentScopes.put(entry.getKey(),values.stream().map(Object::toString).toList());}
  authority.authorizeScopes(c,capability,currentScopes);
  var matching=Arrays.stream(CanonicalFactAdmission.Purpose.values()).filter(p->capability.equals(p.capability())).toList();
  if(matching.size()!=1)throw DomainError.unsupported();return matching.getFirst();
 }
 private Map<String,Object> decode(Object value){try{return new ObjectMapper().readValue(value.toString(),Map.class);}catch(Exception invalid){throw DomainError.forbidden();}}
}
