package com.mulino.inventory;
import static org.junit.jupiter.api.Assertions.*;
import com.mulino.application.core.DomainError;
import com.mulino.domain.inventory.InventoryQuantity;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
class InventoryQuantityTest {
 @Test void decimalRangeAndUnitPrecisionRejectWithoutRounding() {
  assertThrows(DomainError.class,()->InventoryQuantity.parse("0.5",0));
  assertThrows(DomainError.class,()->InventoryQuantity.parse("1.0000000000001",12));
  assertThrows(DomainError.class,()->InventoryQuantity.parse("100000000000000000000000000",12));
  assertThrows(DomainError.class,()->InventoryQuantity.parse(1.0,12));
  assertThrows(DomainError.class,()->InventoryQuantity.parse("1e2",0));
  assertEquals(0,InventoryQuantity.parse("12.000",0).compareTo(new BigDecimal("12")));
 }
 @Test void conversionHasNoInventorySideEffectsAndRejectsUnrepresentableBaseAmount() {
  assertEquals(0,InventoryQuantity.convert("1",new BigDecimal("12"),0).compareTo(new BigDecimal("12")));
  assertThrows(DomainError.class,()->InventoryQuantity.convert("0.1",new BigDecimal("12"),0));
 }
}
