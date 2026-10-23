package org.mulino.verification;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ObservedNullFilterTest {
    final AssertionEngine engine=new AssertionEngine();
    Map<String,JsonNode> result(String rows) {
        return Map.of("db",Json.parse("{\"driverStatus\":\"EXECUTED\",\"provenance\":{\"scopeComplete\":true},\"data\":{\"rows\":"+rows+"}}"));
    }
    JsonNode count(String where,int expected) {
        return Json.parse("{\"op\":\"count\",\"source\":{\"actionId\":\"db\",\"pointer\":\"/data/rows\",\"where\":"+where+"},\"expected\":"+expected+"}");
    }
    @Test void observedNullCloseReasonDoesNotMatchFulfilledAndDoesNotChangeItsSource() {
        var data=result("[{\"state\":\"ACTIVE\",\"closeReason\":null}]");var original=data.get("db").deepCopy();
        engine.check(count("{\"state\":\"CLOSED\",\"closeReason\":\"FULFILLED\"}",0),data);
        assertEquals(original,data.get("db"));
    }
    @Test void explicitlyObservedNullCanBeSelectedAsNull() {
        engine.check(count("{\"closeReason\":null}",1),result("[{\"closeReason\":null}]"));
    }
    @Test void aClosedFulfilledWorkStillViolatesTheZeroCount() {
        assertThrows(AssertionError.class,()->engine.check(count("{\"state\":\"CLOSED\",\"closeReason\":\"FULFILLED\"}",0),result("[{\"state\":\"CLOSED\",\"closeReason\":\"FULFILLED\"}]")));
    }
    @Test void missingColumnStillFailsEvenWhenAnotherFilterWouldExcludeTheRow() {
        assertThrows(AssertionError.class,()->engine.check(count("{\"state\":\"CLOSED\",\"closeReason\":\"FULFILLED\"}",0),result("[{\"state\":\"ACTIVE\"}]")));
    }
    @Test void nullStillCannotBeProjectedAsKnownBusinessQuantity() {
        var assertion=Json.parse("{\"op\":\"sumEquals\",\"source\":{\"actionId\":\"db\",\"pointer\":\"/data/rows\",\"field\":\"quantity\"},\"expected\":\"0\"}");
        assertThrows(AssertionError.class,()->engine.check(assertion,result("[{\"quantity\":null}]")));
    }
    @Test void unavailableObservationStillCannotBecomeZeroEffectPass() {
        assertThrows(AssertionError.class,()->engine.check(count("{\"closeReason\":null}",0),Map.of("db",StepResult.missing("db","unavailable").toJson())));
    }
}
