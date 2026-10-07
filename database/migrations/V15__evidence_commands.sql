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
