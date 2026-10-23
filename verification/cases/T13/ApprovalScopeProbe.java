import com.fasterxml.jackson.databind.*;
import com.mulino.application.core.*;
import com.mulino.application.identity.IdentityAuthorization;
import com.mulino.application.trade.purchase.PurchaseCommands;
import com.mulino.domain.identity.IdentityRepository;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Deterministic fixture-row test of real product authorization; no HTTP/DB/actual scenario claim. */
public class ApprovalScopeProbe {
  static final class Rows extends IdentityRepository {
    final Map<String,List<Map<String,Object>>> data = new HashMap<>();
    Rows() { super(null,null); }
    public List<Map<String,Object>> rows(String table,String org) {
      return data.getOrDefault(table,List.of()).stream()
          .filter(row->org.equals(row.get("organizationId"))).toList();
    }
    void add(String table,Object... pairs) {
      var row=new HashMap<String,Object>(); row.put("organizationId","ORG");
      for(int i=0;i<pairs.length;i+=2)row.put((String)pairs[i],pairs[i+1]);
      data.computeIfAbsent(table,key->new ArrayList<>()).add(row);
    }
  }
  static Rows rows(JsonNode fixture) {
    var rows=new Rows(); var rootActions=new HashSet<String>();
    fixture.path("actors").fields().forEachRemaining(entry->{
      String actor=entry.getKey(); var spec=entry.getValue(); var grant=spec.path("grant");
      String from=grant.path("validFrom").asText(), until=grant.path("validUntil").asText();
      rows.add("Actors","ID",actor,"kind","HUMAN","stableRequestOwner",actor);
      rows.add("Memberships","actorId",actor,"validFrom",from,"validUntil",until);
      for(var cap:spec.path("roleCapabilities"))rows.add("CapabilityAssignments","actorId",actor,
          "capabilityId",cap.asText(),"scopeKind","ORGANIZATION","scopeId","ORG","validFrom",from,"validUntil",until);
      rows.add("Grants","ID",actor+"-grant","actorId",actor,"delegatorId",grant.path("delegatorAlias").asText(),"validFrom",from,"validUntil",until);
      for(var cap:grant.path("actions")) {
        rows.add("GrantActions","grantId",actor+"-grant","capabilityId",cap.asText());
        rootActions.add(cap.asText());
      }
      for(var item:grant.path("scope").path("items"))rows.add("GrantScopes","grantId",actor+"-grant","scopeKind","ITEM","scopeId",item.asText());
    });
    // Existing installer behavior, not a new fixture grant: implicit root delegates
    // the union of authored actions. The patch adds no actor or grant privileges.
    var grant=fixture.path("actors").path("manager").path("grant");
    String from=grant.path("validFrom").asText(),until=grant.path("validUntil").asText();
    rows.add("Actors","ID","supervisor","kind","HUMAN","stableRequestOwner","supervisor");
    rows.add("Memberships","actorId","supervisor","validFrom",from,"validUntil",until);
    rows.add("Grants","ID","supervisor-grant","actorId","supervisor","delegatorId","supervisor","validFrom",from,"validUntil",until);
    rows.add("GrantScopes","grantId","supervisor-grant","scopeKind","ORGANIZATION","scopeId","ORG");
    for(String cap:rootActions) {
      rows.add("GrantActions","grantId","supervisor-grant","capabilityId",cap);
      rows.add("CapabilityAssignments","actorId","supervisor","capabilityId",cap,"scopeKind","ORGANIZATION","scopeId","ORG","validFrom",from,"validUntil",until);
    }
    for(var authority:fixture.path("baseline").path("managementAuthorities"))
      rows.add("ManagementAuthorities","actorId",authority.path("actorAlias").asText(),
          "capabilityId",authority.path("capabilityId").asText(),"scopeKind",authority.path("scopeKind").asText(),
          "scopeId",authority.path("scopeAlias").asText(),"validFrom",authority.path("validFrom").asText(),"validUntil",authority.path("validUntil").asText());
    return rows;
  }
  static void check(boolean value,String message) { if(!value)throw new AssertionError(message); }
  static boolean manager(Rows rows,Instant now,String actor,Map<String,List<String>> scopes) throws Exception {
    var clock=new ExecutionClock(Clock.fixed(now,ZoneOffset.UTC));
    var auth=new IdentityAuthorization(rows,clock);
    var commands=new PurchaseCommands(null,null,rows,auth,null,null,null,clock,null,null);
    var method=PurchaseCommands.class.getDeclaredMethod("manager",DomainContext.class,Map.class);
    method.setAccessible(true);
    try {method.invoke(commands,new DomainContext("ORG",actor,actor,now,now),scopes);return true;}
    catch(java.lang.reflect.InvocationTargetException exception) {
      if(exception.getCause() instanceof DomainError error && error.code().equals("FORBIDDEN"))return false;
      throw exception;
    }
  }
  public static void main(String[] args) throws Exception {
    var fixture=new ObjectMapper().readTree(Path.of(args[0]).toFile());
    var now=Instant.parse(fixture.path("clock").path("asOf").asText());
    var scope=Map.of("ITEM",List.of("P"),"WORK",List.of("created-work"),"PLACE",List.of("W"),"SUPPLIER",List.of("SUP"));
    check(manager(rows(fixture),now,"manager",scope),"positive manager P");
    check(manager(rows(fixture),now,"manager",Map.of("ITEM",List.of("P-alt"))),"positive manager P-alt");
    check(!manager(rows(fixture),now,"manager",Map.of("ITEM",List.of("P-outside"))),"outside item denied");
    check(!manager(rows(fixture),now,"unauthorized",scope),"negative actor denied");
    var missing=rows(fixture);missing.data.remove("ManagementAuthorities");
    var auth=new IdentityAuthorization(missing,new ExecutionClock(Clock.fixed(now,ZoneOffset.UTC)));
    check(auth.permittedScopes(new DomainContext("ORG","manager","manager",now,now),"approvePurchase",scope),"old fixture already passes capability and delegation");
    check(!manager(missing,now,"manager",scope),"old fixture fails only management prerequisite");
    var noParent=rows(fixture);noParent.data.get("Memberships").removeIf(row->row.get("actorId").equals("supervisor"));
    check(!manager(noParent,now,"manager",scope),"missing delegator membership denied");
    var expired=rows(fixture);expired.data.get("ManagementAuthorities").forEach(row->row.put("validUntil",now.toString()));
    check(!manager(expired,now,"manager",scope),"expired management denied");
    var revoked=rows(fixture);revoked.data.get("ManagementAuthorities").forEach(row->row.put("revokedAt",now.toString()));
    check(!manager(revoked,now,"manager",scope),"revoked management denied");
    check(!manager(rows(fixture),Instant.parse("2026-11-01T00:00:00Z"),"manager",scope),"expired membership/grant denied");
    var intersect=rows(fixture);intersect.add("GrantScopes","grantId","manager-grant","scopeKind","PLACE","scopeId","W");
    check(manager(intersect,now,"manager",scope),"matching item AND place allowed");
    check(!manager(intersect,now,"manager",Map.of("ITEM",List.of("P"))),"missing place denied");
    check(!manager(intersect,now,"manager",Map.of("ITEM",List.of("P"),"PLACE",List.of("W-outside"))),"mismatching place denied");
    System.out.println("13 product authorization checks passed: "+fixture.path("fixtureId").asText());
  }
}
