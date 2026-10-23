package org.mulino.verification.nativeclient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.mulino.verification.Json;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
/** Reads only this run's explicitly configured immutable blob root. SQL hashes bind retained bytes to actual documents. */
final class NativeDocumentWitnesses {
    static ArrayNode capture(JsonNode raw) throws Exception {
        var out=Json.array();String configured=System.getProperty("verification.actual.blobRoot");Path root=configured==null?null:Path.of(configured).toAbsolutePath().normalize().resolve("objects");
        for(JsonNode doc:raw.path("mulino_evidence_DocumentVersions")){
            var proof=Json.object().put("documentId",Json.required(doc,"ID")).put("status","UNOBSERVED");out.add(proof);
            if(root==null||!doc.hasNonNull("blobId")||!doc.path("availability").asText().equals("AVAILABLE"))continue;
            String id=Json.required(doc,"blobId");UUID.fromString(id);Path file=root.resolve(id);if(!Files.isRegularFile(file)||Files.size(file)>4*1024*1024)continue;
            NativeClientPolicy.require(file.toRealPath().startsWith(root.toRealPath()),"SOURCE_DOCUMENT_PATH_ESCAPE");byte[] bytes=Files.readAllBytes(file);String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            if(!hash.equals(doc.path("sha256").asText())||bytes.length!=doc.path("byteLength").asLong(-1)){proof.put("status","CHANGED");continue;}
            proof.put("status","CAPTURED").put("sha256",hash).put("sizeBytes",bytes.length).put("bytesBase64",Base64.getEncoder().encodeToString(bytes));
        }return out;
    }
    private NativeDocumentWitnesses() {}
}
