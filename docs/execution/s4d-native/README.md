# S4 native flow 실행 기록 (s4d-native)

## 결론

통합 제품 경로(branch `step3/s4d-native`, 최종 `4b63edf1`)에서 E1·E2·C4·C1
네 flow와 기본 suite는 모두 **FAIL**이다. 13개 commit으로 fixture·runner 결함을 고친
뒤 네 flow는 같은 지점, 즉 직접 수령한 창고 재고의 첫 예약
(`reserveQuantity`)에서 `REJECTED SCOPE_INELIGIBLE "Confirmed warehouse
custody required for fulfillment"`로 멈춘다. 원인은 두 제품 계약의 충돌이다.

- S3 receipt 계약(`docs/execution/s3-receipt/contracts.md`)은 "최초 접수의
  소유·보관 주체는 미확인으로 남긴다"고 정하고,
  `ReceiptGatewayPostgresTest`가 직접 수령 segment의 `ownerId`·`custodianId`가
  모두 null임을 assertion으로 고정한다.
- S4 통합 commit `91f78d61`(`FulfillmentCommands.requireWarehouse`)은 예약·
  교체에 내부 custodian이 확인된 창고 segment를 요구한다.
- 직접 수령 segment의 custodian을 기록하는 공개 명령은 없다. 운송 인계
  `recordHandover`는 segment custodian을 바꾸지 않고, transit 수령만
  transit segment의 custodian을 복사한다. 따라서 계획 E1의 "W에서 실제
  수령한 60 중 30을 판매·출고"는 현재 제품에서 도달할 수 없다.

계획 의미가 분명하지 않아 제품을 고치지 않고 보고한다. 결정 후보는
(a) 직접 창고 수령의 custodian을 확인 actor 또는 명시 slot으로 기록하고
S3 계약·test를 개정, (b) 수령 segment의 보관 주체를 기록하는 별도 증거
명령 추가, (c) 창고 kind만으로 이행을 허용하고 custodian 조건을 외부
보관에만 적용하는 것이다. 사용자 또는 coordinator의 결정이 필요하다.

이 하나의 차단 뒤에 무엇이 남는지 보려고 진단용 branch
`step3/s4d-custody-probe`에 (a)의 최소형
(`ReceiptStockPrimitives`: transit 없는 수령의 `custodianId`를 확인 actor로
기록, commit `ec7baabd`)만 얹고 같은 fixture 수정을 cherry-pick해 실행했다.
probe에서 네 flow와 기본 suite가 모두 **PASS**다(E1 612, E2 410, C4 321,
C1 304, suite 1647 bounded assertion, 실패0). probe commit은 S3 계약과
`ReceiptGatewayPostgresTest`의 null custodian assertion에 반하므로 통합
대상이 아니며 push하지 않았다. probe PASS는 제품 PASS가 아니다.

## 실행 명령과 환경

모든 Maven·verify는 `. /Volumes/VideoStore/Developer/.mulino-tools/env.sh`
뒤 `$MULINO_SLOT`로 한 번에 하나씩 실행했다.

```bash
$MULINO_SLOT python3 verification/actual/s4/build.py            # backend 변경 시
$MULINO_SLOT python3 verification/actual/s4/build.py --reuse-backend
ACTUAL_FLOW_REF=verification/actual/s4/<x>-flow.json \
  $MULINO_SLOT ./verify actual-s4 /tmp/mulino-s4d-<x>-<n>
$MULINO_SLOT ./verify actual-s4 /tmp/mulino-s4d-suite-<n>       # 기본 flow.json
$MULINO_SLOT ./mvnw -B -ntp -f verification/harness/pom.xml test # 434/0/0/0
$MULINO_SLOT python3 verification/actual/s3/build.py && \
  $MULINO_SLOT ./verify actual-s3 /tmp/mulino-s4d-s3-regression-1
```

Java 21.0.5, PostgreSQL 18.6(`postgres@sha256:4ef4dbc9…`) disposable
container, backend는 복사한 JAR(`run-receipt.json`의
`executedJarSha256`)을 loopback으로 실행했다. 같은 backend source의 전체
build도 JAR byte가 매번 달라(`e2307d3f…`, `c98fb782…`) JAR sha는 attempt별
receipt 값을 그대로 기록한다.

## 고친 fixture·runner 결함

모두 제품 판정이 계획과 일치하고 입력이 틀린 경우다. 기대값을 낮추거나
결과를 seed하지 않았다.

| commit | 증상(attempt) | 원인과 수정 |
|---|---|---|
| `184d4820` | E1 create-work HELD VERSION_UNSUPPORTED(`e1-1`) | author가 S4 verb를 추가하며 capability를 선언하지 않아 정의 전체가 VALID가 아니었다. 모든 verb capability를 선언한다 |
| `184d4820` | E2·C4·C1 setup NOT_RUN 23505(`*-1`) | 모든 fixture가 조직 alias `ORG`를 externalAlias로 썼다. setup마다 `<alias>@<setup id>`로 설치하고 JWT·clock actor를 그 별칭으로 바꾼다. S1–S3은 기본값 그대로다 |
| `75af1587` | E1 dispatch-s60-QC-match CONFLICT(`e1-2`) | DISPATCH 근거가 SELL 결정 ID를 재사용했다. 별도 uuid5 결정 ID. C4 UUID externalEventId 접두사도 제거 |
| `17001330` | E2/C4/C1 규제 제출 UNVERIFIED·HELD(`*-2`) | 원본 namespace와 규제 policy sourceNamespace 불일치 |
| `873df311` | E2 dispatch-QC-match CONFLICT(`e2-3`) | E2의 SELL/DISPATCH 결정 ID 재사용 |
| `710d0049` | pick TYPE_INVALID(`probe-*-1`) | 배분 명령 subject가 구매 Work였다. 판매 Work로 선언 |
| `9d509ab9` | 반품·QC 원본 match TYPE_INVALID(`probe-*-3`) | match/link subject를 원본 claim subject에서 도출(`subjects.py`), Invoice·Recall·RecallScope 명사 추가 |
| `7d048c31` | 반품 match UNVERIFIED(`probe-e1-4`), E2 HELD 기대(`probe-e2-4`) | installer가 document placeId를 빠뜨렸다. E2 거부 기대를 계획 §6 enum과 C1에 맞춰 REJECTED·SCOPE_INELIGIBLE·effects {}로 강화 |
| `1071c2f4` | 정산 "0.000000000000"(`probe-e1-5`), E2 옛 승인(`probe-e2-5`), C4 valueState PRESENT(`probe-c4-5`) | `decimalEquals`(정확한 수치 동치) 추가, 옛 승인 요청 dict 복사, valueState KNOWN |
| `59dd2e47` | C4 correctEvidence FORBIDDEN(`probe-c4-6`) | 정정 claim은 attachEvidence 권한이 필요하다 |
| `023254cb`, `38579b31`, `857f9d82` | C4 정정 TYPE_INVALID·FK 23503(`probe-c4-7`~`9`) | 정정은 원 출처 사건의 version 2이고 인도 원본 subject는 출고(DISPATCH)다 |
| `4b63edf1` | probe suite C1 보유0(`probe-suite-1`) | C4가 공유 시계를 09:00:03Z로 옮겨 C1 조회가 비었다. suite 순서를 E1→E2→C1→C4로 바꾼다 |

## acceptance-contract.json 대비 flow 범위

flow는 `fullCaseCoverageClaimed=false`인 bounded HTTP/JDBC 검사다. 아래
"probe에서 관찰"은 probe branch 실행의 값이며 통합 제품 PASS가 아니다.

- E1: 누적 수령 100, W 보유 80, 과거 인도 30, 반품 10, 현재 적격 0,
  송장 원차이 5와 수량차 0, 은행 효과 0(`bankEffect` "0"·payment
  reference 0행), QC·반품(QC/상업/정산 검토)·정산 차이의 별도 인간 의무,
  getObject/getWork의 같은 snapshot·ID 비교를 검사한다. 통합 경로는 예약
  전(476 assertion)에서 멈춘다.
- E2: 실물 60, QC20와 회수 보류의 독립, ADMIN 범위 50, 회수 25와 같은
  25의 폐기를 처리 25로만 집계, 미확인 25로 정상 종료 거부, 옛 승인
  거부, 예외 25 뒤 잔여 책임 유지를 검사한다. 통합 경로는 예약 전(240).
- C4: 인도 100과 반품 20을 별개 사건으로, 실제 98 정정 뒤 과거 판정
  SATISFIED 보존과 의무 2를 검사한다. 계약의
  `resolvedOrWaivedDutyRevival=0`(해소한 의무 재생성0)은 flow에 해소 후
  재정정 단계가 없어 **NOT_RUN**이다. 통합 경로는 예약 전(210).
- C1: 보유 100/판매 근거 40, 철회 뒤 새 예약·출고 0과 기존 배분·QC 책임
  유지를 검사한다. 통합 경로는 예약 전(226).
- `postDispatchRecall`(인도 20·운송 10 보존), V2, V3는 이 native flow에
  없다. inventory 소유 PostgreSQL test의 범위이며 native 인수로는
  **NOT_RUN**이다.

## 미실행과 제한

- 유료 모델·client UAT, BTP 배포, 실제 규제기관 제출, 은행 이체·세금
  신고는 실행하지 않았다(NOT_RUN). fixture의 규제 policy는 가상 값이다.
- S1·S2 actual adapter는 다시 실행하지 않았다. 바뀐 FixtureInstaller·
  ActualAcceptanceDriver는 새 선택 필드가 없을 때 이전과 같고, 같은 두
  class를 쓰는 S3 actual은 `s3-regression-1`(commit `857f9d82`)에서 594
  assertion PASS다. harness 단위 test 434건도 통과했다.
- backend는 이 branch에서 바꾸지 않았다. custody 충돌 해소 전에는 S4
  native 인수가 통과할 수 없다.
- `probe-*-2`는 cherry-pick 명령 실패로 `-1`과 같은 source에서 다시
  실행된 attempt다. 삭제하지 않고 보존한다.

## attempt 목록

`raw/<attempt>/`에 `run-receipt.json`, `actual-s4-native.json.gz`,
`native-stdout.json.gz`(S3 회귀는 `actual-s3-native.json.gz`)를 보존한다.
gzip은 mtime 0으로 만들어 내용이 같은 두 파일은 같은 blob이다.
`raw/index.json`은 아래 표의 기계 판독용 원본이다. `baseline`은 coordinator
첫 실행(`7b540e8f`), `native`는 `step3/s4d-native`, `probe`는
`step3/s4d-custody-probe` 실행이다.

| attempt | branch | commit | JAR sha256 | flow | 결과 | assertion | 첫 실패 |
|---|---|---|---|---|---|---|---|
| `e1-1` | baseline | `7b540e8f` | `d6b389beea85` | e1-flow.json | FAIL | 1 | AssertionError: create-work expected business outcome APPLIED observed {"outcome":"HELD","effects":{},"error":… |
| `e1-2` | native | `184d4820` | `a7f64d74c50c` | e1-flow.json | FAIL | 379 | AssertionError: dispatch-s60-QC-match expected "MATCHED" observed "CONFLICT" |
| `e1-3` | native | `75af1587` | `a7f64d74c50c` | e1-flow.json | FAIL | 476 | AssertionError: e1-reserve30 expected business outcome APPLIED observed {"error":{"code":"SCOPE_INELIGIBLE","m… |
| `e1-final` | native | `857f9d82` | `e2307d3f4a4f` | e1-flow.json | FAIL | 476 | AssertionError: e1-reserve30 expected business outcome APPLIED observed {"outcome":"REJECTED","effects":{},"er… |
| `e2-1` | baseline | `7b540e8f` | `d6b389beea85` | e2-flow.json | NOT_RUN | 0 | IllegalStateException: Actual fixture transaction failed: PSQLException SQLSTATE=23505 table=mulino_identity_o… |
| `e2-2` | native | `75af1587` | `a7f64d74c50c` | e2-flow.json | FAIL | 99 | AssertionError: e2-sell-reg-submitted-match expected "MATCHED" observed "UNVERIFIED" |
| `e2-3` | native | `17001330` | `a7f64d74c50c` | e2-flow.json | FAIL | 143 | AssertionError: e2-dispatch-QC-match expected "MATCHED" observed "CONFLICT" |
| `e2-4` | native | `873df311` | `a7f64d74c50c` | e2-flow.json | FAIL | 240 | AssertionError: e2-reserve30 expected business outcome APPLIED observed {"effects":{},"outcome":"REJECTED","er… |
| `e2-final` | native | `857f9d82` | `e2307d3f4a4f` | e2-flow.json | FAIL | 240 | AssertionError: e2-reserve30 expected business outcome APPLIED observed {"effects":{},"error":{"message":"Conf… |
| `c4-1` | baseline | `7b540e8f` | `d6b389beea85` | c4-flow.json | NOT_RUN | 0 | IllegalStateException: Actual fixture transaction failed: PSQLException SQLSTATE=23505 table=mulino_identity_o… |
| `c4-2` | native | `75af1587` | `a7f64d74c50c` | c4-flow.json | FAIL | 84 | AssertionError: c4-reg-submitted-link expected business outcome APPLIED observed {"error":{"code":"EVIDENCE_UN… |
| `c4-3` | native | `17001330` | `a7f64d74c50c` | c4-flow.json | FAIL | 210 | AssertionError: c4-reserve30 expected business outcome APPLIED observed {"error":{"message":"Confirmed warehou… |
| `c4-final` | native | `857f9d82` | `e2307d3f4a4f` | c4-flow.json | FAIL | 210 | AssertionError: c4-reserve30 expected business outcome APPLIED observed {"outcome":"REJECTED","error":{"code":… |
| `c1-1` | baseline | `7b540e8f` | `d6b389beea85` | c1-flow.json | NOT_RUN | 0 | IllegalStateException: Actual fixture transaction failed: PSQLException SQLSTATE=23505 table=mulino_identity_o… |
| `c1-2` | native | `75af1587` | `a7f64d74c50c` | c1-flow.json | FAIL | 84 | AssertionError: c1-reg-submitted-link expected business outcome APPLIED observed {"effects":{},"outcome":"HELD… |
| `c1-3` | native | `17001330` | `a7f64d74c50c` | c1-flow.json | FAIL | 226 | AssertionError: c1-reserve20 expected business outcome APPLIED observed {"effects":{},"error":{"message":"Conf… |
| `c1-final` | native | `857f9d82` | `e2307d3f4a4f` | c1-flow.json | FAIL | 226 | AssertionError: c1-reserve20 expected business outcome APPLIED observed {"error":{"code":"SCOPE_INELIGIBLE","m… |
| `suite-1` | native | `873df311` | `a7f64d74c50c` | flow.json | FAIL | 476 | AssertionError: e1-reserve30 expected business outcome APPLIED observed {"outcome":"REJECTED","error":{"code":… |
| `suite-2` | native | `857f9d82` | `e2307d3f4a4f` | flow.json | FAIL | 476 | AssertionError: e1-reserve30 expected business outcome APPLIED observed {"outcome":"REJECTED","error":{"code":… |
| `suite-3` | native | `4b63edf1` | `c98fb7823bc2` | flow.json | FAIL | 476 | AssertionError: e1-reserve30 expected business outcome APPLIED observed {"error":{"code":"SCOPE_INELIGIBLE","m… |
| `s3-regression-1` | native | `857f9d82` | `e2307d3f4a4f` | flow.json | PASS | 594 | - |
| `probe-e1-1` | probe | `ec7baabd` | `ac8326e1c832` | e1-flow.json | FAIL | 480 | AssertionError: e1-pick30 expected business outcome APPLIED observed {"effects":{},"error":{"message":"Declare… |
| `probe-e1-2` | probe | `ec7baabd` | `ac8326e1c832` | e1-flow.json | FAIL | 480 | AssertionError: e1-pick30 expected business outcome APPLIED observed {"effects":{},"outcome":"REJECTED","error… |
| `probe-e1-3` | probe | `9effdf45` | `ac8326e1c832` | e1-flow.json | FAIL | 506 | AssertionError: e1-return-intake10 expected business outcome APPLIED observed {"error":{"message":"Declared su… |
| `probe-e1-4` | probe | `790f9e31` | `ac8326e1c832` | e1-flow.json | FAIL | 513 | AssertionError: e1-return-original-link expected business outcome APPLIED observed {"error":{"code":"EVIDENCE_… |
| `probe-e1-5` | probe | `70461727` | `ac8326e1c832` | e1-flow.json | FAIL | 533 | AssertionError: e1-match-invoice-gap5 expected "0" observed "0.000000000000" |
| `probe-e1-6` | probe | `069a36bb` | `ac8326e1c832` | e1-flow.json | PASS | 612 | - |
| `probe-e1-9` | probe | `0c8254b0` | `ac8326e1c832` | e1-flow.json | NOT_RUN | 488 | IllegalStateException: Actual fixture transaction failed: PSQLException SQLSTATE=23503 table=UNKNOWN constrain… |
| `probe-e1-10` | probe | `78b2b5d0` | `ac8326e1c832` | e1-flow.json | PASS | 612 | - |
| `probe-e1-final` | probe | `78b2b5d0` | `1ab4439bdf9c` | e1-flow.json | PASS | 612 | - |
| `probe-e2-1` | probe | `ec7baabd` | `ac8326e1c832` | e2-flow.json | FAIL | 244 | AssertionError: e2-pick30 expected business outcome APPLIED observed {"effects":{},"error":{"message":"Declare… |
| `probe-e2-2` | probe | `ec7baabd` | `ac8326e1c832` | e2-flow.json | FAIL | 244 | AssertionError: e2-pick30 expected business outcome APPLIED observed {"outcome":"REJECTED","error":{"code":"TY… |
| `probe-e2-3` | probe | `9effdf45` | `ac8326e1c832` | e2-flow.json | FAIL | 248 | AssertionError: e2-qc20-match expected business outcome APPLIED observed {"error":{"message":"Declared subject… |
| `probe-e2-4` | probe | `790f9e31` | `ac8326e1c832` | e2-flow.json | FAIL | 282 | AssertionError: e2-dispatch-denied-after-qc-release expected business outcome HELD observed {"effects":{},"out… |
| `probe-e2-5` | probe | `70461727` | `ac8326e1c832` | e2-flow.json | FAIL | 365 | AssertionError: e2-stale-approval-exception-denied expected business outcome HELD observed {"effects":{"partit… |
| `probe-e2-6` | probe | `069a36bb` | `ac8326e1c832` | e2-flow.json | PASS | 410 | - |
| `probe-e2-final` | probe | `78b2b5d0` | `1ab4439bdf9c` | e2-flow.json | PASS | 410 | - |
| `probe-c4-1` | probe | `ec7baabd` | `ac8326e1c832` | c4-flow.json | FAIL | 214 | AssertionError: c4-pick30 expected business outcome APPLIED observed {"error":{"message":"Declared subject dif… |
| `probe-c4-2` | probe | `ec7baabd` | `ac8326e1c832` | c4-flow.json | FAIL | 214 | AssertionError: c4-pick30 expected business outcome APPLIED observed {"outcome":"REJECTED","error":{"message":… |
| `probe-c4-3` | probe | `9effdf45` | `ac8326e1c832` | c4-flow.json | FAIL | 261 | AssertionError: c4-return-original-match expected business outcome APPLIED observed {"outcome":"REJECTED","err… |
| `probe-c4-4` | probe | `790f9e31` | `ac8326e1c832` | c4-flow.json | FAIL | 264 | AssertionError: c4-return-original-link expected business outcome APPLIED observed {"effects":{},"error":{"mes… |
| `probe-c4-5` | probe | `70461727` | `ac8326e1c832` | c4-flow.json | FAIL | 282 | AssertionError: c4-correction98-event expected business outcome APPLIED observed {"outcome":"REJECTED","error"… |
| `probe-c4-6` | probe | `069a36bb` | `ac8326e1c832` | c4-flow.json | FAIL | 282 | AssertionError: c4-correction98-event expected business outcome APPLIED observed {"effects":{},"error":{"messa… |
| `probe-c4-7` | probe | `e6d3b9bf` | `ac8326e1c832` | c4-flow.json | FAIL | 286 | AssertionError: c4-correction98-match expected business outcome APPLIED observed {"effects":{},"outcome":"REJE… |
| `probe-c4-8` | probe | `2db7235e` | `ac8326e1c832` | c4-flow.json | FAIL | 286 | AssertionError: c4-correction98-match expected business outcome APPLIED observed {"error":{"message":"Delivery… |
| `probe-c4-9` | probe | `0c8254b0` | `ac8326e1c832` | c4-flow.json | NOT_RUN | 224 | IllegalStateException: Actual fixture transaction failed: PSQLException SQLSTATE=23503 table=UNKNOWN constrain… |
| `probe-c4-10` | probe | `78b2b5d0` | `ac8326e1c832` | c4-flow.json | PASS | 321 | - |
| `probe-c4-final` | probe | `78b2b5d0` | `1ab4439bdf9c` | c4-flow.json | PASS | 321 | - |
| `probe-c1-1` | probe | `ec7baabd` | `ac8326e1c832` | c1-flow.json | PASS | 304 | - |
| `probe-c1-final` | probe | `78b2b5d0` | `1ab4439bdf9c` | c1-flow.json | PASS | 304 | - |
| `probe-suite-1` | probe | `78b2b5d0` | `1ab4439bdf9c` | flow.json | FAIL | 1544 | AssertionError: c1-custody100-sale40 expected "100" observed "0" |
| `probe-suite-2` | probe | `7adb9b9f` | `1ab4439bdf9c` | flow.json | PASS | 1647 | - |
