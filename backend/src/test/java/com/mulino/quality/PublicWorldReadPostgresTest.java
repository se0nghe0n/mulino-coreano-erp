package com.mulino.quality;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import com.mulino.application.core.*;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Composes the real gateway/evidence fixture without inheriting unrelated tests. */
@SpringBootTest
@ActiveProfiles("local")
class PublicWorldReadPostgresTest {
 static final FulfillmentPostgresTest fixture = new FulfillmentPostgresTest();
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r){FulfillmentPostgresTest.properties(r);}
 @Autowired ApplicationContext context;
 @MockitoBean org.springframework.security.oauth2.jwt.JwtDecoder decoder;
 @MockitoBean ExecutionClock clock;

 @BeforeEach void seed(TestInfo info)throws Exception{
  context.getAutowireCapableBeanFactory().autowireBean(fixture);
  fixture.clock=clock;
  fixture.now=Instant.parse("2026-10-08T00:00:00Z");
  when(clock.instant()).thenAnswer(x->fixture.now);
  fixture.seed(info);
  fixture.saleFixture();
  System.out.println("public-world fixture pid="+ProcessHandle.current().pid()+" containerId="+FulfillmentPostgresTest.PG.getContainerId()+" blobRoot="+FulfillmentPostgresTest.BLOBS+" test="+info.getDisplayName());
 }
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 static {
  // Keep the shared container under Testcontainers ownership and the blob tree alive
  // for every class. JVM delete-on-exit runs after application shutdown hooks, so
  // PublicScopeEligibilityPostgresTest may still clean the same tree safely first.
  FulfillmentPostgresTest.BLOBS.toFile().deleteOnExit();
  Runtime.getRuntime().addShutdownHook(new Thread(()->{
   scheduleBlobCleanup(FulfillmentPostgresTest.BLOBS.toFile());
   System.out.println("public-world shutdown cleanup pid="+ProcessHandle.current().pid()+" containerId="+FulfillmentPostgresTest.PG.getContainerId()+" blobRoot="+FulfillmentPostgresTest.BLOBS);
  },"public-world-fixture-cleanup"));
 }
 private static void scheduleBlobCleanup(File path){
  // Parents register first because JVM deletion visits registrations in reverse.
  // Missing paths and repeated registration are harmless, including another
  // fixture's simultaneous shutdown cleanup of this shared directory.
  path.deleteOnExit();
  File[] children=path.listFiles();
  if(children!=null){Arrays.sort(children,Comparator.comparing(File::getName));for(File child:children)scheduleBlobCleanup(child);}
 }
 @SuppressWarnings("unchecked") Map<String,Object> world(Map<String,Object> extra){
  var scope=fixture.map("organizationId",fixture.org,"itemId",fixture.item);scope.putAll(extra);String before=fingerprint();
  var data=(Map<String,Object>)fixture.read("getObject",fixture.item,scope,fixture.now,fixture.now).get("data");assertEquals(before,fingerprint(),"Public read must not mutate any organization row");return data;
 }
 @SuppressWarnings("unchecked") Map<String,Object> ledger(Map<String,Object> data){return (Map<String,Object>)data.get("effectLedger");}
 @SuppressWarnings("unchecked") List<Map<String,Object>> rows(Map<String,Object> data,String name){return (List<Map<String,Object>>)ledger(data).get(name);}
 String fingerprint(){
  var values=new TreeMap<String,Object>();
  for(String table:fixture.jdbc.queryForList("SELECT table_name FROM information_schema.columns WHERE table_schema='public' AND column_name='organizationid' AND table_name LIKE 'mulino_%' ORDER BY table_name",String.class)){
   assertTrue(table.matches("mulino_[a-z0-9_]+"));
   values.put(table,fixture.jdbc.queryForObject("SELECT md5(coalesce(string_agg(to_jsonb(r)::text,'|' ORDER BY to_jsonb(r)::text),'')) FROM "+table+" r WHERE organizationId=?",String.class,fixture.org));
  }
  return com.mulino.domain.definitions.DefinitionRepository.sha256(fixture.write(values));
 }
 void evidence(String name,Map<String,Object> data)throws Exception{
  String target=System.getProperty("world.evidence");if(target==null)return;Path out=Path.of(target);Files.createDirectories(out);
  Files.writeString(out.resolve(name+".json"),fixture.json.writerWithDefaultPrettyPrinter().writeValueAsString(fixture.map("organizationId",fixture.org,"itemId",fixture.item,"asOf",fixture.now.toString(),"knownAt",fixture.now.toString(),"readFingerprint",fingerprint(),"publicData",data,"sqlSegments",fixture.jdbc.queryForList("SELECT ID,quantity,unit,placeId,retiredAt,retirementRecordedAt FROM mulino_inventory_QuantitySegments WHERE organizationId=? ORDER BY ID",fixture.org),"sqlAllocations",fixture.jdbc.queryForList("SELECT ID,state,quantity,unit,predecessorId,orderLineId FROM mulino_inventory_SegmentAllocations WHERE organizationId=? ORDER BY ID",fixture.org),"sqlDeliveries",fixture.jdbc.queryForList("SELECT ID,quantity,legitimateQuantity,unit,canonicalOccurrenceId FROM mulino_trade_sales_Deliveries WHERE organizationId=? ORDER BY ID",fixture.org))));
 }
 @ParameterizedTest @ValueSource(strings={"normal","late","expiry"})
 void publicWorldPhysicalDeliveryKeepsActualSeparateFromRecognized(String mode)throws Exception{
  fixture.allow("100");fixture.allowDispatch();
  if(mode.equals("expiry"))fixture.salesLine=fixture.effects(fixture.run("createSalesOrder",fixture.map("workId",fixture.work,"customerId",fixture.customer,"itemId",fixture.item,"quantity","30","unit","EA","dueAt",fixture.now.plusSeconds(900).toString(),"destinationId",fixture.destination,"deliveryEndpoint","DELIVERED","qualityTerms","fixture","packageTerms","fixture"),null)).get("salesLineId").toString();
  String dispatch=fixture.dispatched("0","30");
  if(mode.equals("late")){var slots=fixture.permission("placeHold","RECALL","100");slots.put("action","ALL");String doc=fixture.evidence(slots,"HOLD");slots.remove("operation");slots.remove("sourceDecisionId");slots.remove("sourceVersion");slots.putAll(fixture.map("evidenceId",doc,"reason","Post-dispatch recall restriction","nextCheckAt",fixture.now.plusSeconds(60).toString()));fixture.run("placeHold",slots,1);}
  if(mode.equals("expiry"))fixture.now=fixture.now.plusSeconds(150);
  var before=world(Map.of());String amount=mode.equals("normal")?"30":"20";
  var actual=fixture.deliver(fixture.deliveryReport(dispatch,"0",amount,fixture.destination));var after=world(Map.of());
  var scoped=world(fixture.map("salesLineId",fixture.salesLine));assertEquals(amount,scoped.get("deliveredQuantity"));assertEquals(mode.equals("normal")?"0":"10",scoped.get("inTransitQuantity"));
  assertEquals(amount,after.get("deliveredQuantity"));assertEquals(mode.equals("normal")?"0":"10",after.get("inTransitQuantity"));
  assertEquals("100",after.get("heldQuantity"),"Whole ITEM includes warehouse, transit and customer physical goods");
  assertEquals(mode.equals("normal")?amount:"0",actual.get("contributedQuantity"));
  assertEquals(ledger(before).get("allocations"),ledger(after).get("allocations"));
  assertEquals(before.get("warehouseDispatchQuantity"),after.get("warehouseDispatchQuantity"));
  assertEquals("CONSUMED",after.get("allocationStatus"));assertFalse(after.containsKey("deliveryAssessment"));assertFalse(after.containsKey("normalSaleAuthorization"));
  assertEquals(fixture.jdbc.queryForList("SELECT ID FROM mulino_trade_sales_Deliveries WHERE organizationId=? ORDER BY ID",String.class,fixture.org),rows(after,"deliveries").stream().map(r->r.get("ID")).toList());
  assertEquals(fixture.jdbc.queryForList("SELECT ID FROM mulino_inventory_QuantitySegments WHERE organizationId=? ORDER BY ID",String.class,fixture.org),rows(after,"segments").stream().map(r->r.get("ID")).toList());
  for(var obligation:(List<Map<String,Object>>)after.get("obligations")){assertEquals(obligation.get("ID"),obligation.get("id"));assertEquals(obligation.get("valid"),obligation.get("current"));}
  evidence(mode,after);
 }
 @Test void publicWorldClaimsAndDeniedDuplicateReservationKeepLedgerUnchanged()throws Exception{
  fixture.allow("100");fixture.allowDispatch();String dispatch=fixture.dispatched("0","30");var before=world(Map.of());
  var report=fixture.deliveryReport(dispatch,"0","20",fixture.place);fixture.intakeDelivery(report);var after=world(Map.of());
  assertEquals(ledger(before),ledger(after));assertEquals("0",after.get("deliveredQuantity"));
  fixture.segment=fixture.jdbc.queryForObject("SELECT ID FROM mulino_inventory_QuantitySegments WHERE organizationId=? AND placeId=? AND retiredAt IS NULL",String.class,fixture.org,fixture.place);
  String allocation=fixture.reserve("0","60");before=world(Map.of());
  var denied=fixture.apply(fixture.envelope("reserveQuantity",fixture.map("segmentId",fixture.segment,"salesLineId",fixture.salesLine,"startQuantity","0","quantity","60","unit","EA"),0));
  assertEquals("REJECTED",denied.get("outcome"));assertEquals(ledger(before),ledger(world(Map.of())));assertEquals("EXECUTABLE",fixture.allocationState(allocation));evidence("claim-and-denied",after);
 }
 @Test void publicWorldFefoAndPackagingDenialsHaveNoLedgerEffect(){
  fixture.allow("100");fixture.earlierStock(true);var before=world(Map.of());
  assertEquals("REJECTED",fixture.apply(fixture.envelope("reserveQuantity",fixture.reserveSlots(fixture.segment),0)).get("outcome"));assertEquals(ledger(before),ledger(world(Map.of())));
  var wrong=fixture.reserveSlots(fixture.segment);wrong.put("unit","BOX");assertEquals("REJECTED",fixture.apply(fixture.envelope("reserveQuantity",wrong,0)).get("outcome"));assertEquals(ledger(before),ledger(world(Map.of())));
 }
 @Test void publicWorldReplacementReleaseDoesNotReviveOldAllocation()throws Exception{
  fixture.allow("100");String old=fixture.reserve("0","20");fixture.hold("20");String original=fixture.segment;
  String restriction=fixture.jdbc.queryForObject("SELECT ID FROM mulino_inventory_Restrictions WHERE organizationId=?",String.class,fixture.org);
  fixture.segment=fixture.earlierStock(true);String next=fixture.effects(fixture.run("replaceAllocation",fixture.replacementSlots(old,fixture.replacementProof()),fixture.allocationRevision(old))).get("allocationId").toString();
  var replaced=world(Map.of());assertEquals("REPLACED",replaced.get("oldAllocationStatus"));assertEquals("EXECUTABLE",replaced.get("newAllocationStatus"));assertEquals("20",replaced.get("executableReservedQuantity"));
  fixture.segment=original;String release=fixture.evidence(fixture.map("segmentId",fixture.segment,"operation","releaseHold","restrictionId",restriction),"RELEASE_QC");
  fixture.run("releaseHold",fixture.map("restrictionId",restriction,"evidenceId",release,"reason","QC resolved"),0);assertEquals(ledger(replaced).get("allocations"),ledger(world(Map.of())).get("allocations"));
  fixture.run("releaseAllocation",fixture.map("allocationId",next,"reason","Explicit release"),fixture.allocationRevision(next));var released=world(Map.of());assertEquals("REPLACED",released.get("oldAllocationStatus"));assertEquals("RELEASED",released.get("newAllocationStatus"));assertEquals("0",released.get("executableReservedQuantity"));evidence("replacement",released);
 }
 @Test void publicWorldMultipleLinesStayStructuredAndEarlierCutoffsFailClosed()throws Exception{
  fixture.allow("100");String first=fixture.reserve("0","20"),firstLine=fixture.salesLine;
  fixture.salesLine=fixture.effects(fixture.run("createSalesOrder",fixture.map("workId",fixture.work,"customerId",fixture.customer,"itemId",fixture.item,"quantity","30","unit","EA","dueAt",fixture.now.plusSeconds(90).toString(),"destinationId",fixture.destination,"deliveryEndpoint","DELIVERED","qualityTerms","fixture","packageTerms","fixture"),null)).get("salesLineId").toString();fixture.reserve("30","20");
  var data=world(Map.of());assertFalse(data.containsKey("allocationStatus"));assertFalse(data.containsKey("oldAllocationStatus"));assertFalse(data.containsKey("deliveryEndpoint"));assertEquals(2,((List<?>)data.get("orderContracts")).size());assertEquals(2,rows(data,"allocations").size());
  var single=world(fixture.map("salesLineId",firstLine));assertEquals("EXECUTABLE",single.get("allocationStatus"));assertEquals("DELIVERED",single.get("deliveryEndpoint"));assertEquals(List.of(first),rows(single,"allocations").stream().map(a->a.get("ID")).toList());
  Instant recorded=fixture.now;fixture.now=fixture.now.plusSeconds(1);fixture.run("releaseAllocation",fixture.map("allocationId",first,"reason","Late release"),fixture.allocationRevision(first));
  var old=(Map<String,Object>)fixture.read("getObject",fixture.item,fixture.map("organizationId",fixture.org,"itemId",fixture.item),recorded,recorded).get("data");assertFalse(ledger(old).containsKey("allocations"));assertTrue(old.get("publicReadGaps").toString().contains("HISTORICAL_ALLOCATION_STATE_UNAVAILABLE"));
  assertThrows(DomainError.class,()->fixture.read("getObject",fixture.item,fixture.map("organizationId",fixture.org,"itemId",fixture.item),fixture.now.minusSeconds(20),fixture.now.minusSeconds(20)));evidence("multiple-and-temporal",data);
 }
 @Test void publicWorldSharedAncestorDoesNotMergeDifferentOrderCargo(){
  fixture.allow("100");fixture.allowDispatch();String firstLine=fixture.salesLine;fixture.dispatched("0","20");
  fixture.segment=fixture.jdbc.queryForObject("SELECT ID FROM mulino_inventory_QuantitySegments WHERE organizationId=? AND placeId=? AND retiredAt IS NULL",String.class,fixture.org,fixture.place);
  fixture.salesLine=fixture.effects(fixture.run("createSalesOrder",fixture.map("workId",fixture.work,"customerId",fixture.customer,"itemId",fixture.item,"quantity","30","unit","EA","dueAt",fixture.now.plusSeconds(90).toString(),"destinationId",fixture.destination,"deliveryEndpoint","DELIVERED","qualityTerms","fixture","packageTerms","fixture"),null)).get("salesLineId").toString();
  fixture.dispatched("0","10");var whole=world(Map.of());assertEquals("30",whole.get("inTransitQuantity"));
  var first=world(fixture.map("salesLineId",firstLine));var second=world(fixture.map("salesLineId",fixture.salesLine));
  assertEquals("20",first.get("inTransitQuantity"));assertEquals("10",second.get("inTransitQuantity"));
  assertEquals(1,rows(first,"movements").stream().filter(m->"DISPATCH".equals(m.get("kind"))).count());
  assertEquals(1,rows(second,"movements").stream().filter(m->"DISPATCH".equals(m.get("kind"))).count());
 }
 @Test void publicWorldNamedLineBindsTheActualWorkAndPlace()throws Exception{
  fixture.allow("100");String firstWork=fixture.work,firstLine=fixture.salesLine,first=fixture.reserve("0","20");
  var goal=fixture.json.readValue(fixture.jdbc.queryForObject("SELECT slotsJson FROM mulino_work_read_GoalReferences WHERE ID=(SELECT currentGoalVersionId FROM mulino_work_read_Works WHERE ID=?)",String.class,fixture.work),Map.class);
  fixture.work=fixture.effects(fixture.run("createDraft",fixture.map("itemId",fixture.item,"lotId",fixture.lot,"definitionVersionId",fixture.definition,"kind","QUALITY","ownerId",fixture.actor,"supervisorId",fixture.actor,"goal",goal),0)).get("workId").toString();fixture.run("activateWork",fixture.map("workId",fixture.work),0);
  fixture.salesLine=fixture.effects(fixture.run("createSalesOrder",fixture.map("workId",fixture.work,"customerId",fixture.customer,"itemId",fixture.item,"quantity","30","unit","EA","dueAt",fixture.now.plusSeconds(90).toString(),"destinationId",fixture.destination,"deliveryEndpoint","DELIVERED","qualityTerms","fixture","packageTerms","fixture"),null)).get("salesLineId").toString();fixture.reserve("30","20");
  var firstWorld=world(fixture.map("salesLineId",firstLine,"placeId",fixture.place,"lotId",fixture.lot));
  assertEquals(List.of(firstWork),((List<Map<String,Object>>)firstWorld.get("workReferences")).stream().map(w->w.get("ID")).toList());
  assertEquals(List.of(first),rows(firstWorld,"allocations").stream().map(a->a.get("ID")).toList());assertEquals("DELIVERED",firstWorld.get("deliveryEndpoint"));
  assertThrows(DomainError.class,()->world(fixture.map("salesLineId",firstLine,"lotId",fixture.id())));
  assertThrows(DomainError.class,()->world(fixture.map("salesLineId",firstLine,"customerId",fixture.id())));
  evidence("named-line-work-place",firstWorld);
 }
 @Test void publicWorldReadUsesCurrentAllOfAuthorityAndRejectsForeignScope(){
  fixture.allow("100");fixture.reserve("0","20");assertThrows(DomainError.class,()->world(fixture.map("organizationId",fixture.id())));
  assertThrows(DomainError.class,()->world(fixture.map("workId",fixture.id())));assertThrows(DomainError.class,()->world(fixture.map("salesLineId",fixture.id())));
  fixture.jdbc.update("DELETE FROM mulino_identity_GrantScopes WHERE organizationId=? AND grantId=?",fixture.org,fixture.grant);
  for(var e:fixture.map("ITEM",fixture.item,"PLACE",fixture.id()).entrySet())fixture.insert("mulino_identity_GrantScopes",fixture.map("organizationId",fixture.org,"grantId",fixture.grant,"scopeKind",e.getKey(),"scopeId",e.getValue()));
  assertThrows(DomainError.class,()->world(Map.of()));
  fixture.jdbc.update("UPDATE mulino_identity_GrantScopes SET scopeId=? WHERE organizationId=? AND grantId=? AND scopeKind='PLACE'",fixture.place,fixture.org,fixture.grant);
  var scoped=(Map<String,Object>)fixture.read("getInventory",null,fixture.map("organizationId",fixture.org,"itemId",fixture.item,"placeId",fixture.place),fixture.now,fixture.now).get("data");
  assertEquals("100",scoped.get("heldQuantity"));
  fixture.jdbc.update("UPDATE mulino_identity_Grants SET validUntil=? WHERE organizationId=? AND ID=?",Timestamp.from(fixture.now),fixture.org,fixture.grant);assertThrows(DomainError.class,()->world(Map.of()));
 }
}
