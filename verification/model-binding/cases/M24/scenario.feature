# language: ko
기능: M24 수량의 단위를 추정하지 않는다
  시나리오: M24의 같은 fixture와 oracle를 SIT와 UAT로 확인한다
    먼저 model binding "M24"를 준비한다
    만일 model binding "M24"의 "install" 단계를 실행한다
    그리고 model binding "M24"의 "turn-1/context" 단계를 실행한다
    그리고 model binding "M24"의 "turn-1/before" 단계를 실행한다
    그리고 model binding "M24"의 "turn-1/agent" 단계를 실행한다
    그리고 model binding "M24"의 "turn-1/after" 단계를 실행한다
    그리고 model binding "M24"의 "turn-1/assert" 단계를 실행한다
    그리고 model binding "M24"의 "M24/turn-1/common-1" oracle로 "effects.businessEffectCount"를 확인한다
    그리고 model binding "M24"의 "turn-2/context" 단계를 실행한다
    그리고 model binding "M24"의 "turn-2/before" 단계를 실행한다
    그리고 model binding "M24"의 "turn-2/agent" 단계를 실행한다
    그리고 model binding "M24"의 "turn-2/after" 단계를 실행한다
    그리고 model binding "M24"의 "turn-2/assert" 단계를 실행한다
    그리고 model binding "M24"의 "M24/turn-2/common-1" oracle로 "state.allocation.quantity"를 확인한다
    그리고 model binding "M24"의 "M24/turn-2/common-2" oracle로 "state.allocation.unit"를 확인한다
    그러면 model binding "M24"의 모든 turn을 독립 관찰로 판정한다
