package com.mulino.application.definitions;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mulino.application.core.*;
import com.mulino.domain.definitions.*;
import com.mulino.domain.identity.IdentityRepository;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.springframework.stereotype.Component;

/**
 * Plan §8 DefinitionPackage lifecycle DRAFT→IN_REVIEW→PUBLISHED→ACTIVE→RETIRED_FOR_NEW_WORK. User decision 2026-10-11 (2):
 * the package wire shape is the stored {@link Definition} record (organizationId optional, id/state/contentHash server
 * assigned); unknown fields (SQL, scripts, privileges) are TYPE_INVALID and a predicate operator outside the validator's
 * fixed set is rejected at draft. The package hash is the SHA-256 of the stored content and never changes after the draft
 * revision is fixed, so the reviewed, approved, published and activated hash are the same. Publication needs an APPROVED,
 * unconsumed, current approval of that hash by a human other than the drafter and the submitter; ACTIVE is only the
 * organization's pointer. Regression fixtures are recorded as declared and reported NOT_RUN (deferred, see s5e README).
 */
@Component
public class DefinitionLifecycle implements CommandHandler,DefinitionMigrationApprovals {
  static final Set<String> CAPABILITIES=Set.of("createDefinitionDraft","validateDefinition","submitDefinitionReview","approveDefinition","publishDefinition","activateDefinition","retireDefinition");
  private final DefinitionLifecycleRepository r;private final DefinitionRepository definitions;private final DefinitionValidator validator;private final IdentityRepository identities;private final ExecutionClock clock;
  private final ObjectMapper json=new ObjectMapper();
  private final ObjectMapper strict=new ObjectMapper().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
  public DefinitionLifecycle(DefinitionLifecycleRepository r,DefinitionRepository definitions,DefinitionValidator validator,IdentityRepository identities,ExecutionClock clock){this.r=r;this.definitions=definitions;this.validator=validator;this.identities=identities;this.clock=clock;}
  public Set<String> capabilities(){return CAPABILITIES;}

  private static Map<String,Object> slots(Map<String,Object> i){if(!(i.get("slots") instanceof Map<?,?> m))throw DomainError.invalid("Object slots required");var out=new LinkedHashMap<String,Object>();m.forEach((k,v)->out.put(String.valueOf(k),v instanceof Map<?,?> ref&&ref.containsKey("type")&&ref.containsKey("id")&&ref.size()==2?ref.get("id"):v));return out;}
  private static String text(Map<String,Object> s,String k,int max){if(!(s.get(k) instanceof String v)||v.isBlank()||v.length()>max)throw DomainError.invalid("Invalid "+k);return v;}
  private static String uuid(Map<String,Object> s,String k){String v=text(s,k,36);try{UUID.fromString(v);}catch(IllegalArgumentException e){throw DomainError.invalid("UUID required for "+k);}return v;}
  private static Instant instant(Map<String,Object> s,String k,Instant fallback){if(s.get(k)==null)return fallback;try{return Instant.parse(text(s,k,80));}catch(DateTimeParseException e){throw DomainError.invalid("Invalid "+k);}}
  private static String op(Map<String,Object> i){return String.valueOf(i.get("capabilityId"));}
  private String encode(Object o){try{return json.writeValueAsString(o);}catch(Exception e){throw new IllegalStateException(e);}}
  private Map<String,Object> stored(DomainContext c,String id){return r.version(c.organizationId(),id).orElseThrow(DomainError::forbidden);}
  private String hashOf(Map<String,Object> s,Map<String,Object> row){String hash=text(s,"hash",64);if(!hash.equals(row.get("contentHash")))throw new DomainError("CONFLICT","DEFINITION_HASH_CHANGED","Definition package hash differs from the current revision");return hash;}
  private static void state(Map<String,Object> row,String... allowed){if(!List.of(allowed).contains(row.get("state")))throw DomainError.invalid("Invalid definition lifecycle transition");}
  /** Lifecycle state as the plan names it: retirement, then the ACTIVE pointer, then the stored state. */
  public String lifecycleState(String org,String id,String stored){if(r.retired(org,id))return "RETIRED_FOR_NEW_WORK";if(r.pointer(org).map(p->id.equals(p.get("definitionId"))).orElse(false))return "ACTIVE";return stored;}

  public List<SubjectBinding> subjectBindings(DomainContext c,Map<String,Object> i,CommandPreparation p){var s=slots(i);return s.get("definitionId") instanceof String id?List.of(SubjectBinding.optional("DefinitionPackage",Set.of(uuid(s,"definitionId")))):List.of();}
  public CommandPreparation prepare(DomainContext c,Map<String,Object> i){
    var s=slots(i);String op=op(i);String target=s.get("definitionId")!=null?uuid(s,"definitionId"):null;
    if(op.equals("createDefinitionDraft")){if(target!=null){var row=stored(c,target);state(row,"DRAFT","IN_REVIEW");}else if(s.get("parentDefinitionId")!=null)published(c,uuid(s,"parentDefinitionId"));}
    else {if(target==null)throw DomainError.invalid("definitionId required");stored(c,target);}
    var scopes=target!=null?Map.of("TARGET",List.of(target)):s.get("parentDefinitionId")!=null?Map.of("TARGET",List.of(uuid(s,"parentDefinitionId"))):Map.<String,List<String>>of();
    Integer revision=target==null?null:r.revision(c.organizationId(),target);
    return CommandPreparation.ordinary(scopes,List.of("definition-lifecycle:"+c.organizationId()),"DEFINITION",target,revision);
  }
  public Map<String,Object> execute(DomainContext c,Map<String,Object> i){
    var s=slots(i);String org=c.organizationId();Instant now=clock.instant();
    return switch(op(i)){
      case "createDefinitionDraft"->draft(c,s,now);
      case "validateDefinition"->{String id=uuid(s,"definitionId");var row=stored(c,id);state(row,"DRAFT","IN_REVIEW");String hash=hashOf(s,row);var d=content(row);var result=validator.validate(d);var problems=result.problems().stream().map(p->Map.<String,Object>of("path",String.valueOf(p.path()),"code",p.code())).toList();
        boolean publishable="VALID".equals(result.outcome());var refs=lifecycleRefs(org,id);var regression=new LinkedHashMap<String,Object>();regression.put("fixtureRefs",refs);regression.put("status","NOT_RUN");regression.put("reason","regression fixtures are recorded, not executed by the product (deferred)");
        var diff=diffClasses(c,d);String vid=UUID.randomUUID().toString();var v=new LinkedHashMap<String,Object>();v.put("organizationId",org);v.put("ID",vid);v.put("definitionId",id);v.put("hash",hash);v.put("outcome",result.outcome());v.put("publishable",publishable);v.put("problemsJson",encode(problems));v.put("diffClassesJson",encode(diff));v.put("regressionJson",encode(regression));v.put("actorId",c.actorId());v.put("recordedAt",now);r.insertValidation(v);
        int rev=r.bump(org,id,c.actorId(),now);var data=new LinkedHashMap<String,Object>();data.put("validationId",vid);data.put("publishable",publishable);data.put("outcome",result.outcome());data.put("problems",problems);data.put("diffClasses",diff);data.put("regression",regression);
        var out=response(org,id,hash,row,rev,Map.of("validationId",vid));out.put("validationId",vid);out.put("publishable",publishable);out.put("regressionRunIds",List.of());out.put("data",data);yield out;}
      case "submitDefinitionReview"->submit(c,s,now);
      case "approveDefinition"->approve(c,s,now);
      case "publishDefinition"->{String id=uuid(s,"definitionId");var row=stored(c,id);state(row,"IN_REVIEW");String hash=hashOf(s,row);
        Map<String,Object> approval;if(s.get("approvalId")!=null){approval=r.approval(org,uuid(s,"approvalId")).orElseThrow(DomainError::forbidden);}else approval=r.approvals(org,id,hash,"DEFINITION").stream().filter(a->"APPROVED".equals(a.get("decision"))&&a.get("consumedAt")==null).reduce((a,b)->b).orElse(null);
        if(approval==null)throw new DomainError("WAITING_APPROVAL","DEFINITION_APPROVAL_REQUIRED","Publication needs a current approval of this package hash");
        if(!id.equals(approval.get("definitionId"))||!hash.equals(approval.get("hash"))||!"DEFINITION".equals(approval.get("reviewKind"))||!"APPROVED".equals(approval.get("decision")))throw DomainError.forbidden();
        if(approval.get("consumedAt")!=null||now.isBefore(at(approval.get("validFrom")))||!now.isBefore(at(approval.get("validUntil"))))throw new DomainError("WAITING_APPROVAL","DEFINITION_APPROVAL_REQUIRED","The approval is consumed or outside its validity");
        var check=validator.validate(content(row));if(!"VALID".equals(check.outcome()))throw new DomainError("REJECTED","DEFINITION_NOT_PUBLISHABLE","Package fails publication checks: "+check.problems().stream().map(DefinitionValidator.Problem::code).distinct().toList());
        r.consume(org,String.valueOf(approval.get("ID")),c.actorId(),now);r.state(org,id,"IN_REVIEW","PUBLISHED");row.put("state","PUBLISHED");int rev=r.bump(org,id,c.actorId(),now);var out=response(org,id,hash,row,rev,Map.of("approvalId",approval.get("ID")));out.put("approvalId",approval.get("ID"));yield out;}
      case "activateDefinition"->{String id=uuid(s,"definitionId");var row=stored(c,id);state(row,"PUBLISHED");String hash=hashOf(s,row);if(r.retired(org,id))throw new DomainError("HELD","DEFINITION_RETIRED","A definition retired for new work cannot become active");
        published(c,id);Instant effective=instant(s,"effectiveAt",now);r.point(org,id,String.valueOf(row.get("version")),hash,effective,c.actorId(),now);int rev=r.bump(org,id,c.actorId(),now);var out=response(org,id,hash,row,rev,Map.of("effectiveAt",effective.toString()));out.put("effectiveAt",effective.toString());yield out;}
      case "retireDefinition"->{String id=uuid(s,"definitionId");var row=stored(c,id);state(row,"PUBLISHED");String reason=text(s,"reason",500);if(r.retired(org,id))throw DomainError.invalid("Definition already retired for new work");
        r.retire(org,id,reason,c.actorId(),now);r.unpoint(org,id);int rev=r.bump(org,id,c.actorId(),now);yield response(org,id,String.valueOf(row.get("contentHash")),row,rev,Map.of("reason",reason));}
      default->throw DomainError.unsupported();
    };
  }
  @SuppressWarnings("unchecked")
  public Map<String,Object> requireMigrationApproval(DomainContext c,String approvalId,String target,String workId,String migrationId){
    String org=c.organizationId();var a=r.approval(org,approvalId).orElseThrow(DomainError::forbidden);Instant now=clock.instant();
    if(!"WORK_MIGRATION".equals(a.get("reviewKind"))||!"APPROVED".equals(a.get("decision"))||!target.equals(a.get("definitionId")))throw DomainError.forbidden();
    if(a.get("consumedAt")!=null||now.isBefore(at(a.get("validFrom")))||!now.isBefore(at(a.get("validUntil"))))throw new DomainError("WAITING_APPROVAL","DEFINITION_APPROVAL_REQUIRED","The migration approval is consumed or outside its validity");
    var review=r.review(org,String.valueOf(a.get("reviewId"))).orElseThrow(DomainError::forbidden);Map<String,Object> payload;try{payload=json.readValue(String.valueOf(review.get("payloadJson")),Map.class);}catch(Exception e){throw new IllegalStateException(e);}
    if(!migrationId.equals(review.get("migrationId"))||!(payload.get("affectedWorkIds") instanceof List<?> works)||!works.contains(workId))throw DomainError.forbidden();
    Map<String,Object> changes;try{changes=a.get("goalChangesJson")==null?Map.of():json.readValue(String.valueOf(a.get("goalChangesJson")),Map.class);}catch(Exception e){throw new IllegalStateException(e);}
    var out=new LinkedHashMap<String,Object>();out.put("approvalId",approvalId);out.put("goalChanges",changes);out.put("mapping",payload.getOrDefault("mapping",Map.of()));return out;
  }
  public void recordMigration(DomainContext c,String approvalId,String workId,String migrationId,String previousDefinitionId,String definitionId,String previousGoal,String goal,String reason){
    String org=c.organizationId();Instant now=clock.instant();r.consume(org,approvalId,c.actorId(),now);var m=new LinkedHashMap<String,Object>();m.put("organizationId",org);m.put("ID",UUID.randomUUID().toString());m.put("workId",workId);m.put("approvalId",approvalId);m.put("migrationId",migrationId);m.put("previousDefinitionId",previousDefinitionId);m.put("definitionId",definitionId);m.put("previousGoalVersionId",previousGoal);m.put("goalVersionId",goal);m.put("actorId",c.actorId());m.put("reason",reason);m.put("recordedAt",now);r.insertMigration(m);
  }
  private Definition published(DomainContext c,String id){try{return definitions.get(c.organizationId(),id);}catch(NoSuchElementException unavailable){throw DomainError.forbidden();}catch(IllegalStateException unreadable){throw DomainError.unsupported();}}
  private static Instant at(Object o){return o instanceof java.sql.Timestamp t?t.toInstant():o instanceof Instant i?i:Instant.parse(String.valueOf(o));}
  private List<String> lifecycleRefs(String org,String id){try{return json.readValue(String.valueOf(r.lifecycle(org,id).map(x->x.get("regressionFixtureRefs")).orElse("[]")),List.class);}catch(Exception e){throw new IllegalStateException(e);}}
  private Definition content(Map<String,Object> row){try{return json.readValue(String.valueOf(row.get("content")),Definition.class);}catch(Exception e){throw new IllegalStateException("Invalid stored definition",e);}}
  private Map<String,Object> response(String org,String id,String hash,Map<String,Object> row,int revision,Map<String,Object> extra){
    String state=lifecycleState(org,id,String.valueOf(row.get("state")));var effects=new LinkedHashMap<String,Object>(Map.of("definitionId",id,"hash",hash,"state",state));effects.putAll(extra);
    var out=new LinkedHashMap<String,Object>();out.put("outcome","APPLIED");out.put("revision",revision);out.put("definitionId",id);out.put("hash",hash);out.put("state",state);out.put("version",row.get("version"));out.put("effects",effects);return out;
  }

  /** createDefinitionDraft: a new package revision, or (definitionId + parentDraftRevision) a revised draft after review. */
  private Map<String,Object> draft(DomainContext c,Map<String,Object> s,Instant now){
    String org=c.organizationId();boolean revise=s.get("definitionId")!=null;String id=revise?uuid(s,"definitionId"):UUID.randomUUID().toString();
    Map<String,Object> existing=revise?stored(c,id):null;
    if(revise&&(!(s.get("parentDraftRevision") instanceof Number n)||n.intValue()!=r.revision(org,id)))throw new DomainError("CONFLICT","STALE_REVISION","Draft revision changed");
    String parent=revise?(String)existing.get("parentVersionId"):s.get("parentDefinitionId")==null?null:uuid(s,"parentDefinitionId");
    var d=parsePackage(c,s,id,parent,revise?String.valueOf(existing.get("version")):null);
    String refs=encode(regressionRefs(s));String content=encode(d);String hash=DefinitionRepository.sha256(content);
    int rev;if(revise){r.reviseDraft(org,id,hash,content,d.evaluatorVersion(),d.schemaVersion());r.regressionRefs(org,id,refs);rev=r.bump(org,id,c.actorId(),now);}
    else {if(r.versionExists(org,d.version()))throw DomainError.invalid("Definition version already exists");r.insertVersion(org,id,d.version(),parent,hash,content,d.evaluatorVersion(),d.schemaVersion(),now);r.createLifecycle(org,id,c.actorId(),refs,now);rev=1;}
    var row=new LinkedHashMap<String,Object>(Map.of("state","DRAFT","version",d.version()));return response(org,id,hash,row,rev,Map.of("version",d.version()));
  }
  private static List<String> regressionRefs(Map<String,Object> s){if(s.get("regressionFixtureRefs")==null)return List.of();if(!(s.get("regressionFixtureRefs") instanceof List<?> l)||l.size()>100)throw DomainError.invalid("regressionFixtureRefs must be a list");var out=new ArrayList<String>();for(Object o:l){if(!(o instanceof String v)||v.isBlank()||v.length()>200)throw DomainError.invalid("Invalid regression fixture reference");out.add(v);}return out;}
  @SuppressWarnings("unchecked")
  private Definition parsePackage(DomainContext c,Map<String,Object> s,String id,String parent,String fixedVersion){
    if(!(s.get("package") instanceof Map<?,?> raw))throw DomainError.invalid("Definition package required");
    var pkg=new LinkedHashMap<String,Object>((Map<String,Object>)raw);
    Object org=pkg.remove("organizationId");if(org!=null&&!c.organizationId().equals(org))throw DomainError.invalid("Package organization differs from the caller organization");
    for(String k:List.of("id","state","contentHash"))if(pkg.remove(k)!=null)throw DomainError.invalid("Package "+k+" is assigned by the server");
    Object version=pkg.get("version");if(s.get("version")!=null){if(version!=null&&!Objects.equals(version,s.get("version")))throw DomainError.invalid("Package version differs from version slot");version=s.get("version");}
    if(fixedVersion!=null&&version!=null&&!fixedVersion.equals(version))throw DomainError.invalid("A revised draft keeps its version");if(fixedVersion!=null)version=fixedVersion;
    if(!(version instanceof String v)||v.isBlank()||v.length()>80)throw DomainError.invalid("Package version required");
    Object declaredParent=pkg.remove("parentVersionId");if(declaredParent!=null&&!Objects.equals(declaredParent,parent))throw DomainError.invalid("Package parent differs from parentDefinitionId");
    pkg.put("organizationId",c.organizationId());pkg.put("id",id);pkg.put("version",version);pkg.put("parentVersionId",parent);
    Definition d;try{d=strict.convertValue(pkg,Definition.class);}catch(IllegalArgumentException|NullPointerException e){throw DomainError.invalid("Definition package has unsupported or missing fields");}
    if(d.evaluatorVersion()==null||d.evaluatorVersion().isBlank()||d.schemaVersion()==null||d.schemaVersion().isBlank())throw DomainError.invalid("Package evaluatorVersion and schemaVersion required");
    for(var g:d.goals())if(validator.validatePredicate(d,g.predicate()).problems().stream().anyMatch(p->"UNSUPPORTED_OPERATOR".equals(p.code())))throw DomainError.invalid("Goal predicate names an operator outside the fixed evaluator set");
    return new Definition(d.organizationId(),d.id(),d.version(),d.parentVersionId(),null,null,d.evaluatorVersion(),d.schemaVersion(),d.nouns(),d.attributes(),d.verbs(),d.relations(),d.goals(),d.capabilities());
  }
  /** Plan §8 diff classes against the parent package: rename, optional attribute, required slot, unit, endpoint, relation meaning. */
  private List<String> diffClasses(DomainContext c,Definition d){
    if(d.parentVersionId()==null)return List.of();Definition p;try{p=definitions.get(c.organizationId(),d.parentVersionId());}catch(RuntimeException unavailable){return List.of();}
    var out=new TreeSet<String>();var before=new HashMap<String,Definition.Attribute>();p.attributes().forEach(a->before.put(a.nounType()+"."+a.name(),a));
    for(var a:d.attributes()){var old=before.get(a.nounType()+"."+a.name());if(old==null)out.add(a.minimumCount()==0?"OPTIONAL_ATTRIBUTE_ADDED":"REQUIRED_SLOT_CHANGED");else{if(!Objects.equals(old.unit(),a.unit()))out.add("UNIT_POLICY_CHANGED");if(old.minimumCount()!=a.minimumCount()||!Objects.equals(old.requiredStage(),a.requiredStage()))out.add("REQUIRED_SLOT_CHANGED");}}
    var goals=new HashMap<String,Definition.Goal>();p.goals().forEach(g->goals.put(g.name(),g));
    for(var g:d.goals()){var old=goals.get(g.name());if(old==null)continue;if(!Objects.equals(old.endpoint(),g.endpoint()))out.add("ENDPOINT_CHANGED");if(!Objects.equals(old.predicate().get("unit"),g.predicate().get("unit")))out.add("UNIT_POLICY_CHANGED");}
    var relations=new HashMap<String,Definition.Relation>();p.relations().forEach(x->relations.put(x.sourceType()+">"+x.targetType(),x));var relationNames=new HashSet<String>();p.relations().forEach(x->relationNames.add(x.name()));
    for(var x:d.relations()){var old=relations.get(x.sourceType()+">"+x.targetType());if(old==null)out.add("RELATION_MEANING_CHANGED");else if(old.minimumCount()!=x.minimumCount()||old.maximumCount()!=x.maximumCount()||old.cycleAllowed()!=x.cycleAllowed())out.add("RELATION_MEANING_CHANGED");else if(!old.name().equals(x.name()))out.add("RENAME");}
    var verbs=new HashMap<String,String>();p.verbs().forEach(v->verbs.put(v.capabilityId()+"/"+v.intentKind(),v.name()));for(var v:d.verbs()){var old=verbs.get(v.capabilityId()+"/"+v.intentKind());if(old!=null&&!old.equals(v.name()))out.add("RENAME");}
    return List.copyOf(out);
  }
  private Map<String,Object> submit(DomainContext c,Map<String,Object> s,Instant now){
    String org=c.organizationId(),id=uuid(s,"definitionId");var row=stored(c,id);String hash=hashOf(s,row);String kind=s.get("reviewKind")==null?"DEFINITION":text(s,"reviewKind",24);
    var payload=new LinkedHashMap<String,Object>();String validationId=null,migrationId=null;
    if(kind.equals("DEFINITION")){state(row,"DRAFT");validationId=uuid(s,"validationId");var v=r.validation(org,validationId).orElseThrow(DomainError::forbidden);if(!id.equals(v.get("definitionId"))||!hash.equals(v.get("hash")))throw DomainError.invalid("Validation belongs to another package revision");if(!Boolean.TRUE.equals(v.get("publishable")))throw new DomainError("REJECTED","DEFINITION_NOT_PUBLISHABLE","Only a publishable validation can be reviewed");
      for(String k:List.of("migrationId","affectedWorkIds","affectedDataIds","mapping","unsupportedValues","regressionRunIds"))if(s.containsKey(k))throw DomainError.invalid("Definition review carries no migration slot "+k);}
    else if(kind.equals("WORK_MIGRATION")){state(row,"PUBLISHED");migrationId=text(s,"migrationId",80);payload.put("affectedWorkIds",uuids(s,"affectedWorkIds",true));payload.put("affectedDataIds",uuids(s,"affectedDataIds",false));if(!(s.get("mapping") instanceof Map<?,?> mapping)||mapping.isEmpty())throw DomainError.invalid("Migration mapping required");payload.put("mapping",mapping);payload.put("unsupportedValues",s.get("unsupportedValues")==null?List.of():list(s,"unsupportedValues"));payload.put("regressionRunIds",s.get("regressionRunIds")==null?List.of():list(s,"regressionRunIds"));if(s.containsKey("validationId"))throw DomainError.invalid("Migration review is not a package validation");}
    else throw DomainError.invalid("Unsupported reviewKind");
    String rid=UUID.randomUUID().toString();var review=new LinkedHashMap<String,Object>();review.put("organizationId",org);review.put("ID",rid);review.put("definitionId",id);review.put("hash",hash);review.put("reviewKind",kind);review.put("validationId",validationId);review.put("migrationId",migrationId);review.put("payloadJson",encode(payload));review.put("actorId",c.actorId());review.put("recordedAt",now);r.insertReview(review);
    if(kind.equals("DEFINITION")){r.state(org,id,"DRAFT","IN_REVIEW");row.put("state","IN_REVIEW");}
    int rev=r.bump(org,id,c.actorId(),now);var out=response(org,id,hash,row,rev,Map.of("reviewId",rid,"reviewKind",kind));out.put("reviewId",rid);var data=new LinkedHashMap<String,Object>(payload);data.put("reviewId",rid);data.put("reviewKind",kind);if(migrationId!=null)data.put("migrationId",migrationId);out.put("data",data);return out;
  }
  private Map<String,Object> approve(DomainContext c,Map<String,Object> s,Instant now){
    String org=c.organizationId(),id=uuid(s,"definitionId");var row=stored(c,id);String hash=hashOf(s,row);String kind=s.get("reviewKind")==null?"DEFINITION":text(s,"reviewKind",24);String decision=text(s,"decision",16);
    if(!Set.of("APPROVED","REJECTED").contains(decision))throw DomainError.invalid("decision is APPROVED or REJECTED");
    var review=s.get("reviewId")!=null?r.review(org,uuid(s,"reviewId")).orElseThrow(DomainError::forbidden):r.latestReview(org,id,hash,kind).orElseThrow(()->DomainError.invalid("No review of this package revision"));
    if(!id.equals(review.get("definitionId"))||!hash.equals(review.get("hash"))||!kind.equals(review.get("reviewKind")))throw DomainError.invalid("Review belongs to another package revision");
    if(kind.equals("DEFINITION"))state(row,"IN_REVIEW");else state(row,"PUBLISHED");
    var actor=identities.actor(org,c.actorId()).orElseThrow(DomainError::forbidden);if(!"HUMAN".equals(actor.get("kind")))throw DomainError.forbidden();
    String drafter=r.lifecycle(org,id).map(x->String.valueOf(x.get("createdBy"))).orElse(null);if(c.actorId().equals(drafter)||c.actorId().equals(review.get("actorId")))throw DomainError.forbidden();
    String reason=s.get("reason")==null?null:text(s,"reason",500);if(decision.equals("REJECTED")&&reason==null)throw DomainError.invalid("Rejection reason required");
    Instant from=instant(s,"validFrom",now),until=s.get("validUntil")==null?(decision.equals("REJECTED")?from.plusSeconds(1):null):instant(s,"validUntil",null);if(until==null||!until.isAfter(from))throw DomainError.invalid("Approval validity window required");
    String policy=s.get("consumptionPolicy")==null?(kind.equals("DEFINITION")?"ONE_PACKAGE_PUBLISH":"ONE_MIGRATION"):text(s,"consumptionPolicy",40);if(!policy.equals(kind.equals("DEFINITION")?"ONE_PACKAGE_PUBLISH":"ONE_MIGRATION"))throw DomainError.invalid("Unsupported consumptionPolicy for "+kind);
    String goalChanges=null;if(kind.equals("WORK_MIGRATION")){Map<String,Object> payload;try{payload=json.readValue(String.valueOf(review.get("payloadJson")),Map.class);}catch(Exception e){throw new IllegalStateException(e);}if(s.get("affectedWorkIds")!=null&&!((List<?>)payload.get("affectedWorkIds")).containsAll(uuids(s,"affectedWorkIds",true)))throw DomainError.invalid("Approval names Work outside the review");if(s.get("goalChanges")!=null){if(!(s.get("goalChanges") instanceof Map<?,?> g))throw DomainError.invalid("goalChanges must be an object");goalChanges=encode(g);}}
    else for(String k:List.of("affectedWorkIds","goalChanges"))if(s.containsKey(k))throw DomainError.invalid("Package approval carries no migration slot "+k);
    String aid=UUID.randomUUID().toString();var a=new LinkedHashMap<String,Object>();a.put("organizationId",org);a.put("ID",aid);a.put("definitionId",id);a.put("hash",hash);a.put("reviewId",review.get("ID"));a.put("reviewKind",kind);a.put("approverId",c.actorId());a.put("decision",decision);a.put("reason",reason);a.put("validFrom",from);a.put("validUntil",until);a.put("consumptionPolicy",policy);a.put("goalChangesJson",goalChanges);a.put("recordedAt",now);r.insertApproval(a);
    if(kind.equals("DEFINITION")&&decision.equals("REJECTED")){r.state(org,id,"IN_REVIEW","DRAFT");row.put("state","DRAFT");}
    int rev=r.bump(org,id,c.actorId(),now);var out=response(org,id,hash,row,rev,Map.of("approvalId",aid,"decision",decision));out.put("approvalId",aid);out.put("decision",decision);return out;
  }
  private static List<String> uuids(Map<String,Object> s,String k,boolean required){if(s.get(k)==null){if(required)throw DomainError.invalid(k+" required");return List.of();}if(!(s.get(k) instanceof List<?> l)||l.size()>100||required&&l.isEmpty())throw DomainError.invalid("Invalid "+k);var out=new ArrayList<String>();for(Object o:l){Object v=o instanceof Map<?,?> m&&m.containsKey("id")?m.get("id"):o;if(!(v instanceof String id))throw DomainError.invalid("Invalid "+k);try{UUID.fromString(id);}catch(IllegalArgumentException e){throw DomainError.invalid("Invalid "+k);}out.add(id);}return out;}
  private static List<?> list(Map<String,Object> s,String k){if(!(s.get(k) instanceof List<?> l)||l.size()>100)throw DomainError.invalid("Invalid "+k);return l;}
}
