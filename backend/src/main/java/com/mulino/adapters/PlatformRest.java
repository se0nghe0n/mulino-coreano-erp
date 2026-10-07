package com.mulino.adapters;
import com.mulino.application.PlatformCommands;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestController public class PlatformRest {
 private final PlatformCommands service;public PlatformRest(PlatformCommands s){service=s;}
 @GetMapping("/api/platform/scopes/{id}") public Map<String,Object> read(@PathVariable String id){return service.read(id);}
 public record Reserve(String scopeId,String quantity,int expectedRevision,String idempotencyKey){}
 @PostMapping("/api/platform/actions/reserve") public Map<String,Object> reserve(@RequestBody Reserve c){return service.reserve(c.scopeId(),c.quantity(),c.expectedRevision(),c.idempotencyKey());}
 @ExceptionHandler(PlatformCommands.Conflict.class) @ResponseStatus(org.springframework.http.HttpStatus.CONFLICT) Map<String,Object> conflict(Exception e){return Map.of("outcome",e.getMessage());}
}
