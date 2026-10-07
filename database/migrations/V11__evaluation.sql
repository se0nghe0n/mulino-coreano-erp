ALTER TABLE mulino_work_read_AssessmentReferences
  ADD COLUMN definitionVersionId VARCHAR(36),
  ADD COLUMN policyVersionId VARCHAR(160),
  ADD COLUMN knownAt TIMESTAMPTZ,
  ADD COLUMN asOf TIMESTAMPTZ,
  ADD COLUMN previousAssessmentId VARCHAR(36),
  ADD COLUMN inputSnapshotHash VARCHAR(64),
  ADD COLUMN deadlineViolated BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN held BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN conflict BOOLEAN NOT NULL DEFAULT FALSE,
  ADD FOREIGN KEY(organizationId,definitionVersionId) REFERENCES mulino_definitions_DefinitionVersions(organizationId,ID),
  ADD FOREIGN KEY(organizationId,previousAssessmentId) REFERENCES mulino_work_read_AssessmentReferences(organizationId,ID);
CREATE TABLE mulino_evaluation_InputSnapshots (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  assessmentId VARCHAR(36) NOT NULL,
  workId VARCHAR(36) NOT NULL,
  goalId VARCHAR(36) NOT NULL,
  recordedAt TIMESTAMPTZ NOT NULL,
  asOf TIMESTAMPTZ NOT NULL,
  knownAt TIMESTAMPTZ NOT NULL,
  contentHash VARCHAR(64) NOT NULL CHECK(contentHash ~ '^[0-9a-f]{64}$'),
  contentJson TEXT NOT NULL CHECK(jsonb_typeof(contentJson::jsonb)='object'),
  PRIMARY KEY(organizationId,ID),
  UNIQUE(organizationId,assessmentId),
  FOREIGN KEY(organizationId,assessmentId) REFERENCES mulino_work_read_AssessmentReferences(organizationId,ID),
  FOREIGN KEY(organizationId,goalId,workId) REFERENCES mulino_work_read_GoalReferences(organizationId,ID,workId)
);
CREATE TRIGGER evaluation_snapshot_immutable BEFORE UPDATE OR DELETE ON mulino_evaluation_InputSnapshots FOR EACH ROW EXECUTE FUNCTION mulino_work_read_immutable();
CREATE TRIGGER assessment_delete_immutable BEFORE DELETE ON mulino_work_read_AssessmentReferences FOR EACH ROW EXECUTE FUNCTION mulino_work_read_immutable();
