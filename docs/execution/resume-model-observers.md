# 독립 model observation 구현 인계

`2c0e84b3`에서 `rawRows`/physical cell 계약 충돌과 API revision/MVCC
혼동을 수정했다. 물리 DB acquisition과 제한된 원문/host extractor를
추가했다. **전체 model-binding 인수와 전체 semantic mapper는 미완료다.**
실제 모델 호출은 0회다. native config/auth와 deterministic gate는 바꾸지
않았다. 이 문서의 계약 테스트 성공은 모델 의미 인수 성공이 아니다.

## 소유권과 기준선

- worktree: `/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-2c3a1876`
- 시작 commit: `2c0e84b3102586da941d2b5c6158b470ac695bee`
- 시작 branch: `t3/2c3a1876`, 시작 시 변경 파일은 없었다.
- `gpt-6.1-sol` 실행이며 `codex` catalog의 Ultrafast 지원을 확인했다.
  실행 tier는 coordinator가 선택한 값을 유지했다. 도중 전환하지 않았다.
- backend, fixture installer, process control, T01/V5/T26, case oracle,
  `NativeDeterministicGate`, native model 설정은 수정하지 않았다.
- 재위임, push, PR, 외부 메시지, 별도 API 과금/BTP 실행은 하지 않았다.

## 서로 다른 원행과 cell

공통 `contracts/acceptance-observation.schema.json`에는 선택 필드
`physicalCells`만 추가했다. 기존 `rawRows` object와 그 assertion은 그대로다.
모든 model-binding cell 소비자는 이제 `physicalCells` array를 읽는다.
공통 원행 schema와 `physical-columns.schema.json`을 각각 검증한다.

cell의 `rawRowPointer`는 실제 `/rawRows/<table>/<index>/<column>`을
가리킨다. 독립 validator가 value, row ID, column, 실제 query의 table,
unit, 존재하는 canonical physical scope를 대조한다. 중복 cell, 바뀐
value, 위조한 row ID/table/column/unit/scope는 거부한다. preflight 무변경
검사는 원행 object 전체와 cell array를 모두 비교한다. receipt replay와
수량 검증도 새 array 위치만 사용한다. 원행 object를 cell로 덮지 않는다.

수량 reducer에는 `sourceEvidence.<dataset>.<attribute>.complete=true`와
그 entry가 가리키는 원행 수만큼의 cell이 필요하다. mapping 자체가 없거나
원행은 있는데 cell이 빠졌으면 0으로 합산하지 않고 실패한다. 완전히
관찰한 빈 원행 scope만 0이 될 수 있다.

## DB acquisition과 남은 semantic 경계

`NativePhysicalCatalog`는 현재 CDS에서 확인한 109개 table과 column을
명시한다. inventory, 구매/운송/수령/품질/규제, 판매/반품/회수/정산,
Work/Goal/Assessment/의무, evidence, command audit, runtime/outbox를
같은 JDBC read-only `REPEATABLE_READ` transaction으로 읽는다. SQL의
조직 조건은 bind parameter다. `recordedAt` 또는 `createdAt`이 있으면
`knownAt` 조건도 bind한다. 없으면 현재 행만 읽는다는 한계를 유지한다.
없는 table/column은 예외이며 빈 dataset으로 대체하지 않는다.

각 query, parameter, 원행, 모든 column의 cell, 실제 MVCC token을
artifact에 보존한다. ID와 column 이름은 fixture 기대값에서 만들지 않는다.
`obligationObservation`은 실제 유효 assignment의 owner/status/kind,
nextAction/nextCheckAt, Work/scope/quantity와 원행 pointer를 보존한다.
`effectObservation`은 실제 table+ID와 command/operation/quantity 등의
존재하는 column만 보존한다. 다음 사실 때문에 이 둘은 여전히
`complete=false`인 진단이며 `data.effects=[]`를 제공하지 않는다.

1. `CommandRecords`에는 installation/turn이 없고 `CommandAudits.effectRefs`는
   `workId/activityId/obligationId/revision/commandId` 일부만 담는다.
   `CommandRepository.safeRefs`는 모든 실물/수령/배분/outbox의 불변 효과
   ID를 제공하지 않는다. 요청의 turn ID를 복사해서 귀속을 만들 수 없다.
2. allocation/outbox/일부 의무는 mutable current row다. 전후 차이만으로
   한 turn 안의 생성 후 삭제, 중간 변경, 여러 실제 효과를 복원할 수 없다.
3. corpus의 `A`, `B`, `O1`, `H2`, `OB1`은 물리 column 이름이 아니다.
   installation이 확인한 alias→UUID와 semantic selector가 있어야 한다.
   `inventory.onHand`, `assessment.arrived`, `delivery.compliance`,
   `currentWriteAuthorization` 등은 domain evaluator 결과다. 이를 raw
   column처럼 만들거나 observer가 별도 정책 엔진으로 복제하지 않았다.
4. receipt/recall의 실물 중복 제거는 canonical occurrence뿐 아니라 동일한
   실물 interval/root의 보존을 요구한다. 서로 다른 row ID를 곧바로 다른
   실물로 세지 않는다. 원행의 canonical ID/physicalScopeId를 보존하며,
   아직 없는 cross-table interval 증명을 만들어내지 않는다.
5. 109개 table은 전체 외부 write surface의 완전한 journal이 아니다.
   identity의 composite-key grant action/scope, 정의 발행, platform audit
   등을 포함한 효과 분류와 scoped completeness 증명은 별도 필요하다.

`semanticCoverage`는 현재 registry의 DB/effect semantic path 각각에
`UNKNOWN`과 원인을 기록한다. 원행이 0개라는 이유만으로 어떤 미완성
mapping도 0/PASS로 바꾸지 않는다. `NativeProductIntegration.observe`는
설치한 organization과 installation 둘 다 확인하고, 아직
`NOT_IMPLEMENTED`를 유지한다.

### backend owner에게 제안하는 additive effect interface

같은 업무 transaction 안의 append-only `ObservationEffects` journal이
필요하다. 최소 필드는 다음과 같다. 이 제안은 구현하거나 승인한
backend 계약이 아니다.

- immutable `effectId`, organization, command ID 또는 authenticated read
  ID, 실제 installation/turn binding ID, actor, 선택 grant, 발생/기록 시각
- versioned operation/class와 원 table/row ID/column 또는 불변 transition
  ID, 해당 사실의 canonical hash; 변경이면 before/after revision
- quantity effect이면 실제 physical root/interval, canonical occurrence,
  measurement kind/value/unit; obligation이면 assignment/root/scope와
  실제 이전/새 owner 및 nextAction/nextCheckAt
- 조회 감사와 명령 감사, 수량 효과, 의무, 외부 전송을 구분하는 coverage
  version; 알 수 없는 class는 reject한다.

installation/turn 귀속은 서버가 확인한 binding으로 저장해야 하며,
observer 요청 metadata만으로 채우면 안 된다. observer는 이 journal과
원행을 각각 read-only로 읽어 ID/hash/수량/조직을 재검증해야 한다.
현재 스키마에서 빠진 사실을 observer heuristic으로 대신할 수 없다.

## API projection revision과 MVCC

`ApplicationQueries.snapshot` 및 `TransportValues.normalize`를 읽고
현재 hash 원료를 확인했다. `snapshotRevision`은
`org|actor|asOf|knownAt|sorted-json(scope)|sorted-json(state)`의 SHA-256이다.
PostgreSQL `snapshot.id`는 독립 MVCC token이며 두 값을 비교하지 않는다.

`NativeProjectionObserver`와 `ProjectionRevisionObservation`은 다음
좁은 실제 recipe를 제공한다.

- `getObject`의 Place/Product/Manufacturer
- scope는 organizationId와 objectType만 허용한다.
- 실제 query로 읽은 원행과 관계 원행을 사용한다. 관련 relation이 하나라도
  있으면 visibility/기간을 추정하지 않고 거부한다.
- 실제 row의 ID/org, actor, 요청 scope, asOf/knownAt을 묶어 재계산한다.
  응답의 hash나 응답 data를 hash 원료로 쓰지 않는다.
- proof는 `snapshot.artifactRef`가 가리키는 독립 DB capture 안에 둔다.
  validator는 proof row를 capture의 원행 pointer와 대조하고 네 구현 파일의
source hash,
  요청/actor/time, 재계산한 revision을 확인한다.

전체 모델 runner는 이전 결과의 `data.projectionReadRef`가 가리키는
실제 연결된 wire artifact에서 query를 읽는다. `ACTUAL_API_CAPTURE` 또는
`ACTUAL_HOST`, scope/action/time, QUERY capability, 실제 응답의 revision을
확인한다. 이 reference가 없으면 명시적 `NOT_IMPLEMENTED`다.
wire의 서버 관찰 `authenticatedActor.actorId`가 없을 때도 마찬가지다.
그 원래 authenticated query를 `projectionRequest`,
그 실제 actor를 `projectionActorId`로 전달해야 한다. 문자열 hash 하나로
이 두 값을 역산하지 않는다. `RESULT_REVISION`과 `revisionQuery`의 기존
공통 schema 요구도 유지한다. 현재 context endpoint와 command 후
projection의 넓은 recipe가 없으므로 전체 before/after는 미완료다.

### 필요한 넓은 projection interface

backend owner는 실제 authenticated context/read에 대해 `queryId`,
`actorId`, 정규화 전 원래 query scope/filter, projection recipe/version,
사용한 fact row ID/revision 목록과 visibility 결정의 immutable 참조를
추가로 노출해야 한다. 또는 독립 observer가 동일 입력으로 재계산할 수
있는 read-only fact snapshot API를 제공해야 한다. 응답 data나 SHA를
복사한 필드는 독립 증거가 아니다. Work world/재고 적격성의 정책 계산을
observer 안에 몰래 복제하는 변경은 하지 않았다.

## 최종 원문과 native host

`NativeFinalResponseObserver`는 파일 capture와 전달된 capture가 같은지
확인하고, 원래 response string을 공백/개행 포함 그대로 보존한다.
`NativeHostObserver`는 config/summary가 아니라 app-server raw event에서
thread/start 응답의 실제 model/tier, turn 시작/종료, final_answer,
tool RPC와 응답, scope, process 종료 PID/exitCode를 추출한다. 원문 변경,
불완전 process custody, unmatched RPC, 다른 turn, builtin command 실행은
거부한다. 실제 process launch의 독립 command artifact와 기존
`HostObservationValidator` 형식 연결은 아직 필요하다. 해당 연결 없이
native model 실행을 허용하지 않는다.

한국어 semantic extractor는 `ko-explicit-clauses-v1`이라는 제한 문법이다.
원문을 바꾸지 않고 NFC 정규화한 사본만 읽는다. 다음 clause와 마침표 뒤
공백으로 나눈 조합만 완전히 소비한다.

- `<slot명>을/를 알려 주세요.` 또는 `확인해 주십시오.` 계열:
  수량/단위/품목/목적지/기한/끝점/수량 기준/담당자와 명시적 나열
- `실행하지 않았습니다.`, `요청이 거부되었습니다.`,
  `요청을 실행했습니다.`, `조회했습니다.`, `요청은 대기 중입니다.`
- `업무가 완료되었습니다/종료되었습니다/진행 중입니다/차단되었습니다.`
- `담당자의 책임은 유지됩니다.`, `담당자의 책임이 해제되었습니다.`,
  `책임이 이전되었습니다.`, `결과는 미확인입니다.`

이는 답변의 주장 분류일 뿐 실제 실행/완료 증거가 아니다. 수량 서술,
조건문, 인용, 부정의 다른 표현, JSON self-label, 미소비 문장, 중복/상충
clause는 `UNKNOWN/complete=false`다. 원문과 다른 golden answer나
string-similarity를 사용하지 않는다. READ 값 assertion이나 의무 수량을
추정하지 않으며 임의 자유 문장에는 semantic PASS를 내지 않는다.

기존 semantic schema에는 독립 `missingSlots` set과 explicit uncertainty를
추가했다. runner는 독립 missing-slot set을 실제 요구 set과 비교한다.
모델의 structuredIntent에서 이 set을 복사하지 않는다. host acceptance
연결이 없으므로 문법에 맞는 답변도 최종 StepResult는
`NOT_IMPLEMENTED`다.

## 검증

Java 21.0.12.1, Maven 3.9.16에서 다음을 실행했다.

```sh
./mvnw -B -ntp -f verification/harness/pom.xml \
  -Dtest=NativeObservationTest,NativeProductIntegrationTest,NativeClientContractTest,ModelBindingResponseEvidenceTest,ModelBindingEvaluatorTest,ModelBindingContractTest test
verification/model-binding/actual-integration transport-sit
```

- focused JUnit: 95개, failure/error/skip 0, exit 0이다. 신규
  `NativeObservationTest` 14개는 명시적 contract doubles다.
- 실제 transport-SIT: 27개 검사, exit 0이다. 고정 digest PostgreSQL,
  실제 backend JAR, HTTP/MCP, 독립 JDBC를 사용했다. 원행 109개 table과
  118개 cell을 관찰했고 Place projection SHA를 독립 재계산했다.
  artifact를 다시 읽는 projection validator도 통과했다.
- 최종 source hash는 실행 전후 동일했다. disposable container와 임시
  key를 정리했다. 실제 모델 0회, 전체 model-binding은 `NOT_RUN`이다.
- 이 SIT의 의무 원행은 0개였다. 비어 있지 않은 실제 의무/업무 효과의
  end-to-end 분류·귀속은 이 실행으로 검증하지 않았다.
- `git diff --check`를 통과했다. `NativeDeterministicGate` 및 ownership 밖
  backend/fixture/process/case/native-config diff는 없다.

최종 실제 증거는 아래 경로다.

`verification/harness/target/native-integration-runs/f5a24b60-9dd5-44cc-927e-ddb41d2613d4/`

- `execution.json` SHA-256:
  `7024c57df3f3e9f929373247d6f03ba53955ab3ee0e892fe9b557082092b1e96`
- `transport-sit.json` SHA-256:
  `edca39afa38f8abfcabbe7ca6d965b7e5232ea129bbbed3aac6cc663baee9701`

`execution.json.inputs`는 실행한 backend/harness/contracts의 경로별
hash/bytes를 담는다. 각 DB/MCP capture의 hash/bytes는
`transport-sit.json.artifacts`에 있다. 이 파일들은 로컬 증거이며 Git에
넣은 전체 runtime PASS나 모델 인수 receipt가 아니다.

집중 테스트 원본 로그는
`verification/harness/target/native-observer-verification/focused-tests.log`다.

초기 transport 재실행에서는 새 진단 schema의 상대 `$ref`를 standalone
validator가 해석하지 못했다. 해당 진단 schema를 self-contained로
수정하고 두 schema의 별도 검증을 유지한 뒤 위 최종 실행을 통과했다.
초기 build가 생성한 ignored backend source 때문에 발생한 source inventory
변화도 최종 실행에서는 없었다. 실패 evidence는 지우지 않았다.

## Task branch 통합 검사

`9734b5bb`를 `5cc75819`로 통합하고 집중 검사 95개와 실제 transport
검사 27개를 다시 통과했다. 실행 원본은 Task branch의
`verification/harness/target/native-integration-runs/8f2e364c-538c-426f-9d0f-edd512d30702/`
에 있다. source hash는 실행 전후 같고 생성한 container는 정리했다.
Task branch에는 보존 중인 untracked 파일이 있으므로 이 실행은
clean checkout 전체 인수로 기록하지 않는다. `./verify prepare`도
exit 0이다. 실제 model 호출은 0회로 유지했다.

통합 검토에서 native transport가 이미 허용하는 early notification을
host extractor도 처리하도록 보완했다. `turn/start` 응답 전에 도착한
동일 turn 알림은 같은 capture 안의 실제 RPC 응답 ID와 대조한다.
없는 응답이나 다른 turn ID를 추측하지 않는다. 이 수정 뒤
NativeObservationTest·NativeClientContractTest·NativeProductIntegrationTest
35개가 통과했다. 이는 전체 host acceptance 연결을 완료했다는 뜻은 아니다.
