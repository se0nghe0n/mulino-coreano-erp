package com.mulino.quality;
import com.mulino.domain.inventory.QualityRanges;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class QualityRangesTest {
 QualityRanges.Range r(String a,String b){return new QualityRanges.Range(new BigDecimal(a),new BigDecimal(b));}
 @Test void partialIntersectionCannotAddDisjointAuthorizations(){assertEquals(new BigDecimal("30"),QualityRanges.quantity(QualityRanges.intersect(List.of(r("0","100")),List.of(r("0","30")))));assertEquals(BigDecimal.ZERO,QualityRanges.quantity(QualityRanges.intersect(List.of(r("0","30")),List.of(r("30","100")))));}
 @Test void overlapIsUnionedAndIndependentHoldsSubtract(){var total=List.of(r("0","60"));assertEquals(new BigDecimal("40"),QualityRanges.quantity(QualityRanges.subtract(total,List.of(r("0","20")))));assertEquals(BigDecimal.ZERO,QualityRanges.quantity(QualityRanges.subtract(total,List.of(r("0","20"),r("0","60")))));assertEquals(new BigDecimal("60"),QualityRanges.quantity(List.of(r("0","40"),r("20","60"))));}
 @Test void decimalsRemainExact(){assertEquals(new BigDecimal("0.000000000001"),QualityRanges.quantity(QualityRanges.intersect(List.of(r("0","0.000000000003")),List.of(r("0.000000000002","0.000000000004")))));}
 @Test void fullClosedTimeIntervalReleasesAtDecisionInstant(){var at=Instant.parse("2026-10-08T00:00:00Z");var b=new HashMap<String,Object>(Map.of("state","ACTIVE","validFrom",at,"validUntil",at));assertTrue(com.mulino.application.quality.QualityEligibility.active(b,at));assertFalse(com.mulino.application.quality.QualityEligibility.active(b,at.plusNanos(1)));b.put("releasedAt",at);assertFalse(com.mulino.application.quality.QualityEligibility.active(b,at));}
}
