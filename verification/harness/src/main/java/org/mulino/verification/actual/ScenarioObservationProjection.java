package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.mulino.verification.Json;

/** Presentation of persisted facts, never an evaluator. All joins use the caller's read-only snapshot. */
final class ScenarioObservationProjection {
    private final Connection connection;
    private final Path root;
    private final List<String> orgs, items, works, lots;
    private final OffsetDateTime knownAt, asOf;
    private final ArrayNode queries = Json.array();
    private final Map<String,ArrayNode> cache = new HashMap<>();
    private ArrayNode observationQueries;

    ScenarioObservationProjection(Connection connection, Path root, List<String> orgs, List<String> items,
                                  List<String> works, List<String> lots, OffsetDateTime knownAt) {
        this(connection,root,orgs,items,works,lots,knownAt,null);
    }
    ScenarioObservationProjection(Connection connection, Path root, List<String> orgs, List<String> items,
                                  List<String> works, List<String> lots, OffsetDateTime knownAt, OffsetDateTime asOf) {
        this.connection=connection; this.root=root; this.orgs=orgs; this.items=items;
        this.works=works; this.lots=lots; this.knownAt=knownAt; this.asOf=asOf;
    }

    ArrayNode drainQueries() { var copy=queries.deepCopy(); queries.removeAll(); return copy; }

    /** An empty quantity table still has a unit only when the requested ITEM has one actual visible unit row. */
    ObjectNode measurementMetadata() throws Exception {
        var out=Json.object();if(orgs.size()!=1||items.size()!=1)return out;
        var rows=query("mulino_inventory_TradeItems","r.organizationId=? AND r.ID=? AND r.recordedAt<=? AND r.createdAt<=?",
            List.of(orgs.getFirst(),items.getFirst(),knownAt,knownAt));
        if(rows.size()>1)throw new UnsupportedOperationException("Ambiguous ITEM measurement metadata");
        if(rows.size()==1&&rows.get(0).path("baseUnit").isTextual()&&!rows.get(0).path("baseUnit").asText().isBlank()) {
            copy(out,"unit",rows.get(0),"baseUnit");copy(out,"itemId",rows.get(0),"id");
        }
        return out;
    }

    ArrayNode query(String table, String predicate, List<?> parameters) throws Exception {
        return query(table,predicate,parameters,false);
    }

    private ArrayNode query(String table, String predicate, List<?> parameters, boolean physicalVersion) throws Exception {
        String select=physicalVersion?"r.*,r.xmin::text AS \"_tupleXmin\",pg_snapshot_xmax(pg_current_snapshot())::text AS \"_snapshotXmax\"":"r.*";
        String sql="SELECT "+select+" FROM "+table+" r WHERE "+predicate+(ScenarioJdbcObservation.columns(connection,table).contains("id")?" ORDER BY r.ID":"");
        String key=sql+parameters;
        var statement=Json.object().put("table",table).put("sql",sql);
        statement.set("boundValues",Json.MAPPER.valueToTree(parameters.stream().map(Object::toString).toList()));
        queries.add(statement);
        if(cache.containsKey(key))return cache.get(key).deepCopy();
        var rows=Json.array();
        try(var s=connection.prepareStatement(sql)) {
            for(int i=0;i<parameters.size();i++)s.setObject(i+1,parameters.get(i));
            try(var result=s.executeQuery()) {
                var metadata=result.getMetaData(); var names=ScenarioJdbcObservation.names(root,table);
                while(result.next()) {
                    var row=Json.object();
                    for(int col=1;col<=metadata.getColumnCount();col++) {
                        String label=metadata.getColumnLabel(col);String name=names.getOrDefault(label,label);
                        ScenarioJdbcObservation.put(row,"ID".equals(name)?"id":name,result.getObject(col));
                    }
                    rows.add(row);
                }
            }
        }
        cache.put(key,rows.deepCopy()); return rows;
    }

    ArrayNode commands(String org) throws Exception {
        var rows=query("mulino_commands_CommandRecords","r.organizationId=? AND r.state='COMMITTED' AND r.createdAt<=? AND r.completedAt<=?",List.of(org,knownAt,knownAt));
        var sorted=new ArrayList<JsonNode>();rows.forEach(sorted::add);
        sorted.sort(Comparator.comparing((JsonNode r)->r.path("createdAt").asText()).thenComparing(r->r.path("id").asText()));
        var out=Json.array();sorted.forEach(out::add);return out;
    }

    JsonNode linked(String table, JsonNode owner, String id) throws Exception {
        if(id==null||!owner.hasNonNull("organizationId"))return null;
        var columns=ScenarioJdbcObservation.columns(connection,table);
        String time=columns.contains("recordedat")?"recordedAt":columns.contains("createdat")?"createdAt":null;
        if(time==null)throw new UnsupportedOperationException("No observation time fence for "+table);
        var rows=query(table,"r.organizationId=? AND r.ID=? AND r."+time+"<=?",
            List.of(owner.path("organizationId").asText(),id,knownAt));
        if(rows.size()!=1)return null;
        var row=rows.get(0);
        if(table.equals("mulino_commands_CommandRecords")&&row.hasNonNull("completedAt")&&OffsetDateTime.parse(row.path("completedAt").asText()).isAfter(knownAt))return null;
        return row;
    }

    /** Work-owned rows use their actual Work relation for item/lot restrictions, never a requested value as a fact. */
    String predicate(String table, Set<String> columns, List<Object> params) {
        var sql=new StringBuilder(); in(sql,"r.organizationId",orgs,params);
        String time=columns.contains("recordedat")?"recordedAt":columns.contains("createdat")?"createdAt":null;
        if(time!=null){sql.append(" AND r.").append(time).append("<=?");params.add(knownAt);}
        if(!items.isEmpty()&&columns.contains("itemid")){sql.append(" AND ");in(sql,"r.itemId",items,params);}
        String workColumn=table.equals("mulino_work_read_Works")?"ID":columns.contains("workid")?"workId":null;
        if(!works.isEmpty()&&workColumn!=null){sql.append(" AND ");in(sql,"r."+workColumn,works,params);}
        if(!lots.isEmpty()&&columns.contains("lotid")){sql.append(" AND ");in(sql,"r.lotId",lots,params);}
        if(workColumn!=null&&((!items.isEmpty()&&!columns.contains("itemid"))||(!lots.isEmpty()&&!columns.contains("lotid")))) {
            sql.append(" AND EXISTS (SELECT 1 FROM mulino_work_read_Works w WHERE w.organizationId=r.organizationId AND w.ID=r.").append(workColumn).append(" AND w.createdAt<=?");params.add(knownAt);
            if(!items.isEmpty()){sql.append(" AND ");in(sql,"w.itemId",items,params);}
            if(!lots.isEmpty()){sql.append(" AND ");in(sql,"w.lotId",lots,params);}sql.append(")");
        }
        // Approvals have no item/work columns; the exact immutable proposal revision carries both.
        if(table.equals("mulino_trade_purchase_Approvals")&&(!items.isEmpty()||!works.isEmpty()||!lots.isEmpty())) {
            sql.append(" AND EXISTS (SELECT 1 FROM mulino_trade_purchase_ProposalRevisions p JOIN mulino_work_read_Works w ON w.organizationId=p.organizationId AND w.ID=p.workId WHERE p.organizationId=r.organizationId AND p.proposalId=r.proposalId AND p.revision=r.proposalRevision AND p.proposalHash=r.proposalHash AND p.recordedAt<=? AND w.createdAt<=?");params.add(knownAt);params.add(knownAt);
            if(!items.isEmpty()){sql.append(" AND ");in(sql,"p.itemId",items,params);}
            if(!works.isEmpty()){sql.append(" AND ");in(sql,"p.workId",works,params);}
            if(!lots.isEmpty()){sql.append(" AND ");in(sql,"w.lotId",lots,params);}sql.append(")");
        }
        // Evidence and typed decisions are scoped through the actual physical segment, where no direct lot/work exists.
        String segmentColumn=columns.contains("segmentid")?"segmentId":columns.contains("physicalscopeid")?"physicalScopeId":null;
        if(table.equals("mulino_inventory_QuantityMovements"))segmentColumn="COALESCE(r.targetId,r.sourceId)";
        if(segmentColumn==null&&Set.of("mulino_evidence_Events","mulino_evidence_Claims").contains(table))segmentColumn="subjectId";
        if(segmentColumn!=null&&((!items.isEmpty()&&!columns.contains("itemid"))||(!lots.isEmpty()&&!columns.contains("lotid")))) {
            sql.append(" AND EXISTS (SELECT 1 FROM mulino_inventory_QuantitySegments s WHERE s.organizationId=r.organizationId AND s.ID=").append(segmentColumn.startsWith("COALESCE")?segmentColumn:"r."+segmentColumn).append(" AND s.recordedAt<=?");params.add(knownAt);
            if(!items.isEmpty()){sql.append(" AND ");in(sql,"s.itemId",items,params);}
            if(!lots.isEmpty()){sql.append(" AND ");in(sql,"s.lotId",lots,params);}sql.append(")");
        }
        if(!works.isEmpty()&&workColumn==null&&Set.of("mulino_quality_Decisions","mulino_trade_regulatory_DecisionVersions","mulino_trade_regulatory_LabelVerifications").contains(table)) {
            if(columns.contains("procedureid")) {
                sql.append(" AND EXISTS (SELECT 1 FROM mulino_trade_regulatory_Procedures p WHERE p.organizationId=r.organizationId AND p.ID=r.procedureId AND p.recordedAt<=? AND ");params.add(knownAt);in(sql,"p.workId",works,params);sql.append(")");
            } else sql.append(" AND FALSE"); // no Work identity in the stored quality decision itself
        }
        return sql.toString();
    }

    ArrayNode read(String table) throws Exception {
        if(table.equals("mulino_trade_purchase_SupplierReplies"))return supplierReplies();
        var params=new ArrayList<Object>();
        return query(table,predicate(table,ScenarioJdbcObservation.columns(connection,table),params),params);
    }

    private record SupplierBinding(JsonNode order, JsonNode revision, JsonNode goal) {}

    /** Scope comes from the dispatched immutable revision, never the mutable proposal head. */
    private ArrayNode supplierReplies() throws Exception {
        String table="mulino_trade_purchase_SupplierReplies";
        var params=new ArrayList<Object>();var predicate=new StringBuilder();
        in(predicate,"r.organizationId",orgs,params);
        predicate.append(" AND r.recordedAt<=?");params.add(knownAt);
        var bindings=new HashMap<String,SupplierBinding>();
        // Validate the visible organization superset before claiming even an empty scoped result.
        // A broken link cannot safely be classified as belonging to some other requested item.
        for(var reply:query(table,predicate.toString(),params)) {
            var order=supplierLink("mulino_trade_purchase_Orders",reply,"r.ID=?",reply.path("orderId").asText());
            var revision=supplierLink("mulino_trade_purchase_ProposalRevisions",reply,
                "r.proposalId=? AND r.revision=?",order.path("proposalId").asText(),order.path("proposalRevision").asInt());
            if(!order.hasNonNull("proposalHash")||order.path("proposalHash").asText().isBlank()||!order.path("proposalHash").equals(revision.path("proposalHash"))
                ||!revision.hasNonNull("itemId")||!revision.hasNonNull("workId"))
                throw supplierUnknown("order/revision hash or scope mismatch");
            JsonNode goal=null;
            if(revision.hasNonNull("goalVersionId")) {
                goal=supplierLink("mulino_work_read_GoalReferences",reply,"r.ID=?",revision.path("goalVersionId").asText());
                var scope=parse(goal,"scopeJson");
                if(!goal.path("workId").equals(revision.path("workId"))||!scope.isObject()
                    ||(scope.has("itemId")&&!scope.path("itemId").equals(revision.path("itemId")))
                    ||(scope.has("workId")&&!scope.path("workId").equals(revision.path("workId"))))
                    throw supplierUnknown("pinned goal scope mismatch");
            }
            if(!lots.isEmpty()&&(goal==null||!parse(goal,"scopeJson").path("lotId").isTextual()
                ||parse(goal,"scopeJson").path("lotId").asText().isBlank()))
                throw supplierUnknown("no explicit immutable LOT binding");
            bindings.put(reply.path("organizationId").asText()+":"+reply.path("id").asText(),new SupplierBinding(order,revision,goal));
        }
        predicate.append(" AND EXISTS (SELECT 1 FROM mulino_trade_purchase_Orders o JOIN mulino_trade_purchase_ProposalRevisions p ON p.organizationId=o.organizationId AND p.proposalId=o.proposalId AND p.revision=o.proposalRevision AND p.proposalHash=o.proposalHash");
        if(!lots.isEmpty())predicate.append(" JOIN mulino_work_read_GoalReferences g ON g.organizationId=p.organizationId AND g.ID=p.goalVersionId AND g.workId=p.workId");
        predicate.append(" WHERE o.organizationId=r.organizationId AND o.ID=r.orderId AND o.recordedAt<=? AND o.createdAt<=? AND p.recordedAt<=?");
        params.add(knownAt);params.add(knownAt);params.add(knownAt);
        if(!items.isEmpty()){predicate.append(" AND ");in(predicate,"p.itemId",items,params);}
        if(!works.isEmpty()){predicate.append(" AND ");in(predicate,"p.workId",works,params);}
        if(!lots.isEmpty()) {
            predicate.append(" AND g.recordedAt<=? AND ");params.add(knownAt);
            in(predicate,"g.scopeJson::jsonb->>'lotId'",lots,params);
        }
        predicate.append(")");
        var rows=query(table,predicate.toString(),params);
        for(var node:rows) {
            var row=(ObjectNode)node;var binding=bindings.get(row.path("organizationId").asText()+":"+row.path("id").asText());
            copy(row,"purchaseId",row,"orderId");copy(row,"quantity",row,"proposedQuantity");
            copy(row,"itemId",binding.revision(),"itemId");copy(row,"workId",binding.revision(),"workId");
            copy(row,"proposalRevisionId",binding.revision(),"id");copy(row,"proposalHash",binding.order(),"proposalHash");
            if(binding.goal()!=null) {
                copy(row,"goalVersionId",binding.goal(),"id");
                var scope=parse(binding.goal(),"scopeJson");
                if(scope.path("lotId").isTextual())copy(row,"lotId",scope,"lotId");
            }
        }
        return rows;
    }

    private JsonNode supplierLink(String table,JsonNode reply,String predicate,Object... values) throws Exception {
        var params=new ArrayList<Object>();params.add(reply.path("organizationId").asText());params.addAll(Arrays.asList(values));
        var rows=query(table,"r.organizationId=? AND "+predicate,params);
        if(rows.size()!=1)throw supplierUnknown("missing or ambiguous "+table);
        var row=rows.get(0);
        if(!row.hasNonNull("recordedAt")||OffsetDateTime.parse(row.path("recordedAt").asText()).isAfter(knownAt)
            ||(table.endsWith("_Orders")&&(!row.hasNonNull("createdAt")||OffsetDateTime.parse(row.path("createdAt").asText()).isAfter(knownAt))))
            throw supplierUnknown("time-ineligible "+table);
        return row;
    }

    private UnsupportedOperationException supplierUnknown(String reason) {
        return new UnsupportedOperationException("Supplier commitment immutable scope/knownAt is unknown: "+reason);
    }

    void enrich(String table, ArrayNode rows) throws Exception {
        for(var node:rows) {
            var row=(ObjectNode)node;
            switch(table) {
                case "mulino_inventory_Restrictions" -> {
                    var decision=linked("mulino_quality_Decisions",row,row.path("decisionId").asText(null));
                    if(decision!=null&&requiredSame(row,decision,"segmentId","commandId")&&"placeHold".equals(decision.path("operation").asText())) {
                        copy(row,"reason",decision,"reason");copy(row,"reasonDecisionId",decision,"id");
                    }
                    var release=linked("mulino_quality_Decisions",row,row.path("releaseDecisionId").asText(null));
                    if(release!=null&&requiredSame(row,release,"segmentId")&&row.path("id").equals(release.path("restrictionId"))&&"releaseHold".equals(release.path("operation").asText()))
                        copy(row,"releaseReason",release,"reason");
                }
                case "mulino_inventory_SegmentAllocations" -> { allocationBasis(row); allocationAttribution(row); }
                case "mulino_trade_sales_Orders" -> salesOrderLines(row);
                case "mulino_work_read_GoalReferences" -> {
                    var slots=parse(row,"slotsJson");field(row,"slots",slots);
                    goalFields(row,slots);
                    copy(row,"intervalStart",slots,"periodStart");copy(row,"intervalEnd",slots,"periodEnd");
                    copy(row,"startInclusive",slots,"periodStartInclusive");copy(row,"endInclusive",slots,"periodEndInclusive");
                    copy(row,"evidencePolicyVersion",slots,"evidencePolicyVersion");
                }
                case "mulino_trade_sales_Deliveries" -> canonicalDelivery(row);
                case "mulino_trade_purchase_Approvals" -> field(row,"conditions",parse(row,"conditionsJson"));
                case "mulino_work_read_Works" -> {
                    var wait=parse(row,"waitJson");copy(row,"waitReason",wait,"reason");
                    // Names only: the current persisted wait contract owns these values.
                    copy(row,"verifierId",wait,"verifier");copy(row,"overdueAction",wait,"overdueAction");
                    // A transition reason is usable only for this exact persisted revision and state.
                    if(!row.hasNonNull("waitReason")||!row.hasNonNull("closeReason")) {
                        var matches=query("mulino_work_WorkTransitions","r.organizationId=? AND r.workId=? AND r.revision=? AND r.state=? AND r.recordedAt<=?",
                            List.of(row.path("organizationId").asText(),row.path("id").asText(),row.path("revision").asInt(),row.path("status").asText(),knownAt));
                        if(matches.size()==1) {
                            var transition=matches.get(0);var snapshot=parse(transition,"snapshotJson");
                            if(row.path("id").asText().equals(snapshot.path("ID").asText())&&row.path("revision").equals(snapshot.path("revision"))) {
                                if("WAITING".equals(row.path("status").asText()))copy(row,"waitReason",transition,"reason");
                                if("CLOSED".equals(row.path("status").asText()))copy(row,"closeReason",transition,"reason");
                                field(row,"transitionId",transition.path("id"));
                            }
                        }
                    }
                }
                case "mulino_work_read_AssessmentReferences" -> {
                    copy(row,"result",row,"outcome");copy(row,"goalVersionId",row,"goalId");
                    version(row,"definitionVersion","mulino_definitions_DefinitionVersions","definitionVersionId");
                    version(row,"policyVersion","mulino_governance_PolicyVersions","policyVersionId");
                    field(row,"conditionResults",assessmentConditions(row));
                    var snapshots=query("mulino_evaluation_InputSnapshots","r.organizationId=? AND r.assessmentId=? AND r.workId=? AND r.goalId=? AND r.contentHash=? AND r.recordedAt<=?",
                        List.of(row.path("organizationId").asText(),row.path("id").asText(),row.path("workId").asText(),row.path("goalId").asText(),row.path("inputSnapshotHash").asText(),knownAt));
                    var ids=Json.array();for(var snapshot:snapshots)ids.add(snapshot.path("id"));field(row,"inputSnapshotIds",ids);
                }
                case "mulino_evidence_Events" -> {
                    if("STATE_OBSERVATION".equals(row.path("kind").asText())) {
                        var matches=verifiedObservations();
                        var matching=new ArrayList<JsonNode>();for(var m:matches)if(row.path("id").equals(m.path("activityId")))matching.add(m);
                        if(matching.size()==1){copy(row,"quantity",matching.getFirst(),"quantity");copy(row,"unit",matching.getFirst(),"unit");copy(row,"canonicalOccurrenceId",matching.getFirst(),"canonicalOccurrenceId");}
                    }
                }
                default -> {}
            }
        }
    }

    /** The header has no delivery endpoint column. A flat endpoint requires one actual current scoped line. */
    private void salesOrderLines(ObjectNode order) throws Exception {
        if(asOf==null||!order.path("currentRevision").isIntegralNumber())return;
        var revisions=query("mulino_trade_sales_OrderRevisions","r.organizationId=? AND r.orderId=? AND r.revisionNumber=? AND r.recordedAt<=? AND r.effectiveAt<=?",
            List.of(order.path("organizationId").asText(),order.path("id").asText(),order.path("currentRevision").asInt(),knownAt,asOf));
        if(revisions.size()!=1){order.put("orderLineBinding",revisions.isEmpty()?"UNAVAILABLE":"AMBIGUOUS_REVISION");return;}
        var revision=revisions.get(0);
        if(!requiredSame(order,revision,"workId")){order.put("orderLineBinding","SCOPE_MISMATCH");return;}
        var lines=query("mulino_trade_sales_OrderLines","r.organizationId=? AND r.orderId=? AND r.revisionId=? AND r.recordedAt<=? AND r.effectiveAt<=?",
            List.of(order.path("organizationId").asText(),order.path("id").asText(),revision.path("id").asText(),knownAt,asOf));
        for(var line:lines)if(!requiredSame(order,line,"workId")||!requiredSame(revision,line,"customerId")
            ||(!items.isEmpty()&&!items.contains(line.path("itemId").asText()))) {
            order.put("orderLineBinding","SCOPE_MISMATCH");return;
        }
        copy(order,"currentRevisionId",revision,"id");field(order,"currentLines",lines);
        order.put("orderLineBinding",lines.isEmpty()?"UNAVAILABLE":lines.size()==1?"SINGLE_LINE":"MULTIPLE_LINES");
        if(lines.size()==1)copy(order,"deliveryEndpoint",lines.get(0),"deliveryEndpoint");
    }

    /** Physical tuple attribution, deliberately separate from the allocation's retained creation commandId. */
    private void allocationAttribution(ObjectNode row) throws Exception {
        row.remove(List.of("lastCommandId","transactionId"));
        var proof=Json.object().put("status","UNSUPPORTED").put("kind","POSTGRES_XMIN_CURRENT_TUPLE")
            .put("historyComplete",false).put("knownAt",knownAt.toString());
        if(asOf!=null)proof.put("asOf",asOf.toString());
        row.set("atomicAttribution",proof);
        var versions=query("mulino_inventory_SegmentAllocations","r.organizationId=? AND r.ID=?",
            List.of(row.path("organizationId").asText(),row.path("id").asText()),true);
        if(versions.size()!=1){proof.put("gap","ALLOCATION_VERSION_NOT_UNIQUE");return;}
        var version=versions.get(0);String xid=version.path("_tupleXmin").asText();
        proof.put("tupleXmin",xid).put("snapshotXmax",version.path("_snapshotXmax").asText());
        // xmin is 32-bit, and frozen old tuples can retain their original xmin. Never
        // guess its epoch. Only a cluster snapshot before its first billion XIDs is supported.
        if(!unwrappedTupleXid(xid,version.path("_snapshotXmax").asText())){proof.put("gap","XID_EPOCH_OR_AGE_UNSUPPORTED");return;}
        var candidates=query("mulino_commands_CommandRecords","r.organizationId=? AND r.xmin=?::xid",
            List.of(row.path("organizationId").asText(),xid),true);
        proof.put("candidateCount",candidates.size());
        // Count before state/time/capability filtering: excluding a second command
        // in the same changeset would manufacture uniqueness.
        if(candidates.size()!=1){proof.put("gap",candidates.isEmpty()?"NO_COMMAND_IN_TUPLE_TRANSACTION":"AMBIGUOUS_COMMAND_TRANSACTION");return;}
        var command=candidates.get(0);
        if(!"COMMITTED".equals(command.path("state").asText())){proof.put("gap","COMMAND_NOT_COMMITTED");return;}
        if(!attributionTime(command,"createdAt")||!attributionTime(command,"completedAt")){
            proof.put("gap","COMMAND_OUTSIDE_TIME_BOUNDARY");return;
        }
        var intent=parse(command,"canonicalIntentJson");var result=parse(command,"resultJson");
        String capability=command.path("capabilityId").asText();
        if(!capability.equals(intent.path("capabilityId").asText())||!"APPLIED".equals(result.path("outcome").asText())
            ||!command.path("id").equals(result.path("commandId"))||!allocationWriter(row,command,intent,result)) {
            proof.put("gap","COMMAND_EFFECT_DOES_NOT_PROVE_ALLOCATION_WRITE");return;
        }
        // A technical XID, meaningful only in this database/snapshot and supported
        // epoch window. It is not a durable business ID or a historical event ID.
        copy(row,"lastCommandId",command,"id");row.put("transactionId",xid);
        proof.put("status","PROVEN_CURRENT_TUPLE").put("commandId",command.path("id").asText());
    }

    static boolean unwrappedTupleXid(String xmin,String snapshotXmax) {
        try {
            long x=Long.parseLong(xmin),end=Long.parseLong(snapshotXmax);
            return x>=3&&x<end&&end<1_000_000_000L;
        } catch(NumberFormatException invalid){return false;}
    }

    private boolean attributionTime(JsonNode row,String field) {
        if(!row.hasNonNull(field))return false;
        try { var time=OffsetDateTime.parse(row.path(field).asText());return !time.isAfter(knownAt)&&(asOf==null||!time.isAfter(asOf)); }
        catch(java.time.format.DateTimeParseException invalid){return false;}
    }

    private boolean allocationWriter(JsonNode row,JsonNode command,JsonNode intent,JsonNode result) throws Exception {
        var slots=intent.path("slots");var effects=result.path("effects");String capability=command.path("capabilityId").asText();
        if(!Set.of("reserveQuantity","replaceAllocation","releaseAllocation").contains(capability))return false;
        if(!result.path("revision").equals(effects.path("revision")))return false;
        if(capability.equals("replaceAllocation")&&row.path("id").equals(effects.path("predecessorId"))) {
            // The old allocation is not the response row. Prove the actual named
            // predecessor and revision transition, plus the actual successor's
            // retained creation relation. A later release may change that successor's
            // current xmin/state/revision; it does not rewrite this old tuple or the
            // terminal immutable replacement result. Never reconstruct its old tuple.
            if(!"REPLACED".equals(row.path("state").asText())||!row.path("id").equals(slots.path("allocationId"))
                ||!row.path("id").equals(effects.path("fulfillmentBasis").path("replacedAllocationId"))
                ||!nextAllocationRevision(row,intent)||!effects.path("commandId").equals(command.path("id")))return false;
            var successors=query("mulino_inventory_SegmentAllocations","r.organizationId=? AND r.ID=?",
                List.of(row.path("organizationId").asText(),effects.path("ID").asText()));
            return successors.size()==1&&allocationIdentity(successors.get(0),effects)
                &&"EXECUTABLE".equals(effects.path("state").asText())
                &&requiredSame(row,effects,"organizationId","workId","orderLineId","customerId","unit");
        }
        if(!allocationEffect(row,effects))return false;
        return switch(capability) {
            case "reserveQuantity" -> row.path("commandId").equals(command.path("id"))
                &&row.path("segmentId").equals(slots.path("segmentId"))&&row.path("orderLineId").equals(slots.path("salesLineId"))
                &&"EXECUTABLE".equals(row.path("state").asText())&&row.path("revision").asInt(-1)==0;
            case "replaceAllocation" -> row.path("commandId").equals(command.path("id"))
                &&row.path("segmentId").equals(slots.path("segmentId"))&&row.hasNonNull("predecessorId")
                &&row.path("predecessorId").equals(slots.path("allocationId"))
                &&row.path("predecessorId").equals(effects.path("fulfillmentBasis").path("replacedAllocationId"))
                &&"EXECUTABLE".equals(row.path("state").asText())&&row.path("revision").asInt(-1)==0;
            case "releaseAllocation" -> row.path("id").equals(slots.path("allocationId"))
                &&row.path("id").equals(effects.path("fulfillmentBasis").path("releasedAllocationId"))
                &&"RELEASED".equals(row.path("state").asText())&&nextAllocationRevision(row,intent);
            default -> false;
        };
    }

    private static boolean nextAllocationRevision(JsonNode row,JsonNode intent) {
        return row.path("revision").canConvertToInt()&&intent.path("expectedRevision").canConvertToInt()
            &&(long)row.path("revision").asInt()==(long)intent.path("expectedRevision").asInt()+1;
    }

    private static boolean allocationEffect(JsonNode row,JsonNode effects) {
        return allocationIdentity(row,effects)&&requiredSame(row,effects,"state","revision");
    }

    private static boolean allocationIdentity(JsonNode row,JsonNode effects) {
        return row.hasNonNull("id")&&row.path("id").equals(effects.path("ID"))
            &&requiredSame(row,effects,"organizationId","commandId","segmentId","workId","orderLineId","customerId","unit")
            &&row.hasNonNull("predecessorId")==effects.hasNonNull("predecessorId")
            &&(!row.hasNonNull("predecessorId")||row.path("predecessorId").equals(effects.path("predecessorId")))
            &&decimalSame(row,effects,"quantity","startQuantity");
    }

    /** Immutable command proof belongs to this allocation, not merely to a command on the same ITEM. */
    private void allocationBasis(ObjectNode row) throws Exception {
        var command=linked("mulino_commands_CommandRecords",row,row.path("commandId").asText(null));
        if(command==null||!"COMMITTED".equals(command.path("state").asText())
            ||!Set.of("reserveQuantity","replaceAllocation").contains(command.path("capabilityId").asText()))return;
        var result=parse(command,"resultJson");var effects=result.path("effects");
        if(!"APPLIED".equals(result.path("outcome").asText())||!row.path("id").equals(effects.path("ID"))
            ||!requiredSame(row,effects,"organizationId","segmentId","orderLineId","unit")
            ||!decimalSame(row,effects,"quantity","startQuantity"))return;
        var basis=effects.path("fulfillmentBasis");
        if(basis.path("FEFOExceptionReason").isTextual()&&!basis.path("FEFOExceptionReason").asText().isBlank()) {
            copy(row,"FEFOExceptionReason",basis,"FEFOExceptionReason");copy(row,"fulfillmentBasisCommandId",command,"id");
        }
    }

    /** A persisted physical fact binding, never SELL permission or a contribution judgment. */
    private void canonicalDelivery(ObjectNode delivery) throws Exception {
        // The observer must not retain an unverified caller/column marker.
        delivery.remove(List.of("canonical","canonicalSource"));
        var occurrence=linked("mulino_evidence_CanonicalOccurrences",delivery,delivery.path("canonicalOccurrenceId").asText(null));
        var observation=linked("mulino_trade_sales_Observations",delivery,delivery.path("observationId").asText(null));
        var dispatch=linked("mulino_inventory_Dispatches",delivery,delivery.path("dispatchId").asText(null));
        if(!deliveryScope(delivery,occurrence,observation,dispatch)||!coversDelivery(occurrence,delivery)
            ||java.time.Instant.parse(delivery.path("occurredAt").asText()).isAfter(knownAt.toInstant()))return;
        var owners=query("mulino_trade_sales_Deliveries","r.organizationId=? AND r.canonicalOccurrenceId=? AND r.recordedAt<=?",
            List.of(delivery.path("organizationId").asText(),occurrence.path("id").asText(),knownAt));
        if(owners.size()!=1||!owners.get(0).path("id").equals(delivery.path("id")))return;
        if(superseded("mulino_evidence_CanonicalOccurrences",occurrence,"supersedesId"))return;
        // Reconciliation.link reuses one typed occurrence identity; competing facts remain unknown.
        var candidates=query("mulino_evidence_CanonicalOccurrences","r.organizationId=? AND r.kind=? AND r.occurrenceIdentity=? AND r.recordedAt<=?",
            List.of(delivery.path("organizationId").asText(),occurrence.path("kind").asText(),occurrence.path("occurrenceIdentity").asText(),knownAt));
        if(candidates.size()!=1||!candidates.get(0).path("id").equals(occurrence.path("id")))return;
        var verifications=query("mulino_evidence_Verifications","r.organizationId=? AND r.canonicalOccurrenceId=? AND r.recordedAt<=?",
            List.of(delivery.path("organizationId").asText(),occurrence.path("id").asText(),knownAt));
        var chains=new TreeMap<String,JsonNode>();
        for(var verification:verifications) {
            if(!"VERIFIED".equals(verification.path("verdict").asText())
                ||!List.of("sourceMatched","identityMatched","quantityMatched","timeMatched","duplicateChecked").stream()
                    .allMatch(k->verification.path(k).isBoolean()&&verification.path(k).booleanValue()))continue;
            var claim=linked("mulino_evidence_Claims",delivery,verification.path("claimId").asText(null));
            var event=claim==null?null:linked("mulino_evidence_Events",delivery,claim.path("eventId").asText(null));
            var document=linked("mulino_evidence_DocumentVersions",delivery,verification.path("basisDocumentId").asText(null));
            if(!deliverySource(delivery,occurrence,observation,claim,event,document)
                ||!coversDelivery(claim,delivery)||!coversDelivery(event,delivery)||!deliveryOriginal(event,document))continue;
            var profile=linked("mulino_evidence_SourceProfiles",delivery,event.path("sourceProfileId").asText(null));
            if(profile==null||!requiredSame(profile,verification,"policyVersion")||!profile.path("namespace").equals(event.path("sourceNamespace")))continue;
            var inbox=query("mulino_evidence_InboxRecords","r.organizationId=? AND r.sourceNamespace=? AND r.externalEventId=? AND r.sourceVersion=? AND r.recordedAt<=?",
                List.of(delivery.path("organizationId").asText(),event.path("sourceNamespace").asText(),event.path("externalEventId").asText(),event.path("sourceVersion").asText(),knownAt));
            if(inbox.size()!=1||"CONFLICT".equals(inbox.get(0).path("state").asText())
                ||!inbox.get(0).path("eventId").equals(event.path("id"))||!requiredSame(inbox.get(0),event,"payloadHash"))continue;
            if(superseded("mulino_evidence_Events",event,"supersedesId","invalidatesId")
                ||superseded("mulino_evidence_Claims",claim,"supersedesId")
                ||superseded("mulino_evidence_DocumentVersions",document,"supersedesId"))continue;
            String verificationId=verification.path("id").asText();
            if(verificationId.isBlank())continue;
            var chain=Json.object().put("verificationId",verificationId).put("claimId",claim.path("id").asText())
                .put("eventId",event.path("id").asText()).put("documentVersionId",document.path("id").asText())
                .put("sourceProfileId",profile.path("id").asText()).put("inboxId",inbox.get(0).path("id").asText());
            var prior=chains.putIfAbsent(verificationId,chain);
            if(prior!=null&&!prior.equals(chain))return; // the same original row ID cannot name conflicting proofs
        }
        // Reverification adds proof of the same fact, never another quantity row or an arbitrary first proof.
        if(!chains.isEmpty()) {
            var source=Json.object().put("canonicalOccurrenceId",occurrence.path("id").asText())
                .put("observationId",observation.path("id").asText()).put("dispatchId",dispatch.path("id").asText());
            var proofs=Json.array();chains.values().forEach(proofs::add);source.set("verificationChains",proofs);
            delivery.put("canonical",true);delivery.set("canonicalSource",source);
        }
    }

    /** Read only the explicitly configured immutable local object; unavailable bytes remain unknown. */
    static boolean deliveryOriginal(JsonNode event,JsonNode document) {
        String configured=System.getProperty("verification.actual.blobRoot");
        if(configured==null)return false;
        try {
            String id=UUID.fromString(document.path("blobId").asText()).toString();
            Path objects=Path.of(configured).toAbsolutePath().normalize().resolve("objects");
            Path file=objects.resolve(id);
            if(java.nio.file.Files.isSymbolicLink(objects)||java.nio.file.Files.isSymbolicLink(file)
                ||!java.nio.file.Files.isRegularFile(file)||java.nio.file.Files.size(file)>16*1024*1024)return false;
            byte[] bytes=java.nio.file.Files.readAllBytes(file);
            String hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
            return hash.equals(document.path("sha256").asText())&&bytes.length==document.path("byteLength").asLong(-1)
                &&Json.parse(new String(bytes,java.nio.charset.StandardCharsets.UTF_8)).equals(parse(event,"payload"));
        }catch(Exception unavailable){return false;}
    }

    static boolean deliveryScope(JsonNode d,JsonNode fact,JsonNode observation,JsonNode dispatch) {
        if(fact==null||observation==null||dispatch==null)return false;
        try {
            if(!"PHYSICAL_DELIVERY".equals(fact.path("kind").asText())||!"KNOWN".equals(fact.path("valueState").asText())
                ||!"COMPLETE".equals(fact.path("reassessmentState").asText())
                ||!d.path("canonicalOccurrenceId").equals(fact.path("id"))
                ||!d.path("observationId").equals(fact.path("physicalScopeId"))
                ||!d.path("observationId").equals(observation.path("id"))||!d.path("dispatchId").equals(dispatch.path("id"))
                ||!requiredSame(d,fact,"organizationId","workId","itemId","placeId","unit")||!decimalSame(d,fact,"quantity")
                ||!instantSame(d,"occurredAt",fact,"effectiveFrom")
                ||!requiredSame(d,observation,"organizationId","dispatchId","cargoScopeId","salesLineId","workId","customerId","itemId","lotId","rangeRootId","unit","placeId")
                ||(observation.hasNonNull("claimedRangeRootId")&&!observation.path("claimedRangeRootId").equals(d.path("rangeRootId")))
                ||!decimalSame(d,observation,"quantity","startQuantity")||!instantSame(d,"occurredAt",observation,"occurredAt")
                ||!requiredSame(d,dispatch,"organizationId","cargoScopeId","salesLineId","workId","customerId","itemId","lotId","rangeRootId","unit")
                ||!d.path("placeId").equals(dispatch.path("destinationId")))return false;
            var start=new java.math.BigDecimal(d.path("startQuantity").asText());
            var quantity=new java.math.BigDecimal(d.path("quantity").asText());
            var dispatchStart=new java.math.BigDecimal(dispatch.path("startQuantity").asText());
            return quantity.signum()>0&&start.compareTo(dispatchStart)>=0
                &&start.add(quantity).compareTo(dispatchStart.add(new java.math.BigDecimal(dispatch.path("quantity").asText())))<=0
                &&!java.time.Instant.parse(d.path("occurredAt").asText()).isBefore(java.time.Instant.parse(dispatch.path("occurredAt").asText()));
        }catch(RuntimeException invalid){return false;}
    }

    static boolean deliverySource(JsonNode d,JsonNode fact,JsonNode observation,JsonNode claim,JsonNode event,JsonNode document) {
        if(claim==null||event==null||document==null)return false;
        if(!"KNOWN".equals(claim.path("valueState").asText())||!"KNOWN".equals(event.path("valueState").asText())
            ||!"PHYSICAL_DELIVERY".equals(event.path("kind").asText())||!"DISPATCH".equals(fact.path("subjectKind").asText())
            ||!fact.path("subjectId").equals(d.path("dispatchId"))||!event.path("id").equals(observation.path("eventId"))
            ||!requiredSame(d,claim,"organizationId","itemId","workId","placeId","unit")||!decimalSame(d,claim,"quantity")
            ||!instantSame(d,"occurredAt",claim,"effectiveFrom")||!instantSame(d,"occurredAt",event,"effectiveFrom")
            ||!requiredSame(claim,fact,"subjectKind","subjectId","sourceProfileId")
            ||!requiredSame(claim,event,"organizationId","subjectKind","subjectId","sourceProfileId","itemId","workId","placeId")
            ||!requiredSame(claim,document,"organizationId","subjectKind","subjectId","sourceProfileId","itemId","workId","placeId")
            ||!claim.path("documentVersionId").equals(document.path("id"))||!"AVAILABLE".equals(document.path("availability").asText())
            ||!document.hasNonNull("blobId")||!document.path("sha256").asText().matches("[0-9a-f]{64}")
            ||!requiredSame(event,document,"sourceNamespace")
            ||!event.path("externalEventId").equals(document.path("sourceReference")))return false;
        var payload=parse(event,"payload");
        if(!payload.isObject()||payload.hasNonNull("correctionOfDeliveryId")
            ||!payload.path("deliveryEventId").equals(fact.path("occurrenceIdentity"))
            ||!payload.path("physicalScopeId").equals(d.path("observationId")))return false;
        return requiredSame(d,payload,"dispatchId","cargoScopeId","salesLineId","customerId","itemId","lotId","rangeRootId","unit","placeId")
            &&decimalSame(d,payload,"quantity","startQuantity")&&instantSame(d,"occurredAt",payload,"occurredAt");
    }

    private static boolean coversDelivery(JsonNode source,JsonNode delivery) {
        if(source==null)return false;
        try{return !source.hasNonNull("effectiveUntil")||java.time.Instant.parse(delivery.path("occurredAt").asText())
            .isBefore(java.time.Instant.parse(source.path("effectiveUntil").asText()));}
        catch(RuntimeException invalid){return false;}
    }

    private static boolean requiredSame(JsonNode a,JsonNode b,String... fields) {
        for(String field:fields)if(!a.hasNonNull(field)||!b.hasNonNull(field)
            ||a.path(field).asText().isBlank()||!a.path(field).equals(b.path(field)))return false;
        return true;
    }

    private static boolean decimalSame(JsonNode a,JsonNode b,String... fields) {
        try{for(String field:fields)if(!a.hasNonNull(field)||!b.hasNonNull(field)
            ||new java.math.BigDecimal(a.path(field).asText()).compareTo(new java.math.BigDecimal(b.path(field).asText()))!=0)return false;return true;}
        catch(RuntimeException invalid){return false;}
    }
    private static boolean instantSame(JsonNode a,String af,JsonNode b,String bf) {
        try{return java.time.Instant.parse(a.path(af).asText()).equals(java.time.Instant.parse(b.path(bf).asText()));}
        catch(RuntimeException invalid){return false;}
    }

    private void version(ObjectNode row,String target,String table,String idField) throws Exception {
        var linked=linked(table,row,row.hasNonNull(idField)?row.path(idField).asText():null);
        if(linked!=null){copy(row,target,linked,"version");copy(row,target+"Hash",linked,"contentHash");}
    }

    ArrayNode virtual(String source) throws Exception {
        return switch(source) {
            case "@conditionResults" -> {
                var out=Json.array();for(var row:read("mulino_work_read_AssessmentReferences"))out.addAll(assessmentConditions(row));yield out;
            }
            case "@observationCoverage" -> verifiedObservations();
            case "@supervisionIssues" -> supervisionIssues();
            case "@assessmentInputs" -> {
                var out=Json.array();for(var snapshot:read("mulino_evaluation_InputSnapshots")) {
                    var content=parse(snapshot,"contentJson");
                    var assessment=linked("mulino_work_read_AssessmentReferences",snapshot,snapshot.path("assessmentId").asText(null));
                    validateInputSnapshot(snapshot,content,assessment);
                    if(OffsetDateTime.parse(snapshot.path("knownAt").asText()).isAfter(knownAt))
                        throw new UnsupportedOperationException("Immutable assessment input knownAt is in the future");
                    var siblings=query("mulino_evaluation_InputSnapshots","r.organizationId=? AND r.assessmentId=? AND r.recordedAt<=?",
                        List.of(snapshot.path("organizationId").asText(),snapshot.path("assessmentId").asText(),knownAt));
                    if(siblings.size()!=1)throw new UnsupportedOperationException("Ambiguous immutable assessment input snapshot");
                    JsonNode capture=null;
                    if(snapshot.hasNonNull("evidenceCaptureId")) {
                        capture=linked("mulino_evaluation_EvidenceCaptures",snapshot,snapshot.path("evidenceCaptureId").asText());
                        validateCapture(snapshot,content,capture);
                    }
                    out.addAll(inputRows(snapshot,content,capture));
                }
                out.addAll(pendingInputMarkers());yield out;
            }
            default -> throw new UnsupportedOperationException("No scoped virtual projection for "+source);
        };
    }

    /** Pending is a Work-transition marker, not a mutation of a historical envelope. */
    private ArrayNode pendingInputMarkers() throws Exception {
        var out=Json.array();if(asOf==null)return out; // No historical state claim without the request's effective-time fence.
        for(var work:read("mulino_work_read_Works")) {
            var history=query("mulino_work_WorkTransitions","r.organizationId=? AND r.workId=? AND r.recordedAt<=? AND (r.snapshotJson::jsonb->>'effectiveAt')::timestamptz<=?",List.of(work.path("organizationId").asText(),work.path("id").asText(),knownAt,asOf));
            JsonNode latest=null;for(var row:history) {
                var state=parse(row,"snapshotJson");
                try{if(java.time.Instant.parse(state.path("effectiveAt").asText()).isAfter(asOf.toInstant()))continue;}catch(RuntimeException invalid){throw new UnsupportedOperationException("Missing effective time in pending Work history",invalid);}
                if(latest!=null&&row.path("revision").equals(latest.path("revision")))throw new UnsupportedOperationException("Ambiguous pending Work history");
                if(latest==null||row.path("revision").asInt()>latest.path("revision").asInt())latest=row;
            }
            if(latest==null)continue;var state=parse(latest,"snapshotJson");
            if(!state.path("ID").equals(work.path("id"))||!state.path("organizationId").equals(work.path("organizationId"))||!state.path("revision").equals(latest.path("revision")))throw new UnsupportedOperationException("Pending Work history scope/revision mismatch");
            if(!state.path("pendingInvalidation").asBoolean(false))continue;
            var assessments=query("mulino_work_read_AssessmentReferences","r.organizationId=? AND r.workId=? AND r.recordedAt<=?",List.of(work.path("organizationId").asText(),work.path("id").asText(),knownAt));
            var priorIds=new HashSet<String>();for(var row:assessments)if(row.hasNonNull("previousAssessmentId"))priorIds.add(row.path("previousAssessmentId").asText());
            var heads=new ArrayList<JsonNode>();for(var row:assessments)if(!priorIds.contains(row.path("id").asText()))heads.add(row);
            ObjectNode marker;
            if(heads.isEmpty()) {
                // PENDING is an actual Work transition even before the first assessment exists. This marker has no
                // assessment, condition, capture or input snapshot identity and never manufactures any of those rows.
                marker=Json.object();copy(marker,"organizationId",work,"organizationId");copy(marker,"workId",work,"id");
                if(state.hasNonNull("currentGoalVersionId")) {
                    var goal=linked("mulino_work_read_GoalReferences",work,state.path("currentGoalVersionId").asText());
                    if(goal==null||!work.path("id").equals(goal.path("workId")))throw new UnsupportedOperationException("Pending Work goal binding missing or mismatched");
                    copy(marker,"goalVersionId",goal,"id");
                }
                marker.put("assessmentBinding","UNASSESSED");
            } else {
                if(heads.size()!=1)throw new UnsupportedOperationException("Ambiguous pending assessment chain");
                var named=assessmentConditions(heads.getFirst());if(named.size()!=1)continue;
                marker=(ObjectNode)named.get(0).deepCopy();
            }
            marker.put("status","PENDING").put("rowKind","WORK_PENDING_INPUT");
            marker.set("transitionId",latest.path("id"));marker.set("pendingWorkSnapshot",parse(latest,"snapshotJson"));out.add(marker);
        }
        return out;
    }

    private ArrayNode assessmentConditions(JsonNode assessment) throws Exception {
        var snapshots=query("mulino_evaluation_InputSnapshots","r.organizationId=? AND r.assessmentId=? AND r.workId=? AND r.goalId=? AND r.contentHash=? AND r.recordedAt<=?",
            List.of(assessment.path("organizationId").asText(),assessment.path("id").asText(),assessment.path("workId").asText(),assessment.path("goalId").asText(),assessment.path("inputSnapshotHash").asText(),knownAt));
        if(snapshots.size()==1) {
            var snapshot=snapshots.get(0);var content=parse(snapshot,"contentJson");
            validateInputSnapshot(snapshot,content,assessment);
            if(content.path("publicConditions").isArray())return conditionRows(assessment,content.path("publicConditions"));
        }
        return conditionRows(assessment,parse(assessment,"conditionsJson"));
    }

    static void validateCapture(JsonNode snapshot,JsonNode envelope,JsonNode capture) throws Exception {
        if(capture==null)throw new UnsupportedOperationException("Missing shared evidence capture");
        var content=parse(capture,"contentJson");
        String hash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(capture.path("contentJson").asText().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        for(var source:content.path("facts").path("sources")) {
            var ids=new HashMap<String,JsonNode>();for(var row:source) {
                if(row.hasNonNull("organizationId")&&!row.path("organizationId").equals(snapshot.path("organizationId")))throw new UnsupportedOperationException("Foreign source in shared capture");
                if(row.hasNonNull("ID")){var previous=ids.putIfAbsent(row.path("ID").asText(),row);if(previous!=null&&!previous.equals(row))throw new UnsupportedOperationException("Conflicting immutable source identity");}
            }
        }
        if(!snapshot.path("evidenceCaptureId").equals(capture.path("id"))||!hash.equals(capture.path("contentHash").asText())
            ||!same(snapshot,capture,"organizationId","asOf","knownAt")||!content.path("boundary").equals(envelope.path("captureBoundary"))
            ||!content.path("facts").equals(envelope.path("facts"))||!content.path("boundary").path("organizationId").equals(snapshot.path("organizationId")))
            throw new UnsupportedOperationException("Shared evidence capture scope/time/hash/envelope mismatch");
    }

    /** One row per immutable source relationship, never one duplicate tuple per AST condition. */
    static ArrayNode inputRows(JsonNode snapshot,JsonNode content,JsonNode capture) {
        var authored=content.path("publicConditions");
        if(capture==null&&!authored.isArray()) {
            var conditions=conditionRows(snapshot,content.path("conditions"));var out=Json.array();
            if(conditions.isEmpty()){var row=(ObjectNode)snapshot.deepCopy();field(row,"inputSnapshot",content);copy(row,"snapshotId",snapshot,"id");row.put("status","RECORDED");out.add(row);}
            else for(var condition:conditions){var row=(ObjectNode)snapshot.deepCopy();field(row,"inputSnapshot",content);copy(row,"snapshotId",snapshot,"id");copy(row,"goalVersionId",snapshot,"goalId");row.put("status","RECORDED");for(String key:List.of("conditionId","evidenceIds","inputs","result","conflict","evaluatorVersion"))copy(row,key,condition,key);out.add(row);}
            return out;
        }
        var base=(ObjectNode)snapshot.deepCopy();field(base,"inputSnapshot",content);copy(base,"inputEnvelopeId",snapshot,"id");copy(base,"goalVersionId",snapshot,"goalId");
        field(base,"snapshotId",capture==null?snapshot.path("id"):capture.path("id"));base.put("status","RECORDED").put("rowKind","CAPTURE_SOURCE_RELATION");
        field(base,"conditions",authored);field(base,"internalDiagnostics",content.path("conditions"));
        if(authored.size()==1){var named=conditionRows(snapshot,authored).get(0);for(String key:List.of("conditionId","evidenceIds","inputs","result","conflict","evaluatorVersion"))copy(base,key,named,key);}
        var pairs=new TreeMap<String,ObjectNode>();var facts=content.path("facts");
        for(var property:facts.path("properties"))for(var fact:property)for(var movement:facts.path("sources").path("inventory.QuantityMovements")) {
            if(!fact.hasNonNull("physicalScopeId")||!fact.path("physicalScopeId").equals(fact.path("sourceId"))||!fact.path("physicalScopeId").equals(movement.path("sourceId")))continue;
            boolean referenced=false;for(var ref:fact.path("evidenceRefs"))if(ref.equals(movement.path("ID")))referenced=true;
            if(!referenced||!snapshot.path("organizationId").equals(movement.path("organizationId")))continue;
            try{if(java.time.Instant.parse(movement.path("recordedAt").asText()).isAfter(java.time.Instant.parse(snapshot.path("knownAt").asText()))||java.time.Instant.parse(fact.path("recordedAt").asText()).isAfter(java.time.Instant.parse(snapshot.path("knownAt").asText())))throw new UnsupportedOperationException("Future source in immutable input capture");}
            catch(java.time.format.DateTimeParseException invalid){throw new UnsupportedOperationException("Missing source observation time in immutable input capture",invalid);}
            var pair=Json.object();pair.set("segmentId",fact.path("physicalScopeId"));pair.set("movementId",movement.path("ID"));
            pairs.put(pair.toString(),pair);
        }
        var out=Json.array();
        if(pairs.isEmpty()){base.set("sourceRelations",Json.array());out.add(base);}
        else for(var pair:pairs.values()){var row=base.deepCopy();row.set("segmentId",pair.path("segmentId"));row.set("movementId",pair.path("movementId"));row.set("sourceRelations",Json.array().add(pair));out.add(row);}
        return out;
    }

    /** Independently reconstruct the latest committed source state known at the requested instant. */
    private ArrayNode supervisionIssues() throws Exception {
        String history="mulino_runtime_RecoveryObservationHistory";
        var organization=new StringBuilder();var orgParams=new ArrayList<Object>();
        in(organization,"r.organizationId",orgs,orgParams);
        var incompleteParams=new ArrayList<Object>(orgParams);incompleteParams.add(knownAt);
        var incomplete=query(history,organization+" AND (NOT r.historyComplete OR (NOT r.scopeComplete AND r.recordedAt<=?))",incompleteParams);
        // An old installation may contain rows with no INSERT history. Never backdate their current state.
        for(String table:List.of("mulino_runtime_RecoverySchedules","mulino_runtime_IntakeRecoveries")) {
            // Preserve exact non-time JSON values and key coverage; normalize only the known
            // timestamptz fields whose text depends on the writer/reader session TimeZone.
            String timeKeys=table.endsWith("RecoverySchedules")?"ARRAY['nextcheckat','nextvalidityboundary','lastcheckedat']":"ARRAY['nextcheckat','leaseexpiresat']";
            String snapshotMatches="(h.snapshotJson::jsonb-"+timeKeys+")=(to_jsonb(r)-"+timeKeys+") AND "+"(SELECT array_agg(key ORDER BY key) FROM jsonb_object_keys(h.snapshotJson::jsonb) AS snapshot_keys(key))=(SELECT array_agg(key ORDER BY key) FROM jsonb_object_keys(to_jsonb(r)) AS current_keys(key)) AND jsonb_populate_record(NULL::"+table+",h.snapshotJson::jsonb) IS NOT DISTINCT FROM r";
            String predicate=organization+" AND NOT EXISTS (SELECT 1 FROM "+history+" h WHERE h.organizationId=r.organizationId AND h.sourceTable=? AND h.sourceId=r.ID AND h.operation<>'DELETE' AND "+snapshotMatches+" AND NOT EXISTS (SELECT 1 FROM "+history+" later WHERE later.organizationId=h.organizationId AND later.sourceTable=h.sourceTable AND later.sourceId=h.sourceId AND later.ID>h.ID))";
            var params=new ArrayList<Object>(orgParams);params.add(table.toLowerCase(Locale.ROOT));
            var missing=query(table,predicate,params);
            if(!missing.isEmpty())throw new UnsupportedOperationException("Recovery organization superset is nonempty without complete source history; requested supervisionIssues scope/knownAt is unknown");
        }
        if(!incomplete.isEmpty())
            throw new UnsupportedOperationException("Recovery historical source or immutable scope binding is incomplete; supervisionIssues is unknown");
        var params=new ArrayList<Object>(orgParams);
        var predicate=new StringBuilder(organization).append(" AND r.recordedAt<=?");params.add(knownAt);
        predicate.append(" AND NOT EXISTS (SELECT 1 FROM ").append(history).append(" later WHERE later.organizationId=r.organizationId AND later.sourceTable=r.sourceTable AND later.sourceId=r.sourceId AND later.recordedAt<=? AND (later.recordedAt>r.recordedAt OR (later.recordedAt=r.recordedAt AND later.ID>r.ID)))");params.add(knownAt);
        predicate.append(" AND r.operation<>'DELETE'");
        if(!items.isEmpty()){predicate.append(" AND ");in(predicate,"r.itemId",items,params);}
        if(!works.isEmpty()){predicate.append(" AND ");in(predicate,"r.workId",works,params);}
        if(!lots.isEmpty()){predicate.append(" AND ");in(predicate,"r.lotId",lots,params);}
        predicate.append(" AND ((r.sourceTable='mulino_runtime_recoveryschedules' AND r.snapshotJson::jsonb->>'recoverystatus'='HELD_MANUAL') OR (r.sourceTable='mulino_runtime_intakerecoveries' AND r.snapshotJson::jsonb->>'state' IN ('HELD_POLICY','HELD_LINK','HELD_MANUAL')))");
        var out=Json.array();
        for(var historyRow:query(history,predicate.toString(),params)) {
            var stored=Json.parse(historyRow.path("snapshotJson").asText());var row=Json.object();
            String table=historyRow.path("sourceTable").asText();var names=ScenarioJdbcObservation.names(root,table);
            for(var it=stored.fields();it.hasNext();){var field=it.next();String name=names.getOrDefault(field.getKey(),field.getKey());row.set(name.equals("ID")?"id":name,field.getValue());}
            row.set("status",row.path(table.endsWith("recoveryschedules")?"recoveryStatus":"state"));
            if(table.endsWith("intakerecoveries"))row.set("ownerId",row.path("intakeOwnerId"));
            for(String field:List.of("workId","itemId","lotId","recordedAt","transactionId"))row.set(field,historyRow.path(field));
            row.set("historyId",historyRow.path("id"));row.put("sourceTable",table);out.add(row);
        }
        return out;
    }

    /** Verified DB identity chain; no claim payload alone can certify a state observation or quantity. */
    private ArrayNode verifiedObservations() throws Exception {
        if(cache.containsKey("verifiedObservations")){queries.addAll(observationQueries);return cache.get("verifiedObservations").deepCopy();}
        int firstQuery=queries.size();
        var out=Json.array();
        for(var event:read("mulino_evidence_Events")) {
            if(!"STATE_OBSERVATION".equals(event.path("kind").asText())||!"SEGMENT".equals(event.path("subjectKind").asText()))continue;
            var payload=parse(event,"payload");if(!payload.isObject())continue;
            var claims=query("mulino_evidence_Claims","r.organizationId=? AND r.eventId=? AND r.recordedAt<=?",
                List.of(event.path("organizationId").asText(),event.path("id").asText(),knownAt));
            for(var claim:claims) {
                var verifications=query("mulino_evidence_Verifications","r.organizationId=? AND r.claimId=? AND r.recordedAt<=?",
                    List.of(event.path("organizationId").asText(),claim.path("id").asText(),knownAt));
                for(var verification:verifications) {
                    if(!"VERIFIED".equals(verification.path("verdict").asText())||!List.of("sourceMatched","identityMatched","quantityMatched","timeMatched","duplicateChecked").stream().allMatch(k->verification.path(k).isBoolean()&&verification.path(k).booleanValue()))continue;
                    var occurrence=linked("mulino_evidence_CanonicalOccurrences",event,verification.path("canonicalOccurrenceId").asText(null));
                    var document=linked("mulino_evidence_DocumentVersions",event,verification.path("basisDocumentId").asText(null));
                    if(!works.isEmpty()&&(!works.contains(claim.path("workId").asText())||occurrence==null||!works.contains(occurrence.path("workId").asText())))continue;
                    if(occurrence==null||document==null||!"AVAILABLE".equals(document.path("availability").asText())||!document.hasNonNull("blobId"))continue;
                    var segment=linked("mulino_inventory_QuantitySegments",event,event.path("subjectId").asText(null));
                    if(segment==null||!"KNOWN".equals(event.path("valueState").asText())||!"KNOWN".equals(claim.path("valueState").asText())||!"KNOWN".equals(occurrence.path("valueState").asText())
                        ||!same(event,claim,"subjectKind","subjectId","itemId","placeId")||!same(event,occurrence,"subjectKind","subjectId","itemId","placeId")
                        ||!claim.path("documentVersionId").equals(document.path("id"))
                        ||!same(claim,occurrence,"quantity","unit","effectiveFrom","effectiveUntil")
                        ||!event.path("subjectId").equals(occurrence.path("physicalScopeId"))||!event.path("subjectId").equals(payload.path("segmentId"))
                        ||!same(segment,event,"itemId","placeId")||!same(segment,occurrence,"quantity","unit")
                        ||!same(event,document,"sourceProfileId")||!event.path("externalEventId").equals(document.path("sourceReference")))continue;
                    if(!same(payload,occurrence,"quantity","unit","placeId")||!"STATE_OBSERVATION".equals(payload.path("activityKind").asText())||!"STATE_OBSERVATION".equals(occurrence.path("kind").asText()))continue;
                    try {
                        var start=java.time.Instant.parse(payload.path("coverageStart").asText());var end=java.time.Instant.parse(payload.path("coverageEnd").asText());
                        if(end.isBefore(start)||!start.equals(java.time.Instant.parse(occurrence.path("effectiveFrom").asText())))continue;
                        if(start.equals(end)?occurrence.hasNonNull("effectiveUntil"):!end.plusNanos(1000).equals(java.time.Instant.parse(occurrence.path("effectiveUntil").asText())))continue;
                    }catch(RuntimeException invalid){continue;}
                    if(superseded("mulino_evidence_Events",event,"supersedesId", "invalidatesId")||superseded("mulino_evidence_Claims",claim,"supersedesId")||superseded("mulino_evidence_CanonicalOccurrences",occurrence,"supersedesId")||superseded("mulino_evidence_DocumentVersions",document,"supersedesId"))continue;
                    var row=occurrence.deepCopy();((ObjectNode)row).put("activityId",event.path("id").asText()).put("canonicalOccurrenceId",occurrence.path("id").asText()).put("claimId",claim.path("id").asText()).put("verificationId",verification.path("id").asText()).put("documentVersionId",document.path("id").asText());
                    copy((ObjectNode)row,"start",payload,"coverageStart");copy((ObjectNode)row,"end",payload,"coverageEnd");out.add(row);
                }
            }
        }
        observationQueries=Json.array();for(int i=firstQuery;i<queries.size();i++)observationQueries.add(queries.get(i).deepCopy());
        cache.put("verifiedObservations",out.deepCopy());return out;
    }

    private boolean superseded(String table,JsonNode row,String... columns) throws Exception {
        var parts=new ArrayList<String>();for(String column:columns)parts.add("r."+column+"=?");
        var params=new ArrayList<Object>();params.add(row.path("organizationId").asText());params.add(knownAt);for(String ignored:columns)params.add(row.path("id").asText());
        return !query(table,"r.organizationId=? AND r.recordedAt<=? AND ("+String.join(" OR ",parts)+")",params).isEmpty();
    }

    /** Explicit inverse of persisted Goal slots; quantity is the target, never an evaluated amount. */
    static void goalFields(ObjectNode row,JsonNode slots) {
        for(String key:List.of("placeId","periodStart","periodEnd"))copy(row,key,slots,key);
        copy(row,"quantity",row,"targetQuantity");
        copy(row,"evaluationTime",slots,"evaluationAt");
        copy(row,"eligibilityAction",slots,"action");
        copy(row,"includeReservations",slots,"includeReserved");
        copy(row,"distinctContributionScope",slots,"contributionScope");
        if(slots.path("eventKind").isTextual())field(row,"eventKinds",Json.array().add(slots.path("eventKind")));
    }

    /** No mutable facts are joined into an immutable assessment input. */
    static void validateInputSnapshot(JsonNode snapshot,JsonNode content,JsonNode assessment) throws Exception {
        String hash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
            .digest(snapshot.path("contentJson").asText().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        if(assessment==null||!snapshot.hasNonNull("id")||!content.isObject()
            ||!hash.equals(snapshot.path("contentHash").asText())
            ||!snapshot.path("contentHash").equals(assessment.path("inputSnapshotHash"))
            ||!same(snapshot,assessment,"organizationId","workId","goalId","asOf","knownAt")
            ||!snapshot.path("assessmentId").equals(assessment.path("id"))
            ||!snapshot.path("goalId").equals(content.path("goalId"))
            ||!parse(assessment,"conditionsJson").equals(content.path("conditions")))
            throw new UnsupportedOperationException("Immutable assessment input identity/hash/conditions/time binding is unknown");
        var goal=content.path("goal");
        if(!goal.isMissingNode()&&(!same(snapshot,goal,"organizationId","workId")
            ||!snapshot.path("goalId").equals(goal.path("ID"))
            ||!parse(goal,"slotsJson").equals(content.path("goalSlots"))))
            throw new UnsupportedOperationException("Immutable assessment input Goal binding is unknown");
    }

    static ArrayNode conditionRows(JsonNode owner,JsonNode conditions) {
        if(conditions.isObject())conditions=conditions.path("conditions");var out=Json.array();if(!conditions.isArray())return out;
        for(var condition:conditions)if(condition.isObject()) {
            var row=(ObjectNode)condition.deepCopy();bind(row,"assessmentId",owner,owner.has("assessmentId")?"assessmentId":"id");
            bind(row,"workId",owner,"workId");bind(row,"organizationId",owner,"organizationId");
            copy(row,"evaluatorVersion",owner,"evaluatorVersion");copy(row,"result",condition.path("truth"),"state");copy(row,"conflict",condition.path("truth"),"conflict");
            copy(row,"evidenceIds",condition,"evidenceRefs");out.add(row);
        }
        return out;
    }

    static JsonNode parse(JsonNode row,String field) {
        var value=row.path(field);if(value.isContainerNode())return value;
        if(value.isTextual())try{var parsed=Json.parse(value.textValue());return parsed==null?Json.MAPPER.nullNode():parsed;}catch(IllegalArgumentException invalid){return Json.MAPPER.nullNode();}
        return Json.MAPPER.nullNode();
    }
    private static void bind(ObjectNode target,String name,JsonNode owner,String source) {
        if(!owner.hasNonNull(source))return;
        if(target.hasNonNull(name)&&!target.path(name).equals(owner.path(source)))throw new IllegalArgumentException("Stored condition "+name+" does not match its parent");
        target.set(name,owner.path(source));
    }
    static void copy(ObjectNode target,String name,JsonNode source,String field){field(target,name,source.path(field));}
    static void field(ObjectNode target,String name,JsonNode value){if(!target.hasNonNull(name)&&!value.isMissingNode())target.set(name,value.deepCopy());}
    private static boolean same(JsonNode a,JsonNode b,String... fields){for(String field:fields)if(!a.has(field)||!b.has(field)||!a.path(field).equals(b.path(field)))return false;return true;}
    private static void in(StringBuilder sql,String column,List<String> ids,List<Object> params){sql.append(column).append(" IN (").append(String.join(",",Collections.nCopies(ids.size(),"?"))).append(")");params.addAll(ids);}
}
