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

정정 대상이 아직 canonical 사실로 확인되지 않은 EVENT·CLAIM·DOCUMENT라도
접수 책임을 잃지 않는다. ensureEvidenceCorrectionDuty는 authoritative raw
source와 SourceProfile을 조직 범위에서 확인하고 HUMAN intake owner·supervisor,
저장된 nextAction·nextCheckAt을 보존한 UNVERIFIED_SOURCE_REVIEW를 만든다.
raw source revision과 kind를 root key에 사용하고 canonical occurrence나
실물 효과·목표 충족 근거를 만들지 않는다. 이 hook만 raw source 종류를
허용한다. 일반 createObligation은 계속 OCCURRENCE·DECISION만 허용한다.
같은 root가 이미 해소·이전됐으면 접수 재처리가 원 책임을 되돌리지 않는다.

같은 50 완료 증거로 두 50 leaf를 해소하면 실제 50이 책임 100을 지운다.
이 반례를 막기 위해 evidence owner의 ResponsibilityCompletionEvidence가
검증된 immutable source·hash·현재 review에 묶인 root/range를 반환한다.
resolver는 실제 canonical 수량과 typed coverage를 맞추고 authoritative leaf의
startQuantity/quantity가 그 범위 안에 있는지 검사한다. CompletionBindings는
한 canonical occurrence를 한 stable root와 범위에 고정한다. ResolutionCredits는
정확한 assignment/leaf 범위를 같은 transaction에 소비한다. evidence ID lock,
UNIQUE assignment, range EXCLUDE, binding row lock과 deferred 수량 검사가
중복·겹침·경합을 막는다. verified partial 50은 대응하는 50 leaf만 해소한다.
verified full 100은 서로 겹치지 않는 두 50 leaf를 각각 해소할 수 있다.
