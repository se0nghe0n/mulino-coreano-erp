# 독립 physical·projection 관찰 구현 기록

실제 HTTP 응답의 read revision과 독립 PostgreSQL snapshot을 연결하고,
실제 raw column으로 semantic quantity와 현재 책임을 재계산한다.
이 기록은 이 worker의 제한된 구현·실행 증거다. Task, 사용자 Step,
시스템 S5·S6 인수 완료를 선언하지 않는다.

## 소유권과 baseline

- baseline은 `f92415807b541bc28210238d446a1e1be74e5ad6`다.
- branch는 `feat/independent-model-observations`다.
- worktree는 `/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-b73744f6`다.
- 실제 runtime은 `codex_2 / gpt-6.1-sol / xhigh / priority`다.
- AGENTS, 설계 철학·구현 계획 전체, ontology-agent-implementation과
  ontology-scenario-testing skill 및 관련 acceptance/model 계약을 읽었다.
- backend, DB migration, CDS, public schema, common scenario observer와
  다른 worker의 미통합 checkout은 수정하거나 가져오지 않았다.
- ProductDriver에서는 `observe`만 바꿨다. query/send/installFixture,
  ProductSession과 requireReady gate는 coordinator 소유다.
- coordinator가 승인한 PhysicalObservation·BindingEvaluator·projection
  consumer 및 raw/derived discriminator schema만 좁게 확장했다.
- semantic-paths 158개, 원래 60개 model fixture/corpus/binding/oracle과
  원래 41개 scenario 고정 oracle은 바꾸지 않았다.

## 관찰의 경계

NativePhysicalCatalog는 현재 ontology CDS의 142개 실제 table을 닫힌
목록으로 고정한다. platform 기술 spike table은 명시적으로 제외한다.
observer는 같은 organization의 현재 committed row를 REPEATABLE_READ로
읽고 `pg_current_snapshot()`과 실제 `xmin`·`tableoid`를 보존한다.
knownAt보다 늦은 mutable row를 버려 과거의 빈 상태를 만들지 않는다.

실제 table·column inventory, type family, Decimal(38,12)와 현재 CDS 및
migration source hash를 보존한다. 누락·추가 table/column, type 또는
precision/scale 변경은 capture 자체를 실패시킨다. column cell의 value는
원래 raw column과 같아야 한다. 이 검사는 기존 raw 분기에 그대로 남는다.

DERIVED cell은 닫힌 recipe ID/version/source hash, snapshot, 조직·설치·
case·turn scope, 선택용 실제 ID, 완전한 raw column pointer 목록, 실제
unit·physical identity를 가진다. consumer가 모든 값을 다시 계산한다.
artifact에 expression이나 executable code, expected 값은 받지 않는다.
unknown recipe, missing operand/source, incomplete visibility, 혼합 unit,
중복 physical row, 다른 snapshot/scope는 실패한다.

같은 dataset.attribute의 collection/sum 또는 receipt sum/count는 하나의
canonical recipe를 공유한다. distinct physical quantity는 root와 반개구간
범위를 합쳐 중복 source와 겹치는 범위를 다시 더하지 않는다. 완전히
관찰한 닫힌 빈 집합만 scope marker로 나타낸다. UNKNOWN을 0으로 바꾸지
않는다. 현재 사실·누적 사실, provisional·confirmed source, source 검증과
저장된 goal evaluation을 별도로 다룬다.

AUTHENTICATED_API 값은 실제 wire 추출 경로를 유지한다. 독립 SQL 값으로
API 응답을 대체하지 않는다. binding/fixture의 별칭은 실제 ID를 고르는
입력이고 업무 사실의 출처가 아니다.

## 실제 read revision 재계산

기존 단순 Place/Product/Manufacturer recipe를 유지한다. 새 닫힌
`APPLICATION_QUERIES_V2_BUSINESS_WORLD / 1` recipe는 getInventory,
getWork와 TradeItem/QuantitySegment/ManufacturingLot getObject가 사용하는
실제 business world를 raw SQL에서 다시 만든다.

현재 membership·capability assignment·grant·delegator와 all-of scope,
Work의 immutable transition, goal/assessment input snapshot, inventory의
물량·QC·customer·commercial·regulatory·label/source verification,
receipt·sales·dispatch·delivery·ledger와 공개 visibility를 반영한다.
organization inventory의 item block도 별도 recipe로 다룬다. 관련 없는
physical DB 전체를 hash해서 product revision인 것처럼 쓰지 않는다.

proof consumer는 원래 query scope/time/actor, 원래 observationScope,
실제 capture rawRows/sourceEvidence, recipe source hash와 operand set을
대조한 뒤 실제 product revision과 독립 digest가 같은지 검사한다.
알 수 없는 query/filter/object type과 지원하지 않는 visibility 경로는
실패한다. 부분 가시성을 넓혀 통과시키지 않는다.

## causal effect와 현재 책임

before/after는 같은 organization·installation·case·turn이어야 한다.
실제 server-authenticated MCP capture의 응답 commandId와 원래 response
hash, committed CommandRecords.resultJson·actor·stableRequestOwner,
CommandAudits 및 원래 full transactionId와 physical row transaction을
대조한다. Work 생성은 immutable WorkTransitions의 실제 snapshot과
operation으로 확인한다. replay와 foreign/interleaved turn은 자기 효과로
세지 않는다. effect ID는 command + physical table/row + operation이며
최신 mutable xmin으로 이전 효과의 이름을 새로 만들지 않는다.

primary/support table class를 닫힌 목록으로 구분한다. 분류하지 못한
primary effect, immutable journal 없는 update/delete, 완전하지 않은
class inventory는 완료된 0으로 보고하지 않는다. xmin은 원래 transaction
증거를 확인하는 보조 값이며 stable effect identity가 아니다.

현재 assigned duty는 실제 human owner/supervisor, nextAction/nextCheckAt,
Work·root·scope와 물량을 가진다. root/scope source의 실제 row와 시간 및
assignment binding을 확인한다. intake는 실제 immutable
RecoveryObservationHistory의 knownAt head와 source scope에서 읽는다.
기존 row의 불완전 history를 complete로 승격하지 않는다.

mutable assignment의 과거 owner/status는 증명하지 않는다. 현재 duty를
쓸 때는 server authorityAt = knownAt = asOf가 필요하다. 과거 상태와
update effect는 아래 product history layer 전까지 명시적으로 실패한다.

## integration 입력 계약과 남은 범위

observe 요청에는 기존 scope/asOf/knownAt/snapshotRef/projectionRequest/
projectionActorId 외에 `observationScope`가 필요하다.

```json
{
  "scope": {
    "organizationId": "actual UUID",
    "installationId": "actual installation ID",
    "caseId": "actual case ID",
    "turnId": "actual turn ID"
  },
  "businessScope": {"itemId": "actual UUID"},
  "bindings": {
    "A": {"table": "mulino_inventory_QuantitySegments", "id": "actual UUID"}
  },
  "actorId": "actual UUID",
  "authorityAt": "captured server authority time"
}
```

businessScope와 bindings는 독립 설치 receipt가 고정한 실제 ID를 선택한다.
허용된 선택 key만 받으며 값을 사실로 사용하지 않는다. coordinator가
model installer의 typed binding/source receipt와 이 입력을 연결한다.
다른 worker 파일을 추측해서 import하지 않는다.

현재 명시적인 잔여 경계는 다음과 같다.

- ObligationReferences/Handovers/SegmentAllocations의 과거 변경과 update
  effect에는 immutable product history가 필요하다. mutable current row와
  xmin으로 대체하지 않는다. unknown legacy history는 계속 unknown이다.
- primary class가 없는 command 또는 미분류 immutable business effect는
  `PRIMARY_EFFECT_UNCLASSIFIED`/`UNKNOWN_COMMITTED_EFFECT_CLASS`다.
- 실제 Purchase ProposalRevisions.goalVersionId가 NULL이므로
  dueInclusive/qcNotArrivalPrecondition의 immutable goal binding은 unknown이다.
  coordinator에게 propose/revise 시 정확한 goal ID를 저장하고 proposal/hash/
  approval에 묶는 별도 product 변경을 제안했다. 현재 Work goal로 대체하지
  않는다.
- recall unknown partition에는 완전한 scope partition proof가 필요하다.
- intake의 closedByNotification은 실제 closure reason이 기록되지 않으면
  unknown이다. conditional purchase approval은 독립 goal 조건 평가가
  없으면 unknown이다.
- 알 수 없는 historical assessment scalar, 없는 실제 condition operand,
  불완전한 Work/goal/entity selector는 value를 만들지 않는다.
- simple object의 related visibility, 지원하지 않는 projection operation과
  filter는 fail-closed다. 검증한 business recipe와 구분한다.
- requireReady와 최종 response extractor custody/semantic completeness는
  coordinator가 소유한다. 이 commit은 blanket gate를 제거하지 않는다.

## 별도 product history 제안

이 절은 coordinator에게 전달한 제안이다. backend/CDS/migration을 이
worker가 구현한 것이 아니다. 세 append-only table을 제안한다.

| table | source | 불변 scope binding |
|---|---|---|
| ObligationReferenceHistory | ObligationReferences | source assignment, immutable root/scope, Work transition와 item/lot evidence |
| HandoverHistory | responsibility.Handovers | original/target Work, assignment/root/scope, immutable Work/item evidence |
| SegmentAllocationHistory | inventory.SegmentAllocations | original segment, immutable sales line/Work/item/lot/place evidence |

공통 field는 organization, sequence history ID, source ID/revision,
INSERT/UPDATE/DELETE, ExecutionClock recordedAt/effectiveAt, full txID,
commandId 또는 trusted runtime/fixture invocation ID, sourceKind,
installation/source receipt, beforeJson/afterJson, genesis/historyComplete와
scopeComplete다. INSERT genesis도 남긴다. 삭제 뒤 source ID 재사용과
scope identity 변경을 막고, accepted/rejected/expired 이력도 불변으로
보존한다. 같은 transaction 안의 거부/rollback은 history까지 rollback한다.
별도 denial audit가 성공 effect history를 만들면 안 된다.

SQL setter는 application의 ExecutionClock과 현재 command/runtime context를
받아 같은 transaction에만 적용한다. 사용자 slot 시각이나 wall now()를
fallback으로 쓰지 않는다. GUC label만 믿지 않고 원래 CommandRecords,
등록된 runtime invocation 또는 fixture receipt와 대조한다. history 직접
쓰기/update/delete/truncate 권한을 막는다. fixture SQL은 별도 trusted
fixture-clock/sourceKind/installation receipt를 명시하며 business command
효과로 취급하지 않는다. 기존 row에 complete=true를 backfill하지 않는다.

trusted writer context를 적용할 경계는 다음과 같다.

- ApplicationCommands.apply의 repository.begin/CommandExecution.enter부터
  handler·finish/audit까지와 executeClaimed/replay/changeset 경계다.
- ResponsibilityCommands/ResponsibilityService/ResponsibilityRepository의
  assignment 생성·완료·분할·이관·owner 교체·handover 결정/만료다.
- Work 명령 및 QualityResponsibilities/RegulatoryResponsibilities,
  sales DeliveryCommands/DeliveryCorrection/SalesCommands,
  settlement SettlementState/SettlementContributionReview,
  purchase·receipt·recall duty port를 통한 모든 assignment 쓰기다.
- FulfillmentCommands/InventoryCommands/QualityCommands와
  FulfillmentStockPrimitives/StockPrimitives/ReceiptStockPrimitives/
  QualityPrimitives가 쓰는 allocation 생성·pick·release·replace·suspend와
  InventoryRepository의 공통 mutation 경계다.
- InventoryValiditySweeper/ValidityBoundarySweep,
  RuntimeRecoveryExecution/RuntimeRecoverySweeper/RuntimeService,
  RuntimeIntakeLinker와 RuntimeDutyPort의 command 밖 scheduled/runtime 쓰기,
  duty handover expiration 및 verification scheduler 경계다.
- FixtureInstaller/ScenarioFixtureInstaller와 전용 T11/evidence fixture SQL,
  model installer의 앞으로 통합할 synthetic SQL 경계다. installer 계약은
  coordinator를 통해 맞추며 미통합 worker source를 공유하지 않는다.

## 실행 및 artifact

최종 실행 결과와 hash는 아래 실행 receipt에 연결한다. 모든 최초 실패
log와 capture를 유지한다. 자체 loopback server, 자체 이름의 PostgreSQL
container, 자체 RSA key·blob directory만 만들고 정리했다. 다른 worker의
server/container/key/blob은 정리하지 않았다.

시나리오 runner, 실모델, Workflow, 외부 전송, API/BTP 유료 실행은
NOT_RUN이다. 직접 probe는 현재 ontology의 기존 synthetic HTTP 입력 중
필요한 명령만 호출한다. oracle/assertion을 실행하거나 expected를 fact
source로 사용하지 않는다. 기존 source 원본은 실제 private blob에 쓰고
readback SHA-256을 확인한다.

최종 focused 명령은 다음과 같다. 기존 59개 검사를 포함해 70개가
failure/error 없이 exit 0으로 통과했다.

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
./mvnw -B -ntp -f verification/harness/pom.xml \
  -Dtest=NativeDerivedObservationTest,NativeObservationTest,ModelBindingResponseEvidenceTest,ModelBindingEvaluatorTest test
```

직접 실제 검증은 다음 명령으로 8개가 failure/error 없이 exit 0으로
통과했다. 최종 run은 `http-pg-d20697e1-3ec1-4d6a-b86e-95721762182e`이며
Maven test 실행 시간은 100.3초다. scenario/model/Workflow 실행은 없다.

```sh
sh verification/harness/src/test/resources/native-independent/run-http-pg.sh
```

실제 검증은 다음을 확인했다.

1. Place의 실제 HTTP revision, 독립 MVCC token과 raw/derived onHand다.
2. 실제 createWork response commandId·commit/audit/tx·immutable row 증거다.
3. foreign/out-of-turn Work를 자기 효과로 세지 않는 negative다.
4. 실제 현재 READ의 all-of ITEM 차원 누락 거부와 독립 인가 재계산이다.
5. 누락/변경 column, Decimal scale 변경과 unknown table 거부다.
6. 물량이 있는 inventory/work/TradeItem의 실제 business world revision이다.
7. provisional의 새 실물 0, 실제 현재 human owner/supervisor/root/scope,
   PREPARED와 SUBMITTED 구분, source 원본 bytes·검증·중복 수령의 100 BOX,
   QC/customer/commercial/regulatory/label, 실제 30 BOX 최종 인도와
   inventory/work revision, current intake의 immutable genesis·불완전 history
   negative다. source witness가 여러 개이면 해당 canonical의 실제 증거를
   모두 바꿔 검증하며, 다른 정상 증거가 있는 상태를 누락으로 가장하지
   않는다.
8. 동시 foreign turn과 자기 명령, replay, incomplete inventory와 삭제된
   immutable row를 자기 효과/완료된 0으로 받아들이지 않는 negative다.

최초 fixture hash 누락, timestamp/JDBC type 차이, effect support 분류,
probe의 actor/revision 연결, source witness 선택 및 artifact 정수 표현의
실패 log를 보존했다. 이전 반복의 class hash가 없던 시점은 재구성했다고
주장하지 않는다. 최종 실행 직전 manifest의 source/class/schema/runner가
실행 뒤에도 같음을 대조했다. 최종 custody에는 source·fixture·build input,
78개 harness class와 backend JAR 내부 class/resource hash를 기록했다.

사용한 Node는 24.19.0, npm은 11.19.1, CDS는 10.1.1,
CDS DK는 10.1.0이다. JDK 21과 Maven wrapper 3.9.16을 사용했다.
backend npm lock과 자체 Node/npm lock, 실제 Java/Maven version 출력과
pinned PostgreSQL image reference는 log archive에 보존했다.
OrbStack socket은 `unix:///Users/se0nghe0n/.orbstack/run/docker.sock`이다.

16개 자체 disposable container와 모든 자체 key/blob resource directory의
정리를 확인했다. 다른 worker 자원은 건드리지 않았다. build/toolchain과
공개 synthetic 실행 증거는 이 worktree에 보존했다.

[summary](s5-independent-model-observations-evidence/summary.json)와
[custody](s5-independent-model-observations-evidence/custody.json)가 상세
receipt다. raw archive의 원래 경로는
`verification/harness/target/native-integration/` 아래다. 실행 당시의
독립 baseline checkout에서 artifact reference를 복원해 대조할 수 있다.
기존 진단용 native-physical schema와 coordinator의 최종 bridge gate는
변경하지 않았다. 새로운 실제 결과는 acceptance observation과 닫힌
physical/projection recipe consumer로 검증했다.

| artifact | SHA-256 |
|---|---|
| execution-logs.tar.gz | `175293e3e5a9baff532917ae28fa6ea38f132c8f1b324126fa57750b0966c9bb` |
| raw-observations.tar.gz | `b514c29b5ada2d56d08e566a9c342afda17fab494a27c9501ef06f4218a82dc2` |
| harness-classes.tar.gz | `a03cc8127914baa11a4bd05a65a90e0f223ce6d805da2afe1032d4696c2ecef0` |
| custody.json | `c9ca87aeabaab4ac272f4e5f0051a19abc53d8302e96a8d55a8cdf37ad5fb9b5` |
| 실제 backend JAR | `75a9f48e0acc33afb4d012b6a304023ca7d47affcbe8f85ac57d1f438779b250` |
