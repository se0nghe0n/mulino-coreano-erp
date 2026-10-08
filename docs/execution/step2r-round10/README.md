# Step 2 재검토 10라운드: closure review 7 처리와 제품 검사 사슬 전수 점검

Step 2 closure review 7(Claude Opus xhigh `step2-closure-7-opus.json`)은
FAIL이었다(GPT-6-Astra low는 통과). round 9가 pick을 채우자 제품
`FulfillmentCommands`의 다음 검사(운송 장소, pick 뒤 출고 시각)가 같은 출고를
막았다. rounds 6–9가 매번 첫 실패 검사만 고치고 다음 리뷰가 그 뒤 검사를
찾았기 때문에, 이번에는 지적 항목과 함께 적용을 기대하는 11개
capability(dispatch·pick·reserve·replace·release·move·split·confirmReceipt·
receiveProvisional·receiveReturn·recordDelivery)의 제품 검사 사슬 전체를
41개 case에 대조했다. 결과는
[precondition-audit.md](precondition-audit.md)에 있다.

기준 commit은 `d5a14d90`, branch는 `step2r/round10`, worktree는
`/Volumes/VideoStore/Developer/mulino-ontology-step2r-round10`다. 작업 모델은
사용자 Step 2 지정대로 Claude Opus high다. backend는 읽기만 했고
`backend/**`, `verification/actual/**`,
`verification/harness/src/main/java/org/mulino/verification/actual/**`(Step 3),
`.agents/skills/**`(Step 1)는 바꾸지 않았다.

## 항목별 처리

| # | 지적 | 판정 | 내용·증거 |
|---|---|---|---|
| P1 | 배분을 지명한 출고가 TRANSIT 장소를 지명하지 않는다. 대부분 fixture에 TRANSIT 장소도 없다. 제품은 pick 뒤 `transitPlaceId`·kind TRANSIT·PLACE 인가로 거부한다 | FIXED | 기준에서 배분을 지명한 출고 65개 중 40개(C1, C2, C3×6, T04×4, T05, T08, T09×2, T11, T13, T16×2, T24×2, T26×9, V3×3, V7×6)가 장소를 지명하지 않았다. 지금은 T06을 포함한 66개 모두 corpus slot `cargoPlaceId`(C4·E1·E2·T17·T18 관례)로 fixture `TRANSIT` 장소를 지명한다. 장소가 없던 fixture에 TRANSIT(`출고 운송 구간`)을 더하고, grant가 장소를 나열하는 출고 actor와 그 위임자의 scope에 넣었다. 계약 `fixture-place-kinds.json` 1.4.0 `dispatchTransit`, 규칙 `ContractValidator.dispatchTransitProblems`(prepare 연결). 회귀 `dispatchNamesATransitPlaceInsideTheDispatchersPlaceScope`: T05·T13·C3·T24·T26에서 장소를 빼면, W2(INTERNAL_STORAGE)를 지명하면, ordinary나 C3 delegator의 장소 scope에서 TRANSIT을 빼면 거부한다. FORBIDDEN 반례(C3 reader)는 scope를 요구하지 않는다. 제품 slot `transitPlaceId`로 옮기는 일은 Step 3 adapter 요청이다 |
| P2 | T26 `grant_pick`이 pickQuantity를 grant.actions에만 넣고 grant.scope.capabilityIds에는 넣지 않는다 | FIXED | 생성기 `T26/author_review_fixes.py` `grant_pick`이 capabilityIds에도 넣고 재생성했다(9개 만료 fixture). `grantAuthorityProblems`가 모든 fixture actor에서 capabilityIds ⊇ grant.actions를 요구한다. 회귀 `grantsAreConsistentAndCoverTheActionsTheyAuthorize`: lot·grant 만료와 autonomous-loop fixture에서 capabilityIds의 pickQuantity를 빼면 거부한다 |
| P2 | pick보다 앞선 날짜의 출고(C2·T09 `cumulative-versus-state`, T09 `exists-versus-end-throughout`, T11 `cancel-after-shipment`) | FIXED | 시계 모델을 계약으로 정했다(`execution-preconditions.json` clock·occurrence): 제품 시계는 fixture clock asOf에서 시작해 clock control로만 움직이고, 발생 시각은 명시 occurredAt, 없으면 요청 asOf, 없으면 시계다. C2·T09 `cumulative-versus-state`는 fixture clock을 2026-10-05T09:00으로 옮기고 dispatch60 앞(10-06T09:00), receive40 앞(10-07T01:30), 조회 앞(10-07T04:00)에 clock control을 넣었다. 출고·수령·조회 시각과 누적100·현재40 계산은 그대로다. T09 `exists-versus-end-throughout`은 clock을 Q의 validFrom 10-07T00:00에서 시작하고 출고 앞(01:00), 조회 앞(04:00)에서 옮겼다. EXISTS SATISFIED·STATE/THROUGHOUT UNSATISFIED 계산이 그대로다. T11은 출고 시각을 단언하지 않아 pick 시각 02:00(fixture asOf)으로 적었다. 규칙 `occurrenceTimeProblems`: 시계보다 뒤의 발생, pick 시계나 fixture `pickedAt`보다 앞선 출고, 출고보다 앞선 인도를 거부한다. 회귀 `dispatchOccursAfterItsPickOnTheProductClock`: 옛 clock의 C2·T09 세 모양, T11 01:00, clock advance 없는 미래 발생, V3 fixture pick 이전 출고, C4 출고 이전 인도를 거부한다 |
| P3 | NO_TASK의 scheduler 주기 기록 의존이 gap·gate로 등록되지 않았다 | FIXED | `Main.runtimeGates`에 `SCHEDULER_CYCLE_RECORD`(profile recovery, NOT_RUN_GATED)를 더했다. 수동 natural-tick watcher의 submissionStatus를 NO_TASK로 고정한 assertion을 case에서 찾아 이름을 남긴다. 지금은 `T26/lot-expiry-autonomous-loop/repeat-no-due-task` 하나다. harness tick 4개는 포함하지 않는다. 회귀 `schedulerCycleDependencyIsANamedRuntimeGate` |
| P3 | `PRE_PICK_DISPATCH_CODES`가 pick 뒤에도 나오는 code를 받고, 배분 없는 출고를 건너뛴다 | FIXED | 상수를 지웠다. pick 없는 반례는 case가 보이는 pick 전 이유가 있을 때만 받는다: FORBIDDEN(actor 아님, role·grant 없음, 출고 시계에 grant 무효, 앞선 철회), VERSION_UNSUPPORTED(다른 정의 버전), STALE_REVISION(앞선 출고·해제·교체가 소비). fixture가 종결로 선언한 배분은 STALE_REVISION 고정이면 pick이 필요 없다. 배분 없는 출고는 batch·worker route나 다른 조직 대상의 FORBIDDEN만 받는다. T06 `dispatch-pending`은 정정 중인 기출고의 소비된 `old-allocation`과 TRANSIT을 지명하는 잘 갖춘 출고로 바꾸고 CONFLICT·STALE_REVISION(`pending-code` 추가)을 고정했다(생성기 `T06/author_contracts.py`). 회귀 `unpickedNegativesShowWhyTheyPrecedeThePick`. round 9 test 두 곳(T05 FORBIDDEN, T26 code만)을 새 규칙에 맞췄다 |

## 전수 점검에서 새로 찾아 고친 것

| 검사 | 위반 | 처리 |
|---|---|---|
| 위임자 권한(`IdentityAuthorization`: 위임 grant는 위임자도 같은 capability·대상에 허가돼야 한다) | fixture actor인 supervisor가 위임한 action을 갖지 않은 fixture 117개(C2, C5, T06, T09–T12, T22, T24, T26, V5). 11개 capability의 적용 기대 action 46개와 고정 반례 출고 8개가 영향을 받았다. T26 grant 만료 반례는 이 결함만으로도 FORBIDDEN이 나와 만료를 가려내지 못했다 | supervisor role·grant에 위임한 action을 모두 더했다. 규칙 `grantAuthorityProblems`(위임자 보유, 행위 actor capability·유효기간·장소 scope) |
| 이동·분할 근거(`InventoryCommands`는 evidenceRef를 요구한다) | T26 safe-retry 원 이동 4개와 restore 분할 3개 | T26 생성기가 synthetic `evidenceRef`를 넣는다. 규칙 `commandBasisProblems` |
| 창고 보관(`requireWarehouse`, moveQuantity) | 위반 0 | 규칙 `warehouseCustodyProblems`(예약의 QuantitySegment subject와 baseline 행 포함) |

## commit

- `4ac0afa2` fix(검증): 출고 운송 장소·pick 뒤 발생 시각·위임 권한 사슬을 고정한다
- 이 README와 precondition-audit.md: docs commit

## 실행한 checks

환경은 Java 21(zulu), Python 3.14, `env.sh`다. Maven·verify는
`$MULINO_SLOT`으로 하나씩 실행했다. 아래는 `4ac0afa2`의 clean tree 값이다.

| 명령 | exit | 결과 |
|---|---|---|
| `./verify harness` | 0 | 528 tests, failure/error/skip 0(기준 522 + `StepTwoRoundTenRegressionTest` 6) |
| `./verify prepare` | 0 | PREPARED, codeCommit `4ac0afa2`, workingTreeDirty=false, 41/802/24034(기준 24033 + T06 `pending-code`), preparationProblems 0, caseAssetChecks 11개 PASS(generators-reproduce·case-generators-reproduce 포함), knownOpenGaps 34, runtimeGates 2(regulatory, SCHEDULER_CYCLE_RECORD) |
| `./verify coverage` | 2 | NOT_RUN, preparationStatus PREPARED, runtimeStatus NOT_RUN, gateComplete=false, preparationProblems 0, coverageProblems 0, knownOpenGaps 34, caseProfiles 3032, observations 499 |
| `python3 verification/coverage/validate.py` | 0 | VALID, runtimeStatus NOT_RUN, gateComplete=false |
| `./verify contract-red` C1 C2 C3 C5 T04 T05 T06 T08 T09 T10 T11 T12 T13 T16 T22 T24 T26 V3 V5 V7 | 1 | expected=discovered=started=failed=NOT_IMPLEMENTED=493, skipped 0 |
| `python3 -m unittest discover -s verification/coverage -p 'test_*.py'` | 0 | 68 OK(261s), clean tree `4ac0afa2` |
| `python3 -m unittest discover -s verification/requirements -p 'test_*.py'` | 0 | 37 OK(case 생성기 재현 포함) |
| `python3 -m unittest discover -s verification/mcp-tests -p 'test_*.py'` | 0 | 3 OK(T26·C3 post-processor fixed point 포함) |
| `python3 -m unittest discover -s verification/cases -p test_check_vocabulary.py` | 0 | 6 OK |
| `verification/model-binding/run selftest` | 0 | 61 tests OK |
| `verification/model-binding/run prepare` | 0 | PREPARED |
| `python3 -I verification/requirements/validate_catalog.py` | 0 | structure VALID, oracle 122·observation 499·case 41·requirement 26 |
| `python3 -I verification/cases/V2/cases_b_invariants.py` | 0 | 문제 0(E1, E2, C4, V2, V3) |
| `python3 -I verification/cases/check_vocabulary.py --check` | 0 | VALID, problems 0, knownOpen 34 |
| `python3 -I verification/requirements/check_layer_routes.py` | 0 | VALID, unexplained 0, KNOWN_OPEN 0 |
| `python3 -I verification/requirements/check_derived_bindings.py` | 0 | VALID, 5 files, problems 0(T08·V7 caseHash 재생성) |
| `bind_observations.py --check`(T08, V4, V6, V7)·`V4/author_review_fixes.py --check` | 0 | 모두 CURRENT |
| 생성기·작성 script 재실행(round 10 `author_round10.py`, T06 `author_contracts.py`, T26·C3·V4 post-processor, platform-tests `build_cases.py`, mcp-tests `author_cases.py`, round 6–9 작성 script) | 0 | 실행 뒤 `git status` drift 0 |

NOT_RUN: `./verify regulatory`, `--actual`·제품 runtime, 실제 host
adapter·MCP wire·extractor. 운송 장소, 시계·발생 시각, 위임자·grant 규칙,
근거, pick 전 반례 규칙은 계약과 selftest까지만 확인했다. 제품 인수는
`NOT_RUN`이다.

## cross-owner 요청

- Step 3 actual adapter(`verification/harness/src/main/java/org/mulino/verification/actual/**`):
  - 출고 `cargoPlaceId`를 제품 `transitPlaceId`로 옮긴다. 장소를 고르지 않는다.
  - 발생 시각은 명시 `occurredAt`, 없으면 요청 `asOf`, 없으면 제품 시계다
    (출고·이동·분할·인도·반품의 `occurredAt`).
  - 요청 `evidenceRefs`(또는 `evidenceRef`·`evidenceId` 류)를 제품
    `evidenceRef`로 옮긴다. T26 이동·분할은 요청 `evidenceRef` 문자열을 쓴다.
  - 예약의 segment는 slot이 없으면 QuantitySegment subject에서 읽고,
    `startQuantity`가 없으면 0으로 보낸다.
- Step 3 FixtureInstaller(`verification/actual/**`의 설치 경로):
  - 제품 시계를 fixture clock asOf에서 시작하고 clock control(instant, 없으면
    asOf, 없으면 knownAt)로만 옮긴다.
  - fixture 배분의 state(T06 `old-allocation` CONSUMED)와 round 9의
    `pickedAt`·`pickedByAlias`를 설치한다.
  - fixture actor가 아닌 `delegatorAlias`는 위임 범위를 덮는 root grant의
    actor로 설치한다. fixture actor인 위임자는 fixture 그대로다.
  - 차원 제한 grant scope(`placeAliases`·`places`의 TRANSIT 포함, item·segment·
    work 차원)를 설치한다. 지금은 조직 scope만 받는다('Dimension-restricted
    grant fixture mapping pending'). 실행 중 만든 Work의 WORK 차원 규약이
    필요하다.
  - roleCapabilities에만 있는 결정 capability(QC_DECIDE_RESTRICTION 등)를
    PolicyCommandGuard가 읽는 grant action으로, capability별 COMMAND 정책과
    발행 정의 내용을 fixture versions에서 설치한다.
  - round 6–9 요청(장소 kind 기본값, 수령 원본 보관자, 운송 leaf slot 등)은
    그대로 남아 있다.
- Step 3 backend: scheduler·sweeper가 주기 완료 기록(schedulerId, tickId/sweepId,
  startedAt, completedAt, startedBy)을 남긴다. 그 전까지 T26
  `lot-expiry-autonomous-loop/repeat-no-due-task`는 `SCHEDULER_CYCLE_RECORD`
  gate로 NOT_RUN이다.
- Step 1 skill 소유자(GPT-6.1 Sol high):
  `.agents/skills/ontology-scenario-testing/references/repository-harness.md`의
  prepare 설명에 round 10 규칙(출고 운송 장소, 제품 시계·발생 시각, grant
  capabilityIds·위임자·행위 권한·장소 scope, 명령 근거, 창고 보관, pick 전 반례
  이유, SCHEDULER_CYCLE_RECORD gate)을 더한다. round 8·9 요청도 남아 있다.

## 하지 않은 것

- 제품 실행, `--actual`, `./verify regulatory`.
- backend·`verification/actual/**`·`.agents/skills/**` 변경(위 요청).
- 정의 버전 일치(G2)는 audit로만 대조했고 규칙으로 만들지 않았다.
- QualityEligibility 입력, 판매 line, installer 규약(grant scope 어휘, 결정
  capability, 정책·정의 내용)은 강제하지 않았다(precondition-audit.md
  "강제하지 않은 것").
- 일회성 audit Python은 `/tmp`에서 돌렸고 저장소에 두지 않았다. 같은 검사는
  Java 규칙과 회귀 test에 있다.
