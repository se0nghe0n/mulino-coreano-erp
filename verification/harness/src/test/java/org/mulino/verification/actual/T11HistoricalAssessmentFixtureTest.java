package org.mulino.verification.actual;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mulino.verification.Json;

class T11HistoricalAssessmentFixtureTest {
 @Test void incompleteFutureOrRelabeledHistoryIsRejectedBeforeInstallation()throws Exception {
  var f=(ObjectNode)Json.read(Path.of(System.getProperty("repo.root"),"verification/cases/T11/fixtures/native-r2/closed-followup.json"));
  T11HistoricalAssessmentFixture.validate(f);
  for(Consumer<ObjectNode> mutate:java.util.List.<Consumer<ObjectNode>>of(
    x->x.withObject("baseline").withObject("historicalGoalCompletion").remove("originalGoalModeProvided"),
    x->((ObjectNode)x.path("baseline").path("assessments").get(0)).put("evaluatedAt","2026-10-08T00:00:00Z"),
    x->x.withObject("baseline").withObject("historicalGoalCompletion").put("assessmentAlias","NEW_ASSESSMENT"),
    x->x.withObject("baseline").withObject("historicalGoalCompletion").withObject("goal").put("targetQuantity","99"),
    x->x.withObject("baseline").withObject("historicalGoalCompletion").put("historicalEvaluationExecuted",true))) {
   var changed=f.deepCopy();mutate.accept(changed);assertThrows(UnsupportedOperationException.class,()->T11HistoricalAssessmentFixture.validate(changed));
  }
 }
}
