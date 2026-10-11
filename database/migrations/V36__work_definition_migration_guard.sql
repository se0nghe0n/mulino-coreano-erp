-- Plan §8 explicit migration: a Work's pinned definition changes only together with its recorded, approved
-- WorkDefinitionMigrations row (old → new definition) in the same transaction. Item identity stays immutable.
CREATE OR REPLACE FUNCTION mulino_work_lifecycle_guard() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF OLD.lifecycleMode='IMPORTED' THEN RAISE EXCEPTION 'Imported S1 work remains immutable'; END IF;
 IF OLD.status='CLOSED' AND NEW.status<>'CLOSED' THEN RAISE EXCEPTION 'Closed Work cannot reopen'; END IF;
 IF NEW.itemId<>OLD.itemId THEN RAISE EXCEPTION 'Pinned Work identity cannot change'; END IF;
 IF NEW.definitionVersionId<>OLD.definitionVersionId AND NOT EXISTS (SELECT 1 FROM mulino_definitions_WorkDefinitionMigrations m
     WHERE m.organizationId=NEW.organizationId AND m.workId=NEW.ID AND m.previousDefinitionId=OLD.definitionVersionId
       AND m.definitionId=NEW.definitionVersionId AND m.goalVersionId=NEW.currentGoalVersionId) THEN
  RAISE EXCEPTION 'Pinned Work identity cannot change';
 END IF;
 RETURN NEW;
END $$;
