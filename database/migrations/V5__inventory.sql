-- Stable inventory identities and active physical lineage. Flyway is the DDL owner.

CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE mulino_inventory_Products (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 0,
  createdAt TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  recordedAt TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  name VARCHAR(240) NOT NULL,
  PRIMARY KEY (organizationId, ID),
  FOREIGN KEY (organizationId) REFERENCES mulino_identity_Organizations(ID)
);

CREATE TABLE mulino_inventory_SpecificationVersions (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 0,
  createdAt TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  recordedAt TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  productId VARCHAR(36) NOT NULL,
  version VARCHAR(80) NOT NULL,
  contentHash VARCHAR(64) NOT NULL,
  description TEXT,
  PRIMARY KEY (organizationId, ID),
  FOREIGN KEY (organizationId) REFERENCES mulino_identity_Organizations(ID),
  FOREIGN KEY (organizationId,productId) REFERENCES mulino_inventory_Products(organizationId,ID)
);

CREATE TABLE mulino_inventory_PackagingVersions (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 0,
  createdAt TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  recordedAt TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  productId VARCHAR(36) NOT NULL,
  version VARCHAR(80) NOT NULL,
  contentHash VARCHAR(64) NOT NULL,
  description TEXT,
  PRIMARY KEY (organizationId, ID),
  FOREIGN KEY (organizationId) REFERENCES mulino_identity_Organizations(ID),
  FOREIGN KEY (organizationId,productId) REFERENCES mulino_inventory_Products(organizationId,ID)
);

CREATE TABLE mulino_inventory_TradeItems (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 0,
  createdAt TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  recordedAt TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  productId VARCHAR(36) NOT NULL,
  name VARCHAR(240) NOT NULL,
  baseUnit VARCHAR(40) NOT NULL,
  decimalPlaces INTEGER NOT NULL,
  specificationVersionId VARCHAR(36),
  packagingVersionId VARCHAR(36),
  PRIMARY KEY (organizationId, ID),
  FOREIGN KEY (organizationId) REFERENCES mulino_identity_Organizations(ID),
  FOREIGN KEY (organizationId,productId) REFERENCES mulino_inventory_Products(organizationId,ID),
  FOREIGN KEY (organizationId,specificationVersionId) REFERENCES mulino_inventory_SpecificationVersions(organizationId,ID),
  FOREIGN KEY (organizationId,packagingVersionId) REFERENCES mulino_inventory_PackagingVersions(organizationId,ID),
  CHECK (decimalPlaces BETWEEN 0 AND 12),
  UNIQUE (organizationId,ID,baseUnit),
  UNIQUE (organizationId,ID,productId)
);

CREATE TABLE mulino_inventory_UnitConversions (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 0,
  createdAt TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  recordedAt TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  itemId VARCHAR(36) NOT NULL,
  fromUnit VARCHAR(40) NOT NULL,
  toUnit VARCHAR(40) NOT NULL,
  factor NUMERIC(38,12) NOT NULL,
  validFrom TIMESTAMPTZ NOT NULL,
  validUntil TIMESTAMPTZ,
  evidenceRef VARCHAR(240),
  PRIMARY KEY (organizationId, ID),
  FOREIGN KEY (organizationId) REFERENCES mulino_identity_Organizations(ID),
  FOREIGN KEY (organizationId,itemId) REFERENCES mulino_inventory_TradeItems(organizationId,ID),
  CHECK (factor > 0),
  CHECK (validUntil IS NULL OR validUntil > validFrom)
);

CREATE TABLE mulino_inventory_ExternalIdentifiers (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 0,
  createdAt TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  recordedAt TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  itemId VARCHAR(36) NOT NULL,
  issuer VARCHAR(240) NOT NULL,
  namespace VARCHAR(160) NOT NULL,
  value VARCHAR(240) NOT NULL,
  validFrom TIMESTAMPTZ NOT NULL,
  validUntil TIMESTAMPTZ,
  PRIMARY KEY (organizationId, ID),
  FOREIGN KEY (organizationId) REFERENCES mulino_identity_Organizations(ID),
  FOREIGN KEY (organizationId,itemId) REFERENCES mulino_inventory_TradeItems(organizationId,ID),
  CHECK (validUntil IS NULL OR validUntil > validFrom)
);

CREATE TABLE mulino_inventory_Manufacturers (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 0,
  createdAt TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  recordedAt TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  name VARCHAR(240) NOT NULL,
  PRIMARY KEY (organizationId, ID),
  FOREIGN KEY (organizationId) REFERENCES mulino_identity_Organizations(ID)
);

CREATE TABLE mulino_inventory_ManufacturingLots (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 0,
  createdAt TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  recordedAt TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  manufacturerId VARCHAR(36) NOT NULL,
  itemId VARCHAR(36) NOT NULL,
  originalLot VARCHAR(240) NOT NULL,
  productionAt TIMESTAMPTZ,
  expiresAt TIMESTAMPTZ,
  evidenceRef VARCHAR(240),
  PRIMARY KEY (organizationId, ID),
  FOREIGN KEY (organizationId) REFERENCES mulino_identity_Organizations(ID),
  FOREIGN KEY (organizationId,itemId) REFERENCES mulino_inventory_TradeItems(organizationId,ID),
  FOREIGN KEY (organizationId,manufacturerId) REFERENCES mulino_inventory_Manufacturers(organizationId,ID),
  UNIQUE (organizationId,manufacturerId,itemId,originalLot),
  UNIQUE (organizationId,ID,itemId)
);

CREATE TABLE mulino_inventory_Places (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 0,
  createdAt TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  recordedAt TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  name VARCHAR(240) NOT NULL,
  parentId VARCHAR(36),
  kind VARCHAR(40) NOT NULL,
  PRIMARY KEY (organizationId, ID),
  FOREIGN KEY (organizationId) REFERENCES mulino_identity_Organizations(ID),
  FOREIGN KEY (organizationId,parentId) REFERENCES mulino_inventory_Places(organizationId,ID)
);

CREATE TABLE mulino_inventory_QuantitySegments (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 0,
  createdAt TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  recordedAt TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  itemId VARCHAR(36) NOT NULL,
  lotId VARCHAR(36),
  identificationStatus VARCHAR(40) NOT NULL,
  quantity NUMERIC(38,12) NOT NULL,
  unit VARCHAR(40) NOT NULL,
  placeId VARCHAR(36) NOT NULL,
  custodianId VARCHAR(36),
  ownerId VARCHAR(36),
  controlScope VARCHAR(160) NOT NULL,
  validFrom TIMESTAMPTZ NOT NULL,
  retiredAt TIMESTAMPTZ,
  retirementRecordedAt TIMESTAMPTZ,
  mixtureStatus VARCHAR(40) NOT NULL,
  evidenceRef VARCHAR(240),
  PRIMARY KEY (organizationId, ID),
  FOREIGN KEY (organizationId) REFERENCES mulino_identity_Organizations(ID),
  FOREIGN KEY (organizationId,itemId) REFERENCES mulino_inventory_TradeItems(organizationId,ID),
  FOREIGN KEY (organizationId,lotId) REFERENCES mulino_inventory_ManufacturingLots(organizationId,ID),
  FOREIGN KEY (organizationId,placeId) REFERENCES mulino_inventory_Places(organizationId,ID),
  CHECK (quantity > 0),
  CHECK (identificationStatus IN ('CONFIRMED','UNKNOWN','CONFLICT')),
  CHECK (mixtureStatus IN ('IDENTIFIED','UNCERTAIN_MIXTURE')),
  CHECK ((retiredAt IS NULL) = (retirementRecordedAt IS NULL)),
  CHECK (retiredAt IS NULL OR retiredAt >= validFrom),
  CHECK (identificationStatus <> 'CONFIRMED' OR lotId IS NOT NULL),
  FOREIGN KEY (organizationId,itemId,unit) REFERENCES mulino_inventory_TradeItems(organizationId,ID,baseUnit),
  FOREIGN KEY (organizationId,lotId,itemId) REFERENCES mulino_inventory_ManufacturingLots(organizationId,ID,itemId)
);

CREATE TABLE mulino_inventory_LogisticsUnits (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 0,
  createdAt TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  recordedAt TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  name VARCHAR(240) NOT NULL,
  PRIMARY KEY (organizationId, ID),
  FOREIGN KEY (organizationId) REFERENCES mulino_identity_Organizations(ID)
);

CREATE TABLE mulino_inventory_LogisticsMemberships (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 0,
  createdAt TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  recordedAt TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  logisticsUnitId VARCHAR(36) NOT NULL,
  segmentId VARCHAR(36) NOT NULL,
  validFrom TIMESTAMPTZ NOT NULL,
  validUntil TIMESTAMPTZ,
  PRIMARY KEY (organizationId, ID),
  FOREIGN KEY (organizationId) REFERENCES mulino_identity_Organizations(ID),
  FOREIGN KEY (organizationId,logisticsUnitId) REFERENCES mulino_inventory_LogisticsUnits(organizationId,ID),
  FOREIGN KEY (organizationId,segmentId) REFERENCES mulino_inventory_QuantitySegments(organizationId,ID),
  CHECK (validUntil IS NULL OR validUntil > validFrom)
);

CREATE TABLE mulino_inventory_GenealogyEdges (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 0,
  createdAt TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  recordedAt TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  sourceId VARCHAR(36),
  targetId VARCHAR(36),
  quantity NUMERIC(38,12) NOT NULL,
  unit VARCHAR(40) NOT NULL,
  kind VARCHAR(40) NOT NULL,
  uncertain BOOLEAN NOT NULL,
  occurredAt TIMESTAMPTZ NOT NULL,
  evidenceRef VARCHAR(240),
  PRIMARY KEY (organizationId, ID),
  FOREIGN KEY (organizationId) REFERENCES mulino_identity_Organizations(ID),
  FOREIGN KEY (organizationId,sourceId) REFERENCES mulino_inventory_QuantitySegments(organizationId,ID),
  FOREIGN KEY (organizationId,targetId) REFERENCES mulino_inventory_QuantitySegments(organizationId,ID),
  CHECK (quantity > 0),
  CHECK (sourceId <> targetId),
  UNIQUE (organizationId,sourceId,targetId,kind)
);

CREATE TABLE mulino_inventory_QuantityMovements (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 0,
  createdAt TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  recordedAt TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  sourceId VARCHAR(36),
  targetId VARCHAR(36),
  quantity NUMERIC(38,12) NOT NULL,
  unit VARCHAR(40) NOT NULL,
  kind VARCHAR(40) NOT NULL,
  occurredAt TIMESTAMPTZ NOT NULL,
  commandId VARCHAR(36),
  evidenceRef VARCHAR(240),
  PRIMARY KEY (organizationId, ID),
  FOREIGN KEY (organizationId) REFERENCES mulino_identity_Organizations(ID),
  FOREIGN KEY (organizationId,sourceId) REFERENCES mulino_inventory_QuantitySegments(organizationId,ID),
  FOREIGN KEY (organizationId,targetId) REFERENCES mulino_inventory_QuantitySegments(organizationId,ID),
  CHECK (quantity > 0)
);

ALTER TABLE mulino_inventory_ExternalIdentifiers ADD CONSTRAINT inventory_external_namespace_time EXCLUDE USING gist (organizationId WITH =, issuer WITH =, namespace WITH =, value WITH =, tstzrange(validFrom,validUntil,'[)') WITH &&);

ALTER TABLE mulino_inventory_LogisticsMemberships ADD CONSTRAINT inventory_one_logistics_parent EXCLUDE USING gist (organizationId WITH =, segmentId WITH =, tstzrange(validFrom,validUntil,'[)') WITH &&);

CREATE FUNCTION inventory_quantity_precision() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE places INTEGER;
BEGIN
 SELECT decimalPlaces INTO places FROM mulino_inventory_TradeItems WHERE organizationId=NEW.organizationId AND ID=NEW.itemId;
 IF NEW.quantity <> trunc(NEW.quantity,places) THEN RAISE EXCEPTION 'base unit precision exceeded' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER inventory_quantity_precision BEFORE INSERT OR UPDATE ON mulino_inventory_QuantitySegments FOR EACH ROW EXECUTE FUNCTION inventory_quantity_precision();

CREATE FUNCTION inventory_genealogy_cycle() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 PERFORM pg_advisory_xact_lock(hashtextextended(NEW.organizationId||'inventory-genealogy',0));
 IF EXISTS (WITH RECURSIVE descendants(id) AS (SELECT NEW.targetId UNION SELECT g.targetId FROM mulino_inventory_GenealogyEdges g JOIN descendants d ON g.sourceId=d.id WHERE g.organizationId=NEW.organizationId) SELECT 1 FROM descendants WHERE id=NEW.sourceId) THEN RAISE EXCEPTION 'genealogy cycle' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER inventory_genealogy_cycle BEFORE INSERT OR UPDATE ON mulino_inventory_GenealogyEdges FOR EACH ROW EXECUTE FUNCTION inventory_genealogy_cycle();

ALTER TABLE mulino_inventory_SpecificationVersions ADD UNIQUE (organizationId,ID,productId);
ALTER TABLE mulino_inventory_PackagingVersions ADD UNIQUE (organizationId,ID,productId);
ALTER TABLE mulino_inventory_TradeItems ALTER COLUMN specificationVersionId SET NOT NULL;
ALTER TABLE mulino_inventory_TradeItems ALTER COLUMN packagingVersionId SET NOT NULL;
ALTER TABLE mulino_inventory_TradeItems ADD FOREIGN KEY (organizationId,specificationVersionId,productId) REFERENCES mulino_inventory_SpecificationVersions(organizationId,ID,productId);
ALTER TABLE mulino_inventory_TradeItems ADD FOREIGN KEY (organizationId,packagingVersionId,productId) REFERENCES mulino_inventory_PackagingVersions(organizationId,ID,productId);
ALTER TABLE mulino_inventory_QuantitySegments ADD FOREIGN KEY (organizationId,custodianId) REFERENCES mulino_identity_Actors(organizationId,ID);
ALTER TABLE mulino_inventory_QuantitySegments ADD FOREIGN KEY (organizationId,ownerId) REFERENCES mulino_identity_Actors(organizationId,ID);
CREATE FUNCTION inventory_version_immutable() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'inventory version is immutable' USING ERRCODE='23514'; END $$;
CREATE TRIGGER inventory_spec_immutable BEFORE UPDATE OR DELETE ON mulino_inventory_SpecificationVersions FOR EACH ROW EXECUTE FUNCTION inventory_version_immutable();
CREATE TRIGGER inventory_package_immutable BEFORE UPDATE OR DELETE ON mulino_inventory_PackagingVersions FOR EACH ROW EXECUTE FUNCTION inventory_version_immutable();

-- Every structural decomposition consumes the parent; checked at transaction end.
CREATE FUNCTION inventory_retired_parent() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE parent_qty NUMERIC(38,12); parent_retired TIMESTAMPTZ; total NUMERIC; source_item VARCHAR(36); source_lot VARCHAR(36); target_item VARCHAR(36); target_lot VARCHAR(36);
BEGIN
 SELECT quantity,retiredAt,itemId,lotId INTO parent_qty,parent_retired,source_item,source_lot FROM mulino_inventory_QuantitySegments WHERE organizationId=NEW.organizationId AND ID=NEW.sourceId;
 SELECT itemId,lotId INTO target_item,target_lot FROM mulino_inventory_QuantitySegments WHERE organizationId=NEW.organizationId AND ID=NEW.targetId;
 IF parent_retired IS NULL THEN RAISE EXCEPTION 'lineage source must be retired' USING ERRCODE='23514'; END IF;
 IF source_item <> target_item OR source_lot IS DISTINCT FROM target_lot THEN RAISE EXCEPTION 'lineage item/LOT mismatch' USING ERRCODE='23514'; END IF;
 SELECT SUM(quantity) INTO total FROM mulino_inventory_GenealogyEdges WHERE organizationId=NEW.organizationId AND sourceId=NEW.sourceId;
 IF total > parent_qty THEN RAISE EXCEPTION 'lineage exceeds source quantity' USING ERRCODE='23514'; END IF;
 RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER inventory_retired_parent AFTER INSERT OR UPDATE ON mulino_inventory_GenealogyEdges DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION inventory_retired_parent();
CREATE INDEX inventory_segments_scope ON mulino_inventory_QuantitySegments(organizationId,itemId,placeId,validFrom,retiredAt);
CREATE INDEX inventory_genealogy_target ON mulino_inventory_GenealogyEdges(organizationId,targetId);

CREATE TABLE mulino_inventory_ObjectRelations (
 organizationId VARCHAR(36) NOT NULL, ID VARCHAR(36) NOT NULL,
 revision INTEGER NOT NULL DEFAULT 0,
 createdAt TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 recordedAt TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 definitionVersionId VARCHAR(36) NOT NULL, relationDefinitionId VARCHAR(36) NOT NULL,
 sourceType VARCHAR(80) NOT NULL, sourceId VARCHAR(36) NOT NULL,
 targetType VARCHAR(80) NOT NULL, targetId VARCHAR(36) NOT NULL,
 validFrom TIMESTAMPTZ NOT NULL, validUntil TIMESTAMPTZ,
 PRIMARY KEY(organizationId,ID),
 FOREIGN KEY(organizationId) REFERENCES mulino_identity_Organizations(ID),
 FOREIGN KEY(organizationId,definitionVersionId) REFERENCES mulino_definitions_DefinitionVersions(organizationId,ID),
 FOREIGN KEY(organizationId,relationDefinitionId) REFERENCES mulino_definitions_RelationDefinitions(organizationId,ID),
 CHECK(validUntil IS NULL OR validUntil > validFrom)
);

-- Fixed whitelist of core endpoint types: client type strings never become SQL names.
CREATE FUNCTION inventory_endpoint_exists(org VARCHAR, typ VARCHAR, objectId VARCHAR) RETURNS BOOLEAN LANGUAGE plpgsql AS $$
BEGIN
 CASE typ
 WHEN 'Product' THEN RETURN EXISTS(SELECT 1 FROM mulino_inventory_Products WHERE organizationId=org AND ID=objectId);
 WHEN 'TradeItem' THEN RETURN EXISTS(SELECT 1 FROM mulino_inventory_TradeItems WHERE organizationId=org AND ID=objectId);
 WHEN 'ManufacturingLot' THEN RETURN EXISTS(SELECT 1 FROM mulino_inventory_ManufacturingLots WHERE organizationId=org AND ID=objectId);
 WHEN 'Manufacturer' THEN RETURN EXISTS(SELECT 1 FROM mulino_inventory_Manufacturers WHERE organizationId=org AND ID=objectId);
 WHEN 'QuantitySegment' THEN RETURN EXISTS(SELECT 1 FROM mulino_inventory_QuantitySegments WHERE organizationId=org AND ID=objectId);
 WHEN 'Place' THEN RETURN EXISTS(SELECT 1 FROM mulino_inventory_Places WHERE organizationId=org AND ID=objectId);
 WHEN 'LogisticsUnit' THEN RETURN EXISTS(SELECT 1 FROM mulino_inventory_LogisticsUnits WHERE organizationId=org AND ID=objectId);
 ELSE RETURN FALSE;
 END CASE;
END $$;
CREATE FUNCTION inventory_relation_valid() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE meta RECORD; boundary TIMESTAMPTZ; countAt INTEGER;
BEGIN
 SELECT r.*,v.state INTO meta FROM mulino_definitions_RelationDefinitions r JOIN mulino_definitions_DefinitionVersions v ON v.organizationId=r.organizationId AND v.ID=r.definitionVersionId
 WHERE r.organizationId=NEW.organizationId AND r.ID=NEW.relationDefinitionId AND r.definitionVersionId=NEW.definitionVersionId;
 IF NOT FOUND OR meta.state <> 'PUBLISHED' OR meta.sourceType IS DISTINCT FROM NEW.sourceType OR meta.targetType IS DISTINCT FROM NEW.targetType OR meta.maximumCount IS NULL OR meta.minimumCount IS NULL OR meta.cycleAllowed IS NULL THEN RAISE EXCEPTION 'relation definition/type invalid' USING ERRCODE='23514'; END IF;
 IF NOT inventory_endpoint_exists(NEW.organizationId,NEW.sourceType,NEW.sourceId) OR NOT inventory_endpoint_exists(NEW.organizationId,NEW.targetType,NEW.targetId) THEN RAISE EXCEPTION 'relation endpoint unavailable' USING ERRCODE='23503'; END IF;
 IF meta.name='locatedAt' AND (NEW.sourceType <> 'QuantitySegment' OR NEW.targetType <> 'Place' OR NOT EXISTS(SELECT 1 FROM mulino_inventory_QuantitySegments s WHERE s.organizationId=NEW.organizationId AND s.ID=NEW.sourceId AND s.placeId=NEW.targetId)) THEN RAISE EXCEPTION 'locatedAt contradicts physical location' USING ERRCODE='23514'; END IF;
 -- Same source/definition fence serializes concurrent cardinality validation.
 PERFORM pg_advisory_xact_lock(hashtextextended(NEW.organizationId||NEW.relationDefinitionId,0));
 FOR boundary IN SELECT NEW.validFrom UNION SELECT r.validFrom FROM mulino_inventory_ObjectRelations r WHERE r.organizationId=NEW.organizationId AND r.relationDefinitionId=NEW.relationDefinitionId AND r.sourceId=NEW.sourceId AND r.ID<>NEW.ID AND r.validFrom >= NEW.validFrom AND (NEW.validUntil IS NULL OR r.validFrom < NEW.validUntil)
 LOOP
 SELECT COUNT(*) INTO countAt FROM mulino_inventory_ObjectRelations r WHERE r.organizationId=NEW.organizationId AND r.relationDefinitionId=NEW.relationDefinitionId AND r.sourceId=NEW.sourceId AND r.ID<>NEW.ID AND r.validFrom <= boundary AND (r.validUntil IS NULL OR r.validUntil > boundary);
 IF countAt+1 > meta.maximumCount THEN RAISE EXCEPTION 'relation cardinality exceeded' USING ERRCODE='23514'; END IF;
 END LOOP;
 IF NOT meta.cycleAllowed AND EXISTS(WITH RECURSIVE path(typ,id,span) AS (SELECT NEW.targetType,NEW.targetId,tstzrange(NEW.validFrom,NEW.validUntil,'[)') UNION SELECT r.targetType,r.targetId,p.span*tstzrange(r.validFrom,r.validUntil,'[)') FROM mulino_inventory_ObjectRelations r JOIN path p ON r.sourceType=p.typ AND r.sourceId=p.id WHERE r.organizationId=NEW.organizationId AND r.relationDefinitionId=NEW.relationDefinitionId AND r.ID<>NEW.ID AND tstzrange(r.validFrom,r.validUntil,'[)') && p.span) SELECT 1 FROM path WHERE typ=NEW.sourceType AND id=NEW.sourceId) THEN RAISE EXCEPTION 'relation cycle' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER inventory_relation_valid BEFORE INSERT OR UPDATE ON mulino_inventory_ObjectRelations FOR EACH ROW EXECUTE FUNCTION inventory_relation_valid();
CREATE INDEX inventory_relations_source ON mulino_inventory_ObjectRelations(organizationId,sourceType,sourceId,validFrom,validUntil);
