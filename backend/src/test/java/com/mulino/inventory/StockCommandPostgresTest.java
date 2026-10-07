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
class StockCommandPostgresTest {
 static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280");
 static {PG.start();}
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
  r.add("spring.datasource.url",PG::getJdbcUrl);r.add("spring.datasource.username",PG::getUsername);r.add("spring.datasource.password",PG::getPassword);
  r.add("JWT_PUBLIC_KEY",()->System.getenv("JWT_PUBLIC_KEY"));r.add("JWT_ISSUER",()->"https://mulino.local.invalid");r.add("JWT_AUDIENCE",()->"mulino-platform");
 }
 @Autowired JdbcTemplate jdbc;
 @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
 @Autowired CdsRuntime runtime;
 @Autowired InventoryRepository repository;
 @Autowired com.mulino.domain.inventory.StockPrimitives stock;
 @Autowired com.mulino.application.inventory.InventoryCommands commands;
 @Autowired com.mulino.application.inventory.ItemCommands metadata;
 @Autowired com.mulino.application.inventory.InventoryRestrictionGuard restrictions;
 final String source=id(200);
 @MockitoBean IdentityAuthorization authorizer;
 @MockitoBean org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;
 final String org=id(1),other=id(2),product=id(10),item=id(11),spec=id(12),pack=id(13),manufacturer=id(14),lot=id(15),place=id(16),parent=id(20),left=id(21),right=id(22);
 final Instant at=Instant.parse("2026-10-08T00:00:00Z");
 static String id(int n){return "00000000-0000-0000-0000-"+String.format("%012d",n);}
 DomainContext context(){return new DomainContext(org,id(3),id(3),at,at);}
 @BeforeEach void seed(){
  new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(status->{
  jdbc.execute("TRUNCATE mulino_identity_Organizations CASCADE");
  jdbc.update("INSERT INTO mulino_identity_Organizations(id,externalAlias) VALUES (?,?),(?,?)",org,"inventory-a",other,"inventory-b");
  jdbc.update("INSERT INTO mulino_inventory_Products(organizationId,id,name,createdAt,recordedAt) VALUES (?,?,?,'2026-01-01','2026-01-01')",org,product,"biscuit");
  for(String table:List.of("SpecificationVersions","PackagingVersions"))jdbc.update("INSERT INTO mulino_inventory_"+table+"(organizationId,id,productId,version,contentHash,createdAt,recordedAt) VALUES (?,?,?,'1',?,'2026-01-01','2026-01-01')",org,table.startsWith("Spec")?spec:pack,product,"a".repeat(64));
  jdbc.update("INSERT INTO mulino_inventory_TradeItems(organizationId,id,productId,name,baseUnit,decimalPlaces,specificationVersionId,packagingVersionId,createdAt,recordedAt) VALUES (?,?,?,'biscuit','EA',0,?,?,'2026-01-01','2026-01-01')",org,item,product,spec,pack);
  jdbc.update("INSERT INTO mulino_inventory_Manufacturers(organizationId,id,name,createdAt,recordedAt) VALUES (?,?,'M','2026-01-01','2026-01-01')",org,manufacturer);
  jdbc.update("INSERT INTO mulino_inventory_ManufacturingLots(organizationId,id,manufacturerId,itemId,originalLot,createdAt,recordedAt) VALUES (?,?,?,?,'LOT-A','2026-01-01','2026-01-01')",org,lot,manufacturer,item);
  jdbc.update("INSERT INTO mulino_inventory_Places(organizationId,id,name,kind,createdAt,recordedAt) VALUES (?,?,'W','WAREHOUSE','2026-01-01','2026-01-01')",org,place);
  segment(parent,"100",true);segment(left,"40",false);segment(right,"60",false);segment(source,"100",false);
  jdbc.update("INSERT INTO mulino_identity_Actors(organizationId,id,kind,stableRequestOwner) VALUES (?,?,'HUMAN',?)",org,id(3),id(3));
  jdbc.update("UPDATE mulino_inventory_Places SET kind='INTERNAL_STORAGE' WHERE organizationId=? AND id=?",org,place);
  edge(id(30),parent,left,"40");edge(id(31),parent,right,"60");
  });
  when(authorizer.permittedScopes(any(),anyString(),anyMap())).thenReturn(true);
 }
 void segment(String segment,String quantity,boolean retired){
  jdbc.update("INSERT INTO mulino_inventory_QuantitySegments(organizationId,id,itemId,lotId,identificationStatus,quantity,unit,placeId,controlScope,validFrom,retiredAt,retirementRecordedAt,mixtureStatus,createdAt,recordedAt) VALUES (?,?,?,?,'CONFIRMED',?::numeric,'EA',?,'warehouse',?::timestamptz,?::timestamptz,?::timestamptz,'IDENTIFIED',?::timestamptz,?::timestamptz')",org,segment,item,lot,quantity,place,(segment.equals(left)||segment.equals(right))?"2026-02-01":"2026-01-01",retired?"2026-02-01":null,retired?"2026-02-01":null,(segment.equals(left)||segment.equals(right))?"2026-02-01":"2026-01-01",(segment.equals(left)||segment.equals(right))?"2026-02-01":"2026-01-01");
 }
 void edge(String edge,String source,String target,String quantity){jdbc.update("INSERT INTO mulino_inventory_GenealogyEdges(organizationId,id,sourceId,targetId,quantity,unit,kind,uncertain,occurredAt,createdAt,recordedAt) VALUES (?,?,?,?,?::numeric,'EA','SPLIT',false,'2026-02-01','2026-02-01','2026-02-01')",org,edge,source,target,quantity);}
 QueryRequest query(String operation,String id,Map<String,Object> scope){return new QueryRequest(operation,id,scope,Map.of(),50,null,"v1",at,at,null);}
 <T> T tx(java.util.function.Supplier<T> action){return new org.springframework.transaction.support.TransactionTemplate(transactionManager).execute(status->runtime.requestContext().run(ctx->{return action.get();}));}
 Map<String,Object> intent(String capability,Map<String,Object> slots){return Map.of("capabilityId",capability,"intentKind",capability.equals("recordStocktake")?"RECORD":"COMMAND","commandId",id(300),"slots",slots);}
 Map<String,Object> splitIntent(){return intent("splitQuantity",Map.of("segmentId",source,"quantities",List.of("60","40"),"unit","EA","occurredAt",at.toString(),"evidenceRef","split-witness"));}
 @Test void split60and40RetiresParentAndRejectsSecondConsumption(){
  var result=tx(()->commands.execute(context(),splitIntent()));var effects=(Map<?,?>)result.get("effects");
  assertEquals(2,((List<?>)effects.get("segmentIds")).size());
  assertEquals("100.000000000000",jdbc.queryForObject("SELECT SUM(quantity)::text FROM mulino_inventory_QuantitySegments WHERE organizationId=? AND id IN (SELECT targetId FROM mulino_inventory_GenealogyEdges WHERE organizationId=? AND sourceId=?)",String.class,org,org,source));
  assertNotNull(jdbc.queryForObject("SELECT retiredAt FROM mulino_inventory_QuantitySegments WHERE organizationId=? AND id=?",java.sql.Timestamp.class,org,source));
  assertThrows(DomainError.class,()->tx(()->commands.execute(context(),splitIntent())));
  assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM mulino_inventory_GenealogyEdges WHERE sourceId=?",Integer.class,source));
 }
 @Test void allocation40TransfersExactlyOnceAndLeaves60Unallocated(){
  jdbc.update("INSERT INTO mulino_inventory_SegmentAllocations(organizationId,id,createdAt,recordedAt,rootId,segmentId,orderLineId,quantity,unit,state,commandId) VALUES (?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,?,?,?,40,'EA','EXECUTABLE',?)",org,id(301),id(302),source,id(303),id(304));
  tx(()->commands.execute(context(),splitIntent()));
  assertEquals("40.000000000000",jdbc.queryForObject("SELECT SUM(quantity)::text FROM mulino_inventory_SegmentAllocations WHERE state='EXECUTABLE'",String.class));
  assertEquals("REPLACED",jdbc.queryForObject("SELECT state FROM mulino_inventory_SegmentAllocations WHERE id=?",String.class,id(301)));
  assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM mulino_inventory_SegmentAllocations WHERE segmentId=? AND state='EXECUTABLE'",Integer.class,source));
 }
 @Test void splitMergeKeepsConservationAndRejectsCrossPlaceOrControl(){
  var children=tx(()->stock.split(context(),source,List.of("60","40"),"EA",at,"w",id(300)));
  String combined=tx(()->stock.merge(context(),children,at,"w",id(305)));
  assertEquals("100.000000000000",jdbc.queryForObject("SELECT quantity::text FROM mulino_inventory_QuantitySegments WHERE id=?",String.class,combined));
  jdbc.update("INSERT INTO mulino_inventory_Places(organizationId,id,name,kind) VALUES (?,?,'Other','INTERNAL_STORAGE')",org,id(311));
  jdbc.update("INSERT INTO mulino_inventory_QuantitySegments(organizationId,id,itemId,lotId,identificationStatus,quantity,unit,placeId,controlScope,validFrom,mixtureStatus) VALUES (?,?,?,?,'CONFIRMED',1,'EA',?,'warehouse','2026-01-01','IDENTIFIED')",org,id(312),item,lot,id(311));
  assertThrows(DomainError.class,()->tx(()->stock.merge(context(),List.of(combined,id(312)),at,"w",id(306))));
  // Existing S1 leaf has another control scope after an explicit new fixture insertion.
  jdbc.update("INSERT INTO mulino_inventory_QuantitySegments(organizationId,id,itemId,lotId,identificationStatus,quantity,unit,placeId,controlScope,validFrom,mixtureStatus) VALUES (?,?,?,?,'CONFIRMED',1,'EA',?,'other-control','2026-01-01','IDENTIFIED')",org,id(310),item,lot,place);
  assertThrows(DomainError.class,()->tx(()->stock.merge(context(),List.of(combined,id(310)),at,"w",id(306))));
 }
 @Test void fractionalDecimalAndCrossOrgFailWithoutPhysicalEffect(){
  var invalid=intent("splitQuantity",Map.of("segmentId",source,"quantities",List.of("0.5","99.5"),"unit","EA","occurredAt",at.toString(),"evidenceRef","w"));
  assertThrows(DomainError.class,()->tx(()->commands.execute(context(),invalid)));
  assertThrows(DomainError.class,()->tx(()->commands.execute(new DomainContext(other,id(3),id(3),at,at),splitIntent())));
  assertNull(jdbc.queryForObject("SELECT retiredAt FROM mulino_inventory_QuantitySegments WHERE id=?",java.sql.Timestamp.class,source));
 }
 @Test void stocktakeIsObservationDisposalAloneDecreases(){
  String count=tx(()->stock.stocktake(context(),source,"90","EA",at,"count-witness",id(300)));
  assertEquals("90.000000000000",jdbc.queryForObject("SELECT observedQuantity::text FROM mulino_inventory_Stocktakes WHERE id=?",String.class,count));
  assertEquals("100.000000000000",jdbc.queryForObject("SELECT quantity::text FROM mulino_inventory_QuantitySegments WHERE id=?",String.class,source));
  var remaining=tx(()->stock.decrease(context(),source,"10","EA",at,"approved-disposal",id(305),"DISPOSE"));
  assertEquals("90.000000000000",jdbc.queryForObject("SELECT quantity::text FROM mulino_inventory_QuantitySegments WHERE id=?",String.class,remaining.getFirst()));
 }
 @Test void failedLaterWriteRollsBackEveryStockEffect(){
  assertThrows(IllegalStateException.class,()->tx(()->{stock.split(context(),source,List.of("60","40"),"EA",at,"w",id(300));throw new IllegalStateException("audit failure");}));
  assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM mulino_inventory_GenealogyEdges WHERE sourceId=?",Integer.class,source));
  assertNull(jdbc.queryForObject("SELECT retiredAt FROM mulino_inventory_QuantitySegments WHERE id=?",java.sql.Timestamp.class,source));
 }
 @Test void concurrentConsumptionSerializesAndOnlyOneCommits() throws Exception {
  var pool=java.util.concurrent.Executors.newFixedThreadPool(2);var barrier=new java.util.concurrent.CyclicBarrier(2);
  try{
   java.util.concurrent.Callable<Boolean> job=()->{barrier.await();try{tx(()->commands.execute(context(),splitIntent()));return true;}catch(DomainError e){return false;}};
   var a=pool.submit(job);var b=pool.submit(job);assertNotEquals(a.get(15,java.util.concurrent.TimeUnit.SECONDS),b.get(15,java.util.concurrent.TimeUnit.SECONDS));
   assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM mulino_inventory_GenealogyEdges WHERE sourceId=?",Integer.class,source));
  }finally{pool.shutdownNow();}
 }
 @Test void uncertainMixtureNeverBecomesCleanSubset(){
  jdbc.update("UPDATE mulino_inventory_QuantitySegments SET mixtureStatus='UNCERTAIN_MIXTURE' WHERE id=?",source);
  var children=tx(()->stock.split(context(),source,List.of("60","40"),"EA",at,"mixture",id(300)));
  assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM mulino_inventory_GenealogyEdges WHERE sourceId=? AND uncertain=true",Integer.class,source));
  assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM mulino_inventory_QuantitySegments WHERE id IN (?,?) AND mixtureStatus='UNCERTAIN_MIXTURE'",Integer.class,children.get(0),children.get(1)));
 }
 @Test void internalMoveRejectsCustomerDestinationAndUnconfirmedCustody(){
  String destination=id(320);jdbc.update("INSERT INTO mulino_inventory_Places(organizationId,id,name,kind) VALUES (?,?,'customer','CUSTOMER')",org,destination);
  assertThrows(DomainError.class,()->tx(()->stock.moveInternal(context(),source,destination,at,"w",id(300))));
  jdbc.update("UPDATE mulino_inventory_Places SET kind='INTERNAL_STORAGE' WHERE id=?",destination);
  assertThrows(DomainError.class,()->tx(()->stock.moveInternal(context(),source,destination,at,"w",id(300))));
 }
 @Test void externalCodeConflictDoesNotMergeItems(){
  var registration=intent("registerItem",Map.of("name","same name","baseUnit","EA","decimalPlaces",0,"specificationHash","b".repeat(64),"packagingHash","c".repeat(64)));
  var result=tx(()->metadata.execute(context(),registration));String second=(String)((Map<?,?>)result.get("effects")).get("itemId");assertNotEquals(item,second);
  var base=new HashMap<String,Object>(Map.of("itemId",item,"issuer","supplier","namespace","SKU","value","SAME","validFrom","2026-01-01T00:00:00Z"));
  tx(()->metadata.execute(context(),intent("linkExternalId",base)));base.put("itemId",second);
  var conflict=tx(()->metadata.execute(context(),intent("linkExternalId",base)));
  assertEquals("ACCEPTED_PENDING_RECONCILIATION",conflict.get("outcome"));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM mulino_inventory_ExternalIdentifiers",Integer.class));
 }

 @Test void restrictionInsertionSharesFenceAndCommittedRestrictionDeniesEffect() throws Exception {
  var pool=java.util.concurrent.Executors.newFixedThreadPool(2);var locked=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);var attempted=new java.util.concurrent.CountDownLatch(1);
  try {
   var first=pool.submit(()->tx(()->{
    repository.fence(context(),List.of("inventory/control/warehouse"));locked.countDown();
    try{if(!release.await(10,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("barrier timeout");}catch(InterruptedException e){throw new IllegalStateException(e);}
    return true;
   }));
   assertTrue(locked.await(10,java.util.concurrent.TimeUnit.SECONDS));
   var second=pool.submit(()->tx(()->{
    attempted.countDown();jdbc.update("INSERT INTO mulino_inventory_Restrictions(organizationId,id,createdAt,recordedAt,controlScope,action,state,validFrom,decisionId,evidenceRef) VALUES (?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'warehouse','DISPOSE','ACTIVE','2026-01-01',?,'qc-evidence')",org,id(330),id(331));return true;
   }));
   assertTrue(attempted.await(10,java.util.concurrent.TimeUnit.SECONDS));
   assertThrows(java.util.concurrent.TimeoutException.class,()->second.get(150,java.util.concurrent.TimeUnit.MILLISECONDS));
   release.countDown();assertTrue(first.get(10,java.util.concurrent.TimeUnit.SECONDS));assertTrue(second.get(10,java.util.concurrent.TimeUnit.SECONDS));
   var dispose=intent("disposeQuantity",Map.of("segmentId",source,"quantity","10","unit","EA","reason","damage","occurredAt",at.toString(),"evidenceRef","w"));
   assertThrows(DomainError.class,()->tx(()->{var prepared=commands.prepare(context(),dispose);repository.fence(context(),prepared.fenceKeys());restrictions.verify(context(),"disposeQuantity","hash",prepared,dispose);return commands.execute(context(),dispose);}));
   assertNull(jdbc.queryForObject("SELECT retiredAt FROM mulino_inventory_QuantitySegments WHERE id=?",java.sql.Timestamp.class,source));
   assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM mulino_inventory_QuantityMovements WHERE sourceId=?",Integer.class,source));
  } finally {release.countDown();pool.shutdownNow();}
 }
}
