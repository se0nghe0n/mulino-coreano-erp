# S4 창고 보관 주체 개정 실행 기록 (s4e-custody)

## 결론

branch `step3/s4e-custody`(baseline tag `step3-s4e-baseline` =
`a5bd9996`)에서 직접 수령의 보관 주체를 검증된 증거로만 기록하도록
바꿨다. 같은 제품 commit `eed2151f`, 같은 JAR
(`302355ea1bb527ae80cc56f4992f1b9d4bd6608b9abd62cfefe5d0f8b63aa8e4`)에서
native S4 E1·E2·C4·C1 flow와 기본 suite가 모두 **PASS**이고, S3 actual
회귀도 **PASS**다. 남은 C4 계약 항목 `resolvedOrWaivedDutyRevival=0`은
제품에 권한 있는 해소 경로가 없어 **NOT_RUN**이다(아래 "C4 해소 단계").

## 결정과 근거

s4d-native에서 모든 native flow가 첫 `reserveQuantity`에서
`REJECTED SCOPE_INELIGIBLE "Confirmed warehouse custody required for
fulfillment"`로 멈췄다. S3 계약은 직접 수령의 소유·보관 주체를 미확인으로
두고, S4는 내부 보관 주체가 확인된 창고 segment만 예약하게 한다. 진단용
probe(`ec7baabd`)는 확인 actor를 보관 주체로 두어 통과했지만 보관을
추론한 것이라 채택하지 않았다.

coordinator 결정에 따라 보관 주체는 검증된 증거로만 알게 했다. 계획
§4.1(QuantitySegment의 위치·보관자·owner 분리)과 §4.2 126행("위치·
보관자·owner·위험 부담은 별도 관계이며 인계가 소유권 이전을 자동
발생시키지 않는다")이 근거다. 세부 규칙은 S3 계약의 개정 절
(`docs/execution/s3-receipt/contracts.md`)에 있다. 요약하면 다음과 같다.

- `confirmReceipt`의 선택 slot `receivingCustodianId`는 직접 수령에만
  허용한다. 대상 actor는 같은 조직의 HUMAN/AGENT이고 같은 범위에서 현재
  confirmReceipt 권한을 가져야 한다.
- 그 canonical의 현재 검증 chain의 원본 blob과 event payload가 같은
  보관 주체를 지명해야 한다. 불일치·미지명은 HELD이고 효과는 0이다.
- 소유는 수령에서 언제나 미확인이다. transit 수령은 transit leaf의 보관
  주체·소유를 그대로 보존하고 slot을 거부한다.
- 새 column이 필요 없어 migration이나 schema parity 재생성은 없다.

native 입력은 receipt60·receipt40 원본이 `receivingCustodianId=$supervisor`를
지명하고 해당 확인 명령이 같은 slot을 싣는다. 확인 actor는 reader이므로
supervisor 지명은 실행 actor에서 보관 주체를 추론하지 않음을 드러낸다.
E1 실행의 원 행에서 W segment의 `custodianId`는 supervisor, `ownerId`는
모두 null이다(`raw/e1-1/actual-s4-native.json.gz`).

## commit

| commit | 내용 |
|---|---|
| `3b2a5d0a` | `ReceiptCommands`·`ReceiptStockPrimitives`·`TradeEvidence` 변경과 PostgreSQL test |
| `eed2151f` | `author.py` 보관 주체 지명 단계, e1/e2/c1/c4 입력 재생성 |
| 이 기록 commit | S3 계약 개정, S4 notes, 실행 증거 |

진단 branch `step3/s4e-c4-resolution-probe`(`fa200d49`, `2a3f8737`)는
C4 해소 경로 관찰용이며 통합 대상이 아니다. push하지 않았다.

## 실행 명령과 결과

모든 Maven·verify는 `. /Volumes/VideoStore/Developer/.mulino-tools/env.sh`
뒤 `$MULINO_SLOT`로 한 번에 하나씩 실행했다. Java 21.0.5, PostgreSQL 18
container(`postgres@sha256:4ef4dbc9…`).

### backend 집중 test (commit 전 작업 트리, 이후 `3b2a5d0a`와 같은 source)

```bash
$MULINO_SLOT ./mvnw -B -ntp -q -f backend/pom.xml test \
  -Dtest='ReceiptGatewayPostgresTest'
$MULINO_SLOT ./mvnw -B -ntp -q -f backend/pom.xml test \
  -Dtest='FulfillmentPostgresTest,StockCommandPostgresTest,S3CanonicalGatePostgresTest,ReceiptResidualGatewayPostgresTest,DeliveryCorrectionTest,TradeResidualRemedyTest'
```

| class | tests | errors | failures | skipped |
|---|---|---|---|---|
| ReceiptGatewayPostgresTest | 14 | 0 | 0 | 0 |
| FulfillmentPostgresTest | 17 | 0 | 0 | 0 |
| StockCommandPostgresTest | 14 | 0 | 0 | 0 |
| S3CanonicalGatePostgresTest | 17 | 0 | 0 | 0 |
| ReceiptResidualGatewayPostgresTest | 17 | 0 | 0 | 0 |
| TradeResidualRemedyTest | 5 | 0 | 0 | 0 |
| DeliveryCorrectionTest | 2 | 0 | 0 | 0 |

`ReceiptGatewayPostgresTest`의 새 test는 다음을 실제 PostgreSQL로 검사한다.
기존 `actual60plus40CrossSourceDuplicateAndReplay…`의 null 보관 주체
assertion은 바꾸지 않았다.

- `directReceiptRecordsOnlyEvidencedInternalCustodianAndNeverOwner`: 증거와
  slot이 일치하면 custodian 기록·owner null, slot 없는 수령은 증거가 있어도
  미확인.
- `receivingCustodianSlotThatDiffersFromVerifiedEvidenceHasNoEffect`: slot과
  증거 불일치·증거 미지명은 HELD EVIDENCE_UNVERIFIED, 원본과 payload 불일치는
  HELD EVIDENCE_CONFLICT. 세 경우 모두 segment·Receipt·movement가 0이고,
  같은 관측을 올바른 slot으로 다시 확정하면 적용된다.
- `nonInternalUnauthorizedOrUnknownReceivingCustodianIsRejected`: EXTERNAL
  actor, 권한 없는 내부 actor, 철회된 권한은 REJECTED SCOPE_INELIGIBLE,
  모르는 actor는 REJECTED FORBIDDEN이며 효과는 0이다.
- `transitReceiptKeepsTransitCustodyAndRejectsReceivingCustodianSlot`.
- `FulfillmentPostgresTest.reserveRejectsUnknownCustodianReceiptSegment…`:
  보관 주체 미확인 segment 예약은 SCOPE_INELIGIBLE·효과 0, 내부 보관 주체
  segment 예약은 적용.

### harness test (`eed2151f`)

```bash
$MULINO_SLOT ./mvnw -B -ntp -q -f verification/harness/pom.xml test
```

41개 report, 434 tests / 0 errors / 0 skipped / 0 failures.

### native flow (`eed2151f`)

```bash
$MULINO_SLOT python3 verification/actual/s4/build.py
ACTUAL_FLOW_REF=verification/actual/s4/<x>-flow.json \
  $MULINO_SLOT ./verify actual-s4 /tmp/mulino-s4e-<x>-1     # e1 e2 c4 c1
$MULINO_SLOT ./verify actual-s4 /tmp/mulino-s4e-suite-1
$MULINO_SLOT python3 verification/actual/s3/build.py
$MULINO_SLOT ./verify actual-s3 /tmp/mulino-s4e-s3-1
```

| attempt | commit | JAR sha256 | flow | 결과 | assertion | 첫 실패 |
|---|---|---|---|---|---|---|
| `e1-1` | `eed2151f` | `302355ea1bb5` | e1-flow.json | PASS | 612 | - |
| `e2-1` | `eed2151f` | `302355ea1bb5` | e2-flow.json | PASS | 410 | - |
| `c4-1` | `eed2151f` | `302355ea1bb5` | c4-flow.json | PASS | 321 | - |
| `c1-1` | `eed2151f` | `302355ea1bb5` | c1-flow.json | PASS | 304 | - |
| `suite-1` | `eed2151f` | `302355ea1bb5` | flow.json | PASS | 1647 | - |
| `s3-regression-1` | `eed2151f` | `302355ea1bb5` | s3 flow.json | PASS | 594 | - |
| `probe-c4-resolve-1` | `fa200d49` (probe) | `302355ea1bb5` | c4-resolution-probe-flow.json | FAIL | 330 | c4-resolve-deficit2 기대 APPLIED, 관찰 HELD VERSION_UNSUPPORTED |
| `probe-c4-resolve-2` | `2a3f8737` (probe) | `302355ea1bb5` | c4-resolution-probe-flow.json | FAIL | 330 | 같은 지점, 같은 관찰 |

assertion 수는 s4d probe branch의 값(E1 612, E2 410, C4 321, C1 304,
suite 1647)과 같다. 기대값을 바꾸지 않았고 결과를 seed하지 않았다.
`raw/<attempt>/`에 `run-receipt.json`, `actual-s4-native.json.gz`(S3는
`actual-s3-native.json.gz`), `native-stdout.json.gz`를 보존했다. gzip
mtime은 0이다. `raw/index.json`이 위 표의 기계 판독 원본이다.

## C4 해소 단계 (`resolvedOrWaivedDutyRevival=0`)

계획 §13 C4와 `acceptance-contract.json`은 정정으로 생긴 부족2를 권한
있게 해소(RESOLVED 또는 WAIVED)한 뒤 다시 정정해도 그 의무가 부활하지
않을 것을 요구한다. 이 단계를 native C4 flow에 넣으려 했지만 현재 제품에
도달 가능한 해소 경로가 없다.

- `resolveObligation`: `ResponsibilityEvidenceGate.requireResolution`은
  FOLLOWUP_REVIEW 외 kind에 kind별 resolver를 요구한다. resolver는
  QUALITY_REVIEW, REGULATORY_REVIEW, EXTERNAL_RECONCILIATION뿐이고
  DELIVERY_CORRECTED_DEFICIT용은 없다.
- `waiveObligation`: `PolicyCommandGuard`가 `mulino.commands.Approvals`의
  승인 행을 요구하지만 이를 만드는 공개 명령이 없다
  (`ApprovalRepository.create`의 호출자가 없다).

probe branch에서 C4 flow 뒤에 열린 부족2 assignment를 원 행에서 찾아
supervisor가 `resolveObligation`(근거: 98 정정 canonical)을 실행하게 했다.
`probe-c4-resolve-1`은 fixture 정의에 resolveObligation verb·권한이 없어
원인을 가를 수 없었다. `probe-c4-resolve-2`는 verb·capability·grant·
policy rule(`RESPONSIBILITY`)을 넣은 별도 fixture로 실행했으나 같은
`HELD VERSION_UNSUPPORTED`를 관찰했다. 남은 원인은 위의 kind resolver
부재로 판단한다. 다만 같은 오류 문구를 쓰는 다른 경로를 log로 배제하지는
않았다. 어떤 증거가 "부족2 해소"를 증명하는지(고객의 최종 범위 수락,
재인도, 대금 조정 등)는 계획이 정하지 않는다. 이 worker 범위에서 해소
의미를 새로 만들지 않았다. 결정이 필요하다.

## 관찰한 관련 문제 (미변경)

- `ReturnStockPrimitives`는 반품 물량의 보관 주체를 반품 확인 actor
  (`c.actorId()`)로 둔다. E1 원 행에서 반품10 segment의 custodian이
  reader다. 수령 개정과 같은 근거 원칙을 적용할지 결정이 필요하다.
- 인도 이동(`transferRange` 보관 주체 인자 없음)은 고객 장소의 인도
  segment에도 창고 보관 주체를 그대로 둔다. 계획상 고객 인도 뒤 보관
  관계를 어떻게 표현할지는 이 작업 범위가 아니다.

## NOT_RUN과 제한

- 전체 backend suite와 schema parity는 실행하지 않았다(coordinator 담당).
  schema는 바꾸지 않았다.
- C4 `resolvedOrWaivedDutyRevival=0`: NOT_RUN(위 절).
- `postDispatchRecall`, V2, V3: native flow에 없고 inventory 소유
  PostgreSQL test 범위다. native 인수는 NOT_RUN이다.
- S1·S2 actual adapter는 다시 실행하지 않았다. backend 변경은 수령
  확인 slot과 수령 원장에 국한되고, 같은 수령 경로를 쓰는 S3 actual이
  PASS다.
- 유료 모델·client UAT, BTP 배포, 실제 규제기관 제출, 은행 이체·세금
  신고는 실행하지 않았다. fixture의 보관 주체·규제 정책은 가상 값이다.
