package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mulino.verification.*;
import static org.junit.jupiter.api.Assertions.*;

/** Bounded HTTP/SQL contract checks. Does not invoke CaseRunner, Main, or a scenario profile. */
@EnabledIfEnvironmentVariable(named="T17_PG_TEST",matches="true")
class T17ReadQuantityPostgresTest {
 final Path root=Path.of(System.getProperty("repo.root"));
 ActualConfiguration config;ObjectNode fixture,installed,aliases;String sid,org;
 final Map<String,JsonNode> results=new LinkedHashMap<>();
 ObjectNode envelope(){return Json.object().put("driverStatus","EXECUTED").set("provenance",Json.object().put("scopeComplete",true));}
 void save(String name,JsonNode value)throws Exception{Files.writeString(Path.of(System.getenv("T17_TEST_EVIDENCE")).resolve(sid+"-"+name+".json"),value.toPrettyString());}
 void install(String selected)throws Exception{
  sid=selected;results.clear();config=ActualConfiguration.environment(System.getenv());
  fixture=(ObjectNode)Json.read(root.resolve("verification/cases/T17/fixtures/"+sid+".json"));
  var bundle=Json.object().put("fixtureHash",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(fixture.toString().getBytes(StandardCharsets.UTF_8))));bundle.set("fixture",fixture);
  bundle=(ObjectNode)ActualFixtureBindings.bind(root,bundle,config);fixture=(ObjectNode)bundle.path("fixture");
  installed=ScenarioFixtureInstaller.install(config,root,bundle,true);aliases=(ObjectNode)installed.path("aliasMap");org=aliases.path("ORG").asText();
  ScenarioRevisionBinding.reset(List.of(org));var setup=envelope();setup.set("data",installed);results.put("setup",setup);save("setup",setup);
 }
 JsonNode gateway(String actor,String category,String cap,JsonNode body)throws Exception{
  var credential=(ObjectNode)fixture.path("actors").path(actor).deepCopy();credential.put("organizationAlias",installed.path("organizationExternalAlias").asText());
  var http=HttpClient.newHttpClient().send(HttpRequest.newBuilder(config.baseUri().resolve("/api/ontology/"+category+"/"+cap)).timeout(Duration.ofSeconds(60)).header("Content-Type","application/json").header("Authorization","Bearer "+new JwtSigner(config).sign(credential)).POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(),HttpResponse.BodyHandlers.ofString());
  assertEquals(200,http.statusCode(),http.body());return Json.parse(http.body());
 }
 JsonNode subcase()throws Exception{for(var sub:Json.read(root.resolve("verification/cases/T17/case.json")).path("subcases"))if(sid.equals(sub.path("id").asText()))return sub;throw new AssertionError(sid);}
 void prefixThrough(String last)throws Exception{
  for(var a:subcase().path("actions")){
   String id=a.path("id").asText(),kind=a.path("kind").asText();if(kind.equals("installFixture"))continue;
   var result=envelope();
   if(kind.equals("invoke")||kind.equals("query")){
    JsonNode request=new ReferenceResolver(results,aliases).resolve(a.path("request"));String digest=kind.equals("query")?ScenarioRevisionBinding.digest(config):null;
    var response=gateway(a.path("actorRef").asText(),kind.equals("query")?"queries":request.path("intentKind").asText().equals("RECORD")?"records":"commands",a.path("capabilityId").asText(),request);
    result.set("response",response);result.set("request",request);
    if(kind.equals("query"))ScenarioRevisionBinding.bind(config,id,digest,response.path("snapshotRevision").asText());
   }else if(kind.equals("observe")){
    var original=a.path("observation");var request=(ObjectNode)original.deepCopy();var scope=new ReferenceResolver(results,aliases).resolve(original.path("scope"));request.set("scope",scope);
    var prior=original.path("snapshotRef").path("$result");request.put("snapshotRef","RESULT_REVISION");
    var issuing=Json.object().put("actionId",prior.path("actionId").asText()).put("kind","query").put("route","api");request.set("snapshotSource",issuing);
    var captured=ScenarioJdbcObservation.capture(config,root,request);assertTrue(captured.at("/snapshot/revisionQuery/parameters/digestMatched").asBoolean(),captured.toPrettyString());
    result.set("data",captured);result.set("request",request);
   }else throw new AssertionError("Unexpected focused action: "+id);
   results.put(id,result);save(id,result);if(id.equals(last))return;
  }
  throw new AssertionError("Missing focused stop "+last);
 }
 void selectedAssertions()throws Exception{
  var verdicts=Json.array();int count=0;
  for(var a:subcase().path("assertions")){
   String id=a.path("id").asText();boolean own=id.startsWith("read-quantity-")||id.startsWith("condition-source-")||id.startsWith("eligibility-response-")||id.startsWith("unauthorized-packaging-");
   if(!own&&!Set.of("held-after-reserve-1","held-after-reserve-2","eligible-after-reserve-3","unreserved-after-reserve-5","eligible-after-reserve-4","unreserved-after-reserve-6","eligibility-response-3","eligibility-positive-control","no-prior-dispatch-24","eligibility-response-1","eligibility-response-2").contains(id))continue;
   var engine=new AssertionEngine();try{engine.check(a,results,aliases);var proof=Json.object().put("id",id).put("status","PASS");proof.set("assertion",a);proof.set("resolvedAssertion",engine.resolvedAssertion());proof.set("observed",engine.observedValue());verdicts.add(proof);count++;}
   catch(AssertionError error){save("focused-assertions",verdicts);throw new AssertionError(sid+"/"+id+": "+error.getMessage(),error);}
  }
  assertTrue(count>=10);save("focused-assertions",verdicts);
 }
 @ParameterizedTest @ValueSource(strings={"reserve-pick-dispatch","eligibility-unknown","eligibility-conflict","eligibility-fefo-no-reason","eligibility-fefo-with-reason","eligibility-packaging-unapproved","claim-nopriordispatch"})
 void exactPublicQuantityAndIndependentPhysicalSourceContract(String selected)throws Exception{
  install(selected);var beforePhysical=physicalRows();save("independent-physical-before-sql",beforePhysical);prefixThrough(selected.equals("claim-nopriordispatch")?"read-quantity-after-confirm-db":selected.equals("reserve-pick-dispatch")?"reserved-db":"after-reserve-db");selectedAssertions();
  // Independently inspect physical SQL: no permission fields are persisted onto
  // a leaf by this contract. Exact zero rows are observed, not missing values.
  var afterPhysical=physicalRows();save("independent-physical-sql",afterPhysical);assertEquals(beforePhysical,afterPhysical,"Reservation/intake/denial must preserve physical identities, quantities, custody and nullable ownership");
 }
 JsonNode physicalRows()throws Exception{
  try(var db=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password());var s=db.prepareStatement("SELECT ID,itemId,lotId,placeId,quantity::text,unit,retiredAt,custodianId,ownerId FROM mulino_inventory_QuantitySegments WHERE organizationId=? ORDER BY ID")){
   s.setString(1,org);try(var rows=s.executeQuery()){var raw=Json.array();while(rows.next()){var row=Json.object();for(int i=1;i<=9;i++){String value=rows.getString(i);if(value==null)row.putNull(rows.getMetaData().getColumnLabel(i));else row.put(rows.getMetaData().getColumnLabel(i),value);}raw.add(row);}assertFalse(raw.isEmpty());return raw;}
  }
 }
}
