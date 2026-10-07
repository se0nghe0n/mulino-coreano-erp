namespace mulino.trade.regulatory;
// Installed policy is versioned; fictional fixture policy never establishes legal permission.
entity Policies {
  key ID : UUID;
  organizationId : UUID;
  revision : Integer;
  createdAt : Timestamp;
  recordedAt : Timestamp;
  recordedBy : UUID;
  version : String(160);
  authority : String(160);
  sourceNamespace : String(160);
  action : String(80);
  validFrom : Timestamp;
  validUntil : Timestamp;
  fictional : Boolean;
  status : String(24);
  requiresLabel : Boolean;
}
entity Procedures {
  key ID : UUID;
  organizationId : UUID;
  revision : Integer;
  createdAt : Timestamp;
  recordedAt : Timestamp;
  recordedBy : UUID;
  itemId : UUID;
  lotId : UUID;
  physicalScopeId : UUID;
  workId : UUID;
  policyId : UUID;
  currentVersionId : UUID;
}
entity ProcedureVersions {
  key ID : UUID;
  organizationId : UUID;
  revision : Integer;
  createdAt : Timestamp;
  recordedAt : Timestamp;
  recordedBy : UUID;
  procedureId : UUID;
  previousVersionId : UUID;
  itemId : UUID;
  lotId : UUID;
  physicalScopeId : UUID;
  workId : UUID;
  policyId : UUID;
  status : String(40);
  documentVersionId : UUID;
  occurrenceId : UUID;
  occurredAt : Timestamp;
}
entity DecisionVersions {
  key ID : UUID;
  organizationId : UUID;
  revision : Integer;
  createdAt : Timestamp;
  recordedAt : Timestamp;
  recordedBy : UUID;
  procedureId : UUID;
  procedureVersionId : UUID;
  previousVersionId : UUID;
  itemId : UUID;
  lotId : UUID;
  physicalScopeId : UUID;
  policyId : UUID;
  action : String(80);
  decision : String(32);
  authority : String(160);
  startQuantity : Decimal(38,12);
  quantity : Decimal(38,12);
  unit : String(24);
  validFrom : Timestamp;
  validUntil : Timestamp;
  occurrenceId : UUID;
}
entity LabelVerifications {
  key ID : UUID;
  organizationId : UUID;
  revision : Integer;
  createdAt : Timestamp;
  recordedAt : Timestamp;
  recordedBy : UUID;
  procedureId : UUID;
  procedureVersionId : UUID;
  itemId : UUID;
  lotId : UUID;
  physicalScopeId : UUID;
  policyId : UUID;
  packagingVersionId : UUID;
  specificationVersionId : UUID;
  decision : String(24);
  validFrom : Timestamp;
  validUntil : Timestamp;
  occurrenceId : UUID;
}
