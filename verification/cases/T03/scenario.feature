# language: ko
@T03 @D03 @sit @uat @contract-red
기능: 실물 계보와 분할·합침의 보존

  시나리오: 분할과 합침은 실물100과 기존 배분40을 한 번씩 보존한다
    먼저 사례 파일 "verification/cases/T03/case.json"의 "split-merge"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before-api" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "warehouse" 역할이 "split" 행동을 수행한다
    만일 "warehouse" 역할이 "split-api" 행동을 수행한다
    만일 "시스템" 역할이 "split-db" 행동을 수행한다
    만일 "warehouse" 역할이 "merge" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "active-quantity-1" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "active-quantity-2" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "parent-3" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "allocation-transferred-4" assertion으로 "독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다."를 확인한다
    그러면 "allocation-copies-5" assertion으로 "기존 배분40의 활성 root copy가 정확히 하나여야 한다."를 확인한다
    그러면 "atomic-genealogy-6" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "atomic-genealogy-8" assertion으로 "분할의 계보·배분·감사·멱등 결과는 같은 실제 DB transaction에 속한다."를 확인한다
    그러면 "atomic-genealogy-9" assertion으로 "분할의 계보·배분·감사·멱등 결과는 같은 실제 DB transaction에 속한다."를 확인한다
    그러면 "atomic-genealogy-10" assertion으로 "분할의 계보·배분·감사·멱등 결과는 같은 실제 DB transaction에 속한다."를 확인한다
    그러면 "atomic-genealogy-11" assertion으로 "분할의 계보·배분·감사·멱등 결과는 같은 실제 DB transaction에 속한다."를 확인한다
    그러면 "active-quantity-12" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "active-quantity-13" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "allocation-transferred-14" assertion으로 "독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다."를 확인한다
    그러면 "allocation-copies-15" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "atomic-genealogy-16" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "atomic-genealogy-17" assertion으로 "두 분할 edge와 두 합침 edge의 원천 identity를 잃지 않는다."를 확인한다
    그러면 "atomic-second-genealogy-transaction" assertion으로 "분할의 계보·배분·감사·멱등 결과는 같은 실제 DB transaction에 속한다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "complete-source-genealogy" assertion으로 "부모→자식과 자식→합침의 모든 원천 ID·quantity·unit을 정확한 관계 tuple로 보존한다."를 확인한다
    그러면 "retired-parent-physical-consumption" assertion으로 "retired parent 원량은 물리 소비가 아닌 계보 분할이므로 재소비 원행은0이다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다
    그러면 "atomic-genealogy-8-source-nonempty" assertion으로 "동일 transaction/revision/fence 비교의 구체 원 행은 실제 하나 있어야 한다."를 확인한다
    그러면 "atomic-genealogy-8-baseline-nonempty" assertion으로 "동일 transaction/revision/fence 비교의 구체 원 행은 실제 하나 있어야 한다."를 확인한다
    그러면 "atomic-genealogy-9-source-nonempty" assertion으로 "동일 transaction/revision/fence 비교의 구체 원 행은 실제 하나 있어야 한다."를 확인한다
    그러면 "atomic-genealogy-9-baseline-nonempty" assertion으로 "동일 transaction/revision/fence 비교의 구체 원 행은 실제 하나 있어야 한다."를 확인한다
    그러면 "atomic-genealogy-10-source-nonempty" assertion으로 "동일 transaction/revision/fence 비교의 구체 원 행은 실제 하나 있어야 한다."를 확인한다
    그러면 "atomic-genealogy-10-baseline-nonempty" assertion으로 "동일 transaction/revision/fence 비교의 구체 원 행은 실제 하나 있어야 한다."를 확인한다
    그러면 "atomic-genealogy-11-source-nonempty" assertion으로 "동일 transaction/revision/fence 비교의 구체 원 행은 실제 하나 있어야 한다."를 확인한다
    그러면 "atomic-genealogy-11-baseline-nonempty" assertion으로 "동일 transaction/revision/fence 비교의 구체 원 행은 실제 하나 있어야 한다."를 확인한다
    그러면 "atomic-second-genealogy-transaction-source-nonempty" assertion으로 "동일 transaction/revision/fence 비교의 구체 원 행은 실제 하나 있어야 한다."를 확인한다
    그러면 "atomic-second-genealogy-transaction-baseline-nonempty" assertion으로 "동일 transaction/revision/fence 비교의 구체 원 행은 실제 하나 있어야 한다."를 확인한다

  시나리오: 여러 LOT를 담은 팔레트는 LOT를 병합하지 않는다
    먼저 사례 파일 "verification/cases/T03/case.json"의 "pallet-two-lots"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before-api" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "warehouse" 역할이 "pallet" 행동을 수행한다
    만일 "warehouse" 역할이 "cross-lot-merge" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "pallet-quantity-1" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "pallet-quantity-2" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "lot-membership-3" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "lot-membership-4" assertion으로 "실제 팔레트 조회도 서로 다른 제조 LOT 두 개를 보존한다."를 확인한다
    그러면 "cross-lot-segment-merge-5" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "cross-lot-segment-merge-6" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "cross-lot-segment-merge-7" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "cross-lot-segment-merge-8" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "cross-lot-segment-merge-9" assertion으로 "허용된 denial 감사1과 금지된 업무 효과0을 분리한다."를 확인한다
    그러면 "membership-time-10" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다

  시나리오: 잘못된 계보·합침 cycle를 거부한다
    먼저 사례 파일 "verification/cases/T03/case.json"의 "reject-cycle"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before-api" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "warehouse" 역할이 "legitimate-split" 행동을 수행한다
    만일 "warehouse" 역할이 "pre-invalid-api" 행동을 수행한다
    만일 "시스템" 역할이 "pre-invalid-db" 행동을 수행한다
    만일 "warehouse" 역할이 "invalid" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "invalid-results-1" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "invalid-quantity-effects-2" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "invalid-quantity-effects-3" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "invalid-quantity-effects-4" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "invalid-allocation-effects-5" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "invalid-results-6" assertion으로 "명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다."를 확인한다
    그러면 "invalid-results-7" assertion으로 "명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다."를 확인한다
    그러면 "invalid-results-8" assertion으로 "허용된 denial 감사1과 금지된 업무 효과0을 분리한다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "cycle-specific-error" assertion으로 "active 자식에서 ancestor 부모로 되돌리는 edge는 계보 순환 오류로 거부한다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다

  시나리오: 잘못된 계보·합침 consumeRetiredParent를 거부한다
    먼저 사례 파일 "verification/cases/T03/case.json"의 "reject-consumeRetiredParent"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before-api" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "warehouse" 역할이 "legitimate-split" 행동을 수행한다
    만일 "warehouse" 역할이 "pre-invalid-api" 행동을 수행한다
    만일 "시스템" 역할이 "pre-invalid-db" 행동을 수행한다
    만일 "warehouse" 역할이 "invalid" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "invalid-results-1" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "invalid-quantity-effects-2" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "invalid-quantity-effects-3" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "invalid-quantity-effects-4" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "invalid-allocation-effects-5" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "invalid-results-6" assertion으로 "명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다."를 확인한다
    그러면 "invalid-results-7" assertion으로 "명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다."를 확인한다
    그러면 "invalid-results-8" assertion으로 "허용된 denial 감사1과 금지된 업무 효과0을 분리한다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다

  시나리오: 잘못된 계보·합침 mergeDifferentPlace를 거부한다
    먼저 사례 파일 "verification/cases/T03/case.json"의 "reject-mergeDifferentPlace"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before-api" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "warehouse" 역할이 "invalid" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "invalid-results-1" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "invalid-quantity-effects-2" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "invalid-quantity-effects-3" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "invalid-quantity-effects-4" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "invalid-allocation-effects-5" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "invalid-results-6" assertion으로 "명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다."를 확인한다
    그러면 "invalid-results-7" assertion으로 "명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다."를 확인한다
    그러면 "invalid-results-8" assertion으로 "허용된 denial 감사1과 금지된 업무 효과0을 분리한다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다

  시나리오: 잘못된 계보·합침 mergeDifferentControl를 거부한다
    먼저 사례 파일 "verification/cases/T03/case.json"의 "reject-mergeDifferentControl"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before-api" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "warehouse" 역할이 "invalid" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "invalid-results-1" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "invalid-quantity-effects-2" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "invalid-quantity-effects-3" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "invalid-quantity-effects-4" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "invalid-allocation-effects-5" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "invalid-results-6" assertion으로 "명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다."를 확인한다
    그러면 "invalid-results-7" assertion으로 "명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다."를 확인한다
    그러면 "invalid-results-8" assertion으로 "허용된 denial 감사1과 금지된 업무 효과0을 분리한다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다

  시나리오: 잘못된 계보·합침 mergeIncompatibleUnit를 거부한다
    먼저 사례 파일 "verification/cases/T03/case.json"의 "reject-mergeIncompatibleUnit"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before-api" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "warehouse" 역할이 "invalid" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "invalid-results-1" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "invalid-quantity-effects-2" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "invalid-quantity-effects-3" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "invalid-quantity-effects-4" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "invalid-allocation-effects-5" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "invalid-results-6" assertion으로 "명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다."를 확인한다
    그러면 "invalid-results-7" assertion으로 "명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다."를 확인한다
    그러면 "invalid-results-8" assertion으로 "허용된 denial 감사1과 금지된 업무 효과0을 분리한다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다

  시나리오: 식별 불가능 혼합100에서 계보 숫자로 깨끗한60을 선택하지 않는다
    먼저 사례 파일 "verification/cases/T03/case.json"의 "indistinguishable-mixture"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before-api" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "qc" 역할이 "hold" 행동을 수행한다
    만일 "warehouse" 역할이 "trace" 행동을 수행한다
    만일 "warehouse" 역할이 "held-api" 행동을 수행한다
    만일 "시스템" 역할이 "held-db" 행동을 수행한다
    만일 "qc" 역할이 "clean-release" 행동을 수행한다
    만일 "warehouse" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "source-affected-1" assertion으로 "실제 trace의 문제 원천40과 관찰된 기준 단위BOX를 decimal primary로 직접 대조한다."를 확인한다
    그러면 "source-affected-2" assertion으로 "원천40의 기준 단위를 보존한다."를 확인한다
    그러면 "current-candidate-scope-3" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "current-candidate-scope-4" assertion으로 "실물량·단위와 독립 손계산을 대조한다."를 확인한다
    그러면 "current-candidate-scope-5" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "arbitrary-clean-selection-6" assertion으로 "공개 명령의 구조화 outcome을 확인한다."를 확인한다
    그러면 "arbitrary-clean-selection-7" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "arbitrary-clean-selection-8" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "arbitrary-clean-selection-9" assertion으로 "금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다."를 확인한다
    그러면 "arbitrary-clean-selection-10" assertion으로 "허용된 denial 감사1과 금지된 업무 효과0을 분리한다."를 확인한다
    그러면 "trace-certainty-11" assertion으로 "계보의 영향 후보와 오염 확정을 구분한다."를 확인한다
    그러면 "trace-certainty-12" assertion으로 "부분 해제는 실제 분리/검사 근거가 필요하다."를 확인한다
    그러면 "trace-certainty-13" assertion으로 "독립 원 행의 실물·수량·관계·범위를 정확히 대조한다."를 확인한다
    그러면 "active-physical-identities" assertion으로 "현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다."를 확인한다
    그러면 "response-definition-version" assertion으로 "수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다."를 확인한다
    그러면 "mixed-source-quantities" assertion으로 "문제 원천40과 나머지60의 계보 근거를 원 행에서 대조한다. 이 관계만으로 실제 clean subset을 선택하지 않는다."를 확인한다
    그러면 "actual-baseline-physical-rows" assertion으로 "서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다."를 확인한다
