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

CREATE FUNCTION mulino_work_lifecycle_guard() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF OLD.lifecycleMode='IMPORTED' THEN RAISE EXCEPTION 'Imported S1 work remains immutable'; END IF;
 IF OLD.status='CLOSED' AND NEW.status<>'CLOSED' THEN RAISE EXCEPTION 'Closed Work cannot reopen'; END IF;
 IF NEW.itemId<>OLD.itemId OR NEW.definitionVersionId<>OLD.definitionVersionId THEN RAISE EXCEPTION 'Pinned Work identity cannot change'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER work_lifecycle_guard BEFORE UPDATE ON mulino_work_read_Works FOR EACH ROW EXECUTE FUNCTION mulino_work_lifecycle_guard();
CREATE TRIGGER goal_read_delete_immutable BEFORE DELETE ON mulino_work_read_GoalReferences FOR EACH ROW EXECUTE FUNCTION mulino_work_read_immutable();

CREATE TABLE mulino_work_WorkContributions (
 organizationId VARCHAR(36) NOT NULL,ID VARCHAR(36) NOT NULL,
 linkId VARCHAR(36) NOT NULL,occurrenceId VARCHAR(36) NOT NULL,
 targetWorkId VARCHAR(36) NOT NULL,goalId VARCHAR(36) NOT NULL,
 conditionId VARCHAR(100) NOT NULL,startQuantity NUMERIC(38,12) NOT NULL,
 quantity NUMERIC(38,12) NOT NULL,unit VARCHAR(20) NOT NULL,
 recordedAt TIMESTAMPTZ NOT NULL,PRIMARY KEY(organizationId,ID),
 FOREIGN KEY(organizationId,linkId) REFERENCES mulino_work_WorkLinks(organizationId,ID),
 FOREIGN KEY(organizationId,occurrenceId) REFERENCES mulino_evidence_CanonicalOccurrences(organizationId,ID),
 FOREIGN KEY(organizationId,goalId,targetWorkId) REFERENCES mulino_work_read_GoalReferences(organizationId,ID,workId),
 CHECK(startQuantity>=0 AND quantity>0));
CREATE TRIGGER work_contribution_immutable BEFORE UPDATE OR DELETE ON mulino_work_WorkContributions FOR EACH ROW EXECUTE FUNCTION mulino_work_read_immutable();
CREATE FUNCTION mulino_work_contribution_range() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE actual NUMERIC(38,12); actual_unit VARCHAR(20);
BEGIN
 PERFORM pg_advisory_xact_lock(hashtextextended(NEW.organizationId||'|contribution|'||NEW.occurrenceId,0));
 SELECT quantity,unit INTO actual,actual_unit FROM mulino_evidence_CanonicalOccurrences WHERE organizationId=NEW.organizationId AND ID=NEW.occurrenceId FOR UPDATE;
 IF actual IS NULL OR NEW.unit<>actual_unit OR NEW.startQuantity+NEW.quantity>actual THEN RAISE EXCEPTION 'Contribution exceeds actual occurrence'; END IF;
 IF EXISTS(SELECT 1 FROM mulino_work_WorkContributions c WHERE c.organizationId=NEW.organizationId AND c.occurrenceId=NEW.occurrenceId AND c.startQuantity<NEW.startQuantity+NEW.quantity AND NEW.startQuantity<c.startQuantity+c.quantity) THEN RAISE EXCEPTION 'Actual contribution range already credited'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER work_contribution_range BEFORE INSERT ON mulino_work_WorkContributions FOR EACH ROW EXECUTE FUNCTION mulino_work_contribution_range();
