# language: ko
기능: M54 닫힌 부모의 새 이상은 접수 담당을 유지한다
  시나리오: M54의 같은 fixture와 oracle를 SIT와 UAT로 확인한다
    먼저 model binding "M54"를 준비한다
    만일 model binding "M54"의 "install" 단계를 실행한다
    그리고 model binding "M54"의 "turn-1/context" 단계를 실행한다
    그리고 model binding "M54"의 "turn-1/before" 단계를 실행한다
    그리고 model binding "M54"의 "turn-1/agent" 단계를 실행한다
    그리고 model binding "M54"의 "turn-1/after" 단계를 실행한다
    그리고 model binding "M54"의 "turn-1/assert" 단계를 실행한다
    그리고 model binding "M54"의 "M54/turn-1/common-1" oracle로 "state.intake.status"를 확인한다
    그리고 model binding "M54"의 "M54/turn-1/common-2" oracle로 "state.intake.ownerRef"를 확인한다
    그리고 model binding "M54"의 "M54/turn-1/common-3" oracle로 "state.intake.nextCheckAt"를 확인한다
    그리고 model binding "M54"의 "M54/turn-1/common-4" oracle로 "state.intake.closedByNotification"를 확인한다
    그리고 model binding "M54"의 "turn-2/context" 단계를 실행한다
    그리고 model binding "M54"의 "turn-2/before" 단계를 실행한다
    그리고 model binding "M54"의 "turn-2/agent" 단계를 실행한다
    그리고 model binding "M54"의 "turn-2/after" 단계를 실행한다
    그리고 model binding "M54"의 "turn-2/assert" 단계를 실행한다
    그리고 model binding "M54"의 "M54/turn-2/common-1" oracle로 "state.currentObligationCount"를 확인한다
    그리고 model binding "M54"의 "M54/turn-2/common-2" oracle로 "state.followupWorkCount"를 확인한다
    그리고 model binding "M54"의 "M54/turn-2/common-3" oracle로 "state.currentOwnerRef"를 확인한다
    그리고 model binding "M54"의 "M54/turn-2/common-4" oracle로 "state.parent.O1.state"를 확인한다
    그러면 model binding "M54"의 모든 turn을 독립 관찰로 판정한다
