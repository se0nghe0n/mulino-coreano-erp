package com.mulino.application.responsibility;

import com.mulino.application.core.*;
import com.mulino.application.identity.IdentityAuthorization;
import com.mulino.application.work.*;
import com.mulino.domain.identity.IdentityRepository;
import com.mulino.domain.responsibility.ResponsibilityRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;

/** Invoked only inside the common command or runtime transaction, never commits independently. */
@Service
public class ResponsibilityService implements WorkResponsibility, com.mulino.application.runtime.IntakeDutyPort, com.mulino.application.runtime.RuntimeDutyPort {
 @org.springframework.beans.factory.annotation.Autowired private com.mulino.application.evaluation.GoalShortfallProof shortfalls;
 @org.springframework.beans.factory.annotation.Autowired private WorkFollowupSource followupSources;
 private final ResponsibilityRepository r; private final WorkAccess work;
 private final ExecutionClock clock; private final IdentityRepository identities; private final IdentityAuthorization auth; private final ResponsibilityEvidence evidence;
 public ResponsibilityService(ResponsibilityRepository r,@org.springframework.context.annotation.Lazy WorkAccess work,IdentityRepository identities,IdentityAuthorization auth,ResponsibilityEvidence evidence,ExecutionClock clock){this.r=r;this.work=work;this.identities=identities;this.auth=auth;this.evidence=evidence;this.clock=clock;}
 private static String text(Map<String,Object> m,String k){Object v=m.get(k);if(v==null||v.toString().isBlank())throw DomainError.invalid("Missing "+k);return v.toString();}
 private static Instant at(Object v){return v instanceof Instant i?i:Instant.parse(v.toString());}
 private static BigDecimal quantity(Object v){BigDecimal q=new BigDecimal(v.toString());if(q.signum()<=0||q.scale()>12||q.precision()>38)throw DomainError.invalid("Invalid duty quantity");return q;}
 private void human(DomainContext c,String id){if(!"HUMAN".equals(identities.actor(c.organizationId(),id).orElseThrow(DomainError::forbidden).get("kind")))throw DomainError.invalid("Human owner required");if(!identities.rows("Memberships",c.organizationId()).stream().anyMatch(x->id.equals(x.get("actorId"))&&active(x,clock.instant())))throw DomainError.invalid("Inactive human responsibility");}
 private static boolean active(Map<String,Object>x,Instant now){return x.get("validFrom")!=null&&x.get("validUntil")!=null&&!now.isBefore(at(x.get("validFrom")))&&now.isBefore(at(x.get("validUntil")))&&(x.get("revokedAt")==null||now.isBefore(at(x.get("revokedAt"))));}
 private Map<String,Object> row(DomainContext c,String id){var m=new LinkedHashMap<String,Object>();m.put("organizationId",c.organizationId());m.put("ID",id);m.put("revision",0);m.put("createdAt",c.knownAt());m.put("recordedAt",c.knownAt());return m;}
 public void activated(DomainContext c,Map<String,Object>w,Map<String,Object>goal){r.fence(c.organizationId(),text(w,"ID"));human(c,text(w,"ownerId"));human(c,text(w,"supervisorId"));}
 public void initialDuty(DomainContext c,Map<String,Object>w){if(w.get("nextAction")==null)return;String workId=text(w,"ID");r.fence(c.organizationId(),workId);human(c,text(w,"ownerId"));human(c,text(w,"supervisorId"));String scope=canonicalScope("{\"workId\":\""+workId+"\"}");String key=hash("WORK|"+workId+"|1|WORK_NEXT_ACTION|"+scope);r.fence(c.organizationId(),key);if(r.rows("Roots",c.organizationId()).stream().anyMatch(x->key.equals(x.get("dutyKey"))))return;
  var root=row(c,UUID.randomUUID().toString());root.putAll(Map.of("dutyKey",key,"sourceId",workId,"sourceKind","WORK","sourceVersion","1","kind","WORK_NEXT_ACTION","scopeJson",scope));r.insert("Roots",root);var s=row(c,UUID.randomUUID().toString());s.putAll(Map.of("rootId",root.get("ID"),"startQuantity",BigDecimal.ZERO,"quantity",BigDecimal.ONE,"scopeJson",scope,"leaf",true));r.insert("Scopes",s);r.insert("Assignments",assignment(c,root,s,w,text(w,"nextAction"),at(w.get("nextCheckAt")),null));}
 public void workClosed(DomainContext c,String workId,String reason){r.fence(c.organizationId(),workId);var roots=new HashSet<Object>();for(var x:r.rows("Roots",c.organizationId()))if("WORK".equals(x.get("sourceKind"))&&workId.equals(x.get("sourceId"))&&"WORK_NEXT_ACTION".equals(x.get("kind")))roots.add(x.get("ID"));if(roots.isEmpty())return;for(var a:assignments(c,workId))if(roots.contains(a.get("rootId"))&&open(a))r.update("Assignments",c.organizationId(),a.get("ID").toString(),Map.of("status","RESOLVED","evidenceId",workId,"basis","WORK_CLOSED:"+reason,"revision",((Number)a.get("revision")).intValue()+1));}
 public void requireSettled(DomainContext c,String workId){r.fence(c.organizationId(),workId);if(assignments(c,workId).stream().anyMatch(ResponsibilityService::open))throw new DomainError("REJECTED","UNRESOLVED_DUTY","Unresolved valid duties require accepted follow-up responsibility");}
 public void followup(DomainContext c,String original,String target,Map<String,Object>p){
  // A new response Work does not accept an existing transferred obligation.
  var parent=work.require(c,original,true);var child=work.require(c,target,true);
  if(p.get("obligationIds") instanceof List<?> ids&&!ids.isEmpty())throw DomainError.invalid("Existing duties require explicit recipient acceptance");
  if(p.get("sourceEventId")!=null){var source=followupSources.require(c,text(p,"sourceEventId"),text(parent,"itemId"));
   String action=text(child,"nextAction");Instant next=at(child.get("nextCheckAt"));if(!next.isAfter(clock.instant()))throw DomainError.invalid("Future response check required");
   recordDuty(c,Map.of("workId",target,"sourceId",source.get("ID"),"sourceKind","EVENT","sourceVersion",String.valueOf(source.get("revision")),"kind","UNVERIFIED_SOURCE_REVIEW","scopeJson",json(Map.of("originalWorkId",original,"segmentId",source.get("subjectId"))),"nextAction",action,"nextCheckAt",next),true);
  }
 }
 private List<Map<String,Object>> assignments(DomainContext c,String workId){return r.rows("Assignments",c.organizationId()).stream().filter(x->workId.equals(x.get("workId"))).toList();}
 private static boolean open(Map<String,Object>x){return "OPEN".equals(x.get("status"))&&!Boolean.FALSE.equals(x.get("valid"));}
 public Map<String,Object> openDuty(DomainContext c,Map<String,Object>p){return "GOAL_QUANTITY_SHORTFALL".equals(p.get("kind"))?openGoalShortfall(c,p):recordDuty(c,p,false);}
 public Map<String,Object> assessed(DomainContext c,String workId){
  var w=work.require(c,workId,true);String commandId=CommandExecution.commandId();var command=r.requireSource(c.organizationId(),"DECISION",commandId);
  var proof=shortfalls.requireInAssessment(c,workId,command);
  return openGoalShortfall(c,Map.of("workId",workId,"sourceKind","DECISION","sourceId",commandId,"sourceVersion",command.get("canonicalHash"),"scopeJson",json(proof.scope()),"quantity",proof.shortfall(),"unit",proof.unit(),"nextAction",text(w,"nextAction"),"nextCheckAt",at(w.get("nextCheckAt")).toString()),true);
 }
 private Map<String,Object> openGoalShortfall(DomainContext c,Map<String,Object> p){return openGoalShortfall(c,p,false);}
 private Map<String,Object> openGoalShortfall(DomainContext c,Map<String,Object> p,boolean assessing){
  String authority=assessing?"assessGoal":"openObligation";
  String workId=text(p,"workId");r.fence(c.organizationId(),workId);var w=work.require(c,workId,true);
  if(!Set.of("ACTIVE","WAITING").contains(w.get("status")))throw DomainError.invalid("Shortfall duty needs active Work");
  if(!"DECISION".equals(p.get("sourceKind")))throw DomainError.invalid("Shortfall source must be the committed assessment command");
  var command=r.requireSource(c.organizationId(),"DECISION",text(p,"sourceId"));
  if(!text(p,"sourceVersion").equals(command.get("canonicalHash")))throw DomainError.invalid("Assessment command version mismatch");
  com.mulino.application.evaluation.GoalShortfallProof.Proof proof;
  try{proof=assessing?shortfalls.requireInAssessment(c,workId,command):shortfalls.require(c,workId,command);}catch(DomainError failure){
   if("ASSESSMENT_NOT_CURRENT".equals(failure.code())){
    var prior=r.rows("Roots",c.organizationId()).stream().filter(x->"GOAL_QUANTITY_SHORTFALL".equals(x.get("kind"))).filter(x->{var scope=scopeMap(text(x,"scopeJson"));return workId.equals(scope.get("workId"))&&Objects.equals(w.get("currentGoalVersionId"),scope.get("goalVersionId"));}).toList();
    if(prior.size()==1){r.fence(c.organizationId(),text(prior.getFirst(),"ID"));var retained=r.currentAssignments(c.organizationId(),text(prior.getFirst(),"ID"));
     for(var assignment:retained){var scopes=new LinkedHashMap<>(shortfalls.scopes(c,workId));scopes.put("WORK",List.of(text(assignment,"workId")));auth.authorizeScopes(c,authority,scopes);}
     if(!retained.isEmpty())throw ObligationClosureCatalog.held(retained.getFirst(),"openObligation");
    }
   }
   throw failure;
  }
  if(!text(p,"unit").equals(proof.unit())||!canonicalScope(text(p,"scopeJson")).equals(canonicalScope(json(proof.scope()))))throw DomainError.invalid("Requested shortfall unit or immutable goal scope differs from proof");
  if(p.get("quantity")==null)throw DomainError.invalid("Missing shortfall quantity");var requestedQuantity=quantity(p.get("quantity"));
  String key=hash(json(List.of(c.organizationId(),workId,proof.goalId(),proof.conditionId())));r.fence(c.organizationId(),key);
  var old=r.rootByDutyKey(c.organizationId(),key);
  if(old.isPresent()){
   var root=old.get();r.fence(c.organizationId(),text(root,"ID"));var current=r.currentAssignments(c.organizationId(),text(root,"ID"));
   if(current.isEmpty())throw DomainError.invalid("Shortfall root has no current responsibility");
   for(var assignment:current){var scopes=new LinkedHashMap<>(shortfalls.scopes(c,workId));scopes.put("WORK",List.of(text(assignment,"workId")));auth.authorizeScopes(c,authority,scopes);}
   var stored=scopeMap(text(root,"scopeJson"));var first=scopeMap(json(stored.get("proof")));
   boolean sameBasis=Objects.equals(first.get("inputSnapshotHash"),proof.provenance().get("inputSnapshotHash"))
     ||first.get("shortfallBasisHash")!=null&&Objects.equals(first.get("shortfallBasisHash"),proof.provenance().get("shortfallBasisHash"));
   if(!"GOAL_QUANTITY_SHORTFALL".equals(root.get("kind"))||quantity(root.get("quantity")).compareTo(proof.shortfall())!=0||!Objects.equals(root.get("unit"),proof.unit())||!sameBasis)throw ObligationClosureCatalog.held(current.getFirst(),"openObligation");
   if(requestedQuantity.compareTo(proof.shortfall())!=0)throw DomainError.invalid("Requested shortfall quantity differs from proof");
   return Map.of("rootId",root.get("ID"),"assignments",current);
  }
  if(requestedQuantity.compareTo(proof.shortfall())!=0)throw DomainError.invalid("Requested shortfall quantity differs from proof");
  human(c,text(w,"ownerId"));human(c,text(w,"supervisorId"));String action=text(p,"nextAction");Instant next=at(text(p,"nextCheckAt"));
  if(!next.isAfter(clock.instant()))throw DomainError.invalid("Shortfall nextCheckAt must be future");
  var stored=new LinkedHashMap<String,Object>(proof.scope());stored.put("proof",proof.provenance());
  var root=row(c,UUID.randomUUID().toString());root.putAll(Map.of("dutyKey",key,"sourceId",text(command,"ID"),"sourceKind","DECISION","sourceVersion",text(command,"canonicalHash"),"kind","GOAL_QUANTITY_SHORTFALL","scopeJson",canonicalScope(json(stored)),"quantity",proof.shortfall(),"unit",proof.unit()));r.insert("Roots",root);
  var scope=row(c,UUID.randomUUID().toString());scope.putAll(Map.of("rootId",root.get("ID"),"startQuantity",BigDecimal.ZERO,"quantity",proof.shortfall(),"unit",proof.unit(),"scopeJson",root.get("scopeJson"),"leaf",true));r.insert("Scopes",scope);
  var assignment=assignment(c,root,scope,w,action,next,null);r.insert("Assignments",assignment);return Map.of("rootId",root.get("ID"),"assignments",List.of(assignment));
 }
 public void contributed(DomainContext c,String occurrenceId,String targetWorkId){
  var credits=r.workContributions(c,occurrenceId).stream().filter(x->targetWorkId.equals(x.get("targetWorkId"))).toList();
  var actual=r.requireSource(c.organizationId(),"OCCURRENCE",occurrenceId);
  if(!targetWorkId.equals(actual.get("workId"))||actual.get("quantity")==null)return;
  BigDecimal end=BigDecimal.ZERO;
  for(var credit:credits.stream().sorted(Comparator.comparing(x->new BigDecimal(x.get("startQuantity").toString()))).toList()){
   if(!Objects.equals(actual.get("unit"),credit.get("unit"))||new BigDecimal(credit.get("startQuantity").toString()).compareTo(end)!=0)return;
   end=end.add(new BigDecimal(credit.get("quantity").toString()));
  }
  if(end.compareTo(new BigDecimal(actual.get("quantity").toString()))!=0)return;
  for(var root:r.rows("Roots",c.organizationId()))if("RECEIPT_UNALLOCATED".equals(root.get("kind"))&&receiptAllocationSource(c,root,actual,targetWorkId)){
   r.fence(c.organizationId(),text(root,"ID"));
   for(var a:r.currentAssignments(c.organizationId(),text(root,"ID")))if(targetWorkId.equals(a.get("workId")))r.update("Assignments",c.organizationId(),text(a,"ID"),Map.of("status","RESOLVED","evidenceId",CommandExecution.commandId(),"basis","VERIFIED_WORK_CONTRIBUTION_ALLOCATION","revision",((Number)a.get("revision")).intValue()+1));
  }
 }
 private boolean receiptAllocationSource(DomainContext c,Map<String,Object> root,Map<String,Object> actual,String workId){
  if(root.get("quantity")==null||!Objects.equals(root.get("unit"),actual.get("unit"))||new BigDecimal(root.get("quantity").toString()).compareTo(new BigDecimal(actual.get("quantity").toString()))!=0)return false;
  if("OCCURRENCE".equals(root.get("sourceKind")))return Objects.equals(root.get("sourceId"),actual.get("ID"));
  if(!"DECISION".equals(root.get("sourceKind")))return false;
  var scope=scopeMap(text(root,"scopeJson"));if(!workId.equals(scope.get("originalWorkId"))||!Objects.equals(actual.get("ID"),scope.get("domainSourceId")))return false;
  var command=r.requireSource(c.organizationId(),"DECISION",text(root,"sourceId"));
  if(!"COMMITTED".equals(command.get("state"))||!"confirmReceipt".equals(command.get("capabilityId"))||!Objects.equals(root.get("sourceVersion"),command.get("canonicalHash")))return false;
  var result=scopeMap(text(command,"resultJson"));return "APPLIED".equals(result.get("outcome"))&&Objects.equals(actual.get("ID"),result.get("occurrenceId"));
 }
 private static String json(Object value){try{return new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules().writeValueAsString(TransportValues.normalize(value));}catch(Exception failure){throw DomainError.invalid("Shortfall proof serialization failed");}}
 private static Map<String,Object> scopeMap(String value){try{return new com.fasterxml.jackson.databind.ObjectMapper().readValue(value,Map.class);}catch(Exception failure){throw DomainError.invalid("Shortfall proof unavailable");}}

 private Map<String,Object> recordDuty(DomainContext c,Map<String,Object>p,boolean rawSource){String workId=text(p,"workId");r.fence(c.organizationId(),workId);var w=work.require(c,workId,true);if(!Set.of("ACTIVE","WAITING").contains(w.get("status")))throw DomainError.invalid("Duty needs active work");human(c,text(w,"ownerId"));human(c,text(w,"supervisorId"));String kind=text(p,"kind");if(!ObligationClosureCatalog.known(kind))throw DomainError.invalid("Unknown obligation kind");String scope=text(p,"scopeJson"),source=text(p,"sourceId"),version=text(p,"sourceVersion"),sourceKind=text(p,"sourceKind");if(!(rawSource?Set.of("EVENT","CLAIM","DOCUMENT"):Set.of("OCCURRENCE","DECISION")).contains(sourceKind))throw DomainError.invalid("Unsupported duty source");var sourceRow=r.requireSource(c.organizationId(),sourceKind,source);String trustedVersion=sourceRow.get("canonicalHash")!=null?sourceRow.get("canonicalHash").toString():String.valueOf(sourceRow.getOrDefault("revision",0));if(!version.equals(trustedVersion))throw DomainError.invalid("Source occurrence or decision version mismatch");scope=canonicalScope(scope);String key=hash(sourceKind+"|"+source+"|"+version+"|"+kind+"|"+scope);r.fence(c.organizationId(),key);var old=r.rows("Roots",c.organizationId()).stream().filter(x->key.equals(x.get("dutyKey"))).findFirst();if(old.isPresent())return Map.of("rootId",old.get().get("ID"),"assignments",r.rows("Assignments",c.organizationId()).stream().filter(x->old.get().get("ID").equals(x.get("rootId"))&&!Boolean.FALSE.equals(x.get("valid"))).toList());
 if("DELIVERY_RECONCILIATION".equals(kind))w=deliveryIntake(c,p,w,sourceRow,scope);
 var root=row(c,UUID.randomUUID().toString());root.putAll(Map.of("dutyKey",key,"sourceId",source,"sourceKind",sourceKind,"sourceVersion",version,"kind",kind,"scopeJson",scope));BigDecimal q=p.get("quantity")==null?null:quantity(p.get("quantity"));if(q!=null){root.put("quantity",q);root.put("unit",text(p,"unit"));}r.insert("Roots",root);
 var s=row(c,UUID.randomUUID().toString());s.putAll(Map.of("rootId",root.get("ID"),"startQuantity",BigDecimal.ZERO,"quantity",q==null?BigDecimal.ONE:q,"scopeJson",scope,"leaf",true));if(q!=null)s.put("unit",root.get("unit"));r.insert("Scopes",s);var a=assignment(c,root,s,w,text(p,"nextAction"),at(p.get("nextCheckAt")),null);r.insert("Assignments",a);return a;}
 /** The provisional observation retains its source's intake humans without changing Work ownership or accepted transfers. */
 private Map<String,Object> deliveryIntake(DomainContext c,Map<String,Object> p,Map<String,Object> w,Map<String,Object> command,String scopeJson){
  if(!"DECISION".equals(p.get("sourceKind"))||!Objects.equals(command.get("ID"),CommandExecution.commandId())||!"IN_PROGRESS".equals(command.get("state"))||!"RECORD".equals(command.get("effectClass"))||!Set.of("recordDelivery","recordObservedMovement").contains(command.get("capabilityId"))||!c.actorId().equals(command.get("actorId"))||!c.stableRequestOwner().equals(command.get("stableRequestOwner"))||!c.organizationId().equals(command.get("organizationId")))throw DomainError.forbidden();
  var intent=scopeMap(text(command,"canonicalIntentJson"));if(!"RECORD".equals(intent.get("intentKind"))||!Objects.equals(command.get("capabilityId"),intent.get("capabilityId"))||!Objects.equals(command.get("canonicalHash"),CommandRequests.hash(intent)))throw DomainError.forbidden();
  var scope=scopeMap(scopeJson);String observationId=text(scope,"domainSourceId");var observation=r.deliveryObservation(c.organizationId(),observationId);
  if(!"PROVISIONAL".equals(observation.get("state"))||!observationId.equals(scope.get("physicalScopeId"))||!Objects.equals(observation.get("workId"),scope.get("originalWorkId"))||!Objects.equals(observation.get("itemId"),w.get("itemId"))||!Objects.equals(observation.get("nextAction"),p.get("nextAction"))||!at(observation.get("nextCheckAt")).equals(at(p.get("nextCheckAt"))))throw DomainError.invalid("Exact provisional delivery duty required");
  if(p.get("quantity")!=null&&(quantity(p.get("quantity")).compareTo(quantity(observation.get("quantity")))!=0||!Objects.equals(p.get("unit"),observation.get("unit"))))throw DomainError.invalid("Reported delivery duty quantity or unit differs");
  var slots=intent.get("slots") instanceof Map<?,?> values?values:Map.of();
  if(!Objects.equals(slots.get("workId"),observation.get("workId"))||!Objects.equals(slots.get("eventId"),observation.get("eventId"))||slots.get("observationId")!=null&&!observationId.equals(slots.get("observationId")))throw DomainError.forbidden();
  var event=r.requireSource(c.organizationId(),"EVENT",text(observation,"eventId"));
  if(!Set.of("PHYSICAL_DELIVERY","OBSERVED_MOVEMENT").contains(event.get("kind"))||!Objects.equals(observation.get("workId"),event.get("workId"))||!Objects.equals(observation.get("itemId"),event.get("itemId")))throw DomainError.invalid("Delivery intake event subject differs");
  var profile=r.evidenceRows("Profiles",c.organizationId()).stream().filter(x->Objects.equals(event.get("sourceProfileId"),x.get("ID"))).findFirst().orElseThrow(DomainError::forbidden);
  if(!Objects.equals(profile.get("namespace"),event.get("sourceNamespace")))throw DomainError.invalid("Delivery intake source namespace differs");
  String owner=text(profile,"intakeOwnerId"),supervisor=text(profile,"supervisorId");auth.fence(c,new HashSet<>(List.of(owner,supervisor)));human(c,owner);human(c,supervisor);
  var assigned=new LinkedHashMap<String,Object>(w);assigned.put("ownerId",owner);assigned.put("supervisorId",supervisor);return assigned;
 }
 private Map<String,Object> assignment(DomainContext c,Map<String,Object>root,Map<String,Object>s,Map<String,Object>w,String action,Instant next,String predecessor){var a=row(c,UUID.randomUUID().toString());a.putAll(Map.of("effectiveAt",c.asOf(),"workId",w.get("ID"),"rootId",root.get("ID"),"scopeId",s.get("ID"),"kind",root.get("kind"),"status","OPEN","ownerId",w.get("ownerId"),"supervisorId",w.get("supervisorId"),"nextAction",action));a.put("nextCheckAt",next);a.put("scopeJson",s.get("scopeJson"));a.put("valid",true);if(root.get("quantity")!=null){a.put("quantity",s.get("quantity"));a.put("unit",root.get("unit"));}if(predecessor!=null)a.put("predecessorId",predecessor);return a;}
 public Map<String,Object> propose(DomainContext c,Map<String,Object>p,boolean transfer){String workId=text(p,"workId");r.fence(c.organizationId(),workId);var w=work.require(c,workId,true);if(!c.actorId().equals(w.get("ownerId")))throw DomainError.forbidden();String recipient=text(p,"recipientId");human(c,recipient);String target=transfer?text(p,"targetWorkId"):workId;var tw=work.require(c,target,true);if(!Set.of("ACTIVE","WAITING").contains(tw.get("status")))throw DomainError.invalid("Target work inactive");if(transfer&&!recipient.equals(tw.get("ownerId")))throw DomainError.invalid("Target owner must accept");var h=row(c,UUID.randomUUID().toString());h.putAll(Map.of("kind",transfer?"TRANSFER":"OWNER","workId",workId,"targetWorkId",target,"previousOwnerId",w.get("ownerId"),"recipientId",recipient,"status","PROPOSED","expiresAt",at(p.get("expiresAt")),"nextAction",text(p,"nextAction"),"nextCheckAt",at(p.get("nextCheckAt"))));if(!at(h.get("expiresAt")).isAfter(clock.instant()))throw DomainError.invalid("Proposal expired");if(transfer){var a=r.require("Assignments",c.organizationId(),text(p,"assignmentId"));if(!open(a)||!workId.equals(a.get("workId")))throw DomainError.invalid("Duty not current");h.put("assignmentId",a.get("ID"));if(p.get("quantity")!=null)h.put("quantity",quantity(p.get("quantity")));}r.insert("Handovers",h);return h;}
 public Map<String,Object> decide(DomainContext c,String id,String decision){var h=r.require("Handovers",c.organizationId(),id);r.fence(c.organizationId(),text(h,"workId"));r.fence(c.organizationId(),id);h=r.require("Handovers",c.organizationId(),id);if(!"PROPOSED".equals(h.get("status")))throw DomainError.invalid("Handover already decided");boolean expired=!clock.instant().isBefore(at(h.get("expiresAt")));if("EXPIRED".equals(decision)&&!expired)throw DomainError.invalid("Not expired");if(!"EXPIRED".equals(decision)&&!c.actorId().equals(h.get("recipientId")))throw DomainError.forbidden();if("ACCEPTED".equals(decision)&&expired)throw DomainError.invalid("Expired handover");var w=work.require(c,text(h,"workId"),true);if(!h.get("previousOwnerId").equals(w.get("ownerId")))throw DomainError.invalid("Owner changed");if("ACCEPTED".equals(decision)){human(c,text(h,"recipientId"));auth.authorizeScope(c,"acceptHandover","WORK",text(h,"targetWorkId"));if("TRANSFER".equals(h.get("kind")))transfer(c,h);else{work.replaceOwner(c,text(h,"workId"),text(h,"previousOwnerId"),text(h,"recipientId"));for(var a:assignments(c,text(h,"workId")))if(open(a))r.update("Assignments",c.organizationId(),text(a,"ID"),Map.of("ownerId",h.get("recipientId"),"nextAction",h.get("nextAction"),"nextCheckAt",h.get("nextCheckAt"),"revision",((Number)a.get("revision")).intValue()+1));}}
 r.update("Handovers",c.organizationId(),id,Map.of("status",decision,"revision",((Number)h.get("revision")).intValue()+1));return r.require("Handovers",c.organizationId(),id);}
 private void transfer(DomainContext c,Map<String,Object>h){var a=r.require("Assignments",c.organizationId(),text(h,"assignmentId"));r.fence(c.organizationId(),text(a,"rootId"));a=r.require("Assignments",c.organizationId(),text(h,"assignmentId"));if(!open(a))throw DomainError.invalid("Duty already moved");String target=text(h,"targetWorkId");var w=work.require(c,target,true);if(!Set.of("ACTIVE","WAITING").contains(w.get("status"))||!h.get("recipientId").equals(w.get("ownerId")))throw DomainError.invalid("Recipient no longer target owner");var predecessor=a;var visited=new HashSet<String>();while(predecessor!=null){if(!visited.add(text(predecessor,"ID")))throw DomainError.invalid("Transfer cycle");if(target.equals(predecessor.get("workId")))throw DomainError.invalid("Transfer cycle");predecessor=predecessor.get("predecessorId")==null?null:r.require("Assignments",c.organizationId(),text(predecessor,"predecessorId"));}var root=r.require("Roots",c.organizationId(),text(a,"rootId"));var scope=r.require("Scopes",c.organizationId(),text(a,"scopeId"));BigDecimal full=(BigDecimal)scope.get("quantity"),q=h.get("quantity")==null?full:quantity(h.get("quantity"));if(q.compareTo(full)>0)throw DomainError.invalid("Transfer exceeds scope");r.update("Assignments",c.organizationId(),text(a,"ID"),Map.of("status","TRANSFERRED","valid",false,"revision",((Number)a.get("revision")).intValue()+1));if(q.compareTo(full)<0){if(root.get("quantity")==null)throw DomainError.invalid("Nonquantity duty cannot split");r.update("Scopes",c.organizationId(),text(scope,"ID"),Map.of("leaf",false));var remaining=child(c,scope,(BigDecimal)scope.get("startQuantity"),full.subtract(q));var moving=child(c,scope,((BigDecimal)scope.get("startQuantity")).add(full.subtract(q)),q);r.insert("Assignments",assignment(c,root,remaining,work.require(c,text(a,"workId"),true),text(a,"nextAction"),at(a.get("nextCheckAt")),text(a,"ID")));scope=moving;}r.insert("Assignments",assignment(c,root,scope,w,text(h,"nextAction"),at(h.get("nextCheckAt")),text(a,"ID")));}
 private Map<String,Object> child(DomainContext c,Map<String,Object>parent,BigDecimal start,BigDecimal q){var s=row(c,UUID.randomUUID().toString());s.putAll(Map.of("rootId",parent.get("rootId"),"parentScopeId",parent.get("ID"),"startQuantity",start,"quantity",q,"leaf",true,"scopeJson",parent.get("scopeJson")));s.put("unit",parent.get("unit"));r.insert("Scopes",s);return s;}
 public Map<String,Object> settle(DomainContext c,Map<String,Object>p,boolean waive){if(waive)throw DomainError.invalid("Waiver requires the guarded approval decision");String id=text(p,"assignmentId");var a=r.require("Assignments",c.organizationId(),id);r.fence(c.organizationId(),text(a,"rootId"));a=r.require("Assignments",c.organizationId(),id);if(!open(a))throw DomainError.invalid("Duty not open");String ev=text(p,"evidenceId");evidence.requireResolution(c,text(a,"kind"),text(a,"rootId"),text(a,"scopeId"),ev);r.update("Assignments",c.organizationId(),id,Map.of("status","RESOLVED","evidenceId",ev,"basis","VERIFIED_RESOLUTION","revision",((Number)a.get("revision")).intValue()+1));return r.require("Assignments",c.organizationId(),id);}
 /** The approval ID is the guarded top-level decision; it binds this exact assignment, revision, reason and kind authority. */
 public Map<String,Object> waive(DomainContext c,Map<String,Object>p,String approvalId){String id=text(p,"assignmentId"),reason=text(p,"reason");var a=r.require("Assignments",c.organizationId(),id);r.fence(c.organizationId(),text(a,"rootId"));a=r.require("Assignments",c.organizationId(),id);if(!open(a))throw DomainError.invalid("Duty not open");evidence.requireWaiver(c,text(a,"kind"),text(a,"rootId"),id,((Number)a.get("revision")).intValue(),approvalId,reason);
  // The kind owner records what this waiver covers at execution (plan §4.3 134행): a later restoration compares against it.
  String coverage=evidence.waiverCoverage(c,text(a,"kind"),text(a,"rootId"));String basis="WAIVER: "+reason+(coverage==null?"":" "+coverage);if(basis.length()>500)throw DomainError.invalid("Waiver reason too long");
  r.update("Assignments",c.organizationId(),id,Map.of("status","WAIVED","evidenceId",approvalId,"basis",basis,"revision",((Number)a.get("revision")).intValue()+1));return r.require("Assignments",c.organizationId(),id);}
 /**
  * Re-issues every open assignment of one root for a changed basis (plan §5.3 waiveObligation "현재 scope/revision", §4.3 정정 재평가):
  * the owner and work are unchanged, the revision is bumped and nextAction/nextCheck refreshed, so a waiver or other decision bound to
  * the earlier revision no longer executes. Idempotent per basis: reprocessing the same basis changes nothing.
  */
 public List<String> reissueOpen(DomainContext c,String rootId,String nextAction,Instant nextCheckAt,String basis){
  r.fence(c.organizationId(),rootId);var out=new ArrayList<String>();
  for(var a:r.rows("Assignments",c.organizationId())){if(!rootId.equals(a.get("rootId"))||!open(a)||basis.equals(a.get("basis")))continue;
   var update=new LinkedHashMap<String,Object>();update.put("nextAction",nextAction);update.put("nextCheckAt",nextCheckAt);update.put("basis",basis);update.put("revision",((Number)a.get("revision")).intValue()+1);r.update("Assignments",c.organizationId(),text(a,"ID"),update);out.add(text(a,"ID"));}
  return List.copyOf(out);
 }
 /**
  * Emergency reassignment (plan §5.3, §7.1, §7.2): a separate ADMIN command with its own reason and audit, not
  * acceptHandover. The current owner is replaced atomically with every open assignment of the work; nothing is
  * accepted on the recipient's behalf (handoverAccepted=false) and no approval row is involved.
  */
 public Map<String,Object> emergency(DomainContext c,Map<String,Object>p){
  if(!CapabilitySlots.allowed("emergencyReassign").containsAll(p.keySet()))throw DomainError.invalid("Unsupported emergency reassignment slot");
  String workId=text(p,"workId"),recipient=text(p,"newOwnerId"),reason=text(p,"reason");
  if((p.get("nextAction")==null)!=(p.get("nextCheckAt")==null))throw DomainError.invalid("nextAction and nextCheckAt are set together");
  if(p.get("effectiveAt")!=null&&at(p.get("effectiveAt")).isAfter(c.knownAt()))throw DomainError.invalid("Emergency reassignment cannot be future-dated");
  r.fence(c.organizationId(),workId);var w=work.require(c,workId,true);
  if(!Set.of("ACTIVE","WAITING").contains(w.get("status")))throw DomainError.invalid("Work inactive");
  String previous=text(w,"ownerId");if(previous.equals(recipient))throw DomainError.invalid("Recipient already owns the work");
  human(c,recipient);human(c,text(w,"supervisorId"));
  var open=assignments(c,workId).stream().filter(ResponsibilityService::open).toList();
  if(p.get("obligationId")!=null&&open.stream().noneMatch(a->text(p,"obligationId").equals(a.get("ID"))))throw DomainError.invalid("Named obligation is not an open duty of the work");
  work.replaceOwner(c,workId,previous,recipient);
  var moved=new ArrayList<String>();
  for(var a:open){var update=new LinkedHashMap<String,Object>();update.put("ownerId",recipient);update.put("basis","EMERGENCY: "+reason);if(p.get("nextAction")!=null){update.put("nextAction",text(p,"nextAction"));update.put("nextCheckAt",at(p.get("nextCheckAt")));}update.put("revision",((Number)a.get("revision")).intValue()+1);r.update("Assignments",c.organizationId(),text(a,"ID"),update);moved.add(text(a,"ID"));}
  var result=new LinkedHashMap<String,Object>();result.put("workId",workId);result.put("previousOwnerId",previous);result.put("ownerId",recipient);result.put("reason",reason);result.put("reassignedAssignmentIds",List.copyOf(moved));result.put("handoverAccepted",false);return result;
 }
 public Map<String,Object> gap(DomainContext c,String workId){var w=work.require(c,workId,false);boolean gap;try{human(c,text(w,"ownerId"));gap=false;}catch(DomainError e){gap=true;}return Map.of("workId",workId,"responsibilityGap",gap,"ownerId",w.get("ownerId"),"supervisorId",w.get("supervisorId"));}
 public Map<String,Object> ensureRuntimeDuty(DomainContext c,String workId,String sourceDecisionId,String kind,String nextAction,Instant nextCheckAt){return ensureDuty(c,workId,sourceDecisionId,"DECISION",kind,nextAction,nextCheckAt);}
 public Map<String,Object> ensureCorrectionDuty(DomainContext c,String workId,String occurrenceId,String nextAction,Instant nextCheckAt){return ensureDuty(c,workId,occurrenceId,"OCCURRENCE","FOLLOWUP_REVIEW",nextAction,nextCheckAt);}
 public Map<String,Object> ensureEvidenceCorrectionDuty(DomainContext c,String workId,String evidenceId,String evidenceKind,String nextAction,Instant nextCheckAt){
  if(!Set.of("EVENT","CLAIM","DOCUMENT").contains(evidenceKind))throw DomainError.invalid("Typed raw evidence source required");
  var source=r.requireSource(c.organizationId(),evidenceKind,evidenceId);
  var profile=r.evidenceRows("Profiles",c.organizationId()).stream().filter(x->Objects.equals(source.get("sourceProfileId"),x.get("ID"))).findFirst().orElseThrow(DomainError::forbidden);
  String intakeOwner=text(profile,"intakeOwnerId"),supervisor=text(profile,"supervisorId");auth.fence(c,new HashSet<>(List.of(intakeOwner,supervisor)));human(c,intakeOwner);human(c,supervisor);
  String action=text(profile,"nextAction");Instant next=at(profile.get("nextCheckAt"));if(!action.equals(nextAction)||!next.equals(nextCheckAt))throw DomainError.invalid("Correction must retain authoritative intake next action and date");
  r.fence(c.organizationId(),workId);var w=work.require(c,workId,true);if(source.get("itemId")!=null&&!Objects.equals(source.get("itemId"),w.get("itemId")))throw DomainError.invalid("Correction source work item mismatch");
  if("CLOSED".equals(w.get("status")))w=work.ensureFollowup(c,workId,evidenceId,"UNVERIFIED_SOURCE_REVIEW",action,next);
  String version=String.valueOf(source.getOrDefault("revision",0));String scope;try{scope=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(Map.of("originalWorkId",workId,"sourceProfileId",profile.get("ID"),"evidenceKind",evidenceKind));}catch(Exception e){throw new IllegalStateException(e);}
  var result=recordDuty(c,Map.of("workId",w.get("ID"),"sourceId",evidenceId,"sourceKind",evidenceKind,"sourceVersion",version,"kind","UNVERIFIED_SOURCE_REVIEW","scopeJson",scope,"nextAction",action,"nextCheckAt",next),true);
  if(result.get("ID")!=null&&"OPEN".equals(result.get("status"))){r.update("Assignments",c.organizationId(),text(result,"ID"),Map.of("ownerId",intakeOwner,"supervisorId",supervisor));return r.require("Assignments",c.organizationId(),text(result,"ID"));}
  return result;
 }
 private Map<String,Object> ensureDuty(DomainContext c,String originalId,String sourceId,String sourceKind,String kind,String action,Instant next){r.fence(c.organizationId(),originalId);var w=work.require(c,originalId,true);if("CLOSED".equals(w.get("status")))w=work.ensureFollowup(c,originalId,sourceId,kind,action,next);var source=r.requireSource(c.organizationId(),sourceKind,sourceId);String version=source.get("canonicalHash")!=null?source.get("canonicalHash").toString():String.valueOf(source.getOrDefault("revision",0));var scope=new TreeMap<String,Object>();scope.put("originalWorkId",originalId);if(source.get("physicalScopeId")!=null)scope.put("physicalScopeId",source.get("physicalScopeId"));String scopeJson;try{scopeJson=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(scope);}catch(Exception e){throw new IllegalStateException(e);}return openDuty(c,Map.of("workId",w.get("ID"),"sourceId",sourceId,"sourceKind",sourceKind,"sourceVersion",version,"kind",kind,"scopeJson",scopeJson,"nextAction",action,"nextCheckAt",next));}
 /** Conserves immutable root quantity and transferred ownership while discharging only receipt-covered open leaves. */
 public void applyReceiptProgress(DomainContext c,com.mulino.application.trade.TradeResidualRemedy.ReceiptProgress progress){
  r.fence(c.organizationId(),progress.rootId());
  var root=r.require("Roots",c.organizationId(),progress.rootId());
  if(!"RECEIPT_SHORTFALL".equals(root.get("kind"))||progress.covered().compareTo((BigDecimal)root.get("quantity"))>0)throw DomainError.invalid("Receipt residual root mismatch");
  for(var a:r.rows("Assignments",c.organizationId())){
   if(!progress.rootId().equals(a.get("rootId"))||!open(a))continue;
   var scope=r.require("Scopes",c.organizationId(),text(a,"scopeId"));
   BigDecimal start=(BigDecimal)scope.get("startQuantity"),full=(BigDecimal)scope.get("quantity");
   BigDecimal covered=progress.covered().subtract(start).max(BigDecimal.ZERO).min(full);
   if(covered.signum()==0)continue;
   Map<String,Object> resolving=a;
   if(covered.compareTo(full)<0){
    r.update("Assignments",c.organizationId(),text(a,"ID"),Map.of("status","TRANSFERRED","valid",false,"basis","RECEIPT_RANGE_SPLIT","revision",((Number)a.get("revision")).intValue()+1));
    r.update("Scopes",c.organizationId(),text(scope,"ID"),Map.of("leaf",false));
    var done=child(c,scope,start,covered);var remaining=child(c,scope,start.add(covered),full.subtract(covered));
    resolving=receiptChildAssignment(c,a,done);r.insert("Assignments",resolving);r.insert("Assignments",receiptChildAssignment(c,a,remaining));scope=done;
   }
   var credit=row(c,UUID.randomUUID().toString());credit.putAll(Map.of("rootId",root.get("ID"),"scopeId",scope.get("ID"),"assignmentId",resolving.get("ID"),"receiptId",progress.receiptId(),"startQuantity",scope.get("startQuantity"),"quantity",scope.get("quantity")));
   r.insert("ReceiptResidualCredits",credit);
   r.update("Assignments",c.organizationId(),text(resolving,"ID"),Map.of("status","RESOLVED","evidenceId",progress.occurrenceId(),"basis","VERIFIED_RECEIPT_CONTRIBUTION","revision",((Number)resolving.get("revision")).intValue()+1));
  }
 }
 public void applyTradeObservation(DomainContext c,com.mulino.application.trade.TradeObservationRemedy.Completion completion){
  r.fence(c.organizationId(),completion.rootId());var root=r.require("Roots",c.organizationId(),completion.rootId());
  if(!Set.of("DELIVERY_RECONCILIATION","RETURN_RECONCILIATION").contains(completion.kind())||!completion.kind().equals(root.get("kind")))throw DomainError.invalid("Exact trade reconciliation root required");
  if(root.get("quantity")!=null&&(!"DELIVERY_RECONCILIATION".equals(completion.kind())||completion.quantity()==null||quantity(root.get("quantity")).compareTo(completion.quantity())!=0||!Objects.equals(root.get("unit"),completion.unit())))throw DomainError.invalid("Exact verified delivery quantity and unit required");
  for(var assignment:r.rows("Assignments",c.organizationId()))if(completion.rootId().equals(assignment.get("rootId"))&&open(assignment))r.update("Assignments",c.organizationId(),text(assignment,"ID"),Map.of("status","RESOLVED","evidenceId",completion.canonicalId(),"basis","VERIFIED_TRADE_OBSERVATION:"+completion.actualId(),"revision",((Number)assignment.get("revision")).intValue()+1));
 }
 public void applyReceiptObservation(DomainContext c,com.mulino.application.trade.TradeResidualRemedy.ObservationCompletion completion){
  r.fence(c.organizationId(),completion.rootId());var root=r.require("Roots",c.organizationId(),completion.rootId());
  if(!"RECEIPT_RECONCILIATION".equals(root.get("kind"))||root.get("quantity")!=null)throw DomainError.invalid("Observation reconciliation root required");
  for(var a:r.rows("Assignments",c.organizationId()))if(completion.rootId().equals(a.get("rootId"))&&open(a)){
   var credit=row(c,UUID.randomUUID().toString());credit.putAll(Map.of("rootId",completion.rootId(),"assignmentId",a.get("ID"),"observationId",completion.observationId(),"receiptId",completion.receiptId()));r.insert("ObservationReceiptCredits",credit);
   r.update("Assignments",c.organizationId(),text(a,"ID"),Map.of("status","RESOLVED","evidenceId",completion.occurrenceId(),"basis","VERIFIED_RECEIPT_OBSERVATION","revision",((Number)a.get("revision")).intValue()+1));
  }
 }
 private Map<String,Object> receiptChildAssignment(DomainContext c,Map<String,Object>previous,Map<String,Object>scope){
  var result=new LinkedHashMap<>(previous);result.putAll(row(c,UUID.randomUUID().toString()));result.put("effectiveAt",c.asOf());result.put("scopeId",scope.get("ID"));result.put("quantity",scope.get("quantity"));result.put("predecessorId",previous.get("ID"));result.put("status","OPEN");result.put("valid",true);result.remove("evidenceId");result.remove("basis");return result;
 }
 private static String canonicalScope(String scope){try{var json=new com.fasterxml.jackson.databind.ObjectMapper();var tree=json.readTree(scope);if(!tree.isObject())throw DomainError.invalid("Duty scope object required");return json.writeValueAsString(sort(tree));}catch(DomainError e){throw e;}catch(Exception e){throw DomainError.invalid("Duty scope invalid");}}
 private static Object sort(com.fasterxml.jackson.databind.JsonNode node){if(node.isObject()){var result=new TreeMap<String,Object>();node.fields().forEachRemaining(x->result.put(x.getKey(),sort(x.getValue())));return result;}if(node.isArray()){var result=new ArrayList<Object>();node.forEach(x->result.add(sort(x)));return result;}return node;}
 private static String hash(String s){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
