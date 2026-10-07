namespace mulino.runtime;
entity ExecutionScopes {
 key organizationId: UUID; key workId: UUID;
 key capabilityId: String(120); key commandId: String(160);
 fencingToken: Integer64 not null; attemptId: UUID;
}
entity ExecutionAttempts {
 key organizationId: UUID; key ID: UUID; workId: UUID not null;
 capabilityId: String(120) not null; commandId: String(160) not null;
 actorId: UUID not null; stableRequestOwner: String(160) not null;
 leaseOwner: String(160) not null; fencingToken: Integer64 not null;
 startedAt: Timestamp not null; heartbeatAt: Timestamp not null;
 leaseExpiresAt: Timestamp not null; finishedAt: Timestamp;
 status: String(40) not null; technicalCode: String(80);
}
entity Outbox {
 key organizationId: UUID; key ID: UUID; commandId: String(160) not null;
 externalOperationId: String(160) not null; operation: String(120) not null;
 payloadJson: LargeString not null; actorId: UUID not null;
 stableRequestOwner: String(160) not null; status: String(40) not null;
 createdAt: Timestamp not null; nextCheckAt: Timestamp not null;
 deliveryAttempts: Integer not null; fencingToken: Integer64 not null;
 leaseOwner: String(160); leaseExpiresAt: Timestamp;
 idempotencySupported: Boolean not null; lookupSupported: Boolean not null;
 resultEvidenceId: String(160); nextAction: String(500) not null;
}
entity ExternalReconciliations {
 key organizationId: UUID; key ID: UUID; outboxId: UUID not null;
 decision: String(40) not null; actorId: UUID not null;
 evidenceId: String(160) not null; recordedAt: Timestamp not null;
}
entity RecoverySchedules {
 key organizationId: UUID; key ID: UUID; workId: UUID not null;
 kind: String(80) not null; nextCheckAt: Timestamp not null;
 nextValidityBoundary: Timestamp; pendingAssessment: Boolean not null;
 recoveryStatus: String(40) not null; lastOutcome: String(80);
 lastCheckedAt: Timestamp; attemptCount: Integer not null;
 ownerId: UUID not null; supervisorId: UUID not null;
 nextAction: String(500) not null; revision: Integer not null;
}
