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
