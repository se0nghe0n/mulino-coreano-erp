namespace mulino.identity;
entity Organizations { key ID : UUID; externalAlias : String(80); revision : Integer; createdAt : Timestamp; recordedAt : Timestamp; }
entity Actors { key organizationId : UUID; key ID : UUID; kind : String(20); stableRequestOwner : UUID; revision : Integer; createdAt : Timestamp; recordedAt : Timestamp; }
entity ExternalIdentities { key organizationId : UUID; key ID : UUID; actorId : UUID; issuer : String(500); subject : String(200); organizationAlias : String(80); revision : Integer; createdAt : Timestamp; recordedAt : Timestamp; }
entity Memberships { key organizationId : UUID; key ID : UUID; actorId : UUID; validFrom : Timestamp; validUntil : Timestamp; revokedAt : Timestamp; revision : Integer; createdAt : Timestamp; recordedAt : Timestamp; }
entity CapabilityAssignments { key organizationId : UUID; key ID : UUID; actorId : UUID; capabilityId : String(100); scopeKind : String(20); scopeId : UUID; validFrom : Timestamp; validUntil : Timestamp; revokedAt : Timestamp; revision : Integer; createdAt : Timestamp; recordedAt : Timestamp; }
entity Grants { key organizationId : UUID; key ID : UUID; actorId : UUID; delegatorId : UUID; validFrom : Timestamp; validUntil : Timestamp; revokedAt : Timestamp; revision : Integer; createdAt : Timestamp; recordedAt : Timestamp; }
entity GrantActions { key organizationId : UUID; key grantId : UUID; key capabilityId : String(100); }
entity GrantScopes { key organizationId : UUID; key grantId : UUID; key scopeKind : String(20); key scopeId : UUID; }
entity AuthorityFences { key organizationId : UUID; key actorId : UUID; revision : Integer; }

entity ManagementAuthorities { key organizationId : UUID; key ID : UUID; actorId : UUID; capabilityId : String(100); scopeKind : String(20); scopeId : UUID; validFrom : Timestamp; validUntil : Timestamp; revokedAt : Timestamp; revision : Integer; }
entity AuthorityInvalidations { key organizationId : UUID; key ID : UUID; actorId : UUID; nextCheckAt : Timestamp; reason : String(100); revision : Integer; }
