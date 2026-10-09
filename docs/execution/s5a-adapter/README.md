# S5a: Step 2 시나리오 실제 제품 실행 inventory

2026-10-09 결정에 따라 Step 2의 41 case를 정적 검토 대신 새로 build한
backend와 일회용 PostgreSQL에 실행했다. 이 문서는 마지막으로 완료한
실행의 요약이다. subcase별 상세는 `run-61bd0f8a.json`에 있다.

## 실행 방법

```sh
./verify scenarios --actual            # backend package 후 run.sh 호출
sh verification/actual/scenarios/run.sh <evidence-dir>   # 이미 build한 경우
python3 verification/actual/scenarios/inventory.py <evidence-dir>/scenarios.json <out.json>
```

환경 변수 `ACTUAL_PARTIAL_FIXTURES`(기본 true)와 `ACTUAL_RELAX_WIRE`
(기본 false)가 실행 mode다. mode는 evidence의 `run-mode.txt`와 inventory의
`runMode`에 남는다. relax mode는 probe 전용이다(아래 참고).

## 결과 요약 (run `/tmp/s5a-r2`, code 61bd0f8a + 미commit observer)

41 case, 802 subcase를 모두 시작하고 끝냈다. skip은 0이다.
실행한 action은 1647개, 실행한 assertion 결과는 PASS 6, FAIL 5,
NOT_RUN 3513개다. PASS로 끝난 subcase는 0이다.

| 상태 / 분류 | subcase |
|---|---:|
| FAIL / TEST (case 요청이 계약·제품 schema 밖) | 613 |
| FAIL / ADAPTER (fixture 부분 설치, host control 등) | 159 |
| NOT_IMPLEMENTED / ADAPTER | 30 |
| PASS | 0 |

분류는 triage 추정이다. 확정 판정이 아니다.

## 첫 실패 상위 묶음

| 건수 | 분류 | 첫 실패 | case |
|---:|---|---|---|
| 412 | TEST | getInventory scope의 `workId`를 제품이 거부(`Unsupported inventory scope`) | C3 |
| 111 | ADAPTER | 기준 query가 `FORBIDDEN`(grant 차원 교집합 + 부분 fixture) | C5 T01 T09 T10 T20 |
| 73 | TEST | query 요청의 `objectId` 등 제품 query envelope 밖 field | C3 T02 T06 T07 T17 T21 T22 |
| 70 | TEST | command 요청의 `scope`·`asOf`·`knownAt` 등 intent 계약 밖 field | C3 C4 E1 E2 T06 T07 T17 T18 T21 |
| 47 | TEST | command 요청에 `provenance`·`slots`·`subjectRefs` 없음 | C2 C5 T09 T11–T15 T19 |
| 32 | ADAPTER* | getInventory의 `action`·`customerId` filter 거부 | C1 T03 T04 T05 T16 V2 V3 |
| 27 | ADAPTER | process/barrier/fault host control 미구현 | T23 T25 V8 |
| 11 | TEST | `includeDescendants`·`organizationIds` 등 scope key 거부 | C3 T08 |

\* 32건은 fixture가 부분 설치라서 ADAPTER로 내렸다. 계획 §122의
`행동별 적격량`으로 보면 제품 결함(PRODUCT) 후보다.

## Step 2(TEST) 발견

1. **모든 api invoke 1354건이 `contracts/intent.schema.json`을 어긴다.**
   1354건 전부 필수 `provenance`가 없고, 170건은 `slots`, 142건은
   `subjectRefs`가 없다. 638건은 schema에 없는 `scope`, 576건은
   `asOf`·`knownAt`, 244건은 `requesterContext`를 보낸다. 제품은 계약대로
   거부한다. `./verify prepare`는 invoke 요청을 intent schema로 검사하지
   않는다.
2. **query scope key가 계획·제품 schema에 없다.** `includeDescendants`
   1246회, getInventory `workId` 1221회, `organizationIds`·`workIds`·
   `obligationRootId`·`rawRowsOrder`를 쓴다. 계획 §89는 "허용 filter만
   query schema로 연다"고만 하고 이 key를 정의하지 않는다.
3. **getObject는 `objectId`(617회)를 보내는데 제품은 `id`를 받는다.**
   양쪽 다 계약이 없다. 계약 소유자가 하나로 정해야 한다.
4. **grant scope의 차원 의미가 정해지지 않았다.** fixture는
   `itemAliases`·`workAliases`·`segmentAliases`를 한 grant에 함께 쓴다.
   제품은 차원을 교집합으로 계산하므로 업무 밖 품목 조회가 FORBIDDEN이다.
   계획 §218은 교집합을 말하지만 한 grant 안의 목록이 합집합인지는
   정하지 않았다.
5. case namespace key(`caseId`·`subcaseId`·`scenarioId`·`caseNamespace`·
   `caseKey`·`environmentId`)를 query scope에 넣는다. adapter가 wire에서
   지우고 receipt의 `removedScopeKeys`에 남긴다.

## PRODUCT 후보

- getInventory가 `action`·`customerId`(행동별 적격량, 계획 §122)를 받지
  않는다. 32 subcase. 작은 수정이 아니라 고치지 않았다.
- getInventory가 `workId` scope를 받지 않는다. ApplicationQueries는
  `workId`를 허용하므로 두 층이 어긋난다. 업무 범위 물량 의미가 계획에
  없어 TEST로 분류했다.

## 이번에 한 adapter 작업

- `ScenarioFixtureInstaller`: 802 subcase 모두 fixture를 설치한다
  (설치 단계 NOT_IMPLEMENTED 802 → 0). 모든 행은 recordedAt = fixture
  clock asOf다. 옮기지 못한 사실은 `omittedFixtureFacts`에 남는다.
- `ScenarioJdbcObservation` + `observation-sources.json`: 원천 이름
  98개를 제품 table에 대응하는 observer다. 조직·품목·업무·LOT으로
  거르고 recordedAt ≤ knownAt만 읽는다. 대응이 추정인 원천은
  `approximate`에 표시했고, 그 원천의 assertion 실패는 ADAPTER로 센다.
  RESULT_REVISION은 발급 query를 같은 actor·요청으로 다시 호출해 제품
  revision과 비교한다. 독립 재계산이 아니고 `revisionQuery`에 그렇게
  적었다.
- query 경로: suite에서는 api query를 모두 제품에 보낸다. 모르는
  capability는 제품이 답한다.
- probe mode(`ACTUAL_RELAX_WIRE=true`): `includeDescendants`와
  getInventory `workId`를 지우고, `objectId`를 `id`로 바꾸고, command
  envelope을 intent schema에 맞추고(`valueProvenance`→`provenance`, 없으면
  slot마다 USER), grant를 차원별로 나눈다. 기본 실행에는 쓰지 않는다.

## 하지 않은 것과 남은 adapter 공백

- **observer는 실행으로 검증하지 못했다.** r2에서는 모든 subcase가 첫
  observe 전에 멈췄다. probe 실행(r3)은 13:33에 시작했지만 실행 중
  OrbStack Docker daemon이 내려가 보고서가 없다. daemon을 다시 띄우지
  않았다.
- process/barrier/fault/externalResponder host control(698 control
  action 중 clock 외 전부), mcp·worker·wire·blob·batch route, agent
  client 실행이 없다.
- fixture 미설치 사실: PurchaseOrder·SalesOrderLine·Allocation 등 거래
  alias, responsibilities, evidence event/claim, baseline.policies 등.
- observer 미대응 원천: externalEffects, schemaCatalog, restoreSessions,
  locks, lockWaits 등 host·DB 내부 원천.
- harness unit test와 installer·observer 회귀 test를 실행하지 않았다.
