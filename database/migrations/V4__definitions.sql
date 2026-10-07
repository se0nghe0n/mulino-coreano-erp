CREATE TABLE mulino_definitions_DefinitionVersions (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 0,
  createdAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  version VARCHAR(80) NOT NULL,
  parentVersionId VARCHAR(36),
  state VARCHAR(24) NOT NULL,
  contentHash VARCHAR(64) NOT NULL,
  content TEXT NOT NULL,
  evaluatorVersion VARCHAR(80) NOT NULL,
  schemaVersion VARCHAR(40) NOT NULL,
  PRIMARY KEY (organizationId,ID),
  FOREIGN KEY (organizationId) REFERENCES mulino_identity_Organizations(ID),
  FOREIGN KEY (organizationId,parentVersionId) REFERENCES mulino_definitions_DefinitionVersions(organizationId,ID),
  UNIQUE (organizationId,version),
  CHECK (state IN ('DRAFT','IN_REVIEW','PUBLISHED')),
  CHECK (contentHash ~ '^[0-9a-f]{64}$')
);

CREATE TABLE mulino_definitions_NounTypes (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 0,
  createdAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  definitionVersionId VARCHAR(36) NOT NULL,
  name VARCHAR(80) NOT NULL,
  core BOOLEAN,
  PRIMARY KEY (organizationId,ID),
  FOREIGN KEY (organizationId) REFERENCES mulino_identity_Organizations(ID),
  FOREIGN KEY (organizationId,definitionVersionId) REFERENCES mulino_definitions_DefinitionVersions(organizationId,ID)
);

CREATE TABLE mulino_definitions_AttributeDefinitions (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 0,
  createdAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  definitionVersionId VARCHAR(36) NOT NULL,
  nounType VARCHAR(80),
  name VARCHAR(80) NOT NULL,
  valueType VARCHAR(24),
  referenceType VARCHAR(80),
  unit VARCHAR(24),
  decimalPlaces INTEGER,
  minimumCount INTEGER,
  maximumCount INTEGER,
  requiredStage VARCHAR(40),
  core BOOLEAN,
  PRIMARY KEY (organizationId,ID),
  FOREIGN KEY (organizationId) REFERENCES mulino_identity_Organizations(ID),
  FOREIGN KEY (organizationId,definitionVersionId) REFERENCES mulino_definitions_DefinitionVersions(organizationId,ID),
  CHECK (minimumCount >= 0 AND maximumCount >= minimumCount),
  CHECK (decimalPlaces BETWEEN 0 AND 12),
  CHECK (valueType IN ('STRING','BOOLEAN','DECIMAL','INSTANT','DATE','REFERENCE'))
);

CREATE TABLE mulino_definitions_VerbDefinitions (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 0,
  createdAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  definitionVersionId VARCHAR(36) NOT NULL,
  name VARCHAR(80) NOT NULL,
  intentKind VARCHAR(16),
  capabilityId VARCHAR(80),
  stage VARCHAR(40),
  slots TEXT,
  PRIMARY KEY (organizationId,ID),
  FOREIGN KEY (organizationId) REFERENCES mulino_identity_Organizations(ID),
  FOREIGN KEY (organizationId,definitionVersionId) REFERENCES mulino_definitions_DefinitionVersions(organizationId,ID)
);

CREATE TABLE mulino_definitions_RelationDefinitions (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 0,
  createdAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  definitionVersionId VARCHAR(36) NOT NULL,
  name VARCHAR(80) NOT NULL,
  sourceType VARCHAR(80),
  targetType VARCHAR(80),
  minimumCount INTEGER,
  maximumCount INTEGER,
  cycleAllowed BOOLEAN,
  PRIMARY KEY (organizationId,ID),
  FOREIGN KEY (organizationId) REFERENCES mulino_identity_Organizations(ID),
  FOREIGN KEY (organizationId,definitionVersionId) REFERENCES mulino_definitions_DefinitionVersions(organizationId,ID),
  CHECK (minimumCount >= 0 AND maximumCount >= minimumCount)
);

CREATE TABLE mulino_definitions_GoalTemplates (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 0,
  createdAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  definitionVersionId VARCHAR(36) NOT NULL,
  name VARCHAR(80) NOT NULL,
  quantityMode VARCHAR(32),
  endpoint VARCHAR(80),
  evaluatorVersion VARCHAR(80) NOT NULL,
  predicate TEXT,
  PRIMARY KEY (organizationId,ID),
  FOREIGN KEY (organizationId) REFERENCES mulino_identity_Organizations(ID),
  FOREIGN KEY (organizationId,definitionVersionId) REFERENCES mulino_definitions_DefinitionVersions(organizationId,ID)
);

CREATE TABLE mulino_definitions_CapabilityContracts (
  organizationId VARCHAR(36) NOT NULL,
  ID VARCHAR(36) NOT NULL,
  revision INTEGER NOT NULL DEFAULT 0,
  createdAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  definitionVersionId VARCHAR(36) NOT NULL,
  capabilityId VARCHAR(80),
  semanticVersion VARCHAR(80),
  evaluatorVersion VARCHAR(80) NOT NULL,
  inputSchemaVersion VARCHAR(40),
  outputSchemaVersion VARCHAR(40),
  supportedWorkMigration TEXT,
  PRIMARY KEY (organizationId,ID),
  FOREIGN KEY (organizationId) REFERENCES mulino_identity_Organizations(ID),
  FOREIGN KEY (organizationId,definitionVersionId) REFERENCES mulino_definitions_DefinitionVersions(organizationId,ID)
);

CREATE FUNCTION mulino_definitions_immutable() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE v_state text; org text; version_id text;
BEGIN
 IF TG_TABLE_NAME = 'mulino_definitions_definitionversions' THEN
  IF TG_OP <> 'INSERT' AND OLD.state = 'PUBLISHED' THEN RAISE EXCEPTION 'Published definition is immutable' USING ERRCODE='23514'; END IF;
 ELSE
  IF TG_OP = 'INSERT' THEN org := NEW.organizationId; version_id := NEW.definitionVersionId;
  ELSE org := OLD.organizationId; version_id := OLD.definitionVersionId; END IF;
  SELECT state INTO v_state FROM mulino_definitions_DefinitionVersions WHERE organizationId=org AND ID=version_id FOR UPDATE;
  IF v_state='PUBLISHED' THEN RAISE EXCEPTION 'Published definition component is immutable' USING ERRCODE='23514'; END IF;
  IF TG_OP='UPDATE' AND (NEW.organizationId,NEW.definitionVersionId) IS DISTINCT FROM (OLD.organizationId,OLD.definitionVersionId) THEN RAISE EXCEPTION 'Definition parent cannot change' USING ERRCODE='23514'; END IF;
 END IF;
 IF TG_OP='DELETE' THEN RETURN OLD; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER immutable_definition BEFORE INSERT OR UPDATE OR DELETE ON mulino_definitions_DefinitionVersions FOR EACH ROW EXECUTE FUNCTION mulino_definitions_immutable();
CREATE TRIGGER immutable_definition BEFORE INSERT OR UPDATE OR DELETE ON mulino_definitions_NounTypes FOR EACH ROW EXECUTE FUNCTION mulino_definitions_immutable();
CREATE TRIGGER immutable_definition BEFORE INSERT OR UPDATE OR DELETE ON mulino_definitions_AttributeDefinitions FOR EACH ROW EXECUTE FUNCTION mulino_definitions_immutable();
CREATE TRIGGER immutable_definition BEFORE INSERT OR UPDATE OR DELETE ON mulino_definitions_VerbDefinitions FOR EACH ROW EXECUTE FUNCTION mulino_definitions_immutable();
CREATE TRIGGER immutable_definition BEFORE INSERT OR UPDATE OR DELETE ON mulino_definitions_RelationDefinitions FOR EACH ROW EXECUTE FUNCTION mulino_definitions_immutable();
CREATE TRIGGER immutable_definition BEFORE INSERT OR UPDATE OR DELETE ON mulino_definitions_GoalTemplates FOR EACH ROW EXECUTE FUNCTION mulino_definitions_immutable();
CREATE TRIGGER immutable_definition BEFORE INSERT OR UPDATE OR DELETE ON mulino_definitions_CapabilityContracts FOR EACH ROW EXECUTE FUNCTION mulino_definitions_immutable();

CREATE TABLE mulino_governance_PolicyVersions (
 organizationId VARCHAR(36) NOT NULL, ID VARCHAR(36) NOT NULL,
 revision INTEGER NOT NULL DEFAULT 0, createdAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 version VARCHAR(80) NOT NULL, kind VARCHAR(80) NOT NULL, content TEXT NOT NULL,
 contentHash VARCHAR(64) NOT NULL, effectiveFrom TIMESTAMP NOT NULL,
 effectiveUntil TIMESTAMP,
 PRIMARY KEY(organizationId,ID),
 FOREIGN KEY(organizationId) REFERENCES mulino_identity_Organizations(ID),
 UNIQUE(organizationId,kind,version),
 CHECK(effectiveUntil IS NULL OR effectiveUntil>effectiveFrom),
 CHECK(contentHash ~ '^[0-9a-f]{64}$')
);
CREATE FUNCTION mulino_policy_immutable() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Policy version is immutable' USING ERRCODE='23514'; END $$;
CREATE TRIGGER immutable_policy BEFORE UPDATE OR DELETE ON mulino_governance_PolicyVersions
 FOR EACH ROW EXECUTE FUNCTION mulino_policy_immutable();
