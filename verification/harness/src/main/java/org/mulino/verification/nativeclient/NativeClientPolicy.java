package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.mulino.verification.Json;
import org.mulino.verification.modelbinding.ModelBindingRunner;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Checked-in user decision is pinned, not an arbitrary runtime approval JSON. */
public final class NativeClientPolicy {
    public static final String APPROVAL = "verification/native-client/subscription-approval-fast-20261023.json";
    public static final String APPROVAL_HASH = "6508a61c9772bf0c48d54dff14ac0ff606484c3aa4115fa9fb3985b24b36dec1";
    public static final String MODEL = "gpt-6.1-sol", TIER = "priority";
    public static final String VERSION = "codex-cli 0.163.0";
    public static final List<String> PACKAGES = List.of("ontology-work-coordinator", "ontology-procurement-transport",
        "ontology-import-qc", "ontology-sales-returns-recall", "ontology-settlement", "ontology-definition-authoring");
    final Path root;
    public NativeClientPolicy(Path root) { this.root = root.toAbsolutePath().normalize(); }
    public ObjectNode subscriptionPolicy() throws IOException {
        require(Json.sha256(root.resolve(APPROVAL)).equals(APPROVAL_HASH), "R8_APPROVAL_CHANGED");
        ObjectNode a = (ObjectNode) Json.read(root.resolve(APPROVAL));
        return a;
    }
    public ObjectNode approval() throws IOException {
        ObjectNode a=subscriptionPolicy();
        a.put("status", "AUTHORIZED").put("approvalArtifactRef", APPROVAL);
        a.put("promptSha256", Json.sha256(root.resolve("verification/native-client/prompt.txt")));
        a.put("skillSha256", skillHash());
        return a;
    }
    public String skillHash() throws IOException {
        var hashes = Json.object();
        for (Path p : skillFiles()) hashes.put(root.relativize(p).toString(), Json.sha256(p));
        return sha(hashes.toString());
    }
    public List<Path> skillFiles() throws IOException {
        List<Path> files = new ArrayList<>();
        for (String name : PACKAGES) {
            Path base = root.resolve("agents/skills/" + name);
            require(Files.isRegularFile(base.resolve("SKILL.md")), "RUNTIME_SKILL_MISSING");
            require(base.toRealPath().startsWith(root.toRealPath()), "SKILL_ROOT_ESCAPE");
            try (var paths = Files.walk(base)) {
                for (Path p : paths.filter(Files::isRegularFile).sorted().toList()) {
                    require(p.toRealPath().startsWith(base.toRealPath()), "SKILL_PATH_ESCAPE");
                    files.add(p);
                }
            }
        }
        return List.copyOf(files);
    }
    /** Only caller-supplied permitted data crosses the model input boundary. */
    public static ObjectNode input(String utterance, JsonNode context) {
        require(utterance != null && !utterance.isBlank() && context != null && context.isObject(), "INPUT_MISSING");
        ModelBindingRunner.checkContext(context);
        rejectSecrets(context);
        var n = Json.object();n.put("utterance", utterance);n.set("permittedContext", context.deepCopy());
        rejectSecrets(n);return n;
    }
    static void rejectSecrets(JsonNode node) {
        if (node.isObject()) node.fields().forEachRemaining(e -> {
            String k = e.getKey().toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
            require(!Set.of("authorization", "apikey", "accesstoken", "refreshtoken", "idtoken", "password", "secret", "cookie").contains(k), "SECRET_FIELD_REJECTED");
            rejectSecrets(e.getValue());
        });
        else if (node.isArray()) node.forEach(NativeClientPolicy::rejectSecrets);
        else if (node.isTextual()) require(!node.asText().matches("(?s).*(?:Bearer\\s+[A-Za-z0-9._~-]{8,}|sk-[A-Za-z0-9_-]{12,}|-----BEGIN [A-Z ]*PRIVATE KEY-----|eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+).*"), "SECRET_VALUE_REJECTED");
    }
    /** Environment allowlist avoids API keys, provider endpoints, proxies and inherited client settings. */
    static Map<String,String> environment(Map<String,String> inherited, Path isolatedHome) {
        Map<String,String> env = new HashMap<>();
        for (String k : List.of("PATH", "HOME", "TMPDIR", "LANG", "LC_ALL", "SYSTEMROOT"))
            if (inherited.containsKey(k)) env.put(k, inherited.get(k));
        env.put("CODEX_HOME", isolatedHome.toString());return env;
    }
    static void require(boolean b, String code) { if (!b) throw new IllegalStateException(code); }
    static String sha(String value) {
        try { return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
