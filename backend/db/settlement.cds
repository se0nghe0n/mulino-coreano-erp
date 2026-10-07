namespace mulino.trade.settlement;
entity Invoices {
 key organizationId:UUID; key ID:UUID;
 revision:Integer not null default 1; createdAt:Timestamp not null; recordedAt:Timestamp not null; effectiveAt:Timestamp not null;
 workId:UUID not null; itemId:UUID not null; lineId:UUID not null; referenceId:UUID not null;
 scopeKind:String(16) not null; invoiceKind:String(24) not null;
 sourceNamespace:String(160) not null; sourceKey:String(160) not null; sourceVersion:String(80) not null; sourceHash:String(64) not null;
 evidenceId:UUID not null; quantity:Decimal(38,12) not null; unit:String(20) not null;
 unitPrice:Decimal(38,12) not null; originalAmount:Decimal(38,12) not null; currency:String(3) not null;
 fxPair:String(7); fxRate:Decimal(38,12); fxDate:Date; fxSource:String(160); fxPolicyVersion:String(80); fxRounding:String(40); convertedAmount:Decimal(38,12);
 relatedInvoiceId:UUID; taxReferenceOnly:Boolean not null default false;
}
entity Matches {
 key organizationId:UUID; key ID:UUID; revision:Integer not null default 1; createdAt:Timestamp not null; recordedAt:Timestamp not null; effectiveAt:Timestamp not null;
 invoiceId:UUID not null; lineId:UUID not null; referenceId:UUID not null; occurrenceId:UUID not null; invoiceOccurrenceId:UUID not null;
 scopeKind:String(16) not null; orderedQuantity:Decimal(38,12) not null; receivedQuantity:Decimal(38,12) not null; invoiceQuantity:Decimal(38,12) not null;
 quantity:Decimal(38,12) not null; unit:String(20) not null; unitPrice:Decimal(38,12) not null;
 orderedAmount:Decimal(38,12) not null; invoiceAmount:Decimal(38,12) not null; currency:String(3) not null; invoiceCurrency:String(3) not null;
 quantityDifference:Decimal(38,12) not null; priceDifference:Decimal(38,12) not null; originalDifference:Decimal(38,12); currencyDifference:Boolean not null;
 status:String(24) not null; dutyRootId:UUID;
}
entity Charges {
 key organizationId:UUID; key ID:UUID; revision:Integer not null default 1; createdAt:Timestamp not null; recordedAt:Timestamp not null; effectiveAt:Timestamp not null;
 invoiceId:UUID not null; kind:String(40) not null; amount:Decimal(38,12) not null; currency:String(3) not null; occurrenceId:UUID not null; evidenceId:UUID not null;
}
entity Adjustments {
 key organizationId:UUID; key ID:UUID; revision:Integer not null default 1; createdAt:Timestamp not null; recordedAt:Timestamp not null; effectiveAt:Timestamp not null;
 invoiceId:UUID not null; matchId:UUID not null; mode:String(16) not null; amount:Decimal(38,12) not null; currency:String(3) not null;
 reason:String(500) not null; evidenceId:UUID not null; occurrenceId:UUID not null; status:String(24) not null;
 proposalId:UUID; proposalHash:String(64) not null; decisionHash:String(64); approverId:UUID; policyHash:String(64); policyVersion:String(80);
}
entity PaymentReferences {
 key organizationId:UUID; key ID:UUID; revision:Integer not null default 1; createdAt:Timestamp not null; recordedAt:Timestamp not null; effectiveAt:Timestamp not null;
 invoiceId:UUID not null; amount:Decimal(38,12) not null; currency:String(3) not null; externalPaymentReference:String(160) not null;
 observedAt:Timestamp not null; evidenceId:UUID not null; occurrenceId:UUID not null; bankEffect:Decimal(38,12) not null default 0;
}
