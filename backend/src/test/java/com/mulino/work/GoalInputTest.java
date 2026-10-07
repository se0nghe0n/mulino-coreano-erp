package com.mulino.work;
import com.mulino.domain.work.GoalInput;
import com.mulino.application.core.DomainError;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GoalInputTest {
 @Test void missingDraftDoesNotInventModeOrQuantity(){var draft=new LinkedHashMap<String,Object>();GoalInput.validate(draft,false);assertTrue(draft.isEmpty());assertEquals("NEEDS_INPUT",assertThrows(DomainError.class,()->GoalInput.validate(draft,true)).outcome());}
 @Test void numericGoalRequiresDecimalStringAndUnit(){assertThrows(DomainError.class,()->GoalInput.validate(Map.of("quantityMode","STATE_AT","targetQuantity",100.0,"unit","BOX"),false));assertThrows(DomainError.class,()->GoalInput.validate(Map.of("targetQuantity","-1","unit","BOX"),false));}
 @Test void observationGapCannotBeActivatedWithoutPolicy(){var goal=new LinkedHashMap<String,Object>();goal.put("dueAt","2026-10-02T00:00:00Z");goal.putAll(Map.of("quantityMode","THROUGHOUT","endpoint","MAINTAINED","scope",Map.of(),"timezone","Asia/Seoul","evidencePolicyVersion","fixture-v1","conditions",List.of(Map.of("id","safe")),"evaluatorVersion","core-v1","periodStart","2026-10-01T00:00:00Z","periodEnd","2026-10-02T00:00:00Z"));assertEquals("NEEDS_INPUT",assertThrows(DomainError.class,()->GoalInput.validate(goal,true)).outcome());goal.put("observationPolicy","COMPLETE_COVERAGE");assertDoesNotThrow(()->GoalInput.validate(goal,true));}
 @Test void invalidTimeBoundsAndUnknownTimezonesReject(){assertThrows(DomainError.class,()->GoalInput.validate(Map.of("periodStart","2026-10-02T00:00:00Z","periodEnd","2026-10-01T00:00:00Z"),false));assertThrows(DomainError.class,()->GoalInput.validate(Map.of("timezone","Moon/Base"),false));}
}
