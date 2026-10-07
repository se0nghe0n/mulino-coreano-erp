# language: ko
@T04 @D04 @sit @uat @contract-red
기능: decimal과 수량 거래의 불변식

  시나리오: decimal 경계 indivisible-ea를 반올림 없이 거부한다
    먼저 사례 파일 "verification/cases/T04/case.json"의 "indivisible-ea"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before-api" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "warehouse" 역할이 "invalid" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    만일 "warehouse" 역할이 "money" 행동을 수행한다
    그러면 "invalid-decimals-1" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "invalid-decimals-2" assertion으로 "검증 실패를 해당 오류 코드로 구별한다."를 확인한다
    그러면 "rounded-effects-3" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rounded-effects-4" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rounded-effects-5" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rounded-effects-6" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rounded-effects-7" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rounded-effects-8" assertion으로 "허용된 denial 감사1과 금지된 업무 효과0을 분리한다."를 확인한다
    그러면 "wire-decimals-9" assertion으로 "유효하지 않은 원 decimal 문자열을 그대로 반환하며 binary float/반올림으로 바꾸지 않는다."를 확인한다
    그러면 "wire-decimals-10" assertion으로 "원 입력의 실제 단위를 그대로 대조한다."를 확인한다
    그러면 "wire-decimals-11" assertion으로 "금액 역시 정확한 decimal 문자열을 사용한다."를 확인한다
    그러면 "wire-decimals-12" assertion으로 "금액과 원 통화를 함께 보존한다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다

  시나리오: decimal 경계 scale-overflow를 반올림 없이 거부한다
    먼저 사례 파일 "verification/cases/T04/case.json"의 "scale-overflow"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before-api" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "warehouse" 역할이 "invalid" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    만일 "warehouse" 역할이 "money" 행동을 수행한다
    그러면 "invalid-decimals-1" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "invalid-decimals-2" assertion으로 "검증 실패를 해당 오류 코드로 구별한다."를 확인한다
    그러면 "rounded-effects-3" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rounded-effects-4" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rounded-effects-5" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rounded-effects-6" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rounded-effects-7" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rounded-effects-8" assertion으로 "허용된 denial 감사1과 금지된 업무 효과0을 분리한다."를 확인한다
    그러면 "wire-decimals-9" assertion으로 "유효하지 않은 원 decimal 문자열을 그대로 반환하며 binary float/반올림으로 바꾸지 않는다."를 확인한다
    그러면 "wire-decimals-10" assertion으로 "원 입력의 실제 단위를 그대로 대조한다."를 확인한다
    그러면 "wire-decimals-11" assertion으로 "금액 역시 정확한 decimal 문자열을 사용한다."를 확인한다
    그러면 "wire-decimals-12" assertion으로 "금액과 원 통화를 함께 보존한다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다

  시나리오: decimal 경계 precision-overflow를 반올림 없이 거부한다
    먼저 사례 파일 "verification/cases/T04/case.json"의 "precision-overflow"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before-api" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "warehouse" 역할이 "invalid" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    만일 "warehouse" 역할이 "money" 행동을 수행한다
    그러면 "invalid-decimals-1" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "invalid-decimals-2" assertion으로 "검증 실패를 해당 오류 코드로 구별한다."를 확인한다
    그러면 "rounded-effects-3" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rounded-effects-4" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rounded-effects-5" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rounded-effects-6" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rounded-effects-7" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rounded-effects-8" assertion으로 "허용된 denial 감사1과 금지된 업무 효과0을 분리한다."를 확인한다
    그러면 "wire-decimals-9" assertion으로 "유효하지 않은 원 decimal 문자열을 그대로 반환하며 binary float/반올림으로 바꾸지 않는다."를 확인한다
    그러면 "wire-decimals-10" assertion으로 "원 입력의 실제 단위를 그대로 대조한다."를 확인한다
    그러면 "wire-decimals-11" assertion으로 "금액 역시 정확한 decimal 문자열을 사용한다."를 확인한다
    그러면 "wire-decimals-12" assertion으로 "금액과 원 통화를 함께 보존한다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다

  시나리오: 파손 보류10은 보유100을 유지하고 승인 폐기10만90으로 줄인다
    먼저 사례 파일 "verification/cases/T04/case.json"의 "hold-dispose"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before-api" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "qc" 역할이 "hold" 행동을 수행한다
    만일 "warehouse" 역할이 "held-api" 행동을 수행한다
    만일 "시스템" 역할이 "held-db" 행동을 수행한다
    만일 "ordinary" 역할이 "ordinary-disposal" 행동을 수행한다
    만일 "warehouse" 역할이 "denied-api" 행동을 수행한다
    만일 "시스템" 역할이 "denied-db" 행동을 수행한다
    만일 "manager" 역할이 "dispose" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "held-after-hold-1" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "held-after-hold-2" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "eligible-after-hold-3" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "eligible-after-hold-4" assertion으로 "독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다."를 확인한다
    그러면 "decrease-evidence-5" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "decrease-evidence-6" assertion으로 "검증 실패를 해당 오류 코드로 구별한다."를 확인한다
    그러면 "decrease-evidence-7" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "decrease-evidence-8" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "decrease-evidence-9" assertion으로 "허용된 denial 감사1과 금지된 업무 효과0을 분리한다."를 확인한다
    그러면 "held-after-disposal-10" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "held-after-disposal-11" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "physical-disposal-12" assertion으로 "독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다."를 확인한다
    그러면 "decrease-evidence-13" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "decrease-evidence-14" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "hold-physical-delta" assertion으로 "독립 손계산의 실물 delta 0 BOX를 전후 실제 단위와 함께 대조한다."를 확인한다
    그러면 "disposal-physical-delta" assertion으로 "독립 손계산의 실물 delta -10 BOX를 전후 실제 단위와 함께 대조한다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다

  시나리오: 수량 거래의 afterMovementBeforeAllocation 장애는 전체 rollback한다
    먼저 사례 파일 "verification/cases/T04/case.json"의 "rollback-afterMovementBeforeAllocation"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before-api" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "시스템" 역할이 "fault" 행동을 수행한다
    만일 "warehouse" 역할이 "dispatch" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "rollback-committed-result-1" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "rollback-committed-result-2" assertion으로 "검증 실패를 해당 오류 코드로 구별한다."를 확인한다
    그러면 "rollback-ledger-effects-3" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rollback-ledger-effects-4" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rollback-ledger-effects-5" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rollback-allocation-effects-6" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rollback-outbox-effects-7" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rollback-committed-result-8" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rollback-ledger-effects-9" assertion으로 "도메인 성공 감사도 rollback이며 별도 실패 접수 audit만 허용한다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다

  시나리오: 수량 거래의 afterAllocationBeforeAudit 장애는 전체 rollback한다
    먼저 사례 파일 "verification/cases/T04/case.json"의 "rollback-afterAllocationBeforeAudit"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before-api" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "시스템" 역할이 "fault" 행동을 수행한다
    만일 "warehouse" 역할이 "dispatch" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "rollback-committed-result-1" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "rollback-committed-result-2" assertion으로 "검증 실패를 해당 오류 코드로 구별한다."를 확인한다
    그러면 "rollback-ledger-effects-3" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rollback-ledger-effects-4" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rollback-ledger-effects-5" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rollback-allocation-effects-6" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rollback-outbox-effects-7" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rollback-committed-result-8" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rollback-ledger-effects-9" assertion으로 "도메인 성공 감사도 rollback이며 별도 실패 접수 audit만 허용한다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다

  시나리오: 수량 거래의 afterAuditBeforeOutbox 장애는 전체 rollback한다
    먼저 사례 파일 "verification/cases/T04/case.json"의 "rollback-afterAuditBeforeOutbox"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before-api" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "시스템" 역할이 "fault" 행동을 수행한다
    만일 "warehouse" 역할이 "dispatch" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "rollback-committed-result-1" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "rollback-committed-result-2" assertion으로 "검증 실패를 해당 오류 코드로 구별한다."를 확인한다
    그러면 "rollback-ledger-effects-3" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rollback-ledger-effects-4" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rollback-ledger-effects-5" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rollback-allocation-effects-6" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rollback-outbox-effects-7" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rollback-committed-result-8" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rollback-ledger-effects-9" assertion으로 "도메인 성공 감사도 rollback이며 별도 실패 접수 audit만 허용한다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다

  시나리오: 수량 거래의 afterOutboxBeforeCommittedResult 장애는 전체 rollback한다
    먼저 사례 파일 "verification/cases/T04/case.json"의 "rollback-afterOutboxBeforeCommittedResult"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before-api" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "시스템" 역할이 "fault" 행동을 수행한다
    만일 "warehouse" 역할이 "dispatch" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "rollback-committed-result-1" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "rollback-committed-result-2" assertion으로 "검증 실패를 해당 오류 코드로 구별한다."를 확인한다
    그러면 "rollback-ledger-effects-3" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rollback-ledger-effects-4" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rollback-ledger-effects-5" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rollback-allocation-effects-6" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rollback-outbox-effects-7" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rollback-committed-result-8" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "rollback-ledger-effects-9" assertion으로 "도메인 성공 감사도 rollback이며 별도 실패 접수 audit만 허용한다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다

  시나리오: raw 원장 쓰기와 stale revision 재해석을 막는다
    먼저 사례 파일 "verification/cases/T04/case.json"의 "primitive-guards"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before-api" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "warehouse" 역할이 "raw-api" 행동을 수행한다
    만일 "warehouse" 역할이 "raw-db" 행동을 수행한다
    만일 "warehouse" 역할이 "stale" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    만일 "warehouse" 역할이 "legitimate-split" 행동을 수행한다
    만일 "warehouse" 역할이 "locked-api" 행동을 수행한다
    만일 "시스템" 역할이 "locked-db" 행동을 수행한다
    그러면 "guarded-primitives-1" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "guarded-primitives-2" assertion으로 "검증 실패를 해당 오류 코드로 구별한다."를 확인한다
    그러면 "guarded-primitives-3" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "guarded-primitives-4" assertion으로 "검증 실패를 해당 오류 코드로 구별한다."를 확인한다
    그러면 "guarded-primitives-5" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "guarded-primitives-6" assertion으로 "검증 실패를 해당 오류 코드로 구별한다."를 확인한다
    그러면 "guarded-primitives-7" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "guarded-primitives-8" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "guarded-primitives-9" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "guarded-primitives-10" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "guarded-primitives-11" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "guarded-primitives-12" assertion으로 "독립 DB role privilege 조회에서 core UPDATE 허용 행이 없다."를 확인한다
    그러면 "guarded-primitives-13" assertion으로 "충돌 후 payload/expected revision을 몰래 최신 의도로 바꾸지 않는다. bounded retry 한계3 안에서 종료한다."를 확인한다
    그러면 "guarded-primitives-14" assertion으로 "같은 scope의 실제 잠금 획득은 고정 ID 순서다."를 확인한다
    그러면 "guarded-primitives-16" assertion으로 "lock 획득 후 실제 revision reread로 commit guard를 확인한다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다
    그러면 "guarded-primitives-16-source-nonempty" assertion으로 "동일 transaction/revision/fence 비교의 구체 원 행은 실제 하나 있어야 한다."를 확인한다
    그러면 "guarded-primitives-16-baseline-nonempty" assertion으로 "동일 transaction/revision/fence 비교의 구체 원 행은 실제 하나 있어야 한다."를 확인한다
