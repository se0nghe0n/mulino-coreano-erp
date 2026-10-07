# language: ko
기능: M09 분할은 부모를 소모하고 합계를 보존한다
  시나리오: M09의 같은 fixture와 oracle를 SIT와 UAT로 확인한다
    먼저 model binding "M09"를 준비한다
    만일 model binding "M09"의 "install" 단계를 실행한다
    그리고 model binding "M09"의 "turn-1/context" 단계를 실행한다
    그리고 model binding "M09"의 "turn-1/before" 단계를 실행한다
    그리고 model binding "M09"의 "turn-1/agent" 단계를 실행한다
    그리고 model binding "M09"의 "turn-1/after" 단계를 실행한다
    그리고 model binding "M09"의 "turn-1/assert" 단계를 실행한다
    그리고 model binding "M09"의 "M09/turn-1/common-1" oracle로 "state.segment.A.active"를 확인한다
    그리고 model binding "M09"의 "M09/turn-1/common-2" oracle로 "state.activeChildren.total.value"를 확인한다
    그리고 model binding "M09"의 "M09/turn-1/common-3" oracle로 "state.activeChildren.quantities"를 확인한다
    그리고 model binding "M09"의 "M09/turn-1/common-4" oracle로 "effects.newPhysicalQuantity"를 확인한다
    그러면 model binding "M09"의 모든 turn을 독립 관찰로 판정한다
