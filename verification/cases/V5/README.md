# V5 전체 재시작·실제 두 worker·lease fence 인수 계약

한 번의 queue 성공으로 대기 업무와 책임이 사라지면 재시작 뒤 아무도
남은 일을 찾지 못한다. 이 계약은 DB의 WAITING과 stable obligation을
다시 발견하고, 두 실제 worker와 반복 crash가 중복 효과를 만들지 않는지
검사한다. 제품 실행은 아직 `NOT_RUN`이다.

| subcase | 실제 실행·관찰 |
|---|---|
| waiting-full-restart | Work create/activate/wait→queue ACK·삭제→WAITING/queue0 DB 관찰→api/scheduler/두 worker 각각 stop→clock 전진→각각 restart→자율 task terminal을 기다린다. 같은 의무의 escalation1·인간 owner·다음 행동/기한·목표 성공0을 확인한다. |
| two-workers-expired-fence | A의 실제 claim/transaction barrier ACK를 받은 뒤 heartbeat를 멈추고 TTL을 넘긴다. B의 takeover claim도 commit 전 barrier에서 관찰한다. B commit terminal 뒤 A를 재개하고 실제 stale terminal 거부·효과0을 확인한다. |
| repeated-crash-no-duplicate | commit 전 crash와 commit 뒤 ACK 전 crash를 각각 주입한다. 실제 process restart와 lease 만료 clock 뒤 같은 logical command를 재발견한다. 확정 명령/의무 transition 각각1·실물 중복0을 확인한다. |
| bounded-retry-exhausted-owner | 세 번의 transient DB 실패를 실제 task terminal FAILED로 관찰한다. versioned backoff와 controlled clock 뒤 소진 상태·의무1·현재 assignment1·인간 owner/다음 행동/기한을 확인한다. |

test profile은 tick1초·TTL5초·heartbeat1초·관찰30초·maxAttempts3·
backoff1/2/4초다. 실제 process의 wall-clock terminal 시간과 업무 clock을
구별한다.30초는 마지막 restart 완료에서 실제 task terminal까지의
관찰 제한이며 운영 SLA가 아니다. 모든 worker의 service credential은
원 actor의 현재 canonical grant를 대신하지 않는다.

takeover barrier에서 유효 claim은 scope당 하나다. terminal 뒤 claim은
RELEASED/EXPIRED 이력이 될 수 있어 ACTIVE를 계속 요구하지 않는다.
commit token과 실제 claim token은 독립 DB source 사이 `sameAs`로 연결한다.
token 숫자를 기대값으로 복사하지 않고 stale attempt의 거부·확정 효과0을
함께 검사한다. deliveryAttempt1→2, causal eventRevision1, definition-v1은
서로 다른 축이다.

실물20BOX는 queue·retry·알림으로 늘거나 줄지 않는다. DB raw source가
누락됐을 때0으로 간주하지 않는다. `scenario.feature`는 모든 action과
assertion을 각각 한국어 단계로 실행한다. 고정 JUnit 입력은 실제
`AssertionEngine`의 틀린 수량/단위·중복 실물·누락 owner·오염 version·
stale commit·30초 초과·허위 복원 완료 거부를 검사하며 제품 fake가 아니다.

4 subcase·91 assertion이 두 V5 oracle의 여덟 named observation을
연결한다. 상세 연결은 `oracle-bindings.json`이다. 실제 selector RED와
harness 결과는 `evidence/`에 기록하며 준비 PASS와 제품 인수를 구별한다.

최종 준비 검증은 `./verify validate` exit0과 `./verify harness`
149 PASS(이 폴더 관련 selftest19: mutant17·구조2, 실패/오류/skip0)다.
두 case의 실제 Gherkin selector RED는 발견/시작20·NOT_IMPLEMENTED
실패20·scenario skip0, exit1이다. 각 scenario의 첫 필수 assertion이
실패한 뒤 남은 assertion은 실행되지 않았다. 제품 recovery는 exit2
`NOT_RUN`이며 91개 assertion의 관찰 source가 미실행이다.
`evidence/authoring-summary.json`에 실제 입력 hash와 명령을 기록했다.

## barrier 표기(Step 2 재검토 2차)

worker barrier는 `pause-*` fault arm의 `testTransactionId`·
`testParticipantId`·`testBarrierId`·`testBarrierPoint`로 건다. barrier
control은 같은 고정 label을 쓴다. 이전 판은 barrierId·participantId·
transactionId를 claim 관찰 원행에서 읽어 control에 되돌려 넣었다.
그 값은 제품이 쓴 것이라 요청과 ACK의 exact 대조가 echo가 됐다.
worker는 API 요청으로 시작하지 않으므로 arming 위치만 request 대신
fault arm이다. 필드 의미는
[V2 경합 관찰 계약](../V2/race-observation-contract.md)과 같다.
