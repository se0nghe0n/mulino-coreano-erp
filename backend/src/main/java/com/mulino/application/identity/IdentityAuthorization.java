package com.mulino.application.identity;

import com.mulino.application.core.*;
import com.mulino.domain.identity.IdentityRepository;
import java.time.*;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class IdentityAuthorization implements ReadAuthorizer {
  private final IdentityRepository repository;
  private final ExecutionClock clock;
  @org.springframework.beans.factory.annotation.Autowired
  public IdentityAuthorization(IdentityRepository repository,ExecutionClock clock) { this.repository=repository; this.clock=clock; }
  IdentityAuthorization(IdentityRepository repository,Clock clock) { this(repository,new ExecutionClock(clock)); }

  public Instant now() { return clock.instant(); }
  @Override public DomainContext context(Instant asOf,Instant knownAt) {
    var authentication=SecurityContextHolder.getContext().getAuthentication();
    if(authentication==null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof Jwt jwt)
        || jwt.getIssuer()==null || jwt.getSubject()==null) throw denied();
    var external=repository.external(jwt.getIssuer().toString(),jwt.getSubject(),jwt.getClaimAsString("organizationId")).orElseThrow(IdentityAuthorization::denied);
    String org=(String)external.get("organizationId"),actorId=(String)external.get("actorId");
    var actor=repository.actor(org,actorId).orElseThrow(IdentityAuthorization::denied);
    Instant now=clock.instant();
    if(!repository.rows("Memberships",org).stream().anyMatch(r -> actorId.equals(r.get("actorId")) && active(r,now))) throw denied();
    return new DomainContext(org,actorId,(String)actor.get("stableRequestOwner"),asOf==null?now:asOf,knownAt==null?now:knownAt);
  }

  @Override public void authorize(DomainContext context,String capability,String targetId) {
    if(!permitted(context,capability,targetId,null)) throw denied();
  }
  public void authorizeScope(DomainContext context,String capability,String kind,String id) {
    if(!permitted(context,capability,id,kind)) throw denied();
  }
  public boolean permitted(DomainContext context,String capability,String targetId,String kind) {
    if(targetId==null) return permittedScopes(context,capability,null);
    return permittedScopes(context,capability,Map.of(kind==null?"TARGET":kind,List.of(targetId)));
  }
  /** Scope dimensions intersect; IDs within one dimension are alternatives. */
  public boolean permittedScopes(DomainContext context,String capability,Map<String,? extends Collection<String>> targets) {
    return permittedScopes(context,capability,targets,new HashSet<>(),new ArrayList<>());
  }
  public List<Map<String,Object>> authorityEvidence(DomainContext context,String capability,Map<String,? extends Collection<String>> targets) {
    var proof=new ArrayList<Map<String,Object>>();
    if(!permittedScopes(context,capability,targets,new HashSet<>(),proof))throw denied();
    return List.copyOf(proof);
  }
  private boolean permittedScopes(DomainContext context,String capability,Map<String,? extends Collection<String>> targets,Set<String> visited,List<Map<String,Object>> proof) {
    Instant now=clock.instant();
    String org=context.organizationId(),actor=context.actorId();
    if(!visited.add(actor)||visited.size()>32)return false;
    var membership=repository.rows("Memberships",org).stream().filter(r -> actor.equals(r.get("actorId")) && active(r,now)).findFirst().orElse(null);
    if(membership==null)return false;
    var assigned=repository.rows("CapabilityAssignments",org).stream().filter(r -> actor.equals(r.get("actorId")) && capability.equals(r.get("capabilityId")) && active(r,now)).toList();
    var actions=repository.rows("GrantActions",org); var allScopes=repository.rows("GrantScopes",org);
    for(var grant:repository.rows("Grants",org)) {
      if(!actor.equals(grant.get("actorId")) || !active(grant,now)) continue;
      String id=(String)grant.get("ID");
      var parentProof=new ArrayList<Map<String,Object>>();
      Object delegator=grant.get("delegatorId");
      if(delegator instanceof String d&&!d.equals(actor)) {
        var owner=repository.actor(org,d).orElse(null);if(owner==null)continue;
        var parent=new DomainContext(org,d,(String)owner.get("stableRequestOwner"),context.asOf(),context.knownAt());
        if(!permittedScopes(parent,capability,targets,new HashSet<>(visited),parentProof))continue;
      }
      if(actions.stream().noneMatch(r -> id.equals(r.get("grantId")) && capability.equals(r.get("capabilityId")))) continue;
      var scopes=allScopes.stream().filter(r -> id.equals(r.get("grantId"))).toList();
      if(scopes.isEmpty()) continue;
      for(var assignment:assigned) {
        if(targets==null) {
          if(scopes.stream().anyMatch(scope -> overlaps(assignment,scope,org))){appendProof(proof,parentProof,actor,grant,assignment,membership);return true;}
        } else if(matchesTargets(assignment,targets,org)) {
          var dimensions=scopes.stream().filter(r -> !organizationScope(r,org)).map(r -> (String)r.get("scopeKind")).distinct().toList();
          if(dimensions.stream().allMatch(d -> scopes.stream().filter(r -> d.equals(r.get("scopeKind"))).anyMatch(r -> matchesTargets(r,targets,org)))){appendProof(proof,parentProof,actor,grant,assignment,membership);return true;}
        }
      }
    }
    return false;
  }
  private static void appendProof(List<Map<String,Object>> proof,List<Map<String,Object>> parent,String actor,Map<String,Object> grant,Map<String,Object> assignment,Map<String,Object> membership) {
    var row=new LinkedHashMap<String,Object>();row.put("actorId",actor);
    reference(row,"grant",grant);reference(row,"assignment",assignment);reference(row,"membership",membership);
    if(grant.get("delegatorId")!=null)row.put("delegatorId",grant.get("delegatorId"));
    proof.add(Map.copyOf(row));proof.addAll(parent);
  }
  private static void reference(Map<String,Object> output,String kind,Map<String,Object> source) {
    if(source.get("ID")!=null)output.put(kind+"Id",source.get("ID"));
    if(source.get("revision")!=null)output.put(kind+"Revision",source.get("revision"));
  }
  private static boolean matchesTargets(Map<String,Object> scope,Map<String,? extends Collection<String>> targets,String org) {
    if(organizationScope(scope,org)) return true;
    Collection<String> ids=targets.get((String)scope.get("scopeKind"));
    return ids!=null&&ids.contains(scope.get("scopeId"));
  }
  public void fence(DomainContext context,Collection<String> otherActors) {
    var actors=new HashSet<>(otherActors); actors.add(context.actorId());
    var grants=repository.rows("Grants",context.organizationId());
    boolean expanded;do { expanded=false;for(var g:grants)if(actors.contains(g.get("actorId"))&&g.get("delegatorId") instanceof String d)expanded|=actors.add(d); }while(expanded);
    repository.fence(context.organizationId(),actors);
  }
  static boolean overlaps(Map<String,Object> a,Map<String,Object> b,String org) {
    return organizationScope(a,org)||organizationScope(b,org)||Objects.equals(a.get("scopeKind"),b.get("scopeKind"))&&Objects.equals(a.get("scopeId"),b.get("scopeId"));
  }
  static boolean matches(Map<String,Object> scope,String kind,String id,String org) {
    return organizationScope(scope,org) || id.equals(scope.get("scopeId")) && (kind==null||kind.equals(scope.get("scopeKind"))||"TARGET".equals(scope.get("scopeKind")));
  }
  static boolean organizationScope(Map<String,Object> scope,String org) { return "ORGANIZATION".equals(scope.get("scopeKind"))&&org.equals(scope.get("scopeId")); }
  static boolean active(Map<String,Object> row,Instant now) {
    Instant from=instant(row.get("validFrom")),until=instant(row.get("validUntil")),revoked=instant(row.get("revokedAt"));
    return from!=null&&until!=null&&!now.isBefore(from)&&now.isBefore(until)&&(revoked==null||now.isBefore(revoked));
  }
  static Instant instant(Object value) { return value==null?null:value instanceof Instant i?i:Instant.parse(value.toString()); }
  static AccessDeniedException denied() { return new AccessDeniedException("Unavailable authority"); }
}
