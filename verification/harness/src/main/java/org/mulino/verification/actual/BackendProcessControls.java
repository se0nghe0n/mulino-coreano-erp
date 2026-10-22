package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.mulino.verification.Json;
import org.mulino.verification.StepResult;

/**
 * Test-profile process lifecycle controls start, stop and restart (T26, V5, V7) as host process controls. The scenario
 * suite runs one backend JVM on one disposable database (run.sh); every processId (api, scheduler, worker-*, application)
 * and environmentId of a case is a role co-hosted by that JVM, because the product is a single deployable (plan §2). The
 * host command verification/actual/scenarios/process_host.py really terminates or relaunches that JVM on the same port,
 * so memory is lost and only the database survives. A start of a running JVM confirms the role on that same instance
 * without a launch; stop or restart of a stopped JVM fails honestly. The adapter observes the state
 * before the command (OS pid, RUNNING/STOPPED), the command records the after state and terminal snapshot, and the
 * read-only extractor host_extract.py re-reads that snapshot (host-observation-guide.md start/stop/restart).
 */
final class BackendProcessControls {
    static final Set<String> OPERATIONS=Set.of("start","stop","restart");
    private static final String COMMAND="verification/actual/scenarios/process_host.py",EXTRACTOR="verification/actual/scenarios/host_extract.py";
    private final Path root;private final String run;private final URI base;private final String pidFile,argv,log;
    BackendProcessControls(Path root,String run,URI base,Map<String,String> env){this.root=root;this.run=run;this.base=base;pidFile=env.get("ACTUAL_BACKEND_PID_FILE");argv=env.get("ACTUAL_BACKEND_ARGV");log=env.get("ACTUAL_BACKEND_LOG");}
    boolean available(){return pidFile!=null&&argv!=null&&log!=null&&Files.isRegularFile(Path.of(pidFile));}

    /** Before a fixture installation: a subcase that stopped the backend must not leave the next one without it. */
    synchronized void ensureRunning() throws IOException,InterruptedException{
        if(!available())return;var o=exec(base(List.of("--op","start","--quiet-ensure")));if(o.code!=0)throw new IllegalStateException("Backend ensure-start failed: "+o.output.strip());
    }
    synchronized StepResult control(String id,JsonNode request,HostProcessControls.Evidence evidence) throws IOException,InterruptedException {
        String operation=request.path("operation").asText();JsonNode parameters=request.path("parameters");ObjectNode provenance=evidence.provenance();
        String processId=Json.required(parameters,"processId");String environmentId=parameters.path("environmentId").asText("suite");
        String dir="verification/harness/target/evidence/actual/"+run+"/process-"+UUID.randomUUID();Files.createDirectories(root.resolve(dir));
        var refs=new ArrayList<String>();String requestRef=dir+"/request.json";Json.write(root.resolve(requestRef),parameters);refs.add(requestRef);
        ObjectNode scope=parameters.path("scope").isObject()&&!parameters.path("scope").isEmpty()?(ObjectNode)parameters.path("scope").deepCopy():Json.object().put("operation",operation);
        var before=observe(processId);
        String report=dir+"/process-terminal.json";
        var args=base(List.of("--op",operation,"--process-id",processId,"--scope-json",scope.toString(),"--out",report,"--before-json",before.toString()));
        Instant started=Instant.now();var outcome=exec(args);Instant completed=Instant.now();
        String transcript=dir+"/command-transcript.txt";Files.writeString(root.resolve(transcript),"evidenceClass=ACTUAL_HOST\nexitCode="+outcome.code+"\n"+outcome.output,StandardCharsets.UTF_8);refs.add(transcript);
        if(!Files.isRegularFile(root.resolve(report)))throw new IllegalStateException("Process host command wrote no terminal snapshot: "+outcome.output.strip());
        refs.add(report);
        var command=Json.object();var a=command.putArray("argv");for(String s:args)a.add(s.equals(before.toString())?"<before-state>":s);
        command.put("exitCode",outcome.code).put("startedAt",started.toString()).put("completedAt",completed.toString()).put("transcriptRef",transcript).put("redacted",true);
        var generated=Json.array();generated.add(descriptor(report,scope));
        var environment=Json.object().put("profile","LOCAL").put("hostId",host()).put("workspaceId",run).put("isolated",true);
        var identity=Json.object().put("processId",processId).put("environmentId",environmentId);
        String descriptor=dir+"/host-descriptor.json";var described=Json.object();described.put("operation",operation);described.set("command",command);described.set("environment",environment);described.set("scope",scope);described.set("operationEvidence",identity);described.put("evidenceClass","ACTUAL_HOST");described.set("driverProvenance",provenance);
        Json.write(root.resolve(descriptor),described);refs.add(descriptor);
        String rows=dir+"/extractor-rows.json",extractorTranscript=dir+"/extractor-transcript.txt";
        var extractorArgv=List.of("python3","-I",EXTRACTOR,"--root",".","--host",descriptor,"--out",rows,"--report",report);
        Instant xs=Instant.now();var extracted=exec(extractorArgv);Instant xc=Instant.now();
        Files.writeString(root.resolve(extractorTranscript),"evidenceClass=ACTUAL_HOST\nexitCode="+extracted.code+"\n"+extracted.output,StandardCharsets.UTF_8);refs.add(extractorTranscript);
        if(extracted.code!=0)throw new IllegalStateException("Host extractor failed: "+extracted.output.strip());
        refs.add(rows);
        var extractorCommand=Json.object();var xa=extractorCommand.putArray("argv");extractorArgv.forEach(xa::add);
        extractorCommand.put("exitCode",extracted.code).put("startedAt",xs.toString()).put("completedAt",xc.toString()).put("transcriptRef",extractorTranscript).put("redacted",true);
        var extractor=Json.object();extractor.put("name","host_extract.py").put("version","1.0.0").put("source","FILESYSTEM_READ_ONLY").put("independent",true).put("readOnly",true);extractor.set("command",extractorCommand);
        var extractorInputs=extractor.putArray("inputArtifacts");generated.forEach(extractorInputs::add);extractorInputs.add(descriptor(rows,scope));
        extractor.set("rawRows",Json.read(root.resolve(rows)));extractor.put("rawRowsArtifactRef",rows);extractor.putArray("transcriptRefs").add(extractorTranscript);
        var host=Json.object();host.put("schemaVersion","1.0.0").put("operation",operation);host.set("requestedInputs",parameters);host.set("scope",scope);host.put("scopeComplete",true);host.set("command",command);
        var tools=host.putArray("toolVersions");tools.add(Json.object().put("name","python3").put("version",version("python3","--version")));tools.add(Json.object().put("name","java").put("version",System.getProperty("java.version")));
        host.set("environment",environment);host.set("inputArtifacts",Json.array());host.set("generatedOutputs",generated);host.set("observedArtifacts",Json.array());
        host.set("extractor",extractor);host.set("operationEvidence",identity);host.put("evidenceClass","ACTUAL_HOST");host.set("processObservation",Json.read(root.resolve(report)).path("processObservation"));
        var data=Json.object();data.put("acknowledged",true).put("controlType","process").put("operation",operation).put("acknowledgedAt",completed.toString());
        JsonNode terminal=Json.read(root.resolve(report));
        String processAction=terminal.path("rows").path("processAction").asText();
        data.put("processAction",processAction).put("roleConfirmed",outcome.code==0&&operation.equals("start"));
        data.put("launched",outcome.code==0&&Set.of("STARTED","RESTARTED").contains(processAction));data.set("hostObservation",host);
        return new StepResult(id,StepResult.DriverStatus.EXECUTED,data,null,null,provenance,refs);
    }
    private List<String> base(List<String> extra){var out=new ArrayList<>(List.of("python3","-I",COMMAND,"--pid-file",pidFile,"--argv-json",argv,"--port",Integer.toString(base.getPort()),"--log",log));out.addAll(extra);return out;}
    /** The adapter's own observation before the command: the pid file's process, RUNNING only if alive and not a zombie. */
    private ObjectNode observe(String processId) throws IOException,InterruptedException{
        String text=Files.readString(Path.of(pidFile)).trim();Long pid=text.isEmpty()?null:Long.valueOf(text);
        boolean running=pid!=null&&ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
        if(running){var ps=exec(List.of("ps","-o","stat=","-p",Long.toString(pid)));running=ps.code==0&&!ps.output.strip().isEmpty()&&!ps.output.strip().startsWith("Z");}
        var s=Json.object();s.put("processId",processId);if(running)s.put("instanceId",Long.toString(pid));else s.putNull("instanceId");s.put("status",running?"RUNNING":"STOPPED").put("observedAt",Instant.now().toString());return s;
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
