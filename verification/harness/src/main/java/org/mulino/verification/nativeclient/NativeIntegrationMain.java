package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.node.*;
import org.mulino.verification.*;
import org.mulino.verification.actual.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** No code path invokes Codex, starts a model turn, or spends provider API credits. */
public final class NativeIntegrationMain {
    public static void main(String[] args) throws Exception {
        Path root=Path.of(System.getProperty("repo.root",".")).toAbsolutePath().normalize();
        String mode=args.length==0?"readiness":args[0];
        if(mode.equals("readiness")) {
            var report=new NativeProductIntegration(root,System.getenv()).readiness();
            Path path=root.resolve("verification/harness/target/native-integration-readiness.json");Json.write(path,report);
            System.out.println(report.toPrettyString());System.exit(2);
        }
        if(!mode.equals("transport-sit"))throw new IllegalArgumentException("readiness|transport-sit only; model execution is coordinator-owned");
        var evidence=new NativeIntegrationEvidence(root);
        var report=evidence.base("ACTUAL_PRODUCT_TRANSPORT_SIT",Json.object()).put("runtimeStatus","NOT_RUN")
            .put("modelBindingAcceptance","NOT_RUN").put("actualModelCalls",0).put("incrementalApiCostCap","0");
        var artifacts=Json.array();report.set("artifacts",artifacts);
        int exit=1;
        try {
            var config=ActualConfiguration.environment(System.getenv());
            Path source=root.resolve("verification/actual/s1/fixture.json");
            var fixture=NativeFixtureAuthority.current(Json.read(source),Instant.now());
            // Existing strict S1 fixture is a transport fixture, never presented as an M01-M60 installation.
            var bundle=Json.object().put("fixtureHash",NativeClientPolicy.sha(fixture.toString()))
                .put("organizationExternalAlias","native-transport-"+UUID.randomUUID());
            bundle.set("fixture",fixture);bundle.set("bases",Json.array());
            var integration=new NativeProductIntegration(root,System.getenv());
            var installedStep=integration.driver().installFixture("install",bundle);
            check(installedStep.driverStatus()==StepResult.DriverStatus.EXECUTED,"STRICT_FIXTURE_INSTALL_FAILED");
            var installed=installedStep.data();
            var aliases=installed.path("aliasMap");var scope=Json.object().put("organizationId",aliases.path("ORG").asText())
                .put("installationId",installed.path("installationId").asText()).put("caseId","NATIVE_TRANSPORT_SIT").put("turnId","read-1");
            var installation=evidence.base("ACTUAL_FIXTURE_INSTALL",scope);
            installation.set("fixtureSource",evidence.descriptor(source));installation.set("effectiveFixture",fixture);installation.set("result",installedStep.toJson());
            artifacts.add(evidence.save("installation",installation));
            report.set("scope",scope);report.put("buildCommit",config.buildVersion());
            var actor=(ObjectNode)fixture.path("actors").path("reader").deepCopy();actor.put("organizationAlias",installed.path("organizationExternalAlias").asText());
            var transport=new NativeMcpTransport(config,evidence);
            var listed=transport.send("discovery","tools/list",null,Json.object(),actor,scope);artifacts.add(listed.artifact());
            check(!transport.tools(listed).isEmpty(),"PUBLIC_TOOLS_MISSING");
            var query=Json.object().put("asOf",fixture.path("clock").path("asOf").asText()).put("knownAt",fixture.path("clock").path("knownAt").asText());
            query.set("scope",Json.object().put("organizationId",aliases.path("ORG").asText()).put("itemId",aliases.path("P").asText()).put("lotId",aliases.path("L").asText()));
            var observer=new NativePhysicalObserver(config,evidence);
            var before=observer.capture(scope,query.path("asOf").asText(),query.path("knownAt").asText());
            artifacts.add(evidence.save("postgresql-before",before));
            var read=transport.send("inventory","tools/call","getInventory",query,actor,scope);artifacts.add(read.artifact());
            check(read.status()==200&&!read.response().path("result").path("isError").asBoolean(),"INVENTORY_READ_FAILED");
            check(new BigDecimal(read.businessResponse().path("data").path("heldQuantity").asText()).compareTo(new BigDecimal("100"))==0,"PHYSICAL_FIXTURE_QUANTITY_MISMATCH");
            check(NativeMcpTransport.nativeReply(read).path("success").asBoolean(),"NATIVE_TOOL_RESPONSE_FAILED");
            var contextRequest=query.deepCopy().put("view","ACTOR_PERMITTED_BUSINESS_CONTEXT");
            var unsupported=transport.send("context-contract","tools/call","getObject",contextRequest,actor,scope);artifacts.add(unsupported.artifact());
            check(unsupported.response().path("result").path("isError").asBoolean(),"CONTEXT_CONTRACT_UNEXPECTEDLY_ACCEPTED");
            var unknown=actor.deepCopy().put("subject","not-installed-"+UUID.randomUUID());
            var rejected=transport.send("unknown-principal","tools/call","getInventory",query,unknown,scope);artifacts.add(rejected.artifact());
            check(!NativeMcpTransport.nativeReply(rejected).path("success").asBoolean(),"UNKNOWN_PRINCIPAL_ACCEPTED");
            var snapshot=observer.capture(scope,query.path("asOf").asText(),query.path("knownAt").asText());artifacts.add(evidence.save("postgresql",snapshot));
            check(snapshot.path("rawRows").path("segments").size()==2,"RAW_SEGMENT_COUNT_MISMATCH");
            check(before.path("rawRows").equals(snapshot.path("rawRows")),"OBSERVED_PHYSICAL_ROWS_CHANGED_DURING_READS");
            check(!snapshot.path("snapshot").path("id").equals(read.businessResponse().path("snapshotRevision")),"API_HASH_REUSED_AS_MVCC_TOKEN");
            new ContractValidator(root).schema("verification/model-binding/physical-columns.schema.json",snapshot.path("physicalCells"));
            var driverContext=integration.driver().query("context-diagnostic","MCP",actor,"getObject",contextRequest);
            check(driverContext.driverStatus()==StepResult.DriverStatus.NOT_IMPLEMENTED&&!driverContext.artifactRefs().isEmpty(),"MISSING_SERVER_IDENTITY_WAS_FABRICATED");
            var observeRequest=Json.object();observeRequest.set("scope",scope);observeRequest.put("asOf",query.path("asOf").asText()).put("knownAt",query.path("knownAt").asText());
            var driverSnapshot=integration.driver().observe("snapshot-diagnostic",observeRequest);
            check(driverSnapshot.driverStatus()==StepResult.DriverStatus.NOT_IMPLEMENTED&&!driverSnapshot.artifactRefs().isEmpty(),"MISSING_FULL_OBSERVER_WAS_FABRICATED");
            for(String ref:driverContext.artifactRefs())artifacts.add(evidence.descriptor(root.resolve(ref)));
            for(String ref:driverSnapshot.artifactRefs())artifacts.add(evidence.descriptor(root.resolve(ref)));
            var modelFixture=Json.object();modelFixture.set("fixture",Json.read(root.resolve("verification/model-binding/cases/M01/fixture.json")));
            check(integration.driver().installFixture("model-fixture-refusal",modelFixture).driverStatus()==StepResult.DriverStatus.NOT_IMPLEMENTED,"INCOMPLETE_MODEL_FIXTURE_ACCEPTED");
            boolean blocked=false;try{integration.requireReady("M01","turn-1");}catch(IllegalStateException missing){blocked=true;report.put("modelGate",missing.getMessage());}
            check(blocked,"MODEL_GATE_DID_NOT_REFUSE");
            int observerChecks=NativeObserverTransportChecks.verify(config,evidence,transport,actor,aliases,scope,query.path("asOf").asText(),query.path("knownAt").asText(),snapshot,artifacts);
            report.put("runtimeStatus","TRANSPORT_SIT_PASS").put("assertions",14+observerChecks);exit=0;
        } catch(Exception|AssertionError failure) {
            report.put("runtimeStatus","TRANSPORT_SIT_FAIL").put("failureType",failure.getClass().getSimpleName());
            String message=failure.getMessage();if(message!=null&&message.matches("[A-Z0-9_]{1,100}"))report.put("failureCode",message);
        }
        report.put("exitCode",exit);artifacts.add(evidence.source("verification/harness/src/main/java/org/mulino/verification/nativeclient/NativeIntegrationMain.java"));
        Json.write(root.resolve("verification/harness/target/native-integration-sit.json"),report);
        System.out.println(report.toPrettyString());System.exit(exit);
    }
    private static void check(boolean ok,String code){NativeClientPolicy.require(ok,code);}
}
