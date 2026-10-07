package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.mulino.verification.*;

/** Bounded native read acceptance. This does not award any full T/C/V/E case coverage. */
public final class NativeS1ReadMain {
    private static int assertions;
    public static void main(String[] args) throws Exception {
        Path root=Path.of(System.getProperty("repo.root",".")).toAbsolutePath().normalize();
        var report=Json.object();report.put("recordType","S1_ACTUAL_NATIVE_READ_RECEIPT").put("startedAt",Instant.now().toString()).put("status","NOT_RUN").put("gateComplete",false).put("fullCaseCoverageClaimed",false);
        var steps=Json.object();report.set("actions",steps);int exit=3;
        try {
            var config=ActualConfiguration.environment(System.getenv());var driver=new ActualAcceptanceDriver(root,config);
            var validator=new ContractValidator(root);String ref="verification/actual/s1/fixture.json";var fixture=validator.fixture(ref);
            var bundle=Json.object();bundle.set("fixture",fixture);bundle.put("fixtureHash",Json.sha256(root.resolve(ref)));bundle.set("bases",Json.array());
            var installed=driver.installFixture("setup",bundle);steps.set("setup",installed.toJson());require(installed.driverStatus()==StepResult.DriverStatus.EXECUTED,"Fixture was not installed");validator.result(installed,"installFixture");
            var aliases=installed.data().path("aliasMap");var scope=Json.object();scope.put("organizationId",aliases.path("ORG").asText()).put("itemId",aliases.path("P").asText()).put("lotId",aliases.path("L").asText());
            var request=Json.object();request.set("scope",scope);request.put("asOf",fixture.path("clock").path("asOf").asText()).put("knownAt",fixture.path("clock").path("knownAt").asText());
            var actor=fixture.path("actors").path("reader");
            var inventory=driver.query("inventory","api",actor,"getInventory",request);steps.set("inventory",inventory.toJson());validator.result(inventory,"query");
            var nounRequest=request.deepCopy();nounRequest.put("id",aliases.path("P").asText());nounRequest.put("snapshotRef",Json.required(inventory.response(),"snapshotRevision"));
            var noun=driver.query("noun","api",actor,"getObject",nounRequest);steps.set("noun",noun.toJson());validator.result(noun,"query");
            require(inventory.response().path("snapshotRevision").equals(noun.response().path("snapshotRevision")),"Same world snapshot changed across physical reads");
            require(new BigDecimal(inventory.response().path("data").path("heldQuantity").asText()).compareTo(new BigDecimal("100"))==0,"Held quantity must be 100 BOX");
            require(new BigDecimal(noun.response().path("data").path("heldQuantity").asText()).compareTo(new BigDecimal("100"))==0,"Object view held quantity must be 100 BOX");
            require(inventory.response().path("data").path("eligibleQuantity").isNull(),"Missing SELL eligibility must remain unknown");
            var rawRequest=request.deepCopy();rawRequest.set("sources",Json.parse("[\"segments\"]"));
            var db=driver.observe("db-physical",rawRequest);steps.set("db-physical",db.toJson());validator.result(db,"observe");
            JsonNode rows=db.data().path("rawRows").path("segments");require(rows.size()==2,"Exactly two physical rows required");
            for(var entry:java.util.Map.of("A60","60","B40","40").entrySet()) {
                JsonNode found=null;for(JsonNode row:rows)if(row.path("id").asText().equals(aliases.path(entry.getKey()).asText()))found=row;
                require(found!=null,"Independent raw segment identity absent");require(new BigDecimal(found.path("quantity").asText()).compareTo(new BigDecimal(entry.getValue()))==0,"Independent raw quantity differs");require(found.path("unit").asText().equals("BOX"),"Independent raw unit differs");
            }
            var observation=driver.observe("unsupported-correlated-snapshot",nounRequest);steps.set("unsupported-correlated-snapshot",observation.toJson());require(observation.driverStatus()==StepResult.DriverStatus.NOT_IMPLEMENTED,"Projection hash must not become a copied MVCC snapshot");
            report.put("status","PASS").put("boundedAssertions",assertions).put("buildCommit",config.buildVersion()).put("fixtureHash",Json.sha256(root.resolve(ref)));report.put("limitation","Native physical read subset only; work lifecycle, eligibility, cumulative arrival and all full T/C/V/E cases remain unclaimed");exit=0;
        }catch(Throwable failure){report.put("status",failure instanceof AssertionError?"FAIL":"ENVIRONMENT_OR_CONTRACT_FAILURE").put("failure",failure.getClass().getSimpleName()+": "+failure.getMessage());exit=failure instanceof AssertionError?1:3;}
        finally{report.put("finishedAt",Instant.now().toString()).put("exitCode",exit);Json.write(root.resolve("verification/harness/target/evidence/actual-s1-native.json"),report);System.out.println(report.toPrettyString());}
        System.exit(exit);
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);assertions++;}
}
