package com.mulino.inventory;

import static org.junit.jupiter.api.Assertions.*;

import com.mulino.application.core.DomainError;
import com.mulino.application.inventory.ItemCommands;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** S3-6: registerItem's base precision is expressible in contracts/intent.schema.json (a canonical integer string). */
class ItemPrecisionSlotTest {
  @Test void canonicalIntegerStringIsTheSchemaForm() {
    assertEquals(0,ItemCommands.decimalPlaces(Map.of("decimalPlaces","0")));
    assertEquals(12,ItemCommands.decimalPlaces(Map.of("decimalPlaces","12")));
    assertEquals(3,ItemCommands.decimalPlaces(Map.of("decimalPlaces",3)));
  }
  @Test void nonCanonicalOrOutOfRangePrecisionIsRejected() {
    for(Object bad:new Object[]{"13","-1","01","1.0"," 2","",2.0,true,13,-1})
      assertEquals("TYPE_INVALID",assertThrows(DomainError.class,()->ItemCommands.decimalPlaces(Map.of("decimalPlaces",bad))).code(),String.valueOf(bad));
    assertThrows(DomainError.class,()->ItemCommands.decimalPlaces(Map.of()));
  }
}
