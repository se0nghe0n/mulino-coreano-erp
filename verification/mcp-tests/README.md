# 채널의 독립 인수 계약

같은 명사·동사 업무 세계를 실제 wire와 host에서도 유지해야 한다.
T01은 다섯 역량 질문과 제외 효과, T20은 intent·stateless MCP·MRTR·
여섯 제품 skill, T25는 전체 assertion·artifact 추적을 담당한다.
`author_cases.py`는 고정 계약 파일의 작성 도구이며 제품을 실행하지 않는다.
생성한 case.json과 한국어 scenario.feature를 함께 review한다.

MCP request의 protocolOperation은 business capability와 구분한다.
request의 HTTP header/body/version/meta/JSON-RPC ID는 그대로 전송한다.
불일치 header를 수선하거나 빠진 meta를 추가하지 않는다. 실제 결과에는
redacted raw request·response와 원 wire hash, 검증한 인증 문맥을 연결한다.
`data.transcript.request`는 실제 전송한 request다. target state와
inputResponses는 이전 실제 result를 strict reference로 받아 사용한다.
opaqueByteXor는 발급 state의 한 byte만 바꾸며 원 관찰은 보존한다.

MRTR 입력 보완의 conversation ID는 최종 effect key가 아니다.
구매 승인은 immutable proposal hash/revision에 결합하며 변경 시 재승인이
필요하다. 승인 state 소비 경합은 두 실제 wire start, 각각의 barrier,
첫 거래 terminal/commit, 두 번째 거래 resume/terminal, 독립 DB 관찰을
명시한다. 일반 입력 state를 무조건 single-use로 가정하지 않는다.
manager decision 없는 requestState나 accept 문자열은 승인이 아니다.

T25의 verifyCoverage는 `inputSnapshotKind`로 고정한 입력 snapshot을 읽는
독립 검증이다. `PREPARATION`은 `./verify prepare` 보고, 
`REQUIRED_PATH_RUNTIME_EVIDENCE`와 `APPROVED_MODEL_EXECUTION_EVIDENCE`는
실제 runtime manifest, `MODEL_BINDING_PREPARATION`은 모델 binding 준비
산출물이다. live runtime manifest에서 NOT_RUN을 기대하는 subcase는 없다.
같은 실행 중 T25 자신의 PASS를 선행 조건으로 요구하지 않으며
`currentExecution={caseId:T25}`의 link만 CURRENT_EXECUTION으로 둔다.
전체 제품 gate는 모든 case/profile 이후 coordinator의 별도 assembler가
판정한다. runtime-links-required는 나머지 필수 경로의 PASS link·exit0
artifact를 요구하고, mutant는 변조 전 PASS→변조 뒤 FAIL 전이를 본다.

host rawRows는 catalogOracles/catalogObservations의 규범 tuple,
namedObservations의 실제 assertionLinks, runtimeArtifacts의 실제
path/sha256/sizeBytes/scope/fixtureHash/codeCommit/command/expected/observed/
exitCode, validation.issues와 계층별 gate를 반환한다. namedObservations는
독립 catalog 선언순서이며 observationName과 oracleId를 유지한다.
link count나 server Boolean으로 이 연결을 대신하지 않는다.
여러 assertion에 연결한 conjunction의 의미는 별도 QA content review로
확인한다. semanticReview의 PASS는 실제 review artifact가 있어야 한다.

verifyCoverage의 mutation 입력은 임시 격리 사본의 정확한 삭제/변조를
선택한다. mutatedInput artifact와 validation issue를 함께 관찰하며
원 registry/catalog/source를 수정하지 않는다. dropOracle,
dropObservation, removeRuntimeArtifact, replaceWithStubPass, skipCase,
mandatoryWaiver, partialPass, confirmedViolation을 각각 검사한다.
원문/관찰 누락을 스스로 고쳐서 PASS를 만들지 않는다.

통합 모델 경로는 `verification/model-binding/registry.json`과
`verification/harness/target/evidence/model-binding-preparation.json`이다.
전체 실행 manifest는
`verification/harness/target/evidence/runtime-manifest.json`이다.
M60 source/binding은 읽기 전용이며 실제 model transcript와 usage/cost는
준비 metadata와 구분한다. category는 원문에 대응하는 명시 mapping으로
20/10/10/10/10을 대조한다. optional cache/reasoning metric의 null과
missingReason을 보존하고 실제 모델·규제·BTP 증거를 생성하지 않는다.

ChannelsContractTest의 CAPTURED_ASSERTION_SELFTEST 표본은 실제
AssertionEngine의 틀린 수량/단위, 중복 실물, 누락 책임, 다른 snapshot,
수선 wire, loading 없는 hash, 악성 문서의 COMMAND, case 삭제,
NOT_RUN 승격과 비용0 대입을 검사한다. 제품 fake나 host adapter가 아니다.

최종 검증은 공통 dependency `77c2df3` 위의 고정 case hash를 대상으로 했다.
`./verify validate`는 T01/T20/T25 schema를 통과했고 `./verify harness`는
150 tests(채널 20개), failure/error/skip0으로 통과했다. 실제 Gherkin
selector RED는 expected/discovered/started/NOT_IMPLEMENTED failure가 모두
90개, scenario skip0, wrapper exit1이다. 대표 `./verify mcp`는 T20의
56개 subcase에서 NOT_RUN·gateComplete=false·exit2를 반환했다.
`evidence/validation-summary.json`과 실제 wrapper/log/result를 함께 보존한다.
실제 제품·client·model·BTP·규제 인수는 수행하지 않았다.

## 재검토 수정과 generator 재현성(2026-10-08)

T01·T20·T25의 case.json·fixture.json·scenario.feature는 이 generator의
출력과 byte 단위로 같아야 한다. 4233e7ec 뒤 손으로 고친 T20 discovery
변경(tools-list의 connectionState·clientAcceptsServerRequests, 설명문)을
generator로 옮겼고, tools/list의 raw method·header·meta와 HTTP200·
jsonrpc/id·resultType 단언을 추가해 다시 생성했다. T01 출력은 바뀌지
않았다. `verification/platform/protocol/compatibility-repair.md`의
"generator를 같은 계약으로 고쳤다"는 문장은 당시 사실과 달랐으며 이
worker의 소유가 아니라 고치지 않았다.

T20의 wire 오류, MRTR TTL·결속 코드, 여섯 skill 절차, host RECORD 쓰기,
allowed-tools 서버 거부와 T25 입력 종류의 내용은 각 case README에
있다. 재현 확인 명령은 다음과 같다.

```sh
python3 -I verification/mcp-tests/author_cases.py
git diff --exit-code verification/cases/T01 verification/cases/T20 verification/cases/T25
```
