# language: ko
기능: M39 은행 이체 지원 질문은 지급을 실행하지 않는다
  시나리오: M39의 같은 fixture와 oracle를 SIT와 UAT로 확인한다
    먼저 model binding "M39"를 준비한다
    만일 model binding "M39"의 "install" 단계를 실행한다
    그리고 model binding "M39"의 "turn-1/context" 단계를 실행한다
    그리고 model binding "M39"의 "turn-1/before" 단계를 실행한다
    그리고 model binding "M39"의 "turn-1/agent" 단계를 실행한다
    그리고 model binding "M39"의 "turn-1/after" 단계를 실행한다
    그리고 model binding "M39"의 "turn-1/assert" 단계를 실행한다
    그리고 model binding "M39"의 "M39/turn-1/common-1" oracle로 "response.bankTransferSupported"를 확인한다
    그리고 model binding "M39"의 "M39/turn-1/common-2" oracle로 "effects.bankTransferCount"를 확인한다
    그러면 model binding "M39"의 모든 turn을 독립 관찰로 판정한다
