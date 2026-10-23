package com.mulino.trade.sales;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mulino.application.core.*;
import com.mulino.application.trade.sales.DeliveryAdmission;
import com.mulino.domain.evaluation.CanonicalFactAdmission.Purpose;
import com.mulino.domain.trade.sales.SalesRepository;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

/** Privilege counterexample: a WORK dimension matches any granted value, not all candidates. */
class DeliveryAdmissionAuthorityTest {
 private static final Instant AT=Instant.parse("2026-10-07T02:00:00Z");
 private static final DomainContext CONTEXT=new DomainContext("org","actor","owner",AT,AT);
 private static final String REQUESTED="requested-work",SOURCE="source-work";
 private final SalesRepository rows=mock(SalesRepository.class);
 private final Map<String,Object> delivery=Map.ofEntries(
   Map.entry("ID","delivery"),Map.entry("canonicalOccurrenceId","canonical"),
   Map.entry("workId",SOURCE),Map.entry("itemId","item"),Map.entry("placeId","place"),
   Map.entry("observationId","observation"),Map.entry("segmentId","segment"),Map.entry("lotId","lot"),
   Map.entry("quantity",new BigDecimal("90")),Map.entry("legitimateQuantity",new BigDecimal("80")),
   Map.entry("unit","BOX"),Map.entry("occurredAt",AT));
 private final Map<String,Object> canonical=Map.of(
   "ID","canonical","workId",SOURCE,"itemId","item","placeId","place",
   "physicalScopeId","observation","quantity",new BigDecimal("90"),"unit","BOX","effectiveFrom",AT);
 DeliveryAdmissionAuthorityTest(){when(rows.rows(CONTEXT,"Deliveries")).thenReturn(List.of(delivery));when(rows.rows(CONTEXT,"DeliveryCorrections")).thenReturn(List.of());}

 private AnyValueGrant grant(Purpose purpose,Set<String> works){return new AnyValueGrant(Set.of(purpose.capability()),works);}
 private DeliveryAdmission service(AnyValueGrant grant){return new DeliveryAdmission(rows,grant);}

 @ParameterizedTest @EnumSource(value=Purpose.class,names={"ASSESS_GOAL","CLOSE_WORK","OPEN_OBLIGATION"})
 void requestedWorkOnlyCannotReadTheOtherSource(Purpose purpose){
  assertFalse(service(grant(purpose,Set.of(REQUESTED))).admit(CONTEXT,REQUESTED,"goal",canonical,purpose).admitted());
 }
 @ParameterizedTest @EnumSource(value=Purpose.class,names={"ASSESS_GOAL","CLOSE_WORK","OPEN_OBLIGATION"})
 void sourceWorkOnlyCannotActOnRequestedWork(Purpose purpose){
  assertFalse(service(grant(purpose,Set.of(SOURCE))).admit(CONTEXT,REQUESTED,"goal",canonical,purpose).admitted());
 }
 @ParameterizedTest @EnumSource(value=Purpose.class,names={"ASSESS_GOAL","CLOSE_WORK","OPEN_OBLIGATION"})
 void bothWorkEndpointsAllowTheActualLegitimateQuantity(Purpose purpose){
  var result=service(grant(purpose,Set.of(REQUESTED,SOURCE))).admit(CONTEXT,REQUESTED,"goal",canonical,purpose);
  assertTrue(result.admitted());assertEquals(new BigDecimal("80"),result.recognizedQuantity());
  assertEquals("lot",result.lotId());assertEquals(List.of("delivery","segment","canonical"),result.evidenceRefs());
 }
 static Stream<Arguments> wrongDimensions(){return Arrays.stream(Purpose.values()).filter(p->!p.causalRecord()).flatMap(p->Stream.of("TARGET","ITEM","PLACE").map(d->Arguments.of(p,d)));}
 @ParameterizedTest @MethodSource("wrongDimensions")
 void anotherRequiredDimensionCannotBeBorrowed(Purpose purpose,String dimension){
  var grant=grant(purpose,Set.of(REQUESTED,SOURCE));grant.scopes.put(dimension,Set.of("foreign"));
  assertFalse(service(grant).admit(CONTEXT,REQUESTED,"goal",canonical,purpose).admitted());
 }
 @ParameterizedTest @EnumSource(value=Purpose.class,names={"ASSESS_GOAL","CLOSE_WORK","OPEN_OBLIGATION"})
 void permissionForTheOtherOperationIsInsufficient(Purpose purpose){
  var other=purpose==Purpose.ASSESS_GOAL?Purpose.CLOSE_WORK:Purpose.ASSESS_GOAL;
  assertFalse(service(grant(other,Set.of(REQUESTED,SOURCE))).admit(CONTEXT,REQUESTED,"goal",canonical,purpose).admitted());
 }
 @Test void successfulOperationAloneDoesNotChangeFactsOrTheirHash() throws Exception {
  var grant=new AnyValueGrant(Set.of("assessGoal","closeWork"),Set.of(REQUESTED,SOURCE));var service=service(grant);
  var assessed=service.admit(CONTEXT,REQUESTED,"goal",canonical,Purpose.ASSESS_GOAL);
  var closed=service.admit(CONTEXT,REQUESTED,"goal",canonical,Purpose.CLOSE_WORK);
  assertTrue(assessed.admitted());assertEquals(assessed,closed);
  var json=new ObjectMapper();assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(assessed)),MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(closed)));
 }
 @Test void legacyCallStillRequiresAssessGoal(){
  assertTrue(service(grant(Purpose.ASSESS_GOAL,Set.of(REQUESTED,SOURCE))).admit(CONTEXT,REQUESTED,"goal",canonical).admitted());
  assertFalse(service(grant(Purpose.CLOSE_WORK,Set.of(REQUESTED,SOURCE))).admit(CONTEXT,REQUESTED,"goal",canonical).admitted());
 }
 @Test void nullPurposeDoesNotAdmit(){assertThrows(NullPointerException.class,()->service(grant(Purpose.CLOSE_WORK,Set.of(REQUESTED,SOURCE))).admit(CONTEXT,REQUESTED,"goal",canonical,null));}
 @ParameterizedTest @EnumSource(value=Purpose.class,names={"RECORD_DELIVERY","RECORD_OBSERVED_MOVEMENT"})
 void recordCapabilityAndEnumAloneCannotMintACausalAssessment(Purpose purpose){
  var verification=mock(com.mulino.application.evidence.CanonicalEvidenceVerification.class);
  when(rows.currentRecordCause(CONTEXT,REQUESTED,purpose.capability())).thenThrow(DomainError.forbidden());
  var admission=new DeliveryAdmission(rows,grant(purpose,Set.of(REQUESTED,SOURCE)),verification);
  assertFalse(admission.admit(CONTEXT,REQUESTED,"goal",canonical,purpose).admitted());verifyNoInteractions(verification);
 }

 /** Faithful local grant rule: all configured dimensions, any value within each dimension.
  * Passing [requested, source] once is deliberately insufficient proof for both. */
 private static final class AnyValueGrant implements ReadAuthorizer {
  final Set<String> capabilities;final Map<String,Set<String>> scopes=new LinkedHashMap<>();
  AnyValueGrant(Set<String> capabilities,Set<String> works){this.capabilities=capabilities;scopes.put("TARGET",Set.of(REQUESTED));scopes.put("WORK",works);scopes.put("ITEM",Set.of("item"));scopes.put("PLACE",Set.of("place"));}
  public DomainContext context(Instant asOf,Instant knownAt){return new DomainContext("org","actor","owner",asOf,knownAt);}
  public void authorize(DomainContext c,String capability,String target){throw new UnsupportedOperationException("Use the scoped contract");}
  public boolean permittedScopes(DomainContext c,String capability,Map<String,? extends Collection<String>> candidates){
   return c.organizationId().equals("org")&&capabilities.contains(capability)&&scopes.entrySet().stream().allMatch(d->candidates.containsKey(d.getKey())&&d.getValue().stream().anyMatch(candidates.get(d.getKey())::contains));
  }
 }
}
