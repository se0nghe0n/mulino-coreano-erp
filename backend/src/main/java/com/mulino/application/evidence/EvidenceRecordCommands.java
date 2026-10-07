package com.mulino.application.evidence;
import com.mulino.application.core.*;
import com.mulino.application.identity.IdentityAuthorization;
import com.mulino.domain.evidence.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import static com.mulino.application.evidence.EvidenceCommandInputs.*;
import static com.mulino.domain.evidence.EvidenceTypes.*;
@Service
public class EvidenceRecordCommands implements CommandHandler {
  private static final Set<String> EVENT=Set.of("subject","kind","sourceNamespace","externalEventId","sourceVersion","effectiveFrom","effectiveUntil","timeZone","timePrecision","valueState","payload","supersedesId","invalidatesId","documentId","assertion","quantity","unit","evidenceType");
  private static final Set<String> DOCUMENT=Set.of("subject","sourceNamespace","sourceReference","mediaType","expectedHash","provenance","supersedesId","contentBase64","evidenceType");
  private static final Set<String> CLAIM=Set.of("subject","sourceNamespace","eventId","documentId","assertion","quantity","unit","valueState","supersedesId","evidenceType");
  private static String type(String capability,Map<String,Object> slots) {
    String type=Objects.toString(slots.get("evidenceType"),"attachEvidence".equals(capability)?"DOCUMENT":"EVENT");
    if(!Set.of("EVENT","DOCUMENT","CLAIM").contains(type))throw new DomainError("HELD","CAPABILITY_UNSUPPORTED","Evidence correction type unsupported");
    if(!"correctEvidence".equals(capability)&&!type.equals("attachEvidence".equals(capability)?"DOCUMENT":"EVENT"))throw DomainError.invalid("Evidence capability type mismatch");return type;
  }
  private static Set<String> allFields(){var fields=new HashSet<String>(EVENT);fields.addAll(DOCUMENT);fields.addAll(CLAIM);return fields;}
  private static Map<String,Object> input(Map<String,Object> intent) {
    String capability=intent.get("capabilityId").toString();var raw=slots(intent,allFields());
    return slots(intent,switch(type(capability,raw)){case "DOCUMENT"->DOCUMENT;case "CLAIM"->CLAIM;default->EVENT;});
  }
  private final EvidenceRepository r;private final EvidenceRecords records;private final IdentityAuthorization auth;private final ObjectProvider<EvidenceCorrectionImpact> impacts;
  public EvidenceRecordCommands(EvidenceRepository r,EvidenceRecords records,IdentityAuthorization auth,ObjectProvider<EvidenceCorrectionImpact> impacts){this.r=r;this.records=records;this.auth=auth;this.impacts=impacts;}
  public Set<String> capabilities(){return Set.of("recordActivity","attachEvidence","correctEvidence");}
  public Set<String> intentKinds(){return Set.of("RECORD");}
  public CommandPreparation prepare(DomainContext c,Map<String,Object> intent){
    String capability=intent.get("capabilityId").toString();var s=input(intent);String type=type(capability,s);var subject=subject(s);
    var profile=records.source(c,required(s,"sourceNamespace"));var target=r.subject(c.organizationId(),subject.kind().name(),subject.id());
    var dimensions=new LinkedHashMap<String,List<String>>();dimensions.put("TARGET",List.of(subject.id()));dimensions.put("SOURCE",List.of(profile.get("ID").toString()));
    if(subject.kind()==SubjectKind.ITEM)dimensions.put("ITEM",List.of(subject.id()));
    if(subject.kind()==SubjectKind.PLACE)dimensions.put("PLACE",List.of(subject.id()));
    if(subject.kind()==SubjectKind.WORK)dimensions.put("WORK",List.of(subject.id()));
    for(String d:List.of("ITEM","PLACE","WORK"))if(target.get(d.toLowerCase()+"Id")!=null)dimensions.put(d,List.of(target.get(d.toLowerCase()+"Id").toString()));
    Integer revision=null;String previous=string(s,"supersedesId");
    if("correctEvidence".equals(capability)&&previous==null)throw DomainError.invalid("Correction requires prior event revision");
    if("recordActivity".equals(capability)&&(previous!=null||s.get("invalidatesId")!=null))throw DomainError.invalid("Correction must use correctEvidence");
    if(previous!=null){var old=r.require(switch(type){case "DOCUMENT"->"DocumentVersions";case "CLAIM"->"Claims";default->"Events";},c.organizationId(),uuid(previous));if(!subject.id().equals(old.get("subjectId"))||!subject.kind().name().equals(old.get("subjectKind")))throw DomainError.invalid("Correction subject mismatch");auth.authorizeScopes(c,"correctEvidence",scopes(old));revision=((Number)old.get("revision")).intValue();}
    if("attachEvidence".equals(capability)&&previous!=null)throw DomainError.invalid("Document correction must use correctEvidence");
    if("DOCUMENT".equals(type))document(s);else if("EVENT".equals(type))event(s);else {
      var parent=r.require("Events",c.organizationId(),uuid(required(s,"eventId")));
      if(!subject.id().equals(parent.get("subjectId"))||!profile.get("ID").equals(parent.get("sourceProfileId")))throw DomainError.invalid("Claim correction source or subject mismatch");
      state(s);quantity(string(s,"quantity"),string(s,"unit"),state(s));required(s,"assertion");uuid(required(s,"documentId"));
    }
    return new CommandPreparation(dimensions,List.of("evidence-subject:"+subject.id(),"source:"+profile.get("ID")),Set.of(profile.get("intakeOwnerId").toString(),profile.get("supervisorId").toString()),"RECORD",null,null,0,previous==null?subject.id():previous,revision);
  }
  private static String content(Map<String,Object> slots) {
    if(!(slots.get("contentBase64") instanceof String value)||value.length()>24*1024*1024)throw DomainError.invalid("Original content size exceeds upload limit");
    return value;
  }
  public Map<String,Object> execute(DomainContext c,Map<String,Object> intent){
    String capability=intent.get("capabilityId").toString();var s=input(intent);String type=type(capability,s);
    EvidenceCorrectionImpact impact=null;if("correctEvidence".equals(capability)){impact=impacts.getIfAvailable();if(impact==null)throw new DomainError("HELD","POLICY_UNRESOLVED","Correction impact service unavailable");}
    Map<String,Object> recorded;
    if("DOCUMENT".equals(type)){
      byte[] content;try{content=Base64.getDecoder().decode(content(s));}catch(IllegalArgumentException e){throw DomainError.invalid("Original content must be base64 bytes");}
      recorded=records.attachDocument(document(s),content);
    }else if("CLAIM".equals(type))recorded=records.recordClaim(new EvidenceRecords.ClaimInput(required(s,"eventId"),required(s,"documentId"),required(s,"assertion"),string(s,"quantity"),string(s,"unit"),state(s),required(s,"supersedesId")));
    else recorded=records.recordActivity(event(s));
    var output=new LinkedHashMap<>(recorded);
    if("EVENT".equals(type)&&s.get("documentId")!=null){var claim=records.recordClaim(new EvidenceRecords.ClaimInput(output.get("id").toString(),required(s,"documentId"),required(s,"assertion"),string(s,"quantity"),string(s,"unit"),state(s),null));output.put("claimId",claim.get("id"));}
    if("EVENT".equals(type)&&"EVIDENCE_CONFLICT".equals(output.get("outcome"))&&impact==null) {
      var original=r.rows("InboxRecords",c.organizationId()).stream().filter(x->"RECEIVED".equals(x.get("state"))&&Objects.equals(s.get("sourceNamespace"),x.get("sourceNamespace"))&&Objects.equals(s.get("externalEventId"),x.get("externalEventId"))&&Objects.equals(s.get("sourceVersion"),x.get("sourceVersion"))).findFirst();
      var conflictImpact=impacts.getIfAvailable();
      if(original.isPresent()&&conflictImpact!=null)conflictImpact.apply(c,new EvidenceCorrectionImpact.Correction(original.get().get("eventId").toString(),output.get("id").toString(),Set.of(),Set.of()));
      else if(original.isPresent()&&r.db().run(com.sap.cds.ql.Select.from("mulino.work.read.Works").where(x->x.get("organizationId").eq(c.organizationId()))).rowCount()>0)throw new DomainError("HELD","POLICY_UNRESOLVED","Evidence conflict impact service unavailable");
    }
    if(impact!=null){var workIds=new LinkedHashSet<String>();var subject=subject(s);if(subject.kind()==SubjectKind.WORK)workIds.add(subject.id());var old=r.require(switch(type){case "DOCUMENT"->"DocumentVersions";case "CLAIM"->"Claims";default->"Events";},c.organizationId(),required(s,"supersedesId"));if(old.get("workId")!=null)workIds.add(old.get("workId").toString());impact.apply(c,new EvidenceCorrectionImpact.Correction(required(s,"supersedesId"),output.get("id").toString(),workIds,Set.of()));output.put("workReassessment","PENDING");}
    return EvidenceCommandOutcomes.applied(output);
  }
}
