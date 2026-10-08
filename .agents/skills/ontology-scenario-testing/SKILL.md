---
name: ontology-scenario-testing
description: 완제품 수입·B2B 유통 온톨로지의 테스트 선행 계약, 한국어 업무 시나리오, Unit/SIT/UAT/Regression 검증과 실행 증거를 작성하거나 검토할 때 사용한다.
---

# 온톨로지 시나리오 테스트

업무 의미를 독립적인 oracle로 먼저 고정하고, 실제 효과·증거·남은
책임으로 구현을 검증한다. 현재
[구현 계획](../../../docs/ontology-implementation-plan.md)의 D01–D26,
T01–T26, C1–C5, V1–V8, E1/E2가 인수 기준이다. 명사·동사 진입점은
같은 대상·시점·근거·의무를 읽어야 한다. 국내 제조·BOM, 자동 은행
이체·세금 신고를 새 테스트의 전제로 넣지 않는다.

## 읽을 자료

- 새 사례를 쓰거나 실행·증거를 보고할 때 먼저
  [저장소 harness](references/repository-harness.md)를 읽는다. `./verify`
  명령과 exit code, case 구성, 실행 가능한 Gherkin 문법, NOT_RUN 처리,
  준비 gate의 vocabulary·binding·생성기 검사, 감사 원행, runtimeProfile·
  scheduler 제출 증거·관찰 경계, PREPARATION checkout 대조,
  RESULT_REVISION/RUNTIME_TASK_SNAPSHOT, MCP 오류 우선순위, receipt 조건,
  V4 kind별 probe 정책과 V7 원행 primary 규칙이 있다.
- 테스트 계층을 설계할 때 [방법론](references/methodology.md)을 읽는다.
  #57에서 업무 시나리오·공통 SIT/UAT·보류 증거 원칙을 재사용했다.
- 반례와 종단 수량을 작성할 때
  [인수 oracle](references/acceptance-oracles.md)을 읽는다.
  상세 도메인 계약은 구현 계획을 기준으로 확인한다.
- 새 시나리오 블록은 [Gherkin 양식](assets/scenario.feature.template)에서
  시작한다. 기존 case의 `scenario.feature`에 붙이는 harness 문법 양식이다. `HARNESS-EXAMPLE`은 문법 selftest 예제라
  `oracleRef`가 없어 제품 case로 쓸 수 없다. 제품 case의 case.json·fixture 골격은
  `verification/cases/E1/` 같은 기존 case를 따르고, 새 반례는 고정 41개 case의
  새 subcase로 추가한다(`repository-harness.md`).
- 별도 증거 양식은 없다. 실행 증거는 `./verify`와 `verification/coverage`
  pipeline이 만든 receipt·manifest뿐이며 손으로 채운 JSON은 PASS 증거가 아니다.

## 테스트 선행과 Step gate

사용자 Step2는 구현 전에 인수 계약을 테스트로 납품하는 Step다.
각 요구의 fixture·입력·정상/예외 oracle·검증할 효과·실행 경로를
작성하고 실행 가능한 assertion과 RED baseline을 남긴다. RED 원인은
미지원 업무 계약이나 기대 결과의 불일치여야 한다. 구문 오류,
Docker 미기동, 잘못된 인증 설정은 환경 실패이며 의미 있는 RED가 아니다.

구현 전 adapter가 없으면 제한된 계약 harness로 미지원 결과를
관찰할 수 있다. 이를 명시하고 해당 실제 DB/API/MCP 인수는 NOT_RUN으로
남긴다. test double의 성공을 통합 PASS로 세지 않는다. 선언·빈 테스트·
항상 참 assertion·전부 skip된 suite만으로 Step2를 완료하지 않는다.
Step2의 테스트 계약 납품 완료는 시스템 인수 완료가 아니다. 이후 구현을
통합한 뒤 실제 경로에서 필수 사례가 통과해야 시스템 완료를 주장한다.

Step별 baseline commit, 소유 경로, fixture hash, 실행 command,
RED/PASS 결과와 미실행 경로를 남긴다. 기능 부재를 해결하려고
oracle를 구현 결과에 맞추거나 실패 case를 삭제하지 않는다.

## 사례와 fixture

1. D 요구와 T/C/V/E case를 연결하고 정상·예외·경계 결과를 정한다.
   ID 개수만 맞추지 말고 각 연결에 실제 assertion과 artifact를 둔다.
2. `# language: ko` Gherkin으로 업무 프로세스를 표현한다. 단계는 harness의
   세 문장 형태를 따르고(`repository-harness.md`), `그러면`의 설명 인자에
   수량·단위·상태·의무·담당을 적어 업무 독자가 읽을 수 있게 한다. 같은
   규칙의 입력 변형은 독립 subcase로 선언하며 Scenario Outline example도
   subcase 수에 포함한다.
3. fixture는 독립 disposable DB/문서 저장소에 만든다. 실제 운영 자료,
   credential, 외부 발주·제출·이체를 사용하지 않는다. 조직·주체·grant,
   정의/evaluator/정책 버전, 품목/단위·LOT·구별 가능한 물량, 증거 hash와
   사건 ID, owner·기한을 고정한다. 미확인은 정상이나 수량0으로 바꾸지 않는다.
4. 업무 시계의 instant·시간대·기한 끝점·knownAt을 manifest에 고정한다.
   SIT/UAT가 같은 fixture와 시계를 쓴다. 만료·재시작·경합은 가상 시계와
   barrier로 선후를 통제하고 sleep 우연에 의존하지 않는다. wall-clock
   실행 지연과 업무 시간을 별도 기록한다.
5. 기대 수량·금액은 독립 손계산으로 설명한다. decimal 문자열과 단위를
   사용하고 허용 오차·반올림은 적용 정책이 있을 때만 허용한다.

## 실행 경로와 assertion

Unit은 순수 판정·수량·타입·시간 경계의 공개 행위를 검증한다. SIT는
manifest가 정한 실제 PostgreSQL, 서비스 API, stateless MCP, 인증과
runtime의 결합을 검증한다. 테스트 전용 scripted agent는 정해진 의도를
실제 tool에 보내며 서버 판정·권한·원장을 대체하지 않는다. CLI는
필수 경로가 아니다. 정확한 도구·DB·protocol 버전은 검증 manifest에서
읽고 과거 버전이나 이미 없어진 실행 명령을 재사용하지 않는다.

응답의 정확한 수량·판정·구조화 outcome을 검증하고 DB 원장·배분·
의무·감사·outbox와 대조한다. 명사와 동사 조회는 같은 ID·snapshot과
평가시점에서 필드별로 비교하고 paging 안정성도 확인한다
(`repository-harness.md`). 테스트가 호출하는 실제 command·인증 주체와
효과 scope를 기록한다. 응답 필드 존재, 오류 문구, UI 라벨, 내부
메서드 호출 여부만 확인해서 업무 PASS를 주장하지 않는다.

V4의 우회 경로 집합은 고정 목록이 아니라 실행 중 시스템이 노출한 쓰기 면을
열거해 만들어야 한다. kind별 요청 probe class는 `writeCapable`과 독립이며
readonly entity set도 쓰기 거부를 관찰한다. `verification/cases/V4`의
고정 inventory 92개는 열거가 아니고, `exposed-write-surface`는 계약이
정의됐어도 실제 host adapter가
없어 `NOT_RUN`이므로, 그 subcase가 실제 실행되어 PASS하기 전에는 V4의 노출 면 부분을
PASS로 보고하지 않는다(`repository-harness.md`).

명령 오류 코드는 응답의 `/response/error/code` 하나로만 읽고 `/response/code`·
`errorCode` pointer를 쓰지 않는다(`./verify prepare`가 거부한다). 의무·판정
원행의 `current`는 revision 유효성이며 `status`와 독립이다. 열린 의무는
`current=true`와 `status=OPEN`을 함께 단언한다. 자율 loop는 harness tick이 아니라
false/false runtimeProfile에서 자연 tick을 수동 관찰한다. 감사 이름·presence와
PREPARATION commit/clean 필드도 공개 계약을 따른다(`repository-harness.md`).
자율 scheduler·sweeper는 watcher group 전까지 기동하지 않는다.
api·worker는 group 밖에서 시작하고 group의 loop start는 branch당 하나다.
관찰 창은 `data.observationBoundaryAt`부터 잰다. snapshotRef는 API revision과
runtime task snapshot을 구별한다. 고정 수량 primary의 source·baseline·
unitSource·baselineUnitSource에는 `/data/data/` 파생값을 쓰지 않는다.

거부·권한 없음·재시도 사례는 전후 상태를 비교해 금지된 수량 변경,
배분·승인·업무·외부 요청 생성이 없음을 증명한다. 정책이 허용하는
조회/거부 감사와 대조 접수·잔여 책임은 별도 기대 결과로 명시한다.
모든 테이블이 무조건 불변이라는 약한 가정을 쓰지 않는다. 중복 요청은
효과1회와 결과 재사용을, 같은 키의 다른 payload는 conflict와 효과0을
검증한다. 승인이 필요한 행동에는 승인·반려·무권한·승인 후 변경을
포함하되 일반 기록·예약·출고에 새 인간 승인을 임의 추가하지 않는다.

경합/복구는 진짜 transaction과 두 요청/worker로 관찰한다. 현재
grant·정책·제한을 commit 경계에서 다시 확인하는지, rollback이 원장·
감사·outbox를 함께 되돌리는지, 재시작 뒤 의무의 owner/nextAction/
nextCheck와 fencing이 남는지 검증한다. 큐 성공·알림 성공·Agent 종료를
목표 충족이나 책임 종료로 단언하지 않는다.

## UAT와 비용

SIT와 UAT는 같은 Gherkin·fixture·업무 oracle를 사용하고 Agent 실행만
scripted runner에서 실제 지원 client/model로 바꾼다. 실제 모델의 문장이나
tool 호출 순서를 고정 답안으로 삼지 않고 최종 효과·부당 실행·남은
책임을 검증한다. skill discovery, 본문/참고 자료 loading, tool 호출,
서버 결과를 각각 관찰한다. skill hash만으로 loading을 증명하지 않는다.

유료 모델 호출 전에 해당 model/client·반복 수·비용 상한에 대한 명시적
승인이 있어야 한다. 기존 승인의 범위 안이면 다시 묻지 않는다. 승인이나
로그인·model 접근·배포 자원이 없으면 독립적인 결정적 검증을 계속하고
UAT/배포는 NOT_RUN과 원인으로 기록한다. R8의 실제 수용치·비용 상한을
확정하기 전에 계획의 제안 수치를 사업 SLA로 확정하지 않는다.

각 attempt와 retry의 input/output token, 제공된다면 cache/reasoning token,
요금 기준·통화·총비용, p50/p95 지연, client/model/prompt/skill/definition
버전과 확인질문을 기록한다. provider가 주지 않은 metric은 null과
누락 이유로 남긴다. 누락을0으로 계산하거나 일부 usage만으로
총비용 complete를 선언하지 않는다. token/비용 증거가 불완전하면 업무
결과 PASS와 별개로 UAT 전체 gate는 미완료다.

## 증거와 완료 판정

case 결과는 PASS/FAIL/NOT_RUN이다. skip, 접속 실패, `NOT_IMPLEMENTED`,
일부 단계만 실행한 사례는 전체 PASS가 아니다. 부분 실행은 단계별
결과와 중단 이유를 남기고 전체 case는 미실행 부분 때문에 NOT_RUN,
확인된 위반이 있으면 FAIL로 기록한다. 논리검토·테스트 작성·mock 결과·
규제 검토·local/BTP/client 검증을 각각 구분한다.

`./verify`는 구현된 entrypoint이며 모드와 exit code는
`repository-harness.md`에 있다. 실제 command·version·exit code·codeCommit·
receipt 경로를 보고에 인용한다. PASS는 `runtime-manifest.json`의 해당 item이나
profile `status`가 `PASS`이고 `python3 verification/coverage/validate.py`가
현재 입력에 대해 exit0인 경우만 쓴다. `VALID`는 일관성만 뜻하며 manifest의
PASS가 없거나 검증이 실패하면 PASS로 인용하지 않는다.
`ACTUAL`·`SELFTEST`·`CONTRACT_RED`는 coverage index의 evidence class이고
STUB·LOGIC_REVIEW는 보고서에서만 구별하는 범주다.
`./verify coverage`는 assembler(`--check-preparation`)의 exit code를 그대로 돌려준다.
`--actual` profile 실행은 엄격한 조건에서만 `ExecutionReceiptProducer`로 receipt를
만들고, 조건을 못 채우면 `NOT_EMITTED`와 이유를 낸다. 현재 actual driver는
`api`·`fixture`·`db` adapter만 공급하므로 mcp·client·model·process·host가 필요한
subcase는 `NOT_RUN`이고 manifest PASS를 주장할 수 없다. 이 차이를 receipt·report를
손으로 만들어 메우지 않는다. native `actual-sN` 결과는 한정된 custody 증거로 따로
보고한다.
전체 gate는 D26개·C5개·V8개·E2개의 assertion/artifact 추적, 실제 필수
경로 PASS, 미해결 실패·미실행·비용 증거 누락0을 확인한다. 추가 규제·
운영 gate가 미해결이면 범위와 활성화 제한을 표시한다. Regression은
통합 baseline에서 영향받는 사례와 필수 SIT를 실행하며 새 변경이나
미해결 우려 없이 같은 검증을 의례적으로 반복하지 않는다.
