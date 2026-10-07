namespace mulino.trade.purchase;
entity Suppliers { key organizationId:UUID; key ID:UUID; name:String(240) not null;  revision:Integer not null default 1; createdAt:Timestamp not null; recordedAt:Timestamp not null; effectiveAt:Timestamp not null;
}
entity Proposals {
 currentApprovalId:UUID;
 key organizationId:UUID; key ID:UUID; workId:UUID not null;
 currentRevision:Integer not null; status:String(40) not null; createdAt:Timestamp not null;
 revision:Integer not null default 1; recordedAt:Timestamp not null; effectiveAt:Timestamp not null;
}
entity ProposalRevisions {
 key organizationId:UUID; key ID:UUID; proposalId:UUID not null; revision:Integer not null;
 workId:UUID not null; goalVersionId:UUID; itemId:UUID not null; supplierId:UUID not null;
 destinationId:UUID not null; quantity:Decimal(38,12) not null; unit:String(20) not null;
 price:Decimal(38,12) not null; currency:String(3) not null; dueAt:Timestamp not null;
 endpoint:String(40) not null; quantityMode:String(40) not null;
 proposalHash:String(64) not null; reason:String(500); recordedAt:Timestamp not null;
 createdAt:Timestamp not null; effectiveAt:Timestamp not null;
}
entity Approvals {
 key organizationId:UUID; key ID:UUID; proposalId:UUID not null; proposalRevision:Integer not null;
 proposalHash:String(64) not null; approverId:UUID not null; policyHash:String(64) not null;
 decision:String(24) not null; conditionsJson:LargeString not null;
 decidedAt:Timestamp not null; validUntil:Timestamp not null;
 revision:Integer not null default 1; createdAt:Timestamp not null; recordedAt:Timestamp not null; effectiveAt:Timestamp not null;
}
entity Orders {
 conditionAssessmentJson:LargeString not null;
 key organizationId:UUID; key ID:UUID; proposalId:UUID not null; proposalRevision:Integer not null;
 proposalHash:String(64) not null; approvalId:UUID not null; channel:String(80) not null;
 externalOperationId:String(160) not null; outboxId:UUID not null; createdAt:Timestamp not null;
 revision:Integer not null default 1; recordedAt:Timestamp not null; effectiveAt:Timestamp not null;
}
entity OrderLines {
 key organizationId:UUID; key ID:UUID; orderId:UUID not null; proposalId:UUID not null;
 proposalRevision:Integer not null; workId:UUID not null; itemId:UUID not null;
 destinationId:UUID not null; quantity:Decimal(38,12) not null; unit:String(20) not null;
 status:String(40) not null; revision:Integer not null;
 createdAt:Timestamp not null; recordedAt:Timestamp not null; effectiveAt:Timestamp not null;
}
entity SupplierReplies {
 canonicalOccurrenceId:UUID not null;
 key organizationId:UUID; key ID:UUID; orderId:UUID not null; reply:String(24) not null;
 proposedQuantity:Decimal(38,12) not null; unit:String(20) not null;
 evidenceId:UUID not null; recordedAt:Timestamp not null;
 revision:Integer not null default 1; createdAt:Timestamp not null; effectiveAt:Timestamp not null;
}
entity ReceiptCredits {
 key organizationId:UUID; key ID:UUID; lineId:UUID not null; occurrenceId:UUID not null;
 actualQuantity:Decimal(38,12) not null; contributedQuantity:Decimal(38,12) not null;
 excessQuantity:Decimal(38,12) not null; unit:String(20) not null; recordedAt:Timestamp not null;
 revision:Integer not null default 1; createdAt:Timestamp not null; effectiveAt:Timestamp not null;
}
entity ExecutionEffects {
 key organizationId:UUID; key ID:UUID; lineId:UUID not null; kind:String(24) not null;
 referenceId:UUID not null; quantity:Decimal(38,12) not null; unit:String(20) not null;
 recordedAt:Timestamp not null;
 revision:Integer not null default 1; createdAt:Timestamp not null; effectiveAt:Timestamp not null;
}
entity Cancellations {
 key organizationId:UUID; key ID:UUID; orderId:UUID not null; lineId:UUID not null;
 quantity:Decimal(38,12) not null; unit:String(20) not null; reason:String(500) not null;
 externalAcceptance:String(24) not null; outboxId:UUID not null; recordedAt:Timestamp not null;
 revision:Integer not null default 1; createdAt:Timestamp not null; effectiveAt:Timestamp not null;
}
