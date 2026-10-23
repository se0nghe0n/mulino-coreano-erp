package com.mulino.application.inventory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.mulino.application.core.*;
import com.mulino.application.trade.sales.SalesQueries;
import com.mulino.domain.inventory.InventoryRepository;
import com.mulino.domain.trade.purchase.PurchaseRepository;
import com.mulino.domain.trade.sales.SalesRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PublicWorldReadTest {
 @ParameterizedTest @ValueSource(strings={"missing-edge","wrong-unit"})
 void incompletePhysicalBindingNeverBecomesAConfirmedZero(String defect){
  var inventory=mock(InventoryRepository.class);var sales=mock(SalesRepository.class);var purchase=mock(PurchaseRepository.class);var lines=mock(SalesQueries.class);var auth=mock(ReadAuthorizer.class);
  Instant at=Instant.parse("2026-10-08T00:00:00Z");var c=new DomainContext("org","actor","actor",at,at);
  when(inventory.rows(eq(c),anyString())).thenReturn(List.of());when(sales.rows(eq(c),anyString())).thenReturn(List.of());when(purchase.rows(eq(c),anyString())).thenReturn(List.of());
  when(auth.permittedScopes(eq(c),anyString(),anyMap())).thenReturn(true);
  var root=new LinkedHashMap<String,Object>(Map.of("ID","root","itemId","item","quantity",new BigDecimal("30"),"unit","BOX","placeId","warehouse","validFrom",at.minusSeconds(1),"retiredAt",at,"retirementRecordedAt",at));
  var leaf=new LinkedHashMap<String,Object>(Map.of("ID","leaf","itemId","item","quantity",new BigDecimal("30"),"unit",defect.equals("wrong-unit")?"EA":"BOX","placeId","transit","validFrom",at));
  when(inventory.rows(c,"QuantitySegments")).thenReturn(List.of(root,leaf));when(inventory.rows(c,"Places")).thenReturn(List.of(Map.of("ID","warehouse","kind","INTERNAL_STORAGE"),Map.of("ID","transit","kind","TRANSIT")));
  var allocation=Map.<String,Object>of("ID","allocation","segmentId","root","workId","work","orderLineId","line","quantity",new BigDecimal("30"),"unit","BOX","state","CONSUMED","startQuantity",BigDecimal.ZERO);
  when(inventory.rows(c,"SegmentAllocations")).thenReturn(List.of(allocation));
  var dispatch=Map.<String,Object>of("ID","dispatch","itemId","item","workId","work","salesLineId","line","rangeRootId","root","transitSegmentId","leaf","startQuantity",BigDecimal.ZERO,"quantity",new BigDecimal("30"),"unit","BOX","commandId","command");
  when(inventory.rows(c,"Dispatches")).thenReturn(List.of(dispatch));
  if(defect.equals("wrong-unit"))when(inventory.rows(c,"GenealogyEdges")).thenReturn(List.of(Map.of("ID","edge","sourceId","root","targetId","leaf","sourceStartQuantity",BigDecimal.ZERO,"targetStartQuantity",BigDecimal.ZERO,"quantity",new BigDecimal("30"))));
  when(inventory.object(c,"TradeItems","item")).thenReturn(Map.of("baseUnit","BOX"));
  when(lines.worldLines(eq(c),anyMap())).thenReturn(new SalesQueries.WorldLines(List.of(Map.of("ID","line","workId","work","deliveryEndpoint","DELIVERED")),true));
  var result=new PublicWorldRead(inventory,sales,purchase,lines,auth).read(c,Map.of("itemId","item","salesLineId","line"),Map.of("workReferences",List.of(Map.of("ID","work")),"assessmentReferences",List.of()),at);
  assertFalse(result.containsKey("inTransitQuantity"));assertTrue(result.get("publicReadGaps").toString().contains("SCOPED_TRANSIT_RANGE_UNAVAILABLE"));
  assertEquals(defect.equals("wrong-unit")?"EA":"BOX",leaf.get("unit"));
 }
}
