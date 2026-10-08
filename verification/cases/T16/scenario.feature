# language: ko
@T16 @D16 @sit @uat @contract-red
기능: 수령·QC·실사·무이벤트 만료

  시나리오: 임시수령60의 적격0과 QC·회수 제한의 독립 해제를 확인한다
    먼저 사례 파일 "verification/cases/T16/case.json"의 "provisional-holds"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before-api" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "recorder" 역할이 "provisional" 행동을 수행한다
    만일 "warehouse" 역할이 "provisional-api" 행동을 수행한다
    만일 "시스템" 역할이 "provisional-db" 행동을 수행한다
    만일 "recorder" 역할이 "confirm" 행동을 수행한다
    만일 "warehouse" 역할이 "confirmed-api" 행동을 수행한다
    만일 "시스템" 역할이 "confirmed-db" 행동을 수행한다
    만일 "qc" 역할이 "qc-hold" 행동을 수행한다
    만일 "admin" 역할이 "recall-hold" 행동을 수행한다
    만일 "warehouse" 역할이 "held-api" 행동을 수행한다
    만일 "시스템" 역할이 "held-db" 행동을 수행한다
    만일 "ordinary" 역할이 "ordinary-hold-proposal" 행동을 수행한다
    만일 "ordinary" 역할이 "ordinary-hold" 행동을 수행한다
    만일 "ordinary" 역할이 "ordinary-release" 행동을 수행한다
    만일 "warehouse" 역할이 "denied-api" 행동을 수행한다
    만일 "시스템" 역할이 "denied-db" 행동을 수행한다
    만일 "qc" 역할이 "qc-release" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "provisional-eligible-1" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "provisional-eligible-2" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "receipt-transit-double-creation-3" assertion으로 "독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다."를 확인한다
    그러면 "receipt-transit-double-creation-4" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "receipt-transit-double-creation-5" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "receipt-transit-double-creation-6" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "receipt-transit-double-creation-7" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "hold-authority-8" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "hold-authority-9" assertion으로 "응답의 구조화 오류 코드 /response/error/code가 FORBIDDEN다(계획 §3.4, contracts/command-response.schema.json)."를 확인한다
    그러면 "hold-authority-10" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "hold-authority-11" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "hold-authority-12" assertion으로 "허용된 denial 감사1과 금지된 업무 효과0을 분리한다."를 확인한다
    그러면 "eligible-after-QC-only-release-13" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "remaining-recall-14" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "hold-authority-15" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "ordinary-proposal-recorded" assertion으로 "일반 scoped RECORD는 QC 제안과 근거를 보존할 수 있다."를 확인한다
    그러면 "ordinary-cannot-place-qc-hold" assertion으로 "일반 Agent는 QC 제한의 실제 결정을 할 수 없다."를 확인한다
    그러면 "ordinary-qc-proposal-evidence" assertion으로 "QC 제안은 문서/사건으로 남지만 QC 결정으로 승격하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다

  시나리오: 실사98은 관측이며 MANAGER 조정2 뒤 내부20 이동에도 총98이다
    먼저 사례 파일 "verification/cases/T16/case.json"의 "stocktake-adjust-move"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before-api" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "recorder" 역할이 "stocktake" 행동을 수행한다
    만일 "warehouse" 역할이 "counted-api" 행동을 수행한다
    만일 "시스템" 역할이 "counted-db" 행동을 수행한다
    만일 "ordinary" 역할이 "ordinary-adjust" 행동을 수행한다
    만일 "warehouse" 역할이 "denied-api" 행동을 수행한다
    만일 "시스템" 역할이 "denied-db" 행동을 수행한다
    만일 "manager" 역할이 "adjust" 행동을 수행한다
    만일 "warehouse" 역할이 "adjusted-api" 행동을 수행한다
    만일 "시스템" 역할이 "adjusted-db" 행동을 수행한다
    만일 "warehouse" 역할이 "split" 행동을 수행한다
    만일 "warehouse" 역할이 "move" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "held-before-adjustment-1" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "held-before-adjustment-2" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "adjustment-proof-3" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "adjustment-proof-4" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "adjustment-proof-5" assertion으로 "응답의 구조화 오류 코드 /response/error/code가 FORBIDDEN다(계획 §3.4, contracts/command-response.schema.json)."를 확인한다
    그러면 "adjustment-proof-6" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "adjustment-proof-7" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "adjustment-proof-8" assertion으로 "허용된 denial 감사1과 금지된 업무 효과0을 분리한다."를 확인한다
    그러면 "held-after-adjustment-9" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "held-after-adjustment-10" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "adjustment-proof-11" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "distinct-total-after-internal-move-12" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "distinct-total-after-internal-move-13" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "adjustment-proof-14" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "adjustment-proof-16" assertion으로 "부분20 이동 전에 분할 movement가 원장 순서에 먼저 존재한다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다

  시나리오: 이벤트 없는 만료는 예약20과 인간 책임을 재평가한다
    먼저 사례 파일 "verification/cases/T16/case.json"의 "expiry-sweeper"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before-api" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "warehouse" 역할이 "reserved-api" 행동을 수행한다
    만일 "시스템" 역할이 "reserved-db" 행동을 수행한다
    만일 "시스템" 역할이 "advance" 행동을 수행한다
    만일 "시스템" 역할이 "sweep" 행동을 수행한다
    만일 "시스템" 역할이 "await-sweep" 행동을 수행한다
    만일 "warehouse" 역할이 "dispatch" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    만일 "sales" 역할이 "expiry-obligations" 행동을 수행한다
    그러면 "allocation-after-boundary-1" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "post-expiry-dispatched-2" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "post-expiry-dispatched-3" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "expiry-duty-4" assertion으로 "해당 obligation root/scope의 현재 유효 assignment는 하나다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다."를 확인한다
    그러면 "expiry-duty-5" assertion으로 "책임자의 실제 ID와 다음 행동·확인 시각에 공백이 없어야 한다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다."를 확인한다
    그러면 "expiry-duty-6" assertion으로 "fixture의 지정된 인간 owner/supervisor와 고정 후속 행동/시각이 유지된다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다."를 확인한다
    그러면 "boundary-recheck-7" assertion으로 "무이벤트 경계 이후30초 이내 실제 autonomous sweep task의 terminal을 관찰한다."를 확인한다
    그러면 "boundary-recheck-8" assertion으로 "이벤트 없는 만료 전에 예약의 nextValidityBoundary T를 실제 index 원 행으로 등록한다."를 확인한다
    그러면 "boundary-recheck-9" assertion으로 "sweep의 배분 정지와 의무 upsert는 같은 실제 transaction이다."를 확인한다
    그러면 "boundary-recheck-10" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다
    그러면 "actual-expiry-obligation-scope" assertion으로 "현재 allocation20의 만료 의무를 동일 DB snapshot에서 정확히 하나 조회한다."를 확인한다

  시나리오: 이벤트 없는 만료는 예약20과 인간 책임을 재평가한다
    먼저 사례 파일 "verification/cases/T16/case.json"의 "expiry-delayed-sweep"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before-api" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "warehouse" 역할이 "reserved-api" 행동을 수행한다
    만일 "시스템" 역할이 "reserved-db" 행동을 수행한다
    만일 "시스템" 역할이 "sweep-delay" 행동을 수행한다
    만일 "시스템" 역할이 "advance" 행동을 수행한다
    만일 "warehouse" 역할이 "dispatch" 행동을 수행한다
    만일 "warehouse" 역할이 "blocked-api" 행동을 수행한다
    만일 "시스템" 역할이 "blocked-db" 행동을 수행한다
    만일 "시스템" 역할이 "sweep-resume" 행동을 수행한다
    만일 "시스템" 역할이 "sweep" 행동을 수행한다
    만일 "시스템" 역할이 "await-sweep" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    만일 "sales" 역할이 "expiry-obligations" 행동을 수행한다
    그러면 "boundary-recheck-1" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "boundary-recheck-2" assertion으로 "응답의 구조화 오류 코드 /response/error/code가 INSUFFICIENT_ELIGIBLE_QUANTITY다(계획 §3.4, contracts/command-response.schema.json)."를 확인한다
    그러면 "post-expiry-dispatched-3" assertion으로 "명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다."를 확인한다
    그러면 "post-expiry-dispatched-4" assertion으로 "명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다."를 확인한다
    그러면 "allocation-after-boundary-5" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "post-expiry-dispatched-6" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "post-expiry-dispatched-7" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "expiry-duty-8" assertion으로 "해당 obligation root/scope의 현재 유효 assignment는 하나다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다."를 확인한다
    그러면 "expiry-duty-9" assertion으로 "책임자의 실제 ID와 다음 행동·확인 시각에 공백이 없어야 한다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다."를 확인한다
    그러면 "expiry-duty-10" assertion으로 "fixture의 지정된 인간 owner/supervisor와 고정 후속 행동/시각이 유지된다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다."를 확인한다
    그러면 "boundary-recheck-11" assertion으로 "무이벤트 경계 이후30초 이내 실제 autonomous sweep task의 terminal을 관찰한다."를 확인한다
    그러면 "boundary-recheck-12" assertion으로 "이벤트 없는 만료 전에 예약의 nextValidityBoundary T를 실제 index 원 행으로 등록한다."를 확인한다
    그러면 "boundary-recheck-13" assertion으로 "sweep의 배분 정지와 의무 upsert는 같은 실제 transaction이다."를 확인한다
    그러면 "boundary-recheck-14" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다
    그러면 "actual-expiry-obligation-scope" assertion으로 "현재 allocation20의 만료 의무를 동일 DB snapshot에서 정확히 하나 조회한다."를 확인한다
