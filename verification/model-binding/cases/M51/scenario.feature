# language: ko
기능: M51 출고 후 판매금지에도 실제 인도는 보존한다
  시나리오: M51의 같은 fixture와 oracle를 SIT와 UAT로 확인한다
    먼저 model binding "M51"를 준비한다
    만일 model binding "M51"의 "install" 단계를 실행한다
    그리고 model binding "M51"의 "turn-1/context" 단계를 실행한다
    그리고 model binding "M51"의 "turn-1/before" 단계를 실행한다
    그리고 model binding "M51"의 "turn-1/agent" 단계를 실행한다
    그리고 model binding "M51"의 "turn-1/after" 단계를 실행한다
    그리고 model binding "M51"의 "turn-1/assert" 단계를 실행한다
    그리고 model binding "M51"의 "M51/turn-1/common-1" oracle로 "state.delivery.actual.value"를 확인한다
    그리고 model binding "M51"의 "M51/turn-1/common-2" oracle로 "state.transport.remaining.value"를 확인한다
    그리고 model binding "M51"의 "M51/turn-1/common-3" oracle로 "effects.newWarehouseDispatchCount"를 확인한다
    그리고 model binding "M51"의 "M51/turn-1/common-4" oracle로 "effects.newAllocationCount"를 확인한다
    그리고 model binding "M51"의 "M51/turn-1/common-5" oracle로 "state.assessment.normalFulfillment"를 확인한다
    그리고 model binding "M51"의 "M51/turn-1/common-6" oracle로 "state.delivery.compliance"를 확인한다
    그러면 model binding "M51"의 모든 turn을 독립 관찰로 판정한다
