package com.mulino.application.evaluation;
import static org.junit.jupiter.api.Assertions.*;
import com.mulino.domain.evaluation.*;
import com.mulino.domain.evaluation.EvaluationFacts.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class AssessmentPresentationTest {
 final Instant at=Instant.parse("2026-10-07T02:00:00Z");
 Fact fact(String scope,String amount,State state){return new Fact(scope,"1","hash",scope,scope,amount,"BOX",state,state==State.KNOWN,at,null,at,List.of(scope));}
 Map<String,Object> leaf(String property){return Map.of("operator","quantitySum","property",property,"unit","BOX","minimum",Map.of("value","100","unit","BOX"));}
 @Test void authoredRootKeepsEveryAstNodeAndActualUnknownInputs(){
  var facts=new EvaluationFacts(Map.of("Receipt.quantity",List.of(fact("q","98",State.KNOWN))),Map.of());
  var result=new TypedPredicateEvaluator().evaluate(Map.of("operator","all","children",List.of(leaf("Receipt.quantity"),leaf("Missing.quantity"))),facts,at,at);
  var named=AssessmentPresentation.authored("arrived-at-W",result);assertEquals(1,named.size());assertEquals("arrived-at-W",named.getFirst().get("conditionId"));assertEquals(result.conditions(),named.getFirst().get("diagnostics"));
  assertEquals(result.truth(),named.getFirst().get("truth"));assertEquals(3,result.conditions().size());
 }
 @Test void evaluatedQuantityIsActualSumNotTargetAndUnknownOrCompetingQuantitiesAreAbsent(){
  var evaluator=new TypedPredicateEvaluator();var facts=new EvaluationFacts(Map.of("Receipt.quantity",List.of(fact("a","60",State.KNOWN),fact("b","40",State.KNOWN))),Map.of());
  var predicate=leaf("Receipt.quantity");var proof=AssessmentPresentation.quantity(predicate,evaluator.evaluate(predicate,facts,at,at),"CUMULATIVE_EVENT").orElseThrow();assertEquals("100",proof.get("evaluatedQuantity"));
  var ninetyEight=new EvaluationFacts(Map.of("Receipt.quantity",List.of(fact("a","98",State.KNOWN))),Map.of());assertEquals("98",AssessmentPresentation.quantity(predicate,evaluator.evaluate(predicate,ninetyEight,at,at),"CUMULATIVE_EVENT").orElseThrow().get("evaluatedQuantity"));
  assertTrue(AssessmentPresentation.quantity(predicate,evaluator.evaluate(predicate,new EvaluationFacts(Map.of(),Map.of()),at,at),"CUMULATIVE_EVENT").isEmpty());
  var mixed=Map.<String,Object>of("operator","all","children",List.of(predicate,leaf("Other.quantity")));assertTrue(AssessmentPresentation.quantity(mixed,evaluator.evaluate(mixed,facts,at,at),"CUMULATIVE_EVENT").isEmpty());
  assertTrue(AssessmentPresentation.quantity(predicate,evaluator.evaluate(predicate,facts,at,at),"THROUGHOUT").isEmpty());
 }
}
