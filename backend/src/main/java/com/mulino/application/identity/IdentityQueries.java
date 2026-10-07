package com.mulino.application.identity;

import com.mulino.application.core.*;
import com.mulino.domain.identity.IdentityRepository;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class IdentityQueries implements QueryHandler {
  private final IdentityRepository repository;
  private final IdentityAuthorization auth;
  public IdentityQueries(IdentityRepository repository,IdentityAuthorization auth) { this.repository=repository; this.auth=auth; }
  public Set<String> operations() { return Set.of("getAccessContext","getGrant"); }
  public QueryResult query(DomainContext c,QueryRequest request) {
    auth.authorize(c,request.operation(),null);
    if("getAccessContext".equals(request.operation())) {
      var actor=repository.actor(c.organizationId(),c.actorId()).orElseThrow(IdentityAuthorization::denied);
      return QueryResult.of(Map.of("actorId",c.actorId(),"organizationId",c.organizationId(),"stableRequestOwner",c.stableRequestOwner(),"kind",actor.get("kind"),"assignments",repository.rows("CapabilityAssignments",c.organizationId()).stream().filter(r -> c.actorId().equals(r.get("actorId"))&&IdentityAuthorization.active(r,auth.now())).toList(),"grants",repository.rows("Grants",c.organizationId()).stream().filter(r -> c.actorId().equals(r.get("actorId"))&&IdentityAuthorization.active(r,auth.now())).map(this::grantView).toList()),Map.of("organizationId",c.organizationId(),"actorId",c.actorId()));
    }
    var grant=repository.rows("Grants",c.organizationId()).stream().filter(r -> Objects.equals(request.id(),r.get("ID"))&&(c.actorId().equals(r.get("actorId"))||c.actorId().equals(r.get("delegatorId")))).findFirst().orElseThrow(IdentityAuthorization::denied);
    String id=(String)grant.get("ID");
    Map<String,Collection<String>> scopes=new HashMap<>();
    for(var row:repository.rows("GrantScopes",c.organizationId())) if(id.equals(row.get("grantId")))
      scopes.computeIfAbsent((String)row.get("scopeKind"),key -> new HashSet<>()).add((String)row.get("scopeId"));
    scopes.computeIfAbsent("TARGET",key -> new HashSet<>()).add(id);
    if(!auth.permittedScopes(c,"getGrant",scopes))throw IdentityAuthorization.denied();
    return QueryResult.of(grantView(grant),Map.of("organizationId",c.organizationId()));
  }
  private Map<String,Object> grantView(Map<String,Object> row) {
    var view=new HashMap<>(row); String org=(String)row.get("organizationId"),id=(String)row.get("ID");
    view.put("actions",repository.rows("GrantActions",org).stream().filter(r -> id.equals(r.get("grantId"))).map(r -> r.get("capabilityId")).toList());
    view.put("scopes",repository.rows("GrantScopes",org).stream().filter(r -> id.equals(r.get("grantId"))).toList());
    return view;
  }
}
