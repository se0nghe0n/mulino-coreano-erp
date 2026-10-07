CREATE TABLE mulino_responsibility_ReceiptResidualRoots (
 organizationId varchar(36) NOT NULL, ID varchar(36) NOT NULL, revision integer NOT NULL DEFAULT 0,
 createdAt timestamptz NOT NULL, recordedAt timestamptz NOT NULL,
 rootId varchar(36) NOT NULL, lineId varchar(36) NOT NULL, initialReceiptId varchar(36) NOT NULL,
 initialContribution numeric(38,12) NOT NULL, orderedQuantity numeric(38,12) NOT NULL, unit varchar(20) NOT NULL,
 PRIMARY KEY(organizationId,ID), UNIQUE(organizationId,rootId), UNIQUE(organizationId,lineId),
 FOREIGN KEY(organizationId,rootId) REFERENCES mulino_responsibility_Roots(organizationId,ID),
 FOREIGN KEY(organizationId,lineId) REFERENCES mulino_trade_purchase_OrderLines(organizationId,ID),
 FOREIGN KEY(organizationId,initialReceiptId) REFERENCES mulino_trade_receipt_Receipts(organizationId,ID),
 CHECK(initialContribution>=0 AND orderedQuantity>initialContribution)
);
CREATE TABLE mulino_responsibility_ReceiptResidualCredits (
 organizationId varchar(36) NOT NULL, ID varchar(36) NOT NULL, revision integer NOT NULL DEFAULT 0,
 createdAt timestamptz NOT NULL, recordedAt timestamptz NOT NULL,
 rootId varchar(36) NOT NULL, scopeId varchar(36) NOT NULL, assignmentId varchar(36) NOT NULL,
 receiptId varchar(36) NOT NULL, startQuantity numeric(38,12) NOT NULL, quantity numeric(38,12) NOT NULL,
 PRIMARY KEY(organizationId,ID), UNIQUE(organizationId,assignmentId),
 FOREIGN KEY(organizationId,rootId) REFERENCES mulino_responsibility_ReceiptResidualRoots(organizationId,rootId),
 FOREIGN KEY(organizationId,scopeId,rootId) REFERENCES mulino_responsibility_Scopes(organizationId,ID,rootId),
 FOREIGN KEY(organizationId,assignmentId) REFERENCES mulino_work_read_ObligationReferences(organizationId,ID),
 FOREIGN KEY(organizationId,receiptId) REFERENCES mulino_trade_receipt_Receipts(organizationId,ID),
 CHECK(startQuantity>=0 AND quantity>0),
 EXCLUDE USING gist (organizationId WITH =,rootId WITH =,numrange(startQuantity,startQuantity+quantity,'[)') WITH &&)
);
CREATE FUNCTION mulino_receipt_residual_validate() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE binding mulino_responsibility_ReceiptResidualRoots%ROWTYPE; receipt mulino_trade_receipt_Receipts%ROWTYPE;
 root mulino_responsibility_Roots%ROWTYPE; line mulino_trade_purchase_OrderLines%ROWTYPE; total numeric;
BEGIN
 IF TG_TABLE_NAME='mulino_responsibility_receiptresidualroots' THEN binding:=NEW;
 ELSE SELECT * INTO binding FROM mulino_responsibility_ReceiptResidualRoots WHERE organizationId=NEW.organizationId AND rootId=NEW.rootId FOR NO KEY UPDATE; END IF;
 SELECT * INTO root FROM mulino_responsibility_Roots WHERE organizationId=binding.organizationId AND ID=binding.rootId;
 SELECT * INTO line FROM mulino_trade_purchase_OrderLines WHERE organizationId=binding.organizationId AND ID=binding.lineId;
 IF root.kind<>'RECEIPT_SHORTFALL' OR root.quantity<>binding.orderedQuantity-binding.initialContribution OR root.unit<>binding.unit OR line.quantity<>binding.orderedQuantity OR line.unit<>binding.unit OR root.scopeJson::jsonb->>'originalWorkId' IS DISTINCT FROM line.workId OR root.scopeJson::jsonb->'residual'->>'purchaseLineId' IS DISTINCT FROM binding.lineId THEN RAISE EXCEPTION 'Receipt residual root scope mismatch'; END IF;
 SELECT COALESCE(sum(contributedQuantity),0) INTO total FROM mulino_trade_purchase_ReceiptCredits WHERE organizationId=binding.organizationId AND lineId=binding.lineId;
 IF TG_TABLE_NAME='mulino_responsibility_receiptresidualroots' THEN
  SELECT * INTO receipt FROM mulino_trade_receipt_Receipts WHERE organizationId=binding.organizationId AND ID=binding.initialReceiptId;
  IF total<>binding.initialContribution THEN RAISE EXCEPTION 'Receipt residual initial contribution mismatch'; END IF;
 ELSE
  SELECT * INTO receipt FROM mulino_trade_receipt_Receipts WHERE organizationId=NEW.organizationId AND ID=NEW.receiptId;
  IF NEW.startQuantity+NEW.quantity>total-binding.initialContribution OR NOT EXISTS(SELECT 1 FROM mulino_responsibility_Scopes s JOIN mulino_work_read_ObligationReferences a ON a.organizationId=s.organizationId AND a.scopeId=s.ID WHERE s.organizationId=NEW.organizationId AND s.ID=NEW.scopeId AND s.rootId=NEW.rootId AND s.leaf AND s.startQuantity=NEW.startQuantity AND s.quantity=NEW.quantity AND a.ID=NEW.assignmentId AND a.rootId=NEW.rootId AND a.valid AND a.status='RESOLVED' AND a.evidenceId=receipt.canonicalOccurrenceId AND a.basis='VERIFIED_RECEIPT_CONTRIBUTION') THEN RAISE EXCEPTION 'Receipt credit requires exact current covered duty range'; END IF;
 END IF;
 IF receipt.purchaseLineId IS DISTINCT FROM line.ID OR receipt.workId IS DISTINCT FROM line.workId OR receipt.itemId IS DISTINCT FROM line.itemId OR receipt.placeId IS DISTINCT FROM line.destinationId OR receipt.unit IS DISTINCT FROM line.unit OR NOT EXISTS(SELECT 1 FROM mulino_trade_purchase_ReceiptCredits c WHERE c.organizationId=receipt.organizationId AND c.lineId=line.ID AND c.occurrenceId=receipt.canonicalOccurrenceId AND c.actualQuantity=receipt.quantity AND c.contributedQuantity=receipt.contributedQuantity AND c.unit=receipt.unit) THEN RAISE EXCEPTION 'Receipt contribution provenance mismatch'; END IF;
 RETURN NEW;
END $$;
CREATE CONSTRAINT TRIGGER receipt_residual_root_integrity AFTER INSERT ON mulino_responsibility_ReceiptResidualRoots DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION mulino_receipt_residual_validate();
CREATE CONSTRAINT TRIGGER receipt_residual_credit_integrity AFTER INSERT ON mulino_responsibility_ReceiptResidualCredits DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION mulino_receipt_residual_validate();
CREATE TRIGGER receipt_residual_root_immutable BEFORE UPDATE OR DELETE ON mulino_responsibility_ReceiptResidualRoots FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
CREATE TRIGGER receipt_residual_credit_immutable BEFORE UPDATE OR DELETE ON mulino_responsibility_ReceiptResidualCredits FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
CREATE TABLE mulino_responsibility_ObservationReceiptCredits (
 organizationId varchar(36) NOT NULL, ID varchar(36) NOT NULL, revision integer NOT NULL DEFAULT 0,
 createdAt timestamptz NOT NULL, recordedAt timestamptz NOT NULL,
 rootId varchar(36) NOT NULL, assignmentId varchar(36) NOT NULL, observationId varchar(36) NOT NULL, receiptId varchar(36) NOT NULL,
 PRIMARY KEY(organizationId,ID), UNIQUE(organizationId,assignmentId),
 FOREIGN KEY(organizationId,rootId) REFERENCES mulino_responsibility_Roots(organizationId,ID),
 FOREIGN KEY(organizationId,assignmentId) REFERENCES mulino_work_read_ObligationReferences(organizationId,ID),
 FOREIGN KEY(organizationId,observationId) REFERENCES mulino_trade_receipt_Observations(organizationId,ID),
 FOREIGN KEY(organizationId,receiptId) REFERENCES mulino_trade_receipt_Receipts(organizationId,ID)
);
CREATE FUNCTION mulino_receipt_observation_credit_validate() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE root mulino_responsibility_Roots%ROWTYPE; observation mulino_trade_receipt_Observations%ROWTYPE; receipt mulino_trade_receipt_Receipts%ROWTYPE;
BEGIN
 SELECT * INTO root FROM mulino_responsibility_Roots WHERE organizationId=NEW.organizationId AND ID=NEW.rootId;
 SELECT * INTO observation FROM mulino_trade_receipt_Observations WHERE organizationId=NEW.organizationId AND ID=NEW.observationId;
 SELECT * INTO receipt FROM mulino_trade_receipt_Receipts WHERE organizationId=NEW.organizationId AND ID=NEW.receiptId;
 IF root.kind<>'RECEIPT_RECONCILIATION' OR root.quantity IS NOT NULL OR root.scopeJson::jsonb->>'domainSourceId' IS DISTINCT FROM observation.ID OR root.scopeJson::jsonb->>'originalWorkId' IS DISTINCT FROM observation.workId OR root.scopeJson::jsonb->>'physicalScopeId' IS DISTINCT FROM observation.rangeRootId OR observation.state<>'CONFIRMED' OR observation.workId IS DISTINCT FROM receipt.workId OR observation.itemId IS DISTINCT FROM receipt.itemId OR observation.placeId IS DISTINCT FROM receipt.placeId OR observation.purchaseLineId IS DISTINCT FROM receipt.purchaseLineId OR observation.rangeRootId IS DISTINCT FROM receipt.rangeRootId OR observation.startQuantity IS DISTINCT FROM receipt.startQuantity OR observation.quantity IS DISTINCT FROM receipt.quantity OR observation.unit IS DISTINCT FROM receipt.unit OR (observation.lotId IS NOT NULL AND observation.lotId IS DISTINCT FROM receipt.lotId) THEN RAISE EXCEPTION 'Observation reconciliation receipt provenance mismatch'; END IF;
 IF NOT EXISTS(SELECT 1 FROM mulino_work_read_ObligationReferences a JOIN mulino_responsibility_Scopes s ON s.organizationId=a.organizationId AND s.ID=a.scopeId WHERE a.organizationId=NEW.organizationId AND a.ID=NEW.assignmentId AND a.rootId=NEW.rootId AND a.valid AND a.status='RESOLVED' AND a.evidenceId=receipt.canonicalOccurrenceId AND a.basis='VERIFIED_RECEIPT_OBSERVATION' AND s.leaf AND s.startQuantity=0 AND s.quantity=1) THEN RAISE EXCEPTION 'Observation credit requires exact current resolved duty'; END IF;
 RETURN NEW;
END $$;
CREATE CONSTRAINT TRIGGER observation_receipt_credit_integrity AFTER INSERT ON mulino_responsibility_ObservationReceiptCredits DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION mulino_receipt_observation_credit_validate();
CREATE TRIGGER observation_receipt_credit_immutable BEFORE UPDATE OR DELETE ON mulino_responsibility_ObservationReceiptCredits FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
