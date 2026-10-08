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

## 재검토 수정(2026-10-08)

`author_prerequisites.py`가 case.json 전체, 한국어 feature,
`observation-bindings.json`을 결정적으로 다시 만든다. 반복 실행 결과가
같고 내장 self-check가 아래 구조를 강제한다.

- 공유 20개 원천 밖의 주 효과 원천을 capability별로 고정했다
  (`CAP_EFFECTS`, plan §4.1·§6 entity와 backend/db CDS 대응).
  `effect-before`/`effect-after`는 `includeDescendants`에 기대지 않는
  조직 전체 scope로 그 원천을 관찰하고 거부 전후 exact 비교한다.
  reader 자신의 attempt command가 COMMITTED가 아님도 확인한다.
- 정상 counter-call은 같은 payload와 revision을 쓰되 별도 key
  `C3-<route>-<cap>-authorized`를 쓴다. 다른 주체의 같은 key는 plan
  §7.3상 독립 namespace나 거부 둘 다 허용되므로 인가 증명에 섞지 않는다.
- 회수 승인·통지·회수·종료는 실제 선행 사슬을 만든다. 종료는 ADMIN
  승인→통지→회수20→같은 실물 폐기20 뒤 admin이 partitionHash와 종료
  근거로 닫고 종료 행의 처리20·미확인0·예외0을 확인한다. 긴급 재배정은
  acceptHandover payload가 아니라 admin의 업무·의무·새 담당·사유와
  감사 행을 쓴다.
- 선행/정상 업무 assertion은 read-audit-permitted가 아니라 검증하는
  효과 종류(approval·work·followup·inventory·outbox)에 연결한다.
- 모델 조회30건은 client가 받은 tools/list 전체(쓰기 도구 포함), reader의
  실제 QUERY tools/call 한 번 이상, 답에 필요한 조회(5개 문장은
  getInventory·getObligations·traceLot), reader 조회 감사를 요구한다.
  도구가 없는 client나 아무 도구도 부르지 않은 응답은 통과하지 못한다.
- profiles에 catalog requiredLayers의 SKILLS에 대응하는 `skills`를 추가했다.

V4는 같은 20개 원천과 같은 key 재사용을 쓰지만 이 worker의 소유가
아니어서 고치지 않았다.

## 2라운드 profile 도달성 수정(2026-10-08)

profile 선언만으로는 SKILLS·MCP 계층을 실제로 지났는지 알 수 없다.
`model-query-write-boundary`의 세 관찰은 이제 각자 그 계층을 읽는
assertion에 연결된다.

- `skill-stage-discovered`, `skill-stage-body-read`: 실제 host의
  `skillLoadingTrace`에 `stage`가 `DISCOVERED`·`BODY_READ`인 행이 각각
  한 건 이상 있고 `packageName`·`stage`·`path`·`sha256`·`loadedAt`을
  가진다. 세 관찰 모두에 연결한다. stage 어휘는 T20 host loading과
  같다(계획 §9.2 metadata→본문→참고 자료). 어느 package를 읽을지는
  모델이 정하므로 package 이름은 고정하지 않는다.
- `no-write-intent-command`, `no-write-intent-record`: MCP tools/call
  transcript에서 COMMAND·RECORD 의도 호출이 0건이다.
  `query-intent-write-tool-execution`과 `query-intent-business-effects`
  에 연결해 DB 전후 불변과 protocol 경계를 함께 본다.
- 기존 `skill-loaded-files`는 그대로 둔다. hash 목록만으로는 loading을
  증명하지 못하므로 stage 행이 추가 조건이다.

skill을 읽지 않고 답한 실제 client는 이 subcase를 통과하지 못한다.
SKILLS 계층이 catalog의 필수 계층이기 때문이다. 실제 client 실행은
NOT_RUN이다.
