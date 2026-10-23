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
    private final OffsetDateTime knownAt;
    private final ArrayNode queries = Json.array();
    private final Map<String,ArrayNode> cache = new HashMap<>();
    private ArrayNode observationQueries;

    ScenarioObservationProjection(Connection connection, Path root, List<String> orgs, List<String> items,
                                  List<String> works, List<String> lots, OffsetDateTime knownAt) {
        this.connection=connection; this.root=root; this.orgs=orgs; this.items=items;
        this.works=works; this.lots=lots; this.knownAt=knownAt;
    }

    ArrayNode drainQueries() { var copy=queries.deepCopy(); queries.removeAll(); return copy; }

    ArrayNode query(String table, String predicate, List<?> parameters) throws Exception {
        String sql="SELECT r.* FROM "+table+" r WHERE "+predicate+(ScenarioJdbcObservation.columns(connection,table).contains("id")?" ORDER BY r.ID":"");
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
        var params=new ArrayList<Object>();
        return query(table,predicate(table,ScenarioJdbcObservation.columns(connection,table),params),params);
    }

    void enrich(String table, ArrayNode rows) throws Exception {
        for(var node:rows) {
            var row=(ObjectNode)node;
            switch(table) {
                case "mulino_work_read_GoalReferences" -> {
                    var slots=parse(row,"slotsJson");field(row,"slots",slots);
                    copy(row,"intervalStart",slots,"periodStart");copy(row,"intervalEnd",slots,"periodEnd");
                    copy(row,"startInclusive",slots,"periodStartInclusive");copy(row,"endInclusive",slots,"periodEndInclusive");
                    copy(row,"evidencePolicyVersion",slots,"evidencePolicyVersion");
                }
                case "mulino_trade_purchase_Approvals" -> field(row,"conditions",parse(row,"conditionsJson"));
                case "mulino_work_read_Works" -> {
                    var wait=parse(row,"waitJson");copy(row,"waitReason",wait,"reason");
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
                    field(row,"conditionResults",conditionRows(row,parse(row,"conditionsJson")));
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

    private void version(ObjectNode row,String target,String table,String idField) throws Exception {
        var linked=linked(table,row,row.hasNonNull(idField)?row.path(idField).asText():null);
        if(linked!=null){copy(row,target,linked,"version");copy(row,target+"Hash",linked,"contentHash");}
    }

    ArrayNode virtual(String source) throws Exception {
        return switch(source) {
            case "@conditionResults" -> {
                var out=Json.array();for(var row:read("mulino_work_read_AssessmentReferences"))out.addAll(conditionRows(row,parse(row,"conditionsJson")));yield out;
            }
            case "@observationCoverage" -> verifiedObservations();
            case "@supervisionIssues" -> emptySupervisionIssues();
            case "@assessmentInputs" -> {
                var out=Json.array();for(var snapshot:read("mulino_evaluation_InputSnapshots")) {
                    var content=parse(snapshot,"contentJson");var conditions=conditionRows(snapshot,content.path("conditions"));
                    if(conditions.isEmpty()){var row=(ObjectNode)snapshot.deepCopy();field(row,"inputSnapshot",content);copy(row,"goalVersionId",snapshot,"goalId");out.add(row);}
                    else for(var condition:conditions){var row=(ObjectNode)snapshot.deepCopy();field(row,"inputSnapshot",content);copy(row,"goalVersionId",snapshot,"goalId");for(String key:List.of("conditionId","evidenceIds","inputs","result","conflict","evaluatorVersion"))copy(row,key,condition,key);out.add(row);}
                }yield out;
            }
            default -> throw new UnsupportedOperationException("No scoped virtual projection for "+source);
        };
    }

    /**
     * Absence in an organization superset proves absence in every narrower scope in this MVCC snapshot.
     * Neither recovery table stores recordedAt history. A nonempty superset cannot prove historical
     * issues (or their absence), even when its current rows have another work, state or future due time.
     */
    private ArrayNode emptySupervisionIssues() throws SQLException {
        long total=0;
        for(String table:List.of("mulino_runtime_RecoverySchedules","mulino_runtime_IntakeRecoveries")) {
            var predicate=new StringBuilder();var parameters=new ArrayList<Object>();
            in(predicate,"r.organizationId",orgs,parameters);
            String sql="SELECT count(*) FROM "+table+" r WHERE "+predicate;
            long count;
            try(var statement=connection.prepareStatement(sql)) {
                for(int i=0;i<parameters.size();i++)statement.setObject(i+1,parameters.get(i));
                try(var rows=statement.executeQuery()){rows.next();count=rows.getLong(1);}
            }
            var proof=Json.object().put("table",table).put("sql",sql).put("observedRowCount",count)
                .put("proofKind","CURRENT_SNAPSHOT_ORGANIZATION_SUPERSET_EMPTY")
                .put("note","Counts all rows of the requested organizations in the same read-only REPEATABLE_READ snapshot. Zero proves this narrower scope empty in that snapshot only; it does not reconstruct knownAt history, deleted rows, states or business issues.");
            proof.set("boundValues",Json.MAPPER.valueToTree(parameters));queries.add(proof);
            total+=count;
        }
        if(total!=0)throw new UnsupportedOperationException("Recovery organization superset is nonempty in the current snapshot; no recordedAt history proves the requested supervisionIssues scope/knownAt");
        return Json.array();
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
