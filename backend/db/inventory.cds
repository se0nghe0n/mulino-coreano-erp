namespace mulino.inventory;
// Flyway owns DDL. No writable service projections are exposed.
entity Products {
  key organizationId : UUID;
  key ID : UUID;
  revision : Integer;
  createdAt : Timestamp;
  recordedAt : Timestamp;
  name : String(240);
}
entity TradeItems {
  key organizationId : UUID;
  key ID : UUID;
  revision : Integer;
  createdAt : Timestamp;
  recordedAt : Timestamp;
  productId : UUID;
  name : String(240);
  baseUnit : String(40);
  decimalPlaces : Integer;
  specificationVersionId : UUID;
  packagingVersionId : UUID;
}
entity SpecificationVersions {
  key organizationId : UUID;
  key ID : UUID;
  revision : Integer;
  createdAt : Timestamp;
  recordedAt : Timestamp;
  productId : UUID;
  version : String(80);
  contentHash : String(64);
  description : LargeString;
}
entity PackagingVersions {
  key organizationId : UUID;
  key ID : UUID;
  revision : Integer;
  createdAt : Timestamp;
  recordedAt : Timestamp;
  productId : UUID;
  version : String(80);
  contentHash : String(64);
  description : LargeString;
}
entity UnitConversions {
  key organizationId : UUID;
  key ID : UUID;
  revision : Integer;
  createdAt : Timestamp;
  recordedAt : Timestamp;
  itemId : UUID;
  fromUnit : String(40);
  toUnit : String(40);
  factor : Decimal(38,12);
  validFrom : Timestamp;
  validUntil : Timestamp;
  evidenceRef : String(240);
}
entity ExternalIdentifiers {
  key organizationId : UUID;
  key ID : UUID;
  revision : Integer;
  createdAt : Timestamp;
  recordedAt : Timestamp;
  itemId : UUID;
  issuer : String(240);
  namespace : String(160);
  value : String(240);
  validFrom : Timestamp;
  validUntil : Timestamp;
}
entity Manufacturers {
  key organizationId : UUID;
  key ID : UUID;
  revision : Integer;
  createdAt : Timestamp;
  recordedAt : Timestamp;
  name : String(240);
}
entity ManufacturingLots {
  key organizationId : UUID;
  key ID : UUID;
  revision : Integer;
  createdAt : Timestamp;
  recordedAt : Timestamp;
  manufacturerId : UUID;
  itemId : UUID;
  originalLot : String(240);
  productionAt : Timestamp;
  expiresAt : Timestamp;
  evidenceRef : String(240);
}
entity Places {
  key organizationId : UUID;
  key ID : UUID;
  revision : Integer;
  createdAt : Timestamp;
  recordedAt : Timestamp;
  name : String(240);
  parentId : UUID;
  kind : String(40);
}
entity QuantitySegments {
  key organizationId : UUID;
  key ID : UUID;
  revision : Integer;
  createdAt : Timestamp;
  recordedAt : Timestamp;
  itemId : UUID;
  lotId : UUID;
  identificationStatus : String(40);
  quantity : Decimal(38,12);
  unit : String(40);
  placeId : UUID;
  custodianId : UUID;
  ownerId : UUID;
  controlScope : String(160);
  validFrom : Timestamp;
  retiredAt : Timestamp;
  retirementRecordedAt : Timestamp;
  mixtureStatus : String(40);
  evidenceRef : String(240);
}
entity LogisticsUnits {
  key organizationId : UUID;
  key ID : UUID;
  revision : Integer;
  createdAt : Timestamp;
  recordedAt : Timestamp;
  name : String(240);
}
entity LogisticsMemberships {
  key organizationId : UUID;
  key ID : UUID;
  revision : Integer;
  createdAt : Timestamp;
  recordedAt : Timestamp;
  logisticsUnitId : UUID;
  segmentId : UUID;
  validFrom : Timestamp;
  validUntil : Timestamp;
}
entity GenealogyEdges {
  sourceStartQuantity : Decimal(38,12);
  targetStartQuantity : Decimal(38,12);
  key organizationId : UUID;
  key ID : UUID;
  revision : Integer;
  createdAt : Timestamp;
  recordedAt : Timestamp;
  sourceId : UUID;
  targetId : UUID;
  quantity : Decimal(38,12);
  unit : String(40);
  kind : String(40);
  uncertain : Boolean;
  occurredAt : Timestamp;
  evidenceRef : String(240);
}
entity QuantityMovements {
  key organizationId : UUID;
  key ID : UUID;
  revision : Integer;
  createdAt : Timestamp;
  recordedAt : Timestamp;
  sourceId : UUID;
  targetId : UUID;
  quantity : Decimal(38,12);
  unit : String(40);
  kind : String(40);
  occurredAt : Timestamp;
  commandId : UUID;
  evidenceRef : String(240);
}
entity ObjectRelations {
  key organizationId : UUID;
  key ID : UUID;
  revision : Integer;
  createdAt : Timestamp;
  recordedAt : Timestamp;
  definitionVersionId : UUID;
  relationDefinitionId : UUID;
  sourceType : String(80);
  sourceId : UUID;
  targetType : String(80);
  targetId : UUID;
  validFrom : Timestamp;
  validUntil : Timestamp;
}
entity Stocktakes {
 key organizationId : UUID; key ID : UUID; revision : Integer;
 createdAt : Timestamp; recordedAt : Timestamp; segmentId : UUID;
 observedQuantity : Decimal(38,12); unit : String(40); occurredAt : Timestamp;
 evidenceRef : String(240); commandId : UUID;
}
entity IdentifierConflicts {
 key organizationId : UUID; key ID : UUID; revision : Integer;
 createdAt : Timestamp; recordedAt : Timestamp; itemId : UUID;
 existingIdentifierId : UUID; issuer : String(240); namespace : String(160);
 value : String(240); validFrom : Timestamp; validUntil : Timestamp; state : String(40);
}
entity Restrictions {
 key organizationId : UUID; key ID : UUID; revision : Integer;
 createdAt : Timestamp; recordedAt : Timestamp; controlScope : String(160);
 action : String(80); state : String(40); validFrom : Timestamp; validUntil : Timestamp;
 decisionId : UUID; evidenceRef : String(240);
 segmentId : UUID; startQuantity : Decimal(38,12); quantity : Decimal(38,12); unit : String(40);
 category : String(40); customerId : UUID; workId : UUID; nextCheckAt : Timestamp;
 commandId : UUID; releasedAt : Timestamp; releaseDecisionId : UUID; policyHash : String(64);

}
entity DispositionBases {
 key organizationId : UUID; key ID : UUID; revision : Integer;
 createdAt : Timestamp; recordedAt : Timestamp; controlScope : String(160);
 action : String(80); state : String(40); validFrom : Timestamp; validUntil : Timestamp;
 decisionId : UUID; evidenceRef : String(240);
 segmentId : UUID; startQuantity : Decimal(38,12); quantity : Decimal(38,12); unit : String(40);
 category : String(40); customerId : UUID; workId : UUID; nextCheckAt : Timestamp;
 commandId : UUID; releasedAt : Timestamp; releaseDecisionId : UUID; policyHash : String(64);

}
entity SegmentAllocations {
 key organizationId : UUID; key ID : UUID; revision : Integer;
 createdAt : Timestamp; recordedAt : Timestamp; rootId : UUID; segmentId : UUID;
 orderLineId : UUID; quantity : Decimal(38,12); unit : String(40); state : String(40);
 predecessorId : UUID; commandId : UUID;
 startQuantity : Decimal(38,12); action : String(80); customerId : UUID;
 workId : UUID; authorizationActorId : UUID; nextValidityBoundary : Timestamp;
 suspendedAt : Timestamp; suspensionReason : String(240);

}
entity StockAdjustments {
 key organizationId : UUID; key ID : UUID; revision : Integer;
 createdAt : Timestamp; recordedAt : Timestamp; stocktakeId : UUID; segmentId : UUID;
 direction : String(40); quantity : Decimal(38,12); unit : String(40);
 occurredAt : Timestamp; reason : String(240); evidenceRef : String(240); commandId : UUID;
}
