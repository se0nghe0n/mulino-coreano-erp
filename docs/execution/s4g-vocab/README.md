# 도메인 어휘 계약, 오류 이름 정규화, 관찰자 snapshot (s4g-vocab)

## 결론

branch `step3/s4g-vocab`(baseline tag `step3-s4g-baseline` = `de8d19ea`)에서
backend가 내보내는 outcome·오류 code·의무 kind를 하나의 계약
[`contracts/domain-vocabulary.json`](../../../contracts/domain-vocabulary.json)
(+[설명](../../../contracts/domain-vocabulary.md))으로 고정했다. 계획 §3.4와
어긋난 이름(`REVISION_CONFLICT`, `QUANTITY_CONFLICT`, 적격량 부족의
`SCOPE_INELIGIBLE`)을 계획 이름으로 바꿨고, `PENDING_EXTERNAL`을 지웠다.
`emergencyReassign`은 승인 행 없이 지정 ADMIN의 별도 명령으로 실행된다.
native 관찰자는 자기 `pg_current_snapshot()`과 `readMode`를 낸다.

같은 commit `28daf5f7`, 같은 JAR(`ba9404976138…`)에서 native S4 C1·E2·C4·E1
flow와 기본 suite, S3, S2가 **PASS**이고 assertion 수는 이전 기록과 같다.
native S1은 **FAIL**이다. 기준 `de8d19ea`에서도 FAIL이었고(아래 "S1"),
fixture를 고친 뒤에는 제품과 S1 oracle의 다른 불일치에서 멈춘다.

## 근거와 결정

- 계획 §3.4: outcome `APPLIED|WAITING_APPROVAL|NEEDS_INPUT|REJECTED|CONFLICT|
  ACCEPTED_PENDING_EXTERNAL`, 오류는 `TYPE_INVALID` 등 8개 "처럼" 구조화한다.
- 계획 §4.2: 한정 재시도 뒤 revision 충돌은 새 의도로 몰래 실행하지 않고
  conflict를 돌려준다. → revision·대상 교체는 `CONFLICT/STALE_REVISION`.
- 계획 §4.2: 행동별 적격량·미예약 적격량. → 요청 구간이 현재 적격·미예약
  범위에 완전히 들지 않거나 배분이 정지됐으면
  `REJECTED/INSUFFICIENT_ELIGIBLE_QUANTITY`. Step2 oracle(V2·V3·E2·C1·T05·
  V1·T16)도 같은 짝을 기대한다.
- 계획 §8 "미지원 과거 evaluator는 … 효과 없이 보류", §7.1 "policy 미설정
  행동은 효과 없이 차단하고 필요 확인을 반환한다". → `HELD`는 판정 근거가
  아직 없는 fail-closed 보류이고 현재 책임을 유지한다. 계획 6개 outcome의
  확장으로 표시했다.
- `PENDING_EXTERNAL`: backend가 만든 적이 없고 `ApplicationCommands` 허용
  목록과 schema enum에만 있었다. 계획의 외부 미확정 outcome은
  `ACCEPTED_PENDING_EXTERNAL` 하나라 삭제했다.
- 계획 §5.3 "긴급 재배정은 ADMIN의 별도 명령·사유·감사로 수행한다", §7.1 표
  "책임 인계·긴급 재배정 | 기존 owner+인수자 / ADMIN | 인계 수락 또는 긴급
  재배정 사유". → 승인 행이 필요 없다. 이전 구현은 만들 수 없는
  `EMERGENCY_REASSIGN` 승인을 요구해 실행 경로가 없었다.

### 어휘 계약과 정적 검사

`DomainVocabularyContractTest`가 main source를 읽어 양방향으로 대조한다.

- 모든 `new DomainError(outcome, code, …)`·`new ResponsibilityHeld(code, …)`의
  code·outcome 조합(삼항 포함)이 목록에 있고, 목록의 code는 실제로 emit된다.
  code 80개, 각 항목에 의미와 `planRef` 또는 `extension` 근거가 있다.
- `CommandOutcomes.ALL/NOT_APPLIED`(새 registry), `command-response.schema.json`
  enum, source의 literal `"outcome"` 값(검증 `VALIDATED`, 복구 내부 `BUSY`·
  `STALE_EXECUTION` 포함), 증거 RECORD의 `evidenceStatus` 집합이 목록과 같다.
- `ObligationClosureCatalog` kind 32개(`RETURN_<decision>` 5개를 catalog에
  올림)의 면제 권한·해소 증거·자동 종료가 목록과 같다. `recordDuty`는
  catalog 밖 kind를 `REJECTED/TYPE_INVALID`로 거부해 `createObligation`이
  kind를 지어낼 수 없다.
- `@Profile("platform-spike")` S0 spike와 MCP JSON-RPC protocol 오류는
  도메인 계약이 아니어서 제외 목록에 이유와 함께 둔다.

변이 확인: JSON에서 `SNAPSHOT_CHANGED`와 `RETURN_REFUND`를 지우면 두 test가
실패했다(`undeclared code SNAPSHOT_CHANGED at application/core/
ApplicationQueries.java:85`, kind 집합 불일치). 원본을 복원했다.

같은 code가 두 outcome을 갖는 경우는 의미로 구별해 `byOutcome`에 적었다.
`POLICY_UNRESOLVED`(HELD: owner가 남는 명령 정책 미정 / REJECTED: intake
owner·profile이나 guard가 없어 맡길 책임자가 없음), `EVIDENCE_CONFLICT`
(CONFLICT: 대체된 근거를 가리킴 / HELD: 유효 원천끼리 상충),
`EVIDENCE_UNVERIFIED`(HELD: 증거 대기 / REJECTED: canonical 검사 미완료 기록).

### 바꾼 code (호출 위치)

경로 기준은 `backend/src/main/java/com/mulino/`. 줄 번호는 `945c67f5` 기준이다.

| 이전 | 이후 | 위치 |
|---|---|---|
| CONFLICT/REVISION_CONFLICT | CONFLICT/STALE_REVISION | `application/work/WorkLifecycle.java:38` |
| REJECTED/REVISION_CONFLICT | CONFLICT/STALE_REVISION | `application/inventory/InventoryCommands.java:56`, `application/inventory/FulfillmentCommands.java:24,25`, `domain/inventory/StockPrimitives.java:17` |
| REJECTED/REVISION_CONFLICT | REJECTED/TYPE_INVALID | `domain/inventory/StockPrimitives.java:18` (발생 시각 < segment validFrom) |
| REJECTED/REVISION_CONFLICT | CONFLICT/STOCKTAKE_ALREADY_APPLIED | `application/inventory/InventoryCommands.java:74`, `domain/inventory/StockPrimitives.java:76` |
| REJECTED/REVISION_CONFLICT | REJECTED/ALLOCATION_UNRESOLVED | `domain/inventory/StockPrimitives.java:69` |
| REJECTED/SCOPE_INELIGIBLE | REJECTED/INSUFFICIENT_ELIGIBLE_QUANTITY | `application/inventory/FulfillmentCommands.java:30,35`(정지·판매·출고 3곳), `application/quality/QualityEligibility.java:77,80` |
| REJECTED/SCOPE_INELIGIBLE | CONFLICT/STALE_REVISION | `application/quality/QualityEligibility.java:78` |
| REJECTED/QUANTITY_CONFLICT | REJECTED/INSUFFICIENT_ELIGIBLE_QUANTITY | `application/inventory/FulfillmentCommands.java:31` |
| REJECTED/QUANTITY_CONFLICT | REJECTED/SALES_LINE_QUANTITY_EXCEEDED | `application/inventory/FulfillmentCommands.java:32` |

`SCOPE_INELIGIBLE`은 수량이 아닌 범주 불일치(배분 행동·고객, 창고 보관
확인, 수령·반품 보관자 actor·권한) 6곳에만 extension으로 남겼다.
판매 행 남은 주문량 초과는 적격량 부족이 아니라서 별도 code로 나눴다.

따라 바꾼 test와 native 입력:

- `FulfillmentPostgresTest.v2SplitFirstRejectsStaleParentReserveAndPreservesExisting40Once`:
  `REJECTED` → `CONFLICT` + `STALE_REVISION`(V2 oracle과 같다). 첫 실행에서
  이 단언 하나가 실패해 원인을 확인하고 바꿨다.
- `QualityPostgresTest` 만료 sweep 단언: `INSUFFICIENT_ELIGIBLE_QUANTITY`.
- `verification/actual/s4/c1-author.py`(3곳), `e2-author.py`(1곳)의 기대 code를
  바꾸고 `c1-sale-revocation.json`, `e2-flow.json`을 재생성했다. 수정 전
  재생성이 committed 파일과 byte 동일함을 먼저 확인했다.

### emergencyReassign과 면제 결정

- `ResponsibilityCommands`: 승인 action을 없앴다. 실행 시 현재 HUMAN, WORK
  scope의 `emergencyReassign` capability, `ManagementAuthorities` 지정을
  모두 요구한다(waiver 결정과 같은 `designated` 검사).
- `ResponsibilityService.emergency`: slot은 C3 계약 이름(`workId`,
  `newOwnerId`, `reason` 필수, `obligationId`·`effectiveAt`·`nextAction`·
  `nextCheckAt` 선택)이다. 미래 effectiveAt, 같은 owner, 업무에 없는
  obligationId는 TYPE_INVALID다. work owner와 열린 assignment owner를 한
  거래로 바꾸고 basis에 사유를 남기며 `handoverAccepted=false`다.
  응답 top-level `workId`로 감사 effectRefs에 대상이 남는다.

새 PostgreSQL test(실제 `PolicyCommandGuard`, `FulfillmentPostgresTest`):

- `qcAndAdminDutyWaiversSucceedOnlyForTheirDesignatedAuthorityAndRevokedDesignationVoidsTheDecision`:
  catalog 밖 kind의 createObligation은 TYPE_INVALID. `RETURN_QC_REVIEW`는
  ADMIN·MANAGER 결정이 FORBIDDEN이다. 지정 QC가 APPROVE한 뒤 그 지정을
  철회하면 waive는 `REJECTED/FORBIDDEN`, 의무 OPEN, 소비 0이고, 철회된 QC의
  새 결정도 거부된다. 지정을 다시 주면 새 결정으로 WAIVED, 소비 1, 승인
  행의 approver·capability·action이 맞다. `RECALL_INVESTIGATION`은 QC 결정이
  거부되고 지정 ADMIN 결정으로 WAIVED다.
- `emergencyReassignIsADesignatedAdminCommandWithReasonAndAuditAndNoApproval`:
  capability만 있는 owner는 FORBIDDEN, 사유 누락 TYPE_INVALID, 틀린
  expectedRevision `CONFLICT/STALE_REVISION`이고 owner는 그대로다. 지정
  ADMIN은 APPLIED, work와 열린 의무의 owner가 바뀌고 의무 revision+1,
  basis `EMERGENCY: <사유>`, ADMIN 감사 1행, 승인 0행, Handover 0행이다.

한계: 두 의무는 실제 `activateWork` 명령 기록을 DECISION 원천으로 공개
`createObligation` 명령이 만든다. `receiveReturn`·회수 명령이 만든 의무로
면제한 것은 아니다. 그 gateway test들은 `PolicyCommandGuard`를 mock해
면제 guard를 실제로 실행할 수 없다.

### native 관찰자 snapshot

- `ObserverSnapshot`(새 helper): snapshotRef 없음은 `CURRENT_COMMITTED`,
  `CURRENT_COMMITTED`·`CURRENT_LOCK_WAIT`는 그 mode다. 관찰 연결의 첫 문장
  `pg_current_snapshot()`이 `snapshot.id`이고 `readMode`를 같이 낸다.
  요청 참조나 directive와 같은 token은 거부한다.
- 해석된 제품 `snapshotRevision`(RESULT_REVISION)은 연결 전에
  NOT_IMPLEMENTED다. 제품 token은 `ApplicationQueries.snapshot`이
  조직·actor·시점·scope와 인가된 projection 전체를 SHA-256한 값이라,
  관찰자가 제품 projection을 복제하지 않고는 독립 재계산할 수 없다.
  token을 복사하지 않는다.
- 이전에는 S1·S2가 snapshotRef가 있으면 무조건 미구현이었고, S3·S4는
  snapshotRef를 보지 않아 제품 token 요청에도 자기 token을
  snapshotRevision으로 냈다. 이제 네 관찰자가 같은 helper를 쓴다.
- native S4 C1 관찰 8건에서 `readMode: CURRENT_COMMITTED`를 확인했다.

## commit

| commit | 내용 |
|---|---|
| `945c67f5` | 어휘 계약·정적 test, code 정규화, kind 폐쇄, emergencyReassign, PG test, native 입력 |
| `28daf5f7` | native 관찰자 snapshot·readMode, harness test |
| `52024c26` | S1 native fixture에 위임자 supervisor 설치 |
| 이 기록 commit | 실행 기록과 raw 증거 |

## 실행 명령과 결과

모든 Maven·verify는 `. /Volumes/VideoStore/Developer/.mulino-tools/env.sh` 뒤
`$MULINO_SLOT`로 하나씩 실행했다. Java 21, PostgreSQL 18 container
(`postgres@sha256:4ef4dbc9…`).

### backend 집중 test (`945c67f5`와 같은 source)

```bash
$MULINO_SLOT ./mvnw -B -ntp -q -f backend/pom.xml test -Dtest='DomainVocabularyContractTest,FulfillmentPostgresTest,QualityPostgresTest,ReturnGatewayPostgresTest,RecallGatewayPostgresTest,SettlementCommandPostgresTest,ReceiptGatewayPostgresTest,S1ReadIntegrationTest,CommandTransactionTest,ResponsibilityServiceTest,ResponsibilityPostgresTest,ResponsibilitySubjectBindingTest,ResponsibilityEvidenceGateTest,InventoryPostgresTest,S2WorkLifecyclePostgresTest,SalesCommandPostgresTest'
$MULINO_SLOT ./mvnw -B -ntp -q -f backend/pom.xml test -Dtest='FulfillmentPostgresTest'   # V2 단언 수정 뒤
$MULINO_SLOT ./mvnw -B -ntp -q -f backend/pom.xml test -Dtest='CommandSubjectTest,PurchaseCommandPostgresTest,S2WorkActualIntegrationTest,S3RegulatoryActualTest,StockCommandPostgresTest,TradeResidualRemedyTest,InventoryQuantityTest,S4PinnedQuantityTest'
```

첫 명령은 153건 중 1건 실패(위 V2 단언), 나머지는 통과했다. 최종 24개 class
219건 failure·error·skip 0이다. class별 수는 `raw/checks.json`에 있다.
FulfillmentPostgresTest 21, ResponsibilityPostgresTest 27,
PurchaseCommandPostgresTest 22, ReceiptGatewayPostgresTest 14,
StockCommandPostgresTest 14, CommandTransactionTest 10, QualityPostgresTest 10,
ReturnGatewayPostgresTest 8, S1ReadIntegrationTest 8,
SettlementCommandPostgresTest 7, RecallGatewayPostgresTest 6,
DomainVocabularyContractTest 4 등이다.

### harness test (`28daf5f7` source)

```bash
$MULINO_SLOT ./mvnw -B -ntp -f verification/harness/pom.xml test
```

43개 report, 464 tests / 0 failures / 0 errors / 0 skipped
(`ObserverSnapshotTest` 3건 포함).

### native (`28daf5f7`, S1 재시도는 `52024c26`)

```bash
$MULINO_SLOT python3 verification/actual/s4/build.py
ACTUAL_FLOW_REF=verification/actual/s4/<x>-flow.json \
  $MULINO_SLOT ./verify actual-s4 /tmp/mulino-s4g-<x>-1      # c1 e2 c4 e1
$MULINO_SLOT ./verify actual-s4 /tmp/mulino-s4g-suite-1
$MULINO_SLOT python3 verification/actual/s3/build.py
$MULINO_SLOT ./verify actual-s3 /tmp/mulino-s4g-s3-1
$MULINO_SLOT python3 verification/actual/s2/build.py
$MULINO_SLOT ./verify actual-s2 /tmp/mulino-s4g-s2-1
$MULINO_SLOT ./verify actual-s1 /tmp/mulino-s4g-s1-1
```

| attempt | commit | JAR sha256 | flow | 결과 | assertion | 첫 실패 |
|---|---|---|---|---|---|---|
| `c1-1` | `28daf5f7` | `ba9404976138` | s4 c1-flow.json | PASS | 304 | - |
| `e2-1` | `28daf5f7` | `ba9404976138` | s4 e2-flow.json | PASS | 410 | - |
| `c4-1` | `28daf5f7` | `ba9404976138` | s4 c4-flow.json | PASS | 392 | - |
| `e1-1` | `28daf5f7` | `ba9404976138` | s4 e1-flow.json | PASS | 612 | - |
| `suite-1` | `28daf5f7` | `ba9404976138` | s4 flow.json | PASS | 1718 | - |
| `s3-1` | `28daf5f7` | `ba9404976138` | s3 flow.json | PASS | 594 | - |
| `s2-1` | `28daf5f7` | `ba9404976138` | s2 | PASS | 48 | - |
| `s1-baseline-1` | `de8d19ea` | `c97176e24149` | s1 | FAIL | - | Inventory HTTP must succeed (403 FORBIDDEN) |
| `s1-1` | `28daf5f7` | `ba9404976138` | s1 | FAIL | - | Inventory HTTP must succeed (403 FORBIDDEN) |
| `s1-2` | `52024c26` | `bc6bba6585aa` | s1 | FAIL | - | Missing SELL eligibility must remain unknown |

S4·S3 assertion 수는 s4f-duties 기록(C1 304, E2 410, C4 392, E1 612, suite
1718, S3 594)과 같다. 기대값을 줄이지 않았다. C1 native 원행에서
`INSUFFICIENT_ELIGIBLE_QUANTITY` 8회, E2에서 4회 관찰했다. `raw/<attempt>/`에
`run-receipt.json`, `actual-sN-native.json.gz`, `native-stdout.json.gz`를
보존했다(gzip mtime 0, bearer token·private key 문자열 없음). `raw/index.json`이
위 표의 기계 판독 원본이다. 같은 backend source인 `52024c26`의 JAR hash가
`28daf5f7`과 다르다. JAR build가 byte 재현되지 않는다.

### S1

- 기준 `de8d19ea`에서 baseline 자체를 build해 같은 명령으로 실행했고
  getInventory가 403이었다(`s1-baseline-1`). 이번 변경의 회귀가 아니다.
- 원인: reader grant의 `delegatorAlias: supervisor`가 fixture `actors`에 없어
  membership·capability·grant 없는 Human alias로만 설치됐다.
  `IdentityAuthorization`은 위임자도 같은 capability의 현재 권한을 가져야
  위임을 인정한다. `52024c26`에서 S2 fixture처럼 supervisor actor를 추가했다.
- 그 뒤 S1은 `eligibleQuantity`에서 멈춘다. 제품은 SELL 판정 근거가 없을 때
  `eligibleQuantity: "0"`, `eligibilityStatus: "UNKNOWN"`, unknowns 6개를
  돌려준다. native S1 oracle은 null을 요구하고, backend
  `S1ReadIntegrationTest`는 `"0"`+`UNKNOWN`을 단언한다(`InventoryPostgresTest`는
  다른 경로에서 null). AGENTS.md "미확인은 정상이나 수량0으로 바꾸지 않는다"는
  null 쪽이다. 두 oracle이 충돌해 제품이나 단언을 바꾸지 않았다. 결정이
  필요하다.

## 다른 소유자에게 넘기는 불일치

Step2 case oracle과 vocabulary의 code·outcome 짝을 기계 대조했다(case를
수정하지 않았다).

- outcome이 다르다: T01 `CAPABILITY_UNSUPPORTED`를 REJECTED로(제품 HELD),
  E2 `RECALL_RESIDUAL_UNKNOWN`을 REJECTED로(제품 HELD), T11
  `STALE_REVISION`을 REJECTED로(제품 CONFLICT, V2·V3는 CONFLICT) 기대한다.
- 제품이 내지 않는 code: T20 `APPROVAL_REQUIRED`·`INPUT_RESPONSE_UNMATCHED`·
  `REQUEST_STATE_*` 5개, T21 `CARDINALITY_INVALID`·`GOAL_INCOMPLETE`·
  `ORGANIZATION_SCOPE_INVALID`·`REGRESSION_FAILED`·`RELATION_CYCLE`·
  `REQUIRED_STAGE_INVALID`·`UNIT_INCOMPATIBLE`, T04 `DB_PRIVILEGE_DENIED`·
  `QUANTITY_INVALID`·`RAW_CORE_WRITE_FORBIDDEN`·`TRANSACTION_ROLLED_BACK`, T03
  `GENEALOGY_CYCLE`·`SEPARATION_EVIDENCE_REQUIRED`, T24
  `AUDIT_PERSISTENCE_FAILED`. 미구현 기능이거나 이름 차이다. 구현할 때
  vocabulary에 추가하거나 case를 vocabulary 이름으로 맞춰야 한다.
- C1·T03·T04·T05·T16은 `/response/errorCode`를 읽는다.
  `command-response.schema.json`의 canonical pointer는 `/error/code`다.

## NOT_RUN과 제한

- 전체 backend suite와 schema parity는 실행하지 않았다(coordinator 담당).
  schema(CDS)는 바꾸지 않았다.
- RESULT_REVISION 독립 재계산은 구현하지 않았다. 제품 token을 원행에서
  재계산할 수 있게 정의를 바꾸는 결정이 먼저 필요하다. `lockWaits`·
  `databaseTransactions` 원천 mapping도 없다.
- native S1은 FAIL로 남는다(위 S1).
- V4 노출 면 열거, V2·V3 native 경합 인수, 유료 모델·client UAT, BTP 배포,
  실제 규제기관 제출, 은행 이체·세금 신고는 실행하지 않았다. 결정자·정책
  값은 가상 fixture다. native PASS는 한정된 custody 증거이며 coverage
  pipeline PASS가 아니다.
