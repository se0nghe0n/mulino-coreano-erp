-- Canonical assignments extend the existing read entity; there is no shadow table.
DROP TRIGGER obligation_read_immutable ON mulino_work_read_ObligationReferences;
CREATE TABLE mulino_responsibility_Roots (
 organizationId varchar(36) NOT NULL, ID varchar(36) NOT NULL, revision integer NOT NULL DEFAULT 0,
 createdAt timestamptz NOT NULL, recordedAt timestamptz NOT NULL, dutyKey varchar(64) NOT NULL,
 sourceId varchar(36) NOT NULL, sourceKind varchar(20) NOT NULL, sourceVersion varchar(80) NOT NULL,
 kind varchar(80) NOT NULL, scopeJson text NOT NULL, quantity numeric(38,12), unit varchar(20),
 PRIMARY KEY(organizationId,ID), UNIQUE(organizationId,dutyKey),
 FOREIGN KEY(organizationId) REFERENCES mulino_identity_Organizations(ID),
 CHECK(sourceKind IN ('OCCURRENCE','DECISION','EVENT','CLAIM','DOCUMENT')), CHECK(jsonb_typeof(scopeJson::jsonb)='object'),
 CHECK(quantity IS NULL OR (quantity>0 AND unit IS NOT NULL))
);
CREATE TABLE mulino_responsibility_Scopes (
 organizationId varchar(36) NOT NULL, ID varchar(36) NOT NULL, revision integer NOT NULL DEFAULT 0,
 createdAt timestamptz NOT NULL, recordedAt timestamptz NOT NULL, rootId varchar(36) NOT NULL,
 parentScopeId varchar(36), startQuantity numeric(38,12) NOT NULL, quantity numeric(38,12) NOT NULL,
 unit varchar(20), leaf boolean NOT NULL DEFAULT true, scopeJson text NOT NULL,
 PRIMARY KEY(organizationId,ID), UNIQUE(organizationId,ID,rootId),
 FOREIGN KEY(organizationId,rootId) REFERENCES mulino_responsibility_Roots(organizationId,ID),
 FOREIGN KEY(organizationId,parentScopeId,rootId) REFERENCES mulino_responsibility_Scopes(organizationId,ID,rootId),
 CHECK(startQuantity>=0 AND quantity>0),CHECK(jsonb_typeof(scopeJson::jsonb)='object')
);
ALTER TABLE mulino_work_read_ObligationReferences ADD rootId varchar(36), ADD scopeId varchar(36),
 ADD valid boolean DEFAULT true, ADD evidenceId varchar(36), ADD basis varchar(500), ADD predecessorId varchar(36),
 ADD FOREIGN KEY(organizationId,rootId) REFERENCES mulino_responsibility_Roots(organizationId,ID),
 ADD FOREIGN KEY(organizationId,scopeId,rootId) REFERENCES mulino_responsibility_Scopes(organizationId,ID,rootId),
 ADD FOREIGN KEY(organizationId,predecessorId) REFERENCES mulino_work_read_ObligationReferences(organizationId,ID),
 ADD CHECK ((rootId IS NULL AND scopeId IS NULL) OR (rootId IS NOT NULL AND scopeId IS NOT NULL)),
 ADD CHECK(status NOT IN ('RESOLVED','WAIVED') OR rootId IS NULL OR (evidenceId IS NOT NULL AND basis IS NOT NULL));
CREATE UNIQUE INDEX responsibility_one_open_assignment ON mulino_work_read_ObligationReferences(organizationId,scopeId) WHERE status='OPEN' AND valid AND rootId IS NOT NULL;
CREATE TABLE mulino_responsibility_Handovers (
 organizationId varchar(36) NOT NULL, ID varchar(36) NOT NULL, revision integer NOT NULL DEFAULT 0,
 createdAt timestamptz NOT NULL, recordedAt timestamptz NOT NULL, kind varchar(20) NOT NULL,
 workId varchar(36) NOT NULL, assignmentId varchar(36), targetWorkId varchar(36),
 previousOwnerId varchar(36) NOT NULL, recipientId varchar(36) NOT NULL, status varchar(20) NOT NULL,
 expiresAt timestamptz NOT NULL, nextAction varchar(500) NOT NULL, nextCheckAt timestamptz NOT NULL,
 quantity numeric(38,12), reason varchar(500), PRIMARY KEY(organizationId,ID),
 FOREIGN KEY(organizationId,workId) REFERENCES mulino_work_read_Works(organizationId,ID),
 FOREIGN KEY(organizationId,targetWorkId) REFERENCES mulino_work_read_Works(organizationId,ID),
 FOREIGN KEY(organizationId,assignmentId) REFERENCES mulino_work_read_ObligationReferences(organizationId,ID),
 FOREIGN KEY(organizationId,previousOwnerId) REFERENCES mulino_identity_Actors(organizationId,ID),
 FOREIGN KEY(organizationId,recipientId) REFERENCES mulino_identity_Actors(organizationId,ID),
 CHECK(kind IN ('OWNER','TRANSFER')),CHECK(status IN ('PROPOSED','ACCEPTED','REJECTED','EXPIRED')),
 CHECK(quantity IS NULL OR quantity>0),CHECK(length(trim(nextAction))>0)
);
CREATE FUNCTION mulino_responsibility_validate() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE org varchar(36); rid varchar(36); rootq numeric; leafq numeric;
BEGIN
 org:=NEW.organizationId; IF TG_TABLE_NAME='mulino_responsibility_roots' THEN rid:=NEW.ID; ELSE rid:=NEW.rootId; END IF;
 IF rid IS NULL THEN RETURN NEW; END IF;
 SELECT COALESCE(quantity,1) INTO rootq FROM mulino_responsibility_Roots WHERE organizationId=org AND ID=rid;
 SELECT SUM(quantity) INTO leafq FROM mulino_responsibility_Scopes WHERE organizationId=org AND rootId=rid AND leaf;
 IF leafq IS DISTINCT FROM rootq THEN RAISE EXCEPTION 'Duty root quantity conservation'; END IF;
 IF EXISTS(SELECT 1 FROM mulino_responsibility_Scopes s WHERE s.organizationId=org AND s.rootId=rid AND (s.startQuantity+s.quantity>rootq OR (s.parentScopeId IS NOT NULL AND NOT EXISTS(SELECT 1 FROM mulino_responsibility_Scopes p WHERE p.organizationId=org AND p.ID=s.parentScopeId AND p.rootId=rid AND NOT p.leaf AND s.startQuantity>=p.startQuantity AND s.startQuantity+s.quantity<=p.startQuantity+p.quantity)))) THEN RAISE EXCEPTION 'Duty scope outside parent'; END IF;
 IF EXISTS(SELECT 1 FROM mulino_work_read_ObligationReferences a JOIN mulino_responsibility_Scopes s ON s.organizationId=a.organizationId AND s.ID=a.scopeId JOIN mulino_responsibility_Roots r ON r.organizationId=a.organizationId AND r.ID=a.rootId WHERE a.organizationId=org AND a.rootId=rid AND a.valid AND a.status IN ('OPEN','RESOLVED','WAIVED') AND (NOT s.leaf OR (r.quantity IS NOT NULL AND (a.quantity IS DISTINCT FROM s.quantity OR a.unit IS DISTINCT FROM r.unit)))) THEN RAISE EXCEPTION 'Current duty assignment must match leaf quantity'; END IF;
 IF EXISTS(SELECT 1 FROM mulino_responsibility_Scopes a JOIN mulino_responsibility_Scopes b ON a.organizationId=b.organizationId AND a.rootId=b.rootId AND a.ID<b.ID WHERE a.organizationId=org AND a.rootId=rid AND a.leaf AND b.leaf AND a.startQuantity<b.startQuantity+b.quantity AND b.startQuantity<a.startQuantity+a.quantity) THEN RAISE EXCEPTION 'Overlapping duty leaf scopes'; END IF;
 IF EXISTS(SELECT 1 FROM mulino_responsibility_Scopes s WHERE s.organizationId=org AND s.rootId=rid AND s.leaf AND (SELECT COUNT(*) FROM mulino_work_read_ObligationReferences a WHERE a.organizationId=org AND a.scopeId=s.ID AND a.valid AND a.status IN ('OPEN','RESOLVED','WAIVED'))<>1) THEN RAISE EXCEPTION 'Duty leaf requires one current assignment'; END IF;
 RETURN NEW;
END $$;
CREATE CONSTRAINT TRIGGER responsibility_root_integrity AFTER INSERT OR UPDATE ON mulino_responsibility_Roots DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION mulino_responsibility_validate();
CREATE CONSTRAINT TRIGGER responsibility_scope_integrity AFTER INSERT OR UPDATE ON mulino_responsibility_Scopes DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION mulino_responsibility_validate();
CREATE CONSTRAINT TRIGGER responsibility_assignment_integrity AFTER INSERT OR UPDATE ON mulino_work_read_ObligationReferences DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION mulino_responsibility_validate();
CREATE TRIGGER responsibility_assignment_human BEFORE UPDATE ON mulino_work_read_ObligationReferences FOR EACH ROW EXECUTE FUNCTION mulino_work_read_human_responsibility();
-- Responsibility history cannot disappear through a direct row deletion.
CREATE FUNCTION mulino_responsibility_no_delete() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Responsibility history cannot be deleted'; END $$;
CREATE TRIGGER responsibility_root_no_delete BEFORE DELETE ON mulino_responsibility_Roots FOR EACH ROW EXECUTE FUNCTION mulino_responsibility_no_delete();
CREATE TRIGGER responsibility_scope_no_delete BEFORE DELETE ON mulino_responsibility_Scopes FOR EACH ROW EXECUTE FUNCTION mulino_responsibility_no_delete();
CREATE TRIGGER responsibility_assignment_no_delete BEFORE DELETE ON mulino_work_read_ObligationReferences FOR EACH ROW WHEN (OLD.rootId IS NOT NULL) EXECUTE FUNCTION mulino_responsibility_no_delete();
CREATE TRIGGER responsibility_handover_no_delete BEFORE DELETE ON mulino_responsibility_Handovers FOR EACH ROW EXECUTE FUNCTION mulino_responsibility_no_delete();

CREATE TABLE mulino_responsibility_CompletionBindings (
 organizationId varchar(36) NOT NULL, ID varchar(36) NOT NULL,
 revision integer NOT NULL DEFAULT 0, createdAt timestamptz NOT NULL,
 recordedAt timestamptz NOT NULL, occurrenceId varchar(36) NOT NULL,
 rootId varchar(36) NOT NULL, startQuantity numeric(38,12) NOT NULL,
 quantity numeric(38,12) NOT NULL, unit varchar(20),
 verificationId varchar(36) NOT NULL, coverageId varchar(36) NOT NULL,
 PRIMARY KEY(organizationId,ID), UNIQUE(organizationId,occurrenceId),
 UNIQUE(organizationId,ID,rootId),
 FOREIGN KEY(organizationId,rootId) REFERENCES mulino_responsibility_Roots(organizationId,ID),
 FOREIGN KEY(organizationId,occurrenceId) REFERENCES mulino_evidence_CanonicalOccurrences(organizationId,ID),
 FOREIGN KEY(organizationId,verificationId) REFERENCES mulino_evidence_Verifications(organizationId,ID),
 CHECK(startQuantity>=0 AND quantity>0)
);
CREATE TABLE mulino_responsibility_ResolutionCredits (
 organizationId varchar(36) NOT NULL, ID varchar(36) NOT NULL,
 revision integer NOT NULL DEFAULT 0, createdAt timestamptz NOT NULL,
 recordedAt timestamptz NOT NULL, bindingId varchar(36) NOT NULL,
 rootId varchar(36) NOT NULL, scopeId varchar(36) NOT NULL,
 assignmentId varchar(36) NOT NULL, startQuantity numeric(38,12) NOT NULL,
 quantity numeric(38,12) NOT NULL, PRIMARY KEY(organizationId,ID),
 UNIQUE(organizationId,assignmentId),UNIQUE(organizationId,bindingId,scopeId),
 FOREIGN KEY(organizationId,bindingId,rootId) REFERENCES mulino_responsibility_CompletionBindings(organizationId,ID,rootId),
 FOREIGN KEY(organizationId,scopeId,rootId) REFERENCES mulino_responsibility_Scopes(organizationId,ID,rootId),
 FOREIGN KEY(organizationId,assignmentId) REFERENCES mulino_work_read_ObligationReferences(organizationId,ID),
 CHECK(startQuantity>=0 AND quantity>0),
 EXCLUDE USING gist (organizationId WITH =,bindingId WITH =,numrange(startQuantity,startQuantity+quantity,'[)') WITH &&)
);
-- NO KEY UPDATE serializes credit validation while remaining compatible with the
-- binding foreign-key KEY SHARE locks held by concurrent credit insertions.
-- The following SUM is a separate volatile trigger query at READ COMMITTED.
CREATE FUNCTION mulino_responsibility_credit_validate() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE binding mulino_responsibility_CompletionBindings%ROWTYPE; scope mulino_responsibility_Scopes%ROWTYPE; root mulino_responsibility_Roots%ROWTYPE; used numeric;
BEGIN
 IF TG_TABLE_NAME='mulino_responsibility_completionbindings' THEN binding:=NEW; ELSE SELECT * INTO binding FROM mulino_responsibility_CompletionBindings WHERE organizationId=NEW.organizationId AND ID=NEW.bindingId FOR NO KEY UPDATE; END IF;
 SELECT * INTO root FROM mulino_responsibility_Roots WHERE organizationId=binding.organizationId AND ID=binding.rootId;
 IF binding.startQuantity+binding.quantity>COALESCE(root.quantity,1) OR binding.unit IS DISTINCT FROM root.unit THEN RAISE EXCEPTION 'Completion evidence outside duty root'; END IF;
 IF NOT EXISTS(SELECT 1 FROM mulino_evidence_Verifications v JOIN mulino_evidence_CanonicalOccurrences o ON o.organizationId=v.organizationId AND o.ID=v.canonicalOccurrenceId WHERE v.organizationId=binding.organizationId AND v.ID=binding.verificationId AND o.ID=binding.occurrenceId AND v.verdict='VERIFIED' AND v.sourceMatched AND v.identityMatched AND v.quantityMatched AND v.timeMatched AND v.duplicateChecked AND v.basisDocumentId IS NOT NULL AND o.kind='RESPONSE_COMPLETED' AND o.valueState='KNOWN' AND o.reassessmentState='COMPLETE' AND COALESCE(o.quantity,1)=binding.quantity AND o.unit IS NOT DISTINCT FROM binding.unit) THEN RAISE EXCEPTION 'Completion requires verified canonical quantity and action'; END IF;
 SELECT COALESCE(SUM(quantity),0) INTO used FROM mulino_responsibility_ResolutionCredits WHERE organizationId=binding.organizationId AND bindingId=binding.ID;
 IF used>binding.quantity THEN RAISE EXCEPTION 'Completion evidence quantity already consumed'; END IF;
 IF TG_TABLE_NAME='mulino_responsibility_resolutioncredits' THEN
  SELECT * INTO scope FROM mulino_responsibility_Scopes WHERE organizationId=NEW.organizationId AND ID=NEW.scopeId;
  IF NEW.startQuantity IS DISTINCT FROM scope.startQuantity OR NEW.quantity IS DISTINCT FROM scope.quantity OR NEW.startQuantity<binding.startQuantity OR NEW.startQuantity+NEW.quantity>binding.startQuantity+binding.quantity THEN RAISE EXCEPTION 'Completion credit requires exact covered duty leaf range'; END IF;
  IF NOT EXISTS(SELECT 1 FROM mulino_work_read_ObligationReferences a WHERE a.organizationId=NEW.organizationId AND a.ID=NEW.assignmentId AND a.rootId=NEW.rootId AND a.scopeId=NEW.scopeId) THEN RAISE EXCEPTION 'Completion credit assignment scope mismatch'; END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE CONSTRAINT TRIGGER responsibility_binding_credit_integrity AFTER INSERT ON mulino_responsibility_CompletionBindings DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION mulino_responsibility_credit_validate();
CREATE CONSTRAINT TRIGGER responsibility_resolution_credit_integrity AFTER INSERT ON mulino_responsibility_ResolutionCredits DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION mulino_responsibility_credit_validate();
CREATE FUNCTION mulino_responsibility_credit_immutable() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Completion evidence bindings and credits are immutable'; END $$;
CREATE TRIGGER responsibility_binding_immutable BEFORE UPDATE OR DELETE ON mulino_responsibility_CompletionBindings FOR EACH ROW EXECUTE FUNCTION mulino_responsibility_credit_immutable();
CREATE TRIGGER responsibility_credit_immutable BEFORE UPDATE OR DELETE ON mulino_responsibility_ResolutionCredits FOR EACH ROW EXECUTE FUNCTION mulino_responsibility_credit_immutable();
