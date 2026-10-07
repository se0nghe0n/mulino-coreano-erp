-- R3 fresh fixture path: historical policy Timestamp columns are interpreted as UTC.
ALTER TABLE mulino_governance_PolicyVersions
 ALTER COLUMN createdAt TYPE TIMESTAMPTZ USING createdAt AT TIME ZONE 'UTC',
 ALTER COLUMN effectiveFrom TYPE TIMESTAMPTZ USING effectiveFrom AT TIME ZONE 'UTC',
 ALTER COLUMN effectiveUntil TYPE TIMESTAMPTZ USING effectiveUntil AT TIME ZONE 'UTC';
CREATE TABLE mulino_identity_ManagementAuthorities (
 organizationId VARCHAR(36) NOT NULL, ID VARCHAR(36) NOT NULL, actorId VARCHAR(36) NOT NULL,
 capabilityId VARCHAR(100) NOT NULL, scopeKind VARCHAR(20) NOT NULL, scopeId VARCHAR(36) NOT NULL,
 validFrom TIMESTAMPTZ NOT NULL, validUntil TIMESTAMPTZ NOT NULL, revokedAt TIMESTAMPTZ,
 revision INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(organizationId,ID),
 FOREIGN KEY(organizationId,actorId) REFERENCES mulino_identity_Actors(organizationId,ID), CHECK(validUntil>validFrom));
CREATE TABLE mulino_identity_AuthorityInvalidations (
 organizationId VARCHAR(36) NOT NULL, ID VARCHAR(36) NOT NULL, actorId VARCHAR(36) NOT NULL,
 nextCheckAt TIMESTAMPTZ NOT NULL, reason VARCHAR(100) NOT NULL, revision INTEGER NOT NULL,
 PRIMARY KEY(organizationId,ID), FOREIGN KEY(organizationId,actorId) REFERENCES mulino_identity_Actors(organizationId,ID));
CREATE TABLE mulino_governance_PolicyDrafts (
 organizationId VARCHAR(36) NOT NULL REFERENCES mulino_identity_Organizations(ID), ID VARCHAR(36) NOT NULL,
 kind VARCHAR(80) NOT NULL, version VARCHAR(80) NOT NULL, content TEXT NOT NULL, contentHash VARCHAR(64) NOT NULL,
 source VARCHAR(500) NOT NULL, regressionEvidence VARCHAR(500) NOT NULL, effectiveFrom TIMESTAMPTZ NOT NULL,
 effectiveUntil TIMESTAMPTZ, legallyRestrictive BOOLEAN NOT NULL, fixtureOnly BOOLEAN NOT NULL,
 status VARCHAR(30) NOT NULL CHECK(status IN('DRAFT','APPROVED','ACTIVE','RETIRED')), revision INTEGER NOT NULL,
 PRIMARY KEY(organizationId,ID), CHECK(effectiveUntil IS NULL OR effectiveUntil>effectiveFrom));
CREATE TABLE mulino_governance_PolicyApprovals (
 organizationId VARCHAR(36) NOT NULL, ID VARCHAR(36) NOT NULL, draftId VARCHAR(36) NOT NULL,
 contentHash VARCHAR(64) NOT NULL, approverId VARCHAR(36) NOT NULL, approvedAt TIMESTAMPTZ NOT NULL,
 PRIMARY KEY(organizationId,ID), UNIQUE(organizationId,draftId),
 FOREIGN KEY(organizationId,draftId) REFERENCES mulino_governance_PolicyDrafts(organizationId,ID),
 FOREIGN KEY(organizationId,approverId) REFERENCES mulino_identity_Actors(organizationId,ID));
CREATE TABLE mulino_governance_ActivePolicies (
 organizationId VARCHAR(36) NOT NULL, kind VARCHAR(80) NOT NULL, policyId VARCHAR(36) NOT NULL, revision INTEGER NOT NULL,
 PRIMARY KEY(organizationId,kind), FOREIGN KEY(organizationId,policyId) REFERENCES mulino_governance_PolicyVersions(organizationId,ID));
CREATE TABLE mulino_governance_PolicyFences (
 organizationId VARCHAR(36) PRIMARY KEY REFERENCES mulino_identity_Organizations(ID), revision INTEGER NOT NULL DEFAULT 1);
CREATE TABLE mulino_governance_PolicyBoundaries (
 organizationId VARCHAR(36) NOT NULL, ID VARCHAR(36) NOT NULL, policyId VARCHAR(36) NOT NULL,
 nextCheckAt TIMESTAMPTZ NOT NULL, reason VARCHAR(100) NOT NULL, PRIMARY KEY(organizationId,ID),
 FOREIGN KEY(organizationId,policyId) REFERENCES mulino_governance_PolicyVersions(organizationId,ID));
CREATE FUNCTION mulino_control_immutable() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 RAISE EXCEPTION 'Published control content is immutable'; END $$;
CREATE TRIGGER immutable_policy_approvals BEFORE UPDATE OR DELETE ON mulino_governance_PolicyApprovals
 FOR EACH ROW EXECUTE FUNCTION mulino_control_immutable();
CREATE FUNCTION mulino_draft_content_immutable() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF OLD.status <> 'DRAFT' AND ROW(NEW.content,NEW.contentHash,NEW.source,NEW.regressionEvidence,NEW.effectiveFrom,NEW.effectiveUntil,NEW.legallyRestrictive,NEW.fixtureOnly)
 IS DISTINCT FROM ROW(OLD.content,OLD.contentHash,OLD.source,OLD.regressionEvidence,OLD.effectiveFrom,OLD.effectiveUntil,OLD.legallyRestrictive,OLD.fixtureOnly)
 THEN RAISE EXCEPTION 'Approved policy content is immutable'; END IF; RETURN NEW; END $$;
CREATE TRIGGER immutable_approved_draft BEFORE UPDATE ON mulino_governance_PolicyDrafts
 FOR EACH ROW EXECUTE FUNCTION mulino_draft_content_immutable();
