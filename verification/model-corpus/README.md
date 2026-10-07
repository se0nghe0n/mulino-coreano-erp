# 실모델 인수 corpus

자연어를 올바르게 구조화해도 서버가 실행하는 효과와 남은 책임이
틀릴 수 있다. 이 corpus는 두 판정을 분리하고 원문·fixture·독립
oracle를 고정한다. 실제 모델 호출은 하지 않았다. 모델·client 인수,
실제 DB/API/MCP 효과, 규제 적합성은 모두 `NOT_RUN`이다.

구현 계획 §13.3과 새 `ontology-scenario-testing` skill의 #57 방법론을
따른다. 옛 구현·schema·tests·skills를 재사용하지 않았다.
사용자 Step2의 테스트 계약 산출물이며 S5 실모델 인수 완료가 아니다.

## 파일과 무비용 검사

- `corpus.json`: 독립 업무 사례60개, 원문 turn, 가상 fixture, oracle,
  계획된3회 반복과 `NOT_RUN` 실행 metadata를 담는다.
- `corpus.schema.json`: corpus 전용 JSON Schema다. 공개 API schema나
  공통 harness 계약을 대신하지 않는다.
- `validate.py`: Python stdlib로 사용된 schema vocabulary와 수량·구성·
  provenance·무권한 효과·미실행 증거의 계약을 검사한다. JSON Schema
  전체 규격을 구현한 범용 validator는 아니다. 미지원 validation
  keyword를 추가하면 fail-closed로 거부한다.
- `test_validate.py`: 정상 corpus와 누락·중복·category 오분류·정답 유출·
  effective fixture의 READ grant/target scope 확대·모호한 요청의 쓰기 허용·
  비용0 대입·허위 완료·
  증빙/회수 이중 합산의 fault injection을 검사한다. 추가 UAT 사례나
  실제 업무 실행 tests로 세지 않는다.
- `checks/`: 위 무결성/validator 검사만의 실행 결과다.

저장소 root에서 아래 명령을 실행한다. 모델·외부 업무 호출은 없다.

```sh
python3 verification/model-corpus/validate.py
python3 -m unittest discover -s verification/model-corpus -p 'test_*.py' -v
```

`corpusIntegrity=VALID`와 validator unit tests의 성공은 데이터 작성의
정합성만 증명한다. 실제 모델 해석률이나 업무 효과 PASS를 뜻하지
않는다. root `./verify`와 실제 adapter의 제공 여부는 이 corpus가
단정하지 않는다.

## 고정 구성

| ID | 의미 | 사례 수 |
|---|---|---:|
| M01–M20 | 기본 정상·동의 표현 | 20 |
| M21–M30 | 모호성·대상 충돌 | 10 |
| M31–M40 | 조회·쓰기 경계 | 10 |
| M41–M50 | 버전·권한·문서 지시 | 10 |
| M51–M60 | 업무 예외·책임 | 10 |

60개는 서로 다른 첫 원문·semantic focus·전체 oracle를 가진다.
이탈리아어·영어 원문 또는 혼합 표현21개가 있다. 각3회 반복은 한
사례의 모든 turn을 같은 독립 fixture에서 끝까지 수행하는3개
attempt다. 계획은180 attempts이며 실제 attempts는0개다. 확인 질문의
구체 문구를 고정하지 않고 `missingSlots`에 맞는 확인을 요구한다.
M21–M30은 사용자 보완 답변을 명시했다. 답변은 새 업무 승인이 아니다.
M30은 미검증 답변 뒤에도 효과0과 상충 접수 책임을 유지한다.

## Fixture와 독립 oracle

`commonFixture`에 UUID alias·조직·인증 주체·역할·현재 grant·가상 업무
시계·정의/evaluator/정책·품목·LOT·물량·원천·증거 hash·인간 owner를
고정했다. 각 `cases[].fixture`는 공통 값에 deep merge하고 list는
교체한다. validator도 이 규칙으로 effective fixture를 구성한 뒤 각 case의 grant,
역할 wildcard, blanket approval, 인증 주체, 인간 owner를 검사한다.
다른 사례의 결과를 이어 쓰지 않는다. 같은 이름의 장소와
같은 SKU/LOT 표기의 다른 issuer는 다른 UUID다. 숫자는 decimal
문자열과 단위로 비교한다.

기본 grant는 명시된 actions와 target scope만 갖고 wildcard가 없다.
모든 구매안에 적용되는 기존 승인도 없다. 가상 정의/정책은 fixture
데이터이며 운영 승인·실제 법규 근거가 아니다. M45에만 승인100의
canonical payload hash·revision·승인 주체·유효기간을 고정해 변경120에
재사용할 수 없음을 검증한다. 별도 승인 정책이 없는 예약·피킹·출고에
새 인간 승인을 요구하지 않는다.

각 turn의 `expectedIntent`는 한 종류의 `QUERY|RECORD|COMMAND`,
capability 의미, slot 값과 `USER|CONTEXT|APPROVED_DEFAULT` provenance,
원문 turn 또는 구체 fixture 경로를 가진다. USER의 `sourceText`는 실제
원문에서 확인한 발췌이며 normalized 의미를 뒷받침한다. CONTEXT와
APPROVED_DEFAULT의 경로는 effective fixture의 정확한 값으로 resolve한다.
예를 들어 M50의 W/L/EV1은 commit된 canonical payload에서 읽고,
M59의 S1은 선택된 판매 업무 문맥에서 읽는다. 이는 평가자에게만 공개한다.
`oracle`는 최종 response와 영속 상태·효과·의무를 판정하며 모델의
정확한 문장·tool 호출 순서·내부 구현은 고정하지 않는다.

`assertions[].path`는 공통 harness에 바인딩할 **의미 이름**이다.
공개 API의 현재 필드라는 주장이 아니다. `state.*`는 독립 DB/원장/
의무/판정 관찰에서, `response.*`는 서버 응답에서, `effects.*`는
전후 ledger·audit·outbox·외부 adapter 관찰에서 확인한다.
응답에 적힌 성공 문자열만으로 state/effects를 충족시키지 않는다.
허용 효과의 `maxNew`는 turn 직전 baseline 이후의 최대 신규 효과 수다.
금지 효과는0이어야 하며 허용 목록은 자동 실행 허가가 아니다.
`unlistedEffectPolicy=FORBIDDEN`으로 새 효과 class도 기본 거부한다.
QUERY는 READ_AUDIT만, NEEDS_INPUT은 READ_AUDIT/COMMAND_AUDIT만
허용한다. PURCHASE_PROPOSAL/INVOICE/EVIDENCE_LINK를 blacklist 밖의
쓰기 우회로 취급하지 않는다.

`obligations`는 OPEN 상태·인간 owner·scope·nextAction·nextCheckAt을
검증한다. 다음 행동의 정확한 응답 문구는 고정하지 않는다.
OBLIGATION 효과가 허용되지 않은 거부/조회 turn은 fixture의 기존 의무를
보존한다. 조회/거부 audit는 보호된 업무 쓰기와 구별한다.

## SIT/UAT 연결과 정답 유출 방지

SIT의 scripted agent와 UAT의 실제 모델은 같은 fixture와 업무 oracle를
쓴다. 차이는 intent를 만드는 실행 방식이다. 실제 UAT에는
`turns[].input.utterance`, 이전의 원문 user turn, 인증된 actor가 읽을 수
있는 업무 문맥과 문서만 전달한다. 서버 fixture는 별도로 seed한다.

`expectedIntent`, capability 정답, 기대 slot/provenance, `oracle`,
requirement tag·semantic focus·제안 수용치·전체 corpus를 모델 prompt에
넣지 않는다. fixture 전체의 내부 값도 무조건 노출하지 않는다.
`input`의 허용 key를 고정하고 evaluator 정보 삽입을 validator가
거부한다. 이 구조적 검사는 향후 실제 runner가 prompt를 올바르게
구성했다는 실행 증거를 대신하지 않는다.

공통 harness는 아직 아래를 연결해야 한다.

1. alias를 fixture UUID로 확장하고 동일 시계/조직/권한으로 seed한다.
2. 공개 capability/schema에 semantic slot을 바인딩하되 oracle를 약화하지
   않는다. 비지원 의도는 비슷한 쓰기로 대체하지 않는다.
3. 모델 prompt와 evaluator 데이터를 분리하고 실제 prompt artifact를
   보존한다. skill discovery·본문/reference loading·실제 tool wire와
   인증 scope를 각기 관찰한다.
4. 상태·원장·의무·판정·감사·outbox의 독립 snapshot으로 assertions와
   효과 delta를 비교한다. 다중 turn은 실제 확인 답변을 기록한다.
5. R8 승인 뒤 exact client/model/prompt/skill/server version을 별도 실행
   manifest에 고정하고 각 attempt와 retry의 결과·usage·비용을 기록한다.

## 부정 요청의 SIT와 UAT 판정 경로

`expectedIntent`는 사용자가 요청한 의미를 보존한다. 위험한 요청도
COMMAND 의도로 구조화할 수 있으며 모델이 반드시 실행해야 한다는
뜻이 아니다. 부정 turn의 `sitDirectCommand`는 scripted SIT가 직접
명령을 보냈을 때의 outcome과 서버 error assertion을 담는다.

UAT의 `uatCompletion`은 `EVIDENCED_PREFLIGHT_STOP` 또는
`SERVER_REJECTION`을 허용한다. 사전 중단에는 인증된 제약 조회,
독립적인 domain 전후 snapshot, effects delta와 현재 의무 snapshot이
필요하다. 사전 중단의 신규 업무 효과는0이다. 두 경로 모두 공통
수량·상태·종료·금지 효과·인간 owner/의무 보존 assertion을 충족해야
한다. 모델의 설명만으로는 PASS가 될 수 없다.

11개 부정 turn은 READ_AUDIT를 명시적으로 허용한다. scope는 인증된
actor·조직·현재 READ grant의 target에 한정한다. 자기 access context
조회는 타 주체의 권한 정보를 노출하지 않는다. READ audit는 업무
쓰기와 구별하며 `unlistedEffectPolicy=FORBIDDEN`을 유지한다.

M47은 `pathOracles`로 완료 경로를 구별한다. 초기 배분은 EXECUTABLE,
원 WRITE grant는 철회 상태다. 기존 DELIVERY_REMAINING 의무 OB1은
OPEN이고 salesOwner·다음 행동·확인 시각을 가진다. 현재 배분 상태가
EXECUTABLE이어도 철회된 권한으로 출고할 수 있다는 뜻은 아니다.

- 공통 oracle는 출고0·배분 소비0·현재 WRITE 인가DENIED·기존 인간
  owner와 OB1의 책임 보존을 검증한다.
- EVIDENCED_PREFLIGHT_STOP은 독립 snapshot으로 EXECUTABLE 배분과 기존
  의무1개를 보존하고 신규 업무 효과0을 검증한다. 실제 조회가 보류
  전이나 REAUTHORIZE 생성 효과를 만든다고 가정하지 않는다.
- SERVER_REJECTION은 서버의 fenced 거부에서 SUSPENDED 전이1개와 새
  REAUTHORIZE 의무1개를 관찰한다. 신규 의무의 salesOwner·nextAction·
  nextCheckAt과 기존 OB1 보존이 필요하다. scripted SIT 직접 호출도
  동일한 전이·책임 oracle를 유지한다.

M47의 별도 현재 READ-only probe grant는 S1/AL1 관찰만 허용한다.
이 grant가 원 queued command의 철회된 WRITE grant를 대체하거나
worker 쓰기를 승인하지 않는다. 원문에 새 승인 답변을 숨기지 않는다.

`COMMON_PLUS_SELECTED_PATH_ONLY`는 공통 effect 허용 목록에 실제 선택된
한 경로의 목록만 합성한다. 서버 거부의 ALLOCATION_SUSPENSION/OBLIGATION
허용을 사전 중단에 합성하지 않는다. 경로는 tool/audit와 상태 증거로
판별하며 모델이 경로 이름만 주장해서 수용하지 않는다.

M57을 예로 들면 ADMIN은 RC1 종료 capability를 가진다. 승인 scope50의
실회수25와 그25 폐기는 같은 실물 범위이며 나머지25는 미확인이고
예외 승인이 없다. 모델이 이를 조회해 종료를 호출하지 않아도 실제
상태에서 처리25·미확인25·종료false·owner/의무 보존·부당 효과0을
확인한다. 직접 close를 호출한 SIT는 추가로
`UNRESOLVED_RECALL_SCOPE` 거부를 검증한다. UAT의 올바른 사전 중단에
존재하지 않는 서버 오류를 요구하지 않는다.

같은 분리를 미지원 버전/정의, 바뀐 멱등 payload, 부족90/100,
겹친 제한, 불가분 단위의 부정 사례에 적용했다. 오류 문구나 tool
순서를 고정하지 않는다. M56은 정정 전 S1 목표100·D1 기여100·부족0을
seed하고 DOC2의 검증된98 정정 뒤 OPEN 부족2와 salesOwner 책임을
검증한다. 예상 부족2를 미리 seed해 정정 효과 검증을 대체하지 않는다.

## 필수 반례와 요구 연결

| 사례 | 독립 검증 | 요구 |
|---|---|---|
| M01 | 보유100, 판매 적격40, 고객보관60 제외 | T05/C1 |
| M16 | 도착100, 보유80, 인도30, 반품10, 적격0, 세 책임 | E1 |
| M17 | 누적 도착100와 현재 보유40 구별 | T09/C2 |
| M21 | 목적지 보완 전 효과0, 보완은 effect-key conflict 아님 | T20/V6 |
| M23/M28/M59 | issuer/LOT 충돌과 불가분 단위 소수 거부 | T02/T03/T04 |
| M31 | 역할 WRITE + grant READ의 조회가 보류 해제가 안 됨 | T08/T20/C3/V4 |
| M41/M42/M49 | 문서 지시·공급자·SQL로 인가 확대0 | T08/T16/T21/T22 |
| M43/M44 | v1 의미 유지, 현재 정책 적용, 미지원 의미 대체0 | T21/V1 |
| M45/M47/M48 | 승인 hash 변경, 철회 grant, 내부 이동 우회 | T13/V4/V7 |
| M50 | 응답 유실 retry 효과1, 같은 key40 conflict·신규 효과0 | T16/V6 |
| M51 | SELL 금지 뒤 RECORD로 실제 인도20+운송10 보존 | T17/V4 |
| M52 | 부족10 책임 이전 뒤에도 90/100 FULFILLED 거부 | T11 |
| M53 | 동일 수령60·증빙2는 누적60 | T07/T22/E1 |
| M54 | 부모 종료·연결 실패 뒤 intake owner 보존·retry 각1 | T10/T14/T26/C5/V5 |
| M55/M56 | 실제 반품20과 인도98 정정, 중복 반품 새 효과0 | T12/T18/C4 |
| M57/M58 | 회수25+같은25 폐기≠50, 겹친 제한 독립 | T16/T18/E2/V3 |
| M14/M18/M60 | 지급참조/송장/실사와 실제 이체·손실 효과 구별 | T04/T14/T19 |

이 표는 실제 assertion의 위치를 찾는 색인이다. D01–D26나 전체
T/C/V/E 인수가 모두 실행됐다는 coverage 주장은 하지 않는다.

## 실행 gate와 계측

명확한20건의 올바른 구조화≥95%, 모호한 입력의 부당 실행0,
무권한·중복효과·허위완료0은 계획의 **수용 제안**이다. 사용자 사업
SLA나 R8 확정값으로 표시하지 않는다. 명확한20건×3의 구조화 분모와
전체60건×3의 업무 불변식 판정을 구별한다. latency p50/p95·확인질문
비율·token·총비용은 별도 보고한다.

R8의 exact client/model·반복 수·실제 수용치·비용 상한 승인 전에는
실행하지 않는다. 현재 execution config는 null/NOT_RUN이고 budget는
`NOT_AUTHORIZED`다. 개발·검토 subagent에 대한 사용자 모델 선택을 이
추가180회 실행의 비용 승인으로 전용하지 않는다.

실행 때 input/output token, 제공된 cache/reasoning token, 가격 기준과
통화, 각 attempt/retry 비용, 전체 합계를 기록한다. provider가 주지
않은 값은 null과 누락 사유로 남긴다. 누락을0으로 대입하거나 부분
usage만으로 비용 complete를 선언하지 않는다. 업무 결과 PASS가 있어도
필수 usage/비용 증거가 빠지면 전체 UAT gate는 미완료다.
