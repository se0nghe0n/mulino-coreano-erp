package com.mulino.responsibility;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Both a fresh database and the actual V41 shape exercise the trigger without schema changes. */
class GoalShortfallMigrationPostgresTest {
 @ParameterizedTest @ValueSource(booleans={false,true})
 void firstProofIsImmutableAndOtherKindsKeepTheirContract(boolean upgrade) throws Exception {
  try(var pg=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280")){
   pg.start();var ds=new DriverManagerDataSource(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());var db=new JdbcTemplate(ds);
   var migration=Flyway.configure().dataSource(ds).locations("filesystem:../database/migrations").target(MigrationVersion.fromVersion(upgrade?"41":"42")).load();migration.migrate();
   String org=id(),actor=id(),work=id(),product=id(),item=id(),spec=id(),pack=id(),definition=id();var at=Timestamp.from(Instant.parse("2026-10-07T02:00:00Z"));
   db.update("INSERT INTO mulino_identity_Organizations(ID,externalAlias) VALUES(?,?)",org,org);
   db.update("INSERT INTO mulino_identity_Actors(organizationId,ID,kind,stableRequestOwner) VALUES(?,?,'HUMAN',?)",org,actor,id());
   db.update("INSERT INTO mulino_inventory_Products(organizationId,ID,name) VALUES(?,?,'test')",org,product);
   db.update("INSERT INTO mulino_inventory_SpecificationVersions(organizationId,ID,productId,version,contentHash) VALUES(?,?,?,'1',?)",org,spec,product,"1".repeat(64));
   db.update("INSERT INTO mulino_inventory_PackagingVersions(organizationId,ID,productId,version,contentHash) VALUES(?,?,?,'1',?)",org,pack,product,"2".repeat(64));
   db.update("INSERT INTO mulino_inventory_TradeItems(organizationId,ID,productId,name,baseUnit,decimalPlaces,specificationVersionId,packagingVersionId) VALUES(?,?,?,'item','BOX',0,?,?)",org,item,product,spec,pack);
   db.update("INSERT INTO mulino_definitions_DefinitionVersions(organizationId,ID,createdAt,version,state,contentHash,content,evaluatorVersion,schemaVersion) VALUES(?,?,?,'1','DRAFT',?,'{}','core-v1','1.0.0')",org,definition,at,"1".repeat(64));
   db.update("INSERT INTO mulino_work_read_Works(organizationId,ID,createdAt,recordedAt,effectiveAt,itemId,definitionVersionId,kind,status,ownerId,supervisorId) VALUES(?,?,?,?,?,?,?,'FOLLOWUP','ACTIVE',?,?)",org,work,at,at,at,item,definition,actor,actor);
   var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));String root=id(),other=id();
   for(var entry:Map.of(root,"GOAL_QUANTITY_SHORTFALL",other,"FOLLOWUP_REVIEW").entrySet())tx.executeWithoutResult(status->{
    String scope=id();db.update("INSERT INTO mulino_responsibility_Roots(organizationId,ID,createdAt,recordedAt,dutyKey,sourceId,sourceKind,sourceVersion,kind,scopeJson,quantity,unit) VALUES(?,?,?,?,?,?,'DECISION','first-proof',?,'{\"first\":true}',10,'BOX')",org,entry.getKey(),at,at,id(),id(),entry.getValue());
    db.update("INSERT INTO mulino_responsibility_Scopes(organizationId,ID,createdAt,recordedAt,rootId,startQuantity,quantity,unit,scopeJson) VALUES(?,?,?,?,?,0,10,'BOX','{}')",org,scope,at,at,entry.getKey());
    db.update("INSERT INTO mulino_work_read_ObligationReferences(organizationId,ID,createdAt,recordedAt,effectiveAt,workId,kind,status,ownerId,supervisorId,nextAction,nextCheckAt,quantity,unit,scopeJson,rootId,scopeId,valid) VALUES(?,?,?,?,?,?,?,'OPEN',?,?,'review',?,10,'BOX','{}',?,?,true)",org,id(),at,at,at,work,entry.getValue(),actor,actor,Timestamp.from(at.toInstant().plusSeconds(86400)),entry.getKey(),scope);
   });
   var columns=db.queryForList("SELECT table_name,ordinal_position,column_name,data_type,character_maximum_length,numeric_precision,numeric_scale,datetime_precision,is_nullable,column_default FROM information_schema.columns WHERE table_schema='public' ORDER BY table_name,ordinal_position");
   var constraints=db.queryForList("SELECT conrelid::regclass::text AS relation,conname,contype,pg_get_constraintdef(oid) AS definition FROM pg_constraint WHERE connamespace='public'::regnamespace ORDER BY 1,2");
   var before=db.queryForMap("SELECT * FROM mulino_responsibility_Roots WHERE organizationId=? AND ID=?",org,root);
   if(upgrade){var next=Flyway.configure().dataSource(ds).locations("filesystem:../database/migrations").target(MigrationVersion.fromVersion("42")).load();assertEquals(1,next.migrate().migrationsExecuted);next.validate();}
   assertEquals(columns,db.queryForList("SELECT table_name,ordinal_position,column_name,data_type,character_maximum_length,numeric_precision,numeric_scale,datetime_precision,is_nullable,column_default FROM information_schema.columns WHERE table_schema='public' ORDER BY table_name,ordinal_position"));
   assertEquals(constraints,db.queryForList("SELECT conrelid::regclass::text AS relation,conname,contype,pg_get_constraintdef(oid) AS definition FROM pg_constraint WHERE connamespace='public'::regnamespace ORDER BY 1,2"));
   var mutations=Map.ofEntries(Map.entry("sourceVersion","'changed'"),Map.entry("sourceId","'"+id()+"'"),Map.entry("scopeJson","'{\"changed\":true}'"),Map.entry("dutyKey","'"+id()+"'"),Map.entry("kind","'FOLLOWUP_REVIEW'"),Map.entry("quantity","9"),Map.entry("unit","'KG'"),Map.entry("organizationId","'"+id()+"'"),Map.entry("ID","'"+id()+"'"));
   var evidence=new ArrayList<Map<String,Object>>();
   for(var mutation:mutations.entrySet()){
    var denied=assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("UPDATE mulino_responsibility_Roots SET "+mutation.getKey()+"="+mutation.getValue()+" WHERE organizationId=? AND ID=?",org,root));
    var sql=(SQLException)denied.getMostSpecificCause();assertEquals("23514",sql.getSQLState());assertTrue(sql.getMessage().contains("root proof is immutable"));evidence.add(Map.of("column",mutation.getKey(),"sqlState",sql.getSQLState()));assertEquals(before,db.queryForMap("SELECT * FROM mulino_responsibility_Roots WHERE organizationId=? AND ID=?",org,root));
   }
   assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("UPDATE mulino_responsibility_Roots SET kind='GOAL_QUANTITY_SHORTFALL' WHERE organizationId=? AND ID=?",org,other));
   assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("UPDATE mulino_responsibility_Roots SET sourceVersion=sourceVersion WHERE organizationId=? AND ID=?",org,root));
   assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("DELETE FROM mulino_responsibility_Roots WHERE organizationId=? AND ID=?",org,root));
   assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("DELETE FROM mulino_responsibility_Roots WHERE organizationId=? AND ID=?",org,other));
   assertEquals(1,db.update("UPDATE mulino_responsibility_Roots SET sourceVersion='other-kind-still-mutable' WHERE organizationId=? AND ID=?",org,other));
   assertEquals(before,db.queryForMap("SELECT * FROM mulino_responsibility_Roots WHERE organizationId=? AND ID=?",org,root));
   String destination=System.getProperty("shortfall.evidence");if(destination!=null){Path out=Path.of(destination);Files.createDirectories(out);Files.writeString(out.resolve(upgrade?"upgrade41-42.json":"fresh42.json"),new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(Map.of("postgres",db.queryForObject("SHOW server_version",String.class),"columnAndConstraintInventoryUnchanged",true,"rootColumns",columns.stream().filter(c->"mulino_responsibility_roots".equals(c.get("table_name"))).toList(),"beforeRootPreserved",true,"rejectedUpdates",evidence,"otherKindUpdateCount",1,"trigger",db.queryForList("SELECT pg_get_triggerdef(oid) FROM pg_trigger WHERE tgname='goal_shortfall_root_immutable'"))));}
  }
 }
 private static String id(){return UUID.randomUUID().toString();}
}
