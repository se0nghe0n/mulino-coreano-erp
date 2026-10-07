namespace mulino.trade.receipt;
entity Observations {
 key organizationId : UUID; key ID : UUID; revision : Integer;
 createdAt : Timestamp; recordedAt : Timestamp; occurredAt : Timestamp;
 eventId : UUID; rangeRootId : UUID; startQuantity : Decimal(38,12);
 quantity : Decimal(38,12); unit : String(40); itemId : UUID; lotId : UUID;
 placeId : UUID; purchaseLineId : UUID; transitSegmentId : UUID;
 identificationStatus : String(40); state : String(40); workId : UUID;
 ownerId : UUID; supervisorId : UUID; nextAction : String(320); nextCheckAt : Timestamp;
}
entity Receipts {
 key organizationId : UUID; key ID : UUID; revision : Integer;
 createdAt : Timestamp; recordedAt : Timestamp; occurredAt : Timestamp;
 observationId : UUID; canonicalOccurrenceId : UUID; rangeRootId : UUID;
 startQuantity : Decimal(38,12); quantity : Decimal(38,12); unit : String(40);
 itemId : UUID; lotId : UUID; placeId : UUID; segmentId : UUID;
 purchaseLineId : UUID; workId : UUID; contributedQuantity : Decimal(38,12);
 excessQuantity : Decimal(38,12); physicalEffect : String(40);
}
