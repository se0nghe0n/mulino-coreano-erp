# T11 부모 부족 책임과 재평가의 결합 수정

기준선은 `a7f18cc8e7124b9fd1cc8bf811687ed7230a4981`이다.
격리 worktree는 `t3-862dcc5b`, branch는
`fix/t11-parent-shortfall-http-pg-a7`이다. D09/D11/D12의
부모100·기여90·잔여10 계약을 대상으로 한다.

## 원인과 수정

부모가 canonical 수령90 전체를 직접 인정한 뒤 자식의 기여30에 이어
기여60을 연결하면 전체 입력 snapshot은 달라진다. 계산한 부모90과
부족10의 근거는 그대로지만 `ResponsibilityService`가 첫 root의
전체 snapshot hash 일치를 요구했다. 그 결과 정상 재평가도
`HELD/OBLIGATION_RESOLUTION_UNDEFINED`로 rollback했다.

`sameCanonicalClosesBothChildrenAndPreservesParent90`의 세 변형에서
보인 assessmentId pointer 오류도 앞선 이 HELD 응답의 결과다.
pointer 자체나 fixture를 수정하는 문제가 아니다.

`AssessmentService`가 검증한 shortfall proof에
`shortfallBasisHash`를 추가한다. 전체 snapshot을 복사한 뒤 다음
조건을 모두 만족하는 기여 행만 이 별도 비교에서 제외한다.

- canonical occurrence가 평가 대상 Work의 직접 관측이다.
- 입력 fact의 sourceId·canonicalId·physicalScopeId가 그 원천의
  직접 실물 범위와 일치한다.
- 어느 property/relation fact도 해당 allocation ID를 근거로 쓰지 않는다.

자식의 `physicalScopeId=원범위|allocation ID`와 사용한 allocation의
수량·단위·시작점·원 GoalVersion 연결은 계속 hash에 포함한다.
목표·정의·정책·증거·시간·계산 입력도 그대로 비교한다.
전체 snapshot, inputSnapshotHash와 첫 root의 source/proof는 보존한다.

`ResponsibilityService`는 현재 proof·인가를 검증한 뒤 같은 부족량·단위와
같은 전체 capture 또는 같은 별도 근거 hash일 때만 첫 root의 현재
assignment를 반환한다. 의무의 해소·면제·재배정·양 변경은 하지 않는다.
별도 hash가 없는 기존 root는 원래 전체 hash 검사로 fail-closed한다.

기존 T11 author 입력, 110 assertion의 ID/op/expected/oracle/unit,
actor/grant/clock/fixture, 공개 계약과 공통 installer/observer는
수정하지 않았다. 필요한 변경은 제품 두 service와 T11 집중 검증이다.
`T11ParentHttp`는 원인 대조를 위해 GoalReferences/InputSnapshots/
Roots/Scopes/CommandRecords의 read-only SQL dump만 추가한다.

## 실행 증거

증거의 기준 경로는 이 worktree의
`.local/t11-parent-shortfall-fix/`다. 원본 r2 실행은 읽기 전용으로
사용하고 실패 artifact 17개를 `initial/`에 복사했다.
원본 경로와 SHA-256은 `initial/original-artifacts.json`에 기록한다.

새 baseline 재현은 `red-http-pg/`와
`.local/t17-source-pg-6eefe15a-1499-491d-8e8d-08c76f4a9efb/`에 있다.
네 실패만 선택한 JUnit 사례 4개는 4 FAIL, Maven/runner exit1이다.
원본과 동일한 HELD와 세 pointer 오류를 보존했다.
실행 JAR와 report는 `red-build/`에도 보존한다.

사용한 runner는 `verification/cases/T17/run_source_pg_test.py`다.
runner의 디렉터리·container·출력에 남은 T17 이름은 실제 이름이며
T17 기능 검증을 했다는 뜻이 아니다. `T17_PG_TEST_CLASS`로 T11만
선택했다. 이 실행은 scenario runner, Workflow, 실모델 호출이 아니다.

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PWD/backend/node_modules/.bin:$PATH"
export DOCKER_HOST=unix:///Users/se0nghe0n/.orbstack/run/docker.sock
npm ci --prefix backend --ignore-scripts --no-audit --no-fund
./mvnw -B -ntp -f backend/pom.xml \
  -Dtest=GoalShortfallBasisTest,GoalShortfallQuantityTest,ResponsibilitySubjectBindingTest \
  test
export T17_PG_TEST_CLASS=T11ParentContributionPostgresTest,T11GoalShortfallPostgresTest
export T11_PG_TEST=true
export T11_TEST_EVIDENCE="$PWD/.local/t11-parent-shortfall-fix/green-http-pg"
python3 verification/cases/T17/run_source_pg_test.py
```

JDK는 Homebrew OpenJDK21.0.12.1, Maven3.9.16, Node24.16.0,
npm11.13.0, Docker29.4.0이다. npm 의존성은 이 worktree의 lock으로
설치했다. PostgreSQL은 versions.json의 고정 digest를 사용한다.

unit JUnit28은 28 PASS/0 FAIL/0 skip, exit0이다. 새 근거 비교21,
기존 quantity4, subject binding3이다.

실제 HTTP/PG 결합 실행도 exit0이다. Surefire 원문은
`.local/t17-source-pg-31282a25-d765-496b-aa52-f3affaf460db/test.log`에
있다. 두 suite는 다음과 같다.

| 집중 suite | 실행 | 실패 | skip | 결과 |
|---|---:|---:|---:|---|
| T11ParentContributionPostgresTest | 22 | 0 | 0 | PASS |
| T11GoalShortfallPostgresTest | 23 | 0 | 0 | PASS |

원래 실패 네 경로의 새 assessment는 모두 APPLIED/UNSATISFIED다.
`completed-parent-diagnostics.json`은 독립 SQL dump의 첫/새 snapshot을
대조한다. 네 경로 모두 차이는 `facts.sources.work.Contributions`의
1행→2행뿐이며 그 밖의 전체 내용은 같다. 부모 quantity90의 원천·단위·
실물 범위, 첫 root의 immutable source/proof, 부족10의 OPEN assignment와
owner/다음 행동을 유지한다. 두 자식 종료와 원천 사건·수령·물량 효과
각1개, 잘못된 권한·proof·정정·현재 정책·rollback 반례도 통과했다.

실행 시 HEAD는 기준선 a7이고 제품·집중 test의 미commit 수정이 있었다.
runner의 `build-identity.json`과 최종 source를 대조한 drift는0이다.
`green-build/`에 실제 JAR, backend classes, harness test classes와
frozen source를 보존한다. `executed-class-hashes.json`은 class467개의
hash를 기록한다. `t11-input-hashes.json`의 입력33개와 공개 계약은
기준선과 동일하며 원래 assertion110을 변경하지 않았다.

artifact338개의 SHA-256은 `artifact-hashes.json`에 묶는다.
각 raw HTTP 응답과 read-only REPEATABLE READ SQL의 parameter·snapshot은
`green-http-pg/*/http-calls.json`과 `independent-db.json`에 있다.
command/exit는 runner `commands.json`과 `execution-context.json`에 있다.
이들은 집중 실행 증거이며 coverage ACTUAL receipt가 아니다.

| artifact | SHA-256 |
|---|---|
| 실제 green JAR | `e7d1aa124871daec8df9b153a756a4d2ae2d18d85be0d97bd1b163144338d19e` |
| artifact-hashes.json | `303dfa3826206f10630182e1351ceaa4a93bff7755c013b62fa7278f3ef468b7` |
| green build-identity.json | `0e589caf1285d1e3ff130dc3439382ab4969666088694a47ea635d79a8c67d81` |
| green cleanup.json | `5a96f0a41e29dd766728058af6cce08a513be8529edf653e560ee084aa33a61f` |
| red cleanup.json | `280a7f48a7489b804608509f288588fc0d8d90919f09d308a21edb433e972ba8` |

red와 green 모두 자기 container와 서버를 정리했다. 임시 private/public
key·blob이 없고 containerAbsent/serverStopped도 true다.

## 인수 범위

이 기록은 T11 수정의 집중 JUnit/HTTP/PG 증거다. 전체 backend,
41-case scenarios, Haiku native dynamic Workflow, 실제 client/model,
규제·BTP 인수와 coverage 조립은 이 작업에서 NOT_RUN이다.
Task·사용자 Step·S5·S6 종료를 주장하지 않는다. Root 통합·push·PR·
main 변경·Grok 알림은 수행하지 않는다.
