package com.mulino.quality;

import com.mulino.application.core.*;
import com.mulino.application.identity.IdentityAuthorization;
import com.mulino.application.inventory.InventoryReadCandidates;
import com.mulino.application.quality.*;
import com.mulino.application.trade.regulatory.RegulatoryEligibility;
import com.mulino.domain.governance.PolicyRepository;
import com.mulino.domain.inventory.*;
import com.mulino.domain.work.read.WorkReadRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PublicScopeEligibilityTest {
  static final Instant AT=Instant.parse("2026-10-08T00:00:00Z");
  static final String ITEM="00000000-0000-0000-0000-000000000001";
  static final String PLACE="00000000-0000-0000-0000-000000000002";
  static final String SEGMENT="00000000-0000-0000-0000-000000000003";
  final DomainContext c=new DomainContext("org","actor","actor",AT,AT);
  @Test void physicalRetirementHonorsKnowledgeBoundaryWithoutGuessingMissingHistory() {
    var row=new HashMap<String,Object>(Map.of("validFrom",AT.minusSeconds(10),"retiredAt",AT,"retirementRecordedAt",AT.plusSeconds(1)));
    assertTrue(InventoryReadCandidates.active(row,c));
    assertFalse(InventoryReadCandidates.active(row,new DomainContext("org","actor","actor",AT,AT.plusSeconds(1))));
    row.remove("retirementRecordedAt");assertFalse(InventoryReadCandidates.active(row,c));
    row.put("retiredAt",AT.plusNanos(1));assertTrue(InventoryReadCandidates.active(row,c));
    row.put("validFrom",AT.plusNanos(1));assertFalse(InventoryReadCandidates.active(row,c));
  }
  @Test void deniedSegmentIsNeverAssessedOrDisclosed() {
    var repository=mock(InventoryRepository.class);var auth=mock(IdentityAuthorization.class);
    var eligibility=spy(new QualityEligibility(repository,mock(StockPrimitives.class),mock(QualityEvidence.class),mock(RegulatoryEligibility.class),mock(PolicyRepository.class),auth,mock(WorkReadRepository.class)));
    var row=Map.<String,Object>of("ID",SEGMENT,"itemId",ITEM,"placeId",PLACE,"lotId",ITEM,"validFrom",AT.minusSeconds(1),"quantity",BigDecimal.TEN,"unit","EA");
    when(repository.object(c,"TradeItems",ITEM)).thenReturn(Map.of("ID",ITEM));
    when(repository.rows(c,"QuantitySegments")).thenReturn(List.of(row));
    when(auth.permittedScopes(eq(c),eq("evaluateEligibility"),anyMap())).thenReturn(false);
    doThrow(DomainError.forbidden()).when(auth).authorizeScopes(eq(c),eq("evaluateEligibility"),anyMap());
    assertEquals("FORBIDDEN",assertThrows(DomainError.class,()->eligibility.query(c,new QueryRequest("evaluateEligibility",null,Map.of("itemId",ITEM),Map.of(),50,null,null,AT,AT,null))).code());
    verify(eligibility,never()).assess(any(),anyMap(),anyString(),nullable(String.class));
    verify(repository,never()).object(c,"Places",PLACE);
  }
}
