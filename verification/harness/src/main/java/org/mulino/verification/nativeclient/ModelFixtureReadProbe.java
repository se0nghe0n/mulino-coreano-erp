package org.mulino.verification.nativeclient;

import org.mulino.verification.*;
import org.mulino.verification.actual.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.Set;

/** Read-only real product HTTP probe; its output contains no bearer, key, corpus answer or model invocation. */
public final class ModelFixtureReadProbe {
    public static void main(String[] args) throws Exception {
        if(args.length!=2&&args.length!=5)throw new IllegalArgumentException("installation-receipt.json output.json");
        Path root=Path.of(System.getProperty("repo.root",".")).toAbsolutePath().normalize();var config=ActualConfiguration.environment(System.getenv());var receipt=Json.read(root.resolve(args[0]));
        var fixture=Json.read(root.resolve("verification/model-binding/cases/"+(args.length==5?args[2]:"M02")+"/fixture.json"));var actor=(com.fasterxml.jackson.databind.node.ObjectNode)fixture.path("actors").path("command-actor").deepCopy();actor.put("organizationAlias",receipt.path("organizationExternalAlias").asText());
        String operation=args.length==5?args[3]:"getObject",alias=args.length==5?args[4]:"P";
        var request=Json.object().put("id",receipt.path("aliasMap").path(alias).asText()).put("asOf",fixture.path("clock").path("asOf").asText()).put("knownAt",fixture.path("clock").path("knownAt").asText());
        String endpoint=operation.equals("download")?"/api/evidence/documents/"+receipt.path("aliasMap").path(alias).asText()+"/content":"/api/ontology/queries/"+operation;
        if(!Set.of("getObject","getEvidence","getWork","getAssessment","download").contains(operation))throw new IllegalArgumentException("Read-only operation required");
        var builder=HttpRequest.newBuilder(config.baseUri().resolve(endpoint)).header("Content-Type","application/json").header("Authorization","Bearer "+new JwtSigner(config).sign(actor)).timeout(Duration.ofSeconds(15)).timeout(Duration.ofSeconds(15));
        var call=(operation.equals("download")?builder.GET():builder.POST(HttpRequest.BodyPublishers.ofString(request.toString()))).build();
        var response=HttpClient.newHttpClient().send(call,HttpResponse.BodyHandlers.ofString());var output=Json.object().put("evidenceClass","ACTUAL_FOCUSED_HTTP_PG_READ").put("httpStatus",response.statusCode()).put("actualModelCalls",0).put("fixtureSha256",receipt.path("fixtureSha256").asText()).put("acceptanceStatus","NOT_RUN");output.put("endpoint",endpoint);output.set("request",request);output.put("rawResponse",response.body());output.set("purposeGrantBindings",receipt.path("purposeGrantBindings"));
        Path out=root.resolve(args[1]).normalize();if(!out.startsWith(root))throw new IllegalArgumentException("Output escape");Files.createDirectories(out.getParent());Json.MAPPER.writerWithDefaultPrettyPrinter().writeValue(out.toFile(),output);System.out.println(response.statusCode());
    }
    private ModelFixtureReadProbe() {}
}
