package com.mulino.core;

import static org.junit.jupiter.api.Assertions.*;
import com.mulino.application.core.*;
import com.mulino.application.responsibility.ResponsibilityService;
import com.mulino.application.trade.TradeObservationRemedy;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.*;
import org.springframework.transaction.support.TransactionTemplate;

/** Synthetic internal port PG checks. The real completion producer is separately tested; no full RECORD admission is claimed here. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class TradeReconciliationPostgresTest {
 private static final WorkPostgresTestResources OWNED=new WorkPostgresTestResources("t11-trade-reconciliation");
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r){OWNED.properties(r);}
 @Autowired AutowireCapableBeanFactory factory;@Autowired ResponsibilityService service;
 S2WorkActualIntegrationTest f;String root,scope,assignment,actual,canonical;
 @BeforeEach void seed(){f=new S2WorkActualIntegrationTest();factory.autowireBean(f);f.seed();root=f.id();scope=f.id();assignment=f.id();actual=f.id();canonical=f.id();}
 <T>T tx(java.util.function.Supplier<T> body){return f.runtime.requestContext().run(ctx->{return new TransactionTemplate(f.transactions).execute(status->body.get());});}
 Map<String,Object> row(String id){return f.scoped(id);}
 void root(String kind,boolean typed){tx(()->{
  var r=row(root);r.putAll(f.row("dutyKey",root.replace("-","")+root.replace("-",""),"sourceId",f.id(),"sourceKind","DECISION","sourceVersion","synthetic-1","kind",kind,"scopeJson","{}"));if(typed)r.putAll(f.row("quantity",new BigDecimal("40"),"unit","BOX"));f.insert("mulino_responsibility_Roots",r);
  var s=row(scope);s.putAll(f.row("rootId",root,"startQuantity",BigDecimal.ZERO,"quantity",new BigDecimal(typed?"40":"1"),"leaf",true,"scopeJson","{}"));if(typed)s.put("unit","BOX");f.insert("mulino_responsibility_Scopes",s);
  var a=f.workRow(assignment);a.putAll(f.row("rootId",root,"scopeId",scope,"workId",f.work,"kind",kind,"status","OPEN","valid",true,"ownerId",f.actor,"supervisorId",f.actor,"nextAction","실제 보고를 대조한다","nextCheckAt",Timestamp.from(S2WorkActualIntegrationTest.KNOWN.plusSeconds(3600)),"scopeJson","{}"));if(typed)a.putAll(f.row("quantity",new BigDecimal("40"),"unit","BOX"));f.insert("mulino_work_read_ObligationReferences",a);return null;
 });}
 // The private producer remains inaccessible to product callers. This synthetic port input isolates PG rollback/constraints.
 TradeObservationRemedy.Completion completion(String kind,String quantity,String unit)throws Exception{var constructor=TradeObservationRemedy.Completion.class.getDeclaredConstructor(String.class,String.class,String.class,String.class,BigDecimal.class,String.class);constructor.setAccessible(true);return constructor.newInstance(root,kind,canonical,actual,new BigDecimal(quantity),unit);}
 Map<String,Object> assignment(){return f.jdbc.queryForMap("SELECT * FROM mulino_work_read_ObligationReferences WHERE organizationId=? AND ID=?",f.org,assignment);}
 @Test void fortyBoxesCommitsWithUnchangedRootWorkAndOwnership()throws Exception{root("DELIVERY_RECONCILIATION",true);var beforeRoot=f.jdbc.queryForMap("SELECT * FROM mulino_responsibility_Roots WHERE ID=?",root);var beforeWork=f.jdbc.queryForMap("SELECT * FROM mulino_work_read_Works WHERE ID=?",f.work);var completion=completion("DELIVERY_RECONCILIATION","40.00","BOX");tx(()->{service.applyTradeObservation(f.context(),completion);return null;});var resolved=assignment();assertEquals("RESOLVED",resolved.get("status"));assertEquals(canonical,resolved.get("evidenceid"));assertEquals(f.actor,resolved.get("ownerid"));assertEquals(0,new BigDecimal("40").compareTo((BigDecimal)resolved.get("quantity")));assertEquals(beforeRoot,f.jdbc.queryForMap("SELECT * FROM mulino_responsibility_Roots WHERE ID=?",root));assertEquals(beforeWork,f.jdbc.queryForMap("SELECT * FROM mulino_work_read_Works WHERE ID=?",f.work));tx(()->{service.applyTradeObservation(f.context(),completion);return null;});assertEquals(resolved,assignment());}
 @ParameterizedTest @ValueSource(strings={"quantity","unit","return"})
 void mismatchOrTypedReturnRetainsTheExactOpenAssignment(String mode)throws Exception{String kind=mode.equals("return")?"RETURN_RECONCILIATION":"DELIVERY_RECONCILIATION";root(kind,true);var before=assignment();var completion=completion(kind,mode.equals("quantity")?"39":"40",mode.equals("unit")?"EA":"BOX");assertThrows(DomainError.class,()->tx(()->{service.applyTradeObservation(f.context(),completion);return null;}));assertEquals(before,assignment());}
 @ParameterizedTest @ValueSource(strings={"DELIVERY_RECONCILIATION","RETURN_RECONCILIATION"})
 void legacyNonquantityRootStillCompletes(String kind)throws Exception{root(kind,false);var completion=completion(kind,"40","BOX");tx(()->{service.applyTradeObservation(f.context(),completion);return null;});assertEquals("RESOLVED",assignment().get("status"));assertNull(assignment().get("quantity"));}
 @Test void aFailureAfterTheRealUpdateRollsBackAndRetryResolvesOnce()throws Exception{root("DELIVERY_RECONCILIATION",true);var before=assignment();var completion=completion("DELIVERY_RECONCILIATION","40","BOX");assertThrows(IllegalStateException.class,()->tx(()->{service.applyTradeObservation(f.context(),completion);throw new IllegalStateException("synthetic failure after actual reconciliation update");}));assertEquals(before,assignment());tx(()->{service.applyTradeObservation(f.context(),completion);return null;});assertEquals("RESOLVED",assignment().get("status"));assertEquals(1,assignment().get("revision"));}
 @AfterEach void evidence(TestInfo info)throws Exception{try{String destination=System.getProperty("t11.trade.evidence");if(destination!=null){var proof=new LinkedHashMap<String,Object>();proof.put("scope","SYNTHETIC_INTERNAL_RESPONSIBILITY_PORT; full RECORD causal admission NOT_RUN");proof.put("organizationId",f.org);proof.put("containerId",OWNED.pg.getContainerId());for(String table:List.of("mulino_responsibility_Roots","mulino_responsibility_Scopes","mulino_work_read_ObligationReferences","mulino_work_read_Works"))proof.put(table,f.jdbc.queryForList("SELECT row_to_json(t)::text FROM "+table+" t WHERE organizationId=? ORDER BY ID",String.class,f.org));var path=java.nio.file.Path.of(destination);java.nio.file.Files.createDirectories(path);java.nio.file.Files.writeString(path.resolve(info.getDisplayName().replaceAll("[^a-zA-Z0-9-]","_")+".json"),f.json.writerWithDefaultPrettyPrinter().writeValueAsString(proof));}}finally{SecurityContextHolder.clearContext();}}
 @AfterAll static void cleanup()throws Exception{OWNED.close();}
}
