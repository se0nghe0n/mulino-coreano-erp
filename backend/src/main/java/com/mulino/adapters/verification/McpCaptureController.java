package com.mulino.adapters.verification;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Conditional(McpCaptureConfiguration.Enabled.class)
public class McpCaptureController {
  private final McpCaptureStore store;
  private final McpIdentitySnapshot snapshots;
  McpCaptureController(McpCaptureStore store,McpIdentitySnapshot snapshots) { this.store=store;this.snapshots=snapshots; }
  @GetMapping("/internal/verification/mcp-captures/{id}")
  public ResponseEntity<?> receipt(@PathVariable String id,HttpServletRequest request) {
    if(!McpCaptureFilter.loopback(request))return unavailable();
    try {
      UUID.fromString(id);
      @SuppressWarnings("unchecked") var owner=(Map<String,Object>)snapshots.capture().get("identity");
      return store.take(id,owner).<ResponseEntity<?>>map(value->ResponseEntity.ok().header("Cache-Control","no-store").body(value)).orElseGet(this::unavailable);
    } catch(RuntimeException denied) { return unavailable(); }
  }
  private ResponseEntity<?> unavailable() { return ResponseEntity.status(404).header("Cache-Control","no-store").body(Map.of("error","Capture unavailable")); }
}
