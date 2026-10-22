package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.mulino.verification.Json;
import java.net.http.*;
import java.time.*;
import java.sql.*;
import java.util.*;
import java.math.BigDecimal;

/** Explicit T01 historical sources. Canonical verification is performed by the
 * running server, never installed as a true flag or inferred from a document hash. */
final class T01SourceFacts {
    private final ActualConfiguration config;
    private final ObjectNode fixture, aliases;
    private final JsonNode facts;
    private final String external, org, actor;
    private final OffsetDateTime at;
    private final List<String> conventions;
    private final HttpClient http=HttpClient.newHttpClient();
    private final JwtSigner signer;
    private Connection connection;
    static boolean enabled(JsonNode f){return f.path("fixtureId").asText().equals("T01-CHANNEL-BASELINE")&&f.path("baseline").path("sourceFacts").path("version").asInt()==1;}
    static boolean consumes(JsonNode f,String key){return enabled(f)&&Set.of("sourceFacts","relations","restrictions","eligibilityBases").contains(key);}
    static void prepare(ObjectNode f,ObjectNode aliases){
        if(!enabled(f))return;
        aliases.put("t01Procedure",UUID.randomUUID().toString());aliases.put("t01ProcedureVersion",UUID.randomUUID().toString());
        aliases.put("t01RegPolicy",UUID.randomUUID().toString());
        for(var source:f.path("baseline").path("sourceFacts").path("sources")){
            String doc=Json.required(source,"documentAlias");var content=(ObjectNode)f.path("aliases").path(doc).path("fixtureContent");
            if(source.hasNonNull("category"))content.put("sourceDecisionId",aliases.path(doc).asText());
            if(source.path("kind").asText().startsWith("REGULATORY_"))content.put("procedureId",aliases.path("t01Procedure").asText()).put("procedureVersionId",aliases.path("t01ProcedureVersion").asText()).put("regulatoryEventId",aliases.path(doc).asText());
        }
    }
    static void documentSubject(JsonNode f,ObjectNode aliases,String alias,Map<String,Object> doc){
        if(!enabled(f))return;
        for(var s:f.path("baseline").path("sourceFacts").path("sources"))if(alias.equals(s.path("documentAlias").asText())){
            doc.put("subjectKind",Json.required(s,"subjectKind"));doc.put("subjectId",aliases.path(Json.required(s,"subjectAlias")).asText());
            doc.put("workId",aliases.path("O1").asText());doc.put("placeId",aliases.path("W").asText());
        }
    }
    T01SourceFacts(ActualConfiguration config,ObjectNode fixture,ObjectNode aliases,String external,List<String> conventions){
        this.config=config;this.fixture=fixture;this.aliases=aliases;this.external=external;this.facts=fixture.path("baseline").path("sourceFacts");
        this.org=id("ORG");this.actor=id(Json.required(facts,"actorAlias"));this.at=OffsetDateTime.parse(Json.required(fixture.path("clock"),"asOf"));this.conventions=conventions;this.signer=new JwtSigner(config);
    }
    void install() throws Exception {
        validate(fixture);
        if(!facts.path("legalComplianceClaimed").isBoolean()||facts.path("legalComplianceClaimed").asBoolean())throw new IllegalArgumentException("T01 must remain explicitly synthetic");
        transaction(()->{installRelations();installReceiptObservations();installRegulatoryProcedure();});
        for(JsonNode source:facts.path("sources"))publish(source);
        transaction(()->{installPermissions();installReceipts();});
        conventions.add("T01 sourceFacts: original documents and explicit source events were matched and linked by actual HTTP server commands; no VERIFIED row was seeded; receipt/permission history uses the returned canonical identities");
    }
    static void validate(JsonNode fixture){
        JsonNode f=fixture.path("baseline").path("sourceFacts");
        if(f.path("sources").isEmpty())throw new IllegalArgumentException("Explicit source graph required");
        var byDoc=new HashMap<String,JsonNode>();
        for(var source:f.path("sources")){
            String doc=Json.required(source,"documentAlias");if(byDoc.put(doc,source)!=null)throw new IllegalArgumentException("Duplicate source document");
            JsonNode original=fixture.path("aliases").path(doc).path("fixtureContent");
            if(!original.isObject())throw new IllegalArgumentException("Original source content required");
            if(source.hasNonNull("quantity")&&(!source.path("quantity").asText().equals(original.path("quantity").asText())||!source.path("unit").asText().equals(original.path("unit").asText())))throw new IllegalArgumentException("Source/original quantity differs");
        }
        for(var hold:fixture.path("baseline").path("restrictions")){
            JsonNode source=byDoc.get(hold.path("evidenceAlias").asText());
            if(source==null||!source.path("kind").asText().equals("QUALITY_DECISION_placeHold_"+hold.path("kind").asText())||!source.path("segmentAlias").equals(hold.path("segmentAlias"))||!source.path("quantity").equals(hold.path("quantity"))||!"ACTIVE".equals(hold.path("status").asText()))throw new IllegalArgumentException("Unsupported historical restriction");
        }
        for(var basis:fixture.path("baseline").path("eligibilityBases"))for(String key:List.of("qcEvidenceAlias","regulatoryEvidenceAlias","dispositionEvidenceAlias","customerEvidenceAlias")){
            JsonNode source=byDoc.get(basis.path(key).asText());
            if(source==null||!source.path("segmentAlias").equals(basis.path("segmentAlias"))||!source.path("quantity").equals(basis.path("quantity"))||!source.path("unit").equals(basis.path("unit"))||!"SELL".equals(basis.path("action").asText()))throw new IllegalArgumentException("Unsupported historical eligibility basis");
        }
    }
    private void transaction(Checked task)throws Exception{try(var c=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password())){connection=c;c.setAutoCommit(false);try{task.run();c.commit();}catch(Exception e){c.rollback();throw e;}}}
    @FunctionalInterface private interface Checked{void run()throws Exception;}
    private void installRelations()throws Exception{
        String definition=value("SELECT definitionVersionId FROM mulino_work_read_Works WHERE organizationId=? AND ID=?",org,id("O1"));
        for(String[] r:List.of(new String[]{"LOT_QUANTITY","ManufacturingLot","QuantitySegment","relationLotQuantity"},new String[]{"SUBJECT_OF","QuantitySegment","Work","relationSubjectOf"},new String[]{"CUSTOMER","Work","Customer","relationCustomer"})){
            insert("mulino_definitions_RelationDefinitions",base(id(r[3]),"definitionVersionId",definition,"name",r[0],"sourceType",r[1],"targetType",r[2],"minimumCount",0,"maximumCount",100,"cycleAllowed",false));
        }
        for(var rel:fixture.path("baseline").path("relations")){
            String type=Json.required(rel,"type"),source=Json.required(rel,"sourceAlias"),target=Json.required(rel,"targetAlias");
            String sourceType=type.equals("LOT_QUANTITY")?"ManufacturingLot":type.equals("SUBJECT_OF")?"QuantitySegment":"Work";
            String targetType=type.equals("LOT_QUANTITY")?"QuantitySegment":type.equals("SUBJECT_OF")?"Work":"Customer";
            String definitionId=id(switch(type){case "LOT_QUANTITY"->"relationLotQuantity";case "SUBJECT_OF"->"relationSubjectOf";case "CUSTOMER"->"relationCustomer";default->throw new IllegalArgumentException("Unknown source relation");});
            insert("mulino_inventory_ObjectRelations",base(uuid(),"definitionVersionId",definition,"relationDefinitionId",definitionId,"sourceType",sourceType,"sourceId",id(source),"targetType",targetType,"targetId",id(target),"validFrom",at));
        }
        for(var work:fixture.path("baseline").path("works"))insert("mulino_work_read_SubjectLinks",base(uuid(),"effectiveAt",at,"workId",id(Json.required(work,"alias")),"itemId",id(Json.required(work,"itemAlias")),"lotId",id(Json.required(work,"lotAlias"))));
        for(String doc:List.of("hold40"))insert("mulino_work_read_EvidenceReferences",base(uuid(),"effectiveAt",at,"workId",id("O1"),"documentVersionId",id(doc),"role","SOURCE"));
        String policy=value("SELECT policyId FROM mulino_governance_ActivePolicies WHERE organizationId=? AND kind='ELIGIBILITY'",org);aliases.put("eligibilityPolicy",policy);
    }
    private void installReceiptObservations()throws Exception{
        for(var s:facts.path("sources"))if(s.path("kind").asText().equals("PHYSICAL_RECEIPT")){
            String segment=id(Json.required(s,"segmentAlias"));
            insert("mulino_trade_receipt_Observations",base(uuid(),"occurredAt",at,"rangeRootId",segment,"startQuantity",BigDecimal.ZERO,"quantity",new BigDecimal(Json.required(s,"quantity")),"unit",Json.required(s,"unit"),"itemId",id("P"),"lotId",id("L"),"placeId",id("W"),"workId",id("O1"),"identificationStatus","CONFIRMED","state","CONFIRMED","ownerId",actor,"supervisorId",id("supervisor"),"nextAction","T01 receipt source review","nextCheckAt",at.plusHours(1)));
        }
    }
    private void installRegulatoryProcedure()throws Exception{
        JsonNode p=facts.path("regulatoryPolicy");if(!p.path("fictional").asBoolean())throw new IllegalArgumentException("Fictional regulatory policy required");
        insert("mulino_trade_regulatory_Policies",base(id("t01RegPolicy"),"recordedBy",actor,"version",Json.required(p,"version"),"authority",Json.required(p,"authority"),"sourceNamespace",Json.required(p,"sourceNamespace"),"action",Json.required(p,"action"),"validFrom",at,"validUntil",at.plusDays(24),"fictional",true,"status","ACTIVE","requiresLabel",p.path("requiresLabel").asBoolean()));
        insert("mulino_trade_regulatory_Procedures",base(id("t01Procedure"),"recordedBy",actor,"itemId",id("P"),"lotId",id("L"),"physicalScopeId",id("A60"),"workId",id("O1"),"policyId",id("t01RegPolicy")));
        insert("mulino_trade_regulatory_ProcedureVersions",base(id("t01ProcedureVersion"),"recordedBy",actor,"procedureId",id("t01Procedure"),"itemId",id("P"),"lotId",id("L"),"physicalScopeId",id("A60"),"workId",id("O1"),"policyId",id("t01RegPolicy"),"status","PREPARED","documentVersionId",id("regulatorySubmission"),"occurredAt",at));
        update("UPDATE mulino_trade_regulatory_Procedures SET currentVersionId=? WHERE organizationId=? AND ID=?",id("t01ProcedureVersion"),org,id("t01Procedure"));
    }
    private void publish(JsonNode s)throws Exception{
        String docAlias=Json.required(s,"documentAlias"),kind=Json.required(s,"kind");
        JsonNode payload;
        try(var c=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password());var st=c.prepareStatement("SELECT blobId FROM mulino_evidence_DocumentVersions WHERE ID=?")){
            st.setString(1,id(docAlias));try(var rows=st.executeQuery()){if(!rows.next())throw new IllegalStateException("Missing source original");var root=java.nio.file.Path.of(System.getProperty("verification.actual.blobRoot"));payload=Json.read(root.resolve("objects").resolve(rows.getString(1)));}
        }
        if(kind.equals("REGULATORY_DECISION")){
            ((ObjectNode)payload).put("procedureVersionId",id("t01SubmittedVersion"));
            byte[] bytes=payload.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            String hash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
            var upload=Json.object().put("sourceNamespace","synthetic-warehouse").put("sourceReference","T01-regulatory-current-version").put("mediaType","application/json").put("expectedHash",hash).put("provenance","SYNTHETIC_T01_EXACT_SUBMITTED_VERSION").put("contentBase64",Base64.getEncoder().encodeToString(bytes));
            upload.set("subject",Json.object().put("kind","SEGMENT").put("id",id("A60")));
            aliases.put(docAlias,Json.required(invoke("attachEvidence",upload,null),"id"));
        }
        var slots=Json.object();slots.set("subject",Json.object().put("kind",Json.required(s,"subjectKind")).put("id",id(Json.required(s,"subjectAlias"))));
        slots.put("kind",kind).put("sourceNamespace","synthetic-warehouse").put("externalEventId",kind.startsWith("QUALITY_")?payload.path("sourceDecisionId").asText():"T01-"+docAlias).put("sourceVersion","1").put("effectiveFrom",at.toInstant().toString()).put("timeZone","UTC").put("timePrecision","SECOND").put("valueState","KNOWN").put("payload",payload.toString()).put("documentId",id(docAlias)).put("assertion","Explicit synthetic T01 historical source");
        if(s.hasNonNull("quantity"))slots.put("quantity",Json.required(s,"quantity")).put("unit",Json.required(s,"unit"));
        JsonNode event=invoke("recordActivity",slots,null);
        var review=Json.object().put("claimId",Json.required(event,"claimId")).put("basisDocumentId",id(docAlias)).put("physicalScopeId",id(Json.required(s,"segmentAlias"))).put("policyVersion","fixture-v1").put("sourceIdentity","synthetic-warehouse:"+slots.path("externalEventId").asText()+":1").put("effectiveFrom",at.toInstant().toString()).put("reason","Verify explicit original and exact historical source range");
        if(s.hasNonNull("quantity"))review.put("quantity",Json.required(s,"quantity")).put("unit",Json.required(s,"unit"));
        JsonNode matched=invoke("matchSourceIdentity",review,1);if(!matched.path("evidenceStatus").asText().equals("MATCHED"))throw new IllegalStateException("T01 source did not match: "+docAlias+" "+matched);
        JsonNode linked=invoke("linkCanonicalOccurrence",Json.object().put("reconciliationId",Json.required(matched,"id")),1);
        String canonical=Json.required(linked,"id");aliases.put(docAlias+"Fact",canonical);
        if(kind.equals("REGULATORY_SUBMISSION")){var submitted=invoke("recordSubmission",Json.object().put("procedureId",id("t01Procedure")).put("occurredAt",at.toInstant().toString()).put("occurrenceId",canonical),1);aliases.put("t01SubmittedVersion",Json.required(submitted,"procedureVersionId"));}
    }
    private JsonNode invoke(String capability,ObjectNode slots,Integer revision)throws Exception{
        var body=Json.object().put("intentKind",Set.of("recordActivity","attachEvidence","recordSubmission","recordRegulatoryDecision").contains(capability)?"RECORD":"COMMAND").put("definitionVersion",fixture.path("versions").path("definition").asText()).put("capabilityId",capability).put("commandIdempotencyKey",uuid());
        body.set("subjectRefs",Json.array());body.set("slots",slots);body.set("provenance",Json.object().put("sourceNamespace","USER"));if(revision!=null)body.put("expectedRevision",revision);
        var credential=(ObjectNode)fixture.path("actors").path(Json.required(facts,"actorAlias")).deepCopy();credential.put("organizationAlias",external);
        String route=Set.of("recordActivity","attachEvidence","recordSubmission","recordRegulatoryDecision").contains(capability)?"records":"commands";
        var request=HttpRequest.newBuilder(config.baseUri().resolve("/api/ontology/"+route+"/"+capability)).timeout(Duration.ofSeconds(60)).header("Content-Type","application/json").header("Authorization","Bearer "+signer.sign(credential)).POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        var response=http.send(request,HttpResponse.BodyHandlers.ofString());JsonNode result=Json.parse(response.body());
        if(response.statusCode()!=200||!result.path("outcome").asText().equals("APPLIED"))throw new IllegalStateException("T01 source command "+capability+" rejected: HTTP "+response.statusCode()+" "+result);
        return result.has("effects")?result.path("effects"):result;
    }
    private void installPermissions()throws Exception{
        for(var s:facts.path("sources")){
            String doc=Json.required(s,"documentAlias");if(s.hasNonNull("category")){
                String seg=id(Json.required(s,"segmentAlias"));boolean hold=doc.equals("hold40");
                insert("mulino_inventory_"+(hold?"Restrictions":"DispositionBases"),base(uuid(),"controlScope",value("SELECT controlScope FROM mulino_inventory_QuantitySegments WHERE organizationId=? AND ID=?",org,seg),"action","SELL","state","ACTIVE","validFrom",at,"validUntil",at.plusDays(24),"evidenceRef",id(doc),"segmentId",seg,"startQuantity",BigDecimal.ZERO,"quantity",new BigDecimal(Json.required(s,"quantity")),"unit",Json.required(s,"unit"),"category",Json.required(s,"category"),"workId",id("O1"),"nextCheckAt",at.plusHours(1)));
            }
        }
        insert("mulino_trade_regulatory_DecisionVersions",base(uuid(),"recordedBy",actor,"procedureId",id("t01Procedure"),"procedureVersionId",id("t01SubmittedVersion"),"itemId",id("P"),"lotId",id("L"),"physicalScopeId",id("A60"),"policyId",id("t01RegPolicy"),"action","SELL","decision","ALLOWED","authority","SYNTHETIC_T01_AUTHORITY","startQuantity",BigDecimal.ZERO,"quantity",new BigDecimal("60"),"unit","BOX","validFrom",at,"validUntil",at.plusDays(24),"occurrenceId",id("regulatory60Fact")));
    }
    private void installReceipts()throws Exception{
        for(var s:facts.path("sources"))if(s.path("kind").asText().equals("PHYSICAL_RECEIPT")){
            String seg=id(Json.required(s,"segmentAlias")),canonical=id(Json.required(s,"documentAlias")+"Fact");BigDecimal q=new BigDecimal(Json.required(s,"quantity"));
            // Replace only the authored opening balance with its explicit receipt
            // provenance: this is the same physical stock, not a second movement.
            update("UPDATE mulino_inventory_QuantityMovements SET kind='RECEIPT',evidenceRef=? WHERE organizationId=? AND targetId=? AND kind='INITIAL_BALANCE'",canonical,org,seg);
            insert("mulino_trade_receipt_Receipts",base(uuid(),"occurredAt",at,"canonicalOccurrenceId",canonical,"rangeRootId",seg,"startQuantity",BigDecimal.ZERO,"quantity",q,"unit",Json.required(s,"unit"),"itemId",id("P"),"lotId",id("L"),"placeId",id("W"),"segmentId",seg,"workId",id("O1"),"contributedQuantity",q,"excessQuantity",BigDecimal.ZERO,"physicalEffect","CREATED"));
        }
    }
    private Map<String,Object> base(String id,Object... kv){var out=new LinkedHashMap<String,Object>();out.put("organizationId",org);out.put("ID",id);out.put("revision",1);out.put("createdAt",at);out.put("recordedAt",at);for(int i=0;i<kv.length;i+=2)out.put(kv[i].toString(),kv[i+1]);return out;}
    private String id(String alias){return Json.required(aliases,alias);}
    private String uuid(){return UUID.randomUUID().toString();}
    private String value(String sql,Object...args)throws Exception{try(var s=connection.prepareStatement(sql)){for(int i=0;i<args.length;i++)s.setObject(i+1,args[i]);try(var rows=s.executeQuery()){if(!rows.next())throw new IllegalStateException("Missing T01 source binding");return rows.getString(1);}}}
    private void update(String sql,Object...args)throws Exception{try(var s=connection.prepareStatement(sql)){for(int i=0;i<args.length;i++)s.setObject(i+1,args[i]);if(s.executeUpdate()!=1)throw new IllegalStateException("Expected one T01 historical row");}}
    private void insert(String table,Map<String,Object> values)throws Exception{try(var s=connection.prepareStatement("INSERT INTO "+table+"("+String.join(",",values.keySet())+") VALUES("+String.join(",",Collections.nCopies(values.size(),"?"))+")")){int i=1;for(var value:values.values())s.setObject(i++,value);if(s.executeUpdate()!=1)throw new IllegalStateException("Missing T01 row");}}
}
