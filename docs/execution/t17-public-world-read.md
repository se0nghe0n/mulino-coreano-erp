# T17 공개 world/read U1 구현

Task/Step은 OPEN이다. 이 문서는 U1 구현과 집중 증거를 기록하며
원래 T17 actual 결과 79 PASS/178 FAIL을 재분류하지 않는다.

작업 tree는
`/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-8f21ea6b`이고
branch는 `feat/shared-evidence-capture`다. primary가 만든 동기화
`cc94741c`의 tracked tree가 Task `db827fa0`와 같음을 착수 때
`git diff db827fa0`로 확인했다. SYNC와 과거 history를 Task에 병합하지
않으며 이번 새 scoped commit만 통합 대상으로 반환한다.

실제 runtime은 `t3_thread_configuration`으로 확인한
`codex/gpt-6.1-sol`, high, serviceTier=priority(Fast)다.

## 실제 공개 원천과 관계

ApplicationQueries의 기존 REPEATABLE_READ 조회 안에서 PublicWorldRead가
기존 repository의 조직별 원 행을 읽는다. 공개 effectLedger는 다음
원천과 실제 관계를 사용하며 행의 ID 순서가 안정적이다.

| ledger | 저장 원천과 바인딩 |
|---|---|
| segments | QuantitySegments의 실제 item/LOT/place와 시점, 실제 수량·단위·ID |
| allocations | SegmentAllocations의 실제 segment/Work/order line/customer |
| movements | QuantityMovements의 실제 source/target, 양쪽 현재 read 인가 |
| deliveries | Deliveries→Dispatch→allocation, 실제 Work/line/segment |
| receiptContributions | purchase ReceiptCredits→실제 purchase OrderLine |
| assessments | 현재 read 인가를 통과한 기존 저장 AssessmentReferences |

빈 배열은 해당 범위에서 실제 보이는 원 행이 없을 때의 결과다.
원천/권한/단위/관계가 불충분한 경우 배열이나 scalar 상수로 보완하지
않고 `publicReadGaps`와 envelope unknowns를 남긴다. 부분 read 권한에서는
보이는 사실 행만 반환하고 완전한 집계로 오인할 scalar를 생략한다.

실제 인도 quantity와 legitimateQuantity는 각 저장 값을 유지한다.
`deliveredQuantity`는 실제 인도이고 Goal 평가나 SELL 허용을 뜻하지 않는다.
`inTransitQuantity`는 실제 active TRANSIT 수량이다. Work/order line/
customer로 범위가 정해지면 저장된 genealogy 좌표와 Dispatch 범위를
기존 PhysicalRanges로 투영하고 union하여 다른 주문 물량을 더하지 않는다.
같은 부모에서 첫 주문20과 둘째 주문10이 나와도 각각20/10이며 ITEM은30이다.

`warehouseDispatchQuantity`는 실제 INTERNAL_STORAGE 출발 DISPATCH 원행의
누적량이다. 실제 인도 전후 delta는0이다. 기존 ITEM heldQuantity는
기존 InventoryQueries 의미를 유지한다. 창고40+운송60이면 ITEM held100이며
임의 default warehouse를 만들지 않는다. U7 범위 문제는 해결하지 않았다.

실제 allocation이 하나이면 allocationStatus를 읽는다. 두 행이 같은
order line의 유일한 predecessor 관계이고 원행이 REPLACED인 경우에만
oldAllocationStatus/newAllocationStatus를 읽는다. 여러 line·분기·더 긴
chain은 allocationRelations와 실제 행을 반환하고 단일 상태 요약을
생략한다. executableReservedQuantity는 실제 EXECUTABLE 행의 합이며
SUSPENDED/REPLACED/RELEASED를 실행 가능량에 더하지 않는다.

SalesQueries는 mutable Orders.currentRevision을 과거에 보간하지 않고
보이는 immutable OrderRevisions에서 current line을 정한다. 실제 line이
유일하고 읽을 수 있는 Work/장소 관계가 있을 때만 deliveryEndpoint를
노출한다. 여러 주문·line은 orderContracts로 각각 보존한다. 명시한
orderId/salesLineId는 Work world도 실제 해당 line의 Work로 제한한다.

공개 obligations는 실제 ID의 `id`와 저장된 `valid`의 `current`를 추가한다.
current는 OPEN과 같다는 새 주장이 아니다. status·원래 kind·nextAction과
과거 책임 행은 바꾸지 않았다. U2 current 의미, U4 kind, U5 recall 책임
결손도 관찰용 치환으로 숨기지 않았다.

## 현재 권한과 시간 경계

기존 current ReadAuthorizer와 all-of 규칙을 사용한다. physical row는
getInventory, 판매·구매 계약은 실제 Work/item/place를 가진 getObject,
평가 행은 별도 getAssessment 권한으로 읽는다. 움직임의 양 끝 중 한쪽
권한만 있다고 다른 쪽 ID를 노출하지 않는다. 다른 조직·존재하지 않거나
권한 없는 명시 Work/order line은 fail-closed다.

원 행 recordedAt<=knownAt, 실제 effective/occurred/validFrom<=asOf를
사용한다. 이미 저장된 segment의 retirementRecordedAt이 알려지기 전이면
그 퇴역을 과거 값으로 보간하지 않는다. 현 schema의 allocation 현재 행은
완전한 과거 수정 시점 이력을 제공하지 않는다. 과거 asOf/knownAt에는
그 현재 state를 과거로 복사하지 않고 allocations 구간과 상태·실행량
요약을 생략하며 HISTORICAL_ALLOCATION_STATE_UNAVAILABLE을 반환한다.

WorkReadHandler의 고정 authored condition 표시와 인가된 getAssessment
fallback은 그대로 보존했다. capture·envelope 원문/hash를 쓰거나 바꾸지
않는다. 새 read는 명령·원장·책임·policy·schema를 수정하지 않는다.

## 집중 검증과 남은 경계

새 실제 Spring/PostgreSQL 검사는 기존 현재 시스템의 Fulfillment fixture
helper를 재사용하는 별도 class다. 기존 test 파일·원래 T17 oracle/
fixture/grant/수량·시계를 수정하지 않았고 inherited 전체 tests를 실행하지
않는다. 새 테스트는 publicWorld 접두사로만 선택한다.

- 정상 실제30/인정30, late·expiry 실제20/인정0와 TRANSIT10을 읽는다.
- claim/중복 예약/FEFO·포장 단위 거부에서 API ledger 전후가 같다.
- replacement의 원 REPLACED와 새 EXECUTABLE, hold 해제 후 무부활,
  명시 allocation release 후 RELEASED/실행0을 확인한다.
- 서로 다른 주문·Work·line·LOT/customer/place 관계를 합치지 않는다.
- foreign scope와 철회·다차원 all-of grant를 거부한다.
- 조회 전후 모든 organization-scoped DB 행의 독립 fingerprint가 같다.
- genealogy 결손·서로 다른 단위를 운송0으로 바꾸지 않는 결정적 반례를 둔다.
- 원래 getWork/getInventory의 동일 snapshot·과거 시점, 90/100 진실,
  capture public condition 및 인가 fallback을 함께 확인한다.

고유 23개 검사(실제 Spring PG 17개, unit 6개)가 PASS이며 최종 실패와
skip은0이다. 새로운 원천 결손 반례 뒤 관련 실제 운송 범위 검사도
통과했다. package는 tests를 생략한 별도 빌드로 exit0이며 변경 class
bytes가 실행 JAR 안의 bytes와 일치함을 확인했다.

상세 command/exit/count와 source/class/JAR hash, 실제 SQL cells와 조회
응답, 중간 RED·cleanup은 `.local/t17-public-world-read/handoff.json` 및
같은 directory의 logs/pg artifacts에 기록한다. focused 증거를 native
실제 scenario PASS로 주장하지 않는다. full backend/harness, actual
scenario, native Workflow는 실행하지 않았다.

normalSaleAuthorization은 실행 목적·정책·현재 권한의 고정 판정 근거가
없으므로 생략하고 결손을 반환한다. 물리 배송이나 claim 효과0을 허가
DENIED/ALLOWED로 바꾸지 않는다. 원래 U1 67개 assertion reference를
artifacts에 연결하되 이 한 건과 다중 관계·과거 상태의 결손을 숨기지
않는다. U3 조건/CONFLICT 의미, U4 접수 outcome·책임 종류, U5 새 평가/
recall 생성, U6 원자성 identity, U7 창고 선택은 해당 owner 경계로 남긴다.

root·다른 tree·B 소유 recovery/fault/evaluation correction·observer·
Main/verify·AGENTS·task-status를 수정하지 않는다. 새 agent/Workflow/API
과금·배포·Grok·push는 실행하지 않는다. primary가 통합과 최종 인수를
소유하며 Task/Step은 OPEN이다.

## 후속: 공유 fixture 수명과 기본 discovery

`cb93fb40`의 새 test class가 FulfillmentPostgresTest를 상속하면서
원래 test도 discovery에 포함했고, 자체 AfterAll이 다른 class도 쓰는
static PG/blob을 먼저 종료했다. 새 검사
`publicWorldReadUsesCurrentAllOfAuthorityAndRejectsForeignScope` 다음에
원래 `exactSplitProjectsPermissionsAndDisjointHold`를 같은 JVM에서
선택한 RED는 1 PASS/1 ERROR(exit 1)다. 원래 seed의 TRUNCATE에서
`DataAccessResourceFailure: An I/O error occurred while sending to the
backend`가 발생했다.

새 class를 기존 PublicScopeEligibilityPostgresTest와 같은 composition으로
바꿨다. Spring이 helper를 autowire하고 seed와 saleFixture를 명시적으로
실행하며 각 검사마다 원래 시계로 초기화한다. SecurityContext는 매 검사
뒤 해제한다. 기존 test/helper 본문은 명시적 `fixture.` receiver를 제거하면
byte 단위로 원본과 같고 assertion 호출 60곳을 모두 보존했다.

class 종료에서는 공유 PG/blob을 건드리지 않는다. PG 종료는 원래
Testcontainers가 맡는다. JVM 종료 hook은 그때 존재하는 blob tree의
부모부터 deleteOnExit에 등록한다. Java21 JVM의 파일 삭제 단계에서
자식부터 지워지며, 이미 없는 파일과 중복 등록은 무해하다. 기존
PublicScopeEligibilityPostgresTest의 종료 hook은 먼저 같은 tree를 지울
수 있다. 새 hook은 그 class의 삭제와 경합해 직접 파일을 지우지 않는다.

집중 실행은 기본 동일 JVM 설정을 유지했다. 아래 정순·역순은 검증
명령의 선택이며 suite 설정·fork·skip·disabled를 바꾸지 않았다.

| 실행 | 결과 |
|---|---|
| 수정 전 새 검사→원래 검사, reversealphabetical | 1 PASS/1 ERROR, exit 1 |
| 수정 후 같은 두 검사, reversealphabetical | 2 PASS, exit 0 |
| 원래·PublicScope·새 검사, alphabetical | 3 PASS, exit 0 |
| 새·PublicScope·원래 검사, reversealphabetical | 3 PASS, exit 0 |
| PublicWorldReadPostgresTest 기본 discovery, # 필터 없음 | 10 PASS, exit 0 |

각 세 class 실행의 PublicScope 검사는
`allOfScopesIncludeEveryReturnedTargetAndOptionalWork`다. 정확한 명령,
JVM PID·선택 순서·container ID·blob 절대 경로, source/class/log hash,
XML과 cleanup 결과는 `.local/t17-public-world-read-lifecycle/`에 남긴다.
RED의 blob 절대 경로는 원래 cleanup이 출력하지 않아 독립 기록이 없고,
알 수 없는 임시 디렉터리를 추측해 정리하지 않는다. 제품 source,
원래 tests/oracle/grant는 수정하지 않는다. 전체 backend와 native actual은
NOT_RUN이며 primary가 통합 인수를 소유한다. Task/Step은 OPEN이다.
