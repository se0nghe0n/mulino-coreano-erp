package com.mulino.domain.inventory;

import com.mulino.application.core.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/** A place/item measurement and its separately named current-book comparison. No physical mutation. */
public final class PlaceStocktakePrimitives {
  public record Scope(String itemId,String placeId,String lotId,String unit,Instant snapshotAt,
                      List<Map<String,Object>> members,BigDecimal bookQuantity) {}
  private final InventoryRepository repository;
  private final StockPrimitives stock;
  private final ExecutionClock clock;
  public PlaceStocktakePrimitives(InventoryRepository repository,StockPrimitives stock,ExecutionClock clock) {
    this.repository=repository;this.stock=stock;this.clock=clock;
  }
  public Scope scope(DomainContext c,String itemId,String placeId,String lotId,String unit) {
    Instant stamp=clock==null?c.knownAt():clock.instant();
    var item=repository.current(c,"TradeItems",itemId);var place=repository.current(c,"Places",placeId);
    for(var subject:List.of(item,place))for(String time:List.of("createdAt","recordedAt"))
      if(subject.get(time)==null||StockPrimitives.instant(subject.get(time)).isAfter(stamp))throw DomainError.invalid("Stocktake subject is not yet known");
    if(!unit.equals(item.get("baseUnit")))throw DomainError.invalid("Base unit mismatch");
    if(lotId!=null&&!itemId.equals(repository.current(c,"ManufacturingLots",lotId).get("itemId")))throw DomainError.invalid("Stocktake LOT belongs to another item");
    var members=repository.currentRows(c,"QuantitySegments").stream()
      .filter(r->itemId.equals(r.get("itemId"))&&placeId.equals(r.get("placeId"))&&r.get("retiredAt")==null
        &&(lotId==null||lotId.equals(r.get("lotId"))))
      .sorted(Comparator.comparing(r->r.get("ID").toString())).toList();
    // The common guard permits at most 256 exact TARGET combinations, including the place.
    if(members.size()>255)throw DomainError.invalid("Stocktake scope exceeds complete bounded inventory");
    for(var member:members) {
      if(!unit.equals(member.get("unit")))throw DomainError.invalid("Stocktake member unit differs");
      for(String time:List.of("createdAt","recordedAt","validFrom"))
        if(member.get(time)==null||StockPrimitives.instant(member.get(time)).isAfter(stamp))
          throw DomainError.invalid("Stocktake current inventory contains a future or unknown member");
    }
    BigDecimal book=members.stream().map(StockPrimitives::amount).reduce(BigDecimal.ZERO,BigDecimal::add);
    nonNegative(item,book.toPlainString());
    return new Scope(itemId,placeId,lotId,unit,stamp,List.copyOf(members),book);
  }
  public BigDecimal observed(DomainContext c,Scope scope,Object value) {
    return nonNegative(repository.current(c,"TradeItems",scope.itemId()),value);
  }
  public List<String> fences(Scope scope) {
    var keys=new TreeSet<String>(Set.of("inventory/item/"+scope.itemId(),"inventory/place/"+scope.placeId()));
    scope.members().forEach(s->keys.addAll(stock.fences(s)));return List.copyOf(keys);
  }
  public String record(DomainContext c,String item,String place,String lot,Object value,String unit,
                       Instant occurredAt,String evidence,String command) {
    var before=scope(c,item,place,lot,unit);repository.fence(c,fences(before));
    var actual=scope(c,item,place,lot,unit);BigDecimal measurement=observed(c,actual,value);
    if(occurredAt.isAfter(actual.snapshotAt()))throw DomainError.invalid("Stocktake observation is in the future");
    String id=StockPrimitives.id();var row=StockPrimitives.row(c,id,occurredAt);
    // Observation occurrence and the later current-book comparison are different instants.
    row.put("createdAt",actual.snapshotAt());row.put("recordedAt",actual.snapshotAt());
    row.put("itemId",item);row.put("placeId",place);if(lot!=null)row.put("lotId",lot);
    row.put("observedQuantity",measurement);row.put("bookQuantity",actual.bookQuantity());row.put("unit",unit);
    row.put("occurredAt",occurredAt);row.put("snapshotAt",actual.snapshotAt());row.put("evidenceRef",evidence);row.put("commandId",command);
    row.put("observationStatus","RECORDED_UNASSESSED");repository.insert("PlaceStocktakes",row);
    for(var source:actual.members()) {
      var member=StockPrimitives.row(c,StockPrimitives.id(),occurredAt);member.put("createdAt",actual.snapshotAt());member.put("recordedAt",actual.snapshotAt());
      member.put("stocktakeId",id);member.put("segmentId",source.get("ID"));member.put("segmentRevision",source.get("revision"));
      member.put("bookQuantity",source.get("quantity"));member.put("unit",unit);repository.insert("PlaceStocktakeMembers",member);
    }
    return id;
  }
  private static BigDecimal nonNegative(Map<String,Object> item,Object value) {
    if(value instanceof String text&&text.matches("0(?:\\.0{1,12})?")) {
      InventoryQuantity.parse(text.replaceFirst("^0","1"),((Number)item.get("decimalPlaces")).intValue());return new BigDecimal(text);
    }
    return InventoryQuantity.parse(value,((Number)item.get("decimalPlaces")).intValue());
  }
}
