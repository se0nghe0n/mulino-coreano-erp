package com.mulino.domain.identity;

import com.mulino.application.core.DomainError;
import java.util.*;

/** Typed scope vocabulary. Recognition never supplies an assignment or a grant. */
public enum ScopeDimension {
  ORGANIZATION("mulino.identity.Organizations", "Organization"),
  TARGET(null, null),
  WORK("mulino.work.read.Works", "Work"),
  ITEM("mulino.inventory.TradeItems", "TradeItem"),
  PLACE("mulino.inventory.Places", "Place"),
  SOURCE(null, null),
  LOT("mulino.inventory.ManufacturingLots", "ManufacturingLot"),
  CUSTOMER("mulino.trade.sales.Customers", "Customer"),
  SUPPLIER("mulino.trade.purchase.Suppliers", "Supplier");

  private final String entity;
  private final String nounType;
  ScopeDimension(String entity, String nounType) { this.entity=entity; this.nounType=nounType; }
  public String entity() { return entity; }
  public String nounType() { return nounType; }

  public static ScopeDimension require(String kind, String id) {
    ScopeDimension dimension;
    try { dimension=valueOf(kind); } catch (RuntimeException invalid) { throw DomainError.invalid("Unsupported scope dimension"); }
    if(!dimension.validId(id)) throw DomainError.invalid("Invalid scope identifier");
    return dimension;
  }
  private boolean validId(String id) {
    if(id==null||id.isBlank()) return false;
    if(this==SOURCE) return id.codePointCount(0,id.length())<=36;
    try { return id.length()==36&&UUID.fromString(id).toString().equalsIgnoreCase(id); }
    catch(RuntimeException invalid) { return false; }
  }
  public static boolean valid(String kind, String id, String organizationId) {
    try { return require(kind,id)!=ORGANIZATION||Objects.equals(id,organizationId); }
    catch(DomainError invalid) { return false; }
  }
  public static boolean validTargets(Map<String,? extends Collection<String>> targets, String organizationId) {
    if(targets==null) return true;
    if(targets.isEmpty()) return false;
    return targets.entrySet().stream().allMatch(e->e.getValue()!=null&&!e.getValue().isEmpty()
        &&e.getValue().stream().allMatch(id->valid(e.getKey(),id,organizationId)));
  }
}
