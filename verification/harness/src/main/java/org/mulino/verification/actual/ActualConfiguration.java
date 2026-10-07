package org.mulino.verification.actual;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

/** Credentials are inputs only and are never included in receipts. */
public record ActualConfiguration(URI baseUri, String jdbcUrl, String username, String password,
                                  Path signingKey, String issuer, String audience, String buildVersion) {
    public ActualConfiguration {
        if (!"http".equals(baseUri.getScheme()) || !("127.0.0.1".equals(baseUri.getHost()) || "localhost".equals(baseUri.getHost())))
            throw new IllegalArgumentException("Actual acceptance requires loopback HTTP backend");
        if (!jdbcUrl.startsWith("jdbc:postgresql://")) throw new IllegalArgumentException("PostgreSQL JDBC URL required");
    }
    public static ActualConfiguration environment(Map<String,String> env) {
        if (!"true".equals(env.get("ACTUAL_DISPOSABLE_DATABASE")))
            throw new IllegalArgumentException("ACTUAL_DISPOSABLE_DATABASE=true required for fixture installation");
        return new ActualConfiguration(URI.create(required(env,"ACTUAL_BASE_URL")), required(env,"DB_URL"),
            required(env,"DB_USERNAME"), required(env,"DB_PASSWORD"), Path.of(required(env,"JWT_PRIVATE_KEY")),
            required(env,"JWT_ISSUER"), required(env,"JWT_AUDIENCE"), required(env,"ACTUAL_BUILD_COMMIT"));
    }
    private static String required(Map<String,String> env,String name) {
        String value=env.get(name); if(value==null || value.isBlank()) throw new IllegalArgumentException("Missing environment variable "+name); return value;
    }
}
