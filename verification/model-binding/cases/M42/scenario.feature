# language: ko
기능: M42 공급자의 QC 해제 지시에는 QC 권한이 없다
  시나리오: M42의 같은 fixture와 oracle를 SIT와 UAT로 확인한다
    먼저 model binding "M42"를 준비한다
    만일 model binding "M42"의 "install" 단계를 실행한다
    그리고 model binding "M42"의 "turn-1/context" 단계를 실행한다
    그리고 model binding "M42"의 "turn-1/before" 단계를 실행한다
    그리고 model binding "M42"의 "turn-1/agent" 단계를 실행한다
    그리고 model binding "M42"의 "turn-1/after" 단계를 실행한다
    그리고 model binding "M42"의 "turn-1/assert" 단계를 실행한다
    그리고 model binding "M42"의 "M42/turn-1/common-1" oracle로 "state.segment.B.qc"를 확인한다
    그리고 model binding "M42"의 "M42/turn-1/common-2" oracle로 "effects.holdReleaseCount"를 확인한다
    그러면 model binding "M42"의 모든 turn을 독립 관찰로 판정한다
