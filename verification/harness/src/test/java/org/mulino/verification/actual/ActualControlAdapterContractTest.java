package org.mulino.verification.actual;

import java.net.URI;
import java.nio.file.*;
import java.security.KeyPairGenerator;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mulino.verification.*;
import static org.junit.jupiter.api.Assertions.*;

/** Adapter availability never substitutes for an executed control or its host evidence. */
class ActualControlAdapterContractTest {
    @TempDir Path dir;

    @Test void suiteAdvertisesControlButUnsupportedOperationsStillStayUnavailable() throws Exception {
        withSuite(true,driver->{
            assertTrue(driver.availableAdapters().containsAll(Set.of("control","clock","process","host")));
            var unknown=driver.control("unknown",Json.parse("{\"type\":\"process\",\"operation\":\"unsupported-operation\",\"parameters\":{}}"));
            assertEquals(StepResult.DriverStatus.NOT_IMPLEMENTED,unknown.driverStatus());
            var clock=driver.control("clock",Json.parse("{\"type\":\"clock\",\"operation\":\"advanceTo\",\"parameters\":{\"instant\":\"2026-10-07T03:00:00Z\"}}"));
            assertEquals(StepResult.DriverStatus.NOT_IMPLEMENTED,clock.driverStatus());
            assertTrue(clock.reason().contains("installed control identity absent"));
        });
    }

    @Test void ordinaryAdapterDoesNotAdvertiseSuiteControls() throws Exception {
        withSuite(false,driver->assertEquals(Set.of("api","fixture","db"),driver.availableAdapters()));
    }

    private void withSuite(boolean enabled,java.util.function.Consumer<ActualAcceptanceDriver> check) throws Exception {
        String prior=System.getProperty("verification.actual.suiteIsolation");
        var keys=KeyPairGenerator.getInstance("RSA");keys.initialize(2048);
        Path pem=dir.resolve("key.pem");
        Files.writeString(pem,"-----BEGIN PRIVATE KEY-----\n"+Base64.getEncoder().encodeToString(keys.generateKeyPair().getPrivate().getEncoded())+"\n-----END PRIVATE KEY-----\n");
        try {
            System.setProperty("verification.actual.suiteIsolation",Boolean.toString(enabled));
            var config=new ActualConfiguration(URI.create("http://127.0.0.1:1"),"jdbc:postgresql://localhost/unused","unused","unused",pem,"https://fixture.invalid","ontology","control-contract");
            check.accept(new ActualAcceptanceDriver(dir,config,Map.of()));
        } finally {
            if(prior==null)System.clearProperty("verification.actual.suiteIsolation");else System.setProperty("verification.actual.suiteIsolation",prior);
            Files.deleteIfExists(pem);
        }
    }
}
