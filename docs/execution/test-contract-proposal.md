# Step2 테스트 계약 제안

기준일은 2026-10-07이며 조사 baseline은
`393cb5c3d9f4d16fe728933ef211f9b882a73d25`다.
현재 사용자 Step2는 **PENDING_R2**다. 이 문서는 읽기 조사 결과이며
계약 동결·테스트 구현·제품 인수 완료를 뜻하지 않는다. R2 확정 전에는
테스트·공개 계약·앱 코드를 작성하지 않는다.

기준은 [구현 계획](../ontology-implementation-plan.md) 전체,
[설계 철학](../ontology-design-philosophy.md), 새 개발 skills의
[테스트 절차](../../.agents/skills/ontology-scenario-testing/SKILL.md)다.
옛 코드·schema·tests·운영 문서는 읽거나 재사용하지 않았다.
#57 방법론은 새 skill에 보존된 시나리오·SIT/UAT·증거 구분만 따른다.

## 1. 단일 Java 테스트 harness

SIT와 UAT의 업무 계약이 갈라지지 않도록 Java21/Maven의 독립 테스트
모듈 하나에 Cucumber-JVM과 JUnit Platform을 두는 안을 제안한다.
한국어 Gherkin·fixture·수량/책임 assertion을 공유하고 Agent 실행
runner만 교체한다. CAP dependency 없이 먼저 실행할 수 있어야 한다.

후보 dependency는 Cucumber-JVM8.0.4와 JUnit6.1.2다. 공식 자료를
확인한 후보이며 이 조합의 runtime smoke는 아직 수행하지 않았다.
Spring Boot4.1.1 BOM의 JUnit6.0.3을 무조건 섞지 않는다. exact
Cucumber/JUnit/Surefire/JSON/JDBC/Testcontainers 조합은 공통 harness의
최초 discovery·실행 smoke에서 고정한다. CAP Java5.1.1과
Spring Boot4.1.1도 S0의 검증 후보이며 현재 채택 stack이 아니다.
별도 조사 산출물인 [platform 조사](platform-research.md)는 다른
writer가 작성하며 Task branch 통합 뒤 연결을 검증한다.

- `verification/harness/pom.xml`은 Java21 테스트 모듈과 dependency를
  소유한다. JUnit Platform의 Cucumber engine을 명시적으로 discovery한다.
- `verification/harness/src/test/java/`는 공통 Cucumber glue, driver,
  observer, assertion, evidence reporter를 소유한다.
- `verification/cases/<ID>/`는 Gherkin·fixture·입력·고정 기대값을 둔다.
  SIT와 UAT feature를 복제하지 않는다.
- `verification/case-tests/src/test/java/<ID>/`는 필요한 사례별 Java
  assertion을 두며 해당 case writer에게 함께 배정한다.
- 실제 DB profile은 Testcontainers와 PostgreSQL JDBC를 사용한다.
  Cucumber lifecycle에서 container를 관리하며 Jupiter extension이
  Cucumber engine에도 자동 적용된다고 가정하지 않는다.
- protocol runner는 Java HTTP client로 실제 wire를 호출한다. 실제
  host/client runner만 필요한 process adapter를 둔다. 제품 CLI는
  필수 인수 경로로 추가하지 않는다.

Surefire의 suite는 Cucumber engine과 feature resource를 명시하거나
JUnit Platform ConsoleLauncher로 실행한다. dependency 설치 성공만으로
feature discovery·실행 성공을 주장하지 않는다.

공식 근거는 [Cucumber 설치](https://cucumber.io/docs/installation/java/),
[Cucumber JUnit Platform engine](https://github.com/cucumber/cucumber-jvm/blob/v8.0.4/cucumber-junit-platform-engine/README.md),
[Cucumber8.0.0 변경 기록](https://github.com/cucumber/cucumber-jvm/blob/v8.0.4/release-notes/v8.0.0.md),
[Testcontainers 안내](https://java.testcontainers.org/quickstart/junit_5_quickstart/)다.

## 2. 병렬 작성 전 공통 계약

R2 확정 뒤 공통 소유자가 아래 제안 경로를 먼저 구현·검증하고 Task
branch에 통합한다. 현재 파일들이 존재하거나 계약이 채택됐다는 뜻은
아니다. 통합 commit을 `B2`로 기록하고 모든 case writer를 같은 `B2`의
별도 branch/worktree에서 시작한다.

- `contracts/acceptance-driver.schema.json`
- `contracts/acceptance-observation.schema.json`
- `contracts/acceptance-capabilities.json`
- `verification/harness/`, `verification/fixtures/base/`, 루트 `verify`

이는 테스트 호출 규약이며 CAP·HTTP endpoint·내부 schema의 확정이
아니다. capability와 업무 outcome은 계획 §3–§8에서 가져온다.
작성자별 API와 승인 규칙을 만들지 않는다.

공통 `AcceptanceDriver`는 아래 동작을 제공한다.

```text
installFixture(fixtureRef) -> aliasMap + fixtureHash
invoke(route, authenticatedActorRef, capabilityId, request) -> execution
query(route, actorRef, queryName, request) -> execution
observe(scope, snapshotRef, asOf, knownAt) -> observation
control(clock | barrier | fault | process | externalResponder) -> artifact
```

`execution`은 `driverStatus=EXECUTED|NOT_IMPLEMENTED|UNAVAILABLE`,
실제 요청·응답, 인증 주체/scope, route, adapter/build version과
artifact를 가진다. 이 driver 상태는 계획의 업무 `outcome`과 별도다.
`NOT_IMPLEMENTED`를 서버의 새 업무 outcome으로 만들지 않는다.

SIT의 scripted runner는 작성된 typed intent를 실제 tool/API에 보낸다.
UAT의 actual client runner는 같은 업무 요청을 실제 host/model로
수행한다. 두 runner는 같은 fixture·서버·observer·oracle를 사용한다.
모델의 문장이나 정확한 tool 호출 순서는 고정 답안이 아니다. 업무
milestone과 최종 허용 효과·남은 책임을 검증한다.

테스트 driver는 수량·적격성·승인·목표를 계산하거나 업무 상태를
저장하지 않는다. production behavior를 구현하는 fake service를
만들지 않는다. 관찰 표본을 변조해 assertion 자체를 점검하는 것은
제품 동작 인수와 별도로 기록한다.

## 3. fixture와 독립 관찰

fixture는 조직·identity·grant, 정의/evaluator/policy version,
품목/LOT/구별 가능한 실물, decimal 문자열과 단위, 증거 hash와 사건
ID, 인간 owner·기한을 고정한다. 가상 정책은 실제 법규가 아니다.
서버 발급 ID는 fixture alias와 실제 ID의 대응으로 다룬다.

발생시각·기록시각·UTC instant·원 offset/시간대·정밀도·기한 끝점을
고정한다. `MISSING/UNKNOWN/NOT_APPLICABLE/CONFLICT`를0이나 단일 null로
합치지 않는다. fixture/setup 상태는 baseline으로 기록하고 검증 대상
행동의 새 효과와 구별한다. 현재량·누적량 oracle는 정의된 scope 안의
baseline 사실도 포함한다. setup 자체를 해당 capability의 실행
coverage로 세지 않으며 검증하려는 행동을 setup으로 대신하지 않는다.

각 사례는 입력·인증 주체·route·실제 capability, 정상/예외 기대값,
독립 손계산, assertion ID, 관찰 원천, 효과 scope, 허용/금지 변화와
필요 adapter/layer를 가진다. expectation은 구현의 반환값에서 생성하지
않는다. parent 재소비·중복 실물·기여 scope도 합계와 함께 검사한다.

observation vocabulary는 아래 원천을 구분한다.

```text
objects / segments / genealogy / movements / allocations
activities / contributions / restrictions / dispositionBases
works / goalVersions / assessments / obligations / handovers
inbox / reconciliation / approvals / audit / outbox / idempotency
blobs / externalEffects / operationalIssues
```

실제 DB observer는 read-only 연결에서 원 행·키·revision·실행 query와
parameter·snapshot artifact를 기록한다. API 계산 결과를 복사한
observation은 독립 DB 증명이 아니다. schema 확정 뒤 versioned 필드
대응을 추가하고 oracle를 바꾸지 않는다. 명사/동사 조회는 동일 ID와
snapshotRevision·평가시점·knownAt에서 비교한다.

거부 사례는 모든 DB가 불변이라는 가정을 쓰지 않는다.

| 행동/결과 | 금지 효과 | 별도로 허용하거나 요구하는 관찰 |
|---|---|---|
| READ grant의 쓰기 거부 | 원장·배분·업무·승인·업무 outbox 생성/변경0 | 허용된 조회/거부 audit |
| 허위/상충 RECORD | 정상 인도·새 배분·창고 출고·정상 목표 기여0 | inbox/claim·대조 의무·owner/nextCheck |
| 출고 후 SELL 제한의 실제 인도 RECORD | 새 창고 출고·새 배분0 | 기존 운송 실물 대조 이동·실제 사건·위반/회수 책임 |
| 같은 효과 key retry | 두 번째 실물·배분 소비·의무·외부 작업0 | 현재 조회 인가 후 원 결과·별도 attempt 기록 |
| 같은 key의 다른 payload | 신규 업무 효과0 | conflict·허용된 감사 |
| audit 실패 rollback | 도메인·원장·의무·outbox·COMMITTED 결과0 | DB 밖 기술 실패 로그를 업무 audit와 구별 |

각 테스트는 위 범주를 더 좁은 대상 scope와 expected delta로 확정한다.
정상 관측의 업무0과 이상 관측의 책임 생성1도 독립으로 단언한다.

## 4. 의미 있는 RED와 NOT_RUN

미구현 driver는 `NOT_IMPLEMENTED`와 `response=null`만 반환한다.
가짜 수량·승인·물량·의무·DB 관찰을 반환하지 않는다. 각 사례의 첫
필수 assertion은 실제 실행/관찰 가능 여부다. 이를 통과해야 수량과
금지 효과를 검사하므로 앱 부재가 효과0 사례의 PASS가 될 수 없다.

| 실행 종류 | 결과와 제한 |
|---|---|
| contract-red | runner/fixture/구문은 실행되고 필요한 capability 부재로 assertion FAIL·exit1을 남긴다 |
| 실제 schema/API/MCP/recovery | adapter 부재는 NOT_RUN과 원인·미완료 gate를 남긴다 |
| 환경/설정 실패 | Docker·인증·구문 오류는 환경 실패이며 의미 있는 contract RED가 아니다 |
| 부분 실행 | 단계 결과를 보존하며 미실행 단계가 있으면 전체 NOT_RUN, 확인된 위반이 있으면 FAIL이다 |
| harness self-test | 알려진 관찰 표본과 변조 표본으로 assertion을 점검한다. 제품 coverage로 세지 않는다 |

구체 업무 assertion은 모든 사례에 작성한다. 공통 availability assertion
하나만 복제해 요구를 검증했다고 주장하지 않는다. 표본 변조 검사는
잘못된 수량·중복·owner 누락·version 오염을 잡는지 확인하며 실제
도메인 행동을 시뮬레이션하지 않는다.

coverage 준비 검사는 D/T/C/V/E와 구체 assertion의 연결을 검사한다.
runtime coverage는 EXECUTED observation과 실제 artifact를 추가로
요구한다. ID 목록, feature 존재, stub FAIL와 정적 PASS는 실행 coverage가
아니다. `./verify`는 내부 command·version·exit code를 노출하고
`failIfNoTests`, 등록된 필수 case/subcase의 discovery·실행 수 대조로
빈 suite나 누락을 실패 처리한다. T 26개+C 5개+V 8개+E 2개인 총41개
top-level case ID는 최소 index이며 실제 필수 subcase 수는 registry에서
검사한다.
skip·pending·비용 미승인을 PASS에 포함하지 않는다.

## 5. 공통 계약 뒤의 여덟 독립 Subtask

coordinator가 아래 worktree를 같은 `B2`에서 만든다. 경로의 공통
prefix는 `/Volumes/VideoStore/Developer/`다. 각 writer는 자기 case의
fixture/feature/expectation/index와 동일 ID의 case-specific Java assertion만
쓴다. 공통 registry와 evidence manifest는 coordinator가 통합한다.
writer는 넓은 workspace를 공유하며 타인의 변경을 보존한다.

| worktree suffix | case 독점 소유 | 필수 범위 |
|---|---|---|
| `mulino-coreano-ontology-step2-definitions` | T02/T07/T21/V1 | issuer·단위, 타입/cardinality, 발행 불변·명시 전환·과거 의미/현재 제한 |
| `mulino-coreano-ontology-step2-inventory` | T03/T04/T05/T16/C1/V2/V3 | 계보·혼합 식별·decimal·처분·임시 수령·독립 제한·무이벤트 만료·실제 경합 |
| `mulino-coreano-ontology-step2-work` | T09/T10/T11/T12/C2/C5 | quantityMode·인계/부분 이전·부모90/100·판정 이력·부모 없는 접수 책임 |
| `mulino-coreano-ontology-step2-trade` | T13/T14/T15/T17/T18/T19/C4/E1/E2 | 구매·운송·기관·판매·반품·회수·정산의 정상/예외 oracle |
| `mulino-coreano-ontology-step2-security-source` | T06/T08/T22/T24/C3/V4/V6/V7 | 시점·조직/grant·source 대조·감사/보존·우회 route·멱등·철회 |
| `mulino-coreano-ontology-step2-runtime` | T26/V5 | 전체 재시작·2worker·stale fence·retry 소진·접수/의무/만료 복구 |
| `mulino-coreano-ontology-step2-platform` | T23/V8, `verification/platform-tests/` | fresh/live 분기·새 ontology v1→v2·CDS drift·DB/blob/evaluator 복원·local/BTP 분리 |
| `mulino-coreano-ontology-step2-channel-qa` | T01/T20/T25, `verification/mcp-tests/`, `verification/skills-tests/`, `verification/model-corpus/` | 다섯 역량 질문·두 진입점·MRTR/wire·host/skills·60건 corpus·coverage 검사 |

D01–D26는 같은 번호의 T assertion에 연결한다. 교차 C/V/E assertion은
다른 writer의 파일을 다시 작성하지 않고 참조한다. 이 표는 소유권
제안이며 요구 충족이나 coverage 주장으로 사용하지 않는다.

V2/V3/V7은 실제 두 transaction과 barrier로 양쪽 선후를 검사한다.
V6은 수령60 commit/응답 유실·token 갱신/new RPC/동시 retry·같은key40·
타주체·별개 주문·입력 단계 destination 보완을 각각 작성한다. V4는
구현 뒤 실제 route inventory와 direct/nested/batch/projection/MCP/
worker/blob/관리 경로를 대조한다. sleep 타이밍이나 mock lock으로
거래/경합 PASS를 만들지 않는다.

E1은 구매 승인·수령·QC·기관 허용·판매주문·예약·출고·인도·반품 허가/
수령을 각각 실행한다. 뒷 단계가 앞 단계를 몰래 생성하지 않는다.
누적100·W80·인도30·반품10·판매0·은행0과 QC/반품/정산의 owner를
검사한다. E2는 QC 해제만으로 회수 제한이 사라지지 않으며 회수25와
같은25의 폐기를50으로 세지 않는 종료 반례를 검사한다. 상세 fixture는
계획 §13을 줄이지 않고 따른다.

## 6. 실제 인수 layer와 종료 gate

| layer | 실제로 필요한 원천 / 미구현 처리 |
|---|---|
| Unit | production 순수 판정·decimal·타입·시간 결과. 미구현이면 NOT_RUN |
| Schema | PostgreSQL 제약/조직 FK·Flyway 빈 설치/upgrade·compiler drift |
| API/SIT | 실제 인증 요청/outcome와 DB 원장·배분·판정·의무·audit·outbox 대조 |
| MCP | 실제 wire/header-body/MRTR와 API/DB 동등성·현재 인가 |
| Recovery | 진짜 transaction/worker·clock/barrier·restart·외부 결과 대조 |
| Client/Skills | exact host version·discovery/loading/tool transcript. synthetic runner와 구별 |
| Model | 같은 Gherkin/fixture/oracle,60건×3회·최소10건 외국어/혼합·오류0·완전한 usage/비용. R8 전 NOT_RUN |
| BTP/Regulatory | 실제 binding/auth/TLS/deploy/restore와 정책 출처/관할/적용일/확인자. local·가상 정책과 구별 |

진행 순서는 R2 확정→공통 harness smoke·계약 통합/B2 기록→독립 case
작성→coordinator 통합→전체 contract RED·NOT_RUN 목록·assertion 변조
검사→실제 GPT-6.1 Sol xhigh와 GPT-6 Astra low review·지적 수정이다.
case 작성은 사용자 지정 GPT-6.1 Sol high로 수행한다.

공통 wrapper는 계획의 schema→contracts→scenarios/recovery→mcp/skills→
승인된 model/deployment 의존 순서를 보존한다. 실제 product adapter는
사용자 Step3에서 S0를 통과한 뒤 S1–S6 순서로 연결한다. 모델·배포는
승인한 비용과 계정 범위 안에서만 실행한다.

Step2 종료는 실행 가능한 테스트 납품과 review 통합의 완료다. 필수
실모델·BTP·규제·DB/API/MCP 인수가 NOT_RUN이면 제품 gate는 미완료다.
필수 미실행을 waiver나 비대상으로 숨기지 않는다.
