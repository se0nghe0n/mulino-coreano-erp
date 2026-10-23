package com.mulino.core;

import com.mulino.application.core.*;
import com.mulino.application.work.WorkFollowupSource;
import com.mulino.domain.governance.PolicyRepository;
import com.mulino.domain.responsibility.ResponsibilityRepository;
import com.mulino.domain.definitions.DefinitionRepository;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkFollowupSourceTest {
 final Instant now=Instant.parse("2026-10-07T02:00:00Z");
 final DomainContext context=new DomainContext("org","actor","actor",now,now);
 final ResponsibilityRepository sources=mock(ResponsibilityRepository.class);
 final PolicyRepository policies=mock(PolicyRepository.class);
 final WorkFollowupSource service=new WorkFollowupSource(sources,policies,new ExecutionClock(Clock.fixed(now,ZoneOffset.UTC)));
 Map<String,Object> event;
 @BeforeEach void seed(){
  event=new HashMap<>(Map.of("ID","event","kind","TEMPERATURE_OBSERVATION","subjectKind","SEGMENT","subjectId","segment","itemId","item","valueState","KNOWN","recordedAt",now,"effectiveFrom",now.minusSeconds(10),"payload","{\"segmentId\":\"segment\",\"temperature\":{\"unit\":\"CELSIUS\",\"value\":\"12\"}}"));
  when(sources.requireSource("org","EVENT","event")).thenReturn(event);when(sources.evidenceRows("EVENT","org")).thenReturn(List.of(event));
  String content="{\"temperatureResponse\":\"outside_2_8_C\",\"synthetic\":true}";
  when(policies.current("org","EVIDENCE",now)).thenReturn(List.of(Map.of("content",content,"contentHash",DefinitionRepository.sha256(content))));
 }
 @Test void anUnverifiedScopedObservationCanWarrantReviewWithoutFactPromotion(){assertSame(event,service.require(context,"event","item"));verify(sources,never()).insert(anyString(),anyMap());}
 @ParameterizedTest @ValueSource(strings={"2","5","8"}) void normalObservationsNeverCreateAResponse(String temperature){event.put("payload","{\"segmentId\":\"segment\",\"temperature\":{\"unit\":\"CELSIUS\",\"value\":\""+temperature+"\"}}");assertThrows(DomainError.class,()->service.require(context,"event","item"));verify(sources,never()).insert(anyString(),anyMap());}
 @Test void unknownScopeAndSourceCannotBorrowTheParentItem(){event.put("itemId","other-item");assertThrows(DomainError.class,()->service.require(context,"event","item"));}
 @Test void aSupersededRawObservationRequiresReconciliation(){when(sources.evidenceRows("EVENT","org")).thenReturn(List.of(event,Map.of("supersedesId","event")));var failure=assertThrows(DomainError.class,()->service.require(context,"event","item"));assertEquals("EVIDENCE_CONFLICT",failure.code());}
 @Test void policyHashMismatchCannotAuthorizeResponse(){when(policies.current("org","EVIDENCE",now)).thenReturn(List.of(Map.of("content","{}","contentHash","wrong")));var failure=assertThrows(DomainError.class,()->service.require(context,"event","item"));assertEquals("POLICY_UNRESOLVED",failure.code());}
 @Test void futureOrUnknownObservationsAreNotCurrentResponseProof(){event.put("effectiveFrom",now.plusSeconds(1));assertThrows(DomainError.class,()->service.require(context,"event","item"));event.put("effectiveFrom",now);event.put("valueState","UNKNOWN");assertThrows(DomainError.class,()->service.require(context,"event","item"));}
}
