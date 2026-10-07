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
  public Set<String> operations() { return Set.of("getEvidence","getInbox"); }
  @Transactional(readOnly=true)
  public QueryResult query(DomainContext c,QueryRequest q) {
    if(!Set.of("kind","subjectKind","subjectId").containsAll(q.scope().keySet()) ||
       !Set.of("includeSuperseded","verifiedOnly").containsAll(q.filters().keySet()))throw DomainError.invalid("Unsupported evidence selector");
    String kind=Objects.toString(q.scope().getOrDefault("kind","DOCUMENT"));
    String entity="getInbox".equals(q.operation())?"InboxRecords":switch(kind) {
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
    for(Map<String,Object> row:rows) {
      String id=row.get("ID").toString();
      if(q.id()!=null&&!id.equals(uuid(q.id())))continue;
      if(after!=null&&id.compareTo(after)<=0)continue;
      if(!visible(row,c)||!matches(row,q.scope()))continue;
      if(!auth.permittedScopes(c,q.operation(),scopes(row)))continue;
      boolean superseded=rows.stream().anyMatch(next->id.equals(next.get("supersedesId"))&&visible(next,c));
      if(superseded&&!Boolean.TRUE.equals(q.filters().get("includeSuperseded"))&&q.id()==null)continue;
      var output=new LinkedHashMap<>(row);
      output.put("superseded",superseded);
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
        output.put("inventoryEffects","NOT_IMPLEMENTED");
        output.put("workReassessment","NOT_IMPLEMENTED");
        if(!isVerified)unknowns.add(id+":CANONICAL_UNVERIFIED");
      }
      Object state=row.get("valueState");
      if(state!=null&&Set.of("UNKNOWN","MISSING","NOT_APPLICABLE").contains(state.toString()))unknowns.add(id+":"+state);
      if("CONFLICT".equals(state)||"CONFLICT".equals(row.get("state")))conflicts.add(id+":EVIDENCE_CONFLICT");
      selected.add(output);
      if(selected.size()>q.limit())break;
    }
    if(q.id()!=null&&selected.isEmpty())throw DomainError.forbidden();
    String next=null;
    if(selected.size()>q.limit()) { selected.removeLast(); next=ReadCursor.encode(selected.getLast().get("ID").toString(),c,q); }
    return new QueryResult(q.id()==null?selected:selected.getFirst(),q.scope(),unknowns,conflicts,refs,next);
  }
  public boolean verifiedAt(DomainContext c,String canonicalId) {
    return r.rows("Verifications",c.organizationId()).stream().anyMatch(v->canonicalId.equals(v.get("canonicalOccurrenceId"))&&"VERIFIED".equals(v.get("verdict"))&&visible(v,c));
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
