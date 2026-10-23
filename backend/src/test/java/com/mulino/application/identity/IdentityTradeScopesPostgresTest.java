package com.mulino.application.identity;

import com.mulino.application.core.*;
import com.mulino.application.trade.purchase.PurchaseCommands;
import com.mulino.domain.identity.*;
import com.sap.cds.services.runtime.CdsRuntime;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.junit.jupiter.api.Assertions.*;

/** Real CQN/PG identity commands; no scenario installer, model or policy substitution. */
@SpringBootTest
@ActiveProfiles("local")
class IdentityTradeScopesPostgresTest {
  static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280");
  static { PG.start(); }
  @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url",PG::getJdbcUrl);r.add("spring.datasource.username",PG::getUsername);r.add("spring.datasource.password",PG::getPassword);
    r.add("JWT_PUBLIC_KEY",()->System.getenv("JWT_PUBLIC_KEY"));r.add("JWT_ISSUER",()->"https://fixture.invalid");r.add("JWT_AUDIENCE",()->"mulino-acceptance");
  }
  @AfterAll static void stopOwnedContainer() { PG.stop(); }
  @Autowired JdbcTemplate jdbc;
  @Autowired CdsRuntime runtime;
  @Autowired PlatformTransactionManager transactions;
  @Autowired IdentityCommands commands;
  @Autowired IdentityAuthorization auth;
  @Autowired IdentityRepository repository;
  @Autowired PurchaseCommands purchase;
  @Autowired ApplicationQueries queries;
  @Autowired List<QueryHandler> queryHandlers;
  @Autowired List<ObjectReadProvider> objectProviders;
  final String org=id(),foreign=id(),human=id(),agent=id(),item=id(),place=id(),work=id(),supplier=id(),customer=id(),lot=id();
  Instant now;DomainContext parent,child;
  static String id() { return UUID.randomUUID().toString(); }
  <T> T tx(Supplier<T> action) { return runtime.requestContext().run(c->{return new TransactionTemplate(transactions).execute(s->action.get());}); }
  @BeforeEach void seed() {
    now=auth.now();parent=new DomainContext(org,human,human,now,now);child=new DomainContext(org,agent,agent,now,now);
    jdbc.update("INSERT INTO mulino_identity_Organizations(ID,externalAlias) VALUES(?,?),(?,?)",org,org,foreign,foreign);
    for(String actor:List.of(human,agent)) {
      jdbc.update("INSERT INTO mulino_identity_Actors(organizationId,ID,kind,stableRequestOwner) VALUES(?,?,?,?)",org,actor,actor.equals(human)?"HUMAN":"AGENT",actor);
      jdbc.update("INSERT INTO mulino_identity_Memberships(organizationId,ID,actorId,validFrom,validUntil) VALUES(?,?,?,?,?)",org,id(),actor,Timestamp.from(now.minusSeconds(100)),Timestamp.from(now.plusSeconds(3600)));
      jdbc.update("INSERT INTO mulino_identity_AuthorityFences VALUES(?,?,1)",org,actor);
    }
    String product=id(),manufacturer=id(),definition=id(),specification=id(),packaging=id();
    jdbc.update("INSERT INTO mulino_inventory_Products(organizationId,ID,name) VALUES(?,?,'product')",org,product);
    jdbc.update("INSERT INTO mulino_inventory_SpecificationVersions(organizationId,ID,productId,version,contentHash) VALUES(?,?,?,'v1',?)",org,specification,product,"0".repeat(64));
    jdbc.update("INSERT INTO mulino_inventory_PackagingVersions(organizationId,ID,productId,version,contentHash) VALUES(?,?,?,'v1',?)",org,packaging,product,"0".repeat(64));
    jdbc.update("INSERT INTO mulino_inventory_TradeItems(organizationId,ID,productId,name,baseUnit,decimalPlaces,specificationVersionId,packagingVersionId) VALUES(?,?,?,'item','BOX',0,?,?)",org,item,product,specification,packaging);
    jdbc.update("INSERT INTO mulino_inventory_Manufacturers(organizationId,ID,name) VALUES(?,?,'manufacturer')",org,manufacturer);
    jdbc.update("INSERT INTO mulino_inventory_ManufacturingLots(organizationId,ID,manufacturerId,itemId,originalLot) VALUES(?,?,?,?,'lot')",org,lot,manufacturer,item);
    jdbc.update("INSERT INTO mulino_inventory_Places(organizationId,ID,name,kind) VALUES(?,?,'warehouse','INTERNAL_STORAGE')",org,place);
    jdbc.update("INSERT INTO mulino_trade_purchase_Suppliers(organizationId,ID,name) VALUES(?,?,'supplier')",org,supplier);
    jdbc.update("INSERT INTO mulino_trade_sales_Customers(organizationId,ID,revision,createdAt,recordedAt,effectiveAt,name) VALUES(?,?,1,?,?,?,'customer')",org,customer,Timestamp.from(now),Timestamp.from(now),Timestamp.from(now));
    jdbc.update("INSERT INTO mulino_definitions_DefinitionVersions(organizationId,ID,createdAt,version,state,contentHash,content,evaluatorVersion,schemaVersion) VALUES(?,?,?,'scope-test','PUBLISHED',?,'{}','core-v1','1.0.0')",org,definition,Timestamp.from(now),"0".repeat(64));
    jdbc.update("INSERT INTO mulino_work_read_Works(organizationId,ID,createdAt,recordedAt,effectiveAt,itemId,definitionVersionId,kind,status,ownerId,supervisorId,lifecycleMode) VALUES(?,?,?,?,?,?,?,'PURCHASE','ACTIVE',?,?,'COMMAND')",org,work,Timestamp.from(now),Timestamp.from(now),Timestamp.from(now),item,definition,human,human);
    bootstrap("createGrant",Map.of("ORGANIZATION",List.of(org)));
  }
  String bootstrap(String cap,Map<String,List<String>> scopes) {
    String grant=id();jdbc.update("INSERT INTO mulino_identity_Grants(organizationId,ID,actorId,delegatorId,validFrom,validUntil) VALUES(?,?,?,?,?,?)",org,grant,human,human,Timestamp.from(now.minusSeconds(100)),Timestamp.from(now.plusSeconds(3600)));
    jdbc.update("INSERT INTO mulino_identity_GrantActions VALUES(?,?,?)",org,grant,cap);
    scopes.forEach((kind,ids)->ids.forEach(scope->jdbc.update("INSERT INTO mulino_identity_GrantScopes VALUES(?,?,?,?)",org,grant,kind,scope)));
    var first=scopes.entrySet().iterator().next();assignment(human,cap,first.getKey(),first.getValue().getFirst());return grant;
  }
  void assignment(String actor,String cap,String kind,String scope) {
    jdbc.update("INSERT INTO mulino_identity_CapabilityAssignments(organizationId,ID,actorId,capabilityId,scopeKind,scopeId,validFrom,validUntil) VALUES(?,?,?,?,?,?,?,?)",org,id(),actor,cap,kind,scope,Timestamp.from(now.minusSeconds(100)),Timestamp.from(now.plusSeconds(3600)));
  }
  Map<String,Object> grant(String capability,Map<String,List<String>> scopes) {
    var rows=new ArrayList<Map<String,String>>();scopes.forEach((kind,ids)->ids.forEach(scope->rows.add(Map.of("scopeKind",kind,"scopeId",scope))));
    return Map.of("capabilityId","createGrant","slots",Map.of("actorId",agent,"actions",List.of(capability),"scopes",rows,"validFrom",now.toString(),"validUntil",now.plusSeconds(60).toString()));
  }
  List<Integer> effects() {
    return List.of("mulino_identity_Grants","mulino_identity_GrantScopes","mulino_identity_CapabilityAssignments","mulino_identity_AuthorityInvalidations","mulino_trade_purchase_Proposals","mulino_inventory_QuantityMovements","mulino_runtime_Outbox")
        .stream().map(t->jdbc.queryForObject("SELECT count(*) FROM "+t+" WHERE organizationId=?",Integer.class,org)).toList();
  }
  @Test void realPurchaseScopesPersistAndStillRequireAllFourDimensionsAndCurrentParent() {
    var prep=tx(()->purchase.prepare(parent,Map.of("capabilityId","proposePurchase","slots",Map.of("workId",work,"destinationId",place,"supplierId",supplier))));
    assertEquals(Map.of("ITEM",List.of(item),"PLACE",List.of(place),"WORK",List.of(work),"SUPPLIER",List.of(supplier)),prep.scopes());
    String parentGrant=bootstrap("proposePurchase",prep.scopes());assignment(agent,"proposePurchase","SUPPLIER",supplier);
    var result=tx(()->commands.execute(parent,grant("proposePurchase",prep.scopes())));assertEquals("APPLIED",result.get("outcome"));
    assertTrue(tx(()->auth.permittedScopes(child,"proposePurchase",prep.scopes())));
    for(String dimension:prep.scopes().keySet()) {
      var missing=new HashMap<>(prep.scopes());missing.remove(dimension);assertFalse(tx(()->auth.permittedScopes(child,"proposePurchase",missing)),dimension);
      var wrong=new HashMap<>(prep.scopes());wrong.put(dimension,List.of(id()));assertFalse(tx(()->auth.permittedScopes(child,"proposePurchase",wrong)),dimension);
    }
    var before=effects();
    jdbc.update("UPDATE mulino_identity_Grants SET revokedAt=?,revision=revision+1 WHERE organizationId=? AND ID=?",Timestamp.from(auth.now()),org,parentGrant);
    assertFalse(tx(()->auth.permittedScopes(child,"proposePurchase",prep.scopes())));
    assertThrows(AccessDeniedException.class,()->tx(()->commands.execute(parent,grant("proposePurchase",prep.scopes()))));
    assertEquals(before,effects());
  }
  @Test void eachNewTypedDimensionCanBeAssignedDelegatedAndBoundToItsActualNoun() {
    bootstrap("assignCapability",Map.of("ORGANIZATION",List.of(org)));
    for(var entry:Map.of("SUPPLIER",supplier,"CUSTOMER",customer,"LOT",lot).entrySet()) {
      String kind=entry.getKey(),scope=entry.getValue(),cap="scope-test-"+kind;
      bootstrap(cap,Map.of(kind,List.of(scope)));
      jdbc.update("INSERT INTO mulino_identity_ManagementAuthorities(organizationId,ID,actorId,capabilityId,scopeKind,scopeId,validFrom,validUntil) VALUES(?,?,?,?,?,?,?,?)",org,id(),human,cap,kind,scope,Timestamp.from(now.minusSeconds(100)),Timestamp.from(now.plusSeconds(3600)));
      var assign=Map.<String,Object>of("capabilityId","assignCapability","slots",Map.of("actorId",agent,"assignedCapability",cap,"scopeKind",kind,"scopeId",scope,"validFrom",now.toString(),"validUntil",now.plusSeconds(60).toString()));
      assertEquals("APPLIED",tx(()->commands.execute(parent,assign)).get("outcome"));
      var request=new HashMap<>(grant(cap,Map.of(kind,List.of(scope))));String noun=ScopeDimension.valueOf(kind).nounType();request.put("subjectRefs",List.of(Map.of("type",noun,"id",scope)));
      var prep=tx(()->commands.prepare(parent,request));var bindings=tx(()->commands.subjectBindings(parent,request,prep));
      assertEquals(List.of(SubjectBinding.optional(noun,Set.of(scope))),bindings);
      assertEquals("APPLIED",tx(()->commands.execute(parent,request)).get("outcome"));
      assertTrue(tx(()->auth.permittedScopes(child,cap,Map.of(kind,List.of(scope)))));
      assertFalse(tx(()->auth.permittedScopes(child,cap,Map.of("TARGET",List.of(scope)))));
    }
  }
  @Test void malformedUnknownForeignAndWrongTypeTargetsAreRejectedBeforeAnyIdentityEffect() {
    String foreignSupplier=id();jdbc.update("INSERT INTO mulino_trade_purchase_Suppliers(organizationId,ID,name) VALUES(?,?,'foreign')",foreign,foreignSupplier);
    for(var scope:List.of(Map.of("UNREGISTERED",List.of(supplier)),Map.of("SUPPLIER",List.of("not-a-uuid")),Map.of("SUPPLIER",List.of(customer)),Map.of("CUSTOMER",List.of(supplier)),Map.of("LOT",List.of(supplier)),Map.of("SUPPLIER",List.of(id())),Map.of("SUPPLIER",List.of(foreignSupplier)),Map.of("ORGANIZATION",List.of(foreign)))) {
      String expected=scope.containsKey("UNREGISTERED")||scope.values().iterator().next().contains("not-a-uuid")?"TYPE_INVALID":"FORBIDDEN";
      var before=effects();assertEquals(expected,assertThrows(DomainError.class,()->tx(()->commands.prepare(parent,grant("proposePurchase",scope)))).code());
      assertEquals(expected,assertThrows(DomainError.class,()->tx(()->commands.execute(parent,grant("proposePurchase",scope)))).code());assertEquals(before,effects());
      var entry=scope.entrySet().iterator().next();var assign=Map.<String,Object>of("capabilityId","assignCapability","slots",Map.of("actorId",agent,"assignedCapability","proposePurchase","scopeKind",entry.getKey(),"scopeId",entry.getValue().getFirst(),"validFrom",now.toString(),"validUntil",now.plusSeconds(60).toString()));
      assertEquals(expected,assertThrows(DomainError.class,()->tx(()->commands.prepare(parent,assign))).code());
      assertEquals(expected,assertThrows(DomainError.class,()->tx(()->commands.execute(parent,assign))).code());assertEquals(before,effects());
    }
  }
  @Test void newDimensionCannotCreateAuthorityPreviouslyDeniedByParentAllOf() {
    bootstrap("proposePurchase",Map.of("ITEM",List.of(item),"WORK",List.of(work),"PLACE",List.of(place),"SUPPLIER",List.of(supplier)));
    for(var scope:List.of(Map.of("SUPPLIER",List.of(supplier)),Map.of("ITEM",List.of(item),"PLACE",List.of(place),"SUPPLIER",List.of(supplier)))) {
      var before=effects();assertThrows(AccessDeniedException.class,()->tx(()->commands.execute(parent,grant("proposePurchase",scope))));assertEquals(before,effects());
    }
    assertFalse(tx(()->auth.permittedScopes(child,"proposePurchase",Map.of("SUPPLIER",List.of(supplier)))));
  }
  @Test void sourceNamespaceSurvivesCqnUnchangedWithoutBecomingTypedUuidOrOrganizationAuthority() {
    for(String source:List.of("supplier/receipt:원문-v1","😀".repeat(36))) {
      bootstrap("source-test",Map.of("SOURCE",List.of(source)));assignment(agent,"source-test","SOURCE",source);
      var result=tx(()->commands.execute(parent,grant("source-test",Map.of("SOURCE",List.of(source)))));
      assertTrue(tx(()->repository.rows("GrantScopes",org).stream().anyMatch(r->result.get("id").equals(r.get("grantId"))&&source.equals(r.get("scopeId")))));
      assertTrue(tx(()->auth.permittedScopes(child,"source-test",Map.of("SOURCE",List.of(source)))));
      assertFalse(tx(()->auth.permittedScopes(child,"source-test",Map.of("SOURCE",List.of(id())))));
    }
    var tooLong=assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
        ()->assignment(agent,"source-test","SOURCE","😀".repeat(37)));
    assertEquals("23514",((SQLException)tooLong.getMostSpecificCause()).getSQLState());
  }
  @Test void captureActualQueryRegistrationWithoutClaimingMissingCatalogEntriesAreAccepted() throws Exception {
    var registered=new TreeSet<String>();var handlers=new TreeMap<String,Object>();var nouns=new TreeMap<String,Object>();
    for(var handler:queryHandlers){registered.addAll(handler.operations());handlers.put(AopUtils.getTargetClass(handler).getName(),new TreeSet<>(handler.operations()));}
    assertEquals(queries.operations(),registered);
    for(var provider:objectProviders)nouns.put(AopUtils.getTargetClass(provider).getName(),new TreeSet<>(provider.objectTypes()));
    String root=System.getProperty("identity.scope.inventory");
    if(root!=null){var path=java.nio.file.Path.of(root);java.nio.file.Files.createDirectories(path.getParent());
      new com.fasterxml.jackson.databind.ObjectMapper().writeValue(path.toFile(),Map.of("registeredQueryIds",registered,"queryHandlers",handlers,"objectReadProviders",nouns));}
    System.out.println("LIVE_QUERY_REGISTRATION "+registered);
    System.out.println("LIVE_OBJECT_READ_PROVIDERS "+nouns);
  }
  @Test void populatedV44UpgradePreservesOldRowsAndClosesOnlyKnownDimensionGap() throws Exception {
    String database="scope_upgrade_"+id().replace("-","");jdbc.execute("CREATE DATABASE "+database);
    String url=PG.getJdbcUrl().substring(0,PG.getJdbcUrl().lastIndexOf('/')+1)+database;
    try {
      var before=Flyway.configure().dataSource(url,PG.getUsername(),PG.getPassword()).locations("classpath:db/migration").target("44").load();before.migrate();
      try(Connection db=DriverManager.getConnection(url,PG.getUsername(),PG.getPassword());Statement sql=db.createStatement()) {
        sql.execute("INSERT INTO mulino_identity_Organizations(ID,externalAlias) VALUES('"+org+"','upgrade')");
        sql.execute("INSERT INTO mulino_identity_Actors(organizationId,ID,kind,stableRequestOwner) VALUES('"+org+"','"+human+"','HUMAN','"+human+"')");
        sql.execute("INSERT INTO mulino_identity_Grants(organizationId,ID,actorId,delegatorId,validFrom,validUntil) VALUES('"+org+"','"+work+"','"+human+"','"+human+"',now(),now()+interval '1 day')");
        sql.execute("INSERT INTO mulino_identity_GrantScopes VALUES('"+org+"','"+work+"','ITEM','"+item+"')");
        sql.execute("INSERT INTO mulino_identity_GrantScopes VALUES('"+org+"','"+work+"','SOURCE','original/source-v1')");
        for(String kind:List.of("SUPPLIER","CUSTOMER","LOT"))for(String query:scopeInserts(kind))assertEquals("23514",assertThrows(SQLException.class,()->sql.execute(query)).getSQLState());
        System.out.println("V44: GrantScopes and CapabilityAssignments reject SUPPLIER/CUSTOMER/LOT with SQLSTATE 23514 (six observed denials)");
        Flyway.configure().dataSource(url,PG.getUsername(),PG.getPassword()).locations("classpath:db/migration").load().migrate();
        for(String kind:List.of("SUPPLIER","CUSTOMER","LOT"))for(String query:scopeInserts(kind))assertEquals(1,sql.executeUpdate(query));
        for(String query:scopeInserts("UNREGISTERED"))assertEquals("23514",assertThrows(SQLException.class,()->sql.execute(query)).getSQLState());
        System.out.println("V45: six typed inserts accepted; both UNREGISTERED inserts still reject with SQLSTATE 23514");
        try(var old=sql.executeQuery("SELECT scopeId FROM mulino_identity_GrantScopes WHERE scopeKind='ITEM'")) { assertTrue(old.next());assertEquals(item,old.getString(1));assertFalse(old.next()); }
        try(var old=sql.executeQuery("SELECT scopeId FROM mulino_identity_GrantScopes WHERE scopeKind='SOURCE'")) { assertTrue(old.next());assertEquals("original/source-v1",old.getString(1));assertFalse(old.next()); }
      }
    } finally { jdbc.execute("DROP DATABASE "+database); }
  }
  List<String> scopeInserts(String kind) {
    return List.of("INSERT INTO mulino_identity_GrantScopes VALUES('"+org+"','"+work+"','"+kind+"','"+supplier+"')",
        "INSERT INTO mulino_identity_CapabilityAssignments(organizationId,ID,actorId,capabilityId,scopeKind,scopeId,validFrom,validUntil) VALUES('"+org+"','"+id()+"','"+human+"','test','"+kind+"','"+supplier+"',now(),now()+interval '1 day')");
  }
}
