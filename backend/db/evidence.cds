namespace mulino.evidence;

// Immutable facts. Canonical verification never executes an inventory movement.

entity SourceProfiles {
  key ID : UUID;
  organizationId : UUID;
  revision : Integer;
  createdAt : Timestamp;
  recordedAt : Timestamp;
  recordedBy : UUID;
  namespace : String(160);
  policyVersion : String(160);
  intakeOwnerId : UUID;
  supervisorId : UUID;
  nextAction : String(320);
  nextCheckAt : Timestamp;
}

entity DocumentVersions {
  key ID : UUID;
  organizationId : UUID;
  revision : Integer;
  createdAt : Timestamp;
  recordedAt : Timestamp;
  recordedBy : UUID;
  subjectKind : String(20);
  subjectId : UUID;
  itemId : UUID;
  placeId : UUID;
  workId : UUID;
  sha256 : String(64);
  blobId : UUID;
  byteLength : Integer64;
  mediaType : String(160);
  sourceNamespace : String(160);
  sourceProfileId : UUID;
  sourceReference : String(320);
  availability : String(24);
  supersedesId : UUID;
  provenance : LargeString;
}

entity Events {
  key ID : UUID;
  organizationId : UUID;
  revision : Integer;
  createdAt : Timestamp;
  recordedAt : Timestamp;
  recordedBy : UUID;
  subjectKind : String(20);
  subjectId : UUID;
  itemId : UUID;
  placeId : UUID;
  workId : UUID;
  effectiveFrom : Timestamp;
  effectiveUntil : Timestamp;
  timeZone : String(80);
  timePrecision : String(20);
  valueState : String(24);
  kind : String(80);
  sourceNamespace : String(160);
  sourceProfileId : UUID;
  externalEventId : String(160);
  sourceVersion : String(80);
  payloadHash : String(64);
  payload : LargeString;
  supersedesId : UUID;
  invalidatesId : UUID;
}

entity Claims {
  sourceProfileId : UUID;
  key ID : UUID;
  organizationId : UUID;
  revision : Integer;
  createdAt : Timestamp;
  recordedAt : Timestamp;
  recordedBy : UUID;
  subjectKind : String(20);
  subjectId : UUID;
  itemId : UUID;
  placeId : UUID;
  workId : UUID;
  effectiveFrom : Timestamp;
  effectiveUntil : Timestamp;
  timeZone : String(80);
  timePrecision : String(20);
  valueState : String(24);
  eventId : UUID;
  documentVersionId : UUID;
  assertion : LargeString;
  quantity : Decimal(38,12);
  unit : String(24);
  supersedesId : UUID;
}

entity InboxRecords {
  key ID : UUID;
  organizationId : UUID;
  revision : Integer;
  createdAt : Timestamp;
  recordedAt : Timestamp;
  recordedBy : UUID;
  subjectKind : String(20);
  subjectId : UUID;
  itemId : UUID;
  placeId : UUID;
  workId : UUID;
  eventId : UUID;
  sourceNamespace : String(160);
  sourceProfileId : UUID;
  externalEventId : String(160);
  sourceVersion : String(80);
  payloadHash : String(64);
  state : String(24);
  conflictsWithId : UUID;
  intakeOwnerId : UUID;
  supervisorId : UUID;
  nextAction : String(320);
  nextCheckAt : Timestamp;
}

entity CanonicalOccurrences {
  sourceProfileId : UUID;
  key ID : UUID;
  organizationId : UUID;
  revision : Integer;
  createdAt : Timestamp;
  recordedAt : Timestamp;
  recordedBy : UUID;
  subjectKind : String(20);
  subjectId : UUID;
  itemId : UUID;
  placeId : UUID;
  workId : UUID;
  effectiveFrom : Timestamp;
  effectiveUntil : Timestamp;
  timeZone : String(80);
  timePrecision : String(20);
  valueState : String(24);
  kind : String(80);
  physicalScopeId : UUID;
  occurrenceIdentity : UUID;
  quantity : Decimal(38,12);
  unit : String(24);
  supersedesId : UUID;
  reassessmentState : String(24);
}

entity Verifications {
  key ID : UUID;
  organizationId : UUID;
  revision : Integer;
  createdAt : Timestamp;
  recordedAt : Timestamp;
  recordedBy : UUID;
  claimId : UUID;
  canonicalOccurrenceId : UUID;
  basisDocumentId : UUID;
  policyVersion : String(160);
  sourceMatched : Boolean;
  identityMatched : Boolean;
  quantityMatched : Boolean;
  timeMatched : Boolean;
  duplicateChecked : Boolean;
  verdict : String(24);
  reason : String(640);
}

entity EvidenceLinks {
  key ID : UUID;
  organizationId : UUID;
  revision : Integer;
  createdAt : Timestamp;
  recordedAt : Timestamp;
  recordedBy : UUID;
  documentVersionId : UUID;
  eventId : UUID;
  claimId : UUID;
  canonicalOccurrenceId : UUID;
  role : String(40);
}

// Authorized human review is a distinct immutable decision, not an external claim.
entity Reconciliations {
  key ID : UUID;
  organizationId : UUID;
  revision : Integer;
  createdAt : Timestamp;
  recordedAt : Timestamp;
  recordedBy : UUID;
  subjectKind : String(20);
  subjectId : UUID;
  itemId : UUID;
  placeId : UUID;
  workId : UUID;
  sourceProfileId : UUID;
  claimId : UUID;
  basisDocumentId : UUID;
  physicalScopeId : UUID;
  existingCanonicalId : UUID;
  policyVersion : String(160);
  decision : String(24);
  reason : String(640);
  sourceIdentity : String(320);
  quantity : Decimal(38,12);
  unit : String(24);
  effectiveFrom : Timestamp;
  intakeOwnerId : UUID;
  supervisorId : UUID;
  nextAction : String(320);
  nextCheckAt : Timestamp;
}

// Typed completion scope is derived from immutable source bytes only after scoped review.
entity CompletionCoverages {
 key ID : UUID;
 organizationId : UUID;
 revision : Integer;
 createdAt : Timestamp;
 recordedAt : Timestamp;
 recordedBy : UUID;
 occurrenceId : UUID;
 claimId : UUID;
 eventId : UUID;
 verificationId : UUID;
 documentVersionId : UUID;
 rootId : UUID;
 startQuantity : Decimal(38,12);
 quantity : Decimal(38,12);
 unit : String(24);
 originalHash : String(64);
 sourcePayloadHash : String(64);
 policyVersion : String(160);
}
