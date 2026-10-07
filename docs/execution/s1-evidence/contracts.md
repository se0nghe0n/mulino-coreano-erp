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

## 실제 검증과 미완료 경계

`1492b0b` 소스에서 `EvidencePersistenceTest` 15건이 실제 PostgreSQL18.6,
CQN, 현재 IdentityAuthorization 및 macOS 파일 저장소로 통과했다.
정확한 실행 명령과 입력 hash는 [checks.json](checks.json)과
[input-hashes.json](input-hashes.json)에 있다. 임시 model import는 실행
후 제거했다. 통합 담당자가 최종 service model import를 소유한다.

| Test method | 관찰한 효과 / oracle 연결 |
|---|---|
| originalFileHashAndAuthorizedDownloadAreImmutable | T07 원본 byte/hash, DB UPDATE 차단, 현재 grant 철회 후 download 거부 |
| stagingHashMismatchAndDatabaseRollbackLeaveNoReferencesOrObjects | T07 hash 오류 효과0, 실제 DB rollback 후 참조0·추가 파일0 |
| unavailableUriNeverFetchesOriginal | T07 외부 URI 기록은 UNKNOWN, URI read/download 없음 |
| duplicateSourceKeyReplaysOnceAndCompetingHashPreservesConflict | T22 같은 source key replay ID 동일, 다른 hash는 원문2·CONFLICT 접수1 |
| supersedesPreservesKnowledgeTimeAndFourUnknownStates | T06 과거 knownAt 원문 보존, 새 revision 연결과4개 상태 구분 |
| twoDocumentsOneCanonicalReceiptCountsOnlyOneAndHasNoInventoryEffects | T07 문서2·검증2·canonical 사건1·량60; 실물 효과 미구현 |
| sourceConflictCannotBecomeCanonicalVerified | T22 상충 원천의 canonical 생성0 |
| orphanCleanupPreservesReferencedFilesAndMissingFileReturnsUnknown | T07 한 시간 grace 뒤 고아1 정리, 참조 원문 보존, 원문 소실 UNKNOWN |
| foreignOrganizationActorReferenceAndTypedDateAsPlaceFailClosed | V1 다른 조직 actor FK 거부, T07 날짜/Place type 오류 |
| concurrentSourceReplayHasExactlyOneInboxAndEvent | V7 실제2thread/transaction의 사건1·접수1·동일 ID |
| pageMetadataBelongsOnlyToCurrentPageAndCursorCannotChangeQuery | 실제 page별 evidenceRefs1, 다음 page 별도 ID, cursor/filter 변경 거부 |
| sourceScopedGrantCannotReadOtherSourceAndInvalidQuantityCannotBeRecorded | V1 SOURCE grant 범위 거부, BOX 소수와 numeric 범위 초과 claim0 |
| lateCompetingSourceHashPreservesHistoricalVerificationAndBlocksCurrentConfirmation | T06/T22 과거 verified 유지, 현재 상충에서 verified false |
| stagedAndCommittedFilesHaveOwnerOnlyPermissions | 실제 directory0700·staging0600·immutable object0400 |
| dateOnlyRangeAndUtcInstantRoundTripPreserveTemporalBounds | T06 UTC instant·원 timezone·DAY 범위 왕복과 끝점 제외 |

[Fresh compiler 비교](schema-comparison.json)는8개 entity의 column/type을
V6와 대조했다. 24개 Timestamp column은 의도적으로 TIMESTAMPTZ를 쓴다.
CQN UTC/DAY 왕복이 통과했으며 그 밖의 column/type 차이는0이다. compiler
SQL은 [compiler.sql](compiler.sql)에 보존했고 Flyway 외에는 DDL을 실행하지
않았다.

초기 두 실행은 fixture의 필수 Specification/Packaging version 누락으로
실패했다. 세 번째 실행은 테스트 SecurityContext가 authenticated=false인
constructor를 사용해 실패했다. failure 로그를 압축 보존하고 fixture를
교정했다. 이는 제품 업무의 유효한 RED나 성공 증거로 세지 않는다.
9건 통과 뒤 scope·quantity·concurrency·file 권한 보강을 추가했고 최종
15건에서 다시 확인했다. 전체 T06/T07/T22·signed HTTP/MCP·업무 재평가·
실물 원장·보존/복원·T41 runtime 인수는 별도 후속 gate다.

Blob root와 staging/objects는0700이어야 한다. 새 directory/file의 mode는
생성 시 적용하며 기존 무관한 경로를 chmod하지 않는다. 기존 root가
공개 권한이면 fail-closed다. 한 시간 이상 grace의 고아 정리는 내부
명령이며 자동 scheduler·운영 삭제 정책은 S6에서 인수해야 한다.
