package com.mulino.domain.evaluation;

import com.mulino.domain.definitions.PredicateTruth;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static com.mulino.domain.definitions.PredicateTruth.State.*;
import static com.mulino.domain.evaluation.EvaluationFacts.*;

/** Bounded interpreter for published predicates only. No SQL, script or arbitrary path access. */
public final class TypedPredicateEvaluator {
  public static final String VERSION="core-v1";
  public record Condition(String conditionId, PredicateTruth truth, List<Fact> inputs, List<String> evidenceRefs, String evaluatorVersion) {}
  public record Result(PredicateTruth truth,List<Condition> conditions) {}
  public Result evaluate(Map<String,Object> node, EvaluationFacts facts, Instant asOf, Instant knownAt) {
    var results=new ArrayList<Condition>(); var truth=evaluate(node,facts,asOf,knownAt,"root",0,new int[]{0},results);
    return new Result(truth,List.copyOf(results));
  }
  private PredicateTruth evaluate(Map<String,Object> n,EvaluationFacts facts,Instant at,Instant known,String id,int depth,int[] count,List<Condition> out) {
    if(depth>32||++count[0]>1000)throw new IllegalArgumentException("Predicate bounds exceeded");
    String op=Objects.toString(n.get("operator"),"");PredicateTruth result;
    List<Fact> inputs=List.of();
    if(Set.of("all","any","not").contains(op)) {
      if(!(n.get("children") instanceof List<?> children)||children.isEmpty()||children.size()>100||op.equals("not")&&children.size()!=1)throw new IllegalArgumentException("Predicate arity");
      var values=new ArrayList<PredicateTruth>();int i=0;
      for(var child:children) {if(!(child instanceof Map<?,?> m))throw new IllegalArgumentException("Predicate child"); values.add(evaluate(map(m),facts,at,known,id+"."+i++,depth+1,count,out));}
      result=op.equals("all")?PredicateTruth.all(values):op.equals("any")?PredicateTruth.any(values):values.getFirst().not();
    } else {
      if(!Set.of("equals","in","compare","range","exists","cardinality","timeIn","quantitySum","stateQuantity").contains(op))throw new IllegalArgumentException("Unsupported operator");
      inputs=(op.equals("cardinality")?facts.relations().getOrDefault(Objects.toString(n.get("relation")),List.of()):facts.properties().getOrDefault(Objects.toString(n.get("property")),List.of())).stream().filter(f->!f.recordedAt().isAfter(known)).toList();
      if(op.equals("stateQuantity"))inputs=inputs.stream().filter(f->covers(f,at)).toList();
      boolean conflict=inputs.stream().anyMatch(f->f.state()==State.CONFLICT);
      List<Fact> values=inputs.stream().filter(f->f.state()==State.KNOWN&&f.verified()).toList();
      boolean uncertain=inputs.isEmpty()||values.size()!=inputs.size();
      if(op.equals("exists")) result=new PredicateTruth(!values.isEmpty()?SATISFIED:UNVERIFIED,conflict);
      else if(op.equals("quantitySum")||op.equals("stateQuantity")) {
        var scopes=new HashMap<String,Fact>();boolean duplicateConflict=false;
        for(Fact f:values) {
          String key=op.equals("quantitySum")?f.physicalScopeId():f.sourceId();
          if(key==null||op.equals("quantitySum")&&f.canonicalId()==null){uncertain=true;continue;}
          Fact previous=scopes.putIfAbsent(key,f);
          if(previous!=null&&!Objects.equals(previous.value(),f.value()))duplicateConflict=true;
        }
        BigDecimal total=BigDecimal.ZERO;
        for(Fact f:scopes.values()){ if(!Objects.equals(n.get("unit"),f.unit())){uncertain=true;continue;} total=total.add(decimal(f.value())); }
        result=uncertain||duplicateConflict?new PredicateTruth(UNVERIFIED,conflict||duplicateConflict):new PredicateTruth(bounds(total,n),conflict);
      } else if(op.equals("cardinality")) {
        long total=values.stream().filter(f->covers(f,at)).map(Fact::value).distinct().count();
        result=uncertain?new PredicateTruth(UNVERIFIED,conflict):new PredicateTruth(bounds(BigDecimal.valueOf(total),n),conflict);
      } else {
        var answers=new ArrayList<PredicateTruth>();
        for(Fact f:values) {
          if(Set.of("compare","range").contains(op)&&!Objects.equals(n.get("unit"),f.unit())){answers.add(new PredicateTruth(UNVERIFIED,conflict));continue;}
          boolean okay=switch(op) {
            case "equals" -> typedEquals(f,n.get("value"));
            case "in" -> n.get("values") instanceof List<?> l&&l.stream().anyMatch(x->typedEquals(f,x));
            case "compare","range" -> Objects.equals(n.get("unit"),f.unit())&&bounds(decimal(f.value()),n)==SATISFIED;
            case "timeIn" -> timeIn(instant(f.value()),n);
            default -> false;
          };answers.add(new PredicateTruth(okay?SATISFIED:UNSATISFIED,conflict));
        }
        if(uncertain)answers.add(new PredicateTruth(UNVERIFIED,conflict));
        result=answers.isEmpty()?new PredicateTruth(UNVERIFIED,conflict):PredicateTruth.all(answers);
      }
    }
    out.add(new Condition(id,result,List.copyOf(inputs),inputs.stream().flatMap(f->f.evidenceRefs().stream()).distinct().sorted().toList(),VERSION));return result;
  }
  public Result evaluateInterval(Map<String,Object> node,EvaluationFacts facts,Instant start,Instant end,Instant known,boolean throughout) {
    if(!start.isBefore(end))throw new IllegalArgumentException("Invalid interval");
    var boundaries=new TreeSet<Instant>();boundaries.add(start);boundaries.add(end);
    facts.properties().values().stream().flatMap(Collection::stream).filter(f->!f.recordedAt().isAfter(known)).forEach(f->{if(f.effectiveFrom()!=null&&f.effectiveFrom().isAfter(start)&&f.effectiveFrom().isBefore(end))boundaries.add(f.effectiveFrom());if(f.effectiveUntil()!=null&&f.effectiveUntil().isAfter(start)&&f.effectiveUntil().isBefore(end))boundaries.add(f.effectiveUntil());});
    var answers=new ArrayList<PredicateTruth>();var conditions=new ArrayList<Condition>();
    for(Instant at:boundaries.headSet(end)) {
      var p=new HashMap<String,List<Fact>>();facts.properties().forEach((key,values)->p.put(key,values.stream().filter(f->covers(f,at)).toList()));
      var r=evaluate(node,new EvaluationFacts(p,facts.relations()),at,known);answers.add(r.truth());conditions.addAll(r.conditions());
    }
    return new Result(throughout?PredicateTruth.all(answers):PredicateTruth.any(answers),List.copyOf(conditions));
  }
  private static boolean covers(Fact f,Instant at){return f.effectiveFrom()!=null&&!at.isBefore(f.effectiveFrom())&&(f.effectiveUntil()==null||at.isBefore(f.effectiveUntil()));}
  private static boolean typedEquals(Fact f,Object other){if(other instanceof Map<?,?> m)return Objects.equals(f.unit(),m.get("unit"))&&decimal(f.value()).compareTo(decimal(m.get("value")))==0;return Objects.equals(f.value(),other);}
  private static PredicateTruth.State bounds(BigDecimal v,Map<String,Object> n){if(!n.containsKey("minimum")&&!n.containsKey("maximum"))throw new IllegalArgumentException("Bounds required");return n.containsKey("minimum")&&v.compareTo(bound(n.get("minimum")))<0||n.containsKey("maximum")&&v.compareTo(bound(n.get("maximum")))>0?UNSATISFIED:SATISFIED;}
  private static BigDecimal bound(Object v){return decimal(v instanceof Map<?,?> m?m.get("value"):v);}
  private static BigDecimal decimal(Object v){return v instanceof BigDecimal b?b:new BigDecimal(v.toString());}
  private static Instant instant(Object v){return v instanceof Instant i?i:OffsetDateTime.parse(v.toString()).toInstant();}
  private static boolean timeIn(Instant v,Map<String,Object> n){ZoneId.of(n.get("timezone").toString());return (!n.containsKey("minimum")||(Boolean.TRUE.equals(n.get("inclusiveStart"))?!v.isBefore(instant(n.get("minimum"))):v.isAfter(instant(n.get("minimum")))))&&(!n.containsKey("maximum")||(Boolean.TRUE.equals(n.get("inclusiveEnd"))?!v.isAfter(instant(n.get("maximum"))):v.isBefore(instant(n.get("maximum")))));}
  private static Map<String,Object> map(Map<?,?> m){var r=new HashMap<String,Object>();m.forEach((k,v)->r.put(k.toString(),v));return r;}
}
