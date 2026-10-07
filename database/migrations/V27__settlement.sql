
CREATE TABLE mulino_trade_settlement_Invoices (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 1,
  createdAt TIMESTAMP NOT NULL,
  recordedAt TIMESTAMP NOT NULL,
  effectiveAt TIMESTAMP NOT NULL,
  workId VARCHAR(36) NOT NULL,
  itemId VARCHAR(36) NOT NULL,
  lineId VARCHAR(36) NOT NULL,
  referenceId VARCHAR(36) NOT NULL,
  scopeKind VARCHAR(16) NOT NULL,
  invoiceKind VARCHAR(24) NOT NULL,
  sourceNamespace VARCHAR(160) NOT NULL,
  sourceKey VARCHAR(160) NOT NULL,
  sourceVersion VARCHAR(80) NOT NULL,
  sourceHash VARCHAR(64) NOT NULL,
  evidenceId VARCHAR(36) NOT NULL,
  quantity DECIMAL(38, 12) NOT NULL,
  unit VARCHAR(20) NOT NULL,
  unitPrice DECIMAL(38, 12) NOT NULL,
  originalAmount DECIMAL(38, 12) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  fxPair VARCHAR(7),
  fxRate DECIMAL(38, 12),
  fxDate DATE,
  fxSource VARCHAR(160),
  fxPolicyVersion VARCHAR(80),
  fxRounding VARCHAR(40),
  convertedAmount DECIMAL(38, 12),
  relatedInvoiceId VARCHAR(36),
  taxReferenceOnly BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY(organizationId, ID)
);

CREATE TABLE mulino_trade_settlement_Matches (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 1,
  createdAt TIMESTAMP NOT NULL,
  recordedAt TIMESTAMP NOT NULL,
  effectiveAt TIMESTAMP NOT NULL,
  invoiceId VARCHAR(36) NOT NULL,
  lineId VARCHAR(36) NOT NULL,
  referenceId VARCHAR(36) NOT NULL,
  occurrenceId VARCHAR(36) NOT NULL,
  invoiceOccurrenceId VARCHAR(36) NOT NULL,
  scopeKind VARCHAR(16) NOT NULL,
  orderedQuantity DECIMAL(38, 12) NOT NULL,
  receivedQuantity DECIMAL(38, 12) NOT NULL,
  invoiceQuantity DECIMAL(38, 12) NOT NULL,
  quantity DECIMAL(38, 12) NOT NULL,
  unit VARCHAR(20) NOT NULL,
  unitPrice DECIMAL(38, 12) NOT NULL,
  orderedAmount DECIMAL(38, 12) NOT NULL,
  invoiceAmount DECIMAL(38, 12) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  invoiceCurrency VARCHAR(3) NOT NULL,
  quantityDifference DECIMAL(38, 12) NOT NULL,
  priceDifference DECIMAL(38, 12) NOT NULL,
  originalDifference DECIMAL(38, 12),
  currencyDifference BOOLEAN NOT NULL,
  status VARCHAR(24) NOT NULL,
  dutyRootId VARCHAR(36),
  PRIMARY KEY(organizationId, ID)
);

CREATE TABLE mulino_trade_settlement_Charges (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 1,
  createdAt TIMESTAMP NOT NULL,
  recordedAt TIMESTAMP NOT NULL,
  effectiveAt TIMESTAMP NOT NULL,
  invoiceId VARCHAR(36) NOT NULL,
  kind VARCHAR(40) NOT NULL,
  amount DECIMAL(38, 12) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  occurrenceId VARCHAR(36) NOT NULL,
  evidenceId VARCHAR(36) NOT NULL,
  PRIMARY KEY(organizationId, ID)
);

CREATE TABLE mulino_trade_settlement_Adjustments (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 1,
  createdAt TIMESTAMP NOT NULL,
  recordedAt TIMESTAMP NOT NULL,
  effectiveAt TIMESTAMP NOT NULL,
  invoiceId VARCHAR(36) NOT NULL,
  matchId VARCHAR(36) NOT NULL,
  mode VARCHAR(16) NOT NULL,
  amount DECIMAL(38, 12) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  evidenceId VARCHAR(36) NOT NULL,
  occurrenceId VARCHAR(36) NOT NULL,
  status VARCHAR(24) NOT NULL,
  proposalId VARCHAR(36),
  proposalHash VARCHAR(64) NOT NULL,
  decisionHash VARCHAR(64),
  approverId VARCHAR(36),
  policyHash VARCHAR(64),
  policyVersion VARCHAR(80),
  PRIMARY KEY(organizationId, ID)
);

CREATE TABLE mulino_trade_settlement_PaymentReferences (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 1,
  createdAt TIMESTAMP NOT NULL,
  recordedAt TIMESTAMP NOT NULL,
  effectiveAt TIMESTAMP NOT NULL,
  invoiceId VARCHAR(36) NOT NULL,
  amount DECIMAL(38, 12) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  externalPaymentReference VARCHAR(160) NOT NULL,
  observedAt TIMESTAMP NOT NULL,
  evidenceId VARCHAR(36) NOT NULL,
  occurrenceId VARCHAR(36) NOT NULL,
  bankEffect DECIMAL(38, 12) NOT NULL DEFAULT 0,
  PRIMARY KEY(organizationId, ID)
);


-- Reference records remain immutable; no bank write/automatic tax issuance exists.
ALTER TABLE mulino_trade_settlement_Invoices ADD CONSTRAINT settlement_source_unique UNIQUE(organizationId,sourceNamespace,sourceKey,sourceVersion);
ALTER TABLE mulino_trade_settlement_Invoices ADD CONSTRAINT settlement_invoice_kind CHECK(invoiceKind IN ('COMMERCIAL','DOMESTIC_TAX','CREDIT_NOTE','CORRECTION') AND scopeKind IN ('PURCHASE','SALE'));
ALTER TABLE mulino_trade_settlement_Invoices ADD CONSTRAINT settlement_invoice_amount CHECK(quantity>0 AND unitPrice>=0 AND originalAmount>=0 AND quantity*unitPrice=originalAmount AND currency~'^[A-Z]{3}$');
ALTER TABLE mulino_trade_settlement_Invoices ADD CONSTRAINT settlement_tax_reference CHECK(taxReferenceOnly=(invoiceKind='DOMESTIC_TAX'));
ALTER TABLE mulino_trade_settlement_Invoices ADD CONSTRAINT settlement_fx_snapshot CHECK((fxPair IS NULL AND fxRate IS NULL AND fxDate IS NULL AND fxSource IS NULL AND fxPolicyVersion IS NULL AND fxRounding IS NULL AND convertedAmount IS NULL) OR (fxPair IS NOT NULL AND fxRate>0 AND fxDate IS NOT NULL AND fxSource IS NOT NULL AND fxPolicyVersion IS NOT NULL AND fxRounding IS NOT NULL AND convertedAmount IS NOT NULL AND left(fxPair,3)=currency));
ALTER TABLE mulino_trade_settlement_Invoices ADD FOREIGN KEY(organizationId,evidenceId) REFERENCES mulino_evidence_DocumentVersions(organizationId,ID);
ALTER TABLE mulino_trade_settlement_Invoices ADD FOREIGN KEY(organizationId,relatedInvoiceId) REFERENCES mulino_trade_settlement_Invoices(organizationId,ID);
ALTER TABLE mulino_trade_settlement_Matches ADD UNIQUE(organizationId,invoiceId);
ALTER TABLE mulino_trade_settlement_Matches ADD FOREIGN KEY(organizationId,invoiceId) REFERENCES mulino_trade_settlement_Invoices(organizationId,ID);
ALTER TABLE mulino_trade_settlement_Matches ADD FOREIGN KEY(organizationId,occurrenceId) REFERENCES mulino_evidence_CanonicalOccurrences(organizationId,ID);
ALTER TABLE mulino_trade_settlement_Matches ADD FOREIGN KEY(organizationId,invoiceOccurrenceId) REFERENCES mulino_evidence_CanonicalOccurrences(organizationId,ID);
ALTER TABLE mulino_trade_settlement_Matches ADD CHECK(quantity>0 AND quantity<=receivedQuantity AND quantity<=orderedQuantity AND quantity<=invoiceQuantity AND quantityDifference=invoiceQuantity-receivedQuantity AND currencyDifference=(currency<>invoiceCurrency) AND ((currencyDifference AND originalDifference IS NULL) OR (NOT currencyDifference AND originalDifference=invoiceAmount-orderedAmount)));
ALTER TABLE mulino_trade_settlement_Charges ADD FOREIGN KEY(organizationId,invoiceId) REFERENCES mulino_trade_settlement_Invoices(organizationId,ID);
ALTER TABLE mulino_trade_settlement_Charges ADD UNIQUE(organizationId,occurrenceId);
ALTER TABLE mulino_trade_settlement_Charges ADD CHECK(amount>=0 AND currency~'^[A-Z]{3}$');
ALTER TABLE mulino_trade_settlement_Adjustments ADD FOREIGN KEY(organizationId,invoiceId) REFERENCES mulino_trade_settlement_Invoices(organizationId,ID);
ALTER TABLE mulino_trade_settlement_Adjustments ADD FOREIGN KEY(organizationId,matchId) REFERENCES mulino_trade_settlement_Matches(organizationId,ID);
ALTER TABLE mulino_trade_settlement_Adjustments ADD FOREIGN KEY(organizationId,proposalId) REFERENCES mulino_trade_settlement_Adjustments(organizationId,ID);
ALTER TABLE mulino_trade_settlement_Adjustments ADD UNIQUE(organizationId,occurrenceId);
CREATE UNIQUE INDEX settlement_confirmation_unique ON mulino_trade_settlement_Adjustments(organizationId,proposalId) WHERE status='CONFIRMED';
ALTER TABLE mulino_trade_settlement_Adjustments ADD CHECK((mode='PROPOSE' AND status='PROPOSED' AND proposalId IS NULL AND approverId IS NULL AND policyHash IS NULL) OR (mode='CONFIRM' AND status='CONFIRMED' AND proposalId IS NOT NULL AND approverId IS NOT NULL AND policyHash IS NOT NULL AND policyVersion IS NOT NULL AND decisionHash=proposalHash));
ALTER TABLE mulino_trade_settlement_PaymentReferences ADD FOREIGN KEY(organizationId,invoiceId) REFERENCES mulino_trade_settlement_Invoices(organizationId,ID);
ALTER TABLE mulino_trade_settlement_PaymentReferences ADD UNIQUE(organizationId,occurrenceId);
ALTER TABLE mulino_trade_settlement_PaymentReferences ADD UNIQUE(organizationId,externalPaymentReference);
ALTER TABLE mulino_trade_settlement_PaymentReferences ADD CHECK(bankEffect=0 AND amount>=0 AND currency~'^[A-Z]{3}$');
CREATE TRIGGER settlement_invoice_immutable BEFORE UPDATE OR DELETE ON mulino_trade_settlement_Invoices FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
CREATE TRIGGER settlement_match_immutable BEFORE UPDATE OR DELETE ON mulino_trade_settlement_Matches FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
CREATE TRIGGER settlement_charge_immutable BEFORE UPDATE OR DELETE ON mulino_trade_settlement_Charges FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
CREATE TRIGGER settlement_adjustment_immutable BEFORE UPDATE OR DELETE ON mulino_trade_settlement_Adjustments FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
CREATE TRIGGER settlement_payment_immutable BEFORE UPDATE OR DELETE ON mulino_trade_settlement_PaymentReferences FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
