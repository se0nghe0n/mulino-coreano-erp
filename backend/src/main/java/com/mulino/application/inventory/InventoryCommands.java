package com.mulino.application.inventory;

import com.mulino.application.core.*;
import com.mulino.domain.inventory.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;

/** All command adapters dispatch here through the common envelope transaction. */
@Component
public final class InventoryCommands implements CommandHandler {
  private final InventoryRepository repository; private final StockPrimitives stock; private final ReadAuthorizer authorizer; private final InventoryRestrictionGuard restrictions;
  public InventoryCommands(InventoryRepository repository,StockPrimitives stock,ReadAuthorizer authorizer,InventoryRestrictionGuard restrictions){this.repository=repository;this.stock=stock;this.authorizer=authorizer;this.restrictions=restrictions;}
  public Set<String> capabilities(){return Set.of("splitQuantity","mergeQuantity","moveQuantity","recordStocktake","adjustQuantity","disposeQuantity");}
  public Set<String> intentKinds(){return Set.of("COMMAND","RECORD");}
  public CommandPreparation prepare(DomainContext c,Map<String,Object> intent) {
    String capability=text(intent,"capabilityId",100);var slots=slots(intent);Instant at=time(c,slots);
    Set<String> allowed=switch(capability) {
      case "splitQuantity" -> Set.of("segmentId","quantities","unit","occurredAt","evidenceRef");
      case "mergeQuantity" -> Set.of("segmentIds","expectedRevisions","occurredAt","evidenceRef");
      case "moveQuantity" -> Set.of("segmentId","destinationId","occurredAt","evidenceRef");
      case "recordStocktake" -> Set.of("segmentId","observedQuantity","unit","occurredAt","evidenceRef");
      case "adjustQuantity" -> Set.of("segmentId","quantity","unit","direction","reason","stocktakeId","occurredAt","evidenceRef");
      case "disposeQuantity" -> Set.of("segmentId","quantity","unit","reason","occurredAt","evidenceRef");
      default -> throw DomainError.unsupported();
    };
    if(!allowed.containsAll(slots.keySet()))throw DomainError.invalid("Unsupported stock command slot");
    if(!Objects.equals(intent.get("intentKind"),capability.equals("recordStocktake")?"RECORD":"COMMAND"))throw DomainError.invalid("Stock command intent kind mismatch");
    text(slots,"evidenceRef",240);
    List<String> ids=capability.equals("mergeQuantity")?strings(slots,"segmentIds"):List.of(uuid(slots,"segmentId"));
    if(ids.size()>100||new HashSet<>(ids).size()!=ids.size())throw DomainError.invalid("Distinct bounded segments required");
    var sources=ids.stream().sorted().map(id->stock.leaf(c,id,at)).toList();var first=sources.getFirst();
    // Multi-source scope IDs are NOT an authorization union: every consumed parent must be permitted.
    for(var source:sources)authorizer.authorizeScopes(c,capability,Map.of("TARGET",List.of((String)source.get("ID")),"ITEM",List.of((String)source.get("itemId")),"PLACE",List.of((String)source.get("placeId"))));
    var fences=new TreeSet<String>();sources.forEach(s->fences.addAll(stock.fences(s)));
    if(capability.equals("splitQuantity")) {
      var amounts=strings(slots,"quantities");if(amounts.size()<2||amounts.size()>100)throw DomainError.invalid("Split requires 2..100 children");
      var sum=amounts.stream().map(v->stock.quantity(c,first,v,text(slots,"unit",40))).reduce(BigDecimal.ZERO,BigDecimal::add);
      if(sum.compareTo(StockPrimitives.amount(first))!=0)throw DomainError.invalid("Split conservation mismatch");
    }
    if(capability.equals("mergeQuantity")) {
      if(sources.size()<2)throw DomainError.invalid("Two merge parents required");
      if(!(slots.get("expectedRevisions") instanceof Map<?,?> revisions)||revisions.size()!=sources.size())throw DomainError.invalid("Every merge parent revision required");
      for(var s:sources) {
        Object expected=revisions.get(s.get("ID"));if(!(expected instanceof Integer)||!expected.equals(s.get("revision")))throw new DomainError("REJECTED","REVISION_CONFLICT","Merge parent revision changed");
        for(String key:List.of("itemId","lotId","unit","placeId","controlScope","custodianId","ownerId","identificationStatus"))if(!Objects.equals(first.get(key),s.get(key)))throw DomainError.invalid("Incompatible merge "+key);
      }
    }
    if(capability.equals("moveQuantity")) {
      String destination=uuid(slots,"destinationId");var place=repository.current(c,"Places",destination);
      var origin=repository.current(c,"Places",(String)first.get("placeId"));
      if(!"INTERNAL_STORAGE".equals(place.get("kind"))||!"INTERNAL_STORAGE".equals(origin.get("kind"))||destination.equals(first.get("placeId"))||!repository.internalCustodian(c,(String)first.get("custodianId")))throw DomainError.invalid("Configured internal storage and confirmed custody required");
      authorizer.authorizeScopes(c,capability,Map.of("TARGET",List.of((String)first.get("ID")),"ITEM",List.of((String)first.get("itemId")),"PLACE",List.of(destination)));
      fences.add("inventory/place/"+destination);
    }
    if(capability.equals("recordStocktake"))validateObserved(c,first,slots.get("observedQuantity"),text(slots,"unit",40));
    if(capability.equals("adjustQuantity")||capability.equals("disposeQuantity")) {
      text(slots,"reason",240);var q=stock.quantity(c,first,slots.get("quantity"),text(slots,"unit",40));
      if(capability.equals("disposeQuantity")&&q.compareTo(StockPrimitives.amount(first))>0)throw DomainError.invalid("Decrease exceeds physical leaf");
      if(capability.equals("adjustQuantity")) {
        String direction=text(slots,"direction",40);if(!Set.of("INCREASE","DECREASE").contains(direction))throw DomainError.invalid("Adjustment direction required");
        var count=repository.current(c,"Stocktakes",uuid(slots,"stocktakeId"));
        if(repository.currentRows(c,"StockAdjustments").stream().anyMatch(a->count.get("ID").equals(a.get("stocktakeId"))))throw new DomainError("REJECTED","REVISION_CONFLICT","Stocktake difference already applied");
        if(at.isBefore(StockPrimitives.instant(count.get("occurredAt"))))throw DomainError.invalid("Adjustment precedes stocktake occurrence");
        BigDecimal difference=((BigDecimal)count.get("observedQuantity")).subtract(StockPrimitives.amount(first));
        if(!first.get("ID").equals(count.get("segmentId"))||!first.get("unit").equals(count.get("unit"))||difference.abs().compareTo(q)!=0||difference.signum()!=(direction.equals("INCREASE")?1:-1))throw DomainError.invalid("Adjustment must reconcile this stocktake difference");
        fences.add("inventory/stocktake/"+count.get("ID"));
      }
    }
    String effect=switch(capability){case "splitQuantity"->"SPLIT";case "mergeQuantity"->"MERGE";case "moveQuantity"->"INTERNAL_MOVE";case "recordStocktake"->"RECORD_STOCKTAKE";case "adjustQuantity"->"ADJUSTMENT";default->"DISPOSE";};
    Map<String,List<String>> scope=new LinkedHashMap<>();scope.put("TARGET",ids);scope.put("ITEM",sources.stream().map(s->(String)s.get("itemId")).distinct().toList());
    var places=new ArrayList<>(sources.stream().map(s->(String)s.get("placeId")).distinct().toList());if(capability.equals("moveQuantity"))places.add(uuid(slots,"destinationId"));scope.put("PLACE",places);
    var prepared=CommandPreparation.ordinary(scope,new ArrayList<>(fences),effect,(String)first.get("ID"),((Number)first.get("revision")).intValue());
    restrictions.verify(c,capability,"",prepared,intent);return prepared;
  }
  public Map<String,Object> execute(DomainContext c,Map<String,Object> intent) {
    prepare(c,intent);var s=slots(intent);String capability=text(intent,"capabilityId",100),command=CommandExecution.commandId(),evidence=text(s,"evidenceRef",240);Instant at=time(c,s);
    List<String> ids=switch(capability) {
      case "splitQuantity"->stock.split(c,uuid(s,"segmentId"),strings(s,"quantities"),text(s,"unit",40),at,evidence,command);
      case "mergeQuantity"->List.of(stock.merge(c,strings(s,"segmentIds"),at,evidence,command));
      case "moveQuantity"->List.of(stock.moveInternal(c,uuid(s,"segmentId"),uuid(s,"destinationId"),at,evidence,command));
      case "recordStocktake"->List.of(stock.stocktake(c,uuid(s,"segmentId"),s.get("observedQuantity"),text(s,"unit",40),at,evidence,command));
      case "adjustQuantity"->stock.adjust(c,uuid(s,"segmentId"),uuid(s,"stocktakeId"),s.get("quantity"),text(s,"unit",40),text(s,"direction",40),at,evidence,command,text(s,"reason",240));
      case "disposeQuantity"->stock.decrease(c,uuid(s,"segmentId"),s.get("quantity"),text(s,"unit",40),at,evidence,command,"DISPOSE");
      default->throw DomainError.unsupported();
    };
    var effects=new LinkedHashMap<String,Object>();effects.put(capability.equals("recordStocktake")?"stocktakeIds":"segmentIds",ids);effects.put("quantityEffects",capability.equals("recordStocktake")?List.of():ids.stream().map(id->repository.current(c,"QuantitySegments",id)).map(r->Map.of("segmentId",r.get("ID"),"quantity",InventoryQuantity.text(r.get("quantity")),"unit",r.get("unit"))).toList());
    return Map.of("outcome","APPLIED","revision",capability.equals("recordStocktake")?0:1,"effects",effects);
  }
  @SuppressWarnings("unchecked") public static Map<String,Object> slots(Map<String,Object> intent) {if(!(intent.get("slots") instanceof Map<?,?> m)||m.keySet().stream().anyMatch(k->!(k instanceof String)))throw DomainError.invalid("Typed slots required");return (Map<String,Object>)m;}
  public static String text(Map<String,Object> m,String k,int max){if(!(m.get(k) instanceof String s)||s.isBlank()||s.length()>max)throw DomainError.invalid("Invalid "+k);return s;}
  public static String uuid(Map<String,Object> m,String k){String s=text(m,k,36);try{return UUID.fromString(s).toString();}catch(IllegalArgumentException e){throw DomainError.invalid("UUID required for "+k);}}
  public static List<String> strings(Map<String,Object> m,String k){if(!(m.get(k) instanceof List<?> values)||values.isEmpty()||values.stream().anyMatch(v->!(v instanceof String)))throw DomainError.invalid("String list required for "+k);return values.stream().map(v->(String)v).toList();}
  public static Instant time(DomainContext c,Map<String,Object> slots){try{Instant at=Instant.parse(text(slots,"occurredAt",80));if(at.isAfter(c.knownAt()))throw DomainError.invalid("Future actual occurrence rejected");return at;}catch(java.time.format.DateTimeParseException e){throw DomainError.invalid("UTC occurredAt required");}}
  private void validateObserved(DomainContext c,Map<String,Object> first,Object value,String unit){if(value instanceof String text&&text.matches("0(?:\\.0{1,12})?"))stock.quantity(c,first,text.replaceFirst("^0","1"),unit);else stock.quantity(c,first,value,unit);}
}
