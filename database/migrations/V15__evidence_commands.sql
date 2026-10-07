-- Review decisions preserve original observations and cannot execute physical movements.
CREATE TABLE mulino_evidence_Reconciliations (
 ID VARCHAR(36) PRIMARY KEY, organizationId VARCHAR(36) NOT NULL,
 revision INTEGER NOT NULL CHECK(revision >= 1), createdAt TIMESTAMPTZ NOT NULL,
 recordedAt TIMESTAMPTZ NOT NULL, recordedBy VARCHAR(36) NOT NULL,
 subjectKind VARCHAR(20) NOT NULL, subjectId VARCHAR(36) NOT NULL,
 itemId VARCHAR(36), placeId VARCHAR(36), workId VARCHAR(36),
 sourceProfileId VARCHAR(36) NOT NULL, claimId VARCHAR(36) NOT NULL,
 basisDocumentId VARCHAR(36) NOT NULL, physicalScopeId VARCHAR(36),
 existingCanonicalId VARCHAR(36), policyVersion VARCHAR(160) NOT NULL,
 decision VARCHAR(24) NOT NULL CHECK(decision IN ('MATCHED','UNVERIFIED','CONFLICT')),
 reason VARCHAR(640) NOT NULL, sourceIdentity VARCHAR(320) NOT NULL,
 quantity DECIMAL(38,12), unit VARCHAR(24), effectiveFrom TIMESTAMPTZ NOT NULL,
 intakeOwnerId VARCHAR(36) NOT NULL, supervisorId VARCHAR(36) NOT NULL,
 nextAction VARCHAR(320) NOT NULL, nextCheckAt TIMESTAMPTZ NOT NULL,
 UNIQUE(organizationId,ID),
 FOREIGN KEY(organizationId,recordedBy) REFERENCES mulino_identity_Actors(organizationId,ID),
 FOREIGN KEY(organizationId,claimId) REFERENCES mulino_evidence_Claims(organizationId,ID),
 FOREIGN KEY(organizationId,basisDocumentId) REFERENCES mulino_evidence_DocumentVersions(organizationId,ID),
 FOREIGN KEY(organizationId,sourceProfileId) REFERENCES mulino_evidence_SourceProfiles(organizationId,ID),
 FOREIGN KEY(organizationId,intakeOwnerId) REFERENCES mulino_identity_Actors(organizationId,ID),
 FOREIGN KEY(organizationId,supervisorId) REFERENCES mulino_identity_Actors(organizationId,ID),
 FOREIGN KEY(organizationId,existingCanonicalId) REFERENCES mulino_evidence_CanonicalOccurrences(organizationId,ID)
);
CREATE INDEX ON mulino_evidence_Reconciliations(organizationId,claimId,recordedAt);
CREATE TRIGGER evidence_reconciliation_immutable BEFORE UPDATE OR DELETE ON
 mulino_evidence_Reconciliations FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();

CREATE TABLE mulino_evidence_CompletionCoverages (
 ID varchar(36) PRIMARY KEY, organizationId varchar(36) NOT NULL,
 revision integer NOT NULL CHECK(revision>=1), createdAt timestamptz NOT NULL,
 recordedAt timestamptz NOT NULL, recordedBy varchar(36) NOT NULL,
 occurrenceId varchar(36) NOT NULL, claimId varchar(36) NOT NULL,
 eventId varchar(36) NOT NULL, verificationId varchar(36) NOT NULL,
 documentVersionId varchar(36) NOT NULL, rootId varchar(36) NOT NULL,
 startQuantity numeric(38,12) NOT NULL CHECK(startQuantity>=0),
 quantity numeric(38,12) NOT NULL CHECK(quantity>0), unit varchar(24),
 originalHash varchar(64) NOT NULL, sourcePayloadHash varchar(64) NOT NULL,
 policyVersion varchar(160) NOT NULL,
 UNIQUE(organizationId,ID), UNIQUE(organizationId,occurrenceId),
 FOREIGN KEY(organizationId,recordedBy) REFERENCES mulino_identity_Actors(organizationId,ID),
 FOREIGN KEY(organizationId,occurrenceId) REFERENCES mulino_evidence_CanonicalOccurrences(organizationId,ID),
 FOREIGN KEY(organizationId,claimId) REFERENCES mulino_evidence_Claims(organizationId,ID),
 FOREIGN KEY(organizationId,eventId) REFERENCES mulino_evidence_Events(organizationId,ID),
 FOREIGN KEY(organizationId,verificationId) REFERENCES mulino_evidence_Verifications(organizationId,ID),
 FOREIGN KEY(organizationId,documentVersionId) REFERENCES mulino_evidence_DocumentVersions(organizationId,ID),
 FOREIGN KEY(organizationId,rootId) REFERENCES mulino_responsibility_Roots(organizationId,ID)
);
CREATE TRIGGER evidence_completion_immutable BEFORE UPDATE OR DELETE ON
 mulino_evidence_CompletionCoverages FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
ALTER TABLE mulino_responsibility_CompletionBindings ADD FOREIGN KEY(organizationId,coverageId)
 REFERENCES mulino_evidence_CompletionCoverages(organizationId,ID);
CREATE FUNCTION mulino_evidence_completion_binding_validate() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM mulino_evidence_CompletionCoverages c WHERE c.organizationId=NEW.organizationId
  AND c.ID=NEW.coverageId AND c.occurrenceId=NEW.occurrenceId AND c.rootId=NEW.rootId
  AND c.verificationId=NEW.verificationId AND c.startQuantity=NEW.startQuantity
  AND c.quantity=NEW.quantity AND c.unit IS NOT DISTINCT FROM NEW.unit) THEN
  RAISE EXCEPTION 'Completion binding must match authoritative immutable evidence range';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER evidence_completion_binding BEFORE INSERT ON mulino_responsibility_CompletionBindings
 FOR EACH ROW EXECUTE FUNCTION mulino_evidence_completion_binding_validate();
