---
name: supply-chain
description: Use when a Mulino SUPPLY_CHAIN Run needs a persisted replenishment calculation from order history, multilevel BOM, inventory and supplier terms, or when reviewing stock and LOT traceability concerns.
---

# Supply Chain

공급망 역할은 재보충 필요량과 근거를 계산한다. 현재 실행 가능한 범위는 서버의 재보충 계획 API다. 먼저 [공통 실행 계약](../runtime.md)을 읽는다. 업무 흐름 기준은 저장소의 `docs/02_flow.md`이며 생산·입고·출고를 직접 변경하지 않는다.

## 계산 절차

기본 Case·plan view가 complete=false면 계산 결과를 추측하지 않고 인간
확인이 필요하다고 FAILED로 반환한다. 기준일은 서버 planningBasis와
plan asOf이며 native 세션 날짜를 대신 쓰지 않는다. sourceSnapshot과
일별 audit을 읽지 않아도 compact result의 status·issues·purchases·
totalAmount는 같은 서버 계산의 결과다. 현재 업무 연결은 plan show의
currentAssociation과 Case planningAttempts로 확인하며 최종 검증은
서버가 수행한다. fullRead는 원자료 audit이 필요한 경우에만 사용한다.

1. `mulino case show <caseRef>`로 현재 업무와 `caseMetadata.replenishment`를 읽는다. 요청의 `warehouseId`, `productIds`, `targetDate`를 유지한다. 범위가 없거나 모호하면 필요한 값을 구체적으로 설명하고 FAILED를 반환한다.
2. `mulino plan calculate <caseRef> --json '<본문>' --request-key '<workItemRef>:plan:initial'`을 호출한다. 본문은 `{"warehouseId":1,"productIds":[1,2]}`처럼 정확한 Case 범위를 사용한다. `horizonDays`를 생략하면 서버가 목표 날짜로부터 계산한다. 오늘 기준으로 30일을 다시 더하지 않는다. 단위·소수·날짜 계산은 서버가 수행한다.
3. 반환된 계획 `ref`로 `mulino plan show <ref>`를 호출해 실제 저장을 확인한다. `result.status`가 READY이고 현재 업무에서 만든 최신 계획일 때만 완료를 제안한다. NEEDS_ATTENTION 또는 계산 거부이면 원인·요구 자료·반환된 참조를 설명하고 FAILED로 끝낸다. 과거 READY 결과를 현재 실패의 완료 근거로 사용하지 않는다.
4. 최종 JSON의 `resultRef`에 계획 참조를 넣고 compact view에 실제로 있는 기준일·수요 요약·구매 순소요량·선택 공급처·수량·금액·issues를 `summary`에 짧게 설명한다. 생략된 일별 생산·allocation audit을 읽었다고 주장하거나 재계산하지 않는다. READY와 현재 업무 연결은 compact view와 서버 완료 검증으로 확인할 수 있다. 후속 구매안 준비는 Orchestrator가 Procurement에 배정한다. 구매안을 직접 만들거나 승인하지 않는다.

```json
{"outcome":"DONE","summary":"서버에 재보충 계획을 저장했습니다. 선택 공급처와 구매량은 계획에 있으며 구매안 검토가 필요합니다.","waitingConditions":[],"resultRef":"PLAN-실제응답참조"}
```

CLI 성공만으로 업무가 끝나지 않는다. 실행기가 최종 결과를 전달하면 서버가 최신 계산 시도와 원본 업무 연결을 다시 검증한다. 명시적 전이가 필요하면 `mulino work transition <workItemRef> --json '{"outcome":"DONE","summary":"계획 저장 확인","waitingConditions":[]}' --request-key '<workItemRef>:finish'`를 사용하고 실제 응답을 따른다.

## 재고와 추적성

현재 가용 재고와 예정 입고를 구분한다. 생산·LOT·출고 수량 대사가 실패하면 임의 보정이나 로컬 계산으로 계속하지 않는다. `suppliers → purchase_orders → purchase_order_items → inbound → raw_material_lots → production_ingredients → production_lots → outbound_lots → outbound → orders → customers`의 순·역방향 관계를 보존한다. 출고 LOT 합계는 출고 수량과 같아야 하고 원재료 사용량은 잔량과 맞아야 한다.

FEFO·품질 조치의 CLI는 제공되지 않는다. 배정된 LOT trace 조회는 아래 범위에서 제공한다. 해당 요청을 재보충 계산으로 대체하거나 미구현 명령을 만들어 실행하지 않는다. 품질·인증 예외는 검토 대상으로 알리며 원래 업무의 담당과 미완료 상태를 보존한다.

`mulino lot trace <lot-id>`는 현재 SUPPLY_CHAIN Run의 Case·Work와
metadata.lotId에 한정된다. 다른 Case·LOT을 조회하거나 리콜을 제안하지
않는다. ORCHESTRATOR는 해당 업무에 명시적인 lotId 조회 범위를 배정할 수 있다.
complete=false이면 원자료 보완을 요청하고 영향 출하 근거를 모두 보존한다.

## Case 증거·Claim (#44)

관측 출처는 `mulino evidence register CASE_REF --json SOURCE --request-key KEY`로
기록한다. sourceType·externalRef·observedAt·title과 content 또는
contentUri·contentHash를 제공한다. 원자료를 정정할 때는 동일 명령에
correctsEvidenceRef·correctionReason을 추가한다. 기존 원본은 삭제하지 않는다.
주장은 `mulino claim create CASE_REF --json ASSERTION --request-key KEY`로
ASSERTED 상태를 만들고 `mulino claim link CASE_REF CLAIM_ID --json LINK
--request-key KEY`로 evidenceRef와 SUPPORTS/REFUTES 관계를 연결한다.
SUPPORTS는 인간 검증이나 ERP 승인 권한이 아니다. 반박 근거는 지우지 않고
CONFLICTED 상태와 이력으로 남긴다. VERIFIED/REFUTED는 Case 인간의
명시적 판단이며, 에이전트는 해당 상태를 쓰지 않는다.

반박 정정으로 이전 Claim의 모순을 지우지 않는다. 새 해석은 claim create의
supersedesClaimId·supersessionReason으로 같은 Case·subjectType/ref의 이전
Claim을 참조하며 새 ASSERTED를 만든다. 검토한 현재 원본을 새 Claim에
명시적으로 연결하고 인간 판단을 기다린다. 이전 상태와 이력은 유지한다.
