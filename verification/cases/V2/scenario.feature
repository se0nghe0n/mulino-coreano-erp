# language: ko
@V2 @D17 @sit @uat @contract-red
기능: 분할·배분 경합과 실제량 정정

  시나리오: 분할·신규 예약 경합에서 split 거래를 먼저 commit한다
    먼저 사례 파일 "verification/cases/V2/case.json"의 "split-commits-first"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before-api" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "시스템" 역할이 "contender" 행동을 수행한다
    만일 "시스템" 역할이 "reached" 행동을 수행한다
    만일 "시스템" 역할이 "winner" 행동을 수행한다
    만일 "시스템" 역할이 "winner-reached" 행동을 수행한다
    만일 "시스템" 역할이 "resume" 행동을 수행한다
    만일 "시스템" 역할이 "contender-waits" 행동을 수행한다
    만일 "시스템" 역할이 "winner-resume" 행동을 수행한다
    만일 "시스템" 역할이 "winner-terminal" 행동을 수행한다
    만일 "시스템" 역할이 "terminal" 행동을 수행한다
    만일 "warehouse" 역할이 "raced-api" 행동을 수행한다
    만일 "시스템" 역할이 "raced-db" 행동을 수행한다
    만일 "warehouse" 역할이 "split-api" 행동을 수행한다
    만일 "시스템" 역할이 "split-db" 행동을 수행한다
    만일 "warehouse" 역할이 "retired-parent" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "allocation-transfer-1" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "allocation-transfer-2" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "allocation-transfer-3" assertion으로 "검증 실패를 해당 오류 코드로 구별한다."를 확인한다
    그러면 "active-physical-4" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "active-physical-5" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "existing-obligation-6" assertion으로 "독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다."를 확인한다
    그러면 "new-demand-promise-20" assertion으로 "신규 주문 ORDER2의 약속20은 별도 root로 보존된다"를 확인한다
    그러면 "new-executable-reservation-7" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "new-executable-reservation-8" assertion으로 "독립 DB의 완전한 명령/행동 scope에서 해당 효과 원 행이0개다. 누락과 빈 결과를 혼동하지 않는다."를 확인한다
    그러면 "new-executable-reservation-api-exact" assertion으로 "API 신규 실행 예약은 DB 원 행과 정확히 같다"를 확인한다
    그러면 "all-executable-reservations-9" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "all-executable-reservations-10" assertion으로 "독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다."를 확인한다
    그러면 "all-executable-reservations-api-exact" assertion으로 "API 실행 예약 합계는 DB 원 행 합계와 정확히 같다"를 확인한다
    그러면 "retired-parent-reconsumption-11" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "retired-parent-reconsumption-12" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "retired-parent-reconsumption-13" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "retired-parent-reconsumption-14" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "retired-parent-reconsumption-15" assertion으로 "허용된 denial 감사1과 금지된 업무 효과0을 분리한다."를 확인한다
    그러면 "allocation-transfer-16" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "allocation-transfer-17" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "allocation-transfer-18" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "allocation-transfer-19" assertion으로 "실제 첫 read 뒤 두 번째 거래를 commit하기 전 barrier reached ACK다."를 확인한다
    그러면 "allocation-transfer-23" assertion으로 "제출 ACK가 아닌 terminal await를 확인한다."를 확인한다
    그러면 "race-contender-initial-scope" assertion으로 "contender는 경합 대상 실물을 실제로 먼저 읽었다"를 확인한다
    그러면 "race-contender-initial-revision" assertion으로 "contender의 초기 read revision은 1이다"를 확인한다
    그러면 "race-contender-waits-on-winner-lock" assertion으로 "독립 pg_catalog 관찰로 contender가 winner의 scope lock을 실제로 기다린다"를 확인한다
    그러면 "race-both-transactions-open-at-wait" assertion으로 "WAIT 시점 두 실제 DB transaction은 모두 OPEN이다"를 확인한다
    그러면 "race-distinct-db-transactions" assertion으로 "두 참가자는 서로 다른 실제 DB transaction이다"를 확인한다
    그러면 "race-winner-reached-after-lock" assertion으로 "winner는 scope lock 획득 뒤 commit 전에 멈췄다"를 확인한다
    그러면 "race-contender-revalidated-after-lock" assertion으로 "contender는 lock 뒤 현재 상태를 다시 읽고 결정했다"를 확인한다
    그러면 "race-split-lock-reread-revision2" assertion으로 "contender는 lock 뒤 revision2를 읽고 요청 revision1과 비교했다"를 확인한다
    그러면 "genuine-two-transactions" assertion으로 "독립 DB transaction 원 행이 contender와 winner 각 하나씩이다. command ID로 거래 증거를 대신하지 않는다."를 확인한다
    그러면 "genuine-distinct-transaction-identities" assertion으로 "서로 다른 실제 DB transaction ID가 중복되지 않는다."를 확인한다
    그러면 "genuine-two-capabilities" assertion으로 "실제 거래는 분할과 예약 각각이며 mock lock이나 제출 ACK만으로 대신하지 않는다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다

  시나리오: 분할·신규 예약 경합에서 reserve 거래를 먼저 commit한다
    먼저 사례 파일 "verification/cases/V2/case.json"의 "reserve-commits-first"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before-api" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "시스템" 역할이 "contender" 행동을 수행한다
    만일 "시스템" 역할이 "reached" 행동을 수행한다
    만일 "시스템" 역할이 "winner" 행동을 수행한다
    만일 "시스템" 역할이 "winner-reached" 행동을 수행한다
    만일 "시스템" 역할이 "resume" 행동을 수행한다
    만일 "시스템" 역할이 "contender-waits" 행동을 수행한다
    만일 "시스템" 역할이 "winner-resume" 행동을 수행한다
    만일 "시스템" 역할이 "winner-terminal" 행동을 수행한다
    만일 "시스템" 역할이 "terminal" 행동을 수행한다
    만일 "warehouse" 역할이 "raced-api" 행동을 수행한다
    만일 "시스템" 역할이 "raced-db" 행동을 수행한다
    만일 "warehouse" 역할이 "converge-target" 행동을 수행한다
    만일 "warehouse" 역할이 "explicit-fresh-split" 행동을 수행한다
    만일 "warehouse" 역할이 "split-api" 행동을 수행한다
    만일 "시스템" 역할이 "split-db" 행동을 수행한다
    만일 "warehouse" 역할이 "retired-target" 행동을 수행한다
    만일 "warehouse" 역할이 "retired-parent" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "allocation-transfer-1" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "active-physical-4" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "active-physical-5" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "existing-obligation-6" assertion으로 "독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다."를 확인한다
    그러면 "new-demand-promise-20" assertion으로 "신규 주문 ORDER2의 약속20은 별도 root로 보존된다"를 확인한다
    그러면 "new-executable-reservation-7" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "new-executable-reservation-8" assertion으로 "독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다."를 확인한다
    그러면 "new-executable-reservation-api-exact" assertion으로 "API 신규 실행 예약은 DB 원 행과 정확히 같다"를 확인한다
    그러면 "all-executable-reservations-9" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "all-executable-reservations-10" assertion으로 "독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다."를 확인한다
    그러면 "all-executable-reservations-api-exact" assertion으로 "API 실행 예약 합계는 DB 원 행 합계와 정확히 같다"를 확인한다
    그러면 "retired-parent-reconsumption-11" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "retired-parent-reconsumption-12" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "retired-parent-reconsumption-13" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "retired-parent-reconsumption-14" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "retired-parent-reconsumption-15" assertion으로 "허용된 denial 감사1과 금지된 업무 효과0을 분리한다."를 확인한다
    그러면 "allocation-transfer-16" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "allocation-transfer-18" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "allocation-transfer-19" assertion으로 "실제 첫 read 뒤 두 번째 거래를 commit하기 전 barrier reached ACK다."를 확인한다
    그러면 "allocation-transfer-23" assertion으로 "제출 ACK가 아닌 terminal await를 확인한다."를 확인한다
    그러면 "race-contender-initial-scope" assertion으로 "contender는 경합 대상 실물을 실제로 먼저 읽었다"를 확인한다
    그러면 "race-contender-initial-revision" assertion으로 "contender의 초기 read revision은 1이다"를 확인한다
    그러면 "race-contender-waits-on-winner-lock" assertion으로 "독립 pg_catalog 관찰로 contender가 winner의 scope lock을 실제로 기다린다"를 확인한다
    그러면 "race-both-transactions-open-at-wait" assertion으로 "WAIT 시점 두 실제 DB transaction은 모두 OPEN이다"를 확인한다
    그러면 "race-distinct-db-transactions" assertion으로 "두 참가자는 서로 다른 실제 DB transaction이다"를 확인한다
    그러면 "race-winner-reached-after-lock" assertion으로 "winner는 scope lock 획득 뒤 commit 전에 멈췄다"를 확인한다
    그러면 "genuine-two-transactions" assertion으로 "독립 DB transaction 원 행이 contender와 winner 각 하나씩이다. command ID로 거래 증거를 대신하지 않는다."를 확인한다
    그러면 "genuine-distinct-transaction-identities" assertion으로 "서로 다른 실제 DB transaction ID가 중복되지 않는다."를 확인한다
    그러면 "genuine-two-capabilities" assertion으로 "실제 거래는 분할과 예약 각각이며 mock lock이나 제출 ACK만으로 대신하지 않는다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다
    그러면 "contender-outcome-not-rejected" assertion으로 "contender 분할의 결과는 APPLIED 또는 CONFLICT 둘 중 하나다. command-response outcome enum에서 REJECTED를 제외한다. 두 결과 모두 아래 불변식과 기록 일치를 함께 만족해야 한다(계획 §4.2, §13.2 V2)."를 확인한다
    그러면 "contender-outcome-not-waiting-approval" assertion으로 "contender 분할의 결과는 APPLIED 또는 CONFLICT 둘 중 하나다. command-response outcome enum에서 WAITING_APPROVAL를 제외한다. 두 결과 모두 아래 불변식과 기록 일치를 함께 만족해야 한다(계획 §4.2, §13.2 V2)."를 확인한다
    그러면 "contender-outcome-not-needs-input" assertion으로 "contender 분할의 결과는 APPLIED 또는 CONFLICT 둘 중 하나다. command-response outcome enum에서 NEEDS_INPUT를 제외한다. 두 결과 모두 아래 불변식과 기록 일치를 함께 만족해야 한다(계획 §4.2, §13.2 V2)."를 확인한다
    그러면 "contender-outcome-not-accepted-pending-external" assertion으로 "contender 분할의 결과는 APPLIED 또는 CONFLICT 둘 중 하나다. command-response outcome enum에서 ACCEPTED_PENDING_EXTERNAL를 제외한다. 두 결과 모두 아래 불변식과 기록 일치를 함께 만족해야 한다(계획 §4.2, §13.2 V2)."를 확인한다
    그러면 "contender-outcome-not-pending-external" assertion으로 "contender 분할의 결과는 APPLIED 또는 CONFLICT 둘 중 하나다. command-response outcome enum에서 PENDING_EXTERNAL를 제외한다. 두 결과 모두 아래 불변식과 기록 일치를 함께 만족해야 한다(계획 §4.2, §13.2 V2)."를 확인한다
    그러면 "contender-outcome-not-held" assertion으로 "contender 분할의 결과는 APPLIED 또는 CONFLICT 둘 중 하나다. command-response outcome enum에서 HELD를 제외한다. 두 결과 모두 아래 불변식과 기록 일치를 함께 만족해야 한다(계획 §4.2, §13.2 V2)."를 확인한다
    그러면 "contender-reported-equals-recorded" assertion으로 "contender 응답 outcome은 같은 거래가 남긴 감사 원행의 outcome(raced-db derivation contenderOutcome)과 같다. 응답만 CONFLICT로 꾸미고 실제로 적용하거나 그 반대로 보고하면 실패한다."를 확인한다
    그러면 "contender-conflict-is-stale-revision" assertion으로 "contender가 CONFLICT면 감사 오류 코드는 STALE_REVISION이고 lock 뒤 재검증도 STALE_REVISION이다. APPLIED면 두 쪽 모두 빈 집합이다. 재검증이 STALE인데 적용하면(낡은 의도를 몰래 실행) 또는 재검증이 최신인데 충돌로 거부하면 실패한다(계획 §4.2)."를 확인한다
    그러면 "contender-conflict-iff-converge-split-applied" assertion으로 "contender가 CONFLICT(효과0)일 때만 뒤의 수렴 분할이 APPLIED다. contender가 APPLIED면 A60은 이미 retired라 수렴 분할은 적용되지 않는다. CONFLICT로 기록하고 분할 효과를 남기면 수렴 분할이 막혀 실패한다."를 확인한다
    그러면 "raced-physical-60" assertion으로 "경합 직후 active 실물 합은 두 결과 모두 60 BOX다(분할 전 A60 하나 또는 자식 40+20)."를 확인한다
    그러면 "raced-allocations-exactly-once" assertion으로 "경합 직후 실행 배분은 기존 ALLOC 40 BOX와 신규 예약 20 BOX가 각각 정확히 한 번이다. contender가 적용됐다면 둘 다 자식으로 한 번씩 이관됐고, 충돌이면 A60에 그대로 있다. 유실·중복은 실패한다."를 확인한다
    그러면 "race-contender-revalidated-after-lock" assertion으로 "contender는 lock을 얻은 뒤 A60을 다시 읽고 요청 revision1과 비교한다. 그 결과(STALE_REVISION 또는 최신)는 contender-conflict-is-stale-revision이 응답·감사와 묶는다. 재검증 trace는 실제 application transaction에서 만든다(계획 §4.2)."를 확인한다
    그러면 "final-children-40-20" assertion으로 "두 결과 모두 최종 active 실물은 분할 한 번의 자식 40 BOX와 20 BOX뿐이다. 분할이 두 번 적용되거나 부모가 남으면 실패한다."를 확인한다
    그러면 "final-parent-retired" assertion으로 "최종적으로 A60은 retired다."를 확인한다
    그러면 "final-no-allocation-on-retired-parent" assertion으로 "retired A60을 가리키는 active 배분은0이다. 분할은 기존·신규 배분을 모두 자식으로 옮긴다."를 확인한다
    그러면 "final-allocations-exactly-once" assertion으로 "최종 실행 배분도 ALLOC 40 BOX와 신규 예약 20 BOX 각각 한 번이다(합 60 = 실물 60)."를 확인한다

  시나리오: 실제50 정정은 약속60과 부족10 책임을 보존한다
    먼저 사례 파일 "verification/cases/V2/case.json"의 "actual50-correction"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before-api" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "recorder" 역할이 "correct" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "active-physical-1" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "active-physical-2" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "executable-allocation-3" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "executable-allocation-4" assertion으로 "독립 read-only DB SUM query는 원 행 scope의 실행 배분 상한50을 검증한다. product API projection을 복사하지 않는다."를 확인한다
    그러면 "promised-obligation-total-5" assertion으로 "독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다."를 확인한다
    그러면 "minimum-shortage-duty-6" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "minimum-shortage-duty-7" assertion으로 "독립 read-only DB SUM query는 원 행 scope의 부족 하한10을 검증한다. product API projection을 복사하지 않는다."를 확인한다
    그러면 "promise-coverage-conserved-60" assertion으로 "약속60은 실행 배분과 부족 의무로 정확히 한 번 덮인다"를 확인한다
    그러면 "promise-coverage-kinds" assertion으로 "약속 coverage 행은 종류·원천·수량·단위를 가진다"를 확인한다
    그러면 "promise-coverage-shortage-is-correction-duty" assertion으로 "부족 쪽 coverage는 정정이 만든 부족 의무 하나다"를 확인한다
    그러면 "shortage-duty-8" assertion으로 "해당 obligation root/scope의 현재 유효 assignment는 하나다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다."를 확인한다
    그러면 "shortage-duty-9" assertion으로 "책임자의 실제 ID와 다음 행동·확인 시각에 공백이 없어야 한다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다."를 확인한다
    그러면 "shortage-duty-10" assertion으로 "fixture의 지정된 인간 owner/supervisor와 고정 후속 행동/시각이 유지된다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다."를 확인한다
    그러면 "past-reservation-deletion-to-hide-shortage-12" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "past-reservation-deletion-to-hide-shortage-13" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다
