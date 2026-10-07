package com.mulino.application.evidence;

import static org.junit.jupiter.api.Assertions.*;
import com.mulino.application.core.*;
import com.mulino.application.identity.IdentityAuthorization;
import com.mulino.adapters.blob.LocalBlobStore;
import com.mulino.domain.evidence.*;
import com.sap.cds.services.runtime.CdsRuntime;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static com.mulino.domain.evidence.EvidenceTypes.*;
import static com.mulino.application.evidence.EvidenceRecords.*;

@SpringBootTest
@ActiveProfiles("local")
abstract class RecallGatewayFixture {
  static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280");
  static final Path BLOBS;
  static { try { BLOBS=Files.createTempDirectory("mulino-receipt-test-"); }catch(Exception e){throw new IllegalStateException(e);} PG.start(); }
  static final String ORG=uuid(),ACTOR=uuid(),OWNER=uuid(),OTHER=uuid(),ITEM=uuid(),SOURCE=uuid(),GRANT=uuid();
  static final Instant OCCURRED=Instant.parse("2026-10-07T09:00:00Z");
  @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url",PG::getJdbcUrl);r.add("spring.datasource.username",PG::getUsername);r.add("spring.datasource.password",PG::getPassword);
    r.add("JWT_PUBLIC_KEY",()->System.getenv("JWT_PUBLIC_KEY"));r.add("JWT_ISSUER",()->"https://fixture.invalid");r.add("JWT_AUDIENCE",()->"mulino-acceptance");
    r.add("mulino.evidence.blob-root",BLOBS::toString);
  }
  @Autowired JdbcTemplate jdbc; @Autowired IdentityAuthorization auth; @Autowired EvidenceRecords records;
  @Autowired EvidenceQueries queries; @Autowired EvidenceRepository repository; @Autowired LocalBlobStore blobs;
  @Autowired CdsRuntime runtime; @Autowired PlatformTransactionManager tx;
  static String uuid(){return UUID.randomUUID().toString();}
  void seedBase() {
    for(String directory:List.of("objects","staging"))try(var files=Files.list(BLOBS.resolve(directory))){for(Path file:files.toList())Files.delete(file);}catch(Exception e){throw new IllegalStateException(e);}
    jdbc.execute("TRUNCATE mulino_evidence_SourceProfiles, mulino_inventory_TradeItems,mulino_identity_Organizations CASCADE");
    jdbc.update("INSERT INTO mulino_identity_Organizations(ID,externalAlias) VALUES(?,'ORG-A'),(?,'ORG-B')",ORG,OTHER);
    jdbc.update("INSERT INTO mulino_identity_Actors(organizationId,ID,kind,stableRequestOwner) VALUES(?,?,'HUMAN',?)",ORG,ACTOR,OWNER);
    jdbc.update("INSERT INTO mulino_identity_ExternalIdentities(organizationId,ID,actorId,issuer,subject,organizationAlias) VALUES(?,?,?,'https://fixture.invalid','recorder','ORG-A')",ORG,uuid(),ACTOR);
    jdbc.update("INSERT INTO mulino_identity_Memberships(organizationId,ID,actorId,validFrom,validUntil) VALUES(?,?,?,CURRENT_TIMESTAMP-INTERVAL '1 day',CURRENT_TIMESTAMP+INTERVAL '1 day')",ORG,uuid(),ACTOR);
    jdbc.update("INSERT INTO mulino_identity_Grants(organizationId,ID,actorId,delegatorId,validFrom,validUntil) VALUES(?,?,?,?,CURRENT_TIMESTAMP-INTERVAL '1 day',CURRENT_TIMESTAMP+INTERVAL '1 day')",ORG,GRANT,ACTOR,ACTOR);
    for(String cap:List.of("getEvidence","getInbox","attachEvidence","recordActivity","correctEvidence","linkCanonicalOccurrence")) {
      jdbc.update("INSERT INTO mulino_identity_CapabilityAssignments(organizationId,ID,actorId,capabilityId,scopeKind,scopeId,validFrom,validUntil) VALUES(?,?,?,?,'ORGANIZATION',?,CURRENT_TIMESTAMP-INTERVAL '1 day',CURRENT_TIMESTAMP+INTERVAL '1 day')",ORG,uuid(),ACTOR,cap,ORG);
      jdbc.update("INSERT INTO mulino_identity_GrantActions VALUES(?,?,?)",ORG,GRANT,cap);
    }
    jdbc.update("INSERT INTO mulino_identity_GrantScopes VALUES(?,?,'ORGANIZATION',?)",ORG,GRANT,ORG);
    jdbc.update("INSERT INTO mulino_identity_AuthorityFences VALUES(?,?,1)",ORG,ACTOR);
    String product=uuid(),spec=uuid(),pack=uuid();
    jdbc.update("INSERT INTO mulino_inventory_Products(organizationId,ID,name) VALUES(?,?,'Synthetic product')",ORG,product);
    for(String table:List.of("SpecificationVersions","PackagingVersions"))jdbc.update("INSERT INTO mulino_inventory_"+table+"(organizationId,ID,productId,version,contentHash) VALUES(?,?,?,'1',?)",ORG,table.startsWith("Spec")?spec:pack,product,"a".repeat(64));
    jdbc.update("INSERT INTO mulino_inventory_TradeItems(organizationId,ID,productId,name,baseUnit,decimalPlaces,specificationVersionId,packagingVersionId) VALUES(?,?,?,'Synthetic biscuit','BOX',0,?,?)",ORG,ITEM,product,spec,pack);
    jdbc.update("INSERT INTO mulino_evidence_SourceProfiles(ID,organizationId,revision,recordedAt,recordedBy,namespace,policyVersion,intakeOwnerId,supervisorId,nextAction,nextCheckAt) VALUES(?,?,1,CURRENT_TIMESTAMP,?,'warehouse','synthetic-v1',?,?,'Compare source',CURRENT_TIMESTAMP+INTERVAL '1 hour')",SOURCE,ORG,ACTOR,ACTOR,ACTOR);
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("local-test-context").header("alg","RS256").issuer("https://fixture.invalid").subject("recorder").claim("organizationId","ORG-A").build(),List.of()));
  }
  @AfterEach void clear(){SecurityContextHolder.clearContext();}
  <T>T request(Callable<T> work){return runtime.requestContext().run(ctx->{try{return work.call();}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}});}
  DocumentInput document(String prior){return new DocumentInput(new Subject(SubjectKind.ITEM,ITEM),"warehouse","source-reference","text/plain",LocalBlobStore.hash("original".getBytes()),"SYNTHETIC fixture",prior);}
  String document(){return request(()->records.attachDocument(document(null),"original".getBytes())).get("id").toString();}
  EventInput event(String sourceId,String version,String payload,String prior){return new EventInput(new Subject(SubjectKind.ITEM,ITEM),"RECEIPT","warehouse",sourceId,version,new EffectiveTime(OCCURRED,null,"Asia/Seoul","SECOND"),ValueState.KNOWN,payload,prior,null);}
  String event(){return request(()->records.recordActivity(event("R60","1","receipt60",null))).get("id").toString();}
  String claim(String event,String doc,String quantity){return request(()->records.recordClaim(new ClaimInput(event,doc,"Receipt reported",quantity,"BOX",ValueState.KNOWN,null))).get("id").toString();}
  QueryResult read(String kind,String id,Instant knownAt){return request(()->queries.query(auth.context(OCCURRED.plusSeconds(100),knownAt),new QueryRequest("getEvidence",id,Map.of("kind",kind),Map.of(),50,null,null,OCCURRED.plusSeconds(100),knownAt,null)));}

 @Autowired ApplicationCommands gateway;
 @Autowired EvidenceReconciliation reconciliation;
 @Autowired com.mulino.application.trade.receipt.ReceiptCommands receiptCommands;
 @Autowired com.mulino.domain.inventory.ReceiptStockPrimitives physical;
 @Autowired com.mulino.application.trade.receipt.ReceiptQueries receiptQueries;
 @org.springframework.test.context.bean.override.mockito.MockitoBean com.mulino.application.policy.PolicyCommandGuard localPolicy;
 String purchaseLine;
 final String WORK=uuid(),LOT=uuid(),PLACE=uuid(),TRANSIT=uuid();
 @BeforeEach void receiptFixture() throws Exception {
  seedBase();
  purchaseLine=null;
  for(String cap:List.of("receiveProvisional","confirmReceipt","matchSourceIdentity","getReceipt","getReceipts","getInventory","getObject","searchObjects","assessGoal")){
   jdbc.update("INSERT INTO mulino_identity_CapabilityAssignments(organizationId,ID,actorId,capabilityId,scopeKind,scopeId,validFrom,validUntil) VALUES(?,?,?,?,'ORGANIZATION',?,CURRENT_TIMESTAMP-INTERVAL '1 day',CURRENT_TIMESTAMP+INTERVAL '1 day')",ORG,uuid(),ACTOR,cap,ORG);
   jdbc.update("INSERT INTO mulino_identity_GrantActions VALUES(?,?,?)",ORG,GRANT,cap);
  }
  var capabilities=List.of("receiveProvisional","confirmReceipt","assessGoal").stream().map(cap->new com.mulino.domain.definitions.Definition.Capability(cap,"1.0.0","core-v1","1.0.0","1.0.0",List.<String>of())).toList();
  var d=new com.mulino.domain.definitions.Definition(ORG,uuid(),"receipt-v1",null,"PUBLISHED",null,"core-v1","1.0.0",List.of(new com.mulino.domain.definitions.Definition.NounType("Work",true),new com.mulino.domain.definitions.Definition.NounType("TradeItem",true),new com.mulino.domain.definitions.Definition.NounType("Receipt",true),new com.mulino.domain.definitions.Definition.NounType("Goal",true)),List.of(new com.mulino.domain.definitions.Definition.Attribute("Receipt","quantity",com.mulino.domain.definitions.Definition.ValueType.DECIMAL,null,"BOX",0,1,1,"READ",true)),List.of(new com.mulino.domain.definitions.Definition.Verb("receive","RECORD","receiveProvisional","ACTIVE",Map.of()),new com.mulino.domain.definitions.Definition.Verb("confirm","COMMAND","confirmReceipt","ACTIVE",Map.of()),new com.mulino.domain.definitions.Definition.Verb("assess","COMMAND","assessGoal","ACTIVE",Map.of())),List.of(),List.of(new com.mulino.domain.definitions.Definition.Goal("Arrival","CUMULATIVE_EVENT","ARRIVED","core-v1",Map.of("operator","quantitySum","property","Receipt.quantity","minimum",Map.of("value","1","unit","BOX"),"unit","BOX","evidenceSelector","VERIFIED_DISTINCT"))),capabilities);
  String body=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(d);
  jdbc.update("INSERT INTO mulino_definitions_DefinitionVersions(organizationId,ID,createdAt,version,state,contentHash,content,evaluatorVersion,schemaVersion) VALUES (?,?,CURRENT_TIMESTAMP,?,'PUBLISHED',?,?,?,?)",ORG,d.id(),d.version(),com.mulino.domain.definitions.DefinitionRepository.sha256(body),body,"core-v1","1.0.0");
  jdbc.update("INSERT INTO mulino_work_read_Works(organizationId,ID,createdAt,recordedAt,effectiveAt,itemId,definitionVersionId,kind,status,ownerId,supervisorId,lifecycleMode) VALUES(?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,?,?,'RECEIPT','ACTIVE',?,?,'COMMAND')",ORG,WORK,ITEM,d.id(),ACTOR,ACTOR);
  String goal=uuid();var goalSlots=new LinkedHashMap<String,Object>();goalSlots.putAll(Map.of("quantityMode","CUMULATIVE_EVENT","targetQuantity","100","unit","BOX","endpoint","ARRIVED","scope",Map.of("workId",WORK),"timezone","UTC","evidencePolicyVersion","synthetic-v1","periodStart",OCCURRED.minusSeconds(1).toString(),"periodEnd",Instant.now().plusSeconds(7200).toString(),"eventKind","PHYSICAL_RECEIPT"));goalSlots.putAll(Map.of("contributionScope","DIRECT","deduplication","PHYSICAL_SCOPE","conditions",List.of(Map.of("id","Arrival")),"evaluatorVersion","core-v1","dueAt",Instant.now().plusSeconds(3600).toString()));
  jdbc.update("INSERT INTO mulino_work_read_GoalReferences(organizationId,ID,createdAt,recordedAt,effectiveAt,workId,definitionVersionId,quantityMode,targetQuantity,unit,endpoint,scopeJson,slotsJson) VALUES(?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,?,?,'CUMULATIVE_EVENT',100,'BOX','ARRIVED','{}',?)",ORG,goal,WORK,d.id(),new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(goalSlots));jdbc.update("UPDATE mulino_work_read_Works SET currentGoalVersionId=? WHERE organizationId=? AND ID=?",goal,ORG,WORK);
  String policy=uuid();jdbc.update("INSERT INTO mulino_governance_PolicyVersions(organizationId,ID,version,kind,content,contentHash,createdAt,effectiveFrom) VALUES(?,?,'synthetic-v1','EVIDENCE','{}',?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",ORG,policy,com.mulino.domain.definitions.DefinitionRepository.sha256("{}"));jdbc.update("INSERT INTO mulino_governance_ActivePolicies(organizationId,kind,policyId,revision) VALUES(?,'EVIDENCE',?,0)",ORG,policy);
  String maker=uuid();jdbc.update("INSERT INTO mulino_inventory_Manufacturers(organizationId,ID,name) VALUES(?,?,'synthetic maker')",ORG,maker);
  jdbc.update("INSERT INTO mulino_inventory_ManufacturingLots(organizationId,ID,manufacturerId,itemId,originalLot) VALUES(?,?,?,?,'L1')",ORG,LOT,maker,ITEM);
  jdbc.update("INSERT INTO mulino_inventory_Places(organizationId,ID,name,kind) VALUES(?,?,'warehouse','INTERNAL_STORAGE'),(?,?,'transit','TRANSIT')",ORG,PLACE,ORG,TRANSIT);
 }
 Map<String,Object> command(String cap,String key,Map<String,Object> slots,Integer revision){var m=new LinkedHashMap<String,Object>();m.put("intentKind",cap.equals("receiveProvisional")?"RECORD":"COMMAND");m.put("definitionVersion","receipt-v1");m.put("capabilityId",cap);m.put("commandIdempotencyKey",key);m.put("subjectRefs",List.of(Map.of("type","Work","id",WORK)));m.put("slots",slots);m.put("provenance",Map.of("sourceNamespace","USER"));if(revision!=null)m.put("expectedRevision",revision);return m;}
 record Input(String root,String event,String claim,String document,String provisional,String quantity){}
 Input provisional(String root,String quantity,boolean known,String transit){
  var payload=new LinkedHashMap<String,Object>();payload.putAll(Map.of("rangeRootId",root,"startQuantity","0","itemId",ITEM,"lotId",LOT,"placeId",PLACE,"workId",WORK,"quantity",quantity,"unit","BOX","occurredAt",OCCURRED.toString()));
  String json;try{json=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);}catch(Exception e){throw new IllegalStateException(e);}
  String doc=request(()->records.attachDocument(new DocumentInput(new Subject(SubjectKind.WORK,WORK),"warehouse",uuid(),"application/json",LocalBlobStore.hash(json.getBytes()),"SYNTHETIC",null),json.getBytes())).get("id").toString();
  String event=request(()->records.recordActivity(new EventInput(new Subject(SubjectKind.WORK,WORK),"PHYSICAL_RECEIPT","warehouse",uuid(),"1",new EffectiveTime(OCCURRED,null,"Asia/Seoul","SECOND"),ValueState.KNOWN,json,null,null))).get("id").toString();
  String claim=request(()->records.recordClaim(new ClaimInput(event,doc,"Receipt source",quantity,"BOX",ValueState.KNOWN,null))).get("id").toString();
  var slots=new LinkedHashMap<String,Object>();slots.putAll(payload);slots.put("eventId",event);slots.put("ownerId",ACTOR);slots.put("supervisorId",ACTOR);slots.put("nextAction","Reconcile receipt");slots.put("nextCheckAt",Instant.now().plusSeconds(3600).toString());if(!known)slots.remove("lotId");if(transit!=null)slots.put("transitSegmentId",transit);if(purchaseLine!=null)slots.put("purchaseLineId",purchaseLine);
  var result=request(()->gateway.execute(command("receiveProvisional",uuid(),slots,null)));assertEquals("APPLIED",result.get("outcome"),result.toString());String provisional=((Map<?,?>)result.get("effects")).get("receiptId").toString();return new Input(root,event,claim,doc,provisional,quantity);
 }
 String canonical(Input input,String prior){return request(()->new TransactionTemplate(tx).execute(status->{var event=repository.require("Events",ORG,input.event());var c=auth.context(Instant.now(),Instant.now());var matched=reconciliation.match(c,new EvidenceReconciliation.Review(input.claim(),input.document(),input.root(),prior,"synthetic-v1",event.get("sourceNamespace")+":"+event.get("externalEventId")+":"+event.get("sourceVersion"),input.quantity(),"BOX",OCCURRED,"Original receipt checked"),"matchSourceIdentity");assertEquals("MATCHED",matched.get("outcome"),matched.toString());return reconciliation.link(c,matched.get("id").toString()).get("id").toString();}));}
 Map<String,Object> confirm(Input input,String occurrence,String key){return request(()->gateway.execute(command("confirmReceipt",key,Map.of("receiptId",input.provisional(),"canonicalOccurrenceId",occurrence,"lotId",LOT),0)));}
 int count(String table){return jdbc.queryForObject("SELECT count(*) FROM "+table,Integer.class);}

 void ordered100Fixture(){String proposal=uuid(),approval=uuid(),order=uuid();purchaseLine=uuid();
  jdbc.update("INSERT INTO mulino_trade_purchase_Proposals(organizationId,ID,workId,currentRevision,status,createdAt) VALUES(?,?,?,1,'ORDERED',CURRENT_TIMESTAMP)",ORG,proposal,WORK);
  jdbc.update("INSERT INTO mulino_trade_purchase_Approvals(organizationId,ID,proposalId,proposalRevision,proposalHash,approverId,policyHash,decision,conditionsJson,decidedAt,validUntil) VALUES(?,?,?,1,?, ?,?,'APPROVE','{}',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP+INTERVAL '1 day')",ORG,approval,proposal,"a".repeat(64),ACTOR,"b".repeat(64));
  jdbc.update("INSERT INTO mulino_trade_purchase_Orders(organizationId,ID,proposalId,proposalRevision,proposalHash,approvalId,channel,externalOperationId,outboxId,createdAt) VALUES(?,?,?,1,?,?,'SYNTHETIC',?,?,CURRENT_TIMESTAMP)",ORG,order,proposal,"a".repeat(64),approval,uuid(),uuid());
  jdbc.update("INSERT INTO mulino_trade_purchase_OrderLines(organizationId,ID,orderId,proposalId,proposalRevision,workId,itemId,destinationId,quantity,unit,status,revision) VALUES(?,?,?,?,1,?,?,?,100,'BOX','SUPPLIER_ACCEPTED',1)",ORG,purchaseLine,order,proposal,WORK,ITEM,PLACE);
 }

}
