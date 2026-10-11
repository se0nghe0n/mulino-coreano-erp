package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.http.*;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.mulino.verification.*;

/** Calls the product and captures its unmodified JSON response; never loads an oracle. */
public final class ActualAcceptanceDriver implements AcceptanceDriver, IndependentDbObserver {
    private static final Set<String> INTENT_FIELDS=Set.of("intentKind","definitionVersion","capabilityVersion","capabilityId","subjectRefs","slots","conditions","evidenceRefs","sourceRefs","contextRefs","provenance","conversationRequestId","proposalRevision","canonicalIntentHash","commandIdempotencyKey","expectedRevision","approvalId");
    static final String MCP_VERSION="2026-07-28";
    private static final Set<String> QUERIES=Set.of("getObject","getWork","getInventory","getObligations","traceLot","getAssessment","getEvidence","getDefinition","searchObjects","searchWorks","getInbox","searchOperationalIssues");
    private final Path root;
    private final ActualConfiguration configuration;
    private final JwtSigner signer;
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
    private final String run=UUID.randomUUID().toString();
    private final Map<String,CompletableFuture<StepResult>> pending=new ConcurrentHashMap<>();
    /** Barrier races hold several submitted calls open at once; the common pool may have a single worker. */
    private final ExecutorService asyncCalls=Executors.newCachedThreadPool(r->{var t=new Thread(r,"actual-async-call");t.setDaemon(true);return t;});
    private volatile JsonNode controlActor;
    private final FixtureInstaller fixtures;
    private final HostProcessControls hostControls;
    private final SchedulerHostControls schedulerControls;
    private final JdbcObservation observer;
    public ActualAcceptanceDriver(Path root,ActualConfiguration configuration) {
        this.root=root;this.configuration=configuration;signer=new JwtSigner(configuration);
        fixtures=new FixtureInstaller(configuration);observer=new JdbcObservation(configuration);hostControls=new HostProcessControls(root,run);schedulerControls=new SchedulerHostControls(root,run);
    }
    public static ActualAcceptanceDriver fromEnvironment(Path root) {return new ActualAcceptanceDriver(root,ActualConfiguration.environment(System.getenv()));}
    @Override public Set<String> availableAdapters() {return suiteIsolation?Set.of("api","mcp","wire","direct","batch","management","blob","clock","barrier","host","process","coverage","fixture","db"):Set.of("api","fixture","db");}
    @Override public StepResult installFixture(String id,JsonNode bundle) {
        // A new installation is an isolation boundary: never carry the previous subcase's organization or control identity.
        if(suiteIsolation){externalOrganization=null;externalOrganizations=null;controlActor=null;installedActors=null;idTypes=null;}
        try {var bound=ActualFixtureBindings.bind(root,bundle,configuration);
            // Suite isolation: each installed fixture is a fresh synthetic organization with its own external alias,
            // because one disposable database serves every subcase. The authored alias key stays the contract.
            ObjectNode data;
            if(suiteIsolation) {
                ((ObjectNode)bound).put("organizationAliasSuffix",run.substring(0,8)+"-"+installs.incrementAndGet());
                data=ScenarioFixtureInstaller.install(configuration,root,bound,Boolean.getBoolean("verification.actual.partialFixtures"));
                externalOrganization=data.path("organizationExternalAlias").asText();
                externalOrganizations=data.path("organizationExternalAliases");
                var orgIds=new ArrayList<String>();data.path("organizationExternalAliases").fieldNames().forEachRemaining(o->orgIds.add(data.path("aliasMap").path(o).asText()));
                ScenarioRevisionBinding.reset(orgIds);
            } else data=fixtures.install(bound);
            // The control identity is the first installed actor the backend can authenticate (never a declared untrusted identity).
            for(JsonNode candidate:bound.path("fixture").path("actors")){if(candidate.path(ActualFixtureBindings.UNTRUSTED).asBoolean(false))continue;var control=(ObjectNode)candidate.deepCopy();String mapped=data.path("organizationExternalAliases").path(control.path("organizationAlias").asText()).asText(data.path("organizationExternalAlias").asText(null));if(mapped!=null)control.put("organizationAlias",mapped);controlActor=control;break;}
            if(suiteIsolation)idTypes=data.path("idTypes");
            if(suiteIsolation){installedActors=data.path("installedActors").isObject()?data.path("installedActors"):bound.path("fixture").path("actors");fixtureStart(bound.path("fixture"));}
            return executed(id,data,null,provenance(null,"JDBC_FIXTURE_INSTALL",false,null,null),data);}
        catch(UnsupportedOperationException unsupported){return StepResult.missing(id,"NOT_IMPLEMENTED: "+unsupported.getMessage());}
        catch(Exception failure){throw new IllegalStateException("Actual fixture transaction failed: "+SqlFailureSummary.safe(failure),failure);}
    }
    /** Explicit suite mode (Main scenarios): fresh organization alias and fixture-start clock per installed fixture. */
    private final boolean suiteIsolation=Boolean.getBoolean("verification.actual.suiteIsolation");
    private final boolean relaxWire=Boolean.getBoolean("verification.actual.relaxWire");
    private final java.util.concurrent.atomic.AtomicInteger installs=new java.util.concurrent.atomic.AtomicInteger();
    private volatile String externalOrganization;
    private volatile JsonNode externalOrganizations;
    /** Contract: the product clock starts at the fixture clock asOf; installed rows are recorded at or before it. */
    private void fixtureStart(JsonNode fixture) throws Exception {
        String start=Json.required(fixture.path("clock"),"asOf");
        if(controlActor==null)throw new IllegalStateException("Fixture-start clock requires an installed control identity");
        var body=Json.object();body.put("instant",start);
        var uri=configuration.baseUri().resolve("/__verification/runtime/clock/fixture-start");
        var call=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).header("Content-Type","application/json").header("Authorization","Bearer "+signer.sign(controlActor)).POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        var result=http.send(call,HttpResponse.BodyHandlers.ofString());
        if(result.statusCode()!=200)throw new IllegalStateException("Fixture-start clock HTTP "+result.statusCode());
        var response=Json.parse(result.body());
        if(!response.path("authorityClock").asBoolean()||!Instant.parse(start).equals(Instant.parse(response.path("instant").asText())))throw new IllegalStateException("Fixture-start clock not applied");
    }
    /** Credential identity: binding manifest applied, organization claim set to this suite installation's external alias. */
    private JsonNode credentialActor(JsonNode actor) throws Exception {
        JsonNode bound=ActualFixtureBindings.credentialActor(root,configuration,actor);
        // Each authored organization alias maps to this installation's external alias; an unknown alias stays authored (the backend must reject it).
        if(suiteIsolation&&externalOrganizations!=null&&externalOrganizations.hasNonNull(actor.path("organizationAlias").asText()))((ObjectNode)bound).put("organizationAlias",externalOrganizations.path(actor.path("organizationAlias").asText()).asText());
        return bound;
    }
    /** The resolved action.harness of the call this thread is sending; never part of the product request body. */
    private final ThreadLocal<JsonNode> harness=new ThreadLocal<>();
    @Override public StepResult query(String id,String route,JsonNode actor,String operation,JsonNode request,JsonNode declared) {
        StepResult refused=refuseHarness(id,route,declared);if(refused!=null)return refused;
        return withHarness(declared,()->query(id,route,actor,operation,request));
    }
    @Override public StepResult invoke(String id,String route,JsonNode actor,String capability,JsonNode request,JsonNode declared) {
        StepResult refused=refuseHarness(id,route,declared);if(refused!=null)return refused;
        return withHarness(declared,()->invoke(id,route,actor,capability,request));
    }
    private StepResult withHarness(JsonNode declared,java.util.function.Supplier<StepResult> call){
        JsonNode previous=harness.get();harness.set(declared==null?Json.object():declared);
        try{return call.get();}finally{if(previous==null)harness.remove();else harness.set(previous);}
    }
    private volatile Set<String> barrierPoints;
    /**
     * Harness blocks the product cannot honour are refused before anything is sent, so an action never runs in a
     * different environment, without its barrier or with a different credential than the case declared:
     * environmentId names an isolated environment (fresh install, restore) no host control of this adapter
     * provisions; a testBarrier point the product pipeline does not report is never reached; an authenticationTest
     * variant other than one replaced iss/sub/aud claim is not a credential this adapter can sign.
     */
    private StepResult refuseHarness(String id,String route,JsonNode declared) {
        if(declared==null||!declared.isObject()||declared.isEmpty())return null;
        if(declared.hasNonNull("environmentId"))return StepResult.missing(id,"NOT_IMPLEMENTED: actual environment "+declared.path("environmentId").asText()+" not provisioned (no isolated fresh-install/restore host control)");
        JsonNode auth=declared.path("authenticationTest");
        if(auth.isObject()&&!Set.of("issuer","subject","audience").contains(auth.path("variant").asText()))return StepResult.missing(id,"NOT_IMPLEMENTED: actual credential variant "+auth.path("variant").asText());
        JsonNode barrier=declared.path("testBarrier");
        if(barrier.isObject()){
            if(!suiteIsolation)return StepResult.missing(id,"NOT_IMPLEMENTED: actual test barrier outside the verification suite");
            String point=barrier.path("point").asText();
            try{if(!barrierPoints().contains(point))return StepResult.missing(id,"NOT_IMPLEMENTED: product pipeline reports no barrier point "+point);}
            catch(Exception failure){throw new IllegalStateException("Barrier point discovery failed",failure);}
        }
        return null;
    }
    private Set<String> barrierPoints() throws Exception {
        if(barrierPoints!=null)return barrierPoints;
        if(controlActor==null)return Set.of();
        var call=HttpRequest.newBuilder(configuration.baseUri().resolve("/__verification/runtime/barrier/points")).timeout(Duration.ofSeconds(10)).header("Authorization","Bearer "+signer.sign(controlActor)).GET().build();
        var result=http.send(call,HttpResponse.BodyHandlers.ofString());
        if(result.statusCode()!=200)return Set.of();
        var points=new TreeSet<String>();Json.parse(result.body()).path("points").forEach(p->points.add(p.asText()));
        return barrierPoints=Set.copyOf(points);
    }
    /** The authored barrier participant as the loopback header the verification profile reads; null when undeclared. */
    private String barrierHeader(){
        JsonNode b=harness.get()==null?null:harness.get().path("testBarrier");
        if(b==null||!b.isObject())return null;
        var h=Json.object();for(String k:List.of("barrierId","participantId","transactionId","point"))h.put(k,Json.required(b,k));return h.toString();
    }
    private String credential(JsonNode actor) throws Exception {
        JsonNode auth=harness.get()==null?Json.MAPPER.missingNode():harness.get().path("authenticationTest");
        JsonNode bound=credentialActor(actor);
        if(!auth.isObject())return signer.sign(bound);
        String claim=switch(auth.path("variant").asText()){case "issuer"->"iss";case "subject"->"sub";case "audience"->"aud";default->throw new IllegalArgumentException("Unsupported credential variant");};
        return signer.sign(bound,claim,Json.required(auth,"invalidValue"));
    }
    @Override public StepResult query(String id,String route,JsonNode actor,String operation,JsonNode request) {
        if(suiteIsolation&&route.equals("blob"))return blobDownload(id,actor,operation,request);
        if(!route.equals("api")&&!(suiteIsolation&&route.equals("mcp")))return StepResult.missing(id,"NOT_IMPLEMENTED: actual query route/capability "+route+"/"+operation);
        // Suite isolation: every api/mcp query reaches the product; an unknown capability is the product's answer, not an adapter skip.
        if(!suiteIsolation&&!QUERIES.contains(operation))return StepResult.missing(id,"NOT_IMPLEMENTED: actual query route/capability "+route+"/"+operation);
        return send(id,actor,"queries",operation,request,route);
    }
    private StepResult send(String id,JsonNode actor,String category,String operation,JsonNode request) {return send(id,actor,category,operation,request,"api");}
    /** route api: REST endpoint; route mcp: the product MCP channel (/mcp/ontology tools/call) with the same translated arguments. */
    private StepResult send(String id,JsonNode actor,String category,String operation,JsonNode request,String route) {
        if(!operation.matches("[A-Za-z][A-Za-z0-9]*"))throw new IllegalArgumentException("Invalid operation name");
        if(suiteIsolation&&externalOrganization==null)return StepResult.missing(id,"NOT_IMPLEMENTED: no fixture organization installed for this subcase");
        try {
            java.net.URI uri=configuration.baseUri().resolve(category.equals("evidence-upload")?"/api/evidence/uploads":"/api/ontology/"+category+"/"+operation);
            JsonNode wireRequest=request.deepCopy();
            var translated=Json.array();
            if(relaxWire&&category.equals("queries")&&wireRequest.has("objectId")&&!wireRequest.has("id")){((ObjectNode)wireRequest).set("id",wireRequest.path("objectId"));((ObjectNode)wireRequest).remove("objectId");translated.add("objectId->id");}
            // Probe only: an untyped getObject reads TradeItem in the product; name the installed alias type instead.
            if(relaxWire&&operation.equals("getObject")&&wireRequest.hasNonNull("id")&&!wireRequest.path("scope").has("objectType")&&!wireRequest.path("filters").has("type")&&idTypes!=null&&idTypes.hasNonNull(wireRequest.path("id").asText())){
                String type=idTypes.path(wireRequest.path("id").asText()).asText();if(!type.equals("TradeItem")){if(!wireRequest.path("scope").isObject())((ObjectNode)wireRequest).putObject("scope");((ObjectNode)wireRequest.path("scope")).put("objectType",type);translated.add("scope.objectType="+type+" (probe)");}}
            if(category.equals("queries")&&wireRequest.path("scope").isObject()) {
                // Case namespace keys are test metadata (each subcase already has its own organization); includeDescendants
                // is an observation-scope hint the product query schema does not define. Both are removed on the wire and
                // the removal is kept in the receipt (Step 2 finding: the plan defines no such query scope keys).
                var wireScope=(ObjectNode)wireRequest.path("scope");
                for(String key:ScenarioJdbcObservation.CASE_METADATA)if(wireScope.has(key)&&(key.equals("caseId")||suiteIsolation&&!Set.of("includeDescendants","rawRowsOrder").contains(key))){wireScope.remove(key);translated.add(key);}
                // Probe mode only (verification.actual.relaxWire): also drop observation-only keys the product rejects, so the
                // run reaches later steps. Never the default; the inventory records the mode.
                if(relaxWire){for(String key:List.of("includeDescendants","rawRowsOrder"))if(wireScope.has(key)){wireScope.remove(key);translated.add(key);}
                    if(operation.equals("getInventory")&&wireScope.has("workId")){wireScope.remove("workId");translated.add("workId");}}
            }
            if(relaxWire&&!category.equals("queries")&&wireRequest instanceof ObjectNode command)ScenarioWireMapping.command(operation,command,translated);
            if(relaxWire&&!category.equals("queries")&&wireRequest.isObject()) {
                // Probe mode only: complete the contracts/intent.schema.json envelope the cases omit. Fields outside the
                // schema are dropped, valueProvenance becomes provenance, and a missing provenance marks every slot USER.
                var w=(ObjectNode)wireRequest;
                for(String key:iterable(w))if(!INTENT_FIELDS.contains(key)&&!key.equals("valueProvenance")){w.remove(key);translated.add(key);}
                if(!w.has("provenance")&&w.path("valueProvenance").isObject()){w.set("provenance",w.path("valueProvenance"));translated.add("valueProvenance->provenance");}
                w.remove("valueProvenance");
                if(!w.has("provenance")&&w.path("slots").isObject()){var prov=Json.object();w.path("slots").fieldNames().forEachRemaining(k->prov.put(k,"USER"));w.set("provenance",prov);translated.add("provenance:USER-default");}
            }
            String credential=credential(actor);
            String digestBefore=category.equals("queries")&&suiteIsolation?ScenarioRevisionBinding.digest(configuration):null;
            String body=wireRequest.toString();ObjectNode rpc=null;
            var builder=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(60)).header("Content-Type","application/json").header("Authorization","Bearer "+credential);
            JsonNode mcpContext=harness.get()==null?Json.MAPPER.missingNode():harness.get().path("mcp");
            if(route.equals("mcp")) {
                uri=configuration.baseUri().resolve("/mcp/ontology");
                // action.harness.mcp: the authored JSON-RPC id, and MRTR requestState/inputResponses as tools/call params
                // (protocol fields, never tool arguments).
                String rpcId=mcpContext.path("transport").path("rpcId").asText(id);
                rpc=Json.object();rpc.put("jsonrpc","2.0").put("id",rpcId).put("method","tools/call");var params=rpc.putObject("params");params.put("name",operation);params.set("arguments",wireRequest);
                for(String field:List.of("requestState","inputResponses"))if(mcpContext.has(field))params.set(field,mcpContext.path(field));
                var meta=params.putObject("_meta");meta.put("io.modelcontextprotocol/protocolVersion",MCP_VERSION);meta.putObject("io.modelcontextprotocol/clientInfo").put("name","mulino-actual-adapter").put("version","2.1.0");meta.putObject("io.modelcontextprotocol/clientCapabilities");
                body=rpc.toString();
                builder=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(60)).header("Content-Type","application/json").header("Accept","application/json, text/event-stream").header("Mcp-Method","tools/call").header("MCP-Protocol-Version",MCP_VERSION).header("Mcp-Name",operation).header("Authorization","Bearer "+credential);
            }
            // Harness consumption is recorded apart from adapterTranslations: the request body is not rewritten.
            var harnessApplied=Json.array();
            String barrier=barrierHeader();if(barrier!=null){builder.header("X-Mulino-Test-Barrier",barrier);harnessApplied.add("testBarrier->X-Mulino-Test-Barrier header");}
            if(harness.get()!=null&&harness.get().path("authenticationTest").isObject())harnessApplied.add("authenticationTest: credential claim "+harness.get().path("authenticationTest").path("variant").asText()+" replaced by the authored invalid value");
            if(route.equals("mcp")&&mcpContext.isObject())harnessApplied.add("mcp: JSON-RPC id and MRTR params from the case");
            var call=builder.POST(HttpRequest.BodyPublishers.ofString(body)).build();
            Instant submittedAt=Instant.now();
            var result=http.send(call,HttpResponse.BodyHandlers.ofString());
            if(result.statusCode()==404&&!route.equals("mcp"))return StepResult.missing(id,"NOT_IMPLEMENTED: actual HTTP endpoint "+uri.getPath());
            JsonNode response=Json.parse(result.body());
            JsonNode rpcResponse=null;
            // MCP: the business response is the tool result's structuredContent (same command/query envelope as REST);
            // a JSON-RPC protocol error has no business envelope and is kept as the raw RPC body.
            if(route.equals("mcp")){rpcResponse=response;if(response.path("result").has("structuredContent"))response=response.path("result").path("structuredContent");}
            if(digestBefore!=null&&response.hasNonNull("snapshotRevision"))ScenarioRevisionBinding.bind(configuration,id,digestBefore,response.path("snapshotRevision").asText());
            var receipt=Json.object();receipt.put("method","POST").put("path",uri.getPath()).put("route",route).put("httpStatus",result.statusCode()).put("submittedAt",submittedAt.toString()).put("capturedAt",Instant.now().toString());
            if(rpc!=null){receipt.set("rpcRequest",rpc);receipt.set("rpcResponse",rpcResponse);}
            receipt.put("credentialSha256",java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(credential.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
            receipt.set("request",request);receipt.set("wireRequest",wireRequest);receipt.set("response",response);if(!translated.isEmpty())receipt.set("adapterTranslations",translated);if(!harnessApplied.isEmpty())receipt.set("harnessApplied",harnessApplied);
            var identity=Json.object();identity.put("issuer",configuration.issuer()).put("subject",Json.required(actor,"subject")).put("organizationAlias",Json.required(actor,"organizationAlias"));
            var transport=Json.object();transport.put("httpStatus",result.statusCode()).put("route",route);
            return executed(id,transport,response,provenance(identity,"HTTP",false,null,null),receipt);
        } catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new IllegalStateException("Actual HTTP interrupted",interrupted);}
        catch(Exception failure){throw new IllegalStateException("Actual HTTP environment/response failure",failure);}
    }
    @Override public StepResult observe(String id,JsonNode request) {
        try {
            var data=suiteIsolation?ScenarioJdbcObservation.capture(configuration,root,request):observer.capture(request);
            String ref="verification/harness/target/evidence/actual/"+run+"/"+UUID.randomUUID()+".json";
            ((ObjectNode)data.path("snapshot")).put("artifactRef",ref);
            var sources=Json.object();var observedNames=Json.array();if(Set.of("S3","S4").contains(request.path("profile").asText()))data.path("sourceEvidence").fieldNames().forEachRemaining(observedNames::add);else observedNames.addAll((com.fasterxml.jackson.databind.node.ArrayNode)request.path("sources"));for(JsonNode requested:observedNames){String name=requested.asText();var evidence=Json.object();evidence.put("complete",true).put("rowPointer","/rawRows/"+name.replace("~","~0").replace("/","~1")).put("artifactRef",ref);evidence.set("sourceQuery",data.path("sourceEvidence").path(name).hasNonNull("sourceQuery")?data.path("sourceEvidence").path(name).path("sourceQuery"):data.path("sourceQuery"));sources.set(name,evidence);}data.set("sourceEvidence",sources);
            Json.write(root.resolve(ref),data);
            return new StepResult(id,StepResult.DriverStatus.EXECUTED,data,null,null,provenance(null,"POSTGRESQL_JDBC",true,data.path("sourceQuery"),data.path("snapshot")),List.of(ref));
        } catch(UnsupportedOperationException unsupported){return StepResult.missing(id,"NOT_IMPLEMENTED: "+unsupported.getMessage());}
        catch(Exception failure){throw new IllegalStateException("Actual JDBC environment failure",failure);}
    }
    private static List<String> iterable(ObjectNode node){var out=new ArrayList<String>();node.fieldNames().forEachRemaining(out::add);return out;}
    private StepResult executed(String id,JsonNode data,JsonNode response,ObjectNode provenance,JsonNode receipt) throws java.io.IOException {
        String ref="verification/harness/target/evidence/actual/"+run+"/"+UUID.randomUUID()+".json";
        Json.write(root.resolve(ref),receipt);
        return new StepResult(id,StepResult.DriverStatus.EXECUTED,data,response,null,provenance,List.of(ref));
    }
    private ObjectNode provenance(JsonNode actor,String source,boolean independent,JsonNode query,JsonNode snapshot) {
        var p=Json.object();p.put("adapter","actual-http-jdbc").put("adapterVersion","2.0.0").put("buildVersion",configuration.buildVersion());
        p.set("authenticatedActor",actor==null?Json.MAPPER.nullNode():actor);p.put("source",source).put("independent",independent).put("scopeComplete",true);
        p.set("sourceQuery",query==null?Json.MAPPER.nullNode():query);p.set("snapshot",snapshot==null?Json.MAPPER.nullNode():snapshot);return p;
    }
    /**
     * route=blob query: the product evidence download surface GET /api/evidence/documents/{id}/content (EvidenceDownload),
     * for getEvidence with download=true and an evidenceId. The response is the transport outcome and the content digest;
     * the received bytes are kept as one run artifact named in response.downloadedArtifacts (path), the receipt keeps the hash.
     */
    private StepResult blobDownload(String id,JsonNode actor,String operation,JsonNode request) {
        if(!operation.equals("getEvidence")||!request.path("download").asBoolean(false)||!request.hasNonNull("evidenceId"))return StepResult.missing(id,"NOT_IMPLEMENTED: actual blob query "+operation+" without download evidenceId");
        if(externalOrganization==null)return StepResult.missing(id,"NOT_IMPLEMENTED: no fixture organization installed for this subcase");
        try {
            String document=request.path("evidenceId").asText();UUID.fromString(document);
            var uri=configuration.baseUri().resolve("/api/evidence/documents/"+document+"/content");String credential=credential(actor);
            var result=http.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).header("Authorization","Bearer "+credential).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
            var response=Json.object();response.put("httpStatus",result.statusCode());
            String saved=null;
            if(result.statusCode()==200){response.put("sha256",java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(result.body()))).put("sizeBytes",result.body().length);
                // The received bytes as a run artifact, so a host inspection (T24 inspectArtifacts) can hash the actual download.
                saved="verification/harness/target/evidence/actual/"+run+"/download-"+UUID.randomUUID()+".bin";java.nio.file.Files.createDirectories(root.resolve(saved).getParent());java.nio.file.Files.write(root.resolve(saved),result.body());
                response.putArray("downloadedArtifacts").add(Json.object().put("path",saved));}
            else{JsonNode body=parseOrText(new String(result.body(),java.nio.charset.StandardCharsets.UTF_8));response.set("body",body);if(body.has("error"))response.set("error",body.path("error"));if(body.has("outcome"))response.set("outcome",body.path("outcome"));}
            var headers=response.putObject("headers");for(String h:List.of("content-type","cache-control","content-disposition","x-content-type-options"))result.headers().firstValue(h).ifPresent(v->headers.put(h,v));
            var receipt=Json.object();receipt.put("method","GET").put("path",uri.getPath()).put("route","blob").put("httpStatus",result.statusCode());receipt.set("request",request);receipt.set("response",response);
            var identity=Json.object().put("issuer",configuration.issuer()).put("subject",Json.required(actor,"subject")).put("organizationAlias",Json.required(actor,"organizationAlias"));
            var step=executed(id,Json.object().put("httpStatus",result.statusCode()).put("route","blob"),response,provenance(identity,"HTTP_BLOB_DOWNLOAD",false,null,null),receipt);
            if(saved==null)return step;
            var refs=new ArrayList<>(step.artifactRefs());refs.add(saved);
            return new StepResult(id,StepResult.DriverStatus.EXECUTED,step.data(),step.response(),null,step.provenance(),refs);
        } catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new IllegalStateException("Actual blob interrupted",interrupted);}
        catch(Exception failure){throw new IllegalStateException("Actual blob environment/response failure",failure);}
    }
    @Override public StepResult invoke(String id,String route,JsonNode actor,String capability,JsonNode request) {
        // route=blob upload: the product upload surface POST /api/evidence/uploads carries the authored businessAction intent
        // unchanged; documentId/contentHash have no field on that endpoint and stay in the receipt only.
        if(suiteIsolation&&route.equals("blob")){
            if(!request.path("businessAction").isObject())return StepResult.missing(id,"NOT_IMPLEMENTED: actual blob command without businessAction");
            return send(id,actor,"evidence-upload",capability,request.path("businessAction"),"blob");
        }
        if(suiteIsolation&&route.equals("worker"))
            return StepResult.missing(id,"NOT_IMPLEMENTED: no product surface for route worker: the product exposes no external worker submission endpoint; durable workers execute only server-claimed intake/recovery commands (ApplicationCommands.executeClaimed)");
        if(suiteIsolation&&Set.of("nested","projection").contains(route))
            return StepResult.missing(id,"NOT_IMPLEMENTED: no product surface for route "+route+": OntologyService exposes only the query/command/validateCommand actions, no entity set or navigation to write through");
        if(suiteIsolation&&route.equals("direct"))return odataCommand(id,actor,capability,request);
        if(suiteIsolation&&route.equals("batch"))return odataBatch(id,actor,request);
        // Management commands (retrySafeCommand, emergencyRepair, ...) are product commands guarded by management authority;
        // the product has no separate management transport, so the route is the command API with the operator's identity.
        String transport=suiteIsolation&&route.equals("management")?"api":route;
        if(!transport.equals("api")&&!(suiteIsolation&&transport.equals("mcp")))return StepResult.missing(id,"NOT_IMPLEMENTED: actual command route "+route);
        return send(id,actor,request.path("intentKind").asText().equals("RECORD")?"records":"commands",capability,request,transport);
    }
    @Override public StepResult control(String id,JsonNode request) {
        if(suiteIsolation&&request.path("type").asText().equals("barrier")&&Set.of("waitReached","resume").contains(request.path("operation").asText()))return barrierControl(id,request);
        if(suiteIsolation&&request.path("type").asText().equals("process")&&HostProcessControls.OPERATIONS.contains(request.path("operation").asText())) {
            try{return hostControls.control(id,request,()->provenance(null,"ACTUAL_HOST_PROCESS",true,null,null));}
            catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new IllegalStateException("Host control interrupted",interrupted);}
            catch(java.io.IOException failure){throw new IllegalStateException("Host control environment failure",failure);}
        }
        if(suiteIsolation&&request.path("type").asText().equals("process")&&SchedulerHostControls.OPERATIONS.contains(request.path("operation").asText())) {
            if(controlActor==null)return StepResult.missing(id,"NOT_IMPLEMENTED: installed control identity absent");
            try{return schedulerControls.control(id,request,configuration.baseUri().toString(),signer.sign(controlActor),()->provenance(null,"ACTUAL_HOST_PROCESS",true,null,null));}
            catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new IllegalStateException("Scheduler control interrupted",interrupted);}
            catch(java.io.IOException failure){throw new IllegalStateException("Scheduler control environment failure",failure);}
        }
        if(suiteIsolation&&request.path("type").asText().equals("externalResponder"))return responderControl(id,request);
        if(suiteIsolation&&request.path("type").asText().equals("fault")&&Set.of("arm","inject","install","disarm","remove").contains(request.path("operation").asText()))return faultControl(id,request);
        if(!request.path("type").asText().equals("clock")||!Set.of("advanceTo","set","advance").contains(request.path("operation").asText()))
            return StepResult.missing(id,"NOT_IMPLEMENTED: actual "+request.path("type").asText()+"/"+request.path("operation").asText()+" host control absent");
        if(controlActor==null)return StepResult.missing(id,"NOT_IMPLEMENTED: installed control identity absent");
        // contracts/execution-preconditions clock: a clock control sets the clock to instant, else asOf, else knownAt.
        String instant=null;for(String key:List.of("instant","asOf","knownAt"))if(request.path("parameters").hasNonNull(key)){instant=request.path("parameters").path(key).asText();break;}
        if(instant==null)throw new IllegalArgumentException("Clock control requires instant, asOf or knownAt");
        try {
            var body=Json.object();body.put("instant",instant);
            var uri=configuration.baseUri().resolve("/__verification/runtime/clock");
            var call=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).header("Content-Type","application/json").header("Authorization","Bearer "+signer.sign(controlActor)).POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
            var result=http.send(call,HttpResponse.BodyHandlers.ofString());
            if(result.statusCode()==404)return StepResult.missing(id,"NOT_IMPLEMENTED: verification clock profile not installed");
            var response=Json.parse(result.body());
            var data=clockAcknowledgment(result.statusCode(),body.path("instant").asText(),response);data.put("operation",request.path("operation").asText());
            var receipt=Json.object();receipt.put("httpStatus",result.statusCode()).put("path",uri.getPath());receipt.set("requestedControl",request);receipt.set("wireRequest",body);receipt.set("serverResponse",response);
            return executed(id,data,response,provenance(null,"HTTP_VERIFICATION_CLOCK",false,null,null),receipt);
        }catch(InterruptedException failure){Thread.currentThread().interrupt();throw new IllegalStateException("Actual clock interrupted",failure);}
        catch(Exception failure){throw new IllegalStateException("Actual clock environment/response failure",failure);}
    }
    /**
     * Barrier control: waitReached and resume on the verification-profile barrier registry. The ACK carries the state the
     * product observed for that participant (REACHED, BLOCKED, RESUMED, or NOT_SEEN/TIMED_OUT), never the requested one;
     * the harness compares it with the authored state.
     */
    private StepResult barrierControl(String id,JsonNode request) {
        if(controlActor==null)return StepResult.missing(id,"NOT_IMPLEMENTED: installed control identity absent");
        JsonNode p=request.path("parameters");
        try {
            var body=Json.object();body.put("operation",request.path("operation").asText()).put("barrierId",Json.required(p,"barrierId")).put("participantId",Json.required(p,"participantId"));
            if(request.path("operation").asText().equals("waitReached"))body.put("timeoutSeconds",p.path("timeoutSeconds").asInt(30));
            var uri=configuration.baseUri().resolve("/__verification/runtime/barrier");
            var call=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(70)).header("Content-Type","application/json").header("Authorization","Bearer "+signer.sign(controlActor)).POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
            var result=http.send(call,HttpResponse.BodyHandlers.ofString());
            if(result.statusCode()==404)return StepResult.missing(id,"NOT_IMPLEMENTED: verification barrier profile not installed");
            var response=Json.parse(result.body());
            if(result.statusCode()!=200)throw new IllegalStateException("Barrier control HTTP "+result.statusCode());
            var data=Json.object();data.put("acknowledged",true).put("controlType","barrier").put("operation",request.path("operation").asText()).put("acknowledgedAt",Instant.now().toString());
            for(String k:List.of("barrierId","participantId"))data.set(k,response.path(k));
            // transactionId and point come from the product registry entry, which the request header declared.
            data.set("transactionId",response.hasNonNull("transactionId")?response.path("transactionId"):Json.MAPPER.nullNode());
            data.set("point",response.hasNonNull("point")?response.path("point"):Json.MAPPER.nullNode());
            data.set("state",response.path("state"));data.set("serverObservation",response);
            var receipt=Json.object();receipt.put("httpStatus",result.statusCode()).put("path",uri.getPath());receipt.set("requestedControl",request);receipt.set("wireRequest",body);receipt.set("serverResponse",response);
            return executed(id,data,response,provenance(null,"HTTP_VERIFICATION_BARRIER",false,null,null),receipt);
        }catch(InterruptedException failure){Thread.currentThread().interrupt();throw new IllegalStateException("Actual barrier interrupted",failure);}
        catch(Exception failure){throw new IllegalStateException("Actual barrier environment/response failure",failure);}
    }
    /**
     * Fault control on the verification-profile fault registry: arm/inject/install a fault for the subcase organization
     * at a pipeline point, or disarm/remove it by id. The authored parameter names map one to one (commandKey or
     * commandIdempotencyKey, occurrences/times/once, fault/failure); nothing else is sent. A point the product pipeline
     * does not report stays NOT_IMPLEMENTED with its name instead of being armed and never firing.
     */
    /**
     * externalResponder control on the verification-profile synthetic receiver (backend VerificationResponder), the external
     * system the verification scheduler's delivery tick talks to. configure/setResponse/loseResponse/commitThenLoseResponse
     * set how it answers; inspectRequests/queryOperation/queryStatus read back what it received. Authored names map to the
     * responder fields below; descriptive parameters (receiver, profile, effect class) are kept in the receipt, and any
     * behaviour the receiver cannot play (a notification channel, a queue acknowledgement) stays NOT_IMPLEMENTED.
     * queryOperation/queryStatus materialize the receiver's own answer for one externalOperationId as a receipt document.
     */
    private StepResult responderControl(String id,JsonNode request) {
        if(controlActor==null)return StepResult.missing(id,"NOT_IMPLEMENTED: installed control identity absent");
        String operation=request.path("operation").asText();JsonNode p=request.path("parameters");
        try {
            if(Set.of("configure","setResponse","loseResponse","commitThenLoseResponse").contains(operation)) {
                if(p.has("channel"))return StepResult.missing(id,"NOT_IMPLEMENTED: externalResponder channel "+p.path("channel").asText()+": the product has no notification delivery channel, only the transactional outbox");
                String mode;
                if(operation.equals("commitThenLoseResponse"))mode="ACCEPT_THEN_DROP_RESPONSE";
                else if(operation.equals("loseResponse")){if(!"AFTER_EXTERNAL_EFFECT".equals(p.path("when").asText("AFTER_EXTERNAL_EFFECT")))return StepResult.missing(id,"NOT_IMPLEMENTED: externalResponder loseResponse when="+p.path("when").asText());mode="ACCEPT_THEN_DROP_RESPONSE";}
                else if(p.hasNonNull("behavior")||p.hasNonNull("responseMode"))mode=p.hasNonNull("behavior")?p.path("behavior").asText():p.path("responseMode").asText();
                else if(p.path("response").isObject())mode=p.path("response").path("delivered").asBoolean(false)||"DELIVERED".equals(p.path("response").path("status").asText())?"CONFIRMED_SUCCESS":"CONFIRMED_FAILURE";
                else return StepResult.missing(id,"NOT_IMPLEMENTED: externalResponder "+operation+" without a response mode");
                if(!Set.of("CONFIRMED_SUCCESS","CONFIRMED_FAILURE","ACCEPT_THEN_DROP_RESPONSE","TIMEOUT_BEFORE_EFFECT").contains(mode))
                    return StepResult.missing(id,"NOT_IMPLEMENTED: externalResponder mode "+mode+": the synthetic outbox receiver answers CONFIRMED_SUCCESS, CONFIRMED_FAILURE, ACCEPT_THEN_DROP_RESPONSE or TIMEOUT_BEFORE_EFFECT");
                var body=Json.object();body.put("mode",mode);
                String organization=p.path("scope").path("organizationId").asText("");if(organization.matches("[0-9a-f-]{36}"))body.put("organizationId",organization);
                String external=p.hasNonNull("externalOperationId")?p.path("externalOperationId").asText():p.hasNonNull("operationId")?p.path("operationId").asText():null;if(external!=null)body.put("externalOperationId",external);
                if(p.hasNonNull("capabilityId"))body.put("operation",p.path("capabilityId").asText());
                for(String k:List.of("idempotencySupported","supportsIdempotency"))if(p.path(k).isBoolean())body.put("idempotencySupported",p.path(k).asBoolean());
                for(String k:List.of("lookupSupported","statusQuerySupported"))if(p.path(k).isBoolean())body.put("lookupSupported",p.path(k).asBoolean());
                if("ACTUAL_REPLY".equals(p.path("statusQueryMode").asText()))body.put("lookupSupported",true);
                if(p.hasNonNull("remoteResultId"))body.put("remoteResultId",p.path("remoteResultId").asText());
                var applied=Set.of("behavior","responseMode","response","externalOperationId","operationId","capabilityId","idempotencySupported","supportsIdempotency","lookupSupported","statusQuerySupported","statusQueryMode","remoteResultId","when");
                var descriptive=Json.array();p.fieldNames().forEachRemaining(k->{if(!applied.contains(k))descriptive.add(k);});
                return responderCall(id,request,"POST","/__verification/runtime/responder",body,descriptive,null);
            }
            if(Set.of("inspectRequests","queryOperation","queryStatus").contains(operation)) {
                String external=p.hasNonNull("externalOperationId")?p.path("externalOperationId").asText():null;
                if(!operation.equals("inspectRequests")&&external==null)return StepResult.missing(id,"NOT_IMPLEMENTED: externalResponder "+operation+" without externalOperationId");
                String path="/__verification/runtime/responder/requests"+(external==null?"":"?externalOperationId="+java.net.URLEncoder.encode(external,java.nio.charset.StandardCharsets.UTF_8));
                return responderCall(id,request,"GET",path,null,Json.array(),operation.equals("inspectRequests")?null:external);
            }
            return StepResult.missing(id,"NOT_IMPLEMENTED: actual externalResponder/"+operation+" host control absent");
        }catch(InterruptedException failure){Thread.currentThread().interrupt();throw new IllegalStateException("Responder control interrupted",failure);}
        catch(Exception failure){throw new IllegalStateException("Responder control environment/response failure",failure);}
    }
    private StepResult responderCall(String id,JsonNode request,String method,String path,ObjectNode body,com.fasterxml.jackson.databind.node.ArrayNode descriptive,String queried) throws Exception {
        var uri=configuration.baseUri().resolve(path);
        var builder=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).header("Authorization","Bearer "+signer.sign(controlActor));
        var call=body==null?builder.GET().build():builder.header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        var result=http.send(call,HttpResponse.BodyHandlers.ofString());
        if(result.statusCode()==404)return StepResult.missing(id,"NOT_IMPLEMENTED: verification responder profile not installed");
        var response=Json.parse(result.body());
        if(result.statusCode()==422)return StepResult.missing(id,"NOT_IMPLEMENTED: synthetic responder refused: "+response.path("reason").asText());
        if(result.statusCode()!=200)throw new IllegalStateException("Responder control HTTP "+result.statusCode());
        var data=Json.object();data.put("acknowledged",true).put("controlType","externalResponder").put("operation",request.path("operation").asText()).put("acknowledgedAt",Instant.now().toString());
        var refs=new ArrayList<String>();
        if(body!=null){data.set("configuration",response);}
        else {
            data.set("requests",response.path("requests"));
            if(queried!=null) {
                boolean applied=false,received=false;String remote=null;
                for(JsonNode r:response.path("requests")){received=true;if(r.path("effectApplied").asBoolean(false)){applied=true;if(r.hasNonNull("remoteResultId"))remote=r.path("remoteResultId").asText();}}
                String outcome=applied?"CONFIRMED_SUCCESS":received?"CONFIRMED_FAILURE":"NOT_RECEIVED";
                var receipt=Json.object();receipt.put("recordType","SYNTHETIC_RESPONDER_RECEIPT").put("externalOperationId",queried).put("result",outcome);if(remote!=null)receipt.put("remoteResultId",remote);receipt.set("requests",response.path("requests"));receipt.put("observedAt",response.path("observedAt").asText());
                String receiptRef="verification/harness/target/evidence/actual/"+run+"/responder-receipt-"+UUID.randomUUID()+".json";Json.write(root.resolve(receiptRef),receipt);refs.add(receiptRef);
                String hash=Json.sha256(root.resolve(receiptRef));
                data.put("externalOperationId",queried).put("result",outcome).put("receiptArtifact",receiptRef).put("receiptHash",hash).put("documentUri",receiptRef).put("evidenceHash",hash);
                if(remote!=null)data.put("remoteResultId",remote);
            }
        }
        var receipt=Json.object();receipt.put("httpStatus",result.statusCode()).put("method",method).put("path",uri.getPath());receipt.set("requestedControl",request);if(body!=null)receipt.set("wireRequest",body);receipt.set("descriptiveParameters",descriptive);receipt.set("serverResponse",response);
        var step=executed(id,data,response,provenance(null,"HTTP_VERIFICATION_RESPONDER",false,null,null),receipt);
        if(refs.isEmpty())return step;
        refs.addAll(step.artifactRefs());
        return new StepResult(id,StepResult.DriverStatus.EXECUTED,data,response,null,step.provenance(),refs);
    }
    private StepResult faultControl(String id,JsonNode request) {
        if(controlActor==null)return StepResult.missing(id,"NOT_IMPLEMENTED: installed control identity absent");
        JsonNode p=request.path("parameters");String operation=request.path("operation").asText();
        var body=Json.object();body.put("operation",operation);
        if(Set.of("disarm","remove").contains(operation)){
            if(!p.hasNonNull("faultId"))return StepResult.missing(id,"NOT_IMPLEMENTED: actual fault/"+operation+" without faultId");
            body.put("faultId",p.path("faultId").asText());
        } else {
            String organization=p.path("scope").path("organizationId").asText(null);
            if(organization==null)return StepResult.missing(id,"NOT_IMPLEMENTED: actual fault/"+operation+" without scope organization");
            body.put("organizationId",organization).put("point",Json.required(p,"point"));
            if(p.hasNonNull("faultId"))body.put("faultId",p.path("faultId").asText());
            if(p.hasNonNull("capabilityId"))body.put("capabilityId",p.path("capabilityId").asText());
            String key=p.hasNonNull("commandKey")?p.path("commandKey").asText():p.hasNonNull("commandIdempotencyKey")?p.path("commandIdempotencyKey").asText():null;
            if(key!=null)body.put("commandKey",key);
            if(p.has("occurrences"))body.put("occurrences",p.path("occurrences").asInt());else if(p.has("times"))body.put("occurrences",p.path("times").asInt());else if(p.path("once").asBoolean(false))body.put("occurrences",1);
            String kind=p.hasNonNull("fault")?p.path("fault").asText():p.hasNonNull("failure")?p.path("failure").asText():null;
            if(kind!=null)body.put("fault",kind);
            for(var it=p.fieldNames();it.hasNext();){String k=it.next();if(!Set.of("scope","point","faultId","capabilityId","commandKey","commandIdempotencyKey","occurrences","times","once","fault","failure").contains(k))return StepResult.missing(id,"NOT_IMPLEMENTED: actual fault parameter "+k+" (point "+p.path("point").asText()+")");}
        }
        try {
            var uri=configuration.baseUri().resolve("/__verification/runtime/fault");
            var call=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).header("Content-Type","application/json").header("Authorization","Bearer "+signer.sign(controlActor)).POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
            var result=http.send(call,HttpResponse.BodyHandlers.ofString());
            if(result.statusCode()==404)return StepResult.missing(id,"NOT_IMPLEMENTED: verification fault profile not installed");
            var response=Json.parse(result.body());
            if(result.statusCode()==422)return StepResult.missing(id,"NOT_IMPLEMENTED: product pipeline reports no fault point "+response.path("point").asText());
            if(result.statusCode()!=200)throw new IllegalStateException("Fault control HTTP "+result.statusCode());
            var data=Json.object();data.put("acknowledged",true).put("controlType","fault").put("operation",operation).put("acknowledgedAt",Instant.now().toString());
            for(String k:List.of("faultId","point","state"))data.set(k,response.path(k));
            data.set("serverObservation",response);
            var receipt=Json.object();receipt.put("httpStatus",result.statusCode()).put("path",uri.getPath());receipt.set("requestedControl",request);receipt.set("wireRequest",body);receipt.set("serverResponse",response);
            return executed(id,data,response,provenance(null,"HTTP_VERIFICATION_FAULT",false,null,null),receipt);
        }catch(InterruptedException failure){Thread.currentThread().interrupt();throw new IllegalStateException("Actual fault control interrupted",failure);}
        catch(Exception failure){throw new IllegalStateException("Actual fault control environment/response failure",failure);}
    }
    static ObjectNode clockAcknowledgment(int httpStatus,String requestedInstant,JsonNode serverResponse) {
        if(httpStatus!=200||!serverResponse.path("authorityClock").asBoolean()||!"verification".equals(serverResponse.path("profile").asText())||!Instant.parse(requestedInstant).equals(Instant.parse(serverResponse.path("instant").asText())))throw new IllegalStateException("Verification clock control did not prove applied authority clock");
        var data=Json.object();data.put("acknowledged",true).put("controlType","clock").put("operation","advanceTo").put("acknowledgedAt",Instant.now().toString());data.set("instant",serverResponse.path("instant"));data.set("serverObservation",serverResponse);return data;
    }
    /** route=direct: the product's exposed generic write action, CAP OData OntologyService.command(requestJson) at /odata/v4/ontology/command. */
    private StepResult odataCommand(String id,JsonNode actor,String capability,JsonNode request) {
        if(externalOrganization==null)return StepResult.missing(id,"NOT_IMPLEMENTED: no fixture organization installed for this subcase");
        try {
            var translated=Json.array();JsonNode wire=request.deepCopy();if(relaxWire&&wire instanceof ObjectNode command)ScenarioWireMapping.command(capability,command,translated);
            var body=Json.object();body.put("requestJson",wire.toString());
            var uri=configuration.baseUri().resolve(ODATA+"/command");String credential=signer.sign(credentialActor(actor));
            var result=http.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).header("Content-Type","application/json").header("Accept","application/json").header("Authorization","Bearer "+credential).POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(),HttpResponse.BodyHandlers.ofString());
            if(result.statusCode()==404)return StepResult.missing(id,"NOT_IMPLEMENTED: actual HTTP endpoint "+uri.getPath());
            JsonNode raw=parseOrText(result.body());JsonNode response=raw.path("value").isTextual()?parseOrText(raw.path("value").asText()):raw;
            var receipt=Json.object();receipt.put("method","POST").put("path",uri.getPath()).put("route","direct").put("httpStatus",result.statusCode());receipt.set("request",request);receipt.set("wireRequest",body);receipt.set("rawResponse",raw);receipt.set("response",response);if(!translated.isEmpty())receipt.set("adapterTranslations",translated);
            var identity=Json.object().put("issuer",configuration.issuer()).put("subject",Json.required(actor,"subject")).put("organizationAlias",Json.required(actor,"organizationAlias"));
            return executed(id,Json.object().put("httpStatus",result.statusCode()).put("route","direct"),response,provenance(identity,"HTTP_ODATA_ACTION",false,null,null),receipt);
        } catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new IllegalStateException("Actual OData interrupted",interrupted);}
        catch(Exception failure){throw new IllegalStateException("Actual OData environment/response failure",failure);}
    }
    /**
     * route=batch: OData v4 multipart $batch of OntologyService.command calls; atomic=true puts every operation in one
     * changeset. The step response is an adapter aggregate: outcome APPLIED only when every operation applied, else the
     * first non-applied operation's outcome and error; every operation envelope is kept under operations[].
     */
    private StepResult odataBatch(String id,JsonNode actor,JsonNode request) {
        if(externalOrganization==null)return StepResult.missing(id,"NOT_IMPLEMENTED: no fixture organization installed for this subcase");
        try {
            String batch="batch_"+UUID.randomUUID(),change="changeset_"+UUID.randomUUID();boolean atomic=request.path("atomic").asBoolean(false);
            var translated=Json.array();var sb=new StringBuilder();int n=0;
            if(atomic)sb.append("--").append(batch).append("\r\nContent-Type: multipart/mixed; boundary=").append(change).append("\r\n\r\n");
            for(JsonNode op:request.path("operations")) {
                JsonNode wire=op.deepCopy();if(relaxWire&&wire instanceof ObjectNode command)ScenarioWireMapping.command(op.path("capabilityId").asText(),command,translated);
                var body=Json.object();body.put("requestJson",wire.toString());
                sb.append("--").append(atomic?change:batch).append("\r\nContent-Type: application/http\r\nContent-Transfer-Encoding: binary\r\nContent-ID: ").append(++n).append("\r\n\r\n")
                  .append("POST command HTTP/1.1\r\nContent-Type: application/json\r\nAccept: application/json\r\n\r\n").append(body).append("\r\n");
            }
            if(atomic)sb.append("--").append(change).append("--\r\n");
            sb.append("--").append(batch).append("--\r\n");
            var uri=configuration.baseUri().resolve(ODATA+"/$batch");String credential=signer.sign(credentialActor(actor));
            var result=http.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(60)).header("Content-Type","multipart/mixed; boundary="+batch).header("Accept","multipart/mixed").header("Authorization","Bearer "+credential).POST(HttpRequest.BodyPublishers.ofString(sb.toString())).build(),HttpResponse.BodyHandlers.ofString());
            if(result.statusCode()==404)return StepResult.missing(id,"NOT_IMPLEMENTED: actual HTTP endpoint "+uri.getPath());
            var operations=Json.array();
            var m=java.util.regex.Pattern.compile("HTTP/1\\.1 (\\d{3})[^\\n]*\\n(?:[^\\n]+\\n)*?\\r?\\n(\\{.*?\\})\\r?\\n(?=--)",java.util.regex.Pattern.DOTALL).matcher(result.body());
            while(m.find()){JsonNode raw=parseOrText(m.group(2));var entry=Json.object();entry.put("httpStatus",Integer.parseInt(m.group(1)));entry.set("response",raw.path("value").isTextual()?parseOrText(raw.path("value").asText()):raw);operations.add(entry);}
            var response=Json.object();JsonNode first=null;for(JsonNode op:operations)if(!"APPLIED".equals(op.path("response").path("outcome").asText())){first=op.path("response");break;}
            if(operations.isEmpty()){response.put("outcome","UNPARSED_BATCH_RESPONSE");}
            else if(first==null)response.put("outcome","APPLIED");else{response.set("outcome",first.path("outcome").isMissingNode()?Json.MAPPER.getNodeFactory().textNode("NO_ENVELOPE"):first.path("outcome"));if(first.has("error"))response.set("error",first.path("error"));}
            response.set("operations",operations);
            var receipt=Json.object();receipt.put("method","POST").put("path",uri.getPath()).put("route","batch").put("httpStatus",result.statusCode()).put("aggregate","outcome APPLIED only when every operation applied; else the first non-applied operation's outcome/error");
            receipt.set("request",request);receipt.put("wireBody",sb.toString());receipt.put("rawResponse",result.body());receipt.set("response",response);if(!translated.isEmpty())receipt.set("adapterTranslations",translated);
            var identity=Json.object().put("issuer",configuration.issuer()).put("subject",Json.required(actor,"subject")).put("organizationAlias",Json.required(actor,"organizationAlias"));
            return executed(id,Json.object().put("httpStatus",result.statusCode()).put("route","batch").put("operationCount",operations.size()),response,provenance(identity,"HTTP_ODATA_BATCH",false,null,null),receipt);
        } catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new IllegalStateException("Actual OData batch interrupted",interrupted);}
        catch(Exception failure){throw new IllegalStateException("Actual OData batch environment/response failure",failure);}
    }
    static final String ODATA="/odata/v4/ontology";
    private static JsonNode parseOrText(String body){try{return Json.parse(body);}catch(Exception notJson){return Json.MAPPER.getNodeFactory().textNode(body);}}
    /**
     * Raw protocol transport (route=wire): the authored method, headers and body are sent unchanged to the product MCP
     * channel (/mcp/ontology), or to the authored path for HTTP/* operations. The only addition is the bearer credential of
     * credentialProfileRef (else the acting actor); "anonymous" sends none. The transcript keeps the raw request with the
     * Authorization value redacted to its SHA-256.
     */
    @Override public StepResult wire(String id,JsonNode actor,String protocolOperation,JsonNode raw) {
        if(!suiteIsolation)return StepResult.missing(id,"NOT_IMPLEMENTED: actual raw protocol transport adapter absent");
        if(externalOrganization==null)return StepResult.missing(id,"NOT_IMPLEMENTED: no fixture organization installed for this subcase");
        if(!raw.path("transport").asText("streamable-http").equals("streamable-http")&&!raw.has("path"))return StepResult.missing(id,"NOT_IMPLEMENTED: actual raw transport "+raw.path("transport").asText());
        try {
            String path=raw.path("path").asText("/mcp/ontology");if(!path.startsWith("/")||path.contains(".."))throw new IllegalArgumentException("Raw path must be an absolute product path");
            String method=raw.path("httpMethod").asText(raw.path("method").asText("POST"));
            var uri=configuration.baseUri().resolve(path);
            String body=raw.path("body").isTextual()?raw.path("body").asText():raw.path("body").isMissingNode()?"":raw.path("body").toString();
            var builder=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).method(method,body.isEmpty()?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body));
            var headers=Json.object();
            for(var it=raw.path("headers").fields();it.hasNext();){var h=it.next();if(Set.of("host","content-length","connection").contains(h.getKey().toLowerCase(Locale.ROOT)))continue;builder.header(h.getKey(),h.getValue().asText());headers.put(h.getKey(),h.getValue().asText());}
            if(raw.hasNonNull("contentType")&&!raw.path("headers").has("Content-Type")){builder.header("Content-Type",raw.path("contentType").asText());headers.put("Content-Type",raw.path("contentType").asText());}
            String profile=raw.path("credentialProfileRef").asText(null);String credentialHash=null;
            JsonNode signed=profile==null?actor:"anonymous".equals(profile)?null:installedActors!=null&&installedActors.has(profile)?installedActors.path(profile):actor;
            if(signed!=null){String credential=signer.sign(credentialActor(signed));builder.header("Authorization","Bearer "+credential);credentialHash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(credential.getBytes(java.nio.charset.StandardCharsets.UTF_8)));headers.put("Authorization","REDACTED sha256:"+credentialHash);}
            Instant submittedAt=Instant.now();
            var result=http.send(builder.build(),HttpResponse.BodyHandlers.ofString());
            JsonNode responseBody;try{responseBody=result.body().isEmpty()?Json.MAPPER.nullNode():Json.parse(result.body());}catch(Exception notJson){responseBody=Json.MAPPER.getNodeFactory().textNode(result.body());}
            var response=Json.object();response.put("httpStatus",result.statusCode());var rh=response.putObject("headers");result.headers().map().forEach((k,v)->{if(!k.equalsIgnoreCase("set-cookie"))rh.put(k,String.join(", ",v));});response.set("body",responseBody);
            var transcript=Json.object();var req=transcript.putObject("request");req.put("method",method).put("path",path);req.set("headers",headers);req.set("body",raw.path("body"));
            transcript.put("protocolOperation",protocolOperation).put("credentialProfileRef",profile).put("submittedAt",submittedAt.toString()).put("capturedAt",Instant.now().toString());
            var data=Json.object();data.set("transcript",transcript);data.put("httpStatus",result.statusCode());
            var receipt=Json.object();receipt.set("transcript",transcript);receipt.set("response",response);receipt.set("authoredRequest",redacted(raw));
            var identity=signed==null?null:Json.object().put("issuer",configuration.issuer()).put("subject",Json.required(signed,"subject")).put("organizationAlias",Json.required(signed,"organizationAlias"));
            return executed(id,data,response,provenance(identity,"HTTP_RAW_WIRE",false,null,null),receipt);
        } catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new IllegalStateException("Actual raw wire interrupted",interrupted);}
        catch(Exception failure){throw new IllegalStateException("Actual raw wire environment/response failure",failure);}
    }
    private static JsonNode redacted(JsonNode raw){var copy=raw.deepCopy();if(copy.path("headers") instanceof ObjectNode h)for(String k:List.of("Authorization","authorization","Cookie","cookie"))if(h.has(k))h.put(k,"REDACTED");return copy;}
    private volatile JsonNode installedActors,idTypes;
    @Override public StepResult start(String id,String route,JsonNode actor,String capability,JsonNode request) {return start(id,route,actor,capability,request,Json.object());}
    @Override public StepResult start(String id,String route,JsonNode actor,String capability,JsonNode request,JsonNode declared) {
        if(!route.equals("api"))return StepResult.missing(id,"NOT_IMPLEMENTED: actual async route "+route);
        StepResult refused=refuseHarness(id,route,declared);if(refused!=null)return refused;
        String handle=UUID.randomUUID().toString();
        pending.put(handle,CompletableFuture.supplyAsync(()->invoke(id,route,actor,capability,request,declared),asyncCalls));
        var ack=Json.object();ack.put("invocationHandle",handle).put("submittedAt",Instant.now().toString());
        try {return executed(id,ack,null,provenance(null,"HTTP_ASYNC_SUBMISSION",false,null,null),ack);}
        catch(java.io.IOException failure){throw new IllegalStateException("Cannot persist asynchronous submission",failure);}
    }
    @Override public StepResult await(String id,JsonNode handle,int timeout) {
        String key=handle.asText();var invocation=pending.get(key);
        if(invocation==null)throw new IllegalArgumentException("Unknown invocation handle");
        if(timeout<=0||timeout>300)throw new IllegalArgumentException("Await timeout must be 1..300 seconds");
        try {
            StepResult original=invocation.get(timeout,TimeUnit.SECONDS);
            if(original.driverStatus()!=StepResult.DriverStatus.EXECUTED)return StepResult.missing(id,original.reason());
            var data=original.data().deepCopy();((ObjectNode)data).set("invocationHandle",handle);
            ((ObjectNode)data).put("completed",true).put("terminalStatus",original.driverStatus()==StepResult.DriverStatus.EXECUTED && original.data().path("httpStatus").asInt()<400?"SUCCEEDED":"FAILED");
            return new StepResult(id,original.driverStatus(),data,original.response(),original.reason(),original.provenance(),original.artifactRefs());
        } catch(TimeoutException failure){throw new IllegalStateException("Actual invocation did not reach terminal response within timeout",failure);}
        catch(InterruptedException failure){Thread.currentThread().interrupt();throw new IllegalStateException("Actual await interrupted",failure);}
        catch(ExecutionException failure){throw new IllegalStateException("Actual asynchronous transport failed",failure.getCause());}
    }
}
