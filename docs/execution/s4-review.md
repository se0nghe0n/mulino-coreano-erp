# S4 adversarial review

S4(판매·출고·인도·반품·회수·정산·의무)를 닫기 전에 Claude Opus xhigh와
Claude Fable low가 code commit `de8d19ea`(기록 `6fa7beb1`)를 4개
slice로 나눠 검토했다. P0–P2 지적마다 Opus xhigh skeptic이 반박을
시도했고 반박되지 않은 지적만 확정했다. 정적 읽기이며 runtime 실행이
아니다. 결합 검사 증거는 [step3-s4/de8d19ea](evidence/step3-s4/de8d19ea/summary.json)다.

| slice | reviewer | 판정 | 미검증 P3 |
|---|---|---|---|
| fulfillment-sales | opus-xhigh | FAIL | 1 |
| fulfillment-sales | fable-low | FAIL | 3 |
| returns-recall | opus-xhigh | FAIL | 2 |
| returns-recall | fable-low | FAIL | 3 |
| settlement-duties | opus-xhigh | FAIL | 1 |
| settlement-duties | fable-low | PASS | 4 |
| evidence-native | opus-xhigh | FAIL | 3 |
| evidence-native | fable-low | PASS | 3 |

확정 39건(P1 9, P2 29, P3 1), 반박 1건이다. 같은 근본 원인(정정 부족
의무의 high-water mark)을 여러 reviewer가 따로 지적한 항목이 있다.
원문과 반박 근거는 [evidence/s4-review](evidence/s4-review/)에 있다.

## 확정 지적과 수정 소유자

모든 수정은 사용자 Step 3 지정인 Claude Opus medium worker가
`step3-s4h-baseline`(`2e1d90cc`)의 독립 worktree에서 한다. native S1의
미확인 판매 적격 0 표기 결함은 sales worker에 함께 맡겼다.

### sales — `step3/s4h-sales` 출고·판매·재고 조회·S1 미확인 적격

| ID | 등급 | reviewer | 지적 | 위치 |
|---|---|---|---|---|
| s4-sales-00 | P1 | opus-xhigh | dispatchQuantity accepts any past occurredAt but checks SELL/DISPATCH eligibility only at the current clock, so an unauthorized past dispatch is recorded as a normal one | `backend/src/main/java/com/mulino/application/inventory/FulfillmentCommands.java:35` |
| s4-sales-01 | P1 | opus-xhigh | Delivery legitimacy only checks DISPATCH authority, so delivering after a SELL-only hold, SELL basis revocation/expiry, or regulatory SELL withdrawal counts as full fulfilment with no violation duty | `backend/src/main/java/com/mulino/domain/inventory/FulfillmentStockPrimitives.java:40` |
| s4-sales-02 | P1 | fable-low | Delivery legitimacy ignores SELL-only restrictions/revocations recorded after dispatch (T17/C1 gap) | `backend/src/main/java/com/mulino/domain/inventory/FulfillmentStockPrimitives.java:89` |
| s4-sales-03 | P1 | fable-low | Corrected-delivery deficit that recurs after a verified restoration gets no obligation (duty vanishes) | `backend/src/main/java/com/mulino/application/trade/sales/DeliveryCorrection.java:458` |
| s4-sales-04 | P1 | opus-xhigh | Stock already delivered to the customer counts as held and unreserved SELL-eligible at item scope; the E1 noun/verb views report eligible 20 / held 100 and the suite still passes | `backend/src/main/java/com/mulino/application/inventory/InventoryQueries.java:87` |
| s4-sales-05 | P2 | opus-xhigh | Delivery correction credits min(q, original legitimate), so it ignores the exact range and over-credits when the legitimate part is not at the start of the range | `backend/src/main/java/com/mulino/application/trade/sales/DeliveryCorrection.java:10` |
| s4-sales-06 | P2 | opus-xhigh | A deficit that comes back after a verified restoration has no duty: the new-deficit test uses the historical max of all corrections | `backend/src/main/java/com/mulino/application/trade/sales/DeliveryCorrection.java:11` |
| s4-sales-07 | P2 | opus-xhigh | Partial deliveries confirmed out of chronological order are permanently rejected, leaving the actual delivery stuck in transit with an unclosable reconciliation duty | `backend/src/main/java/com/mulino/domain/inventory/FulfillmentStockPrimitives.java:42` |
| s4-sales-08 | P2 | opus-xhigh | Deliveries are judged against the superseded line's dueAt after a revision that changed the due date while cargo was in transit | `backend/src/main/java/com/mulino/application/trade/sales/DeliveryCommands.java:22` |
| s4-sales-09 | P2 | opus-xhigh | Sales delivery records use recording time as effective time and change observation state in place, so asOf/knownAt reads disagree with inventory | `backend/src/main/java/com/mulino/application/trade/sales/DeliveryCommands.java:23` |
| s4-sales-10 | P2 | opus-xhigh | replaceAllocation never authorizes the replaced allocation's physical scope, and dispatch never authorizes its transit destination | `backend/src/main/java/com/mulino/application/inventory/FulfillmentCommands.java:28` |
| s4-sales-11 | P2 | fable-low | Delivery recognition is judged against the sales line frozen at dispatch, not the current order revision (dueAt changes ignored) | `backend/src/main/java/com/mulino/application/trade/sales/DeliveryCommands.java:440` |
| s4-sales-12 | P2 | fable-low | Deliveries/Observations valid-time projection uses the confirmation clock, so asOf reads disagree with the inventory ledger | `backend/src/main/java/com/mulino/application/trade/sales/SalesQueries.java:570` |
| s4-sales-13 | P2 | opus-xhigh | Corrected-delivery deficit high-water mark: a deficit that re-emerges after restoration-based resolution gets no duty | `backend/src/main/java/com/mulino/application/trade/sales/DeliveryCorrection.java:11` |
| s4-sales-14 | P2 | opus-xhigh | A corrected-delivery deficit is not re-created after the evidence that resolved it is superseded (98 -> 100 resolve -> 98 leaves no deficit duty) | `backend/src/main/java/com/mulino/application/trade/sales/DeliveryCorrection.java:11` |

### recall — `step3/s4h-recall` 반품·회수

| ID | 등급 | reviewer | 지적 | 위치 |
|---|---|---|---|---|
| s4-recall-00 | P1 | opus-xhigh | Re-scoping a recall drops all physical processing recorded under the previous version, and a DB constraint shared across versions makes recovered or disposed ranges impossible to close truthfully | `backend/src/main/java/com/mulino/application/trade/recall/RecallCommands.java:44` |
| s4-recall-01 | P1 | opus-xhigh | Recall DISPOSED destroys stock on a RECORD intent with no MANAGER stock-decrease decision and no human requirement | `backend/src/main/java/com/mulino/application/trade/recall/RecallCommands.java:44` |
| s4-recall-02 | P2 | opus-xhigh | Return provisional intake records the authorization rather than the observed return, and mismatched or duplicate arrivals leave a RETURN_RECONCILIATION duty that can never be closed | `backend/src/main/java/com/mulino/application/trade/returns/ReturnCommands.java:33` |
| s4-recall-03 | P2 | opus-xhigh | Recall reads are not bitemporal: the Investigation row is mutated in place and historical getRecall/getRecalls return FORBIDDEN or the current state | `backend/src/main/java/com/mulino/application/trade/recall/RecallQueries.java:15` |
| s4-recall-04 | P2 | opus-xhigh | The authorizing manager's current permission (and the QC/ADMIN disposition authority) is re-checked with any-value scope semantics, weaker than the per-value check the gateway applied | `backend/src/main/java/com/mulino/application/trade/returns/ReturnCommands.java:61` |
| s4-recall-05 | P2 | opus-xhigh | Recall ADMIN gating and the realistic recovery paths are untested; one backend closure-denial test is vacuous | `backend/src/test/java/com/mulino/application/evidence/RecallGatewayPostgresTest.java:40` |
| s4-recall-06 | P2 | fable-low | decideReturnDisposition is unreachable with honestly dated evidence and has zero end-to-end coverage | `backend/src/main/java/com/mulino/application/trade/returns/ReturnEvidence.java:19` |
| s4-recall-07 | P2 | fable-low | Recall scope re-versioning orphans physical recovery/disposal facts and permits contradictory terminal partitions across versions | `backend/src/main/java/com/mulino/application/trade/recall/RecallCommands.java:40` |
| s4-recall-08 | P2 | fable-low | Terminal CONSUMED_LOST/EXCEPTION may be recorded over a range already RECOVERED into the warehouse; ledger and closure disagree | `backend/src/main/java/com/mulino/application/trade/recall/RecallCommands.java:42` |
| s4-recall-09 | P2 | fable-low | Provisional return observations are never deduplicated; duplicate or already-returned-range observations leave RETURN_RECONCILIATION duties that can never close | `backend/src/main/java/com/mulino/application/trade/returns/ReturnCommands.java:33` |

### settle — `step3/s4h-settle` 정산

| ID | 등급 | reviewer | 지적 | 위치 |
|---|---|---|---|---|
| s4-settle-00 | P1 | opus-xhigh | Invoice match uses actual receipt quantity, not the recognized line contribution, so excess receipts are billed as MATCHED/SATISFIED | `backend/src/main/java/com/mulino/application/trade/SettlementFactsAdapter.java:37` |
| s4-settle-01 | P1 | fable-low | SETTLEMENT_DIFFERENCE duty resolves on zero remaining amount while the product's own settlement result still says UNSATISFIED (quantity difference ignored by the resolver) | `backend/src/main/java/com/mulino/application/trade/settlement/SettlementResponsibilities.java:29` |
| s4-settle-02 | P2 | opus-xhigh | Settlement result is never re-evaluated after a delivery or receipt correction; getSettlement stays SATISFIED | `backend/src/main/java/com/mulino/application/trade/settlement/SettlementQueries.java:19` |
| s4-settle-03 | P2 | opus-xhigh | SETTLEMENT_DIFFERENCE closes on amount alone: a zero-amount 'adjustment' resolves a pure quantity/price mismatch and bypasses the typed waiver | `backend/src/main/java/com/mulino/application/trade/settlement/SettlementResponsibilities.java:30` |
| s4-settle-04 | P2 | opus-xhigh | Adjustments confirmed after closure, or on MATCHED invoices, create ownerless settlement differences | `backend/src/main/java/com/mulino/application/trade/settlement/SettlementCommands.java:59` |
| s4-settle-05 | P2 | opus-xhigh | The FX snapshot in the source original is silently dropped when the caller omits the fxSnapshot slot | `backend/src/main/java/com/mulino/application/trade/settlement/SettlementCommands.java:48` |
| s4-settle-06 | P2 | opus-xhigh | CREDIT_NOTE/CORRECTION invoices get no persisted review duty and are invisible on the original invoice | `backend/src/main/java/com/mulino/application/trade/settlement/SettlementCommands.java:49` |
| s4-settle-07 | P2 | opus-xhigh | Every matched invoice records INVOICED, so duplicate and DOMESTIC_TAX invoices double-count invoiced quantity and block sales revision | `backend/src/main/java/com/mulino/application/trade/settlement/SettlementCommands.java:57` |
| s4-settle-08 | P2 | fable-low | Adjustments can be proposed/confirmed after the duty is RESOLVED, leaving a non-zero settlement difference with no open duty (duty vanishes, cannot be re-created) | `backend/src/main/java/com/mulino/application/trade/settlement/SettlementCommands.java:59` |
| s4-settle-09 | P2 | fable-low | PURCHASE-scope matchInvoice reconciles against the stored ReceiptCredit quantity without re-verifying that the receipt canonical occurrence is still current (SALE path does); a superseded receipt fact can be matched as MATCHED | `backend/src/main/java/com/mulino/application/trade/SettlementFactsAdapter.java:36` |

### native — `step3/s4h-native` native 인수·명사/동사 대조·수령 보관 충돌

| ID | 등급 | reviewer | 지적 | 위치 |
|---|---|---|---|---|
| s4-native-00 | P2 | opus-xhigh | The E1 noun/verb 'same ID and snapshot' parity check is tautological | `backend/src/main/java/com/mulino/application/core/ApplicationQueries.java:68` |
| s4-native-01 | P2 | opus-xhigh | A second verified source naming a different receiving custodian is silently accepted when the confirm omits receivingCustodianId (custody conflict check is caller opt-in) | `backend/src/main/java/com/mulino/application/trade/receipt/ReceiptCommands.java:55` |
| s4-native-02 | P2 | fable-low | E1 noun/verb entry-point parity is tautological; API surfacing of the three human duties is never asserted | `verification/actual/s4/e1-final.json:153` |
| s4-native-03 | P3 | opus-xhigh | The humanDuties oracle matches by kind only; C1's 'existing responsibility retained' passes on an unrelated purchase-Work QUALITY_REVIEW, and the suspended sales reservation has no duty | `verification/harness/src/main/java/org/mulino/verification/actual/NativeS4TradeMain.java:112` |

## 종료 조건

모든 확정 지적이 FIXED 또는 근거 있는 NOT_A_DEFECT가 되고, 통합본에서
backend 전체·harness·prepare·native S1–S4가 통과하고, 같은 두 reviewer의
closure 검토가 PASS해야 S4를 닫는다.
