package org.mulino.verification.actual;

import java.nio.file.*;
import java.security.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mulino.verification.*;
import static org.junit.jupiter.api.Assertions.*;

/** Adapter plumbing tests; no product runtime/acceptance coverage is claimed. */
public final class ActualAdapterContractTest {
    @Test void defaultAndRedNeverEnableActualRuntime() {
        String prior=System.getProperty("verification.driver");
        try {
            System.clearProperty("verification.driver");assertInstanceOf(UnimplementedDriver.class,DriverFactory.create(Path.of("."),false));
            System.setProperty("verification.driver","actual");assertInstanceOf(UnimplementedDriver.class,DriverFactory.create(Path.of("."),true));
        }finally{if(prior==null)System.clearProperty("verification.driver");else System.setProperty("verification.driver",prior);}
    }
    @Test void disposableDatabaseAndLoopbackRequired() {
        assertThrows(IllegalArgumentException.class,()->ActualConfiguration.environment(Map.of()));
        assertThrows(IllegalArgumentException.class,()->new ActualConfiguration(java.net.URI.create("http://example.org"),"jdbc:postgresql://localhost/test","u","p",Path.of("key"),"i","a","v"));
    }
    @Test void signedJwtVerifiesAndContainsNoFixtureAuthority() throws Exception {
        var pair=KeyPairGenerator.getInstance("RSA");pair.initialize(2048);var keys=pair.generateKeyPair();
        Path pem=Files.createTempFile("actual-jwt-test-", ".pem");
        try{
            Files.writeString(pem,"-----BEGIN PRIVATE KEY-----\n"+Base64.getEncoder().encodeToString(keys.getPrivate().getEncoded())+"\n-----END PRIVATE KEY-----\n");
            var config=new ActualConfiguration(java.net.URI.create("http://127.0.0.1:8080"),"jdbc:postgresql://localhost/test","u","p",pem,"issuer","aud","commit");
            var actor=Json.object();actor.put("subject","reader").put("organizationAlias","ORG");actor.set("roleCapabilities",Json.parse("[\"approvePurchase\"]"));
            String[] parts=new JwtSigner(config).sign(actor).split("\\.");
            var signature=Signature.getInstance("SHA256withRSA");signature.initVerify(keys.getPublic());signature.update((parts[0]+"."+parts[1]).getBytes(java.nio.charset.StandardCharsets.US_ASCII));assertTrue(signature.verify(Base64.getUrlDecoder().decode(parts[2])));
            var claims=Json.parse(new String(Base64.getUrlDecoder().decode(parts[1]),java.nio.charset.StandardCharsets.UTF_8));
            assertEquals("reader",claims.path("sub").asText());assertFalse(claims.has("roleCapabilities"));assertFalse(claims.has("grant"));
        }finally{Files.deleteIfExists(pem);}
    }
    @Test void unknownSourceAndProjectionSnapshotCannotBecomeEmptyObservation() throws Exception {
        var c=new ActualConfiguration(java.net.URI.create("http://127.0.0.1:8080"),"jdbc:postgresql://localhost/test","u","p",Path.of("key"),"i","a","commit");
        assertThrows(UnsupportedOperationException.class,()->new JdbcObservation(c).capture(Json.parse("{\"snapshotRef\":\"projection-hash\",\"sources\":[\"segments\"]}")));
        assertThrows(UnsupportedOperationException.class,()->new JdbcObservation(c).capture(Json.parse("{\"sources\":[\"approvals\"]}")));
        assertThrows(UnsupportedOperationException.class,()->new FixtureInstaller(c).install(Json.parse("{\"fixture\":{\"synthetic\":true,\"aliases\":{\"w\":{\"type\":\"Work\"}}}}")));
    }
}
