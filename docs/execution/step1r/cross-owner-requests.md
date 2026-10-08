# Step 1 skills가 요청하는 cross-owner 항목

Step 1 소유 경로(skills, `docs/execution/step1r/**`) 밖의 수정이 필요한
항목이다. 이 branch는 해당 파일을 고치지 않았다.

## 1. V4 노출 면 열거 subcase (Step 2 / V4·registry 소유자)

- 요구: 계획 §4.2·`D01`/`D08`의 "모든 쓰기 경로 열거"는 실행 중 시스템의
  `$metadata` entity set·action, MCP `tools/list`·`server/discover`, worker
  handler registry, 관리 endpoint를 읽어 경로 집합을 만들고 `$batch`·deep
  insert·upsert·draft를 시도해야 한다.
- 현재: `verification/cases/V4`는 고정 route inventory(92 subcase)이며 이
  열거를 구현하지 않는다. `raw-crud-*`는 가상 경로의 404와 행 불변만 본다.
  `V4/README.md`도 inventory가 "존재하는 endpoint 주장이 아니다"라고 적는다.
- 요청: V4 case 소유자가 열거 subcase를 추가하고, registry 소유자가
  `verification/cases/registry.json`의 `subcaseIds`·`expectedSubcases`를,
  catalog 소유자가 `mandatory-oracles.json` 연결을 갱신한다. 새 caseId는
  만들 수 없다(고정 41개).
- 그 전까지: skills는 V4의 노출 면 부분을 `NOT_RUN`으로 보고하게 한다.
  `acceptance-oracles.md`의 V4 행은 hash 잠금(`normative-contract-lock.json`)이라
  이 branch에서 고치지 않았다.

## 2. Step 1 담당 모델 표기 (coordinator)

- `docs/execution/claude-rereview-steps-1-2.md`(25행 부근 소유자 문구와
  `### step1` 제목)와 `docs/execution/task-status.md`(27행 부근)는 Step 1을
  GPT-6.1 Sol high로 적는다. `AGENTS.md`의 표(Step 1 = Claude Sonnet 5.5 high,
  `41d3c613`)와 다르다. 두 파일은 coordinator 문서라 이 branch에서 고치지 않았다.
