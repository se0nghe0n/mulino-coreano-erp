package com.mulino.domain.identity;

import java.nio.file.*;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
  @Bean
  JwtDecoder jwtDecoder(Environment e) throws Exception {
    if (!Arrays.asList(e.getActiveProfiles()).contains("local"))
      throw new IllegalStateException("Operational identity binding is required");
    String issuer = e.getRequiredProperty("JWT_ISSUER"),
        audience = e.getRequiredProperty("JWT_AUDIENCE");
    String pem =
        Files.readString(Path.of(e.getRequiredProperty("JWT_PUBLIC_KEY")))
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", "");
    RSAPublicKey key =
        (RSAPublicKey)
            KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(pem)));
    NimbusJwtDecoder d = NimbusJwtDecoder.withPublicKey(key).build();
    d.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuer),
            jwt -> {
              boolean ok =
                  jwt.getAudience().contains(audience)
                      && jwt.getSubject() != null
                      && jwt.getExpiresAt() != null
                      && nonempty(jwt, "organizationId")
                      && nonempty(jwt, "stableRequestOwner");
              return ok
                  ? OAuth2TokenValidatorResult.success()
                  : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
            }));
    return d;
  }

  private static boolean nonempty(Jwt j, String c) {
    return j.getClaimAsString(c) != null && !j.getClaimAsString(c).isBlank();
  }

  @Bean
  SecurityFilterChain security(HttpSecurity h) throws Exception {
    return h.csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a -> a.anyRequest().authenticated())
        .oauth2ResourceServer(o -> o.jwt(j -> {}))
        .build();
  }
}
