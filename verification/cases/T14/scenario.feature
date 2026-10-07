# language: ko
@T14 @D14 @sit @uat
기능: 선적 배분 구간 인계와 원천 관측을 보존한다

  시나리오: 두 발주와 두 선적은 같은 실물을 중복 배정하지 않는다
    먼저 사례 파일 "verification/cases/T14/case.json"의 "many-to-many"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "split" 행동을 수행한다
    만일 "procurement" 역할이 "S1" 행동을 수행한다
    만일 "procurement" 역할이 "S2" 행동을 수행한다
    만일 "procurement" 역할이 "before-double" 행동을 수행한다
    만일 "시스템" 역할이 "before-double-db" 행동을 수행한다
    만일 "procurement" 역할이 "double" 행동을 수행한다
    만일 "procurement" 역할이 "after-double" 행동을 수행한다
    만일 "시스템" 역할이 "after-double-db" 행동을 수행한다
    만일 "source" 역할이 "S1-port" 행동을 수행한다
    만일 "source" 역할이 "S2-port" 행동을 수행한다
    만일 "warehouse" 역할이 "port-view" 행동을 수행한다
    만일 "시스템" 역할이 "port-db" 행동을 수행한다
    만일 "source" 역할이 "S1-handover" 행동을 수행한다
    만일 "source" 역할이 "S1-W" 행동을 수행한다
    만일 "source" 역할이 "S2-handover" 행동을 수행한다
    만일 "source" 역할이 "S2-W" 행동을 수행한다
    만일 "warehouse" 역할이 "W-view" 행동을 수행한다
    만일 "시스템" 역할이 "W-db" 행동을 수행한다
    그러면 "double-rejected" assertion으로 "고정 oracle double-order-allocation의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "cargo-no-double" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "double-denial-audit" assertion으로 "고정 oracle double-order-allocation의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "physical100" assertion으로 "고정 oracle distinct-cargo-total의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "physical-unique" assertion으로 "고정 oracle distinct-cargo-total의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "po-cargo-tuples" assertion으로 "고정 oracle leg-facts의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "port-not-W" assertion으로 "고정 oracle leg-facts의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "handover-custodian" assertion으로 "고정 oracle leg-facts의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "ownership-unchanged" assertion으로 "고정 oracle leg-facts의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "planned-actual-legs" assertion으로 "고정 oracle leg-facts의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "source-genealogy4" assertion으로 "고정 oracle leg-facts의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "genealogy-original-source" assertion으로 "고정 oracle leg-facts의 실제 값과 범위를 확인한다"를 확인한다

  시나리오: 출하100 수령98의 나머지2는 운송중이다
    먼저 사례 파일 "verification/cases/T14/case.json"의 "discrepancy-transit2"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "source" 역할이 "shipped" 행동을 수행한다
    만일 "warehouse" 역할이 "receive98" 행동을 수행한다
    만일 "source" 역할이 "transit2" 행동을 수행한다
    만일 "warehouse" 역할이 "view" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "received-api98" assertion으로 "고정 oracle received의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "loss-api0" assertion으로 "고정 oracle automatic-loss의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "received98" assertion으로 "고정 oracle received의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "receipt98" assertion으로 "고정 oracle received의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "shipclaim100" assertion으로 "고정 oracle separate-observations의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "no-loss-movement" assertion으로 "고정 oracle automatic-loss의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "loss-before-after" assertion으로 "고정 oracle automatic-loss의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "physical-total100" assertion으로 "고정 oracle separate-observations의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "transit-api2" assertion으로 "고정 oracle transit의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "transit2" assertion으로 "고정 oracle transit의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "discrepancy-owner-one" assertion으로 "고정 oracle discrepancy-responsibility의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "discrepancy-owner-owner" assertion으로 "고정 oracle discrepancy-responsibility의 실제 값과 범위를 확인한다"를 확인한다

  시나리오: 출하100 수령98의 나머지2는 미확인이다
    먼저 사례 파일 "verification/cases/T14/case.json"의 "discrepancy-unobserved2"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "source" 역할이 "shipped" 행동을 수행한다
    만일 "warehouse" 역할이 "receive98" 행동을 수행한다
    만일 "warehouse" 역할이 "view" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "received-api98" assertion으로 "고정 oracle received의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "loss-api0" assertion으로 "고정 oracle automatic-loss의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "received98" assertion으로 "고정 oracle received의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "receipt98" assertion으로 "고정 oracle received의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "shipclaim100" assertion으로 "고정 oracle separate-observations의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "no-loss-movement" assertion으로 "고정 oracle automatic-loss의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "loss-before-after" assertion으로 "고정 oracle automatic-loss의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "physical-total100" assertion으로 "고정 oracle separate-observations의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "unobserved-is-unknown" assertion으로 "고정 oracle separate-observations의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "unobserved-not-zero" assertion으로 "고정 oracle separate-observations의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "discrepancy-owner-one" assertion으로 "고정 oracle discrepancy-responsibility의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "discrepancy-owner-owner" assertion으로 "고정 oracle discrepancy-responsibility의 실제 값과 범위를 확인한다"를 확인한다

  시나리오: 종료된 부모의 온도 이상은 연결 실패와 알림 뒤에도 접수 책임을 유지한다
    먼저 사례 파일 "verification/cases/T14/case.json"의 "orphan-temperature"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "link-failure" 행동을 수행한다
    만일 "source" 역할이 "temperature" 행동을 수행한다
    만일 "intake" 역할이 "followup" 행동을 수행한다
    만일 "intake" 역할이 "before-notify" 행동을 수행한다
    만일 "시스템" 역할이 "before-notify-db" 행동을 수행한다
    만일 "시스템" 역할이 "notification-response" 행동을 수행한다
    만일 "시스템" 역할이 "notification-tick" 행동을 수행한다
    만일 "시스템" 역할이 "notification-terminal" 행동을 수행한다
    만일 "intake" 역할이 "after-notify" 행동을 수행한다
    만일 "시스템" 역할이 "after-notify-db" 행동을 수행한다
    그러면 "intake-human-one" assertion으로 "고정 oracle orphan-intake의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "intake-human-owner" assertion으로 "고정 oracle orphan-intake의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "no-notification-resolution" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "links-still0" assertion으로 "고정 oracle notification-closes-intake의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "notification-delivered" assertion으로 "고정 oracle notification-closes-intake의 실제 값과 범위를 확인한다"를 확인한다
