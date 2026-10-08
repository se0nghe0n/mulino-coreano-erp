# S4k 종료 리뷰 3 지적 수정 기록

사용자 Step3(Claude Opus medium) 구현 worker의 기록이다. 기준선은 tag
`step3-s4k-baseline`(`75999b09`, backend 509 PASS, harness 501 PASS,
native S4/S3/S2/S1 PASS)이고, branch는 `step3/s4k-closure3`, worktree는
`/Volumes/VideoStore/Developer/mulino-ontology-step3-s4k`다. 소유 범위는
`backend/**`, `verification/actual/**`,
`verification/harness/src/main/java/org/mulino/verification/actual/**`다.
이번 수정은 `backend/**`만 바꿨다. coordinator만 Task branch에 통합한다.

입력은 skeptic이 검증한
`/Volumes/VideoStore/Developer/.mulino-tools/s4-closure-3-opus-verdicts.json`이다.
전체 review는 같은 폴더의 `s4-closure-3-opus.json`이다. Astra low review
`s4-closure-3-astra.json`은 S4를 통과시켰다. 계획 근거는
`docs/ontology-implementation-plan.md`의 다음 절이다.

- §4.3 정정 재평가. 134행 "의무의 해소/면제 결정이 이미 유효하면 자동
  부활시키지 않는다"를 포함한다.
- §5.1 폐쇄·가져온 업무를 다시 쓰지 않는다.
- §5.3과 246행. `waiveObligation`은 현재 scope/revision을 검사하고 현재
  책임의 단절을 거부한다.
- §6 정산.

## 판정

| 항목 | 판정 | 원인과 수정 | test (RED → GREEN) |
|---|---|---|---|
| MUST opus3[0] (+[4]/[5] 중복, P2) 복원 정정 전에 결정하고 뒤에 실행한 SETTLEMENT_DIFFERENCE 면제가 마지막 열린 root를 닫음 | FIXED | 복원 정정 link는 열린 root가 있으면 그 root를 건드리지 않고 넘어갔다. 그래서 정정 전에 assignment revision에 묶어 결정한 MANAGER 면제가 정정 뒤에도 binding 검사를 모두 통과했다. 결과는 CURRENT+UNSATISFIED(+2)인데 owner가 없는 상태였고, 반대 순서와 결과가 달랐다. 이제 `SettlementContributionReview.contributionChanged`는 CURRENT·비SATISFIED·열린 root가 있으면 `ResponsibilityService.reissueOpen`을 부른다. 이 메서드는 root의 열린 assignment마다 revision을 올리고 nextAction(복원 기준 문구)·nextCheck를 새로 쓰며 basis `CONTRIBUTION_RESTORED:<canonical>`를 남긴다. owner와 work는 그대로다. 같은 basis를 다시 처리하면 아무것도 바꾸지 않는다. 그래서 정정 전 결정은 실행 시점에 거부되고, 차이는 같은 root의 OPEN 의무로 남는다. 정정 뒤 현재 revision으로 새로 결정하면 실행된다. | `FulfillmentPostgresTest.waiverDecidedBeforeARestoringCorrectionCannotCloseTheRestoredDifference`(새). RED: 정정 전 결정으로 실행한 `waiveObligation`이 `APPLIED`였다. GREEN: 결정 당시 revision은 `CONFLICT/STALE_REVISION`, 다시 발행된 revision은 `REJECTED`다. R1이 owner·복원 nextAction·nextCheck를 가진 채 OPEN이고, 조회는 CURRENT/UNSATISFIED/differenceDutyOpen=true다. 재처리는 revision을 바꾸지 않는다. 새 결정은 APPLIED다. 반대 순서 test `restorationAfterAdjustmentAndWaiverOpensAnOwnedSettlementDifference`도 그대로 PASS다 |
| SHOULD opus3[1] (P3) CURRENT 경로가 이미 MANAGER가 면제한 차이에 root를 다시 엶 | FIXED | 복원 뒤 열린 root가 없으면 무조건 새 root를 열었다. 그래서 원 대조 차이 R0와 30→28 정정 R1을 모두 면제한 뒤 30으로 복원하면, R0가 이미 면제한 잔여에 R2와 follow-up이 생겼다(계획 134행 위반). 이제 새 root를 열기 전에 `waivedResidual`이 이 Match의 WAIVED root가 지금의 CURRENT 잔여를 이미 덮었는지 본다. 덮은 잔여는 둘이다. Match 자신의 root(`dutyRootId`)는 불변 `originalDifference`, CURRENT 경로 root는 scope residual에 새로 기록한 `settlementDifference`다. 같으면 열지 않는다. 금액이 다르거나 알 수 없으면 기존처럼 연다(fail-closed). 새 정정(CHANGED)이 새 의무를 여는 것은 그대로다(계획 167행). | `FulfillmentPostgresTest.restorationToAnAlreadyWaivedOriginalDifferenceDoesNotReviveIt`(새, 단가 1.1). RED: 복원 뒤 OPEN 1(새 root). GREEN: OPEN 0, SETTLEMENT_DIFFERENCE root 2 |
| SHOULD opus3[2] (P3) relink 없이 무효화된 SALE 인도 증거에 정산 의무가 없음 | FIXED(무효화) / DEFERRED(미연결 supersession) | `AssessmentCorrectionImpact.apply`는 PHYSICAL_RECEIPT canonical에만 settlement port를 불렀다. 이제 정정 event가 `invalidatesId`를 가지면 PHYSICAL_DELIVERY canonical에도 부른다. 그러면 UNVERIFIED Match에 무효화된 canonical을 source로 하는 SETTLEMENT_DIFFERENCE가 owner와 함께 열린다. 무효화가 아닌 일반 supersession은 일부러 제외했다. 일반 정정 경로(`correctEvidence` 다음 `linkCanonicalOccurrence`)는 별도 거래이므로, apply에서 열면 정정마다 UNVERIFIED root와 link의 CHANGED root가 함께 열린다. 그러면 기존 단일 root 계약(조정 대상 해석, 복원 해소)이 깨진다. relink를 기다리는 이 상태는 apply가 여는 FOLLOWUP_REVIEW 의무가 owner로 맡는다. Match는 SATISFIED로 읽히지 않는다. | `FulfillmentPostgresTest.invalidatedDeliveryEvidenceOpensAnOwnedSettlementDifference`(새). RED: OPEN 0. GREEN: 조회 UNVERIFIED, OPEN 1(source=원 인도 canonical, owner·nextAction·nextCheck 있음) |
| SHOULD opus3[3] (P3) 넓어진 link context가 IMPORTED Work를 무효화하다가 raw trigger 오류 | FIXED | `affected()`는 lifecycleMode를 보지 않았다. 그래서 IMPORTED S1 Work를 `markInvalidation`으로 UPDATE했고, V9 `work_lifecycle_guard`가 `Imported S1 work remains immutable`로 거래 전체를 rollback했다. 이제 `affected()`는 IMPORTED Work를 결과에서 뺀다. 가져온 행은 가져온 뒤 바뀌지 않으므로 knownAt view에 보인다. 이 거래가 방금 고친 COMMAND Work는 그대로 남는다. 적용 범위는 `evidenceLinked`와 `apply`가 함께 쓰는 집합이다. 그래서 IMPORTED Work를 직접 대상으로 한 정정도 이제 실패하지 않는다. 그 Work의 무효화와 FOLLOWUP_REVIEW를 만들지 않고 정정을 적용한다. | `CompletionCoverageGatewayTest.verifiedLinkSkipsAnImmutableImportedWorkInsteadOfFailingOnItsTrigger`(새, fixture `rootFixture("IMPORTED")`). RED: `PSQLException: Imported S1 work remains immutable`. GREEN: link APPLIED, Work revision·pendingInvalidation 불변, 범위 의무 RESOLVED |

## DEFERRED 잔여

| 항목 | 이유 | owner · 맡을 Step |
|---|---|---|
| opus3[2] 일반 supersession 정정 event를 끝내 relink하지 않은 SALE 인도 | fail-closed다. Match는 UNVERIFIED이고 FOLLOWUP_REVIEW 의무에 owner가 있다. 정산 의무까지 열려면 apply의 UNVERIFIED root와 link의 CHANGED root를 하나로 잇는 계약이 필요하다. 예를 들면 link가 UNVERIFIED root를 이어받는 방식이고, 지금 단일 root 단언 test들이 같이 바뀐다. | settlement owner · S5 진입 backlog |
| opus3[3] IMPORTED Work를 직접 대상으로 한 정정의 후속 책임 | 정정은 이제 적용된다. 다만 IMPORTED Work에는 무효화 표시나 FOLLOWUP_REVIEW를 남기지 않는다. 가져온 참조 업무의 후속 책임을 어디에 둘지는 계획이 정하지 않았다. COMMAND follow-up Work를 만드는 방식이 후보다. | work/responsibility owner · S5 진입 backlog |

## 실행 증거

환경: `. /Volumes/VideoStore/Developer/.mulino-tools/env.sh`, `backend/`에서
`npm ci`(exit 0), `$MULINO_SLOT`, Testcontainers PostgreSQL. 집중 명령은
`$MULINO_SLOT ./mvnw -B -ntp -f backend/pom.xml -Dmulino.evidence.blob-root=$(mktemp -d) -Dtest=<classes> test`다.
결과는 run/fail/error/skip 순서다.

| 시점 | classes | exit | 결과 |
|---|---|---|---|
| RED: 새 test + baseline 제품 코드(`backend/src/main` stash) | FulfillmentPostgresTest 새 3개, CompletionCoverageGatewayTest 새 1개 | 1 | 4/3/1/0. 위 표의 RED 열. MUST test는 assertion을 결과 우선으로 다시 배치한 뒤 한 번 더 RED로 확인했다. 1/1/0/0이었고 정정 전 결정이 `APPLIED`였다 |
| 집중 GREEN | FulfillmentPostgresTest 48, CompletionCoverageGatewayTest 20, SettlementCommandPostgresTest 15, ResponsibilityServiceTest 7, SettlementStateTest 4, DomainVocabularyContractTest 4 | 0 | 98/0/0/0 |
| 전체 `./mvnw -B -ntp -f backend/pom.xml clean package` | 전체 | 0 | 513/0/0/0, BUILD SUCCESS(기준선 509 + 새 4) |

native는 코드 commit `3addaefe`의 clean tree에서 순서대로 실행했다.
`python3 verification/actual/sN/build.py` 다음에
`$MULINO_SLOT ./verify actual-sN /tmp/s4k-native/sN/run`을 실행했다(S1은
build 없음). 첫 시도는 이 README가 untracked로 있어서 build.py가
"committed clean source"를 요구하며 exit 1로 멈췄다. 파일을 잠시 밖으로
옮긴 뒤 다시 실행했다.

| 실행 | build exit | verify exit | receipt status | codeCommit | receipt 사본 |
|---|---|---|---|---|---|
| actual-s4 | 0 | 0 | PASS | 3addaefe | raw/native-s4/run-receipt.json |
| actual-s3 | 0 | 0 | PASS | 3addaefe | raw/native-s3/run-receipt.json |
| actual-s2 | 0 | 0 | PASS | 3addaefe | raw/native-s2/run-receipt.json |
| actual-s1 | — | 0 | PASS | 3addaefe | raw/native-s1/run-receipt.json |

native 결과는 한정된 custody 증거다. coverage manifest의 PASS가 아니다.

schema·migration·contract는 바꾸지 않았다. assignment revision·nextAction·
basis는 기존 열이다. 그래서 schema parity 재생성도 하지 않았다.

## NOT_RUN

- NOT_RUN: `./verify scenarios --actual`. Step2 case 범위이며 coordinator
  통합 단계에서 실행한다. opus3[3] verdict는 인수 전에 이 실행을 요구한다.
- NOT_RUN: 위 DEFERRED 항목의 수정과 test.
