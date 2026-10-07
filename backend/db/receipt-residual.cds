namespace mulino.responsibility;
entity ReceiptResidualRoots {
 key organizationId:UUID; key ID:UUID; revision:Integer not null default 0;
 createdAt:Timestamp not null; recordedAt:Timestamp not null;
 rootId:UUID not null; lineId:UUID not null; initialReceiptId:UUID not null;
 initialContribution:Decimal(38,12) not null; orderedQuantity:Decimal(38,12) not null; unit:String(20) not null;
}
entity ReceiptResidualCredits {
 key organizationId:UUID; key ID:UUID; revision:Integer not null default 0;
 createdAt:Timestamp not null; recordedAt:Timestamp not null;
 rootId:UUID not null; scopeId:UUID not null; assignmentId:UUID not null;
 receiptId:UUID not null; startQuantity:Decimal(38,12) not null; quantity:Decimal(38,12) not null;
}
entity ObservationReceiptCredits {
 key organizationId:UUID; key ID:UUID; revision:Integer not null default 0;
 createdAt:Timestamp not null; recordedAt:Timestamp not null;
 rootId:UUID not null; assignmentId:UUID not null; observationId:UUID not null; receiptId:UUID not null;
}
