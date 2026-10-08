# 독립 규범 oracle catalog

case 작성자와 구현자가 함께 놓친 조항을 ID 개수 검사로 통과시키지
않기 위해 구현 계획의 의무를 별도 catalog로 고정한다. 이 디렉터리는
case registry나 애플리케이션 출력에서 기대값을 만들지 않는다.

`mandatory-oracles.json`은 41개 case, D01–D26의 122개 하위 oracle와
499개 이름 있는 관찰을 담는다. 상태는 모두 `NOT_RUN`이다. 파일이
있거나 validator가 성공했다는 이유로 업무·통합·모델·규제·BTP 인수를
PASS로 표시할 수 없다. 필수 경로의 실제 assertion 연결과 실행은
후속 Step의 인수 대상이다.

## 안정된 형식

- `oracleId`: `caseId.descriptive-clause` 형식의 안정된 고유 ID다.
- `caseId`, `requirementIds`: T/C/V/E와 D 요구의 연결이다.
- `sourceRefs`: source 파일·SHA-256·절·상세 조항이다. source 변경은
  catalog와 관련 assertion의 재검토를 요구한다.
- `fixture`: 독립 고정 입력이다. 수량은 decimal 문자열과 명시 단위를
  사용한다. 가상값은 실제 규제·운영 정책 승인이 아니다.
- `requiredLayers`: 실제 인수할 경로다. `UNIT`, `API`, `DB`, `MCP`,
  `SKILLS`, `MODEL`, `LOCAL_DEPLOYMENT`, `BTP_DEPLOYMENT`,
  `REGULATORY_REVIEW`를 구별한다.
- `requiredCapabilities`, `controlPrerequisites`: 검증할 지원 기능과
  통제된 clock/barrier·실제 신원·비용 등 실행 전제다. 내부 구현
  메서드명이 아니라 규범 기능 식별이다.
- `expectedObservations`: `name`, `type`, `scope`, `operator`,
  `expected`, `artifactKinds`를 가진다. `scope`는 업무상 비교 범위이며
  특정 구현의 JSON pointer가 아니다.
- `sharedOracleRefs`: 추가로 함께 충족할 교차 계약이다. 참조만으로
  이 oracle의 관찰이나 실제 증거를 대신할 수 없다.

수량의 `expected`는 `{"value":"20","unit":"BOX"}`처럼 exact
값을 가진다. `effect=0`은 금지된 업무 효과가 없다는 뜻이며 허용된
조회/거부 감사·접수·대조 책임까지 없애라는 뜻이 아니다. `satisfies`의
object는 각 항목의 conjunction이다. 단순 필드 존재나 항상 참인
assertion은 이 object의 의미를 검증하지 못한다. 책임 관찰은 인간
owner·nextAction·nextCheckAt·유효 assignment 1개를 함께 요구한다.

## assertion과 실제 증거의 매칭

case assertion은 `oracleRef.oracleId`와
`oracleRef.observationNames`로 모든 필수 관찰을 명시 연결한다.
assertion의 source pointer는 실제 API/DB/MCP 관찰로 매핑하되 catalog의
관찰 이름·타입·범위·operator·기대 의미를 유지해야 한다. 한 복합 관찰을
여러 assertion으로 나누면 각 clause의 명시적 대응을 남긴다. 숫자
`20`을 문자열 `"20"`으로 표현하는 등 전송상의 정규화는 허용하지만
단위·양·비교 방향·책임·금지 효과를 약화할 수 없다.

`constraint`의 `eligibilityRereadAfterLock=true`나
`notAutomaticallyCompliantGoalFulfilment=true`는 의미 계약이다.
production response에 같은 이름의 boolean 필드를 만들라는 뜻이 아니다.
테스트를 통과시키기 위한 server boolean이나 observer가 만든 의미 판정
flag만 비교해서 계약을 입증할 수 없다.

예를 들어 `V3.hold-before-dispatch`의 `scope-lock` 관찰은 같은
`oracleRef`에 연결한 여러 구체 assertion으로 입증한다. 출고 요청이
초기 적격 조회 뒤 barrier에 멈췄다는 ACK, 별도 QC transaction의 보류
commit과 이후 출고 재개 순서, 출고의 구조화 거부 outcome, 독립 DB
원장의 신규 출고량0을 각각 비교한다. 이 조합으로 잠금 뒤 현재 제한을
적용했는지 검증하며, `eligibilityRereadAfterLock` flag 자체를 답안으로
삼지 않는다. barrier/transaction 기록·API 응답·DB 원장 artifact를
함께 연결하고 각 assertion이 `scope-lock`의 어느 clause를 입증하는지
남긴다. 연결의 구조 검사만으로 이 조합의 의미가 충분하다고 판정하지
않으며 QA가 실제 실행 경로와 반대 commit 순서의 사례도 대조한다.

각 `requiredLayers`의 실제 실행과 `artifactKinds`를 immutable artifact에
연결한다. command/version/commit/fixture hash/시간/expected/observed/
exit code와 `PASS|FAIL|NOT_RUN`을 기록한다. stub·정적 구조검사·논리
review는 실제 API/DB/MCP/model/BTP 증거가 아니다. 모델 호출과 BTP는
R8/R7의 실행 조건이 확정되지 않으면 `NOT_RUN`을 유지한다. 모델
60건 × 3회와 token·비용·version 증거는 별도 필수 gate다.

자동 matcher는 연결과 고정값의 구조적 불일치를 탐지한다. source의
모든 의미를 옳게 추출했는지, assertion이 그 의미를 실제로 검증하는지는
QA가 source와 case 내용을 대조하고 두 adversarial reviewer가 확인해야
한다. 구조검사 성공을 semantic completeness 또는 coverage complete로
보고하지 않는다.

## 독립 검사

repository root에서 Python 표준 라이브러리만 사용한다. 애플리케이션,
DB, 네트워크나 모델 호출이 필요하지 않다.

```bash
python3 verification/requirements/validate_catalog.py
python3 -m unittest discover -s verification/requirements -p 'test_catalog.py' -v
```

validator는 lock 자체의 형태부터 확인한다. 파일이 있으나 `{}`, `null`,
`[]`, `0`처럼 비었거나 object가 아니면 lock 검사를 건너뛰지 않고
INVALID다. coverage assembler도 parse된 lock은 항상 이 validator에
넘긴다. 파일이 없으면 NOT_RUN이다.

validator는 schema, 고유 41 case/D26, D/T 연결, source 파일 hash,
교차 참조, 필수 수량/책임 필드와 별도 `normative-contract-lock.json`의
고정 oracle·관찰·전체 계약 hash를 검사한다. T17 정상 인도 oracle 제거,
실제20 관찰 삭제/0으로 약화, C4 해소 의무 보존 제거, V7 인가/철회
순서 제거, DB 계층 제거 등 omission mutation을 거부한다.

lock은 catalog 삭제·약화의 drift fence다. source 완전성을 자동 증명하는
자료가 아니다. catalog와 lock을 함께 바꾸면 기술적으로 fence를
우회할 수 있으므로 자동 재생성하거나 case 출력에 맞춰 갱신하지
않는다. 변경 때 source 조항·이유·관련 case/assertion 영향을 review하고
의도된 변경을 별도 commit에서 승인된 QA 결과와 함께 갱신한다.

## 검토 지적에 따른 누락 보완

Sol xhigh의 catalog P2 검토는 계획에 있던 세 계약의 누락을 확인했다.
이번 변경은 원문 정책을 확대하지 않고 catalog와 lock을 명시적으로
갱신한다. 같은 Sol xhigh reviewer가 `7f972dd`에서 세 P2의 closure를
확인했다([공통 검토 기록](../../docs/execution/step-2-common-review.md)).
lock의 `reviewUpdates[0].closureReview`도 이 결과로 갱신했다. 실제
실행 인수는 남아 있다.

- §3.2: `T07.typed-relations-and-predicates`에 확정 true/false의 부정,
  `not(UNKNOWN/CONFLICT)=UNVERIFIED`와 conflict flag 보존을 추가했다.
- §5.1: `T09.exists-in-versus-end-state`는 구간 처음100·끝0의 같은
  관측에서 EXISTS_IN 충족, 끝 STATE_AT와 THROUGHOUT 미충족을
  구별한다. `T09.throughout-fully-observed`는 정책상 충분한 관측이
  유지되는100의 정상 충족을 별도로 검증한다. 기존 관측 공백의
  미확인 oracle도 유지한다.
- §7.1: `T05.disposition-manager-decision`과
  `T19.settlement-manager-decision`은 기본 MANAGER 확인 정책에서
  일반 WRITE 역할·grant만으로 확정 효과0, 정당한 범위·근거·결정 후
  정상 효과를 검증한다. 제안·관측 등록은 허용하며 일반 예약/출고에
  새 인간 승인을 요구하지 않는다.

새 조항·관찰 삭제와 기대값 약화에 대한 mutation 10개를 추가했다.
`normative-contract-lock.json`의 `reviewUpdates`에 근거 절과 이번
누락 보완의 이유를 기록했다. source 파일 hash와 의미는 그대로다.

## 현재 한계

catalog 준비와 24개 자체 검사만 실행했다. 업무 애플리케이션,
transaction/경합, DB/API/MCP, skill loading, 실제 모델, 법규와 BTP는
이 디렉터리 작성 과정에서 실행하지 않았다. 전체 인수·coverage는
`NOT_RUN`이다. catalog의 내용 검토와 case/assertion의 전체 연결은
Task integration에서 확인해야 한다.

## T25 artifactKinds 정정(2026-10-08, 2라운드)

`T25.independent-traceability`의 coverage-link·result-separation과
`T25.evidence-manifest-and-entrypoints`의 evidence-fields·wrapper-truth는
499개 중 484개가 공유하는 기본값 `api_response`·`db_snapshot`을
artifactKinds로 갖고 있었다. T25는 coverage verifier의 출력만
관찰하므로 이 두 artifact를 만들 수 없고, harness는 이를
`artifactKindAttributionGaps`로 보고했다. 계획 §13.4는 coverage
verifier가 각 case의 실제 assertion·artifact 연결을 확인하라고 할 뿐
T25가 업무 DB를 읽으라고 하지 않는다.

- 바꾼 값: coverage-link·result-separation은 `coverage_report`,
  `runtime_manifest`. evidence-fields·wrapper-truth는 여기에
  `wrapper_record`를 더한다.
- 약화가 아닌 이유: requiredLayers·expected·관찰 이름·operator는
  그대로다. 다른 관찰의 db_snapshot 등 artifactKind와 필수 profile
  PASS link는 T25 runtime-links-required가 관찰마다 요구하도록
  강화했다(`verification/cases/T25/README.md`).
- lock: 두 oracle의 `contractSha256`을 갱신하고 `reviewUpdates[2]`에
  이유·이전 값·바뀐 관찰을 남겼다. source 파일 hash는 그대로다.
- 이 catalog bytes를 입력으로 고정한 `verification/cases/E1/case.json`의
  `model-reference-host` inspectArtifacts descriptor(sha256·크기)를 함께
  갱신했다. T08 `observation-bindings.json`의 `catalogSha256` 등
  준비 기록의 옛 hash는 당시 기록이다. T08은 소유자가
  `bind_observations.py`로 다시 만들어야 `--check`가 통과한다.

## 관찰 단위 계층 경로 검사(2026-10-08, 2라운드)

coverage assembler는 subcase의 모든 assertion을 case가 선언한 모든
profile에 연결한다. 그래서 `mcp`·`skills` profile만 선언해도 MCP나
skill loading 결과를 하나도 읽지 않는 관찰이 "도달 가능"으로 보인다.
`check_layer_routes.py`는 MCP·SKILLS layer를 요구하는 관찰마다 연결
assertion 중 하나 이상이 그 계층의 증거(MCP·wire route 응답, MCP 쓰기
뒤의 독립 DB 관찰, protocol transcript, skill loading stage, profile·
artifactKind 필터의 coverage 행)를 읽는지 정적으로 검사한다.

```bash
python3 verification/requirements/check_layer_routes.py            # MCP·SKILLS
python3 verification/requirements/check_layer_routes.py --include-db # DB 참고 목록
python3 -m unittest discover -s verification/requirements -p 'test_layer_routes.py'
```

검토한 예외는 E1 model-reference 하나(host 모델 gate 입력)다. 3라운드에
`KNOWN_OPEN`으로 기록한 열린 항목(T20 3개, V4 1개)은 4라운드에 case가 해당
layer 증거를 읽게 되어 모두 닫았고 목록에서 지웠다(아래 4라운드 절). 결과 `VALID`는 선언 구조의 일관성일 뿐 runtime
PASS가 아니다. API layer는 검사하지 않는다. 효과 관찰은 DB 원행이
artifact이고 명령 응답은 같은 oracle의 다른 관찰에서 API로 읽기
때문이다.

### gate 연결과 기록된 예외 목록(2026-10-08, 3라운드)

2라운드의 검사는 어떤 gate에도 연결되지 않았고 예외 목록이 script 안의
상수였다. 3라운드에서 다음을 고쳤다.

- 예외는 `layer-route-review.json`에 기록한다. 항목마다 `status`
  (EXEMPT·KNOWN_OPEN), oracleId·observationName·layer, `owner`,
  `reason`이 필수이고 KNOWN_OPEN은 `closeWhen`(종료 조건)도 필수다.
  필드가 빠진 항목은 예외로 인정하지 않아 gap이 FAIL로 드러난다.
- 목록은 정확해야 한다. gap이 아닌 항목(닫힌 gap의 남은 기록)은
  `stale layer-route review entry`로 실패한다. owner가 gap을 닫으면
  항목도 지워야 한다.
- `./verify prepare`가 `caseAssetChecks`의 `layer-routes`로 실행하고
  KNOWN_OPEN 행을 `knownOpenGaps`에 옮긴다. coverage assembler도
  `review()`를 불러 같은 판정을 하고 KNOWN_OPEN 관찰을 NOT_RUN으로 둔다.

## 파생 binding stamp와 생성기 재현(2026-10-08, 3라운드)

`observation-bindings.json`은 case.json과 catalog에서 만든 파생 자료다.
d84f2942의 catalog 변경 뒤 T08만 다시 만들었고 V4·V6·V7·C3는 이전
catalogSha256(61968b22…)을 유지했는데 prepare는 이를 보지 못했다.

- `check_derived_bindings.py`는 모든 bindings 파일의 `caseHash`·
  `catalogSha256`이 현재 입력의 sha256과 같은지 본다. 3라운드에는 C3
  `author_prerequisites.py`가 catalogSha256을 갱신하지 않아 KNOWN_OPEN 한
  건이 있었다. 4라운드에 생성기가 catalogSha256도 다시 계산하고 C3
  observation 목록이 catalog와 같은지 확인하게 했다. 다시 만든 bindings를
  commit하고 KNOWN_OPEN 항목을 지웠다. 지금 목록은 비어 있다.
- `test_case_generators_reproduce.py`는 V4 `author_review_fixes.py`,
  T06 `author_contracts.py`(T06·T22·T24), `platform-tests/build_cases.py`
  (T23·V8)를 임시 복사본에서 실행하고 쓰는 디렉터리 전체가 commit과
  byte 단위로 같은지 본다. post-processor(C3·T26·V4)는 자기 출력에 대한
  고정점(멱등)만 증명한다.
- 둘 다 `./verify prepare`의 `caseAssetChecks`다. V4·V6·V7 내용 drift는
  `V7/bind_observations.py V4|V6|V7 --check`가 따로 본다.

```bash
python3 verification/requirements/check_derived_bindings.py
python3 -m unittest discover -s verification/requirements -p 'test_case_generators_reproduce.py'
```

## layer route KNOWN_OPEN 종료(2026-10-08, 4라운드)

requiredLayers를 좁히는 lock reviewUpdate는 쓰지 않았다. 네 gap 모두 case가
해당 layer의 결과를 실제로 읽게 해서 닫았다. 근거는
`docs/execution/step2r-round4/README.md`에 있다.

| observation | layer | 닫은 방법 |
|---|---|---|
| T20 `allowed-tools-as-server-authorization` | SKILLS | host-allowed-tools-write가 `requireDiscovery`·`requireBodyRead`로 skill을 실제로 loading하게 하고, loading 원행의 DISCOVERED·BODY_READ를 각각 1개로 센다. skill이 쓰이지 않아서 쓰기0이 저절로 맞는 경우를 막는다 |
| T20 `document-instruction-authority` | SKILLS | host-malicious-document가 skill 본문 BODY_READ 1개를 세고, BODY_READ 행의 package가 `ontology-work-coordinator` 하나뿐임을 본다. 첨부 문서를 skill로 loading하지 않는다 |
| T20 `skill-hash-as-loading-proof` | MCP | host-hash-only가 protocol transcript(`toolCalls`)의 COMMAND·RECORD 쓰기 호출0을 센다 |
| V4 `mixed-batch-allowed-partial-effects` | MCP | mixed-atomic-batch에 같은 혼합 연산을 JSON-RPC batch 배열 하나로 보내는 MCP 경로를 더했다. HTTP 400·`-32600`·tool result 없음·허용 RECORD의 COMMITTED command0·claim0·조직 범위 원행 불변을 본다 |

`check_layer_routes.py`는 KNOWN_OPEN 0이다. 시험은 V4 MCP 경로를 지운
임시 복사본으로 unexplained·KNOWN_OPEN·stale 세 판정을 계속 확인한다.
