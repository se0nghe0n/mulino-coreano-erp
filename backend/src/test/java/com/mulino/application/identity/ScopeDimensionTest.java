package com.mulino.application.identity;

import com.mulino.application.core.DomainError;
import com.mulino.domain.identity.ScopeDimension;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScopeDimensionTest {
  final String id=UUID.randomUUID().toString();
  @Test void typedVocabularyRequiresCanonicalUuidExceptOpaqueSource() {
    assertEquals(Set.of("ORGANIZATION","TARGET","WORK","ITEM","PLACE","SOURCE","LOT","CUSTOMER","SUPPLIER"),
        Arrays.stream(ScopeDimension.values()).map(Enum::name).collect(java.util.stream.Collectors.toSet()));
    for(var d:ScopeDimension.values()) {
      assertSame(d,ScopeDimension.require(d.name(),id));
      if(d!=ScopeDimension.SOURCE)for(String malformed:List.of("supplier-alias","1-1-1-1-1"," ",""))
        assertEquals("TYPE_INVALID",assertThrows(DomainError.class,()->ScopeDimension.require(d.name(),malformed)).code());
    }
    String namespace="이탈리아/warehouse.receipt.v1";
    assertSame(ScopeDimension.SOURCE,ScopeDimension.require("SOURCE",namespace));
    assertSame(ScopeDimension.SOURCE,ScopeDimension.require("SOURCE","😀".repeat(36)));
    assertThrows(DomainError.class,()->ScopeDimension.require("SOURCE","😀".repeat(37)));
    for(String kind:List.of("SUPPLIERS","supplier","SEGMENT","UNKNOWN"))assertThrows(DomainError.class,()->ScopeDimension.require(kind,id));
  }
  @Test void targetValidationNeverTreatsUnknownOrForeignOrganizationAsWildcard() {
    assertFalse(ScopeDimension.validTargets(Map.of("UNKNOWN",List.of(id)),id));
    assertFalse(ScopeDimension.validTargets(Map.of("ORGANIZATION",List.of(UUID.randomUUID().toString())),id));
    assertFalse(ScopeDimension.validTargets(Map.of("SUPPLIER",List.of("alias")),id));
    assertFalse(ScopeDimension.validTargets(Map.of("ITEM",List.of()),id));
    assertFalse(ScopeDimension.validTargets(Map.of(),id));
  }
}
