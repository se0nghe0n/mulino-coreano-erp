# S5e: 시나리오 adapter 5차 — 2026-10-11 사용자 결정과 남은 제품 surface

s5d 최종 실행(`../s5d-adapter/run-6b9aed36.json`)의 첫 실패 중 사용자가
2026-10-11에 정한 제품 동작 세 가지(UD-1·UD-2·UD-3, AGENTS.md)와 제품 쪽
공백(정의 lifecycle, worker route, model runner, process control, 관찰
원천)을 다룬다. 고친 뒤 실제 제품에 다시 실행해 확인했다(2026-10-09
run-driven 절차).

worktree `mulino-ontology-step3-s5e-adapter`, branch `step3/s5e-adapter`, 기준
`9dfa95db`. 소유: `backend/**`(migration 포함), `contracts/capability-slots.json`,
`contracts/intent.schema.json`, `verification/actual/**`, `verification/harness/**`.
case·fixture(`verification/cases/**`)는 바꾸지 않았다. 소유 밖 파일 두 개를
바꿨다(아래 "소유 밖 변경").

## Commit

| commit | 항목 | 내용 |
|---|---|---|
| `3713c28e` | 1a UD-1 | 판매 주문이 자기 인도 Work를 만들고 가격을 선택 입력으로 둔다(V33) |
| `f2c3378d` | 1b UD-2 | Work 생성이 다음 행동을 받고 활성화가 첫 의무를 연다(V34) |
| `c4e7f8cf` | 1c UD-3 | splitQuantity 응답의 children[].index pointer를 slot 목록에 적는다 |
| `f34cdb1d` | 2 | 정의 package lifecycle 7 capability(V35), schema inventory 갱신 |
| `391b71e8` | 2 | migrateWorkDefinition(V36) |
| `6d2710df` | 2·6 | adapter: 정의 lifecycle convention·DefinitionPackage 설치·관찰 원천·Work nextAction 설치 |
| `702b2b03` | 5 | process start·stop·restart host control |
| `20fbe574` | 6 | PUBLISHED·ACTIVE·RETIRED DefinitionPackage 설치, regressionRuns 원천 |
| `c649ea5c` | 병합 | coordinator 지시로 step2r/round14(40f4a0e4)를 병합한다(충돌 없음) |
| `b7372080` | R14-7 | round 14의 새 case 모양에 맞춰 harness test 8개를 고친다 |
| `c6413e1b` | R14-2 | transit 수령 검사가 split 자식을 quantities index로도 식별한다 |
| `a8430558` | R14-1 | capability slot 검사가 정의 package verb를 명령으로 세지 않는다(+test) |
| `4c5c52fd` | R14-8 | HTTP 500 세 경로(RegulatoryCommands·IdentityCommands NPE, MCP Instant)를 구조화 오류로 |
| `cb3fa60f` | R14-3 | split 응답이 자식으로 옮긴 배분(`allocations[]`)을 돌려준다 |
| `4e9b2e22` | R14-4 | recordDelivery가 소비된 배분 참조(allocationId)를 받아 출고와 대조한다 |
| `d63302d7` | R14-6 | installer가 retirePolicy가 읽는 ACTIVE 정책 draft와 PolicyApprovals를 쓴다 |
| 이 commit | — | 최종 실행 inventory `run-d63302d7.json`과 이 문서 |


## 항목별 상태

| # | 항목 | 상태 | 요약 |
|---|---|---|---|
| 1a | UD-1 createSalesOrder 인도 Work·가격 선택 | DONE | 3713c28e. RED(`Invalid workId`) 뒤 GREEN. workId가 없으면 같은 거래에서 DRAFT 인도 Work(kind DELIVERY, owner=ownerId 또는 요청 human, supervisor=supervisorId 또는 owner, 초안 GoalVersion=line의 수량·단위·끝점·기한·목적지)를 만든다. price·currency는 함께 선택. V33. 응답 `workId`·`orderId`·`line{id,revisionId,orderId}`. 가격 없는 판매 line의 송장 대조는 NEEDS_INPUT SALES_PRICE_UNSET |
| 1b | UD-2 createWork nextAction·초기 의무 | DONE | f2c3378d. RED(`Unsupported slot ... nextAction`) 뒤 GREEN. createWork·createDraft가 nextAction·nextCheckAt을 함께 받고, activateWork(와 createWork의 즉시 활성화)가 Work를 원천으로 한 WORK_NEXT_ACTION 의무를 연다. closeWork가 그 의무를 WORK_CLOSED로 해소한다. V34(Works 두 열, Roots.sourceKind WORK) |
| 1c | UD-3 split 자식 index | DONE(문서) | c4e7f8cf. 제품 변경 없음(52cb9458부터 `children[{index,segmentId,quantity,unit}]`). capability-slots.json에 response 설명 |
| 2 | 정의 lifecycle 8 capability | DONE(slice) | f34cdb1d·391b71e8. 아래 절. 회귀 fixture 실행은 deferred |
| 3 | worker route | 사용자 결정 UD-W | 계획은 외부 worker 제출 endpoint를 정하지 않는다. 아래 절 |
| 4 | model runner | NOT_IMPLEMENTED(S5 실모델) | 아래 절 |
| 5 | process start·stop·restart | DONE(test profile, 단일 JVM) / 사용자 결정 UD-P | 702b2b03. 아래 절. 최종 실행에서 start는 모두 FAILED terminal |
| 6a | 관찰 원천 identity_matches·domain_records | DONE(approximate) | 6d2710df |
| 6b | 관찰 원천 tombstones·deletion_log | 제품 gap | 보존 삭제(계획 §7.4) 미구현. unmapped에 이유 |
| 6c | 관찰 원천 locks·lockWaits | 제품 gap(그대로) | s5c |
| 6d | 정의 관찰 원천과 target-before revision | DONE | 6d2710df. activeDefinitionPointers·definitionApprovals 등, getDefinition `data.revision`·`hash`·`lifecycleState`, DefinitionPackage alias 설치 |


### round 14 Step 3 발견(coordinator 지시, 병합 뒤)

| 발견 | 상태 | 요약 |
|---|---|---|
| 7 harness test 8개 | DONE | b7372080. ./verify harness 534 tests 0 failure |
| 2 transit 수령과 split index | DONE | c6413e1b. `quantities[i]` → `/response/children/<i>/segmentId`. `contracts/fixture-place-kinds.json` splitChildPointer 문구는 소유 밖이라 그대로 |
| 1 정의 package verb false positive | DONE | a8430558. nouns·verbs 배열을 함께 가진 객체(정의 package)는 명령 탐색에서 뺀다. prepare 위반 명령 463(정의 lifecycle capability가 공개돼 C3·V4의 범용 slot 묶음이 새로 위반으로 셈해진다, 결함 15) |
| 8 HTTP 500 | DONE | 4c5c52fd. 절차에 policyId 없음 → HELD POLICY_UNRESOLVED, 철회 대상의 actor·scope 없음 → FORBIDDEN, MCP 결과는 TransportValues로 정규화. 전용 test 없음(시나리오로 확인) |
| 3 split의 이전 배분 | DONE | cb3fa60f. `allocations[{id,revision,childIndex,segmentId,startQuantity,quantity,unit,state,predecessorId}]` |
| 6 정책 draft 상태 | DONE | d63302d7. 활성 정책에 ACTIVE PolicyDrafts 행, 승인자 있는 APPROVED·PUBLISHED draft에 PolicyApprovals |
| 4 계획이 정한 입력 | 부분 | recordDelivery allocationId(계획 §6 "이미 소비된 배분 참조")만 구현했다. 나머지는 아래 UD-S |
| 5 T01 제외 업무 code | 사용자 결정 UD-X | 아래 |

### 정의 lifecycle(항목 2)

2026-10-11 사용자 결정 2에 따라 package wire 형태는 제품이 저장하는
`Definition` record다. `DefinitionLifecycle`(7 capability)과
`WorkLifecycle.migrateWorkDefinition`이 계획 §8의 8 capability를 낸다.

- createDefinitionDraft: `package`(Definition 형태, organizationId 선택,
  id·state·contentHash는 서버가 정함), `version`, `parentDefinitionId`,
  `regressionFixtureRefs`. 미지원 field(sql·script·newExecutionPrivilege 등)와
  고정 집합 밖 predicate operator는 draft에서 TYPE_INVALID다. 반려 뒤 수정은
  `definitionId`+`parentDraftRevision`(= lifecycle revision)으로 같은 package를
  새 hash의 DRAFT로 바꾼다.
- hash는 저장 content의 SHA-256이다. content의 state·contentHash를 비워 두므로
  검토·승인·발행·활성 hash가 같다. lifecycle revision은 별도 행
  (DefinitionLifecycles)이라 발행 뒤 불변 trigger와 충돌하지 않는다.
- validateDefinition: `DefinitionValidator` 결과·부모 대비 diffClasses(RENAME,
  OPTIONAL_ATTRIBUTE_ADDED, REQUIRED_SLOT_CHANGED, UNIT_POLICY_CHANGED,
  ENDPOINT_CHANGED, RELATION_MEANING_CHANGED)를 기록하고 publishable이
  아니어도 APPLIED다(검증 기록 자체가 효과다).
- submitDefinitionReview: DEFINITION(같은 hash의 publishable 검증 필요,
  DRAFT→IN_REVIEW) 또는 WORK_MIGRATION(PUBLISHED package의 전환 검토:
  migrationId·affectedWorkIds·mapping).
- approveDefinition: drafter·submitter가 아닌 human. REJECTED는 DRAFT로
  되돌리고 사유를 요구한다. 승인은 validFrom·validUntil·consumptionPolicy
  (ONE_PACKAGE_PUBLISH | ONE_MIGRATION)를 저장한다.
- publishDefinition: 같은 hash의 유효·미소비 APPROVED 승인을 소비하고
  IN_REVIEW→PUBLISHED. 승인이 없거나 소비·만료면 WAITING_APPROVAL. 발행 직전에
  검증기를 다시 돌린다.
- activateDefinition: 조직당 하나의 ACTIVE pointer를 옮긴다(이전 package로
  rollback 포함). retireDefinition: RETIRED_FOR_NEW_WORK, pointer 제거, 새
  Work 생성은 HELD DEFINITION_RETIRED.
- migrateWorkDefinition: Work의 human owner만, 대상 manifest의
  supportedWorkMigration에 migrationId가 있고 요청 quantity·endpoint가 승인의
  goalChanges와 같을 때. 새 GoalVersion(previousGoalId 유지)·Work의 pinned
  definition 변경(V36 trigger는 같은 전환 행이 있을 때만 허용)·승인 소비·
  WorkDefinitionMigrations 기록이 한 거래다.
- getDefinition: 모든 state를 읽고 `revision`·`hash`·`lifecycleState`를 더한다.

deferred: 회귀 fixture 실행(검증 기록에 fixtureRefs와 `NOT_RUN`으로 남기고
발행을 막지 않는다. 계획 §8은 회귀 결과를 발행 검사에 넣는다), migration
mapping·regression의 별도 행(migrationMappings·migrationRegressions 관찰 원천
없음), NounTypes 등 component table 채우기(제품은 content JSON을 읽는다).

### worker route(항목 3): 사용자 결정 UD-W

계획은 C3("모든 공개/worker 쓰기 경로는 효과0")과 V4("MCP/worker 대체경로")에서
worker 경로를 시험하라고 하지만, 외부에서 부를 수 있는 worker 제출 endpoint의
형태는 정하지 않는다(§7, §10은 server가 claim한 실행만 말한다). 제품의 worker
실행은 `ApplicationCommands.executeClaimed`뿐이고 identity는 영속 claim(lease·
fencing token)에서만 나온다. 74 subcase(C3 worker-* 등)가 `no product surface`다.

- A: test·운영 모두에 인증된 worker 제출 endpoint(예: `POST
  /api/runtime/worker/commands`, body = intent + claim{claimId, fencingToken})를
  두고 `executeClaimed`로 보낸다. claim 없는·위조·만료 claim은 FORBIDDEN, 효과0.
  case는 이 endpoint로 READ grant actor가 쓰기를 못 함을 본다.
- B: 공개 worker 경로가 없다는 것을 계약으로 하고(V4 enumerateWriteSurface가
  WORKER_HANDLER_REGISTRY에서 외부 진입점 0을 관찰) worker route subcase를
  그 관찰로 바꾼다.
- 권고: A. 기존 파이프라인 위의 얇은 endpoint이고, 시험하는 것이 claim fence
  자체다. B는 "경로가 없다"를 보이는 별도 열거 구현이 먼저 필요하다.

### model runner(항목 4)

C3 model-query 30 subcase는 실제 client/model이 질의 문장을 tool 호출로 바꾸는지
본다. 계획 §9.2·§13은 server 계약 test와 실모델 의미 평가를 구별하고 "C3
실모델 인수는 S5", "유료 모델 호출은 비용 범위 승인 후"라고 한다. harness의
SCRIPTED_SIT runner는 의도적으로 이것을 대신하지 않는다(대본 응답은 모델 판단의
증거가 아니다). 그래서 S5 실모델 항목으로 두고 NOT_IMPLEMENTED(`subcase requires
an actual client/model runner`)를 유지한다. 필요한 것: 승인된 비용 범위·계정,
`--agent-runner=actual`의 client binding.

### process start·stop·restart(항목 5): 사용자 결정 UD-P

test profile에서 suite의 backend JVM 하나가 case의 모든 processId와
environmentId를 host한다고 두고(계획 §2 단일 배포 application) 그 JVM을 실제로
끄고 켠다(`process_host.py`, `BackendProcessControls`). host-observation-guide의
processObservation(before/after OS pid, terminal snapshot, 독립 extractor)을 만든다.
restart는 새 pid로, stop은 port가 빌 때까지 확인한다. launch 뒤에는 harness가
마지막 verification clock instant를 다시 건다.

case는 process가 case의 start 전에 꺼져 있고 api·scheduler·worker-a·worker-b·
due-sweeper가 별도 process라고 가정한다. fixture 설치가 실행 중인 backend를
쓰므로 start-app-api부터 FAILED terminal(이미 실행 중)이다.

- A: case가 processId를 한 application의 role로 보고, 첫 start를 그 application
  launch로, 나머지 role start를 같은 instance의 확인으로 바꾼다(guide의 start
  성공 조건도 함께 바꾼다).
- B: 제품이 role별 실행 profile(api만, scheduler만, worker만)을 갖고 harness가
  processId마다 별도 JVM을 띄운다. JVM마다 verification clock을 맞춰야 하고
  T26 25 subcase에 약 120회 JVM 기동이 든다.
- 권고: A. 계획 §2가 단일 배포를 기준으로 하고, V5의 "2worker" 중복효과0은
  같은 DB의 claim fence 문제라 별도 JVM 없이 claim control로 볼 수 있다.

## 실행 비교

모두 `./verify scenarios --actual`(backend package 뒤
`verification/actual/scenarios/run.sh`), 기본 mode(partial fixture, 요청 재작성·
grant 분리 없음), 일회용 PostgreSQL, **기본 heap**(`JAVA_TOOL_OPTIONS` 없음).
inventory는 `verification/actual/scenarios/inventory.py`. 최종 실행 직전 같은
commit(d63302d7)에서 `./verify prepare`가 PREPARED(problems 0)였고 실행 시작 때
작업 트리는 비어 있었다(`working-tree-status.txt` 0 byte, `workingTreeDirty`
false). 실행 시간 17분 49초. 최종 실행 전 `verification/model-binding/run
prepare`로 model-binding preparation 산출물을 만들었다(s5d 실행에는 없어 T25
6이 앞에서 멈췄다).

| 실행 | 실행 action | PASS(모두 부분 fixture) | FAIL/ADAPTER | FAIL/TEST | NOT_IMPL |
|---|---:|---:|---:|---:|---:|
| s5d 기준 `../s5d-adapter/run-6b9aed36.json` | 5,781 | 54 | 565 | 104 | 79 |
| round 14 `../step2r-round14/run-68d8dbd9.json`(옛 backend) | 5,913 | 96 | 519 | 107 | 80 |
| 중간 702b2b03(병합 전, 파일 미보존) | 6,044 | 54 | 562 | 108 | 78 |
| **최종 `run-d63302d7.json`** | **6,226** | **109** | 500 | 114 | 79 |

- s5d 대비: 90 subcase가 더 많은 action을 실행했고 줄어든 것은 0이다. 새 PASS 55
  (C3 40, V4 15), 잃은 PASS 0.
- round 14 대비: 69 subcase가 더 실행했고 줄어든 것 0. 새 PASS 13(C3 9, V4 4),
  잃은 PASS 0. 모두 부분 fixture 위의 PASS이고 runtime 인수가 아니다.
- FAIL/TEST 114의 대부분은 worker route(제품 surface 없음, 84)와 nested·
  projection route(20)다. worker가 74→84로 는 것은 정의 lifecycle subcase가
  기준 조회를 지나 worker 시도에 닿았기 때문이다.

| 첫 실패 묶음 | 6b9aed36 | 68d8dbd9 | d63302d7 |
|---|---:|---:|---:|
| `Invalid workId`(createSalesOrder, UD-1) | 27 | 27 | 0 |
| `Unsupported slot for createWork: nextAction`(UD-2) | 17 | 17 | 0 |
| VERSION_UNSUPPORTED(정의 lifecycle 등) | 20 | 20 | 5 |
| `Supported endpoint is DELIVERED`(결함 17) | 0 | 0 | 18 |
| `Unsupported lifecycle slot`(결함 16) | 0 | 0 | 17 |
| 상류 FORBIDDEN `Unavailable scope` | 77 | 51 | 39 |
| worker route 제품 surface 없음 | 74 | 77 | 84 |
| C3 grantId | 74 | 0 | 0 |
| expected X, observed TYPE_INVALID | 72 | 35 | 46 |
| 권한 있는 대조 호출 commit 0(`count mismatch: 0 != 1`) | 6 | 72 | 70 |
| 관찰 원천 identity_matches | 14 | 14 | 0 |
| 관찰 원천 tombstones·deletion_log | 19 | 19 | 19 |
| process/clientProbe host control 없음 | 13 | 13 | 13 |
| model runner | 30 | 30 | 30 |
| envelope `Unsupported slot for` | 51 | 40 | 23 |

UD-1·UD-2의 첫 실패는 사라지고 그 subcase는 다음 case 결함(끝점 이름 18,
goal 형 workType 17)에서 멈춘다. TYPE_INVALID 35→46의 증가는 정의 lifecycle
명령이 공개돼 C3·V4의 범용 slot 묶음이 FORBIDDEN 전에 envelope에서 거부되는
것이다(결함 15). process start는 모두 FAILED terminal이다(UD-P).

### 남은 첫 실패 상위(d63302d7)

| 건수 | 첫 실패 | 소유 |
|---:|---|---|
| 84 | worker route 제품 surface 없음 | UD-W |
| 70 | 권한 있는 대조 호출 commit 0 | round 14 기록(실체가 필요한 C3 시도) |
| 46 | expected FORBIDDEN, observed TYPE_INVALID | 결함 4·15 |
| 39 | 상류 FORBIDDEN | s5d 결함 6–9, 결함 13 |
| 30 | model runner | S5 실모델 |
| 19 | 관찰 원천 tombstones·deletion_log | 제품 gap(계획 §7.4) |
| 18 | `Supported endpoint is DELIVERED` | 결함 17 / UD-1 보충 |
| 17·17 | 관찰 원천 locks / `Unsupported lifecycle slot` | 제품 gap / 결함 16 |
| 16·9 | GOAL_SLOT_MISSING periodStart·periodStartInclusive | round 13 Step 2 미결 5 |
| 13 | process/clientProbe host control 없음 | host adapter(clientProbe·backup 등) |


## 사용자 결정이 필요한 것

- **UD-X T01 제외 업무의 거부.** 계획 §1은 제외 범위만 말하고, §3.4는 미지원
  capability의 오류로 VERSION_UNSUPPORTED를 들며 결과 목록에 HELD가 없다.
  제품은 HELD VERSION_UNSUPPORTED(vocabulary §8 "미지원은 효과 없이 보류")다.
  T01은 REJECTED CAPABILITY_UNSUPPORTED를 기대한다. 권고: 제품 그대로 두고 case가
  VERSION_UNSUPPORTED를 기대한다(§3.4가 이 code를 정한다). REJECTED/HELD 중 어느
  outcome인지는 계획이 정하지 않으므로 사용자 결정이다.
- **UD-S 계획이 개념만 정한 입력**(round 14 발견 4 나머지). 각각 저장·의미가
  정해져야 slot을 공개할 수 있다: recordExternalReconciliation
  externalOperationId(outboxId와의 대응), reserveQuantity FEFO 예외 사유(저장 열),
  transferObligation 인수 근거·부분 이전 자식 scope(§5.3 분할 규칙의 wire 형태),
  approvePurchase 소비 정책(저장과 허용값), createPolicyDraft 확인자·적용일(R
  gate의 확인 주체), authorizeReturn 대상 실물 범위(physicalScopeId와 원 인도의
  관계), adjustQuantity MANAGER 확인 근거(evidenceRef와의 구별), activatePolicy
  영향 scope·효력시점. 권고: 하나씩 계획 절에 slot 이름·저장을 적고 Step 3가
  구현한다.
- **UD-W worker route**(위 절): 외부 worker 제출 endpoint를 둘지. 권고 A(claim을
  받는 얇은 endpoint).
- **UD-P process 모델**(위 절): case의 processId를 한 application의 role로 볼지,
  제품이 role별 process를 지원할지. 권고 A.
- **UD-2 보충: nextAction 없는 activateWork.** 계획 §5.2는 activateWork가 초기
  의무를 만든다고 한다. 지금은 nextAction·nextCheckAt을 선언한 Work에만 연다.
  필수로 하면 선언 없는 Work의 활성화는 NEEDS_INPUT이 된다(기존 case·Work 다수).
  권고: 필수화는 case가 두 slot을 보낸 뒤에 한다.
- **UD-1 보충: qualityTerms·deliveryEndpoint.** createSalesOrder는 qualityTerms를
  필수로, 인도 끝점은 DELIVERED만 받는다. case 21 요청이 qualityTerms를 빼고 34
  요청이 `CUSTOMER_ACCEPTED`를 보낸다(첫 실패 18). 어느 계약도 끝점 이름을 정하지
  않는다. 선택 A: 제품이 qualityTerms를 선택으로 하고 끝점 어휘에
  CUSTOMER_ACCEPTED를 더한다(인도 평가가 그 끝점을 알아야 한다). 선택 B: case가
  DELIVERED와 qualityTerms를 쓴다. 권고: B(제품의 인도 평가는 DELIVERED만 구현).
- **정의 회귀 fixture 실행.** 계획 §8은 발행 검사에 회귀 결과를 넣는다. 제품은
  선언만 기록하고 NOT_RUN으로 둔다. 회귀를 무엇으로 실행할지(./verify scenarios의
  case 부분 실행? 별도 runner?)가 정해져야 발행을 그 결과로 막을 수 있다.

## Step 2 case 결함(바꾸지 않고 기록)

s5d 결함 1–9는 그대로 열려 있다(slot 계약 위반, request_contract 배열 규칙, C3
grantId, 계획 §3.3 순서의 TYPE_INVALID, T01 excluded-*, RECALL scopeId, T19
OLD-WORK, T20 lot scope, C3 자리표시 alias). 이번에 새로 보인 것:

10. **T21·V1 package의 미구현 capability 계약.** draft package가 resumeWork를
    semanticVersion `sell-eligible-v2`·evaluator `eligibility-v2`로 선언한다. 그
    계약을 구현한 handler가 없어 검증이 UNSUPPORTED_CAPABILITY·
    UNSUPPORTED_RUNTIME_CONTRACT이고 publishable이 아니며, 계획 §8("새 효과는
    개발자가 capability를 구현·배포")대로 발행할 수 없다(T21 4 subcase가 submit에서
    DEFINITION_NOT_PUBLISHABLE). 지원 계약(`1.0.0`/`core-v1`)으로 바꾸거나 v2
    evaluator를 Step 3 범위로 정해야 한다.
11. **T21 validation-constraints의 검증 oracle.** validate가 REJECTED와
    문제별 code(`TYPE_INVALID`, `CARDINALITY_INVALID` 등)를 내면서 동시에
    `/response/data/publishable`을 읽는다. 거부 응답에는 data가 없다. 제품은
    검증을 APPLIED로 기록하고 `data.problems[{path,code}]`·`publishable`을 낸다
    (code는 DefinitionValidator의 것: CARDINALITY, STAGE, UNIT_MISMATCH 등).
12. **T21 getDefinition pin.** 요청이 `definitionVersion: definition-v1`을 pin한 채
    v2 draft id를 읽어 `Definition pin mismatch`(TYPE_INVALID)이고,
    invalid-executable-payloads `before`는 id 없이 getDefinition을 부른다.
13. **T21 grant scope.** explicit-authorized-migration·pinned-meaning의 fde grant가
    items를 scope로 둔다. 정의 명령에는 ITEM 차원이 없어 all-of(2026-10-09)로
    FORBIDDEN이다.
14. **T21 draft-organization.** 다른 조직 organizationId의 package는 draft에서
    TYPE_INVALID다(case는 validate에서 ORGANIZATION_SCOPE_INVALID를 기대).
15. **C3·V4 정의 명령의 범용 slot 묶음.** 6 capability에 definitionVersion·
    newDefinitionVersion·workId·reason·regressionArtifact·typedDefinition을 보내
    envelope가 FORBIDDEN 전에 TYPE_INVALID다(prepare: C3 51·V4 16 위반 명령).
16. **createWork goal 형의 workType.** V5 4·T26 13이 goal 형에 `workType`을 함께
    보내 `Unsupported lifecycle slot`이다. goal 형의 업무 유형은 `kind`다
    (capability-slots.json forms.goal).
17. **판매 끝점·품질 조건.** 위 UD-1 보충.
18. **process lifecycle 가정.** 위 UD-P.
19. **C3 DefinitionPackage 저작 hash.** fixture가 정한 hash는 installer의
    convention content로 재현되지 않는다(notRepresented).

## 소유 밖 변경

- `contracts/domain-vocabulary.json`: 제품이 내는 obligation kind
  WORK_NEXT_ACTION과 code SALES_PRICE_UNSET·DEFINITION_HASH_CHANGED·
  DEFINITION_APPROVAL_REQUIRED·DEFINITION_NOT_PUBLISHABLE·DEFINITION_RETIRED.
  DomainVocabularyContractTest가 제품 literal과 이 파일이 같기를 요구한다.
- `docs/execution/s4-integration/schema-compatibility.json`: S1Read의 CDS/Flyway
  전수 비교 inventory(V33–V35 71 column). 3713c28e·f2c3378d는 이 갱신 없이
  commit돼 그 두 commit 상태로는 freshFlyway test가 실패한다(f34cdb1d가 고침).

## 하지 않은 것

- backend 전체 suite와 native S1–S4는 지시대로 돌리지 않았다(coordinator).
  probe mode(`ACTUAL_RELAX_WIRE=true`)도 돌리지 않았다.
- worker endpoint, model runner, role별 process, 보존 삭제(tombstones·
  deletion_log), locks 관찰 원천.
- DefinitionLifecycle의 RED test는 구현과 같은 commit에 썼다(UD-1·UD-2는 RED
  실패를 확인한 뒤 구현했다).
- 702b2b03 첫 전체 실행은 실행 중에 이 문서 초안을 worktree에 만들어
  checkoutDirty가 되었고(T25 5 PASS 상실) 버렸다. 같은 commit을 깨끗한 tree로
  다시 실행한 것이 위 표의 702b2b03 행이다. 20fbe574 실행은 round 14 병합 지시로
  중간에 멈췄다.
- regulatory 정책 없는 절차의 reader 시도는 이제 인가 전에 HELD
  POLICY_UNRESOLVED다(C3 4, 전에는 500). 계획 §3.3 순서상 제품 판정이 먼저다.

## 검사

| 명령 | exit | 결과 |
|---|---:|---|
| SalesCommandPostgresTest(UD-1 RED→GREEN) | 0 | 8/0 |
| S2WorkActualIntegrationTest(UD-2 RED→GREEN) | 0 | 9/0 |
| DefinitionLifecyclePostgresTest | 0 | 3/0 |
| S1ReadIntegrationTest(CDS/Flyway 전수 비교 포함) | 0 | 10/0 |
| Fulfillment·Settlement·Receipt·Return·Recall·Shipment·Purchase 등(3713c28e) | 0 | 173/0 |
| Responsibility 4 class, S2WorkLifecycle, DomainVocabulary, CapabilitySlot | 0 | 0 failure |
| StockCommandPostgresTest·FulfillmentPostgresTest(cb3fa60f·allocationId) | 0 | 14/0, 54/0 |
| S3RegulatoryActual·Identity 2 class·S4RegulatoryProjection(4c5c52fd) | 0 | 0 failure |
| `./verify harness`(d63302d7) | 0 | 534 tests, 0 failure |
| `./verify prepare`(d63302d7) | 0 | PREPARED, problems 0, capabilitySlotContract 1,864 명령 중 463 위반 |
| 부분 실행 T21·C4·V1, V5·V7, C3·E2·T14 | 1 | 본문 |
| `./verify scenarios --actual`(d63302d7, 기본 heap) | 1 | 위 실행 비교 |

