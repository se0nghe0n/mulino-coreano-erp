package com.mulino.application.policy;
import com.mulino.application.core.*;
import com.mulino.application.identity.*;
import com.mulino.domain.governance.*;
import com.mulino.domain.identity.IdentityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;
@Component
public class PolicyCommandGuard implements CommandGuard {
 private final Object proofResource=new Object();
 private final IdentityAuthorization auth;private final IdentityRepository identity;private final PolicyRepository policy;private final ApprovalRepository approvals;
 public PolicyCommandGuard(IdentityAuthorization auth,IdentityRepository identity,PolicyRepository policy,ApprovalRepository approvals){this.auth=auth;this.identity=identity;this.policy=policy;this.approvals=approvals;}
 public void fence(DomainContext c,CommandPreparation p){policy.fence(c.organizationId());auth.fence(c,p.authorityActors());}
 public void verify(DomainContext c,String capability,String hash,CommandPreparation p,Map<String,Object> intent){
   if(!everyScope(c,capability,p.scopes()))throw DomainError.forbidden();
   var selection=rule(c,capability);var rule=selection.rule();
   if(!p.effectClass().equals(rule.get("effectClass")))throw held("Effect class is unresolved");
   String action=(String)rule.get("approvalAction");
   if(!Objects.equals(action,p.approvalAction()))throw held("Approval policy mismatch");
   if(action==null){remember(c,capability,hash,p,List.of());return;}
   var a=approval(c,intent);String approver=(String)a.get("approverId");auth.fence(c,List.of(approver));
   String decision=(String)rule.get("decisionCapability");if(decision==null)throw held("Decision capability is unresolved");
   var actor=identity.actor(c.organizationId(),approver).orElseThrow(DomainError::forbidden);if(!"HUMAN".equals(actor.get("kind")))throw DomainError.forbidden();
   var ac=new DomainContext(c.organizationId(),approver,(String)actor.get("stableRequestOwner"),c.asOf(),c.knownAt());
   if(!everyScope(ac,decision,p.scopes())||!decision.equals(a.get("decisionCapability")))throw DomainError.forbidden();
   Instant now=auth.now();
   if(!"APPROVED".equals(a.get("decision"))||!action.equals(a.get("action"))||!hash.equals(a.get("canonicalHash"))||!scopeHash(p.scopes()).equals(a.get("scopeHash"))||!Objects.equals(p.proposalId(),a.get("proposalId"))||p.proposalRevision()!=number(a.get("proposalRevision"))||!Objects.equals(p.targetId(),a.get("targetId"))||!Objects.equals(p.currentRevision(),a.get("targetRevision"))||!selection.policyHash().equals(a.get("policyHash"))||now.isBefore(instant(a.get("decidedAt")))||!now.isBefore(instant(a.get("expiresAt")))||(Boolean.TRUE.equals(a.get("singleUse"))&&approvals.consumed(c,(String)a.get("ID"))))throw DomainError.forbidden();
   remember(c,capability,hash,p,List.of(approver));
   var proofs=(Map<String,Instant>)org.springframework.transaction.support.TransactionSynchronizationManager.getResource(proofResource);
   proofs.merge(proofKey(c,capability,hash),instant(a.get("expiresAt")),(x,y)->x.isBefore(y)?x:y);
 }
 public void verifyCommit(DomainContext c,String capability,String hash,CommandPreparation p,Map<String,Object> intent,boolean mutatesAuthorization){
   if(!mutatesAuthorization){verify(c,capability,hash,p,intent);return;}
   if(!Set.of("createGrant","revokeGrant","assignCapability","revokeCapability","activatePolicy","retirePolicy").contains(capability)||!Set.of("IDENTITY_CONTROL","POLICY_CONTROL").contains(p.effectClass()))throw DomainError.forbidden();
   var proofs=(Map<String,Instant>)org.springframework.transaction.support.TransactionSynchronizationManager.getResource(proofResource);
   Instant boundary=proofs==null?null:proofs.get(proofKey(c,capability,hash));
   if(boundary==null||!auth.now().isBefore(boundary))throw DomainError.forbidden();
 }
 private String proofKey(DomainContext c,String capability,String hash){return c.organizationId()+"/"+c.actorId()+"/"+capability+"/"+hash;}
 private void remember(DomainContext c,String capability,String hash,CommandPreparation p,List<String> extra){
   if(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Authority proof requires transaction");
   var proofs=(Map<String,Instant>)org.springframework.transaction.support.TransactionSynchronizationManager.getResource(proofResource);
   if(proofs==null){proofs=new HashMap<>();org.springframework.transaction.support.TransactionSynchronizationManager.bindResource(proofResource,proofs);
     org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization(){public void afterCompletion(int status){org.springframework.transaction.support.TransactionSynchronizationManager.unbindResourceIfPossible(proofResource);}});
   }
   Instant boundary=Instant.MAX,now=auth.now();var actors=new HashSet<>(p.authorityActors());actors.add(c.actorId());actors.addAll(extra);
   var grants=identity.rows("Grants",c.organizationId());boolean expanded;do{expanded=false;for(var g:grants)if(actors.contains(g.get("actorId"))&&g.get("delegatorId") instanceof String d)expanded|=actors.add(d);}while(expanded);
   for(String entity:List.of("Memberships","CapabilityAssignments","Grants"))for(var row:identity.rows(entity,c.organizationId()))if(actors.contains(row.get("actorId"))&&active(row,now)){
     Instant until=instant(row.get("validUntil"));if(until.isBefore(boundary))boundary=until;Object revoked=row.get("revokedAt");if(revoked!=null&&instant(revoked).isBefore(boundary))boundary=instant(revoked);
   }
   for(var row:policy.current(c.organizationId(),"COMMAND",now))if(row.get("effectiveUntil")!=null&&instant(row.get("effectiveUntil")).isBefore(boundary))boundary=instant(row.get("effectiveUntil"));
   proofs.putIfAbsent(proofKey(c,capability,hash),boundary);
 }
 private static boolean active(Map<String,Object> r,Instant now){return r.get("validFrom")!=null&&r.get("validUntil")!=null&&!now.isBefore(instant(r.get("validFrom")))&&now.isBefore(instant(r.get("validUntil")))&&(r.get("revokedAt")==null||now.isBefore(instant(r.get("revokedAt"))));}
 public void consume(DomainContext c,CommandPreparation p,Map<String,Object> i,String commandId){if(p.approvalAction()!=null){var a=approval(c,i);if(Boolean.TRUE.equals(a.get("singleUse")))approvals.consume(c,(String)a.get("ID"),commandId,auth.now());}}
 public Selection rule(DomainContext c,String capability){
   var current=policy.current(c.organizationId(),"COMMAND",auth.now());if(current.size()!=1)throw held("Current policy is unresolved");var selected=current.getFirst();
   try{var document=new ObjectMapper().readValue((String)selected.get("content"),Map.class);var rules=(Map<?,?>)document.get("rules");Object found=rules==null?null:rules.get(capability);if(!(found instanceof Map<?,?> rule))throw held("Action policy is unresolved");return new Selection((Map<String,Object>)rule,(String)selected.get("contentHash"));}catch(DomainError e){throw e;}catch(Exception e){throw held("Current policy is invalid");}
 }
 public record Selection(Map<String,Object> rule,String policyHash){}
 private boolean everyScope(DomainContext c,String capability,Map<String,List<String>> scopes){
   if(scopes.isEmpty())return auth.permittedScopes(c,capability,null);
   List<Map<String,List<String>>> combinations=new ArrayList<>();combinations.add(Map.of());
   for(var dimension:new TreeMap<>(scopes).entrySet()){
     if(dimension.getValue().isEmpty()||(long)combinations.size()*dimension.getValue().size()>256)return false;
     var expanded=new ArrayList<Map<String,List<String>>>();
     for(var candidate:combinations)for(String id:dimension.getValue()){var copy=new HashMap<>(candidate);copy.put(dimension.getKey(),List.of(id));expanded.add(copy);}combinations=expanded;
   }
   return combinations.stream().allMatch(candidate->auth.permittedScopes(c,capability,candidate));
 }

 private Map<String,Object> approval(DomainContext c,Map<String,Object> i){Object id=i.get("approvalId");if(id==null)id=IdentityCommands.payload(i).get("approvalId");if(!(id instanceof String s))throw DomainError.forbidden();return approvals.find(c,s).orElseThrow(DomainError::forbidden);}
 public static String scopeHash(Map<String,? extends Collection<String>> scopes){return CommandRequests.hash(Map.of("scope",scopes));}

 private static DomainError held(String message){return new DomainError("HELD","POLICY_UNRESOLVED",message);}
 private static int number(Object x){return x instanceof Number n?n.intValue():-1;}
 private static Instant instant(Object v){return v instanceof Instant t?t:Instant.parse(v.toString());}
}
