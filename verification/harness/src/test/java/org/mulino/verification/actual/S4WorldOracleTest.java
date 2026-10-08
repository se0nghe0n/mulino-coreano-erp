package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mulino.verification.Json;
import static org.junit.jupiter.api.Assertions.*;

/** Oracle semantics for s4-native-00/02/03: bound duties and ledger-backed world reads. */
final class S4WorldOracleTest {
 private final Path root=Path.of(System.getProperty("repo.root",".")).toAbsolutePath().normalize();
 static final String HUMAN="00000000-0000-0000-0000-00000000000a",AGENT="00000000-0000-0000-0000-00000000000b",WORK="00000000-0000-0000-0000-000000000001",SALES="00000000-0000-0000-0000-000000000002",SOURCE="00000000-0000-0000-0000-000000000003";
 static final S4WorldOracle.Require STRICT=(condition,message)->{if(!condition)throw new AssertionError(message);};
 static JsonNode json(String text){try{return Json.MAPPER.readTree(text.replace('\'','"'));}catch(Exception e){throw new IllegalStateException(e);}}
 static String duty(String id,String kind,String work,String quantity,String owner){return "{'id':'"+id+"','kind':'"+kind+"','status':'OPEN','valid':true,'workid':'"+work+"','quantity':"+(quantity==null?"null":"'"+quantity+"'")+",'ownerid':'"+owner+"','nextaction':'review','nextcheckat':'2026-10-08 18:00:00.0','scopejson':'{\\'domainSourceId\\':\\'"+SOURCE+"\\'}'}";}
 static JsonNode actors(){return json("[{'id':'"+HUMAN+"','kind':'HUMAN'},{'id':'"+AGENT+"','kind':'AGENT'}]");}
 static JsonNode qcSpec(){return json("[{'kind':'QUALITY_REVIEW','where':{'workid':'"+SALES+"','scope.domainSourceId':'"+SOURCE+"','quantity':'20'},'count':1}]");}

 /** RED on the kind-only oracle: a QUALITY_REVIEW of an unrelated Work no longer satisfies the bound duty. */
 @Test void unrelatedDutyOfTheSameKindDoesNotSatisfyABoundDuty(){
  var rows=json("["+duty("d1","QUALITY_REVIEW",WORK,"40",HUMAN)+"]");
  var failure=assertThrows(AssertionError.class,()->S4WorldOracle.humanDuties(STRICT,"c1",rows,qcSpec(),actors()));
  assertTrue(failure.getMessage().contains("expected 1 bound open human duty QUALITY_REVIEW"),failure.getMessage());
 }
 @Test void boundDutyPassesButAnExtraOrDuplicateOpenDutyOfThatKindFails(){
  S4WorldOracle.humanDuties(STRICT,"ok",json("["+duty("d1","QUALITY_REVIEW",SALES,"20.000",HUMAN)+"]"),qcSpec(),actors());
  var extra=json("["+duty("d1","QUALITY_REVIEW",SALES,"20",HUMAN)+","+duty("d2","QUALITY_REVIEW",WORK,"40",HUMAN)+"]");
  assertTrue(assertThrows(AssertionError.class,()->S4WorldOracle.humanDuties(STRICT,"extra",extra,qcSpec(),actors())).getMessage().contains("unbound or duplicate"));
  var duplicate=json("["+duty("d1","QUALITY_REVIEW",SALES,"20",HUMAN)+","+duty("d2","QUALITY_REVIEW",SALES,"20",HUMAN)+"]");
  assertThrows(AssertionError.class,()->S4WorldOracle.humanDuties(STRICT,"duplicate",duplicate,qcSpec(),actors()));
 }
 @Test void boundDutyNeedsAHumanOwnerAndNonEmptyBinding(){
  assertTrue(assertThrows(AssertionError.class,()->S4WorldOracle.humanDuties(STRICT,"agent",json("["+duty("d1","QUALITY_REVIEW",SALES,"20",AGENT)+"]"),qcSpec(),actors())).getMessage().contains("not a human"));
  assertThrows(AssertionError.class,()->S4WorldOracle.humanDuties(STRICT,"kindOnly",json("["+duty("d1","QUALITY_REVIEW",SALES,"20",HUMAN)+"]"),json("[{'kind':'QUALITY_REVIEW','count':1}]"),actors()));
 }
 /** API camelCase rows bind through the same spec (scope object instead of scopeJson text). */
 @Test void productObligationsBindThroughTheSameSpec(){
  var api=json("[{'ID':'d1','kind':'QUALITY_REVIEW','status':'OPEN','valid':true,'workId':'"+SALES+"','quantity':'20','ownerId':'"+HUMAN+"','nextAction':'review','nextCheckAt':'2026-10-08T09:00:00Z','scope':{'domainSourceId':'"+SOURCE+"'}}]");
  S4WorldOracle.humanDuties(STRICT,"api",api,qcSpec(),actors());
 }
 /** RED on the self-parity check: a world read that drops or alters a ledger duty fails against the ledger. */
 @Test void productRowsMustEqualTheLedgerFieldByField(){
  TimeZone saved=TimeZone.getDefault();TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
  try{
   var ledger=json("["+duty("d1","QUALITY_REVIEW",SALES,"20",HUMAN)+","+duty("d2","RETURN_QC_REVIEW",WORK,null,HUMAN)+","+duty("d3","OTHER","00000000-0000-0000-0000-000000000009",null,HUMAN)+"]");
   JsonNode where=json("{'workid':['"+WORK+"','"+SALES+"']}");
   String d1="{'ID':'d1','kind':'QUALITY_REVIEW','status':'OPEN','valid':true,'workId':'"+SALES+"','quantity':'20.000000000000','ownerId':'"+HUMAN+"','nextAction':'review','nextCheckAt':'2026-10-08T09:00:00Z','scope':{'domainSourceId':'"+SOURCE+"'}}";
   String d2="{'ID':'d2','kind':'RETURN_QC_REVIEW','status':'OPEN','valid':true,'workId':'"+WORK+"','quantity':null,'ownerId':'"+HUMAN+"','nextAction':'review','nextCheckAt':'2026-10-08T09:00:00Z','scope':{'domainSourceId':'"+SOURCE+"'}}";
   S4WorldOracle.ledgerRows(STRICT,"ok",json("["+d1+","+d2+"]"),ledger,where);
   assertTrue(assertThrows(AssertionError.class,()->S4WorldOracle.ledgerRows(STRICT,"empty",json("[]"),ledger,where)).getMessage().contains("missing from product"));
   assertThrows(AssertionError.class,()->S4WorldOracle.ledgerRows(STRICT,"owner",json("["+d1+","+d2.replace(HUMAN,AGENT)+"]"),ledger,where));
   assertThrows(AssertionError.class,()->S4WorldOracle.ledgerRows(STRICT,"time",json("["+d1+","+d2.replace("09:00:00Z","09:00:01Z")+"]"),ledger,where));
   assertThrows(AssertionError.class,()->S4WorldOracle.ledgerRows(STRICT,"invented",json("["+d1+","+d2.replace("'nextAction'","'invented'")+"]"),ledger,where));
  }finally{TimeZone.setDefault(saved);}
 }
 @Test void ownersComeFromLedgerWorksAndTheirOpenDuties(){
  var works=json("[{'id':'"+WORK+"','ownerid':'"+HUMAN+"'},{'id':'"+SALES+"','ownerid':'"+HUMAN+"'}]");
  var duties=json("["+duty("d1","QUALITY_REVIEW",SALES,"20",AGENT)+"]");JsonNode ids=json("['"+WORK+"','"+SALES+"']");
  S4WorldOracle.ledgerOwners(STRICT,"ok",json("['"+AGENT+"','"+HUMAN+"']"),works,duties,ids);
  assertThrows(AssertionError.class,()->S4WorldOracle.ledgerOwners(STRICT,"missing",json("['"+HUMAN+"']"),works,duties,ids));
 }
 /** Every committed humanDuties assertion binds each duty; no kind-only oracle remains. */
 @Test void committedFlowsBindEveryHumanDuty()throws Exception {
  int found=0;
  try(var files=Files.list(root.resolve(NativeS4FlowAliases.DIRECTORY))){for(Path f:files.filter(p->p.toString().endsWith(".json")).toList()){
   for(JsonNode action:Json.read(f).path("actions"))for(JsonNode a:action.path("assertions"))if("humanDuties".equals(a.path("operator").asText())){found++;
    assertFalse(a.has("kinds"),f+" "+action.path("id"));assertTrue(a.path("duties").size()>0,f.toString());
    for(JsonNode spec:a.path("duties")){assertTrue(spec.path("count").isInt(),f.toString());assertTrue(spec.path("where").has("workid"),f+" duty "+spec+" must bind its Work");}}}}
  assertTrue(found>=5,"E1 SQL/noun/verb, C1, C4 and E2 bind human duties: "+found);
 }
 /** E1 compares noun and verb answers with the independent ledger, not only with each other. */
 @Test void e1WorldReadsAreCheckedAgainstTheLedger()throws Exception {
  var actions=new HashMap<String,JsonNode>();for(JsonNode a:Json.read(root.resolve(NativeS4FlowAliases.DIRECTORY+"/e1-final.json")).path("actions"))actions.put(a.path("id").asText(),a);
  for(String id:List.of("e1-noun","e1-verb")){var operators=new HashSet<String>();for(JsonNode a:actions.get(id).path("assertions"))operators.add(a.path("pointer").asText()+" "+a.path("operator").asText());
   for(String required:List.of("/data/obligations ledgerRows","/data/obligations humanDuties","/data/ownerIds ledgerOwners","/data/workIds sameSet","/data/contributions ledgerRows"))assertTrue(operators.contains(required),id+" lacks "+required);}
  var verb=new HashSet<String>();for(JsonNode a:actions.get("e1-verb").path("assertions"))verb.add(a.path("pointer").asText()+"="+a.path("expected").asText());
  assertTrue(verb.contains("/data/ID=$WORK")&&verb.contains("/data/workId=$WORK"),verb.toString());
 }
 /** Every mapped contract key names committed actions that author at least one assertion (a dropped assertion fails here). */
 @Test void acceptanceContractKeysMapToAuthoredAssertions()throws Exception {
  var authored=new HashMap<String,Integer>();
  try(var files=Files.list(root.resolve(NativeS4FlowAliases.DIRECTORY))){for(Path f:files.filter(p->p.toString().endsWith(".json")&&!p.getFileName().toString().equals("acceptance-contract.json")).toList())for(JsonNode a:Json.read(f).path("actions"))if(a.has("id"))authored.merge(a.path("id").asText(),a.path("assertions").size(),Math::max);}
  var contract=Json.read(root.resolve(NativeS4FlowAliases.DIRECTORY+"/acceptance-contract.json"));var map=contract.path("assertionMap");int mapped=0;
  for(var cases=contract.path("cases").fields();cases.hasNext();){var c=cases.next();assertTrue(map.has(c.getKey()),"unmapped case "+c.getKey());
   for(var keys=c.getValue().fieldNames();keys.hasNext();){String key=keys.next();assertTrue(map.path(c.getKey()).has(key),c.getKey()+"."+key+" has no assertion map entry");
    for(JsonNode action:map.path(c.getKey()).path(key)){mapped++;assertTrue(authored.getOrDefault(action.asText(),0)>0,c.getKey()+"."+key+" maps to "+action+" without authored assertions");}}}
  for(String key:List.of("unresolvedHumanDuties","entrypointParity","bankEffects"))assertTrue(map.path("E1").path(key).size()>0,"E1."+key);
  assertTrue(mapped>=30,"mapped actions "+mapped);
 }
}
