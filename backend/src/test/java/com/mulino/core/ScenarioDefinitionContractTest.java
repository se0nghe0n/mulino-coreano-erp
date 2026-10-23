package com.mulino.core;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mulino.application.core.*;
import com.mulino.application.evaluation.AssessmentService;
import com.mulino.domain.definitions.*;
import com.sap.cds.services.runtime.CdsRuntime;
import java.nio.file.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** The ordinary scenario package must satisfy the deployed registry before a goal can activate. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"local", "verification"})
class ScenarioDefinitionContractTest {
  static final Set<String> REPAIRED_QUERIES = Set.of("traceLot", "getDefinition", "searchObjects",
      "searchWorks", "getInbox", "searchOperationalIssues", "getSettlement", "traceRecall");
  static final PostgreSQLContainer PG = new PostgreSQLContainer(
      "postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280");
  static final Path KEY_PATH;
  static {
    try {
      PG.start();
      var generator = java.security.KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      KEY_PATH = Files.createTempFile("mulino-scenario-definition-", ".pem");
      Files.writeString(KEY_PATH, "-----BEGIN PUBLIC KEY-----\n"
          + Base64.getMimeEncoder(64, new byte[]{10}).encodeToString(generator.generateKeyPair().getPublic().getEncoded())
          + "\n-----END PUBLIC KEY-----\n");
    } catch (Exception failure) { throw new ExceptionInInitializerError(failure); }
  }
  @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", PG::getJdbcUrl);
    r.add("spring.datasource.username", PG::getUsername);
    r.add("spring.datasource.password", PG::getPassword);
    r.add("JWT_PUBLIC_KEY", KEY_PATH::toString);
    r.add("JWT_ISSUER", () -> "https://mulino.local.invalid");
    r.add("JWT_AUDIENCE", () -> "mulino-platform");
    r.add("mulino.verification.instant", () -> "2026-10-07T09:00:00Z");
  }
  @AfterAll static void stop() throws Exception { PG.stop(); Files.deleteIfExists(KEY_PATH); }

  @Autowired DefinitionValidator validator;
  @Autowired DefinitionRepository definitions;
  @Autowired ApplicationQueries queries;
  @Autowired ApplicationCommands commands;
  @Autowired List<QueryHandler> queryHandlers;
  @Autowired List<CommandHandler> commandHandlers;
  @Autowired AssessmentService assessment;
  @Autowired JdbcTemplate jdbc;
  @Autowired CdsRuntime runtime;
  final ObjectMapper json = new ObjectMapper();

  ObjectNode common() throws Exception {
    return (ObjectNode) json.readTree(Path.of(System.getProperty("repo.root", ".."))
        .resolve("verification/actual/scenarios/installer-conventions.json").toFile()).path("definition").deepCopy();
  }
  Definition decode(ObjectNode content) throws Exception { return json.treeToValue(content, Definition.class); }
  Definition install(ObjectNode content) throws Exception {
    String org = UUID.randomUUID().toString(), id = UUID.randomUUID().toString();
    content.put("organizationId", org).put("id", id).put("state", "PUBLISHED");
    String bytes = json.writeValueAsString(content);
    jdbc.update("INSERT INTO mulino_identity_Organizations(ID,externalAlias) VALUES(?,?)", org, org);
    jdbc.update("INSERT INTO mulino_definitions_DefinitionVersions"
        + "(organizationId,ID,version,state,contentHash,content,evaluatorVersion,schemaVersion,createdAt)"
        + " VALUES(?,?,?,'PUBLISHED',?,?,?,?,?)", org, id, content.path("version").asText(),
        DefinitionRepository.sha256(bytes), bytes, content.path("evaluatorVersion").asText(),
        content.path("schemaVersion").asText(), Timestamp.from(Instant.parse("2026-10-07T09:00:00Z")));
    return runtime.requestContext().run(c -> { return definitions.get(org, id); });
  }
  Map<String,Object> goal(Definition d, String endpoint) throws Exception {
    return Map.of("definitionVersionId", d.id(), "slotsJson", json.writeValueAsString(Map.of(
        "evaluatorVersion", "core-v1", "quantityMode", "CUMULATIVE_EVENT", "endpoint", endpoint,
        "conditions", List.of(endpoint.equals("ARRIVED") ? "arrived" : "delivered"))));
  }
  void requireSupported(Definition d, Map<String,Object> goal) {
    Instant at = Instant.parse("2026-10-07T09:00:00Z");
    runtime.requestContext().run(c -> {
      assessment.requireSupportedGoal(new DomainContext(d.organizationId(), "actor", "owner", at, at), goal);
      return null;
    });
  }
  void unsupported(Definition d, Map<String,Object> goal, String message) {
    var error = assertThrows(DomainError.class, () -> requireSupported(d, goal));
    assertEquals("VERSION_UNSUPPORTED", error.code());
    assertEquals(message, error.getMessage());
  }

  @Test void installedCommonPackageUsesTheActualRegistryAndKeepsTypedGoals() throws Exception {
    var d = install(common());
    var result = validator.validate(d);
    System.out.println("COMMON_DEFINITION_VALIDATION " + result);
    assertEquals("VALID", result.outcome(), result.problems().toString());
    assertTrue(result.problems().isEmpty());
    var registeredQueries = queryHandlers.stream().flatMap(h -> h.operations().stream()).collect(Collectors.toSet());
    var registeredCommands = commandHandlers.stream().flatMap(h -> h.capabilities().stream()).collect(Collectors.toSet());
    assertEquals(registeredQueries, queries.operations());
    assertEquals(registeredCommands, commands.operations());
    assertTrue(registeredQueries.containsAll(REPAIRED_QUERIES));
    for (var verb : d.verbs()) {
      assertTrue((verb.intentKind().equals("QUERY") ? registeredQueries : registeredCommands)
          .contains(verb.capabilityId()), "No actual handler for " + verb);
      assertEquals(1, d.capabilities().stream().filter(c -> c.capabilityId().equals(verb.capabilityId())).count());
    }
    assertFalse(registeredQueries.contains("getCommandResult"));
    assertFalse(d.verbs().stream().anyMatch(v -> v.capabilityId().equals("getCommandResult")));
    assertFalse(d.capabilities().stream().anyMatch(c -> c.capabilityId().equals("getCommandResult")));
    assertEquals(List.of("arrived", "delivered", "held-at", "held-exists", "held-throughout"), d.goals().stream().map(Definition.Goal::name).toList());
    for (var template : d.goals().stream().filter(g -> g.quantityMode().equals("CUMULATIVE_EVENT")).toList()) {
      assertEquals("core-v1", template.evaluatorVersion());
      assertEquals("quantitySum", template.predicate().get("operator"));
      assertEquals("VERIFIED_DISTINCT", template.predicate().get("evidenceSelector"));
      requireSupported(d, goal(d, template.endpoint()));
    }
  }

  @Test void previousManifestHasExactlyNineViolationsAndCannotActivateGoals() throws Exception {
    var content = common();
    var capabilities = (ArrayNode) content.path("capabilities");
    for (int i = capabilities.size() - 1; i >= 0; i--)
      if (REPAIRED_QUERIES.contains(capabilities.get(i).path("capabilityId").asText())) capabilities.remove(i);
    ((ArrayNode) content.path("verbs")).addObject().put("name", "getCommandResult")
        .put("intentKind", "QUERY").put("capabilityId", "getCommandResult").put("stage", "READ").putObject("slots");
    var d = install(content);
    var result = validator.validate(d);
    var expected = new HashSet<DefinitionValidator.Problem>();
    REPAIRED_QUERIES.forEach(q -> expected.add(new DefinitionValidator.Problem(q, "UNSUPPORTED_CAPABILITY")));
    expected.add(new DefinitionValidator.Problem("getCommandResult", "UNSUPPORTED_CAPABILITY"));
    System.out.println("PREVIOUS_DEFINITION_VALIDATION " + result);
    assertEquals("UNSUPPORTED", result.outcome());
    assertEquals(expected, new HashSet<>(result.problems()));
    assertEquals(9, result.problems().size());
    unsupported(d, goal(d, "ARRIVED"), "Pinned predicate unsupported");
  }

  @Test void conflictingAndUnimplementedCapabilitiesRemainUnsupported() throws Exception {
    var conflicting = common();
    var capabilities = (ArrayNode) conflicting.path("capabilities");
    var conflict = (ObjectNode) capabilities.get(0).deepCopy();
    conflict.put("semanticVersion", "99.0.0").put("inputSchemaVersion", "99.0.0");
    capabilities.add(conflict);
    var d = install(conflicting);
    var result = validator.validate(d);
    String id = conflict.path("capabilityId").asText();
    assertTrue(result.problems().contains(new DefinitionValidator.Problem(id, "DUPLICATE_CAPABILITY")));
    assertTrue(result.problems().contains(new DefinitionValidator.Problem(id, "UNSUPPORTED_CAPABILITY")));
    assertTrue(result.problems().contains(new DefinitionValidator.Problem(id, "UNSUPPORTED_RUNTIME_CONTRACT")));
    unsupported(d, goal(d, "ARRIVED"), "Pinned predicate unsupported");
    var phantom = common();
    ((ArrayNode) phantom.path("capabilities")).add(conflict.deepCopy().put("capabilityId", "getCommandResult")
        .put("semanticVersion", "1.0.0").put("inputSchemaVersion", "1.0.0"));
    var phantomResult = validator.validate(decode(phantom));
    assertEquals(List.of(new DefinitionValidator.Problem("getCommandResult", "UNSUPPORTED_CAPABILITY")), phantomResult.problems());
  }

  @Test void unsupportedEvaluatorAndTamperedGoalSelectorCannotUseTheCommonPackage() throws Exception {
    var d = install(common());
    var selected = goal(d, "ARRIVED");
    var slots = (ObjectNode) json.readTree((String) selected.get("slotsJson"));
    slots.put("evaluatorVersion", "future-v2");
    unsupported(d, Map.of("definitionVersionId", d.id(), "slotsJson", slots.toString()), "Pinned evaluator unsupported");
    slots.put("evaluatorVersion", "core-v1").putArray("conditions").add("unpublished-goal");
    unsupported(d, Map.of("definitionVersionId", d.id(), "slotsJson", slots.toString()), "Condition selector not published in pinned definition");
    var future = common();
    future.put("evaluatorVersion", "future-v2");
    for (var cap : future.path("capabilities")) ((ObjectNode) cap).put("evaluatorVersion", "future-v2");
    var futureDefinition = install(future);
    assertNotEquals("VALID", validator.validate(futureDefinition).outcome());
    unsupported(futureDefinition, goal(futureDefinition, "ARRIVED"), "Pinned predicate unsupported");
  }
  @Test void stockTemplatesAreTypedDistinctAndRequireExplicitPublishedSelectors() throws Exception {
    var d=install(common());
    assertEquals(d.goals().size(),d.goals().stream().map(g->g.endpoint()+"/"+g.quantityMode()).distinct().count());
    for(var template:d.goals().stream().filter(g->!g.quantityMode().equals("CUMULATIVE_EVENT")).toList()){
      var slots=new LinkedHashMap<String,Object>();
      slots.putAll(Map.of("quantityMode",template.quantityMode(),"endpoint",template.endpoint(),"evaluatorVersion","core-v1","unit","BOX","targetQuantity","100","placeId",UUID.randomUUID().toString(),"action","PHYSICAL_HELD","includeReserved",true,"observationPolicy","complete-initial-and-final-ledger"));
      slots.put("conditions",List.of(template.name()));
      var selected=Map.<String,Object>of("definitionVersionId",d.id(),"slotsJson",json.writeValueAsString(slots));
      requireSupported(d,selected);
      slots.remove("conditions");
      unsupported(d,Map.of("definitionVersionId",d.id(),"slotsJson",json.writeValueAsString(slots)),"Complete pinned goal conditions required");
      slots.put("conditions",List.of("arrived"));
      unsupported(d,Map.of("definitionVersionId",d.id(),"slotsJson",json.writeValueAsString(slots)),"Condition selector not published in pinned definition");
      if(!template.quantityMode().equals("STATE_AT")){
        slots.put("conditions",List.of(template.name()));slots.remove("observationPolicy");
        unsupported(d,Map.of("definitionVersionId",d.id(),"slotsJson",json.writeValueAsString(slots)),"Explicit supported stock observation policy required");
      }
    }
    var duplicate=common();
    ((ArrayNode)duplicate.path("goals")).add(duplicate.path("goals").get(0).deepCopy());
    var ambiguous=install(duplicate);
    unsupported(ambiguous,goal(ambiguous,"ARRIVED"),"Unique pinned goal template required");
  }

}
