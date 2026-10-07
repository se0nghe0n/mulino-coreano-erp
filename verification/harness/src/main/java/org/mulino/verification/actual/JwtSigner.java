package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import org.mulino.verification.Json;

/** RS256 real transport credentials, short wall-clock validity, fixture subject preserved. */
public final class JwtSigner {
    private final ActualConfiguration configuration;
    private final PrivateKey key;
    public JwtSigner(ActualConfiguration configuration) {
        this.configuration=configuration;
        try {
            String pem=Files.readString(configuration.signingKey()).replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");
            key=KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)));
        } catch(Exception failure) { throw new IllegalArgumentException("Cannot load local PKCS8 RSA signing key",failure); }
    }
    public String sign(JsonNode actor) {
        try {
            Instant now=Instant.now(); var claims=Json.object();
            claims.put("iss",configuration.issuer()).put("aud",configuration.audience()).put("sub",Json.required(actor,"subject"));
            claims.put("organizationId",Json.required(actor,"organizationAlias"));
            // This claim satisfies the S0 decoder shape; current server identity ignores it for ownership.
            claims.put("stableRequestOwner","UNTRUSTED-CLIENT-CLAIM").put("iat",now.getEpochSecond()).put("exp",now.plusSeconds(120).getEpochSecond());
            String input=encoded("{\"alg\":\"RS256\",\"typ\":\"JWT\"}")+"."+encoded(claims.toString());
            Signature signer=Signature.getInstance("SHA256withRSA");signer.initSign(key);signer.update(input.getBytes(StandardCharsets.US_ASCII));
            return input+"."+Base64.getUrlEncoder().withoutPadding().encodeToString(signer.sign());
        } catch(GeneralSecurityException failure) { throw new IllegalStateException("JWT signing failed",failure); }
    }
    private static String encoded(String value) {return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));}
}
