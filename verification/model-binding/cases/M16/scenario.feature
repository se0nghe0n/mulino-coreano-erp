# language: ko
기능: M16 종단 수량과 세 책임을 함께 조회한다
  시나리오: M16의 같은 fixture와 oracle를 SIT와 UAT로 확인한다
    먼저 model binding "M16"를 준비한다
    만일 model binding "M16"의 "install" 단계를 실행한다
    그리고 model binding "M16"의 "turn-1/context" 단계를 실행한다
    그리고 model binding "M16"의 "turn-1/before" 단계를 실행한다
    그리고 model binding "M16"의 "turn-1/agent" 단계를 실행한다
    그리고 model binding "M16"의 "turn-1/after" 단계를 실행한다
    그리고 model binding "M16"의 "turn-1/assert" 단계를 실행한다
    그리고 model binding "M16"의 "M16/turn-1/common-1" oracle로 "response.arrived.value"를 확인한다
    그리고 model binding "M16"의 "M16/turn-1/common-2" oracle로 "response.onHand.value"를 확인한다
    그리고 model binding "M16"의 "M16/turn-1/common-3" oracle로 "response.delivered.value"를 확인한다
    그리고 model binding "M16"의 "M16/turn-1/common-4" oracle로 "response.returned.value"를 확인한다
    그리고 model binding "M16"의 "M16/turn-1/common-5" oracle로 "response.eligible.value"를 확인한다
    그리고 model binding "M16"의 "M16/turn-1/common-6" oracle로 "response.openObligationKinds"를 확인한다
    그리고 model binding "M16"의 "M16/turn-1/common-7" oracle로 "effects.bankTransferCount"를 확인한다
    그러면 model binding "M16"의 모든 turn을 독립 관찰로 판정한다
