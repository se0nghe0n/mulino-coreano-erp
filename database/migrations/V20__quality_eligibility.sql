-- Exact physical permission scopes and immutable action-specific decisions.
ALTER TABLE mulino_inventory_Restrictions
 ADD segmentId VARCHAR(36), ADD startQuantity NUMERIC(38,12), ADD quantity NUMERIC(38,12), ADD unit VARCHAR(40),
 ADD category VARCHAR(40), ADD customerId VARCHAR(36), ADD workId VARCHAR(36), ADD nextCheckAt TIMESTAMPTZ,
 ADD commandId VARCHAR(36), ADD releasedAt TIMESTAMPTZ, ADD releaseDecisionId VARCHAR(36), ADD policyHash VARCHAR(64),
 ADD FOREIGN KEY(organizationId,segmentId) REFERENCES mulino_inventory_QuantitySegments(organizationId,ID),
 ADD CHECK(segmentId IS NULL OR (startQuantity IS NOT NULL AND quantity IS NOT NULL AND startQuantity>=0 AND quantity>0 AND unit IS NOT NULL AND commandId IS NOT NULL AND category IS NOT NULL AND workId IS NOT NULL AND policyHash IS NOT NULL AND validUntil IS NOT NULL AND validUntil>=validFrom));
ALTER TABLE mulino_inventory_DispositionBases
 ADD segmentId VARCHAR(36), ADD startQuantity NUMERIC(38,12), ADD quantity NUMERIC(38,12), ADD unit VARCHAR(40),
 ADD category VARCHAR(40), ADD customerId VARCHAR(36), ADD workId VARCHAR(36), ADD nextCheckAt TIMESTAMPTZ,
 ADD commandId VARCHAR(36), ADD releasedAt TIMESTAMPTZ, ADD releaseDecisionId VARCHAR(36), ADD policyHash VARCHAR(64),
 ADD FOREIGN KEY(organizationId,segmentId) REFERENCES mulino_inventory_QuantitySegments(organizationId,ID),
 ADD CHECK(segmentId IS NULL OR (startQuantity IS NOT NULL AND quantity IS NOT NULL AND startQuantity>=0 AND quantity>0 AND unit IS NOT NULL AND commandId IS NOT NULL AND category IS NOT NULL AND workId IS NOT NULL AND policyHash IS NOT NULL AND validUntil IS NOT NULL AND validUntil>=validFrom));
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
-- New scoped decisions cannot be changed into a different permission by a release.
CREATE FUNCTION quality_permission_release_only() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN
  IF OLD.segmentId IS NOT NULL THEN RAISE EXCEPTION 'scoped permission history immutable' USING ERRCODE='23514'; END IF;
  RETURN OLD;
 END IF;
 IF OLD.segmentId IS NOT NULL AND
   ((to_jsonb(NEW)-ARRAY['state','releasedat','releasedecisionid','revision']) IS DISTINCT FROM
    (to_jsonb(OLD)-ARRAY['state','releasedat','releasedecisionid','revision']) OR
    OLD.state<>'ACTIVE' OR NEW.state NOT IN ('RELEASED','REVOKED') OR
    NEW.revision<>OLD.revision+1 OR NEW.releasedAt IS NULL OR NEW.releaseDecisionId IS NULL) THEN
  RAISE EXCEPTION 'only referenced scoped release is permitted' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER quality_hold_release_only BEFORE UPDATE OR DELETE ON mulino_inventory_Restrictions FOR EACH ROW EXECUTE FUNCTION quality_permission_release_only();
CREATE TRIGGER quality_basis_release_only BEFORE UPDATE OR DELETE ON mulino_inventory_DispositionBases FOR EACH ROW EXECUTE FUNCTION quality_permission_release_only();
