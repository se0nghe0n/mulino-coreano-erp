# host/process 독립 관찰 계약

제품 API 응답·DB 원 행과 실제 host 실행은 다른 증거다. process control의
단순 ACK나 서버 `leakCount=0` 응답으로 파일 검사·복구·schema·client·배포
인수를 대신하지 않는다. 이 계약은 실제 adapter가 반환할 증거를 고정한다.
Step2의 고정 관찰 표본은 validator selftest이며 실제 process를 실행하지
않는다. Step3 adapter가 없으면 `NOT_IMPLEMENTED`, 제품 case는 `NOT_RUN`이다.

`HostObservationValidator.validate(ContractValidator, JsonNode, StepResult,
boolean requireActualHost)`의 두 번째 인자는 `$alias`/`$result` 치환을 마친
`action.control`이다. 제품 profile 실행(`CaseRunner` 기본 PRODUCT 정책,
`Main`·Gherkin)은 `requireActualHost=true`로 호출한다. 이때
`evidenceClass=ACTUAL_HOST`와 provenance `source=ACTUAL_HOST_PROCESS`만
받는다. `CAPTURED_SELFTEST`/`CANNED_CONTRACT_SELFTEST`는 라벨이 서로
맞아도 거부하며 실행은 exit3 형식 오류다. 세 인자 형태와
`CaseRunner.harnessSelftest(...)`는 harness 단위 시험 전용이다. 일반 DB
`observe`는 기존 observation 계약을 쓴다. `type=process`의 `EXECUTED` 결과는
`data.hostObservation`에 아래 schema를 따른다.

```text
contracts/acceptance-host-observation.schema.json
```

## provenance와 실제 파일

- `evidenceClass=ACTUAL_HOST`는 StepResult provenance의
  `source=ACTUAL_HOST_PROCESS`와 함께 쓴다. 고정 selftest는
  `CAPTURED_SELFTEST`와 `CANNED_CONTRACT_SELFTEST`로 구별한다.
  raw rows·runtime snapshot JSON의 evidenceClass도 같은 값이어야 한다.
  command/extractor transcript는 JSON root의 evidenceClass 또는 첫 줄
  `evidenceClass=ACTUAL_HOST` metadata를 둔다. selftest 표지가 남은
  artifact의 host/provenance 두 label만 바꾸면 실패한다.
  ACTUAL_HOST provenance는 independent=true이고 adapter/adapterVersion/
  buildVersion에 SELFTEST·captured 표지가 없어야 한다.
- `requestedInputs`는 치환된 `control.parameters` 전체와 정확히 같다.
  `scope`는 비어 있지 않은 object며 실제 extractor scope와 같다.
  요청에 scope가 있으면 그 값도 같다. `scopeComplete=true`가 필요하다.
- `command`는 redaction을 마친 실제 `argv[]`, 실제 `exitCode`,
  `startedAt`, `completedAt`, `transcriptRef`, `redacted=true`를 가진다.
  argv는 관찰 metadata다. harness는 이 배열을 shell로 실행하지 않는다.
  command 실패 exit도 실제 실행 사실이며 업무 성공을 뜻하지 않는다.
- `toolVersions[]`에 실제 도구 이름/버전을 기록한다. `environment`는
  `profile`, `hostId`, `workspaceId`, `isolated=true`를 기록한다.
  BTP probe는 BTP, client probe는 CLIENT의 실제 관찰만 받는다.
  local 검사로 MODEL·REGULATORY·BTP·CLIENT gate를 닫지 않는다.
- `inputArtifacts`, `generatedOutputs`, `observedArtifacts`는 분리한다.
  각 descriptor는 `path`, SHA-256 소문자64자리 `sha256`, integer
  `sizeBytes`, 비어 있지 않은 `scope`, `completeness=COMPLETE`를 가진다.
  요청 artifact의 identity는 path/hash/bytes/scope로 고정한다. 생성할
  output의 미래 hash를 입력에 요구하지 않는다.
- 각 path는 repository 안의 실제 evidence 파일이다. validator는
  파일의 bytes/hash를 직접 읽으며 repository 밖 symlink도 거부한다.
  원문 secret·Authorization·Cookie·token을 artifact에 저장하지 않는다.
  redacted transcript와 별도 원문 hash·검증된 인증 metadata를 사용한다.
  scan은 발견한 원문을 출력하지 않고 pattern ID/byte offset/length만 남긴다.

redaction 후 파일 hash와 원문 hash는 서로 다른 값이다. 이 계약의
`artifact.sha256`는 검사하는 실제 저장 파일의 bytes를 가리킨다. 원문
hash metadata로 다른 redacted 파일의 무결성을 주장하지 않는다.

## 독립 extractor와 좁힌 operation

`extractor`는 `name`, `version`, `source=FILESYSTEM_READ_ONLY|HOST_TOOL_READ_ONLY`,
`independent=true`, `readOnly=true`, 실제 read-only `command`, hash/bytes/scope가
있는 `inputArtifacts[]`를 기록한다. extractor command는 exit0이어야 완전한
관찰이다. 생성/검사 output마다 동일한 descriptor가 extractor input에 있어야
한다. 제품 write API나 서버 요약 Boolean은 독립 extractor가 아니다.

`rawRows`와 `rawRowsArtifactRef`, 또는 `transcriptRefs[]`를 둔다. raw rows는
참조한 실제 JSON 파일의 내용과 정확히 같다. transcript 경로도 extractor
input hash와 StepResult artifactRefs에 연결한다. 기계 검증할 transcript는
아래 identity와 metadata를 포함한 redacted JSON을 반환한다. transcript 문자열을 추측해 업무
결과로 바꾸지 않는다.

독립 artifact의 evidenceClass, operation, scope, command, environment,
driverProvenance는 각각 선언한 host metadata와 StepResult provenance의
같은 값을 가진다. 아래 snippet은 identity 예시만 보이며 전체 artifact에는
이 metadata도 필수다. label만 바꿔 다른 환경·명령의 관찰로 재사용하지 않는다.

```json
{
  "evidenceClass": "ACTUAL_HOST",
  "scope": {"environmentId": "isolated-ontology-v2"},
  "operationEvidence": {
    "environmentId": "isolated-ontology-v2",
    "fromVersion": "ontology-v1",
    "toVersion": "ontology-v2",
    "migrationOwner": "Flyway"
  }
}
```

`operationEvidence`는 실제 read-only rows/transcript의 동일 field와 정확히
같다. 아래 identity key를 모두 요구한다. request에 동일 key가 있으면 그
요청값과도 정확히 같아야 한다. 목록 밖 범용 shell operation은 없다.

| operation | 독립 identity / 입력 의미 | 실제 output·terminal 인수 |
|---|---|---|
| start | processId, environmentId / 명시한 parameters.processId | 실제 미기동→RUNNING instance와 terminal snapshot |
| stop | processId, environmentId / 명시한 parameters.processId | 실제 전후 process instance/status와 정지 terminal snapshot |
| restart | processId, environmentId / 명시한 parameters.processId | 실제 새 instance의 RUNNING과 전후 terminal snapshot |
| tickScheduler | schedulerId, tickId, submissionStatus / due scope와 고정 clock. 수동 관찰이면 trigger·observationWindowSeconds·triggeredBy | SUBMITTED의 실제 taskId/handle 또는 관찰된 NO_TASK; 완료 업무와 구별한다 |
| claim | schedulerId, claimId, leaseId, fencingToken / claim scope | 실제 lease/fence rows; 요청 taskId/invocationHandle은 관찰 identity와 exact 대조한다. stale worker 효과는 DB assertion으로 검사한다 |
| sweepDue | schedulerId, sweepId, submissionStatus / due scope와 clock. 수동 관찰이면 trigger·observationWindowSeconds·triggeredBy | SUBMITTED의 실제 taskId/handle 또는 관찰된 NO_TASK; 예약 전이·의무·출고0은 독립 DB로 검사한다 |
| awaitRuntimeTask | schedulerId / 실제 taskId 또는 invocationHandle | 아래 autonomous task terminal과 snapshot을 모두 검사한다 |
| archiveInventory | repositoryId, baselineCommit, inventoryId | 실제 파일별 hash·분류·보존 위치 inventory artifact |
| archiveRestore | archiveId, restoreEnvironmentId, restoredCommit | 실제 archive를 읽은 isolated 복원 artifact·대조 report |
| dataInventory | environmentId, inventoryId, authoritativeSourceId | 실제 authoritative DB/blob/외부효과/미해결 업무 inventory |
| schemaInstall | environmentId, schemaVersion, migrationOwner | 실제 빈 DB 설치·migration 결과 artifact; owner는 Flyway |
| schemaUpgrade | environmentId, fromVersion, toVersion, migrationOwner | 새 ontology의 서로 다른 version upgrade artifact; owner는 Flyway |
| compileSchema | compilerId, compilerVersion, sourceRevision | 고정 compiler/source의 actual CSN/schema artifact |
| compilerSchemaProbe | compilerId, compilerVersion, schemaVersion | actual compiler schema·DB schema 비교 artifact |
| backup | environmentId, backupId, snapshotId | 실제 DB/blob/definition/capability/evaluator/skill/config/배포 bundle |
| restore | backupId, restoreEnvironmentId, snapshotId | actual backup을 읽은 isolated restore 결과 artifact |
| cutoverStage | environmentId, cutoverId, stage | 지정된 하나의 실제 stage 결과 artifact |
| deploymentProbe | deploymentId, environmentId, profile=BTP | 실제 BTP binding/auth/TLS/실행 report |
| clientProbe | clientId, clientVersion, profile=CLIENT | 대상 실제 client 발견/loading/tool 왕복 report |
| retentionSweep | environmentId, policyVersion, sweepId | actual 보존 policy·legal hold·삭제/보류 report |
| inspectArtifacts | inspectionId / 이미 존재하는 artifacts[] | 요청한 모든 파일의 exact identity와 완전한 독립 관찰 |
| scanArtifacts | scanId / artifacts[]와 literal patterns[] | 각 파일 전체 bytes/digest/발견 위치 목록 |
| verifyCoverage | registryHash, catalogHash / inputSnapshotKind와 그 입력 path | 실제 registry/catalog/assertion/artifact 연결 report. 아래 "verifyCoverage 입력 snapshot" |

생성 작업의 `generatedOutputs`는 최소 하나다. 보존 bundle 구성, 복원 수량·
계보·현재 의무·원문 hash·definition v1 판정·인가, schema FK/수량 제약·
outbox/CQN·허용 custom constraint 차이와 실제 대상 compiler version 등은
case assertion이 actual artifact/raw rows를 추가로 검사한다. descriptor의
`completeness`나 operation identity만으로 내용 oracle PASS를 주장하지
않는다. 이 validator는 host integrity와 연결 계약이며 업무 구현이 아니다.

cutover stage는 `WRITE_FREEZE`, `FINAL_SNAPSHOT`, `RECONCILE`, `APPLY_VERSION`,
`SMOKE_AUTH_RESUME`, `OPEN_WRITES`, `ROLLBACK_BEFORE_OPEN`,
`STOP_AND_RECONCILE_AFTER_OPEN`, `FORWARD_REPAIR`로 제한한다. 하나의 ACK로
전환 전체 완료를 선언하지 않는다. 개방 후 쓰기·외부효과를 old commit
복구로 없앴다고 표시하지 않는다.

start/stop/restart에는 `processObservation`을 둔다. `requestedProcessId`,
`before`, `after`, `terminalStatus`, `completedAt`, `artifactRef`가 필수다.
before/after는 processId, 실제 instanceId(string 또는 미존재 null),
status(RUNNING/STOPPED/NOT_PRESENT), observedAt을 기록한다. RUNNING은
non-null instanceId가 필요하다. before는 command 시작 전, after는
terminal 완료 후 command 종료까지 관찰한다. 성공 start/restart는
RUNNING, 성공 stop은 STOPPED/NOT_PRESENT이고 성공 restart의 새 instance는
이전 instance와 달라야 한다. 실패 terminal도 실제 상태를 보존한다.

artifactRef의 실제 JSON은 evidenceClass/scope/operation/processObservation을
exact 반환한다. 해당 파일은 generatedOutputs, extractor input hash와
StepResult artifactRefs에 연결한다. 새 business 상태를 fake worker로 만들지
않고 lifecycle 실제 terminal과 업무 종료 oracle를 따로 검사한다.

## 생성 output의 strict 후속 검사

생성 작업 뒤 actual output 배열을 후속 request에서 참조한다. 사전
fixture의 예상 hash를 실제 생성 hash로 바꾸어 주장하지 않는다.

```json
{
  "type": "process",
  "operation": "inspectArtifacts",
  "parameters": {
    "artifacts": {
      "$result": {
        "actionId": "actual-backup",
        "pointer": "/data/hostObservation/generatedOutputs"
      }
    }
  }
}
```

별도 case scope를 parameters에 넣을 수 있다. `$result` 치환은 실제 실행·
완전한 source·non-null 값이 있을 때만 가능하다. 위 request를 실행한
adapter는 inspectionId를 독립 관찰하고 실제 descriptor 목록을 반환한다.
`inspectArtifacts`는 배열 일부 누락·unrequested 파일·잘못된 bytes/hash/
scope·부분 completeness·가짜 count0을 모두 거부한다. 설치/백업 명령의
ACK만으로 후속 inspection을 생략하지 않는다.

## literal scan과 양성 sentinel

```json
{
  "artifacts": [{
    "path": "verification/evidence/redacted-output.json",
    "sha256": "<실제 저장 파일 SHA-256>",
    "sizeBytes": 123,
    "scope": {"environmentId": "isolated-ontology-v2"}
  }],
  "patterns": [{"id": "synthetic-marker", "literal": "VIRTUAL_MARKER"}]
}
```

pattern은 id/literal만 허용한다. regex/shell/SQL DSL은 없다. `reads[]`의
각 row는 exact `artifact`, `bytesRead`, `digest`, `findings[]`를 가진다.
finding은 `patternId`, zero-based UTF-8 `byteOffset`, `lengthBytes`다.
중첩/반복 발견도 모두 기록한다. 순서는 달라도 set은 정확히 같아야
하며 중복·누락은 실패다. 요청 파일별 전체 bytes를 harness가 읽어 독립
계산한다. match0도 실제 전체 bytes/hash/파일 scope가 있어야 한다.

selftest의 `sentinel.txt`에는 가상 marker 두 개가 있다. 발견0·틀린 위치·
누락 파일·읽은 bytes0·다른 digest mutant가 거부되는지 실행한다. 제품의
`leakCount=0` Boolean을 oracle로 쓰지 않는다. 실제 secret을 양성 fixture로
만들거나 repository에 저장하지 않는다. literal scan은 지정한 pattern의
검출 계약이며 모든 종류의 secret 부재를 자동 증명하지 않는다.

## autonomous scheduler terminal

tickScheduler/sweepDue의 `operationEvidence.submissionStatus`는 실제 독립
rows에 기록된 `SUBMITTED` 또는 `NO_TASK`다. SUBMITTED에는 실제 `taskId`,
`invocationHandle`, `submittedAt`을 모두 둔다. submittedAt은 해당 host
command의 시작·종료 사이여야 한다. NO_TASK에는 세 field를 넣지 않는다.
미관찰 task를 NO_TASK로 치환하지 않고 원행·scope·실제 command evidence를
같이 검사한다. 제출 ACK에는 runtimeTask terminal을 넣지 않는다.

한 scoped control의 typed submission identity를 후속 `$result`로 읽는다.
단순 task 배열의 첫 row를 추측하지 않는다. 실제 task를 제출했다면
CaseRunner는 동일 scheduler/taskId/invocationHandle의 terminal을 요구한다.
scope·environment·evidenceClass도 submission과 같으며 완료 시각은 제출
시각 이후다. terminal이 없으면 제품 case는 NOT_RUN, 다른 identity나
환경의 terminal이면 계약 실패다. NO_TASK만 관찰한 control은 terminal을
생성하지 않는다. 업무 생성0과 scheduler technical task 부재는 별개다.

`awaitRuntimeTask` request는 이전 actual scheduler 관찰 결과의 `taskId`
또는 `invocationHandle`을 strict `$result`로 받는다. 두 값을 request에
전달하면 둘 다 exact 대조한다. 실제 `runtimeTask`에는 두 identity 모두,
`origin=autonomousScheduler`, `terminalStatus=SUCCEEDED|FAILED|CANCELLED`,
`completedAt`, `snapshot={id,capturedAt,artifactRef}`를 반환한다.

참조한 실제 JSON snapshot에는 taskId/invocationHandle/origin/terminalStatus/
completedAt/schedulerId/snapshotId/capturedAt이 있으며 관찰값과 exact 같다.
snapshot은 terminal completion 이후, await command completion까지 관찰된다.
해당 snapshot 파일은 독립 extractor input hash와 StepResult artifactRefs에
연결한다. direct `resumeWork`나 fake worker 실행은 이 경로의 대체물이
아니다. 실제 scheduler claim·업무 효과·책임 종료는 독립 DB assertion으로
추가 검증한다. task terminal FAILED는 실제 종료 관찰이며 업무 성공이
아니다. command exit0·제출 ACK·알림 전달만으로 terminal을 선언하지 않는다.

## 자연 tick 수동 관찰(OBSERVE_NEXT_NATURAL_TICK)

T26의 `*-autonomous-loop` subcase는 harness가 tick이나 sweep을 일으키지
않고 scheduler loop가 스스로 due 의무·대기·미연결 intake를 찾는지 본다.
tickScheduler/sweepDue 요청에 아래 세 parameter를 두면 수동 관찰이다.

| parameter | 값 | 의미 |
|---|---|---|
| `trigger` | `OBSERVE_NEXT_NATURAL_TICK`만 | harness는 아무것도 실행하지 않고 다음 자연 tick을 관찰만 한다 |
| `observationWindowSeconds` | 정수 1–30 | 관찰 창. 계획 §10 개발/CI 관찰 제한30초 |
| `triggeredBy` | `SCHEDULER_LOOP` | 기대하는 제출 주체 |

`HostObservationValidator`(`naturalTick`)가 다음을 요구한다.

- 독립 extractor가 읽은 scheduler 원행의 `operationEvidence`에 같은
  `trigger`·`observationWindowSeconds`·`triggeredBy`가 있다. 값은 요청과
  같아야 하며 원행에서 나온다(StepResult 요약이 아니다). `triggeredBy`는
  case assertion(`autonomous-trigger-loop`)이 다시 읽는다.
- host `command`는 관찰자(watcher)의 실제 argv/구간이다. 구간은
  `observationWindowSeconds`를 넘지 않고, SUBMITTED의 `submittedAt`은
  command 시작부터 창 안에 있다. NO_TASK는 창 안에 제출이 없었다는 실제
  관찰이며 자율 발견 실패로 case assertion에서 드러난다.
- argv에 `tickScheduler`·`sweepDue`·`resumeWork`·`fakeWorker` 호출을
  넣지 않는다. 이 검사는 명백한 trigger를 막는 guard다. 자율성의 증거는
  독립 원행의 제출 주체와 case의 DB attempt 원행(`autonomous-attempt-source`)이다.
- extractor는 independent·readOnly다.
- 수동 parameter가 없는 harness tick/sweep은 원행이 `trigger=
  OBSERVE_NEXT_NATURAL_TICK`이나 `triggeredBy=SCHEDULER_LOOP`를 주장하면
  거부한다. harness가 일으킨 tick을 자연 tick으로 바꿔 부를 수 없다.

schema(`acceptance-host-observation.schema.json`)는 tickScheduler/sweepDue
`operationEvidence`에 세 field를 허용하고 `trigger`가 있으면 나머지 둘을
요구한다. 운영 process가 sweeper loop를 실제로 실행하는지의 deployment
profile 검사는 아직 없다(T26 case·deployment 소유자 후속).

## verifyCoverage 입력 snapshot

T25 verifyCoverage는 `inputSnapshotKind`로 고정한 한 입력만 읽는다.
준비 보고를 runtime 증거처럼, 또는 runtime manifest를 준비 보고처럼 읽어
기대값이 서로 모순되거나 준비 사실이 실행 PASS를 대신하지 않게 한다.

| inputSnapshotKind | 반드시 있는 입력 | 있으면 안 되는 parameter |
|---|---|---|
| `PREPARATION` | `preparationReportPath`(`target/evidence/prepare.json`) | manifestPath, modelManifestPath, currentExecution |
| `REQUIRED_PATH_RUNTIME_EVIDENCE` | `manifestPath`(실제 runtime manifest), `currentExecution={caseId}` | preparationReportPath, modelManifestPath |
| `MODEL_BINDING_PREPARATION` | `modelManifestPath`(`target/evidence/model-binding-preparation.json`) | manifestPath, preparationReportPath, currentExecution |
| `APPROVED_MODEL_EXECUTION_EVIDENCE` | `manifestPath`(승인된 실모델 실행의 runtime manifest) | preparationReportPath, modelManifestPath, currentExecution |

모든 종류에 `registryPath`·`catalogPath`가 필요하다. validator(`coverageSnapshot`)
검사는 다음과 같다.

- 입력 snapshot 파일, registry, catalog가 `inputArtifacts`에 path/hash/bytes로
  묶여 있다(현재 파일 bytes와 대조). `operationEvidence.registryHash`·
  `catalogHash`는 그 descriptor의 sha256과 같다.
- 결과는 구조화된 `extractor.rawRows`다. `rawRows.input`은
  `{snapshotKind, path, sha256}`이고 요청 종류·묶인 파일과 같다.
  REQUIRED_PATH_RUNTIME_EVIDENCE면 `input.currentExecution`도 요청과 같다.
- `mutation`이 `none`이 아니면 `rawRows.mutatedInput.mutation`이 요청과 같다.
- `rawRows.assertionLinks[]`와 `rawRows.namedObservations[].assertionLinks[]`의
  status는 PASS·FAIL·NOT_RUN·CURRENT_EXECUTION이다. CURRENT_EXECUTION은
  REQUIRED_PATH_RUNTIME_EVIDENCE에서 currentExecution case의 link에만, 그리고
  그 case의 link에는 반드시 쓴다. 현재 실행 중인 T25 결과를 같은 실행의
  선행 PASS로 요구하는 순환을 막는다.

T25가 읽는 rawRows 출력은 다음과 같다. 값의 의미와 기대값은 T25 case가
고정하고, 이 표는 adapter가 반환할 이름의 계약이다.

| rawRows 경로 | 내용 |
|---|---|
| `input` | 위 snapshotKind·path·sha256(·currentExecution) |
| `requiredCases`, `requirementIds` | 읽은 registry/catalog의 41 case, D01–D26 |
| `catalogOracles[]`, `catalogObservations[]` | oracleId / oracleId·name·type·scope·operator 원문 tuple |
| `namedObservations[]` | catalog 선언순 observation과 그 `assertionLinks[]`(caseId·subcaseId·assertionId·profile·status·evidenceRefs) |
| `assertionLinks[]` | 모든 named observation link의 평면 목록 |
| `runtimeArtifacts[]` | caseId·subcaseId·profile·assertionId·path·sha256·sizeBytes·scope·fixtureHash·codeCommit·command·expected·observed·exitCode·status |
| `gate` | preparationStatus·runtimeStatus·requiredPathStatus·modelStatus·regulatoryStatus·usageStatus·uatComplete·gateComplete·semanticOracleEquivalence |
| `validation` | `status`, `issues[].code`(MISSING_ORACLE·MISSING_OBSERVATION·MISSING_RUNTIME_ARTIFACT·NON_RUNTIME_PASS·SKIPPED_REQUIRED_CASE·MANDATORY_PATH_WAIVED·PARTIAL_EXECUTION_PASS·VERIFIED_VIOLATION 등) |
| `baseline` | mutation 전 사본의 `validation`·`gate`·`runtimeArtifacts` |
| `mutatedInput` | 변조한 caseId·mutation |
| `regulatoryReviews[]`, `modelAuthorizations[]` | 법규 검토 출처·관할·적용일·검토자, 실모델 승인·버전 |
| `records[]`, `wrappers[]`, `evidenceClasses`, `dependencies`, `semanticReview` | wrapper·증거 class·profile 선행·QA review |
| `corpus`, `acceptance`, `attempts[]`, `modelCaseRuns[]`, `metrics`, `usage`, `authorization`, `uatPasses`, `invalidUnprovidedMetrics`, `versions` | 모델 corpus·R8 수용값·시도·지표·비용 |

PREPARATION의 `gate.semanticOracleEquivalence`는 준비 보고의 값
`REQUIRES_CASE_REVIEW`이고, assembler runtime manifest의 값은
`REQUIRES_CASE_AND_RUNTIME_REVIEW`다. 두 입력의 성질이 달라서 맞추지 않는다.

## 실행 증거와 미실행 범위

`./verify harness`는 schema·validator 고정 표본과 mutant를 실제 실행한다.
`verification/harness/evidence/host-observation/`에 command/version/exit와
JUnit 결과를 남긴다. selftest의 argv와 JSON은 `CAPTURED_SELFTEST`라고
명시된 고정 관찰 입력이며 executable fake process adapter가 아니다.
제품 실행 경로는 이 표본을 제품 증거로 받지 않는다. 같은 표본은 PRODUCT
정책의 `CaseRunner`와 `requireActualHost=true` 검증에서 거부된다
(`StepTwoReReviewRegressionTest`).

실제 inspect/scan/schema/restore/scheduler/client/profile adapter는 없다.
해당 `./verify` 제품 profile은 계속 `NOT_RUN`이며 비용 승인 없이 모델이나
BTP를 실행하지 않는다. 표본·논리 review·준비 PASS를 실제 host/model/규제/
배포 인수로 전환하지 않는다. 실제 host 연결 뒤 case의 DB/업무 oracle,
actual artifacts와 같은 baseline의 결합 checks가 통과해야 제품 gate가 닫힌다.
