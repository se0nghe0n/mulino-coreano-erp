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

## 첫 proof의 DB 불변성

f2139fe0의 실제 PG 검사에서 Roots.sourceVersion UPDATE가 성공하는 RED를
보존했다. V42는 OLD 또는 NEW kind가 GOAL_QUANTITY_SHORTFALL인 모든
UPDATE를 거부한다. kind 진입/이탈, org/ID 변경과 동일값 UPDATE도 포함한다.
기존 DELETE guard, 다른 kind의 UPDATE, scope/assignment의 transfer는 유지한다.
새 table/column/PK/FK나 backfill은 없다. V41→V42 및 fresh V42 검사에서 기존
column/constraint inventory와 최초 root 값이 동일한지 확인한다.

`AssessmentService.require`는 target minimum과 일치하는 quantitySum leaf를
센 뒤 정확히 하나일 때만 proof를 인정한다. 같은 target leaf 두 개가 마지막
leaf 선택으로 통과하던 HTTP/PG RED는 별도 artifact로 보존한다.

## 별도 남은90

목표 부족10은 PO 없는 실수령90의 RECEIPT_UNALLOCATED와 다르다. 진단은
부족10만 실제 후속 Work로 이전하고, 부모의 미배분90은 그대로 OPEN임을
확인한다. 미배분90을10으로 바꾸거나 같은 successor Work로 옮기지 않는다.
원래 alternative-close까지의 통과를 주장하지 않는다. 다음 경계는 실제
미배분90의 별도 담당·배분·해소 방식에 대한 coordinator의 명시적 결정이다.

## 집중 검증과 별도 합성 반례

이전 실행은 `.local/t11-goal-shortfall/`, V42 후속은
`.local/t11-goal-shortfall-v42/`에 원문·JUnit·source/JAR hash·SQL·정리를
보존한다. 후속 검사는 기존26 HTTP/PG, 추가 합성 정의·평가·수령 반례,
backend evaluator/책임/migration, Python 보존/vocabulary, prepare를 구분한다.

별도 합성 정의는 원본 fixture의 메모리 사본에만 적용한다. 실제 평가에서
중복 target leaf, 비수량 timeIn 실패, shortfall에 미지원인 any 조건을 확인하고
효과0으로 거부한다. 평가 없음은 공개 createDraft→activateWork 경로를 사용한다.
UNVERIFIED는 실제 source 없는 Work를 assessGoal로 평가한다.

추가5 수령은 새 문서·event·provisional·동일성 대조·canonical·confirmReceipt를
실행한 별도 합성 물리 사실이다. 원래90과 별개 range/canonical이며 실제 부모
인식량95/목표100을 확인한다. 현재 부족5 요청과 과거 부족10 요청 모두 현재
assignment를 보존하는 HELD이며 최초 root10을 바꾸지 않는다. 원래 두 allocation,
종료된 자식, 원본 미배분90은 유지한다. 추가5를 원래 fixture 사실로 쓰지 않는다.

source 정정, goal revision, 정책 변경, open grant 폐기는 실제 이전 후에 검사한다.
현재 권한이 없으면 책임의 상세를 공개하지 않고, source/정책 변경 시 기존
root와 유효한 후속 assignment를 구조화 HELD로 돌려준다. resolve/waiver 의미는
추가하지 않는다. 전체 backend/harness 및 원래 full flow/실모델은 재실행하지 않는다.

V42 후속 최종 집중 검증은 backend115, HTTP/PG36(기존26+신규10),
Python33, prepare12가 모두 PASS다. precision/scale까지 포함한 migration2를
추가로 재검사했고, HELD 응답4개는 읽기 전용 SQL의 현재 assignment와
owner/supervisor/nextAction/nextCheckAt를 대조했다. 수정 전 중복 leaf의
APPLIED 반례(2 PASS/1 FAIL)는 `ambiguous-red/`에 보존한다.
Task/Step의 통합 gate와 미배분90의 별도 업무 결정은 계속 OPEN이다.
