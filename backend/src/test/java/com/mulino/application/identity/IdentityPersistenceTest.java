package com.mulino.application.identity;

import static org.junit.jupiter.api.Assertions.*;
import com.mulino.domain.identity.IdentityRepository;
import com.mulino.application.core.DomainContext;
import com.sap.cds.services.runtime.CdsRuntime;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("local")
class IdentityPersistenceTest {
  static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280");
  static { PG.start(); }
  static final String ORG="00000000-0000-0000-0000-000000000101",ACTOR="00000000-0000-0000-0000-000000000102",OWNER="00000000-0000-0000-0000-000000000103",TARGET="00000000-0000-0000-0000-000000000104",GRANT="00000000-0000-0000-0000-000000000105",OTHER="00000000-0000-0000-0000-000000000106";
  @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url",PG::getJdbcUrl);r.add("spring.datasource.username",PG::getUsername);r.add("spring.datasource.password",PG::getPassword);
    r.add("JWT_PUBLIC_KEY",()->System.getenv("JWT_PUBLIC_KEY"));r.add("JWT_ISSUER",()->"https://fixture.invalid");r.add("JWT_AUDIENCE",()->"mulino-acceptance");
  }
  @Autowired JdbcTemplate jdbc; @Autowired IdentityAuthorization auth; @Autowired IdentityRepository repository; @Autowired CdsRuntime runtime;
  @BeforeEach void seed() {
    for(String table:List.of("GrantScopes","GrantActions","Grants","CapabilityAssignments","Memberships","ExternalIdentities","AuthorityFences","Actors","Organizations")) jdbc.execute("DELETE FROM mulino_identity_"+table);
    jdbc.update("INSERT INTO mulino_identity_Organizations(ID,externalAlias) VALUES(?, 'ORG-A'),(?, 'ORG-B')",ORG,OTHER);
    jdbc.update("INSERT INTO mulino_identity_Actors(organizationId,ID,kind,stableRequestOwner) VALUES(?,?,'AGENT',?)",ORG,ACTOR,OWNER);
    jdbc.update("INSERT INTO mulino_identity_ExternalIdentities(organizationId,ID,actorId,issuer,subject,organizationAlias) VALUES(?, ?, ?, 'https://fixture.invalid','writer-a','ORG-A')",ORG,UUID.randomUUID().toString(),ACTOR);
    jdbc.update("INSERT INTO mulino_identity_Memberships(organizationId,ID,actorId,validFrom,validUntil) VALUES(?,?,?,CURRENT_TIMESTAMP-INTERVAL '1 day',CURRENT_TIMESTAMP+INTERVAL '1 day')",ORG,UUID.randomUUID().toString(),ACTOR);
    for(String capability:List.of("getObject","dispatchQuantity")) jdbc.update("INSERT INTO mulino_identity_CapabilityAssignments(organizationId,ID,actorId,capabilityId,scopeKind,scopeId,validFrom,validUntil) VALUES(?,?,?,?,'TARGET',?,CURRENT_TIMESTAMP-INTERVAL '1 day',CURRENT_TIMESTAMP+INTERVAL '1 day')",ORG,UUID.randomUUID().toString(),ACTOR,capability,TARGET);
    jdbc.update("INSERT INTO mulino_identity_Grants(organizationId,ID,actorId,delegatorId,validFrom,validUntil) VALUES(?,?,?,?,CURRENT_TIMESTAMP-INTERVAL '1 day',CURRENT_TIMESTAMP+INTERVAL '1 day')",ORG,GRANT,ACTOR,ACTOR);
    jdbc.update("INSERT INTO mulino_identity_GrantActions VALUES(?,?,'getObject')",ORG,GRANT);
    jdbc.update("INSERT INTO mulino_identity_GrantScopes VALUES(?,?,'TARGET',?)",ORG,GRANT,TARGET);
    jdbc.update("INSERT INTO mulino_identity_AuthorityFences VALUES(?,?,1)",ORG,ACTOR);
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("local-unit-context").header("alg","RS256").issuer("https://fixture.invalid").subject("writer-a").claim("organizationId","ORG-A").claim("stableRequestOwner","untrusted").build(),List.of()));
  }
  @AfterEach void clear() { SecurityContextHolder.clearContext(); }
  @Test void cqnMappingAndCurrentReadGrantDeniesWriterCapability() {
    runtime.requestContext().run(ctx -> {
      DomainContext c=auth.context(Instant.now(),Instant.now());assertEquals(ACTOR,c.actorId());assertEquals(ORG,c.organizationId());assertEquals(OWNER,c.stableRequestOwner());
      auth.authorize(c,"getObject",TARGET);assertTrue(auth.permitted(c,"getObject",null,null));assertFalse(auth.permitted(c,"getObject",OTHER,null));
      assertThrows(AccessDeniedException.class,()->auth.authorize(c,"dispatchQuantity",TARGET));
      jdbc.update("UPDATE mulino_identity_Grants SET revokedAt=CURRENT_TIMESTAMP,revision=revision+1 WHERE organizationId=? AND ID=?",ORG,GRANT);
      assertThrows(AccessDeniedException.class,()->auth.authorize(c,"getObject",TARGET));
    });
  }
  @Test void databaseCompositeFkRejectsOtherOrganizationActorAndCqnNoLeak() {
    assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("INSERT INTO mulino_identity_Memberships(organizationId,ID,actorId,validFrom,validUntil) VALUES(?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP+INTERVAL '1 day')",OTHER,UUID.randomUUID().toString(),ACTOR));
    runtime.requestContext().run(ctx -> { assertTrue(repository.actor(OTHER,ACTOR).isEmpty()); });
  }
}
