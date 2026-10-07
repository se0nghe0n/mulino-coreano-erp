package com.mulino.application.evidence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.mulino.application.core.*;
import com.mulino.application.identity.IdentityAuthorization;
import com.mulino.application.trade.*;
import com.mulino.adapters.blob.LocalBlobStore;
import com.mulino.domain.evidence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.ObjectProvider;

/** Real public reconciliation logic with isolated repository ports; DB/API acceptance stays separate. */
class S4DeliveryCorrectionContractTest {
 static final String ORG=id(),ACTOR=id(),WORK=id(),ITEM=id(),PLACE=id(),SUBJECT=id(),SOURCE=id(),OLD_EVENT=id(),EVENT=id(),OLD_CLAIM=id(),CLAIM=id(),OLD_CANONICAL=id(),CANONICAL=id(),DOCUMENT=id(),BLOB=id(),IDENTITY=id();
 static final Instant AT=Instant.parse("2026-10-07T09:00:00Z"),NOW=AT.plusSeconds(60);
 final DomainContext c=new DomainContext(ORG,ACTOR,"request",NOW,NOW);
 final Map<String,List<Map<String,Object>>> rows=new HashMap<>();
 EvidenceRepository repository;EvidenceRecords records;EvidenceReconciliation service;EvidenceCorrectionImpact impacts;
 Map<String,Object> currentEvent,currentClaim,oldCanonical;
 @SuppressWarnings("unchecked") @BeforeEach void fixture(){
  repository=mock(EvidenceRepository.class);records=mock(EvidenceRecords.class);var auth=mock(IdentityAuthorization.class);var blobs=mock(LocalBlobStore.class);var clock=mock(ExecutionClock.class);var completion=mock(VerifiedResponsibilityCompletionEvidence.class);impacts=mock(EvidenceCorrectionImpact.class);
  when(clock.instant()).thenReturn(NOW);when(repository.rows(anyString(),eq(ORG))).thenAnswer(i->rows.getOrDefault(i.getArgument(0),List.of()));
  when(repository.require(anyString(),eq(ORG),anyString())).thenAnswer(i->rows.getOrDefault(i.getArgument(0),List.of()).stream().filter(x->i.getArgument(2).equals(x.get("ID"))).findFirst().orElseThrow(DomainError::forbidden));
  doAnswer(i->{rows.computeIfAbsent(i.getArgument(0),k->new ArrayList<>()).add(i.getArgument(1));return null;}).when(repository).insert(anyString(),anyMap());
  var profile=Map.<String,Object>of("ID",SOURCE,"policyVersion","policy","intakeOwnerId",ACTOR,"supervisorId",ACTOR,"nextAction","대조한다","nextCheckAt",NOW.plusSeconds(3600));when(records.source(c,"warehouse")).thenReturn(profile);
  currentEvent=subject(EVENT);currentEvent.putAll(Map.of("kind","PHYSICAL_DELIVERY","sourceNamespace","warehouse","externalEventId","delivery","sourceVersion","2","payload","{\"quantity\":\"98\"}","valueState","KNOWN","effectiveFrom",AT,"supersedesId",OLD_EVENT));var oldEvent=subject(OLD_EVENT);oldEvent.put("kind","PHYSICAL_DELIVERY");
  currentClaim=subject(CLAIM);currentClaim.putAll(Map.of("eventId",EVENT,"quantity",new BigDecimal("98"),"unit","BOX","effectiveFrom",AT,"valueState","KNOWN"));var priorClaim=subject(OLD_CLAIM);priorClaim.put("eventId",OLD_EVENT);
  oldCanonical=subject(OLD_CANONICAL);oldCanonical.putAll(Map.of("kind","PHYSICAL_DELIVERY","physicalScopeId",SUBJECT,"occurrenceIdentity",IDENTITY,"occurrenceSemanticHash","a".repeat(64),"quantity",new BigDecimal("100"),"unit","BOX","effectiveFrom",AT));
  var document=subject(DOCUMENT);document.putAll(Map.of("availability","AVAILABLE","blobId",BLOB,"sha256","b".repeat(64)));when(blobs.available(UUID.fromString(BLOB),"b".repeat(64))).thenReturn(true);when(blobs.read(UUID.fromString(BLOB),"b".repeat(64))).thenReturn("{\"quantity\":\"98\"}".getBytes());
  rows.put("Events",new ArrayList<>(List.of(oldEvent,currentEvent)));rows.put("Claims",new ArrayList<>(List.of(priorClaim,currentClaim)));rows.put("CanonicalOccurrences",new ArrayList<>(List.of(oldCanonical)));rows.put("DocumentVersions",new ArrayList<>(List.of(document)));rows.put("Verifications",new ArrayList<>(List.of(Map.of("ID",id(),"claimId",OLD_CLAIM,"canonicalOccurrenceId",OLD_CANONICAL,"verdict","VERIFIED"))));rows.put("InboxRecords",new ArrayList<>(List.of(Map.of("sourceNamespace","warehouse","externalEventId","delivery","sourceVersion","2"))));
  var trade=mock(TradeEvidenceScopePort.class);when(trade.eventKinds()).thenReturn(Set.of("PHYSICAL_DELIVERY"));when(trade.require(eq(c),eq(SUBJECT),anyMap(),anyMap(),anyMap(),any())).thenReturn(new TradeEvidenceScopePort.Scope(Map.of("itemId",ITEM,"placeId",PLACE,"workId",WORK),Map.of("ITEM",List.of(ITEM),"WORK",List.of(WORK)),IDENTITY,"c".repeat(64)));
  ObjectProvider<TradeEvidenceScopePort> tradeProvider=mock(ObjectProvider.class);when(tradeProvider.stream()).thenAnswer(i->java.util.stream.Stream.of(trade));ObjectProvider<EvidenceCorrectionImpact> impactProvider=mock(ObjectProvider.class);when(impactProvider.getIfAvailable()).thenReturn(impacts);
  ObjectProvider<com.mulino.domain.evidence.ExternalOperationScopePort> external=mock(ObjectProvider.class);ObjectProvider<ReceiptEvidenceScopePort> receipts=mock(ObjectProvider.class);
  service=new EvidenceReconciliation(repository,records,auth,blobs,impactProvider,external,clock,completion,receipts,tradeProvider);
  when(records.verifyReviewedReceiptCanonical(any(),anyMap())).thenReturn(Map.of("id",CANONICAL,"outcome","VERIFIED_RECORD_ONLY"));
 }
 @Test void explicitRevisionAppendsCanonicalPredecessorAndAtomicImpact(){
  var matched=service.match(c,review(),"matchSourceIdentity");assertEquals("MATCHED",matched.get("outcome"));
  var linked=service.link(c,matched.get("id").toString());assertEquals(CANONICAL,linked.get("id"));
  var input=org.mockito.ArgumentCaptor.forClass(EvidenceRecords.CanonicalInput.class);verify(records).verifyReviewedReceiptCanonical(input.capture(),anyMap());assertEquals(OLD_CANONICAL,input.getValue().supersedesId());assertNull(input.getValue().existingCanonicalId());
  assertEquals(new BigDecimal("100"),oldCanonical.get("quantity"));verify(impacts).evidenceLinked(c,CLAIM,CANONICAL);
 }
 @Test void changedQuantityWithoutExplicitRevisionRemainsConflict(){
  currentEvent.remove("supersedesId");assertEquals("CONFLICT",service.match(c,review(),"matchSourceIdentity").get("outcome"));verifyNoInteractions(impacts);
 }
 @Test void competingCanonicalSuccessorCannotBeReplacedAgain(){
  var successor=subject(id());successor.put("supersedesId",OLD_CANONICAL);rows.get("CanonicalOccurrences").add(successor);
  assertEquals("EVIDENCE_CONFLICT",assertThrows(DomainError.class,()->service.match(c,review(),"matchSourceIdentity")).code());
 }
 private EvidenceReconciliation.Review review(){return new EvidenceReconciliation.Review(CLAIM,DOCUMENT,SUBJECT,null,"policy","warehouse:delivery:2","98","BOX",AT,"원 인도 수량을 정정한다");}
 private Map<String,Object> subject(String id){var m=new LinkedHashMap<String,Object>();m.putAll(Map.of("ID",id,"organizationId",ORG,"subjectKind","DELIVERY_OBSERVATION","subjectId",SUBJECT,"sourceProfileId",SOURCE,"workId",WORK,"itemId",ITEM,"placeId",PLACE));return m;}
 private static String id(){return UUID.randomUUID().toString();}
}
