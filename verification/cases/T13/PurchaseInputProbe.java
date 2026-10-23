import com.fasterxml.jackson.databind.*;
import com.mulino.application.core.*;
import com.mulino.application.trade.purchase.*;
import com.mulino.domain.trade.purchase.PurchaseRepository;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Real projection/subject/approval methods; memory rows, no HTTP/DB acceptance claim. */
public class PurchaseInputProbe {
  static final Instant NOW=Instant.parse("2026-10-07T09:00:00Z");
  static final DomainContext C=new DomainContext("org","actor","owner",NOW,NOW.plusSeconds(1));
  static final class Rows extends PurchaseRepository {
    final Map<String,List<Map<String,Object>>> tables=new HashMap<>();
    Rows(){super(null,null,new ExecutionClock(Clock.fixed(NOW,ZoneOffset.UTC)));}
    public List<Map<String,Object>> rows(DomainContext c,String table){return tables.getOrDefault(table,List.of());}
  }
  static String id(String label){return UUID.nameUUIDFromBytes(label.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();}
  static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
  static Object value(JsonNode node){
    if(node.has("$alias"))return id(node.path("$alias").asText());
    if(node.has("value"))return new BigDecimal(node.path("value").asText());
    return node.asText();
  }
  public static void main(String[] args)throws Exception {
    var json=new ObjectMapper();var tree=json.readTree(Path.of(args[0]).toFile());int fields=0,guards=0;
    ReadAuthorizer read=new ReadAuthorizer(){
      public DomainContext context(Instant a,Instant k){return C;}
      public void authorize(DomainContext c,String cap,String target){}
      public boolean permittedScopes(DomainContext c,String cap,Map<String,? extends Collection<String>> scopes){return true;}
    };
    for(var sub:tree.path("subcases")){
      String name=sub.path("id").asText();if(!name.startsWith("changed-"))continue;
      String field=name.substring("changed-".length());var rows=new Rows();
      var current=new HashMap<String,Object>(Map.of("ID",id("revision-row"),"proposalId",id("proposal"),"revision",2,"itemId",id("P"),"workId",id("work"),"recordedAt",NOW,"unit","BOX","currency","EUR"));
      for(var a:sub.path("actions"))if(a.path("id").asText().equals("revision"))current.put(field,value(a.path("request").path("slots").path("changes").path(field)));
      var root=new HashMap<String,Object>(Map.of("ID",id("proposal"),"currentRevision",2,"recordedAt",NOW));
      rows.tables.put("Proposals",List.of(root));rows.tables.put("ProposalRevisions",List.of(current));
      var query=new PurchaseQueries(rows,read);
      var result=query.query(C,new QueryRequest("getObject",id("proposal"),Map.of("objectType","PurchaseProposal"),Map.of(),50,null,null,NOW,NOW.plusSeconds(1),null));
      JsonNode actual=json.valueToTree(TransportValues.normalize(result.data()));
      for(var assertion:sub.path("assertions"))if(assertion.path("id").asText().startsWith("changed-field-")){
        String key=assertion.path("source").path("pointer").asText().replace("/response/data/","");
        Object expected=value(assertion.path("expected"));
        if(assertion.path("op").asText().equals("decimalEquals"))check(new BigDecimal(actual.path(key).asText()).compareTo(new BigDecimal(expected.toString()))==0,name+" decimal");
        else check(actual.path(key).asText().equals(expected.toString()),name+" "+key+" "+actual);
        fields++;
      }
      var commands=new PurchaseCommands(rows,null,null,null,null,null,null,new ExecutionClock(Clock.fixed(NOW,ZoneOffset.UTC)),null,null);
      var intent=Map.<String,Object>of("capabilityId","dispatchPurchaseOrder","slots",Map.of("proposalId",id("proposal")));
      var binding=commands.subjectBindings(C,intent,null).stream().filter(b->b.nounType().equals("TradeItem")).findFirst().orElseThrow();
      check(binding.targetIds().equals(Set.of(current.get("itemId").toString())),name+" current subject");
      if(field.equals("itemId"))check(!binding.targetIds().contains(id("P")),"old P cannot bind changed proposal");
      var method=PurchaseCommands.class.getDeclaredMethod("approveForDispatch",DomainContext.class,Map.class,Map.class,Map.class);method.setAccessible(true);
      root.put("currentApprovalId","new-approval");
      try {method.invoke(commands,C,intent,current,Map.of("ID","old-approval","consumptionPolicy","SINGLE_ORDER_REVISION"));throw new AssertionError("old approval accepted");}
      catch(java.lang.reflect.InvocationTargetException e){var d=(DomainError)e.getCause();check(d.outcome().equals("HELD")&&d.code().equals("APPROVAL_NOT_CURRENT"),d.toString());guards++;}
    }
    check(fields==9&&guards==7,"projection and guard coverage");
    System.out.println("PASS real PurchaseQueries/TransportValues: 7 revised fields + exact unit/currency; 7 current-subject/APPROVAL_NOT_CURRENT guards");
  }
}
