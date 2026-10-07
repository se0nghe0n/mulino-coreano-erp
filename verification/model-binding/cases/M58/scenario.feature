# language: ko
기능: M58 겹친 회수 제한은 QC 해제로 사라지지 않는다
  시나리오: M58의 같은 fixture와 oracle를 SIT와 UAT로 확인한다
    먼저 model binding "M58"를 준비한다
    만일 model binding "M58"의 "install" 단계를 실행한다
    그리고 model binding "M58"의 "turn-1/context" 단계를 실행한다
    그리고 model binding "M58"의 "turn-1/before" 단계를 실행한다
    그리고 model binding "M58"의 "turn-1/agent" 단계를 실행한다
    그리고 model binding "M58"의 "turn-1/after" 단계를 실행한다
    그리고 model binding "M58"의 "turn-1/assert" 단계를 실행한다
    그리고 model binding "M58"의 "M58/turn-1/common-1" oracle로 "effects.dispatchCount"를 확인한다
    그리고 model binding "M58"의 "M58/turn-1/common-2" oracle로 "state.hold.H2.status"를 확인한다
    그리고 model binding "M58"의 "M58/turn-1/common-3" oracle로 "state.contaminationConfirmed"를 확인한다
    그러면 model binding "M58"의 모든 turn을 독립 관찰로 판정한다
