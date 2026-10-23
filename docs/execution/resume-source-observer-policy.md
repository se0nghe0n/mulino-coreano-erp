# 원천 입력·DB 관찰과 구매 승인 소비 정책 통합

2026-10-23 기록이다. 전체 Task와 현재 Step는 OPEN이다.
이번 deterministic 검증을 실제 시나리오나 실모델 인수로 승격하지 않는다.

## 변경과 근거

T17 입력 worker `2348dfd3`를 `b8dae950`에 통합했다. 원래 SELL 근거와
별개인 새 synthetic DISPATCH 원문을 명시하고, 실제 document → event →
claim → reconciliation → canonical occurrence → typed decision 경로로
설치한다. 원래 dispatch가 예약한 A만 대상으로 하며 UNKNOWN·DENIED·
CONFLICT를 허용으로 바꾸지 않는다. 보류 입력에는 operation별 원문과
새로 선택한 유효기간을 구별해 기록한다. 원래 257개 업무 assertion과
기존 15개 public pointer 교정, 사업 actor/grant·시계·수량·조건은 이
commit에서 보존했다. 이후 새 fixture 사전 입력 수정은 별도 작업이다.

관찰 worker `e542bb72`를 `f4dbe48f`에 통합했다. 저장된 목표 기간,
정확한 과거 definition/policy ID, 판정의 원래 condition ID와 입력
snapshot을 같은 read-only REPEATABLE_READ snapshot에서 읽는다.
STATE_OBSERVATION은 실제 검증·canonical·문서·실물 범위 연결을 확인한
경우에만 coverage와 수량을 투영한다. SQL과 boundValues를 보존하며
UNKNOWN을 0으로 바꾸거나 실제 INITIAL_BALANCE 행을 숨기지 않는다.

coordinator는 이 변경의 `supervisionIssues` 무조건 거부가 다른 정상
source 관찰까지 막는 회귀를 발견했다. 후속 `fa593e5f`를 `db775896`에
통합했다. 해당 조직의 두 recovery 원본 테이블이 현재 snapshot에서
모두 비어 있음을 SQL count로 입증할 때만 좁은 범위의 빈 결과를
반환한다. 같은 조직에 행이 하나라도 있으면 상태나 미래 기한과 관계없이
계속 거부한다. 이 증거는 과거 knownAt 이력이나 삭제 이력을 복원하지
않는다. 다른 unsupported scope는 완화하지 않았다.

구매 승인 worker `19b4fb98`를 `5f00e5d8`에 통합했다. 계획 §7이 요구하는
`consumptionPolicy`를 실제 immutable Approvals에 저장하고 dispatch 때
검사한다. 공개 정책은 `SINGLE_ORDER_REVISION`이며 새 요청에서 생략하면
같은 정책을 서버 기본값으로 명시 저장한다. 미지원 값은 거부한다.
V38은 과거 NULL을 보존하며 그 승인은 새 결정 없이 발주할 수 없다.
기존 fence·unique constraints와 transaction 안에서 주문 revision당
단일 발주·outbox를 유지한다. 현재 승인·정책·승인자 권한도 재검사한다.

`bd8b7a54`에서 공개 slot과 운영 skill mirror·package hash를 동기화하고
정확한 CDS/Flyway 목록에 추가 열 하나를 반영했다. 기존 열의 종류·크기·
scale·PK·허용된 timestamp/NOT NULL 차이는 변경하지 않았다.

## coordinator 검증

`f4dbe48f`의 결합 결과는 다음과 같다.

| 검사 | 결과 | 원본 artifact |
|---|---|---|
| 전체 harness, 실제 observer PG 9개 포함 | 625 PASS, T17 opt-in 17 SKIP, 실패·오류 0 | `.local/resume-20261023/source-observer-integrated/harness.log` |
| fresh build T17 HTTP/Spring/PostgreSQL | 17 PASS, 실패·오류·skip 0 | `.local/t17-source-pg-d37b96a5-8a0f-49fd-89e5-48bbd7acc723/test.log` |
| T17 원본 보존·가역·재현 Python | 21 PASS | `.local/resume-20261023/t17-source-observer-python.log` |
| prepare | 41 cases / 802 subcases / 24,260 assertions, 문제 0 | `.local/resume-20261023/prepare-source-observer.log` |

T17 PG runner는 해당 checkout의 backend를 새로 package하고 source/JAR
hash와 실제 HTTP 요청·응답을 남겼다. 실제 SELL-only DISPATCH 거부와
명시한 DISPATCH 30 BOX 출고, 잘못된 수량·기간·물량·타 조직 증거의
차단을 확인했다. 보류 원문은 검증됐지만 원래 사업 actor의 category
권한은 FORBIDDEN이었다. missing dueAt 원본 negative control도 효과 없이
거부됐다. 생성한 DB와 임시 인증키를 정리했다.

이 결합 증거와 로그 hash는
`.local/resume-20261023/source-observer-integration-proof.json`에 있다.

구매 정책을 통합한 제품 코드의 결합 검사는 47 PASS다.
PurchaseCommandPostgresTest 32개, V37 → V38 migration 1개,
S1ReadIntegrationTest 14개를 한 번에 실행했다. explicit/default 저장,
두 조회 진입점, 미지원·과거 NULL 차단, replay·새 key·동시 발주의 단일
효과, 권한 철회와 rollback을 확인했다. 로그는
`.local/resume-20261023/purchase-policy-backend-integrated.log`다.
CDS와 실제 PostgreSQL의 1,939개 열 및 PK가 정확히 일치하며, 관찰
artifact SHA-256은 schema compatibility manifest에 기록했다.

runtime skill 정적 검사도 6 packages / 12 discovery links로 통과했다.
이 검사의 actualModelCalls는 0이며 실제 body loading/tool execution
인수를 뜻하지 않는다. 후속 observer의 첫 결합 검사는 새 V38 열이 없다는
옛 테스트 기대 한 건으로 실패했다. 과거 NULL과 실제 저장값을 구별하도록
`e2514946`에서 수정한 뒤 629 PASS / T17 opt-in 17 SKIP으로 통과했다.
첫 실패 로그와 수정 후 로그를 각각 보존했다.

T17 후속 `a5fd4d0c`도 `d8e56c17`에 통합했다. 새 합성 기한 7건은 fixture
시계의 한 시간 뒤이며 원래 조건·LOT 만료보다 빠르다. category 권한은
8개 fixture의 기존 actor·조직·scope·기간을 유지한 최소 추가다. 원래
grant의 별도 negative copy는 그대로 거부된다. 누락 packageTerms 2건은
원래 품목의 포장 version을 참조한 새 거래 입력이다. replacement와
release 원문은 원래 descriptor를 보존하고 실제 주문 Work·sales-line·
allocation·restriction ID와 대상 revision을 공개 응답으로 연결한다.
기존 날짜·수량·조건·scope, 257개 assertion byte block과 두 이전 ledger를
보존했다. 상세 변경은 [T17 후속 입력](../../verification/cases/T17/COMPLETION_INPUTS.md)에 있다.

최종 source의 Python 25개와 prepare를 통과했다. 전체 harness는 실제
observer PG 13개를 포함해 629 PASS / T17 opt-in 22 SKIP, 실패·오류 0이다.
그 22개도 별도 fresh build HTTP/Spring/PostgreSQL 실행에서 모두 통과했다.
이 최종 실행은 `d8e56c17`의 source/JAR hash에 연결되며 생성한 DB와
임시 인증키를 정리했다. 통합 증거와 로그 hash는
`.local/resume-20261023/source-observer-policy-completion-proof.json`에 있다.
T17 원본 assertion 전체에 대한 실제 시나리오 결과는 아직 NOT_RUN이다.

## 남은 작업과 실행 구분

새 synthetic fixture 사전 입력을 운영 승인이나 원래 주어진 권한으로
표시하지 않는다. 이 입력으로 명령이 APPLIED에 도달한 것과 실제 업무
oracle 전체의 PASS는 서로 다른 증거다.

실제 시나리오는 지정된 Haiku 5.5 native dynamic Workflow로만 다시
실행한다. 마지막 확인된 55개 wave는 과거 `35234fa6`에서 23 PASS /
32 FAIL이며, T17 과거 `baf6ea4d` 실행은 15 FAIL이다. 서로 다른 source의
행이나 assertion PASS를 합쳐 최신 전체 인수율로 표시하지 않는다.
이번 source의 실제 결과가 나올 때까지 runtime gate는 OPEN이다.

위임 구현은 실제 catalog가 지원하는 `codex/gpt-6.1-sol` Ultrafast로
진행했고 child configuration도 확인했다. coordinator의 실제 대화 설정은
Sol xhigh / `serviceTier=default`다. 현재 turn을 바꾸는 도구가 없어
coordinator의 소규모 통합 수정·결합 검증까지 Ultrafast였다고 주장하지
않는다. 이 차이를 사용자에게 알렸고 이후 새 구현 위임에는 Ultrafast를
유지한다. 사용량 snapshot 도구는 현재 catalog에 없어 비율을 추정하지
않고 작은 팀으로 진행했다. 기존 구독 인증만 사용하며 별도 API 과금,
BTP 계정·region·배포 예산은 승인된 것으로 보지 않는다.
