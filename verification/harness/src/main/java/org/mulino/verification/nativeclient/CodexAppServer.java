package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.mulino.verification.Json;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import static org.mulino.verification.nativeclient.NativeClientPolicy.*;

/** Dedicated stdio process. No daemon, shell, API key, automatic retry or provider fallback. */
final class CodexAppServer implements AutoCloseable {
    private final Process process;
    private final BufferedWriter writer;
    private final BlockingQueue<JsonNode> incoming = new ArrayBlockingQueue<>(64);
    private final long deadline;
    private final Thread reader;
    private int sequence;
    private final Deque<JsonNode> pending = new ArrayDeque<>();
    final ArrayNode events = Json.array();
    private int eventBytes;
    private static final int MAX_CAPTURE = 16 * 1024 * 1024;
    private static final JsonNode EOF = Json.object().put("nativeEof", true);
    final List<String> command;
    CodexAppServer(List<String> command, Map<String,String> environment, Path cwd, Duration timeout) throws IOException {
        this.command = List.copyOf(command);
        deadline = System.nanoTime() + timeout.toNanos();
        var pb = new ProcessBuilder(command).directory(cwd.toFile()).redirectError(ProcessBuilder.Redirect.DISCARD);
        pb.environment().clear();pb.environment().putAll(environment);process = pb.start();
        writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        reader = Thread.ofVirtual().start(() -> {
            try (Reader stream = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)) {
                StringBuilder line = new StringBuilder(); int c;
                while ((c = stream.read()) != -1) {
                    if (c == '\n') { incoming.put(Json.MAPPER.readTree(line.toString()));line.setLength(0); }
                    else { if (line.length() >= 2 * 1024 * 1024) throw new IOException("OVERSIZE_FRAME");line.append((char)c); }
                }
            } catch (Exception ignored) { /* Never expose raw client errors/credentials. */ }
            finally { incoming.offer(EOF); }
        });
    }
    JsonNode next() throws IOException {
        try {
            long left = deadline - System.nanoTime();
            if (left <= 0) throw new IOException("CLIENT_TIMEOUT");
            JsonNode msg = incoming.poll(left, TimeUnit.NANOSECONDS);
            if (msg == null) throw new IOException("CLIENT_TIMEOUT");
            if (msg == EOF) throw new IOException("CLIENT_EXIT_OR_INVALID_FRAME");
            return msg;
        } catch (InterruptedException e) { Thread.currentThread().interrupt();throw new IOException("CLIENT_INTERRUPTED"); }
    }
    void send(JsonNode msg) throws IOException { writer.write(msg.toString());writer.newLine();writer.flush(); }
    void record(String direction, JsonNode msg) {
        try{rejectSecrets(msg);}catch(IllegalStateException sensitive){
            events.add(Json.object().put("direction",direction).put("at",Instant.now().toString())
                .put("redacted",true).put("canonicalMessageSha256",sha(msg.toString())).put("reason","SECRET_REJECTED"));
            throw sensitive;
        }
        eventBytes += msg.toString().length();require(eventBytes <= MAX_CAPTURE, "CAPTURE_LIMIT_EXCEEDED");
        var e = Json.object();e.put("direction", direction).put("at", Instant.now().toString());e.set("message", msg.deepCopy());events.add(e);
    }
    JsonNode rpc(String method, JsonNode params, boolean capture) throws IOException {
        int id = ++sequence;var request = Json.object().put("id", id).put("method", method);request.set("params",params);
        if(capture) record("sent", request);send(request);
        while (true) {
            JsonNode msg = next();
            if (msg.path("id").asInt(-1) == id && !msg.has("method")) {
                if (msg.has("error")) throw new IOException("CLIENT_RPC_REJECTED_" + method.replace('/', '_').toUpperCase(Locale.ROOT));
                if(capture) record("received", msg);return msg.path("result");
            }
            if (msg.has("id")) {
                if(method.equals("turn/start")){if(capture)record("received",msg);pending.add(msg);continue;}
                deny(msg);throw new IOException("UNEXPECTED_CLIENT_REQUEST");
            }
            if(capture) record("received",msg);
            if(method.equals("turn/start")) pending.add(msg);
        }
    }
    void initialize() throws IOException {
        var p = Json.object();p.set("clientInfo",Json.object().put("name","mulino_native_acceptance").put("version","1.0.0"));
        p.set("capabilities", Json.object().put("experimentalApi",true));rpc("initialize",p,false);
        send(Json.object().put("method","initialized"));
    }
    ObjectNode preflight() throws IOException {
        initialize();
        // Never persist account payload (email/account identifiers) or read auth/config contents.
        JsonNode account = rpc("account/read",Json.object().put("refreshToken",false),false);
        require(account.path("account").path("type").asText().equals("chatgpt"), "CHATGPT_SUBSCRIPTION_REQUIRED");
        String plan=account.path("account").path("planType").asText();
        require(!plan.contains("usage_based")&&!plan.equals("unknown")&&!plan.isBlank(),"SUBSCRIPTION_BILLING_UNCONFIRMED");
        JsonNode limits=rpc("account/rateLimits/read",Json.object(),false);
        JsonNode credit=limits.path("rateLimits").path("credits");
        require(credit.has("hasCredits")&&!credit.path("hasCredits").asBoolean()&&!credit.path("unlimited").asBoolean(),"CREDIT_FALLBACK_CANNOT_BE_EXCLUDED");
        JsonNode chosen = null;String cursor = null;int pages=0;
        do {
            require(++pages<=20,"MODEL_CATALOG_PAGINATION_LIMIT");
            var p = Json.object().put("limit",100).put("includeHidden",true);if(cursor!=null)p.put("cursor",cursor);
            JsonNode page = rpc("model/list",p,false);
            for(JsonNode m:page.path("data")) if(m.path("model").asText().equals(MODEL)) chosen=m;
            cursor=page.path("nextCursor").isTextual()?page.path("nextCursor").asText():null;
        } while (cursor != null);
        require(chosen != null,"EXACT_MODEL_UNAVAILABLE");
        boolean supported=false;for(JsonNode t:chosen.path("serviceTiers")) if(t.path("id").asText().equals(TIER)) supported=true;
        require(supported,"REQUESTED_SERVICE_TIER_NOT_ADVERTISED");
        boolean effort=false;for(JsonNode e:chosen.path("supportedReasoningEfforts"))if(e.path("reasoningEffort").asText().equals("high"))effort=true;
        require(effort,"HIGH_EFFORT_NOT_ADVERTISED");
        var r=Json.object().put("authentication","chatgpt").put("requestedModel",MODEL).put("requestedServiceTier",TIER);
        r.put("planType",plan).put("creditFallbackAvailable",false);r.set("catalogModel",chosen);r.put("actualModelCalls",0);return r;
    }
    ObjectNode probeConfiguration(Path workspace)throws IOException {
        var p=Json.object().put("model",MODEL).put("modelProvider","openai").put("serviceTier",TIER)
            .put("allowProviderModelFallback",false).put("ephemeral",true).put("cwd",workspace.toString())
            .put("baseInstructions","Configuration probe only. No turn is started.").put("developerInstructions","")
            .put("approvalPolicy","never").put("sandbox","read-only");
        p.set("dynamicTools",Json.array().add(NativeCodexClient.skillTool()));
        p.set("environments",Json.array());p.set("runtimeWorkspaceRoots",Json.array().add(workspace.toString()));
        var r=rpc("thread/start",p,false);
        require(r.path("model").asText().equals(MODEL)&&r.path("modelProvider").asText().equals("openai")
            &&r.path("serviceTier").asText().equals(TIER),"RESOLVED_MODEL_TIER_MISMATCH");
        require(r.path("instructionSources").isEmpty(),"AMBIENT_INSTRUCTIONS_REJECTED");
        return Json.object().put("model",r.path("model").asText()).put("modelProvider",r.path("modelProvider").asText())
            .put("serviceTier",r.path("serviceTier").asText()).put("reasoningEffort",r.path("reasoningEffort").asText())
            .put("dynamicToolSchemaAccepted",true).put("turnStarted",false);
    }
    ObjectNode run(JsonNode threadParams, JsonNode input, Function<JsonNode,JsonNode> tools) throws IOException {
        var start=rpc("thread/start",threadParams,true);
        require(start.path("model").asText().equals(MODEL) && start.path("serviceTier").asText().equals(TIER)
            && start.path("modelProvider").asText().equals("openai"),"RESOLVED_MODEL_TIER_MISMATCH");
        require(start.path("instructionSources").isEmpty(),"AMBIENT_INSTRUCTIONS_REJECTED");
        String thread=start.path("thread").path("id").asText();require(!thread.isBlank(),"THREAD_ID_MISSING");
        var p=Json.object().put("threadId",thread).put("model",MODEL).put("serviceTier",TIER).put("serviceTierForTurn",TIER).put("effort","high");p.set("input",input);
        long began=System.nanoTime();var turn=rpc("turn/start",p,true);
        String turnId=turn.path("turn").path("id").asText();require(!turnId.isBlank(),"TURN_ID_MISSING");
        String finalText=null;JsonNode usage=Json.MAPPER.nullNode();Set<String> callIds=new HashSet<>();
        while(true) {
            boolean recorded=!pending.isEmpty();JsonNode msg=recorded?pending.removeFirst():next();String method=msg.path("method").asText();JsonNode q=msg.path("params");
            if(!recorded) record("received",msg);
            if(msg.has("id")) {
                if(!method.equals("item/tool/call")) {deny(msg);throw new IOException("UNSUPPORTED_CLIENT_REQUEST");}
                require(q.path("threadId").asText().equals(thread)&&q.path("turnId").asText().equals(turnId),"TOOL_SCOPE_MISMATCH");
                require(!q.path("callId").asText().isBlank()&&callIds.add(q.path("callId").asText()),"DUPLICATE_OR_MISSING_TOOL_CALL");
                require(q.path("namespace").isMissingNode()||q.path("namespace").isNull()||q.path("namespace").asText().isEmpty(),"UNEXPECTED_TOOL_NAMESPACE");
                JsonNode result=invokeTool(tools,q);rejectSecrets(result);
                var reply=Json.object();reply.set("id",msg.get("id"));reply.set("result",result);record("sent",reply);send(reply);
            } else if(method.equals("thread/tokenUsage/updated")) {
                require(q.path("threadId").asText().equals(thread)&&q.path("turnId").asText().equals(turnId),"USAGE_SCOPE_MISMATCH");usage=q.path("tokenUsage").deepCopy();
            } else if(method.equals("thread/settings/updated")) {
                require((!q.hasNonNull("model")||q.path("model").asText().equals(MODEL))
                    &&(!q.hasNonNull("serviceTier")||q.path("serviceTier").asText().equals(TIER)),"RUNTIME_MODEL_TIER_CHANGED");
            } else if(method.equals("item/completed")||method.equals("item/started")) {
                require(q.path("threadId").asText().equals(thread)&&q.path("turnId").asText().equals(turnId),"ITEM_SCOPE_MISMATCH");
                var item=q.path("item");String type=item.path("type").asText();
                require(Set.of("agentMessage","userMessage","reasoning","dynamicToolCall","plan").contains(type),"UNEXPECTED_BUILTIN_TOOL");
                if(method.equals("item/completed")&&type.equals("agentMessage")&&item.path("phase").asText().equals("final_answer"))finalText=item.path("text").asText();
            } else if(method.equals("turn/completed")) {
                require(q.path("threadId").asText().equals(thread)&&q.path("turn").path("id").asText().equals(turnId),"TURN_SCOPE_MISMATCH");
                require(q.path("turn").path("status").asText().equals("completed"),"TURN_NOT_COMPLETED");
                require(finalText!=null,"FINAL_RESPONSE_NOT_OBSERVED");break;
            } else if(method.equals("error")) throw new IOException("CLIENT_TURN_ERROR_NO_RETRY");
        }
        var result=Json.object().put("threadId",thread).put("turnId",turnId).put("finalResponse",finalText)
            .put("latencyMillis",TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-began));
        result.set("usage",usage);if(usage.isNull())result.put("usageUnknownReason","Native client did not emit token usage");result.putNull("cost").put("costUnknownReason","Native subscription client does not report per-turn monetary charges; no API price inference");
        result.put("resolvedModel",start.path("model").asText()).put("resolvedServiceTier",start.path("serviceTier").asText());
        result.put("identityEvidence","app-server thread/start response; not an independent server billing attestation");return result;
    }
    private JsonNode invokeTool(Function<JsonNode,JsonNode> tools,JsonNode request)throws IOException {
        var task=new FutureTask<JsonNode>(() -> tools.apply(request));Thread.ofVirtual().start(task);
        try{return task.get(Math.max(1,deadline-System.nanoTime()),TimeUnit.NANOSECONDS);}
        catch(TimeoutException e){task.cancel(true);throw new IOException("TOOL_TIMEOUT_UNKNOWN_RESULT");}
        catch(InterruptedException e){task.cancel(true);Thread.currentThread().interrupt();throw new IOException("CLIENT_INTERRUPTED");}
        catch(ExecutionException e){throw new IOException("PRODUCT_TOOL_FAILED_NO_RETRY");}
    }
    private void deny(JsonNode msg) throws IOException {
        var reply=Json.object();reply.set("id",msg.get("id"));reply.set("error",Json.object().put("code",-32601).put("message","Host request not allowed"));send(reply);
    }
    long pid(){return process.pid();}
    @Override public void close() {
        try {writer.close();}catch(IOException ignored){}
        var children=process.descendants().toList();children.forEach(ProcessHandle::destroy);process.destroy();
        try {if(!process.waitFor(2,TimeUnit.SECONDS))process.destroyForcibly();process.waitFor(2,TimeUnit.SECONDS);}
        catch(InterruptedException e){Thread.currentThread().interrupt();process.destroyForcibly();}
        children.stream().filter(ProcessHandle::isAlive).forEach(ProcessHandle::destroyForcibly);reader.interrupt();
        var termination=Json.object().put("processId",process.pid()).put("termination","HOST_CLOSED_DEDICATED_PROCESS");
        if(!process.isAlive())termination.put("exitCode",process.exitValue());else termination.putNull("exitCode");
        record("lifecycle",termination);
    }
}
