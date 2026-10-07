package com.mulino.application.policy;
import com.mulino.application.core.*;
import com.mulino.application.identity.*;
import com.mulino.domain.governance.PolicyRepository;
import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.stereotype.Component;
import static com.mulino.application.identity.IdentityCommands.*;
@Component
public class PolicyCommands implements CommandHandler {
 private final PolicyRepository repo;private final IdentityAuthorization auth;private final com.mulino.domain.identity.IdentityRepository identity;
 public PolicyCommands(PolicyRepository repo,IdentityAuthorization auth,com.mulino.domain.identity.IdentityRepository identity){this.repo=repo;this.auth=auth;this.identity=identity;}
 public Set<String> capabilities(){return Set.of("createPolicyDraft","approvePolicy","activatePolicy","retirePolicy");}
 public boolean mutatesAuthorization(String capability){return Set.of("activatePolicy","retirePolicy").contains(capability);}
 public CommandPreparation prepare(DomainContext c,Map<String,Object> i){
   var p=payload(i);var row=op(i).equals("createPolicyDraft")?null:draft(c,text(p,"id"));
   var actors=new HashSet<String>();if(row!=null)for(var a:repo.rows("PolicyApprovals",c.organizationId()))if(row.get("ID").equals(a.get("draftId")))actors.add((String)a.get("approverId"));
   return new CommandPreparation(Map.of("ORGANIZATION",List.of(c.organizationId())),List.of(),actors,"POLICY_CONTROL",null,null,0,row==null?null:(String)row.get("ID"),row==null?null:((Number)row.get("revision")).intValue());
 }
 public Map<String,Object> execute(DomainContext c,Map<String,Object> i){
   var p=payload(i);String operation=op(i),id;int revision;Instant now=auth.now();
   if(operation.equals("createPolicyDraft")){
     id=UUID.randomUUID().toString();String content=text(p,"content"),source=text(p,"source"),evidence=text(p,"regressionEvidence");
     Instant from=Instant.parse(text(p,"effectiveFrom"));Instant until=p.get("effectiveUntil")==null?null:Instant.parse(text(p,"effectiveUntil"));
     if(until!=null&&!until.isAfter(from))throw DomainError.invalid("Invalid policy interval");
     if(!Boolean.TRUE.equals(p.get("fixtureOnly")))throw new DomainError("HELD","OPERATING_POLICY_NOT_CONFIGURED","Actual regulatory policy review is pending");
     var row=new HashMap<String,Object>(fields("organizationId",c.organizationId(),"ID",id,"kind",text(p,"kind"),"version",text(p,"version"),"content",content,"contentHash",hash(content),"source",source,"regressionEvidence",evidence,"effectiveFrom",from,"revision",1));
     row.put("effectiveUntil",until);row.put("legallyRestrictive",Boolean.TRUE.equals(p.get("legallyRestrictive")));row.put("fixtureOnly",true);row.put("status","DRAFT");repo.insert("PolicyDrafts",row);revision=1;
   }else{
     id=text(p,"id");var row=draft(c,id);String status=(String)row.get("status");revision=((Number)row.get("revision")).intValue()+1;
     if(operation.equals("approvePolicy")){
       if(!"HUMAN".equals(identity.actor(c.organizationId(),c.actorId()).orElseThrow(DomainError::forbidden).get("kind")))throw DomainError.forbidden();
       if(!status.equals("DRAFT"))throw DomainError.invalid("Policy is not draft");
       if(!text(p,"contentHash").equals(row.get("contentHash")))throw DomainError.invalid("Policy content changed");
       repo.insert("PolicyApprovals",Map.of("organizationId",c.organizationId(),"ID",UUID.randomUUID().toString(),"draftId",id,"contentHash",row.get("contentHash"),"approverId",c.actorId(),"approvedAt",now));
       repo.update("PolicyDrafts",c.organizationId(),id,Map.of("status","APPROVED","revision",revision));
     }else if(operation.equals("activatePolicy")){
       if(!status.equals("APPROVED"))throw DomainError.invalid("Policy has no approval");
       var approval=repo.rows("PolicyApprovals",c.organizationId()).stream().filter(a->id.equals(a.get("draftId"))&&row.get("contentHash").equals(a.get("contentHash"))).findFirst().orElseThrow(DomainError::forbidden);
       DomainContext approver=new DomainContext(c.organizationId(),(String)approval.get("approverId"),c.stableRequestOwner(),c.asOf(),c.knownAt());
       if(!auth.permittedScopes(approver,"approvePolicy",Map.of("ORGANIZATION",List.of(c.organizationId()))))throw DomainError.forbidden();
       var published=new HashMap<String,Object>();for(String key:List.of("organizationId","ID","kind","version","content","contentHash","effectiveFrom","effectiveUntil"))published.put(key,row.get(key));published.put("revision",1);published.put("createdAt",now);
       repo.insert("PolicyVersions",published);repo.activate(c.organizationId(),(String)row.get("kind"),id);repo.update("PolicyDrafts",c.organizationId(),id,Map.of("status","ACTIVE","revision",revision));boundary(c,id,instant(row.get("effectiveFrom")),"ACTIVATE");if(row.get("effectiveUntil")!=null)boundary(c,id,instant(row.get("effectiveUntil")),"EXPIRE");
     }else{
       if(!status.equals("ACTIVE"))throw DomainError.invalid("Policy is not active");text(p,"terminationBasis");
       if(Boolean.TRUE.equals(row.get("legallyRestrictive"))&&(row.get("effectiveUntil")==null||now.isBefore(instant(row.get("effectiveUntil"))))){
         String replacement=text(p,"replacementPolicyId");if(repo.current(c.organizationId(),(String)row.get("kind"),now).stream().noneMatch(r->replacement.equals(r.get("ID"))&&!id.equals(replacement)))throw new DomainError("HELD","LEGAL_RESTRICTION_ACTIVE","Replacement policy is required");
       }
       repo.retire(c.organizationId(),id);repo.update("PolicyDrafts",c.organizationId(),id,Map.of("status","RETIRED","revision",revision));boundary(c,id,now,"RETIRE");
     }
   }return Map.of("outcome","APPLIED","effects",Map.of("policyId",id),"id",id,"revision",revision);
 }
 private void boundary(DomainContext c,String id,Instant at,String reason){repo.insert("PolicyBoundaries",Map.of("organizationId",c.organizationId(),"ID",UUID.randomUUID().toString(),"policyId",id,"nextCheckAt",at,"reason",reason));}
 private Map<String,Object> draft(DomainContext c,String id){return repo.row("PolicyDrafts",c.organizationId(),id).orElseThrow(DomainError::forbidden);}
 private static Instant instant(Object v){return v instanceof Instant i?i:Instant.parse(v.toString());}
 public static String hash(String content){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
