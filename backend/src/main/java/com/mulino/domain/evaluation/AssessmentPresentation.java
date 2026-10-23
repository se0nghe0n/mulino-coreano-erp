package com.mulino.domain.evaluation;

import java.util.*;
import static com.mulino.domain.evaluation.EvaluationFacts.*;

/** Stored authored condition result plus lossless internal AST diagnostics. Never re-evaluates a goal. */
public final class AssessmentPresentation {
 private AssessmentPresentation(){}
 public static List<Map<String,Object>> authored(String name,TypedPredicateEvaluator.Result result){
  if(name==null||name.isBlank())return List.of();
  var root=result.conditions().stream().filter(c->c.conditionId().equals("root")||c.conditionId().equals(name)).toList();
  if(root.size()!=1)return List.of();
  var inputs=result.conditions().stream().flatMap(c->c.inputs().stream()).distinct().toList();
  var evidence=result.conditions().stream().flatMap(c->c.evidenceRefs().stream()).distinct().sorted().toList();
  var row=new LinkedHashMap<String,Object>();row.put("conditionId",name);row.put("truth",root.getFirst().truth());row.put("result",root.getFirst().truth().state().name());row.put("conflict",root.getFirst().truth().conflict());row.put("inputs",inputs);row.put("evidenceRefs",evidence);row.put("evaluatorVersion",root.getFirst().evaluatorVersion());row.put("diagnostics",result.conditions());
  row.put("quantityKnowledge",inputs.stream().anyMatch(f->f.state()==State.CONFLICT)?"CONFLICT":!inputs.isEmpty()&&inputs.stream().allMatch(f->f.state()==State.KNOWN&&f.verified())?"KNOWN":"UNKNOWN");
  return List.of(row);
 }
 public static Optional<Map<String,Object>> quantity(Map<String,Object> predicate,TypedPredicateEvaluator.Result result,String mode){
  if(!Set.of("CUMULATIVE_EVENT","STATE_AT").contains(mode))return Optional.empty();
  var leaves=new LinkedHashMap<String,Map<String,Object>>();leaves(predicate,"root",leaves);
  String property=null,operator=null,unit=null;java.math.BigDecimal total=null;var ids=new ArrayList<String>();
  for(var entry:leaves.entrySet()){
   var node=entry.getValue();String op=Objects.toString(node.get("operator"));
   if(!Set.of("quantitySum","stateQuantity").contains(op))continue;
   if(node.get("unit")==null||node.get("property")==null)return Optional.empty();
   var matches=result.conditions().stream().filter(c->c.conditionId().equals(entry.getKey())).toList();
   if(matches.size()!=1||matches.getFirst().truth().conflict())return Optional.empty();
   var q=TypedPredicateEvaluator.quantity(matches.getFirst().inputs(),node.get("unit"),op.equals("quantitySum"));
   if(q.uncertain()||q.conflict())return Optional.empty();
   if(total!=null&&(!property.equals(node.get("property"))||!operator.equals(op)||!unit.equals(node.get("unit"))||total.compareTo(q.total())!=0))return Optional.empty();
   property=node.get("property").toString();operator=op;unit=node.get("unit").toString();total=q.total();ids.add(entry.getKey());
  }
  if(total==null)return Optional.empty();
  return Optional.of(Map.of("evaluatedQuantity",total.stripTrailingZeros().toPlainString(),"unit",unit,"property",property,"operator",operator,"conditionIds",ids));
 }
 private static void leaves(Map<String,Object> n,String id,Map<String,Map<String,Object>> out){
  if(n.get("children") instanceof List<?> children){for(int i=0;i<children.size();i++)leaves((Map<String,Object>)children.get(i),id+"."+i,out);}else out.put(id,n);
 }
}
