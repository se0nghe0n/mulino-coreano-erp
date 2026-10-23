# T09 fulfillment 내부 증거 검증

Task와 현재 Step은 OPEN이다. 이 문서는 `e932710856553570094a3350268417f19b03a021`
기준의 한정 PRODUCT 수정과 결정적 backend 검증을 기록한다.
원래 T09 fixture와 전체 시나리오의 성공을 선언하지 않는다.

## 원인과 수정

예약·피킹·출고의 현재 적격성 검사가 `EvidenceQueries.verifiedAt`의
공개 `getEvidence` 인가를 함께 요구했다. warehouse가 가진
fulfillment 권한으로 정확한 물량을 실행해도, 증거 조회 권한이 없으면
실제 검증된 QC·규제 사실을 적격성에 사용할 수 없었다.

2026-10-23 coordinator의 fresh HTTP/PG 정정에 따르면 원래
`cumulative-versus-state` warehouse에는 `getEvidence`가 있고,
`exists-versus-end-throughout` warehouse에는 없다. 두 실제 grant는
그대로 유지한다. 이 수정은 조회 권한이 없는 경우의 PRODUCT 경로를
고친다. 두 T09 흐름 전체가 이 원인 하나로 실패했다고 주장하지 않는다.
입력 writer의 regulatory `$encode` resolver 작업과 별도로 검증한다.

`FulfillmentEvidencePurpose`는 inventory package의 비공개 factory에서
네 명령 `reserveQuantity`, `replaceAllocation`, `pickQuantity`,
`dispatchQuantity`에만 만든다. 공개 slot·API·grant는 추가하지 않는다.
factory는 preparation transaction 안에서 실제 segment와 현재 sales line을
다시 읽고 organization·actor·stable owner·item·place·Work·customer·unit을
묶는다. 사용 때마다 같은 현재 인가와 line revision을 다시 확인한다.

명령은 먼저 실제 TARGET·ITEM·PLACE·판매 WORK에 현재 권한을 가져야 한다.
원천의 WORK·PLACE·SOURCE도 같은 명령 권한으로 별도 대조한다. 따라서
판매 Work 하나의 grant나 같은 조직이라는 사실만으로 다른 물량·원천을
사용할 수 없다. 원천 Work와 판매 Work가 다르면 둘 모두의 권한이 필요하다.
원천 물량은 정확한 ancestor-to-descendant 물리 좌표로 연결돼야 한다.
같은 품목의 무관한 segment는 사용할 수 없다.

preparation은 active command ID가 생기기 전에도 실행된다. 따라서 witness는
`CommandExecution.commandId()`를 요구하지 않지만 transaction과 현재 인가를
요구한다. 기존 gateway의 정의·subject binding·revision·policy·승인·현재
grant/delegator fence와 commit 검사는 그대로 실행된다. 관련 item의 원문
source/subject, 물리 segment, 규제 procedure fence도 준비 목록에 넣어
잠금 후 같은 목록과 사실을 재검증한다. 사용한 원천 scope의 현재 권한은
효과 기록 뒤 handler 반환 전에도 확인한다. 별도 source Work grant가
만료되면 기록한 효과도 rollback한다. witness가 gateway 검사를 대체하지 않는다.

`CanonicalEvidenceVerification`은 기존 canonical 검증 chain을 공유한다.
공개 `verifiedAt`은 계속 공개 `getEvidence` 권한으로 검증한다. 내부 경로는
witness와 정확한 QC/규제 kind를 요구하고 다음 사실을 추가로 대조한다.

- 원문 blob·hash·availability, 원문 JSON과 event payload의 일치다.
- 다섯 verification flag, claim/event/canonical identity·수량·단위·시간이다.
- asOf/knownAt 가시성, 현재 source profile/policy, 단일 inbox의 event/hash다.
- event·claim·document·canonical의 supersession/invalidations 부재다.
- QC decision과 basis의 연결, 원문의 category/action/customer/기간/좌표다.
- 규제의 실제 procedure/version/제출/decision 및 필요한 label 원문이다.

QC·CUSTOMER·COMMERCIAL·규제·기한·보류·custody·정확한 range·FEFO 계산은
유지한다. SELL의 사실은 DISPATCH를 대신하지 않는다. 누락 사실은 계속
UNKNOWN/적격0이며 명령 효과는0이다. FEFO 후보와 출고 발생 시각부터 현재까지의
연속성 검사에도 같은 한정 witness 경로를 쓴다.

## 유지한 경계와 교차 owner 접점

공개 `getEvidence`, 다운로드, `evaluateEligibility`, `getInventory`, 기본
`assess`/`current`/`verifiedAt`의 읽기 권한은 그대로다. 명령 응답은 기존
계약의 ID·lineage만 반환하며 원문·blob을 노출하지 않는다.

`replaceAllocation`의 적격성/FEFO 경로는 수정했다. 별도로 사용자가 명시하는
`replacement-proof` 원문은 기존 `FulfillmentBasis.replacement`의 공개 읽기
인가를 유지한다. 이 원문은 canonical QC/규제 사실과 다른 명시적 명령
입력이며 기존 `replacementRejectsUnavailableSupersededAndUnauthorizedOriginal`
계약을 바꾸지 않았다. 따라서 이 별도 원문까지 읽을 권한이 없는 actor는
replace 자체가 계속 FORBIDDEN이다. 새 회귀 검사로 이 경계를 고정한다.

기본 sweeper, quality/regulatory 쓰기, delivery historical read, receipt와
assessment admission은 수정하지 않았다. A owner의 canonical/receipt/
assessment 변경과 결합 검증은 coordinator가 소유한다. T09 source writer의
fixture·graph·harness 변경은 이 worktree에 포함하지 않는다.

## 변경 파일과 symbol

아래 Java 경로의 prefix는 `backend/src/main/java/com/mulino/`다.

| 파일 | 변경 symbol |
|---|---|
| `application/evidence/EvidenceQueries.java` | `verifiedAt`, 공유 verifier 연결 |
| `application/evidence/CanonicalEvidenceVerification.java` | `verifiedForRead`, `verifiedForFulfillment`, `strict` |
| `application/inventory/FulfillmentEvidencePurpose.java` | `authorize`, `permitsCanonical`, `permitsSource`, `verifyCurrentSources` |
| `application/inventory/FulfillmentCommands.java` | `prepare`, `execute`, `requireContinuousAuthority` |
| `application/inventory/FulfillmentBasis.java` | `allocation`, `addEligibilityFences` |
| `application/quality/QualityEligibility.java` | `assessForFulfillment`, 기존 `assess` 공통 계산 |
| `application/quality/QualityEvidence.java` | `requireProof`, witness를 받는 `valid` |
| `application/trade/regulatory/RegulatoryEligibility.java` | `assessForFulfillment`, 기존 `assess` 공통 계산 |
| `application/trade/regulatory/RegulatoryEvidence.java` | witness를 받는 `current`, 공통 `require` |

새 검사는 `backend/src/test/java/com/mulino/quality/`의
`FulfillmentPostgresTest.java`에 `internal*` 이름으로 추가했다. 기존
71개 테스트는 수정하지 않았다. fixture를 상속하는 별도 test class가
기존 71개를 다시 실행하지 않도록 같은 클래스에 추가했다.
이 문서까지 총 11개 파일만 commit한다.

## 검증

실제 PostgreSQL container와 공개 attach/activity/reconciliation/canonical/
quality/regulatory 명령으로 만든 source graph를 사용한다. 새 canonical 사실을
SQL seed하지 않는다. 누락·raw·미검증·정정·중복은 공개 명령으로 만든다.
직접 source 변조는 immutable trigger에 거부되는지 별도로 검사하며 이를
우회하지 않는다. `getEvidence` 제거도 새 합성 test actor에만 한다.

검증 환경은 Java 21.0.12.1, Maven 3.9.16, Node 24.16.0,
CAP Java 5.1.1, Spring Boot 4.1.1, 실제 PostgreSQL 18.6이다.
PostgreSQL image는 기존 fixture의 digest
`4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280`을 쓴다.

| 실행 | 결과 |
|---|---|
| 기존 `FulfillmentPostgresTest` 71건 | PASS, failure/error/skip 0이다. |
| 기존 `QualityPostgresTest` 10건 | PASS, failure/error/skip 0이다. |
| 새 `FulfillmentPostgresTest#internal*` 32건 | PASS, failure/error/skip 0, Maven exit 0이다. |
| `git diff --check` | PASS다. |

기존 81건과 마지막 새 32건 사이의 9개 제품 Java 파일 hash가 같음을
확인했다. 새 검사는 별도 상속 클래스에서 같은 fixture 클래스의
`internal*` 메서드로 옮겨 중복 실행을 없앴다. 기존 71건 본문은 그대로다.

최초 결합 실행의 selector는 아래와 같다. 이 실행에서 기존 81건은
통과했고, 새 32건 중 정정 기록 시각 2건이 실패했다.

```text
-Dtest=FulfillmentInternalEvidencePostgresTest#internal*,QualityPostgresTest,FulfillmentPostgresTest
```

두 정정의 기록 시각을 이전 버전보다 1초 뒤로 고친 뒤, 최종 실행은
아래와 같이 통과했다. 테스트 전용 RSA public key를 `KEY`에 사용했으며
실행 후 삭제했다. 별도 API 인증이나 외부 모델 호출은 없다.

```sh
PATH="/Users/se0nghe0n/.nvm/versions/node/v24.16.0/bin:$PATH" \
JAVA_HOME=/opt/homebrew/opt/openjdk@21 JWT_PUBLIC_KEY="$KEY" \
./mvnw -B -ntp -f backend/pom.xml \
  '-Dtest=FulfillmentPostgresTest#internal*' \
  -Dlogging.level.com.sap.cds.services.utils.telemetry=OFF test
```

새 32건은 공개 조회/다운로드 거부, 준비 검증, 예약·pick·dispatch,
FEFO, SELL-only 거부, all-of, 역할/위임/철회/만료, 미완성 QC/규제 source,
정정·중복·원문 손실·LOT 만료, source Work 경계, rollback/replay,
효과 기록 중 source grant 만료를 검사한다. 새 source grant는 만료
반례의 합성 actor에만 있으며 제품/원래 T09 grant를 바꾸지 않는다.

초기 시도의 문제는 모두 구별해 남겼다. 빈 worktree의 CDS dependency는
`npm ci --ignore-scripts`로 준비했다. 직접 source 변조는 DB immutable
trigger가 거부하므로 이를 끄지 않고 공개 정정/미검증 경로로 고쳤다.
같은 시각 정정 두 건은 monotone supersession 계약에 맞춰 수정했다.
CAP transaction proxy가 beforeCommit 예외를 감싸는 경계도 확인했으며,
원천 권한의 마지막 검사는 handler 안에서 수행해 `FORBIDDEN` 응답과
rollback을 유지했다. 해당 회귀는 마지막 32건에 포함한다.

실행 artifact는 이 worktree의 `.local/t09-internal-eligibility/`에 있다.

- `final-focused-tests.log`, `reports-baseline-regression/`은 기존 81건과
  정정 테스트의 최초 오류를 보존한다.
- `new-tests-final.log`, `reports-internal-final/`은 마지막 32건 PASS다.
- `source-final-recheck.log`는 예약/출고와 source grant 만료 2건 PASS다.
- `test-summary.json`, `source-manifest.json`은 결과와 source hash다.
- `cleanup.json`은 이 실행의 container 15개가 모두 제거됐음을 기록한다.

개인 key는 생성 직후, public key는 최종 검사 후 삭제했다. worktree와
로그는 인계를 위해 보존한다. 기존 fixture의 OS 임시 blob directory
정리 정책은 바꾸지 않았다. 별도 JAR는 만들지 않았고 자신의
`backend/target`에서 compile/test했다.

전체 backend·harness·actual scenario·실모델·HTTP API/BTP는 NOT_RUN이다.
coordinator의 baseline 전체 검사와 원래 T09 실행을 이 수정의 성공으로
대신 세지 않는다. 결합 인수와 Task/Step 종료는 coordinator가 맡는다.
