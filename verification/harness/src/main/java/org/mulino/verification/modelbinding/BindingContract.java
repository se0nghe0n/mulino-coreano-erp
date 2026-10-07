package org.mulino.verification.modelbinding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.mulino.verification.*;
import java.nio.file.*;
import java.util.*;
import java.io.IOException;

/** Separate M60 registry: immutable source oracle, no T/C/V/E registry changes. */
public final class BindingContract {
    public final ContractValidator validator;
    public final JsonNode registry,corpus,pathRegistry;
    private final Map<String,JsonNode> paths=new LinkedHashMap<>();
    public BindingContract(Path root) throws IOException {
        validator=new ContractValidator(root);
        registry=Json.read(validator.path("verification/model-binding/registry.json"));
        validator.schema("verification/model-binding/registry.schema.json",registry);
        corpus=Json.read(validator.path(registry.path("corpusRef").asText()));
        require(Json.sha256(validator.path(registry.path("corpusRef").asText())).equals(registry.path("corpusSha256").asText()),"Closed corpus hash drift");
        pathRegistry=Json.read(validator.path("verification/model-binding/semantic-paths.json"));
        require(pathRegistry.path("corpusSha256").equals(registry.path("corpusSha256")),"Mapping source hash drift");
        for(JsonNode m:pathRegistry.path("paths")) require(paths.put(Json.required(m,"semanticPath"),m)==null,"Duplicate semantic path");
    }
    public JsonNode binding(String id) throws IOException {
        for(JsonNode e:registry.path("cases")) if(e.path("caseId").asText().equals(id)) {
            JsonNode b=Json.read(validator.path(Json.required(e,"bindingRef")));
            validator.schema("verification/model-binding/binding.schema.json",b); return b;
        }
        throw new IllegalArgumentException("Unknown model case "+id);
    }
    public JsonNode mapping(String path) {JsonNode m=paths.get(path);require(m!=null,"Unbound semantic path "+path);return m;}
    public JsonNode sourceCase(JsonNode b){return corpus.at(Json.required(b,"corpusCasePointer"));}
    public JsonNode sourceTurn(JsonNode t){return corpus.at(Json.required(t,"corpusTurnPointer"));}
    public JsonNode fixture(JsonNode b) throws IOException {return validator.fixture(Json.required(b,"fixtureRef"));}
    public ObjectNode prepare() throws IOException {
        Set<String> ids=new HashSet<>(),commonPaths=new HashSet<>();int turns=0,assertions=0;
        Set<String> capabilities=new HashSet<>();for(JsonNode c:Json.read(validator.path("contracts/acceptance-capabilities.json")).path("capabilities")) capabilities.add(c.path("id").asText());
        for(JsonNode e:registry.path("cases")) {
            String id=Json.required(e,"caseId");require(ids.add(id),"Duplicate model case "+id);
            JsonNode b=binding(id),c=sourceCase(b),f=fixture(b);
            require(c.path("id").asText().equals(id),"Case pointer mismatch");require(b.path("corpusSha256").equals(registry.path("corpusSha256")),"Binding hash drift");
            require(b.path("turns").size()==c.path("turns").size(),"Missing turn");
            require(e.path("turnIds").size()==b.path("turns").size(),"Registry missing turn");
            JsonNode effective=deepMerge(corpus.path("commonFixture"),c.path("fixture"));
            for(String profile:List.of("command-actor","read-probe-actor")) {
                JsonNode a=f.path("actors").path(profile);require(!a.isMissingNode(),"Missing actor profile");
                require(!a.path("grant").path("actions").toString().contains("*"),"Wildcard grant");
                require(!a.path("grant").path("scope").path("targetAliases").toString().contains("*"),"Wildcard targets");
            }
            String active=effective.path("authentication").path("actorRef").asText();JsonNode principal=effective.path("actors").path(active);String commandGrant=principal.path("grantRef").asText();String readGrant=principal.path("readGrantRef").asText(commandGrant);
            actorMatches(f.path("actors").path("command-actor"),principal,effective.path("grants").path(commandGrant),commandGrant,false);
            actorMatches(f.path("actors").path("read-probe-actor"),principal,effective.path("grants").path(readGrant),readGrant,!readGrant.equals(commandGrant));
            require(f.path("baseline").path("principalFacts").equals(effective.path("actors"))&&f.path("baseline").path("grantFacts").equals(effective.path("grants")),"Authentication fixture facts drift");
            if(id.equals("M47")) {
                JsonNode a=f.path("actors").path("command-actor"),r=f.path("actors").path("read-probe-actor");
                for(String k:List.of("issuer","subject","audience","organizationAlias")) require(a.path(k).equals(r.path(k)),"M47 probe changes principal");
                require(a.path("grant").path("scope").path("revokedAt").isTextual(),"M47 lost revocation");
                require(r.path("grant").path("actions").equals(Json.parse("[\"READ\"]")),"M47 probe restores command authority");
            }
            // Conversion must preserve every effective business fact; auth caches are not seeded.
            JsonNode facts=((ObjectNode)effective.deepCopy()).remove(List.of("ids","authentication","actors","grants","businessClock","mergeRule","currentWriteAuthorization"));
            require(facts.equals(f.path("baseline").path("domainFacts")),"Effective fixture differs from isolated driver facts");
            Set<String> assertionIds=new HashSet<>();List<String> expectedSteps=new ArrayList<>();expectedSteps.add("install");
            for(int i=0;i<b.path("turns").size();i++) {
                JsonNode t=b.path("turns").get(i),s=sourceTurn(t);String tid="turn-"+(i+1);
                require(t.path("id").asText().equals(tid) && e.path("turnIds").get(i).asText().equals(tid),"Turn ordering drift");
                require(s.equals(c.path("turns").get(i)),"Turn pointer mismatch");
                String semantic=s.path("expectedIntent").path("capabilityId").asText(),pub=t.path("capabilityMapping").path("public").asText();
                require(t.path("capabilityMapping").path("semantic").asText().equals(semantic),"Semantic capability drift");
                require(pub.equals(semantic.equals("linkRelation")?"recordRelation":semantic),"Unsupported capability remapping");
                require(capabilities.contains(pub),"Missing public capability "+pub);
                require(t.path("oracleRef").asText().equals(t.path("corpusTurnPointer").asText()+"/oracle"),"Oracle pointer drift");
                require(t.path("commonAssertions").size()==s.path("oracle").path("assertions").size(),"Missing common assertion binding");
                validateRefList(t.path("commonAssertions"),s.path("oracle").path("assertions"),t.path("oracleRef").asText()+"/assertions");
                for(JsonNode ref:t.path("commonAssertions")) validateAssertionRef(ref,assertionIds);
                validateRefList(t.path("oracleAssertions").path("SIT_DIRECT_COMMAND"),s.path("oracle").path("sitDirectCommand").path("assertions"),t.path("oracleRef").asText()+"/sitDirectCommand/assertions");
                s.path("oracle").path("uatCompletion").path("pathOracles").fields().forEachRemaining(branch->validateRefList(t.path("oracleAssertions").path(branch.getKey()),branch.getValue().path("assertions"),t.path("oracleRef").asText()+"/uatCompletion/pathOracles/"+branch.getKey()+"/assertions"));
                t.path("oracleAssertions").forEach(list->list.forEach(ref->validateAssertionRef(ref,assertionIds)));
                for(JsonNode a:s.path("oracle").path("assertions")) {mapping(a.path("path").asText());commonPaths.add(a.path("path").asText());assertions++;}
                for(JsonNode a:s.path("oracle").path("sitDirectCommand").path("assertions")) mapping(a.path("path").asText());
                s.path("oracle").path("uatCompletion").path("pathOracles").forEach(p->p.path("assertions").forEach(a->mapping(a.path("path").asText())));
                for(JsonNode step:t.path("steps")) expectedSteps.add(tid+"/"+step.asText());turns++;
            }
            String feature=Files.readString(validator.path(Json.required(b,"featureRef")));
            require(feature.startsWith("# language: ko"),"Gherkin language drift");
            List<String> actualSteps=feature.lines().filter(l->l.contains("단계를 실행한다")).map(l->l.split("\"")[3]).toList();
            require(actualSteps.equals(expectedSteps),"Missing/reordered Gherkin action "+id);
            for(String assertionId:assertionIds) if(assertionId.contains("/common-")) require(feature.contains("\""+assertionId+"\" oracle"),"Gherkin omitted substantive assertion "+assertionId);
        }
        require(ids.size()==60 && turns==73 && assertions==221 && commonPaths.size()==154,"Corpus coverage count drift");
        Set<String> mappedCommon=new HashSet<>();for(JsonNode m:pathRegistry.path("paths"))if(m.path("common").asBoolean())mappedCommon.add(m.path("semanticPath").asText());require(mappedCommon.equals(commonPaths),"Common mapping set differs from actual corpus paths");
        for(int i=1;i<=60;i++) require(ids.contains(String.format("M%02d",i)),"Missing case");
        ObjectNode r=report("PREPARED","NOT_RUN");r.put("preparationStatus","PREPARED");return r;
    }
    void actorMatches(JsonNode profile,JsonNode principal,JsonNode grant,String alias,boolean readOnly){
        for(String key:List.of("issuer","subject","audience"))require(profile.path(key).equals(principal.path(key)),"Actor identity drift "+key);
        require(profile.path("roleCapabilities").equals(readOnly?Json.parse("[\"READ\"]"):principal.path("roles")),"Actor role widened");
        for(String key:List.of("actions","validFrom","validUntil","revision"))require(profile.path("grant").path(key).equals(grant.path(key)),"Grant scope/version drift "+key);
        JsonNode scope=profile.path("grant").path("scope");require(scope.path("grantAlias").asText().equals(alias)&&scope.path("targetAliases").equals(grant.path("targetScope"))&&scope.path("revokedAt").equals(grant.path("revokedAt")),"Grant identity/target/revocation drift");
    }
    private void validateRefList(JsonNode refs,JsonNode assertions,String pointer){require(refs.size()==assertions.size(),"Missing path assertion binding");for(int i=0;i<assertions.size();i++)require(refs.get(i).path("corpusAssertionPointer").asText().equals(pointer+"/"+i)&&refs.get(i).path("semanticPath").equals(assertions.get(i).path("path")),"Assertion coverage pointer drift");}
    private void validateAssertionRef(JsonNode ref,Set<String> ids){require(ids.add(Json.required(ref,"assertionId")),"Duplicate bound assertion ID");JsonNode a=corpus.at(Json.required(ref,"corpusAssertionPointer"));require(a.isObject()&&a.path("path").equals(ref.path("semanticPath")),"Assertion pointer/path drift");mapping(ref.path("semanticPath").asText());}
    public ObjectNode report(String status,String runtime) throws IOException {
        ObjectNode r=Json.object();r.put("schemaVersion","1.0.0").put("baselineCommit",registry.path("baselineCommit").asText()).put("status",status).put("runtimeStatus",runtime).put("productGateComplete",false).put("actualModelCalls",0).put("caseCount",60).put("turnCount",73).put("commonAssertionCount",221).put("semanticPathCount",154).put("plannedRepeats",3).put("plannedAttempts",180).put("corpusSha256",registry.path("corpusSha256").asText()).put("bindingRegistrySha256",Json.sha256(validator.path("verification/model-binding/registry.json"))).put("timestamp",java.time.Instant.now().toString());
        r.put("codeCommit",git("rev-parse","HEAD").strip());r.put("workingTreeDirty",!git("status","--porcelain").isBlank());r.put("javaVersion",System.getProperty("java.runtime.version"));r.set("inputArtifacts",inputArtifacts());r.put("profile","MODEL_BINDING_PREPARATION");r.put("exitCode",0);r.set("versions",Json.parse("{\"binding\":\"1.0.0\",\"mapping\":\"model-binding-v1\"}"));r.set("attempts",Json.array());r.putNull("usage");r.putNull("cost");r.put("missingReason","R8 actual model/client/config/cost authorization and real product adapters absent");r.set("executionPlan",corpus.path("executionPlan"));return r;
    }
    private String git(String... args) throws IOException {var command=new ArrayList<String>();command.add("git");command.addAll(List.of(args));Process process=new ProcessBuilder(command).directory(validator.root().toFile()).start();String output=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);try{require(process.waitFor()==0,"Cannot record actual Git evidence");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException(e);}return output;}
    private com.fasterxml.jackson.databind.node.ArrayNode inputArtifacts() throws IOException {
        var artifacts=Json.array();Set<Path> files=new TreeSet<>();files.add(validator.path(registry.path("corpusRef").asText()));
        for(String ref:List.of("verification/model-binding/registry.json","verification/model-binding/semantic-paths.json","verification/model-binding/binding.schema.json","verification/model-binding/registry.schema.json","verification/model-binding/physical-columns.schema.json","verification/model-binding/observer-rows.schema.json","verification/model-binding/captured-api.schema.json","verification/model-binding/final-response-observation.schema.json","verification/model-binding/capture-artifacts.schema.json"))files.add(validator.path(ref));
        for(JsonNode e:registry.path("cases")){files.add(validator.path(e.path("bindingRef").asText()));JsonNode b=binding(e.path("caseId").asText());files.add(validator.path(b.path("fixtureRef").asText()));files.add(validator.path(b.path("featureRef").asText()));}
        try(var source=Files.walk(validator.path("verification/harness/src/main/java/org/mulino/verification/modelbinding"))){source.filter(p->p.toString().endsWith(".java")).forEach(files::add);}
        for(String ref:List.of("contracts/acceptance-capabilities.json","contracts/acceptance-fixture.schema.json","contracts/acceptance-observation.schema.json","contracts/acceptance-host-observation.schema.json","contracts/acceptance-driver.schema.json","verification/harness/src/main/java/org/mulino/verification/AgentRunner.java","verification/harness/src/main/java/org/mulino/verification/AcceptanceDriver.java","verification/harness/src/main/java/org/mulino/verification/AssertionEngine.java","verification/harness/src/main/java/org/mulino/verification/HostObservationValidator.java"))files.add(validator.path(ref));
        for(Path file:files){var a=Json.object();a.put("path",validator.root().relativize(file).toString()).put("sha256",Json.sha256(file)).put("sizeBytes",Files.size(file));artifacts.add(a);}return artifacts;
    }
    public static JsonNode deepMerge(JsonNode a,JsonNode b){ObjectNode out=(ObjectNode)a.deepCopy();b.fields().forEachRemaining(e->{JsonNode prev=out.get(e.getKey());out.set(e.getKey(),prev!=null&&prev.isObject()&&e.getValue().isObject()?deepMerge(prev,e.getValue()):e.getValue().deepCopy());});return out;}
    public static void require(boolean good,String reason){if(!good)throw new IllegalArgumentException(reason);}
}
