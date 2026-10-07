# S2 의무·책임 구현 경계

이전 assignment의 TRANSFERRED만으로 원 목표를 충족시키거나 의무를 해소하면
미해결 책임이 사라진다. 따라서 stable Roots 아래의 leaf Scopes마다 현재
assignment 하나를 유지하고 수락 전에는 기존 OPEN assignment를 보존한다.
V7 ObligationReferences가 authoritative assignment다. 별도 mutable projection을
만들지 않는다. V10의 deferred constraint는 root 물량, leaf 비중첩, 부모
범위, 현재 assignment 수량과 한 명의 인간 책임자를 commit에서 검사한다.

ResponsibilityService는 caller transaction 안에서만 실행한다. 공개 명령은
ResponsibilityCommands를 통해 공통 gateway의 현재 권한·policy·expected
revision·idem·audit·outbox를 사용한다. owner 교체는 recipient 수락 또는
명시적 EMERGENCY_REASSIGN 승인 action에만 있다. emergency는 Handover를
자동 수락시키지 않는다. 거절·만료·실패는 원 책임과 다음 행동을 유지한다.

부분 이전은 기존 leaf를 닫고 비중첩 잔여·이전 child를 같은 transaction에
만든다. predecessor chain으로 이전 work 재방문을 막는다. 원 목표의 판정은
변경하지 않는다. close/cancel hook은 유효 OPEN duty가 남으면 거부한다.

Core FOLLOWUP_REVIEW의 resolution은 실제 RESPONSE_COMPLETED canonical
occurrence와 COMPLETE 재평가, VERIFIED 원천·동일성·수량·시간·중복 근거를
요구한다. scope·현재 responsibleWork·수량·단위가 모두 일치하고 superseded
근거가 없어야 한다. 이후 QC·운송·판매별 종류의 실제 행위 oracle는 해당
도메인 resolver가 구현될 때까지 VERSION_UNSUPPORTED로 차단한다.
면제는 WAIVE_<kind> immutable APPROVED decision과 현재 gateway 정책을
요구하며 같은 approvalId를 evidenceId로 기록한다. 면제는 실제 행위 완료를
꾸미지 않는다. 실제 운영 승인자·법적 처분 근거는 fixture로 대신하지 않는다.

검증은 focused Java 단위와 fresh PostgreSQL의 독립 raw SQL 관찰을 구별한다.
현재 SQL concurrency 증거는 gateway·HTTP·MCP end-to-end 수락 인수가 아니다.
C2의 실제 도착/출고와 C5 온도 이상 intake 전체는 후속 도메인 결합 인수다.
