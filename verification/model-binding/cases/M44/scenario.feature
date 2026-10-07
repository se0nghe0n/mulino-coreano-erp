# language: ko
기능: M44 과거 evaluator 미지원은 최신 의미로 바꾸지 않는다
  시나리오: M44의 같은 fixture와 oracle를 SIT와 UAT로 확인한다
    먼저 model binding "M44"를 준비한다
    만일 model binding "M44"의 "install" 단계를 실행한다
    그리고 model binding "M44"의 "turn-1/context" 단계를 실행한다
    그리고 model binding "M44"의 "turn-1/before" 단계를 실행한다
    그리고 model binding "M44"의 "turn-1/agent" 단계를 실행한다
    그리고 model binding "M44"의 "turn-1/after" 단계를 실행한다
    그리고 model binding "M44"의 "turn-1/assert" 단계를 실행한다
    그리고 model binding "M44"의 "M44/turn-1/common-1" oracle로 "state.work.O1.state"를 확인한다
    그리고 model binding "M44"의 "M44/turn-1/common-2" oracle로 "effects.businessEffectCount"를 확인한다
    그러면 model binding "M44"의 모든 turn을 독립 관찰로 판정한다
