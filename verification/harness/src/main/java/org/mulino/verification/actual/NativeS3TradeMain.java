package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.math.BigDecimal;
import org.mulino.verification.*;

/** Authored S3 public-HTTP script plus independent SQL; no normative full-case claim. */
public final class NativeS3TradeMain {
    private final Path root;
    private final ObjectNode actions=Json.object(),bindings=Json.object(),report=Json.object();
    private ActualAcceptanceDriver driver;
    private JsonNode actor;
    private String work;
    private int checks;
    private NativeS3TradeMain(Path root){this.root=root;}
    public static void main(String[] args)throws Exception {new NativeS3TradeMain(Path.of(System.getProperty("repo.root",".")).toAbsolutePath().normalize()).run();}
    private void run()throws Exception {
        int exit=3;report.put("recordType","S3_ACTUAL_NATIVE_TRADE_RECEIPT").put("status","NOT_RUN").put("gateComplete",false).put("fullCaseCoverageClaimed",false).put("startedAt",Instant.now().toString());report.set("actions",actions);
        try {
            driver=new ActualAcceptanceDriver(root,ActualConfiguration.environment(System.getenv()));
            String ref="verification/actual/s3/fixture.json";JsonNode fixture=Json.read(root.resolve(ref));
            var bundle=Json.object();bundle.set("fixture",fixture);bundle.set("bases",Json.array());bundle.put("fixtureHash",Json.sha256(root.resolve(ref)));
            var installed=driver.installFixture("setup",bundle);capture(installed);available(installed);bindings.setAll((ObjectNode)installed.data().path("aliasMap"));actor=fixture.path("actors").path("reader");
            clock("2026-10-07T09:00:02Z");
            var script=Json.read(root.resolve("verification/actual/s3/flow.json"));
            for(JsonNode action:script.path("actions"))execute(action);
            report.put("status","PASS").put("boundedAssertions",checks).put("fixtureHash",Json.sha256(root.resolve(ref))).put("flowHash",Json.sha256(root.resolve("verification/actual/s3/flow.json"))).put("buildCommit",ActualConfiguration.environment(System.getenv()).buildVersion()).put("limitation","Bounded S3 HTTP/JDBC assertions only; normative T13-T16/C1/C5/V6 full case coverage, paid model, regulatory and BTP acceptance remain separate");exit=0;
        }catch(Throwable failure){boolean assertion=failure instanceof AssertionError,unavailable=failure instanceof Unavailable;report.put("status",assertion?"FAIL":"NOT_RUN").put("failure",failure.getClass().getSimpleName()+": "+failure.getMessage());exit=assertion?1:unavailable?2:3;}
        finally {
            report.put("finishedAt",Instant.now().toString()).put("exitCode",exit).put("boundedAssertions",checks);
            Path out=Path.of(System.getProperty("verification.actual.output",root.resolve("verification/harness/target/evidence/actual-s3-native").toString())).toAbsolutePath().normalize();Files.createDirectories(out);Json.write(out.resolve("actual-s3-native.json"),report);
            for(JsonNode action:actions)for(JsonNode artifact:action.path("artifactRefs")){Path source=root.resolve(artifact.asText()).normalize(),target=out.resolve(artifact.asText()).normalize();if(!source.startsWith(root)||!target.startsWith(out))throw new IllegalArgumentException("Artifact escapes custody");Files.createDirectories(target.getParent());Files.copy(source,target,StandardCopyOption.REPLACE_EXISTING);}
            System.out.println(report.toPrettyString());
        }System.exit(exit);
    }
    private void execute(JsonNode a)throws Exception {
        String id=Json.required(a,"id"),type=Json.required(a,"type");
        switch(type) {
            case "clock" -> clock(a.path("instant").asText());
            case "uuid" -> bindings.put(Json.required(a,"alias"),UUID.randomUUID().toString());
            case "original" -> original(id,resolve(a.path("fixture")),resolve(a.path("binding")));
            case "command" -> {
                JsonNode request=resolve(a.path("request"));var result=driver.invoke(id,"api",a.path("actor").asText().equals("supervisor")?resolveActor("supervisor"):actor,Json.required(a,"capability"),request);capture(result);available(result);
                require(result.data().path("httpStatus").asInt()==a.path("httpStatus").asInt(200),id+" HTTP status "+result.data().path("httpStatus"));
                require(result.response().path("outcome").asText().equals(a.path("outcome").asText("APPLIED")),id+" expected business outcome "+a.path("outcome").asText("APPLIED")+" observed "+result.response());
                for(var it=a.path("bind").fields();it.hasNext();){var e=it.next();JsonNode value=result.response().at(e.getValue().asText());require(!value.isMissingNode(),id+" missing response binding "+e.getValue());bindings.set(e.getKey(),value);if(e.getKey().equals("WORK"))work=value.asText();}
                assertions(id,result.response(),a.path("assertions"));
            }
            case "observe" -> {
                var request=Json.object();request.put("profile","S3").put("asOf",a.path("asOf").asText("2026-10-07T09:00:02Z")).put("knownAt",a.path("knownAt").asText("2026-10-07T09:00:02Z"));var scope=Json.object();scope.set("organizationId",bindings.path("ORG"));request.set("scope",scope);request.set("sources",Json.parse("[\"s3\"]"));
                var result=driver.observe(id,request);capture(result);available(result);assertions(id,result.data(),a.path("assertions"));
                for(var it=a.path("bind").fields();it.hasNext();){var e=it.next();JsonNode value=result.data().at(e.getValue().asText());require(!value.isMissingNode(),id+" missing SQL binding "+e.getValue());bindings.set(e.getKey(),value);}
            }
            default -> throw new IllegalArgumentException("Unsupported authored S3 action "+type);
        }
    }
    private JsonNode resolveActor(String name)throws Exception{return Json.read(root.resolve("verification/actual/s3/fixture.json")).path("actors").path(name);}
    private void assertions(String id,JsonNode data,JsonNode assertions) {
        for(JsonNode assertion:assertions) {
            JsonNode value=data.at(Json.required(assertion,"pointer"));require(!value.isMissingNode(),id+" missing observation "+assertion.path("pointer"));
            switch(Json.required(assertion,"operator")) {
                case "equals" -> require(value.equals(resolve(assertion.path("expected"))),id+" expected "+resolve(assertion.path("expected"))+" observed "+value);
                case "size" -> require(value.size()==assertion.path("expected").asInt(),id+" expected rows "+assertion.path("expected")+" observed "+value.size());
                case "sum" -> {BigDecimal sum=BigDecimal.ZERO;for(JsonNode row:value){if(assertion.has("where")&&!matches(row,resolve(assertion.path("where"))))continue;JsonNode quantity=row.path(Json.required(assertion,"column"));require(quantity.isTextual()||quantity.isNumber(),id+" missing/non-numeric amount "+row);sum=sum.add(new BigDecimal(quantity.asText()));}require(sum.compareTo(new BigDecimal(assertion.path("expected").asText()))==0,id+" expected sum "+assertion.path("expected")+" observed "+sum);}
                case "matchingRows" -> {int count=0;for(JsonNode row:value)if(matches(row,resolve(assertion.path("where"))))count++;require(count==assertion.path("expected").asInt(),id+" expected matching rows "+assertion.path("expected")+" observed "+count);}
                default -> throw new IllegalArgumentException("Unsupported independent assertion operator");
            }
        }
    }
    private boolean matches(JsonNode row,JsonNode expected){for(var it=expected.fields();it.hasNext();){var e=it.next();if(!row.path(e.getKey()).equals(e.getValue()))return false;}return true;}
    private void original(String id,JsonNode fixture,JsonNode binding)throws Exception {
        var bundle=Json.object();bundle.put("fixturePhase","S3_ORIGINAL").put("fixtureHash",java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(fixture.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8))));bundle.set("fixture",fixture);bundle.set("binding",binding);
        var result=driver.installFixture(id,bundle);capture(result);available(result);require(result.data().path("sourceContentSha256").equals(result.data().path("blobReadbackSha256")),"Original blob custody mismatch");
        for(var it=result.data().path("aliasMap").fields();it.hasNext();){var e=it.next();bindings.set(id+"."+e.getKey(),e.getValue());}bindings.set(id+".hash",result.data().path("sourceContentSha256"));
    }
    private JsonNode resolve(JsonNode node){if(node.isTextual()&&node.asText().startsWith("$")){String key=node.asText().substring(1);if(!bindings.has(key))throw new IllegalArgumentException("Unbound action alias "+key);return bindings.path(key).deepCopy();}if(node.isArray()){var result=Json.array();for(JsonNode item:node)result.add(resolve(item));return result;}if(node.isObject()){var result=Json.object();node.fields().forEachRemaining(e->result.set(e.getKey(),resolve(e.getValue())));return result;}return node.deepCopy();}
    private void clock(String instant){var control=Json.object();control.put("type","clock").put("operation","advanceTo");var parameters=Json.object();parameters.put("instant",instant);control.set("parameters",parameters);var result=driver.control("clock-"+actions.size(),control);capture(result);available(result);}
    private void capture(StepResult result){actions.set(result.actionId(),result.toJson());}
    private static void available(StepResult result){if(result.driverStatus()!=StepResult.DriverStatus.EXECUTED)throw new Unavailable(result.reason());}
    private void require(boolean condition,String message){if(!condition)throw new AssertionError(message);checks++;}
    private static final class Unavailable extends RuntimeException {Unavailable(String message){super(message);}}
}
