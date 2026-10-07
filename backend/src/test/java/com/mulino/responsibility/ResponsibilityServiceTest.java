package com.mulino.responsibility;
import com.mulino.application.responsibility.*;
import com.mulino.application.work.*;
import com.mulino.application.core.*;
import com.mulino.application.identity.IdentityAuthorization;
import com.mulino.domain.identity.IdentityRepository;
import com.mulino.domain.responsibility.ResponsibilityRepository;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class ResponsibilityServiceTest {
 final ResponsibilityRepository repo=mock(ResponsibilityRepository.class);final WorkAccess works=mock(WorkAccess.class);
 final IdentityRepository ids=mock(IdentityRepository.class);final IdentityAuthorization auth=mock(IdentityAuthorization.class);
 final ResponsibilityEvidence evidence=mock(ResponsibilityEvidence.class);final Instant now=Instant.parse("2026-10-08T00:00:00Z");
 final DomainContext c=new DomainContext("org","owner","stable",now,now);
 final ResponsibilityService service=new ResponsibilityService(repo,works,ids,auth,evidence,new ExecutionClock(Clock.fixed(now,ZoneOffset.UTC)));
 @Test void c5CannotCloseWorkWithOpenDutyAndReadHasNoEffects(){when(repo.rows("Assignments","org")).thenReturn(List.of(Map.of("workId","work","status","OPEN","valid",true)));var e=assertThrows(DomainError.class,()->service.requireSettled(c,"work"));assertEquals("UNRESOLVED_DUTY",e.code());verify(repo,never()).update(any(),any(),any(),any());}
 @Test void transferDoesNotResolveGoalOrCloseSource(){when(repo.rows("Assignments","org")).thenReturn(List.of(Map.of("workId","work","status","TRANSFERRED","valid",false)));service.requireSettled(c,"work");verify(works,never()).markInvalidation(any(),any(),anyBoolean());verify(works,never()).currentGoal(any(),any());}
 @Test void failedResolutionEvidenceKeepsDutyOpen(){var a=Map.<String,Object>of("ID","assignment","rootId","root","scopeId","scope","kind","QC","status","OPEN","valid",true);when(repo.require("Assignments","org","assignment")).thenReturn(a);doThrow(DomainError.unsupported()).when(evidence).requireResolution(c,"QC","root","scope","evidence");assertThrows(DomainError.class,()->service.settle(c,Map.of("assignmentId","assignment","evidenceId","evidence"),false));verify(repo,never()).update(any(),any(),any(),any());}
 @Test void rejectPreservesOldOwnerAndAssignments(){var h=new LinkedHashMap<String,Object>();h.putAll(Map.of("ID","h","workId","work","recipientId","owner","previousOwnerId","previous","status","PROPOSED","expiresAt",now.plusSeconds(20),"revision",0));when(repo.require("Handovers","org","h")).thenReturn(h);when(works.require(c,"work",true)).thenReturn(Map.of("ownerId","previous"));service.decide(c,"h","REJECTED");verify(works,never()).replaceOwner(any(),any(),any(),any());verify(repo,never()).update(eq("Assignments"),any(),any(),any());}
 @Test void expiredAcceptanceCannotMoveResponsibility(){var h=Map.<String,Object>of("ID","h","workId","work","recipientId","owner","previousOwnerId","previous","status","PROPOSED","expiresAt",now,"revision",0);when(repo.require("Handovers","org","h")).thenReturn(h);assertThrows(DomainError.class,()->service.decide(c,"h","ACCEPTED"));verify(works,never()).replaceOwner(any(),any(),any(),any());}
 @Test void unverifiedCorrectionRetainsSourceProfileResponsibilityWithoutCanonicalFacts(){
  var source=Map.<String,Object>of("ID","event","sourceProfileId","profile","revision",2,"itemId","item");when(repo.requireSource("org","EVENT","event")).thenReturn(source);
  when(repo.evidenceRows("Profiles","org")).thenReturn(List.of(Map.of("ID","profile","intakeOwnerId","intake","supervisorId","intake","nextAction","inspect raw source","nextCheckAt",now.plusSeconds(100))));
  for(String actor:List.of("intake","workOwner"))when(ids.actor("org",actor)).thenReturn(Optional.of(Map.of("kind","HUMAN")));
  when(ids.rows("Memberships","org")).thenReturn(List.of(Map.of("actorId","intake","validFrom",now.minusSeconds(1),"validUntil",now.plusSeconds(1000)),Map.of("actorId","workOwner","validFrom",now.minusSeconds(1),"validUntil",now.plusSeconds(1000))));
  when(works.require(c,"work",true)).thenReturn(Map.of("ID","work","status","ACTIVE","ownerId","workOwner","supervisorId","workOwner","itemId","item"));when(repo.rows("Roots","org")).thenReturn(List.of());when(repo.require(eq("Assignments"),eq("org"),anyString())).thenReturn(Map.of("ownerId","intake","supervisorId","intake","status","OPEN"));
  var result=service.ensureEvidenceCorrectionDuty(c,"work","event","EVENT","inspect raw source",now.plusSeconds(100));assertEquals("intake",result.get("ownerId"));
  verify(repo).update(eq("Assignments"),eq("org"),anyString(),eq(Map.of("ownerId","intake","supervisorId","intake")));verify(works,never()).markInvalidation(any(),any(),anyBoolean());
 }
 @Test void rawCorrectionCannotReplaceAuthoritativeIntakeDeadline(){when(repo.requireSource("org","DOCUMENT","document")).thenReturn(Map.of("sourceProfileId","profile"));when(repo.evidenceRows("Profiles","org")).thenReturn(List.of(Map.of("ID","profile","intakeOwnerId","intake","supervisorId","intake","nextAction","review","nextCheckAt",now.plusSeconds(100))));when(ids.actor("org","intake")).thenReturn(Optional.of(Map.of("kind","HUMAN")));when(ids.rows("Memberships","org")).thenReturn(List.of(Map.of("actorId","intake","validFrom",now.minusSeconds(1),"validUntil",now.plusSeconds(1000))));assertThrows(DomainError.class,()->service.ensureEvidenceCorrectionDuty(c,"work","document","DOCUMENT","review",now.plusSeconds(999)));verify(repo,never()).insert(any(),any());}

}
