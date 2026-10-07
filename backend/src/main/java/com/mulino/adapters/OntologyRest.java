package com.mulino.adapters;

import com.mulino.application.core.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestController
public class OntologyRest {
  private final ApplicationQueries queries;
  public OntologyRest(ApplicationQueries queries){this.queries=queries;}
  @PostMapping("/api/ontology/queries/{operation}")
  public Map<String,Object> query(@PathVariable String operation,@RequestBody Map<String,Object> input){return queries.query(QueryRequests.parse(operation,input));}
  @GetMapping("/api/ontology/objects/{id}")
  public Map<String,Object> object(@PathVariable String id,@RequestParam Map<String,String> params){Map<String,Object> body=new LinkedHashMap<>(params);body.put("id",id);return query("getObject",body);}
  @GetMapping("/api/ontology/works/{id}")
  public Map<String,Object> work(@PathVariable String id,@RequestParam Map<String,String> params){Map<String,Object> body=new LinkedHashMap<>(params);body.put("id",id);return query("getWork",body);}
  @ExceptionHandler(DomainError.class)
  ResponseEntity<Map<String,Object>> error(DomainError failure){return ResponseEntity.status(failure.code().equals("FORBIDDEN")?403:failure.outcome().equals("CONFLICT")?409:400).body(failure.response());}
}
