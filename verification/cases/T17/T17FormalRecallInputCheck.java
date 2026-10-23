package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.mulino.verification.*;
import java.math.BigDecimal;
import java.net.http.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** 한 fixture의 실제 HTTP·PostgreSQL 계약 검사이며 scenario/coverage 실행기가 아니다. */
public final class T17FormalRecallInputCheck {
    static final String SID="late-restriction-actual-delivery", WORK="t17FormalRecallWork";
    final Path root,out; final ActualConfiguration config;
    final Map<String,JsonNode> results=new LinkedHashMap<>();
    final ObjectNode report=Json.object();
    ObjectNode fixture,aliases,installed; Connection db; String org;
    int checks=0,sequence=0;
    T17FormalRecallInputCheck(Path root,Path out){this.root=root;this.out=out;config=ActualConfiguration.environment(System.getenv());}
    void check(boolean yes,String message){checks++;if(!yes)throw new AssertionError(message);}
    void equal(Object expected,Object actual,String message){check(Objects.equals(expected,actual),message+": "+expected+" != "+actual);}
    void decimal(String expected,String actual,String message){check(new BigDecimal(expected).compareTo(new BigDecimal(actual))==0,message+": "+actual);}
    String id(String alias){return aliases.path(alias).asText();}
    JsonNode response(String action){return results.get(action).path("response");}
    ObjectNode query(String identity,ObjectNode scope){var q=Json.object().put("id",identity).put("asOf","2026-10-07T09:00:00Z").put("knownAt","2026-10-07T09:00:01Z");q.set("scope",scope);return q;}
    JsonNode gateway(String name,String actor,String capability,ObjectNode request,boolean query)throws Exception {
        var credential=(ObjectNode)fixture.path("actors").path(actor).deepCopy();credential.put("organizationAlias",installed.path("organizationExternalAlias").asText());
        String category=query?"queries":"RECORD".equals(request.path("intentKind").asText())?"records":"commands";
        var http=HttpClient.newHttpClient().send(HttpRequest.newBuilder(config.baseUri().resolve("/api/ontology/"+category+"/"+capability))
            .header("Content-Type","application/json").header("Authorization","Bearer "+new JwtSigner(config).sign(credential))
            .POST(HttpRequest.BodyPublishers.ofString(request.toString())).build(),HttpResponse.BodyHandlers.ofString());
        var artifact=Json.object().put("httpStatus",http.statusCode()).put("actor",actor).put("capability",capability);artifact.set("request",request);
        JsonNode result=Json.parse(http.body());artifact.set("response",result);Json.write(out.resolve(String.format("%03d-%s.json",++sequence,name)),artifact);
        equal(200,http.statusCode(),name+" HTTP");return result;
    }
    int count(String table)throws Exception {try(var s=db.prepareStatement("SELECT count(*) FROM "+table+" WHERE organizationId=?")){s.setString(1,org);try(var r=s.executeQuery()){r.next();return r.getInt(1);}}}
    void sql(String query,String...args)throws Exception {try(var s=db.prepareStatement(query)){for(int i=0;i<args.length;i++)s.setString(i+1,args[i]);s.executeUpdate();}}
    ObjectNode rows(String table)throws Exception {
        var data=Json.object();var rows=data.putArray("rows");try(var s=db.prepareStatement("SELECT * FROM "+table+" WHERE organizationId=? ORDER BY ID")){
            s.setString(1,org);try(var r=s.executeQuery()){var m=r.getMetaData();while(r.next()){var row=rows.addObject();for(int i=1;i<=m.getColumnCount();i++){Object v=r.getObject(i);if(v==null)row.putNull(m.getColumnLabel(i));else row.put(m.getColumnLabel(i),v.toString());}}}}
        return data;
    }
    ObjectNode domainSnapshot()throws Exception {
        var snapshot=Json.object();for(String table:List.of("mulino_trade_recall_Investigations","mulino_trade_recall_Scopes","mulino_trade_recall_Approvals","mulino_trade_recall_Actions","mulino_trade_recall_Closures",
            "mulino_inventory_Restrictions","mulino_responsibility_Roots","mulino_responsibility_Scopes","mulino_work_read_ObligationReferences",
            "mulino_inventory_QuantitySegments","mulino_inventory_QuantityMovements","mulino_inventory_SegmentAllocations","mulino_trade_sales_Deliveries","mulino_runtime_Outbox"))snapshot.set(table,rows(table));return snapshot;
    }
    ObjectNode retry(ObjectNode request){var copy=request.deepCopy();copy.put("commandIdempotencyKey",UUID.randomUUID().toString());return copy;}
    void reject(String name,ObjectNode request,String code)throws Exception {
        var before=domainSnapshot();var result=gateway(name,"admin","openInvestigation",request,false);
        equal(code,result.at("/error/code").asText(),name+" exact error");check(!"APPLIED".equals(result.path("outcome").asText()),name+" outcome");
        var after=domainSnapshot();equal(before,after,name+" domain/outbox rollback");
        var proof=Json.object().put("name",name).put("error",code).put("unchanged",before.equals(after));proof.set("before",before);proof.set("after",after);
        Json.write(out.resolve(name+"-rollback.json"),proof);
    }
    void negatives(ObjectNode request)throws Exception {
        equal(0,count("mulino_trade_recall_Investigations"),"placeHold alone creates no Investigation");
        String grant;try(var s=db.prepareStatement("SELECT ID FROM mulino_identity_Grants WHERE organizationId=? AND actorId=?")){s.setString(1,org);s.setString(2,id("admin"));try(var r=s.executeQuery()){check(r.next(),"admin grant exists");grant=r.getString(1);check(!r.next(),"one admin grant");}}
        for(String dimension:List.of("WORK","PLACE")){
            sql("INSERT INTO mulino_identity_GrantScopes(organizationId,grantId,scopeKind,scopeId) VALUES(?,?,?,?)",org,grant,dimension,UUID.randomUUID().toString());
            try{reject("all-of-wrong-"+dimension,retry(request),"FORBIDDEN");}
            finally{sql("DELETE FROM mulino_identity_GrantScopes WHERE organizationId=? AND grantId=? AND scopeKind=?",org,grant,dimension);}
        }
        sql("DELETE FROM mulino_identity_GrantActions WHERE organizationId=? AND grantId=? AND capabilityId='getEvidence'",org,grant);
        try{reject("missing-current-evidence-authority",retry(request),"FORBIDDEN");}
        finally{sql("INSERT INTO mulino_identity_GrantActions(organizationId,grantId,capabilityId) VALUES(?,?,'getEvidence')",org,grant);}
        for(String variant:List.of("document-is-not-event","range-overflow","wrong-unit","future-occurrence","expired-next-check")){
            var changed=retry(request);var slots=(ObjectNode)changed.path("slots");
            switch(variant){case "document-is-not-event"->slots.put("basisEvidenceId",response("late-hold-source-document").path("id").asText());
                case "range-overflow"->slots.put("quantity","31");case "wrong-unit"->slots.put("unit","KG");
                case "future-occurrence"->slots.put("occurredAt","2026-10-07T09:00:01Z");
                case "expired-next-check"->slots.put("nextCheckAt","2026-10-07T09:00:00Z");}
            reject(variant,changed,variant.equals("document-is-not-event")?"FORBIDDEN":"TYPE_INVALID");
        }
    }
    void investigationProof()throws Exception {
        var work=response("formal-recall-work").path("data");equal(id(WORK),work.path("ID").asText(),"public Work identity");
        for(var pair:Map.of("kind","RECALL","status","ACTIVE","ownerId",id("admin"),"supervisorId",id("supervisor"),"itemId",id("P"),"lotId",id("L")).entrySet())equal(pair.getValue(),work.path(pair.getKey()).asText(),"Work "+pair.getKey());
        var event=response("formal-recall-basis").path("data");equal(response("late-hold-source-event").path("id").asText(),event.path("ID").asText(),"public Event source identity");
        var investigation=response("formal-recall-investigation").path("data");
        equal(response("open-recall-investigation").at("/effects/investigationId").asText(),investigation.path("ID").asText(),"public Investigation identity");
        equal("CANDIDATE",investigation.path("impactState").asText(),"candidate is not confirmed contamination");
        equal("INVESTIGATING",investigation.path("status").asText(),"formal investigation state");check(!investigation.path("contaminationConfirmed").asBoolean(),"no fabricated contamination");
        equal(id(WORK),investigation.path("workId").asText(),"Investigation Work");equal(event.path("ID"),investigation.path("basisEvidenceId"),"Event basis");
        equal(response("late-hold-source-scope").at("/data/ID"),investigation.path("segmentId"),"actual transit root");
        decimal("0",investigation.path("startQuantity").asText(),"range start");decimal("30",investigation.path("quantity").asText(),"range size");equal("BOX",investigation.path("unit").asText(),"range unit");
        var dbRows=rows("mulino_trade_recall_Investigations").path("rows");equal(1,dbRows.size(),"one DB Investigation");var stored=dbRows.get(0);
        for(String field:List.of("ID","itemId","lotId","segmentId","workId","basisEvidenceId","ownerId","supervisorId","status","impactState","nextAction"))equal(investigation.path(field).asText(),stored.path(field.toLowerCase(Locale.ROOT)).asText(),"public/DB Investigation "+field);
        try(var s=db.prepareStatement("SELECT occurredAt,nextCheckAt,quantity,startQuantity FROM mulino_trade_recall_Investigations WHERE organizationId=?")){s.setString(1,org);try(var r=s.executeQuery()){check(r.next(),"DB Investigation times");equal(java.time.Instant.parse("2026-10-07T09:00:00Z"),r.getTimestamp(1).toInstant(),"occurredAt");equal(java.time.Instant.parse("2026-10-07T10:00:00Z"),r.getTimestamp(2).toInstant(),"nextCheckAt");decimal("30",r.getBigDecimal(3).toPlainString(),"DB quantity");decimal("0",r.getBigDecimal(4).toPlainString(),"DB start");}}
        try(var s=db.prepareStatement("SELECT count(*) FROM mulino_evidence_Events WHERE organizationId=? AND ID=? AND itemId=?")){s.setString(1,org);s.setString(2,event.path("ID").asText());s.setString(3,id("P"));try(var r=s.executeQuery()){r.next();equal(1,r.getInt(1),"basis is a real ITEM P Event");}}
        try(var s=db.prepareStatement("SELECT c.ID,v.canonicalOccurrenceId,v.basisDocumentId,v.verdict,o.quantity,o.unit FROM mulino_evidence_Claims c JOIN mulino_evidence_Verifications v ON v.organizationId=c.organizationId AND v.claimId=c.ID JOIN mulino_evidence_CanonicalOccurrences o ON o.organizationId=v.organizationId AND o.ID=v.canonicalOccurrenceId WHERE c.organizationId=? AND c.eventId=?")){
            s.setString(1,org);s.setString(2,event.path("ID").asText());try(var r=s.executeQuery()){check(r.next(),"genuine Event has canonical source proof");equal(response("late-hold-source-event").path("claimId").asText(),r.getString(1),"actual source Claim");equal(response("late-hold-source-canonical").path("id").asText(),r.getString(2),"actual source CanonicalOccurrence");equal(response("late-hold-source-document").path("id").asText(),r.getString(3),"actual source Document");equal("VERIFIED",r.getString(4),"actual verification verdict");decimal("30",r.getBigDecimal(5).toPlainString(),"canonical basis quantity30");equal("BOX",r.getString(6),"canonical basis BOX");check(!r.next(),"one exact canonical chain");}}
        try(var s=db.prepareStatement("SELECT root.sourceId,root.scopeJson,d.ownerId,d.supervisorId,d.workId,d.status,d.nextAction,d.nextCheckAt FROM mulino_responsibility_Roots root JOIN mulino_work_read_ObligationReferences d ON d.organizationId=root.organizationId AND d.rootId=root.ID AND d.valid=true WHERE root.organizationId=? AND root.kind='RECALL_INVESTIGATION'")){
            s.setString(1,org);try(var r=s.executeQuery()){check(r.next(),"formal recall root exists");var scope=Json.parse(r.getString(2));equal(investigation.path("ID").asText(),scope.path("domainSourceId").asText(),"root source is actual Investigation");equal(id("admin"),r.getString(3),"ADMIN owner");equal(id("supervisor"),r.getString(4),"human supervisor");equal(id(WORK),r.getString(5),"root responsible Work");equal("OPEN",r.getString(6),"root OPEN");equal("미확인 실물과 회수 근거를 확인한다",r.getString(7),"original nextAction");equal(java.time.Instant.parse("2026-10-07T10:00:00Z"),r.getTimestamp(8).toInstant(),"root nextCheck");
                try(var command=db.prepareStatement("SELECT capabilityId FROM mulino_commands_CommandRecords WHERE organizationId=? AND ID=?")){command.setString(1,org);command.setString(2,r.getString(1));try(var c=command.executeQuery()){check(c.next(),"root immutable command source");equal("openInvestigation",c.getString(1),"real gateway command");}}check(!r.next(),"exactly one formal recall root assignment");}}
        equal(0,count("mulino_trade_recall_Approvals"),"no invented approval");equal(0,count("mulino_trade_recall_Actions"),"no invented notice/recovery");equal(0,count("mulino_trade_recall_Closures"),"no invented closure");
        Json.write(out.resolve("formal-recall-domain-proof.json"),domainSnapshot());
    }
    void run()throws Exception {
        fixture=(ObjectNode)Json.read(root.resolve("verification/cases/T17/fixtures/"+SID+".json"));var bundle=Json.object().put("fixtureHash",HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(fixture.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8))));bundle.set("fixture",fixture);
        bundle=(ObjectNode)ActualFixtureBindings.bind(root,bundle,config);fixture=(ObjectNode)bundle.path("fixture");installed=ScenarioFixtureInstaller.install(config,root,bundle,true);aliases=(ObjectNode)installed.path("aliasMap");org=id("ORG");Json.write(out.resolve("installation.json"),installed);
        db=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password());
        ScenarioRevisionBinding.reset(List.of(org));
        try {
            equal(0,count("mulino_trade_recall_Investigations"),"fixture installs no Investigation");
            JsonNode sub=null;for(var s:Json.read(root.resolve("verification/cases/T17/case.json")).path("subcases"))if(SID.equals(s.path("id").asText()))sub=s;check(sub!=null,"owned subcase exists");
            var resolver=new ReferenceResolver(results,aliases);
            for(var action:sub.path("actions")){
                String name=action.path("id").asText(),kind=action.path("kind").asText();var wrapped=Json.object().put("driverStatus","EXECUTED");wrapped.set("provenance",Json.object().put("scopeComplete",true).put("source","FOCUSED_HTTP_POSTGRES"));
                if(kind.equals("installFixture"))wrapped.set("response",installed);
                else if(kind.equals("observe")){
                    var observation=(ObjectNode)resolver.resolve(action.path("observation"));
                    String issuing=action.at("/observation/snapshotRef/$result/actionId").asText();
                    observation.put("snapshotRef","RESULT_REVISION");
                    for(var source:sub.path("actions"))if(issuing.equals(source.path("id").asText())){
                        var binding=Json.object().put("actionId",issuing);
                        for(String key:List.of("kind","route","capabilityId","actorRef"))binding.set(key,source.path(key));
                        observation.set("snapshotSource",binding);
                    }
                    var data=ScenarioJdbcObservation.capture(config,root,observation);ScenarioJdbcObservation.bindArtifact(data,observation.path("sources"),name+".json");wrapped.set("data",data);
                    equal(response(issuing).path("snapshotRevision"),data.path("snapshotRevision"),name+" independently bound query/DB snapshot");
                } else {
                    var request=(ObjectNode)resolver.resolve(action.path("request"));
                    if(name.equals("open-recall-investigation"))negatives(request);
                    String before=kind.equals("query")?ScenarioRevisionBinding.digest(config):null;
                    var result=gateway(name,action.path("actorRef").asText(),action.path("capabilityId").asText(),request,kind.equals("query"));wrapped.set("response",result);
                    if(before!=null)ScenarioRevisionBinding.bind(config,name,before,result.path("snapshotRevision").asText());
                    if(!kind.equals("query"))equal(name.equals("delivery-provisional")?"ACCEPTED_PENDING_EXTERNAL":"APPLIED",result.path("outcome").asText(),name+" outcome "+result);
                }
                results.put(name,wrapped);Json.write(out.resolve(name+".json"),wrapped);
            }
            investigationProof();
            var delivery=response("delivery");decimal("20",delivery.at("/effects/physicalQuantity").asText(),"actual confirmed delivery20");decimal("0",delivery.at("/effects/contributedQuantity").asText(),"recognized0 under restriction");
            var assertions=report.putArray("originalAssertions");int failed=0;
            for(var a:sub.path("assertions")){var result=assertions.addObject().put("id",a.path("id").asText());var engine=new AssertionEngine();try{engine.check(a,results,aliases);result.put("status","PASS");}catch(AssertionError e){result.put("status","FAIL").put("reason",e.getMessage());failed++;}if(engine.observedValue()!=null)result.set("observed",engine.observedValue());}
            report.put("scope","FOCUSED_HTTP_POSTGRES_NOT_SCENARIO_ACCEPTANCE").put("checks",checks).put("originalAssertions",assertions.size()).put("originalAssertionFailures",failed);
            report.set("assertionResults",assertions);Json.write(out.resolve("focused-result.json"),report);
            equal(0,failed,"unchanged original late recall assertions");
            System.out.println("Focused formal recall checks="+checks+", original assertions="+assertions.size()+"; no scenario acceptance claimed");
        } finally {Json.write(out.resolve("final-domain-snapshot.json"),domainSnapshot());db.close();}
    }
    public static void main(String[] args)throws Exception {new T17FormalRecallInputCheck(Path.of(args[0]),Path.of(args[1])).run();}
}
