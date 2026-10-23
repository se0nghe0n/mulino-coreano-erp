# T11 parent90 후속 구현 제안 — 아직 적용하지 않았다

원래 child→parent 링크, canonical90 하나, 자식30/60, 부모 목표100과
부족10을 유지하는 제안이다. 제품·migration·fixture·권한은 변경하지 않았다.
근거는 [계획 §5.1–5.3 및 D11](../../../docs/ontology-implementation-plan.md),
`7fd055f8`의 실제 HTTP/PG 증거다. 이전 실패와 JAR/source hash는
`.local/t11-parent-followup/`에 보존한다. 이번 작업은 역변환 bridge만 수정한다.

## 1. 하나의 range allocation을 양쪽 목표가 읽는다

현재 [WorkRepository.credit/contributions](../../../backend/src/main/java/com/mulino/domain/work/WorkRepository.java)는
한 allocation을 `targetWorkId + goalId`에 저장·조회한다.
[AssessmentFactProvider.load](../../../backend/src/main/java/com/mulino/domain/evaluation/AssessmentFactProvider.java)는
자기 canonical이 아니고 target credit도 없으면 그 사건을 제외한다.
따라서 child30은 실제 연결을 만들어도 입력이 없고 UNVERIFIED다.
이는 `closeWork`의 판정 검사를 완화해서 해결할 문제가 아니다.

제안하는 저장·조회 변경은 다음과 같다.

| 파일·심볼 | 최소 변경안 |
|---|---|
| `backend/db/work.cds:WorkContributions`, 새 Flyway migration | 기존 target `goalId`를 유지하고 `sourceWorkId`, `sourceGoalVersionId`, `sourceConditionId`를 추가한다. source goal은 같은 org의 `(GoalReferences.ID, workId)` FK로 고정한다. 기존 `conditionId`는 target binding이다. |
| `WorkLifecycle.createWorkLink`, `WorkRepository.credit` | 같은 transaction/fence에서 실제 source/target Work의 현재 GoalVersion을 각각 읽어 고정한다. 두 목표에서 canonical 사건·단위·범위·공개 quantity condition이 유일하게 호환되는지 검증한다. source/target 조건이 모호하면 거부한다. 외부 입력의 고정 goal ID를 무검증으로 믿지 않는다. |
| `WorkContributionRead`, `WorkRepository.contributions` | 기존 target view와 새 source view를 반환하되 **같은 allocation ID**를 쓴다. source view는 요청한 goal ID가 저장된 sourceGoalVersionId와 일치할 때만 포함한다. `SOURCE/TARGET`은 조회 역할이며 새 credit/실물이 아니다. |
| `AssessmentFactProvider.load` | 각 view의 고정 GoalVersion·condition으로 actual range30/60만 Fact에 넣는다. 각 목표의 policy/definition/evaluator·사건 종류·기간·지점·knownAt를 다시 검증하고 snapshot에 allocation/양쪽 goal/condition/policy 근거를 남긴다. 부모의 판정 결과나 quantity90을 자식에게 복사하지 않는다. |
| `AssessmentCorrectionImpact.affected` | credit의 고정 source와 target을 모두 invalidation 대상으로 찾는다. 기존 광범위 item 조회와 과거 snapshot 참조만으로 source 연결을 대신하지 않는다. |
| `WorkReadHandler.contributions` | 동일한 읽기 역할과 고정 goal 참조를 공개한다. 양쪽 조회를 합산해 실제 수령량으로 보고하지 않는다. |

`sourceGoalVersionId`는 필요하다. 지금 Work의 `currentGoalVersionId`를
나중에 읽으면 reviseGoal 후의 목표에 과거 range를 소급하게 된다.
condition은 현재의 임의 문자열이 아니라 **고정 정의의 유일한 quantity
condition**으로 해석·저장한다. 두 GoalReferences가 고정한 기간·정책명·정의와
각 assessment가 실제 사용한 policy ID/hash를 구별한다. 정책 변경 때는
기존 closure의 현재 정책 검사를 그대로 적용한다.

기존 row에는 source goal이 없다. 현재 Work를 보고 backfill하지 않는다.
과거 target 조회는 유지하고 source view는 명시적으로 unknown으로 남긴다.
새 row부터 완전한 source binding을 강제한다. `WorkLinks`에는 현재 mutation
방지 trigger가 없으므로 그것을 불변 source 근거로 가정하지 않는다.
credit에 양쪽 ID를 저장하고 link와의 일치를 insert 때 검사하며, credit이
참조한 link의 identity 변경·삭제도 새 migration에서 막는 편이 안전하다.
기존 V9/V39는 수정하지 않고 migration 번호는 coordinator가 배정한다.

중복 방지는 [V9 range trigger](../../../database/migrations/V9__work.sql)를
유지한다. allocation은 `[0,30)`, `[30,90)` 두 행뿐이며 source용 행을 다시
INSERT하지 않는다. raw canonical90과 같은 사건의 부분 allocation을 동시에
읽을 때도 합이120이 되면 안 된다. 현재 `allocations.isEmpty()` 분기는 부분
credit 하나만 있을 때 부모의 직접90을30으로 줄일 수도 있다. 단일 canonical의
직접 범위와 allocation 범위를 정규화해 겹친 범위를 한 번만 세는 처리가
필요하다. 동일 allocation을 두 view에서 읽는 것과 실제 원장 이중 계상은 다르다.

자식 CLOSED를 credit 제외 조건으로 쓰지 않는다. 자식 종료 전후 부모90은
같다. source goal을 개정하면 그 새 goal에는 자동 승계하지 않는다. canonical
정정/무효화는 양쪽 판정을 stale/pending으로 만들고, CLOSED 자식에는 기존
followup 경로를 적용한다. 과거 assessment·credit·closure는 다시 쓰지 않는다.
새 canonical로의 자동 재결합도 제안하지 않는다.

## 2. 부족10은 수령 미배분90과 다른 의무다

[ResponsibilityService.initialDuty](../../../backend/src/main/java/com/mulino/application/responsibility/ResponsibilityService.java)는
명시된 nextAction이 있을 때 수량 없는 WORK_NEXT_ACTION을 만든다.
[ReceiptCommands.execute](../../../backend/src/main/java/com/mulino/application/trade/receipt/ReceiptCommands.java)는
PO 없는 실제90을 RECEIPT_UNALLOCATED90으로 만든다.
[TradeResidualRemedy.reconcileReceiptShortfall](../../../backend/src/main/java/com/mulino/application/trade/TradeResidualRemedy.java)는
실제 PO line·ReceiptCredits가 있을 때만 RECEIPT_SHORTFALL을 만든다.
현재 T11에는 그 PO/credit이 없으므로 이 경로를 재사용할 수 없다.

이미 공개된 `openObligation`/`createObligation`은 있다.
[ResponsibilityCommands.execute → ResponsibilityService.openDuty/recordDuty](../../../backend/src/main/java/com/mulino/application/responsibility/ResponsibilityCommands.java)는
OCCURRENCE 또는 DECISION(approval/실제 command record)을 source로 받는다.
그러나 현재는 요청 quantity와 goal assessment의 부족분을 대조하지 않으며,
[ObligationClosureCatalog](../../../backend/src/main/java/com/mulino/application/responsibility/ObligationClosureCatalog.java)에는
GOAL_QUANTITY_SHORTFALL도 없다. 범용 명령에 kind/quantity10만 넣거나
FOLLOWUP_REVIEW/RECEIPT_SHORTFALL로 이름을 바꾸는 것은 증명이 아니다.
현재 **goal 부족을 입증하는 전용 public business path는 없다.**

최소 explicit 경로 제안은 새 endpoint 대신 기존 `openObligation`에 검증된
`GOAL_QUANTITY_SHORTFALL` 분기를 추가하는 것이다.

- `AssessmentService`/quantity evaluator가 저장한 정확한 assessment·input
  snapshot에서 고정 Work/GoalVersion/condition, target100, 검증된 distinct
  canonical90, unit, knownAt를 확인하는 `GoalShortfallProof`를 둔다.
  UNSATISFIED라는 문자열만으로10을 만들지 않는다. UNVERIFIED·conflict·
  수량 외 조건 실패·비교 단위/기간 불일치는 수량 의무 생성 근거가 아니다.
- source는 **그 assessment를 저장한 실제 assessGoal command**의 DECISION이다.
  결과의 assessment ID·Work·hash와 COMMITTED 상태를 대조한다. 현재
  `requireSource(DECISION)`의 command 존재/버전 확인만으로는 부족하다.
- `ResponsibilityService.recordDuty`의 해당 kind는 서버가 증명한10을 사용한다.
  caller quantity/scope와 불일치하면 거부한다. 다음 행동/시각은 명시적
  업무 입력이어야 하며 source profile이나 host clock으로 만들지 않는다.
- 새 `GoalShortfallBindings`에 `(org, workId, goalVersionId, conditionId)` unique와
  최초 assessment/source command/root ID를 둔다. root key에 매번 새
  assessment ID를 넣지 않는다. 동일 부족의 재평가·새 idempotency key·경합은
  같은 root와 **현재 유효 assignment**를 반환한다. 이미 이전된 의무를
  원 Work에 다시 열지 않는다. generic `recordDuty`는 source가 바뀌면 새
  key가 되므로 그대로 호출하는 것만으로 이 보장을 얻을 수 없다.
- 최초 root quantity10은 보존한다. 후속 판정에서 부족이 줄거나 늘 때의
  처리도 kind의 소유자에게 둔다. 감소는 정확한 typed goal progress가
  덮는 scope만 해소하고 이전된 owner를 보존한다. 증가·정정은 기존 root를
  임의 resize/재개하지 않고 새 근거에 연결된 차이 또는 후속 검토를 남긴다.
  이 부분을 정하기 전에는 단순 재평가로 자동 해소/재발행하지 않는다.

자동 생성이 제품 요구라면 같은 typed service를 `AssessmentService.assess`
저장 transaction 안의 port로 호출할 수 있다. 이 경우 아직 COMMITTED가 아닌
현재 command의 내부 proof만 신뢰하고 assessment/root/command를 원자적으로
commit한다. 외부 `openObligation`이 미완료 command를 신뢰하게 만들지 않는다.
다만 현재 assessGoal 입력은
workId뿐이고 부모 Work에는 nextAction/nextCheckAt이 없다. automatic hook은
그 설정을 명시적으로 요구해야 한다. **explicit open 경로는 작은 수정과
분명한 담당 입력**, **automatic hook은 누락 방지와 대신 추가 설정 계약**이라는
차이가 있다. 어느 쪽을 채택할지는 coordinator의 제품 결정이다.

추가 차단점도 있다. goalshortfall10을 옮겨도 미배분90은 계속 OPEN이다.
목표 credit은 구매 배분이나 procurement duty의 해소 근거가 아니다.
현재 공개 receipt 기능은 receiveProvisional/confirmReceipt이고, 이미 확정된
무PO receipt를 사후 배분해 이 의무를 해소하는 전용 경로는 찾지 못했다.
기존 MANAGER waiver는 별도 승인·권한·근거가 필요하다. 원래 fixture의
manager에는 approvePurchase만 있으므로 그 권한을 몰래 더하지 않는다.
미배분90의 별도 인계/해소 또는 해당 제품 의미를 먼저 결정해야 원래
alternative-close까지 PASS를 요구할 수 있다.

## 3. 제안하는 명시적 fixture 준비 — 아직 적용하지 않는다

1. 위 explicit 경로를 선택한다면 원래 A에게 `openObligation` 하나를 실제
   roleCapabilities와 grant.actions 양쪽에 명시한다. 기존 조직/품목/기간
   범위를 넓히지 않는다. 내부 assessment hook을 선택하면 이 grant 추가는
   필요 없지만 부모 Work의 nextAction/nextCheckAt 설정이 필요하다.
2. 부모의 UNSATISFIED assessment ID와 command ID/hash로 typed 부족10을
   생성/조회하고 실제 root/current assignment ID를 받는다. 지금 없는
   `getAssessment/obligationId`나 정렬되지 않은 obligations[0]을 쓰지 않는다.
   `getWork.data.obligations`로 그 exact root·kind·quantity·valid를 재확인한다.
3. `transferObligation`에는 원래 target followup·recipient A·quantity10·
   nextAction·nextCheckAt을 유지한다. 새 synthetic `expiresAt` 후보는 기존
   `nextCheckAt=2026-10-08T09:00:00Z`이며 실제 실행시각보다 미래인지 검사한다.
   현재 Work revision은 직전 getWork에서 읽는다. DOC의 acceptanceEvidenceId는
   ledger에 남기고 서버 입력으로 보내지 않는다.
4. 응답 `effects.responsibility.ID/revision`의 실제 PROPOSED handover를
   `acceptHandover`에 전달한다. 수행자는 **원래 recipient A**이며 현재 fixture의
   A와 그 delegator supervisor에는 acceptHandover가 이미 있다. 추가 인수자나
   역할 승격이 필요하지 않다. 새 권한이 필요한 경우는 위 explicit open뿐이다.
5. 수락 전 원 assignment OPEN, 수락 후 원 TRANSFERRED + 같은 root의 후속
   assignment OPEN을 관찰한다. 이후 false-close는 여전히 REJECTED이며
   parent assessment90/100은 UNSATISFIED다. 같은 A의 인계도 실제 accept 없이
   완료하지 않는다. 90의 다른 책임은 이10의 인계로 해소하지 않는다.

## 수용 반례와 구현 정지점

- 동일 canonical/range를 두 번째 credit로 등록하면 전역 overlap 거부;
  다른 org·없는 canonical·unit/condition/기간 불일치도 효과0이다.
- 부모의 직접90 + child30 view가120이 되지 않고, 첫 partial link만 생성해도
  부모 직접90을30으로 축소하지 않는다. 모든 자식 종료 뒤에도 parent90이다.
- source/target 어느 GoalVersion이든 바꾸면 과거 binding은 새 목표에 자동
  적용되지 않는다. source pin이 없는 legacy row를 현재 Work로 보충하지 않는다.
- 원천 정정·상충·정책 교체 때 양쪽 input hash와 closure를 다시 확인한다.
  폐쇄 자식은 재개하지 않고 correction followup을 남긴다.
- 동일 assessment 재처리/동시 실행/새 command key가 부족 의무를 중복 생성하지
  않는다. 다른 kind/목표/기간의90과10은 같은 root로 합치지 않는다.
- 부족이 UNVERIFIED이면0/10으로 단정하지 않는다. 이전·면제·부분 해소 이력은
  새 assessment로 삭제하거나 부활시키지 않는다.
- A 이외의 수락, 만료·거절·grant 폐기, target 비활성, 동시 이관은 원 책임을
  보존한다. DOC만으로 수락된 handover나 resolved duty를 만들지 않는다.

이 문서는 검토용 제안이다. schema 번호·새 kind/closure 정책·자동/명시적 생성
선택·미배분90 처리와 추가 fixture 입력을 coordinator가 확정한 뒤 구현한다.
