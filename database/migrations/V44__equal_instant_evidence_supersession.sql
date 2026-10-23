-- Recording clocks can be fixed for verification and multiple revisions can share
-- an instant. Append-only existing-prior + revision+1 orders that instant without
-- changing effective/knowledge time, historical bytes or single-successor guards.
CREATE OR REPLACE FUNCTION mulino_evidence_validate_supersession()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE prior_org varchar(36); prior_subject varchar(36); prior_kind varchar(20);
        prior_recorded timestamptz; prior_revision integer;
BEGIN
  IF NEW.supersedesId IS NOT NULL THEN
    EXECUTE format('SELECT organizationId, subjectId, subjectKind, recordedAt, revision FROM %I WHERE ID=$1', TG_TABLE_NAME)
      INTO prior_org, prior_subject, prior_kind, prior_recorded, prior_revision
      USING NEW.supersedesId;
    IF prior_revision IS NULL OR NEW.revision IS NULL
      OR prior_recorded IS NULL OR NEW.recordedAt IS NULL
      OR NEW.ID = NEW.supersedesId
      OR prior_org IS DISTINCT FROM NEW.organizationId
      OR prior_subject IS DISTINCT FROM NEW.subjectId
      OR prior_kind IS DISTINCT FROM NEW.subjectKind
      OR prior_recorded > NEW.recordedAt
      OR NEW.revision <> prior_revision+1 THEN
      RAISE EXCEPTION 'Invalid immutable supersession' USING ERRCODE='23514';
    END IF;
  END IF;
  RETURN NEW;
END $$;
