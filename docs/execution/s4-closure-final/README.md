# S4 closure 종료 판정 및 S5 진입 backlog

작성 기준일: 2026-10-08. 이 문서는 Step 3(S4) closure의 전 과정,
최종 판정 근거, 미해결 항목 목록을 한 곳에 기록한다.

## 1. closure 개요

S4 closure는 리뷰 5라운드로 종결됐다. 각 라운드의 지적은 Opus
xhigh와 GPT-6-Astra low(Fable low 대체) 두 리뷰어가 냈고, 리뷰어별
skeptic이 재현성과 심각도를 조정한 뒤 구현 worker에게 전달됐다.
리뷰 원본 JSON과 verdicts JSON은 저장소 밖 도구 폴더에 라운드별로
보관한다.

| 라운드 | 수정 commit | backend | native |
|---|---|---|---|
| 1 (s4i) | `7fa78db3` | focused 15 class 165/0 | 통합 뒤 S1·S3 회귀 → `a3a9bc2d`·`1401bc45`로 수정 |
| 2 (s4j) | `e350b814` | 509/0 | S4·S3·S2·S1 PASS |
| 3 (s4k) | `3addaefe` | 513/0 | S4·S3·S2·S1 PASS |
| 4 (s4l) | `8d201c77` | 518/0 | S4·S3·S2·S1 PASS |
| 5 | 수정 없음 (head `104b8c3b`) | 518/0, harness 507/0 | S4·S3·S2·S1 PASS |

라운드 1–4 기록: `docs/execution/s4i-closure/` – `s4l-closure/`.

## 2. 최종 판정

head `104b8c3b`에서 Opus xhigh, Astra low 모두 PASS를 냈다. P0–P2
지적이 없다. coordinator가 같은 head에서 확보한 증거는 다음과 같다.

- backend 전체: 518 run, 0 fail, 0 error.
- `./verify harness`: 507/0.
- `./verify actual-s4`, `actual-s3`, `actual-s2`, `actual-s1`: 모두
  PASS.

이로써 S4(판매·인도·반품·회수·정산)는 종료 상태다.

## 3. 라운드 5 잔여 P3 — S5 진입 backlog

두 리뷰어가 PASS를 냈으나 아래 세 항목은 P3 fail-closed로 남았다.

| # | 항목 | 위치 | S5 처리 방향 |
|---|---|---|---|
| R5-1 | waiver coverage가 실행 시점 CURRENT일 때만 기록된다. UNVERIFIED/CHANGED 상태에서 실행된 면제는 이후 동일 잔여 복원 시 새 의무를 연다. | `SettlementResponsibilities.java:71` | match 자체 root는 상태와 무관하게 remaining을 기록하고, system read context로 계산한다. |
| R5-2 | `[SETTLEMENT_COVERED …]` suffix가 `varchar(500)` basis를 넘기면 이미 승인된 면제 실행이 거부된다. | `ResponsibilityService.java:46` | 결정 시점에 suffix 최대 길이를 예약해 검증하거나 별도 열로 분리한다. |
| R5-3 | goal 없이 CLOSED이거나 S1 상태(FULFILLED 등)인 IMPORTED Work를 대상으로 정정·충돌 intake를 하면 `NEEDS_INPUT/IMPORTED_WORK`로 영구 rollback된다. | `AssessmentCorrectionImpact.java:26` | 증거 source profile 기준 COMMAND follow-up을 열거나 감사 marker와 함께 건너뛴다. |

## 4. 전 라운드 DEFERRED 통합 backlog

라운드 2(s4j), 3(s4k), 4(s4l)에서 이월된 항목이다. owner는 논리
역할이며 모두 S5 진입 backlog다.

| 항목 | 이유 | owner |
|---|---|---|
| recall returnId·고객 leaf `recordRecovery` test | coverage 공백. 반품 receipt 회수 경로 fixture가 크다. | trade/recall test |
| 첫 확정 뒤 연결된 상충 custody chain | `requireWarehouse`는 저장 custodian만 본다. 새 의무 kind·어휘·closure 계약이 필요하다. | inventory/receipt custody |
| slot 없이 확정한 segment의 custody 부착 경로 | 예약·출고 거부로 fail-closed. 감사 가능한 정정 명령이 필요하다. | inventory/receipt custody |
| DELIVERY_RESTRICTION_RESPONSE 재-scope | 의무는 있고 수량만 오래됐다. scope revision 공개 명령이 필요하다. | responsibility |
| traceRecall leg Work 인가 | 같은 조직 안 read-scope 누출. MCP 조회 노출 전 필수. | trade/recall query |
| waiveObligation 결정 누락·승인 재사용 오류 code 분리 | 공유 guard 경로라 어휘 contract 변경과 병행해야 한다. | governance/policy guard |
| RECALL_INVESTIGATION ADMIN 면제만 허용 | 조사 완료 증거 형식을 계획이 정하지 않았다. | trade/recall |
| RETURN_RECONCILIATION 수량 불일치 임시 반품 종결 경로 | 반품 계약 변경이 필요하다. fail-closed. | trade/returns |
| 순서가 뒤바뀐 부분 인도의 원장 날짜 | valid-time 분할 primitive 계약 변경이 필요하다. | inventory ledger |
| 조직 전체 경계 500개 상한 | segment 기반 filter와 경합 test가 필요하다. fail-closed. | inventory fulfillment |
| 회수 재-scope 뒤 옛 RECALL_EXCLUDED_SCOPE | 새 scope 포함 범위의 제외 의무를 줄이지 않는다. | trade/recall |
| RECOVERED 전제가 조사 사이에 공유됨 | 조사별 제한 또는 명시 대조 계약이 필요하다. | trade/recall |
| 출고된 적 없는 재고의 CONSUMED_LOST/EXCEPTION | 전체 요청 범위 heldQuantity 검사가 필요하다. | trade/recall |
| 중복 반품 projection | 해소된 RETURN_RECONCILIATION을 따라 투영해야 한다. | trade/returns query |
| ManagementCoverage 단일 차원 통과 | 정책 결정이 선행돼야 한다. | governance/identity |
| relink하지 않은 일반 supersession SALE 인도의 정산 의무 | UNVERIFIED root와 CHANGED root 승계 계약이 필요하다. fail-closed. | settlement |
| invalidatesId ≠ supersedesId 정정의 canonical 재도출 | 두 event RED fixture가 먼저 필요하다. fail-closed. | settlement/evidence |

## 5. Step 2 → Step 3/S5 cross-owner 요청

Step 2 재검토 round 5–7이 Step 3에 요청한 항목이다.

### Step 3 actual (`verification/actual/**`, `FixtureInstaller`)

| 항목 | 근거 |
|---|---|
| `ObserverSnapshot` `RUNTIME_TASK_SNAPSHOT` 구현 | round 5. 현재 NOT_IMPLEMENTED다. |
| passive watcher adapter의 `observeFrom`·`naturalTickSeconds` | round 5–7. 창 `[observeFrom, observeFrom+window]`의 행만 내고, NO_TASK는 최소 한 tick 관찰해야 한다. |
| `verifyCoverage` adapter | round 5. checkout 상태를 harness git과 대조하고 T25 field를 낸다. |
| `enumerateWriteSurface` extractor의 `kind`·`capabilityId` | round 5–6. QUERY 면제가 그 값을 쓴다. |
| `FixtureInstaller` Place kind 기본값 제거, `receivingCustodianAlias` 설치 | round 6–7. kind 누락을 거부하고, 직접 수령 custodian을 원문과 event payload에 넣는다. |
| native fixture Place kind·custodian 보완 | round 6. s2 W에 kind가 없고 s1·s2 A60·B40에 custodian이 없다. |

### Step 3 backend (`OntologyMcp`)

| 항목 | 현재 | 계약 |
|---|---|---|
| `_meta` 누락 | -32020 | -32602 |
| `Mcp-Name` 불일치 | -32602 | -32020 |
| `clientInfo` 형식 오류 | 검사 안 함 | -32602 |
| build-info endpoint | 없음 | 관찰된 build identity 제공 |

## 6. 실행하지 않은 항목

- `./verify scenarios --actual`: Step 2 전체 suite를 product에 대해
  실행하지 않았다.
- V2·V3 경합 profile.
- T19 독립 oracle.

Step 2 closure는 round 7 수정이 통합 중이며 closure review 5차가
남아 있다.
