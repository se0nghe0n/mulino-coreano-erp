# language: ko
기능: M60 실사98과 운송중2는 즉시 분실 조정이 아니다
  시나리오: M60의 같은 fixture와 oracle를 SIT와 UAT로 확인한다
    먼저 model binding "M60"를 준비한다
    만일 model binding "M60"의 "install" 단계를 실행한다
    그리고 model binding "M60"의 "turn-1/context" 단계를 실행한다
    그리고 model binding "M60"의 "turn-1/before" 단계를 실행한다
    그리고 model binding "M60"의 "turn-1/agent" 단계를 실행한다
    그리고 model binding "M60"의 "turn-1/after" 단계를 실행한다
    그리고 model binding "M60"의 "turn-1/assert" 단계를 실행한다
    그리고 model binding "M60"의 "M60/turn-1/common-1" oracle로 "state.stocktake.observed.value"를 확인한다
    그리고 model binding "M60"의 "M60/turn-1/common-2" oracle로 "state.transport.remaining.value"를 확인한다
    그리고 model binding "M60"의 "M60/turn-1/common-3" oracle로 "effects.lossAdjustmentCount"를 확인한다
    그리고 model binding "M60"의 "M60/turn-1/common-4" oracle로 "effects.bankTransferCount"를 확인한다
    그러면 model binding "M60"의 모든 turn을 독립 관찰로 판정한다
