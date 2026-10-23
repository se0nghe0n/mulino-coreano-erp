package com.mulino.quality;

import com.mulino.application.core.*;
import com.sap.cds.services.runtime.CdsRuntime;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/** Uses the existing real command/evidence fixture without inheriting its unrelated test methods. */
@SpringBootTest
@ActiveProfiles("local")
class PublicScopeEligibilityPostgresTest {
  static final FulfillmentPostgresTest fixture = new FulfillmentPostgresTest();
  @DynamicPropertySource static void properties(DynamicPropertyRegistry r) { FulfillmentPostgresTest.properties(r); }
  @Autowired ApplicationContext context;
  @Autowired ApplicationQueries queries;
  @Autowired CdsRuntime runtime;
  @Autowired JdbcTemplate jdbc;
  @MockitoBean org.springframework.security.oauth2.jwt.JwtDecoder decoder;
  @MockitoBean ExecutionClock clock;

  @BeforeEach void seed(TestInfo info) throws Exception {
    context.getAutowireCapableBeanFactory().autowireBean(fixture);
    fixture.clock=clock;
    fixture.now=Instant.parse("2026-10-08T00:00:00Z");
    when(clock.instant()).thenAnswer(x->fixture.now);
    fixture.seed(info);
    fixture.saleFixture();
    capability("evaluateEligibility");
  }
  @AfterEach void clear(){SecurityContextHolder.clearContext();}
  static {
    // The helper's container is shared if its original class is selected in the same JVM.
    // Testcontainers owns container cleanup; release shared blob storage only after every test finishes.
    Runtime.getRuntime().addShutdownHook(new Thread(()->{
      try(var files=Files.walk(FulfillmentPostgresTest.BLOBS)) {
        for(Path path:files.sorted(Comparator.reverseOrder()).toList())Files.delete(path);
        String report=System.getProperty("public.scope.cleanupReport");
        if(report!=null)Files.writeString(Path.of(report),"blobRoot="+FulfillmentPostgresTest.BLOBS+"\nblobRootExists="+Files.exists(FulfillmentPostgresTest.BLOBS)+"\ncontainerId="+FulfillmentPostgresTest.PG.getContainerId()+"\n");
      } catch(java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
    }));
  }
  void capability(String name) {
    fixture.insert("mulino_identity_CapabilityAssignments",fixture.map("organizationId",fixture.org,"ID",fixture.id(),"actorId",fixture.actor,"capabilityId",name,"scopeKind","ORGANIZATION","scopeId",fixture.org,"validFrom",Timestamp.from(fixture.now.minusSeconds(1000)),"validUntil",Timestamp.from(fixture.now.plusSeconds(10000))));
    fixture.insert("mulino_identity_GrantActions",fixture.map("organizationId",fixture.org,"grantId",fixture.grant,"capabilityId",name));
  }
  Map<String,Object> read(String segment,Map<String,Object> scope,Map<String,Object> filters,Instant asOf,Instant knownAt) {
    return runtime.requestContext().run(ctx->{return queries.query(new QueryRequest("evaluateEligibility",segment,scope,filters,200,null,"definition-v1",asOf,knownAt,null));});
  }
  Map<String,Object> read(String segment,Map<String,Object> scope,Map<String,Object> filters) {return read(segment,scope,filters,fixture.now,fixture.now);}
  @SuppressWarnings("unchecked") Map<String,Object> data(Map<String,Object> response){return (Map<String,Object>)response.get("data");}
  @SuppressWarnings("unchecked") List<Map<String,Object>> segments(Map<String,Object> response){return (List<Map<String,Object>>)data(response).get("segments");}
  Map<String,Object> scope(){return fixture.map("organizationId",fixture.org,"itemId",fixture.item);}
  void equivalent(Map<String,Object> collection) {
    for(var row:segments(collection)) {
      var filters=fixture.map("action",row.get("action"));
      if(row.containsKey("customerId"))filters.put("customerId",row.get("customerId"));
      var single=read((String)row.get("segmentId"),Map.of(),filters,Instant.parse((String)collection.get("asOf")),Instant.parse((String)collection.get("knownAt")));
      data(single).forEach((key,value)->assertEquals(value,row.get(key),key));
      assertEquals(single.get("unknowns"),row.get("unknowns"));
      assertEquals(single.get("evidenceRefs"),row.get("evidenceRefs"));
    }
  }
  String extra(String place,String lot) {return extra(place,lot,fixture.now.minusSeconds(10),fixture.now.minusSeconds(10));}
  String extra(String place,String lot,Instant known,Instant valid) {
    String segment=fixture.id();
    fixture.record("mulino_inventory_QuantitySegments",segment,fixture.map("itemId",fixture.item,"lotId",lot,"identificationStatus","CONFIRMED","quantity",new BigDecimal("100"),"unit","EA","placeId",place,"controlScope","quality","validFrom",Timestamp.from(valid),"recordedAt",Timestamp.from(known),"mixtureStatus","IDENTIFIED","ownerId",fixture.actor,"custodianId",fixture.actor));
    return segment;
  }
  String basis(String category,String start,String amount,String customer) {
    var slots=fixture.permission("recordDispositionBasis",category,amount);
    slots.put("startQuantity",start);if(customer!=null)slots.put("customerId",customer);
    String doc=fixture.evidence(slots,"QUALITY_"+category);
    slots.remove("operation");slots.remove("sourceDecisionId");slots.remove("sourceVersion");
    slots.putAll(fixture.map("evidenceId",doc,"reason","Exact public test range","nextCheckAt",fixture.now.plusSeconds(60).toString()));
    fixture.run("recordDispositionBasis",slots,0);return doc;
  }
  void allow(String start,String amount,String customer) {
    fixture.regulatory("100");
    for(String category:List.of("QC","CUSTOMER","COMMERCIAL"))basis(category,start,amount,category.equals("CUSTOMER")?customer:null);
  }
  String fingerprint() {
    var tables=jdbc.queryForList("SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename LIKE 'mulino_%' ORDER BY tablename",String.class);
    var snapshot=new StringBuilder();
    for(String table:tables)snapshot.append(table).append(jdbc.queryForObject("SELECT coalesce(jsonb_agg(to_jsonb(t) ORDER BY to_jsonb(t)::text)::text,'[]') FROM "+table+" t",String.class));
    return com.mulino.domain.definitions.DefinitionRepository.sha256(snapshot.toString());
  }
  @Test void exactPerSegmentRangesCustomersPlacesAndReadOnlySnapshot() {
    allow("0","40",fixture.customer);String first=fixture.segment;
    var firstResult=read(first,Map.of(),Map.of("customerId",fixture.customer));
    var firstRefs=new HashSet<>((List<?>)firstResult.get("evidenceRefs"));
    String warehouse=fixture.id();fixture.record("mulino_inventory_Places",warehouse,fixture.map("name","second warehouse","kind","INTERNAL_STORAGE"));
    String second=extra(warehouse,fixture.lot);fixture.segment=second;
    String otherCustomer=fixture.id();allow("20","30",otherCustomer);
    var hold=fixture.permission("placeHold","QC","10");hold.put("startQuantity","70");hold.put("validUntil",fixture.now.plusSeconds(10).toString());
    String holdDoc=fixture.evidence(hold,"HOLD");hold.remove("operation");hold.remove("sourceDecisionId");hold.remove("sourceVersion");
    hold.putAll(fixture.map("evidenceId",holdDoc,"reason","Unrelated segment boundary","nextCheckAt",fixture.now.plusSeconds(5).toString()));fixture.run("placeHold",hold,0);
    String transit=extra(fixture.transit,fixture.lot);
    String before=fingerprint();
    var result=read(null,scope(),Map.of("customerId",fixture.customer));
    assertEquals(3,segments(result).size());equivalent(result);
    assertFalse(data(result).containsKey("eligibleQuantity"));
    var firstRow=segments(result).stream().filter(r->first.equals(r.get("segmentId"))).findFirst().orElseThrow();
    assertEquals("40",firstRow.get("eligibleQuantity"));assertEquals(firstRefs,new HashSet<>((List<?>)firstRow.get("evidenceRefs")));
    data(firstResult).forEach((key,value)->assertEquals(value,firstRow.get(key),"Unrelated segment must not change "+key));
    var secondRow=segments(result).stream().filter(r->second.equals(r.get("segmentId"))).findFirst().orElseThrow();
    assertEquals("0",secondRow.get("eligibleQuantity"));assertEquals("UNKNOWN",secondRow.get("eligibilityStatus"));
    var matching=read(null,scope(),Map.of("customerId",otherCustomer));equivalent(matching);
    var range=segments(matching).stream().filter(r->second.equals(r.get("segmentId"))).findFirst().orElseThrow();
    assertEquals(List.of(Map.of("startQuantity","20","quantity","30")),range.get("allowedRanges"));
    var scoped=scope();scoped.put("placeId",warehouse);
    assertEquals(List.of(second),data(read(null,scoped,Map.of())).get("segmentIds"));
    scoped.put("placeId",fixture.transit);
    var outside=read(null,scoped,Map.of());assertEquals(List.of(transit),data(outside).get("segmentIds"));
    assertEquals("DENIED",segments(outside).getFirst().get("eligibilityStatus"));assertEquals("0",segments(outside).getFirst().get("eligibleQuantity"));
    var inventory=runtime.requestContext().run(ctx->{return queries.query(new QueryRequest("getInventory",null,scope(),Map.of(),200,null,null,fixture.now,fixture.now,null));});
    assertEquals("300",data(inventory).get("heldQuantity"));
    assertEquals(before,fingerprint());
    assertEquals(result.get("snapshotRevision"),read(null,scope(),Map.of("customerId",fixture.customer)).get("snapshotRevision"));
  }
  @Test void exactAuthorityAndNoEvidenceReadNeverBecomePositive() {
    allow("0","40",fixture.customer);
    jdbc.update("DELETE FROM mulino_identity_GrantActions WHERE grantId=? AND capabilityId='getEvidence'",fixture.grant);
    var single=read(fixture.segment,Map.of(),Map.of());var all=read(null,scope(),Map.of());
    assertEquals("UNKNOWN",data(single).get("eligibilityStatus"));assertEquals("0",segments(all).getFirst().get("eligibleQuantity"));
    equivalent(read(null,scope(),Map.of("customerId",fixture.customer)));
    assertThrows(DomainError.class,()->runtime.requestContext().run(ctx->{return queries.query(new QueryRequest("getEvidence",null,Map.of(),Map.of(),50,null,null,fixture.now,fixture.now,null));}));
    // The existing internal fulfillment path still verifies its own sources without borrowing public getEvidence.
    assertNotNull(fixture.reserve("0","20"));
  }
  @ParameterizedTest @CsvSource({"Memberships,revokedAt","Memberships,validUntil","CapabilityAssignments,revokedAt","CapabilityAssignments,validUntil","Grants,revokedAt","Grants,validUntil"})
  void revokedAndExpiredCurrentAuthorityCannotReadHistoricalScope(String table,String column) {
    String extra=table.equals("CapabilityAssignments")?" AND capabilityId='evaluateEligibility'":"";
    jdbc.update("UPDATE mulino_identity_"+table+" SET "+column+"=? WHERE actorId=?"+extra,Timestamp.from(fixture.now),fixture.actor);
    assertEquals("FORBIDDEN",assertThrows(DomainError.class,()->read(null,scope(),Map.of(),fixture.now.minusSeconds(5),fixture.now.minusSeconds(5))).code());
  }
  @Test void allOfScopesIncludeEveryReturnedTargetAndOptionalWork() {
    String allowed=fixture.segment,denied=extra(fixture.place,fixture.lot);
    jdbc.update("DELETE FROM mulino_identity_GrantScopes WHERE grantId=?",fixture.grant);
    for(var entry:Map.of("TARGET",allowed,"ITEM",fixture.item,"PLACE",fixture.place,"WORK",fixture.work).entrySet())
      fixture.insert("mulino_identity_GrantScopes",fixture.map("organizationId",fixture.org,"grantId",fixture.grant,"scopeKind",entry.getKey(),"scopeId",entry.getValue()));
    var request=scope();request.put("workId",fixture.work);
    assertEquals(List.of(allowed),data(read(null,request,Map.of())).get("segmentIds"));
    assertEquals("FORBIDDEN",assertThrows(DomainError.class,()->read(denied,Map.of(),Map.of())).code());
    assertEquals("FORBIDDEN",assertThrows(DomainError.class,()->read(null,scope(),Map.of())).code());
    for(String dimension:List.of("TARGET","ITEM","PLACE","WORK")) {
      String original=jdbc.queryForObject("SELECT scopeId FROM mulino_identity_GrantScopes WHERE grantId=? AND scopeKind=?",String.class,fixture.grant,dimension);
      jdbc.update("UPDATE mulino_identity_GrantScopes SET scopeId=? WHERE grantId=? AND scopeKind=?",fixture.id(),fixture.grant,dimension);
      assertEquals("FORBIDDEN",assertThrows(DomainError.class,()->read(null,request,Map.of())).code(),dimension);
      jdbc.update("UPDATE mulino_identity_GrantScopes SET scopeId=? WHERE grantId=? AND scopeKind=?",original,fixture.grant,dimension);
    }
    request.put("organizationId",fixture.id());assertEquals("FORBIDDEN",assertThrows(DomainError.class,()->read(null,request,Map.of())).code());
  }
  @ParameterizedTest @CsvSource({"revokedAt","validUntil"})
  void revokedDelegatorAndMissingRoleFailClosed(String boundary) {
    String parent=fixture.manager;
    jdbc.update("UPDATE mulino_identity_Grants SET delegatorId=? WHERE ID=?",parent,fixture.grant);
    assertEquals("FORBIDDEN",assertThrows(DomainError.class,()->read(null,scope(),Map.of())).code());
    fixture.insert("mulino_identity_CapabilityAssignments",fixture.map("organizationId",fixture.org,"ID",fixture.id(),"actorId",parent,"capabilityId","evaluateEligibility","scopeKind","ORGANIZATION","scopeId",fixture.org,"validFrom",Timestamp.from(fixture.now.minusSeconds(1000)),"validUntil",Timestamp.from(fixture.now.plusSeconds(10000))));
    String parentGrant=jdbc.queryForObject("SELECT ID FROM mulino_identity_Grants WHERE actorId=?",String.class,parent);
    fixture.insert("mulino_identity_GrantActions",fixture.map("organizationId",fixture.org,"grantId",parentGrant,"capabilityId","evaluateEligibility"));
    assertEquals(1,segments(read(null,scope(),Map.of())).size());
    jdbc.update("UPDATE mulino_identity_Memberships SET "+boundary+"=? WHERE actorId=?",Timestamp.from(fixture.now),parent);
    assertEquals("FORBIDDEN",assertThrows(DomainError.class,()->read(null,scope(),Map.of())).code());
  }
  @Test void asOfKnownAtAndWorkLotFiltersUseExistingCandidateContract() {
    allow("0","40",fixture.customer);
    String future=extra(fixture.place,fixture.lot,fixture.now.plusSeconds(10),fixture.now.plusSeconds(5));
    assertEquals(List.of(fixture.segment),data(read(null,scope(),Map.of())).get("segmentIds"));
    var later=read(null,scope(),Map.of("customerId",fixture.customer),fixture.now.plusSeconds(5),fixture.now.plusSeconds(10));assertEquals(2,segments(later).size());equivalent(later);
    String otherLot=fixture.id();
    String maker=jdbc.queryForObject("SELECT manufacturerId FROM mulino_inventory_ManufacturingLots WHERE ID=?",String.class,fixture.lot);
    fixture.record("mulino_inventory_ManufacturingLots",otherLot,fixture.map("manufacturerId",maker,"itemId",fixture.item,"originalLot","OTHER","expiresAt",Timestamp.from(fixture.now.plusSeconds(100))));
    String unrelated=extra(fixture.place,otherLot);
    var work=scope();work.put("workId",fixture.work);
    assertEquals(List.of(fixture.segment),data(read(null,work,Map.of())).get("segmentIds"));
    var lotScope=scope();lotScope.put("lotId",otherLot);assertEquals(List.of(unrelated),data(read(null,lotScope,Map.of())).get("segmentIds"));
    work.put("lotId",fixture.lot);assertEquals(1,segments(read(null,work,Map.of())).size());
    work.put("workId",fixture.id());assertEquals("FORBIDDEN",assertThrows(DomainError.class,()->read(null,work,Map.of())).code());
    assertEquals("TYPE_INVALID",assertThrows(DomainError.class,()->read(null,scope(),Map.of("itemId",fixture.id()))).code());
  }
  @ParameterizedTest @CsvSource({"CONFLICT","UNAVAILABLE"})
  void conflictingOrUnavailableSourceDoesNotAuthorizeAnyRange(String state) throws Exception {
    allow("0","40",fixture.customer);
    String document=jdbc.queryForObject("SELECT evidenceRef FROM mulino_inventory_DispositionBases WHERE category='QC'",String.class);
    if(state.equals("UNAVAILABLE")) {
      String blob=jdbc.queryForObject("SELECT blobId FROM mulino_evidence_DocumentVersions WHERE ID=?",String.class,document);
      // Remove only this fixture's blob, so both public source checks must report unavailable.
      try(var paths=Files.walk(FulfillmentPostgresTest.BLOBS)) {
        var files=paths.filter(Files::isRegularFile).filter(path->path.toString().contains(blob)).toList();
        assertFalse(files.isEmpty());for(var file:files)Files.delete(file);
      }
    } else {
      var inbox=new LinkedHashMap<>(jdbc.queryForMap("SELECT i.* FROM mulino_evidence_InboxRecords i JOIN mulino_evidence_Claims c ON c.eventId=i.eventId WHERE c.documentVersionId=?",document));
      String old=(String)inbox.get("id");inbox.put("id",fixture.id());inbox.put("payloadhash","f".repeat(64));inbox.put("state","CONFLICT");inbox.put("conflictswithid",old);
      fixture.insert("mulino_evidence_InboxRecords",inbox);
    }
    String before=fingerprint();var result=read(null,scope(),Map.of("customerId",fixture.customer));equivalent(result);
    assertEquals("0",segments(result).getFirst().get("eligibleQuantity"));
    assertEquals("UNKNOWN",segments(result).getFirst().get("eligibilityStatus"));
    assertEquals(before,fingerprint());
  }
  @Test void missingUnknownSupersededAndExpiredSourcesStayNonPositive() {
    assertEquals("UNKNOWN",segments(read(null,scope(),Map.of())).getFirst().get("eligibilityStatus"));
    allow("0","40",fixture.customer);
    String doc=jdbc.queryForObject("SELECT evidenceRef FROM mulino_inventory_DispositionBases WHERE category='QC'",String.class);
    String replacement=fixture.id();
    var successor=new LinkedHashMap<>(jdbc.queryForMap("SELECT * FROM mulino_evidence_DocumentVersions WHERE ID=?",doc));
    fixture.now=fixture.now.plusSeconds(1);
    successor.put("id",replacement);successor.put("supersedesid",doc);
    successor.put("revision",((Number)successor.get("revision")).intValue()+1);successor.put("recordedat",Timestamp.from(fixture.now));
    fixture.insert("mulino_evidence_DocumentVersions",successor);
    // A successor is stored before reading; the query never fabricates evidence.
    var result=read(null,scope(),Map.of("customerId",fixture.customer));equivalent(result);
    assertEquals("0",segments(result).getFirst().get("eligibleQuantity"));
    fixture.now=fixture.now.plusSeconds(1001);
    result=read(null,scope(),Map.of("customerId",fixture.customer));equivalent(result);assertEquals("0",segments(result).getFirst().get("eligibleQuantity"));
  }
  @Test @SuppressWarnings("unchecked") void semanticConditionsKeepRealSourcesAndFefoExcludesReservedStock() {
    allow("0","40",fixture.customer);
    var response=read(null,scope(),Map.of("customerId",fixture.customer));var row=segments(response).getFirst();
    var conditions=(List<Map<String,Object>>)row.get("conditionResults");
    assertEquals(Set.of("QC","REGULATORY","LABEL","DISPOSITION","CUSTOMER_CONTRACT"),conditions.stream().map(c->c.get("conditionId")).collect(java.util.stream.Collectors.toSet()));
    assertEquals(conditions,data(response).get("conditionResults"));assertEquals(fixture.segment,data(response).get("preferredSegmentId"));
    assertEquals("40",row.get("unreservedEligibleQuantity"));
    for(var condition:conditions){assertEquals(fixture.segment,condition.get("segmentId"));assertEquals(com.mulino.application.quality.QualityEligibility.EVALUATOR_VERSION,condition.get("evaluatorVersion"));assertEquals("ALLOWED",condition.get("status"));assertFalse(((List<?>)condition.get("evidenceRefs")).isEmpty());}
    for(var category:Map.of("QC","QC","DISPOSITION","COMMERCIAL","CUSTOMER_CONTRACT","CUSTOMER").entrySet()){
      String doc=fixture.jdbc.queryForObject("SELECT evidenceRef FROM mulino_inventory_DispositionBases WHERE category=?",String.class,category.getValue());
      assertEquals(doc,conditions.stream().filter(c->category.getKey().equals(c.get("conditionId"))).findFirst().orElseThrow().get("evidenceId"));
    }
    var label=conditions.stream().filter(c->"LABEL".equals(c.get("conditionId"))).findFirst().orElseThrow();
    assertEquals(false,label.get("required"));assertEquals("NOT_REQUIRED_BY_POLICY",label.get("basis"));assertEquals(fixture.regPolicy,label.get("evidenceId"));
    var inventory=runtime.requestContext().run(ctx->{return queries.query(new QueryRequest("getInventory",null,scope(),Map.of("customerId",fixture.customer),200,null,null,fixture.now,fixture.now,null));});
    assertEquals(conditions,data(inventory).get("conditionResults"));
    fixture.reserve("0","40");var reserved=read(null,scope(),Map.of("customerId",fixture.customer));
    assertEquals("0",segments(reserved).getFirst().get("unreservedEligibleQuantity"));assertFalse(data(reserved).containsKey("preferredSegmentId"));
  }
  @Test @SuppressWarnings("unchecked") void qcPermissionAndCurrentHoldRemainIndependentAndBothSourcesAreNamed() {
    allow("0","100",fixture.customer);fixture.hold("100");
    var response=read(null,scope(),Map.of("customerId",fixture.customer));var row=segments(response).getFirst();
    var qc=((List<Map<String,Object>>)row.get("conditionResults")).stream().filter(c->"QC".equals(c.get("conditionId"))).findFirst().orElseThrow();
    assertEquals("ALLOWED",qc.get("permissionStatus"));assertEquals("DENIED",qc.get("restrictionStatus"));assertEquals("DENIED",qc.get("status"));assertEquals("0",row.get("eligibleQuantity"));
    var expected=new HashSet<>(fixture.jdbc.queryForList("SELECT evidenceRef FROM mulino_inventory_DispositionBases WHERE category='QC' UNION SELECT evidenceRef FROM mulino_inventory_Restrictions WHERE category='QC'",String.class));
    assertEquals(expected,new HashSet<>((List<String>)qc.get("evidenceRefs")));assertNull(qc.get("evidenceId"));
  }
}
