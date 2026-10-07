# language: ko
기능: M20 정의 조회는 발행이나 전환을 만들지 않는다
  시나리오: M20의 같은 fixture와 oracle를 SIT와 UAT로 확인한다
    먼저 model binding "M20"를 준비한다
    만일 model binding "M20"의 "install" 단계를 실행한다
    그리고 model binding "M20"의 "turn-1/context" 단계를 실행한다
    그리고 model binding "M20"의 "turn-1/before" 단계를 실행한다
    그리고 model binding "M20"의 "turn-1/agent" 단계를 실행한다
    그리고 model binding "M20"의 "turn-1/after" 단계를 실행한다
    그리고 model binding "M20"의 "turn-1/assert" 단계를 실행한다
    그리고 model binding "M20"의 "M20/turn-1/common-1" oracle로 "response.endpoint"를 확인한다
    그리고 model binding "M20"의 "M20/turn-1/common-2" oracle로 "response.quantityMode"를 확인한다
    그리고 model binding "M20"의 "M20/turn-1/common-3" oracle로 "response.lotRequiredAt"를 확인한다
    그러면 model binding "M20"의 모든 turn을 독립 관찰로 판정한다
