# T11 명시적 목표 수량 부족 의무

Task와 현재 Step은 OPEN이다. 원래 T11의 입력·110 oracle·grant는 변경하지
않는다. 이 문서의 HTTP 입력과 open 권한은 별도 합성 진단에만 사용한다.

## 제품 경로

`openObligation(kind=GOAL_QUANTITY_SHORTFALL)`은 실제 COMMITTED
`assessGoal` command의 ID와 canonical hash를 DECISION으로 받는다.
`createObligation` alias는 이 kind를 거부한다. 별도 endpoint나 자동 assessment
hook은 없다.

`GoalShortfallProof`는 Work의 현재 불변 GoalVersion, 최신 assessment,
InputSnapshot의 실제 hash, command 결과의 assessment/work/goal/hash를 대조한다.
현재 사실은 내부 `OPEN_OBLIGATION` purpose로 다시 계산한다. 공개 assessGoal의
권한은 바뀌지 않는다. 요청자 identity를 evaluator identity로 바꾸지 않는다.
물리 사실 admission은 원래 TARGET/요청 Work/원천 Work/ITEM/PLACE의 현재
권한을 각각 검사한다. 기존 command transaction과 Work/authority/policy
fence 안에서 실행한다.

지원 범위는 CUMULATIVE_EVENT의 유일한 quantitySum 속성에 대한 lower bound다.
기존 evaluator의 distinct physical scope 합산을 공용 함수로 사용한다.
quantity 외 조건은 전부 SATISFIED여야 하며 OR/NOT, 상한, 모호한 속성,
UNVERIFIED/conflict/held/pending/새 goal/현재 정책 불일치는 부족량 증명이 아니다.
request scopeJson은 고정 scope에 workId/goalVersionId/conditionId를 더한 값과
정확히 일치해야 한다. quantity와 unit도 계산된 부족분에 맞아야 한다.

root key는 서버가 `(org, workId, goalVersionId, conditionId)`의 JSON에서 만든
SHA-256이다. 최초 command/sourceVersion, assessment/input hash, 정의와 정책,
100/90/10 및 실제 입력을 root.scopeJson에 보존한다. 최초 owner/supervisor는
현재 Work의 활성 Human이며 nextAction/미래 nextCheckAt은 명시 입력이다.
동일 입력의 새 command key/새 assessment는 같은 root와 현재 OPEN assignment를
반환한다. 실제 transfer/accept 후에는 후속 Work의 assignment를 반환한다.
원래 assignment를 다시 열지 않는다.

부족량이나 입력이 변한 기존 root는 재크기 조정·해소·면제하지 않는다.
효과 없는 human-nextAction HELD로 현재 책임을 보존한다. 이 kind의 자동 해소,
수동 해소 증거, waiver 권한은 정의하지 않았다. transfer/accept는 기존 경로다.

## DB 불변성 미해결 경계

V10의 Roots는 `(organizationId,dutyKey)` UNIQUE, root/scope/assignment FK와
지연 수량 보존 검사, DELETE 금지를 갖는다. 그러나 UPDATE 금지가 없다.
실제 PG에서 새 GOAL_QUANTITY_SHORTFALL root의 sourceVersion UPDATE가 성공해
`firstRootProofMustBeDatabaseImmutable`가 RED다. 진단 transaction은 rollback한다.

새 table/column/PK/FK는 필요하지 않다. 이 kind를 OLD 또는 NEW kind로 갖는
Roots의 UPDATE를 거부하는 forward trigger면 최초 proof의 불변성을 보장할 수
있다. 다른 kind의 의미나 기존 migration checksum은 바꾸지 않는다. migration
번호가 승인되지 않아 이번 경계에서는 migration을 추가하지 않는다.
이 DB 불변성 RED가 해소되기 전에는 새 제품 경로의 전체 인수를 주장하지 않는다.

## 별도 남은90

목표 부족10은 PO 없는 실수령90의 RECEIPT_UNALLOCATED와 다르다. 진단은
부족10만 실제 후속 Work로 이전하고, 부모의 미배분90은 그대로 OPEN임을
확인한다. 미배분90을10으로 바꾸거나 같은 successor Work로 옮기지 않는다.
원래 alternative-close까지의 통과를 주장하지 않는다. 다음 경계는 실제
미배분90의 별도 담당·배분·해소 방식에 대한 coordinator의 명시적 결정이다.

## 이번 집중 검증

- 관련 backend 112 PASS, Python T11/T09 보존/vocabulary 33 PASS,
  prepare asset checks 12 PASS다.
- 실제 Spring/PG 결합 26개 중 25 PASS, 1 FAIL이다. 실패는 위 root UPDATE
  불변성이다. 기존 child30/60 CLOSED·parent90 보존 3개도 이26개에 포함된다.
- commit 전 fault의 FIRED 상태와 rollback, 동시 새 key의 단일 root 반환은
  보강한 단일 HTTP/PG 검사로 다시 PASS했다.
- 실제 실행 원문·첫 setup 오류·직렬화/replay 수정 전 실패·최종 실패와
  source/JAR hash·정리는 `.local/t11-goal-shortfall/`에 보존한다.
- 부족량이 실제로 달라지는 추가 수령, nonquantity/모호한 조건, assessment
  없음/UNVERIFIED head의 별도 PG 반례는 이번 실행에 포함하지 않았다.
  전체 backend/harness 및 원래 full flow/실모델도 재실행하지 않았다.
