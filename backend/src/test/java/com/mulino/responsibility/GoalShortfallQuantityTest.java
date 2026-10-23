package com.mulino.responsibility;

import static org.junit.jupiter.api.Assertions.*;
import com.mulino.domain.evaluation.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Shared evaluator proof keeps duplicate scopes and unknown sources out of deficit arithmetic. */
class GoalShortfallQuantityTest {
 private static final Instant NOW=Instant.parse("2026-10-07T02:00:00Z");
 private EvaluationFacts.Fact fact(String source,String scope,String value,String unit,boolean verified){return new EvaluationFacts.Fact(source,"1","hash",source,scope,new BigDecimal(value),unit,EvaluationFacts.State.KNOWN,verified,NOW,null,NOW,List.of(source));}
 @Test void onePhysicalNinetyAcrossTwoSourcesIsNinety(){var q=TypedPredicateEvaluator.quantity(List.of(fact("a","physical","90","BOX",true),fact("b","physical","90","BOX",true)),"BOX",true);assertFalse(q.uncertain());assertEquals(new BigDecimal("90"),q.total());}
 @Test void noFactsAndUnverifiedFactsAreNotAZeroShortfallProof(){assertTrue(TypedPredicateEvaluator.quantity(List.of(),"BOX",true).uncertain());assertTrue(TypedPredicateEvaluator.quantity(List.of(fact("a","physical","90","BOX",false)),"BOX",true).uncertain());}
 @Test void conflictingScopeNeverProducesUsableQuantity(){var q=TypedPredicateEvaluator.quantity(List.of(fact("a","physical","90","BOX",true),fact("b","physical","80","BOX",true)),"BOX",true);assertTrue(q.conflict());assertTrue(q.uncertain());}
 @Test void missingPhysicalIdentityAndWrongUnitStayUnknown(){assertTrue(TypedPredicateEvaluator.quantity(List.of(fact("a",null,"90","BOX",true)),"BOX",true).uncertain());assertTrue(TypedPredicateEvaluator.quantity(List.of(fact("a","physical","90","KG",true)),"BOX",true).uncertain());}
}
