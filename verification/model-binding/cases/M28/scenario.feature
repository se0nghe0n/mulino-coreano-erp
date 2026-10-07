# language: ko
기능: M28 외부 LOT 표기는 제조자와 품목을 확인한다
  시나리오: M28의 같은 fixture와 oracle를 SIT와 UAT로 확인한다
    먼저 model binding "M28"를 준비한다
    만일 model binding "M28"의 "install" 단계를 실행한다
    그리고 model binding "M28"의 "turn-1/context" 단계를 실행한다
    그리고 model binding "M28"의 "turn-1/before" 단계를 실행한다
    그리고 model binding "M28"의 "turn-1/agent" 단계를 실행한다
    그리고 model binding "M28"의 "turn-1/after" 단계를 실행한다
    그리고 model binding "M28"의 "turn-1/assert" 단계를 실행한다
    그리고 model binding "M28"의 "M28/turn-1/common-1" oracle로 "effects.businessEffectCount"를 확인한다
    그리고 model binding "M28"의 "turn-2/context" 단계를 실행한다
    그리고 model binding "M28"의 "turn-2/before" 단계를 실행한다
    그리고 model binding "M28"의 "turn-2/agent" 단계를 실행한다
    그리고 model binding "M28"의 "turn-2/after" 단계를 실행한다
    그리고 model binding "M28"의 "turn-2/assert" 단계를 실행한다
    그리고 model binding "M28"의 "M28/turn-2/common-1" oracle로 "response.lotRef"를 확인한다
    그리고 model binding "M28"의 "M28/turn-2/common-2" oracle로 "response.mergedLotRefs"를 확인한다
    그러면 model binding "M28"의 모든 turn을 독립 관찰로 판정한다
