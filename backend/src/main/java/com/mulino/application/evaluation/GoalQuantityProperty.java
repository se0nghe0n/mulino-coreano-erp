package com.mulino.application.evaluation;
import com.mulino.application.core.DomainError;
import com.mulino.domain.definitions.Definition;
import java.util.*;
/** Target quantities bind to the selected published predicate, not all nouns in a definition. */
public final class GoalQuantityProperty {
 private GoalQuantityProperty(){}
 public static String resolve(Definition definition,Map<String,Object> predicate,String unit){
  var references=new TreeSet<String>();collect(predicate,references);
  var quantities=definition.attributes().stream().filter(a->Set.of("quantity","stateQuantity").contains(a.name())&&a.type()==Definition.ValueType.DECIMAL&&Objects.equals(a.unit(),unit)).map(a->a.nounType()+"."+a.name()).toList();
  if(!references.isEmpty()){
   if(references.size()!=1||!quantities.contains(references.first()))throw held();
   return references.first();
  }
  if(quantities.size()!=1)throw held();return quantities.getFirst();
 }
 private static void collect(Map<?,?> node,Set<String> properties){
  if(Set.of("quantitySum","stateQuantity").contains(node.get("operator"))){if(!(node.get("property") instanceof String p))throw held();properties.add(p);}
  if(node.get("children") instanceof List<?> children)for(Object child:children)if(child instanceof Map<?,?> map)collect(map,properties);else throw held();
 }
 private static DomainError held(){return new DomainError("HELD","VERSION_UNSUPPORTED","Unique typed quantity property in pinned goal required");}
}
