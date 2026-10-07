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
class EvidenceReconciliationTest {
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
  @Autowired EvidenceReconciliation reconciliation; @Autowired EvidenceRecordCommands commands;
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
    for(String cap:List.of("getEvidence","getInbox","getReconciliation","attachEvidence","recordActivity","correctEvidence","linkCanonicalOccurrence","matchSourceIdentity","resolveEvidenceConflict")) {
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

  String segment(){
    String id=uuid(),manufacturer=uuid(),lot=uuid(),place=uuid();
    jdbc.update("INSERT INTO mulino_inventory_Manufacturers(organizationId,ID,name) VALUES(?,?, 'Fixture maker')",ORG,manufacturer);
    jdbc.update("INSERT INTO mulino_inventory_ManufacturingLots(organizationId,ID,itemId,manufacturerId,originalLot) VALUES(?,?,?,?,?)",ORG,lot,ITEM,manufacturer,id);
    jdbc.update("INSERT INTO mulino_inventory_Places(organizationId,ID,name,kind) VALUES(?,?, 'Fixture W','WAREHOUSE')",ORG,place);
    jdbc.update("INSERT INTO mulino_inventory_QuantitySegments(organizationId,ID,itemId,lotId,placeId,controlScope,quantity,unit,identificationStatus,mixtureStatus,validFrom) VALUES(?,?,?,?,?,'fixture',60,'BOX','CONFIRMED','IDENTIFIED',?)",ORG,id,ITEM,lot,place,java.sql.Timestamp.from(OCCURRED));
    return id;
  }
  EvidenceReconciliation.Review review(String claim,String doc,String segment,String quantity){return new EvidenceReconciliation.Review(claim,doc,segment,null,"synthetic-v1","warehouse:R60:1",quantity,"BOX",OCCURRED,"Human reviewed original, source, event identity and exact physical scope");}
  @Test void authorizedReviewLinksTwoDocumentsToOneCanonicalSixty(){
    String physical=segment(),event=event(),first=document(),second=document();
    String c1=claim(event,first,"60"),c2=claim(event,second,"60");
    var r1=request(()->reconciliation.match(auth.context(null,null),review(c1,first,physical,"60"),"matchSourceIdentity"));
    assertEquals("MATCHED",r1.get("outcome"));
    var canonical1=request(()->reconciliation.link(auth.context(null,null),r1.get("id").toString()));
    var r2=request(()->reconciliation.match(auth.context(null,null),review(c2,second,physical,"60"),"matchSourceIdentity"));
    var canonical2=request(()->reconciliation.link(auth.context(null,null),r2.get("id").toString()));
    assertEquals(canonical1.get("id"),canonical2.get("id"));
    assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_CanonicalOccurrences",Integer.class));
    assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM mulino_inventory_QuantityMovements",Integer.class));
  }
  @Test void oneSourceObservationCannotBeCountedAgainstTwoPhysicalScopes(){
    String physical=segment(),otherPhysical=segment(),event=event(),doc=document(),claim=claim(event,doc,"60");
    var first=request(()->reconciliation.match(auth.context(null,null),review(claim,doc,physical,"60"),"matchSourceIdentity"));
    request(()->reconciliation.link(auth.context(null,null),first.get("id").toString()));
    var conflicting=request(()->reconciliation.match(auth.context(null,null),review(claim,doc,otherPhysical,"60"),"matchSourceIdentity"));
    assertEquals("CONFLICT",conflicting.get("outcome"));
    assertThrows(DomainError.class,()->request(()->reconciliation.link(auth.context(null,null),conflicting.get("id").toString())));
    assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_CanonicalOccurrences",Integer.class));
  }
  @Test void unknownIdentityAndQuantityMismatchRetainIntakeResponsibility(){
    String event=event(),doc=document(),claim=claim(event,doc,"60"),physical=segment();
    var result=request(()->reconciliation.match(auth.context(null,null),review(claim,doc,physical,"58"),"matchSourceIdentity"));
    assertEquals("UNVERIFIED",result.get("outcome"));
    assertThrows(DomainError.class,()->request(()->reconciliation.link(auth.context(null,null),result.get("id").toString())));
    var persisted=jdbc.queryForMap("SELECT intakeOwnerId,nextAction,nextCheckAt FROM mulino_evidence_Reconciliations WHERE ID=?",result.get("id"));
    assertEquals(ACTOR,persisted.get("intakeOwnerId"));assertNotNull(persisted.get("nextAction"));assertNotNull(persisted.get("nextCheckAt"));
    assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_CanonicalOccurrences",Integer.class));
  }
  @Test void competingSourceHashCannotBeResolvedBySelectingPriority(){
    String event=event(),doc=document(),claim=claim(event,doc,"60"),physical=segment();
    request(()->records.recordActivity(event("R60","1","different original claim",null)));
    var result=request(()->reconciliation.match(auth.context(null,null),review(claim,doc,physical,"60"),"resolveEvidenceConflict"));
    assertEquals("CONFLICT",result.get("outcome"));
    assertThrows(DomainError.class,()->request(()->reconciliation.link(auth.context(null,null),result.get("id").toString())));
    assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_Events",Integer.class));
    assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("UPDATE mulino_evidence_Reconciliations SET decision='MATCHED' WHERE ID=?",result.get("id")));
  }
  @Test void publicSlotsRejectBooleanProofAndCrossSourceGrant(){
    var slots=new LinkedHashMap<String,Object>();slots.put("subject",Map.of("kind","ITEM","id",ITEM));slots.put("sourceNamespace","warehouse");slots.put("sourceMatched",true);
    assertThrows(DomainError.class,()->request(()->commands.prepare(auth.context(null,null),Map.of("capabilityId","recordActivity","slots",slots))));
    String event=event(),doc=document(),claim=claim(event,doc,"60"),physical=segment();
    jdbc.update("DELETE FROM mulino_identity_GrantScopes WHERE grantId=?",GRANT);
    jdbc.update("INSERT INTO mulino_identity_GrantScopes VALUES(?,?,'SOURCE',?)",ORG,GRANT,uuid());
    assertThrows(DomainError.class,()->request(()->reconciliation.match(auth.context(null,null),review(claim,doc,physical,"60"),"matchSourceIdentity")));
  }
  @Test void correctionRequiresAtomicImpactImplementation(){
    String previous=event();
    var slots=new LinkedHashMap<String,Object>();slots.put("subject",Map.of("kind","ITEM","id",ITEM));slots.put("kind","RECEIPT");slots.put("sourceNamespace","warehouse");slots.put("externalEventId","R60");slots.put("sourceVersion","2");slots.put("effectiveFrom",OCCURRED.toString());slots.put("timeZone","Asia/Seoul");slots.put("timePrecision","SECOND");slots.put("valueState","KNOWN");slots.put("payload","actual58");slots.put("supersedesId",previous);
    var unavailable=new EvidenceRecordCommands(repository,records,auth,new org.springframework.beans.factory.support.DefaultListableBeanFactory().getBeanProvider(EvidenceCorrectionImpact.class));
    assertThrows(DomainError.class,()->request(()->unavailable.execute(auth.context(null,null),Map.of("capabilityId","correctEvidence","slots",slots))));
    assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_Events",Integer.class));
  }
}
