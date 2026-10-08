package com.mulino.core;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mulino.application.core.CommandOutcomes;
import com.mulino.application.responsibility.ObligationClosureCatalog;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;
import org.junit.jupiter.api.Test;

/**
 * contracts/domain-vocabulary.json is the single list of what the backend may emit (plan §3.4, §5.3, §7).
 * The main sources are scanned statically: every DomainError/ResponsibilityHeld code and outcome, every literal
 * command outcome and every obligation kind must be declared, and every declared entry must still be emitted.
 */
class DomainVocabularyContractTest {
  static final Path BACKEND=Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
  static final Path MAIN=BACKEND.resolve("src/main/java/com/mulino");
  static final Path VOCABULARY=BACKEND.resolveSibling("contracts").resolve("domain-vocabulary.json");
  static final Pattern LITERAL=Pattern.compile("\"([A-Z][A-Z0-9_]+)\"");

  record Emission(String code,String outcome,String site){}

  static JsonNode vocabulary()throws Exception{return new ObjectMapper().readTree(VOCABULARY.toFile());}
  static Set<String> excluded(JsonNode v){var files=new TreeSet<String>();for(JsonNode e:v.path("excludedEmitters"))for(JsonNode s:e.path("sources")){String name=s.asText().split(" ")[0];if(name.endsWith(".java"))files.add(Path.of(name).getFileName().toString());}return files;}
  static List<Path> sources(Set<String> excluded)throws Exception{try(var walk=Files.walk(MAIN)){return walk.filter(p->p.toString().endsWith(".java")).filter(p->!excluded.contains(p.getFileName().toString())).sorted().toList();}}

  /** Top-level arguments of the call whose opening parenthesis ends at {@code start}; string literals are respected. */
  static List<String> arguments(String s,int start){
    var args=new ArrayList<String>();var current=new StringBuilder();int depth=1;boolean string=false;
    for(int i=start;depth>0;i++){
      char ch=s.charAt(i);
      if(string){current.append(ch);if(ch=='\\'){current.append(s.charAt(++i));}else if(ch=='"')string=false;continue;}
      if(ch=='"'){string=true;current.append(ch);}
      else if(ch=='('||ch=='['||ch=='{'){depth++;current.append(ch);}
      else if(ch==')'||ch==']'||ch=='}'){depth--;if(depth>0)current.append(ch);}
      else if(ch==','&&depth==1){args.add(current.toString().trim());current.setLength(0);}
      else current.append(ch);
    }
    args.add(current.toString().trim());return args;
  }
  static List<String> literals(String text){var m=LITERAL.matcher(text);var out=new ArrayList<String>();while(m.find())out.add(m.group(1));return out;}

  static List<Emission> emissions(Set<String> excluded)throws Exception{
    var out=new ArrayList<Emission>();var call=Pattern.compile("new (DomainError|ResponsibilityHeld)\\(");
    for(Path file:sources(excluded)){
      String s=Files.readString(file);var m=call.matcher(s);
      while(m.find()){
        var args=arguments(s,m.end());String site=MAIN.relativize(file)+":"+(s.substring(0,m.start()).split("\n",-1).length);
        if(m.group(1).equals("DomainError")){
          if(args.size()!=3)continue;// the constructor declaration itself
          var outcomes=literals(args.get(0));var codes=literals(args.get(1));
          assertFalse(codes.isEmpty(),"DomainError without a literal code at "+site);
          for(String code:codes){if(outcomes.isEmpty())out.add(new Emission(code,null,site));else for(String outcome:outcomes)out.add(new Emission(code,outcome,site));}
        }else{
          var codes=literals(args.get(0));assertFalse(codes.isEmpty(),"ResponsibilityHeld without a literal code at "+site);
          for(String code:codes)out.add(new Emission(code,"HELD",site));
        }
      }
    }
    return out;
  }

  @Test void everyEmittedErrorCodeAndOutcomeIsDeclaredAndEveryDeclaredCodeIsEmitted()throws Exception{
    var v=vocabulary();var emissions=emissions(excluded(v));assertTrue(emissions.size()>100,"scanner found too few emitters: "+emissions.size());
    var declared=new TreeMap<String,Set<String>>();
    for(JsonNode c:v.path("errorCodes")){var outcomes=new TreeSet<String>();c.path("outcomes").forEach(o->outcomes.add(o.asText()));assertNull(declared.put(c.path("code").asText(),outcomes),"duplicate code "+c.path("code").asText());
      assertTrue(c.hasNonNull("meaning"),"meaning required: "+c.path("code").asText());assertTrue(c.hasNonNull("planRef")^c.hasNonNull("extension"),"planRef xor extension rationale required: "+c.path("code").asText());}
    var emitted=new TreeMap<String,Set<String>>();var problems=new ArrayList<String>();
    for(var e:emissions){
      var allowed=declared.get(e.code());
      if(allowed==null){problems.add("undeclared code "+e.code()+" at "+e.site());continue;}
      if(e.outcome()==null){if(!allowed.equals(CommandOutcomes.NOT_APPLIED))problems.add("dynamic-outcome code "+e.code()+" must declare every non-applied outcome");emitted.computeIfAbsent(e.code(),k->new TreeSet<>()).addAll(CommandOutcomes.NOT_APPLIED);}
      else{if(!allowed.contains(e.outcome()))problems.add("undeclared outcome "+e.outcome()+" for "+e.code()+" at "+e.site());emitted.computeIfAbsent(e.code(),k->new TreeSet<>()).add(e.outcome());}
    }
    for(var d:declared.entrySet())if(!d.getValue().equals(emitted.getOrDefault(d.getKey(),Set.of())))problems.add("declared "+d.getKey()+d.getValue()+" but emitted "+emitted.getOrDefault(d.getKey(),Set.of()));
    assertEquals(List.of(),problems);
    var outcomes=new TreeSet<String>();v.path("outcomes").forEach(o->outcomes.add(o.path("outcome").asText()));
    for(var d:declared.entrySet())assertTrue(outcomes.containsAll(d.getValue()),"code outcome outside vocabulary: "+d.getKey());
  }

  @Test void plan34CodesKeepPlanNamesAndContradictingNamesAreGone()throws Exception{
    var v=vocabulary();var byCode=new HashMap<String,JsonNode>();v.path("errorCodes").forEach(c->byCode.put(c.path("code").asText(),c));
    // Plan §3.4 named codes are declared with a plan reference, not as extensions.
    for(String code:List.of("TYPE_INVALID","VERSION_UNSUPPORTED","POLICY_UNRESOLVED","FORBIDDEN","STALE_REVISION","IDEMPOTENCY_CONFLICT","INSUFFICIENT_ELIGIBLE_QUANTITY","EVIDENCE_CONFLICT"))
      assertTrue(byCode.containsKey(code)&&byCode.get(code).path("planRef").asText().contains("§3.4"),code);
    assertEquals("[\"CONFLICT\"]",byCode.get("STALE_REVISION").path("outcomes").toString());
    assertEquals("[\"REJECTED\"]",byCode.get("INSUFFICIENT_ELIGIBLE_QUANTITY").path("outcomes").toString());
    for(String gone:List.of("REVISION_CONFLICT","QUANTITY_CONFLICT"))assertFalse(byCode.containsKey(gone),gone);
  }

  @Test void commandOutcomesMatchTheRegistryAndLiteralOutcomesAreDeclared()throws Exception{
    var v=vocabulary();var outcomes=new TreeSet<String>();v.path("outcomes").forEach(o->outcomes.add(o.path("outcome").asText()));
    assertEquals(new TreeSet<>(CommandOutcomes.ALL),outcomes);
    var notApplied=new TreeSet<String>();v.path("outcomes").forEach(o->{if(!o.path("applied").asBoolean())notApplied.add(o.path("outcome").asText());});
    assertEquals(new TreeSet<>(CommandOutcomes.NOT_APPLIED),notApplied);
    var retired=new TreeSet<String>();v.path("retiredOutcomes").forEach(o->retired.add(o.path("outcome").asText()));
    assertEquals(Set.of("PENDING_EXTERNAL"),retired);assertFalse(CommandOutcomes.ALL.contains("PENDING_EXTERNAL"));
    var schema=new ObjectMapper().readTree(VOCABULARY.resolveSibling("command-response.schema.json").toFile());
    var schemaOutcomes=new TreeSet<String>();schema.path("properties").path("outcome").path("enum").forEach(o->schemaOutcomes.add(o.asText()));
    assertEquals(outcomes,schemaOutcomes,"command-response.schema.json outcome enum");
    var allowed=new TreeSet<>(outcomes);for(String group:List.of("validationOutcomes","internalOutcomes"))v.path(group).forEach(o->allowed.add(o.path("outcome").asText()));
    var literal=Pattern.compile("(?:Map\\.of|put)\\(\\s*\"outcome\"\\s*,\\s*\"([A-Z_]+)\"");var problems=new ArrayList<String>();
    for(Path file:sources(excluded(v))){var m=literal.matcher(Files.readString(file));while(m.find())if(!allowed.contains(m.group(1)))problems.add(m.group(1)+" in "+MAIN.relativize(file));}
    assertEquals(List.of(),problems);
    var evidence=new TreeSet<String>();v.path("evidenceStatuses").forEach(o->evidence.add(o.asText()));
    String outcomesFile=Files.readString(MAIN.resolve("application/evidence/EvidenceCommandOutcomes.java"));
    var set=Pattern.compile("Set\\.of\\(([^)]*)\\)\\.contains\\(status\\)").matcher(outcomesFile);assertTrue(set.find());assertEquals(evidence,new TreeSet<>(literals(set.group(1))));
  }

  @Test void obligationKindsAreClosedAndMatchTheClosureCatalog()throws Exception{
    var v=vocabulary();var declared=new TreeMap<String,JsonNode>();v.path("obligationKinds").forEach(k->declared.put(k.path("kind").asText(),k));
    assertEquals(new TreeSet<>(ObligationClosureCatalog.kinds().keySet()),declared.keySet());
    for(var closure:ObligationClosureCatalog.kinds().values()){
      var k=declared.get(closure.kind());
      assertEquals(closure.authorityClass(),k.path("waiverAuthority").isNull()?null:k.path("waiverAuthority").asText(),closure.kind());
      assertEquals(closure.waiverCapability(),k.path("waiverDecisionCapability").isNull()?null:k.path("waiverDecisionCapability").asText(),closure.kind());
      assertEquals(closure.resolvable(),!k.path("resolveObligation").isNull(),closure.kind());
      assertEquals(closure.autoClosure()!=null,!k.path("autoClosure").isNull(),closure.kind());
      assertTrue(k.hasNonNull("createdBy"),closure.kind());
    }
    // Every catalog kind except the dynamic RETURN_<decision> family is created by a literal outside the catalog.
    var text=new StringBuilder();for(Path file:sources(excluded(v)))if(!file.getFileName().toString().equals("ObligationClosureCatalog.java"))text.append(Files.readString(file));
    var unused=declared.keySet().stream().filter(k->!ObligationClosureCatalog.RETURN_DECISIONS.stream().map(d->"RETURN_"+d).toList().contains(k)).filter(k->!text.toString().contains("\""+k+"\"")).collect(Collectors.toList());
    assertEquals(List.of(),unused);
    var statuses=new TreeSet<String>();v.path("obligationStatuses").path("values").forEach(s->statuses.add(s.asText()));assertEquals(Set.of("OPEN","RESOLVED","TRANSFERRED","WAIVED"),statuses);
    String returns=Files.readString(MAIN.resolve("application/trade/returns/ReturnCommands.java"));assertTrue(returns.contains("ObligationClosureCatalog.RETURN_DECISIONS.contains(decision)"));
    String service=Files.readString(MAIN.resolve("application/responsibility/ResponsibilityService.java"));assertTrue(service.contains("if(!ObligationClosureCatalog.known(kind))throw DomainError.invalid(\"Unknown obligation kind\")"));
  }
}
