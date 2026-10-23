package com.mulino.core;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Real V39 -> V41 upgrade: V40 is reserved and not present in this isolated checkout. */
class SourceGoalContributionMigrationPostgresTest {
 @Test void v39LegacyRemainsUnknownAndNewBindingsAreImmutable() throws Exception {
  try(var pg=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280")){
   pg.start();var ds=new DriverManagerDataSource(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());var db=new JdbcTemplate(ds);
   Flyway.configure().dataSource(ds).locations("filesystem:../database/migrations").target(MigrationVersion.fromVersion("39")).load().migrate();
   String org=id(),actor=id(),item=id(),product=id(),spec=id(),pack=id(),definition=id(),source=id(),target=id(),sg=id(),tg=id(),canonical=id(),profile=id(),link=id(),allocation=id();var at=Timestamp.from(Instant.parse("2026-10-07T02:00:00Z"));
   db.update("INSERT INTO mulino_identity_Organizations(ID,externalAlias) VALUES(?,?)",org,org);
   db.update("INSERT INTO mulino_identity_Actors(organizationId,ID,kind,stableRequestOwner) VALUES(?,?,'HUMAN',?)",org,actor,id());
   db.update("INSERT INTO mulino_inventory_Products(organizationId,ID,name,createdAt,recordedAt) VALUES(?,?,'fixture',?,?)",org,product,at,at);
   db.update("INSERT INTO mulino_inventory_SpecificationVersions(organizationId,ID,productId,version,contentHash,createdAt,recordedAt) VALUES(?,?,?,'1',?,?,?)",org,spec,product,"1".repeat(64),at,at);
   db.update("INSERT INTO mulino_inventory_PackagingVersions(organizationId,ID,productId,version,contentHash,createdAt,recordedAt) VALUES(?,?,?,'1',?,?,?)",org,pack,product,"2".repeat(64),at,at);
   db.update("INSERT INTO mulino_inventory_TradeItems(organizationId,ID,productId,name,baseUnit,decimalPlaces,createdAt,recordedAt,specificationVersionId,packagingVersionId) VALUES(?,?,?,'fixture','BOX',0,?,?,?,?)",org,item,product,at,at,spec,pack);
   db.update("INSERT INTO mulino_definitions_DefinitionVersions(organizationId,ID,version,state,contentHash,content,evaluatorVersion,schemaVersion,createdAt) VALUES(?,?,'1','PUBLISHED',?,'{}','core-v1','1.0.0',?)",org,definition,"1".repeat(64),at);
   for(String work:List.of(source,target))db.update("INSERT INTO mulino_work_read_Works(organizationId,ID,createdAt,recordedAt,effectiveAt,itemId,definitionVersionId,kind,status,ownerId,supervisorId,lifecycleMode) VALUES(?,?,?,?,?,?,?,'PURCHASE','ACTIVE',?,?,'COMMAND')",org,work,at,at,at,item,definition,actor,actor);
   for(var pair:List.of(List.of(sg,source),List.of(tg,target))){db.update("INSERT INTO mulino_work_read_GoalReferences(organizationId,ID,createdAt,recordedAt,effectiveAt,workId,definitionVersionId,quantityMode,scopeJson) VALUES(?,?,?,?,?,?,?,'CUMULATIVE_EVENT','{}')",org,pair.get(0),at,at,at,pair.get(1),definition);db.update("UPDATE mulino_work_read_Works SET currentGoalVersionId=? WHERE organizationId=? AND ID=?",pair.get(0),org,pair.get(1));}
   db.update("INSERT INTO mulino_evidence_SourceProfiles(organizationId,ID,revision,createdAt,recordedAt,recordedBy,namespace,policyVersion,intakeOwnerId,supervisorId,nextAction,nextCheckAt) VALUES(?,?,1,?,?,?,'fixture','1',?,?,'review',?)",org,profile,at,at,actor,actor,actor,at);
   db.update("INSERT INTO mulino_evidence_CanonicalOccurrences(organizationId,ID,revision,createdAt,recordedAt,recordedBy,sourceProfileId,subjectKind,subjectId,workId,itemId,effectiveFrom,quantity,unit,physicalScopeId,kind,valueState,timeZone,timePrecision,reassessmentState) VALUES(?,?,1,?,?,?,?,'WORK',?,?,?,?,90,'BOX',?,'RECEIPT','KNOWN','UTC','SECOND','PENDING')",org,canonical,at,at,actor,profile,target,target,item,at,id());
   db.update("INSERT INTO mulino_work_WorkLinks(organizationId,ID,sourceWorkId,targetWorkId,kind,createdAt) VALUES(?,?,?,?,'CONTRIBUTES_TO',?)",org,link,source,target,at);
   db.update("INSERT INTO mulino_work_WorkContributions(organizationId,ID,linkId,occurrenceId,targetWorkId,goalId,conditionId,startQuantity,quantity,unit,recordedAt) VALUES(?,?,?,?,?,?,'quantity',0,30,'BOX',?)",org,allocation,link,canonical,target,tg,at);
   var beforeColumns=db.queryForList("SELECT column_name,data_type,character_maximum_length,is_nullable,column_default FROM information_schema.columns WHERE table_name='mulino_work_workcontributions' ORDER BY ordinal_position");
   var migration=Flyway.configure().dataSource(ds).locations("filesystem:../database/migrations").target(MigrationVersion.fromVersion("41")).load();assertEquals(1,migration.migrate().migrationsExecuted);migration.validate();
   var legacy=db.queryForMap("SELECT * FROM mulino_work_WorkContributions WHERE ID=?",allocation);assertNull(legacy.get("sourceworkid"));assertNull(legacy.get("sourcegoalversionid"));assertNull(legacy.get("sourceconditionid"));assertEquals(tg,legacy.get("goalid"));
   assertThrows(Exception.class,()->db.update("UPDATE mulino_work_WorkContributions SET sourceWorkId=?,sourceGoalVersionId=?,sourceConditionId='Receipt.quantity' WHERE ID=?",source,sg,allocation));
   assertThrows(Exception.class,()->db.update("UPDATE mulino_work_WorkLinks SET kind='SHARES_ACTIVITY' WHERE ID=?",link));
   assertThrows(Exception.class,()->db.update("DELETE FROM mulino_work_WorkContributions WHERE ID=?",allocation));
   assertThrows(Exception.class,()->db.update("INSERT INTO mulino_work_WorkContributions(organizationId,ID,linkId,occurrenceId,targetWorkId,goalId,conditionId,startQuantity,quantity,unit,recordedAt) VALUES(?,?,?,?,?,?,'quantity',30,30,'BOX',?)",org,id(),link,canonical,target,tg,at));
   db.update("INSERT INTO mulino_work_WorkContributions(organizationId,ID,linkId,occurrenceId,targetWorkId,goalId,conditionId,startQuantity,quantity,unit,recordedAt,sourceWorkId,sourceGoalVersionId,sourceConditionId) VALUES(?,?,?,?,?,?,'Receipt.quantity',30,30,'BOX',?,?,?,'Receipt.quantity')",org,id(),link,canonical,target,tg,at,source,sg);
   String allocationSql="INSERT INTO mulino_work_WorkContributions(organizationId,ID,linkId,occurrenceId,targetWorkId,goalId,conditionId,startQuantity,quantity,unit,recordedAt,sourceWorkId,sourceGoalVersionId,sourceConditionId) VALUES(?,?,?,?,?,?,'Receipt.quantity',60,30,'BOX',?,?,?,'Receipt.quantity')";
   assertThrows(Exception.class,()->db.update(allocationSql,org,id(),link,canonical,target,tg,at,source,tg));
   var tx=new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(ds));
   tx.executeWithoutResult(status->{db.update(allocationSql,org,id(),link,canonical,target,tg,at,source,sg);status.setRollbackOnly();});
   assertEquals(2,db.queryForObject("SELECT count(*) FROM mulino_work_WorkContributions",Integer.class));
   try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)){
    var start=new java.util.concurrent.CountDownLatch(1);var attempts=new ArrayList<java.util.concurrent.Future<Boolean>>();
    for(int i=0;i<2;i++)attempts.add(executor.submit(()->{start.await();try{tx.executeWithoutResult(status->db.update(allocationSql,org,id(),link,canonical,target,tg,at,source,sg));return true;}catch(org.springframework.dao.DataAccessException expected){return false;}}));
    start.countDown();int successes=0;for(var attempt:attempts)if(attempt.get(20,java.util.concurrent.TimeUnit.SECONDS))successes++;assertEquals(1,successes);
   }
   assertEquals(3,db.queryForObject("SELECT count(*) FROM mulino_work_WorkContributions",Integer.class));
   var columns=db.queryForList("SELECT column_name,data_type,character_maximum_length,is_nullable,column_default FROM information_schema.columns WHERE table_name='mulino_work_workcontributions' ORDER BY ordinal_position");
   assertEquals(14,columns.size());assertEquals(beforeColumns,columns.subList(0,11));assertTrue(columns.subList(11,14).stream().allMatch(x->"YES".equals(x.get("is_nullable"))&&x.get("column_default")==null));
   var csn=new tools.jackson.databind.ObjectMapper().readTree(Files.readString(Path.of("src/main/resources/edmx/csn.json"))).path("definitions").path("mulino.work.WorkContributions").path("elements");assertEquals(columns.size(),csn.size());
   for(var column:columns){var field=csn.properties().stream().filter(e->e.getKey().equalsIgnoreCase(column.get("column_name").toString())).findFirst().orElseThrow().getValue();assertEquals(field.path("key").asBoolean()||field.path("notNull").asBoolean()?"NO":"YES",column.get("is_nullable"));String type=field.path("type").asString();if(type.equals("cds.UUID")||type.equals("cds.String")){assertEquals("character varying",column.get("data_type"));assertEquals(type.equals("cds.UUID")?36:field.path("length").asInt(),((Number)column.get("character_maximum_length")).intValue());}else assertEquals(type.equals("cds.Decimal")?"numeric":"timestamp with time zone",column.get("data_type"));}
   var keys=db.queryForList("SELECT a.attname FROM pg_index i JOIN pg_attribute a ON a.attrelid=i.indrelid AND a.attnum=ANY(i.indkey) WHERE i.indrelid='mulino_work_workcontributions'::regclass AND i.indisprimary ORDER BY a.attnum",String.class);assertEquals(List.of("organizationid","id"),keys);
   var constraints=db.queryForList("SELECT conname,contype,pg_get_constraintdef(oid) AS definition FROM pg_constraint WHERE conrelid='mulino_work_workcontributions'::regclass ORDER BY conname");
   assertTrue(constraints.stream().anyMatch(c->"work_contribution_source_goal_fk".equals(c.get("conname"))&&c.get("definition").toString().contains("sourcegoalversionid, sourceworkid")));
   String evidence=System.getProperty("contribution.evidence");if(evidence!=null){Path out=Path.of(evidence);Files.createDirectories(out);Files.writeString(out.resolve("schema-upgrade.json"),new tools.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(Map.of("upgrade","39->41 (V40 absent)","unchangedBaselineColumns",beforeColumns,"legacySourcePinsRemainNull",true,"columns",columns,"primaryKey",keys,"constraints",constraints,"postgresVersion",db.queryForObject("SHOW server_version",String.class))));}
  }
 }
 private static String id(){return UUID.randomUUID().toString();}
}
