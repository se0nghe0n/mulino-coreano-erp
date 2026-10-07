# 현재 철회와 실행 fence의 테스트 계약

세 직렬화 지점과 전체 restart를 구별한다. enqueue commit 뒤/실행
인가 전, 초기 인가 읽기 뒤/common scope fence 전에는 철회가 먼저
commit하도록 한다. fence를 획득한 출고가 먼저인 반대 순서에서는
철회 요청의 BLOCKED ACK를 확인하고 출고를 resume/terminal await한
뒤 철회를 완료한다. 같은 fence를 잡은 출고를 멈춘 채 철회 commit을
기다려 교착시키지 않는다. sleep이나 mock lock은 없다.

모든 start는 정확히 하나의 terminal await로 끝난다. barrier 요청과
ACK의 transaction/participant/point/state/scope를 exact 대조한다.
실제 fenceCommits의 scope revision과 명령 ID 순서는 독립 원행이다.
서버의 serialized=true 같은 Boolean을 oracle로 쓰지 않는다.

철회가 먼저면 새 출고0 BOX·새 COMMITTED 효과0이며 owner·다음
재인가 행동·다음 확인 시각·현재 assignment1이 남는다. 출고가
먼저면20 BOX의 확정 사실을 보존하고 철회 이후 요청의 새 효과0을
검사한다. 전체 restart 분기는 prior20을 실제 command로 먼저 만든다.
기확정 효과를 seed하지 않는다. 원 시도 terminal, application/
scheduler/두 worker의 실제 새 process instance, autonomous scheduler
task terminal 뒤 현재 grant/claim을 재검사하는 safe retry를 관찰한다.

모든 fixture는 가상값이다. Step2의 parser·assertion·RED 준비와 실제
제품 DB/API/MCP/경합/host/모델 인수를 구별한다. 제품 인수는 NOT_RUN이다.

작성 검증의 실제 명령·exit·scenario 수·파일 hash는
`verification/cases/T08/evidence/authority-suite/checks.json`에 있다.
`observation-bindings.json`은 이 case의 모든 catalog observation을
구체 subcase/action/assertion 및 JSON pointer에 연결한다.
고정 수량 oracle의 primary와 보조 관계/assertion을 함께 보존한다.
이 연결은 작성 증거이며 실제 제품 효과를 관측한 결과가 아니다.
