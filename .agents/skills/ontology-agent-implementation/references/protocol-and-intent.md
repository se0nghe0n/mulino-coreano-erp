# Protocol과 intent

검색어: `params._meta`, `server/discover`, `input_required`,
`conversationRequestId`, `commandIdempotencyKey`, `V6`.

## 규격과 실제 지원

기준 protocol은 MCP `2026-07-28`이다. 공식
[규격](https://modelcontextprotocol.io/specification/2026-07-28),
[transport](https://modelcontextprotocol.io/specification/2026-07-28/basic/transports),
[HTTP binding](https://modelcontextprotocol.io/specification/2026-07-28/basic/transports/streamable-http),
[MRTR](https://modelcontextprotocol.io/specification/2026-07-28/basic/patterns/mrtr)를
구현 시 확인하고 조회일·SDK/client version과 지원 범위를 기록한다.
SDK의 stateless 옵션 하나는 protocol 준수 증거가 아니다.

요청의 `params._meta`에서 `io.modelcontextprotocol/protocolVersion`,
`io.modelcontextprotocol/clientInfo`,
`io.modelcontextprotocol/clientCapabilities`를 검증한다.
`server/discover`와 지원하는 tools/resources/prompts의 실제 schema를
제공한다. 현대 protocol에 initialize handshake, connection session,
서버발 JSON-RPC request를 끼워 넣지 않는다. 구형 client 지원은
독립 adapter와 compatibility test가 있는 경우에만 표시한다.

원격 기본값은 인증된 Streamable HTTP다. 각 메시지는 독립 POST이며
JSON 또는 요청 범위 SSE response를 처리한다. Origin 검증과 endpoint
인증을 구현한다. `MCP-Protocol-Version`, `Mcp-Method`, 필요한 `Mcp-Name`
및 채택한 `x-mcp-header`의 header/body 일치를 검사한다. 잘못된 표기는
공식 header encoding/오류 계약대로 거부한다. stdio는 필요한 로컬
인수에만 사용하며 같은 schema·protocol·제한된 자격을 적용한다.

공식 오류의 실제 code를 확인해 `contracts/mcp-errors.md`에 domain
outcome 매핑을 고정한다. malformed wire와 업무 `NEEDS_INPUT`,
`WAITING_APPROVAL`, `CONFLICT`, `FORBIDDEN`, `ACCEPTED_PENDING_EXTERNAL`을
합치지 않는다. Tool 실행 오류를 성공 text 하나로 감추지 않는다.

## 입력 초안 → canonical proposal → 효과

`intentKind=QUERY|RECORD|COMMAND`, definitionVersion/capabilityId,
typed subjectRefs/slots, 조건, evidenceRefs, 원문/문맥 참조와
provenance `USER|CONTEXT|APPROVED_DEFAULT`를 검증한다. decimal은 단위
있는 문자열이다. 필수 단계·끝점·quantityMode를 명시한다. 날짜를
장소로 받거나 같은 이름의 대상을 임의 선택하지 않는다.

| 식별자 | 의미와 수명 |
|---|---|
| JSON-RPC ID | 한 wire 요청. MRTR retry마다 새 값 |
| conversationRequestId | 입력 수집 연속성. 보완 가능하며 효과 key 아님 |
| requestState | MRTR 연속성. 불투명·무결성 검증; 권한/승인 아님 |
| canonical hash/proposalRevision | 효과 0인 검증 뒤 확정. 승인은 이 내용에 연결 |
| commandIdempotencyKey | 확정 명령 효과. 같은 key/다른 hash는 conflict |
| externalOperationId | outbox의 동일 외부 작업. RPC/token 교체로 바뀌지 않음 |

MRTR는 지원 method의 `resultType=input_required` 결과에
`inputRequests` 또는 `requestState`를 넣고, 같은 요청의 retry에서
대응 `inputResponses`와 정확한 state를 받는다. Client가 선언하지 않은
elicitation을 요구하지 않는다. 무결성·주체·method/의도·TTL을 검사하고
single-use 승인 소비는 DB에서 원자적으로 강제한다. accept 문자열은
서버의 행동 승인 결정을 대신하지 않는다. 미지원 client에는 명시적
추가 입력 경로 또는 미지원 결과를 제공한다.

## 최소 walk-through oracle

다음은 구현할 때 실행할 사례이며 문서 자체는 runtime PASS가 아니다.

**Destination 보완:** 인증된 구매 요청100 BOX의 품목·기한·끝점은
확정됐지만 destination이 없다. 효과 0으로 NEEDS_INPUT과 MRTR를
반환한다. 사용자 W 보완은 같은 conversationRequestId, 새 RPC ID,
검증 가능한 state로 이어진다. W 타입/조직/권한 검증 뒤 새로운
canonical hash/revision을 확정한다. 아직 효과 key에 내용이 고정된
단계가 아니므로 보완을 IDEMPOTENCY_CONFLICT로 처리하지 않는다.
구매 정책상 필요한 MANAGER 승인 뒤 별도 effect key로 실행한다.

**수령 응답 유실:** 수령60 commit 뒤 response를 버린다. token 갱신,
새 RPC ID와 동시 retry도 같은 stableRequestOwner/capability/effect key를
사용한다. 결과 조회 인가 뒤 원 효과를 반환하며 수령·원장·의무·outbox
효과는 각각 한 번이다. 같은 key로40은 conflict이며 다른 주체에게
원 결과를 누설하지 않는다. 별개 구매는 별개 key다.

**Boundary 변조:** state의 주체/method/TTL/무결성을 하나씩 바꾸고,
header와 body의 method/name/version 불일치도 넣는다. 추가 입력이나
tool 실행 전 거부하고 업무 효과0을 관찰한다. 실지원 없는 client에서
elicitation 성공을 합성하지 않는다.
