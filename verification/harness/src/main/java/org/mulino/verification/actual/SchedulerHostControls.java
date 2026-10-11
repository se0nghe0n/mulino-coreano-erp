package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.mulino.verification.Json;
import org.mulino.verification.StepResult;

/**
 * Scheduler process controls of the scenario suite: tickScheduler, sweepDue and awaitRuntimeTask against the
 * verification-profile scheduler (backend VerificationScheduler). The scenario server runs no scheduler loop of its own,
 * so a harness tick is exactly one scheduler cycle submitted as one task (controlledTicks). A host command
 * (verification/actual/scenarios/scheduler_host.py) posts the tick or reads the terminal and keeps its transcript; a
 * separate read-only extractor (scheduler_extract.py) re-reads the scheduler's own submission, cycle and task rows, never
 * the tick reply. The observation follows contracts/acceptance-host-observation.schema.json and host-observation-guide.md
 * "autonomous scheduler terminal". A passive natural-tick watch (OBSERVE_NEXT_NATURAL_TICK) needs a running scheduler loop
 * the scenario server does not have, so it stays NOT_IMPLEMENTED.
 */
final class SchedulerHostControls {
    static final Set<String> OPERATIONS=Set.of("tickScheduler","sweepDue","awaitRuntimeTask");
    private static final String HOST="verification/actual/scenarios/scheduler_host.py",EXTRACTOR="verification/actual/scenarios/scheduler_extract.py";
    /** Authored names of the instant a tick runs at (the business clock moves there first). */
    private static final List<String> INSTANT_KEYS=List.of("clock","clockInstant","asOf","instant");
    private final Path root;private final String run;
    SchedulerHostControls(Path root,String run){this.root=root;this.run=run;}

    StepResult control(String id,JsonNode request,String baseUri,String token,HostProcessControls.Evidence evidence) throws IOException,InterruptedException {
        String operation=request.path("operation").asText();JsonNode parameters=request.path("parameters");
        for(String key:List.of("trigger","triggeredBy","observationWindowSeconds"))if(parameters.has(key))
            return StepResult.missing(id,"NOT_IMPLEMENTED: passive natural-tick observation needs a running scheduler loop; the scenario server only runs harness-controlled ticks (VerificationScheduler)");
        String scheduler=Json.required(parameters,"schedulerId");
        ObjectNode provenance=evidence.provenance();
        String dir="verification/harness/target/evidence/actual/"+run+"/scheduler-"+UUID.randomUUID();
        Files.createDirectories(root.resolve(dir));
        var refs=new ArrayList<String>();
        ObjectNode scope=parameters.path("scope").isObject()&&!parameters.path("scope").isEmpty()?(ObjectNode)parameters.path("scope"):Json.object().put("operation",operation);
        String requestRef=dir+"/request.json";Json.write(root.resolve(requestRef),parameters);refs.add(requestRef);
        List<String> argv;String output;ObjectNode selector=Json.object();
        if(operation.equals("awaitRuntimeTask")) {
            if(!parameters.hasNonNull("taskId")&&!parameters.hasNonNull("invocationHandle"))return StepResult.missing(id,"NOT_IMPLEMENTED: awaitRuntimeTask without an actual taskId or invocationHandle");
            output=dir+"/runtime-task-snapshot.json";
            argv=new ArrayList<>(List.of("python3","-I",HOST,"await","--scheduler",scheduler,"--timeout",String.valueOf(parameters.path("timeoutSeconds").asInt(30)),"--out",output));
            for(String key:List.of("taskId","invocationHandle"))if(parameters.hasNonNull(key)){argv.add(key.equals("taskId")?"--task-id":"--handle");argv.add(parameters.path(key).asText());selector.put(key,parameters.path(key).asText());}
        } else {
            var wire=Json.object();wire.put("schedulerId",scheduler).put("kind",operation.equals("sweepDue")?"SWEEP":"TICK");
            String runKey=operation.equals("sweepDue")?"sweepId":"tickId";if(parameters.hasNonNull(runKey))wire.put("runId",parameters.path(runKey).asText());
            for(String key:INSTANT_KEYS)if(parameters.path(key).isTextual()){wire.put("instant",parameters.path(key).asText());break;}
            String organization=scope.path("organizationId").asText("");if(organization.matches("[0-9a-f-]{36}"))wire.put("organizationId",organization);
            String wireRef=dir+"/wire-request.json";Json.write(root.resolve(wireRef),wire);refs.add(wireRef);
            output=dir+"/submission.json";
            argv=List.of("python3","-I",HOST,"tick","--request",wireRef,"--out",output);
        }
        String transcript=dir+"/command-transcript.txt";
        Instant started=Instant.now();var outcome=exec(argv,baseUri,token);Instant completed=Instant.now();
        Files.writeString(root.resolve(transcript),"evidenceClass=ACTUAL_HOST\nexitCode="+outcome.code+"\n"+outcome.output,StandardCharsets.UTF_8);refs.add(transcript);
        if(outcome.code!=0) {
            if(Files.isRegularFile(root.resolve(output)))refs.add(output);
            throw new IllegalStateException("Scheduler host control "+operation+" failed (exit "+outcome.code+"): "+outcome.output.strip());
        }
        refs.add(output);
        var command=Json.object();var args=command.putArray("argv");argv.forEach(args::add);
        command.put("exitCode",outcome.code).put("startedAt",started.toString()).put("completedAt",completed.toString()).put("transcriptRef",transcript).put("redacted",true);
        var generated=Json.array();generated.add(descriptor(output,scope));
        ObjectNode identity=Json.object();ObjectNode runtimeTask=null;JsonNode written=Json.read(root.resolve(output));
        if(operation.equals("awaitRuntimeTask")) {
            identity.put("schedulerId",scheduler);
            runtimeTask=Json.object();for(String key:List.of("taskId","invocationHandle","origin","terminalStatus","completedAt"))runtimeTask.set(key,written.path(key));
            runtimeTask.putObject("snapshot").put("id",written.path("snapshotId").asText()).put("capturedAt",written.path("capturedAt").asText()).put("artifactRef",output);
        } else {
            JsonNode reply=written.path("reply");String runKey=operation.equals("sweepDue")?"sweepId":"tickId";
            identity.put("schedulerId",reply.path("schedulerId").asText()).put(runKey,reply.path(runKey).asText()).put("submissionStatus",reply.path("submissionStatus").asText());
            if("SUBMITTED".equals(reply.path("submissionStatus").asText()))identity.put("taskId",reply.path("taskId").asText()).put("invocationHandle",reply.path("invocationHandle").asText()).put("submittedAt",reply.path("submittedAt").asText());
        }
        var environment=Json.object().put("profile","LOCAL").put("hostId",host()).put("workspaceId",run).put("isolated",true);
        String descriptorRef=dir+"/host-descriptor.json";
        var described=Json.object();described.put("operation",operation);described.set("command",command);described.set("environment",environment);described.set("scope",scope);described.set("operationEvidence",identity);described.put("evidenceClass","ACTUAL_HOST");described.set("driverProvenance",provenance);described.set("taskSelector",selector);
        Json.write(root.resolve(descriptorRef),described);refs.add(descriptorRef);
        String rows=dir+"/extractor-rows.json",extractorTranscript=dir+"/extractor-transcript.txt";
        var extractorArgv=new ArrayList<>(List.of("python3","-I",EXTRACTOR,"--root",".","--host",descriptorRef,"--out",rows));
        if(operation.equals("awaitRuntimeTask")){extractorArgv.add("--snapshot");extractorArgv.add(output);}
        Instant xs=Instant.now();var extracted=exec(extractorArgv,baseUri,token);Instant xc=Instant.now();
        Files.writeString(root.resolve(extractorTranscript),"evidenceClass=ACTUAL_HOST\nexitCode="+extracted.code+"\n"+extracted.output,StandardCharsets.UTF_8);refs.add(extractorTranscript);
        if(extracted.code!=0)throw new IllegalStateException("Scheduler extractor failed: "+extracted.output.strip());
        refs.add(rows);
        var extractorCommand=Json.object();var xa=extractorCommand.putArray("argv");extractorArgv.forEach(xa::add);
        extractorCommand.put("exitCode",extracted.code).put("startedAt",xs.toString()).put("completedAt",xc.toString()).put("transcriptRef",extractorTranscript).put("redacted",true);
        var extractor=Json.object();extractor.put("name","scheduler_extract.py").put("version","1.0.0").put("source","HOST_TOOL_READ_ONLY").put("independent",true).put("readOnly",true);
        extractor.set("command",extractorCommand);
        var extractorInputs=extractor.putArray("inputArtifacts");generated.forEach(extractorInputs::add);extractorInputs.add(descriptor(descriptorRef,scope));extractorInputs.add(descriptor(rows,scope));
        extractor.set("rawRows",Json.read(root.resolve(rows)));extractor.put("rawRowsArtifactRef",rows);extractor.putArray("transcriptRefs").add(extractorTranscript);
        var hostObservation=Json.object();hostObservation.put("schemaVersion","1.0.0").put("operation",operation);
        hostObservation.set("requestedInputs",parameters);hostObservation.set("scope",scope);hostObservation.put("scopeComplete",true);hostObservation.set("command",command);
        hostObservation.putArray("toolVersions").add(Json.object().put("name","python3").put("version",version()));
        hostObservation.set("environment",environment);hostObservation.set("inputArtifacts",Json.array().add(descriptor(requestRef,scope)));hostObservation.set("generatedOutputs",generated);hostObservation.set("observedArtifacts",Json.array());
        hostObservation.set("extractor",extractor);hostObservation.set("operationEvidence",identity);if(runtimeTask!=null)hostObservation.set("runtimeTask",runtimeTask);hostObservation.put("evidenceClass","ACTUAL_HOST");
        var data=Json.object();data.put("acknowledged",true).put("controlType","process").put("operation",operation).put("acknowledgedAt",completed.toString());data.set("hostObservation",hostObservation);
        return new StepResult(id,StepResult.DriverStatus.EXECUTED,data,null,null,provenance,refs);
    }
    private ObjectNode descriptor(String ref,ObjectNode scope) throws IOException {
        Path path=root.resolve(ref);
        return Json.object().put("path",ref).put("sha256",Json.sha256(path)).put("sizeBytes",Files.size(path)).<ObjectNode>set("scope",scope).put("completeness","COMPLETE");
    }
    private record Outcome(int code,String output){}
    /** The base URL and bearer go through the environment, never argv, so no transcript carries a credential. */
    private Outcome exec(List<String> argv,String baseUri,String token) throws IOException,InterruptedException {
        var builder=new ProcessBuilder(argv).directory(root.toFile()).redirectErrorStream(true);
        if(baseUri!=null)builder.environment().put("MULINO_BASE_URL",baseUri);
        if(token!=null)builder.environment().put("MULINO_CONTROL_TOKEN",token);
        var process=builder.start();
        String output=new String(process.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
        return new Outcome(process.waitFor(),output.length()>20000?output.substring(0,20000):output);
    }
    private String version(){try{var o=exec(List.of("python3","--version"),null,null);return o.output.strip().isEmpty()?"unknown":o.output.strip();}catch(Exception failure){return "unknown";}}
    private static String host(){try{return java.net.InetAddress.getLocalHost().getHostName();}catch(Exception failure){return "localhost";}}
}
