package com.mulino.domain.definitions;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import com.mulino.application.core.CommandHandler;

/** Bounded, structural validation. Validation never grants execution authority. */
@Component
public class DefinitionValidator {
  private final ObjectProvider<CommandHandler> handlers;
  public DefinitionValidator(){this.handlers=null;}
  @Autowired public DefinitionValidator(ObjectProvider<CommandHandler> handlers){this.handlers=handlers;}
  private boolean supportedCapability(Definition.Capability capability){
    if(Set.of("getDefinition","getObject","searchObjects","getWork","searchWorks","getInventory","getEvidence","getAssessment","getObligations","getReconciliation").contains(capability.capabilityId()))return true;
    return handlers!=null&&handlers.orderedStream().anyMatch(h->h.capabilities().contains(capability.capabilityId())&&h.semanticVersion().equals(capability.semanticVersion()));
  }
  private static final Set<String> RESERVED=Set.of("eligible","eligibility","role","roles","remainingquantity","assessment","permissions");
  private static final Set<String> OPERATORS=Set.of("equals","in","compare","range","exists","cardinality","timeIn","all","any","not","quantitySum","stateQuantity");
  public record Problem(String path,String code) {}
  public record Result(String outcome,List<Problem> problems) {
    public Result { problems=List.copyOf(problems); }
  }
  public Result validate(Definition d) {
    var p=new ArrayList<Problem>(); var names=new HashSet<String>();
    for(var n:d.nouns()) if(!names.add(n.name())) p.add(new Problem(n.name(),"DUPLICATE_TYPE"));
    var keys=new HashSet<String>();
    for(var a:d.attributes()) {
      String path=a.nounType()+"."+a.name();
      if(!keys.add(path)) p.add(new Problem(path,"DUPLICATE_PROPERTY"));
      if(!names.contains(a.nounType())) p.add(new Problem(path,"UNKNOWN_TYPE"));
      if(RESERVED.contains(a.name().toLowerCase(Locale.ROOT))) p.add(new Problem(path,"RESERVED_CORE"));
      if(a.minimumCount()<0||a.maximumCount()<a.minimumCount()) p.add(new Problem(path,"CARDINALITY"));
      if(a.type()==Definition.ValueType.REFERENCE&&!names.contains(a.referenceType())) p.add(new Problem(path,"REFERENCE_TYPE"));
      if(a.type()==Definition.ValueType.DECIMAL&&(a.unit()==null||a.unit().isBlank()||a.decimalPlaces()<0||a.decimalPlaces()>12)) p.add(new Problem(path,"UNIT_PRECISION"));
    }
    var relationNames=new HashSet<String>();
    for(var r:d.relations()) {
      if(!relationNames.add(r.name())) p.add(new Problem(r.name(),"DUPLICATE_RELATION"));
      if(!names.contains(r.sourceType())||!names.contains(r.targetType())) p.add(new Problem(r.name(),"ENDPOINT_TYPE"));
      if(r.minimumCount()<0||r.maximumCount()<r.minimumCount()) p.add(new Problem(r.name(),"CARDINALITY"));
    }
    var capabilityNames=new HashSet<String>();
    for(var capability:d.capabilities()) {
      if(!capabilityNames.add(capability.capabilityId())) p.add(new Problem(capability.capabilityId(),"DUPLICATE_CAPABILITY"));
      if(!supportedCapability(capability)) p.add(new Problem(capability.capabilityId(),"UNSUPPORTED_CAPABILITY"));
      if(!"core-v1".equals(capability.evaluatorVersion())||!"1.0.0".equals(capability.inputSchemaVersion())||!"1.0.0".equals(capability.outputSchemaVersion())) p.add(new Problem(capability.capabilityId(),"UNSUPPORTED_RUNTIME_CONTRACT"));
    }
    for(var v:d.verbs()) {
      if(!Set.of("QUERY","RECORD","COMMAND").contains(v.intentKind())) p.add(new Problem(v.name(),"INTENT_KIND"));
      if(d.capabilities().stream().noneMatch(c->c.capabilityId().equals(v.capabilityId()))) p.add(new Problem(v.name(),"UNSUPPORTED_CAPABILITY"));
      for(var slot:v.slots().entrySet()) if(!names.contains(slot.getValue())&&!Set.of("DECIMAL","STRING","INSTANT","DATE","BOOLEAN").contains(slot.getValue())) p.add(new Problem(v.name()+"."+slot.getKey(),"SLOT_TYPE"));
    }
    for(var g:d.goals()) {
      if(!Set.of("CUMULATIVE_EVENT","STATE_AT","EXISTS_IN","THROUGHOUT").contains(g.quantityMode())||g.endpoint()==null||g.endpoint().isBlank()) p.add(new Problem(g.name(),"GOAL_INCOMPLETE"));
      if(d.capabilities().stream().noneMatch(c->c.evaluatorVersion().equals(g.evaluatorVersion()))) p.add(new Problem(g.name(),"UNSUPPORTED_EVALUATOR"));
      predicate(d,g.predicate(),g.name(),p,0,new int[]{0});
    }
    return result(p);
  }
  public Result validateIntent(Definition d,String verbName,String intentKind,String stage,Map<String,Object> slots) {
    var p=new ArrayList<Problem>();
    var verb=d.verbs().stream().filter(v->v.name().equals(verbName)).findFirst();
    if(verb.isEmpty()) return new Result("UNSUPPORTED",List.of(new Problem(verbName,"UNSUPPORTED_VERB")));
    if(!verb.get().intentKind().equals(intentKind)) p.add(new Problem(verbName,"INTENT_KIND"));
    if(!verb.get().stage().equals(stage)) p.add(new Problem(verbName,"STAGE"));
    for(var slot:verb.get().slots().entrySet()) {
      var attribute=d.attributes().stream().filter(a->a.name().equals(slot.getKey())).findFirst();
      if(!slots.containsKey(slot.getKey())) p.add(new Problem(slot.getKey(),"MISSING_SLOT"));
      else if(attribute.isPresent()) p.addAll(validateValue(attribute.get(),slots.get(slot.getKey())).problems());
      else if(!typedSlot(slot.getValue(),slots.get(slot.getKey()))) p.add(new Problem(slot.getKey(),"SLOT_TYPE"));
    }
    for(var key:slots.keySet()) if(!verb.get().slots().containsKey(key)) p.add(new Problem(key,"UNKNOWN_SLOT"));
    return result(p);
  }
  private boolean typedSlot(String type,Object value) {
    try {
      return switch(type) {
        case "STRING" -> value instanceof String;
        case "BOOLEAN" -> value instanceof Boolean;
        case "INSTANT" -> value instanceof String s && OffsetDateTime.parse(s)!=null;
        case "DATE" -> value instanceof String s && LocalDate.parse(s)!=null;
        default -> value instanceof Map<?,?> m && type.equals(m.get("type")) && m.get("id") instanceof String id && id.length()==36 && UUID.fromString(id)!=null;
      };
    } catch(RuntimeException e) {return false;}
  }
  public Result compatibility(Definition d,String capability,String evaluator,String inputSchema,String outputSchema) {
    boolean supported=d.capabilities().stream().anyMatch(c->c.capabilityId().equals(capability)&&c.evaluatorVersion().equals(evaluator)&&c.inputSchemaVersion().equals(inputSchema)&&c.outputSchemaVersion().equals(outputSchema));
    return new Result(supported?"VALID":"HELD_UNSUPPORTED",supported?List.of():List.of(new Problem(d.version(),"UNSUPPORTED_PINNED_CONTRACT")));
  }
  public Result validateValue(Definition.Attribute a,Object value) {
    var p=new ArrayList<Problem>();
    try {
      boolean valid=switch(a.type()) {
        case STRING -> value instanceof String text && text.length()<=4096;
        case BOOLEAN -> value instanceof Boolean;
        case DATE -> value instanceof String s && LocalDate.parse(s)!=null;
        case INSTANT -> value instanceof String s && OffsetDateTime.parse(s)!=null;
        case REFERENCE -> value instanceof Map<?,?> m && m.keySet().equals(Set.of("id","type")) && m.get("id") instanceof String id && id.length()==36 && UUID.fromString(id)!=null && a.referenceType().equals(m.get("type"));
        case DECIMAL -> decimal(a,value);
      };
      if(!valid) p.add(new Problem(a.name(),"VALUE_TYPE_OR_UNIT"));
    } catch(RuntimeException e) { p.add(new Problem(a.name(),"VALUE_TYPE_OR_UNIT")); }
    return result(p);
  }
  private boolean decimal(Definition.Attribute a,Object value) {
    if(!(value instanceof Map<?,?> m)||!m.keySet().equals(Set.of("value","unit"))||!(m.get("value") instanceof String s)||!a.unit().equals(m.get("unit"))||s.length()>40||!s.matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?")) return false;
    var n=new BigDecimal(s); return n.scale()<=a.decimalPlaces()&&n.scale()<=12&&n.precision()-n.scale()<=26;
  }
  public Result validateRelation(Definition.Relation r,String sourceType,List<String> targetTypes) {
    var p=new ArrayList<Problem>();
    if(!r.sourceType().equals(sourceType)||targetTypes.stream().anyMatch(t->!r.targetType().equals(t))) p.add(new Problem(r.name(),"ENDPOINT_TYPE"));
    if(targetTypes.size()<r.minimumCount()||targetTypes.size()>r.maximumCount()) p.add(new Problem(r.name(),"CARDINALITY"));
    return result(p);
  }
  public Result validatePredicate(Definition d,Map<String,Object> predicate) {
    var p=new ArrayList<Problem>();predicate(d,predicate,"predicate",p,0,new int[]{0});return result(p);
  }
  private void predicate(Definition d,Map<String,Object> node,String path,List<Problem> p,int depth,int[] budget) {
    if(++budget[0]>1000) {p.add(new Problem(path,"NODE_LIMIT"));return;}
    if(depth>32) {p.add(new Problem(path,"DEPTH_LIMIT"));return;}
    Object op=node.get("operator");
    if(!(op instanceof String s)||!OPERATORS.contains(s)) {p.add(new Problem(path,"UNSUPPORTED_OPERATOR"));return;}
    Set<String> fields=switch(s) {
      case "all","any","not" -> Set.of("operator","children");
      case "equals" -> Set.of("operator","property","value","evidenceSelector");
      case "in" -> Set.of("operator","property","values","evidenceSelector");
      case "exists" -> Set.of("operator","property","evidenceSelector");
      case "cardinality" -> Set.of("operator","relation","minimum","maximum");
      case "timeIn" -> Set.of("operator","property","minimum","maximum","timezone","inclusiveStart","inclusiveEnd","evidenceSelector");
      default -> Set.of("operator","property","minimum","maximum","unit","evidenceSelector");
    };
    if(!fields.containsAll(node.keySet())) p.add(new Problem(path,"UNSUPPORTED_FIELD"));
    if(Set.of("all","any","not").contains(s)) {
      if(!(node.get("children") instanceof List<?> children)||children.isEmpty()||children.size()>100||(s.equals("not")&&children.size()!=1)) {p.add(new Problem(path,"ARITY"));return;}
      for(Object child:children) if(child instanceof Map<?,?> m) predicate(d,stringMap(m),path+".children",p,depth+1,budget);else p.add(new Problem(path,"TYPE"));
      return;
    }
    if(s.equals("cardinality")) {
      var r=d.relations().stream().filter(x->x.name().equals(node.get("relation"))).findFirst();
      if(r.isEmpty()) p.add(new Problem(path,"UNKNOWN_RELATION"));
      else {
        if(!(node.get("minimum") instanceof Integer min)||min<r.get().minimumCount()) p.add(new Problem(path,"CARDINALITY"));
        if(!(node.get("maximum") instanceof Integer max)||max>r.get().maximumCount()||max<r.get().minimumCount()) p.add(new Problem(path,"CARDINALITY"));
        if(node.get("minimum") instanceof Integer min&&node.get("maximum") instanceof Integer max&&min>max)p.add(new Problem(path,"REVERSED_BOUNDS"));
      }
      return;
    }
    var a=d.attributes().stream().filter(x->(x.nounType()+"."+x.name()).equals(node.get("property"))).findFirst();
    if(a.isEmpty()) {p.add(new Problem(path,"UNKNOWN_PROPERTY"));return;}
    if(Set.of("compare","range","quantitySum","stateQuantity").contains(s)&&a.get().type()!=Definition.ValueType.DECIMAL) p.add(new Problem(path,"NUMERIC_TYPE"));
    if(s.equals("timeIn")&&(a.get().type()!=Definition.ValueType.INSTANT||!(node.get("timezone") instanceof String tz)||!validZone(tz)||!(node.get("inclusiveStart") instanceof Boolean)||!(node.get("inclusiveEnd") instanceof Boolean))) p.add(new Problem(path,"TIME_SEMANTICS"));
    if(node.containsKey("evidenceSelector")&&!Set.of("VERIFIED_DISTINCT","CURRENT_STATE").contains(node.get("evidenceSelector"))) p.add(new Problem(path,"EVIDENCE_SELECTOR"));
    if(node.containsKey("unit")&&!Objects.equals(a.get().unit(),node.get("unit"))) p.add(new Problem(path,"UNIT_MISMATCH"));
    if(Set.of("quantitySum","stateQuantity").contains(s)) {
      String selector=s.equals("quantitySum")?"VERIFIED_DISTINCT":"CURRENT_STATE";
      if(!selector.equals(node.get("evidenceSelector"))) p.add(new Problem(path,"EVIDENCE_SELECTOR_REQUIRED"));
      if(!Objects.equals(a.get().unit(),node.get("unit"))) p.add(new Problem(path,"UNIT_REQUIRED"));
    }
    if(s.equals("equals")) p.addAll(validateValue(a.get(),node.get("value")).problems());
    if(s.equals("in")) {
      if(!(node.get("values") instanceof List<?> values)||values.isEmpty()||values.size()>100) p.add(new Problem(path,"TYPE"));
      else for(Object value:values) p.addAll(validateValue(a.get(),value).problems());
    }
    if(Set.of("compare","range","quantitySum","stateQuantity","timeIn").contains(s)) {
      if(!node.containsKey("minimum")&&!node.containsKey("maximum")) p.add(new Problem(path,"BOUNDS_REQUIRED"));
      for(String bound:List.of("minimum","maximum")) if(node.containsKey(bound)) p.addAll(validateValue(a.get(),node.get(bound)).problems());
      if(node.containsKey("minimum")&&node.containsKey("maximum")&&validateValue(a.get(),node.get("minimum")).problems().isEmpty()&&validateValue(a.get(),node.get("maximum")).problems().isEmpty()) {
        int ordering=0;
        if(a.get().type()==Definition.ValueType.DECIMAL) ordering=new BigDecimal((String)((Map<?,?>)node.get("minimum")).get("value")).compareTo(new BigDecimal((String)((Map<?,?>)node.get("maximum")).get("value")));
        if(a.get().type()==Definition.ValueType.INSTANT) ordering=OffsetDateTime.parse((String)node.get("minimum")).toInstant().compareTo(OffsetDateTime.parse((String)node.get("maximum")).toInstant());
        if(ordering>0) p.add(new Problem(path,"REVERSED_BOUNDS"));
      }
    }
  }
  private boolean validZone(String zone) { try {ZoneId.of(zone);return true;}catch(RuntimeException e){return false;} }
  private Map<String,Object> stringMap(Map<?,?> map) {var out=new HashMap<String,Object>();map.forEach((k,v)->out.put(String.valueOf(k),v));return out;}
  private Result result(List<Problem> p) {return new Result(p.isEmpty()?"VALID":p.stream().anyMatch(x->x.code().startsWith("UNSUPPORTED"))?"UNSUPPORTED":"INVALID",p);}
}
