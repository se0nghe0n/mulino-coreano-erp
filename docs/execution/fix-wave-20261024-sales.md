# T17 sales producer 수정 기록

기준선은 `b4772eb6`이며 product source의 직전 변경은 `f8bb11e9`다.
작업 branch는 `fix/t17-eligibility-contract`다. S5/S6와 전체 Task/Step은
OPEN이다. coordinator만 통합·push한다. 이 파일은 producer 인계 기록이다.
runtime은 codex_2 / gpt-6.1-sol / xhigh / serviceTier=priority다.

## observer 인계 계약

### T11 writer와 causal 재평가 계약

현재 사용자의 후속 지시에 따라 아래 계약으로 갱신한다. 기존
`WorkAssessmentGuard.assessCreated(c,workId)`를 사용하며 상호 tree의
미commit 코드를 가져오지 않는다.

- sales handler는 실제 canonical delivery를 반영하고 의무/기여를 연결한
  같은 명령 transaction에서 ACTIVE/WAITING, lifecycleMode=COMMAND,
  kind=DELIVERY인 affected Work에 assessCreated를 호출한다.
  public assessGoal grant를 요구하거나 fixture에 추가하지 않는다.
- T11의 assessCreated는 CommandExecution.commandId의 persisted
  IN_PROGRESS CommandRecord와 actor/stableRequestOwner/org, canonical
  intent hash, RECORD capability/confirmation slots를 검증한다.
  해당 명령의 실제 DeliveryTransfers.commandId/canonicalId와 적용된
  sales Delivery/confirmed Observation의 affected Work가 일치해야 한다.
  생성 호출(createWork)의 기존 동작은 유지한다.
- sales가 추가하는 server-only Purpose enum은
  RECORD_DELIVERY(recordDelivery),
  RECORD_OBSERVED_MOVEMENT(recordObservedMovement)다. T11은
  `Purpose.values()`에서 검증한 capability로 찾아 compile 독립성을
  유지할 수 있다. 둘을 client input으로 받지 않는다.
- T11은 실제 record cause가 있을 때 그 purpose를 calculate/provider와
  captureBoundary.admissionPurpose에 전달한다. source 불허/미확인은
  현재 근거의 UNVERIFIED/pending으로 남기고 SATISFIED로 만들지 않는다.
- sales DeliveryAdmission은 같은 현재 RECORD 명령에 묶인 witness로
  requested/source Work, item/place/target과 canonical·claim·event·원본
  document의 SOURCE scope를 매번 all-of로 검사한다. public evidence
  read를 대신 발급하지 않고 기존 canonical integrity 검사를 재사용한다.
- 이 tree는 기존 AssessmentService 때문에 합쳐진 causal full path를
  아직 실행할 수 없다. witness/admission의 독립 focused 검증을 남기고
  결합 후 권한 없는 receiver의 실제 재평가를 T11/coordinator가 인수한다.

구현된 독립 witness는 `DeliveryRecordCause`다. SalesRepository의
currentRecordCause는 IN_PROGRESS 명령의 실제 identity/hash/confirmation
slot, CONFIRMED observation와 하나의 Delivery, 같은 commandId와 canonicalId의
DeliveryTransfer, COMMAND/DELIVERY/ACTIVE 또는 WAITING Work를 재검증한다.
CanonicalEvidenceVerification.verifiedForDeliveryRecord는 기존 strict
canonical graph 검사를 재사용하고 각 claim/event/document에 같은 RECORD
grant를 검사한다. handler 반환 전에 track한 source scope를 다시 확인한다.
recordDelivery preparation에도 실제 event.sourceProfileId의 SOURCE를
추가해 SOURCE가 있는 all-of grant를 처음부터 대조한다.

T11 tree의 새 CausalAssessmentGuard를 읽었다. causal 선택은 현재 Work의
kind=DELIVERY/lifecycleMode=COMMAND, 현재 goal.endpoint=DELIVERED와
PHYSICAL_DELIVERY selector도 한정해야 한다. 다른 provider가
purpose.capability만 보고 RECEIPT 같은 사실을 RECORD_DELIVERY로
admit하지 않게 record 목적의 입력은 PHYSICAL_DELIVERY에 한정한다.
sales witness는 PHYSICAL_DELIVERY 이외 canonical을 항상 거부한다.

- `evaluateEligibility.data.segments[]`와
  `getInventory.data.segmentEligibility[]`는 같은 현재 evaluator의 실제
  per-segment 결과를 반환한다. `conditionResults[]`는 각각
  `segmentId`, `conditionId`, `status`, `conflict`, `evidenceRefs`,
  `evidenceId`, `evaluatorVersion`과 계산에 사용한 range를 가진다.
  최상위 `conditionResults[]`는 이 목록을 펼친 것이다.
- 실제 evaluator ID는 `quality-eligibility-v1`다. fixture의 과거
  `evaluator-v1` 문자열을 구현 버전인 것처럼 복사하지 않는다.
  공개 semantic 조건은 QC/REGULATORY/LABEL/DISPOSITION/CUSTOMER_CONTRACT다.
  raw `conditions[]`는 POLICY/LOT_VALIDITY/CUSTODY와 permission·restriction을
  별도로 보존한다. 원 fixture의 `agency-proof`를 새 원본 증거 ID로 만들지 않는다.
- 단일 근거일 때만 `evidenceId`를 반환한다. 여러 근거는
  `evidenceRefs[]`이고 근거 미확인은 빈 목록과 null이다. 규제 근거는
  현재 regulatory evaluator가 인정한 canonical occurrence다.
- `unreservedEligibleQuantity`, LOT의 `expiresAt`, 실제 적격·미예약
  후보 중 FEFO의 `preferredSegmentId`를 공개한다. 이 조회는 미래
  reserve/dispatch 권한을 만들지 않는다.
- `getObject/getInventory/getWork`의 공통 world에
  `deliveryObservations[]`를 공개한다. 이것은 sales Observations 원행의
  인가·시점 대조 결과다. item의 quality/regulatory Claims 전체와
  구별한다. `canonical=false`, `status=PENDING_RECONCILIATION`은
  실제 PROVISIONAL 상태이며 사실을 생성하지 않는다. `eventId`,
  `claimedRangeRootId`, `rangeRootId`, `claimPayloadJson`, 수량·단위·시간·
  Work·출고 연결을 보존한다. `saleExecutionAuthorized=false`는 이
  RECORD가 미래 실행을 허가하지 않는다는 의미다.
- `warehouseHeldQuantity`는 조회 scope 안의 active INTERNAL_STORAGE
  실물량이다. 기존 `heldQuantity`는 전체 선택 scope의 물량이다.
  W40 assertion은 query에 `placeId=W`를 명시하는 방향으로 수정한다.
- `deliveryAssessments[]`와 단일 목표의 `deliveryAssessment`는 저장된
  현재 GoalVersion의 실제 immutable assessment만 읽는다. pending이나
  판정 부재는 gap으로 남기고 물리 인도량에서 SATISFIED를 추론하지
  않는다. actual delivery 반영 뒤 활성 DELIVERY Work는 위의 현재
  RECORD 명령에 묶인 server-only purpose로 기존 evaluator를 호출한다.
  public assessGoal이나 evidence read 권한을 발급하지 않는다.

### 원래 조건의 대응

| 원래 semantic 조건 | 실제 producer의 계산과 출처 |
|---|---|
| QC | QC DispositionBases의 permission과 같은 범위 QC Restrictions를 각각 반환한다. 전체 QC hold는 permission ALLOWED여도 semantic QC DENIED다 |
| REGULATORY | RegulatoryEligibility가 인정한 현행 DecisionVersions의 허용/거부 interval과 원본 canonical occurrence다. label 때문에 규제 판단 자체를 덮지 않는다 |
| LABEL | 같은 RegulatoryEligibility가 현재 policy.requiresLabel, 현재 procedure version의 최신 LabelVerifications, 포장/규격 version 일치, 유효기간, canonical 원본 대조를 함께 계산한다 |
| DISPOSITION | COMMERCIAL permission과 해당 category 제한이다. 소유·위치나 앱 권한을 처분 근거로 쓰지 않는다 |
| CUSTOMER_CONTRACT | 해당 customer의 CUSTOMER permission과 해당 category 제한이다 |

LABEL이 필요 없을 때만 실제 policy ID를 근거로 required=false와
NOT_REQUIRED_BY_POLICY를 반환한다. 현행 정책을 못 찾으면 LABEL도
UNKNOWN이다. LABEL 원본이 없거나 유효성을 확인하지 못하면 label
ID·결정·원본 ID를 공개 근거처럼 반환하지 않는다. 규제 허용과 LABEL은
독립 설명이므로 LABEL 원본이 있어도 규제 허용이 없으면 판매 가능량은
0이다. raw 조건과 semantic 조건을 함께 보존한다.

### coordinator가 변환할 10개 조건 assertion

아래 ID의 source/expected는 이 commit에서 교정하지 않았다. 원래
다섯 semantic 조건, 각 segment의 ALLOWED/UNKNOWN/CONFLICT/DENIED
의미와 독립 원천 대조를 모두 유지해야 한다.

- `T17/eligibility-unknown/eligibility-response-1`
- `T17/eligibility-unknown/eligibility-response-2`
- `T17/eligibility-conflict/eligibility-response-1`
- `T17/eligibility-conflict/eligibility-response-2`
- `T17/eligibility-fefo-no-reason/eligibility-response-1`
- `T17/eligibility-fefo-no-reason/eligibility-response-2`
- `T17/eligibility-fefo-with-reason/eligibility-response-1`
- `T17/eligibility-fefo-with-reason/eligibility-response-2`
- `T17/eligibility-packaging-unapproved/eligibility-response-1`
- `T17/eligibility-packaging-unapproved/eligibility-response-2`

현재 original response-1은 rawRows.conditions의
segmentId/conditionId/status/evidenceId/evaluatorVersion을 fixture alias인
qc-proof/agency-proof와 evaluator-v1로 대조한다. response-2는 공개
conditionResults를 같은 rawRows.conditions와 대조한다. observer의
`@conditionResults`는 실제 Work AssessmentReferences를 읽으므로 현재
eligibility evaluator의 조건과 대응하지 않는다. 위 alias 두 개는
공개 producer가 생성한 permission document/규제 canonical proof ID가
아니며 LABEL·DISPOSITION·CUSTOMER_CONTRACT가 agency-proof 하나를
공유한다고 주장할 근거가 없다. 예상값을 현재 관측에 복사하지 않았다.

실제 fixture 설치 artifact의 정확한 receipt 경로는
`/sourceFactEvidence/conditions`다. 배열 index는 skip된 조건 때문에
원래 index와 다르므로 각 receipt의 `/pointer` 값
(`/baseline/conditions/N`)과 `/given/segmentAlias`, `/given/conditionId`로
결합해야 한다. 각 receipt는 `/documentId`, `/eventId`, `/reviewId`,
`/canonicalOccurrenceId`, `/typedDecisionId`를 가지며 규제 결과는
`/procedure/procedureId`, `/procedure/procedureVersionId`를 가진다.
QC/DISPOSITION/CUSTOMER_CONTRACT의 permission 근거는 documentId다.
REGULATORY/LABEL의 원본 근거는 canonicalOccurrenceId다. 실제 hold
근거는 현재 Restrictions.evidenceRef와 분리해 결합한다. LABEL이 정책상
불필요하면 현재 Policies.ID를 basis로 사용하며, 필요하면 현재 label의
원본·procedure version·packaging/specification을 모두 대조한다.

원본 T17-S01의 setup artifact 예시는 다음 read-only 파일이다.
`verification/harness/target/evidence/actual/` 아래
`28b9e3c1-c598-4fc9-8d6f-13d5f1719376/`
`c2b2b353-0427-4df2-b585-90e3a0857b48.json`이다.
frozen root는
`/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/`
`scenario-semantic-f8bb11e9-20261024/T17-S01`이다. 이 원행에서
QC의 given.evidenceAlias=qc-proof와 새 documentId/canonicalOccurrenceId가
서로 별개임을 확인했다. 설치 사실을 의미별로 검증하며 alias 문자열을
증거 ID로 복제하지 않는다.

REGULATORY CONFLICT는 실제 두 원본 결정의 교집합이 있어야 한다.
T17SourceFacts.install은 original status!=ALLOWED 조건을
`/sourceFactEvidence/uninstalledConditions`에 남긴다. 원래 CONFLICT
상태만으로 새 conflict 판정을 생성하지 않는다. coordinator가 실제
상반된 두 current procedure/decision/source를 설치해야 한다.

## 원본 실패의 구분

frozen T17-S01…S05의 원본 보고서와 action response/observer 원행을
읽었다. 15 subcase, 257 assertion 중 92 FAIL이었다. claim 시나리오의
전체 Claims 16개는 quality/regulatory 준비 증거까지 포함한 결과다.
실제 delivery observation은 한 개이며 DELIVERY_RECONCILIATION OPEN
책임이 존재한다. source는 scopeJson.domainSourceId의 observation ID와
source DECISION command를 구별해야 한다.

일반 RECONCILIATION 기대는 공개 vocabulary의 DELIVERY_RECONCILIATION과
일치하지 않는다. coordinator가 허용한 T17 입력·생성기 변경에서
실제 OPEN 인간 책임과 출처·양·다음 행동·기한을 유지해 수정한다.
`current=true`는 revision 유효성이며 status=OPEN을 대신하지 않는다.

T17 입력 교정은 67개의 정확한 before/after delta로 기록했다.
W40 query만 placeId=W를 명시했다. 별도 dispatched-item query를 추가해
기존 ITEM observer의 snapshot과 운송60·인도0·배분 상태를 대조한다.
W observer에 TRANSIT 행을 섞지 않는다. claim의 원천 assertion은
deliveryObservations source와 그 recordActivity eventId로 한정했다.
일반 Claims source는 변경하지 않는다. duty selector는
DELIVERY_RECONCILIATION/current=true/status=OPEN이다. claim 입력의
nextAction은 원래 responsibility의 한국어 문장을 사용한다. 기존
257개 ID·op·expected·oracleRef와 fixture·grant·clock은 불변이다.

관측 접수의 DELIVERY_RECONCILIATION에는 reported quantity/unit과
eventId·claimedRangeRootId를 residual에 남긴다. 이 수량은
REPORTED_UNCONFIRMED이며 confirmed stock이나 goal contribution이 아니다.

### coordinator가 이어서 해결할 계약 공백

- T17 actor/grant의 assessGoal 부재는 fixture authority를 추가할 이유가
  아니다. 실제 RECORD effect에 묶인 위 server-only 재평가로 해결한다.
  양쪽 writer의 결합 경로는 통합 뒤 검증해야 하며 이를 외부 blocker나
  완료된 runtime PASS로 분류하지 않는다.
- claim source profile의 reconciliationOwnerAlias는 intake다. 현재
  TradeImpact.recorded→ResponsibilityService.openDuty는 연결 Work의
  owner를 택한다. 출고 Work가 sales owner여서 intake 기대와 다르다.
  source profile이 확인한 인간 intake 책임을 연결하는 responsibility
  port가 필요하다. 이 writer는 responsibility/lifecycle의 owner를
  직접 UPDATE하지 않는다. 남은 owner assertion을 바꾸지 않는다.
- late restriction fixture에는 admin-owned ACTIVE RECALL Work와
  openInvestigation이 없다. openInvestigation은 명시한 Work의 owner로
  RECALL_INVESTIGATION을 만든다. 현재 actor 중 createDraft/createWork
  권한자는 없다. placeHold로 정식 조사·승인 사실을 만들거나 sales
  owner를 admin으로 바꾸지 않는다. 별도 실제 조사 입력/업무 준비가
  필요하며 원래 recall-response oracle는 남긴다.
- T17SourceFacts installer는 baseline REGULATORY CONFLICT 조건을
  ALLOWED가 아니라는 이유로 설치하지 않는다. 이를 product CONFLICT
  결과로 꾸미지 않는다. 원래 conflict 의미를 실현할 두 원천·현재
  절차·결정의 실제 입력이 필요하다. 조건 fixture와 원래 status 기대는
  보존한다.

placeHold(RECALL)은 제한 근거이며 formal approved recall이 아니다.
실제 공개 계약에서 정식 조사는 openInvestigation과 실제 업무 준비가
필요하다. 제품은 restricted actual delivery의
DELIVERY_RESTRICTION_RESPONSE를 실제 부족·위반량으로 남긴다. 별도의
RECALL_INVESTIGATION을 placeHold만으로 만들지 않는다. frozen setup의
idTypes/workReferences에는 sourceWork-A QUALITY와 주문 DELIVERY만
있다. fixture responsibilities의 workKind 목록은 설치된 RECALL Work가
아니다. installer도 명시적 installed workAlias가 없는 책임은 그 Work로
설치하지 않는다.

### 수량 있는 접수 의무의 종결 계약

reported quantity를 실제 DELIVERY_RECONCILIATION root에 남긴 뒤 실제
PG에서 기존 ResponsibilityService.applyTradeObservation의
root.quantity!=null 거부가 전체 confirmation을 rollback함을 확인했다.
`causal-witness4.log`는 Docker 환경 오류와 다른 제품 결합 실패다.
T11 writer에게 TradeObservationRemedy.Completion과 해당 validator를
함께 소유해 검증된 actual quantity/unit의 정확한 일치만 종결하도록
요청했다. T11 read-only tree에서 그 두 변경을 확인했으며 통합하지
않았다. 이 writer는 책임 모듈 파일을 직접 수정하지 않는다.
null-quantity legacy/RETURN 경계와 불일치 fail-closed는 유지해야 한다.

## 검증과 인계 경계

Java21.0.12.1과 Maven wrapper3.9.16, locked PostgreSQL image를 사용했다.
Docker context의 실제 endpoint를 inspect해
`unix:///Users/se0nghe0n/.orbstack/run/docker.sock`로 확인했다. PG 명령은
새 JVM에 DOCKER_HOST와 TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE를
명시했다. dependency는 같은 lock의 기존 local cache만 사용했으며 이
checkout source를 컴파일했다. 다른 source의 JAR를 복사하지 않았다.

명령과 보고서는 이 worktree의 `.local/t17-producer-proof/handoff.json`에
있다. 모든 log, XML, source/test/case/fixture hash와 cleanup을 함께
남긴다. 검사의 시점별 source 차이와 마지막 검사를 구별한다.

- producer-regression.log: `./mvnw -B -ntp -q -f backend/pom.xml`
  `-Dtest=PublicScopeEligibilityPostgresTest,RegulatoryConflictPostgresTest,`
  `S3RegulatoryActualTest#labelRejectionVetoAndExpiryBoundary,`
  `DeliveryAdmissionAuthorityTest,PublicScopeEligibilityTest,`
  `PublicWorldReadTest,S4RegulatoryProjectionTest test`는63 tests,
  failure0/error0/skip0, exit0이다. 이 중 실제 PG32개, Unit31개다.
  원래5개 semantic 조건, FEFO, QC permission/hold 독립 근거, 공공
  조회 equivalence, 실제 규제 interval conflict를 검사했다.
- report-read-authority.log: PublicWorldReadPostgresTest의
  publicWorldClaimsAndDeniedDuplicateReservationKeepLedgerUnchanged와
  readWithoutRecordAuthorityCannotConfirmDeliveryOrCreateAnAssessment는
  실제 PG2개, failure0/error0/skip0, exit0이다. 접수의 실제 typed OPEN
  인간 의무·보고20 EA·다음 행동/기한, canonical 실물0, 판정0을
  대조했다. READ는 전체 조직 row fingerprint를 바꾸지 않았고 RECORD
  권한 없는 확정은 실제 인도·실물·판정을 모두0으로 유지했다.
- label-source-binding-final2.log: 위 S3 LABEL test를 원본 binding과
  blob 부재 반례로 보강한 뒤 실제 PG1개를 failure0/error0/skip0,
  exit0으로 확인했다. 현재 requiresLabel 정책·포장/규격 일치·유효기간,
  원본 포장 version 불일치의 HELD, blob 부재의 LABEL UNKNOWN,
  별도 실제 REJECTED label의 LABEL DENIED/REGULATORY ALLOWED를
  대조했다. 기존 stop hook에 cleanup 출력만 더했다. 중복 cleanup
  hook을 만들었던 실패 로그 final.log도 보존하고 중복을 제거했다.
- final-source-unit.log: 현재 RECORD effectClass/capability/transaction
  바인딩 보강 뒤 DeliveryAdmissionAuthorityTest26개가
  failure0/error0/skip0, exit0이다. enum이나 grant 이름만으로 witness가
  생기지 않으며 기존 assessGoal/closeWork/openGoalShortfall 권한과
  WORK 양끝·TARGET/ITEM/PLACE 경계도 유지한다. 전체 test compile을
  포함한다. 위63개를 마지막 source에서 다시 실행했다고 주장하지 않는다.
- t17-inputs-final2.log: `python3 -m unittest discover -s
  verification/cases/T17 -p 'test_*.py'`는34 tests, exit0이다.
  정확한257개 assertion의 ID·순서·op·expected·oracleRef와 모든 fixture
  byte를 baseline과 대조했다. 새67개 delta의 inverse와 역사별
  generator round trip, W40와 ITEM transit60 분리, 실제 report source,
  OPEN 의무의 인간·출처·수량·다음 행동·기한 보존을 검사했다.
- prepare-final.log: `./verify prepare`는 PREPARED,
  preparationProblems0, exit0이다. 새 DELIVERY_RECONCILIATION selector가
  공개 kind와 맞으므로 기존 check_vocabulary.py의 사용되지 않는
  RECONCILIATION pending 예외 한 줄을 제거했다. 그 예외가 남아
  prepare를 실패시킨 첫 로그도 보존했다. case나 business oracle를
  줄인 것이 아니다. 준비는 실제 실행 인수가 아니다.
- `git diff --check`는 exit0이다. cleanup.json에서 이 worker log의
  disposable container27개가 모두 없고 알려진 임시 blob/key 경로10개가
  모두 제거됐음을 확인했다. 사용자 파일과 다른 worker container는
  정리하지 않았다.

초기 postgres.log는 Docker discovery29 errors다. `/var/run/docker.sock`
부재로 Testcontainers가 JVM 안에 실패를 cache한 환경 오류이며 product
FAIL로 합치지 않는다. 새 JVM에 검증한 OrbStack endpoint를 지정한
postgres2.log는28 tests, failure0/error0/skip0, exit0이었다. 이 성공은
그 시점 source의 결과이며 이후 추가한 causal 경로의 성공 증거가 아니다.
추가 test의 definition fixture 형식/중복 version/활성화 실패 로그도
보존했다. 기존 같은 version의 실제 fixture definition에 지원하는
DELIVERED template/Delivery.quantity를 추가해 준비 오류를 바로잡았다.
업무 fixture의 capability/grant는 추가하지 않았다.

causal-witness4.log의 실제 PG 실패는 위 typed reconciliation validator
결합 문제다. 원래 public-world confirmation 회귀와 다음3개의 full
causal/admission 검사는 T11 validator·AssessmentService 통합 뒤
coordinator가 실행한다. 이 tree에서 성공으로 표시하거나 mock으로
실제 책임 종결을 대체하지 않았다.

`DOCKER_HOST=unix:///Users/se0nghe0n/.orbstack/run/docker.sock`
`TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock`
`JWT_PUBLIC_KEY=<local-public-test-key>`를 설정한 새 JVM에서 다음을
실행해야 한다.

`./mvnw -B -ntp -q -f backend/pom.xml`
`-Dtest=PublicWorldReadPostgresTest test`

여기에 causal-delivery-integration tag의 normal/restricted2개와
causalRecordWitnessRequiresRealCommandEffectAndCurrentAllOfSourcesWithoutPublicRead
1개가 포함된다. 앞의2개는 실제 immutable assessment를 검사한다.
뒤의1개는 assessment port만 spy해 실제 현재 command·canonical graph·
SOURCE/TARGET/WORK/ITEM/PLACE·revocation gate를 분리 검사한다.
실제 책임 remedy는 mock하지 않는다. 둘 다 public assessGoal이 없는
receiver이며 새 fixture grant를 발급하지 않는다. integration source
witness 검사는 아직 해당 validator를 통과하지 못했으므로 NOT_RUN이다.

`./verify scenarios --actual`·실모델·BTP는 NOT_RUN이다. 실제461개
원본 ID의 실행 인수는 통합 뒤 Haiku native dynamic Workflow 소유다.
Task/Step/S5/S6는 OPEN이며 이 producer 인계가 전체 완료를 뜻하지 않는다.
coordinator는 별도 tree에서 위10개 조건 oracle의 원천 대응, 실제
CONFLICT 설치, T11의 typed 종결/인과 재평가, intake owner와 정식 recall
준비 공백을 이어서 처리한다. 이 writer는 observer나 lifecycle 변경을
가져오지 않았고 shared task-status/decisions를 수정하지 않았다.

## 변경 파일

다음30개 파일만 own commit에 포함한다. 전체 SHA-256은 handoff.json에
기록했다. T11/observer 파일이나 기존15개 business fixture는 수정하지
않았다.

- `backend/src/main/java/com/mulino/application/evidence/CanonicalEvidenceVerification.java`
- `backend/src/main/java/com/mulino/application/inventory/InventoryQueries.java`
- `backend/src/main/java/com/mulino/application/inventory/PublicWorldRead.java`
- `backend/src/main/java/com/mulino/application/quality/QualityEligibility.java`
- `backend/src/main/java/com/mulino/application/trade/regulatory/RegulatoryEligibility.java`
- `backend/src/main/java/com/mulino/application/trade/regulatory/RegulatoryEvidence.java`
- `backend/src/main/java/com/mulino/application/trade/sales/DeliveryAdmission.java`
- `backend/src/main/java/com/mulino/application/trade/sales/DeliveryCommands.java`
- `backend/src/main/java/com/mulino/application/trade/sales/DeliveryRecordCause.java`
- `backend/src/main/java/com/mulino/application/trade/sales/SalesQueries.java`
- `backend/src/main/java/com/mulino/domain/evaluation/CanonicalFactAdmission.java`
- `backend/src/main/java/com/mulino/domain/trade/sales/SalesRepository.java`
- `backend/src/test/java/com/mulino/core/S3RegulatoryActualTest.java`
- `backend/src/test/java/com/mulino/quality/FulfillmentPostgresTest.java`
- `backend/src/test/java/com/mulino/quality/PublicScopeEligibilityPostgresTest.java`
- `backend/src/test/java/com/mulino/quality/PublicWorldReadPostgresTest.java`
- `backend/src/test/java/com/mulino/trade/sales/DeliveryAdmissionAuthorityTest.java`
- `docs/execution/fix-wave-20261024-sales.md`
- `verification/cases/T17/author_delivery_goal_inputs.py`
- `verification/cases/T17/author_public_contracts.py`
- `verification/cases/T17/author_sales_contract_inputs.py`
- `verification/cases/T17/case.json`
- `verification/cases/T17/sales-contract-input-delta.json`
- `verification/cases/T17/scenario.feature`
- `verification/cases/T17/test_completion_inputs.py`
- `verification/cases/T17/test_delivery_goal_inputs.py`
- `verification/cases/T17/test_first_failure_inputs.py`
- `verification/cases/T17/test_public_contracts.py`
- `verification/cases/T17/test_sales_contract_inputs.py`
- `verification/cases/check_vocabulary.py`
