package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.mulino.verification.Json;

/** Imports one explicitly synthetic historical Given. It never evaluates, registers an evaluator or selects a current goal. */
final class T11HistoricalAssessmentFixture {
 static boolean enabled(JsonNode f){return "T11-closed-followup".equals(f.path("fixtureId").asText())&&f.path("baseline").has("historicalGoalCompletion");}
 static void validate(JsonNode f) {
  var b=f.path("baseline");var x=b.path("historicalGoalCompletion");
  require(enabled(f)&&x.path("synthetic").asBoolean()&&x.path("coordinatorSelectedSynthetic").asBoolean()
   &&x.path("originalGoalModeProvided").isBoolean()&&!x.path("originalGoalModeProvided").asBoolean()
   &&x.path("originalAuthorityClaimed").isBoolean()&&!x.path("originalAuthorityClaimed").asBoolean()
   &&x.path("historicalEvaluationExecuted").isBoolean()&&!x.path("historicalEvaluationExecuted").asBoolean(),"Explicit synthetic historical completion required");
  require(b.path("assessments").size()==1&&b.path("works").size()==1,"One historical Work and assessment required");
  var a=b.path("assessments").get(0);var w=b.path("works").get(0);var g=x.path("goal");
  require(x.path("workAlias").equals(w.path("alias"))&&x.path("assessmentAlias").equals(a.path("alias"))&&x.path("definitionVersion").equals(f.path("versions").path("definition")),"Historical completion must name the original Work/assessment/definition");
  require("PARENT".equals(w.path("alias").asText())&&"CLOSED".equals(w.path("state").asText())&&"FULFILLED".equals(w.path("closeReason").asText())
   &&"OLDASSESS".equals(a.path("alias").asText())&&a.path("alias").equals(w.path("originalAssessmentAlias"))&&a.path("workAlias").equals(w.path("alias"))
   &&a.path("goalVersion").equals(w.path("goalVersion"))&&a.path("goalVersion").equals(x.path("originalGoalLabel"))
   &&"SATISFIED".equals(a.path("result").asText())&&"evaluator-v1".equals(a.path("evaluatorVersion").asText()),"Original historical identity/version/result differs");
  require("STATE_AT".equals(g.path("quantityMode").asText())&&"100".equals(g.path("targetQuantity").asText())&&"BOX".equals(g.path("unit").asText())
   &&"PHYSICAL_HELD".equals(g.path("action").asText())&&g.path("includeReserved").asBoolean()&&g.path("evaluationAt").equals(a.path("evaluatedAt")),"Explicit physical STATE_AT completion required");
  var at=OffsetDateTime.parse(a.path("evaluatedAt").asText());require(!at.isAfter(OffsetDateTime.parse(f.path("clock").path("asOf").asText())),"Historical assessment must precede fixture clock");
  var segment=b.path("segments").get(0);require("Q".equals(segment.path("alias").asText())&&g.path("targetQuantity").equals(segment.path("quantity"))&&g.path("unit").equals(segment.path("unit"))&&g.path("placeAlias").equals(segment.path("locationAlias"))&&g.path("itemAlias").equals(segment.path("itemAlias"))&&g.path("lotAlias").equals(segment.path("lotAlias"))&&!OffsetDateTime.parse(segment.path("validFrom").asText()).isAfter(at),"Historical completion must match original physical Given");
 }
 static ObjectNode install(Connection c,JsonNode f,ObjectNode aliases)throws Exception {
  validate(f);var b=f.path("baseline");var x=b.path("historicalGoalCompletion");var authored=b.path("assessments").get(0);var work=b.path("works").get(0);
  String org=Json.required(aliases,"ORG"),wid=Json.required(aliases,"PARENT"),aid=Json.required(aliases,"OLDASSESS"),goalId=UUID.randomUUID().toString(),snapshotId=UUID.randomUUID().toString();
  var installed=time(f.path("clock").path("asOf").asText());var evaluated=time(authored.path("evaluatedAt").asText());
  String definitionId,definitionHash;JsonNode definition;
  try(var s=c.prepareStatement("SELECT ID,contentHash,content FROM mulino_definitions_DefinitionVersions WHERE organizationId=? AND version=? AND state='PUBLISHED'")){
   s.setString(1,org);s.setString(2,x.path("definitionVersion").asText());try(var r=s.executeQuery()){require(r.next(),"Published fixture definition missing");definitionId=r.getString(1);definitionHash=r.getString(2);definition=Json.parse(r.getString(3));require(!r.next()&&definitionHash.equals(Json.sha256Text(definition.toString())),"Published definition identity/hash ambiguous");}
  }
  JsonNode template=null;for(var g:definition.path("goals"))if("held-at".equals(g.path("name").asText())){require(template==null,"Ambiguous STATE_AT template");template=g;}
  require(template!=null&&template.equals(x.path("template"))&&Json.sha256Text(canonical(template)).equals(x.path("templateSha256").asText()),"Historical template differs from published fixture definition");
  var predicate=(ObjectNode)template.path("predicate").deepCopy();predicate.withObject("minimum").put("value","100");
  require(predicate.equals(x.path("instantiatedPredicate"))&&Json.sha256Text(canonical(predicate)).equals(x.path("predicateSha256").asText())&&template.path("endpoint").equals(x.path("goal").path("endpoint")),"Historical predicate parameterization/hash/endpoint differs");
  var slots=(ObjectNode)x.path("goal").deepCopy();for(String key:List.of("item","lot","place")){String alias=slots.remove(key+"Alias").asText();slots.put(key+"Id",Json.required(aliases,alias));}
  slots.put("historicalGoalLabel",x.path("originalGoalLabel").asText()).put("runtimeEvaluationClaimed",false);
  var scope=Json.object().put("itemId",Json.required(aliases,"P")).put("lotId",Json.required(aliases,"LOT")).put("placeId",Json.required(aliases,"W"));
  var goal=Json.object().put("organizationId",org).put("ID",goalId).put("workId",wid).put("slotsJson",slots.toString());
  insert(c,"mulino_work_read_GoalReferences",row("organizationId",org,"ID",goalId,"revision",1,"createdAt",installed,"recordedAt",installed,"effectiveAt",evaluated,"workId",wid,"definitionVersionId",definitionId,"quantityMode","STATE_AT","targetQuantity",new java.math.BigDecimal("100"),"unit","BOX","endpoint",slots.path("endpoint").asText(),"scopeJson",scope.toString(),"slotsJson",slots.toString(),"provenanceJson",x.toString(),"goalVersion",1));
  var conditions=Json.object().put("recordType","SYNTHETIC_HISTORICAL_GIVEN").put("runtimeEvaluationClaimed",false);conditions.set("authoredAssessment",authored);conditions.set("conditions",Json.array());
  var snapshot=Json.object().put("recordType","SYNTHETIC_HISTORICAL_GIVEN").put("runtimeEvaluationClaimed",false).put("organizationId",org).put("assessmentId",aid).put("workId",wid).put("goalId",goalId).put("definitionHash",definitionHash);
  snapshot.set("goal",goal);snapshot.set("goalSlots",slots);snapshot.set("conditions",conditions);snapshot.set("originalAssessment",authored);snapshot.set("originalWork",work);snapshot.set("syntheticCompletion",x);
  String bytes=snapshot.toString(),hash=Json.sha256Text(bytes);
  insert(c,"mulino_work_read_AssessmentReferences",row("organizationId",org,"ID",aid,"revision",1,"createdAt",installed,"recordedAt",installed,"effectiveAt",evaluated,"workId",wid,"goalId",goalId,"outcome",authored.path("result").asText(),"evaluatorVersion",authored.path("evaluatorVersion").asText(),"assessedAt",evaluated,"conditionsJson",conditions.toString(),"definitionVersionId",definitionId,"asOf",evaluated,"knownAt",installed,"inputSnapshotHash",hash));
  insert(c,"mulino_evaluation_InputSnapshots",row("organizationId",org,"ID",snapshotId,"assessmentId",aid,"workId",wid,"goalId",goalId,"recordedAt",installed,"asOf",evaluated,"knownAt",installed,"contentHash",hash,"contentJson",bytes));
  return Json.object().put("recordType","SYNTHETIC_HISTORICAL_GIVEN").put("historicalEvaluationExecuted",false).put("currentGoalPromoted",false).put("organizationId",org).put("workId",wid).put("assessmentId",aid).put("goalId",goalId).put("snapshotId",snapshotId).put("inputSnapshotHash",hash).put("definitionHash",definitionHash).put("recordedAt",installed.toInstant().toString());
 }
 static void require(boolean ok,String message){if(!ok)throw new UnsupportedOperationException(message);}
 static OffsetDateTime time(String value){return OffsetDateTime.parse(value);}
 static String canonical(JsonNode value)throws Exception {return Json.MAPPER.writeValueAsString(sorted(value));}
 private static Object sorted(JsonNode value){if(value.isObject()){var m=new TreeMap<String,Object>();value.fields().forEachRemaining(e->m.put(e.getKey(),sorted(e.getValue())));return m;}if(value.isArray()){var list=new ArrayList<Object>();value.forEach(v->list.add(sorted(v)));return list;}return Json.MAPPER.convertValue(value,Object.class);}
 static Map<String,Object> row(Object... values){var out=new LinkedHashMap<String,Object>();for(int i=0;i<values.length;i+=2)if(values[i+1]!=null)out.put(values[i].toString(),values[i+1]);return out;}
 static void insert(Connection c,String table,Map<String,Object> values)throws SQLException {String sql="INSERT INTO "+table+"("+String.join(",",values.keySet())+") VALUES("+String.join(",",Collections.nCopies(values.size(),"?"))+")";try(var s=c.prepareStatement(sql)){int i=1;for(var v:values.values())s.setObject(i++,v);if(s.executeUpdate()!=1)throw new SQLException("Expected one synthetic historical row");}}
}
