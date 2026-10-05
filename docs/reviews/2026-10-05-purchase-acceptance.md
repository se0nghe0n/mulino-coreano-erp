# 2026-10-05 실제 구매 인수 기록

MANAGER 승인 이후 로컬 데모 DB에 실제 PO가 생성되고 불변 감사 로그,
후속 WAITING 책임, 열린 Case가 남는 경로는 통과했다. #34의 별도 인간
취소 결정 이력은 현재 API에 없어 전체 완료 조건은 충족하지 못했다.

## 실행 대상

- source: `187e99d91ca3bbbf35197f5bd6a8a7489c797e50`
- branch: `feat/34-purchase-acceptance`
- 기반: 개인 fork의 통합 source다. 원 저장소 PR의 실제 merge를 뜻하지 않는다.
- Java 21.0.12.1, PostgreSQL 18.6, `local` profile, localhost 55463
- DB: `mulino_purchase_acceptance_20261005_v6`
- 실행: 2026-10-05 20:51:34 Asia/Seoul
- JAR SHA-256:
  `a4d095cc978c9b40d0dc1ad731ae17eecb9d9416ed6c8f8e3007e77067b6e51a`
- fixture SHA-256:
  `c16d9d26ac591685924c56aeb0f84dfdd8c12c8c2731f16809eb05d94ae4906d`

`./gradlew bootJar --no-daemon` exit 0으로 만든 JAR를 빈 DB에 실행했다.
Flyway가 적용되고 HTTP `/api-docs`가 200으로 응답했다. 실행 절차와
환경 변수는 [데모 runbook](../15_demo_runbook.md)에 있다.

```bash
python3 mcp-server/scripts/purchase-acceptance.py
```

`python3 -m py_compile`과 `git diff --check`도 통과했다. 잘못된 URL
6개와 DB 이름 1개는 SQL 실행 및 증거 디렉터리 생성 전에 거부됐다.

exit 0이었다. 증거 root는
`/tmp/mulino-repo-fork-20261005/acceptance/`이며 최종 JSON은 `v6/`에 있다.
`build.log`, `backend-v6.log`, `run-v6.log`도 함께 보관했다.

## 업무 결과

| #34 조건 | 결과 | 직접 증거 |
|---|---|---|
| MANAGER 승인 뒤 실제 PO | Pass | orders.json: PO 3, ORDERED, createdBy 3, application 1 |
| 해당 액션 불변 감사 | Pass | business-state.json: action 3의 PURCHASE_APPLIED; audit-mutation-denied.json |
| 후속 WAITING 업무 | Pass | business-state.json: followupStatus WAITING, followups 1 |
| Case 임의 종결 방지 | Pass | Case CASE-2c1c6d9b0fb44b의 status WAITING, 구매 업무 DONE |
| version/hash별 승인·거절·취소 | 부분 Pass / 미충족 | history.json: BLOCK·EXPIRED·APPROVE 분리. 별도 CANCEL 없음 |

최종 application 1, 연결 PO 1, followup 1, final decision 2,
audit 8이었다. VIEWER의 승인 및 attention 쓰기는 stdio MCP에서 거부됐다.
MANAGER 승인 및 같은 key 재전송은 동일한 PO 3을 반환했다. BLOCK 재전송도
결정·감사·발주·후속 수를 늘리지 않았다. version/hash 불일치는 HTTP 409와
전체 상태 수 불변으로 확인했다. 가격 변경 후 승인 시도는 EXPIRED로
남았으며 발주가 없었다. 가격 복구 후 같은 Case를 재계산해 새 승인을 받았다.

발주는 밀가루 5 KG × 1,200원 = 6,000원, 설탕 5 KG × 1,500원 =
7,500원, 포장재 60 EA × 50원 = 3,000원으로 합계 16,500원이다.
주문일 2026-10-05, 납기 2026-10-07, 입고량은 모두 0이었다.
`taxInvoiceNumber`와 `taxInvoiceDate`는 null이었다. 전자세금계산서 발급은
실행하지 않았다. 승인 발주 연결 inbound는 0, 생산 LOT는 fixture의
기존 8개 그대로였다. 후속 책임을 끝내기 위한 입고·생산을 만들지 않았다.

## 개별 구매안 이력

동일 Case 1의 액션은 아래와 같다. EXPIRED는 서버가 입력 변경을 감지한
상태이며 인간의 취소 결정으로 세지 않는다.

| action | version | 상태 | 인간 결정 | 사용자 |
|---|---|---|---|---|
| 1 | 1 | BLOCKED | BLOCK | 3 (MANAGER) |
| 2 | 2 | EXPIRED | 없음 | 없음 |
| 3 | 3 | APPROVED | APPROVE | 3 (MANAGER) |

- version 1 hash:
  `fe9da136df656638516baeed7db071102529b67ad9c2f8a686658cdf49afb030`
- version 2 hash:
  `4f385f1e5ca1479c012f179940ff6aec17d52950cf2c2e245b40c67f6ff3e08e`
- version 3 hash:
  `b0d5a9905541ede3877bdb496c85359c37ea06164ebca8505eb09f20f9b4e348`

`governance_audit_logs`의 action 3 PURCHASE_APPLIED 행에 UPDATE,
DELETE, 전체 TRUNCATE를 각각 실행했다. 모두 nonzero로 거부됐고 각 시도
뒤 원행 JSON이 같았다. 예시 오류는 다음과 같다.

```text
ERROR: governance_audit_logs is append-only: UPDATE is not allowed
ERROR: governance_audit_logs is append-only: DELETE is not allowed
ERROR: governance_audit_logs is append-only: TRUNCATE is not allowed
```

## 한계와 실패 보존

`PurchaseDecisionRequest`는 APPROVE|BLOCK만 허용한다. CANCEL 요청은
HTTP 400이며 결정·감사·ERP 상태를 바꾸지 않았다. BLOCK은 구매 업무와
대기를 CANCELLED로 만들지만 독립적인 인간 CANCEL 이력은 아니다.
이 조건의 제품 범위 결정 또는 구현이 끝나기 전 #34를 완료로 처리하지 않는다.

agent Run의 업무 배정은 fixture SQL, claim·계산·제안은 수동 HTTP였다.
인간 승인은 실제 stdio MCP였다. model은 호출하지 않았다. 실제 LLM UAT,
자율 업무 배정, 외부 공급사 전송, 운영 발주 및 운영 감사 권한 검증은 하지
않았다. 이번 변경은 문서와 인수 script이며 backend unit/SIT를 반복 실행하지
않았다. 현재 JAR build와 실제 HTTP·stdio·DB 검증을 실행했다.

초기 시도 DB와 로그는 삭제하지 않았다. 첫 시도는 업무 ref의 20자 제한,
v2는 빠진 local profile로 claim 403, v3는 다른 Case의 동일 warehouse
계획으로 409를 만났다. 짧은 ref, 명시적 local profile, 동일 Case 연속
version으로 수정한 v4와 snapshot을 추가한 v5가 통과했다. URL·DB guard와 생산 전후 비교를
보강한 최종 v6도 통과했다. 이 실패는
현재 제품의 인수 실패로 분류하지 않았다. 각 DB와 `run*.log`를 보존했다.
