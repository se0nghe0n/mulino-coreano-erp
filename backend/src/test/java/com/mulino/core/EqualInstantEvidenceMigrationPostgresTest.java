package com.mulino.core;

import static org.junit.jupiter.api.Assertions.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Real fresh V44 and V43 -> V44, with every unrelated DDL inventory preserved. */
class EqualInstantEvidenceMigrationPostgresTest {
 static final Instant AT=Instant.parse("2026-10-07T02:00:00Z");
 @ParameterizedTest @ValueSource(booleans={false,true})
 void equalInstantUsesStrictExistingPriorRevisionAndKeepsSingleSuccessor(boolean upgrade)throws Exception{
  try(var pg=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280")){
   pg.start();var ds=new DriverManagerDataSource(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());var db=new JdbcTemplate(ds);
   Flyway.configure().dataSource(ds).target(upgrade?"43":"44").load().migrate();String org=id(),actor=id(),profile=id(),subject=id(),prior=id();
   db.update("INSERT INTO mulino_identity_Organizations(ID,externalAlias) VALUES(?,?)",org,org);
   db.update("INSERT INTO mulino_identity_Actors(organizationId,ID,kind,stableRequestOwner) VALUES(?,?,'HUMAN',?)",org,actor,id());
   db.update("INSERT INTO mulino_evidence_SourceProfiles(organizationId,ID,revision,recordedAt,recordedBy,namespace,policyVersion,intakeOwnerId,supervisorId,nextAction,nextCheckAt) VALUES(?,?,1,?,?,'same-instant-fixture','fixture-v1',?,?,'review',?)",org,profile,Timestamp.from(AT),actor,actor,actor,Timestamp.from(AT.plusSeconds(3600)));
   append(ds,org,actor,profile,subject,prior,null,1,AT,"ITEM");var frozen=db.queryForList("SELECT * FROM mulino_evidence_Events");
   var inventory=inventory(db);
   if(upgrade){var denied=assertThrows(SQLException.class,()->append(ds,org,actor,profile,subject,id(),prior,2,AT,"ITEM"));assertEquals("23514",denied.getSQLState());assertTrue(denied.getMessage().contains("Invalid immutable supersession"));System.out.println("V43_EXPECTED_RED equal recordedAt: "+denied.getSQLState()+" "+denied.getMessage());var v44=Flyway.configure().dataSource(ds).target("44").load();assertEquals(1,v44.migrate().migrationsExecuted);v44.validate();assertEquals(inventory,inventory(db));assertEquals(frozen,db.queryForList("SELECT * FROM mulino_evidence_Events"));}
   String next=id();append(ds,org,actor,profile,subject,next,prior,2,AT,"ITEM");String third=id();append(ds,org,actor,profile,subject,third,next,3,AT,"ITEM");assertTrue(db.queryForList("SELECT * FROM mulino_evidence_Events").containsAll(frozen));
   for(String bad:List.of("earlier","missing","org","subject","kind","revision","self")){
    String row=id();var failure=assertThrows(SQLException.class,()->append(ds,bad.equals("org")?id():org,actor,profile,bad.equals("subject")?id():subject,row,bad.equals("missing")?id():bad.equals("self")?row:third,bad.equals("revision")?5:4,bad.equals("earlier")?AT.minusNanos(1000):AT,bad.equals("kind")?"WORK":"ITEM"));assertEquals("23514",failure.getSQLState(),bad+": "+failure);assertTrue(failure.getMessage().contains("Invalid immutable supersession"),bad);
   }
   // Existing immutable UPDATE/DELETE guards still reject attempts to create a cycle or rewrite history.
   assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("UPDATE mulino_evidence_Events SET supersedesId=? WHERE ID=?",third,prior));assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("DELETE FROM mulino_evidence_Events WHERE ID=?",third));
   // Two actual transactions append the same successor. The original unique index must serialize and reject one.
   String concurrentPrior=third;var pool=Executors.newFixedThreadPool(2);var entered=new CountDownLatch(1);var waiterPid=new java.util.concurrent.atomic.AtomicInteger();
   try(var first=ds.getConnection()){
    first.setAutoCommit(false);append(first,org,actor,profile,subject,id(),concurrentPrior,4,AT,"ITEM");
    var loser=pool.submit(()->{try(var second=ds.getConnection()){try(var q=second.createStatement();var r=q.executeQuery("SELECT pg_backend_pid()")){r.next();waiterPid.set(r.getInt(1));}entered.countDown();append(second,org,actor,profile,subject,id(),concurrentPrior,4,AT,"ITEM");return "ACCEPTED";}catch(SQLException error){return error.getSQLState();}});assertTrue(entered.await(10,TimeUnit.SECONDS));
    long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);boolean blocked=false;while(System.nanoTime()<deadline){blocked=db.queryForObject("SELECT EXISTS(SELECT 1 FROM pg_stat_activity WHERE pid=? AND wait_event_type='Lock')",Boolean.class,waiterPid.get());if(blocked)break;Thread.onSpinWait();}assertTrue(blocked,"Second actual successor must block on the first writer unique fence");
    // PostgreSQL's uniqueness fence is exercised by committing the first actual writer.
    first.commit();assertEquals("23505",loser.get(10,TimeUnit.SECONDS));
   }finally{pool.shutdownNow();}
   assertEquals(4,db.queryForObject("SELECT count(*) FROM mulino_evidence_Events",Integer.class));assertTrue(db.queryForList("SELECT * FROM mulino_evidence_Events").containsAll(frozen));
   System.out.println("V44_PROOF "+(upgrade?"upgrade43-44":"fresh44")+" postgres="+db.queryForObject("SHOW server_version",String.class)+" originalBytesPreserved=true noDDLInventoryChange=true equalInstantChainRevisions=1,2,3,4 duplicateBranchSQLSTATE=23505 container="+pg.getContainerId());
  }
 }
 static Map<String,Object> inventory(JdbcTemplate db){return Map.of("columns",db.queryForList("SELECT table_name,ordinal_position,column_name,data_type,is_nullable,column_default FROM information_schema.columns WHERE table_schema='public' ORDER BY 1,2"),"constraints",db.queryForList("SELECT conrelid::regclass::text,conname,pg_get_constraintdef(oid) FROM pg_constraint WHERE connamespace='public'::regnamespace ORDER BY 1,2"),"triggers",db.queryForList("SELECT tgrelid::regclass::text,tgname,pg_get_triggerdef(oid) FROM pg_trigger WHERE NOT tgisinternal ORDER BY 1,2"),"indexes",db.queryForList("SELECT tablename,indexname,indexdef FROM pg_indexes WHERE schemaname='public' ORDER BY 1,2"));}
 static void append(javax.sql.DataSource ds,String org,String actor,String profile,String subject,String id,String prior,int revision,Instant at,String kind)throws SQLException{try(var c=ds.getConnection()){append(c,org,actor,profile,subject,id,prior,revision,at,kind);}}
 static void append(Connection c,String org,String actor,String profile,String subject,String id,String prior,int revision,Instant at,String kind)throws SQLException{
  try(var q=c.prepareStatement("INSERT INTO mulino_evidence_Events(organizationId,ID,revision,recordedAt,recordedBy,subjectKind,subjectId,kind,sourceNamespace,sourceProfileId,externalEventId,sourceVersion,effectiveFrom,timeZone,timePrecision,valueState,payload,payloadHash,supersedesId) VALUES(?,?,?,?,?,?,?,'RECEIPT','same-instant-fixture',?,?,?,?,'UTC','SECOND','KNOWN',?, ?,?)")){int i=0;for(Object v:List.of(org,id,revision,Timestamp.from(at),actor,kind,subject,profile,subject,"v"+revision,Timestamp.from(AT),revision==1?"{\"quantity\":100}":"{\"quantity\":98}","a".repeat(64)))q.setObject(++i,v);q.setString(++i,prior);q.executeUpdate();}
 }
 static String id(){return UUID.randomUUID().toString();}
}
