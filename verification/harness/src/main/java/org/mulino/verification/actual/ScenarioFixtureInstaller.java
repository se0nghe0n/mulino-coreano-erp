package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.mulino.verification.Json;

/**
 * Scenario-suite fixture installer (./verify scenarios --actual). One atomic JDBC transaction per installed fixture into
 * the disposable database; every authored organization becomes a fresh organization with its own external alias.
 *
 * Contract (contracts/execution-preconditions): the product clock starts at the fixture clock asOf, so every installed
 * row is recorded at that instant (recordedAt <= starting clock). Facts the installer cannot map yet are listed in
 * {@code omittedFacts}; strict mode refuses the fixture (NOT_IMPLEMENTED) instead of installing a partial world,
 * partial mode (verification.actual.partialFixtures=true) installs the mappable subset and reports what it left out.
 */
final class ScenarioFixtureInstaller {
    static final Set<String> SUPPORTED_TYPES=Set.of("Organization","Human","Agent","TradeItem","Product","Manufacturer","ManufacturerLot","ManufacturingLot",
        "Place","Customer","Supplier","QuantitySegment","Work","DocumentVersion","DefinitionVersion","PolicyVersion");
    /** Baseline keys that are descriptive only (no product row is implied). */
    static final Set<String> DESCRIPTIVE_BASELINE=Set.of("setupIsExecutionCoverage","identitySources","traceabilitySeed","policy","fixtureClassification","installationMode",
        "rolesAreDescriptiveNotImplicitAuthority","sentinelIsSyntheticNotCredential","approvalNote","operationUnderTest","syntheticPolicyBasis");
    static final Set<String> GRANT_METADATA=Set.of("organizationAlias","caseId","subcaseId","caseNamespace","createdWorkNamespace","environmentId","capabilityIds");
    private final ActualConfiguration config;
    private final Path root;
    private final JsonNode bundle;
    private final ObjectNode fixture;
    private final boolean partial;
    private final boolean unionGrantDimensions=Boolean.getBoolean("verification.actual.relaxWire");
    private final ObjectNode aliases=Json.object();
    private final Map<String,String> orgOf=new HashMap<>();
    private final Map<String,String> externals=new LinkedHashMap<>();
    private final List<String> omitted=new ArrayList<>();
    private final List<String> conventions=new ArrayList<>();
    private OffsetDateTime asOf;
    private String defaultOrgAlias;
    private Connection c;
    private final Set<String> ensured=new HashSet<>();

    private ScenarioFixtureInstaller(ActualConfiguration config,Path root,JsonNode bundle,boolean partial) throws Exception {
        this.config=config;this.root=root;this.bundle=bundle;this.partial=partial;this.fixture=merged(bundle);
    }
    static ObjectNode install(ActualConfiguration config,Path root,JsonNode bundle,boolean partial) throws Exception {
        return new ScenarioFixtureInstaller(config,root,bundle,partial).run();
    }
    /** baseRefs: base fixtures first, the subcase fixture's own aliases/actors/baseline keys override them. */
    private static ObjectNode merged(JsonNode bundle) {
        ObjectNode own=(ObjectNode)bundle.path("fixture").deepCopy();
        if(bundle.path("bases").isEmpty())return own;
        ObjectNode out=Json.object();
        for(String key:List.of("aliases","actors","baseline")) out.set(key,Json.object());
        out.set("evidence",Json.array());out.set("responsibilities",Json.array());
        List<JsonNode> layers=new ArrayList<>();for(JsonNode b:bundle.path("bases"))layers.add(b.path("fixture"));layers.add(own);
        for(JsonNode layer:layers) {
            for(String key:List.of("aliases","actors","baseline")) layer.path(key).fields().forEachRemaining(e->((ObjectNode)out.path(key)).set(e.getKey(),e.getValue()));
            for(String key:List.of("evidence","responsibilities")) for(JsonNode x:layer.path(key))((ArrayNode)out.path(key)).add(x);
            for(String key:List.of("schemaVersion","fixtureId","synthetic","clock","versions")) if(layer.has(key))out.set(key,layer.path(key));
        }
        return out;
    }
    private ObjectNode run() throws Exception {
        if(!fixture.path("synthetic").asBoolean(false))throw new IllegalArgumentException("Only synthetic fixtures allowed");
        asOf=time(Json.required(fixture.path("clock"),"asOf"));
        fixture.path("aliases").fieldNames().forEachRemaining(a->aliases.put(a,UUID.randomUUID().toString()));
        // Authored root delegators that are not aliases still need an actor id.
        for(JsonNode actor:fixture.path("actors")) {String d=actor.path("grant").path("delegatorAlias").asText(null);if(d!=null&&!aliases.has(d))aliases.put(d,UUID.randomUUID().toString());}
        organizations();
        survey();
        if(!partial&&!omitted.isEmpty())throw new UnsupportedOperationException("scenario fixture facts not installable: "+String.join("; ",omitted.subList(0,Math.min(12,omitted.size())))+(omitted.size()>12?" (+"+(omitted.size()-12)+" more)":""));
        try(Connection connection=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password())) {
            c=connection;c.setAutoCommit(false);
            try {
                for(String org:externals.keySet()) insert("mulino_identity_Organizations",row("ID",aliases.path(org).asText(),"externalAlias",externals.get(org),"createdAt",asOf,"recordedAt",asOf));
                actors();definitionsAndPolicies();items();places();parties();lots();segments();works();evidence();grants();
                if(!partial&&!omitted.isEmpty())throw new UnsupportedOperationException("scenario fixture facts not installable: "+String.join("; ",omitted.subList(0,Math.min(12,omitted.size()))));
                c.commit();
            } catch(Exception failure){c.rollback();throw failure;}
        }
        var result=Json.object();result.set("aliasMap",aliases);result.put("fixtureHash",Json.required(bundle,"fixtureHash"));result.set("clock",fixture.path("clock"));
        if(bundle.hasNonNull("identityBindingHash"))result.set("identityBindingHash",bundle.path("identityBindingHash"));
        result.put("organizationExternalAlias",externals.get(defaultOrgAlias));result.set("organizationExternalAliases",Json.MAPPER.valueToTree(externals));
        result.put("installer","scenario-fixture-installer-v1").put("recordedAtRule","every installed row recordedAt=createdAt=fixture clock asOf (starting clock)");
        result.put("fixtureComplete",omitted.isEmpty());result.set("omittedFacts",Json.MAPPER.valueToTree(omitted));result.set("installerConventions",Json.MAPPER.valueToTree(conventions));
        result.put("committed",true).put("businessExecutionClaimed",false);return result;
    }

    // ---- organizations and survey -------------------------------------------------------------------------------
    private void organizations() {
        List<String> orgs=new ArrayList<>();
        fixture.path("aliases").fields().forEachRemaining(e->{if(e.getValue().path("type").asText().equals("Organization"))orgs.add(e.getKey());});
        if(orgs.isEmpty())throw new IllegalArgumentException("Organization alias required");
        // The primary organization is the one the first actor belongs to (the control identity).
        String first=null;for(JsonNode a:fixture.path("actors")){first=a.path("organizationAlias").asText(null);break;}
        defaultOrgAlias=first!=null&&orgs.contains(first)?first:orgs.get(0);
        String suffix=bundle.path("organizationAliasSuffix").asText(UUID.randomUUID().toString().substring(0,8));
        for(String org:orgs){String external=org+"-"+suffix;if(external.length()>80)throw new IllegalArgumentException("Organization external alias too long");externals.put(org,external);}
        fixture.path("aliases").fields().forEachRemaining(e->{String o=e.getValue().path("organizationAlias").asText(e.getValue().path("type").asText().equals("Organization")?e.getKey():defaultOrgAlias);
            if(!externals.containsKey(o))o=defaultOrgAlias;orgOf.put(e.getKey(),aliases.path(o).asText());});
        for(JsonNode actor:fixture.path("actors")){String d=actor.path("grant").path("delegatorAlias").asText(null);if(d!=null&&!orgOf.containsKey(d))orgOf.put(d,aliases.path(externals.containsKey(actor.path("organizationAlias").asText())?actor.path("organizationAlias").asText():defaultOrgAlias).asText());}
    }
    private void survey() {
        fixture.path("aliases").fields().forEachRemaining(e->{String t=e.getValue().path("type").asText();if(!SUPPORTED_TYPES.contains(t))omitted.add("aliasType "+t+" ("+e.getKey()+")");});
        fixture.path("baseline").fields().forEachRemaining(e->{
            String k=e.getKey();JsonNode v=e.getValue();
            if(DESCRIPTIVE_BASELINE.contains(k)||v.isNull()||v.isBoolean()&&!v.asBoolean()||v.isContainerNode()&&v.isEmpty())return;
            if(Set.of("segments","works","work").contains(k))return;
            omitted.add("baseline."+k);
        });
        if(!fixture.path("responsibilities").isEmpty())omitted.add("responsibilities ("+fixture.path("responsibilities").size()+")");
        if(!fixture.path("evidence").isEmpty())omitted.add("evidence events/claims/verifications (documents installed with availability UNKNOWN only)");
        for(var it=fixture.path("actors").fields();it.hasNext();) {var e=it.next();
            for(var s=e.getValue().path("grant").path("scope").fieldNames();s.hasNext();){String k=s.next();if(!GRANT_METADATA.contains(k)&&scopeKind(k)==null)omitted.add("grantScope "+k+" ("+e.getKey()+")");}
        }
    }

    // ---- identity --------------------------------------------------------------------------------------------------
    private void actors() throws SQLException {
        Set<String> done=new HashSet<>();
        for(var it=fixture.path("aliases").fields();it.hasNext();){var e=it.next();String t=e.getValue().path("type").asText();
            if(!Set.of("Human","Agent").contains(t))continue;actor(e.getKey(),t.equals("Human")?"HUMAN":"AGENT");done.add(e.getKey());}
        for(var it=fixture.path("actors").fields();it.hasNext();){var e=it.next();if(done.add(e.getKey())){actor(e.getKey(),"HUMAN");conventions.add("actor "+e.getKey()+" has no Human/Agent alias; installed as HUMAN");}
            String d=e.getValue().path("grant").path("delegatorAlias").asText(null);if(d!=null&&done.add(d)){actor(d,"HUMAN");conventions.add("root delegator "+d+" installed as HUMAN");}}
    }
    private void actor(String alias,String kind) throws SQLException {
        String org=org(alias),id=id(alias);
        insert("mulino_identity_Actors",row("organizationId",org,"ID",id,"kind",kind,"stableRequestOwner",UUID.randomUUID().toString(),"createdAt",asOf,"recordedAt",asOf));
        insert("mulino_identity_AuthorityFences",row("organizationId",org,"actorId",id));
    }
    private void grants() throws SQLException {
        Map<String,Set<String>> rootActions=new LinkedHashMap<>();Map<String,OffsetDateTime[]> rootValidity=new HashMap<>();
        for(var it=fixture.path("actors").fields();it.hasNext();) {
            var e=it.next();JsonNode a=e.getValue();String alias=e.getKey(),actor=id(alias),org=org(alias);
            boolean untrusted=a.path(ActualFixtureBindings.UNTRUSTED).asBoolean(false);
            if(!untrusted&&(!config.issuer().equals(Json.required(a,"issuer"))||!config.audience().equals(Json.required(a,"audience"))))throw new IllegalArgumentException("Fixture identity issuer/audience differs from backend");
            String orgAlias=a.path("organizationAlias").asText();String external=externals.getOrDefault(orgAlias,externals.get(defaultOrgAlias));
            JsonNode grant=a.path("grant");
            OffsetDateTime from=time(Json.required(grant,"validFrom")),until=time(Json.required(grant,"validUntil"));
            insert("mulino_identity_ExternalIdentities",row("organizationId",org,"ID",uuid(),"actorId",actor,"issuer",untrusted?Json.required(a,"issuer"):config.issuer(),"subject",Json.required(a,"subject"),"organizationAlias",external,"createdAt",asOf,"recordedAt",asOf));
            insert("mulino_identity_Memberships",row("organizationId",org,"ID",uuid(),"actorId",actor,"validFrom",from,"validUntil",until,"createdAt",asOf,"recordedAt",asOf));
            for(JsonNode cap:a.path("roleCapabilities"))insert("mulino_identity_CapabilityAssignments",row("organizationId",org,"ID",uuid(),"actorId",actor,"capabilityId",cap.asText(),"scopeKind","ORGANIZATION","scopeId",org,"validFrom",from,"validUntil",until,"createdAt",asOf,"recordedAt",asOf));
            String delegatorAlias=grant.path("delegatorAlias").asText(alias);String delegator=id(delegatorAlias);
            if(!org.equals(org(delegatorAlias))) {
                // Cross-organization delegation is not representable (Grants FK is organization-local): the actor's own
                // organization receives a shadow root delegator covering exactly the delegated actions.
                String shadow=delegatorAlias+"@"+orgAlias;
                if(!aliases.has(shadow)){aliases.put(shadow,uuid());orgOf.put(shadow,org);actor(shadow,"HUMAN");}
                conventions.add("cross-organization delegator "+delegatorAlias+" of "+alias+" replaced by shadow root "+shadow);
                omitted.add("cross-organization delegation "+delegatorAlias+" -> "+alias+" (shadow root installed)");
                delegatorAlias=shadow;delegator=id(shadow);
            }
            Set<String> actions=new LinkedHashSet<>();for(JsonNode x:grant.path("actions"))actions.add(x.asText());
            List<String[]> scopes=new ArrayList<>();
            for(var s=grant.path("scope").fields();s.hasNext();) {
                var se=s.next();String kind=scopeKind(se.getKey());if(kind==null)continue;
                JsonNode values=se.getValue().isArray()?se.getValue():Json.array().add(se.getValue());
                for(JsonNode v:values){String target=aliases.path(v.asText()).asText(null);if(target==null){if(kind.equals("SOURCE")&&v.asText().length()<=36)target=v.asText();else{omitted.add("grantScope "+se.getKey()+" value "+v.asText()+" ("+alias+")");continue;}}scopes.add(new String[]{kind,target});}
            }
            // Dimension restrictions are installed as-is; an organization row is added only when no dimension is named.
            if(scopes.isEmpty())scopes.add(new String[]{"ORGANIZATION",org});
            // Default: one grant, its dimensions intersect (product semantics). Probe mode unionGrantDimensions: one grant per
            // dimension kind, so the authored item/work/target lists act as alternatives (the plan does not decide this).
            Map<String,List<String[]>> groups=new LinkedHashMap<>();
            for(String[] sc:scopes)groups.computeIfAbsent(unionGrantDimensions?sc[0]:"ALL",k->new ArrayList<>()).add(sc);
            if(groups.size()>1)conventions.add("grant of "+alias+" split into "+groups.size()+" single-dimension grants (probe unionGrantDimensions)");
            for(var group:groups.values()) {
                String grantId=uuid();
                var g=row("organizationId",org,"ID",grantId,"actorId",actor,"delegatorId",delegator,"validFrom",from,"validUntil",until,"createdAt",asOf,"recordedAt",asOf);
                if(grant.hasNonNull("revision"))g.put("revision",grant.path("revision").asInt());
                if(grant.path("scope").hasNonNull("revokedAt"))g.put("revokedAt",time(grant.path("scope").path("revokedAt").asText()));
                insert("mulino_identity_Grants",g);
                for(String x:actions)insert("mulino_identity_GrantActions",row("organizationId",org,"grantId",grantId,"capabilityId",x));
                Set<String> seen=new HashSet<>();for(String[] sc:group)if(seen.add(sc[0]+":"+sc[1]))insert("mulino_identity_GrantScopes",row("organizationId",org,"grantId",grantId,"scopeKind",sc[0],"scopeId",sc[1]));
            }
            // A delegator that is not itself a fixture actor is the fixture's root: it receives a self-rooted grant covering what it delegates.
            if(!fixture.path("actors").has(delegatorAlias)&&!delegatorAlias.equals(alias)) {
                rootActions.computeIfAbsent(delegatorAlias,k->new LinkedHashSet<>()).addAll(actions);
                var v=rootValidity.computeIfAbsent(delegatorAlias,k->new OffsetDateTime[]{from,until});if(from.isBefore(v[0]))v[0]=from;if(until.isAfter(v[1]))v[1]=until;
            }
        }
        for(var e:rootActions.entrySet()) {
            String alias=e.getKey(),actor=id(alias),org=org(alias);var v=rootValidity.get(alias);String grantId=uuid();
            insert("mulino_identity_Memberships",row("organizationId",org,"ID",uuid(),"actorId",actor,"validFrom",v[0],"validUntil",v[1],"createdAt",asOf,"recordedAt",asOf));
            insert("mulino_identity_Grants",row("organizationId",org,"ID",grantId,"actorId",actor,"delegatorId",actor,"validFrom",v[0],"validUntil",v[1],"createdAt",asOf,"recordedAt",asOf));
            insert("mulino_identity_GrantScopes",row("organizationId",org,"grantId",grantId,"scopeKind","ORGANIZATION","scopeId",org));
            for(String x:e.getValue()){insert("mulino_identity_GrantActions",row("organizationId",org,"grantId",grantId,"capabilityId",x));
                insert("mulino_identity_CapabilityAssignments",row("organizationId",org,"ID",uuid(),"actorId",actor,"capabilityId",x,"scopeKind","ORGANIZATION","scopeId",org,"validFrom",v[0],"validUntil",v[1],"createdAt",asOf,"recordedAt",asOf));}
            conventions.add("root delegator "+alias+" received an organization root grant for "+e.getValue().size()+" delegated actions");
        }
    }
    static String scopeKind(String key) {
        return switch(key) {
            case "itemAliases","items","itemAlias" -> "ITEM";
            case "workAliases","works" -> "WORK";
            case "placeAliases","places" -> "PLACE";
            case "segmentAliases","segments","lotAliases","customerAliases","supplierAliases","documentAliases","targetAliases","physicalRootAliases" -> "TARGET";
            case "sourceNamespaces" -> "SOURCE";
            default -> null;
        };
    }

    // ---- definitions and policies ----------------------------------------------------------------------------------
    private void definitionsAndPolicies() throws Exception {
        JsonNode conv=Json.read(root.resolve("verification/actual/scenarios/installer-conventions.json"));
        String version=fixture.path("versions").path("definition").asText("definition-v1");
        Set<String> orgs=new HashSet<>();for(String o:externals.keySet())orgs.add(aliases.path(o).asText());
        Map<String,String> definitionAliases=new LinkedHashMap<>();
        fixture.path("aliases").fields().forEachRemaining(e->{if(e.getValue().path("type").asText().equals("DefinitionVersion"))definitionAliases.put(e.getKey(),orgOf.get(e.getKey()));});
        Set<String> publishedFor=new HashSet<>();
        for(var e:definitionAliases.entrySet()) {
            JsonNode a=fixture.path("aliases").path(e.getKey());ObjectNode content=a.path("content").isObject()&&!a.path("content").isEmpty()?(ObjectNode)a.path("content").deepCopy():(ObjectNode)conv.path("definition").deepCopy();
            String state=a.path("content").path("state").asText(a.path("state").asText("PUBLISHED"));
            if(!a.path("content").isObject()||a.path("content").isEmpty()){content.put("version",a.path("version").asText(version));conventions.add("definition "+e.getKey()+" content from installer convention");}
            if(!publishedFor.add(e.getValue()+":"+content.path("version").asText())){omitted.add("duplicate definition version "+content.path("version").asText()+" ("+e.getKey()+")");continue;}
            publish(e.getValue(),id(e.getKey()),content,state);
        }
        for(String org:orgs) if(!publishedFor.contains(org+":"+version)) {
            ObjectNode content=(ObjectNode)conv.path("definition").deepCopy();content.put("version",version);publish(org,uuid(),content,"PUBLISHED");publishedFor.add(org+":"+version);
            conventions.add("published convention definition "+version);
        }
        Set<String> authoredKinds=new HashSet<>();
        for(var it=fixture.path("aliases").fields();it.hasNext();) {
            var e=it.next();JsonNode a=e.getValue();if(!a.path("type").asText().equals("PolicyVersion"))continue;
            if(!a.path("content").isObject()||!a.hasNonNull("kind")||!a.hasNonNull("effectiveFrom")){omitted.add("PolicyVersion "+e.getKey()+" without authored kind/content/effectiveFrom");continue;}
            String org=orgOf.get(e.getKey());policy(org,id(e.getKey()),a.path("kind").asText(),a.path("version").asText(e.getKey()),a.path("content"),time(a.path("effectiveFrom").asText()),a.hasNonNull("effectiveUntil")?time(a.path("effectiveUntil").asText()):null,a.path("activate").asBoolean(true));
            authoredKinds.add(org+":"+a.path("kind").asText());
        }
        for(String org:orgs) for(JsonNode p:conv.path("policies")) if(!authoredKinds.contains(org+":"+p.path("kind").asText())) {
            policy(org,uuid(),p.path("kind").asText(),p.path("version").asText(),p.path("content"),asOf.minusDays(30),null,true);
        }
        conventions.add("activated convention policies for kinds without an authored PolicyVersion");
    }
    private void publish(String org,String id,ObjectNode content,String state) throws Exception {
        content.put("organizationId",org).put("id",id).put("state",state);
        insert("mulino_definitions_DefinitionVersions",row("organizationId",org,"ID",id,"version",Json.required(content,"version"),"state",state,"contentHash",FixtureInstaller.contentHash(content),"content",content.toString(),"evaluatorVersion",Json.required(content,"evaluatorVersion"),"schemaVersion",Json.required(content,"schemaVersion"),"createdAt",asOf.toLocalDateTime()));
    }
    private void policy(String org,String id,String kind,String version,JsonNode content,OffsetDateTime from,OffsetDateTime until,boolean activate) throws Exception {
        insert("mulino_governance_PolicyVersions",row("organizationId",org,"ID",id,"version",version,"kind",kind,"content",content.toString(),"contentHash",FixtureInstaller.contentHash(content),"effectiveFrom",from,"effectiveUntil",until,"createdAt",asOf));
        if(activate)insert("mulino_governance_ActivePolicies",row("organizationId",org,"kind",kind,"policyId",id,"revision",0));
    }

    // ---- master data -----------------------------------------------------------------------------------------------
    private void items() throws Exception {
        for(var it=fixture.path("aliases").fields();it.hasNext();) {
            var e=it.next();JsonNode a=e.getValue();if(!a.path("type").asText().equals("TradeItem"))continue;
            String org=orgOf.get(e.getKey()),product=uuid(),spec=uuid(),pack=uuid(),name=a.path("name").asText(e.getKey()),unit=a.path("unit").asText(a.path("baseUnit").asText("BOX"));
            if(!a.has("unit")&&!a.has("baseUnit"))conventions.add("TradeItem "+e.getKey()+" without unit installed with BOX");
            insert("mulino_inventory_Products",row("organizationId",org,"ID",product,"name",name,"createdAt",asOf,"recordedAt",asOf));
            for(String[] v:new String[][]{{"mulino_inventory_SpecificationVersions",spec},{"mulino_inventory_PackagingVersions",pack}}) {
                var content=Json.object();content.put("description","scenario installer synthetic "+e.getKey());
                insert(v[0],row("organizationId",org,"ID",v[1],"productId",product,"version","1","contentHash",FixtureInstaller.contentHash(content),"description",content.toString(),"createdAt",asOf,"recordedAt",asOf));
            }
            insert("mulino_inventory_TradeItems",row("organizationId",org,"ID",id(e.getKey()),"productId",product,"name",name,"baseUnit",unit,"decimalPlaces",a.path("decimalPlaces").asInt(0),"specificationVersionId",spec,"packagingVersionId",pack,"createdAt",asOf,"recordedAt",asOf));
        }
    }
    private void places() throws SQLException {
        for(var it=fixture.path("aliases").fields();it.hasNext();) {
            var e=it.next();JsonNode a=e.getValue();if(!a.path("type").asText().equals("Place"))continue;
            if(!a.hasNonNull("kind")){omitted.add("Place "+e.getKey()+" without kind (no default)");continue;}
            var r=row("organizationId",orgOf.get(e.getKey()),"ID",id(e.getKey()),"name",a.path("name").asText(e.getKey()),"kind",a.path("kind").asText(),"createdAt",asOf,"recordedAt",asOf);
            insert("mulino_inventory_Places",r);
        }
    }
    private void parties() throws SQLException {
        for(var it=fixture.path("aliases").fields();it.hasNext();) {
            var e=it.next();JsonNode a=e.getValue();String t=a.path("type").asText(),org=orgOf.get(e.getKey()),name=a.path("name").asText(e.getKey());
            if(t.equals("Customer"))insert("mulino_trade_sales_Customers",row("organizationId",org,"ID",id(e.getKey()),"name",name,"revision",1,"createdAt",asOf,"recordedAt",asOf,"effectiveAt",asOf));
            if(t.equals("Supplier"))insert("mulino_trade_purchase_Suppliers",row("organizationId",org,"ID",id(e.getKey()),"name",name,"revision",1,"createdAt",asOf,"recordedAt",asOf,"effectiveAt",asOf));
            if(t.equals("Manufacturer"))insert("mulino_inventory_Manufacturers",row("organizationId",org,"ID",id(e.getKey()),"name",name,"createdAt",asOf,"recordedAt",asOf));
        }
    }
    private void lots() throws SQLException {
        for(var it=fixture.path("aliases").fields();it.hasNext();) {
            var e=it.next();JsonNode a=e.getValue();if(!Set.of("ManufacturerLot","ManufacturingLot").contains(a.path("type").asText()))continue;
            String item=a.path("itemAlias").asText(null);if(item==null)item=inferLotItem(e.getKey());
            if(item==null){omitted.add("lot "+e.getKey()+" without inferable item");continue;}
            String org=orgOf.get(e.getKey()),manufacturer;
            if(a.hasNonNull("manufacturerAlias")) {
                String m=a.path("manufacturerAlias").asText();manufacturer=id(m);
                if(!fixture.path("aliases").path(m).path("type").asText().equals("Manufacturer")&&ensured.add(org+":M:"+manufacturer)){
                    insert("mulino_inventory_Manufacturers",row("organizationId",org,"ID",manufacturer,"name",fixture.path("aliases").path(m).path("name").asText(m),"createdAt",asOf,"recordedAt",asOf));
                    conventions.add("lot "+e.getKey()+" manufacturer "+m+" ("+fixture.path("aliases").path(m).path("type").asText()+") installed as a Manufacturer row with the same id");}
            }
            else {manufacturer=uuid();insert("mulino_inventory_Manufacturers",row("organizationId",org,"ID",manufacturer,"name","synthetic manufacturer of "+e.getKey(),"createdAt",asOf,"recordedAt",asOf));conventions.add("lot "+e.getKey()+" manufacturer synthesized");}
            insert("mulino_inventory_ManufacturingLots",row("organizationId",org,"ID",id(e.getKey()),"manufacturerId",manufacturer,"itemId",id(item),"originalLot",a.path("originalLot").asText(e.getKey()),"expiresAt",a.hasNonNull("expiresAt")?time(a.path("expiresAt").asText()):null,"createdAt",asOf,"recordedAt",asOf));
        }
    }
    private String inferLotItem(String lot) {
        Set<String> items=new HashSet<>();
        for(JsonNode s:segmentEntries().values())if(lot.equals(s.path("lotAlias").asText())&&s.hasNonNull("itemAlias"))items.add(s.path("itemAlias").asText());
        if(items.size()==1)return items.iterator().next();
        List<String> all=new ArrayList<>();fixture.path("aliases").fields().forEachRemaining(e->{if(e.getValue().path("type").asText().equals("TradeItem")&&orgOf.get(e.getKey()).equals(orgOf.get(lot)))all.add(e.getKey());});
        if(all.size()==1){conventions.add("lot "+lot+" item inferred as the organization's only TradeItem");return all.get(0);}
        return null;
    }
    private Map<String,ObjectNode> segmentEntries() {
        Map<String,ObjectNode> out=new LinkedHashMap<>();
        fixture.path("aliases").fields().forEachRemaining(e->{if(e.getValue().path("type").asText().equals("QuantitySegment"))out.put(e.getKey(),(ObjectNode)e.getValue().deepCopy());});
        for(JsonNode s:fixture.path("baseline").path("segments")) if(s.hasNonNull("alias")) {ObjectNode merged=out.computeIfAbsent(s.path("alias").asText(),k->Json.object());s.fields().forEachRemaining(f->{if(!merged.has(f.getKey()))merged.set(f.getKey(),f.getValue());});}
        return out;
    }
    private void segments() throws SQLException {
        for(var e:segmentEntries().entrySet()) {
            String alias=e.getKey();ObjectNode s=e.getValue();
            if(!aliases.has(alias)){omitted.add("baseline segment "+alias+" has no alias");continue;}
            if(s.has("active")&&!s.path("active").asBoolean(true)){omitted.add("inactive segment "+alias);continue;}
            String lot=s.path("lotAlias").asText(null),item=s.path("itemAlias").asText(null),place=s.path("locationAlias").asText(s.path("placeAlias").asText(null));
            if(item==null&&lot!=null)item=fixture.path("aliases").path(lot).path("itemAlias").asText(null);
            if(item==null&&lot!=null)item=inferLotItem(lot);
            if(item==null||place==null||!s.hasNonNull("quantity")||!s.hasNonNull("unit")||s.has("parentAlias")){omitted.add("segment "+alias+" (item/place/quantity/unit/parent not mappable)");continue;}
            OffsetDateTime from=s.hasNonNull("validFrom")?time(s.path("validFrom").asText()):asOf;
            for(String role:List.of("ownerAlias","custodianAlias")) if(s.hasNonNull(role)) partyActor(orgOf.get(alias),s.path(role).asText());
            insert("mulino_inventory_QuantitySegments",row("organizationId",orgOf.get(alias),"ID",id(alias),"itemId",id(item),"lotId",lot==null?null:id(lot),"identificationStatus",lot==null?"UNKNOWN":"CONFIRMED","quantity",new BigDecimal(s.path("quantity").asText()),"unit",s.path("unit").asText(),
                "placeId",id(place),"controlScope",s.path("physicalScope").asText(alias),"validFrom",from,"mixtureStatus","IDENTIFIED","createdAt",asOf,"recordedAt",asOf,
                "ownerId",s.hasNonNull("ownerAlias")?id(s.path("ownerAlias").asText()):null,"custodianId",s.hasNonNull("custodianAlias")?id(s.path("custodianAlias").asText()):null,"evidenceRef","authored-input:"+Json.required(bundle,"fixtureHash")));
            insert("mulino_inventory_QuantityMovements",row("organizationId",orgOf.get(alias),"ID",uuid(),"revision",1,"targetId",id(alias),"quantity",new BigDecimal(s.path("quantity").asText()),"unit",s.path("unit").asText(),"kind","INITIAL_BALANCE","occurredAt",from,"evidenceRef","authored-input:"+Json.required(bundle,"fixtureHash"),"createdAt",asOf,"recordedAt",asOf));
        }
    }

    /** Segment owner/custodian is an Actor FK: an authored Organization/Supplier/Customer party becomes an EXTERNAL actor row with the same id. */
    private void partyActor(String org,String alias) throws SQLException {
        String type=fixture.path("aliases").path(alias).path("type").asText();
        if(Set.of("Human","Agent").contains(type)||fixture.path("actors").has(alias))return;
        if(ensured.add(org+":A:"+id(alias))){
            insert("mulino_identity_Actors",row("organizationId",org,"ID",id(alias),"kind","EXTERNAL","stableRequestOwner",uuid(),"createdAt",asOf,"recordedAt",asOf));
            conventions.add("segment party "+alias+" ("+type+") installed as an EXTERNAL actor with the same id");
        }
    }

    // ---- works -----------------------------------------------------------------------------------------------------
    private void works() throws Exception {
        List<JsonNode> entries=new ArrayList<>();
        for(String key:List.of("works","work")){JsonNode v=fixture.path("baseline").path(key);if(v.isArray())v.forEach(entries::add);else if(v.isObject()&&v.has("alias"))entries.add(v);else if(v.isObject()&&!v.isEmpty())omitted.add("baseline."+key+" (unkeyed object)");}
        Set<String> installed=new HashSet<>();
        for(JsonNode w:entries) {
            String alias=w.path("alias").asText(null);if(alias==null||!aliases.has(alias)){omitted.add("work entry without alias");continue;}
            if(!installed.add(alias))continue;
            work(alias,w);
        }
        for(var it=fixture.path("aliases").fields();it.hasNext();){var e=it.next();if(e.getValue().path("type").asText().equals("Work")&&!installed.contains(e.getKey()))omitted.add("Work "+e.getKey()+" without baseline entry");}
    }
    private void work(String alias,JsonNode w) throws Exception {
        String org=orgOf.get(alias);
        String item=w.path("itemAlias").asText(null);if(item==null)item=workItem(alias);
        if(item==null){omitted.add("Work "+alias+" without inferable item");return;}
        String owner=w.path("ownerAlias").asText(null),supervisor=w.path("supervisorAlias").asText(null);
        for(JsonNode r:fixture.path("responsibilities")) if(alias.equals(r.path("scope").path("workAlias").asText())){if(owner==null)owner=r.path("ownerAlias").asText(null);if(supervisor==null)supervisor=r.path("supervisorAlias").asText(null);}
        if(supervisor==null&&aliases.has("supervisor"))supervisor="supervisor";
        if(owner==null)owner=supervisor;
        if(owner==null||supervisor==null||!aliases.has(owner)||!aliases.has(supervisor)){omitted.add("Work "+alias+" owner/supervisor not resolvable");return;}
        String status=w.path("status").asText(w.path("state").asText("ACTIVE"));
        String version=w.path("definitionVersion").asText(fixture.path("versions").path("definition").asText("definition-v1"));
        String definition=definitionId(org,version);
        if(definition==null){omitted.add("Work "+alias+" definition "+version+" not installed");return;}
        var row=row("organizationId",org,"ID",id(alias),"revision",w.path("revision").asInt(1),"createdAt",asOf,"recordedAt",asOf,"effectiveAt",asOf,"itemId",id(item),"definitionVersionId",definition,
            "kind",w.path("kind").asText(guessKind(alias)),"status",status,"ownerId",id(owner),"supervisorId",id(supervisor),"lifecycleMode","COMMAND","pendingInvalidation",false);
        if(w.hasNonNull("closeReason"))row.put("closeReason",w.path("closeReason").asText());
        if(status.equals("WAITING")) {
            var wait=Json.object();JsonNode authored=w.path("wait");
            wait.put("reason",authored.path("reason").asText(w.path("waitReason").asText("authored fixture wait")));
            wait.put("target",authored.path("target").asText(alias));
            wait.set("resumePredicate",authored.has("resumePredicate")?authored.path("resumePredicate"):w.has("resumePredicate")?w.path("resumePredicate"):Json.object());
            wait.put("verifier",authored.has("verifier")?authored.path("verifier").asText():w.hasNonNull("verifierAlias")?id(w.path("verifierAlias").asText()):id(supervisor));
            wait.put("nextCheckAt",authored.path("nextCheckAt").asText(w.path("nextCheckAt").asText(asOf.plusHours(1).toInstant().toString())));
            wait.put("overdueAction",authored.path("overdueAction").asText(w.path("overdueAction").asText("supervisor review")));
            row.put("waitJson",wait.toString());
        }
        insert("mulino_work_read_Works",row);
        JsonNode goal=w.path("goal");
        if(goal.isObject()&&!goal.isEmpty()) {
            var slots=Json.object();
            if(goal.hasNonNull("quantityMode"))slots.put("quantityMode",goal.path("quantityMode").asText());
            String quantity=goal.path("targetQuantity").asText(goal.path("quantity").asText(w.path("quantity").asText(null)));
            String unit=goal.path("unit").asText(w.path("unit").asText(null));
            if(quantity!=null&&unit!=null){slots.put("targetQuantity",quantity);slots.put("unit",unit);}
            for(String k:List.of("endpoint","dueAt","timezone","evidencePolicyVersion"))if(goal.hasNonNull(k))slots.put(k,goal.path(k).asText());
            var scope=Json.object();scope.put("itemId",id(item));if(goal.hasNonNull("destinationAlias"))scope.put("placeId",id(goal.path("destinationAlias").asText()));slots.set("scope",scope);
            String goalId=w.hasNonNull("goalVersionAlias")&&aliases.has(w.path("goalVersionAlias").asText())?id(w.path("goalVersionAlias").asText()):uuid();
            var g=row("organizationId",org,"ID",goalId,"revision",1,"createdAt",asOf,"recordedAt",asOf,"effectiveAt",asOf,"workId",id(alias),"definitionVersionId",definition,"quantityMode",slots.path("quantityMode").asText(null),
                "targetQuantity",quantity!=null&&unit!=null?new BigDecimal(quantity):null,"unit",unit,"endpoint",slots.path("endpoint").asText(null),"scopeJson",scope.toString(),"slotsJson",slots.toString(),"provenanceJson","{}",
                "timezone",slots.path("timezone").asText(null),"dueAt",slots.hasNonNull("dueAt")?time(slots.path("dueAt").asText()):null,"goalVersion",w.path("goalVersion").asInt(1));
            insert("mulino_work_read_GoalReferences",g);
            update("UPDATE mulino_work_read_Works SET currentGoalVersionId=? WHERE organizationId=? AND ID=?",goalId,org,id(alias));
            conventions.add("Work "+alias+" goal mapped quantity->targetQuantity, destinationAlias->scope.placeId");
        }
    }
    private String guessKind(String alias) {
        for(JsonNode r:fixture.path("baseline").path("works"))if(alias.equals(r.path("alias").asText())&&r.hasNonNull("kind"))return r.path("kind").asText();
        conventions.add("Work "+alias+" kind defaulted to PURCHASE");return "PURCHASE";
    }
    private String workItem(String work) {
        Set<String> items=new LinkedHashSet<>();
        for(JsonNode a:fixture.path("actors")){JsonNode s=a.path("grant").path("scope");boolean has=false;for(JsonNode w:s.path("workAliases"))has|=w.asText().equals(work);if(has)for(JsonNode i:s.path("itemAliases"))items.add(i.asText());}
        items.removeIf(i->!orgOf.getOrDefault(i,"").equals(orgOf.get(work)));
        if(items.size()==1){conventions.add("Work "+work+" item inferred from grant scope");return items.iterator().next();}
        List<String> all=new ArrayList<>();fixture.path("aliases").fields().forEachRemaining(e->{if(e.getValue().path("type").asText().equals("TradeItem")&&orgOf.get(e.getKey()).equals(orgOf.get(work)))all.add(e.getKey());});
        if(all.size()==1){conventions.add("Work "+work+" item inferred as the organization's only TradeItem");return all.get(0);}
        return null;
    }
    private String definitionId(String org,String version) throws SQLException {
        try(var s=c.prepareStatement("SELECT ID FROM mulino_definitions_DefinitionVersions WHERE organizationId=? AND version=? AND state='PUBLISHED'")){s.setString(1,org);s.setString(2,version);try(var r=s.executeQuery()){return r.next()?r.getString(1):null;}}
    }

    // ---- evidence documents ----------------------------------------------------------------------------------------
    private void evidence() throws SQLException {
        Map<String,String> profiles=new HashMap<>();
        String supervisor=aliases.has("supervisor")?"supervisor":null;
        for(JsonNode e:fixture.path("evidence")) {
            String alias=e.path("alias").asText(null);if(alias==null||!aliases.has(alias)){omitted.add("evidence entry without alias");continue;}
            String org=orgOf.get(alias),namespace=e.path("sourceNamespace").asText("synthetic");
            String recorder=supervisor!=null?id(supervisor):firstActor(org);
            if(recorder==null){omitted.add("evidence "+alias+" without recorder actor");continue;}
            String profile=profiles.get(org+":"+namespace);
            if(profile==null){profile=uuid();profiles.put(org+":"+namespace,profile);
                insert("mulino_evidence_SourceProfiles",row("organizationId",org,"ID",profile,"revision",1,"createdAt",asOf,"recordedAt",asOf,"recordedBy",recorder,"namespace",namespace,"policyVersion","fixture-v1","intakeOwnerId",recorder,"supervisorId",recorder,"nextAction","authored fixture source review","nextCheckAt",asOf.plusDays(1)));}
            String item=firstOfType(org,"TradeItem");
            if(item==null){omitted.add("evidence "+alias+" without item subject");continue;}
            String sha=e.path("sha256").asText();if(!sha.matches("[0-9a-f]{64}")){omitted.add("evidence "+alias+" sha256 not hex");continue;}
            insert("mulino_evidence_DocumentVersions",row("organizationId",org,"ID",id(alias),"revision",1,"createdAt",asOf,"recordedAt",asOf,"recordedBy",recorder,"subjectKind","ITEM","subjectId",item,"itemId",item,"sha256",sha,"byteLength",0L,"mediaType","application/octet-stream",
                "sourceNamespace",namespace,"sourceProfileId",profile,"sourceReference",e.path("externalEventId").asText(null),"availability","UNKNOWN","provenance","{\"source\":\"scenario-installer-authored-hash-only\"}"));
        }
    }
    private String firstActor(String org) {for(var it=fixture.path("aliases").fields();it.hasNext();){var e=it.next();if(e.getValue().path("type").asText().equals("Human")&&org.equals(orgOf.get(e.getKey())))return id(e.getKey());}return null;}
    private String firstOfType(String org,String type) {for(var it=fixture.path("aliases").fields();it.hasNext();){var e=it.next();if(e.getValue().path("type").asText().equals(type)&&org.equals(orgOf.get(e.getKey())))return id(e.getKey());}return null;}

    // ---- helpers ---------------------------------------------------------------------------------------------------
    private String id(String alias){String v=aliases.path(alias).asText(null);if(v==null)throw new IllegalArgumentException("Unknown fixture alias "+alias);return v;}
    private String org(String alias){String o=orgOf.get(alias);return o!=null?o:aliases.path(defaultOrgAlias).asText();}
    private static String uuid(){return UUID.randomUUID().toString();}
    private static OffsetDateTime time(String v){return OffsetDateTime.parse(v);}
    private static Map<String,Object> row(Object... kv){var m=new LinkedHashMap<String,Object>();for(int i=0;i<kv.length;i+=2)m.put((String)kv[i],kv[i+1]);return m;}
    private void insert(String table,Map<String,Object> row) throws SQLException {
        var r=new LinkedHashMap<String,Object>();row.forEach((k,v)->{if(v!=null)r.put(k,v);});
        String sql="INSERT INTO "+table+"("+String.join(",",r.keySet())+") VALUES("+String.join(",",Collections.nCopies(r.size(),"?"))+")";
        try(var s=c.prepareStatement(sql)){int i=1;for(Object v:r.values())s.setObject(i++,v);if(s.executeUpdate()!=1)throw new SQLException("Fixture insert did not create exactly one row: "+table);}
        catch(SQLException failure){throw new SQLException("insert "+table+" failed: "+SqlFailureSummary.safe(failure),failure.getSQLState());}
    }
    private void update(String sql,Object... p) throws SQLException {try(var s=c.prepareStatement(sql)){for(int i=0;i<p.length;i++)s.setObject(i+1,p[i]);s.executeUpdate();}}
}
