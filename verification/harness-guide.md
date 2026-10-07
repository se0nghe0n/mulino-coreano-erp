# Step2 공통 인수 harness 계약

업무 oracle를 구현 결과에 맞춰 바꾸지 않기 위해 Java21/Maven의 독립
harness를 먼저 둔다. CAP dependency와 제품 schema는 없다. 이 harness의
PASS는 assertion·parser·입력 계약의 검증이다. 실제 제품 DB/API/MCP,
복구, client, 모델, 규제와 배포 인수는 별도로 `NOT_RUN`이다.

## 작성자 실행 명령

저장소 루트에서 실행한다. `./verify`는 실제 command, Java/Maven version과
exit code를 출력하며 `verification/harness/target/wrapper-commands.json`에
기록한다. `target/`은 commit하지 않는 실행 산출물이다.

```sh
./verify harness
./verify validate verification/cases/T17/case.json
./verify contract-red verification/cases/T17/case.json
./verify contract-red
./verify prepare
./verify coverage
./verify scenarios
```

- `harness`: 한국어 Cucumber smoke와 assertion/계약 selftest만 실행한다.
- `validate <case.json...>`: 실제 JSON Schema와 semantic 검증이다. 파일이
  없거나 빈 입력이면 실패한다. 제품 행동·Gherkin 실행 PASS가 아니다.
- `contract-red <case.json...>`: 각 파일 옆 `scenario.feature`를 실제 JUnit6
  file selector로 발견하고 실행한다. 기대 수는 JSON의 subcase 수다.
  발견/시작/NOT_IMPLEMENTED assertion 실패 수가 모두 같고 skip0이어야
  의미 있는 RED다. 정상 의도된 RED의 exit code는1이다.
- 인수 파일 없는 `contract-red`는 `HARNESS-EXAMPLE`의 한국어 scenario
  하나를 실행한다. `contract-red --all`은 모든 case의 JSON 행동과 모든
  substantive assertion을 실행한다. 이 모드는 Gherkin discovery와
  구별하며 `red.json`의 미구현 assertion들을 기록한다.
- `prepare`/`coverage`: case/assertion/Gherkin 연결, 독립 registry와 규범
  catalog를 검사한다. 준비 PASS는 `PREPARED`, runtime/artifact는
  `NOT_RUN`이며 제품 gate는 닫히지 않는다. semantic oracle equivalence는
  case review가 필요하다. JSON pointer 존재만으로 이를 증명하지 않는다.
- `schema`, `contracts`, `scenarios`, `recovery`, `mcp`, `skills`, `model`,
  `deployment`: 실제 adapter가 없는 현재는 `NOT_RUN`, exit2다. 선행
  profile과 미완료 gate를 보고한다. 모델·유료 배포를 호출하지 않는다.

exit0은 harness/준비 검증의 성공, exit1은 assertion/인수 계약 실패,
exit2는 필수 경로 `NOT_RUN`, exit3은 환경·형식·discovery 오류다.
컴파일 오류와 미발견 suite를 의미 있는 RED로 세지 않는다.

## 파일과 소유 경계

```text
contracts/acceptance-case.schema.json
contracts/acceptance-fixture.schema.json
contracts/acceptance-driver.schema.json
contracts/acceptance-observation.schema.json
contracts/acceptance-capabilities.json
verification/fixtures/base/identity-clock.json
verification/cases/<Txx|Cx|Vx|Ex>/{case.json,fixture.json,scenario.feature}
verification/cases/registry.json
verification/requirements/mandatory-oracles.json
```

case 작성자는 자신에게 배정된 case 디렉터리만 쓴다. 공통 schema/runner,
registry와 독립 규범 catalog는 각 소유자가 통합한다. registry 형식은
`{schemaVersion, expectedCases:41, expectedSubcases, cases:[{caseId,path,
feature,subcaseIds:[...]}]}`다. 모든 실제 필수 subcase를 미리 등록한다.
독립 catalog의 모든 named observation을 assertion에 연결한다. 요구와
case를 같이 삭제해 coverage를 통과시키지 않는다.

전체 예제는 `verification/harness/src/test/resources/examples/`
`HARNESS-EXAMPLE/`에 있다. 업무 case ID가 아닌 harness 예제이며 실제
제품 효과를 만드는 fake service가 아니다. `canned-observations.json`은
assertion selftest 전용이다. 해당 표본에 `EXECUTED` 문자열이 있어도
제품 실행/독립 DB 증거로 사용하지 않는다.

## case JSON

`schemaVersion=1.0.0`, `caseId`, `title`, `requirementRefs`, `profiles`,
`subcases[]`를 둔다. subcase는 `id`, `title`, `fixtureRef`,
`requiredAdapters`, `actions`, `assertions`, `oracleExplanation`이 필수다.
SIT/UAT는 같은 파일·fixture·oracle를 사용하며 runner만 교체한다.

각 assertion은 다음을 반드시 가진다.

```json
{
  "id": "warehouse-held-80",
  "op": "decimalEquals",
  "source": {"actionId": "after-db", "pointer": "/data/data/heldQuantity"},
  "expected": "80",
  "unit": "BOX",
  "unitSource": {"actionId":"after-db", "pointer":"/data/data/unit"},
  "requirementRefs": ["D17"],
  "evidenceRefs": ["after-db:rawRows", "after-db:sourceQuery", "after-db:snapshot"],
  "scope": {"itemAlias": "P", "placeAlias": "W"},
  "oracleExplanation": "실제 수령100−출고30+반품10=80이다.",
  "oracleRef": {"oracleId": "<독립 catalog의 ID>", "observationNames": ["<named observation>"]}
}
```

위 snippet의 placeholder를 실제 catalog 항목으로 바꾼다. 제품 assertion의
`oracleRef`는 필수다. 단일 assertion이 여러 observation을 참조하더라도
각 규범 기대값과 의미를 유지해야 한다. presence만으로 수량·owner·
version·효과·종료 조건의 conjunction을 검증했다고 주장하지 않는다.

`source`는 StepResult JSON부터 읽는 RFC6901 pointer다. API 응답은
`/response/...`, DB observation은 `/data/data/...`다. 배열 선택에는
`where:{field:fixedValue}`와 `field:"quantity"`를 쓴다. `field:["id",
"parentId","quantity"]`는 관계 tuple을 만든다. 필드가 누락되면 실패하며
누락·UNKNOWN·CONFLICT를0이나 빈 배열로 바꾸지 않는다.

지원 op는 `equals`, `notEquals`, `present`, `absent`, `decimalEquals`,
`decimalAtMost`, `decimalAtLeast`, `sumEquals`, `decimalDelta`, `count`,
`exactSet`, `relationSet`, `unique`, `sameAs`, `fieldsPresent`, `timeEquals`,
`timeBefore`, `timeAtMostSeconds`다. decimal expected는 정확한 문자열이다.
단위 있는 assertion은 `unitSource`로 실제 관찰 단위도 검사한다.
`decimalDelta`/`sameAs`는 별도 `baseline` source를 요구한다.
단위 있는 `decimalDelta`는 `baselineUnitSource`도 필수다. baseline과
후속 관찰의 단위가 모두 고정 `unit`과 같아야 한다. 수치가 같아도
100KG와100BOX의 차이를0BOX로 판정하지 않는다. set/관계는
순서를 무시하되 중복 ID/tuple을 거부한다. `absent`도 실제로 관찰된 부모
scope가 필요하며 explicit null은 absence가 아니다. `fieldsPresent`는
필수 row가0개이면 실패한다. 효과0도 실제 전후 관찰을 요구한다.

## 단계와 한국어 Gherkin

행동 하나마다 `id`, `kind`, `evidenceRefs`를 둔다. `invoke`/`query`는
`actorRef`, `route`, `capabilityId`, `request`가 필수이며 capability는
계획 §3–§8의 공통 registry에 있어야 한다. 새 업무 의미가 필요하면
임의 case-local capability를 만들지 않고 공통 소유자에게 전달한다.

```gherkin
# language: ko
@T17 @D17 @sit @uat
기능: 출고 사실과 실제 인도 책임을 대조한다
  시나리오: CONSUMED 배분의 실제 인도
    먼저 사례 파일 "verification/cases/T17/case.json"의 "normal-delivery"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "dispatch" 행동을 수행한다
    만일 "receiver" 역할이 "delivery" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "warehouse-held-80" assertion으로 "현재 W 보유량"를 확인한다
```

fixture는 준비하되 `installFixture`도 명시적 action으로 실행한다.
시나리오에는 해당 subcase의 모든 top-level action과 모든 assertion을
각각 표현한다. action 순서는 JSON의 선언 순서와 같다. 한 opaque Given에
전체 업무를 숨기지 않는다. 한 subcase는 한 discovered scenario에 대응한다.
Scenario Outline의 example도 독립 subcase로 선언해 기대 수를 맞춘다.

명사/동사 조회는 snapshot token, scope, asOf와 knownAt을 함께 비교한다.
같은 knownAt 문자열만으로 같은 DB snapshot이라고 주장하지 않는다.
observe의 요청 `scope`/`asOf`/`knownAt`은 결과의 같은 필드와 정확히
같아야 한다. 요청 `snapshotRef`는 실제 결과 `data.snapshotRevision`을
지칭하며 `data.snapshot.id`도 같은 실제 DB snapshot token이어야 한다.
logical version이나 시각만 같은 다른 DB snapshot을 대신하지 않는다.
`data.snapshot`/`sourceQuery`는 provenance의 실제 값과 같아야 한다.
거부 사례는 대상 원장/배분/승인/업무 outbox의 금지 delta와 허용된 denial
감사·inbox/대조 책임을 별도 assertion으로 둔다. 전체 DB 불변을 가정하지
않는다. baseline 물량도 현재/누적 oracle의 scope에 포함하되 setup 자체를
실행 coverage로 세지 않는다.

## 서버 ID와 이전 단계 결과의 연결

fixture 설치 결과는 실제 `data.aliasMap`과 `fixtureHash`를 반환한다.
이후 요청에서 `{"$alias":"A60"}`는 그 서버 발급 ID로 치환한다.
새로 실행한 명령의 결과는 아래와 같이 명시적으로 연결한다.

```json
{
  "proposalId": {"$result":{"actionId":"proposal","pointer":"/response/proposalId"}},
  "proposalHash": {"$result":{"actionId":"proposal","pointer":"/response/proposalHash"}},
  "expectedRevision": {"$result":{"actionId":"proposal","pointer":"/response/revision"}}
}
```

`$result`는 actionId/pointer만 받는다. 실제 실행·완전한 scope·non-null
값이 없으면 사용할 수 없다. 임의 eval, SQL, 업무 선택/계산 로직은 없다.
fixture ID와 신규 명령 결과를 구별하며 adapter가 승인·수령·예약을
암묵적으로 생성하지 않는다. 예제는 hold의 실제 snapshotRevision과
restrictionId를 후속 query/observe request에 연결한다.

## 경합·장애·raw wire

`start`는 `call`로 명시한 invoke/query를 실제 비동기 실행한다. nested
call에도 id/kind/actorRef/route/capabilityId/request/evidenceRefs를 둔다.
start 결과의 `data.invocationHandle`은 실제 제출 ACK다. `await`는
`awaitActionId`와 `timeoutSeconds`로 그 실행의 실제 결과/commit을 기다린다.

```text
start(dispatch)
control(barrier, waitReached, transaction/point/participant)
invoke(placeHold) + 실제 commit artifact
control(barrier, resume, 동일 barrier/participant)
await(dispatch-start)
observe(after-hold-before-dispatch 또는 실제 최종 snapshot)
assert(출고 delta0·배분 미소비·책임 유지)
```

반대 직렬화 순서도 별도 subcase로 작성한다. V2/V3/V7는 실제 두 거래의
ACK/commit·잠금/fence 증거가 필요하다. `control`은
`type=clock|barrier|fault|process|externalResponder`, `operation`,
`parameters`를 받는다. EXECUTED control은 actual artifact와
`data.acknowledged=true`, 요청과 같은 `controlType`/`operation`,
`acknowledgedAt`을 요구한다. barrier ACK는 barrierId/participantId/
transactionId/point/state도 필요하며 요청 `control.parameters`의
각 값과 정확히 같아야 한다. 따라서 barrier 요청에도 다섯 필드를
모두 명시한다. await는 `data.completed=true`, start와 동일한
`data.invocationHandle`, `data.terminalStatus=SUCCEEDED|FAILED|CANCELLED`의
실제 terminal ACK가 필요하다. terminalStatus는 실행 종료 상태이며
별도 업무 assertion의 기대 outcome을 대신하지 않는다. 모든 start는
parallel child를 포함해 정확히 하나의 awaitActionId로 연결해야 한다.
제출 ACK와 DB 효과0만으로 case가 완료되지 않는다. barrier/no-op나 sleep 우연을 경합
PASS로 세지 않는다. `parallel.branches`는 실제 async start/control/query를
각각 실행하고 제출 ACK를 모은다. 이는 거래 완료를 주장하지 않는다.
각 invocation의 `await`와 post-commit 독립 관찰을 별도로 둔다. 모든
child도 실행 상태·scope·artifact 검증 대상이다.

parallel의 timeoutSeconds는 모든 branch를 합친 하나의 deadline이다.
timeout/실패에는 모든 Future를 cancel/interrupt하고 local executor와
실제 branch finally 종료를 최대1초 더 기다린다. Future의 cancelled나
Executor 종료 표지만으로 살아 있는 port 호출의 cleanup을 선언하지
않는다. 실패 뒤 새 adapter 호출과 늦은 result publication을 차단한다.
`target/evidence/parallel-failure-*.json`과 case evidence에는 cancellation,
남은 local branch, cleanupComplete와 FAIL을 기록한다. cleanupComplete는
local port executor의 종료이며 remote 효과 취소를 주장하지 않는다.
실제 adapter는 interruption·bounded transport timeout·외부 task 취소/대조를
지원해야 한다. 미확정 외부 효과와 cleanup 불완료는 isolated 환경을
보존해 대조하며 runtime PASS로 바꾸지 않는다.

`route=wire`의 request는 raw HTTP header/body, content type, protocol,
JSON-RPC ID, MRTR state/inputResponses/effect key를 그대로 전달할 수 있다.
harness가 공격 입력을 정상화하거나 header/body mismatch를 수선하지
않는다. 이전 wire result의 state도 `$result`로 추출한다. adapter의 실제
artifact는 Authorization/Cookie/token/비밀을 redact하고 원문 hash와
검증된 인증 metadata를 따로 둔다. committed artifact에 credential 원문을
저장하지 않는다.

## fixture와 실제 실행 port

fixture는 전체 `acceptance-fixture.schema.json`을 따른다. 가상 정책임을
`synthetic:true`로 표시한다. clock에는 asOf/knownAt/시간대/정밀도/기한
끝점, versions에는 definition/evaluator/policy를 고정한다. 조직·주체·
issuer/audience, 구체 role capability/grant actions·scope·유효기간·revision,
구별 가능한 실물 alias, 단위 있는 decimal baseline, evidence hash·원천
사건/시간, 인간 owner/supervisor/nextAction/nextCheck를 작성한다.
baseRefs는 별도 hash와 문서로 installation bundle에 전달한다. 묵시적
policy allow, wildcard 권한, 합의되지 않은 실제 규제값을 만들지 않는다.
승인 fixture가 필요한 경우 proposal hash/revision·scope·결정자·시각·
유효기간·소비 정책을 baseline에 명시한다. 검증할 승인을 setup으로
대신하지 않는다.

`AcceptanceDriver`의 public Java port는 installFixture/invoke/query/observe/
control/start/await다. 구현체는 실제 서비스·인증·DB/프로세스에 연결하며
수량·적격성·목표/승인 계산이나 업무 상태 저장을 하지 않는다.
`IndependentDbObserver`는 read-only 원 행/실제 query·parameter·mapping
version·snapshot token/isolation·artifact와 완전한 scope를 요구한다.
API projection을 복사한 관찰은 독립 DB 증거가 아니다.

모든 결과는 StepResult의 `driverStatus=EXECUTED|NOT_IMPLEMENTED|
UNAVAILABLE`, data/response, reason, provenance, artifactRefs를 가진다.
이 상태와 서버 업무 outcome은 별개다. 미구현 결과는 data/response=null만
반환한다. 관찰/설치/control/await/parallel child 중 하나라도 미실행이면
전체 제품 case는 `NOT_RUN`이고 확인한 위반이 있으면 `FAIL`이다.
첫 availability assertion만 통과시키고 이후 미관찰을0으로 읽지 않는다.
각 assertion은 source/baseline/unitSource/baselineUnitSource의 실행
상태를 따로 검사한다. 실행된 source의 위반은 다른 source의 미실행에도
FAIL로 보존하고, 미실행 source의 assertion만 NOT_RUN으로 기록한다.
`absent`는 관찰한 object/array 부모에서만 판정한다. null·scalar 부모는
실제 관찰 scope가 아니므로 absence PASS를 만들지 않는다.

`AgentRunner.Scripted`는 선언 typed intent를 실제 도구에 전달한다.
`AgentRunner.ActualClientPort`는 raw userUtterance/permittedContext만 받는다.
actual prompt에 기대 capability/slot/typed intent/oracle를 넣지 않는다.
문장·tool 순서는 고정 답안이 아니며 실제 milestone·효과·책임을 같은
observer/oracle로 검사한다. 현재 actual client 구현과 비용 승인은 없으며
모델 실행은 `NOT_RUN`이다.

## 현재 확인과 제한

Cucumber8.0.4/JUnit6.1.2는 독립 Maven 모듈의 실제 compile/discovery 실행으로
확인한다. Cucumber8 JSON report에는 Jackson2 databind뿐 아니라
jackson-datatype-jdk8가 필요하여 둘 다2.21.2로 고정했다. Cucumber.features
property는 JUnit6 discovery를 중복시키는 관찰이 있어 file/resource selector를
사용한다. dependency install 성공만으로 discovery를 주장하지 않는다.

제품 adapter는 사용자 Step3/S0 이후 연결한다. CAP 후보, PostgreSQL18,
transaction/barrier/worker, source/기관 제출, actual host/model, BTP와 규제
인수는 이 harness selftest가 대신하지 않는다. source 없는 법규나 비용을
fixture/manifest에 확정하지 않는다. runtime artifact가 없으면 준비된
assertion 수와 별개로 제품 gate를 미완료로 남긴다.

## case 작성에 필요한 identity·원행·protocol 계약

`expected`, `scope`, `source.where`, `baseline.where`와 단위 source의
where에는 기존 strict `$alias`/`$result`를 재귀적으로 쓸 수 있다.
fixture alias는 설치 결과의 실제 ID이고 새 object의 ID/revision/hash는
선행 명령 결과에서 가져온다. 선언 자체는 바뀌지 않는다.

```json
{
  "source": {
    "actionId":"after-db", "pointer":"/data/rawRows/relations",
    "where":{"sourceId":{"$alias":"LOT-A"}}, "field":"targetId"
  },
  "expected":[{"$result":{"actionId":"create-work","pointer":"/response/workId"}}],
  "scope":{"workId":{"$result":{"actionId":"create-work","pointer":"/response/workId"}}}
}
```

이 참조는 identity 연결용이다. 관찰 수량·상태·효과를 자기 expected로
복사하면 independent oracle가 무력화된다. expected/where/scope의
`$result` pointer 끝 필드는 아래 case-sensitive allowlist만 허용한다.
접미사나 case-insensitive 추측으로 새로운 필드를 허용하지 않는다. 수량·효과·시간·단위의 기대값은
독립 고정값으로 둔다. field/type 검사는 의미 완전성의 증명이 아니며
잘못 이름 붙인 업무 결과로 oracle를 우회해서는 안 된다. 미실행 source는
NOT_RUN이며 missing/null reference를0이나 임의 ID로 대체하지 않는다.
unknown action과 identity가 아닌 result 참조는 준비 단계에서 거부한다.

observe의 `sources`는 중복 없는 원천 이름 배열이다. 요청한 각 이름의
`data.rawRows[name]`은 실제 원행 object 배열이어야 한다. 빈 배열도
scope 완료와 query 증거가 있어야 한다. 결과의
`data.sourceEvidence[name]`은 다음 계약을 따른다.

```json
{
  "complete":true,
  "rowPointer":"/rawRows/movements",
  "sourceQuery":{
    "statementId":"movement-scope-v1",
    "sql":"SELECT movement_id FROM movement_source WHERE work_id = :workId",
    "parameters":{"workId":"ACTUAL_WORK_ID"},
    "mappingVersion":"observer-v1"
  },
  "artifactRef":"verification/harness/target/evidence/movement-query.json"
}
```

artifactRef는 실제 파일이며 StepResult.artifactRefs에도 있어야 한다.
요청 movements/allocations 중 allocations 원행/완료/query mapping이
빠졌으면 movements가 비어 있어도 전체 관찰이 완료되지 않는다.
위 SQL/ID는 형식 표본이며 실행된 SQL이나 제품 상태가 아니다.

`route=wire`의 invoke/query는 업무 `capabilityId` 대신
`protocolOperation`을 선언할 수 있다. 둘을 동시에 쓰지 않는다.
`protocolOperation="server/discover"` 같은 값은 protocol 분류이며
업무 권한이나 공개 business capability가 아니다. public driver의
`wire(actionId, authenticatedActor, protocolOperation, rawRequest)`는
선언과 다른 실제 method/header/body도 그대로 전달한다. harness가 raw
request를 정상화하거나 일치하도록 수선하지 않는다. 미구현 wire port는
NOT_IMPLEMENTED/null이며 actual transport나 모델을 호출하지 않는다.

실제로 발급된 MRTR state는 `$result`로 재사용하거나 `$transform`으로
한 번 변조한다. transform은 실제 strict result reference에서만 시작한다.

```json
{"$transform":{
  "source":{"$result":{"actionId":"issued","pointer":"/response/state"}},
  "operation":"opaqueByteXor", "index":0, "xor":1
}}
```

opaqueByteXor는 최대65536 UTF-8 bytes 중 printable ASCII 한 byte를
1..127 mask로 XOR하고 printable ASCII를 유지한다. index 범위 오류,
mask0, 가짜 고정 source를 거부한다. JSON state에는
`jsonPointerReplace`+`pointer`+bounded literal `value` 또는
`jsonPointerRemove`+`pointer`를 쓴다. 비어 있지 않은 RFC6901 pointer의
기존 target만 바꾸며 원본 result는 보존한다. input JSON과 replacement는
최대65536 bytes, pointer는1024 characters다. missing target, unknown
operation, 추가 field, nested reference value를 거부한다. 임의 eval/SQL/
업무 계산과 assertion expected 변조는 지원하지 않는다.

registry에는 계획의 최소 공개 행동을 추가했다. `recordRelation`은 허용된
structural typed source/target/relationType/cardinality/evidence만 다루며
WorkLink·ledger·approval 불변식의 우회통로가 아니다. `createWorkLink`는
DEPENDS_ON/CONTRIBUTES_TO/SHARES_ACTIVITY 관계와 cycle·물량 기여 불변식을 검증한다.
`convertUnit`은 승인된 item/from/to unit value의 read이며 stock/order
효과0이다. 독립 catalog의 UnitConversion/WorkLink 의미를 각각 이
capability로 연결한다. `getCommandResult`는 현재 principal/org 권한을
검증한다. `structureIntent`는 해석/NEEDS_INPUT만 반환한다.
`emergencyRepair`는 explicit dryRun/diff·권한/근거·apply/recheck를 요구한다.
endpoint나 production service는 구현하지 않았다. typed predicate는
기존 `getAssessment`의 goal/scope/asOf/knownAt/definition/evaluator
identity로 검증하며 별도 evaluatePredicate surface를 만들지 않는다.

host/runtime 검사는 `control(type=process)`의 명시 operation으로 선언한다.
[host 관찰 계약](host-observation-guide.md)은 input artifact와 생성 output,
실제 process/command/exit/version, 독립 artifact scan과 자율 runtime task
완료 증거를 구분한다. server/discover·skillLoading·modelEvaluation 등의
pseudo label을 public business capability로 추가하지 않는다.


## assertion identity field allowlist

| 실제 pointer 끝 필드 | 허용 실제 값 |
|---|---|
| id, objectId, itemId, lotId, segmentId, workId, activityId, obligationId, ownerId, actorId, principalId, subjectId, organizationId, tenantId, commandId, requestId, proposalId, approvalId, restrictionId, allocationId, movementId, evidenceId, occurrenceId, definitionId, evaluatorId, policyId, grantId, taskId, runId, workLinkId, parentWorkId, childWorkId, supplierId, customerId, shipmentId, invoiceId, externalId, runtimeTaskId, invocationHandle, transactionId, goalVersionId | 비어 있지 않은 string, 최대512 characters |
| ids, workIds, obligationIds | 비어 있지 않은 string ID array, 각 ID 최대512 characters |
| hash, proposalHash, evidenceHash, definitionHash, policyHash, artifactHash, requestHash, inputHash, sha256 | SHA-256 64자리 hexadecimal string |
| revision, proposalRevision, snapshotRevision, definitionRevision, policyRevision, grantRevision, workRevision, approvalRevision | 비어 있지 않은 string(최대512 characters) 또는 nonnegative integer(Long 범위) |

approvalValid/isValid/amountPaid/quantity와 Boolean 업무 결과는 허용하지
않는다. ID/hash field가 number/Boolean/object를 반환해도 실제 값 타입
검사에서 거부한다. `$alias`의 assertion identity도 실제 string ID여야
한다. 버전·단위·업무 상태와 수량 기대값은 계속 독립 고정값으로 둔다.
허용 목록 확장은 계획상 identity임을 확인한 뒤 explicit 계약 변경으로
처리한다. 이름만 바꾼 업무값을 identity로 표시해서 oracle를 우회하지 않는다.


Host 통합은 EXECUTED process control의 ACK 검사 뒤
`HostObservationValidator.validate(validator, resolvedControl, result)`를
직접 호출한다. 단순 ACK·빈 hostObservation은 runtime assertion 전에
거부한다. NOT_IMPLEMENTED/UNAVAILABLE 결과는 사실 없는 null 상태를
유지하며 전체 case를 PASS로 만들지 않는다. clock/barrier 등 다른 control은
각자의 기존 계약을 사용한다. 검증 대상은 실제 실행 증거이며 harness가
process command argv를 실행하지 않는다.


## raw wire 비동기 제출

MRTR single-use 경합은 `start.call`의 route=wire/protocolOperation으로
직접 선언한다. business capabilityId와 protocolOperation은 동시에
쓰지 않는다. schema는 기존 invoke/query wire call을 그대로 사용한다.

```json
{
  "id":"mrtr-left-start", "kind":"start", "evidenceRefs":["wire:async-submission"],
  "call":{
    "id":"mrtr-left-call", "kind":"invoke", "actorRef":"qc", "route":"wire",
    "protocolOperation":"tools/call",
    "request":{"headers":{"Content-Type":"text/plain"},"body":"INTENTIONALLY_HOSTILE_RAW_BODY"},
    "evidenceRefs":["wire:rawRequest"]
  }
}
```

이 action은 typed `AcceptanceDriver.startWire(actionId, actor,
protocolOperation, rawRequest)`로 실제 비동기 제출 ACK를 받는다.
기본 구현은 NOT_IMPLEMENTED/null이다. sync wire()/clientProbe로 대체하거나
동기 완료를 async 제출 ACK로 표시하지 않는다. raw method/header/body는
protocol 분류와 불일치하더라도 수정하지 않는다. 실제 발급 state의
기존 $result/$transform 입력도 이 request 안에 쓸 수 있다.

기존 start→barrier reached/commit→resume→각 await→독립 DB 관찰을
그대로 사용한다. 모든 parallel child start마다 exactly-one await가
필요하며 actual invocationHandle 일치와 completed/terminalStatus를
확인해야 한다. 제출 ACK만 있거나 하나의 await가 미구현이면 전체 완료가
아니다. clientProbe의 불투명한 동작에 raw wire action을 숨기지 않는다.

transactionId와 goalVersionId를 명시 string identity allowlist에 추가했다.
둘은 비어 있지 않은 string(최대512 characters)만 허용한다. number,
Boolean, object와 transactionValid/goalVersionValid 같은 유사 업무명은
거부한다. suffix 허용 범위는 넓히지 않았으며 instanceId/fencingToken은
이번 계약의 assertion identity 목록에 추가하지 않았다.


Assertion은 identity 치환 전에 primary/baseline/unit source와 모든
$result source의 driverStatus를 검사한다. $alias assertion은 CaseRunner에서
실제 installFixture source 가용성도 먼저 확인한다. 미실행 source는
contract RED에서 NOT_IMPLEMENTED/UNAVAILABLE AssertionError, 일반 profile에서
NOT_RUN이며 alias·값을 만들어 채우지 않는다. EXECUTED source의 실제
missing alias/pointer/type는 진짜 contract 오류로 남긴다. 별도 실행된
fixed assertion의 확인된 FAIL은 다른 미실행 source가 덮지 않는다.
