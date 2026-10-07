package com.mulino.domain.definitions;

import java.util.*;

/** Immutable pinned meaning; current authority policy is deliberately separate. */
public record Definition(String organizationId, String id, String version, String parentVersionId,
    String state, String contentHash, String evaluatorVersion, String schemaVersion,
    List<NounType> nouns, List<Attribute> attributes, List<Verb> verbs,
    List<Relation> relations, List<Goal> goals, List<Capability> capabilities) {
  public Definition {
    nouns=List.copyOf(nouns); attributes=List.copyOf(attributes); verbs=List.copyOf(verbs);
    relations=List.copyOf(relations); goals=List.copyOf(goals); capabilities=List.copyOf(capabilities);
  }
  public enum ValueType { STRING, BOOLEAN, DECIMAL, INSTANT, DATE, REFERENCE }
  public record NounType(String name, boolean core) {}
  public record Attribute(String nounType, String name, ValueType type, String referenceType,
      String unit, int decimalPlaces, int minimumCount, int maximumCount, String requiredStage,
      boolean core) {}
  public record Verb(String name, String intentKind, String capabilityId, String stage,
      Map<String,String> slots) { public Verb { slots=Map.copyOf(slots); } }
  public record Relation(String name, String sourceType, String targetType, int minimumCount,
      int maximumCount, boolean cycleAllowed) {}
  public record Goal(String name, String quantityMode, String endpoint, String evaluatorVersion,
      Map<String,Object> predicate) { public Goal { predicate=Map.copyOf(predicate); } }
  public record Capability(String capabilityId, String semanticVersion, String evaluatorVersion,
      String inputSchemaVersion, String outputSchemaVersion, List<String> supportedWorkMigration) {
    public Capability { supportedWorkMigration=List.copyOf(supportedWorkMigration); }
  }
}
