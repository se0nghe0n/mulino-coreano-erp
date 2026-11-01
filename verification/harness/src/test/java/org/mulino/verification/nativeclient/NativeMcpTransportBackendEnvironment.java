package org.mulino.verification.nativeclient;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Allowlisted child-process environment for the packaged test backend. */
final class NativeMcpTransportBackendEnvironment {
  private static final List<String> SAFE_BASE_KEYS = List.of(
      "PATH", "LANG", "LC_ALL", "TZ");

  private NativeMcpTransportBackendEnvironment() {}

  static Map<String, String> create(Map<String, String> inherited, boolean verificationEnabled,
                                    String databaseUrl, String databaseUser, String databasePassword,
                                    Path publicKey, String issuer, String audience,
                                    Instant fixedInstant, Path blobRoot) {
    Map<String, String> environment = new HashMap<>();
    for (String key : SAFE_BASE_KEYS) {
      String value = inherited.get(key);
      if (value != null) environment.put(key, value);
    }
    String ownedTemporaryDirectory = publicKey.toAbsolutePath().normalize().getParent().toString();
    environment.put("HOME", ownedTemporaryDirectory);
    environment.put("TMPDIR", ownedTemporaryDirectory);
    environment.put("TMP", ownedTemporaryDirectory);
    environment.put("TEMP", ownedTemporaryDirectory);
    environment.put("SPRING_PROFILES_ACTIVE", verificationEnabled ? "local,verification" : "local");
    environment.put("DB_URL", databaseUrl);
    environment.put("DB_USERNAME", databaseUser);
    environment.put("DB_PASSWORD", databasePassword);
    environment.put("JWT_PUBLIC_KEY", publicKey.toString());
    environment.put("JWT_ISSUER", issuer);
    environment.put("JWT_AUDIENCE", audience);
    environment.put("MULINO_VERIFICATION_MCP_CAPTURE_ENABLED", "true");
    environment.put("MULINO_VERIFICATION_INSTANT", fixedInstant.toString());
    environment.put("MULINO_EVIDENCE_BLOB_ROOT", blobRoot.toString());
    return Map.copyOf(environment);
  }
}
