# language: ko
기능: M02 발주 표현은 구매 제안과 도착 목표를 구성한다
  시나리오: M02의 같은 fixture와 oracle를 SIT와 UAT로 확인한다
    먼저 model binding "M02"를 준비한다
    만일 model binding "M02"의 "install" 단계를 실행한다
    그리고 model binding "M02"의 "turn-1/context" 단계를 실행한다
    그리고 model binding "M02"의 "turn-1/before" 단계를 실행한다
    그리고 model binding "M02"의 "turn-1/agent" 단계를 실행한다
    그리고 model binding "M02"의 "turn-1/after" 단계를 실행한다
    그리고 model binding "M02"의 "turn-1/assert" 단계를 실행한다
    그리고 model binding "M02"의 "M02/turn-1/common-1" oracle로 "state.purchaseProposal.quantity"를 확인한다
    그리고 model binding "M02"의 "M02/turn-1/common-2" oracle로 "state.purchaseProposal.endpoint"를 확인한다
    그리고 model binding "M02"의 "M02/turn-1/common-3" oracle로 "effects.newInventoryQuantity"를 확인한다
    그리고 model binding "M02"의 "M02/turn-1/common-4" oracle로 "effects.externalSendCount"를 확인한다
    그러면 model binding "M02"의 모든 turn을 독립 관찰로 판정한다
