package org.mulino.verification.actual;
import java.nio.file.*;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.mulino.verification.Json;
import static org.junit.jupiter.api.Assertions.*;
final class ActualS4FixtureSafetyTest {
 @Test void seededFinalQuantityIsRejectedBeforeDatabaseConnection()throws Exception {
  Path root=Path.of(System.getProperty("repo.root",".")).toAbsolutePath();
  var fixture=Json.read(root.resolve("verification/actual/s4/fixture.json"));
  var bundle=Json.object();bundle.set("fixture",fixture);bundle.set("bases",Json.array());bundle.put("fixtureHash","synthetic");
  var finalQuantity=Json.object();finalQuantity.put("type","QuantitySegment").put("quantity","100");
  ((com.fasterxml.jackson.databind.node.ObjectNode)fixture.path("aliases")).set("FORBIDDEN_FINAL",finalQuantity);
  var config=new ActualConfiguration(URI.create("http://127.0.0.1:1"),"jdbc:postgresql://127.0.0.1:1/no-database","unused","unused",Path.of("unused"),"https://mulino-native.invalid","isolated-ontology","source");
  var failure=assertThrows(IllegalArgumentException.class,()->new FixtureInstaller(config).install(bundle));
  assertTrue(failure.getMessage().contains("public product commands"));
 }
 @Test void fixtureSchemaPreservesTypedDefinitionAndOriginalOnlyBaseline()throws Exception {
  Path root=Path.of(System.getProperty("repo.root",".")).toAbsolutePath();
  var fixture=new org.mulino.verification.ContractValidator(root).fixture("verification/actual/s4/fixture.json");
  assertTrue(fixture.path("baseline").isEmpty());assertTrue(fixture.path("responsibilities").isEmpty());
  for(var alias:fixture.path("aliases"))assertNotEquals("QuantitySegment",alias.path("type").asText());
  for(var noun:fixture.path("aliases").path("DEF").path("content").path("nouns"))assertTrue(noun.isObject());
 }
 @Test void everyAuthoredVerbPinsADeclaredCapability()throws Exception {
  // The server validates the whole pinned definition; one undeclared verb
  // capability HOLDs every goal as VERSION_UNSUPPORTED before any effect.
  Path root=Path.of(System.getProperty("repo.root",".")).toAbsolutePath();
  for(String ref:java.util.List.of("fixture.json","e2-fixture.json","c4-fixture.json")) {
   var definition=Json.read(root.resolve("verification/actual/s4/"+ref)).path("aliases").path("DEF").path("content");
   var declared=new java.util.HashSet<String>();for(var c:definition.path("capabilities"))declared.add(c.path("capabilityId").asText());
   for(var verb:definition.path("verbs"))assertTrue(declared.contains(verb.path("capabilityId").asText()),ref+" verb without capability "+verb.path("name"));
  }
 }
 @Test void repeatedSetupGetsADistinctExternalOrganizationAliasBeforeDatabaseConnection()throws Exception {
  Path root=Path.of(System.getProperty("repo.root",".")).toAbsolutePath();
  var bundle=Json.object();bundle.set("fixture",Json.read(root.resolve("verification/actual/s4/fixture.json")));bundle.set("bases",Json.array());bundle.put("fixtureHash","synthetic");
  bundle.put("organizationExternalAlias","x".repeat(81));
  var config=new ActualConfiguration(URI.create("http://127.0.0.1:1"),"jdbc:postgresql://127.0.0.1:1/no-database","unused","unused",Path.of("unused"),"https://mulino-native.invalid","isolated-ontology","source");
  var failure=assertThrows(IllegalArgumentException.class,()->new FixtureInstaller(config).install(bundle));
  assertTrue(failure.getMessage().contains("external alias"));
 }
}
