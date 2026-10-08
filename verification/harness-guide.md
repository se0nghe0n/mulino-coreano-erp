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
./verify model --manifest <run-manifest.json>
./verify deployment --manifest <run-manifest.json>
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
- `prepare`: case/assertion/Gherkin 연결, 독립 registry와 규범
  catalog를 검사한다. 준비 PASS는 `PREPARED`, runtime/artifact는
  `NOT_RUN`이며 제품 gate는 닫히지 않는다. semantic oracle equivalence는
  case review가 필요하다. JSON pointer 존재만으로 이를 증명하지 않는다.
  catalog observation의 `artifactKinds`에 `db_snapshot`이 있으면 연결된
  subcase 중 하나 이상이 독립 `observe` 결과를 source/baseline으로 읽는
  assertion을 그 observation 또는 같은 oracle의 sibling observation에
  연결해야 한다. API 응답만으로는 문제로 보고한다.
  coverage assembler와 같은 규칙으로 다음도 preparation 문제로 센다.
  - **필수 profile 도달성**: oracle `requiredLayers`가 요구하는 profile
    (UNIT→contracts, API/DB→scenarios, MCP→mcp, SKILLS→skills,
    MODEL→model, LOCAL/BTP_DEPLOYMENT→local-/btp-deployment,
    REGULATORY_REVIEW→regulatory)에 연결된 assertion이 하나도 없는
    observation은 실행해도 영원히 NOT_RUN이다. `Unreachable required
    profile: <oracle>/<observation> requires <profile> ...`로 보고한다.
    case의 `deployment`는 두 deployment profile로 펼치고, REGULATORY_REVIEW
    observation에 연결된 subcase는 별도 regulatory 증거 profile에도 둔다
    (`CatalogLinkValidator.requiredProfileReachability`, `assemble.py`의
    `declarations`).
  - **규범 lock과 case 자산 검사**: `prepare.json`의 `caseAssetChecks`에
    command·script hash·exit를 남긴다. `verification/requirements/
    validate_catalog.py`(assembler가 쓰는 같은 lock 검증기, 빈·null lock도
    FAIL), `verification/requirements/check_layer_routes.py`(관찰 단위
    MCP·SKILLS 경로, assembler도 같은 검사를 읽는다),
    `verification/cases/V2/cases_b_invariants.py`,
    `verification/cases/check_vocabulary.py --check`(case의 outcome·code·
    의무 kind·감사 field를 공개 vocabulary와 감사 계약으로 검사, assembler도
    같은 `review()`를 읽는다. 아래 "독립 observer의 논리 원행 계약"),
    `verification/cases/T08/bind_observations.py --check`,
    `verification/cases/V7/bind_observations.py V4|V6|V7 --check`,
    `verification/requirements/check_derived_bindings.py`(모든
    observation-bindings.json의 caseHash·catalogSha256 stamp),
    `verification/mcp-tests/test_generators_reproduce.py`와
    `verification/requirements/test_case_generators_reproduce.py`(생성기
    출력과 commit 파일의 byte 동일성, 임시 복사본에서 실행)다. 모두
    저장소를 쓰지 않으며 python이나 script가 없으면 실패한다. 출력의
    `KNOWN_OPEN` 행(owner가 기록된 열린 gap)은 check 기록의 `knownOpen`과
    보고서 `knownOpenGaps`에 옮긴다. 목록은
    `verification/requirements/layer-route-review.json`,
    `check_derived_bindings.py`, `check_vocabulary.py` PENDING의 기록이며,
    닫힌 gap의 항목이 남으면 실패한다.
  - **감사 field와 tick profile**: `ContractValidator.auditFieldProblems`
    (공개되지 않은 감사 source·field·where)와 `runtimeProfileProblems`
    (fixture runtimeProfile과 tick 방식, 자율 loop 패턴, case가 직접 쓴
    watcher `observeFrom`·`naturalTickSeconds`, 수동 관찰 fixture의
    `tickSeconds` 누락·창 절반 초과(NO_TASK는 두 tick을 관찰하고, 관찰 구간
    안에서 완료된 scheduler 주기 기록 `rawRows.schedulerCycles`를 요구한다),
    `verification/host-observation-guide.md`)가
    어긋나면 준비 문제다.
  - **fixture 장소 종류**: `ContractValidator.placeKindProblems`가 subcase
    fixture(baseRefs 포함)의 모든 Place alias에
    `contracts/fixture-place-kinds.json`의 kind(INTERNAL_STORAGE·TRANSIT·
    CUSTOMER·SUPPLIER·EXTERNAL_PORT)를 요구한다. 어휘 밖 kind는
    `kindControl=UNRECOGNIZED_PLACE_KIND`로 선언한 반례만 받는다.
    baseline.places는 alias kind와 같아야 하고, INTERNAL_STORAGE에 있는
    segment는 같은 조직의 Human/Agent alias를 `custodianAlias`로 가져야
    한다. kind 누락·옛 kind(INTERNAL_WAREHOUSE·WAREHOUSE·PORT·TRANSPORT)는
    준비 실패다(step2r round 6).
  - **운송 수령**: `ContractValidator.receiptCustodyProblems`는 fixture
    QuantitySegment(또는 그 leaf를 나눈 splitQuantity의 명시 children 자식,
    `/response/children/<alias>/segmentId`)를 지명하는 confirmReceipt를 운송
    수령으로 본다. leaf는 하나이고 TRANSIT 장소에 있으며 식별돼 있어야
    한다(INDISTINGUISHABLE_MIXTURE 아님). 수령 수량·단위는 leaf와 정확히 같고
    itemId·lotId는 leaf와 같으며 수령 장소는 INTERNAL_STORAGE다. 일부만 받으면
    먼저 split한다(계획 §4.2, 제품 `ReceiptStockPrimitives.receive`). 운송
    수령의 실물을 뒤에서 쓰면 leaf 보관자가 내부 보관자여야 한다
    (`contracts/fixture-place-kinds.json` transitReceipt, step2r round 8).
  - **직접 수령의 보관자**: 그 밖의 confirmReceipt(직접 수령)의 실물을 뒤에서
    예약·배분 교체·pick·출고·이동하는 subcase에 `receivingCustodianId` slot을
    요구한다. slot은 확인한 actor와 같은 조직이며 confirmReceipt 권한과 수령
    장소 scope를 가진 내부 Human/Agent fixture actor여야 한다. 수령의 canonical
    occurrence를 검증한 basis 원본이 같은 alias를 지명해야 한다. basis 원본은
    `verificationBasisSlots`(evidenceId·evidenceIds·verifiedEvidenceIds)의
    DocumentVersion alias(`fixtureContent.receivingCustodianAlias`, evidence
    sha256은 canonical content의 hash)와, 그 slot이 `$result`로 인용한 앞선
    action이 첨부한 문서(그 action의 document·basis slot alias와 inline JSON
    content), 그리고 같은 `canonicalOccurrenceKey`의 앞선 confirmReceipt의
    basis다. 요청 evidenceRefs는 증인이라 세지 않는다(제품은 occurrence의
    검증된 chain에서만 보관자를 읽는다, round 9). 같은 수령의 모든
    confirm은 같은 slot을 보내고, 운송 수령은 slot을 보내지 않는다(directReceiptCustody,
    step2r round 7·8).
  - **보관자 반례 선언**: slot을 가진 confirmReceipt action에
    `custodyControl`(FORBIDDEN·SCOPE_INELIGIBLE·EVIDENCE_CONFLICT·
    EVIDENCE_UNVERIFIED)을 두면 위 검사를 뒤집는다. 선언 값은 제품이 처음
    걸리는 검사여야 한다(확인한 조직의 actor 아님·다른 조직 FORBIDDEN →
    Human/Agent·권한·장소 SCOPE_INELIGIBLE → basis 원본끼리 상충 → basis
    원본이 slot을 지명하지 않음). subcase는 그
    action의 `/response/outcome`(REJECTED 또는 HELD)과
    `/response/error/code`를 고정하고, 뒤의 observe에서
    `/data/rawRows/segments`나 `/data/rawRows/receipts`의 count 0으로 효과
    0을 단언한다. 그 수령의 결과를 뒤에서 쓰지 않는다. 이 field는 harness
    선언이며 제품에 보내지 않는다. 예: E1 `receipt-custody-unverified`.
  - **pick 뒤 출고**: `ContractValidator.pickBeforeDispatchProblems`가 모든
    dispatchQuantity의 pick 상태를 검사한다(step2r round 9). 제품은 pickedAt
    없는 배분의 출고를 INVALID 'Pick before dispatch required'로 거부하고
    (`FulfillmentCommands`), pick만 pickedAt을 기록하며 두 번째 pick은
    'Allocation already picked'로 거부하고 배분 revision을 올린다. adapter는
    pick을 만들지 않는다(아래 "adapter가 … 암묵적으로 생성하지 않는다").
    - fixture 배분(`$alias`의 Allocation)은 설치 상태다. fixture가 그 배분의
      state를 적은 자리(alias, `baseline.priorEntities`, `baseline.allocations`/
      `allocation` 행)에 `pickedAt`(fixture clock knownAt 이전의 ISO instant)과
      `pickedByAlias`(fixture actor)를 선언하거나, 앞선 pickQuantity가 그
      alias를 pick해야 한다. 선언된 pick 뒤에 실패로 고정하지 않은 pick이 또
      있으면 문제다. FixtureInstaller가 pickedAt을 설치한다(Step 3).
    - 실행 중 배분(앞선 action의 `$result`)의 출고가 적용될 것으로 기대되면
      (`/response/outcome` APPLIED 고정, 또는 뒤의 action·assertion이 결과의
      다른 부분을 읽음) 같은 `$result`(actionId·pointer)를 지명하는 앞선
      pickQuantity가 있어야 한다.
    - pick되지 않은 실행 중 배분의 출고는 APPLIED 밖 outcome과, 제품이 pick
      검사 전에 내는 code(STALE_REVISION·FORBIDDEN·
      INSUFFICIENT_ELIGIBLE_QUANTITY·SCOPE_INELIGIBLE·VERSION_UNSUPPORTED)를
      고정한 반례일 때만 받는다. 아니면 검사 대상 규칙이 없는 제품도 pick
      누락만으로 거부해 통과한다.
    - 앞선 pick의 outcome을 APPLIED 밖으로 고정하면 안 되고, 출고
      expectedRevision이 pick보다 앞선 action의 `$result`이면 stale
      revision이라 문제다. 비동기 출고(start의 call)의 고정은 그 await
      action의 assertion에서 읽는다.
  - **Streamable HTTP transport header**: `ContractValidator.wireTransportProblems`가
    `route=wire`·`transport=streamable-http` 요청의 Accept가
    `application/json`과 `text/event-stream`을 모두 나열하기를 요구하고
    Origin을 금지한다. 문자열 일치가 아니라 media type 비교다(순서·공백·
    대소문자·parameter 무관, `q=0` 범위와 wildcard는 나열이 아님, round 7).
    예외는 그 action의 `/response/httpStatus`를 406(Accept) 또는
    403(Origin)으로 고정한 transport 반례 하나뿐이다
    (`contracts/mcp/s0-protocol.md`).
  - **정의되지 않은 host 조작**: `type=process` control의 operation이
    `contracts/acceptance-host-observation.schema.json`의 operation enum에
    없으면 문제로 센다(`ContractValidator.hostOperationProblems`).
  - **실행 경로가 없는 증거**: 보고서 `runtimeGates`에 이 harness가 만들
    수 없는 필수 증거를 이름으로 남긴다. 현재는 regulatory profile(T15
    REGULATORY_REVIEW 관찰 3개)이다. `./verify regulatory`는 exit2
    `NOT_RUN_GATED`만 보고하고 receipt를 쓰지 않는다.
  - **비정규 오류 pointer**: 아래 "응답 오류 envelope" 절.
- `coverage`: `verification/model-binding/run prepare` 뒤
  `python3 verification/coverage/assemble.py --check-preparation`을 실행하고
  assembler의 exit code(0 PASS, 1 FAIL, 2 NOT_RUN, 3 형식 오류)를 그대로
  돌려준다. assembler가 새 `./verify prepare`를 직접 실행하고 receipt·
  artifact bytes를 다시 읽는다. `--index <file>`로 실제 실행 index를 준다
  (기본은 빈 `verification/coverage/runtime-evidence-index.json`). Java
  `Main`의 `coverage` mode는 없다(형식 오류). 이전에는 `./verify coverage`가
  준비 보고만 만들고 exit0을 냈다.
- `schema`, `contracts`, `scenarios`, `recovery`, `mcp`, `skills`, `model`,
  `deployment`: 실제 adapter가 없는 현재는 `NOT_RUN`, exit2다. 선행
  profile과 미완료 gate를 보고한다. 모델·유료 배포를 호출하지 않는다.
  보고서에는 assembler가 읽는 `discovered`(저장소가 이 profile에 선언한
  subcase 수)·`started`·`completed`(실행한 수)와 `skipped=0`,
  `explicitCaseSelection`, 첫 case 전의 `preRun`(HEAD·git status·시각)이
  있다. case 파일을 명시한 실행은 부분 실행이라 `discovered`가 실행 수보다
  크고 `gateComplete=false`다. `gateComplete`는 이 profile 자체의 gate다.
  case 파일을 명시하지 않은 실제 driver(`--actual`) 실행에서 선언된 모든
  subcase가 시작·완료되고 PASS일 때만 true다. 선행 profile은
  `prerequisiteRuntimeComplete=false`로 남기고 assembler가 합성한다.
- `--actual`의 schema~skills profile 실행은 coverage receipt를 만든다
  (`ExecutionReceiptProducer`). 조건을 하나라도 못 채우면 receipt를 쓰지
  않고 stdout `COVERAGE_RECEIPT {"status":"NOT_EMITTED",...}`로 이유를 낸다.
  조건: 실제 driver, 모든 case가 PRODUCT 정책, 실행 action provenance의
  label 값(`source`·`adapter`·`adapterVersion`·`buildVersion`·
  `snapshot.isolation`·`sourceQuery.mappingVersion`)에 selftest/canned/
  stub/fake/captured/unimplemented 표지 없음(key나 `capturedAt` 같은 data는
  보지 않는다), command가 profile 이름을 포함하고 case 파일 명시가 없음,
  Main이 첫 case 전(`preRun`)과 실행 뒤에 직접 본 working tree가 같은
  HEAD에서 clean, `ACTUAL_BUILD_COMMIT`=기록 commit,
  `ACTUAL_SCHEMA_VERSION`·(scenarios 이후)`ACTUAL_DB_VERSION`·(mcp/skills)
  `ACTUAL_MCP_PROTOCOL_VERSION`. receipt는 `workingTreeObservations`
  (before/after)와 `buildIdentity`를 남긴다. backend가 build-info를
  노출하지 않으므로 build commit은 관찰이 아니라 선언이며
  `buildIdentity.source=DECLARED_ACTUAL_BUILD_COMMIT`로 그렇게 적는다.
  산출물은 `target/evidence/<profile>-receipt.json`, envelope 문서
  `target/evidence/receipts/<profile>-<runId>/`, 갱신된
  `target/evidence/actual-runtime-evidence-index.json`이다. 실행 identity의
  `hostId`·`actorId`는 `ACTUAL_HOST_ID`·`ACTUAL_EXECUTION_ACTOR`(없으면
  hostname·OS 사용자)다. harness selftest·RED·unimplemented driver는 이
  경로에 오지 않는다. receipt는 무결성·연결 증거이며 adapter 뒤 시스템이
  진짜라는 attestation이 아니다. 형식은 `verification/coverage/README.md`.
- `model|deployment --manifest <path>`(또는 `--manifest=<path>`):
  `contracts/acceptance-run-manifest.schema.json`의 실행 manifest를 검증한다.
  manifest를 case 파일로 읽지 않는다. 보고서 `runManifest`에 path·sha256·
  승인 증거 유무를 남긴다. 승인 증거가 있어도 이 harness는 유료 모델이나
  배포를 실행하지 않으므로 `NOT_RUN`, exit2다. manifest 없음은 `ABSENT`의
  `NOT_RUN`이다. 다른 profile의 `--manifest`, 값 없는 `--manifest`,
  profile 불일치, 알 수 없는 `--` option은 exit3 형식 오류다.
- `--agent-runner=scripted|actual`: agent action의 runner를 고른다.
  기본은 `scripted`(SIT)다. `actual`은 ServiceLoader로 설치된
  `AgentRunner.ActualClientPort`가 정확히 하나여야 하며 없으면 exit3이다.

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

`requiredAdapters`는 계약이다. 이름은 `contracts/acceptance-capabilities.json`
의 `adapterAliases`로 정규화한다(`actualClient→client`,
`independent-db-observer→db`, `process-control→process`). 제품 실행
(PRODUCT 정책)에서 driver `availableAdapters()`와 agent runner가 공급하지
않는 adapter가 남으면 그 subcase는 확인된 FAIL이 없는 한 `NOT_RUN`이다.
evidence의 `missingAdapters`에 이름을 남긴다. `client`/`model`은 실제
client runner(UAT)만 공급하며 scripted runner는 대신하지 않는다.
이런 UAT 전용 subcase의 agent action은 typed `intent` 없이
`userUtterance`/`permittedContext`만 둘 수 있다. scripted SIT로 실행되면
port를 호출하지 않고 `NOT_IMPLEMENTED`로 남는다. UAT 전용이 아닌 agent
action은 공개 registry의 `intent.capabilityId`가 필수다. evidence의
`agentRunner`/`agentActionRunners`는 실제 사용한 runner를 기록한다.

각 assertion은 다음을 반드시 가진다.

```json
{
  "id": "warehouse-held-80",
  "op": "sumEquals",
  "source": {"actionId": "after-db", "pointer": "/data/rawRows/segments",
             "where": {"placeId": {"$alias": "W"}, "active": true}, "field": "quantity"},
  "expected": "80",
  "unit": "BOX",
  "unitSource": {"actionId":"after-db", "pointer":"/data/rawRows/segments",
                 "where": {"placeId": {"$alias": "W"}, "active": true}, "field": "unit"},
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
`/response/...`, DB observation은 원행 `/data/rawRows/<source>`다.
수량 primary는 원행의 `sumEquals`/`count`와 고정 `where`로 둔다.
`/data/data/...`는 observer가 `derivations`에 고정 문법으로 선언하고
harness가 rawRows에서 다시 계산해 일치한 값만 허용한다. 각 scalar leaf의
JSON pointer(예 `/inventory/heldQuantity`)마다 `rowPointer`(요청 source),
literal `where`, `aggregate=sum|count|single|distinct`, `field`,
선택적 `unitField`를 둔다. 선언 없는 값, 재계산과 다른 값, 배열·null
값은 형식 오류다. API projection을 복사한 숫자는 통과하지 못한다.
`/provenance`, `/reason`, `/artifactRefs`, `/driverStatus`, `/actionId`는
driver가 스스로 쓴 요청 측 metadata라 oracle 원천이 아니다.
`./verify prepare`가 이런 source를 문제로 보고한다. 예를 들어
`provenance.authenticatedActor`는 harness가 서명을 요청한 주체다.
서버가 검증한 주체는 감사·command 원행처럼 서버가 기록한 독립 관찰에서
읽는다. assertion `scope`는 추적용 선언이며 harness가 원행을 거르지 않는다.
실제 선택은 `source.where`와 observe 요청의 scope로만 강제한다.
evidence는 `declaredScope`와 `scopeEnforced:false`를 남긴다. 배열 선택에는
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
같아야 한다. API의 logical read revision과 observer의 MVCC snapshot은
다르다. `snapshotRef`는 셋 중 하나다.

- `$result`로 받은 API `snapshotRevision`: harness는 이 값을 observer에
  넘기지 않는다. observer 요청에는 `snapshotRef=RESULT_REVISION`과
  `snapshotSource`(그 revision을 발급한 action의 id·pointer·kind·route·
  capabilityId·actorRef·fixture actor·해석된 request)가 간다. observer는
  권한 범위의 원행에서 projection revision을 재계산해
  `data.snapshotRevision`에 내고, harness가 보관한 발급 값과 비교한다.
  `data.snapshot.readMode=RESULT_REVISION`과 재계산 query
  `snapshot.revisionQuery`가 필수다. 요청에 값이 없으므로 복사(echo)는
  불일치로 드러난다. 현재 actual observer(`ObserverSnapshot`)는 이
  재계산을 구현하지 않아 `NOT_IMPLEMENTED`(NOT_RUN)다.
- `$result`로 받은 host runtime snapshot id(`awaitRuntimeTask` control의
  `/data/hostObservation/runtimeTask/snapshot/id`): projection revision이
  아니므로 DB 원행에서 재계산할 수 없다. observer 요청에는
  `snapshotRef=RUNTIME_TASK_SNAPSHOT`과 `snapshotSource`(발급 action의
  id·pointer, `operation=awaitRuntimeTask`, 해석된 `schedulerId`·`taskId`·
  `invocationHandle`·`scope`)가 간다. snapshot id 자체는 보내지 않는다.
  observer는 그 task의 host snapshot artifact를 스스로 찾아 읽고
  `data.snapshot.readMode=RUNTIME_TASK_SNAPSHOT`과
  `snapshot.runtimeTaskSnapshot={schedulerId, taskId, invocationHandle,
  snapshotId, artifactRef, sha256}`을 낸다. harness는 schedulerId를 요청과,
  taskId·invocationHandle·artifactRef를 await 결과의 `runtimeTask`와,
  snapshotId를 보관한 발급 값과 비교하고 artifact bytes의 SHA-256을 직접
  계산해 대조한다. 그 artifact는 이 관찰의 `artifactRefs`에 있어야 하고
  observer의 `snapshot.capturedAt`은 task `completedAt`보다 이르면 안 된다.
  `snapshotRevision`은 observer 자신의 값이다. 현재 actual observer는 이
  mode도 구현하지 않아 `NOT_IMPLEMENTED`(NOT_RUN)다.
- literal directive `CURRENT_COMMITTED`/`CURRENT_LOCK_WAIT`: 앞선 action이
  끝난 뒤의 새 read다. `readMode`가 directive와 같아야 하고
  `snapshotRevision`은 observer 자신의 값이다. 다른 literal은 schema가
  거부한다.

`./verify prepare`(`ContractValidator.snapshotRefProblems`)는 `$result`
snapshotRef가 invoke/query/start의 `/response/snapshotRevision`이거나
`awaitRuntimeTask` control의 `/data/hostObservation/runtimeTask/snapshot/id`인
경우만 받는다. 다른 pointer는 RESULT_REVISION으로 보내져도 원행에서 재계산할
수 없어 올바른 제품도 통과하지 못하므로 준비 문제다.

모든 경우 `data.snapshot.id`는 observer 자신의 DB snapshot token
(예 `pg_current_snapshot()`)이다. 요청 참조나 directive와 같으면 echo로
거부한다. 같은 subcase에서 `snapshot.capturedAt`은 앞선 관찰보다 이르면
안 된다. `data.snapshot`/`sourceQuery`는 provenance의 실제 값과 같아야 한다.
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

Adapter metadata 조회가 cancellation을 무시해 늦게 반환해도 dispatch
직전에 중단 상태를 다시 확인하여 새 port 호출을 차단한다. port에 걸친
lock은 잡지 않는다. 중단 검사와 호출 사이의 경쟁이나 이미 전송된 원격
효과를 interrupt만으로 취소했다고 주장하지 않는다. 실제 adapter는
자체 deadline과 원격 효과 대조를 제공해야 한다.

`route=wire`의 request는 raw HTTP header/body, content type, protocol,
JSON-RPC ID, MRTR state/inputResponses/effect key를 그대로 전달할 수 있다.
harness가 공격 입력을 정상화하거나 header/body mismatch를 수선하지
않는다. 그래서 Streamable HTTP 요청의 `Accept`·`Origin`은 case가 계약대로
쓴다. adapter가 빠진 Accept를 보충하지 않는다(위 prepare의 transport
header 검사). 이전 wire result의 state도 `$result`로 추출한다. adapter의 실제
artifact는 Authorization/Cookie/token/비밀을 redact하고 원문 hash와
검증된 인증 metadata를 따로 둔다. committed artifact에 credential 원문을
저장하지 않는다.

## fixture와 실제 실행 port

fixture는 전체 `acceptance-fixture.schema.json`을 따른다. Place.kind와
보관자 내부성은 [fixture 장소 종류 계약](../contracts/fixture-place-kinds.md)을
따른다. 가상 정책임을
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
미실행 action의 `$result`나 미설치 fixture의 `$alias`를 입력으로 쓰는
후속 action은 port를 호출하지 않고 `NOT_IMPLEMENTED`로 남는다. 일부
adapter만 있는 driver가 환경 오류(exit3)로 전체 profile을 끊지 않는다.

`CaseRunner` 기본 정책은 PRODUCT다. 실행된 결과의 provenance
`source`/`adapter`/`adapterVersion`/`buildVersion`에 `SELFTEST`,
`CAPTURED`, `CANNED`가 있으면 제품 증거가 아니므로 거부한다(exit3).
process control은 `ACTUAL_HOST`만 받는다. captured port를 쓰는 harness
단위 시험만 `CaseRunner.harnessSelftest(...)`를 쓰며 그 결과는 제품 인수로
세지 않는다. Gherkin actual 실행은 첫 실패 뒤 남은 assertion을 모두
평가하고 구조화된 `failureKind`(`SOURCE_UNAVAILABLE`/`VIOLATION`)로
`NOT_RUN`/`FAIL`을 정한다. 오류 문구 검색으로 분류하지 않는다.
첫 availability assertion만 통과시키고 이후 미관찰을0으로 읽지 않는다.
각 assertion은 source/baseline/unitSource/baselineUnitSource의 실행
상태를 따로 검사한다. 실행된 source의 위반은 다른 source의 미실행에도
FAIL로 보존하고, 미실행 source의 assertion만 NOT_RUN으로 기록한다.
`absent`는 관찰한 object/array 부모에서만 판정한다. null·scalar 부모는
실제 관찰 scope가 아니므로 absence PASS를 만들지 않는다.

`AgentRunner.Scripted`는 선언 typed intent를 실제 도구에 전달한다.
typed intent가 없으면 만들어 내지 않고 `NOT_IMPLEMENTED`를 반환한다.
`AgentRunner.ActualClientPort`는 raw userUtterance/permittedContext만 받는다.
actual prompt에 기대 capability/slot/typed intent/oracle를 넣지 않는다.
문장·tool 순서는 고정 답안이 아니며 실제 milestone·효과·책임을 같은
observer/oracle로 검사한다. 현재 actual client 구현과 비용 승인은 없으며
모델 실행은 `NOT_RUN`이다.

## 독립 observer의 논리 원행 계약

observer가 `/data/rawRows/<source>`로 돌려주는 행의 이름은 case마다 정하지
않는다. 공통 계약으로 고정한다. 같은 원천을 case마다 다른 이름으로 읽으면
올바른 제품도 어느 한쪽 case에서 실패하기 때문이다.

### 감사 원행(audit·queryAudit)

[`contracts/audit-observation-fields.json`](../contracts/audit-observation-fields.json)
(설명은 같은 이름의 `.md`)이 감사 원행의 SSOT다. source는 둘이다.

| source | 의미 | 제품 원천 |
|---|---|---|
| `audit` | 명령 감사. rollback 뒤 따로 commit한 거부 감사와 replay 감사를 포함한다 | `CommandAudits` ⋈ `CommandRecords`(commandId) |
| `queryAudit` | 조회 감사. 거부된 조회 포함. 계획 §7.4 "조회 감사는 업무 상태 변경과 구별한다" | 아직 없음(PENDING_PRODUCT, Step 3) |

field마다 presence가 있다. `ALWAYS`는 모든 행에 값이 있고, `CONDITIONAL`은
조건이 맞는 행에만 있다(예: `errorCode`는 outcome이 APPLIED·
ACCEPTED_PENDING_EXTERNAL이 아닐 때). `PENDING`은 계획이 요구하지만
backend가 아직 기록하지 않는 내용이다. case는 이 이름으로 읽고, 구현은
Step 3 추가 요청이다. `AssertionEngine`은 `where` key가 source의 모든 행에
있어야 하므로 `where`에는 ALWAYS field만 쓴다. CONDITIONAL·PENDING field는
ALWAYS field로 고른 행에서 `field`로 투영한다. outcome 값은
[domain vocabulary](../contracts/domain-vocabulary.md)의 명령 outcome이다.
오류 code를 outcome 자리에 쓰지 않는다(FORBIDDEN 거부는
`outcome=REJECTED`, `errorCode=FORBIDDEN`). `queryAudit.outcome`은 READ 또는
REJECTED다. 이전 이름(`commandKey`, `action`, `result`, `kind`, `id`,
`denialAudit` source 등)은 계약의 `retiredNames`가 새 이름으로 옮긴다.

`./verify prepare`는 두 곳에서 이 계약을 강제하고, 어긋나면 준비 문제로
보고한다.

- `ContractValidator.auditFieldProblems`: 감사와 비슷한 이름의 observe
  source나 `/data/rawRows/<source>` pointer가 공개된 source가 아니면 거부한다.
  pointer 경로의 field, `source.field` 투영, `fieldsPresent` 기대 field가
  그 source의 공개 field가 아니어도 거부한다. `where` key가 공개되지
  않았거나 ALWAYS가 아니어도 거부한다. source·baseline·unitSource·
  baselineUnitSource에 같은 규칙을 적용한다. notEquals·absent처럼 값을
  금지하는 assertion도 공개 field 이름을 써야 한다.
- caseAssetChecks `vocabulary`(`verification/cases/check_vocabulary.py
  --check`): outcome·오류 code·code/outcome 짝·의무 kind와 감사 where의
  presence·outcome 값을 vocabulary와 이 계약으로 검사한다. 아직 vocabulary에
  없는 이름은 PENDING 목록에 계획 근거와 함께 둔다. 그 항목은
  `KNOWN_OPEN` 줄로 출력되고, prepare.json `knownOpenGaps`와 coverage
  manifest `knownOpenGaps`(assembler가 같은 `review()`를 부른다)에 owner와
  함께 남는다. 쓰지 않는 PENDING 항목은 그 자체로 문제다.

### observer 파생 원천(V2 promiseCoverage)

어떤 rawRows source는 제품 표가 아니라 observer가 같은 snapshot의 다른
원행으로 만드는 파생이다. 파생은 아래 조건을 지킨다. 원천 행은 같은
observe의 rawRows에 함께 두고, SQL·parameter·mapping version은
sourceEvidence에 남긴다. 값은 원행에서 그대로 복사하고 다시 계산하지
않는다. 제품 응답이나 API projection을 읽지 않는다. case는 파생에만 기대지
않고, 원행 하나 이상을 직접 읽어 파생과 대조한다.

`promiseCoverage`(V2 `actual50-*`, 계획 §4.2 부족 의무와 대체 배분)의
규칙은 다음과 같다. 상세는 `verification/cases/V2/race-observation-contract.md`다.

| coverageKind | 원천 행 | sourceId | 복사하는 값 |
|---|---|---|---|
| `EXECUTABLE_ALLOCATION` | `allocations`에서 state=EXECUTABLE·active인 현재 배분 | allocation ID | promiseRootId·quantity·unit |
| `SHORTAGE_OBLIGATION` | `obligations`에서 current=true·status=OPEN이고 같은 약속 root에 연결된 부족 의무 | obligation ID | promiseRootId·quantity·unit |

한 원천 행은 한 번만 나온다. 같은 행을 두 번 세거나 빠뜨리면 약속 수량
합이 틀려 case가 실패한다. V2는 `shortage-obligation-row-quantity`로 부족
의무 원행(정정 응답의 obligationId)의 quantity·unit을 직접 읽는다.
promiseCoverage 행은 harness가 rawRows에서 다시 계산하는
`/data/data` derivation이 아니다. 원행 조합 규칙은 이 계약과 위 대조
assertion으로 고정한다. 현재 actual observer는 이 파생을 구현하지 않아
NOT_RUN이다(Step 3 `actual/` 소유).

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

## 응답 오류 envelope과 의무 행 유효성

명령 응답의 오류 코드는 하나의 envelope에만 있다.
`contracts/command-response.schema.json`의 `/error/code`(대문자 구조 코드)이며
case는 api/mcp invoke와 그 await 결과에서 `/response/error/code`로 읽는다.
`REJECTED`·`CONFLICT`는 `error`를 반드시 가지며 응답 최상위 `errorCode`·
`code`는 계약이 금지한다. raw MCP wire의 tool 결과는 같은 envelope을
`/response/body/result/structuredContent/error/code`로 읽는다. JSON-RPC
protocol 오류(`/response/body/error/code`, 정수 -32xxx)는 공식 MCP의 다른
envelope이라 허용한다.

`./verify prepare`는 assertion source/baseline이 `/response/code`이거나
`/response` 아래 마지막 segment가 `errorCode`·`error_code`인 pointer를
`... is not the canonical error envelope /response/error/code`로 거부한다
(`ContractValidator.errorPointerProblems`). 이런 assertion은 계약을 지킨
제품에서 실패하고 계약을 어긴 제품에서 통과하기 때문이다.
2026-10-08 `d21aee7c` 기준 남은 위반은 C1 3, T03 2, T04 11, T05 2,
T16 3(`/response/errorCode`), T18 1(`/response/code`)의 22개다. 각 case
소유자가 고친다.

의무 원행(`obligations`)과 판정 원행(`assessments`)의 `current`는 그 행이
현재 유효한 revision인지(대체·정정되지 않았는지)를 뜻한다. 상태(`status`:
OPEN·RESOLVED·WAIVED 등)와 독립이다. 해소된 의무도 대체되지 않았으면
`current=true`이고, 정정으로 대체된 과거 판정은 `current=false`로 남는다.
따라서 "현재 열린 의무"는 `{"current":true,"status":"OPEN"}`처럼 두 조건을
함께 쓴다. `current`만으로 미해결을 뜻하게 쓰지 않는다(C4·E1이 이 의미다).

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


## runtime assertion 기록

`CaseRunner`의 assertion 기록(`evidence().assertions[]`)은 판정 결과만이
아니라 선언과 비교 값을 함께 남긴다. `op`, 선언 `unit`·`baseline`·
`unitSource`·`baselineUnitSource`, `source.where`·`source.field`를
`where`·`field`로, 참조를 푼 `resolvedExpected`·`resolvedWhere`, 그리고
where/field projection 뒤 실제 비교한 `observed`·`observedUnit`·
`observedBaseline`이다. 부재(`absent`)는 `{"observation":"ABSENT"}`로 쓴다.
coverage assembler는 PASS 기록의 op/unit/where/field가 case 선언과 같은지,
`observed`가 capture된 action bytes를 같은 규칙으로 projection한 값과
같은지 다시 확인하고 operator를 재적용한다. `$result`는 capture bytes에서,
`$alias`는 실행된 installFixture의 `aliasMap`에서 푼다. bytes로 정할 수
없는 경우에만 기록된 값으로 operator를 재적용하고, 그것도 안 되면
review에 남긴다. 기록이 없거나 다르면 FAIL이다.

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
