package com.mulino.application.trade.recall;
import com.mulino.application.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
final class RecallInputs {
 static Map<String,Object> slots(Map<String,Object>i){return (Map<String,Object>)i.get("slots");}
 static String text(Map<String,Object>m,String k){if(!(m.get(k) instanceof String s)||s.isBlank())throw DomainError.invalid("Missing "+k);return s;}
 static String id(Map<String,Object>m,String k){String s=text(m,k);CommandRequests.uuid(s);return s;}
 static int n(Object o){return ((Number)o).intValue();}
 static BigDecimal decimal(Object o,boolean positive){if(!(o instanceof String||o instanceof BigDecimal))throw DomainError.invalid("Decimal string required");try{var q=new BigDecimal(o.toString());if(q.scale()>12||q.precision()-q.scale()>26||(positive?q.signum()<=0:q.signum()<0))throw DomainError.invalid("Invalid quantity");return q;}catch(NumberFormatException e){throw DomainError.invalid("Invalid decimal");}}
 static Instant at(Object o){try{return o instanceof Instant t?t:Instant.parse(o.toString());}catch(Exception e){throw DomainError.invalid("Instant required");}}
 static Map<String,Object> row(DomainContext c){var r=new LinkedHashMap<String,Object>();r.putAll(Map.of("organizationId",c.organizationId(),"ID",UUID.randomUUID().toString(),"revision",0,"createdAt",c.knownAt(),"recordedAt",c.knownAt()));return r;}
 static String json(Object o){try{return new ObjectMapper().findAndRegisterModules().configure(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS,true).writeValueAsString(o);}catch(Exception e){throw DomainError.invalid("Invalid JSON");}}
 static DomainError held(){return new DomainError("HELD","EVIDENCE_UNVERIFIED","Current exact recall source and immutable physical scope required");}
}
