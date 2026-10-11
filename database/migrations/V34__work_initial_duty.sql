-- User decision 2026-10-11 UD-2 (plan §5.2 activateWork "초기 의무", §5.3): a Work states its owner's first next action at
-- creation and activation opens the Work-sourced initial duty. The duty root's source is the Work itself.
ALTER TABLE mulino_work_read_Works ADD nextAction varchar(320), ADD nextCheckAt timestamptz,
 ADD CONSTRAINT works_next_action_together CHECK ((nextAction IS NULL) = (nextCheckAt IS NULL)),
 ADD CONSTRAINT works_next_action_text CHECK (nextAction IS NULL OR length(trim(nextAction))>0);
DO $$
DECLARE c text;
BEGIN
 FOR c IN SELECT conname FROM pg_constraint WHERE conrelid='mulino_responsibility_roots'::regclass AND contype='c'
   AND pg_get_constraintdef(oid) LIKE '%sourcekind%' LOOP
  EXECUTE format('ALTER TABLE mulino_responsibility_Roots DROP CONSTRAINT %I',c);
 END LOOP;
END $$;
ALTER TABLE mulino_responsibility_Roots ADD CONSTRAINT responsibility_roots_source_kind
 CHECK(sourceKind IN ('OCCURRENCE','DECISION','EVENT','CLAIM','DOCUMENT','WORK'));
