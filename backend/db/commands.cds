namespace mulino.commands;
entity CommandRecords {
 key ID : UUID;
 organizationId : UUID; stableRequestOwner : String(160);
 capabilityId : String(120); commandIdempotencyKey : String(160);
 canonicalHash : String(64); definitionVersion : String(80); capabilityVersion : String(80);
 state : String(30); resultJson : LargeString; createdAt : Timestamp; completedAt : Timestamp;
}
entity CommandAudits {
 key ID : UUID; organizationId : UUID; actorId : UUID;
 commandId : UUID; capabilityId : String(120); canonicalHash : String(64);
 outcome : String(40); effectRefs : LargeString; createdAt : Timestamp;
}
entity Approvals {
 key ID : UUID; organizationId : UUID; proposalId : UUID; proposalRevision : Integer;
 canonicalHash : String(64); scopeHash : String(64); targetId : UUID; targetRevision : Integer;
 action : String(120); policyHash : String(64); decision : String(30);
 decisionCapability : String(120); approverId : UUID; decidedAt : Timestamp; expiresAt : Timestamp;
 singleUse : Boolean;
}
entity ApprovalConsumptions {
 key ID : UUID; organizationId : UUID; approvalId : UUID; commandId : UUID; consumedAt : Timestamp;
}
