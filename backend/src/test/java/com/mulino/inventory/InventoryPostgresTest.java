package com.mulino.inventory;

import static org.junit.jupiter.api.Assertions.*;
import com.mulino.application.core.*;
import com.mulino.application.inventory.InventoryQueries;
import com.mulino.domain.inventory.InventoryRepository;
import com.mulino.application.identity.IdentityAuthorization;
import com.sap.cds.services.runtime.CdsRuntime;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("local")
class InventoryPostgresTest {
 static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280");
 static {PG.start();}
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
  r.add("spring.datasource.url",PG::getJdbcUrl);r.add("spring.datasource.username",PG::getUsername);r.add("spring.datasource.password",PG::getPassword);
  r.add("JWT_PUBLIC_KEY",()->System.getenv("JWT_PUBLIC_KEY"));r.add("JWT_ISSUER",()->"https://mulino.local.invalid");r.add("JWT_AUDIENCE",()->"mulino-platform");
 }
 @Autowired JdbcTemplate jdbc;
 @Autowired CdsRuntime runtime;
 @Autowired InventoryRepository repository;
 @MockitoBean IdentityAuthorization authorizer;
 @MockitoBean org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;
 final String org=id(1),other=id(2),product=id(10),item=id(11),spec=id(12),pack=id(13),manufacturer=id(14),lot=id(15),place=id(16),parent=id(20),left=id(21),right=id(22);
 final Instant at=Instant.parse("2026-10-08T00:00:00Z");
 static String id(int n){return "00000000-0000-0000-0000-"+String.format("%012d",n);}
 DomainContext context(){return new DomainContext(org,id(3),id(3),at,at);}
 @BeforeEach void seed(){
  jdbc.execute("TRUNCATE mulino_identity_Organizations CASCADE");
  jdbc.update("INSERT INTO mulino_identity_Organizations(id,externalAlias) VALUES (?,?),(?,?)",org,"inventory-a",other,"inventory-b");
  jdbc.update("INSERT INTO mulino_inventory_Products(organizationId,id,name,createdAt,recordedAt) VALUES (?,?,?,'2026-01-01','2026-01-01')",org,product,"biscuit");
  for(String table:List.of("SpecificationVersions","PackagingVersions"))jdbc.update("INSERT INTO mulino_inventory_"+table+"(organizationId,id,productId,version,contentHash,createdAt,recordedAt) VALUES (?,?,?,'1',?,'2026-01-01','2026-01-01')",org,table.startsWith("Spec")?spec:pack,product,"a".repeat(64));
  jdbc.update("INSERT INTO mulino_inventory_TradeItems(organizationId,id,productId,name,baseUnit,decimalPlaces,specificationVersionId,packagingVersionId,createdAt,recordedAt) VALUES (?,?,?,'biscuit','EA',0,?,?,'2026-01-01','2026-01-01')",org,item,product,spec,pack);
  jdbc.update("INSERT INTO mulino_inventory_Manufacturers(organizationId,id,name,createdAt,recordedAt) VALUES (?,?,'M','2026-01-01','2026-01-01')",org,manufacturer);
  jdbc.update("INSERT INTO mulino_inventory_ManufacturingLots(organizationId,id,manufacturerId,itemId,originalLot,createdAt,recordedAt) VALUES (?,?,?,?,'LOT-A','2026-01-01','2026-01-01')",org,lot,manufacturer,item);
  jdbc.update("INSERT INTO mulino_inventory_Places(organizationId,id,name,kind,createdAt,recordedAt) VALUES (?,?,'W','WAREHOUSE','2026-01-01','2026-01-01')",org,place);
  segment(parent,"100",true);segment(left,"40",false);segment(right,"60",false);
  jdbc.update("UPDATE mulino_inventory_QuantitySegments SET validFrom='2026-02-01',createdAt='2026-02-01',recordedAt='2026-02-01' WHERE organizationId=? AND id IN (?,?)",org,left,right);
  edge(id(30),parent,left,"40");edge(id(31),parent,right,"60");
  when(authorizer.permittedScopes(any(),anyString(),anyMap())).thenReturn(true);
 }
 void segment(String segment,String quantity,boolean retired){
  jdbc.update("INSERT INTO mulino_inventory_QuantitySegments(organizationId,id,itemId,lotId,identificationStatus,quantity,unit,placeId,controlScope,validFrom,retiredAt,retirementRecordedAt,mixtureStatus,createdAt,recordedAt) VALUES (?,?,?,?,'CONFIRMED',?::numeric,'EA',?,'warehouse','2026-01-01',?::timestamptz,?::timestamptz,'IDENTIFIED','2026-01-01','2026-01-01')",org,segment,item,lot,quantity,place,retired?"2026-02-01":null,retired?"2026-02-01":null);
 }
 void edge(String edge,String source,String target,String quantity){jdbc.update("INSERT INTO mulino_inventory_GenealogyEdges(organizationId,id,sourceId,targetId,quantity,unit,kind,uncertain,occurredAt,createdAt,recordedAt) VALUES (?,?,?,?,?::numeric,'EA','SPLIT',false,'2026-02-01','2026-02-01','2026-02-01')",org,edge,source,target,quantity);}
 QueryRequest query(String operation,String id,Map<String,Object> scope){return new QueryRequest(operation,id,scope,Map.of(),50,null,"v1",at,at,null);}
 @Test void cqnActiveLeavesIndependentSqlSumAndLineage(){runtime.requestContext().run(ctx->{
  var queries=new InventoryQueries(repository,authorizer);
  Map<String,Object> data=(Map<String,Object>)queries.query(context(),query("getInventory",item,Map.of())).data();
  assertEquals("100",data.get("heldQuantity"));assertNull(data.get("eligibleQuantity"));
  assertEquals(Set.of(left,right),new HashSet<>((List<String>)data.get("segmentIds")));
  assertEquals("100.000000000000",jdbc.queryForObject("SELECT SUM(quantity)::text FROM mulino_inventory_QuantitySegments WHERE organizationId=? AND retiredAt IS NULL",String.class,org));
  Map<String,Object> trace=(Map<String,Object>)queries.query(context(),query("getTrace",parent,Map.of())).data();
  assertEquals(Set.of(left,right),new HashSet<>((List<String>)trace.get("activeLeafIds")));
  assertEquals(2,((List<?>)trace.get("edges")).size());return null;
 });}
 @Test void orgCompositeFkAndBaseUnitPrecisionReject(){
  assertThrows(RuntimeException.class,()->jdbc.update("INSERT INTO mulino_inventory_ManufacturingLots(organizationId,id,manufacturerId,itemId,originalLot) VALUES (?,?,?,?,'LOT-A')",other,id(50),manufacturer,item));
  assertThrows(RuntimeException.class,()->segment(id(51),"0.5",false));
  assertThrows(RuntimeException.class,()->edge(id(52),left,right,"1"));
  assertThrows(RuntimeException.class,()->edge(id(53),parent,left,"1"));
 }
 @Test void namespacedExternalIdTimeAndLotManufacturerContext(){
  String insert="INSERT INTO mulino_inventory_ExternalIdentifiers(organizationId,id,itemId,issuer,namespace,value,validFrom,validUntil) VALUES (?,?,?,?,'SKU','SAME',?::timestamptz,?::timestamptz)";
  jdbc.update(insert,org,id(60),item,"issuer-a","2026-01-01","2026-02-01");
  jdbc.update(insert,org,id(61),item,"issuer-b","2026-01-01","2026-02-01");
  jdbc.update(insert,org,id(62),item,"issuer-a","2026-02-01","2026-03-01");
  assertThrows(RuntimeException.class,()->jdbc.update(insert,org,id(63),item,"issuer-a","2026-01-15","2026-02-15"));
  jdbc.update("INSERT INTO mulino_inventory_Manufacturers(organizationId,id,name) VALUES (?,?,'M2')",org,id(64));
  jdbc.update("INSERT INTO mulino_inventory_ManufacturingLots(organizationId,id,manufacturerId,itemId,originalLot) VALUES (?,?,?,?,'LOT-A')",org,id(65),id(64),item);
  assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM mulino_inventory_ManufacturingLots",Integer.class));
 }
 @Test void uncertainMixturePreservesCandidateAndCrossOrgReadDenied(){
  jdbc.update("UPDATE mulino_inventory_QuantitySegments SET mixtureStatus='UNCERTAIN_MIXTURE' WHERE organizationId=? AND id=?",org,left);
  runtime.requestContext().run(ctx->{
   var queries=new InventoryQueries(repository,authorizer);
   Map<String,Object> trace=(Map<String,Object>)queries.query(context(),query("getTrace",parent,Map.of())).data();
   assertEquals("CANDIDATE_UNCERTAIN_MIXTURE",trace.get("impactStatus"));assertEquals(false,trace.get("cleanSubsetSelectable"));
   assertThrows(DomainError.class,()->repository.object(new DomainContext(other,id(3),id(3),at,at),"TradeItems",item));return null;
  });
 }
 @Test void publishedTypedRelationsEnforceEndpointsPeriodCardinalityAndCycles(){
  String version=id(70),relation=id(71),hierarchy=id(72),secondPlace=id(73);
  jdbc.update("INSERT INTO mulino_definitions_DefinitionVersions(organizationId,id,version,state,contentHash,content,evaluatorVersion,schemaVersion) VALUES (?,?,'inventory-relations-v1','DRAFT',?,'{}','core-v1','1.0.0')",org,version,"a".repeat(64));
  jdbc.update("INSERT INTO mulino_definitions_RelationDefinitions(organizationId,id,definitionVersionId,name,sourceType,targetType,minimumCount,maximumCount,cycleAllowed) VALUES (?,?,?,'locatedAt','QuantitySegment','Place',0,1,false)",org,relation,version);
  jdbc.update("INSERT INTO mulino_definitions_RelationDefinitions(organizationId,id,definitionVersionId,name,sourceType,targetType,minimumCount,maximumCount,cycleAllowed) VALUES (?,?,?,'within','Place','Place',0,1,false)",org,hierarchy,version);
  jdbc.update("UPDATE mulino_definitions_DefinitionVersions SET state='PUBLISHED' WHERE organizationId=? AND id=?",org,version);
  jdbc.update("INSERT INTO mulino_inventory_Places(organizationId,id,name,kind) VALUES (?,?,'W2','WAREHOUSE')",org,secondPlace);
  String insert="INSERT INTO mulino_inventory_ObjectRelations(organizationId,id,definitionVersionId,relationDefinitionId,sourceType,sourceId,targetType,targetId,validFrom,validUntil) VALUES (?,?,?,?,?,?,?,?,?::timestamptz,?::timestamptz)";
  jdbc.update(insert,org,id(74),version,relation,"QuantitySegment",left,"Place",place,"2026-01-01","2026-02-01");
  jdbc.update(insert,org,id(75),version,relation,"QuantitySegment",left,"Place",place,"2026-02-01","2026-03-01");
  assertThrows(RuntimeException.class,()->jdbc.update(insert,org,id(76),version,relation,"QuantitySegment",left,"Place",place,"2026-01-15","2026-02-15"));
  assertThrows(RuntimeException.class,()->jdbc.update(insert,org,id(77),version,relation,"QuantitySegment",right,"Place",secondPlace,"2026-01-01",null));
  assertThrows(RuntimeException.class,()->jdbc.update(insert,org,id(78),version,relation,"Place",place,"QuantitySegment",left,"2026-01-01",null));
  assertThrows(RuntimeException.class,()->jdbc.update(insert,org,id(79),version,hierarchy,"Place",place,"Place",id(999),"2026-01-01",null));
  jdbc.update(insert,org,id(80),version,hierarchy,"Place",place,"Place",secondPlace,"2026-01-01",null);
  assertThrows(RuntimeException.class,()->jdbc.update(insert,org,id(81),version,hierarchy,"Place",secondPlace,"Place",place,"2026-01-01",null));
 }

 @Test void historicalSnapshotAndFilteredSearchKeepServerContext(){runtime.requestContext().run(ctx->{
  var queries=new InventoryQueries(repository,authorizer);
  Instant january=Instant.parse("2026-01-15T00:00:00Z");
  var historical=new DomainContext(org,id(3),id(3),january,january);
  Map<String,Object> before=(Map<String,Object>)queries.query(historical,query("getInventory",item,Map.of())).data();
  assertEquals("100",before.get("heldQuantity"));assertEquals(List.of(parent),before.get("segmentIds"));
  when(authorizer.permittedScopes(any(),anyString(),anyMap())).thenAnswer(call->{Map<String,Collection<String>> scopes=call.getArgument(2);return scopes.get("TARGET").contains(left);});
  Map<String,Object> visible=(Map<String,Object>)queries.query(context(),query("getInventory",item,Map.of())).data();
  assertEquals("40",visible.get("heldQuantity"));assertEquals(List.of(left),visible.get("segmentIds"));
  return null;
 });}
 @Test void logisticsMembershipCannotDoubleCountOrCrossOrg(){
  String container=id(90),container2=id(91);
  jdbc.update("INSERT INTO mulino_inventory_LogisticsUnits(organizationId,id,name) VALUES (?,?,'pallet'),(?,?,'pallet2')",org,container,org,container2);
  String insert="INSERT INTO mulino_inventory_LogisticsMemberships(organizationId,id,logisticsUnitId,segmentId,validFrom,validUntil) VALUES (?,?,?,?,?::timestamptz,?::timestamptz)";
  jdbc.update(insert,org,id(92),container,left,"2026-02-01","2026-03-01");
  jdbc.update(insert,org,id(93),container,right,"2026-02-01","2026-03-01");
  assertThrows(RuntimeException.class,()->jdbc.update(insert,org,id(94),container2,left,"2026-02-15","2026-03-01"));
  jdbc.update(insert,org,id(95),container2,left,"2026-03-01",null);
  assertThrows(RuntimeException.class,()->jdbc.update(insert,other,id(96),container,left,"2026-04-01",null));
 }

 @Test void lineageRejectsHistoricalParentChildOverlap(){
  segment(id(97),"1",false);
  assertThrows(RuntimeException.class,()->edge(id(98),parent,id(97),"1"));
  assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM mulino_inventory_GenealogyEdges",Integer.class));
 }

}
