# 2026-10-24 observer 수정 인계

Task와 Step, S5/S6 gate는 OPEN이다. baseline은 b4772eb6이고 제품
source baseline은 f8bb11e9다. 작업 tree는
`/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-967b0ec4`다.
runtime은 T3 configuration으로 codex_2 / gpt-6.1-sol / xhigh /
serviceTier=priority를 확인했다. actual 시나리오는 Haiku 소유다.

## T11 writer가 사용할 원천 범위 계약

`contributions`의 실제 WorkContributions 행에 다음 관찰 필드를 붙인다.
원래 `id`, `targetWorkId`, `occurrenceId`, `startQuantity`, `quantity`,
`unit`, source/target Goal pin은 변경하지 않는다.

- `workId`는 저장된 `targetWorkId`다.
- `physicalRange.physicalScopeId`는 같은 조직의 실제
  CanonicalOccurrences.physicalScopeId다.
- `physicalRange.occurrenceId`는 실제 WorkContributions.occurrenceId다.
- `physicalRange.startQuantity`와 `endQuantity`는 실제 start와
  start+quantity다. decimal 문자열로 내며 끝점은 제외한다.
- `physicalRange.unit`은 canonical occurrence와 일치한 실제 unit이다.

knownAt 안의 canonical occurrence 하나에 연결되고 범위가 음수가
아니며 quantity>0, end<=canonical quantity일 때만 physicalRange를 낸다.
원천에 없는 serial-boxes 이름은 만들지 않는다. 원래 T11의30+30은
`workId/startQuantity/quantity` tuple의 `[parent-one,0,30]`,
`[parent-two,30,30]`와 동일 occurrence/physicalScope 관계로 검증할 수
있다. T11 case/generator 변경은 T11 writer가 소유한다.

## T17 writer와의 경계

T17의92 FAIL 전체를 원본 보고서의 assertion별로 확인했다.
제품 요구 초안은 `.local/observer-fix-wave/product-requirements.md`와
공유 root `.local/resume-20261024/observer-product-requirements.md`에 있다.
sales writer가 T17 입력·case·producer를 소유한다. observer는 실제
source entity와 Work/ITEM/시간 관계에 따라 관찰한다. 일반 Claims를
전역에서 줄이거나 원천 없는 조건·evaluator·상태를 만들지 않는다.

검증 명령·결과·수정 selector와 잔여 범위는 완료 전 아래에 기록한다.

## T09 관찰 selector의 수정 근거

원본 frozen run의8 FAIL은 세 원인으로 나뉜다.

1. 누적60+40 수령은 실제 Receipts에 있고 WorkContributions는
   WorkLink에 배분된 별개 원천이다. 원래 두 assertion의 관찰 pointer를
   receipts로, occurrence field를 실제 canonicalOccurrenceId로 바꾼다.
   같은 cumulative Work filter,100 BOX와 두 occurrence ID 기대는
   유지한다. ReceiptCredits의 기여0/초과100도 그대로 둔다.
2. THROUGHOUT의 count0은 설치한 INITIAL_BALANCE까지 센 결과다.
   movement-baseline을 설치 직후, 모든 업무 action 전에 독립 DB에서
   읽는다. 같은 scope/asOf/knownAt의 after-db와 countAdded expected0으로
   대조한다. baseline ID의 전체 원행은 그대로 남아야 하며 새 ID 수가0이어야
   한다. 행 삭제·변조·교체·중복·누락 ID도 실패다. 전역 kind 제외는 없다.
3. 나머지5 FAIL은 최종 평가의 원천 관계가 없어서 발생하지 않았다.
   실제 snapshot에는 segmentId/movementId가 올바르게 있었다.
   같은 ITEM의 다른 판매 Work가 남긴 UNASSESSED marker의 누락
   assessmentId 때문에 엄격한 filter가 중단됐다. marker는 실제 absence를
   null로 내며, 원래 누락 column의 실패 규칙은 바꾸지 않는다.

수정 전후의 세 assertion 전체와 원본 artifact SHA-256은
`verification/cases/T09/observer-selector-corrections.json`에 있다.
새 생성기 layer는 inverse와 전체 round trip을 검증한다. 이전 source와
sale-source generator도 재현하며, 기대값·단위·oracleRef·ID는 보존한다.
모든 기존 invoke/query/control 입력, business clock, fixture와 grant는
변경하지 않는다. 새 movement-baseline은 시스템 관찰 단계다.

## T17 관찰 entity와 판정 연결

새 logical source `deliveryObservations`는 실제 sales Observations를
읽는다. 일반 evidence Claims는 그대로다. pending report는 보이는
Delivery가 없을 때 canonical=false/PENDING_RECONCILIATION으로 읽는다.
정상 확정은 정확히 하나의 Delivery와 기존 canonicalDelivery의 실제
claim/event/document/blob/profile/inbox 검증 chain을 요구한다. 증거 무효나
경합하는 두 Delivery는 canonical/status를 추정하지 않는다. 원행의 ID와
reported 범위·수량을 유지하며 판매 적격성과 정상 이행량을 만들지 않는다.

assessments의 goalKind=DELIVERY는 실제 같은 조직의 Goal/Work와
endpoint=DELIVERED를 대조한다. current는 현재 Goal pin과 유일한
assessment chain head를 대조한다. asOf나 원천 binding이 없거나 head가
모호하면 current를 추정하지 않는다. 기존 outcome/조건/수량은 바꾸지 않는다.

기존 generic Claims의 LOT scope는 subject를 segment로 연결하는 한계가
있다. DISPATCH subject의 일반 claim을 LOT로 관찰할 수 있다고 주장하지
않는다. 이번 report source는 직접 LOT를 가진 sales Observations이며
T17 원본 관찰 scope는 ITEM이다. 집중 canonical 테스트의 일반 Claims
대조는 실제 직접 binding이 있는 ITEM/Work scope로 수행한다. 이 한계는
새 report source로 일반 Claims를 숨겨서 해결하지 않는다.

## 집중 검사와 실제 실행 경계

Java21.0.12.1, Maven wrapper3.9.16, PostgreSQL18.6을 사용했다.
backend node_modules는 같은 package-lock의 기존 로컬 cache를 APFS로
복사했고 이 checkout의 CDS에서 CSN을 생성했다. 다른 source의 JAR는
복사·실행하지 않았다. 제품 backend 코드는 변경하지 않았다.

정확한 명령·결과·source/class/fixture hash·DB container cleanup은
`.local/observer-fix-wave/handoff.json`과 같은 directory의 log에 있다.

- `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw -B -ntp -o
  -f verification/harness/pom.xml
  -Dtest=AssertionEngineTest,ScenarioObservationProjectionTest test`는
  22 tests, failure0/error0/skip0, exit0이다.
- `python3 -m unittest discover -s verification/coverage
  -p 'test_coverage.py'`는69 tests, exit0이다. countAdded의 baseline
  전체 원행·추가 수를 coverage가 다시 계산하며 삭제·변조·중복·누락을
  거부하는 반례를 포함한다. coverage runtime PASS 주장이 아니다.
- `python3 -m unittest discover -s verification/cases/T09
  -p 'test_*.py'`는33 tests, exit0이다. 그 뒤 새 ledger 대조 한 건을
  추가해 `-p 'test_observer_selectors.py'`의4 tests를 exit0으로 확인했다.
  기존30개와 새4개가 통과했고34개를 한 번에 실행했다고 기록하지 않는다.
- 실제 PostgreSQL의 새 ObserverWavePostgresTest11개는 exit0이다.
  범위30+30, canonical 단위/조직/knownAt/overflow 반례, receipt60+40,
  opening balance와 같은 시각 새 DISPATCH, marker null/누락 실패,
  실제 delivery goal/head 판정 연결을 검사했다.
- T17CanonicalDeliveryProjectionPostgresTest의 새 parameterized
  `deliveryReportUsesItsActualCanonicalProofAndNeverBorrowsGeneralClaimState`
  2개는 exit0이다. 실제 blob 원문을 가진 정상 canonical source와
  증거 무효·경합 Delivery의 미확정, 일반 Claims 보존을 검사했다.
- 기존 SharedCaptureObservationPostgresTest의66개와
  ObservationSourceRelationsPostgresTest37개는 통과했다. 선택적 과거
  HTTP artifact replay 한 건은 입력 부재로 skip이며 NOT_RUN이다.
  새 test의 LOT subject fixture 오류 한 건을 포함한 첫 실행 로그를
  보존했다. source factory를 바로잡은 후 해당11개만 다시 실행했다.
- assessment head의 유일성 검사를 보강한 후 그 새 PostgreSQL 테스트
  한 건을 다시 통과했다. canonical report의 일반 Claims 대조 scope
  교정 전 실패 두 건도 attempt3 log에 보존했다. 수정 후 그 두 건만
  다시 실행해 통과했다.
- 최종 `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./verify prepare`는
  PREPARED, preparationProblems0, exit0이다. 첫 Gherkin 역할 표기
  오류와 수정 로그도 남겼다. 준비는 제품 실행 인수가 아니다.
- `git diff --check`는 exit0이다. 모든 disposable PostgreSQL container는
  종료·삭제했고 cleanup exit0이다.

실제 source 관계는 PG에서 CSN-shaped 독립 table로 검증했다. 전체
제품 DB migration/서비스 transaction·actual 시나리오·실모델·BTP는 이번
worker가 실행하지 않았다. `./verify scenarios --actual`은 NOT_RUN이며
통합 뒤 Haiku native dynamic Workflow가 원본461 IDs로 실행한다.

## 보존과 coordinator 인계

T09 94, T11 110, T17 257의 총461개 원본 assertion ID·순서·expected·unit·
oracleRef는 baseline과 같다. 세 case의 모든 업무 fixture는 byte가 같다.
T11/T17 case와 generator도 이 branch에서 변경하지 않았다.
shared task-status/decisions와 사용자의 untracked 디렉터리는 수정하지
않았다. 새 countAdded는 단일 no-change 관찰에만 쓰며 전역 누락 필드나
marker 생략 규칙을 만들지 않았다.

T11 writer는 위 physicalRange와 실제 좌표를 사용해 serial label selector를
교정해야 한다. T17 product/case와 현재 권한·조사 원천·intake owner 계약은
sales/lifecycle/coordinator의 작업이다. 기존 generic Claims의 LOT 한계와
sales 진행 중 W scope의 observer 미지원·같은 응답 inTransit60 영향은
공유 요구 보고서에 명시했다. W40은 실제 warehouseHeldQuantity field로
연결하거나 별도 관찰을 사용해야 하며 W source에 TRANSIT row를 넣지 않는다.
실제 적격 판정은 제품 evaluator의 authenticated response와 독립 원천을
대조하며 observer에 새 evaluator를 복제하지 않는다.
