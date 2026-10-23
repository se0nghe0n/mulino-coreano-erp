package com.mulino.application.evaluation;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mulino.domain.definitions.DefinitionRepository;
import com.mulino.domain.evaluation.EvidenceCaptureRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

class SharedEvidenceCapturePostgresTest {
 static String id(){return UUID.randomUUID().toString();}
 final ObjectMapper json=new ObjectMapper();final Instant at=Instant.parse("2026-10-07T02:00:00Z");
 Map<String,Object> boundary(String org,String item,Instant known){return new TreeMap<>(Map.of("organizationId",org,"scope",Map.of("itemId",item),"asOf",at.toString(),"knownAt",known.toString(),"evaluationHorizon",at.toString(),"admissionPurpose","ASSESS_GOAL","admissionContract","canonical-admission-v1","definitionHash","definition","policyHash","policy"));}
 String payload(Map<String,Object> b,Map<String,Object> facts)throws Exception{return json.writeValueAsString(Map.of("boundary",b,"facts",facts));}
 @ParameterizedTest @ValueSource(booleans={false,true}) void captureIsRealImmutableScopedAndConcurrent(boolean upgrade)throws Exception{
  try(var pg=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280")){
   pg.start();var ds=new DriverManagerDataSource(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());var db=new JdbcTemplate(ds);var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
   var fly=Flyway.configure().dataSource(ds).locations("filesystem:../database/migrations").target(MigrationVersion.fromVersion(upgrade?"42":"43")).load();fly.migrate();
   String org=id(),item=id();db.update("INSERT INTO mulino_identity_Organizations(ID,externalAlias) VALUES(?,?)",org,org);
   String old=legacySnapshot(db,org,item);var legacyBefore=db.queryForMap("SELECT contentHash,contentJson FROM mulino_evaluation_InputSnapshots WHERE ID=?",old);
   if(upgrade){var next=Flyway.configure().dataSource(ds).locations("filesystem:../database/migrations").target(MigrationVersion.fromVersion("43")).load();assertEquals(1,next.migrate().migrationsExecuted);next.validate();}
   assertEquals(legacyBefore,db.queryForMap("SELECT contentHash,contentJson FROM mulino_evaluation_InputSnapshots WHERE ID=?",old));assertNull(db.queryForObject("SELECT evidenceCaptureId FROM mulino_evaluation_InputSnapshots WHERE ID=?",String.class,old));
   var b=boundary(org,item,at);var facts=Map.<String,Object>of("properties",Map.of(),"relations",Map.of(),"sources",Map.of());String raw=payload(b,facts),scope=json.writeValueAsString(b.get("scope"));
   var repository=new EvidenceCaptureRepository(db);assertThrows(IllegalStateException.class,()->repository.acquire(org,scope,at,at,at,"ASSESS_GOAL",raw,at));
   var pool=Executors.newFixedThreadPool(4);var start=new CountDownLatch(1);var futures=new ArrayList<Future<String>>();
   try{for(int n=0;n<4;n++)futures.add(pool.submit(()->{start.await();return tx.execute(status->repository.acquire(org,scope,at,at,at,"ASSESS_GOAL",raw,at));}));start.countDown();var ids=new HashSet<String>();for(var f:futures)ids.add(f.get(30,TimeUnit.SECONDS));assertEquals(1,ids.size());String capture=ids.iterator().next();assertDoesNotThrow(()->UUID.fromString(capture));assertEquals(1,db.queryForObject("SELECT count(*) FROM mulino_evaluation_EvidenceCaptures",Integer.class));
    var before=db.queryForMap("SELECT * FROM mulino_evaluation_EvidenceCaptures WHERE ID=?",capture);
    for(String mutation:List.of("contentJson=contentJson","scopeJson='{}'","organizationId='"+id()+"'"))assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("UPDATE mulino_evaluation_EvidenceCaptures SET "+mutation+" WHERE ID=?",capture));
    assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("DELETE FROM mulino_evaluation_EvidenceCaptures WHERE ID=?",capture));assertEquals(before,db.queryForMap("SELECT * FROM mulino_evaluation_EvidenceCaptures WHERE ID=?",capture));
    String different=payload(boundary(org,item,at.plusSeconds(1)),facts);String next=tx.execute(s->repository.acquire(org,scope,at,at.plusSeconds(1),at,"ASSESS_GOAL",different,at));assertNotEquals(capture,next);
    String org2=id(),item2=id();db.update("INSERT INTO mulino_identity_Organizations(ID,externalAlias) VALUES(?,?)",org2,org2);legacySnapshot(db,org2,item2);String scopeOrg2=json.writeValueAsString(Map.of("itemId",item2));String otherOrg=payload(boundary(org2,item2,at),facts);assertNotEquals(capture,tx.execute(s->repository.acquire(org2,scopeOrg2,at,at,at,"ASSESS_GOAL",otherOrg,at)));
    String place=id();db.update("INSERT INTO mulino_inventory_Places(organizationId,ID,name,kind) VALUES(?,?,'W','INTERNAL_STORAGE')",org,place);var alternateScope=boundary(org,item,at);alternateScope.put("scope",Map.of("itemId",item,"placeId",place));String scope2=json.writeValueAsString(alternateScope.get("scope")),scopePayload=payload(alternateScope,facts);assertNotEquals(capture,tx.execute(s->repository.acquire(org,scope2,at,at,at,"ASSESS_GOAL",scopePayload,at)));
    var admission=boundary(org,item,at);admission.put("admissionPurpose","CLOSE_WORK");String admissionPayload=payload(admission,facts);assertNotEquals(capture,tx.execute(s->repository.acquire(org,scope,at,at,at,"CLOSE_WORK",admissionPayload,at)));
    assertThrows(IllegalArgumentException.class,()->tx.execute(s->repository.acquire(org,scope,at.plusSeconds(1),at,at,"ASSESS_GOAL",raw,at)));
    var deniedBinding=assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("INSERT INTO mulino_evaluation_InputSnapshots SELECT organizationId,?,assessmentId,workId,goalId,recordedAt,asOf,knownAt,contentHash,contentJson,? FROM mulino_evaluation_InputSnapshots WHERE ID=?",id(),capture,old));assertTrue(deniedBinding.getMostSpecificCause().getMessage().contains("binding differs"));
    assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("INSERT INTO mulino_evaluation_InputSnapshots SELECT organizationId,?,assessmentId,workId,goalId,recordedAt,asOf,knownAt,contentHash,contentJson,? FROM mulino_evaluation_InputSnapshots WHERE ID=?",id(),db.queryForObject("SELECT ID FROM mulino_evaluation_EvidenceCaptures WHERE organizationId=?",String.class,org2),old));
    String wrongOrgItem=payload(boundary(org2,item,at),facts);assertThrows(org.springframework.dao.DataAccessException.class,()->tx.execute(s->repository.acquire(org2,scope,at,at,at,"ASSESS_GOAL",wrongOrgItem,at)));
    String sourceChanged=payload(b,Map.of("properties",Map.of(),"relations",Map.of(),"sources",Map.of("source",List.of(Map.of("ID",id(),"organizationId",org)))));assertNotEquals(capture,tx.execute(s->repository.acquire(org,scope,at,at,at,"ASSESS_GOAL",sourceChanged,at)));
    String foreign=payload(b,Map.of("properties",Map.of(),"relations",Map.of(),"sources",Map.of("source",List.of(Map.of("ID",id(),"organizationId",id())))));assertThrows(org.springframework.dao.DataAccessException.class,()->tx.execute(s->repository.acquire(org,scope,at,at,at,"ASSESS_GOAL",foreign,at)));
    assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("INSERT INTO mulino_evaluation_EvidenceCaptures SELECT organizationId,?,itemId,lotId,placeId,workId,scopeJson,asOf,knownAt,evaluationHorizon,admissionPurpose,?,contentJson,recordedAt FROM mulino_evaluation_EvidenceCaptures WHERE ID=?",id(),"0".repeat(64),capture));
    if(System.getProperty("capture.evidence")!=null){var dest=java.nio.file.Path.of(System.getProperty("capture.evidence"));java.nio.file.Files.createDirectories(dest);java.nio.file.Files.writeString(dest.resolve(upgrade?"upgrade42-43.json":"fresh43.json"),json.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of("captureId",capture,"contentHash",DefinitionRepository.sha256(raw),"sourcePayload",raw,"postgres",db.queryForObject("SHOW server_version",String.class),"concurrentTransactions",4,"captureCount",db.queryForObject("SELECT count(*) FROM mulino_evaluation_EvidenceCaptures",Integer.class))));}
   }finally{pool.shutdownNow();}
  }
 }
 String legacySnapshot(JdbcTemplate db,String org,String item){
  String actor=id(),product=id(),spec=id(),pack=id(),definition=id(),work=id(),goal=id(),assessment=id(),snapshot=id();var time=Timestamp.from(at);
  db.update("INSERT INTO mulino_identity_Actors(organizationId,ID,kind,stableRequestOwner) VALUES(?,?,'HUMAN',?)",org,actor,id());
  db.update("INSERT INTO mulino_inventory_Products(organizationId,ID,name) VALUES(?,?,'test')",org,product);
  db.update("INSERT INTO mulino_inventory_SpecificationVersions(organizationId,ID,productId,version,contentHash) VALUES(?,?,?,'1',?)",org,spec,product,"1".repeat(64));
  db.update("INSERT INTO mulino_inventory_PackagingVersions(organizationId,ID,productId,version,contentHash) VALUES(?,?,?,'1',?)",org,pack,product,"2".repeat(64));
  db.update("INSERT INTO mulino_inventory_TradeItems(organizationId,ID,productId,name,baseUnit,decimalPlaces,specificationVersionId,packagingVersionId) VALUES(?,?,?,'item','BOX',0,?,?)",org,item,product,spec,pack);
  db.update("INSERT INTO mulino_definitions_DefinitionVersions(organizationId,ID,createdAt,version,state,contentHash,content,evaluatorVersion,schemaVersion) VALUES(?,?,?,'1','DRAFT',?,'{}','core-v1','1.0.0')",org,definition,time,"1".repeat(64));
  db.update("INSERT INTO mulino_work_read_Works(organizationId,ID,createdAt,recordedAt,effectiveAt,itemId,definitionVersionId,kind,status,ownerId,supervisorId) VALUES(?,?,?,?,?,?,?,'FOLLOWUP','ACTIVE',?,?)",org,work,time,time,time,item,definition,actor,actor);
  db.update("INSERT INTO mulino_work_read_GoalReferences(organizationId,ID,workId,createdAt,recordedAt,effectiveAt,definitionVersionId,quantityMode,endpoint,scopeJson,slotsJson) VALUES(?,?,?,?,?,?,?,'CUMULATIVE_EVENT','ARRIVED','{}','{}')",org,goal,work,time,time,time,definition);
  db.update("INSERT INTO mulino_work_read_AssessmentReferences(organizationId,ID,workId,goalId,createdAt,recordedAt,effectiveAt,assessedAt,outcome,evaluatorVersion,conditionsJson,inputSnapshotHash,asOf,knownAt) VALUES(?,?,?,?,?,?,?,?,'UNVERIFIED','core-v1','[]',?,?,?)",org,assessment,work,goal,time,time,time,time,DefinitionRepository.sha256("{}"),time,time);
  db.update("INSERT INTO mulino_evaluation_InputSnapshots(organizationId,ID,assessmentId,workId,goalId,recordedAt,asOf,knownAt,contentHash,contentJson) VALUES(?,?,?,?,?,?,?,?,?,'{}')",org,snapshot,assessment,work,goal,time,time,time,DefinitionRepository.sha256("{}"));return snapshot;
 }

}
