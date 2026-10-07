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
}
