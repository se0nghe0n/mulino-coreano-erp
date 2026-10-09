# Step 2 재검토 11라운드: closure review 8의 P1 두 건

Step 2 closure review 8은 Claude Opus xhigh(`step2-closure-8-opus.json`, NF1–NF9)와
GPT-6-Astra low(`step2-closure-8-astra.json`) 모두 FAIL이었다. 2026-10-09 사용자
결정(AGENTS.md)에 따라 정적 adversarial review를 멈추고, 이 round는 P1 두 건만
닫는다. 하나는 시계와 fixture 기록 시각(NF1)이고, 다른 하나는 예약 이중 계상(NF2)이다.
나머지 지적은 아래 backlog로 넘긴다. backlog는 Step 3 test adapter와 fixture
installer가 생긴 뒤 시나리오를 실제 제품에 실행하는 round가 다룬다.

기준 commit은 `22c7a7da`(`b97f0964`에서 AGENTS.md 결정을 fast-forward)다.
branch는 `step2r/round11`, worktree는
`/Volumes/VideoStore/Developer/mulino-ontology-step2r-round11`다. 작업 모델은
사용자 Step 2 지정대로 Claude Opus high다. backend와 FixtureInstaller는 읽기만
했다. `backend/**`, `verification/actual/**`,
`verification/harness/src/main/java/org/mulino/verification/actual/**`(Step 3),
`.agents/skills/**`(Step 1)는 바꾸지 않았다.

## 항목별 처리

| # | 지적 | 판정 | 내용·증거 |
|---|---|---|---|
| NF1 (P1) | round 10 계약은 제품 시계를 fixture asOf에서 시작하지만 기존 installer는 설치 행을 fixture knownAt에 기록한다. 제품은 `recordedAt` ≤ 시계인 행만 읽으므로(`InventoryRepository.rows/object`, `TradeEvidence.visible`) asOf 09:00:00·knownAt 09:00:01 fixture와 02:00·04:00 fixture에서 명령이 자기가 읽을 사실보다 먼저 실행된다 | FIXED (계약·규칙), installer는 Step 3 요청 | review의 첫 안을 고정했다(병행 Step 3 installer 작업과 같은 안). `contracts/execution-preconditions.json` 1.1.0 `clock.installation`: 모든 설치 행은 시작 시계 asOf 이하에 기록하고, fixture knownAt은 시각을 지정하지 않은 조회의 기본 인지 시각일 뿐이다. 근거의 선언 recordedAt이 knownAt 이하이면 설치 지식으로 asOf에 기록하고, 이런 근거는 asOf 뒤에 발생하지 않는다. knownAt보다 늦으면 늦게 알려진 사실이다. 그 시각에 기록하며, 적용을 기대하고 그것을 지명하는 action은 그 시각 이후에 실행한다. fixture `pickedAt` ≤ asOf다. 규칙 `ContractValidator.fixtureRecordTimeProblems`(prepare 연결)는 설치 이전 시계의 COMMAND·RECORD, 설치할 수 없는 근거, 기록 전에 늦은 근거를 지명하는 적용 기대 action을 거부한다. `pickBeforeDispatchProblems`의 pickedAt 상한은 knownAt에서 asOf로 강화했다. 이 규칙이 새로 찾은 위반은 하나다. T17 `post-dispatch-expiry/delivery`가 시계 09:06:00에 원천 근거 `delivery-20`(기록 09:06:01)을 지명했다. 근거 기록 시각을 발생 시각 09:06:00으로 맞췄다. 시계를 옮기면 knownAt 09:06:00의 `after-delivery` 조회가 인도를 못 보므로 그 방법은 쓰지 않았다. C2·T09의 `DOC` 세 건은 NF8 backlog(`knownOpen`, 아래)다. 회귀 `StepTwoRoundElevenRegressionTest.installedFactsAreRecordedNoLaterThanTheStartingClock` |
| NF2 (P1) | C3·V4가 함께 쓰는 `fixture-reserveQuantity.json`의 `ALLOCATION`(EXECUTABLE, A20 20 BOX, SALE-LINE 전부) 때문에 인가된 예약 대조 호출이 같은 실물과 line을 다시 예약한다. V2 `reserve-commits-first` winner는 좌표 없는 ALLOC40과 겹친다. 올바른 제품은 거부하고 이중 예약하는 제품은 통과한다 | FIXED | 예약 fixture에서 `ALLOCATION` priorEntity를 뺐다(이 fixture의 subcase는 그것을 읽지 않는다). V2는 `ALLOC`에 `startQuantity` "0"을 주고, `winner-call`은 `startQuantity` 40으로 A60의 [40,60)을 ORDER2(20)에 예약한다. 뒤의 `explicit-fresh-split`이 ALLOC을 child0 [0,40)에, winner를 child1 [40,60)에 옮기는 순서와 맞다. 계약 `reserveCapacity`, 규칙 `reserveCapacityProblems`(prepare 연결)는 적용을 기대하는 reserve·replace를 fixture 배분과 subcase에서 앞서 만든 배분(해제·교체·출고 반영)에 대조한다. 같은 segment의 EXECUTABLE·SUSPENDED 배분과 겹치면(좌표 없음은 segment 전체), segment를 넘으면, line 주문량에서 배분(EXECUTABLE·SUSPENDED·CONSUMED)을 뺀 나머지를 넘으면 거부한다. batch `operations`와 blob `businessAction`도 본다. 수정 전 C3 3, V4 8, V2 1, 모두 12개 subcase가 걸렸고(review 수와 같다) 지금은 0이다. 회귀 `reservesDoNotDoubleBookFixtureOrRuntimeAllocations`: C3·V4 배분 복원, SUSPENDED(겹침)·CONSUMED(line만), V2 좌표 제거·line ORDER·segment 초과, T17 실행 중 두 번째 예약을 APPLIED로 바꾼 모양을 거부한다 |

## 바꾼 action의 검사 사슬 재점검

round 10 방식으로 바꾼 action만 다시 대조했다. 표와 분류는
[precondition-audit.md](../step2r-round10/precondition-audit.md)의 "round 11 재점검"에
있다. 요약은 아래와 같다.

- 설치 시각: 시계를 asOf 이전으로 되돌린 뒤 실행하는 COMMAND·RECORD는 0개다.
  knownAt 이하로 선언된 근거 1448개는 모두 asOf 이전에 발생한다. knownAt보다
  늦은 근거는 4개(C2·T09 `DOC` 3, T17 `delivery-20`)이고, T17만 고쳤다. fixture
  `pickedAt` > asOf는 0개다.
- C3 api·mcp·worker와 V4 8개 경로의 인가된 예약: 구간 [0,20)이 A20(20) 안에 있다.
  겹치는 배분은 0이고 SALE-LINE 20 중 0이 차 있다. 인가(delegator: item P,
  work WORK, place W)와 창고 보관(W, warehouse)은 기존 규칙으로 맞다. slot 투영,
  line 설치, SELL 근거, segment revision은 adapter·installer 몫이다(아래 요청).
- V2 winner: [40,60)은 ALLOC [0,40)과 겹치지 않고 ORDER2 20 중 0이 차 있다.
  인가 scope(segment A60, work S2, place W)와 위임자 supervisor는 규칙상 맞다.
- T17 인도: `delivery-20`이 인도 시계 09:06:00에 보인다. 발생 시각은 시계
  이하이고 출고 09:00:00 이후다.
- 새로 찾은 검사는 prepare 규칙 두 개로 옮겼다. installer 쪽 요구(설치 시각,
  배분 좌표, segment revision, 판매 line 설치)는 Step 3 요청으로 남겼다.

## backlog (다음 실행 기반 round)

closure review 8의 나머지 지적이다. 고치지 않았다.

- NF3 (P2): C3 같은 입력 대조 호출 중 split(자식 목록 없음),
  adjust(direction·stocktake 없음), merge(두 parent revision·같은 controlScope
  없음)는 올바른 제품에서 적용될 수 없다. C3
  `{api,mcp,worker}-{splitQuantity,adjustQuantity,mergeQuantity}`.
- NF4 (P2): 출고 뒤 품목 수준 `heldQuantity` 조회에 `placeId`가 없어 TRANSIT
  leaf까지 합산된다. C2·T09 `cumulative-versus-state` `api-held40`, T09
  `final-held0`, T17 `reserve-pick-dispatch` `warehouse-after-dispatch-12`, E1
  `W-held`(mcp·anchor 변형 포함).
- NF5 (P2): round 10이 supervisor에 위임 action을 준 뒤 위임자 FORBIDDEN에 가려졌던
  반례 약 50개를 다시 도출하지 않았다. T11 `unsupported-evaluator`(HELD
  VERSION_UNSUPPORTED여야 한다), T11 `parent90-{cancelled,impossible,superseded}`
  false-close, C5·T09·T10·T11·T22·T24·T26의 closeWork·activateWork·resumeWork·
  transferObligation·resolveObligation·retrySafeCommand 반례.
- NF6 (P3), Astra P2(T13): 적용을 기대하는 출고 중 DISPATCH 적격 근거가 없는
  fixture가 있다(SELL만 있음). T05 `manager-disposition`, V3 `dispatch-first`,
  T13 `partial-excess-return-relocation`, 그리고 C2·T09·T11·C3·V7(판매나 action
  없는 조건만 있음).
- NF7 (P3), Astra P2(T06): T06 `inconsistent-after-dispatch/dispatch-pending`은
  소비된 배분이라 STALE_REVISION으로만 거부된다. PENDING 정정 중의 실행 차단(계획
  :132)은 corpus 어디서도 검증되지 않는다. oracleExplanation도 틀렸다.
- NF8 (P3): C2·T09 시계 이동 뒤 `DOC`(발생 10-07T00:00, 기록 10-07T04:00)가
  시작 시계보다 늦게 기록된다. C2 `cumulative-versus-state`, T09
  `cumulative-versus-state`·`exists-versus-end-throughout`. 지금은 계약
  `clock.installation.knownOpen`에 이름이 있고 prepare `knownOpenGaps`(check
  `fixture-record-time`, 21줄)로 보인다. 고친 뒤 항목을 지우지 않으면 prepare
  문제가 된다.
- NF9 (P3): `grantAuthorityProblems`가 위임자 교집합을 fixture 수준에서 금지하므로,
  위임자 자신의 권한을 철회·만료한 뒤 위임받은 자가 행동하는 실행 반례가 없다.
  T08·V7·V6·T26의 철회는 위임받은 자의 grant만 대상이다.
- Astra P3: `prePickReason`(ContractValidator)은 대상·결과·복원과 관계없이 앞선
  revokeGrant·revokeCapability 아무것이나 pick 전 FORBIDDEN 이유로 받는다. 지금
  corpus에서 걸리는 action은 없다(잠재 결함).

## commit

- `a1106dee` fix(검증): 설치 사실의 기록 시각과 예약 이중 계상을 prepare 규칙으로 막는다
- 이 README: docs commit

## 실행한 checks

환경은 Java 21(zulu), Python 3.14, `env.sh`다. Maven·verify는
`$MULINO_SLOT`으로 하나씩 실행했다. 사용자 지시대로 focused checks만 돌렸고,
전체 집합은 coordinator가 통합 때 실행한다. 아래는 clean tree `a1106dee`의 값이다.

| 명령 | exit | 결과 |
|---|---|---|
| `./verify harness` | 0 | 530 tests, failure/error/skip 0(기준 528 + `StepTwoRoundElevenRegressionTest` 2) |
| `./verify prepare` | 0 | PREPARED, codeCommit `a1106dee`, workingTreeDirty=false, 41/802/24034, preparationProblems 0, caseAssetChecks 11개 모두 PASS(generators-reproduce·case-generators-reproduce·cases-b-invariants·v4-observation-bindings 포함), knownOpenGaps 55(vocabulary 34 + fixture-record-time 21: C2·T09 `DOC` NF8 backlog), runtimeGates 2(regulatory, SCHEDULER_CYCLE_RECORD) |
| `./verify contract-red verification/cases/{C3,V2,V4,T17}/case.json` | 1 | expected=discovered=started=failed=NOT_IMPLEMENTED=420, skipped 0 |
| `python3 -I docs/execution/step2r-round11/author_round11.py` 재실행 | 0 | drift 0(멱등) |
| `python3 -I docs/execution/step2r-round10/author_round10.py` 재실행 | 0 | drift 0(round 10 작성과 충돌 없음) |
| `python3 -I verification/cases/V2/cases_b_invariants.py` | 0 | 문제 0(E1, E2, C4, V2, V3) |
| `python3 -I verification/cases/check_vocabulary.py --check` | 0 | VALID, problems 0, knownOpen 34 |
| `python3 -I verification/requirements/check_derived_bindings.py` | 0 | VALID, 5 files, problems 0 |
| `bind_observations.py V4 --check`·`V4/author_review_fixes.py --check` | 0 | CURRENT |
| Maven focused `StepTwoRoundEleven·Ten·NineRegressionTest`(commit 전) | 0 | 13 tests 통과 |

실행하지 않은 것: `./verify coverage`, coverage·requirements·mcp-tests Python
unittest 전체, model-binding, 다른 case의 contract-red(coordinator 통합 몫).
`--actual`·제품 runtime, `./verify regulatory`도 실행하지 않았다. 설치 시각과
예약 용량은 계약과 selftest까지만 확인했고, 제품 인수는 `NOT_RUN`이다.

## cross-owner 요청

- Step 3 FixtureInstaller(`verification/actual/**`):
  - 모든 설치 행의 `createdAt`·`recordedAt`을 시작 시계(fixture asOf) 이하로
    둔다. 지금 `seedTemporal`, ManufacturingLots, QuantitySegments,
    QuantityMovements, RegulatoryPolicies는 recordedAt에 fixture knownAt을 쓴다.
    fixture knownAt은 조회 기본 인지 시각으로만 쓴다.
  - 근거: 선언 recordedAt이 fixture knownAt 이하이면 asOf에 기록하고, 더 늦으면
    선언 시각에 기록한다(늦게 알려진 사실). 선언 시각보다 앞당기거나 미루지 않는다.
  - fixture 배분의 `startQuantity`를 선언대로 설치한다(V2 `ALLOC` 0). 선언이
    없으면 좌표 없음(제품은 segment 전체와 겹친다고 본다)이다.
  - QuantitySegment의 `revision`을 설치한다(alias `revision`, 없으면
    `entityRevisions.initial`, 없으면 1). 지금 insert는 revision을 비워 두고,
    V4는 literal `expectedRevision` 1을 보낸다.
  - SalesOrder·SalesOrderLine을 설치한다(지금 TYPES에 없음). C3 template처럼
    quantity가 없는 line은 fixture의 유일한 SalesOrder에서 quantity·unit·
    destination을 받는다. 품목은 같은 조직의 유일한 TradeItem, 고객은 Customer
    alias, Work는 `baseline.work`다. C3 grant scope가 WORK·P·W로 제한되므로
    이 값이 다르면 예약은 FORBIDDEN이 된다.
- Step 3 actual adapter(`verification/harness/src/main/java/org/mulino/verification/actual/**`):
  - reserveQuantity 요청은 제품 slot 허용 목록 {segmentId, salesLineId,
    startQuantity, quantity, unit}으로 투영한다. segment는 QuantitySegment
    subject 또는 `segmentId`·`sourceSegmentId`에서 읽는다. line은 `orderLineId`·
    `salesLineId`·`saleLineId`·`salesOrderLineId`·`orderId` 순서로 읽는다.
    `startQuantity`는 typed value이고, 없으면 0이다. C3 공통 slot 묶음의
    itemId·destinationId·reason·decreaseKind·allocationId·sourceSegmentIds는
    보내지 않는다. 보내면 'Unsupported fulfillment slot'이 된다.
- Step 1 skill 소유자(GPT-6.1 Sol high):
  `.agents/skills/ontology-scenario-testing/references/repository-harness.md`의
  prepare 설명에 round 11 규칙(설치 시각 `fixtureRecordTimeProblems`, 예약 용량
  `reserveCapacityProblems`, knownOpenGaps check `fixture-record-time`)을 더한다.
  round 8–10 요청도 남아 있다.

## 하지 않은 것

- backlog 항목(NF3–NF9, Astra P3) 수정.
- 제품 실행, `--actual`, installer·adapter·backend 변경(위 요청).
- 시작 시계를 max(asOf, knownAt)로 옮기는 둘째 안은 쓰지 않았다. 병행 Step 3
  installer가 첫 안(설치 행 ≤ asOf)으로 구현 중이라 양쪽을 맞췄다.
- 일회성 audit Python(설치 시각·예약 겹침 survey)은 `/tmp`에서 돌렸고 저장소에
  두지 않았다. 같은 검사는 Java 규칙과 회귀 test에 있다.
