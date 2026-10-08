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
 private final Object auditResource=new Object();
 private final IdentityAuthorization auth;private final IdentityRepository identity;private final PolicyRepository policy;private final ApprovalRepository approvals;
 public PolicyCommandGuard(IdentityAuthorization auth,IdentityRepository identity,PolicyRepository policy,ApprovalRepository approvals){this.auth=auth;this.identity=identity;this.policy=policy;this.approvals=approvals;}
 public void fence(DomainContext c,CommandPreparation p){policy.fence(c.organizationId());auth.fence(c,p.authorityActors());}
 public void authorizeReplay(DomainContext c,String capability,Map<String,List<String>> savedScopes){
   // This is only a serialization fence; replay does not select or reconsume policy decisions.
   policy.fence(c.organizationId());auth.fence(c,List.of());
   if(!everyScope(c,capability,savedScopes))throw DomainError.forbidden();
 }
 public void verify(DomainContext c,String capability,String hash,CommandPreparation p,Map<String,Object> intent){
   if(!everyScope(c,capability,p.scopes()))throw DomainError.forbidden();
   var selection=rule(c,capability);var rule=selection.rule();
   if(!p.effectClass().equals(rule.get("effectClass")))throw held("Effect class is unresolved");
   String action=(String)rule.get("approvalAction");String decision=(String)rule.get("decisionCapability");
   // Explicit per-subject approval actions (e.g. WAIVE_<duty kind>) map to their own decision capability; no wildcard.
   if(p.approvalAction()!=null&&rule.get("approvalActions") instanceof Map<?,?> mapped&&mapped.get(p.approvalAction()) instanceof String selected){action=p.approvalAction();decision=selected;}
   if(!Objects.equals(action,p.approvalAction()))throw held("Approval policy mismatch");
   if(action==null){remember(c,capability,hash,p,List.of());rememberFacts(c,capability,hash,p,selection,null,null);return;}
   var a=approval(c,intent);String approver=(String)a.get("approverId");auth.fence(c,List.of(approver));
   if(decision==null)throw held("Decision capability is unresolved");
   var actor=identity.actor(c.organizationId(),approver).orElseThrow(DomainError::forbidden);if(!"HUMAN".equals(actor.get("kind")))throw DomainError.forbidden();
   var ac=new DomainContext(c.organizationId(),approver,(String)actor.get("stableRequestOwner"),c.asOf(),c.knownAt());
   if(!everyScope(ac,decision,p.scopes())||!decision.equals(a.get("decisionCapability")))throw DomainError.forbidden();
   Instant now=auth.now();
   if(!"APPROVED".equals(a.get("decision"))||!action.equals(a.get("action"))||!hash.equals(a.get("canonicalHash"))||!scopeHash(p.scopes()).equals(a.get("scopeHash"))||!Objects.equals(p.proposalId(),a.get("proposalId"))||p.proposalRevision()!=number(a.get("proposalRevision"))||!Objects.equals(p.targetId(),a.get("targetId"))||!Objects.equals(p.currentRevision(),a.get("targetRevision"))||!selection.policyHash().equals(a.get("policyHash"))||now.isBefore(instant(a.get("decidedAt")))||!now.isBefore(instant(a.get("expiresAt")))||(Boolean.TRUE.equals(a.get("singleUse"))&&approvals.consumed(c,(String)a.get("ID"))))throw DomainError.forbidden();
   remember(c,capability,hash,p,List.of(approver));
   rememberFacts(c,capability,hash,p,selection,a,ac);
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
   try{var document=new ObjectMapper().readValue((String)selected.get("content"),Map.class);var rules=(Map<?,?>)document.get("rules");Object found=rules==null?null:rules.get(capability);if(!(found instanceof Map<?,?> rule))throw held("Action policy is unresolved");return new Selection((Map<String,Object>)rule,(String)selected.get("contentHash"),(String)selected.get("ID"),(String)selected.get("version"));}catch(DomainError e){throw e;}catch(Exception e){throw held("Current policy is invalid");}
 }
 public record Selection(Map<String,Object> rule,String policyHash,String policyVersionId,String policyVersion){}
 public Map<String,Object> auditFacts(DomainContext c,String capability,CommandPreparation p,Map<String,Object> intent){
   var facts=(Map<String,Map<String,Object>>)org.springframework.transaction.support.TransactionSynchronizationManager.getResource(auditResource);
   var selected=facts==null?null:facts.get(proofKey(c,capability,CommandRequests.hash(intent)));
   if(selected==null)throw DomainError.forbidden();
   return selected;
 }
 private void rememberFacts(DomainContext c,String capability,String hash,CommandPreparation p,Selection selection,Map<String,Object> approval,DomainContext approver){
   var stored=(Map<String,Map<String,Object>>)org.springframework.transaction.support.TransactionSynchronizationManager.getResource(auditResource);
   if(stored==null){stored=new HashMap<>();org.springframework.transaction.support.TransactionSynchronizationManager.bindResource(auditResource,stored);
     org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization(){public void afterCompletion(int status){org.springframework.transaction.support.TransactionSynchronizationManager.unbindResourceIfPossible(auditResource);}});
   }
   String key=proofKey(c,capability,hash);if(stored.containsKey(key))return;
   var facts=new LinkedHashMap<String,Object>();facts.put("policyVersionId",selection.policyVersionId());facts.put("policyVersion",selection.policyVersion());facts.put("policyHash",selection.policyHash());
   facts.put("authorityChain",authorityChain(c,capability,p.scopes()));
   if(approval!=null){
     for(String field:List.of("ID","canonicalHash","scopeHash","proposalId","proposalRevision","decisionCapability","approverId","policyHash")){
       String label=switch(field){case "ID"->"approvalId";case "canonicalHash"->"approvalCanonicalHash";case "scopeHash"->"approvalScopeHash";case "proposalId"->"approvalProposalId";case "proposalRevision"->"approvalProposalRevision";case "policyHash"->"approvalPolicyHash";default->field;};
       if(approval.get(field)!=null)facts.put(label,approval.get(field));
     }
     facts.put("decisionAuthorityChain",authorityChain(approver,(String)approval.get("decisionCapability"),p.scopes()));
   }
   stored.put(key,Map.copyOf(facts));
 }
 private List<Map<String,Object>> authorityChain(DomainContext c,String capability,Map<String,List<String>> scopes){
   var evidence=new LinkedHashSet<Map<String,Object>>();
   if(scopes.isEmpty())evidence.addAll(auth.authorityEvidence(c,capability,null));
   else for(var candidate:scopeCombinations(scopes))evidence.addAll(auth.authorityEvidence(c,capability,candidate));
   return List.copyOf(evidence);
 }
 private List<Map<String,List<String>>> scopeCombinations(Map<String,List<String>> scopes){
   List<Map<String,List<String>>> combinations=new ArrayList<>();combinations.add(Map.of());
   for(var dimension:new TreeMap<>(scopes).entrySet()){
     if(dimension.getValue().isEmpty()||(long)combinations.size()*dimension.getValue().size()>256)throw DomainError.forbidden();
     var expanded=new ArrayList<Map<String,List<String>>>();
     for(var candidate:combinations)for(String id:dimension.getValue()){var copy=new HashMap<>(candidate);copy.put(dimension.getKey(),List.of(id));expanded.add(copy);}combinations=expanded;
   }
   return combinations;
 }
 private boolean everyScope(DomainContext c,String capability,Map<String,List<String>> scopes){
   if(scopes.isEmpty())return auth.permittedScopes(c,capability,null);
   return scopeCombinations(scopes).stream().allMatch(candidate->auth.permittedScopes(c,capability,candidate));
 }


 private Map<String,Object> approval(DomainContext c,Map<String,Object> i){Object id=i.get("approvalId");if(id==null)id=IdentityCommands.payload(i).get("approvalId");if(!(id instanceof String s))throw DomainError.forbidden();return approvals.find(c,s).orElseThrow(DomainError::forbidden);}
 public static String scopeHash(Map<String,? extends Collection<String>> scopes){return CommandRequests.hash(Map.of("scope",scopes));}

 private static DomainError held(String message){return new DomainError("HELD","POLICY_UNRESOLVED",message);}
 private static int number(Object x){return x instanceof Number n?n.intValue():-1;}
 private static Instant instant(Object v){return v instanceof Instant t?t:Instant.parse(v.toString());}
}
