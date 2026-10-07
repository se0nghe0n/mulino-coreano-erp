
CREATE TABLE mulino_work_read_Works (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 0,
  createdAt TIMESTAMPTZ NOT NULL,
  recordedAt TIMESTAMPTZ NOT NULL,
  effectiveAt TIMESTAMPTZ NOT NULL,
  itemId VARCHAR(36) NOT NULL,
  lotId VARCHAR(36),
  definitionVersionId VARCHAR(36) NOT NULL,
  kind VARCHAR(80) NOT NULL,
  status VARCHAR(40) NOT NULL,
  ownerId VARCHAR(36) NOT NULL,
  supervisorId VARCHAR(36) NOT NULL,
  waitJson TEXT,
  PRIMARY KEY(organizationId, ID)
);

CREATE TABLE mulino_work_read_GoalReferences (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 0,
  createdAt TIMESTAMPTZ NOT NULL,
  recordedAt TIMESTAMPTZ NOT NULL,
  effectiveAt TIMESTAMPTZ NOT NULL,
  workId VARCHAR(36) NOT NULL,
  definitionVersionId VARCHAR(36) NOT NULL,
  quantityMode VARCHAR(40) NOT NULL,
  targetQuantity DECIMAL(38, 12),
  unit VARCHAR(20),
  endpoint VARCHAR(80),
  scopeJson TEXT NOT NULL,
  PRIMARY KEY(organizationId, ID)
);

CREATE TABLE mulino_work_read_AssessmentReferences (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 0,
  createdAt TIMESTAMPTZ NOT NULL,
  recordedAt TIMESTAMPTZ NOT NULL,
  effectiveAt TIMESTAMPTZ NOT NULL,
  workId VARCHAR(36) NOT NULL,
  goalId VARCHAR(36) NOT NULL,
  outcome VARCHAR(40) NOT NULL,
  evaluatorVersion VARCHAR(80) NOT NULL,
  assessedAt TIMESTAMPTZ NOT NULL,
  conditionsJson TEXT NOT NULL,
  PRIMARY KEY(organizationId, ID)
);

CREATE TABLE mulino_work_read_ObligationReferences (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 0,
  createdAt TIMESTAMPTZ NOT NULL,
  recordedAt TIMESTAMPTZ NOT NULL,
  effectiveAt TIMESTAMPTZ NOT NULL,
  workId VARCHAR(36) NOT NULL,
  kind VARCHAR(80) NOT NULL,
  status VARCHAR(40) NOT NULL,
  ownerId VARCHAR(36) NOT NULL,
  supervisorId VARCHAR(36) NOT NULL,
  nextAction VARCHAR(500) NOT NULL,
  nextCheckAt TIMESTAMPTZ NOT NULL,
  quantity DECIMAL(38, 12),
  unit VARCHAR(20),
  scopeJson TEXT NOT NULL,
  PRIMARY KEY(organizationId, ID)
);

CREATE TABLE mulino_work_read_SubjectLinks (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 0,
  createdAt TIMESTAMPTZ NOT NULL,
  recordedAt TIMESTAMPTZ NOT NULL,
  effectiveAt TIMESTAMPTZ NOT NULL,
  workId VARCHAR(36) NOT NULL,
  itemId VARCHAR(36) NOT NULL,
  lotId VARCHAR(36),
  PRIMARY KEY(organizationId, ID)
);

CREATE TABLE mulino_work_read_EvidenceReferences (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 0,
  createdAt TIMESTAMPTZ NOT NULL,
  recordedAt TIMESTAMPTZ NOT NULL,
  effectiveAt TIMESTAMPTZ NOT NULL,
  workId VARCHAR(36) NOT NULL,
  documentVersionId VARCHAR(36) NOT NULL,
  role VARCHAR(80) NOT NULL,
  PRIMARY KEY(organizationId, ID)
);


-- S1 read-facing imports reference the same organization, item, definition and human owners.
ALTER TABLE mulino_work_read_Works
  ADD FOREIGN KEY (organizationId) REFERENCES mulino_identity_Organizations(ID),
  ADD FOREIGN KEY (organizationId,itemId) REFERENCES mulino_inventory_TradeItems(organizationId,ID),
  ADD FOREIGN KEY (organizationId,lotId,itemId) REFERENCES mulino_inventory_ManufacturingLots(organizationId,ID,itemId),
  ADD FOREIGN KEY (organizationId,definitionVersionId) REFERENCES mulino_definitions_DefinitionVersions(organizationId,ID),
  ADD FOREIGN KEY (organizationId,ownerId) REFERENCES mulino_identity_Actors(organizationId,ID),
  ADD FOREIGN KEY (organizationId,supervisorId) REFERENCES mulino_identity_Actors(organizationId,ID),
  ADD CHECK (status IN ('DRAFT','READY','RUNNING','WAITING','FULFILLED','CANCELLED','FAILED','CLOSED')),
  ADD CHECK (revision >= 0),
  ADD CHECK (waitJson IS NULL OR jsonb_typeof(waitJson::jsonb)='object');
ALTER TABLE mulino_work_read_GoalReferences
  ADD FOREIGN KEY (organizationId,workId) REFERENCES mulino_work_read_Works(organizationId,ID),
  ADD FOREIGN KEY (organizationId,definitionVersionId) REFERENCES mulino_definitions_DefinitionVersions(organizationId,ID),
  ADD CHECK (quantityMode IN ('CUMULATIVE_EVENT','STATE_AT','EXISTS_IN','THROUGHOUT')),
  ADD CHECK (targetQuantity IS NULL OR (targetQuantity >= 0 AND unit IS NOT NULL)),
  ADD CHECK (jsonb_typeof(scopeJson::jsonb)='object');
ALTER TABLE mulino_work_read_AssessmentReferences
  ADD FOREIGN KEY (organizationId,workId) REFERENCES mulino_work_read_Works(organizationId,ID),
  ADD FOREIGN KEY (organizationId,goalId) REFERENCES mulino_work_read_GoalReferences(organizationId,ID),
  ADD CHECK (outcome IN ('SATISFIED','UNSATISFIED','UNVERIFIED')),
  ADD CHECK (jsonb_typeof(conditionsJson::jsonb) IN ('object','array'));
ALTER TABLE mulino_work_read_ObligationReferences
  ADD FOREIGN KEY (organizationId,workId) REFERENCES mulino_work_read_Works(organizationId,ID),
  ADD FOREIGN KEY (organizationId,ownerId) REFERENCES mulino_identity_Actors(organizationId,ID),
  ADD FOREIGN KEY (organizationId,supervisorId) REFERENCES mulino_identity_Actors(organizationId,ID),
  ADD CHECK (status IN ('OPEN','RESOLVED','WAIVED','TRANSFERRED')),
  ADD CHECK (quantity IS NULL OR (quantity >= 0 AND unit IS NOT NULL)),
  ADD CHECK (jsonb_typeof(scopeJson::jsonb)='object');
ALTER TABLE mulino_work_read_SubjectLinks
  ADD FOREIGN KEY (organizationId,workId) REFERENCES mulino_work_read_Works(organizationId,ID),
  ADD FOREIGN KEY (organizationId,itemId) REFERENCES mulino_inventory_TradeItems(organizationId,ID),
  ADD FOREIGN KEY (organizationId,lotId,itemId) REFERENCES mulino_inventory_ManufacturingLots(organizationId,ID,itemId),
  ADD UNIQUE(organizationId,workId,itemId,lotId);
ALTER TABLE mulino_work_read_EvidenceReferences
  ADD FOREIGN KEY (organizationId,workId) REFERENCES mulino_work_read_Works(organizationId,ID),
  ADD FOREIGN KEY (organizationId,documentVersionId) REFERENCES mulino_evidence_DocumentVersions(organizationId,ID),
  ADD UNIQUE(organizationId,workId,documentVersionId,role);

CREATE INDEX work_read_scope ON mulino_work_read_Works(organizationId,itemId,lotId,ID);
CREATE INDEX obligation_read_work ON mulino_work_read_ObligationReferences(organizationId,workId,ID);
-- S1 accepts only immutable imported read facts; no lifecycle API exists yet.
CREATE FUNCTION mulino_work_read_immutable() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'S1 imported read references are immutable'; END $$;
CREATE TRIGGER work_read_immutable BEFORE UPDATE ON mulino_work_read_Works FOR EACH ROW EXECUTE FUNCTION mulino_work_read_immutable();
CREATE TRIGGER goal_read_immutable BEFORE UPDATE ON mulino_work_read_GoalReferences FOR EACH ROW EXECUTE FUNCTION mulino_work_read_immutable();
CREATE TRIGGER assessment_read_immutable BEFORE UPDATE ON mulino_work_read_AssessmentReferences FOR EACH ROW EXECUTE FUNCTION mulino_work_read_immutable();
CREATE TRIGGER obligation_read_immutable BEFORE UPDATE ON mulino_work_read_ObligationReferences FOR EACH ROW EXECUTE FUNCTION mulino_work_read_immutable();

ALTER TABLE mulino_work_read_GoalReferences ADD UNIQUE(organizationId,ID,workId);
ALTER TABLE mulino_work_read_AssessmentReferences ADD FOREIGN KEY(organizationId,goalId,workId) REFERENCES mulino_work_read_GoalReferences(organizationId,ID,workId);
ALTER TABLE mulino_work_read_Works ADD UNIQUE(organizationId,ID,itemId);
ALTER TABLE mulino_work_read_SubjectLinks ADD FOREIGN KEY(organizationId,workId,itemId) REFERENCES mulino_work_read_Works(organizationId,ID,itemId);

CREATE FUNCTION mulino_work_read_human_responsibility() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF NOT EXISTS(SELECT 1 FROM mulino_identity_Actors WHERE organizationId=NEW.organizationId AND ID=NEW.ownerId AND kind='HUMAN')
     OR NOT EXISTS(SELECT 1 FROM mulino_identity_Actors WHERE organizationId=NEW.organizationId AND ID=NEW.supervisorId AND kind='HUMAN')
  THEN RAISE EXCEPTION 'Human responsibility references required'; END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER work_read_human_owner BEFORE INSERT ON mulino_work_read_Works FOR EACH ROW EXECUTE FUNCTION mulino_work_read_human_responsibility();
CREATE TRIGGER obligation_read_human_owner BEFORE INSERT ON mulino_work_read_ObligationReferences FOR EACH ROW EXECUTE FUNCTION mulino_work_read_human_responsibility();
