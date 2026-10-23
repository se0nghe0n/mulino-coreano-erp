package com.mulino.core;

import com.mulino.application.core.*;
import com.mulino.application.trade.purchase.PurchaseQueries;
import com.mulino.application.trade.regulatory.RegulatoryQueries;
import com.mulino.application.trade.settlement.SettlementQueries;
import com.sap.cds.services.runtime.CdsRuntime;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real PostgreSQL, current IdentityAuthorization and public noun dispatch; no model/scenario runner. */
@SpringBootTest @ActiveProfiles("local") @SuppressWarnings("unchecked")
class BusinessDiscoveryPostgresTest {
 static final WorkPostgresTestResources HOST=new WorkPostgresTestResources("business-discovery");
 static final Instant T=Instant.parse("2026-10-07T09:00:00Z"),K=T.plusSeconds(1);
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r){HOST.properties(r);}
 @Autowired JdbcTemplate db;
 @Autowired CdsRuntime runtime;
 @Autowired ApplicationQueries queries;
 @Autowired ReadAuthorizer auth;
 @Autowired PurchaseQueries purchase;
 @Autowired SettlementQueries settlement;
 @Autowired RegulatoryQueries regulatory;
 @MockitoBean ExecutionClock clock;
 String org,actor,grant,item,otherItem,work,otherWork,place,otherPlace,supplier,document,segment,definition;
 final Map<String,List<String>> nouns=new HashMap<>();
 String id(){return UUID.randomUUID().toString();}
 Map<String,Object> row(Object... fields){var out=new LinkedHashMap<String,Object>();for(int i=0;i<fields.length;i+=2)out.put(fields[i].toString(),fields[i+1]);return out;}
 void insert(String table,Map<String,Object> row){db.update("INSERT INTO "+table+" ("+String.join(",",row.keySet())+") VALUES ("+String.join(",",Collections.nCopies(row.size(),"?"))+")",row.values().toArray());}
 Map<String,Object> base(String id){return row("organizationId",org,"ID",id,"revision",1,"createdAt",Timestamp.from(T.minusSeconds(10)),"recordedAt",Timestamp.from(T.minusSeconds(10)));}
 void record(String table,String id,Object... fields){var r=base(id);r.putAll(row(fields));insert(table,r);}
 void trade(String table,String id,Instant recorded,Object... fields){var r=base(id);r.put("effectiveAt",Timestamp.from(T.minusSeconds(10)));r.put("recordedAt",Timestamp.from(recorded));r.putAll(row(fields));if(table.startsWith("mulino_trade_settlement_"))r.replaceAll((key,value)->value instanceof Timestamp at?java.time.LocalDateTime.ofInstant(at.toInstant(),java.time.ZoneOffset.UTC):value);insert(table,r);}
 @BeforeEach void seed(){
  when(clock.instant()).thenReturn(T);org=id();actor=id();grant=id();item=id();otherItem=id();work=id();otherWork=id();place=id();otherPlace=id();supplier=id();document=id();segment=id();definition=id();
  insert("mulino_identity_Organizations",row("ID",org,"externalAlias",org));
  insert("mulino_identity_Actors",row("organizationId",org,"ID",actor,"kind","HUMAN","stableRequestOwner",actor));
  insert("mulino_identity_ExternalIdentities",row("organizationId",org,"ID",id(),"actorId",actor,"issuer","https://mulino.local.invalid","subject",actor,"organizationAlias",org));
  insert("mulino_identity_Memberships",row("organizationId",org,"ID",id(),"actorId",actor,"validFrom",Timestamp.from(T.minusSeconds(100)),"validUntil",Timestamp.from(T.plusSeconds(100))));
  insert("mulino_identity_Grants",row("organizationId",org,"ID",grant,"actorId",actor,"delegatorId",actor,"validFrom",Timestamp.from(T.minusSeconds(100)),"validUntil",Timestamp.from(T.plusSeconds(100))));
  String support=id();insert("mulino_identity_Grants",row("organizationId",org,"ID",support,"actorId",actor,"delegatorId",actor,"validFrom",Timestamp.from(T.minusSeconds(100)),"validUntil",Timestamp.from(T.plusSeconds(100))));
  for(String op:List.of("getObject","searchObjects","getWork","getObligations","getPurchase","getSettlement","getRegulatoryProcedure")){
   insert("mulino_identity_GrantActions",row("organizationId",org,"grantId",Set.of("getWork","getObligations").contains(op)?support:grant,"capabilityId",op));
   insert("mulino_identity_CapabilityAssignments",row("organizationId",org,"ID",id(),"actorId",actor,"capabilityId",op,"scopeKind","ORGANIZATION","scopeId",org,"validFrom",Timestamp.from(T.minusSeconds(100)),"validUntil",Timestamp.from(T.plusSeconds(100))));
  }
  scope("ITEM",item);scope("WORK",work);
  for(var dimension:Map.of("ITEM",item,"WORK",work).entrySet())insert("mulino_identity_GrantScopes",row("organizationId",org,"grantId",support,"scopeKind",dimension.getKey(),"scopeId",dimension.getValue()));
  String product=id(),spec=id(),pack=id();record("mulino_inventory_Products",product,"name","same business name");
  record("mulino_inventory_SpecificationVersions",spec,"productId",product,"version","1","contentHash","a".repeat(64));
  record("mulino_inventory_PackagingVersions",pack,"productId",product,"version","1","contentHash","b".repeat(64));
  for(String it:List.of(item,otherItem))record("mulino_inventory_TradeItems",it,"productId",product,"name","same business name","baseUnit","BOX","decimalPlaces",0,"specificationVersionId",spec,"packagingVersionId",pack);
  for(String loc:List.of(place,otherPlace))record("mulino_inventory_Places",loc,"name","same destination name","kind","INTERNAL_STORAGE");
  insert("mulino_definitions_DefinitionVersions",row("organizationId",org,"ID",definition,"version","1","state","PUBLISHED","contentHash","a".repeat(64),"content","{}","evaluatorVersion","core-v1","schemaVersion","1.0.0"));
  for(String w:List.of(work,otherWork))trade("mulino_work_read_Works",w,T.minusSeconds(10),"itemId",item,"definitionVersionId",definition,"kind","PURCHASE","status","ACTIVE","lifecycleMode","COMMAND","ownerId",actor,"supervisorId",actor);
  record("mulino_inventory_QuantitySegments",segment,"itemId",item,"identificationStatus","UNKNOWN","quantity",10,"unit","BOX","placeId",place,"controlScope","discovery","validFrom",Timestamp.from(T.minusSeconds(10)),"mixtureStatus","IDENTIFIED");
  String profile=id();record("mulino_evidence_SourceProfiles",profile,"recordedBy",actor,"namespace","discovery-test","policyVersion","synthetic-v1","intakeOwnerId",actor,"supervisorId",actor,"nextAction","review","nextCheckAt",Timestamp.from(T.plusSeconds(60)));
  record("mulino_evidence_DocumentVersions",document,"recordedBy",actor,"subjectKind","ITEM","subjectId",item,"itemId",item,"availability","UNKNOWN","sha256","c".repeat(64),"byteLength",0L,"mediaType","text/plain","sourceProfileId",profile,"provenance","{}");
  trade("mulino_trade_purchase_Suppliers",supplier,T.minusSeconds(10),"name","supplier");
  for(String type:List.of("PurchaseProposal","PurchaseOrder","Invoice","RegulatoryProcedure"))nouns.put(type,new ArrayList<>());
  for(int i=0;i<5;i++)seedBusiness(i,i==1?otherItem:item,i==2?otherWork:work,i==4?K.plusSeconds(10):T.minusSeconds(10));
  login();
 }
 void login(){SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("trusted-test-only").header("alg","RS256").issuer("https://mulino.local.invalid").subject(actor).claim("organizationId",org).build(),List.of()));}
 void scope(String kind,String value){insert("mulino_identity_GrantScopes",row("organizationId",org,"grantId",grant,"scopeKind",kind,"scopeId",value));}
 void seedBusiness(int index,String it,String w,Instant at){seedBusiness(index,it,w,at,at,T.minusSeconds(10),place);}
 void seedBusiness(int index,String it,String w,Instant revisionAt,Instant lineAt,Instant orderAt,String destination){
  // UUID lexical position is stable; hidden candidates deliberately sit between visible rows.
  String prefix=org.substring(0,8),tail=String.format("%012d",index+1);
  String proposal=prefix+"-0001-0000-0000-"+tail,order=prefix+"-0002-0000-0000-"+tail,invoice=prefix+"-0003-0000-0000-"+tail,proc=prefix+"-0004-0000-0000-"+tail;
  nouns.get("PurchaseProposal").add(proposal);nouns.get("PurchaseOrder").add(order);nouns.get("Invoice").add(invoice);nouns.get("RegulatoryProcedure").add(proc);
  trade("mulino_trade_purchase_Proposals",proposal,T.minusSeconds(10),"workId",w,"currentRevision",1,"status","APPROVED");
  String revision=id();trade("mulino_trade_purchase_ProposalRevisions",revision,revisionAt,"proposalId",proposal,"workId",w,"itemId",it,"supplierId",supplier,"destinationId",destination,"quantity",10,"unit","BOX","price",1,"currency","EUR","dueAt",Timestamp.from(T.plusSeconds(60)),"endpoint","ARRIVED","quantityMode","CUMULATIVE_EVENT","proposalHash","a".repeat(64));
  String approval=id();trade("mulino_trade_purchase_Approvals",approval,revisionAt,"proposalId",proposal,"proposalRevision",1,"proposalHash","a".repeat(64),"approverId",actor,"policyHash","b".repeat(64),"consumptionPolicy","SINGLE_ORDER_REVISION","decision","APPROVE","conditionsJson","[]","decidedAt",Timestamp.from(T.minusSeconds(10)),"validUntil",Timestamp.from(T.plusSeconds(60)));
  trade("mulino_trade_purchase_Orders",order,orderAt,"proposalId",proposal,"proposalRevision",1,"proposalHash","a".repeat(64),"approvalId",approval,"channel","synthetic","externalOperationId",id(),"outboxId",id(),"conditionAssessmentJson","{}");
  String line=id();trade("mulino_trade_purchase_OrderLines",line,lineAt,"orderId",order,"proposalId",proposal,"proposalRevision",1,"workId",w,"itemId",it,"destinationId",destination,"quantity",10,"unit","BOX","status","DISPATCH_PENDING");
  trade("mulino_trade_settlement_Invoices",invoice,T.minusSeconds(10),"workId",w,"itemId",it,"lineId",line,"referenceId",id(),"scopeKind","PURCHASE","invoiceKind","COMMERCIAL","sourceNamespace","discovery-test","sourceKey",id(),"sourceVersion","1","sourceHash","c".repeat(64),"evidenceId",document,"quantity",10,"unit","BOX","unitPrice",1,"originalAmount",10,"currency","EUR");
  record("mulino_trade_regulatory_Procedures",proc,"recordedBy",actor,"itemId",it,"physicalScopeId",segment,"workId",w);
  var v=base(id());v.putAll(row("recordedAt",Timestamp.from(revisionAt),"recordedBy",actor,"procedureId",proc,"itemId",it,"physicalScopeId",segment,"workId",w,"status","PREPARING","documentVersionId",document));insert("mulino_trade_regulatory_ProcedureVersions",v);
 }
 QueryRequest request(String type,String target,String cursor,int limit,Instant known,Map<String,Object> extra){var scope=new LinkedHashMap<String,Object>(Map.of("organizationId",org,"objectType",type));scope.putAll(extra);return new QueryRequest(target==null?"searchObjects":"getObject",target,scope,Map.of("sort","ID"),limit,cursor,null,T,known,null);}
 Map<String,Object> read(QueryRequest q){return runtime.requestContext().run(ctx->{return queries.query(q);});}
 List<Map<String,Object>> rows(Map<String,Object> reply){return (List<Map<String,Object>>)reply.get("data");}
 List<String> ids(Map<String,Object> reply){return rows(reply).stream().map(x->x.get("ID").toString()).toList();}
 void denied(String type,String target){assertEquals("FORBIDDEN",assertThrows(DomainError.class,()->read(request(type,target,null,1,K,Map.of()))).code());}
 @ParameterizedTest @ValueSource(strings={"PurchaseProposal","PurchaseOrder","Invoice","RegulatoryProcedure"})
 void currentAllOfPagesContainOnlyVisibleTargetsAndBoundCursor(String type){
  var q=request(type,null,null,1,K,Map.of());var first=read(q);String cursor=(String)first.get("nextCursor");assertNotNull(cursor);assertEquals(List.of(nouns.get(type).get(0)),ids(first));assertEquals(first,read(q));
  String raw=new String(Base64.getUrlDecoder().decode(cursor),java.nio.charset.StandardCharsets.UTF_8);assertTrue(raw.endsWith(nouns.get(type).get(0)));assertFalse(raw.contains(nouns.get(type).get(1)));
  var second=read(request(type,null,cursor,1,K,Map.of()));assertEquals(List.of(nouns.get(type).get(3)),ids(second));assertNull(second.get("nextCursor"));
  for(int index:List.of(1,2,4))denied(type,nouns.get(type).get(index));denied(type,id());
  assertEquals("TYPE_INVALID",assertThrows(DomainError.class,()->read(request(type,null,cursor,1,K.plusSeconds(20),Map.of()))).code());
  assertEquals("TYPE_INVALID",assertThrows(DomainError.class,()->read(request(type,null,cursor,1,K,Map.of("itemId",item)))).code());
  assertEquals(List.of(nouns.get(type).get(0),nouns.get(type).get(3),nouns.get(type).get(4)),ids(read(request(type,null,null,10,K.plusSeconds(20),Map.of()))));
  var foreign=request(type,null,null,10,K,Map.of("organizationId",id()));assertEquals("FORBIDDEN",assertThrows(DomainError.class,()->read(foreign)).code());
  // Cursor is only a position; revoking current authority cannot preserve its old page access.
  db.update("UPDATE mulino_identity_Grants SET revokedAt=? WHERE organizationId=? AND ID=?",Timestamp.from(T),org,grant);
  assertEquals("FORBIDDEN",assertThrows(DomainError.class,()->read(request(type,null,cursor,1,K,Map.of()))).code());
 }
 @Test void invoiceJoinedLineVisibilityAndScopesComeFromCurrentServerFacts(){
  scope("PLACE",place);var page=read(request("Invoice",null,null,10,K,Map.of()));assertEquals(2,rows(page).size());var actual=(Map<String,List<String>>)rows(page).getFirst().get("readScope");assertEquals(List.of(place),actual.get("PLACE"));assertEquals(List.of(supplier),actual.get("SUPPLIER"));assertEquals(List.of(item),actual.get("ITEM"));
  Instant past=T.minusSeconds(10),future=K.plusSeconds(10);
  seedBusiness(5,item,work,past,past,past,otherPlace);
  seedBusiness(6,item,work,future,past,past,place);
  seedBusiness(7,item,work,past,future,past,place);
  seedBusiness(8,item,work,past,past,future,place);
  for(int index:List.of(4,5,6,7,8))denied("Invoice",nouns.get("Invoice").get(index));
  assertEquals(ids(page),ids(read(request("Invoice",null,null,20,K,Map.of()))));
  var knownLater=ids(read(request("Invoice",null,null,20,K.plusSeconds(20),Map.of())));
  for(int index:List.of(4,6,7,8))assertTrue(knownLater.contains(nouns.get("Invoice").get(index)));
  assertFalse(knownLater.contains(nouns.get("Invoice").get(5)));

 }
 @Test void hiddenCorrectionJoinedLineCannotLeakThroughVisibleInvoice(){
  scope("PLACE",place);Instant past=T.minusSeconds(10);seedBusiness(5,item,work,past,past,past,otherPlace);
  var copy=new LinkedHashMap<>(db.queryForMap("SELECT * FROM mulino_trade_settlement_Invoices WHERE organizationId=? AND ID=?",org,nouns.get("Invoice").get(5)));
  String hiddenCorrection=id();copy.put("id",hiddenCorrection);copy.put("invoicekind","CORRECTION");copy.put("relatedinvoiceid",nouns.get("Invoice").get(0));copy.put("sourcekey",id());insert("mulino_trade_settlement_Invoices",copy);
  denied("Invoice",hiddenCorrection);denied("Invoice",nouns.get("Invoice").get(0));
  var page=read(request("Invoice",null,null,10,K,Map.of()));assertEquals(List.of(nouns.get("Invoice").get(3)),ids(page));assertFalse(page.toString().contains(hiddenCorrection));
 }

 @Test void distinctSearchAndObjectCapabilitiesAreNotSubstitutedAndBadQueriesKeepErrors(){
  db.update("DELETE FROM mulino_identity_GrantActions WHERE organizationId=? AND grantId=? AND capabilityId='searchObjects'",org,grant);
  for(String type:nouns.keySet())assertEquals("FORBIDDEN",assertThrows(DomainError.class,()->read(request(type,null,null,2,K,Map.of()))).code());
  db.update("INSERT INTO mulino_identity_GrantActions(organizationId,grantId,capabilityId) VALUES(?,?,'searchObjects')",org,grant);
  for(String type:nouns.keySet()){
   var q=request(type,null,null,2,K,Map.of());var invalid=new QueryRequest(q.operation(),null,q.scope(),Map.of("sort","quantity"),2,null,null,T,K,null);
   assertEquals("TYPE_INVALID",assertThrows(DomainError.class,()->read(invalid)).code());
   assertEquals("TYPE_INVALID",assertThrows(DomainError.class,()->read(request(type,null,"forged",2,K,Map.of()))).code());
  }
 }
 @Test void pagedDiscoveryAndNounVerbProviderUseTheSameBusinessProjection(){
  for(String type:nouns.keySet()){
   var q=request(type,null,null,10,K,Map.of());var first=rows(read(q)).getFirst();
   QueryHandler handler=type.equals("Invoice")?settlement:type.equals("RegulatoryProcedure")?regulatory:purchase;
   String operation=type.equals("Invoice")?"getSettlement":type.equals("RegulatoryProcedure")?"getRegulatoryProcedure":"getPurchase";
   var direct=new QueryRequest(operation,nouns.get(type).getFirst(),q.scope(),q.filters(),10,null,null,T,K,null);
   var result=runtime.requestContext().run(ctx->{return handler.query(auth.context(T,K),direct);});
   assertEquals(first,TransportValues.normalize(result.data()));
  }
 }
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @AfterAll static void stop()throws Exception{HOST.close();}
}
