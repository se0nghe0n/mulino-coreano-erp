package com.mulino.settlement;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.mulino.application.core.*;
import com.mulino.application.trade.TradeEvidence;
import com.mulino.application.trade.settlement.*;
import com.mulino.domain.trade.settlement.SettlementRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
/**
 * s4-settle-02 (plan §4.3·§6 정산): a SALE match frozen at delivery100 is re-derived from the current
 * corrected delivery credit (98) at read and closure time. The PostgreSQL delivery-correction path itself
 * belongs to the sales owner (DeliveryCorrection); this exercises the shared settlement predicate only.
 */
class SettlementStateTest {
 final DomainContext c=new DomainContext("org","actor","request",Instant.EPOCH,Instant.EPOCH);
 final SettlementTradeFacts facts=mock(SettlementTradeFacts.class);
 final SettlementState state=new SettlementState(mock(SettlementRepository.class),facts,mock(TradeEvidence.class));
 static BigDecimal d(String v){return new BigDecimal(v);}
 Map<String,Object> match(){var m=new LinkedHashMap<String,Object>();m.putAll(Map.of("ID","match","scopeKind","SALE","lineId","line","referenceId","delivery","receivedQuantity",d("100"),"orderedQuantity",d("100"),"invoiceQuantity",d("100"),"invoiceAmount",d("100000"),"unitPrice",d("1000"),"unit","BOX"));m.putAll(Map.of("quantityDifference",d("0"),"priceDifference",d("0"),"originalDifference",d("0"),"currencyDifference",false,"scopeDifference",false));return m;}
 void credit(String recognized){when(facts.contribution(c,"SALE","line","delivery")).thenReturn(Map.of("quantity",d(recognized),"recognizedQuantity",d(recognized),"unit","BOX","occurrenceId","canonical","referenceId","delivery"));}

 @Test void unchangedDeliveryKeepsSatisfiedMatch(){credit("100");var a=state.assess(c,match(),List.of());assertEquals("SATISFIED",a.get("result"));assertEquals("CURRENT",a.get("contributionState"));}

 @Test void correctedDeliveryMakesFrozenMatchUnsatisfiedWithCurrentDifference(){credit("98");var a=state.assess(c,match(),List.of());
  assertEquals("UNSATISFIED",a.get("result"));assertEquals("CHANGED",a.get("contributionState"));assertEquals(0,d("2").compareTo((BigDecimal)a.get("currentQuantityDifference")));assertEquals(0,d("2000").compareTo((BigDecimal)a.get("currentOriginalDifference")));
  // The immutable match keeps its original judgement (§4.3 당시 판정 보존).
  assertEquals(0,d("0").compareTo((BigDecimal)a.get("quantityDifference")));}

 @Test void unverifiableCurrentDeliveryIsUnverifiedNotSatisfied(){when(facts.contribution(c,"SALE","line","delivery")).thenThrow(new DomainError("HELD","EVIDENCE_UNVERIFIED","superseded"));var a=state.assess(c,match(),List.of());assertEquals("UNVERIFIED",a.get("result"));assertEquals("UNVERIFIED",a.get("contributionState"));}

 @Test void quantityDifferenceIsNeverSatisfiedByAmountAdjustment(){credit("100");var m=match();m.put("invoiceQuantity",d("120"));m.put("quantityDifference",d("20"));m.put("invoiceAmount",d("120000"));m.put("originalDifference",d("20000"));
  var a=state.assess(c,m,List.of(Map.of("matchId","match","status","CONFIRMED","amount",d("20000"))));assertEquals(0,BigDecimal.ZERO.compareTo((BigDecimal)a.get("settlementDifference")));assertEquals("UNSATISFIED",a.get("result"));}
}
