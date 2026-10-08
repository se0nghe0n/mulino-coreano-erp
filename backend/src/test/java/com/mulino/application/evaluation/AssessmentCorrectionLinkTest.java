package com.mulino.application.evaluation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.mulino.application.core.*;
import com.mulino.application.responsibility.ResponsibilityService;
import com.mulino.application.trade.DeliveryCorrectionPort;
import com.mulino.domain.evaluation.AssessmentRepository;
import com.mulino.domain.evidence.EvidenceRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.ObjectProvider;

/** The canonical written by the link transaction is newer than the request knownAt fence; it must still be read exactly, and unknown IDs fail closed. */
class AssessmentCorrectionLinkTest {
 static final String ORG=id(),CLAIM=id(),CANONICAL=id(),PRIOR=id(),SCOPE=id(),DELIVERY=id();
 static final Instant KNOWN=Instant.parse("2026-10-07T09:00:00Z");
 final DomainContext c=new DomainContext(ORG,id(),"request",KNOWN,KNOWN);
 AssessmentRepository assessments;EvidenceRepository evidence;DeliveryCorrectionPort port;AssessmentCorrectionImpact impact;
 @SuppressWarnings("unchecked") @BeforeEach void fixture(){
  assessments=mock(AssessmentRepository.class);evidence=mock(EvidenceRepository.class);port=mock(DeliveryCorrectionPort.class);
  when(assessments.rows(eq(c),anyString())).thenReturn(List.of());
  when(assessments.rows(c,"mulino.trade.sales.Deliveries")).thenReturn(List.of(Map.of("ID",DELIVERY,"observationId",SCOPE)));
  when(evidence.require(eq("CanonicalOccurrences"),eq(ORG),anyString())).thenThrow(DomainError.forbidden());
  ObjectProvider<ResponsibilityService> duties=mock(ObjectProvider.class);ObjectProvider<DeliveryCorrectionPort> ports=mock(ObjectProvider.class);when(ports.stream()).thenAnswer(i->java.util.stream.Stream.of(port));
  ObjectProvider<com.mulino.application.trade.SettlementContributionPort> settlements=mock(ObjectProvider.class);when(settlements.stream()).thenAnswer(i->java.util.stream.Stream.empty());
  impact=new AssessmentCorrectionImpact(mock(AssessmentService.class),assessments,duties,ports,evidence,settlements);
 }
 @Test void deliveryCorrectionWrittenAfterKnownAtReachesExactOriginalDelivery(){
  doReturn(new LinkedHashMap<String,Object>(Map.of("ID",CANONICAL,"organizationId",ORG,"kind","PHYSICAL_DELIVERY","supersedesId",PRIOR,"physicalScopeId",SCOPE,"recordedAt",KNOWN.plusSeconds(1)))).when(evidence).require("CanonicalOccurrences",ORG,CANONICAL);
  impact.evidenceLinked(c,CLAIM,CANONICAL);
  verify(port).correctionImpact(c,DELIVERY,CANONICAL);
 }
 @Test void ordinaryCanonicalHasNoDeliveryCorrection(){
  doReturn(new LinkedHashMap<String,Object>(Map.of("ID",CANONICAL,"organizationId",ORG,"kind","PHYSICAL_RECEIPT","physicalScopeId",SCOPE,"recordedAt",KNOWN.plusSeconds(1)))).when(evidence).require("CanonicalOccurrences",ORG,CANONICAL);
  impact.evidenceLinked(c,CLAIM,CANONICAL);
  verifyNoInteractions(port);
 }
 @Test void unknownCanonicalFailsClosed(){
  assertEquals("FORBIDDEN",assertThrows(DomainError.class,()->impact.evidenceLinked(c,CLAIM,id())).code());
  verifyNoInteractions(port);
 }
 private static String id(){return UUID.randomUUID().toString();}
}
