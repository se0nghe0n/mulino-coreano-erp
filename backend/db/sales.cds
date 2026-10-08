namespace mulino.trade.sales;
entity Customers {
 key organizationId:UUID; key ID:UUID;
 revision:Integer;
 createdAt:Timestamp;
 recordedAt:Timestamp;
 effectiveAt:Timestamp;
 name:String(240);
}
entity Orders {
 key organizationId:UUID; key ID:UUID;
 revision:Integer;
 createdAt:Timestamp;
 recordedAt:Timestamp;
 effectiveAt:Timestamp;
 workId:UUID;
 currentRevision:Integer;
}
entity OrderRevisions {
 key organizationId:UUID; key ID:UUID;
 revision:Integer;
 createdAt:Timestamp;
 recordedAt:Timestamp;
 effectiveAt:Timestamp;
 orderId:UUID;
 workId:UUID;
 customerId:UUID;
 revisionNumber:Integer;
 reason:String(500);
}
entity OrderLines {
 key organizationId:UUID; key ID:UUID;
 revision:Integer;
 createdAt:Timestamp;
 recordedAt:Timestamp;
 effectiveAt:Timestamp;
 orderId:UUID;
 revisionId:UUID;
 workId:UUID;
 customerId:UUID;
 itemId:UUID;
 quantity:Decimal(38,12);
 unit:String(40);
 price:Decimal(38,12);
 currency:String(3);
 dueAt:Timestamp;
 destinationId:UUID;
 deliveryEndpoint:String(40);
 qualityTerms:String(500);
 packageTerms:String(500);
 previousLineId:UUID;
}
entity Observations {
 key organizationId:UUID; key ID:UUID;
 revision:Integer;
 createdAt:Timestamp;
 recordedAt:Timestamp;
 effectiveAt:Timestamp;
 eventId:UUID;
 dispatchId:UUID;
 cargoScopeId:UUID;
 salesLineId:UUID;
 workId:UUID;
 customerId:UUID;
 itemId:UUID;
 lotId:UUID;
 rangeRootId:UUID;
 startQuantity:Decimal(38,12);
 quantity:Decimal(38,12);
 unit:String(40);
 placeId:UUID;
 occurredAt:Timestamp;
 state:String(40);
 nextCheckAt:Timestamp;
 nextAction:String(320);
}
entity Deliveries {
 key organizationId:UUID; key ID:UUID;
 revision:Integer;
 createdAt:Timestamp;
 recordedAt:Timestamp;
 effectiveAt:Timestamp;
 observationId:UUID;
 canonicalOccurrenceId:UUID;
 dispatchId:UUID;
 cargoScopeId:UUID;
 salesLineId:UUID;
 workId:UUID;
 customerId:UUID;
 itemId:UUID;
 lotId:UUID;
 rangeRootId:UUID;
 startQuantity:Decimal(38,12);
 quantity:Decimal(38,12);
 unit:String(40);
 placeId:UUID;
 segmentId:UUID;
 occurredAt:Timestamp;
 legitimateQuantity:Decimal(38,12);
 legitimateRangesJson:LargeString;
}
entity ExecutionEffects {
 key organizationId:UUID; key ID:UUID;
 revision:Integer;
 createdAt:Timestamp;
 recordedAt:Timestamp;
 effectiveAt:Timestamp;
 lineId:UUID;
 kind:String(40);
 referenceId:UUID;
 quantity:Decimal(38,12);
 unit:String(40);
}
entity DeliveryCorrections {
 key organizationId:UUID; key ID:UUID;
 revision:Integer;
 createdAt:Timestamp;
 recordedAt:Timestamp;
 effectiveAt:Timestamp;
 deliveryId:UUID;
 canonicalOccurrenceId:UUID;
 quantity:Decimal(38,12);
 legitimateQuantity:Decimal(38,12);
 unit:String(40);
}
