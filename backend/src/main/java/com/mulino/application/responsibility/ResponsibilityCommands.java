package com.mulino.application.responsibility;
import com.mulino.application.core.*;
import com.mulino.application.work.WorkAccess;
import com.mulino.domain.responsibility.ResponsibilityRepository;
import java.util.*;
import org.springframework.stereotype.Component;
@Component
public class ResponsibilityCommands implements CommandHandler {
 private final ResponsibilityService service;private final ResponsibilityRepository repository;private final WorkAccess works;
 public ResponsibilityCommands(ResponsibilityService s,ResponsibilityRepository r,WorkAccess w){service=s;repository=r;works=w;}
 public Set<String> capabilities(){return Set.of("openObligation","resolveObligation","waiveObligation","transferObligation","proposeHandover","acceptHandover","rejectHandover","expireHandover");}
 @SuppressWarnings("unchecked") private Map<String,Object> slots(Map<String,Object> i){if(!(i.get("slots") instanceof Map))throw DomainError.invalid("Typed slots required");return (Map<String,Object>)i.get("slots");}
 public CommandPreparation prepare(DomainContext c,Map<String,Object>i){var p=slots(i);String cap=i.get("capabilityId").toString();String workId;Map<String,Object> subject;
 if(Set.of("acceptHandover","rejectHandover","expireHandover").contains(cap)){subject=repository.require("Handovers",c.organizationId(),p.get("handoverId").toString());workId=subject.get("workId").toString();}
 else if(Set.of("resolveObligation","waiveObligation").contains(cap)){subject=repository.require("Assignments",c.organizationId(),p.get("assignmentId").toString());workId=subject.get("workId").toString();}
 else{workId=p.get("workId").toString();subject=works.require(c,workId,false);}
 var scopes=new LinkedHashMap<String,List<String>>();scopes.put("WORK",p.get("targetWorkId")==null?List.of(workId):List.of(workId,p.get("targetWorkId").toString()));var actors=new HashSet<String>();if(p.get("recipientId")!=null)actors.add(p.get("recipientId").toString());if(subject.get("recipientId")!=null)actors.add(subject.get("recipientId").toString());return new CommandPreparation(scopes,List.of("work:"+workId,"responsibility:"+workId),actors,"RESPONSIBILITY",null,null,0,subject.get("ID").toString(),((Number)subject.get("revision")).intValue());}
 public Map<String,Object> execute(DomainContext c,Map<String,Object>i){var p=slots(i);var result=switch(i.get("capabilityId").toString()){
 case "openObligation"->service.openDuty(c,p);case "resolveObligation"->service.settle(c,p,false);case "waiveObligation"->service.settle(c,p,true);case "transferObligation"->service.propose(c,p,true);case "proposeHandover"->service.propose(c,p,false);case "acceptHandover"->service.decide(c,p.get("handoverId").toString(),"ACCEPTED");case "rejectHandover"->service.decide(c,p.get("handoverId").toString(),"REJECTED");case "expireHandover"->service.decide(c,p.get("handoverId").toString(),"EXPIRED");default->throw DomainError.unsupported();};return Map.of("outcome","ACCEPTED","effects",Map.of("responsibility",result));}
}
