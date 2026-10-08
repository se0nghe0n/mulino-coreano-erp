# T26 지속 복구·운영·복원 인수 계약

queue 전달 성공은 업무 원장이 아니다. 이 계약은 실제 명령 뒤 발생한
장애를 DB에서 다시 찾고, 기술 재시도·현재 인가·물량·인간 책임을
각각 확인한다. 복구 완료 상태를 fixture에 넣지 않는다.

기준선은 `step2-b2`=`feaca0af9673620eff9a5ac0f08a657ce14e9ccd`다.
Step 2는 제품 구현 전 계약 작성이다. 실제 제품 API·PostgreSQL·worker·
host 복원 adapter는 `NOT_RUN`이다. 고정 JUnit 표본은 실제
`AssertionEngine`을 검사하며 제품 worker나 상태 전이를 구현하지 않는다.

| subcase | 실제로 만드는 장애와 관찰 |
|---|---|
| due-wait-db-rediscovery | create/activate/wait 뒤 queue message가 0임을 확인한다(별도 purge action은 없다). 전체 process를 중지하고 clock을 전진시킨 뒤 재시작 후에도 queue0인 상태에서 DB due index로 같은 의무를 찾는다. |
| orphan-intake-recovered | 역사적 종료 부모의 새 온도 이상을 RECORD한다. link commit fault 뒤 접수 책임을 확인하고 실제 자율 재시도에서 업무·의무 각1을 연결한다. |
| outbox-exhaustion-alert-dedupe | 구매 proposal·MANAGER 승인·발주 전달을 실행한다. 세 번의 확인된 retryable 실패를 backoff clock으로 소진시키고 알림 성공 뒤에도 의무 OPEN과 owner를 확인한다. |
| operational-nine-categories | 명령·명시적 복구 손상 drill로 아홉 종류의 문제를 만든다. API 문제 ID·stateVersion·owner·다음 행동과 해당 DB 원 행을 대조한다. |
| safe-retry-canonical-current-grant | move commit fault로 효과0을 만든다. OPERATIONS retry는 원 commandId와 사유만 보낸다. 서버가 저장 command에서 읽은 원 actor·canonical hash/key·현재 grant·claim fence와 실제 이동20 한 번을 확인한다. |
| safe-retry-revoked-blocked | 같은 rollback 뒤 grant를 철회하고 재시작한다. retry의 FORBIDDEN·허용 denial audit와 금지된 물량 효과0·남은 인간 책임을 구별한다. |
| safe-retry-forged-original-actor | warehouse grant 철회 뒤 OPERATIONS가 이동 권한이 남은 warehouseLead를 payload의 originalActorId로 넣는다. 재시도는 REJECTED이고 실물·계보·배분 효과와 COMMITTED retry는0이다. |
| safe-retry-forged-request-hash | grant가 유효해도 다른 payload의 canonicalRequestHash를 함께 보내면 REJECTED, 이동0, 저장 hash 불변이다. |
| unknown-external-reconcile-before-retry | 상대 문서 commit 뒤 응답을 버린다. status 조회도 unavailable로 둔다. 대조 전 retry 거부, 담당 대조 성공 뒤 외부 전달1 유지와 로컬 연결을 확인한다. |
| lot-expiry-no-event | LOT 만료20의 boundary를 기록한다. Work 활성화·예약 후 clock을 전진한다. 출고나 query 전에 sweep terminal snapshot의 독립 DB에서 사건·판정·후속 의무를 확인하고 반복 sweep 뒤 같은 원 행을 비교한다. |
| disposition-expiry-no-event | 처분 허용의 만료를 같은 순서로 확인한다. |
| grant-expiry-no-event | warehouse grant 만료를 같은 순서로 확인한다. |
| policy-expiry-no-event | 현재 정책 만료를 같은 순서로 확인한다. |
| planned-transition-reindexes | 과거 승인·발행된 v2 artifact를 실제 activate 명령으로 전환한다. 영향 scope 경계 갱신과 기존 Work v1 의미 보존을 확인한다. |
| emergency-repair-dryrun-apply | 실제 projection 손상 drill 뒤 마지막 확정 명령·scope를 찾는다. dry-run diff·조회 주체 apply 거부·인가된 apply·근거/audit·원장 재대조를 수행한다. |
| restore-complete | 실제 split8+12·증거 연결·Work 대기 뒤 DB/blob/정의/capability/evaluator/skills/config/배포 bundle을 백업하고 새 환경에 복원한다. |
| restore-missing-blob | 백업 뒤 복원 reader의 원문 part 읽기 실패를 주입한다. DB 기록이 있어도 원문 가용성 MISSING·복원 INCOMPLETE를 확인한다. |
| restore-missing-v1-evaluator | 백업 뒤 evaluator v1 part의 복원 읽기 실패를 주입한다. 과거 v1 판정은 남지만 복원 완료나 신규 실행 허용으로 표시하지 않는다. |

## 관찰과 손계산

fixture의 실물은 구별된20BOX 하나다. 임의 wildcard·자동 승인이나
현재 복구 결과를 seed하지 않는다. 실제 source document bytes와 hash는
`fixtures/artifacts/`에 있으며 개별 fixture의 `baseline.fixtureArtifacts`가
그 path/hash/bytes를 고정한다. v2의 과거 발행·승인은 boundary 전환의
baseline이며 실제 activation 효과를 대신하지 않는다.

만료·출고 거부·권한 철회는 보유20을 감소시키지 않는다. guard가
SUSPENDED·대조 의무를 기록할 수 있으므로 모든 DB 행의 불변을 요구하지
않는다. 전후 실물·계보·이동은 같고 배분 CONSUMED0, 원 배분 ID/수량은
유지돼야 한다. sweep 뒤 실행 배분0·미해결 의무1·현재 인간 assignment1을
검사한다. split 후 active8+12=20이고 retired 부모20을 더하지 않는다.

각 `observe`는 명시된 organization·case/subcase·추가 대상 범위와 실제
API snapshot token 또는 자율 task terminal snapshot token을 사용한다. `sources`의 모든 원 행은 read-only
query/parameters/mapping version·source artifact·완전성·snapshot으로
연결한다. adapter가 API projection을 복사하거나 빈 결과를 만들어서는
안 된다. `movements`, `dutyTransitions`, `externalDeliveries` 등은 새 S0
persistence 선택 후 실제 저장소의 대응 행을 읽는 논리 source 이름이다.

process start/stop/restart는 api/scheduler/worker-a/worker-b 각각의 실제
instance와 terminal을 관찰한다. `tickScheduler`는 실제 자율 task ID/handle을
반환하고 `awaitRuntimeTask`는 같은 handle의 terminal 및 완료 이후
snapshot을 기다린다. `resumeWork`나 fake worker로 대신하지 않는다.
backup/restore 뒤에는 생성된 actual output descriptor를 strict result
ref로 `inspectArtifacts`에 전달한다. 실제 secret은 bundle에 넣지 않고
별도 secret manager 경로를 관찰한다.

## 추적과 실행 상태

20 subcase와558 assertion이 세 T26 oracle의 여섯 named observation을
연결한다. 상세 연결은 `oracle-bindings.json`이다. 이는 선언 추적이며
실행 coverage를 증명하지 않는다. 실행 명령·exit·검증 수·fixture hash는
`evidence/`에 별도 기록한다. 환경/형식 오류 exit3은 의도된 RED가 아니다.

수정 전 baseline에 기록된 준비 검증은 `./verify validate` exit0과 `./verify harness`
149 PASS(이 폴더 관련 selftest19: mutant17·구조2, 실패/오류/skip0)다.
당시 두 case의 실제 Gherkin selector RED는 발견/시작20·NOT_IMPLEMENTED
실패20·scenario skip0, exit1이다. 각 scenario의 첫 필수 assertion이
실패한 뒤 남은 assertion은 실행되지 않았다. 제품 recovery는 exit2
`NOT_RUN`이며 297개 assertion의 관찰 source가 미실행이다.
`evidence/authoring-summary.json`에 실제 입력 hash와 명령을 기록했다.

## Step2 review 수정

네 `*-expiry-delayed-guard`는 자동 sweep 없이 중지 상태에서 출고를
거부하는 별도 fixture다. 기존 `*-expiry-no-event`는 sweep 직후 독립
DB를 먼저 관찰한다. 활성화한 Work의 현재 assessment는
`causeKind=VALIDITY_EXPIRED`, `asOf`로 만료 재평가를 식별하며 원래
확정 증거가 없으므로 `UNVERIFIED`를 유지한다. 내부 만료 사건은
`events.kind=VALIDITY_EXPIRED`, 만료 후속 의무와 assignment는
`sourceKind=VALIDITY_EXPIRED`로 최초 활성화 책임과 구별한다. 이는
read-only logical source mapping 계약이며 제품 실행 증거가 아니다.

`dryrun-db`와 `denied-db`는 인가된 APPLY 전에 projection 전체 원 행을
`before-db`와 비교한다. 대상 ID·sourceRevision0·비어 있지 않은
projection ID를 확인해 빈 배열 비교나 조기 repair로 통과하지 못한다.
Host task ID/handle은 typed `operationEvidence`에서 읽으며 claims,
backup, restore 원 행은 `extractor/rawRows`에서 읽는다.

이번 review 수정의 검증은 `evidence/review-case-contracts/checks.json`에
기록했다. 당시 이름인 `CaseContractRoutesSelftest`7개와 기존 runtime
selftest19개가
실패·오류·skip0으로 PASS했고 세 case schema가 유효하다. 명시적
Gherkin RED는 T26/V5/T14 합계28개를 모두 발견·시작했고
NOT_IMPLEMENTED assertion 실패28·scenario skip0·exit1을 관찰했다.
고정 payload의 schema·참조·assertion 검사이며 실제 제품 인수는
NOT_RUN이다. 과거 evidence는 수정 전 snapshot으로 보존했다.

자동 만료의 `sweep-db`와 `repeat-db`는 출고 이전 snapshot에서 OPEN
의무·현재 OPEN assignment 각1과 owner·supervisor·nextAction·nextCheckAt을
검사한다. assignment의 obligationId·rootId·workId를 같은 snapshot의
만료 의무 ID·rootId·responsibleWorkId와 대조하고 반복 sweep 뒤 같은
assignment 원 행을 유지해야 한다. 이후 출고가 책임 필드를 복구해도
자동 sweep의 누락은 통과하지 못한다.

A8 후속 검증은 `evidence/review-autonomous-responsibility/checks.json`에
기록했다. 새 책임 mutant selftest1개가 네 만료 profile과 출고 전 두
snapshot의 반례180개를 거부했고 실패·오류·skip0으로 PASS했다.
Cucumber CLI의 `--name`으로 자동 만료 Gherkin4개만 실행해
NOT_IMPLEMENTED 실패4·undefined0·scenario skip0·exit1을 관찰했다.
뒤 assertion step의 skip은 첫 미지원 제품 assertion 이후의 중단이며
제품·DB·worker·모델 실행 인수는 NOT_RUN이다.

기본 Maven Surefire discovery를 위해 현재 클래스·파일 이름은
`CaseContractRoutesSelfTest`다. 이름 변경은 assertion 동작을 바꾸지
않으며 의도한 RED test의 exclusion도 유지한다. 현재 focused 실행은
`./mvnw -B -ntp -f verification/harness/pom.xml
-Dtest=CaseContractRoutesSelfTest test`로 8개 SELFTEST를 실행한다.
과거 evidence의 실행 command·파일 hash는 당시 값으로 보존한다.

## 재검토 수정(2026-10-08)

`author_review_fixes.py`가 이 절의 subcase·assertion과 Gherkin,
`oracle-bindings.json`을 다시 만든다. 같은 입력에서 반복 실행해도
결과가 같다.

- 모든 `retrySafeCommand` 요청은 C3와 같은 모양
  `slots={commandId, reason}`만 보낸다. originalActorId·
  canonicalRequestHash·원 key를 호출자가 주지 않으므로 저장 command의
  actor/hash 단언은 payload를 되읽는 값이 아니다(plan §3.3, §7.2).
- 자율 loop 세 subcase는 harness가 process 재시작과 가상 clock 전진만
  한다. `tickScheduler`/`sweepDue`는 `trigger=OBSERVE_NEXT_NATURAL_TICK`,
  `clockInstant` 없음, 관찰창30초로 다음 자연 tick의 제출만 관찰한다.
  `triggeredBy=SCHEDULER_LOOP`, scheduler RUNNING 뒤30초 안의 제출
  (`timeAtMostSeconds`), 독립 DB attempt의 triggeredBy를 함께 본다.
  tick hook만 있고 loop가 없는 구현은 통과할 수 없다(plan §10).

이 관찰 의미(observe-only tick)는 host 관찰 계약의 operation 표에 아직
없다. 공통 harness 소유자가 `host-observation-guide.md`와
`HostObservationValidator`에 반영해야 실제 adapter가 같은 의미로
구현된다. 운영 profile이 scheduler loop를 실제로 띄우는지의 배포 검사도
남은 범위다.
