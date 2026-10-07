package com.mulino.core;

import com.mulino.application.core.*;
import com.mulino.application.trade.TradeImpact;
import com.mulino.application.work.WorkAccess;
import com.mulino.domain.definitions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import static org.junit.jupiter.api.Assertions.*;

/** Shared integration contract only; domain runtime acceptance requires real S3 domain handlers. */
@Import(S3SharedIntegrationTest.Configuration.class)
class S3SharedIntegrationTest extends S1ReadIntegrationTest {
  @Autowired ApplicationCommands commands;
  @Autowired TradeImpact impact;
  @BeforeEach void commandContract()throws Exception {
    // Same authoritative COMMAND Work setup as ReceiptGatewayPostgresTest;
    // imported S1 rows remain immutable and are not rewritten by this fixture.
    work=id();
    var authoritative=workRow(work);
    authoritative.putAll(row("itemId",item,"lotId",lot,"definitionVersionId",definition,"kind","REVIEW","status","ACTIVE","ownerId",actor,"supervisorId",actor,"lifecycleMode","COMMAND"));
    insert("mulino_work_read_Works",authoritative);
    String grant=jdbc.queryForObject("SELECT ID FROM mulino_identity_Grants WHERE organizationId=?",String.class,org);
    insert("mulino_identity_CapabilityAssignments",row("organizationId",org,"ID",id(),"actorId",actor,"capabilityId","s3SharedImpact","scopeKind","ORGANIZATION","scopeId",org,"validFrom",Timestamp(Instant.now().minusSeconds(3600)),"validUntil",Timestamp(Instant.now().plusSeconds(3600))));
    insert("mulino_identity_GrantActions",row("organizationId",org,"grantId",grant,"capabilityId","s3SharedImpact"));
    var caps=List.of(new Definition.Capability("s3SharedImpact","1.0.0","core-v1","1.0.0","1.0.0",List.of()));
    var d=new Definition(org,id(),"s3-shared-v1",null,"PUBLISHED",null,"core-v1","1.0.0",List.of(new Definition.NounType("Work",true)),List.of(),List.of(new Definition.Verb("s3SharedImpact","COMMAND","s3SharedImpact","ACTIVE",Map.of())),List.of(),List.of(),caps);
    String body=json.writeValueAsString(d);
    insert("mulino_definitions_DefinitionVersions",row("organizationId",org,"ID",d.id(),"createdAt",Timestamp(Instant.now()),"version",d.version(),"state","PUBLISHED","contentHash",DefinitionRepository.sha256(body),"content",body,"evaluatorVersion","core-v1","schemaVersion","1.0.0"));
    String policy=id(),content="{\"rules\":{\"s3SharedImpact\":{\"effectClass\":\"TRADE_IMPACT\"}}}";
    insert("mulino_governance_PolicyVersions",row("organizationId",org,"ID",policy,"revision",1,"createdAt",Timestamp(Instant.now()),"version","s3-shared-v1","kind","COMMAND","content",content,"contentHash",DefinitionRepository.sha256(content),"effectiveFrom",Timestamp(Instant.now().minusSeconds(60)),"effectiveUntil",Timestamp(Instant.now().plusSeconds(3600))));
    insert("mulino_governance_ActivePolicies",row("organizationId",org,"kind","COMMAND","policyId",policy,"revision",1));
  }
  java.sql.Timestamp Timestamp(Instant time){return java.sql.Timestamp.from(time);}
  Map<String,Object> command(boolean fail,String key){return row("intentKind","COMMAND","definitionVersion","s3-shared-v1","capabilityId","s3SharedImpact","commandIdempotencyKey",key,"subjectRefs",List.of(Map.of("type","Work","id",work)),"slots",Map.of("workId",work,"sourceId","regulatory-version-1","physicalScopeId",segment,"fail",fail),"provenance",Map.of("sourceNamespace","USER"),"expectedRevision",jdbc.queryForObject("SELECT revision FROM mulino_work_read_Works WHERE organizationId=? AND ID=?",Integer.class,org,work));}
  Map<String,Object> execute(Map<String,Object> input){return runtime.requestContext().run(c->{return commands.execute(input);});}
  @Test void gatewayDutyAndAssessmentPendingCommitOnceAndCurrentGrantReplayIsDenied(){
    var input=command(false,"once");var first=execute(input);assertEquals("APPLIED",first.get("outcome"));assertEquals(first,execute(input));
    assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_responsibility_Roots WHERE organizationId=?",Integer.class,org));
    assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_work_read_ObligationReferences WHERE organizationId=? AND rootId IS NOT NULL AND ownerId=? AND nextAction IS NOT NULL AND nextCheckAt IS NOT NULL",Integer.class,org,actor));
    assertEquals(true,jdbc.queryForObject("SELECT pendingInvalidation FROM mulino_work_read_Works WHERE organizationId=? AND ID=?",Boolean.class,org,work));
    assertEquals(first.get("commandId"),jdbc.queryForObject("SELECT sourceId FROM mulino_responsibility_Roots WHERE organizationId=?",String.class,org));
    jdbc.update("DELETE FROM mulino_identity_GrantActions WHERE organizationId=? AND capabilityId='s3SharedImpact'",org);
    assertEquals("FORBIDDEN",((Map<?,?>)execute(input).get("error")).get("code"));assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_responsibility_Roots WHERE organizationId=?",Integer.class,org));
    assertEquals("COMMITTED",jdbc.queryForObject("SELECT state FROM mulino_commands_CommandRecords WHERE ID=?",String.class,first.get("commandId")));
  }
  @Test void domainFailureRollsBackDutyPendingAndTransitionsTogether(){
    var rejected=execute(command(true,"fail"));assertEquals("HELD",rejected.get("outcome"));
    assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM mulino_responsibility_Roots WHERE organizationId=?",Integer.class,org));
    assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM mulino_work_WorkTransitions WHERE organizationId=?",Integer.class,org));
    assertEquals(false,jdbc.queryForObject("SELECT pendingInvalidation FROM mulino_work_read_Works WHERE organizationId=? AND ID=?",Boolean.class,org,work));
    assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM mulino_inventory_QuantityMovements WHERE organizationId=?",Integer.class,org));
    assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_commands_CommandAudits WHERE organizationId=? AND outcome='HELD'",Integer.class,org));
  }
  @Test void distinctExpiryBoundariesHaveSeparateStableDuties(){
    var applied=execute(command(false,"origin"));String source=applied.get("commandId").toString();
    var transaction=new org.springframework.transaction.support.TransactionTemplate(transactions);
    for(String boundary:List.of("permission-v1:2026-10-08T00:00:00Z","permission-v1:2026-10-08T00:00:00Z","permission-v2:2026-10-09T00:00:00Z"))transaction.executeWithoutResult(s->runtime.requestContext().run(ctx->{Instant now=Instant.now();impact.recorded(new DomainContext(org,actor,actor,now,now),work,boundary,"VALIDITY_EXPIRED",segment,"Review expiry",now.plusSeconds(3600),source);return null;}));
    assertEquals(3,jdbc.queryForObject("SELECT count(*) FROM mulino_responsibility_Roots WHERE organizationId=?",Integer.class,org));
    assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM mulino_responsibility_Roots WHERE organizationId=? AND kind='VALIDITY_EXPIRED'",Integer.class,org));
  }
  @TestConfiguration static class Configuration {
    @Bean CommandHandler sharedImpactHandler(TradeImpact impact,WorkAccess works){return new CommandHandler(){
      public Set<String> capabilities(){return Set.of("s3SharedImpact");}
      public CommandPreparation prepare(DomainContext c,Map<String,Object> intent){var p=(Map<String,Object>)intent.get("slots");String id=p.get("workId").toString();var w=works.require(c,id,false);return CommandPreparation.ordinary(Map.of("WORK",List.of(id),"ITEM",List.of(w.get("itemId").toString())),List.of("work:"+id),"TRADE_IMPACT",id,((Number)w.get("revision")).intValue());}
      public List<SubjectBinding> subjectBindings(DomainContext c,Map<String,Object> intent,CommandPreparation p){return List.of(SubjectBinding.required("Work",Set.of(p.targetId())));}
      public Map<String,Object> execute(DomainContext c,Map<String,Object> intent){var p=(Map<String,Object>)intent.get("slots");String id=p.get("workId").toString();impact.recorded(c,id,p.get("sourceId").toString(),"REGULATORY_REVIEW",p.get("physicalScopeId").toString(),"Review regulatory evidence",Instant.now().plusSeconds(3600));if(Boolean.TRUE.equals(p.get("fail")))throw new DomainError("HELD","FIXTURE_FAILURE","Rollback shared impact");return Map.of("outcome","APPLIED","effects",Map.of("workId",id));}
    };}
  }
}
