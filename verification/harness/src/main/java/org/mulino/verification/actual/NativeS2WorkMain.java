package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.mulino.verification.*;

/** Bounded real command/idempotency/closure probe, separate from all normative cases. */
public final class NativeS2WorkMain {
    private static int assertions;
    public static void main(String[] args) throws Exception {
        Path root=Path.of(System.getProperty("repo.root",".")).toAbsolutePath().normalize();
        var report=Json.object();var actions=Json.object();report.set("actions",actions);
        report.put("recordType","S2_ACTUAL_NATIVE_WORK_RECEIPT").put("startedAt",Instant.now().toString()).put("status","NOT_RUN").put("gateComplete",false).put("fullCaseCoverageClaimed",false);
        int exit=3;
        try {
            var config=ActualConfiguration.environment(System.getenv());var driver=new ActualAcceptanceDriver(root,config);
            var validator=new ContractValidator(root);String ref="verification/actual/s2/fixture.json";var fixture=validator.fixture(ref);
            String hash=Json.sha256(root.resolve(ref));var bundle=Json.object();bundle.set("fixture",fixture);bundle.set("bases",Json.array());bundle.put("fixtureHash",hash);
            var setup=driver.installFixture("setup",bundle);capture(actions,setup);available(setup);validator.result(setup,"installFixture");
            var aliases=setup.data().path("aliasMap");var actor=fixture.path("actors").path("reader");
            // Explicit server clock control proves the verification profile is deployed.
            var control=Json.object();control.put("type","clock").put("operation","advanceTo");var parameters=Json.object();parameters.put("instant",fixture.path("clock").path("asOf").asText());control.set("parameters",parameters);
            var clock=driver.control("server-clock",control);capture(actions,clock);available(clock);validator.result(clock,"control");
            var scope=Json.object();scope.put("organizationId",aliases.path("ORG").asText()).put("itemId",aliases.path("P").asText());
            var goal=Json.object();goal.put("quantityMode","CUMULATIVE_EVENT").put("targetQuantity","100").put("unit","BOX").put("endpoint","ARRIVED").put("timezone","Asia/Seoul").put("evidencePolicyVersion","fixture-v1").put("periodStart","2026-10-01T00:00:00Z").put("periodEnd","2026-10-31T00:00:00Z").put("eventKind","CONFIRMED_RECEIPT").put("deduplication","CANONICAL_OCCURRENCE").put("evaluatorVersion","core-v1");
            var goalScope=Json.object();goalScope.put("itemId",aliases.path("P").asText());goal.set("scope",goalScope);goal.set("contributionScope",goalScope);goal.set("conditions",Json.parse("[{\"id\":\"PINNED_GOAL\"}]"));
            var slots=Json.object();slots.put("itemId",aliases.path("P").asText()).put("definitionVersionId",aliases.path("DEF").asText()).put("kind","PURCHASE").put("ownerId",aliases.path("reader").asText()).put("supervisorId",aliases.path("supervisor").asText());slots.set("goal",goal);
            var intent=envelope("createDraft",slots,0,UUID.randomUUID().toString(),aliases.path("P").asText());
            var first=driver.start("submit-1","api",actor,"createDraft",intent);capture(actions,first);available(first);
            var second=driver.start("submit-2","api",actor,"createDraft",intent);capture(actions,second);available(second);
            var a=driver.await("result-1",first.data().path("invocationHandle"),30);var b=driver.await("result-2",second.data().path("invocationHandle"),30);capture(actions,a);capture(actions,b);available(a);available(b);validator.result(a,"await");validator.result(b,"await");
            var ar=Json.read(root.resolve(a.artifactRefs().getFirst()));var br=Json.read(root.resolve(b.artifactRefs().getFirst()));
            boolean overlap=!Instant.parse(ar.path("capturedAt").asText()).isBefore(Instant.parse(br.path("submittedAt").asText()))&&!Instant.parse(br.path("capturedAt").asText()).isBefore(Instant.parse(ar.path("submittedAt").asText()));
            report.put("requestIntervalsOverlap",overlap).put("concurrencyBarrierControlled",false);
            require(a.data().path("httpStatus").asInt()==200&&b.data().path("httpStatus").asInt()==200,"Concurrent draft requests must return successful HTTP responses");
            require(a.response().path("outcome").asText().equals("ACCEPTED"),"Draft must be accepted");
            String work=Json.required(a.response().path("effects"),"workId");require(work.equals(b.response().path("effects").path("workId").asText()),"Same stable command key created different works");
            var changed=intent.deepCopy();((ObjectNode)changed.path("slots").path("goal")).put("targetQuantity","101");
            var conflict=driver.invoke("different-payload","api",actor,"createDraft",changed);capture(actions,conflict);available(conflict);require(conflict.data().path("httpStatus").asInt()==409,"Same key with changed payload must conflict");
            var observe=Json.object();observe.set("scope",scope);observe.put("asOf",fixture.path("clock").path("asOf").asText()).put("knownAt",fixture.path("clock").path("knownAt").asText());observe.set("sources",Json.parse("[\"works\",\"goals\"]"));
            var before=driver.observe("db-before-close",observe);capture(actions,before);available(before);validator.result(before,"observe");
            var works=before.data().path("rawRows").path("works");require(works.size()==1,"Independent PostgreSQL must contain exactly one draft");require(works.get(0).path("id").asText().equals(work),"Independent work ID differs");require(works.get(0).path("status").asText().equals("DRAFT"),"New work must remain DRAFT");
            require(before.data().path("rawRows").path("goals").size()==1,"Idempotent draft must have one canonical GoalVersion");
            var cancelSlots=Json.object();cancelSlots.put("workId",work).put("reason","DRAFT_CANCELLED");
            var cancelled=driver.invoke("cancel","api",actor,"cancelDraft",envelope("cancelDraft",cancelSlots,0,UUID.randomUUID().toString(),work));capture(actions,cancelled);available(cancelled);require(cancelled.data().path("httpStatus").asInt()==200,"Draft cancellation must succeed");
            var after=driver.observe("db-after-close",observe);capture(actions,after);available(after);validator.result(after,"observe");
            var closed=after.data().path("rawRows").path("works");require(closed.size()==1,"Cancellation must preserve canonical work identity");require(closed.get(0).path("id").asText().equals(work),"Cancellation replaced work identity");require(closed.get(0).path("status").asText().equals("CLOSED"),"Cancelled draft must be CLOSED");require(closed.get(0).path("closeReason").asText().equals("CANCELLED"),"Cancellation close reason must be preserved");
            report.put("status","PASS").put("boundedAssertions",assertions).put("fixtureHash",hash).put("buildCommit",config.buildVersion()).put("limitation","Native work/idempotency/cancel subset only; no normative T/C/V/E full case PASS, fulfillment, paid model or BTP claim");exit=0;
        } catch(Throwable failure) {
            report.put("status",ActualAttemptStatus.classify(failure instanceof AssertionError,failure instanceof Unavailable,!(failure instanceof AssertionError)&&!(failure instanceof Unavailable))).put("failure",failure.getClass().getSimpleName()+": "+failure.getMessage());exit=failure instanceof AssertionError?1:failure instanceof Unavailable?2:3;
        } finally {
            report.put("finishedAt",Instant.now().toString()).put("exitCode",exit);
            Path out=Path.of(System.getProperty("verification.actual.output",root.resolve("verification/harness/target/evidence/actual-s2-native").toString())).toAbsolutePath().normalize();Files.createDirectories(out);Json.write(out.resolve("actual-s2-native.json"),report);
            for(JsonNode action:actions)for(JsonNode artifact:action.path("artifactRefs")){Path source=root.resolve(artifact.asText()).normalize();Path target=out.resolve(artifact.asText()).normalize();if(!source.startsWith(root)||!target.startsWith(out))throw new IllegalArgumentException("Artifact escapes custody");Files.createDirectories(target.getParent());Files.copy(source,target,StandardCopyOption.REPLACE_EXISTING);}
            System.out.println(report.toPrettyString());
        }
        System.exit(exit);
    }
    private static ObjectNode envelope(String capability,JsonNode slots,int revision,String key,String subject) {
        var intent=Json.object();intent.put("intentKind","COMMAND").put("definitionVersion","definition-v1").put("capabilityId",capability).put("expectedRevision",revision).put("commandIdempotencyKey",key);intent.set("slots",slots);intent.set("provenance",Json.object());var refs=Json.array();var ref=Json.object();ref.put("type",capability.equals("createDraft")?"TradeItem":"Work").put("id",subject);refs.add(ref);intent.set("subjectRefs",refs);return intent;
    }
    private static void capture(ObjectNode actions,StepResult result){actions.set(result.actionId(),result.toJson());}
    private static final class Unavailable extends RuntimeException {Unavailable(String reason){super(reason);}}
    private static void available(StepResult result){if(result.driverStatus()!=StepResult.DriverStatus.EXECUTED)throw new Unavailable(result.reason());}
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);assertions++;}
}
