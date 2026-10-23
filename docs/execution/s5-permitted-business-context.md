# S5 현재 권한의 업무 context read

현재 제품의 인가된 조회를 실제 MCP로 실행해 모델 입력용 업무
context를 구성한다. 이름이 같은 대상의 서로 다른 ID, 수량과 단위,
Work·증거·의무의 원 응답을 보존한다. fixture·corpus·binding·oracle나
예상 capability를 업무 사실로 복사하지 않는다.

작업 경로는
`/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-7fc2158f`이며
branch는 `feat/s5-permitted-business-context`다. 시작 baseline은
`a7f18cc8e7124b9fd1cc8bf811687ed7230a4981`이다. 2026-10-24 coordinator의
명시적 지시로 이미 Task에 통합된
`ce9afb325df609b129a721636a6d10e738fd83e9`를 ff-only로 반영했다.
자기 변경을 보존했으며 다른 worker의 미통합 변경은 가져오지 않았다.

## 변경 경계

새 helper는 `NativePermittedContext`와 `NativePermittedContextCapture`다.
기존 파일에서는 `NativeProductIntegration.ProductDriver.query`와
`send`만 수정한다. installFixture·observe·requireReady·readiness·session,
`NativeMcpTransport`, backend source·공개 query schema는 수정하지 않는다.
별도 fixture installer·T17·RuntimeService worker의 소유 범위도 유지한다.

개발용 `ontology-agent-implementation` skill, AGENTS.md, 설계 철학과
구현 계획 전체를 읽고 현재 source의 조회·현재 인가 계약에 연결했다.
이 산출물은 모델 업무 실행이나 Task·사용자 Step·S5/S6 종료 증거가 아니다.

## harness의 context 요청과 실제 제품 read

`getObject(view=ACTOR_PERMITTED_BUSINESS_CONTEXT)`는 harness 내부 요청이다.
입력은 view, scope, asOf, knownAt 네 필드로 제한한다. scope는
organizationId, installationId, caseId, turnId만 받는다. 설치 때 받은
조직·installation과 일치해야 한다. case/turn/installation은 capture
상관관계이며 제품 권한이나 업무 대상 selector로 쓰지 않는다.

제품에는 view와 이 세 correlation ID를 보내지 않는다. 같은 인증
주체로 아래의 기존 공개 read만 보낸다.

- 지원되는 26개 noun type의 `searchObjects`를 ID/cursor 순서로 읽는다.
  동명 대상을 ID별로 유지하며 하나를 임의 선택하지 않는다.
- `searchWorks`와 발견한 Work의 `getWork`를 읽는다. 발견한 TradeItem의
  `getObject`도 읽어 대상·현재 Work·의무·증거 연결을 보존한다.
- DOCUMENT/EVENT/CLAIM/CANONICAL 네 종류의 `getEvidence`와 조직 범위의
  `getInventory`를 읽는다. 결과는 제품이 현재 권한으로 걸러 준 값이다.

페이지 상한20, 전체 read 상한1024를 넘으면 실패한다. cursor 반복,
중복 ID page, 다른 조직 응답, 요청과 다른 시각, 불완전 응답도 실패한다.
FORBIDDEN은 읽을 수 있는 사실로 넣지 않는다. 그 밖의 제품 오류를
빈 목록이나 성공으로 바꾸지 않는다. 마지막 anchor가 거부되면 이전
성공 read를 재사용하지 않는다.

`permittedContext.reads[]`에는 각 실제 request와 response를 보존한다.
서버의 unknowns/conflicts/evidenceRefs도 유지한다. 모델 입력 전에 기존
`ModelBindingRunner.checkContext`와 비밀 검사를 적용한다. 누설 금지
필드를 제거해서 성공시키는 수선은 하지 않는다.

## 실제 discovery의 남은 범위

현재 `PurchaseQueries`와 `SettlementQueries`는 ID가 필요하다.
실제 `searchObjects(PurchaseProposal)`는
`TYPE_INVALID: Purchase ID required`를 반환했다. PurchaseProposal,
PurchaseOrder, Invoice의 목록을 지원한다고 표시하지 않는다.
RegulatoryProcedure의 목록은 완전한 cursor 계약이 없어 자동 열거하지
않는다. 이 네 noun의 discovery 공백을 context에 명시한다.

이 공백을 해소하려면 backend 소유자가 각 provider의 현재 scope·인가,
안정된 ID/cursor·knownAt 계약을 제공해야 한다. 이번 변경은 제품
schema를 느슨하게 하거나 fixture에서 해당 ID를 가져오지 않는다.

## snapshot과 인증 capture

context는 여러 HTTP read의 모음이다. 각 read의 실제 revision과 시각을
보존하며 하나의 원자적 DB snapshot이라고 표시하지 않는다.
`StepResult.data.snapshotRevision`은 실제 anchor read의 값이다.
`projectionReadRef`는 그 read의 원 request/response와 인증 주체를 담은
captured-api artifact를 가리킨다. 합친 context를 hash해 제품 revision을
만들거나 projection response를 재구성하지 않는다.

가능하면 Place/Product/Manufacturer의 실제 단일 `getObject`를 anchor로
택한다. 허용되는 단일 대상이 없으면 마지막에 `getInventory`를 다시
읽는다. 후자의 독립 projection recipe는 기존 observer의 별도 공백이며
단순 noun의 독립 proof 성공으로 이를 대신하지 않는다.

서버가 이미 검증한 `SERVER_AUTHENTICATED_MCP_CAPTURE_V1` API를 소비한다.
artifact SHA, 원 request/response, RPC/tool, 조직과 서버 주체를 대조한다.
payload의 actor·role·grant는 이 주체와 grant의 출처가 아니다.
서버가 현재 관찰한 활성 grant 후보가 해당 capability에 정확히 하나일
때만 그 ID를 기록한다. 여러 후보 중 사용 grant를 추측하지 않는다.
hook의 `actionAuthorization=NOT_ASSERTED` 의미도 유지한다.

실제 capture를 모두 갖춘 결과만
`source=ACTUAL_AUTHENTICATED_TRANSPORT`로 반환한다. capture가 없거나
불일치하면 업무 data/response가 없는 NOT_IMPLEMENTED와 원 wire
진단을 반환한다. unit double은 `CAPTURED_CONTRACT_SELFTEST`로 남긴다.
실제 SQL 관찰·모델 응답·전체 model-binding 관찰을 서로 대신하지 않는다.

## 한정 실행 증거

원 증거는 이 worktree의 ignored 경로
`.local/s5-permitted-context/proof/`와
`verification/harness/target/native-integration/`에 보존한다.
자기 worktree는 coordinator가 통합·증거 회수할 수 있게 유지한다.
원 token·개인 키·DB password는 증거에 저장하지 않는다.

Java는 `21.0.12.1`, Maven wrapper는 `3.9.16`, Docker server는 `29.4.0`이다.
제품은 CAP Java `5.1.1`과 고정 PostgreSQL image
`postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280`
를 사용했다. ce9의 packaged JAR SHA-256은
`64c1926a2f8db8b29e5b6aad985f7b6ff68f1b80a43fced165d755f3acda19d9`다.

`PATH="$PWD/backend/node_modules/.bin:$PATH" ./mvnw -q -f backend/pom.xml
-DskipTests package`는 exit0이다. 최초 cds executable 누락 exit1과
`npm ci --ignore-scripts --no-audit --no-fund` 후의 성공을 별도 log로
남겼다. 생성된 node_modules와 build 산출물만 자기 worktree에 둔다.

focused 검증은 아래 명령을 사용한다.

```sh
./mvnw -q -f verification/harness/pom.xml \
  -Dtest=NativePermittedContextTest,NativeProductIntegrationTest,NativeMcpTransportCaptureTest \
  test
```

명령은 exit0이며 tests17개(PermittedContext8, 기존 integration5,
transport capture4)가 모두 통과했다. `NativePermittedContextTest` 8개는
correlation 분리, payload spoof,
동명 ID 보존, 원 projection wire, capture 부재, 현재 시각의 grant 철회,
복수 grant, 시각/원 wire/cursor 변조, oracle 필드, 마지막 anchor 철회,
정상 paging을 검증한다. 기존 integration·transport capture tests도
함께 검증한다.

실제 probe는 `NativePermittedContextHttpPgTest` 하나다.
`ACTUAL_PERMITTED_CONTEXT_HTTP_PG=true`와 기존 ActualConfiguration의
disposable 환경을 명시하고, 자기 loopback 서버를
`local,verification` profile과 MCP capture enabled로 실행한다.
권한 시계는 `MULINO_VERIFICATION_INSTANT`로 고정하며 업무 asOf/knownAt은
2026-10-07로 별도 유지한다. 모델·Workflow·scenario runner는 호출하지 않는다.

probe는 exit0이며 실제 read와 PostgreSQL을 대조해 다음 12개 항목을 검증한다.

- 동명 창고 두 ID, 물량48 BOX, 현재 Work·증거·의무가 조회된다.
- 실제 서버의 actor/조직/현재 grant와 원 projection wire가 연결된다.
- 독립 PostgreSQL read로 anchor revision을 재계산해 정확히 대조한다.
- 같은 read의 revision은 같고 변경 뒤 옛 snapshotRef는
  SNAPSHOT_CHANGED로 거부된다.
- foreign organization과 직접 foreign ID를 거부한다.
- payload actor spoof를 TYPE_INVALID로 거부한다.
- ITEM∩PLACE all-of를 가진 Human에게37 BOX만 보이고 다른11 BOX와
  scope가 맞지 않는 Work는 보이지 않는다.
- hidden ID와 존재하지 않는 ID의 오류가 같다.
- AGENT와 조작된 client grant/role에도 같은 all-of 경계를 적용한다.
- 현재 delegator capability 철회를 과거 business time으로 우회하지 못한다.
- 현재 READ grant 철회를 과거 business time으로 우회하지 못한다.
- 현재 grant 만료를 과거 asOf로 우회하지 못한다.

read 전후 segment·Work·의무·증거·command record·outbox fingerprint도 같다.
초기 probe 입력의 document 필수 metadata 누락과 미지원 구매 목록 read
실패, a7에서 capture 부재로 닫힌 성공 진단은 그대로 보존했다.
ce9 이후 실제 capture 성공과 혼동하지 않는다.

최종 실제 probe의 안전한 보고서는 이 worktree 아래에 있다.

```text
verification/harness/target/native-integration/
  b74eede9-fc3c-42d3-bf2a-264148863fdd/
  direct-context-report-4f7ba8c2-e0f8-479d-a1c9-1d876322496a.json
SHA-256 5abc0d0ebc1406ec9c5def006e56c96caebb618e1b31029c38399e0289ac2002
```

보고서는 실제 인증 context 결과, 제한된 context 결과, 원 wire descriptor,
독립 anchor projection과 PG snapshot을 연결한다. resource receipt는
`.local/s5-permitted-context/proof/5ebdf19b-dbdf-4c00-89da-061c7e762434/`
아래에 있으며 backendStopped, ownContainerRemoved, ephemeralKeysRemoved가
모두 true다. `.local/s5-permitted-context/proof/ce9-focused.log`와
surefire XML은 focused 검증의 별도 증거다.

원60 model case·corpus·binding·fixture와 기존 공개 scenario 파일
2068개의 SHA-256을 시작 때 고정했다. baseline 갱신 뒤에도 drift는0이다.
각 실행은 자기 서버·컨테이너·임시 키를 정리한 receipt를 남긴다.
실모델·전체 model-binding 관찰·Scenario 재실행·S5/S6 인수는 NOT_RUN이다.
