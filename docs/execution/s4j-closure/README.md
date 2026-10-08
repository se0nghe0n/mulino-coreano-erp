# S4j 종료 리뷰 2 지적 수정 기록

사용자 Step3(Claude Opus medium) 구현 worker의 기록이다. 기준선은 tag
`step3-s4j-baseline`(`a89885f8`, backend 501 PASS, native S4/S3/S2/S1
PASS), branch는 `step3/s4j-closure2`, worktree는
`/Volumes/VideoStore/Developer/mulino-ontology-step3-s4j`다. 소유 범위는
`backend/**`, `verification/actual/**`,
`verification/harness/src/main/java/org/mulino/verification/actual/**`다.
coordinator만 Task branch에 통합한다.

입력은 skeptic이 검증한
`/Volumes/VideoStore/Developer/.mulino-tools/s4-closure-2-opus-xhigh-verdicts.json`과
`s4-closure-2-astra-low-verdicts.json`(전체 review는 같은 폴더의
`s4-closure-2-opus.json`, `s4-closure-2-astra.json`)이다. 계획 근거는
`docs/ontology-implementation-plan.md` §3.1(기록 시점·미확인 구별),
§4.2(lock을 얻은 뒤 현재 상태를 다시 읽는다), §4.3(정정 재평가), §5.3
(의무의 충족 증거·면제), §6 정산·판매, §7.1(승인 hash 결합)이다.

## 판정

| 항목 | 판정 | 원인과 수정 | test (RED → GREEN) |
|---|---|---|---|
| MUST 1 opus[1] 움직이는 시계에서 PHYSICAL_DELIVERY 정정 link가 항상 HELD (P2) | FIXED | gateway는 실행 전에 request knownAt을 고정한다. link는 정정 canonical·verification을 `clock.instant()` 시각으로 쓴다. 그런데 `correctionImpact`와 `SettlementContributionReview`가 같은 knownAt view로 그 행을 읽어 `EVIDENCE_UNVERIFIED`가 됐고 거래 전체가 rollback됐다. `DomainContext.knownThrough(at)`를 추가했다. knowledge time만 늘리고 줄이지 않으며 asOf는 그대로 둔다. `AssessmentCorrectionImpact.evidenceLinked`는 방금 쓴 canonical, 그 Verifications·EvidenceLinks 행의 최대 recordedAt까지만 늘린 context로 무효화·정정·정산 재평가를 한다. 같은 결함이 `apply`(correctEvidence)에도 있었다. 정정 행 자신을 knownAt view로 찾아 `SOURCE_UNVERIFIED`가 되거나 superseding event를 못 봤다. 그래서 정정 event·claim의 recordedAt까지 같은 방식으로 늘렸다. | `FulfillmentPostgresTest.deliveryCorrectionLinksUnderAnAdvancingClockAndOpensDeficitAndSettlementDuties`(새, 1 ms씩 전진하는 clock). RED: correction link `HELD/EVIDENCE_UNVERIFIED`. GREEN: link APPLIED, 부족 의무 2 OPEN, DeliveryCorrections 28, SETTLEMENT_DIFFERENCE 1 OPEN(source=정정 canonical, owner·nextAction·nextCheck 있음) |
| MUST 2 opus[0] matchInvoice가 고정 knownAt으로 직전 정정을 놓침 (P2, 1번 수정 뒤 도달 가능) | FIXED | `match`는 lock 전에 고정한 knownAt으로 DeliveryCorrections를 읽었다. 그래서 기다리는 동안 commit된 정정을 놓치고 옛 30으로 MATCHED를 기록했고 SETTLEMENT_DIFFERENCE도 열지 않았다. 계획 §4.2대로 기여 범위를 writer와 같은 key로 잠근다(SALE `sales/delivery-correction/<deliveryId>`, PURCHASE `purchase-credit:<receipt occurrence>`). 그 lock 아래에서 `c.knownThrough(clock.instant())`로 현재 기여를 다시 읽는다. | `FulfillmentPostgresTest.matchFixedBeforeACommittedCorrectionRereadsTheContributionUnderItsLock`(새, 두 실제 거래와 전진 clock). match 거래는 knownAt을 고정한 뒤 `prepare`에서 barrier로 멈춘다. 그동안 정정 28이 commit되고, 그 뒤 match가 재개된다. RED(1번만 고친 코드): `MATCHED`, received 30, 정산 의무 0. GREEN: `DIFFERENCE`, received 28, quantityDifference 2, Match.occurrenceId=정정 canonical, 정산 의무 1(owner·nextAction) |
| MUST 3 astra[0] 조정+면제 뒤 복원하면 정산 차이의 owner가 없음 (P2) | FIXED | hook은 CURRENT면 건너뛰었다. 그래서 30→28 정정, −2 조정 확정, MANAGER 면제, 30 복원을 거치면 결과가 UNSATISFIED(+2 남음)인데 열린 root가 없었다. `SettlementContributionReview`는 이제 정정 뒤 판정이 SATISFIED가 아니고 이 Match의 열린 root가 없을 때 복원 canonical을 source로 새 root를 연다. 기존 CHANGED 경로의 root(정정마다 새 root)는 그대로다. | `FulfillmentPostgresTest.restorationAfterAdjustmentAndWaiverOpensAnOwnedSettlementDifference`(새, gateway 전체 순서). fixture COMMAND policy에 `WAIVE_SETTLEMENT_DIFFERENCE→decideQuantityDutyWaiver`를 넣었다. RED: 복원 뒤 OPEN 정산 의무 0. GREEN: CHANGED root는 −2 조정으로 해소되지 않고(HELD) MANAGER 면제로만 닫힌다. 복원 뒤 새 root 1 OPEN(source=복원 canonical, owner·nextAction·nextCheck). 조회 contributionState CURRENT, result UNSATISFIED, differenceDutyOpen true. +2 PROPOSE·CONFIRM이 APPLIED이고 그 조정으로 root가 RESOLVED된다. 같은 정정을 재처리해도 root는 2개 그대로다 |
| MUST 3 opus[3] 복원 뒤 기여 변경 root가 계속 OPEN | FIXED | 기여 변경 root에 해소 경로가 없었다. `SettlementResponsibilities.requireResolution`은 evidenceId가 조정이 아니면 복원으로 판정한다. 조건은 셋이다. root가 기여 변경 root여야 한다. evidence가 현재 기여의 검증된 canonical이고 root를 연 정정을 supersede chain으로 대체해야 한다. 공통 판정식이 SATISFIED여야 한다. 원 차이 root에는 이 해소가 없다. closure catalog·어휘의 resolution 문구를 고쳤다. | `FulfillmentPostgresTest.restoredContributionResolvesItsChangeRootOnlyByTheRestoringCanonical`(새). RED: 복원 canonical로 resolve하면 `HELD`(조정 없음). GREEN: 정정 v2·원 canonical로는 HELD, 복원 v3으로 RESOLVED(evidenceId=v3). 복원에서 새 root는 열리지 않는다 |
| SHOULD astra[1] matchRoots/correctionRoots가 knownAt을 무시 | FIXED | `SettlementState.matchRoots`·`correctionRoots`는 recordedAt>knownAt인 root를 읽지 않는다. 이 command 거래가 쓴 root는 command knownAt을 recordedAt으로 가지므로 gate에서 계속 보인다. 공개 `getObject`를 정정 전 knownAt으로 읽으면 이미 FORBIDDEN이다. 정정이 invoice가 gate로 쓰는 Work 행을 바꾸기 때문이고 기존 동작이다. 그래서 회귀는 getObject와 getSettlement가 공유하는 `SettlementState` 읽기에서 단언한다. | `FulfillmentPostgresTest.historicalSettlementReadDoesNotListALaterContributionRoot`(새). RED: 정정 전 knownAt에서 root 1개가 보였다. GREEN: 정정 전 `[]`, 현재 `[root]`, 현재 getObject `contributionDutyRootIds=[root]` |
| SHOULD opus[4] 오래된 조정 제안이 다른 root로 확정됨 | FIXED | 제안 hash가 대상 root를 포함하지 않았다. 이제 `proposalHash`는 invoice·match·금액·통화·사유·`dutyRootId`의 hash다. root는 slot `dutyRootId`로 지정한다. 지정하지 않으면 열린 root가 하나일 때만 그 root로 정하고, 여럿이면 `TYPE_INVALID`다. CONFIRM은 현재 열린 root에서 같은 방식으로 다시 정한다. 그래서 제안 당시 root가 닫힌 뒤 열린 다른 root로는 확정할 수 없다. 조정으로 의무를 해소할 때도 그 조정의 hash가 해당 root에 결합됐는지 검사한다. schema 변경은 없다(hash로 결합). | `FulfillmentPostgresTest.adjustmentProposalIsBoundToItsTargetDutyRoot`(새). RED: root 둘이 열린 상태에서 이름 없는 CONFIRM이 APPLIED. GREEN: 이름 없음 REJECTED, 다른 root 지정은 HELD `APPROVAL_HASH_MISMATCH`, 원 root 면제 뒤 CONFIRM은 HELD `APPROVAL_HASH_MISMATCH`, 닫힌 원 root 지정은 REJECTED `SETTLEMENT_DIFFERENCE_NOT_OPEN`, CONFIRMED 0 |
| SHOULD opus[5] 어휘 밖 Places.kind가 확정 DENIED 0 | FIXED | 명시적 외부 kind(TRANSIT, CUSTOMER, SUPPLIER, `EXTERNAL_*`)만 OUTSIDE다. INTERNAL_STORAGE는 내부 custodian이 있어야 CONFIRMED다. 그 밖의 kind(예: WAREHOUSE)는 UNCONFIRMED로 판정하고 unknowns에 `CUSTODY_UNCONFIRMED`와 `PLACE_KIND_UNRECOGNIZED`를 남긴다. `PLACE_KIND_UNRECOGNIZED`는 조회 unknowns 표지이고 error code가 아니다. `contracts/domain-vocabulary.json`은 error code·outcome·의무 kind만 담는다. `DomainVocabularyContractTest`는 선언됐지만 내보내지 않는 code를 거부하므로 어휘 항목을 추가하지 않았다(기존 `CUSTODY_UNCONFIRMED`와 같다). | `FulfillmentPostgresTest.unrecognizedPlaceKindIsUnknownCustodyAndSegmentEligibilityAppliesCustody`(새). RED: WAREHOUSE item-scope `DENIED`. GREEN: eligible 0, `UNKNOWN`, unknowns에 PLACE_KIND_UNRECOGNIZED·CUSTODY_UNCONFIRMED |
| SHOULD opus[6] segment-scope evaluateEligibility가 custody를 무시 | FIXED | `QualityEligibility.query`는 SELL·DISPATCH에 item-scope 읽기와 같은 custody 판정(`withCustody`)을 적용하고 조건 `CUSTODY`를 응답 conditions에 넣는다. DISPOSE·INTERNAL_MOVE는 그대로다. | 같은 test. INTERNAL_STORAGE+내부 custodian은 ALLOWED 100, WAREHOUSE는 UNKNOWN+PLACE_KIND_UNRECOGNIZED, CUSTOMER는 DENIED 0이고 unknowns가 없다. custodian 없는 내부 segment는 UNKNOWN+CUSTODY_UNCONFIRMED |
| SHOULD opus[2] PURCHASE 정정 branch가 죽은 코드 | FIXED | receipt canonical은 새 canonical로 supersede되지 않는다. 그래서 정산 hook이 영수 chain 정정에서 한 번도 실행되지 않았다. `AssessmentCorrectionImpact.apply`는 정정된 evidence에 연결된 PHYSICAL_RECEIPT canonical마다 settlement port를 호출한다. 그 receipt의 Match는 UNVERIFIED가 되고, 해당 canonical을 source로 하는 SETTLEMENT_DIFFERENCE가 owner와 함께 같은 거래에서 열린다. 이 root는 복원 canonical이 없으므로 MANAGER 면제로 닫힌다. | `SettlementCommandPostgresTest.correctedReceiptChainOpensAnOwnedSettlementDifferenceForItsPurchaseMatch`(새, 실제 system clock, request knownAt이 superseding event 기록 전). RED: OPEN 정산 의무 0. GREEN: 1 OPEN(source=receipt canonical, owner·nextAction·nextCheck, scope contributionState UNVERIFIED), getSettlement contributionState UNVERIFIED, differenceDutyOpen true |
| opus[7]/[10]/[19], astra[2]/[4]/[13] s4-settle-02 잔여 (중복) | FIXED | 위 MUST 1·2·3과 opus[2]로 닫았다. | 위 test |
| opus[8] s4-sales-04 | NOT_A_DEFECT | verdict가 RESOLVED이고 잔여는 opus[5]·[6]이다. | — |

## DEFERRED 잔여 (P3, 모두 fail-closed 또는 coverage)

owner는 논리 역할이다. Step은 그 항목을 맡을 시스템 단계 backlog다.

| 항목 | 이유 | owner · 맡을 Step |
|---|---|---|
| opus[9]/[18], astra[3]/[12] s4-recall-05 returnId·고객 leaf recordRecovery test | coverage 공백이며 제품 결함이 아니다. 반품 receipt에서 회수하는 실제 경로 fixture(인도·반품 승인·수령·회수 승인)가 크다. | trade/recall test owner(사용자 Step2 tests 담당과 trade/recall 구현자) · S5 진입 backlog |
| opus[11], astra[5] 첫 확정 뒤 연결된 상충 custody chain | `requireWarehouse`는 저장된 custodian만 본다. 뒤에 연결된 다른 custodian 증거에 대조 의무가 없다. 수정하려면 receipt·fulfillment의 공동 custody 재도출 계약과 새 의무 kind(어휘·closure)가 필요하다. | inventory/receipt custody owner · S5 진입 backlog |
| opus[22], astra[15] slot 없이 확정한 segment에 custody를 붙일 경로 없음(scenario B) | 예약·출고가 거부되는 fail-closed 상태다. 이미 분할·이동된 segment의 custody를 감사 가능한 별도 기록으로 고치는 명령이 필요하다. item-scope 조회는 CUSTODY_UNCONFIRMED를 보인다. | inventory/receipt custody owner · S5 진입 backlog |
| opus[20] DELIVERY_RESTRICTION_RESPONSE 재-scope | 의무는 있고 수량만 오래됐다. responsibility 소유의 scope revision 공개 명령·어휘·closure catalog 변경이 필요하다. | responsibility owner · S5 진입 backlog |
| opus[21], astra[14] traceRecall leg Work 인가 | 같은 조직 안의 read-scope 누출이고 쓰기 효과는 없다. 회귀 test에 Shipment·Leg·Cargo·검증 canonical chain fixture가 필요하다. | trade/recall query owner · S5 진입 backlog(MCP 조회 노출 전 필수) |
| opus[12] waiveObligation 결정 누락·승인 재사용 오류 code | 공용 `PolicyCommandGuard.verify`와 `DomainError.forbidden()`에서 나온다. code를 나누면 모든 승인 경로의 공개 오류 어휘와 native S3/S4 author 단언이 같이 바뀐다. | governance/policy guard owner · S5 진입 backlog(어휘 contract와 native 입력을 함께 변경) |
| opus[12], astra[6] RECALL_INVESTIGATION은 ADMIN 면제만 있음 | 계획이 조사 완료의 검증 증거 형식을 정하지 않는다. 의무에는 owner가 있다. | trade/recall owner · S5 진입 backlog(R 결정: 조사 결과 증거 정책) |
| opus[17], astra[11] RETURN_RECONCILIATION 범위가 다른 임시 반품(10을 8로 검증)을 닫을 수 없음 | `receiveReturn`은 관측 자신의 수량으로 canonical을 요구한다. resolution·waiver가 없어 OPEN으로 남는다. owner가 있어 fail-closed다. 더 적은 수량으로 닫고 잔여 의무를 남기려면 반품 계약 변경이 필요하다. | trade/returns owner · S5 진입 backlog |
| opus[12] T18(반품 새 receipt 독립 oracle) | 사용자 Step2 case 소유 범위다. 이 worker 소유가 아니다. | Step2 verification/cases owner · 사용자 Step2 후속 round |
| opus[13]/[16]/[24], astra[7]/[10]/[17] 순서가 뒤바뀐 부분 인도의 원장 날짜 | `FulfillmentStockPrimitives`는 ledger transfer를 `max(validFrom, at)`으로 미룬다. 적법성은 발생 시점 leaf로 판정한다. 원장 날짜를 고치려면 inventory primitive의 valid-time 분할 계약을 바꿔야 한다. | inventory ledger owner · S5 진입 backlog |
| opus[14]/[23], astra[8]/[16] 조직 전체 경계 500개 상한 | segment와 무관한 경계까지 모아 500을 넘으면 `INSUFFICIENT_ELIGIBLE_QUANTITY`로 거부한다(fail-closed). 경계 row를 segment의 item/lot/place/root로 거르는 filter와 그 경합 test가 필요하다. | inventory fulfillment owner · S5 진입 backlog |
| opus[15], astra[9] 회수 재-scope 뒤 옛 RECALL_EXCLUDED_SCOPE | 새 scope가 포함한 범위의 제외 의무를 무효화하거나 줄이지 않는다. 의무에 owner가 있다(ADMIN 면제 가능). | trade/recall owner · S5 진입 backlog |
| opus[25], astra[18] RECOVERED 전제가 조사 사이에 공유됨 | 조사 A의 회수가 조사 B의 폐기 전제를 채운다. 조사별 제한 또는 명시 대조 계약이 필요하다. | trade/recall owner · S5 진입 backlog |
| opus[26], astra[19] 출고된 적 없는 재고의 CONSUMED_LOST/EXCEPTION | heldQuantity를 회수 겹침 범위로만 검사한다. 전체 요청 범위 검사와 회수 fixture test가 필요하다. | trade/recall owner · S5 진입 backlog |
| opus[27] 중복 반품 projection | `ReturnQueries`는 관측 자신의 Receipts로 투영한다. 해소된 RETURN_RECONCILIATION·canonical을 따라 투영해야 한다. 조회만 다르고 재고 효과는 없다. | trade/returns query owner · S5 진입 backlog |
| opus[28], astra[20] ManagementCoverage가 한 차원만 포함해도 통과 | 기존 정책이고 문서화되지 않았다. allMatch로 바꾸면 모든 MANAGER 결정 경로의 인가가 바뀐다. 정책 결정이 먼저 필요하다. | governance/identity owner · S5 진입 backlog(R 결정: 관리 범위 정책) |

## 다른 owner에게 알릴 사항

- `verification/cases/C1/fixtures/custody-not-sale.json`·`revoked-basis.json`은
  Place kind `INTERNAL_WAREHOUSE`·`PORT`를, `verification/cases/C2/...`는
  `WAREHOUSE`·`PORT`를 쓴다. 이번 opus[5] 수정으로 이 kind들은 OUTSIDE가
  아니라 UNKNOWN(PLACE_KIND_UNRECOGNIZED)이다. eligible은 그대로 0이고
  상태와 unknowns만 바뀐다. 이 case들은 Step2 소유이며 이 worker는
  `./verify scenarios --actual`을 실행하지 않았다.
- `FixtureInstaller`의 Place kind 기본값 `WAREHOUSE`는 바꾸지 않았다.
  기본값을 INTERNAL_STORAGE로 바꾸면 kind를 적지 않은 fixture가 판매
  가능 재고처럼 보이게 되므로 oracle을 약하게 만든다. kind 없는 native
  fixture는 `verification/actual/s2/fixture.json`의 Place 하나다. S2 native
  흐름은 적격량을 단언하지 않는다(아래 실행 결과).
- 조정 제안 hash에 `dutyRootId`가 들어갔다. 공개 slot `dutyRootId`는
  선택이다. 열린 정산 의무가 둘 이상이면 필수다. native S1–S4 흐름은
  `recordSettlementAdjustment`를 쓰지 않는다.

## 실행 증거

환경: `. /Volumes/VideoStore/Developer/.mulino-tools/env.sh`, `$MULINO_SLOT`,
Testcontainers PostgreSQL. 집중 명령은
`$MULINO_SLOT ./mvnw -B -ntp -f backend/pom.xml -Dmulino.evidence.blob-root=$(mktemp -d) -Dtest=<classes> test`다.
결과 형식은 run/fail/error/skip이다. log 요약은 `raw/`에 있다.

| 시점 | classes | exit | 결과 | log |
|---|---|---|---|---|
| RED A: 새 test + baseline 제품 코드(`backend/src/main`, `contracts` stash) | FulfillmentPostgresTest 새 7개, SettlementCommandPostgresTest 새 1개 | 1 | 8/8/0/0. 위 표의 RED 열 | raw/red-summary.log |
| RED B: MUST 2만 되돌린 코드(1·3 등은 적용) | FulfillmentPostgresTest#matchFixed* | 1 | 1/1/0/0. `MATCHED`, received 30 | raw/red-b-summary.log |
| 집중 GREEN 1차 | FulfillmentPostgresTest 45, SettlementCommandPostgresTest 15, QualityPostgresTest 10, ReceiptGateway 16, ReturnGateway 16, RecallGateway 13, EvidenceGatewayTest 17, EvidenceReconciliationTest 6, SalesCommandPostgresTest 6, DeliveryCorrectionTest 6, InventoryPostgresTest 9, DomainVocabularyContractTest 4, SettlementStateTest 4, S4DeliveryCorrectionContractTest 3, S4SettlementFactsTest 1, AssessmentCorrectionLinkTest 3 | 1 | 174/0/1/0. AssessmentCorrectionLinkTest가 mock을 옛 request context로 고정했다. 기대를 넓힌 context(`c.knownThrough(canonical.recordedAt)`)로 고친 뒤 3/0/0/0 | raw/focused-summary.log |
| 전체 1차 `clean package` | 전체 | 1 | 509/0/2/0. `CompletionCoverageGatewayTest` 2건: fixture가 lifecycleMode 기본값 IMPORTED인 ACTIVE follow-up Work를 넣는다. 수정 전에는 시계 결함 때문에 방금 연결한 canonical이 보이지 않아 이 Work가 무효화 대상에서 빠졌다. 이제 무효화하므로 trigger `Imported S1 work remains immutable`가 거부했다. fixture Work를 `COMMAND` lifecycle로 고쳤고 19/0/0/0 | raw/completion-coverage-summary.log |
| 전체 최종 `./mvnw -B -ntp -f backend/pom.xml clean package` (코드 `e350b814`) | 전체 | 0 | 509/0/0/0, BUILD SUCCESS(기준선 501 + 새 8) | raw/full-package-summary.log |

native는 코드 commit `e350b814`의 clean tree에서 순서대로 실행했다.
`python3 verification/actual/sN/build.py` 뒤
`$MULINO_SLOT ./verify actual-sN /tmp/s4j-native/sN/run`이다(S1은 build
없음). blob 디렉터리 권한 문제는 일어나지 않았다.

| 실행 | build exit | verify exit | receipt status | codeCommit | receipt 사본 |
|---|---|---|---|---|---|
| actual-s4 | 0 | 0 | PASS | e350b814 | raw/native-s4/run-receipt.json |
| actual-s3 | 0 | 0 | PASS | e350b814 | raw/native-s3/run-receipt.json |
| actual-s2 | 0 | 0 | PASS | e350b814 | raw/native-s2/run-receipt.json |
| actual-s1 | — | 0 | PASS | e350b814 | raw/native-s1/run-receipt.json |

native 결과는 한정된 custody 증거다. coverage manifest의 PASS가 아니다.

schema·migration은 바꾸지 않았다(조정 root 결합은 hash로 했다). 그래서
schema parity 재생성도 하지 않았다.

## NOT_RUN

- NOT_RUN: `./verify scenarios --actual`(Step2 case, coordinator 통합 단계).
- NOT_RUN: T19 독립 oracle, V2/V3 경합 profile.
- NOT_RUN: 위 DEFERRED 항목의 수정과 test.
