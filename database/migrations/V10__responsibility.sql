-- Canonical assignments extend the existing read entity; there is no shadow table.
DROP TRIGGER obligation_read_immutable ON mulino_work_read_ObligationReferences;
CREATE TABLE mulino_responsibility_Roots (
 organizationId varchar(36) NOT NULL, ID varchar(36) NOT NULL, revision integer NOT NULL DEFAULT 0,
 createdAt timestamptz NOT NULL, recordedAt timestamptz NOT NULL, dutyKey varchar(64) NOT NULL,
 sourceId varchar(36) NOT NULL, sourceKind varchar(20) NOT NULL, sourceVersion varchar(80) NOT NULL,
 kind varchar(80) NOT NULL, scopeJson text NOT NULL, quantity numeric(38,12), unit varchar(20),
 PRIMARY KEY(organizationId,ID), UNIQUE(organizationId,dutyKey),
 FOREIGN KEY(organizationId) REFERENCES mulino_identity_Organizations(ID),
 CHECK(sourceKind IN ('OCCURRENCE','DECISION')), CHECK(jsonb_typeof(scopeJson::jsonb)='object'),
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
 IF EXISTS(SELECT 1 FROM mulino_responsibility_Scopes a JOIN mulino_responsibility_Scopes b ON a.organizationId=b.organizationId AND a.rootId=b.rootId AND a.ID<b.ID WHERE a.organizationId=org AND a.rootId=rid AND a.leaf AND b.leaf AND a.startQuantity<b.startQuantity+b.quantity AND b.startQuantity<a.startQuantity+a.quantity) THEN RAISE EXCEPTION 'Overlapping duty leaf scopes'; END IF;
 IF EXISTS(SELECT 1 FROM mulino_responsibility_Scopes s WHERE s.organizationId=org AND s.rootId=rid AND s.leaf AND (SELECT COUNT(*) FROM mulino_work_read_ObligationReferences a WHERE a.organizationId=org AND a.scopeId=s.ID AND a.valid AND a.status IN ('OPEN','RESOLVED','WAIVED'))<>1) THEN RAISE EXCEPTION 'Duty leaf requires one current assignment'; END IF;
 RETURN NEW;
END $$;
CREATE CONSTRAINT TRIGGER responsibility_root_integrity AFTER INSERT OR UPDATE ON mulino_responsibility_Roots DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION mulino_responsibility_validate();
CREATE CONSTRAINT TRIGGER responsibility_scope_integrity AFTER INSERT OR UPDATE ON mulino_responsibility_Scopes DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION mulino_responsibility_validate();
CREATE CONSTRAINT TRIGGER responsibility_assignment_integrity AFTER INSERT OR UPDATE ON mulino_work_read_ObligationReferences DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION mulino_responsibility_validate();
CREATE TRIGGER responsibility_assignment_human BEFORE UPDATE ON mulino_work_read_ObligationReferences FOR EACH ROW EXECUTE FUNCTION mulino_work_read_human_responsibility();
