# language: ko
@T05 @D05 @sit @uat @contract-red
기능: 위치·보관·소유·처분 허용의 독립성

  시나리오: 위탁40과 고객 보관60의 보유·판매·처분을 구분한다
    먼저 사례 파일 "verification/cases/T05/case.json"의 "custody-not-sale"를 준비한다
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
    만일 "warehouse" 역할이 "alternate-action" 행동을 수행한다
    그러면 "held-1" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "held-2" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "sell-eligible-3" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "sell-eligible-4" assertion으로 "독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다."를 확인한다
    그러면 "unreserved-eligible-5" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "unreserved-eligible-6" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "relations-independent-7" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "relations-independent-8" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "relations-independent-9" assertion으로 "응답의 구조화 오류 코드 /response/error/code가 INSUFFICIENT_ELIGIBLE_QUANTITY다(계획 §3.4, contracts/command-response.schema.json)."를 확인한다
    그러면 "relations-independent-10" assertion으로 "명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다."를 확인한다
    그러면 "relations-independent-11" assertion으로 "명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다."를 확인한다
    그러면 "relations-independent-12" assertion으로 "명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다."를 확인한다
    그러면 "relations-independent-13" assertion으로 "허용된 denial 감사1과 금지된 업무 효과0을 분리한다."를 확인한다
    그러면 "relations-independent-14" assertion으로 "SELL 부적격이어도 반송 허용은 별도 행동의 처분 근거로 판단한다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다

  시나리오: 예정 목적지W는 실제 위치IT-port와 수령을 바꾸지 않는다
    먼저 사례 파일 "verification/cases/T05/case.json"의 "planned-not-actual"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before-api" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "recorder" 역할이 "shipment" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "current-location-1" assertion으로 "실제 마지막 확인 위치는 이탈리아 항구다."를 확인한다
    그러면 "current-location-2" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "planned-destination-3" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "planned-destination-4" assertion으로 "계획 목적지를 현재 장소와 따로 저장한다."를 확인한다
    그러면 "planned-receipt-effects-5" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "planned-receipt-effects-6" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "planned-receipt-effects-7" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다

  시나리오: 일반WRITE의 문서 접수와 MANAGER 처분 확인을 분리한다
    먼저 사례 파일 "verification/cases/T05/case.json"의 "manager-disposition"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before-api" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "ordinary" 역할이 "proposal" 행동을 수행한다
    만일 "ordinary" 역할이 "ordinary-confirm" 행동을 수행한다
    만일 "warehouse" 역할이 "unconfirmed-api" 행동을 수행한다
    만일 "시스템" 역할이 "unconfirmed-db" 행동을 수행한다
    만일 "manager" 역할이 "manager-confirm" 행동을 수행한다
    만일 "warehouse" 역할이 "confirmed-api" 행동을 수행한다
    만일 "시스템" 역할이 "confirmed-db" 행동을 수행한다
    만일 "ordinary" 역할이 "reserve" 행동을 수행한다
    만일 "warehouse" 역할이 "reserved-api" 행동을 수행한다
    만일 "시스템" 역할이 "reserved-db" 행동을 수행한다
    만일 "ordinary" 역할이 "dispatch" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    만일 "qc" 역할이 "independent-qc" 행동을 수행한다
    만일 "admin" 역할이 "independent-recall" 행동을 수행한다
    만일 "warehouse" 역할이 "qc-api" 행동을 수행한다
    만일 "시스템" 역할이 "qc-db" 행동을 수행한다
    그러면 "ordinary-write-confirmed-basis-effects-1" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "ordinary-write-confirmed-basis-effects-2" assertion으로 "응답의 구조화 오류 코드 /response/error/code가 FORBIDDEN다(계획 §3.4, contracts/command-response.schema.json)."를 확인한다
    그러면 "ordinary-write-confirmed-basis-effects-3" assertion으로 "명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다."를 확인한다
    그러면 "ordinary-write-confirmed-basis-effects-4" assertion으로 "허용된 denial 감사1과 금지된 업무 효과0을 분리한다."를 확인한다
    그러면 "eligible-before-manager-confirmation-5" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "eligible-before-manager-confirmation-6" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "proposal-evidence-preserved-7" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "proposal-evidence-preserved-8" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "proposal-evidence-preserved-9" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "authorized-confirmation-count-10" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "eligible-after-manager-confirmation-11" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "eligible-after-manager-confirmation-12" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "manager-decision-binding-13" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "ordinary-authorized-reservation-after-confirmation-14" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "ordinary-authorized-reservation-after-confirmation-15" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "ordinary-authorized-dispatch-after-confirmation-16" assertion으로 "독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다."를 확인한다
    그러면 "ordinary-authorized-dispatch-after-confirmation-17" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "new-human-approval-added-to-reservation-or-dispatch-18" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "new-human-approval-added-to-reservation-or-dispatch-19" assertion으로 "처분 확인 후 일반 reserve는 새 인간 승인 없이 실행한다."를 확인한다
    그러면 "manager-decision-binding-20" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "manager-decision-binding-21" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "manager-decision-binding-22" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다
