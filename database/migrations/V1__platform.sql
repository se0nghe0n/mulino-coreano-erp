
CREATE TABLE mulino_platform_Scopes (
  ID VARCHAR(36) NOT NULL,
  organizationId VARCHAR(80),
  quantity DECIMAL(38, 12),
  reserved DECIMAL(38, 12),
  revision INTEGER,
  blocked BOOLEAN,
  PRIMARY KEY(ID)
);

CREATE TABLE mulino_platform_Grants (
  ID VARCHAR(36) NOT NULL,
  scopeId VARCHAR(36),
  actor VARCHAR(80),
  organizationId VARCHAR(80),
  allowed BOOLEAN,
  revision INTEGER,
  PRIMARY KEY(ID)
);

CREATE TABLE mulino_platform_Policies (
  ID VARCHAR(36) NOT NULL,
  scopeId VARCHAR(36),
  state VARCHAR(20),
  revision INTEGER,
  PRIMARY KEY(ID)
);

CREATE TABLE mulino_platform_Restrictions (
  ID VARCHAR(36) NOT NULL,
  scopeId VARCHAR(36),
  active BOOLEAN,
  PRIMARY KEY(ID)
);

CREATE TABLE mulino_platform_Audit (
  ID VARCHAR(36) NOT NULL,
  scopeId VARCHAR(36),
  actor VARCHAR(80),
  operation VARCHAR(40),
  PRIMARY KEY(ID)
);

CREATE TABLE mulino_platform_Outbox (
  ID VARCHAR(36) NOT NULL,
  scopeId VARCHAR(36),
  operationId VARCHAR(36),
  state VARCHAR(40),
  PRIMARY KEY(ID)
);

CREATE TABLE mulino_platform_Idempotency (
  ID VARCHAR(36) NOT NULL,
  organizationId VARCHAR(80),
  owner VARCHAR(80),
  capability VARCHAR(80),
  commandKey VARCHAR(160),
  intentHash VARCHAR(64),
  response TEXT,
  PRIMARY KEY(ID)
);

CREATE TABLE mulino_platform_Preserved (
  ID VARCHAR(36) NOT NULL,
  workState VARCHAR(40),
  definitionVersion VARCHAR(40),
  evidenceHash VARCHAR(64),
  PRIMARY KEY(ID)
);

ALTER TABLE mulino_platform_scopes ADD CONSTRAINT scope_quantity CHECK(quantity >= 0 AND reserved >= 0 AND reserved <= quantity);
ALTER TABLE mulino_platform_grants ADD CONSTRAINT grant_scope_fk FOREIGN KEY(scopeId) REFERENCES mulino_platform_scopes(id);
ALTER TABLE mulino_platform_policies ADD CONSTRAINT policy_scope_fk FOREIGN KEY(scopeId) REFERENCES mulino_platform_scopes(id);
ALTER TABLE mulino_platform_restrictions ADD CONSTRAINT restriction_scope_fk FOREIGN KEY(scopeId) REFERENCES mulino_platform_scopes(id);
ALTER TABLE mulino_platform_idempotency ADD CONSTRAINT idem_owner_key UNIQUE(organizationId,owner,capability,commandKey);
