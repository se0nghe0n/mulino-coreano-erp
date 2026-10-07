package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Independent fixed expectations. No application state transitions or evaluator simulation. */
public final class AssertionEngine {
    public void check(JsonNode assertion, Map<String,JsonNode> results) {
        String op=Json.required(assertion,"op");
        JsonNode expected=assertion.get("expected");
        if (expected == null) fail("expected is missing");
        JsonNode value=select(assertion.get("source"),results,op.equals("absent"));
        if(assertion.has("unit")) {
            JsonNode observedUnit=select(assertion.get("unitSource"),results,false);
            checkUnit(observedUnit,assertion.get("unit"));
        }
        switch(op) {
            case "equals" -> require(value.equals(expected),"expected "+expected+", observed "+value);
            case "notEquals" -> require(!value.equals(expected),"unexpected equality: "+value);
            case "present" -> require(!value.isMissingNode() && !value.isNull(),"value is not present");
            case "absent" -> require(value.isMissingNode(),"value exists (null is an explicit value)");
            case "decimalEquals" -> require(decimal(value).compareTo(decimal(expected))==0,"decimal mismatch: "+value+" != "+expected);
            case "decimalAtMost" -> require(decimal(value).compareTo(decimal(expected))<=0,"decimal exceeds limit");
            case "decimalAtLeast" -> require(decimal(value).compareTo(decimal(expected))>=0,"decimal below limit");
            case "sumEquals" -> {
                require(value.isArray(),"sum source must be an observed array");
                BigDecimal sum=BigDecimal.ZERO; for(JsonNode n:value) sum=sum.add(decimal(n));
                require(sum.compareTo(decimal(expected))==0,"sum mismatch: "+sum+" != "+expected);
            }
            case "decimalDelta" -> {
                if(assertion.has("unit")) checkUnit(select(assertion.get("baselineUnitSource"),results,false),assertion.get("unit"));
                BigDecimal baseline=decimal(select(assertion.get("baseline"),results,false));
                require(decimal(value).subtract(baseline).compareTo(decimal(expected))==0,"delta mismatch");
            }
            case "count" -> {
                require(value.isArray() && expected.isIntegralNumber(),"count requires observed array and integer expected");
                require(value.size()==expected.intValue(),"count mismatch: "+value.size()+" != "+expected);
            }
            case "exactSet", "relationSet" -> require(set(value).equals(set(expected)),"set or relation mismatch: "+value+" != "+expected);
            case "unique" -> set(value);
            case "sameAs" -> require(value.equals(select(assertion.get("baseline"),results,false)),"two observations differ");
            case "fieldsPresent" -> {
                require(value.isArray() && expected.isArray(),"fieldsPresent requires row array and field list");
                require(!value.isEmpty(),"required rows are empty");
                for(JsonNode row:value) for(JsonNode field:expected) {
                    JsonNode f=row.get(field.asText()); require(f!=null && !f.isNull() && !(f.isTextual() && f.asText().isBlank()),"missing field "+field);
                }
            }
            case "timeEquals" -> require(instant(value).equals(instant(expected)),"instant mismatch");
            case "timeBefore" -> require(instant(value).isBefore(instant(expected)),"time is not before fixed boundary");
            case "timeAtMostSeconds" -> {
                Instant baseline=instant(select(assertion.get("baseline"),results,false));
                BigDecimal elapsed=BigDecimal.valueOf(Duration.between(baseline,instant(value)).toNanos(),9);
                require(elapsed.signum()>=0 && elapsed.compareTo(decimal(expected))<=0,"elapsed time outside fixed interval");
            }
            default -> fail("Unsupported assertion operator "+op);
        }
    }
    public JsonNode select(JsonNode source,Map<String,JsonNode> results,boolean allowAbsent) {
        if(source==null) fail("source is missing");
        String actionId=Json.required(source,"actionId");
        JsonNode result=results.get(actionId);
        if(result==null) fail("No result for source "+actionId);
        require("EXECUTED".equals(result.path("driverStatus").asText()),"source "+actionId+" is "+result.path("driverStatus").asText()+": "+result.path("reason").asText());
        require(result.path("provenance").path("scopeComplete").asBoolean(false),"source scope is incomplete");
        String pointer=source.path("pointer").asText();
        JsonNode value=result.at(pointer);
        if (value.isMissingNode() && allowAbsent) {
            int slash=pointer.lastIndexOf('/');
            JsonNode parent=slash<0 ? null : result.at(pointer.substring(0,slash));
            require(parent!=null && (parent.isObject() || parent.isArray()),"absence parent scope not observed as object/array");
            return value;
        }
        require(!value.isMissingNode() && !value.isNull(),"Missing/null observed value at "+actionId+pointer);
        JsonNode where=source.get("where");
        if(where!=null) {
            require(value.isArray(),"where source must be an array"); ArrayNode rows=Json.array();
            for(JsonNode row:value) {
                boolean match=true;
                for(var it=where.fields();it.hasNext();) { var entry=it.next(); JsonNode f=row.get(entry.getKey());
                    require(f!=null && !f.isNull(),"filter field missing: "+entry.getKey()); if(!f.equals(entry.getValue())) match=false;
                }
                if(match) rows.add(row);
            }
            value=rows;
        }
        JsonNode field=source.get("field");
        if(field!=null) {
            require(value.isArray(),"field projection source must be array"); ArrayNode values=Json.array();
            for(JsonNode row:value) {
                if(field.isArray()) { ArrayNode tuple=Json.array(); for(JsonNode name:field) tuple.add(requiredField(row,name.asText())); values.add(tuple); }
                else values.add(requiredField(row,field.asText()));
            }
            value=values;
        }
        return value;
    }
    private static void checkUnit(JsonNode observed,JsonNode expected) {
        if(observed.isArray()) { require(!observed.isEmpty(),"Unit-bearing rows are empty"); for(JsonNode u:observed) require(u.equals(expected),"Observed unit differs from fixed expected unit"); }
        else require(observed.equals(expected),"Observed unit differs from fixed expected unit");
    }
    private static JsonNode requiredField(JsonNode row,String field) {
        JsonNode value=row.get(field); require(value!=null && !value.isNull(),"projected field missing: "+field); return value;
    }
    private static Set<JsonNode> set(JsonNode value) {
        require(value.isArray(),"set source must be an observed array"); Set<JsonNode> values=new HashSet<>();
        for(JsonNode n:value) require(values.add(n),"duplicate set member/physical identity: "+n); return values;
    }
    private static BigDecimal decimal(JsonNode value) {
        require(value!=null && value.isTextual() && value.asText().matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?"),"decimal must be a known exact string: "+value);
        return new BigDecimal(value.asText());
    }
    private static Instant instant(JsonNode value) {
        require(value!=null && value.isTextual(),"time must be a known instant");
        try { return OffsetDateTime.parse(value.asText()).toInstant(); } catch(RuntimeException e) { throw new AssertionError("Invalid instant "+value,e); }
    }
    private static void require(boolean ok,String reason) { if(!ok) fail(reason); }
    private static void fail(String reason) { throw new AssertionError(reason); }
}
