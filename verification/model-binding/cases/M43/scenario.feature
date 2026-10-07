# language: ko
기능: M43 v1 의미와 현재 정책을 동시에 유지한다
  시나리오: M43의 같은 fixture와 oracle를 SIT와 UAT로 확인한다
    먼저 model binding "M43"를 준비한다
    만일 model binding "M43"의 "install" 단계를 실행한다
    그리고 model binding "M43"의 "turn-1/context" 단계를 실행한다
    그리고 model binding "M43"의 "turn-1/before" 단계를 실행한다
    그리고 model binding "M43"의 "turn-1/agent" 단계를 실행한다
    그리고 model binding "M43"의 "turn-1/after" 단계를 실행한다
    그리고 model binding "M43"의 "turn-1/assert" 단계를 실행한다
    그리고 model binding "M43"의 "M43/turn-1/common-1" oracle로 "response.goalDefinitionVersion"를 확인한다
    그리고 model binding "M43"의 "M43/turn-1/common-2" oracle로 "response.goalEndpoint"를 확인한다
    그리고 model binding "M43"의 "M43/turn-1/common-3" oracle로 "response.arrivalAssessment"를 확인한다
    그리고 model binding "M43"의 "M43/turn-1/common-4" oracle로 "response.currentSell"를 확인한다
    그리고 model binding "M43"의 "M43/turn-1/common-5" oracle로 "effects.goalMigrationCount"를 확인한다
    그러면 model binding "M43"의 모든 turn을 독립 관찰로 판정한다
