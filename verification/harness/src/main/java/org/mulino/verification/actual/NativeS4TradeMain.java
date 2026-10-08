package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.math.BigDecimal;
import org.mulino.verification.*;

/** Authored S4 public-HTTP script plus independent SQL; no normative full-case claim. */
public final class NativeS4TradeMain {
    private final Path root;
    private final ObjectNode actions=Json.object(),bindings=Json.object(),report=Json.object();
    private ActualAcceptanceDriver driver;
    private JsonNode actor,currentFixture;
    private String externalOrganization;
    private String work;
    private int checks;
    /** Authored oracle assertions only; checks also counts HTTP/outcome/binding plumbing. */
    private int oracleAssertions;
    /** Actions whose authored assertions all executed and held. */
    private final Set<String> asserted=new TreeSet<>();
    private NativeS4TradeMain(Path root){this.root=root;}
    public static void main(String[] args)throws Exception {new NativeS4TradeMain(Path.of(System.getProperty("repo.root",".")).toAbsolutePath().normalize()).run();}
    private void run()throws Exception {
        int exit=3;report.put("recordType","S4_ACTUAL_NATIVE_TRADE_RECEIPT").put("status","NOT_RUN").put("gateComplete",false).put("fullCaseCoverageClaimed",false).put("startedAt",Instant.now().toString());report.set("actions",actions);
        try {
            String flowRef=System.getProperty("verification.actual.flowRef","verification/actual/s4/flow.json");
            // Authoring defects stop before any fixture or product effect exists.
            var unbound=NativeS4FlowAliases.check(root,flowRef);if(!unbound.isEmpty())throw new IllegalArgumentException("Authored flow has unbound references: "+unbound);
            driver=new ActualAcceptanceDriver(root,ActualConfiguration.environment(System.getenv()));
            String ref="verification/actual/s4/fixture.json";JsonNode fixture=Json.read(root.resolve(ref));currentFixture=fixture;
            var installed=install("setup",ref,fixture);bindings.setAll((ObjectNode)installed.data().path("aliasMap"));actor=resolveActor("reader");
            clock("2026-10-07T09:00:02Z");
            var script=Json.read(root.resolve(flowRef));report.put("flowRef",flowRef);
            for(JsonNode action:script.path("actions"))execute(action);
            report.set("contractCases",contractCoverage(script.path("requiredCases")));
            report.put("status","PASS").put("boundedAssertions",checks).put("fixtureHash",Json.sha256(root.resolve(ref))).put("flowHash",Json.sha256(root.resolve(flowRef))).put("buildCommit",ActualConfiguration.environment(System.getenv()).buildVersion()).put("limitation","Bounded S4 HTTP/JDBC assertions only; normative T17-T19/C1/C4/E1/E2 full case coverage, paid model, regulatory and BTP acceptance remain separate");exit=0;
        }catch(Throwable failure){boolean assertion=failure instanceof AssertionError,unavailable=failure instanceof Unavailable;report.put("status",assertion?"FAIL":"NOT_RUN").put("failure",failure.getClass().getSimpleName()+": "+failure.getMessage());exit=assertion?1:unavailable?2:3;}
        finally {
            report.put("finishedAt",Instant.now().toString()).put("exitCode",exit).put("boundedAssertions",checks).put("authoredOracleAssertions",oracleAssertions);
            Path out=Path.of(System.getProperty("verification.actual.output",root.resolve("verification/harness/target/evidence/actual-s4-native").toString())).toAbsolutePath().normalize();Files.createDirectories(out);Json.write(out.resolve("actual-s4-native.json"),report);
            for(JsonNode action:actions)for(JsonNode artifact:action.path("artifactRefs")){Path source=root.resolve(artifact.asText()).normalize(),target=out.resolve(artifact.asText()).normalize();if(!source.startsWith(root)||!target.startsWith(out))throw new IllegalArgumentException("Artifact escapes custody");Files.createDirectories(target.getParent());Files.copy(source,target,StandardCopyOption.REPLACE_EXISTING);}
            System.out.println(report.toPrettyString());
        }System.exit(exit);
    }
    private void execute(JsonNode a)throws Exception {
        String id=Json.required(a,"id"),type=Json.required(a,"type");
        executeAction(a,id,type);
        if(Set.of("command","query","observe").contains(type)&&a.path("assertions").size()>0)asserted.add(id);
    }
    /**
     * acceptance-contract.json names, per case key, the authored actions that
     * observe it. A flow claiming a case must have asserted every mapped
     * action; unmapped keys are reported NOT_RUN rather than silently passed.
     */
    private ObjectNode contractCoverage(JsonNode requiredCases)throws java.io.IOException {
        var contract=Json.read(root.resolve("verification/actual/s4/acceptance-contract.json")).path("assertionMap");var result=Json.object();var required=new HashSet<String>();for(JsonNode c:requiredCases)required.add(c.asText());
        for(var cases=contract.fields();cases.hasNext();){var c=cases.next();if(c.getKey().startsWith("_"))continue;var keys=Json.object();
            for(var it=c.getValue().fields();it.hasNext();){var e=it.next();var entry=Json.object();entry.set("actions",e.getValue());boolean all=e.getValue().size()>0;for(JsonNode action:e.getValue())all&=asserted.contains(action.asText());
                String status=e.getValue().isEmpty()?"UNMAPPED":all?"ASSERTED":"NOT_RUN";entry.put("status",status);keys.set(e.getKey(),entry);
                if(required.contains(c.getKey())&&status.equals("NOT_RUN"))require(false,"Flow requires "+c.getKey()+" but did not assert contract key "+e.getKey()+" via "+e.getValue());}
            result.set(c.getKey(),keys);}
        return result;
    }
    private void executeAction(JsonNode a,String id,String type)throws Exception {
        switch(type) {
            case "include" -> {for(JsonNode nested:Json.read(root.resolve(Json.required(a,"scriptRef"))).path("actions"))execute(nested);}
            case "require-contract" -> throw new Unavailable(a.path("reason").asText());
            // A setup is an isolation boundary: a suite flow cannot read a previous organization's aliases.
            case "setup" -> {
                String ref=Json.required(a,"fixtureRef");var fixture=Json.read(root.resolve(ref));currentFixture=fixture;var result=install(id,ref,fixture);bindings.removeAll();work=null;bindings.setAll((ObjectNode)result.data().path("aliasMap"));if(a.has("organizationAlias"))bindings.set("ORG",result.data().path("aliasMap").path(Json.required(a,"organizationAlias")));actor=resolveActor("reader");
            }
            case "clock" -> clock(a.path("instant").asText());
            case "uuid" -> bindings.put(Json.required(a,"alias"),UUID.randomUUID().toString());
            case "original" -> original(id,resolve(a.path("fixture")),resolve(a.path("binding")));
            case "command" -> {
                JsonNode request=resolve(a.path("request"));var result=driver.invoke(id,"api",a.hasNonNull("actor")?resolveActor(a.path("actor").asText()):actor,Json.required(a,"capability"),request);capture(result);available(result);
                require(result.data().path("httpStatus").asInt()==a.path("httpStatus").asInt(200),id+" HTTP status "+result.data().path("httpStatus"));
                require(result.response().path("outcome").asText().equals(a.path("outcome").asText("APPLIED")),id+" expected business outcome "+a.path("outcome").asText("APPLIED")+" observed "+result.response());
                for(var it=a.path("bind").fields();it.hasNext();){var e=it.next();JsonNode value=result.response().at(e.getValue().asText());require(!value.isMissingNode(),id+" missing response binding "+e.getValue());bindings.set(e.getKey(),value);if(e.getKey().equals("WORK"))work=value.asText();}
                assertions(id,result.response(),a.path("assertions"));
            }
            case "lost-response" -> {
                var config=ActualConfiguration.environment(System.getenv());JsonNode request=resolve(a.path("request"));JsonNode proof;
                try(var proxy=new S3ResponseLossProxy(config.baseUri())) {
                    var lossy=new ActualAcceptanceDriver(root,new ActualConfiguration(proxy.uri(),config.jdbcUrl(),config.username(),config.password(),config.signingKey(),config.issuer(),config.audience(),config.buildVersion()));boolean responseLost=false;
                    try{lossy.invoke(id,"api",actor,Json.required(a,"capability"),request);}catch(IllegalStateException expected){responseLost=true;}
                    require(responseLost,id+" client must lose its actual HTTP response");proof=proxy.receipt();require(proof.path("upstreamHttpStatus").asInt()==200&&proof.path("response").path("outcome").asText().equals("APPLIED"),id+" upstream must commit APPLIED before response loss");
                }
                String ref="verification/harness/target/evidence/actual/response-loss-"+UUID.randomUUID()+".json";Json.write(root.resolve(ref),proof);var control=Json.object();control.put("driverStatus","EXECUTED").put("source","REAL_LOOPBACK_RESPONSE_LOSS_PROXY");control.set("response",proof);control.set("artifactRefs",Json.MAPPER.valueToTree(List.of(ref)));actions.set(id,control);
                for(var it=a.path("bind").fields();it.hasNext();){var e=it.next();JsonNode value=proof.path("response").at(e.getValue().asText());require(!value.isMissingNode(),id+" missing upstream binding");bindings.set(e.getKey(),value);}
            }
            case "parallel" -> {
                JsonNode request=resolve(a.path("request"));var first=driver.start(id+"-start1","api",actor,Json.required(a,"capability"),request);var second=driver.start(id+"-start2","api",actor,Json.required(a,"capability"),request);capture(first);capture(second);available(first);available(second);
                var r1=driver.await(id+"-result1",first.data().path("invocationHandle"),30);var r2=driver.await(id+"-result2",second.data().path("invocationHandle"),30);capture(r1);capture(r2);available(r1);available(r2);require(r1.response().path("outcome").asText().equals("APPLIED")&&r2.response().path("outcome").asText().equals("APPLIED"),id+" retries must return APPLIED business outcome");require(r1.response().path("effects").equals(r2.response().path("effects")),id+" retry effects differ");var p1=Json.read(root.resolve(r1.artifactRefs().getFirst()));var p2=Json.read(root.resolve(r2.artifactRefs().getFirst()));require(!p1.path("credentialSha256").equals(p2.path("credentialSha256")),id+" retries must use refreshed distinct JWT credentials");
            }
            case "query" -> {
                var result=driver.query(id,"api",actor,Json.required(a,"capability"),resolve(a.path("request")));capture(result);available(result);require(result.data().path("httpStatus").asInt()==a.path("httpStatus").asInt(200),id+" query HTTP failed");assertions(id,result.response(),a.path("assertions"));
                for(var it=a.path("bind").fields();it.hasNext();){var e=it.next();JsonNode value=result.response().at(e.getValue().asText());require(!value.isMissingNode(),id+" missing query binding "+e.getValue());bindings.set(e.getKey(),value);}
            }
            case "observe" -> {
                var request=Json.object();request.put("profile","S4").put("asOf",a.path("asOf").asText("2026-10-07T09:00:02Z")).put("knownAt",a.path("knownAt").asText("2026-10-07T09:00:02Z"));var scope=Json.object();scope.set("organizationId",bindings.path("ORG"));request.set("scope",scope);request.set("sources",Json.parse("[\"s4\"]"));
                var result=driver.observe(id,request);capture(result);available(result);new ContractValidator(root).result(result,"observe");assertions(id,result.data(),a.path("assertions"));
                for(var it=a.path("bind").fields();it.hasNext();){var e=it.next();JsonNode value=result.data().at(e.getValue().asText());require(!value.isMissingNode(),id+" missing SQL binding "+e.getValue());bindings.set(e.getKey(),value);}
                for(var it=a.path("bindRows").fields();it.hasNext();){var e=it.next();var selector=resolve(e.getValue());var rows=result.data().at(Json.required(selector,"pointer"));JsonNode selected=null;for(JsonNode row:rows)if(matches(row,selector.path("where"))){require(selected==null,id+" ambiguous SQL row binding "+e.getKey());selected=row;}require(selected!=null,id+" missing SQL row binding "+e.getKey());JsonNode value=selected.path(Json.required(selector,"column"));require(!value.isMissingNode(),id+" missing SQL column "+e.getKey());bindings.set(e.getKey(),value);}
            }
            default -> throw new IllegalArgumentException("Unsupported authored S4 action "+type);
        }
    }
    /**
     * Every setup is a new organization in the same disposable database. Its
     * external alias carries the setup action id so a repeated or sibling
     * fixture can neither collide with nor authenticate into an earlier one.
     */
    private StepResult install(String id,String ref,JsonNode fixture)throws Exception {
        String orgAlias=null;for(var it=fixture.path("aliases").fields();it.hasNext();){var e=it.next();if(e.getValue().path("type").asText().equals("Organization"))orgAlias=e.getKey();}
        if(orgAlias==null)throw new IllegalArgumentException("Organization alias required");
        var bundle=Json.object();bundle.set("fixture",fixture);bundle.set("bases",Json.array());bundle.put("fixtureHash",Json.sha256(root.resolve(ref)));bundle.put("organizationExternalAlias",orgAlias+"@"+id);
        var result=driver.installFixture(id,bundle);capture(result);available(result);
        externalOrganization=result.data().path("organizationExternalAlias").asText();if(!externalOrganization.equals(orgAlias+"@"+id))throw new IllegalStateException(id+" installed organization external alias differs");
        return result;
    }
    private JsonNode resolveActor(String name){JsonNode authored=currentFixture.path("actors").path(name);if(authored.isMissingNode())throw new IllegalArgumentException("Unknown fixture actor "+name);var bound=(ObjectNode)authored.deepCopy();bound.put("organizationAlias",externalOrganization);return bound;}
    private void assertions(String id,JsonNode data,JsonNode assertions) {
        for(JsonNode assertion:assertions) {
            oracleAssertions++;
            JsonNode value=data.at(Json.required(assertion,"pointer"));require(!value.isMissingNode(),id+" missing observation "+assertion.path("pointer"));
            switch(Json.required(assertion,"operator")) {
                case "equals" -> require(value.equals(resolve(assertion.path("expected"))),id+" expected "+resolve(assertion.path("expected"))+" observed "+value);
                // Exact decimal equality: scale is presentation, value is not (plan §13 decimal strings).
                case "decimalEquals" -> {require(value.isTextual()||value.isNumber(),id+" non-decimal observation "+value);require(new BigDecimal(value.asText()).compareTo(new BigDecimal(resolve(assertion.path("expected")).asText()))==0,id+" expected decimal "+resolve(assertion.path("expected"))+" observed "+value);}
                case "size" -> require(value.size()==assertion.path("expected").asInt(),id+" expected rows "+assertion.path("expected")+" observed "+value.size());
                case "sum" -> {BigDecimal sum=BigDecimal.ZERO;for(JsonNode row:value){if(assertion.has("where")&&!matches(row,resolve(assertion.path("where"))))continue;JsonNode quantity=row.path(Json.required(assertion,"column"));require(quantity.isTextual()||quantity.isNumber(),id+" missing/non-numeric amount "+row);sum=sum.add(new BigDecimal(quantity.asText()));}require(sum.compareTo(new BigDecimal(assertion.path("expected").asText()))==0,id+" expected sum "+assertion.path("expected")+" observed "+sum);}
                // Each duty is bound to its exact Work/subject/quantity and counted exactly;
                // every other open duty of a listed kind fails (no kind-only match).
                case "humanDuties" -> {require(!assertion.has("kinds"),id+" kind-only humanDuties is not an oracle; bind each duty");S4WorldOracle.humanDuties(this::require,id,value,resolve(assertion.path("duties")),assertion.has("actors")?resolve(assertion.path("actors")):data.at("/rawRows/mulino_identity_actors"));}
                // Product rows compared field-by-field with the independent JDBC ledger rows.
                case "ledgerRows" -> S4WorldOracle.ledgerRows(this::require,id,value,resolve(assertion.path("expected")),resolve(assertion.path("where")));
                case "sameSet" -> {var expected=resolve(assertion.path("expected"));require(value.isArray()&&expected.isArray(),id+" sameSet needs arrays");require(S4WorldOracle.set(this::require,id,value).equals(S4WorldOracle.set(this::require,id,expected)),id+" expected set "+expected+" observed "+value);}
                // ownerIds of a world read: owners of the ledger Works plus owners of their open valid duties.
                case "ledgerOwners" -> S4WorldOracle.ledgerOwners(this::require,id,value,resolve(assertion.path("works")),resolve(assertion.path("obligations")),resolve(assertion.path("workIds")));
                case "matchingRows" -> {int count=0;for(JsonNode row:value)if(matches(row,resolve(assertion.path("where"))))count++;require(count==assertion.path("expected").asInt(),id+" expected matching rows "+assertion.path("expected")+" observed "+count);}
                default -> throw new IllegalArgumentException("Unsupported independent assertion operator");
            }
        }
    }
    private boolean matches(JsonNode row,JsonNode expected){return S4WorldOracle.matches(row,expected);}
    private void original(String id,JsonNode fixture,JsonNode binding)throws Exception {
        var bundle=Json.object();bundle.put("fixturePhase","S4_ORIGINAL").put("fixtureHash",java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(fixture.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8))));bundle.set("fixture",fixture);bundle.set("binding",binding);
        var result=driver.installFixture(id,bundle);capture(result);available(result);require(result.data().path("sourceContentSha256").equals(result.data().path("blobReadbackSha256")),"Original blob custody mismatch");
        for(var it=result.data().path("aliasMap").fields();it.hasNext();){var e=it.next();bindings.set(id+"."+e.getKey(),e.getValue());}bindings.set(id+".hash",result.data().path("sourceContentSha256"));
    }
    private JsonNode resolve(JsonNode node){if(node.isTextual()&&node.asText().contains("${")){String value=node.asText();for(var it=bindings.fields();it.hasNext();){var entry=it.next();value=value.replace("${"+entry.getKey()+"}",entry.getValue().asText());}if(value.contains("${"))throw new IllegalArgumentException("Unbound template "+value);return Json.MAPPER.valueToTree(value);}if(node.isTextual()&&node.asText().startsWith("$")){String key=node.asText().substring(1);if(!bindings.has(key))throw new IllegalArgumentException("Unbound action alias "+key);return bindings.path(key).deepCopy();}if(node.isArray()){var result=Json.array();for(JsonNode item:node)result.add(resolve(item));return result;}if(node.isObject()){var result=Json.object();node.fields().forEachRemaining(e->result.set(e.getKey(),resolve(e.getValue())));return result;}return node.deepCopy();}
    private void clock(String instant){var control=Json.object();control.put("type","clock").put("operation","advanceTo");var parameters=Json.object();parameters.put("instant",instant);control.set("parameters",parameters);var result=driver.control("clock-"+actions.size(),control);capture(result);available(result);}
    private void capture(StepResult result){actions.set(result.actionId(),result.toJson());}
    private static void available(StepResult result){if(result.driverStatus()!=StepResult.DriverStatus.EXECUTED)throw new Unavailable(result.reason());}
    private void require(boolean condition,String message){if(!condition)throw new AssertionError(message);checks++;}
    private static final class Unavailable extends RuntimeException {Unavailable(String message){super(message);}}
}
