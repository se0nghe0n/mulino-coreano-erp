package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.mulino.verification.Json;
import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;

/** Calls the compiled scenario installer mapping. JDBC capture is not DB/scenario acceptance. */
public final class T17SourceProfileMappingCheck {
    private static Object field(Object target, String name) throws Exception {
        var f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);
    }
    private static void set(Object target,String name,Object value)throws Exception {
        var f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);
    }
    private static void call(Object target,String name)throws Exception {
        var m=target.getClass().getDeclaredMethod(name);m.setAccessible(true);m.invoke(target);
    }
    private static void require(boolean truth,String reason){if(!truth)throw new AssertionError(reason);}
    @SuppressWarnings("unchecked")
    private static void check(Path root, JsonNode fixture, boolean old) throws Exception {
        var bundle=Json.object();bundle.set("fixture",fixture);
        var ctor=ScenarioFixtureInstaller.class.getDeclaredConstructor(ActualConfiguration.class,Path.class,JsonNode.class,boolean.class);
        ctor.setAccessible(true);Object installer=ctor.newInstance(null,root,bundle,true);
        var aliases=(ObjectNode)field(installer,"aliases");
        fixture.path("aliases").fieldNames().forEachRemaining(k->aliases.put(k,UUID.nameUUIDFromBytes(k.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString()));
        fixture.path("actors").fieldNames().forEachRemaining(k->aliases.put(k,UUID.nameUUIDFromBytes(k.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString()));
        aliases.put("supervisor",UUID.nameUUIDFromBytes("supervisor".getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString());
        set(installer,"defaultOrgAlias","ORG");
        set(installer,"asOf",OffsetDateTime.parse(fixture.path("clock").path("asOf").asText()));
        var rows=new ArrayList<Map<String,Object>>();
        Connection capture=(Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(p,m,args)->{
            if(m.getName().equals("prepareStatement")){
                String sql=(String)args[0];require(sql.startsWith("INSERT INTO mulino_evidence_SourceProfiles("),"Unexpected table: "+sql);
                String[] columns=sql.substring(sql.indexOf('(')+1,sql.indexOf(')')).split(",");
                var row=new LinkedHashMap<String,Object>();
                return Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),new Class[]{PreparedStatement.class},(pp,mm,aa)->{
                    if(mm.getName().equals("setObject")){row.put(columns[(Integer)aa[0]-1],aa[1]);return null;}
                    if(mm.getName().equals("executeUpdate")){rows.add(row);return 1;}
                    if(mm.getName().equals("close"))return null;
                    throw new AssertionError("Unexpected JDBC statement operation "+mm.getName());
                });
            }
            throw new AssertionError("Unexpected JDBC connection operation "+m.getName());
        });
        set(installer,"c",capture);call(installer,"sourceProfiles");
        var omitted=(List<String>)field(installer,"omitted");
        int count=fixture.path("baseline").path("sourceProfiles").size();
        if(old){require(rows.isEmpty(),"Old profiles must remain omitted");require(omitted.size()==count,"All old namespaces omitted");}
        else {
            require(omitted.isEmpty(),"Corrected profile omitted: "+omitted);require(rows.size()==count,"One row per original namespace");
            for(int i=0;i<count;i++){
                var profile=fixture.path("baseline").path("sourceProfiles").get(i);var row=rows.get(i);
                require(row.get("namespace").equals(profile.path("sourceNamespace").asText()),"Namespace changed");
                require(row.get("policyVersion").equals(profile.path("version").asText()),"Policy not pinned");
                require(row.get("intakeOwnerId").equals(aliases.path("intake").asText()),"Wrong intake owner");
                require(row.get("supervisorId").equals(aliases.path("supervisor").asText()),"Wrong supervisor");
            }
        }
        // Test the real survey boundary too: neither declaration is installable eligibility.
        call(installer,"survey");
        require(omitted.contains("baseline.conditions"),"Conditions unexpectedly became installable");
        require(omitted.contains("baseline.sourceProvenance"),"Provenance unexpectedly became installable");
        require(!ScenarioFixtureInstaller.SUPPORTED_TYPES.contains("RegulatoryPolicy"),"Reassess newly supported policy path");
    }
    public static void main(String[] args)throws Exception {
        Path root=Path.of(args[0]);int profiles=0,fixtures=0;
        try(var paths=Files.list(root.resolve("verification/cases/T17/fixtures"))){
            for(Path path:paths.filter(p->p.toString().endsWith(".json")).sorted().toList()){
                ObjectNode fixture=(ObjectNode)Json.read(path);check(root,fixture,false);
                ObjectNode old=fixture.deepCopy();
                for(JsonNode profile:old.path("baseline").path("sourceProfiles")){
                    ((ObjectNode)profile).remove(List.of("namespace","reconciliationOwnerAlias","policyVersion"));profiles++;
                }
                check(root,old,true);fixtures++;
            }
        }
        System.out.println("PASS compiled installer mapping: "+fixtures+" fixtures, "+profiles+" source profiles; original omission and remaining policy/condition blockers confirmed. JDBC captured only, scenarios NOT_RUN.");
    }
}
