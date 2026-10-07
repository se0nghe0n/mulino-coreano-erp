package com.mulino.application.core;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.access.AccessDeniedException;
/** One product write pipeline shared by REST, CAP and MCP. */
@Service
public class ApplicationCommands {
  private final Map<String,CommandHandler> handlers=new TreeMap<>();
  private final CommandDefinitionResolver definitions;private final ReadAuthorizer auth;private final CommandRepository repository;private final ExecutionClock clock;
  private final ObjectProvider<CommandGuard> guard;private final ObjectProvider<CommandLeasePort> leases;
  private final TransactionTemplate transaction;private final TransactionTemplate denial;
  public ApplicationCommands(List<CommandHandler> handlers,ReadAuthorizer auth,CommandRepository repository,
      ExecutionClock clock,CommandDefinitionResolver definitions,ObjectProvider<CommandGuard> guard,ObjectProvider<CommandLeasePort> leases,PlatformTransactionManager manager){
    this.definitions=definitions;this.auth=auth;this.repository=repository;this.clock=clock;this.guard=guard;this.leases=leases;
    for(CommandHandler h:handlers)for(String cap:h.capabilities())if(this.handlers.put(cap,h)!=null)throw new IllegalStateException("Duplicate command capability "+cap);
    transaction=new TransactionTemplate(manager);transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    denial=new TransactionTemplate(manager);denial.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }
  public Set<String> operations(){return Set.copyOf(handlers.keySet());}
  public List<Map<String,Object>> manifest(){return handlers.entrySet().stream().map(e->Map.<String,Object>of("capabilityId",e.getKey(),"semanticVersion",e.getValue().semanticVersion(),"definitionVersions",e.getValue().definitionVersions(),"intentKinds",e.getValue().intentKinds())).toList();}
  public Map<String,Object> validate(Map<String,Object> input){
    return transaction.execute(status->{Map<String,Object> intent=typed(input,false);DomainContext c=auth.context(clock.instant(),clock.instant());CommandHandler h=handler(intent);definitions.verify(c,intent,h);CommandPreparation prep=h.prepare(c,intent);auth.authorizeScopes(c,(String)intent.get("capabilityId"),prep.scopes());return Map.of("outcome","VALIDATED","canonicalIntentHash",CommandRequests.hash(intent),"proposalRevision",prep.proposalRevision()>0?prep.proposalRevision():1,"capabilityVersion",h.semanticVersion(),"effectClass",prep.effectClass(),"scope",prep.scopes());});
  }
  /** Safe operational replay of the stored canonical command; never creates a new revision or key. */
  public Map<String,Object> retryOriginal(String commandRecordId,Map<String,Object> serverClaim){
    CommandRequests.uuid(commandRecordId);
    Map<String,Object> original=transaction.execute(status->{
      DomainContext c=auth.context(clock.instant(),clock.instant());var row=repository.owned(c,commandRecordId).orElseThrow(DomainError::forbidden);
      if("UNKNOWN_EXTERNAL".equals(row.get("state")))throw new DomainError("REJECTED","EXTERNAL_RECONCILIATION_REQUIRED","External result requires reconciliation");
      auth.authorize(c,(String)row.get("capabilityId"),null);return repository.original(row);
    });
    return execute(original,serverClaim);
  }
  public Map<String,Object> execute(Map<String,Object> input){return execute(input,Map.of());}
  /** Internal runtime entrypoint; executionClaim is server-issued and never accepted by public adapters. */
  public Map<String,Object> execute(Map<String,Object> input,Map<String,Object> serverExecutionClaim){
    Map<String,Object> intent;
    try{intent=typed(input,true);}catch(DomainError failure){return failure.response();}
    String hash=CommandRequests.hash(intent);
    if(intent.containsKey("canonicalIntentHash")&&!hash.equals(intent.get("canonicalIntentHash")))return new DomainError("CONFLICT","IDEMPOTENCY_CONFLICT","Canonical intent changed").response();
    for(int attempt=0;attempt<3;attempt++)try{return transaction.execute(status->apply(intent,hash,serverExecutionClaim));}
    catch(DomainError failure){return reject(intent,hash,failure);}
    catch(AccessDeniedException failure){return reject(intent,hash,DomainError.forbidden());}
    catch(RuntimeException failure){if(transientFailure(failure)&&attempt<2)continue;throw failure;}
    throw new IllegalStateException("Unreachable retry state");
  }
  private Map<String,Object> typed(Map<String,Object> input,boolean executing){Map<String,Object> intent=new LinkedHashMap<>(CommandRequests.parse(input,executing));CommandHandler h=handlers.get(intent.get("capabilityId"));if(h!=null)intent.putIfAbsent("capabilityVersion",h.semanticVersion());else intent.putIfAbsent("capabilityVersion","UNKNOWN");return Map.copyOf(intent);}
  private CommandHandler handler(Map<String,Object> intent){CommandHandler h=handlers.get(intent.get("capabilityId"));if(h==null||!h.semanticVersion().equals(intent.get("capabilityVersion"))||!h.intentKinds().contains(intent.get("intentKind")))throw DomainError.unsupported();return h;}
  private Map<String,Object> apply(Map<String,Object> intent,String hash,Map<String,Object> claim){
    DomainContext c=auth.context(clock.instant(),clock.instant());String cap=(String)intent.get("capabilityId"),key=(String)intent.get("commandIdempotencyKey");
    CommandHandler h=handler(intent);definitions.verify(c,intent,h);CommandPreparation initial=h.prepare(c,intent);
    List<String> fences=new ArrayList<>(initial.fenceKeys());fences.add("command:"+c.stableRequestOwner()+":"+cap+":"+key);repository.fence(c,fences);
    CommandGuard current=guard.getIfAvailable();if(current==null)throw new DomainError("REJECTED","POLICY_UNRESOLVED","Current command guard unavailable");current.fence(c,initial);
    CommandPreparation prep=h.prepare(c,intent);if(!initial.authorityActors().equals(prep.authorityActors())||!initial.effectClass().equals(prep.effectClass())||!Objects.equals(initial.approvalAction(),prep.approvalAction())||!initial.scopes().equals(prep.scopes())||!new TreeSet<>(initial.fenceKeys()).equals(new TreeSet<>(prep.fenceKeys())))throw new DomainError("CONFLICT","STALE_REVISION","Command scope changed");
    auth.authorizeScopes(c,cap,prep.scopes());
    if(!claim.isEmpty()){CommandLeasePort lease=leases.getIfAvailable();if(lease==null)throw DomainError.forbidden();lease.fenceAndVerify(c,claim);}
    Optional<Map<String,Object>> old=repository.find(c,cap,key);
    if(old.isPresent()){if(!hash.equals(old.get().get("canonicalHash")))throw new DomainError("CONFLICT","IDEMPOTENCY_CONFLICT","Idempotency key has different content");if(old.get().get("resultJson")==null)throw new DomainError("CONFLICT","COMMAND_IN_PROGRESS","Command unavailable");return repository.result(old.get());}
    if(prep.currentRevision()!=null&&(!intent.containsKey("expectedRevision")||((Number)intent.get("expectedRevision")).intValue()!=prep.currentRevision()))throw new DomainError("CONFLICT","STALE_REVISION","Expected revision changed");
    if(intent.containsKey("proposalRevision")&&((Number)intent.get("proposalRevision")).intValue()!=(prep.proposalRevision()>0?prep.proposalRevision():1))throw new DomainError("CONFLICT","STALE_REVISION","Proposal revision changed");
    current.verify(c,cap,hash,prep,intent);
    String id=repository.begin(c,intent,hash,clock.instant(),true);Map<String,Object> result;String previous=CommandExecution.enter(id);
    try{result=new LinkedHashMap<>(h.execute(c,intent));}finally{CommandExecution.exit(previous);}
    if(!Set.of("APPLIED","ACCEPTED_PENDING_EXTERNAL","PENDING_EXTERNAL","WAITING_APPROVAL","NEEDS_INPUT","REJECTED","CONFLICT").contains(result.get("outcome")))throw new IllegalStateException("Invalid handler command outcome");
    if(Set.of("REJECTED","CONFLICT","NEEDS_INPUT","WAITING_APPROVAL").contains(result.get("outcome")))throw new DomainError((String)result.get("outcome"),"COMMAND_NOT_APPLIED","Command did not apply");
    if(!h.mutatesAuthorization(cap))auth.authorizeScopes(c,cap,prep.scopes());current.verifyCommit(c,cap,hash,prep,intent,h.mutatesAuthorization(cap));if(!claim.isEmpty())leases.getObject().fenceAndVerify(c,claim);
    current.consume(c,prep,intent,id);
    result.put("commandId",id);result.put("canonicalIntentHash",hash);result.put("proposalRevision",prep.proposalRevision()>0?prep.proposalRevision():1);
    result.putIfAbsent("effects",Map.of());repository.finish(c,id,intent,hash,result,clock.instant());return result;
  }
  /** Expected denial audit is committed separately only after the effect transaction rolled back. */
  private Map<String,Object> reject(Map<String,Object> intent,String hash,DomainError failure){
    Map<String,Object> result=failure.response();
    return denial.execute(status->{DomainContext c;try{c=auth.context(clock.instant(),clock.instant());}catch(AccessDeniedException unauthenticated){return result;}
      String cap=(String)intent.get("capabilityId"),key=(String)intent.get("commandIdempotencyKey");repository.fence(c,List.of("command:"+c.stableRequestOwner()+":"+cap+":"+key));var old=repository.find(c,cap,key);
      if(old.isPresent())return result;String id=repository.begin(c,intent,hash,clock.instant(),false);repository.finish(c,id,intent,hash,result,clock.instant());return result;});
  }
  private boolean transientFailure(RuntimeException failure){Throwable cause=failure;while(cause!=null){if(cause instanceof java.sql.SQLException sql&&Set.of("55P03","40P01","40001").contains(sql.getSQLState()))return true;cause=cause.getCause();}return false;}
}
