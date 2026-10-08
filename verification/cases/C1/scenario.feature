# language: ko
@C1 @D05 @sit @uat @contract-red
기능: 위탁과 고객 보관의 판매 범위

  시나리오: 위탁40과 고객 보관60의 보유·판매·처분을 구분한다
    먼저 사례 파일 "verification/cases/C1/case.json"의 "custody-not-sale"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before-api" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "warehouse" 역할이 "eligible" 행동을 수행한다
    만일 "recorder" 역할이 "custody" 행동을 수행한다
    만일 "qc" 역할이 "hold-customer" 행동을 수행한다
    만일 "qc" 역할이 "release-customer" 행동을 수행한다
    만일 "ordinary" 역할이 "app-write-reserve-customer" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "held-1" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "held-2" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "sell-eligible-3" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "sell-eligible-4" assertion으로 "독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다."를 확인한다
    그러면 "unreserved-eligible-5" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "unreserved-eligible-6" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "customer60-added-by-qc-or-app-write-7" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "customer60-added-by-qc-or-app-write-8" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "customer60-added-by-qc-or-app-write-9" assertion으로 "응답의 구조화 오류 코드 /response/error/code가 INSUFFICIENT_ELIGIBLE_QUANTITY다(계획 §3.4, contracts/command-response.schema.json)."를 확인한다
    그러면 "customer60-added-by-qc-or-app-write-10" assertion으로 "명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다."를 확인한다
    그러면 "customer60-added-by-qc-or-app-write-11" assertion으로 "명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다."를 확인한다
    그러면 "customer60-added-by-qc-or-app-write-12" assertion으로 "명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다."를 확인한다
    그러면 "customer60-added-by-qc-or-app-write-13" assertion으로 "허용된 denial 감사1과 금지된 업무 효과0을 분리한다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다

  시나리오: 위탁 허용 철회 뒤 예약20의 책임을 보존하고 새 효과를 막는다
    먼저 사례 파일 "verification/cases/C1/case.json"의 "revoked-basis"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before-api" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "manager" 역할이 "revoke" 행동을 수행한다
    만일 "warehouse" 역할이 "revoked-api" 행동을 수행한다
    만일 "시스템" 역할이 "revoked-db" 행동을 수행한다
    만일 "sales" 역할이 "new-reserve" 행동을 수행한다
    만일 "warehouse" 역할이 "dispatch" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "new-reservation-1" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "new-reservation-2" assertion으로 "응답의 구조화 오류 코드 /response/error/code가 INSUFFICIENT_ELIGIBLE_QUANTITY다(계획 §3.4, contracts/command-response.schema.json)."를 확인한다
    그러면 "new-reservation-3" assertion으로 "명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다."를 확인한다
    그러면 "new-dispatch-4" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "new-dispatch-5" assertion으로 "응답의 구조화 오류 코드 /response/error/code가 INSUFFICIENT_ELIGIBLE_QUANTITY다(계획 §3.4, contracts/command-response.schema.json)."를 확인한다
    그러면 "new-dispatch-6" assertion으로 "명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다."를 확인한다
    그러면 "new-dispatch-7" assertion으로 "명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다."를 확인한다
    그러면 "new-reservation-8" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "new-reservation-9" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "new-reservation-10" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "existing-allocation-11" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "existing-obligation-scope-12" assertion으로 "독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다."를 확인한다
    그러면 "revocation-duty-13" assertion으로 "해당 obligation root/scope의 현재 유효 assignment는 하나다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다."를 확인한다
    그러면 "revocation-duty-14" assertion으로 "책임자의 실제 ID와 다음 행동·확인 시각에 공백이 없어야 한다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다."를 확인한다
    그러면 "revocation-duty-15" assertion으로 "fixture의 지정된 인간 owner/supervisor와 고정 후속 행동/시각이 유지된다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다."를 확인한다
    그러면 "new-reservation-16" assertion으로 "허용된 denial 감사1과 금지된 업무 효과0을 분리한다."를 확인한다
    그러면 "new-dispatch-17" assertion으로 "허용된 denial 감사1과 금지된 업무 효과0을 분리한다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다
    그러면 "new-reservation-quantity-primary" assertion으로 "실제 독립DB의 해당 명령 효과 원행 quantity 합계는0이며, 같은 실물CON40 원행에서 관찰한 단위는BOX다. 완료된 빈 효과 scope만0으로 합산하며 누락/null/미구현은 거부한다."를 확인한다
    그러면 "new-dispatch-quantity-primary" assertion으로 "실제 독립DB의 해당 명령 효과 원행 quantity 합계는0이며, 같은 실물CON40 원행에서 관찰한 단위는BOX다. 완료된 빈 효과 scope만0으로 합산하며 누락/null/미구현은 거부한다."를 확인한다
