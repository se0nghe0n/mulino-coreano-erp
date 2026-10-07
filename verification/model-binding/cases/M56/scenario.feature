# language: ko
기능: M56 인도98 정정은 물리 반품을 만들지 않는다
  시나리오: M56의 같은 fixture와 oracle를 SIT와 UAT로 확인한다
    먼저 model binding "M56"를 준비한다
    만일 model binding "M56"의 "install" 단계를 실행한다
    그리고 model binding "M56"의 "turn-1/context" 단계를 실행한다
    그리고 model binding "M56"의 "turn-1/before" 단계를 실행한다
    그리고 model binding "M56"의 "turn-1/agent" 단계를 실행한다
    그리고 model binding "M56"의 "turn-1/after" 단계를 실행한다
    그리고 model binding "M56"의 "turn-1/assert" 단계를 실행한다
    그리고 model binding "M56"의 "M56/turn-1/common-1" oracle로 "state.delivery.currentActual.value"를 확인한다
    그리고 model binding "M56"의 "M56/turn-1/common-2" oracle로 "state.assessment.historical"를 확인한다
    그리고 model binding "M56"의 "M56/turn-1/common-3" oracle로 "state.currentShortfall.value"를 확인한다
    그리고 model binding "M56"의 "M56/turn-1/common-4" oracle로 "effects.returnCount"를 확인한다
    그리고 model binding "M56"의 "M56/turn-1/common-5" oracle로 "effects.newInventoryQuantity"를 확인한다
    그러면 model binding "M56"의 모든 turn을 독립 관찰로 판정한다
