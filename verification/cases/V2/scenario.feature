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
    만일 "warehouse" 역할이 "winner" 행동을 수행한다
    만일 "시스템" 역할이 "resume" 행동을 수행한다
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
    그러면 "new-executable-reservation-7" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "new-executable-reservation-8" assertion으로 "독립 DB의 완전한 명령/행동 scope에서 해당 효과 원 행이0개다. 누락과 빈 결과를 혼동하지 않는다."를 확인한다
    그러면 "all-executable-reservations-9" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "all-executable-reservations-10" assertion으로 "독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다."를 확인한다
    그러면 "retired-parent-reconsumption-11" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "retired-parent-reconsumption-12" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "retired-parent-reconsumption-13" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "retired-parent-reconsumption-14" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "retired-parent-reconsumption-15" assertion으로 "허용된 denial 감사1과 금지된 업무 효과0을 분리한다."를 확인한다
    그러면 "allocation-transfer-16" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "allocation-transfer-17" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "allocation-transfer-18" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "allocation-transfer-19" assertion으로 "실제 첫 read 뒤 두 번째 거래를 commit하기 전 barrier reached ACK다."를 확인한다
    그러면 "allocation-transfer-22" assertion으로 "barrier와 terminal await는 같은 contender transaction을 관찰한다."를 확인한다
    그러면 "allocation-transfer-23" assertion으로 "제출 ACK가 아닌 terminal await를 확인한다."를 확인한다
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
    만일 "sales" 역할이 "winner" 행동을 수행한다
    만일 "시스템" 역할이 "resume" 행동을 수행한다
    만일 "시스템" 역할이 "terminal" 행동을 수행한다
    만일 "warehouse" 역할이 "raced-api" 행동을 수행한다
    만일 "시스템" 역할이 "raced-db" 행동을 수행한다
    만일 "warehouse" 역할이 "explicit-fresh-split" 행동을 수행한다
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
    그러면 "new-executable-reservation-7" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "new-executable-reservation-8" assertion으로 "독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다."를 확인한다
    그러면 "all-executable-reservations-9" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "all-executable-reservations-10" assertion으로 "독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다."를 확인한다
    그러면 "retired-parent-reconsumption-11" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "retired-parent-reconsumption-12" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "retired-parent-reconsumption-13" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "retired-parent-reconsumption-14" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "retired-parent-reconsumption-15" assertion으로 "허용된 denial 감사1과 금지된 업무 효과0을 분리한다."를 확인한다
    그러면 "allocation-transfer-16" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "allocation-transfer-17" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "allocation-transfer-18" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "allocation-transfer-19" assertion으로 "실제 첫 read 뒤 두 번째 거래를 commit하기 전 barrier reached ACK다."를 확인한다
    그러면 "allocation-transfer-22" assertion으로 "barrier와 terminal await는 같은 contender transaction을 관찰한다."를 확인한다
    그러면 "allocation-transfer-23" assertion으로 "제출 ACK가 아닌 terminal await를 확인한다."를 확인한다
    그러면 "genuine-two-transactions" assertion으로 "독립 DB transaction 원 행이 contender와 winner 각 하나씩이다. command ID로 거래 증거를 대신하지 않는다."를 확인한다
    그러면 "genuine-distinct-transaction-identities" assertion으로 "서로 다른 실제 DB transaction ID가 중복되지 않는다."를 확인한다
    그러면 "genuine-two-capabilities" assertion으로 "실제 거래는 분할과 예약 각각이며 mock lock이나 제출 ACK만으로 대신하지 않는다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다

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
    그러면 "shortage-duty-8" assertion으로 "해당 obligation root/scope의 현재 유효 assignment는 하나다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다."를 확인한다
    그러면 "shortage-duty-9" assertion으로 "책임자의 실제 ID와 다음 행동·확인 시각에 공백이 없어야 한다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다."를 확인한다
    그러면 "shortage-duty-10" assertion으로 "fixture의 지정된 인간 owner/supervisor와 고정 후속 행동/시각이 유지된다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다."를 확인한다
    그러면 "past-reservation-deletion-to-hide-shortage-12" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "past-reservation-deletion-to-hide-shortage-13" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다
