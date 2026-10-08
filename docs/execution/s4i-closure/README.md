# S4i 종료 리뷰 지적 수정 기록

사용자 Step3(Claude Opus medium) 구현 worker의 기록이다. 기준선은 tag
`step3-s4i-baseline`(`3023e0fa`), branch는 `step3/s4i-closure`,
worktree는 `/Volumes/VideoStore/Developer/mulino-ontology-step3-s4i-closure`다.
coordinator만 Task branch에 통합한다. 코드 commit은 `7fa78db3`이다.

입력은 `/Volumes/VideoStore/Developer/.mulino-tools/s4-closure-1-todo.json`
(skeptic이 조정한 심각도)과 `s4-closure-1.json`이다. 계획 근거는
`docs/ontology-implementation-plan.md` §6(사실 기록과 실행 권한, 판매,
반품·회수, 정산, 구매·운송의 납기 변경), §4.2, §4.3, §5.3, §7.1이다.

## 판정

| 항목 | 판정 | 원인과 수정 | test |
|---|---|---|---|
| MUST 1 반품 중복 보고 (P2) | FIXED | 중복 분기가 다른 관측(O1)의 receipt canonical로 O2를 확정하고 의무를 닫았다. 이제 O2는 자기 canonical(physicalScope=O2, occurrenceIdentity=O2.eventId)로만 확정한다. 그 검증 chain의 원본과 event가 같은 값으로 받은 반품(`sameOccurrenceAsReturnId`=O1)을 지명하고, O2의 발생 시각이 receipt와 같아야 한다. 남의 canonical 인용은 HELD/EVIDENCE_UNVERIFIED, 지명 없음은 EVIDENCE_UNVERIFIED, 다른 반품 지명·다른 시각은 EVIDENCE_CONFLICT이고 O2는 PROVISIONAL, RETURN_RECONCILIATION은 OPEN으로 남는다. 닫힐 때 의무의 evidenceId는 O2 자신의 canonical이다. 재고 이동은 없다. | `ReturnGatewayPostgresTest.otherReportsOfReceivedReturnCloseAgainstTheOneReceipt`(갱신: 남의 canonical HELD 뒤 자기 증거로 APPLIED), `laterArrivalOverReceivedRangeKeepsItsReconciliationDuty`(새: 2주 뒤 실물은 자기 증거로도 HELD, 의무 OPEN, 재고 10 유지). 기존 `sameKeyReplayAndNewEventCannotRecreatePhysicalInventory`는 지명 없는 새 사건이라 그대로 not APPLIED다 |
| MUST 2 s4-settle-02 잔여 (P2) | FIXED (아래 한계) | 정정 hook이 없었다. settlement 소유 port `SettlementContributionPort`를 두고, `AssessmentCorrectionImpact.evidenceLinked`가 대체 canonical(PHYSICAL_DELIVERY·PHYSICAL_RECEIPT, supersedesId 있음)을 연결하는 같은 거래에서 호출한다. `SettlementContributionReview`는 supersede chain에 속한 occurrence의 Match마다 공통 판정식(`SettlementState.assess`)을 다시 구한다. CURRENT가 아니면 정정 canonical을 source(OCCURRENCE)로 하는 새 SETTLEMENT_DIFFERENCE root를 work owner·nextAction·nextCheckAt과 함께 연다. CLOSED work면 follow-up work에 연다. 조정 gate는 Match의 열린 정산 의무 전체(원 차이 root와 기여 변경 root)를 본다. MATCHED였던 송장도 이 의무가 열려 있는 동안 조정을 제안할 수 있다. 조회는 `contributionDutyRootIds`, `differenceDutyOpen`, `currentSettlementDifference`를 보인다. 기여 변경 root의 해소는 기존 판정식 그대로다. 현재 기여가 원 Match와 다르면 SATISFIED가 될 수 없어 MANAGER 면제로만 닫힌다(s4-settle-03과 같은 규칙). | `FulfillmentPostgresTest.postMatchDeliveryCorrectionOpensOwnedSettlementDifferenceThatSurvivesTheDeficitWaiver`(새, SALE 실제 경로: 인도30·송장30 MATCHED, 28 정정 뒤 OPEN 정산 의무 1(owner·nextAction·nextCheck), 물류 부족2 MANAGER 면제 뒤에도 OPEN, 조정 PROPOSE APPLIED, 재처리해도 root 1). `AssessmentCorrectionLinkTest`는 생성자 인자만 갱신 |
| P3 SELL 적격 denylist | FIXED | `QualityEligibility.read`가 {TRANSIT,CUSTOMER,SUPPLIER} denylist였다. 이제 `FulfillmentCommands.requireWarehouse`와 같은 allowlist(Places.kind=INTERNAL_STORAGE이고 custodian이 내부 actor)다. | `FulfillmentPostgresTest.itemScopeSaleEligibilityIsAnInternalStorageAllowlist`(새: 고객지 kind EXTERNAL_CUSTOMER에 인도20 뒤 eligible 70, 고객지 0/DENIED) |
| P3 인도 기한 revision | FIXED | `recordDelivery`가 확정 시점의 현재 revision으로 기한을 판정했다. `SalesCommands.lineInForceAt`은 previousLineId chain에서 effectiveAt이 발생 시각 이하인 가장 새 revision을 고르고, `DeliveryCommands`가 그 line으로 판정한다. | `FulfillmentPostgresTest.deliveryTimelinessUsesTheOrderRevisionInForceAtTheOccurrence`(새: 발생 뒤·확정 전 기한 단축은 정시 인도 10을 지우지 않고, 발생 뒤 소급 연장은 지연 10을 인정하지 않으며 대응 의무가 생긴다) |
| P3 Reconciliations 두 번째 행 | FIXED | `link`의 commit fence 재검사가 `match`를 다시 불러 검토 행을 하나 더 썼다. 재검사는 같은 판정을 하되 행을 쓰지 않는다(`match(...,record=false)`). | `ReturnGatewayPostgresTest.linkingAReviewWritesNoSecondReconciliationRow`(새: claim당 검토 1, 검증 1) |
| P3 transfer 실제 PG test | ADDED (test만) | 제품 동작은 이미 맞다. 수용된 30/100 이전은 원 assignment를 TRANSFERRED·invalid로 바꾸고 source [0,70)·target [70,100) leaf 두 개를 OPEN으로 만든다. 거절·만료는 원 assignment를 OPEN·100으로 두고 자식이 없다. 결함이 없어 RED는 없다. | `FulfillmentPostgresTest.partialDutyTransferSplitsTheExactLeafOnlyOnAcceptanceAndRejectOrExpiryKeepsItOpen`(새, `ResponsibilityService` 직접 호출) |
| P3 s3-receipt contracts 중복 custody 문장 | FIXED (문서) | `docs/execution/s3-receipt/contracts.md`가 slot 있는 중복만 설명했다. 모든 확정의 재검사와 scenario B fail-closed를 현재 코드대로 적었다. | 문서 |
| P3 duplicate custody scenario B | DEFERRED | slot 없이 첫 확정한 segment에 보관 주체를 뒤에 붙일 공개 경로가 없다. 이 segment는 `requireWarehouse`에서 예약·출고가 거부되는 fail-closed 상태이고 잘못된 효과는 없다. 다만 이를 소유하는 의무도 없다. 증거 있는 덧쓰기 경로는 이미 분할·이동됐을 수 있는 segment의 custody를 감사 가능한 별도 기록으로 고쳐야 한다. 싸게 고칠 수 없어 남긴다. |  |
| P3 DELIVERY_RESTRICTION_RESPONSE 재-scope | DEFERRED | 의무는 존재하고 수량만 오래된 값이다. responsibility 소유의 scope revision API는 새 공개 명령·어휘·closure catalog 변경이 필요하다. 이번 범위에서 싸지 않다. |  |
| P3 traceRecall leg 인가 | DEFERRED | 코드상 실재한다. 같은 조직 안의 read-scope 누출이고 쓰기 효과는 없다. 회귀 test에 Shipment·Leg·Cargo·검증 canonical 사슬이 필요하다. 검증된 leg event를 seed하지 않는 기준 때문에 이번에 하지 않았다. |  |
| P3 recall returnId·고객 leaf 회수 test | DEFERRED | coverage 공백이며 제품 결함은 아니다. 반품 receipt에서 회수하는 실제 경로 fixture가 크다. |  |
| P3 waiveObligation 결정 누락·승인 재사용 code | DEFERRED | 둘 다 공용 `PolicyCommandGuard.verify`의 합성 조건과 `ResponsibilityCommands`의 `DomainError.forbidden()`에서 나온다. code를 나누면 모든 승인 경로의 공개 오류 어휘와 native S3/S4 author 단언이 같이 바뀐다. 어휘 contract와 native 입력을 함께 바꾸는 별도 작업이 필요하다. |  |
| P3 정산 응답의 상수 bankEffect | NOT_A_DEFECT (유지) | `bankEffect="0"`, `taxIssuanceEffect="0"`, `paymentAuthorized=false`는 계산값이 아니라 계획 §6 "실제 은행 지급·수금·세금 발행/신고 자동 연동은 미지원"을 응답마다 명시하는 불변 표지다. 저장 열 `PaymentReferences.bankEffect`도 0으로 기록된다. 기존 test(`oneRemittanceReferencesSeveralInvoicesOncePerInvoice`)가 합계 0을 단언한다. 제거하면 소비자가 은행 효과 유무를 추론해야 하므로 남긴다. |  |

## 남은 한계

- MUST 2는 대체 canonical이 연결될 때 동작한다. 수령·인도 claim이나
  event만 정정되고 새 canonical이 아직 없으면 정산은 UNVERIFIED다.
  그 기간의 owner는 evidence 정정의 FOLLOWUP_REVIEW(영향 work)다.
  정산 의무는 새 canonical이 연결되는 거래에서 생긴다. PURCHASE 수령
  chain의 대체 canonical 경로는 실제 PG로 실행하지 않았다(SALE만 실행).
- 기여 변경 정산 의무의 MANAGER 면제는 운영 COMMAND policy의
  `approvalActions`에 `WAIVE_SETTLEMENT_DIFFERENCE`가 있어야 한다.
  test fixture policy에는 넣지 않았고 이번 test는 면제를 실행하지 않았다.
- MUST 2 test는 송장 기록·대조·조정 제안을 공개 gateway와 공개 증거
  명령으로 만든다. 검증 사실을 seed하지 않는다.

## 실행 증거

환경: `. /Volumes/VideoStore/Developer/.mulino-tools/env.sh`, `$MULINO_SLOT`,
Testcontainers PostgreSQL. 명령은
`$MULINO_SLOT ./mvnw -B -ntp -f backend/pom.xml -Dmulino.evidence.blob-root=$(mktemp -d) -Dtest=<classes> test`다.
결과 형식은 run/fail/error/skip이다. log 요약은 `raw/`에 있다.

| 시점 | classes | exit | 결과 | log |
|---|---|---|---|---|
| RED MUST: 새 test + baseline 제품 코드(port 구현 제거) | ReturnGatewayPostgresTest, FulfillmentPostgresTest#postMatch* | 1 | 16/3/0/0. `otherReports…`(남의 canonical이 APPLIED), `laterArrival…`(남의 canonical이 APPLIED), `postMatch…`(정산 의무 0) | raw/red-must-summary.log |
| RED P3 eligibility·timeliness | FulfillmentPostgresTest#itemScope*+deliveryTimeliness* | 1 | 3/2/0/0. allowlist: eligible 90(기대 70), timeliness: 정시 인도 contributed 0(기대 10) | raw/red-p3-eligibility-timeliness-summary.log |
| RED P3 reconciliation | ReturnGatewayPostgresTest#linkingAReview* | 1 | 1/1/0/0. 검토 행 2(기대 1) | raw/red-p3-reconciliation-summary.log |
| GREEN (코드 `7fa78db3`) | ReturnGatewayPostgresTest 16, FulfillmentPostgresTest 38, SettlementCommandPostgresTest 14, AssessmentCorrectionLinkTest 3, SettlementStateTest 4, DomainVocabularyContractTest 4, S4DeliveryCorrectionContractTest 3, SalesCommandPostgresTest 6, DeliveryCorrectionTest 6, EvidenceReconciliationTest 6, EvidenceGatewayTest 17, ReceiptGatewayPostgresTest 16, RecallGatewayPostgresTest 13, QualityPostgresTest 10, InventoryPostgresTest 9 | 0 | 165/0/0/0 | raw/green-summary.log |

transfer test는 결함 수정이 아니어서 RED가 없다. schema·migration·어휘는
바꾸지 않았다. V33은 만들지 않았고 schema parity 재생성도 하지 않았다.

## NOT_RUN

coordinator 지시(2026-10-08 check 범위 변경)에 따라 실행하지 않았다.
coordinator가 통합 때 실행한다.

- NOT_RUN (coordinator): 전체 backend suite.
- NOT_RUN (coordinator): native S4 suite(`verification/actual/s4/build.py`
  뒤 `./verify actual-s4`), `actual-s3`, `actual-s1`. SELL 적격 allowlist는
  내부 보관자가 없는 창고 segment의 item-scope 적격을 0으로 바꾼다.
  native E1/C1 기대값에 영향이 있는지는 이 실행에서 확인해야 한다.
- NOT_RUN: T19 독립 oracle, V2/V3 경합.
