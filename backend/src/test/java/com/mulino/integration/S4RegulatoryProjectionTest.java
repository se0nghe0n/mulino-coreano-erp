package com.mulino.integration;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.mulino.application.core.*;
import com.mulino.application.trade.regulatory.*;
import com.mulino.domain.trade.regulatory.RegulatoryRepository;
import com.mulino.domain.inventory.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Deterministic range oracle; actual E1 DB/API acceptance remains separate. */
class S4RegulatoryProjectionTest {
 @Test void agencyFirstThirtyRemainsOnlyOnMovedThirtyAfterSplittingSixty(){
  var c=new DomainContext("org","actor","request",Instant.parse("2026-10-07T09:00:00Z"),Instant.parse("2026-10-07T09:00:00Z"));
  var inventory=mock(InventoryRepository.class);var repository=mock(RegulatoryRepository.class);var proof=mock(RegulatoryEvidence.class);
  var ranges=new PhysicalRanges(inventory);var service=new RegulatoryEligibility(repository,proof,inventory,ranges);
  when(inventory.rows(c,"GenealogyEdges")).thenReturn(List.of(edge("receipt60","moved30","0","0","30"),edge("receipt60","warehouse30","30","0","30")));
  for(String segment:List.of("moved30","warehouse30"))when(inventory.current(c,"QuantitySegments",segment)).thenReturn(Map.of("itemId","item","lotId","lot","unit","BOX","quantity",new BigDecimal("30"),"mixtureStatus","IDENTIFIED","identificationStatus","CONFIRMED"));
  var from=Instant.parse("2026-10-07T08:00:00Z");
  when(repository.rows(c,"Policies")).thenReturn(List.of(Map.of("ID","policy","action","SALE","status","ACTIVE","validFrom",from,"requiresLabel",false)));
  when(repository.rows(c,"DecisionVersions")).thenReturn(List.of(Map.ofEntries(Map.entry("ID","decision"),Map.entry("itemId","item"),Map.entry("lotId","lot"),Map.entry("physicalScopeId","receipt60"),Map.entry("action","SALE"),Map.entry("unit","BOX"),Map.entry("policyId","policy"),Map.entry("validFrom",from),Map.entry("occurrenceId","agency-proof"),Map.entry("procedureId","procedure"),Map.entry("procedureVersionId","version"),Map.entry("startQuantity",BigDecimal.ZERO),Map.entry("quantity",new BigDecimal("30")),Map.entry("decision","ALLOWED"))));
  when(repository.rows(c,"LabelVerifications")).thenReturn(List.of());when(repository.require(c,"Procedures","procedure")).thenReturn(Map.of("ID","procedure"));
  var version=Map.<String,Object>of("ID","version","procedureId","procedure","status","SUBMITTED","occurrenceId","submission-proof");
  when(repository.rows(c,"ProcedureVersions")).thenReturn(List.of(version));when(repository.require(c,"ProcedureVersions","version")).thenReturn(version);when(proof.current(c,"agency-proof")).thenReturn(true);when(proof.current(c,"submission-proof")).thenReturn(true);
  var moved=service.assess(c,"item","lot","moved30","SALE",new BigDecimal("30"),"BOX");var remainder=service.assess(c,"item","lot","warehouse30","SALE",new BigDecimal("30"),"BOX");
  assertEquals(new BigDecimal("30"),moved.eligibleQuantity());assertEquals(List.of(new RegulatoryEligibility.Range(BigDecimal.ZERO,new BigDecimal("30"))),moved.allowedRanges());assertEquals(BigDecimal.ZERO,remainder.eligibleQuantity());assertEquals(List.of(),remainder.allowedRanges());assertEquals(List.of("agency-proof"),moved.evidenceRefs());
  verify(proof,times(2)).current(c,"agency-proof");verify(proof,never()).current(c,"moved30");
 }
 private Map<String,Object> edge(String source,String target,String from,String to,String q){return Map.of("sourceId",source,"targetId",target,"sourceStartQuantity",new BigDecimal(from),"targetStartQuantity",new BigDecimal(to),"quantity",new BigDecimal(q),"uncertain",false);}
}
