package com.mulino.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.mulino.application.core.*;
import com.sap.cds.ql.Select;
import com.sap.cds.services.persistence.PersistenceService;
import com.sap.cds.services.runtime.CdsRuntime;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class PurchaseCommandPostgresTest {
  static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280");
  static final java.security.KeyPair KEY;
  static final Path KEY_PATH;
  static final Path BLOB_PATH;
  static { try {PG.start();var generator=java.security.KeyPairGenerator.getInstance("RSA");generator.initialize(2048);KEY=generator.generateKeyPair();BLOB_PATH=Files.createTempDirectory("mulino-purchase-blob-");KEY_PATH=Files.createTempFile("mulino-purchase-public-",".pem");Files.writeString(KEY_PATH,"-----BEGIN PUBLIC KEY-----\n"+Base64.getMimeEncoder(64,new byte[]{10}).encodeToString(KEY.getPublic().getEncoded())+"\n-----END PUBLIC KEY-----\n");}catch(Exception failure){throw new ExceptionInInitializerError(failure);} }
  @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("spring.datasource.url",PG::getJdbcUrl);r.add("spring.datasource.username",PG::getUsername);r.add("spring.datasource.password",PG::getPassword);r.add("JWT_PUBLIC_KEY",KEY_PATH::toString);r.add("JWT_ISSUER",()->"https://mulino.local.invalid");r.add("JWT_AUDIENCE",()->"mulino-platform");r.add("mulino.evidence.blob-root",BLOB_PATH::toString);}
  @Autowired com.mulino.application.work.WorkLifecycle lifecycle;
  @Autowired com.mulino.domain.work.WorkRepository workRepository;
  @Autowired com.mulino.application.evaluation.AssessmentService assessmentService;
  @Autowired com.mulino.adapters.blob.LocalBlobStore blobs;

  @Autowired JdbcTemplate jdbc;
  @Autowired ApplicationQueries queries;
  @Autowired ApplicationCommands commands;
  @Autowired PersistenceService persistence;
  @Autowired CdsRuntime runtime;
  @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
  @LocalServerPort int port;
  final ObjectMapper json=new ObjectMapper();
  @Autowired com.mulino.application.trade.purchase.PurchaseLinePort purchaseLines;
  String supplier;
  String org,actor,product,item,spec,pack,manufacturer,lot,place,segment,definition,work,goal,obligation,document,profile;
  static final Instant AS_OF=Instant.parse("2026-10-07T09:00:00Z"),KNOWN=Instant.parse("2026-10-07T09:00:01Z");
  String id(){return UUID.randomUUID().toString();}
  void insert(String table,Map<String,Object> row){String columns=String.join(",",row.keySet());jdbc.update("INSERT INTO "+table+" ("+columns+") VALUES ("+String.join(",",Collections.nCopies(row.size(),"?"))+")",row.values().toArray());}
  Map<String,Object> row(Object... values){var m=new LinkedHashMap<String,Object>();for(int i=0;i<values.length;i+=2)m.put((String)values[i],values[i+1]);return m;}
  Map<String,Object> scoped(String id){return row("organizationId",org,"ID",id,"revision",0,"createdAt",Timestamp.from(AS_OF.minusSeconds(10)),"recordedAt",Timestamp.from(KNOWN.minusSeconds(10)));}
  Map<String,Object> workRow(String id){var r=scoped(id);r.put("effectiveAt",Timestamp.from(AS_OF));return r;}
  @BeforeEach void seed(){
    org=id();actor=id();product=id();item=id();spec=id();pack=id();manufacturer=id();lot=id();place=id();segment=id();definition=id();work=id();goal=id();obligation=id();document=id();
    insert("mulino_identity_Organizations",row("ID",org,"externalAlias",org));
    insert("mulino_identity_Actors",row("organizationId",org,"ID",actor,"kind","HUMAN","stableRequestOwner",id()));
    insert("mulino_identity_AuthorityFences",row("organizationId",org,"actorId",actor,"revision",1));
    insert("mulino_identity_ExternalIdentities",row("organizationId",org,"ID",id(),"actorId",actor,"issuer","https://mulino.local.invalid","subject","writer-a","organizationAlias",org));
    insert("mulino_identity_Memberships",row("organizationId",org,"ID",id(),"actorId",actor,"validFrom",Timestamp.from(Instant.now().minusSeconds(3600)),"validUntil",Timestamp.from(Instant.now().plusSeconds(3600))));
    String grant=id();insert("mulino_identity_Grants",row("organizationId",org,"ID",grant,"actorId",actor,"delegatorId",actor,"validFrom",Timestamp.from(Instant.now().minusSeconds(3600)),"validUntil",Timestamp.from(Instant.now().plusSeconds(3600))));
    for(String cap:List.of("getObject","searchObjects","getWork","searchWorks","getInventory","getEvidence","getAssessment","getObligations","getDefinition","createDraft","createWork","cancelDraft","reviseGoal")){
      insert("mulino_identity_CapabilityAssignments",row("organizationId",org,"ID",id(),"actorId",actor,"capabilityId",cap,"scopeKind","ORGANIZATION","scopeId",org,"validFrom",Timestamp.from(Instant.now().minusSeconds(3600)),"validUntil",Timestamp.from(Instant.now().plusSeconds(3600))));
      insert("mulino_identity_GrantActions",row("organizationId",org,"grantId",grant,"capabilityId",cap));
    }
    insert("mulino_identity_GrantScopes",row("organizationId",org,"grantId",grant,"scopeKind","ORGANIZATION","scopeId",org));
    var p=scoped(product);p.put("name","integration item");insert("mulino_inventory_Products",p);
    var s=scoped(spec);s.putAll(row("productId",product,"version","1","contentHash","1".repeat(64)));insert("mulino_inventory_SpecificationVersions",s);
    var pk=scoped(pack);pk.putAll(row("productId",product,"version","1","contentHash","2".repeat(64)));insert("mulino_inventory_PackagingVersions",pk);
    var i=scoped(item);i.putAll(row("productId",product,"name","integration item","baseUnit","BOX","decimalPlaces",0,"specificationVersionId",spec,"packagingVersionId",pack));insert("mulino_inventory_TradeItems",i);
    var m=scoped(manufacturer);m.put("name","maker");insert("mulino_inventory_Manufacturers",m);
    var l=scoped(lot);l.putAll(row("manufacturerId",manufacturer,"itemId",item,"originalLot","LOT-1"));insert("mulino_inventory_ManufacturingLots",l);
    var loc=scoped(place);loc.putAll(row("name","warehouse","kind","WAREHOUSE"));insert("mulino_inventory_Places",loc);
    var seg=scoped(segment);seg.putAll(row("itemId",item,"lotId",lot,"identificationStatus","CONFIRMED","quantity",new java.math.BigDecimal("100"),"unit","BOX","placeId",place,"controlScope","warehouse","validFrom",Timestamp.from(AS_OF),"mixtureStatus","IDENTIFIED"));insert("mulino_inventory_QuantitySegments",seg);
    try {
      var predicate=Map.<String,Object>of("operator","quantitySum","property","Receipt.quantity","minimum",Map.of("value","1","unit","BOX"),"unit","BOX","evidenceSelector","VERIFIED_DISTINCT");
      var d=new com.mulino.domain.definitions.Definition(org,definition,"1.0.0",null,"PUBLISHED","", "core-v1","1.0.0",List.of(new com.mulino.domain.definitions.Definition.NounType("PurchaseOrder",true),new com.mulino.domain.definitions.Definition.NounType("Receipt",true),new com.mulino.domain.definitions.Definition.NounType("TradeItem",true),new com.mulino.domain.definitions.Definition.NounType("Work",true)),List.of(new com.mulino.domain.definitions.Definition.Attribute("Receipt","quantity",com.mulino.domain.definitions.Definition.ValueType.DECIMAL,null,"BOX",0,1,1,"READ",true)),List.of("proposePurchase","revisePurchase","approvePurchase","dispatchPurchaseOrder","recordSupplierReply","cancelPurchase").stream().map(cap->new com.mulino.domain.definitions.Definition.Verb(cap,"COMMAND",cap,"WORK",Map.of())).toList(),List.of(),List.of(new com.mulino.domain.definitions.Definition.Goal("Arrival","CUMULATIVE_EVENT","ARRIVED","core-v1",predicate),new com.mulino.domain.definitions.Definition.Goal("CurrentPhysicalStock","STATE_AT","PHYSICAL","core-v1",Map.of("operator","stateQuantity","property","Receipt.quantity","minimum",Map.of("value","1","unit","BOX"),"unit","BOX","evidenceSelector","CURRENT_STATE"))),List.of("proposePurchase","revisePurchase","approvePurchase","dispatchPurchaseOrder","recordSupplierReply","cancelPurchase").stream().map(cap->new com.mulino.domain.definitions.Definition.Capability(cap,"1.0.0","core-v1","1.0.0","1.0.0",List.of())).toList());
      String content=json.writeValueAsString(d);
      insert("mulino_definitions_DefinitionVersions",row("organizationId",org,"ID",definition,"createdAt",Timestamp.from(AS_OF),"version","1.0.0","state","PUBLISHED","contentHash",com.mulino.domain.definitions.DefinitionRepository.sha256(content),"content",content,"evaluatorVersion","core-v1","schemaVersion","1.0.0"));
    }catch(Exception failure){throw new IllegalStateException(failure);}

    var w=workRow(work);w.putAll(row("itemId",item,"lotId",lot,"definitionVersionId",definition,"kind","PURCHASE","lifecycleMode","COMMAND","status","ACTIVE","ownerId",actor,"supervisorId",actor,"waitJson","{\"reason\":\"QC_PENDING\"}"));insert("mulino_work_read_Works",w);
    var g=workRow(goal);g.putAll(row("workId",work,"definitionVersionId",definition,"quantityMode","CUMULATIVE_EVENT","targetQuantity",new java.math.BigDecimal("100"),"unit","BOX","endpoint","ARRIVED","scopeJson","{}"));insert("mulino_work_read_GoalReferences",g);
    var o=workRow(obligation);o.putAll(row("workId",work,"kind","QC_REVIEW","status","OPEN","ownerId",actor,"supervisorId",actor,"nextAction","검사 근거 확인","nextCheckAt",Timestamp.from(KNOWN.plusSeconds(3600)),"scopeJson","{}"));insert("mulino_work_read_ObligationReferences",o);
    profile=id();insert("mulino_evidence_SourceProfiles",row("ID",profile,"organizationId",org,"revision",1,"recordedAt",Timestamp.from(KNOWN),"recordedBy",actor,"namespace","native-fixture","policyVersion","fixture-v1","intakeOwnerId",actor,"supervisorId",actor,"nextAction","review","nextCheckAt",Timestamp.from(KNOWN.plusSeconds(3600))));
    insert("mulino_evidence_DocumentVersions",row("ID",document,"organizationId",org,"revision",1,"recordedAt",Timestamp.from(KNOWN),"recordedBy",actor,"subjectKind","ITEM","subjectId",item,"itemId",item,"sha256","3".repeat(64),"availability","UNKNOWN","byteLength",0L,"mediaType","text/plain","sourceProfileId",profile,"provenance","{}"));
    var e=workRow(id());e.putAll(row("workId",work,"documentVersionId",document,"role","QC"));insert("mulino_work_read_EvidenceReferences",e);
    String policy=id();insert("mulino_governance_PolicyVersions",row("organizationId",org,"ID",policy,"version","fixture-v1","kind","EVIDENCE","content","{}","contentHash",com.mulino.domain.definitions.DefinitionRepository.sha256("{}"),"createdAt",LocalDateTime.ofInstant(AS_OF.minusSeconds(100),ZoneOffset.UTC),"effectiveFrom",LocalDateTime.ofInstant(AS_OF.minusSeconds(100),ZoneOffset.UTC)));
    if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT to_regclass('mulino_governance_activepolicies') IS NOT NULL",Boolean.class)))jdbc.update("INSERT INTO mulino_governance_ActivePolicies(organizationId,kind,policyId,revision) VALUES(?,'EVIDENCE',?,0)",org,policy);
    try {String commandPolicy=id();String content=json.writeValueAsString(Map.of("rules",List.of("proposePurchase","revisePurchase","approvePurchase","dispatchPurchaseOrder","recordSupplierReply","cancelPurchase").stream().collect(java.util.stream.Collectors.toMap(x->x,x->Map.of("effectClass","PURCHASE")))));insert("mulino_governance_PolicyVersions",row("organizationId",org,"ID",commandPolicy,"version","command-fixture-v1","kind","COMMAND","content",content,"contentHash",com.mulino.domain.definitions.DefinitionRepository.sha256(content),"createdAt",LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1),"effectiveFrom",LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1)));jdbc.update("INSERT INTO mulino_governance_ActivePolicies(organizationId,kind,policyId,revision) VALUES(?,'COMMAND',?,0)",org,commandPolicy);}catch(Exception failure){throw new IllegalStateException(failure);}
    supplier=id();insert("mulino_trade_purchase_Suppliers",row("organizationId",org,"ID",supplier,"name","test supplier"));
    String purchaseGrant=id();insert("mulino_identity_Grants",row("organizationId",org,"ID",purchaseGrant,"actorId",actor,"delegatorId",actor,"validFrom",Timestamp.from(Instant.now().minusSeconds(3600)),"validUntil",Timestamp.from(Instant.now().plusSeconds(3600))));
    for(String cap:List.of("proposePurchase","revisePurchase","approvePurchase","dispatchPurchaseOrder","recordSupplierReply","cancelPurchase")) {insert("mulino_identity_CapabilityAssignments",row("organizationId",org,"ID",id(),"actorId",actor,"capabilityId",cap,"scopeKind","ORGANIZATION","scopeId",org,"validFrom",Timestamp.from(Instant.now().minusSeconds(3600)),"validUntil",Timestamp.from(Instant.now().plusSeconds(3600))));insert("mulino_identity_GrantActions",row("organizationId",org,"grantId",purchaseGrant,"capabilityId",cap));}
    insert("mulino_identity_GrantScopes",row("organizationId",org,"grantId",purchaseGrant,"scopeKind","ORGANIZATION","scopeId",org));
    insert("mulino_identity_ManagementAuthorities",row("organizationId",org,"ID",id(),"actorId",actor,"capabilityId","approvePurchase","scopeKind","ORGANIZATION","scopeId",org,"validFrom",Timestamp.from(Instant.now().minusSeconds(3600)),"validUntil",Timestamp.from(Instant.now().plusSeconds(3600))));
    login();
  }
  void login(){SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("trusted-test-context").header("alg","RS256").subject("writer-a").issuer("https://mulino.local.invalid").claim("organizationId",org).claim("stableRequestOwner","forged-owner-ignored").build(),List.of()));}
  @AfterEach void clear(){SecurityContextHolder.clearContext();}
  @AfterAll static void stop()throws Exception{PG.stop();Files.deleteIfExists(KEY_PATH);try(var paths=Files.walk(BLOB_PATH)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
  Map<String,Object> request(String operation,String target,String snapshot){Map<String,Object> body=row("scope",Map.of("organizationId",org,"itemId",item,"lotId",lot),"asOf",AS_OF.toString(),"knownAt",KNOWN.toString());if(target!=null)body.put("id",target);if(snapshot!=null)body.put("snapshotRef",snapshot);return body;}
  Map<String,Object> read(String operation,String target,String snapshot){return runtime.requestContext().run(c->{return queries.query(QueryRequests.parse(operation,request(operation,target,snapshot)));});}

  DomainContext context(){return new DomainContext(org,actor,actor,Instant.now().plusSeconds(60),Instant.now().plusSeconds(60));}
  Map<String,Object> command(String op,int rev,Map<String,Object> slots){return row("capabilityId",op,"expectedRevision",rev,"slots",slots);}
  Map<String,Object> run(String op,int rev,Map<String,Object> slots){return runtime.requestContext().run(ctx->{var template=new org.springframework.transaction.support.TransactionTemplate(transactions);return template.execute(tx->lifecycle.execute(context(),command(op,rev,slots)));});}
  Map<String,Object> goalInputs(){return row("quantityMode","CUMULATIVE_EVENT","targetQuantity","100","unit","BOX","endpoint","ARRIVED","dueAt","2026-10-31T00:00:00Z","scope",Map.of("itemId",item),"timezone","Asia/Seoul","evidencePolicyVersion","fixture-v1","periodStartInclusive",true,"periodEndInclusive",true,"periodStart","2026-10-01T00:00:00Z","periodEnd","2026-10-31T00:00:00Z","eventKind","RECEIPT","contributionScope",Map.of("itemId",item),"deduplication","CANONICAL_OCCURRENCE","conditions",List.of(Map.of("id","Arrival")),"evaluatorVersion","core-v1");}
  String draft(Map<String,Object> goal){var result=run("createDraft",0,row("itemId",item,"definitionVersionId",definition,"kind","PURCHASE","ownerId",actor,"supervisorId",actor,"goal",goal));return String.valueOf(((Map<?,?>)result.get("effects")).get("workId"));}

  Map<String,Object> assess(String id){return runtime.requestContext().run(ctx->{return new org.springframework.transaction.support.TransactionTemplate(transactions).execute(tx->assessmentService.assess(context(),id));});}
  int revision(String id){return jdbc.queryForObject("SELECT revision FROM mulino_work_read_Works WHERE ID=?",Integer.class,id);}

  Map<String,Object> purchase(String cap,int rev,Map<String,Object> slots,String key){return runtime.requestContext().run(c->{return commands.execute(row("intentKind","COMMAND","capabilityId",cap,"definitionVersion","1.0.0","expectedRevision",rev,"slots",slots,"provenance",Map.of(),"subjectRefs",List.of(),"commandIdempotencyKey",key));});}
  Map<String,Object> propose(){return purchase("proposePurchase",revision(work),row("workId",work,"quantity",Map.of("value","100","unit","BOX"),"price",Map.of("value","1","currency","EUR"),"supplierId",supplier,"destinationId",place,"dueAt",Instant.now().plusSeconds(86400).toString(),"endpoint","ARRIVED","quantityMode","CUMULATIVE_EVENT"),id());}
  Map<String,Object> approve(Map<String,Object> proposal,String decision){return purchase("approvePurchase",((Number)proposal.get("proposalRevision")).intValue(),row("proposalId",proposal.get("proposalId"),"proposalHash",proposal.get("proposalHash"),"decision",decision,"conditions",decision.equals("CONDITIONAL")?List.of("supplierCertificateRequired"):List.of(),"validUntil",Instant.now().plusSeconds(3600).toString()),id());}
  Map<String,Object> dispatch(Map<String,Object> proposal,Map<String,Object> approval){return purchase("dispatchPurchaseOrder",((Number)proposal.get("proposalRevision")).intValue(),row("proposalId",proposal.get("proposalId"),"proposalHash",proposal.get("proposalHash"),"approvalId",approval.get("approvalId"),"channel","SYNTHETIC_SUPPLIER","externalOperationId",id()),id());}
  int count(String table){return jdbc.queryForObject("SELECT count(*) FROM "+table+" WHERE organizationId=?",Integer.class,org);}
  @Test void proposalWithoutLateLotNeverCreatesStockAndApprovalDispatchAreSeparate(){jdbc.update("UPDATE mulino_work_read_Works SET lotId=NULL WHERE ID=?",work);int stock=count("mulino_inventory_QuantitySegments");var proposal=propose();assertEquals("APPLIED",proposal.get("outcome"));assertEquals(1,count("mulino_trade_purchase_ProposalRevisions"));assertEquals(stock,count("mulino_inventory_QuantitySegments"));var approval=approve(proposal,"APPROVE");assertEquals("APPROVED",approval.get("businessStatus"));var dispatched=dispatch(proposal,approval);assertEquals("APPLIED",dispatched.get("outcome"));assertEquals("DISPATCH_PENDING",dispatched.get("businessStatus"));assertEquals("UNKNOWN",dispatched.get("supplierAcceptance"));assertEquals("0",dispatched.get("arrivalQuantity"));assertEquals(stock,count("mulino_inventory_QuantitySegments"));assertEquals(1,count("mulino_runtime_Outbox"));assertEquals(1,count("mulino_work_read_ObligationReferences")-1);assertTrue(count("mulino_commands_CommandAudits")>=3);}
  @Test void approval100CannotDispatchChanged120AndOriginalRevisionIsImmutable(){var p=propose();var a=approve(p,"APPROVE");var revised=purchase("revisePurchase",1,row("proposalId",p.get("proposalId"),"changes",Map.of("quantity",Map.of("value","120","unit","BOX")),"reason","increased need"),id());assertEquals("APPLIED",revised.get("outcome"),revised.toString());assertNotEquals(p.get("proposalHash"),revised.get("proposalHash"));assertEquals(2,count("mulino_trade_purchase_ProposalRevisions"));var denied=dispatch(revised,a);assertEquals("HELD",denied.get("outcome"));assertEquals(0,count("mulino_trade_purchase_Orders"));assertEquals(0,count("mulino_runtime_Outbox"));assertEquals(100,jdbc.queryForObject("SELECT quantity FROM mulino_trade_purchase_ProposalRevisions WHERE organizationId=? AND revision=1",java.math.BigDecimal.class,org).intValueExact());assertThrows(Exception.class,()->jdbc.update("UPDATE mulino_trade_purchase_ProposalRevisions SET quantity=120 WHERE organizationId=?",org));}
  @Test void rejectionAndConditionalDecisionsCannotDispatch(){var p=propose();assertEquals("HELD",dispatch(p,approve(p,"REJECT")).get("outcome"));assertEquals("HELD",dispatch(p,approve(p,"CONDITIONAL")).get("outcome"));assertEquals(0,count("mulino_trade_purchase_Orders"));}
  @Test void currentManagerRevocationBlocksOldApproval(){var p=propose();var a=approve(p,"APPROVE");jdbc.update("UPDATE mulino_identity_ManagementAuthorities SET revokedAt=CURRENT_TIMESTAMP WHERE organizationId=?",org);assertEquals("REJECTED",dispatch(p,a).get("outcome"));assertEquals(0,count("mulino_trade_purchase_Orders"));}
  @Test void expiredApprovalAndStaleProposalVersionHaveZeroEffects(){var p=propose();var expired=purchase("approvePurchase",1,row("proposalId",p.get("proposalId"),"proposalHash",p.get("proposalHash"),"decision","APPROVE","validUntil",Instant.now().minusSeconds(60).toString()),id());assertEquals("HELD",expired.get("outcome"));assertEquals(0,count("mulino_trade_purchase_Approvals"));assertEquals("CONFLICT",purchase("revisePurchase",0,row("proposalId",p.get("proposalId"),"changes",Map.of("quantity",Map.of("value","120","unit","BOX")),"reason","stale"),id()).get("outcome"));assertEquals(1,count("mulino_trade_purchase_ProposalRevisions"));}
  @Test void proposalIdempotencyReusesEffectAndDifferentPayloadConflicts(){var slots=row("workId",work,"quantity",Map.of("value","100","unit","BOX"),"price",Map.of("value","1","currency","EUR"),"supplierId",supplier,"destinationId",place,"dueAt",Instant.now().plusSeconds(86400).toString(),"endpoint","ARRIVED","quantityMode","CUMULATIVE_EVENT");String key=id();var first=purchase("proposePurchase",0,slots,key);var repeat=purchase("proposePurchase",0,slots,key);assertEquals(first,repeat);assertEquals(1,count("mulino_trade_purchase_Proposals"));slots.put("quantity",Map.of("value","120","unit","BOX"));assertEquals("CONFLICT",purchase("proposePurchase",0,slots,key).get("outcome"));assertEquals(1,count("mulino_trade_purchase_Proposals"));}
  @Test void cancellationRetainsExecuted60AndConcreteExternalAcceptanceResponsibility(){var p=propose();var order=dispatch(p,approve(p,"APPROVE"));String line=order.get("poLineId").toString();runtime.requestContext().run(c->{new org.springframework.transaction.support.TransactionTemplate(transactions).execute(tx->{purchaseLines.recordExecutionEffect(context(),line,"SHIPPED",id(),new java.math.BigDecimal("60"),"BOX");return null;});});var cancel=purchase("cancelPurchase",1,row("purchaseId",order.get("orderId"),"quantity",Map.of("value","40","unit","BOX"),"reason","remaining no longer needed","externalAcceptance","UNKNOWN"),id());assertEquals("APPLIED",cancel.get("outcome"),cancel.toString());assertEquals("CANCELLATION_PENDING",cancel.get("businessStatus"));assertEquals("60",cancel.get("preservedExecutedQuantity"));assertEquals(1,count("mulino_trade_purchase_ExecutionEffects"));assertEquals(2,count("mulino_runtime_Outbox"));assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_work_read_ObligationReferences WHERE organizationId=? AND kind='PURCHASE_CANCELLATION_ACCEPTANCE' AND status='OPEN' AND ownerId=? AND nextAction IS NOT NULL AND nextCheckAt IS NOT NULL",Integer.class,org,actor));assertEquals("REJECTED",purchase("cancelPurchase",2,row("purchaseId",order.get("orderId"),"quantity",Map.of("value","1","unit","BOX"),"reason","cannot erase shipped","externalAcceptance","UNKNOWN"),id()).get("outcome"));assertEquals(1,count("mulino_trade_purchase_Cancellations"));}
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings={"quantity","price","itemId","supplierId","dueAt","destinationId","endpoint"})
  void everyT13CommercialDimensionInvalidatesPriorApproval(String field){
    var p=propose();var a=approve(p,"APPROVE");Object change;
    switch(field){
      case "quantity"->change=Map.of("value","120","unit","BOX");
      case "price"->change=Map.of("value","1.20","currency","EUR");
      case "dueAt"->change=Instant.now().plusSeconds(172800).toString();
      case "endpoint"->change="SELL_ELIGIBLE";
      case "supplierId"->{change=id();insert("mulino_trade_purchase_Suppliers",row("organizationId",org,"ID",change,"name","alternate supplier"));}
      case "destinationId"->{change=id();var dest=scoped(change.toString());dest.putAll(row("name","alternate warehouse","kind","WAREHOUSE"));insert("mulino_inventory_Places",dest);}
      case "itemId"->{change=id();var alt=scoped(change.toString());alt.putAll(row("productId",product,"name","alternate product","baseUnit","BOX","decimalPlaces",0,"specificationVersionId",spec,"packagingVersionId",pack));insert("mulino_inventory_TradeItems",alt);}
      default->throw new AssertionError(field);
    }
    var revised=purchase("revisePurchase",1,row("proposalId",p.get("proposalId"),"changes",Map.of(field,change),"reason","approval scope changed"),id());assertEquals("APPLIED",revised.get("outcome"),revised.toString());assertNotEquals(p.get("proposalHash"),revised.get("proposalHash"));var denial=dispatch(revised,a);assertEquals("HELD",denial.get("outcome"),denial.toString());assertEquals(0,count("mulino_trade_purchase_Orders"));assertEquals(0,count("mulino_runtime_Outbox"));
  }
  @Test void policyReplacementCannotReuseApproval(){var p=propose();var a=approve(p,"APPROVE");String replacement=id();String content="{\"rules\":{\"dispatchPurchaseOrder\":{\"effectClass\":\"PURCHASE\"}},\"revision\":2}";insert("mulino_governance_PolicyVersions",row("organizationId",org,"ID",replacement,"version","command-fixture-v2","kind","COMMAND","content",content,"contentHash",com.mulino.domain.definitions.DefinitionRepository.sha256(content),"createdAt",Timestamp.from(Instant.now().minusSeconds(1)),"effectiveFrom",Timestamp.from(Instant.now().minusSeconds(1))));jdbc.update("UPDATE mulino_governance_ActivePolicies SET policyId=? WHERE organizationId=? AND kind='COMMAND'",replacement,org);var denied=dispatch(p,a);assertEquals("HELD",denied.get("outcome"));assertEquals(0,count("mulino_trade_purchase_Orders"));assertEquals(0,count("mulino_runtime_Outbox"));}

  @Test void supportedPublishedConditionalPredicateUsesCurrentPhysicalFacts(){var p=propose();var a=purchase("approvePurchase",1,row("proposalId",p.get("proposalId"),"proposalHash",p.get("proposalHash"),"decision","CONDITIONAL","conditions",List.of("CurrentPhysicalStock"),"validUntil",Instant.now().plusSeconds(3600).toString()),id());assertEquals("CONDITIONAL",a.get("businessStatus"));var result=dispatch(p,a);assertEquals("APPLIED",result.get("outcome"),result.toString());String proof=jdbc.queryForObject("SELECT conditionAssessmentJson FROM mulino_trade_purchase_Orders WHERE organizationId=?",String.class,org);assertTrue(proof.contains("CurrentPhysicalStock"));assertTrue(proof.contains("SATISFIED"));assertEquals("CONDITIONAL",jdbc.queryForObject("SELECT decision FROM mulino_trade_purchase_Approvals WHERE ID=?",String.class,a.get("approvalId")));}
  @Test void changedCurrentFactsKeepConditionalDecisionButHoldDispatch(){var p=propose();var a=purchase("approvePurchase",1,row("proposalId",p.get("proposalId"),"proposalHash",p.get("proposalHash"),"decision","CONDITIONAL","conditions",List.of("CurrentPhysicalStock"),"validUntil",Instant.now().plusSeconds(3600).toString()),id());jdbc.update("UPDATE mulino_inventory_QuantitySegments SET validFrom=? WHERE organizationId=?",Timestamp.from(Instant.now().plusSeconds(86400)),org);var result=dispatch(p,a);assertEquals("HELD",result.get("outcome"),result.toString());assertEquals(0,count("mulino_trade_purchase_Orders"));assertEquals(0,count("mulino_runtime_Outbox"));assertEquals("CONDITIONAL",jdbc.queryForObject("SELECT decision FROM mulino_trade_purchase_Approvals WHERE ID=?",String.class,a.get("approvalId")));}

 @Test void laterRejectionMakesOldApprovalUnusable(){var p=propose();var a=approve(p,"APPROVE");approve(p,"REJECT");var result=dispatch(p,a);assertEquals("HELD",result.get("outcome"));assertEquals(0,count("mulino_trade_purchase_Orders"));assertEquals(0,count("mulino_runtime_Outbox"));}
 @Test void malformedDueAtIsTypedRejectionAndEffectZero(){var result=purchase("proposePurchase",0,row("workId",work,"quantity",Map.of("value","100","unit","BOX"),"price",Map.of("value","1","currency","EUR"),"supplierId",supplier,"destinationId",place,"dueAt","Friday","endpoint","ARRIVED","quantityMode","CUMULATIVE_EVENT"),id());assertEquals("REJECTED",result.get("outcome"));assertEquals(0,count("mulino_trade_purchase_Proposals"));assertEquals(0,count("mulino_runtime_Outbox"));}

 @Test void unrelatedShipmentAndReceiptEffectsCannotBeAssumedToOverlap(){var p=propose();var order=dispatch(p,approve(p,"APPROVE"));String line=order.get("poLineId").toString();runtime.requestContext().run(c->{new org.springframework.transaction.support.TransactionTemplate(transactions).execute(tx->{purchaseLines.recordExecutionEffect(context(),line,"SHIPPED",id(),new java.math.BigDecimal("60"),"BOX");purchaseLines.recordExecutionEffect(context(),line,"RECEIVED",id(),new java.math.BigDecimal("40"),"BOX");return null;});});var result=purchase("cancelPurchase",1,row("purchaseId",order.get("orderId"),"quantity",Map.of("value","40","unit","BOX"),"reason","unknown overlapping ranges","externalAcceptance","UNKNOWN"),id());assertEquals("HELD",result.get("outcome"));assertEquals("EXECUTION_OVERLAP_UNVERIFIED",((Map<?,?>)result.get("error")).get("code"));assertEquals(2,count("mulino_trade_purchase_ExecutionEffects"));assertEquals(0,count("mulino_trade_purchase_Cancellations"));assertEquals(1,count("mulino_runtime_Outbox"));}

}
