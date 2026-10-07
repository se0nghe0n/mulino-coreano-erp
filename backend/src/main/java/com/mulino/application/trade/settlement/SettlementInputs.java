package com.mulino.application.trade.settlement;
import com.mulino.application.core.*;
import java.util.*;
import java.math.BigDecimal;
import static com.mulino.domain.trade.settlement.SettlementAmounts.*;
final class SettlementInputs {
 static Object value(Object v){return v instanceof Map<?,?> m&&m.containsKey("value")?m.get("value"):v;}
 static Map<String,Object> map(Object o){if(!(value(o) instanceof Map<?,?> m))throw DomainError.invalid("Object required");return (Map<String,Object>)m;}
 static String text(Map<String,Object> m,String k){return required(Map.of(k,Objects.toString(value(m.get(k)),"")),k);}
 static String id(Map<String,Object> m,String k){String s=text(m,k);CommandRequests.uuid(s);return s;}
 static String dim(Map<String,Object> m,String k,String d){Object o=m.get(k);return o instanceof Map<?,?> a&&a.get(d)!=null?a.get(d).toString():text(m,d);}
 static BigDecimal number(Map<String,Object> m,String k){return decimal(value(m.get(k)));}
 static Map<String,Object> row(DomainContext c,Object... kv){var m=new LinkedHashMap<String,Object>();m.put("organizationId",c.organizationId());m.put("ID",UUID.randomUUID().toString());for(int n=0;n<kv.length;n+=2)m.put(kv[n].toString(),kv[n+1]);return m;}
 static String encode(Object o){try{return new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules().configure(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS,true).writeValueAsString(o);}catch(Exception e){throw DomainError.invalid("Invalid data");}}
 static DomainError held(){return new DomainError("HELD","EVIDENCE_UNVERIFIED","Immutable original and exact settlement scope required");}
}
