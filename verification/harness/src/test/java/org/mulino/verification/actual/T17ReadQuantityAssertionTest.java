package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mulino.verification.*;
import static org.junit.jupiter.api.Assertions.*;

/** Deliberate bad observations verify that zero and missing/type errors cannot be conflated. */
class T17ReadQuantityAssertionTest {
 final Path root=Path.of(System.getProperty("repo.root"));
 final ObjectNode aliases=Json.object().put("A","physical-a").put("U","physical-u").put("ORG","org").put("P","item");
 JsonNode assertion(String subcase,String id)throws Exception{for(var sub:Json.read(root.resolve("verification/cases/T17/case.json")).path("subcases"))if(subcase.equals(sub.path("id").asText()))for(var a:sub.path("assertions"))if(id.equals(a.path("id").asText()))return a;throw new AssertionError(id);}
 ObjectNode response(JsonNode data){var out=Json.object().put("driverStatus","EXECUTED");out.set("provenance",Json.object().put("scopeComplete",true));out.set("response",Json.object().set("data",data));return out;}
 ObjectNode detail(){return Json.object().put("segmentId","physical-a").put("unit","BOX").put("eligibilityStatus","ALLOWED").put("eligibleQuantity","60").put("unreservedEligibleQuantity","0");}
 void sum(String id,JsonNode rows)throws Exception{new AssertionEngine().check(assertion("reserve-pick-dispatch",id),Map.of("reserved",response(Json.object().set("segmentEligibility",rows))),aliases);}
 @Test void reservationDoesNotShrinkEligibilityAndPhysicalQuantityCannotReplaceAvailability()throws Exception{
  sum("eligible-after-reserve-4",Json.array().add(detail()));sum("unreserved-after-reserve-6",Json.array().add(detail()));
  var wrong=detail();wrong.put("unreservedEligibleQuantity","60");assertThrows(AssertionError.class,()->sum("unreserved-after-reserve-6",Json.array().add(wrong)));
  var missing=detail();missing.remove("unreservedEligibleQuantity");assertThrows(AssertionError.class,()->sum("unreserved-after-reserve-6",Json.array().add(missing)));
  var nil=detail();nil.putNull("unreservedEligibleQuantity");assertThrows(AssertionError.class,()->sum("unreserved-after-reserve-6",Json.array().add(nil)));
  var wrongUnit=detail();wrongUnit.put("unit","EA");assertThrows(AssertionError.class,()->sum("eligible-after-reserve-4",Json.array().add(wrongUnit)));
  assertThrows(AssertionError.class,()->sum("eligible-after-reserve-4",Json.array().add(detail()).add(detail())));
 }
 @Test void unknownCandidateCannotBecomeAllowedAndMissingStatusIsNotAZero()throws Exception{
  var a=assertion("eligibility-conflict","eligibility-response-3");var row=Json.object().put("segmentId","physical-u").put("eligibilityStatus","UNKNOWN");
  new AssertionEngine().check(a,Map.of("after-reserve",response(Json.object().set("segmentEligibility",Json.array().add(row)))),aliases);
  row.put("eligibilityStatus","ALLOWED");assertThrows(AssertionError.class,()->new AssertionEngine().check(a,Map.of("after-reserve",response(Json.object().set("segmentEligibility",Json.array().add(row)))),aliases));
  row.remove("eligibilityStatus");assertThrows(AssertionError.class,()->new AssertionEngine().check(a,Map.of("after-reserve",response(Json.object().set("segmentEligibility",Json.array().add(row)))),aliases));
 }
 @Test void denialIsTheExactPrerequisiteStatusAndNotAQuantityOrAuthorizationBoolean()throws Exception{
  var a=assertion("claim-nopriordispatch","no-prior-dispatch-24");
  var check=Json.object().put("status","DENIED");var result=response(Json.object().set("priorDispatchCheck",check));
  new AssertionEngine().check(a,Map.of("read-quantity-prior-dispatch",result),aliases);
  for(JsonNode invalid:List.of(Json.MAPPER.valueToTree(false),Json.MAPPER.valueToTree("0"),Json.MAPPER.valueToTree("UNKNOWN"),Json.MAPPER.nullNode())){
   check.set("status",invalid);assertThrows(AssertionError.class,()->new AssertionEngine().check(a,Map.of("read-quantity-prior-dispatch",result),aliases));
  }
 }
}
