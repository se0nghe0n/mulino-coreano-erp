-- Scope IDs remain polymorphic. Concrete target validation belongs to the
-- identity command repository; this migration does not manufacture target FKs.
-- CAP checks String length in UTF-16 code units. 72 units admit every permitted
-- 36-codepoint SOURCE while PostgreSQL keeps the original character limit.
ALTER TABLE mulino_identity_GrantScopes
  ALTER COLUMN scopeId TYPE VARCHAR(72),
  ADD CONSTRAINT identity_grant_scope_id_length CHECK (char_length(scopeId) <= 36);
ALTER TABLE mulino_identity_CapabilityAssignments
  ALTER COLUMN scopeId TYPE VARCHAR(72),
  ADD CONSTRAINT identity_assignment_scope_id_length CHECK (char_length(scopeId) <= 36);
ALTER TABLE mulino_identity_ManagementAuthorities
  ALTER COLUMN scopeId TYPE VARCHAR(72),
  ADD CONSTRAINT identity_management_scope_id_length CHECK (char_length(scopeId) <= 36);

ALTER TABLE mulino_identity_GrantScopes
  DROP CONSTRAINT mulino_identity_grantscopes_scopekind_check;
ALTER TABLE mulino_identity_GrantScopes
  ADD CONSTRAINT mulino_identity_grantscopes_scopekind_check CHECK
    (scopeKind IN ('ORGANIZATION','TARGET','WORK','ITEM','PLACE','SOURCE',
                   'LOT','CUSTOMER','SUPPLIER'));

ALTER TABLE mulino_identity_CapabilityAssignments
  DROP CONSTRAINT mulino_identity_capabilityassignments_scopekind_check;
ALTER TABLE mulino_identity_CapabilityAssignments
  ADD CONSTRAINT mulino_identity_capabilityassignments_scopekind_check CHECK
    (scopeKind IN ('ORGANIZATION','TARGET','WORK','ITEM','PLACE','SOURCE',
                   'LOT','CUSTOMER','SUPPLIER'));
