namespace mulino.work;
entity WorkLinks {
 key organizationId : UUID;
 key ID : UUID;
 sourceWorkId : UUID;
 activityId : UUID;
 targetWorkId : UUID not null;
 kind : String(40) not null;
 createdAt : Timestamp not null;
}
entity WorkTransitions {
 key organizationId : UUID;
 key ID : UUID;
 workId : UUID not null;
 revision : Integer not null;
 operation : String(80) not null;
 previousState : String(40);
 state : String(40) not null;
 actorId : UUID not null;
 reason : String(500);
 recordedAt : Timestamp not null;
 snapshotJson : LargeString not null;
}
entity WorkContributions {
 key organizationId : UUID;
 key ID : UUID;
 linkId : UUID not null;
 occurrenceId : UUID not null;
 targetWorkId : UUID not null;
 goalId : UUID not null;
 conditionId : String(100) not null;
 startQuantity : Decimal(38,12) not null;
 quantity : Decimal(38,12) not null;
 unit : String(20) not null;
 recordedAt : Timestamp not null;
}
