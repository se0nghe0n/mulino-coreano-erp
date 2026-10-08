# T25 전체 추적과 실제 증거의 분리

ID나 파일의 수만 맞추면 누락된 업무 oracle를 숨길 수 있다.
독립 catalog의41 case·D26·122 oracle·499 named observation과
실제 assertion link를 대조한다. 원문 tuple의 type/scope/operator도
같아야 한다. 각 named observation의 assertionLinks는 비어 있으면 실패다.

verifyCoverage는 명시한 선행 입력 snapshot을 독립적으로 검사한다.
입력 종류는 `inputSnapshotKind`로 고정한다.

| 값 | 입력 | 의미 |
|---|---|---|
| `PREPARATION` | `preparationReportPath`=`target/evidence/prepare.json` | 준비 보고만 읽는다. runtime NOT_RUN·artifact0·model/규제 NOT_RUN은 이 입력의 성질이다 |
| `REQUIRED_PATH_RUNTIME_EVIDENCE` | `manifestPath`=`target/evidence/runtime-manifest.json`, `currentExecution` | 인수 대상 실행의 실제 runtime manifest다. 현재 T25 link만 CURRENT_EXECUTION으로 구별하고 나머지 필수 경로는 PASS여야 한다 |
| `MODEL_BINDING_PREPARATION` | `modelManifestPath`=`target/evidence/model-binding-preparation.json` | 모델 binding 준비 산출물이다 |
| `APPROVED_MODEL_EXECUTION_EVIDENCE` | runtime manifest | 승인된 실제 M60×3 실행 증거다 |

검증기는 읽은 종류를 `input.snapshotKind`로 반환한다. live runtime
manifest를 읽으면서 NOT_RUN을 기대하는 subcase는 없다. 그래서 S6에서
실제 증거가 생겨도 준비 subcase와 runtime subcase가 서로 모순되지 않는다.
currentExecution은 현재 T25 결과를 그 실행의 선행 입력으로 요구하지
않도록 표시한다. 전체 gate는 이 결과를 포함한 모든 case/profile 이후
coordinator의 별도 assembler가 판정한다. preparation과 실제 runtime
artifact 검사는 별도 subcase다. 실제 artifact descriptor는 후속
inspectArtifacts에서 bytes/hash/scope를 직접 검증한다.

oracle/관찰/실행 artifact 삭제, stub PASS, skip, 필수 경로 waiver,
부분 실행 PASS, 확인된 위반을 숨긴 결과를 각각 거부한다. 각 mutant는
같은 입력의 변조 전 사본이 `baseline.validation.status=PASS`임을 함께
요구해 PASS→FAIL 전이만 거부 증거로 센다. runtime artifact를 다루는
네 mutant는 실제 runtime 입력을 쓰며 변조 전 필수 경로가 PASS이고
변조 뒤 PASS가 아님을 확인한다. removeRuntimeArtifact는 변조 전 입력에
T20 artifact가 실제로 있어야 한다.

runtime-links-required는 link의 존재만 보지 않는다. 각 named observation에
PASS link가 하나 이상 있고, 평면화한 link와 runtime artifact에 FAIL·
NOT_RUN이 없어야 한다. artifact exit code는 1·2·3 각각0건에 더해
`runtime-artifacts-all-exit0`·`runtime-artifacts-all-pass`가 전체 artifact의
exitCode가 정수0, status가 PASS임을 요구한다. 필터한 목록이 전체 목록과
같아야 하므로 4·137·문자열 '1' 같은 값도 실패한다.

PREPARATION 입력을 읽는 다섯 subcase(coverage-none, dropOracle,
dropObservation, skipCase, mandatoryWaiver)는 준비 보고가 지금 검증하는
checkout의 것임을 요구한다. 검증기 rawRows `input`에 준비 보고의
`codeCommit`·`workingTreeDirty`와 검증기가 읽은 현재 checkout의
`checkoutCommit`·`checkoutDirty`를 둔다. 두 tree는 clean이고 두 commit은
같아야 한다. 이 네 field는 verifyCoverage 출력 계약의 확장이며
host-observation-guide.md·HostObservationValidator 반영은 harness 소유자에게
요청했다(docs/execution/step2r-cases3/README.md).
evidence-wrapper-fields는 법규 검토의 출처·관할·적용일·검토자와 실모델
비용 승인 필드를 요구하고 waiver된 법규 검토를 거부한다. command/version/
fixture hash/expected/observed/exit의 누락도 실패다. QA content review는
별도 실제 artifact로 연결하며 구조검사 성공이 semantic completeness는 아니다.

M60은60건·각3회·최소 외국어/혼합10건이다. 원문 category의
20/10/10/10/10 대응과 binding60/turn73을 읽는다. 실제 model transcript는
별도다. 승인·attempt/retry usage·가격·통화·총비용·version·지연·확인질문이
없으면 MODEL/UAT는 NOT_RUN이며 null/missingReason을0으로 바꾸지 않는다.
R8 전 제안 수용값을 업무 SLA로 확정하지 않는다.

새 제품·host·model·규제·BTP 결과는 이 디렉터리에서 만들지 않는다.
현재 고정 검사와 Gherkin RED는 계약 준비의 증거다.

## 2라운드 profile·artifact 연결(2026-10-08)

PASS link가 하나 있다는 것만으로는 필수 계층을 지났는지 알 수 없다.
API profile의 PASS 하나로 MCP를 요구하는 관찰이 통과했고, DB
snapshot이 없는 관찰도 통과했다. verifyCoverage 출력 계약을 아래처럼
넓힌다. 실제 verifier 구현은 Step 3/S6 소유다.

- runtime-links-required: T25 자신을 뺀 모든 named observation i에 대해
  - `namedObservations/i/assertionLinks`에 catalog requiredLayers가
    대응하는 profile마다 `status=PASS`·`profile=<p>` link가 한 건
    이상 있다(`profile-links-NNN-<p>`). 대응은 assembler의
    LAYER_PROFILE과 같다(UNIT→contracts, API/DB→scenarios,
    MCP→mcp, SKILLS→skills, MODEL→model, LOCAL/BTP→local-/
    btp-deployment, REGULATORY_REVIEW→regulatory).
  - `namedObservations/i/artifacts`에 catalog artifactKinds마다
    `artifactKind=<k>`·`status=PASS` artifact가 path·sha256·크기·
    case·subcase·profile과 함께 있다(`artifact-kind-NNN-<k>`).
  - `profiles`에 10개 profile 결과가 각자 한 행이며 모두 PASS다
    (`profile-result-<p>`).
- evidence-wrapper-fields: 모든 기록이 `profile`을 가진다. mcp 기록은
  protocol·tool·DB version, skills 기록은 skill·client·protocol
  version을 가진다. scenarios·mcp wrapper는 DB·protocol version을
  드러낸다. 평면 `artifacts`에 db_snapshot(DB version 포함),
  api_response, protocol_transcript(protocol version 포함) 행이 있다.
- actual-model-usage-required: M60×3 각 실행의
  `protocolTranscriptStatus=VERIFIED`, `skillDiscoveryStatus`·
  `skillBodyStatus=OBSERVED`를 요구하고, trace artifact hash를 검증하지
  못한 attempt는 0이다.

catalog의 T25 네 관찰(coverage-link, result-separation, evidence-
fields, wrapper-truth)은 기본값 `api_response`·`db_snapshot`을
artifactKinds로 갖고 있었다. T25는 업무 API를 부르거나 업무 DB를 읽지
않으므로 이 artifact를 스스로 만들 수 없다. DB 관찰을 T25에 덧붙이면
coverage와 무관한 형식적 probe가 된다. 그래서 규범 lock 절차로
`coverage_report`·`runtime_manifest`(evidence 두 관찰은
`wrapper_record` 추가)로 바로잡았다. requiredLayers·기대 predicate·
관찰 이름은 그대로다. DB·API·MCP·SKILLS 증거 요구는 위의 per-profile·
per-artifactKind 검사로 옮겨 오히려 강해졌다(lock `reviewUpdates[2]`).
