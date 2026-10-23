package org.mulino.verification.nativeclient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mulino.verification.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Contract doubles only. These tests never start a real model or claim UAT coverage. */
class NativeClientContractTest {
    @TempDir Path tmp;
    private CodexAppServer server(String scenario,Duration timeout)throws Exception {
        Path script=tmp.resolve("fake.py");Files.writeString(script,FAKE);
        return new CodexAppServer(List.of("python3",script.toString(),scenario),Map.of("PATH",System.getenv("PATH")),tmp,timeout);
    }
    private JsonNode params(){return Json.object().put("model",NativeClientPolicy.MODEL).put("serviceTier",NativeClientPolicy.TIER);}
    @Test void boundedProcessObservesActualEventsFinalTextUsageAndToolRoundTrip()throws Exception {
        try(var server=server("ok",Duration.ofSeconds(5))){
            assertEquals(0,server.preflight().path("actualModelCalls").asInt());
            var calls=new ArrayList<JsonNode>();
            var result=server.run(params(),Json.array(),r->{calls.add(r);return NativeCodexClient.toolText("observed product reply");});
            assertEquals(1,calls.size());assertEquals("실제 답변",result.path("finalResponse").asText());
            assertEquals(42,result.path("usage").path("total").path("totalTokens").asInt());
            assertTrue(result.path("cost").isNull());assertTrue(server.events.size()>5);
        }
    }
    @Test void callbacksBeforeTurnStartResponseAreNotDropped()throws Exception {
        try(var server=server("early",Duration.ofSeconds(5))){server.preflight();
            assertEquals("실제 답변",server.run(params(),Json.array(),r->NativeCodexClient.toolText("ok")).path("finalResponse").asText());}
    }
    @Test void mismatchedTierFailsBeforeStartingModel()throws Exception {
        try(var server=server("wrong-tier",Duration.ofSeconds(5))){server.preflight();
            assertEquals("RESOLVED_MODEL_TIER_MISMATCH",assertThrows(IllegalStateException.class,()->server.run(params(),Json.array(),r->null)).getMessage());
            assertFalse(server.events.toString().contains("turn/start"));}
    }
    @Test void unavailableTierAndApiAuthAndCreditsFailBeforeModel()throws Exception {
        for(String scenario:List.of("api","no-tier","credits"))try(var server=server(scenario,Duration.ofSeconds(5))){
            assertThrows(IllegalStateException.class,server::preflight);assertFalse(server.events.toString().contains("turn/start"));}
    }
    @Test void timeoutKillsProcessWithoutRetry()throws Exception {
        long pid;try(var server=server("hang",Duration.ofMillis(250))){pid=server.pid();assertThrows(java.io.IOException.class,server::preflight);}
        assertFalse(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false));
    }
    @Test void blockedProductToolAlsoHonorsOverallDeadline()throws Exception {
        try(var server=server("ok",Duration.ofMillis(400))){server.preflight();
            assertEquals("TOOL_TIMEOUT_UNKNOWN_RESULT",assertThrows(java.io.IOException.class,()->server.run(params(),Json.array(),r->{
                try{Thread.sleep(10000);}catch(InterruptedException e){Thread.currentThread().interrupt();}
                return NativeCodexClient.toolText("late");})).getMessage());}
    }
    @Test void invalidFrameAndPrematureExitFail()throws Exception {
        for(String scenario:List.of("invalid","exit"))try(var server=server(scenario,Duration.ofSeconds(2))){assertThrows(java.io.IOException.class,server::preflight);}
    }
    @Test void toolScopeAndMissingFinalResponseFail()throws Exception {
        for(String scenario:List.of("wrong-scope","no-final","settings-change"))try(var server=server(scenario,Duration.ofSeconds(5))){server.preflight();assertThrows(IllegalStateException.class,()->server.run(params(),Json.array(),r->NativeCodexClient.toolText("ok")));}
    }
    @Test void inputDoesNotAcceptOracleFixtureTitleOrCredentials(){
        for(String key:List.of("oracle","expectedIntent","title","fixture","requirementRefs","authorization","api_key")){
            var ctx=Json.object().set("nested",Json.object().put(key,"forbidden"));
            assertThrows(RuntimeException.class,()->NativeClientPolicy.input("사용자 발화",ctx));
        }
        var ctx=Json.object().put("objectId","real-id");
        assertEquals(ctx,NativeClientPolicy.input("사용자 발화",ctx).path("permittedContext"));
        assertThrows(IllegalStateException.class,()->NativeClientPolicy.input("Bearer abcdefghijklmnop",ctx));
    }
    @Test void sensitiveNativeEventRetainsOnlyDigestAndNoSecret()throws Exception {
        try(var server=server("ok",Duration.ofSeconds(5))){
            assertThrows(IllegalStateException.class,()->server.record("received",Json.object().put("authorization","secret")));
            assertTrue(server.events.get(0).path("redacted").asBoolean());
            assertFalse(server.events.toString().contains("secret"));
            assertTrue(server.events.get(0).has("canonicalMessageSha256"));
        }
    }
    @Test void secretEnvironmentNeverReachesClient(){
        var env=NativeClientPolicy.environment(Map.of("PATH","/bin","HOME","/home/u","OPENAI_API_KEY","secret","ANTHROPIC_API_KEY","secret","OPENAI_BASE_URL","evil","HTTPS_PROXY","evil","CODEX_HOME","evil"),tmp);
        assertEquals(Set.of("PATH","HOME","CODEX_HOME"),env.keySet());assertEquals(tmp.toString(),env.get("CODEX_HOME"));
    }
    @Test void actualEntryPointWithoutExplicitEnablementRemainsNotRun()throws Exception {
        assertEquals(2,org.mulino.verification.modelbinding.ModelBindingActualMain.execute(Path.of(System.getProperty("repo.root"))));
    }
    @Test void approvalIsPinnedAndServiceRegistrationCannotBypassMissingIntegration()throws Exception {
        var policy=new NativeClientPolicy(Path.of(System.getProperty("repo.root")));assertEquals("R8",policy.subscriptionPolicy().path("decisionRef").asText());
        var provider=new NativeModelBindingProvider();assertThrows(IllegalStateException.class,()->provider.executionGate().requireAuthorized("M01","turn-1"));
        AgentRunner runner=AgentRunner.fromProperty("actual");assertTrue(runner.providedAdapters().isEmpty());
        var client=new NativeCodexClient(policy,null);assertNotEquals(StepResult.DriverStatus.EXECUTED,client.execute("a","MCP",Json.object(),"읽어줘",Json.object()).driverStatus());
    }
    @Test void runtimePackagesHaveDirectDiscoveryLinksAndMatchingContentHashes()throws Exception {
        Path root=Path.of(System.getProperty("repo.root")).toRealPath();
        var policy=new NativeClientPolicy(root);
        var manifest=Json.read(root.resolve("agents/runtime-package-manifest.json"));
        assertEquals(NativeClientPolicy.PACKAGES.size(),manifest.path("packages").size());
        assertEquals(manifest.path("skillSha256").asText(),policy.skillHash());
        var files=policy.skillFiles();
        assertEquals(13,files.size());
        for(var entry:manifest.path("packages")) {
            String name=entry.path("name").asText();
            assertTrue(NativeClientPolicy.PACKAGES.contains(name));
            Path base=root.resolve(entry.path("path").asText());
            assertFalse(Files.isSymbolicLink(base));
            for(String directory:List.of(".agents/skills/", ".claude/skills/")) {
                Path link=root.resolve(directory+name);
                assertTrue(Files.isSymbolicLink(link));
                assertEquals(Path.of("../../agents/skills/"+name),Files.readSymbolicLink(link));
                assertEquals(base.toRealPath(),link.toRealPath());
            }
            var hashes=Json.object();
            var fields=entry.path("files").fields();
            while(fields.hasNext()) {
                var file=fields.next();Path path=root.resolve(file.getKey());
                assertTrue(files.contains(path));
                assertEquals(file.getValue().asText(),Json.sha256(path));
                hashes.put(file.getKey(),Json.sha256(path));
            }
            assertEquals(entry.path("sha256").asText(),NativeClientPolicy.sha(hashes.toString()));
        }
        var approval=policy.approval();
        assertEquals("gpt-6.1-sol",approval.path("modelExactId").asText());
        assertEquals("priority",approval.path("serviceTier").asText());
        assertFalse(approval.path("implementationModelCallsAllowed").asBoolean());
        assertEquals("0",approval.path("approvedCostCap").asText());
    }
    @Test void missingOrEscapedRuntimePackageFailsClosed()throws Exception {
        var policy=new NativeClientPolicy(tmp);
        assertEquals("RUNTIME_SKILL_MISSING",assertThrows(IllegalStateException.class,policy::skillFiles).getMessage());
        Path outside=Files.createTempDirectory("mulino-skill-contract-");
        try {
            Files.writeString(outside.resolve("SKILL.md"),"unapproved");
            Path parent=Files.createDirectories(tmp.resolve("agents/skills"));
            Files.createSymbolicLink(parent.resolve(NativeClientPolicy.PACKAGES.getFirst()),outside);
            assertEquals("SKILL_ROOT_ESCAPE",assertThrows(IllegalStateException.class,policy::skillFiles).getMessage());
        } finally {Files.deleteIfExists(outside.resolve("SKILL.md"));Files.deleteIfExists(outside);}
    }
    private static final String FAKE="""
        import json,sys,time
        scenario=sys.argv[1]
        if scenario=='hang': time.sleep(60)
        if scenario=='exit': sys.exit(3)
        if scenario=='invalid': print('not json',flush=True);sys.exit(0)
        def emit(o): print(json.dumps(o,ensure_ascii=False),flush=True)
        def notice(m,p): emit({'method':m,'params':p})
        def tool(): emit({'id':99,'method':'item/tool/call','params':{'threadId':'wrong' if scenario=='wrong-scope' else 'thread','turnId':'turn','callId':'call','tool':'getObject','arguments':{}}})
        for line in sys.stdin:
            q=json.loads(line);m=q.get('method');r={}
            if m=='initialized':continue
            if 'result' in q:
                if scenario=='settings-change':notice('thread/settings/updated',{'threadId':'thread','model':'wrong','serviceTier':'priority'})
                if scenario!='no-final':notice('item/completed',{'threadId':'thread','turnId':'turn','item':{'id':'final','type':'agentMessage','phase':'final_answer','text':'실제 답변'}})
                notice('thread/tokenUsage/updated',{'threadId':'thread','turnId':'turn','tokenUsage':{'total':{'totalTokens':42}}})
                notice('turn/completed',{'threadId':'thread','turn':{'id':'turn','status':'completed'}});continue
            if m=='account/read':r={'account':{'type':'apiKey' if scenario=='api' else 'chatgpt','planType':'pro'}}
            if m=='account/rateLimits/read':r={'rateLimits':{'credits':{'hasCredits':scenario=='credits','unlimited':False}}}
            if m=='model/list':r={'data':[{'model':'gpt-6.1-sol','supportedReasoningEfforts':[{'reasoningEffort':'high'}],'serviceTiers':[] if scenario=='no-tier' else [{'id':'priority'}]}]}
            if m=='thread/start':r={'model':'gpt-6.1-sol','modelProvider':'openai','serviceTier':'ultrafast' if scenario=='wrong-tier' else 'priority','instructionSources':[],'thread':{'id':'thread'}}
            if m=='turn/start':
                r={'turn':{'id':'turn'}}
                if scenario=='early':tool()
            emit({'id':q['id'],'result':r})
            if m=='turn/start' and scenario!='early':tool()
        """;
}
