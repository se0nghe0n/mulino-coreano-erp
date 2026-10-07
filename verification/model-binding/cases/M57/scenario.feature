# language: ko
기능: M57 회수25의 폐기25는 처리50으로 합산하지 않는다
  시나리오: M57의 같은 fixture와 oracle를 SIT와 UAT로 확인한다
    먼저 model binding "M57"를 준비한다
    만일 model binding "M57"의 "install" 단계를 실행한다
    그리고 model binding "M57"의 "turn-1/context" 단계를 실행한다
    그리고 model binding "M57"의 "turn-1/before" 단계를 실행한다
    그리고 model binding "M57"의 "turn-1/agent" 단계를 실행한다
    그리고 model binding "M57"의 "turn-1/after" 단계를 실행한다
    그리고 model binding "M57"의 "turn-1/assert" 단계를 실행한다
    그리고 model binding "M57"의 "M57/turn-1/common-1" oracle로 "state.recall.distinctProcessed.value"를 확인한다
    그리고 model binding "M57"의 "M57/turn-1/common-2" oracle로 "state.recall.unknown.value"를 확인한다
    그리고 model binding "M57"의 "M57/turn-1/common-3" oracle로 "state.recall.closed"를 확인한다
    그러면 model binding "M57"의 모든 turn을 독립 관찰로 판정한다
