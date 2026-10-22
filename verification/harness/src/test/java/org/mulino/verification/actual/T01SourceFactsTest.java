package org.mulino.verification.actual;
import org.junit.jupiter.api.Test;
import org.mulino.verification.Json;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
class T01SourceFactsTest {
 private ObjectNode fixture()throws Exception{return (ObjectNode)Json.read(Path.of(System.getProperty("repo.root"),"verification/cases/T01/fixture.json"));}
 @Test void declaredSourcesMatchEveryHistoricalPermission()throws Exception{T01SourceFacts.validate(fixture());}
 @Test void differingQuantityCannotMasqueradeAsConsumedBaseline()throws Exception{
  var f=fixture();((ObjectNode)f.path("baseline").path("restrictions").get(0)).put("quantity","39");assertThrows(IllegalArgumentException.class,()->T01SourceFacts.validate(f));
 }
 @Test void unavailableOriginalCannotBecomeVerified()throws Exception{
  var f=fixture();((ObjectNode)f.path("aliases").path("qc60")).remove("fixtureContent");assertThrows(IllegalArgumentException.class,()->T01SourceFacts.validate(f));
 }
 @Test void legacyUnsupportedBaselineStaysUnsupported()throws Exception{
  var f=fixture();((ObjectNode)f.path("baseline")).remove("sourceFacts");assertFalse(T01SourceFacts.consumes(f,"eligibilityBases"));assertFalse(T01SourceFacts.consumes(f,"restrictions"));
 }
}
