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
| 필수 mirrored header 누락/불일치 | 400 | error.code=-32020 HeaderMismatch |
| 지원하지 않는 version | 400 | error.code=-32022, data.supported/requested |
| 지원하지 않는 RPC/modern initialize | 404 | error.code=-32601 |
| malformed JSON/invalid envelope | 400 | parse/invalid-request JSON-RPC 오류 |
| 잘못된 tool arguments/미지원 tool | 별도 protocol 오류 | 유효한 request ID와 error 보존 |
| 업무 권한/revision/멱등 충돌 | 200 | resultType=complete, isError=true, structuredContent에 domain outcome |

Mcp-Name의 `=?base64?<UTF8base64>?=` 표기는 decode 후 본문과 비교한다.
S0 schema에는 x-mcp-header가 없으므로 추가 매핑을 광고하지 않는다.
Origin이 없으면 CLI 요청을 허용하며 있으면 허용 origin만 받는다.
local server는 loopback에 bind한다. 요청 artifact는 Authorization을
`[REDACTED_SECRET]`으로 치환하며 response credential echo를 발견하면
저장을 중단한다. 이 fixture 인증은 운영 OAuth/IAS discovery 인수가 아니다.

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
