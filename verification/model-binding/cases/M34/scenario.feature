# language: ko
기능: M34 승인 담당 질문은 승인 결정이 아니다
  시나리오: M34의 같은 fixture와 oracle를 SIT와 UAT로 확인한다
    먼저 model binding "M34"를 준비한다
    만일 model binding "M34"의 "install" 단계를 실행한다
    그리고 model binding "M34"의 "turn-1/context" 단계를 실행한다
    그리고 model binding "M34"의 "turn-1/before" 단계를 실행한다
    그리고 model binding "M34"의 "turn-1/agent" 단계를 실행한다
    그리고 model binding "M34"의 "turn-1/after" 단계를 실행한다
    그리고 model binding "M34"의 "turn-1/assert" 단계를 실행한다
    그리고 model binding "M34"의 "M34/turn-1/common-1" oracle로 "response.requiredApproverRole"를 확인한다
    그리고 model binding "M34"의 "M34/turn-1/common-2" oracle로 "response.approvalStatus"를 확인한다
    그리고 model binding "M34"의 "M34/turn-1/common-3" oracle로 "effects.approvalCount"를 확인한다
    그러면 model binding "M34"의 모든 turn을 독립 관찰로 판정한다
