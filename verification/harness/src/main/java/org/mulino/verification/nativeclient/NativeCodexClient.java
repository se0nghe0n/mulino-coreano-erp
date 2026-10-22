package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.mulino.verification.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.mulino.verification.nativeclient.NativeClientPolicy.*;

/** Native execution only; independent business/semantic observation belongs to the product SPI. */
public final class NativeCodexClient implements AgentRunner.ActualClientPort {
    private final NativeClientPolicy policy;
    private final NativeClientIntegration integration;
    public NativeCodexClient() { this(new NativeClientPolicy(Path.of(System.getProperty("repo.root", "."))), discover()); }
    NativeCodexClient(NativeClientPolicy policy, NativeClientIntegration integration) { this.policy=policy;this.integration=integration; }
    static NativeClientIntegration discover() {
        var all=ServiceLoader.load(NativeClientIntegration.class).stream().toList();
        require(all.size()<=1,"AMBIGUOUS_NATIVE_INTEGRATION");return all.isEmpty()?null:all.getFirst().get();
    }
    @Override public Set<String> providedAdapters() { return integration==null?Set.of():Set.of("client","model","skills"); }
    @Override public StepResult execute(String id,String route,JsonNode actor,String utterance,JsonNode context) {
        if(!Boolean.getBoolean("verification.nativeClient.enabled"))return StepResult.missing(id,"NATIVE_CLIENT_DISABLED: explicit coordinator execution required");
        if(integration==null)return StepResult.missing(id,"NATIVE_INTEGRATION_ABSENT: authenticated transport, DB/host/final-response extractors required before model call");
        Path capture=null;var evidence=Json.object();long began=System.nanoTime();
        try {
            var input=input(utterance,context);policy.approval();
            try(var session=integration.open(id,route,actor,utterance,context)) {
                require(session!=null,"AUTHENTICATED_SESSION_MISSING");session.requireReady();
                JsonNode productTools=session.tools();require(productTools.isArray()&&!productTools.isEmpty(),"PRODUCT_TOOLS_MISSING");
                ModelInputGuard.tools(productTools);
                Path dir=policy.root.resolve("verification/harness/target/native-client/"+UUID.randomUUID());
                Files.createDirectories(dir);Files.setPosixFilePermissions(dir,PosixFilePermissions.fromString("rwx------"));
                capture=dir.resolve("capture.json");
                evidence.put("evidenceClass","ACTUAL_NATIVE_CLIENT").put("actionId",id).put("startedAt",Instant.now().toString());
                evidence.set("approval",policy.approval());
                try(var runtime=new RuntimeHome(policy);var client=runtime.start(Duration.ofSeconds(180))) {
                    evidence.set("events",client.events);evidence.set("command",Json.MAPPER.valueToTree(client.command));
                    evidence.put("processId",client.pid()).put("clientExactVersion",VERSION).put("clientBinarySha256",runtime.binaryHash);
                    evidence.put("configSha256",sha(RuntimeHome.CONFIG)).put("config",RuntimeHome.CONFIG);
                    evidence.set("preflight",client.preflight());
                    Map<String,Path> skills=new LinkedHashMap<>();
                    for(Path file:policy.skillFiles())skills.put(policy.root.relativize(file).toString(),file);
                    var skillManifest=Json.object();for(var e:skills.entrySet())skillManifest.put(e.getKey(),Json.sha256(e.getValue()));
                    evidence.set("skillManifest",skillManifest);
                    StringBuilder instructions=new StringBuilder(Files.readString(policy.root.resolve("verification/native-client/prompt.txt")));
                    instructions.append("\n운영 skill metadata다. 필요한 SKILL.md와 reference를 load_operational_skill로 읽는다.\n");
                    for(String name:PACKAGES){String body=Files.readString(policy.root.resolve("agents/skills/"+name+"/SKILL.md"));
                        String front=body.startsWith("---")?body.substring(0,body.indexOf("---",3)+3):"";
                        instructions.append(name).append(": ").append(front).append('\n');}
                    var tools=(ArrayNode)productTools.deepCopy();tools.add(skillTool());
                    evidence.put("effectiveInstructionsSha256",sha(instructions.toString())).put("inputSha256",sha(input.toString()))
                        .put("toolSchemasSha256",sha(tools.toString()));
                    var params=Json.object().put("model",MODEL).put("modelProvider","openai").put("serviceTier",TIER)
                        .put("allowProviderModelFallback",false).put("ephemeral",true).put("approvalPolicy","never")
                        .put("sandbox","read-only").put("cwd",runtime.workspace.toString()).put("baseInstructions",instructions.toString())
                        .put("developerInstructions","").put("experimentalRawEvents",false);
                    params.set("dynamicTools",tools);params.set("environments",Json.array());params.set("runtimeWorkspaceRoots",Json.array().add(runtime.workspace.toString()));
                    var messages=Json.array().add(Json.object().put("type","text").put("text",input.toString()));
                    Set<String> loaded=new java.util.concurrent.CopyOnWriteArraySet<>();
                    try {
                        var result=client.run(params,messages,request -> {
                            if(request.path("tool").asText().equals("load_operational_skill")) {
                                String name=request.path("arguments").path("path").asText();require(skills.containsKey(name),"UNAPPROVED_SKILL_PATH");
                                try {String body=Files.readString(skills.get(name));require(Json.sha256(skills.get(name)).equals(skillManifest.path(name).asText()),"SKILL_CHANGED_DURING_RUN");loaded.add(name);return toolText(body);}
                                catch(IOException e){throw new IllegalStateException("SKILL_READ_FAILED");}
                            }
                            require(ModelInputGuard.containsTool(productTools,request.path("tool").asText()),"UNDECLARED_PRODUCT_TOOL");
                            return session.call(request);
                        });
                        evidence.set("result",result);evidence.put("clientStatus","COMPLETED");
                        evidence.put("skillLoadingObserved",!loaded.isEmpty());
                    } finally {
                        evidence.set("events",client.events);evidence.set("loadedSkills",Json.MAPPER.valueToTree(loaded));
                    }
                }
                evidence.put("finishedAt",Instant.now().toString());writeCapture(capture,evidence);
                // This SPI must not promote final model text into authenticated API/DB facts.
                StepResult observed=session.observe(capture,evidence.deepCopy());
                require(observed!=null&&observed.actionId().equals(id),"OBSERVER_ACTION_MISMATCH");
                if(observed.driverStatus()==StepResult.DriverStatus.EXECUTED) {
                    require(evidence.path("skillLoadingObserved").asBoolean(),"SKILL_LOADING_UNOBSERVED");
                    require(observed.response()!=null&&observed.response().equals(evidence.path("result").path("finalResponse")),"FINAL_RESPONSE_CHANGED_BY_OBSERVER");
                    require(observed.artifactRefs().contains(policy.root.relativize(capture).toString()),"NATIVE_CAPTURE_UNLINKED");
                }
                if(observed.driverStatus()!=StepResult.DriverStatus.EXECUTED) {
                    var refs=new ArrayList<>(observed.artifactRefs());String ref=policy.root.relativize(capture).toString();
                    if(!refs.contains(ref))refs.add(ref);
                    return new StepResult(id,observed.driverStatus(),observed.data(),observed.response(),observed.reason(),observed.provenance(),refs);
                }
                return observed;
            }
        } catch(Exception|AssertionError failure) {
            // Deliberately suppress uncontrolled CLI/transport exception messages: they may contain credentials.
            String code=failure instanceof IOException||failure instanceof IllegalStateException?failure.getMessage():failure.getClass().getSimpleName();
            if(code==null||!code.matches("[A-Z0-9_]{1,100}"))code="NATIVE_CLIENT_OR_OBSERVER_FAILED";
            if(capture!=null)try{
                evidence.put("elapsedMillis",TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-began));
                evidence.putNull("cost").put("costUnknownReason","Native subscription client supplies no monetary charge evidence");
                if(!evidence.has("result"))evidence.putNull("usage").put("usageUnknownReason","Execution did not produce a completed native result");
                evidence.put("clientStatus","FAILED").put("failureCode",code).put("finishedAt",Instant.now().toString());writeCapture(capture,evidence);}catch(Exception ignored){}
            StepResult missing=StepResult.missing(id,code);
            return new StepResult(id,StepResult.DriverStatus.UNAVAILABLE,null,null,code,missing.provenance(),
                capture!=null&&Files.isRegularFile(capture)?List.of(policy.root.relativize(capture).toString()):List.of());
        }
    }
    static void writeCapture(Path file,JsonNode evidence)throws IOException {
        rejectSecrets(evidence);
        Path temporary=Files.createTempFile(file.getParent(),"safe-capture-",".json",PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        try{Files.writeString(temporary,Json.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(evidence));
            Files.move(temporary,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        }finally{Files.deleteIfExists(temporary);}
    }
    public ObjectNode preflight() throws IOException {
        policy.subscriptionPolicy();
        try(var runtime=new RuntimeHome(policy);var client=runtime.start(Duration.ofSeconds(40))) {
            var r=client.preflight();
            r.set("resolvedConfiguration",client.probeConfiguration(runtime.workspace));
            r.put("observedAt",Instant.now().toString());r.put("clientExactVersion",VERSION).put("clientBinarySha256",runtime.binaryHash);
            r.put("configSha256",sha(RuntimeHome.CONFIG)).put("status","CLIENT_PREFLIGHT_ONLY");
            r.put("runtimeStatus","NOT_RUN").put("approvalSha256",APPROVAL_HASH).put("config",RuntimeHome.CONFIG);
            r.set("command",Json.MAPPER.valueToTree(client.command));return r;
        }
    }
    static JsonNode toolText(String text){return Json.object().put("success",true).set("contentItems",Json.array().add(Json.object().put("type","inputText").put("text",text)));}
    static JsonNode skillTool(){
        var schema=Json.object().put("type","object").put("additionalProperties",false);
        schema.set("properties",Json.object().set("path",Json.object().put("type","string")));schema.set("required",Json.array().add("path"));
        return Json.object().put("type","function").put("name","load_operational_skill").put("description","Read an approved agents/skills/<name>/SKILL.md or its reference by repository-relative path").set("inputSchema",schema);
    }
    /** No raw credentials are read or copied by this harness; only Codex follows this link. */
    static final class RuntimeHome implements AutoCloseable {
        static final String CONFIG="""
            model = "gpt-6.1-sol"
            model_provider = "openai"
            service_tier = "ultrafast"
            model_reasoning_effort = "high"
            forced_login_method = "chatgpt"
            cli_auth_credentials_store = "file"
            approval_policy = "never"
            sandbox_mode = "read-only"
            web_search = "disabled"
            project_doc_max_bytes = 0
            [analytics]
            enabled = false
            [features]
            shell_tool = false
            unified_exec = false
            apps = false
            plugins = false
            hooks = false
            multi_agent = false
            multi_agent_v2 = false
            memories = false
            skill_search = false
            skip_host_skill_discovery = true
            skill_mcp_dependency_install = false
            code_mode = false
            code_mode_host = false
            browser_use = false
            browser_use_external = false
            computer_use = false
            image_generation = false
            in_app_browser = false
            in_app_local_automation = false
            daemon_auto_start = false
            unbounded_connection_retries = false
            ultrafast_mode = true
            """;
        final Path root,home,workspace,binary;
        final String binaryHash;
        final Map<String,String> env;
        RuntimeHome(NativeClientPolicy policy)throws IOException {
            binary=Path.of(System.getProperty("verification.nativeClient.codex","/opt/homebrew/bin/codex")).toRealPath();
            require(Files.isExecutable(binary),"CODEX_NOT_EXECUTABLE");binaryHash=Json.sha256(binary);
            Path original=Path.of(System.getenv().getOrDefault("CODEX_HOME",System.getProperty("user.home")+"/.codex"));
            Path auth=original.resolve("auth.json").toAbsolutePath();
            require(Files.isRegularFile(auth),"EXISTING_FILE_AUTH_UNAVAILABLE_NO_COPY_OR_KEYCHAIN_FALLBACK");
            root=Files.createTempDirectory("mulino-native-");home=Files.createDirectory(root.resolve("codex"));workspace=Files.createDirectory(root.resolve("workspace"));
            Files.setPosixFilePermissions(root,PosixFilePermissions.fromString("rwx------"));
            env=environment(System.getenv(),home);
            try {
                Files.createSymbolicLink(home.resolve("auth.json"),auth);
                Files.writeString(home.resolve("config.toml"),CONFIG);
                var pb=new ProcessBuilder(binary.toString(),"--version").directory(workspace.toFile()).redirectError(ProcessBuilder.Redirect.DISCARD);
                pb.environment().clear();pb.environment().putAll(env);Process check=pb.start();
                boolean done;
                try{done=check.waitFor(5,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();check.destroyForcibly();throw new IOException("CLIENT_INTERRUPTED");}
                if(!done){check.destroyForcibly();throw new IOException("VERSION_TIMEOUT");}
                require(check.exitValue()==0&&new String(check.getInputStream().readNBytes(1024),StandardCharsets.UTF_8).trim().equals(VERSION),"EXACT_CLIENT_VERSION_MISMATCH");
            }catch(IOException|RuntimeException e){close();throw e;}
        }
        CodexAppServer start(Duration timeout)throws IOException {return new CodexAppServer(List.of(binary.toString(),"app-server","--stdio"),env,workspace,timeout);}
        @Override public void close()throws IOException {try(var paths=Files.walk(root)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
    }
    static final class ModelInputGuard {
        static void tools(JsonNode tools){Set<String> names=new HashSet<>();for(JsonNode t:tools){require(t.path("type").asText().equals("function"),"ONLY_FUNCTION_TOOLS_SUPPORTED");String name=t.path("name").asText();require(!name.isBlank()&&!name.equals("load_operational_skill")&&names.add(name),"INVALID_TOOL_NAME");rejectSecrets(t);}}
        static boolean containsTool(JsonNode tools,String name){for(JsonNode t:tools)if(t.path("name").asText().equals(name))return true;return false;}
    }
}
