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
class ReturnGatewayPostgresTest {
  static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280");
  static final Path BLOBS;
  static { try { BLOBS=Files.createTempDirectory("mulino-return-test-"); }catch(Exception e){throw new IllegalStateException(e);} PG.start(); }
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
 @Autowired ApplicationCommands gateway;
 @Autowired EvidenceReconciliation reconciliation;



 @org.springframework.test.context.bean.override.mockito.MockitoBean com.mulino.application.policy.PolicyCommandGuard localPolicy;
 String purchaseLine;
 final String WORK=uuid(),LOT=uuid(),PLACE=uuid(),TRANSIT=uuid();
 @BeforeEach void receiptFixture() throws Exception {
  seedBase();
  purchaseLine=null;
  for(String cap:List.of("authorizeReturn","receiveReturn","decideReturnDisposition","matchSourceIdentity","getReceipt","getReceipts","getInventory","getObject","searchObjects","assessGoal")){
   jdbc.update("INSERT INTO mulino_identity_CapabilityAssignments(organizationId,ID,actorId,capabilityId,scopeKind,scopeId,validFrom,validUntil) VALUES(?,?,?,?,'ORGANIZATION',?,CURRENT_TIMESTAMP-INTERVAL '1 day',CURRENT_TIMESTAMP+INTERVAL '1 day')",ORG,uuid(),ACTOR,cap,ORG);
   jdbc.update("INSERT INTO mulino_identity_GrantActions VALUES(?,?,?)",ORG,GRANT,cap);
  }
  var capabilities=List.of("authorizeReturn","receiveReturn","decideReturnDisposition","assessGoal").stream().map(cap->new com.mulino.domain.definitions.Definition.Capability(cap,"1.0.0","core-v1","1.0.0","1.0.0",List.<String>of())).toList();
  var d=new com.mulino.domain.definitions.Definition(ORG,uuid(),"return-v1",null,"PUBLISHED",null,"core-v1","1.0.0",List.of(new com.mulino.domain.definitions.Definition.NounType("Work",true),new com.mulino.domain.definitions.Definition.NounType("TradeItem",true),new com.mulino.domain.definitions.Definition.NounType("Receipt",true),new com.mulino.domain.definitions.Definition.NounType("Goal",true)),List.of(new com.mulino.domain.definitions.Definition.Attribute("Receipt","quantity",com.mulino.domain.definitions.Definition.ValueType.DECIMAL,null,"BOX",0,1,1,"READ",true)),List.of(new com.mulino.domain.definitions.Definition.Verb("authorize","COMMAND","authorizeReturn","ACTIVE",Map.of()),new com.mulino.domain.definitions.Definition.Verb("receive","RECORD","receiveReturn","ACTIVE",Map.of()),new com.mulino.domain.definitions.Definition.Verb("decide","COMMAND","decideReturnDisposition","ACTIVE",Map.of()),new com.mulino.domain.definitions.Definition.Verb("assess","COMMAND","assessGoal","ACTIVE",Map.of())),List.of(),List.of(new com.mulino.domain.definitions.Definition.Goal("Arrival","CUMULATIVE_EVENT","ARRIVED","core-v1",Map.of("operator","quantitySum","property","Receipt.quantity","minimum",Map.of("value","1","unit","BOX"),"unit","BOX","evidenceSelector","VERIFIED_DISTINCT"))),capabilities);
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
 @Autowired com.mulino.application.trade.returns.ReturnCommands returns;
 final String CUSTOMER=uuid(),CUSTOMER_PLACE=uuid(),DELIVERY=uuid(),CUSTOMER_STOCK=uuid(),ROOT=uuid(),DISPATCH=uuid(),CARGO=uuid(),LINE=uuid();
 @BeforeEach void returnFixture(){
  jdbc.update("INSERT INTO mulino_trade_sales_Customers(organizationId,ID,revision,createdAt,recordedAt,effectiveAt,name) VALUES(?,?,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'customer')",ORG,CUSTOMER);
  jdbc.update("INSERT INTO mulino_inventory_Places(organizationId,ID,name,kind) VALUES(?,?,'customer','CUSTOMER')",ORG,CUSTOMER_PLACE);
  String order=uuid(),revision=uuid(),allocation=uuid(),observation=uuid(),canonical=uuid();
  String priorEvent=request(()->records.recordActivity(new EventInput(new Subject(SubjectKind.WORK,WORK),"PHYSICAL_DELIVERY","warehouse",uuid(),"1",new EffectiveTime(OCCURRED.minusSeconds(60),null,"UTC","SECOND"),ValueState.KNOWN,"{}",null,null))).get("id").toString();
  new TransactionTemplate(tx).execute(status->{
   insert("mulino_inventory_QuantitySegments",CUSTOMER_STOCK,"itemId",ITEM,"lotId",LOT,"identificationStatus","CONFIRMED","quantity",new java.math.BigDecimal("30"),"unit","BOX","placeId",CUSTOMER_PLACE,"ownerId",ACTOR,"custodianId",ACTOR,"controlScope","return-physical","validFrom",OCCURRED.minusSeconds(600),"mixtureStatus","IDENTIFIED");
   insert("mulino_inventory_QuantitySegments",ROOT,"itemId",ITEM,"lotId",LOT,"identificationStatus","CONFIRMED","quantity",new java.math.BigDecimal("30"),"unit","BOX","placeId",TRANSIT,"ownerId",ACTOR,"custodianId",ACTOR,"controlScope","return-physical","validFrom",OCCURRED.minusSeconds(1200),"mixtureStatus","IDENTIFIED","retiredAt",OCCURRED.minusSeconds(600),"retirementRecordedAt",Instant.now());
   insert("mulino_inventory_GenealogyEdges",uuid(),"sourceId",ROOT,"targetId",CUSTOMER_STOCK,"quantity",new java.math.BigDecimal("30"),"unit","BOX","kind","DELIVERY","uncertain",false,"occurredAt",OCCURRED.minusSeconds(600),"sourceStartQuantity",java.math.BigDecimal.ZERO,"targetStartQuantity",java.math.BigDecimal.ZERO);
   insert("mulino_trade_sales_Orders",order,"effectiveAt",Instant.now(),"workId",WORK,"currentRevision",1);
   insert("mulino_trade_sales_OrderRevisions",revision,"effectiveAt",Instant.now(),"orderId",order,"workId",WORK,"customerId",CUSTOMER,"revisionNumber",1,"reason","Explicit prior delivery precondition");
   insert("mulino_trade_sales_OrderLines",LINE,"effectiveAt",Instant.now(),"orderId",order,"revisionId",revision,"workId",WORK,"customerId",CUSTOMER,"itemId",ITEM,"quantity",new java.math.BigDecimal("30"),"unit","BOX","price",java.math.BigDecimal.ONE,"currency","EUR","dueAt",Instant.now().plusSeconds(3600),"destinationId",CUSTOMER_PLACE,"deliveryEndpoint","DELIVERED","qualityTerms","SYNTHETIC","packageTerms","SYNTHETIC");
   insert("mulino_inventory_SegmentAllocations",allocation,"rootId",allocation,"segmentId",ROOT,"orderLineId",LINE,"quantity",new java.math.BigDecimal("30"),"unit","BOX","state","CONSUMED","commandId",uuid());
   insert("mulino_inventory_Dispatches",DISPATCH,"cargoScopeId",CARGO,"allocationId",allocation,"salesLineId",LINE,"customerId",CUSTOMER,"workId",WORK,"itemId",ITEM,"lotId",LOT,"rangeRootId",ROOT,"startQuantity",java.math.BigDecimal.ZERO,"quantity",new java.math.BigDecimal("30"),"unit","BOX","transitSegmentId",ROOT,"destinationId",CUSTOMER_PLACE,"occurredAt",OCCURRED.minusSeconds(600),"commandId",uuid(),"evidenceRef",priorEvent);
   insert("mulino_inventory_CargoScopes",CARGO,"dispatchId",DISPATCH,"rangeRootId",ROOT,"startQuantity",java.math.BigDecimal.ZERO,"quantity",new java.math.BigDecimal("30"),"unit","BOX","segmentId",ROOT,"customerId",CUSTOMER,"workId",WORK,"commandId",uuid());
   insert("mulino_trade_sales_Observations",observation,"effectiveAt",Instant.now(),"eventId",priorEvent,"dispatchId",DISPATCH,"cargoScopeId",CARGO,"salesLineId",LINE,"workId",WORK,"customerId",CUSTOMER,"itemId",ITEM,"lotId",LOT,"rangeRootId",ROOT,"startQuantity",java.math.BigDecimal.ZERO,"quantity",new java.math.BigDecimal("30"),"unit","BOX","placeId",CUSTOMER_PLACE,"occurredAt",OCCURRED.minusSeconds(60),"state","CONFIRMED","nextCheckAt",Instant.now().plusSeconds(3600),"nextAction","Fixture predecessor");
   // Prior actual delivery is the test precondition; no VERIFIED decision is seeded.
   insert("mulino_evidence_CanonicalOccurrences",canonical,"recordedBy",ACTOR,"subjectKind","WORK","subjectId",WORK,"itemId",ITEM,"placeId",CUSTOMER_PLACE,"workId",WORK,"effectiveFrom",OCCURRED.minusSeconds(60),"timeZone","UTC","timePrecision","SECOND","valueState","KNOWN","kind","PHYSICAL_DELIVERY","physicalScopeId",observation,"quantity",new java.math.BigDecimal("30"),"unit","BOX","reassessmentState","COMPLETE");
   insert("mulino_trade_sales_Deliveries",DELIVERY,"effectiveAt",Instant.now(),"observationId",observation,"canonicalOccurrenceId",canonical,"dispatchId",DISPATCH,"cargoScopeId",CARGO,"salesLineId",LINE,"workId",WORK,"customerId",CUSTOMER,"itemId",ITEM,"lotId",LOT,"rangeRootId",ROOT,"startQuantity",java.math.BigDecimal.ZERO,"quantity",new java.math.BigDecimal("30"),"unit","BOX","placeId",CUSTOMER_PLACE,"segmentId",CUSTOMER_STOCK,"occurredAt",OCCURRED.minusSeconds(60),"legitimateQuantity",new java.math.BigDecimal("30"));
   return null;
  });
  jdbc.update("INSERT INTO mulino_identity_ManagementAuthorities(organizationId,ID,actorId,capabilityId,scopeKind,scopeId,validFrom,validUntil) VALUES(?,?,?,'authorizeReturn','ORGANIZATION',?,CURRENT_TIMESTAMP-INTERVAL '1 day',CURRENT_TIMESTAMP+INTERVAL '1 day')",ORG,uuid(),ACTOR,ORG);
  org.mockito.Mockito.when(localPolicy.rule(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.anyString())).thenReturn(new com.mulino.application.policy.PolicyCommandGuard.Selection(Map.of("effectClass","RETURN"),"a".repeat(64),uuid(),"fixture"));
 }
 void insert(String table,String id,Object...fields){var row=new LinkedHashMap<String,Object>();row.put("organizationId",ORG);row.put("ID",id);row.put("revision",0);row.put("createdAt",Instant.now());row.put("recordedAt",Instant.now());for(int index=0;index<fields.length;index+=2)row.put(fields[index].toString(),fields[index+1]);jdbc.update("INSERT INTO "+table+"("+String.join(",",row.keySet())+") VALUES("+String.join(",",Collections.nCopies(row.size(),"?"))+")",row.values().stream().map(v->v instanceof Instant i?java.sql.Timestamp.from(i):v).toArray());}
 Map<String,Object> command(String cap,String key,Map<String,Object> slots,Integer revision){var m=new LinkedHashMap<String,Object>();m.put("intentKind",cap.equals("receiveReturn")?"RECORD":"COMMAND");m.put("definitionVersion","return-v1");m.put("capabilityId",cap);m.put("commandIdempotencyKey",key);m.put("subjectRefs",List.of(Map.of("type","Work","id",WORK)));m.put("slots",slots);m.put("provenance",Map.of("sourceNamespace","USER"));if(revision!=null)m.put("expectedRevision",revision);return m;}
 Map<String,Object> run(String cap,Map<String,Object> slots,Integer rev){var result=request(()->gateway.execute(command(cap,uuid(),slots,rev)));assertEquals("APPLIED",result.get("outcome"),result.toString());return (Map<String,Object>)result.get("effects");}
 String provisional(String stableEvent,String quantity){String authorization=run("authorizeReturn",Map.of("deliveryId",DELIVERY,"startQuantity","0","quantity",quantity,"unit","BOX","destinationId",PLACE,"validUntil",Instant.now().plusSeconds(3600).toString(),"reason","Synthetic return"),0).get("authorizationId").toString();return run("receiveReturn",Map.of("authorizationId",authorization,"eventId",stableEvent,"occurredAt",OCCURRED.toString(),"nextCheckAt",Instant.now().plusSeconds(3600).toString()),0).get("returnId").toString();}
 record Fact(String observation,String canonical,String document,String claim){}
 Fact canonical(String observation,String wrongCustomer){
  var row=jdbc.queryForMap("SELECT * FROM mulino_trade_returns_Observations WHERE ID=?",observation);var body=new LinkedHashMap<String,Object>();body.put("returnId",observation);body.put("kind","RETURN_RECEIPT");for(String key:List.of("eventId","deliveryId","customerId","itemId","lotId","rangeRootId","startQuantity","quantity","unit","placeId","workId"))body.put(key,Objects.toString(row.get(key.toLowerCase())));body.put("occurredAt",OCCURRED.toString());if(wrongCustomer!=null)body.put("customerId",wrongCustomer);String source;try{source=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body);}catch(Exception e){throw new IllegalStateException(e);}byte[] bytes=source.getBytes(java.nio.charset.StandardCharsets.UTF_8);
  String doc=request(()->records.attachDocument(new DocumentInput(new Subject(SubjectKind.RETURN,observation),"warehouse",uuid(),"application/json",LocalBlobStore.hash(bytes),"SYNTHETIC",null),bytes)).get("id").toString();String event=request(()->records.recordActivity(new EventInput(new Subject(SubjectKind.RETURN,observation),"RETURN_RECEIPT","warehouse",uuid(),"1",new EffectiveTime(OCCURRED,null,"UTC","SECOND"),ValueState.KNOWN,source,null,null))).get("id").toString();String quantity=body.get("quantity").toString();String claim=request(()->records.recordClaim(new ClaimInput(event,doc,"Return receipt",quantity,"BOX",ValueState.KNOWN,null))).get("id").toString();
  String id=request(()->new TransactionTemplate(tx).execute(status->{var e=repository.require("Events",ORG,event);var c=auth.context(Instant.now(),Instant.now());var review=reconciliation.match(c,new EvidenceReconciliation.Review(claim,doc,observation,null,"synthetic-v1",e.get("sourceNamespace")+":"+e.get("externalEventId")+":"+e.get("sourceVersion"),quantity,"BOX",OCCURRED,"Original return matched"),"matchSourceIdentity");if(wrongCustomer!=null){assertNotEquals("MATCHED",review.get("outcome"),review.toString());throw DomainError.invalid("Original customer mismatch rejected");}assertEquals("MATCHED",review.get("outcome"),review.toString());return reconciliation.link(c,review.get("id").toString()).get("id").toString();}));return new Fact(observation,id,doc,claim);
 }
 Map<String,Object> confirm(Fact fact,String key){return request(()->gateway.execute(command("receiveReturn",key,Map.of("returnId",fact.observation(),"canonicalOccurrenceId",fact.canonical(),"nextCheckAt",Instant.now().plusSeconds(3600).toString()),0)));}
 int count(String table){return jdbc.queryForObject("SELECT count(*) FROM "+table,Integer.class);}
 @Test void publicOriginalMovesExistingTenAndPreservesDeliveryThirtyAndIndependentDuties(){String observation=provisional(uuid(),"10");assertEquals(2,count("mulino_inventory_QuantitySegments"));Fact fact=canonical(observation,null);var first=confirm(fact,uuid());assertEquals("APPLIED",first.get("outcome"),first.toString());assertEquals("30.000000000000",jdbc.queryForObject("SELECT SUM(quantity)::text FROM mulino_inventory_QuantitySegments WHERE retiredAt IS NULL",String.class));assertEquals("10.000000000000",jdbc.queryForObject("SELECT SUM(quantity)::text FROM mulino_inventory_QuantitySegments WHERE retiredAt IS NULL AND placeId=?",String.class,PLACE));assertEquals("30.000000000000",jdbc.queryForObject("SELECT quantity::text FROM mulino_trade_sales_Deliveries WHERE ID=?",String.class,DELIVERY));assertEquals(0,count("mulino_trade_purchase_ReceiptCredits"));assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_inventory_Restrictions WHERE category='QC' AND state='ACTIVE' AND action='ALL'",Integer.class));for(String kind:List.of("RETURN_QC_REVIEW","RETURN_COMMERCIAL_REVIEW","RETURN_SETTLEMENT_REVIEW"))assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_work_read_ObligationReferences WHERE kind=? AND ownerId IS NOT NULL AND nextCheckAt IS NOT NULL",Integer.class,kind));}
 @Test void immutableOriginalWrongCustomerCannotAuthorizePhysicalReturn(){String observation=provisional(uuid(),"10");assertThrows(DomainError.class,()->canonical(observation,uuid()));assertEquals(0,count("mulino_trade_returns_Receipts"));assertEquals(0,count("mulino_inventory_QuantityMovements"));}
 @Test void sameKeyReplayAndNewEventCannotRecreatePhysicalInventory(){Fact fact=canonical(provisional(uuid(),"10"),null);String key=uuid();var payload=command("receiveReturn",key,Map.of("returnId",fact.observation(),"canonicalOccurrenceId",fact.canonical(),"nextCheckAt",Instant.now().plusSeconds(3600).toString()),0);var applied=request(()->gateway.execute(payload));assertEquals("APPLIED",applied.get("outcome"));assertEquals(applied,request(()->gateway.execute(payload)));Fact repeated=canonical(provisional(uuid(),"10"),null);assertNotEquals("APPLIED",confirm(repeated,uuid()).get("outcome"));assertEquals(1,count("mulino_trade_returns_Receipts"));assertEquals("10.000000000000",jdbc.queryForObject("SELECT SUM(quantity)::text FROM mulino_inventory_QuantitySegments WHERE retiredAt IS NULL AND placeId=?",String.class,PLACE));}
 @Test void simulatedCommitFailureRollsBackMoveReceiptHoldAndResidualDuties(){Fact fact=canonical(provisional(uuid(),"10"),null);int duties=count("mulino_work_read_ObligationReferences");org.mockito.Mockito.doThrow(new IllegalStateException("audit-fail")).when(localPolicy).verifyCommit(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.eq("receiveReturn"),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.anyMap(),org.mockito.ArgumentMatchers.eq(false));assertThrows(IllegalStateException.class,()->confirm(fact,uuid()));assertEquals(2,count("mulino_inventory_QuantitySegments"));assertEquals(0,count("mulino_trade_returns_Receipts"));assertEquals(0,count("mulino_inventory_Restrictions"));assertEquals(duties,count("mulino_work_read_ObligationReferences"));}
 @Test void revokedAuthorizingManagerPreventsReceiptEvenWithReceiverCapability(){String observation=provisional(uuid(),"10");Fact fact=canonical(observation,null);jdbc.update("UPDATE mulino_identity_ManagementAuthorities SET revokedAt=CURRENT_TIMESTAMP WHERE capabilityId='authorizeReturn'");assertNotEquals("APPLIED",confirm(fact,uuid()).get("outcome"));assertEquals(0,count("mulino_trade_returns_Receipts"));assertEquals(0,count("mulino_inventory_QuantityMovements"));}
 @Test void readOnlyGrantCannotApplyReceiptOrDisposition(){Fact fact=canonical(provisional(uuid(),"10"),null);jdbc.update("DELETE FROM mulino_identity_GrantActions WHERE organizationId=? AND grantId=? AND capabilityId='receiveReturn'",ORG,GRANT);assertNotEquals("APPLIED",confirm(fact,uuid()).get("outcome"));assertEquals(0,count("mulino_trade_returns_Receipts"));assertEquals(0,count("mulino_inventory_Restrictions"));}

}
