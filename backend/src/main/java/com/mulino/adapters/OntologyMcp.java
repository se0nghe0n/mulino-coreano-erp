package com.mulino.adapters;

import com.mulino.application.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

/** Stateless product read channel; S0 protocol proof remains at /mcp. */
@RestController
public class OntologyMcp {
  private static final String VERSION="2026-07-28";
  private final ApplicationQueries queries;
  private final ObjectMapper json=new ObjectMapper();
  public OntologyMcp(ApplicationQueries queries){this.queries=queries;}
  @PostMapping(value="/mcp/ontology",consumes="application/json",produces="application/json")
  @SuppressWarnings("unchecked")
  public ResponseEntity<Map<String,Object>> rpc(@RequestBody Map<String,Object> body,HttpServletRequest request){
    Object id=body.get("id");
    if(!"2.0".equals(body.get("jsonrpc"))||id==null||!(body.get("method")instanceof String method)||!(body.get("params")instanceof Map<?,?> params))return rpcError(id,400,-32600,"Invalid request");
    String accept=request.getHeader("Accept");
    if(accept==null||!accept.contains("application/json")||!accept.contains("text/event-stream"))return rpcError(id,406,-32600,"Required Accept missing");
    String origin=request.getHeader("Origin");if(origin!=null&&!origin.equals("http://localhost:8080"))return rpcError(id,403,-32020,"Origin denied");
    if(!method.equals(request.getHeader("Mcp-Method"))||!VERSION.equals(request.getHeader("MCP-Protocol-Version"))||!(params.get("_meta")instanceof Map<?,?> meta)||!VERSION.equals(meta.get("io.modelcontextprotocol/protocolVersion"))||!(meta.get("io.modelcontextprotocol/clientCapabilities")instanceof Map<?,?>))return rpcError(id,400,-32020,"HeaderMismatch");
    Map<String,Object> result;
    switch(method){
      case "server/discover" -> result=Map.of("supportedVersions",List.of(VERSION),"capabilities",Map.of("tools",Map.of()),"_meta",Map.of("io.modelcontextprotocol/serverInfo",Map.of("name","mulino-ontology","version","1.0.0")));
      case "tools/list" -> result=Map.of("tools",queries.operations().stream().sorted().map(this::tool).toList());
      case "tools/call" -> {
        if(!(params.get("name")instanceof String operation)||!operation.equals(request.getHeader("Mcp-Name"))||!(params.get("arguments")instanceof Map<?,?> arguments))return rpcError(id,400,-32602,"Invalid tool arguments");
        try{var value=queries.query(QueryRequests.parse(operation,(Map<String,Object>)arguments));result=toolResult(value,false);}
        catch(DomainError failure){result=toolResult(failure.response(),true);}
      }
      default -> {return rpcError(id,404,-32601,"Method not found");}
    }
    Map<String,Object> complete=new LinkedHashMap<>(result);complete.put("resultType","complete");
    return ResponseEntity.ok(Map.of("jsonrpc","2.0","id",id,"result",complete));
  }
  private Map<String,Object> tool(String operation){return Map.of("name",operation,"description","Authorized ontology read", "inputSchema",Map.of("type","object","properties",Map.of("id",Map.of("type","string","format","uuid"),"scope",Map.of("type","object"),"filters",Map.of("type","object"),"asOf",Map.of("type","string","format","date-time"),"knownAt",Map.of("type","string","format","date-time"),"snapshotRef",Map.of("type","string"),"limit",Map.of("type","integer","minimum",1,"maximum",200),"cursor",Map.of("type","string"),"definitionVersion",Map.of("type","string")),"additionalProperties",false));}
  private Map<String,Object> toolResult(Object value,boolean error){try{return Map.of("isError",error,"structuredContent",value,"content",List.of(Map.of("type","text","text",json.writeValueAsString(value))));}catch(Exception failure){throw new IllegalStateException(failure);}}
  private ResponseEntity<Map<String,Object>> rpcError(Object id,int status,int code,String message){Map<String,Object> body=new LinkedHashMap<>();body.put("jsonrpc","2.0");body.put("id",id);body.put("error",Map.of("code",code,"message",message));return ResponseEntity.status(status).body(body);}
}
