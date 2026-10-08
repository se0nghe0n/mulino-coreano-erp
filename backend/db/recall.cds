namespace mulino.trade.recall;
entity Investigations {
 key organizationId : UUID; key ID : UUID; revision : Integer;
 createdAt : Timestamp; recordedAt : Timestamp; occurredAt : Timestamp;
 itemId : UUID; lotId : UUID; segmentId : UUID; workId : UUID;
 quantity : Decimal(38,12); unit : String(40); startQuantity : Decimal(38,12);
 basisEvidenceId : UUID; impactState : String(40); status : String(40);
 currentScopeId : UUID; ownerId : UUID; supervisorId : UUID;
 nextAction : String(320); nextCheckAt : Timestamp; closedAt : Timestamp;
}
entity Scopes {
 key organizationId : UUID; key ID : UUID; revision : Integer;
 createdAt : Timestamp; recordedAt : Timestamp; investigationId : UUID;
 version : Integer; scopeHash : String(64); rootSegmentId : UUID;
 startQuantity : Decimal(38,12); quantity : Decimal(38,12); unit : String(40);
 itemId : UUID; lotId : UUID; workId : UUID; reason : String(320);
}
entity Approvals {
 key organizationId : UUID; key ID : UUID; revision : Integer;
 createdAt : Timestamp; recordedAt : Timestamp; scopeId : UUID;
 scopeVersion : Integer; scopeHash : String(64); approverId : UUID;
 decision : String(40); validUntil : Timestamp; policyHash : String(64);
}
entity Actions {
 key organizationId : UUID; key ID : UUID; revision : Integer;
 createdAt : Timestamp; recordedAt : Timestamp; occurredAt : Timestamp;
 scopeId : UUID; investigationId : UUID; approvalId : UUID; canonicalOccurrenceId : UUID;
 actualEventId : UUID; kind : String(40); rootSegmentId : UUID;
 startQuantity : Decimal(38,12); quantity : Decimal(38,12); unit : String(40);
 sourceSegmentId : UUID; currentSegmentId : UUID; currentPlaceId : UUID;
 returnId : UUID; reason : String(320); residualDutyId : UUID;
}
entity Closures {
 key organizationId : UUID; key ID : UUID; revision : Integer;
 createdAt : Timestamp; recordedAt : Timestamp; investigationId : UUID;
 scopeId : UUID; approvalId : UUID; scopeHash : String(64);
 partitionJson : LargeString; evidenceId : UUID; closedBy : UUID;
}
