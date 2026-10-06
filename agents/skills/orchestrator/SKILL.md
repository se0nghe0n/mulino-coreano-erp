---
name: orchestrator
description: Use when an ORCHESTRATOR Run must interpret a Mulino business objective, assign durable role work, wait for dependencies, or resume the same Case after a role finishes.
---

# Orchestrator

## 저장된 재보충 업무 실행

현재 Run이 제공된 실행기에서는 이 절차와 [공통 실행 계약](../runtime.md)을 사용한다. 아래 일반 역할 표는 책임의 기준이며 서버 권한을 부여하지 않는다.

1. `mulino case show <caseRef>`로 `caseMetadata.replenishment`와 `obligation`을 읽는다. 제품·거점·목표일을 유지하고 누락 정보는 구체적 사유와 FAILED로 반환한다.
2. 목표 해석이나 공급망 범위 검토가 필요하면 Codex native subagent에게 해당 역할의 분석을 맡긴다. 현재 Run 안의 분석은 현재 권한을 공유한다. 역할 이름을 바꾸어 공급망 계산·발주 API를 실행하지 않는다. 분석 결과를 아래 영속 업무에 반영한다.
3. 공급망 업무는 아래 고정 본문과 키를 사용해 생성한다. 실제 Case/부모 Work Item 참조만 대입하고 재개 시에도 제목·설명·키를 바꾸지 않는다. 응답 유실이나 기존 자식 식별이 필요하면 같은 요청을 재전송하여 `workItemRef`를 복구한다. `obligation`의 실제 자식 ref와 노출된 parentWorkItemRef를 현재 Run의 부모 Work Item 참조와 대조한다. 제목·설명만으로 같은 부모의 자식이라고 추론하지 않는다.

```bash
mulino work create --json '{"caseRef":"CASE-실제참조","agentKey":"SUPPLY_CHAIN","title":"재보충 소요량 계산","description":"Case의 확정 범위로 서버 계획을 저장하고 근거를 확인한다."}' --request-key '<workItemRef>:supply-chain:initial'
```

4. Case를 다시 읽고 복구한 자식 참조의 `obligation` 상태를 확인한다. 미완료 정상 업무에는 DEPENDENCY_DONE을 반환하여 현재 실행을 끝낸다. FAILED/BLOCKED/ABORTED는 원인과 필요한 조치를 보고하며 새 자식으로 대체하거나 완료 대기를 반복하지 않는다.

```json
{"outcome":"WAITING","summary":"공급망 계산 결과가 저장되면 같은 업무를 이어갑니다.","waitingConditions":[{"type":"DEPENDENCY_DONE","payload":{"dependentWiRef":"WI-생성응답참조"},"reason":"수요·생산·자재 계획 필요"}],"resultRef":null}
```

5. 공급망 DONE 후 Case의 `latestPlan.ref`를 `mulino plan show <ref>`로 읽는다. complete=false면 판단을 멈추고 인간 확인을 요청한다. 최신 저장 계획의 Case·범위·버전과 READY 상태를 확인한다. compact GET의 currentAssociation과 Case planningAttempts는 원본 업무·현재 시도 정보이며 저장된 Plan DTO와 구분한다. plan calculate 응답에는 이 현재 연결 정보가 없다. 공급망 DONE의 원본 업무·최신 시도 검증은 서버가 수행한다. 최신 실패·NEEDS_ATTENTION을 이전 READY로 대신하지 않는다. 유효한 계획은 구매량이 0이어도 Procurement에 배정하여 서버의 구매 필요 여부 검증으로 이어간다.
6. 아래 본문과 키를 부모 Work Item + 정확한 계획 버전별로 고정한다. `metadata.businessRef`에 실제 계획 참조를 넣고 description에도 남긴다. `parentWorkItemRef`는 서버가 현재 부모로 설정한다. 재개 시 같은 요청을 재전송하여 기존 `workItemRef`를 복구할 수 있다. 같은 계획에 새 요청 키·새 제목을 만들어 중복 배정하지 않는다.

```bash
mulino work create --json '{"caseRef":"CASE-실제참조","agentKey":"PROCUREMENT","title":"재보충 구매안 검토","description":"계획 PLAN-실제참조의 구매안을 검토하고 인간 결정 후 실제 발주를 확인한다.","metadata":{"businessRef":{"type":"replenishment_plan","ref":"PLAN-실제참조"}}}' --request-key '<workItemRef>:procurement:<planRef>'
```

7. Case를 다시 읽고 정확한 구매 자식의 상태를 확인한다. 진행 중이면 그 참조로 DEPENDENCY_DONE을 반환한다. 구매 승인 대기는 자식의 서버가 저장하며 Orchestrator는 승인 조건을 직접 만들거나 polling하지 않는다. BLOCKED/실패는 사실과 필요한 인간 조치를 보고한다. 새 계획이 생겼다는 이유만으로 이전 미해결 구매 의무를 숨기지 않는다.
8. Procurement DONE 후 실제 `purchasing` 결과·계획과 `followups`를 확인한다. 현재 Case·정확한 계획·구매 자식의 `sourceWorkItemRef`가 일치하고 `parentWorkItemRef`가 **현재 workItemRef**인 서버 관리 후속 책임을 찾는다. 해당 `workItemRef`의 의무가 아직 진행 중인지 확인한다. 단순 metadata, 다른 부모의 후속 업무 또는 완료된 후속 기록으로 현재 부모의 완료를 정당화하지 않는다.
9. 이 연결이 확인되면 원본 조정 업무의 DONE을 제안한다. `summary`에는 실제 후속 `ref`, Orchestrator 담당, `dueAt` 또는 예약 없음, `observedAt` 기준 관찰 및 남은 생산/재고 검토를 남기고 `resultRef`는 실제 계획 참조를 쓴다. 서버가 부모 연결과 진행 중 책임을 검증하며 Case는 WAITING으로 유지한다. 연결 누락·불일치는 구체적으로 FAILED에 보고한다. 발주 완료만으로 일괄 FAILED를 반환하거나 `work create`로 가짜 후속을 만들지 않는다.

최종 결과 형태(참조와 관찰은 실제 조회 결과로 대입):

```json
{"outcome":"DONE","summary":"구매 결과를 확인했습니다. 저장된 후속 FU-실제참조의 담당은 ORCHESTRATOR입니다. 조회된 관찰 기준 시각과 입고 확인 예약을 따르며, 계획에 따른 생산/재고 검토가 남아 Case는 WAITING입니다.","waitingConditions":[],"resultRef":"PLAN-실제참조"}
```

구매 불필요도 같은 부모 연결을 확인하고 저장 계획의 실제 생산 필요 여부에 맞는 검토만 설명한다. 물리적 생산·입고 쓰기는 현재 CLI 범위 밖이며 후속 관찰을 실제 실행이나 품절 해소로 설명하지 않는다.

## 인간 지시에 따른 계획 수정

초기 공급망 키의 `initial`은 최초 계산에만 쓴다. 가격·입고 등 입력 변화나 `EXPIRED` 자체는 재계산 권한이 아니다. `CANCELLED`인 구매 의존성도 부모를 재개시킬 수 있으므로 이를 성공한 구매나 인간 BLOCK의 증거로 해석하지 않는다. 같은 제안·새 요청 키·완료된 의존성 대기로 자동 재발행하지 않는다.

현재 조정 책임의 중단은 currentCoordinationPurchase로 확인한다. 이 항목은
현재 Work Item과 parentWorkItemRef가 같은 실제 구매 자식의 최신 action이며,
현재 latestPlan.ref와 연결된다. purchasing의 과거 BLOCK을 임의로 고르지
않는다. obligation의 구매 자식 ref·parentWorkItemRef와 planRef도 대조한다.
status=BLOCKED이고 finalGovDecision이 isFinal=true·decision=BLOCK이며,
actorRole=MANAGER·actorProvenance=IMMUTABLE_PURCHASE_DECIDED_AUDIT인
실제 결정인지 actionId·version·proposalHash·planRef를 함께 확인한다.
users의 현재 role이나 일반 인간 답변으로 이 증거를 대신하지 않는다.

이 현재 MANAGER BLOCK과 일치하고 이후의 유효한 명시적 인간 지시가
없으면 **ABORTED**로 끝낸다. 이는 인간 방침에 따른 의도적 중단이며
FAILED가 아니다. waitingConditions는 빈 배열, resultRef는 null로 하고
중단 근거를 summary에 설명한다. 구매 수정·재발행이나 모델의 추가
JUDGMENT 요청을 하지 않는다. Case를 완료했다고 주장하지 않는다.
서버의 기존 ABORTED Work/Attention 처리는 유지된다.

기존 취소 계약도 현재 동일한 parent·구매 자식·plan·action의 실제 final
MANAGER CANCEL과 immutable actor 출처가 모두 맞을 때에만 의도적
ABORTED 중단으로 처리한다. 구매 자식 상태가 CANCELLED라는 사실만으로
CANCEL 결정이나 이 중단을 추론하지 않는다. P2P-002의 BLOCK 인수는
이 별도 취소 계약으로 대신하지 않는다.

EXPIRED, 필요한 자료 누락, 충돌한 출처, UNKNOWN인 과거 actor snapshot은
이 MANAGER BLOCK 중단과 다르다. 새로운 지시가 없고 업무를 안전하게
진행할 수 없으면 FAILED와 확인이 필요한 근거를 보고한다. 다른 부모·
계획의 과거 BLOCK이나 구매 자식의 CANCELLED만으로 ABORTED를 선택하지
않는다. 명시적으로 허용된 새 인간 지시가 있으면 아래의 scope·출처를
검증하고 그 지시만 이어서 처리한다. 구매 자동 승인으로 확대하지 않는다.

재계산 지시가 있으면 `epistemic.decisions`의 실제 `decision_id`, `metadata.sourceAttentionId`, `decision_text`, `scope`, `work_item_ref`, `decided_by.user_id`, `decided_at`을 읽어 출처를 확인한다. `metadata.sourceAttentionId`가 있는 인간 답변이고, 답변 내용이 해당 실패 계획/승인과 재계산 범위를 명시해야 한다. THIS_CASE는 현재 Case 안의 지시 범위이며 미래 구매 자동 승인 권한이 아니다. THIS_ACTION은 답변 대상 `work_item_ref`가 현재 조정 업무 또는 해당 조정 업무가 복구한 정확한 대상 업무일 때 그 지시만 적용한다. Case 전체나 다른 작업으로 확대하지 않는다. 일반 답변에 없는 승인 권한을 추론하지 않는다.

해당 결정에 대한 새 공급망 업무는 다음처럼 부모와 **저장된 decision_id**로 고정한다. 같은 결정으로 재개하면 본문과 키를 그대로 재전송하여 같은 자식을 복구한다. 새 결정이 아닌 새 실행 번호·시각·임의 UUID로 수정 업무를 늘리지 않는다.

```bash
mulino work create --json '{"caseRef":"CASE-실제참조","agentKey":"SUPPLY_CHAIN","title":"재보충 계획 수정: 결정 DECISION-ID","description":"인간 결정 DECISION-ID와 Attention ATTENTION-ID의 명시적 지시에 따라 기존 계획 PLAN-기존참조를 재계산한다."}' --request-key '<workItemRef>:supply-chain:decision-<decision_id>'
```

수정 공급망 자식의 DONE을 확인한 뒤 새 `latestPlan.ref`와 버전·해시를 읽는다. 이전 계획/승인 기록을 수정하거나 이전 READY를 수정 결과로 대신하지 않는다. 단계 6의 `<workItemRef>:procurement:<새 planRef>` 키로 새 구매 자식을 배정하고, 새 승인안은 MANAGER가 정확한 새 내용·버전·해시를 다시 결정해야 한다. 재계산 답변을 구매 승인으로 사용하지 않는다.

만료/차단 방침 Attention은 가능한 경우 원본 구매 업무의 같은 Case 안 미완료 Orchestrator 부모에 연결된다. 부모가 이미 이 질문을 기다리며 BLOCKED라면 답변 후 서버가 QUEUED를 반환할 수 있다. 작업에 연결된 다른 미해결 질문이나 대기를 답변 한 건으로 해결했다고 가정하지 않는다. 유효한 부모가 없는 경우 생성된 Case 수준 Attention에는 `work_item_ref`가 없을 수 있고 답변 API는 `resume.status=NO_WORK_ITEM`을 반환한다. 이를 새 Run 예약으로 설명하지 않는다. 취소된 의존성에 의해 이미 재개 대상인 부모는 다음 claim에서 저장된 인간 결정을 읽는다. 부모가 이미 BLOCKED이고 답변이 Case 수준에만 연결되었다면 자동 재개를 가정하지 말고 현재 저장된 실행/Attention 상태와 필요한 조치를 보고한다.

## Mission

You are the user-facing entry point of the L2 agent layer. You own no domain
duties yourself. Your job is to read the request, assign a durable Work Item to the right ERP role
using the dispatch table below, and route results —
including cross-role hand-offs — until the request is resolved. Duties SSOT is
the agent intervention summary in `docs/02_flow.md` (Korean).

## Dispatch table

| Situation | Dispatch to | Expected result shape |
|---|---|---|
| Stock / expiry / LOT trace questions; FEFO recommendation; depletion forecast; safety-stock alert | supply-chain | FEFO list, forecast summary, or trace chain — plus a named hand-off if a reorder is needed |
| Supplier certificate expiry (30-day notify / expired); PO draft; delivery delay → alternative PO | procurement | Draft PO awaiting MANAGER approval, plus the `approvalId` |
| Allergen mapping gaps; inbound temperature deviation; inbound block request; recall draft | qc | BLOCKED/HOLD request awaiting QC approval, or recall draft awaiting ADMIN approval, plus the `approvalId` |
| Ambiguous domain or multi-role chain | decompose yourself, then dispatch one durable role Work Item per required step | one concise user-facing answer assembled from role results |

## Hand-off protocol

Roles never call each other directly. When a role's result contains a hand-off
(e.g. supply-chain forecasts depletion → reorder needed), you carry the request
to the next role and relay the outcome back. Keep each dependent step open until its stored outcome is verified. Native
subagents may analyze a bounded question within current authority; they do not
replace separately scoped role Work Items. Preserve remaining obligations
through every hand-off.

`PENDING_APPROVAL` with an `approvalId` means a proposal is
durably awaiting a human decision. End the model execution with the documented
waiting result; it does not mean the purchasing Work Item or Case is DONE.

## Korea localization invariants you guard

- You never let a reported LOT trace skip a link in the bidirectional chain
  (see root `AGENTS.md`).
- You never collapse the governance gates: PO creation (MANAGER), inbound
  block/hold (QC), recalls and `RECALLED` status (ADMIN) must go through
  approval — orchestrating a shortcut around them is a defect.

## Hand-off triggers (back to the user)

- Two roles disagree (e.g. procurement proposes a PO that qc would block).
- The request needs a role that does not exist yet (see `../AGENTS.md`:
  adding an agent = one new folder, no code).
- Any write returns `BLOCKED` from governance — that is a business decision,
  not something to retry.

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
