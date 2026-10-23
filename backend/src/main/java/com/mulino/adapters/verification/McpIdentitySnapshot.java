package com.mulino.adapters.verification;

import com.mulino.application.core.TransportValues;
import com.mulino.application.identity.IdentityAuthorization;
import com.mulino.domain.identity.IdentityRepository;
import com.sap.cds.services.runtime.CdsRuntime;
import java.util.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.*;

/** Observation only: no authorize/permitted/fence/write calls and no claim that a grant permits an action. */
public class McpIdentitySnapshot {
  private final IdentityAuthorization auth;
  private final IdentityRepository repository;
  private final CdsRuntime runtime;
  McpIdentitySnapshot(IdentityAuthorization auth,IdentityRepository repository,CdsRuntime runtime) {
    this.auth=auth;this.repository=repository;this.runtime=runtime;
  }
  @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
  public Map<String,Object> capture() {
    return runtime.requestContext().run(ignored -> {
      var authentication=SecurityContextHolder.getContext().getAuthentication();
      if(authentication==null||!authentication.isAuthenticated()||!(authentication.getPrincipal() instanceof Jwt jwt))
        throw new IllegalStateException("Capture identity unavailable");
      var c=auth.context(null,null); // The product's external identity + current membership resolver, never payload actor/owner.
      var identity=new LinkedHashMap<String,Object>();
      identity.put("issuer",jwt.getIssuer().toString());identity.put("subject",jwt.getSubject());
      identity.put("audiences",jwt.getAudience().stream().sorted().toList());
      identity.put("organizationAlias",jwt.getClaimAsString("organizationId"));
      identity.put("organizationId",c.organizationId());identity.put("actorId",c.actorId());
      identity.put("stableRequestOwner",c.stableRequestOwner());
      for(Object v:identity.values())if(v==null||v.toString().isBlank())throw new IllegalStateException("Incomplete capture identity");
      if(jwt.getAudience().isEmpty())throw new IllegalStateException("Incomplete capture audience");
      var grants=new ArrayList<Map<String,Object>>();
      var actions=repository.rows("GrantActions",c.organizationId());
      var scopes=repository.rows("GrantScopes",c.organizationId());
      for(var row:repository.rows("Grants",c.organizationId()))if(c.actorId().equals(row.get("actorId"))) {
        var g=pick(row,"ID","actorId","delegatorId","validFrom","validUntil","revokedAt","revision");
        g.put("actions",actions.stream().filter(a->row.get("ID").equals(a.get("grantId"))).map(a->pick(a,"capabilityId")).toList());
        g.put("scopes",scopes.stream().filter(s->row.get("ID").equals(s.get("grantId"))).map(s->pick(s,"scopeKind","scopeId")).toList());
        grants.add(g);
      }
      if(grants.size()>256)throw new IllegalStateException("Capture grant limit");
      var result=new LinkedHashMap<String,Object>();result.put("identity",identity);result.put("grants",grants);
      result.put("memberships",repository.rows("Memberships",c.organizationId()).stream().filter(r->c.actorId().equals(r.get("actorId"))).map(r->pick(r,"ID","validFrom","validUntil","revokedAt","revision")).toList());
      result.put("assignments",repository.rows("CapabilityAssignments",c.organizationId()).stream().filter(r->c.actorId().equals(r.get("actorId"))).map(r->pick(r,"ID","capabilityId","scopeKind","scopeId","validFrom","validUntil","revokedAt","revision")).toList());
      result.put("observedAt",java.time.Instant.now().toString());result.put("authorityAsOf",auth.now().toString());
      result.put("actionAuthorization","NOT_ASSERTED");
      result.put("observationPhase","BEFORE_DISPATCH_READ_SNAPSHOT");
      return (Map<String,Object>)TransportValues.normalize(result);
    });
  }
  private static Map<String,Object> pick(Map<String,Object> row,String... fields) {
    var result=new LinkedHashMap<String,Object>();for(String field:fields)result.put(field,row.get(field));return result;
  }
}
