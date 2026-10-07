# Step 2 영역별 테스트 작성 인계

현재는 공통 runner의 review 수정 중이다. B2가 명시되고 coordinator가
별도 worktree를 지정하기 전에는 읽기 조사만 수행한다. 이후 각 writer는
같은 B2에서 시작하며 아래 소유 범위의 테스트만 작성한다.

사용자 Step 2의 실제 모델은 GPT-6.1 Sol high다. 전체 Step 2의 tests
납품·두 모델 review·지적 수정·통합 검사가 끝나야 Step 3으로 넘어간다.
독립 소유권은 작업량을 나눠 critical path를 줄이기 위해 기존 제안의
trade/security를 세분화했다. 업무 요구나 oracle는 줄이지 않는다.

| 소유 단위 | 독점 case ID |
|---|---|
| definitions | T02, T07, T21, V1 |
| inventory | T03, T04, T05, T16, C1, V2, V3 |
| work | T09, T10, T11, T12, C2, C5 |
| trade-supply | T13, T14, T15, T19 |
| trade-sales | T17, T18, C4, E1, E2 |
| authority | T08, C3, V4, V6, V7 |
| evidence | T06, T22, T24 |
| runtime | T26, V5 |
| platform | T23, V8 |
| channels | T01, T20, T25 |

## 입력과 소유권

AGENTS, 설계 철학, 구현 계획 전체와 해당 절, 새 개발·Agent·테스트
skills 및 그 관련 reference, 공통 harness guide/schema/example을 읽는다.
독립 `verification/requirements/mandatory-oracles.json`의 배정 case별
모든 oracle와 named observation을 구현 계획 원문에 대조한다.
옛 코드/schema/tests/skills/운영 문서는 읽거나 재사용하지 않는다.
#57 방법론은 새 scenario-testing skill에 보존된 범위만 참고한다.

배정받은 `verification/cases/<ID>/`의 case.json, fixture, 한국어
scenario.feature, 수량·책임 계산/관찰 설명과 증거를 소유한다. 해당
영역의 assertion 자체를 검증할 표본·mutant JUnit 파일은 명시된
`verification/harness/src/test/java/org/mulino/verification/cases/<area>/`
아래만 추가한다. 공통 runner/schema/pom/registry/catalog는 바꾸지 않는다.
필요한 capability나 표현이 없으면 coordinator에게 구체 요구를 보낸다.

platform은 추가로 `verification/platform-tests/`, channels는
`verification/mcp-tests/`와 `verification/skills-tests/`를 소유한다.
corpus source와 별도 binding은 다른 담당자가 작성한다. registry와
최종 coverage/evidence manifest는 coordinator가 통합한다.
다른 writer가 wider workspace에서 작업하므로 타인 변경을 보존한다.

## 인수 조건

- 모든 필수 observation은 실제 response/DB 원 행/control/host artifact를
  읽는 substantive assertion에 연결한다. 이름이 같은 서버 Boolean을
  만들거나 presence만으로 conjunction을 대신하지 않는다.
- fixture는 actor·role·grant·범위·시계·정의/evaluator/정책·식별된 물량·
  단위·원천/증거·인간 책임을 명시한다. implicit wildcard/auto-approval,
  expected output을 미리 seed한 효과 검증은 금지한다.
- 각 subcase의 모든 행동과 assertion을 한국어 Gherkin에 표현한다.
  installFixture도 행동이다. 한 Given에 전체 workflow를 숨기지 않는다.
  서버 발급 ID·hash·revision·snapshot은 strict alias/result ref로 연결한다.
- API 결과와 독립 DB scope/snapshot의 quantity·identity·relations·
  obligation·audit·outbox를 함께 검증한다. 거부 시 허용 감사/대조 책임과
  금지된 업무 효과0을 구별하고 실제 전후 snapshot을 비교한다.
- 수량은 독립 손계산과 실제 단위를 검증한다. decimalDelta에는 현재
  unitSource와 baselineUnitSource가 필요하다. count/set/owner도 정확한
  scope를 갖는다. 미확인·누락·상충을0이나 빈 결과로 바꾸지 않는다.
- 경합은 async start → 해당 barrier reached ACK → 다른 commit → resume
  → 각 handle의 terminal await → 독립 관찰 순서를 사용한다. 요청/ACK의
  거래·대상·시점이 일치해야 한다. sleep이나 mock lock을 쓰지 않는다.
- E1은 구매·승인·발주 전달·수령·QC·기관 허용·판매주문·예약·출고·인도·
  반품 허가·수령을 명시적으로 실행한다. 선행 효과를 adapter에 숨기지 않는다.
- 필요한 표본 검사에서는 실제 AssertionEngine을 재사용해 틀린 수량·
  단위·중복 실물·누락 owner·오염 version 등을 거부하는지 확인한다.
  이 고정 표본은 assertion 검사이며 앱 behavior를 구현하는 fake가 아니다.
- 각 case schema 검증과 실제 Gherkin file-selector contract RED를 실행한다.
  기대/discovered/started/NOT_IMPLEMENTED failure 수가 모든 subcase 수와
  일치하고 skip0이어야 한다. exit1은 의도된 미구현 RED다. 구문·환경 오류는
  RED가 아니다. 실제 제품 profile은 NOT_RUN이며 제품 PASS를 주장하지 않는다.
- common source 변경 없이 필요한 추가 JUnit 검사를 Maven 기본 발견 경로에
  넣고 전체 harness를 실행한다. 실행하지 않은 adapter/model은 NOT_RUN이다.

각 writer는 commit, 변경 파일, case/subcase/assertion 수, catalog observation
연결 수, 검증 명령·exit·실제 결과, 남은 문제를 반환한다. coordinator만
Task integration branch에 통합하며 main·remote 쓰기는 하지 않는다.
