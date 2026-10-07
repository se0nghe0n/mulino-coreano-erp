package org.mulino.verification.actual;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/** Tests the transport fault mechanism only, without claiming product business acceptance. */
final class ActualS3ResponseLossTest {
 @Test void upstreamResponseIsObservedButNeverReturnedToClient()throws Exception {
  var upstream=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);var commits=new AtomicInteger();
  upstream.createContext("/",exchange->{exchange.getRequestBody().readAllBytes();commits.incrementAndGet();byte[] body="{\"outcome\":\"APPLIED\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();});upstream.start();
  try(var proxy=new S3ResponseLossProxy(URI.create("http://127.0.0.1:"+upstream.getAddress().getPort()))) {
   var request=HttpRequest.newBuilder(proxy.uri().resolve("/actual-command")).header("Authorization","Bearer synthetic-fixture").POST(HttpRequest.BodyPublishers.ofString("{}" )).build();
   assertThrows(java.io.IOException.class,()->HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofString()));
   assertEquals(1,commits.get());assertEquals(1,proxy.receipt().path("upstreamCalls").asInt());
   assertFalse(proxy.receipt().path("clientResponseReturned").asBoolean());assertEquals("APPLIED",proxy.receipt().path("response").path("outcome").asText());
  }finally{upstream.stop(0);}
 }
}
