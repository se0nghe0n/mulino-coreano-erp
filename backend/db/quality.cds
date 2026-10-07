namespace mulino.quality;
entity Decisions {
 key organizationId : UUID; key ID : UUID; revision : Integer;
 createdAt : Timestamp; recordedAt : Timestamp; actorId : UUID;
 operation : String(80); segmentId : UUID; restrictionId : UUID; dispositionBasisId : UUID;
 evidenceId : UUID; policyHash : String(64); commandId : UUID; reason : String(240);
}
