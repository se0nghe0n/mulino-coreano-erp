# language: ko
@V3 @D17 @sit @uat @contract-red
기능: QC 제한과 출고의 직렬화

  시나리오: QC 보류와 출고 경합에서 hold commit이 먼저다
    먼저 사례 파일 "verification/cases/V3/case.json"의 "hold-first"를 준비한다
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
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "scope-lock-1" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "new-dispatched-2" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "new-dispatched-3" assertion으로 "검증 실패를 해당 오류 코드로 구별한다."를 확인한다
    그러면 "new-dispatched-4" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "new-dispatched-5" assertion으로 "출고 시도 뒤 실물 위치·수량은 경합 전과 같다"를 확인한다
    그러면 "new-dispatched-6" assertion으로 "QC 보류와 거부된 출고는 이동 원행을 바꾸지 않는다"를 확인한다
    그러면 "new-dispatched-7" assertion으로 "contender 출고 명령의 이동은 0건이다"를 확인한다
    그러면 "new-dispatched-8" assertion으로 "contender 출고 명령의 BUSINESS outbox는 0건이다"를 확인한다
    그러면 "allocation-9" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "blocked-dispatch-duty-10" assertion으로 "해당 obligation root/scope의 현재 유효 assignment는 하나다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다."를 확인한다
    그러면 "blocked-dispatch-duty-11" assertion으로 "책임자의 실제 ID와 다음 행동·확인 시각에 공백이 없어야 한다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다."를 확인한다
    그러면 "blocked-dispatch-duty-12" assertion으로 "fixture의 지정된 인간 owner/supervisor와 고정 후속 행동/시각이 유지된다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다."를 확인한다
    그러면 "scope-lock-13" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "scope-lock-14" assertion으로 "제한 삽입과 출고는 같은 실제 scope fence에 참여하고 현재 제한을 재읽는다."를 확인한다
    그러면 "scope-lock-16" assertion으로 "비동기 요청은 실제 terminal await가 있어야 끝난다."를 확인한다
    그러면 "race-contender-initial-scope" assertion으로 "contender는 경합 대상 실물을 실제로 먼저 읽었다"를 확인한다
    그러면 "race-contender-initial-eligible20" assertion으로 "contender 출고는 보류 전 적격20을 실제로 먼저 읽었다"를 확인한다
    그러면 "race-contender-waits-on-winner-lock" assertion으로 "독립 pg_catalog 관찰로 contender가 winner의 scope lock을 실제로 기다린다"를 확인한다
    그러면 "race-both-transactions-open-at-wait" assertion으로 "WAIT 시점 두 실제 DB transaction은 모두 OPEN이다"를 확인한다
    그러면 "race-distinct-db-transactions" assertion으로 "두 참가자는 서로 다른 실제 DB transaction이다"를 확인한다
    그러면 "race-winner-reached-after-lock" assertion으로 "winner는 scope lock 획득 뒤 commit 전에 멈췄다"를 확인한다
    그러면 "race-contender-revalidated-after-lock" assertion으로 "contender는 lock 뒤 현재 상태를 다시 읽고 결정했다"를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다
    그러면 "scope-lock-14-source-nonempty" assertion으로 "동일 transaction/revision/fence 비교의 구체 원 행은 실제 하나 있어야 한다."를 확인한다
    그러면 "scope-lock-14-baseline-nonempty" assertion으로 "동일 transaction/revision/fence 비교의 구체 원 행은 실제 하나 있어야 한다."를 확인한다

  시나리오: QC 보류와 출고 경합에서 dispatch commit이 먼저다
    먼저 사례 파일 "verification/cases/V3/case.json"의 "dispatch-first"를 준비한다
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
    만일 "시스템" 역할이 "race-settled-db" 행동을 수행한다
    만일 "qc" 역할이 "fresh-hold-target" 행동을 수행한다
    만일 "qc" 역할이 "fresh-hold" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "committed-dispatch-preserved-1" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "committed-dispatch-preserved-2" assertion으로 "독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다."를 확인한다
    그러면 "allocation-3" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "post-dispatch-hold-response-4" assertion으로 "해당 obligation root/scope의 현재 유효 assignment는 하나다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다."를 확인한다
    그러면 "post-dispatch-hold-response-5" assertion으로 "책임자의 실제 ID와 다음 행동·확인 시각에 공백이 없어야 한다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다."를 확인한다
    그러면 "post-dispatch-hold-response-6" assertion으로 "fixture의 지정된 인간 owner/supervisor와 고정 후속 행동/시각이 유지된다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다."를 확인한다
    그러면 "history-deletion-or-fake-rollback-7" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "post-dispatch-hold-response-8" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "history-deletion-or-fake-rollback-9" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "post-dispatch-hold-response-11" assertion으로 "비동기 요청은 실제 terminal await가 있어야 끝난다."를 확인한다
    그러면 "race-contender-initial-scope" assertion으로 "contender는 경합 대상 실물을 실제로 먼저 읽었다"를 확인한다
    그러면 "race-contender-initial-revision" assertion으로 "contender의 초기 read revision은 1이다"를 확인한다
    그러면 "race-contender-waits-on-winner-lock" assertion으로 "독립 pg_catalog 관찰로 contender가 winner의 scope lock을 실제로 기다린다"를 확인한다
    그러면 "race-both-transactions-open-at-wait" assertion으로 "WAIT 시점 두 실제 DB transaction은 모두 OPEN이다"를 확인한다
    그러면 "race-distinct-db-transactions" assertion으로 "두 참가자는 서로 다른 실제 DB transaction이다"를 확인한다
    그러면 "race-winner-reached-after-lock" assertion으로 "winner는 scope lock 획득 뒤 commit 전에 멈췄다"를 확인한다
    그러면 "race-contender-revalidated-after-lock" assertion으로 "contender는 lock 뒤 현재 상태를 다시 읽고 결정했다"를 확인한다
    그러면 "stale-hold-conflict" assertion으로 "출고 commit 뒤 옛 revision의 QC 보류는 CONFLICT다"를 확인한다
    그러면 "stale-hold-reason" assertion으로 "옛 revision QC 보류의 거부 이유는 STALE_REVISION이다"를 확인한다
    그러면 "stale-hold-no-restriction" assertion으로 "옛 revision QC 보류는 제한을 만들지 않는다"를 확인한다
    그러면 "fresh-hold-applied" assertion으로 "현재 revision으로 낸 새 QC 보류는 APPLIED다"를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다

  시나리오: 늦은 v1 해제는 새 v2 보류를 덮어쓰지 않는다
    먼저 사례 파일 "verification/cases/V3/case.json"의 "late-v1-release"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before-api" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "qc" 역할이 "new-hold" 행동을 수행한다
    만일 "warehouse" 역할이 "new-held-api" 행동을 수행한다
    만일 "시스템" 역할이 "new-held-db" 행동을 수행한다
    만일 "recorder" 역할이 "old-release" 행동을 수행한다
    만일 "qc" 역할이 "old-hold-target" 행동을 수행한다
    만일 "qc" 역할이 "apply-old-release" 행동을 수행한다
    만일 "warehouse" 역할이 "dispatch-target" 행동을 수행한다
    만일 "warehouse" 역할이 "dispatch" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "new-hold-1" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "newly-dispatched-2" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "newly-dispatched-3" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "late-release-overwrites-new-hold-4" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "apply-old-release-not-applied" assertion으로 "대체된 OLD_HOLD의 늦은 v1 해제 명령은 APPLIED가 아니다"를 확인한다
    그러면 "apply-old-release-not-external" assertion으로 "늦은 v1 해제는 ACCEPTED_PENDING_EXTERNAL도 아니다"를 확인한다
    그러면 "old-hold-not-released" assertion으로 "OLD_HOLD 원행은 RELEASED가 되지 않는다"를 확인한다
    그러면 "old-release-no-applied-audit" assertion으로 "늦은 해제 명령의 APPLIED 감사는 0건이다"를 확인한다
    그러면 "newly-dispatched-5" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "newly-dispatched-6" assertion으로 "검증 실패를 해당 오류 코드로 구별한다."를 확인한다
    그러면 "source-order-reconciliation-7" assertion으로 "해당 obligation root/scope의 현재 유효 assignment는 하나다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다."를 확인한다
    그러면 "source-order-reconciliation-8" assertion으로 "책임자의 실제 ID와 다음 행동·확인 시각에 공백이 없어야 한다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다."를 확인한다
    그러면 "source-order-reconciliation-9" assertion으로 "fixture의 지정된 인간 owner/supervisor와 고정 후속 행동/시각이 유지된다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다."를 확인한다
    그러면 "source-order-reconciliation-10" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다
