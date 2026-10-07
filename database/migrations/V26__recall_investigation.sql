-- Exact versioned recall decisions; recovery and final disposition remain separate.
CREATE TABLE mulino_trade_recall_Investigations (
 organizationId varchar(36) NOT NULL,
 ID varchar(36) NOT NULL,
 revision integer,
 createdAt timestamptz,
 recordedAt timestamptz,
 occurredAt timestamptz,
 itemId varchar(36),
 lotId varchar(36),
 segmentId varchar(36),
 workId varchar(36),
 quantity numeric(38,12),
 unit varchar(40),
 startQuantity numeric(38,12),
 basisEvidenceId varchar(36),
 impactState varchar(40),
 status varchar(40),
 currentScopeId varchar(36),
 ownerId varchar(36),
 supervisorId varchar(36),
 nextAction varchar(320),
 nextCheckAt timestamptz,
 closedAt timestamptz,
 PRIMARY KEY(organizationId,ID)
);
CREATE TABLE mulino_trade_recall_Scopes (
 organizationId varchar(36) NOT NULL,
 ID varchar(36) NOT NULL,
 revision integer,
 createdAt timestamptz,
 recordedAt timestamptz,
 investigationId varchar(36),
 version integer,
 scopeHash varchar(64),
 rootSegmentId varchar(36),
 startQuantity numeric(38,12),
 quantity numeric(38,12),
 unit varchar(40),
 itemId varchar(36),
 lotId varchar(36),
 workId varchar(36),
 reason varchar(320),
 PRIMARY KEY(organizationId,ID),
 UNIQUE(organizationId,investigationId,version),
 CHECK(startQuantity>=0 AND quantity>0),
 FOREIGN KEY(organizationId,investigationId) REFERENCES mulino_trade_recall_Investigations(organizationId,ID)
);
CREATE TRIGGER recall_scopes_immutable BEFORE UPDATE OR DELETE ON mulino_trade_recall_Scopes FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
CREATE TABLE mulino_trade_recall_Approvals (
 organizationId varchar(36) NOT NULL,
 ID varchar(36) NOT NULL,
 revision integer,
 createdAt timestamptz,
 recordedAt timestamptz,
 scopeId varchar(36),
 scopeVersion integer,
 scopeHash varchar(64),
 approverId varchar(36),
 decision varchar(40),
 validUntil timestamptz,
 policyHash varchar(64),
 PRIMARY KEY(organizationId,ID),
 FOREIGN KEY(organizationId,scopeId) REFERENCES mulino_trade_recall_Scopes(organizationId,ID),
 CHECK(decision IN ('APPROVE','REJECT'))
);
CREATE TRIGGER recall_approvals_immutable BEFORE UPDATE OR DELETE ON mulino_trade_recall_Approvals FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
CREATE TABLE mulino_trade_recall_Actions (
 organizationId varchar(36) NOT NULL,
 ID varchar(36) NOT NULL,
 revision integer,
 createdAt timestamptz,
 recordedAt timestamptz,
 occurredAt timestamptz,
 scopeId varchar(36),
 approvalId varchar(36),
 canonicalOccurrenceId varchar(36),
 actualEventId varchar(36),
 kind varchar(40),
 rootSegmentId varchar(36),
 startQuantity numeric(38,12),
 quantity numeric(38,12),
 unit varchar(40),
 sourceSegmentId varchar(36),
 currentSegmentId varchar(36),
 currentPlaceId varchar(36),
 returnId varchar(36),
 reason varchar(320),
 residualDutyId varchar(36),
 PRIMARY KEY(organizationId,ID),
 UNIQUE(organizationId,actualEventId),
 UNIQUE(organizationId,canonicalOccurrenceId),
 CHECK(startQuantity>=0 AND quantity>0),
 CHECK(kind IN ('NOTICE','RECOVERED','DISPOSED','SAFE','CONSUMED_LOST','EXCEPTION')),
 FOREIGN KEY(organizationId,scopeId) REFERENCES mulino_trade_recall_Scopes(organizationId,ID),
 FOREIGN KEY(organizationId,approvalId) REFERENCES mulino_trade_recall_Approvals(organizationId,ID),
 FOREIGN KEY(organizationId,canonicalOccurrenceId) REFERENCES mulino_evidence_CanonicalOccurrences(organizationId,ID)
);
CREATE TRIGGER recall_actions_immutable BEFORE UPDATE OR DELETE ON mulino_trade_recall_Actions FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
CREATE TABLE mulino_trade_recall_Closures (
 organizationId varchar(36) NOT NULL,
 ID varchar(36) NOT NULL,
 revision integer,
 createdAt timestamptz,
 recordedAt timestamptz,
 investigationId varchar(36),
 scopeId varchar(36),
 approvalId varchar(36),
 scopeHash varchar(64),
 partitionJson text,
 evidenceId varchar(36),
 closedBy varchar(36),
 PRIMARY KEY(organizationId,ID),
 UNIQUE(organizationId,investigationId),
 FOREIGN KEY(organizationId,scopeId) REFERENCES mulino_trade_recall_Scopes(organizationId,ID)
);
CREATE TRIGGER recall_closures_immutable BEFORE UPDATE OR DELETE ON mulino_trade_recall_Closures FOR EACH ROW EXECUTE FUNCTION mulino_evidence_reject_mutation();
ALTER TABLE mulino_trade_recall_Actions ADD CONSTRAINT recall_terminal_disjoint EXCLUDE USING gist (organizationId WITH =, scopeId WITH =, numrange(startQuantity,startQuantity+quantity,'[)') WITH &&) WHERE (kind IN ('DISPOSED','SAFE','CONSUMED_LOST','EXCEPTION'));
ALTER TABLE mulino_trade_recall_Actions ADD CONSTRAINT recall_recovery_disjoint EXCLUDE USING gist (organizationId WITH =, rootSegmentId WITH =, numrange(startQuantity,startQuantity+quantity,'[)') WITH &&) WHERE (kind='RECOVERED');
CREATE FUNCTION mulino_recall_action_bounds() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE s mulino_trade_recall_Scopes%ROWTYPE; a mulino_trade_recall_Approvals%ROWTYPE;
BEGIN
 SELECT * INTO s FROM mulino_trade_recall_Scopes WHERE organizationId=NEW.organizationId AND ID=NEW.scopeId;
 SELECT * INTO a FROM mulino_trade_recall_Approvals WHERE organizationId=NEW.organizationId AND ID=NEW.approvalId;
 IF NEW.rootSegmentId<>s.rootSegmentId OR NEW.unit<>s.unit OR NEW.startQuantity<s.startQuantity OR NEW.startQuantity+NEW.quantity>s.startQuantity+s.quantity OR a.scopeId<>s.ID OR a.scopeVersion<>s.version OR a.scopeHash<>s.scopeHash OR a.decision<>'APPROVE' THEN RAISE EXCEPTION 'Recall action must match exact approved scope'; END IF;
 IF NEW.kind='EXCEPTION' AND (NEW.reason IS NULL OR NEW.residualDutyId IS NULL) THEN RAISE EXCEPTION 'Recall exception needs reason and retained responsibility'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER recall_action_exact_scope BEFORE INSERT ON mulino_trade_recall_Actions FOR EACH ROW EXECUTE FUNCTION mulino_recall_action_bounds();
