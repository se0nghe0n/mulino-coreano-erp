package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.http.*;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import org.mulino.verification.*;

/** Calls the product and captures its unmodified JSON response; never loads an oracle. */
public final class ActualAcceptanceDriver implements AcceptanceDriver, IndependentDbObserver {
    private static final Set<String> QUERIES=Set.of("getObject","getWork","getInventory","getObligations","traceLot","getAssessment","getEvidence","getDefinition","searchObjects","searchWorks");
    private final Path root;
    private final ActualConfiguration configuration;
    private final JwtSigner signer;
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
    private final String run=UUID.randomUUID().toString();
    private final FixtureInstaller fixtures;
    private final JdbcObservation observer;
    public ActualAcceptanceDriver(Path root,ActualConfiguration configuration) {
        this.root=root;this.configuration=configuration;signer=new JwtSigner(configuration);
        fixtures=new FixtureInstaller(configuration);observer=new JdbcObservation(configuration);
    }
    public static ActualAcceptanceDriver fromEnvironment(Path root) {return new ActualAcceptanceDriver(root,ActualConfiguration.environment(System.getenv()));}
    @Override public Set<String> availableAdapters() {return Set.of("api","fixture","db");}
    @Override public StepResult installFixture(String id,JsonNode bundle) {
        try {var data=fixtures.install(bundle);return executed(id,data,null,provenance(null,"JDBC_FIXTURE_INSTALL",false,null,null),data);}
        catch(UnsupportedOperationException unsupported){return StepResult.missing(id,"NOT_IMPLEMENTED: "+unsupported.getMessage());}
        catch(Exception failure){throw new IllegalStateException("Actual fixture transaction failed: "+SqlFailureSummary.safe(failure),failure);}
    }
    @Override public StepResult query(String id,String route,JsonNode actor,String operation,JsonNode request) {
        if(!route.equals("api")||!QUERIES.contains(operation))return StepResult.missing(id,"NOT_IMPLEMENTED: actual query route/capability "+route+"/"+operation);
        try {
            var uri=configuration.baseUri().resolve("/api/ontology/queries/"+operation);
            var call=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).header("Content-Type","application/json").header("Authorization","Bearer "+signer.sign(actor)).POST(HttpRequest.BodyPublishers.ofString(request.toString())).build();
            var result=http.send(call,HttpResponse.BodyHandlers.ofString());
            JsonNode response=Json.parse(result.body());
            var receipt=Json.object();receipt.put("method","POST").put("path",uri.getPath()).put("httpStatus",result.statusCode()).put("capturedAt",Instant.now().toString());
            receipt.set("request",request);receipt.set("response",response);
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
            var sources=Json.object();var evidence=Json.object();evidence.put("complete",true).put("rowPointer","/rawRows/segments").put("artifactRef",ref);evidence.set("sourceQuery",data.path("sourceQuery"));sources.set("segments",evidence);data.set("sourceEvidence",sources);
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
        var p=Json.object();p.put("adapter","actual-s1").put("adapterVersion","1.0.0").put("buildVersion",configuration.buildVersion());
        p.set("authenticatedActor",actor==null?Json.MAPPER.nullNode():actor);p.put("source",source).put("independent",independent).put("scopeComplete",true);
        p.set("sourceQuery",query==null?Json.MAPPER.nullNode():query);p.set("snapshot",snapshot==null?Json.MAPPER.nullNode():snapshot);return p;
    }
    @Override public StepResult invoke(String id,String route,JsonNode actor,String capability,JsonNode request){return StepResult.missing(id,"NOT_IMPLEMENTED: product command adapter absent in S1");}
    @Override public StepResult control(String id,JsonNode request){return StepResult.missing(id,"NOT_IMPLEMENTED: actual host/clock/barrier controls absent");}
    @Override public StepResult start(String id,String route,JsonNode actor,String capability,JsonNode request){return StepResult.missing(id,"NOT_IMPLEMENTED: actual async submission absent");}
    @Override public StepResult await(String id,JsonNode handle,int timeout){return StepResult.missing(id,"NOT_IMPLEMENTED: actual async terminal observation absent");}
}
