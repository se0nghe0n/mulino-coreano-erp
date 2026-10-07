# language: ko
기능: M35 반품 절차 질문은 반품 실물을 생성하지 않는다
  시나리오: M35의 같은 fixture와 oracle를 SIT와 UAT로 확인한다
    먼저 model binding "M35"를 준비한다
    만일 model binding "M35"의 "install" 단계를 실행한다
    그리고 model binding "M35"의 "turn-1/context" 단계를 실행한다
    그리고 model binding "M35"의 "turn-1/before" 단계를 실행한다
    그리고 model binding "M35"의 "turn-1/agent" 단계를 실행한다
    그리고 model binding "M35"의 "turn-1/after" 단계를 실행한다
    그리고 model binding "M35"의 "turn-1/assert" 단계를 실행한다
    그리고 model binding "M35"의 "M35/turn-1/common-1" oracle로 "response.defaultReturnQc"를 확인한다
    그리고 model binding "M35"의 "M35/turn-1/common-2" oracle로 "effects.returnCount"를 확인한다
    그러면 model binding "M35"의 모든 turn을 독립 관찰로 판정한다
