# s4h-sales: S4 판매·출고·인도 review 지적 수정

사용자 Step 3(새 시스템 구현, Claude Opus medium), 시스템 S4 판매 범위다.
`step3-s4h-baseline`(=`2e1d90cc`)에서 branch `step3/s4h-sales`를 만들었다.
입력은 `.mulino-tools/s4review/sales.json`의 confirmed 15건과
unverifiedP3 4건이다. 같은 근본 원인은 한 번만 고쳤다. 다른 S4 fix
worker 셋이 형제 worktree를 쓰며 통합은 coordinator가 한다.

소유: `application/inventory/**`, `domain/inventory/**`(Return·Recall
primitive 제외), `application/quality/**`, `application/trade/sales/**`,
`domain/trade/sales/**`, `sales.cds`, migration V30, 그리고 지정 test.
소유 밖 파일은 고치지 않았다. 예외는 schema 재생성 도구
`docs/execution/s4c-settlement/regenerate-schema-compatibility.py`의 새 열
review 사유 한 항목과 그 산출물 `docs/execution/s4-integration/
schema-compatibility.json`이다(작업 지시의 재생성 절차).

## 지적별 결과

| id | 판정 | 원인과 수정 | 회귀 test |
|---|---|---|---|
| s4-sales-00 (P1) | FIXED | 출고가 현재 시각 적격만 검사했다. 발생 시각이 pick보다 앞서면 `REJECTED/TYPE_INVALID`. 발생 시각부터 현재까지의 모든 권한 경계(제한·처분 근거의 validFrom·validUntil·releasedAt, LOT 만료, 평가가 돌려준 nextValidityBoundary)에서 SELL·DISPATCH가 배분 범위를 덮지 않으면 `INSUFFICIENT_ELIGIBLE_QUANTITY` (§4.2, §6, 늦은 sweeper도 guard가 막는다) | `dispatchDatedInsideAnEarlierHoldWindowIsRejectedWithZeroEffect`, `dispatchSpanningAnUnsweptSaleBasisGapIsRejectedWithZeroEffect` |
| unverifiedP3-2 | FIXED | 위와 같은 원인(발생 시각 < pick·배분 생성). pick 하한으로 고쳤다 | 같은 test |
| s4-sales-01, -02 (P1) | FIXED | 인도 정당량이 DISPATCH만 평가했다. 인도 시각의 SELL∩DISPATCH(고객별) 교집합으로 계산한다. SELL 보류·SELL 근거 철회 뒤 인도는 기여0과 `DELIVERY_RESTRICTION_RESPONSE`를 남긴다(§6 사실 기록과 실행 권한, T17/C1) | `postDispatchSellOnlyHoldMakesTheDeliveryAViolationWithResponseDuty`, `postDispatchSellBasisRevocationMakesTheDeliveryAViolationWithResponseDuty` |
| s4-sales-03, -06, -13, -14 | FIXED | 새 부족 의무를 과거 최대 부족(high-water mark)으로만 열었다. 현재 부족 `[0,deficit)` 중 OPEN·WAIVED 의무가 덮지 않는 구간에 의무를 연다. RESOLVED는 당시 현재 정정으로만 해소됐고 새 정정이 그것을 대체하므로 덮지 않는다. 면제(WAIVED)는 부활하지 않는다. 새 root의 source는 정정 사건이라 옛 RESOLVED root로 dedupe되지 않는다(§4.3, §5.3, C4) | `correctedDeficitThatReemergesAfterItsRestorationResolutionGetsANewDuty`, `DeliveryCorrectionTest` 2건, 기존 C4 test 유지 |
| s4-sales-04 (P1) | FIXED(적격), 보유량은 미변경 | 품목 범위 판매 적격이 운송·고객 장소 물량을 포함했다. `QualityEligibility.read`가 TRANSIT·CUSTOMER·SUPPLIER 장소 segment를 판매 적격·미예약 적격에서 뺀다(§13.3 E1 현재 판매 적격0, §4.2). 보유량(heldQuantity)은 §4.2 "현재 위치의 active physical segment"를 그대로 따라 고객 장소 물량을 포함한다. 정의 변경은 결정이 필요해 하지 않았다 | `itemScopeSaleEligibilityExcludesDispatchedAndDeliveredStockInNounAndVerbReads` |
| s4-sales-05 (P2) | FIXED | 정정 인정량이 `min(q, 정당량)`이었다. 확정 때 정당 좌표를 `Deliveries.legitimateRangesJson`(V30)에 남기고 정정 구간 `[start,start+q)`와 교차한다. 좌표가 없거나 합이 정당량과 다르면 보수적 하한 `max(0, 정당량-(원량-q))`(§4.2) | `correctionCreditsOnlyTheExactLegitimateCoordinatesOfTheCorrectedInterval`, `DeliveryCorrectionTest` 2건 |
| s4-sales-07 (P2) | FIXED, 원장 시각 제한 있음 | 늦게 확정된 앞선 부분 인도가 영구 거부됐다. 정당성은 인도 시각에 그 범위를 가진 운송 leaf로 판정하고, 원장 이동은 현재 leaf 시작 시각(=먼저 확정된 인도 시각)으로 기록한다. Delivery·DeliveryTransfer의 occurredAt은 실제 시각이다. 그래서 asOf가 두 인도 사이인 재고 조회는 그 10을 아직 운송 중으로 보인다(§6, D06) | `partialDeliveriesConfirmedOutOfChronologicalOrderAreBothReconciled` |
| s4-sales-08, -11 (P2) | FIXED | 인도를 출고 당시 line의 dueAt으로 판정했다. 같은 주문의 현재 revision line(`SalesCommands.currentLine`)으로 dueAt·목적지·고객을 판정한다. 납기 연장 안 인도는 기여, 앞당긴 납기 뒤 인도는 위반 의무(§6 판매: 인도 끝점은 주문 계약대로) | `deliveryInsideAnAgreedDueDateExtensionCountsAndADeliveryAfterAShortenedDueDateDoesNot` |
| s4-sales-09, -12 (P2) | FIXED | Observation·Delivery·DeliveryCorrection의 effectiveAt이 기록 시각이었다. 발생 시각으로 저장한다. Observation 확정 상태는 knownAt에 보이는 Delivery로 투영한다(현재 행 update는 현재 상태 소비자 때문에 유지)(§3.1, §3.4, §4.3) | `deliveryReadsUseOccurrenceTimeAndConfirmationVisibleOnlyFromItsKnownAt` |
| s4-sales-10 (P2) | FIXED | replace는 원배분 segment scope를, 출고는 운송 PLACE를 인가하지 않았다. 각각 별도 `authorizeScopes`(§7.1, §6) | `dispatchRequiresAuthorityOverTheTransitDestination`, `replaceAllocationRequiresAuthorityOverTheReplacedAllocationScope` |
| unverifiedP3-0 | FIXED | 예약 startQuantity와 인도 관측 startQuantity가 품목 소수 자릿수를 검사하지 않았다 | `reservationCoordinateFinerThanTheItemScaleIsRejected`(예약만; 관측 경로 test 없음) |
| unverifiedP3-1 | NOT_FIXED | 실재한다. 정정이 `DELIVERY_RESTRICTION_RESPONSE`의 수량 범위를 재평가하지 않는다. 의무 범위 수정 API는 responsibility 소유이고 싸지 않다. coordinator 요청으로 넘긴다 | - |
| unverifiedP3-3 | FIXED | `SalesRepository.fence`가 `org|key`를 써 gateway의 `org:key`와 다른 lock이었다 | 직접 test 없음(lock 동일성은 SQL 문자열 변경) |
| S1 native FAIL | FIXED | 현재 적격 정책이 없으면 확정 부분집합을 정할 수 없다. `eligibleQuantity`·`unreservedEligibleQuantity`를 null, 상태 UNKNOWN으로 둔다. 정책이 있는 미확인(E1 W, C1 철회 뒤)은 §6 "불명확 범위는 confirmed eligible에 넣지 않는다"와 §13.3 E1 "현재 판매 적격0"대로 확정 부분 0을 유지한다. `S1ReadIntegrationTest`의 "0" 단언을 null로 고쳤다 | `S1ReadIntegrationTest.twoEntrypoints…` |

## 검증

환경: `. .mulino-tools/env.sh`, 모든 Maven·verify는 `$MULINO_SLOT`.

### backend focused

```
$MULINO_SLOT ./mvnw -B -ntp -q -f backend/pom.xml test -Dtest="S1ReadIntegrationTest,DomainVocabularyContractTest,DeliveryCorrectionTest,FulfillmentPostgresTest,StockCommandPostgresTest,SalesCommandPostgresTest,S4DeliveryCorrectionContractTest,InventoryPostgresTest,QualityPostgresTest,SettlementCommandPostgresTest,ReturnGatewayPostgresTest,RecallGatewayPostgresTest,S3SharedIntegrationTest,S2WorkLifecyclePostgresTest,S2WorkActualIntegrationTest,S3RegulatoryActualTest,AssessmentCorrectionLinkTest,S4PinnedQuantityTest,S4SettlementFactsTest,ReceiptGatewayPostgresTest"
```

exit 0, 20 class 159 test, 0 failure·0 error·0 skipped(`raw/green-*.txt`).
FulfillmentPostgresTest 34(baseline 21 + 새 13), DeliveryCorrectionTest
6(+4), S1ReadIntegrationTest 8.

RED: 새 FulfillmentPostgresTest 13건은 main source·cds·V30을 `2e1d90cc`로
되돌리고 새 test만 둔 채 실행해 모두 실패했다(`raw/red-baseline-*.txt`).
대부분 old 동작 그대로의 단언 실패(APPLIED, 기여 20, eligible 100,
`Occurrence precedes the physical segment` 등)이고
`correctionCreditsOnly…`는 V30 열이 없어 SQL 오류였다(같은 동작의
단위 RED는 아래 한계). `S1ReadIntegrationTest`는 `expected <null> but
was <0>`로 실패했다. `DeliveryCorrectionTest` 새 4건은 생성자가 바뀌어
baseline에서 compile되지 않아 RED를 따로 보이지 않았다.

### schema parity

```
$MULINO_SLOT ./mvnw -B -ntp -q -f backend/pom.xml test -Dtest=S1ReadIntegrationTest   # 관찰(1865≠1866 실패, artifact 기록)
cp backend/target/s4-compatibility-observed.json docs/execution/s4h-sales/schema-observed-v30.json
python3 -I docs/execution/s4c-settlement/regenerate-schema-compatibility.py \
  docs/execution/s4h-sales/schema-observed-v30.json 2e1d90cc V1–V30
# {"columns": 1866, "s3": 1432, "s4Added": 434, "timestampWidening": 340,
#  "notNullStrengthening": 787, "observedArtifactSha256": "d4f7913a…"}
```

재생성 뒤 `S1ReadIntegrationTest` 8/0/0/0. 새 열은 nullable이다. 처음에는
NOT NULL로 만들었으나 소유 밖 `ReturnGatewayPostgresTest`가 Deliveries를
직접 insert해 8건이 깨졌다. fixture를 고치지 않고 nullable과 보수적
하한으로 바꿨다.

### native

code commit `d409700a`(fix commit), JAR sha256 `873e38bb8253…`(네 run 같음).

```
$MULINO_SLOT python3 verification/actual/s4/build.py
$MULINO_SLOT ./verify actual-s4 /tmp/mulino-s4h-suite-1
$MULINO_SLOT ./verify actual-s1 /tmp/mulino-s4h-s1-1
$MULINO_SLOT python3 verification/actual/s3/build.py
$MULINO_SLOT ./verify actual-s3 /tmp/mulino-s4h-s3-1
```

| run | flow | exit | status | assertion | raw |
|---|---|---|---|---|---|
| s4 suite | s4 flow.json(C1 304+E2 410+C4 392+E1 612) | 0 | PASS | 1718 | `raw/native-s4-suite/` |
| s1 | s1 | 0 | PASS | 21 | `raw/native-s1/` |
| s3 | s3 flow.json | 0 | PASS | 594 | `raw/native-s3/` |
| s2 | s2 | 0 | PASS | 48 | `raw/native-s2/` |

S4 assertion 수는 s4g 기록과 같다. 기대값을 줄이지 않았다. S1은 baseline에서
`Missing SELL eligibility must remain unknown`으로 FAIL이었고 이제 PASS다.
제품 응답 변화(단언 대상 아님): E1 `e1-noun`·`e1-verb`의 eligibleQuantity가
20(PARTIAL)에서 0(UNKNOWN)으로, `e1-delivery-revision`이 30에서 0으로 바뀌었다.
고객 장소 20과 W의 미확인30·보류40만 남기 때문이다. raw의 gzip은 mtime 0이며
bearer token·private key 문자열이 없다(`grep`/`zgrep` 0건). S2 native는
정책 미설정이라 eligibleQuantity가 null로 바뀌므로 `python3
verification/actual/s2/build.py` 뒤 `./verify actual-s2 /tmp/mulino-s4h-s2-1`로
따로 확인했다(PASS 48, 명사·동사 값은 둘 다 null로 같다).

## 한계와 NOT_RUN

- 전체 backend suite, harness suite, `./verify prepare`는 실행하지 않았다
  (coordinator 담당).
- 보유량(heldQuantity)은 고객 장소 물량을 계속 포함한다. E1 noun/verb의
  보유 100 vs W 80 정의는 결정이 필요하다. native E1 author script에
  noun/verb 적격 단언을 추가하지 않았다(author script 소유가 불분명).
- 늦게 확정된 앞선 부분 인도의 원장 이동 시각은 leaf 시작 시각으로
  당겨진다(위 s4-sales-07).
- `ApplicationQueries`의 world 조립은 중첩 `@Transactional` 읽기가
  FORBIDDEN을 던지면 transaction을 rollback-only로 만들어 전체 조회가
  `UnexpectedRollbackException`이 된다. test actor에 `getAssessment`가
  없을 때 관찰했다. 소유 밖이라 고치지 않고 test actor에 권한을 줬다.
- unverifiedP3-1(정정 시 제한 위반 의무 범위 재평가)은 미수정이다.
- V4 노출 면 열거, V2·V3 native 경합, 유료 모델·client UAT, BTP 배포는
  실행하지 않았다. native PASS는 한정된 custody 증거이며 coverage
  pipeline PASS가 아니다.
