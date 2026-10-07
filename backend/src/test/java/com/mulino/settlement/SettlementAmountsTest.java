package com.mulino.settlement;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.mulino.domain.trade.settlement.SettlementAmounts.*;
import java.math.BigDecimal;
import java.util.Map;
class SettlementAmountsTest {
 @Test void exactRateSnapshotAndExplicitRounding(){var fx=fx("EUR",new BigDecimal("100"),Map.of("pair","EUR/KRW","rate","1500","date","2026-10-07","source","synthetic-fx","policyVersion","fx-v1","rounding","HALF_UP_SCALE_0"));assertEquals(new BigDecimal("150000"),fx.convertedAmount());assertEquals("fx-v1",fx.policyVersion());assertEquals("1500",fx.rate().toString());}
 @Test void floatOverflowWrongPairAndMissingPolicyAreRejected(){assertThrows(RuntimeException.class,()->decimal(0.1d));assertThrows(RuntimeException.class,()->decimal("0.0000000000001"));assertThrows(RuntimeException.class,()->decimal("100000000000000000000000000"));assertThrows(RuntimeException.class,()->fx("EUR",BigDecimal.ONE,Map.of("pair","USD/KRW","rate","1500","date","2026-10-07","source","s","policyVersion","v","rounding","HALF_UP_SCALE_0")));assertThrows(RuntimeException.class,()->fx("EUR",BigDecimal.ONE,Map.of("pair","EUR/KRW","rate","1500","date","2026-10-07","source","s","rounding","HALF_UP_SCALE_0")));}
}
