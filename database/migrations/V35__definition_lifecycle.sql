-- Plan §8 DefinitionPackage lifecycle DRAFT→IN_REVIEW→PUBLISHED→ACTIVE→RETIRED_FOR_NEW_WORK (user decision 2026-10-11 (2):
-- the package contract is the stored Definition shape). DefinitionVersions keeps the content, hash and DRAFT/IN_REVIEW/
-- PUBLISHED; the lifecycle revision, validations, reviews, approvals, the per-organization ACTIVE pointer and retirement
-- are separate rows, so published content and hash stay immutable while ACTIVE is only a pointer.
CREATE TABLE mulino_definitions_DefinitionLifecycles (
 organizationId varchar(36) NOT NULL, definitionId varchar(36) NOT NULL, revision integer NOT NULL,
 createdBy varchar(36) NOT NULL, regressionFixtureRefs text NOT NULL DEFAULT '[]',
 createdAt timestamptz NOT NULL, recordedAt timestamptz NOT NULL,
 PRIMARY KEY(organizationId,definitionId),
 FOREIGN KEY(organizationId,definitionId) REFERENCES mulino_definitions_DefinitionVersions(organizationId,ID),
 CHECK(revision>=1), CHECK(jsonb_typeof(regressionFixtureRefs::jsonb)='array')
);
CREATE TABLE mulino_definitions_DefinitionValidations (
 organizationId varchar(36) NOT NULL, ID varchar(36) NOT NULL, definitionId varchar(36) NOT NULL,
 hash varchar(64) NOT NULL, outcome varchar(24) NOT NULL, publishable boolean NOT NULL,
 problemsJson text NOT NULL, diffClassesJson text NOT NULL, regressionJson text NOT NULL,
 actorId varchar(36) NOT NULL, recordedAt timestamptz NOT NULL,
 PRIMARY KEY(organizationId,ID),
 FOREIGN KEY(organizationId,definitionId) REFERENCES mulino_definitions_DefinitionVersions(organizationId,ID)
);
CREATE TABLE mulino_definitions_DefinitionReviews (
 organizationId varchar(36) NOT NULL, ID varchar(36) NOT NULL, definitionId varchar(36) NOT NULL,
 hash varchar(64) NOT NULL, reviewKind varchar(24) NOT NULL, validationId varchar(36), migrationId varchar(80),
 payloadJson text NOT NULL, actorId varchar(36) NOT NULL, recordedAt timestamptz NOT NULL,
 PRIMARY KEY(organizationId,ID),
 FOREIGN KEY(organizationId,definitionId) REFERENCES mulino_definitions_DefinitionVersions(organizationId,ID),
 FOREIGN KEY(organizationId,validationId) REFERENCES mulino_definitions_DefinitionValidations(organizationId,ID),
 CHECK(reviewKind IN ('DEFINITION','WORK_MIGRATION'))
);
CREATE TABLE mulino_definitions_DefinitionApprovals (
 organizationId varchar(36) NOT NULL, ID varchar(36) NOT NULL, definitionId varchar(36) NOT NULL,
 hash varchar(64) NOT NULL, reviewId varchar(36) NOT NULL, reviewKind varchar(24) NOT NULL,
 approverId varchar(36) NOT NULL, decision varchar(16) NOT NULL, reason varchar(500),
 validFrom timestamptz NOT NULL, validUntil timestamptz NOT NULL, consumptionPolicy varchar(40) NOT NULL,
 goalChangesJson text, consumedAt timestamptz, consumedBy varchar(36), recordedAt timestamptz NOT NULL,
 PRIMARY KEY(organizationId,ID),
 FOREIGN KEY(organizationId,definitionId) REFERENCES mulino_definitions_DefinitionVersions(organizationId,ID),
 FOREIGN KEY(organizationId,reviewId) REFERENCES mulino_definitions_DefinitionReviews(organizationId,ID),
 CHECK(decision IN ('APPROVED','REJECTED')), CHECK(validUntil>validFrom)
);
CREATE TABLE mulino_definitions_ActiveDefinitionPointers (
 organizationId varchar(36) NOT NULL, definitionId varchar(36) NOT NULL, version varchar(80) NOT NULL,
 hash varchar(64) NOT NULL, effectiveAt timestamptz NOT NULL, actorId varchar(36) NOT NULL,
 revision integer NOT NULL, recordedAt timestamptz NOT NULL,
 PRIMARY KEY(organizationId),
 FOREIGN KEY(organizationId,definitionId) REFERENCES mulino_definitions_DefinitionVersions(organizationId,ID)
);
CREATE TABLE mulino_definitions_DefinitionRetirements (
 organizationId varchar(36) NOT NULL, definitionId varchar(36) NOT NULL, reason varchar(500) NOT NULL,
 actorId varchar(36) NOT NULL, retiredAt timestamptz NOT NULL,
 PRIMARY KEY(organizationId,definitionId),
 FOREIGN KEY(organizationId,definitionId) REFERENCES mulino_definitions_DefinitionVersions(organizationId,ID)
);
CREATE TABLE mulino_definitions_WorkDefinitionMigrations (
 organizationId varchar(36) NOT NULL, ID varchar(36) NOT NULL, workId varchar(36) NOT NULL,
 approvalId varchar(36) NOT NULL, migrationId varchar(80) NOT NULL,
 previousDefinitionId varchar(36) NOT NULL, definitionId varchar(36) NOT NULL,
 previousGoalVersionId varchar(36) NOT NULL, goalVersionId varchar(36) NOT NULL,
 actorId varchar(36) NOT NULL, reason varchar(500) NOT NULL, recordedAt timestamptz NOT NULL,
 PRIMARY KEY(organizationId,ID), UNIQUE(organizationId,approvalId,workId),
 FOREIGN KEY(organizationId,approvalId) REFERENCES mulino_definitions_DefinitionApprovals(organizationId,ID),
 FOREIGN KEY(organizationId,workId) REFERENCES mulino_work_read_Works(organizationId,ID)
);
CREATE TRIGGER definition_validations_immutable BEFORE UPDATE OR DELETE ON mulino_definitions_DefinitionValidations
 FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
CREATE TRIGGER definition_reviews_immutable BEFORE UPDATE OR DELETE ON mulino_definitions_DefinitionReviews
 FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
CREATE TRIGGER work_definition_migrations_immutable BEFORE UPDATE OR DELETE ON mulino_definitions_WorkDefinitionMigrations
 FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
