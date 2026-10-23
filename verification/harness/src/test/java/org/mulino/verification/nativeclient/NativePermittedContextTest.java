package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mulino.verification.*;
import org.mulino.verification.modelbinding.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Contract doubles only. Their captured-api artifacts explicitly remain CAPTURED_CONTRACT_SELFTEST. */
class NativePermittedContextTest {
    @TempDir Path root;
    final String org=UUID.randomUUID().toString(),installation=UUID.randomUUID().toString(),actorId=UUID.randomUUID().toString(),grantId=UUID.randomUUID().toString();
    NativeIntegrationEvidence store;
    @BeforeEach void setup() throws Exception {root=root.toRealPath();store=new NativeIntegrationEvidence(root);}
    ObjectNode scope(){return Json.object().put("organizationId",org).put("installationId",installation).put("caseId","correlation-case").put("turnId","turn-1");}
    ObjectNode request(){var q=Json.object().put("view",NativePermittedContext.VIEW).put("asOf","2026-10-07T09:00:00Z").put("knownAt","2026-10-07T09:00:00Z");q.set("scope",scope());return q;}
    ObjectNode actor(){return Json.object().put("issuer","https://synthetic.invalid").put("subject","reader").put("audience","ontology");}
    ObjectNode server(JsonNode rpc) {
        var s=Json.object().put("recordType","SERVER_AUTHENTICATED_MCP_CAPTURE_V1").put("authorityAsOf","2026-10-24T09:00:00Z")
            .put("observationPhase","BEFORE_DISPATCH_READ_SNAPSHOT").put("actionAuthorization","NOT_ASSERTED")
            .put("requestSha256",NativeClientPolicy.sha(rpc.toString())).put("httpStatus",200);
        s.set("rpcId",rpc.path("id"));s.set("rpcMethod",rpc.path("method"));s.set("toolName",rpc.path("params").path("name"));
        var identity=actor().put("organizationId",org).put("actorId",actorId);identity.remove("audience");identity.set("audiences",Json.array().add("ontology"));s.set("identity",identity);
        var grant=Json.object().put("ID",grantId).put("actorId",actorId).put("validFrom","2026-10-20T00:00:00Z").put("validUntil","2026-11-01T00:00:00Z");
        grant.set("actions",Json.array().add(Json.object().set("capabilityId",rpc.path("params").path("name"))));
        grant.set("scopes",Json.array().add(Json.object().put("scopeKind","ORGANIZATION").put("scopeId",org)));
        s.set("grants",Json.array().add(grant));
        var assignment=Json.object().put("scopeKind","ORGANIZATION").put("scopeId",org).put("validFrom","2026-10-20T00:00:00Z").put("validUntil","2026-11-01T00:00:00Z");assignment.set("capabilityId",rpc.path("params").path("name"));s.set("assignments",Json.array().add(assignment));return s;
    }
    NativeMcpTransport.Capture capture(String operation,JsonNode args,JsonNode data,String revision,boolean authenticated) throws Exception {
        var rpc=Json.object().put("jsonrpc","2.0").put("id",UUID.randomUUID().toString()).put("method","tools/call");var params=rpc.putObject("params").put("name",operation);params.set("arguments",args);
        var business=Json.object().put("snapshotRevision",revision).put("asOf",args.path("asOf").asText()).put("knownAt",args.path("knownAt").asText()).putNull("nextCursor");business.set("scope",args.path("scope"));business.set("data",data);
        var response=Json.object().put("jsonrpc","2.0");response.set("id",rpc.path("id"));response.set("result",Json.object().put("isError",false).set("structuredContent",business));
        var receipt=store.base("CAPTURED_CONTRACT_SELFTEST",scope()).put("authenticatedActorStatus",authenticated?"CAPTURED":"NOT_IMPLEMENTED")
            .put("requestSentAt","2026-10-24T09:00:00Z").put("responseReceivedAt","2026-10-24T09:00:01Z").put("httpStatus",200);
        receipt.set("request",rpc);receipt.set("response",response);if(authenticated)receipt.set("serverAuthenticatedCapture",server(rpc));
        return new NativeMcpTransport.Capture(rpc,response,200,store.save("contract",receipt));
    }
    @Test void correlationAndPayloadSpoofNeverBecomeProductSelectors() throws Exception {
        var q=request();NativePermittedContext.validate(q,scope());
        var calls=new ArrayList<JsonNode>();
        var collected=NativePermittedContext.collect(q,(op,input)->{calls.add(input);return capture(op,input,op.equals("getInventory")?Json.object():Json.array(),"real-server-revision",false);});
        assertFalse(collected.context().path("reads").isEmpty());
        for(JsonNode input:calls){assertFalse(input.has("view"));assertFalse(input.has("actor"));assertEquals(Set.of("organizationId"),NativePermittedContext.keys(input.path("scope")).stream().filter(k->!Set.of("objectType","kind").contains(k)).collect(java.util.stream.Collectors.toSet()));}
        ((ObjectNode)q.path("scope")).put("organizationId",UUID.randomUUID().toString());
        assertThrows(IllegalStateException.class,()->NativePermittedContext.validate(q,scope()));
        var spoof=request();spoof.putObject("actor").put("role","ADMIN");assertThrows(IllegalStateException.class,()->NativePermittedContext.validate(spoof,scope()));
        spoof.remove("actor");
        ((ObjectNode)spoof.path("scope")).put("itemId",UUID.randomUUID().toString());assertThrows(IllegalStateException.class,()->NativePermittedContext.validate(spoof,scope()));
    }
    @Test void duplicateNamesKeepBothIdsAndProjectionUsesExactSingleServerRead() throws Exception {
        String one=UUID.randomUUID().toString(),two=UUID.randomUUID().toString();
        var q=request();
        var collected=NativePermittedContext.collect(q,(op,input)->{
            var rows=Json.array();String type=input.path("scope").path("objectType").asText();
            if(op.equals("searchObjects")&&type.equals("Place")){rows.add(Json.object().put("ID",one).put("name","동명 창고"));rows.add(Json.object().put("ID",two).put("name","동명 창고"));}
            JsonNode data=op.equals("getObject")?Json.object().put("ID",input.path("id").asText()).put("name","동명 창고"):
                op.equals("getInventory")?Json.object().put("itemId",org).put("heldQuantity","71"):rows;
            return capture(op,input,data,op.equals("getObject")?"anchor-server-revision":"list-server-revision",true);
        });
        assertTrue(collected.context().toString().contains(one));assertTrue(collected.context().toString().contains(two));
        var result=NativePermittedContextCapture.result("turn-1/context",actor(),scope(),collected,store);
        assertEquals(StepResult.DriverStatus.EXECUTED,result.driverStatus());assertEquals("anchor-server-revision",result.data().path("snapshotRevision").asText());
        var wire=Json.read(root.resolve(result.data().path("projectionReadRef").asText()));
        assertEquals("CAPTURED_CONTRACT_SELFTEST",wire.path("evidenceClass").asText());assertEquals("CAPTURED_CONTRACT_SELFTEST",result.provenance().path("source").asText());
        assertEquals(collected.projection().request(),wire.path("request"));assertEquals(collected.projection().response(),wire.path("response"));
        assertEquals(result.data().path("snapshotRevision"),CapturedApiObservation.decode(wire.path("toolCall"),wire).response().path("snapshotRevision"));
        new ContractValidator(Path.of(System.getProperty("repo.root"))).schema("verification/model-binding/captured-api.schema.json",wire);
    }
    @Test void absentCaptureNeverReturnsFactsEvenWhenActualQueriesSucceeded() throws Exception {
        var q=request();var collected=NativePermittedContext.collect(q,(op,input)->capture(op,input,op.equals("getInventory")?Json.object().put("heldQuantity","71"):Json.array(),"r",false));
        var result=NativePermittedContextCapture.result("context",actor(),scope(),collected,store);
        assertEquals(StepResult.DriverStatus.NOT_IMPLEMENTED,result.driverStatus());assertNull(result.data());assertNull(result.response());assertFalse(result.artifactRefs().isEmpty());
    }
    @Test void currentAuthorityTimeAndAmbiguousGrantAreNeverReplacedByRequestPayload() {
        var rpc=Json.object().put("method","tools/call");rpc.set("params",Json.object().put("name","getObject"));var server=server(rpc);
        // Business time is old, current authority is 24 October. Grant starts on 20 October and is current.
        var requested=actor();requested.putObject("grant").put("grantId",UUID.randomUUID().toString()).put("role","ADMIN");
        var args=Json.object().set("scope",Json.object().put("objectType","Place"));var response=Json.object().set("data",Json.object().put("ID",org));
        assertEquals(grantId,NativePermittedContextCapture.identity(server,requested,scope(),"getObject",args,response).path("grantId").asText());
        ((ObjectNode)server.path("grants").get(0)).put("revokedAt","2026-10-24T08:59:59Z");
        assertThrows(IllegalStateException.class,()->NativePermittedContextCapture.identity(server,requested,scope(),"getObject",args,response));
        ((ObjectNode)server.path("grants").get(0)).remove("revokedAt");((ArrayNode)server.path("grants")).add(server.path("grants").get(0).deepCopy());
        assertThrows(IllegalStateException.class,()->NativePermittedContextCapture.identity(server,requested,scope(),"getObject",args,response));
    }
    @Test void wrongRevisionTimeAndCursorCounterexamplesRemainClosed() throws Exception {
        var q=request();
        assertThrows(IllegalStateException.class,()->NativePermittedContext.collect(q,(op,input)->{
            var c=capture(op,input,Json.array(),"r",false);((ObjectNode)c.businessResponse()).put("asOf","2026-10-08T09:00:00Z");return c;
        }));
        assertThrows(IllegalStateException.class,()->NativePermittedContext.collect(q,(op,input)->{
            var c=capture(op,input,Json.array().add(Json.object().put("ID","same-id")),"r",false);((ObjectNode)c.businessResponse()).put("nextCursor","same-cursor");return c;
        }));
        var c=capture("getObject",Json.object().set("scope",scope()),Json.object(),"r",true);
        ((ObjectNode)c.response()).put("forged",true);assertThrows(IllegalStateException.class,()->NativePermittedContextCapture.receipt(c,store));
    }
    @Test void oracleFieldFromAnyUpstreamSourceCannotEnterPrompt() {
        assertThrows(IllegalArgumentException.class,()->NativePermittedContext.collect(request(),(op,input)->capture(op,input,
            Json.array().add(Json.object().put("ID","x").put("expectedIntent","forbidden")),"r",false)));
    }
    @Test void aDeniedFinalAnchorCannotReuseFactsFromBeforeRevocation() throws Exception {
        int[] inventory={0};
        var failure=assertThrows(IllegalStateException.class,()->NativePermittedContext.collect(request(),(op,input)->{
            var c=capture(op,input,op.equals("getInventory")?Json.object():Json.array(),"r",false);
            if(op.equals("getInventory")&&++inventory[0]==2){((ObjectNode)c.response().path("result")).put("isError",true);((ObjectNode)c.response().path("result")).set("structuredContent",Json.object().set("error",Json.object().put("code","FORBIDDEN")));}
            return c;
        }));
        assertEquals("PERMITTED_CONTEXT_NO_AUTHORIZED_READ",failure.getMessage());
    }
    @Test void stablePagingRetainsEachIdAndNeverSilentlyTruncates() throws Exception {
        var collected=NativePermittedContext.collect(request(),(op,input)->{
            boolean places=op.equals("searchObjects")&&input.path("scope").path("objectType").asText().equals("Place");
            var data=places?Json.array().add(Json.object().put("ID",input.has("cursor")?"second":"first").put("name","동명")):
                op.equals("getInventory")||op.equals("getObject")?Json.object():Json.array();
            var c=capture(op,input,data,"r",false);if(places&&!input.has("cursor"))((ObjectNode)c.businessResponse()).put("nextCursor","opaque-server-cursor");return c;
        });
        int pages=0;for(JsonNode read:collected.context().path("reads"))if(read.path("operation").asText().equals("searchObjects")&&read.path("request").path("scope").path("objectType").asText().equals("Place"))pages++;
        assertEquals(2,pages);assertTrue(collected.context().toString().contains("second"));
    }
    @Test void sourceDimensionMustMatchActualReadTargetsAndNeverFixtureGrantChoice() {
        var rpc=Json.object().put("method","tools/call");rpc.set("params",Json.object().put("name","getObject"));var s=server(rpc);
        var source=s.path("grants").get(0).deepCopy();((ObjectNode)source).put("ID",UUID.randomUUID().toString());
        ((ArrayNode)source.path("scopes")).add(Json.object().put("scopeKind","SOURCE").put("scopeId","carrier-namespace"));((ArrayNode)s.path("grants")).add(source);
        var args=Json.object().set("scope",Json.object().put("objectType","Place"));var response=Json.object().set("data",Json.object().put("ID",org).put("sourceProfileId","carrier-namespace"));
        // A random sourceProfileId property cannot add a SOURCE dimension to a Place read.
        assertEquals(grantId,NativePermittedContextCapture.identity(s,actor(),scope(),"getObject",args,response).path("grantId").asText());
        ((ObjectNode)s.path("grants").get(0)).put("revokedAt","2026-10-24T08:00:00Z");
        assertThrows(IllegalStateException.class,()->NativePermittedContextCapture.identity(s,actor(),scope(),"getObject",args,response));
    }
    @Test void allOfUsesServerTargetPlaceAndCurrentAssignmentAndRejectsTwoApplicableGrants() {
        var rpc=Json.object().put("method","tools/call");rpc.set("params",Json.object().put("name","getObject"));var s=server(rpc);
        String segment=UUID.randomUUID().toString(),item=UUID.randomUUID().toString(),place=UUID.randomUUID().toString();
        var grant=(ObjectNode)s.path("grants").get(0);grant.set("scopes",Json.array().add(Json.object().put("scopeKind","TARGET").put("scopeId",segment)).add(Json.object().put("scopeKind","PLACE").put("scopeId",place)));
        var args=Json.object().set("scope",Json.object().put("objectType","QuantitySegment"));var response=Json.object().set("data",Json.object().put("ID",segment).put("itemId",item).put("placeId",place));
        var wrong=grant.deepCopy().put("ID",UUID.randomUUID().toString());((ObjectNode)wrong.path("scopes").get(1)).put("scopeId",UUID.randomUUID().toString());((ArrayNode)s.path("grants")).add(wrong);
        assertEquals(grantId,NativePermittedContextCapture.identity(s,actor(),scope(),"getObject",args,response).path("grantId").asText());
        ((ObjectNode)s.path("grants").get(1).path("scopes").get(1)).put("scopeId",place);
        assertThrows(IllegalStateException.class,()->NativePermittedContextCapture.identity(s,actor(),scope(),"getObject",args,response));
        ((ArrayNode)s.path("grants")).remove(1);((ObjectNode)s.path("assignments").get(0)).put("revokedAt","2026-10-24T08:00:00Z");
        assertThrows(IllegalStateException.class,()->NativePermittedContextCapture.identity(s,actor(),scope(),"getObject",args,response));
    }
    @Test void evidenceSourceComesOnlyFromActualEvidenceRowAndEmptyRowsDoNotClaimAGrant() {
        var rpc=Json.object().put("method","tools/call");rpc.set("params",Json.object().put("name","getEvidence"));var s=server(rpc);
        ((ObjectNode)s.path("grants").get(0)).set("scopes",Json.array().add(Json.object().put("scopeKind","SOURCE").put("scopeId",org)));
        var args=Json.object();var response=Json.object().set("data",Json.array().add(Json.object().put("ID",actorId).put("sourceProfileId",org)));
        assertEquals(grantId,NativePermittedContextCapture.identity(s,actor(),scope(),"getEvidence",args,response).path("grantId").asText());
        // A syntactically valid but different namespace never inherits the observed SOURCE authority.
        ((ObjectNode)s.path("grants").get(0).path("scopes").get(0)).put("scopeId","different-namespace");
        assertThrows(IllegalStateException.class,()->NativePermittedContextCapture.identity(s,actor(),scope(),"getEvidence",args,response));
        ((ObjectNode)s.path("grants").get(0).path("scopes").get(0)).put("scopeId",org);
        ((ObjectNode)response.path("data").get(0)).remove("sourceProfileId");
        assertThrows(IllegalStateException.class,()->NativePermittedContextCapture.identity(s,actor(),scope(),"getEvidence",args,response));
        ((ObjectNode)response).set("data",Json.array());assertTrue(NativePermittedContextGrants.emptyRead("getEvidence",args,response));
        assertThrows(IllegalStateException.class,()->NativePermittedContextCapture.identity(s,actor(),scope(),"getEvidence",args,response));
    }
    @Test void invoiceGrantAttributionUsesOnlyServerJoinedReadScopeAndRejectsMissingOrWrongDimension() {
        var rpc=Json.object().put("method","tools/call");rpc.set("params",Json.object().put("name","getObject"));var s=server(rpc);
        var grant=(ObjectNode)s.path("grants").get(0);grant.set("scopes",Json.array().add(Json.object().put("scopeKind","ITEM").put("scopeId",org)).add(Json.object().put("scopeKind","PLACE").put("scopeId",actorId)));
        var args=Json.object().set("scope",Json.object().put("objectType","Invoice"));
        var invoice=Json.object().put("ID",UUID.randomUUID().toString()).put("itemId",org).put("placeId",actorId);
        var response=Json.object().set("data",invoice);
        assertThrows(IllegalStateException.class,()->NativePermittedContextCapture.identity(s,actor(),scope(),"getObject",args,response));
        var joined=invoice.putObject("readScope");joined.set("ITEM",Json.array().add(org));joined.set("WORK",Json.array().add(actorId));joined.set("PLACE",Json.array().add(actorId));
        assertEquals(grantId,NativePermittedContextCapture.identity(s,actor(),scope(),"getObject",args,response).path("grantId").asText());
        var correction=Json.object().put("ID",UUID.randomUUID().toString()).set("readScope",joined.deepCopy());invoice.set("corrections",Json.array().add(correction));
        assertEquals(grantId,NativePermittedContextCapture.identity(s,actor(),scope(),"getObject",args,response).path("grantId").asText());
        String otherPlace=UUID.randomUUID().toString();((ObjectNode)correction.path("readScope")).set("PLACE",Json.array().add(otherPlace));
        assertThrows(IllegalStateException.class,()->NativePermittedContextCapture.identity(s,actor(),scope(),"getObject",args,response));
        var second=grant.deepCopy().put("ID",UUID.randomUUID().toString());((ObjectNode)second.path("scopes").get(1)).put("scopeId",otherPlace);((ArrayNode)s.path("grants")).add(second);
        assertThrows(IllegalStateException.class,()->NativePermittedContextCapture.identity(s,actor(),scope(),"getObject",args,response));
        ((ArrayNode)s.path("grants")).remove(1);invoice.remove("corrections");
        joined.set("PLACE",Json.array().add(UUID.randomUUID().toString()));
        assertThrows(IllegalStateException.class,()->NativePermittedContextCapture.identity(s,actor(),scope(),"getObject",args,response));
        joined.remove("PLACE");joined.set("SOURCE",Json.array().add(actorId));
        assertThrows(IllegalStateException.class,()->NativePermittedContextCapture.identity(s,actor(),scope(),"getObject",args,response));
    }
}
