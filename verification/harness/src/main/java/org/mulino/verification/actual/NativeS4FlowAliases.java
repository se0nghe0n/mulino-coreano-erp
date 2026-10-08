package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import org.mulino.verification.Json;

/** Static walk of an authored S4 flow in NativeS4TradeMain execution order; reads no runtime state. */
public final class NativeS4FlowAliases {
    static final String DIRECTORY="verification/actual/s4",BASE_FIXTURE=DIRECTORY+"/fixture.json";
    /** S3OriginalFixtureInstaller alias keys plus the runner's hash; canonical/verification come only from public link commands. */
    private static final List<String> ORIGINAL_KEYS=List.of("document","event","inbox","claim","sourceProfile","physicalScope","blob","hash");
    private static final Pattern TEMPLATE=Pattern.compile("\\$\\{([^}]+)}");
    private final Path root;private final String flow;
    private static final Pattern TABLE=Pattern.compile("(?i)create table (?:if not exists )?([a-z0-9_]+)"),RAW=Pattern.compile("^/rawRows/([^/]+)");
    private final Set<String> bound=new HashSet<>(),ids=new HashSet<>(),tables=new HashSet<>();private Set<String> actors=Set.of();
    private final List<String> errors=new ArrayList<>();
    private NativeS4FlowAliases(Path root,String flow){this.root=root;this.flow=flow;}
    /** Every unbound alias, unknown actor/script and duplicate captured action ID of one flow. */
    public static List<String> check(Path root,String flowRef)throws IOException {var c=new NativeS4FlowAliases(root,flowRef);c.migrations();c.install(BASE_FIXTURE);c.walk(flowRef);return List.copyOf(c.errors);}
    /** Independent SQL pointers must name a migrated table; a typo would otherwise fail only after a full disposable run. */
    private void migrations()throws IOException {Path dir=root.resolve("database/migrations");if(!Files.isDirectory(dir))return;try(var files=Files.list(dir)){for(Path f:files.filter(p->p.toString().endsWith(".sql")).toList()){var m=TABLE.matcher(Files.readString(f));while(m.find())tables.add(m.group(1).toLowerCase(Locale.ROOT));}}}
    private void rawRows(String id,JsonNode node){if(node.isTextual()){var m=RAW.matcher(node.asText());if(m.find()&&!tables.isEmpty()&&!tables.contains(m.group(1)))errors.add(flow+": "+id+" observes unknown table "+m.group(1));}else for(JsonNode child:node)rawRows(id,child);}
    public static List<String> flows(Path root)throws IOException {try(var files=Files.list(root.resolve(DIRECTORY))){return files.filter(p->p.getFileName().toString().endsWith("flow.json")).map(p->root.relativize(p).toString()).sorted().toList();}}
    private JsonNode install(String ref)throws IOException {JsonNode fixture=Json.read(root.resolve(ref));fixture.path("aliases").fieldNames().forEachRemaining(bound::add);var names=new HashSet<String>();fixture.path("actors").fieldNames().forEachRemaining(names::add);actors=names;return fixture;}
    private void walk(String ref)throws IOException {Path path=root.resolve(ref).normalize();if(!path.startsWith(root)||!Files.isRegularFile(path)){errors.add(flow+": missing script "+ref);return;}for(JsonNode a:Json.read(path).path("actions"))action(a);}
    private void action(JsonNode a)throws IOException {
        String id=a.path("id").asText(),type=a.path("type").asText();
        // Captured driver results are keyed by action ID; uuid/include/clock actions capture nothing.
        if(!Set.of("include","require-contract","clock","uuid").contains(type)&&!ids.add(id))errors.add(flow+": duplicate captured action id "+id);
        if(type.equals("observe"))rawRows(id,a);
        switch(type) {
            case "include" -> walk(a.path("scriptRef").asText());
            case "setup" -> {bound.clear();JsonNode fixture=install(a.path("fixtureRef").asText());if(a.has("organizationAlias")){if(!fixture.path("aliases").has(a.path("organizationAlias").asText()))errors.add(flow+": "+id+" unknown organizationAlias");bound.add("ORG");}}
            case "uuid" -> bound.add(a.path("alias").asText());
            case "original" -> {need(id,a.path("fixture"),"fixture");need(id,a.path("binding"),"binding");for(String k:ORIGINAL_KEYS)bound.add(id+"."+k);}
            case "command","query","lost-response","parallel" -> {need(id,a.path("request"),"request");if(a.hasNonNull("actor")&&!actors.contains(a.path("actor").asText()))errors.add(flow+": "+id+" unknown actor "+a.path("actor").asText());assertionRefs(id,a);a.path("bind").fieldNames().forEachRemaining(bound::add);}
            case "observe" -> {assertionRefs(id,a);a.path("bind").fieldNames().forEachRemaining(bound::add);for(var it=a.path("bindRows").fields();it.hasNext();){var e=it.next();need(id,e.getValue(),"bindRows "+e.getKey());}a.path("bindRows").fieldNames().forEachRemaining(bound::add);}
            case "clock","require-contract" -> {}
            default -> errors.add(flow+": "+id+" unsupported action type "+type);
        }
    }
    private void assertionRefs(String id,JsonNode a){for(JsonNode x:a.path("assertions")){need(id,x.path("expected"),"assertion");need(id,x.path("where"),"assertion where");}}
    /** Mirrors NativeS4TradeMain.resolve: `${KEY}` templates first, otherwise a whole `$KEY` value. */
    private void need(String id,JsonNode node,String where) {
        if(node.isTextual()){String text=node.asText();if(text.contains("${")){var m=TEMPLATE.matcher(text);while(m.find())if(!bound.contains(m.group(1)))errors.add(flow+": "+id+" "+where+" reads unbound ${"+m.group(1)+"}");}else if(text.startsWith("$")&&!bound.contains(text.substring(1)))errors.add(flow+": "+id+" "+where+" reads unbound $"+text.substring(1));}
        else for(JsonNode child:node)need(id,child,where);
    }
    /** Author-time gate: exits 1 on any unbound reference in the given or all committed S4 flows. */
    public static void main(String[] args)throws IOException {
        Path root=Path.of(System.getProperty("repo.root",".")).toAbsolutePath().normalize();var errors=new ArrayList<String>();var targets=args.length>0?List.of(args):flows(root);
        for(String f:targets)errors.addAll(check(root,f));errors.forEach(System.out::println);System.out.println("checked "+targets.size()+" flow(s), "+errors.size()+" unbound/invalid reference(s)");System.exit(errors.isEmpty()?0:1);
    }
}
