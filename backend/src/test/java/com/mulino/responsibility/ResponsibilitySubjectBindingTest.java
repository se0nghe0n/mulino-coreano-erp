package com.mulino.responsibility;
import com.mulino.application.core.*;
import com.mulino.application.responsibility.*;
import com.mulino.application.work.WorkAccess;
import com.mulino.domain.responsibility.ResponsibilityRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ResponsibilitySubjectBindingTest {
 String id(){return UUID.randomUUID().toString();}
 @Test void proposalBindsExactValidatedWorkAndItemWithOptionalZero(){var repo=mock(ResponsibilityRepository.class);var works=mock(WorkAccess.class);var commands=new ResponsibilityCommands(mock(ResponsibilityService.class),repo,works,null,null,null,null,null);var c=new DomainContext(id(),id(),id(),Instant.now(),Instant.now());String work=id(),item=id();when(works.require(c,work,false)).thenReturn(Map.of("ID",work,"itemId",item));var bindings=commands.subjectBindings(c,Map.of("capabilityId","proposeHandover","slots",Map.of("workId",work)),null);assertEquals(Set.of(work),bindings.get(0).targetIds());assertEquals("Work",bindings.get(0).nounType());assertEquals(Set.of(item),bindings.get(1).targetIds());assertEquals("TradeItem",bindings.get(1).nounType());assertEquals(0,bindings.get(0).minimumCount());assertEquals(0,bindings.get(1).minimumCount());}
 @Test void acceptanceSubjectTargetsComeFromStoredHandoverNotPayloadHints(){var repo=mock(ResponsibilityRepository.class);var works=mock(WorkAccess.class);var commands=new ResponsibilityCommands(mock(ResponsibilityService.class),repo,works,null,null,null,null,null);var c=new DomainContext(id(),id(),id(),Instant.now(),Instant.now());String handover=id(),source=id(),target=id(),item=id(),fake=id();when(repo.require("Handovers",c.organizationId(),handover)).thenReturn(Map.of("ID",handover,"workId",source,"targetWorkId",target));when(works.require(c,source,false)).thenReturn(Map.of("itemId",item));when(works.require(c,target,false)).thenReturn(Map.of("itemId",item));var bindings=commands.subjectBindings(c,Map.of("capabilityId","acceptHandover","slots",Map.of("handoverId",handover,"workId",fake,"targetWorkId",fake)),null);assertEquals(Set.of(source,target),bindings.get(0).targetIds());assertFalse(bindings.stream().anyMatch(x->x.targetIds().contains(fake)));assertEquals(Set.of(handover),bindings.getLast().targetIds());}
 @Test void transferCannotDeclareAssignmentBelongingToAnotherWork(){var repo=mock(ResponsibilityRepository.class);var commands=new ResponsibilityCommands(mock(ResponsibilityService.class),repo,mock(WorkAccess.class),null,null,null,null,null);var c=new DomainContext(id(),id(),id(),Instant.now(),Instant.now());String assignment=id(),source=id();when(repo.require("Assignments",c.organizationId(),assignment)).thenReturn(Map.of("ID",assignment,"workId",id()));assertThrows(DomainError.class,()->commands.subjectBindings(c,Map.of("capabilityId","transferObligation","slots",Map.of("workId",source,"assignmentId",assignment,"targetWorkId",id())),null));}
}
