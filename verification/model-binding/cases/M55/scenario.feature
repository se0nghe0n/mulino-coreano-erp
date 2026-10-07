# language: ko
기능: M55 실제 반품은 과거 인도100을 정정하지 않는다
  시나리오: M55의 같은 fixture와 oracle를 SIT와 UAT로 확인한다
    먼저 model binding "M55"를 준비한다
    만일 model binding "M55"의 "install" 단계를 실행한다
    그리고 model binding "M55"의 "turn-1/context" 단계를 실행한다
    그리고 model binding "M55"의 "turn-1/before" 단계를 실행한다
    그리고 model binding "M55"의 "turn-1/agent" 단계를 실행한다
    그리고 model binding "M55"의 "turn-1/after" 단계를 실행한다
    그리고 model binding "M55"의 "turn-1/assert" 단계를 실행한다
    그리고 model binding "M55"의 "M55/turn-1/common-1" oracle로 "state.delivery.historical.value"를 확인한다
    그리고 model binding "M55"의 "M55/turn-1/common-2" oracle로 "state.return.received.value"를 확인한다
    그리고 model binding "M55"의 "M55/turn-1/common-3" oracle로 "state.return.qc"를 확인한다
    그리고 model binding "M55"의 "M55/turn-1/common-4" oracle로 "state.inventory.returnedOnHand.value"를 확인한다
    그리고 model binding "M55"의 "M55/turn-1/common-5" oracle로 "effects.deliveryCorrectionCount"를 확인한다
    그리고 model binding "M55"의 "turn-2/context" 단계를 실행한다
    그리고 model binding "M55"의 "turn-2/before" 단계를 실행한다
    그리고 model binding "M55"의 "turn-2/agent" 단계를 실행한다
    그리고 model binding "M55"의 "turn-2/after" 단계를 실행한다
    그리고 model binding "M55"의 "turn-2/assert" 단계를 실행한다
    그리고 model binding "M55"의 "M55/turn-2/common-1" oracle로 "state.delivery.historical.value"를 확인한다
    그리고 model binding "M55"의 "M55/turn-2/common-2" oracle로 "state.return.received.value"를 확인한다
    그리고 model binding "M55"의 "M55/turn-2/common-3" oracle로 "effects.newReturnCount"를 확인한다
    그리고 model binding "M55"의 "M55/turn-2/common-4" oracle로 "effects.newPhysicalQuantity"를 확인한다
    그리고 model binding "M55"의 "M55/turn-2/common-5" oracle로 "state.currentReturnObligationCount"를 확인한다
    그러면 model binding "M55"의 모든 turn을 독립 관찰로 판정한다
