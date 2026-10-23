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

## 최초 구현의 변경 경계

최초 baseline의 새 helper는 `NativePermittedContext`와 `NativePermittedContextCapture`다.
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

## 최초 baseline에서 관찰한 discovery 공백

최초 구현 baseline의 `PurchaseQueries`와 `SettlementQueries`는 ID가 필요했다.
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
서버가 현재 관찰한 capability·유효기간·assignment와 실제 read 대상의
all-of scope를 대조해 후보가 정확히 하나일 때만 그 ID를 기록한다.
여러 후보 중 사용 grant를 추측하지 않는다.
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

## fea71358 이후 grant 적용 범위와 fixture clock 보완

후속 baseline은 `fea71358747e08962f450470159790b6982eb67f`다.
다른 worker의 미통합 변경과 root dirty tree는 가져오지 않았다.
기존 query/send/helper 소유권 안에서 `NativePermittedContextGrants`를
추가했다. 제품의 현재 read handler가 인가하는 대상 차원을 실제 서버
응답에서 재구성하는 관찰 recipe다. 제품의 인가를 대신하지 않는다.

일반 Place·item·segment·Work read에는 SOURCE 차원이 없다.
SOURCE와 TARGET을 함께 요구하는 grant는 TARGET이 맞더라도 제외한다.
evidence read의 SOURCE는 현재 `EvidenceTypes.scopes`와 같이 실제 응답
행의 sourceProfileId에서만 읽는다. client가 임의로 붙인 SOURCE/role,
fixture의 원 grant alias나 purpose grant 이름으로 후보를 고르지 않는다.
ID가 다른 여러 반환 대상에서 서로 다른 grant가 적용되거나 같은
대상에 여러 grant가 적용되면 전체 attribution을 차단한다.
현재 assignment의 capability·scope·만료·철회도 대조한다.

빈 목록과 FORBIDDEN read에는 사용된 업무 대상 grant를 주장하지 않는다.
그 read의 원 인증 capture를 보존하고 read plan에
`NO_BUSINESS_TARGET_ASSERTED`를 기록한다. anchor와 사실을 반환하는
read는 실제 대상이 관찰돼야 한다. 알 수 없는 대상 recipe를 빈 목록으로
해석하지 않는다. `actionAuthorization=NOT_ASSERTED`는 유지한다.

`NativePermittedContextHttpPgTest`는 자기 synthetic 설치 뒤 인증된
`/__verification/runtime/clock/fixture-start`를 호출하고 실제 HTTP200,
authorityClock=true, instant 일치를 확인한 receipt를 저장한다.
권한 유효기간도 선언한 `MULINO_VERIFICATION_INSTANT`를 기준으로
설치한다. business asOf/knownAt은 바꾸지 않는다. 앞선 suite class가
시계를 전진시켰어도 자기 probe가 시작 시각을 확립한다.

별도 read grant와 SOURCE+TARGET grant를 함께 설치한 실제 probe에서
read grant를 선택하고 SOURCE grant의 행·actions·scope·시간 불변을
검증한다. 적용 가능한 두 read grant의 실제 HTTP 반례도 계속
`PERMITTED_CONTEXT_GRANT_ATTRIBUTION_UNAVAILABLE`이다. 모델 호출은0이다.

coordinator에게 제안한 backend 소유 범위는 다음 네 경로다.

- `backend/src/main/java/com/mulino/application/trade/purchase/PurchaseQueries.java`:
  PurchaseProposal/PurchaseOrder의 기존 `searchObjects`를 완전한
  현재 scope 인가·knownAt·안정된 ID/cursor 목록으로 제공한다.
- `backend/src/main/java/com/mulino/application/trade/settlement/SettlementQueries.java`:
  Invoice 목록을 같은 방식으로 제공한다. 기존 joined line 인가의
  PLACE/CUSTOMER/SUPPLIER 차원을 추측하지 않도록 실제 line의 read scope를
  서버가 계산한 응답에 연결한다. 공개 입력 schema와 쓰기 경로는 유지한다.
- `backend/src/main/java/com/mulino/application/trade/regulatory/RegulatoryQueries.java`:
  기존 목록에 ID 순서와 query-bound cursor를 제공한다.
- `backend/src/test/java/com/mulino/core/BusinessDiscoveryPostgresTest.java`:
  네 noun의 paging·직접 조회 일치·권한·foreign/hidden·knownAt 반례를
  검증하는 새 focused test다.

이 문서 갱신 시점에는 위 backend 소유권을 요청했으며 수정하지 않았다.
Invoice의 joined line 범위가 응답에서 관찰되지 않는 동안 그 grant
recipe는 fail-closed다. root의 RuntimeService 파일도 수정하지 않는다.

추가로 transport 소유자에게 다음 기존 계약 차이를 알렸다.
`ModelFixtureInstaller.grantChildren`은 SOURCE scopeId에36자 이하의
namespace를 저장하지만 현재 `NativeMcpTransport.validateServerCapture`는
SOURCE까지 UUID로 검사한다. SOURCE만 제품의 실제 bounded string
계약으로 검사하고 나머지 ID 차원의 UUID 검사를 유지하는 수정이
필요하다. 이 worker는 소유권 밖인 NativeMcpTransport를 수정하지 않았다.
실제 probe는 이 차이와 별도로 UUID 형태의 SOURCE 값을 사용하며,
namespace 문자열 반례는 helper unit에서 검증한다.

후속 focused 명령은 기존 명령과 같고 tests20개(PermittedContext11,
integration5, transport capture4)가 exit0으로 통과했다.
실제 HTTP/PG probe1개도 exit0이며15개 관찰 항목을 통과했다.
앱은 command-line 초기 시각을 `2026-10-08T09:00:00Z`로 띄웠고,
probe의 선언 환경은 `2026-10-07T09:00:00Z`였다. 저장한 fixture-start
receipt의 실제 응답 instant는 후자와 같다. 기존 expiry/revocation
assertion은 유지했다.

후속 실제 보고서는 다음 경로에 보존한다.

```text
verification/harness/target/native-integration/
  81eca9c5-3160-461a-8e38-888942621d8f/
  direct-context-report-55d871f8-5eb5-411b-8ad9-7266a3673714.json
SHA-256 8448cc4ce5b12a2eb60fce62b920cf9254d89065f8b0954efadd244ac36b64f4
```

같은 디렉터리의 clock receipt
`fixture-start-clock-a1ab2eb7-f472-4a4a-9338-f78a3b20cab9.json`의 SHA-256은
`e4904688f844b81a60484dde0344ab65944c259b1699934ebf110ba499cf09e0`이다.
resource receipt는
`.local/s5-permitted-context/proof/33e9a21d-5bfb-4e17-b31f-a073a981f193/`
아래에 있으며 자기 서버·컨테이너·임시 키 정리는 모두 true다.
별도 후속 증거 목록은 `.local/s5-permitted-context/followup/manifest.json`에
둔다. 기존2068개 입력의 drift는0이고 original corpus·binding·fixture와
공개 scenario oracle bytes는 보존했다.


## 85f5d151 이후 승인된 discovery·SOURCE 구현

coordinator가 세 provider와 새 `BusinessDiscoveryPostgresTest`,
`NativeMcpTransport.validateServerCapture`의 SOURCE 검사만 추가 승인했다.
baseline은 `85f5d1510a6b818175753132aa02ef62e10ea1f0`이며 다른 source는
통합하지 않았다. ApplicationQueries·QueryRequests·public schema·쓰기
handler·RuntimeService는 수정하지 않았다. 위 최초 baseline의 네 noun
공백과 SOURCE UUID 제한은 이 후속 구현으로 대체한다.

PurchaseProposal, PurchaseOrder, Invoice, RegulatoryProcedure를 기존
`searchObjects`의 typed noun selector로 조회한다. 현재 인증 주체의
실제 searchObjects 권한으로 각 후보를 인가하며 ID 순서로 정렬한다.
인가된 limit+1번째 행이 있을 때만 마지막 반환 ID의 ReadCursor를
발행한다. hidden 후보의 ID·count·cursor는 응답에 넣지 않는다.
cursor는 조직·actor·업무 시각·지식 시각·operation·scope·filter에
묶이며 다음 page에서도 현재 철회·만료를 다시 검사한다.

Invoice는 실제 joined line·order·revision이 asOf/knownAt에 보일 때만
반환한다. 같은 조직의 invoice 저장 item/work와 joined line의 item/work도
일치해야 한다. 서버가 기존 SettlementCommands.scopes로 인가한
ITEM·WORK·PLACE·CUSTOMER·SUPPLIER를 `readScope`로 관찰한다.
correction도 같은 검사와 scope 관찰을 거친다. hidden correction이
있는 Invoice를 부분 projection으로 노출하지 않고 FORBIDDEN으로 닫는다.
helper는 이 실제 scope들만 사용하며 서로 다른 적용 grant가 필요하면
계속 fail-closed다. fixture의 목적·예상 grant ID는 참조하지 않는다.

SOURCE는 현재 IdentityCommands.text의 nonblank 문자열 조건과
V3__identity의 scopeKind enum·scopeId VARCHAR(36)에 근거한다.
명시적인 SOURCE scopeId에만36자 이하 namespace를 허용한다.
다른 차원의 UUID 검사는 유지하며 누락·빈 값·숫자·object·미정의 차원은
거부한다. 유효한 다른 namespace도 실제 evidence row의 SOURCE와
일치하지 않으면 grant 후보가 될 수 없다. 이 검사는 서버 endpoint에서
읽은 관찰의 typed shape 검사이며 임의 namespace를 action 승인으로
바꾸지 않는다. 원 wire·서버 관찰과 actionAuthorization=NOT_ASSERTED를
보존한다.

직접 PostgreSQL tests8개는 네 noun의 stable cursor·all-of·현재 철회,
knownAt·foreign/hidden·verb projection 동등성 및 Invoice의 joined
revision/line/order와 hidden correction을 검증했다. 기존 구매 정책
noun/verb2개와 Invoice noun/verb1개도 통과했다. native focused22개도
exit0이다. 이 단계의 로그·XML은
`.local/s5-permitted-context/discovery/`에 별도로 보존한다.
fixture 설정 중 발견한 필수 열·approval 소비 정책 누락과 SQL TIMESTAMP
UTC 저장 보정, 지원 read의 별도 grant 보정 이전 실패 로그도 남긴다.
후속 actual HTTP/PG 증거는 별도로 추가한다. 이전18개·21개 검증의
manifest와 원 artifact는 유지한다. 전체 결합 epoch와 S5/S6 인수는
coordinator의 별도 범위다.
