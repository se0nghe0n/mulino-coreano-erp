package com.mulino.application.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mulino.application.evaluation.CausalAssessmentGuard;
import com.mulino.application.work.WorkAccess;
import com.mulino.domain.evaluation.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CausalAssessmentGuardTest {
 final AssessmentRepository repository=mock(AssessmentRepository.class);final WorkAccess works=mock(WorkAccess.class);final ReadAuthorizer authority=mock(ReadAuthorizer.class);
 final CausalAssessmentGuard guard=new CausalAssessmentGuard(repository,works,authority);
 final DomainContext context=new DomainContext("org","actor","stable",Instant.parse("2026-10-07T02:00:00Z"),Instant.parse("2026-10-07T02:00:00Z"));
 Map<String,Object> command;Map<String,Object> intent;Map<String,Object> work,goal;String previous;
 @BeforeEach void command()throws Exception{
  TransactionSynchronizationManager.setActualTransactionActive(true);previous=CommandExecution.enter("current");
  intent=new LinkedHashMap<>(Map.of("intentKind","RECORD","capabilityId","recordDelivery","slots",Map.of("observationId","observation","canonicalOccurrenceId","canonical")));
  command=new LinkedHashMap<>(Map.of("ID","current","organizationId","org","actorId","actor","stableRequestOwner","stable","state","IN_PROGRESS","effectClass","RECORD","capabilityId","recordDelivery","canonicalIntentJson",new ObjectMapper().writeValueAsString(intent),"canonicalHash",CommandRequests.hash(intent),"authorizationScopeJson","{\"WORK\":[\"work\"],\"ITEM\":[\"item\"]}"));
  when(repository.currentCommand(context,"current")).thenReturn(command);work=new LinkedHashMap<>(Map.of("itemId","item","status","ACTIVE","kind","DELIVERY","lifecycleMode","COMMAND","currentGoalVersionId","goal","definitionVersionId","definition"));
  goal=new LinkedHashMap<>(Map.of("ID","goal","workId","work","definitionVersionId","definition","endpoint","DELIVERED","slotsJson","{\"endpoint\":\"DELIVERED\",\"eventKind\":\"PHYSICAL_DELIVERY\"}"));
  when(works.require(context,"work",true)).thenReturn(work);when(works.currentGoal(context,"work")).thenReturn(goal);
 }
 @AfterEach void clear(){CommandExecution.exit(previous);TransactionSynchronizationManager.setActualTransactionActive(false);}
 @ParameterizedTest @ValueSource(strings={"ID","organizationId","actorId","stableRequestOwner","state","effectClass","canonicalHash"})
 void commandIdentityStateAndHashAreNotBorrowable(String field){command.put(field,"forged");var denied=assertThrows(DomainError.class,()->guard.purpose(context,"work"));assertEquals("FORBIDDEN",denied.code());verify(repository,never()).save(anyMap(),anyMap());}
 @Test void onlyAnActualTransferInThisCommandCanTriggerTheRecordPurpose(){when(repository.hasCausalDelivery(eq(context),eq("work"),eq("current"),anyMap())).thenReturn(false);var denied=assertThrows(DomainError.class,()->guard.purpose(context,"work"));assertEquals("FORBIDDEN",denied.code());verifyNoInteractions(authority);}
 @Test void anotherAffectedWorkCannotUseTheOriginalGrant(){command.put("authorizationScopeJson","{\"WORK\":[\"other\"],\"ITEM\":[\"item\"]}");assertThrows(DomainError.class,()->guard.purpose(context,"work"));verify(repository,never()).hasCausalDelivery(any(),anyString(),anyString(),anyMap());}
 @Test void closedDeliveryWorkCannotBeReopenedByTheHelper(){when(works.require(context,"work",true)).thenReturn(Map.of("itemId","item","status","CLOSED"));assertThrows(DomainError.class,()->guard.purpose(context,"work"));}
 @Test void currentAllOfAuthorityIsRecheckedBeforeSelectingARecordPurpose(){when(repository.hasCausalDelivery(eq(context),eq("work"),eq("current"),anyMap())).thenReturn(true);doThrow(DomainError.forbidden()).when(authority).authorizeScopes(eq(context),eq("recordDelivery"),anyMap());assertThrows(DomainError.class,()->guard.purpose(context,"work"));verify(authority).authorizeScopes(context,"recordDelivery",Map.of("WORK",List.of("work"),"ITEM",List.of("item")));}
 @Test void ordinaryCreationRetainsDefaultAssessmentPurpose()throws Exception{intent.put("intentKind","COMMAND");intent.put("capabilityId","createWork");command.put("canonicalIntentJson",new ObjectMapper().writeValueAsString(intent));assertEquals(CanonicalFactAdmission.Purpose.ASSESS_GOAL,guard.purpose(context,"work"));verifyNoInteractions(works,authority);}
 @Test void aNonCommandDomainCreationKeepsItsExistingBehavior(){CommandExecution.exit(previous);previous=null;assertEquals(CanonicalFactAdmission.Purpose.ASSESS_GOAL,guard.purpose(context,"work"));verifyNoInteractions(repository,works,authority);}
 @ParameterizedTest @ValueSource(strings={"kind","lifecycleMode"})
 void onlyCommandDeliveryWorksCanUseTheCausalPort(String field){work.put(field,"other");assertThrows(DomainError.class,()->guard.purpose(context,"work"));verify(repository,never()).hasCausalDelivery(any(),anyString(),anyString(),anyMap());}
 @ParameterizedTest @ValueSource(strings={"ID","workId","definitionVersionId","endpoint"})
 void theExactCurrentDeliveredGoalIsRequired(String field){goal.put(field,"other");assertThrows(DomainError.class,()->guard.purpose(context,"work"));verifyNoInteractions(authority);}
 @ParameterizedTest @ValueSource(strings={"{}","{\"endpoint\":\"ARRIVED\",\"eventKind\":\"PHYSICAL_DELIVERY\"}","{\"endpoint\":\"DELIVERED\"}","{\"endpoint\":\"DELIVERED\",\"eventKind\":\"PHYSICAL_RECEIPT\"}"})
 void aMissingOrDifferentPinnedDeliverySelectorCannotBorrowThePort(String slots){goal.put("slotsJson",slots);assertThrows(DomainError.class,()->guard.purpose(context,"work"));verify(repository,never()).hasCausalDelivery(any(),anyString(),anyString(),anyMap());}

}
