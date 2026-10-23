package com.mulino.quality;

import com.mulino.application.core.*;
import java.io.File;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/** Real persisted dispatch and current all-of read authorization, independent of fixture labels. */
@SpringBootTest @ActiveProfiles("local")
class PriorDispatchReadPostgresTest {
 static final FulfillmentPostgresTest f=new FulfillmentPostgresTest();
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r){FulfillmentPostgresTest.properties(r);}
 @Autowired ApplicationContext context;
 @MockitoBean org.springframework.security.oauth2.jwt.JwtDecoder decoder;
 @MockitoBean ExecutionClock clock;
 @BeforeEach void seed(TestInfo info)throws Exception{context.getAutowireCapableBeanFactory().autowireBean(f);f.clock=clock;f.now=Instant.parse("2026-10-08T00:00:00Z");when(clock.instant()).thenAnswer(x->f.now);f.seed(info);f.saleFixture();}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 static void deleteAtExit(File path){path.deleteOnExit();var children=path.listFiles();if(children!=null)for(var child:children)deleteAtExit(child);}
 static{Runtime.getRuntime().addShutdownHook(new Thread(()->deleteAtExit(FulfillmentPostgresTest.BLOBS.toFile())));}
 @SuppressWarnings("unchecked") Map<String,Object> check(String observation){return (Map<String,Object>)((Map<?,?>)f.read("getObject",observation,f.map("organizationId",f.org,"objectType","DeliveryObservation"),f.now,f.now).get("data")).get("priorDispatchCheck");}
 Map<String,Object> noDispatch(){return f.map("deliveryEventId",f.id(),"physicalScopeId",f.id(),"customerId",f.customer,"itemId",f.item,"lotId",f.lot,"rangeRootId",f.segment,"startQuantity","0","quantity","20","unit","EA","placeId",f.destination,"occurredAt",f.now.toString());}
 String matched(){f.allow("100");f.allowDispatch();String dispatch=f.dispatched("0","30");var report=f.deliveryReport(dispatch,"0","20",f.destination);f.intakeDelivery(report);return report.get("physicalScopeId").toString();}
 void grant(String observation,String dispatch){
  // The public noun also composes independent inventory/Work reads. Give only
  // those other read capabilities their own grant; getObject still has exactly
  // one all-of grant and no alternative path to the observation or dispatch.
  String composition=f.id();f.insert("mulino_identity_Grants",f.map("organizationId",f.org,"ID",composition,"actorId",f.actor,"delegatorId",f.actor,"validFrom",Timestamp.from(f.now.minusSeconds(1000)),"validUntil",Timestamp.from(f.now.plusSeconds(10000))));
  f.insert("mulino_identity_GrantScopes",f.map("organizationId",f.org,"grantId",composition,"scopeKind","ORGANIZATION","scopeId",f.org));
  for(String cap:List.of("getInventory","getWork","getAssessment","getEvidence","getObligations"))f.insert("mulino_identity_GrantActions",f.map("organizationId",f.org,"grantId",composition,"capabilityId",cap));
  f.jdbc.update("DELETE FROM mulino_identity_GrantScopes WHERE grantId=?",f.grant);for(var entry:Map.of("ITEM",f.item,"WORK",f.work,"PLACE",f.destination,"TARGET",observation).entrySet())f.insert("mulino_identity_GrantScopes",f.map("organizationId",f.org,"grantId",f.grant,"scopeKind",entry.getKey(),"scopeId",entry.getValue()));if(dispatch!=null)f.insert("mulino_identity_GrantScopes",f.map("organizationId",f.org,"grantId",f.grant,"scopeKind","TARGET","scopeId",dispatch));}
 @Test void noDispatchCannotConfirmOrCreateNormalExecutionEffects(){
  var report=noDispatch();f.intakeDelivery(report);String observation=report.get("physicalScopeId").toString();
  assertEquals(f.map("status","DENIED","linkedDispatchId",null,"salesLineId",null,"reasonCode","PRIOR_DISPATCH_MISSING","required",true),check(observation));
  var before=f.jdbc.queryForList("SELECT ID,quantity,unit,placeId,retiredAt FROM mulino_inventory_QuantitySegments WHERE organizationId=? ORDER BY ID",f.org);
  var refused=f.apply(f.envelope("recordDelivery",f.map("observationId",observation,"canonicalOccurrenceId",f.id()),1));
  assertEquals("HELD",refused.get("outcome"));assertEquals("DELIVERY_UNMATCHED",f.code(refused));
  assertEquals(before,f.jdbc.queryForList("SELECT ID,quantity,unit,placeId,retiredAt FROM mulino_inventory_QuantitySegments WHERE organizationId=? ORDER BY ID",f.org));
  for(String table:List.of("mulino_inventory_Dispatches","mulino_inventory_SegmentAllocations","mulino_inventory_QuantityMovements","mulino_trade_sales_Deliveries","mulino_trade_sales_ExecutionEffects","mulino_work_read_AssessmentReferences"))assertEquals(0,f.jdbc.queryForObject("SELECT count(*) FROM "+table+" WHERE organizationId=?",Integer.class,f.org),table);
  assertEquals(1,f.jdbc.queryForObject("SELECT count(*) FROM mulino_work_read_ObligationReferences WHERE organizationId=? AND kind='DELIVERY_RECONCILIATION' AND status='OPEN'",Integer.class,f.org));
 }
 @Test void actualMatchingDispatchIsAllowedOnlyAsAPrerequisite(){String observation=matched();var actual=f.jdbc.queryForMap("SELECT dispatchId,salesLineId FROM mulino_trade_sales_Observations WHERE organizationId=? AND ID=?",f.org,observation);var prerequisite=check(observation);assertEquals("ALLOWED",prerequisite.get("status"));assertEquals(actual.get("dispatchid"),prerequisite.get("linkedDispatchId"));assertEquals(actual.get("saleslineid"),prerequisite.get("salesLineId"));assertEquals("PRIOR_DISPATCH_MATCHED",prerequisite.get("reasonCode"));assertFalse(prerequisite.containsKey("saleExecutionAuthorized"));assertEquals(0,f.customerQuantity().signum());
  f.jdbc.update("UPDATE mulino_identity_CapabilityAssignments SET revokedAt=? WHERE organizationId=? AND actorId=? AND capabilityId='recordDelivery'",Timestamp.from(f.now),f.org,f.actor);
  assertEquals("ALLOWED",check(observation).get("status"),"Read prerequisite does not depend on RECORD execution permission");
  var denied=f.apply(f.envelope("recordDelivery",f.map("observationId",observation,"canonicalOccurrenceId",f.id()),1));assertEquals("FORBIDDEN",f.code(denied));assertEquals(0,f.customerQuantity().signum());
 }
 @Test void currentAllOfReadAndHiddenDispatchRemainFailClosed(){String observation=matched(),dispatch=check(observation).get("linkedDispatchId").toString();grant(observation,dispatch);assertEquals("ALLOWED",check(observation).get("status"));
  for(String dimension:List.of("TARGET","ITEM","PLACE","WORK")){
   var original=f.jdbc.queryForList("SELECT scopeId FROM mulino_identity_GrantScopes WHERE grantId=? AND scopeKind=?",String.class,f.grant,dimension);
   f.jdbc.update("DELETE FROM mulino_identity_GrantScopes WHERE grantId=? AND scopeKind=?",f.grant,dimension);
   f.insert("mulino_identity_GrantScopes",f.map("organizationId",f.org,"grantId",f.grant,"scopeKind",dimension,"scopeId",f.id()));
   assertEquals("FORBIDDEN",assertThrows(DomainError.class,()->check(observation)).code(),dimension);
   f.jdbc.update("DELETE FROM mulino_identity_GrantScopes WHERE grantId=? AND scopeKind=?",f.grant,dimension);
   for(String id:original)f.insert("mulino_identity_GrantScopes",f.map("organizationId",f.org,"grantId",f.grant,"scopeKind",dimension,"scopeId",id));
  }
  f.jdbc.update("DELETE FROM mulino_identity_GrantScopes WHERE grantId=? AND scopeKind='TARGET' AND scopeId=?",f.grant,dispatch);assertEquals("UNKNOWN",check(observation).get("status"));assertEquals("DISPATCH_READ_SCOPE_UNAVAILABLE",check(observation).get("reasonCode"));
  f.jdbc.update("UPDATE mulino_identity_Grants SET revokedAt=? WHERE ID=?",Timestamp.from(f.now),f.grant);assertEquals("FORBIDDEN",assertThrows(DomainError.class,()->check(observation)).code());
 }
}
