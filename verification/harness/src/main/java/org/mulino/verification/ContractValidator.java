package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class ContractValidator {
    private final Path root;
    private final Set<String> capabilities=new HashSet<>();
    private final Map<String,String> adapterAliases=new HashMap<>();
    /** Adapter names that only an actual client/model host (UAT runner) can supply. */
    public static final Set<String> CLIENT_ADAPTERS=Set.of("client","model");
    /** Driver metadata is request-side provenance written by the harness port, never an independent oracle source. */
    public static final List<String> NON_ORACLE_POINTERS=List.of("/provenance","/reason","/artifactRefs","/driverStatus","/actionId");
    public ContractValidator(Path root) throws IOException {
        this.root=root.toAbsolutePath().normalize();
        JsonNode registry=Json.read(root.resolve("contracts/acceptance-capabilities.json"));
        for(JsonNode c:registry.path("capabilities")) capabilities.add(Json.required(c,"id"));
        registry.path("adapterAliases").fields().forEachRemaining(e->adapterAliases.put(e.getKey(),e.getValue().asText()));
    }
    /** Canonical adapter vocabulary; unknown names stay distinct so no driver can satisfy them implicitly. */
    public String adapter(String name) { return adapterAliases.getOrDefault(name,name); }
    public Set<String> adapters(Iterable<?> names) {
        Set<String> out=new TreeSet<>();
        for(Object n:names) out.add(adapter(n instanceof JsonNode j ? j.asText() : String.valueOf(n)));
        return out;
    }
    /** UAT-only subcases need a real client/model host; a scripted SIT runner cannot substitute. */
    public boolean requiresActualClient(JsonNode subcase) {
        for(String a:adapters(subcase.path("requiredAdapters"))) if(CLIENT_ADAPTERS.contains(a)) return true;
        return false;
    }
    /** Preparation problems for oracle sources that read harness-side driver metadata. */
    public List<String> oracleSourceProblems(JsonNode caseFile) {
        List<String> problems=new ArrayList<>();
        for(JsonNode sub:caseFile.path("subcases")) for(JsonNode assertion:sub.path("assertions"))
            for(String field:List.of("source","baseline","unitSource","baselineUnitSource")) if(assertion.has(field)) {
                String pointer=assertion.path(field).path("pointer").asText();
                for(String forbidden:NON_ORACLE_POINTERS) if(pointer.equals(forbidden) || pointer.startsWith(forbidden+"/"))
                    problems.add(caseFile.path("caseId").asText()+"/"+sub.path("id").asText()+"/"+assertion.path("id").asText()+": "+field+" "+pointer+" reads driver request-side metadata, not an independent server/DB observation");
            }
        return problems;
    }
    public Path root() { return root; }
    public Path path(String relative) {
        Path p=root.resolve(relative).normalize();
        if(!p.startsWith(root)) throw new IllegalArgumentException("Reference escapes repository: "+relative);
        return p;
    }
    public void schema(String file,JsonNode instance) throws IOException {
        JsonSchema s=JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(Json.read(path(file)));
        Set<ValidationMessage> errors=s.validate(instance);
        if(!errors.isEmpty()) throw new IllegalArgumentException("Schema invalid "+file+": "+errors);
    }
    public JsonNode caseFile(Path file) throws IOException {
        JsonNode c=Json.read(file); schema("contracts/acceptance-case.schema.json",c);
        boolean example=c.path("caseId").asText().startsWith("HARNESS-");
        Set<String> subs=new HashSet<>();
        for(JsonNode sub:c.path("subcases")) {
            require(subs.add(Json.required(sub,"id")),"Duplicate subcase ID");
            fixture(Json.required(sub,"fixtureRef"));
            Set<String> ids=new HashSet<>(); List<JsonNode> all=new ArrayList<>(); collect(sub.path("actions"),all);
            boolean uatOnly=requiresActualClient(sub);
            for(JsonNode a:all) { require(ids.add(Json.required(a,"id")),"Duplicate action ID"); checkAction(a,uatOnly); }
            Set<String> starts=new HashSet<>(), awaited=new HashSet<>();
            for(JsonNode a:all) if(a.path("kind").asText().equals("start")) starts.add(Json.required(a,"id"));
            for(JsonNode a:all) if(a.path("kind").asText().equals("await")) {
                String source=Json.required(a,"awaitActionId");
                require(starts.contains(source),"awaitActionId must reference start");
                require(awaited.add(source),"start must have exactly one terminal await");
            }
            require(awaited.equals(starts),"Every start requires terminal await, including parallel children");
            Set<String> assertionIds=new HashSet<>();
            for(JsonNode assertion:sub.path("assertions")) {
                require(assertionIds.add(Json.required(assertion,"id")),"Duplicate assertion ID");
                require(ids.contains(Json.required(assertion.path("source"),"actionId")),"Unknown assertion source");
                if(assertion.has("unitSource")) require(ids.contains(Json.required(assertion.path("unitSource"),"actionId")),"Unknown assertion unit source");
                if(assertion.has("baselineUnitSource")) require(ids.contains(Json.required(assertion.path("baselineUnitSource"),"actionId")),"Unknown assertion baseline unit source");
                if(assertion.has("baseline")) require(ids.contains(Json.required(assertion.path("baseline"),"actionId")),"Unknown assertion baseline");
                if(!example) require(assertion.has("oracleRef"),"Product assertion requires independent oracleRef");
                references(assertion,ids);
                for(String field:List.of("expected","scope")) identityReferences(assertion.path(field));
                for(String field:List.of("source","baseline","unitSource","baselineUnitSource")) identityReferences(assertion.path(field).path("where"));
                for(JsonNode ref:assertion.path("evidenceRefs")) require(!ref.asText().isBlank(),"Empty evidence requirement");
            }
            require(!sub.path("assertions").isEmpty(),"No substantive assertions");
            for(JsonNode action:all) references(action,ids);
        }
        return c;
    }
    private void references(JsonNode node,Set<String> ids) {
        if(node.isObject() && node.has("$result")) {
            require(node.size()==1,"Result reference allows only $result");JsonNode ref=node.path("$result");
            require(ref.isObject() && ref.size()==2 && ref.has("actionId") && ref.has("pointer"),"Result reference requires only actionId/pointer");
            require(ids.contains(Json.required(ref,"actionId")),"Unknown result reference action");
            require(Json.required(ref,"pointer").startsWith("/"),"Result reference must use RFC6901 pointer");
        } else if(node.isObject() && node.has("$transform")) {
            require(node.size()==1,"Transform reference allows only $transform");
            validateTransformDeclaration(node.path("$transform"));references(node.path("$transform").path("source"),ids);
        } else if(node.isObject() && node.has("$alias")) { require(node.size()==1 && node.path("$alias").isTextual(),"Alias reference requires only textual $alias"); }
        else if(node.isContainerNode()) node.forEach(n->references(n,ids));
    }
    private void identityReferences(JsonNode node) {
        if(node.isObject() && node.has("$transform")) require(false,"Assertion identity bindings cannot transform expected values");
        if(node.isObject() && node.has("$result")) require(ReferenceResolver.identityPointer(node.path("$result").path("pointer").asText()),"Assertion references must bind identity, not observed business values");
        else if(node.isContainerNode()) node.forEach(this::identityReferences);
    }
    private void validateTransformDeclaration(JsonNode t) {
        try {
            JsonNode schema=Json.read(path("contracts/acceptance-case.schema.json"));
            schema=((com.fasterxml.jackson.databind.node.ObjectNode)schema).deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)schema).put("$ref","#/$defs/transformReference");
            ((com.fasterxml.jackson.databind.node.ObjectNode)schema).remove(List.of("type","properties","required","additionalProperties"));
            Set<ValidationMessage> errors=JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schema).validate(Json.object().set("$transform",t));
            require(errors.isEmpty(),"Invalid bounded transform declaration: "+errors);
        } catch(IOException e) {throw new IllegalArgumentException(e);}
    }
    private void checkAction(JsonNode action,boolean uatOnly) {
        String kind=Json.required(action,"kind");
        if(Set.of("invoke","query").contains(kind) && !action.has("protocolOperation")) require(capabilities.contains(Json.required(action,"capabilityId")),"Unknown public capability "+action.path("capabilityId"));
        if(kind.equals("start")) { String child=action.path("call").path("kind").asText(); require(Set.of("invoke","query").contains(child),"start call must be invoke/query"); checkAction(action.path("call"),uatOnly); }
        if(kind.equals("agent")) {
            JsonNode context=action.path("permittedContext");
            for(String forbidden:List.of("expected","assertions","oracle","oracleRef","intent","capabilityId","slots"))
                require(!containsKey(context,forbidden),"Actual agent context contains planned answer/oracle: "+forbidden);
            JsonNode capability=action.path("intent").path("capabilityId");
            // SIT replays a declared typed intent; only UAT-only subcases may omit it.
            if(capability.isMissingNode()) require(uatOnly,"Agent action without scripted typed intent must belong to a UAT-only subcase (requiredAdapters client/model)");
            else require(capability.isTextual() && capabilities.contains(capability.asText()),"Unknown scripted agent capability "+capability);
        }
    }
    private static boolean containsKey(JsonNode node,String key) {
        if(node.isObject() && node.has(key)) return true;
        if(node.isContainerNode()) for(JsonNode child:node) if(containsKey(child,key)) return true;
        return false;
    }
    private static void collect(JsonNode actions,List<JsonNode> all) {
        for(JsonNode a:actions) { all.add(a); for(JsonNode b:a.path("branches")) collect(b.path("actions"),all); }
    }
    public JsonNode fixture(String ref) throws IOException {
        return fixture(ref,new HashSet<>());
    }
    private JsonNode fixture(String ref,Set<String> visiting) throws IOException {
        require(visiting.add(ref),"Fixture baseRefs cycle"); JsonNode fixture=Json.read(path(ref)); schema("contracts/acceptance-fixture.schema.json",fixture);
        for(JsonNode base:fixture.path("baseRefs")) fixture(base.asText(),visiting);
        visiting.remove(ref); return fixture;
    }
    public void result(StepResult result,String kind) throws IOException {
        schema("contracts/acceptance-driver.schema.json",result.toJson());
        if(result.driverStatus()!=StepResult.DriverStatus.EXECUTED) return;
        require(result.provenance().path("scopeComplete").asBoolean(false),"Executed action scope is incomplete");
        for(String artifact:result.artifactRefs()) require(Files.isRegularFile(path(artifact)),"Missing actual execution artifact "+artifact);
        if(kind.equals("observe")) {
            require(result.provenance().path("independent").asBoolean(false),"DB observation must be independent");
            schema("contracts/acceptance-observation.schema.json",result.data());
            require(!result.provenance().path("sourceQuery").isNull() && !result.provenance().path("snapshot").isNull(),"Observer query/snapshot provenance missing");
        }
        if(kind.equals("control")) require(result.data()!=null && result.data().path("acknowledged").asBoolean(false),"Control needs actual ACK");
        if(kind.equals("start")) require(result.data()!=null && result.data().hasNonNull("invocationHandle"),"Async submission needs actual invocationHandle ACK");
    }
    static void require(boolean ok,String message) { if(!ok) throw new IllegalArgumentException(message); }
}
