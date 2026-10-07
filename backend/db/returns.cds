namespace mulino.trade.returns;
entity Authorizations {
 key organizationId : UUID; key ID : UUID; revision : Integer;
 createdAt : Timestamp; recordedAt : Timestamp; workId : UUID;
 deliveryId : UUID; customerId : UUID; itemId : UUID; lotId : UUID;
 rangeRootId : UUID; startQuantity : Decimal(38,12); quantity : Decimal(38,12);
 unit : String(40); destinationId : UUID; actorId : UUID; validUntil : Timestamp;
 policyHash : String(64); commandId : UUID; reason : String(320);
}
entity Observations {
 key organizationId : UUID; key ID : UUID; revision : Integer;
 createdAt : Timestamp; recordedAt : Timestamp; authorizationId : UUID;
 eventId : UUID; workId : UUID; deliveryId : UUID; customerId : UUID;
 itemId : UUID; lotId : UUID; rangeRootId : UUID; startQuantity : Decimal(38,12);
 quantity : Decimal(38,12); unit : String(40); placeId : UUID;
 occurredAt : Timestamp; nextCheckAt : Timestamp; state : String(40);
}
entity Receipts {
 key organizationId : UUID; key ID : UUID; revision : Integer;
 createdAt : Timestamp; recordedAt : Timestamp; observationId : UUID;
 authorizationId : UUID; eventId : UUID; canonicalOccurrenceId : UUID;
 workId : UUID; deliveryId : UUID; customerId : UUID; itemId : UUID; lotId : UUID;
 rangeRootId : UUID; startQuantity : Decimal(38,12); quantity : Decimal(38,12);
 unit : String(40); placeId : UUID; segmentId : UUID; restrictionId : UUID;
 occurredAt : Timestamp; commandId : UUID;
}
entity Dispositions {
 key organizationId : UUID; key ID : UUID; revision : Integer;
 createdAt : Timestamp; recordedAt : Timestamp; returnId : UUID;
 workId : UUID; decision : String(40); actorId : UUID; evidenceId : UUID;
 policyHash : String(64); commandId : UUID; reason : String(320);
}
