-- Purchase plans and immutable commercial decisions never create physical stock.
CREATE TABLE mulino_trade_purchase_Suppliers (organizationId varchar(36) NOT NULL,ID varchar(36) NOT NULL,name varchar(240) NOT NULL,PRIMARY KEY(organizationId,ID));
CREATE TABLE mulino_trade_purchase_Proposals (organizationId varchar(36) NOT NULL,ID varchar(36) NOT NULL,workId varchar(36) NOT NULL,currentRevision integer NOT NULL CHECK(currentRevision>0),status varchar(40) NOT NULL,createdAt timestamptz NOT NULL,PRIMARY KEY(organizationId,ID));
CREATE TABLE mulino_trade_purchase_ProposalRevisions (organizationId varchar(36) NOT NULL,ID varchar(36) NOT NULL,proposalId varchar(36) NOT NULL,revision integer NOT NULL CHECK(revision>0),workId varchar(36) NOT NULL,goalVersionId varchar(36),itemId varchar(36) NOT NULL,supplierId varchar(36) NOT NULL,destinationId varchar(36) NOT NULL,quantity numeric(38,12) NOT NULL CHECK(quantity>0),unit varchar(20) NOT NULL,price numeric(38,12) NOT NULL CHECK(price>=0),currency varchar(3) NOT NULL,dueAt timestamptz NOT NULL,endpoint varchar(40) NOT NULL,quantityMode varchar(40) NOT NULL,proposalHash varchar(64) NOT NULL,reason varchar(500),recordedAt timestamptz NOT NULL,PRIMARY KEY(organizationId,ID));
CREATE TABLE mulino_trade_purchase_Approvals (organizationId varchar(36) NOT NULL,ID varchar(36) NOT NULL,proposalId varchar(36) NOT NULL,proposalRevision integer NOT NULL,proposalHash varchar(64) NOT NULL,approverId varchar(36) NOT NULL,policyHash varchar(64) NOT NULL,decision varchar(24) NOT NULL,conditionsJson text NOT NULL,decidedAt timestamptz NOT NULL,validUntil timestamptz NOT NULL CHECK(validUntil>decidedAt),PRIMARY KEY(organizationId,ID));
CREATE TABLE mulino_trade_purchase_Orders (organizationId varchar(36) NOT NULL,ID varchar(36) NOT NULL,proposalId varchar(36) NOT NULL,proposalRevision integer NOT NULL,proposalHash varchar(64) NOT NULL,approvalId varchar(36) NOT NULL,channel varchar(80) NOT NULL,externalOperationId varchar(160) NOT NULL,outboxId varchar(36) NOT NULL,createdAt timestamptz NOT NULL,PRIMARY KEY(organizationId,ID));
CREATE TABLE mulino_trade_purchase_OrderLines (organizationId varchar(36) NOT NULL,ID varchar(36) NOT NULL,orderId varchar(36) NOT NULL,proposalId varchar(36) NOT NULL,proposalRevision integer NOT NULL,workId varchar(36) NOT NULL,itemId varchar(36) NOT NULL,destinationId varchar(36) NOT NULL,quantity numeric(38,12) NOT NULL CHECK(quantity>0),unit varchar(20) NOT NULL,status varchar(40) NOT NULL,revision integer NOT NULL CHECK(revision>0),PRIMARY KEY(organizationId,ID));
CREATE TABLE mulino_trade_purchase_SupplierReplies (organizationId varchar(36) NOT NULL,ID varchar(36) NOT NULL,orderId varchar(36) NOT NULL,reply varchar(24) NOT NULL,proposedQuantity numeric(38,12) NOT NULL CHECK(proposedQuantity>=0),unit varchar(20) NOT NULL,evidenceId varchar(36) NOT NULL,recordedAt timestamptz NOT NULL,PRIMARY KEY(organizationId,ID));
CREATE TABLE mulino_trade_purchase_ReceiptCredits (organizationId varchar(36) NOT NULL,ID varchar(36) NOT NULL,lineId varchar(36) NOT NULL,occurrenceId varchar(36) NOT NULL,actualQuantity numeric(38,12) NOT NULL CHECK(actualQuantity>0),contributedQuantity numeric(38,12) NOT NULL CHECK(contributedQuantity>=0),excessQuantity numeric(38,12) NOT NULL CHECK(excessQuantity>=0),unit varchar(20) NOT NULL,recordedAt timestamptz NOT NULL,CHECK(actualQuantity=contributedQuantity+excessQuantity),PRIMARY KEY(organizationId,ID));
CREATE TABLE mulino_trade_purchase_ExecutionEffects (organizationId varchar(36) NOT NULL,ID varchar(36) NOT NULL,lineId varchar(36) NOT NULL,kind varchar(24) NOT NULL CHECK(kind IN ('SHIPPED','RECEIVED','INVOICED')),referenceId varchar(36) NOT NULL,quantity numeric(38,12) NOT NULL CHECK(quantity>0),unit varchar(20) NOT NULL,recordedAt timestamptz NOT NULL,PRIMARY KEY(organizationId,ID));
CREATE TABLE mulino_trade_purchase_Cancellations (organizationId varchar(36) NOT NULL,ID varchar(36) NOT NULL,orderId varchar(36) NOT NULL,lineId varchar(36) NOT NULL,quantity numeric(38,12) NOT NULL CHECK(quantity>0),unit varchar(20) NOT NULL,reason varchar(500) NOT NULL,externalAcceptance varchar(24) NOT NULL,outboxId varchar(36) NOT NULL,recordedAt timestamptz NOT NULL,PRIMARY KEY(organizationId,ID));
CREATE TRIGGER purchase_proposalrevisions_immutable BEFORE UPDATE OR DELETE ON mulino_trade_purchase_ProposalRevisions FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
CREATE TRIGGER purchase_approvals_immutable BEFORE UPDATE OR DELETE ON mulino_trade_purchase_Approvals FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
CREATE TRIGGER purchase_orders_immutable BEFORE UPDATE OR DELETE ON mulino_trade_purchase_Orders FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
CREATE TRIGGER purchase_supplierreplies_immutable BEFORE UPDATE OR DELETE ON mulino_trade_purchase_SupplierReplies FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
CREATE TRIGGER purchase_receiptcredits_immutable BEFORE UPDATE OR DELETE ON mulino_trade_purchase_ReceiptCredits FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
CREATE TRIGGER purchase_executioneffects_immutable BEFORE UPDATE OR DELETE ON mulino_trade_purchase_ExecutionEffects FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
CREATE TRIGGER purchase_cancellations_immutable BEFORE UPDATE OR DELETE ON mulino_trade_purchase_Cancellations FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
ALTER TABLE mulino_trade_purchase_Proposals ADD FOREIGN KEY(organizationId,workId) REFERENCES mulino_work_read_Works(organizationId,ID);
ALTER TABLE mulino_trade_purchase_ProposalRevisions ADD FOREIGN KEY(organizationId,proposalId) REFERENCES mulino_trade_purchase_Proposals(organizationId,ID);
ALTER TABLE mulino_trade_purchase_ProposalRevisions ADD FOREIGN KEY(organizationId,itemId) REFERENCES mulino_inventory_TradeItems(organizationId,ID);
ALTER TABLE mulino_trade_purchase_ProposalRevisions ADD FOREIGN KEY(organizationId,supplierId) REFERENCES mulino_trade_purchase_Suppliers(organizationId,ID);
ALTER TABLE mulino_trade_purchase_ProposalRevisions ADD FOREIGN KEY(organizationId,destinationId) REFERENCES mulino_inventory_Places(organizationId,ID);
ALTER TABLE mulino_trade_purchase_Approvals ADD FOREIGN KEY(organizationId,proposalId) REFERENCES mulino_trade_purchase_Proposals(organizationId,ID);
ALTER TABLE mulino_trade_purchase_Approvals ADD FOREIGN KEY(organizationId,approverId) REFERENCES mulino_identity_Actors(organizationId,ID);
ALTER TABLE mulino_trade_purchase_Orders ADD FOREIGN KEY(organizationId,approvalId) REFERENCES mulino_trade_purchase_Approvals(organizationId,ID);
ALTER TABLE mulino_trade_purchase_OrderLines ADD FOREIGN KEY(organizationId,orderId) REFERENCES mulino_trade_purchase_Orders(organizationId,ID);
ALTER TABLE mulino_trade_purchase_SupplierReplies ADD FOREIGN KEY(organizationId,orderId) REFERENCES mulino_trade_purchase_Orders(organizationId,ID);
ALTER TABLE mulino_trade_purchase_SupplierReplies ADD FOREIGN KEY(organizationId,evidenceId) REFERENCES mulino_evidence_DocumentVersions(organizationId,ID);
ALTER TABLE mulino_trade_purchase_ReceiptCredits ADD FOREIGN KEY(organizationId,lineId) REFERENCES mulino_trade_purchase_OrderLines(organizationId,ID);
ALTER TABLE mulino_trade_purchase_ReceiptCredits ADD FOREIGN KEY(organizationId,occurrenceId) REFERENCES mulino_evidence_CanonicalOccurrences(organizationId,ID);
ALTER TABLE mulino_trade_purchase_ExecutionEffects ADD FOREIGN KEY(organizationId,lineId) REFERENCES mulino_trade_purchase_OrderLines(organizationId,ID);
ALTER TABLE mulino_trade_purchase_Cancellations ADD FOREIGN KEY(organizationId,lineId) REFERENCES mulino_trade_purchase_OrderLines(organizationId,ID);
CREATE UNIQUE INDEX ON mulino_trade_purchase_ProposalRevisions(organizationId,proposalId,revision);
CREATE UNIQUE INDEX ON mulino_trade_purchase_Orders(organizationId,approvalId);
CREATE UNIQUE INDEX ON mulino_trade_purchase_Orders(organizationId,proposalId,proposalRevision);
CREATE UNIQUE INDEX ON mulino_trade_purchase_ReceiptCredits(organizationId,lineId,occurrenceId);
CREATE UNIQUE INDEX ON mulino_trade_purchase_ExecutionEffects(organizationId,lineId,kind,referenceId);

ALTER TABLE mulino_trade_purchase_Suppliers ADD revision integer NOT NULL DEFAULT 1;
ALTER TABLE mulino_trade_purchase_Suppliers ADD createdAt timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE mulino_trade_purchase_Suppliers ADD recordedAt timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE mulino_trade_purchase_Suppliers ADD effectiveAt timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE mulino_trade_purchase_Proposals ADD revision integer NOT NULL DEFAULT 1;
ALTER TABLE mulino_trade_purchase_Proposals ADD recordedAt timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE mulino_trade_purchase_Proposals ADD effectiveAt timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE mulino_trade_purchase_ProposalRevisions ADD createdAt timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE mulino_trade_purchase_ProposalRevisions ADD effectiveAt timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE mulino_trade_purchase_Approvals ADD revision integer NOT NULL DEFAULT 1;
ALTER TABLE mulino_trade_purchase_Approvals ADD createdAt timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE mulino_trade_purchase_Approvals ADD recordedAt timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE mulino_trade_purchase_Approvals ADD effectiveAt timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE mulino_trade_purchase_Orders ADD revision integer NOT NULL DEFAULT 1;
ALTER TABLE mulino_trade_purchase_Orders ADD recordedAt timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE mulino_trade_purchase_Orders ADD effectiveAt timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE mulino_trade_purchase_OrderLines ADD createdAt timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE mulino_trade_purchase_OrderLines ADD recordedAt timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE mulino_trade_purchase_OrderLines ADD effectiveAt timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE mulino_trade_purchase_SupplierReplies ADD revision integer NOT NULL DEFAULT 1;
ALTER TABLE mulino_trade_purchase_SupplierReplies ADD createdAt timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE mulino_trade_purchase_SupplierReplies ADD effectiveAt timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE mulino_trade_purchase_ReceiptCredits ADD revision integer NOT NULL DEFAULT 1;
ALTER TABLE mulino_trade_purchase_ReceiptCredits ADD createdAt timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE mulino_trade_purchase_ReceiptCredits ADD effectiveAt timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE mulino_trade_purchase_ExecutionEffects ADD revision integer NOT NULL DEFAULT 1;
ALTER TABLE mulino_trade_purchase_ExecutionEffects ADD createdAt timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE mulino_trade_purchase_ExecutionEffects ADD effectiveAt timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE mulino_trade_purchase_Cancellations ADD revision integer NOT NULL DEFAULT 1;
ALTER TABLE mulino_trade_purchase_Cancellations ADD createdAt timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE mulino_trade_purchase_Cancellations ADD effectiveAt timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE mulino_trade_purchase_ProposalRevisions ADD FOREIGN KEY(organizationId,workId) REFERENCES mulino_work_read_Works(organizationId,ID);
ALTER TABLE mulino_trade_purchase_ProposalRevisions ADD FOREIGN KEY(organizationId,goalVersionId) REFERENCES mulino_work_read_GoalReferences(organizationId,ID);
ALTER TABLE mulino_trade_purchase_Orders ADD FOREIGN KEY(organizationId,proposalId) REFERENCES mulino_trade_purchase_Proposals(organizationId,ID);
ALTER TABLE mulino_trade_purchase_OrderLines ADD FOREIGN KEY(organizationId,proposalId) REFERENCES mulino_trade_purchase_Proposals(organizationId,ID);
ALTER TABLE mulino_trade_purchase_OrderLines ADD FOREIGN KEY(organizationId,workId) REFERENCES mulino_work_read_Works(organizationId,ID);
ALTER TABLE mulino_trade_purchase_OrderLines ADD FOREIGN KEY(organizationId,itemId) REFERENCES mulino_inventory_TradeItems(organizationId,ID);
ALTER TABLE mulino_trade_purchase_OrderLines ADD FOREIGN KEY(organizationId,destinationId) REFERENCES mulino_inventory_Places(organizationId,ID);
ALTER TABLE mulino_trade_purchase_Cancellations ADD FOREIGN KEY(organizationId,orderId) REFERENCES mulino_trade_purchase_Orders(organizationId,ID);

ALTER TABLE mulino_trade_purchase_Orders ADD conditionAssessmentJson text NOT NULL DEFAULT '{}';

ALTER TABLE mulino_trade_purchase_SupplierReplies ADD canonicalOccurrenceId varchar(36) NOT NULL;
ALTER TABLE mulino_trade_purchase_SupplierReplies ADD FOREIGN KEY(organizationId,canonicalOccurrenceId) REFERENCES mulino_evidence_CanonicalOccurrences(organizationId,ID);
CREATE UNIQUE INDEX ON mulino_trade_purchase_SupplierReplies(organizationId,canonicalOccurrenceId);
CREATE FUNCTION mulino_purchase_line_immutable() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF (NEW.organizationId,NEW.ID,NEW.orderId,NEW.proposalId,NEW.proposalRevision,NEW.workId,NEW.itemId,NEW.destinationId,NEW.quantity,NEW.unit)
 IS DISTINCT FROM (OLD.organizationId,OLD.ID,OLD.orderId,OLD.proposalId,OLD.proposalRevision,OLD.workId,OLD.itemId,OLD.destinationId,OLD.quantity,OLD.unit)
 THEN RAISE EXCEPTION 'Immutable purchase order line snapshot'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER purchase_line_snapshot_immutable BEFORE UPDATE ON mulino_trade_purchase_OrderLines
 FOR EACH ROW EXECUTE FUNCTION mulino_purchase_line_immutable();
