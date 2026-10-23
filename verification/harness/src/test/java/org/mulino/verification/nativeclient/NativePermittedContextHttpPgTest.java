package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mulino.verification.*;
import org.mulino.verification.actual.*;
import org.mulino.verification.modelbinding.CapturedApiObservation;
import java.nio.file.*;
import java.net.http.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Direct disposable HTTP/PostgreSQL probe, not a scenario, Workflow, model call or coverage receipt.
 * Actual context evidence requires the separately integrated server capture hook. No model is invoked. */
@EnabledIfEnvironmentVariable(named="ACTUAL_PERMITTED_CONTEXT_HTTP_PG",matches="true")
class NativePermittedContextHttpPgTest {
    final Path root=Path.of(System.getProperty("repo.root")).toAbsolutePath().normalize();
    ActualConfiguration config;NativeIntegrationEvidence store;NativeMcpTransport transport;
    ObjectNode aliases,actor,scope,contextRequest;final ArrayNode checks=Json.array();
    String work,obligation,document,foreignProduct,readGrant,sourceGrant;final Map<String,String> discovered=new LinkedHashMap<>();final Instant fact=Instant.parse("2026-10-07T09:00:00Z");
    Instant authority;
    static final List<String> READS=List.of("searchObjects","searchWorks","getObject","getWork","getInventory","getObligations","getEvidence","getAssessment");
    @Test void directCurrentProductContextAndAuthorityCounterexamples() throws Exception {
        config=ActualConfiguration.environment(System.getenv());store=new NativeIntegrationEvidence(root);transport=new NativeMcpTransport(config,store);
        authority=Instant.parse(System.getenv("MULINO_VERIFICATION_INSTANT"));
        assertEquals("true",System.getenv("ACTUAL_DISPOSABLE_DATABASE"));
        var input=fixture("context-"+UUID.randomUUID());
        var bundle=Json.object();bundle.set("fixture",input);bundle.put("fixtureHash",NativeClientPolicy.sha(input.toString())).put("organizationExternalAlias",input.path("probeExternalAlias").asText());bundle.set("bases",Json.array());
        var installation=new FixtureInstaller(config).install(bundle);aliases=(ObjectNode)installation.path("aliasMap");actor=(ObjectNode)input.path("actors").path("reader");
        scope=Json.object().put("organizationId",id("ORG")).put("installationId",UUID.randomUUID().toString()).put("caseId","direct-context-probe").put("turnId","turn-1");
        contextRequest=Json.object().put("view",NativePermittedContext.VIEW).put("asOf",fact.toString()).put("knownAt",fact.plusSeconds(1).toString());contextRequest.set("scope",scope);
        seedBusiness();
        JsonNode clockReceipt=fixtureStartClock();
        readGrant=sqlScalar("SELECT ID FROM mulino_identity_Grants WHERE organizationId=? AND actorId=?",id("ORG"),id("reader"));
        sourceGrant=UUID.randomUUID().toString();
        execute("INSERT INTO mulino_identity_Grants(organizationId,ID,actorId,delegatorId,validFrom,validUntil,revision,createdAt,recordedAt) SELECT organizationId,?,actorId,delegatorId,validFrom,validUntil,revision,createdAt,recordedAt FROM mulino_identity_Grants WHERE organizationId=? AND ID=?",sourceGrant,id("ORG"),readGrant);
        execute("INSERT INTO mulino_identity_GrantActions(organizationId,grantId,capabilityId) SELECT organizationId,?,capabilityId FROM mulino_identity_GrantActions WHERE organizationId=? AND grantId=?",sourceGrant,id("ORG"),readGrant);
        execute("INSERT INTO mulino_identity_GrantScopes(organizationId,grantId,scopeKind,scopeId) VALUES(?,?,?,?)",id("ORG"),sourceGrant,"ORGANIZATION",id("ORG"));
        execute("INSERT INTO mulino_identity_GrantScopes(organizationId,grantId,scopeKind,scopeId) VALUES(?,?,?,?)",id("ORG"),sourceGrant,"SOURCE","carrier-context-probe");
        execute("INSERT INTO mulino_identity_GrantScopes(organizationId,grantId,scopeKind,scopeId) VALUES(?,?,?,?)",id("ORG"),sourceGrant,"TARGET",id("W"));
        String originalSourceGrant=sourceFingerprint();
        var other=fixture("foreign-"+UUID.randomUUID());var otherBundle=bundle.deepCopy();otherBundle.set("fixture",other);otherBundle.put("fixtureHash",NativeClientPolicy.sha(other.toString())).put("organizationExternalAlias",other.path("probeExternalAlias").asText());
        foreignProduct=new FixtureInstaller(config).install(otherBundle).path("aliasMap").path("PRD").asText();
        String before=businessFingerprint();
        var positive=collect("positive-human",actor);
        assertTrue(positive.context().toString().contains(id("W")));assertTrue(positive.context().toString().contains(id("W2")));
        assertTrue(positive.context().toString().contains("동명 창고"));assertTrue(positive.context().toString().contains(work));assertTrue(positive.context().toString().contains(obligation));assertTrue(positive.context().toString().contains(document));
        assertFalse(positive.context().toString().contains(foreignProduct));
        assertEquals("48",sqlScalar("SELECT sum(quantity)::text FROM mulino_inventory_QuantitySegments WHERE organizationId=?",id("ORG")).replaceAll("\\.0+$",""));
        assertTrue(positive.context().path("reads").findValuesAsText("heldQuantity").contains("48"));
        assertEquals(before,businessFingerprint());check("positive-human-current-work-evidence-obligation-48BOX-and-duplicate-name",true);
        for(var noun:discovered.entrySet()) {
            boolean found=false;for(JsonNode read:positive.context().path("reads"))if(read.path("request").path("scope").path("objectType").asText().equals(noun.getKey()))
                for(JsonNode row:read.path("response").path("data"))found|=row.path("ID").asText().equals(noun.getValue());
            assertTrue(found,"Actual paged discovery missing "+noun.getKey());
        }
        assertTrue(positive.context().path("discoveryGaps").isEmpty());check("four-business-nouns-discovered-over-actual-authenticated-HTTP-PG",true);
        var result=NativePermittedContextCapture.result("positive-human",actor,scope,positive,store);
        assertEquals(StepResult.DriverStatus.EXECUTED,result.driverStatus());assertEquals("ACTUAL_AUTHENTICATED_TRANSPORT",result.provenance().path("source").asText());
        assertEquals(id("reader"),result.provenance().path("authenticatedActor").path("actorId").asText());
        assertEquals(id("ORG"),result.provenance().path("authenticatedActor").path("organizationId").asText());
        assertEquals(readGrant,result.provenance().path("authenticatedActor").path("grantId").asText());
        var actualSource=positive.captures().getFirst().artifact();
        var actualReceipt=Json.read(root.resolve(actualSource.path("path").asText()));
        assertTrue(actualReceipt.path("serverAuthenticatedCapture").path("grants").toString().contains("carrier-context-probe"));
        assertEquals("NOT_ASSERTED",actualReceipt.path("serverAuthenticatedCapture").path("actionAuthorization").asText());
        validateActualSourceMutants(actualReceipt);
        check("actual-non-UUID-typed-SOURCE-namespace-preserved-without-authority-widening",true);
        check("source-dimension-inapplicable-purpose-read-grant-is-actual-candidate",true);
        var wire=Json.read(root.resolve(result.data().path("projectionReadRef").asText()));
        new ContractValidator(root).schema("verification/model-binding/captured-api.schema.json",wire);
        CapturedApiObservation.checkCaptureContext(wire,scope,Json.MAPPER.valueToTree("positive-human"),null);
        assertEquals(result.data().path("snapshotRevision"),CapturedApiObservation.decode(wire.path("toolCall"),wire).response().path("snapshotRevision"));
        check("actual-server-identity-grant-capture-and-exact-projection-wire",true);
        var projectionRequest=Json.object().put("operation","getObject");var projectionArgs=positive.projection().request().path("params").path("arguments");
        projectionRequest.set("id",projectionArgs.path("id"));projectionRequest.set("scope",projectionArgs.path("scope"));
        var physical=new NativePhysicalObserver(config,store).capture(scope,contextRequest.path("asOf").asText(),contextRequest.path("knownAt").asText(),projectionRequest,id("reader"));
        assertEquals(result.data().path("snapshotRevision"),physical.path("projectionRevisionProof").path("revision"));
        check("independent-postgresql-anchor-projection-revision-exact",true);

        String ambiguousGrant=UUID.randomUUID().toString();
        execute("INSERT INTO mulino_identity_Grants(organizationId,ID,actorId,delegatorId,validFrom,validUntil,revision,createdAt,recordedAt) SELECT organizationId,?,actorId,delegatorId,validFrom,validUntil,revision,createdAt,recordedAt FROM mulino_identity_Grants WHERE organizationId=? AND ID=?",ambiguousGrant,id("ORG"),readGrant);
        execute("INSERT INTO mulino_identity_GrantActions(organizationId,grantId,capabilityId) VALUES(?,?,?)",id("ORG"),ambiguousGrant,"getObject");
        execute("INSERT INTO mulino_identity_GrantScopes(organizationId,grantId,scopeKind,scopeId) VALUES(?,?,?,?)",id("ORG"),ambiguousGrant,"TARGET",id("W"));
        var ambiguousArgs=Json.object().put("id",id("W")).put("asOf",fact.toString()).put("knownAt",fact.plusSeconds(1).toString());ambiguousArgs.set("scope",Json.object().put("organizationId",id("ORG")).put("objectType","Place"));
        var ambiguousCapture=transport.send("genuinely-two-applicable","tools/call","getObject",ambiguousArgs,actor,scope);
        assertEquals("PERMITTED_CONTEXT_GRANT_ATTRIBUTION_UNAVAILABLE",assertThrows(IllegalStateException.class,()->NativePermittedContextCapture.ordinary("genuinely-two-applicable",actor,scope,ambiguousCapture,store)).getMessage());
        execute("UPDATE mulino_identity_Grants SET revokedAt=? WHERE organizationId=? AND ID=?",OffsetDateTime.ofInstant(authority.minusSeconds(1),ZoneOffset.UTC),id("ORG"),ambiguousGrant);check("two-truly-applicable-read-grants-remain-fail-closed",true);

        var anchor=positive.projection();var anchorRequest=anchor.request().path("params").path("arguments");
        var same=transport.send("same-snapshot","tools/call","getObject",anchorRequest,actor,scope);
        assertEquals(anchor.businessResponse().path("snapshotRevision"),same.businessResponse().path("snapshotRevision"));
        execute("UPDATE mulino_inventory_Places SET name=?,revision=revision+1 WHERE organizationId=? AND ID=?","서버에서 변경한 이름",id("ORG"),anchorRequest.path("id").asText());
        var stale=anchorRequest.deepCopy();((ObjectNode)stale).set("snapshotRef",anchor.businessResponse().path("snapshotRevision"));
        var changed=transport.send("stale-snapshot","tools/call","getObject",stale,actor,scope);assertEquals("SNAPSHOT_CHANGED",changed.businessResponse().path("error").path("code").asText());check("single-read-revision-stable-and-stale-snapshot-conflict",true);

        var foreign=anchorRequest.deepCopy();((ObjectNode)foreign).put("id",foreignProduct);((ObjectNode)foreign.path("scope")).put("objectType","Product");
        var foreignResult=transport.send("foreign-id","tools/call","getObject",foreign,actor,scope);assertEquals("FORBIDDEN",foreignResult.businessResponse().path("error").path("code").asText());
        ((ObjectNode)foreign.path("scope")).put("organizationId",UUID.randomUUID().toString());
        assertEquals("FORBIDDEN",transport.send("foreign-scope","tools/call","getObject",foreign,actor,scope).businessResponse().path("error").path("code").asText());check("foreign-organization-and-id-hidden",true);
        var spoof=anchorRequest.deepCopy();((ObjectNode)spoof).putObject("actor").put("role","ADMIN");
        assertEquals("TYPE_INVALID",transport.send("spoof-payload","tools/call","getObject",spoof,actor,scope).businessResponse().path("error").path("code").asText());check("payload-actor-spoof-rejected",true);

        execute("DELETE FROM mulino_identity_GrantScopes WHERE organizationId=? AND grantId=?",id("ORG"),readGrant);
        execute("INSERT INTO mulino_identity_GrantScopes(organizationId,grantId,scopeKind,scopeId) VALUES(?,?,?,?)",id("ORG"),readGrant,"ITEM",id("P"));
        execute("INSERT INTO mulino_identity_GrantScopes(organizationId,grantId,scopeKind,scopeId) VALUES(?,?,?,?)",id("ORG"),readGrant,"PLACE",id("W"));
        var restricted=collect("all-of-human",actor);assertTrue(restricted.context().toString().contains(id("A60")));assertFalse(restricted.context().toString().contains(id("B40")));assertFalse(restricted.context().toString().contains(work));
        assertTrue(restricted.context().path("reads").findValuesAsText("heldQuantity").contains("37"));check("all-of-item-place-intersection-human-37BOX-hidden-11BOX",true);
        var restrictedResult=NativePermittedContextCapture.result("all-of-human",actor,scope,restricted,store);
        assertEquals(StepResult.DriverStatus.EXECUTED,restrictedResult.driverStatus());assertFalse(restrictedResult.data().path("permittedContext").toString().contains(id("B40")));
        var target=Json.object().put("id",id("B40")).put("asOf",fact.toString()).put("knownAt",fact.plusSeconds(1).toString());target.set("scope",Json.object().put("organizationId",id("ORG")).put("objectType","QuantitySegment"));
        var hidden=transport.send("hidden-target","tools/call","getObject",target,actor,scope);
        target.put("id",UUID.randomUUID().toString());var absent=transport.send("absent-target","tools/call","getObject",target,actor,scope);
        assertEquals(hidden.businessResponse(),absent.businessResponse());assertEquals("FORBIDDEN",hidden.businessResponse().path("error").path("code").asText());check("hidden-and-unknown-target-indistinguishable",true);
        execute("UPDATE mulino_identity_Actors SET kind=? WHERE organizationId=? AND ID=?","AGENT",id("ORG"),id("reader"));
        var spoofedActor=actor.deepCopy();spoofedActor.put("role","ADMIN");spoofedActor.set("grant",Json.object().put("actions","ALL").put("scope","ORGANIZATION"));
        var agent=collect("all-of-agent",spoofedActor);assertTrue(agent.context().toString().contains(id("A60")));assertFalse(agent.context().toString().contains(id("B40")));check("agent-boundary-and-client-declared-grant-cannot-widen",true);
        assertEquals(StepResult.DriverStatus.EXECUTED,NativePermittedContextCapture.result("all-of-agent",spoofedActor,scope,agent,store).driverStatus());

        execute("UPDATE mulino_identity_CapabilityAssignments SET revokedAt=? WHERE organizationId=? AND actorId=?",OffsetDateTime.ofInstant(authority.minusSeconds(1),ZoneOffset.UTC),id("ORG"),id("supervisor"));
        assertEquals("PERMITTED_CONTEXT_NO_AUTHORIZED_READ",assertThrows(IllegalStateException.class,()->collect("delegator-revoked",actor)).getMessage());check("delegator-current-capability-revocation-old-business-time-denied",true);
        execute("UPDATE mulino_identity_CapabilityAssignments SET revokedAt=NULL WHERE organizationId=? AND actorId=?",id("ORG"),id("supervisor"));
        execute("UPDATE mulino_identity_Grants SET revokedAt=? WHERE organizationId=? AND ID=?",OffsetDateTime.ofInstant(authority.minusSeconds(1),ZoneOffset.UTC),id("ORG"),readGrant);
        assertEquals("PERMITTED_CONTEXT_NO_AUTHORIZED_READ",assertThrows(IllegalStateException.class,()->collect("grant-revoked",actor)).getMessage());check("current-read-grant-revoked-old-business-time-denied",true);
        execute("UPDATE mulino_identity_Grants SET revokedAt=NULL,validFrom=?,validUntil=? WHERE organizationId=? AND ID=?",OffsetDateTime.ofInstant(authority.minusSeconds(100),ZoneOffset.UTC),OffsetDateTime.ofInstant(authority.minusSeconds(1),ZoneOffset.UTC),id("ORG"),readGrant);
        assertEquals("PERMITTED_CONTEXT_NO_AUTHORIZED_READ",assertThrows(IllegalStateException.class,()->collect("grant-expired",actor)).getMessage());check("current-read-expiry-not-backdated-by-asOf",true);
        assertEquals(originalSourceGrant,sourceFingerprint());check("original-source-grant-unchanged",true);
        var report=store.base("DIRECT_HTTP_POSTGRESQL_PERMITTED_CONTEXT_PROBE",scope).put("status","PASS").put("actualModelCalls",0)
            .put("fullModelRunStatus","NOT_RUN").put("authenticatedServerCapture","CAPTURED")
            .put("baseline",config.buildVersion()).put("effectiveFixtureSha256",NativeClientPolicy.sha(input.toString()));
        report.set("checks",checks);report.set("positiveProductContext",positive.context());report.set("restrictedProductContext",restricted.context());
        report.set("authenticatedContextResult",result.toJson());report.put("authorityAsOf",authority.toString());
        report.set("restrictedAuthenticatedContextResult",restrictedResult.toJson());
        report.set("fixtureStartClockReceipt",clockReceipt);report.put("readGrantId",readGrant).put("originalSourceGrantId",sourceGrant);
        report.set("independentAnchorProjection",physical.path("projectionRevisionProof"));report.set("physicalSnapshot",physical.path("snapshot"));
        var captures=report.putArray("rawWireArtifacts");for(var c:positive.captures())captures.add(c.artifact());for(var c:restricted.captures())captures.add(c.artifact());
        var artifact=store.save("direct-context-report",report);Json.write(root.resolve(".local/s5-permitted-context/proof/direct-http-pg-report.json"),artifact);
    }
    ObjectNode fixture(String external) throws Exception {
        var f=NativeFixtureAuthority.current(Json.read(root.resolve("verification/actual/s1/fixture.json")),authority);f.put("fixtureId","NATIVE-PERMITTED-CONTEXT-DIRECT");
        var alias=(ObjectNode)f.path("aliases");alias.set("W2",Json.object().put("type","Place").put("kind","INTERNAL_STORAGE").put("name","동명 창고"));((ObjectNode)alias.path("W")).put("name","동명 창고");
        ((ObjectNode)alias.path("A60")).put("quantity","37");((ObjectNode)alias.path("B40")).put("quantity","11").put("locationAlias","W2");
        alias.set("DEF",Json.object().put("type","DefinitionVersion").set("content",Json.object().put("version","direct-read-v1").put("state","PUBLISHED").put("evaluatorVersion","1").put("schemaVersion","1")));
        for(JsonNode a:f.path("actors")){((ObjectNode)a).put("issuer",config.issuer()).put("audience",config.audience()).put("organizationAlias","ORG");((ObjectNode)a).set("roleCapabilities",Json.MAPPER.valueToTree(READS));((ObjectNode)a.path("grant")).set("actions",Json.MAPPER.valueToTree(READS));}
        f.put("probeExternalAlias",external);return f;
    }
    void seedBusiness() throws Exception {
        // Each installation uses an explicit external alias without touching another installation.
        String external="context-auth-"+UUID.randomUUID();execute("UPDATE mulino_identity_Organizations SET externalAlias=? WHERE ID=?",external,id("ORG"));
        execute("UPDATE mulino_identity_ExternalIdentities SET organizationAlias=? WHERE organizationId=?",external,id("ORG"));actor.put("organizationAlias",external);
        work=UUID.randomUUID().toString();obligation=UUID.randomUUID().toString();document=UUID.randomUUID().toString();
        var time=OffsetDateTime.ofInstant(fact,ZoneOffset.UTC);
        execute("INSERT INTO mulino_work_read_Works(organizationId,ID,createdAt,recordedAt,effectiveAt,itemId,definitionVersionId,kind,status,ownerId,supervisorId,nextAction,nextCheckAt) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",id("ORG"),work,time,time,time,id("P"),id("DEF"),"PURCHASE","WAITING",id("reader"),id("supervisor"),"도착 증거를 확인한다",time.plusDays(2));
        execute("INSERT INTO mulino_work_read_ObligationReferences(organizationId,ID,createdAt,recordedAt,effectiveAt,workId,kind,status,ownerId,supervisorId,nextAction,nextCheckAt,quantity,unit,scopeJson) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",id("ORG"),obligation,time,time,time,work,"FOLLOWUP","OPEN",id("reader"),id("supervisor"),"부족 물량을 대조한다",time.plusDays(2),new java.math.BigDecimal("9"),"BOX","{}");
        String profile=UUID.randomUUID().toString();
        execute("INSERT INTO mulino_evidence_SourceProfiles(organizationId,ID,revision,createdAt,recordedAt,recordedBy,namespace,policyVersion,intakeOwnerId,supervisorId,nextAction,nextCheckAt) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",id("ORG"),profile,1,time,time,id("reader"),"direct-http-pg","synthetic-read-only-v1",id("reader"),id("supervisor"),"원문을 대조한다",time.plusDays(2));
        execute("INSERT INTO mulino_evidence_DocumentVersions(organizationId,ID,revision,createdAt,recordedAt,recordedBy,subjectKind,subjectId,itemId,workId,sha256,byteLength,mediaType,availability,sourceNamespace,sourceProfileId,sourceReference,provenance) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",id("ORG"),document,1,time,time,id("reader"),"WORK",work,id("P"),work,"b".repeat(64),0L,"text/plain","UNKNOWN","direct-http-pg",profile,"실제 서버 문서 metadata","{}");
        seedDiscovery(time);
    }
    void seedDiscovery(OffsetDateTime time) throws Exception {
        String supplier=UUID.randomUUID().toString(),proposal=UUID.randomUUID().toString(),approval=UUID.randomUUID().toString(),order=UUID.randomUUID().toString(),line=UUID.randomUUID().toString(),invoice=UUID.randomUUID().toString(),proc=UUID.randomUUID().toString();
        insertTrade("Suppliers",supplier,time,"name","direct supplier");
        insertTrade("Proposals",proposal,time,"workId",work,"currentRevision",1,"status","APPROVED");
        insertTrade("ProposalRevisions",UUID.randomUUID().toString(),time,"proposalId",proposal,"workId",work,"itemId",id("P"),"supplierId",supplier,"destinationId",id("W"),"quantity",9,"unit","BOX","price",1,"currency","EUR","dueAt",time.plusDays(2),"endpoint","ARRIVED","quantityMode","CUMULATIVE_EVENT","proposalHash","a".repeat(64));
        insertTrade("Approvals",approval,time,"proposalId",proposal,"proposalRevision",1,"proposalHash","a".repeat(64),"approverId",id("reader"),"policyHash","b".repeat(64),"consumptionPolicy","SINGLE_ORDER_REVISION","decision","APPROVE","conditionsJson","[]","decidedAt",time,"validUntil",time.plusDays(2));
        insertTrade("Orders",order,time,"proposalId",proposal,"proposalRevision",1,"proposalHash","a".repeat(64),"approvalId",approval,"channel","synthetic","externalOperationId",UUID.randomUUID().toString(),"outboxId",UUID.randomUUID().toString(),"conditionAssessmentJson","{}");
        insertTrade("OrderLines",line,time,"orderId",order,"proposalId",proposal,"proposalRevision",1,"workId",work,"itemId",id("P"),"destinationId",id("W"),"quantity",9,"unit","BOX","status","DISPATCH_PENDING");
        execute("INSERT INTO mulino_trade_settlement_Invoices(organizationId,ID,revision,createdAt,recordedAt,effectiveAt,workId,itemId,lineId,referenceId,scopeKind,invoiceKind,sourceNamespace,sourceKey,sourceVersion,sourceHash,evidenceId,quantity,unit,unitPrice,originalAmount,currency) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",id("ORG"),invoice,1,LocalDateTime.ofInstant(time.toInstant(),ZoneOffset.UTC),LocalDateTime.ofInstant(time.toInstant(),ZoneOffset.UTC),LocalDateTime.ofInstant(time.toInstant(),ZoneOffset.UTC),work,id("P"),line,UUID.randomUUID().toString(),"PURCHASE","COMMERCIAL","direct-read",UUID.randomUUID().toString(),"1","c".repeat(64),document,9,"BOX",1,9,"EUR");
        execute("INSERT INTO mulino_trade_regulatory_Procedures(organizationId,ID,revision,createdAt,recordedAt,recordedBy,itemId,physicalScopeId,workId) VALUES(?,?,?,?,?,?,?,?,?)",id("ORG"),proc,1,time,time,id("reader"),id("P"),id("A60"),work);
        execute("INSERT INTO mulino_trade_regulatory_ProcedureVersions(organizationId,ID,revision,createdAt,recordedAt,recordedBy,procedureId,itemId,physicalScopeId,workId,status,documentVersionId) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",id("ORG"),UUID.randomUUID().toString(),1,time,time,id("reader"),proc,id("P"),id("A60"),work,"PREPARING",document);
        discovered.put("PurchaseProposal",proposal);discovered.put("PurchaseOrder",order);discovered.put("Invoice",invoice);discovered.put("RegulatoryProcedure",proc);
    }
    void insertTrade(String table,String target,OffsetDateTime time,Object...fields) throws Exception {
        var values=new LinkedHashMap<String,Object>();values.put("organizationId",id("ORG"));values.put("ID",target);values.put("revision",1);values.put("createdAt",time);values.put("recordedAt",time);values.put("effectiveAt",time);
        for(int i=0;i<fields.length;i+=2)values.put(fields[i].toString(),fields[i+1]);
        execute("INSERT INTO mulino_trade_purchase_"+table+" ("+String.join(",",values.keySet())+") VALUES ("+String.join(",",Collections.nCopies(values.size(),"?"))+")",values.values().toArray());
    }
    void validateActualSourceMutants(JsonNode receipt) {
        var server=receipt.path("serverAuthenticatedCapture");var rpc=receipt.path("request");String reply=receipt.path("response").toString();
        java.util.function.Consumer<JsonNode> validate=c->NativeMcpTransport.validateServerCapture(c,server.path("captureId").asText(),server.path("requestNonce").asText(),rpc,reply,receipt.path("httpStatus").asInt(),actor,scope,receipt.path("requestSentAt").asText(),receipt.path("responseReceivedAt").asText());
        assertDoesNotThrow(()->validate.accept(server));
        for(String mode:List.of("missing","wrong-dimension","oversize","untyped")){
            var mutated=server.deepCopy();boolean changed=false;
            for(JsonNode g:mutated.path("grants"))for(JsonNode s:g.path("scopes"))if(s.path("scopeKind").asText().equals("SOURCE")){
                var source=(ObjectNode)s;switch(mode){case "missing"->source.remove("scopeKind");case "wrong-dimension"->source.put("scopeKind","TARGET");case "oversize"->source.put("scopeId","n".repeat(37));default->source.set("scopeId",Json.object().put("namespace","carrier-context-probe"));}changed=true;
            }
            assertTrue(changed);assertThrows(RuntimeException.class,()->validate.accept(mutated));
        }
        check("actual-server-capture-missing-wrong-dimension-tampered-SOURCE-mutants-fail-closed",true);
    }
    NativePermittedContext.Collected collect(String action,JsonNode principal) throws Exception {
        var c=NativePermittedContext.collect(contextRequest,(op,q)->transport.send(action,"tools/call",op,q,principal,scope));
        for(var capture:c.captures())assertFalse(capture.request().path("params").path("arguments").path("scope").has("caseId"));return c;
    }
    String id(String alias){return aliases.path(alias).asText();}
    JsonNode fixtureStartClock() throws Exception {
        var body=Json.object().put("instant",authority.toString());String sent=Instant.now().toString();
        var http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        var reset=HttpRequest.newBuilder(config.baseUri().resolve("/__verification/runtime/clock/fixture-start"))
            .timeout(Duration.ofSeconds(10)).header("Content-Type","application/json").header("Authorization","Bearer "+new JwtSigner(config).sign(actor))
            .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        var response=http.send(reset,HttpResponse.BodyHandlers.ofString());var raw=Json.parse(response.body());
        assertEquals(200,response.statusCode());assertTrue(raw.path("authorityClock").asBoolean());assertEquals(authority,Instant.parse(raw.path("instant").asText()));
        var receipt=store.base("ACTUAL_VERIFICATION_FIXTURE_START_CLOCK",scope).put("endpointPath","/__verification/runtime/clock/fixture-start")
            .put("httpStatus",response.statusCode()).put("requestSentAt",sent).put("responseReceivedAt",Instant.now().toString());
        receipt.set("request",body);receipt.set("response",raw);return store.save("fixture-start-clock",receipt);
    }
    String sourceFingerprint() throws Exception {return sqlScalar("SELECT md5(row_to_json(g)::text || (SELECT json_agg(s ORDER BY scopeKind,scopeId)::text FROM mulino_identity_GrantScopes s WHERE s.organizationId=g.organizationId AND s.grantId=g.ID) || (SELECT json_agg(a ORDER BY capabilityId)::text FROM mulino_identity_GrantActions a WHERE a.organizationId=g.organizationId AND a.grantId=g.ID)) FROM mulino_identity_Grants g WHERE organizationId=? AND ID=?",id("ORG"),sourceGrant);}
    void execute(String sql,Object...args) throws Exception {try(var c=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password());var s=c.prepareStatement(sql)){for(int i=0;i<args.length;i++)s.setObject(i+1,args[i]);s.executeUpdate();}}
    String sqlScalar(String sql,Object...args) throws Exception {try(var c=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password());var s=c.prepareStatement(sql)){for(int i=0;i<args.length;i++)s.setObject(i+1,args[i]);try(var r=s.executeQuery()){assertTrue(r.next());return r.getString(1);}}}
    String businessFingerprint() throws Exception {
        var all=Json.object();for(String table:List.of("mulino_inventory_QuantitySegments","mulino_work_read_Works","mulino_work_read_ObligationReferences","mulino_evidence_DocumentVersions","mulino_commands_CommandRecords","mulino_runtime_Outbox","mulino_trade_purchase_Proposals","mulino_trade_purchase_ProposalRevisions","mulino_trade_purchase_Orders","mulino_trade_purchase_OrderLines","mulino_trade_settlement_Invoices","mulino_trade_regulatory_Procedures","mulino_trade_regulatory_ProcedureVersions"))
            all.put(table,sqlScalar("SELECT md5(coalesce(string_agg(to_jsonb(r)::text,'|' ORDER BY ID),'')) FROM "+table+" r WHERE organizationId=?",id("ORG")));
        return NativeClientPolicy.sha(all.toString());
    }
    void check(String name,boolean pass){checks.add(Json.object().put("check",name).put("passed",pass));}
}
