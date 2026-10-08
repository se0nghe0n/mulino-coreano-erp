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
class SalesCommandPostgresTest {
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

  Map<String,Object> purchase(String cap,int rev,Map<String,Object> slots,String key){return runtime.requestContext().run(c->{return commands.execute(row("intentKind","COMMAND","capabilityId",cap,"definitionVersion","1.1.0","expectedRevision",rev,"slots",slots,"provenance",Map.of(),"subjectRefs",List.of(),"commandIdempotencyKey",key));});}
  Map<String,Object> propose(){return purchase("proposePurchase",revision(work),row("workId",work,"quantity",Map.of("value","100","unit","BOX"),"price",Map.of("value","1","currency","EUR"),"supplierId",supplier,"destinationId",place,"dueAt",Instant.now().plusSeconds(86400).toString(),"endpoint","ARRIVED","quantityMode","CUMULATIVE_EVENT"),id());}
  Map<String,Object> approve(Map<String,Object> proposal,String decision){return purchase("approvePurchase",((Number)proposal.get("proposalRevision")).intValue(),row("proposalId",proposal.get("proposalId"),"proposalHash",proposal.get("proposalHash"),"decision",decision,"conditions",decision.equals("CONDITIONAL")?List.of("supplierCertificateRequired"):List.of(),"validUntil",Instant.now().plusSeconds(3600).toString()),id());}
  Map<String,Object> dispatch(Map<String,Object> proposal,Map<String,Object> approval){return purchase("dispatchPurchaseOrder",((Number)proposal.get("proposalRevision")).intValue(),row("proposalId",proposal.get("proposalId"),"proposalHash",proposal.get("proposalHash"),"approvalId",approval.get("approvalId"),"channel","SYNTHETIC_SUPPLIER","externalOperationId",id()),id());}
  int count(String table){return jdbc.queryForObject("SELECT count(*) FROM "+table+" WHERE organizationId=?",Integer.class,org);}

 @Autowired com.mulino.application.trade.sales.SalesCommands sales;
 String customer;
 @BeforeEach void salesSetup()throws Exception{
  customer=id();insert("mulino_trade_sales_Customers",row("organizationId",org,"ID",customer,"name","B2B customer","revision",1,"createdAt",Timestamp.from(AS_OF),"recordedAt",Timestamp.from(AS_OF),"effectiveAt",Timestamp.from(AS_OF)));
  var old=json.readTree(jdbc.queryForObject("SELECT content FROM mulino_definitions_DefinitionVersions WHERE organizationId=? AND ID=?",String.class,org,definition));
  String newDef=id();((com.fasterxml.jackson.databind.node.ObjectNode)old).put("id",newDef).put("version","1.1.0");
  var noun=(com.fasterxml.jackson.databind.node.ArrayNode)old.get("nouns");for(String type:List.of("SalesOrder","SalesOrderLine","SalesOrderRevision"))noun.add(json.valueToTree(Map.of("name",type,"core",true)));
  var verbs=(com.fasterxml.jackson.databind.node.ArrayNode)old.get("verbs");var caps=(com.fasterxml.jackson.databind.node.ArrayNode)old.get("capabilities");
  String grant=id();insert("mulino_identity_Grants",row("organizationId",org,"ID",grant,"actorId",actor,"delegatorId",actor,"validFrom",Timestamp.from(Instant.now().minusSeconds(3600)),"validUntil",Timestamp.from(Instant.now().plusSeconds(3600))));insert("mulino_identity_GrantScopes",row("organizationId",org,"grantId",grant,"scopeKind","ORGANIZATION","scopeId",org));
  for(String cap:List.of("createSalesOrder","reviseSalesOrder")){verbs.add(json.valueToTree(new com.mulino.domain.definitions.Definition.Verb(cap,"COMMAND",cap,"WORK",Map.of())));caps.add(json.valueToTree(new com.mulino.domain.definitions.Definition.Capability(cap,"1.0.0","core-v1","1.0.0","1.0.0",List.of())));insert("mulino_identity_CapabilityAssignments",row("organizationId",org,"ID",id(),"actorId",actor,"capabilityId",cap,"scopeKind","ORGANIZATION","scopeId",org,"validFrom",Timestamp.from(Instant.now().minusSeconds(3600)),"validUntil",Timestamp.from(Instant.now().plusSeconds(3600))));insert("mulino_identity_GrantActions",row("organizationId",org,"grantId",grant,"capabilityId",cap));}
  String content=json.writeValueAsString(old);insert("mulino_definitions_DefinitionVersions",row("organizationId",org,"ID",newDef,"createdAt",Timestamp.from(AS_OF),"version","1.1.0","state","PUBLISHED","contentHash",com.mulino.domain.definitions.DefinitionRepository.sha256(content),"content",content,"evaluatorVersion","core-v1","schemaVersion","1.0.0"));
  String policy=id(),rule=json.writeValueAsString(Map.of("rules",Map.of("createSalesOrder",Map.of("effectClass","SALES_ORDER"),"reviseSalesOrder",Map.of("effectClass","SALES_ORDER"))));insert("mulino_governance_PolicyVersions",row("organizationId",org,"ID",policy,"version","sales-fixture-v1","kind","COMMAND","content",rule,"contentHash",com.mulino.domain.definitions.DefinitionRepository.sha256(rule),"createdAt",LocalDateTime.ofInstant(AS_OF,ZoneOffset.UTC),"effectiveFrom",LocalDateTime.ofInstant(AS_OF,ZoneOffset.UTC)));jdbc.update("UPDATE mulino_governance_ActivePolicies SET policyId=? WHERE organizationId=? AND kind='COMMAND'",policy,org);
 }
 /** Writes carry the commit clock as knownAt, as ApplicationCommands does; a future knownAt would hide the row from current commands. */
 DomainContext commitContext(){Instant now=Instant.now();return new DomainContext(org,actor,actor,now,now);}
 Map<String,Object> saleSlots(String quantity){return row("workId",work,"customerId",customer,"itemId",item,"quantity",quantity,"unit","BOX","price","1","currency","EUR","dueAt","2026-10-31T00:00:00Z","destinationId",place,"deliveryEndpoint","DELIVERED","qualityTerms","QC accepted","packageTerms","BOX12");}
 Map<String,Object> effects(Map<String,Object> result){assertEquals("APPLIED",result.get("outcome"),result.toString());return (Map<String,Object>)result.get("effects");}
 @Test void immutableRevisionRetryAndConflictingPayloadAreAtomic(){var slots=saleSlots("100");String key=id();var first=purchase("createSalesOrder",0,slots,key);var e=effects(first);assertEquals(first,purchase("createSalesOrder",0,slots,key));var changed=new LinkedHashMap<>(slots);changed.put("quantity","101");assertEquals("CONFLICT",purchase("createSalesOrder",0,changed,key).get("outcome"));assertEquals(1,count("mulino_trade_sales_Orders"));var revise=new LinkedHashMap<>(slots);revise.put("orderId",e.get("orderId"));revise.put("reason","quantity revised");revise.put("quantity","120");var revised=effects(purchase("reviseSalesOrder",1,revise,id()));assertEquals(2,count("mulino_trade_sales_OrderRevisions"));assertEquals(100,jdbc.queryForObject("SELECT quantity FROM mulino_trade_sales_OrderLines WHERE ID=?",java.math.BigDecimal.class,e.get("lineId")).intValueExact());assertThrows(Exception.class,()->jdbc.update("UPDATE mulino_trade_sales_OrderLines SET quantity=1 WHERE ID=?",e.get("lineId")));runtime.requestContext().run(ctx->{assertEquals(java.math.BigDecimal.ZERO,sales.remainingQuantity(context(),e.get("lineId").toString()));assertEquals(120,sales.remainingQuantity(context(),revised.get("lineId").toString()).intValueExact());});}
 @Test void readonlyAuthorityHasZeroCommercialEffects(){jdbc.update("DELETE FROM mulino_identity_GrantActions WHERE organizationId=? AND capabilityId='createSalesOrder'",org);assertEquals("REJECTED",purchase("createSalesOrder",0,saleSlots("100"),id()).get("outcome"));assertEquals(0,count("mulino_trade_sales_Orders"));assertEquals(0,count("mulino_trade_sales_OrderLines"));}
 @Test void invoiceAndDispatchHistoryCannotBeErasedByRevision(){var e=effects(purchase("createSalesOrder",0,saleSlots("100"),id()));runtime.requestContext().run(ctx->{new org.springframework.transaction.support.TransactionTemplate(transactions).execute(tx->{sales.recordExecutionEffect(commitContext(),e.get("lineId").toString(),"DISPATCHED",id(),new java.math.BigDecimal("60"),"BOX");return null;});});var changed=saleSlots("59");changed.put("orderId",e.get("orderId"));changed.put("reason","cannot erase dispatched");assertEquals("HELD",purchase("reviseSalesOrder",1,changed,id()).get("outcome"));assertEquals(1,count("mulino_trade_sales_OrderRevisions"));changed.put("quantity","120");var revised=effects(purchase("reviseSalesOrder",1,changed,id()));runtime.requestContext().run(ctx->{assertEquals(60,sales.remainingQuantity(context(),revised.get("lineId").toString()).intValueExact());assertEquals(0,sales.remainingQuantity(context(),e.get("lineId").toString()).intValueExact());});}
 @Test void auditInsertFailureRollsBackCommercialRows(){jdbc.execute("CREATE FUNCTION sales_test_audit_fail() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'sales test audit failure'; END $$");jdbc.execute("CREATE TRIGGER sales_test_audit_fail BEFORE INSERT ON mulino_commands_CommandAudits FOR EACH ROW EXECUTE FUNCTION sales_test_audit_fail()");try{assertThrows(Exception.class,()->purchase("createSalesOrder",0,saleSlots("100"),id()));assertEquals(0,count("mulino_trade_sales_Orders"));assertEquals(0,count("mulino_trade_sales_OrderRevisions"));assertEquals(0,count("mulino_trade_sales_OrderLines"));}finally{jdbc.execute("DROP TRIGGER sales_test_audit_fail ON mulino_commands_CommandAudits");jdbc.execute("DROP FUNCTION sales_test_audit_fail()");}}
 @Test void executableAndSuspendedAllocationsBlockRevision(){for(String state:List.of("EXECUTABLE","SUSPENDED")){var e=effects(purchase("createSalesOrder",0,saleSlots("100"),id()));var allocation=id();insert("mulino_inventory_SegmentAllocations",row("organizationId",org,"ID",allocation,"createdAt",Timestamp.from(AS_OF),"recordedAt",Timestamp.from(AS_OF),"rootId",segment,"segmentId",segment,"orderLineId",e.get("lineId"),"quantity",new java.math.BigDecimal("20"),"unit","BOX","state",state,"commandId",id()));var change=saleSlots("120");change.put("orderId",e.get("orderId"));change.put("reason","active allocation must reconcile first");assertEquals("HELD",purchase("reviseSalesOrder",1,change,id()).get("outcome"));assertEquals(state,jdbc.queryForObject("SELECT state FROM mulino_inventory_SegmentAllocations WHERE ID=?",String.class,allocation));}assertEquals(2,count("mulino_trade_sales_OrderRevisions"));}
 @SuppressWarnings("unchecked") Map<String,Object> salesOrderAt(String order,Instant asOf,Instant knownAt){return runtime.requestContext().run(ctx->{return (Map<String,Object>)queries.query(QueryRequests.parse("getSalesOrder",row("id",order,"scope",Map.of("organizationId",org),"asOf",asOf.toString(),"knownAt",knownAt.toString()))).get("data");});}
 Instant effectiveAt(String table,String id){return jdbc.queryForObject("SELECT effectiveAt FROM "+table+" WHERE ID=?",Timestamp.class,id).toInstant();}
 Instant recordedAt(String table,String id){return jdbc.queryForObject("SELECT recordedAt FROM "+table+" WHERE ID=?",Timestamp.class,id).toInstant();}
 @Test void historicalOrderProjectionShowsRevisionOnlyAfterEffectiveAt(){
  String readGrant=id();insert("mulino_identity_Grants",row("organizationId",org,"ID",readGrant,"actorId",actor,"delegatorId",actor,"validFrom",Timestamp.from(Instant.now().minusSeconds(3600)),"validUntil",Timestamp.from(Instant.now().plusSeconds(3600))));insert("mulino_identity_GrantScopes",row("organizationId",org,"grantId",readGrant,"scopeKind","ORGANIZATION","scopeId",org));insert("mulino_identity_CapabilityAssignments",row("organizationId",org,"ID",id(),"actorId",actor,"capabilityId","getSalesOrder","scopeKind","ORGANIZATION","scopeId",org,"validFrom",Timestamp.from(Instant.now().minusSeconds(3600)),"validUntil",Timestamp.from(Instant.now().plusSeconds(3600))));insert("mulino_identity_GrantActions",row("organizationId",org,"grantId",readGrant,"capabilityId","getSalesOrder"));
  var e=effects(purchase("createSalesOrder",0,saleSlots("100"),id()));String order=e.get("orderId").toString();Instant first=effectiveAt("mulino_trade_sales_OrderRevisions",e.get("revisionId").toString());
  while(!Instant.now().isAfter(first.plusMillis(2)))Thread.onSpinWait();
  var revise=saleSlots("120");revise.put("orderId",order);revise.put("reason","quantity revised");var r2=effects(purchase("reviseSalesOrder",1,revise,id()));Instant second=effectiveAt("mulino_trade_sales_OrderRevisions",r2.get("revisionId").toString());Instant known=recordedAt("mulino_trade_sales_OrderRevisions",r2.get("revisionId").toString());assertTrue(first.isBefore(second),first+" "+second);
  Instant now=Instant.now();
  var before=salesOrderAt(order,first,now);assertEquals(1,((Number)before.get("currentRevision")).intValue(),before.toString());assertEquals(1,((Number)before.get("revision")).intValue(),before.toString());assertEquals(1,((List<?>)before.get("revisions")).size());var lines=(List<Map<String,Object>>)before.get("lines");assertEquals(1,lines.size());assertEquals(e.get("lineId"),lines.getFirst().get("ID"));assertEquals(0,new java.math.BigDecimal("100").compareTo(new java.math.BigDecimal(lines.getFirst().get("quantity").toString())));
  var unknownYet=salesOrderAt(order,now,known.minusNanos(1000));assertEquals(1,((Number)unknownYet.get("currentRevision")).intValue(),unknownYet.toString());assertEquals(1,((List<?>)unknownYet.get("lines")).size());
  var after=salesOrderAt(order,second,now);assertEquals(2,((Number)after.get("currentRevision")).intValue(),after.toString());assertEquals(2,((Number)after.get("revision")).intValue());assertEquals(2,((List<?>)after.get("revisions")).size());assertEquals(Set.of(e.get("lineId"),r2.get("lineId")),((List<Map<String,Object>>)after.get("lines")).stream().map(x->x.get("ID")).collect(java.util.stream.Collectors.toSet()));
  assertThrows(DomainError.class,()->salesOrderAt(order,first.minusNanos(1000),now));
 }
}
