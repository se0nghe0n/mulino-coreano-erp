package com.mulinocoreano.backend.recall;

import com.mulinocoreano.backend.procurement.PurchaseDecisionRequest;
import com.mulinocoreano.backend.security.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/v1")
public class RecallController {
  private final RecallService service;

  public RecallController(RecallService service) {
    this.service = service;
  }

  @GetMapping("/recall/lots/{id}/trace")
  public JsonNode trace(@PathVariable long id) {
    return service.trace(id);
  }

  @GetMapping("/agent/recall/lots/{id}/trace")
  public JsonNode agentTrace(
      @PathVariable long id,
      @RequestHeader("Authorization") String auth,
      @AuthenticationPrincipal AgentActor actor) {
    return service.agentTrace(id, auth.substring(7), actor);
  }

  @GetMapping("/recall/approvals/{id}")
  public JsonNode approval(@PathVariable long id) {
    return service.approval(id);
  }

  @PostMapping("/recall/lots/{id}/assign")
  public JsonNode assign(
      @PathVariable long id,
      @RequestBody RecallService.RecallRequest request,
      @RequestHeader("Idempotency-Key") String key,
      @AuthenticationPrincipal ErpActor actor) {
    return service.assign(id, request, actor, key);
  }

  @PostMapping({"/recall/lots/{id}/propose", "/agent/recall/lots/{id}/propose"})
  public JsonNode propose(
      @PathVariable long id,
      @RequestBody RecallService.RecallRequest request,
      @RequestHeader(value = "Authorization", required = false) String auth,
      @RequestHeader("Idempotency-Key") String key,
      @AuthenticationPrincipal ErpActor actor) {
    return service.propose(id, request, actor, auth == null ? null : auth.substring(7), key);
  }

  @PostMapping("/recall/approvals/{id}/decision")
  public ResponseEntity<JsonNode> decide(
      @PathVariable long id,
      @Valid @RequestBody PurchaseDecisionRequest request,
      @RequestHeader("Idempotency-Key") String key,
      @AuthenticationPrincipal ErpActor actor) {
    var result = service.decide(id, request, actor, key);
    return ResponseEntity.status(result.has("error") ? 409 : 200).body(result);
  }

  @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
  public ResponseEntity<java.util.Map<String, String>> unsafe() {
    return ResponseEntity.status(409).body(java.util.Map.of("error", "RECALL_CONSTRAINT_REJECTED"));
  }
}
