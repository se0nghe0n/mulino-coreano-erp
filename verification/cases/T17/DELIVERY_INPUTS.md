# T17 인도 Work와 공개 원천 연결

기준은 `bcac964ced0d5cf47a990afabba5eddefb66b2c8`이다.
전체 Task와 Step는 OPEN이다. 이 변경은 실제 Haiku 시나리오 실행이나
model-client 인수 결과가 아니다.

`author_delivery_inputs.py`가 기존 completion layer 다음에 적용된다.
`delivery-input-delta.json`은 원래 요청과 변경 후 요청, 새 준비행동,
원래 source actor 및 최소 권한 변경을 별도로 보존한다. 기존 public
15개 pointer ledger, first-failure/completion ledger와 원래 257개
assertion byte block을 바꾸지 않는다. 새 원문은 모두 synthetic이며
`originalAuthorityClaimed=false`, `operatingApprovalClaimed=false`다.

출고가 있는 7개 인도/claim 사례는 실제 주문의 Work를 `getWork`로 읽고
그 ID와 revision으로 `activateWork`를 요청한다. 이 준비는 원래
출고 다음에 둬 이미 검증된 출고 경로를 보존한다. `t17Source`에만
`activateWork`를 추가하며 기존 조직/ITEM all-of scope, grant 기간,
위임자와 사업 actor 권한은 그대로다. no-prior-dispatch 사례의
원래 Work와 요청은 바꾸지 않는다.

Dispatch와 SalesOrderLine은 `getObject`로 조회한 `data.ID`를 원문에
연결한다. 인도 원문에는 원래 evidence descriptor와 원래 요청을
그대로 남긴다. provisional 기록 다음의 DeliveryObservation도
`getObject`로 확인한 ID를 match에 전달한다. 이 ID는 실물 segment로
바꾸지 않는다. 현재 제품의 PHYSICAL_DELIVERY 대조 계약은 observation
ID를 받고 그 행에서 실제 dispatch의 Work/line/range를 검증한다.

원래 normal-consumed-delivery fixture의 인도는 30 BOX다. late recall과
post-dispatch-expiry의 인도는 20 BOX다. 이를 같은 수량으로 통일하지
않는다. 잘못된 source/범위/Work, 미활성 Work, 원래 no-dispatch claim,
중복 확정의 효과와 실제 movements/allocations/deliveries 행을 별도
HTTP/Spring/PostgreSQL 테스트에서 대조한다.

## 남은 준비 계약

현재 `createSalesOrder`가 생성하는 DRAFT goal은 `activateWork`의
complete 검증이 요구하는 timezone, evidencePolicyVersion, conditions,
evaluatorVersion, periodStart/End 및 포함 경계를 모두 제공하지 않는다.
새로운 기간/조건/정책을 임의로 선택하거나 사업 actor에 `reviseGoal`을
추가하지 않는다. 실제 실행에서 확인된 첫 실패와 해결 소유 범위는
[evidence/delivery-input-repair-summary.json](evidence/delivery-input-repair-summary.json)에
기록한다. root는 생성 goal의 완성 계약 또는 추가 공개 preparation의
입력/권한을 결정해야 한다. 자동 활성화로 제품 규칙을 바꾸지 않는다.

```sh
npm ci --prefix backend
python3 -m unittest discover -s verification/cases/T17 -p 'test_*.py'
python3 verification/cases/T17/run_source_pg_test.py
./mvnw -B -ntp -f verification/harness/pom.xml test
./verify prepare
```

root 소유 `run_source_pg_test.py`는 수정하지 않는다. 이 runner는 이
checkout을 fresh package하고 독립 DB를 사용한다. source test의
APPLIED나 부분 PASS를 원래 257개 oracle의 PASS로 보고하지 않는다.

## 실행 결과

최종 HTTP/Spring/PostgreSQL 검사는 32개 중 24 PASS, 8 FAIL,
0 ERROR, 0 SKIP이다. 실패 8개는 모두 실제 주문 Work 활성화의
`NEEDS_INPUT / GOAL_SLOT_MISSING: Missing goal slot: timezone`이다.
공개 `getWork`의 `currentGoal.slots`에는 timezone,
evidencePolicyVersion, conditions, evaluatorVersion, periodStart,
periodEnd, periodStartInclusive, periodEndInclusive가 없다.

원래 주문/출고 뒤 Work revision은 실제로 4였다. 고정 revision으로
실패를 덮지 않고 공개 조회의 revision으로 활성화를 호출했다.
권한을 제거한 사본의 FORBIDDEN, 미활성 Work의 provisional 거부,
원래 no-dispatch claim의 실물/인도 효과 0 및 기존 원문/범위 거부를
검증했다. 초기 INITIAL_BALANCE를 0행으로 가정하지 않고 거부 전후
동일성을 대조한다. 실패한 경우도 after-test SQL 원장을 보존한다.

Python 보존 검사는 28 PASS다. 전체 harness는 616 PASS와 45 SKIP
이며, 그 중 32개가 별도 source PG opt-in 검사다. prepare는
41 cases / 802 subcases / 24,260 assertions와 preparationProblems 0을
기록했다. source PG가 FAIL이므로 인도 원천 사슬의 전체 인수는
완료되지 않았다. 실제 Haiku Workflow와 model-client는 NOT_RUN이다.

상세 source/JAR hash, 명령과 로그, SQL 원장, 초기 탐색 실패 및
중단·환경 오류를 포함한 이력은
[evidence/delivery-input-repair-summary.json](evidence/delivery-input-repair-summary.json)에
남겼다. product/backend, observer, ReferenceResolver, 공통 installer와
root PG runner는 수정하지 않았다. 별도 eligibility/query/observation
공백도 이 변경의 범위에서 고치지 않았다.
