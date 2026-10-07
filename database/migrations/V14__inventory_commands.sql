-- Command foundations; public adapters never expose these as writable projections.
CREATE TABLE mulino_inventory_Stocktakes (
 organizationId VARCHAR(36) NOT NULL, ID VARCHAR(36) NOT NULL,
 revision INTEGER NOT NULL DEFAULT 0, createdAt TIMESTAMPTZ NOT NULL,
 recordedAt TIMESTAMPTZ NOT NULL, segmentId VARCHAR(36) NOT NULL,
 observedQuantity NUMERIC(38,12) NOT NULL CHECK(observedQuantity >= 0),
 unit VARCHAR(40) NOT NULL, occurredAt TIMESTAMPTZ NOT NULL,
 evidenceRef VARCHAR(240) NOT NULL, commandId VARCHAR(36) NOT NULL,
 PRIMARY KEY(organizationId,ID),
 FOREIGN KEY(organizationId,segmentId) REFERENCES mulino_inventory_QuantitySegments(organizationId,ID)
);
CREATE TABLE mulino_inventory_IdentifierConflicts (
 organizationId VARCHAR(36) NOT NULL, ID VARCHAR(36) NOT NULL,
 revision INTEGER NOT NULL DEFAULT 0, createdAt TIMESTAMPTZ NOT NULL,
 recordedAt TIMESTAMPTZ NOT NULL, itemId VARCHAR(36) NOT NULL,
 existingIdentifierId VARCHAR(36) NOT NULL, issuer VARCHAR(240) NOT NULL,
 namespace VARCHAR(160) NOT NULL, value VARCHAR(240) NOT NULL,
 validFrom TIMESTAMPTZ NOT NULL, validUntil TIMESTAMPTZ,
 state VARCHAR(40) NOT NULL CHECK(state='RECONCILIATION_REQUIRED'),
 PRIMARY KEY(organizationId,ID),
 FOREIGN KEY(organizationId,itemId) REFERENCES mulino_inventory_TradeItems(organizationId,ID),
 FOREIGN KEY(organizationId,existingIdentifierId) REFERENCES mulino_inventory_ExternalIdentifiers(organizationId,ID)
);
CREATE TABLE mulino_inventory_Restrictions (
 organizationId VARCHAR(36) NOT NULL, ID VARCHAR(36) NOT NULL,
 revision INTEGER NOT NULL DEFAULT 0, createdAt TIMESTAMPTZ NOT NULL,
 recordedAt TIMESTAMPTZ NOT NULL, controlScope VARCHAR(160) NOT NULL,
 action VARCHAR(80) NOT NULL, state VARCHAR(40) NOT NULL CHECK(state IN ('ACTIVE','RELEASED')),
 validFrom TIMESTAMPTZ NOT NULL, validUntil TIMESTAMPTZ,
 decisionId VARCHAR(36) NOT NULL, evidenceRef VARCHAR(240) NOT NULL,
 PRIMARY KEY(organizationId,ID), CHECK(validUntil IS NULL OR validUntil > validFrom)
);
CREATE TABLE mulino_inventory_DispositionBases (
 organizationId VARCHAR(36) NOT NULL, ID VARCHAR(36) NOT NULL,
 revision INTEGER NOT NULL DEFAULT 0, createdAt TIMESTAMPTZ NOT NULL,
 recordedAt TIMESTAMPTZ NOT NULL, controlScope VARCHAR(160) NOT NULL,
 action VARCHAR(80) NOT NULL, state VARCHAR(40) NOT NULL CHECK(state IN ('CONFIRMED','REVOKED')),
 validFrom TIMESTAMPTZ NOT NULL, validUntil TIMESTAMPTZ NOT NULL,
 decisionId VARCHAR(36) NOT NULL, evidenceRef VARCHAR(240) NOT NULL,
 PRIMARY KEY(organizationId,ID), CHECK(validUntil > validFrom)
);
CREATE TABLE mulino_inventory_SegmentAllocations (
 organizationId VARCHAR(36) NOT NULL, ID VARCHAR(36) NOT NULL,
 revision INTEGER NOT NULL DEFAULT 0, createdAt TIMESTAMPTZ NOT NULL,
 recordedAt TIMESTAMPTZ NOT NULL, rootId VARCHAR(36) NOT NULL,
 segmentId VARCHAR(36) NOT NULL, orderLineId VARCHAR(36) NOT NULL,
 quantity NUMERIC(38,12) NOT NULL CHECK(quantity > 0), unit VARCHAR(40) NOT NULL,
 state VARCHAR(40) NOT NULL CHECK(state IN ('EXECUTABLE','SUSPENDED','REPLACED','CONSUMED','RELEASED')),
 predecessorId VARCHAR(36), commandId VARCHAR(36) NOT NULL,
 PRIMARY KEY(organizationId,ID),
 FOREIGN KEY(organizationId,segmentId) REFERENCES mulino_inventory_QuantitySegments(organizationId,ID),
 FOREIGN KEY(organizationId,predecessorId) REFERENCES mulino_inventory_SegmentAllocations(organizationId,ID)
);
CREATE INDEX inventory_restriction_scope ON mulino_inventory_Restrictions(organizationId,controlScope,state);
CREATE INDEX inventory_allocation_segment ON mulino_inventory_SegmentAllocations(organizationId,segmentId,state);
-- Same stable fence as command preparation, including insertion into a previously empty scope.
CREATE FUNCTION inventory_control_fence() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 PERFORM pg_advisory_xact_lock(hashtextextended(NEW.organizationId||':inventory/control/'||NEW.controlScope,0));
 RETURN NEW;
END $$;
CREATE TRIGGER inventory_restriction_fence BEFORE INSERT OR UPDATE ON mulino_inventory_Restrictions FOR EACH ROW EXECUTE FUNCTION inventory_control_fence();
CREATE TRIGGER inventory_basis_fence BEFORE INSERT OR UPDATE ON mulino_inventory_DispositionBases FOR EACH ROW EXECUTE FUNCTION inventory_control_fence();
-- Structural replacement conserves the full parent, including explicitly approved decrease.
CREATE FUNCTION inventory_conservation() RETURNS TRIGGER LANGUAGE plpgsql AS $$
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
  IF EXISTS(SELECT 1 FROM mulino_inventory_SegmentAllocations WHERE organizationId=org AND segmentId=sid AND state IN ('EXECUTABLE','SUSPENDED')) THEN RAISE EXCEPTION 'retired parent retains allocation' USING ERRCODE='23514'; END IF;
 END IF;
 RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER inventory_segment_conservation AFTER INSERT OR UPDATE ON mulino_inventory_QuantitySegments DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION inventory_conservation();
CREATE CONSTRAINT TRIGGER inventory_edge_conservation AFTER INSERT ON mulino_inventory_GenealogyEdges DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION inventory_conservation();
CREATE CONSTRAINT TRIGGER inventory_decrease_conservation AFTER INSERT ON mulino_inventory_QuantityMovements DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION inventory_conservation();
-- Original physical identity/amount is immutable; moves replace a leaf instead of rewriting history.
CREATE FUNCTION inventory_physical_immutable() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.itemId<>OLD.itemId OR NEW.lotId IS DISTINCT FROM OLD.lotId OR NEW.quantity<>OLD.quantity OR NEW.unit<>OLD.unit OR NEW.placeId<>OLD.placeId OR NEW.custodianId IS DISTINCT FROM OLD.custodianId OR NEW.ownerId IS DISTINCT FROM OLD.ownerId OR NEW.controlScope<>OLD.controlScope OR NEW.validFrom<>OLD.validFrom OR NEW.recordedAt<>OLD.recordedAt OR (OLD.retiredAt IS NOT NULL AND (NEW.retiredAt IS DISTINCT FROM OLD.retiredAt OR NEW.retirementRecordedAt IS DISTINCT FROM OLD.retirementRecordedAt)) THEN RAISE EXCEPTION 'physical history immutable' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER inventory_physical_immutable BEFORE UPDATE ON mulino_inventory_QuantitySegments FOR EACH ROW EXECUTE FUNCTION inventory_physical_immutable();
CREATE TRIGGER inventory_stocktake_immutable BEFORE UPDATE OR DELETE ON mulino_inventory_Stocktakes FOR EACH ROW EXECUTE FUNCTION inventory_version_immutable();
