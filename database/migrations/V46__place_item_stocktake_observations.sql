-- A place/item measurement is an observation, never an allocation of the
-- measured quantity among leaves and never a stock adjustment.
CREATE TABLE mulino_inventory_PlaceStocktakes (
 organizationId VARCHAR(36) NOT NULL, ID VARCHAR(36) NOT NULL,
 revision INTEGER NOT NULL CHECK (revision=0),
 createdAt TIMESTAMPTZ NOT NULL, recordedAt TIMESTAMPTZ NOT NULL,
 itemId VARCHAR(36) NOT NULL, placeId VARCHAR(36) NOT NULL, lotId VARCHAR(36),
 observedQuantity NUMERIC(38,12) NOT NULL CHECK (observedQuantity>=0),
 bookQuantity NUMERIC(38,12) NOT NULL CHECK (bookQuantity>=0), unit VARCHAR(40) NOT NULL,
 occurredAt TIMESTAMPTZ NOT NULL, snapshotAt TIMESTAMPTZ NOT NULL,
 evidenceRef VARCHAR(240) NOT NULL CHECK (length(trim(evidenceRef))>0),
 commandId VARCHAR(36) NOT NULL,
 observationStatus VARCHAR(40) NOT NULL CHECK (observationStatus='RECORDED_UNASSESSED'),
 sourceMembersJson TEXT NOT NULL,
 PRIMARY KEY (organizationId,ID), UNIQUE (organizationId,commandId),
 FOREIGN KEY (organizationId,itemId,unit) REFERENCES mulino_inventory_TradeItems(organizationId,ID,baseUnit),
 FOREIGN KEY (organizationId,placeId) REFERENCES mulino_inventory_Places(organizationId,ID),
 FOREIGN KEY (organizationId,lotId) REFERENCES mulino_inventory_ManufacturingLots(organizationId,ID),
 FOREIGN KEY (organizationId,commandId) REFERENCES mulino_commands_CommandRecords(organizationId,ID),
 CHECK (createdAt=recordedAt AND recordedAt=snapshotAt AND occurredAt<=snapshotAt)
);
CREATE TABLE mulino_inventory_PlaceStocktakeMembers (
 organizationId VARCHAR(36) NOT NULL, ID VARCHAR(36) NOT NULL,
 revision INTEGER NOT NULL CHECK (revision=0),
 createdAt TIMESTAMPTZ NOT NULL, recordedAt TIMESTAMPTZ NOT NULL,
 stocktakeId VARCHAR(36) NOT NULL, segmentId VARCHAR(36) NOT NULL,
 segmentRevision INTEGER NOT NULL CHECK (segmentRevision>=0),
 bookQuantity NUMERIC(38,12) NOT NULL CHECK (bookQuantity>0), unit VARCHAR(40) NOT NULL,
 PRIMARY KEY (organizationId,ID), UNIQUE (organizationId,stocktakeId,segmentId),
 FOREIGN KEY (organizationId,stocktakeId) REFERENCES mulino_inventory_PlaceStocktakes(organizationId,ID),
 FOREIGN KEY (organizationId,segmentId) REFERENCES mulino_inventory_QuantitySegments(organizationId,ID)
);

CREATE FUNCTION inventory_place_stocktake_snapshot() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE members JSONB; total NUMERIC; invalid BOOLEAN; places INTEGER;
BEGIN
 IF NEW.lotId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM mulino_inventory_ManufacturingLots
   WHERE organizationId=NEW.organizationId AND ID=NEW.lotId AND itemId=NEW.itemId) THEN
   RAISE EXCEPTION 'Stocktake LOT differs from item';
 END IF;
 SELECT decimalPlaces INTO places FROM mulino_inventory_TradeItems
   WHERE organizationId=NEW.organizationId AND ID=NEW.itemId;
 IF places IS NULL OR NEW.observedQuantity<>round(NEW.observedQuantity,places) THEN
   RAISE EXCEPTION 'Invalid stocktake measurement precision';
 END IF;
 SELECT COALESCE(jsonb_agg(to_jsonb(s) ORDER BY s.ID),'[]'::jsonb), COALESCE(sum(s.quantity),0),
   COALESCE(bool_or(s.unit<>NEW.unit OR s.createdAt>NEW.snapshotAt OR
      s.recordedAt>NEW.snapshotAt OR s.validFrom>NEW.snapshotAt),false)
 INTO members,total,invalid FROM mulino_inventory_QuantitySegments s
 WHERE s.organizationId=NEW.organizationId AND s.itemId=NEW.itemId
   AND s.placeId=NEW.placeId AND s.retiredAt IS NULL AND (NEW.lotId IS NULL OR s.lotId=NEW.lotId);
 IF invalid OR jsonb_array_length(members)>255 OR total<>NEW.bookQuantity THEN
   RAISE EXCEPTION 'Incomplete or inconsistent current stocktake book snapshot';
 END IF;
 -- Caller-provided source JSON is always replaced with actual SQL rows.
 NEW.sourceMembersJson=members::text;
 RETURN NEW;
END $$;
CREATE TRIGGER place_stocktake_snapshot BEFORE INSERT ON mulino_inventory_PlaceStocktakes
 FOR EACH ROW EXECUTE FUNCTION inventory_place_stocktake_snapshot();

CREATE FUNCTION inventory_place_stocktake_member() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE parent mulino_inventory_PlaceStocktakes%ROWTYPE; source JSONB;
BEGIN
 SELECT * INTO parent FROM mulino_inventory_PlaceStocktakes
  WHERE organizationId=NEW.organizationId AND ID=NEW.stocktakeId;
 SELECT x INTO source FROM jsonb_array_elements(parent.sourceMembersJson::jsonb) x
  WHERE x->>'id'=NEW.segmentId;
 IF source IS NULL OR (source->>'revision')::integer<>NEW.segmentRevision
    OR (source->>'quantity')::numeric<>NEW.bookQuantity OR source->>'unit'<>NEW.unit
    OR NEW.createdAt<>parent.createdAt OR NEW.recordedAt<>parent.recordedAt THEN
  RAISE EXCEPTION 'Stocktake member differs from captured book source';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER place_stocktake_member BEFORE INSERT ON mulino_inventory_PlaceStocktakeMembers
 FOR EACH ROW EXECUTE FUNCTION inventory_place_stocktake_member();

CREATE FUNCTION inventory_place_stocktake_complete() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE parent mulino_inventory_PlaceStocktakes%ROWTYPE; result JSONB;
        memberCount BIGINT; total NUMERIC; cid VARCHAR(36);
BEGIN
 IF TG_TABLE_NAME='mulino_inventory_placestocktakes' THEN cid=NEW.ID; ELSE cid=NEW.stocktakeId; END IF;
 SELECT * INTO parent FROM mulino_inventory_PlaceStocktakes WHERE organizationId=NEW.organizationId AND ID=cid;
 SELECT count(*),COALESCE(sum(bookQuantity),0) INTO memberCount,total
  FROM mulino_inventory_PlaceStocktakeMembers WHERE organizationId=parent.organizationId AND stocktakeId=parent.ID;
 IF memberCount<>jsonb_array_length(parent.sourceMembersJson::jsonb) OR total<>parent.bookQuantity THEN
   RAISE EXCEPTION 'Stocktake members are incomplete';
 END IF;
 SELECT resultJson::jsonb INTO result FROM mulino_commands_CommandRecords
  WHERE organizationId=parent.organizationId AND ID=parent.commandId
   AND state='COMMITTED' AND capabilityId='recordStocktake';
 IF result IS NULL OR result->>'outcome' IS DISTINCT FROM 'APPLIED'
   OR result#>>'{effects,placeStocktakeId}' IS DISTINCT FROM parent.ID
   OR result#>'{effects,quantityEffects}' IS DISTINCT FROM '[]'::jsonb THEN
   RAISE EXCEPTION 'Stocktake must belong to its committed observation command';
 END IF;
 RETURN NEW;
END $$;
CREATE CONSTRAINT TRIGGER place_stocktake_complete AFTER INSERT ON mulino_inventory_PlaceStocktakes
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION inventory_place_stocktake_complete();
CREATE CONSTRAINT TRIGGER place_stocktake_members_complete AFTER INSERT ON mulino_inventory_PlaceStocktakeMembers
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION inventory_place_stocktake_complete();
CREATE TRIGGER place_stocktake_immutable BEFORE UPDATE OR DELETE ON mulino_inventory_PlaceStocktakes
 FOR EACH ROW EXECUTE FUNCTION inventory_version_immutable();
CREATE TRIGGER place_stocktake_members_immutable BEFORE UPDATE OR DELETE ON mulino_inventory_PlaceStocktakeMembers
 FOR EACH ROW EXECUTE FUNCTION inventory_version_immutable();
CREATE FUNCTION inventory_place_stocktake_no_truncate() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE populated BOOLEAN;
BEGIN
 EXECUTE format('SELECT EXISTS (SELECT 1 FROM %I.%I)',TG_TABLE_SCHEMA,TG_TABLE_NAME) INTO populated;
 IF populated THEN RAISE EXCEPTION 'Stocktake observation is immutable'; END IF;
 RETURN NULL;
END $$;
CREATE TRIGGER place_stocktake_no_truncate BEFORE TRUNCATE ON mulino_inventory_PlaceStocktakes
 FOR EACH STATEMENT EXECUTE FUNCTION inventory_place_stocktake_no_truncate();
CREATE TRIGGER place_stocktake_members_no_truncate BEFORE TRUNCATE ON mulino_inventory_PlaceStocktakeMembers
 FOR EACH STATEMENT EXECUTE FUNCTION inventory_place_stocktake_no_truncate();
