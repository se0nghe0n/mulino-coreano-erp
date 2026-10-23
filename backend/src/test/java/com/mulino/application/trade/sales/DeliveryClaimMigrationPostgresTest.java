package com.mulino.application.trade.sales;

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

class DeliveryClaimMigrationPostgresTest {
 String id(){return UUID.randomUUID().toString();}
 Map<String,Object> values(Object... fields){var row=new LinkedHashMap<String,Object>();for(int i=0;i<fields.length;i+=2)row.put(fields[i].toString(),fields[i+1]);return row;}
 void insert(JdbcTemplate db,String table,Map<String,Object> row){db.update("INSERT INTO "+table+" ("+String.join(",",row.keySet())+") VALUES ("+String.join(",",Collections.nCopies(row.size(),"?"))+")",row.values().toArray());}
 @Test void v39UpgradePreservesLegacyAndVerifiedForeignKeysWithoutInventingOriginalClaims()throws Exception {
  try(var pg=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280")) {
   pg.start();var source=new DriverManagerDataSource(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());var db=new JdbcTemplate(source);
   Flyway.configure().dataSource(source).locations("filesystem:../database/migrations").target(MigrationVersion.fromVersion("39")).load().migrate();
   String org=id(),actor=id(),product=id(),spec=id(),pack=id(),item=id(),maker=id(),lot=id(),place=id(),root=id(),definition=id(),work=id(),profile=id(),event=id(),customer=id(),legacy=id();var now=Timestamp.from(Instant.parse("2026-10-07T09:00:00Z"));
   insert(db,"mulino_identity_Organizations",values("ID",org,"externalAlias",org));
   insert(db,"mulino_identity_Actors",values("organizationId",org,"ID",actor,"kind","HUMAN","stableRequestOwner",actor));
   insert(db,"mulino_inventory_Products",values("organizationId",org,"ID",product,"name","legacy product"));
   for(String table:List.of("SpecificationVersions","PackagingVersions"))insert(db,"mulino_inventory_"+table,values("organizationId",org,"ID",table.equals("SpecificationVersions")?spec:pack,"productId",product,"version","1","contentHash","a".repeat(64)));
   insert(db,"mulino_inventory_TradeItems",values("organizationId",org,"ID",item,"productId",product,"name","legacy item","baseUnit","BOX","decimalPlaces",0,"specificationVersionId",spec,"packagingVersionId",pack));
   insert(db,"mulino_inventory_Places",values("organizationId",org,"ID",place,"name","legacy transit","kind","TRANSIT"));
   insert(db,"mulino_inventory_Manufacturers",values("organizationId",org,"ID",maker,"name","legacy manufacturer"));
   insert(db,"mulino_inventory_ManufacturingLots",values("organizationId",org,"ID",lot,"manufacturerId",maker,"itemId",item,"originalLot","legacy lot"));
   insert(db,"mulino_inventory_QuantitySegments",values("organizationId",org,"ID",root,"itemId",item,"lotId",lot,"identificationStatus","CONFIRMED","quantity",new java.math.BigDecimal("30"),"unit","BOX","placeId",place,"controlScope","legacy","validFrom",now,"mixtureStatus","IDENTIFIED"));
   insert(db,"mulino_definitions_DefinitionVersions",values("organizationId",org,"ID",definition,"version","legacy-v1","state","PUBLISHED","contentHash","a".repeat(64),"content","{}","evaluatorVersion","core-v1","schemaVersion","1.0.0"));
   insert(db,"mulino_work_read_Works",values("organizationId",org,"ID",work,"createdAt",now,"recordedAt",now,"effectiveAt",now,"itemId",item,"definitionVersionId",definition,"kind","DELIVERY","status","ACTIVE","ownerId",actor,"supervisorId",actor));
   insert(db,"mulino_evidence_SourceProfiles",values("organizationId",org,"ID",profile,"revision",1,"recordedAt",now,"recordedBy",actor,"namespace","legacy","policyVersion","legacy-v1","intakeOwnerId",actor,"supervisorId",actor,"nextAction","Legacy source review","nextCheckAt",now));
   insert(db,"mulino_evidence_Events",values("organizationId",org,"ID",event,"revision",1,"recordedAt",now,"recordedBy",actor,"subjectKind","ITEM","subjectId",item,"itemId",item,"workId",work,"kind","PHYSICAL_DELIVERY","valueState","UNKNOWN","effectiveFrom",now,"timeZone","UTC","timePrecision","SECOND","sourceNamespace","legacy","sourceProfileId",profile,"externalEventId","legacy-event","sourceVersion","1","payloadHash",com.mulino.domain.definitions.DefinitionRepository.sha256("{}"),"payload","{}"));
   insert(db,"mulino_trade_sales_Customers",values("organizationId",org,"ID",customer,"revision",1,"createdAt",now,"recordedAt",now,"effectiveAt",now,"name","legacy customer"));
   var old=values("organizationId",org,"ID",legacy,"revision",1,"createdAt",now,"recordedAt",now,"effectiveAt",now,"eventId",event,"workId",work,"customerId",customer,"itemId",item,"rangeRootId",root,"startQuantity",java.math.BigDecimal.ZERO,"quantity",new java.math.BigDecimal("20"),"unit","BOX","placeId",place,"occurredAt",now,"state","PROVISIONAL","nextCheckAt",now,"nextAction","Legacy report");
   insert(db,"mulino_trade_sales_Observations",old);
   var before=db.queryForMap("SELECT * FROM mulino_trade_sales_Observations WHERE ID=?",legacy);
   var foreignKeys=db.queryForList("SELECT conname,pg_get_constraintdef(oid) AS definition FROM pg_constraint WHERE conrelid IN ('mulino_trade_sales_observations'::regclass,'mulino_trade_sales_deliveries'::regclass) AND contype='f' ORDER BY conname");
   var checksums=db.queryForList("SELECT version,checksum FROM flyway_schema_history WHERE success ORDER BY installed_rank");
   var upgrade=Flyway.configure().dataSource(source).locations("filesystem:../database/migrations").load();assertEquals(1,upgrade.migrate().migrationsExecuted);upgrade.validate();
   var after=db.queryForMap("SELECT * FROM mulino_trade_sales_Observations WHERE ID=?",legacy);assertNull(after.remove("claimedrangerootid"));assertNull(after.remove("claimpayloadjson"));assertEquals(before,after);
   assertEquals(foreignKeys,db.queryForList("SELECT conname,pg_get_constraintdef(oid) AS definition FROM pg_constraint WHERE conrelid IN ('mulino_trade_sales_observations'::regclass,'mulino_trade_sales_deliveries'::regclass) AND contype='f' ORDER BY conname"));
   assertEquals(checksums,db.queryForList("SELECT version,checksum FROM flyway_schema_history WHERE success AND version<>'40' ORDER BY installed_rank"));
   var claim=new LinkedHashMap<>(old);String unknown=id();claim.put("ID",id());claim.put("rangeRootId",null);claim.put("claimedRangeRootId",unknown);claim.put("claimPayloadJson","{\"rangeRootId\":\""+unknown+"\"}");insert(db,"mulino_trade_sales_Observations",claim);
   assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("UPDATE mulino_trade_sales_Observations SET state='CONFIRMED' WHERE ID=?",claim.get("ID")));
   assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("UPDATE mulino_trade_sales_Observations SET state='CONFIRMED',rangeRootId=? WHERE ID=?",root,claim.get("ID")));
   var invalid=new LinkedHashMap<>(old);invalid.put("ID",id());invalid.put("rangeRootId",id());assertThrows(org.springframework.dao.DataAccessException.class,()->insert(db,"mulino_trade_sales_Observations",invalid));
   assertEquals(2,db.queryForObject("SELECT count(*) FROM mulino_trade_sales_Observations",Integer.class));assertEquals(0,db.queryForObject("SELECT count(*) FROM mulino_trade_sales_Deliveries",Integer.class));
   String evidence=System.getProperty("delivery.evidence");if(evidence!=null){var out=Path.of(evidence);Files.createDirectories(out);Files.writeString(out.resolve("migration-v39-v40.json"),new tools.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(values("upgrade","39->40","legacyUnchanged",true,"legacyOriginalClaimBackfilled",false,"foreignKeys",foreignKeys,"previousChecksums",checksums,"unknownClaimRetained",true,"postgresVersion",db.queryForObject("SHOW server_version",String.class))));}
  }
 }
}
