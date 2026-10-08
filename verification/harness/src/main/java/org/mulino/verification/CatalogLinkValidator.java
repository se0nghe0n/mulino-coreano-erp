package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/** Structural linkage only; supporting assertions and final semantic review remain necessary. */
final class CatalogLinkValidator {
    private static final Set<String> EXACT_QUANTITY_OPS=Set.of("decimalEquals","sumEquals","decimalDelta");
    /** Observer-derived data (ObserverDerivations); its derivation is chosen by the observer, not pinned by the case. */
    static final String OBSERVER_DERIVED="/data/data/";
    private CatalogLinkValidator() {}

    static void validate(ContractValidator validator,JsonNode catalog,Map<String,JsonNode> cases,List<String> problems) throws IOException {
        validate(validator,catalog,cases,problems,new ArrayList<>());
    }
    /**
     * attributionGaps: db_snapshot observations whose linked subcases declare no DB adapter at all. That is a
     * catalog-layer versus case-profile mismatch (coverage owner), reported but not double-gated here.
     */
    static void validate(ContractValidator validator,JsonNode catalog,Map<String,JsonNode> cases,List<String> problems,List<String> attributionGaps) throws IOException {
        Map<String,JsonNode> oracles=new HashMap<>();
        for(JsonNode oracle:catalog.path("oracles")) oracles.put(oracle.path("oracleId").asText(),oracle);
        Set<String> covered=new HashSet<>(),fixedQuantities=new HashSet<>(),dbObserved=new HashSet<>(),oracleSubcasesWithDb=new HashSet<>();
        Map<String,Set<String>> linkingSubcases=new HashMap<>();Set<String> dbAdapterSubcases=new HashSet<>();
        for(JsonNode c:cases.values()) for(JsonNode sub:c.path("subcases")) {
          Map<String,String> kinds=new HashMap<>();collectKinds(sub.path("actions"),kinds);
          String subKey=c.path("caseId").asText()+"/"+sub.path("id").asText();
          if(validator.adapters(sub.path("requiredAdapters")).contains("db")) dbAdapterSubcases.add(subKey);
          for(JsonNode assertion:sub.path("assertions")) {
            JsonNode ref=assertion.path("oracleRef");String id=ref.path("oracleId").asText();JsonNode oracle=oracles.get(id);
            if(oracle==null) {problems.add("Unknown independent oracle "+id);continue;}
            if(!oracle.path("caseId").asText().equals(c.path("caseId").asText())) {
                problems.add("Oracle points at different case "+id);continue;
            }
            // db_snapshot artifacts come only from an independent observe action (source or baseline).
            boolean observeSourced="observe".equals(kinds.get(assertion.path("source").path("actionId").asText()))
                || "observe".equals(kinds.get(assertion.path("baseline").path("actionId").asText()));
            if(observeSourced) oracleSubcasesWithDb.add(id+"@"+subKey);
            for(JsonNode name:ref.path("observationNames")) {
                JsonNode observation=null;
                for(JsonNode item:oracle.path("expectedObservations")) if(item.path("name").asText().equals(name.asText())) observation=item;
                String key=id+"/"+name.asText();
                if(observation==null) {problems.add("Unknown oracle observation "+key);continue;}
                covered.add(key);linkingSubcases.computeIfAbsent(key,k->new HashSet<>()).add(subKey);
                if(observeSourced) dbObserved.add(key);
                if(observation.path("type").asText().equals("quantity") && fixedQuantityAssertion(assertion,observation)) fixedQuantities.add(key);
                // Relations, identities, counts, and other quantity aspects may support this same
                // observation. They need not repeat its aggregate value; none replaces its primary.
            }
          }
        }
        for(JsonNode oracle:catalog.path("oracles")) for(JsonNode observation:oracle.path("expectedObservations")) {
            String key=oracle.path("oracleId").asText()+"/"+observation.path("name").asText();
            if(!covered.contains(key)) problems.add("Unlinked normative observation "+key);
            else if(observation.path("type").asText().equals("quantity") && !fixedQuantities.contains(key))
                problems.add("Missing substantive fixed quantity assertion (value/unit/operator) "+key);
            if(covered.contains(key) && contains(observation.path("artifactKinds"),"db_snapshot") && !dbObserved.contains(key)) {
                // A sibling observation of the same oracle observed in the same subcase shares that DB snapshot.
                boolean shared=false;
                for(String subKey:linkingSubcases.getOrDefault(key,Set.of())) shared|=oracleSubcasesWithDb.contains(oracle.path("oracleId").asText()+"@"+subKey);
                boolean declaresDb=false;for(String subKey:linkingSubcases.getOrDefault(key,Set.of())) declaresDb|=dbAdapterSubcases.contains(subKey);
                if(!shared && declaresDb) problems.add("Observation requires db_snapshot artifact but no linked subcase observes the independent DB "+key);
                else if(!shared) attributionGaps.add("db_snapshot artifactKind with no DB adapter in any linked subcase (catalog layer vs case profile) "+key);
            }
        }
        for(JsonNode source:catalog.path("sourceFiles"))
            if(!Json.sha256(validator.path(source.path("path").asText())).equals(source.path("sha256").asText()))
                problems.add("Normative source hash drift "+source.path("path"));
        requiredProfileReachability(catalog,cases,problems);
    }

    /** Catalog requiredLayers to execution profiles; identical to verification/coverage/assemble.py LAYER_PROFILE. */
    static final Map<String,String> LAYER_PROFILE=Map.of("UNIT","contracts","API","scenarios","DB","scenarios","MCP","mcp","SKILLS","skills",
        "MODEL","model","LOCAL_DEPLOYMENT","local-deployment","BTP_DEPLOYMENT","btp-deployment","REGULATORY_REVIEW","regulatory");
    static final Set<String> PROFILES=Set.of("schema","contracts","scenarios","recovery","mcp","skills","model","local-deployment","btp-deployment","regulatory");

    /**
     * Same rule as the coverage assembler: an observation whose oracle requiredLayers demand a profile on which no
     * linked case assertion executes can never reach a clause state, so it would stay NOT_RUN forever while the
     * declaration looked complete. Links exist only on the case's declared profiles; {@code deployment} expands to
     * local-deployment and btp-deployment, and a subcase asserting a REGULATORY_REVIEW observation is also on the
     * separate regulatory evidence profile. Declaring the profile is necessary, not sufficient (case review).
     */
    static void requiredProfileReachability(JsonNode catalog,Map<String,JsonNode> cases,List<String> problems) {
        Map<String,JsonNode> oracles=new HashMap<>();
        for(JsonNode oracle:catalog.path("oracles")) oracles.put(oracle.path("oracleId").asText(),oracle);
        Map<String,Set<String>> linked=new LinkedHashMap<>();
        for(JsonNode c:cases.values()) {
            String caseId=c.path("caseId").asText();
            List<String> declared=new ArrayList<>();
            for(JsonNode p:c.path("profiles")) if(p.asText().equals("deployment")) {declared.add("local-deployment");declared.add("btp-deployment");} else declared.add(p.asText());
            Set<String> unknown=new TreeSet<>(declared);unknown.removeAll(PROFILES);
            if(!unknown.isEmpty()) problems.add("Case declares unknown verification profile(s): "+caseId+" "+unknown);
            for(JsonNode sub:c.path("subcases")) {
                Set<String> subProfiles=new LinkedHashSet<>(declared);
                for(JsonNode assertion:sub.path("assertions")) {
                    JsonNode oracle=oracles.get(assertion.path("oracleRef").path("oracleId").asText());
                    if(oracle==null || !oracle.path("caseId").asText().equals(caseId) || !contains(oracle.path("requiredLayers"),"REGULATORY_REVIEW")) continue;
                    for(JsonNode name:assertion.path("oracleRef").path("observationNames")) for(JsonNode o:oracle.path("expectedObservations"))
                        if(o.path("name").asText().equals(name.asText())) subProfiles.add("regulatory");
                }
                for(JsonNode assertion:sub.path("assertions")) {
                    JsonNode ref=assertion.path("oracleRef");JsonNode oracle=oracles.get(ref.path("oracleId").asText());
                    if(oracle==null || !oracle.path("caseId").asText().equals(caseId)) continue;
                    for(JsonNode name:ref.path("observationNames")) {
                        boolean known=false;for(JsonNode o:oracle.path("expectedObservations")) known|=o.path("name").asText().equals(name.asText());
                        if(known) linked.computeIfAbsent(ref.path("oracleId").asText()+"/"+name.asText(),k->new TreeSet<>()).addAll(subProfiles);
                    }
                }
            }
        }
        for(JsonNode oracle:catalog.path("oracles")) {
            Set<String> required=new TreeSet<>();
            for(JsonNode layer:oracle.path("requiredLayers")) if(LAYER_PROFILE.containsKey(layer.asText())) required.add(LAYER_PROFILE.get(layer.asText()));
            for(JsonNode observation:oracle.path("expectedObservations")) {
                String key=oracle.path("oracleId").asText()+"/"+observation.path("name").asText();
                Set<String> profiles=linked.get(key);
                if(profiles==null) continue; // reported as an unlinked normative observation
                for(String profile:required) if(!profiles.contains(profile))
                    problems.add("Unreachable required profile: "+key+" requires "+profile+" but case "+oracle.path("caseId").asText()+" links no assertion on that profile");
            }
        }
    }

    private static boolean fixedQuantityAssertion(JsonNode assertion,JsonNode observation) {
        String op=assertion.path("op").asText();
        boolean quantityOp=switch(observation.path("operator").asText()) {
            case "eq" -> EXACT_QUANTITY_OPS.contains(op);
            case "lte" -> op.equals("decimalAtMost");
            case "gte" -> op.equals("decimalAtLeast");
            default -> false;
        };
        if(!quantityOp || !decimalConstant(assertion.path("expected")) || !decimalConstant(observation.path("expected").path("value"))) return false;
        if(new BigDecimal(assertion.path("expected").asText()).compareTo(new BigDecimal(observation.path("expected").path("value").asText()))!=0) return false;
        JsonNode expectedUnit=observation.path("expected").path("unit");
        if(!expectedUnit.isTextual() || expectedUnit.asText().isBlank() || !expectedUnit.equals(assertion.path("unit"))) return false;
        if(!observedSource(assertion.path("source")) || !observedSource(assertion.path("unitSource"))) return false;
        // A fixed-quantity primary reads authoritative rows or the server response, never an observer-derived /data/data
        // value: the observer chooses the derivation's filter and aggregate, so arithmetic consistency would not prove the
        // business filter (step2r round 5). Derived values may still support the observation.
        for(String field:List.of("source","baseline","unitSource","baselineUnitSource")) if(assertion.path(field).path("pointer").asText().startsWith(OBSERVER_DERIVED)) return false;
        return !op.equals("decimalDelta") || (observedSource(assertion.path("baseline")) && observedSource(assertion.path("baselineUnitSource"))
            && !assertion.path("source").equals(assertion.path("baseline")));
    }
    private static void collectKinds(JsonNode actions,Map<String,String> kinds) {
        for(JsonNode a:actions) {
            kinds.put(a.path("id").asText(),a.path("kind").asText());
            if(a.has("call")) kinds.put(a.path("call").path("id").asText(),a.path("call").path("kind").asText());
            for(JsonNode b:a.path("branches")) collectKinds(b.path("actions"),kinds);
        }
    }
    private static boolean contains(JsonNode array,String value) { for(JsonNode n:array) if(n.asText().equals(value)) return true; return false; }
    private static boolean decimalConstant(JsonNode node) {
        return node.isTextual() && node.asText().matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?");
    }
    private static boolean observedSource(JsonNode source) {
        return source.isObject() && source.path("actionId").isTextual() && !source.path("actionId").asText().isBlank()
            && source.path("pointer").isTextual() && source.path("pointer").asText().startsWith("/");
    }
}
