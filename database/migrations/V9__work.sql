-- S1 imported references become the same S2 authoritative lifecycle records.
DROP TRIGGER work_read_immutable ON mulino_work_read_Works;
ALTER TABLE mulino_work_read_Works ADD COLUMN currentGoalVersionId VARCHAR(36),
 ADD COLUMN closeReason VARCHAR(40),
 ADD COLUMN pendingInvalidation BOOLEAN NOT NULL DEFAULT FALSE,
 ADD COLUMN originalText TEXT,
 ADD COLUMN conversationRequestId VARCHAR(240),
 ADD COLUMN lifecycleMode VARCHAR(20) NOT NULL DEFAULT 'IMPORTED';
ALTER TABLE mulino_work_read_Works DROP CONSTRAINT mulino_work_read_works_status_check;
ALTER TABLE mulino_work_read_Works ADD CHECK(status IN ('DRAFT','ACTIVE','READY','RUNNING','WAITING','FULFILLED','CANCELLED','FAILED','CLOSED')),
 ADD CHECK(closeReason IS NULL OR closeReason IN ('FULFILLED','CANCELLED','IMPOSSIBLE','SUPERSEDED')),
 ADD CHECK(lifecycleMode IN ('IMPORTED','COMMAND')),
 ADD FOREIGN KEY(organizationId,currentGoalVersionId,ID) REFERENCES mulino_work_read_GoalReferences(organizationId,ID,workId);
CREATE TRIGGER work_read_human_update BEFORE UPDATE ON mulino_work_read_Works FOR EACH ROW EXECUTE FUNCTION mulino_work_read_human_responsibility();
ALTER TABLE mulino_work_read_GoalReferences ALTER COLUMN quantityMode DROP NOT NULL;
ALTER TABLE mulino_work_read_GoalReferences ADD COLUMN slotsJson TEXT,
 ADD COLUMN provenanceJson TEXT,
 ADD COLUMN evidencePolicyVersion VARCHAR(80),
 ADD COLUMN timezone VARCHAR(80), ADD COLUMN dueAt TIMESTAMPTZ,
 ADD COLUMN previousGoalId VARCHAR(36), ADD COLUMN goalVersion INTEGER NOT NULL DEFAULT 1,
 ADD FOREIGN KEY(organizationId,previousGoalId,workId) REFERENCES mulino_work_read_GoalReferences(organizationId,ID,workId),
 ADD CHECK(slotsJson IS NULL OR jsonb_typeof(slotsJson::jsonb)='object'),
 ADD CHECK(provenanceJson IS NULL OR jsonb_typeof(provenanceJson::jsonb)='object'),
 ADD CHECK(goalVersion>0);
CREATE TABLE mulino_work_WorkLinks (
 organizationId VARCHAR(36) NOT NULL, ID VARCHAR(36) NOT NULL,
 sourceWorkId VARCHAR(36) NOT NULL, targetWorkId VARCHAR(36) NOT NULL,
 kind VARCHAR(40) NOT NULL, createdAt TIMESTAMPTZ NOT NULL,
 PRIMARY KEY(organizationId,ID),
 FOREIGN KEY(organizationId,sourceWorkId) REFERENCES mulino_work_read_Works(organizationId,ID),
 FOREIGN KEY(organizationId,targetWorkId) REFERENCES mulino_work_read_Works(organizationId,ID),
 CHECK(sourceWorkId<>targetWorkId), CHECK(kind IN ('CONTRIBUTES_TO','DEPENDS_ON','SHARES_ACTIVITY','FOLLOWUP')),
 UNIQUE(organizationId,sourceWorkId,targetWorkId,kind));
CREATE TABLE mulino_work_WorkTransitions (
 organizationId VARCHAR(36) NOT NULL, ID VARCHAR(36) NOT NULL,
 workId VARCHAR(36) NOT NULL, revision INTEGER NOT NULL,
 operation VARCHAR(80) NOT NULL, previousState VARCHAR(40), state VARCHAR(40) NOT NULL,
 actorId VARCHAR(36) NOT NULL, reason VARCHAR(500), recordedAt TIMESTAMPTZ NOT NULL,
 snapshotJson TEXT NOT NULL, PRIMARY KEY(organizationId,ID),
 FOREIGN KEY(organizationId,workId) REFERENCES mulino_work_read_Works(organizationId,ID),
 FOREIGN KEY(organizationId,actorId) REFERENCES mulino_identity_Actors(organizationId,ID),
 UNIQUE(organizationId,workId,revision), CHECK(jsonb_typeof(snapshotJson::jsonb)='object'));
CREATE TRIGGER work_transition_immutable BEFORE UPDATE OR DELETE ON mulino_work_WorkTransitions FOR EACH ROW EXECUTE FUNCTION mulino_work_read_immutable();
