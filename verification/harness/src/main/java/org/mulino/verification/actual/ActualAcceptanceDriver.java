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
    private volatile JsonNode controlActor;
    private final FixtureInstaller fixtures;
    private final JdbcObservation observer;
    public ActualAcceptanceDriver(Path root,ActualConfiguration configuration) {
        this.root=root;this.configuration=configuration;signer=new JwtSigner(configuration);
        fixtures=new FixtureInstaller(configuration);observer=new JdbcObservation(configuration);
    }
    public static ActualAcceptanceDriver fromEnvironment(Path root) {return new ActualAcceptanceDriver(root,ActualConfiguration.environment(System.getenv()));}
    @Override public Set<String> availableAdapters() {return Set.of("api","mcp","wire","fixture","db");}
    @Override public StepResult installFixture(String id,JsonNode bundle) {
        // A new installation is an isolation boundary: never carry the previous subcase's organization or control identity.
        if(suiteIsolation){externalOrganization=null;externalOrganizations=null;controlActor=null;installedActors=null;}
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
    @Override public StepResult query(String id,String route,JsonNode actor,String operation,JsonNode request) {
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
            java.net.URI uri=configuration.baseUri().resolve("/api/ontology/"+category+"/"+operation);
            JsonNode wireRequest=request.deepCopy();
            var translated=Json.array();
            if(relaxWire&&category.equals("queries")&&wireRequest.has("objectId")&&!wireRequest.has("id")){((ObjectNode)wireRequest).set("id",wireRequest.path("objectId"));((ObjectNode)wireRequest).remove("objectId");translated.add("objectId->id");}
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
            if(suiteIsolation&&!category.equals("queries")&&wireRequest instanceof ObjectNode command)ScenarioWireMapping.command(operation,command,translated);
            if(relaxWire&&!category.equals("queries")&&wireRequest.isObject()) {
                // Probe mode only: complete the contracts/intent.schema.json envelope the cases omit. Fields outside the
                // schema are dropped, valueProvenance becomes provenance, and a missing provenance marks every slot USER.
                var w=(ObjectNode)wireRequest;
                for(String key:iterable(w))if(!INTENT_FIELDS.contains(key)&&!key.equals("valueProvenance")){w.remove(key);translated.add(key);}
                if(!w.has("provenance")&&w.path("valueProvenance").isObject()){w.set("provenance",w.path("valueProvenance"));translated.add("valueProvenance->provenance");}
                w.remove("valueProvenance");
                if(!w.has("provenance")&&w.path("slots").isObject()){var prov=Json.object();w.path("slots").fieldNames().forEachRemaining(k->prov.put(k,"USER"));w.set("provenance",prov);translated.add("provenance:USER-default");}
            }
            String credential=signer.sign(credentialActor(actor));
            String digestBefore=category.equals("queries")&&suiteIsolation?ScenarioRevisionBinding.digest(configuration):null;
            String body=wireRequest.toString();ObjectNode rpc=null;
            var builder=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).header("Content-Type","application/json").header("Authorization","Bearer "+credential);
            if(route.equals("mcp")) {
                uri=configuration.baseUri().resolve("/mcp/ontology");
                rpc=Json.object();rpc.put("jsonrpc","2.0").put("id",id).put("method","tools/call");var params=rpc.putObject("params");params.put("name",operation);params.set("arguments",wireRequest);
                var meta=params.putObject("_meta");meta.put("io.modelcontextprotocol/protocolVersion",MCP_VERSION);meta.putObject("io.modelcontextprotocol/clientInfo").put("name","mulino-actual-adapter").put("version","2.1.0");meta.putObject("io.modelcontextprotocol/clientCapabilities");
                body=rpc.toString();
                builder=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).header("Content-Type","application/json").header("Accept","application/json, text/event-stream").header("Mcp-Method","tools/call").header("MCP-Protocol-Version",MCP_VERSION).header("Mcp-Name",operation).header("Authorization","Bearer "+credential);
            }
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
            receipt.set("request",request);receipt.set("wireRequest",wireRequest);receipt.set("response",response);if(!translated.isEmpty())receipt.set("adapterTranslations",translated);
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
    @Override public StepResult invoke(String id,String route,JsonNode actor,String capability,JsonNode request) {
        if(!route.equals("api")&&!(suiteIsolation&&route.equals("mcp")))return StepResult.missing(id,"NOT_IMPLEMENTED: actual command route "+route);
        return send(id,actor,request.path("intentKind").asText().equals("RECORD")?"records":"commands",capability,request,route);
    }
    @Override public StepResult control(String id,JsonNode request) {
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
    static ObjectNode clockAcknowledgment(int httpStatus,String requestedInstant,JsonNode serverResponse) {
        if(httpStatus!=200||!serverResponse.path("authorityClock").asBoolean()||!"verification".equals(serverResponse.path("profile").asText())||!Instant.parse(requestedInstant).equals(Instant.parse(serverResponse.path("instant").asText())))throw new IllegalStateException("Verification clock control did not prove applied authority clock");
        var data=Json.object();data.put("acknowledged",true).put("controlType","clock").put("operation","advanceTo").put("acknowledgedAt",Instant.now().toString());data.set("instant",serverResponse.path("instant"));data.set("serverObservation",serverResponse);return data;
    }
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
    private volatile JsonNode installedActors;
    @Override public StepResult start(String id,String route,JsonNode actor,String capability,JsonNode request) {
        if(!route.equals("api"))return StepResult.missing(id,"NOT_IMPLEMENTED: actual async route "+route);
        String handle=UUID.randomUUID().toString();
        pending.put(handle,CompletableFuture.supplyAsync(()->invoke(id,route,actor,capability,request)));
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
