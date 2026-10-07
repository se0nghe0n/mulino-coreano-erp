package com.mulino.inventory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.mulino.application.core.*;
import com.mulino.application.inventory.*;
import com.mulino.domain.inventory.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Binding targets come from exact slots and scoped physical rows, never authorization scopes. */
class InventorySubjectBindingTest {
 private static String id(int value){return "00000000-0000-0000-0000-"+String.format("%012d",value);}
 private final DomainContext context=new DomainContext(id(1),id(2),id(2),Instant.EPOCH,Instant.EPOCH);
 private final InventoryRepository repository=mock(InventoryRepository.class);
 private final InventoryCommands inventory=new InventoryCommands(repository,mock(StockPrimitives.class),mock(ReadAuthorizer.class),mock(InventoryRestrictionGuard.class));
 private final ItemCommands metadata=new ItemCommands(repository);
 private final CommandPreparation preparation=CommandPreparation.ordinary(Map.of("TARGET",List.of(id(99)),"ITEM",List.of(id(98)),"PLACE",List.of(id(97))),List.of(),"SPLIT",id(99),0);
 private Map<String,Object> intent(String capability,Map<String,Object> slots){return Map.of("capabilityId",capability,"slots",slots);}
 @Test void eachStockCapabilityBindsActualSegmentAndItsActualItem(){
  when(repository.current(context,"QuantitySegments",id(10))).thenReturn(Map.of("ID",id(10),"itemId",id(20)));
  for(String capability:List.of("splitQuantity","moveQuantity","recordStocktake","adjustQuantity","disposeQuantity")) {
   var bindings=inventory.subjectBindings(context,intent(capability,Map.of("segmentId",id(10))),preparation);
   assertEquals(List.of(SubjectBinding.optional("QuantitySegment",Set.of(id(10))),SubjectBinding.optional("TradeItem",Set.of(id(20)))),bindings);
   assertTrue(bindings.stream().allMatch(b->b.minimumCount()==0));
   assertTrue(bindings.stream().noneMatch(b->b.targetIds().contains(id(99))||b.targetIds().contains(id(98))||b.targetIds().contains(id(97))));
  }
 }
 @Test void mergeBindsBothSourcesAndTheirConcreteItemWithoutDestination(){
  when(repository.current(context,"QuantitySegments",id(10))).thenReturn(Map.of("ID",id(10),"itemId",id(20)));
  when(repository.current(context,"QuantitySegments",id(11))).thenReturn(Map.of("ID",id(11),"itemId",id(20)));
  var bindings=inventory.subjectBindings(context,intent("mergeQuantity",Map.of("segmentIds",List.of(id(10),id(11)),"destinationId",id(99))),preparation);
  assertEquals(Set.of(id(10),id(11)),bindings.getFirst().targetIds());assertEquals(2,bindings.getFirst().maximumCount());
  assertEquals(Set.of(id(20)),bindings.getLast().targetIds());assertEquals("TradeItem",bindings.getLast().nounType());
 }
 @Test void creationHasNoExistingSubjectAndExternalLinkUsesOnlyActualItem(){
  assertEquals(List.of(),metadata.subjectBindings(context,intent("registerItem",Map.of()),preparation));
  verifyNoInteractions(repository);
  when(repository.current(context,"TradeItems",id(20))).thenReturn(Map.of("ID",id(20)));
  assertEquals(List.of(SubjectBinding.optional("TradeItem",Set.of(id(20)))),metadata.subjectBindings(context,intent("linkExternalId",Map.of("itemId",id(20))),preparation));
  verify(repository).current(context,"TradeItems",id(20));
 }
 @Test void unavailableScopedPhysicalTargetCannotAcquireBindings(){
  when(repository.current(context,"QuantitySegments",id(10))).thenThrow(DomainError.forbidden());
  var denied=assertThrows(DomainError.class,()->inventory.subjectBindings(context,intent("splitQuantity",Map.of("segmentId",id(10))),preparation));
  assertEquals("FORBIDDEN",denied.code());
 }
}
