package com.mulino.integration;
import static org.junit.jupiter.api.Assertions.*;
import com.mulino.application.core.DomainError;
import com.mulino.application.evaluation.GoalQuantityProperty;
import com.mulino.domain.definitions.Definition;
import java.util.*;import org.junit.jupiter.api.Test;
class S4PinnedQuantityTest {
 @Test void onePublishedDefinitionSupportsDistinctReceiptAndDeliveryGoals(){
  var d=new Definition("org","id","v1",null,"PUBLISHED","hash","core-v1","4",List.of(),List.of(attribute("Receipt"),attribute("Delivery")),List.of(),List.of(),List.of(),List.of());
  assertEquals("Receipt.quantity",GoalQuantityProperty.resolve(d,Map.of("operator","quantitySum","property","Receipt.quantity"),"BOX"));
  assertEquals("Delivery.quantity",GoalQuantityProperty.resolve(d,Map.of("operator","all","children",List.of(Map.of("operator","quantitySum","property","Delivery.quantity"))),"BOX"));
  assertThrows(DomainError.class,()->GoalQuantityProperty.resolve(d,Map.of("operator","exists","property","Receipt.quantity"),"BOX"));
  assertThrows(DomainError.class,()->GoalQuantityProperty.resolve(d,Map.of("operator","quantitySum","property","Delivery.quantity"),"EA"));
 }
 private Definition.Attribute attribute(String noun){return new Definition.Attribute(noun,"quantity",Definition.ValueType.DECIMAL,null,"BOX",0,1,1,"RECORD",false);}
}
