# T17 인도 goal의 공개 완성 입력

아래 결과는 bffbde51 시점이다. 이후 미확인 root 저장 변경은
[PRODUCT_CLAIM_INPUTS.md](PRODUCT_CLAIM_INPUTS.md)에 별도로 기록한다.

`f2f978b51c0eb9b50d75764ebbb0a0329d6a48ea`의 활성화 준비 앞에
`reviseGoal`을 추가한다. DRAFT를 자동 활성화하거나 원래 주문 Work를
바꾸지 않는다. 이전 DELIVERY_INPUTS의 미완성 결과와 모든 실패
artifact는 역사적 증거로 보존한다. 전체 Task와 Step는 OPEN이다.

## 새로 승인된 전제

`author_delivery_goal_inputs.py`와 `delivery-goal-input-delta.json`은
새 layer만 작성한다. 역변환은 f2f978b5를 복원하고 기존 delivery
layer의 역변환은 다시 bcac964를 복원한다. 이전 네 ledger와 257개
assertion byte block, 원래 source descriptor는 바꾸지 않는다.

- `t17Source`에만 `reviseGoal`을 추가한다. 조직/ITEM all-of scope,
  유효기간, 위임자, 모든 사업 actor의 권한은 그대로다.
- 공개 `getWork`의 원래 `currentGoal.slots`에서 모든 기존 슬롯을
  그대로 가져온다. quantity, dueAt, scope, orderId를 포함한
  contributionScope, place, endpoint, event, deduplication과 provenance를
  상수로 재작성하지 않는다.
- 새 timezone은 fixture의 timezone이다. 새 기간은 fixture 시작부터
  원래 주문 dueAt까지 양 끝을 포함하며, 비어 있으면 작성을 거부한다.
  원래 주어진 기간이나 운영 승인으로 주장하지 않는다.
- `installer-conventions.json`의 유일한 DELIVERED/CUMULATIVE_EVENT
  template `delivered`와 evaluator `core-v1`을 사용한다. 기존 설치
  방식이 발행하던 동일한 definition의 명시적인 fixture alias를 추가해
  실행 시 ID를 연결한다. 어휘나 predicate를 바꾸지 않는다.
- 실제 EVIDENCE convention policy는 `fixture-v1`이고 content는 `{}`다.
  fake version이나 claim policy를 사용하지 않는다. PG 검사에서 현재
  ActivePolicies와 immutable PolicyVersions 행을 실제 대조한다.
- definition은 기존 convention 의미를 유지하며 key 순서만 정규화한다.
  공개 입력 encoder로 실제 org/definition ID가 결합된 전체 content의
  hash를 계산한다. PG 검사는 PUBLISHED 행의 실제 contentHash와
  일치하는지 확인한다. 새 `defaultContext`의 각 입력은 synthetic이고
  `originalAuthorityClaimed=false`, `operatingApprovalClaimed=false`다.
- 수정 뒤 다시 `getWork`를 호출하고 새 revision으로 활성화한다.
  원래 Work ID, goal 슬롯과 두 조회의 revision을 함께 검증한다.

현재 원본의 normal, late, expiry 주문 target은 모두 30이다.
late/expiry의 실제 인도 보고가 20이다. 목표 수량을 20으로 조용히
고치지 않고 실제 원래 goal을 보존한다.

## 검증

Python 검사는 새 layer와 모든 이전 layer의 재현·복원, 최소 권한,
실제 convention binding, 원래 기간·수량·문장·assertion 보존을 검사한다.
HTTP/Spring/PostgreSQL은 원래 goal의 모든 key/value, 실제 definition
상태/hash/template/evaluator, 실제 active EVIDENCE policy hash를 먼저
대조하고 공개 명령을 실행한다. goal 슬롯 누락과 잘못된 selector,
reviseGoal/activateWork 권한 누락은 별도 반례로 둔다. 초기 잔액 행은
0으로 간주하지 않고 명령 전후 실제 원장을 대조한다.

`T17_PG_METHODS`는 빠른 진단을 위한 명시적인 method 선택이다.
선택하지 않은 검사는 SKIP이며 인수 PASS로 세지 않는다. 최종 source
suite는 이 변수를 빼고 root 소유 `run_source_pg_test.py`를 실행한다.
runner의 fresh package, 독립 DB와 임시 인증키 정리는 그대로다.

## 다음 제품 경계

`claim-falseclaim`은 새 준비를 통과한 뒤 `recordDelivery`에서 HTTP 500을
반환했다. 원래 보고의 `rangeRootId`는 존재하지 않는 root를 가리킨다.
`DeliveryCommands.java:19`는 원래 보고를 Observations에 그대로 넣고,
`database/migrations/V28__s4_cross_domain_links.sql:21`의 실제 segment FK가
commit 때 이를 거부한다. 이 오류를 피하려고 원래 claim의 root를 A로
바꾸거나 가짜 segment를 설치하지 않는다.

root가 해결할 범위는 **미확인 보고의 주장 ID와 검증된 재고 관계를
구별하는 observation 저장 계약**이다. 보고를 보존하면서도 실제 재고,
인도, canonical 인정으로 승격하지 않아야 한다. 기존 FK 전체를
완화하거나 허위 root에 물량을 만드는 해법은 이 Subtask의 산출물이
아니다. 해당 제품 코드·DB migration은 수정하지 않는다. 같은 경계를
지나는 나머지 원래 claim도 전체 PG 결과로 따로 기록한다.

## 이번 실행 결과

Java 21.0.12.1에서 해당 worktree의 `npm ci`와 root runner의 fresh
package를 사용했다. Python 31 PASS / 0 FAIL, source HTTP/Spring/PG
32 PASS / 2 FAIL / 0 SKIP이다. `./verify prepare`는 41 case, 802
subcase, 24,260 assertion과 문제 0개로 PREPARED다. full harness와
실제 Haiku는 이번 Subtask에서 NOT_RUN이며 전체 oracle PASS가 아니다.

정상 30은 물리 인도/인정 모두 30, late/expiry 20은 물리 인도 20과
인정 0으로 실행됐다. 부분 인도에는 RETAINED 10과 CUSTOMER 20,
TRANSIT 10을 실제 원장으로 대조했다. conflictingquantity/scopeoverflow
claim은 관측만 남기고 인도 0이다. falseclaim/unidentifiedscope는 위
제품 FK 오류로 실패했고 원본 입력과 기대값은 그대로다. no-prior,
다른 Work/source/scope, 중복, 누락 goal/grant의 기존 반례는 통과했다.

전체 raw 결과와 JUnit은
`.local/t17-source-pg-bcfd5bba-7247-428e-987a-947c9311e714/`에 있다.
이번 요약은 `evidence/delivery-goal-input-repair-summary.json`이다.
이전 실패 artifact를 보존했고 임시 JWT 키와 PostgreSQL container의
삭제를 확인했다. root의 제품 수정·통합·결합 검증 전까지 Task/Step는
OPEN이다.
