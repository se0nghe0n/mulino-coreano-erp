package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.node.*;
import org.mulino.verification.Json;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;

/** Deliberately bounded Korean grammar. Unconsumed text => UNKNOWN, including JSON self-labels.
 * Spans are evidence of words the author asserted, not evidence that the asserted business facts are true. */
final class NativeResponseSemantics {
    private static final Map<String,String> SLOTS=Map.ofEntries(Map.entry("수량","quantity"),Map.entry("단위","unit"),
        Map.entry("품목","itemRef"),Map.entry("목적지","destinationRef"),Map.entry("기한","dueAt"),
        Map.entry("끝점","endpoint"),Map.entry("수량 기준","quantityMode"),Map.entry("담당자","ownerRef"));
    private static final Pattern QUESTION=Pattern.compile("^(.+?)(?:을|를) (?:알려|확인해)\\s*주(?:세요|십시오)\\.$");
    static ObjectNode extract(String original) {
        var result=Json.object().put("grammarVersion","ko-explicit-clauses-v1").put("complete",false)
            .put("status","UNKNOWN").put("sourceResponse",original);
        var spans=Json.array();result.set("spans",spans);
        var claims=Json.object().put("execution","UNKNOWN").put("completion","UNKNOWN").put("responsibility","UNKNOWN")
            .put("uncertainty","UNKNOWN");claims.set("assertions",Json.array());claims.set("residualObligations",Json.array());
        claims.set("missingSlots",Json.array());result.set("claims",claims);
        String text=Normalizer.normalize(original,Normalizer.Form.NFC).strip();
        if(text.isEmpty())return result.put("reason","EMPTY_RESPONSE");
        String[] clauses=text.split("(?<=\\.)\\s+");
        Set<String> dimensions=new HashSet<>(),slots=new TreeSet<>();
        for(String clause:clauses) {
            Matcher question=QUESTION.matcher(clause);String dimension=null,value=null;
            if(question.matches()) {
                String[] labels=question.group(1).split("(?:과|와) |,\\s*");
                boolean known=true;for(String label:labels){String slot=SLOTS.get(label);if(slot==null||!slots.add(slot)){known=false;break;}}
                if(!known)return result.put("reason","UNSUPPORTED_OR_DUPLICATE_MISSING_SLOT");
                dimension="execution";value="NEEDS_INPUT";
            } else {
                switch(clause) {
                    case "실행하지 않았습니다." -> {dimension="execution";value="NOT_EXECUTED";}
                    case "요청이 거부되었습니다." -> {dimension="execution";value="REJECTED";}
                    case "요청을 실행했습니다." -> {dimension="execution";value="EXECUTED";}
                    case "조회했습니다." -> {dimension="execution";value="READ";}
                    case "요청은 대기 중입니다." -> {dimension="execution";value="PENDING";}
                    case "업무가 완료되었습니다." -> {dimension="completion";value="FULFILLED";}
                    case "업무가 종료되었습니다." -> {dimension="completion";value="CLOSED";}
                    case "업무가 진행 중입니다." -> {dimension="completion";value="OPEN";}
                    case "업무가 차단되었습니다." -> {dimension="completion";value="BLOCKED";}
                    case "담당자의 책임은 유지됩니다." -> {dimension="responsibility";value="PRESERVED";}
                    case "담당자의 책임이 해제되었습니다." -> {dimension="responsibility";value="RELEASED";}
                    case "책임이 이전되었습니다." -> {dimension="responsibility";value="TRANSFERRED";}
                    case "결과는 미확인입니다." -> {dimension="uncertainty";value="EXPLICIT_UNKNOWN";}
                    default -> {return result.put("reason","UNCONSUMED_TEXT");}
                }
            }
            if(!dimensions.add(dimension))return result.put("reason","DUPLICATE_OR_CONFLICTING_CLAUSE");
            claims.put(dimension,value);spans.add(Json.object().put("text",clause).put("dimension",dimension).put("value",value));
        }
        claims.set("missingSlots",Json.MAPPER.valueToTree(slots));
        // Full grammar coverage describes explicit claims; arbitrary factual/quantity prose is not covered.
        if(!dimensions.contains("execution"))return result.put("reason","EXECUTION_CLAIM_UNOBSERVED");
        if(!dimensions.contains("completion"))claims.put("completion","NOT_CLAIMED");
        if(!dimensions.contains("responsibility"))claims.put("responsibility","NOT_CLAIMED");
        if(!dimensions.contains("uncertainty"))claims.put("uncertainty","NOT_CLAIMED");
        result.put("complete",true).put("status","EXTRACTED");return result;
    }
    private NativeResponseSemantics() {}
}
