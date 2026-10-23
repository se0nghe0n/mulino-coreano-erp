-- No backfill: pre-existing recovery rows have no proven historical state.
CREATE SEQUENCE mulino_runtime_recovery_history_sequence;
CREATE TABLE mulino_runtime_RecoveryObservationHistory (
 organizationId varchar(36) NOT NULL,
 ID bigint NOT NULL DEFAULT nextval('mulino_runtime_recovery_history_sequence'),
 sourceTable varchar(80) NOT NULL, sourceId varchar(36) NOT NULL,
 sourceRevision integer NOT NULL, recordedAt timestamptz NOT NULL,
 transactionId varchar(40) NOT NULL, operation varchar(10) NOT NULL,
 historyComplete boolean NOT NULL, scopeComplete boolean NOT NULL,
 workId varchar(36), itemId varchar(36), lotId varchar(36),
 snapshotJson text NOT NULL,
 PRIMARY KEY(organizationId,ID),
 CHECK(operation IN ('INSERT','UPDATE','DELETE'))
);
CREATE INDEX runtime_recovery_history_source ON mulino_runtime_RecoveryObservationHistory
 (organizationId,sourceTable,sourceId,recordedAt DESC,ID DESC);

CREATE FUNCTION mulino_recovery_history_immutable() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP <> 'INSERT' OR pg_trigger_depth() <> 2 THEN
  RAISE EXCEPTION 'Recovery observation history is append-only and trigger-owned';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER recovery_history_immutable BEFORE INSERT OR UPDATE OR DELETE
 ON mulino_runtime_RecoveryObservationHistory FOR EACH ROW EXECUTE FUNCTION mulino_recovery_history_immutable();
CREATE TRIGGER recovery_history_no_truncate BEFORE TRUNCATE
 ON mulino_runtime_RecoveryObservationHistory FOR EACH STATEMENT EXECUTE FUNCTION mulino_recovery_history_immutable();

CREATE FUNCTION mulino_record_recovery_history() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
 stamp timestamptz;
 body jsonb;
 previous mulino_runtime_RecoveryObservationHistory%ROWTYPE;
 org varchar(36); source varchar(36);
 bound_work varchar(36); bound_item varchar(36); bound_lot varchar(36);
 complete boolean := false; scope_complete boolean := false;
 source_event record;
BEGIN
 -- The writer must supply ExecutionClock in this transaction. Host now() is never a fallback.
 stamp := nullif(current_setting('mulino.recovery_recorded_at',true),'')::timestamptz;
 IF stamp IS NULL THEN RAISE EXCEPTION 'Recovery write requires transaction ExecutionClock'; END IF;
 body := CASE WHEN TG_OP='DELETE' THEN to_jsonb(OLD) ELSE to_jsonb(NEW) END;
 org := body->>'organizationid'; source := body->>'id';
 IF TG_OP='UPDATE' AND ((TG_TABLE_NAME='mulino_runtime_recoveryschedules' AND to_jsonb(NEW)->>'workid' IS DISTINCT FROM to_jsonb(OLD)->>'workid') OR (TG_TABLE_NAME='mulino_runtime_intakerecoveries' AND (to_jsonb(NEW)->>'eventid' IS DISTINCT FROM to_jsonb(OLD)->>'eventid' OR to_jsonb(NEW)->>'sourceprofileid' IS DISTINCT FROM to_jsonb(OLD)->>'sourceprofileid'))) THEN
  RAISE EXCEPTION 'Recovery source scope identity is immutable';
 END IF;
 IF TG_OP='UPDATE' AND (NEW.organizationId<>OLD.organizationId OR NEW.ID<>OLD.ID) THEN
  RAISE EXCEPTION 'Recovery source identity is immutable';
 END IF;
 SELECT * INTO previous FROM mulino_runtime_RecoveryObservationHistory
  WHERE organizationId=org AND sourceTable=TG_TABLE_NAME AND sourceId=source
  ORDER BY ID DESC LIMIT 1;
 IF FOUND THEN
  IF stamp<previous.recordedAt THEN RAISE EXCEPTION 'Recovery clock cannot move backwards'; END IF;
  IF previous.operation='DELETE' THEN RAISE EXCEPTION 'Deleted recovery source identity cannot be reused'; END IF;
  complete:=previous.historyComplete; scope_complete:=previous.scopeComplete;
  bound_work:=previous.workId; bound_item:=previous.itemId; bound_lot:=previous.lotId;
 ELSE
  complete:=TG_OP='INSERT';
  IF TG_TABLE_NAME='mulino_runtime_recoveryschedules' THEN
   bound_work:=body->>'workid';
   SELECT w.itemId,w.lotId,true INTO bound_item,bound_lot,scope_complete
    FROM mulino_work_read_Works w WHERE w.organizationId=org AND w.ID=bound_work
      AND w.createdAt<=stamp AND w.recordedAt<=stamp;
  ELSE
   -- Bind to the immutable source event, never to a later mutable response/draft Work.
   SELECT e.workId,e.itemId,e.subjectKind,e.subjectId INTO source_event
    FROM mulino_evidence_Events e JOIN mulino_evidence_InboxRecords i
      ON i.organizationId=e.organizationId AND i.eventId=e.ID
    WHERE i.organizationId=org AND i.ID=source AND e.ID=body->>'eventid'
      AND e.recordedAt<=stamp AND i.recordedAt<=stamp;
   IF FOUND THEN
    bound_work:=source_event.workId; bound_item:=source_event.itemId; scope_complete:=false;
    IF source_event.subjectKind='WORK' AND bound_work IS NULL THEN bound_work:=source_event.subjectId; END IF;
    IF source_event.subjectKind='SEGMENT' THEN
     SELECT s.itemId,s.lotId,(s.itemId IS NOT NULL AND (bound_item IS NULL OR s.itemId=bound_item))
      INTO bound_item,bound_lot,scope_complete
      FROM mulino_inventory_QuantitySegments s WHERE s.organizationId=org AND s.ID=source_event.subjectId AND s.recordedAt<=stamp;
    ELSIF source_event.subjectKind='LOT' THEN
     SELECT l.itemId,l.ID,(l.itemId IS NOT NULL AND (bound_item IS NULL OR l.itemId=bound_item))
      INTO bound_item,bound_lot,scope_complete
      FROM mulino_inventory_ManufacturingLots l
      WHERE l.organizationId=org AND l.ID=source_event.subjectId AND l.recordedAt<=stamp;
    ELSIF bound_work IS NOT NULL THEN
     SELECT w.itemId,w.lotId,(w.itemId IS NOT NULL AND (bound_item IS NULL OR w.itemId=bound_item))
      INTO bound_item,bound_lot,scope_complete
      FROM mulino_work_read_Works w WHERE w.organizationId=org AND w.ID=bound_work AND w.createdAt<=stamp AND w.recordedAt<=stamp;
    ELSE
     -- Other subject relations need their own proof; an absent binding is never an empty LOT fact.
     IF source_event.subjectKind='ITEM' THEN
      SELECT i.ID,(bound_item IS NULL OR i.ID=bound_item) INTO bound_item,scope_complete
       FROM mulino_inventory_TradeItems i WHERE i.organizationId=org AND i.ID=source_event.subjectId AND i.recordedAt<=stamp;
     END IF;
    END IF;
    IF (source_event.subjectKind='WORK' AND source_event.subjectId IS DISTINCT FROM bound_work)
       OR (source_event.subjectKind='ITEM' AND source_event.subjectId IS DISTINCT FROM bound_item)
       OR (source_event.subjectKind='LOT' AND source_event.subjectId IS DISTINCT FROM bound_lot) THEN
     scope_complete:=false;
    END IF;
   END IF;
  END IF;
 END IF;
 INSERT INTO mulino_runtime_RecoveryObservationHistory
  (organizationId,sourceTable,sourceId,sourceRevision,recordedAt,transactionId,operation,
   historyComplete,scopeComplete,workId,itemId,lotId,snapshotJson)
 VALUES(org,TG_TABLE_NAME,source,(body->>'revision')::integer,stamp,txid_current()::text,TG_OP,
  complete,coalesce(scope_complete,false),bound_work,bound_item,bound_lot,body::text);
 RETURN CASE WHEN TG_OP='DELETE' THEN OLD ELSE NEW END;
END $$;
CREATE TRIGGER recovery_schedule_history AFTER INSERT OR UPDATE OR DELETE
 ON mulino_runtime_RecoverySchedules FOR EACH ROW EXECUTE FUNCTION mulino_record_recovery_history();
CREATE TRIGGER recovery_intake_history AFTER INSERT OR UPDATE OR DELETE
 ON mulino_runtime_IntakeRecoveries FOR EACH ROW EXECUTE FUNCTION mulino_record_recovery_history();
CREATE FUNCTION mulino_recovery_no_truncate() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE populated boolean;
BEGIN
 EXECUTE format('SELECT EXISTS (SELECT 1 FROM %I.%I)',TG_TABLE_SCHEMA,TG_TABLE_NAME) INTO populated;
 IF populated THEN RAISE EXCEPTION 'Recovery sources require row deletes with history'; END IF;
 RETURN NULL;
END $$;
CREATE TRIGGER recovery_schedule_no_truncate BEFORE TRUNCATE ON mulino_runtime_RecoverySchedules
 FOR EACH STATEMENT EXECUTE FUNCTION mulino_recovery_no_truncate();
CREATE TRIGGER recovery_intake_no_truncate BEFORE TRUNCATE ON mulino_runtime_IntakeRecoveries
 FOR EACH STATEMENT EXECUTE FUNCTION mulino_recovery_no_truncate();
