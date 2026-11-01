package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.mulino.verification.Json;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

/** M47's repeated owner snapshot is linked only to the exact authored owner source. */
class ModelFixtureOwnerClaimTest {
    private final Path root=Path.of(System.getProperty("repo.root")).toAbsolutePath();

    @Test void m47OwnerSnapshotIsAnExactClaimBindingAndLeavesOtherMissingFactsBlocked() throws Exception {
        Path fixtureFile=root.resolve("verification/model-binding/cases/M47/fixture.json");
        JsonNode fixture=Json.read(fixtureFile);String hash=Json.sha256(fixtureFile);
        JsonNode inputs=archiveInput("verification/model-installation/v1/inputs/M47-isolated.json");
        String before=inputs.toString();

        JsonNode inventory=ModelFixtureInstaller.inventory(fixture,inputs,hash);

        assertFalse(inventory.path("installable").asBoolean(),"Sparse queued command/allocation facts remain fail-closed");
        assertTrue(inventory.path("issues").toString().contains("queuedCommand"));
        assertTrue(inventory.path("issues").toString().contains("REQUIRED_TYPED_FIELD:Allocation"));
        assertFalse(hasIssue(inventory,"/baseline/domainFacts/currentOwnerRef"));
        JsonNode claim=claim(inventory,"/baseline/domainFacts/currentOwnerRef");
        assertEquals("salesOwner",claim.path("originalClaim").asText());
        assertEquals("EXACT_DUPLICATE_OWNER_SNAPSHOT",claim.path("relation").asText());
        assertEquals("Work",claim.path("linkedFactType").asText());
        assertEquals("S1:deliveryWork",claim.path("linkedFactAlias").asText());
        assertEquals("ownerId",claim.path("linkedField").asText());
        assertEquals("/baseline/domainFacts/objects/S1/ownerRef",claim.path("linkedFactSourcePointer").asText());
        assertEquals("/baseline/domainFacts/obligations/0/ownerRef",claim.path("obligationSourcePointer").asText());
        assertFalse(claim.path("additionalProductFactClaimed").asBoolean());
        assertTrue(inventory.path("unverifiedOriginalClaims").toString().contains("DUPLICATE_OWNER_SNAPSHOT_BOUND_TO_EXPLICIT_S1_OWNER_NO_ADDITIONAL_ROW"));
        assertFalse(inventory.path("mapping").toString().contains("currentOwnerRef"),"No new row is created for a duplicate claim");
        assertEquals(before,inputs.toString(),"Inventory must not mutate the supplied installation input");
        assertEquals(hash,Json.sha256(fixtureFile),"Original fixture bytes remain unchanged");
    }

    @Test void m47OwnerSnapshotMismatchDoesNotBindToTheTypedWorkOwner() throws Exception {
        Path fixtureFile=root.resolve("verification/model-binding/cases/M47/fixture.json");
        ObjectNode fixture=(ObjectNode)Json.read(fixtureFile).deepCopy();
        ((ObjectNode)fixture.path("baseline").path("domainFacts")).put("currentOwnerRef","procurementOwner");
        JsonNode inputs=archiveInput("verification/model-installation/v1/inputs/M47-isolated.json");
        String changedHash=sha256(fixture.toString().getBytes(StandardCharsets.UTF_8));
        ((ObjectNode)inputs).put("fixtureSha256",changedHash);

        JsonNode inventory=ModelFixtureInstaller.inventory(fixture,inputs,changedHash);

        assertTrue(hasIssue(inventory,"/baseline/domainFacts/currentOwnerRef"));
        assertTrue(inventory.path("issues").toString().contains("CURRENT_OWNER_DIFFERS_FROM_EXPLICIT_S1_OWNER"));
        assertNull(claimOrNull(inventory,"/baseline/domainFacts/currentOwnerRef"));
    }

    @Test void m47OwnerMismatchReasonsCoverEveryExplicitSourceDirection() {
        assertEquals("CURRENT_OWNER_DIFFERS_FROM_EXPLICIT_S1_OWNER",
            ModelFixturePlan.m47CurrentOwnerMismatchReason("procurementOwner","salesOwner","procurement-id","procurement-id","salesOwner"));
        assertEquals("CURRENT_OWNER_DIFFERS_FROM_TYPED_WORK_OWNER",
            ModelFixturePlan.m47CurrentOwnerMismatchReason("salesOwner","salesOwner","sales-id","procurement-id","salesOwner"));
        assertEquals("CURRENT_OWNER_DIFFERS_FROM_EXPLICIT_OPEN_OBLIGATION_OWNER",
            ModelFixturePlan.m47CurrentOwnerMismatchReason("salesOwner","salesOwner","sales-id","sales-id","procurementOwner"));
        assertNull(ModelFixturePlan.m47CurrentOwnerMismatchReason("salesOwner","salesOwner","sales-id","sales-id","salesOwner"));
    }

    @Test void m47OwnerSnapshotAndSalesOrderOwnerCannotOverrideOpenObligationOwner() throws Exception {
        Path fixtureFile=root.resolve("verification/model-binding/cases/M47/fixture.json");
        ObjectNode fixture=(ObjectNode)Json.read(fixtureFile).deepCopy();
        ((ObjectNode)fixture.path("baseline").path("domainFacts")).put("currentOwnerRef","procurementOwner");
        ((ObjectNode)fixture.at("/baseline/domainFacts/objects/S1")).put("ownerRef","procurementOwner");
        JsonNode inputs=archiveInput("verification/model-installation/v1/inputs/M47-isolated.json");
        for(JsonNode record:inputs.path("records"))if(record.path("type").asText().equals("Work")&&record.path("alias").asText().equals("S1:deliveryWork"))
            ((ObjectNode)record.path("fields")).put("ownerId","procurementOwner");
        String changedHash=sha256(fixture.toString().getBytes(StandardCharsets.UTF_8));
        ((ObjectNode)inputs).put("fixtureSha256",changedHash);

        JsonNode inventory=ModelFixtureInstaller.inventory(fixture,inputs,changedHash);

        assertTrue(hasIssue(inventory,"/baseline/domainFacts/currentOwnerRef"));
        assertTrue(inventory.path("issues").toString().contains("CURRENT_OWNER_DIFFERS_FROM_EXPLICIT_OPEN_OBLIGATION_OWNER"));
        assertNull(claimOrNull(inventory,"/baseline/domainFacts/currentOwnerRef"));
    }

    private JsonNode archiveInput(String wanted) throws Exception {
        Path archive=root.resolve("verification/model-installation/v1/installation-inputs.tar.gz");
        try(var in=new DataInputStream(new GZIPInputStream(Files.newInputStream(archive)))) {
            byte[] header=new byte[512];
            while(true) {
                int first=in.read();if(first<0)break;header[0]=(byte)first;
                try {in.readFully(header,1,511);} catch(EOFException end){break;}
                boolean empty=true;for(byte b:header)if(b!=0){empty=false;break;}if(empty)break;
                String name=field(header,0,100),prefix=field(header,345,155);if(!prefix.isEmpty())name=prefix+"/"+name;
                long size=Long.parseLong(field(header,124,12).trim(),8);if(size<0||size>16*1024*1024)throw new IOException("Unexpected archive member size");
                byte[] content=in.readNBytes((int)size);if(content.length!=size)throw new EOFException("Truncated tar member: "+name);
                long padding=(512-size%512)%512;if(padding>0)in.skipNBytes(padding);
                if(name.equals(wanted))return Json.MAPPER.readTree(content);
            }
        }
        throw new FileNotFoundException("Archive member not found: "+wanted);
    }

    private String field(byte[] header,int start,int length) {
        int end=start;while(end<start+length&&header[end]!=0)end++;
        return new String(header,start,end-start,StandardCharsets.UTF_8);
    }
    private String sha256(byte[] bytes) throws Exception {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    private boolean hasIssue(JsonNode inventory,String pointer) {for(JsonNode issue:inventory.path("issues"))if(issue.path("pointer").asText().equals(pointer))return true;return false;}
    private JsonNode claim(JsonNode inventory,String pointer) {JsonNode found=claimOrNull(inventory,pointer);assertNotNull(found);return found;}
    private JsonNode claimOrNull(JsonNode inventory,String pointer) {for(JsonNode claim:inventory.path("preservedOriginalClaims"))if(claim.path("sourcePointer").asText().equals(pointer))return claim;return null;}
}
