# 2026-10-23 일반 tier의 이동 ID·입력 projection 통합

출고의 실제 이동 ID와 저장된 목표·immutable 입력 projection을 통합했다.
고정9ebec25c의 전체 harness는753개 중673 PASS·80 opt-in SKIP이며
실패·오류0, Maven exit0이다. Task와 현재 Step은 OPEN이다.

## 구현과 결합 검사

coordinator의 b59ce369는 같은 organization·command·출발/도착 segment의
실제 DISPATCH 이동 ID를 응답에 연결한다. 부분/전체 출고와 replay,
rollback 뒤 같은 요청 재시도 등 실제 gateway/PG8개가 PASS했다.
[이동 ID 기록](dispatch-movement-identity.md)을 따른다.

worker700101fd를9ebec25c로 통합했다. 실제 Goal 슬롯과 InputSnapshots.ID를
투영하고 조직·Work·Goal·Assessment·hash·조건·시점을 대조한다.
worker의 unit30개와 실제 PG13개가 PASS했다. 서로 다른 Goal의 실제
snapshot ID를 합성 공통 ID로 바꾸지 않았다. ID 동등성 기대2개와
immutable 입력의 segment/movement 원천 등 잔여는 그대로 남긴다.
[projection 계약](t09-goal-input-projection.md)을 따른다.

root 결합 harness는 실제 PG observer53개와 source-row replay를 포함한다.
이 replay는 기존 HTTP에서 잡힌 immutable 행을 새 PG로 읽은 증거이며
제품 시나리오 재실행이 아니다. source hash drift는0이다. prepare는
41 case·802 subcase·24,260 assertion·문제0·asset12 PASS이고 gate는 false다.
이후 전체 backend를 다시 실행한 것으로 표시하지 않는다.

첫 root harness 시도는 observer test의 고정 fixture 인증과 임의 생성한
인증 설정이 달라 실패했다. 환경 오류의 원본 로그·JUnit을 보존하고
정해진 fixture 인증으로 새 PG에서 전체 검사에 통과했다. 두 시도의
자체 container 부재를 확인했다. 실행·hash·cleanup proof는
`.local/resume-20261023/goal-input-integrated-corrected/`에 있다.
첫 시도는 sibling `goal-input-integrated/`에 보존했다.

## 이전 고정 source의 Haiku 진단

별도 db594723 실행은 후속 b59ce369/9ebec25c를 포함하지 않는다.
native Workflow는 wf_6aee2f78-bd3이고 agent4개 metadata 및 assistant
message138개의 실제 모델이 모두 claude-haiku-5-5임을 확인했다.

| 사례 | raw subcase PASS/FAIL | raw assertion PASS/FAIL/미관찰 |
|---|---|---|
| T09 | 6/3 | 72/22/0 |
| T11 | 3/9 | 31/8/71 |
| T17 | 0/15 | 74/183/0 |

T17 native node의 첫 실행이 조기 SIGTERM으로 끝났고 controller가
Workflow 밖 background command로 다시 실행했다. 표의 T17 두 번째
결과는 진단 자료로만 보존하며 사용자 지정 native Workflow 인수 증거로
인정하지 않는다. 36개 전체의 Workflow 인수 완료를 주장하지 않는다.
T11 미관찰71개도 PASS로 세지 않는다. 최상위 complete flag 부재는
ABSENT이며 gate는 false다. 독립 raw ID/count/hash54개와 모델 대조는
`.local/resume-20261023/standard-wave-diagnostic-proof.json`에 있다.

새 Sol worker와 coordinator의 serviceTier는 default다. 제품 실모델 client
호출0회, 실제 규제·운영·BTP 인수와 전체 Task 완료는 여전히 미확정이다.
