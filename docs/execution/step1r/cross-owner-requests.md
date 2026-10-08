# Step 1 skills가 요청하는 cross-owner 항목

Step 1 소유 경로(skills, `docs/execution/step1r/**`) 밖의 수정이 필요한
항목이다. 이 branch는 해당 파일을 고치지 않았다. 상태는
`48d616e3`(Step 2 재검토 2차 통합) 기준이다.

## 1. V4 노출 면 열거 (harness·catalog·규범 lock 소유자)

- 요구 근거: 계획 §4.2는 금지된 core UPDATE를 막고 "실제 노출된
  direct/nested/batch/projection 경로 모두 검사한다"고 쓰며, §13.2 V4 행은
  "READ grant로 direct/nested/batch/projection/MCP/worker 대체경로 호출"을
  요구한다. 계획 본문에는 `$metadata`·`tools/list`·deep insert·draft 같은 열거
  수단이 없다. 이 수단은 Step 1 skill이 CAP 노출 면을 근거로 구체화한 것이다.
- 해소된 부분: V4에 열거 subcase `exposed-write-surface`가 추가됐다
  (`eded4248`, registry의 V4 subcase 93개).
- 남은 요청:
  - harness 소유자: `enumerateWriteSurface`를
    `contracts/acceptance-host-observation.schema.json`의 operation 목록과
    `verification/host-observation-guide.md`에 추가한다. 없으면 이 subcase는
    `NOT_IMPLEMENTED`이고 V4의 노출 면 부분은 `NOT_RUN`이다(`V4/README.md`).
  - catalog 소유자: `mandatory-oracles.json`의 `V4.all-alternate-write-paths`
    same-auth-path 연결을 새 subcase로 갱신한다.
  - 규범 lock 소유자: `acceptance-oracles.md`의 V4 행은 hash 잠금
    (`normative-contract-lock.json`)이라 이 branch에서 고치지 않았다. 열거 요구를
    그 행에 반영할지 결정한다.
- 그 전까지 skills는 V4의 노출 면 부분을 `NOT_RUN`으로 보고하게 한다.

## 2. coverage receipt producer (해소)

이전 요청("실행 receipt를 만드는 도구가 없어 coverage PASS를 만들 수 없다")은
Step 2 재검토 2차의 `22e72344`(`ExecutionReceiptProducer`)로 해소됐다. skill은
그 조건과 `NOT_EMITTED` 이유를 반영했다. 남은 한계는 요청이 아니라 사실이다.
현재 actual driver는 `api`·`fixture`·`db` adapter만 공급하고, native
`actual-sN` custody와 `--actual` profile 실행의 연결은 Step 3(coverage·actual
소유자) 몫이다(`verification/coverage/README.md`).

## 3. 계획 §13.4의 낡은 증거 경로 (계획 소유자)

§13.4는 증거를 `verification/manifest.json`에 기록한다고 쓰고 `./verify`
entrypoint를 "지금 실행 가능한 명령이 아니다"라고 한다. 실제 저장소는
`verification/harness/target/evidence/runtime-manifest.json`을 `assemble.py`가
만들고 `./verify`가 이미 있다. skill은 실제 pipeline을 따르게 했으며 계획 본문
정정은 계획 소유자에게 요청한다.

## 4. 해소된 항목

- Step 1 담당 모델 표기: `docs/execution/task-status.md`와
  `docs/execution/claude-rereview-steps-1-2.md`가 이미 Claude Sonnet 5.5 high로
  바로잡혀 있어 요청을 닫는다.
