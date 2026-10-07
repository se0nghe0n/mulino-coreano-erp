namespace mulino.platform;
entity Scopes { key ID : UUID; organizationId : String(80); quantity : Decimal(38,12); reserved : Decimal(38,12); revision : Integer; blocked : Boolean; }
entity Grants { key ID : UUID; scopeId : UUID; actor : String(80); organizationId : String(80); allowed : Boolean; revision : Integer; }
entity Policies { key ID : UUID; scopeId : UUID; state : String(20); revision : Integer; }
entity Restrictions { key ID : UUID; scopeId : UUID; active : Boolean; }
entity Audit { key ID : UUID; scopeId : UUID; actor : String(80); operation : String(40); }
entity Outbox { key ID : UUID; scopeId : UUID; operationId : UUID; state : String(40); }
entity Idempotency { key ID : UUID; organizationId : String(80); owner : String(80); capability : String(80); commandKey : String(160); intentHash : String(64); response : LargeString; }
entity Preserved { key ID : UUID; workState : String(40); definitionVersion : String(40); evidenceHash : String(64); }
