# Sales writer에 전달하는 인과 판정 port 계약

AssessmentService의 소유자는 T11 writer다. sales writer는 이 파일을
읽고 `WorkAssessmentGuard.assessCreated(c, workId)`만 호출한다.
새 enum 값과 DeliveryAdmission, DeliveryCommands 변경은 sales 소유다.
서로의 commit이나 JAR를 가져오지 않는다.

2026-10-24 추가 소유권 합의에 따라 T11 writer가
`TradeObservationRemedy.java`와 `ResponsibilityService.java`를 함께
수정한다. sales writer는 두 파일을 수정하지 않는다. Completion의
private 생성자와 그 호출에서 이미 검증한 actual quantity/unit를
typed accessor로 전달하므로 reflection이나 상호 compile 의존은 없다.
quantity가 있는 DELIVERY_RECONCILIATION은 root와 completion의 수량 및
단위가 정확히 같을 때만 해소한다. null quantity legacy와 RETURN의
기존 경계는 유지한다. 결합 성공은 통합 후 실제 PG로 확인해야 하며
sales의 현재 rollback 증거를 성공으로 바꾸어 기록하지 않는다.

`assessCreated`는 활성 command가 RECORD일 때 두 capability만 받는다.
`recordDelivery`, `recordObservedMovement`다. persisted IN_PROGRESS command의
ID·조직·actor·stableRequestOwner·canonical hash와 RECORD effect class를
확인한다. 저장된 authorization scope에 실제 Work와 item이 있어야 하고
Work는 ACTIVE 또는 WAITING이어야 한다. 현재 all-of grant를 같은 저장
scope로 다시 확인한다.

추가로 Work.lifecycleMode=COMMAND, Work.kind=DELIVERY를 요구한다.
currentGoalVersionId와 같은 Goal의 workId·definitionVersionId를 확인하고,
그 Goal과 slotsJson의 endpoint는 DELIVERED, slotsJson.eventKind는
PHYSICAL_DELIVERY여야 한다. 일반 판정기나 조회에서 이 port를 쓰지 않는다.

원래 intent의 observationId/canonicalOccurrenceId에 실제 Deliveries 행이
하나 있어야 한다. 그 행의 Work·Dispatch·segment·quantity와 같은
DeliveryTransfers 행이 **현재 commandId**에 있어야 한다. 따라서
provisional 접수, 다른 Work, 이전 command의 인도, 단순 claim이나
이름만 맞는 요청은 이 port를 이용할 수 없다. duplicate 경로에는 새
transfer가 없으므로 호출하지 않는다. 실제 인도 효과를 기록한 뒤,
같은 transaction에서 호출한다.

Purpose는 외부 slot 없이 enum.values()에서 capability()가 현재 RECORD
capability와 같은 정확히 하나의 값으로 정한다. sales가 해당 enum을
통합하기 전에는 VERSION_UNSUPPORTED로 보류한다. enum 이름 자체는
정하지 않으며 T11 source가 미통합 enum 상수를 참조하지 않는다.

그 Purpose를 기존 calculate/provider/evaluator에 전달한다. 원래 전체
GoalVersion의 판정이며 quantity만으로 SATISFIED를 만들지 않는다.
새 Assessment/InputSnapshot/EvidenceCapture는 실제 Purpose를 남긴다.
기존 snapshot은 쓰지 않는다. close/open guard는 각각 현재 권한으로
source를 다시 admit하고, source 비교에서는 시각 및 admission authority
provenance만 정규화한다. source identity·version·값·근거는 그대로
비교한다. 일반 assessGoal과 비RECORD 생성 판정의 기본 Purpose는 유지한다.

이 Work는 같은 명령이 방금 기록한 행을 읽기 위해 knownThrough를 적용한다.
asOf는 바꾸지 않는다. command pipeline의 기존 scope fence와 commit
재인가도 유지한다. 신규 capability/slot이나 조회 쓰기는 추가하지 않는다.

양쪽 통합 뒤 실제 RECORD→판정 경로는 sales의 focused PG 및 Haiku 실제
시나리오로 인수해야 한다. T11 단독 tree에는 RECORD Purpose가 아직 없어
성공 경로는 NOT_RUN이다. 현재 helper의 거부·기본 경로와 T11 회귀는
별도 focused checks에 기록한다.

## 접수 의무의 수량과 intake 책임

Completion은 실제 observation/actual의 subject·range·시각·unit 일치,
현재 canonical 검증, 실제 movement 수량 검사를 통과한 뒤만 생성한다.
duplicate RETURN은 자기 원천 검증과 기존 receipt의 검증을 함께 거친다.
세 진입 경로를 처리하는 두 private 생성자 호출에 실제 quantity/unit를
전달한다. DELIVERY root의 수량이 있으면 completion과 수량은 decimal
값으로, 단위는 정확히 비교한다. mismatch는 assignment 갱신 전에
거부한다. RETURN의 수량 root는 계속 거부하고 null quantity legacy는
기존대로 해소한다. immutable root나 이관 owner를 다시 쓰지 않는다.

sales의 기존 TradeImpact.recorded 호출은 바꾸지 않는다.
ResponsibilityService.openDuty의 DELIVERY_RECONCILIATION 생성 경로가
저장된 observation과 원본 EVENT의 source profile을 직접 읽는다.
이 경로는 현재 persisted IN_PROGRESS RECORD의 ID·조직·actor·request
owner·capability·canonical hash를 확인한다. command slots의 Work·Event·
명시한 observation ID와 저장된 PROVISIONAL 보고, scope의 originalWorkId·
domainSourceId·physicalScopeId, item·action·기한이 같아야 한다.
typed reported quantity/unit도 실제 보고와 같아야 한다.

원본 Event의 Work·item·kind와 profile namespace가 일치할 때만 profile의
intakeOwnerId·supervisorId를 사용한다. 두 인간의 authority fence와 현재
membership을 확인한 뒤 새 assignment에 배정한다. 기존 인도 Work의
owner는 바꾸지 않는다. Work owner와 raw source 접수 의무의 intake는
서로 다른 책임이다. 이미 있는 root는 현재 assignment를 그대로
반환하므로 수락한 이관 책임도 되돌리지 않는다. caller가 owner를 넣는
새 slot이나 capability는 없다. source admission의 기존 all-of 권한은
DeliveryCommands가 그대로 검사한다.
