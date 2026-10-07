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
    만일 "qc" 역할이 "winner" 행동을 수행한다
    만일 "warehouse" 역할이 "winner-committed-api" 행동을 수행한다
    만일 "시스템" 역할이 "winner-committed-db" 행동을 수행한다
    만일 "시스템" 역할이 "resume" 행동을 수행한다
    만일 "시스템" 역할이 "terminal" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "scope-lock-1" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "new-dispatched-2" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "new-dispatched-3" assertion으로 "검증 실패를 해당 오류 코드로 구별한다."를 확인한다
    그러면 "new-dispatched-4" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "new-dispatched-5" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "new-dispatched-6" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "new-dispatched-7" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "new-dispatched-8" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "allocation-9" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "blocked-dispatch-duty-10" assertion으로 "해당 obligation root/scope의 현재 유효 assignment는 하나다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다."를 확인한다
    그러면 "blocked-dispatch-duty-11" assertion으로 "책임자의 실제 ID와 다음 행동·확인 시각에 공백이 없어야 한다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다."를 확인한다
    그러면 "blocked-dispatch-duty-12" assertion으로 "fixture의 지정된 인간 owner/supervisor와 고정 후속 행동/시각이 유지된다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다."를 확인한다
    그러면 "scope-lock-13" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "scope-lock-14" assertion으로 "제한 삽입과 출고는 같은 실제 scope fence에 참여하고 현재 제한을 재읽는다."를 확인한다
    그러면 "scope-lock-15" assertion으로 "barrier reached와 terminal은 동일 contender transaction의 실제 증거다."를 확인한다
    그러면 "scope-lock-16" assertion으로 "비동기 요청은 실제 terminal await가 있어야 끝난다."를 확인한다
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
    만일 "warehouse" 역할이 "winner" 행동을 수행한다
    만일 "warehouse" 역할이 "winner-committed-api" 행동을 수행한다
    만일 "시스템" 역할이 "winner-committed-db" 행동을 수행한다
    만일 "시스템" 역할이 "resume" 행동을 수행한다
    만일 "시스템" 역할이 "terminal" 행동을 수행한다
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
    그러면 "post-dispatch-hold-response-10" assertion으로 "barrier reached와 terminal은 동일 contender transaction의 실제 증거다."를 확인한다
    그러면 "post-dispatch-hold-response-11" assertion으로 "비동기 요청은 실제 terminal await가 있어야 끝난다."를 확인한다
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
    만일 "qc" 역할이 "apply-old-release" 행동을 수행한다
    만일 "warehouse" 역할이 "dispatch" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "new-hold-1" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "newly-dispatched-2" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "newly-dispatched-3" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "late-release-overwrites-new-hold-4" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "newly-dispatched-5" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "newly-dispatched-6" assertion으로 "검증 실패를 해당 오류 코드로 구별한다."를 확인한다
    그러면 "source-order-reconciliation-7" assertion으로 "해당 obligation root/scope의 현재 유효 assignment는 하나다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다."를 확인한다
    그러면 "source-order-reconciliation-8" assertion으로 "책임자의 실제 ID와 다음 행동·확인 시각에 공백이 없어야 한다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다."를 확인한다
    그러면 "source-order-reconciliation-9" assertion으로 "fixture의 지정된 인간 owner/supervisor와 고정 후속 행동/시각이 유지된다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다."를 확인한다
    그러면 "source-order-reconciliation-10" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다
