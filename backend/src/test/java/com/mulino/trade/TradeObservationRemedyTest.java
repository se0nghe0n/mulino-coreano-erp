package com.mulino.trade;

import com.mulino.application.core.*;
import com.mulino.application.trade.*;
import com.mulino.application.responsibility.ResponsibilityService;
import com.mulino.domain.responsibility.ResponsibilityRepository;
import com.mulino.domain.inventory.InventoryRepository;
import com.mulino.domain.trade.sales.SalesRepository;
import com.mulino.domain.trade.returns.ReturnRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Exercises the private completion producer and real responsibility validator together. */
class TradeObservationRemedyTest {
 final SalesRepository sales=mock(SalesRepository.class);final ReturnRepository returns=mock(ReturnRepository.class);
 final InventoryRepository inventory=mock(InventoryRepository.class);final TradeEvidence evidence=mock(TradeEvidence.class);
 final ResponsibilityRepository duties=mock(ResponsibilityRepository.class);
 final ResponsibilityService service=new ResponsibilityService(duties,null,null,null,null,null);
 final TradeObservationRemedy remedy=new TradeObservationRemedy(sales,returns,inventory,evidence,duties,service);
 final Instant now=Instant.parse("2026-10-07T02:00:00Z");final DomainContext c=new DomainContext("org","receiver","stable",now,now);
 Map<String,Object> root,observation,actual,assignment;
 @BeforeEach void seed(){
  root=new LinkedHashMap<>(Map.of("ID","root","kind","DELIVERY_RECONCILIATION","quantity",new BigDecimal("40.000"),"unit","BOX","scopeJson","{\"originalWorkId\":\"work\",\"domainSourceId\":\"report\",\"physicalScopeId\":\"report\"}"));
  observation=new LinkedHashMap<>(Map.of("ID","report","state","CONFIRMED","workId","work","itemId","item","lotId","lot","rangeRootId","range","unit","BOX","startQuantity",BigDecimal.ZERO,"quantity",new BigDecimal("40"),"occurredAt",now));
  observation.putAll(Map.of("placeId","customer-place","customerId","customer","eventId","event","deliveryId","original-delivery"));
  actual=new LinkedHashMap<>(observation);actual.putAll(Map.of("ID","actual","observationId","report","canonicalOccurrenceId","canonical","segmentId","segment"));
  assignment=new LinkedHashMap<>(Map.of("ID","assignment","rootId","root","status","OPEN","valid",true,"revision",2,"ownerId","intake"));
  when(sales.require(c,"Observations","report")).thenReturn(observation);when(sales.require(c,"Deliveries","actual")).thenReturn(actual);
  when(returns.require(c,"Observations","report")).thenReturn(observation);when(returns.require(c,"Receipts","actual")).thenReturn(actual);
  when(duties.rows("Roots","org")).thenReturn(List.of(root));when(duties.require("Roots","org","root")).thenReturn(root);when(duties.rows("Assignments","org")).thenReturn(List.of(assignment));
  movement("DELIVERY",new BigDecimal("40"));
 }
 void movement(String kind,BigDecimal q){when(inventory.rows(c,"QuantityMovements")).thenReturn(List.of(Map.of("targetId","segment","evidenceRef","canonical","kind",kind,"quantity",q)));}
 void deliver(){remedy.reconcileDeliveryObservation(c,"report","actual");}
 void resolved(String canonical){verify(duties).update("Assignments","org","assignment",Map.of("status","RESOLVED","evidenceId",canonical,"basis","VERIFIED_TRADE_OBSERVATION:actual","revision",3));}
 @Test void exactFortyBoxesResolvesTheReportedRootWithoutRewritingItsQuantityOrOwner(){deliver();resolved("canonical");assertEquals(new BigDecimal("40.000"),root.get("quantity"));assertEquals("intake",assignment.get("ownerId"));verify(evidence).requireCanonical(c,"canonical","PHYSICAL_DELIVERY","item","report",new BigDecimal("40"),"BOX");verify(duties,never()).update(eq("Roots"),anyString(),anyString(),anyMap());}
 @Test void differentActualQuantityCannotResolveTheRoot(){root.put("quantity",new BigDecimal("39"));assertThrows(DomainError.class,this::deliver);verify(duties,never()).update(anyString(),anyString(),anyString(),anyMap());}
 @Test void differentUnitCannotResolveTheRoot(){root.put("unit","EA");assertThrows(DomainError.class,this::deliver);verify(duties,never()).update(anyString(),anyString(),anyString(),anyMap());}
 @Test void reportedQuantityDoesNotReplaceVerifiedActualQuantity(){observation.put("quantity",new BigDecimal("41"));assertThrows(DomainError.class,this::deliver);verifyNoInteractions(evidence);verify(duties,never()).update(anyString(),anyString(),anyString(),anyMap());}
 @Test void noAppliedMovementMeansNoCompletion(){movement("DELIVERY",new BigDecimal("39"));assertThrows(DomainError.class,this::deliver);verify(duties,never()).update(anyString(),anyString(),anyString(),anyMap());}
 @Test void unverifiedActualEvidenceCannotDischargeTheDuty(){doThrow(DomainError.forbidden()).when(evidence).requireCanonical(any(),anyString(),anyString(),anyString(),anyString(),any(),anyString());assertThrows(DomainError.class,this::deliver);verify(duties,never()).update(anyString(),anyString(),anyString(),anyMap());}
 @Test void legacyNonquantityDeliveryStillResolves(){root.remove("quantity");root.remove("unit");deliver();resolved("canonical");}
 void returnRoot(){root.put("kind","RETURN_RECONCILIATION");root.put("scopeJson","{\"originalWorkId\":\"work\",\"domainSourceId\":\"report\",\"physicalScopeId\":\"range\"}");movement("RETURN_MOVE",new BigDecimal("40"));}
 @Test void legacyNonquantityReturnStillResolves(){returnRoot();root.remove("quantity");root.remove("unit");remedy.reconcileReturnObservation(c,"report","actual");resolved("canonical");}
 @Test void quantityBearingReturnRemainsUnsupported(){returnRoot();assertThrows(DomainError.class,()->remedy.reconcileReturnObservation(c,"report","actual"));verify(duties,never()).update(anyString(),anyString(),anyString(),anyMap());}
 @Test void duplicateLegacyReturnKeepsItsOwnVerifiedEvidence(){returnRoot();root.remove("quantity");root.remove("unit");actual.put("observationId","first-report");when(evidence.requireCanonical(c,"own-canonical","RETURN_RECEIPT","item","report",new BigDecimal("40"),"BOX")).thenReturn(Map.of("occurrenceIdentity","event"));remedy.reconcileDuplicateReturnObservation(c,"report","actual","own-canonical");resolved("own-canonical");verifyNoInteractions(inventory);}
 @ParameterizedTest @ValueSource(strings={"RESOLVED","WAIVED","TRANSFERRED"})
 void alreadySettledOrTransferredAssignmentsAreNotChanged(String status){assignment.put("status",status);deliver();verify(duties,never()).update(anyString(),anyString(),anyString(),anyMap());}
}
