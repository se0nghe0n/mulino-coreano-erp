package com.mulinocoreano.backend.scenario;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.test.context.DynamicPropertyRegistry;

/** Host-only, per-JVM keys: context restarts retain them; native agents never receive them. */
final class ScenarioSecrets {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String HUMAN = generate();
    private static final String SERVICE = generate();

    private ScenarioSecrets() {}
    private static String generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    static String human() { return HUMAN; }
    static String service() { return SERVICE; }
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("mulino.local-auth.human-secret", ScenarioSecrets::human);
        registry.add("mulino.local-auth.service-secret", ScenarioSecrets::service);
    }
}
