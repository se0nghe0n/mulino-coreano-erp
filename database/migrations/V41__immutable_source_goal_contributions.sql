-- Existing target allocations remain valid. Unknown source pins are never backfilled.
ALTER TABLE mulino_work_WorkContributions
 ADD COLUMN sourceWorkId VARCHAR(36),
 ADD COLUMN sourceGoalVersionId VARCHAR(36),
 ADD COLUMN sourceConditionId VARCHAR(100),
 ADD CONSTRAINT work_contribution_source_complete CHECK (
  (sourceWorkId IS NULL AND sourceGoalVersionId IS NULL AND sourceConditionId IS NULL)
  OR (sourceWorkId IS NOT NULL AND sourceGoalVersionId IS NOT NULL AND sourceConditionId IS NOT NULL)),
 ADD CONSTRAINT work_contribution_source_goal_fk FOREIGN KEY(organizationId,sourceGoalVersionId,sourceWorkId)
 REFERENCES mulino_work_read_GoalReferences(organizationId,ID,workId);

CREATE FUNCTION mulino_work_contribution_binding() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE relation mulino_work_WorkLinks%ROWTYPE; source_goal VARCHAR(36); target_goal VARCHAR(36);
BEGIN
 SELECT * INTO relation FROM mulino_work_WorkLinks WHERE organizationId=NEW.organizationId AND ID=NEW.linkId FOR UPDATE;
 IF relation.targetWorkId IS DISTINCT FROM NEW.targetWorkId OR relation.sourceWorkId IS DISTINCT FROM NEW.sourceWorkId THEN
  RAISE EXCEPTION 'Contribution link endpoints differ from pinned allocation';
 END IF;
 IF NEW.sourceGoalVersionId IS NULL OR NEW.sourceConditionId IS NULL THEN
  RAISE EXCEPTION 'New contribution requires immutable source goal and condition';
 END IF;
 SELECT currentGoalVersionId INTO source_goal FROM mulino_work_read_Works WHERE organizationId=NEW.organizationId AND ID=NEW.sourceWorkId FOR UPDATE;
 SELECT currentGoalVersionId INTO target_goal FROM mulino_work_read_Works WHERE organizationId=NEW.organizationId AND ID=NEW.targetWorkId FOR UPDATE;
 IF source_goal IS DISTINCT FROM NEW.sourceGoalVersionId OR target_goal IS DISTINCT FROM NEW.goalId THEN
  RAISE EXCEPTION 'Contribution must pin current source and target goals';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER work_contribution_binding BEFORE INSERT ON mulino_work_WorkContributions FOR EACH ROW EXECUTE FUNCTION mulino_work_contribution_binding();

-- Link provenance cannot be redirected after an immutable allocation references it.
CREATE FUNCTION mulino_work_allocated_link_immutable() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF EXISTS(SELECT 1 FROM mulino_work_WorkContributions WHERE organizationId=OLD.organizationId AND linkId=OLD.ID) THEN
  RAISE EXCEPTION 'Allocated Work link is immutable';
 END IF;
 IF TG_OP='DELETE' THEN RETURN OLD; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER work_allocated_link_immutable BEFORE UPDATE OR DELETE ON mulino_work_WorkLinks FOR EACH ROW EXECUTE FUNCTION mulino_work_allocated_link_immutable();
