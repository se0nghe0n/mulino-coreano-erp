# S0 MCP wire 계약

업무 실행과 protocol의 성공을 구별하기 위해 새 adapter의 최소 wire를
고정한다. 이 문서는 S0 read/reserve spike의 계약이다. 전체 T20, client
로딩, MRTR와 실모델 인수는 S5에서 별도로 통과해야 한다.

## 버전과 구현

2026-10-08 공식 규격을 조회했다. 기준은 MCP `2026-07-28`, JSON-RPC
`2.0`, Streamable HTTP다. endpoint는 인증된 `POST /mcp`이며 응답은
`application/json`이다. 서버 SDK는 사용하지 않는 custom adapter다.
독립 검증 client는 Python standard library의 `wire_probe.py` 1.0.0이다.
이 선택을 SDK interoperability 성공으로 표시하지 않는다.

공식 release API에서 최신 stable Java SDK `2.0.1`, Python SDK `2.3.0`,
TypeScript SDK `2.3.1`, Inspector `2.9.0`을 확인했다. 설치/실행 인수는
아직 하지 않았다. Python release note는 modern 요청과 legacy 요청을
구별한다. S5 후보 client는 Python SDK 2.3.0과 Inspector 2.9.0이다.
Java SDK2.0.1의 공식 ProtocolVersions.java는 2025-03-26, 2025-06-18,
2025-11-25만 선언한다. McpSchema에 initialize가 있고 discovery 타입은
없다. 따라서 이 Java SDK를 modern adapter로 채택하지 않았다.
[고정 Java protocol constants](https://github.com/modelcontextprotocol/java-sdk/blob/v2.0.1/mcp-core/src/main/java/io/modelcontextprotocol/spec/ProtocolVersions.java)

- [규격](https://modelcontextprotocol.io/specification/2026-07-28)
- [HTTP binding](https://modelcontextprotocol.io/specification/2026-07-28/basic/transports/streamable-http)
- [versioning](https://modelcontextprotocol.io/specification/2026-07-28/basic/versioning)
- [discovery](https://modelcontextprotocol.io/specification/2026-07-28/server/discover)
- [tools](https://modelcontextprotocol.io/specification/2026-07-28/server/tools)
- [Java SDK release](https://github.com/modelcontextprotocol/java-sdk/releases/tag/v2.0.1)
- [Python SDK release](https://github.com/modelcontextprotocol/python-sdk/releases/tag/v2.3.0)
- [TypeScript SDK release](https://github.com/modelcontextprotocol/typescript-sdk/releases/tag/v2.3.1)
- [Inspector release](https://github.com/modelcontextprotocol/inspector/releases/tag/2.9.0)

## 독립 요청

각 요청은 `params._meta`에 version과 clientCapabilities를 포함한다.
clientInfo는 공식 규격상 optional이며 probe는 명시적으로 보낸다.
`MCP-Protocol-Version`, `Mcp-Method`, tools/call의 `Mcp-Name`은 본문과
일치해야 한다. `Accept`는 `application/json, text/event-stream`을,
`Content-Type`은 `application/json`을 사용한다. client는 실제 선택한
JSON 응답을 관찰한다. 이 probe의 성공이 SSE client 인수는 아니다.

서버는 `server/discover`에 supportedVersions, capabilities와
resultType를 반환한다. serverInfo는 result._meta에 둔다. tools 목록과
schema는 별도 `tools/list`에서 받는다. 모든 성공 result에는
`resultType=complete`가 있다. `tools/call`은 content와 structuredContent,
isError를 반환한다. 요청 ID는 string 또는 integer이며 응답이 같은 ID를
되돌려준다. null ID, batch와 malformed 입력은 거부한다.

initialize handshake, protocol session, standalone GET stream,
DELETE termination을 제공하지 않는다. GET/DELETE는405다.
Mcp-Session-Id와 Last-Event-ID는 무시하고 session ID를 발급/반환하지
않는다. 서버발 JSON-RPC request를 사용하지 않는다.

## 최소 tools

| tool | 입력 | application 경로 |
|---|---|---|
| platform.readScope | scopeId UUID | 현재 신원·조직·READ 검사 후 scope snapshot 조회 |
| platform.reserve | scopeId UUID, quantity decimal string, expectedRevision integer, idempotencyKey string | 동일 service에서 현재 RESERVE·revision·물량·멱등 검사 후 DB transaction |

이 두 tool은 S0의 기술 fixture이며 공개 도메인 capability의 완료 목록이
아니다. actor/role payload를 신원으로 사용하지 않는다. JWT issuer,
audience, 서명, 유효기간, organizationId와 stableRequestOwner를 서버가
검증한다. API와 MCP는 같은 application service·멱등 namespace를 쓴다.

새 RPC ID로 같은 effect key를 재시도하면 현재 조회 인가 뒤 원 결과를
반환한다. 같은 key로 다른 quantity를 보내면 structured domain conflict를
반환한다. RPC ID를 효과 key로 저장하지 않는다. conversationRequestId,
requestState, approval hash/revision과 externalOperationId는 이 S0 slice의
입력이 아니며 S5에서도 별개 의미로 유지한다.

## 오류와 경계

| 원인 | HTTP | JSON-RPC 또는 tool 결과 |
|---|---|---|
| 인증 누락/유효하지 않은 token | 401 | 인증 단계, business tool 실행0 |
| 허용되지 않은 Origin | 403 | transport 거부, business tool 실행0 |
| `Accept` 누락 또는 `application/json, text/event-stream` 아님 | 406 | transport 거부, JSON-RPC 처리·business tool 실행0 |
| 필수 mirrored header 누락/불일치 | 400 | error.code=-32020 HeaderMismatch |
| 지원하지 않는 version | 400 | error.code=-32022, data.supported/requested |
| 지원하지 않는 RPC/modern initialize | 404 | error.code=-32601 |
| malformed JSON/invalid envelope | 400 | parse/invalid-request JSON-RPC 오류 |
| 필수 `_meta` 누락, `_meta`의 clientInfo·clientCapabilities 내용 오류 | 400 | error.code=-32602 Invalid params |
| 잘못된 tool arguments/미지원 tool | 별도 protocol 오류 | 유효한 request ID와 error 보존 |
| 업무 권한/revision/멱등 충돌 | 200 | resultType=complete, isError=true, structuredContent에 domain outcome |

한 요청이 본문 envelope 오류와 mirrored header 오류를 함께 가지면
envelope 오류 하나로 답한다. JSON parse 실패는 -32700, JSON-RPC batch
배열·object가 아닌 본문·`jsonrpc`/`id`/`method`/`params` 형식 위반은
400 -32600이다. 여기서 `params` 형식 위반은 `params`가 object나 array가
아닌 경우뿐이다. `params`가 object이면 envelope은 유효하다. 그 안의
필수 `_meta` 누락, `_meta`의 clientInfo·clientCapabilities 값 형식 오류는
params 내용 오류이며 400 -32602다(T20 `wire-missing-meta`,
`wire-invalid-client-info`, `wire-missing-capabilities`). mirrored
header(`MCP-Protocol-Version`, `Mcp-Method`, tools/call의 `Mcp-Name`)의
누락/불일치(400 -32020)는 envelope이 유효할 때만 판정한다. `_meta`가
없는 요청에는 version header와 비교할 본문 값이 없으므로 -32020이 아니라
-32602다. 인증·Origin·Accept·Content-Type 같은 transport 거부와 다른
행 사이의 상대 순서는 이 절이 정하지 않는다. 그래서 transport 거부를
보지 않는 case 요청은 필수 `Accept`를 보내고 `Origin`을 보내지 않는다.

envelope 검사가 header 검사보다 먼저인 이유는 header가 비교할 단일
method/name이 envelope이 유효할 때만 있기 때문이다. batch 배열에는
요소마다 다른 method/name이 있어 `Mcp-Method`·`Mcp-Name`과 대조할 값이
없다. 그래서 `Mcp-Name` 없이 tools/call 두 개를 담은 batch(V4
`mixed-atomic-batch`의 `mcp-batch`)는 -32020이 아니라 -32600이다. 단일
object 본문의 method가 `Mcp-Method`와 다른 요청(T20
`wire-method-mismatch`)은 envelope이 유효하므로 -32020이다. 현재
backend의 `PlatformMcp`(S0)와 `OntologyMcp`도 envelope(-32600)을 header
(-32020)보다 먼저 검사한다(2026-10-08 소스 확인, 실행 인수 아님).

Mcp-Name의 `=?base64?<UTF8base64>?=` 표기는 decode 후 본문과 비교한다.
S0 schema에는 x-mcp-header가 없으므로 추가 매핑을 광고하지 않는다.
Origin이 없으면 CLI 요청을 허용하며 있으면 허용 origin만 받는다.
이 계약과 case fixture는 허용 origin을 선언하지 않는다. 따라서 T20·V4의
Streamable HTTP 요청은 Origin을 보내지 않고, Origin을 보내는 요청은
허용 목록 밖 origin의 403 반례(T20 `wire-bad-origin`)뿐이다. `Accept`가
없는 요청은 406 반례(T20 `wire-missing-accept`)뿐이다. `./verify prepare`
(`ContractValidator.wireTransportProblems`)는 그 밖의 요청이 이 두 header를
어기면 준비 실패로 낸다.
local server는 loopback에 bind한다. 요청 artifact는 Authorization을
`[REDACTED_SECRET]`으로 치환하며 response credential echo를 발견하면
저장을 중단한다. 이 fixture 인증은 운영 OAuth/IAS discovery 인수가 아니다.

## MRTR requestState(S5 계약)

S0 adapter는 MRTR를 구현하지 않는다. 이 절은 S5 adapter가 지킬 값을
고정한다. 계획 §9.1은 `requestState`를 주체·method/의도·TTL에 묶고
무결성을 검증하라고 하지만 TTL 수치는 정하지 않았다. 테스트 계약은
아래 값을 쓴다.

- `requestState` TTL은 600초다(`requestStateTtlSeconds=600`). 기준은
  state를 발급한 시각이다. 발급 뒤 599초의 continuation은 정상 처리하고
  (T20 `before-expiry`는 STRUCTURED), 601초의 continuation은
  `REQUEST_STATE_EXPIRED`로 거부한다. T20
  fixture `baseline.mrtr.requestStateTtlSeconds`와 두 경계 시각
  (09:09:59Z·09:10:01Z)은 `verification/mcp-tests/author_cases.py`의
  `MRTR_TTL_SECONDS` 하나에서 계산한다. 생성기는 이 문서의 값과 다르면
  실행을 멈춘다.
- 이 값은 계획 §10의 versioned config와 같은 성격의 개발/CI 값이다.
  운영 SLA가 아니며 deployment profile에서 다시 정할 수 있다. 그때는
  이 문서, 생성기 상수, T20 fixture를 함께 바꾼다.
- 만료 외의 결속 실패 code도 T20이 고정한다. 변조는
  `REQUEST_STATE_INTEGRITY_FAILED`, 다른 주체는
  `REQUEST_STATE_PRINCIPAL_MISMATCH`, 다른 intent는
  `REQUEST_STATE_INTENT_MISMATCH`, 맞지 않는 응답은
  `INPUT_RESPONSE_UNMATCHED`, 이미 소비한 single-use state는
  `REQUEST_STATE_CONSUMED`다. 이 code들은 아직
  `contracts/domain-vocabulary.json`에 없다. Step 3 추가 요청이며
  `verification/cases/check_vocabulary.py`의 PENDING 항목이다.
- TTL 안의 state도 업무 승인 자체가 아니다. 승인 소비의 single-use는
  DB에서 원자적으로 강제한다(계획 §9.1).

## 실제 인수

`verification/platform/protocol/wire_probe.py`는 실제 endpoint를 호출한다.
mock 응답을 만들지 않으며 실패 assertion이면 exit1이다. JSON transcript와
SHA256를 함께 기록한다. token은 MCP_TOKEN 환경으로만 전달한다.

```sh
python3 verification/platform/protocol/wire_probe.py \
  --url http://127.0.0.1:8080/mcp --scope-id "$S0_SCOPE_ID" \
  --revision 0 --output verification/platform/protocol/evidence/wire.json
```

실제 local platform에 두 번 수행했다. 최종29개 HTTP 요청과118개
assertion이 PASS이며 원 wire와 SHA256는 wire-run-2.json에 있다.
첫 discovery의 namespaced meta 추출 오류는 실제 FAIL로 보존한 뒤
platform 수정 후 같은 공식 요청으로 검증했다. 전체 T20/S5는 NOT_RUN이다. S0 wire 성공은 DB effects/rollback,
CQN read/action parity, V2/V3 fence 인수를 대신하지 않는다. 해당 증거는
platform의 실제 integration test와 결합해야 한다.

S5에는 전체 action/query, resources/prompts의 광고 범위, MRTR 정상/변조/
TTL/주체/method/capability fallback, action schema의 parity, client 발견·
skill 로딩·실제 tool 실행, 모델 의미 corpus, OAuth/운영 TLS 인수가 남는다.
