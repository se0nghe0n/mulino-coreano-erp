namespace mulino.work.read;
aspect scopedRead {
  key organizationId : UUID;
  key ID : UUID;
  revision : Integer not null default 0;
  createdAt : Timestamp not null;
  recordedAt : Timestamp not null;
  effectiveAt : Timestamp not null;
}
entity Works : scopedRead {
  itemId : UUID not null;
  lotId : UUID;
  definitionVersionId : UUID not null;
  kind : String(80) not null;
  status : String(40) not null;
  ownerId : UUID not null;
  supervisorId : UUID not null;
  waitJson : LargeString;
}
entity GoalReferences : scopedRead {
  workId : UUID not null;
  definitionVersionId : UUID not null;
  quantityMode : String(40) not null;
  targetQuantity : Decimal(38,12);
  unit : String(20);
  endpoint : String(80);
  scopeJson : LargeString not null;
}
entity AssessmentReferences : scopedRead {
  workId : UUID not null;
  goalId : UUID not null;
  outcome : String(40) not null;
  evaluatorVersion : String(80) not null;
  assessedAt : Timestamp not null;
  conditionsJson : LargeString not null;
}
entity ObligationReferences : scopedRead {
  workId : UUID not null;
  kind : String(80) not null;
  status : String(40) not null;
  ownerId : UUID not null;
  supervisorId : UUID not null;
  nextAction : String(500) not null;
  nextCheckAt : Timestamp not null;
  quantity : Decimal(38,12);
  unit : String(20);
  scopeJson : LargeString not null;
}
entity SubjectLinks : scopedRead {
  workId : UUID not null;
  itemId : UUID not null;
  lotId : UUID;
}
entity EvidenceReferences : scopedRead {
  workId : UUID not null;
  documentVersionId : UUID not null;
  role : String(80) not null;
}
