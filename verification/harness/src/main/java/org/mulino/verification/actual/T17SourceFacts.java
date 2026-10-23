package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.mulino.verification.Json;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;

/** T17 given facts become originals and public command results, never seeded permission/verdict rows. */
final class T17SourceFacts {
    private final ActualConfiguration config;
    private final ObjectNode fixture, aliases;
    private final String external, actor, namespace, policyVersion;
    private final HttpClient http=HttpClient.newHttpClient();
    private final JwtSigner signer;
    private final ArrayNode commands=Json.array(), conditions=Json.array(), provenance=Json.array(), uninstalled=Json.array();
    record Source(String document,String event,String review,String canonical) {}
    static boolean enabled(JsonNode f){return f.path("fixtureId").asText().startsWith("T17-")&&f.path("baseline").path("t17SourceFacts").path("version").asInt()==1;}
    static void validate(JsonNode f){
        if(!enabled(f)||!f.path("synthetic").asBoolean()||!f.path("baseline").path("t17SourceFacts").path("legalComplianceClaimed").isBoolean()||f.path("baseline").path("t17SourceFacts").path("legalComplianceClaimed").asBoolean())throw new IllegalArgumentException("Explicit synthetic T17 source configuration required");
        var spec=f.path("baseline").path("t17SourceFacts");String actor=Json.required(spec,"actorAlias");
        if(!f.path("actors").has(actor)||!f.path("aliases").has(Json.required(spec,"policyAlias")))throw new IllegalArgumentException("Source actor and policy required");
        var policy=f.path("aliases").path(Json.required(spec,"policyAlias"));
        if(!policy.path("fictional").asBoolean()||!Json.required(spec,"namespace").equals(policy.path("sourceNamespace").asText()))throw new IllegalArgumentException("Fictional source policy namespace mismatch");
        if(!"RegulatoryPolicy".equals(policy.path("type").asText())||!actor.equals(policy.path("actorAlias").asText()))throw new IllegalArgumentException("Typed policy must name the source actor");
        JsonNode sourceProfile=null;for(var p:f.path("baseline").path("sourceProfiles"))if(spec.path("namespace").asText().equals(p.path("namespace").asText()))sourceProfile=p;
        if(sourceProfile==null||!actor.equals(sourceProfile.path("reconciliationOwnerAlias").asText())||!policy.path("version").asText().equals(sourceProfile.path("policyVersion").asText()))throw new IllegalArgumentException("Source owner/namespace/policy must agree");
        var seen=new HashSet<String>();
        for(var row:f.path("baseline").path("conditions")){
            String segment=Json.required(row,"segmentAlias");var physical=f.path("aliases").path(segment);
            if(!seen.add(segment+":"+Json.required(row,"conditionId")))throw new IllegalArgumentException("Duplicate given condition scope");
            String work=spec.path("workAliases").path(segment).asText();JsonNode workRow=null;for(var w:f.path("baseline").path("works"))if(work.equals(w.path("alias").asText()))workRow=w;
            if(workRow==null||!physical.path("itemAlias").asText().equals(workRow.path("itemAlias").asText())||!physical.path("lotAlias").asText().equals(workRow.path("lotAlias").asText())||!f.path("aliases").has(Json.required(row,"evidenceAlias")))throw new IllegalArgumentException("Given condition Work/evidence binding mismatch");
            if(!row.path("synthetic").asBoolean()||!"QuantitySegment".equals(physical.path("type").asText())||!spec.path("workAliases").hasNonNull(segment))throw new IllegalArgumentException("Exact synthetic segment and Work required");
            if(new BigDecimal(Json.required(row,"quantity")).signum()<=0||new BigDecimal(Json.required(row,"quantity")).compareTo(new BigDecimal(Json.required(physical,"quantity")))>0||!Json.required(row,"unit").equals(physical.path("unit").asText()))throw new IllegalArgumentException("Source range exceeds original physical quantity/unit");
            var from=Instant.parse(Json.required(row,"validFrom"));var until=Instant.parse(Json.required(row,"validUntil"));
            if(!until.isAfter(from)||from.isAfter(Instant.parse(f.path("clock").path("asOf").asText())))throw new IllegalArgumentException("Source interval is not a past/current given");
            if(!Set.of("QC","CUSTOMER_CONTRACT","DISPOSITION","REGULATORY","LABEL").contains(Json.required(row,"conditionId"))||!Set.of("ALLOWED","UNKNOWN","CONFLICT","DENIED").contains(Json.required(row,"status")))throw new IllegalArgumentException("Unsupported source condition");
        }
        for(var row:f.path("baseline").path("sourceProvenance")) {
            var physical=f.path("aliases").path(Json.required(row,"segmentAlias"));
            if(!"Supplier".equals(f.path("aliases").path(Json.required(row,"supplierAlias")).path("type").asText())||!physical.path("lotAlias").asText().equals(Json.required(row,"lotAlias"))||!f.path("aliases").has(Json.required(row,"evidenceAlias")))throw new IllegalArgumentException("Given supplier/LOT/segment provenance binding mismatch");
        }
    }
    T17SourceFacts(ActualConfiguration config,ObjectNode fixture,ObjectNode aliases,String external){
        validate(fixture);this.config=config;this.fixture=fixture;this.aliases=aliases;this.external=external;
        var spec=fixture.path("baseline").path("t17SourceFacts");actor=Json.required(spec,"actorAlias");namespace=Json.required(spec,"namespace");policyVersion=fixture.path("aliases").path(Json.required(spec,"policyAlias")).path("version").asText();signer=new JwtSigner(config);
    }
    ObjectNode install()throws Exception {
        resetClock();
        var procedures=new HashMap<String,JsonNode>();
        for(int index=0;index<fixture.path("baseline").path("conditions").size();index++){
            var row=fixture.path("baseline").path("conditions").get(index);String kind=Json.required(row,"conditionId"),segment=Json.required(row,"segmentAlias");
            if(!"ALLOWED".equals(row.path("status").asText())){uninstalled.add(Json.object().put("pointer","/baseline/conditions/"+index).put("reason","Original "+row.path("status").asText()+" is not a positive authority"));continue;}
            ObjectNode receipt;
            if(Set.of("QC","CUSTOMER_CONTRACT","DISPOSITION").contains(kind))receipt=quality(row,index);
            else {
                JsonNode proc=procedures.get(segment);
                if(proc==null){proc=procedure(segment,row);procedures.put(segment,proc);}
                receipt=regulatory(row,index,proc);
                if(kind.equals("REGULATORY"))procedures.put(segment,receipt.path("procedure"));
            }
            receipt.put("pointer","/baseline/conditions/"+index);receipt.set("given",row.deepCopy());conditions.add(receipt);
        }
        int index=0;
        for(var row:fixture.path("baseline").path("sourceProvenance")){
            String segment=Json.required(row,"segmentAlias");var physical=fixture.path("aliases").path(segment);
            var original=Json.object().put("supplierId",id(Json.required(row,"supplierAlias"))).put("lotId",id(Json.required(row,"lotAlias"))).put("segmentId",id(segment)).put("quantity",Json.required(physical,"quantity")).put("unit",Json.required(physical,"unit")).put("givenEvidenceAlias",Json.required(row,"evidenceAlias"));
            var source=publish(segment,"T17_SUPPLIER_PROVENANCE",original,fixture.path("clock").path("asOf").asText(),physical.path("quantity").asText(),physical.path("unit").asText());
            var receipt=receipt(source).put("pointer","/baseline/sourceProvenance/"+index++).put("typedSupplierRelationInstalled",false).put("supplierRelationshipVerified",false);receipt.set("given",row.deepCopy());provenance.add(receipt);
        }
        var out=Json.object().put("verificationPath","ACTUAL_HTTP_COMMAND_GATEWAY").put("seededVerificationRows",0).put("seededPermissionRows",0).put("legalComplianceClaimed",false);
        out.set("conditions",conditions);out.set("uninstalledConditions",uninstalled);out.set("sourceProvenance",provenance);out.set("commands",commands);
        out.put("provenanceLimitation","The original supplier assertion has a generic exact-segment canonical record, not verified supplier/LOT semantics; no published relationship command exists, so baseline.sourceProvenance stays omitted.");
        return out;
    }
    private String work(String segment){return id(fixture.path("baseline").path("t17SourceFacts").path("workAliases").path(segment).asText());}
    private ObjectNode quality(JsonNode row,int index)throws Exception {
        String segment=Json.required(row,"segmentAlias"),category=switch(Json.required(row,"conditionId")){case "QC"->"QC";case "CUSTOMER_CONTRACT"->"CUSTOMER";default->"COMMERCIAL";};
        var original=Json.object().put("segmentId",id(segment)).put("sourceDecisionId",identity("quality-"+index)).put("sourceVersion","1").put("workId",work(segment)).put("operation","recordDispositionBasis").put("action","SELL").put("category",category).put("startQuantity","0").put("quantity",Json.required(row,"quantity")).put("unit",Json.required(row,"unit")).put("validFrom",Json.required(row,"validFrom")).put("validUntil",Json.required(row,"validUntil")).put("givenEvidenceAlias",Json.required(row,"evidenceAlias"));
        Source source=publish(segment,"QUALITY_DECISION_recordDispositionBasis_"+category,original,row.path("validFrom").asText(),row.path("quantity").asText(),row.path("unit").asText());
        var slots=original.deepCopy();slots.remove(List.of("operation","sourceDecisionId","sourceVersion","givenEvidenceAlias"));slots.put("evidenceId",source.document()).put("reason","Synthetic original T17 given condition "+index).put("nextCheckAt",Instant.parse(fixture.path("clock").path("asOf").asText()).plusSeconds(3600).toString());
        var applied=invoke("recordDispositionBasis",slots,1);return receipt(source).put("typedDecisionId",Json.required(applied,"dispositionBasisId"));
    }
    private JsonNode procedure(String segment,JsonNode row)throws Exception {
        var a=fixture.path("aliases").path(segment);var original=Json.object().put("purpose","Synthetic T17 procedure source for declared given").put("segmentId",id(segment)).put("givenEvidenceAlias",Json.required(row,"evidenceAlias"));
        Source source=publish(segment,"T17_REGULATORY_PREPARATION",original,fixture.path("clock").path("asOf").asText(),a.path("quantity").asText(),a.path("unit").asText());
        return invoke("prepareRegulatoryProcedure",Json.object().put("itemId",id(Json.required(a,"itemAlias"))).put("lotId",id(Json.required(a,"lotAlias"))).put("physicalScopeId",id(segment)).put("workId",work(segment)).put("policyId",id(fixture.path("baseline").path("t17SourceFacts").path("policyAlias").asText())).put("documentVersionId",source.document()),null);
    }
    private ObjectNode regulatory(JsonNode row,int index,JsonNode prepared)throws Exception {
        String segment=Json.required(row,"segmentAlias");JsonNode proc=prepared;var a=fixture.path("aliases").path(segment);
        if(row.path("conditionId").asText().equals("REGULATORY")){
            var original=regOriginal(segment,proc).put("occurredAt",Json.required(row,"validFrom"));
            var source=publish(segment,"REGULATORY_SUBMISSION",original,row.path("validFrom").asText(),null,null);
            proc=invoke("recordSubmission",Json.object().put("procedureId",Json.required(proc,"procedureId")).put("occurredAt",row.path("validFrom").asText()).put("occurrenceId",source.canonical()),proc.path("revision").asInt());
        }
        boolean label=row.path("conditionId").asText().equals("LABEL");var original=regOriginal(segment,proc).put("decision",label?"VERIFIED":"ALLOWED").put("validFrom",Json.required(row,"validFrom")).put("validUntil",Json.required(row,"validUntil"));
        if(label){original.put("packagingVersionId",itemField(id(Json.required(a,"itemAlias")),"packagingVersionId")).put("specificationVersionId",itemField(id(Json.required(a,"itemAlias")),"specificationVersionId"));}
        else original.put("action","SELL").put("startQuantity","0").put("quantity",Json.required(row,"quantity")).put("unit",Json.required(row,"unit"));
        Source source=publish(segment,label?"LABEL_VERIFICATION":"REGULATORY_DECISION",original,row.path("validFrom").asText(),label?null:row.path("quantity").asText(),label?null:row.path("unit").asText());
        var slots=original.deepCopy();slots.remove(List.of("procedureVersionId","itemId","lotId","physicalScopeId","authority","policyVersion","regulatoryEventId"));slots.put("occurrenceId",source.canonical());
        var applied=invoke(label?"verifyLabel":"recordRegulatoryDecision",slots,proc.path("revision").asInt());
        var result=receipt(source).put("typedDecisionId",Json.required(applied,"versionId"));result.set("procedure",applied);return result;
    }
    private ObjectNode regOriginal(String segment,JsonNode proc){var a=fixture.path("aliases").path(segment);var policy=fixture.path("aliases").path(fixture.path("baseline").path("t17SourceFacts").path("policyAlias").asText());return Json.object().put("procedureId",Json.required(proc,"procedureId")).put("procedureVersionId",Json.required(proc,"procedureVersionId")).put("regulatoryEventId",UUID.randomUUID().toString()).put("itemId",id(Json.required(a,"itemAlias"))).put("lotId",id(Json.required(a,"lotAlias"))).put("physicalScopeId",id(segment)).put("authority",Json.required(policy,"authority")).put("policyVersion",policyVersion);}
    Source publish(String segment,String kind,ObjectNode original,String effective,String quantity,String unit)throws Exception {
        byte[] bytes=original.toString().getBytes(StandardCharsets.UTF_8);String eventKey=original.path("sourceDecisionId").asText(UUID.randomUUID().toString());
        var subject=Json.object().put("kind","SEGMENT").put("id",id(segment));
        var upload=Json.object().put("sourceNamespace",namespace).put("sourceReference",eventKey).put("mediaType","application/json").put("expectedHash",HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes))).put("provenance","SYNTHETIC_T17_GIVEN_SOURCE_NOT_REGULATORY_APPROVAL").put("contentBase64",Base64.getEncoder().encodeToString(bytes));upload.set("subject",subject);
        String doc=Json.required(invoke("attachEvidence",upload,null),"id");
        var observation=Json.object().put("kind",kind).put("sourceNamespace",namespace).put("externalEventId",eventKey).put("sourceVersion","1").put("effectiveFrom",effective).put("timeZone","UTC").put("timePrecision","SECOND").put("valueState","KNOWN").put("payload",original.toString()).put("documentId",doc).put("assertion","Synthetic given source; original scope must pass current server verification");observation.set("subject",subject);
        if(quantity!=null)observation.put("quantity",quantity).put("unit",unit);
        var event=invoke("recordActivity",observation,null);
        var review=Json.object().put("claimId",Json.required(event,"claimId")).put("basisDocumentId",doc).put("physicalScopeId",id(segment)).put("policyVersion",policyVersion).put("sourceIdentity",namespace+":"+eventKey+":1").put("effectiveFrom",effective).put("reason","Original T17 given source and exact physical scope reconciliation");
        if(quantity!=null)review.put("quantity",quantity).put("unit",unit);
        var matched=invoke("matchSourceIdentity",review,1);
        if(!"MATCHED".equals(matched.path("evidenceStatus").asText()))throw new IllegalStateException("T17 source did not match: "+matched);
        var linked=invoke("linkCanonicalOccurrence",Json.object().put("reconciliationId",Json.required(matched,"id")),1);
        return new Source(doc,Json.required(event,"id"),Json.required(matched,"id"),Json.required(linked,"id"));
    }
    private ObjectNode receipt(Source s){return Json.object().put("documentId",s.document()).put("eventId",s.event()).put("reviewId",s.review()).put("canonicalOccurrenceId",s.canonical());}
    private String itemField(String item,String field)throws Exception {if(!Set.of("packagingVersionId","specificationVersionId").contains(field))throw new IllegalArgumentException();try(var c=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password());var s=c.prepareStatement("SELECT "+field+" FROM mulino_inventory_TradeItems WHERE organizationId=? AND ID=?")){s.setString(1,id("ORG"));s.setString(2,item);try(var r=s.executeQuery()){if(!r.next())throw new IllegalStateException("Missing item version");return r.getString(1);}}}
    private String id(String alias){return Json.required(aliases,alias);}
    private String identity(String key){return UUID.nameUUIDFromBytes((fixture.path("fixtureId").asText()+":"+key).getBytes(StandardCharsets.UTF_8)).toString();}
    private void resetClock()throws Exception {
        var response=send("/__verification/runtime/clock/fixture-start",Json.object().put("instant",fixture.path("clock").path("asOf").asText()));if(response.statusCode()!=200)throw new IllegalStateException("Fixture clock rejected");
    }
    private HttpResponse<String> send(String path,ObjectNode body)throws Exception {
        var credential=(ObjectNode)fixture.path("actors").path(actor).deepCopy();credential.put("organizationAlias",external);
        return http.send(HttpRequest.newBuilder(config.baseUri().resolve(path)).timeout(Duration.ofSeconds(60)).header("Content-Type","application/json").header("Authorization","Bearer "+signer.sign(credential)).POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(),HttpResponse.BodyHandlers.ofString());
    }
    JsonNode request(String capability,ObjectNode slots,Integer revision)throws Exception {
        boolean record=Set.of("attachEvidence","recordActivity","recordDispositionBasis","recordSubmission","recordRegulatoryDecision","verifyLabel").contains(capability);
        var body=Json.object().put("intentKind",record?"RECORD":"COMMAND").put("definitionVersion",fixture.path("versions").path("definition").asText()).put("capabilityId",capability).put("commandIdempotencyKey",UUID.randomUUID().toString());body.set("subjectRefs",Json.array());body.set("slots",slots);body.set("provenance",Json.object().put("sourceNamespace","USER"));if(revision!=null)body.put("expectedRevision",revision);
        var response=send("/api/ontology/"+(record?"records/":"commands/")+capability,body);var result=Json.parse(response.body());var receipt=Json.object().put("httpStatus",response.statusCode());receipt.set("request",body);receipt.set("response",result);commands.add(receipt);
        var merged=(ObjectNode)result.deepCopy();if(result.path("effects").isObject())result.path("effects").fields().forEachRemaining(e->merged.set(e.getKey(),e.getValue()));return merged;
    }
    JsonNode commandEvidence(){return commands.deepCopy();}
    private JsonNode invoke(String capability,ObjectNode slots,Integer revision)throws Exception {var result=request(capability,slots,revision);if(!"APPLIED".equals(result.path("outcome").asText()))throw new IllegalStateException("T17 source command "+capability+" rejected: "+result);return result;}
}
