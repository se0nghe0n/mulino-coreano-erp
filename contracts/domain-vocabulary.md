# 도메인 어휘 계약

[`domain-vocabulary.json`](domain-vocabulary.json)은 backend가 내보낼 수 있는
명령 outcome, 구조화 오류 code, 의무 kind의 단일 목록이다. 계획 §3.4는
outcome 6개와 오류 code 8개를 "처럼"으로 예시하고 §5.3·§7은 의무 종료와
권한을 정한다. 이 파일은 그 이름을 그대로 쓰고, 계획에 없는 이름은
`extension`에 계획 근거와 함께 이유를 적는다.

`backend/src/test/java/com/mulino/core/DomainVocabularyContractTest`가 main
source를 정적으로 읽어 다음을 강제한다.

- 모든 `new DomainError(outcome, code, …)`와 `new ResponsibilityHeld(code, …)`의
  code와 outcome 조합이 목록에 있고, 목록의 모든 code가 실제로 emit된다.
  outcome이 변수인 `COMMAND_NOT_APPLIED`는 미적용 outcome 전체를 선언해야 한다.
- `CommandOutcomes.ALL`·`NOT_APPLIED`, `command-response.schema.json`의 enum,
  source의 literal `"outcome"` 값이 목록(명령·검증·내부 outcome)과 같다.
- `ObligationClosureCatalog`의 kind 집합·면제 권한·해소 증거·자동 종료가
  목록과 같다. `createObligation`도 catalog 밖 kind를 만들 수 없다
  (`REJECTED/TYPE_INVALID`).

## outcome

| outcome | 근거 | 의미 |
|---|---|---|
| APPLIED | §3.4 | 효과·감사·결과가 한 거래로 commit됐다 |
| ACCEPTED_PENDING_EXTERNAL | §3.4·§7.3 | 로컬 효과와 outbox는 commit됐고 외부 결과는 미확정이다 |
| WAITING_APPROVAL | §3.4·§7.1 | 승인 결정을 기다린다. 효과0 |
| NEEDS_INPUT | §3.3·§3.4 | 활성 목표를 정하는 입력이 빠졌다. 효과0 |
| REJECTED | §3.4 | 요청이 계약·권한·적격량을 만족하지 않는다. 효과0 |
| CONFLICT | §3.4·§4.2·§7.3 | 요청이 읽은 상태나 멱등 계약이 commit 상태와 다르다. 효과0 |
| HELD | extension | 아래 설명 |

`HELD`는 계획 §8 "미지원 과거 evaluator는 … 효과 없이 보류·담당에게
알린다", §7.1 "policy 미설정 행동은 효과 없이 차단하고 필요 확인을
반환한다", §5.3 잔여 책임 유지를 한 outcome으로 묶은 확장이다. 요청이
틀린 것이 아니라 서버가 지금 판정할 근거(지원 버전·현재 정책·검증 증거·
정의된 의무 종료 경로)가 없을 때 쓴다. 효과는 0이고 현재 책임은 그대로
남는다. 의무 종료가 미정의이면 응답 `responsibility`에 assignment·owner·
nextAction·nextCheckAt·closure 계약을 담는다.

`PENDING_EXTERNAL`은 삭제했다. backend가 만든 적이 없고 허용 목록에만
있었다. 계획의 외부 미확정 outcome은 `ACCEPTED_PENDING_EXTERNAL` 하나다.
`VALIDATED`는 `validateCommand`의 효과0 검증 결과(§3.3)이고, `BUSY`·
`STALE_EXECUTION`은 복구 sweeper 내부 신호라 공개 명령 응답이 아니다.

## 같은 code의 두 outcome

| code | outcome | 구별 |
|---|---|---|
| POLICY_UNRESOLVED | HELD | 명령 정책 rule·승인 mapping·현재 정책이 없다. 업무 owner의 책임이 남는다 |
| POLICY_UNRESOLVED | REJECTED | RECORD 접수 profile·intake owner가 없거나 명령 guard가 없다. 맡길 owner가 없어 보류가 아니라 거부다(§5.3) |
| EVIDENCE_CONFLICT | CONFLICT | 요청이 대체·무효화된 근거나 다른 hash의 같은 원천 key를 가리킨다 |
| EVIDENCE_CONFLICT | HELD | 유효한 원천끼리 상충해 대조 책임을 남긴다 |
| EVIDENCE_UNVERIFIED | HELD | 정확한 범위·원본·현재 검증 증거가 아직 없다 |
| EVIDENCE_UNVERIFIED | REJECTED | canonical 동일성·원천 검사가 끝나지 않은 사실 기록 요청이다 |

## 계획 이름으로 바꾼 code

| 이전 | 이후 | 위치(`backend/src/main/java/com/mulino/`) |
|---|---|---|
| CONFLICT/REVISION_CONFLICT | CONFLICT/STALE_REVISION | `application/work/WorkLifecycle.java` 업무 lifecycle 명령의 expectedRevision 검사 |
| REJECTED/REVISION_CONFLICT | CONFLICT/STALE_REVISION | `InventoryCommands` merge parent, `FulfillmentCommands` 종료·대체 배분과 과거 판매 행, `StockPrimitives.leaf` 소모된 parent |
| REJECTED/REVISION_CONFLICT | REJECTED/TYPE_INVALID | `StockPrimitives.leaf` 발생 시각이 segment validFrom보다 앞섬 |
| REJECTED/REVISION_CONFLICT | CONFLICT/STOCKTAKE_ALREADY_APPLIED | `InventoryCommands`, `StockPrimitives.adjust` |
| REJECTED/REVISION_CONFLICT | REJECTED/ALLOCATION_UNRESOLVED | `StockPrimitives.decrease` 배분 책임이 남은 폐기 |
| REJECTED/SCOPE_INELIGIBLE | REJECTED/INSUFFICIENT_ELIGIBLE_QUANTITY | `FulfillmentCommands` 판매 구간·정지 배분·판매/출고 적격 범위, `QualityEligibility` 정지 배분·교집합 부족 |
| REJECTED/SCOPE_INELIGIBLE | CONFLICT/STALE_REVISION | `QualityEligibility` 종료·대체된 배분 |
| REJECTED/QUANTITY_CONFLICT | REJECTED/INSUFFICIENT_ELIGIBLE_QUANTITY | `FulfillmentCommands` 이미 예약된 물리 구간 |
| REJECTED/QUANTITY_CONFLICT | REJECTED/SALES_LINE_QUANTITY_EXCEEDED | `FulfillmentCommands` 판매 행 남은 주문량 초과 |

정확한 줄 번호는 JSON `renamedCodes`에 있다. `SCOPE_INELIGIBLE`은 수량
부족이 아닌 범주 불일치(배분 행동·고객, 창고 보관 확인, 수령 보관자
actor·권한)에만 남긴 extension이다. `SALES_LINE_QUANTITY_EXCEEDED`는 주문량
초과라 적격량 부족과 구별한다.

## 제외한 emitter

`@Profile("platform-spike")` S0 spike(`PlatformCommands`, `Platform*`
adapter)의 `REVISION_CONFLICT`·`INSUFFICIENT_QUANTITY` 등은 제품 profile에서
bean이 없어 도메인 계약이 아니다. MCP JSON-RPC protocol 오류(정수 code)도
도메인 `error.code`가 아니다.
