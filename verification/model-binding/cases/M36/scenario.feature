# language: ko
기능: M36 출고 상태의 완료 표현을 재출고로 해석하지 않는다
  시나리오: M36의 같은 fixture와 oracle를 SIT와 UAT로 확인한다
    먼저 model binding "M36"를 준비한다
    만일 model binding "M36"의 "install" 단계를 실행한다
    그리고 model binding "M36"의 "turn-1/context" 단계를 실행한다
    그리고 model binding "M36"의 "turn-1/before" 단계를 실행한다
    그리고 model binding "M36"의 "turn-1/agent" 단계를 실행한다
    그리고 model binding "M36"의 "turn-1/after" 단계를 실행한다
    그리고 model binding "M36"의 "turn-1/assert" 단계를 실행한다
    그리고 model binding "M36"의 "M36/turn-1/common-1" oracle로 "response.dispatched.value"를 확인한다
    그리고 model binding "M36"의 "M36/turn-1/common-2" oracle로 "response.delivered.value"를 확인한다
    그리고 model binding "M36"의 "M36/turn-1/common-3" oracle로 "response.deliveryRemaining.value"를 확인한다
    그리고 model binding "M36"의 "M36/turn-1/common-4" oracle로 "effects.newDispatchCount"를 확인한다
    그러면 model binding "M36"의 모든 turn을 독립 관찰로 판정한다
