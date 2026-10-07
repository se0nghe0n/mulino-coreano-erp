# 전체 준비·실행 증거 assembly

준비된 case 수와 고정 표본의 PASS로 실제 제품 인수를 완료했다고
표시하지 않기 위해 전체41 case와 모델60 case의 증거를 따로 조립한다.
이 도구는 파일을 읽고 기존 준비 검사를 호출할 수 있을 뿐 제품 행동,
DB adapter, 실제 모델 호출이나 배포를 구현하지 않는다.

## 실제 명령과 산출물

repository root에서 Python 표준 라이브러리만으로 실행한다.

```sh
python3 verification/coverage/assemble.py
python3 verification/coverage/assemble.py --check-preparation
python3 verification/coverage/validate.py
python3 -m unittest discover -s verification/coverage -p 'test_*.py' -v
```

`assemble.py`는 `verification/harness/target/evidence/runtime-manifest.json`을
생성한다. runtime PASS는 exit0, 확인된 위반은 exit1, 필수 경로 미실행은
exit2다. 형식·환경 오류는 exit3이다. 출력 파일은 Git에 commit하지 않는다.
`validate.py`의 exit0은 저장 manifest가 현재 입력과 일치한다는 뜻이다.
manifest가 NOT_RUN이면 제품 PASS가 아니다.

`--check-preparation`은 case·registry·catalog·fixture의 필수 입력이 모두
존재할 때 현재 checkout의 `./verify prepare`를 새로 실행한다. 실제
Gherkin Pickle/parser, JSON Schema와 정확한 registry 검사를 재사용한다.
그 결과의 commit과 compiled Main hash도 확인한다. 이 옵션 없이 이전
준비 결과만 보고 전체 PREPARED를 새로 주장하지 않는다. 입력 부재는
NOT_RUN, source/hash/identity의 관찰된 불일치는 FAIL이다.

전체 preparationStatus=PREPARED는 이 fresh 제품 case 준비와 source를
대조한 model-binding 준비를 모두 요구한다. runtimeStatus, runtimeComplete,
gateComplete, artifactCoverageStatus와 별도다. semantic oracle 동등성은
case와 실제 실행의 review가 필요하며 단순 연결 검사로 증명하지 않는다.

## 입력 index와 실제 실행 receipt

`runtime-evidence-index.json`은 현재 빈 실제 실행 목록이다. coordinator가
실제 runner의 증거를 통합한 뒤 별도 경로를 `--index`로 지정할 수 있다.
root `./verify`의 의미나 다른 작성자의 runner는 이 변경에서 수정하지 않는다.

```json
{
  "schemaVersion": "1.0.0",
  "profiles": [{
    "profile": "scenarios",
    "reportRef": "verification/harness/target/evidence/actual-scenarios.json",
    "receiptRef": "verification/harness/target/evidence/scenarios-receipt.json",
    "evidenceClass": "ACTUAL"
  }],
  "modelBindingReportRef": "verification/harness/target/evidence/model-binding-preparation.json"
}
```

이는 형식 설명이며 실제 존재하는 runtime report 예제가 아니다.
`runtime-evidence-index.schema.json`과 `execution-receipt.schema.json`을 따른다.
ACTUAL entry에는 별도 receipt가 필수다. SELFTEST·CONTRACT_RED entry는
제품 runtime PASS로 세지 않는다. 필수 MODEL·BTP를 waiver로 대신하지 않는다.

receipt에는 실제 codeCommit, executionIdentity의 runId/hostId/workspaceId/
actorId, command의 argv/display/exitCode/startedAt/completedAt, versions의
schema/definition/evaluator/policy/tool과 실제 경로별 DB/protocol 또는
client/model/prompt/skill version이 필요하다. receipt.reportArtifact는
report의 현재 path/hash/sizeBytes와 정확히 같아야 한다. report의 commit,
execution identity, command display, exitCode도 receipt와 같아야 한다.

`inputs[]`는 사용한 case·fixture·base의 실제 path/hash/sizeBytes를 포함한다.
`artifacts[]`에는 같은 descriptor와 scope/completeness=COMPLETE를 둔다.
각 actual artifact JSON은 다음을 포함한다.

- ACTUAL/ACTUAL_HOST/ACTUAL_RUNTIME evidenceClass와 receipt와 같은
  executionIdentity, command, versions, scope를 기록한다.
- independent=true, 실제 ACTUAL_ source인 provenance를 기록한다.
- 실제 profile report와 정확히 같은 `profileResult`를 기록한다.
- 실제 actionId별 StepResult와 정확히 같은 `observations`를 기록한다.
  모델 실행은 모든 실제 `modelAttempts`를 같은 bytes에 연결한다.

assembler는 위 JSON을 직접 읽어 metadata뿐 아니라 실제 결과 payload도
대조한다. label만 바꾼 canned/selftest source, 이미 알려진 test resource
경로, 다른 execution identity, scope, hash, bytes나 action response를 거부한다.
JSON의 ACTUAL 표지는 외부 시스템의 진실성을 자동 증명하지 않는다.
실제 adapter·인증·독립 extractor의 실행 인수는 별도로 필요하다. 여기의
검사는 무결성과 연결 계약이며 metadata를 위조한 시스템의 attestation을
새로 발급하지 않는다. raw credential은 report나 artifact에 저장하지 않는다.

## case·oracle·artifact 연결

독립 catalog122 oracle의 모든499 named observation을 선언순으로 유지한다.
각 observation의 oracleId/observationName/caseId/expected/requiredProfiles와
assertionLinks를 출력한다. 링크는 실제 case/subcase/assertion ID, profile,
status, evidenceRefs를 가진다. quantity의 고정 값·단위·비교 operator를
검증하는 주 assertion과 보조 관찰의 구별은 fresh 공통 CatalogLinkValidator에
위임한다. 보조 assertion 전부에 aggregate 수량을 반복하도록 요구하지 않는다. ID 개수만 같고 조항 하나를 빠뜨리면 준비 완료가 아니다.

case 결과는 caseHash·fixture/base hash·version·command interval·정확한
모든 action/모든 assertion membership을 대조한다. source action은 실제
EXECUTED이고 captured artifact의 StepResult와 같아야 한다. 독립 DB 관찰은
query/snapshot/scopeComplete/independent를 요구한다. assertion의 실제
source bytes를 observed로 남기며 미관찰을0이나 빈 배열로 바꾸지 않는다.
일부 action/assertion/profile 미실행이나 skip은 NOT_RUN이다. 확인된 FAIL은
receipt/artifact가 나중에 누락돼도 NOT_RUN으로 낮추지 않는다.

`runtimeArtifacts[]`는 실제 path/hash/sizeBytes에 case/subcase/profile/
assertionId/status/fixtureHash/codeCommit/command/versions/expected/observed/
exitCode를 연결한다. `runtimeArtifactDescriptors[]`는 후속 host
inspectArtifacts용 순수 path/hash/sizeBytes/scope/completeness 배열이다.
scope는 actual receipt와 캡처 파일에서 읽으며 manifest가 만들어 내지 않는다.
actual 증거가 없으면 두 배열은 빈 배열이고 전체 runtime은 NOT_RUN이다.

schema→contracts→scenarios/recovery→mcp/skills→model/deployment의 필수
선행을 확인한다. local-deployment와 btp-deployment는 별도 profile이며
case의 기존 deployment 선언을 둘로 펼친다. regulatory도 별도 필수
profile이다. 관찰의 requiredLayers는 각 필수 profile에 연결한다.
그 경로에 실행 assertion이 없으면 해당 관찰을 PASS로 바꾸지 않는다.

T25의 verifyCoverage는 지정된 입력 snapshot의 연결·누락·상태 분류를
검사한다. snapshot 자체나 T25 결과가 전체 gate PASS일 필요는 없다.
T25를 포함한 모든 결과를 나중에 assembler가 통합 판정한다. 현재 T25
결과를 T25의 선행 입력으로 요구하는 순환을 만들지 않는다.

## 모델 준비와 runtime

모델 준비는 실제 corpus60 case·73 turn·221 공통 assertion·154 공통
semantic path를 model-binding registry/binding/source pointer와 대조한다.
준비 report의 corpus/registry hash와 inputArtifacts의 모든 binding,
fixture, feature, semantic path 및 runner/schema source hash도 직접 읽는다.
동일 count를 유지하면서 다른 case/turn/pointer/path로 바꾸는 변조를 거부한다.
기존 binding parser/schema 검사는 해당 작성자의 준비 report가 맡는다.

실제 runtime은60 case×3회=180 unique attempt와73 turn×3회를 별도
검사한다. turnResults의 turnId와 각 assertionId/semanticPath가 준비된
공통 assertion에 정확히 대응해야 한다. 실제 모델 호출은 최소219이며
partial/skipped attempt, assertion 실패, artifact 불일치와 usage/cost
누락은 전체 MODEL gate를 닫지 못한다. provider의 input/output token,
cost의 amount/currency/pricingRef가 필요하다. 없는 실제 usage·cost·call
수는 null과 missingReason으로 남긴다. 준비/RED report의 calls0을 실제
모델 실행 관찰로 복사하지 않는다.

## selftest와 현재 제한

`test_coverage.py`의 임시 receipt·binding·capture는 SELFTEST다. 일부
unit 검사에서 ACTUAL protocol 모양을 검증하더라도 실제 제품 실행이나
모델 PASS 증거가 아니다. fake 서비스나 모델을 실행하지 않는다.
새 Java `coverage/CoverageSchemaTest`는 실제 새 schema와 기존 base manifest
schema의 호환성, completion 변조와 receipt 없는 ACTUAL index를 검사한다.

`checks/`에는 B2의 실제 assembly NOT_RUN, 저장 manifest validation,
Python/JUnit selftest command/version/exit와 source hash를 기록한다.
제품 case41개와 binding은 다른 writer가 작성 중이므로 이 isolated
baseline의 파일 부재를 성공으로 세지 않는다. 통합 후 fresh prepare와
binding preparation 및 실제 필수 runtime을 연결해야 한다. 이 산출물의
selftest PASS는 사용자 Step2 전체 완료나 제품 DB/API/MCP/client/model/
규제/BTP 인수가 아니다.
