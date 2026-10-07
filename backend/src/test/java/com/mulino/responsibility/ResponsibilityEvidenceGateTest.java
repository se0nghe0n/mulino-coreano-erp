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
 final ResponsibilityRepository repo=mock(ResponsibilityRepository.class);final Instant now=Instant.parse("2026-10-08T00:00:00Z");final DomainContext c=new DomainContext("org","actor","stable",now,now);final org.springframework.beans.factory.ObjectProvider<ResponsibilityCompletionEvidence> completionProvider=mock(org.springframework.beans.factory.ObjectProvider.class);final ResponsibilityCompletionEvidence completion=mock(ResponsibilityCompletionEvidence.class);final ResponsibilityEvidenceGate gate=new ResponsibilityEvidenceGate(repo,new ExecutionClock(Clock.fixed(now,ZoneOffset.UTC)),mock(org.springframework.beans.factory.ObjectProvider.class),completionProvider);
 Map<String,Object> event;
 @BeforeEach void seed(){event=new LinkedHashMap<>(Map.of("ID","evidence","kind","RESPONSE_COMPLETED","valueState","KNOWN","reassessmentState","COMPLETE","physicalScopeId","physical","workId","work","quantity",new BigDecimal("10"),"unit","EA"));when(repo.evidenceRows("Occurrences","org")).thenReturn(List.of(event));when(repo.require("Roots","org","root")).thenReturn(Map.of("ID","root","quantity",new BigDecimal("10"),"unit","EA"));when(repo.require("Scopes","org","scope")).thenReturn(Map.of("ID","scope","rootId","root","leaf",true,"startQuantity",BigDecimal.ZERO,"quantity",new BigDecimal("10"),"scopeJson","{\"physicalScopeId\":\"physical\"}"));when(completionProvider.getIfAvailable()).thenReturn(completion);when(completion.requireCoverage(c,"evidence")).thenReturn(new ResponsibilityCompletionEvidence.Coverage("root",BigDecimal.ZERO,new BigDecimal("10"),"EA","verification","coverage"));when(repo.rows("Assignments","org")).thenReturn(List.of(Map.of("ID","assignment","scopeId","scope","status","OPEN","valid",true,"workId","work","quantity",new BigDecimal("10"),"unit","EA")));when(repo.evidenceRows("Verifications","org")).thenReturn(List.of(Map.of("ID","verification","canonicalOccurrenceId","evidence","verdict","VERIFIED","sourceMatched",true,"identityMatched",true,"quantityMatched",true,"timeMatched",true,"duplicateChecked",true,"basisDocumentId","document")));}
 @Test void verifiedActualResponseCanResolveCoreReview(){assertDoesNotThrow(()->gate.requireResolution(c,"FOLLOWUP_REVIEW","root","scope","evidence"));}
 @Test void documentOrWrongActionCannotResolveDuty(){event.put("kind","NOTIFICATION_SENT");assertThrows(DomainError.class,()->gate.requireResolution(c,"FOLLOWUP_REVIEW","root","scope","evidence"));}
 @Test void insufficientQuantityCannotResolveDuty(){event.put("quantity",new BigDecimal("9"));assertThrows(DomainError.class,()->gate.requireResolution(c,"FOLLOWUP_REVIEW","root","scope","evidence"));}
 @Test void waiverRequiresMatchingKindApprovedDecision(){when(repo.evidenceRows("Approvals","org")).thenReturn(List.of(Map.of("ID","approval","decision","APPROVED","action","WAIVE_FOLLOWUP_REVIEW","targetId","assignment","expiresAt",now.plusSeconds(100))));when(repo.require("Assignments","org","assignment")).thenReturn(Map.of("rootId","root","status","OPEN"));assertDoesNotThrow(()->gate.requireWaiver(c,"FOLLOWUP_REVIEW","root","approval","approved reason"));assertThrows(DomainError.class,()->gate.requireWaiver(c,"QC","root","approval","approved reason"));}
 @Test void partialFiftyCompletionCannotDischargeOtherFiftyLeaf(){
  event.put("quantity",new BigDecimal("50"));when(repo.require("Roots","org","root")).thenReturn(Map.of("ID","root","quantity",new BigDecimal("100"),"unit","EA"));when(completion.requireCoverage(c,"evidence")).thenReturn(new ResponsibilityCompletionEvidence.Coverage("root",BigDecimal.ZERO,new BigDecimal("50"),"EA","verification","coverage"));
  when(repo.require("Scopes","org","scope")).thenReturn(leaf("scope",0,50));when(repo.rows("Assignments","org")).thenReturn(List.of(current("assignment","scope",50)));assertDoesNotThrow(()->gate.requireResolution(c,"FOLLOWUP_REVIEW","root","scope","evidence"));
  when(repo.require("Scopes","org","other")).thenReturn(leaf("other",50,50));when(repo.rows("Assignments","org")).thenReturn(List.of(current("otherAssignment","other",50)));assertThrows(DomainError.class,()->gate.requireResolution(c,"FOLLOWUP_REVIEW","root","other","evidence"));
  verify(repo,times(1)).insert(eq("ResolutionCredits"),any());
 }
 @Test void fullHundredCompletionCreditsTwoDisjointFiftyLeavesExactlyOnce(){
  event.put("quantity",new BigDecimal("100"));when(repo.require("Roots","org","root")).thenReturn(Map.of("ID","root","quantity",new BigDecimal("100"),"unit","EA"));when(completion.requireCoverage(c,"evidence")).thenReturn(new ResponsibilityCompletionEvidence.Coverage("root",BigDecimal.ZERO,new BigDecimal("100"),"EA","verification","coverage"));
  var bindings=new ArrayList<Map<String,Object>>();var credits=new ArrayList<Map<String,Object>>();when(repo.rows("CompletionBindings","org")).thenAnswer(x->bindings);when(repo.rows("ResolutionCredits","org")).thenAnswer(x->credits);doAnswer(x->{String kind=x.getArgument(0);Map<String,Object> row=x.getArgument(1);if(kind.equals("CompletionBindings"))bindings.add(row);if(kind.equals("ResolutionCredits"))credits.add(row);return null;}).when(repo).insert(anyString(),any());
  when(repo.require("Scopes","org","scope")).thenReturn(leaf("scope",0,50));when(repo.rows("Assignments","org")).thenReturn(List.of(current("assignment","scope",50)));gate.requireResolution(c,"FOLLOWUP_REVIEW","root","scope","evidence");
  when(repo.require("Scopes","org","other")).thenReturn(leaf("other",50,50));when(repo.rows("Assignments","org")).thenReturn(List.of(current("otherAssignment","other",50)));gate.requireResolution(c,"FOLLOWUP_REVIEW","root","other","evidence");
  assertEquals(2,credits.size());assertEquals(0,new BigDecimal("100").compareTo(credits.stream().map(x->(BigDecimal)x.get("quantity")).reduce(BigDecimal.ZERO,BigDecimal::add)));assertThrows(DomainError.class,()->gate.requireResolution(c,"FOLLOWUP_REVIEW","root","other","evidence"));
 }
 Map<String,Object> leaf(String id,int start,int quantity){return Map.of("ID",id,"rootId","root","leaf",true,"startQuantity",new BigDecimal(start),"quantity",new BigDecimal(quantity),"scopeJson","{\"physicalScopeId\":\"physical\"}");}
 Map<String,Object> current(String id,String scope,int quantity){return Map.of("ID",id,"scopeId",scope,"status","OPEN","valid",true,"workId","work","quantity",new BigDecimal(quantity),"unit","EA");}

}
