package com.mulino.application.runtime;

import com.mulino.OntologyApplication;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import tools.jackson.databind.ObjectMapper;

/** Separate native JVM for isolated fixture delivery, never a paid/product adapter. */
public final class RuntimeVerificationHost {
 public static void main(String[] args) throws Exception {
  String external=System.getenv("MULINO_FIXTURE_EXTERNAL_URL");
  if(external==null)throw new IllegalStateException("Explicit isolated external fixture URL required");
  URI uri=URI.create(external);
  if(!"http".equals(uri.getScheme())||!java.util.Set.of("127.0.0.1","localhost","::1").contains(uri.getHost()))throw new IllegalArgumentException("Fixture service must be loopback");
  var app=new SpringApplication(OntologyApplication.class);app.setAdditionalProfiles("local","verification");
  try(var context=app.run(args)){
   var worker=context.getBean(DurableDeliveryWorker.class);
   var sweeper=context.getBean(RuntimeRecoverySweeper.class);
   var intake=context.getBean(RuntimeIntakeProcessor.class);
   var cds=context.getBean(com.sap.cds.services.runtime.CdsRuntime.class);
   var adapter=new ExternalDeliveryAdapter(){
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
    private final ObjectMapper json=new ObjectMapper();
    public Support support(String operation){return new Support(true,true);}
    public Result deliver(String operation,String id,Map<String,Object> payload){
     try{var request=HttpRequest.newBuilder(uri.resolve("/effects")).timeout(Duration.ofSeconds(2)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("externalOperationId",id,"operation",operation,"payload",payload)))).build();
      var response=http.send(request,HttpResponse.BodyHandlers.ofString());return response.statusCode()==200?Result.CONFIRMED_SUCCESS:Result.UNKNOWN;
     }catch(Exception failure){return Result.UNKNOWN;}
    }
    public Result lookup(String operation,String id){
     try{var response=http.send(HttpRequest.newBuilder(uri.resolve("/effects/"+id)).timeout(Duration.ofSeconds(2)).GET().build(),HttpResponse.BodyHandlers.ofString());return response.statusCode()==200?Result.CONFIRMED_SUCCESS:response.statusCode()==404?Result.CONFIRMED_FAILURE:Result.UNKNOWN;}
     catch(Exception failure){return Result.UNKNOWN;}
    }
   };
   while(!Thread.currentThread().isInterrupted()){cds.requestContext().run(c->{sweeper.sweep();intake.recoverDue();});worker.tick("verification-native-worker",Map.of("fixtureExternalEffect",adapter));Thread.sleep(1000);}
  }
 }
}
