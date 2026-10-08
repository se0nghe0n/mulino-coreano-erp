# 겹친 제한과 중복 없는 회수 처분·예외 책임의 인수 계약

B2 `feaca0af9673620eff9a5ac0f08a657ce14e9ccd`에서 작성한
Step 2 테스트 계약이다. 제품 API/DB/host 실행은 NOT_RUN이다.
fixture 설치는 입력이며 업무 효과나 승인 성공을 미리 저장하지 않는다.
모든 action·assertion은 같은 한국어 scenario에 대응한다.

| subcase | 손계산과 책임 |
|---|---|
| overlapping-holds | 동일 LOT60의 QC20만 해제해도 회수 조사60 제한과 조사 책임은 남는다 |
| qc-release-only-control | 회수 보류가 없으면 같은 QC20 해제 뒤 같은 형식의 출고40이 적용된다(대조군) |
| same-25-not-50 | 승인50 중 회수25와 동일 실물 폐기25는 최종25이며 미확인25의 종료를 막는다 |
| exception-responsibility | 일반 역할의 예외 종료0 뒤 ADMIN 근거 있는 미확인25 예외 종료에도 잔여 책임은 보인다 |

수량은 BOX이며 송장 차이는 EUR다. API와 독립 DB의 동일 snapshot을
대조한다. 원 행은 각 requested source의 완전한 query·parameters·mapping·
snapshot artifact와 함께 반환해야 한다. 미확인/상충은 0으로 바꾸지 않는다.
거부 감사와 접수 책임은 허용하되 금지된 원장·배분·정상 이행·외부
효과는 명시한 전후 원 행으로 비교한다. 과거 인도와 현재 반품을 빼서
한 값으로 만들지 않는다. 회수25를 다시 폐기해도 distinct 실물은25다.

회수 대상 serial1–50 중1–25를 회수하고 같은1–25를 폐기한다.
회수25와 폐기25의 union은25이며 최종 처리25+미확인25=승인 범위50이다.
serial26–50은 미확인 상태이므로 처리량에 넣지 않는다. 폐기된1–25의
lastConfirmedPlaceId가 W여도 현재 물리 재고가 W에25 있다는 뜻은 아니다.
currentControlStatus와 finalDisposition을 독립 축으로 대조한다.
ADMIN의 근거 있는 예외 종료에도 미확인25의 owner·다음 행동·기한과
원래 scope50의 membership은 남아야 한다.

| oracle | named observation과 assertion |
|---|---|
| E2.overlapping-holds | physical-held → overlapping-holds:physical-held-1, overlapping-holds:physical-held-2, overlapping-holds:after-dispatch-mcp-same-snapshot, overlapping-holds:physical-held-1-mcp |
| E2.overlapping-holds | dispatched-after-QC-only-release → overlapping-holds:dispatched-after-QC-only-release-3, overlapping-holds:dispatched-after-QC-only-release-4, overlapping-holds:dispatched-after-QC-only-release-5, overlapping-holds:dispatched-after-QC-only-release-6, overlapping-holds:dispatched-after-QC-only-release-7, overlapping-holds:qc-release-applied, overlapping-holds:qc-release-same-restriction, overlapping-holds:qc20-released-before-dispatch, overlapping-holds:qc20-remains-released-after-dispatch, overlapping-holds:recall60-scope-quantity-remains-active, overlapping-holds:qc20-active-before-release, overlapping-holds:dispatch20-rejected, overlapping-holds:dispatch20-reason, overlapping-holds:dispatch20-denial-audit, overlapping-holds:dispatch40-rejected, overlapping-holds:dispatch40-reason, overlapping-holds:dispatch40-denial-audit, overlapping-holds:recall-suspends-untouched40-before, overlapping-holds:recall-suspends-untouched40-after, overlapping-holds:dispatched-after-QC-only-release-3-mcp, overlapping-holds:dispatched-after-QC-only-release-6-mcp, overlapping-holds:dispatched-after-QC-only-release-7-mcp, qc-release-only-control:control-dispatch40-applied, qc-release-only-control:control-warehouse-dispatch40, qc-release-only-control:control-dispatch40-movement, qc-release-only-control:control-allocation40-consumed, qc-release-only-control:control-qc-release-applied, qc-release-only-control:after-dispatch-mcp-same-snapshot, qc-release-only-control:control-warehouse-dispatch40-mcp |
| E2.overlapping-holds | recall-hold → overlapping-holds:recall-hold-8, overlapping-holds:recall-hold-9, overlapping-holds:recall-hold-8-mcp |
| E2.overlapping-holds | candidate-as-confirmed-contamination → overlapping-holds:candidate-as-confirmed-contamination-10, overlapping-holds:candidate-as-confirmed-contamination-11, overlapping-holds:candidate-as-confirmed-contamination-12, overlapping-holds:candidate-as-confirmed-contamination-12-mcp |
| E2.overlapping-holds | investigation-duty → overlapping-holds:investigation-duty-13, overlapping-holds:investigation-duty-14, overlapping-holds:investigation-duty-15, overlapping-holds:investigation-duty-16, overlapping-holds:investigation-duty-17, overlapping-holds:investigation-duty-18, overlapping-holds:investigation-duty-19, overlapping-holds:investigation-duty-19-mcp |
| E2.same-25-not-50 | unique-recovered → same-25-not-50:unique-recovered-1, same-25-not-50:unique-recovered-2, same-25-not-50:after-close-mcp-same-snapshot, same-25-not-50:unique-recovered-1-mcp |
| E2.same-25-not-50 | unique-finally-processed → same-25-not-50:unique-finally-processed-3, same-25-not-50:unique-finally-processed-4, same-25-not-50:unique-finally-processed-3-mcp |
| E2.same-25-not-50 | unknown → same-25-not-50:unknown-5, same-25-not-50:unknown-6, same-25-not-50:unknown-5-mcp |
| E2.same-25-not-50 | 25-plus-25-equals50 → same-25-not-50:25-plus-25-equals50-7, same-25-not-50:25-plus-25-equals50-8, same-25-not-50:25-plus-25-equals50-8-mcp |
| E2.same-25-not-50 | unapproved-unknown-close → same-25-not-50:unapproved-unknown-close-9, same-25-not-50:unapproved-unknown-close-10, same-25-not-50:unapproved-unknown-close-11, same-25-not-50:unapproved-unknown-close-12, same-25-not-50:unapproved-unknown-close-13, same-25-not-50:unapproved-unknown-close-reason, same-25-not-50:unapproved-unknown-close-audit, same-25-not-50:unapproved-unknown-close-11-mcp, same-25-not-50:unapproved-unknown-close-12-mcp |
| E2.same-25-not-50 | status-axes → same-25-not-50:status-axes-14, same-25-not-50:status-axes-15, same-25-not-50:status-axes-16, same-25-not-50:status-axes-17, same-25-not-50:status-axes-18, same-25-not-50:status-axes-19, same-25-not-50:status-axes-20, same-25-not-50:status-axes-21, same-25-not-50:status-axes-22 |
| E2.exception-responsibility | unauthorized-exception-close → exception-responsibility:unauthorized-exception-close-1, exception-responsibility:unauthorized-exception-close-2, exception-responsibility:unauthorized-exception-close-3, exception-responsibility:unauthorized-exception-close-4, exception-responsibility:unauthorized-exception-close-5, exception-responsibility:unauthorized-exception-close-6, exception-responsibility:unauthorized-exception-close-7 |
| E2.exception-responsibility | exception-unresolved-scope → exception-responsibility:exception-unresolved-scope-8, exception-responsibility:exception-unresolved-scope-9, exception-responsibility:closed-mcp-same-snapshot, exception-responsibility:exception-unresolved-scope-8-mcp |
| E2.exception-responsibility | exception-residual-duty → exception-responsibility:exception-residual-duty-10, exception-responsibility:exception-residual-duty-11, exception-responsibility:exception-residual-duty-12, exception-responsibility:exception-residual-duty-13, exception-responsibility:exception-residual-duty-14, exception-responsibility:exception-residual-duty-15, exception-responsibility:exception-residual-duty-16, exception-responsibility:exception-residual-duty-16-mcp |
| E2.exception-responsibility | exception-evidence → exception-responsibility:exception-evidence-17, exception-responsibility:exception-evidence-18, exception-responsibility:exception-evidence-19, exception-responsibility:exception-evidence-20, exception-responsibility:exception-evidence-21, exception-responsibility:exception-evidence-22, exception-responsibility:exception-evidence-23, exception-responsibility:exception-evidence-24, exception-responsibility:exception-evidence-25, exception-responsibility:exception-evidence-26, exception-responsibility:exception-evidence-27, exception-responsibility:exception-evidence-28, exception-responsibility:exception-evidence-29, exception-responsibility:exception-evidence-21-mcp, exception-responsibility:exception-evidence-22-mcp |

검증 증거는 evidence/에 보존했다. 최종 전체 harness143개
(판매 표본·mutant13개 포함)는 PASS이며 JSON schema는 유효하다. 이 case의 실제 Gherkin
selector는 3개를 발견·시작했고 모두 NOT_IMPLEMENTED assertion으로
RED(exit1), scenario skip0이다. 실제 제품 profile은 NOT_RUN(exit2)이다.
전체 명령·version·집계는 E1/evidence/sales-preparation-summary.json에 있다.

## 2026-10-08 재검토 수정

- overlapping-holds의 출고 probe는 피킹을 마친 단일 allocationId·
  cargoPlaceId TRANSIT·직전 getObject 현재 revision의 dispatch20·
  dispatch40이다. 둘 다 REJECTED·INSUFFICIENT_ELIGIBLE_QUANTITY·거부
  감사1이며, QC가 닿지 않은 자식40의 배분이 회수 보류만으로 출고 전후
  SUSPENDED임을 확인해 원인을 회수 보류로 묶는다. qc-release20의
  expectedRevision은 qc-hold20의 것이다.
- qc-release-only-control은 조사·회수 보류만 뺀 대조군이다. 같은 형식의
  출고40이 APPLIED, 창고 출고40, 배분 CONSUMED가 된다.
- same-25-not-50의 false-close는 admin-exception과 같은 scopeVersion·
  scopeHash·근거를 보내고 RECALL_RESIDUAL_UNKNOWN과 거부 감사1을 고정한다.
- 모든 oracle이 MCP layer를 요구하므로 `mcp` profile을 선언하고 최종
  조회를 같은 snapshot의 MCP route로 다시 읽는다.
- 오류 코드는 `/response/error/code`로만 읽는다.
