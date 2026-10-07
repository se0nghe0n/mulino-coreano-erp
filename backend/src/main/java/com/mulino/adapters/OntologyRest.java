package com.mulino.adapters;

import com.mulino.application.core.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestController
public class OntologyRest {
  private final ApplicationQueries queries;
  private final ApplicationCommands commands;
  public OntologyRest(ApplicationQueries queries,ApplicationCommands commands){this.queries=queries;this.commands=commands;}
  @PostMapping("/api/ontology/queries/{operation}")
  public Map<String,Object> query(@PathVariable String operation,@RequestBody Map<String,Object> input){return queries.query(QueryRequests.parse(operation,input));}
  @GetMapping("/api/ontology/objects/{id}")
  public Map<String,Object> object(@PathVariable String id,@RequestParam Map<String,String> params){Map<String,Object> body=new LinkedHashMap<>(params);body.put("id",id);return query("getObject",body);}
  @GetMapping("/api/ontology/works/{id}")
  public Map<String,Object> work(@PathVariable String id,@RequestParam Map<String,String> params){Map<String,Object> body=new LinkedHashMap<>(params);body.put("id",id);return query("getWork",body);}
  @PostMapping("/api/ontology/commands/validate")
  public Map<String,Object> validateCommand(@RequestBody Map<String,Object> input){return commands.validate(input);}
  @PostMapping("/api/ontology/commands/{capability}")
  public Map<String,Object> command(@PathVariable String capability,@RequestBody Map<String,Object> input){return execute(capability,"COMMAND",input);}
  @PostMapping("/api/ontology/records/{capability}")
  public Map<String,Object> record(@PathVariable String capability,@RequestBody Map<String,Object> input){return execute(capability,"RECORD",input);}
  @PostMapping("/api/evidence/uploads")
  public Map<String,Object> upload(@RequestBody Map<String,Object> input){return execute("attachEvidence","RECORD",input);}
  @GetMapping("/api/ontology/capabilities")
  public Map<String,Object> capabilities(){return Map.of("queries",queries.operations(),"commands",commands.manifest());}
  private Map<String,Object> execute(String capability,String kind,Map<String,Object> input){
    if(!capability.equals(input.get("capabilityId"))||!kind.equals(input.get("intentKind")))throw DomainError.invalid("Capability and intent kind must match endpoint");
    return commands.execute(input);
  }
  @ExceptionHandler(DomainError.class)
  ResponseEntity<Map<String,Object>> error(DomainError failure){return ResponseEntity.status(failure.code().equals("FORBIDDEN")?403:failure.outcome().equals("CONFLICT")?409:400).body(failure.response());}
}
