package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.mulino.verification.Json;

/** Authored synthetic source facts only. Never runs evaluation or changes a Work. */
final class S3OriginalFixtureInstaller {
    static ObjectNode install(ActualConfiguration config,JsonNode bundle) throws Exception {
        JsonNode f=bundle.path("fixture"),binding=bundle.path("binding"),occurrence=f.path("occurrence");
        if(!f.path("synthetic").asBoolean())throw new IllegalArgumentException("Synthetic evidence fixture required");
        for(String field:List.of("organizationId","workId","itemId","actorId"))UUID.fromString(Json.required(binding,field));
        String org=Json.required(binding,"organizationId"),work=Json.required(binding,"workId"),item=Json.required(binding,"itemId"),actor=Json.required(binding,"actorId");
        String rootProperty=System.getProperty("verification.actual.blobRoot");if(rootProperty==null)throw new UnsupportedOperationException("Explicit isolated backend blob root required for evidence fixture");
        Path objects=Path.of(rootProperty).toAbsolutePath().normalize().resolve("objects");
        if(!Files.isDirectory(objects)||Files.isSymbolicLink(objects)||Files.isSymbolicLink(objects.getParent()))throw new IllegalArgumentException("Actual private backend object store required");
        boolean response=false;
        JsonNode original=response?bundle.path("preparedOriginal"):null;
        if(response){UUID.fromString(Json.required(original,"dutyRootId"));for(String field:List.of("startQuantity","quantity","unit"))Json.required(original,field);}
        byte[] bytes=(response?Json.MAPPER.writeValueAsString(original):(occurrence.path("content").isObject()?occurrence.path("content").toString():Json.required(occurrence,"contentText"))).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String subjectKind=binding.path("subjectKind").asText("WORK"),subjectId=binding.path("subjectId").asText(work);
        UUID.fromString(subjectId);
        String hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
        String blob=id(),profile=id(),document=id(),event=id(),inbox=id(),claim=id(),canonical=id(),verification=id();
        String physical=binding.hasNonNull("physicalScopeId")?Json.required(binding,"physicalScopeId"):id();UUID.fromString(physical);
        Path file=objects.resolve(blob);boolean created=false,committed=false;
        OffsetDateTime recorded=OffsetDateTime.parse(Json.required(f.path("clock"),"knownAt")),effective=OffsetDateTime.parse(Json.required(f.path("clock"),"asOf"));
        String namespace=Json.required(f.path("sourceProfile"),"namespace"),policy=Json.required(f.path("sourceProfile"),"policyVersion"),kind=Json.required(occurrence,"kind"),external=Json.required(occurrence,"externalEventId");
        Object quantity=occurrence.hasNonNull("quantity")?new java.math.BigDecimal(Json.required(occurrence,"quantity")):null;String unit=occurrence.hasNonNull("unit")?Json.required(occurrence,"unit"):null;
        try(var c=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password())) {
            c.setAutoCommit(false);
            try {
                if(response)validateDuty(c,org,work,physical,original);
                try(var s=c.prepareStatement("SELECT 1 FROM mulino_work_read_Works WHERE organizationId=? AND ID=? AND itemId=?")){s.setString(1,org);s.setString(2,work);s.setString(3,item);try(var r=s.executeQuery()){if(!r.next())throw new IllegalArgumentException("Evidence fixture binding is not an installed canonical Work/item");}}
                Files.write(file,bytes,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);created=true;Files.setPosixFilePermissions(file,PosixFilePermissions.fromString("r--------"));
                boolean existingProfile=false;
                try(var statement=c.prepareStatement("SELECT ID,policyVersion,intakeOwnerId,supervisorId FROM mulino_evidence_SourceProfiles WHERE organizationId=? AND namespace=?")){statement.setString(1,org);statement.setString(2,namespace);try(var rows=statement.executeQuery()){if(rows.next()){profile=rows.getString(1);existingProfile=true;if(!policy.equals(rows.getString(2))||!actor.equals(rows.getString(3))||!Json.required(binding,"supervisorId").equals(rows.getString(4)))throw new IllegalArgumentException("Existing source identity baseline differs");}}}
                if(!existingProfile){var source=common(org,profile,actor,recorded);source.put("namespace",namespace);source.put("policyVersion",policy);source.put("intakeOwnerId",actor);source.put("supervisorId",Json.required(binding,"supervisorId"));source.put("nextAction",Json.required(f.path("sourceProfile"),"nextAction"));source.put("nextCheckAt",OffsetDateTime.parse(Json.required(f.path("sourceProfile"),"nextCheckAt")));insert(c,"mulino_evidence_SourceProfiles",source);}
                var doc=subject(org,document,actor,recorded,work,item,profile);doc.put("subjectKind",subjectKind);doc.put("subjectId",subjectId);doc.putAll(Map.of("sha256",hash,"blobId",blob,"byteLength",bytes.length,"mediaType","text/plain","sourceNamespace",namespace,"availability","AVAILABLE","provenance","{\"source\":\"authored-synthetic-native-fixture\"}"));insert(c,"mulino_evidence_DocumentVersions",doc);
                var e=subject(org,event,actor,recorded,work,item,profile);e.put("subjectKind",subjectKind);e.put("subjectId",subjectId);e.put("placeId",binding.hasNonNull("placeId")?binding.path("placeId").asText():null);temporal(e,effective);e.putAll(Map.of("kind",kind,"sourceNamespace",namespace,"externalEventId",external,"sourceVersion","1","payloadHash",hash,"payload",new String(bytes,java.nio.charset.StandardCharsets.UTF_8)));insert(c,"mulino_evidence_Events",e);
                var in=subject(org,inbox,actor,recorded,work,item,profile);in.putAll(Map.of("eventId",event,"sourceNamespace",namespace,"externalEventId",external,"sourceVersion","1","payloadHash",hash,"state","RECEIVED","intakeOwnerId",actor,"supervisorId",Json.required(binding,"supervisorId"),"nextAction",Json.required(f.path("sourceProfile"),"nextAction"),"nextCheckAt",OffsetDateTime.parse(Json.required(f.path("sourceProfile"),"nextCheckAt"))));insert(c,"mulino_evidence_InboxRecords",in);
                var cl=subject(org,claim,actor,recorded,work,item,profile);cl.put("subjectKind",subjectKind);cl.put("subjectId",subjectId);cl.put("placeId",binding.hasNonNull("placeId")?binding.path("placeId").asText():null);temporal(cl,effective);cl.putAll(Map.of("eventId",event,"documentVersionId",document,"assertion",Json.required(occurrence,"assertion")));cl.put("quantity",quantity);cl.put("unit",unit);insert(c,"mulino_evidence_Claims",cl);
                c.commit();committed=true;
            } catch(Exception failure){c.rollback();throw failure;}
        } finally {if(created&&!committed)Files.deleteIfExists(file);}
        if(!hash.equals(HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)))))throw new IllegalStateException("Committed fixture blob readback hash differs");
        var result=Json.object();result.put("committed",true).put("businessExecutionClaimed",false).put("fixtureHash",Json.required(bundle,"fixtureHash"));
        var aliases=Json.object();for(var e:Map.of("document",document,"event",event,"inbox",inbox,"claim",claim,"canonical",canonical,"verification",verification,"sourceProfile",profile,"physicalScope",physical,"blob",blob).entrySet())aliases.put(e.getKey(),e.getValue());result.set("aliasMap",aliases);
        aliases.remove("canonical");aliases.remove("verification");result.put("publication","ACTUAL_HTTP_REVIEW_AND_LINK_PENDING");
        result.put("sourceContentSha256",hash).put("blobReadbackSha256",hash).put("blobByteLength",bytes.length).put("blobFileName",blob);result.set("clock",f.path("clock"));result.set("binding",binding);return result;
    }
    private static void validateDuty(Connection c,String org,String work,String physical,JsonNode original)throws SQLException {
        String sql="SELECT root.unit,s.startQuantity,s.quantity,root.scopeJson FROM mulino_responsibility_Roots root JOIN mulino_responsibility_Scopes s ON s.organizationId=root.organizationId AND s.rootId=root.ID JOIN mulino_work_read_ObligationReferences a ON a.organizationId=s.organizationId AND a.scopeId=s.ID WHERE root.organizationId=? AND root.ID=? AND a.workId=? AND a.valid=true AND a.status='OPEN' AND s.leaf=true";
        try(var statement=c.prepareStatement(sql)){statement.setString(1,org);statement.setString(2,Json.required(original,"dutyRootId"));statement.setString(3,work);try(var rows=statement.executeQuery()){if(!rows.next())throw new IllegalArgumentException("Response original duty binding has no actual open leaf");
            if(!Objects.equals(rows.getString(1),Json.required(original,"unit"))||rows.getBigDecimal(2).compareTo(new java.math.BigDecimal(Json.required(original,"startQuantity")))!=0||rows.getBigDecimal(3).compareTo(new java.math.BigDecimal(Json.required(original,"quantity")))!=0||!physical.equals(Json.parse(rows.getString(4)).path("physicalScopeId").asText())||rows.next())throw new IllegalArgumentException("Response original does not bind exactly one actual duty range");}}
    }
    private static String id(){return UUID.randomUUID().toString();}
    private static Map<String,Object> common(String org,String id,String actor,OffsetDateTime recorded){var row=new LinkedHashMap<String,Object>();row.put("organizationId",org);row.put("ID",id);row.put("revision",1);row.put("createdAt",recorded);row.put("recordedAt",recorded);row.put("recordedBy",actor);return row;}
    private static Map<String,Object> subject(String org,String id,String actor,OffsetDateTime recorded,String work,String item,String profile){var row=common(org,id,actor,recorded);row.put("subjectKind","WORK");row.put("subjectId",work);row.put("workId",work);row.put("itemId",item);row.put("sourceProfileId",profile);return row;}
    private static void temporal(Map<String,Object> row,OffsetDateTime effective){row.put("effectiveFrom",effective);row.put("timeZone","UTC");row.put("timePrecision","SECOND");row.put("valueState","KNOWN");}
    private static void insert(Connection c,String table,Map<String,Object> row)throws SQLException{String sql="INSERT INTO "+table+"("+String.join(",",row.keySet())+") VALUES("+String.join(",",Collections.nCopies(row.size(),"?"))+")";try(var s=c.prepareStatement(sql)){int i=1;for(Object value:row.values())s.setObject(i++,value);if(s.executeUpdate()!=1)throw new SQLException("Fixture insert did not create exactly one actual row");}}
    private S3OriginalFixtureInstaller(){}
}
