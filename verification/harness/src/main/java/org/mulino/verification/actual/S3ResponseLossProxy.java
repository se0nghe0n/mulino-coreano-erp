package org.mulino.verification.actual;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.concurrent.atomic.*;
import org.mulino.verification.Json;

/** One actual upstream command is committed, then the client socket closes without a response. */
final class S3ResponseLossProxy implements AutoCloseable {
 private final HttpServer server;
 private final AtomicReference<JsonNode> upstream=new AtomicReference<>();
 private final AtomicInteger forwarded=new AtomicInteger(),closed=new AtomicInteger();
 S3ResponseLossProxy(URI backend)throws Exception {
  server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/",exchange->{
   try {
    byte[] body=exchange.getRequestBody().readAllBytes();
    if(forwarded.compareAndSet(0,1)){
     var target=backend.resolve(exchange.getRequestURI().toString());
     var request=HttpRequest.newBuilder(target).timeout(Duration.ofSeconds(30)).header("Content-Type","application/json").header("Authorization",exchange.getRequestHeaders().getFirst("Authorization")).POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
     var response=HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofString());
     var receipt=Json.object();receipt.put("upstreamHttpStatus",response.statusCode()).put("method","POST").put("path",target.getPath()).put("clientResponseReturned",false);receipt.set("response",Json.parse(response.body()));upstream.set(receipt);
    }
   }catch(Exception failure){var error=Json.object();error.put("failure",failure.getClass().getSimpleName());upstream.compareAndSet(null,error);}
   finally {closed.incrementAndGet();exchange.close();}
  });server.start();
 }
 URI uri(){return URI.create("http://127.0.0.1:"+server.getAddress().getPort());}
 JsonNode receipt(){var value=upstream.get();if(value==null)throw new IllegalStateException("Response-loss proxy did not observe upstream commit response");var result=value.deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)result).put("upstreamCalls",forwarded.get()).put("clientSocketsClosedWithoutResponse",closed.get());return result;}
 public void close(){server.stop(0);}
}
