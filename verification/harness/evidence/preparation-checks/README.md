# 준비 검사 P2 보완 증거

feature 전체의 ID 검색은 다른 scenario에 남은 같은 ID로 누락을
감출 수 있었다. registry는 entry 수와 subcase 집합만 비교해 동일
case entry로 다른 case를 대체하거나 잘못된 파일을 연결할 수 있었다.

`PreparationValidator`는 Gherkin 42.0.1의 실제 Pickle stream을 읽는다.
Background와 Scenario Outline example을 확장한 scenario마다 case 파일과
subcase를 한 번 준비하고, JSON의 모든 top-level action을 같은 순서와
역할로 실행하고, 모든 assertion을 중복 없이 연결하도록 검사한다.
이후 assertion 순서는 집합으로 비교한다. 준비 뒤 action보다 assertion이
먼저 오거나, 중복 준비·subcase·action·assertion·정의되지 않은 단계가
있으면 준비 실패다. keyword 종류와 추가 table/docstring도 검사한다.

registry의 case ID 집합은 고정된 필수 ID와 발견된 ID 모두에 정확히
일치해야 한다. case·feature는 실제 regular file의 canonical 경로를
비교한다. 다른 기존 파일, 없는 파일, repository 밖 경로와 외부를
가리키는 symlink를 거부한다. subcase ID도 중복 없는 정확한 집합이어야
한다. 독립 catalog의 기존 연결·수량·단위·source hash 검사는 유지한다.

- `harness.txt`: `./verify harness`, exit0이다. 57개 검사에서 실패·오류·
  skip0이며 새 준비 검사39개를 포함한다.
- `preparation-junit.xml`: 정상 minimal fixture, 완전한 새 harness 예제,
  Background·Outline과 누락·오염·중복·경로 변조 반례의 JUnit 결과다.
  제품 case41개의 존재를 단위 검사의 전제로 삼지 않는다.
- `contract-red.txt`: `./verify contract-red`, exit1이다. 실제 scenario1개가
  발견·실행되고 미구현 adapter의 `NOT_IMPLEMENTED` assertion으로
  실패한다. skip0이다. parser/환경 오류를 RED로 세지 않았다.
- `scenarios.json`: `./verify scenarios`, exit2·`NOT_RUN`이다. 실제 제품
  adapter와 인수 경로를 실행했다고 주장하지 않는다.
- `summary.json`: baseline commit, 변경 source와 새 예제 fixture hash,
  command/version/exit code와 catalog122 oracle·499 observation 확인을
  기록한다. 실행은 commit 전 dirty tree에서 이뤄졌음을 명시한다.
  저장한 command log는 trailing whitespace와 tab 들여쓰기만 정리했다.

Java21.0.5/Maven3.9.16에서 확인했다. compile dependency는 Gherkin42.0.1
하나를 추가했으며 messages34.2.1은 기존 Cucumber8.0.4 BOM이 고정한다.
parser API는 [공식 Java source](https://github.com/cucumber/gherkin/blob/main/java/src/main/java/io/cucumber/gherkin/GherkinParser.java)와
설치된42.0.1 jar의 public API를 대조했다. step text의 binding만 정규식으로
읽으며 Gherkin grammar/discovery를 문자열 검색으로 대체하지 않는다.

이 isolated baseline에는 제품 case41개가 없어 전체 `prepare`를 준비
완료로 보고하지 않는다. 해당 case·registry를 Task branch에 통합한 뒤
전체 준비 검사를 실행해야 한다. 이 산출물의 PASS는 제품 인수나 사용자
Step2 전체 완료가 아니다. 제품 DB/API/MCP·복구·실모델·규제·BTP 인수는
`NOT_RUN`이며 애플리케이션 구현은 추가하지 않았다.
