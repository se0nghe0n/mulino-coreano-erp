# language: ko
기능: M47 철회된 grant는 queue 자격으로 대체하지 않는다
  시나리오: M47의 같은 fixture와 oracle를 SIT와 UAT로 확인한다
    먼저 model binding "M47"를 준비한다
    만일 model binding "M47"의 "install" 단계를 실행한다
    그리고 model binding "M47"의 "turn-1/context" 단계를 실행한다
    그리고 model binding "M47"의 "turn-1/before" 단계를 실행한다
    그리고 model binding "M47"의 "turn-1/agent" 단계를 실행한다
    그리고 model binding "M47"의 "turn-1/after" 단계를 실행한다
    그리고 model binding "M47"의 "turn-1/assert" 단계를 실행한다
    그리고 model binding "M47"의 "M47/turn-1/common-1" oracle로 "effects.dispatchCount"를 확인한다
    그리고 model binding "M47"의 "M47/turn-1/common-2" oracle로 "effects.allocationConsumptionCount"를 확인한다
    그리고 model binding "M47"의 "M47/turn-1/common-3" oracle로 "state.currentWriteAuthorization"를 확인한다
    그리고 model binding "M47"의 "M47/turn-1/common-4" oracle로 "state.currentOwnerRef"를 확인한다
    그리고 model binding "M47"의 "M47/turn-1/common-5" oracle로 "state.obligation.OB1.status"를 확인한다
    그리고 model binding "M47"의 "M47/turn-1/common-6" oracle로 "state.obligation.OB1.ownerRef"를 확인한다
    그러면 model binding "M47"의 모든 turn을 독립 관찰로 판정한다
