namespace mulino.governance;
entity PolicyVersions {
 key organizationId : UUID; key ID : UUID;
 revision : Integer; createdAt : Timestamp;
 version : String(80); kind : String(80); content : LargeString;
 contentHash : String(64); effectiveFrom : Timestamp; effectiveUntil : Timestamp;
}
