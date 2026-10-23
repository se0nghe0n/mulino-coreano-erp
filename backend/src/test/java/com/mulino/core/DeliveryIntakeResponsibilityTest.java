package com.mulino.application.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mulino.application.responsibility.*;
import com.mulino.application.work.WorkAccess;
import com.mulino.application.identity.IdentityAuthorization;
import com.mulino.domain.identity.IdentityRepository;
import com.mulino.domain.responsibility.ResponsibilityRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeliveryIntakeResponsibilityTest {
 final ResponsibilityRepository repository=mock(ResponsibilityRepository.class);final WorkAccess works=mock(WorkAccess.class);
 final IdentityRepository identities=mock(IdentityRepository.class);final IdentityAuthorization authority=mock(IdentityAuthorization.class);
 final Instant now=Instant.parse("2026-10-07T02:00:00Z");final DomainContext c=new DomainContext("org","receiver","stable",now,now);
 final ResponsibilityService service=new ResponsibilityService(repository,works,identities,authority,mock(ResponsibilityEvidence.class),new ExecutionClock(Clock.fixed(now,ZoneOffset.UTC)));
 Map<String,Object> command,parameters,observation,event;String previous;
 @BeforeEach void seed()throws Exception{
  TransactionSynchronizationManager.setActualTransactionActive(true);previous=CommandExecution.enter("current");
  var intent=Map.<String,Object>of("intentKind","RECORD","capabilityId","recordDelivery","slots",Map.of("workId","work","eventId","event","observationId","report"));
  command=new LinkedHashMap<>(Map.of("ID","current","organizationId","org","actorId","receiver","stableRequestOwner","stable","state","IN_PROGRESS","effectClass","RECORD","capabilityId","recordDelivery","canonicalHash",CommandRequests.hash(intent),"canonicalIntentJson",new ObjectMapper().writeValueAsString(intent)));
  parameters=new LinkedHashMap<>(Map.of("workId","work","sourceId","current","sourceKind","DECISION","sourceVersion",CommandRequests.hash(intent),"kind","DELIVERY_RECONCILIATION","scopeJson","{\"originalWorkId\":\"work\",\"domainSourceId\":\"report\",\"physicalScopeId\":\"report\"}","quantity",new BigDecimal("40"),"unit","BOX","nextAction","대조한다","nextCheckAt",now.plusSeconds(100)));
  observation=new LinkedHashMap<>(Map.of("ID","report","workId","work","itemId","item","eventId","event","state","PROVISIONAL","quantity",new BigDecimal("40"),"unit","BOX","nextAction","대조한다","nextCheckAt",now.plusSeconds(100)));
  event=new LinkedHashMap<>(Map.of("ID","event","kind","PHYSICAL_DELIVERY","sourceProfileId","profile","sourceNamespace","source","workId","work","itemId","item"));
  when(repository.requireSource("org","DECISION","current")).thenReturn(command);when(repository.deliveryObservation("org","report")).thenReturn(observation);when(repository.requireSource("org","EVENT","event")).thenReturn(event);
  when(repository.evidenceRows("Profiles","org")).thenReturn(List.of(Map.of("ID","profile","namespace","source","intakeOwnerId","intake","supervisorId","supervisor")));
  when(works.require(c,"work",true)).thenReturn(Map.of("ID","work","status","ACTIVE","ownerId","sales","supervisorId","sales","itemId","item"));
  for(String actor:List.of("sales","intake","supervisor"))when(identities.actor("org",actor)).thenReturn(Optional.of(Map.of("kind","HUMAN")));
  when(identities.rows("Memberships","org")).thenReturn(List.of("sales","intake","supervisor").stream().map(actor->Map.<String,Object>of("actorId",actor,"validFrom",now.minusSeconds(1),"validUntil",now.plusSeconds(1000))).toList());
 }
 @AfterEach void clear(){CommandExecution.exit(previous);TransactionSynchronizationManager.setActualTransactionActive(false);}
 @Test void actualSourceIntakeOwnsOnlyTheNewDutyWithOriginalReportedForty(){
  var result=service.openDuty(c,parameters);assertEquals("intake",result.get("ownerId"));assertEquals("supervisor",result.get("supervisorId"));assertEquals("work",result.get("workId"));assertEquals(new BigDecimal("40"),result.get("quantity"));assertEquals("BOX",result.get("unit"));
  verify(authority).fence(c,Set.of("intake","supervisor"));verify(works,never()).replaceOwner(any(),anyString(),anyString(),anyString());verify(repository,never()).update(anyString(),anyString(),anyString(),anyMap());
  var root=ArgumentCaptor.forClass(Map.class);verify(repository).insert(eq("Roots"),root.capture());assertEquals("current",root.getValue().get("sourceId"));assertEquals("DECISION",root.getValue().get("sourceKind"));
 }
 @Test void replayKeepsAcceptedOwnershipAndNeverReassignsTheExistingRoot(){service.openDuty(c,parameters);var root=ArgumentCaptor.forClass(Map.class);verify(repository).insert(eq("Roots"),root.capture());when(repository.rows("Roots","org")).thenReturn(List.of(root.getValue()));when(repository.rows("Assignments","org")).thenReturn(List.of(Map.of("rootId",root.getValue().get("ID"),"valid",true,"ownerId","accepted-recipient","status","OPEN")));clearInvocations(repository,authority);var replay=service.openDuty(c,parameters);assertEquals("accepted-recipient",((List<Map<String,Object>>)replay.get("assignments")).getFirst().get("ownerId"));verifyNoInteractions(authority);verify(repository,never()).insert(anyString(),anyMap());verify(repository,never()).deliveryObservation(anyString(),anyString());}
 @ParameterizedTest @ValueSource(strings={"state","effectClass","actorId","stableRequestOwner","organizationId","canonicalHash"})
 void anotherCommandCannotSelectAnIntakeOwner(String field){command.put(field,"other");assertThrows(DomainError.class,()->service.openDuty(c,parameters));verify(repository,never()).insert(anyString(),anyMap());}
 @ParameterizedTest @ValueSource(strings={"workId","itemId","sourceNamespace"})
 void theExactEventAndProfileBindingIsRequired(String field){event.put(field,"other");assertThrows(DomainError.class,()->service.openDuty(c,parameters));verify(repository,never()).insert(anyString(),anyMap());}
 @Test void reportedQuantityMismatchHasNoDutyEffects(){parameters.put("quantity",new BigDecimal("39"));assertThrows(DomainError.class,()->service.openDuty(c,parameters));verify(repository,never()).insert(anyString(),anyMap());}
 @Test void inactiveIntakeCannotReceiveResponsibility(){when(identities.actor("org","intake")).thenReturn(Optional.empty());assertThrows(DomainError.class,()->service.openDuty(c,parameters));verify(repository,never()).insert(anyString(),anyMap());}
}
