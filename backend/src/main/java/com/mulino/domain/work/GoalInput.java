package com.mulino.domain.work;

import com.mulino.application.core.*;
import java.time.*;
import java.util.*;

/** Typed immutable goal payload; absent draft inputs remain absent. */
public final class GoalInput {
 private GoalInput(){}
 public static final Set<String> MODES=Set.of("CUMULATIVE_EVENT","STATE_AT","EXISTS_IN","THROUGHOUT");
 public static void validate(Map<String,Object> goal,boolean complete){
  if(!Set.of("quantityMode","targetQuantity","unit","endpoint","scope","timezone","dueAt","evidencePolicyVersion","periodStart","periodEnd","periodStartInclusive","periodEndInclusive","eventKind","contributionScope","deduplication","evaluationAt","placeId","action","includeReserved","observationPolicy","conditions","evaluatorVersion","provenance","defaultContext").containsAll(goal.keySet()))throw DomainError.invalid("Unsupported goal slot");
  if(goal.containsKey("quantityMode")&&!MODES.contains(goal.get("quantityMode")))throw DomainError.invalid("Unsupported quantity mode");
  if(goal.containsKey("targetQuantity")){var d=new DecimalValue(text(goal,"targetQuantity"),text(goal,"unit"));if(new java.math.BigDecimal(d.value()).signum()<0)throw DomainError.invalid("Negative goal target");}
  for(String key:List.of("dueAt","periodStart","periodEnd","evaluationAt"))if(goal.containsKey(key))try{Instant.parse(text(goal,key));}catch(DateTimeException e){throw DomainError.invalid("Invalid goal instant");}
  if(goal.containsKey("timezone"))try{ZoneId.of(text(goal,"timezone"));}catch(DateTimeException e){throw DomainError.invalid("Invalid goal timezone");}
  for(String key:List.of("endpoint","evidencePolicyVersion","evaluatorVersion","eventKind","deduplication","action","observationPolicy"))if(goal.containsKey(key))text(goal,key);
  if(goal.containsKey("provenance")&&!(goal.get("provenance") instanceof Map))throw DomainError.invalid("Goal provenance must be object");
  if(goal.containsKey("scope")&&!(goal.get("scope") instanceof Map))throw DomainError.invalid("Goal scope must be object");
  for(String key:List.of("periodStartInclusive","periodEndInclusive"))if(goal.containsKey(key)&&!(goal.get(key) instanceof Boolean))throw DomainError.invalid("Goal interval inclusion must be boolean");
  if(goal.containsKey("includeReserved")&&!(goal.get("includeReserved") instanceof Boolean))throw DomainError.invalid("includeReserved must be boolean");
  if(goal.containsKey("periodStart")&&goal.containsKey("periodEnd")&&!Instant.parse(text(goal,"periodStart")).isBefore(Instant.parse(text(goal,"periodEnd"))))throw DomainError.invalid("Empty or reversed goal period");
  if(complete){var required=new ArrayList<>(List.of("quantityMode","endpoint","scope","timezone","dueAt","evidencePolicyVersion","conditions","evaluatorVersion"));if(!goal.containsKey("quantityMode"))throw new DomainError("NEEDS_INPUT","GOAL_SLOT_MISSING","Missing goal slot: quantityMode");String mode=text(goal,"quantityMode");required.addAll(switch(mode){case "CUMULATIVE_EVENT"->List.of("targetQuantity","unit","periodStart","periodEnd","periodStartInclusive","periodEndInclusive","eventKind","contributionScope","deduplication");case "STATE_AT"->List.of("targetQuantity","unit","evaluationAt","placeId","action","includeReserved");default->List.of("periodStart","periodEnd","observationPolicy");});for(String key:required)if(!goal.containsKey(key)||goal.get(key)==null)throw new DomainError("NEEDS_INPUT","GOAL_SLOT_MISSING","Missing goal slot: "+key);if(!(goal.get("conditions") instanceof List<?> list)||list.isEmpty())throw DomainError.invalid("Required goal conditions missing");}
 }
 public static String text(Map<String,Object> p,String key){if(!(p.get(key) instanceof String s)||s.isBlank())throw DomainError.invalid("Invalid "+key);return s;}
}
