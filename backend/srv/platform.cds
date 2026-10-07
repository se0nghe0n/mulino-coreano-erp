using mulino.platform as db from '../db/schema';
@path:'/platform'
@requires:'platform-spike'
service PlatformService {
 @readonly entity Scopes as projection on db.Scopes;
 action reserve(scopeId: UUID, quantity: String, expectedRevision: Integer, idempotencyKey: String) returns String;
}
