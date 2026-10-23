# 구매 제안의 불변 goal pin

구매 제안이 승인 당시의 목표 정책을 다시 읽을 수 있도록 기존
`ProposalRevisions.goalVersionId`에 실제 goal ID를 저장한다. 최신
Work goal로 과거의 기한 끝점이나 QC 조건을 역추정하지 않는다.
이 변경은 독립 제품 fix이며 Task·S5·S6 closure가 아니다.

## 범위와 baseline

- Worktree는 다음 절대 경로다.
  `/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-22cf88c9`
- Branch는 `fix/purchase-goal-pin`이며 시작 HEAD는 다음과 같다.
  `33f075f8208c357a95537512c9757e4a1cc9a857`
- 시작 tree는 clean이었다. coordinator만 Task branch에 통합한다.
  main·remote·다른 worker의 source와 build를 변경하지 않았다.
- 구현 근거는 계획 §3.3·§5.2·§6·§7.1의 불변 제안·목표 변경·승인
  계약과 D09/D12/D13/D21, V1/V6다. 원본 60건의 corpus·binding·
  fixture·ID·fixed oracle·actor·grant·clock은 수정하지 않았다.
- SOURCE·identity·Inventory·공통 observer·ModelFixtureInstaller·
  ProductDriver는 수정하지 않았다. 실제 scenario·model·Workflow·
  Grok·외부 메시지는 실행하지 않았다.

위임 brief는 `codex_2/gpt-6.1-sol/xhigh/priority`다. live catalog에서
inherited provider/model과 `priority` 지원을 확인했고, harness는
`gpt-6.1-sol/xhigh`를 표시했다. catalog의 composer 기본 tier를 이번
위임의 effective tier 증거로 바꾸지 않는다. 자식 turn에는 별도의
실행 tier 반환값이 없다.

## 서버 계약

서버가 `Work.currentGoalVersionId`로 선택한
`mulino.work.read.GoalReferences` 행이 pin의 원본이다. 새 요청 slot이나
새 goal 테이블을 추가하지 않는다. 같은 명령 transaction에서 Work row를
잠그고 organization·work·current pointer·definition의 일치를 검사한다.
기존 `work:<id>` fence는 `reviseGoal`과 같은 직렬화 경계다.

`proposePurchase`는 선택 slot의 유무와 관계없이 이 pin을 저장한다.
기존 선택 slot `goalVersionId`를 주면 현재 pin과 일치해야 한다.
다른 Work·다른 organization·과거·미존재 ID는 모두 효과0의
`REJECTED/TYPE_INVALID`이며 해당 ID의 존재를 설명하지 않는다.
잘못된 값이 들어간 과거 revision도 승인 시 조직/Work 검사를 통과해야 한다.

`revisePurchase`는 실행 시점의 current pin을 새 revision에 저장한다.
이전 revision·goal 행·승인 행은 갱신하지 않는다. 목표만 바뀌었거나
과거 pin/hash 계약이 없는 제안을 전환할 때는 기존 `changes: {}`와
명시적 reason으로 새 revision을 만들 수 있다. 변경 없는 current
제안의 빈 changes는 계속 거부한다. 변경 slot allowlist는 그대로다.

`PurchaseHash`는 기존 상업 조건 전부와 `goalVersionId`를 결합한다.
quantity·price는 decimal의 trailing zero를 정규화하고 dueAt은 Instant로
정규화한다. DB의 numeric scale과 wire 표현의 차이가 동일한 제안의
hash를 바꾸지 않는다. 승인 행의 기존 proposal hash가 goal pin도
승인하므로 승인용 새 goal column은 필요하지 않다.

승인 생성·발주·외부 전달 재검증은 저장된 pin의 조직/Work, 현재 goal과의
일치, pin을 포함한 hash를 검사한다. Work goal이 바뀌면 기존 제안의
새 승인/발주는 `HELD/APPROVAL_HASH_MISMATCH`다. 새 revision은 기존
`currentApprovalId`를 비우며 새 승인을 요구한다. 기존 승인 소비 정책,
조건부 승인 평가, manager 권한·유효기간·현재 정책 검사는 유지한다.

멱등 replay는 기존 공통 pipeline이 현재 조회 권한을 검사한 뒤 저장된
응답을 반환한다. 따라서 성공한 제안의 같은 key replay는 goal이
바뀌어도 원 pin/결과를 반환하며 새 효과를 만들지 않는다. 새 key의
제안과 명시적인 새 revision만 current goal을 선택한다.

price/amount 검증의 기존 범위와 정밀도는 바꾸지 않았다. 검사 중 공개된
flat `unit`·`currency` slot을 proposePurchase가 validatedRevision에
전달하지 않아 `Missing unit`이 발생하는 기존 오류를 찾았다. 기존
slot 두 개를 전달하도록 고쳐 typed/flat 금액을 같은 검증에 연결했다.
상업 조건과 Work goal의 수량·끝점이 반드시 같다는 새 제약은 두지 않았다.

## 기존 schema와 legacy

Migration은 필요하지 않다. V16에 nullable `goalVersionId`와
`(organizationId, goalVersionId)` FK가 이미 있고 GoalReferences와
ProposalRevisions의 mutation 거부 trigger도 있다. 실제 PG에서 column,
FK와 immutable trigger를 확인했다. V45 identity·V46 stocktake·V47
future history의 예약 번호를 사용하거나 기존 migration을 바꾸지 않았다.

baseline은 slot을 생략한 제안에 NULL pin을 허용했고, 선택 slot을
주더라도 hash에는 pin을 넣지 않았다. 과거 NULL pin을 current goal로
backfill하면 당시 정책을 발명하므로 기존 행을 보존한다.

- Work에 current goal pointer가 없으면, 과거 goal 행이 있어도 새 제안을
  `HELD/POLICY_UNRESOLVED`로 막는다.
- 과거 제안의 NULL pin은 새 승인·발주를 같은 code로 보류한다.
- pin이 있어도 과거 hash가 pin을 결합하지 않았으면
  `HELD/APPROVAL_HASH_MISMATCH`로 보류한다.
- current goal을 확정하고 명시적으로 revise하면 새 revision만 pin을
  얻는다. 과거 revision·승인 hash·판정 의미는 보존한다.

DB의 기존 nullable column은 보존한다. 범용 raw DB INSERT를 새 제품
명령으로 허용하지 않으며 새 제안의 필수 pin은 command 경계가 보장한다.

## 실행한 독립 검사

검사는 자기 worktree의 Maven build·실제 CQN·HTTP·disposable PG에서
실행했다. test double을 실제 PG나 전체 제품 인수로 세지 않는다.

`PurchaseGoalPinPostgresTest`는 새 fixture의 current goal pointer와
불변 goal 행을 준비한다. 같은 checkout의 기존
`PurchaseCommandPostgresTest`에서 41개 구매 회귀를 상속하고 새 PG
검사 17개를 더했다. 기존 test나 그 seed 파일은 수정하지 않았다.
`PurchaseHashTest`는 순수 hash 검사 2개다.

| 검사 | 관찰한 결과 |
|---|---|
| 생략·명시 pin | 저장된 실제 goal ID·응답 ID·재계산 hash가 같다 |
| current goal 없음 | 제안/revision/승인/outbox가 0이며 과거 goal을 택하지 않는다 |
| 다른 Work·조직·과거·미존재 pin | 새 제안 효과0이며 조직 FK도 다른 조직을 거부한다 |
| goal 변경 | 원 proposal의 deadlineInclusive=true를 보존하고 current false와 구별한다 |
| 승인 후 goal 변경 | 이전 승인으로 새 발주와 외부 전달을 진행하지 않는다 |
| goal만 새로 pin | 빈 changes로 revision을 만들고 새 승인 후 발주 1건을 만든다 |
| replay | 원 pin 응답 재사용·효과1회·다른 payload conflict를 확인한다 |
| legacy NULL/옛 hash | 과거 행은 불변이며 명시한 새 revision만 current pin을 얻는다 |
| rollback | 새 revision·Work invalidation·audit·멱등 결과가 함께 되돌아간다 |
| 금액 | typed/flat의 0·최소 소수·12자리 소수·numeric(38,12) 최대값을 승인/발주한다 |
| 기존 구매 회귀 | 조건부/반려/만료/철회/정책 변경·상업 조건 변경·경합·전달 재검증을 유지한다 |

초기 test compile 오류와 HTTP test JWT의 필수 claim 누락(401)은 제품
RED로 세지 않고 별도 로그에 보존했다. baseline 원실패는 실제 pin
assertion에서 expected UUID/observed NULL인 1 failure, errors0, skip0다.
flat dimension 오류도 수정 전 4 failure로 보존했다.

최종 toolchain은 JDK Homebrew 21.0.12.1, Node 24.16.0, npm 11.13.0,
Maven 3.9.16, CAP Java 5.1.1, Boot 4.1.1이다. 기존 npm lock으로
`npm ci --ignore-scripts --no-audit --no-fund --prefix backend`를 실행했다.
직접 CDS는 10.1.1, compiler는 7.1.1, DK는 10.1.0이다. lock에 있는 DK
내부 CDS 10.1.0/compiler 7.1.0도 version 로그에 구별해 남겼다.
`npm ls`는 기존 DK의 peer `picomatch@*` 누락을 보고한다. lock을 바꾸지
않았으며 실제 CDS compile과 아래 clean build가 성공하는지 검사했다.

PostgreSQL은 다음 고정 image의 실제 18.6이다.

```text
postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280
```

최종 명령은 repo root에서 다음과 같이 실행했다. 기본 shell이나 cwd에
따른 Node 선택을 피하도록 PATH를 명시한다.

```sh
export PATH=/Users/se0nghe0n/.nvm/versions/node/v24.16.0/bin:/Library/Java/JavaVirtualMachines/openjdk-21.jdk/Contents/Home/bin:$PATH
export JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk-21.jdk/Contents/Home
export DOCKER_HOST=unix:///Users/se0nghe0n/.orbstack/run/docker.sock
export DOCKER_CONTEXT=orbstack
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
./mvnw -f backend/pom.xml -B -ntp \
  '-Dtest=PurchaseGoalPinPostgresTest,PurchaseHashTest' clean test
```

최종 clean build는 exit0이며 실제 PG/HTTP 회귀 58건과 hash unit 2건이
failures0/errors0/skipped0으로 끝났다. `git diff --check`도 exit0이다.
다른 디렉터리에서 기본 Node26이 선택되는 환경을 확인했으므로 최종
실행은 위의 명시 PATH와 새 compiled class로 다시 수행했다.

## 증거 custody와 남은 gate

로컬 원증거는 이 worktree의 `.local/purchase-goal-pin/evidence/`다.
`final-inputs.json`은 source·compiled main/test class·tool binary·version
artifact의 개별 SHA256과 정렬된 집합 SHA256을 담는다. 이 검사는 dirty
상태의 자기 변경을 hash로 묶은 focused 검사이며 clean coverage receipt를
발행했다고 주장하지 않는다. 최종 own commit은 handoff에 반환한다.

Source 집합 SHA256은 다음과 같다.

```text
f357892dbefb53ae0c1928180486fcb2a65967bb247f85a10d29f456bfffac28
```

Compiled main class 집합 SHA256은 다음과 같다.

```text
8bafa5b8b687c76b1e1ea9d37f1f8a8a26ae6ae787b4cda03fc5bdc5ac38ec89
```

Compiled test class 집합 SHA256은 다음과 같다.

```text
e893a0d4f5eaebbeec2077746fa4a7b4bbefdd3a94c943523c4c364efa72de24
```

Compiled resources 집합 SHA256은 다음과 같다.

```text
84343f6334314dd8031fc6efea540582e79b57441e2c4a0e29403097c075bd8e
```

Toolchain version artifact 집합 SHA256은 다음과 같다.

```text
fd4924b567d46802667ae619d4e0ec32e2bcd96618e2c952d4d2c83d0d11def2
```

Baseline 원실패 XML SHA256은 다음과 같다.

```text
f4435cc82037dc83e90a3372c1f4d8d458ff7ab91e3726d1bc63bf3c99dff7f8
```

최종 PG/HTTP XML SHA256은 다음과 같다.

```text
a4cfb6d64a68e9f3f94fb21dd39643c5e45144e6591fe37a779d81c758b79124
```

최종 hash unit XML SHA256은 다음과 같다.

```text
345c5f4deef50ced28a01794fed29e087e9d2e994ba7f02a4d231b4449e4623a
```

최종 clean build log SHA256은 다음과 같다.

```text
eeec723d72f320f58014832e7ebb3fa3537980fc1315b59656fc66b5305f1d59
```

최종 input custody JSON SHA256은 다음과 같다.

```text
84f692d2c7945502f235ced5afd4d16529c31ea2ad38244af333fba9edb1d71f
```

`artifact-sha256.json`과 `check-results.json`은 원 JUnit XML·로그와
검사 수를 대조한다. coverage receipt나 Task PASS manifest가 아니다.
로그·XML·입력·toolchain·cleanup을 묶은 로컬 전달 파일은 다음과 같다.

```text
.local/purchase-goal-pin/final-evidence.tar.gz
28f4836c361f24a7c08fcff1b4af578e901dc33d4076e8fa4b130df096555935
```

모든 기록된 자기 PG/ryuk container ID의 제거를 OrbStack inspect로
확인한다. 상속한 AfterAll이 자기 임시 public key와 blob directory를
지우며 cleanup error는 없다. 자기 자원 외에는 정리하지 않았다.

기존 product943·fullharness R3 증거를 이번 변경의 결과로 재사용하지
않는다. 원래 PurchaseCommandPostgresTest의 seed는 current pointer가
없는 legacy 형태다. 새 suite의 별도 fixture 확장으로 기존 41개 업무
assertion을 검사했으며 원래 전체 backend suite가 그대로 통과한다고
주장하지 않는다. 이 intentional fail-closed 계약과 legacy fixture의
기대값은 해당 소유자가 통합 gate에서 함께 검토해야 한다.

Task branch 통합·전체 backend/harness·원본 60건의 독립 observer 및
scenario/model 재실행은 coordinator의 gate다. 이번 worker는 실행하지
않았다. R4/R5/R7/R8 운영·규제·BTP·실모델 인수도 완료하지 않았다.

## 2026-10-24 후속: 기존 positive Given의 current pointer 연결

사용자가 기존 backend positive seed의 포인터 연결까지 소유를 확장했다.
후속 시작 baseline은 `1dac141bb93d5dd55e6dd10a42b7dce0ccf13451`이다.
같은 worktree와 branch를 사용하며 시작 tree는 clean이었다. 앞 절의
60건 실행과 `final-evidence.tar.gz`를 덮어쓰지 않고 보존했다.

기존 `PurchaseCommandPostgresTest`와 `SettlementCommandPostgresTest`에
각각 한 줄만 추가했다. 이미 있는 GoalReferences INSERT 직후 같은
organization·Work를 조건으로 기존 goal ID를 currentGoalVersionId에
연결한다. 순환 FK 때문에 Work를 먼저 만든 뒤 goal을 삽입하고 pointer를
연결한다. 새 goal·정책·목표 slot을 만들지 않는다.

추가한 한 줄을 제거하면 각 파일은 `1dac141b` 원본과 byte 단위로 같다.
`minimal-given-invariance.json`이 전후 SHA256과 이 검사를 남긴다.
따라서 원래 goal·definition·predicate·hash·actor·grant·clock,
모든 assertion ID/op/expected와 mock/legacy NULL negative는 그대로다.
제품 PurchaseCommands·pin helper·hash도 변경하지 않았다.

호출 조사에서 다음과 같이 실행 의존성을 구별했다.

| 기존 경로 | 조사와 후속 처리 |
|---|---|
| PurchaseCommandPostgresTest | 실제 제안·승인·발주를 수행한다. 기존 goal pointer 한 줄을 연결하고 원본 클래스 자체를 실행한다 |
| SettlementCommandPostgresTest | prepareInvoice가 실제 구매 제안·승인·발주를 선행한다. 동일한 pointer 한 줄만 연결한다 |
| SalesCommandPostgresTest | propose/approve/dispatch helper는 선언만 있고 호출되지 않는다. source를 수정하지 않고 판매 회귀를 실행한다 |
| PurchaseApprovalMigrationPostgresTest | V37→V38의 legacy NULL 소비 정책을 검증한다. negative를 그대로 실행한다 |
| BusinessDiscoveryPostgresTest | legacy NULL goal pin과 기존 임의 hash를 가진 raw 행의 조회를 검증한다. backfill 없이 기존 조회 회귀를 실행한다 |
| ReceiptGatewayPostgresTest | raw 구매 line을 준비하며 proposePurchase를 호출하지 않는다. 이미 자기 수령 goal pointer가 있고 이번 hook 대상이 아니다 |
| ShipmentGatewayTest·RecallGatewayFixture·VerificationSchedulerTest | mock/raw 참조 또는 scheduler 계약이며 실제 구매 제안 positive Given이 아니다. 수정하지 않는다 |

`PurchaseGoalPinPostgresTest`의 NULL pointer·다른 Work/조직·goal 변경·
옛 hash·legacy NULL pin negative와 `PurchaseHashTest`도 그대로 다시
실행했다. 상속 subclass의 결과와 원본 41건 결과는 별도 JUnit XML로
구별한다. 클래스마다 새 JVM을 사용해 공통 static PostgreSQL의
AfterAll과 다음 클래스의 lifecycle이 섞이지 않게 했다.

후속 실행은 앞 절과 같은 명시 PATH/JAVA_HOME/OrbStack 환경에서
다음 명령으로 실행했다.

```sh
./mvnw -f backend/pom.xml -B -ntp \
  -DforkCount=1 -DreuseForks=false \
  '-Dtest=PurchaseCommandPostgresTest,SettlementCommandPostgresTest,PurchaseGoalPinPostgresTest,PurchaseHashTest,SalesCommandPostgresTest,PurchaseApprovalMigrationPostgresTest,BusinessDiscoveryPostgresTest' \
  clean test
```

수정 전 원본 클래스의 positive 한 건은 APPLIED 기대/HELD 관찰로
실패했다. 이는 기존 current pointer NULL 공백의 실제 실행 기록이다.
수정 뒤 위 clean build는 exit0, 총 133건 failures0/errors0/skipped0이다.
원본 클래스 자체와 subclass를 별도 JVM과 XML로 실행한 결과는 다음과 같다.

| 실행 클래스 | 실제 검사 수 | 실패/오류/skip |
|---|---:|---|
| BusinessDiscoveryPostgresTest | 8 | 0/0/0 |
| PurchaseApprovalMigrationPostgresTest | 1 | 0/0/0 |
| PurchaseCommandPostgresTest | 41 | 0/0/0 |
| PurchaseGoalPinPostgresTest | 58 | 0/0/0 |
| SalesCommandPostgresTest | 8 | 0/0/0 |
| SettlementCommandPostgresTest | 15 | 0/0/0 |
| PurchaseHashTest | 2 | 0/0/0 |

후속 원증거는 `.local/purchase-goal-seed-followup/evidence/`다.
앞선 60건 archive와 후속 archive는 별개다. 추가한 Given 이외의 기존
파일 내용을 바꾸지 않았고 제품 main class가 이전 실행과 같은지도
대조했다. 전체 source 집합은 두 test source가 바뀌므로 별도 hash다.

후속 source 집합 SHA256은 다음과 같다.

```text
07a0aac53c9736a69cfbbd7e1a30e0ebdf7b1bffaec0dce840ccda4b21f57f48
```

후속 compiled main class 집합 SHA256은 다음과 같다.

```text
8bafa5b8b687c76b1e1ea9d37f1f8a8a26ae6ae787b4cda03fc5bdc5ac38ec89
```

후속 compiled test class 집합 SHA256은 다음과 같다.

```text
78fb036d0e548084e8cd1ec4ac58af0e1440d160e0cfd1e84097aa13dd4f3a10
```

후속 compiled resources 집합 SHA256은 다음과 같다.

```text
84343f6334314dd8031fc6efea540582e79b57441e2c4a0e29403097c075bd8e
```

후속 toolchain version artifact 집합 SHA256은 다음과 같다.

```text
4f05d7ce5c63fc1c30db62ce5d5474c47f220eb386478d79e77008b118de1963
```

원본 구매 클래스 41건 JUnit XML SHA256은 다음과 같다.

```text
69966c63afe911af8de44307e3aa940978f3f36d74ec0864ad99897814b21b98
```

후속 clean build log SHA256은 다음과 같다.

```text
e9d4f80f7e305f10d062c54c00bbeb6fa187dcc4e4e2652c7c61bface0e37436
```

후속 input custody JSON SHA256은 다음과 같다.

```text
e31ffaf79e8cb9e0fea423e6a5ebac1144fffa06a4f72e1f2c72e88c67d0fa9f
```

후속 raw archive는 다음과 같다.

```text
.local/purchase-goal-seed-followup/final-evidence.tar.gz
afe5a289b1da1c403ba8d2c744d634c4f38458458141ba8858208289eeea1f09
```

기록된 자기 PG/ryuk container 14개의 exact ID가 모두 제거됐음을
명시한 OrbStack socket에서 inspect했다. 클래스별 AfterAll/close가 자기
public key·blob directory를 정리했고 cleanup error는 없다. 다른 worker의
container·process·tree를 정리하지 않았다.

이 후속은 기존 positive Given의 pointer 공백을 닫는 한정 회귀다.
위 초기 기록의 원본 구매 클래스 미실행 경계를 이 후속 결과로 갱신하며,
이전 subclass 결과를 원본 클래스의 결과로 승격하지 않는다. 원본 60건
model·41개 scenario fixture의 변경이나 실행을 허가받은 것으로
해석하지 않았다. root Task 6929 source를 가져오지 않았다.
V45 identity·V46 stocktake와 별도 worker의 V47 history는 이 branch에
통합하지 않았다. 이 조합과 full backend/harness·scenario/model은
coordinator의 종합 gate로 남는다. Grok이나 외부 메시지는 보내지 않았다.
