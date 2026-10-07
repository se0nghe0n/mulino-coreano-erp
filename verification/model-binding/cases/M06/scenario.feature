# language: ko
기능: M06 혼합 언어 보류 조회는 구별된 B 범위만 읽는다
  시나리오: M06의 같은 fixture와 oracle를 SIT와 UAT로 확인한다
    먼저 model binding "M06"를 준비한다
    만일 model binding "M06"의 "install" 단계를 실행한다
    그리고 model binding "M06"의 "turn-1/context" 단계를 실행한다
    그리고 model binding "M06"의 "turn-1/before" 단계를 실행한다
    그리고 model binding "M06"의 "turn-1/agent" 단계를 실행한다
    그리고 model binding "M06"의 "turn-1/after" 단계를 실행한다
    그리고 model binding "M06"의 "turn-1/assert" 단계를 실행한다
    그리고 model binding "M06"의 "M06/turn-1/common-1" oracle로 "response.heldSegmentRefs"를 확인한다
    그리고 model binding "M06"의 "M06/turn-1/common-2" oracle로 "response.heldQuantity.value"를 확인한다
    그리고 model binding "M06"의 "M06/turn-1/common-3" oracle로 "response.scopeAllItemHeld"를 확인한다
    그러면 model binding "M06"의 모든 turn을 독립 관찰로 판정한다
