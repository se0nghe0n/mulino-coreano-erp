# S5b: 시나리오 adapter 2차 — fixture installer, 독립 observer, route

s5a 실행(`../s5a-adapter/run-61bd0f8a.json`)은 802 subcase 모두 요청 모양,
부분 fixture, api 밖 route에서 멈췄다. 이번 작업은 ADAPTER 원인을 줄여
뒤 단계까지 실제 제품에 닿게 하는 것이다. 작업 중 Step 2 round 12
(`1bd91081`)가 case 요청을 제품 계약에 맞췄고 이를 merge했다(`77dd16a7`).

worktree `mulino-ontology-step3-s5b-adapter`, branch `step3/s5b-adapter`,
기준 `ce4970c4`. 소유: `backend/**`, `verification/actual/**`,
`verification/harness/src/main/java/org/mulino/verification/actual/**`, 이
디렉터리. case·contract·비 actual harness는 바꾸지 않았다.

## 실행 결과

모두 `sh verification/actual/scenarios/run.sh <dir>`(`./verify scenarios
--actual`의 backend package 뒤 단계), partial fixture, 일회용 PostgreSQL.
inventory는 `inventory.py`로 만들었다.

| 실행 | code | mode | 실행 action | PASS | FAIL/TEST | FAIL/ADAPTER | NOT_IMPL |
|---|---|---|---:|---:|---:|---:|---:|
| s5a `61bd0f8a`(전) | 61bd0f8a(dirty) | 기본 | 1,647 | 0 | 613 | 159 | 30 |
| 이 branch 시작 `ce4970c4` | ce4970c4 | 기본 | 1,647 | 0 | 615 | 157 | 30 |
| round 12 기준 `run-f4299880.json` | f4299880 | 기본 | 1,614 | 0 | 3 | 769 | 30 |
| **최종 `run-85ce2365.json`** | 85ce2365(clean) | 기본 | **1,664** | 0 | 3 | 769 | 30 |
| round 12 probe `probe-f4299880.json` | f4299880 | probe | 4,071 | 0 | 9 | 713 | 80 |
| **최종 probe `probe-85ce2365.json`** | 85ce2365 | probe | **4,448** | 10 | 29 | 676 | 87 |

probe는 `ACTUAL_RELAX_WIRE=true` 진단 mode다(grant 차원별 분리, 요청 slot
변환, 형식 없는 getObject에 alias 형식 지정). 판정에 쓰지 않는다. probe
PASS 10(T20 wire 5, T20 locationFridayTypeError, V4 raw-crud 4)은 모두
부분 fixture 위의 PASS다.

기본 실행 총계가 round 12 기준과 같아 보이지만 첫 실패의 내용이 바뀌었다.
`Unsupported inventory filter`(32)와 `Unresolved server fixture alias
"need"`(32) 묶음이 사라지고 그 subcase가 다음 단계로 진행했다. 대부분은
다음 검사인 기준 조회 FORBIDDEN에 다시 멈춘다(587→629).

### 남은 첫 실패 상위(기본 85ce2365)

| 건수 | 첫 실패 | 소유 |
|---:|---|---|
| 629 | 기준 조회 FORBIDDEN(아래 case별 표) | Step 2 case 결함 |
| 23 | `Unsupported Work goal slot` | slot 어휘(Step 2 판정) |
| 22 | process/verifyCoverage host control 없음 | Step 3 host adapter |
| 18 | `Unsupported sales order slots` | slot 어휘 |
| 17 | `Unsupported lifecycle slot`(T26·V5) | slot 어휘 |
| 16 | `Item scope required`(T02·T07·T21·V1) | 제품 gap PG-ORGANIZATION-SNAPSHOT |
| 12 | VERSION_UNSUPPORTED(C3·T21·V1) | fixture 정의 버전/미구현 capability |

## 한 일

### fixture installer (`ScenarioFixtureInstaller`)

- baseline `items`·`places`·`lots`·`documents`·`priorEntities`·
  `salesOrder`·`allocation(s)`를 alias 속성으로 합친다(작성된 alias key 우선).
- SalesOrder → Orders + OrderRevisions(1), SalesOrderLine → OrderLines.
  수량 없는 line은 유일한 주문의 수량·단위·목적지를 받는다(round 11).
  fixture가 말하지 않는 NOT NULL 열(price·currency·dueAt·endpoint·terms)은
  명시적 synthetic 값이고 규약에 남긴다. Customer가 아닌 고객 alias는 같은
  ID의 Customer 행, Place가 아닌 목적지는 조직의 유일한 CUSTOMER 장소다.
- Allocation → SegmentAllocations: segment, line, 수량, state, 선언된
  `startQuantity`(없으면 좌표 없음), revision, `pickedAt`(asOf 이하만).
  `NOT_CREATED`는 설치하지 않는다. `pickedByAlias`는 제품 열이 없다.
- QuantitySegment revision(alias, 없으면 `entityRevisions.initial`, 없으면 1).
- `baseline.sourceProfiles` → SourceProfiles. fixtureContent 원본은 alias를
  설치 ID로 풀어(`receivingCustodianAlias`→`receivingCustodianId`) canonical
  JSON bytes를 backend blob store에 쓰고 AVAILABLE로 설치한다. 저장 sha256은
  풀어 쓴 bytes의 hash다. `documents[].path`는 파일 hash가 작성값과 같을 때만
  쓴다. 선언 recordedAt이 fixture knownAt보다 늦은 근거는 그 시각에 기록한다.
- fixture `evidence[]`의 alias(`need` 등)를 alias map에 묶는다(round 12 요청).
- grant: 2026-10-09 사용자 결정대로 기본 실행은 grant를 작성된 그대로 하나로
  설치한다(all-of). `scopeComposition`은 값 검사만 하고, 차원별 분리는 probe에만
  있다. GrantScopes CHECK가 허용하지 않는 차원(customerAliases·
  supplierAliases·workKinds·basisKinds·settlementModes·derived segment)은 grant에서
  빠지므로 omitted와 `notRepresentedFacts`에 함께 둔다.
- 제품 schema 제약(FK·CHECK)에 걸리는 사실(item 단위와 다른 segment, 설치 안 된
  lot)은 설치를 실패시키지 않고 omitted로 남긴다. 설치 단계 실패는 0이다.

### observer

- `ScenarioRevisionBinding`: RESULT_REVISION을 발급 query 재호출 없이 판정한다.
  query 직전·직후 조직 원행 digest(`mulino_runtime_*` 제외, 모든 행·열)가 같을
  때만 제품 revision을 묶고, observer가 자기 REPEATABLE READ snapshot에서 digest를
  다시 계산해 같을 때만 그 revision을 낸다. 다르면 `row-digest:<hash>`라 harness가
  불일치로 본다. 최종 probe에서 1,274개 관찰이 모두 일치했다. 제품 projection
  계산 자체의 정확성은 증명하지 않는다(assertion 몫).
- 파생 field: movements `segmentId`, 의무 행 `sourceKind`·`sourceId`·
  `obligationId`·`responsibleWorkId`.
- approximate 30개 중 24개를 제품 schema 열로 확인해 `confirmed`로 옮겼다.
  남은 approximate: boundaries, compatibilityManifests, externalDeliveries,
  fenceCommits, recoveryObligations, validityBoundaries와 새로 더한 activities,
  retryAttempts, definitionPackages, externalEffects, transactions(xmin),
  dbPrivileges(role_table_grants), conditionResults(conditionsJson 원소).

### route와 control (`ActualAcceptanceDriver`)

- mcp: 제품 `/mcp/ontology` `tools/call`, 응답은 `structuredContent`.
- wire: 작성된 method·header·body를 그대로 보내고 `credentialProfileRef`의
  bearer만 붙인다. transcript의 Authorization은 hash로 가린다.
- direct: CAP OData `OntologyService.command`(`/odata/v4/ontology/command`).
- batch: 같은 action의 OData multipart `$batch`, atomic이면 한 changeset.
  step 응답은 adapter 집계(모두 APPLIED일 때만 APPLIED)이고 receipt에 규칙을 남긴다.
- management: 별도 transport가 없어 운영자 identity의 명령 API.
- nested·projection: 제품이 entity set·navigation을 노출하지 않아 경로가 없다.
  `no product surface`로 남기고 inventory는 TEST로 분류한다.
- clock: `advanceTo`·`set`·`advance`, instant→asOf→knownAt.
- round 12 이후 기본 실행은 요청 본문을 다시 쓰지 않는다. round 9–11이 요청한
  slot 변환(`ScenarioWireMapping`)은 probe에만 남겼다.

### 제품 수정

- getInventory 행동별 적격량(계획 §122, PG-INVENTORY-ACTION-ELIGIBILITY):
  `InventoryQueries`가 filter `action`(대문자 token, 기본 SELL)과 `customerId`를
  받고 `QualityEligibility`가 그 행동·고객으로 계산한다. scope customerId와 다르면
  TYPE_INVALID다. `ApplicationQueries` world projection도 같은 action을 쓴다. round
  12 case는 getInventory 93건이 top-level `action`·`customerId`를 보내며 기본 실행에서
  이 거부가 0이 됐다. traceLot의 action 1건(T01)은 열지 않았다.
- test: `FulfillmentPostgresTest.inventoryEligibilityFollowsTheRequestedActionAndCustomer`
  (실제 PostgreSQL, SELL·DISPATCH·정의 없는 RETURN·충돌 고객·잘못된 token).

## 검사

| 명령 | 결과 |
|---|---|
| backend focused `-Dtest=FulfillmentPostgresTest` | 53 tests, 0 failures |
| backend 전체 `./mvnw -f backend/pom.xml test` | 519 tests, failures 0, errors 2. 두 오류는 `PlatformIntegrationTest.compilerColumnsMatchFlywaySchema`·`S1ReadIntegrationTest.freshFlywayMatches…`가 실행 중 `NODE24_BIN`을 찾지 못한 환경 오류(`error=2`, 실행 중 node binary가 교체됨)다 |
| 위 두 class 재실행(`-Dtest=PlatformIntegrationTest,S1ReadIntegrationTest`) | 19 tests, 0 failures, 0 errors → 전체 519 통과 |
| 시나리오 기본·probe | 위 표 |
| native `./verify actual-s1`, `actual/s2..s4` `build.py`+`run.sh`(85ce2365 이후 clean tree) | 네 실행 모두 exit 0 |

## Step 2 발견(case 결함, oracle은 바꾸지 않음)

1. **기준 조회 FORBIDDEN 629(사용자 결정: all-of)**. 여러 차원 grant가 대상에 없는
   차원(예: inventory 조회에 WORK)을 가져 거부된다. case별 첫 실패 action:
   C3 306(before-snapshot 294, precondition-recall 9, precondition-proposal 3),
   V4 93(before-snapshot), T20 59(noun 49, purchase-work 5, pre-domain 4,
   api-result 1), T08 23, T10 19, T01 12(noun), T11 12, T24 12(view-before),
   T04 9, T19 9(invoice), V6 9, T03 8, T22 7, T09 6, T17 6, T12 5(work),
   C5 4, T16 4, T26 4, V7 4, C1 3, T05 3, V2 3, V3 3, T06 2, T02 1, T13 1,
   T15 1, V8 1. 상세는 `run-85ce2365.json` subcases.
2. C3·V4·V6·T08: 모든 capability에 같은 범용 slot 묶음을 보내 handler가 인가
   전에 TYPE_INVALID로 거부한다(probe 65 `expected FORBIDDEN, observed TYPE_INVALID`).
3. slot 어휘: dispatch `cargoPlaceId`·reserve `orderLineId`·typed `{value,unit}`
   수량 등이 제품 handler 허용 목록과 다르다(Work goal 23, sales order 18,
   lifecycle 17 등). adapter는 고치지 않는다.
4. C3 base fixture의 거래 alias(PurchaseOrder, Invoice, Recall, Shipment 등)는
   속성이 없어 설치할 수 없고, 대상 조회가 FORBIDDEN이나 `Invalid typed selector`가
   된다(probe 51).
5. `action.harness`(testBarrier, environmentId, authenticationTest,
   executionContext, mcp)는 비 actual harness가 driver에 넘기지 않는다
   (`AcceptanceDriver` port에 인자가 없다). adapter는 읽을 수 없고 요청을 다시
   쓰지도 않는다. CaseRunner·port 소유자가 전달 경로를 정해야 한다.
6. 제품 행·차원이 없는 fixture 사실(`notRepresentedFacts`): 책임의
   nextAction/nextCheckAt(800), 근거 entry의 event/claim/검증 chain(763 — entry가
   kind·claim·canonical scope를 말하지 않는다), Work 없는 책임(209), 위 grant 차원.

## 제품 발견(고치지 않음)

- direct/batch OData action은 domain 오류가 아닌 실패를 명령 envelope 없이 HTTP
  400 CAP 오류로 답한다(probe V4 21 `observed "400"`).
- getObject는 형식이 없으면 TradeItem만 찾는다(형식 지정은 round 12가 case에 넣음).
- inventory handler는 intent schema의 typed `{value,unit}` 수량과 배열 slot
  (PG-INTENT-ARRAY-SLOT)을 받지 않는다.
- MCP structureIntent/MRTR(T20 requestState·NEEDS_INPUT·STRUCTURED)가 없다.
- local profile은 DB owner로 접속해 role 권한 경계가 없다(dbPrivileges로 관찰).

## 남은 adapter 공백

- host control: process(start/stop/restart/tickScheduler/awaitRuntimeTask/
  verifyCoverage/inspectArtifacts 등), barrier, fault, externalResponder. backend에
  barrier·fault·scheduler 주기 기록 hook이 없어 adapter만으로는 닫을 수 없다.
- worker·blob route, client/model runner.
- observer: locks, lockWaits, supervisionIssues, identity_matches, tombstones,
  deletion_log, domain_records, schemaCatalog, restoreSessions.
- installer: Restriction·DispositionBasis·eligibilityBases, PurchaseOrder 등 거래
  alias, 근거 검증 chain(action 문맥이 installer에 오지 않는다).
