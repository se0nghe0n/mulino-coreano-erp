package com.mulino.application.work;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mulino.application.core.*;
import com.mulino.domain.governance.PolicyRepository;
import com.mulino.domain.responsibility.ResponsibilityRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;

/** A raw temperature observation can warrant review without becoming a verified physical fact. */
@Component
public class WorkFollowupSource {
 private final ResponsibilityRepository repository;private final PolicyRepository policies;private final ExecutionClock clock;
 public WorkFollowupSource(ResponsibilityRepository repository,PolicyRepository policies,ExecutionClock clock){this.repository=repository;this.policies=policies;this.clock=clock;}
 public Map<String,Object> require(DomainContext c,String eventId,String itemId){
  var event=repository.requireSource(c.organizationId(),"EVENT",eventId);
  if(!"TEMPERATURE_OBSERVATION".equals(event.get("kind"))||!"SEGMENT".equals(event.get("subjectKind"))||!itemId.equals(event.get("itemId"))||!"KNOWN".equals(event.get("valueState"))||Instant.parse(event.get("recordedAt").toString()).isAfter(c.knownAt())||Instant.parse(event.get("effectiveFrom").toString()).isAfter(c.asOf()))throw DomainError.invalid("Current scoped temperature source required");
  if(repository.evidenceRows("EVENT",c.organizationId()).stream().anyMatch(x->eventId.equals(x.get("supersedesId"))||eventId.equals(x.get("invalidatesId"))))throw new DomainError("HELD","EVIDENCE_CONFLICT","Superseded observation requires reconciliation");
  var active=policies.current(c.organizationId(),"EVIDENCE",clock.instant());
  if(active.size()!=1)throw new DomainError("HELD","POLICY_UNRESOLVED","Current response policy required");
  var policy=active.getFirst();var content=decode(policy.get("content"));
  if(!com.mulino.domain.definitions.DefinitionRepository.sha256(policy.get("content").toString()).equals(policy.get("contentHash"))||!"outside_2_8_C".equals(content.get("temperatureResponse")))throw new DomainError("HELD","POLICY_UNRESOLVED","Supported explicit temperature response policy required");
  var payload=decode(event.get("payload"));
  if(!Objects.equals(event.get("subjectId"),payload.get("segmentId"))||!(payload.get("temperature") instanceof Map<?,?> temperature)||!"CELSIUS".equals(temperature.get("unit")))throw DomainError.invalid("Scoped Celsius observation required");
  BigDecimal value;try{value=new BigDecimal(temperature.get("value").toString());}catch(RuntimeException invalid){throw DomainError.invalid("Temperature decimal required");}
  if(value.compareTo(new BigDecimal("2"))>=0&&value.compareTo(new BigDecimal("8"))<=0)throw DomainError.invalid("Normal observation has no response duty");
  return event;
 }
 private Map<String,Object> decode(Object value){try{return new ObjectMapper().readValue(value.toString(),Map.class);}catch(Exception invalid){throw DomainError.invalid("Typed response source required");}}
}
