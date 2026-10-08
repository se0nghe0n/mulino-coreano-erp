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
    public static void write(Path path, Object value) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        Files.writeString(path, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value) + "\n");
    }
    public static String required(JsonNode node, String name) {
        JsonNode value = node.get(name);
        if (value == null || !value.isTextual() || value.asText().isBlank()) throw new IllegalArgumentException("Missing string: " + name);
        return value.asText();
    }
    public static String sha256(Path file) throws IOException {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    public static String sha256Text(String text) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private Json() {}
}
