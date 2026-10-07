CREATE TABLE mulino_commands_CommandRecords (
 ID VARCHAR(36) PRIMARY KEY, organizationId VARCHAR(36) NOT NULL,
 actorId VARCHAR(36) NOT NULL, canonicalIntentJson TEXT,
 stableRequestOwner VARCHAR(160) NOT NULL, capabilityId VARCHAR(120) NOT NULL,
 commandIdempotencyKey VARCHAR(160) NOT NULL, canonicalHash VARCHAR(64) NOT NULL,
 definitionVersion VARCHAR(80) NOT NULL, capabilityVersion VARCHAR(80) NOT NULL,
 state VARCHAR(30) NOT NULL CHECK(state IN ('IN_PROGRESS','COMMITTED','REJECTED','UNKNOWN_EXTERNAL')),
 resultJson TEXT, createdAt TIMESTAMPTZ NOT NULL, completedAt TIMESTAMPTZ,
 UNIQUE(organizationId,stableRequestOwner,capabilityId,commandIdempotencyKey),
 FOREIGN KEY(organizationId) REFERENCES mulino_identity_Organizations(ID),
 FOREIGN KEY(organizationId,actorId) REFERENCES mulino_identity_Actors(organizationId,ID)
);
CREATE TABLE mulino_commands_CommandAudits (
 ID VARCHAR(36) PRIMARY KEY, organizationId VARCHAR(36) NOT NULL, actorId VARCHAR(36) NOT NULL,
 commandId VARCHAR(36) NOT NULL, capabilityId VARCHAR(120) NOT NULL,
 canonicalHash VARCHAR(64) NOT NULL, outcome VARCHAR(40) NOT NULL,
 effectRefs TEXT NOT NULL, createdAt TIMESTAMPTZ NOT NULL,
 FOREIGN KEY(organizationId,actorId) REFERENCES mulino_identity_Actors(organizationId,ID),
 FOREIGN KEY(commandId) REFERENCES mulino_commands_CommandRecords(ID)
);
CREATE FUNCTION mulino_commands_immutable_audit() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Command audit is immutable'; END $$;
CREATE TRIGGER command_audits_immutable BEFORE UPDATE OR DELETE ON mulino_commands_CommandAudits
 FOR EACH ROW EXECUTE FUNCTION mulino_commands_immutable_audit();
CREATE TABLE mulino_commands_Approvals (
 ID VARCHAR(36) PRIMARY KEY,organizationId VARCHAR(36) NOT NULL,proposalId VARCHAR(36) NOT NULL,proposalRevision INTEGER NOT NULL CHECK(proposalRevision>0),
 canonicalHash VARCHAR(64) NOT NULL,scopeHash VARCHAR(64) NOT NULL,targetId VARCHAR(36),targetRevision INTEGER,
 action VARCHAR(120) NOT NULL,policyHash VARCHAR(64) NOT NULL,
 decision VARCHAR(30) NOT NULL CHECK(decision IN ('APPROVED','REJECTED','CONDITIONAL')),
 decisionCapability VARCHAR(120) NOT NULL,approverId VARCHAR(36) NOT NULL,
 decidedAt TIMESTAMPTZ NOT NULL,expiresAt TIMESTAMPTZ NOT NULL,singleUse BOOLEAN NOT NULL,
 CHECK(expiresAt>decidedAt),UNIQUE(organizationId,ID),
 FOREIGN KEY(organizationId,approverId) REFERENCES mulino_identity_Actors(organizationId,ID)
);
CREATE TABLE mulino_commands_ApprovalConsumptions (
 ID VARCHAR(36) PRIMARY KEY,organizationId VARCHAR(36) NOT NULL,approvalId VARCHAR(36) NOT NULL,commandId VARCHAR(36) NOT NULL,
 consumedAt TIMESTAMPTZ NOT NULL,UNIQUE(organizationId,approvalId),
 FOREIGN KEY(organizationId,approvalId) REFERENCES mulino_commands_Approvals(organizationId,ID),
 FOREIGN KEY(commandId) REFERENCES mulino_commands_CommandRecords(ID)
);
CREATE TRIGGER approvals_immutable BEFORE UPDATE OR DELETE ON mulino_commands_Approvals
 FOR EACH ROW EXECUTE FUNCTION mulino_commands_immutable_audit();
CREATE TRIGGER approval_consumptions_immutable BEFORE UPDATE OR DELETE ON mulino_commands_ApprovalConsumptions
 FOR EACH ROW EXECUTE FUNCTION mulino_commands_immutable_audit();
