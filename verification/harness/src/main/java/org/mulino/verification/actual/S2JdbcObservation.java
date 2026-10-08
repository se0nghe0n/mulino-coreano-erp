package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.mulino.verification.Json;

/** Explicit canonical tables only, independently read in one PostgreSQL RR transaction. */
final class S2JdbcObservation {
    private record Source(String table,String columns,String workColumn,String timeColumn) {Source(String table,String columns,String workColumn){this(table,columns,workColumn,"effectiveAt");}}
    private static final Map<String,Source> SOURCES=Map.ofEntries(
        Map.entry("dutyRoots",new Source("mulino_responsibility_Roots","ID,revision,sourceId,sourceKind,sourceVersion,kind,scopeJson,quantity,unit,recordedAt",null,null)),
        Map.entry("dutyScopes",new Source("mulino_responsibility_Scopes","ID,revision,rootId,parentScopeId,startQuantity,quantity,leaf,scopeJson,unit,recordedAt",null,null)),
        Map.entry("completionCoverages",new Source("mulino_evidence_CompletionCoverages","ID,revision,occurrenceId,claimId,eventId,verificationId,documentVersionId,rootId,startQuantity,quantity,unit,originalHash,sourcePayloadHash,policyVersion,recordedAt",null,null)),
        Map.entry("resolutionCredits",new Source("mulino_responsibility_ResolutionCredits","ID,revision,bindingId,rootId,scopeId,assignmentId,startQuantity,quantity,recordedAt",null,null)),
        Map.entry("canonicalOccurrences",new Source("mulino_evidence_CanonicalOccurrences","ID,revision,workId,itemId,kind,physicalScopeId,quantity,unit,valueState,effectiveFrom,recordedAt,reassessmentState","workId","effectiveFrom")),
        Map.entry("documents",new Source("mulino_evidence_DocumentVersions","ID,revision,workId,itemId,sha256,blobId,byteLength,availability,sourceNamespace,sourceProfileId,recordedAt","workId",null)),
        Map.entry("assessmentInputs",new Source("mulino_evaluation_InputSnapshots","ID,assessmentId,workId,goalId,recordedAt,asOf,knownAt,contentHash,contentJson","workId","asOf")),
        Map.entry("works",new Source("mulino_work_read_Works","ID,revision,itemId,lotId,definitionVersionId,kind,status,ownerId,supervisorId,waitJson,currentGoalVersionId,closeReason,pendingInvalidation,lifecycleMode","ID")),
        Map.entry("goals",new Source("mulino_work_read_GoalReferences","ID,revision,workId,definitionVersionId,quantityMode,targetQuantity,unit,endpoint,scopeJson,slotsJson,provenanceJson,evidencePolicyVersion,timezone,dueAt,previousGoalId,goalVersion","workId")),
        Map.entry("assessments",new Source("mulino_work_read_AssessmentReferences","ID,revision,workId,goalId,outcome,evaluatorVersion,assessedAt,conditionsJson,definitionVersionId,policyVersionId,knownAt,asOf,previousAssessmentId,inputSnapshotHash,deadlineViolated,held,conflict","workId")),
        Map.entry("obligations",new Source("mulino_work_read_ObligationReferences","ID,revision,workId,kind,status,ownerId,supervisorId,nextAction,nextCheckAt,quantity,unit,scopeJson,rootId,scopeId,valid,evidenceId,basis,predecessorId","workId")),
        Map.entry("assignments",new Source("mulino_work_read_ObligationReferences","ID,revision,workId,kind,status,ownerId,supervisorId,nextAction,nextCheckAt,quantity,unit,scopeJson,rootId,scopeId,valid,evidenceId,basis,predecessorId","workId")),
        Map.entry("definitions",new Source("mulino_definitions_DefinitionVersions","ID,revision,version,state,contentHash,content,evaluatorVersion,schemaVersion",null)),
        Map.entry("policies",new Source("mulino_governance_PolicyVersions","ID,revision,version,kind,content,contentHash,effectiveFrom,effectiveUntil",null)),
        Map.entry("grants",new Source("mulino_identity_Grants","ID,revision,actorId,delegatorId,validFrom,validUntil,revokedAt",null)),
        Map.entry("capabilityAssignments",new Source("mulino_identity_CapabilityAssignments","ID,revision,actorId,capabilityId,scopeKind,scopeId,validFrom,validUntil,revokedAt",null)));
    static ObjectNode capture(ActualConfiguration config,JsonNode request) throws Exception {
        String readMode=ObserverSnapshot.readMode(request);
        if(request.path("sources").isEmpty())throw new IllegalArgumentException("Raw sources required");
        for(JsonNode source:request.path("sources"))if(!SOURCES.containsKey(source.asText()))throw new UnsupportedOperationException("Raw source "+source.asText()+" not mapped");
        JsonNode scope=request.path("scope");UUID.fromString(Json.required(scope,"organizationId"));
        for(var fields=scope.fieldNames();fields.hasNext();) {String field=fields.next();if(!Set.of("organizationId","itemId","lotId","workId","caseId").contains(field))throw new UnsupportedOperationException("Unsupported raw scope "+field);if(!field.equals("caseId"))UUID.fromString(Json.required(scope,field));}
        var raw=Json.object();var queries=Json.array();String mvcc;
        try(var c=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password())) {
            c.setAutoCommit(false);c.setReadOnly(true);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            mvcc=ObserverSnapshot.token(c);
            for(JsonNode requested:request.path("sources")) {
                String name=requested.asText();Source source=SOURCES.get(name);List<Object> parameters=new ArrayList<>();parameters.add(scope.path("organizationId").asText());
                String select=Arrays.stream(source.columns().split(",")).map(column->"r."+column+" AS \""+(column.equals("ID")?"id":column)+"\"").collect(java.util.stream.Collectors.joining(","));
                String sql="SELECT "+select+" FROM "+source.table()+" r WHERE r.organizationId=?";
                if(Set.of("dutyRoots","dutyScopes","completionCoverages","resolutionCredits").contains(name)) {
                    String rootColumn=name.equals("dutyRoots")?"ID":"rootId";
                    sql+=" AND EXISTS(SELECT 1 FROM mulino_work_read_ObligationReferences a JOIN mulino_work_read_Works w ON w.organizationId=a.organizationId AND w.ID=a.workId WHERE a.organizationId=r.organizationId AND a.rootId=r."+rootColumn;
                    for(String field:List.of("workId","itemId","lotId"))if(scope.hasNonNull(field)){sql+=" AND "+(field.equals("workId")?"a.workId":"w."+field)+"=?";parameters.add(scope.path(field).asText());}sql+=") AND r.recordedAt<=?";parameters.add(OffsetDateTime.parse(Json.required(request,"knownAt")));
                }
                if(source.workColumn()!=null) {
                    if(scope.hasNonNull("workId")){sql+=" AND r."+source.workColumn()+"=?";parameters.add(scope.path("workId").asText());}
                    if(scope.hasNonNull("itemId")||scope.hasNonNull("lotId")) {
                        sql+=" AND EXISTS(SELECT 1 FROM mulino_work_read_Works w WHERE w.organizationId=r.organizationId AND w.ID=r."+source.workColumn();
                        for(String field:List.of("itemId","lotId"))if(scope.hasNonNull(field)){sql+=" AND w."+field+"=?";parameters.add(scope.path(field).asText());}sql+=")";
                    }
                    if(source.timeColumn()!=null){sql+=" AND r."+source.timeColumn()+"<=?";parameters.add(OffsetDateTime.parse(Json.required(request,"asOf")));}sql+=" AND r.recordedAt<=?";parameters.add(OffsetDateTime.parse(Json.required(request,"knownAt")));
                }
                sql+=" ORDER BY r.ID";var rows=Json.array();
                try(var s=c.prepareStatement(sql)) {for(int i=0;i<parameters.size();i++)s.setObject(i+1,parameters.get(i));try(var result=s.executeQuery()){while(result.next()){var row=Json.object();var metadata=result.getMetaData();for(int col=1;col<=metadata.getColumnCount();col++){String label=metadata.getColumnLabel(col);Object value=result.getObject(col);if(value==null)row.putNull(label);else if(value instanceof java.math.BigDecimal decimal)row.put(label,decimal.stripTrailingZeros().toPlainString());else if(value instanceof Number number)row.set(label,Json.MAPPER.valueToTree(number));else if(value instanceof Boolean bool)row.put(label,bool);else row.put(label,value.toString());}rows.add(row);}}}
                raw.set(name,rows);var query=Json.object();query.put("source",name).put("statementId","s2-"+name+"-v1").put("sql",sql).put("mappingVersion","1.0.0");var boundParameters=Json.object();boundParameters.set("boundValues",Json.MAPPER.valueToTree(parameters.stream().map(Object::toString).toList()));query.set("parameters",boundParameters);queries.add(query);
            }c.commit();
        }
        var result=Json.object();result.put("snapshotRevision",mvcc).put("asOf",Json.required(request,"asOf")).put("knownAt",Json.required(request,"knownAt")).put("scopeComplete",true);result.set("scope",scope);var sourceEvidence=Json.object();for(JsonNode query:queries){var individual=(ObjectNode)query.deepCopy();String name=Json.required(individual,"source");individual.remove("source");var evidence=Json.object();evidence.put("complete",true).put("rowPointer","/rawRows/"+name);evidence.set("sourceQuery",individual);sourceEvidence.set(name,evidence);}result.set("sourceEvidence",sourceEvidence);result.set("sourceQuery",sourceEvidence.path(request.path("sources").get(0).asText()).path("sourceQuery"));result.set("rawRows",raw);result.set("data",Json.object());
        result.set("snapshot",ObserverSnapshot.snapshot(mvcc,readMode,request));return result;
    }
    private S2JdbcObservation() {}
}
