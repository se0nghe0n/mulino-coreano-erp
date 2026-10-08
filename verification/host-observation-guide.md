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
| enumerateWriteSurface | environmentId, enumerationId, allowlistSha256 / surfaces·probeClasses·allowlistRef·actorRef·targetPolicy | 실행 중 시스템이 노출한 쓰기 면 열거와 probe 결과. 아래 "enumerateWriteSurface 쓰기 면 열거" |

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
command의 시작·종료 사이여야 한다. 수동 watcher는 시작 대신 요청의
`observeFrom`(아래 "자연 tick 수동 관찰")부터 잰다. NO_TASK에는 세 field를 넣지 않는다.
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

이 terminal snapshot에서 독립 DB를 읽는 `observe`는 snapshotRef를
`{"$result":{"actionId":"<await action>","pointer":"/data/hostObservation/runtimeTask/snapshot/id"}}`로
둔다(T26 `*-expiry-no-event`·`lot-expiry-autonomous-loop`의 `sweep-db`). harness는
이 값을 API projection revision으로 다루지 않는다. observer에는
`snapshotRef=RUNTIME_TASK_SNAPSHOT`과 task identity만 보내고, observer가 보고한
`snapshot.runtimeTaskSnapshot`(snapshotId·artifactRef·sha256)을 await 결과와
artifact bytes로 대조한다. 규칙은 harness 가이드 snapshot 절에 있다.

## 자연 tick 수동 관찰(OBSERVE_NEXT_NATURAL_TICK)

T26의 `*-autonomous-loop` subcase는 harness가 tick이나 sweep을 일으키지
않고 scheduler loop가 스스로 due 의무·대기·미연결 intake를 찾는지 본다.
tickScheduler/sweepDue 요청에 아래 세 parameter를 두면 수동 관찰이다.

| parameter | 값 | 의미 |
|---|---|---|
| `trigger` | `OBSERVE_NEXT_NATURAL_TICK`만 | harness는 아무것도 실행하지 않고 다음 자연 tick을 관찰만 한다 |
| `observationWindowSeconds` | 정수 1–30 | 관찰 창. 계획 §10 개발/CI 관찰 제한30초 |
| `triggeredBy` | `SCHEDULER_LOOP` | 기대하는 제출 주체 |
| `observeFrom` | ISO-8601 instant, harness가 해석 | 관찰 창의 시작. case는 쓰지 않는다 |
| `naturalTickSeconds` | 정수 1–`observationWindowSeconds`, harness가 해석 | fixture `runtimeProfile.tickSeconds`. NO_TASK 최소 관찰 길이. case는 쓰지 않는다 |

세 값은 watcher 설정이다. scheduler 증거가 아니다. 이전 계약은
`operationEvidence`에 같은 세 값을 되돌려 달라고 했다. 요청을 그대로
복사하는 observer도 통과했으므로 증거가 되지 못했다. 지금은
`operationEvidence`에 세 key가 있으면 schema와 validator가 모두 거부한다.

scheduler가 실제로 한 일은 extractor `rawRows.schedulerSubmissions[]`로
돌려준다. scheduler가 스스로 남긴 제출 기록(제출 표·log)을 독립 extractor가
읽은 행이다.

| rawRows.schedulerSubmissions[] field | 내용 |
|---|---|
| `schedulerId` | 요청 schedulerId와 같다 |
| `tickId`(tickScheduler) / `sweepId`(sweepDue) | scheduler가 붙인 실행 단위 ID |
| `taskId`, `invocationHandle` | 제출한 technical task identity |
| `submittedAt` | scheduler가 기록한 제출 시각 |
| `submittedBy` | 제출을 시작한 주체. 자연 tick이면 `SCHEDULER_LOOP` |

`HostObservationValidator`(`naturalTick`)가 다음을 요구한다.

- 관찰 창은 `[observeFrom, observeFrom+observationWindowSeconds]`다.
  `observeFrom`은 `CaseRunner.controlRequest`가 watcher 요청 parameter에
  넣어 host adapter에 보낸다(step2r round 6). 자율 loop group의 watcher면
  group의 어떤 branch도 제출하기 직전에 잡은 harness 시각(`parallel` 결과의
  `data.observationBoundaryAt`과 같은 값)이고, group 밖 watcher(T26
  `lot-expiry-autonomous-loop`의 `repeat-sweep`)면 그 watcher를 dispatch하기
  직전의 harness 시각이다. case가 `observeFrom`을 직접 쓰면
  `runtimeProfileProblems`가 준비 실패로 낸다. host adapter의 extractor는
  scheduler의 지속 제출 기록에서 이 창 안의 행만 읽는다. 그래서 늦게 뜬
  watcher thread도 경계 뒤 제출을 잃지 않고, group 밖 반복 watcher가 앞선
  sweep의 행을 자기 관찰로 보고하지 않는다. validator는 `observeFrom`이
  없으면 거부하고, 경계 값이 있으면 같아야 한다. watcher command는
  `observeFrom`보다 먼저 시작할 수 없고 `observeFrom+observationWindowSeconds`
  뒤에 끝날 수 없다.
- `rawRows.schedulerSubmissions`가 배열로 있다. 행마다 위 field가 비어
  있지 않고 schedulerId가 요청과 같으며, submittedAt이 관찰 경계부터
  `observationWindowSeconds` 안이다.
- SUBMITTED면 `operationEvidence`의 tickId 또는 sweepId·taskId·
  invocationHandle·submittedAt이 창 안 가장 이른 행과 같다. NO_TASK면 창
  안에 행이 없다. NO_TASK는 자율 발견 실패를 그대로 드러내는 관찰이며
  case assertion에서 실패한다.
- NO_TASK는 watcher가 다음 자연 tick을 실제로 지켜본 뒤에만 증거다
  (step2r round 7). `CaseRunner.controlRequest`가 fixture
  `runtimeProfile.tickSeconds`를 `naturalTickSeconds`로 요청에 넣는다.
  NO_TASK면 watcher command의 `completedAt`이 `observeFrom+naturalTickSeconds`
  이후여야 하고, `naturalTickSeconds`가 없거나 1–`observationWindowSeconds`
  밖이면 거부한다. 그래서 tick 1초인 loop에서 0.5초 만에 돌아온 watcher의
  "제출 없음"으로, 이미 처리한 업무를 다음 tick에 다시 제출하는 sweeper가
  T26 `repeat-no-due-task`를 통과하지 못한다. 창 전체(30초)를 요구하지
  않는 것은 창 끝을 넘을 수 없다는 위 상한과 동시에 만족할 수 없기
  때문이다. 수동 관찰 subcase의 fixture는 `tickSeconds`를 1–창 길이의
  정수로 둬야 하고, case가 `naturalTickSeconds`를 직접 쓰면
  `runtimeProfileProblems`가 준비 실패로 낸다.
- host `command`는 watcher의 실제 argv/구간이다. 구간은
  `observationWindowSeconds`를 넘지 않는다. SUBMITTED의 submittedAt은
  `observeFrom` 이후, watcher command 종료 이전이다.
- argv에 `tickScheduler`·`sweepDue`·`resumeWork`·`fakeWorker` 호출을
  넣지 않는다. 명백한 trigger를 막는 guard다.
- extractor는 independent·readOnly다.
- 수동 parameter가 없는 harness tick/sweep에서 schedulerSubmissions 행이
  `submittedBy=SCHEDULER_LOOP`를 주장하면 거부한다. harness가 일으킨
  tick을 자연 tick으로 바꿔 부를 수 없다.

T26 `autonomous-trigger-loop` assertion은 `operationEvidence.taskId`로
고른 schedulerSubmissions 행의 `submittedBy`가 정확히 `{SCHEDULER_LOOP}`인지
본다. 요청 parameter의 복사로는 이 값을 만들 수 없다. 자율성의 다른 증거는
case의 DB attempt 원행(`autonomous-attempt-source`)이다. 운영 process가
sweeper loop를 실제로 실행하는지의 deployment profile 검사는 아직 없다
(T26 case·deployment 소유자 후속).

## runtimeProfile과 자율 loop 패턴

fixture `baseline.runtimeProfile`(`contracts/acceptance-fixture.schema.json`)은
scheduler·sweeper loop가 tick을 어떻게 일으키는지 정한다. 정의된 조합은
둘뿐이다.

| controlledTicks | pausedUntilTickControl | 의미 | 쓰는 subcase |
|---|---|---|---|
| `true` | `true` | loop는 스스로 아무것도 제출하지 않는다. harness tickScheduler/sweepDue 요청 하나가 정확히 그 tick 하나를 일으킨다 | harness tick subcase(T26 rediscovery·expiry·guard, V5) |
| `false` | `false` | loop가 자기 `tickSeconds`로 돈다. harness는 수동 watcher(OBSERVE_NEXT_NATURAL_TICK)로 관찰만 한다 | T26 `*-autonomous-loop` |

runtimeProfile이 있으면 두 flag가 모두 필요하고, 섞인 조합(true/false)은
schema가 거부한다. runtimeProfile이 없는 fixture는 harness tick fixture로
본다. `tickSeconds`·`claimTTLSeconds`·`heartbeatSeconds`는 1 이상의 정수,
`observationLimitSeconds`는 1–30이다(계획 §10).

`./verify prepare`(`ContractValidator.runtimeProfileProblems`)는 subcase마다
다음을 검사한다.

- 한 subcase에 harness tick과 수동 watcher가 섞이면 거부한다. 한 profile로
  두 제출의 주체를 가를 수 없다.
- harness tick은 controlledTicks=true·pausedUntilTickControl=true fixture에서만,
  수동 watcher는 둘 다 false인 명시 runtimeProfile에서만 쓴다.
- 자율 loop 패턴. 첫 수동 watcher는 top-level `parallel` action의 branch 0
  첫 action이다. 나머지 branch는 각각 process `start` 하나만 한다. 한
  branch에 start를 여럿 두면 순서대로 실행되어 관찰 창을 기동 시간으로
  써 버린다. `restart`는 쓰지 않는다. stop과 start 사이가 없어서 watcher보다
  먼저 loop가 제출할 수 있기 때문이다.
- group이 시작하는 process(loop process)는 group 전까지 한 번도 실행 중이면
  안 된다. installFixture 뒤 group 앞의 모든 action(fault·seed 명령·조회·
  관찰·clock 전진)에서 loop process가 멈춰 있어야 한다. 다른 process의
  start/stop만 예외다. 자율 fixture의 loop는 1초마다 스스로 돌므로, 장애나
  seed 단계에 떠 있으면 미연결 intake 같은 seed 상태를 before-db나 watcher
  전에 처리해 버린다(step2r round 5, opus[1]). process는 case가 start하기
  전에는 실행 중이 아니다. 실행 중이던 process를 start하면 lifecycle 검사가
  거부한다.

api·worker처럼 스스로 제출하지 않는 process는 group 밖에서 순서대로
시작한다. group 안에서는 loop process 기동과 첫 tick만 관찰 창을 쓴다.
watcher branch를 먼저 두고, 관찰 창은 group 직전의 관찰 경계(watcher 요청의
`observeFrom`)에서 시작한다.
`parallel`은 barrier가 아니지만 창의 시작이 watcher thread의 기동 시각이
아니라 경계이고 extractor가 지속 제출 기록을 읽으므로, watcher가 늦게 떠도
경계 뒤의 첫 제출을 놓치지 않는다. 남은 지연은 watcher가 창 끝(경계+30초)
뒤까지 늦는 경우뿐이며 그때는 NO_TASK·창 밖 행으로 fail-closed다. 30초
기준은 validator와 같은 관찰 경계(`autonomous-within-30s`의 baseline
`<group>/data/observationBoundaryAt`)다. 제출이 loop process start command의
`/data/hostObservation/command/startedAt`보다 앞서지 않는지는
`autonomous-after-loop-start`가 따로 본다.

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
- PREPARATION이면 `rawRows.input`에 아래 네 field가 있다(`preparationInput`).
  준비 보고가 어느 commit의 clean tree에서 나왔는지, verifier가 어느
  checkout에서 읽었는지를 같이 남긴다.

  | rawRows.input field | 형식 | 원천과 validator 검사 |
  |---|---|---|
  | `codeCommit` | 40 또는 64자리 소문자 hex | 묶인 준비 보고 bytes의 `codeCommit`과 같다 |
  | `workingTreeDirty` | boolean | 묶인 준비 보고 bytes의 `workingTreeDirty`와 같다 |
  | `checkoutCommit` | 40 또는 64자리 소문자 hex | verifier가 실행된 checkout의 HEAD. 제품 실행에서는 harness git HEAD와 같다 |
  | `checkoutDirty` | boolean | verifier가 실행된 checkout의 변경 여부. 제품 실행에서는 harness의 working tree 상태와 같다 |

  validator는 형식과 준비 보고와의 일치를 강제한다. 제품 실행
  (`requireActualHost=true`)에서는 `checkoutCommit`·`checkoutDirty`도 harness가
  같은 저장소에서 직접 읽은 `git rev-parse HEAD`·`git status --porcelain`
  결과와 같아야 한다(`checkoutMatchesHarness`). verifier가 `codeCommit`을
  `checkoutCommit`에 복사하고 clean이라고 적는 것으로는 통과하지 못한다.
  이 보고가 현재 clean checkout의 것인지(`workingTreeDirty=false`,
  `checkoutDirty=false`, `codeCommit`=`checkoutCommit`)는 T25 PREPARATION
  subcase의 assertion이 판정한다. 그래서 낡거나 dirty인 준비 보고는 형식
  오류가 아니라 case FAIL로 드러난다.
- `rawRows.assertionLinks[]`와 `rawRows.namedObservations[].assertionLinks[]`의
  status는 PASS·FAIL·NOT_RUN·CURRENT_EXECUTION이다. CURRENT_EXECUTION은
  REQUIRED_PATH_RUNTIME_EVIDENCE에서 currentExecution case의 link에만, 그리고
  그 case의 link에는 반드시 쓴다. 현재 실행 중인 T25 결과를 같은 실행의
  선행 PASS로 요구하는 순환을 막는다.

T25가 읽는 rawRows 출력은 다음과 같다. 값의 의미와 기대값은 T25 case가
고정하고, 이 표는 adapter가 반환할 이름의 계약이다.

| rawRows 경로 | 내용 |
|---|---|
| `input` | 위 snapshotKind·path·sha256(·currentExecution). PREPARATION이면 아래 commit·clean 4 field도 |
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

## enumerateWriteSurface 쓰기 면 열거

V4 `exposed-write-surface`(계획 §4.2·§13.2 V4 "실제 노출된 direct/nested/
batch/projection 경로 모두 검사")의 host 조작이다. 고정 경로 목록이 아니라
host command가 실행 중인 시스템의 쓰기 면을 직접 읽고, 열거한 쓰기 가능
항목에 READ grant 주체로 probe한다. 독립 read-only extractor가 결과를
구조화한다. LOCAL profile 전용이다.

요청 parameters는 `environmentId`, `enumerationId`, `actorRef`,
`surfaces`(ODATA_METADATA·MCP_SERVER_DISCOVER·MCP_TOOLS_LIST·
WORKER_HANDLER_REGISTRY·MANAGEMENT_ENDPOINTS 중 비지 않은 집합),
`probeClasses`(DIRECT_CREATE·DIRECT_UPDATE·DIRECT_DELETE·DEEP_INSERT·UPSERT·
BATCH_CHANGESET·DRAFT_ACTIVATE·NESTED_NAVIGATION_CREATE/UPDATE/DELETE·
BOUND_ACTION·UNBOUND_ACTION·MCP_TOOL_CALL·WORKER_HANDLER_SUBMIT·
MANAGEMENT_ENDPOINT_WRITE 중 비지 않은 집합), `allowlistRef`,
`allowlistSha256`, `targetPolicy=SYNTHETIC_FIXTURE_ENTITIES_ONLY`다.
`operationEvidence`는 `environmentId`·`enumerationId`·`allowlistSha256`이고
요청과 같아야 한다. 결과는 `extractor.rawRows`의 네 표다.

| rawRows 경로 | 행 |
|---|---|
| `surfaces[]` | surface, sourceRef(읽은 원문 artifact), itemCount, sha256 |
| `surfaceItems[]` | surface, itemId, kind, writeCapable, capabilityId(없으면 null), allowlisted |
| `probes[]` | probeId, surface, target(itemId), probeClass, outcome, committed, transcriptRef |
| `probeCoverage[]` | probeClass, applicableTargets, probedTargets, complete |

probe `outcome`은 `contracts/domain-vocabulary.json`의 명령 outcome과
`NOT_EXPOSED`(경로·method 없음), `UNKNOWN`(시간초과·응답 유실 등 결과 미확인)
이다. UNKNOWN을 거부로 세지 않는다. validator(`writeSurface`)는 다음을
검사한다. extractor 값을 그대로 믿지 않고 harness가 다시 계산하는 부분이 있다.

- `allowlistRef`의 저장소 bytes가 `allowlistSha256`과 같고 그 artifact가
  `inputArtifacts`에 같은 hash로 묶여 있다.
- `surfaces`는 요청 집합과 정확히 같고 각 `sourceRef`는 같은 sha256의
  `observedArtifacts`다. `itemCount`는 그 surface의 `surfaceItems` 수와 같다.
- `surfaceItems`의 (surface, itemId)는 중복이 없고, `allowlisted`는
  harness가 allowlist bytes에서 다시 계산한 값(`capabilityId`가 allowlist의
  capability id인가)과 같다. extractor가 목록 밖 쓰기 항목을 allowlisted로
  적으면 실패한다.
- `writeCapable=true`인 항목은 모두 probe 대상이다. probe는 요청한
  probeClass·열거된 항목만 쓰고 transcript가 StepResult artifact로 연결된다.
- 적용 정책(아래 표)에 따라 항목의 `kind`가 정하는 probe class 중 요청한
  class마다 그 항목의 probe 행이 있어야 한다. extractor의 `writeCapable`과
  무관하다. `@readonly` entity set도 쓰기 거부를 보여야 하기 때문이다.
  QUERY capability를 부르는 tool·action만 아래 QUERY 면제로 빠진다.
  정책에 없는 kind, 그 kind가 나올 수 없는 surface의 항목은 거부한다.
- `probeCoverage`는 요청 probeClass와 정확히 같다. `applicableTargets`는
  harness가 정책으로 다시 센 적용 항목 수와 같아야 한다. extractor가 적게
  적으면(예: 적용 항목이 있는데 0/0 complete=true) 거부한다. `probedTargets`는
  그 class probe 행의 서로 다른 target 수, `complete`는 적용 항목이 모두
  probe되었는지다.

| kind | 나올 수 있는 surface | 적용 probe class |
|---|---|---|
| `ENTITY_SET` | ODATA_METADATA | DIRECT_CREATE·DIRECT_UPDATE·DIRECT_DELETE·DEEP_INSERT·UPSERT·BATCH_CHANGESET·DRAFT_ACTIVATE·NESTED_NAVIGATION_CREATE/UPDATE/DELETE |
| `BOUND_ACTION` | ODATA_METADATA | BOUND_ACTION·BATCH_CHANGESET |
| `UNBOUND_ACTION` | ODATA_METADATA | UNBOUND_ACTION·BATCH_CHANGESET |
| `FUNCTION` | ODATA_METADATA | 없음(OData function은 정의상 읽기다. writeCapable이면 위 규칙대로 probe한다) |
| `TOOL` | MCP_SERVER_DISCOVER·MCP_TOOLS_LIST | MCP_TOOL_CALL |
| `WORKER_HANDLER` | WORKER_HANDLER_REGISTRY | WORKER_HANDLER_SUBMIT |
| `MANAGEMENT_ENDPOINT` | MANAGEMENT_ENDPOINTS | MANAGEMENT_ENDPOINT_WRITE |

QUERY 면제(step2r round 6). `TOOL`·`BOUND_ACTION`·`UNBOUND_ACTION` 항목이
하나의 QUERY capability를 부르면 FUNCTION처럼 적용 probe class가 없다.
READ 주체의 getInventory 호출은 성공한 읽기이고 그 결과를 정직하게 담을
probe outcome이 없기 때문이다. 면제는 harness가 계산한다. 조건은 셋이다.
`allowlistRef`의 hash로 묶인 bytes(`contracts/acceptance-capabilities.json`)에서
그 `capabilityId`의 `kind`가 `QUERY`이고, `writeCapable=false`이며, 항목
이름(`itemId`)이 그 capability id이거나 `.`·`/` 뒤에 그 id로 끝난다. 그래서
다른 이름의 명령 항목이 QUERY id를 빌려 면제받을 수 없다. COMMAND·RECORD
항목, `capabilityId`가 없거나 목록 밖인 항목(예: 범용 `query`·`command`
dispatcher action), `writeCapable=true` 항목은 표의 probe를 모두 받는다.
범용 dispatcher의 probe는 쓰기 형태의 요청을 보내 거부(REJECTED 등)를
관찰한다. 면제 항목도 `applicableTargets` 재계산에서 빠질 뿐 열거·hash·
allowlist 대조는 그대로다.

서비스가 제공하지 않는 경로(navigation이 없는 entity의 nested 쓰기, draft가
아닌 entity의 activation)도 probe하고 `NOT_EXPOSED`로 기록한다. 존재하지 않는
경로의 응답만으로 거부를 증명하지는 않으며 전후 DB 효과0은 V4 assertion이
따로 본다. 이 정책은 harness와 같은 신뢰 수준의 extractor가 항목 `kind`를
정직하게 적는다고 가정한다. 열거 자체의 누락은 surface 원문 artifact의
`itemCount`·hash 대조와 고정 `batch-<family>` subcase가 보완한다.

목록 밖 쓰기 면 0, probe commit 0, APPLIED 0, UNKNOWN 0, probe class별
완전성은 V4 case assertion이 판정한다. 이 validator는 열거 결과가 연결·
재계산 계약을 지키는지만 본다. 실제 열거 adapter는 아직 없어 이 subcase는
`NOT_IMPLEMENTED`(NOT_RUN)다. harness 표본(`enumerateWriteSurface-rows.json`)은
`CAPTURED_SELFTEST`이며 제품 증거가 아니다.

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
