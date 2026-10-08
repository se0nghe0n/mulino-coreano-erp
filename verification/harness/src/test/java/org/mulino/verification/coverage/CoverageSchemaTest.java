package org.mulino.verification.coverage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.mulino.verification.ContractValidator;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/** SELFTEST: generated NOT_RUN manifests and counterexamples, never a product execution. */
class CoverageSchemaTest {
    private final Path root=Path.of(System.getProperty("repo.root"));
    private final ObjectMapper mapper=new ObjectMapper();

    private JsonNode assemble() throws Exception {
        Process process=new ProcessBuilder("python3","verification/coverage/assemble.py").directory(root.toFile()).redirectErrorStream(true).start();
        String output=new String(process.getInputStream().readAllBytes());
        int exit=process.waitFor();
        // Without runtime evidence the gate is NOT_RUN(2), or FAIL(1) when preparation observed a
        // structural defect such as an unreachable required profile. It is never PASS.
        assertTrue(exit==1||exit==2,output);
        JsonNode value=mapper.readTree(root.resolve("verification/harness/target/evidence/runtime-manifest.json").toFile());
        assertEquals(exit,value.path("exitCode").asInt(),output);
        assertNotEquals("PASS",value.path("status").asText());
        return value;
    }
    @Test void currentNotRunAssemblySatisfiesNewAndBaseManifestSchemas() throws Exception {
        JsonNode value=assemble();ContractValidator validator=new ContractValidator(root);
        validator.schema("verification/coverage/runtime-manifest.schema.json",value);
        validator.schema("verification/manifest.schema.json",value);
        validator.schema("verification/coverage/runtime-evidence-index.schema.json",mapper.readTree(root.resolve("verification/coverage/runtime-evidence-index.json").toFile()));
        assertEquals(499,value.path("namedObservations").size());
        assertFalse(value.path("gateComplete").asBoolean());
        assertTrue(value.path("model").path("usage").isNull());
    }
    @Test void completionWithNotRunAndMissingActualModelUsageAreRejected() throws Exception {
        ObjectNode value=(ObjectNode)assemble();ContractValidator validator=new ContractValidator(root);
        value.put("gateComplete",true);
        assertThrows(IllegalArgumentException.class,()->validator.schema("verification/coverage/runtime-manifest.schema.json",value));
        value.put("status","PASS").put("exitCode",0).put("runtimeComplete",true).put("productRuntimeClaimed",true).put("preparationStatus","PREPARED").put("runtimeStatus","PASS");
        ((ObjectNode)value.path("model")).put("status","PASS");
        assertThrows(IllegalArgumentException.class,()->validator.schema("verification/coverage/runtime-manifest.schema.json",value));
    }
    @Test void actualRuntimeIndexNeedsCommandReceiptAndRejectsWaivedProfile() throws Exception {
        ContractValidator validator=new ContractValidator(root);
        JsonNode absent=mapper.readTree("{\"schemaVersion\":\"1.0.0\",\"profiles\":[{\"profile\":\"model\",\"reportRef\":\"report.json\",\"evidenceClass\":\"ACTUAL\"}]}");
        assertThrows(IllegalArgumentException.class,()->validator.schema("verification/coverage/runtime-evidence-index.schema.json",absent));
        JsonNode waiver=mapper.readTree("{\"schemaVersion\":\"1.0.0\",\"profiles\":[{\"profile\":\"btp-deployment\",\"reportRef\":\"report.json\",\"evidenceClass\":\"WAIVER\"}]}");
        assertThrows(IllegalArgumentException.class,()->validator.schema("verification/coverage/runtime-evidence-index.schema.json",waiver));
    }
}
