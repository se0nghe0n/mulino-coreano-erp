namespace mulino.evaluation;
entity InputSnapshots {
  key organizationId : UUID;
  key ID : UUID;
  assessmentId : UUID not null;
  workId : UUID not null;
  goalId : UUID not null;
  recordedAt : Timestamp not null;
  asOf : Timestamp not null;
  knownAt : Timestamp not null;
  contentHash : String(64) not null;
  contentJson : LargeString not null;
}

// A real common source capture; goal-specific calculations remain InputSnapshots.
entity EvidenceCaptures {
  key organizationId : UUID;
  key ID : UUID;
  itemId : UUID not null;
  lotId : UUID;
  placeId : UUID;
  workId : UUID;
  scopeJson : LargeString not null;
  asOf : Timestamp not null;
  knownAt : Timestamp not null;
  evaluationHorizon : Timestamp not null;
  admissionPurpose : String(40) not null;
  contentHash : String(64) not null;
  contentJson : LargeString not null;
  recordedAt : Timestamp not null;
}
extend InputSnapshots with { evidenceCaptureId : UUID; }
