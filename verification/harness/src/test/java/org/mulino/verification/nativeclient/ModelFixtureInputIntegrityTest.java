package org.mulino.verification.nativeclient;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mulino.verification.Json;
import java.nio.file.*;
import java.util.*;

class ModelFixtureInputIntegrityTest {
  final Path root=Path.of(System.getProperty("repo.root"));
  @Test void currentBusinessDataAndBothVersionedConsumerSchemasRemainExactlyPinned()throws Exception {
    var lock=Json.read(root.resolve("verification/model-installation/v1/original-input-lock.json"));
    for(var artifact:lock.path("artifacts"))assertDoesNotThrow(()->ModelFixtureInputIntegrity.require(root,artifact));
    var original=lock.path("artifacts").get(0).deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)original).put("sha256","0".repeat(64));
    assertThrows(IllegalArgumentException.class,()->ModelFixtureInputIntegrity.require(root,original));
  }
  @Test void aChangedPriorSchemaCurrentSchemaOrUpgradeSetCannotMasqueradeAsPreservation(@TempDir Path temp)throws Exception {
    String ledger="verification/model-installation/v1/schema-upgrades/manifest.json";
    var manifest=Json.read(root.resolve(ledger));var lock=Json.read(root.resolve("verification/model-installation/v1/original-input-lock.json"));
    for(String mutation:List.of("before","after","extra","originalHash")) {
      Path copy=temp.resolve(mutation);Files.createDirectories(copy);
      var paths=new ArrayList<String>(List.of(ledger));for(var entry:manifest.path("upgrades")){paths.add(entry.path("path").asText());paths.add(entry.path("beforeSnapshot").asText());}
      for(String path:paths){Files.createDirectories(copy.resolve(path).getParent());Files.copy(root.resolve(path),copy.resolve(path));}
      var first=manifest.path("upgrades").get(0);var artifact=java.util.stream.StreamSupport.stream(lock.path("artifacts").spliterator(),false).filter(a->a.path("path").equals(first.path("path"))).findFirst().orElseThrow().deepCopy();
      if(mutation.equals("before"))Files.writeString(copy.resolve(first.path("beforeSnapshot").asText()),"{}");
      if(mutation.equals("after"))Files.writeString(copy.resolve(first.path("path").asText()),"{}");
      if(mutation.equals("originalHash"))((com.fasterxml.jackson.databind.node.ObjectNode)artifact).put("sha256","0".repeat(64));
      if(mutation.equals("extra")){var altered=manifest.deepCopy();((com.fasterxml.jackson.databind.node.ArrayNode)altered.path("upgrades")).add(first.deepCopy());Files.writeString(copy.resolve(ledger),altered.toString());}
      assertThrows(IllegalArgumentException.class,()->ModelFixtureInputIntegrity.require(copy,artifact),mutation);
    }
  }
}
