ALTER TABLE mulino_inventory_GenealogyEdges ADD COLUMN sourceStartQuantity numeric(38,12), ADD COLUMN targetStartQuantity numeric(38,12);
ALTER TABLE mulino_inventory_GenealogyEdges ADD CONSTRAINT genealogy_exact_offsets CHECK ((sourceStartQuantity IS NULL AND targetStartQuantity IS NULL) OR (sourceStartQuantity IS NOT NULL AND targetStartQuantity IS NOT NULL AND sourceStartQuantity >= 0 AND targetStartQuantity >= 0));
CREATE TABLE mulino_inventory_Dispatches (
 organizationId varchar(36) NOT NULL, ID varchar(36) NOT NULL, revision integer NOT NULL DEFAULT 0,
 createdAt timestamptz NOT NULL, recordedAt timestamptz NOT NULL,
 cargoScopeId varchar(36) NOT NULL, allocationId varchar(36) NOT NULL, salesLineId varchar(36) NOT NULL, customerId varchar(36) NOT NULL, workId varchar(36) NOT NULL,
 itemId varchar(36) NOT NULL, lotId varchar(36) NOT NULL, rangeRootId varchar(36) NOT NULL, startQuantity numeric(38,12) NOT NULL CHECK(startQuantity>=0), quantity numeric(38,12) NOT NULL CHECK(quantity>0), unit varchar(40) NOT NULL,
 transitSegmentId varchar(36) NOT NULL, destinationId varchar(36) NOT NULL, occurredAt timestamptz NOT NULL, commandId varchar(36) NOT NULL, evidenceRef varchar(240) NOT NULL,
 PRIMARY KEY(organizationId,ID), UNIQUE(organizationId,allocationId), FOREIGN KEY(organizationId,allocationId) REFERENCES mulino_inventory_SegmentAllocations(organizationId,ID), FOREIGN KEY(organizationId,transitSegmentId) REFERENCES mulino_inventory_QuantitySegments(organizationId,ID)
);
CREATE TABLE mulino_inventory_CargoScopes (
 organizationId varchar(36) NOT NULL, ID varchar(36) NOT NULL, revision integer NOT NULL DEFAULT 0, createdAt timestamptz NOT NULL, recordedAt timestamptz NOT NULL,
 dispatchId varchar(36) NOT NULL, rangeRootId varchar(36) NOT NULL, startQuantity numeric(38,12) NOT NULL CHECK(startQuantity>=0), quantity numeric(38,12) NOT NULL CHECK(quantity>0), unit varchar(40) NOT NULL,
 segmentId varchar(36) NOT NULL, customerId varchar(36) NOT NULL, workId varchar(36) NOT NULL, commandId varchar(36) NOT NULL,
 PRIMARY KEY(organizationId,ID), UNIQUE(organizationId,dispatchId), FOREIGN KEY(organizationId,dispatchId) REFERENCES mulino_inventory_Dispatches(organizationId,ID)
);
CREATE TABLE mulino_inventory_DeliveryTransfers (
 legitimateQuantity numeric(38,12) NOT NULL CHECK(legitimateQuantity>=0 AND legitimateQuantity<=quantity),
 organizationId varchar(36) NOT NULL, ID varchar(36) NOT NULL, revision integer NOT NULL DEFAULT 0, createdAt timestamptz NOT NULL, recordedAt timestamptz NOT NULL,
 dispatchId varchar(36) NOT NULL, canonicalId varchar(36) NOT NULL, startQuantity numeric(38,12) NOT NULL CHECK(startQuantity>=0), quantity numeric(38,12) NOT NULL CHECK(quantity>0), unit varchar(40) NOT NULL,
 segmentId varchar(36) NOT NULL, occurredAt timestamptz NOT NULL, commandId varchar(36) NOT NULL,
 PRIMARY KEY(organizationId,ID), UNIQUE(organizationId,canonicalId), FOREIGN KEY(organizationId,dispatchId) REFERENCES mulino_inventory_Dispatches(organizationId,ID)
);
CREATE TRIGGER dispatch_immutable BEFORE UPDATE OR DELETE ON mulino_inventory_Dispatches FOR EACH ROW EXECUTE FUNCTION inventory_version_immutable();
CREATE TRIGGER cargo_immutable BEFORE UPDATE OR DELETE ON mulino_inventory_CargoScopes FOR EACH ROW EXECUTE FUNCTION inventory_version_immutable();
CREATE TRIGGER delivery_transfer_immutable BEFORE UPDATE OR DELETE ON mulino_inventory_DeliveryTransfers FOR EACH ROW EXECUTE FUNCTION inventory_version_immutable();
ALTER TABLE mulino_inventory_SegmentAllocations ADD COLUMN pickedAt timestamptz;

CREATE OR REPLACE FUNCTION inventory_conservation() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE org VARCHAR(36); sid VARCHAR(36); original NUMERIC; retired TIMESTAMPTZ; total NUMERIC;
BEGIN
 IF TG_TABLE_NAME='mulino_inventory_quantitysegments' THEN org:=NEW.organizationId; sid:=NEW.ID;
 ELSE org:=NEW.organizationId; sid:=NEW.sourceId; END IF;
 IF sid IS NULL THEN RETURN NULL; END IF;
 SELECT quantity,retiredAt INTO original,retired FROM mulino_inventory_QuantitySegments WHERE organizationId=org AND ID=sid;
 IF retired IS NOT NULL THEN
  SELECT COALESCE(SUM(quantity),0) INTO total FROM mulino_inventory_GenealogyEdges WHERE organizationId=org AND sourceId=sid;
  SELECT total+COALESCE(SUM(quantity),0) INTO total FROM mulino_inventory_QuantityMovements WHERE organizationId=org AND sourceId=sid AND targetId IS NULL AND kind IN ('DISPOSE','ADJUST_DECREASE');
  IF total<>original THEN RAISE EXCEPTION 'inventory parent conservation failed' USING ERRCODE='23514'; END IF;
  IF EXISTS(SELECT 1 FROM mulino_inventory_SegmentAllocations WHERE organizationId=org AND segmentId=sid AND (state='EXECUTABLE' OR (state='SUSPENDED' AND COALESCE(suspensionReason,'') NOT IN ('PHYSICAL_SHORTAGE','PHYSICAL_RANGE_UNCONFIRMED')))) THEN RAISE EXCEPTION 'retired parent retains allocation' USING ERRCODE='23514'; END IF;
 END IF;
 RETURN NULL;
END $$;
