namespace mulino.governance;
entity PolicyVersions {
 key organizationId : UUID; key ID : UUID;
 revision : Integer; createdAt : Timestamp;
 version : String(80); kind : String(80); content : LargeString;
 contentHash : String(64); effectiveFrom : Timestamp; effectiveUntil : Timestamp;
}

entity PolicyDrafts { key organizationId : UUID; key ID : UUID; kind : String(80); version : String(80); content : LargeString; contentHash : String(64); source : String(500); regressionEvidence : String(500); effectiveFrom : Timestamp; effectiveUntil : Timestamp; legallyRestrictive : Boolean; fixtureOnly : Boolean; status : String(30); revision : Integer; }
entity PolicyApprovals { key organizationId : UUID; key ID : UUID; draftId : UUID; contentHash : String(64); approverId : UUID; approvedAt : Timestamp; }
entity ActivePolicies { key organizationId : UUID; key kind : String(80); policyId : UUID; revision : Integer; }
entity PolicyFences { key organizationId : UUID; revision : Integer; }
entity PolicyBoundaries { key organizationId : UUID; key ID : UUID; policyId : UUID; nextCheckAt : Timestamp; reason : String(100); }
