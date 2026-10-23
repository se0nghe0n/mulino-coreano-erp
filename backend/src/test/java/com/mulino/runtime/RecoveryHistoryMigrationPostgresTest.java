package com.mulino.runtime;

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

class RecoveryHistoryMigrationPostgresTest {
 @Test void upgradePreservesUnknownLegacyRowsAndMatchesCdsColumns() throws Exception {
  try(var pg=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280")) {
   pg.start();var source=new DriverManagerDataSource(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());var db=new JdbcTemplate(source);
   Flyway.configure().dataSource(source).locations("filesystem:../database/migrations").target(MigrationVersion.fromVersion("38")).load().migrate();
   String org=UUID.randomUUID().toString(),id=UUID.randomUUID().toString();var now=Timestamp.from(Instant.parse("2026-10-07T02:00:00Z"));
   db.update("INSERT INTO mulino_runtime_RecoverySchedules(organizationId,ID,workId,kind,nextCheckAt,ownerId,supervisorId,nextAction) VALUES(?,?,?,'LEGACY',?,?,?,'Existing source')",org,id,UUID.randomUUID().toString(),now,UUID.randomUUID().toString(),UUID.randomUUID().toString());
   var upgraded=Flyway.configure().dataSource(source).locations("filesystem:../database/migrations").target(MigrationVersion.fromVersion("39")).load();assertEquals(1,upgraded.migrate().migrationsExecuted);upgraded.validate();
   assertEquals(0,db.queryForObject("SELECT count(*) FROM mulino_runtime_RecoveryObservationHistory",Integer.class));
   assertEquals("Existing source",db.queryForObject("SELECT nextAction FROM mulino_runtime_RecoverySchedules WHERE ID=?",String.class,id));
   var tx=new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(source));
   tx.executeWithoutResult(status->{db.queryForObject("SELECT set_config('mulino.recovery_recorded_at',?,true)",String.class,now.toInstant().toString());db.update("DELETE FROM mulino_runtime_RecoverySchedules WHERE ID=?",id);});
   assertFalse(db.queryForObject("SELECT historyComplete FROM mulino_runtime_RecoveryObservationHistory",Boolean.class));
   assertEquals("DELETE",db.queryForObject("SELECT operation FROM mulino_runtime_RecoveryObservationHistory",String.class));
   var columns=db.queryForList("SELECT column_name,data_type,character_maximum_length,is_nullable FROM information_schema.columns WHERE table_schema='public' AND table_name='mulino_runtime_recoveryobservationhistory' ORDER BY ordinal_position");
   var keys=db.queryForList("SELECT a.attname FROM pg_index i JOIN pg_attribute a ON a.attrelid=i.indrelid AND a.attnum=ANY(i.indkey) WHERE i.indrelid='mulino_runtime_recoveryobservationhistory'::regclass AND i.indisprimary ORDER BY a.attnum",String.class);
   assertEquals(List.of("organizationid","id"),keys);assertEquals(14,columns.size());
   var csn=new tools.jackson.databind.ObjectMapper().readTree(Files.readString(Path.of("src/main/resources/edmx/csn.json"))).path("definitions").path("mulino.runtime.RecoveryObservationHistory").path("elements");
   var expected=new HashSet<String>();csn.properties().forEach(e->expected.add(e.getKey().toLowerCase(Locale.ROOT)));assertEquals(expected,new HashSet<>(columns.stream().map(c->c.get("column_name").toString()).toList()));
   for(var column:columns){
    var field=csn.properties().stream().filter(e->e.getKey().equalsIgnoreCase(column.get("column_name").toString())).findFirst().orElseThrow().getValue();
    String type=switch(field.path("type").asString()){case "cds.UUID","cds.String"->"character varying";case "cds.LargeString"->"text";case "cds.Integer"->"integer";case "cds.Integer64"->"bigint";case "cds.Boolean"->"boolean";case "cds.Timestamp"->"timestamp with time zone";default->throw new AssertionError("Unknown CDS type");};
    assertEquals(type,column.get("data_type"));assertEquals(field.path("key").asBoolean()||field.path("notNull").asBoolean()?"NO":"YES",column.get("is_nullable"));
    if(type.equals("character varying"))assertEquals(field.path("type").asString().equals("cds.UUID")?36:field.path("length").asInt(),((Number)column.get("character_maximum_length")).intValue());
   }
   String evidence=System.getProperty("recovery.evidence");if(evidence!=null){Path out=Path.of(evidence);Files.createDirectories(out);Files.writeString(out.resolve("schema-upgrade.json"),new tools.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(Map.of("upgrade","38->39","historyRowsBeforeLegacyMutation",0,"primaryKey",keys,"columns",columns,"postgresVersion",db.queryForObject("SHOW server_version",String.class),"csnColumnsMatch",true)));}
  }
 }
}
