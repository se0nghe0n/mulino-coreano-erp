package com.mulino.application.evidence;
import com.mulino.application.core.*;
import com.mulino.application.policy.PolicyCommandGuard;
import com.mulino.domain.definitions.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.junit.jupiter.api.Assertions.*;
/** Real command gateway, PostgreSQL, identity scope and file store; policy permission is a local fixture. */
class EvidenceGatewayTest extends EvidencePersistenceTest {
 @Autowired ApplicationCommands gateway;
 @MockitoBean PolicyCommandGuard policyFixture;
 @BeforeEach void definition()throws Exception {
  var caps=List.of("recordActivity","attachEvidence","correctEvidence","matchSourceIdentity","linkCanonicalOccurrence","resolveEvidenceConflict").stream().map(cap->new Definition.Capability(cap,"1.0.0","core-v1","1.0.0","1.0.0",List.<String>of())).toList();
  var d=new Definition(ORG,uuid(),"evidence-gateway-v1",null,"PUBLISHED",null,"core-v1","1.0.0",List.of(new Definition.NounType("TradeItem",true),new Definition.NounType("Work",true)),List.of(),List.of(new Definition.Verb("record","RECORD","recordActivity","ACTIVE",Map.of()),new Definition.Verb("attach","RECORD","attachEvidence","ACTIVE",Map.of()),new Definition.Verb("correct","RECORD","correctEvidence","ACTIVE",Map.of()),new Definition.Verb("match","COMMAND","matchSourceIdentity","ACTIVE",Map.of()),new Definition.Verb("link","COMMAND","linkCanonicalOccurrence","ACTIVE",Map.of())),List.of(),List.of(),caps);
  String body=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(d);
  jdbc.update("INSERT INTO mulino_definitions_DefinitionVersions(organizationId,ID,createdAt,version,state,contentHash,content,evaluatorVersion,schemaVersion) VALUES (?,?,CURRENT_TIMESTAMP,?,'PUBLISHED',?,?,?,?)",ORG,d.id(),d.version(),DefinitionRepository.sha256(body),body,"core-v1","1.0.0");
 }
 Map<String,Object> envelope(String cap,String key,Map<String,Object> slots,Integer revision){var input=new LinkedHashMap<String,Object>();input.put("intentKind","RECORD");input.put("definitionVersion","evidence-gateway-v1");input.put("capabilityId",cap);input.put("commandIdempotencyKey",key);input.put("subjectRefs",List.of(Map.of("type","TradeItem","id",ITEM)));input.put("slots",slots);input.put("provenance",Map.of("sourceNamespace","USER"));if(revision!=null)input.put("expectedRevision",revision);return input;}
 Map<String,Object> slots(String payload){var slots=new LinkedHashMap<String,Object>();slots.put("subject",Map.of("kind","ITEM","id",ITEM));slots.put("kind","RECEIPT");slots.put("sourceNamespace","warehouse");slots.put("externalEventId","native60");slots.put("sourceVersion","1");slots.put("effectiveFrom",OCCURRED.toString());slots.put("timeZone","Asia/Seoul");slots.put("timePrecision","SECOND");slots.put("valueState","KNOWN");slots.put("payload",payload);return slots;}
 Map<String,Object> execute(Map<String,Object> input){return request(()->gateway.execute(input));}
 @Test void actualGatewayRecordReplayAndConflictingHashPersistIntakeWithZeroStock(){
  var request=envelope("recordActivity","record60",slots("actual60"),null);var first=execute(request);
  assertEquals("APPLIED",first.get("outcome"));assertEquals("RECORDED",first.get("evidenceStatus"));assertEquals(first,execute(request));
  var conflict=execute(envelope("recordActivity","conflict58",slots("actual58"),null));assertEquals("APPLIED",conflict.get("outcome"));assertEquals("EVIDENCE_CONFLICT",conflict.get("evidenceStatus"));
  assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_Events",Integer.class));assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_InboxRecords",Integer.class));
  assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_InboxRecords WHERE intakeOwnerId=? AND supervisorId=? AND nextAction IS NOT NULL AND nextCheckAt IS NOT NULL",Integer.class,ACTOR,ACTOR));
  assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM mulino_inventory_QuantityMovements",Integer.class));assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM mulino_commands_CommandRecords WHERE state='COMMITTED' AND resultJson::jsonb->>'outcome'='APPLIED'",Integer.class));
 }
 @Test void actualGatewayAttachmentAndCorrectionAreCommittedWithImmutableOriginal(){
  var doc=new LinkedHashMap<String,Object>();doc.put("subject",Map.of("kind","ITEM","id",ITEM));doc.put("sourceNamespace","warehouse");doc.put("sourceReference","original.json");doc.put("mediaType","text/plain");doc.put("expectedHash",com.mulino.adapters.blob.LocalBlobStore.hash("original".getBytes()));doc.put("provenance","SYNTHETIC");doc.put("contentBase64",Base64.getEncoder().encodeToString("original".getBytes()));
  var attached=execute(envelope("attachEvidence","original",doc,null));assertEquals("APPLIED",attached.get("outcome"));assertArrayEquals("original".getBytes(),request(()->queries.download(attached.get("id").toString())).bytes());
  var first=execute(envelope("recordActivity","first",slots("actual60"),null));var corrected=slots("actual58");corrected.put("sourceVersion","2");corrected.put("supersedesId",first.get("id"));
  var correction=execute(envelope("correctEvidence","correction",corrected,1));assertEquals("APPLIED",correction.get("outcome"));assertEquals("RECORDED",correction.get("evidenceStatus"));
  assertEquals("actual60",jdbc.queryForObject("SELECT payload FROM mulino_evidence_Events WHERE ID=?",String.class,first.get("id")));assertEquals(first.get("id"),jdbc.queryForObject("SELECT supersedesId FROM mulino_evidence_Events WHERE ID=?",String.class,correction.get("id")));
  assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM mulino_inventory_QuantityMovements",Integer.class));assertEquals(3,jdbc.queryForObject("SELECT count(*) FROM mulino_commands_CommandAudits",Integer.class));
 }
}
