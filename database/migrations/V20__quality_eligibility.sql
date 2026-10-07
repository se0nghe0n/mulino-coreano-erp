-- Exact physical permission scopes and immutable action-specific decisions.
ALTER TABLE mulino_inventory_Restrictions
 ADD segmentId VARCHAR(36), ADD startQuantity NUMERIC(38,12), ADD quantity NUMERIC(38,12), ADD unit VARCHAR(40),
 ADD category VARCHAR(40), ADD customerId VARCHAR(36), ADD workId VARCHAR(36), ADD nextCheckAt TIMESTAMPTZ,
 ADD commandId VARCHAR(36), ADD releasedAt TIMESTAMPTZ, ADD releaseDecisionId VARCHAR(36), ADD policyHash VARCHAR(64),
 ADD FOREIGN KEY(organizationId,segmentId) REFERENCES mulino_inventory_QuantitySegments(organizationId,ID),
 ADD CHECK(segmentId IS NULL OR (startQuantity>=0 AND quantity>0 AND unit IS NOT NULL AND commandId IS NOT NULL));
ALTER TABLE mulino_inventory_DispositionBases
 ADD segmentId VARCHAR(36), ADD startQuantity NUMERIC(38,12), ADD quantity NUMERIC(38,12), ADD unit VARCHAR(40),
 ADD category VARCHAR(40), ADD customerId VARCHAR(36), ADD workId VARCHAR(36), ADD nextCheckAt TIMESTAMPTZ,
 ADD commandId VARCHAR(36), ADD releasedAt TIMESTAMPTZ, ADD releaseDecisionId VARCHAR(36), ADD policyHash VARCHAR(64),
 ADD FOREIGN KEY(organizationId,segmentId) REFERENCES mulino_inventory_QuantitySegments(organizationId,ID),
 ADD CHECK(segmentId IS NULL OR (startQuantity>=0 AND quantity>0 AND unit IS NOT NULL AND commandId IS NOT NULL));
ALTER TABLE mulino_inventory_SegmentAllocations
 ADD startQuantity NUMERIC(38,12), ADD action VARCHAR(80), ADD customerId VARCHAR(36),
 ADD workId VARCHAR(36), ADD authorizationActorId VARCHAR(36), ADD nextValidityBoundary TIMESTAMPTZ,
 ADD suspendedAt TIMESTAMPTZ, ADD suspensionReason VARCHAR(240), ADD CHECK(startQuantity IS NULL OR startQuantity>=0);
CREATE TABLE mulino_quality_Decisions (
 organizationId VARCHAR(36) NOT NULL, ID VARCHAR(36) NOT NULL, revision INTEGER NOT NULL DEFAULT 0,
 createdAt TIMESTAMPTZ NOT NULL, recordedAt TIMESTAMPTZ NOT NULL, actorId VARCHAR(36) NOT NULL,
 operation VARCHAR(80) NOT NULL, segmentId VARCHAR(36) NOT NULL, restrictionId VARCHAR(36), dispositionBasisId VARCHAR(36),
 evidenceId VARCHAR(36) NOT NULL, policyHash VARCHAR(64) NOT NULL, commandId VARCHAR(36) NOT NULL, reason VARCHAR(240) NOT NULL,
 PRIMARY KEY(organizationId,ID),
 FOREIGN KEY(organizationId,segmentId) REFERENCES mulino_inventory_QuantitySegments(organizationId,ID),
 FOREIGN KEY(organizationId,evidenceId) REFERENCES mulino_evidence_DocumentVersions(organizationId,ID)
);
CREATE TRIGGER quality_decision_immutable BEFORE UPDATE OR DELETE ON mulino_quality_Decisions FOR EACH ROW EXECUTE FUNCTION inventory_version_immutable();
