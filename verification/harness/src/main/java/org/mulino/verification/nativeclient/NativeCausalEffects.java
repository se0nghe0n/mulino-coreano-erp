package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.mulino.verification.*;
import java.nio.file.*;
import java.util.*;

/** Causal effects of captured successful commands. xmin checks a transaction, never names an effect. */
final class NativeCausalEffects {
    private record Meaning(String clazz,String operation) {}
    private static final Map<String,Meaning> PRIMARY=Map.ofEntries(
        entry("mulino_inventory_QuantityMovements","QUANTITY","MOVE"),
        entry("mulino_inventory_Dispatches","DISPATCH","DISPATCH"),
        entry("mulino_inventory_DeliveryTransfers","OBSERVED_MOVEMENT","APPLY_OBSERVED_MOVEMENT"),
        entry("mulino_inventory_Stocktakes","STOCKTAKE_OBSERVATION","RECORD_STOCKTAKE"),
        entry("mulino_inventory_StockAdjustments","QUANTITY","ADJUST_LOSS"),
        entry("mulino_inventory_ObjectRelations","RELATION","RECORD_RELATION"),
        entry("mulino_trade_receipt_Receipts","RECEIPT","RECORD_RECEIPT"),
        entry("mulino_trade_receipt_Observations","INBOX","RECEIVE_PROVISIONAL"),
        entry("mulino_trade_sales_Deliveries","DELIVERY_RECORD","RECORD_DELIVERY"),
        entry("mulino_trade_sales_DeliveryCorrections","EVIDENCE_CORRECTION","CORRECT_DELIVERY"),
        entry("mulino_trade_returns_Authorizations","RETURN_AUTHORIZATION","AUTHORIZE_RETURN"),
        entry("mulino_trade_returns_Receipts","RETURN","RECEIVE_RETURN"),
        entry("mulino_trade_settlement_Invoices","INVOICE","RECORD_INVOICE"),
        entry("mulino_trade_settlement_PaymentReferences","PAYMENT_REFERENCE","RECORD_PAYMENT_REFERENCE"),
        entry("mulino_trade_purchase_ProposalRevisions","PURCHASE_PROPOSAL","PROPOSE_PURCHASE"),
        entry("mulino_trade_purchase_SupplierReplies","SUPPLIER_REPLY","RECORD_SUPPLIER_REPLY"),
        entry("mulino_trade_purchase_Approvals","APPROVAL","APPROVE"),
        entry("mulino_trade_recall_Approvals","RECALL_APPROVAL","APPROVE_RECALL"),
        entry("mulino_trade_regulatory_ProcedureVersions","SUBMISSION_RECORD","RECORD_SUBMISSION"),
        entry("mulino_evidence_EvidenceLinks","EVIDENCE_LINK","ATTACH_EVIDENCE"),
        entry("mulino_runtime_Outbox","OUTBOX","CREATE_OUTBOX"),
        entry("mulino_definitions_WorkDefinitionMigrations","GOAL_MIGRATION","MIGRATE_GOAL"));
    // Auxiliary storage supports one primary effect. It is still inventoried and retained as causal operands.
    private static final Set<String> SUPPORT=Set.of(
        "mulino_commands_CommandRecords","mulino_commands_CommandAudits","mulino_commands_ApprovalConsumptions",
        "mulino_inventory_QuantitySegments","mulino_inventory_GenealogyEdges","mulino_inventory_CargoScopes",
        "mulino_inventory_Restrictions","mulino_inventory_DispositionBases","mulino_quality_Decisions",
        "mulino_work_read_Works","mulino_work_read_GoalReferences","mulino_work_read_AssessmentReferences","mulino_work_read_SubjectLinks","mulino_work_read_EvidenceReferences",
        "mulino_work_WorkLinks","mulino_work_WorkContributions","mulino_evaluation_InputSnapshots","mulino_evaluation_EvidenceCaptures",
        "mulino_responsibility_Roots","mulino_responsibility_Scopes","mulino_responsibility_CompletionBindings","mulino_responsibility_ResolutionCredits",
        "mulino_responsibility_ReceiptResidualRoots","mulino_responsibility_ReceiptResidualCredits","mulino_responsibility_ObservationReceiptCredits",
        "mulino_evidence_Events","mulino_evidence_Claims","mulino_evidence_InboxRecords","mulino_evidence_CanonicalOccurrences","mulino_evidence_Verifications","mulino_evidence_CompletionCoverages","mulino_evidence_Reconciliations",
        "mulino_trade_purchase_Proposals","mulino_trade_purchase_Orders","mulino_trade_purchase_OrderLines","mulino_trade_purchase_ReceiptCredits","mulino_trade_purchase_ExecutionEffects",
        "mulino_trade_sales_Orders","mulino_trade_sales_OrderRevisions","mulino_trade_sales_OrderLines","mulino_trade_sales_Observations","mulino_trade_sales_ExecutionEffects",
        "mulino_trade_returns_Observations","mulino_trade_returns_Dispositions",
        "mulino_runtime_RecoverySchedules","mulino_runtime_IntakeRecoveries","mulino_runtime_RecoveryObservationHistory",
        "mulino_trade_regulatory_Procedures","mulino_trade_regulatory_DecisionVersions","mulino_trade_regulatory_LabelVerifications");
    static ObjectNode observe(JsonNode before,JsonNode after,List<JsonNode> wires) {
        NativeClientPolicy.require(before.path("scope").equals(after.path("scope")),"EFFECT_BOUNDARY_SCOPE_MISMATCH");
        NativeClientPolicy.require(before.path("catalogEvidence").path("complete").asBoolean(false)&&after.path("catalogEvidence").path("complete").asBoolean(false),"EFFECT_CLASS_INVENTORY_INCOMPLETE");
        var out=Json.object().put("complete",true).put("status","CAUSALLY_OBSERVED");var effects=Json.array();var excluded=Json.array();var proofs=Json.array();out.set("rows",effects);out.set("unattributed",excluded);out.set("commandProofs",proofs);
        var actual=new LinkedHashMap<String,JsonNode>();
        for(JsonNode wire:wires) {
            // Scope labels alone grant no causality. Exact server response bytes and committed IDs are checked below.
            if(!wire.path("scope").equals(after.path("scope")))continue;
            NativeClientPolicy.require(wire.path("authenticatedActorStatus").asText().equals("CAPTURED")&&wire.path("serverAuthenticatedCapture").path("recordType").asText().equals("SERVER_AUTHENTICATED_MCP_CAPTURE_V1"),"EFFECT_WIRE_NOT_AUTHENTICATED");
            var server=wire.path("serverAuthenticatedCapture");NativeClientPolicy.require(server.path("requestSha256").asText().equals(NativeClientPolicy.sha(wire.path("request").toString()))&&server.path("responseSha256").asText().equals(NativeClientPolicy.sha(wire.path("response").toString()))&&server.path("rpcId").equals(wire.path("request").path("id"))&&server.path("toolName").equals(wire.path("request").path("params").path("name")),"EFFECT_WIRE_BYTES_CHANGED");
            var response=wire.path("response").path("result").path("structuredContent");if(!response.hasNonNull("commandId"))continue;
            String command=Json.required(response,"commandId");JsonNode prior=actual.putIfAbsent(command,wire);if(prior!=null)NativeClientPolicy.require(prior.path("response").path("result").path("structuredContent").equals(response),"CAPTURED_COMMAND_REPLAY_CHANGED");
        }
        var records=index(after.path("rawRows").path("mulino_commands_CommandRecords"));
        var baselineRecords=index(before.path("rawRows").path("mulino_commands_CommandRecords"));
        var commandTransactions=new HashMap<String,String>();
        for(var entry:actual.entrySet()){
            String command=entry.getKey();JsonNode wire=entry.getValue(),record=records.get(command);NativeClientPolicy.require(record!=null,"CAPTURED_COMMAND_RECORD_MISSING");
            JsonNode response=wire.path("response").path("result").path("structuredContent"),stored=Json.parse(Json.required(record,"resultJson"));
            NativeClientPolicy.require(stored.equals(response)&&record.path("state").asText().equals("COMMITTED"),"CAPTURED_COMMAND_NOT_COMMITTED");
            String xid=Json.required(stored,"transactionId");NativeClientPolicy.require(xid.equals(record.path("__xmin").asText()),"COMMAND_TRANSACTION_VERSION_CHANGED");
            var identity=wire.path("serverAuthenticatedCapture").path("identity");NativeClientPolicy.require(record.path("organizationId").equals(after.path("scope").path("organizationId"))&&record.path("actorId").equals(identity.path("actorId"))&&record.path("stableRequestOwner").equals(identity.path("stableRequestOwner")),"COMMAND_AUTHENTICATED_IDENTITY_MISMATCH");
            boolean audited=false;for(JsonNode audit:after.path("rawRows").path("mulino_commands_CommandAudits"))if(audit.path("commandId").asText().equals(command)&&audit.path("__xmin").asText().equals(xid)&&audit.path("canonicalHash").equals(record.path("canonicalHash"))&&audit.path("outcome").equals(stored.path("outcome")))audited=true;
            NativeClientPolicy.require(audited,"COMMITTED_COMMAND_AUDIT_MISSING");
            if(baselineRecords.containsKey(command))continue; // A replay cannot create this turn's new business effect.
            NativeClientPolicy.require(!commandTransactions.containsValue(xid),"COMMAND_TRANSACTION_ATTRIBUTION_AMBIGUOUS");commandTransactions.put(command,xid);
            proofs.add(Json.object().put("commandId",command).put("transactionId",xid).put("serverCaptureId",wire.path("serverAuthenticatedCapture").path("captureId").asText()).put("responseSha256",wire.path("serverAuthenticatedCapture").path("responseSha256").asText()));
        }
        for(var command:commandTransactions.entrySet()){
            JsonNode record=records.get(command.getKey());String cap=Json.required(record,"capabilityId"),xid=command.getValue();var supporting=Json.array();int primary=0;
            for(var table:NativePhysicalCatalog.TABLES){String dataset=NativePhysicalObserver.dataset(table.name());var old=index(before.path("rawRows").path(dataset),table);var rows=after.path("rawRows").path(dataset);int i=0;
                for(JsonNode row:rows){String id=identity(row,table),pointer="/rawRows/"+dataset+"/"+i++;if(!row.path("__xmin").asText().equals(xid))continue;
                    if(old.containsKey(id)){
                        if(old.get(id).equals(row))continue;
                        // Work has immutable transitions. Other current rows cannot serve as update journals.
                        if(table.name().equals("mulino_work_read_Works")||SUPPORT.contains(table.name())){supporting.add(pointer);continue;}
                        out.put("complete",false);excluded.add(Json.object().put("table",table.name()).put("rowId",id).put("reason","MUTABLE_UPDATE_HAS_NO_IMMUTABLE_HISTORY"));continue;
                    }
                    if(table.name().equals("mulino_work_WorkTransitions")){
                        var snapshot=Json.parse(Json.required(row,"snapshotJson"));NativeClientPolicy.require(snapshot.path("ID").equals(row.path("workId")),"WORK_TRANSITION_IDENTITY_MISMATCH");String op=row.path("operation").asText();if(Set.of("createDraft","createWork").contains(op)){add(effects,after,record,row,table.name(),pointer,new Meaning("WORK","CREATE_WORK"),command.getKey(),"workId");primary++;}else supporting.add(pointer);continue;
                    }
                    if(table.name().equals("mulino_work_read_ObligationReferences")){add(effects,after,record,row,table.name(),pointer,new Meaning("OBLIGATION","CREATE_OBLIGATION"),command.getKey(),"ID");primary++;continue;}
                    if(table.name().equals("mulino_inventory_SegmentAllocations")){add(effects,after,record,row,table.name(),pointer,new Meaning("ALLOCATION","CREATE_ALLOCATION"),command.getKey(),"segmentId");primary++;continue;}
                    Meaning meaning=PRIMARY.get(table.name());
                    if(table.name().equals("mulino_trade_regulatory_ProcedureVersions")){
                        if(row.path("status").asText().equals("PREPARED"))meaning=new Meaning("REGULATORY_PROCEDURE","PREPARE_REGULATORY_PROCEDURE");
                        else if(!row.path("status").asText().equals("SUBMITTED")){out.put("complete",false);excluded.add(Json.object().put("table",table.name()).put("rowId",id).put("reason","UNKNOWN_REGULATORY_VERSION_EFFECT"));continue;}
                    }
                    if(meaning!=null){
                        // Secondary evidence links belonging to a receipt/delivery are operands, not another attach command.
                        if(table.name().equals("mulino_evidence_EvidenceLinks")&&!cap.equals("attachEvidence")){supporting.add(pointer);continue;}
                        add(effects,after,record,row,table.name(),pointer,meaning,command.getKey(),"ID");primary++;
                    }else if(SUPPORT.contains(table.name()))supporting.add(pointer);
                    else {out.put("complete",false);excluded.add(Json.object().put("table",table.name()).put("rowId",id).put("reason","UNKNOWN_COMMITTED_EFFECT_CLASS"));}
                }
            }
            // A command with only auxiliary writes has an unclassified business effect, never a zero result.
            if(primary==0&&!supporting.isEmpty()){out.put("complete",false);excluded.add(Json.object().put("commandId",command.getKey()).put("reason","PRIMARY_EFFECT_UNCLASSIFIED"));}
            for(JsonNode effect:effects)if(effect.path("commandId").asText().equals(command.getKey()))((ObjectNode)effect).set("supportingRowPointers",supporting);
            // The audit is immutable and separate from business effects.
            int i=0;for(JsonNode audit:after.path("rawRows").path("mulino_commands_CommandAudits")){String pointer="/rawRows/mulino_commands_CommandAudits/"+i++;if(audit.path("commandId").asText().equals(command.getKey())&&audit.path("__xmin").asText().equals(xid))add(effects,after,record,audit,"mulino_commands_CommandAudits",pointer,new Meaning("COMMAND_AUDIT","COMMAND_AUDIT"),command.getKey(),"commandId");}
        }
        // A vanished row has no attributable immutable DELETE proof. Never certify an empty delta.
        for(var table:NativePhysicalCatalog.TABLES){String dataset=NativePhysicalObserver.dataset(table.name());var remaining=index(after.path("rawRows").path(dataset),table);for(JsonNode row:before.path("rawRows").path(dataset))if(!remaining.containsKey(identity(row,table))){out.put("complete",false);excluded.add(Json.object().put("table",table.name()).put("rowId",identity(row,table)).put("reason","DELETE_HAS_NO_IMMUTABLE_HISTORY"));}}
        // Foreign/interleaved writes remain visible evidence, with no invented ownership or counted effects.
        for(var table:NativePhysicalCatalog.TABLES){String dataset=NativePhysicalObserver.dataset(table.name());var old=index(before.path("rawRows").path(dataset),table);for(JsonNode row:after.path("rawRows").path(dataset))if(!old.containsKey(identity(row,table))&&!commandTransactions.containsValue(row.path("__xmin").asText()))excluded.add(Json.object().put("table",table.name()).put("rowId",identity(row,table)).put("reason","NO_CAPTURED_TURN_COMMAND"));}
        return out;
    }
    static List<JsonNode> wires(Path directory) throws Exception {var result=new ArrayList<JsonNode>();try(var files=Files.list(directory)){for(Path file:files.filter(p->p.getFileName().toString().startsWith("mcp-")&&p.toString().endsWith(".json")).toList()){JsonNode wire=Json.read(file);if(wire.path("evidenceClass").asText().equals("ACTUAL_MCP_TRANSPORT"))result.add(wire);}}return result;}
    private static void add(ArrayNode effects,JsonNode capture,JsonNode record,JsonNode row,String table,String pointer,Meaning meaning,String command,String target){
        String id=command+"|"+table+"|"+row.path("ID").asText()+"|"+meaning.operation();var effect=Json.object().put("id",id).put("operation",meaning.operation()).put("organizationRef",capture.path("scope").path("organizationId").asText()).put("installationId",capture.path("scope").path("installationId").asText()).put("turnId",capture.path("scope").path("turnId").asText()).put("actorRef",record.path("actorId").asText()).put("commandId",command).put("table",table).put("rowId",row.path("ID").asText()).put("rawRowPointer",pointer).put("transactionId",Json.parse(record.path("resultJson").asText()).path("transactionId").asText());
        effect.set("classes",Json.array().add(meaning.clazz()));if(row.hasNonNull(target))effect.set("targetRef",row.path(target));if(meaning.clazz().equals("OBLIGATION"))effect.set("obligationRef",row.path("ID"));
        var measurements=Json.array();if(table.equals("mulino_inventory_QuantityMovements")&&row.path("kind").asText().equals("RECEIPT")){for(String kind:List.of("INVENTORY","PHYSICAL","CONFIRMED_PHYSICAL"))measurements.add(Json.object().put("kind",kind).put("value",row.path("quantity").asText()).put("unit",row.path("unit").asText()));}effect.set("measurements",measurements);effects.add(effect);
    }
    private static Map.Entry<String,Meaning> entry(String table,String clazz,String operation){return Map.entry(table,new Meaning(clazz,operation));}
    private static Map<String,JsonNode> index(JsonNode rows){NativeClientPolicy.require(rows.isArray(),"PHYSICAL_EFFECT_DATASET_UNOBSERVED");var result=new LinkedHashMap<String,JsonNode>();for(JsonNode row:rows){String id=row.hasNonNull("ID")?row.path("ID").asText():row.toString();NativeClientPolicy.require(result.put(id,row)==null,"PHYSICAL_EFFECT_DUPLICATE_ROW");}return result;}
    private static Map<String,JsonNode> index(JsonNode rows,NativePhysicalCatalog.Table table){NativeClientPolicy.require(rows.isArray(),"PHYSICAL_EFFECT_DATASET_UNOBSERVED");var result=new LinkedHashMap<String,JsonNode>();for(JsonNode row:rows)NativeClientPolicy.require(result.put(identity(row,table),row)==null,"PHYSICAL_EFFECT_DUPLICATE_ROW");return result;}
    private static String identity(JsonNode row,NativePhysicalCatalog.Table table){if(row.hasNonNull("ID"))return row.path("ID").asText();var key=new ArrayList<String>();for(String field:table.keys())key.add(row.path(field).asText());return String.join("|",key);}
    private NativeCausalEffects() {}
}
