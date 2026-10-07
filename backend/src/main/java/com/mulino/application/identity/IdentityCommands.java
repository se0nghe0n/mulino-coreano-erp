package com.mulino.application.identity;
import com.mulino.application.core.*;
import com.mulino.domain.identity.IdentityRepository;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;
@Component
public class IdentityCommands implements CommandHandler {
 private final IdentityRepository repo;private final IdentityAuthorization auth;private final IdentityControlGuard guard;
 public IdentityCommands(IdentityRepository repo,IdentityAuthorization auth,IdentityControlGuard guard){this.repo=repo;this.auth=auth;this.guard=guard;}
 public Set<String> capabilities(){return Set.of("createGrant","revokeGrant","assignCapability","revokeCapability");}
 public boolean mutatesAuthorization(String capability){return capabilities().contains(capability);}
 public CommandPreparation prepare(DomainContext c,Map<String,Object> i){
   var p=payload(i);String op=op(i),recipient;
   Map<String,Object> row=null;
   if(op.startsWith("revoke")){row=find(op.equals("revokeGrant")?"Grants":"CapabilityAssignments",c,text(p,"id"));recipient=(String)row.get("actorId");}
   else recipient=text(p,"actorId");
   Map<String,List<String>> scopes=new HashMap<>();
   if(op.equals("createGrant")) for(var s:scopes(p)) scopes.computeIfAbsent(s.kind(),k->new ArrayList<>()).add(s.id());
   else if(op.equals("assignCapability"))scopes.put(text(p,"scopeKind"),List.of(text(p,"scopeId")));
   else if(op.equals("revokeGrant"))for(var s:repo.rows("GrantScopes",c.organizationId()))if(row.get("ID").equals(s.get("grantId")))scopes.computeIfAbsent((String)s.get("scopeKind"),k->new ArrayList<>()).add((String)s.get("scopeId"));
   else scopes.put((String)row.get("scopeKind"),List.of((String)row.get("scopeId")));
   return new CommandPreparation(scopes,List.of(),Set.of(recipient),"IDENTITY_CONTROL",null,null,0,row==null?recipient:(String)row.get("ID"),row==null?null:((Number)row.get("revision")).intValue());
 }
 public Map<String,Object> execute(DomainContext c,Map<String,Object> i){
   var p=payload(i);String op=op(i),id;String actor;Instant now=auth.now();
   if(op.equals("createGrant")){
     actor=text(p,"actorId");Set<String> actions=new HashSet<>((List<String>)p.get("actions"));var scopes=scopes(p);Instant from=instant(p,"validFrom"),until=instant(p,"validUntil");
     guard.creatingGrant(c,actor,actions,scopes,from,until);id=UUID.randomUUID().toString();
     repo.invalidate(c.organizationId(),actor,until,"GRANT_EXPIRY");
     repo.insert("Grants",Map.of("organizationId",c.organizationId(),"ID",id,"actorId",actor,"delegatorId",c.actorId(),"validFrom",from,"validUntil",until,"revision",1,"createdAt",now,"recordedAt",now));
     for(String a:actions)repo.insert("GrantActions",Map.of("organizationId",c.organizationId(),"grantId",id,"capabilityId",a));
     for(var s:scopes)repo.insert("GrantScopes",Map.of("organizationId",c.organizationId(),"grantId",id,"scopeKind",s.kind(),"scopeId",s.id()));
   }else if(op.equals("assignCapability")){
     actor=text(p,"actorId");String capability=text(p,"assignedCapability"),kind=text(p,"scopeKind"),scope=text(p,"scopeId");management(c,capability,kind,scope);repo.actor(c.organizationId(),actor).orElseThrow(IdentityAuthorization::denied);
     if(!repo.rows("Memberships",c.organizationId()).stream().anyMatch(r->actor.equals(r.get("actorId"))&&IdentityAuthorization.active(r,now)))throw IdentityAuthorization.denied();
     Instant from=instant(p,"validFrom"),until=instant(p,"validUntil");if(!until.isAfter(from)||!until.isAfter(now))throw DomainError.invalid("Invalid authority interval");
     id=UUID.randomUUID().toString();repo.invalidate(c.organizationId(),actor,until,"CAPABILITY_EXPIRY");repo.insert("CapabilityAssignments",fields("organizationId",c.organizationId(),"ID",id,"actorId",actor,"capabilityId",capability,"scopeKind",kind,"scopeId",scope,"validFrom",from,"validUntil",until,"revision",1,"createdAt",now,"recordedAt",now));
   }else{
     id=text(p,"id");String entity=op.equals("revokeGrant")?"Grants":"CapabilityAssignments";var row=find(entity,c,id);actor=(String)row.get("actorId");
     if(op.equals("revokeGrant"))guard.revokingGrant(c,id,((Number)row.get("revision")).longValue());else management(c,(String)row.get("capabilityId"),(String)row.get("scopeKind"),(String)row.get("scopeId"));
     repo.update(entity,c.organizationId(),id,Map.of("revokedAt",now,"revision",((Number)row.get("revision")).intValue()+1));
   }
   repo.invalidate(c.organizationId(),actor,now,op);return Map.of("outcome","APPLIED","effects",Map.of("identityId",id),"id",id,"actorId",actor,"revision",op.startsWith("revoke")?((Number)find(op.equals("revokeGrant")?"Grants":"CapabilityAssignments",c,id).get("revision")).intValue():1);
 }
 private void management(DomainContext c,String capability,String kind,String scope){
   var actor=repo.actor(c.organizationId(),c.actorId()).orElseThrow(IdentityAuthorization::denied);if(!"HUMAN".equals(actor.get("kind")))throw IdentityAuthorization.denied();
   if(repo.rows("ManagementAuthorities",c.organizationId()).stream().noneMatch(r->c.actorId().equals(r.get("actorId"))&&capability.equals(r.get("capabilityId"))&&IdentityAuthorization.active(r,auth.now())&&IdentityAuthorization.matches(r,kind,scope,c.organizationId())))throw IdentityAuthorization.denied();
 }
 public static Map<String,Object> fields(Object... values){var result=new HashMap<String,Object>();for(int x=0;x<values.length;x+=2)result.put((String)values[x],values[x+1]);return result;}
 private Map<String,Object> find(String e,DomainContext c,String id){return repo.rows(e,c.organizationId()).stream().filter(r->id.equals(r.get("ID"))).findFirst().orElseThrow(IdentityAuthorization::denied);}
 public static Map<String,Object> payload(Map<String,Object> i){return i.get("slots") instanceof Map<?,?> p?(Map<String,Object>)p:i.get("payload") instanceof Map<?,?> p?(Map<String,Object>)p:i;}
 public static String op(Map<String,Object> i){Object v=i.get("capabilityId");if(v==null)v=i.get("capability");return Objects.toString(v,"");}
 public static String text(Map<String,Object> p,String k){Object v=p.get(k);if(!(v instanceof String s)||s.isBlank())throw DomainError.invalid("Missing "+k);return s;}
 private static Instant instant(Map<String,Object> p,String k){try{return Instant.parse(text(p,k));}catch(Exception e){throw DomainError.invalid("Invalid "+k);}}
 private static Set<IdentityControlGuard.Scope> scopes(Map<String,Object> p){if(!(p.get("scopes") instanceof List<?> list)||list.isEmpty())throw DomainError.invalid("Missing scopes");var out=new HashSet<IdentityControlGuard.Scope>();for(Object v:list){var m=(Map<String,Object>)v;out.add(new IdentityControlGuard.Scope(text(m,"scopeKind"),text(m,"scopeId")));}return out;}
}
