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
        "Place","Customer","Supplier","QuantitySegment","Work","DocumentVersion","DefinitionVersion","PolicyVersion","SalesOrder","SalesOrderLine","Allocation",
        "PurchaseProposal","PurchaseOrder","PurchaseOrderLine","Invoice","Recall","Investigation","Shipment","CargoScope","RegulatoryProcedure","ReconciliationCase","Grant");
    /** Baseline keys whose content is merged into alias attributes or installed by a dedicated step. */
    static final Set<String> CONSUMED_BASELINE=Set.of("segments","works","work","items","places","lots","organizations","priorEntities","documents","sourceProfiles","entityRevisions","salesOrder","allocations","allocation","managementAuthorities");
    /**
     * Grant scope keys with no installable product dimension: mulino_identity_GrantScopes.scopeKind is CHECKed to
     * ORGANIZATION, TARGET, WORK, ITEM, PLACE, SOURCE. The grant is installed without them (broader than authored), so the
     * fact stays in omittedFacts (fixture incomplete) and is listed in notRepresentedFacts as a Step 2/product finding.
     */
    static final Set<String> UNREPRESENTED_SCOPE=Set.of("workKinds","derivedSegmentsWithinSamePhysicalRoot","settlementModes","basisKinds","customerAliases","supplierAliases");
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
    /** Authored facts the product has no row or authorization dimension for: reported (Step 2/product findings), not an installer gap. */
    private final List<String> notRepresented=new ArrayList<>();
    private OffsetDateTime knownAt;
    private final Path blobRoot=System.getProperty("verification.actual.blobRoot")==null?null:Path.of(System.getProperty("verification.actual.blobRoot"));
    private final List<Path> writtenBlobs=new ArrayList<>();
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
        asOf=time(Json.required(fixture.path("clock"),"asOf"));knownAt=fixture.path("clock").hasNonNull("knownAt")?time(fixture.path("clock").path("knownAt").asText()):asOf;
        normalizeAliases();
        fixture.path("aliases").fieldNames().forEachRemaining(a->aliases.put(a,UUID.randomUUID().toString()));
        // Fixture evidence entries name their own alias (round 12: T13-T15/T19 "need"); bind it like any alias.
        for(JsonNode e:fixture.path("evidence")){String a=e.path("alias").asText(null);if(a!=null&&!aliases.has(a)){aliases.put(a,UUID.randomUUID().toString());((ObjectNode)fixture.path("aliases")).set(a,Json.object().put("type","DocumentVersion"));}}
        // Authored root delegators that are not aliases still need an actor id.
        for(JsonNode actor:fixture.path("actors")) {String d=actor.path("grant").path("delegatorAlias").asText(null);if(d!=null&&!aliases.has(d))aliases.put(d,UUID.randomUUID().toString());}
        organizations();
        survey();
        if(!partial&&!omitted.isEmpty())throw new UnsupportedOperationException("scenario fixture facts not installable: "+String.join("; ",omitted.subList(0,Math.min(12,omitted.size())))+(omitted.size()>12?" (+"+(omitted.size()-12)+" more)":""));
        try(Connection connection=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password())) {
            c=connection;c.setAutoCommit(false);
            try {
                for(String org:externals.keySet()) insert("mulino_identity_Organizations",row("ID",aliases.path(org).asText(),"externalAlias",externals.get(org),"createdAt",asOf,"recordedAt",asOf));
                actors();definitionsAndPolicies();items();places();parties();lots();segments();works();sourceProfiles();evidence();sales();allocations();tradeAliases();grants();
                if(!partial&&!omitted.isEmpty())throw new UnsupportedOperationException("scenario fixture facts not installable: "+String.join("; ",omitted.subList(0,Math.min(12,omitted.size()))));
                c.commit();
            } catch(Exception failure){c.rollback();for(Path b:writtenBlobs)java.nio.file.Files.deleteIfExists(b);throw failure;}
        }
        var result=Json.object();result.set("aliasMap",aliases);result.put("fixtureHash",Json.required(bundle,"fixtureHash"));result.set("clock",fixture.path("clock"));
        if(bundle.hasNonNull("identityBindingHash"))result.set("identityBindingHash",bundle.path("identityBindingHash"));
        result.put("organizationExternalAlias",externals.get(defaultOrgAlias));result.set("organizationExternalAliases",Json.MAPPER.valueToTree(externals));
        result.put("installer","scenario-fixture-installer-v2").put("recordedAtRule","every installed row recordedAt=createdAt=fixture clock asOf (starting clock); evidence declared recordedAt later than the fixture knownAt is recorded at that declared instant (late-known)");
        result.put("fixtureComplete",omitted.isEmpty());result.set("omittedFacts",Json.MAPPER.valueToTree(omitted));result.set("notRepresentedFacts",Json.MAPPER.valueToTree(notRepresented));var types=Json.object();fixture.path("aliases").fields().forEachRemaining(e->{if(aliases.has(e.getKey()))types.put(aliases.path(e.getKey()).asText(),e.getValue().path("type").asText());});result.set("idTypes",types);result.set("installerConventions",Json.MAPPER.valueToTree(conventions));
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
            if(CONSUMED_BASELINE.contains(k))return;
            omitted.add("baseline."+k);
        });
        responsibilitySurvey();
        if(!fixture.path("evidence").isEmpty())notRepresented.add("evidence events/claims/verified chains: an authored evidence entry states hash, namespace and times only (no event kind, claim or canonical scope); the chain is built by the case's own evidence commands");
        for(var it=fixture.path("actors").fields();it.hasNext();) {var e=it.next();
            for(var s=e.getValue().path("grant").path("scope").fieldNames();s.hasNext();){String k=s.next();if(GRANT_METADATA.contains(k)||scopeKind(k)!=null)continue;
                if(UNREPRESENTED_SCOPE.contains(k)){String note="grantScope "+k+" (no product grant scope kind; grant installed without it)";if(!notRepresented.contains(note))notRepresented.add(note);}omitted.add("grantScope "+k+" ("+e.getKey()+")");}
        }
    }

    /**
     * Baseline lists that restate alias attributes are merged into the alias entries (authored alias keys win):
     * items (baseUnit, fractionDigits -> decimalPlaces), places (kind), lots (itemAlias, expiry -> expiresAt), documents
     * (path, mediaType, sha256), priorEntities (per-alias state, quantity, revision, ...), baseline.salesOrder and
     * baseline.allocations / baseline.allocation (an unaliased allocation binds to the fixture's only Allocation alias).
     */
    private void normalizeAliases() {
        ObjectNode all=(ObjectNode)fixture.path("aliases");JsonNode b=fixture.path("baseline");
        for(String key:List.of("items","places","lots","documents"))for(JsonNode e:b.path(key))if(e.hasNonNull("alias")&&all.has(e.path("alias").asText()))merge((ObjectNode)all.path(e.path("alias").asText()),e);
        for(JsonNode e:b.path("items"))if(e.has("fractionDigits")&&all.path(e.path("alias").asText()) instanceof ObjectNode a&&!a.has("decimalPlaces"))a.set("decimalPlaces",e.path("fractionDigits"));
        for(JsonNode e:b.path("lots"))if(e.has("expiry")&&all.path(e.path("alias").asText()) instanceof ObjectNode a&&!a.has("expiresAt"))a.set("expiresAt",e.path("expiry"));
        for(var it=b.path("priorEntities").fields();it.hasNext();){var e=it.next();if(all.path(e.getKey()) instanceof ObjectNode a&&e.getValue().isObject())merge(a,e.getValue());}
        JsonNode so=b.path("salesOrder");
        if(so.isObject()&&so.hasNonNull("alias")) {
            if(!all.has(so.path("alias").asText()))all.set(so.path("alias").asText(),Json.object().put("type","SalesOrder"));
            merge((ObjectNode)all.path(so.path("alias").asText()),so);
            if(so.hasNonNull("lineAlias")){String line=so.path("lineAlias").asText();if(!all.has(line))all.set(line,Json.object().put("type","SalesOrderLine"));var l=(ObjectNode)all.path(line);merge(l,so);l.put("salesOrderAlias",so.path("alias").asText());l.remove("lineAlias");l.put("type","SalesOrderLine");}
        }
        for(JsonNode e:b.path("allocations"))if(e.hasNonNull("alias")){if(!all.has(e.path("alias").asText()))all.set(e.path("alias").asText(),Json.object().put("type","Allocation"));merge((ObjectNode)all.path(e.path("alias").asText()),e);}
        JsonNode one=b.path("allocation");
        if(one.isObject()&&!one.isEmpty()) {
            List<String> allocs=new ArrayList<>();all.fields().forEachRemaining(e->{if(e.getValue().path("type").asText().equals("Allocation"))allocs.add(e.getKey());});
            String target=one.hasNonNull("alias")?one.path("alias").asText():allocs.size()==1?allocs.get(0):null;
            if(target==null){target="baseline-allocation";all.set(target,Json.object().put("type","Allocation"));conventions.add("baseline.allocation installed under synthetic alias "+target);}
            if(!all.has(target))all.set(target,Json.object().put("type","Allocation"));
            var a=(ObjectNode)all.path(target);merge(a,one);
            if(!a.has("segmentAlias")&&one.hasNonNull("scopeAlias"))a.set("segmentAlias",one.path("scopeAlias"));
            if(!a.has("workAlias")&&one.hasNonNull("responsibleWorkAlias"))a.set("workAlias",one.path("responsibleWorkAlias"));
            if(!a.has("state")&&one.hasNonNull("status"))a.set("state",one.path("status"));
        }
        // An alias added above needs an installation id.
        all.fieldNames().forEachRemaining(a->{if(!aliases.has(a))aliases.put(a,UUID.randomUUID().toString());});
    }
    private static void merge(ObjectNode into,JsonNode from){from.fields().forEachRemaining(f->{if(!f.getKey().equals("alias")&&!into.has(f.getKey()))into.set(f.getKey(),f.getValue());});}
    private int revision(JsonNode entry){return entry.hasNonNull("revision")?entry.path("revision").asInt():fixture.path("baseline").path("entityRevisions").path("initial").asInt(1);}
    /**
     * Fixture responsibilities name the owner/supervisor of a scope. For a Work scope they are the installed Work's
     * owner/supervisor (works() reads them). nextAction/nextCheckAt and scopes without a Work have no product row unless
     * an obligation kind is authored, so they are reported as not represented.
     */
    private void responsibilitySurvey() {
        Set<String> works=new HashSet<>();for(String k:List.of("works","work")){JsonNode v=fixture.path("baseline").path(k);if(v.isArray())v.forEach(w->works.add(w.path("alias").asText()));else if(v.isObject())works.add(v.path("alias").asText());}
        int unrepresented=0;
        for(JsonNode r:fixture.path("responsibilities")){String w=r.path("scope").path("workAlias").asText(null);if(w!=null&&works.contains(w))continue;unrepresented++;}
        if(!fixture.path("responsibilities").isEmpty())notRepresented.add("responsibility nextAction/nextCheckAt (no Work-level product column; owner/supervisor of an installed Work scope are installed on the Work)");
        if(unrepresented>0)notRepresented.add("responsibilities without an installed Work scope ("+unrepresented+"): no product row without an obligation kind");
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
        // The stable request owner is the installed principal itself (plan §7.3: server-verified requester continuity, not a
        // token/RPC/Run id), so case observations that name the actor as stableRequestOwnerId see the same identity.
        insert("mulino_identity_Actors",row("organizationId",org,"ID",id,"kind",kind,"stableRequestOwner",id,"createdAt",asOf,"recordedAt",asOf));
        if(conventions.stream().noneMatch(x->x.startsWith("installed actors use their actor id")))conventions.add("installed actors use their actor id as stableRequestOwner");
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
            // User decision 2026-10-09: a grant naming several dimensions authorizes only when every dimension matches (the
            // product's all-of rule), so a grant is installed as written whatever its scopeComposition. Only the probe flag
            // (unionGrantDimensions, ACTUAL_RELAX_WIRE) installs one grant per dimension kind, as a diagnostic.
            String composition=grant.path("scopeComposition").asText("ALL_DIMENSIONS");
            if(!Set.of("PER_DIMENSION","ALL_DIMENSIONS").contains(composition))throw new IllegalArgumentException("Unknown grant scopeComposition "+composition);
            Map<String,List<String[]>> groups=new LinkedHashMap<>();
            for(String[] sc:scopes)groups.computeIfAbsent(unionGrantDimensions?sc[0]:"ALL",k->new ArrayList<>()).add(sc);
            if(groups.size()>1)conventions.add("grant of "+alias+" split into "+groups.size()+" single-dimension grants (probe unionGrantDimensions)");
            String boundGrant=grantAlias(alias,grant.path("delegatorAlias").asText(alias));
            for(var group:groups.values()) {
                String grantId=boundGrant!=null?id(boundGrant):uuid();boundGrant=null;
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
        for(String g:aliasesOfType("Grant"))if(!boundGrantAliases.contains(g))omitted.add("Grant alias "+g+" names no installed actor grant");
        managementAuthorities();
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
            case "segmentAliases","segments","lotAliases","documentAliases","targetAliases","physicalRootAliases","definitionPackages","evidenceIds" -> "TARGET";
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
            if(DRAFT_POLICY_STATES.contains(a.path("state").asText())&&!a.path("content").isObject())continue; // installed as a PolicyDraft by tradeAliases()
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
            itemUnits.put(e.getKey(),unit);
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
            installedLots.add(e.getKey());
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
            if(!s.path("unit").asText().equals(itemUnits.get(item))){omitted.add("segment "+alias+" unit "+s.path("unit").asText()+" differs from item "+item+" base unit (product FK forbids)");continue;}
            if(lot!=null&&!installedLots.contains(lot)){omitted.add("segment "+alias+" lot "+lot+" not installed");continue;}
            OffsetDateTime from=s.hasNonNull("validFrom")?time(s.path("validFrom").asText()):asOf;
            for(String role:List.of("ownerAlias","custodianAlias")) if(s.hasNonNull(role)) partyActor(orgOf.get(alias),s.path(role).asText());
            insert("mulino_inventory_QuantitySegments",row("organizationId",orgOf.get(alias),"ID",id(alias),"revision",revision(s),"itemId",id(item),"lotId",lot==null?null:id(lot),"identificationStatus",lot==null?"UNKNOWN":"CONFIRMED","quantity",new BigDecimal(s.path("quantity").asText()),"unit",s.path("unit").asText(),
                "placeId",id(place),"controlScope",s.path("physicalScope").asText(alias),"validFrom",from,"mixtureStatus","IDENTIFIED","createdAt",asOf,"recordedAt",asOf,
                "ownerId",s.hasNonNull("ownerAlias")?id(s.path("ownerAlias").asText()):null,"custodianId",s.hasNonNull("custodianAlias")?id(s.path("custodianAlias").asText()):null,"evidenceRef","authored-input:"+Json.required(bundle,"fixtureHash")));
            installedSegments.add(alias);
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
        insert("mulino_work_read_Works",row);installedWorks.add(alias);workItems.put(alias,item);
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
            row.put("currentGoalVersionId",goalId);
            conventions.add("Work "+alias+" goal mapped quantity->targetQuantity, destinationAlias->scope.placeId");
        }
        workTransition(org,alias,row);
    }
    /**
     * S3-1: the product reads a COMMAND Work only through its WorkTransitions snapshot (WorkReadRepository.workHistory), the
     * row every lifecycle command writes. An installed Work gets the same revision snapshot (operation INSTALLED_FIXTURE),
     * recorded at the starting clock, so getWork sees exactly the Works row the installer wrote.
     */
    private void workTransition(String org,String alias,Map<String,Object> work) throws Exception {
        var snapshot=new TreeMap<String,Object>();
        for(String k:List.of("organizationId","ID","revision","createdAt","recordedAt","effectiveAt","itemId","lotId","definitionVersionId","kind","status","ownerId","supervisorId","waitJson","currentGoalVersionId","closeReason","pendingInvalidation","goalRevisionRequired","originalText","conversationRequestId","lifecycleMode"))snapshot.put(k,null);
        work.forEach((k,v)->snapshot.put(k,v instanceof OffsetDateTime t?t.toInstant().toString():v));
        snapshot.putIfAbsent("goalRevisionRequired",false);if(snapshot.get("goalRevisionRequired")==null)snapshot.put("goalRevisionRequired",false);
        String status=String.valueOf(work.get("status"));
        insert("mulino_work_WorkTransitions",row("organizationId",org,"ID",uuid(),"workId",id(alias),"revision",work.get("revision"),"operation","INSTALLED_FIXTURE","previousState",null,"state",status,
            "actorId",work.get("ownerId"),"reason","scenario fixture installation","recordedAt",asOf,"snapshotJson",Json.MAPPER.writeValueAsString(snapshot)));
        if(conventions.stream().noneMatch(x->x.startsWith("installed COMMAND Works carry")))conventions.add("installed COMMAND Works carry one INSTALLED_FIXTURE WorkTransitions snapshot at the starting clock (the product reads COMMAND Works only through transition snapshots)");
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

    // ---- sales orders and allocations --------------------------------------------------------------------------------
    private final Map<String,ObjectNode> lines=new LinkedHashMap<>();
    /**
     * SalesOrder -> Orders + OrderRevisions(revisionNumber 1); SalesOrderLine -> OrderLines on that revision (Step 2
     * round 11 request). A line without quantity takes quantity/unit/destination from its order; item = itemAlias, else the
     * organization's only TradeItem; customer = customerAlias, else the order's, else the organization's only Customer;
     * Work = workAlias/salesWorkAlias, else the order's, else baseline.work. The Work must be installed.
     */
    private void sales() throws Exception {
        Map<String,String> orderWork=new HashMap<>(),orderCustomer=new HashMap<>(),orderRevision=new HashMap<>();
        List<String> orders=aliasesOfType("SalesOrder"),saleLines=aliasesOfType("SalesOrderLine");
        String baselineWork=fixture.path("baseline").path("work").path("alias").asText(null);
        for(String o:orders) {
            JsonNode a=fixture.path("aliases").path(o);String org=orgOf.get(o);
            String work=firstAlias(a,"workAlias","salesWorkAlias");
            if(work==null)for(String l:saleLines){JsonNode la=fixture.path("aliases").path(l);if(o.equals(la.path("salesOrderAlias").asText(o.equals(onlyInOrg("SalesOrder",org))?o:null))){work=firstAlias(la,"workAlias","salesWorkAlias");if(work!=null)break;}}
            if(work==null&&baselineWork!=null&&aliases.has(baselineWork))work=baselineWork;
            String customer=firstAlias(a,"customerAlias");if(customer==null)customer=onlyInOrg("Customer",org);
            if(customer!=null)ensureCustomer(org,customer);
            if(work==null||!installedWork(work)||customer==null){if(referencedByLine(o,saleLines))omitted.add("SalesOrder "+o+" (work/customer not resolvable)");else conventions.add("SalesOrder alias "+o+" has no line or work; not installed");continue;}
            insert("mulino_trade_sales_Orders",row("organizationId",org,"ID",id(o),"revision",1,"currentRevision",1,"workId",id(work),"createdAt",asOf,"recordedAt",asOf,"effectiveAt",asOf));
            String rev=uuid();insert("mulino_trade_sales_OrderRevisions",row("organizationId",org,"ID",rev,"revision",1,"orderId",id(o),"workId",id(work),"customerId",id(customer),"revisionNumber",1,"reason","authored fixture sales order","createdAt",asOf,"recordedAt",asOf,"effectiveAt",asOf));
            orderWork.put(o,work);orderCustomer.put(o,customer);orderRevision.put(o,rev);
        }
        for(String l:saleLines) {
            JsonNode a=fixture.path("aliases").path(l);String org=orgOf.get(l);
            String order=firstAlias(a,"salesOrderAlias","orderAlias");if(order==null)order=onlyInOrg("SalesOrder",org);
            if(order==null||!orderRevision.containsKey(order)){
                // A line without an installed order: an order of its own (same work/customer).
                String work=firstAlias(a,"workAlias","salesWorkAlias");if(work==null&&baselineWork!=null&&aliases.has(baselineWork))work=baselineWork;
                String customer=firstAlias(a,"customerAlias");if(customer==null)customer=onlyInOrg("Customer",org);
                if(work==null||!installedWork(work)||customer==null||!a.hasNonNull("quantity")){omitted.add("SalesOrderLine "+l+" (order/work/customer/quantity not resolvable)");continue;}
                ensureCustomer(org,customer);
                order="order-of-"+l;aliases.put(order,uuid());orgOf.put(order,org);
                insert("mulino_trade_sales_Orders",row("organizationId",org,"ID",id(order),"revision",1,"currentRevision",1,"workId",id(work),"createdAt",asOf,"recordedAt",asOf,"effectiveAt",asOf));
                String rev=uuid();insert("mulino_trade_sales_OrderRevisions",row("organizationId",org,"ID",rev,"revision",1,"orderId",id(order),"workId",id(work),"customerId",id(customer),"revisionNumber",1,"reason","authored fixture sales line","createdAt",asOf,"recordedAt",asOf,"effectiveAt",asOf));
                orderWork.put(order,work);orderCustomer.put(order,customer);orderRevision.put(order,rev);conventions.add("SalesOrderLine "+l+" installed with its own synthetic order");
            }
            JsonNode o=fixture.path("aliases").path(order);
            String quantity=a.path("quantity").asText(o.path("quantity").asText(null)),unit=a.path("unit").asText(o.path("unit").asText(null));
            String destination=firstAlias(a,"destinationAlias");if(destination==null)destination=firstAlias(o,"destinationAlias");
            String item=firstAlias(a,"itemAlias");if(item==null)item=firstAlias(o,"itemAlias");if(item==null)item=onlyInOrg("TradeItem",org);
            String work=firstAlias(a,"workAlias","salesWorkAlias");if(work==null)work=orderWork.get(order);
            String customer=firstAlias(a,"customerAlias");if(customer==null)customer=orderCustomer.get(order);
            if(customer!=null)ensureCustomer(org,customer);
            if(quantity==null||unit==null||item==null||work==null||customer==null||!installedWork(work)){omitted.add("SalesOrderLine "+l+" (quantity/unit/item/work not resolvable)");continue;}
            if(!a.hasNonNull("quantity"))conventions.add("SalesOrderLine "+l+" quantity/unit/destination from its order "+order);
            if(destination!=null&&!fixture.path("aliases").path(destination).path("type").asText().equals("Place")){String place=onlyPlaceOfKind(org,"CUSTOMER");conventions.add("SalesOrderLine "+l+" destination "+destination+" is a "+fixture.path("aliases").path(destination).path("type").asText()+", not a Place; "+(place==null?"no CUSTOMER place":"the organization's only CUSTOMER place "+place)+" used");destination=place;}
            if(destination==null)destination=onlyPlaceOfKind(org,"CUSTOMER");
            if(destination==null){omitted.add("SalesOrderLine "+l+" (destination not resolvable)");continue;}
            // OrderLines columns the fixtures never state are NOT NULL in the product schema (price, currency, dueAt, endpoint,
            // terms). They get explicit synthetic placeholders recorded as an installer convention; no case asserts them.
            JsonNode money=a.path("monetaryReference");
            String due=a.path("dueAt").asText(o.path("dueAt").asText(null));
            var r=row("organizationId",org,"ID",id(l),"revision",1,"orderId",id(order),"revisionId",orderRevision.get(order),"workId",id(work),"customerId",id(customer),"itemId",id(item),"quantity",new BigDecimal(quantity),"unit",unit,
                "destinationId",id(destination),"price",new BigDecimal(money.path("price").asText(money.path("amount").asText("0"))),"currency",money.path("currency").asText("XXX"),"dueAt",due!=null?time(due):asOf.plusDays(30),
                "deliveryEndpoint",a.path("endpoint").asText(o.path("endpoint").asText("DELIVERED")),"qualityTerms","fixture-unspecified","packageTerms","fixture-unspecified","createdAt",asOf,"recordedAt",asOf,"effectiveAt",asOf);
            if(!syntheticLineTerms){syntheticLineTerms=true;conventions.add("SalesOrderLine terms not authored (price 0 / currency XXX unless monetaryReference, dueAt asOf+30d unless authored, endpoint DELIVERED unless authored, quality/package terms 'fixture-unspecified') installed as synthetic placeholders");}
            insert("mulino_trade_sales_OrderLines",r);
            var installed=Json.object();installed.put("work",work).put("customer",customer).put("unit",unit).put("item",item);lines.put(l,installed);
        }
    }
    /**
     * Allocation aliases with authored facts -> SegmentAllocations: segment, quantity/unit, state (default EXECUTABLE),
     * startQuantity as declared (absent = no coordinate), revision (alias, else entityRevisions.initial, else 1), the
     * sales line (orderLineAlias, else the only line of its workAlias, else the organization's only line), action SELL,
     * pickedAt as declared (must not be after asOf). pickedByAlias has no product column (the product records only pickedAt).
     */
    private void allocations() throws Exception {
        for(String alias:aliasesOfType("Allocation")) {
            JsonNode a=fixture.path("aliases").path(alias);String org=orgOf.get(alias);
            if(!a.hasNonNull("segmentAlias")&&!a.hasNonNull("quantity")){conventions.add("Allocation alias "+alias+" has no authored facts; not installed");continue;}
            String segment=firstAlias(a,"segmentAlias","scopeAlias");
            String line=firstAlias(a,"orderLineAlias","salesLineAlias");
            if(line==null){List<String> c=new ArrayList<>();for(var e:lines.entrySet())if(a.hasNonNull("workAlias")&&a.path("workAlias").asText().equals(e.getValue().path("work").asText())&&orgOf.get(e.getKey()).equals(org))c.add(e.getKey());if(c.size()==1)line=c.get(0);}
            if(line==null){List<String> c=new ArrayList<>();for(String l:lines.keySet())if(orgOf.get(l).equals(org))c.add(l);if(c.size()==1)line=c.get(0);}
            if(segment==null||line==null||!lines.containsKey(line)||!a.hasNonNull("quantity")){omitted.add("Allocation "+alias+" (segment/sales line/quantity not resolvable)");continue;}
            JsonNode l=lines.get(line);
            String state=a.path("state").asText(a.path("status").asText("EXECUTABLE"));
            if(state.equals("NOT_CREATED")){conventions.add("Allocation "+alias+" declared NOT_CREATED; not installed");continue;}
            if(!Set.of("EXECUTABLE","SUSPENDED","REPLACED","CONSUMED","RELEASED").contains(state)){omitted.add("Allocation "+alias+" state "+state+" outside the product allocation states");continue;}
            if(!installedSegments.contains(segment)){omitted.add("Allocation "+alias+" segment "+segment+" not installed");continue;}
            OffsetDateTime picked=a.hasNonNull("pickedAt")?time(a.path("pickedAt").asText()):null;
            if(picked!=null&&picked.isAfter(asOf)){omitted.add("Allocation "+alias+" pickedAt after the starting clock");continue;}
            var r=row("organizationId",org,"ID",id(alias),"revision",revision(a),"rootId",id(segment),"segmentId",id(segment),"orderLineId",id(line),"quantity",new BigDecimal(a.path("quantity").asText()),"unit",a.path("unit").asText(l.path("unit").asText()),
                "state",state,"action","SELL","customerId",id(l.path("customer").asText()),"workId",id(a.hasNonNull("workAlias")&&aliases.has(a.path("workAlias").asText())?a.path("workAlias").asText():l.path("work").asText()),
                "startQuantity",a.hasNonNull("startQuantity")?new BigDecimal(a.path("startQuantity").asText()):null,"pickedAt",picked,"commandId",uuid(),"createdAt",asOf,"recordedAt",asOf);
            insert("mulino_inventory_SegmentAllocations",r);
            if(!syntheticAllocationCommand){syntheticAllocationCommand=true;conventions.add("fixture allocations carry a synthetic commandId (NOT NULL column; no CommandRecord row exists for an authored allocation)");}
            if(a.hasNonNull("pickedByAlias"))notRepresented.add("Allocation "+alias+" pickedByAlias (the product records pickedAt only)");
        }
    }
    private boolean syntheticLineTerms,syntheticAllocationCommand;
    private final Set<String> installedSegments=new HashSet<>(),installedLots=new HashSet<>();
    private final Map<String,String> itemUnits=new HashMap<>();
    /** A sales customer alias that is not a Customer (e.g. an Organization or Place standing for the buyer) gets a Customer row with the same id. */
    private void ensureCustomer(String org,String alias) throws SQLException {
        if(fixture.path("aliases").path(alias).path("type").asText().equals("Customer")||!ensured.add(org+":C:"+id(alias)))return;
        insert("mulino_trade_sales_Customers",row("organizationId",org,"ID",id(alias),"name",fixture.path("aliases").path(alias).path("name").asText(alias),"revision",1,"createdAt",asOf,"recordedAt",asOf,"effectiveAt",asOf));
        conventions.add("sales customer "+alias+" ("+fixture.path("aliases").path(alias).path("type").asText()+") installed as a Customer row with the same id");
    }
    private String onlyPlaceOfKind(String org,String kind){String found=null;for(String a:aliasesOfType("Place"))if(org.equals(orgOf.get(a))&&kind.equals(fixture.path("aliases").path(a).path("kind").asText())){if(found!=null)return null;found=a;}return found;}
    private List<String> aliasesOfType(String type){List<String> out=new ArrayList<>();fixture.path("aliases").fields().forEachRemaining(e->{if(e.getValue().path("type").asText().equals(type))out.add(e.getKey());});return out;}
    private String onlyInOrg(String type,String org){String found=null;for(String a:aliasesOfType(type))if(org.equals(orgOf.get(a))){if(found!=null)return null;found=a;}return found;}
    private boolean referencedByLine(String order,List<String> saleLines){for(String l:saleLines)if(order.equals(fixture.path("aliases").path(l).path("salesOrderAlias").asText(null)))return true;return !saleLines.isEmpty();}
    private final Set<String> installedWorks=new HashSet<>();
    private boolean installedWork(String alias){return installedWorks.contains(alias);}


    // ---- trade, governance and evidence aliases (S3-2) ---------------------------------------------------------------
    static final Set<String> DRAFT_POLICY_STATES=Set.of("DRAFT","IN_REVIEW","APPROVED","PUBLISHED");
    private final Map<String,String> workItems=new HashMap<>();
    private final Set<String> installedDocs=new HashSet<>();
    /** Installed purchase lines: alias -> {work,item,destination,unit,quantity,price,currency}. */
    private final Map<String,ObjectNode> purchaseLines=new LinkedHashMap<>();
    private final Set<String> boundGrantAliases=new HashSet<>();
    private boolean syntheticTradeTerms;
    /**
     * Authored trade/governance aliases (plan §6 and §7.2 nouns) become the product rows their getObject/verb reads use.
     * Facts the fixture states are copied; NOT NULL columns it never states get explicit synthetic placeholders recorded as
     * installer conventions. Every install is all-or-omitted: an alias whose required relation (Work, item, segment, place,
     * document, sales/purchase line) is not installed is listed in omittedFacts instead of being guessed further.
     */
    private void tradeAliases() throws Exception {
        for(String a:aliasesOfType("PurchaseProposal"))purchaseProposal(a);
        for(String a:aliasesOfType("PurchaseOrderLine"))purchaseOrder(null,a);
        for(String a:aliasesOfType("PurchaseOrder"))purchaseOrder(a,null);
        for(String a:aliasesOfType("Invoice"))invoice(a);
        Set<String> investigations=new HashSet<>();
        for(String a:aliasesOfType("Recall"))recall(a,investigations);
        for(String a:aliasesOfType("Investigation"))if(!investigations.contains(a))investigation(a,null);
        for(String a:aliasesOfType("Shipment"))shipment(a);
        for(String a:aliasesOfType("CargoScope"))if(!installedCargo.contains(a))omitted.add("CargoScope "+a+" without an installed Shipment");
        for(String a:aliasesOfType("RegulatoryProcedure"))regulatory(a);
        for(String a:aliasesOfType("ReconciliationCase"))reconciliation(a);
        for(String a:aliasesOfType("PolicyVersion"))policyDraft(a);
        if(syntheticTradeTerms)conventions.add("trade aliases: NOT NULL terms the fixture does not state are synthetic placeholders (price 0 / currency XXX unless authored, dueAt asOf+30d, endpoint ARRIVED, quantityMode CUMULATIVE_EVENT, channel/carrier 'fixture-unspecified', source namespace scenario-fixture)");
    }
    private JsonNode facts(String alias){return fixture.path("aliases").path(alias);}
    private String recorder(String org){if(aliases.has("supervisor")&&org.equals(orgOf.get("supervisor")))return id("supervisor");return firstActor(org);}
    private String human(String org,String... preferred){for(String p:preferred)if(p!=null&&aliases.has(p)&&org.equals(orgOf.get(p))&&Set.of("Human").contains(facts(p).path("type").asText("Human")))return p;return aliases.has("supervisor")&&org.equals(orgOf.get("supervisor"))?"supervisor":null;}
    private String workFor(JsonNode a,String org){
        String w=firstAlias(a,"workAlias","logisticsWorkAlias","responsibleWorkAlias");if(w!=null)return installedWorks.contains(w)?w:null;
        String baseline=fixture.path("baseline").path("work").path("alias").asText(null);if(baseline!=null&&installedWorks.contains(baseline)&&org.equals(orgOf.get(baseline)))return baseline;
        String only=null;for(String x:installedWorks)if(org.equals(orgOf.get(x))){if(only!=null)return null;only=x;}return only;
    }
    private String itemFor(JsonNode a,String org,String work){String i=firstAlias(a,"itemAlias");if(i!=null)return itemUnits.containsKey(i)?i:null;if(work!=null&&workItems.containsKey(work))return workItems.get(work);String only=onlyInOrg("TradeItem",org);return only!=null&&itemUnits.containsKey(only)?only:null;}
    /** The segment a scoped fact names, else the organization's only installed segment of that item with the same quantity/unit. */
    private String segmentFor(JsonNode a,String org,String item) {
        String s=firstAlias(a,"rootSegmentAlias","segmentAlias","scopeSegmentAlias","physicalScopeAlias");if(s!=null)return installedSegments.contains(s)?s:null;
        String found=null;for(var e:segmentEntries().entrySet()){if(!installedSegments.contains(e.getKey())||!org.equals(orgOf.get(e.getKey())))continue;JsonNode v=e.getValue();
            if(item!=null&&!item.equals(v.path("itemAlias").asText(item)))continue;if(a.hasNonNull("quantity")&&new BigDecimal(a.path("quantity").asText()).compareTo(new BigDecimal(v.path("quantity").asText("0")))!=0)continue;if(found!=null)return null;found=e.getKey();}
        return found;
    }
    private String document(JsonNode a,String org){String d=firstAlias(a,"evidenceAlias","documentAlias","basisDocumentAlias");if(d!=null)return installedDocs.contains(d)?d:null;if(installedDocs.contains("DOC")&&org.equals(orgOf.get("DOC")))return "DOC";for(String x:installedDocs)if(org.equals(orgOf.get(x)))return x;return null;}
    private String internalPlace(String org,String except){List<String> c=new ArrayList<>();for(String p:aliasesOfType("Place"))if(org.equals(orgOf.get(p))&&"INTERNAL_STORAGE".equals(facts(p).path("kind").asText())&&!p.equals(except))c.add(p);Collections.sort(c);return c.isEmpty()?null:c.get(0);}
    private static boolean hex64(String v){return v!=null&&v.matches("[0-9a-f]{64}");}
    private String supplier(JsonNode a,String org) throws SQLException {
        String s=firstAlias(a,"supplierAlias");if(s==null)s=onlyInOrg("Supplier",org);if(s!=null)return id(s);
        String key=org+":S:synthetic";String id=aliases.has(key)?aliases.path(key).asText():null;if(id!=null)return id;id=uuid();aliases.put(key,id);orgOf.put(key,org);
        insert("mulino_trade_purchase_Suppliers",row("organizationId",org,"ID",id,"name","scenario synthetic supplier","revision",1,"createdAt",asOf,"recordedAt",asOf,"effectiveAt",asOf));
        conventions.add("purchase facts without a Supplier alias use one synthetic Supplier row per organization");return id;
    }
    /** ProposalRevisions terms; null when a required relation is not installed. */
    private ObjectNode proposalTerms(String alias,JsonNode a,String org) throws Exception {
        String work=workFor(a,org),item=itemFor(a,org,work),destination=firstAlias(a,"destinationAlias","placeAlias");
        if(destination==null){JsonNode goal=fixture.path("baseline").path("work").path("goal");if(work!=null&&work.equals(fixture.path("baseline").path("work").path("alias").asText()))destination=firstAlias(goal,"destinationAlias");}
        if(work==null||item==null||destination==null||!a.hasNonNull("quantity")){omitted.add(facts(alias).path("type").asText()+" "+alias+" (work/item/destination/quantity not resolvable)");return null;}
        var t=Json.object();t.put("work",work).put("item",item).put("destination",destination).put("quantity",a.path("quantity").asText()).put("unit",a.path("unit").asText(itemUnits.get(item)));
        if(!a.hasNonNull("price")&&!a.hasNonNull("unitPrice")||!a.hasNonNull("currency")||!a.hasNonNull("dueAt"))syntheticTradeTerms=true;
        t.put("price",a.path("price").asText(a.path("unitPrice").asText("0"))).put("currency",a.path("currency").asText("XXX")).put("dueAt",a.hasNonNull("dueAt")?time(a.path("dueAt").asText()).toInstant().toString():asOf.plusDays(30).toInstant().toString());
        t.put("endpoint",a.path("endpoint").asText("ARRIVED")).put("quantityMode",a.path("quantityMode").asText("CUMULATIVE_EVENT"));
        return t;
    }
    private void proposalRows(String org,String proposal,ObjectNode t,JsonNode a,String status,int revision) throws Exception {
        String supplier=supplier(a,org);
        var canonical=new TreeMap<String,Object>();t.fields().forEachRemaining(f->canonical.put(f.getKey(),f.getValue().asText()));canonical.put("proposalId",proposal);canonical.put("revision",String.valueOf(revision));canonical.put("supplierId",supplier);
        String hash=hex64(a.path("canonicalHash").asText(null))?a.path("canonicalHash").asText():sha(Json.MAPPER.writeValueAsBytes(canonical));
        insert("mulino_trade_purchase_Proposals",row("organizationId",org,"ID",proposal,"workId",id(t.path("work").asText()),"currentRevision",revision,"status",status,"createdAt",asOf,"revision",1,"recordedAt",asOf,"effectiveAt",asOf));
        insert("mulino_trade_purchase_ProposalRevisions",row("organizationId",org,"ID",uuid(),"proposalId",proposal,"revision",revision,"workId",id(t.path("work").asText()),"itemId",id(t.path("item").asText()),"supplierId",supplier,"destinationId",id(t.path("destination").asText()),
            "quantity",new BigDecimal(t.path("quantity").asText()),"unit",t.path("unit").asText(),"price",new BigDecimal(t.path("price").asText()),"currency",t.path("currency").asText(),"dueAt",time(t.path("dueAt").asText()),"endpoint",t.path("endpoint").asText(),"quantityMode",t.path("quantityMode").asText(),
            "proposalHash",hash,"reason","authored fixture proposal","recordedAt",asOf,"createdAt",asOf,"effectiveAt",asOf));
        t.put("proposalHash",hash).put("supplierId",supplier);
    }
    /** PurchaseProposal IN_REVIEW/PROPOSED -> Proposals(PROPOSED) + its one ProposalRevision (proposalRevision, else revision, else 1). */
    private void purchaseProposal(String alias) throws Exception {
        JsonNode a=facts(alias);String org=orgOf.get(alias);String state=a.path("state").asText("PROPOSED");
        if(!Set.of("PROPOSED","IN_REVIEW").contains(state)){omitted.add("PurchaseProposal "+alias+" state "+state+" (only an unapproved proposal is installable without an authored approval)");return;}
        ObjectNode t=proposalTerms(alias,a,org);if(t==null)return;
        proposalRows(org,id(alias),t,a,"PROPOSED",a.path("proposalRevision").asInt(a.path("revision").asInt(1)));
        if(state.equals("IN_REVIEW"))conventions.add("PurchaseProposal "+alias+" IN_REVIEW installed as product status PROPOSED (the product has no separate review state before approvePurchase)");
    }
    /**
     * PurchaseOrder / PurchaseOrderLine -> an ORDERED proposal, its APPROVE decision, the dispatched order and one line. The
     * approval, order channel and outbox id are synthetic: the fixture states the order, not the approval/dispatch history.
     * A line alias takes its terms from baseline.historicalFacts.purchase when that names the line (T19).
     */
    private void purchaseOrder(String orderAlias,String lineAlias) throws Exception {
        String alias=orderAlias!=null?orderAlias:lineAlias;ObjectNode a=(ObjectNode)facts(alias).deepCopy();String org=orgOf.get(alias);
        JsonNode history=fixture.path("baseline").path("historicalFacts").path("purchase");
        if(lineAlias!=null&&lineAlias.equals(history.path("lineAlias").asText(null))){history.fields().forEachRemaining(f->{if(!a.has(f.getKey()))a.set(f.getKey(),f.getValue());});
            if(!a.has("price")&&history.has("unitPrice"))a.set("price",history.path("unitPrice"));
            if(history.has("received"))notRepresented.add("PurchaseOrderLine "+lineAlias+" historical receipt "+history.path("received").asText()+" (ReceiptCredits need a canonical receipt occurrence the fixture does not install)");}
        if(orderAlias!=null){for(String l:aliasesOfType("PurchaseOrderLine"))if(orderAlias.equals(facts(l).path("orderAlias").asText(null))){conventions.add("PurchaseOrder "+orderAlias+" installed with its line alias "+l);return;}}
        if(lineAlias!=null&&purchaseLines.containsKey(lineAlias))return;
        ObjectNode t=proposalTerms(alias,a,org);if(t==null)return;
        String proposal=uuid(),approval=uuid(),order=orderAlias!=null?id(orderAlias):firstAlias(a,"orderAlias")!=null?id(firstAlias(a,"orderAlias")):uuid(),line=lineAlias!=null?id(lineAlias):uuid();
        String approver=human(org,firstAlias(a,"approverAlias"),"manager");if(approver==null){omitted.add("PurchaseOrder "+alias+" without a human approver alias");return;}
        proposalRows(org,proposal,t,a,"ORDERED",1);
        insert("mulino_trade_purchase_Approvals",row("organizationId",org,"ID",approval,"proposalId",proposal,"proposalRevision",1,"proposalHash",t.path("proposalHash").asText(),"approverId",id(approver),"policyHash",sha(("scenario-fixture-policy:"+alias).getBytes()),"decision","APPROVE","conditionsJson","[]",
            "decidedAt",asOf,"validUntil",asOf.plusDays(1),"revision",1,"createdAt",asOf,"recordedAt",asOf,"effectiveAt",asOf));
        update("UPDATE mulino_trade_purchase_Proposals SET currentApprovalId=? WHERE organizationId=? AND ID=?",approval,org,proposal);
        insert("mulino_trade_purchase_Orders",row("organizationId",org,"ID",order,"proposalId",proposal,"proposalRevision",1,"proposalHash",t.path("proposalHash").asText(),"approvalId",approval,"channel","fixture-unspecified","externalOperationId","scenario-fixture:"+alias,"outboxId",uuid(),"createdAt",asOf,"conditionAssessmentJson","{\"decision\":\"UNCONDITIONAL\"}","revision",a.path("revision").asInt(1),"recordedAt",asOf,"effectiveAt",asOf));
        String state=a.path("state").asText("DISPATCH_PENDING"),status=switch(state){case "ACCEPTED","SUPPLIER_ACCEPTED"->"SUPPLIER_ACCEPTED";case "REJECTED","SUPPLIER_REJECTED"->"SUPPLIER_REJECTED";default->"DISPATCH_PENDING";};
        insert("mulino_trade_purchase_OrderLines",row("organizationId",org,"ID",line,"orderId",order,"proposalId",proposal,"proposalRevision",1,"workId",id(t.path("work").asText()),"itemId",id(t.path("item").asText()),"destinationId",id(t.path("destination").asText()),
            "quantity",new BigDecimal(t.path("quantity").asText()),"unit",t.path("unit").asText(),"status",status,"revision",1,"createdAt",asOf,"recordedAt",asOf,"effectiveAt",asOf));
        if(status.startsWith("SUPPLIER_"))notRepresented.add("PurchaseOrder "+alias+" supplier "+state+" (line status only: a SupplierReply row needs a verified canonical occurrence the fixture does not state)");
        conventions.add("PurchaseOrder "+alias+" installed as an ORDERED proposal, a synthetic APPROVE decision by "+approver+", a dispatched order (channel fixture-unspecified) and line status "+status);
        purchaseLines.put(lineAlias!=null?lineAlias:alias,t.put("line",line).put("lineAlias",lineAlias!=null?lineAlias:alias));
    }
    /** Invoice -> settlement Invoices on the purchase line it names (else the only purchase line, else the only sales line). */
    private void invoice(String alias) throws Exception {
        JsonNode a=facts(alias);String org=orgOf.get(alias);
        String line=firstAlias(a,"lineAlias","purchaseLineAlias","salesLineAlias","orderLineAlias");String scope=null,work=null,item=null;
        List<String> purchase=new ArrayList<>();for(var e:purchaseLines.entrySet())if(org.equals(orgOf.get(e.getKey())))purchase.add(e.getKey());
        List<String> sale=new ArrayList<>();for(String l:lines.keySet())if(org.equals(orgOf.get(l)))sale.add(l);
        if(line==null&&purchase.size()==1)line=purchase.get(0);if(line==null&&purchase.isEmpty()&&sale.size()==1)line=sale.get(0);
        if(line!=null&&purchaseLines.containsKey(line)){scope="PURCHASE";work=purchaseLines.get(line).path("work").asText();item=purchaseLines.get(line).path("item").asText();}
        else if(line!=null&&lines.containsKey(line)){scope="SALE";work=lines.get(line).path("work").asText();item=lines.get(line).path("item").asText(null);}
        String doc=document(a,org);
        if(scope==null||item==null||doc==null||!a.hasNonNull("quantity")||!a.hasNonNull("amount")&&!a.hasNonNull("originalAmount")){omitted.add("Invoice "+alias+" (line/item/document/quantity/amount not resolvable)");return;}
        BigDecimal q=new BigDecimal(a.path("quantity").asText()),amount=new BigDecimal(a.path("amount").asText(a.path("originalAmount").asText()));
        BigDecimal unitPrice=a.hasNonNull("unitPrice")?new BigDecimal(a.path("unitPrice").asText()):amount.divide(q,12,java.math.RoundingMode.UNNECESSARY);
        String lineId=scope.equals("PURCHASE")?purchaseLines.get(line).path("line").asText():id(line);String kind=a.path("kind").asText(a.path("invoiceKind").asText("COMMERCIAL"));
        insert("mulino_trade_settlement_Invoices",row("organizationId",org,"ID",id(alias),"revision",a.path("revision").asInt(1),"createdAt",asOf.toLocalDateTime(),"recordedAt",asOf.toLocalDateTime(),"effectiveAt",asOf.toLocalDateTime(),"workId",id(work),"itemId",id(item),"lineId",lineId,"referenceId",uuid(),
            "scopeKind",scope,"invoiceKind",kind,"sourceNamespace","scenario-fixture","sourceKey",alias+"-"+id(alias),"sourceVersion","1","sourceHash",sha(("scenario-fixture-invoice:"+alias).getBytes()),"evidenceId",id(doc),"quantity",q,"unit",a.path("unit").asText(),
            "unitPrice",unitPrice,"originalAmount",amount,"currency",a.path("currency").asText(),"taxReferenceOnly",kind.equals("DOMESTIC_TAX")));
        conventions.add("Invoice "+alias+" installed on "+scope+" line "+line+" with evidence "+doc+" and a synthetic referenceId/source key (the fixture states amounts only)");
    }
    private final Set<String> installedCargo=new HashSet<>();
    /**
     * Recall -> the product Recall noun is the recall Investigation; a Recall naming investigationAlias shares that
     * Investigation's id (alias rebound) and its scope version is the RecallScope (version, hash, root segment, range).
     */
    private void recall(String alias,Set<String> investigations) throws Exception {
        JsonNode a=facts(alias);String inv=firstAlias(a,"investigationAlias");
        if(inv!=null&&"Investigation".equals(facts(inv).path("type").asText())){aliases.put(alias,id(inv));investigations.add(inv);conventions.add("Recall "+alias+" bound to its investigation "+inv+" (the product Recall noun is the Investigation; the recall scope version is its RecallScope)");investigation(inv,a);}
        else investigation(alias,a);
    }
    private void investigation(String alias,JsonNode recall) throws Exception {
        ObjectNode a=(ObjectNode)facts(alias).deepCopy();if(recall!=null)recall.fields().forEachRemaining(f->{if(!a.has(f.getKey()))a.set(f.getKey(),f.getValue());});
        String org=orgOf.get(alias),work=workFor(a,org),item=itemFor(a,org,work),segment=segmentFor(a,org,item);
        if(work==null||item==null||segment==null||!a.hasNonNull("quantity")){omitted.add(facts(alias).path("type").asText()+" "+alias+" (work/item/segment/quantity not resolvable)");return;}
        String lot=firstAlias(a,"lotAlias");if(lot==null){JsonNode s=segmentEntries().get(segment);lot=s==null?null:firstAlias(s,"lotAlias");}
        String owner=human(org,firstAlias(a,"ownerAlias")),supervisor=human(org,firstAlias(a,"supervisorAlias"));if(owner==null)owner=supervisor;
        if(owner==null){omitted.add("Investigation "+alias+" owner/supervisor not resolvable");return;}
        String scope=recall!=null?uuid():null;
        insert("mulino_trade_recall_Investigations",row("organizationId",org,"ID",id(alias),"revision",a.path("revision").asInt(1),"createdAt",asOf,"recordedAt",asOf,"occurredAt",asOf,"itemId",id(item),"lotId",lot==null?null:id(lot),"segmentId",id(segment),"workId",id(work),
            "quantity",new BigDecimal(a.path("quantity").asText()),"unit",a.path("unit").asText(itemUnits.get(item)),"startQuantity",new BigDecimal(a.path("startQuantity").asText("0")),"basisEvidenceId",document(a,org)==null?null:id(document(a,org)),"impactState",a.path("impactState").asText("CANDIDATE"),
            "status",scope==null?"INVESTIGATING":"PROPOSED","currentScopeId",scope,"ownerId",id(owner),"supervisorId",id(supervisor==null?owner:supervisor),"nextAction",a.path("nextAction").asText("authored fixture recall review"),"nextCheckAt",a.hasNonNull("nextCheckAt")?time(a.path("nextCheckAt").asText()):asOf.plusHours(1)));
        if(scope!=null){
            int version=a.path("scopeVersion").asInt(1);String hash=hex64(a.path("scopeHash").asText(null))?a.path("scopeHash").asText():sha(("scenario-fixture-recall-scope:"+alias+":"+version).getBytes());
            insert("mulino_trade_recall_Scopes",row("organizationId",org,"ID",scope,"revision",1,"createdAt",asOf,"recordedAt",asOf,"investigationId",id(alias),"version",version,"scopeHash",hash,"rootSegmentId",id(segment),"startQuantity",new BigDecimal(a.path("startQuantity").asText("0")),
                "quantity",new BigDecimal(a.path("quantity").asText()),"unit",a.path("unit").asText(itemUnits.get(item)),"itemId",id(item),"lotId",lot==null?null:id(lot),"workId",id(work),"reason","authored fixture recall scope"));
        }
    }
    /** Shipment -> Shipments(state as authored) + one ShipmentCargo per CargoScope alias naming it (else one synthetic cargo from the shipment's own quantity). */
    private void shipment(String alias) throws Exception {
        JsonNode a=facts(alias);String org=orgOf.get(alias);
        String origin=firstAlias(a,"originAlias");if(origin==null)origin=internalPlace(org,null);String destination=firstAlias(a,"destinationAlias");if(destination==null)destination=internalPlace(org,origin);
        List<String> cargo=new ArrayList<>();for(String cs:aliasesOfType("CargoScope"))if(alias.equals(facts(cs).path("shipmentAlias").asText(null))||cs.equals(a.path("cargoScopeAlias").asText(null)))cargo.add(cs);
        String rec=recorder(org);
        if(origin==null||destination==null||rec==null){omitted.add("Shipment "+alias+" (origin/destination/recorder not resolvable)");return;}
        String work=firstAlias(a,"workAlias");if(work!=null&&!installedWorks.contains(work))work=null;
        insert("mulino_trade_shipment_Shipments",row("organizationId",org,"ID",id(alias),"revision",a.path("revision").asInt(1),"createdAt",asOf,"recordedAt",asOf,"recordedBy",rec,"originId",id(origin),"destinationId",id(destination),"carrierRef","fixture-unspecified","workId",work==null?null:id(work),"state",a.path("state").asText("PLANNED")));
        if(!a.hasNonNull("originAlias"))conventions.add("Shipment "+alias+" origin/destination not authored: the organization's first two INTERNAL_STORAGE places "+origin+" -> "+destination);
        if(cargo.isEmpty()&&a.hasNonNull("quantity")){String item=itemFor(a,org,null);if(item!=null){insert("mulino_trade_shipment_ShipmentCargo",row("organizationId",org,"ID",uuid(),"revision",1,"createdAt",asOf,"recordedAt",asOf,"recordedBy",rec,"shipmentId",id(alias),"itemId",id(item),"plannedQuantity",new BigDecimal(a.path("quantity").asText()),"unit",a.path("unit").asText(itemUnits.get(item))));}}
        for(String cs:cargo){JsonNode c0=facts(cs);String item=itemFor(c0,org,null);if(item==null||!c0.hasNonNull("quantity")){omitted.add("CargoScope "+cs+" (item/quantity not resolvable)");continue;}
            insert("mulino_trade_shipment_ShipmentCargo",row("organizationId",org,"ID",id(cs),"revision",c0.path("revision").asInt(1),"createdAt",asOf,"recordedAt",asOf,"recordedBy",rec,"shipmentId",id(alias),"itemId",id(item),"plannedQuantity",new BigDecimal(c0.path("quantity").asText()),"unit",c0.path("unit").asText(itemUnits.get(item))));installedCargo.add(cs);}
    }
    /** RegulatoryProcedure -> Procedures + one ProcedureVersions head (status = authored state) on the physical segment it scopes. */
    private void regulatory(String alias) throws Exception {
        JsonNode a=facts(alias);String org=orgOf.get(alias),work=workFor(a,org),item=itemFor(a,org,work),segment=segmentFor(a,org,item),rec=recorder(org);
        if(work==null||item==null||segment==null||rec==null){omitted.add("RegulatoryProcedure "+alias+" (work/item/segment not resolvable)");return;}
        String lot=firstAlias(a,"lotAlias");if(lot==null){JsonNode s=segmentEntries().get(segment);lot=s==null?null:firstAlias(s,"lotAlias");}String version=uuid();
        insert("mulino_trade_regulatory_Procedures",row("organizationId",org,"ID",id(alias),"revision",a.path("revision").asInt(1),"createdAt",asOf,"recordedAt",asOf,"recordedBy",rec,"itemId",id(item),"lotId",lot==null?null:id(lot),"physicalScopeId",id(segment),"workId",id(work)));
        insert("mulino_trade_regulatory_ProcedureVersions",row("organizationId",org,"ID",version,"revision",1,"createdAt",asOf,"recordedAt",asOf,"recordedBy",rec,"procedureId",id(alias),"itemId",id(item),"lotId",lot==null?null:id(lot),"physicalScopeId",id(segment),"workId",id(work),"status",a.path("state").asText("PREPARED"),"occurredAt",asOf));
        update("UPDATE mulino_trade_regulatory_Procedures SET currentVersionId=? WHERE organizationId=? AND ID=?",version,org,id(alias));
        if(a.hasNonNull("policyVersion"))notRepresented.add("RegulatoryProcedure "+alias+" policyVersion "+a.path("policyVersion").asText()+" (no regulatory Policies row is authored; the procedure is installed without policyId)");
    }
    /** ReconciliationCase OPEN -> an UNVERIFIED Reconciliations row over a synthetic item Claim on the fixture document. */
    private void reconciliation(String alias) throws Exception {
        JsonNode a=facts(alias);String org=orgOf.get(alias),item=itemFor(a,org,workFor(a,org)),doc=document(a,org),rec=recorder(org);
        String owner=human(org,firstAlias(a,"ownerAlias","intakeOwnerAlias")),supervisor=human(org,firstAlias(a,"supervisorAlias"));if(supervisor==null)supervisor=owner;
        if(item==null||doc==null||rec==null||owner==null){omitted.add("ReconciliationCase "+alias+" (item/document/owner not resolvable)");return;}
        String state=a.path("state").asText("OPEN");if(!Set.of("OPEN","UNVERIFIED").contains(state)){omitted.add("ReconciliationCase "+alias+" state "+state+" (only an open case is installable without an authored decision basis)");return;}
        String profile=profile(org,"scenario-fixture",rec,rec,"fixture-v1"),claim=uuid(),event=uuid(),zone=fixture.path("clock").path("timezone").asText("UTC"),payload="{\"source\":\"scenario-fixture-reconciliation-case\",\"alias\":\""+alias+"\"}";
        insert("mulino_evidence_Events",row("ID",event,"organizationId",org,"revision",1,"createdAt",asOf,"recordedAt",asOf,"recordedBy",rec,"subjectKind","ITEM","subjectId",id(item),"itemId",id(item),"effectiveFrom",asOf,"timeZone",zone,"timePrecision","SECOND","valueState","UNKNOWN",
            "kind","SCENARIO_FIXTURE_RECONCILIATION","sourceNamespace","scenario-fixture","sourceProfileId",profile,"externalEventId","scenario-fixture:"+alias+":"+id(alias),"sourceVersion","1","payloadHash",sha(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8)),"payload",payload));
        insert("mulino_evidence_Claims",row("sourceProfileId",profile,"ID",claim,"organizationId",org,"revision",1,"createdAt",asOf,"recordedAt",asOf,"recordedBy",rec,"subjectKind","ITEM","subjectId",id(item),"itemId",id(item),"effectiveFrom",asOf,"timeZone",zone,"timePrecision","SECOND","valueState","UNKNOWN","eventId",event,"documentVersionId",id(doc),"assertion",payload));
        insert("mulino_evidence_Reconciliations",row("ID",id(alias),"organizationId",org,"revision",a.path("revision").asInt(1),"createdAt",asOf,"recordedAt",asOf,"recordedBy",rec,"subjectKind","ITEM","subjectId",id(item),"itemId",id(item),"sourceProfileId",profile,"claimId",claim,"basisDocumentId",id(doc),"policyVersion","fixture-v1",
            "decision","UNVERIFIED","reason","authored fixture reconciliation case "+state,"sourceIdentity","scenario-fixture:"+alias,"effectiveFrom",asOf,"intakeOwnerId",id(owner),"supervisorId",id(supervisor),"nextAction",a.path("nextAction").asText("authored fixture reconciliation review"),"nextCheckAt",a.hasNonNull("nextCheckAt")?time(a.path("nextCheckAt").asText()):asOf.plusHours(1)));
        conventions.add("ReconciliationCase "+alias+" ("+state+") installed as an UNVERIFIED reconciliation over a synthetic item event/claim on "+doc);
    }
    /** PolicyVersion with a draft state and no content -> PolicyDrafts (IN_REVIEW -> DRAFT: the product's approvePolicy takes a DRAFT). */
    private void policyDraft(String alias) throws Exception {
        JsonNode a=facts(alias);String state=a.path("state").asText();if(!DRAFT_POLICY_STATES.contains(state)||a.path("content").isObject())return;
        String org=orgOf.get(alias);var content=Json.object();content.put("source","scenario-fixture-policy-draft").put("alias",alias);
        String hash=hex64(a.path("hash").asText(null))?a.path("hash").asText():FixtureInstaller.contentHash(content);
        insert("mulino_governance_PolicyDrafts",row("organizationId",org,"ID",id(alias),"kind",a.path("kind").asText("SCENARIO_FIXTURE_DRAFT"),"version",a.path("version").asText(alias),"content",content.toString(),"contentHash",hash,"source","scenario fixture","regressionEvidence","scenario fixture",
            "effectiveFrom",a.hasNonNull("effectiveFrom")?time(a.path("effectiveFrom").asText()):asOf,"legallyRestrictive",false,"fixtureOnly",true,"status",Set.of("APPROVED","PUBLISHED").contains(state)?"APPROVED":"DRAFT","revision",a.path("revision").asInt(1)));
        conventions.add("PolicyVersion "+alias+" ("+state+") installed as a PolicyDrafts row with the authored hash; content is a synthetic placeholder (the fixture states state/hash only), kind SCENARIO_FIXTURE_DRAFT unless authored");
        if(Set.of("APPROVED","PUBLISHED").contains(state))notRepresented.add("PolicyVersion "+alias+" "+state+" installed as an APPROVED draft without a PolicyApprovals row (approver not authored; the product's policy states are DRAFT/APPROVED/ACTIVE/RETIRED)");
    }
    /** baseline.managementAuthorities -> ManagementAuthorities (plan §7.2 management capability of a human; scope ORGANIZATION or an installed alias). */
    private void managementAuthorities() throws SQLException {
        for(JsonNode m:fixture.path("baseline").path("managementAuthorities")){
            String actor=firstAlias(m,"actorAlias");String kind=m.path("scopeKind").asText("ORGANIZATION");String scope=firstAlias(m,"scopeAlias");
            if(actor==null||!m.hasNonNull("capabilityId")||!m.hasNonNull("validFrom")||!m.hasNonNull("validUntil")){omitted.add("managementAuthority without actor/capability/validity");continue;}
            String org=org(actor);String scopeId=scope!=null?id(scope):kind.equals("ORGANIZATION")?org:null;if(scopeId==null){omitted.add("managementAuthority "+actor+" "+m.path("capabilityId").asText()+" scope not resolvable");continue;}
            insert("mulino_identity_ManagementAuthorities",row("organizationId",org,"ID",uuid(),"actorId",id(actor),"capabilityId",m.path("capabilityId").asText(),"scopeKind",kind,"scopeId",scopeId,"validFrom",time(m.path("validFrom").asText()),"validUntil",time(m.path("validUntil").asText()),
                "revokedAt",m.hasNonNull("revokedAt")?time(m.path("revokedAt").asText()):null,"revision",m.path("revision").asInt(1)));
        }
    }
    /** The Grant alias whose facts name this actor (and its delegator): the actor's installed grant takes the alias id. */
    private String grantAlias(String actor,String delegator) {
        for(String g:aliasesOfType("Grant")){JsonNode a=facts(g);if(boundGrantAliases.contains(g)||!actor.equals(a.path("actorAlias").asText(null)))continue;if(a.hasNonNull("delegatorAlias")&&!a.path("delegatorAlias").asText().equals(delegator))continue;boundGrantAliases.add(g);conventions.add("Grant alias "+g+" bound to the installed grant of "+actor);return g;}
        return null;
    }

    // ---- evidence documents ----------------------------------------------------------------------------------------
    private final Map<String,String> profiles=new HashMap<>();
    /** baseline.sourceProfiles: one SourceProfile per namespace; intake owner = reconciliationOwnerAlias, else recordActorAlias. */
    private void sourceProfiles() throws SQLException {
        for(JsonNode p:fixture.path("baseline").path("sourceProfiles")) {
            String namespace=p.path("namespace").asText(null);if(namespace==null){omitted.add("sourceProfile without namespace");continue;}
            String owner=firstAlias(p,"reconciliationOwnerAlias","intakeOwnerAlias","recordActorAlias");
            String supervisor=firstAlias(fixture.path("baseline"),"supervisorAlias");if(supervisor==null&&aliases.has("supervisor"))supervisor="supervisor";if(supervisor==null)supervisor=owner;
            if(owner==null){omitted.add("sourceProfile "+namespace+" without resolvable owner");continue;}
            String org=org(owner);profile(org,namespace,id(owner),id(supervisor),p.path("policyVersion").asText("fixture-v1"));
        }
    }
    private String profile(String org,String namespace,String owner,String supervisor,String policy) throws SQLException {
        String key=org+":"+namespace,id=profiles.get(key);if(id!=null)return id;id=uuid();profiles.put(key,id);
        insert("mulino_evidence_SourceProfiles",row("organizationId",org,"ID",id,"revision",1,"createdAt",asOf,"recordedAt",asOf,"recordedBy",owner,"namespace",namespace,"policyVersion",policy,"intakeOwnerId",owner,"supervisorId",supervisor,
            "nextAction",fixture.path("baseline").path("nextAction").asText("authored fixture source review"),"nextCheckAt",fixture.path("baseline").hasNonNull("nextCheckAt")?time(fixture.path("baseline").path("nextCheckAt").asText()):asOf.plusDays(1)));
        return id;
    }
    /**
     * Evidence documents. An authored evidence entry is installed as its DocumentVersion. When the alias carries canonical
     * fixtureContent (or baseline.documents names a repository file), the original bytes are written to the backend blob
     * store and the document is AVAILABLE: fixtureContent aliases (keys ending in Alias naming a fixture alias) are resolved
     * to installed IDs under the same key without the Alias suffix plus "Id" (receivingCustodianAlias -> receivingCustodianId),
     * so the stored sha256 is the hash of the resolved canonical bytes, not the authored hash. Recording time: declared
     * recordedAt at or before the fixture knownAt -> the starting clock (asOf); later -> the declared instant (late-known).
     */
    private void evidence() throws Exception {
        String supervisor=aliases.has("supervisor")?"supervisor":null;
        Map<String,JsonNode> entries=new LinkedHashMap<>();
        for(JsonNode e:fixture.path("evidence")){String alias=e.path("alias").asText(null);if(alias==null||!aliases.has(alias)){omitted.add("evidence entry without alias");continue;}entries.put(alias,e);}
        for(var it=fixture.path("aliases").fields();it.hasNext();){var e=it.next();if(e.getValue().path("type").asText().equals("DocumentVersion")&&!entries.containsKey(e.getKey())&&(e.getValue().has("fixtureContent")||e.getValue().has("path")))entries.put(e.getKey(),Json.object());}
        for(var entry:entries.entrySet()) {
            String alias=entry.getKey();JsonNode e=entry.getValue();JsonNode a=fixture.path("aliases").path(alias);
            String org=orgOf.get(alias),namespace=e.path("sourceNamespace").asText(a.path("fixtureContent").path("sourceNamespace").asText("synthetic"));
            String recorder=supervisor!=null&&org.equals(org(supervisor))?id(supervisor):firstActor(org);
            if(recorder==null){omitted.add("evidence "+alias+" without recorder actor");continue;}
            String profile=profile(org,namespace,recorder,recorder,"fixture-v1");
            String item=a.path("fixtureContent").hasNonNull("itemAlias")&&aliases.has(a.path("fixtureContent").path("itemAlias").asText())?id(a.path("fixtureContent").path("itemAlias").asText()):firstOfType(org,"TradeItem");
            if(item==null){omitted.add("evidence "+alias+" without item subject");continue;}
            OffsetDateTime recorded=asOf;
            if(e.hasNonNull("recordedAt")){OffsetDateTime declared=time(e.path("recordedAt").asText());if(declared.isAfter(knownAt))recorded=declared;}
            byte[] bytes=null;String media="application/octet-stream";
            if(a.path("fixtureContent").isObject()){bytes=CANONICAL.writeValueAsBytes(Json.MAPPER.treeToValue(resolved((ObjectNode)a.path("fixtureContent").deepCopy()),Object.class));media="application/json";}
            else if(a.hasNonNull("path")){Path file=root.resolve(a.path("path").asText()).normalize();if(!file.startsWith(root.normalize())){omitted.add("evidence "+alias+" path escapes repository");continue;}bytes=java.nio.file.Files.readAllBytes(file);media=a.path("mediaType").asText(media);
                String authored=e.path("sha256").asText(a.path("sha256").asText(""));if(!authored.isEmpty()&&!authored.equals(sha(bytes))){omitted.add("evidence "+alias+" file hash differs from authored sha256");continue;}}
            String sha=bytes!=null?sha(bytes):e.path("sha256").asText();
            if(!sha.matches("[0-9a-f]{64}")){omitted.add("evidence "+alias+" sha256 not hex");continue;}
            String blob=null;
            if(bytes!=null) {
                if(blobRoot==null){omitted.add("evidence "+alias+" original bytes (no isolated blob root configured)");}
                else{blob=uuid();Path objects=blobRoot.toAbsolutePath().normalize().resolve("objects");if(!java.nio.file.Files.isDirectory(objects))throw new IllegalStateException("Backend blob object store missing");
                    Path file=objects.resolve(blob);java.nio.file.Files.write(file,bytes,java.nio.file.StandardOpenOption.CREATE_NEW);writtenBlobs.add(file);
                    java.nio.file.Files.setPosixFilePermissions(file,java.nio.file.attribute.PosixFilePermissions.fromString("r--------"));}
            }
            var doc=row("organizationId",org,"ID",id(alias),"revision",1,"createdAt",recorded,"recordedAt",recorded,"recordedBy",recorder,"subjectKind","ITEM","subjectId",item,"itemId",item,"sha256",sha,"byteLength",bytes==null?0L:(long)bytes.length,"mediaType",media,
                "sourceNamespace",namespace,"sourceProfileId",profile,"sourceReference",e.path("externalEventId").asText(a.path("fixtureContent").path("externalEventId").asText(null)),"availability",blob!=null?"AVAILABLE":"UNKNOWN",
                "provenance",blob!=null?"{\"source\":\"scenario-installer-resolved-canonical-content\"}":"{\"source\":\"scenario-installer-authored-hash-only\"}");
            if(blob!=null)doc.put("blobId",blob);
            insert("mulino_evidence_DocumentVersions",doc);installedDocs.add(alias);
            if(recorded!=asOf)conventions.add("evidence "+alias+" recorded at its declared late-known instant "+recorded);
        }
    }
    /** fixtureContent alias fields resolved to installed IDs (field fooAlias naming an alias adds fooId). */
    private JsonNode resolved(ObjectNode content) {
        for(String key:new ArrayList<>(iterable(content))) {
            JsonNode v=content.path(key);
            if(key.endsWith("Alias")&&v.isTextual()&&aliases.has(v.asText()))content.put(key.substring(0,key.length()-5)+"Id",id(v.asText()));
            else if(v.isObject())resolved((ObjectNode)v);
        }
        return content;
    }
    private static final com.fasterxml.jackson.databind.ObjectMapper CANONICAL=new com.fasterxml.jackson.databind.ObjectMapper().configure(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS,true);
    private static String sha(byte[] bytes) throws Exception {return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));}
    private static List<String> iterable(ObjectNode n){var out=new ArrayList<String>();n.fieldNames().forEachRemaining(out::add);return out;}
    private String firstAlias(JsonNode node,String... keys){for(String k:keys){String v=node.path(k).asText(null);if(v!=null&&aliases.has(v))return v;}return null;}

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
