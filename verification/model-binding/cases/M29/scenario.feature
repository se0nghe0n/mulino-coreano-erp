# language: ko
기능: M29 대명사는 선택된 여러 물량을 임의 해제하지 않는다
  시나리오: M29의 같은 fixture와 oracle를 SIT와 UAT로 확인한다
    먼저 model binding "M29"를 준비한다
    만일 model binding "M29"의 "install" 단계를 실행한다
    그리고 model binding "M29"의 "turn-1/context" 단계를 실행한다
    그리고 model binding "M29"의 "turn-1/before" 단계를 실행한다
    그리고 model binding "M29"의 "turn-1/agent" 단계를 실행한다
    그리고 model binding "M29"의 "turn-1/after" 단계를 실행한다
    그리고 model binding "M29"의 "turn-1/assert" 단계를 실행한다
    그리고 model binding "M29"의 "M29/turn-1/common-1" oracle로 "effects.businessEffectCount"를 확인한다
    그리고 model binding "M29"의 "turn-2/context" 단계를 실행한다
    그리고 model binding "M29"의 "turn-2/before" 단계를 실행한다
    그리고 model binding "M29"의 "turn-2/agent" 단계를 실행한다
    그리고 model binding "M29"의 "turn-2/after" 단계를 실행한다
    그리고 model binding "M29"의 "turn-2/assert" 단계를 실행한다
    그리고 model binding "M29"의 "M29/turn-2/common-1" oracle로 "response.segmentRefs"를 확인한다
    그리고 model binding "M29"의 "M29/turn-2/common-2" oracle로 "response.heldQuantity.value"를 확인한다
    그러면 model binding "M29"의 모든 turn을 독립 관찰로 판정한다
