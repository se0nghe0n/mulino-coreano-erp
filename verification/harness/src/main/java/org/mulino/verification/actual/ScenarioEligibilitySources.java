package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.OffsetDateTime;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;
import org.mulino.verification.Json;

/** Current SELL source relationships. No permission, eligibility status or evaluator result is computed here. */
final class ScenarioEligibilitySources {
    static final List<String> CONDITIONS=List.of("QC","REGULATORY","LABEL","DISPOSITION","CUSTOMER_CONTRACT");
    private final ScenarioObservationProjection db;
    private final OffsetDateTime knownAt;
    private final Instant asOf;
    private final ArrayNode links=Json.array();

    ScenarioEligibilitySources(ScenarioObservationProjection db,OffsetDateTime knownAt,OffsetDateTime asOf) {
        this.db=db;this.knownAt=knownAt;
        if(asOf==null)throw unavailable("asOf is required");this.asOf=asOf.toInstant();
    }
    ArrayNode links(){return links.deepCopy();}

    ArrayNode read() throws Exception {
        var out=Json.array();
        var segments=new ArrayList<JsonNode>();db.read("mulino_inventory_QuantitySegments").forEach(segments::add);
        segments.sort(Comparator.comparing(r->r.path("id").asText()));
        // This projection is deliberately bounded to direct, independent leaves. It never guesses genealogy ranges.
        if(!db.read("mulino_inventory_GenealogyEdges").isEmpty())throw unavailable("genealogy requires an explicit source-range oracle");
        var bases=db.read("mulino_inventory_DispositionBases");var holds=db.read("mulino_inventory_Restrictions");
        var policies=db.read("mulino_trade_regulatory_Policies");var decisions=db.read("mulino_trade_regulatory_DecisionVersions");
        var labels=db.read("mulino_trade_regulatory_LabelVerifications");
        for(var segment:segments) {
            String org=text(segment,"organizationId"),sid=text(segment,"id");
            var item=required("mulino_inventory_TradeItems",segment,text(segment,"itemId"));
            var lot=required("mulino_inventory_ManufacturingLots",segment,text(segment,"lotId"));
            if(!same(item,segment,"organizationId")||!lot.path("itemId").equals(segment.path("itemId")))throw unavailable("ITEM/LOT binding");
            var context=Json.object();context.set("item",item);context.set("lot",lot);
            var activePolicies=Json.array();
            for(var policy:db.query("mulino_governance_ActivePolicies","r.organizationId=?",List.of(org))){
                var version=required("mulino_governance_PolicyVersions",policy,text(policy,"policyId"));
                var row=policy.deepCopy();((ObjectNode)row).set("version",version);activePolicies.add(row);
            }
            context.set("activePolicies",activePolicies);context.put("activePointerHistoryComplete",false);
            var current=new ArrayList<JsonNode>();
            for(var policy:policies)if(org.equals(policy.path("organizationId").asText())&&"SELL".equals(policy.path("action").asText())&&"ACTIVE".equals(policy.path("status").asText())&&valid(policy,false))current.add(policy);
            if(current.size()!=1)throw unavailable("one current SELL regulatory policy required");
            JsonNode policy=current.getFirst();context.set("regulatoryPolicy",policy);
            var refs=new LinkedHashMap<String,SortedSet<String>>();CONDITIONS.forEach(k->refs.put(k,new TreeSet<>()));
            for(var basis:bases)if(sid.equals(basis.path("segmentId").asText())&&"SELL".equals(basis.path("action").asText())&&active(basis)){
                if(basis.hasNonNull("customerId"))throw unavailable("customer-specific source requires an explicit observation context");
                String condition=condition(basis.path("category").asText());if(condition==null)continue;
                add(segment,condition,basis,"mulino_inventory_DispositionBases",text(basis,"evidenceRef"),true,
                    "QUALITY_DECISION_recordDispositionBasis_"+text(basis,"category"),refs);
            }
            for(var hold:holds)if(org.equals(hold.path("organizationId").asText())&&segment.path("controlScope").equals(hold.path("controlScope"))&&Set.of("SELL","ALL").contains(hold.path("action").asText())&&active(hold)){
                if(hold.hasNonNull("segmentId")&&!sid.equals(hold.path("segmentId").asText()))continue;
                String condition=condition(hold.path("category").asText());if(condition==null)continue;
                add(segment,condition,hold,"mulino_inventory_Restrictions",text(hold,"evidenceRef"),true,
                    "QUALITY_DECISION_placeHold_"+text(hold,"category"),refs);
            }
            for(var decision:decisions)if(scope(decision,segment,policy)&&"SELL".equals(decision.path("action").asText())&&valid(decision,false)){
                boolean superseded=false;for(var next:decisions)if(decision.path("id").equals(next.path("previousVersionId"))&&!instant(next,"validFrom").isAfter(asOf))superseded=true;
                if(superseded)continue;
                var procedure=procedure(decision,segment,policy);var version=head(procedure);
                if(!decision.path("procedureVersionId").equals(version.path("id"))||!"SUBMITTED".equals(version.path("status").asText()))continue;
                proof(version,segment,text(version,"occurrenceId"),false,"REGULATORY_SUBMISSION");
                add(segment,"REGULATORY",decision,"mulino_trade_regulatory_DecisionVersions",text(decision,"occurrenceId"),false,"REGULATORY_DECISION",refs);
            }
            if(!policy.path("requiresLabel").isBoolean())throw unavailable("stored requiresLabel is unknown");
            if(!policy.path("requiresLabel").booleanValue()){
                refs.get("LABEL").add(text(policy,"id"));
                links.add(link(segment,"LABEL",policy,"mulino_trade_regulatory_Policies",text(policy,"id"),null));
            } else {
                for(var procedure:db.read("mulino_trade_regulatory_Procedures"))if(scope(procedure,segment,policy)){
                    var version=head(procedure);JsonNode selected=null;
                    for(var label:labels)if(procedure.path("id").equals(label.path("procedureId"))&&version.path("id").equals(label.path("procedureVersionId"))&&!instant(label,"validFrom").isAfter(asOf)){
                        if(selected==null||label.path("revision").asInt()>selected.path("revision").asInt())selected=label;
                        else if(label.path("revision").equals(selected.path("revision"))&&!label.path("id").equals(selected.path("id")))throw unavailable("ambiguous label revision");
                    }
                    if(selected!=null&&valid(selected,false))add(segment,"LABEL",selected,"mulino_trade_regulatory_LabelVerifications",text(selected,"occurrenceId"),false,"LABEL_VERIFICATION",refs);
                }
            }
            for(String condition:CONDITIONS){var row=Json.object().put("segmentId",sid).put("conditionId",condition).put("action","SELL");
                row.set("evidenceRefs",Json.MAPPER.valueToTree(refs.get(condition)));row.set("contextSources",context.deepCopy());out.add(row);}
        }
        return out;
    }
    private void add(JsonNode segment,String condition,JsonNode source,String table,String ref,boolean document,String kind,Map<String,SortedSet<String>> refs)throws Exception {
        var proofs=proof(source,segment,ref,document,kind);
        refs.get(condition).add(ref);links.add(link(segment,condition,source,table,ref,proofs));
    }
    private ObjectNode link(JsonNode segment,String condition,JsonNode source,String table,String ref,ArrayNode proofs){
        var row=Json.object().put("segmentId",text(segment,"id")).put("conditionId",condition).put("evidenceId",ref).put("sourceTable",table).put("sourceId",text(source,"id"));
        row.set("sourceRow",source.deepCopy());if(proofs!=null)row.set("verificationChains",proofs);return row;
    }
    /** Persisted verified source chain + actual immutable bytes; no product API, authorization or business conclusion. */
    private ArrayNode proof(JsonNode source,JsonNode segment,String ref,boolean byDocument,String kind)throws Exception {
        String org=text(segment,"organizationId");
        var verified=db.query("mulino_evidence_Verifications","r.organizationId=? AND r."+(byDocument?"basisDocumentId":"canonicalOccurrenceId")+"=? AND r.recordedAt<=?",List.of(org,ref,knownAt));
        var out=Json.array();
        for(var verification:verified){
            if(!"VERIFIED".equals(verification.path("verdict").asText())||!List.of("sourceMatched","identityMatched","quantityMatched","timeMatched","duplicateChecked").stream().allMatch(k->verification.path(k).isBoolean()&&verification.path(k).booleanValue()))continue;
            var occurrence=required("mulino_evidence_CanonicalOccurrences",source,text(verification,"canonicalOccurrenceId"));
            var claim=required("mulino_evidence_Claims",source,text(verification,"claimId"));
            var event=required("mulino_evidence_Events",source,text(claim,"eventId"));
            var doc=required("mulino_evidence_DocumentVersions",source,text(verification,"basisDocumentId"));
            if(!kind.equals(occurrence.path("kind").asText())||!kind.equals(event.path("kind").asText())||!"COMPLETE".equals(occurrence.path("reassessmentState").asText())||!"KNOWN".equals(occurrence.path("valueState").asText())
                ||!segment.path("id").equals(occurrence.path("physicalScopeId"))||!segment.path("itemId").equals(occurrence.path("itemId"))
                ||!"AVAILABLE".equals(doc.path("availability").asText())||!doc.path("id").equals(claim.path("documentVersionId"))
                ||!same(occurrence,claim,"organizationId","subjectKind","subjectId","sourceProfileId")||!same(claim,event,"organizationId","subjectKind","subjectId","sourceProfileId")||!same(claim,doc,"organizationId","subjectKind","subjectId","sourceProfileId")
                ||!sameInstant(occurrence,event,"effectiveFrom")||!sameInstant(claim,event,"effectiveFrom")||instant(occurrence,"effectiveFrom").isAfter(asOf)
                ||!ScenarioObservationProjection.deliveryOriginal(event,doc))continue;
            var payload=ScenarioObservationProjection.parse(event,"payload");
            boolean bound=true;
            for(String key:List.of("category","action","segmentId","quantity","unit","startQuantity","validFrom","validUntil","procedureId","procedureVersionId","decision","packagingVersionId","specificationVersionId","occurredAt"))if(source.hasNonNull(key)){
                if(!payload.hasNonNull(key)||!equalValue(key,source.path(key),payload.path(key)))bound=false;
            }
            if(!bound)continue;
            var profile=required("mulino_evidence_SourceProfiles",source,text(event,"sourceProfileId"));
            if(!profile.path("namespace").equals(event.path("sourceNamespace"))||!profile.path("policyVersion").equals(verification.path("policyVersion")))continue;
            var inbox=db.query("mulino_evidence_InboxRecords","r.organizationId=? AND r.sourceNamespace=? AND r.externalEventId=? AND r.sourceVersion=? AND r.recordedAt<=?",List.of(org,text(event,"sourceNamespace"),text(event,"externalEventId"),text(event,"sourceVersion"),knownAt));
            if(inbox.size()!=1||"CONFLICT".equals(inbox.get(0).path("state").asText())||!inbox.get(0).path("eventId").equals(event.path("id"))||!inbox.get(0).path("payloadHash").equals(event.path("payloadHash")))continue;
            if(superseded(occurrence,"mulino_evidence_CanonicalOccurrences","supersedesId")||superseded(claim,"mulino_evidence_Claims","supersedesId")||superseded(event,"mulino_evidence_Events","supersedesId","invalidatesId")||superseded(doc,"mulino_evidence_DocumentVersions","supersedesId"))continue;
            var chain=Json.object().put("verificationId",text(verification,"id")).put("canonicalOccurrenceId",text(occurrence,"id")).put("documentId",text(doc,"id")).put("eventId",text(event,"id")).put("claimId",text(claim,"id")).put("sourceProfileId",text(profile,"id")).put("inboxId",text(inbox.get(0),"id"));out.add(chain);
        }
        if(out.isEmpty())throw unavailable("no verified immutable source for "+ref);return out;
    }
    private JsonNode procedure(JsonNode decision,JsonNode segment,JsonNode policy)throws Exception {
        var row=required("mulino_trade_regulatory_Procedures",decision,text(decision,"procedureId"));
        if(!scope(row,segment,policy))throw unavailable("procedure scope mismatch");return row;
    }
    private JsonNode head(JsonNode procedure)throws Exception {
        var rows=db.query("mulino_trade_regulatory_ProcedureVersions","r.organizationId=? AND r.procedureId=? AND r.recordedAt<=?",List.of(text(procedure,"organizationId"),text(procedure,"id"),knownAt));
        var heads=new ArrayList<JsonNode>();for(var row:rows){boolean prior=false;for(var next:rows)if(row.path("id").equals(next.path("previousVersionId")))prior=true;if(!prior)heads.add(row);}
        if(heads.size()!=1||!heads.getFirst().path("id").equals(procedure.path("currentVersionId")))throw unavailable("procedure head mismatch");return heads.getFirst();
    }
    private JsonNode required(String table,JsonNode owner,String id)throws Exception {var row=db.linked(table,owner,id);if(row==null)throw unavailable("missing "+table+"/"+id);return row;}
    private boolean superseded(JsonNode row,String table,String...keys)throws Exception {
        for(String key:keys)if(!db.query(table,"r.organizationId=? AND r."+key+"=? AND r.recordedAt<=?",List.of(text(row,"organizationId"),text(row,"id"),knownAt)).isEmpty())return true;return false;
    }
    private boolean active(JsonNode row){return valid(row,true)&&(row.hasNonNull("releasedAt")?instant(row,"releasedAt").isAfter(knownAt.toInstant())||asOf.isBefore(instant(row,"releasedAt")):"ACTIVE".equals(row.path("state").asText()));}
    private boolean valid(JsonNode row,boolean closed){return !instant(row,"validFrom").isAfter(asOf)&&(!row.hasNonNull("validUntil")||(closed?!instant(row,"validUntil").isBefore(asOf):instant(row,"validUntil").isAfter(asOf)));}
    private static boolean scope(JsonNode row,JsonNode segment,JsonNode policy){return same(row,segment,"organizationId","itemId","lotId")&&row.path("physicalScopeId").equals(segment.path("id"))&&row.path("policyId").equals(policy.path("id"));}
    private static String condition(String category){return switch(category){case "QC"->"QC";case "COMMERCIAL"->"DISPOSITION";case "CUSTOMER"->"CUSTOMER_CONTRACT";default->null;};}
    private static boolean sameInstant(JsonNode a,JsonNode b,String key){try{return instant(a,key).equals(instant(b,key));}catch(RuntimeException invalid){return false;}}
    private static boolean same(JsonNode a,JsonNode b,String...keys){for(String key:keys)if(!a.hasNonNull(key)||!b.hasNonNull(key)||!a.path(key).equals(b.path(key)))return false;return true;}
    private static boolean equalValue(String key,JsonNode a,JsonNode b){try{return Set.of("quantity","startQuantity").contains(key)?new BigDecimal(a.asText()).compareTo(new BigDecimal(b.asText()))==0:Set.of("validFrom","validUntil","occurredAt").contains(key)?Instant.parse(a.asText()).equals(Instant.parse(b.asText())):a.equals(b);}catch(RuntimeException invalid){return false;}}
    private static Instant instant(JsonNode row,String key){try{return Instant.parse(text(row,key));}catch(RuntimeException invalid){throw unavailable("invalid "+key);}}
    private static String text(JsonNode row,String key){if(!row.path(key).isTextual()||row.path(key).asText().isBlank())throw unavailable("missing "+key);return row.path(key).asText();}
    private static UnsupportedOperationException unavailable(String reason){return new UnsupportedOperationException("SELL condition source relationship unavailable: "+reason);}
}
