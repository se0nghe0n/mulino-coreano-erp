package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mulino.verification.*;
import static org.junit.jupiter.api.Assertions.*;

/** Bounded real HTTP/PG policy experiment. Does not invoke a scenario runner. */
@EnabledIfEnvironmentVariable(named="T17_PG_TEST",matches="true")
class T17DeliveryAssessmentPolicyPostgresTest {
    static final String POLICY="t17-synthetic-v1", ALIAS="t17DeliveryAssessmentPolicy";
    final Path root=Path.of(System.getProperty("repo.root"));
    ActualConfiguration config;ObjectNode fixture,installed,aliases;Connection db;
    String name,sid,org;JsonNode sub;final Map<String,JsonNode> results=new LinkedHashMap<>();
    @BeforeEach void name(TestInfo info){name=info.getTestMethod().orElseThrow().getName();}
    @AfterEach void close()throws Exception{if(db!=null)db.close();}
    String id(String alias){return Json.required(aliases,alias);}
    static String hash(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    void save(String suffix,JsonNode node)throws Exception{Files.writeString(Path.of(System.getenv("T17_TEST_EVIDENCE")).resolve(name+"-"+suffix+".json"),node.toPrettyString());}
    void install(String selected,String variant)throws Exception {
        sid=selected;config=ActualConfiguration.environment(System.getenv());
        fixture=(ObjectNode)Json.read(root.resolve("verification/cases/T17/fixtures/"+sid+".json"));
        for(var s:Json.read(root.resolve("verification/cases/T17/case.json")).path("subcases"))if(sid.equals(s.path("id").asText()))sub=s.deepCopy();
        assertNotNull(sub);
        if(Set.of("baseline","goalOnly").contains(variant)){
            ((ObjectNode)fixture.path("aliases")).remove(ALIAS);
            ((ObjectNode)fixture.path("baseline").path("policy")).remove("deliveryAssessmentPolicyAuthoring");
            // This negative changes only the goal version, without referencing
            // an uninstalled fixture identity that would fail in the resolver.
            ((ObjectNode)action("complete-order-delivery-goal").at("/request/slots/goal/defaultContext")).remove(List.of("evidencePolicyVersionId","deliveryAssessmentPolicyAuthoring"));
        }
        if(variant.equals("baseline")){
            var goal=(ObjectNode)action("complete-order-delivery-goal").at("/request/slots/goal");
            goal.put("evidencePolicyVersion","fixture-v1");
            var context=(ObjectNode)goal.path("defaultContext");context.put("evidencePolicyVersion","fixture-v1");context.remove(List.of("evidencePolicyVersionId","deliveryAssessmentPolicyAuthoring"));
            ((ObjectNode)context.at("/fieldProvenance/evidencePolicyVersion")).put("source","installed immutable EVIDENCE policy");
        }
        var policy=(ObjectNode)fixture.path("aliases").get(ALIAS);
        if(variant.equals("inactive"))policy.put("activate",false);
        if(variant.equals("future"))policy.put("effectiveFrom","2026-10-07T09:01:00Z");
        if(variant.equals("expired")){policy.put("effectiveFrom","2026-10-06T09:00:00Z");policy.put("effectiveUntil","2026-10-07T09:00:00Z");}
        var bundle=Json.object().put("fixtureHash",hash(fixture.toString().getBytes(StandardCharsets.UTF_8)));bundle.set("fixture",fixture);
        bundle=(ObjectNode)ActualFixtureBindings.bind(root,bundle,config);fixture=(ObjectNode)bundle.path("fixture");
        installed=ScenarioFixtureInstaller.install(config,root,bundle,true);aliases=(ObjectNode)installed.path("aliasMap");org=id("ORG");
        db=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password());save("setup",installed);
    }
    JsonNode action(String actionId){for(var a:sub.path("actions"))if(actionId.equals(a.path("id").asText()))return a;throw new AssertionError(actionId);}
    ObjectNode request(String actionId){return (ObjectNode)new ReferenceResolver(results,aliases).resolve(action(actionId).path("request"));}
    JsonNode http(String actor,String endpoint,JsonNode body)throws Exception {
        var credential=(ObjectNode)fixture.path("actors").path(actor).deepCopy();credential.put("organizationAlias",installed.path("organizationExternalAlias").asText());
        var response=HttpClient.newHttpClient().send(HttpRequest.newBuilder(config.baseUri().resolve("/api/ontology/"+endpoint)).timeout(Duration.ofSeconds(60)).header("Content-Type","application/json").header("Authorization","Bearer "+new JwtSigner(config).sign(credential)).POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,response.statusCode(),response.body());return Json.parse(response.body());
    }
    void run(String actionId)throws Exception {
        var a=action(actionId);var req=request(actionId);String kind=req.path("intentKind").asText();
        var response=http(a.path("actorRef").asText(),(a.path("kind").asText().equals("query")?"queries/":kind.equals("RECORD")?"records/":"commands/")+a.path("capabilityId").asText(),req);
        var result=Json.object().put("driverStatus","EXECUTED");result.set("response",response);result.set("provenance",Json.object().put("scopeComplete",true));results.put(actionId,result);save(actionId,result);
        if(!a.path("kind").asText().equals("query"))assertEquals(actionId.equals("delivery-provisional")?"ACCEPTED_PENDING_EXTERNAL":"APPLIED",response.path("outcome").asText(),response.toPrettyString());
    }
    void ready(boolean includeHold)throws Exception {
        for(var a:sub.path("actions")){
            String actionId=a.path("id").asText();if(actionId.equals("delivery"))break;
            // This counterfactual excludes the restriction and its formal recall branch.
            // Both require the skipped late-hold source chain; delivery inputs still execute.
            if(!includeHold&&(actionId.startsWith("late-hold")||Set.of("open-recall-investigation","formal-recall-work","formal-recall-basis","formal-recall-investigation").contains(actionId)))continue;
            // Only the existing case's HTTP commands and their required reads.
            if(a.path("kind").asText().equals("invoke")||a.path("kind").asText().equals("query"))run(actionId);
        }
    }
    ArrayNode rows(String sql,Object...args)throws Exception {
        var out=Json.array();try(var s=db.prepareStatement(sql)){for(int i=0;i<args.length;i++)s.setObject(i+1,args[i]);try(var r=s.executeQuery()){
            var meta=r.getMetaData();while(r.next()){var row=out.addObject();for(int col=1;col<=meta.getColumnCount();col++){
                String key=meta.getColumnLabel(col).toLowerCase(Locale.ROOT);Object value=r.getObject(col);
                if(value==null)row.putNull(key);else if(value instanceof Boolean b)row.put(key,b);else if(value instanceof Timestamp t)row.put(key,t.toInstant().toString());else row.put(key,value.toString());
            }}
        }}return out;
    }
    String work(){return results.get("order-delivery-work").at("/response/data/ID").asText();}
    JsonNode proof()throws Exception {
        var proof=Json.object();
        for(String table:List.of("mulino_evidence_SourceProfiles","mulino_evidence_Verifications","mulino_evidence_CanonicalOccurrences","mulino_evidence_Claims","mulino_evidence_Events","mulino_evidence_InboxRecords","mulino_evidence_DocumentVersions","mulino_work_read_GoalReferences","mulino_work_read_AssessmentReferences","mulino_evaluation_InputSnapshots","mulino_governance_PolicyVersions","mulino_governance_ActivePolicies","mulino_inventory_QuantitySegments","mulino_trade_sales_Deliveries"))
            proof.set(table,rows("SELECT * FROM "+table+" WHERE organizationId=? ORDER BY "+(table.endsWith("ActivePolicies")?"kind":"ID"),org));
        save("proof",proof);return proof;
    }
    JsonNode assessment()throws Exception {
        var request=Json.object().put("id",work()).put("asOf",fixture.at("/clock/asOf").asText()).put("knownAt",fixture.at("/clock/knownAt").asText());request.set("scope",Json.object().put("organizationId",org).put("workId",work()));
        var read=http("observer","queries/getAssessment",request);save("getAssessment",read);return read.path("data");
    }
    JsonNode snapshot(JsonNode assessment)throws Exception {
        var all=rows("SELECT * FROM mulino_evaluation_InputSnapshots WHERE organizationId=? AND assessmentId=?",org,assessment.path("ID").asText());assertEquals(1,all.size());
        var snap=all.get(0);assertEquals(snap.path("contenthash").asText(),hash(snap.path("contentjson").asText().getBytes(StandardCharsets.UTF_8)));
        assertEquals(assessment.path("inputSnapshotHash").asText(),snap.path("contenthash").asText());return Json.parse(snap.path("contentjson").asText());
    }
    void physical(String quantity,String legitimate)throws Exception {
        var effect=results.get("delivery").at("/response/effects");assertEquals(quantity,effect.path("physicalQuantity").asText());assertEquals(legitimate,effect.path("contributedQuantity").asText());
        var deliveries=rows("SELECT quantity,legitimateQuantity FROM mulino_trade_sales_Deliveries WHERE organizationId=?",org);assertEquals(1,deliveries.size());
        assertEquals(0,new java.math.BigDecimal(quantity).compareTo(new java.math.BigDecimal(deliveries.get(0).path("quantity").asText())));
        assertEquals(0,new java.math.BigDecimal(legitimate).compareTo(new java.math.BigDecimal(deliveries.get(0).path("legitimatequantity").asText())));
        var transit=rows("SELECT coalesce(sum(quantity),0) AS quantity FROM mulino_inventory_QuantitySegments WHERE organizationId=? AND retiredAt IS NULL AND placeId=?",org,id("TRANSIT"));
        assertEquals(0,new java.math.BigDecimal("30").subtract(new java.math.BigDecimal(quantity)).compareTo(new java.math.BigDecimal(transit.get(0).path("quantity").asText())));
    }
    void assertPolicyAndVerification(JsonNode assessment,JsonNode snap,String recognized)throws Exception {
        var selected=rows("SELECT p.ID,p.kind,p.version,p.content,p.contentHash,p.effectiveFrom,p.createdAt FROM mulino_governance_PolicyVersions p JOIN mulino_governance_ActivePolicies a ON a.organizationId=p.organizationId AND a.policyId=p.ID WHERE p.organizationId=? AND p.kind='EVIDENCE' AND p.version=? AND p.effectiveFrom<=? AND (p.effectiveUntil IS NULL OR p.effectiveUntil>?)",org,POLICY,Timestamp.from(java.time.Instant.parse(fixture.at("/clock/asOf").asText())),Timestamp.from(java.time.Instant.parse(fixture.at("/clock/asOf").asText())));
        assertEquals(1,selected.size());var p=selected.get(0);assertEquals(id(ALIAS),p.path("id").asText());assertEquals("{}",p.path("content").asText());assertEquals(hash("{}".getBytes(StandardCharsets.UTF_8)),p.path("contenthash").asText());assertEquals(id(ALIAS),assessment.path("policyVersionId").asText());
        assertEquals(fixture.at("/clock/asOf").asText(),p.path("effectivefrom").asText());
        assertEquals(POLICY,snap.at("/goalSlots/evidencePolicyVersion").asText());assertEquals("RECORD_DELIVERY",snap.at("/captureBoundary/admissionPurpose").asText());assertEquals(id(ALIAS),snap.at("/captureBoundary/policyId").asText());
        var verified=rows("SELECT v.*,p.policyVersion AS sourcePolicyVersion,p.ID AS sourceProfileId FROM mulino_evidence_Verifications v JOIN mulino_evidence_Claims c ON c.organizationId=v.organizationId AND c.ID=v.claimId JOIN mulino_evidence_Events e ON e.organizationId=c.organizationId AND e.ID=c.eventId JOIN mulino_evidence_SourceProfiles p ON p.organizationId=e.organizationId AND p.namespace=e.sourceNamespace WHERE v.organizationId=? AND v.canonicalOccurrenceId=?",org,results.get("delivery-source-canonical").at("/response/id").asText());
        assertEquals(1,verified.size());var v=verified.get(0);assertEquals(POLICY,v.path("policyversion").asText());assertEquals(POLICY,v.path("sourcepolicyversion").asText());assertNotEquals(p.path("id"),v.path("sourceprofileid"));assertEquals("VERIFIED",v.path("verdict").asText());for(String flag:List.of("sourcematched","identitymatched","quantitymatched","timematched","duplicatechecked"))assertTrue(v.path(flag).asBoolean());
        var doc=rows("SELECT * FROM mulino_evidence_DocumentVersions WHERE organizationId=? AND ID=?",org,v.path("basisdocumentid").asText()).get(0);
        var original=Files.readAllBytes(Path.of(System.getProperty("verification.actual.blobRoot"),"objects",doc.path("blobid").asText()));assertEquals(doc.path("sha256").asText(),hash(original));
        var event=rows("SELECT e.* FROM mulino_evidence_Events e JOIN mulino_evidence_Claims c ON c.organizationId=e.organizationId AND c.eventId=e.ID WHERE c.organizationId=? AND c.ID=?",org,v.path("claimid").asText()).get(0);assertEquals(Json.parse(new String(original,StandardCharsets.UTF_8)),Json.parse(event.path("payload").asText()));
        boolean found=false;for(var condition:snap.path("conditions"))for(var input:condition.path("inputs")){
            found=true;assertTrue(input.path("verified").asBoolean());assertEquals("KNOWN",input.path("state").asText());assertEquals(recognized,input.path("value").asText());assertEquals("BOX",input.path("unit").asText());
            var refs=new HashSet<String>();for(var ref:input.path("evidenceRefs"))refs.add(ref.asText());assertTrue(refs.contains(v.path("id").asText()));assertTrue(refs.contains(v.path("basisdocumentid").asText()));assertTrue(refs.contains(v.path("claimid").asText()));assertTrue(refs.contains(event.path("id").asText()));
        }assertTrue(found);
        assertFalse(fixture.path("actors").path("receiver").path("grant").path("actions").toString().contains("assessGoal"));
    }
    @Test void originalPolicyMismatchIsProvenByActualRows()throws Exception {
        install("late-restriction-actual-delivery","baseline");ready(true);run("delivery");physical("20","0");var a=assessment();assertEquals("UNVERIFIED",a.path("outcome").asText());assertFalse(a.path("held").asBoolean());var snap=snapshot(a);
        assertEquals("fixture-v1",snap.at("/goalSlots/evidencePolicyVersion").asText());assertEquals("fixture-v1",snap.at("/policy/version").asText());
        var v=rows("SELECT policyVersion,verdict FROM mulino_evidence_Verifications WHERE organizationId=? AND canonicalOccurrenceId=?",org,results.get("delivery-source-canonical").at("/response/id").asText());assertEquals(1,v.size());assertEquals(POLICY,v.get(0).path("policyversion").asText());assertEquals("VERIFIED",v.get(0).path("verdict").asText());
        for(var c:snap.path("conditions"))for(var input:c.path("inputs")){assertEquals("0",input.path("value").asText());assertFalse(input.path("verified").asBoolean());assertEquals(3,input.path("evidenceRefs").size());}proof();
    }
    @Test void changingGoalAloneKeepsEvidencePolicyUnresolved()throws Exception {
        install("late-restriction-actual-delivery","goalOnly");ready(true);run("delivery");physical("20","0");var a=assessment();assertEquals("UNVERIFIED",a.path("outcome").asText());assertTrue(a.path("held").asBoolean());assertEquals("EVIDENCE_POLICY_UNRESOLVED",snapshot(a).path("unknown").asText());proof();
    }
    @Test void matchingCurrentPolicyVerifiesRestrictedZeroWithoutAssessmentGrant()throws Exception {
        install("late-restriction-actual-delivery","corrected");ready(true);run("delivery");physical("20","0");var a=assessment();assertEquals("UNSATISFIED",a.path("outcome").asText());assertFalse(a.path("held").asBoolean());assertPolicyAndVerification(a,snapshot(a),"0");
        run("after-delivery");assertEquals("UNSATISFIED",results.get("after-delivery").at("/response/data/deliveryAssessment").asText());
        var before=rows("SELECT * FROM mulino_work_read_AssessmentReferences WHERE organizationId=? ORDER BY ID",org);
        var current=http("observer","queries/getWork",Json.object().put("id",work()));
        var unauthorized=Json.object().put("intentKind","COMMAND").put("definitionVersion","definition-v1").put("capabilityId","assessGoal").put("commandIdempotencyKey",UUID.randomUUID().toString()).put("expectedRevision",current.at("/data/revision").asInt());
        unauthorized.set("subjectRefs",Json.array());unauthorized.set("slots",Json.object().put("workId",work()));unauthorized.set("provenance",Json.object().put("workId","CONTEXT"));
        var denied=http("receiver","commands/assessGoal",unauthorized);save("unauthorized-assessGoal",denied);assertEquals("FORBIDDEN",denied.at("/error/code").asText());
        assertEquals(before,rows("SELECT * FROM mulino_work_read_AssessmentReferences WHERE organizationId=? ORDER BY ID",org));
        var again=assessment();assertEquals(a.path("ID"),again.path("ID"));assertEquals(a.path("inputSnapshotHash"),again.path("inputSnapshotHash"));proof();
    }
    @Test void normalPartialDeliveryTwentyAgainstThirtyIsUnsatisfied()throws Exception {
        install("late-restriction-actual-delivery","corrected");ready(false);run("delivery");physical("20","20");var a=assessment();assertEquals("UNSATISFIED",a.path("outcome").asText());assertPolicyAndVerification(a,snapshot(a),"20");proof();
    }
    @Test void normalFullDeliveryRemainsSatisfied()throws Exception {
        install("normal-consumed-delivery","corrected");ready(false);run("delivery");physical("30","30");var a=assessment();assertEquals("SATISFIED",a.path("outcome").asText());assertPolicyAndVerification(a,snapshot(a),"30");proof();
    }
    @ParameterizedTest @ValueSource(strings={"inactive","future","expired"})
    void unavailableCurrentPolicyNeverBecomesVerified(String variant)throws Exception {
        name+="-"+variant;install("late-restriction-actual-delivery",variant);ready(true);run("delivery");physical("20","0");var a=assessment();assertTrue(a.path("held").asBoolean());assertEquals("UNVERIFIED",a.path("outcome").asText());assertEquals("EVIDENCE_POLICY_UNRESOLVED",snapshot(a).path("unknown").asText());proof();
    }
    @Test void sourceIdentityPolicyCannotBeReplacedByOldGoalPolicy()throws Exception {
        install("normal-consumed-delivery","corrected");
        for(var a:sub.path("actions")){if(a.path("id").asText().equals("delivery-source-match"))break;if(Set.of("invoke","query").contains(a.path("kind").asText()))run(a.path("id").asText());}
        var req=request("delivery-source-match");req.put("commandIdempotencyKey",UUID.randomUUID().toString());((ObjectNode)req.path("slots")).put("policyVersion","fixture-v1");
        var denied=http("t17Source","commands/matchSourceIdentity",req);save("wrong-source-policy",denied);assertEquals("POLICY_UNRESOLVED",denied.at("/error/code").asText());assertEquals("REJECTED",denied.path("outcome").asText());
        assertEquals(0,rows("SELECT ID FROM mulino_trade_sales_Deliveries WHERE organizationId=?",org).size());
        run("delivery-source-match");run("delivery-source-canonical");run("delivery");physical("30","30");assertEquals("SATISFIED",assessment().path("outcome").asText());proof();
    }
}
