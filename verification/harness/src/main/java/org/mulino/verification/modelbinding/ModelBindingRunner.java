package org.mulino.verification.modelbinding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.mulino.verification.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** M60 execution wrapper. Both profiles share Gherkin/fixtures/oracle; only agent port changes. */
public final class ModelBindingRunner {
    private final BindingContract contract;
    private final AcceptanceDriver driver;
    private final AgentRunner agent;
    private final BindingEvaluator evaluator;
    private final JsonNode binding,fixture;
    private final String profile;
    private UatExecutionGate executionGate=UatExecutionGate.notAuthorized();
    private int actualInvocations=0;
    private final Map<String,String> capabilityKinds=new HashMap<>();
    public void authorizedRuntime(UatExecutionGate gate){BindingContract.require(gate!=null,"Missing explicit execution gate");executionGate=gate;}
    private final Map<String,JsonNode> results=new LinkedHashMap<>();
    private final Set<String> completed=new LinkedHashSet<>();
    private final ArrayNode turnReports=Json.array();
    private JsonNode aliasMap=Json.object();
    public ModelBindingRunner(BindingContract c,String caseId,String profile,AcceptanceDriver driver,AgentRunner agent) throws IOException {
        BindingContract.require(Set.of("SIT","UAT").contains(profile),"Unknown model binding profile");
        contract=c;binding=c.binding(caseId);fixture=c.fixture(binding);this.profile=profile;this.driver=driver;this.agent=agent;evaluator=new BindingEvaluator(c);
        for(JsonNode capability:Json.read(c.validator.path("contracts/acceptance-capabilities.json")).path("capabilities"))capabilityKinds.put(capability.path("id").asText(),capability.path("kind").asText());
        BindingContract.require(profile.equals("SIT")?agent instanceof AgentRunner.Scripted:agent instanceof AgentRunner.Actual,"SIT/UAT runner/profile mismatch");
    }
    public void execute(String step) throws IOException {
        BindingContract.require(completed.add(step),"Duplicate Gherkin step "+step);
        if(step.equals("install")) {
            ObjectNode bundle=Json.object();bundle.set("fixture",fixture);bundle.put("fixtureHash",Json.sha256(contract.validator.path(binding.path("fixtureRef").asText())));bundle.put("caseHash",Json.sha256(contract.validator.path("verification/model-binding/cases/"+binding.path("caseId").asText()+"/binding.json")));bundle.set("baseRefs",Json.array());
            save(step,driver.installFixture(step,bundle),"installFixture");
            if(executed(step)){aliasMap=results.get(step).path("data").path("aliasMap");BindingContract.require(aliasMap.isObject()&&!aliasMap.isEmpty(),"Actual fixture installation lacks aliases");for(String a:List.of("org",fixture.path("actors").path("command-actor").path("grant").path("scope").path("grantAlias").asText())) BindingContract.require(aliasMap.hasNonNull(a),"Missing actual alias "+a);}
            return;
        }
        String[] parts=step.split("/",-1);BindingContract.require(parts.length==2,"Unknown binding step");JsonNode turn=null;for(JsonNode t:binding.path("turns"))if(t.path("id").asText().equals(parts[0]))turn=t;
        BindingContract.require(turn!=null,"Unknown turn");JsonNode source=contract.sourceTurn(turn);String prefix=parts[0];
        List<String> order=List.of("context","before","agent","after","assert");int ix=order.indexOf(parts[1]);BindingContract.require(ix>=0,"Unknown milestone");
        BindingContract.require(completed.contains(ix==0?"install":prefix+"/"+order.get(ix-1)),"Out-of-order milestone");
        if(parts[1].equals("assert")){evaluate(prefix,turn,source);return;}
        if(!executed("install") || (ix>0&&!executed(prefix+"/"+order.get(ix-1)))) {results.put(step,StepResult.missing(step,"NOT_IMPLEMENTED: prerequisite actual adapter/observation unavailable").toJson());return;}
        JsonNode actor=resolvedActor(parts[1].equals("agent")?turn.path("commandActorRef").asText():turn.path("readActorRef").asText());
        if(parts[1].equals("context")) {
            // Actual server authenticates/filter context. Never copy fixture or oracle into prompt.
            ObjectNode request=Json.object();request.put("view","ACTOR_PERMITTED_BUSINESS_CONTEXT");request.set("scope",scope(prefix));request.put("asOf",fixture.path("clock").path("asOf").asText());request.put("knownAt",fixture.path("clock").path("knownAt").asText());
            save(step,driver.query(step,"MCP",actor,"getObject",request),"query");
            if(executed(step)){JsonNode context=results.get(step).path("data").path("permittedContext");BindingContract.require(context.isObject(),"Authenticated business context absent");checkContext(context);}
        } else if(parts[1].equals("before")||parts[1].equals("after")) {
            String prior=prefix+(parts[1].equals("before")?"/context":"/agent");JsonNode revision=results.get(prior).path("data").path("snapshotRevision");BindingContract.require(!revision.isMissingNode()&&!revision.isNull(),"Actual source snapshot revision absent");
            ObjectNode request=Json.object();request.set("scope",scope(prefix));request.set("snapshotRef",revision);request.set("asOf",fixture.path("clock").path("asOf"));request.set("knownAt",fixture.path("clock").path("knownAt"));request.put("mappingVersion","model-binding-v1");
            save(step,driver.observe(step,request),"observe");if(executed(step))validateSnapshot(results.get(step),request);
        } else {
            ObjectNode action=Json.object();action.put("userUtterance",source.path("input").path("utterance").asText());
            ObjectNode context=results.get(prefix+"/context").path("data").path("permittedContext").deepCopy();ArrayNode prior=Json.array();
            int ordinal=Integer.parseInt(prefix.substring(5));JsonNode sourceCase=contract.sourceCase(binding);
            for(int i=0;i<ordinal-1;i++) {prior.add(sourceCase.path("turns").get(i).path("input").path("utterance"));String earlier="turn-"+(i+1)+"/agent";if(results.containsKey(earlier)&&executed(earlier)) {context.set("previousClientResult",results.get(earlier).path("response"));}}
            context.set("priorUserTurns",prior);checkContext(context);action.set("permittedContext",context);
            // Only scripted runner is given typed intent. Actual action has no expected fields.
            if(profile.equals("SIT")) {JsonNode intent=source.path("expectedIntent").deepCopy();((ObjectNode)intent).put("capabilityId",turn.path("capabilityMapping").path("public").asText());((ObjectNode)intent).set("slots",resolveSlotValues(intent.path("slots")));action.set("intent",intent);}
            if(profile.equals("UAT")) {
                // No authorization exists in Step2. Injection does not authorize paid calls.
                JsonNode authorization;try{authorization=executionGate.requireAuthorized(binding.path("caseId").asText(),prefix);}catch(IllegalStateException missing){save(step,StepResult.missing(step,"NOT_IMPLEMENTED: "+missing.getMessage()),"agent");return;}
                BindingContract.require(authorization.path("decisionRef").asText().equals("R8")&&authorization.path("status").asText().equals("AUTHORIZED"),"Missing explicit R8 authorization");
                for(String k:List.of("approvalArtifactRef","clientExactVersion","modelExactId","promptSha256","skillSha256","approvedCostCap","currency")) Json.required(authorization,k);
                contract.validator.path(authorization.path("approvalArtifactRef").asText()).toRealPath();
                actualInvocations++;save(step,agent.run(step,"MCP",actor,action,driver),"agent");
            } else save(step,agent.run(step,"MCP",actor,action,driver),"agent");
        }
    }
    private JsonNode resolveSlotValues(JsonNode slots){ObjectNode out=Json.object();slots.fields().forEachRemaining(e->{ObjectNode s=e.getValue().deepCopy();s.set("value",BindingEvaluator.aliases(s.path("value"),aliasMap));out.set(e.getKey(),s);});return out;}
    private JsonNode resolvedActor(String name){ObjectNode actor=fixture.path("actors").path(name).deepCopy();JsonNode a=actor.path("grant");ObjectNode scope=(ObjectNode)a.path("scope");scope.set("targetIds",BindingEvaluator.aliases(scope.path("targetAliases"),aliasMap));scope.set("organizationId",aliasMap.get(actor.path("organizationAlias").asText()));scope.set("grantId",aliasMap.get(scope.path("grantAlias").asText()));return actor;}
    private ObjectNode scope(String turn){ObjectNode scope=Json.object();scope.set("organizationId",aliasMap.get("org"));scope.set("installationId",results.get("install").path("data").path("installationId"));BindingContract.require(scope.hasNonNull("installationId"),"Actual installation identity missing");scope.put("caseId",binding.path("caseId").asText());scope.put("turnId",turn);return scope;}
    private void save(String step,StepResult result,String kind) throws IOException {BindingContract.require(result.actionId().equals(step),"Driver action identity mismatch");contract.validator.result(result,kind);if(result.driverStatus()==StepResult.DriverStatus.EXECUTED&&Set.of("query","agent").contains(kind)){JsonNode observed=result.provenance().path("authenticatedActor");String profile=kind.equals("query")?"read-probe-actor":"command-actor";JsonNode requested=fixture.path("actors").path(profile);for(String k:List.of("issuer","subject","audience"))BindingContract.require(observed.path(k).equals(requested.path(k)),"Actual authenticated principal mismatch "+k);BindingContract.require(observed.path("organizationId").equals(aliasMap.path("org")),"Actual authenticated organization mismatch");if(kind.equals("query")||this.profile.equals("SIT"))BindingContract.require(observed.path("grantId").equals(aliasMap.path(requested.path("grant").path("scope").path("grantAlias").asText())),"Actual authenticated grant mismatch");}results.put(step,result.toJson());}
    private boolean executed(String id){return results.containsKey(id)&&results.get(id).path("driverStatus").asText().equals("EXECUTED");}
    public static void checkContext(JsonNode n){
        if(n.isObject())n.fields().forEachRemaining(e->{BindingContract.require(!Set.of("expectedIntent","expected","oracle","oracleRef","assertions","acceptanceProposal","requirementRefs","semanticFocus","plannedFixtureAssertionLabels","corpusCasePointer","corpusTurnPointer","corpusSha256","assertionId","bindingRef","plannedCaseTitle").contains(e.getKey()),"Oracle leaked to actual prompt: "+e.getKey());checkContext(e.getValue());});else if(n.isArray())n.forEach(ModelBindingRunner::checkContext);
    }
    private void validateSnapshot(JsonNode result,JsonNode requested) throws IOException {
        JsonNode d=result.path("data");contract.validator.schema("contracts/acceptance-observation.schema.json",d);
        for(String k:List.of("scope","asOf","knownAt"))BindingContract.require(d.path(k).equals(requested.path(k)),"Independent snapshot scope/time mismatch "+k);
        BindingContract.require(d.path("snapshotRevision").equals(requested.path("snapshotRef"))&&d.path("snapshot").path("id").equals(requested.path("snapshotRef")),"Independent snapshot token mismatch");
        BindingContract.require(result.path("provenance").path("independent").asBoolean(false),"Observation not independent");
        BindingContract.require(d.path("sourceQuery").equals(result.path("provenance").path("sourceQuery"))&&d.path("snapshot").equals(result.path("provenance").path("snapshot")),"Observation provenance mismatch");
        BindingContract.require(d.path("sourceQuery").path("mappingVersion").asText().equals("model-binding-v1"),"Unknown physical row mapping");
        contract.validator.schema("verification/model-binding/physical-columns.schema.json",d.path("rawRows"));
        contract.validator.schema("verification/model-binding/observer-rows.schema.json",d.path("data"));
        BindingContract.require(d.path("data").path("effects").isArray()&&d.path("data").path("obligations").isArray(),"Effects/obligations scope unobserved");
    }
    private void evaluate(String prefix,JsonNode turn,JsonNode source) throws IOException {
        ObjectNode report=Json.object();report.put("turnId",prefix);report.set("assertionResults",Json.array());turnReports.add(report);
        if(!executed(prefix+"/after")){for(JsonNode ref:turn.path("commonAssertions")){ObjectNode a=Json.object();a.put("assertionId",ref.path("assertionId").asText()).put("semanticPath",ref.path("semanticPath").asText()).put("status","NOT_RUN").put("reason","Actual observation absent");((ArrayNode)report.path("assertionResults")).add(a);}report.put("status","NOT_RUN").put("reason","NOT_IMPLEMENTED: real response/DB/effect/host observation absent");return;}
        JsonNode exec=results.get(prefix+"/agent"),before=results.get(prefix+"/before"),after=results.get(prefix+"/after"),oracle=source.path("oracle"),path=Json.object();String selected="DIRECT";
        ArrayNode ars=(ArrayNode)report.path("assertionResults");
        try {
            JsonNode transcript=Json.object();CapturedApiObservation api;
            List<JsonNode> effects=BindingEvaluator.newEffects(before.path("data").path("data").path("effects"),after.path("data").path("data").path("effects"));
            if(profile.equals("UAT")) {
                JsonNode control=Json.parse("{\"type\":\"process\",\"operation\":\"clientProbe\",\"parameters\":{}}");((ObjectNode)control).set("parameters",scope(prefix));
                StepResult host=new StepResult(prefix+"/agent",StepResult.DriverStatus.EXECUTED,exec.path("data"),exec.path("response"),exec.path("reason").asText(),exec.path("provenance"),strings(exec.path("artifactRefs")));
                HostObservationValidator.validate(contract.validator,control,host);
                transcript=completionWitness(agentTranscript(exec),prefix);
                // §13.3: clear requests are scored as a structured-intent rate (proposal >=95%), not a per-turn
                // hard FAIL. Safety invariants below still run and stay zero-tolerance. Ambiguous/incomplete
                // input (expected NEEDS_INPUT) remains exact, because acting on it is the improper execution.
                List<String> intentMismatches=structuredIntentMismatches(source.path("expectedIntent"),transcript.path("structuredIntent"));
                report.put("intentMatch",intentMismatches.isEmpty());if(!intentMismatches.isEmpty())report.set("intentMismatches",Json.MAPPER.valueToTree(intentMismatches));
                BindingContract.require(intentMismatches.isEmpty()||source.path("expectedIntent").path("status").asText().equals("STRUCTURED"),"Ambiguous input structured differently: "+intentMismatches);
                validateUatCalls(transcript,source,turn);
                if(oracle.has("uatCompletion")){selected=evaluator.selectPath(oracle,transcript,effects,aliasMap,fixture);path=oracle.path("uatCompletion").path("pathOracles").path(selected);if(selected.equals("SERVER_REJECTION")&&path.isMissingNode())path=oracle.path("sitDirectCommand");}
            } else if(oracle.has("sitDirectCommand")){selected="SERVER_REJECTION";path=oracle.path("sitDirectCommand");}
            api=capturedApis(exec,transcript,prefix);
            api.requireExecutionCall(source.path("expectedIntent"),turn.path("capabilityMapping").path("public").asText(),selected);
            if(binding.path("caseId").asText().equals("M50")&&prefix.equals("turn-1")){JsonNode replayPayload=fixture.path("baseline").path("domainFacts").path("receipt").path("canonicalPayload");api.requireReplay(replayPayload,aliasMap,"confirmReceipt");api.requireReplayReferences(before,after,BindingEvaluator.aliases(replayPayload,aliasMap),"confirmReceipt");}
            report.put("selectedPathId",selected).put("selectedPath",selected);report.set("capturedApiCallIds",Json.MAPPER.valueToTree(api.ids()));report.set("apiAssertionSources",api.sources());
            for(JsonNode effect:effects){BindingContract.require(effect.path("organizationRef").equals(aliasMap.path("org"))&&effect.path("installationId").equals(results.get("install").path("data").path("installationId"))&&effect.path("turnId").asText().equals(prefix),"Effect delta scope mismatch");}
            if(selected.equals("EVIDENCED_PREFLIGHT_STOP")){verifyGroundedRead(completionWitness(agentTranscript(exec),prefix),after,turn,source);BindingContract.require(rowSet(before.path("data").path("rawRows")).equals(rowSet(after.path("data").path("rawRows"))),"Preflight changed observed business rows");}
            if(!selected.equals("EVIDENCED_PREFLIGHT_STOP")) {
                String cap=turn.path("capabilityMapping").path("public").asText(),original=source.path("expectedIntent").path("slots").path("originalCapability").path("value").asText();
                boolean finalOnly=source.path("expectedIntent").path("status").asText().equals("NEEDS_INPUT")||profile.equals("UAT")&&source.path("expectedIntent").path("intentKind").asText().equals("QUERY");
                JsonNode outcomeSource=finalOnly?exec.path("response"):api.commandResponse(cap,original);
                if(!(profile.equals("UAT")&&finalOnly))BindingContract.require(outcomeSource.path("outcome").asText().equals(oracle.path("outcome").asText()),"Actual captured server outcome differs");
                if(profile.equals("SIT"))BindingContract.require(exec.path("response").equals(outcomeSource),"Scripted API result differs from actual captured response");
            }
            if(profile.equals("UAT")){JsonNode semantics=finalResponseObservation(exec,transcript,prefix);report.put("finalResponseObservationRef",Json.required(exec.path("data"),"finalResponseObservationRef"));FinalResponseObservation.check(semantics,transcript.path("finalResponse"),selected,oracle.has("uatCompletion"),oracle,exec,after,effects,aliasMap,evaluator,api);}
            int ai=0;for(JsonNode a:oracle.path("assertions"))checkAssertion(a,turn.path("commonAssertions").get(ai++).path("assertionId").asText(),exec,after,effects,ars,source,api);
            String branch=profile.equals("SIT")?"SIT_DIRECT_COMMAND":selected;JsonNode refs=turn.path("oracleAssertions").path(branch);if(refs.isMissingNode())refs=turn.path("oracleAssertions").path("SIT_DIRECT_COMMAND");ai=0;for(JsonNode a:path.path("assertions"))checkAssertion(a,refs.get(ai++).path("assertionId").asText(),exec,after,effects,ars,source,api);
            evaluator.effectWhitelist(oracle,path,effects,aliasMap);evaluator.preserveObligations(before.path("data").path("data").path("obligations"),after.path("data").path("data").path("obligations"),effects);evaluator.obligations(oracle,path,after.path("data").path("data").path("obligations"),aliasMap);verifyHumanOwners(after.path("data").path("data").path("obligations"));
            // input-required is structural, not fixed response text. User answers never approve.
            JsonNode clarification=oracle.path("clarification");BindingContract.require(exec.path("response").path("clarification").path("required").asBoolean(false)==clarification.path("required").asBoolean(),"Clarification requirement differs");
            if(clarification.path("required").asBoolean()){BindingContract.require(exec.path("response").path("clarification").path("slots").equals(clarification.path("slots")),"Clarification slots differ");BindingContract.require(effects.stream().noneMatch(BindingEvaluator::business),"Write before user clarification");}
            report.put("status","PASS");
        } catch(AssertionError|IllegalArgumentException e){report.put("status","FAIL").put("reason",e.getMessage());}
    }
    private void verifyHumanOwners(JsonNode obligations){Set<JsonNode> humans=new HashSet<>();fixture.path("baseline").path("principalFacts").fields().forEachRemaining(e->{if(e.getValue().path("human").asBoolean(false)){JsonNode id=aliasMap.get(e.getKey());BindingContract.require(id!=null,"Installed human owner alias missing");humans.add(id);}});for(JsonNode duty:obligations)if(duty.path("status").asText().equals("OPEN"))BindingContract.require(humans.contains(duty.path("ownerRef")),"OPEN obligation has no existing human owner");}
    private JsonNode agentTranscript(JsonNode execution) throws IOException {
        String ref=Json.required(execution.path("data"),"agentTranscriptRef");
        JsonNode transcript=linkedArtifact(execution,ref,true);
        BindingContract.require(transcript.path("evidenceClass").asText().equals("ACTUAL_HOST"),"Captured transcript cannot establish UAT");
        BindingContract.require(transcript.has("finalResponse")&&transcript.path("finalResponse").equals(execution.path("response")),"Client final response differs from independently captured transcript");
        return transcript;
    }
    /** Only artifacts consumed by the actual independent host extractor establish UAT. */
    private JsonNode linkedArtifact(JsonNode execution,String ref,boolean host) throws IOException {
        BindingContract.require(strings(execution.path("artifactRefs")).contains(ref),"Actual response/wire artifact unlinked");
        Path file=contract.validator.path(ref);BindingContract.require(file.toRealPath().startsWith(contract.validator.root().toRealPath()),"Artifact escapes repository");
        if(!host)BindingContract.require(execution.path("provenance").path("independent").asBoolean(false)&&execution.path("provenance").path("source").asText().equals("ACTUAL_AUTHENTICATED_TRANSPORT"),"API capture is a client projection rather than independent transport observation");
        JsonNode artifacts=host?execution.path("data").path("hostObservation").path("extractor").path("inputArtifacts"):execution.path("data").path("capturedArtifacts");
        if(!host)contract.validator.schema("verification/model-binding/capture-artifacts.schema.json",artifacts);
        boolean linked=false;for(JsonNode artifact:artifacts)if(artifact.path("path").asText().equals(ref)){BindingContract.require(artifact.path("sha256").asText().equals(Json.sha256(file))&&artifact.path("sizeBytes").asLong(-1)==Files.size(file),"Captured artifact hash/bytes differ");linked=true;}
        BindingContract.require(linked,"Artifact lacks independent capture hash/bytes");return Json.read(file);
    }
    private CapturedApiObservation.Call capturedCall(JsonNode execution,JsonNode call,String prefix,boolean host) throws IOException {
        JsonNode wire=linkedArtifact(execution,Json.required(call,"wireArtifactRef"),host);
        contract.validator.schema("verification/model-binding/captured-api.schema.json",wire);
        BindingContract.require(wire.path("evidenceClass").asText().equals(host?"ACTUAL_HOST":"ACTUAL_API_CAPTURE"),"Selftest/projection cannot establish actual captured API");
        CapturedApiObservation.checkCaptureContext(wire,scope(prefix),execution.path("actionId"),host?execution.path("data").path("hostObservation").path("command"):null);
        return CapturedApiObservation.decode(call,wire);
    }
    private CapturedApiObservation capturedApis(JsonNode execution,JsonNode transcript,String prefix) throws IOException {
        JsonNode calls=profile.equals("UAT")?transcript.path("toolCalls"):execution.path("data").path("capturedApiCalls");
        BindingContract.require(calls.isArray(),"Actual API calls unobserved");List<CapturedApiObservation.Call> captured=new ArrayList<>();
        if(profile.equals("SIT")){var transport=Json.object();transport.put("terminal",true);transport.set("toolCalls",calls);JsonNode turn=null;for(JsonNode t:binding.path("turns"))if(t.path("id").asText().equals(prefix))turn=t;validateUatCalls(transport,contract.sourceTurn(turn),turn);}
        for(JsonNode call:calls)captured.add(capturedCall(execution,call,prefix,profile.equals("UAT")));
        if(profile.equals("UAT")){JsonNode context=results.get(prefix+"/context"),read=context.path("data").path("authenticatedRead");if(read.isObject())captured.add(capturedCall(context,read,prefix,false));}
        JsonNode sources=profile.equals("UAT")?transcript.path("apiAssertionSources"):execution.path("data").path("apiAssertionSources");
        return new CapturedApiObservation(captured,sources);
    }
    private JsonNode finalResponseObservation(JsonNode execution,JsonNode transcript,String prefix) throws IOException {
        JsonNode observation=linkedArtifact(execution,Json.required(execution.path("data"),"finalResponseObservationRef"),true);
        contract.validator.schema("verification/model-binding/final-response-observation.schema.json",observation);
        BindingContract.require(observation.path("evidenceClass").asText().equals("ACTUAL_HOST")&&observation.path("scope").equals(scope(prefix)),"Final-response semantics has wrong actual scope");
        BindingContract.require(observation.path("sourceTranscriptRef").asText().equals(Json.required(execution.path("data"),"agentTranscriptRef"))&&observation.path("sourceResponse").equals(transcript.path("finalResponse")),"Semantic extractor did not observe entire actual final response");
        JsonNode extractor=execution.path("data").path("hostObservation").path("extractor");
        BindingContract.require(observation.path("extractor").path("command").equals(extractor.path("command")),"Final-response semantics lacks actual independent extractor command");
        JsonNode rows=extractor.path("rawRows");
        if(rows.isMissingNode())for(JsonNode ref:extractor.path("transcriptRefs")){JsonNode candidate=linkedArtifact(execution,ref.asText(),true);if(candidate.has("operationEvidence")){rows=candidate;break;}}
        BindingContract.require(rows.path("finalResponseObservation").equals(observation),"Final-response semantics differs from independently extracted host output");
        return observation;
    }
    private void validateUatCalls(JsonNode transcript,JsonNode source,JsonNode turn){
        BindingContract.require(transcript.path("terminal").asBoolean(false)&&transcript.path("toolCalls").isArray(),"Actual client invocation not fully observed");Set<String> permittedCommands=new HashSet<>();permittedCommands.add(turn.path("capabilityMapping").path("public").asText());String original=source.path("expectedIntent").path("slots").path("originalCapability").path("value").asText();if(!original.isBlank())permittedCommands.add(original);
        for(JsonNode call:transcript.path("toolCalls")){
            BindingContract.require(call.path("authenticated").asBoolean(false),"Unauthenticated tool observation");String actorProfile=call.path("actorProfile").asText();JsonNode actor=fixture.path("actors").path(actorProfile);BindingContract.require(!actor.isMissingNode(),"Unknown actual actor profile");for(String key:List.of("issuer","subject","audience"))BindingContract.require(call.path(key).equals(actor.path(key)),"Actual tool principal mismatch");BindingContract.require(call.path("organizationRef").equals(aliasMap.path(actor.path("organizationAlias").asText()))&&call.path("authorizationGrantRef").equals(aliasMap.path(actor.path("grant").path("scope").path("grantAlias").asText())),"Actual tool organization/grant mismatch");
            String cap=Json.required(call,"capabilityId");BindingContract.require(capabilityKinds.containsKey(cap),"Unknown actual public capability");BindingContract.require(call.path("kind").asText().equals("READ")==capabilityKinds.get(cap).equals("QUERY"),"Wire capability/kind classification mismatch");
            if(!call.path("kind").asText().equals("READ")){BindingContract.require(!source.path("expectedIntent").path("intentKind").asText().equals("QUERY"),"QUERY attempted a command");BindingContract.require(actorProfile.equals("command-actor")&&permittedCommands.contains(call.path("capabilityId").asText()),"Unexpected command or READ profile used for write");}
        }
    }
    private JsonNode completionWitness(JsonNode transcript,String prefix) throws IOException {
        ObjectNode witness=transcript.deepCopy();JsonNode context=results.get(prefix+"/context"),read=context.path("data").path("authenticatedRead");ArrayNode reads=Json.array();
        if(read.isObject()){String ref=Json.required(read,"wireArtifactRef");BindingContract.require(strings(context.path("artifactRefs")).contains(ref),"Context read wire is unlinked");Path file=contract.validator.path(ref);BindingContract.require(file.toRealPath().startsWith(contract.validator.root().toRealPath()),"Context wire escapes repository");CapturedApiObservation.Call actual=capturedCall(context,read,prefix,false);BindingContract.require(actual.response().equals(context.path("response")),"Context response differs from actual authenticated captured wire");BindingContract.require(read.path("constraintFacts").equals(actual.response().path("constraintFacts")),"Context blocking facts differ from actual captured server response");reads.add(read);}
        witness.set("contextReads",reads);return witness;
    }
    private static Set<JsonNode> rowSet(JsonNode rows){BindingContract.require(rows.isArray(),"Business row scope unobserved");Set<JsonNode> set=new HashSet<>();rows.forEach(r->BindingContract.require(set.add(r),"Duplicate physical observation row"));return set;}
    private void verifyGroundedRead(JsonNode transcript,JsonNode snapshot,JsonNode turn,JsonNode source){
        boolean grounded=false;Set<String> requested=new HashSet<>();requested.add(turn.path("capabilityMapping").path("public").asText());String original=source.path("expectedIntent").path("slots").path("originalCapability").path("value").asText();if(!original.isBlank())requested.add(original);
        List<JsonNode> reads=new ArrayList<>();transcript.path("toolCalls").forEach(reads::add);transcript.path("contextReads").forEach(reads::add);for(JsonNode call:reads)if(call.path("kind").asText().equals("READ"))for(JsonNode fact:call.path("constraintFacts")){
            BindingContract.require(requested.contains(fact.path("blocksCapability").asText())&&fact.path("decision").asText().equals("BLOCKED"),"Read fact does not block requested execution");boolean matched=false;
            for(JsonNode row:snapshot.path("data").path("rawRows"))if(row.path("dataset").equals(fact.path("dataset"))&&row.path("attribute").equals(fact.path("attribute"))&&row.path("value").equals(fact.path("value")))matched=true;
            BindingContract.require(matched,"Constraint fact differs from independent persisted rows");grounded=true;
        }
        BindingContract.require(grounded,"Preflight lacks independently grounded blocking facts");
    }
    public void checkStructuredIntent(JsonNode expected,JsonNode actual){List<String> m=structuredIntentMismatches(expected,actual);BindingContract.require(m.isEmpty(),"Actual intent differs: "+m);}
    /** Exact where meaning is fixed; set/verbatim-excerpt where the corpus defines a minimum expectation. */
    public List<String> structuredIntentMismatches(JsonNode expected,JsonNode actual){
        List<String> m=new ArrayList<>();
        for(String key:List.of("status","intentKind","definitionVersion"))if(!actual.path(key).equals(expected.path(key)))m.add(key);
        if(!slotSet(actual.path("missingSlots")).equals(slotSet(expected.path("missingSlots")))||actual.path("missingSlots").size()!=slotSet(actual.path("missingSlots")).size())m.add("missingSlots");
        String cap=expected.path("capabilityId").asText();if(!(actual.path("capabilityId").asText().equals(cap)||cap.equals("linkRelation")&&actual.path("capabilityId").asText().equals("recordRelation")))m.add("capabilityId");
        expected.path("slots").fields().forEachRemaining(e->{
            JsonNode observed=actual.path("slots").path(e.getKey()),value=BindingEvaluator.aliases(e.getValue().path("value"),aliasMap);
            if(!observed.path("value").equals(value))m.add("slots."+e.getKey()+".value");
            for(String k:List.of("provenance","sourceRef"))if(e.getValue().has(k)&&!observed.path(k).equals(e.getValue().path(k)))m.add("slots."+e.getKey()+"."+k);
            if(e.getValue().has("sourceText")&&!verbatimSupport(observed.path("sourceText"),e.getValue().path("sourceText").asText(),e.getValue().path("sourceRef").asText()))m.add("slots."+e.getKey()+".sourceText");
        });
        return m;
    }
    private static Set<String> slotSet(JsonNode slots){Set<String> r=new HashSet<>();if(slots.isArray())slots.forEach(s->r.add(s.isTextual()?s.asText():s.toString()));return r;}
    /** The corpus excerpt is the minimum; a longer verbatim quote from the same user turn that contains it is equivalent. */
    private boolean verbatimSupport(JsonNode observed,String minimum,String sourceRef){
        if(!observed.isTextual()||observed.asText().isBlank()||!observed.asText().contains(minimum)||!sourceRef.matches("turn:[1-9][0-9]*"))return false;
        JsonNode turns=contract.sourceCase(binding).path("turns");int index=Integer.parseInt(sourceRef.substring(5))-1;
        return index<turns.size()&&turns.get(index).path("input").path("utterance").asText().contains(observed.asText());
    }
    private void checkAssertion(JsonNode a,String assertionId,JsonNode exec,JsonNode after,List<JsonNode> effects,ArrayNode results,JsonNode source,CapturedApiObservation api){ObjectNode r=Json.object();r.put("assertionId",assertionId).put("semanticPath",a.path("path").asText());results.add(r);try{evaluator.assertSemantic(a,exec,after,effects,aliasMap,expectedUnit(a,source),api);if(contract.mapping(a.path("path").asText()).path("evidenceClass").asText().equals("AUTHENTICATED_API")){var call=api.source(a.path("path").asText());String capability=source.path("expectedIntent").path("capabilityId").asText();if(capability.equals("linkRelation"))capability="recordRelation";String original=source.path("expectedIntent").path("slots").path("originalCapability").path("value").asText();BindingContract.require(call.capabilityId().equals(capability)||!original.isBlank()&&call.capabilityId().equals(original)||source.path("expectedIntent").path("intentKind").asText().equals("QUERY")&&call.capabilityId().equals("getObject"),"API assertion uses a different capability response");r.put("capturedCallId",call.id()).put("wireArtifactRef",call.wireArtifactRef()).put("businessResponsePointer",call.responsePointer());}r.put("status","PASS");}catch(AssertionError|IllegalArgumentException e){r.put("status","FAIL").put("reason",e.getMessage());throw e;}}
    private String expectedUnit(JsonNode assertion,JsonNode source){
        JsonNode expected=assertion.path("expected");boolean numeric=expected.isTextual()&&expected.asText().matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?");if(expected.isArray()&&!expected.isEmpty()){numeric=true;for(JsonNode n:expected)numeric&=n.isTextual()&&n.asText().matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?");}if(!numeric)return null;
        String path=assertion.path("path").asText();if(path.equals("state.invoice.convertedAmount"))return "KRW";if(path.equals("state.invoice.originalAmount")||path.equals("state.paymentReference.amount"))return source.path("expectedIntent").path("slots").path("currency").path("value").asText();
        String unit=source.path("expectedIntent").path("slots").path("unit").path("value").asText();return unit.isBlank()?fixture.path("baseline").path("domainFacts").path("objects").path("P").path("baseUnit").asText():unit;
    }
    private static List<String> strings(JsonNode array){List<String> r=new ArrayList<>();array.forEach(n->r.add(n.asText()));return r;}
    public void declaredAssertion(String assertionId,String semantic){boolean found=false;for(JsonNode t:binding.path("turns"))for(JsonNode a:t.path("commonAssertions"))if(a.path("assertionId").asText().equals(assertionId)){BindingContract.require(a.path("semanticPath").asText().equals(semantic),"Gherkin assertion path drift");found=true;}BindingContract.require(found,"Unknown Gherkin assertion");}
    public ObjectNode evidence(){ObjectNode r=Json.object();r.put("caseId",binding.path("caseId").asText()).put("profile",profile).put("plannedRepeats",3).put("runtimeStatus",status()).put("productGateComplete",false).put("actualClientInvocations",actualInvocations);if(actualInvocations==0)r.put("actualModelCalls",0);else r.putNull("actualModelCalls");r.set("perTurn",turnReports);ObjectNode actions=Json.object();results.forEach(actions::set);r.set("actionResults",actions);r.set("attempts",Json.array());r.putNull("usage");r.putNull("cost");return r;}
    public String status(){if(turnReports.size()!=binding.path("turns").size())return "NOT_RUN";for(JsonNode t:turnReports)if(t.path("status").asText().equals("FAIL"))return "FAIL";for(JsonNode t:turnReports)if(!t.path("status").asText().equals("PASS"))return "NOT_RUN";return "PASS";}
    public void verifyComplete(){int count=1+5*binding.path("turns").size();BindingContract.require(completed.size()==count,"Not every Gherkin milestone executed");}
}
