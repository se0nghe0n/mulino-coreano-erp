namespace mulino.trade.shipment;
// Plans do not generate physical inventory. Events retain exact canonical facts.
entity Shipments {
 key organizationId : UUID;
 key ID : UUID;
 revision : Integer;
 createdAt : Timestamp;
 recordedAt : Timestamp;
 recordedBy : UUID;
 originId : UUID;
 destinationId : UUID;
 carrierRef : String(240);
 workId : UUID;
 state : String(40);
}
entity ShipmentCargo {
 key organizationId : UUID;
 key ID : UUID;
 revision : Integer;
 createdAt : Timestamp;
 recordedAt : Timestamp;
 recordedBy : UUID;
 shipmentId : UUID;
 itemId : UUID;
 plannedQuantity : Decimal(38,12);
 unit : String(40);
}
entity CargoAllocations {
 key organizationId : UUID;
 key ID : UUID;
 revision : Integer;
 createdAt : Timestamp;
 recordedAt : Timestamp;
 recordedBy : UUID;
 shipmentId : UUID;
 cargoId : UUID;
 poLineId : UUID;
 quantity : Decimal(38,12);
 unit : String(40);
}
entity ShipmentLegs {
 key organizationId : UUID;
 key ID : UUID;
 revision : Integer;
 createdAt : Timestamp;
 recordedAt : Timestamp;
 recordedBy : UUID;
 shipmentId : UUID;
 sequence : Integer;
 originId : UUID;
 destinationId : UUID;
 carrierRef : String(240);
 plannedDepartureAt : Timestamp;
 plannedArrivalAt : Timestamp;
}
entity LegEvents {
 key organizationId : UUID;
 key ID : UUID;
 revision : Integer;
 createdAt : Timestamp;
 recordedAt : Timestamp;
 recordedBy : UUID;
 shipmentId : UUID;
 cargoId : UUID;
 legId : UUID;
 kind : String(40);
 placeId : UUID;
 physicalScopeId : UUID;
 occurrenceId : UUID;
 quantity : Decimal(38,12);
 unit : String(40);
 occurredAt : Timestamp;
 detailStatus : String(40);
}
entity CustodyHandovers {
 key organizationId : UUID;
 key ID : UUID;
 revision : Integer;
 createdAt : Timestamp;
 recordedAt : Timestamp;
 recordedBy : UUID;
 shipmentId : UUID;
 cargoId : UUID;
 legId : UUID;
 placeId : UUID;
 fromCustodianId : UUID;
 toCustodianId : UUID;
 physicalScopeId : UUID;
 occurrenceId : UUID;
 quantity : Decimal(38,12);
 unit : String(40);
 occurredAt : Timestamp;
 detailStatus : String(40);
}
entity DepartureAllocations {
 key organizationId : UUID;
 key ID : UUID;
 revision : Integer;
 createdAt : Timestamp;
 recordedAt : Timestamp;
 recordedBy : UUID;
 shipmentId : UUID;
 cargoId : UUID;
 observationId : UUID;
 physicalScopeId : UUID;
 poLineId : UUID;
 quantity : Decimal(38,12);
 unit : String(40);
}
