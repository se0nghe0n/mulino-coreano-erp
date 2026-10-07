CREATE TABLE mulino_runtime_ExecutionScopes (
 organizationId varchar(36) NOT NULL, workId varchar(36) NOT NULL,
 capabilityId varchar(120) NOT NULL, commandId varchar(160) NOT NULL,
 fencingToken bigint NOT NULL DEFAULT 0, attemptId varchar(36),
 PRIMARY KEY(organizationId,workId,capabilityId,commandId)
);
CREATE TABLE mulino_runtime_ExecutionAttempts (
 organizationId varchar(36) NOT NULL, ID varchar(36) NOT NULL,
 workId varchar(36) NOT NULL, capabilityId varchar(120) NOT NULL,
 commandId varchar(160) NOT NULL, actorId varchar(36) NOT NULL,
 stableRequestOwner varchar(160) NOT NULL, leaseOwner varchar(160) NOT NULL,
 fencingToken bigint NOT NULL, startedAt timestamptz NOT NULL,
 heartbeatAt timestamptz NOT NULL, leaseExpiresAt timestamptz NOT NULL,
 finishedAt timestamptz, status varchar(40) NOT NULL CHECK(status IN
 ('ACTIVE','SUCCEEDED','FAILED','EXPIRED','HELD','CANCELLED')),
 technicalCode varchar(80), PRIMARY KEY(organizationId,ID),
 FOREIGN KEY(organizationId,workId,capabilityId,commandId) REFERENCES
 mulino_runtime_ExecutionScopes(organizationId,workId,capabilityId,commandId)
);
CREATE INDEX runtime_attempt_expiry ON mulino_runtime_ExecutionAttempts(leaseExpiresAt)
 WHERE status='ACTIVE';
CREATE TABLE mulino_runtime_Outbox (
 organizationId varchar(36) NOT NULL, ID varchar(36) NOT NULL,
 commandId varchar(160) NOT NULL, externalOperationId varchar(160) NOT NULL,
 operation varchar(120) NOT NULL, payloadJson text NOT NULL,
 actorId varchar(36) NOT NULL, stableRequestOwner varchar(160) NOT NULL,
 status varchar(40) NOT NULL CHECK(status IN
 ('PENDING','IN_FLIGHT','SUCCEEDED','UNKNOWN_EXTERNAL','EXHAUSTED','CANCELLED')),
 createdAt timestamptz NOT NULL, nextCheckAt timestamptz NOT NULL,
 deliveryAttempts integer NOT NULL DEFAULT 0, fencingToken bigint NOT NULL DEFAULT 0,
 leaseOwner varchar(160), leaseExpiresAt timestamptz,
 idempotencySupported boolean NOT NULL DEFAULT false,
 lookupSupported boolean NOT NULL DEFAULT false,
 resultEvidenceId varchar(160), nextAction varchar(500) NOT NULL,
 PRIMARY KEY(organizationId,ID), UNIQUE(organizationId,externalOperationId)
);
CREATE INDEX runtime_outbox_due ON mulino_runtime_Outbox(nextCheckAt)
 WHERE status IN ('PENDING','UNKNOWN_EXTERNAL','EXHAUSTED');
CREATE INDEX runtime_outbox_expiry ON mulino_runtime_Outbox(leaseExpiresAt)
 WHERE status='IN_FLIGHT';
CREATE TABLE mulino_runtime_ExternalReconciliations (
 organizationId varchar(36) NOT NULL, ID varchar(36) NOT NULL,
 outboxId varchar(36) NOT NULL, decision varchar(40) NOT NULL CHECK(decision IN
 ('CONFIRMED_SUCCESS','CONFIRMED_FAILURE','UNRESOLVED')),
 actorId varchar(36) NOT NULL, evidenceId varchar(160) NOT NULL,
 recordedAt timestamptz NOT NULL, PRIMARY KEY(organizationId,ID),
 FOREIGN KEY(organizationId,outboxId) REFERENCES mulino_runtime_Outbox(organizationId,ID)
);
CREATE TABLE mulino_runtime_RecoverySchedules (
 organizationId varchar(36) NOT NULL, ID varchar(36) NOT NULL,
 workId varchar(36) NOT NULL, kind varchar(80) NOT NULL,
 nextCheckAt timestamptz NOT NULL, nextValidityBoundary timestamptz,
 pendingAssessment boolean NOT NULL DEFAULT false,
 recoveryStatus varchar(40) NOT NULL DEFAULT 'READY',
 lastOutcome varchar(80), lastCheckedAt timestamptz, attemptCount integer NOT NULL DEFAULT 0,
 ownerId varchar(36) NOT NULL, supervisorId varchar(36) NOT NULL,
 nextAction varchar(500) NOT NULL, revision integer NOT NULL DEFAULT 0,
 PRIMARY KEY(organizationId,ID), UNIQUE(organizationId,workId,kind)
);
CREATE INDEX runtime_schedule_due ON mulino_runtime_RecoverySchedules(nextCheckAt);
CREATE INDEX runtime_schedule_boundary ON mulino_runtime_RecoverySchedules(nextValidityBoundary);
CREATE INDEX runtime_schedule_pending ON mulino_runtime_RecoverySchedules(organizationId,workId)
 WHERE pendingAssessment=true;
