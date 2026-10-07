# language: ko
기능: M17 이탈리아어 누적 도착과 현재 보유를 비교한다
  시나리오: M17의 같은 fixture와 oracle를 SIT와 UAT로 확인한다
    먼저 model binding "M17"를 준비한다
    만일 model binding "M17"의 "install" 단계를 실행한다
    그리고 model binding "M17"의 "turn-1/context" 단계를 실행한다
    그리고 model binding "M17"의 "turn-1/before" 단계를 실행한다
    그리고 model binding "M17"의 "turn-1/agent" 단계를 실행한다
    그리고 model binding "M17"의 "turn-1/after" 단계를 실행한다
    그리고 model binding "M17"의 "turn-1/assert" 단계를 실행한다
    그리고 model binding "M17"의 "M17/turn-1/common-1" oracle로 "response.cumulativeArrived.value"를 확인한다
    그리고 model binding "M17"의 "M17/turn-1/common-2" oracle로 "response.currentOnHand.value"를 확인한다
    그리고 model binding "M17"의 "M17/turn-1/common-3" oracle로 "response.cumulativeGoal"를 확인한다
    그리고 model binding "M17"의 "M17/turn-1/common-4" oracle로 "response.current100Goal"를 확인한다
    그러면 model binding "M17"의 모든 turn을 독립 관찰로 판정한다
