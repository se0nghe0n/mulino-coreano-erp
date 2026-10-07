package com.mulino.application.inventory;

import com.mulino.application.core.*;
import com.mulino.domain.inventory.InventoryRepository;
import java.time.Instant;
import java.util.*;

/** Metadata-only registration; inventory receipt and eligibility are separate commands. */
public class ItemRegistration {
  private final InventoryRepository repository;
  private final ReadAuthorizer authorizer;
  public ItemRegistration(InventoryRepository repository,ReadAuthorizer authorizer) {this.repository=repository;this.authorizer=authorizer;}
  // Deliberately not a Spring handler: S2 must supply envelope, audit and idempotency.
  public Map<String,Object> execute(DomainContext c,Map<String,Object> intent) {
    authorizer.authorize(c,"inventory.registerItem.v1",null);
    Object raw=intent.get("parameters");
    if(!(raw instanceof Map<?,?>)) raw=intent.get("payload");
    if(!(raw instanceof Map<?,?>)) throw DomainError.invalid("Item parameters required");
    Map<String,Object> p=(Map<String,Object>)raw;
    if(!Set.of("name","baseUnit","decimalPlaces","specificationHash","packagingHash","specificationDescription","packagingDescription").containsAll(p.keySet())) throw DomainError.invalid("Unsupported item property");
    String name=text(p,"name",240),unit=text(p,"baseUnit",40),spec=text(p,"specificationHash",64),pack=text(p,"packagingHash",64);
    if(!spec.matches("[0-9a-f]{64}")||!pack.matches("[0-9a-f]{64}")) throw DomainError.invalid("Version content SHA-256 required");
    if(!(p.get("decimalPlaces") instanceof Number number)||number.doubleValue()!=number.intValue()||number.intValue()<0||number.intValue()>12) throw DomainError.invalid("Unit precision required");
    String product=UUID.randomUUID().toString(),item=UUID.randomUUID().toString(),specId=UUID.randomUUID().toString(),packId=UUID.randomUUID().toString();
    var pr=row(c,product);pr.put("name",name);repository.register("Products",pr);
    var sr=row(c,specId);sr.put("productId",product);sr.put("version","1");sr.put("contentHash",spec);sr.put("description",p.getOrDefault("specificationDescription",""));repository.register("SpecificationVersions",sr);
    var pk=row(c,packId);pk.put("productId",product);pk.put("version","1");pk.put("contentHash",pack);pk.put("description",p.getOrDefault("packagingDescription",""));repository.register("PackagingVersions",pk);
    var it=row(c,item);it.put("productId",product);it.put("name",name);it.put("baseUnit",unit);it.put("decimalPlaces",number.intValue());it.put("specificationVersionId",specId);it.put("packagingVersionId",packId);repository.register("TradeItems",it);
    return Map.of("outcome","ACCEPTED","revision",0,"effects",Map.of("productId",product,"itemId",item,"specificationVersionId",specId,"packagingVersionId",packId),"quantityEffects",List.of());
  }
  private Map<String,Object> row(DomainContext c,String id){var row=new LinkedHashMap<String,Object>();row.put("organizationId",c.organizationId());row.put("ID",id);row.put("revision",0);row.put("createdAt",Instant.now());row.put("recordedAt",Instant.now());return row;}
  private String text(Map<String,Object> p,String key,int max){if(!(p.get(key) instanceof String s)||s.isBlank()||s.length()>max)throw DomainError.invalid("Invalid "+key);return s;}
}
