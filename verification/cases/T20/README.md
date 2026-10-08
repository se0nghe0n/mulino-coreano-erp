# T20 intent·wire·host 인수

구조적으로 유효한 intent, 실제 사실, 현재 권한은 각각 검증한다.
QUERY/RECORD/COMMAND와 USER/CONTEXT/APPROVED_DEFAULT를 고정하며
구매 초안의 LOT 부재는 수령 단계의 필수 LOT와 구별한다.
날짜가 장소 slot에 오면 TYPE_INVALID이고 모호한 날짜·동명 대상은
NEEDS_INPUT이다. 입력 보완은 같은 conversation에서 새 hash/revision을
확정하며 최종 effect key와 분리한다.100→120의 구매 변경은 이전
승인을 사용할 수 없다.

MCP는 request마다 version/clientInfo/capabilities meta와 raw HTTP
header/body/JSON-RPC ID를 그대로 검증한다. 실제 transcript의 원 요청과
거부 결과, 독립 DB 전후 원행을 연결한다. API/MCP의 동일 effect key는
하나의 command namespace이며 다른 payload는 conflict다. stdio에서도
READ grant의 쓰기 효과는0이다. 구형 client 성공을 최신 인수로 세지 않는다.

MRTR의 정상 보완, state byte 변조, 만료, 다른 주체/method/intent,
대응하지 않는 inputResponses, 승인으로 오해한 state/accept 문자열,
지원 capability 없는 client fallback을 각각 실행한다.
별도 승인 소비 경합은 실제 manager가 발급받은 하나의 state를 두
wire 요청이 다른 RPC/effect key로 소비한다. 두 start/barrier/terminal과
DB approval1/state consumption1을 각각 관찰한다. 일반 입력수집 state에
임의 single-use 규칙을 추가하지 않는다.

여섯 skill은 실제 host의 discovery·본문·필요 reference·tool·의무 확인을
분리한다. 실제 모델에 oracle·typed 답안·예상 tool을 넣지 않는다.
host/model/usage가 없으면 해당 실행은 NOT_RUN이다. 고정 selftest는
parser와 AssertionEngine의 거부 성질만 검증하며 실제 결과를 만들지 않는다.

## 재검토 수정(2026-10-08)

case.json·fixture·Gherkin은 `verification/mcp-tests/author_cases.py`의
출력이다. 손으로 고치지 않고 generator를 고친 뒤 다시 만든다.

- wire 오류는 공식 2026-07-28 코드로 검증한다. header 누락/불일치는
  400·-32020, 필수 `_meta` 누락은 400·-32602, 미지원 version은
  400·-32022와 `data.supported=["2026-07-28"]`·`data.requested`다.
  프로젝트가 만든 `error.data.category`는 oracle에서 뺐다. `_meta`
  전체 누락은 두 규칙이 겹쳐 -32602와 -32020이 모두 규격에 맞으므로
  400 오류 envelope만 고정한다. 401/403은 JSON-RPC 처리 전 거부라
  HTTP status와 업무 효과0만 본다. 값이 있으나 형식이 틀린
  clientInfo는 -32602로 거부한다(`wire-invalid-client-info`).
- tools/list는 별도 request로 보내고 HTTP200(stdio는 transport),
  jsonrpc·id echo, resultType=complete, raw method·header·meta를
  모두 검증한다.
- MRTR state TTL은 fixture `baseline.mrtr.requestStateTtlSeconds=600`이다.
  09:09:59Z continuation은 STRUCTURED, 09:10:01Z는
  REQUEST_STATE_EXPIRED다. 변조·주체·intent·응답 불일치는 각각
  REQUEST_STATE_INTEGRITY_FAILED·REQUEST_STATE_PRINCIPAL_MISMATCH·
  REQUEST_STATE_INTENT_MISMATCH·INPUT_RESPONSE_UNMATCHED를 고정한다.
  다른 intent는 slot이 같은 createWork로 바꿔 타입 오류가 결속 검사를
  대신하지 못하게 했다. 다른 method는 header와 body가 일치하는
  resources/read로 보내며 -32602와 결과 없음을 요구한다.
- 여섯 skill은 계획 §9.2 절차마다 다른 문장·필수 QUERY 호출·금지
  command·해당 업무 의무를 가진다. 필수 호출은 한 번 이상(subset)으로
  검사하고 호출 순서나 횟수를 고정 답안으로 삼지 않는다. reference와
  tool 단계는 여러 번 관찰돼도 된다.
- host 변형은 COMMAND와 RECORD 호출을 각각0으로 센다. 근거 연결·
  의무·관계 원행도 전후 비교하고 client 주체를 readAgent로 고정한다.
  `host-allowed-tools-write`는 fixture의 `skillVariants`로 frontmatter
  allowed-tools에 reserveQuantity를 설치하고 쓰기를 요청한다. 모델이
  쓰기를 시도하는지는 요구하지 않는다. grant를 확인하고 거절하는 client도
  올바르다. host가 제출한 COMMAND·RECORD는 적용·외부전달·승인대기가
  0건이어야 한다. 서버 경계는 같은 readAgent 주체의 scripted
  `tools/call reserveQuantity`(action `scripted-write`)로 결정적으로 본다.
  이 호출은 HTTP 200 tool result, outcome REJECTED, code FORBIDDEN,
  isError=true이고 전후 원행은 같다. client allowed-tools는 서버 인가가
  아니다.

wire 오류는 2026-07-28 공식 규격을 따른다. `_meta` 전체가 없으면
필수 field 누락이므로 HTTP 400·-32602다(basic/index "Per-request protocol
fields"). header와 비교할 body 값이 없어 -32020으로 보지 않는다.
clientInfo 형식 오류도 HTTP 400·-32602다. MRTR requestState TTL은
fixture `baseline.mrtr.requestStateTtlSeconds`=600이고 두 경계 시각
09:09:59Z·09:10:01Z는 생성기가 이 값에서 계산한다.

계획 §9.1 문장과 catalog clause의 clientInfo 표현은 이 worktree에서
고치지 않았다. 공식 규격상 clientInfo는 optional이며 이 case는
"값이 있으면 검증한다"로 해석한다.
