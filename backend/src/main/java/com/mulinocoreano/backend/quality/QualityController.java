package com.mulinocoreano.backend.quality;

import com.mulinocoreano.backend.procurement.PurchaseDecisionRequest;
import com.mulinocoreano.backend.security.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/v1")
public class QualityController {
    private final QualityService quality;
    public QualityController(QualityService quality) { this.quality=quality; }
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<java.util.Map<String,String>> unsafe(org.springframework.dao.DataIntegrityViolationException error) {
        return ResponseEntity.status(409).body(java.util.Map.of("error","QUALITY_CONSTRAINT_REJECTED"));
    }
    @PostMapping("/quality/production-inputs")
    public JsonNode produce(@RequestBody QualityService.ProductionInput input,
            @RequestHeader("Idempotency-Key") String key,@AuthenticationPrincipal ErpActor actor) {
        return quality.produce(input,actor,key);
    }
    @GetMapping("/quality/approvals/{id}")
    public JsonNode approval(@PathVariable long id) { return quality.inspection(id); }
    @GetMapping("/quality/inbound/{id}")
    public JsonNode inbound(@PathVariable long id) { return quality.inbound(id); }
    @GetMapping("/agent/quality/inbound/{id}")
    public JsonNode agentInbound(@PathVariable long id, @RequestHeader("Authorization") String auth,
            @AuthenticationPrincipal AgentActor actor) { return quality.agentInbound(id,auth.substring(7),actor); }
    @PostMapping("/quality/inbound/{id}/assign")
    public JsonNode assign(@PathVariable long id,@RequestBody QualityService.InspectRequest request,
            @RequestHeader("Idempotency-Key") String key,@AuthenticationPrincipal ErpActor actor) {
        return quality.assign(id,request,actor,key);
    }
    @PostMapping({"/quality/inbound/{id}/inspect","/agent/quality/inbound/{id}/inspect"})
    public JsonNode inspect(@PathVariable long id, @RequestBody QualityService.InspectRequest request,
            @RequestHeader(value="Authorization",required=false) String auth,
            @RequestHeader("Idempotency-Key") String key, @AuthenticationPrincipal ErpActor actor) {
        return quality.inspect(id,request,actor,auth==null?null:auth.substring(7),key);
    }
    @PostMapping("/quality/approvals/{id}/decision")
    public ResponseEntity<JsonNode> decide(@PathVariable long id,@Valid @RequestBody PurchaseDecisionRequest request,
            @RequestHeader("Idempotency-Key") String key,@AuthenticationPrincipal ErpActor actor) {
        var response=quality.decide(id,request,actor,key); return ResponseEntity.status(response.has("error")?409:200).body(response);
    }
}
