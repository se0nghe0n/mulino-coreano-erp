package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import org.mulino.verification.Json;
import java.nio.file.*;
import java.util.*;

/** Original model data is exact; two explicitly versioned consumer schemas retain exact prior bytes. */
final class ModelFixtureInputIntegrity {
  private static final Set<String> UPGRADED=Set.of("verification/model-binding/physical-columns.schema.json",
      "verification/model-binding/projection-revision-observation.schema.json");
  static void require(Path root,JsonNode artifact)throws Exception {
    String path=Json.required(artifact,"path"),original=Json.required(artifact,"sha256");
    if(!UPGRADED.contains(path)) {
      if(!original.equals(Json.sha256(root.resolve(path))))throw new IllegalArgumentException("Original model input drift: "+path);
      return;
    }
    var manifest=Json.read(root.resolve("verification/model-installation/v1/schema-upgrades/manifest.json"));
    if(!"MODEL_CONSUMER_SCHEMA_UPGRADE_V1".equals(manifest.path("version").asText())
      ||manifest.path("upgrades").size()!=UPGRADED.size())throw new IllegalArgumentException("Invalid consumer schema version ledger");
    var entries=new HashMap<String,JsonNode>();for(var entry:manifest.path("upgrades"))
      if(!UPGRADED.contains(entry.path("path").asText())||entries.put(entry.path("path").asText(),entry)!=null)throw new IllegalArgumentException("Unknown consumer schema upgrade");
    var entry=entries.get(path);String snapshot="verification/model-installation/v1/schema-upgrades/"+Path.of(path).getFileName()+".before.json";
    if(entry==null||!original.equals(entry.path("beforeSha256").asText())
      ||!snapshot.equals(entry.path("beforeSnapshot").asText())
      ||!root.resolve(snapshot).toRealPath().startsWith(root.toRealPath())
      ||!original.equals(Json.sha256(root.resolve(snapshot)))
      ||!entry.path("afterSha256").asText().equals(Json.sha256(root.resolve(path))))throw new IllegalArgumentException("Unrecorded model consumer schema drift: "+path);
  }
}
