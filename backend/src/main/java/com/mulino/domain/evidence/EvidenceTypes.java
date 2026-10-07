package com.mulino.domain.evidence;

import com.mulino.application.core.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;

public final class EvidenceTypes {
  private EvidenceTypes() {}
  public enum ValueState { KNOWN, MISSING, UNKNOWN, NOT_APPLICABLE, CONFLICT }
  public enum SubjectKind { ITEM, LOT, SEGMENT, WORK, PLACE, PURCHASE_ORDER, SALES_ORDER, SALES_ORDER_LINE, DISPATCH, CARGO_SCOPE, DELIVERY, DELIVERY_OBSERVATION, RETURN, RECALL, RECALL_SCOPE, SETTLEMENT, INVOICE, PURCHASE_ORDER_LINE }
  public enum SelectorKind { DOCUMENT, EVENT, CLAIM, CANONICAL }
  public record Subject(SubjectKind kind,String id) {
    public Subject { Objects.requireNonNull(kind); uuid(id); }
  }
  public record EffectiveTime(Instant from,Instant until,String timeZone,String precision) {
    public EffectiveTime {
      Objects.requireNonNull(from); ZoneId.of(timeZone);
      if(!Set.of("INSTANT","SECOND","MINUTE","DAY","RANGE").contains(precision) || (until!=null&&!until.isAfter(from)))
        throw DomainError.invalid("Invalid effective time range");
      if("DAY".equals(precision)&&until==null) throw DomainError.invalid("Date-only evidence requires a range");
    }
  }
  public static String uuid(String id) {
    try { return UUID.fromString(id).toString(); } catch(RuntimeException e) { throw DomainError.invalid("Invalid typed identifier"); }
  }
  public static String text(String value,int max) {
    if(value==null||value.isBlank()||value.length()>max) throw DomainError.invalid("Missing or overlong evidence field");
    return value;
  }
  public static BigDecimal quantity(String value,String unit,ValueState state) {
    if(state!=ValueState.KNOWN) {
      if(value!=null) throw DomainError.invalid("Unknown value cannot contain a quantity");
      return null;
    }
    if(value==null)return null;
    try {
      BigDecimal q=new BigDecimal(value);
      if(q.signum()<0||q.scale()>12||q.precision()-q.scale()>26||q.precision()>38||unit==null||unit.isBlank()) throw DomainError.invalid("Invalid quantity and unit");
      return q;
    }catch(NumberFormatException e) { throw DomainError.invalid("Invalid decimal quantity"); }
  }
  public static Instant instant(Object value) {
    if(value instanceof Instant i)return i;
    if(value instanceof java.sql.Timestamp t)return t.toInstant();
    if(value instanceof OffsetDateTime t)return t.toInstant();
    return Instant.parse(value.toString());
  }
  public static Map<String,Collection<String>> scopes(Map<String,Object> row) {
    Map<String,Collection<String>> result=new LinkedHashMap<>();
    result.put("TARGET",row.get("subjectId")==null?List.of(row.get("ID").toString()):List.of(row.get("ID").toString(),row.get("subjectId").toString()));
    for(String dimension:List.of("ITEM","PLACE","WORK")) {
      Object id=row.get(dimension.toLowerCase()+"Id");
      if(id!=null) result.put(dimension,List.of(id.toString()));
    }
    if(row.get("sourceProfileId")!=null)result.put("SOURCE",List.of(row.get("sourceProfileId").toString()));
    return result;
  }
}
