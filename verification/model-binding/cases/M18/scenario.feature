# language: ko
기능: M18 원 통화와 환율 snapshot을 가진 송장을 기록한다
  시나리오: M18의 같은 fixture와 oracle를 SIT와 UAT로 확인한다
    먼저 model binding "M18"를 준비한다
    만일 model binding "M18"의 "install" 단계를 실행한다
    그리고 model binding "M18"의 "turn-1/context" 단계를 실행한다
    그리고 model binding "M18"의 "turn-1/before" 단계를 실행한다
    그리고 model binding "M18"의 "turn-1/agent" 단계를 실행한다
    그리고 model binding "M18"의 "turn-1/after" 단계를 실행한다
    그리고 model binding "M18"의 "turn-1/assert" 단계를 실행한다
    그리고 model binding "M18"의 "M18/turn-1/common-1" oracle로 "state.invoice.originalAmount"를 확인한다
    그리고 model binding "M18"의 "M18/turn-1/common-2" oracle로 "state.invoice.currency"를 확인한다
    그리고 model binding "M18"의 "M18/turn-1/common-3" oracle로 "state.invoice.convertedAmount"를 확인한다
    그리고 model binding "M18"의 "M18/turn-1/common-4" oracle로 "state.invoice.kind"를 확인한다
    그리고 model binding "M18"의 "M18/turn-1/common-5" oracle로 "effects.taxIssueCount"를 확인한다
    그러면 model binding "M18"의 모든 turn을 독립 관찰로 판정한다
