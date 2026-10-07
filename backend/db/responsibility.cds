using { mulino.work.read as read } from './work-read';
namespace mulino.responsibility;
aspect record { key organizationId : UUID; key ID : UUID; revision : Integer not null default 0; createdAt : Timestamp not null; recordedAt : Timestamp not null; }
entity Roots : record { dutyKey : String(64) not null; sourceId : UUID not null; sourceKind : String(20) not null; sourceVersion : String(80) not null; kind : String(80) not null; scopeJson : LargeString not null; quantity : Decimal(38,12); unit : String(20); }
entity Scopes : record { rootId : UUID not null; parentScopeId : UUID; startQuantity : Decimal(38,12) not null; quantity : Decimal(38,12) not null; unit : String(20); leaf : Boolean not null default true; scopeJson : LargeString not null; }
entity Handovers : record { kind : String(20) not null; workId : UUID not null; assignmentId : UUID; targetWorkId : UUID; previousOwnerId : UUID not null; recipientId : UUID not null; status : String(20) not null; expiresAt : Timestamp not null; nextAction : String(500) not null; nextCheckAt : Timestamp not null; quantity : Decimal(38,12); reason : String(500); }
extend read.ObligationReferences with { rootId : UUID; scopeId : UUID; valid : Boolean default true; evidenceId : UUID; basis : String(500); predecessorId : UUID; }
entity CompletionBindings : record { occurrenceId : UUID not null; rootId : UUID not null; startQuantity : Decimal(38,12) not null; quantity : Decimal(38,12) not null; unit : String(20); verificationId : UUID not null; coverageId : UUID not null; }
entity ResolutionCredits : record { bindingId : UUID not null; rootId : UUID not null; scopeId : UUID not null; assignmentId : UUID not null; startQuantity : Decimal(38,12) not null; quantity : Decimal(38,12) not null; }
