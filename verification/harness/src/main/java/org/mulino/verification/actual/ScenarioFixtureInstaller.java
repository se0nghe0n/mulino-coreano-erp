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
        "Place","Customer","Supplier","QuantitySegment","Work","DocumentVersion","DefinitionVersion","PolicyVersion","SalesOrder","SalesOrderLine","Allocation");
    /** Baseline keys whose content is merged into alias attributes or installed by a dedicated step. */
    static final Set<String> CONSUMED_BASELINE=Set.of("segments","works","work","items","places","lots","organizations","priorEntities","documents","sourceProfiles","entityRevisions","salesOrder","allocations","allocation");
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
                actors();definitionsAndPolicies();items();places();parties();lots();segments();works();sourceProfiles();evidence();sales();allocations();grants();
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
            // grant.scopeComposition (Step 2 round 12): PER_DIMENSION -> one grant per dimension kind (same actions,
            // delegator, validity, revision), so the authored dimension lists are alternatives; absent or ALL_DIMENSIONS ->
            // one grant whose dimensions intersect (product semantics). Probe unionGrantDimensions splits every grant.
            String composition=grant.path("scopeComposition").asText("ALL_DIMENSIONS");
            if(!Set.of("PER_DIMENSION","ALL_DIMENSIONS").contains(composition))throw new IllegalArgumentException("Unknown grant scopeComposition "+composition);
            boolean perDimension=unionGrantDimensions||composition.equals("PER_DIMENSION");
            Map<String,List<String[]>> groups=new LinkedHashMap<>();
            for(String[] sc:scopes)groups.computeIfAbsent(perDimension?sc[0]:"ALL",k->new ArrayList<>()).add(sc);
            if(groups.size()>1)conventions.add("grant of "+alias+" installed as "+groups.size()+" single-dimension grants ("+(composition.equals("PER_DIMENSION")?"scopeComposition PER_DIMENSION":"probe unionGrantDimensions")+")");
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
        insert("mulino_work_read_Works",row);installedWorks.add(alias);
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
            var installed=Json.object();installed.put("work",work).put("customer",customer).put("unit",unit);lines.put(l,installed);
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
            insert("mulino_evidence_DocumentVersions",doc);
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
