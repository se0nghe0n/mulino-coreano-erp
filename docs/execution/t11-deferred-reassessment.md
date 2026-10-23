# T11 정정의 동일 기록 시각과 재평가 보류

Task와 Step은 OPEN이다. 이 문서는 격리 branch의 집중 구현 증거이며
원래 T11 110 assertions의 실제 인수 완료를 뜻하지 않는다.
SYNC e58817d5는 Task로 역통합하지 않는다.

## 동일 기록 시각의 정정

원래 고정 verification clock에서는 원본 EVENT와 정정 EVENT의
`recordedAt`이 같다. V6의 `mulino_evidence_validate_supersession()`은
`prior_recorded >= NEW.recordedAt`에서 SQLSTATE 23514를 발생시킨다.
이 경우 HTTP500은 성공한 correction의 증거가 아니다. 기존 pending은
앞선 canonical 연결에서 생겼을 수 있으므로 correction commit과
구별한다. 실제 PostgreSQL V43에서도 동일 조건을 재현했다.

V44는 이 함수만 교체한다. 기존 prior가 실제 존재하고 같은 조직·
subjectId·subjectKind이며 revision이 정확히 1 증가할 때 같은 기록
instant를 허용한다. 더 이른 instant·누락 prior·자기 참조·다른 scope·
잘못된 revision은 계속 23514다. 불변 prior와 엄격한 revision 증가가
동일 시각 내 순서를 정한다. 기존 single-successor unique indexes와
UPDATE/DELETE 금지 trigger는 그대로다. 재귀적으로 과거를 수정하거나
backfill하지 않는다. 원래 effective/knownAt·fixture clock을 바꾸지 않는다.

`EqualInstantEvidenceMigrationPostgresTest`는 실제 PostgreSQL 18.6에서
fresh V44와 V43→V44를 각각 검사한다. V43 동일 시각 거부 RED,
revision 1→2→3→4의 같은 시각 append, 과거 원행 보존,
이전 시각·missing prior·org/subject/kind/revision/self 참조 거부,
UPDATE/DELETE 거부, 두 실제 transaction의 unique fence lock과
중복 branch 23505를 확인한다. Upgrade 전후 columns·constraints·
indexes·triggers inventory가 같고 migration은 정확히 하나다.

schema delta는 함수 body 교체 하나다. 추가/수정 tables·columns·PK·FK는
0개이며 schema manifest와 과거 migration checksum은 수정하지 않는다.

집중 원로그와 JUnit은 `.local/t11-deferred-reassessment/`에 보존한다.
`attempt6-v44-green-runtime-envelope-red.log`는 V44 검사 2 PASS와
고정 clock의 실제 HTTP EVENT98 APPLIED를 포함한다. 동일 로그의
runtime UUID 입력 오류는 migration과 별도 경계로 남겼다.

## 보류와 release

`ReassessmentDeferral`은 production bean이 없는 optional port다.
verification profile의 `VerificationFaults`만 구현한다. 실제
`correctEvidence` command의 IN_PROGRESS 원행과 command key를 같은
transaction에서 읽고, 실제 invalidation과 책임 보존이 끝난 Work만
checkpoint로 묶는다. IMPORTED 참조만 있는 경로는 도달로 표시하지 않는다.

arm은 ARMED다. 실제 correcting transaction 안에서 REACHED로 예약해
commit 직후의 scheduler race를 막는다. afterCommit에서만 DEFERRED와
committed 시각을 기록한다. rollback은 ROLLED_BACK이며 gate를 해제한다.
HTTP correction은 release를 기다리지 않는다. 원본 EVENT98·pending·
실제 correction duty·command 결과가 함께 commit되어야 release할 수 있다.
clock은 제어하지 않는다. control의 reached/committed 시각은 운영 사실이
아닌 host 측 verification 관찰 시각이다.

`RuntimeRecoverySweeper`는 gate된 정확한 org/Work의 기존 schedule을
남겨둔다. `RuntimeRecoveryExecution`도 claim 전후에 gate를 재확인한다.
실제 command가 correction보다 먼저 fence를 잡으면 기존 pipeline이
직렬화하고, 나중이면 수정된 Work revision과 현재 사실을 다시 검증한다.
부여된 권한·evaluator·policy·evidence 검사를 생략하지 않는다.

실제 PG 검사에서 기존 recovery envelope가 workId의 `{type:WORK,id:…}`를
보내지만 AssessmentService는 typed object의 `value`를 읽어
`TYPE_INVALID: UUID workId required`가 됨도 확인했다. 소유한 execution
파일에서 실제 scalar workId로 envelope만 고쳤다. handler·판정 의미나
권한을 바꾸지 않는다. RED는 attempt5/6 원로그에 남겼다.

release는 정확한 point/org와 필요시 faultId/capability/key로 선택한다.
도달 전·미commit·rollback·다른 org·모호한 선택은 422다. 같은 확정된
release의 반복은 harmless다. 다른 실패 attempt가 생겼으면 이전
release를 빌려 성공으로 응답하지 않는다. reached 상태를 disarm으로
우회하지 못한다. 활성 control 최대128, control당 Work 최대1000이며
HTTP를 block하거나 자동 timeout으로 조용히 release하지 않는다.

controls는 process-local verification 상태다. process restart를 가로지르는
보류는 지원하지 않는다. production에는 control endpoint와 gate 구현이
없다. 원래 T11의 arm→correction→observe→release→tick 순서는 같은
verification process를 전제로 한다.

ActualAcceptanceDriver는 기존 fault HTTP adapter로 release를 전송한다.
서버의 동일 org/point·committed=true·RELEASED 응답 없이는 acknowledgment를
만들지 않는다. 422의 실제 거부 reason을 유지한다. 조회 응답이나
observer·case oracle를 합성/변경하지 않는다.

## 집중 검증의 범위

실제 Spring/PG 검사는 별도 합성 조직·정의·현재 policy·명시적인 새
합성 권한을 사용한다. 원래 case grant·110 assertions·ledger·수량·
시계를 수정하지 않는다. 실제 public HTTP로 고정 clock EVENT100→98,
원행 보존·replay 효과0·pending true와 assessment/snapshot 0을 확인한다.
다른 검사에서는 실제 사전 assessment/snapshot을 생성해 보류 동안
불변을 확인하고, 다른 조직의 실제 scheduler 진행, 정확한 release 후
기존 claimed pipeline의 새 assessment와 pending 해제, revoked grant의
CURRENT_AUTHORITY_DENIED 및 pending 유지, checkpoint 뒤 rollback의
정정·의무·pending 효과0을 확인한다. 원래 rollback/lost-response
CommandTransactionTest도 유지한다.

이 합성 EVENT만으로 canonical 수령/인도나 기여량을 만들지 않는다.
원래 receipt/canonical 정정의 전체 T11 업무 흐름은 실제 시나리오 재실행
전까지 NOT_RUN이다. 원래 negative outcome HELD/REJECTED 차이,
observer의 사전 assessment 없는 PENDING 표현, shared contribution
projection 문제는 이 변경의 범위 밖이며 원래 기대를 수정하지 않는다.

최종 prior 조회는 `organizationId=$1 AND ID=$2`를 bind한다. 이 tree의
실제 migration catalog에서 evidence Events의 PK는 `PRIMARY KEY(id)`다.
따라서 실제 product table에 다른 조직의 같은 ID를 동시에 저장할 수는
없다. 제품 schema는 수정하지 않고, 별도 TEMP composite-key table에
동일한 실제 V44 함수를 붙여 foreign 동일 ID가 먼저 존재해도 own prior로
정확히 append됨과 foreign-only prior 거부를 추가 검증했다. 실제 table의
foreign-only 거부와 기존 네 correction unique indexes도 그대로다.

최신 class별 집중 결과는 backend 36 PASS / 0 FAIL / 0 skip다.
(Migration2, Spring/HTTP/PG4, deferral8, scheduler3, runtimePG7,
command transaction12.) Harness adapter는 4 PASS / 0 FAIL / 0 skip다.
Prepare는 12 asset checks PASS, preparationProblems0, exit0이며
capability slot findings413은 계속 별도 FINDINGS다. runtimeComplete와
artifactCoverageStatus는 NOT_RUN이다. 자체 npm ci(Node24)·Java21 CDS
생성·package를 수행했으며 sibling build를 복사하지 않았다.

최종 JUnit은 `.local/t11-deferred-reassessment/final-junit/`, 실제 SQL 원행은
`sql-proof/`, command/exit·source/class/JAR hash·원본 byte 대조·정리 증거는
같은 artifact directory의 handoff 및 inventory JSON에 남긴다. 원래 native
T11 재실행·full backend·full harness는 primary 소유라 실행하지 않았다.
