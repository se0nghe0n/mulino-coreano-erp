package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.mulino.verification.Json;

/** Disposable test-only typed seed, atomic, with no HTTP fixture surface. */
public final class FixtureInstaller {
    private static final Set<String> TYPES=Set.of("Organization","Human","Agent","Product","SpecificationVersion","PackagingVersion","TradeItem","Manufacturer","ManufacturerLot","Place","QuantitySegment","DefinitionVersion","PolicyVersion","Supplier","Customer","RegulatoryPolicy","ManagementAuthority");
    private final ActualConfiguration configuration;
    public FixtureInstaller(ActualConfiguration configuration){this.configuration=configuration;}
    public ObjectNode install(JsonNode bundle) throws Exception {
        if(Set.of("S3_ORIGINAL","S4_ORIGINAL").contains(bundle.path("fixturePhase").asText()))return S3OriginalFixtureInstaller.install(configuration,bundle);
        if(bundle.path("fixturePhase").asText().equals("EVIDENCE"))return EvidenceFixtureInstaller.install(configuration,bundle);
        JsonNode fixture=bundle.path("fixture");
        if(!fixture.path("synthetic").asBoolean(false))throw new IllegalArgumentException("Only synthetic fixtures allowed");
        if(!bundle.path("bases").isEmpty()||!fixture.path("baseRefs").isEmpty())throw new UnsupportedOperationException("Fixture inheritance not installed in S1");
        if(fixture.path("fixtureId").asText().startsWith("S4-")||fixture.path("fixtureId").asText().startsWith("S3-"))for(JsonNode alias:fixture.path("aliases"))if(alias.path("type").asText().equals("QuantitySegment")) {
            if(!fixture.path("preexistingTransitBaseline").asBoolean()||!"60".equals(alias.path("quantity").asText())||!"TRANSIT".equals(fixture.path("aliases").path(alias.path("locationAlias").asText()).path("kind").asText()))throw new IllegalArgumentException("S3 final physical quantity must be created through public product commands");
        }
        for(JsonNode alias:fixture.path("aliases")) if(!TYPES.contains(alias.path("type").asText()))throw new UnsupportedOperationException("S1 fixture alias type "+alias.path("type").asText());
        if(!fixture.path("baseline").isEmpty()||!fixture.path("evidence").isEmpty()||!fixture.path("responsibilities").isEmpty())
            throw new UnsupportedOperationException("S1 installer cannot silently omit baseline/evidence/responsibility facts");
        ObjectNode aliases=Json.object();fixture.path("aliases").fieldNames().forEachRemaining(a->aliases.put(a,UUID.randomUUID().toString()));
        String orgAlias=null;
        for(var it=fixture.path("aliases").fields();it.hasNext();) {var e=it.next();if(e.getValue().path("type").asText().equals("Organization")){if(orgAlias!=null)throw new UnsupportedOperationException("Multi-organization fixtures pending");orgAlias=e.getKey();}}
        if(orgAlias==null)throw new IllegalArgumentException("Organization alias required");
        String org=aliases.path(orgAlias).asText();
        // A runner that installs several fixtures into one disposable DB names each
        // organization's external alias; the authored alias key stays the contract.
        String externalAlias=bundle.path("organizationExternalAlias").asText(orgAlias);
        if(externalAlias.isBlank()||externalAlias.length()>80)throw new IllegalArgumentException("Organization external alias must be 1..80 characters");
        try(Connection c=DriverManager.getConnection(configuration.jdbcUrl(),configuration.username(),configuration.password())) {
            c.setAutoCommit(false);
            try {
                seedTemporal(c,fixture,"INSERT INTO mulino_identity_Organizations(ID,externalAlias) VALUES(?,?)",org,externalAlias);
                for(String type:List.of("Human","Agent","Product","Manufacturer","Place","SpecificationVersion","PackagingVersion","TradeItem","ManufacturerLot","QuantitySegment","DefinitionVersion","PolicyVersion","Supplier","Customer","RegulatoryPolicy","ManagementAuthority"))
                    for(var it=fixture.path("aliases").fields();it.hasNext();) {var entry=it.next();JsonNode a=entry.getValue();if(!type.equals(a.path("type").asText()))continue;
                        String id=aliases.path(entry.getKey()).asText();String name=a.path("name").asText(entry.getKey());
                        switch(type) {
                            case "Human","Agent" -> {seedTemporal(c,fixture,"INSERT INTO mulino_identity_Actors(organizationId,ID,kind,stableRequestOwner) VALUES(?,?,?,?)",org,id,type.equals("Human")?"HUMAN":"AGENT",UUID.randomUUID().toString());seed(c,"INSERT INTO mulino_identity_AuthorityFences(organizationId,actorId) VALUES(?,?)",org,id);}
                            case "Manufacturer" -> seedTemporal(c,fixture,"INSERT INTO mulino_inventory_Manufacturers(organizationId,ID,name) VALUES(?,?,?)",org,id,name);
                            case "Place" -> seedTemporal(c,fixture,"INSERT INTO mulino_inventory_Places(organizationId,ID,name,kind) VALUES(?,?,?,?)",org,id,name,a.path("kind").asText("WAREHOUSE"));
                            case "Product" -> seedTemporal(c,fixture,"INSERT INTO mulino_inventory_Products(organizationId,ID,name) VALUES(?,?,?)",org,id,name);
                            case "SpecificationVersion","PackagingVersion" -> {
                                JsonNode content=a.path("content");if(!content.isObject()||content.isEmpty())throw new IllegalArgumentException("Versioned fixture content required");
                                String hash=contentHash(content);
                                String sql=type.equals("SpecificationVersion")?"INSERT INTO mulino_inventory_SpecificationVersions(organizationId,ID,productId,version,contentHash,description) VALUES(?,?,?,?,?,?)":"INSERT INTO mulino_inventory_PackagingVersions(organizationId,ID,productId,version,contentHash,description) VALUES(?,?,?,?,?,?)";
                                seedTemporal(c,fixture,sql,org,id,ref(aliases,a,"productAlias"),Json.required(a,"version"),hash,content.toString());
                            }
                            case "DefinitionVersion" -> {
                                JsonNode template=a.path("content");
                                if(!template.isObject()||template.isEmpty())throw new IllegalArgumentException("Complete authored definition content required");
                                ObjectNode content=template.deepCopy();content.put("organizationId",org).put("id",id);
                                if(!"PUBLISHED".equals(content.path("state").asText()))throw new IllegalArgumentException("Published fixture definition required");
                                seed(c,"INSERT INTO mulino_definitions_DefinitionVersions(organizationId,ID,version,state,contentHash,content,evaluatorVersion,schemaVersion,createdAt) VALUES(?,?,?,?,?,?,?,?,?)",org,id,Json.required(content,"version"),"PUBLISHED",contentHash(content),content.toString(),Json.required(content,"evaluatorVersion"),Json.required(content,"schemaVersion"),time(fixture.path("clock").path("asOf").asText()));
                            }
                            case "PolicyVersion" -> {
                                JsonNode content=a.path("content");if(!content.isObject())throw new IllegalArgumentException("Authored policy object required");
                                seed(c,"INSERT INTO mulino_governance_PolicyVersions(organizationId,ID,version,kind,content,contentHash,effectiveFrom,effectiveUntil,createdAt) VALUES(?,?,?,?,?,?,?,?,?)",org,id,Json.required(a,"version"),Json.required(a,"kind"),content.toString(),contentHash(content),time(Json.required(a,"effectiveFrom")),a.hasNonNull("effectiveUntil")?time(a.path("effectiveUntil").asText()):null,time(fixture.path("clock").path("asOf").asText()));
                                if(a.path("activate").asBoolean())seed(c,"INSERT INTO mulino_governance_ActivePolicies(organizationId,kind,policyId,revision) VALUES(?,?,?,?)",org,Json.required(a,"kind"),id,0);
                            }
                            case "TradeItem" -> seedTemporal(c,fixture,"INSERT INTO mulino_inventory_TradeItems(organizationId,ID,productId,name,baseUnit,decimalPlaces,specificationVersionId,packagingVersionId) VALUES(?,?,?,?,?,?,?,?)",org,id,ref(aliases,a,"productAlias"),name,Json.required(a,"unit"),0,ref(aliases,a,"specificationVersionAlias"),ref(aliases,a,"packagingVersionAlias"));
                            case "ManufacturerLot" -> seedTemporal(c,fixture,"INSERT INTO mulino_inventory_ManufacturingLots(organizationId,ID,manufacturerId,itemId,originalLot,expiresAt) VALUES(?,?,?,?,?,?)",org,id,ref(aliases,a,"manufacturerAlias"),ref(aliases,a,"itemAlias"),entry.getKey(),a.hasNonNull("expiresAt")?time(a.path("expiresAt").asText()):null);
                            case "Customer" -> seedTemporal(c,fixture,"INSERT INTO mulino_trade_sales_Customers(organizationId,ID,name,revision,effectiveAt) VALUES(?,?,?,?,?)",org,id,name,1,time(fixture.path("clock").path("asOf").asText()));
                            case "Supplier" -> seed(c,"INSERT INTO mulino_trade_purchase_Suppliers(organizationId,ID,name) VALUES(?,?,?)",org,id,name);
                            case "ManagementAuthority" -> seed(c,"INSERT INTO mulino_identity_ManagementAuthorities(organizationId,ID,actorId,capabilityId,scopeKind,scopeId,validFrom,validUntil,revision) VALUES(?,?,?,?,?,?,?,?,?)",org,id,ref(aliases,a,"actorAlias"),Json.required(a,"capabilityId"),"ORGANIZATION",org,time(Json.required(a,"validFrom")),time(Json.required(a,"validUntil")),1);
                            case "RegulatoryPolicy" -> seed(c,"INSERT INTO mulino_trade_regulatory_Policies(organizationId,ID,revision,createdAt,recordedAt,recordedBy,version,authority,sourceNamespace,action,validFrom,validUntil,fictional,status,requiresLabel) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",org,id,1,time(fixture.path("clock").path("asOf").asText()),time(fixture.path("clock").path("knownAt").asText()),ref(aliases,a,"actorAlias"),Json.required(a,"version"),Json.required(a,"authority"),Json.required(a,"sourceNamespace"),Json.required(a,"action"),time(Json.required(a,"validFrom")),time(Json.required(a,"validUntil")),true,Json.required(a,"status"),a.path("requiresLabel").asBoolean());
                            case "QuantitySegment" -> {
                                seed(c,"INSERT INTO mulino_inventory_QuantitySegments(organizationId,ID,itemId,lotId,identificationStatus,quantity,unit,placeId,controlScope,validFrom,mixtureStatus,createdAt,recordedAt,ownerId,custodianId,evidenceRef) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",org,id,ref(aliases,a,"itemAlias"),ref(aliases,a,"lotAlias"),"CONFIRMED",new java.math.BigDecimal(Json.required(a,"quantity")),Json.required(a,"unit"),ref(aliases,a,"locationAlias"),Json.required(a,"physicalScope"),time(fixture.path("clock").path("asOf").asText()),"IDENTIFIED",time(fixture.path("clock").path("asOf").asText()),time(fixture.path("clock").path("knownAt").asText()),a.hasNonNull("ownerAlias")?ref(aliases,a,"ownerAlias"):null,a.hasNonNull("custodianAlias")?ref(aliases,a,"custodianAlias"):null,fixture.path("preexistingTransitBaseline").asBoolean()?"authored-input:"+Json.required(bundle,"fixtureHash"):null);
                                if(fixture.path("preexistingTransitBaseline").asBoolean())seed(c,"INSERT INTO mulino_inventory_QuantityMovements(organizationId,ID,revision,targetId,quantity,unit,kind,occurredAt,evidenceRef,createdAt,recordedAt) VALUES(?,?,?,?,?,?,?,?,?,?,?)",org,UUID.randomUUID().toString(),1,id,new java.math.BigDecimal(Json.required(a,"quantity")),Json.required(a,"unit"),"INITIAL_BALANCE",time(fixture.path("clock").path("asOf").asText()),"authored-input:"+Json.required(bundle,"fixtureHash"),time(fixture.path("clock").path("asOf").asText()),time(fixture.path("clock").path("knownAt").asText()));
                            }
                            default -> throw new IllegalStateException(type);
                        }
                    }
                for(var it=fixture.path("actors").fields();it.hasNext();) {var entry=it.next();JsonNode a=entry.getValue();String actor=aliases.path(entry.getKey()).asText();
                    if(actor.isBlank())throw new IllegalArgumentException("Actor alias absent");
                    if(!orgAlias.equals(Json.required(a,"organizationAlias")))throw new UnsupportedOperationException("Actor organization differs");
                    boolean untrusted=a.path(ActualFixtureBindings.UNTRUSTED).asBoolean(false);
                    if(!untrusted&&(!configuration.issuer().equals(Json.required(a,"issuer"))||!configuration.audience().equals(Json.required(a,"audience"))))throw new IllegalArgumentException("Fixture identity issuer/audience differs from backend");
                    var grant=a.path("grant");String delegator=ref(aliases,grant,"delegatorAlias"),grantId=UUID.randomUUID().toString();
                    var from=time(Json.required(grant,"validFrom"));var until=time(Json.required(grant,"validUntil"));
                    // An untrusted authored identity is bound to its own issuer; the backend never resolves it through the deployed issuer.
                    seedTemporal(c,fixture,"INSERT INTO mulino_identity_ExternalIdentities(organizationId,ID,actorId,issuer,subject,organizationAlias) VALUES(?,?,?,?,?,?)",org,UUID.randomUUID().toString(),actor,untrusted?Json.required(a,"issuer"):configuration.issuer(),Json.required(a,"subject"),externalAlias);
                    seedTemporal(c,fixture,"INSERT INTO mulino_identity_Memberships(organizationId,ID,actorId,validFrom,validUntil) VALUES(?,?,?,?,?)",org,UUID.randomUUID().toString(),actor,from,until);
                    seedTemporal(c,fixture,"INSERT INTO mulino_identity_Grants(organizationId,ID,actorId,delegatorId,validFrom,validUntil) VALUES(?,?,?,?,?,?)",org,grantId,actor,delegator,from,until);
                    // Only an explicit organization scope is supported; never broaden item/work-restricted grants.
                    if(grant.path("scope").size()!=1||!orgAlias.equals(grant.path("scope").path("organizationAlias").asText()))throw new UnsupportedOperationException("Dimension-restricted grant fixture mapping pending");
                    seed(c,"INSERT INTO mulino_identity_GrantScopes(organizationId,grantId,scopeKind,scopeId) VALUES(?,?,?,?)",org,grantId,"ORGANIZATION",org);
                    for(JsonNode action:grant.path("actions"))seed(c,"INSERT INTO mulino_identity_GrantActions(organizationId,grantId,capabilityId) VALUES(?,?,?)",org,grantId,action.asText());
                    for(JsonNode action:a.path("roleCapabilities"))seedTemporal(c,fixture,"INSERT INTO mulino_identity_CapabilityAssignments(organizationId,ID,actorId,capabilityId,scopeKind,scopeId,validFrom,validUntil) VALUES(?,?,?,?,?,?,?,?)",org,UUID.randomUUID().toString(),actor,action.asText(),"ORGANIZATION",org,from,until);
                }
                c.commit();
            } catch(Exception failure){c.rollback();throw failure;}
        }
        var result=Json.object();result.set("aliasMap",aliases);result.put("fixtureHash",Json.required(bundle,"fixtureHash"));result.set("clock",fixture.path("clock"));if(bundle.hasNonNull("identityBindingHash"))result.set("identityBindingHash",bundle.path("identityBindingHash"));result.put("organizationExternalAlias",externalAlias);result.put("committed",true).put("businessExecutionClaimed",false);return result;
    }
    static String contentHash(JsonNode content) throws java.security.NoSuchAlgorithmException {
        return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(content.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
    private static String ref(JsonNode aliases,JsonNode object,String field) {String alias=Json.required(object,field);String id=aliases.path(alias).asText();if(id.isBlank())throw new IllegalArgumentException("Unknown fixture alias "+alias);return id;}
    private static OffsetDateTime time(String value){return OffsetDateTime.parse(value);}
    private static void seedTemporal(Connection c,JsonNode fixture,String sql,Object... parameters) throws SQLException {
        String temporal=sql.replace(") VALUES(",",createdAt,recordedAt) VALUES(");
        temporal=temporal.substring(0,temporal.length()-1)+",?,?)";
        Object[] all=Arrays.copyOf(parameters,parameters.length+2);all[parameters.length]=time(fixture.path("clock").path("asOf").asText());all[parameters.length+1]=time(fixture.path("clock").path("knownAt").asText());seed(c,temporal,all);
    }
    private static void seed(Connection c,String sql,Object... parameters) throws SQLException {try(var statement=c.prepareStatement(sql)){for(int i=0;i<parameters.length;i++)statement.setObject(i+1,parameters[i]);if(statement.executeUpdate()!=1)throw new SQLException("Fixture insert did not create exactly one row");}}
}
