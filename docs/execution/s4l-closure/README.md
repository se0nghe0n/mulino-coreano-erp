# S4l 종료 리뷰 4 지적 수정 기록

사용자 Step3(Claude Opus medium) 구현 worker의 기록이다. 기준선은 tag
`step3-s4l-baseline`(`69b2b8ef`, backend 513 PASS, harness 507 PASS,
native S4/S3/S2/S1 PASS)이고, branch는 `step3/s4l-closure4`, worktree는
`/Volumes/VideoStore/Developer/mulino-ontology-step3-s4l`다. 소유 범위는
`backend/**`, `verification/actual/**`,
`verification/harness/src/main/java/org/mulino/verification/actual/**`다.
이번 수정은 `backend/**`만 바꿨다. coordinator만 Task branch에 통합한다.

입력은 skeptic이 검증한
`/Volumes/VideoStore/Developer/.mulino-tools/s4-closure-4-verdicts.json`이다
(claims `s4-closure-4-claims.json`, 전체 review `s4-closure-4-opus.json`,
`s4-closure-4-astra.json`). 계획 근거는
`docs/ontology-implementation-plan.md`의 다음 절이다.

- §4.3 129행. 정정 명령은 영향받는 판정·의무를 같은 처리 단위에서
  재평가한다.
- §4.3 134행. 의무의 해소/면제 결정이 이미 유효하면 자동 부활시키지
  않는다.
- §5.1 가져온 업무의 원 사건을 다시 쓰지 않는다.
- §5.3과 246행. `waiveObligation`은 현재 scope/revision을 검사하고 현재
  책임의 단절/중복을 거부한다.
- §6 정산.

## 판정

| 항목 | 판정 | 원인과 수정 | test (RED → GREEN) |
|---|---|---|---|
| index 0 (P3) 복원 link와 복원 전 면제의 경합 | NOT_A_DEFECT (REFUTED) | 모든 guarded command는 `ApplicationCommands.apply`에서 organization row를 `FOR UPDATE`로 잡고 commit까지 유지한다(`PolicyCommandGuard.fence` → `PolicyRepository.fence`). 그래서 두 command 본문은 직렬화된다. 이 의존을 `SettlementContributionReview.contributionChanged`의 재발행 지점에 주석으로 남겼다. fence가 바뀌면 경합이 다시 열린다는 경고다. | 새 test 없음(brief가 주석 또는 test 하나를 허용). |
| MUST index 1/5 (P2) `waivedResidual`가 실제로 면제한 잔여가 아니라 root를 연 시점 금액과 비교 | FIXED | 이전에는 R0의 불변 `originalDifference`, CURRENT root는 open 시점 `settlementDifference`와 비교했다. 그래서 조정 뒤 R0를 면제한 경우에도 복원된 잔여가 "이미 면제"로 막혔다. 이제 `ResponsibilityService.waive`가 kind owner에게 실행 시점 coverage를 묻는다(`ResponsibilityEvidence.waiverCoverage` → Gate → `ResponsibilityKindEvidence.waiverCoverage`, 둘 다 default null). 답은 WAIVED assignment basis 끝에 붙인다. `SettlementResponsibilities.waiverCoverage`는 공유 predicate로 이 Match를 평가한다. CURRENT이면 `[SETTLEMENT_COVERED CURRENT <잔여>]`, CHANGED/UNVERIFIED나 정정 송장이면 금액 없이 상태만 남긴다. `waivedResidual`은 이 server suffix의 CURRENT 잔여와만 비교한다. 기록이 없거나 CURRENT가 아니면 막지 않고 root를 연다(fail-closed). basis가 500자를 넘으면 `INVALID`로 거부한다. schema는 바꾸지 않았다. | `FulfillmentPostgresTest.restorationReopensAResidualLargerThanTheAdjustedOriginalWaiverCovered`(astra: 3 → +1 → R0 면제(2) → 28 → −1 → R1 면제 → 30). `...restorationReopensAResidualThatTheZeroResidualWaiverNeverCovered`(opus: +3 → R0 면제(0) → 28 → −3 → R1 면제 → 30, basis `[SETTLEMENT_COVERED CURRENT 0]` 확인). RED: 둘 다 복원 뒤 OPEN 0. GREEN: OPEN 1(source=복원 canonical, owner·nextAction·nextCheck), 조회 CURRENT/UNSATISFIED/differenceDutyOpen=true. 조정 없는 기존 `restorationToAnAlreadyWaivedOriginalDifferenceDoesNotReviveIt`는 그대로 막힌다(PASS). |
| MUST index 4/6 (P2) `affected()`의 IMPORTED 제외가 직접 정정 대상 Work의 후속 의무까지 버림 | FIXED | `affected()`는 이제 IMPORTED Work를 빼지 않는다. `AssessmentCorrectionImpact.apply`가 IMPORTED Work에 대해서는 `service.invalidate`만 건너뛴다. 정정 의무(`ensureCorrectionDuty`/`ensureEvidenceCorrectionDuty`)는 그대로 연다. CLOSED이면 기존 `ensureFollowup`의 COMMAND follow-up이 맡는다. 상태가 ACTIVE/WAITING/CLOSED가 아니어서 의무를 둘 수 없으면 `NEEDS_INPUT/IMPORTED_WORK`로 정정 전체를 rollback한다. 조용히 버리지 않는다. `evidenceLinked`는 무효화만 하므로 IMPORTED Work를 건너뛴다. round 3 동작과 같다. | `CompletionCoverageGatewayTest.correctingEvidenceThatResolvedAnImportedWorkDutyOpensAnOwnedFollowup`(새, 해소 뒤 정정). RED: OPEN FOLLOWUP_REVIEW 0. GREEN: 정정 APPLIED, Work revision·pendingInvalidation 불변, 해소된 assignment는 RESOLVED 유지, 정정된 canonical을 source로 하는 FOLLOWUP_REVIEW 1건 OPEN(owner·nextAction·nextCheck). round 3 test `verifiedLinkSkipsAnImmutableImportedWorkInsteadOfFailingOnItsTrigger`도 PASS(raw trigger 오류 없음). |
| SHOULD index 2 (P3) 같은 수량 정정이 `reissueOpen`을 부름 | FIXED | CURRENT 경로에서 두 조건이 맞으면 재발행하지 않는다. 정정 canonical이 직접 대체한 canonical과 수량·단위가 같아야 한다. 열린 root가 모두 Match 자신의 root이거나 CURRENT 기여로 열린 root여야 한다. CHANGED/UNVERIFIED로 열린 root가 있거나 앞 canonical을 모르면 기존처럼 재발행한다(fail-closed). | `FulfillmentPostgresTest.sameQuantityCorrectionDoesNotReissueTheOpenDifferenceRoot`. RED: 30→30 정정 뒤 revision +1. GREEN: revision 불변, 정정 전에 결정한 면제가 APPLIED, OPEN 0. |
| SHOULD index 3 (P3) 무효화 정정의 claim relink가 UNVERIFIED root U 옆에 두 번째 root를 엶 | FIXED(adopt) / DEFERRED(invalidatesId 재도출) | relink는 비CURRENT 경로에서 새 root를 열기 전에 같은 Match의 열린 root를 찾는다. 조건은 셋이다. root가 `contributionState=UNVERIFIED`이고, `contributionCanonicalId`가 새 chain에 있고, 지금 canonical과 달라야 한다. 찾으면 그 root를 `reissueOpen`(basis `CONTRIBUTION_RELINKED:<canonical>`)으로 이어받는다. owner와 work는 그대로다. 복원 해소는 U의 opened canonical 뒤 chain으로 계속 판정된다. | `FulfillmentPostgresTest.relinkingAnInvalidatingCorrectionAdoptsItsUnverifiedRoot`. RED: OPEN 2(U와 새 root). GREEN: OPEN 1이 U이고 revision이 올랐다. |

## DEFERRED 잔여

| 항목 | 이유 | owner · 맡을 Step |
|---|---|---|
| index 3 후반: `invalidatesId`가 `supersedesId`와 다른 정정에서 무효화된 event의 canonical을 재도출 | 현재 test 경로에서 `invalidatesId`는 `supersedesId`와 같다. 그래서 재도출 대상이 이미 포함된다. 서로 다른 같은 subject event 둘(예: 한 dispatch의 부분 인도 두 건)로 RED를 먼저 관찰해야 고칠 수 있다. 이번에는 그 fixture를 만들지 않았다. fail-closed다. 남는 것은 FOLLOWUP_REVIEW와 UNVERIFIED 조회이고, 조용한 SATISFIED는 없다. | settlement/evidence owner · S5 진입 backlog |

## 실행 증거

환경: `. /Volumes/VideoStore/Developer/.mulino-tools/env.sh`, `backend/`에서
`npm ci`(exit 0), `$MULINO_SLOT`, Testcontainers PostgreSQL. 집중 명령은
`$MULINO_SLOT ./mvnw -B -ntp -f backend/pom.xml -Dmulino.evidence.blob-root=$(mktemp -d) -Dtest=<classes> test`다.
결과는 run/fail/error/skip 순서다. 발췌는 `raw/`에 있다.

| 시점 | classes | exit | 결과 |
|---|---|---|---|
| RED: 새 test + baseline 제품 코드(test 먼저 작성, `backend/src/main` 미변경) | FulfillmentPostgresTest 새 4개, CompletionCoverageGatewayTest 새 1개 | 1 | 5/5/0/0. 위 표의 RED 열이다. `raw/red-focused.txt` |
| 집중 GREEN | FulfillmentPostgresTest 52, CompletionCoverageGatewayTest 21, SettlementCommandPostgresTest 15, ResponsibilityEvidenceGateTest 8, ResponsibilityServiceTest 7, EvidenceReconciliationTest 6, SettlementStateTest 4, DomainVocabularyContractTest 4 | 0 | 117/0/0/0. `raw/green-focused.txt` |
| 전체 `./mvnw -B -ntp -f backend/pom.xml clean package` | 전체 | 0 | 518/0/0/0, BUILD SUCCESS(기준선 513 + 새 5). `raw/full-package.txt` |

native는 코드 commit `8d201c77`의 clean tree에서 순서대로 실행했다.
`python3 verification/actual/sN/build.py` 다음에
`$MULINO_SLOT ./verify actual-sN /tmp/s4l-native/sN/run`을 실행했다(S1은
build 없음). 이 README는 native 실행 뒤에 작성했다.

| 실행 | build exit | verify exit | receipt status | codeCommit | receipt 사본 |
|---|---|---|---|---|---|
| actual-s4 | 0 | 0 | PASS | 8d201c77 | raw/native-s4/run-receipt.json |
| actual-s3 | 0 | 0 | PASS | 8d201c77 | raw/native-s3/run-receipt.json |
| actual-s2 | 0 | 0 | PASS | 8d201c77 | raw/native-s2/run-receipt.json |
| actual-s1 | — | 0 | PASS | 8d201c77 | raw/native-s1/run-receipt.json |

native 결과는 한정된 custody 증거다. coverage manifest의 PASS가 아니다.

schema·migration·contract는 바꾸지 않았다. 면제 coverage는 기존
`basis varchar(500)` 열에, 재발행·이어받기 기록은 기존 assignment
revision·nextAction·basis 열에 둔다. 그래서 schema parity 재생성은
하지 않았다.

## NOT_RUN

- NOT_RUN: `./verify scenarios --actual`과 harness 전체. Step2 case
  범위이며 coordinator 통합 단계에서 실행한다.
- NOT_RUN: 위 DEFERRED 항목의 수정과 test.
- NOT_RUN: index 0 전용 동시성 test. brief에 따라 주석으로 대신했다.
