package com.mulino.application.inventory;

import com.mulino.application.core.*;
import com.mulino.domain.inventory.*;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.stereotype.Component;

/** Metadata identity never creates physical quantity or merges by name/code. */
@Component
public final class ItemCommands implements CommandHandler {
 private final InventoryRepository repository;
 public ItemCommands(InventoryRepository repository){this.repository=repository;}
 public Set<String> capabilities(){return Set.of("registerItem","linkExternalId");}
 public CommandPreparation prepare(DomainContext c,Map<String,Object> intent) {
  var p=InventoryCommands.slots(intent);String capability=InventoryCommands.text(intent,"capabilityId",100);
  if(!"COMMAND".equals(intent.get("intentKind")))throw DomainError.invalid("Metadata COMMAND required");
  if(capability.equals("registerItem")) {
   if(!Set.of("name","baseUnit","decimalPlaces","specificationHash","packagingHash","specificationDescription","packagingDescription").containsAll(p.keySet()))throw DomainError.invalid("Unsupported item slot");
   InventoryCommands.text(p,"name",240);InventoryCommands.text(p,"baseUnit",40);
   for(String key:List.of("specificationHash","packagingHash"))if(!InventoryCommands.text(p,key,64).matches("[0-9a-f]{64}"))throw DomainError.invalid("Version SHA-256 required");
   if(!(p.get("decimalPlaces") instanceof Integer precision)||precision<0||precision>12)throw DomainError.invalid("Integer base precision required");
   for(String key:List.of("specificationDescription","packagingDescription"))if(p.containsKey(key)&&(!(p.get(key) instanceof String description)||description.length()>100000))throw DomainError.invalid("Version description invalid");
   return CommandPreparation.ordinary(Map.of("ORGANIZATION",List.of(c.organizationId())),List.of("inventory/metadata"),"ITEM_METADATA",null,null);
  }
  if(!capability.equals("linkExternalId"))throw DomainError.unsupported();
  if(!Set.of("itemId","issuer","namespace","value","validFrom","validUntil").containsAll(p.keySet()))throw DomainError.invalid("Unsupported external identity slot");
  String item=InventoryCommands.uuid(p,"itemId");var row=repository.current(c,"TradeItems",item);
  String issuer=InventoryCommands.text(p,"issuer",240),namespace=InventoryCommands.text(p,"namespace",160),value=InventoryCommands.text(p,"value",240);
  interval(p);
  return CommandPreparation.ordinary(Map.of("ITEM",List.of(item),"TARGET",List.of(item)),List.of("inventory/item/"+item,"inventory/external/"+hash(List.of(issuer,namespace,value))),"ITEM_METADATA",item,((Number)row.get("revision")).intValue());
 }
 public Map<String,Object> execute(DomainContext c,Map<String,Object> intent) {
  prepare(c,intent);var p=InventoryCommands.slots(intent);
  if(intent.get("capabilityId").equals("registerItem")) {
   String product=StockPrimitives.id(),item=StockPrimitives.id(),spec=StockPrimitives.id(),pack=StockPrimitives.id();
   var productRow=StockPrimitives.row(c,product,c.knownAt());productRow.put("name",p.get("name"));repository.register("Products",productRow);
   var specRow=StockPrimitives.row(c,spec,c.knownAt());specRow.put("productId",product);specRow.put("version","1");specRow.put("contentHash",p.get("specificationHash"));specRow.put("description",p.getOrDefault("specificationDescription",""));repository.register("SpecificationVersions",specRow);
   var packRow=StockPrimitives.row(c,pack,c.knownAt());packRow.put("productId",product);packRow.put("version","1");packRow.put("contentHash",p.get("packagingHash"));packRow.put("description",p.getOrDefault("packagingDescription",""));repository.register("PackagingVersions",packRow);
   var itemRow=StockPrimitives.row(c,item,c.knownAt());for(String key:List.of("name","baseUnit","decimalPlaces"))itemRow.put(key,p.get(key));itemRow.put("productId",product);itemRow.put("specificationVersionId",spec);itemRow.put("packagingVersionId",pack);repository.register("TradeItems",itemRow);
   return Map.of("outcome","ACCEPTED","revision",0,"effects",Map.of("itemId",item,"productId",product,"specificationVersionId",spec,"packagingVersionId",pack),"quantityEffects",List.of());
  }
  String item=InventoryCommands.uuid(p,"itemId");Instant[] interval=interval(p);
  var overlaps=repository.currentRows(c,"ExternalIdentifiers").stream().filter(r->Objects.equals(r.get("issuer"),p.get("issuer"))&&Objects.equals(r.get("namespace"),p.get("namespace"))&&Objects.equals(r.get("value"),p.get("value"))).filter(r->(interval[1]==null||StockPrimitives.instant(r.get("validFrom")).isBefore(interval[1]))&&(r.get("validUntil")==null||interval[0].isBefore(StockPrimitives.instant(r.get("validUntil"))))).toList();
  for(var existing:overlaps) {
   if(item.equals(existing.get("itemId")))return Map.of("outcome","ACCEPTED","revision",0,"effects",Map.of("externalIdentifierId",existing.get("ID"),"alreadyLinked",true));
   String id=StockPrimitives.id();var conflict=StockPrimitives.row(c,id,c.knownAt());for(String key:List.of("issuer","namespace","value"))conflict.put(key,p.get(key));conflict.put("itemId",item);conflict.put("existingIdentifierId",existing.get("ID"));conflict.put("validFrom",interval[0]);conflict.put("validUntil",interval[1]);conflict.put("state","RECONCILIATION_REQUIRED");
   // Persist the conflict through a bounded metadata-only method.
   repository.recordIdentifierConflict(conflict);
   return Map.of("outcome","ACCEPTED_PENDING_RECONCILIATION","revision",0,"effects",Map.of("conflictId",id),"nextAction","RECONCILE_EXTERNAL_IDENTIFIER");
  }
  String id=StockPrimitives.id();var row=StockPrimitives.row(c,id,c.knownAt());for(String key:List.of("issuer","namespace","value"))row.put(key,p.get(key));row.put("itemId",item);row.put("validFrom",interval[0]);row.put("validUntil",interval[1]);repository.register("ExternalIdentifiers",row);
  return Map.of("outcome","ACCEPTED","revision",0,"effects",Map.of("externalIdentifierId",id));
 }
 private static Instant[] interval(Map<String,Object> p){try{Instant from=Instant.parse(InventoryCommands.text(p,"validFrom",80)),until=p.containsKey("validUntil")?Instant.parse(InventoryCommands.text(p,"validUntil",80)):null;if(until!=null&&!until.isAfter(from))throw DomainError.invalid("External identity interval invalid");return new Instant[]{from,until};}catch(java.time.format.DateTimeParseException e){throw DomainError.invalid("UTC identity interval required");}}
 private static String hash(List<String> values){try{var digest=MessageDigest.getInstance("SHA-256");for(String value:values){digest.update(Integer.toString(value.length()).getBytes(StandardCharsets.UTF_8));digest.update((byte)':');digest.update(value.getBytes(StandardCharsets.UTF_8));}return HexFormat.of().formatHex(digest.digest());}catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}
}
