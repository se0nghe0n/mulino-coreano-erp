package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.mulino.verification.Json;
import static org.mulino.verification.actual.T11HistoricalAssessmentFixture.*;

/** Explicit new synthetic positive sources use HTTP; the already-authored duty is a separate historical Given. */
final class T11LifecycleBaselineFixture {
 static boolean enabled(JsonNode f){return "T11-cancel-after-shipment".equals(f.path("fixtureId").asText())&&f.path("baseline").has("nativeR2LifecycleCompletion");}
 static void validate(JsonNode f){
  var b=f.path("baseline");var x=b.path("nativeR2LifecycleCompletion");
  require(enabled(f)&&x.path("synthetic").asBoolean()&&x.path("coordinatorSelectedSynthetic").asBoolean()&&!x.path("originalAuthorityClaimed").asBoolean(true)&&!x.path("operatingApprovalClaimed").asBoolean(true),"Explicit isolated synthetic lifecycle completion required");
  require(b.path("obligations").size()==1&&b.path("assignments").size()==1,"One original duty root/assignment required");
  var root=b.path("obligations").get(0);var assignment=b.path("assignments").get(0);
  require("ROOT".equals(root.path("alias").asText())&&"ASSIGN".equals(assignment.path("alias").asText())&&root.path("alias").equals(assignment.path("obligationAlias"))&&"SOURCE".equals(assignment.path("workAlias").asText())&&"A".equals(assignment.path("ownerAlias").asText())&&"OPEN".equals(root.path("status").asText())&&"OPEN".equals(assignment.path("status").asText())&&"10".equals(root.path("quantity").asText())&&"BOX".equals(root.path("unit").asText()),"Original duty identity/quantity/owner differs");
  var doc=f.path("aliases").path(x.path("sourceDocumentAlias").asText()).path("fixtureContent");
  require("DOCUMENT".equals(x.path("sourceKind").asText())&&root.path("sourceOccurrence").equals(doc.path("sourceOccurrence"))&&root.path("occurrenceVersion").equals(doc.path("sourceVersion"))&&root.equals(doc.path("originalObligation"))&&assignment.equals(doc.path("originalAssignment")),"Historical source document must preserve exact original Given");
  require(OffsetDateTime.parse(assignment.path("validFrom").asText()).compareTo(time(f.path("clock").path("asOf").asText()))<=0,"Duty Given is in the future");
  var actor=f.path("actors").path(x.path("actorAlias").asText());require(actor.isObject()&&!actor.path("roleCapabilities").toString().contains("assessGoal")&&!actor.path("grant").path("actions").toString().contains("assessGoal"),"No evaluation authority may be added");
  var physical=b.path("segments").get(0);require("Q".equals(physical.path("alias").asText())&&"100".equals(physical.path("quantity").asText())&&"BOX".equals(physical.path("unit").asText())&&physical.path("quantity").equals(x.path("quantity"))&&physical.path("unit").equals(x.path("unit"))&&physical.path("validFrom").equals(x.path("validFrom")),"Positive source completion must match exact original Q100/time");
  require(x.path("actions").equals(Json.array().add("SELL").add("DISPATCH"))&&x.path("positiveConditions").equals(Json.array().add("QC").add("CUSTOMER").add("COMMERCIAL").add("REGULATORY").add("LABEL"))&&"0".equals(x.path("startQuantity").asText()),"Every explicit positive action/condition is required");
  require(x.path("originalCurrentSaleConditions").equals(b.path("policies").get(0).path("currentSaleConditions"))&&x.path("originalSourceOccurrence").equals(root.path("sourceOccurrence"))&&x.path("sourceVersion").equals(root.path("occurrenceVersion")),"Original policy/source label changed");
  require(actor.path("grant").path("delegatorAlias").equals(x.path("actorAlias"))&&actor.path("grant").path("scope").path("itemAliases").equals(Json.array().add("P")),"New source authority must be explicitly isolated to item P");
  var expiry=x.path("lotExpiryCompletion");require(expiry.path("coordinatorSelectedSynthetic").asBoolean()&&!expiry.path("originalExpiryProvided").asBoolean(true)&&!expiry.path("originalAuthorityClaimed").asBoolean(true)&&expiry.path("applied").isBoolean(),"LOT expiry completion must declare its synthetic provenance");
  require(expiry.path("applied").asBoolean()?expiry.path("expiresAt").equals(f.path("aliases").path("LOT").path("expiresAt")):!f.path("aliases").path("LOT").has("expiresAt"),"Declared expiry completion application differs from installed LOT");
  for(String action:List.of("SELL","DISPATCH")) {
   var policy=f.path("aliases").path("nativeR2Policy"+action);require(policy.path("fictional").asBoolean()&&policy.path("requiresLabel").asBoolean()&&policy.path("version").equals(x.path("policyVersion"))&&policy.path("sourceNamespace").equals(x.path("namespace"))&&action.equals(policy.path("action").asText())&&policy.path("actorAlias").equals(x.path("actorAlias")),"Explicit typed policy/action/source binding mismatch");
  }
 }
 static ObjectNode installDuty(Connection c,JsonNode f,ObjectNode aliases)throws Exception {
  validate(f);var b=f.path("baseline");var x=b.path("nativeR2LifecycleCompletion");var original=b.path("obligations").get(0);var a=b.path("assignments").get(0);
  String org=Json.required(aliases,"ORG"),root=Json.required(aliases,"ROOT"),assignment=Json.required(aliases,"ASSIGN"),source=Json.required(aliases,x.path("sourceDocumentAlias").asText()),scopeId=UUID.randomUUID().toString();var at=time(f.path("clock").path("asOf").asText());
  try(var q=c.prepareStatement("SELECT availability,blobId,sha256 FROM mulino_evidence_DocumentVersions WHERE organizationId=? AND ID=?")){q.setString(1,org);q.setString(2,source);try(var r=q.executeQuery()){require(r.next()&&"AVAILABLE".equals(r.getString(1))&&r.getString(2)!=null&&r.getString(3).matches("[a-f0-9]{64}"),"Historical source Document/blob/hash unavailable");}}
  var scope=Json.object().put("itemId",Json.required(aliases,"P")).put("originalSourceOccurrence",original.path("sourceOccurrence").asText()).put("syntheticHistoricalGiven",true);scope.set("physicalScope",original.path("physicalScope"));String bytes=scope.toString();var quantity=new java.math.BigDecimal(original.path("quantity").asText());
  insert(c,"mulino_responsibility_Roots",row("organizationId",org,"ID",root,"revision",1,"createdAt",at,"recordedAt",at,"dutyKey",Json.sha256Text("T11-native-r2:"+root),"sourceId",source,"sourceKind","DOCUMENT","sourceVersion",original.path("occurrenceVersion").asText(),"kind",original.path("kind").asText(),"scopeJson",bytes,"quantity",quantity,"unit",original.path("unit").asText()));
  insert(c,"mulino_responsibility_Scopes",row("organizationId",org,"ID",scopeId,"revision",1,"createdAt",at,"recordedAt",at,"rootId",root,"startQuantity",java.math.BigDecimal.ZERO,"quantity",quantity,"unit",original.path("unit").asText(),"leaf",true,"scopeJson",bytes));
  insert(c,"mulino_work_read_ObligationReferences",row("organizationId",org,"ID",assignment,"revision",1,"createdAt",at,"recordedAt",at,"effectiveAt",time(a.path("validFrom").asText()),"workId",Json.required(aliases,a.path("workAlias").asText()),"kind",original.path("kind").asText(),"status",a.path("status").asText(),"ownerId",Json.required(aliases,a.path("ownerAlias").asText()),"supervisorId",Json.required(aliases,"supervisor"),"nextAction",a.path("nextAction").asText(),"nextCheckAt",time(a.path("nextCheckAt").asText()),"quantity",quantity,"unit",original.path("unit").asText(),"scopeJson",bytes,"rootId",root,"scopeId",scopeId,"valid",true,"evidenceId",source,"basis","SYNTHETIC_HISTORICAL_GIVEN"));
  return Json.object().put("recordType","SYNTHETIC_HISTORICAL_GIVEN").put("businessExecutionClaimed",false).put("rootId",root).put("assignmentId",assignment).put("sourceKind","DOCUMENT").put("sourceId",source).put("sourceVersion",original.path("occurrenceVersion").asText());
 }
 private final ActualConfiguration config;private final JsonNode fixture,spec,aliases;private final String external;private final ArrayNode commands=Json.array();private final HttpClient http=HttpClient.newHttpClient();private final JwtSigner signer;
 T11LifecycleBaselineFixture(ActualConfiguration config,JsonNode fixture,JsonNode aliases,String external){validate(fixture);this.config=config;this.fixture=fixture;this.spec=fixture.path("baseline").path("nativeR2LifecycleCompletion");this.aliases=aliases;this.external=external;signer=new JwtSigner(config);}
 private String id(String alias){return Json.required(aliases,alias);}
 private JsonNode call(String capability,ObjectNode slots,Integer revision)throws Exception {
  boolean record=Set.of("attachEvidence","recordActivity","recordDispositionBasis","recordSubmission","recordRegulatoryDecision","verifyLabel").contains(capability);
  var request=Json.object().put("intentKind",record?"RECORD":"COMMAND").put("definitionVersion",fixture.path("versions").path("definition").asText()).put("capabilityId",capability).put("commandIdempotencyKey",UUID.randomUUID().toString());request.set("subjectRefs",Json.array());request.set("slots",slots);var provenance=Json.object();slots.fieldNames().forEachRemaining(k->provenance.put(k,"USER"));request.set("provenance",provenance);if(revision!=null)request.put("expectedRevision",revision);
  var credential=(ObjectNode)fixture.path("actors").path(spec.path("actorAlias").asText()).deepCopy();credential.put("organizationAlias",external);
  var response=http.send(HttpRequest.newBuilder(config.baseUri().resolve("/api/ontology/"+(record?"records/":"commands/")+capability)).timeout(Duration.ofSeconds(30)).header("Content-Type","application/json").header("Authorization","Bearer "+signer.sign(credential)).POST(HttpRequest.BodyPublishers.ofString(request.toString())).build(),HttpResponse.BodyHandlers.ofString());
  var body=Json.parse(response.body());var receipt=Json.object().put("httpStatus",response.statusCode());receipt.set("request",request);receipt.set("response",body);commands.add(receipt);
  Json.write(Path.of(System.getProperty("repo.root"),".local/t11-native-r2-source-http",id("ORG")+".json"),commands);
  require(response.statusCode()==200&&"APPLIED".equals(body.path("outcome").asText()),"T11 synthetic source "+capability+" rejected: "+body);
  var out=(ObjectNode)body.deepCopy();if(body.path("effects").isObject())body.path("effects").fields().forEachRemaining(e->out.set(e.getKey(),e.getValue()));return out;
 }
 private record Source(String document,String canonical){}
 private Source publish(String kind,ObjectNode original,boolean quantity)throws Exception {
  original.set("syntheticCompletion",spec);String bytes=original.toString(),key=original.path("sourceDecisionId").asText(UUID.randomUUID().toString());var subject=Json.object().put("kind","SEGMENT").put("id",id("Q"));
  var upload=Json.object().put("sourceNamespace",spec.path("namespace").asText()).put("sourceReference",key).put("mediaType","application/json").put("expectedHash",Json.sha256Text(bytes)).put("provenance","NEW_EXPLICIT_T11_SYNTHETIC_COMPLETION_NOT_ORIGINAL_AUTHORITY").put("contentBase64",Base64.getEncoder().encodeToString(bytes.getBytes(StandardCharsets.UTF_8)));upload.set("subject",subject);String doc=Json.required(call("attachEvidence",upload,null),"id");
  var record=Json.object().put("kind",kind).put("sourceNamespace",spec.path("namespace").asText()).put("externalEventId",key).put("sourceVersion",spec.path("sourceVersion").asText()).put("effectiveFrom",spec.path("validFrom").asText()).put("timeZone","UTC").put("timePrecision","SECOND").put("valueState","KNOWN").put("payload",bytes).put("documentId",doc).put("assertion","Explicit synthetic fixture completion; no operating approval claimed");record.set("subject",subject);if(quantity)record.put("quantity","100").put("unit","BOX");var event=call("recordActivity",record,null);
  var match=Json.object().put("claimId",Json.required(event,"claimId")).put("basisDocumentId",doc).put("physicalScopeId",id("Q")).put("policyVersion",spec.path("policyVersion").asText()).put("sourceIdentity",spec.path("namespace").asText()+":"+key+":1").put("effectiveFrom",spec.path("validFrom").asText()).put("reason","Explicit synthetic original scope reconciliation");if(quantity)match.put("quantity","100").put("unit","BOX");var review=call("matchSourceIdentity",match,1);require("MATCHED".equals(review.path("evidenceStatus").asText()),"Synthetic source match unavailable");var canonical=call("linkCanonicalOccurrence",Json.object().put("reconciliationId",Json.required(review,"id")),1);return new Source(doc,Json.required(canonical,"id"));
 }
 private ObjectNode regOriginal(JsonNode procedure,String action){var p=fixture.path("aliases").path("nativeR2Policy"+action);return Json.object().put("procedureId",procedure.path("procedureId").asText()).put("procedureVersionId",procedure.path("procedureVersionId").asText()).put("regulatoryEventId",UUID.randomUUID().toString()).put("itemId",id("P")).put("lotId",id("LOT")).put("physicalScopeId",id("Q")).put("authority",p.path("authority").asText()).put("policyVersion",p.path("version").asText());}
 ObjectNode installSources()throws Exception {
  var credential=(ObjectNode)fixture.path("actors").path(spec.path("actorAlias").asText()).deepCopy();credential.put("organizationAlias",external);
  var clock=http.send(HttpRequest.newBuilder(config.baseUri().resolve("/__verification/runtime/clock/fixture-start")).timeout(Duration.ofSeconds(30)).header("Content-Type","application/json").header("Authorization","Bearer "+signer.sign(credential)).POST(HttpRequest.BodyPublishers.ofString(Json.object().put("instant",fixture.path("clock").path("asOf").asText()).toString())).build(),HttpResponse.BodyHandlers.ofString());
  require(clock.statusCode()==200&&Json.parse(clock.body()).path("authorityClock").asBoolean(),"Synthetic source fixture clock unavailable");
  for(String action:List.of("SELL","DISPATCH")) {
   for(String category:List.of("QC","CUSTOMER","COMMERCIAL")) {
    var original=Json.object().put("segmentId",id("Q")).put("sourceDecisionId",UUID.randomUUID().toString()).put("sourceVersion","1").put("workId",id("SOURCE")).put("operation","recordDispositionBasis").put("action",action).put("category",category).put("startQuantity","0").put("quantity","100").put("unit","BOX").put("validFrom",spec.path("validFrom").asText()).put("validUntil",spec.path("validUntil").asText());
    var source=publish("QUALITY_DECISION_recordDispositionBasis_"+category,original,true);var slots=original.deepCopy();slots.remove(List.of("sourceDecisionId","sourceVersion","operation","syntheticCompletion"));slots.put("evidenceId",source.document()).put("reason","Explicit synthetic fixture completion").put("nextCheckAt","2026-10-08T09:00:00Z");call("recordDispositionBasis",slots,1);
   }
   var preparation=Json.object().put("purpose","Explicit synthetic T11 regulatory source").put("segmentId",id("Q"));var prep=publish("T11_REGULATORY_PREPARATION",preparation,true);
   var procedure=call("prepareRegulatoryProcedure",Json.object().put("itemId",id("P")).put("lotId",id("LOT")).put("physicalScopeId",id("Q")).put("workId",id("SOURCE")).put("policyId",id("nativeR2Policy"+action)).put("documentVersionId",prep.document()),null);
   var submission=regOriginal(procedure,action).put("occurredAt",spec.path("validFrom").asText());var submitted=publish("REGULATORY_SUBMISSION",submission,false);
   procedure=call("recordSubmission",Json.object().put("procedureId",procedure.path("procedureId").asText()).put("occurredAt",spec.path("validFrom").asText()).put("occurrenceId",submitted.canonical()),procedure.path("revision").asInt());
   for(boolean label:List.of(false,true)) {
    var original=regOriginal(procedure,action).put("decision",label?"VERIFIED":"ALLOWED").put("validFrom",spec.path("validFrom").asText()).put("validUntil",spec.path("validUntil").asText());
    if(label){try(var c=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password());var q=c.prepareStatement("SELECT packagingVersionId,specificationVersionId FROM mulino_inventory_TradeItems WHERE organizationId=? AND ID=?")){q.setString(1,id("ORG"));q.setString(2,id("P"));try(var r=q.executeQuery()){require(r.next(),"Original item missing");original.put("packagingVersionId",r.getString(1)).put("specificationVersionId",r.getString(2));}}}
    else original.put("action",action).put("startQuantity","0").put("quantity","100").put("unit","BOX");
    var source=publish(label?"LABEL_VERIFICATION":"REGULATORY_DECISION",original,!label);var slots=original.deepCopy();slots.remove(List.of("procedureVersionId","itemId","lotId","physicalScopeId","authority","policyVersion","regulatoryEventId","syntheticCompletion"));slots.put("occurrenceId",source.canonical());procedure=call(label?"verifyLabel":"recordRegulatoryDecision",slots,procedure.path("revision").asInt());
   }
  }
  var out=Json.object().put("verificationPath","ACTUAL_HTTP_FOR_NEW_SYNTHETIC_SOURCE_COMPLETION").put("originalHistoricalDutyExecutionClaimed",false).put("operatingApprovalClaimed",false);out.set("commands",commands);return out;
 }
}
