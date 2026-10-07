# language: ko
기능: M14 이탈리아어 지급 참조는 은행 이체를 만들지 않는다
  시나리오: M14의 같은 fixture와 oracle를 SIT와 UAT로 확인한다
    먼저 model binding "M14"를 준비한다
    만일 model binding "M14"의 "install" 단계를 실행한다
    그리고 model binding "M14"의 "turn-1/context" 단계를 실행한다
    그리고 model binding "M14"의 "turn-1/before" 단계를 실행한다
    그리고 model binding "M14"의 "turn-1/agent" 단계를 실행한다
    그리고 model binding "M14"의 "turn-1/after" 단계를 실행한다
    그리고 model binding "M14"의 "turn-1/assert" 단계를 실행한다
    그리고 model binding "M14"의 "M14/turn-1/common-1" oracle로 "state.paymentReference.amount"를 확인한다
    그리고 model binding "M14"의 "M14/turn-1/common-2" oracle로 "state.paymentReference.currency"를 확인한다
    그리고 model binding "M14"의 "M14/turn-1/common-3" oracle로 "effects.bankTransferCount"를 확인한다
    그러면 model binding "M14"의 모든 turn을 독립 관찰로 판정한다
