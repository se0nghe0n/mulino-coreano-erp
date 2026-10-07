package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.gherkin.GherkinParser;
import io.cucumber.messages.types.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/** Preparation only: discovered Pickles and registry references must match declared cases. */
public final class PreparationValidator {
    private static final String STRING="(\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*')";
    private static final Pattern PREPARE=Pattern.compile("사례 파일 "+STRING+"의 "+STRING+"를 준비한다");
    private static final Pattern ACTION=Pattern.compile(STRING+" 역할이 "+STRING+" 행동을 수행한다");
    private static final Pattern ASSERTION=Pattern.compile(STRING+" assertion으로 "+STRING+"를 확인한다");
    private final Path root;
    private final Path realRoot;

    public PreparationValidator(Path root) throws IOException { this.root=root.toAbsolutePath().normalize();this.realRoot=this.root.toRealPath(); }

    public List<String> feature(Path casePath,JsonNode caseNode) {
        List<String> problems=new ArrayList<>();String caseId=caseNode.path("caseId").asText();
        Map<String,JsonNode> subs=new LinkedHashMap<>();
        for(JsonNode sub:caseNode.path("subcases")) {
            String id=sub.path("id").asText();
            if(subs.put(id,sub)!=null) problems.add("Duplicate declared subcase "+caseId+"/"+id);
        }
        Set<String> discovered=new HashSet<>();
        try {
            Path actualCase=canonical(casePath.toString());
            Path feature=canonical(casePath.resolveSibling("scenario.feature").toString());
            List<Envelope> envelopes;
            try(var stream=GherkinParser.builder().includeSource(false).includeGherkinDocument(false).includePickles(true).build().parse(feature)) {
                envelopes=stream.toList();
            }
            for(Envelope envelope:envelopes) {
                if(envelope.getParseError().isPresent()) problems.add("Gherkin parse error "+caseId+": "+envelope.getParseError().get().getMessage());
                if(envelope.getPickle().isEmpty()) continue;
                Pickle pickle=envelope.getPickle().get();String label=caseId+" scenario "+pickle.getName();
                if(!pickle.getLanguage().equals("ko")) problems.add("Feature is not Korean "+label);
                List<PickleStep> steps=pickle.getSteps();
                if(steps.isEmpty()) { problems.add("Missing preparation "+label);continue; }
                Matcher preparation=PREPARE.matcher(steps.getFirst().getText());
                if(!preparation.matches()) { problems.add("Preparation must be first step "+label);continue; }
                checkType(steps.getFirst(),PickleStepType.CONTEXT,label,problems);
                try { if(!canonical(value(preparation,1)).equals(actualCase)) problems.add("Preparation case path mismatch "+label); }
                catch(IOException|IllegalArgumentException e) { problems.add("Invalid preparation case path "+label+": "+e.getMessage()); }
                String subId=value(preparation,2);JsonNode sub=subs.get(subId);
                if(!discovered.add(subId)) problems.add("Duplicate discovered subcase "+caseId+"/"+subId);
                if(sub==null) { problems.add("Unknown discovered subcase "+caseId+"/"+subId);continue; }
                List<String> actions=new ArrayList<>();Set<String> assertions=new HashSet<>();
                Map<String,String> roles=new HashMap<>();List<String> expectedActions=new ArrayList<>();
                for(JsonNode action:sub.path("actions")) { String id=action.path("id").asText();expectedActions.add(id);roles.put(id,action.path("actorRef").asText("시스템")); }
                Set<String> expectedAssertions=new HashSet<>();sub.path("assertions").forEach(a->expectedAssertions.add(a.path("id").asText()));
                boolean outcomesStarted=false;
                for(PickleStep step:steps.subList(1,steps.size())) {
                    String text=step.getText();Matcher action=ACTION.matcher(text),assertion=ASSERTION.matcher(text);
                    if(step.getArgument().isPresent()) problems.add("Unexpected step argument "+label);
                    if(action.matches()) {
                        checkType(step,PickleStepType.ACTION,label,problems);
                        String id=value(action,2);actions.add(id);
                        if(outcomesStarted) problems.add("Action after assertion "+label+"/"+id);
                        if(!Objects.equals(roles.get(id),value(action,1))) problems.add("Action role mismatch "+label+"/"+id);
                    } else if(assertion.matches()) {
                        checkType(step,PickleStepType.OUTCOME,label,problems);outcomesStarted=true;
                        String id=value(assertion,1);if(!assertions.add(id)) problems.add("Duplicate assertion "+label+"/"+id);
                    } else problems.add("Unexpected or duplicate preparation step "+label+": "+text);
                }
                if(steps.getFirst().getArgument().isPresent()) problems.add("Unexpected preparation argument "+label);
                if(!actions.equals(expectedActions)) problems.add("Action sequence mismatch "+caseId+"/"+subId+": expected "+expectedActions+", actual "+actions);
                if(!assertions.equals(expectedAssertions)) problems.add("Assertion membership mismatch "+caseId+"/"+subId+": expected "+expectedAssertions+", actual "+assertions);
            }
            if(!discovered.equals(subs.keySet())) problems.add("Discovered subcase membership mismatch "+caseId+": expected "+subs.keySet()+", actual "+discovered);
        } catch(IOException|IllegalArgumentException e) { problems.add("Invalid case/feature "+caseId+": "+e.getMessage()); }
        return problems;
    }

    public List<String> registry(JsonNode registry,Map<String,JsonNode> cases,Map<String,Path> casePaths,Set<String> expectedIds) {
        List<String> problems=new ArrayList<>();
        if(!cases.keySet().equals(expectedIds)) problems.add("Discovered case membership differs from expected cases");
        int subcount=cases.values().stream().mapToInt(c->c.path("subcases").size()).sum();
        if(!registry.path("expectedCases").isIntegralNumber() || registry.path("expectedCases").asInt(-1)!=expectedIds.size() || registry.path("expectedCases").asInt(-1)!=cases.size()
            || !registry.path("expectedSubcases").isIntegralNumber() || registry.path("expectedSubcases").asInt(-1)!=subcount) problems.add("Case/subcase discovery count differs from registry");
        if(!registry.path("cases").isArray()) problems.add("Registry cases must be an array");
        Set<String> registeredCases=new HashSet<>();
        for(JsonNode entry:registry.path("cases")) {
            String id=entry.path("caseId").asText();
            if(!registeredCases.add(id)) problems.add("Duplicate registry caseId "+id);
            JsonNode c=cases.get(id);if(c==null) {problems.add("Undiscovered registry case "+id);continue;}
            try {
                Path discoveredPath=casePaths.get(id);
                if(discoveredPath==null) throw new IllegalArgumentException("Missing discovered case path");
                Path actual=canonical(discoveredPath.toString());
                if(!canonical(Json.required(entry,"path")).equals(actual)) problems.add("Registry case path mismatch "+id);
                if(!canonical(Json.required(entry,"feature")).equals(canonical(casePaths.get(id).resolveSibling("scenario.feature").toString()))) problems.add("Registry feature path mismatch "+id);
            } catch(IOException|IllegalArgumentException e) { problems.add("Invalid registry path "+id+": "+e.getMessage()); }
            Set<String> actualSubs=new HashSet<>(),registeredSubs=new HashSet<>();
            for(JsonNode s:c.path("subcases")) if(!actualSubs.add(s.path("id").asText())) problems.add("Duplicate declared subcase "+id);
            if(!entry.path("subcaseIds").isArray()) problems.add("Registry subcaseIds must be an array "+id);
            for(JsonNode s:entry.path("subcaseIds")) {
                if(!s.isTextual() || s.asText().isBlank()) problems.add("Invalid registry subcaseId "+id);
                if(!registeredSubs.add(s.asText())) problems.add("Duplicate registry subcaseId "+id+"/"+s.asText());
            }
            if(!registeredSubs.equals(actualSubs)) problems.add("Subcase registry mismatch "+id);
        }
        if(!registeredCases.equals(expectedIds) || !registeredCases.equals(cases.keySet())) problems.add("Registry case membership mismatch: expected "+expectedIds+", actual "+registeredCases);
        return problems;
    }

    private Path canonical(String ref) throws IOException {
        Path lexical=root.resolve(ref).normalize();
        if(!lexical.startsWith(root)) throw new IllegalArgumentException("Reference escapes repository: "+ref);
        Path real=lexical.toRealPath();
        if(!real.startsWith(realRoot)) throw new IllegalArgumentException("Reference symlink escapes repository: "+ref);
        if(!Files.isRegularFile(real)) throw new IllegalArgumentException("Reference is not a regular file: "+ref);
        return real;
    }
    private static String value(Matcher matcher,int group) {
        String quoted=matcher.group(group);char quote=quoted.charAt(0);
        return quoted.substring(1,quoted.length()-1).replace("\\"+quote,""+quote).replace("\\\\","\\");
    }
    private static void checkType(PickleStep step,PickleStepType expected,String label,List<String> problems) {
        if(step.getType().orElse(PickleStepType.UNKNOWN)!=expected) problems.add("Step keyword type mismatch "+label+": "+step.getText());
    }
}
