-- No backfill: old assessment envelopes keep their exact bytes/hash and no capture.
CREATE TABLE mulino_evaluation_EvidenceCaptures (
 organizationId VARCHAR(36) NOT NULL REFERENCES mulino_identity_Organizations(ID),
 ID VARCHAR(36) NOT NULL,
 itemId VARCHAR(36) NOT NULL,
 lotId VARCHAR(36),
 placeId VARCHAR(36),
 workId VARCHAR(36),
 scopeJson TEXT NOT NULL CHECK(jsonb_typeof(scopeJson::jsonb)='object'),
 asOf TIMESTAMPTZ NOT NULL,
 knownAt TIMESTAMPTZ NOT NULL,
 evaluationHorizon TIMESTAMPTZ NOT NULL,
 admissionPurpose VARCHAR(40) NOT NULL,
 contentHash VARCHAR(64) NOT NULL,
 contentJson TEXT NOT NULL,
 recordedAt TIMESTAMPTZ NOT NULL,
 PRIMARY KEY(organizationId,ID),
 FOREIGN KEY(organizationId,itemId) REFERENCES mulino_inventory_TradeItems(organizationId,ID),
 FOREIGN KEY(organizationId,lotId) REFERENCES mulino_inventory_ManufacturingLots(organizationId,ID),
 FOREIGN KEY(organizationId,placeId) REFERENCES mulino_inventory_Places(organizationId,ID),
 FOREIGN KEY(organizationId,workId) REFERENCES mulino_work_read_Works(organizationId,ID),
 CHECK(scopeJson::jsonb->>'itemId'=itemId),
 CHECK((scopeJson::jsonb->>'lotId') IS NOT DISTINCT FROM lotId),
 CHECK((scopeJson::jsonb->>'placeId') IS NOT DISTINCT FROM placeId),
 CHECK((scopeJson::jsonb->>'workId') IS NOT DISTINCT FROM workId),
 CHECK((scopeJson::jsonb - ARRAY['itemId','lotId','placeId','workId'])='{}'::jsonb),
 UNIQUE(organizationId,contentHash),
 UNIQUE(organizationId,ID,asOf,knownAt),
 CHECK(contentHash=encode(sha256(convert_to(contentJson,'UTF8')),'hex')),
 CHECK(jsonb_typeof(contentJson::jsonb)='object'),
 CHECK(contentJson::jsonb->'boundary'->>'organizationId'=organizationId),
 CHECK(contentJson::jsonb->'boundary'->'scope'=scopeJson::jsonb),
 CHECK((contentJson::jsonb->'boundary'->>'asOf')::timestamptz=asOf),
 CHECK((contentJson::jsonb->'boundary'->>'knownAt')::timestamptz=knownAt),
 CHECK((contentJson::jsonb->'boundary'->>'evaluationHorizon')::timestamptz=evaluationHorizon),
 CHECK(contentJson::jsonb->'boundary'->>'admissionPurpose'=admissionPurpose),
 CHECK(jsonb_typeof(contentJson::jsonb->'facts')='object'),
 CHECK((contentJson::jsonb->'facts') ?& ARRAY['properties','relations','sources']),
 CHECK(jsonb_typeof(contentJson::jsonb->'facts'->'properties')='object' AND jsonb_typeof(contentJson::jsonb->'facts'->'relations')='object' AND jsonb_typeof(contentJson::jsonb->'facts'->'sources')='object'),
 CHECK(contentJson::jsonb ? 'boundary' AND contentJson::jsonb ? 'facts'),
 CHECK(jsonb_typeof(contentJson::jsonb->'boundary') IS NOT DISTINCT FROM 'object'),
 CHECK((contentJson::jsonb->'boundary') ?& ARRAY['organizationId','scope','asOf','knownAt','evaluationHorizon','admissionPurpose','admissionContract','definitionHash','policyHash']),
 CHECK(NOT jsonb_path_exists(contentJson::jsonb->'boundary', '$.* ? (@ == null)'))
);
CREATE FUNCTION mulino_evaluation_capture_source_scope() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE source_row jsonb;
BEGIN
 IF NEW.lotId IS NOT NULL AND NOT EXISTS(SELECT 1 FROM mulino_inventory_ManufacturingLots WHERE organizationId=NEW.organizationId AND ID=NEW.lotId AND itemId=NEW.itemId) THEN RAISE EXCEPTION 'Capture LOT/item mismatch' USING ERRCODE='23514'; END IF;
 IF NEW.workId IS NOT NULL AND NOT EXISTS(SELECT 1 FROM mulino_work_read_Works WHERE organizationId=NEW.organizationId AND ID=NEW.workId AND itemId=NEW.itemId) THEN RAISE EXCEPTION 'Capture Work/item mismatch' USING ERRCODE='23514'; END IF;
 FOR source_row IN SELECT entries.value FROM jsonb_each(NEW.contentJson::jsonb->'facts'->'sources') sources, LATERAL jsonb_array_elements(sources.value) entries LOOP
  IF source_row ? 'organizationId' AND source_row->>'organizationId' IS DISTINCT FROM NEW.organizationId THEN
   RAISE EXCEPTION 'Foreign organization in evidence capture sources' USING ERRCODE='23514';
  END IF;
  IF source_row ? 'recordedAt' AND (source_row->>'recordedAt')::timestamptz>NEW.knownAt THEN RAISE EXCEPTION 'Future source in evidence capture' USING ERRCODE='23514'; END IF;
 END LOOP;
 FOR source_row IN SELECT entries.value FROM jsonb_each(NEW.contentJson::jsonb->'facts'->'properties') properties, LATERAL jsonb_array_elements(properties.value) entries LOOP
  IF (source_row->>'recordedAt') IS NULL OR (source_row->>'recordedAt')::timestamptz>NEW.knownAt THEN RAISE EXCEPTION 'Unbounded Fact observation time in evidence capture' USING ERRCODE='23514'; END IF;
 END LOOP;
 RETURN NEW;
END $$;
CREATE TRIGGER evidence_capture_source_scope BEFORE INSERT ON mulino_evaluation_EvidenceCaptures
 FOR EACH ROW EXECUTE FUNCTION mulino_evaluation_capture_source_scope();
CREATE TRIGGER evidence_capture_immutable BEFORE UPDATE OR DELETE ON mulino_evaluation_EvidenceCaptures
 FOR EACH ROW EXECUTE FUNCTION mulino_work_read_immutable();
ALTER TABLE mulino_evaluation_InputSnapshots ADD COLUMN evidenceCaptureId VARCHAR(36),
 ADD FOREIGN KEY(organizationId,evidenceCaptureId,asOf,knownAt)
 REFERENCES mulino_evaluation_EvidenceCaptures(organizationId,ID,asOf,knownAt);
CREATE FUNCTION mulino_evaluation_capture_binding() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE capture mulino_evaluation_EvidenceCaptures%ROWTYPE; assessment mulino_work_read_AssessmentReferences%ROWTYPE;
BEGIN
 IF NEW.evidenceCaptureId IS NULL THEN
  IF NEW.contentJson::jsonb ? 'captureBoundary' THEN RAISE EXCEPTION 'Missing evidence capture binding' USING ERRCODE='23514'; END IF;
  RETURN NEW;
 END IF;
 SELECT * INTO capture FROM mulino_evaluation_EvidenceCaptures WHERE organizationId=NEW.organizationId AND ID=NEW.evidenceCaptureId;
 SELECT * INTO assessment FROM mulino_work_read_AssessmentReferences WHERE organizationId=NEW.organizationId AND ID=NEW.assessmentId;
 IF capture.ID IS NULL OR assessment.ID IS NULL
  OR NEW.contentHash IS DISTINCT FROM encode(sha256(convert_to(NEW.contentJson,'UTF8')),'hex')
  OR NEW.contentHash IS DISTINCT FROM assessment.inputSnapshotHash
  OR NEW.workId IS DISTINCT FROM assessment.workId OR NEW.goalId IS DISTINCT FROM assessment.goalId
  OR NEW.asOf IS DISTINCT FROM assessment.asOf OR NEW.knownAt IS DISTINCT FROM assessment.knownAt
  OR NEW.contentJson::jsonb->'captureBoundary' IS DISTINCT FROM capture.contentJson::jsonb->'boundary'
  OR NEW.contentJson::jsonb->'facts' IS DISTINCT FROM capture.contentJson::jsonb->'facts'
  OR NEW.contentJson::jsonb->>'goalId' IS DISTINCT FROM NEW.goalId
  OR NEW.contentJson::jsonb->'goal'->>'ID' IS DISTINCT FROM NEW.goalId
  OR NEW.contentJson::jsonb->'goal'->>'workId' IS DISTINCT FROM NEW.workId
  OR NEW.contentJson::jsonb->'goal'->>'organizationId' IS DISTINCT FROM NEW.organizationId
  OR NEW.contentJson::jsonb->'captureBoundary'->>'definitionId' IS DISTINCT FROM assessment.definitionVersionId
  OR NEW.contentJson::jsonb->'captureBoundary'->>'policyId' IS DISTINCT FROM assessment.policyVersionId
  OR NEW.contentJson::jsonb->'conditions' IS DISTINCT FROM assessment.conditionsJson::jsonb THEN
  RAISE EXCEPTION 'Immutable evidence capture/envelope binding differs' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER evidence_capture_binding BEFORE INSERT ON mulino_evaluation_InputSnapshots
 FOR EACH ROW EXECUTE FUNCTION mulino_evaluation_capture_binding();
