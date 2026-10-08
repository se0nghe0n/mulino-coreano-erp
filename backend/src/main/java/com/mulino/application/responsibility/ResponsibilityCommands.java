package com.mulino.application.responsibility;
import com.mulino.application.core.*;
import com.mulino.application.identity.IdentityAuthorization;
import com.mulino.application.policy.PolicyCommandGuard;
import com.mulino.application.work.WorkAccess;
import com.mulino.domain.governance.ApprovalRepository;
import com.mulino.domain.identity.IdentityRepository;
import com.mulino.domain.responsibility.ResponsibilityRepository;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;
@Component
public class ResponsibilityCommands implements CommandHandler {
 private final ResponsibilityService service;private final ResponsibilityRepository repository;private final WorkAccess works;
 private final ApprovalRepository approvals;private final PolicyCommandGuard policy;private final IdentityRepository identities;private final IdentityAuthorization auth;private final ExecutionClock clock;
 public ResponsibilityCommands(ResponsibilityService s,ResponsibilityRepository r,WorkAccess w,ApprovalRepository approvals,PolicyCommandGuard policy,IdentityRepository identities,IdentityAuthorization auth,ExecutionClock clock){service=s;repository=r;works=w;this.approvals=approvals;this.policy=policy;this.identities=identities;this.auth=auth;this.clock=clock;}
 private static final Set<String> ASSIGNMENT_COMMANDS=Set.of("resolveObligation","waiveObligation","decideQuantityDutyWaiver","decideQualityDutyWaiver","decideRecallDutyWaiver");
 public Set<String> capabilities(){return Set.of("createObligation","openObligation","resolveObligation","waiveObligation","transferObligation","proposeHandover","acceptHandover","rejectHandover","expireHandover","emergencyReassign","decideQuantityDutyWaiver","decideQualityDutyWaiver","decideRecallDutyWaiver");}
 @SuppressWarnings("unchecked") private Map<String,Object> slots(Map<String,Object> i){if(!(i.get("slots") instanceof Map))throw DomainError.invalid("Typed slots required");return (Map<String,Object>)i.get("slots");}
 public List<SubjectBinding> subjectBindings(DomainContext c,Map<String,Object> intent,CommandPreparation preparation){
  var p=slots(intent);String capability=intent.get("capabilityId").toString();var workIds=new TreeSet<String>();var assignmentIds=new TreeSet<String>();var handoverIds=new TreeSet<String>();
  if(Set.of("acceptHandover","rejectHandover","expireHandover").contains(capability)){
   var handover=repository.require("Handovers",c.organizationId(),p.get("handoverId").toString());handoverIds.add(handover.get("ID").toString());workIds.add(handover.get("workId").toString());if(handover.get("targetWorkId")!=null)workIds.add(handover.get("targetWorkId").toString());if(handover.get("assignmentId")!=null)assignmentIds.add(handover.get("assignmentId").toString());
  }else if(ASSIGNMENT_COMMANDS.contains(capability)){
   var assignment=repository.require("Assignments",c.organizationId(),p.get("assignmentId").toString());assignmentIds.add(assignment.get("ID").toString());workIds.add(assignment.get("workId").toString());
  }else{
   String sourceWork=p.get("workId").toString();workIds.add(sourceWork);
   if("transferObligation".equals(capability)){var assignment=repository.require("Assignments",c.organizationId(),p.get("assignmentId").toString());if(!sourceWork.equals(assignment.get("workId")))throw DomainError.forbidden();assignmentIds.add(assignment.get("ID").toString());workIds.add(p.get("targetWorkId").toString());}
  }
  var itemIds=new TreeSet<String>();for(String workId:workIds)itemIds.add(works.require(c,workId,false).get("itemId").toString());
  var bindings=new ArrayList<SubjectBinding>();bindings.add(SubjectBinding.optional("Work",workIds));bindings.add(SubjectBinding.optional("TradeItem",itemIds));if(!assignmentIds.isEmpty())bindings.add(SubjectBinding.optional("Obligation",assignmentIds));if(!handoverIds.isEmpty())bindings.add(SubjectBinding.optional("Handover",handoverIds));return List.copyOf(bindings);
 }
 public CommandPreparation prepare(DomainContext c,Map<String,Object>i){var p=slots(i);String cap=i.get("capabilityId").toString();String workId;Map<String,Object> subject;
 if(Set.of("acceptHandover","rejectHandover","expireHandover").contains(cap)){subject=repository.require("Handovers",c.organizationId(),p.get("handoverId").toString());workId=subject.get("workId").toString();}
 else if(ASSIGNMENT_COMMANDS.contains(cap)){subject=repository.require("Assignments",c.organizationId(),p.get("assignmentId").toString());workId=subject.get("workId").toString();}
 else{workId=p.get("workId").toString();subject=works.require(c,workId,false);}
 String approvalAction=null,proposalId=null;int proposalRevision=0;String effect="RESPONSIBILITY";
 if("waiveObligation".equals(cap)){
  if(p.containsKey("evidenceId"))throw DomainError.invalid("Waiver decision is the guarded approvalId, never a slot");
  // A kind without a waiver authority is held effect-free in execute after authorization.
  if(ObligationClosureCatalog.closure(String.valueOf(subject.get("kind"))).waivable()){approvalAction="WAIVE_"+subject.get("kind");proposalId=subject.get("rootId").toString();proposalRevision=1;}
 }
 if(ObligationClosureCatalog.DECISION_CAPABILITIES.contains(cap))effect="RESPONSIBILITY_DECISION";
 if("emergencyReassign".equals(cap)){approvalAction="EMERGENCY_REASSIGN";proposalId=p.get("proposalId").toString();proposalRevision=((Number)p.get("proposalRevision")).intValue();}
 var scopes=new LinkedHashMap<String,List<String>>();String targetWork=p.get("targetWorkId")==null?(subject.get("targetWorkId")==null?workId:subject.get("targetWorkId").toString()):p.get("targetWorkId").toString();
 scopes.put("WORK",Set.of("acceptHandover","rejectHandover").contains(cap)?List.of(targetWork):targetWork.equals(workId)?List.of(workId):List.of(workId,targetWork));var actors=new HashSet<String>();if(p.get("recipientId")!=null)actors.add(p.get("recipientId").toString());if(subject.get("recipientId")!=null)actors.add(subject.get("recipientId").toString());for(String wid:new TreeSet<>(List.of(workId,targetWork))){var w=works.require(c,wid,false);actors.add(w.get("ownerId").toString());actors.add(w.get("supervisorId").toString());}return new CommandPreparation(scopes,new TreeSet<>(List.of("work:"+workId,"work:"+targetWork,"responsibility:"+workId)).stream().toList(),actors,effect,approvalAction,proposalId,proposalRevision,subject.get("ID").toString(),((Number)subject.get("revision")).intValue());}
 public Map<String,Object> execute(DomainContext c,Map<String,Object>i){var p=slots(i);String cap=i.get("capabilityId").toString();var result=switch(cap){
 case "emergencyReassign"->service.emergency(c,p);case "createObligation", "openObligation"->service.openDuty(c,p);
 case "resolveObligation"->{var a=repository.require("Assignments",c.organizationId(),p.get("assignmentId").toString());if(!ObligationClosureCatalog.closure(String.valueOf(a.get("kind"))).resolvable())throw ObligationClosureCatalog.held(a,cap);yield service.settle(c,p,false);}
 case "waiveObligation"->{var a=repository.require("Assignments",c.organizationId(),p.get("assignmentId").toString());if(!ObligationClosureCatalog.closure(String.valueOf(a.get("kind"))).waivable())throw ObligationClosureCatalog.held(a,cap);if(!(i.get("approvalId") instanceof String approval))throw DomainError.forbidden();var decision=approvals.find(c,approval).orElseThrow(DomainError::forbidden);if(!designatedAuthority(identities,clock.instant(),c.organizationId(),String.valueOf(decision.get("approverId")),String.valueOf(decision.get("decisionCapability")),a.get("workId").toString()))throw new DomainError("REJECTED","FORBIDDEN","Waiver decider is no longer the designated authority");yield service.waive(c,p,approval);}
 case "decideQuantityDutyWaiver","decideQualityDutyWaiver","decideRecallDutyWaiver"->decideWaiver(c,i,p,cap);
 case "transferObligation"->service.propose(c,p,true);case "proposeHandover"->service.propose(c,p,false);case "acceptHandover"->service.decide(c,p.get("handoverId").toString(),"ACCEPTED");case "rejectHandover"->service.decide(c,p.get("handoverId").toString(),"REJECTED");case "expireHandover"->service.decide(c,p.get("handoverId").toString(),"EXPIRED");default->throw DomainError.unsupported();};return Map.of("outcome","APPLIED","effects",Map.of("responsibility",result));}
 /**
  * Typed per-kind waiver decision (plan §5.3, §7.1): the designated human of the kind's authority
  * class binds an immutable approval to the exact waiveObligation intent hash, its WORK scope hash,
  * the duty root, the assignment revision, the current COMMAND policy hash, validity and single use.
  * Negative decisions are kept. The guard re-verifies all bindings when the waiver executes.
  */
 private Map<String,Object> decideWaiver(DomainContext c,Map<String,Object> i,Map<String,Object> p,String cap){
  if(!Set.of("assignmentId","decision","reason","validUntil").containsAll(p.keySet()))throw DomainError.invalid("Unsupported waiver decision slot");
  String assignmentId=text(p,"assignmentId"),decision=text(p,"decision"),reason=text(p,"reason");
  if(!Set.of("APPROVE","REJECT").contains(decision))throw DomainError.invalid("Waiver decision must be APPROVE or REJECT");
  Instant until;try{until=Instant.parse(text(p,"validUntil"));}catch(java.time.format.DateTimeParseException e){throw DomainError.invalid("validUntil instant required");}
  if(!until.isAfter(clock.instant()))throw DomainError.invalid("Waiver decision already expired");
  var a=repository.require("Assignments",c.organizationId(),assignmentId);repository.fence(c.organizationId(),a.get("rootId").toString());a=repository.require("Assignments",c.organizationId(),assignmentId);
  if(!"OPEN".equals(a.get("status"))||!Boolean.TRUE.equals(a.get("valid")))throw DomainError.invalid("Only an open current duty can be waived");
  var closure=ObligationClosureCatalog.closure(String.valueOf(a.get("kind")));
  if(!closure.waivable())throw ObligationClosureCatalog.held(a,"waiveObligation");
  if(!closure.waiverCapability().equals(cap))throw new DomainError("REJECTED","FORBIDDEN","Waiver of this duty kind requires the "+closure.authorityClass()+" decision");
  String workId=a.get("workId").toString();
  designated(c,cap,workId);
  var selection=policy.rule(c,"waiveObligation");String action="WAIVE_"+a.get("kind");
  if(!(selection.rule().get("approvalActions") instanceof Map<?,?> mapped)||!cap.equals(mapped.get(action)))throw new DomainError("HELD","POLICY_UNRESOLVED","Current policy does not assign this waiver decision");
  var waive=new LinkedHashMap<String,Object>();waive.put("intentKind","COMMAND");waive.put("definitionVersion",i.get("definitionVersion"));waive.put("capabilityId","waiveObligation");waive.put("capabilityVersion",semanticVersion());
  waive.put("subjectRefs",i.get("subjectRefs"));waive.put("slots",Map.of("assignmentId",assignmentId,"reason",reason));waive.put("provenance",i.get("provenance"));waive.put("expectedRevision",((Number)a.get("revision")).intValue());
  for(String optional:List.of("evidenceRefs","sourceRefs","contextRefs","conditions"))if(i.get(optional)!=null)waive.put(optional,i.get(optional));
  String hash=CommandRequests.hash(waive);var waivePreparation=prepare(c,waive);
  if(!action.equals(waivePreparation.approvalAction()))throw new IllegalStateException("Waiver preparation drift");
  var row=new LinkedHashMap<String,Object>();
  row.put("proposalId",waivePreparation.proposalId());row.put("proposalRevision",waivePreparation.proposalRevision());row.put("canonicalHash",hash);row.put("scopeHash",PolicyCommandGuard.scopeHash(waivePreparation.scopes()));
  row.put("targetId",assignmentId);row.put("targetRevision",waivePreparation.currentRevision());row.put("action",action);row.put("policyHash",selection.policyHash());
  row.put("decision","APPROVE".equals(decision)?"APPROVED":"REJECTED");row.put("decisionCapability",cap);row.put("decidedAt",clock.instant());row.put("expiresAt",until);row.put("singleUse",true);
  String approvalId=approvals.create(c,row);
  var result=new LinkedHashMap<String,Object>();result.put("approvalId",approvalId);result.put("decision",row.get("decision"));result.put("assignmentId",assignmentId);result.put("kind",a.get("kind"));result.put("authorityClass",closure.authorityClass());
  result.put("waiverIntentHash",hash);result.put("targetRevision",row.get("targetRevision"));result.put("expiresAt",until.toString());result.put("reason",reason);result.put("singleUse",true);
  return result;
 }
 /** The decider is a current human with the kind's decision capability in scope and a designated management authority. */
 private void designated(DomainContext c,String cap,String workId){
  if(!"HUMAN".equals(identities.actor(c.organizationId(),c.actorId()).orElseThrow(DomainError::forbidden).get("kind")))throw DomainError.forbidden();
  if(!auth.permittedScopes(c,cap,Map.of("WORK",List.of(workId))))throw DomainError.forbidden();
  if(!designatedAuthority(identities,clock.instant(),c.organizationId(),c.actorId(),cap,workId))throw new DomainError("REJECTED","FORBIDDEN","Designated waiver authority required");
 }
 static boolean designatedAuthority(IdentityRepository identities,Instant now,String org,String actor,String cap,String workId){
  return identities.rows("ManagementAuthorities",org).stream().anyMatch(x->actor.equals(x.get("actorId"))&&cap.equals(x.get("capabilityId"))&&active(x,now)&&("ORGANIZATION".equals(x.get("scopeKind"))&&org.equals(x.get("scopeId"))||"WORK".equals(x.get("scopeKind"))&&workId.equals(x.get("scopeId"))));
 }
 private static boolean active(Map<String,Object> x,Instant now){return x.get("validFrom")!=null&&x.get("validUntil")!=null&&!now.isBefore(at(x.get("validFrom")))&&now.isBefore(at(x.get("validUntil")))&&(x.get("revokedAt")==null||now.isBefore(at(x.get("revokedAt"))));}
 private static Instant at(Object v){return v instanceof Instant t?t:v instanceof java.sql.Timestamp s?s.toInstant():Instant.parse(v.toString());}
 private static String text(Map<String,Object> m,String k){Object v=m.get(k);if(v==null||v.toString().isBlank())throw DomainError.invalid("Missing "+k);return v.toString();}
}
