package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.*;
import java.time.OffsetDateTime;

/**
 * Binds every observer {@code data.data} field to the raw rows it came from.
 * The observer declares a fixed-grammar derivation (row source, literal filter, aggregate);
 * the harness recomputes it from {@code rawRows}. A field with no derivation, or whose value
 * differs from the recomputation, is rejected, so a projection copy or invented number cannot
 * stand in for independent DB evidence. This is mechanical aggregation, not business logic.
 */
public final class ObserverDerivations {
    private static final Set<String> AGGREGATES=Set.of("sum","count","single","distinct");
    private ObserverDerivations() {}

    public static void verify(JsonNode requestedSources,JsonNode observation) {
        Set<String> sources=new HashSet<>();for(JsonNode s:requestedSources) sources.add(s.asText());
        JsonNode values=observation.path("data"),derivations=observation.path("derivations");
        require(values.isObject(),"Observer data must be an object");
        require(derivations.isMissingNode() || derivations.isObject(),"Observer derivations must be an object");
        Map<String,JsonNode> declared=new TreeMap<>();
        for(var it=derivations.fields();it.hasNext();) {var e=it.next();String key=e.getKey().startsWith("/")?e.getKey():"/"+e.getKey();require(declared.put(key,e.getValue())==null,"Duplicate derivation "+key);}
        Map<String,JsonNode> leaves=new TreeMap<>();leaves(values,"",leaves);
        for(String pointer:leaves.keySet())
            require(declared.containsKey(pointer),"Observer data field '"+pointer+"' has no raw-row derivation; free-form observer values are not independent evidence");
        measurementRows(observation);
        for(var e:declared.entrySet()) {
            require(leaves.containsKey(e.getKey()),"Derivation '"+e.getKey()+"' has no observer data value");
            JsonNode expected=compute(e.getKey(),e.getValue(),sources,observation),actual=leaves.get(e.getKey());
            require(e.getValue().path("rowPointer").asText().equals("/measurementMetadataRows")?expected.equals(actual):equal(expected,actual),"Observer data field '"+e.getKey()+"' differs from its raw-row recomputation: declared "+actual+", recomputed "+expected);
        }
    }
    /** Scalars under nested objects are the only observer values; row lists belong in rawRows. */
    private static void leaves(JsonNode node,String pointer,Map<String,JsonNode> out) {
        if(node.isObject()) {for(var it=node.fields();it.hasNext();) {var e=it.next();leaves(e.getValue(),pointer+"/"+e.getKey().replace("~","~0").replace("/","~1"),out);}}
        else {require(!node.isArray(),"Observer data '"+pointer+"' is an array; row lists must be returned as rawRows with sourceEvidence");require(!node.isNull(),"Observer data '"+pointer+"' is null; unknown values are not derived quantities");out.put(pointer,node);}
    }
    static JsonNode compute(String key,JsonNode d,Set<String> sources,JsonNode observation) {
        String pointer=d.path("rowPointer").asText(),aggregate=d.path("aggregate").asText();
        if(pointer.equals("/measurementMetadataRows")) {
            String field=key.equals("/itemId")?"id":key.equals("/unit")?"baseUnit":null;
            require(field!=null&&field.equals(d.path("field").asText())&&aggregate.equals("single")&&d.size()==3,
                "Measurement metadata may derive only itemId/id or unit/baseUnit with an unfiltered single row");
        } else require(pointer.startsWith("/rawRows/") && sources.contains(unescape(pointer.substring("/rawRows/".length()))),"Derivation '"+key+"' must read one requested raw source");
        require(AGGREGATES.contains(aggregate),"Derivation '"+key+"' has unsupported aggregate "+aggregate);
        JsonNode rows=observation.at(pointer);require(rows.isArray(),"Derivation '"+key+"' raw source is not an array");
        JsonNode where=d.path("where");require(where.isMissingNode() || where.isObject(),"Derivation where must be an object");
        for(JsonNode v:where) require(v.isValueNode() && !v.isNull(),"Derivation '"+key+"' filter values must be literal scalars");
        List<JsonNode> matched=new ArrayList<>();
        for(JsonNode row:rows) {
            boolean match=true;
            for(var f=where.fields();f.hasNext();) {var c=f.next();JsonNode v=row.get(c.getKey());require(v!=null && !v.isNull(),"Derivation '"+key+"' filter field missing: "+c.getKey());if(!v.equals(c.getValue())) match=false;}
            if(match) matched.add(row);
        }
        if(aggregate.equals("count")) return Json.MAPPER.getNodeFactory().numberNode(matched.size());
        String field=d.path("field").asText();require(!field.isBlank(),"Derivation '"+key+"' requires field");
        switch(aggregate) {
            case "sum" -> {
                BigDecimal total=BigDecimal.ZERO;JsonNode unit=null;String unitField=d.path("unitField").asText();
                for(JsonNode row:matched) {
                    total=total.add(decimal(row.get(field),key));
                    if(!unitField.isBlank()) {JsonNode u=row.get(unitField);require(u!=null && u.isTextual(),"Derivation '"+key+"' row unit missing");require(unit==null || unit.equals(u),"Derivation '"+key+"' sums rows with different units");unit=u;}
                }
                return Json.MAPPER.getNodeFactory().textNode(total.stripTrailingZeros().toPlainString());
            }
            case "single" -> {require(matched.size()==1,"Derivation '"+key+"' single requires exactly one row, matched "+matched.size());JsonNode v=matched.get(0).get(field);require(v!=null && !v.isNull(),"Derivation '"+key+"' field missing");return v;}
            default -> {
                require(!matched.isEmpty(),"Derivation '"+key+"' distinct requires at least one row");JsonNode first=matched.get(0).get(field);require(first!=null && !first.isNull(),"Derivation '"+key+"' field missing");
                for(JsonNode row:matched) require(first.equals(row.get(field)),"Derivation '"+key+"' distinct rows disagree");
                return first;
            }
        }
    }

    /** A fixed technical companion row, never an extra requested business source or an oracle exemption. */
    private static void measurementRows(JsonNode observation) {
        JsonNode rows=observation.path("measurementMetadataRows"),queries=observation.path("snapshot").path("measurementMetadataQueries");
        if(rows.isMissingNode()&&queries.isMissingNode())return;
        require(rows.isArray()&&rows.size()<=1,"Measurement metadata requires zero or one actual TradeItems row");
        require(queries.isArray()&&queries.size()==1,"Measurement metadata requires exactly one actual SQL query");
        require("REPEATABLE_READ".equals(observation.path("snapshot").path("isolation").asText()),"Measurement metadata must share the REPEATABLE_READ snapshot");
        String org=singleScope(observation.path("scope"),"organizationId","organizationIds"),item=singleScope(observation.path("scope"),"itemId","itemIds");
        var query=queries.get(0);var bound=query.path("boundValues");
        require(query.size()==3&&"mulino_inventory_TradeItems".equals(query.path("table").asText())
            &&query.path("sql").asText().equals("SELECT r.* FROM mulino_inventory_TradeItems r WHERE r.organizationId=? AND r.ID=? AND r.recordedAt<=? AND r.createdAt<=? ORDER BY r.ID")
            &&bound.isArray()&&bound.size()==4,"Measurement metadata SQL must bind organization, ITEM and both knownAt fences");
        require(bound.get(0).isTextual()&&bound.get(1).isTextual()&&org.equals(bound.get(0).asText())&&item.equals(bound.get(1).asText()),"Measurement metadata SQL scope differs from observation scope");
        var known=instant(observation.path("knownAt"));
        require(known.equals(instant(bound.get(2)))&&known.equals(instant(bound.get(3))),"Measurement metadata SQL time bounds differ from knownAt");
        if(rows.isEmpty())return;
        var row=rows.get(0);
        require(row.isObject()&&row.size()==5&&row.has("baseUnit")&&row.path("id").isTextual()&&row.path("organizationId").isTextual()&&org.equals(row.path("organizationId").asText())&&item.equals(row.path("id").asText()),
            "Measurement metadata row must be the actual scoped organization/ITEM");
        require(!instant(row.path("recordedAt")).isAfter(known)&&!instant(row.path("createdAt")).isAfter(known),"Measurement metadata row is outside knownAt");
        require(row.path("baseUnit").isNull()||row.path("baseUnit").isTextual(),"Measurement metadata baseUnit must retain its stored type");
        if(observation.path("data").has("unit"))require(row.path("baseUnit").isTextual()&&!row.path("baseUnit").asText().isBlank(),"Measurement metadata has no known unit");
    }

    private static String singleScope(JsonNode scope,String singular,String plural) {
        var ids=new HashSet<String>();
        if(scope.has(singular)){require(scope.path(singular).isTextual()&&!scope.path(singular).asText().isBlank(),"Invalid measurement scope "+singular);ids.add(scope.path(singular).asText());}
        if(scope.has(plural)){require(scope.path(plural).isArray(),"Invalid measurement scope "+plural);for(var id:scope.path(plural)){require(id.isTextual()&&!id.asText().isBlank(),"Invalid measurement scope ID");ids.add(id.asText());}}
        require(ids.size()==1,"Measurement metadata requires exactly one organization and ITEM");return ids.iterator().next();
    }
    private static java.time.Instant instant(JsonNode value) {
        try {require(value.isTextual(),"Measurement metadata timestamp must be text");return OffsetDateTime.parse(value.asText()).toInstant();}
        catch(java.time.format.DateTimeParseException invalid){throw new IllegalArgumentException("Invalid measurement metadata timestamp",invalid);}
    }
    private static boolean equal(JsonNode expected,JsonNode actual) {
        if(expected.isTextual() && actual!=null && actual.isTextual() && isDecimal(expected.asText()) && isDecimal(actual.asText()))
            return new BigDecimal(expected.asText()).compareTo(new BigDecimal(actual.asText()))==0;
        return expected.equals(actual);
    }
    private static boolean isDecimal(String s) { return s.matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?"); }
    private static BigDecimal decimal(JsonNode v,String key) {
        require(v!=null && v.isTextual() && isDecimal(v.asText()),"Derivation '"+key+"' sums a non-decimal or unknown value: "+v);
        return new BigDecimal(v.asText());
    }
    private static String unescape(String token) { return token.replace("~1","/").replace("~0","~"); }
    private static void require(boolean ok,String message) { if(!ok) throw new IllegalArgumentException(message); }
}
