package com.mulino.quality;

import static org.junit.jupiter.api.Assertions.*;
import com.mulino.application.core.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** New focused tests use the existing real gateway/evidence fixture; inherited tests are not selected. */
class PublicWorldReadPostgresTest extends FulfillmentPostgresTest {
 @AfterAll static void cleanupOwnedFixture()throws Exception{PG.stop();try(var paths=Files.walk(BLOBS)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
 @SuppressWarnings("unchecked") Map<String,Object> world(Map<String,Object> extra){
  var scope=map("organizationId",org,"itemId",item);scope.putAll(extra);String before=fingerprint();
  var data=(Map<String,Object>)read("getObject",item,scope,now,now).get("data");assertEquals(before,fingerprint(),"Public read must not mutate any organization row");return data;
 }
 @SuppressWarnings("unchecked") Map<String,Object> ledger(Map<String,Object> data){return (Map<String,Object>)data.get("effectLedger");}
 @SuppressWarnings("unchecked") List<Map<String,Object>> rows(Map<String,Object> data,String name){return (List<Map<String,Object>>)ledger(data).get(name);}
 String fingerprint(){
  var values=new TreeMap<String,Object>();
  for(String table:jdbc.queryForList("SELECT table_name FROM information_schema.columns WHERE table_schema='public' AND column_name='organizationid' AND table_name LIKE 'mulino_%' ORDER BY table_name",String.class)){
   assertTrue(table.matches("mulino_[a-z0-9_]+"));
   values.put(table,jdbc.queryForObject("SELECT md5(coalesce(string_agg(to_jsonb(r)::text,'|' ORDER BY to_jsonb(r)::text),'')) FROM "+table+" r WHERE organizationId=?",String.class,org));
  }
  return com.mulino.domain.definitions.DefinitionRepository.sha256(write(values));
 }
 void evidence(String name,Map<String,Object> data)throws Exception{
  String target=System.getProperty("world.evidence");if(target==null)return;Path out=Path.of(target);Files.createDirectories(out);
  Files.writeString(out.resolve(name+".json"),json.writerWithDefaultPrettyPrinter().writeValueAsString(map("organizationId",org,"itemId",item,"asOf",now.toString(),"knownAt",now.toString(),"readFingerprint",fingerprint(),"publicData",data,"sqlSegments",jdbc.queryForList("SELECT ID,quantity,unit,placeId,retiredAt,retirementRecordedAt FROM mulino_inventory_QuantitySegments WHERE organizationId=? ORDER BY ID",org),"sqlAllocations",jdbc.queryForList("SELECT ID,state,quantity,unit,predecessorId,orderLineId FROM mulino_inventory_SegmentAllocations WHERE organizationId=? ORDER BY ID",org),"sqlDeliveries",jdbc.queryForList("SELECT ID,quantity,legitimateQuantity,unit,canonicalOccurrenceId FROM mulino_trade_sales_Deliveries WHERE organizationId=? ORDER BY ID",org))));
 }
 @ParameterizedTest @ValueSource(strings={"normal","late","expiry"})
 void publicWorldPhysicalDeliveryKeepsActualSeparateFromRecognized(String mode)throws Exception{
  allow("100");allowDispatch();
  if(mode.equals("expiry"))salesLine=effects(run("createSalesOrder",map("workId",work,"customerId",customer,"itemId",item,"quantity","30","unit","EA","dueAt",now.plusSeconds(900).toString(),"destinationId",destination,"deliveryEndpoint","DELIVERED","qualityTerms","fixture","packageTerms","fixture"),null)).get("salesLineId").toString();
  String dispatch=dispatched("0","30");
  if(mode.equals("late")){var slots=permission("placeHold","RECALL","100");slots.put("action","ALL");String doc=evidence(slots,"HOLD");slots.remove("operation");slots.remove("sourceDecisionId");slots.remove("sourceVersion");slots.putAll(map("evidenceId",doc,"reason","Post-dispatch recall restriction","nextCheckAt",now.plusSeconds(60).toString()));run("placeHold",slots,1);}
  if(mode.equals("expiry"))now=now.plusSeconds(150);
  var before=world(Map.of());String amount=mode.equals("normal")?"30":"20";
  var actual=deliver(deliveryReport(dispatch,"0",amount,destination));var after=world(Map.of());
  var scoped=world(map("salesLineId",salesLine));assertEquals(amount,scoped.get("deliveredQuantity"));assertEquals(mode.equals("normal")?"0":"10",scoped.get("inTransitQuantity"));
  assertEquals(amount,after.get("deliveredQuantity"));assertEquals(mode.equals("normal")?"0":"10",after.get("inTransitQuantity"));
  assertEquals("100",after.get("heldQuantity"),"Whole ITEM includes warehouse, transit and customer physical goods");
  assertEquals(mode.equals("normal")?amount:"0",actual.get("contributedQuantity"));
  assertEquals(ledger(before).get("allocations"),ledger(after).get("allocations"));
  assertEquals(before.get("warehouseDispatchQuantity"),after.get("warehouseDispatchQuantity"));
  assertEquals("CONSUMED",after.get("allocationStatus"));assertFalse(after.containsKey("deliveryAssessment"));assertFalse(after.containsKey("normalSaleAuthorization"));
  assertEquals(jdbc.queryForList("SELECT ID FROM mulino_trade_sales_Deliveries WHERE organizationId=? ORDER BY ID",String.class,org),rows(after,"deliveries").stream().map(r->r.get("ID")).toList());
  assertEquals(jdbc.queryForList("SELECT ID FROM mulino_inventory_QuantitySegments WHERE organizationId=? ORDER BY ID",String.class,org),rows(after,"segments").stream().map(r->r.get("ID")).toList());
  for(var obligation:(List<Map<String,Object>>)after.get("obligations")){assertEquals(obligation.get("ID"),obligation.get("id"));assertEquals(obligation.get("valid"),obligation.get("current"));}
  evidence(mode,after);
 }
 @Test void publicWorldClaimsAndDeniedDuplicateReservationKeepLedgerUnchanged()throws Exception{
  allow("100");allowDispatch();String dispatch=dispatched("0","30");var before=world(Map.of());
  var report=deliveryReport(dispatch,"0","20",place);intakeDelivery(report);var after=world(Map.of());
  assertEquals(ledger(before),ledger(after));assertEquals("0",after.get("deliveredQuantity"));
  segment=jdbc.queryForObject("SELECT ID FROM mulino_inventory_QuantitySegments WHERE organizationId=? AND placeId=? AND retiredAt IS NULL",String.class,org,place);
  String allocation=reserve("0","60");before=world(Map.of());
  var denied=apply(envelope("reserveQuantity",map("segmentId",segment,"salesLineId",salesLine,"startQuantity","0","quantity","60","unit","EA"),0));
  assertEquals("REJECTED",denied.get("outcome"));assertEquals(ledger(before),ledger(world(Map.of())));assertEquals("EXECUTABLE",allocationState(allocation));evidence("claim-and-denied",after);
 }
 @Test void publicWorldFefoAndPackagingDenialsHaveNoLedgerEffect(){
  allow("100");earlierStock(true);var before=world(Map.of());
  assertEquals("REJECTED",apply(envelope("reserveQuantity",reserveSlots(segment),0)).get("outcome"));assertEquals(ledger(before),ledger(world(Map.of())));
  var wrong=reserveSlots(segment);wrong.put("unit","BOX");assertEquals("REJECTED",apply(envelope("reserveQuantity",wrong,0)).get("outcome"));assertEquals(ledger(before),ledger(world(Map.of())));
 }
 @Test void publicWorldReplacementReleaseDoesNotReviveOldAllocation()throws Exception{
  allow("100");String old=reserve("0","20");hold("20");String original=segment;
  String restriction=jdbc.queryForObject("SELECT ID FROM mulino_inventory_Restrictions WHERE organizationId=?",String.class,org);
  segment=earlierStock(true);String next=effects(run("replaceAllocation",replacementSlots(old,replacementProof()),allocationRevision(old))).get("allocationId").toString();
  var replaced=world(Map.of());assertEquals("REPLACED",replaced.get("oldAllocationStatus"));assertEquals("EXECUTABLE",replaced.get("newAllocationStatus"));assertEquals("20",replaced.get("executableReservedQuantity"));
  segment=original;String release=evidence(map("segmentId",segment,"operation","releaseHold","restrictionId",restriction),"RELEASE_QC");
  run("releaseHold",map("restrictionId",restriction,"evidenceId",release,"reason","QC resolved"),0);assertEquals(ledger(replaced).get("allocations"),ledger(world(Map.of())).get("allocations"));
  run("releaseAllocation",map("allocationId",next,"reason","Explicit release"),allocationRevision(next));var released=world(Map.of());assertEquals("REPLACED",released.get("oldAllocationStatus"));assertEquals("RELEASED",released.get("newAllocationStatus"));assertEquals("0",released.get("executableReservedQuantity"));evidence("replacement",released);
 }
 @Test void publicWorldMultipleLinesStayStructuredAndEarlierCutoffsFailClosed()throws Exception{
  allow("100");String first=reserve("0","20"),firstLine=salesLine;
  salesLine=effects(run("createSalesOrder",map("workId",work,"customerId",customer,"itemId",item,"quantity","30","unit","EA","dueAt",now.plusSeconds(90).toString(),"destinationId",destination,"deliveryEndpoint","DELIVERED","qualityTerms","fixture","packageTerms","fixture"),null)).get("salesLineId").toString();reserve("30","20");
  var data=world(Map.of());assertFalse(data.containsKey("allocationStatus"));assertFalse(data.containsKey("oldAllocationStatus"));assertFalse(data.containsKey("deliveryEndpoint"));assertEquals(2,((List<?>)data.get("orderContracts")).size());assertEquals(2,rows(data,"allocations").size());
  var single=world(map("salesLineId",firstLine));assertEquals("EXECUTABLE",single.get("allocationStatus"));assertEquals("DELIVERED",single.get("deliveryEndpoint"));assertEquals(List.of(first),rows(single,"allocations").stream().map(a->a.get("ID")).toList());
  Instant recorded=now;now=now.plusSeconds(1);run("releaseAllocation",map("allocationId",first,"reason","Late release"),allocationRevision(first));
  var old=(Map<String,Object>)read("getObject",item,map("organizationId",org,"itemId",item),recorded,recorded).get("data");assertFalse(ledger(old).containsKey("allocations"));assertTrue(old.get("publicReadGaps").toString().contains("HISTORICAL_ALLOCATION_STATE_UNAVAILABLE"));
  assertThrows(DomainError.class,()->read("getObject",item,map("organizationId",org,"itemId",item),now.minusSeconds(20),now.minusSeconds(20)));evidence("multiple-and-temporal",data);
 }
 @Test void publicWorldSharedAncestorDoesNotMergeDifferentOrderCargo(){
  allow("100");allowDispatch();String firstLine=salesLine;dispatched("0","20");
  segment=jdbc.queryForObject("SELECT ID FROM mulino_inventory_QuantitySegments WHERE organizationId=? AND placeId=? AND retiredAt IS NULL",String.class,org,place);
  salesLine=effects(run("createSalesOrder",map("workId",work,"customerId",customer,"itemId",item,"quantity","30","unit","EA","dueAt",now.plusSeconds(90).toString(),"destinationId",destination,"deliveryEndpoint","DELIVERED","qualityTerms","fixture","packageTerms","fixture"),null)).get("salesLineId").toString();
  dispatched("0","10");var whole=world(Map.of());assertEquals("30",whole.get("inTransitQuantity"));
  var first=world(map("salesLineId",firstLine));var second=world(map("salesLineId",salesLine));
  assertEquals("20",first.get("inTransitQuantity"));assertEquals("10",second.get("inTransitQuantity"));
  assertEquals(1,rows(first,"movements").stream().filter(m->"DISPATCH".equals(m.get("kind"))).count());
  assertEquals(1,rows(second,"movements").stream().filter(m->"DISPATCH".equals(m.get("kind"))).count());
 }
 @Test void publicWorldNamedLineBindsTheActualWorkAndPlace()throws Exception{
  allow("100");String firstWork=work,firstLine=salesLine,first=reserve("0","20");
  var goal=json.readValue(jdbc.queryForObject("SELECT slotsJson FROM mulino_work_read_GoalReferences WHERE ID=(SELECT currentGoalVersionId FROM mulino_work_read_Works WHERE ID=?)",String.class,work),Map.class);
  work=effects(run("createDraft",map("itemId",item,"lotId",lot,"definitionVersionId",definition,"kind","QUALITY","ownerId",actor,"supervisorId",actor,"goal",goal),0)).get("workId").toString();run("activateWork",map("workId",work),0);
  salesLine=effects(run("createSalesOrder",map("workId",work,"customerId",customer,"itemId",item,"quantity","30","unit","EA","dueAt",now.plusSeconds(90).toString(),"destinationId",destination,"deliveryEndpoint","DELIVERED","qualityTerms","fixture","packageTerms","fixture"),null)).get("salesLineId").toString();reserve("30","20");
  var firstWorld=world(map("salesLineId",firstLine,"placeId",place,"lotId",lot));
  assertEquals(List.of(firstWork),((List<Map<String,Object>>)firstWorld.get("workReferences")).stream().map(w->w.get("ID")).toList());
  assertEquals(List.of(first),rows(firstWorld,"allocations").stream().map(a->a.get("ID")).toList());assertEquals("DELIVERED",firstWorld.get("deliveryEndpoint"));
  assertThrows(DomainError.class,()->world(map("salesLineId",firstLine,"lotId",id())));
  assertThrows(DomainError.class,()->world(map("salesLineId",firstLine,"customerId",id())));
  evidence("named-line-work-place",firstWorld);
 }
 @Test void publicWorldReadUsesCurrentAllOfAuthorityAndRejectsForeignScope(){
  allow("100");reserve("0","20");assertThrows(DomainError.class,()->world(map("organizationId",id())));
  assertThrows(DomainError.class,()->world(map("workId",id())));assertThrows(DomainError.class,()->world(map("salesLineId",id())));
  jdbc.update("DELETE FROM mulino_identity_GrantScopes WHERE organizationId=? AND grantId=?",org,grant);
  for(var e:map("ITEM",item,"PLACE",id()).entrySet())insert("mulino_identity_GrantScopes",map("organizationId",org,"grantId",grant,"scopeKind",e.getKey(),"scopeId",e.getValue()));
  assertThrows(DomainError.class,()->world(Map.of()));
  jdbc.update("UPDATE mulino_identity_GrantScopes SET scopeId=? WHERE organizationId=? AND grantId=? AND scopeKind='PLACE'",place,org,grant);
  var scoped=(Map<String,Object>)read("getInventory",null,map("organizationId",org,"itemId",item,"placeId",place),now,now).get("data");
  assertEquals("100",scoped.get("heldQuantity"));
  jdbc.update("UPDATE mulino_identity_Grants SET validUntil=? WHERE organizationId=? AND ID=?",Timestamp.from(now),org,grant);assertThrows(DomainError.class,()->world(Map.of()));
 }
}
