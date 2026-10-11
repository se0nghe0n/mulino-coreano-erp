package org.mulino.verification;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.io.IOException;
import java.nio.file.*;
import java.security.*;

public final class Json {
    public static final ObjectMapper MAPPER = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
    public static JsonNode read(Path path) throws IOException { return MAPPER.readTree(Files.readString(path)); }
    public static JsonNode parse(String text) { try { return MAPPER.readTree(text); } catch (IOException e) { throw new IllegalArgumentException(e); } }
    public static ObjectNode object() { return MAPPER.createObjectNode(); }
    public static ArrayNode array() { return MAPPER.createArrayNode(); }
    /**
     * Streams the pretty-printed document (S3-11): a full scenarios report is hundreds of MB, and building it as one
     * String first exhausted the default heap. The bytes are identical to the former writeValueAsString + newline.
     */
    public static void write(Path path, Object value) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        try (var out = new java.io.BufferedOutputStream(Files.newOutputStream(path))) { print(out, value); }
    }
    /** Streams the pretty-printed document plus a newline to an open stream without closing it. */
    public static void print(java.io.OutputStream out, Object value) throws IOException {
        MAPPER.writerWithDefaultPrettyPrinter().without(com.fasterxml.jackson.core.JsonGenerator.Feature.AUTO_CLOSE_TARGET).writeValue(out, value);
        out.write('\n'); out.flush();
    }
    public static String required(JsonNode node, String name) {
        JsonNode value = node.get(name);
        if (value == null || !value.isTextual() || value.asText().isBlank()) throw new IllegalArgumentException("Missing string: " + name);
        return value.asText();
    }
    public static String sha256(Path file) throws IOException {
        try (var in = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256"); byte[] buffer = new byte[1 << 16];
            for (int n; (n = in.read(buffer)) > 0; ) digest.update(buffer, 0, n);
            return java.util.HexFormat.of().formatHex(digest.digest());
        }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    public static String sha256Text(String text) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private Json() {}
}
