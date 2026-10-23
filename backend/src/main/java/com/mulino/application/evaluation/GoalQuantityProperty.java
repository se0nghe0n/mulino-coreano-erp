package com.mulino.application.evaluation;
import com.mulino.application.core.DomainError;
import com.mulino.domain.definitions.Definition;
import java.util.*;
/** Target quantities bind to the selected published predicate, not all nouns in a definition. */
public final class GoalQuantityProperty {
 private GoalQuantityProperty(){}
 public static String resolve(Definition definition,Map<String,Object> predicate,String unit){
  var properties=resolveAll(definition,predicate,unit);
  if(properties.size()!=1)throw held();return properties.getFirst();
 }
 /** Each explicit quantity predicate in a stock interval constrains the same requested target. */
 public static List<String> resolveAll(Definition definition,Map<String,Object> predicate,String unit){
  var references=new TreeSet<String>();collect(predicate,references);
  var quantities=definition.attributes().stream().filter(a->Set.of("quantity","stateQuantity","observedQuantity").contains(a.name())&&a.type()==Definition.ValueType.DECIMAL&&Objects.equals(a.unit(),unit)).map(a->a.nounType()+"."+a.name()).toList();
  if(!references.isEmpty()){
   if(!quantities.containsAll(references))throw held();
   return List.copyOf(references);
  }
  if(quantities.size()!=1)throw held();return List.copyOf(quantities);
 }
 public static String operator(Map<String,Object> predicate,String property,String mode){
  var operators=new HashSet<String>();collectOperators(predicate,property,operators);
  if(operators.size()>1)throw held();
  return operators.isEmpty()?("CUMULATIVE_EVENT".equals(mode)?"quantitySum":"stateQuantity"):operators.iterator().next();
 }
 private static void collectOperators(Map<?,?> node,String property,Set<String> operators){
  if(property.equals(node.get("property"))&&Set.of("quantitySum","stateQuantity").contains(node.get("operator")))operators.add(node.get("operator").toString());
  if(node.get("children") instanceof List<?> children)for(Object child:children)if(child instanceof Map<?,?> map)collectOperators(map,property,operators);
 }
 private static void collect(Map<?,?> node,Set<String> properties){
  if(Set.of("quantitySum","stateQuantity").contains(node.get("operator"))){if(!(node.get("property") instanceof String p))throw held();properties.add(p);}
  if(node.get("children") instanceof List<?> children)for(Object child:children)if(child instanceof Map<?,?> map)collect(map,properties);else throw held();
 }
 private static DomainError held(){return new DomainError("HELD","VERSION_UNSUPPORTED","Unique typed quantity property in pinned goal required");}
}
