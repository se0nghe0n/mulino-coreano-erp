package org.mulino.verification.actual;

import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.file.*;
import java.security.KeyPairGenerator;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mulino.verification.*;
import static org.junit.jupiter.api.Assertions.*;

/** Transport contract only: the separate Spring/PG test proves the actual checkpoint. */
class ActualDeferredFaultControlTest {
 @TempDir Path dir;
 @Test void releaseTransmitsExactScopeAndRetainsCommittedServerEvidence()throws Exception{
  exercise(200,"RELEASED",true,(driver,wire)->{var result=driver.control("release",request());assertEquals(StepResult.DriverStatus.EXECUTED,result.driverStatus());assertTrue(result.data().path("serverObservation").path("committed").asBoolean());assertEquals("RELEASED",result.data().path("state").asText());assertEquals("release",wire.get().path("operation").asText());assertEquals("11111111-1111-1111-1111-111111111111",wire.get().path("organizationId").asText());assertEquals("evidence.correct.afterPendingBeforeAssessment",wire.get().path("point").asText());assertFalse(wire.get().has("scope"));});
 }
 @Test void refusedOrUncommittedReleaseCannotBecomeAcknowledged()throws Exception{
  exercise(422,"ARMED",false,(driver,wire)->{var result=driver.control("release",request());assertEquals(StepResult.DriverStatus.NOT_IMPLEMENTED,result.driverStatus());assertTrue(result.reason().contains("has not reached"));});
  exercise(200,"RELEASED",false,(driver,wire)->assertThrows(IllegalStateException.class,()->driver.control("release",request())));
 }
 com.fasterxml.jackson.databind.JsonNode request(){return Json.parse("{\"type\":\"fault\",\"operation\":\"release\",\"parameters\":{\"point\":\"evidence.correct.afterPendingBeforeAssessment\",\"scope\":{\"organizationId\":\"11111111-1111-1111-1111-111111111111\",\"caseId\":\"T11\"}}}");}
 interface Check{void run(ActualAcceptanceDriver driver,AtomicReference<com.fasterxml.jackson.databind.JsonNode> wire)throws Exception;}
 void exercise(int status,String state,boolean committed,Check check)throws Exception{
  String prior=System.getProperty("verification.actual.suiteIsolation");var wire=new AtomicReference<com.fasterxml.jackson.databind.JsonNode>();var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/__verification/runtime/fault",exchange->{wire.set(Json.parse(new String(exchange.getRequestBody().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)));var response=Json.object().put("organizationId",wire.get().path("organizationId").asText()).put("point",wire.get().path("point").asText()).put("faultId","actual-control-id").put("state",state).put("committed",committed);if(status==422)response.put("reason","Checkpoint has not reached a committed correction");byte[] bytes=response.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(status,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();});server.start();
  Path pem=dir.resolve(UUID.randomUUID()+".pem");var generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);Files.writeString(pem,"-----BEGIN PRIVATE KEY-----\n"+Base64.getEncoder().encodeToString(generator.generateKeyPair().getPrivate().getEncoded())+"\n-----END PRIVATE KEY-----\n");
  try{System.setProperty("verification.actual.suiteIsolation","true");var config=new ActualConfiguration(URI.create("http://127.0.0.1:"+server.getAddress().getPort()),"jdbc:postgresql://localhost/unused","unused","unused",pem,"https://fixture.invalid","ontology","deferred-control-contract");var driver=new ActualAcceptanceDriver(dir,config,Map.of());var field=ActualAcceptanceDriver.class.getDeclaredField("controlActor");field.setAccessible(true);field.set(driver,Json.object().put("subject","synthetic-control").put("organizationAlias","11111111-1111-1111-1111-111111111111"));check.run(driver,wire);}
  finally{server.stop(0);Files.deleteIfExists(pem);if(prior==null)System.clearProperty("verification.actual.suiteIsolation");else System.setProperty("verification.actual.suiteIsolation",prior);}
 }
}
