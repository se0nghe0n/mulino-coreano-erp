package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.mulino.verification.*;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;

/** Bounded deterministic T13 HTTP fixture probe; never a model/scenario runner or DB writer. */
final class T13SupplierSourceFacts {
 final Path root;final ActualConfiguration config;final ObjectNode fixture,installed;final JsonNode aliases,sub;
 final Map<String,JsonNode> results=new LinkedHashMap<>();final List<JsonNode> httpReceipts=Collections.synchronizedList(new ArrayList<>());final HttpClient http=HttpClient.newHttpClient();
 T13SupplierSourceFacts(Path root,ActualConfiguration config,String subcase)throws Exception {
  this(root,config,subcase,f->{});
 }
 T13SupplierSourceFacts(Path root,ActualConfiguration config,String subcase,java.util.function.Consumer<ObjectNode> mutate)throws Exception {
  this.root=root;this.config=config;
  var c=Json.read(root.resolve("verification/cases/T13/case.json"));JsonNode selected=null;
  if(!Set.of("supplier-accept","supplier-reject","supplier-change").contains(subcase))throw new IllegalArgumentException("T13 supplier scope required");
  for(var row:c.path("subcases"))if(row.path("id").asText().equals(subcase))selected=row;
  sub=Objects.requireNonNull(selected);fixture=(ObjectNode)Json.read(root.resolve(sub.path("fixtureRef").asText()));
  mutate.accept(fixture);
  var bundle=Json.object().put("fixtureHash",Json.sha256(root.resolve(sub.path("fixtureRef").asText())));bundle.set("fixture",fixture);
  var bound=ActualFixtureBindings.bind(root,bundle,config);
  installed=ScenarioFixtureInstaller.install(config,root,bound,true);aliases=installed.path("aliasMap");
  post("/__verification/runtime/clock/fixture-start",Json.object().put("instant",fixture.path("clock").path("asOf").asText()),"procurement");
 }
 JsonNode post(String path,ObjectNode body,String actor)throws Exception {return request(path,"POST",body,actor,null);}
 JsonNode request(String path,String method,ObjectNode body,String actor,ObjectNode barrier)throws Exception {
  var credential=(ObjectNode)ActualFixtureBindings.credentialActor(root,config,fixture.path("actors").path(actor));credential.put("organizationAlias",installed.path("organizationExternalAlias").asText());
  var builder=HttpRequest.newBuilder(config.baseUri().resolve(path)).timeout(java.time.Duration.ofSeconds(60)).header("Authorization","Bearer "+new JwtSigner(config).sign(credential)).header("Content-Type","application/json");
  if(barrier!=null)builder.header("X-Mulino-Test-Barrier",barrier.toString());
  if(method.equals("GET"))builder.GET();else builder.POST(HttpRequest.BodyPublishers.ofString(body.toString()));
  var r=http.send(builder.build(),HttpResponse.BodyHandlers.ofString());
  var response=Json.parse(r.body());var receipt=Json.object().put("httpStatus",r.statusCode()).put("driverStatus","EXECUTED");receipt.set("provenance",Json.object().put("scopeComplete",true).put("source","HTTP"));receipt.set("request",body==null?Json.object():body);receipt.set("response",response);receipt.set("transport",Json.object().put("method",method).put("path",path).put("actorRef",actor));httpReceipts.add(receipt);return receipt;
 }
 JsonNode call(String cap,ObjectNode request,String actor)throws Exception {return post("/api/ontology/"+(request.path("intentKind").asText().equals("RECORD")?"records/":"commands/")+cap,request,actor);}
 void executeThrough(String stop)throws Exception {executeThrough(stop,Set.of());}
 void executeThrough(String stop,Set<String> omitted)throws Exception {
  for(var a:sub.path("actions")){
   String id=a.path("id").asText(),kind=a.path("kind").asText();if(omitted.contains(id))continue;JsonNode receipt=null;
   if(kind.equals("invoke")) {
    var request=(ObjectNode)new ReferenceResolver(results,aliases).resolve(a.path("request"));receipt=call(a.path("capabilityId").asText(),request,a.path("actorRef").asText());results.put(id,receipt);
    if(!"APPLIED".equals(receipt.path("response").path("outcome").asText()))throw new IllegalStateException(id+": "+receipt);
   } else if(id.equals("delivery-ack")) {
    var p=a.path("control").path("parameters");receipt=post("/__verification/runtime/responder",Json.object().put("mode","CONFIRMED_SUCCESS").put("externalOperationId",p.path("externalOperationId").asText()),"procurement");results.put(id,receipt);
   } else if(id.equals("source-delivery-tick")) {
    receipt=post("/__verification/runtime/scheduler/tick",Json.object().put("schedulerId","T13-bounded-http"),"procurement");results.put(id,receipt);
   }
   if(id.equals(stop))return;
  }
 }
 ObjectNode replyRequest() {for(var a:sub.path("actions"))if(a.path("id").asText().equals("supplier-reply"))return (ObjectNode)new ReferenceResolver(results,aliases).resolve(a.path("request"));throw new IllegalStateException();}
 void save(Path path)throws Exception {var r=Json.object();
  var raw=Json.object();
  try(var db=java.sql.DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password())) {
   for(String table:List.of("mulino_trade_purchase_Orders","mulino_trade_purchase_OrderLines","mulino_trade_purchase_SupplierReplies","mulino_trade_purchase_ReceiptCredits","mulino_runtime_Outbox","mulino_runtime_ExternalReconciliations","mulino_evidence_CanonicalOccurrences","mulino_evidence_Verifications","mulino_work_read_ObligationReferences","mulino_commands_CommandAudits")) {
    var rows=Json.array();try(var q=db.prepareStatement("SELECT * FROM "+table+" WHERE organizationId=?")){q.setString(1,aliases.path("ORG").asText());try(var result=q.executeQuery()){while(result.next()){var row=Json.object();for(int i=1;i<=result.getMetaData().getColumnCount();i++)row.put(result.getMetaData().getColumnLabel(i),result.getString(i));rows.add(row);}}}raw.set(table,rows);
   }
  }
  synchronized(httpReceipts){r.set("httpReceipts",Json.MAPPER.valueToTree(httpReceipts));}r.set("rawRows",raw);r.set("installer",installed);r.set("commands",Json.MAPPER.valueToTree(results));Json.write(path,r);}
}
