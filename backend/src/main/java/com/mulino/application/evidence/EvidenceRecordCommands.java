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
  private static final Set<String> EVENT=Set.of("subject","kind","sourceNamespace","externalEventId","sourceVersion","effectiveFrom","effectiveUntil","timeZone","timePrecision","valueState","payload","supersedesId","invalidatesId","documentId","assertion","quantity","unit");
  private static final Set<String> DOCUMENT=Set.of("subject","sourceNamespace","sourceReference","mediaType","expectedHash","provenance","supersedesId","contentBase64");
  private final EvidenceRepository r;private final EvidenceRecords records;private final IdentityAuthorization auth;private final ObjectProvider<EvidenceCorrectionImpact> impacts;
  public EvidenceRecordCommands(EvidenceRepository r,EvidenceRecords records,IdentityAuthorization auth,ObjectProvider<EvidenceCorrectionImpact> impacts){this.r=r;this.records=records;this.auth=auth;this.impacts=impacts;}
  public Set<String> capabilities(){return Set.of("recordActivity","attachEvidence","correctEvidence");}
  public Set<String> intentKinds(){return Set.of("RECORD");}
  public CommandPreparation prepare(DomainContext c,Map<String,Object> intent){
    String capability=intent.get("capabilityId").toString();var s=slots(intent,"attachEvidence".equals(capability)?DOCUMENT:EVENT);var subject=subject(s);
    var profile=records.source(c,required(s,"sourceNamespace"));var target=r.subject(c.organizationId(),subject.kind().name(),subject.id());
    var dimensions=new LinkedHashMap<String,List<String>>();dimensions.put("TARGET",List.of(subject.id()));dimensions.put("SOURCE",List.of(profile.get("ID").toString()));
    if(subject.kind()==SubjectKind.ITEM)dimensions.put("ITEM",List.of(subject.id()));
    if(subject.kind()==SubjectKind.PLACE)dimensions.put("PLACE",List.of(subject.id()));
    if(subject.kind()==SubjectKind.WORK)dimensions.put("WORK",List.of(subject.id()));
    for(String d:List.of("ITEM","PLACE","WORK"))if(target.get(d.toLowerCase()+"Id")!=null)dimensions.put(d,List.of(target.get(d.toLowerCase()+"Id").toString()));
    Integer revision=null;String previous=string(s,"supersedesId");
    if("correctEvidence".equals(capability)&&previous==null)throw DomainError.invalid("Correction requires prior event revision");
    if("recordActivity".equals(capability)&&(previous!=null||s.get("invalidatesId")!=null))throw DomainError.invalid("Correction must use correctEvidence");
    if(previous!=null){var old=r.require("attachEvidence".equals(capability)?"DocumentVersions":"Events",c.organizationId(),uuid(previous));if(!subject.id().equals(old.get("subjectId"))||!subject.kind().name().equals(old.get("subjectKind")))throw DomainError.invalid("Correction subject mismatch");auth.authorizeScopes(c,"correctEvidence",scopes(old));revision=((Number)old.get("revision")).intValue();}
    if("attachEvidence".equals(capability))document(s);else event(s);
    return new CommandPreparation(dimensions,List.of("evidence-subject:"+subject.id(),"source:"+profile.get("ID")),Set.of(profile.get("intakeOwnerId").toString(),profile.get("supervisorId").toString()),"RECORD",null,null,0,previous==null?subject.id():previous,revision);
  }
  private static String content(Map<String,Object> slots) {
    if(!(slots.get("contentBase64") instanceof String value)||value.length()>24*1024*1024)throw DomainError.invalid("Original content size exceeds upload limit");
    return value;
  }
  public Map<String,Object> execute(DomainContext c,Map<String,Object> intent){
    String capability=intent.get("capabilityId").toString();var s=slots(intent,"attachEvidence".equals(capability)?DOCUMENT:EVENT);
    if("attachEvidence".equals(capability)){
      byte[] content;try{content=Base64.getDecoder().decode(content(s));}catch(IllegalArgumentException e){throw DomainError.invalid("Original content must be base64 bytes");}
      return records.attachDocument(document(s),content);
    }
    EvidenceCorrectionImpact impact=null;if("correctEvidence".equals(capability)){impact=impacts.getIfAvailable();if(impact==null)throw new DomainError("HELD","POLICY_UNRESOLVED","Correction impact service unavailable");}
    var output=new LinkedHashMap<>(records.recordActivity(event(s)));
    if(s.get("documentId")!=null){var claim=records.recordClaim(new EvidenceRecords.ClaimInput(output.get("id").toString(),required(s,"documentId"),required(s,"assertion"),string(s,"quantity"),string(s,"unit"),state(s),null));output.put("claimId",claim.get("id"));}
    if(impact!=null){var workIds=new LinkedHashSet<String>();var subject=subject(s);if(subject.kind()==SubjectKind.WORK)workIds.add(subject.id());var old=r.require("Events",c.organizationId(),required(s,"supersedesId"));if(old.get("workId")!=null)workIds.add(old.get("workId").toString());impact.apply(c,new EvidenceCorrectionImpact.Correction(required(s,"supersedesId"),output.get("id").toString(),workIds,Set.of()));output.put("workReassessment","PENDING");}
    return output;
  }
}
