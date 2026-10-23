package org.mulino.verification.actual;
import org.junit.jupiter.api.Test;
import org.mulino.verification.Json;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class T17SourceFactsTest {
 private ObjectNode fixture()throws Exception{return (ObjectNode)Json.read(Path.of(System.getProperty("repo.root"),"verification/cases/T17/fixtures/normal-consumed-delivery.json"));}
 @Test void originalGivenSourcesValidateWithoutAssertingBusinessSuccess()throws Exception {
  try(var files=Files.list(Path.of(System.getProperty("repo.root"),"verification/cases/T17/fixtures"))){for(Path file:files.filter(p->p.toString().endsWith(".json")).toList())T17SourceFacts.validate(Json.read(file));}
 }
 @Test void quantityOutsidePhysicalGivenIsRejected()throws Exception {var f=fixture();((ObjectNode)f.path("baseline").path("conditions").get(0)).put("quantity","31");assertThrows(IllegalArgumentException.class,()->T17SourceFacts.validate(f));}
 @Test void missingScopeAndFutureSourceAreRejected()throws Exception {var f=fixture();((ObjectNode)f.path("baseline").path("t17SourceFacts").path("workAliases")).remove("A");assertThrows(IllegalArgumentException.class,()->T17SourceFacts.validate(f));var g=fixture();((ObjectNode)g.path("baseline").path("conditions").get(0)).put("validFrom","2026-10-08T00:00:00Z");assertThrows(IllegalArgumentException.class,()->T17SourceFacts.validate(g));}
 @Test void syntheticEvidenceCannotClaimLegalApproval()throws Exception {var f=fixture();((ObjectNode)f.path("baseline").path("t17SourceFacts")).put("legalComplianceClaimed",true);assertThrows(IllegalArgumentException.class,()->T17SourceFacts.validate(f));}
 @Test void unknownAndConflictRemainDistinctInputs()throws Exception {var f=fixture();((ObjectNode)f.path("baseline").path("conditions").get(0)).put("status","UNKNOWN");T17SourceFacts.validate(f);((ObjectNode)f.path("baseline").path("conditions").get(0)).put("status","CONFLICT");T17SourceFacts.validate(f);}
}
