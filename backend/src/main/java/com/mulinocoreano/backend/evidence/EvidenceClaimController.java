package com.mulinocoreano.backend.evidence;

import com.mulinocoreano.backend.security.ErpActor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping({"/api/v1/epistemic/cases/{caseRef}", "/api/v1/agent/epistemic/cases/{caseRef}"})
public class EvidenceClaimController {
  private final EvidenceClaimService service;

  public EvidenceClaimController(EvidenceClaimService service) {
    this.service = service;
  }

  private String token(String auth) {
    return auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : null;
  }

  @PostMapping("/evidence")
  public JsonNode source(
      @PathVariable String caseRef,
      @RequestBody EvidenceClaimService.Source request,
      @RequestHeader("Idempotency-Key") String key,
      @RequestHeader(value = "Authorization", required = false) String auth,
      @AuthenticationPrincipal ErpActor actor) {
    return service.register(caseRef, request, actor, token(auth), key);
  }

  @PostMapping("/claims")
  public JsonNode assertClaim(
      @PathVariable String caseRef,
      @RequestBody EvidenceClaimService.Assertion request,
      @RequestHeader("Idempotency-Key") String key,
      @RequestHeader(value = "Authorization", required = false) String auth,
      @AuthenticationPrincipal ErpActor actor) {
    return service.assertClaim(caseRef, request, actor, token(auth), key);
  }

  @PostMapping("/claims/{claimId}/links")
  public JsonNode link(
      @PathVariable String caseRef,
      @PathVariable long claimId,
      @RequestBody EvidenceClaimService.Link request,
      @RequestHeader("Idempotency-Key") String key,
      @RequestHeader(value = "Authorization", required = false) String auth,
      @AuthenticationPrincipal ErpActor actor) {
    return service.link(caseRef, claimId, request, actor, token(auth), key);
  }

  @PostMapping("/claims/{claimId}/judgment")
  public JsonNode judge(
      @PathVariable String caseRef,
      @PathVariable long claimId,
      @RequestBody EvidenceClaimService.Judgment request,
      @RequestHeader("Idempotency-Key") String key,
      @RequestHeader(value = "Authorization", required = false) String auth,
      @AuthenticationPrincipal ErpActor actor) {
    return service.judge(caseRef, claimId, request, actor, token(auth), key);
  }

  @GetMapping("/claims/{claimId}")
  public JsonNode review(
      @PathVariable String caseRef,
      @PathVariable long claimId,
      @RequestHeader(value = "Authorization", required = false) String auth,
      @AuthenticationPrincipal ErpActor actor) {
    return service.review(caseRef, claimId, actor, token(auth));
  }

  @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
  public ResponseEntity<java.util.Map<String, String>> constraint() {
    return ResponseEntity.status(409)
        .body(java.util.Map.of("error", "EVIDENCE_CONSTRAINT_REJECTED"));
  }
}
