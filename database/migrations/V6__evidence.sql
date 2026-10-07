-- Flyway owns DDL; CDS owns the CQN persistence names. Immutable evidence is append-only.

CREATE TABLE mulino_evidence_SourceProfiles (
  ID VARCHAR(36) NOT NULL,
  organizationId VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL,
  recordedAt TIMESTAMP WITH TIME ZONE NOT NULL,
  recordedBy VARCHAR(36) NOT NULL,
  namespace VARCHAR(160),
  policyVersion VARCHAR(160),
  intakeOwnerId VARCHAR(36),
  supervisorId VARCHAR(36),
  nextAction VARCHAR(320),
  nextCheckAt TIMESTAMP WITH TIME ZONE,
  PRIMARY KEY(ID),
  UNIQUE(organizationId,ID),
  FOREIGN KEY(organizationId) REFERENCES mulino_identity_Organizations(ID),
  FOREIGN KEY(organizationId,recordedBy) REFERENCES mulino_identity_Actors(organizationId,ID),
  CHECK(revision >= 1)
);

CREATE INDEX ON mulino_evidence_SourceProfiles(organizationId,recordedAt,ID);

CREATE TABLE mulino_evidence_DocumentVersions (
  ID VARCHAR(36) NOT NULL,
  organizationId VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL,
  recordedAt TIMESTAMP WITH TIME ZONE NOT NULL,
  recordedBy VARCHAR(36) NOT NULL,
  subjectKind VARCHAR(20),
  subjectId VARCHAR(36),
  itemId VARCHAR(36),
  placeId VARCHAR(36),
  workId VARCHAR(36),
  sha256 VARCHAR(64),
  blobId VARCHAR(36),
  byteLength BIGINT,
  mediaType VARCHAR(160),
  sourceNamespace VARCHAR(160),
  sourceProfileId VARCHAR(36),
  sourceReference VARCHAR(320),
  availability VARCHAR(24),
  supersedesId VARCHAR(36),
  provenance TEXT,
  PRIMARY KEY(ID),
  UNIQUE(organizationId,ID),
  FOREIGN KEY(organizationId) REFERENCES mulino_identity_Organizations(ID),
  FOREIGN KEY(organizationId,recordedBy) REFERENCES mulino_identity_Actors(organizationId,ID),
  CHECK(revision >= 1)
);

ALTER TABLE mulino_evidence_DocumentVersions ADD FOREIGN KEY(organizationId,sourceProfileId) REFERENCES mulino_evidence_SourceProfiles(organizationId,ID);

ALTER TABLE mulino_evidence_DocumentVersions ADD FOREIGN KEY(organizationId,supersedesId) REFERENCES mulino_evidence_DocumentVersions(organizationId,ID);

ALTER TABLE mulino_evidence_DocumentVersions ADD CHECK(subjectKind IN ('ITEM','LOT','SEGMENT','WORK','PLACE')), ADD CHECK(subjectId IS NOT NULL);

CREATE INDEX ON mulino_evidence_DocumentVersions(organizationId,recordedAt,ID);

CREATE TABLE mulino_evidence_Events (
  ID VARCHAR(36) NOT NULL,
  organizationId VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL,
  recordedAt TIMESTAMP WITH TIME ZONE NOT NULL,
  recordedBy VARCHAR(36) NOT NULL,
  subjectKind VARCHAR(20),
  subjectId VARCHAR(36),
  itemId VARCHAR(36),
  placeId VARCHAR(36),
  workId VARCHAR(36),
  effectiveFrom TIMESTAMP WITH TIME ZONE,
  effectiveUntil TIMESTAMP WITH TIME ZONE,
  timeZone VARCHAR(80),
  timePrecision VARCHAR(20),
  valueState VARCHAR(24),
  kind VARCHAR(80),
  sourceNamespace VARCHAR(160),
  sourceProfileId VARCHAR(36),
  externalEventId VARCHAR(160),
  sourceVersion VARCHAR(80),
  payloadHash VARCHAR(64),
  payload TEXT,
  supersedesId VARCHAR(36),
  invalidatesId VARCHAR(36),
  PRIMARY KEY(ID),
  UNIQUE(organizationId,ID),
  FOREIGN KEY(organizationId) REFERENCES mulino_identity_Organizations(ID),
  FOREIGN KEY(organizationId,recordedBy) REFERENCES mulino_identity_Actors(organizationId,ID),
  CHECK(revision >= 1)
);

ALTER TABLE mulino_evidence_Events ADD FOREIGN KEY(organizationId,sourceProfileId) REFERENCES mulino_evidence_SourceProfiles(organizationId,ID);

ALTER TABLE mulino_evidence_Events ADD FOREIGN KEY(organizationId,supersedesId) REFERENCES mulino_evidence_Events(organizationId,ID);

ALTER TABLE mulino_evidence_Events ADD FOREIGN KEY(organizationId,invalidatesId) REFERENCES mulino_evidence_Events(organizationId,ID);

ALTER TABLE mulino_evidence_Events ADD CHECK(valueState IN ('KNOWN','MISSING','UNKNOWN','NOT_APPLICABLE','CONFLICT')), ADD CHECK(effectiveUntil IS NULL OR effectiveUntil > effectiveFrom), ADD CHECK(timePrecision IN ('INSTANT','SECOND','MINUTE','DAY','RANGE'));

ALTER TABLE mulino_evidence_Events ADD CHECK(subjectKind IN ('ITEM','LOT','SEGMENT','WORK','PLACE')), ADD CHECK(subjectId IS NOT NULL);

CREATE INDEX ON mulino_evidence_Events(organizationId,recordedAt,ID);

CREATE TABLE mulino_evidence_Claims (
  ID VARCHAR(36) NOT NULL,
  organizationId VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL,
  recordedAt TIMESTAMP WITH TIME ZONE NOT NULL,
  recordedBy VARCHAR(36) NOT NULL,
  subjectKind VARCHAR(20),
  subjectId VARCHAR(36),
  itemId VARCHAR(36),
  placeId VARCHAR(36),
  workId VARCHAR(36),
  effectiveFrom TIMESTAMP WITH TIME ZONE,
  effectiveUntil TIMESTAMP WITH TIME ZONE,
  timeZone VARCHAR(80),
  timePrecision VARCHAR(20),
  valueState VARCHAR(24),
  eventId VARCHAR(36),
  documentVersionId VARCHAR(36),
  assertion TEXT,
  quantity DECIMAL(38,12),
  unit VARCHAR(24),
  supersedesId VARCHAR(36),
  PRIMARY KEY(ID),
  UNIQUE(organizationId,ID),
  FOREIGN KEY(organizationId) REFERENCES mulino_identity_Organizations(ID),
  FOREIGN KEY(organizationId,recordedBy) REFERENCES mulino_identity_Actors(organizationId,ID),
  CHECK(revision >= 1)
);

ALTER TABLE mulino_evidence_Claims ADD FOREIGN KEY(organizationId,supersedesId) REFERENCES mulino_evidence_Claims(organizationId,ID);

ALTER TABLE mulino_evidence_Claims ADD FOREIGN KEY(organizationId,eventId) REFERENCES mulino_evidence_Events(organizationId,ID);

ALTER TABLE mulino_evidence_Claims ADD FOREIGN KEY(organizationId,documentVersionId) REFERENCES mulino_evidence_DocumentVersions(organizationId,ID);

ALTER TABLE mulino_evidence_Claims ADD CHECK(valueState IN ('KNOWN','MISSING','UNKNOWN','NOT_APPLICABLE','CONFLICT')), ADD CHECK(effectiveUntil IS NULL OR effectiveUntil > effectiveFrom), ADD CHECK(timePrecision IN ('INSTANT','SECOND','MINUTE','DAY','RANGE'));

ALTER TABLE mulino_evidence_Claims ADD CHECK(subjectKind IN ('ITEM','LOT','SEGMENT','WORK','PLACE')), ADD CHECK(subjectId IS NOT NULL);

ALTER TABLE mulino_evidence_Claims ADD CHECK(quantity IS NULL OR (quantity >= 0 AND unit IS NOT NULL));

CREATE INDEX ON mulino_evidence_Claims(organizationId,recordedAt,ID);

CREATE TABLE mulino_evidence_InboxRecords (
  ID VARCHAR(36) NOT NULL,
  organizationId VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL,
  recordedAt TIMESTAMP WITH TIME ZONE NOT NULL,
  recordedBy VARCHAR(36) NOT NULL,
  subjectKind VARCHAR(20),
  subjectId VARCHAR(36),
  itemId VARCHAR(36),
  placeId VARCHAR(36),
  workId VARCHAR(36),
  eventId VARCHAR(36),
  sourceNamespace VARCHAR(160),
  sourceProfileId VARCHAR(36),
  externalEventId VARCHAR(160),
  sourceVersion VARCHAR(80),
  payloadHash VARCHAR(64),
  state VARCHAR(24),
  conflictsWithId VARCHAR(36),
  intakeOwnerId VARCHAR(36),
  supervisorId VARCHAR(36),
  nextAction VARCHAR(320),
  nextCheckAt TIMESTAMP WITH TIME ZONE,
  PRIMARY KEY(ID),
  UNIQUE(organizationId,ID),
  FOREIGN KEY(organizationId) REFERENCES mulino_identity_Organizations(ID),
  FOREIGN KEY(organizationId,recordedBy) REFERENCES mulino_identity_Actors(organizationId,ID),
  CHECK(revision >= 1)
);

ALTER TABLE mulino_evidence_InboxRecords ADD FOREIGN KEY(organizationId,sourceProfileId) REFERENCES mulino_evidence_SourceProfiles(organizationId,ID);

ALTER TABLE mulino_evidence_InboxRecords ADD FOREIGN KEY(organizationId,eventId) REFERENCES mulino_evidence_Events(organizationId,ID);

ALTER TABLE mulino_evidence_InboxRecords ADD FOREIGN KEY(organizationId,conflictsWithId) REFERENCES mulino_evidence_InboxRecords(organizationId,ID);

ALTER TABLE mulino_evidence_InboxRecords ADD CHECK(subjectKind IN ('ITEM','LOT','SEGMENT','WORK','PLACE')), ADD CHECK(subjectId IS NOT NULL);

CREATE INDEX ON mulino_evidence_InboxRecords(organizationId,recordedAt,ID);

CREATE TABLE mulino_evidence_CanonicalOccurrences (
  ID VARCHAR(36) NOT NULL,
  organizationId VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL,
  recordedAt TIMESTAMP WITH TIME ZONE NOT NULL,
  recordedBy VARCHAR(36) NOT NULL,
  subjectKind VARCHAR(20),
  subjectId VARCHAR(36),
  itemId VARCHAR(36),
  placeId VARCHAR(36),
  workId VARCHAR(36),
  effectiveFrom TIMESTAMP WITH TIME ZONE,
  effectiveUntil TIMESTAMP WITH TIME ZONE,
  timeZone VARCHAR(80),
  timePrecision VARCHAR(20),
  valueState VARCHAR(24),
  kind VARCHAR(80),
  physicalScopeId VARCHAR(36),
  quantity DECIMAL(38,12),
  unit VARCHAR(24),
  supersedesId VARCHAR(36),
  reassessmentState VARCHAR(24),
  PRIMARY KEY(ID),
  UNIQUE(organizationId,ID),
  FOREIGN KEY(organizationId) REFERENCES mulino_identity_Organizations(ID),
  FOREIGN KEY(organizationId,recordedBy) REFERENCES mulino_identity_Actors(organizationId,ID),
  CHECK(revision >= 1)
);

ALTER TABLE mulino_evidence_CanonicalOccurrences ADD FOREIGN KEY(organizationId,supersedesId) REFERENCES mulino_evidence_CanonicalOccurrences(organizationId,ID);

ALTER TABLE mulino_evidence_CanonicalOccurrences ADD CHECK(valueState IN ('KNOWN','MISSING','UNKNOWN','NOT_APPLICABLE','CONFLICT')), ADD CHECK(effectiveUntil IS NULL OR effectiveUntil > effectiveFrom), ADD CHECK(timePrecision IN ('INSTANT','SECOND','MINUTE','DAY','RANGE'));

ALTER TABLE mulino_evidence_CanonicalOccurrences ADD CHECK(subjectKind IN ('ITEM','LOT','SEGMENT','WORK','PLACE')), ADD CHECK(subjectId IS NOT NULL);

ALTER TABLE mulino_evidence_CanonicalOccurrences ADD CHECK(quantity IS NULL OR (quantity >= 0 AND unit IS NOT NULL));

CREATE INDEX ON mulino_evidence_CanonicalOccurrences(organizationId,recordedAt,ID);

CREATE TABLE mulino_evidence_Verifications (
  ID VARCHAR(36) NOT NULL,
  organizationId VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL,
  recordedAt TIMESTAMP WITH TIME ZONE NOT NULL,
  recordedBy VARCHAR(36) NOT NULL,
  claimId VARCHAR(36),
  canonicalOccurrenceId VARCHAR(36),
  basisDocumentId VARCHAR(36),
  policyVersion VARCHAR(160),
  sourceMatched BOOLEAN,
  identityMatched BOOLEAN,
  quantityMatched BOOLEAN,
  timeMatched BOOLEAN,
  duplicateChecked BOOLEAN,
  verdict VARCHAR(24),
  reason VARCHAR(640),
  PRIMARY KEY(ID),
  UNIQUE(organizationId,ID),
  FOREIGN KEY(organizationId) REFERENCES mulino_identity_Organizations(ID),
  FOREIGN KEY(organizationId,recordedBy) REFERENCES mulino_identity_Actors(organizationId,ID),
  CHECK(revision >= 1)
);

ALTER TABLE mulino_evidence_Verifications ADD FOREIGN KEY(organizationId,claimId) REFERENCES mulino_evidence_Claims(organizationId,ID);

ALTER TABLE mulino_evidence_Verifications ADD FOREIGN KEY(organizationId,basisDocumentId) REFERENCES mulino_evidence_DocumentVersions(organizationId,ID);

ALTER TABLE mulino_evidence_Verifications ADD FOREIGN KEY(organizationId,canonicalOccurrenceId) REFERENCES mulino_evidence_CanonicalOccurrences(organizationId,ID);

CREATE INDEX ON mulino_evidence_Verifications(organizationId,recordedAt,ID);

CREATE TABLE mulino_evidence_EvidenceLinks (
  ID VARCHAR(36) NOT NULL,
  organizationId VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL,
  recordedAt TIMESTAMP WITH TIME ZONE NOT NULL,
  recordedBy VARCHAR(36) NOT NULL,
  documentVersionId VARCHAR(36),
  eventId VARCHAR(36),
  claimId VARCHAR(36),
  canonicalOccurrenceId VARCHAR(36),
  role VARCHAR(40),
  PRIMARY KEY(ID),
  UNIQUE(organizationId,ID),
  FOREIGN KEY(organizationId) REFERENCES mulino_identity_Organizations(ID),
  FOREIGN KEY(organizationId,recordedBy) REFERENCES mulino_identity_Actors(organizationId,ID),
  CHECK(revision >= 1)
);

ALTER TABLE mulino_evidence_EvidenceLinks ADD FOREIGN KEY(organizationId,eventId) REFERENCES mulino_evidence_Events(organizationId,ID);

ALTER TABLE mulino_evidence_EvidenceLinks ADD FOREIGN KEY(organizationId,claimId) REFERENCES mulino_evidence_Claims(organizationId,ID);

ALTER TABLE mulino_evidence_EvidenceLinks ADD FOREIGN KEY(organizationId,documentVersionId) REFERENCES mulino_evidence_DocumentVersions(organizationId,ID);

ALTER TABLE mulino_evidence_EvidenceLinks ADD FOREIGN KEY(organizationId,canonicalOccurrenceId) REFERENCES mulino_evidence_CanonicalOccurrences(organizationId,ID);

CREATE INDEX ON mulino_evidence_EvidenceLinks(organizationId,recordedAt,ID);

ALTER TABLE mulino_evidence_SourceProfiles ADD UNIQUE(organizationId,namespace), ADD FOREIGN KEY(organizationId,intakeOwnerId) REFERENCES mulino_identity_Actors(organizationId,ID), ADD FOREIGN KEY(organizationId,supervisorId) REFERENCES mulino_identity_Actors(organizationId,ID), ADD CHECK(intakeOwnerId IS NOT NULL AND supervisorId IS NOT NULL AND nextAction IS NOT NULL AND nextCheckAt IS NOT NULL AND policyVersion IS NOT NULL);

ALTER TABLE mulino_evidence_DocumentVersions ADD CHECK(sha256 ~ '^[0-9a-f]{64}$'), ADD CHECK(byteLength >= 0), ADD CHECK(availability IN ('AVAILABLE','UNKNOWN')), ADD CHECK((availability='AVAILABLE' AND blobId IS NOT NULL) OR (availability='UNKNOWN' AND blobId IS NULL));

ALTER TABLE mulino_evidence_InboxRecords ADD CHECK(state IN ('RECEIVED','CONFLICT')), ADD FOREIGN KEY(organizationId,intakeOwnerId) REFERENCES mulino_identity_Actors(organizationId,ID), ADD FOREIGN KEY(organizationId,supervisorId) REFERENCES mulino_identity_Actors(organizationId,ID);

CREATE UNIQUE INDEX evidence_source_hash ON mulino_evidence_InboxRecords(organizationId,sourceNamespace,externalEventId,sourceVersion,payloadHash);

ALTER TABLE mulino_evidence_Verifications ADD CHECK(verdict IN ('VERIFIED','REJECTED','UNKNOWN','CONFLICT')), ADD CHECK(verdict <> 'VERIFIED' OR (sourceMatched AND identityMatched AND quantityMatched AND timeMatched AND duplicateChecked AND canonicalOccurrenceId IS NOT NULL AND basisDocumentId IS NOT NULL));

ALTER TABLE mulino_evidence_EvidenceLinks ADD CHECK(documentVersionId IS NOT NULL), ADD CHECK(num_nonnulls(eventId,claimId,canonicalOccurrenceId)=1);

ALTER TABLE mulino_evidence_CanonicalOccurrences ADD CHECK(reassessmentState IN ('NOT_IMPLEMENTED','PENDING','COMPLETE'));

CREATE OR REPLACE FUNCTION mulino_evidence_reject_mutation() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'Immutable evidence cannot be updated or deleted' USING ERRCODE='23514'; END $$;

CREATE TRIGGER evidence_immutable BEFORE UPDATE OR DELETE ON mulino_evidence_SourceProfiles FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();

CREATE TRIGGER evidence_immutable BEFORE UPDATE OR DELETE ON mulino_evidence_DocumentVersions FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();

CREATE TRIGGER evidence_immutable BEFORE UPDATE OR DELETE ON mulino_evidence_Events FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();

CREATE TRIGGER evidence_immutable BEFORE UPDATE OR DELETE ON mulino_evidence_Claims FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();

CREATE TRIGGER evidence_immutable BEFORE UPDATE OR DELETE ON mulino_evidence_InboxRecords FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();

CREATE TRIGGER evidence_immutable BEFORE UPDATE OR DELETE ON mulino_evidence_CanonicalOccurrences FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();

CREATE TRIGGER evidence_immutable BEFORE UPDATE OR DELETE ON mulino_evidence_Verifications FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();

CREATE TRIGGER evidence_immutable BEFORE UPDATE OR DELETE ON mulino_evidence_EvidenceLinks FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();

ALTER TABLE mulino_evidence_DocumentVersions
  ALTER COLUMN sha256 SET NOT NULL, ALTER COLUMN byteLength SET NOT NULL,
  ALTER COLUMN mediaType SET NOT NULL, ALTER COLUMN availability SET NOT NULL,
  ALTER COLUMN sourceProfileId SET NOT NULL, ALTER COLUMN provenance SET NOT NULL;
ALTER TABLE mulino_evidence_Events
  ALTER COLUMN effectiveFrom SET NOT NULL, ALTER COLUMN timeZone SET NOT NULL,
  ALTER COLUMN timePrecision SET NOT NULL, ALTER COLUMN valueState SET NOT NULL,
  ALTER COLUMN sourceNamespace SET NOT NULL, ALTER COLUMN sourceProfileId SET NOT NULL,
  ALTER COLUMN externalEventId SET NOT NULL, ALTER COLUMN sourceVersion SET NOT NULL,
  ALTER COLUMN payloadHash SET NOT NULL, ALTER COLUMN payload SET NOT NULL;
ALTER TABLE mulino_evidence_Claims
  ALTER COLUMN eventId SET NOT NULL, ALTER COLUMN documentVersionId SET NOT NULL,
  ALTER COLUMN effectiveFrom SET NOT NULL, ALTER COLUMN valueState SET NOT NULL;
ALTER TABLE mulino_evidence_InboxRecords
  ALTER COLUMN eventId SET NOT NULL, ALTER COLUMN sourceProfileId SET NOT NULL,
  ALTER COLUMN state SET NOT NULL, ALTER COLUMN intakeOwnerId SET NOT NULL,
  ALTER COLUMN supervisorId SET NOT NULL, ALTER COLUMN nextAction SET NOT NULL,
  ALTER COLUMN nextCheckAt SET NOT NULL;
ALTER TABLE mulino_evidence_CanonicalOccurrences
  ALTER COLUMN physicalScopeId SET NOT NULL, ALTER COLUMN effectiveFrom SET NOT NULL,
  ALTER COLUMN valueState SET NOT NULL, ALTER COLUMN reassessmentState SET NOT NULL;
ALTER TABLE mulino_evidence_Verifications
  ALTER COLUMN claimId SET NOT NULL, ALTER COLUMN policyVersion SET NOT NULL,
  ALTER COLUMN verdict SET NOT NULL, ALTER COLUMN reason SET NOT NULL;
ALTER TABLE mulino_evidence_Claims ADD CHECK(valueState='KNOWN' OR quantity IS NULL);
