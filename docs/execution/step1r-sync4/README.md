# Step 1 개발 skills의 harness 동기화

Step 2의 새 준비 gate와 관찰 계약을 빠뜨린 skill로 case를 작성하면
준비 실패를 제품 결함으로 오해하거나 요청 echo를 실행 증거로 삼게 된다.
현재 harness·contract의 단일 원본을 가리키도록 개발 skills를 동기화한다.
제품 실행·Step closure를 완료했다는 기록은 아니다.

- worker: 사용자 Step 1, GPT-6.1 Sol high다.
- baseline: `d803e1d60426fa8293129f863eb29b8a0de19ddf`다.
- branch/worktree: `step1r/sync4`,
  `/Volumes/VideoStore/Developer/mulino-ontology-step1r-sync4`다.
- 소유 범위: 개발 skill 세 개와 이 README다. 다른 worktree와
  verification·contracts·backend·계획은 수정하지 않았다.
- 읽은 기록: `step1r/**`, `claude-rereview-steps-1-2.md`,
  `step2r-harness3/README.md`, `step2r-cases3/README.md`,
  `step2r-round4/README.md`다. 기록의 주장도 현재 파일과 대조했다.

## 동기화 항목별 판정

1. FIXED — `caseAssetChecks` 11개를 현재 이름으로 연결한다.
   `PreparationAssetChecks.java`의 CHECKS와 harness-guide prepare 절을
   대조했다. vocabulary는 `check_vocabulary.py --check`와
   `domain-vocabulary.json`·`audit-observation-fields.json`을 사용한다.
   PENDING은 owner가 있는 `knownOpenGaps`로 남기고 검사 실패는 닫는다.
   `assemble.py.vocabulary()`도 같은 review를 사용한다.
2. FIXED — 감사 논리 원행을 공개 계약으로 연결한다. `audit`와
   `queryAudit`, presence·ALWAYS-only where·outcome/errorCode의 구별,
   퇴역 이름을 `audit-observation-fields.json`과 harness-guide의
   observer 절에서 확인했다. `ContractValidator.auditFieldProblems`가
   공개 계약 밖 이름을 준비 문제로 판정한다. V2 promiseCoverage도 그 절을 가리킨다.
3. FIXED — runtimeProfile 두 flag와 자율 loop 패턴을 연결한다.
   fixture schema·host guide·`runtimeProfileProblems`를 확인했다.
   true/true와 false/false를 구별하고 stop/clock advance/parallel
   watcher branch 0·start 순서를 따른다. parallel은 barrier가 아니다.
4. FIXED — scheduler 제출 증거를 요청 echo와 구별한다.
   host schema·guide·`HostObservationValidator.naturalTick`을 대조했다.
   tick/sweep operationEvidence의 watcher parameter echo는 거부되며
   `rawRows.schedulerSubmissions`의 실제 제출 행과 identity를 대조한다.
5. FIXED — PREPARATION 입력 네 field를 연결한다.
   `codeCommit`·`workingTreeDirty`·`checkoutCommit`·`checkoutDirty`의
   형식/준비 보고 결속은 validator, 현재 commit·두 tree clean 판정은
   T25 assertion이라는 구별을 host guide·`preparationInput`에서 확인했다.
6. FIXED — binding `--check`, derived-binding stamp, layer-route,
   생성기 임시 복사본 재현이 prepare gate라는 사실을 연결한다.
   현재 source에는 layer gap·C3 stamp의 옛 KNOWN_OPEN이 없다.
7. FIXED — MRTR requestState의 개발/CI TTL 600초를 protocol reference에
   추가한다. `s0-protocol.md`의 S5 절과 `author_cases.py`를 확인했다.
   발급 후 599초 정상·601초 만료이며 운영 SLA로 확정하지 않는다.
8. FIXED — harness3 이후의 receipt 조건과 V4 상태도 동기화한다.
   preRun·부분 실행 receipt 거부·label 값 검사·선언된 build identity,
   model/deployment manifest·regulatory gate·RESULT_REVISION 재계산을
   현재 guide와 구현에서 확인했다. enumerateWriteSurface 계약은 정의됐고
   actual adapter는 없다. 모델/reviewer 표는 AGENTS.md를 계속 가리킨다.

## 남은 P3 일곱 개의 재검증

번호는 `claude-rereview-steps-1-2.md`의 "남은 P3" 순서다.

1. NOT_A_DEFECT — nextCursor를 목록에 한정한다는 지적이다. baseline의
   implementation-contracts.md는 계획 §3.4처럼 모든 조회 응답에 둔다고
   이미 명시한다. 이번 변경에서 축소하지 않았다.
2. NOT_A_DEFECT — validate.py의 현재 입력 검증 조건이 빠졌다는 지적이다.
   baseline repository-harness.md의 PASS 규칙에 이미 exit0·현재 입력
   대조가 있다. 이번에는 세 SKILL.md의 요약도 같은 조건으로 맞춘다.
3. NOT_A_DEFECT — caseId pattern이 고정 41개를 강제한다고 한다는
   지적이다. baseline reference는 pattern이 형태만 검사하며 Main.prepare의
   expected 집합·registry가 집합을 강제한다고 이미 구별한다.
4. FIXED — Gherkin 양식의 주석은 subcase 추가를 말하지만 본문에는
   기능 줄이 남아 있다. 그 줄을 지워 기존 Feature에 붙이는 Scenario
   블록으로 맞춘다. 새 case ID나 실행 문법을 만들지 않았다.
5. NOT_A_DEFECT — 수동 V4 근거를 PR description에 두라는 지적이다.
   baseline reference는 이미 Task handoff·checks를 가리킨다.
6. CROSS_OWNER — 과거 cross-owner 문서의 요구 과장은 현재 계획과
   대조하면 결함이 아니다. 다만 V4 요청 소유자에서 coverage가 빠진
   점은 남아 있다. `docs/execution/step1r/cross-owner-requests.md`는
   이번 소유 범위 밖이다. 아래 요청에서 현재 소유자와 해소 범위를 적는다.
7. NOT_A_DEFECT — 과거 cross-owner 문서의 계획 인용이 없다는 지적이다.
   현재 계획 §4.2에 "실제 노출된 direct/nested/batch/projection 경로 모두
   검사한다"가 그대로 있다. §13.2 V4 행의 READ grant 경로 인용도
   일치한다. 원문을 지우거나 더 약한 oracle로 바꾸지 않는다.

## Checks

검증 시점은 baseline 위 skill 문서 수정 tree다. 모든 명령은 지정
worktree에서 실행했다. 제품 runtime의 PASS 증거는 없다.

- `python3 -I verification/cases/check_vocabulary.py --check`: exit0,
  VALID·problems 0·knownOpen 34다. 전부 Step 3 vocabulary owner다.
- `python3 -I verification/requirements/check_layer_routes.py`: exit0,
  VALID·unexplainedGaps 0·knownOpen 0·problems 0다.
- `python3 -I verification/requirements/check_derived_bindings.py`: exit0,
  VALID·files 5·knownOpen 0·problems 0다.
- `python3 -I verification/requirements/validate_catalog.py`: exit0,
  structure VALID·41 cases·122 oracles·499 observations다.
  runtimeCoverage는 NOT_RUN이다. hash-lock된 acceptance-oracles.md를
  수정하지 않았다.
- `python3 -I verification/platform/inventory/check.py`: exit0,
  6 checks PASS·runtimeRestore NOT_RUN이다. skills의 전용 validator는
  아니다. 보존 문서·소유 파일 hash 검사에 영향이 없음을 확인한다.
- skill 전용 실행 validator는 repo에 없다. `.agents/skills/**/scripts`와
  verification을 검색했고 `verification/skills-tests/README.md`는 제품
  host 인수 계약이다. inline Python으로 frontmatter·상대 링크·symlink·
  양식·소유 범위를 검사했다. Python 3.14.7·exit0이며 frontmatter 3개,
  상대 링크 35개(broken 0), 개발 skill symlink 3개와 현재 전체 Claude
  discovery symlink의 resolve가 정상이다. 양식은 Scenario 1개·Feature
  0개이며 CHECKS의 11개 이름과 reference가 일치한다. 변경 9개 파일은
  모두 소유 범위 안이다.
- `rg -n 'caseAssetChecks|manifest\.json|PR description|Sol|Astra'`를
  개발 skill 세 디렉터리에 실행했다. caseAssetChecks는 11개이고
  manifest 일치는 현재 runtime/harness/preparation 산출물뿐이다.
  옛 `verification/manifest.json`·PR description·Sol/Astra 표는 없다.
- `git diff --check`: exit0이며 공백 오류가 없다.

## Cross-owner 요청과 해소된 옛 요청

- 기록 소유자/coordinator: 과거 `step1r/cross-owner-requests.md`의 V4
  요청에 coverage 소유자를 포함하고 기준 commit 이후 상태를 연결한다.
  계획 인용 자체는 일치한다. 계획의 경로별 oracle와 skill의 열거 수단을
  계속 구별한다. 이 README가 이번 상태 갱신이며 과거 기록은 수정하지 않았다.
- Step 3 host/actual adapter + Step 2 harness·catalog·coverage 소유자:
  V4 enumerateWriteSurface를 실제 실행하고 열거의 미실행이 전체 V4 인수로
  세어지지 않게 함께 확인한다. schema·guide 정의 부재 요청은 해소됐다.
  catalog의 same-auth-path와 열거 assertion 연결도 이미 존재한다.
  실제 adapter·관찰 bytes·coverage 인수만 남는다.
- Step 3 observer/host 소유자: RESULT_REVISION 독립 재계산, 감사 원행과
  promiseCoverage, schedulerSubmissions, PREPARATION 입력 네 field를
  현재 계약대로 생산하기 전까지 해당 관찰은 NOT_RUN이다. scope 밖
  구현을 이 worker가 추가하지 않았다.
- Step 3 backend/vocabulary 소유자: queryAudit의 PENDING_PRODUCT·감사
  PENDING field와 vocabulary PENDING 34건을 해소할 때 계약·case 검사를
  함께 갱신한다. 검사 exit0은 현재 제품이 이를 구현했다는 뜻이 아니다.
- 계획 소유자: §9.1의 `contracts/mcp-errors.md`와 §13.4의
  `verification/manifest.json`·미존재 wrapper 설명은 현재 파일과 다르다.
  실제 오류 계약은 `contracts/mcp/s0-protocol.md`, 실행 증거는 coverage
  pipeline이다. 계획·normative lock 변경은 소유자 검토에 맡긴다.

## 하지 않은 일

Maven·`./verify` 전체 suite·prepare·coverage·제품 runtime·실모델·client·
BTP·규제 실행은 하지 않았다. coordinator가 통합본의 결합 checks와
adversarial closure review를 수행한다. push·merge·PR 생성은 하지 않았다.
