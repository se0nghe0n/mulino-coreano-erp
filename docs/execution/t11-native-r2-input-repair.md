# T11 native r2 입력과 과거 판정 Given을 고친다

2026-10-24에 baseline `fb58d86663f28ec90dd38322399c384a42de4672`의
`fix/t11-native-runtime-oracles`에서 작업한다. 지정 runtime은
`codex_2/gpt-6.1-sol/xhigh/priority`다. 제품과 공통 observer는
수정하지 않는다. main·push·PR·Grok·다른 writer commit import는
수행하지 않는다. 전체 Task와 S5/S6는 OPEN이다.

## 원인과 보존 범위를 고정한다

root의 `native-audit-r2`와 frozen `native-preparation-r2/manifest.json`은
`fa316f01`의 Haiku native 실행이다. T11은 원110개 중94개가 통과하고,
closed-followup 두 개가 실패하며 cancel14개는 실행 중단으로 미관찰이다.
이 기록을 이번 focused 실행의 결과로 바꾸지 않는다.

원본 `cancel-after-shipment`는 pick·dispatch의 allocation을
`reserve/response/allocationId`로 읽는다. 실제 성공 응답의 경로는
`/response/effects/allocationId`다. 다만 native raw의 reserve 자체도
`REJECTED/INSUFFICIENT_ELIGIBLE_QUANTITY`이며 effects가 비었다.
pointer만 바꾼 attempt1에서도 같은 거부를 확인했다.

원 fixture의 `currentSaleConditions`는 실제 typed source가 아니며,
`baseline.obligations`와 `assignments`도 installer에서 빠져 있었다.
typed 원천을 설치한 뒤에도 원 LOT에 만료일이 없어
`LOT_EXPIRY_UNKNOWN`, 적격량0이 남았다. 원본 상태를 정상으로
바꾸지 않고 새 명시 합성 입력과 음성 검사를 분리한다.

closed-followup 원 fixture에는 PARENT=CLOSED/FULFILLED/revision4와
OLDASSESS/SATISFIED/evaluator-v1/평가00:00이 이미 있다. installer가
Assessment alias와 baseline.assessments를 설치하지 않아 실제 SQL은0행이다.
기존 focused 검사의 빈 배열 전후 equality는 이 누락을 놓쳤다.
새 후속 사건이 과거 판정을 덮은 제품 동작으로 해석할 근거는 없었다.

원 fixture bytes·기존 actor/grant·clock·source 값과 baseline의
110개 assertion 전체 필드를 유지한다. 추가 fixture는
`verification/cases/T11/fixtures/native-r2/`에만 둔다.
`native-r2-fixture-ledger.json`은 원본·추가본·bytes hash를 함께 보존하고,
strict inverse가 원본 bytes를 복원한다. case의 fixtureRef와 입력 변경은
`lifecycle-input-ledger.json`에도 기록한다.

## coordinator가 채택한 추가 Given을 구별한다

과거 Goal에는 이름 `goal-closed-v1`만 있고 quantityMode/predicate가 없다.
Assessment의 필수 Goal FK를 만족시키려고 이를 임의로 추정하지 않는다.
coordinator는 새 synthetic completion으로 Q100 BOX, P/LOT/W, 평가00:00의
STATE_AT/PHYSICAL_HELD를 선택했다. fixture에
`originalGoalModeProvided=false`, `coordinatorSelectedSynthetic=true`,
`originalAuthorityClaimed=false`, `historicalEvaluationExecuted=false`를 적는다.

실제 발행된 definition-v1의 `held-at` template를 조회해
STATE_AT·ARRIVED·stateQuantity·CURRENT_STATE와 template hash를 대조한다.
100이라는 명시 target으로 치환한 predicate hash도 대조한다.
원 definition label을 바꾸거나 evaluator-v1를 현재 evaluator로 등록하지
않는다. historical Goal row와 원 OLDASSESS, immutable InputSnapshot을
같은 설치 transaction에 저장한다. snapshot에는 원 Work·원 판정과 새
completion을 따로 담고 실제 bytes hash를 Assessment에 연결한다.

설치 recordedAt/createdAt은 fixture 시작02:00이고 원 effectiveAt/assessedAt은
00:00이다. Work.currentGoalVersionId는 NULL을 유지한다. 새 completion을
현재 Goal로 승격하지 않으며 과거 evaluator의 실행 성공을 주장하지 않는다.
판정과 입력 snapshot의 UPDATE/DELETE 거부도 실제 PG에서 확인한다.

cancel에는 기존 주체와 분리한 `nativeR2Source`와 ITEM P의 self grant를
추가한다. 원천 대조·QC/customer/commercial·regulatory/label만 허용하며
assessment·openObligation·권한 관리 capability는 추가하지 않는다.
허구의 SELL/DISPATCH 정책과 명시 원천 입력을 사용해 각 행동의 실제
typed source 체인을 HTTP로 만든다. 이 추가 authority는 운영 승인이나
원래 존재하던 권한이 아니다.

원 ROOT10/ASSIGN/SOURCE/A/물리 scope/기한/원천 label은 별도 historical
DOCUMENT Given이다. 실제 DocumentVersions·blob·hash·조직·시각과
sourceKind=DOCUMENT/sourceVersion1을 연결한다. 원 sourceOccurrence 문자열
`baseline-shortfall-10`은 문서 원문과 root scope에 남기며 canonical receipt나
OCCURRENCE FK를 만들지 않는다. generic ROOT의 중복 설치는 건너뛴다.

새 positive fixture에서만 LOT.expiresAt을2026-10-31T23:59:59Z로 선언한다.
`originalExpiryProvided=false`와 합성 선택·권한 비주장 flag를 남긴다.
음성 진단은 이 보완의 applied=false와 원래 만료일 부재를 명시한다.
나머지 typed 근거가 있어도 unknown expiry가 eligible0과 예약 거부를
유지하는지 실제 경로에서 검사한다.

## 한정 실행 증거를 남긴다

실행 명령은 아래와 같다. `./verify scenarios --actual`과 모델을
호출하지 않는다. 원110개의 Haiku native 재실행은 통합 뒤 별도 gate다.

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@21 \
PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH \
DOCKER_HOST=unix:///Users/se0nghe0n/.orbstack/run/docker.sock \
T11_TEST_SELECT=T11NativeInputsPostgresTest,T11HistoricalAssessmentFixtureTest,T11WorkLifecyclePostgresTest \
python3 verification/actual/t11/run.py .local/t11-native-r2-attempt7
```

runner가 자기 npm/CDS/JAR를 만들고 disposable PostgreSQL과 HTTP backend,
임시 RSA key/blob root를 소유한다. run.json은 JAR/CSN hash, 명령별 exit,
서버·컨테이너·키 cleanup을 기록한다. source-input-hashes.json은 이번
dirty source와 fixture의 정확한 SHA256을 기록한다. runtime codeCommit은
baseline이며 최종 commit과 같은 것으로 가장하지 않는다.

focused observer는 직렬 HTTP 바로 뒤의 CURRENT_COMMITTED read-only
REPEATABLE_READ다. native RESULT_REVISION binding 인수와 구별한다.
공통 observer를 수정하거나 관찰되지 않은 transaction 필드를 채우지 않는다.

attempt5에서는 예약·피킹·출고·B 수락·취소가 실제 APPLIED이고 cancel의
원14개 중12개가 통과했다. 고정 revision1의 첫 close는 STALE_REVISION으로
실패했다. 입력 layer에 close-with-duty 직전 getWork를 추가해 원래
REJECTED oracle가 실제 잔여 의무 거부를 검사하도록 한다.

나머지 원자성 oracle는 audit.transactionId와 새 assignment의
createdTransactionId가 공통 observer에 없어 실패했다. source worker가
별도 scope에서 이 projection을 담당한다. attempt5는 cleanup 전에 xmin를
캡처하지 않았으므로 소급 생성하지 않는다. attempt7에서 CommandRecord,
수락 감사, 신규 successor row의 raw bytes와 xmin를 별도 SQL로 수집한다.
xmin를 모든 과거 row의 생성 transaction으로 해석하지 않는다. successor가
이전 snapshot에 없고 revision0인 새 행인지 먼저 확인한다.

최종 focused 결과와 잔여 observer gate, artifact hash는 handoff와
`.local/t11-native-r2-attempt7/`의 원자료에 기록한다. 원자성 projection과
parentWorkId projection의 통합 전 실패를 전체 runtime PASS로 표시하지
않는다. static 검사·focused PG·Haiku native·전체 인수를 구별한다.

attempt7의 실제 결과는 다음과 같다.

- 선택된 JUnit11개 중10개가 통과하고1개가 실패해 Maven/runner exit1이다.
  기존 lifecycle7개와 새 historical Given·unknown expiry 검사는 통과한다.
  나머지1개는 DB 설치 전 fail-closed 입력 변이 검사다.
- cancel 원14개는13 PASS/1 FAIL이다. 실패 원문은
  `acceptance-and-assignment-one-transaction: projected field missing: transactionId`다.
  첫 close는 현재 revision을 사용해 원래 REJECTED 기대를 충족한다.
- 실제 수락 응답 transactionId와 CommandRecord/감사/새 successor의
  xmin는 모두1020이다. SQL 검사는 통과하지만 누락된 공통 projection을
  대신 채우지 않으므로 위 원oracle의 FAIL을 유지한다.
- closed-followup의 원 history 관련8개 oracle와 원 Work 불변, history
  count1/원 ID/문구/hash/전후 bytes/UPDATE·DELETE 거부를 확인한다.
  별도 owner가 맡은 parentWorkId oracle1개는 이 focused 검사에서 실행하지
  않는다. 원 WorkLinks FOLLOWUP 행은 독립 SQL artifact에 남는다.
- positive source의70 HTTP 호출은 APPLIED다. QC/REGULATORY/LABEL/
  DISPOSITION/CUSTOMER_CONTRACT가 ALLOWED100이며 실제 예약·피킹·출고와
  B 수락, 잔여10 이전, 취소를 실행했다. unknown expiry 분기는 같은
  근거가 있어도 LOT_EXPIRY_UNKNOWN/eligible0/예약 거부를 유지한다.
- backendStopped/containerRemoved/ephemeralKeysRemoved가 모두 true다.
  임시 blob도 같은 temporary directory와 함께 제거한다.

읽기전용 index는 `.local/t11-native-r2-attempt7/artifact-index.json`이다.
원자성 SQL artifact SHA256은
`109dbd3105e4cf4e90892e427232829fda048563ef3ae0cf6df036415c5c7958`이다.
history-before와 history-after 파일 bytes는 동일하다. 이 index는 수집한
파일의 위치·hash를 묶으며 acceptance receipt나 runtime PASS manifest가 아니다.

`python3 -m unittest discover -s verification/cases/T11`은26개가 통과한다.
원110개 assertion 전체, 원 fixture bytes와 strict inverse를 검사한다.
`./verify prepare`는 입력 준비 검사이며 제품 runtime 성공이 아니다.
