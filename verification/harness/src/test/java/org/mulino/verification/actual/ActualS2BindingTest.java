package org.mulino.verification.actual;
import java.nio.file.*;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.mulino.verification.Json;
import static org.junit.jupiter.api.Assertions.*;
final class ActualS2BindingTest {
    @Test void nativeFixtureIsCompleteAuthoredInputAndSchemaValid() throws Exception {
        Path root=Path.of(System.getProperty("repo.root",".")).toAbsolutePath();
        var fixture=new org.mulino.verification.ContractValidator(root).fixture("verification/actual/s2/fixture.json");
        var definition=fixture.path("aliases").path("DEF").path("content");
        assertEquals("core-v1",definition.path("evaluatorVersion").asText());
        assertEquals("PUBLISHED",definition.path("state").asText());
        assertEquals("2026-10-07T09:00:00Z",fixture.path("clock").path("asOf").asText());
        assertFalse(definition.path("goals").isEmpty());
        for(var capability:definition.path("capabilities"))assertEquals("1.0.0",capability.path("semanticVersion").asText());
        assertFalse(fixture.has("expected"));assertFalse(fixture.has("assertions"));
    }

    @Test void explicitBindingPreservesAuthoredClockScopesAndHash() throws Exception {
        String prior=System.getProperty("verification.actual.identityBinding");
        try {
            Path root=Path.of(System.getProperty("repo.root",".")).toAbsolutePath();
            System.setProperty("verification.actual.identityBinding","verification/actual/s2/identity-binding.json");
            var bundle=Json.object();bundle.set("fixture",Json.read(root.resolve("verification/fixtures/base/identity-clock.json")));bundle.put("fixtureHash","authored-hash");
            var config=new ActualConfiguration(URI.create("http://127.0.0.1:8080"),"jdbc:postgresql://localhost/test","u","p",Path.of("unused"),"https://mulino-native.invalid","isolated-ontology","source");
            var bound=ActualFixtureBindings.bind(root,bundle,config);
            assertEquals(bundle.path("fixture").path("clock"),bound.path("fixture").path("clock"));
            assertEquals(bundle.path("fixture").path("actors").path("qc").path("grant"),bound.path("fixture").path("actors").path("qc").path("grant"));
            assertEquals("fixture-issuer",bundle.path("fixture").path("actors").path("qc").path("issuer").asText());
            assertEquals(config.issuer(),bound.path("fixture").path("actors").path("qc").path("issuer").asText());assertEquals("authored-hash",bound.path("fixtureHash").asText());
            assertEquals(64,bound.path("identityBindingHash").asText().length());
            ((com.fasterxml.jackson.databind.node.ObjectNode)bundle.path("fixture").path("actors").path("qc")).put("issuer","unknown-issuer");
            assertThrows(UnsupportedOperationException.class,()->ActualFixtureBindings.bind(root,bundle,config));
        } finally {if(prior==null)System.clearProperty("verification.actual.identityBinding");else System.setProperty("verification.actual.identityBinding",prior);}
    }
}
