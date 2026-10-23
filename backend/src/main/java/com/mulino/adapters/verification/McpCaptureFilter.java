package com.mulino.adapters.verification;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.*;

final class McpCaptureFilter extends OncePerRequestFilter {
  static final String REQUEST_HEADER="X-Mulino-Verification-Capture",RECEIPT_HEADER="X-Mulino-Verification-Receipt";
  private final McpCaptureStore store;
  private final McpIdentitySnapshot snapshots;
  private final ObjectMapper json=new ObjectMapper();
  McpCaptureFilter(McpCaptureStore store,McpIdentitySnapshot snapshots) { this.store=store;this.snapshots=snapshots; }
  static boolean loopback(HttpServletRequest r) {
    // Addresses come from the socket, not forwarded headers; no DNS names are accepted.
    try {return literalLoopback(r.getRemoteAddr())&&literalLoopback(r.getLocalAddr());}
    catch(Exception invalid){return false;}
  }
  private static boolean literalLoopback(String address) throws Exception {
    return address!=null&&address.matches("[0-9a-fA-F:.]+")&&InetAddress.getByName(address).isLoopbackAddress();
  }
  @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
    String nonce=request.getHeader(REQUEST_HEADER);
    if(!"POST".equals(request.getMethod())||!loopback(request)||nonce==null||!nonce.matches("[a-f0-9-]{36}")) {
      chain.doFilter(request,response);return;
    }
    Map<String,Object> snapshot;
    try {UUID.fromString(nonce);snapshot=snapshots.capture();}
    catch(RuntimeException unavailable){chain.doFilter(request,response);return;}
    var input=new ContentCachingRequestWrapper(request,1_048_576);
    var output=new ContentCachingResponseWrapper(response);
    try {
      chain.doFilter(input,output);
      try {
        byte[] requestBytes=input.getContentAsByteArray(),responseBytes=output.getContentAsByteArray();
        if(requestBytes.length>=1_048_576||responseBytes.length>4_194_304)return;
        var rpc=json.readTree(requestBytes);var reply=json.readTree(responseBytes);
        if(rpc==null||reply==null||!"2.0".equals(rpc.path("jsonrpc").asText())||!rpc.hasNonNull("id"))return;
        var receipt=new LinkedHashMap<String,Object>(snapshot);
        receipt.put("recordType","SERVER_AUTHENTICATED_MCP_CAPTURE_V1");receipt.put("requestNonce",nonce);
        receipt.put("requestSha256",sha(requestBytes));receipt.put("responseSha256",sha(responseBytes));
        receipt.put("rpcId",json.convertValue(rpc.path("id"),Object.class));
        receipt.put("rpcMethod",rpc.path("method").asText());receipt.put("toolName",rpc.path("params").path("name").asText(""));
        receipt.put("httpStatus",output.getStatus());receipt.put("endpointPath","/mcp/ontology");
        receipt.put("completedAt",Instant.now().toString());
        @SuppressWarnings("unchecked") var owner=(Map<String,Object>)snapshot.get("identity");
        String capture=store.put(owner,receipt);if(capture!=null)output.setHeader(RECEIPT_HEADER,capture);
      } catch(Exception incomplete) { /* Missing receipt is fail-closed evidence, never a product response rewrite. */ }
    } finally { output.copyBodyToResponse(); }
  }
  static String sha(byte[] bytes) {
    try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    catch(Exception impossible){throw new IllegalStateException(impossible);}
  }
}
