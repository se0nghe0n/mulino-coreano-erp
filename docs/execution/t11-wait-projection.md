# T11 Work 대기 필드 projection을 보완한다

Task와 현재 Step은 OPEN이다. baseline은
`7e1236bf1573a526630054890ece8d0193ad83f1`이다.
`fix/t11-wait-projection`의 배정 worktree에서만 수정한다.
실제 thread 설정은 `codex/gpt-6.1-sol`, effort `medium`,
`serviceTier=default`다.

## 저장된 원천과 변경

읽기 전용 fresh 자료는 다음 경로다.

`/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-6c163226/.local/standard-integrated-wave-20261023/T11/scenarios.json`

이 report의 `wait-timeout.actions`는 배열이 아닌 객체다.
`actions.wait.response`는 revision 2의 APPLIED다. 원본 case의
waitWork request는 `wait.verifier=$alias A`를 전달한다. 제품
WorkLifecycle은 이 wait 객체를 waitJson으로 저장한다.
`actions.after-db.data.rawRows.works`에서 SOURCE
`89712d34-932e-4bd2-928c-40e01dc1ab19`의 revision 2는 WAITING이며
waitJson에 다음 값을 실제 포함한다.

- `verifier`: `1feb8408-bc65-434c-ba71-a26344b85651`
- `overdueAction`: `ESCALATE_SUPERVISOR`

같은 행의 supervisorId는 다른 ID다. sourceEvidence.works의 SQL은
organizationId와 itemId, `recordedAt <= knownAt`으로 Works를 읽는다.
knownAt은 `2026-10-07T04:00:00Z`다. 원본 실패는
`wait-remains: projected field missing: verifierId`다.
원본 report를 수정하거나 이 문서의 기대값을 원천으로 사용하지 않는다.

Works projection에서 `waitJson.verifier`를 verifierId로,
`waitJson.overdueAction`을 overdueAction으로 복사한다. 기존 waitReason과
closeReason의 transition 처리와 Goal/Input projection은 보존한다.
새 두 필드는 현재 Work waitJson만 사용한다. 누락·malformed JSON은
결손으로 남기며 transition snapshot fallback은 추가하지 않는다.
supervisor·actor·기대값으로 보충하지 않는다. SQL, Work 상태, 의무,
자동 승인 및 권한 판정은 변경하지 않는다.

## 집중 검증과 한계

- ScenarioObservationProjectionTest: 6 tests, 실패·오류·skip 0이다.
  임의의 verifier와 overdueAction도 원문 그대로 복사함을 확인한다.
- 새 PostgreSQL projection 검사: 11 tests, 실패·오류·skip 0이다.
  저장 waitJson의 독립 SQL 대조, revision 보존, 누락·malformed·부분 JSON,
  다른 Work·조직·revision 및 knownAt 이후 원천의 미투영을 확인한다.
  정확한 revision의 transition snapshot도 누락값 대체에 쓰지 않는다.
- 기존 READONLY/MVCC 검사: 1 test, 실패·오류·skip 0이다.
  REPEATABLE_READ 중 concurrent commit 전 상태 보존과 쓰기 거부를
  확인한다. PostgreSQL은 로컬 `postgres:18.6` 이미지다.
- `./verify prepare`: exit 0이다. 준비 성공이며 runtime 인수는 아니다.
- T11 입력 22개 파일은 baseline과 byte가 일치한다. 원래 assertion
  110개를 보존한다. Goal/Input 변경이 포함된 baseline을 유지한다.

PostgreSQL 검사는 CSN 형태의 disposable source-row 검사다.
시나리오 실행이나 원본 고정 DB 재실행으로 부르지 않는다.
`./verify scenarios --actual`은 별도 Haiku Workflow 소유이며 NOT_RUN이다.
원본 T11 3 PASS / 9 FAIL과 assertion 31 PASS / 8 FAIL / 71 UNOBSERVED를
이번 집중 검사 결과로 갱신하지 않는다. 다른 T11 실패는 범위 밖이다.

명령·exit·count·입력/source/class hash·SQL 관찰·cleanup 증거는
`.local/t11-wait-projection/handoff.json`과 같은 디렉터리에 기록한다.
두 disposable PG container와 test schema는 제거했다.
첫 PG 실행에 로컬에 없는 `postgres:18.3`을 지정해 registry pull이
시작됐으나 exit 130으로 중단했다. 실제 검사는 기존 로컬 이미지에
`--pull=never`를 지정했다. push·merge·Grok 알림은 수행하지 않는다.

## Task branch 결합

coordinator가24c96df0를648ca638에 통합했다. 원래 T11 입력22개와
assertion110개, 원본 report의 실제 waitJson을 독립 대조했다.
root에서 기존 Goal/Input 관계와 함께 observer PG64개·unit6개가 모두
PASS했다. 실패·오류·SKIP0, Maven exit0이며 source3,161개 drift0과
실행 class hash 일치, 자체 PG container 부재를 확인했다.
prepare는41 case·802 subcase·24,260 assertion·문제0·asset12 PASS다.
`.local/resume-20261023/t11-wait-integrated/`에 실행 증거를 남겼다.

이는 집중 결합 검증이다. 원래 T11 시나리오 결과는 아직 갱신하지
않았으며 전체 Task와 현재 Step은 OPEN이다. 별도 Haiku T09/T17 실행은
7e1236bf로 고정돼 이 후속 변경을 포함하지 않는다.
