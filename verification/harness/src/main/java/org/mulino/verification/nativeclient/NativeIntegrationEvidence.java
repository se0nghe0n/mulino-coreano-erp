package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.mulino.verification.Json;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.UUID;

/** Captures evidence before returning it; secrets are refused, never written and then redacted. */
final class NativeIntegrationEvidence {
    final Path root;
    private final Path directory;
    NativeIntegrationEvidence(Path root) {
        this.root=root.toAbsolutePath().normalize();
        directory=this.root.resolve("verification/harness/target/native-integration/"+UUID.randomUUID());
    }
    ObjectNode save(String kind,JsonNode value) throws IOException {
        NativeClientPolicy.rejectSecrets(value);
        Files.createDirectories(directory);
        Path file=directory.resolve(kind+"-"+UUID.randomUUID()+".json");
        NativeCodexClient.writeCapture(file,value);
        return descriptor(file);
    }
    ObjectNode descriptor(Path file) throws IOException {
        Path path=file.toRealPath();
        NativeClientPolicy.require(path.startsWith(root.toRealPath()),"EVIDENCE_PATH_ESCAPE");
        return Json.object().put("path",root.relativize(path).toString()).put("sha256",Json.sha256(path)).put("sizeBytes",Files.size(path));
    }
    ObjectNode source(String relative) throws IOException { return descriptor(root.resolve(relative)); }
    ObjectNode base(String kind,JsonNode scope) {
        var result=Json.object().put("evidenceClass",kind).put("capturedAt",Instant.now().toString());
        result.set("scope",scope.deepCopy());return result;
    }
}
