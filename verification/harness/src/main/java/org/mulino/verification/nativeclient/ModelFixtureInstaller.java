package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.mulino.verification.Json;
import org.mulino.verification.actual.ActualConfiguration;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.math.*;
import java.util.*;

/** Strict, disposable, atomic model-fact installer. It never reads expected intent, oracle, or corpus answers. */
public final class ModelFixtureInstaller {
    public static final String VERSION="model-fixture-installer-v1";
    private final Path root,blobRoot;
    private final ActualConfiguration config;
    public ModelFixtureInstaller(Path root,ActualConfiguration config,Path blobRoot) {
        this.root=root.toAbsolutePath().normalize();this.config=config;this.blobRoot=blobRoot==null?null:blobRoot.toAbsolutePath().normalize();
    }
    public static final class MappingException extends IllegalArgumentException {
        private final JsonNode inventory;
        MappingException(JsonNode inventory){super("MODEL_FIXTURE_INPUTS_REQUIRED: "+inventory.path("issues"));this.inventory=inventory.deepCopy();}
        public JsonNode inventory(){return inventory.deepCopy();}
    }
    /** Static mapping inventory does not contact a database or install anything. */
    public static ObjectNode inventory(JsonNode fixture,JsonNode inputs,String fixtureHash) {
        var plan=plan(fixture,inputs,fixtureHash);return inventory(plan);
    }
    private static ModelFixturePlan plan(JsonNode fixture,JsonNode inputs,String hash) {
        if(!fixture.isObject()||!inputs.isObject())throw new IllegalArgumentException("Typed fixture and installation input object required");
        if(!hash.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("Exact fixture SHA-256 required");
        if(!inputs.path("schemaVersion").asText().equals("model-installation-inputs-v1")||!inputs.path("fixtureSha256").asText().equals(hash))throw new IllegalArgumentException("Installation layer version/hash mismatch");
        Set<String> keys=Set.of("schemaVersion","fixtureSha256","records","documentBytes","originalInputProvided","originalAuthorityClaimed","operatingApprovalClaimed","authorityCorrection","typedReferences","logicalCustody","completionLedger","versionTranslations","templateArtifacts","grantContents","quantityCorrection","staticClaims","priorReceipt","physicalCorrection","readCompletion","receiptAuthority");
        inputs.fieldNames().forEachRemaining(k->{if(!keys.contains(k))throw new IllegalArgumentException("Unknown installation layer field: "+k);});
        for(String k:List.of("originalInputProvided","originalAuthorityClaimed","operatingApprovalClaimed"))if(!inputs.path(k).isBoolean()||inputs.path(k).asBoolean())throw new IllegalArgumentException("Explicit false synthetic provenance required: "+k);
        if(inputs.has("physicalCorrection")||inputs.has("priorReceipt")) {
            if(!fixture.path("fixtureId").asText().equals("M50-isolated"))throw new IllegalArgumentException("M50 physical exception cannot apply to another case");
            JsonNode physical=inputs.path("physicalCorrection"),prior=inputs.path("priorReceipt");
            if(!physical.path("version").asText().equals("model-M50-original-reference-only-v1")||!physical.path("originalSnapshot").equals(fixture.at("/baseline/domainFacts/objects/A"))||!physical.path("originalId").equals(fixture.at("/aliases/A"))||physical.path("originalIdentityPreservedInProduct").asBoolean(true)||!physical.path("correctedGiven").asBoolean()||!prior.path("version").asText().equals("model-M50-prior-receipt-v1"))throw new IllegalArgumentException("Exact M50 original reference snapshot and prior receipt required");
            if(!prior.path("commandIdempotencyKey").equals(fixture.at("/baseline/domainFacts/receipt/commandIdempotencyKey"))||!prior.path("originalCanonicalHash").equals(fixture.at("/baseline/domainFacts/receipt/canonicalHash"))||!prior.path("originalCanonicalPayload").equals(fixture.at("/baseline/domainFacts/receipt/canonicalPayload")))throw new IllegalArgumentException("M50 original reference payload changed");
        }
        if(!inputs.path("records").isArray())throw new IllegalArgumentException("Typed installation records required");
        try {
            var normalized=ModelFixtureInputCorrection.apply(fixture,inputs.path("quantityCorrection"),hash);
            var correction=ModelFixtureAuthority.apply(Path.of(System.getProperty("repo.root",".")),normalized.fixture(),inputs.path("authorityCorrection"),hash);
            var plan=new ModelFixturePlan(correction.fixture(),inputs,hash);plan.inputCorrections.addAll(normalized.ledger());plan.authorityCorrections.addAll(correction.ledger());plan.purposeGrantBindings.addAll(correction.purposeBindings());plan.compile();return plan;
        }catch(RuntimeException failure){throw failure;}catch(Exception failure){throw new IllegalArgumentException("Authority correction inputs invalid",failure);}
    }
    private static ObjectNode inventory(ModelFixturePlan plan) {
        var out=Json.object().put("installer",VERSION).put("fixtureSha256",plan.hash).put("installable",plan.issues.isEmpty()).put("originalInputProvided",false).put("originalAuthorityClaimed",false).put("operatingApprovalClaimed",false);
        out.set("inputCorrections",plan.inputCorrections);out.set("staticClaimReceipts",plan.staticClaims);out.set("purposeGrantBindings",plan.purposeGrantBindings);out.set("authorityCorrections",plan.authorityCorrections);out.set("issues",plan.issues);out.set("unverifiedOriginalClaims",plan.warnings);out.set("preservedOriginalClaims",plan.claims);
        var mapping=Json.array();for(var r:plan.rows)mapping.add(Json.object().put("type",r.type()).put("alias",r.alias()).put("id",r.id()).put("sourcePointer",r.pointer()).put("table",ModelFixtureCatalog.KINDS.get(r.type()).table()));
        out.set("mapping",mapping);return out;
    }
    /** The bundle retains the original fixture tree and byte hash. inputs are independently versioned and pinned. */
    public ObjectNode install(JsonNode bundle,JsonNode inputs) throws Exception {
        String hash=Json.required(bundle,"fixtureHash");JsonNode fixture=bundle.path("fixture");verifyOriginal(bundle);
        for(JsonNode artifact:inputs.path("templateArtifacts")){Path path=root.resolve(artifact.path("path").asText()).normalize();if(!path.toRealPath().startsWith(root.toRealPath())||!Json.sha256(path).equals(artifact.path("sha256").asText()))throw new IllegalArgumentException("Pinned published template drift");}
        var plan=plan(fixture,inputs,hash);if(!plan.issues.isEmpty())throw new MappingException(inventory(plan));
        String installation=UUID.randomUUID().toString();var receipts=Json.array();var inverse=Json.array();var snapshot=Json.object();var written=new ArrayList<Path>();
        var rows=materialize(plan);validateTypedReferences(rows,plan);validateDocumentBytes(rows,inputs);validate(rows,plan);
        try(Connection c=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password())) {
            c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                for(var row:ordered(plan)) {
                    var values=rows.get(row.alias());var kind=ModelFixtureCatalog.KINDS.get(row.type());
                    if(row.type().equals("Document"))document(row,values,inputs,written,plan);
                    // A newly installed Work is linked to its goal after both exist.
                    Object currentGoal=values.remove("currentGoalVersionId");Object currentVersion=values.remove("currentVersionId");Object currentApproval=values.remove("currentApprovalId");
                    insert(c,kind.table(),values);
                    if(currentGoal!=null)values.put("currentGoalVersionId",currentGoal);if(currentVersion!=null)values.put("currentVersionId",currentVersion);if(currentApproval!=null)values.put("currentApprovalId",currentApproval);
                    var key=Json.object().put("ID",row.id());if(!row.type().equals("Organization"))key.put("organizationId",plan.org);
                    receipt(receipts,inverse,row.pointer(),row.type(),kind.table(),key,values,row.synthetic());
                }
                for(var row:plan.rows)if(row.type().equals("Work")&&rows.get(row.alias()).containsKey("currentGoalVersionId")) {
                    try(var s=c.prepareStatement("UPDATE mulino_work_read_Works SET currentGoalVersionId=? WHERE organizationId=? AND ID=?")) {s.setObject(1,rows.get(row.alias()).get("currentGoalVersionId"));s.setString(2,plan.org);s.setString(3,row.id());if(s.executeUpdate()!=1)throw new SQLException("Work goal link disappeared");}
                }
                for(var row:plan.rows)if(row.type().equals("RegulatoryProcedure")&&rows.get(row.alias()).containsKey("currentVersionId")) {
                    try(var s=c.prepareStatement("UPDATE mulino_trade_regulatory_Procedures SET currentVersionId=? WHERE organizationId=? AND ID=?")){s.setObject(1,rows.get(row.alias()).get("currentVersionId"));s.setString(2,plan.org);s.setString(3,row.id());if(s.executeUpdate()!=1)throw new SQLException("Regulatory current version link disappeared");}
                }
                for(var row:plan.rows)if(row.type().equals("PurchaseProposal")&&rows.get(row.alias()).containsKey("currentApprovalId")){try(var statement=c.prepareStatement("UPDATE mulino_trade_purchase_Proposals SET currentApprovalId=? WHERE organizationId=? AND ID=?")){statement.setObject(1,rows.get(row.alias()).get("currentApprovalId"));statement.setString(2,plan.org);statement.setString(3,row.id());if(statement.executeUpdate()!=1)throw new SQLException("Purchase approval link missing");}}
                installWorkHistory(c,plan,rows,receipts,inverse);
                for(var row:plan.rows)if(row.type().equals("Principal"))child(c,receipts,inverse,row.pointer(),"AuthorityFence","mulino_identity_AuthorityFences",map("organizationId",plan.org,"actorId",row.id(),"revision",1));
                grantChildren(c,plan,receipts,inverse);syntheticGrantChildren(c,plan,receipts,inverse);
                for(JsonNode record:inputs.path("records"))if(record.path("activate").asBoolean()) {
                    if(!record.path("type").asText().equals("PolicyVersion"))throw new IllegalArgumentException("Only explicit PolicyVersion activation supported");
                    child(c,receipts,inverse,record.path("sourcePointer").asText(),"ActivePolicy","mulino_governance_ActivePolicies",map("organizationId",plan.org,"kind",record.path("fields").path("kind").asText(),"policyId",record.path("id").asText(),"revision",1));
                }
                readback(c,receipts);var checks=checkDeclaredInventory(c,plan);checks.addAll(checkDomainFacts(c,plan));snapshot.set("declaredFactChecks",checks);
                try(var s=c.createStatement();var rs=s.executeQuery("SELECT current_database(),current_schema(),txid_current(),pg_current_snapshot(),version()")){rs.next();snapshot.put("database",rs.getString(1)).put("schema",rs.getString(2)).put("transactionId",rs.getString(3)).put("snapshot",rs.getString(4)).put("postgresqlVersion",rs.getString(5)).put("isolation","SERIALIZABLE");}
                verifyOriginal(bundle);c.commit();
            }catch(Exception failure){c.rollback();for(Path p:written)Files.deleteIfExists(p);throw failure;}
        }
        var result=inventory(plan).put("installationId",installation).put("committed",true).put("fixtureComplete",plan.warnings.isEmpty()).put("businessExecutionClaimed",false).put("acceptanceStatus","NOT_RUN");
        var installedAliases=Json.object();for(var row:plan.rows)installedAliases.put(row.alias(),row.id());result.set("aliasMap",installedAliases);var bindings=Json.object();for(var row:plan.rows)bindings.set(row.alias(),Json.object().put("table",ModelFixtureCatalog.KINDS.get(row.type()).table()).put("id",row.id()));
        inputs.path("typedReferences").fields().forEachRemaining(e->{e.getValue().fields().forEachRemaining(t->{if(BINDING_TYPES.containsKey(t.getKey()))bindings.set(e.getKey()+":"+t.getKey(),Json.object().put("table",ModelFixtureCatalog.KINDS.get(BINDING_TYPES.get(t.getKey())).table()).put("id",t.getValue().asText()));});});
        result.set("bindings",bindings);result.set("originalAliasMap",fixture.path("aliases").deepCopy());result.set("typedReferences",inputs.path("typedReferences").deepCopy());result.set("logicalCustody",inputs.path("logicalCustody").deepCopy());result.set("completionLedger",inputs.path("completionLedger").deepCopy());result.set("versionTranslations",inputs.path("versionTranslations").deepCopy());result.set("clock",fixture.path("clock"));result.put("organizationExternalAlias",plan.org);result.set("organizationExternalAliases",Json.object().put("org",plan.org));
        result.set("transaction",snapshot);result.set("sourceFactReceipts",receipts);result.put("inverseExecutionBoundary","Immutable append-only product rows require restoration of the owned pre-install disposable database snapshot; row inverse entries are exact logical removals, not permission to bypass immutability or delete concurrent work");
        // Array reversal is explicit: ledger consumers must execute the exact reverse insertion order.
        var reverse=Json.array();for(int i=inverse.size()-1;i>=0;i--)reverse.add(inverse.get(i));result.set("inverse",reverse);
        var blobs=Json.array();for(Path path:written)blobs.add(Json.object().put("path",path.toString()).put("sha256",Json.sha256(path)).put("sizeBytes",Files.size(path)));result.set("writtenBlobs",blobs);
        result.set("currentOrganizationLinks",Json.object().put("organizationId",plan.org).put("fixtureSha256",hash).put("installationId",installation));
        if(inputs.has("receiptAuthority"))ModelFixturePriorReceipt.installAuthority(config,fixture,inputs.path("receiptAuthority"),result);
        if(inputs.has("readCompletion"))ModelFixtureReadCompletion.complete(config,fixture,inputs,result);
        if(inputs.has("priorReceipt"))ModelFixturePriorReceipt.complete(root,config,fixture,inputs,result);
        return result;
    }
    /** Called by the integration hook; no original fixture or actor identity normalization occurs. */
    public ObjectNode install(JsonNode bundle) throws Exception {
        String location=System.getProperty("verification.model.installationInputs");
        if(location==null)throw new MappingException(inventory(bundle.path("fixture"),emptyInputs(Json.required(bundle,"fixtureHash")),Json.required(bundle,"fixtureHash")));
        Path file=root.resolve(location).normalize();if(Files.isDirectory(file))file=file.resolve(bundle.path("fixture").path("fixtureId").asText()+".json");
        if(!file.toRealPath().startsWith(root.toRealPath()))throw new IllegalArgumentException("Installation input path escapes repository");
        var result=install(bundle,Json.read(file));result.set("installationInputArtifact",Json.object().put("path",root.relativize(file).toString()).put("sha256",Json.sha256(file)).put("sizeBytes",Files.size(file)));return result;
    }
    public static ObjectNode emptyInputs(String hash) {var n=Json.object().put("schemaVersion","model-installation-inputs-v1").put("fixtureSha256",hash).put("originalInputProvided",false).put("originalAuthorityClaimed",false).put("operatingApprovalClaimed",false);n.set("records",Json.array());n.set("documentBytes",Json.object());return n;}
    private void verifyOriginal(JsonNode bundle) throws Exception {
        String id=bundle.path("fixture").path("fixtureId").asText();if(!id.matches("M[0-9]{2}-isolated")) {
            if(!sha(bundle.path("fixture").toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)).equals(Json.required(bundle,"fixtureHash")))throw new IllegalArgumentException("Focused fixture canonical byte hash mismatch");return;
        }
        Path path=root.resolve("verification/model-binding/cases/"+id.substring(0,3)+"/fixture.json");
        if(!Json.sha256(path).equals(Json.required(bundle,"fixtureHash"))||!Json.read(path).equals(bundle.path("fixture")))throw new IllegalArgumentException("Original fixture tree or byte hash drift");
    }
    private LinkedHashMap<String,LinkedHashMap<String,Object>> materialize(ModelFixturePlan plan) {
        var result=new LinkedHashMap<String,LinkedHashMap<String,Object>>();
        for(var row:plan.rows) {
            var kind=ModelFixtureCatalog.KINDS.get(row.type());var values=new LinkedHashMap<String,Object>();if(!row.type().equals("Organization"))values.put("organizationId",plan.org);values.put("ID",row.id());
            row.fields().fields().forEachRemaining(e->{Character type=kind.fields().get(e.getKey());if(type==null)throw new IllegalArgumentException("Unknown typed field "+row.type()+"."+e.getKey());JsonNode n=e.getValue();if(n.isNull())return;
                Object value=switch(type) {
                    case 'r' -> {String ref=plan.aliases.path(n.asText()).asText(n.asText());UUID.fromString(ref);yield ref;}
                    case 't' -> OffsetDateTime.parse(n.asText());
                    case 'd' -> {if(!n.isTextual())throw new IllegalArgumentException("Decimal string required");var d=new BigDecimal(n.asText());if(d.scale()>12||d.precision()-d.scale()>26)throw new IllegalArgumentException("Exact numeric(38,12) required");yield d;}
                    case 'i' -> {if(!n.isIntegralNumber()||!n.canConvertToInt())throw new IllegalArgumentException("Integer required");yield n.asInt();}
                    case 'l' -> {if(!n.isIntegralNumber()||!n.canConvertToLong())throw new IllegalArgumentException("Integer64 required");yield n.asLong();}
                    case 'b' -> {if(!n.isBoolean())throw new IllegalArgumentException("Boolean required");yield n.asBoolean();}
                    case 'h' -> {if(!n.asText().matches("[0-9a-f]{64}"))throw new IllegalArgumentException("Unprefixed SHA-256 required");yield n.asText();}
                    case 'j' -> {if(!n.isObject()&&!n.isArray())throw new IllegalArgumentException("Typed JSON object/array required");yield n.toString();}
                    default -> {if(!n.isTextual()||n.asText().isBlank())throw new IllegalArgumentException("Nonempty typed string required");yield n.asText();}
                };values.put(e.getKey(),value);
            });
            result.put(row.alias(),values);
        }return result;
    }
    private static List<ModelFixturePlan.Row> ordered(ModelFixturePlan plan) {
        var remaining=new ArrayList<>(plan.rows);var ordered=new ArrayList<ModelFixturePlan.Row>();var pending=new HashSet<String>();for(var r:remaining)pending.add(r.id());
        while(!remaining.isEmpty()) {
            boolean progress=false;
            for(var iterator=remaining.iterator();iterator.hasNext();) {
                var row=iterator.next();var kind=ModelFixtureCatalog.KINDS.get(row.type());boolean blocked=false;
                for(var e:kind.fields().entrySet())if(e.getValue()=='r'&&!Set.of("currentGoalVersionId","dispositionBasisId","restrictionId","cargoScopeId","currentVersionId","currentApprovalId").contains(e.getKey())) {
                    String value=row.fields().path(e.getKey()).asText();value=plan.aliases.path(value).asText(value);
                    if(!value.equals(row.id())&&pending.contains(value)){blocked=true;break;}
                }
                if(!row.type().equals("Organization")&&pending.contains(plan.org))blocked=true;
                if(!blocked){ordered.add(row);pending.remove(row.id());iterator.remove();progress=true;}
            }
            if(!progress)throw new IllegalArgumentException("Typed installation reference cycle: "+pending);
        }return ordered;
    }
    private static final Map<String,String> BINDING_TYPES=Map.of("SalesOrder","SalesOrder","SalesOrderLine","SalesLine","Work","Work","Dispatch","Dispatch","CargoScope","CargoScope","QuantitySegment","QuantitySegment","TransitQuantitySegment","QuantitySegment","ReceiptObservation","ReceiptObservation","CanonicalOccurrence","CanonicalOccurrence","Document","Document");
    private static void validateTypedReferences(Map<String,LinkedHashMap<String,Object>> rows,ModelFixturePlan plan) {
        plan.inputs.path("typedReferences").fields().forEachRemaining(e->{
            String alias=e.getKey();JsonNode binding=e.getValue();
            binding.fields().forEachRemaining(b->{if(BINDING_TYPES.containsKey(b.getKey())) {var record=plan.rows.stream().filter(r->r.id().equals(b.getValue().asText())).findFirst().orElseThrow(()->new IllegalArgumentException("Typed binding row missing"));if(!record.type().equals(BINDING_TYPES.get(b.getKey())))throw new IllegalArgumentException("Typed binding physical kind mismatch");}else if(!Set.of("sourcePointer","originalInputProvided","originalAuthorityClaimed","operatingApprovalClaimed").contains(b.getKey()))throw new IllegalArgumentException("Unknown typed binding kind");});
            if(binding.hasNonNull("SalesOrder")) {
                if(!plan.fixture.path("baseline").path("domainFacts").path("objects").path(alias).path("type").asText().equals("SalesOrder")||!plan.aliases.path(alias).asText().equals(binding.path("SalesOrder").asText()))throw new IllegalArgumentException("Typed original SalesOrder binding mismatch");
                var order=rows.get(alias);if(order==null||!Objects.equals(order.get("workId"),binding.path("Work").asText())||Objects.equals(order.get("ID"),order.get("workId")))throw new IllegalArgumentException("Typed Work must be the distinct linked delivery Work");
                var work=rowById(rows,binding.path("Work").asText());if(!Objects.equals(work.get("kind"),"DELIVERY"))throw new IllegalArgumentException("Typed linked resource is not a delivery Work");
            }
        });
    }
    private void validateDocumentBytes(Map<String,LinkedHashMap<String,Object>> rows,JsonNode inputs) throws Exception {
        for(var row:rows.values())if(row.containsKey("availability")) {
            String alias=rows.entrySet().stream().filter(e->e.getValue()==row).findFirst().orElseThrow().getKey();
            JsonNode descriptor=inputs.path("documentBytes").path(alias);
            if(!descriptor.isMissingNode()) {
                Path path=root.resolve(descriptor.asText()).normalize().toRealPath();
                if(!path.startsWith(root.toRealPath()))throw new IllegalArgumentException("Document path escape");
                requireHash(row,"sha256",Files.readAllBytes(path));row.put("availability","AVAILABLE");
            }else if(!Objects.equals(row.get("availability"),"UNKNOWN"))throw new IllegalArgumentException("AVAILABLE document requires actual matching bytes: "+alias);
        }
    }
    private void validate(Map<String,LinkedHashMap<String,Object>> rows,ModelFixturePlan plan) throws Exception {
        for(JsonNode link:plan.inputs.path("logicalCustody")) {
            Object id=link.path("representativeActorId").asText();var representative=rowById(rows,id);if(!Set.of("HUMAN","EXTERNAL").contains(representative.get("kind")))throw new IllegalArgumentException("Typed custody representative required");
            for(var r:plan.rows)if(Set.of("Grant","CapabilityAssignment","Membership").contains(r.type())&&Objects.equals(rows.get(r.alias()).get("actorId"),id))throw new IllegalArgumentException("Custody representative must have zero authority");
            if(link.path("capabilityCount").asInt(-1)!=0||link.path("grantCount").asInt(-1)!=0)throw new IllegalArgumentException("Explicit zero custody authority required");
        }
        var originals=new HashSet<String>();plan.fixture.path("baseline").path("principalFacts").fieldNames().forEachRemaining(a->originals.add(plan.aliases.path(a).asText()));
        for(var row:plan.rows) {
            var fields=rows.get(row.alias());
            if(row.type().equals("Membership")||row.type().equals("Grant")||row.type().equals("CapabilityAssignment"))if(!((OffsetDateTime)fields.get("validUntil")).isAfter((OffsetDateTime)fields.get("validFrom")))throw new IllegalArgumentException("Invalid authority validity interval");
            if(row.pointer().isEmpty()&&Set.of("Membership","Grant","CapabilityAssignment","ManagementAuthority").contains(row.type())&&originals.contains(fields.get("actorId"))) {
                boolean selectedPurchase=Set.of("M04-isolated","M14-isolated","M45-isolated").contains(plan.fixture.path("fixtureId").asText())&&row.alias().startsWith("synthetic:purchase:approvalAuthority")&&Objects.equals(fields.get("actorId"),plan.aliases.path("manager").asText())&&(row.type().equals("Grant")||Objects.equals(fields.get("capabilityId"),"approvePurchase"));
                boolean selectedResponsibility=Set.of("M03-isolated","M50-isolated").contains(plan.fixture.path("fixtureId").asText())&&row.type().equals("Membership")&&row.alias().startsWith("synthetic:"+plan.fixture.path("fixtureId").asText().substring(0,3)+":responsibilityMembership:")&&(Objects.equals(fields.get("actorId"),plan.aliases.path(plan.fixture.at("/baseline/domainFacts/objects/O1/ownerRef").asText()).asText())||Objects.equals(fields.get("actorId"),plan.aliases.path(plan.fixture.at("/baseline/domainFacts/policy/supervisorRef").asText()).asText()));
                if(!selectedPurchase&&!selectedResponsibility)throw new IllegalArgumentException("Synthetic authority cannot broaden an original principal");
            }
            if(row.type().equals("Event"))requireHash(fields,"payloadHash",fields.get("payload").toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if(row.type().equals("DefinitionVersion")||row.type().equals("PolicyVersion"))requireHash(fields,"contentHash",fields.get("content").toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if(row.type().equals("PurchaseRevision")) {
                var canonical=new TreeMap<String,String>();for(String key:List.of("proposalId","revision","workId","itemId","supplierId","destinationId","quantity","unit","price","currency","dueAt","endpoint","quantityMode")){Object value=fields.get(key);canonical.put(key,value instanceof OffsetDateTime t?t.toInstant().toString():value.toString());}
                requireHash(fields,"proposalHash",Json.MAPPER.writeValueAsBytes(canonical));
            }
            if(row.type().equals("Verification"))verifySourceChain(fields,rows,plan);
        }
    }
    private static void requireHash(Map<String,Object> fields,String key,byte[] bytes) throws Exception {if(!Objects.equals(fields.get(key),sha(bytes)))throw new IllegalArgumentException("Original bytes/hash mismatch: "+key);}
    private static String sha(byte[] bytes) throws Exception {return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));}
    private static Map<String,Object> rowById(Map<String,LinkedHashMap<String,Object>> rows,Object id){return rows.values().stream().filter(r->Objects.equals(r.get("ID"),id)).findFirst().orElseThrow(()->new IllegalArgumentException("Missing typed source chain reference "+id));}
    private void verifySourceChain(Map<String,Object> verification,Map<String,LinkedHashMap<String,Object>> rows,ModelFixturePlan plan) throws Exception {
        for(String field:List.of("sourceMatched","identityMatched","quantityMatched","timeMatched","duplicateChecked","verdict"))if(verification.containsKey(field))throw new IllegalArgumentException("Caller seeded verification outcome: "+field);
        var claim=rowById(rows,verification.get("claimId"));var canonical=rowById(rows,verification.get("canonicalOccurrenceId"));var event=rowById(rows,claim.get("eventId"));var document=rowById(rows,verification.get("basisDocumentId"));
        if(!Objects.equals(claim.get("documentVersionId"),document.get("ID"))||!Objects.equals(document.get("availability"),"AVAILABLE"))throw new IllegalArgumentException("Verification requires matching available source original");
        for(String key:List.of("subjectKind","subjectId","itemId","placeId","workId","effectiveFrom","quantity","unit"))if(!Objects.equals(claim.get(key),canonical.get(key)))throw new IllegalArgumentException("Canonical/claim scope mismatch: "+key);
        if(!Objects.equals(event.get("subjectId"),claim.get("subjectId"))||!Objects.equals(event.get("sourceProfileId"),document.get("sourceProfileId")))throw new IllegalArgumentException("Source event/document identity mismatch");
        String documentAlias=rows.entrySet().stream().filter(e->e.getValue()==document).findFirst().orElseThrow().getKey();
        Path original=root.resolve(plan.inputs.path("documentBytes").path(documentAlias).asText()).normalize().toRealPath();
        JsonNode actualBytes=Json.read(original);
        for(String key:List.of("subjectKind","subjectId","itemId","placeId","workId","effectiveFrom","quantity","unit")) {
            Object value=claim.get(key);if(value==null){if(actualBytes.hasNonNull(key))throw new IllegalArgumentException("Unbound source document field: "+key);continue;}
            boolean matches=actualBytes.hasNonNull(key)&&(value instanceof OffsetDateTime t?OffsetDateTime.parse(actualBytes.path(key).asText()).toInstant().equals(t.toInstant()):value instanceof BigDecimal d?new BigDecimal(actualBytes.path(key).asText()).compareTo(d)==0:actualBytes.path(key).asText().equals(value.toString()));
            if(!matches)throw new IllegalArgumentException("Source bytes do not substantiate claim: "+key);
        }
        if(!Objects.equals(event.get("payload"),actualBytes.toString()))throw new IllegalArgumentException("Actual source bytes differ from event payload");
        var profile=rowById(rows,event.get("sourceProfileId"));if(!Objects.equals(profile.get("namespace"),event.get("sourceNamespace"))||!Objects.equals(profile.get("policyVersion"),verification.get("policyVersion")))throw new IllegalArgumentException("Source profile namespace/policy mismatch");
        var inbox=plan.rows.stream().filter(r->r.type().equals("Inbox")).map(r->rows.get(r.alias())).filter(r->Objects.equals(r.get("eventId"),event.get("ID"))).toList();if(inbox.size()!=1)throw new IllegalArgumentException("Exactly one matching source inbox required");
        for(String key:List.of("sourceProfileId","sourceNamespace","externalEventId","sourceVersion","payloadHash","subjectKind","subjectId"))if(!Objects.equals(inbox.getFirst().get(key),event.get(key)))throw new IllegalArgumentException("Source inbox mismatch: "+key);
        if(!Objects.equals(claim.get("assertion"),actualBytes.toString()))throw new IllegalArgumentException("Claim assertion differs from actual source bytes");
        if(!Objects.equals(event.get("effectiveFrom"),claim.get("effectiveFrom")))throw new IllegalArgumentException("Source event occurrence time mismatch");
        if(plan.rows.stream().filter(r->r.type().equals("CanonicalOccurrence")).filter(r->Objects.equals(rows.get(r.alias()).get("physicalScopeId"),canonical.get("physicalScopeId"))&&Objects.equals(rows.get(r.alias()).get("kind"),canonical.get("kind"))&&Objects.equals(rows.get(r.alias()).get("occurrenceIdentity"),canonical.get("occurrenceIdentity"))).count()!=1)throw new IllegalArgumentException("Duplicate canonical physical scope");
        for(String k:List.of("sourceMatched","identityMatched","quantityMatched","timeMatched","duplicateChecked"))verification.put(k,true);verification.put("verdict","VERIFIED");
    }
    private void document(ModelFixturePlan.Row row,Map<String,Object> values,JsonNode inputs,List<Path> written,ModelFixturePlan plan) throws Exception {
        JsonNode descriptor=inputs.path("documentBytes").path(row.alias());
        if(!descriptor.isMissingNode()) {
            if(!descriptor.isTextual())throw new IllegalArgumentException("documentBytes maps aliases to repository paths");Path file=root.resolve(descriptor.asText()).normalize().toRealPath();if(!file.startsWith(root.toRealPath()))throw new IllegalArgumentException("Document path escape");
            byte[] bytes=Files.readAllBytes(file);requireHash(values,"sha256",bytes);
            if(blobRoot==null||!Files.isDirectory(blobRoot.resolve("objects")))throw new IllegalArgumentException("Isolated existing backend blob root required");
            String blob=UUID.randomUUID().toString();Path target=blobRoot.resolve("objects").resolve(blob);Files.write(target,bytes,StandardOpenOption.CREATE_NEW);written.add(target);Files.setPosixFilePermissions(target,java.nio.file.attribute.PosixFilePermissions.fromString("r--------"));
            values.put("blobId",blob);values.put("byteLength",(long)bytes.length);values.put("availability","AVAILABLE");
        } else if(!Objects.equals(values.get("availability"),"UNKNOWN"))throw new IllegalArgumentException("AVAILABLE document requires actual matching bytes: "+row.alias());
    }
    private static void grantChildren(Connection c,ModelFixturePlan plan,ArrayNode receipts,ArrayNode inverse) throws Exception {
        for(var i=plan.fixture.path("baseline").path("grantFacts").fields();i.hasNext();) {
            var g=i.next();String id=plan.aliases.path(g.getKey()).asText(),p="/baseline/grantFacts/"+g.getKey();
            for(JsonNode action:g.getValue().path("actions"))child(c,receipts,inverse,p,"GrantAction","mulino_identity_GrantActions",map("organizationId",plan.org,"grantId",id,"capabilityId",action.asText()));
            child(c,receipts,inverse,p,"GrantScope","mulino_identity_GrantScopes",map("organizationId",plan.org,"grantId",id,"scopeKind","ORGANIZATION","scopeId",plan.org));
            for(JsonNode alias:g.getValue().path("targetScope"))child(c,receipts,inverse,p,"GrantScope","mulino_identity_GrantScopes",map("organizationId",plan.org,"grantId",id,"scopeKind","TARGET","scopeId",plan.ref(alias.asText(),p)));
            for(JsonNode alias:g.getValue().path("targetScope")) {
                JsonNode binding=plan.inputs.path("typedReferences").path(alias.asText());
                if(binding.hasNonNull("Work"))child(c,receipts,inverse,p,"GrantScope","mulino_identity_GrantScopes",map("organizationId",plan.org,"grantId",id,"scopeKind","TARGET","scopeId",binding.path("Work").asText()));
            }
            var namespaces=new LinkedHashSet<String>();g.getValue().path("sourceNamespaces").forEach(n->namespaces.add(n.asText()));if(g.getValue().hasNonNull("sourceNamespace"))namespaces.add(g.getValue().path("sourceNamespace").asText());
            for(String namespace:namespaces){if(namespace.isBlank()||namespace.length()>36)throw new IllegalArgumentException("Product SOURCE scope supports at most 36 characters");child(c,receipts,inverse,p,"GrantScope","mulino_identity_GrantScopes",map("organizationId",plan.org,"grantId",id,"scopeKind","SOURCE","scopeId",namespace));}
        }
    }
    private static void installWorkHistory(Connection c,ModelFixturePlan plan,Map<String,LinkedHashMap<String,Object>> rows,ArrayNode receipts,ArrayNode inverse) throws Exception {
        for(var work:plan.rows)if(work.type().equals("Work")&&Objects.equals(rows.get(work.alias()).get("lifecycleMode"),"COMMAND")) {
            var snapshot=Json.object();String sql="SELECT * FROM mulino_work_read_Works WHERE organizationId=? AND ID=?";
            try(var statement=c.prepareStatement(sql)){statement.setString(1,plan.org);statement.setString(2,work.id());try(var rs=statement.executeQuery()){if(!rs.next())throw new SQLException("Installed Work missing before initial transition");for(String key:rows.get(work.alias()).keySet()){Object actual=rs.getObject(key);if(actual instanceof Timestamp t)snapshot.put(key,t.toInstant().toString());else if(actual!=null)snapshot.set(key,Json.MAPPER.valueToTree(actual));}}}
            String id=UUID.nameUUIDFromBytes((plan.hash+":initial-work-history:"+work.id()).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
            child(c,receipts,inverse,work.pointer(),"WorkTransition","mulino_work_WorkTransitions",map("organizationId",plan.org,"ID",id,"workId",work.id(),"revision",rows.get(work.alias()).get("revision"),"operation","MODEL_FIXTURE_INSTALL","state",rows.get(work.alias()).get("status"),"actorId",rows.get(work.alias()).get("ownerId"),"reason","Explicit synthetic model fixture initial source snapshot","recordedAt",OffsetDateTime.parse(plan.fixture.at("/clock/knownAt").asText()),"snapshotJson",snapshot.toString()));
        }
    }
    private static void syntheticGrantChildren(Connection c,ModelFixturePlan plan,ArrayNode receipts,ArrayNode inverse) throws Exception {
        for(var iterator=plan.inputs.path("grantContents").fields();iterator.hasNext();) {
            var entry=iterator.next();var row=plan.byAlias.get(entry.getKey());if(row==null||!row.type().equals("Grant")||!row.pointer().isEmpty())throw new IllegalArgumentException("Synthetic grant contents require a new explicit Grant record");
            var definition=entry.getValue();Set<String> keys=Set.of("actions","scope","purpose");definition.fieldNames().forEachRemaining(k->{if(!keys.contains(k))throw new IllegalArgumentException("Unknown synthetic grant field: "+k);});
            Set<String> setup=Set.of("attachEvidence","matchSourceIdentity","recordDispositionBasis","placeHold","prepareRegulatoryProcedure","recordSubmission","recordRegulatoryDecision","verifyLabel");
            var actions=definition.path("actions");if(!actions.isArray()||actions.isEmpty())throw new IllegalArgumentException("Explicit synthetic setup actions required");
            boolean purchase=definition.path("purpose").asText().equals("SYNTHETIC_PURCHASE_SETUP")&&Set.of("M04-isolated","M14-isolated","M45-isolated").contains(plan.fixture.path("fixtureId").asText())&&entry.getKey().equals("synthetic:purchase:approvalAuthority");
            if(!purchase&&!definition.path("purpose").asText().equals("SYNTHETIC_SOURCE_SETUP"))throw new IllegalArgumentException("Synthetic authority purpose required");
            String actor=row.fields().path("actorId").asText();actor=plan.aliases.path(actor).asText(actor);
            for(JsonNode a:actions){if(!(purchase?a.asText().equals("approvePurchase"):setup.contains(a.asText())))throw new IllegalArgumentException("Synthetic source identity cannot gain business-command authority");child(c,receipts,inverse,"/grantContents/"+entry.getKey(),"GrantAction","mulino_identity_GrantActions",map("organizationId",plan.org,"grantId",row.id(),"capabilityId",a.asText()));}
            var purpose=Json.object().put("purpose",definition.path("purpose").asText()).put("grantId",row.id()).put("principalId",actor).put("originalInputProvided",false).put("originalAuthorityClaimed",false).put("operatingApprovalClaimed",false);purpose.set("actions",actions.deepCopy());purpose.set("scope",definition.path("scope").deepCopy());purpose.set("typedGrant",row.fields().deepCopy());plan.purposeGrantBindings.add(purpose);
            for(var scopes=definition.path("scope").fields();scopes.hasNext();) {var scope=scopes.next();if(!Set.of("ORGANIZATION","TARGET","SOURCE","ITEM","PLACE","WORK","SUPPLIER").contains(scope.getKey())||!scope.getValue().isArray()||scope.getValue().isEmpty())throw new IllegalArgumentException("Invalid synthetic setup scope");for(JsonNode id:scope.getValue())child(c,receipts,inverse,"/grantContents/"+entry.getKey(),"GrantScope","mulino_identity_GrantScopes",map("organizationId",plan.org,"grantId",row.id(),"scopeKind",scope.getKey(),"scopeId",plan.aliases.path(id.asText()).asText(id.asText())));}
        }
    }
    private static void child(Connection c,ArrayNode receipts,ArrayNode inverse,String pointer,String type,String table,LinkedHashMap<String,Object> values) throws Exception {insert(c,table,values);var key=new LinkedHashMap<>(values);if(type.equals("WorkTransition")){Object id=key.get("ID");key.clear();key.put("organizationId",values.get("organizationId"));key.put("ID",id);}if(type.equals("AuthorityFence"))key.remove("revision");if(type.equals("ActivePolicy")){key.remove("policyId");key.remove("revision");}receipt(receipts,inverse,pointer,type,table,Json.MAPPER.valueToTree(key),values,true);}
    private static LinkedHashMap<String,Object> map(Object... entries){var m=new LinkedHashMap<String,Object>();for(int i=0;i<entries.length;i+=2)m.put(entries[i].toString(),entries[i+1]);return m;}
    static void insert(Connection c,String table,Map<String,Object> values) throws SQLException {
        String sql="INSERT INTO "+table+"("+String.join(",",values.keySet())+") VALUES("+String.join(",",Collections.nCopies(values.size(),"?"))+")";
        try(var statement=c.prepareStatement(sql)){int i=1;for(Object value:values.values())statement.setObject(i++,value);if(statement.executeUpdate()!=1)throw new SQLException("Typed fact insert affected other than one row");}
    }
    static void receipt(ArrayNode receipts,ArrayNode inverse,String pointer,String type,String table,JsonNode key,Map<String,Object> values,boolean synthetic) {
        var r=Json.object().put("sourcePointer",pointer).put("type",type).put("table",table).put("originalInputProvided",!synthetic).put("originalAuthorityClaimed",false).put("operatingApprovalClaimed",false);r.set("key",key);r.set("installedValues",Json.MAPPER.valueToTree(values.entrySet().stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,e->e.getValue() instanceof OffsetDateTime t?t.toString():e.getValue(),(a,b)->a,LinkedHashMap::new))));receipts.add(r);
        var undo=Json.object().put("operation","DELETE_EXACT_INSERTED_ROW").put("table",table);undo.set("key",key.deepCopy());undo.set("expectedValues",r.path("installedValues").deepCopy());inverse.add(undo);
    }
    private static ArrayNode checkDeclaredInventory(Connection c,ModelFixturePlan plan) throws Exception {
        var checks=Json.array();JsonNode declared=plan.fixture.path("baseline").path("domainFacts").path("inventory");
        for(var i=declared.fields();i.hasNext();) {
            var fact=i.next();String column=switch(fact.getKey()){case "onHand"->"ALL";case "reserved"->"RESERVED";case "consignmentSell"->"SyntheticSupplier";case "customerCustody"->"Customer";default->throw new IllegalArgumentException("Unknown inventory aggregate");};
            String sql;var parameters=new ArrayList<String>();parameters.add(plan.org);
            if(column.equals("RESERVED"))sql="SELECT COALESCE(SUM(quantity),0) FROM mulino_inventory_SegmentAllocations WHERE organizationId=? AND state IN ('EXECUTABLE','SUSPENDED')";
            else {sql="SELECT COALESCE(SUM(quantity),0) FROM mulino_inventory_QuantitySegments WHERE organizationId=? AND retiredAt IS NULL";
                if(!column.equals("ALL")){String owner=null;for(JsonNode link:plan.inputs.path("logicalCustody"))if(link.path("representsType").asText().equals(column))owner=link.path("representativeActorId").asText();if(owner==null)throw new IllegalArgumentException("Explicit ownership relation required for "+column);sql+=" AND ownerId=?";parameters.add(owner);}}
            BigDecimal observed;try(var statement=c.prepareStatement(sql)){for(int p=0;p<parameters.size();p++)statement.setString(p+1,parameters.get(p));try(var result=statement.executeQuery()){result.next();observed=result.getBigDecimal(1);}}
            if(observed.compareTo(new BigDecimal(fact.getValue().asText()))!=0)throw new IllegalArgumentException("Original declared inventory aggregate differs from actual source rows: "+fact.getKey());
            var check=Json.object().put("sourcePointer","/baseline/domainFacts/inventory/"+fact.getKey()).put("sql",sql).put("observed",observed.toPlainString());check.set("parameters",Json.MAPPER.valueToTree(parameters));checks.add(check);
        }return checks;
    }
    private static ArrayNode checkDomainFacts(Connection c,ModelFixturePlan plan) throws Exception {
        var checks=Json.array();JsonNode facts=plan.fixture.at("/baseline/domainFacts");
        if(facts.has("c2")) {
            for(var i=facts.path("c2").fields();i.hasNext();) {var f=i.next();String sql=switch(f.getKey()) {
                case "mondayArrived"->"SELECT COALESCE(SUM(quantity),0) FROM mulino_trade_receipt_Receipts WHERE organizationId=? AND occurredAt >= '2026-10-05T00:00:00Z' AND occurredAt < '2026-10-06T00:00:00Z'";
                case "wednesdayArrived"->"SELECT COALESCE(SUM(quantity),0) FROM mulino_trade_receipt_Receipts WHERE organizationId=? AND occurredAt >= '2026-10-07T00:00:00Z' AND occurredAt <= '2026-10-07T03:00:00Z'";
                case "tuesdayDispatched"->"SELECT COALESCE(SUM(quantity),0) FROM mulino_inventory_QuantityMovements WHERE organizationId=? AND kind='DISPATCH' AND occurredAt >= '2026-10-06T00:00:00Z' AND occurredAt < '2026-10-07T00:00:00Z'";
                case "arrived"->"SELECT COALESCE(SUM(quantity),0) FROM mulino_trade_receipt_Receipts WHERE organizationId=?";
                case "onHand"->"SELECT COALESCE(SUM(s.quantity),0) FROM mulino_inventory_QuantitySegments s JOIN mulino_inventory_Places p ON p.organizationId=s.organizationId AND p.ID=s.placeId WHERE s.organizationId=? AND s.retiredAt IS NULL AND p.kind='INTERNAL_STORAGE'";
                default->throw new IllegalArgumentException("Unknown c2 source fact");};checkNumber(c,checks,plan.org,"/baseline/domainFacts/c2/"+f.getKey(),sql,new BigDecimal(f.getValue().asText()));}
        }
        JsonNode dispatch=facts.path("dispatch");if(dispatch.has("transportRemaining")) {
            String sql="SELECT COALESCE(SUM(s.quantity),0) FROM mulino_inventory_QuantitySegments s JOIN mulino_inventory_Places p ON p.organizationId=s.organizationId AND p.ID=s.placeId WHERE s.organizationId=? AND s.retiredAt IS NULL AND p.kind='TRANSIT'";checkNumber(c,checks,plan.org,"/baseline/domainFacts/dispatch/transportRemaining",sql,new BigDecimal(dispatch.path("transportRemaining").asText()));
        }
        return checks;
    }
    private static void checkNumber(Connection c,ArrayNode checks,String org,String pointer,String sql,BigDecimal original) throws Exception {
        BigDecimal observed;try(var statement=c.prepareStatement(sql)){statement.setString(1,org);try(var rs=statement.executeQuery()){rs.next();observed=rs.getBigDecimal(1);}}
        if(observed.compareTo(original)!=0)throw new IllegalArgumentException("Original source quantity differs from installed facts: "+pointer+" expected="+original+" actual="+observed);
        var receipt=Json.object().put("sourcePointer",pointer).put("sql",sql).put("observed",observed.toPlainString());receipt.set("parameters",Json.array().add(org));checks.add(receipt);
    }
    static void readback(Connection c,ArrayNode receipts) throws Exception {
        for(JsonNode receipt:receipts){var keys=new ArrayList<String>();receipt.path("key").fieldNames().forEachRemaining(keys::add);String sql="SELECT * FROM "+receipt.path("table").asText()+" WHERE "+String.join(" AND ",keys.stream().map(k->k+"=?").toList());
            try(var s=c.prepareStatement(sql)){for(int i=0;i<keys.size();i++)s.setString(i+1,receipt.path("key").path(keys.get(i)).asText());try(var rs=s.executeQuery()){if(!rs.next())throw new SQLException("Installed fact readback missing");var observed=Json.object();for(int i=1;i<=rs.getMetaData().getColumnCount();i++){Object v=rs.getObject(i);if(v!=null)observed.put(rs.getMetaData().getColumnLabel(i),v.toString());}for(var fields=receipt.path("installedValues").fields();fields.hasNext();) {
                        var expected=fields.next();Object actual=rs.getObject(expected.getKey());JsonNode value=expected.getValue();boolean equal;
                        if(actual instanceof java.math.BigDecimal decimal)equal=value.isNumber()?decimal.compareTo(value.decimalValue())==0:decimal.compareTo(new BigDecimal(value.asText()))==0;
                        else if(actual instanceof java.sql.Timestamp timestamp)equal=timestamp.toInstant().equals(OffsetDateTime.parse(value.asText()).toInstant());
                        else equal=Objects.equals(actual==null?null:actual.toString(),value.isNull()?null:value.asText());
                        if(!equal)throw new SQLException("Installed fact readback differs: "+receipt.path("type").asText()+"."+expected.getKey());
                    }
                    if(rs.next())throw new SQLException("Installed fact key not unique");((ObjectNode)receipt).set("readback",observed);var query=Json.object().put("sql",sql);var parameters=Json.array();for(String key:keys)parameters.add(receipt.path("key").path(key));query.set("parameters",parameters);((ObjectNode)receipt).set("sourceQuery",query);}}
        }
    }
}
