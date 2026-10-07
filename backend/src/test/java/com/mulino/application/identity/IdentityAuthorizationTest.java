package com.mulino.application.identity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.mulino.domain.identity.IdentityRepository;
import com.mulino.application.core.DomainContext;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class IdentityAuthorizationTest {
  final String org="00000000-0000-0000-0000-000000000001",actor="00000000-0000-0000-0000-000000000002",owner="00000000-0000-0000-0000-000000000003",target="00000000-0000-0000-0000-000000000004",grant="00000000-0000-0000-0000-000000000005";
  final Instant now=Instant.parse("2026-10-07T12:00:00Z");
  IdentityRepository repository; IdentityAuthorization auth; DomainContext context;
  Map<String,Object> current() { return Map.of("actorId",actor,"validFrom",now.minusSeconds(10),"validUntil",now.plusSeconds(10)); }
  @BeforeEach void setup() {
    repository=mock(IdentityRepository.class); auth=new IdentityAuthorization(repository,Clock.fixed(now,ZoneOffset.UTC));context=new DomainContext(org,actor,owner,now,now);
    when(repository.rows("Memberships",org)).thenReturn(List.of(current()));
    var assignment=new HashMap<>(current());assignment.putAll(Map.of("capabilityId","getObject","scopeKind","TARGET","scopeId",target));
    when(repository.rows("CapabilityAssignments",org)).thenReturn(List.of(assignment));
    var g=new HashMap<>(current());g.put("ID",grant);when(repository.rows("Grants",org)).thenReturn(List.of(g));
    when(repository.rows("GrantActions",org)).thenReturn(List.of(Map.of("grantId",grant,"capabilityId","getObject")));
    when(repository.rows("GrantScopes",org)).thenReturn(List.of(Map.of("grantId",grant,"scopeKind","TARGET","scopeId",target)));
  }
  @AfterEach void clear() { SecurityContextHolder.clearContext(); }
  @Test void externalSubjectMapsToActorAndOwnerComesFromServer() {
    when(repository.external("https://fixture.invalid","writer-a","org-alias")).thenReturn(Optional.of(Map.of("organizationId",org,"actorId",actor)));
    when(repository.actor(org,actor)).thenReturn(Optional.of(Map.of("stableRequestOwner",owner)));
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("synthetic-unit-context").header("alg","RS256").issuer("https://fixture.invalid").subject("writer-a").claim("organizationId","org-alias").claim("stableRequestOwner","forged-owner").claim("roles",List.of("ADMIN")).build(),List.of()));
    assertEquals(context,auth.context(now,now));
  }
  @Test void scopedSearchAndDirectReadDoNotAmplify() {
    auth.authorize(context,"getObject",null);auth.authorize(context,"getObject",target);
    assertFalse(auth.permitted(context,"getObject",owner,null));
    assertThrows(AccessDeniedException.class,()->auth.authorize(context,"dispatchQuantity",target));
  }
  @Test void expiryAndRevocationCannotBeRevivedByHistoricalAsOf() {
    var revoked=new HashMap<>(current());revoked.put("ID",grant);revoked.put("revokedAt",now);
    when(repository.rows("Grants",org)).thenReturn(List.of(revoked));
    assertThrows(AccessDeniedException.class,()->auth.authorize(new DomainContext(org,actor,owner,now.minusSeconds(5),now.minusSeconds(5)),"getObject",target));
    assertFalse(IdentityAuthorization.active(Map.of("validFrom",now.minusSeconds(10),"validUntil",now),now));
  }
  @Test void assignmentGrantIntersectionRejectsDisjointScopesAndMissingMembership() {
    when(repository.rows("GrantScopes",org)).thenReturn(List.of(Map.of("grantId",grant,"scopeKind","TARGET","scopeId",owner)));
    assertFalse(auth.permitted(context,"getObject",null,null));
    when(repository.rows("Memberships",org)).thenReturn(List.of());assertFalse(auth.permitted(context,"getObject",target,null));
  }
  @Test void scopeDimensionsIntersectAndTargetsWithinDimensionAreAlternatives() {
    when(repository.rows("GrantScopes",org)).thenReturn(List.of(Map.of("grantId",grant,"scopeKind","TARGET","scopeId",target),Map.of("grantId",grant,"scopeKind","PLACE","scopeId",owner)));
    assertFalse(auth.permitted(context,"getObject",target,"TARGET"));
    assertTrue(auth.permittedScopes(context,"getObject",Map.of("TARGET",List.of(target),"PLACE",List.of(owner))));
    assertFalse(auth.permittedScopes(context,"getObject",Map.of("TARGET",List.of(target),"PLACE",List.of(actor))));
  }
  @Test void rolesCannotCreateIdentityAndAgentCannotDelegate() {
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("fixture").header("alg","RS256").issuer("https://unknown.invalid").subject("reader").claim("organizationId","org-alias").claim("roles",List.of("IDENTITY_ADMIN")).build(),List.of()));
    assertThrows(AccessDeniedException.class,()->auth.context(now,now));
    var guard=new IdentityControlGuard(auth,repository);
    assertThrows(AccessDeniedException.class,()->guard.creatingGrant(context,owner,Set.of("dispatchQuantity"),Set.of(new IdentityControlGuard.Scope("TARGET",target)),now,now.plusSeconds(1)));
    assertThrows(AccessDeniedException.class,()->guard.assigningCapability(context,actor,"getObject",new IdentityControlGuard.Scope("TARGET",target)));
  }
}
