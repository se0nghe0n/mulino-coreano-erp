# T17 인도 판정의 evidence policy 입력 대조

Confirmed 인도의 물리 사실과 정당한 목표 기여를 구별한 채 정책 입력의
불일치를 교정한다. FactProvider의 policy exact match, 현재 권한,
원문 hash·가용성·supersession 검사는 변경하지 않는다.

## 범위와 기준선

Task baseline은 `a6e1197705351dbfb69f283c2922a28e4825906d`다.
제품 baseline `d0885a80`, condition 층, observer `fa316f01`, rollback
테스트 수정 `1f17`을 이어받은 별도 branch `fix/t17-goal-assessment`다.
작업 경로는
`/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-230af5b2`다.
사용자 Step 7의 수정·검증이며 S4/S5의 전체 인수 완료를 뜻하지 않는다.

`codex_2/gpt-6.1-sol`, `xhigh/priority`로 위임받은 구현 작업이다.
T3 catalog와 thread에서 provider/model/worktree를 확인했다.
Catalog의 model 기본 option 값은 이 실행의 tier 증거로 쓰지 않는다.
`ontology-implementation`, `ontology-scenario-testing` skill을 적용한다.

Recall worker의 commit·fixture·action은 가져오지 않았다. 해당 tree의
원본 `focused-result.json`, `formal-recall-domain-proof.json`,
`after-delivery-db.json`만 읽기 전용으로 대조했다. 원본 경로는
`/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-db3fa28c/.local/`
`t17-formal-recall-f747b281-b4be-464c-bfd2-6c24af9a7682/`다.
원본 assessment의 KNOWN 0 BOX, verified=false, admission refs 3개와
실물20/목표기여0/운송10은 이 작업의 새 실행 결과와 구별한다.

## 두 정책 계약과 교정

`AssessmentService.calculate`는 GoalVersion에 pin한 version과 일치하는
현재 `EVIDENCE` PolicyVersion이 정확히 하나인지 확인한다.
`AssessmentFactProvider`는 별도로 같은 version의 VERIFIED Verification을
요구하고 원문·Claim·Event·Inbox와 현재 admission 근거를 검사한다.

원래 goal은 `fixture-v1`이고 설치 convention의 EVIDENCE도 같은 version이다.
인도 SourceProfile과 실제 Verification은 `t17-synthetic-v1`이다.
따라서 물리 사실의 admission은 성공해도 일반 증거 verification을
찾지 못하는지 actual row와 immutable input snapshot으로 대조한다.
Goal만 `t17-synthetic-v1`로 바꾸면 현재 EVIDENCE 선택이 불가능하므로
그 음성 대조도 별도로 보존한다.

SourceProfile의 원천 identity 정책과 goal의 EVIDENCE 정책은 별도 계약이다.
새 alias `t17DeliveryAssessmentPolicy`는 `kind=EVIDENCE`,
`version=t17-synthetic-v1`, `content={}`인 독립 정책이다. 기존 convention의
내용 hash를 유지하고 `effectiveFrom=fixture.clock.asOf`를 명시한다.
합성 입력임과 운영 승인·원래 권한을 주장하지 않음을 기록한다.
SourceProfile·RegulatoryPolicy를 EVIDENCE 정책으로 재분류하지 않는다.

대상은 `complete-order-delivery-goal` action으로 기계적으로 센 다음
7개 fixture뿐이다.

- `normal-consumed-delivery`
- `late-restriction-actual-delivery`
- `post-dispatch-expiry`
- `claim-falseclaim`
- `claim-unidentifiedscope`
- `claim-conflictingquantity`
- `claim-scopeoverflow`

각 fixture는 새 alias와 `deliveryAssessmentPolicyAuthoring` metadata만
추가한다. 기존 alias·actor·grant·clock·condition·SourceProfile은 보존한다.
GoalVersion의 새 입력은 정책 version·독립 policy ID·출처 metadata만
바꾼다. 301 assertions와 원래 257 oracle·105 business status는 그대로다.

## 가역 층과 결합

`author_sales_contract_inputs.py --assessment-policy`가 별도 입력 층과
`delivery-assessment-policy-delta.json`을 만든다. Ledger는 바뀐 goal
필드의 before/after와 전체 goal hash, fixture의 정확한 alias/metadata,
이전 fixture hash를 보존한다. 변조된 goal이나 정책 입력의 inverse는
실패한다. 다른 writer의 추가 alias와 Work는 지우지 않는다.

전체 public generator는 condition 층 적용 뒤 policy case/fixture 층을
합성한다. Goal fixture inverse는 policy fixture inverse를 먼저 수행한다.
이 branch에는 recall 층이 없다. Root 통합의 역순은
`policy → recall → condition → sales → goal`이다. Forward는
`goal → sales → condition → recall → policy`로 합성한다.
Root가 recall inverse와 각 원래 assertion/fixture byte를 함께 대조한다.

## 검증과 미완료

실제 HTTP/PostgreSQL focused test는
`T17DeliveryAssessmentPolicyPostgresTest`다. Scenario runner를 호출하지
않고 공개 command/query와 실제 DB 원행·원문을 대조한다.
`./verify scenarios --actual`은 실행하지 않는다. Haiku native Workflow의
전체 301 assertions, recall 결합, 의무 current/sourceOccurrenceId와
전체 시스템 gate는 coordinator의 후속 인수다.

실행 경로·판정·초기 실패·cleanup의 최종 결과는 아래에 기록한다.

### 실제 결과

최종 fresh 실행은 다음 경로에 남겼다.

`.local/t17-source-pg-38188015-f4a9-4e4a-a1d0-809504a5bbd9/`

`test.log`와 `junit.xml`에서 9 tests, failures0, errors0, skipped0을
확인했다. `build-identity.json`은 자체 source commit·JAR hash·입력 hash를
보존한다. 실행 후 source custody 대조의 drift는0이다.

| 대조 | 물리 인도 / 정당 기여 | 실제 판정 |
|---|---|---|
| 기존 goal/current EVIDENCE `fixture-v1`, Verification `t17-synthetic-v1` | 20 / 0 BOX | UNVERIFIED, held=false |
| goal version만 교정, 해당 EVIDENCE 미설치 | 20 / 0 BOX | UNVERIFIED, held=true, EVIDENCE_POLICY_UNRESOLVED |
| 독립 EVIDENCE와 goal 함께 교정, 현재 제한 있음 | 20 / 0 BOX | UNSATISFIED, held=false |
| 같은20을 현재 제한 없이 확인, 목표30 | 20 / 20 BOX | UNSATISFIED, held=false |
| 정상 전량, 목표30 | 30 / 30 BOX | SATISFIED, held=false |
| inactive/future/expired EVIDENCE | 20 / 0 BOX | 세 경우 모두 UNVERIFIED, held=true |
| source identity review에 `fixture-v1` 오용 | 확인 효과0 | REJECTED/POLICY_UNRESOLVED |

Goal-only 대조는 실제 `recordDelivery`의 바깥 outcome이 `APPLIED`다.
물리 사실을 저장한 뒤 생성한 Assessment가 `held=true`이며 immutable
InputSnapshot의 `unknown=EVIDENCE_POLICY_UNRESOLVED`다. 이를 바깥 명령의
`HELD` 응답으로 바꿔 적지 않는다. 실제 응답·DB 원행·snapshot 원문은
`changingGoalAloneKeepsEvidencePolicyUnresolved-*`에 보존했다.

교정된 제한 인도의 `getAssessment`와 item `getObject`는 모두
UNSATISFIED다. DB에는 고객 실물20과 운송 잔량10이 남는다.
실제 GoalReferences·현재 ActivePolicies/PolicyVersions·SourceProfiles·
Verifications는 각 `*-proof.json`에 있다. 선택한 EVIDENCE는 정확히
하나이며, source policy와 별도 ID·kind를 가진다. 내용 hash와 유효
시작도 입력과 같다.

제한 인도 input은 `state=KNOWN`, `value=0`, `unit=BOX`, `verified=true`다.
Verification의 다섯 대조 flag와 verdict, 원문 blob SHA-256,
Event payload, Claim/Event/Inbox/Document chain을 직접 대조했다.
Input의 evidenceRefs에 실제 Verification·Document·Claim·Event ID가
들어간다. 정상 부분 인도는 같은 검증 경로로20을 읽는다. 0이나
verified 값을 테스트 adapter 또는 제품에 주입하지 않았다.

Capture의 `admissionPurpose=RECORD_DELIVERY`를 확인했다. Receiver에
assessGoal grant가 없고, 직접 assessGoal을 호출하면 FORBIDDEN이다.
그 전후 Assessment 원행과 판정 ID·inputSnapshotHash는 불변이다.
동일 policy version이더라도 현재 포인터·기간이 없으면 검증을
성공시키지 않는다.

### 재현과 보존

실제 focused 실행 command는 다음과 같다.

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/opt/openjdk@21/bin:$PWD/backend/node_modules/.bin:$PATH \
DOCKER_HOST=unix:///Users/se0nghe0n/.orbstack/run/docker.sock \
JAVA_TOOL_OPTIONS='-Dlogging.level.com.sap.cds=ERROR -Djunit.jupiter.execution.parallel.enabled=true -Djunit.jupiter.execution.parallel.mode.default=concurrent -Djunit.jupiter.execution.parallel.config.strategy=fixed -Djunit.jupiter.execution.parallel.config.fixed.parallelism=3' \
T17_PG_TEST_CLASS=T17DeliveryAssessmentPolicyPostgresTest \
python3 verification/cases/T17/run_source_pg_test.py
```

자체 `npm ci`로 CDS 의존성을 설치하고 자체 JDK21·Maven으로 package했다.
다른 tree의 compiled artifact를 복사하지 않았다. 실제 JDK는
Homebrew21.0.12.1, Maven3.9.16, CAP5.1.1, Spring Boot4.1.1,
PostgreSQL18.6의 고정 image digest다. 실제 Node26.9.0과 npm11.19.1을
사용했으며 package의 Node24 engine 경고를 보존했다. 과거 S0 Node24
실행과 같은 환경이라고 표시하지 않는다.

추가 결과는 `.local/t17-delivery-assessment-policy-checks/`에 남겼다.

- Python 전체 T17 계약40 tests 성공, 원래301 assertions 불변이다.
- public generator 재실행은 출력 byte drift0이다.
- `request_contract.py --check`와 `git diff --check`는 exit0이다.
- 기존 causal guard23, source authority26, presentation2 tests가
  성공했다. 이51개는 Unit 결과다.
- 기존 EvidenceReconciliation13, EvidencePersistence15 tests가
  실제 PostgreSQL에서 성공했다. 원문 hash·supersession·중복·권한
  경계를 검증하는28개 회귀다.
- `preservation.json`은15개 fixture의 before/after/inverse hash와
  exact7개 변경을 보존한다. 모든 inverse byte가 baseline과 같다.
- `final-source-custody.json`의 모든 입력 hash가 실행 당시와 같다.

첫 Python 실행의6개 실패는 새 층을 제거하기 전 fixture bytes를
과거 baseline과 대조한 테스트였다. 기존 검증은 정확한 새 inverse를
먼저 적용하도록 합성하고 원래 oracle는 바꾸지 않았다.
첫 HTTP/PG 실행은 provisional 접수의 실제
ACCEPTED_PENDING_EXTERNAL을 새 focused test가 APPLIED로 잘못 기대한
실패8개와 goal-only copy의 미설치 alias 참조 오류1개였다.
새 test를 공개 계약에 맞춰 고쳤다. 두 번째 실행은8개 성공과 같은
alias 참조 오류1개였고, 최종 fresh 실행에서9개가 모두 성공했다.
첫 실행 경로는 `.local/t17-source-pg-`
`d7456cbc-11d1-4e73-b07f-44777e2b4839/`, 두 번째는
`.local/t17-source-pg-89fe0328-4b4a-4e77-adc9-f88ddca050f1/`다.
이 실패 원문과 JUnit 결과를 삭제하거나 성공으로 바꾸지 않았다.

추가 evidence 회귀의 첫 실행은 JWT_PUBLIC_KEY 누락으로28개가
ApplicationContext 환경 실패였다. 임시 RSA 공개키를 갖춰 같은28개를
재실행했고 성공했다. 실패 로그도 보존했다.

세 focused runner의 `cleanup.json`은 containerAbsent/serverStopped/
privateKeyAbsent/publicKeyAbsent/blobRootAbsent가 모두 true다.
추가 회귀의 임시 인증키도 제거했다. 각 runner의 commands·Spring·DB
로그와 build identity는 보존했다. 다른 writer의 실행·컨테이너·tree는
정리하지 않았다. 이 worker branch는 coordinator의 통합을 위해 남긴다.

이 결과는 한정된 focused 검증이다. Coverage receipt나 전체 시나리오
PASS를 만들지 않았고, Haiku native Workflow의 전체 T17, recall 합성,
나머지 의무 실패와 S4–S6/실모델/규제/BTP gate는 남아 있다.
