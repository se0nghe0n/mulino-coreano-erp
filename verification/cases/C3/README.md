# 역할과 현재 grant 교집합의 테스트 계약

93개 COMMAND/RECORD capability를 API·MCP·worker에서 각각 호출한다.
일반 역할은 지정 capability 목록을 갖지만 현재 grant는 QUERY만
허용한다. capability마다 필수 입력과 단계에 맞는 prior state를
별도 fixture에 보존한다. wildcard 권한이나 묵시적 승인은 없다.

각 거부 직후 before/after 원행을 캡처한다. 동일 fixture에서 현재
권한이 있는 delegator가 같은 입력·route를 수행하는 counter-call과
별도 authorized-after command COMMITTED 원행도 검사한다. denied
입력이 TYPE_INVALID/상태 guard여서 쓰기가 우연히0인 경우를
인가 성공으로 오인하지 않는다. alias 재설치로 ID를 바꾸지 않는다.

Work·Goal·물량·원장·배분·승인·후속·outbox·권한·정의·정책·관계
원행을 전후 비교한다. 읽기/거부 감사와 rejected command record는
허용된 별도 기록이다. read audit는 실제 인증 주체/행동/결과에
연결되며 업무 변경으로 세지 않는다.

실제 조회 문장10개×3회는 actual client port에 원문과 허용 문맥만
보낸다. 실제 MCP transcript의 capability별 쓰기 호출0과 독립 DB의
업무 효과0, skill loading/usage/version artifact를 검사한다.
서버가 쓰기를 거부해도 모델이 쓰기 도구를 호출하면 실패다.
R8 비용·client/model 확정과 실제 지원 host가 없으므로 MODEL은
NOT_RUN이다. scripted runner로 의미 성능을 대체하지 않는다.

모든 fixture는 가상값이다. Step2의 parser·assertion·RED 준비와 실제
제품 DB/API/MCP/경합/host/모델 인수를 구별한다. 제품 인수는 NOT_RUN이다.

작성 검증의 실제 명령·exit·scenario 수·파일 hash는
`verification/cases/T08/evidence/authority-suite/checks.json`에 있다.
`observation-bindings.json`은 이 case의 모든 catalog observation을
구체 subcase/action/assertion 및 JSON pointer에 연결한다.
고정 수량 oracle의 primary와 보조 관계/assertion을 함께 보존한다.
이 연결은 작성 증거이며 실제 제품 효과를 관측한 결과가 아니다.

발주·정의 전환·의무 이전의 정상 counter-call은 업무 전제를 별도로
충족한다. 발주는 MANAGER의 실제 approvePurchase 결과를 먼저 얻고
그 approval/hash와 같은 externalOperationId의 outbox 한 행을 대조한다.
정의 전환은 FDE의 검증·발행과 CONFIG_APPROVER의 발행/전환 승인을
먼저 수행하고 v2 업무·전환 기록·기존 목표 이력을 확인한다. 의무 이전은
WORK2에서 WORK로20BOX를 맡겠다는 warehouse의 identity·revision·scope·
유효기간이 결합된 가상 prior acceptance 원문을 사용한다. 일반 DOC나
발신자의 acceptingOwnerId 주장만으로 수신자 수락을 대체하지 않는다.

각 API/MCP/worker 거부와 정상 호출의 request는 같은 payload와 revision을
사용한다. 승인·수락·전환 guard가 없는 입력의 실패를 인가 증거로 세지
않는다. `author_prerequisites.py`는 이9개 사례의 선언을 재작성하며 실제
제품 행동을 실행하지 않는다. `AuthorityPrerequisiteAssertionsTest`의
mutation 검사는 관찰 assertion의 작성 검증이며 제품 인수는 NOT_RUN이다.

수정 후18개 JUnit PASS와9개 의미 RED, skip0의 실행 기록과 검증한
source/fixture hash는 `evidence/review-authority/checks.json`에 남겼다.
