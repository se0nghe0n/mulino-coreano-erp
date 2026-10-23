package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.mulino.verification.*;
import org.mulino.verification.actual.*;
import java.nio.file.*;
import java.util.*;

/** Product transport is usable without a model. Model execution additionally needs every independent observation.
 * No expectation, corpus or planned intent enters a transport request or is substituted for a missing observation. */
public final class NativeProductIntegration implements NativeClientIntegration {
    private final Path root;
    private final NativeIntegrationEvidence evidence;
    private final AcceptanceDriver driver;
    private final NativeMcpTransport transport;
    public NativeProductIntegration() {
        this(Path.of(System.getProperty("repo.root",".")),System.getenv());
    }
    NativeProductIntegration(Path root,Map<String,String> environment) {
        this.root=root.toAbsolutePath().normalize();evidence=new NativeIntegrationEvidence(this.root);
        ActualConfiguration config=null;
        try {config=ActualConfiguration.environment(environment);}catch(IllegalArgumentException missing) { /* Unconfigured discovery is safe. */ }
        if(config==null) {transport=null;driver=new UnimplementedDriver();}
        else {transport=new NativeMcpTransport(config,evidence);driver=new ProductDriver(config);}
    }
    @Override public AcceptanceDriver driver(){return driver;}
    @Override public void requireReady(String caseId,String turnId) {
        new NativeDeterministicGate(root).requireReady();
        NativeClientPolicy.require(transport!=null,"ACTUAL_PRODUCT_CONFIGURATION_MISSING");
        // These interfaces are absent in this product/schema baseline. A config flag cannot override them.
        throw new IllegalStateException("NATIVE_INDEPENDENT_OBSERVATIONS_NOT_IMPLEMENTED");
    }
    @Override public Session open(String action,String route,JsonNode actor,String utterance,JsonNode permittedContext) {
        NativeClientPolicy.input(utterance,permittedContext);
        // Direct ActualClientPort calls cannot evade the corpus prerequisite gate.
        requireReady("DIRECT",action);
        return new ProductSession(action,actor,permittedContext);
    }
    private final class ProductSession implements Session {
        private final String action;
        private final JsonNode actor,scope;
        private ArrayNode exposed;
        ProductSession(String action,JsonNode actor,JsonNode context) {
            this.action=action;this.actor=actor.deepCopy();this.scope=context.path("scope").deepCopy();
            NativeClientPolicy.require(scope.isObject()&&scope.hasNonNull("installationId"),"AUTHENTICATED_CONTEXT_SCOPE_MISSING");
        }
        public void requireReady(){NativeProductIntegration.this.requireReady("DIRECT",action);}
        public JsonNode tools() {
            requireReady();
            try {exposed=transport.tools(transport.send(action,"tools/list",null,Json.object(),actor,scope));return exposed.deepCopy();}
            catch(Exception e){throw new IllegalStateException("PRODUCT_TOOL_DISCOVERY_UNAVAILABLE");}
        }
        public JsonNode call(JsonNode request) {
            requireReady();
            NativeClientPolicy.require(exposed!=null&&NativeCodexClient.ModelInputGuard.containsTool(exposed,request.path("tool").asText()),"UNDECLARED_PRODUCT_TOOL");
            try {return NativeMcpTransport.nativeReply(transport.send(action,"tools/call",request.path("tool").asText(),request.path("arguments"),actor,scope));}
            catch(Exception e){throw new IllegalStateException("PRODUCT_TOOL_TRANSPORT_UNAVAILABLE");}
        }
        public StepResult observe(Path capture,JsonNode nativeEvidence) {
            try{return NativeFinalResponseObserver.observe(action,scope,capture,nativeEvidence,evidence);}
            catch(Exception e){return StepResult.missing(action,"NATIVE_FINAL_RESPONSE_OBSERVATION_UNAVAILABLE");}
        }
    }
    /** Exact source evidence explaining why the full SPI remains unavailable, without issuing a model call. */
    ObjectNode readiness() throws Exception {
        var r=evidence.base("NATIVE_INTEGRATION_READINESS",Json.object()).put("runtimeStatus","NOT_RUN")
            .put("actualModelCalls",0).put("incrementalApiCostCap","0").put("transportConfigured",transport!=null);
        try{new NativeDeterministicGate(root).requireReady();r.put("deterministicGate","PASS");}
        catch(IllegalStateException missing){r.put("deterministicGate",missing.getMessage());}
        r.set("missingInterfaces",Json.array()
            .add("Model fixture aliases are UUID values and baseline.domainFacts/principalFacts/grantFacts have no strict complete installer")
            .add("getObject does not support view=ACTOR_PERMITTED_BUSINESS_CONTEXT or case/installation/turn scope")
            .add("MCP has no server-authenticated principal/grant capture hook")
            .add("World/context projection needs independent row recipe; simple-object SHA proof and MVCC separation implemented")
            .add("Physical rawRows/cells contract resolved; semantic selectors and effect attribution still incomplete")
            .add("Full immutable effect/obligation/physical-scope model-binding mapping")
            .add("Raw host and bounded Korean semantic extraction exist; full host process contract and arbitrary prose remain unavailable"));
        var sources=Json.array();for(String path:List.of(
            "backend/src/main/java/com/mulino/adapters/OntologyMcp.java",
            "backend/src/main/java/com/mulino/application/core/QueryRequests.java",
            "backend/src/main/java/com/mulino/application/core/ApplicationQueries.java",
            "verification/harness/src/main/java/org/mulino/verification/actual/ScenarioFixtureInstaller.java",
            "contracts/acceptance-observation.schema.json",
            "verification/model-binding/physical-columns.schema.json",
            "verification/model-binding/final-response-observation.schema.json"))sources.add(evidence.source(path));
        r.set("sourceArtifacts",sources);return r;
    }
    private final class ProductDriver implements AcceptanceDriver {
        private final ActualAcceptanceDriver actual;
        private final NativePhysicalObserver observer;
        private final ModelFixtureInstaller modelFixtures;
        private JsonNode installedScope;
        ProductDriver(ActualConfiguration config) {actual=new ActualAcceptanceDriver(root,config);observer=new NativePhysicalObserver(config,evidence);modelFixtures=new ModelFixtureInstaller(root,config,System.getProperty("verification.actual.blobRoot")==null?null:Path.of(System.getProperty("verification.actual.blobRoot")));}
        public Set<String> availableAdapters(){return Set.of("api","mcp","fixture","db");}
        public StepResult installFixture(String id,JsonNode bundle) {
            installedScope=null;
            JsonNode fixture=bundle.path("fixture");
            // S1/S2 fixture installer is reusable for its declared typed shape. Do not erase model-only facts.
            boolean typed=fixture.path("aliases").isObject()&&!fixture.path("aliases").isEmpty();
            for(JsonNode alias:fixture.path("aliases"))typed&=alias.isObject()&&alias.hasNonNull("type");
            if(fixture.path("baseline").has("domainFacts")) {
                try {
                    var data=modelFixtures.install(bundle);
                    installedScope=Json.object().put("organizationId",data.path("aliasMap").path("org").asText()).put("installationId",data.path("installationId").asText());
                    var artifact=evidence.save("model-fixture-installation",data);
                    var provenance=Json.object().put("adapter",ModelFixtureInstaller.VERSION).put("adapterVersion","1.0.0").put("buildVersion",System.getenv().getOrDefault("ACTUAL_BUILD_COMMIT","UNKNOWN")).putNull("authenticatedActor").put("source","JDBC_FIXTURE_INSTALL").put("independent",false).put("scopeComplete",data.path("fixtureComplete").asBoolean()).putNull("sourceQuery").putNull("snapshot");
                    return new StepResult(id,StepResult.DriverStatus.EXECUTED,data,null,null,provenance,List.of(artifact.path("path").asText()));
                }catch(ModelFixtureInstaller.MappingException incomplete) {
                    return missing(id,"MODEL_FIXTURE_EXPLICIT_INPUTS_REQUIRED",incomplete.inventory());
                }catch(Exception failure){throw new IllegalStateException("Model fixture transaction failed",failure);}
            }
            if(!typed)return missing(id,"MODEL_FIXTURE_COMPLETE_MAPPING_NOT_IMPLEMENTED",bundle);
            StepResult result=actual.installFixture(id,bundle);
            if(result.driverStatus()==StepResult.DriverStatus.EXECUTED) {
                String orgAlias=fixture.path("actors").elements().next().path("organizationAlias").asText();
                installedScope=Json.object().put("organizationId",result.data().path("aliasMap").path(orgAlias).asText()).put("installationId",UUID.randomUUID().toString());
                var data=(ObjectNode)result.data().deepCopy();data.set("installationId",installedScope.path("installationId"));
                return new StepResult(id,result.driverStatus(),data,result.response(),result.reason(),result.provenance(),result.artifactRefs());
            }return result;
        }
        public StepResult query(String id,String route,JsonNode actor,String query,JsonNode request){
            if(!NativePermittedContext.matches(query,request))return send(id,route,actor,query,request);
            if(!route.equalsIgnoreCase("MCP"))return StepResult.missing(id,"NATIVE_ROUTE_NOT_IMPLEMENTED");
            try {
                return NativePermittedContext.query(id,actor,request,installedScope,
                    (operation,input)->transport.send(id,"tools/call",operation,input,actor,request.path("scope")),evidence);
            }catch(Exception failure){return StepResult.missing(id,"NATIVE_PERMITTED_CONTEXT_UNAVAILABLE");}
        }
        public StepResult invoke(String id,String route,JsonNode actor,String capability,JsonNode request){return send(id,route,actor,capability,request);}
        private StepResult send(String id,String route,JsonNode actor,String operation,JsonNode request) {
            if(!route.equalsIgnoreCase("MCP"))return StepResult.missing(id,"NATIVE_ROUTE_NOT_IMPLEMENTED");
            try {
                if(installedScope==null)return StepResult.missing(id,"NATIVE_INSTALLATION_SCOPE_MISSING");
                var capture=transport.send(id,"tools/call",operation,request,actor,installedScope);
                try{return NativePermittedContextCapture.ordinary(id,actor,installedScope,capture,evidence);}
                catch(Exception unavailable){return missing(id,"SERVER_AUTHENTICATED_CAPTURE_NOT_IMPLEMENTED",capture.response(),capture.artifact());}
            }catch(Exception failure){return StepResult.missing(id,"NATIVE_PRODUCT_TRANSPORT_UNAVAILABLE");}
        }
        public StepResult observe(String id,JsonNode request) {
            try {
                if(installedScope==null||!request.path("scope").path("organizationId").equals(installedScope.path("organizationId")))
                    return StepResult.missing(id,"NATIVE_INSTALLATION_SCOPE_MISMATCH");
                if(!request.path("scope").path("installationId").equals(installedScope.path("installationId")))
                    return StepResult.missing(id,"NATIVE_INSTALLATION_SCOPE_MISMATCH");
                var capture=request.has("projectionRequest")
                    ?observer.capture(request.path("scope"),Json.required(request,"asOf"),Json.required(request,"knownAt"),request.path("projectionRequest"),Json.required(request,"projectionActorId"))
                    :observer.capture(request.path("scope"),Json.required(request,"asOf"),Json.required(request,"knownAt"));
                return missing(id,"MODEL_BINDING_OBSERVATION_NOT_IMPLEMENTED",capture);
            }catch(Exception failure){return StepResult.missing(id,"NATIVE_POSTGRESQL_OBSERVATION_UNAVAILABLE");}
        }
        public StepResult control(String id,JsonNode control){return actual.control(id,control);}
        public StepResult start(String id,String route,JsonNode actor,String capability,JsonNode request){return StepResult.missing(id,"NATIVE_ASYNC_NOT_IMPLEMENTED");}
        public StepResult await(String id,JsonNode handle,int timeout){return StepResult.missing(id,"NATIVE_ASYNC_NOT_IMPLEMENTED");}
    }
    private StepResult missing(String id,String reason,JsonNode diagnostic,JsonNode... captures) {
        try {
            var item=evidence.base("NATIVE_INTEGRATION_DIAGNOSTIC",Json.object()).put("actionId",id).put("status","NOT_IMPLEMENTED").put("reason",reason);
            // Hash uninstalled fixture bytes rather than publishing full authority/context as an execution artifact.
            if(reason.equals("MODEL_FIXTURE_COMPLETE_MAPPING_NOT_IMPLEMENTED")) {
                item.put("inputSha256",NativeClientPolicy.sha(diagnostic.toString()));
                if(diagnostic.has("fixtureHash"))item.set("submittedFixtureHash",diagnostic.path("fixtureHash"));
            }
            else item.set("observation",diagnostic);
            var refs=new ArrayList<String>();for(JsonNode capture:captures)refs.add(capture.path("path").asText());
            item.set("capturedArtifacts",Json.MAPPER.valueToTree(captures));refs.add(evidence.save("missing",item).path("path").asText());
            StepResult missing=StepResult.missing(id,reason);return new StepResult(id,missing.driverStatus(),null,null,reason,missing.provenance(),refs);
        }catch(Exception failure){return StepResult.missing(id,reason);}
    }
}
