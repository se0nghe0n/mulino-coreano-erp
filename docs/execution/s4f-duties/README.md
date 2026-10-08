# S4 의무 종료 경로와 보관 주체 추론 제거 (s4f-duties)

## 결론

branch `step3/s4f-duties`(baseline tag `step3-s4f-baseline` = `e7f561f3`)에서
제품이 만드는 모든 의무 kind의 종료 경로를 고정했다. 정정 부족
(`DELIVERY_CORRECTED_DEFICIT`)은 이제 검증된 회복 정정으로 해소하거나
지정 MANAGER의 typed 면제 결정으로 면제할 수 있다. 정의되지 않은 경로는
generic `VERSION_UNSUPPORTED`가 아니라 owner·nextAction을 담은 구조화
`HELD`다. 반품 수령과 고객 인도에서 보관 주체 추론을 없앴다.

같은 commit `38b1eaf0`, 같은 JAR
(`6d344e884144db58099e6933c9d35fef600b30092f5a3610a9cfcd994273b557`)에서
native S4 E1·E2·C4·C1 flow와 기본 suite, S3 actual 회귀가 모두 **PASS**다.
C4의 `resolvedOrWaivedDutyRevival=0`은 이번에 native에서 실제로 관찰했다
(아래 "C4 native").

## 근거와 결정

- 계획 §5.3: Obligation status는 `OPEN|RESOLVED|TRANSFERRED|WAIVED`다.
  `resolveObligation`은 해당 의무의 충족 증거를 검증하고,
  `waiveObligation`은 종류별 권한자의 면제 결정과 사유를 요구한다.
  의무키 재처리는 중복을 만들지 않고 같은 내용의 새 사건은 새 의무일 수 있다.
- 계획 §7.1: 승인은 immutable proposal/scope hash, 대상 revision, 승인자,
  시각, 유효기간·소비 정책을 저장한다. 부정 결정도 보존하고 새 행동에
  재사용하지 않는다. 표의 기본 정책은 수량·정산 차이 MANAGER, 품질 QC,
  회수 ADMIN이다. 역할명은 권한이 아니다.
- 계획 §7.2 `resolveObligation, waiveObligation, transferObligation` 행:
  owner의 수행 증거 또는 의무 종류별 권한자의 면제 근거를 요구한다.
- 계획 §4.2 126행: 위치·보관자·owner는 별도 관계이고 인계가 소유권을
  자동 이전하지 않는다. §4.3·§13.2 C4: 실제 98 정정은 과거 판정을
  보존하고 현재 의무2를 만들며, 권한 있게 해소된 의무2는 부활하지 않는다.

### typed 면제 결정

기존 경로는 두 가지 이유로 도달할 수 없었다. 승인 행을 만드는 명령이
없었고(`ApprovalRepository.create` 호출자 없음), `waiveObligation`이
`slots.evidenceId == approvalId`를 요구해 canonical hash가 승인 ID를
포함했다. 승인은 실행할 명령의 hash를 미리 저장해야 하므로 순환이었다.

- 결정 명령 `decideQuantityDutyWaiver`(MANAGER 계열),
  `decideQualityDutyWaiver`(QC), `decideRecallDutyWaiver`(ADMIN)을
  `ResponsibilityCommands`에 두었다. 결정자는 현재 HUMAN, 해당 WORK
  scope의 capability, 같은 capability의 `ManagementAuthorities` 지정이
  모두 있어야 한다. kind의 권한 계열과 다른 결정 capability는 거부한다.
- 결정은 서버가 실행될 `waiveObligation` intent(같은 envelope, slots
  `assignmentId`·`reason`, `expectedRevision`=현재 assignment revision)를
  재구성해 hash를 계산하고 `commands.Approvals`에 proposalId=duty root,
  canonicalHash, WORK scopeHash, targetId=assignment, targetRevision,
  action=`WAIVE_<kind>`, 현재 COMMAND policy hash, decision
  `APPROVED|REJECTED`, decisionCapability, decidedAt, expiresAt,
  singleUse=true로 기록한다. 응답은 `waiverIntentHash`를 돌려준다.
- `waiveObligation`은 top-level `approvalId`만 받는다(hash 제외 필드).
  `PolicyCommandGuard`가 hash·scope·root·revision·policy·유효기간·
  결정자 현재 capability·single-use를 검사하고 성공 시 소비를 기록한다.
  `ResponsibilityEvidenceGate.requireWaiver`는 같은 assignment·revision·
  root·kind 승인인지 다시 확인하고, 결정자의 지정이 철회됐으면 효과가 없다.
- COMMAND policy rule은 `approvalActions`로 `WAIVE_<kind>`→결정
  capability를 명시한다(`PolicyCommandGuard`). wildcard는 없다. 매핑이
  없으면 `HELD POLICY_UNRESOLVED`다.
- 새 column이 필요 없어 migration(V30)과 schema parity 재생성은 없다.

### kind별 종료 경로

`ObligationClosureCatalog`가 아래 표의 SSOT다. "해소 증거"가 없는 kind의
`resolveObligation`, "면제 권한"이 없는 kind의 `waiveObligation`은
`HELD OBLIGATION_RESOLUTION_UNDEFINED|OBLIGATION_WAIVER_UNDEFINED`이고
응답 `responsibility`에 assignmentId·kind·ownerId·supervisorId·
nextAction·nextCheckAt·closure 계약을 담는다. 효과는 0이다.

| kind | 생성 위치 | 자동 종료 | resolveObligation 해소 증거 | 면제 권한 |
|---|---|---|---|---|
| FOLLOWUP_REVIEW | AssessmentCorrectionImpact | - | 검증된 RESPONSE_COMPLETED가 root 범위를 정확히 덮음(기존) | 미정의 |
| QUALITY_REVIEW | QualityCommands placeHold/revoke | 참조 제한의 releaseHold | 같은 제한의 QC releaseHold 결정(기존) | 미정의 |
| REGULATORY_REVIEW | RegulatoryResponsibilities | 같은 절차 새 결과 | 같은 절차의 현재 검증 결과(기존) | 미정의 |
| EXTERNAL_RECONCILIATION | RuntimeService | - | 모든 outbox 효과의 CONFIRMED_SUCCESS 증거(기존) | 미정의 |
| DELIVERY_CORRECTED_DEFICIT | DeliveryCorrection | - | 같은 인도의 **현재** 검증 PHYSICAL_DELIVERY 정정이 해당 부족 범위를 회복(신규) | MANAGER |
| SETTLEMENT_DIFFERENCE | SettlementCommands matchInvoice | - | 같은 invoice match의 MANAGER 확정 조정으로 남은 차이 0, 통화·범위 차이 없음(신규) | MANAGER |
| RECEIPT_SHORTFALL | TradeResidualRemedy | 검증된 수령 기여 | - | MANAGER |
| RECEIPT_EXCESS / RECEIPT_UNALLOCATED | ReceiptCommands | - | - | MANAGER |
| PURCHASE_EXCESS_RECONCILIATION | PurchaseCommands creditReceipt | - | - | MANAGER |
| ALLOCATION_SHORTAGE | InventoryCommands adjustQuantity | - | - | MANAGER |
| RETURN_SETTLEMENT_REVIEW | ReturnCommands receiveReturn | - | - | MANAGER |
| RETURN_QC_REVIEW | ReturnCommands receiveReturn | - | - | QC |
| RECALL_INVESTIGATION / RECALL_EXCLUDED_SCOPE | RecallCommands | - | - | ADMIN |
| RECEIPT_RECONCILIATION | ReceiptCommands provisional | 같은 관측의 검증 확정 | - | 미정의 |
| DELIVERY_RECONCILIATION | DeliveryCommands provisional | 같은 관측의 검증 확정 | - | 미정의 |
| RETURN_RECONCILIATION | ReturnCommands provisional | 같은 관측의 검증 확정 | - | 미정의 |
| RECALL_EXCEPTION_RESIDUAL | RecallCommands EXCEPTION | - | - | 미정의(ADMIN 예외의 잔여 책임 유지) |
| DELIVERY_RESTRICTION_RESPONSE | DeliveryCommands confirm | - | - | 미정의 |
| RETURN_COMMERCIAL_REVIEW | ReturnCommands receiveReturn | - | - | 미정의 |
| RETURN_<decision> | ReturnCommands decideReturnDisposition | - | - | 미정의 |
| SUPPLIER_DISPATCH_RECONCILIATION / SUPPLIER_REPLY_REVIEW / PURCHASE_CANCELLATION_ACCEPTANCE | PurchaseCommands | - | - | 미정의 |
| UNVERIFIED_SOURCE_REVIEW | ResponsibilityService intake correction | - | - | 미정의 |
| RUNTIME_RECOVERY | RuntimeService | - | - | 미정의 |
| INVENTORY_VALIDITY | InventoryValiditySweeper | - | - | 미정의 |

판단 근거와 한계:

- 정정 부족의 해소는 "부족 범위가 현재 검증된 실제 인도로 더 이상
  부족이 아님"으로 정했다. 부족을 만든 정정, 대체된 정정, 원 인도는
  증거가 될 수 없다. 고객의 최종 수용·대금 조정처럼 계획이 정의하지
  않은 증거는 해소가 아니라 MANAGER 면제의 사유로만 남는다.
- MANAGER/QC/ADMIN 계열 배정은 §7.1 표의 기본 정책을 kind에 적용한
  구현 결정이다. 실제 결정자 mapping(R5)은 구축 manifest에서 확정한다.
- `RECALL_EXCEPTION_RESIDUAL`은 ADMIN 예외 자체가 남기는 잔여 책임이라
  면제를 열면 §6 회수 규칙("승인된 미해결 예외와 잔여 책임")을 우회한다.
  계획이 해소 증거를 정의하지 않아 fail-closed로 두었다.
- 자동 종료 kind(`*_RECONCILIATION`, `RECEIPT_SHORTFALL`)는 도메인
  명령이 같은 transaction에서 RESOLVED로 만든다. 공개 resolve는 열지 않았다.

### 보관 주체 추론 제거

- `ReturnStockPrimitives`는 반품 실물의 보관 주체를 확인 actor로 두던
  것을 없앴다. `receiveReturn` 확인 slot `receivingCustodianId`(선택)가
  있으면 대상은 같은 조직의 HUMAN/AGENT 내부 actor이고 같은 scope의 현재
  `receiveReturn` 권한이 있어야 하며, 그 canonical의 현재 검증 chain의
  원본 blob과 event payload가 같은 보관 주체를 지명해야 한다(수령의
  `receivingCustodianId`와 같은 계약). 불일치·미지명은 HELD, 효과 0이다.
  slot이 없으면 보관 주체는 미확인이다. 소유는 바뀌지 않는다.
- `FulfillmentStockPrimitives.observeDelivery`는
  `StockPrimitives.transferRangeReleasingCustody`로 고객 장소에 옮긴
  segment의 창고 보관 주체를 지운다(미확인). 소유는 바뀌지 않는다.
- E1 반품 이후 단계는 반품 물량을 예약·이동하지 않아 원본에 보관 주체를
  추가할 필요가 없었다. E1 원 행에서 고객 20과 반품 10 segment의
  `custodianId`는 null, 창고 30·40은 supervisor다
  (`raw/e1-1/actual-s4-native.json.gz`, `e1-final-independent`).

## commit

| commit | 내용 |
|---|---|
| `373a977a` | 종료 catalog, typed 면제 결정, kind resolver 2개, guard, 보관 주체, backend test |
| `38b1eaf0` | `c4-author.py` 면제·재정정 단계, c4 fixture/flow, `c4-duty-closure.json` |
| 이 기록 commit | 실행 기록과 raw 증거 |

probe branch `step3/s4e-c4-resolution-probe`는 읽기만 했고 병합하지 않았다.
이 branch에 probe 파일(`c4-resolution-probe*.json`)은 없다.

## 실행 명령과 결과

모든 Maven·verify는 `. /Volumes/VideoStore/Developer/.mulino-tools/env.sh`
뒤 `$MULINO_SLOT`로 한 번에 하나씩 실행했다. Java 21, PostgreSQL 18
container(`postgres@sha256:4ef4dbc9…`).

### backend 집중 test (`373a977a`와 같은 source)

```bash
$MULINO_SLOT ./mvnw -B -ntp -q -f backend/pom.xml test \
  -Dtest='ReceiptGatewayPostgresTest,FulfillmentPostgresTest,RecallGatewayPostgresTest,S4DeliveryCorrectionContractTest,S1ReadIntegrationTest,ResponsibilityEvidenceGateTest,ResponsibilityServiceTest,ResponsibilitySubjectBindingTest,CompletionCoverageGatewayTest,QualityPostgresTest,ControlPersistenceTest'
$MULINO_SLOT ./mvnw -B -ntp -q -f backend/pom.xml test -Dtest='SettlementCommandPostgresTest'
$MULINO_SLOT ./mvnw -B -ntp -q -f backend/pom.xml test -Dtest='ReturnGatewayPostgresTest'
```

| class | tests | failures | errors | skipped |
|---|---|---|---|---|
| FulfillmentPostgresTest | 19 | 0 | 0 | 0 |
| SettlementCommandPostgresTest | 7 | 0 | 0 | 0 |
| ReturnGatewayPostgresTest | 8 | 0 | 0 | 0 |
| ReceiptGatewayPostgresTest | 14 | 0 | 0 | 0 |
| RecallGatewayPostgresTest | 6 | 0 | 0 | 0 |
| S4DeliveryCorrectionContractTest | 3 | 0 | 0 | 0 |
| S1ReadIntegrationTest | 8 | 0 | 0 | 0 |
| QualityPostgresTest | 10 | 0 | 0 | 0 |
| ControlPersistenceTest | 7 | 0 | 0 | 0 |
| CompletionCoverageGatewayTest | 19 | 0 | 0 | 0 |
| ResponsibilityEvidenceGateTest | 8 | 0 | 0 | 0 |
| ResponsibilityServiceTest | 7 | 0 | 0 | 0 |
| ResponsibilitySubjectBindingTest | 3 | 0 | 0 | 0 |

새·확장 test(실제 PostgreSQL, 실제 gateway·PolicyCommandGuard):

- `FulfillmentPostgresTest.correctedDeliveryDeficitClosesOnlyByVerifiedRestorationOrTypedManagerWaiverAndIsNotRevived`:
  인도30→정정28로 OPEN 부족2. 부족을 만든 정정·원 인도로 resolve는 HELD,
  OPEN 유지. 결정 없는 waive, 비지정 owner의 결정, 다른 계열(QC) 결정은
  REJECTED. REJECT 결정은 행으로 보존되고 그것으로 waive는 REJECTED.
  승인 뒤 owner handover로 assignment revision이 바뀌면 그 승인은 REJECTED.
  사유를 바꾼 waive는 REJECTED. 올바른 waive는 WAIVED, 같은 key replay는
  같은 commandId, 새 key 재사용은 무효, 소비 1행. 같은 정정 재처리는
  duplicate, 같은 28의 v3 정정 뒤에도 OPEN 0·root 1·WAIVED 1. 새 사실
  27은 새 의무1(root 2), 대체된 27 정정으로 resolve는 HELD, 30 회복 정정으로
  resolve는 RESOLVED. 고객 인도 segment의 custodian null, owner 유지.
- `FulfillmentPostgresTest.restrictionResponseWithoutDefinedClosureIsHeldWithRetainedOwnerAndNextAction`:
  DELIVERY_RESTRICTION_RESPONSE의 resolve·waive는 HELD
  `OBLIGATION_*_UNDEFINED`, `responsibility.ownerId/nextAction/assignmentId`,
  effects 빈 값, assignment revision 불변, 승인 행 0.
- `SettlementCommandPostgresTest.exactManagerConfirmationRetainsOriginalVarianceAndDuty`
  끝에 추가: PROPOSE 조정으로 resolve는 HELD, 다른 invoice(범위 차이)에
  확정 조정을 대면 HELD, 같은 match의 확정 조정은 RESOLVED, 원 차이 5 유지.
- `ReturnGatewayPostgresTest.returnedStockCustodyIsUnknownUnlessVerifiedReturnEvidenceNamesTheInternalReceiver`:
  slot 없는 반품은 custodian null·owner 유지, 증거가 지명하지 않은 slot은
  HELD EVIDENCE_UNVERIFIED·효과 0, 모르는 actor slot은 거부, 증거가 지명한
  내부 actor slot은 그 보관 주체로 기록.
- `ResponsibilityEvidenceGateTest`: 다른 assignment·revision·빈 사유,
  REJECTED·만료 승인은 면제 불가(단위).

### harness test (`38b1eaf0`)

```bash
$MULINO_SLOT ./mvnw -B -ntp -q -f verification/harness/pom.xml test
```

41개 report, 434 tests / 0 failures / 0 errors / 0 skipped.

### native flow (`38b1eaf0`)

```bash
$MULINO_SLOT python3 verification/actual/s4/build.py
ACTUAL_FLOW_REF=verification/actual/s4/<x>-flow.json \
  $MULINO_SLOT ./verify actual-s4 /tmp/mulino-s4f-<x>-1     # c4 e1 e2 c1
$MULINO_SLOT ./verify actual-s4 /tmp/mulino-s4f-suite-1
$MULINO_SLOT python3 verification/actual/s3/build.py
$MULINO_SLOT ./verify actual-s3 /tmp/mulino-s4f-s3-1
```

| attempt | commit | JAR sha256 | flow | 결과 | assertion | 첫 실패 |
|---|---|---|---|---|---|---|
| `c4-1` | `38b1eaf0` | `6d344e884144` | c4-flow.json | PASS | 392 | - |
| `e1-1` | `38b1eaf0` | `6d344e884144` | e1-flow.json | PASS | 612 | - |
| `e2-1` | `38b1eaf0` | `6d344e884144` | e2-flow.json | PASS | 410 | - |
| `c1-1` | `38b1eaf0` | `6d344e884144` | c1-flow.json | PASS | 304 | - |
| `suite-1` | `38b1eaf0` | `6d344e884144` | flow.json | PASS | 1718 | - |
| `s3-regression-1` | `38b1eaf0` | `6d344e884144` | s3 flow.json | PASS | 594 | - |

E1 612·E2 410·C1 304·S3 594는 s4e-custody와 같다. C4는 321→392,
suite는 1647→1718로 늘었다(C4 면제·재정정 단계 71개). 기대값을 줄이지
않았고 결과를 seed하지 않았다. 실패한 시도는 없다. `raw/<attempt>/`에
`run-receipt.json`, `actual-s4-native.json.gz`(S3는
`actual-s3-native.json.gz`), `native-stdout.json.gz`를 보존했다. gzip
mtime은 0이다. `raw/index.json`이 위 표의 기계 판독 원본이다.

### C4 native (`c4-duty-closure.json`)

실제 98 정정 뒤 `c4-deficit2-target`이 OPEN 부족2를 잡는다. 부족을 만든
`c4-correction98` canonical로 resolve는 `HELD EVIDENCE_UNVERIFIED`, 결정
없는 waive는 REJECTED, 의무 행은 1개로 OPEN이다. supervisor(WAIVE_AUTH
지정)의 `decideQuantityDutyWaiver` APPROVE가 MANAGER 계열 승인을 만들고,
reader(work owner)의 waive가 WAIVED다. 같은 승인 재사용은 REJECTED다.
같은 실제 98의 v3 정정(`c4-correction98-reprocess`)을 연결한 뒤
`c4-waived-deficit-not-revived`에서 OPEN 0, 의무 행 1(WAIVED), 정정 행 2,
과거 판정 SATISFIED, 인도 100, 반품 20을 관찰했다. 이것이 계약
`resolvedOrWaivedDutyRevival=0`의 관찰이다. coverage pipeline의 PASS는
아니며 native custody 증거다.

## NOT_RUN과 제한

- 전체 backend suite와 schema parity는 실행하지 않았다(coordinator 담당).
  schema는 바꾸지 않았다.
- 결정 이후 결정자의 `ManagementAuthorities` 지정이 철회되면 waive를 거부하는
  검사는 구현했지만 그 반례 test는 작성하지 않았다.
- 결정 capability `decideQualityDutyWaiver`·`decideRecallDutyWaiver`의
  실제 승인·면제 실행(RETURN_QC_REVIEW, RECALL_*)은 PostgreSQL test가
  계열 거부만 확인했고 승인 성공 경로는 실행하지 않았다.
- RETURN_QC_REVIEW 등 "해소 증거 미정의" kind의 도메인 해소 의미(예:
  반품 QC 판정으로 해소)는 계획이 정하지 않아 열지 않았다. 결정이 필요하다.
- `transferObligation`·`emergencyReassign`의 승인 행 생성은 이번 범위가
  아니다. `emergencyReassign`은 여전히 `EMERGENCY_REASSIGN` 승인 행을
  만드는 명령이 없다.
- 운송 중(TRANSIT) segment의 보관 주체는 바꾸지 않았다(창고 보관 주체
  유지). 운송인 인계 증거로 보관 주체를 바꾸는 계약은 이번 범위가 아니다.
- V4 노출 면 열거, `postDispatchRecall`, V2, V3의 native 인수는 NOT_RUN이다.
- 유료 모델·client UAT, BTP 배포, 실제 규제기관 제출, 은행 이체·세금
  신고는 실행하지 않았다. 결정자·정책 값은 가상 fixture다.
