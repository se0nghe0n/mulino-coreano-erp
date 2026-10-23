import com.mulino.application.core.*;
import com.mulino.domain.runtime.RuntimeRepository;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Real enqueue contract, in-memory JDBC boundary only; no external call/DB/scenario. */
public class ExternalOperationProbe {
  static final class Rows extends JdbcTemplate {
    final Map<String,Map<String,Object>> rows=new HashMap<>();
    int inserts;
    @Override public List<Map<String,Object>> queryForList(String sql,Object... args) {
      var row=rows.get(args[1].toString());return row==null?List.of():List.of(row);
    }
    @Override public int update(String sql,Object... a) {
      var row=new HashMap<String,Object>();
      row.put("id",a[1]);row.put("commandid",a[2]);row.put("operation",a[4]);
      row.put("payloadjson",a[5]);row.put("actorid",a[6]);row.put("stablerequestowner",a[7]);
      rows.put(a[3].toString(),row);inserts++;return 1;
    }
  }
  static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
  static void fails(String code,Runnable action){
    try {action.run();throw new AssertionError("Expected "+code);}
    catch(DomainError error){check(code.equals(error.code()),error.code());}
  }
  public static void main(String[] args){
    var now=Instant.parse("2026-10-07T09:00:00Z");
    var c=new DomainContext("org","actor","owner",now,now);
    var rows=new Rows();var r=new RuntimeRepository(rows,new ExecutionClock(Clock.fixed(now,ZoneOffset.UTC)));
    TransactionSynchronizationManager.setActualTransactionActive(true);
    try {
      for(String id:args){
        UUID.fromString(id);
        String out=r.enqueue(c,"command",id,"dispatchPurchaseOrder",Map.of("orderId","order","quantity","100"));
        check(out.equals(r.enqueue(c,"command",id,"dispatchPurchaseOrder",Map.of("quantity","100","orderId","order"))),"same operation replay");
        fails("STALE_EXECUTION",()->r.enqueue(c,"command",id,"dispatchPurchaseOrder",Map.of("orderId","order","quantity","40")));
        fails("STALE_EXECUTION",()->r.enqueue(c,"new-command",id,"dispatchPurchaseOrder",Map.of("orderId","order","quantity","100")));
      }
      fails("TYPE_INVALID",()->r.enqueue(c,"command","T13-supplier-accept-external","dispatchPurchaseOrder",Map.of("quantity","100")));
      check(rows.inserts==args.length,"Only one outbox insert per stable operation");
      System.out.println("PASS real RuntimeRepository: "+args.length+" UUIDs, replay reuse, changed payload/command conflict, original label TYPE_INVALID");
    } finally {TransactionSynchronizationManager.setActualTransactionActive(false);}
  }
}
