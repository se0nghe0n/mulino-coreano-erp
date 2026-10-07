# language: ko
기능: M07 외부 접수 증거가 있는 제출만 기록한다
  시나리오: M07의 같은 fixture와 oracle를 SIT와 UAT로 확인한다
    먼저 model binding "M07"를 준비한다
    만일 model binding "M07"의 "install" 단계를 실행한다
    그리고 model binding "M07"의 "turn-1/context" 단계를 실행한다
    그리고 model binding "M07"의 "turn-1/before" 단계를 실행한다
    그리고 model binding "M07"의 "turn-1/agent" 단계를 실행한다
    그리고 model binding "M07"의 "turn-1/after" 단계를 실행한다
    그리고 model binding "M07"의 "turn-1/assert" 단계를 실행한다
    그리고 model binding "M07"의 "M07/turn-1/common-1" oracle로 "state.regulatory.status"를 확인한다
    그리고 model binding "M07"의 "M07/turn-1/common-2" oracle로 "effects.externalSubmissionCount"를 확인한다
    그러면 model binding "M07"의 모든 turn을 독립 관찰로 판정한다
