# 의무 원천과 후속 Work 관계의 독립 관찰

Task/S5/S6는 OPEN이다. 이 변경은 실제 시나리오에서 빠진 관찰 필드를
실제 저장 원천에 연결한다. baseline은
`fb58d86663f28ec90dd38322399c384a42de4672`이고 branch는
`fix/t17-source-occurrence`다. 작업 tree는
`/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-b370b5a3`다.
위임된 수정 runtime은 `codex_2/gpt-6.1-sol/xhigh/priority`다.
catalog에서 provider/model과 `reasoningEffort=xhigh`,
`serviceTier=priority`의 지원을 확인했다.

소유 범위는 ScenarioJdbcObservation, ScenarioObservationProjection의
해당 projection, observation-sources.json, 새 focused PG 회귀와 이
문서다. 추가 지시에 따라 T11 후속 Work의 parent 관계를 포함한다.
Backend, AssertionEngine, SourceFacts, case, fixture와 goal은 바꾸지
않는다. root와 다른 writer의 tree도 수정하지 않는다.

## sourceOccurrenceId의 정확한 의미

계획 §5.3은 의무의 원천을 `sourceOccurrence/decision`으로 정의한다.
실제 제품의 ResponsibilityRepository.requireSource와
ResponsibilityService.recordDuty는 종류가 있는 원천 ID와 발생 버전을
저장한다. 따라서 관찰의 `sourceOccurrenceId`는 이 **typed causal
source의 실제 row ID**다. 같은 행의 `sourceKind`, `sourceId`,
`sourceVersion`과 함께 해석한다. DECISION CommandRecord의 ID를 Event
또는 CanonicalOccurrence의 ID로 표시하지 않는다.

`scopeJson.domainSourceId`는 원천 command와 별개의 업무 대상이다.
Delivery나 Investigation ID를 sourceOccurrenceId에 대신 넣지 않는다.
요청 scope, expected, case alias, 임의 UUID나 문자열은 source가 아니다.
원천의 존재는 사건의 검증, 허가, 적격량 또는 목표 기여의 판정이 아니다.

| root sourceKind | 실제 source table | 발생 버전 | knownAt의 시간 경계 |
|---|---|---|---|
| OCCURRENCE | evidence CanonicalOccurrences | revision | createdAt, recordedAt |
| EVENT | evidence Events | revision | createdAt, recordedAt |
| CLAIM | evidence Claims | revision | createdAt, recordedAt |
| DOCUMENT | evidence DocumentVersions | revision | createdAt, recordedAt |
| DECISION | commands Approvals | canonicalHash | decidedAt |
| DECISION | commands CommandRecords | canonicalHash | createdAt, completedAt |
| WORK | work.read Works | 초기 계약의 `1` | createdAt, recordedAt |

DECISION의 두 대안 중 실제 같은 조직/ID의 행이 정확히 하나여야 한다.
CommandRecord는 COMMITTED여야 한다. Approvals에는 createdAt나
recordedAt가 없으므로 그런 필드를 가정하지 않는다. 다른 source kind는
각자 테이블만 읽고 모든 의무에 Event/CanonicalOccurrence/Approval
연결이 있다고 가정하지 않는다.

V34의 WORK_NEXT_ACTION은 Work 자체가 원천이고 생성 당시 계약 버전은
`1`이다. 활성화 후 mutable Work revision을 초기 sourceVersion과
비교하지 않는다. 실제 Work ID, root kind, scopeJson.workId와 시각을
검증한다. 다른 kind를 이 특례로 통과시키지 않는다.

## 동일 snapshot의 검증과 derivation

기존 read-only REPEATABLE_READ connection에서 아래 순서로 읽는다.

1. 원래 ObligationReferences를 기존 scope/knownAt 조건으로 읽는다.
   `current=valid`, responsibleWorkId=workId, obligationId=rootId는
   그대로다. status는 current와 독립이다. RESOLVED/WAIVED의
   current=true를 유지한다. C4/E1의 유효한 해소 행을 숨기지 않는다.
2. 실제 organizationId/rootId로 Roots를 읽는다. 유일한 행의
   createdAt/recordedAt가 knownAt 이내여야 하고 kind와 scopeJson은
   assignment에 일치해야 한다. 기존 sourceKind/sourceId 원필드는
   유지하며, 없던 alias만 실제 root에서 읽는다.
3. root sourceKind에 맞는 실제 source table에서 같은 조직/ID의 유일한
   행을 읽고 위 표의 시간과 발생 버전을 검증한다.
4. 모든 근거가 성립하면 sourceOccurrenceId=실제 source row ID와
   sourceVersion=검증된 root 발생 버전을 추가한다.

sourceOccurrenceProof에는 실제 root와 source row, source table을
남긴다. 모든 SQL/바인딩은 sourceEvidence의 sourceQuery에 남는다.
실패하면 명시적 gap을 남기고 sourceOccurrenceId를 생략한다. 원래
obligation 행, rootId, sourceKind/sourceId, valid/current/status를
제거하거나 바꾸지 않는다.

시간은 source의 저장/기록 경계인 knownAt를 검증한다. 이 projection은
mutable source의 과거 asOf 상태를 복원하거나 wall-clock commit 시각을
발명하지 않는다. PostgreSQL의 실제 commit 가시성은 같은 MVCC
snapshot으로 제한한다.

두 T17 domain hook은 추가로 다음 원천을 대조한다.

- DELIVERY_RESTRICTION_RESPONSE는 실제 Delivery의 ID/Work/rangeRoot,
  ITEM/LOT와 root의 domainSourceId/originalWorkId/physicalScopeId가
  일치해야 한다. 실제 recordDelivery command의 canonical slots는
  observationId를, APPLIED result의 commandId/effects.deliveryId는
  그 command와 Delivery를 가리켜야 한다.
- RECALL_INVESTIGATION은 같은 구조로 실제 Investigation과
  openInvestigation의 Work/ITEM/LOT/segment slots 및
  effects.investigationId를 대조한다.

DELIVERY_RECONCILIATION의 ACCEPTED_PENDING_EXTERNAL command도 실제
COMMITTED 원천으로 읽는다. 이는 미확인 보고의 책임 원천이며, 인도
확인이나 canonical 검증을 뜻하지 않는다. expiry는 같은
DELIVERY_RESTRICTION_RESPONSE 계약을 따른다.

## 읽기 전용 실제 실패 자료

원본 자료는 아래 tree의 `.local`에서 읽기만 했다.

`/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-db3fa28c/.local/t17-formal-recall-f747b281-b4be-464c-bfd2-6c24af9a7682/`

| 원본 artifact | SHA-256 |
|---|---|
| after-delivery-db.json | `8a16ba27710b49ad40ee98c79acc164a1bd78172ea0c670b089107ddf303c21c` |
| formal-recall-domain-proof.json | `71567fe4475ac84f3b753a67cccb55efd42e953288e4fc38669656072d94b289` |
| focused-result.json | `cc549906f91768d6a6cdeca62705bf45419b4442d1e7d930e69c90a350434d7b` |

원본 DELIVERY_RESTRICTION_RESPONSE root
`9dfe912f-9e62-4629-98d2-bc5bf9f28553`의 DECISION source는
`ca501ea3-da67-4df3-b128-0e0fb52f8752`이고 발생 버전은
`3ce055d2ad92f6cb53a7492fa95607b0955ea4e57e30e5985624aa4a6c610b8b`다.
domainSourceId는 Delivery
`f403eab5-d441-4ab9-a68e-e3ecc702338a`다.

RECALL_INVESTIGATION root
`2c34b11c-7e13-4c48-aa4b-82cf203679b1`의 DECISION source는
`2f954531-efdd-4d5c-9c63-3401a5b296ff`이고 발생 버전은
`18c98f940141addb1dd46484f5103083e967c7931bfb93dbfef6d14953a646ae`다.
domainSourceId는 Investigation
`dace4ce6-363e-48f9-840e-3f9930d5a619`다.

이 원본의 26 oracle 중 21 PASS/5 FAIL과 146 focused checks는 원본
실행 결과로 보존한다. 원본 artifact를 수정하거나 이번 검사의 결과로
재표기하지 않는다.

## T11 parentWorkId의 실제 관계

제품 schema에는 FollowupLinks 테이블이 없다. V9와 WorkLifecycle.link는
`mulino_work_WorkLinks(kind=FOLLOWUP)`에
`sourceWorkId=원 Work`, `targetWorkId=후속 Work`를 저장한다.

같은 조직의 targetWorkId가 관찰 중인 child이고 createdAt가 knownAt
이내인 FOLLOWUP 링크가 정확히 하나여야 한다. 실제 parent Work도
같은 조직/ITEM에 존재하고 parent와 child의 createdAt/recordedAt가
knownAt 이내여야 한다. parentWorkId는 이 실제 link.sourceWorkId다.
요청한 parent ID, converse 링크, 다른 관계 kind를 복사하지 않는다.

followupParentProof에 실제 link와 parent row를 남기고 query/bindings를
같은 sourceQuery에 기록한다. foreign/future/dangling/self/다른 ITEM,
중복 또는 다중 parent는 필드를 생략한다. Work 행과 CLOSED 원 Work의
상태는 변경하지 않는다.

## 한정 검증과 원본 보존

자체 npm 설치와 CDS 생성, JDK21 backend JAR build를 수행했다.
다른 tree의 compiled 산출물을 복사하지 않았다. Docker는 명시적으로
`unix:///Users/se0nghe0n/.orbstack/run/docker.sock`을 사용했다.
전용 PostgreSQL 18.4 container는 `obligation-source-b370-pg`이고
테스트마다 고유 `obs_<UUID>` schema를 만들고 제거했다.

새 PG 회귀는 원래 T17의 late 두 개, expiry 하나, claim 다섯 개의
fieldsPresent assertion을 case.json에서 그대로 읽어 실제 JDBC 관찰에
적용한다. T11 followup-parent-link assertion도 그대로 실행한다.
이는 CSN 형태의 독립 PG fixture에서 수행한 projection 회귀다.
전체 scenario 또는 제품 command 실행 인수로 세지 않는다.

foreign root/command/domain/parent, 누락·dangling source, 발생 버전
불일치, 미래 root/source/domain/link, source/child 모호성, 잘못된
명령 effect/slot/capability, raw source kind별 실제 필드, RESOLVED와
current=true, 동시 commit 뒤 snapshot 고정과 read-only 쓰기 거부를
검사한다.

실제 JDK는 OpenJDK `21.0.12.1`, Maven wrapper는 `3.9.16`,
Node는 `24.16.0`, CDS/compiler는 package-lock의
`10.1.1`/`7.1.1`이다. 자체 backend JAR build는 exit0이며 SHA-256은
`6895578d89c5743551bb4e3b82bc0fc8e285fbb268ae9344462d162cac93a456`다.

다음 결합 검사는 exit0이다. 166개 중 165개가 통과했고 실패/오류는
0개다. 기존 선택 replay
`goalInputProjectionReplaysCapturedImmutableRowsInFreshPostgres`는
`T09_GOAL_INPUT_SOURCE`가 없어 1개 미실행이다. 새 의무 원천 42개와
후속 관계 15개는 skip 없이 통과했다.

```sh
JAVA_HOME=$(/usr/libexec/java_home -v 21) \
OBSERVER_PG_TEST=true \
OBSERVER_PG_URL=jdbc:postgresql://127.0.0.1:32865/observer \
OBSERVER_TEST_EVIDENCE="$PWD/.local/obligation-source-occurrence/captures-final" \
DOCKER_HOST=unix:///Users/se0nghe0n/.orbstack/run/docker.sock \
./mvnw -B -ntp -o -f verification/harness/pom.xml \
  -Dtest=ObligationSourceOccurrencePostgresTest,FollowupParentProjectionPostgresTest,ScenarioObservationProjectionTest,ScenarioObservationPostgresTest,ObservationSourceRelationsPostgresTest \
  test
```

테스트 입력 command의 canonicalHash도 그 canonical JSON의 실제
SHA-256으로 만들도록 강화한 뒤 의무 원천 42개를 다시 실행해
exit0/42 통과를 확인했다. 이는 원본 fixture 수정이 아니라 새 회귀의
독립 입력 구성이다. `combined-pg.log`, `sourcehash-pg.log`, `reports/`,
`focused-check-summary.json`과 capture는
`.local/obligation-source-occurrence/`에 보존한다. 이 summary는
Surefire XML/hash/byte 비교를 수집한 한정 검사 기록이며 coverage
receipt나 runtime-manifest를 대신하지 않는다.

변경한 Java 2개, mapping과 새 회귀 2개를 경로 순으로 정렬한
`{path: sha256}` JSON의 compact bytes에 대한 source SHA-256은
`e3eb35f38f40e47afe2bfc30f339b5fd96be2358a27cb632c2fcdac7c3ad1163`다.
개별 파일 hash도 focused-check-summary에 있다.

T09/T11/T17의 추적 파일 145개가 baseline Git blob과 byte 동일함을
확인했다. 원본 SHA-256 inventory hash는
`c85441aecd9f2723f601b69520f99c38af429f905ca59f2f74db68f08a1991a8`다.
`git diff --check`는 exit0이다. 남은 `obs_*` schema가 0개임을 확인한
뒤 자체 container와 volume을 제거했다. 다른 tree·process·container는
정리하지 않았다.

새 runtime scenario, 전체 coverage, 실모델과 배포 gate는 NOT_RUN이다.
별도 caseworker의 T11 baseline/cancel pointer와 policyworker의
EVIDENCE/goal binding, root 소유의 current=true exact-kind 공백은
이 변경의 완료 범위가 아니다.
