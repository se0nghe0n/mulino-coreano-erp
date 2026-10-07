# 공통 harness 인수 기록

사용자 Step2의 공통 계약을 독립 worktree에서 검증했다. baseline은
`cef526e9f77679d8429fb517184f018d64824443`, branch는
`step2/test-harness`다. source는 새 문서와 새 개발 skills만 사용했다.
옛 구현·schema·tests를 읽거나 재사용하지 않았다.

| 실제 command | 관찰 | exit |
|---|---|---|
| `./verify harness` | 18 tests, 실패0·오류0·skip0. 한국어 Gherkin1 실행 | 0 |
| `./verify contract-red` | scenario1 발견/실행, NOT_IMPLEMENTED assertion FAIL1 | 1 |
| `./verify contract-red verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json` | expected/discovered/started/NOT_IMPLEMENTED 각1, skip0 | 1 |
| `./verify validate verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json` | JSON Schema와 semantic 계약 유효 | 0 |
| `./verify schema` / `contracts` / `recovery` / `mcp` / `skills` / `model` / `deployment` | 실제 adapter 부재, NOT_RUN·gate 미완료 | 각2 |
| `./verify scenarios verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json` | 설치/조회/명령/DB 관찰 미구현, 모든 assertion NOT_RUN | 2 |

[실행 summary](evidence/common-harness/summary.json)에 정확한 source hash,
fixture hash, 각 내부 command/version/exit code와 log/JSON/JUnit/Cucumber
artifact를 연결했다. 실행 때 code는 baseline 위의 미commit 변경이었다.
이를 dirty working tree와 source hash로 표시하며 baseline에 이미 있던
제품의 성공으로 주장하지 않는다. coordinator 통합 뒤의 source와 commit은
별도로 재검증한다.

wrong quantity·duplicate physical identity·missing owner·wrong version,
unknown/missing value·incomplete scope·wrong deadline·wrong unit를 변조한
표본이 실패하는 것을 확인했다. 실제 client port는 원문 요청/허용 문맥만
받고 typed intent/oracle를 받지 않는 것을 검사했다. 미구현 parallel child도
각자 결과를 남기며 전체는 NOT_RUN이다. 이는 assertion/port selftest이며
제품 업무 simulation이나 DB/API/MCP 인수 PASS가 아니다.

첫 probe에서는 Cucumber8 JSON reporter에 필요한 Jackson Jdk8Module을
발견했다. 이를2.21.2로 추가했다. `cucumber.features` property의 JUnit6
중복 discovery 때문에 Maven exit0이 나오는 probe는 RED로 세지 않았다.
explicit resource/file selector와 실제 scenario 수 대조로 교정했다.
후속 compile 오류도 wrapper가 환경/형식 실패로 거부했으며 최종 표에
의미 있는 RED로 포함하지 않았다.

Maven3.9.16 공식 zip과 SHA512를 대조하고 wrapper SHA256을 고정했다.
Wrapper3.3.4, Cucumber8.0.4, JUnit6.1.2, Surefire3.5.6, Jackson2.21.2,
JSON Schema validator1.5.9의 resolution과 Java21 compile/discovery를
확인했다. CAP·PostgreSQL18·actual host/model·BTP·규제 인수는 NOT_RUN이다.

이 worktree에는 D/T/C/V/E case와 독립 registry/catalog가 없다. `prepare`
전체 성공이나 runtime coverage를 주장하지 않는다. coordinator가 다른
산출물과 통합하고 필수 assertion/observation/registry를 대조해야 한다.

## 공통 계약 리뷰 보완

baseline `5aec49eccd07e30f9c3ba0e3be5e38a8bedadee3` 이후 Astra 리뷰에서
여섯 가지 false PASS/실패 은폐 가능성을 발견해 수정했다. observe의
scope·실제 snapshot token과 barrier ACK의 거래/참여자/point/state를
요청과 대조한다. 실행한 source의 assertion FAIL을 미실행 source가
덮지 않는다. absent는 관찰한 container 부모를 요구하며, start는 같은
handle의 terminal await까지 요구한다. decimalDelta는 실제 baseline
단위도 검사한다. 이에 맞춰 schema/example/guide를 보완했다.

| 실제 command | 관찰 | exit |
|---|---|---|
| `./verify harness` | 26 tests, 실패0·오류0·skip0. 한국어 Gherkin1 실행 | 0 |
| `./verify validate verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json` | 새 baselineUnitSource 계약 포함 유효 | 0 |
| `./verify contract-red` | scenario1 발견/실행, NOT_IMPLEMENTED assertion FAIL1 | 1 |
| `./verify contract-red verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json` | expected/discovered/started/NOT_IMPLEMENTED 각1, skip0 | 1 |
| 제품 schema/contracts/scenarios/recovery/mcp/skills/model/deployment | 실제 adapter 부재, 각 NOT_RUN·gate 미완료 | 각2 |

[리뷰 보완 실행 증거](evidence/review-fixes/summary.json)에 source hash와
JUnit/Cucumber/console/profile artifact를 남겼다. wrong scope/revision/
physical snapshot/provenance, barrier 다섯 identity field 변조, 다른
source 미실행 때의 확인된 위반, await 누락/다른 handle/status 누락,
null/scalar absent 부모, 단위가 다른 zero delta와 schema 누락을
selftest에서 거부했다. 표본과 mechanical ACK는 제품 실행 증거가 아니다.

per-scenario Gherkin과 exact registry 보완은 다른 worker가 소유한다.
coordinator 통합 검사가 남았으므로 이 commit만으로 B2나 Step2 완료를
주장하지 않는다. 실제 서비스·DB·경합·host/model·BTP 검증은 NOT_RUN이다.
