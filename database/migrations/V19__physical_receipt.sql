-- Provisional observations cannot create eligible stock. Physical receipt history is immutable.
CREATE TABLE mulino_trade_receipt_Observations (
 organizationId varchar(36) NOT NULL, ID varchar(36) NOT NULL,
 revision integer NOT NULL CHECK(revision>=0), createdAt timestamptz NOT NULL,
 recordedAt timestamptz NOT NULL, occurredAt timestamptz NOT NULL,
 eventId varchar(36) NOT NULL, rangeRootId varchar(36) NOT NULL,
 startQuantity numeric(38,12) NOT NULL CHECK(startQuantity>=0),
 quantity numeric(38,12) NOT NULL CHECK(quantity>0), unit varchar(40) NOT NULL,
 itemId varchar(36) NOT NULL, lotId varchar(36), placeId varchar(36) NOT NULL,
 purchaseLineId varchar(36), transitSegmentId varchar(36),
 identificationStatus varchar(40) NOT NULL CHECK(identificationStatus IN ('UNKNOWN','CLAIMED')),
 state varchar(40) NOT NULL CHECK(state IN ('PROVISIONAL','CONFIRMED')), workId varchar(36) NOT NULL,
 ownerId varchar(36) NOT NULL, supervisorId varchar(36) NOT NULL,
 nextAction varchar(320) NOT NULL, nextCheckAt timestamptz NOT NULL,
 PRIMARY KEY(organizationId,ID),
 FOREIGN KEY(organizationId,eventId) REFERENCES mulino_evidence_Events(organizationId,ID),
 FOREIGN KEY(organizationId,itemId) REFERENCES mulino_inventory_TradeItems(organizationId,ID),
 FOREIGN KEY(organizationId,lotId) REFERENCES mulino_inventory_ManufacturingLots(organizationId,ID),
 FOREIGN KEY(organizationId,placeId) REFERENCES mulino_inventory_Places(organizationId,ID),
 FOREIGN KEY(organizationId,transitSegmentId) REFERENCES mulino_inventory_QuantitySegments(organizationId,ID),
 FOREIGN KEY(organizationId,purchaseLineId) REFERENCES mulino_trade_purchase_OrderLines(organizationId,ID),
 FOREIGN KEY(organizationId,workId) REFERENCES mulino_work_read_Works(organizationId,ID),
 FOREIGN KEY(organizationId,ownerId) REFERENCES mulino_identity_Actors(organizationId,ID),
 FOREIGN KEY(organizationId,supervisorId) REFERENCES mulino_identity_Actors(organizationId,ID)
);
CREATE INDEX ON mulino_trade_receipt_Observations(organizationId,rangeRootId);
CREATE TABLE mulino_trade_receipt_Receipts (
 organizationId varchar(36) NOT NULL, ID varchar(36) NOT NULL,
 revision integer NOT NULL CHECK(revision>=0), createdAt timestamptz NOT NULL,
 recordedAt timestamptz NOT NULL, occurredAt timestamptz NOT NULL,
 observationId varchar(36) NOT NULL, canonicalOccurrenceId varchar(36) NOT NULL,
 rangeRootId varchar(36) NOT NULL, startQuantity numeric(38,12) NOT NULL CHECK(startQuantity>=0),
 quantity numeric(38,12) NOT NULL CHECK(quantity>0), unit varchar(40) NOT NULL,
 itemId varchar(36) NOT NULL, lotId varchar(36) NOT NULL, placeId varchar(36) NOT NULL,
 segmentId varchar(36) NOT NULL, purchaseLineId varchar(36), workId varchar(36) NOT NULL,
 contributedQuantity numeric(38,12) NOT NULL CHECK(contributedQuantity>=0),
 excessQuantity numeric(38,12) NOT NULL CHECK(excessQuantity>=0),
 physicalEffect varchar(40) NOT NULL CHECK(physicalEffect IN ('NEW_STOCK','TRANSIT_MOVE')),
 CHECK(contributedQuantity+excessQuantity=quantity),
 PRIMARY KEY(organizationId,ID), UNIQUE(organizationId,canonicalOccurrenceId),
 UNIQUE(organizationId,rangeRootId,startQuantity),
 FOREIGN KEY(organizationId,observationId) REFERENCES mulino_trade_receipt_Observations(organizationId,ID),
 FOREIGN KEY(organizationId,canonicalOccurrenceId) REFERENCES mulino_evidence_CanonicalOccurrences(organizationId,ID),
 FOREIGN KEY(organizationId,itemId) REFERENCES mulino_inventory_TradeItems(organizationId,ID),
 FOREIGN KEY(organizationId,lotId) REFERENCES mulino_inventory_ManufacturingLots(organizationId,ID),
 FOREIGN KEY(organizationId,placeId) REFERENCES mulino_inventory_Places(organizationId,ID),
 FOREIGN KEY(organizationId,segmentId) REFERENCES mulino_inventory_QuantitySegments(organizationId,ID),
 FOREIGN KEY(organizationId,purchaseLineId) REFERENCES mulino_trade_purchase_OrderLines(organizationId,ID),
 FOREIGN KEY(organizationId,workId) REFERENCES mulino_work_read_Works(organizationId,ID)
);
CREATE TRIGGER receipt_immutable BEFORE UPDATE OR DELETE ON mulino_trade_receipt_Receipts
 FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
CREATE FUNCTION mulino_receipt_range_guard() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 PERFORM pg_advisory_xact_lock(hashtextextended(NEW.organizationId||':receipt/range/'||NEW.rangeRootId,0));
 IF EXISTS(SELECT 1 FROM mulino_trade_receipt_Receipts r WHERE r.organizationId=NEW.organizationId
 AND r.rangeRootId=NEW.rangeRootId AND r.startQuantity<NEW.startQuantity+NEW.quantity
 AND NEW.startQuantity<r.startQuantity+r.quantity) THEN RAISE EXCEPTION 'Committed physical receipt ranges overlap'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER receipt_range_guard BEFORE INSERT ON mulino_trade_receipt_Receipts
 FOR EACH ROW EXECUTE FUNCTION mulino_receipt_range_guard();
CREATE FUNCTION mulino_receipt_observation_guard() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Receipt observation history cannot be deleted'; END IF;
 IF (to_jsonb(NEW)-'state'-'revision') IS DISTINCT FROM (to_jsonb(OLD)-'state'-'revision')
 OR OLD.state<>'PROVISIONAL' OR NEW.state<>'CONFIRMED' OR NEW.revision<>OLD.revision+1 THEN
  RAISE EXCEPTION 'Receipt observation identity is immutable';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER receipt_observation_guard BEFORE UPDATE OR DELETE ON mulino_trade_receipt_Observations
 FOR EACH ROW EXECUTE FUNCTION mulino_receipt_observation_guard();
