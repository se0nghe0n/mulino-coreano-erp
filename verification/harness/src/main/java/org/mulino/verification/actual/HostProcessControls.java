package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.mulino.verification.Json;
import org.mulino.verification.StepResult;

/**
 * Host process controls of the scenario suite that run on this checkout, not in the product: verifyCoverage (T25) and
 * inspectArtifacts. Each runs a real host command (verification/actual/scenarios/coverage_verify.py) in a subprocess,
 * keeps its transcript, then a separate read-only extractor (host_extract.py) re-reads the generated or requested files.
 * The observation follows contracts/acceptance-host-observation.schema.json and host-observation-guide.md; the harness
 * validates it (HostObservationValidator) and the case assertions read the extractor rows. Nothing here computes an
 * oracle value.
 */
final class HostProcessControls {
    static final Set<String> OPERATIONS=Set.of("verifyCoverage","inspectArtifacts");
    private static final String VERIFIER="verification/actual/scenarios/coverage_verify.py",EXTRACTOR="verification/actual/scenarios/host_extract.py";
    private final Path root;private final String run;
    HostProcessControls(Path root,String run){this.root=root;this.run=run;}

    /** Provenance and evidence writing stay with the driver; this class only builds the observation. */
    interface Evidence {ObjectNode provenance();}

    StepResult control(String id,JsonNode request,Evidence evidence) throws IOException,InterruptedException {
        String operation=request.path("operation").asText();JsonNode parameters=request.path("parameters");ObjectNode provenance=evidence.provenance();
        String dir="verification/harness/target/evidence/actual/"+run+"/host-"+UUID.randomUUID();
        Files.createDirectories(root.resolve(dir));
        var refs=new ArrayList<String>();
        ObjectNode scope=parameters.path("scope").isObject()&&!parameters.path("scope").isEmpty()?(ObjectNode)parameters.path("scope"):Json.object().put("operation",operation);
        var inputs=Json.array();var generated=Json.array();var observed=Json.array();ObjectNode identity=Json.object();
        List<String> argv;String report=null;List<String> inspected=new ArrayList<>();
        if(operation.equals("verifyCoverage")) {
            String requestRef=dir+"/request.json";Json.write(root.resolve(requestRef),parameters);refs.add(requestRef);
            report=dir+"/coverage-report.json";
            argv=List.of("python3","-I",VERIFIER,"--root",".","--request",requestRef,"--out",report);
            for(String key:List.of("registryPath","catalogPath")) inputs.add(descriptor(Json.required(parameters,key),scope));
            String snapshot=parameters.path(switch(parameters.path("inputSnapshotKind").asText()){case "PREPARATION"->"preparationReportPath";case "MODEL_BINDING_PREPARATION"->"modelManifestPath";default->"manifestPath";}).asText(null);
            if(snapshot==null||!Files.isRegularFile(root.resolve(snapshot)))
                return StepResult.missing(id,"NOT_IMPLEMENTED: verifyCoverage input snapshot absent: "+snapshot+" ("+parameters.path("inputSnapshotKind").asText()+"; run its producer on this clean commit first)");
            inputs.add(descriptor(snapshot,scope));
            identity.put("registryHash",inputs.get(0).path("sha256").asText()).put("catalogHash",inputs.get(1).path("sha256").asText());
        } else {
            JsonNode list=parameters.path("artifacts");
            if(!list.isArray()||list.isEmpty())return StepResult.missing(id,"NOT_IMPLEMENTED: inspectArtifacts without an explicit artifact list");
            for(JsonNode a:list){String path=Json.required(a,"path");inspected.add(path);}
            String requestRef=dir+"/request.json";Json.write(root.resolve(requestRef),parameters);refs.add(requestRef);
            argv=new ArrayList<>(List.of("python3","-I","-c","import hashlib,sys\nfor p in sys.argv[1:]:\n d=open(p,'rb').read();print(p,hashlib.sha256(d).hexdigest(),len(d))"));argv.addAll(inspected);
            identity.put("inspectionId",dir.substring(dir.lastIndexOf('/')+1));
        }
        // Host command.
        String transcript=dir+"/command-transcript.txt";
        Instant started=Instant.now();var outcome=exec(argv);Instant completed=Instant.now();
        Files.writeString(root.resolve(transcript),"evidenceClass=ACTUAL_HOST\nexitCode="+outcome.code+"\n"+outcome.output,StandardCharsets.UTF_8);refs.add(transcript);
        if(operation.equals("verifyCoverage")&&outcome.code==4)return StepResult.missing(id,"NOT_IMPLEMENTED: verifyCoverage input snapshot absent ("+outcome.output.strip()+")");
        if(operation.equals("verifyCoverage")&&outcome.code==5)return StepResult.missing(id,"NOT_IMPLEMENTED: verifyCoverage runtime-manifest snapshot verification absent ("+parameters.path("inputSnapshotKind").asText()+")");
        var command=Json.object();var args=command.putArray("argv");argv.forEach(args::add);
        command.put("exitCode",outcome.code).put("startedAt",started.toString()).put("completedAt",completed.toString()).put("transcriptRef",transcript).put("redacted",true);
        if(report!=null&&outcome.code==0){generated.add(descriptor(report,scope));refs.add(report);}
        if(operation.equals("inspectArtifacts"))for(JsonNode a:parameters.path("artifacts"))if(Files.isRegularFile(root.resolve(a.path("path").asText())))observed.add(descriptor(a.path("path").asText(),a.path("scope").isObject()?(ObjectNode)a.path("scope"):scope));
        var environment=Json.object().put("profile","LOCAL").put("hostId",host()).put("workspaceId",run).put("isolated",true);
        // Host descriptor the extractor reads (written before it starts).
        String descriptor=dir+"/host-descriptor.json";
        var described=Json.object();described.put("operation",operation);described.set("command",command);described.set("environment",environment);described.set("scope",scope);described.set("operationEvidence",identity);described.put("evidenceClass","ACTUAL_HOST");described.set("driverProvenance",provenance);
        Json.write(root.resolve(descriptor),described);refs.add(descriptor);
        // Independent read-only extractor.
        String rows=dir+"/extractor-rows.json",extractorTranscript=dir+"/extractor-transcript.txt";
        var extractorArgv=new ArrayList<>(List.of("python3","-I",EXTRACTOR,"--root",".","--host",descriptor,"--out",rows));
        if(report!=null&&outcome.code==0){extractorArgv.add("--report");extractorArgv.add(report);}
        for(String path:inspected){extractorArgv.add("--artifact");extractorArgv.add(path);}
        Instant xs=Instant.now();var extracted=exec(extractorArgv);Instant xc=Instant.now();
        Files.writeString(root.resolve(extractorTranscript),"evidenceClass=ACTUAL_HOST\nexitCode="+extracted.code+"\n"+extracted.output,StandardCharsets.UTF_8);refs.add(extractorTranscript);
        if(extracted.code!=0)throw new IllegalStateException("Host extractor failed: "+extracted.output.strip());
        refs.add(rows);
        var extractorCommand=Json.object();var xa=extractorCommand.putArray("argv");extractorArgv.forEach(xa::add);
        extractorCommand.put("exitCode",extracted.code).put("startedAt",xs.toString()).put("completedAt",xc.toString()).put("transcriptRef",extractorTranscript).put("redacted",true);
        var extractor=Json.object();extractor.put("name","host_extract.py").put("version","1.0.0").put("source","FILESYSTEM_READ_ONLY").put("independent",true).put("readOnly",true);
        extractor.set("command",extractorCommand);
        var extractorInputs=extractor.putArray("inputArtifacts");generated.forEach(extractorInputs::add);observed.forEach(extractorInputs::add);extractorInputs.add(descriptor(rows,scope));
        extractor.set("rawRows",Json.read(root.resolve(rows)));extractor.put("rawRowsArtifactRef",rows);extractor.putArray("transcriptRefs").add(extractorTranscript);
        var hostObservation=Json.object();hostObservation.put("schemaVersion","1.0.0").put("operation",operation);
        hostObservation.set("requestedInputs",parameters);hostObservation.set("scope",scope);hostObservation.put("scopeComplete",true);hostObservation.set("command",command);
        var tools=hostObservation.putArray("toolVersions");tools.add(Json.object().put("name","python3").put("version",version("python3","--version")));tools.add(Json.object().put("name","git").put("version",version("git","--version")));
        hostObservation.set("environment",environment);hostObservation.set("inputArtifacts",inputs);hostObservation.set("generatedOutputs",generated);hostObservation.set("observedArtifacts",observed);
        hostObservation.set("extractor",extractor);hostObservation.set("operationEvidence",identity);hostObservation.put("evidenceClass","ACTUAL_HOST");
        var data=Json.object();data.put("acknowledged",true).put("controlType","process").put("operation",operation).put("acknowledgedAt",completed.toString());data.set("hostObservation",hostObservation);
        return new StepResult(id,StepResult.DriverStatus.EXECUTED,data,null,null,provenance,refs);
    }
    private ObjectNode descriptor(String ref,ObjectNode scope) throws IOException {
        Path path=root.resolve(ref);
        return Json.object().put("path",ref).put("sha256",Json.sha256(path)).put("sizeBytes",Files.size(path)).<ObjectNode>set("scope",scope).put("completeness","COMPLETE");
    }
    private record Outcome(int code,String output){}
    private Outcome exec(List<String> argv) throws IOException,InterruptedException {
        var process=new ProcessBuilder(argv).directory(root.toFile()).redirectErrorStream(true).start();
        String output=new String(process.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
        return new Outcome(process.waitFor(),output.length()>20000?output.substring(0,20000):output);
    }
    private String version(String... argv){try{var o=exec(List.of(argv));return o.output.strip().isEmpty()?"unknown":o.output.strip();}catch(Exception failure){return "unknown";}}
    private static String host(){try{return java.net.InetAddress.getLocalHost().getHostName();}catch(Exception failure){return "localhost";}}
}
