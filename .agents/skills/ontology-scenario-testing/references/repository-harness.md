# 저장소 harness와 실행 증거

이 파일은 `./verify`·Gherkin·증거가 저장소에서 실제로 하는 일만 적는다.
전체 계약은 [harness 가이드](../../../../verification/harness-guide.md),
[coverage README](../../../../verification/coverage/README.md)다.
명령이나 schema가 바뀌면 그 문서가 우선이다.

## 명령과 exit code

| 명령 | 의미 |
|---|---|
| `./verify validate <case.json>` | JSON Schema·semantic 검증. 제품 PASS 아님 |
| `./verify contract-red <case.json>` | 미구현 driver로 실행한 의미 있는 RED. 정상 exit1 |
| `./verify prepare`, `./verify coverage` | case↔assertion↔Gherkin·registry·catalog 연결. 결과 `PREPARED` |
| `./verify scenarios --actual` | loopback 실제 backend와 disposable DB 필요(`ACTUAL_BASE_URL`, `ACTUAL_DISPOSABLE_DATABASE=true`, `DB_URL` 등). `--actual` 없으면 `NOT_RUN` exit2 |
| `./verify actual-s1`…`actual-s4` | `verification/actual/sN/run.sh`: 복사한 빌드, disposable PostgreSQL, native flow 실행. 커밋된 clean tree가 필요하고 s2–s4는 먼저 `python3 verification/actual/sN/build.py`로 custody를 만든다 |
| `./verify harness` | harness selftest. 제품 증거가 아니다 |

exit0 harness/준비 성공, exit1 assertion·계약 실패, exit2 필수 경로
`NOT_RUN`, exit3 환경·형식·discovery 오류(의미 있는 RED가 아님).
`--actual`은 `harness/contract-red/prepare/coverage/validate/model/deployment`와
함께 쓸 수 없다.

## case 한 개의 구성

`verification/cases/<ID>/`의 `case.json`, `fixture.json`(또는 `fixtures/`),
`scenario.feature`가 한 벌이다. `case.json`의 subcase 하나가 Scenario 하나다.
action은 JSON 선언 순서대로, assertion은 모두 Gherkin에 쓴다. case는
`verification/cases/registry.json`에 정확히 등록하고 `oracleRef`는 독립
catalog(`verification/requirements/mandatory-oracles.json`)의 항목에 연결한다.
case-local capability를 만들지 않는다. 공통 schema·runner·registry·catalog는 각 소유자가
고친다. 완전한 예는 `verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/`와
`verification/cases/E1/`이다. 새 case는 이 파일들을 복사해 시작한다.
`./verify validate <case.json>` → `./verify contract-red <case.json>` →
`./verify prepare` 후에만 case가 있다고 보고한다.

## Gherkin 문법

`ScenarioGlue`의 제품 단계는 세 가지뿐이다. 다른 문장은 undefined step이다.

```gherkin
먼저 사례 파일 "<case.json 경로>"의 "<subcase id>"를 준비한다
만일 "<actorRef>" 역할이 "<action id>" 행동을 수행한다
그러면 "<assertion id>" assertion으로 "<업무 수량·단위·판정 설명>"를 확인한다
```

첫 단계는 준비 단계이고 action 단계는 선언 순서와 같다. 다음 두 가지는 기계가
검증하지 않으므로 review가 지킨다.

- 세 번째 인자(`humanExplanation`)는 glue가 비교하지 않는다. 업무 독자가
  Gherkin만 읽어도 알도록 수량·단위·판정·owner를 쓴다
  (`"W 현재 보유 80 BOX = 100−30+10"`). assertion ID나 `"확인한다"`만 쓰지 않는다.
  이 값은 `case.json`의 `expected`·`unit`과 같아야 한다.
- `HARNESS-EXAMPLE`의 canned 단계(`고정 관찰 표본과…`)는 harness selftest 전용이다.

## NOT_RUN과 태그

`@pending`은 runner 기능이 아니다. 쓰지 않는다.

- 사례 Gherkin은 `case.json` 옆 `scenario.feature`를 file selector로 고르므로
  `@contract-red` 태그가 필요 없다. 이 태그는 HARNESS-EXAMPLE suite를 가르는
  JUnit 필터이며 NOT_RUN 표지가 아니다.
- 미구현 case는 `NOT_IMPLEMENTED` assertion RED(exit1)이고 skip은 금지(skip0)다.
  발견·시작·`NOT_IMPLEMENTED` 실패 수가 모두 subcase 수와 같아야 한다.
- `--actual` 실행에서 `NOT_IMPLEMENTED`만 실패한 scenario는 `NOT_RUN`으로
  기록된다. 위반이 하나라도 있으면 `FAIL`이다.
- profile 미실행은 `./verify` exit2와 `runtime-manifest.json`의 `NOT_RUN`이다.

## 증거 pipeline과 증거 class

Gherkin·`./verify prepare`는 `PREPARED`일 뿐 실행 PASS가 아니다. 기록 위치는
`verification/manifest.json`이 아니다(이 파일은 없다).

| 단계 | 산출물·도구 |
|---|---|
| 실제 실행 | `verification/actual/sN/run.sh`가 쓰는 `target/evidence/actual-sN-run-<uuid>/run-receipt.json`(`S4_DISPOSABLE_RUN_RECEIPT`), `actual-sN-native.json`, 정리 결과와 artifact hash |
| 입력 index | `verification/coverage/runtime-evidence-index.json`([schema](../../../../verification/coverage/runtime-evidence-index.schema.json)) |
| coverage receipt | [execution-receipt.schema.json](../../../../verification/coverage/execution-receipt.schema.json): `evidenceClass=ACTUAL`, `codeCommit`, `executionIdentity`, `command`, `versions`, `reportArtifact`, `inputs`, `artifacts` |
| 조립·검증 | `python3 verification/coverage/assemble.py` → `target/evidence/runtime-manifest.json`([schema](../../../../verification/coverage/runtime-manifest.schema.json)); `python3 verification/coverage/validate.py` |
| 준비 기록 | `verification/harness-manifest.json`, `verification/cases/<ID>/evidence/preparation/manifest.json` |

native `run-receipt.json`은 소스·빌드·정리 custody를 보인다. 그것만으로 coverage의
ACTUAL receipt가 되지는 않는다. coordinator가 index·receipt로 연결·조립해야 한다.

증거 class는 `ACTUAL`, `SELFTEST`, `CONTRACT_RED`, `STUB`, `LOGIC_REVIEW`로
구별해 보고한다. 보고 규칙은 아래와 같다.

1. PASS는 `assemble.py`가 `ACTUAL` receipt를 받아들이고 `validate.py`가
   `VALID`를 출력한 항목만 쓴다. 그렇지 않은 PASS 주장은 `NOT_RUN`이다.
2. `SELFTEST`·`CONTRACT_RED`·`canned-observations.json`·`./verify harness`는
   제품 PASS로 세지 않는다. 손으로 쓴 evidence JSON은 증거가 아니다.
3. 보고에는 command·exit code·codeCommit·receipt 경로·hash·status를 함께 인용한다.
4. manifest가 `NOT_RUN`이거나 현재 입력과 어긋나면 그 상태를 그대로 보고한다.

## 모든 쓰기 경로 열거(V4)

V4 경로 집합은 고정 목록이 아니라 실행 중 시스템이 실제 노출한 면에서 만든다.
OData/CAP service의 `$metadata` entity set·action·function, MCP
`server/discover`·`tools/list`, worker handler registry, 관리/actuator endpoint를
읽는다. 각 entity set에 `$batch` changeset, deep insert, upsert, draft activation,
nested navigation의 생성·수정·삭제를 시도한다. capability allowlist에 없는
쓰기 가능 항목은 FAIL이다. core entity는 노출하지 않거나 `@readonly`·`@restrict`로
막고 READ grant 주체의 우회 효과가 0임을 전후 DB 관찰로 증명한다.
존재하지 않는 경로의 404만으로는 부족하다. 새 projection을 추가하면 같은 PR에서
이 열거 검사를 갱신한다.

## 명사·동사 조회와 query 계약(계획 §3.4)

같은 ID·`snapshotRevision`·`asOf`/`knownAt`·`scope`로 두 진입점
(`getObject`/`getWork`, `searchObjects`/`searchWorks` 등)을 호출한다.
다음을 필드별로 비교한다: 객체/업무 ID, 수량과 단위, `unknowns`,
`conflicts`, `evidenceRefs`, 의무의 owner/nextAction/nextCheck, `nextCursor`.
paging은 안정된 ID tie-break와 cursor를 쓴다. page 사이의 시점 변화와
다른 조직 객체의 존재 노출 여부를 관찰한다. 목표 판정은 물류 도착과 정산을
독립 assertion으로 둔다.
