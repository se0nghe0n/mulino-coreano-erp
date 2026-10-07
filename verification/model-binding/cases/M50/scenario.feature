# language: ko
기능: M50 동일 명령 retry는 새 RPC로도 수령 효과 하나다
  시나리오: M50의 같은 fixture와 oracle를 SIT와 UAT로 확인한다
    먼저 model binding "M50"를 준비한다
    만일 model binding "M50"의 "install" 단계를 실행한다
    그리고 model binding "M50"의 "turn-1/context" 단계를 실행한다
    그리고 model binding "M50"의 "turn-1/before" 단계를 실행한다
    그리고 model binding "M50"의 "turn-1/agent" 단계를 실행한다
    그리고 model binding "M50"의 "turn-1/after" 단계를 실행한다
    그리고 model binding "M50"의 "turn-1/assert" 단계를 실행한다
    그리고 model binding "M50"의 "M50/turn-1/common-1" oracle로 "state.receipt.cumulative.value"를 확인한다
    그리고 model binding "M50"의 "M50/turn-1/common-2" oracle로 "state.receipt.totalEffectCount"를 확인한다
    그리고 model binding "M50"의 "M50/turn-1/common-3" oracle로 "effects.newReceiptCount"를 확인한다
    그리고 model binding "M50"의 "M50/turn-1/common-4" oracle로 "effects.newMovementCount"를 확인한다
    그리고 model binding "M50"의 "M50/turn-1/common-5" oracle로 "effects.newOutboxCount"를 확인한다
    그리고 model binding "M50"의 "M50/turn-1/common-6" oracle로 "response.reusedCommittedResult"를 확인한다
    그리고 model binding "M50"의 "turn-2/context" 단계를 실행한다
    그리고 model binding "M50"의 "turn-2/before" 단계를 실행한다
    그리고 model binding "M50"의 "turn-2/agent" 단계를 실행한다
    그리고 model binding "M50"의 "turn-2/after" 단계를 실행한다
    그리고 model binding "M50"의 "turn-2/assert" 단계를 실행한다
    그리고 model binding "M50"의 "M50/turn-2/common-1" oracle로 "state.receipt.cumulative.value"를 확인한다
    그리고 model binding "M50"의 "M50/turn-2/common-2" oracle로 "effects.newReceiptCount"를 확인한다
    그리고 model binding "M50"의 "M50/turn-2/common-3" oracle로 "effects.newMovementCount"를 확인한다
    그리고 model binding "M50"의 "M50/turn-2/common-4" oracle로 "effects.newOutboxCount"를 확인한다
    그러면 model binding "M50"의 모든 turn을 독립 관찰로 판정한다
