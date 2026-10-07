package com.mulino.application.trade.regulatory;
import com.mulino.application.core.*;
import java.util.*;
import java.math.BigDecimal;
import java.time.Instant;
final class RegulatoryInputs {
 static String text(Map<String,Object> p,String k){Object x=p.get(k);if(!(x instanceof String s)||s.isBlank()||s.length()>320)throw DomainError.invalid("Required regulatory field: "+k);return s;}
 static String id(Map<String,Object> p,String k){try{return UUID.fromString(text(p,k)).toString();}catch(IllegalArgumentException e){throw DomainError.invalid("UUID required: "+k);}}
 static Instant time(Map<String,Object> p,String k){try{return Instant.parse(text(p,k));}catch(java.time.format.DateTimeParseException e){throw DomainError.invalid("UTC time required: "+k);}}
 static BigDecimal quantity(Map<String,Object> p,String k){try{BigDecimal n=new BigDecimal(text(p,k));if(n.signum()<0||n.scale()>12||n.precision()-n.scale()>26)throw DomainError.invalid("Invalid regulatory quantity");return n;}catch(NumberFormatException e){throw DomainError.invalid("Decimal quantity required");}}
 @SuppressWarnings("unchecked") static Map<String,Object> slots(Map<String,Object> i){if(!(i.get("slots") instanceof Map))throw DomainError.invalid("Slots required");return (Map<String,Object>)i.get("slots");}
 static Map<String,Object> row(DomainContext c){var r=new LinkedHashMap<String,Object>();r.put("ID",UUID.randomUUID().toString());r.put("organizationId",c.organizationId());r.put("revision",1);r.put("createdAt",c.knownAt());r.put("recordedAt",c.knownAt());r.put("recordedBy",c.actorId());return r;}
 static boolean valid(Map<String,Object> r,Instant at){return !com.mulino.domain.evidence.EvidenceTypes.instant(r.get("validFrom")).isAfter(at)&&(r.get("validUntil")==null||at.isBefore(com.mulino.domain.evidence.EvidenceTypes.instant(r.get("validUntil"))));}
}
