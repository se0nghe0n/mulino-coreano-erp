# T20 discovery 계약 정정

Step2의 discovery assertion은 현재 공식 응답에 없는 protocolVersion과
전체 tools 배열을 요구했다. 그대로 구현하면 서버가 discovery 응답에
비표준 필드를 붙이고 실제 tools/list를 검증하지 않아도 통과하게 된다.
2026-10-08 공식 source를 대조한 뒤 해당 protocol oracle를 정정했다.

[공식 DiscoverResult](https://modelcontextprotocol.io/specification/2026-07-28/server/discover)는
resultType, supportedVersions, capabilities와 선택적인
_meta['io.modelcontextprotocol/serverInfo']를 사용한다. tools 배열은
[tools/list](https://modelcontextprotocol.io/specification/2026-07-28/server/tools)의
결과다. [per-request metadata](https://modelcontextprotocol.io/specification/2026-07-28/basic)의
required table에서 protocolVersion과 clientCapabilities는 required이고
clientInfo는 optional이다. 계획/skill의 clientInfo 검증 표현은 값이 있을
때 검증한다는 의미로 적용하며 누락만으로 거부하지 않는다.

공식 latest alias 두 개도 직접 HTTP 조회했다. specification/latest는
specification/2026-07-28로, latest/server/discover는 같은 날짜의
server/discover로 redirect했고 최종200이었다. 최신 alias와 pinned 문서의
version 선택이 일치했다. SDK version과 지원 후보는 research-lock.json에
있으며 SDK/client runtime 인수는 NOT_RUN이다.

정정 범위는 T20의 wire-discover, wire-initialize-not-required,
wire-server-request-not-required, wire-stdio, wire-missing-client-info 다섯
subcase다. 다른 subcase의 내용은 JSON 구조 비교로 불변을 확인했다.

- 기존 protocol-result-version의 source를 result.supportedVersions로,
  기대값을 ['2026-07-28']로 바꿨다.
- 독립 tools-list wire action을 추가하고 기존 tool-schema-registry의
  source를 그 action으로 옮겼다. 공개 capability 전체 exactSet은 유지했다.
- resultType=complete와 capabilities.tools={}를 추가로 exact 검증한다.
- clientInfo 누락 case는 HTTP200 discovery와 실제 tools/list를 요구한다.
  원 요청의 clientInfo 부재와 mandatory meta는 그대로 raw 검증한다.
- 다섯 Gherkin selector만 action/assertion 변경과 맞췄다.
  author_cases.py의 해당 생성 fragment도 같은 계약으로 수정했다.
  전체 generator는 실행하지 않아 수동 승인/MRTR oracle를 덮어쓰지 않았다.

ChannelsContractTest의 두 selftest는 올바른 supportedVersions를 받고,
legacy protocolVersion만 있는 결과와 tools-list 관찰 부재를 거부한다.
또 clientInfo 부재를400으로 잘못 거부한 결과를 실패시킨다. 이는 고정
parser sample 검증이며 실제 서버 wire 성공의 증거가 아니다.

첫 focused 실행은 stale Gherkin action/membership을 발견해22개 중1개가
실패했다. 해당 다섯 block의 selector를 맞춘 뒤 같은 명령이22/22 PASS다.
정확한 command, hash와 artifact는 evidence/compatibility-check.json에
기록했다. 이전에 생성된 NOT_RUN evidence는 과거 baseline의 기록이며
성공으로 고치지 않았다. 전체 T20 runtime은 S5까지 NOT_RUN이다.
