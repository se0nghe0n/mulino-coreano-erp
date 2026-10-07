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
 private final IdentityAuthorization auth;private final IdentityRepository identity;private final PolicyRepository policy;private final ApprovalRepository approvals;
 public PolicyCommandGuard(IdentityAuthorization auth,IdentityRepository identity,PolicyRepository policy,ApprovalRepository approvals){this.auth=auth;this.identity=identity;this.policy=policy;this.approvals=approvals;}
 public void fence(DomainContext c,CommandPreparation p){policy.fence(c.organizationId());auth.fence(c,p.authorityActors());}
 public void verify(DomainContext c,String capability,String hash,CommandPreparation p,Map<String,Object> intent){
   if(!auth.permittedScopes(c,capability,p.scopes().isEmpty()?null:p.scopes()))throw DomainError.forbidden();
   var selection=rule(c,capability);var rule=selection.rule();
   if(!p.effectClass().equals(rule.get("effectClass")))throw held("Effect class is unresolved");
   String action=(String)rule.get("approvalAction");
   if(!Objects.equals(action,p.approvalAction()))throw held("Approval policy mismatch");
   if(action==null)return;
   var a=approval(c,intent);String approver=(String)a.get("approverId");auth.fence(c,List.of(approver));
   String decision=(String)rule.get("decisionCapability");if(decision==null)throw held("Decision capability is unresolved");
   var actor=identity.actor(c.organizationId(),approver).orElseThrow(DomainError::forbidden);if(!"HUMAN".equals(actor.get("kind")))throw DomainError.forbidden();
   var ac=new DomainContext(c.organizationId(),approver,(String)actor.get("stableRequestOwner"),c.asOf(),c.knownAt());
   if(!auth.permittedScopes(ac,decision,p.scopes())||!decision.equals(a.get("decisionCapability")))throw DomainError.forbidden();
   Instant now=auth.now();
   if(!"APPROVED".equals(a.get("decision"))||!action.equals(a.get("action"))||!hash.equals(a.get("canonicalHash"))||!scopeHash(p.scopes()).equals(a.get("scopeHash"))||!Objects.equals(p.proposalId(),a.get("proposalId"))||p.proposalRevision()!=number(a.get("proposalRevision"))||!Objects.equals(p.targetId(),a.get("targetId"))||!Objects.equals(p.currentRevision(),a.get("targetRevision"))||!selection.policyHash().equals(a.get("policyHash"))||now.isBefore(instant(a.get("decidedAt")))||!now.isBefore(instant(a.get("expiresAt")))||(Boolean.TRUE.equals(a.get("singleUse"))&&approvals.consumed(c,(String)a.get("ID"))))throw DomainError.forbidden();
 }
 public void consume(DomainContext c,CommandPreparation p,Map<String,Object> i,String commandId){if(p.approvalAction()!=null){var a=approval(c,i);if(Boolean.TRUE.equals(a.get("singleUse")))approvals.consume(c,(String)a.get("ID"),commandId,auth.now());}}
 public Selection rule(DomainContext c,String capability){
   var current=policy.current(c.organizationId(),"COMMAND",auth.now());if(current.size()!=1)throw held("Current policy is unresolved");var selected=current.getFirst();
   try{var document=new ObjectMapper().readValue((String)selected.get("content"),Map.class);var rules=(Map<?,?>)document.get("rules");Object found=rules==null?null:rules.get(capability);if(!(found instanceof Map<?,?> rule))throw held("Action policy is unresolved");return new Selection((Map<String,Object>)rule,(String)selected.get("contentHash"));}catch(DomainError e){throw e;}catch(Exception e){throw held("Current policy is invalid");}
 }
 public record Selection(Map<String,Object> rule,String policyHash){}
 private Map<String,Object> approval(DomainContext c,Map<String,Object> i){Object id=i.get("approvalId");if(id==null)id=IdentityCommands.payload(i).get("approvalId");if(!(id instanceof String s))throw DomainError.forbidden();return approvals.find(c,s).orElseThrow(DomainError::forbidden);}
 public static String scopeHash(Map<String,? extends Collection<String>> scopes){var sorted=new TreeMap<String,List<String>>();scopes.forEach((k,v)->sorted.put(k,v.stream().distinct().sorted().toList()));try{return PolicyCommands.hash(new ObjectMapper().writeValueAsString(sorted));}catch(Exception e){throw new IllegalStateException(e);}}
 private static DomainError held(String message){return new DomainError("HELD","POLICY_UNRESOLVED",message);}
 private static int number(Object x){return x instanceof Number n?n.intValue():-1;}
 private static Instant instant(Object v){return v instanceof Instant t?t:Instant.parse(v.toString());}
}
