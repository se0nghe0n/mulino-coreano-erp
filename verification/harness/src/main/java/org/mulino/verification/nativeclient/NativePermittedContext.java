package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.mulino.verification.*;
import org.mulino.verification.modelbinding.ModelBindingRunner;
import java.time.Instant;
import java.util.*;

/** Harness-only read plan. Correlation IDs never enter product authority or select business records.
 * Every fact comes from ordinary authenticated product queries, not installation input or model oracles. */
final class NativePermittedContext {
    static final String VIEW="ACTOR_PERMITTED_BUSINESS_CONTEXT";
    // Supported business nouns, deliberately excluding command records, policy and definition control payloads.
    static final List<String> TYPES=List.of("Product","TradeItem","ManufacturingLot","QuantitySegment",
        "LogisticsUnit","Place","Manufacturer","Customer","SalesOrder","SalesOrderRevision","SalesOrderLine",
        "Delivery","DeliveryObservation","Shipment","Receipt","ReceiptObservation",
        "Return","Recall","RecallScope","Allocation","Restriction",
        "DispositionBasis","Dispatch","ReturnAuthorization","Obligation","Handover",
        "PurchaseProposal","PurchaseOrder","Invoice","RegulatoryProcedure");
    static final int MAX_READS=1024,MAX_PAGES=20;
    @FunctionalInterface interface Reader { NativeMcpTransport.Capture read(String operation,JsonNode request) throws Exception; }
    record Collected(ObjectNode context,List<NativeMcpTransport.Capture> captures,NativeMcpTransport.Capture projection) {}

    static boolean matches(String operation,JsonNode request) {
        return "getObject".equals(operation)&&VIEW.equals(request.path("view").asText());
    }
    static void validate(JsonNode request,JsonNode installation) {
        require(request.isObject()&&keys(request).equals(Set.of("view","scope","asOf","knownAt"))
            &&VIEW.equals(request.path("view").asText()),"PERMITTED_CONTEXT_REQUEST_INVALID");
        JsonNode scope=request.path("scope");
        require(scope.isObject()&&keys(scope).equals(Set.of("organizationId","installationId","caseId","turnId")),"PERMITTED_CONTEXT_SCOPE_INVALID");
        for(String k:List.of("organizationId","installationId","caseId","turnId"))Json.required(scope,k);
        UUID.fromString(Json.required(scope,"organizationId"));
        require(installation!=null&&scope.path("organizationId").equals(installation.path("organizationId"))
            &&scope.path("installationId").equals(installation.path("installationId")),"NATIVE_INSTALLATION_SCOPE_MISMATCH");
        for(String k:List.of("asOf","knownAt"))Instant.parse(Json.required(request,k));
    }
    static StepResult query(String id,JsonNode actor,JsonNode request,JsonNode installation,Reader reader,
                            NativeIntegrationEvidence evidence) throws Exception {
        validate(request,installation);
        var collected=collect(request,reader);
        return NativePermittedContextCapture.result(id,actor,request.path("scope"),collected,evidence);
    }
    /** Also used for direct HTTP diagnostics when the baseline has no server identity capture hook. */
    static Collected collect(JsonNode request,Reader reader) throws Exception {
        var collector=new Collector(request,reader);
        for(String type:TYPES)collector.pages("searchObjects",type,null);
        collector.pages("searchWorks",null,null);
        for(String kind:List.of("DOCUMENT","EVENT","CLAIM","CANONICAL"))collector.pages("getEvidence",null,kind);
        // Organization inventory is itself filtered per segment/item by the product, including all-of grants.
        collector.call("getInventory",collector.request(null,null));
        // A separate exact read anchors independent projection verification. Never hash the merged context as a DB revision.
        for(String type:List.of("Place","Product","Manufacturer")) {
            if(collector.anchor!=null)break;
            JsonNode rows=collector.discovered.get(type);
            if(rows!=null)for(JsonNode row:rows) {
                var q=collector.request(type,null);q.put("id",Json.required(row,"ID"));
                var capture=collector.call("getObject",q);
                if(success(capture)){collector.anchor=capture;break;}
            }
        }
        if(collector.anchor==null) {
            // Re-read at the end: a denied anchor must never fall back to a success from before a revocation.
            var finalRead=collector.call("getInventory",collector.request(null,null));
            if(success(finalRead))collector.anchor=finalRead;
        }
        require(collector.anchor!=null,"PERMITTED_CONTEXT_NO_AUTHORIZED_READ");
        var context=Json.object().put("contextVersion","native-permitted-business-context-v1")
            .put("snapshotSemantics","PER_READ_REVISIONS; projectionReadRef identifies one exact read, not an atomic context snapshot")
            .put("asOf",Json.required(request,"asOf")).put("knownAt",Json.required(request,"knownAt"));
        context.set("scope",request.path("scope").deepCopy());context.set("reads",collector.reads);
        context.set("discoveryGaps",Json.array());
        ModelBindingRunner.checkContext(context);NativeClientPolicy.rejectSecrets(context);
        return new Collected(context,List.copyOf(collector.captures),collector.anchor);
    }
    private static final class Collector {
        final JsonNode contextRequest;final Reader reader;
        final ArrayNode reads=Json.array();final List<NativeMcpTransport.Capture> captures=new ArrayList<>();
        final Map<String,ArrayNode> discovered=new LinkedHashMap<>();
        NativeMcpTransport.Capture anchor;
        Collector(JsonNode request,Reader reader){this.contextRequest=request;this.reader=reader;}
        ObjectNode request(String type,String kind) {
            var q=Json.object();var scope=q.putObject("scope");scope.set("organizationId",contextRequest.path("scope").path("organizationId"));
            if(type!=null)scope.put("objectType",type);if(kind!=null)scope.put("kind",kind);
            q.set("asOf",contextRequest.path("asOf"));q.set("knownAt",contextRequest.path("knownAt"));return q;
        }
        NativeMcpTransport.Capture call(String operation,JsonNode q) throws Exception {
            require(captures.size()<MAX_READS,"PERMITTED_CONTEXT_READ_BOUND_EXCEEDED");
            var capture=reader.read(operation,q.deepCopy());captures.add(capture);
            require(capture.request().path("params").path("name").asText().equals(operation)
                &&capture.request().path("params").path("arguments").equals(q),"PERMITTED_CONTEXT_WIRE_REQUEST_MISMATCH");
            if(!success(capture)) {
                // A denied read is absent data, never an empty authorized result. Other failures invalidate the context.
                require(capture.status()==200&&capture.businessResponse().path("error").path("code").asText().equals("FORBIDDEN"),
                    "PERMITTED_CONTEXT_PRODUCT_READ_FAILED");
                return capture;
            }
            var response=capture.businessResponse();
            require(response.path("scope").path("organizationId").equals(q.path("scope").path("organizationId")),"PERMITTED_CONTEXT_FOREIGN_RESPONSE");
            for(String k:List.of("asOf","knownAt"))require(response.path(k).equals(q.path(k)),"PERMITTED_CONTEXT_READ_TIME_MISMATCH");
            Json.required(response,"snapshotRevision");require(response.has("data")&&response.has("nextCursor"),"PERMITTED_CONTEXT_READ_INCOMPLETE");
            var read=Json.object().put("operation",operation);read.set("request",q.deepCopy());read.set("response",response.deepCopy());
            reads.add(read);return capture;
        }
        void pages(String operation,String type,String kind) throws Exception {
            var q=request(type,kind).put("limit",200);var cursors=new HashSet<String>();var ids=new HashSet<String>();
            var rows=Json.array();
            for(int page=0;page<MAX_PAGES;page++) {
                var capture=call(operation,q);if(!success(capture))return;
                var response=capture.businessResponse();require(response.path("data").isArray(),"PERMITTED_CONTEXT_LIST_INVALID");
                for(JsonNode row:response.path("data")) {
                    require(row.isObject()&&ids.add(Json.required(row,"ID")),"PERMITTED_CONTEXT_PAGE_DRIFT");rows.add(row);
                }
                JsonNode next=response.path("nextCursor");
                if(next.isNull()) {
                    if(type!=null)discovered.put(type,rows);
                    if(operation.equals("searchWorks"))for(JsonNode row:rows){var detail=request(null,null);detail.put("id",Json.required(row,"ID"));call("getWork",detail);}
                    // Item detail connects the noun to current Work, evidence and obligations using the same public world read.
                    if("TradeItem".equals(type))for(JsonNode row:rows){var detail=request(type,null);detail.put("id",Json.required(row,"ID"));call("getObject",detail);}
                    return;
                }
                require(next.isTextual()&&!next.asText().isBlank()&&cursors.add(next.asText()),"PERMITTED_CONTEXT_CURSOR_INVALID");
                q.set("cursor",next);
            }
            throw new IllegalStateException("PERMITTED_CONTEXT_PAGE_BOUND_EXCEEDED");
        }
    }
    static boolean success(NativeMcpTransport.Capture c) {return c.status()==200&&!c.response().has("error")
        &&!c.response().path("result").path("isError").asBoolean()&&c.businessResponse().isObject()&&!c.businessResponse().has("error");}
    static Set<String> keys(JsonNode n){var out=new HashSet<String>();n.fieldNames().forEachRemaining(out::add);return out;}
    static void require(boolean value,String reason){NativeClientPolicy.require(value,reason);}
    private NativePermittedContext() {}
}
