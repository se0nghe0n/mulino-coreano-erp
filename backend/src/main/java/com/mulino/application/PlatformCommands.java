package com.mulino.application;
import com.mulino.domain.inventory.ScopeRepository; import com.mulino.domain.identity.CurrentIdentity; import com.mulino.domain.identity.CurrentIdentity.Identity; import com.sap.cds.ql.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import org.springframework.security.access.AccessDeniedException; import java.util.*; import java.math.BigDecimal; import java.nio.charset.StandardCharsets; import java.security.MessageDigest;
@Service public class PlatformCommands {
 private final ScopeRepository r; private final CurrentIdentity identities;
 public PlatformCommands(ScopeRepository r,CurrentIdentity identities){this.r=r;this.identities=identities;}
 public Map<String,Object> read(String id){ var who=identities.get(); var s=r.scope(id); authorize(who,s,false); return s; }
 public List<Map<String,Object>> list(){var who=identities.get();return r.db.run(Select.from("mulino.platform.Scopes").where(x->x.get("organizationId").eq(who.organization()))).listOf(Map.class).stream().map(x->(Map<String,Object>)x).toList();}
 private void authorize(Identity who,Map<String,Object> s,boolean write){
  if(!who.organization().equals(s.get("organizationId"))) throw new AccessDeniedException("Unavailable scope");
  var grants=r.db.run(Select.from("mulino.platform.Grants").where(x->x.get("scopeId").eq(s.get("ID")).and(x.get("actor").eq(who.actor())).and(x.get("organizationId").eq(who.organization())))).listOf(Map.class);
  if(grants.isEmpty() || (write && !Boolean.TRUE.equals(grants.getFirst().get("allowed")))) throw new AccessDeniedException("Current grant denied");
 }
 @Transactional public Map<String,Object> reserve(String scopeId,String quantity,int expectedRevision,String key){
  var who=identities.get(); var q=new BigDecimal(quantity); if(q.signum()<=0 || q.scale()>12 || q.precision()>38 || key==null || key.isBlank()) throw new IllegalArgumentException("Invalid command");
  r.fence(scopeId); var s=r.scope(scopeId); authorize(who,s,true);
  var policy=r.db.run(Select.from("mulino.platform.Policies").where(x->x.get("scopeId").eq(scopeId))).first().orElseThrow(()->new AccessDeniedException("Policy unresolved"));
  if(!"ALLOWED".equals(policy.get("state"))) throw new AccessDeniedException("Policy unresolved");
  if(Boolean.TRUE.equals(s.get("blocked")) || r.db.run(Select.from("mulino.platform.Restrictions").where(x->x.get("scopeId").eq(scopeId).and(x.get("active").eq(true)))).rowCount()>0) throw new Conflict("BLOCKED");
  String hash=hash(scopeId+"|"+q.stripTrailingZeros().toPlainString()+"|"+expectedRevision);
  var previous=r.db.run(Select.from("mulino.platform.Idempotency").where(x->x.get("organizationId").eq(who.organization()).and(x.get("owner").eq(who.owner())).and(x.get("capability").eq("platform.reserve")).and(x.get("commandKey").eq(key)))).first();
  if(previous.isPresent()){if(!hash.equals(previous.get().get("intentHash"))) throw new Conflict("IDEMPOTENCY_CONFLICT"); return decode((String)previous.get().get("response"));}
  int revision=((Number)s.get("revision")).intValue(); if(revision!=expectedRevision)throw new Conflict("REVISION_CONFLICT");
  BigDecimal reserved=(BigDecimal)s.get("reserved"), total=(BigDecimal)s.get("quantity"); if(reserved.add(q).compareTo(total)>0)throw new Conflict("INSUFFICIENT_QUANTITY");
  r.update("Scopes",scopeId,Map.of("reserved",reserved.add(q),"revision",revision+1));
  r.insert("Audit",Map.of("ID",UUID.randomUUID().toString(),"scopeId",scopeId,"actor",who.actor(),"operation","RESERVE"));
  String operation=UUID.randomUUID().toString();r.insert("Outbox",Map.of("ID",operation,"scopeId",scopeId,"operationId",operation,"state","PENDING"));
  var result=Map.<String,Object>of("outcome","ACCEPTED","revision",revision+1,"reserved",reserved.add(q).toPlainString(),"operationId",operation);
  String response=encode(result);r.insert("Idempotency",Map.of("ID",UUID.randomUUID().toString(),"organizationId",who.organization(),"owner",who.owner(),"capability","platform.reserve","commandKey",key,"intentHash",hash,"response",response)); return result;
 }
 @Transactional public void restrict(String id){r.fence(id);var s=r.scope(id);authorize(identities.get(),s,true); r.insert("Restrictions",Map.of("ID",UUID.randomUUID().toString(),"scopeId",id,"active",true));}
 @Transactional public void revoke(String id,String actor){r.fence(id);var s=r.scope(id);authorize(identities.get(),s,true);r.db.run(Update.entity("mulino.platform.Grants").data("allowed",false).where(x->x.get("scopeId").eq(id).and(x.get("actor").eq(actor))));}
 private static String hash(String s){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 private static String encode(Map<String,Object> m){try{return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(m);}catch(Exception e){throw new IllegalStateException(e);}}
 private static Map<String,Object> decode(String s){try{return new com.fasterxml.jackson.databind.ObjectMapper().readValue(s,Map.class);}catch(Exception e){throw new IllegalStateException(e);}}
 public static class Conflict extends RuntimeException {public Conflict(String message){super(message);}}
}
