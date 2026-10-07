package com.mulino.application.evidence;

import static org.junit.jupiter.api.Assertions.*;
import com.mulino.application.core.*;
import com.mulino.application.identity.IdentityAuthorization;
import com.mulino.adapters.blob.LocalBlobStore;
import com.mulino.domain.evidence.*;
import com.sap.cds.services.runtime.CdsRuntime;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static com.mulino.domain.evidence.EvidenceTypes.*;
import static com.mulino.application.evidence.EvidenceRecords.*;

@SpringBootTest
@ActiveProfiles("local")
class EvidencePersistenceTest {
  static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280");
  static final Path BLOBS;
  static { try { BLOBS=Files.createTempDirectory("mulino-evidence-test-"); }catch(Exception e){throw new IllegalStateException(e);} PG.start(); }
  static final String ORG=uuid(),ACTOR=uuid(),OWNER=uuid(),OTHER=uuid(),ITEM=uuid(),SOURCE=uuid(),GRANT=uuid();
  static final Instant OCCURRED=Instant.parse("2026-10-07T09:00:00Z");
  @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url",PG::getJdbcUrl);r.add("spring.datasource.username",PG::getUsername);r.add("spring.datasource.password",PG::getPassword);
    r.add("JWT_PUBLIC_KEY",()->System.getenv("JWT_PUBLIC_KEY"));r.add("JWT_ISSUER",()->"https://fixture.invalid");r.add("JWT_AUDIENCE",()->"mulino-acceptance");
    r.add("mulino.evidence.blob-root",BLOBS::toString);
  }
  @Autowired JdbcTemplate jdbc; @Autowired IdentityAuthorization auth; @Autowired EvidenceRecords records;
  @Autowired EvidenceQueries queries; @Autowired EvidenceRepository repository; @Autowired LocalBlobStore blobs;
  @Autowired CdsRuntime runtime; @Autowired PlatformTransactionManager tx;
  static String uuid(){return UUID.randomUUID().toString();}
  @BeforeEach void seed() {
    for(String directory:List.of("objects","staging"))try(var files=Files.list(BLOBS.resolve(directory))){for(Path file:files.toList())Files.delete(file);}catch(Exception e){throw new IllegalStateException(e);}
    jdbc.execute("TRUNCATE mulino_evidence_SourceProfiles, mulino_inventory_TradeItems,mulino_identity_Organizations CASCADE");
    jdbc.update("INSERT INTO mulino_identity_Organizations(ID,externalAlias) VALUES(?,'ORG-A'),(?,'ORG-B')",ORG,OTHER);
    jdbc.update("INSERT INTO mulino_identity_Actors(organizationId,ID,kind,stableRequestOwner) VALUES(?,?,'HUMAN',?)",ORG,ACTOR,OWNER);
    jdbc.update("INSERT INTO mulino_identity_ExternalIdentities(organizationId,ID,actorId,issuer,subject,organizationAlias) VALUES(?,?,?,'https://fixture.invalid','recorder','ORG-A')",ORG,uuid(),ACTOR);
    jdbc.update("INSERT INTO mulino_identity_Memberships(organizationId,ID,actorId,validFrom,validUntil) VALUES(?,?,?,CURRENT_TIMESTAMP-INTERVAL '1 day',CURRENT_TIMESTAMP+INTERVAL '1 day')",ORG,uuid(),ACTOR);
    jdbc.update("INSERT INTO mulino_identity_Grants(organizationId,ID,actorId,delegatorId,validFrom,validUntil) VALUES(?,?,?,?,CURRENT_TIMESTAMP-INTERVAL '1 day',CURRENT_TIMESTAMP+INTERVAL '1 day')",ORG,GRANT,ACTOR,ACTOR);
    for(String cap:List.of("getEvidence","getInbox","attachEvidence","recordActivity","correctEvidence","linkCanonicalOccurrence")) {
      jdbc.update("INSERT INTO mulino_identity_CapabilityAssignments(organizationId,ID,actorId,capabilityId,scopeKind,scopeId,validFrom,validUntil) VALUES(?,?,?,?,'ORGANIZATION',?,CURRENT_TIMESTAMP-INTERVAL '1 day',CURRENT_TIMESTAMP+INTERVAL '1 day')",ORG,uuid(),ACTOR,cap,ORG);
      jdbc.update("INSERT INTO mulino_identity_GrantActions VALUES(?,?,?)",ORG,GRANT,cap);
    }
    jdbc.update("INSERT INTO mulino_identity_GrantScopes VALUES(?,?,'ORGANIZATION',?)",ORG,GRANT,ORG);
    jdbc.update("INSERT INTO mulino_identity_AuthorityFences VALUES(?,?,1)",ORG,ACTOR);
    String product=uuid(),spec=uuid(),pack=uuid();
    jdbc.update("INSERT INTO mulino_inventory_Products(organizationId,ID,name) VALUES(?,?,'Synthetic product')",ORG,product);
    for(String table:List.of("SpecificationVersions","PackagingVersions"))jdbc.update("INSERT INTO mulino_inventory_"+table+"(organizationId,ID,productId,version,contentHash) VALUES(?,?,?,'1',?)",ORG,table.startsWith("Spec")?spec:pack,product,"a".repeat(64));
    jdbc.update("INSERT INTO mulino_inventory_TradeItems(organizationId,ID,productId,name,baseUnit,decimalPlaces,specificationVersionId,packagingVersionId) VALUES(?,?,?,'Synthetic biscuit','BOX',0,?,?)",ORG,ITEM,product,spec,pack);
    jdbc.update("INSERT INTO mulino_evidence_SourceProfiles(ID,organizationId,revision,recordedAt,recordedBy,namespace,policyVersion,intakeOwnerId,supervisorId,nextAction,nextCheckAt) VALUES(?,?,1,CURRENT_TIMESTAMP,?,'warehouse','synthetic-v1',?,?,'Compare source',CURRENT_TIMESTAMP+INTERVAL '1 hour')",SOURCE,ORG,ACTOR,ACTOR,ACTOR);
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("local-test-context").header("alg","RS256").issuer("https://fixture.invalid").subject("recorder").claim("organizationId","ORG-A").build(),List.of()));
  }
  @AfterEach void clear(){SecurityContextHolder.clearContext();}
  <T>T request(Callable<T> work){return runtime.requestContext().run(ctx->{try{return work.call();}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}});}
  DocumentInput document(String prior){return new DocumentInput(new Subject(SubjectKind.ITEM,ITEM),"warehouse","source-reference","text/plain",LocalBlobStore.hash("original".getBytes()),"SYNTHETIC fixture",prior);}
  String document(){return request(()->records.attachDocument(document(null),"original".getBytes())).get("id").toString();}
  EventInput event(String sourceId,String version,String payload,String prior){return new EventInput(new Subject(SubjectKind.ITEM,ITEM),"RECEIPT","warehouse",sourceId,version,new EffectiveTime(OCCURRED,null,"Asia/Seoul","SECOND"),ValueState.KNOWN,payload,prior,null);}
  String event(){return request(()->records.recordActivity(event("R60","1","receipt60",null))).get("id").toString();}
  String claim(String event,String doc,String quantity){return request(()->records.recordClaim(new ClaimInput(event,doc,"Receipt reported",quantity,"BOX",ValueState.KNOWN,null))).get("id").toString();}
  QueryResult read(String kind,String id,Instant knownAt){return request(()->queries.query(auth.context(OCCURRED.plusSeconds(100),knownAt),new QueryRequest("getEvidence",id,Map.of("kind",kind),Map.of(),50,null,null,OCCURRED.plusSeconds(100),knownAt,null)));}
  @Test void originalFileHashAndAuthorizedDownloadAreImmutable(){
    String doc=document();var output=(Map<?,?>)read("DOCUMENT",doc,Instant.now().plusSeconds(1)).data();
    assertEquals("AVAILABLE",output.get("availability"));assertFalse(output.containsKey("blobId"));
    assertArrayEquals("original".getBytes(),request(()->queries.download(doc)).bytes());
    assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("UPDATE mulino_evidence_DocumentVersions SET provenance='changed' WHERE ID=?",doc));
    jdbc.update("UPDATE mulino_identity_Grants SET revokedAt=CURRENT_TIMESTAMP WHERE ID=?",GRANT);
    assertThrows(DomainError.class,()->request(()->queries.download(doc)));
  }
  @Test void stagingHashMismatchAndDatabaseRollbackLeaveNoReferencesOrObjects()throws Exception{
    long before;try(var objects=Files.list(BLOBS.resolve("objects"))){before=objects.count();}
    assertThrows(DomainError.class,()->request(()->records.attachDocument(document(null),"wrong".getBytes())));
    request(()->new TransactionTemplate(tx).execute(status->{records.attachDocument(document(null),"original".getBytes());status.setRollbackOnly();return null;}));
    assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_DocumentVersions",Integer.class));
    try(var objects=Files.list(BLOBS.resolve("objects"))){assertEquals(before,objects.count());}
  }
  @Test void unavailableUriNeverFetchesOriginal(){
    var input=new DocumentInput(new Subject(SubjectKind.ITEM,ITEM),"warehouse","file:///etc/passwd","text/plain","0".repeat(64),"External-only fixture",null);
    String id=request(()->records.recordUnavailableDocument(input)).get("id").toString();
    assertEquals("UNKNOWN",((Map<?,?>)read("DOCUMENT",id,Instant.now().plusSeconds(1)).data()).get("availability"));
    assertThrows(DomainError.class,()->request(()->queries.download(id)));
  }
  @Test void duplicateSourceKeyReplaysOnceAndCompetingHashPreservesConflict(){
    String original=event();var replay=request(()->records.recordActivity(event("R60","1","receipt60",null)));
    assertEquals(original,replay.get("id"));assertEquals("REPLAYED",replay.get("outcome"));
    var conflict=request(()->records.recordActivity(event("R60","1","receipt58",null)));
    assertEquals("EVIDENCE_CONFLICT",conflict.get("outcome"));assertNotEquals(original,conflict.get("id"));
    assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_Events",Integer.class));
    assertEquals("receipt60",jdbc.queryForObject("SELECT payload FROM mulino_evidence_Events WHERE ID=?",String.class,original));
    assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_InboxRecords WHERE state='CONFLICT' AND intakeOwnerId IS NOT NULL AND nextCheckAt IS NOT NULL",Integer.class));
  }
  @Test void supersedesPreservesKnowledgeTimeAndFourUnknownStates(){
    String original=event();Instant before=Instant.now();String corrected=request(()->records.recordActivity(event("R60","2","receipt58",original))).get("id").toString();
    assertEquals(false,((Map<?,?>)read("EVENT",original,before).data()).get("superseded"));
    assertEquals(true,((Map<?,?>)read("EVENT",original,Instant.now().plusSeconds(1)).data()).get("superseded"));
    assertEquals(original,((Map<?,?>)read("EVENT",corrected,Instant.now().plusSeconds(1)).data()).get("supersedesId"));
    for(ValueState state:List.of(ValueState.MISSING,ValueState.UNKNOWN,ValueState.NOT_APPLICABLE,ValueState.CONFLICT)){
      var input=new EventInput(new Subject(SubjectKind.ITEM,ITEM),"TEMPERATURE","warehouse",state.name(),"1",new EffectiveTime(OCCURRED,null,"Asia/Seoul","SECOND"),state,state.name(),null,null);
      String id=request(()->records.recordActivity(input)).get("id").toString();assertEquals(state.name(),((Map<?,?>)read("EVENT",id,Instant.now().plusSeconds(1)).data()).get("valueState"));
    }
  }
  @Test void twoDocumentsOneCanonicalReceiptCountsOnlyOneAndHasNoInventoryEffects(){
    String e=event(),doc=document(),claim=claim(e,doc,"60"),physical=uuid();
    var first=request(()->records.verifyCanonical(new CanonicalInput(claim,physical,doc,"synthetic-v1",true,true,true,true,true,null,null,"Manual fixture reconciliation")));
    String canonical=first.get("id").toString();String doc2=document();String e2=request(()->records.recordActivity(event("second-report","1","receipt60",null))).get("id").toString();String claim2=claim(e2,doc2,"60");
    request(()->records.verifyCanonical(new CanonicalInput(claim2,physical,doc2,"synthetic-v1",true,true,true,true,true,canonical,null,"Second report same occurrence")));
    assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_CanonicalOccurrences",Integer.class));
    assertEquals("60.000000000000",jdbc.queryForObject("SELECT quantity::text FROM mulino_evidence_CanonicalOccurrences",String.class));
    assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_Verifications",Integer.class));
    assertEquals("NOT_IMPLEMENTED",first.get("inventoryEffects"));
    assertEquals(true,((Map<?,?>)read("CANONICAL",canonical,Instant.now().plusSeconds(1)).data()).get("verified"));
  }
  @Test void sourceConflictCannotBecomeCanonicalVerified(){
    String e=event(),doc=document(),claim=claim(e,doc,"60");request(()->records.recordActivity(event("R60","1","receipt58",null)));
    assertThrows(DomainError.class,()->request(()->records.verifyCanonical(new CanonicalInput(claim,uuid(),doc,"synthetic-v1",true,true,true,true,true,null,null,"Conflict not resolved"))));
    assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_CanonicalOccurrences",Integer.class));
  }
  @Test void orphanCleanupPreservesReferencedFilesAndMissingFileReturnsUnknown()throws Exception{
    String doc=document();var staged=blobs.stage("orphan".getBytes(),LocalBlobStore.hash("orphan".getBytes()));blobs.commit(staged);
    Files.setLastModifiedTime(BLOBS.resolve("objects").resolve(staged.id().toString()),java.nio.file.attribute.FileTime.from(Instant.now().minusSeconds(7200)));
    assertEquals(1,blobs.cleanupOrphans(Instant.now().minusSeconds(3601),repository::referenced));
    String blob=jdbc.queryForObject("SELECT blobId FROM mulino_evidence_DocumentVersions WHERE ID=?",String.class,doc);Files.delete(BLOBS.resolve("objects").resolve(blob));
    assertEquals("UNKNOWN",((Map<?,?>)read("DOCUMENT",doc,Instant.now().plusSeconds(1)).data()).get("availability"));
  }
  @Test void foreignOrganizationActorReferenceAndTypedDateAsPlaceFailClosed(){
    assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("INSERT INTO mulino_evidence_SourceProfiles(ID,organizationId,revision,recordedAt,recordedBy,namespace,policyVersion,intakeOwnerId,supervisorId,nextAction,nextCheckAt) VALUES(?,?,1,CURRENT_TIMESTAMP,?,'foreign','v1',?,?,'check',CURRENT_TIMESTAMP)",uuid(),OTHER,ACTOR,ACTOR,ACTOR));
    assertThrows(DomainError.class,()->new Subject(SubjectKind.PLACE,"2026-10-07"));
    assertThrows(DomainError.class,()->request(()->queries.query(auth.context(OCCURRED,Instant.now()),new QueryRequest("getEvidence",null,Map.of("subjectKind","PLACE","subjectId","2026-10-07"),Map.of(),50,null,null,OCCURRED,Instant.now(),null))));
  }
  @Test void concurrentSourceReplayHasExactlyOneInboxAndEvent()throws Exception{
    var authentication=SecurityContextHolder.getContext().getAuthentication();
    var barrier=new java.util.concurrent.CyclicBarrier(2);
    try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)) {
      Callable<String> writer=()->{SecurityContextHolder.getContext().setAuthentication(authentication);try{barrier.await();return event();}finally{SecurityContextHolder.clearContext();}};
      var a=executor.submit(writer);var b=executor.submit(writer);assertEquals(a.get(),b.get());
    }
    assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_Events",Integer.class));
    assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_InboxRecords",Integer.class));
  }
  @Test void pageMetadataBelongsOnlyToCurrentPageAndCursorCannotChangeQuery(){
    document();document();Instant known=Instant.now().plusSeconds(1);
    QueryRequest first=new QueryRequest("getEvidence",null,Map.of("organizationId",ORG,"kind","DOCUMENT"),Map.of(),1,null,null,OCCURRED,known,null);
    QueryResult page=request(()->queries.query(auth.context(OCCURRED,known),first));assertEquals(1,((List<?>)page.data()).size());assertEquals(1,page.evidenceRefs().size());assertNotNull(page.nextCursor());
    QueryRequest second=new QueryRequest("getEvidence",null,first.scope(),first.filters(),1,page.nextCursor(),null,OCCURRED,known,null);
    QueryResult next=request(()->queries.query(auth.context(OCCURRED,known),second));assertNotEquals(page.evidenceRefs(),next.evidenceRefs());assertNull(next.nextCursor());
    assertThrows(DomainError.class,()->request(()->queries.query(auth.context(OCCURRED,known),new QueryRequest("getEvidence",null,Map.of("kind","EVENT"),Map.of(),1,page.nextCursor(),null,OCCURRED,known,null))));
    assertThrows(DomainError.class,()->request(()->queries.query(auth.context(OCCURRED,known),new QueryRequest("getEvidence",null,Map.of("kind","DOCUMENT"),Map.of("verifiedOnly","true"),1,null,null,OCCURRED,known,null))));
  }
  @Test void sourceScopedGrantCannotReadOtherSourceAndInvalidQuantityCannotBeRecorded(){
    String e=event(),doc=document();
    jdbc.update("INSERT INTO mulino_identity_GrantScopes VALUES(?,?,'SOURCE',?)",ORG,GRANT,uuid());
    assertThrows(DomainError.class,()->read("DOCUMENT",doc,Instant.now().plusSeconds(1)));
    jdbc.update("DELETE FROM mulino_identity_GrantScopes WHERE organizationId=? AND scopeKind='SOURCE'",ORG);
    assertThrows(DomainError.class,()->claim(e,doc,"0.5"));
    assertThrows(DomainError.class,()->claim(e,doc,"1E+38"));
    assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_Claims",Integer.class));
  }
  @Test void lateCompetingSourceHashPreservesHistoricalVerificationAndBlocksCurrentConfirmation(){
    String e=event(),doc=document(),claim=claim(e,doc,"60");
    String canonical=request(()->records.verifyCanonical(new CanonicalInput(claim,uuid(),doc,"synthetic-v1",true,true,true,true,true,null,null,"Verified before competing report"))).get("id").toString();
    Instant before=Instant.now();request(()->records.recordActivity(event("R60","1","receipt58",null)));
    assertEquals(true,((Map<?,?>)read("CANONICAL",canonical,before).data()).get("verified"));
    assertEquals(false,((Map<?,?>)read("CANONICAL",canonical,Instant.now().plusSeconds(1)).data()).get("verified"));
  }

  @Test void stagedAndCommittedFilesHaveOwnerOnlyPermissions()throws Exception{
    var staged=blobs.stage("private".getBytes(),LocalBlobStore.hash("private".getBytes()));
    assertEquals(java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"),Files.getPosixFilePermissions(BLOBS.resolve("staging")));
    assertEquals(java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"),Files.getPosixFilePermissions(BLOBS.resolve("objects")));
    assertEquals(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"),Files.getPosixFilePermissions(BLOBS.resolve("staging").resolve(staged.id().toString())));
    blobs.commit(staged);assertEquals(java.nio.file.attribute.PosixFilePermissions.fromString("r--------"),Files.getPosixFilePermissions(BLOBS.resolve("objects").resolve(staged.id().toString())));
    blobs.discard(staged.id());
  }
  @Test void dateOnlyRangeAndUtcInstantRoundTripPreserveTemporalBounds(){
    var day=new EventInput(new Subject(SubjectKind.ITEM,ITEM),"OBSERVATION","warehouse","day","1",new EffectiveTime(OCCURRED,OCCURRED.plusSeconds(86400),"Asia/Seoul","DAY"),ValueState.UNKNOWN,"Date-range fixture",null,null);
    String id=request(()->records.recordActivity(day)).get("id").toString();
    var observed=(Map<?,?>)read("EVENT",id,Instant.now().plusSeconds(1)).data();assertEquals(OCCURRED,instant(observed.get("effectiveFrom")));assertEquals(OCCURRED.plusSeconds(86400),instant(observed.get("effectiveUntil")));assertEquals("Asia/Seoul",observed.get("timeZone"));assertEquals("DAY",observed.get("timePrecision"));
    Instant end=OCCURRED.plusSeconds(86400),known=Instant.now().plusSeconds(1);
    assertThrows(DomainError.class,()->request(()->queries.query(auth.context(end,known),new QueryRequest("getEvidence",id,Map.of("kind","EVENT"),Map.of(),50,null,null,end,known,null))));
  }

}
