package com.mulino;
import com.mulino.domain.evaluation.*;
import com.mulino.domain.definitions.PredicateTruth;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TypedPredicateEvaluatorTest {
 final Instant t=Instant.parse("2026-10-08T00:00:00Z");final TypedPredicateEvaluator evaluator=new TypedPredicateEvaluator();
 EvaluationFacts.Fact fact(String id,String physical,String qty,Instant from,Instant until,boolean verified){return new EvaluationFacts.Fact(id,"1","hash",id,physical,new BigDecimal(qty),"BOX",EvaluationFacts.State.KNOWN,verified,from,until,t,List.of("evidence-"+id));}
 Map<String,Object> sum(){return Map.of("operator","quantitySum","property","Receipt.quantity","minimum",Map.of("value","100","unit","BOX"),"unit","BOX","evidenceSelector","VERIFIED_DISTINCT");}
 EvaluationFacts facts(EvaluationFacts.Fact... facts){return new EvaluationFacts(Map.of("Receipt.quantity",List.of(facts)),Map.of());}
 @Test void samePhysicalReceiptAcrossSourcesIsCountedOnce(){var r=evaluator.evaluate(sum(),facts(fact("a","physical60","60",t,null,true),fact("b","physical60","60",t,null,true)),t,t);assertEquals(PredicateTruth.State.UNSATISFIED,r.truth().state());assertEquals(2,r.conditions().getFirst().inputs().size());}
 @Test void parentHundredWithOnlyNinetyIsNotFulfilled(){assertEquals(PredicateTruth.State.UNSATISFIED,evaluator.evaluate(sum(),facts(fact("a","p","90",t,null,true)),t,t).truth().state());}
 @Test void rawClaimsAndMissingDataRemainUnknown(){assertEquals(PredicateTruth.State.UNVERIFIED,evaluator.evaluate(sum(),facts(fact("a","p","100",t,null,false)),t,t).truth().state());assertEquals(PredicateTruth.State.UNVERIFIED,evaluator.evaluate(sum(),facts(),t,t).truth().state());}
 @Test void conflictDoesNotDisappearUnderAny(){var conflict=new EvaluationFacts.Fact("c","1","hash","c","p",null,"BOX",EvaluationFacts.State.CONFLICT,false,t,null,t,List.of());var node=Map.<String,Object>of("operator","any","children",List.of(sum(),Map.of("operator","exists","property","Other.present")));var f=new EvaluationFacts(Map.of("Receipt.quantity",List.of(conflict),"Other.present",List.of(fact("b","b","1",t,null,true))),Map.of());var r=evaluator.evaluate(node,f,t,t);assertEquals(PredicateTruth.State.SATISFIED,r.truth().state());assertTrue(r.truth().conflict());}
 @Test void throughputGapIsUnknownEvenWithSatisfiedEndpoints(){var predicate=Map.<String,Object>of("operator","compare","property","Receipt.quantity","minimum",Map.of("value","100","unit","BOX"),"unit","BOX");var f=facts(fact("a","a","100",t,t.plusSeconds(10),true),fact("b","b","100",t.plusSeconds(20),t.plusSeconds(30),true));assertEquals(PredicateTruth.State.UNVERIFIED,evaluator.evaluateInterval(predicate,f,t,t.plusSeconds(30),t,true).truth().state());assertEquals(PredicateTruth.State.SATISFIED,evaluator.evaluateInterval(predicate,f,t,t.plusSeconds(30),t,false).truth().state());}
 @Test void exactTimeEndpointsRespectInclusivity(){var f=new EvaluationFacts(Map.of("Receipt.at",List.of(new EvaluationFacts.Fact("a","1","h","a","a",t,null,EvaluationFacts.State.KNOWN,true,t,null,t,List.of()))),Map.of());var n=Map.<String,Object>of("operator","timeIn","property","Receipt.at","minimum",t.toString(),"maximum",t.plusSeconds(10).toString(),"timezone","Asia/Seoul","inclusiveStart",false,"inclusiveEnd",true);assertEquals(PredicateTruth.State.UNSATISFIED,evaluator.evaluate(n,f,t,t).truth().state());}
 @Test void currentStateRetiredParentAndActiveChildrenAreNotDoubleCounted(){
   var n=Map.<String,Object>of("operator","stateQuantity","property","Receipt.quantity","minimum",Map.of("value","100","unit","BOX"),"unit","BOX");
   var input=facts(fact("parent","parent","100",t.minusSeconds(100),t,true),fact("child60","child60","60",t,null,true),fact("child25","child25","25",t,null,true));
   var result=evaluator.evaluate(n,input,t,t);assertEquals(PredicateTruth.State.UNSATISFIED,result.truth().state());assertEquals(2,result.conditions().getFirst().inputs().size());
 }
 @Test void equalInRangeAndNotPreserveTypedFacts(){
   var f=facts(fact("a","a","100",t,null,true));
   for(String op:List.of("equals","in")){
     var n=new HashMap<String,Object>();n.put("operator",op);n.put("property","Receipt.quantity");n.put(op.equals("equals")?"value":"values",op.equals("equals")?Map.of("value","100","unit","BOX"):List.of(Map.of("value","100","unit","BOX")));
     assertEquals(PredicateTruth.State.SATISFIED,evaluator.evaluate(n,f,t,t).truth().state());
     assertEquals(PredicateTruth.State.UNSATISFIED,evaluator.evaluate(Map.of("operator","not","children",List.of(n)),f,t,t).truth().state());
   }
   assertEquals(PredicateTruth.State.SATISFIED,evaluator.evaluate(Map.of("operator","range","property","Receipt.quantity","minimum",Map.of("value","90","unit","BOX"),"maximum",Map.of("value","110","unit","BOX"),"unit","BOX"),f,t,t).truth().state());
 }
 @Test void cardinalityUsesDistinctCurrentTargetsAndKnowledgeBoundary(){
   var f=new EvaluationFacts(Map.of(),Map.of("locatedAt",List.of(fact("a","a","1",t,null,true),fact("b","b","1",t,null,true))));
   assertEquals(PredicateTruth.State.SATISFIED,evaluator.evaluate(Map.of("operator","cardinality","relation","locatedAt","minimum",1,"maximum",1),f,t,t).truth().state());
   assertEquals(PredicateTruth.State.UNVERIFIED,evaluator.evaluate(sum(),facts(fact("a","a","100",t,null,true)),t,t.minusSeconds(1)).truth().state());
 }

 @Test void intervalEndpointsUseExplicitGoalInclusivity(){
   var n=Map.<String,Object>of("operator","exists","property","Receipt.quantity");
   var endOnly=facts(fact("end","end","1",t.plusSeconds(10),t.plusSeconds(10).plusNanos(1),true));
   assertEquals(PredicateTruth.State.UNVERIFIED,evaluator.evaluateInterval(n,endOnly,t,t.plusSeconds(10),t,false,true,false).truth().state());
   assertEquals(PredicateTruth.State.SATISFIED,evaluator.evaluateInterval(n,endOnly,t,t.plusSeconds(10),t,false,true,true).truth().state());
   var startOnly=facts(fact("start","start","1",t,t.plusNanos(1),true));
   assertEquals(PredicateTruth.State.UNVERIFIED,evaluator.evaluateInterval(n,startOnly,t,t.plusSeconds(10),t,false,false,false).truth().state());
   assertEquals(PredicateTruth.State.SATISFIED,evaluator.evaluateInterval(n,startOnly,t,t.plusSeconds(10),t,false,true,false).truth().state());
 }

}
