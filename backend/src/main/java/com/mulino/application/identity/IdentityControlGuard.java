package com.mulino.application.identity;

import com.mulino.application.core.DomainContext;
import com.mulino.domain.identity.IdentityRepository;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;

/** Guard port for the S2 atomic command boundary; this is deliberately not a write API. */
@Component
public class IdentityControlGuard {
  public record Scope(String kind,String id) {}
  private final IdentityAuthorization auth;
  private final IdentityRepository repository;
  public IdentityControlGuard(IdentityAuthorization auth,IdentityRepository repository) { this.auth=auth; this.repository=repository; }

  public void creatingGrant(DomainContext context,String recipient,Set<String> capabilities,Set<Scope> scopes,Instant from,Instant until) {
    auth.fence(context,List.of(recipient));
    auth.authorize(context,"createGrant",null);
    var actor=repository.actor(context.organizationId(),context.actorId()).orElseThrow(IdentityAuthorization::denied);
    if(!"HUMAN".equals(actor.get("kind"))||recipient.equals(context.actorId())||capabilities.isEmpty()||scopes.isEmpty()||!until.isAfter(from)) throw IdentityAuthorization.denied();
    repository.actor(context.organizationId(),recipient).orElseThrow(IdentityAuthorization::denied);
    Map<String,Collection<String>> requested=new HashMap<>();
    for(Scope scope:scopes) requested.computeIfAbsent(scope.kind(),k -> new HashSet<>()).add(scope.id());
    var combinations=scopeCombinations(requested);
    for(String capability:capabilities) {
      for(var candidate:combinations) if(!auth.permittedScopes(context,capability,candidate)) throw IdentityAuthorization.denied();
      for(Scope scope:scopes) if("ORGANIZATION".equals(scope.kind()) && !organizationAuthority(context,capability)) throw IdentityAuthorization.denied();
    }
    // Delegation validity cannot exceed any current authority's earliest terminal boundary.
    Instant bound=delegationBoundary(context,capabilities,scopes);
    if(from.isBefore(Instant.now())||until.isAfter(bound)) throw IdentityAuthorization.denied();
  }
  private List<Map<String,Collection<String>>> scopeCombinations(Map<String,Collection<String>> scopes) {
    List<Map<String,Collection<String>>> result=new ArrayList<>();result.add(Map.of());
    for(var dimension:scopes.entrySet()) {
      if((long)result.size()*dimension.getValue().size()>256) throw IdentityAuthorization.denied();
      List<Map<String,Collection<String>>> expanded=new ArrayList<>();
      for(var candidate:result) for(String id:dimension.getValue()) {
        var copy=new HashMap<>(candidate);copy.put(dimension.getKey(),List.of(id));expanded.add(copy);
      }
      result=expanded;
    }
    return result;
  }
  public void revokingGrant(DomainContext context,String grantId,long expectedRevision) {
    var grant=repository.rows("Grants",context.organizationId()).stream().filter(r -> grantId.equals(r.get("ID"))).findFirst().orElseThrow(IdentityAuthorization::denied);
    auth.fence(context,List.of((String)grant.get("actorId")));
    auth.authorize(context,"revokeGrant",null);
    if(!context.actorId().equals(grant.get("delegatorId"))||((Number)grant.get("revision")).longValue()!=expectedRevision) throw IdentityAuthorization.denied();
  }
  public void assigningCapability(DomainContext context,String recipient,String capability,Scope scope) {
    auth.fence(context,List.of(recipient));
    auth.authorizeScope(context,"assignCapability",scope.kind(),scope.id());
    repository.actor(context.organizationId(),recipient).orElseThrow(IdentityAuthorization::denied);
    // Identity administrator management authority is itself assigned, scoped and currently granted.
    // The administrator must also possess the exact capability to prevent self-amplification.
    if(recipient.equals(context.actorId())) throw IdentityAuthorization.denied();
    auth.authorizeScope(context,capability,scope.kind(),scope.id());
  }
  private boolean organizationAuthority(DomainContext c,String capability) {
    return auth.permitted(c,capability,c.organizationId(),"ORGANIZATION");
  }
  private Instant delegationBoundary(DomainContext c,Set<String> capabilities,Set<Scope> scopes) {
    Instant now=Instant.now(),bound=Instant.MAX;
    for(var row:repository.rows("Memberships",c.organizationId())) if(c.actorId().equals(row.get("actorId"))&&IdentityAuthorization.active(row,now)) bound=earliest(bound,row);
    for(var row:repository.rows("CapabilityAssignments",c.organizationId())) if(c.actorId().equals(row.get("actorId"))&&capabilities.contains(row.get("capabilityId"))&&IdentityAuthorization.active(row,now)) bound=earliest(bound,row);
    for(var row:repository.rows("Grants",c.organizationId())) if(c.actorId().equals(row.get("actorId"))&&IdentityAuthorization.active(row,now)) bound=earliest(bound,row);
    return bound;
  }
  private Instant earliest(Instant bound,Map<String,Object> row) {
    Instant end=IdentityAuthorization.instant(row.get("validUntil")),revoked=IdentityAuthorization.instant(row.get("revokedAt"));
    if(revoked!=null&&revoked.isBefore(end))end=revoked;
    return end.isBefore(bound)?end:bound;
  }
}
