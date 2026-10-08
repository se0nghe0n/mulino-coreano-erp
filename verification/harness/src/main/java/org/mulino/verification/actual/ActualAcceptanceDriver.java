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
    @Override public Set<String> availableAdapters() {return Set.of("api","fixture","db");}
    @Override public StepResult installFixture(String id,JsonNode bundle) {
        try {var bound=ActualFixtureBindings.bind(root,bundle,configuration);var data=fixtures.install(bound);var actors=bound.path("fixture").path("actors").elements();if(actors.hasNext()){var control=(ObjectNode)actors.next().deepCopy();if(data.hasNonNull("organizationExternalAlias"))control.put("organizationAlias",data.path("organizationExternalAlias").asText());controlActor=control;}return executed(id,data,null,provenance(null,"JDBC_FIXTURE_INSTALL",false,null,null),data);}
        catch(UnsupportedOperationException unsupported){return StepResult.missing(id,"NOT_IMPLEMENTED: "+unsupported.getMessage());}
        catch(Exception failure){throw new IllegalStateException("Actual fixture transaction failed: "+SqlFailureSummary.safe(failure),failure);}
    }
    @Override public StepResult query(String id,String route,JsonNode actor,String operation,JsonNode request) {
        if(!route.equals("api")||!QUERIES.contains(operation))return StepResult.missing(id,"NOT_IMPLEMENTED: actual query route/capability "+route+"/"+operation);
        return send(id,actor,"queries",operation,request);
    }
    private StepResult send(String id,JsonNode actor,String category,String operation,JsonNode request) {
        if(!operation.matches("[A-Za-z][A-Za-z0-9]*"))throw new IllegalArgumentException("Invalid operation name");
        try {
            var uri=configuration.baseUri().resolve("/api/ontology/"+category+"/"+operation);
            JsonNode wireRequest=request.deepCopy();
            if(category.equals("queries")&&wireRequest.path("scope").isObject())((ObjectNode)wireRequest.path("scope")).remove("caseId");
            String credential=signer.sign(actor);
            var call=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).header("Content-Type","application/json").header("Authorization","Bearer "+credential).POST(HttpRequest.BodyPublishers.ofString(wireRequest.toString())).build();
            Instant submittedAt=Instant.now();
            var result=http.send(call,HttpResponse.BodyHandlers.ofString());
            if(result.statusCode()==404)return StepResult.missing(id,"NOT_IMPLEMENTED: actual HTTP endpoint "+uri.getPath());
            JsonNode response=Json.parse(result.body());
            var receipt=Json.object();receipt.put("method","POST").put("path",uri.getPath()).put("httpStatus",result.statusCode()).put("submittedAt",submittedAt.toString()).put("capturedAt",Instant.now().toString());
            receipt.put("credentialSha256",java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(credential.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
            receipt.set("request",request);receipt.set("wireRequest",wireRequest);receipt.set("response",response);
            var identity=Json.object();identity.put("issuer",configuration.issuer()).put("subject",Json.required(actor,"subject")).put("organizationAlias",Json.required(actor,"organizationAlias"));
            var transport=Json.object();transport.put("httpStatus",result.statusCode());
            return executed(id,transport,response,provenance(identity,"HTTP",false,null,null),receipt);
        } catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new IllegalStateException("Actual HTTP interrupted",interrupted);}
        catch(Exception failure){throw new IllegalStateException("Actual HTTP environment/response failure",failure);}
    }
    @Override public StepResult observe(String id,JsonNode request) {
        try {
            var data=observer.capture(request);
            String ref="verification/harness/target/evidence/actual/"+run+"/"+UUID.randomUUID()+".json";
            ((ObjectNode)data.path("snapshot")).put("artifactRef",ref);
            var sources=Json.object();var observedNames=Json.array();if(Set.of("S3","S4").contains(request.path("profile").asText()))data.path("sourceEvidence").fieldNames().forEachRemaining(observedNames::add);else observedNames.addAll((com.fasterxml.jackson.databind.node.ArrayNode)request.path("sources"));for(JsonNode requested:observedNames){String name=requested.asText();var evidence=Json.object();evidence.put("complete",true).put("rowPointer","/rawRows/"+name.replace("~","~0").replace("/","~1")).put("artifactRef",ref);evidence.set("sourceQuery",data.path("sourceEvidence").path(name).hasNonNull("sourceQuery")?data.path("sourceEvidence").path(name).path("sourceQuery"):data.path("sourceQuery"));sources.set(name,evidence);}data.set("sourceEvidence",sources);
            Json.write(root.resolve(ref),data);
            return new StepResult(id,StepResult.DriverStatus.EXECUTED,data,null,null,provenance(null,"POSTGRESQL_JDBC",true,data.path("sourceQuery"),data.path("snapshot")),List.of(ref));
        } catch(UnsupportedOperationException unsupported){return StepResult.missing(id,"NOT_IMPLEMENTED: "+unsupported.getMessage());}
        catch(Exception failure){throw new IllegalStateException("Actual JDBC environment failure",failure);}
    }
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
        if(!route.equals("api"))return StepResult.missing(id,"NOT_IMPLEMENTED: actual command route "+route);
        return send(id,actor,request.path("intentKind").asText().equals("RECORD")?"records":"commands",capability,request);
    }
    @Override public StepResult control(String id,JsonNode request) {
        if(!request.path("type").asText().equals("clock")||!request.path("operation").asText().equals("advanceTo"))return StepResult.missing(id,"NOT_IMPLEMENTED: actual requested host/barrier control absent");
        if(controlActor==null)return StepResult.missing(id,"NOT_IMPLEMENTED: installed control identity absent");
        try {
            var body=Json.object();body.put("instant",Json.required(request.path("parameters"),"instant"));
            var uri=configuration.baseUri().resolve("/__verification/runtime/clock");
            var call=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).header("Content-Type","application/json").header("Authorization","Bearer "+signer.sign(controlActor)).POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
            var result=http.send(call,HttpResponse.BodyHandlers.ofString());
            if(result.statusCode()==404)return StepResult.missing(id,"NOT_IMPLEMENTED: verification clock profile not installed");
            var response=Json.parse(result.body());
            var data=clockAcknowledgment(result.statusCode(),body.path("instant").asText(),response);
            var receipt=Json.object();receipt.put("httpStatus",result.statusCode()).put("path",uri.getPath());receipt.set("requestedControl",request);receipt.set("wireRequest",body);receipt.set("serverResponse",response);
            return executed(id,data,response,provenance(null,"HTTP_VERIFICATION_CLOCK",false,null,null),receipt);
        }catch(InterruptedException failure){Thread.currentThread().interrupt();throw new IllegalStateException("Actual clock interrupted",failure);}
        catch(Exception failure){throw new IllegalStateException("Actual clock environment/response failure",failure);}
    }
    static ObjectNode clockAcknowledgment(int httpStatus,String requestedInstant,JsonNode serverResponse) {
        if(httpStatus!=200||!serverResponse.path("authorityClock").asBoolean()||!"verification".equals(serverResponse.path("profile").asText())||!Instant.parse(requestedInstant).equals(Instant.parse(serverResponse.path("instant").asText())))throw new IllegalStateException("Verification clock control did not prove applied authority clock");
        var data=Json.object();data.put("acknowledged",true).put("controlType","clock").put("operation","advanceTo").put("acknowledgedAt",Instant.now().toString());data.set("instant",serverResponse.path("instant"));data.set("serverObservation",serverResponse);return data;
    }
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
