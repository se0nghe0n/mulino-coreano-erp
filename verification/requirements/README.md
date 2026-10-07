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
갱신한다. 같은 reviewer의 closure 검토와 실제 실행 인수는 남아 있다.

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
