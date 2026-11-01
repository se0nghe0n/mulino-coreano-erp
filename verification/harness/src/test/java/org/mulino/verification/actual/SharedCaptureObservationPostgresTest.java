package org.mulino.verification.actual;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mulino.verification.Json;

@EnabledIfEnvironmentVariable(named="OBSERVER_PG_TEST",matches="true")
class SharedCaptureObservationPostgresTest extends ScenarioObservationPostgresTest {
 ObjectNode captureRow;ObjectNode boundary;ObjectNode facts;
 void captureSource()throws Exception{
  base();boundary=Json.object().put("organizationId",org).put("asOf",now).put("knownAt",now);boundary.set("scope",Json.object().put("itemId",item));
  facts=Json.object();var fact=Json.object().put("sourceId","q").put("physicalScopeId","q").put("recordedAt",before);fact.set("evidenceRefs",Json.array().add("m"));
  var props=Json.object();props.set("QuantitySegment.stateQuantity",Json.array().add(fact));props.set("Other.quantity",Json.array().add(fact.deepCopy()));facts.set("properties",props);
  var source=Json.object().put("ID","m").put("sourceId","q").put("organizationId",org).put("recordedAt",before).put("kind","DISPATCH");var sources=Json.object();sources.set("inventory.QuantityMovements",Json.array().add(source));facts.set("sources",sources);
  var body=Json.object();body.set("boundary",boundary);body.set("facts",facts);
  captureRow=Json.object().put("ID",id()).put("organizationId",org).put("asOf",now).put("knownAt",now).put("recordedAt",now).put("contentJson",body.toString()).put("contentHash",inputHash(body.toString()));insert("mulino_evaluation_EvidenceCaptures",captureRow);
 }
 ObjectNode envelope(String name,int diagnosticCount)throws Exception{
  var conditions=Json.array();for(int i=0;i<diagnosticCount;i++){var condition=Json.object().put("conditionId",i==0?name:name+"."+i);condition.set("truth",Json.object().put("state","SATISFIED").put("conflict",false));condition.set("inputs",Json.array());conditions.add(condition);}
  var authored=conditions.get(0).deepCopy();((ObjectNode)authored).set("diagnostics",conditions.deepCopy());
  var body=Json.object().put("goalId",goal);body.set("conditions",conditions);body.set("publicConditions",Json.array().add(authored));body.set("facts",facts);body.set("captureBoundary",boundary);
  String aid=id();var a=assessment(aid,work,now,conditions.toString()).put("inputSnapshotHash",inputHash(body.toString())).put("asOf",now).put("knownAt",now);insert("mulino_work_read_AssessmentReferences",a);
  var snapshot=Json.object().put("ID",id()).put("organizationId",org).put("recordedAt",now).put("asOf",now).put("knownAt",now).put("assessmentId",aid).put("goalId",goal).put("workId",work).put("evidenceCaptureId",captureRow.path("ID").asText()).put("contentJson",body.toString()).put("contentHash",inputHash(body.toString()));insert("mulino_evaluation_InputSnapshots",snapshot);return snapshot;
 }
 @Test void sharedCaptureProjectsOneUniqueRelationAndNamedConditionPerEnvelope()throws Exception{
  captureSource();var ids=new HashSet<String>();for(int n:List.of(3,3,6))ids.add(envelope("held-throughout",n).path("ID").asText());
  var result=capture("assessmentInputs","conditionResults");var inputs=result.at("/rawRows/assessmentInputs");assertEquals(3,inputs.size());
  for(var input:inputs){assertEquals(captureRow.path("ID"),input.path("snapshotId"));assertTrue(ids.contains(input.path("inputEnvelopeId").asText()));assertEquals("q",input.path("segmentId").asText());assertEquals("m",input.path("movementId").asText());assertEquals(1,input.path("conditions").size());assertTrue(input.path("internalDiagnostics").size()>=3);}
  assertEquals(3,result.at("/rawRows/conditionResults").size());for(var condition:result.at("/rawRows/conditionResults")){assertEquals("held-throughout",condition.path("conditionId").asText());assertTrue(condition.path("diagnostics").size()>=3);}
 }
 @Test void sharedCaptureMismatchNeverFallsBackToEnvelopeOrQueryIdentity()throws Exception{
  captureSource();var snapshot=envelope("arrived-at-W",3);changeSupplier("mulino_evaluation_EvidenceCaptures",captureRow,"contentHash","0".repeat(64));assertThrows(UnsupportedOperationException.class,()->capture("assessmentInputs"));
 }
 @Test void sharedCaptureKeepsTwelveSingleConditionAndElevenPendingMarkerSeparate()throws Exception{
  captureSource();envelope("arrived-at-W",3);var result=capture("assessmentInputs");assertEquals(1,result.at("/rawRows/assessmentInputs").size());assertEquals("arrived-at-W",result.at("/rawRows/assessmentInputs/0/conditionId").asText());
  var transition=row(id());transition.remove("createdAt");transition.put("workId",work).put("revision",3).put("state","ACTIVE").put("snapshotJson",Json.object().put("ID",work).put("organizationId",org).put("revision",3).put("effectiveAt",now).put("pendingInvalidation",true).toString());insert("mulino_work_WorkTransitions",transition);
  int wiredPending=0;for(var input:capture("assessmentInputs").at("/rawRows/assessmentInputs"))if(input.path("status").asText().equals("PENDING"))wiredPending++;
  assertEquals(1,wiredPending,"ScenarioJdbcObservation must pass the original request asOf to the projection");
  com.fasterxml.jackson.databind.JsonNode rows;
  try(var reader=database.openSchemaConnection(schema)){reader.setAutoCommit(false);reader.setReadOnly(true);reader.setTransactionIsolation(java.sql.Connection.TRANSACTION_REPEATABLE_READ);ObserverSnapshot.token(reader);rows=new ScenarioObservationProjection(reader,root,List.of(org),List.of(item),List.of(work),List.of(lot),java.time.OffsetDateTime.parse(now),java.time.OffsetDateTime.parse(now)).virtual("@assessmentInputs");reader.commit();}
  int pending=0;for(var input:rows)if(input.path("status").asText().equals("PENDING")){pending++;assertEquals("WORK_PENDING_INPUT",input.path("rowKind").asText());assertFalse(input.has("snapshotId"));assertFalse(input.has("movementId"));}
  assertEquals(1,pending);
  try(var reader=database.openSchemaConnection(schema)){reader.setAutoCommit(false);reader.setReadOnly(true);reader.setTransactionIsolation(java.sql.Connection.TRANSACTION_REPEATABLE_READ);var earlier=new ScenarioObservationProjection(reader,root,List.of(org),List.of(item),List.of(work),List.of(lot),java.time.OffsetDateTime.parse(now),java.time.OffsetDateTime.parse(before)).virtual("@assessmentInputs");for(var input:earlier)assertNotEquals("PENDING",input.path("status").asText());reader.commit();}
 }
}
