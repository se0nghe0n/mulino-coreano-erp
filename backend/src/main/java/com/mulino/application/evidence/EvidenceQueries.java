package com.mulino.application.evidence;

import com.mulino.application.core.*;
import com.mulino.domain.evidence.*;
import com.mulino.adapters.blob.LocalBlobStore;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.mulino.domain.evidence.EvidenceTypes.*;

@Service
public class EvidenceQueries implements QueryHandler {
  private final EvidenceRepository r;
  private final ReadAuthorizer auth;
  private final LocalBlobStore blobs;
  public EvidenceQueries(EvidenceRepository r,ReadAuthorizer auth,LocalBlobStore blobs) { this.r=r; this.auth=auth; this.blobs=blobs; }
  public Set<String> operations() { return Set.of("getEvidence","getInbox","getReconciliation"); }
  @Transactional(readOnly=true)
  public QueryResult query(DomainContext c,QueryRequest q) {
    if(!Set.of("organizationId","kind","subjectKind","subjectId").containsAll(q.scope().keySet()) ||
       !Set.of("includeSuperseded","verifiedOnly").containsAll(q.filters().keySet()))throw DomainError.invalid("Unsupported evidence selector");
    if(q.scope().containsKey("subjectId")!=q.scope().containsKey("subjectKind"))throw DomainError.invalid("Typed subject requires kind and identifier");
    if(q.scope().get("subjectKind")!=null)try { SubjectKind.valueOf(q.scope().get("subjectKind").toString());uuid(q.scope().get("subjectId").toString()); }catch(IllegalArgumentException e){throw DomainError.invalid("Invalid typed subject");}
    for(Object filter:q.filters().values())if(!(filter instanceof Boolean))throw DomainError.invalid("Evidence flags must be boolean");
    if(q.scope().get("organizationId")!=null&&!c.organizationId().equals(q.scope().get("organizationId")))throw DomainError.forbidden();
    String kind=Objects.toString(q.scope().getOrDefault("kind","DOCUMENT"));
    String entity="getInbox".equals(q.operation())?"InboxRecords":"getReconciliation".equals(q.operation())?"Reconciliations":switch(kind) {
      case "DOCUMENT" -> "DocumentVersions"; case "EVENT" -> "Events";
      case "CLAIM" -> "Claims"; case "CANONICAL" -> "CanonicalOccurrences";
      default -> throw DomainError.invalid("Invalid evidence selector kind");
    };
    boolean verified=Boolean.TRUE.equals(q.filters().get("verifiedOnly"));
    if(verified&&!"CanonicalOccurrences".equals(entity))throw DomainError.invalid("Verified selector requires canonical occurrences");
    List<Map<String,Object>> rows=r.rows(entity,c.organizationId());
    String after=ReadCursor.decode(q.cursor(),c,q);
    List<Map<String,Object>> selected=new ArrayList<>();
    List<String> unknowns=new ArrayList<>(),conflicts=new ArrayList<>(),refs=new ArrayList<>();
    boolean hasMore=false;
    for(Map<String,Object> row:rows) {
      String id=row.get("ID").toString();
      if(q.id()!=null&&!id.equals(uuid(q.id())))continue;
      if(after!=null&&id.compareTo(after)<=0)continue;
      if(!visible(row,c)||!matches(row,q.scope()))continue;
      if(!auth.permittedScopes(c,q.operation(),scopes(row)))continue;
      boolean superseded=rows.stream().anyMatch(next->id.equals(next.get("supersedesId"))&&visible(next,c));
      boolean invalidated=rows.stream().anyMatch(next->id.equals(next.get("invalidatesId"))&&visible(next,c));
      if((superseded||invalidated)&&!Boolean.TRUE.equals(q.filters().get("includeSuperseded"))&&q.id()==null)continue;
      if(verified&&!verifiedAt(c,id))continue;
      if(selected.size()==q.limit()){hasMore=true;break;}
      var output=new LinkedHashMap<>(row);
      output.put("superseded",superseded);output.put("invalidated",invalidated);
      if("DocumentVersions".equals(entity)) {
        boolean available="AVAILABLE".equals(row.get("availability"))&&row.get("blobId")!=null&&blobs.available(UUID.fromString(row.get("blobId").toString()),row.get("sha256").toString());
        output.put("availability",available?"AVAILABLE":"UNKNOWN");
        output.remove("blobId");
        if(!available)unknowns.add(id+":CONTENT_UNAVAILABLE");
        refs.add(id);
      }
      if("CanonicalOccurrences".equals(entity)) {
        boolean isVerified=verifiedAt(c,id);
        if(verified&&!isVerified)continue;
        output.put("verified",isVerified);
        output.put("inventoryEffects","NONE");
        output.put("workReassessment",row.getOrDefault("reassessmentState","UNKNOWN"));
        if(!isVerified)unknowns.add(id+":CANONICAL_UNVERIFIED");
      }
      for(var link:r.rows("EvidenceLinks",c.organizationId())) {
        if(!visible(link,c)||!(id.equals(link.get("eventId"))||id.equals(link.get("claimId"))||id.equals(link.get("canonicalOccurrenceId"))))continue;
        var doc=r.require("DocumentVersions",c.organizationId(),link.get("documentVersionId").toString());
        if(visible(doc,c)&&auth.permittedScopes(c,"getEvidence",scopes(doc)))refs.add(doc.get("ID").toString());
      }
      Object state=row.get("valueState");
      if(state!=null&&Set.of("UNKNOWN","MISSING","NOT_APPLICABLE").contains(state.toString()))unknowns.add(id+":"+state);
      if("CONFLICT".equals(state)||"CONFLICT".equals(row.get("state"))||"CONFLICT".equals(row.get("decision")))conflicts.add(id+":EVIDENCE_CONFLICT");
      selected.add(output);
    }
    if(q.id()!=null&&selected.isEmpty())throw DomainError.forbidden();
    String next=null;
    if(hasMore)next=ReadCursor.encode(selected.getLast().get("ID").toString(),c,q);
    return new QueryResult(q.id()==null?selected:selected.getFirst(),q.scope(),unknowns,conflicts,refs.stream().distinct().toList(),next);
  }
  public boolean verifiedAt(DomainContext c,String canonicalId) {
    return r.rows("Verifications",c.organizationId()).stream().anyMatch(v->{
      if(!canonicalId.equals(v.get("canonicalOccurrenceId"))||!"VERIFIED".equals(v.get("verdict"))||!visible(v,c))return false;
      var claim=r.require("Claims",c.organizationId(),v.get("claimId").toString());
      var event=r.require("Events",c.organizationId(),claim.get("eventId").toString());
      var basis=r.require("DocumentVersions",c.organizationId(),v.get("basisDocumentId").toString());
      if(!auth.permittedScopes(c,"getEvidence",scopes(claim))||!auth.permittedScopes(c,"getEvidence",scopes(event))||!auth.permittedScopes(c,"getEvidence",scopes(basis))
          ||!"AVAILABLE".equals(basis.get("availability"))||basis.get("blobId")==null||!blobs.available(UUID.fromString(basis.get("blobId").toString()),basis.get("sha256").toString()))return false;
      if(r.rows("Events",c.organizationId()).stream().anyMatch(x->visible(x,c)&&(event.get("ID").equals(x.get("invalidatesId"))||event.get("ID").equals(x.get("supersedesId")))))return false;
      if(r.rows("DocumentVersions",c.organizationId()).stream().anyMatch(x->visible(x,c)&&basis.get("ID").equals(x.get("supersedesId"))))return false;
      if(r.rows("Claims",c.organizationId()).stream().anyMatch(x->visible(x,c)&&claim.get("ID").equals(x.get("supersedesId"))))return false;
      long variants=r.rows("InboxRecords",c.organizationId()).stream().filter(x->visible(x,c)&&Objects.equals(event.get("sourceNamespace"),x.get("sourceNamespace"))&&Objects.equals(event.get("externalEventId"),x.get("externalEventId"))&&Objects.equals(event.get("sourceVersion"),x.get("sourceVersion"))).count();
      return variants==1;
    });
  }
  @Transactional(readOnly=true)
  public Download download(String id) {
    DomainContext c=auth.context(Instant.now(),Instant.now());
    Map<String,Object> document=r.require("DocumentVersions",c.organizationId(),uuid(id));
    auth.authorizeScopes(c,"getEvidence",scopes(document));
    if(!"AVAILABLE".equals(document.get("availability"))||document.get("blobId")==null)
      throw new DomainError("REJECTED","EVIDENCE_UNAVAILABLE","Original content unavailable");
    return new Download(blobs.read(UUID.fromString(document.get("blobId").toString()),document.get("sha256").toString()),document.get("mediaType").toString());
  }
  public record Download(byte[] bytes,String mediaType) {}
  private static boolean matches(Map<String,Object> row,Map<String,Object> selector) {
    if(selector.get("subjectKind")!=null&&!Objects.equals(selector.get("subjectKind"),row.get("subjectKind")))return false;
    if(selector.get("subjectId")!=null&&!Objects.equals(uuid(selector.get("subjectId").toString()),row.get("subjectId")))return false;
    if(selector.containsKey("subjectId")!=selector.containsKey("subjectKind"))throw DomainError.invalid("Typed subject requires kind and identifier");
    if(selector.get("subjectKind")!=null)try { SubjectKind.valueOf(selector.get("subjectKind").toString()); } catch(IllegalArgumentException e) { throw DomainError.invalid("Invalid subject kind"); }
    return true;
  }
  static boolean visible(Map<String,Object> row,DomainContext c) {
    if(instant(row.get("recordedAt")).isAfter(c.knownAt()))return false;
    if(row.get("effectiveFrom")!=null&&instant(row.get("effectiveFrom")).isAfter(c.asOf()))return false;
    return row.get("effectiveUntil")==null||c.asOf().isBefore(instant(row.get("effectiveUntil")));
  }
}
