package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mulino.verification.Json;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real JDBC scope traversal in one MVCC snapshot; CSN-shaped tables isolate the observer. */
@EnabledIfEnvironmentVariable(named="OBSERVER_PG_TEST",matches="true")
class ScenarioWorkDescendantsPostgresTest {
    private ScenarioObservationPostgresTest f;
    @BeforeEach void setup(TestInfo info) throws Exception {f=new ScenarioObservationPostgresTest();f.setup(info);f.base();}
    @AfterEach void cleanup() throws Exception {if(f!=null)f.cleanup();}
    private String work(String org,String item,String at) throws Exception {
        String id=ScenarioObservationPostgresTest.id();f.insert("mulino_work_read_Works",f.row(id).put("organizationId",org)
            .put("itemId",item).put("lotId",f.lot).put("createdAt",at).put("recordedAt",at).put("status","ACTIVE"));return id;
    }
    private void link(String org,String parent,String child,String kind,String at) throws Exception {
        f.insert("mulino_work_WorkLinks",Json.object().put("ID",ScenarioObservationPostgresTest.id()).put("organizationId",org)
            .put("sourceWorkId",parent).put("targetWorkId",child).put("kind",kind).put("createdAt",at));
    }
    private ScenarioWorkDescendants.Expansion expand(List<String> orgs,List<String> roots) throws Exception {
        try(var c=DriverManager.getConnection(f.config.jdbcUrl(),f.config.username(),f.config.password())) {
            c.setAutoCommit(false);c.setReadOnly(true);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            var result=ScenarioWorkDescendants.expand(c,orgs,roots,OffsetDateTime.parse(f.now));c.commit();return result;
        }
    }
    @Test void followsOnlyDirectedVisibleSameOrganizationAndItemFollowups() throws Exception {
        String child=work(f.org,f.item,f.now),grandchild=work(f.org,f.item,f.now);
        link(f.org,f.work,child,"FOLLOWUP",f.now);link(f.org,child,grandchild,"FOLLOWUP",f.now);
        work(f.org,f.item,f.now); // Unrelated same-ITEM Work is not a descendant.
        String related=work(f.org,f.item,f.now);link(f.org,f.work,related,"CONTRIBUTES_TO",f.now);
        String futureLink=work(f.org,f.item,f.now);link(f.org,f.work,futureLink,"FOLLOWUP",f.later);
        String futureChild=work(f.org,f.item,f.later);link(f.org,f.work,futureChild,"FOLLOWUP",f.now);
        String foreign=work(ScenarioObservationPostgresTest.id(),f.item,f.now);link(f.org,f.work,foreign,"FOLLOWUP",f.now);
        String otherItem=work(f.org,ScenarioObservationPostgresTest.id(),f.now);link(f.org,f.work,otherItem,"FOLLOWUP",f.now);
        String reversed=work(f.org,f.item,f.now);link(f.org,reversed,f.work,"FOLLOWUP",f.now);
        var result=expand(List.of(f.org),List.of(f.work));
        assertEquals(Set.of(f.work,child,grandchild),new HashSet<>(result.ids()));
        assertEquals(result.ids(),Json.MAPPER.convertValue(result.query().path("selectedWorkIds"),List.class));
        assertTrue(result.query().path("sql").asText().contains("WITH RECURSIVE"));
        assertTrue(result.query().path("sql").asText().contains("child.itemId=original.itemId"));
        assertFalse(result.query().path("sql").asText().contains(f.org),"All scope values are bound parameters");
    }
    @Test void cycleAndDuplicateEdgesTerminateWithoutDuplicateOrUnrelatedRows() throws Exception {
        String child=work(f.org,f.item,f.now);link(f.org,f.work,child,"FOLLOWUP",f.now);
        link(f.org,f.work,child,"FOLLOWUP",f.now);link(f.org,child,f.work,"FOLLOWUP",f.now);
        var result=expand(List.of(f.org),List.of(f.work,f.work));assertEquals(2,result.ids().size());assertEquals(Set.of(f.work,child),new HashSet<>(result.ids()));
    }
    @Test void missingOrFutureRootsAndMultipleOrganizationsFailClosed() throws Exception {
        assertThrows(UnsupportedOperationException.class,()->expand(List.of(f.org),List.of(ScenarioObservationPostgresTest.id())));
        String future=work(f.org,f.item,f.later);assertThrows(UnsupportedOperationException.class,()->expand(List.of(f.org),List.of(future)));
        assertThrows(UnsupportedOperationException.class,()->expand(List.of(f.org,ScenarioObservationPostgresTest.id()),List.of(f.work)));
        assertThrows(IllegalArgumentException.class,()->expand(List.of(f.org),List.of()));
    }
    @Test void descendantScopeIsUsedByTheActualCaptureAndBoundIntoEverySource() throws Exception {
        String child=work(f.org,f.item,f.now),unrelated=work(f.org,f.item,f.now);link(f.org,f.work,child,"FOLLOWUP",f.now);
        var request=f.request(f.scope().put("includeDescendants",true),"works","goals");
        var actual=ScenarioJdbcObservation.capture(f.config,f.root,request);Set<String> ids=new HashSet<>();
        actual.path("rawRows").path("works").forEach(w->ids.add(w.path("id").asText()));
        assertEquals(Set.of(f.work,child),ids);assertFalse(ids.contains(unrelated));
        for(String source:List.of("works","goals")) {
            JsonNode proof=Json.parse(actual.path("sourceEvidence").path(source).path("sourceQuery").path("sql").asText()).get(0);
            assertEquals("FOLLOWUP_DESCENDANT_SCOPE_V1",proof.path("recipe").asText());
            assertEquals(Set.of(f.work,child),new HashSet<>(Json.MAPPER.convertValue(proof.path("selectedWorkIds"),List.class)));
        }
        String ref="verification/harness/target/evidence/descendant-capture-"+UUID.randomUUID()+".json";
        ScenarioJdbcObservation.bindArtifact(actual,request.path("sources"),ref);Json.write(f.root.resolve(ref),actual);f.validate(request,actual);
    }
    @Test void omissionOfIncludeDescendantsRetainsExactRootScope() throws Exception {
        String child=work(f.org,f.item,f.now);link(f.org,f.work,child,"FOLLOWUP",f.now);
        var result=ScenarioJdbcObservation.capture(f.config,f.root,f.request(f.scope(),"works"));
        assertEquals(1,result.path("rawRows").path("works").size());assertEquals(f.work,result.path("rawRows").path("works").get(0).path("id").asText());
    }
}
