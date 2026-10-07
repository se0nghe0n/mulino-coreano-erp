package com.mulino.adapters;

import com.mulino.application.PlatformCommands;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PlatformMcp {
  private static final String VERSION = "2026-07-28";
  private final PlatformCommands s;

  public PlatformMcp(PlatformCommands s) {
    this.s = s;
  }

  @PostMapping(value = "/mcp", produces = "application/json", consumes = "application/json")
  public ResponseEntity<Map<String, Object>> rpc(
      @RequestBody Map<String, Object> body, HttpServletRequest request) {
    Object id = body.get("id");
    String accept = request.getHeader("Accept");
    if (accept == null
        || !accept.contains("application/json")
        || !accept.contains("text/event-stream"))
      return error(id, 406, -32600, "Required Accept missing", Map.of());
    if (!"2.0".equals(body.get("jsonrpc"))
        || !body.containsKey("id")
        || !(body.get("method") instanceof String)
        || (id != null
            && !(id instanceof String)
            && !(id instanceof Integer)
            && !(id instanceof Long))
        || !(body.get("params") instanceof Map<?, ?>))
      return error(id, 400, -32600, "Invalid request", Map.of());
    String method = (String) body.get("method");
    Map<String, Object> p = (Map<String, Object>) body.get("params");
    if (!(p.get("_meta") instanceof Map<?, ?>))
      return error(id, 400, -32600, "Required metadata missing", Map.of());
    Map<String, Object> meta = (Map<String, Object>) p.get("_meta");
    String origin = request.getHeader("Origin");
    if (origin != null && !origin.equals("http://localhost:8080"))
      return error(id, 403, -32020, "Origin denied", Map.of());
    if (!Objects.equals(method, request.getHeader("Mcp-Method"))
        || request.getHeader("MCP-Protocol-Version") == null
        || !Objects.equals(
            request.getHeader("MCP-Protocol-Version"),
            meta.get("io.modelcontextprotocol/protocolVersion")))
      return error(id, 400, -32020, "HeaderMismatch", Map.of());
    if (!VERSION.equals(meta.get("io.modelcontextprotocol/protocolVersion")))
      return error(
          id,
          400,
          -32022,
          "UnsupportedProtocolVersion",
          Map.of(
              "supported",
              List.of(VERSION),
              "requested",
              String.valueOf(meta.get("io.modelcontextprotocol/protocolVersion"))));
    if (!(meta.get("io.modelcontextprotocol/clientCapabilities") instanceof Map<?, ?>))
      return error(id, 400, -32020, "HeaderMismatch", Map.of());
    String name = request.getHeader("Mcp-Name");
    if (name != null && name.startsWith("=?base64?") && name.endsWith("?=")) {
      try {
        name =
            new String(
                Base64.getDecoder().decode(name.substring(9, name.length() - 2)),
                StandardCharsets.UTF_8);
      } catch (Exception e) {
        return error(id, 400, -32020, "HeaderMismatch", Map.of());
      }
    }
    if ("tools/call".equals(method) && !Objects.equals(name, p.get("name")))
      return error(id, 400, -32020, "HeaderMismatch", Map.of());
    Map<String, Object> result;
    switch (String.valueOf(method)) {
      case "server/discover":
        result =
            Map.of(
                "supportedVersions",
                List.of(VERSION),
                "capabilities",
                Map.of("tools", Map.of()),
                "_meta",
                Map.of(
                    "io.modelcontextprotocol/serverInfo",
                    Map.of("name", "mulino-platform-s0", "version", "0.1.0")));
        break;
      case "tools/list":
        result =
            Map.of(
                "tools",
                List.of(tool("platform.readScope", false), tool("platform.reserve", true)));
        break;
      case "tools/call":
        try {
          Map<String, Object> a = (Map<String, Object>) p.getOrDefault("arguments", Map.of());
          Object value;
          if ("platform.readScope".equals(p.get("name"))) value = s.read((String) a.get("scopeId"));
          else if ("platform.reserve".equals(p.get("name")))
            value =
                s.reserve(
                    (String) a.get("scopeId"),
                    (String) a.get("quantity"),
                    ((Number) a.get("expectedRevision")).intValue(),
                    (String) a.get("idempotencyKey"));
          else return error(id, 404, -32602, "Unknown tool", Map.of());
          result =
              Map.of(
                  "isError",
                  false,
                  "structuredContent",
                  value,
                  "content",
                  List.of(Map.of("type", "text", "text", json(value))));
        } catch (Exception e) {
          String outcome =
              e instanceof org.springframework.security.access.AccessDeniedException
                  ? "DENIED"
                  : e instanceof PlatformCommands.Conflict ? e.getMessage() : "INVALID_COMMAND";
          result =
              Map.of(
                  "isError",
                  true,
                  "structuredContent",
                  Map.of("outcome", outcome),
                  "content",
                  List.of(Map.of("type", "text", "text", outcome)));
        }
        break;
      default:
        return error(id, 404, -32601, "Method not found", Map.of());
    }
    var complete = new LinkedHashMap<String, Object>(result);
    complete.put("resultType", "complete");
    var response = new LinkedHashMap<String, Object>();
    response.put("jsonrpc", "2.0");
    response.put("id", id);
    response.put("result", complete);
    return ResponseEntity.ok(response);
  }

  private static String json(Object value) {
    try {
      return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value);
    } catch (Exception failure) {
      throw new IllegalStateException(failure);
    }
  }

  private static Map<String, Object> tool(String name, boolean write) {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("scopeId", Map.of("type", "string", "format", "uuid"));
    if (write) {
      properties.put("quantity", Map.of("type", "string"));
      properties.put("expectedRevision", Map.of("type", "integer"));
      properties.put("idempotencyKey", Map.of("type", "string"));
    }
    return Map.of(
        "name",
        name,
        "description",
        "S0 isolated platform proof",
        "inputSchema",
        Map.of(
            "type",
            "object",
            "properties",
            properties,
            "required",
            new ArrayList<>(properties.keySet()),
            "additionalProperties",
            false));
  }

  private static ResponseEntity<Map<String, Object>> error(
      Object id, int status, int code, String message, Map<String, Object> data) {
    Map<String, Object> b = new LinkedHashMap<>();
    b.put("jsonrpc", "2.0");
    b.put("id", id);
    b.put("error", Map.of("code", code, "message", message, "data", data));
    return ResponseEntity.status(status).body(b);
  }
}
