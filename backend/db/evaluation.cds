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
