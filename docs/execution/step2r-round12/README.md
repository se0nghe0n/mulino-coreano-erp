# Step 2 round 12: case 요청을 제품이 공개한 요청 계약에 맞춘다

첫 실제 실행(`docs/execution/s5a-adapter/run-61bd0f8a.json`)에서 802
subcase 중 613건이 업무 규칙보다 먼저 요청 모양 때문에 거부됐다. api
명령 1354건이 모두 `contracts/intent.schema.json`을 어겼고, 조회는
계획·제품 어디에도 정의되지 않은 key를 보냈다. 한 grant에 품목·업무·
segment를 함께 적은 fixture는 제품의 차원 교집합 때문에 111건이
FORBIDDEN이었다. 이번 round는 2026-10-09 결정(AGENTS.md)대로 실행
결과로 고칠 곳을 정하고, 고친 뒤 다시 실행해 확인했다.

worktree `mulino-ontology-step2r-round12`, branch `step2r/round12`,
기준 `ce4970c4`. backend·`verification/actual/**`·actual adapter Java는
읽기만 했다.

## Commit

| commit | 내용 |
|---|---|
| `2a756901` | 계약 파일·검사기·conform module, 생성기 hook, round script, 443개 파일 재생성, prepare 연결 |
| `6015e986` | 필수 식별자 규칙(`requires`)과 case 값에서 id·workId 파생 |
| `f4299880` | 명령 대상 조회를 제품 명사 유형과 전용 조회로 보냄, C3 fixed point 수정 |
| 이 commit | 실행 inventory 두 개와 이 문서 |

## 항목별 상태

| 항목 | 상태 | 요약 |
|---|---|---|
| 1. 명령이 intent schema를 지킨다 | DONE | api·mcp·worker·management·우회 route·batch operation·blob businessAction·wire `tools/call` 인자를 `intent.schema.json` 자체로 검사한다. 위반 0. 배열 slot 345건은 제품 gap(아래) |
| 1a. provenance 규칙 | DONE | 아래 "provenance 규칙". slot마다 정확히 하나. case가 말한 값 1,927건(slot에 묻혀 있던 값)은 그대로 옮겼다 |
| 1b. 계약 밖 field 이동 | DONE | scope·asOf·knownAt·requesterContext 삭제, 업무 값은 slots, test barrier·환경·인증 변형·worker 문맥·MCP 문맥은 `action.harness`, `originalTextRef`/`contextRef`→`sourceRefs`/`contextRefs`, `canonicalRequestHash`→`canonicalIntentHash`, slot `proposalRevision`→intent `proposalRevision` |
| 1c. 의도된 계약 위반 | DONE | 6건을 `harness.intentionalViolation`으로 선언(아래). prepare가 선언과 실제 위반이 같고 거부가 고정됐는지 확인한다 |
| 1d. MCP·worker·wire | DONE | 제품 MCP command tool의 inputSchema는 `CommandSchemas.input`(intent + commandIdempotencyKey 필수), read tool은 query field다. wire `tools/call` 인자 41건도 같은 규칙으로 검사한다. worker route는 제품에 외부 endpoint가 없어 intent 규칙만 적용한다 |
| 2. 조회 key | DONE | 아래 "조회 key 처리". 미정의 key 0. 필요한데 계약이 없는 selector는 key를 새로 만들지 않고 KNOWN_OPEN 제품 gap으로 남겼다 |
| 3. grant 차원 의미 | DONE(case) / OPEN(installer) | 두 차원 이상을 쓰는 grant 3,972개에 `scopeComposition: PER_DIMENSION`을 선언했다. installer가 아직 이 field를 읽지 않아 기본 실행에는 반영되지 않는다(Step 3 요청) |
| 4. prepare가 1·2를 강제 | DONE | `check_request_contracts.py`를 `PreparationAssetChecks`의 `request-contracts`로 연결했다. 위반은 FAIL, 기록된 gap은 KNOWN_OPEN |
| 재생성 | DONE | 생성기와 후처리기가 같은 module을 호출한다. 손 관리 case·fixture는 `author_round12.py`. 기준 상태에서 한 번 실행한 결과와 두 번 실행한 결과가 같다 |

### provenance 규칙

계획 §3.3은 각 값의 provenance를 `USER|CONTEXT|APPROVED_DEFAULT`로
요구한다. 값의 출처는 case의 이야기에서 정했다.

1. case가 말한 provenance가 있으면 그대로 쓴다. 기존 case는 1,927개
   slot 값 안에 `provenance`를 적어 두었다(USER 1,357, CONTEXT 541,
   APPROVED_DEFAULT 29). 값에서 떼어 intent의 `provenance`로 옮겼다.
2. 없으면 요청자가 fixture 세계나 앞 응답에서 고른 값(`$alias`,
   `$result`, `$transform`, typed `{type,id}`, 그런 값만 담은 list)은
   CONTEXT다. 기존 작성자의 선택(참조 값의 95%가 CONTEXT)과 같다.
3. 그 밖에 요청자가 요청에 직접 적은 literal(문장, decimal, boolean,
   수량, aggregate)은 USER다.
4. APPROVED_DEFAULT는 추론하지 않는다. case가 말한 29건뿐이다.
5. `valueProvenance: "USER"` 같은 일괄 문자열은 slot별 진술이 아니라
   무시했다. V6 `destination-input-supplement/input-complete`는 사용자가
   MRTR 입력으로 목적지를 답했으므로 `destinationId`를 USER로 명시했다.

결과 분포: USER 7,084, CONTEXT 5,853, APPROVED_DEFAULT 29.

### 버린 명령 field의 근거

- `asOf`·`knownAt`: 명령 576건의 asOf가 모두 그 시점의 제품 시계와
  같았다(fixture clock과 clock control로 재계산). 명령은 제품 시계에서
  실행되므로 잃는 의미가 없다. knownAt(시계+1초)은 조회 개념이다.
- `scope`·`requesterContext`·자기 조직 `organizationId`: 요청자와 범위는
  인증 문맥과 grant에서 온다(계획 §3.3, §7.1).
- api의 `requestState`: api 입력 수집은 `conversationRequestId`가
  잇는다(계획 §3.3). MCP의 requestState·inputResponses는 tool 인자가
  아니라 MCP protocol field라 `harness.mcp`로 옮겼다.
- null slot(V6 `destinationId`, T15 `officialSource`)은 빠진 값이므로
  slot을 지웠다(계획 §3.1 MISSING). 정수 slot `fractionDigits`는
  decimal 문자열이다.

### 의도된 계약 위반 6건

| case/subcase/action | field | 이유 |
|---|---|---|
| T08/payload-org/attack | organizationId, actorId | 본문의 조직·주체 주장 |
| T08/payload-actor/attack | actorId, delegatorId, role | 본문의 주체·위임 주장 |
| T24/deny-worker/attack | originalActorId, originalDelegatorId | worker 본문의 원 요청자 위조 |
| T24/sentinel-artifact-scan/request-with-sentinel | forceValidationError, syntheticAuthenticationMetadata | 거부된 요청이 비밀 sentinel을 실어야 artifact scan이 성립한다(거부 응답은 고정하지 않음, 이유 기록) |
| T26/safe-retry-forged-original-actor/retry | originalActorId | retry 본문의 원 actor 위조 |
| T04/scale-overflow/invalid | slots/quantity | 소수 13자리 수량은 반올림 없이 거부해야 한다(계획 §3.1) |

T24 `deny-admin`은 이야기가 다른 조직의 주체를 지목하는 것이라 본문의
`organizationId`를 지우고 slot `subjectId`로 남겼다. T26 위조 hash는
계약 field `canonicalIntentHash`로 보낸다. T08 `document-admin`의
문서 본문은 `sourceRefs`로 옮겨, 권한 판정만 남게 했다.

### 조회 key 처리

| key | 처리 | 근거 |
|---|---|---|
| `includeDescendants`, `rawRowsOrder`, case namespace(`caseId`·`subcaseId`·`scenarioId`·`caseNamespace`·`caseKey`) | 삭제 | 계획·제품에 없음. item 범위 조회가 이미 모든 active leaf를 돌려준다. 관찰(observe) scope는 그대로 |
| getInventory `workId`, `workIds`, `obligationRootId`, `quantityMode`, `endpoint` | 삭제 | 업무 범위 재고는 계획에 없다(§3.4는 업무 조회가 물량을 돌려준다). item 범위 snapshot으로 같은 전후 비교가 된다 |
| `organizationIds` | 읽는 actor의 조직 하나 | 조직은 인증 문맥 하나다. 그 조직 품목이 하나면 itemId를 붙였다 |
| `objectId`, `policyId`, `grantId`, `definitionId`, `commandId`, `evidenceId` | `id` | QueryRequests의 식별자 field |
| getObject 유형 | `scope.objectType` | top-level `type`은 filters로 올라가 provider가 거부한다 |
| 비명사 대상(Work, PolicyVersion, DefinitionPackage, Grant, ReconciliationCase) | 전용 조회 | 계획 §3.4 업무 조회, 제품 PolicyQueries·IdentityQueries·EvidenceQueries |
| `subjectRefs`, QUERY intent `slots`·`intentKind`·`capabilityId` | 연산별 id·itemId·workId·lotId·subjectKind/subjectId로 | 제품 query envelope에는 intent field가 없다 |
| `include`, `evaluationTime`(모두 asOf와 같음), `issueKinds`, `includeValidation`, `includeDrafts`, `predicateInputId` 등 | 삭제 | 응답이 이미 전체 투영을 돌려준다 |
| getAccessContext `actorId`(= 호출자) | 삭제 | 제품은 호출자 문맥을 답한다 |
| getObject id 없음 + scope item | id = 그 item | T13·T14·T15 |
| getAssessment·getWork work 없음 | subcase가 만든 유일한 Work | T13 after-* |

## 제품 gap(KNOWN_OPEN, owner Step 3)

`contracts/request-contracts.json` `knownOpen`. prepare 보고서의
`knownOpenGaps`(check `request-contracts`)에 case별 한 줄씩 나온다.

| gap | 건수 | 내용 |
|---|---:|---|
| PG-INTENT-ARRAY-SLOT | 345 | intent schema value에 배열이 없는데 제품 handler가 list slot을 읽는다(splitQuantity quantities, mergeQuantity segmentIds) |
| PG-INVENTORY-ACTION-ELIGIBILITY | 188 | 계획 §4.2 행동별 적격량. getInventory가 action/customerId를 filter로 거부 |
| PG-TARGET-REVISION-READ | 114 | Allocation·Obligation 등 8종 명령 대상의 revision을 읽을 조회가 없다 |
| PG-OBLIGATION-SELECTOR | 51 | 의무 id·kind·allocation 또는 work 없는 의무 조회 |
| PG-ORGANIZATION-SNAPSHOT | 43 | T02·T07·T21·V1이 item 없는 조직 단위 snapshot을 읽는다 |
| PG-SOURCE-KEY-SELECTOR | 32 | 계획 §4.3 원천 키·occurrence·activity로 inbox/evidence 조회 |
| PG-QUERY-CAPABILITY-ABSENT | 15 | evaluateEligibility·convertUnit·getCommandResult handler 없음 |
| PG-ASSESSMENT-INVOICE, PG-DEFINITION-LISTING, PG-EXTERNAL-ID-LOOKUP, PG-ACCESS-CONTEXT-OTHER-ACTOR, PG-SEARCH-MULTI-TYPE, PG-TRACE-AFFECTED-SUBSET | 12 | 각 1–3건 |

## 사용자 결정 필요: grant 차원 의미

계획 §7.1·§218은 역할과 grant의 교집합을 말하지만, 한 grant 안의
품목·업무·장소 목록이 서로 대안인지 모두 맞아야 하는지는 정하지
않았다. 제품 `IdentityAuthorization.permittedScopes`는 한 grant가 이름
붙인 모든 차원 종류가 대상과 맞아야 통과시킨다(all-of). 대상에 그
차원이 없으면(품목 조회에 WORK 차원) 거부된다. 이번 round는 지시대로
제품 의미를 따르고, case 의도(대안)를 별도 grant로 표현하도록
`scopeComposition: PER_DIMENSION`을 선언했다. S4 backlog의
'ManagementCoverage any-dimension'과 같은 뿌리다. 한편 round 10 규칙
(`grantAuthorityProblems`)은 장소 목록을 제한으로 다루므로, 장소 제한을
교집합으로 남길지는 사용자가 정해야 한다. `ALL_DIMENSIONS`도 선언할 수
있다.

## 실행 비교

모두 `./verify scenarios --actual`, partial fixture, 일회용 PostgreSQL.
기준은 s5a의 `61bd0f8a`(working tree dirty, observer 미commit) 실행이다.

| 실행 | mode | 실행 action | FAIL/TEST | FAIL/ADAPTER | NOT_IMPL | 요청 모양 첫 실패 |
|---|---|---:|---:|---:|---:|---:|
| `61bd0f8a`(전) | 기본 | 1,647 | 613 | 159 | 30 | 613 |
| `2a756901` | 기본 | 1,614 | 3 | 769 | 30 | 3 |
| `f4299880`(`run-f4299880.json`) | 기본 | 1,614 | 3 | 769 | 30 | 3 |
| `f4299880`(`probe-f4299880.json`) | probe(`ACTUAL_RELAX_WIRE=true`) | 4,071 | 9 | 713 | 80 | 9 |

"요청 모양 첫 실패"는 첫 실패가 `Unsupported inventory scope`,
`Unsupported query field`, `Unsupported command field`, `Typed slots,
provenance and subjects required`, `Unsupported scope key`,
`Unsupported object type` 등 envelope 거부인 subcase 수다. 남은 3건(T22
accepted `sourceKey`, T26 orphan-intake `sourceNamespace`)은
PG-SOURCE-KEY-SELECTOR로 기록한 의도적 잔류다. 기본 실행의 adapter
wire 수정(`removedScopeKeys`)은 0건이다(전에는 case namespace key를
지웠다). probe 실행에서도 요청을 고친 흔적이 0건이라, probe mode가 다른
점은 grant를 차원별로 나눠 설치한 것 하나다.

기본 실행은 grant가 하나의 all-of grant로 설치돼 첫 실패가 대부분
`FORBIDDEN: Unavailable scope`(587)로 옮겨 갔다. 실행 action 수가
늘지 않은 이유다. 차원별 grant를 설치한 probe에서는 실행 action이
1,614→4,071로 늘고 FORBIDDEN은 185로 줄었다.

### 남은 첫 실패 상위(기본 `f4299880`)

| 건수 | 분류 | 첫 실패 | 소유 |
|---:|---|---|---|
| 587 | ADAPTER | 기준 조회 FORBIDDEN(grant all-of 설치) | Step 3 installer: `scopeComposition` |
| 32 | ADAPTER* | getInventory action/customerId | 제품 gap PG-INVENTORY-ACTION-ELIGIBILITY |
| 27 | ADAPTER | host/barrier control 없음 | Step 3 |
| 18 | ADAPTER | `Unsupported sales order slots` | slot 어휘(다음 round 판정) |
| 17 | ADAPTER | `Unsupported lifecycle slot`(T26·V5) | slot 어휘 |
| 16 | ADAPTER | `Item scope required` | 제품 gap PG-ORGANIZATION-SNAPSHOT |
| 32 | ADAPTER | `Unresolved server fixture alias "need"` | Step 3 installer: fixture evidence alias 미설치 |
| 12 | ADAPTER | `VERSION_UNSUPPORTED` | fixture 정의 버전 |

### 남은 첫 실패 상위(probe `f4299880`)

185 FORBIDDEN은 C3 대상 조회(getWork 36, getDefinition 21,
PurchaseOrder·Invoice 각 15 …)와 T20 purchase-work다. 대상이 delegator
grant의 차원 밖이거나 partial fixture로 설치되지 않았다. 51
`Invalid typed selector`는 PG-TARGET-REVISION-READ 대상이다. 31
`expected FORBIDDEN, observed TYPE_INVALID`는 C3·V4가 모든 능력에
같은 범용 slot 묶음을 보내 handler가 인가 전에 slot을 거부하거나(C3·
V4·V6), T08 의도된 위반에 제품이 TYPE_INVALID로 답한 경우다. 둘 다
아래 Step 3 발견으로 넘긴다.

## Step 3 발견과 요청

- installer(`ScenarioFixtureInstaller`): grant `scopeComposition:
  PER_DIMENSION`이면 차원 종류마다 grant를 하나씩 설치한다(같은
  actions·delegator·유효기간·revision). probe의 `unionGrantDimensions`
  코드가 그대로다. 없거나 `ALL_DIMENSIONS`이면 지금처럼 하나다.
- installer: fixture `evidence[]`의 alias(T13·T14·T15·T19 `need`,
  `warehouse-receipt` 등)를 설치하고 alias map에 묶는다. 전에는
  literal 문자열 `"need"`가 UUID가 아니라 제품이 거부했고, 이제
  `{"$alias":"need"}`라 harness가 해석 단계에서 멈춘다.
- adapter: `action.harness`(testBarrier, environmentId,
  authenticationTest, executionContext, mcp)를 읽어 header·route·
  환경으로 옮긴다. 요청 본문으로 보내지 않는다.
- 제품: 위 KNOWN_OPEN gap 13종. 특히 intent schema 배열 value와 행동별
  적격량.
- oracle 판단 필요: T08 payload-org·payload-actor는 FORBIDDEN을
  기대하지만 제품은 계약 밖 field를 TYPE_INVALID로 먼저 거부한다.
  계획 §3.3 검증 순서로 보면 TYPE_INVALID가 맞을 수 있다. oracle은
  바꾸지 않았다.

## 계약 정렬이 드러낸 test 결함(고침)

- C4 `resolved-no-resurrection-*` duty: 계약 밖 `filter` 안의 의무
  kind `DELIVERY_DEFICIT`가 vocabulary 검사를 피했다. 같은 case의
  oracle이 쓰는 `DELIVERY_CORRECTED_DEFICIT`로 고쳤다.
- T06 deadline 3건·T22 two-sources: 수령이 W(INTERNAL_STORAGE)의 기존
  segment를 지목했다. top-level field라 transit 수령 규칙이 못 봤다.
  segmentId 없는 직접 수령으로 고쳤다(`author_contracts.py`).
- T20 stdio 쓰기 probe: `commandIdempotencyKey`가 없어 tool input
  schema(-32602)에서 멈추고 기대한 FORBIDDEN에 닿지 않는다. key를
  더했다(`author_cases.py`).
- `ContractValidator.namesBasis`가 provenance map의 key(`evidenceRef`)를
  근거로 셌다. 근거 검사에서 provenance를 뺐다.

## 검사

| 명령 | exit | 결과 |
|---|---:|---|
| `./verify prepare` | 0 | PREPARED 41/802/24034, problems 0, caseAssetChecks 12개 PASS(request-contracts 포함), knownOpenGaps 122 |
| `./verify harness` | 0 | 530 tests, failures 0 |
| `python3 -I verification/cases/check_request_contracts.py` | 0 | VALID, 41 cases, 374 fixtures, knownOpen 800건(67 line) |
| `python3 -m unittest discover -s verification/cases` | 0 | 28 tests(test_request_contract 22) |
| `python3 -m unittest discover -s verification/requirements` | 0 | 38 tests(round 12 author fixed point, 생성기 reproduce 포함) |
| `python3 -m unittest discover -s verification/mcp-tests` | 0 | 3 tests |
| `python3 -m unittest discover -s verification/coverage` | 0 | 68 tests(2a756901 시점) |
| `validate_catalog.py`·`check_layer_routes.py`·`check_derived_bindings.py`·`check_vocabulary.py --check`·`cases_b_invariants.py` | 0 | VALID |
| `T08/bind_observations.py --check`, `V7/bind_observations.py V4·V6·V7 --check`, `V4/author_review_fixes.py --check`, `author_round12.py --check` | 0 | CURRENT |
| `./verify scenarios --actual`(기본, 3회) | 1 | 위 표. 제품 PASS 없음 |
| `ACTUAL_RELAX_WIRE=true ./verify scenarios --actual`(probe, 2회) | 1 | 위 표 |

## 하지 않은 것

- contract-red는 이번 변경이 action·assertion 집합을 바꾸지 않아
  prepare의 feature·registry 대조와 harness test로 대신했다.
  `./verify contract-red --all`은 실행하지 않았다.
- coverage unittest는 첫 commit(`2a756901`) 뒤 한 번만 돌렸다.
- slot 이름 어휘(제품 handler의 slot 허용 목록과 case slot)는 손대지
  않았다. round 11 요청처럼 adapter 투영으로 할지 case를 고칠지 판정이
  필요하다.
- C3·V4 대상 조회의 grant 차원 범위, partial fixture 거래 alias 설치는
  다음 실행 판정으로 남겼다.
- installer·adapter·backend는 바꾸지 않았다. push·merge·Grok Bot
  알림은 하지 않았다.
