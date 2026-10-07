package com.mulino.returns;
import com.mulino.application.trade.returns.ReturnCommands;
import com.mulino.domain.inventory.PhysicalRanges;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ReturnRangeContractTest {
 private Map<String,Object> range(String start,String quantity){return Map.of("startQuantity",new BigDecimal(start),"quantity",new BigDecimal(quantity));}
 @Test void duplicatePhysicalRangeIgnoresDocumentAndOriginKeys(){assertTrue(ReturnCommands.overlap(range("0","20"),range("10","20")));assertFalse(ReturnCommands.overlap(range("0","20"),range("20","10")));assertTrue(ReturnCommands.overlap(range("0.0","20.00"),range("0","20")));}
 @Test void physicalProjectionPreservesActualReturnTenInsideOriginalDeliveryThirty(){var edges=List.<Map<String,Object>>of(Map.of("sourceId","delivery","targetId","customer","sourceStartQuantity",BigDecimal.ZERO,"targetStartQuantity",BigDecimal.ZERO,"quantity",new BigDecimal("30"),"uncertain",false));var result=PhysicalRanges.project(edges,"delivery","customer",new BigDecimal("20"),new BigDecimal("10"));assertEquals(1,result.size());assertEquals(new BigDecimal("20"),result.getFirst().start());assertEquals(new BigDecimal("30"),result.getFirst().end());assertTrue(PhysicalRanges.project(edges,"other-origin","customer",BigDecimal.ZERO,new BigDecimal("10")).isEmpty());}
 @Test void uncertainAncestryCannotCreateCleanReturnScope(){var edge=Map.<String,Object>of("sourceId","delivery","targetId","customer","sourceStartQuantity",BigDecimal.ZERO,"targetStartQuantity",BigDecimal.ZERO,"quantity",new BigDecimal("100"),"uncertain",true);assertTrue(PhysicalRanges.project(List.of(edge),"delivery","customer",BigDecimal.ZERO,new BigDecimal("20")).isEmpty());}
}
