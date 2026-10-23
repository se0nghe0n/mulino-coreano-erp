package org.mulino.verification.modelbinding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.mulino.verification.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import static org.mulino.verification.modelbinding.PhysicalRecipeRows.*;

/** Closed semantic recipes over retained SQL operands. No expression, oracle, or expected value is accepted. */
public final class PhysicalFactRecipes {
    public static final String VERSION="1";
    public static final List<String> SOURCES=List.of(
        "verification/harness/src/main/java/org/mulino/verification/modelbinding/PhysicalFactRecipes.java",
        "verification/harness/src/main/java/org/mulino/verification/modelbinding/PhysicalRecipeRows.java",
        "verification/harness/src/main/java/org/mulino/verification/modelbinding/PhysicalAuthorization.java",
        "verification/harness/src/main/java/org/mulino/verification/modelbinding/PhysicalIntervals.java",
        "verification/harness/src/main/java/org/mulino/verification/nativeclient/NativePhysicalCatalog.java",
        "verification/harness/src/main/java/org/mulino/verification/modelbinding/PhysicalSourceProofs.java");
    public record Fact(JsonNode value,String unit,String physicalIdentity) {}
    public record Computed(List<Fact> facts,PhysicalRecipeRows rows) {}
    private static final String I="mulino_inventory_", W="mulino_work_read_", E="mulino_evidence_", T="mulino_trade_";
    private PhysicalFactRecipes() {}
    public static String recipeId(String semantic){return switch(semantic){case "state.activeChildren.total.value"->"state.activeChildren.quantities";case "state.receipt.distinctOccurrenceCount"->"state.receipt.cumulative.value";default->semantic;};}
    public static Computed compute(String semantic,JsonNode capture,JsonNode parameters) {
        final String path=recipeId(semantic);
        var r=new PhysicalRecipeRows(capture,parameters);var out=new ArrayList<Fact>();
        switch(path) {
            case "state.inventory.onHand.value","state.inventory.W.onHand.value","state.inventory.returnedOnHand.value" -> {
                String unit=r.unit();var segments=r.segments();var allowed=new HashSet<String>();
                for(JsonNode p:r.known(I+"Places"))if(r.text(p,"kind").equals("INTERNAL_STORAGE"))allowed.add(r.text(p,"ID"));
                segments=segments.stream().filter(s->allowed.contains(r.text(s,"placeId"))).toList();
                if(path.contains("returned")){var ids=new HashSet<String>();for(JsonNode q:r.temporal(T+"returns_Receipts"))ids.add(r.text(q,"segmentId"));segments=segments.stream().filter(s->ids.contains(r.text(s,"ID"))).toList();}
                quantity(out,r.sum(segments,"quantity",unit),unit);
            }
            case "state.inventory.A.allocated.value" -> {
                current(r);JsonNode a=r.bound("A",I+"QuantitySegments");String unit=r.text(a,"unit");var rows=r.known(I+"SegmentAllocations").stream().filter(x->x.path("segmentId").equals(a.path("ID"))&&Set.of("EXECUTABLE","SUSPENDED").contains(x.path("state").asText())).toList();quantity(out,r.sum(rows,"quantity",unit),unit);
            }
            case "state.activeChildren.quantities","state.activeChildren.total.value" -> {
                JsonNode parent=r.bound("A",I+"QuantitySegments");String unit=r.text(parent,"unit");Set<String> ids=new HashSet<>();for(JsonNode e:r.temporal(I+"GenealogyEdges"))if(e.path("sourceId").equals(parent.path("ID")))ids.add(r.text(e,"targetId"));
                for(JsonNode row:r.segments())if(ids.contains(r.text(row,"ID"))){require(r.text(row,"unit").equals(unit),"RECIPE_MIXED_UNITS");out.add(new Fact(r.field(row,"quantity"),unit,r.text(row,"ID")));}
            }
            case "state.allocation.quantity","state.allocation.status","state.allocation.unit","state.pick.quantity" -> {
                current(r);JsonNode a=r.selected(I+"SegmentAllocations","allocation","allocation1");String unit=r.text(a,"unit");
                if(path.equals("state.pick.quantity")){require(a.has("pickedAt"),"RECIPE_COLUMN_MISSING:pickedAt");require(a.hasNonNull("pickedAt"),"PICK_NOT_OBSERVED");quantity(out,r.decimal(a,"quantity"),unit);}
                else out.add(new Fact(r.field(a,path.endsWith("status")?"state":path.endsWith("unit")?"unit":"quantity"),unit,null));
            }
            case "state.segment.A.active" -> {JsonNode a=r.bound("A",I+"QuantitySegments");out.add(fact(r.active(a)));}
            case "state.segment.B.qc" -> out.add(fact(qc(r,r.bound("B",I+"QuantitySegments"))));
            case "state.hold.H2.status" -> {current(r);out.add(fact(r.text(r.bound("H2",I+"Restrictions"),"state")));}
            case "state.work.O1.state","state.parent.O1.state" -> out.add(fact(r.text(r.work(),"status")));
            case "state.work.O1.fulfilled" -> {var work=r.work();out.add(fact(r.text(work,"status").equals("CLOSED")&&r.text(work,"closeReason").equals("FULFILLED")));}
            case "state.currentOwnerRef","state.work.blockedOwnerRef" -> out.add(fact(r.text(r.work(),"ownerId")));
            case "state.followupWorkCount" -> {var work=r.work();Set<String> ids=new HashSet<>();for(JsonNode link:r.known("mulino_work_WorkLinks"))if(link.path("sourceWorkId").equals(work.path("ID"))&&link.path("kind").asText().equals("FOLLOWUP"))ids.add(r.text(link,"targetWorkId"));for(JsonNode row:r.works())if(ids.contains(r.text(row,"ID")))out.add(fact(r.text(row,"ID")));}
            case "state.currentObligationCount","state.currentReturnObligationCount","state.obligation.remaining.value","state.currentShortfall.value" -> {
                current(r);String unit=r.unit();var duties=r.temporal(W+"ObligationReferences").stream().filter(r::matches).filter(q->q.path("valid").asBoolean(false)&&q.path("status").asText().equals("OPEN")).toList();
                if(path.equals("state.currentReturnObligationCount"))duties=duties.stream().filter(q->q.path("kind").asText().contains("RETURN")).toList();
                if(path.endsWith("Count"))for(JsonNode duty:duties)out.add(fact(r.text(duty,"ID")));
                else {var selected=duties.stream().filter(q->q.hasNonNull("quantity")&&(!path.contains("Shortfall")||q.path("kind").asText().contains("SHORTFALL"))).toList();quantity(out,r.sum(selected,"quantity",unit),unit);}
            }
            case "state.obligation.OB1.ownerRef","state.obligation.OB1.status" -> {current(r);var row=r.bound("OB1",W+"ObligationReferences");out.add(fact(r.text(row,path.endsWith("ownerRef")?"ownerId":"status")));}
            case "state.claims.preservedCount" -> out.add(fact(r.temporal(E+"Claims").stream().filter(r::matches).count()));
            case "state.evidence.documentCount" -> {for(JsonNode d:r.known(E+"DocumentVersions"))if(r.matches(d))out.add(fact(r.text(d,"ID")));}
            case "state.receipt.cumulative.value","state.receipt.distinctOccurrenceCount" -> {
                var ranges=new ArrayList<PhysicalIntervals.Range>();String unit=r.unit();for(JsonNode receipt:r.temporal(T+"receipt_Receipts"))if(r.matches(receipt)){
                    verified(r,r.text(receipt,"canonicalOccurrenceId"));require(r.text(receipt,"unit").equals(unit),"RECIPE_MIXED_UNITS");ranges.add(new PhysicalIntervals.Range(r.text(receipt,"rangeRootId"),r.decimal(receipt,"startQuantity"),r.decimal(receipt,"quantity"),unit));}
                for(var range:PhysicalIntervals.union(ranges))out.add(new Fact(text(range.quantity()),unit,range.identity()));
            }
            case "state.receipt.totalEffectCount" -> {var ids=new HashSet<String>();for(JsonNode receipt:r.temporal(T+"receipt_Receipts"))if(r.matches(receipt)){verified(r,r.text(receipt,"canonicalOccurrenceId"));if(!r.text(receipt,"physicalEffect").equals("NONE"))ids.add(r.text(receipt,"ID"));}out.add(fact(ids.size()));}
            case "state.receipt.unit" -> out.add(fact(r.unit()));
            case "state.delivery.actual.value","state.delivery.cumulative.value","state.delivery.currentActual.value","state.delivery.historical.value" -> {
                String unit=r.unit();var rows=r.temporal(T+"sales_Deliveries").stream().filter(r::matches).toList();BigDecimal sum=BigDecimal.ZERO;
                for(JsonNode delivery:rows){verified(r,r.text(delivery,"canonicalOccurrenceId"));BigDecimal q=r.decimal(delivery,"quantity");
                    if(!path.contains("historical")){var corrections=r.temporal(T+"sales_DeliveryCorrections").stream().filter(x->x.path("deliveryId").equals(delivery.path("ID"))).toList();require(corrections.size()<=1,"DELIVERY_CORRECTION_HISTORY_AMBIGUOUS");if(!corrections.isEmpty())q=r.decimal(corrections.getFirst(),"quantity");}
                    require(r.text(delivery,"unit").equals(unit),"RECIPE_MIXED_UNITS");sum=sum.add(q);
                }quantity(out,sum,unit);
            }
            case "state.delivery.compliance","state.assessment.normalFulfillment","state.assessment.arrived.value","state.assessment.historical" -> {
                JsonNode a=assessment(r);var conditions=Json.parse(r.text(a,"conditionsJson"));
                if(path.equals("state.assessment.historical"))throw new IllegalArgumentException("HISTORICAL_ASSESSMENT_SCALAR_RECIPE_UNSPECIFIED");
                else if(path.equals("state.assessment.arrived.value")){var values=new ArrayList<JsonNode>();for(JsonNode condition:conditions)if(condition.path("kind").asText().equals("ARRIVED")||condition.path("endpoint").asText().equals("ARRIVED"))if(condition.has("actualQuantity"))values.add(condition.path("actualQuantity"));out.add(new Fact(single(values),r.unit(),null));}
                else out.add(fact(r.text(a,"outcome")));
            }
            case "state.return.authorized.value","state.return.received.value","state.return.qc" -> {
                String table=path.contains("authorized")?T+"returns_Authorizations":T+"returns_Receipts",unit=r.unit();var rows=r.temporal(table).stream().filter(r::matches).toList();
                if(path.endsWith("qc")){JsonNode receipt=single(rows);out.add(fact(qc(r,r.one(I+"QuantitySegments",r.text(receipt,"segmentId")))));}
                else quantity(out,r.sum(rows,"quantity",unit),unit);
            }
            case "state.invoice.convertedAmount","state.invoice.currency","state.invoice.kind","state.invoice.originalAmount" -> {
                var invoice=r.selected(T+"settlement_Invoices","invoice");String field=path.substring(path.lastIndexOf('.')+1);if(field.equals("kind"))field="invoiceKind";String unit=r.text(invoice,"currency");if(field.equals("convertedAmount")){String pair=r.text(invoice,"fxPair");require(pair.matches("[A-Z]{3}/[A-Z]{3}"),"FX_PAIR_INVALID");unit=pair.substring(4);}out.add(new Fact(r.field(invoice,field),unit,null));
            }
            case "state.paymentReference.amount","state.paymentReference.currency" -> {var row=r.selected(T+"settlement_PaymentReferences","paymentReference");scopedParent(r,r.one(T+"settlement_Invoices",r.text(row,"invoiceId")));out.add(new Fact(r.field(row,path.endsWith("amount")?"amount":"currency"),r.text(row,"currency"),null));}
            case "state.stocktake.observed.value" -> {var row=r.selected(I+"Stocktakes","stocktake");out.add(new Fact(r.field(row,"observedQuantity"),r.text(row,"unit"),null));}
            case "state.movement.destinationRef","state.movement.quantity" -> {var row=r.selected(I+"QuantityMovements","movement");out.add(path.endsWith("quantity")?new Fact(r.field(row,"quantity"),r.text(row,"unit"),null):fact(r.text(r.one(I+"QuantitySegments",r.text(row,"targetId")),"placeId")));}
            case "state.relation.targetRef" -> out.add(fact(r.text(r.selected(I+"ObjectRelations","relation"),"targetId")));
            case "state.supplierCommitment.status" -> {var reply=r.selected(T+"purchase_SupplierReplies","supplierReply");var line=single(r.known(T+"purchase_OrderLines").stream().filter(l->l.path("orderId").equals(reply.path("orderId"))).toList());scopedParent(r,line);verified(r,r.text(reply,"canonicalOccurrenceId"));out.add(fact(r.text(reply,"reply")));}
            case "state.regulatory.status" -> {current(r);var procedure=r.selected(T+"regulatory_Procedures","regulatory");out.add(fact(r.text(r.one(T+"regulatory_ProcedureVersions",r.text(procedure,"currentVersionId")),"status")));}
            case "state.settlementDifference.status" -> {current(r);var row=r.selected(T+"settlement_Matches","settlementDifference");scopedParent(r,r.one(T+"settlement_Invoices",r.text(row,"invoiceId")));out.add(fact(r.text(row,"status")));}
            case "state.reconciliation.status" -> out.add(fact(r.text(r.selected(E+"Reconciliations","reconciliation"),"decision")));
            case "state.recall.closed","state.contaminationConfirmed" -> {current(r);var row=r.selected(T+"recall_Investigations","recall");out.add(fact(r.text(row,path.endsWith("closed")?"status":"impactState").equals(path.endsWith("closed")?"CLOSED":"CONFIRMED")));}
            case "state.recall.distinctProcessed.value" -> {String unit=r.unit();var ranges=new ArrayList<PhysicalIntervals.Range>();for(JsonNode action:r.temporal(T+"recall_Actions"))if(r.matches(action)&&Set.of("RECOVERY","DISPOSAL","SAFE_RELEASE","CONSUMED","LOSS").contains(r.text(action,"kind")))ranges.add(new PhysicalIntervals.Range(r.text(action,"rootSegmentId"),r.decimal(action,"startQuantity"),r.decimal(action,"quantity"),r.text(action,"unit")));for(var range:PhysicalIntervals.union(ranges)){require(unit.equals(range.unit()),"RECIPE_MIXED_UNITS");out.add(new Fact(text(range.quantity()),unit,range.identity()));}}
            case "state.recall.unknown.value" -> throw new IllegalArgumentException("RECALL_PARTITION_RECIPE_REQUIRES_CLOSED_SCOPE_PROOF");
            case "state.purchaseProposal.quantity","state.purchaseProposal.destinationRef","state.purchaseProposal.dueAt","state.purchaseProposal.endpoint","state.purchaseProposal.dueInclusive","state.purchaseProposal.qcNotArrivalPrecondition","state.purchaseProposal.approvalValid" -> purchase(out,r,path);
            case "state.transport.quantity","state.transport.remaining.value" -> {String unit=r.unit();var places=new HashSet<String>();for(JsonNode p:r.known(I+"Places"))if(r.text(p,"kind").equals("TRANSIT"))places.add(r.text(p,"ID"));quantity(out,r.sum(r.segments().stream().filter(s->places.contains(r.text(s,"placeId"))).toList(),"quantity",unit),unit);}
            case "state.intake.ownerRef","state.intake.nextCheckAt","state.intake.status","state.intake.closedByNotification" -> intake(out,r,path);
            case "state.grant.readGrant.actions" -> {current(r);var grant=r.bound("readGrant","mulino_identity_Grants");var actions=Json.array();for(JsonNode action:r.rows("mulino_identity_GrantActions"))if(action.path("grantId").equals(grant.path("ID")))actions.add(r.field(action,"capabilityId"));require(!actions.isEmpty(),"GRANT_ACTIONS_UNOBSERVED");out.add(new Fact(actions,null,null));}
            case "state.currentWriteAuthorization" -> {current(r);String capability=Json.required(parameters,"capabilityId"),actor=Json.required(parameters,"actorId");out.add(fact(PhysicalAuthorization.allowed(r,actor,capability,PhysicalAuthorization.businessTargets(r.parameters.path("businessScope")),new HashSet<>())?"ALLOWED":"DENIED"));}
            default -> throw new IllegalArgumentException("UNKNOWN_PHYSICAL_RECIPE:"+path);
        }
        return new Computed(List.copyOf(out),r);
    }
    private static void current(PhysicalRecipeRows r){require(r.parameters.hasNonNull("authorityAt")&&Instant.parse(r.parameters.path("authorityAt").asText()).equals(r.knownAt)&&r.asOf.equals(r.knownAt),"MUTABLE_HISTORY_UNAVAILABLE");}
    private static void quantity(List<Fact> out,BigDecimal value,String unit){out.add(new Fact(text(value),unit,null));}
    private static TextNode text(BigDecimal q){return Json.MAPPER.getNodeFactory().textNode(q.stripTrailingZeros().toPlainString());}
    private static Fact fact(Object value){return new Fact(value instanceof Number?Json.parse(value.toString()):Json.MAPPER.valueToTree(value),null,null);}
    private static String qc(PhysicalRecipeRows r,JsonNode segment){
        current(r);
        var holds=r.known(I+"Restrictions").stream().filter(q->q.path("segmentId").equals(segment.path("ID"))&&q.path("category").asText().equals("QC")&&q.path("state").asText().equals("ACTIVE")&&at(q,"validFrom",r.asOf)&&(!q.hasNonNull("validUntil")||!at(q,"validUntil",r.asOf))).toList();
        if(!holds.isEmpty())return "HELD";
        var releases=r.known("mulino_quality_Decisions").stream().filter(q->q.path("segmentId").equals(segment.path("ID"))&&q.path("operation").asText().equals("releaseHold")).toList();
        require(!releases.isEmpty(),"QC_PASS_UNVERIFIED");return "PASSED";
    }
    private static JsonNode assessment(PhysicalRecipeRows r){var work=r.work();var rows=r.temporal(W+"AssessmentReferences").stream().filter(a->a.path("workId").equals(work.path("ID"))&&a.path("goalId").equals(work.path("currentGoalVersionId"))).toList();return single(rows.stream().filter(a->rows.stream().noneMatch(n->n.path("previousAssessmentId").equals(a.path("ID")))).toList());}
    private static void verified(PhysicalRecipeRows r,String canonical){require(PhysicalSourceProofs.verified(r,canonical,null),"SOURCE_VERIFICATION_INCOMPLETE");}
    private static void purchase(List<Fact> out,PhysicalRecipeRows r,String path){
        current(r);JsonNode proposal=r.selected(T+"purchase_Proposals","purchaseProposal");var revisions=r.temporal(T+"purchase_ProposalRevisions").stream().filter(q->q.path("proposalId").equals(proposal.path("ID"))&&q.path("revision").equals(proposal.path("currentRevision"))).toList();JsonNode rev=single(revisions);String suffix=path.substring(path.lastIndexOf('.')+1);
        if(suffix.equals("approvalValid")){
            if(!proposal.hasNonNull("currentApprovalId")){out.add(fact(false));return;}
            var approval=r.one(T+"purchase_Approvals",r.text(proposal,"currentApprovalId"));
            boolean valid=at(approval,"createdAt",r.knownAt)&&at(approval,"decidedAt",r.asOf)&&approval.path("proposalId").equals(proposal.path("ID"))&&approval.path("proposalRevision").equals(rev.path("revision"))&&approval.path("proposalHash").equals(rev.path("proposalHash"))&&approval.path("consumptionPolicy").asText().equals("SINGLE_ORDER_REVISION")&&approval.hasNonNull("validUntil")&&r.asOf.isBefore(Instant.parse(r.text(approval,"validUntil")));
            if(!valid||!Set.of("APPROVE","CONDITIONAL").contains(r.text(approval,"decision"))){out.add(fact(false));return;}
            require(!r.text(approval,"decision").equals("CONDITIONAL"),"CONDITIONAL_APPROVAL_REQUIRES_INDEPENDENT_GOAL_EVALUATION");
            var policies=r.known("mulino_governance_PolicyVersions").stream().filter(p->p.path("kind").asText().equals("COMMAND")&&at(p,"effectiveFrom",r.asOf)&&(!p.hasNonNull("effectiveUntil")||r.asOf.isBefore(Instant.parse(r.text(p,"effectiveUntil"))))).toList();
            JsonNode policy=single(policies);require(Json.sha256Text(r.text(policy,"content")).equals(r.text(policy,"contentHash"))&&Json.parse(r.text(policy,"content")).path("rules").path("dispatchPurchaseOrder").isObject(),"APPROVAL_POLICY_RECIPE_UNRESOLVED");
            var targets=Map.of("ITEM",Set.of(r.text(rev,"itemId")),"WORK",Set.of(r.text(rev,"workId")),"PLACE",Set.of(r.text(rev,"destinationId")));String approver=r.text(approval,"approverId");
            boolean human=r.text(r.one("mulino_identity_Actors",approver),"kind").equals("HUMAN");boolean managed=false;
            for(JsonNode m:r.rows("mulino_identity_ManagementAuthorities"))if(m.path("actorId").asText().equals(approver)&&m.path("capabilityId").asText().equals("approvePurchase")&&at(m,"validFrom",r.asOf)&&m.hasNonNull("validUntil")&&r.asOf.isBefore(Instant.parse(r.text(m,"validUntil")))&&(!m.hasNonNull("revokedAt")||r.asOf.isBefore(Instant.parse(r.text(m,"revokedAt"))))&&(m.path("scopeKind").asText().equals("ORGANIZATION")&&m.path("scopeId").asText().equals(r.organization)||targets.getOrDefault(m.path("scopeKind").asText(),Set.of()).contains(m.path("scopeId").asText())))managed=true;
            out.add(fact(approval.path("policyHash").equals(policy.path("contentHash"))&&human&&managed&&PhysicalAuthorization.allowed(r,approver,"approvePurchase",targets,new HashSet<>())));return;
        }
        if(Set.of("dueInclusive","qcNotArrivalPrecondition").contains(suffix)){
            var goal=r.one(W+"GoalReferences",r.text(rev,"goalVersionId"));var slots=Json.parse(r.text(goal,"slotsJson"));
            if(suffix.equals("dueInclusive")){require(slots.path("periodEndInclusive").isBoolean()&&slots.hasNonNull("periodEnd")&&Instant.parse(r.text(slots,"periodEnd")).equals(Instant.parse(r.text(rev,"dueAt"))),"GOAL_DEADLINE_POLICY_OPERAND_MISSING");out.add(new Fact(slots.path("periodEndInclusive"),null,null));return;}
            require(r.text(goal,"endpoint").equals("ARRIVED")&&slots.path("conditions").isArray()&&!slots.path("conditions").isEmpty(),"ARRIVAL_GOAL_CONDITIONS_UNOBSERVED");var definition=r.one("mulino_definitions_DefinitionVersions",r.text(goal,"definitionVersionId"));require(Json.sha256Text(r.text(definition,"content")).equals(r.text(definition,"contentHash")),"GOAL_DEFINITION_SOURCE_CHANGED");var published=Json.parse(r.text(definition,"content"));
            for(JsonNode condition:slots.path("conditions")){var matches=new ArrayList<JsonNode>();for(JsonNode template:published.path("goals"))if(template.path("name").equals(condition.path("id")))matches.add(template);var template=single(matches);var predicate=template.path("predicate");require(template.path("endpoint").asText().equals("ARRIVED")&&template.path("quantityMode").asText().equals("CUMULATIVE_EVENT")&&predicate.path("operator").asText().equals("quantitySum"),"UNKNOWN_ARRIVAL_PRECONDITION_RECIPE");var keys=new HashSet<String>();predicate.fieldNames().forEachRemaining(keys::add);require(keys.equals(Set.of("operator","property","minimum","unit","evidenceSelector"))&&predicate.path("property").asText().equals("Receipt.quantity")&&predicate.path("evidenceSelector").asText().equals("VERIFIED_DISTINCT"),"UNKNOWN_ARRIVAL_PRECONDITION_OPERAND");}
            out.add(fact(slots.path("conditions").size()>0));return;
        }
        out.add(new Fact(r.field(rev,suffix.equals("destinationRef")?"destinationId":suffix),suffix.equals("quantity")?r.text(rev,"unit"):null,null));
    }
    private static void scopedParent(PhysicalRecipeRows r,JsonNode row){for(String key:List.of("itemId","workId"))if(r.scope(key)!=null)require(row.hasNonNull(key)&&row.path(key).asText().equals(r.scope(key)),"DERIVED_PARENT_OUTSIDE_BUSINESS_SCOPE");}
    private static void intake(List<Fact> out,PhysicalRecipeRows r,String path){
        var current=r.selected("mulino_runtime_IntakeRecoveries","intake");var histories=r.known("mulino_runtime_RecoveryObservationHistory").stream().filter(h->h.path("sourceId").equals(current.path("ID"))&&h.path("sourceTable").asText().equals("mulino_runtime_intakerecoveries")).sorted(Comparator.comparingLong((JsonNode h)->Long.parseLong(h.path("ID").asText())).reversed()).toList();
        JsonNode head=histories.isEmpty()?null:histories.getFirst();require(head!=null&&head.path("historyComplete").asBoolean(false)&&head.path("scopeComplete").asBoolean(false)&&!head.path("operation").asText().equals("DELETE"),"INTAKE_HISTORY_INCOMPLETE");scopedParent(r,head);var row=Json.parse(r.text(head,"snapshotJson"));String suffix=path.substring(path.lastIndexOf('.')+1);
        if(suffix.equals("closedByNotification"))throw new IllegalArgumentException("INTAKE_CLOSURE_REASON_NOT_RECORDED");out.add(fact(r.text(row,suffix.equals("ownerRef")?"intakeownerid":suffix.equals("nextCheckAt")?"nextcheckat":"state")));
    }
    public static ObjectNode cell(String path,String dataset,String attribute,Computed computed,int index,Path root) throws Exception {
        Fact f=index<0?null:computed.facts().get(index);var recipe=Json.object().put("id",recipeId(path)).put("version",VERSION).put("snapshotId",computed.rows().capture.path("snapshot").path("id").asText());
        recipe.set("parameters",computed.rows().parameters);recipe.set("documents",computed.rows().capture.path("sourceDocuments").isArray()?computed.rows().capture.path("sourceDocuments"):Json.array());recipe.set("inputs",computed.rows().pointers());recipe.set("datasets",computed.rows().datasets());var sources=Json.array();for(String source:SOURCES)sources.add(Json.object().put("path",source).put("sha256",Json.sha256(root.resolve(source))));recipe.set("sources",sources);
        var result=Json.object().put("kind","DERIVED").put("dataset",dataset).put("attribute",attribute).put("rowId",path+":"+index).put("recipeIndex",index);result.set("value",f==null?Json.MAPPER.nullNode():f.value());result.set("recipe",recipe);if(f==null){result.put("scopeOnly",true);return result;}if(f.unit()!=null)result.put("unit",f.unit());if(f.physicalIdentity()!=null)result.put("physicalScopeId",f.physicalIdentity());return result;
    }
    public static void validate(JsonNode capture,JsonNode cell,Path root) {
        JsonNode recipe=cell.path("recipe");require(recipe.path("version").asText().equals(VERSION)&&recipe.path("snapshotId").equals(capture.path("snapshot").path("id")),"DERIVED_RECIPE_VERSION_OR_SNAPSHOT_MISMATCH");
        try{Set<String> paths=new HashSet<>();for(JsonNode source:recipe.path("sources")){String path=Json.required(source,"path");require(SOURCES.contains(path)&&paths.add(path)&&Json.sha256(root.resolve(path)).equals(source.path("sha256").asText()),"DERIVED_RECIPE_SOURCE_CHANGED");}require(paths.equals(new HashSet<>(SOURCES)),"DERIVED_RECIPE_SOURCES_INCOMPLETE");}catch(java.io.IOException error){throw new IllegalArgumentException("DERIVED_RECIPE_SOURCE_MISSING",error);}
        var inputs=(ObjectNode)capture.deepCopy();inputs.set("sourceDocuments",recipe.path("documents"));var recomputed=compute(Json.required(recipe,"id"),inputs,recipe.path("parameters"));require(recipe.path("inputs").equals(recomputed.rows().pointers())&&recipe.path("datasets").equals(recomputed.rows().datasets()),"DERIVED_RECIPE_INPUTS_INCOMPLETE");
        int index=cell.path("recipeIndex").asInt(-1);if(cell.path("scopeOnly").asBoolean(false)){require(index==-1&&recomputed.facts().isEmpty()&&cell.path("value").isNull(),"DERIVED_EMPTY_SCOPE_FORGED");return;}require(index>=0&&index<recomputed.facts().size(),"DERIVED_RECIPE_INDEX_INVALID");Fact actual=recomputed.facts().get(index);
        require(actual.value().equals(cell.path("value"))&&Objects.equals(actual.unit(),cell.hasNonNull("unit")?cell.path("unit").asText():null)&&Objects.equals(actual.physicalIdentity(),cell.hasNonNull("physicalScopeId")?cell.path("physicalScopeId").asText():null),"DERIVED_RECIPE_VALUE_CHANGED");
    }
}
