package org.mulino.verification.actual;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mulino.verification.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Exercises JSON schema plus the same request-bound semantic gate as CaseRunner. */
@EnabledIfEnvironmentVariable(named="OBSERVER_PG_TEST",matches="true")
class MeasurementMetadataPostgresTest {
 final ScenarioObservationPostgresTest f=new ScenarioObservationPostgresTest();
 @BeforeEach void setup(TestInfo info)throws Exception{f.setup(info);f.base();}
 @AfterEach void cleanup()throws Exception{f.cleanup();}
 @Test void emptyBusinessRowsStillHaveActualItemAndUnitLineageThroughTheNativeSemanticGate()throws Exception{
  f.insert("mulino_inventory_TradeItems",f.row(f.item).put("baseUnit","KG"));
  var result=f.capture("deliveries");assertTrue(result.at("/rawRows/deliveries").isEmpty());
  assertEquals(f.item,result.at("/data/itemId").asText());assertEquals("KG",result.at("/data/unit").asText());
  assertEquals(f.item,result.at("/measurementMetadataRows/0/id").asText());
  assertEquals("/measurementMetadataRows",result.at("/derivations/~1itemId/rowPointer").asText());
 }
 ObjectNode actual()throws Exception{f.insert("mulino_inventory_TradeItems",f.row(f.item).put("baseUnit","KG"));return (ObjectNode)f.capture("deliveries");}
 @ParameterizedTest @ValueSource(strings={"missing","foreign","otherItem","futureCreated","futureRecorded","nullUnit","blankUnit"})
 void unavailableMetadataDoesNotInventItemOrUnit(String variant)throws Exception{
  if(!variant.equals("missing")){
   var item=f.row(variant.equals("otherItem")?ScenarioObservationPostgresTest.id():f.item).put("baseUnit",variant.equals("blankUnit")?" ":"KG");
   if(variant.equals("nullUnit"))item.putNull("baseUnit");if(variant.equals("foreign"))item.put("organizationId",ScenarioObservationPostgresTest.id());
   if(variant.equals("futureCreated"))item.put("createdAt",f.later);if(variant.equals("futureRecorded"))item.put("recordedAt",f.later);
   f.insert("mulino_inventory_TradeItems",item);
  }
  var result=f.capture("deliveries");assertTrue(result.path("data").isEmpty());assertFalse(result.has("derivations"));
  assertEquals(variant.endsWith("Unit")?1:0,result.path("measurementMetadataRows").size());
 }
 @Test void ambiguousActualMetadataIsUnsupportedEvenWhenUnitsAgree()throws Exception{
  for(int n=0;n<2;n++)f.insert("mulino_inventory_TradeItems",f.row(f.item).put("baseUnit","KG"));
  assertThrows(UnsupportedOperationException.class,()->f.capture("deliveries"));
 }
 @ParameterizedTest @ValueSource(strings={"missingItem","multipleItems","multipleOrganizations","plural"})
 void metadataIsBoundToExactlyOneOrganizationAndItemWithoutInventingASource(String variant)throws Exception{
  f.insert("mulino_inventory_TradeItems",f.row(f.item).put("baseUnit","KG"));var scope=f.scope();
  if(variant.equals("missingItem"))scope.remove("itemId");
  if(variant.equals("multipleItems"))scope.putArray("itemIds").add(ScenarioObservationPostgresTest.id());
  if(variant.equals("multipleOrganizations"))scope.putArray("organizationIds").add(ScenarioObservationPostgresTest.id());
  if(variant.equals("plural")){scope.remove(List.of("organizationId","itemId"));scope.putArray("organizationIds").add(f.org);scope.putArray("itemIds").add(f.item);}
  var request=f.request(scope,"deliveries");var result=ScenarioJdbcObservation.capture(f.config,f.root,request);
  // Bind the actual capture once, then run exactly the production schema/context/derivation checks.
  String ref="verification/harness/target/evidence/measurement-scope-"+UUID.randomUUID()+".json";
  ((ObjectNode)result.path("snapshot")).put("artifactRef",ref);result.path("sourceEvidence").forEach(e->((ObjectNode)e).put("artifactRef",ref));Json.write(f.root.resolve(ref),result);f.validate(request,result);
  assertEquals(variant.equals("plural"),result.has("measurementMetadataRows"));assertEquals(variant.equals("plural"),result.path("data").has("itemId"));
  assertEquals(Set.of("deliveries"),fieldNames(result.path("rawRows")));assertEquals(Set.of("deliveries"),fieldNames(result.path("sourceEvidence")));
 }
 Set<String> fieldNames(JsonNode node){var names=new HashSet<String>();node.fieldNames().forEachRemaining(names::add);return names;}
 @ParameterizedTest @ValueSource(strings={"dataItem","dataUnit","rowItem","rowOrg","baseUnit","futureRecorded","futureCreated","noRows","duplicateRows","missingRows","missingQuery","wrongTable","wrongSql","boundOrg","boundItem","boundRecorded","boundCreated","wrongPointer","wrongField","wrongAggregate","businessValue","unrequestedSource","multiScope"})
 void tamperedValuesRowsSqlScopeAndBusinessSourceSubstitutionFailSemanticValidation(String variant)throws Exception{
  var result=actual().deepCopy();var item=(ObjectNode)result.at("/measurementMetadataRows/0");var query=(ObjectNode)result.at("/snapshot/measurementMetadataQueries/0");
  var binding=(com.fasterxml.jackson.databind.node.ArrayNode)query.path("boundValues");var derivation=(ObjectNode)result.at("/derivations/~1itemId");
  switch(variant){
   case "dataItem"->((ObjectNode)result.path("data")).put("itemId","unrelated");case "dataUnit"->((ObjectNode)result.path("data")).put("unit","BOX");
   case "rowItem"->item.put("id","unrelated");case "rowOrg"->item.put("organizationId","foreign");case "baseUnit"->item.put("baseUnit","BOX");
   case "futureRecorded"->item.put("recordedAt",f.later);case "futureCreated"->item.put("createdAt",f.later);
   case "noRows"->((com.fasterxml.jackson.databind.node.ArrayNode)result.path("measurementMetadataRows")).removeAll();
   case "duplicateRows"->((com.fasterxml.jackson.databind.node.ArrayNode)result.path("measurementMetadataRows")).add(item.deepCopy());
   case "missingRows"->result.remove("measurementMetadataRows");case "missingQuery"->((ObjectNode)result.path("snapshot")).remove("measurementMetadataQueries");
   case "wrongTable"->query.put("table","mulino_inventory_QuantitySegments");case "wrongSql"->query.put("sql",query.path("sql").asText().replace("r.organizationId=? AND ",""));
   case "boundOrg"->binding.set(0,Json.MAPPER.valueToTree("foreign"));case "boundItem"->binding.set(1,Json.MAPPER.valueToTree("other"));
   case "boundRecorded"->binding.set(2,Json.MAPPER.valueToTree(f.later));case "boundCreated"->binding.set(3,Json.MAPPER.valueToTree(f.later));
   case "wrongPointer"->derivation.put("rowPointer","/rawRows/TradeItems");case "wrongField"->derivation.put("field","baseUnit");case "wrongAggregate"->derivation.put("aggregate","distinct");
   case "businessValue"->{((ObjectNode)result.path("data")).put("heldQuantity",f.item);((ObjectNode)result.path("derivations")).set("/heldQuantity",derivation.deepCopy());}
   case "unrequestedSource"->{((ObjectNode)result.path("rawRows")).set("notRequested",result.path("measurementMetadataRows"));derivation.put("rowPointer","/rawRows/notRequested");}
   case "multiScope"->((ObjectNode)result.path("scope")).putArray("itemIds").add("other");
  }
  var requested=f.request(f.scope(),"deliveries");
  assertThrows(IllegalArgumentException.class,()->ObserverDerivations.verify(requested.path("sources"),result),variant);
  assertThrows(IllegalArgumentException.class,()->f.validate(requested,result),variant);
 }
 @Test void unitIdentityDoesNotUseDecimalQuantityEquivalence()throws Exception{
  f.insert("mulino_inventory_TradeItems",f.row(f.item).put("baseUnit","1"));var result=(ObjectNode)f.capture("deliveries");
  ((ObjectNode)result.path("data")).put("unit","1.0");
  assertThrows(IllegalArgumentException.class,()->ObserverDerivations.verify(f.request(f.scope(),"deliveries").path("sources"),result));
 }
 @Test void coherentlyRewrittenRowsAndValuesStillCannotReplacePersistedSnapshotEvidence()throws Exception{
  var result=actual().deepCopy();((ObjectNode)result.at("/measurementMetadataRows/0")).put("baseUnit","BOX");((ObjectNode)result.path("data")).put("unit","BOX");
  ObserverDerivations.verify(f.request(f.scope(),"deliveries").path("sources"),result);
  assertTrue(assertThrows(IllegalArgumentException.class,()->f.validate(f.request(f.scope(),"deliveries"),result)).getMessage().contains("persisted snapshot artifact"));
 }
 @Test void nativeCaseRunnerExecutesTheWholeObservationValidationPathOnRealPgCapture()throws Exception{
  var captured=actual();var request=f.request(f.scope(),"deliveries").put("snapshotRef","CURRENT_COMMITTED");
  var sourceCase=Json.read(f.root.resolve("verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json"));
  var sub=(ObjectNode)sourceCase.path("subcases").get(0);var action=Json.object().put("id","observe").put("kind","observe");action.putArray("evidenceRefs").add("observe:actual-jdbc-capture");action.set("observation",request);sub.putArray("actions").add(action);
  var assertion=(ObjectNode)sub.path("assertions").get(0).deepCopy();assertion.put("id","metadata-unit").put("op","equals");assertion.remove(List.of("unit","unitSource","baseline","baselineUnitSource"));assertion.set("source",Json.object().put("actionId","observe").put("pointer","/data/data/unit"));assertion.put("expected","KG");assertion.putArray("evidenceRefs").add("observe:actual-jdbc-capture");sub.putArray("assertions").add(assertion);
  Path casePath=Files.createTempFile(f.root.resolve("verification/harness/target"),"measurement-validation-",".json");Json.write(casePath,sourceCase);
  class JdbcPort implements AcceptanceDriver,IndependentDbObserver{
   public Set<String> availableAdapters(){return Set.of("db");}
   public StepResult observe(String id,JsonNode input){assertEquals(request,input);return f.step(captured);}
   public StepResult installFixture(String id,JsonNode input){throw new UnsupportedOperationException();}
   public StepResult invoke(String id,String route,JsonNode actor,String cap,JsonNode input){throw new UnsupportedOperationException();}
   public StepResult query(String id,String route,JsonNode actor,String cap,JsonNode input){throw new UnsupportedOperationException();}
   public StepResult control(String id,JsonNode input){throw new UnsupportedOperationException();}
   public StepResult start(String id,String route,JsonNode actor,String cap,JsonNode input){throw new UnsupportedOperationException();}
   public StepResult await(String id,JsonNode input,int timeout){throw new UnsupportedOperationException();}
  }
  try{var runner=new CaseRunner(new ContractValidator(f.root),new JdbcPort(),new AgentRunner.Scripted(),casePath,sub.path("id").asText());runner.execute("observe");assertEquals("EXECUTED",runner.results().get("observe").path("driverStatus").asText());}
  finally{Files.deleteIfExists(casePath);}
 }

}
