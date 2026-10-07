package com.mulino.trade.sales;
import com.mulino.application.core.*;
import com.mulino.application.trade.*;
import com.mulino.application.trade.sales.*;
import com.mulino.application.work.WorkAccess;
import com.mulino.application.responsibility.ResponsibilityService;
import com.mulino.domain.evidence.EvidenceRepository;
import com.mulino.domain.trade.sales.SalesRepository;
import java.math.BigDecimal;import java.time.Instant;import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;import static org.mockito.ArgumentMatchers.*;
class DeliveryCorrectionTest {
 final SalesRepository r=mock(SalesRepository.class);final TradeEvidence verified=mock(TradeEvidence.class);final EvidenceRepository evidence=mock(EvidenceRepository.class);final WorkAccess work=mock(WorkAccess.class);final ResponsibilityService duties=mock(ResponsibilityService.class);
 final DeliveryCorrection service=new DeliveryCorrection(r,verified,evidence,work,duties);
 final Instant at=Instant.parse("2026-10-08T00:00:00Z");final DomainContext c=new DomainContext("org","actor","owner",at,at);
 final List<Map<String,Object>> corrections=new ArrayList<>(),opened=new ArrayList<>();
 final Map<String,Object> delivery=new LinkedHashMap<>(Map.ofEntries(Map.entry("ID","delivery"),Map.entry("workId","work"),Map.entry("itemId","item"),Map.entry("observationId","observation"),Map.entry("placeId","place"),Map.entry("unit","EA"),Map.entry("quantity",q(100)),Map.entry("legitimateQuantity",q(100)),Map.entry("canonicalOccurrenceId","original"),Map.entry("occurredAt",at)));
 static BigDecimal q(int n){return BigDecimal.valueOf(n);}
 DeliveryCorrectionTest(){when(r.require(c,"Deliveries","delivery")).thenReturn(delivery);when(r.rows(c,"DeliveryCorrections")).thenAnswer(x->List.copyOf(corrections));doAnswer(x->{corrections.add(new LinkedHashMap<>(x.getArgument(1)));return null;}).when(r).insert(eq("DeliveryCorrections"),anyMap());when(work.require(c,"work",true)).thenReturn(Map.of("ID","work","status","ACTIVE"));when(duties.openDuty(eq(c),anyMap())).thenAnswer(x->{opened.add(new LinkedHashMap<>(x.getArgument(1)));return Map.of("rootId","root"+opened.size());});when(evidence.require("CanonicalOccurrences","org","original")).thenReturn(Map.of("revision",1));}
 void fact(String id,String previous,int quantity){when(evidence.require("CanonicalOccurrences","org",id)).thenReturn(Map.of("ID",id,"supersedesId",previous,"workId","work","placeId","place","quantity",q(quantity),"effectiveFrom",at));}
 @Test void fixedClockCorrectionsCreateOnlyNewDeficitPortion(){fact("first","original",98);fact("second","first",97);var first=service.correctionImpact(c,"delivery","first");assertEquals(q(2),first.get("deficitQuantity"));assertEquals(q(2),opened.getFirst().get("quantity"));var second=service.correctionImpact(c,"delivery","second");assertEquals(q(3),second.get("deficitQuantity"));assertEquals(q(1),opened.getLast().get("quantity"));assertTrue(opened.getLast().get("scopeJson").toString().contains("\"startQuantity\":\"2\""));assertEquals(2,corrections.getLast().get("revision"));assertEquals(q(100),delivery.get("quantity"));assertEquals(q(100),delivery.get("legitimateQuantity"));assertTrue(((List<?>)second.get("physicalEffects")).isEmpty());service.correctionImpact(c,"delivery","second");assertEquals(2,opened.size());assertEquals(2,corrections.size());fact("restored","second",99);service.correctionImpact(c,"delivery","restored");fact("again","restored",97);service.correctionImpact(c,"delivery","again");assertEquals(2,opened.size());assertEquals(4,corrections.size());assertEquals(4,corrections.getLast().get("revision"));}
 @Test void unrelatedCanonicalCannotProduceCorrectionOrDuty(){fact("unrelated","other",98);assertThrows(DomainError.class,()->service.correctionImpact(c,"delivery","unrelated"));assertTrue(opened.isEmpty());assertTrue(corrections.isEmpty());}
}
