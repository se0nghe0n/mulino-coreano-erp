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
            var validator=new ContractValidator(root);String ref="verification/actual/s1/fixture.json";var template=validator.fixture(ref);Instant authorityWallClock=Instant.now();var fixture=NativeFixtureAuthority.current(template,authorityWallClock);
            String effectiveHash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(fixture.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            report.put("authorityWallClock",authorityWallClock.toString()).put("effectiveFixtureHash",effectiveHash).put("fixtureTemplateHash",Json.sha256(root.resolve(ref)));
            report.set("authorityValidity",fixture.path("actors").path("reader").path("grant").deepCopy());
            var bundle=Json.object();bundle.set("fixture",fixture);bundle.put("fixtureHash",effectiveHash);bundle.set("bases",Json.array());
            var installed=driver.installFixture("setup",bundle);steps.set("setup",installed.toJson());available(installed);validator.result(installed,"installFixture");
            var aliases=installed.data().path("aliasMap");var scope=Json.object();scope.put("organizationId",aliases.path("ORG").asText()).put("itemId",aliases.path("P").asText()).put("lotId",aliases.path("L").asText());
            var request=Json.object();request.set("scope",scope);request.put("asOf",fixture.path("clock").path("asOf").asText()).put("knownAt",fixture.path("clock").path("knownAt").asText());
            var actor=fixture.path("actors").path("reader");
            var inventory=driver.query("inventory","api",actor,"getInventory",request);steps.set("inventory",inventory.toJson());available(inventory);validator.result(inventory,"query");require(inventory.data().path("httpStatus").asInt()==200,"Inventory HTTP must succeed");
            var nounRequest=request.deepCopy();nounRequest.put("id",aliases.path("P").asText());nounRequest.put("snapshotRef",Json.required(inventory.response(),"snapshotRevision"));
            var noun=driver.query("noun","api",actor,"getObject",nounRequest);steps.set("noun",noun.toJson());available(noun);validator.result(noun,"query");require(noun.data().path("httpStatus").asInt()==200,"Object HTTP must succeed");
            require(inventory.response().path("snapshotRevision").equals(noun.response().path("snapshotRevision")),"Same world snapshot changed across physical reads");
            require(noun.response().path("data").path("productId").asText().equals(aliases.path("PRD").asText()),"Trade item product dependency differs");
            for(var versionEntry:java.util.Map.of("specificationVersion","SPEC","packagingVersion","PACK").entrySet()) {
                var actual=noun.response().path("data").path(versionEntry.getKey());var expected=fixture.path("aliases").path(versionEntry.getValue());
                require(noun.response().path("data").path(versionEntry.getKey()+"Id").asText().equals(aliases.path(versionEntry.getValue()).asText()),"Trade item version dependency differs");
                require(actual.path("productId").asText().equals(aliases.path("PRD").asText()),"Version belongs to another product");
                require(actual.path("contentHash").asText().equals(FixtureInstaller.contentHash(expected.path("content"))),"Persisted version content hash differs");
            }
            require(new BigDecimal(inventory.response().path("data").path("heldQuantity").asText()).compareTo(new BigDecimal("100"))==0,"Held quantity must be 100 BOX");
            require(new BigDecimal(noun.response().path("data").path("heldQuantity").asText()).compareTo(new BigDecimal("100"))==0,"Object view held quantity must be 100 BOX");
            require(inventory.response().path("data").path("eligibleQuantity").isNull(),"Missing SELL eligibility must remain unknown");
            var rawRequest=request.deepCopy();rawRequest.set("sources",Json.parse("[\"segments\"]"));
            var db=driver.observe("db-physical",rawRequest);steps.set("db-physical",db.toJson());available(db);validator.result(db,"observe");
            JsonNode rows=db.data().path("rawRows").path("segments");require(rows.size()==2,"Exactly two physical rows required");
            for(var entry:java.util.Map.of("A60","60","B40","40").entrySet()) {
                JsonNode found=null;for(JsonNode row:rows)if(row.path("id").asText().equals(aliases.path(entry.getKey()).asText()))found=row;
                require(found!=null,"Independent raw segment identity absent");require(new BigDecimal(found.path("quantity").asText()).compareTo(new BigDecimal(entry.getValue()))==0,"Independent raw quantity differs");require(found.path("unit").asText().equals("BOX"),"Independent raw unit differs");
            }
            var observation=driver.observe("unsupported-correlated-snapshot",nounRequest);steps.set("unsupported-correlated-snapshot",observation.toJson());require(observation.driverStatus()==StepResult.DriverStatus.NOT_IMPLEMENTED,"Projection hash must not become a copied MVCC snapshot");
            report.put("status","PASS").put("boundedAssertions",assertions).put("buildCommit",config.buildVersion()).put("fixtureHash",effectiveHash);report.put("limitation","Native physical read subset only; work lifecycle, eligibility, cumulative arrival and all full T/C/V/E cases remain unclaimed");exit=0;
        }catch(Throwable failure){report.put("status",ActualAttemptStatus.classify(failure instanceof AssertionError,failure instanceof Unavailable,!(failure instanceof AssertionError)&&!(failure instanceof Unavailable))).put("failure",failure.getClass().getSimpleName()+": "+failure.getMessage());exit=failure instanceof AssertionError?1:failure instanceof Unavailable?2:3;}
        finally{report.put("finishedAt",Instant.now().toString()).put("exitCode",exit);Json.write(root.resolve("verification/harness/target/evidence/actual-s1-native.json"),report);
            String output=System.getProperty("verification.actual.output");
            if(output!=null) {
                Path destination=Path.of(output);Json.write(destination.resolve("actual-s1-native.json"),report);
                for(JsonNode action:steps)for(JsonNode artifact:action.path("artifactRefs")) {
                    String artifactRef=artifact.asText();Path source=validatorPath(root,artifactRef);Path copy=destination.resolve(artifactRef).normalize();
                    if(!copy.startsWith(destination.toAbsolutePath().normalize()))throw new IllegalArgumentException("Artifact escapes evidence directory");
                    Files.createDirectories(copy.getParent());Files.copy(source,copy,StandardCopyOption.REPLACE_EXISTING);
                }
            }
            System.out.println(report.toPrettyString());}
        System.exit(exit);
    }
    private static Path validatorPath(Path root,String ref) {Path p=root.resolve(ref).normalize();if(!p.startsWith(root))throw new IllegalArgumentException("Artifact escapes repository");return p;}
    private static final class Unavailable extends RuntimeException {Unavailable(String message){super(message);}}
    private static void available(StepResult result){if(result.driverStatus()!=StepResult.DriverStatus.EXECUTED)throw new Unavailable(result.reason());}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);assertions++;}
}
