# S4 반품·회수 adversarial 지적 수정 (s4h-recall)

## 결론

branch `step3/s4h-recall`(baseline tag `step3-s4h-baseline` = `2e1d90cc`)에서
`/Volumes/VideoStore/Developer/.mulino-tools/s4review/recall.json`의
confirmed 10건 중 9건을 FIXED, 1건(커버리지 지적 5)을 부분 FIXED로 처리했다.
unverified P3 5건은 2건 FIXED, 3건은 검증 후 미수정이다. 각 수정에는 실제
PostgreSQL 회귀 test가 있다. 새 제품 test 12개는 baseline 코드에서
FAIL(clean build, 아래 RED)이고, ADMIN gating test는 baseline이 이미
맞으므로 mutation으로 RED를 확인했다.

같은 commit `48d29beb`, 같은 JAR(`213c4ae26b85…`)에서 native S4 suite·
E2·E1·C4·C1과 S3가 **PASS**다. E2 assertion은 410→414(새 거부 단계 1개),
suite는 1718→1722이고 나머지는 이전 기록(s4g-vocab)과 같다.

## commit

| commit | 내용 |
|---|---|
| `d1955ac5` | recall: 조사 단위 사실·처분 MANAGER 확인·권한 모든 값 검사·knownAt 조회, V31, schema 재생성 |
| `5f41ec2d` | returns: 관측 범위·같은 사건 중복 제거·중복 보고 확정·지연 도착·권한 모든 값·처분 증거 |
| `48d29beb` | native E2 author 입력(처분 MANAGER 권한, 거부 단계) |
| 이 기록 commit | 실행 기록 |

## 지적별 결과

계획 근거는 `docs/ontology-implementation-plan.md` §3.4, §4.3, §5.3, §6
(반품·회수 행과 "업무별 추가 규칙 반품·회수"), §7.1 표다.

| # | 지적 | 결과 | 수정과 test |
|---|---|---|---|
| C0 P1 | 재-scope가 앞 version의 실물 처리를 버리고 root 단위 DB 제약이 복구를 막는다 | FIXED | `RecallCommands`가 partition·종료·"처분 전 회수"를 조사 전체 action을 현재 scope로 자른 값(`RecallPartition.clip`)으로 계산한다. 같은 root 재회수는 SQL 오류가 아니라 `CONFLICT/RECALL_RANGE_ALREADY_RECOVERED`. 재-scope는 아직 열린 앞 version 제외 범위 의무와 겹치는 `RECALL_EXCLUDED_SCOPE`를 다시 만들지 않는다. 앞 version의 EXCEPTION은 조사 단위 최종 분류라 같은 범위의 두 번째 잔여 의무가 생기지 않는다. test `rescopeAfterRecoveryCarriesPhysicalFactsAndClosesTruthfully` |
| C7 P2 | version 사이 상충 최종 분류 | FIXED (C0과 같은 원인) | V31 `Actions.investigationId`와 조사 단위 `recall_terminal_disjoint`. test `disposalUnderEarlierVersionStaysDisposedAndBlocksContradictoryTerminalLabels`(v1 처분 0–25 뒤 v2 SAFE·CONSUMED_LOST → `HELD/RECALL_PARTITION_CONFLICT`) |
| C1 P1 | DISPOSED가 MANAGER 재고 감소 확인 없이 재고를 줄인다 | FIXED (부분, 아래) | DISPOSED는 기록자가 HUMAN이고 `disposeQuantity` capability와 현재 `disposeQuantity` ManagementAuthority를 모든 scope 값에 대해 가져야 한다. ADMIN 쪽은 기존 `approval()`의 현재 정확한 ADMIN scope 승인 재검사로 둔다. test `recallDisposalNeedsCurrentHumanManagerConfirmation`(권한 철회·권한 없는 인간 → `REJECTED/FORBIDDEN`, DISPOSE 이동 0) |
| C8 P2 | 회수해 보관 중인 범위를 CONSUMED_LOST/EXCEPTION으로 닫는다 | FIXED | 원장에 아직 남은 회수 실물(`RecallStockPrimitives.heldQuantity`) 위의 CONSUMED_LOST·EXCEPTION과 CONSUMED_LOST 위의 RECOVERED는 `HELD/RECALL_PARTITION_CONFLICT`. 원장에서 조정·폐기된 뒤에는 허용한다. test `consumedLostOrExceptionOverStillHeldRecoveredRangeIsHeld` |
| C3 P2 | recall 조회가 bitemporal이 아니다 | FIXED | `RecallQueries.known`이 status·currentScopeId·revision·closedAt을 knownAt까지의 불변 Scopes/Approvals/Closures에서 유도한다. 가변 포인터로 `require`하지 않는다. test `historicalRecallReadsUseStateKnownAtThatTime`(scope 전 knownAt: INVESTIGATING·목록 정상, 종료 전 knownAt: APPROVED·closedAt 없음) |
| C5 P2 | ADMIN gating·현실 회수 경로 미검증, 종료 거부 test가 공허 | 부분 FIXED | 두 번째 인간 operator로 EXCEPTION·closeRecall 거부(`nonAdminCannotRecordExceptionOrCloseRecall`), 기존 test에 유효한 RECALL_CLOSURE 원본으로 `RECALL_RESIDUAL_UNKNOWN` 단언 추가. 고객 leaf와 `returnId` 회수 경로 test는 만들지 않았다 |
| C2 P2 | 임시 반품이 승인 범위를 기록, 불일치·중복이 닫히지 않음, 지연 도착 기록 불가 | FIXED (미식별 접수 제외) | `receiveReturn` 임시 접수가 관측 범위(승인 범위 안)를 받고 승인 현재성은 확정 때만 본다. 확정에 같은 인도·범위를 덮는 더 최근 승인을 쓸 수 있다. 이미 받은 반품의 다른 보고는 그 receipt의 canonical로 확정해 의무만 닫는다(`TradeObservationRemedy.reconcileDuplicateReturnObservation`). test `partialArrivalRecordsObservedQuantityAndClosesItsDuty`, `otherReportsOfReceivedReturnCloseAgainstTheOneReceipt`, `lateArrivalIsRecordedAndConfirmedOnlyUnderCurrentAuthorization` |
| C9 P2 | 같은 사건 임시 관측이 중복되고 의무가 영원히 열림 | FIXED (C2와 같은 원인) | 같은 eventId는 같은 관측을 돌려주고 다른 범위는 `HELD/EVIDENCE_CONFLICT`. V31 unique `(organizationId,eventId)`. 이미 반환된 범위의 새 승인 관측은 위 중복 확정으로 닫힌다. test `sameReturnEventObservedTwiceCreatesOneObservationAndOneDuty`와 위 test의 세 번째 보고 |
| C4 P2 | 승인 manager·QC/ADMIN·수령 보관자 재검사가 값 하나만 맞으면 통과 | FIXED | `ReturnCommands.manager/every/everyPermitted`와 `ManagementCoverage`(차원의 모든 값을 덮어야 함). recall `admin()`의 ManagementAuthority도 같은 규칙. test `authorizingManagerIsRecheckedOnEveryPlaceOfTheReturn`(grant PLACE C만 → FORBIDDEN, authority PLACE C만 → FORBIDDEN, W 추가 → APPLIED) |
| C6 P2 | 처분 결정 증거가 반품 도착 시각으로 되돌려 적혀야 함, e2e 없음 | FIXED | `ReturnEvidence`가 `RETURN_DISPOSITION_*`를 확정된 반품에 묶고 decisionId를 occurrence identity, 결정 시각(도착 이후·knownAt 이전)을 effectiveFrom으로 쓴다. test `dispositionWithLaterDecisionDateRecordsDutyAndKeepsQcHold`(QC 권한 없으면 FORBIDDEN, 있으면 결정 1·`RETURN_RESALE` 의무·outbox `returnDispositionReview`·QC 보류 유지, 도착 전 결정 원본은 match 거부). native 단계는 추가하지 않았다 |
| P3-0 | EXCEPTION 잔여 의무가 지난 nextCheckAt을 물려받음 / 조사 의무가 종료 때 안 닫힘 | 앞부분 FIXED | EXCEPTION은 `nextAction`·`nextCheckAt` slot을 받을 수 있고 결과 nextCheckAt이 미래가 아니면 거부한다. test `exceptionResidualCarriesItsOwnFutureNextCheck`. `RECALL_INVESTIGATION` 의무 종료는 `ObligationClosureCatalog`(소유 밖)에 해소 경로가 없어 하지 않았다 |
| P3-1 | traceRecall leg event를 recall Work로 인가 | 실재 확인, 미수정 | 코드상 실재한다. test에 Shipment·Leg·Cargo·canonical 사슬이 필요하고 검증된 leg event를 직접 seed하지 않는다는 기준 때문에 이번에 고치지 않았다 |
| P3-2 | 종료 거부 test 공허 | FIXED | C5와 같다 |
| P3-3 | Investigations 가변·Approvals revision unique 없음 | 부분 FIXED | V31 `recall_approval_revision_once`. Investigations는 여전히 가변이지만 조회는 불변 행에서 유도한다(C3) |
| P3-4 | 미식별·무승인 임시 반품 접수 미구현 | 부분 | 승인 만료·정책 변경 뒤 도착은 기록된다(C2). 승인 없는 미식별 접수(T18)는 구현하지 않았다 |

## 검증

모든 Maven·verify는 `. /Volumes/VideoStore/Developer/.mulino-tools/env.sh` 뒤
`$MULINO_SLOT`로 하나씩 실행했다. Java 21, PostgreSQL container
`postgres@sha256:4ef4dbc9…`.

### backend 집중 test (`48d29beb`와 같은 backend source)

```bash
$MULINO_SLOT ./mvnw -B -ntp -q -f backend/pom.xml test -Dtest='RecallGatewayPostgresTest,ReturnGatewayPostgresTest,ReturnRangeContractTest,DomainVocabularyContractTest,S1ReadIntegrationTest,SalesCommandPostgresTest,ResponsibilityPostgresTest'
```

exit 0. RecallGatewayPostgresTest 13(새 7), ReturnGatewayPostgresTest
14(새 6), ReturnRangeContractTest 3, DomainVocabularyContractTest 4,
S1ReadIntegrationTest 8, SalesCommandPostgresTest 6,
ResponsibilityPostgresTest 27. failure·error·skip 0. 새 code/outcome 조합은
없어 `contracts/domain-vocabulary.json`은 바꾸지 않았다.

### RED (baseline 제품 코드 + 새 test)

`backend/src/main`, `backend/db`, V31을 baseline으로 되돌린 상태에서
`clean test -Dtest='RecallGatewayPostgresTest,ReturnGatewayPostgresTest'`.
새 test 12개가 FAIL했다(`raw/red-baseline-code.txt`). 재-scope test는 지적대로
`recall_recovery_disjoint` SQL 오류로 실패했다. 기존 test는 모두 통과했다
(Recall 13 중 FAIL 4·ERROR 2, Return 14 중 FAIL 6 = 새 test 12개). 그 전의
비-clean 실행은 `target/classes`에 남은 V31 때문에 무효로 버렸다.
`nonAdminCannotRecordExceptionOrCloseRecall`은 baseline이 맞으므로
EXCEPTION의 `admin()`과 closeRecall의 `admin()`을 각각 지운 두 mutation에서
FAIL(105행, 107행)을 확인하고 원복했다.

### schema parity

```bash
$MULINO_SLOT ./mvnw -B -ntp -q -f backend/pom.xml test -Dtest=S1ReadIntegrationTest   # 1866≠1865로 실패, artifact 기록
cp backend/target/s4-compatibility-observed.json docs/execution/s4h-recall/schema-observed-v31.json
python3 -I docs/execution/s4c-settlement/regenerate-schema-compatibility.py \
  docs/execution/s4h-recall/schema-observed-v31.json \
  2e1d90cc91c4eab182ad69d76bc691d486e4e82f "V1–V31(V30 미존재)"
# {"columns": 1866, "s3": 1432, "s4Added": 434, "timestampWidening": 340,
#  "notNullStrengthening": 788, "observedArtifactSha256": "d382a876…"}
```

새 열은 `mulino_trade_recall_actions.investigationid` 하나(CDS nullable, DB
NOT NULL → strengthening)다. 생성 script의 REVIEWED에 사유를 추가했고
S1ReadIntegrationTest의 S4 delta guard를 433→434로 바꿨다. 재실행 8/8 PASS.
V30은 이 branch에 없다(다른 worker 배정). Flyway는 번호 공백을 허용한다.

### native

```bash
$MULINO_SLOT python3 verification/actual/s4/build.py
$MULINO_SLOT ./verify actual-s4 /tmp/s4h-recall-suite-1
ACTUAL_FLOW_REF=verification/actual/s4/<x>-flow.json \
  $MULINO_SLOT ./verify actual-s4 /tmp/s4h-recall-<x>-1      # e2 e1 c4 c1
$MULINO_SLOT python3 verification/actual/s3/build.py
$MULINO_SLOT ./verify actual-s3 /tmp/s4h-recall-s3-1
```

| run | commit | JAR | flow | 결과 | assertions |
|---|---|---|---|---|---|
| suite-1 | `48d29beb` | `213c4ae26b85` | s4 flow.json | PASS | 1722 |
| e2-1 | `48d29beb` | `213c4ae26b85` | s4 e2-flow.json | PASS | 414 |
| e1-1 | `48d29beb` | `213c4ae26b85` | s4 e1-flow.json | PASS | 612 |
| c4-1 | `48d29beb` | `213c4ae26b85` | s4 c4-flow.json | PASS | 392 |
| c1-1 | `48d29beb` | `213c4ae26b85` | s4 c1-flow.json | PASS | 304 |
| s3-1 | `48d29beb` | S3 build | s3 flow.json | PASS | 594 |

receipt·report hash는 `raw/native-runs.json`에 있다. 모든 receipt는
`gateComplete=false`이며 coverage PASS가 아니다. E2의 새 단계
`e2-dispose25-without-manager-denied`는 `REJECTED/FORBIDDEN`으로 관찰됐다.
입력은 `python3 -I verification/actual/s4/e2-author.py`로만 재생성했다.

## NOT_RUN과 남은 일

- NOT_RUN: 전체 backend suite와 harness suite(coordinator 담당), actual-s2·
  actual-s1, coverage 조립.
- 별도 typed `RECALL_DISPOSITION_CLEARANCE` ADMIN 결정은 없다. 처분은 현재
  정확한 ADMIN scope 승인 + 기록자 MANAGER 확인으로 막는다. Step 2 E2/T18
  계약의 `disposeQuantity`+`dispositionBasisId`+`recallDispositionDecisionId`
  모양과 다르다.
- 고객 leaf·`returnId` 회수 경로, traceRecall leg event 인가(P3-1),
  `RECALL_INVESTIGATION` 종료 시 해소, 승인 없는 미식별 반품 접수(T18),
  틀린 반품 관측을 닫는 `RETURN_RECONCILIATION` 면제 경로, 중복 확정 관측을
  `ReturnQueries`가 PROVISIONAL로 보이는 문제는 남았다.
- 처분 결정의 native 단계는 추가하지 않았다(backend PG test만).

## 소유 밖 변경 (coordinator 확인 필요)

- `backend/src/main/java/com/mulino/application/trade/TradeObservationRemedy.java`:
  `reconcileDuplicateReturnObservation` method 추가(기존 method 불변).
  `Completion` 생성자가 private이라 returns 패키지 안에서 의무를 닫을 수 없었다.
- `backend/src/test/java/com/mulino/core/S1ReadIntegrationTest.java`: S4 delta
  433→434. `docs/execution/s4-integration/schema-compatibility.json`,
  `docs/execution/s4c-settlement/regenerate-schema-compatibility.py`(REVIEWED 1건).
  다른 worker가 열을 추가하면 수와 재생성 결과가 다시 바뀐다.
