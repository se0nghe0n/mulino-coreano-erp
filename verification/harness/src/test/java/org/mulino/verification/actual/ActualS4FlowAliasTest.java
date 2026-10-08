package org.mulino.verification.actual;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.mulino.verification.Json;
import static org.junit.jupiter.api.Assertions.*;
final class ActualS4FlowAliasTest {
 private final Path root=Path.of(System.getProperty("repo.root",".")).toAbsolutePath().normalize();
 @Test void everyCommittedFlowBindsEachAliasBeforeItIsRead()throws Exception {
  var flows=NativeS4FlowAliases.flows(root);
  var suite=new java.util.ArrayList<String>();for(var include:Json.read(root.resolve(NativeS4FlowAliases.DIRECTORY+"/flow.json")).path("actions"))suite.add(include.path("scriptRef").asText());
  for(String required:new String[]{"e1-flow.json","e2-flow.json","c4-flow.json","c1-flow.json","flow.json"})assertTrue(flows.contains(NativeS4FlowAliases.DIRECTORY+"/"+required),required);
  // The default ./verify actual-s4 flow is the suite of every committed case flow.
  assertEquals(flows.stream().filter(f->!f.endsWith("/flow.json")).sorted().toList(),suite.stream().sorted().toList());
  for(String flow:flows)assertEquals(java.util.List.of(),NativeS4FlowAliases.check(root,flow),flow);
 }
 @Test void unboundAliasAndLateBindingAreReported(@org.junit.jupiter.api.io.TempDir Path temp)throws Exception {
  Path dir=temp.resolve(NativeS4FlowAliases.DIRECTORY);Files.createDirectories(dir);Files.copy(root.resolve(NativeS4FlowAliases.BASE_FIXTURE),temp.resolve(NativeS4FlowAliases.BASE_FIXTURE));
  Files.writeString(dir.resolve("bad-flow.json"),"""
   {"actions":[
    {"id":"read-before-bind","type":"command","capability":"pickQuantity","request":{"slots":{"allocationId":"$ALLOCATION","template":"x-${LATER}"}}},
    {"id":"bind","type":"command","capability":"reserveQuantity","request":{},"bind":{"ALLOCATION":"/effects/allocationId","LATER":"/id"}},
    {"id":"renamed","type":"uuid","alias":"c4-RANGE60"},
    {"id":"receipt","type":"original","fixture":{"content":{"rangeRootId":"$RANGE60"}},"binding":{"organizationId":"$ORG"}},
    {"id":"segment","type":"observe","assertions":[{"pointer":"/rawRows/x","operator":"matchingRows","where":{"id":"$receipt40.segment"},"expected":1}]},
    {"id":"unlinked","type":"query","capability":"getObject","request":{"id":"$receipt.canonical","document":"$receipt.document","rev":"$ALLOCATION"}}]}
   """);
  var errors=NativeS4FlowAliases.check(temp,NativeS4FlowAliases.DIRECTORY+"/bad-flow.json");
  assertEquals(5,errors.size(),errors.toString());
  for(String alias:new String[]{"$ALLOCATION","${LATER}","$RANGE60","$receipt40.segment","$receipt.canonical"})assertTrue(errors.stream().anyMatch(e->e.contains("unbound "+alias)),alias);
  assertNotNull(Json.read(dir.resolve("bad-flow.json")));
 }
 @Test void observedTableMustExistInMigrations(@org.junit.jupiter.api.io.TempDir Path temp)throws Exception {
  Path dir=temp.resolve(NativeS4FlowAliases.DIRECTORY);Files.createDirectories(dir);Files.copy(root.resolve(NativeS4FlowAliases.BASE_FIXTURE),temp.resolve(NativeS4FlowAliases.BASE_FIXTURE));
  Files.createDirectories(temp.resolve("database/migrations"));Files.writeString(temp.resolve("database/migrations/V1__x.sql"),"CREATE TABLE mulino_inventory_Restrictions(id int);");
  Files.writeString(dir.resolve("table-flow.json"),"{\"actions\":[{\"id\":\"o\",\"type\":\"observe\",\"assertions\":[{\"pointer\":\"/rawRows/mulino_inventory_restrictions\",\"operator\":\"size\",\"expected\":0},{\"pointer\":\"/rawRows/mulino_quality_restrictions\",\"operator\":\"size\",\"expected\":0}]}]}");
  assertEquals(java.util.List.of(NativeS4FlowAliases.DIRECTORY+"/table-flow.json: o observes unknown table mulino_quality_restrictions"),NativeS4FlowAliases.check(temp,NativeS4FlowAliases.DIRECTORY+"/table-flow.json"));
 }
}
