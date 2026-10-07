# language: ko
기능: M15 LOT 양방향 추적은 실물 식별을 보존한다
  시나리오: M15의 같은 fixture와 oracle를 SIT와 UAT로 확인한다
    먼저 model binding "M15"를 준비한다
    만일 model binding "M15"의 "install" 단계를 실행한다
    그리고 model binding "M15"의 "turn-1/context" 단계를 실행한다
    그리고 model binding "M15"의 "turn-1/before" 단계를 실행한다
    그리고 model binding "M15"의 "turn-1/agent" 단계를 실행한다
    그리고 model binding "M15"의 "turn-1/after" 단계를 실행한다
    그리고 model binding "M15"의 "turn-1/assert" 단계를 실행한다
    그리고 model binding "M15"의 "M15/turn-1/common-1" oracle로 "response.originRef"를 확인한다
    그리고 model binding "M15"의 "M15/turn-1/common-2" oracle로 "response.deliveryRefs"를 확인한다
    그리고 model binding "M15"의 "M15/turn-1/common-3" oracle로 "response.returnRefs"를 확인한다
    그리고 model binding "M15"의 "M15/turn-1/common-4" oracle로 "response.confirmedLotRefs"를 확인한다
    그러면 model binding "M15"의 모든 turn을 독립 관찰로 판정한다
