package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import org.mulino.verification.Json;

/**
 * Independent S4 oracles over already-resolved JSON: a product world read is
 * compared with JDBC ledger rows, and residual human duties are bound to their
 * exact Work, subject and quantity (plan §3.4, §13.3). No product code is used.
 */
final class S4WorldOracle {
    @FunctionalInterface interface Require {void that(boolean condition,String message);}
    private static final Pattern SQL_TIMESTAMP=Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}(\\.\\d+)?");
    private S4WorldOracle(){}

    /** Equality on every key; an array value means membership (e.g. workid in {WORK,SALES_WORK}). */
    static boolean matches(JsonNode row,JsonNode expected){
        for(var it=expected.fields();it.hasNext();){var e=it.next();JsonNode actual=row.path(e.getKey());
            if(e.getValue().isArray()&&!actual.isArray()){boolean member=false;for(JsonNode option:e.getValue())member|=actual.equals(option);if(!member)return false;}
            else if(!actual.equals(e.getValue()))return false;}
        return true;
    }

    /**
     * Each spec {kind, where, count} must match exactly count OPEN, valid duties
     * with a human owner, next action and next check; and every OPEN duty of a
     * listed kind must be one of the bound ones (no unrelated or duplicate duty).
     */
    static void humanDuties(Require require,String id,JsonNode rows,JsonNode specs,JsonNode actors){
        require.that(specs.isArray()&&!specs.isEmpty(),id+" humanDuties needs bound duties");
        var humans=new HashSet<String>();for(JsonNode a:actors)if("HUMAN".equals(field(a,"kind").asText()))humans.add(field(a,"id").asText());
        var expectedPerKind=new TreeMap<String,Integer>();
        for(JsonNode spec:specs){
            String kind=Json.required(spec,"kind");require.that(spec.path("count").isInt(),id+" humanDuties spec needs an exact count");require.that(spec.path("where").size()>0,id+" humanDuties spec must bind its Work/subject");
            int expected=spec.path("count").asInt();expectedPerKind.merge(kind,expected,Integer::sum);int count=0;
            for(JsonNode duty:open(rows,kind)){
                boolean bound=true;for(var it=spec.path("where").fields();it.hasNext();){var e=it.next();if(!sameValue(field(duty,e.getKey()),e.getValue()))bound=false;}
                if(!bound)continue;
                String owner=field(duty,"ownerid").asText();
                require.that(!owner.isBlank()&&!field(duty,"nextaction").asText().isBlank()&&!field(duty,"nextcheckat").asText().isBlank(),id+" residual duty lacks owner/next action/check "+kind);
                require.that(humans.contains(owner),id+" residual duty owner is not a human "+kind);count++;
            }
            require.that(count==expected,id+" expected "+expected+" bound open human duty "+kind+" "+spec.path("where")+" observed "+count);
        }
        for(var e:expectedPerKind.entrySet()){int open=open(rows,e.getKey()).size();require.that(open==e.getValue(),id+" unbound or duplicate open duty of kind "+e.getKey()+": expected "+e.getValue()+" observed "+open);}
    }
    private static List<JsonNode> open(JsonNode rows,String kind){var result=new ArrayList<JsonNode>();for(JsonNode duty:rows)if(kind.equals(field(duty,"kind").asText())&&"OPEN".equals(field(duty,"status").asText())&&!"false".equals(field(duty,"valid").asText()))result.add(duty);return result;}

    /** The product rows are exactly the selected ledger rows, field by field. */
    static void ledgerRows(Require require,String id,JsonNode product,JsonNode ledgerRows,JsonNode where){
        require.that(product.isArray()&&ledgerRows.isArray(),id+" ledgerRows needs arrays");
        var byId=new TreeMap<String,JsonNode>();for(JsonNode row:ledgerRows)if(matches(row,where))require.that(byId.put(field(row,"id").asText(),row)==null,id+" duplicate ledger row");
        var seen=new TreeSet<String>();
        for(JsonNode row:product){String key=field(row,"id").asText();require.that(seen.add(key),id+" duplicate product row "+key);JsonNode source=byId.get(key);require.that(source!=null,id+" product row absent from ledger "+key);
            for(var it=row.fields();it.hasNext();){var e=it.next();JsonNode column=field(source,e.getKey());require.that(!column.isMissingNode(),id+" product field "+e.getKey()+" has no ledger column");require.that(sameValue(e.getValue(),column),id+" "+key+" field "+e.getKey()+" product "+e.getValue()+" ledger "+column);}}
        require.that(seen.equals(byId.keySet()),id+" ledger rows missing from product "+byId.keySet()+" observed "+seen);
    }

    /** Owners of the ledger Works plus owners of their OPEN valid duties. */
    static void ledgerOwners(Require require,String id,JsonNode product,JsonNode works,JsonNode duties,JsonNode workIds){
        var ids=set(require,id,workIds);var owners=new TreeSet<String>();
        for(JsonNode w:works)if(ids.contains(field(w,"id").asText()))owners.add(field(w,"ownerid").asText());
        for(JsonNode o:duties)if(ids.contains(field(o,"workid").asText())&&"OPEN".equals(field(o,"status").asText())&&!"false".equals(field(o,"valid").asText()))owners.add(field(o,"ownerid").asText());
        require.that(set(require,id,product).equals(owners),id+" expected ledger owners "+owners+" observed "+product);
    }

    static Set<String> set(Require require,String id,JsonNode values){var result=new TreeSet<String>();for(JsonNode v:values)require.that(result.add(v.asText()),id+" duplicate set member "+v);return result;}

    /** Case-insensitive column lookup; a dotted path enters scope/scopeJson (parsed when stored as text). */
    static JsonNode field(JsonNode row,String name){
        String[] path=name.split("\\.");JsonNode current=column(row,path[0]);
        if(current.isMissingNode()&&path[0].equalsIgnoreCase("scope"))current=column(row,"scopejson");
        for(int i=1;i<path.length;i++){if(current.isTextual())try{current=Json.MAPPER.readTree(current.asText());}catch(Exception malformed){return MissingNode.getInstance();}current=column(current,path[i]);}
        return current;
    }
    private static JsonNode column(JsonNode row,String name){for(var it=row.fields();it.hasNext();){var e=it.next();if(e.getKey().equalsIgnoreCase(name))return e.getValue();}return MissingNode.getInstance();}

    /** Product JSON vs JDBC value: same instant, same decimal, same parsed JSON, or same scalar text. */
    static boolean sameValue(JsonNode product,JsonNode ledger){
        boolean productNull=product==null||product.isNull()||product.isMissingNode(),ledgerNull=ledger==null||ledger.isNull()||ledger.isMissingNode();
        if(productNull||ledgerNull)return productNull&&ledgerNull;
        if(product.isContainerNode()){JsonNode parsed=ledger;if(ledger.isTextual())try{parsed=Json.MAPPER.readTree(ledger.asText());}catch(Exception malformed){return false;}return product.equals(parsed);}
        String p=product.asText(),l=ledger.asText();
        // S4JdbcObservation renders TIMESTAMP with java.sql.Timestamp#toString in this JVM's zone.
        if(SQL_TIMESTAMP.matcher(l).matches()){try{return java.sql.Timestamp.valueOf(l).toInstant().equals(Instant.parse(p));}catch(Exception notInstant){return false;}}
        if(decimal(p)&&decimal(l))return new BigDecimal(p).compareTo(new BigDecimal(l))==0;
        return p.equals(l);
    }
    private static boolean decimal(String text){return text.matches("-?[0-9]+(\\.[0-9]+)?");}
}
