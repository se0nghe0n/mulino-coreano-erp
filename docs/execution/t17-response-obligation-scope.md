# T17 후속 책임과 ITEM 전체 assignment의 범위

`fact-not-permission-11`은 인도와 조사에서 생긴 열린 후속 책임 두 종류를
확인한다. 기존 ITEM 전체 조회에는 준비 과정의 REGULATORY_REVIEW와
QUALITY_REVIEW, 해소된 DELIVERY_RECONCILIATION도 함께 존재한다.
`current=true`는 유효한 assignment이며 OPEN status와 독립이다.
이 의미와 C4/E1의 RESOLVED/current=true 계약을 유지한다.

## 변경과 보존

기준선은 `fb58d86663f28ec90dd38322399c384a42de4672`다.
Coordinator가 별도 branch `fix/t17-response-obligation-scope-20261024`와
`coordinator-t17-response-scope-fb58` worktree에서 작성했다.
수정 runtime은 `codex_2/gpt-6.1-sol/xhigh/priority`다.

새 독립 SQL observation은 실제 판매 인도 Work와 명시 recall Work에
속한 obligation 행을 읽는다. 같은 snapshot에서 OPEN/current=true를
선택한 원 assertion의 기대 두 종류, op, ID, oracleRef는 보존한다.
제품, observer, 현재 인가, fixture와 전체 ITEM 조회는 바꾸지 않는다.

새 assertion 8개는 제한 범위의 전체 assignment 세 행과 공개 조회의
ID/root/Work/owner/status/nextAction/nextCheck 연결을 확인한다.
ITEM 전체의 여섯 행, REGULATORY_REVIEW RESOLVED 두 행,
DELIVERY_RECONCILIATION RESOLVED 한 행과 QUALITY_REVIEW OPEN 한 행도
각각 확인한다. 열린 책임을 검사하기 위해 과거 유효 행을 숨기지 않는다.

`author_response_scope.py`와 `response-scope-delta.json`에 한 원래
selector 변경, 새 observation과 assertion의 정확한 before/after를
기록한다. inverse는 변경된 범위·fixed 값·누락된 증거를 거부한다.
원 301개 assertion의 순서와 fixed expected/op/oracle을 보존한다.
추가 8개를 포함한 총수는 309개다. fixture 15개는 byte 동일하다.

## 검증과 통합

Python 45개가 exit0으로 통과했다. 원본 assertion 301개 전체의 inverse,
15개 fixture bytes, Work 범위, 전체·해소·품질 행의 보존과 변조 거부를
검사했다. 기존 public/recall generator의 전체 재생성도 byte drift 없이
통과했다. Gherkin은 새 action 순서와 모든 assertion을 선언한다.
Java의 명시 총수 회귀도 309개로 맞췄다.

Root의 독립 policy 층과 합성할 때 역순은
`response scope → policy → recall → condition → sales → goal`이다.
이 branch에는 다른 worker 변경을 가져오지 않았다. Root는 각 inverse를
합성하고 policy 이전 301개 및 fixture delta를 다시 대조한다.

이 검증은 입력 보존과 generator 재현 검사다. 새 SQL 범위와 고정 oracle의
실제 결합 scenario 인수는 다음 Haiku 5.5 native dynamic Workflow에서
실행해야 한다. 전체 scenario·client·운영 gate는 NOT_RUN이며
Task/S5/S6는 OPEN이다.
