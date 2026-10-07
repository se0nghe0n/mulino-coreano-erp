package com.mulino.application.identity;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.mulino.application.core.DomainContext;
import com.mulino.domain.identity.IdentityRepository;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class AuthorityEvidenceTest {
 @Test void evidenceComesFromSameScopedCurrentAuthorityEvaluation(){
  var repository=mock(IdentityRepository.class);Instant now=Instant.parse("2026-10-08T00:00:00Z");
  var auth=new IdentityAuthorization(repository,Clock.fixed(now,ZoneOffset.UTC));var context=new DomainContext("org","agent","owner",now.minusSeconds(100),now.minusSeconds(100));
  when(repository.rows("Memberships","org")).thenReturn(List.of(row("m-agent","agent",now),row("m-human","human",now)));
  var aa=row("a-agent","agent",now);aa.putAll(Map.of("capabilityId","moveQuantity","scopeKind","TARGET","scopeId","target","revision",2));
  var ah=row("a-human","human",now);ah.putAll(Map.of("capabilityId","moveQuantity","scopeKind","TARGET","scopeId","target","revision",3));
  when(repository.rows("CapabilityAssignments","org")).thenReturn(List.of(aa,ah));
  var agent=row("g-agent","agent",now);agent.put("delegatorId","human");var human=row("g-human","human",now);human.put("delegatorId","human");
  when(repository.rows("Grants","org")).thenReturn(List.of(agent,human));
  when(repository.rows("GrantActions","org")).thenReturn(List.of(Map.of("grantId","g-agent","capabilityId","moveQuantity"),Map.of("grantId","g-human","capabilityId","moveQuantity")));
  when(repository.rows("GrantScopes","org")).thenReturn(List.of(Map.of("grantId","g-agent","scopeKind","TARGET","scopeId","target"),Map.of("grantId","g-human","scopeKind","TARGET","scopeId","target")));
  when(repository.actor("org","human")).thenReturn(Optional.of(Map.of("stableRequestOwner","server-owner")));
  var proof=auth.authorityEvidence(context,"moveQuantity",Map.of("TARGET",List.of("target")));
  assertEquals(List.of("g-agent","g-human"),proof.stream().map(x->x.get("grantId")).toList());assertEquals("human",proof.getFirst().get("delegatorId"));assertEquals(2,proof.getFirst().get("assignmentRevision"));
  assertThrows(org.springframework.security.access.AccessDeniedException.class,()->auth.authorityEvidence(context,"moveQuantity",Map.of("TARGET",List.of("forged"))));
 }
 private static Map<String,Object> row(String id,String actor,Instant now){return new HashMap<>(Map.of("ID",id,"actorId",actor,"revision",1,"validFrom",now.minusSeconds(60),"validUntil",now.plusSeconds(60)));}
}
