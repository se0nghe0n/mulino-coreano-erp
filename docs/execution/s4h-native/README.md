# s4h-native: S4 native oracle과 수령 custody 충돌 수정 기록

사용자 Step 3(구현, Claude Opus medium) S4 adversarial review의
evidence-native 지적(`/Volumes/VideoStore/Developer/.mulino-tools/s4review/native.json`)
을 고친 worker 기록이다. baseline은 tag `step3-s4h-baseline`(`2e1d90cc`),
worktree는 `mulino-ontology-step3-s4h-native`, branch는
`step3/s4h-native`다. push·merge는 하지 않았다. 통합은 coordinator가 한다.

## 결론

- 확인된 지적 4건(s4-native-00~03)은 두 원인이다. E1 world read를
  자기 자신과만 비교한 oracle(00·02)과, 종류만 세는 humanDuties(03)를
  독립 JDBC 원장과 정확한 결속으로 바꿨다. 수령 custody 충돌(01)은
  제품에서 고쳤다.
- native S4 E2·C1·C4 flow와 actual-s3·actual-s2는 PASS다. 전체
  suite는 E1 끝의 `e1-noun-eligibility`(item 전체 eligible 0)에서만
  FAIL한다. 그 앞 E1 단계는 새 ledger 비교를 포함해 모두 통과했다.
  이 FAIL은 아직 통합되지 않은 sales worker의 InventoryQueries 수정
  (고객에게 인도된 재고를 W 보유·적격에서 제외)에 달려 있으므로
  `EXPECTED_PENDING_INTEGRATION`이다.

## 지적별 결과

| ID | 결과 | 내용 |
|---|---|---|
| s4-native-00 | FIXED (oracle) | e1-noun·e1-verb가 서로 같은지만 보던 비교를 독립 원장 대조로 바꿨다. 각 응답의 `obligations`는 `mulino_work_read_obligationreferences`의 {WORK,SALES_WORK} 행과 ID 집합·필드별로 같아야 한다(`ledgerRows`). `ownerIds`는 원장 Works owner와 OPEN·valid 의무 owner의 합집합(`ledgerOwners`), `workIds`는 정확히 {WORK,SALES_WORK}(`sameSet`), `contributions`는 `mulino_work_workcontributions` 원장과 같아야 한다. 동사는 `data.ID`=`data.workId`=`$WORK`, `kind`=PURCHASE를 단언한다. 이 내용 검사를 통과한 뒤에 snapshot·의무·owner·근거·보유·적격량의 명사·동사 일치를 확인한다. `ApplicationQueries`는 바꾸지 않았다. 같은 world builder로 일치를 보장하는 방식은 §3.4의 정당한 구현이고, reviewer도 제품 결함은 없다고 판정했다(NOT_A_DEFECT, 제품 부분). |
| s4-native-01 | FIXED (제품) | `ReceiptCommands`는 이제 모든 confirm(신규·중복, slot 유무와 무관)에서 canonical의 모든 VERIFIED chain이 지명한 보관 주체 집합을 읽는다. 둘 이상이면 HELD `EVIDENCE_CONFLICT`이고 효과는 0이다. 중복 출처의 증거가 segment에 기록된 custodian과 다르면 역시 `EVIDENCE_CONFLICT`다. slot이 있으면 집합이 정확히 {slot}이어야 한다(기존 `EVIDENCE_UNVERIFIED`). 근거는 §4.1–§4.3과 D07이다. |
| s4-native-02 | FIXED (00과 같은 원인) | API 응답에서 QC·반품 3종·정산 의무를 Work·출처에 결속하고, owner는 인간이어야 하며 nextAction·nextCheck가 있어야 한다. 이 humanDuties 검사를 e1-noun과 e1-verb에 각각 적용했다. actors 원장은 같은 observe에서 bind한다. |
| s4-native-03 | FIXED (oracle) / NOT_A_DEFECT (제품) | `humanDuties`는 이제 `duties:[{kind, where, count}]`만 받는다. 의무를 Work·출처·수량에 결속하고 count를 정확히 센다. 결속되지 않았거나 중복된 같은 종류의 OPEN 의무가 있으면 FAIL이고, `kinds`만 쓰면 거부한다. C1은 철회된 basis(`$C1_COMMERCIAL`, 수량40, COMMERCIAL·SELL)에, C4는 `$DELIVERY`의 0–2 정정 부족(`$SALES_WORK`)에, E2는 `$E2_SCOPE` 잔여 25·제외 10과 `$E2_RESIDUAL_DUTY`에 결속했다. 정지된 판매 배분에 별도 의무가 없다는 제품 쪽 주장은 verdict에 따라 계획 요구가 아니므로 NOT_A_DEFECT다. 기존 책임은 C1의 SUSPENDED 배분 20과 `reservationResponsibilityQuantity` 20으로 이미 단언한다. |

### unverifiedP3

| 지적 | 결과 | 내용 |
|---|---|---|
| custody 효과를 native로 관찰하지 않음 | FIXED | e1-final-independent가 현재 segment 4개를 검사한다. W의 수령 재고 2개는 custodian=`$supervisor`이고 owner는 null이다. W의 반품 10과 고객 장소 재고는 custodian·owner가 모두 null이다. |
| bank-effect 0 oracle이 공허함 | PARTIAL | E1에 `mulino_runtime_outbox`가 1행뿐이고 그것이 `dispatchPurchaseOrder`라는 단언을 추가했다. 정산이 은행·지급 outbox를 만들면 FAIL한다. `SettlementCommands`의 상수 `bankEffect:"0"` 필드는 소유 밖이라 cross-owner로 넘긴다. |
| 음성 명령이 아무 거부 사유나 수용함·assertion 수 부풀림 | PARTIAL | e2 반복 회수·폐기는 `RECALL_EVENT_ALREADY_APPLIED`와 효과 `{}`를 단언한다. report는 authored oracle assertion 수(`authoredOracleAssertions`)를 plumbing check(`boundedAssertions`)와 따로 낸다. c4 waiver의 두 거부는 실제로 FORBIDDEN `Unavailable scope`이다. 이 코드는 결정 부재를 구별하지 못하고, 올바른 사유는 responsibility 소유자가 정해야 하므로 고치지 않았다. |
| 중복 출처 slot 없는 confirm (scenario A) | FIXED | s4-native-01과 같다. |
| 첫 confirm이 slot을 생략하면 custody를 뒤에 붙일 수 없음 (scenario B) | NOT_FIXED | 지금도 HELD로 fail-closed이고 잘못된 효과는 없다. 이미 분할·이동됐을 수 있는 segment에 custody를 뒤늦게 기록하려면 감사되는 별도 기록 단계가 필요하다. 싸게 고칠 수 없어 남긴다. |
| acceptance-contract.json을 아무도 읽지 않음 | FIXED | `assertionMap`을 추가해 case key마다 단언 action을 지정했다. runner는 `contractCases`(ASSERTED/NOT_RUN/UNMAPPED)를 내고, flow의 `requiredCases`에 속한 key를 단언하지 못하면 FAIL한다. `S4WorldOracleTest.acceptanceContractKeysMapToAuthoredAssertions`는 매핑된 action마다 실제로 authored assertion이 있는지 정적으로 검사한다. E2 physical/qcHold, postDispatchRecall, V2/V3은 native 단언이 없어 UNMAPPED(NOT_RUN)이다. |
| linkCanonicalOccurrence가 Reconciliations 행을 두 번 기록함 | CONFIRMED, 소유 밖 | c1-1 증거에서 claim별 MATCHED 행이 2개였다(`mulino_evidence_reconciliations` 36행). `EvidenceReconciliation`은 소유 밖이라 cross-owner로 넘긴다. |

## 변경 파일

- `backend/src/main/java/com/mulino/application/trade/receipt/ReceiptCommands.java`:
  `evidencedCustodians`가 모든 chain의 보관 주체 진술을 모은다. JSON이
  아닌 출처는 custody를 진술하지 않은 것으로 본다.
- `backend/src/test/java/com/mulino/application/evidence/ReceiptGatewayPostgresTest.java`:
  `secondVerifiedSourceNamingAnotherCustodianHoldsEveryConfirmWithoutEffect`,
  `conflictingCustodySourcesLinkedBeforeFirstConfirmCreateNoStock`.
- `verification/harness/src/main/java/org/mulino/verification/actual/S4WorldOracle.java`(신규),
  `NativeS4TradeMain.java`, `S4JdbcObservation.java`(`mulino_work_` 표 관찰),
  `NativeS4FlowAliases.java`(assertion 전체 alias 검사).
- `verification/harness/src/test/java/org/mulino/verification/actual/S4WorldOracleTest.java`(신규, 9),
  `ActualS4FlowAliasTest.java`(+1).
- author 스크립트 `author.py`, `c1-author.py`, `c4-author.py`, `e2-author.py`와
  그 생성물 `e1-final.json`, `c1-sale-revocation.json`,
  `c4-history-return-correction.json`, `e2-flow.json`. 생성물은
  `python3 -B verification/actual/s4/{author,c1-author,c4-author,e2-author}.py`
  로만 갱신했다. 다시 실행해도 결과가 같다.
- `verification/actual/s4/acceptance-contract.json`: `assertionMap`.

## 검사

| 검사 | 결과 |
|---|---|
| `ReceiptGatewayPostgresTest`(수정 전 코드, 새 test 포함) | RED: 16 중 2 FAIL(새 test 2개가 APPLIED) |
| `ReceiptGatewayPostgresTest` 16, `ReturnGatewayPostgresTest` 8, `FulfillmentPostgresTest` 21 | PASS |
| `ReceiptResidualGatewayPostgresTest` 19, `SettlementCommandPostgresTest` 7, `RecallGatewayPostgresTest` 6 | PASS |
| `S4WorldOracleTest`(이전 flow JSON) | RED: 2 FAIL(`committedFlowsBindEveryHumanDuty`, `e1WorldReadsAreCheckedAgainstTheLedger`) |
| harness `org.mulino.verification.actual.*Test` 37개 | PASS |

backend test는 실제 PostgreSQL(Testcontainers)에서 실행했다. 명령은
`$MULINO_SLOT ./mvnw -B -ntp -q -f backend/pom.xml -Dtest=... test`이고,
harness는 `-f verification/harness/pom.xml`로 같은 방식으로 실행했다.

## native 실행 (commit `99eb026e`)

`$MULINO_SLOT python3 verification/actual/s4/build.py` 뒤 실행했다.
raw는 `raw/<attempt>/`, 요약은 `raw/index.json`에 있다.

| attempt | 명령 | 결과 | bounded / authored |
|---|---|---|---|
| suite-1 | `./verify actual-s4 /tmp/s4h-native-suite.*` | FAIL exit1: `e1-noun-eligibility expected decimal "0" observed "20"`. EXPECTED_PENDING_INTEGRATION(sales fix). e1-final-independent, e1-noun, e1-verb는 모두 실행돼 통과했다 | 2172 / 132 |
| e2-1 | `ACTUAL_FLOW_REF=verification/actual/s4/e2-flow.json ./verify actual-s4 …` | PASS. E2 contract key 8개 ASSERTED, 2개 UNMAPPED | 426 / 66 |
| c1-1 | `ACTUAL_FLOW_REF=…/c1-flow.json` | PASS. C1 key 3개 ASSERTED | 309 / 27 |
| c4-1 | `ACTUAL_FLOW_REF=…/c4-flow.json` | PASS. C4 key 5개 ASSERTED | 397 / 38 |
| s3-1 | `python3 verification/actual/s3/build.py` 후 `./verify actual-s3` | PASS | 594 / - |
| s2-1 | `python3 verification/actual/s2/build.py` 후 `./verify actual-s2` | PASS | 48 / - |

suite가 E1에서 멈췄으므로 E2·C1·C4는 같은 commit의 개별 flow로
실행했다. 모든 run에서 backend 정지, container 제거, 임시 key·blob
삭제가 확인됐다.

## NOT_RUN과 남은 일

- 전체 S4 suite PASS: sales 수정을 통합한 뒤 coordinator가 다시 실행한다.
- `./verify prepare`, backend 전체 test, harness 전체 test, actual-s1:
  이 worker는 실행하지 않았다(coordinator 범위).
- schema 변경 없음. migration과 schema parity artifact는 손대지 않았다.
- cross-owner 요청:
  - responsibility: c4 waiver 무결정·재사용 거부가 FORBIDDEN 대신 결정
    부재와 단일 사용 소진을 구별하는 code를 내야 하는지 정한다. 다른
    OPEN 의무로 승인을 재사용하는 사례를 추가한다.
  - settlement: 응답의 상수 `bankEffect:"0"`를 제거하거나 의미를 문서화한다.
  - evidence: `linkCanonicalOccurrence`의 재검사가 두 번째
    Reconciliations 행을 기록하지 않게 한다. link 시점에 receipt custody를
    다시 검토한다.
  - fulfillment: `requireWarehouse`가 segment custodian의 증거 충돌을
    다시 확인한다.
  - docs: `docs/execution/s3-receipt/contracts.md`의 "중복 출처가 slot으로
    다른 보관 주체를 주장하면" 문장을 slot과 무관한 충돌 규칙으로 고친다.
