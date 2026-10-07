-- Shipment plans and verified observations are separate from the stock ledger.
CREATE TABLE mulino_trade_shipment_Shipments (
 organizationId varchar(36) NOT NULL,
 ID varchar(36) NOT NULL,
 revision integer NOT NULL,
 createdAt timestamptz NOT NULL,
 recordedAt timestamptz NOT NULL,
 recordedBy varchar(36) NOT NULL,
 originId varchar(36) NOT NULL,
 destinationId varchar(36) NOT NULL,
 carrierRef varchar(240) NOT NULL,
 workId varchar(36),
 state varchar(40) NOT NULL,
 PRIMARY KEY(organizationId,ID),
 FOREIGN KEY(organizationId,recordedBy) REFERENCES mulino_identity_Actors(organizationId,ID),
 FOREIGN KEY(organizationId,originId) REFERENCES mulino_inventory_Places(organizationId,ID),
 FOREIGN KEY(organizationId,destinationId) REFERENCES mulino_inventory_Places(organizationId,ID),
 FOREIGN KEY(organizationId,workId) REFERENCES mulino_work_read_Works(organizationId,ID)
);
CREATE TABLE mulino_trade_shipment_ShipmentCargo (
 organizationId varchar(36) NOT NULL,
 ID varchar(36) NOT NULL,
 revision integer NOT NULL,
 createdAt timestamptz NOT NULL,
 recordedAt timestamptz NOT NULL,
 recordedBy varchar(36) NOT NULL,
 shipmentId varchar(36) NOT NULL,
 itemId varchar(36) NOT NULL,
 plannedQuantity numeric(38,12) NOT NULL,
 unit varchar(40) NOT NULL,
 PRIMARY KEY(organizationId,ID),
 FOREIGN KEY(organizationId,recordedBy) REFERENCES mulino_identity_Actors(organizationId,ID),
 FOREIGN KEY(organizationId,shipmentId) REFERENCES mulino_trade_shipment_Shipments(organizationId,ID),
 FOREIGN KEY(organizationId,itemId) REFERENCES mulino_inventory_TradeItems(organizationId,ID),
 CHECK(plannedQuantity>0)
);
CREATE TRIGGER shipment_shipmentcargo_immutable BEFORE UPDATE OR DELETE ON mulino_trade_shipment_ShipmentCargo FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
CREATE TABLE mulino_trade_shipment_CargoAllocations (
 organizationId varchar(36) NOT NULL,
 ID varchar(36) NOT NULL,
 revision integer NOT NULL,
 createdAt timestamptz NOT NULL,
 recordedAt timestamptz NOT NULL,
 recordedBy varchar(36) NOT NULL,
 shipmentId varchar(36) NOT NULL,
 cargoId varchar(36) NOT NULL,
 poLineId varchar(36) NOT NULL,
 quantity numeric(38,12) NOT NULL,
 unit varchar(40) NOT NULL,
 PRIMARY KEY(organizationId,ID),
 FOREIGN KEY(organizationId,recordedBy) REFERENCES mulino_identity_Actors(organizationId,ID),
 FOREIGN KEY(organizationId,shipmentId) REFERENCES mulino_trade_shipment_Shipments(organizationId,ID),
 FOREIGN KEY(organizationId,cargoId) REFERENCES mulino_trade_shipment_ShipmentCargo(organizationId,ID),
 CHECK(quantity>0)
);
CREATE TRIGGER shipment_cargoallocations_immutable BEFORE UPDATE OR DELETE ON mulino_trade_shipment_CargoAllocations FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
CREATE TABLE mulino_trade_shipment_ShipmentLegs (
 organizationId varchar(36) NOT NULL,
 ID varchar(36) NOT NULL,
 revision integer NOT NULL,
 createdAt timestamptz NOT NULL,
 recordedAt timestamptz NOT NULL,
 recordedBy varchar(36) NOT NULL,
 shipmentId varchar(36) NOT NULL,
 sequence integer NOT NULL,
 originId varchar(36) NOT NULL,
 destinationId varchar(36) NOT NULL,
 carrierRef varchar(240) NOT NULL,
 plannedDepartureAt timestamptz,
 plannedArrivalAt timestamptz,
 PRIMARY KEY(organizationId,ID),
 FOREIGN KEY(organizationId,recordedBy) REFERENCES mulino_identity_Actors(organizationId,ID),
 FOREIGN KEY(organizationId,shipmentId) REFERENCES mulino_trade_shipment_Shipments(organizationId,ID),
 FOREIGN KEY(organizationId,originId) REFERENCES mulino_inventory_Places(organizationId,ID),
 FOREIGN KEY(organizationId,destinationId) REFERENCES mulino_inventory_Places(organizationId,ID),
 UNIQUE(organizationId,shipmentId,sequence),
 CHECK(sequence>=0),
 CHECK(plannedArrivalAt IS NULL OR plannedDepartureAt IS NULL OR plannedArrivalAt>=plannedDepartureAt)
);
CREATE TRIGGER shipment_shipmentlegs_immutable BEFORE UPDATE OR DELETE ON mulino_trade_shipment_ShipmentLegs FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
CREATE TABLE mulino_trade_shipment_LegEvents (
 organizationId varchar(36) NOT NULL,
 ID varchar(36) NOT NULL,
 revision integer NOT NULL,
 createdAt timestamptz NOT NULL,
 recordedAt timestamptz NOT NULL,
 recordedBy varchar(36) NOT NULL,
 shipmentId varchar(36) NOT NULL,
 cargoId varchar(36) NOT NULL,
 legId varchar(36) NOT NULL,
 kind varchar(40) NOT NULL,
 placeId varchar(36) NOT NULL,
 physicalScopeId varchar(36) NOT NULL,
 occurrenceId varchar(36) NOT NULL,
 quantity numeric(38,12) NOT NULL,
 unit varchar(40) NOT NULL,
 occurredAt timestamptz NOT NULL,
 detailStatus varchar(40) NOT NULL,
 PRIMARY KEY(organizationId,ID),
 FOREIGN KEY(organizationId,recordedBy) REFERENCES mulino_identity_Actors(organizationId,ID),
 FOREIGN KEY(organizationId,shipmentId) REFERENCES mulino_trade_shipment_Shipments(organizationId,ID),
 FOREIGN KEY(organizationId,cargoId) REFERENCES mulino_trade_shipment_ShipmentCargo(organizationId,ID),
 FOREIGN KEY(organizationId,legId) REFERENCES mulino_trade_shipment_ShipmentLegs(organizationId,ID),
 FOREIGN KEY(organizationId,occurrenceId) REFERENCES mulino_evidence_CanonicalOccurrences(organizationId,ID),
 UNIQUE(organizationId,occurrenceId),
 CHECK(quantity>0)
);
CREATE TRIGGER shipment_legevents_immutable BEFORE UPDATE OR DELETE ON mulino_trade_shipment_LegEvents FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
CREATE TABLE mulino_trade_shipment_CustodyHandovers (
 organizationId varchar(36) NOT NULL,
 ID varchar(36) NOT NULL,
 revision integer NOT NULL,
 createdAt timestamptz NOT NULL,
 recordedAt timestamptz NOT NULL,
 recordedBy varchar(36) NOT NULL,
 shipmentId varchar(36) NOT NULL,
 cargoId varchar(36) NOT NULL,
 legId varchar(36) NOT NULL,
 placeId varchar(36) NOT NULL,
 fromCustodianId varchar(36) NOT NULL,
 toCustodianId varchar(36) NOT NULL,
 physicalScopeId varchar(36) NOT NULL,
 occurrenceId varchar(36) NOT NULL,
 quantity numeric(38,12) NOT NULL,
 unit varchar(40) NOT NULL,
 occurredAt timestamptz NOT NULL,
 detailStatus varchar(40) NOT NULL,
 PRIMARY KEY(organizationId,ID),
 FOREIGN KEY(organizationId,recordedBy) REFERENCES mulino_identity_Actors(organizationId,ID),
 FOREIGN KEY(organizationId,shipmentId) REFERENCES mulino_trade_shipment_Shipments(organizationId,ID),
 FOREIGN KEY(organizationId,cargoId) REFERENCES mulino_trade_shipment_ShipmentCargo(organizationId,ID),
 FOREIGN KEY(organizationId,legId) REFERENCES mulino_trade_shipment_ShipmentLegs(organizationId,ID),
 FOREIGN KEY(organizationId,occurrenceId) REFERENCES mulino_evidence_CanonicalOccurrences(organizationId,ID),
 UNIQUE(organizationId,occurrenceId),
 CHECK(quantity>0)
);
CREATE TRIGGER shipment_custodyhandovers_immutable BEFORE UPDATE OR DELETE ON mulino_trade_shipment_CustodyHandovers FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
ALTER TABLE mulino_trade_shipment_CargoAllocations ADD FOREIGN KEY(organizationId,poLineId) REFERENCES mulino_trade_purchase_OrderLines(organizationId,ID);
ALTER TABLE mulino_trade_shipment_LegEvents ADD FOREIGN KEY(organizationId,placeId) REFERENCES mulino_inventory_Places(organizationId,ID);
ALTER TABLE mulino_trade_shipment_CustodyHandovers ADD FOREIGN KEY(organizationId,placeId) REFERENCES mulino_inventory_Places(organizationId,ID);
ALTER TABLE mulino_trade_shipment_CustodyHandovers ADD FOREIGN KEY(organizationId,fromCustodianId) REFERENCES mulino_identity_Actors(organizationId,ID);
ALTER TABLE mulino_trade_shipment_CustodyHandovers ADD FOREIGN KEY(organizationId,toCustodianId) REFERENCES mulino_identity_Actors(organizationId,ID);
CREATE TABLE mulino_trade_shipment_DepartureAllocations (
 organizationId varchar(36) NOT NULL,ID varchar(36) NOT NULL,
 revision integer NOT NULL,createdAt timestamptz NOT NULL,
 recordedAt timestamptz NOT NULL,recordedBy varchar(36) NOT NULL,
 shipmentId varchar(36) NOT NULL,cargoId varchar(36) NOT NULL,
 observationId varchar(36) NOT NULL,physicalScopeId varchar(36) NOT NULL,
 poLineId varchar(36) NOT NULL,quantity numeric(38,12) NOT NULL CHECK(quantity>0),
 unit varchar(40) NOT NULL,PRIMARY KEY(organizationId,ID),
 UNIQUE(organizationId,observationId,poLineId),
 UNIQUE(organizationId,cargoId,physicalScopeId,poLineId),
 FOREIGN KEY(organizationId,recordedBy) REFERENCES mulino_identity_Actors(organizationId,ID),
 FOREIGN KEY(organizationId,shipmentId) REFERENCES mulino_trade_shipment_Shipments(organizationId,ID),
 FOREIGN KEY(organizationId,cargoId) REFERENCES mulino_trade_shipment_ShipmentCargo(organizationId,ID),
 FOREIGN KEY(organizationId,observationId) REFERENCES mulino_trade_shipment_LegEvents(organizationId,ID),
 FOREIGN KEY(organizationId,poLineId) REFERENCES mulino_trade_purchase_OrderLines(organizationId,ID)
);
CREATE TRIGGER shipment_departureallocations_immutable BEFORE UPDATE OR DELETE ON
 mulino_trade_shipment_DepartureAllocations FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
