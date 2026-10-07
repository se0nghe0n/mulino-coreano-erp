# S1 증거 계약

증거를 실행 허가나 실물 효과로 잘못 승격하지 않도록 원본·주장·확정 사건을
분리한다. 기준선은 `164414418b6c3ce2959979939388d59cf2068e2c`이며
writer는 `step3/s1-evidence`의 독립 worktree를 사용한다.

`backend/db/evidence.cds`와 `database/migrations/V6__evidence.sql`이
`mulino.evidence`의 SourceProfiles, DocumentVersions, Events, Claims,
InboxRecords, CanonicalOccurrences, Verifications, EvidenceLinks를 정의한다.
모든 ID는 server UUID다. `(organizationId,ID)` composite FK가 actor와
증거 간 조직 혼합을 막는다. UPDATE/DELETE trigger는 사실 원문을 보존한다.

DocumentVersions의 공개 필드는 ID, organizationId, revision, recordedAt,
recordedBy, subjectKind/subjectId, itemId/placeId/workId, sha256, byteLength,
mediaType, sourceNamespace/sourceProfileId, sourceReference, availability,
supersedesId, provenance다. 내부 blobId는 조회 응답에서 제외한다. 원문
hash를 매 다운로드에서 검증하며 파일이 사라지거나 변경되면 UNKNOWN이다.
외부 sourceReference를 저장해도 URI를 읽지 않는다.

`getEvidence`의 scope는 kind=`DOCUMENT|EVENT|CLAIM|CANONICAL`, 선택적
subjectKind=`ITEM|LOT|SEGMENT|WORK|PLACE`와 subjectId다. kind 기본값은
DOCUMENT다. filters는 includeSuperseded와 verifiedOnly boolean이다.
verifiedOnly는 CANONICAL에서만 허용한다. `getInbox`는 같은 typed subject
범위의 원천 접수를 읽는다. ID와 asOf/knownAt으로 직접 조회하거나 UUID
순서 cursor로 읽는다. TARGET 및 실제 ITEM/PLACE/WORK/SOURCE dimensions를
현재 인가한다. SOURCE는 namespace 문자열이 아닌 sourceProfile UUID다.

Events는 발생 범위·원 timezone/precision과 기록 시점을 보존한다. 원천
namespace+eventId+version의 같은 canonical payload는 replay다. 다른
payload는 새 immutable CONFLICT 접수이며 과거 원문을 덮지 않는다.
MISSING/UNKNOWN/NOT_APPLICABLE/CONFLICT를0으로 바꾸지 않는다.
supersedes는 같은 subject의 새 revision으로 기존 ID를 참조한다.

내부 EvidenceRecords port는 attachDocument, recordUnavailableDocument,
recordActivity, recordClaim, verifyCanonical이다. S1 public write dispatcher는
없다. canonical 검증은 현재 source policy, verified identity/quantity/time/
duplicate 대조, 사용 가능한 원문과 별도 인가를 요구한다. 서로 다른
문서 두 개는 existingCanonicalId 하나로 연결한다. 문서 수나 claim 수를
실물 수량에 합산하지 않는다. source key 충돌은 자동 확정하지 않는다.

Blob는 UUID staging→hash 검사→immutable object rename 후 DB 참조를
저장한다. DB rollback이면 transaction callback이 파일을 제거한다. process
종료 고아 파일은 grace가 지난 뒤 committed DB 참조 대조로 정리한다.
GET `/api/evidence/documents/{UUID}/content`는 현재 getEvidence 인가 후
원본 byte를 attachment로 반환하며 no-store/nosniff를 적용한다.

S2/S4 inventory movement·Assessment·현재 의무·Work 재평가는 아직
NOT_IMPLEMENTED다. VERIFIED_RECORD_ONLY도 업무 이행 PASS가 아니다.
T06/T07 일부 증거 oracle를 focused DB/file tests에 연결하고 전체 사례와
T41 runtime는 해당 후속 통합에서 별도 실행한다.
