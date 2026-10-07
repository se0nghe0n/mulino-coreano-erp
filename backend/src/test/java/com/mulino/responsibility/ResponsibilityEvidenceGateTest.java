package com.mulino.responsibility;
import com.mulino.application.core.*;
import com.mulino.application.responsibility.*;
import com.mulino.domain.responsibility.ResponsibilityRepository;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ResponsibilityEvidenceGateTest {
 final ResponsibilityRepository repo=mock(ResponsibilityRepository.class);final Instant now=Instant.parse("2026-10-08T00:00:00Z");final DomainContext c=new DomainContext("org","actor","stable",now,now);final ResponsibilityEvidenceGate gate=new ResponsibilityEvidenceGate(repo,new ExecutionClock(Clock.fixed(now,ZoneOffset.UTC)),mock(org.springframework.beans.factory.ObjectProvider.class));
 Map<String,Object> event;
 @BeforeEach void seed(){event=new LinkedHashMap<>(Map.of("ID","evidence","kind","RESPONSE_COMPLETED","valueState","KNOWN","reassessmentState","COMPLETE","physicalScopeId","physical","workId","work","quantity",new BigDecimal("10"),"unit","EA"));when(repo.evidenceRows("Occurrences","org")).thenReturn(List.of(event));when(repo.require("Scopes","org","scope")).thenReturn(Map.of("scopeJson","{\"physicalScopeId\":\"physical\"}"));when(repo.rows("Assignments","org")).thenReturn(List.of(Map.of("scopeId","scope","status","OPEN","valid",true,"workId","work","quantity",new BigDecimal("10"),"unit","EA")));when(repo.evidenceRows("Verifications","org")).thenReturn(List.of(Map.of("canonicalOccurrenceId","evidence","verdict","VERIFIED","sourceMatched",true,"identityMatched",true,"quantityMatched",true,"timeMatched",true,"duplicateChecked",true,"basisDocumentId","document")));}
 @Test void verifiedActualResponseCanResolveCoreReview(){assertDoesNotThrow(()->gate.requireResolution(c,"FOLLOWUP_REVIEW","root","scope","evidence"));}
 @Test void documentOrWrongActionCannotResolveDuty(){event.put("kind","NOTIFICATION_SENT");assertThrows(DomainError.class,()->gate.requireResolution(c,"FOLLOWUP_REVIEW","root","scope","evidence"));}
 @Test void insufficientQuantityCannotResolveDuty(){event.put("quantity",new BigDecimal("9"));assertThrows(DomainError.class,()->gate.requireResolution(c,"FOLLOWUP_REVIEW","root","scope","evidence"));}
 @Test void waiverRequiresMatchingKindApprovedDecision(){when(repo.evidenceRows("Approvals","org")).thenReturn(List.of(Map.of("ID","approval","decision","APPROVED","action","WAIVE_FOLLOWUP_REVIEW","targetId","assignment","expiresAt",now.plusSeconds(100))));when(repo.require("Assignments","org","assignment")).thenReturn(Map.of("rootId","root","status","OPEN"));assertDoesNotThrow(()->gate.requireWaiver(c,"FOLLOWUP_REVIEW","root","approval","approved reason"));assertThrows(DomainError.class,()->gate.requireWaiver(c,"QC","root","approval","approved reason"));}
}
