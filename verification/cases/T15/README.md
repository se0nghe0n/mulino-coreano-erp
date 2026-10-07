# 기관 제출 부분 허용과 내부 QC를 구별한다

## 독립 손계산과 범위

구별된30+70=100 BOX에 기관 허용30을 기록하고 내부 QC100을 별도로 기록한다. 다른 조건은 가상 정책의 명시된
근거로 허용한다. 판매 후보는30이고70의 기관 조건은 UNKNOWN이다. 로컬 작성 PREPARED, 접수 미확인
SUBMISSION_UNCONFIRMED, 접수증거가 있는 SUBMITTED를 구별한다.
부분허용30·보완70·반려70·재제출70은 original procedure와 version을 보존하며 label100
scope는 독립이다. 공식 근거·적용일·검토자의 각각 누락은 실제 정책 활성화와 법적 적격 확정을 막는다.

fixture의 historicalFacts는 대조에 필요한 원천 입력이다. 승인·매칭·기관 판정·판매 적격
projection·의무 해소를 예상값으로 설치하지 않는다. 구매·수령·반품처럼 이 사례가 검증하는 효과는 Gherkin의 개별
명령으로 실행한다. 모든 fixture는 synthetic=true이며 운영 규제와 지급 권한의 근거가 아니다.

## 원행 관찰 계약

observe는 설치된 isolated organization·item·case·subcase와 API가 발급한 실제
snapshot token을 명시한다. source별 rawRows와 sourceEvidence의 실제
query/parameter/mapping/artifact를 요구한다. 수량은 decimal 문자열과 실제 단위로 검사한다.
identity는 strict alias/result ref를 사용하며 고정 수량 기대값은 결과에서 복사하지 않는다. 의무는
종류와 OPEN 범위로 좁혀 owner·nextAction·nextCheckAt·현재 assignment 수를 검사한다. 거부의
전후 원행과 허용된 감사는 별도로 관찰한다. 원행 배열의 sameAs 비교는 observer의 안정된 ID 순서를 요구한다.

## 규범 catalog와 실제 assertion 연결

| oracle / named observation | subcase / assertion |
|---|---|
| T15.partial-regulatory-eligibility / confirmed-sell-eligible | agency30-qc100/eligible-api30, agency30-qc100/confirmed-leaf30, agency30-qc100/eligible-db30 |
| T15.partial-regulatory-eligibility / remaining-agency | agency30-qc100/unknown70-agency |
| T15.partial-regulatory-eligibility / qc-expanded-agency-allowance | agency30-qc100/QC100-scope, agency30-qc100/qc-no-agency-expansion |
| T15.partial-regulatory-eligibility / decision-scope | agency30-qc100/decision-scope-raw |
| T15.procedure-lifecycle / local-document-state | procedure-lifecycle/local-PREPARED, procedure-lifecycle/local-db-PREPARED |
| T15.procedure-lifecycle / unknown-submit-state | procedure-lifecycle/submit-unconfirmed, procedure-lifecycle/submit-unconfirmed-raw |
| T15.procedure-lifecycle / local-document-external-submission | procedure-lifecycle/local-external-zero, procedure-lifecycle/local-outbox-zero, procedure-lifecycle/no-local-submit, procedure-lifecycle/no-local-submit-outbox |
| T15.procedure-lifecycle / regulatory-history | procedure-lifecycle/submitted-requires-receipt, procedure-lifecycle/immutable-v1-v2, procedure-lifecycle/decision-history, procedure-lifecycle/labels-not-agency |
| T15.unresolved-real-policy / confirmed-eligible | missing-officialSource/unverified-eligible0, missing-applicableDate/unverified-eligible0, missing-reviewer/unverified-eligible0 |
| T15.unresolved-real-policy / legal-eligibility | missing-officialSource/legal-UNVERIFIED, missing-officialSource/agency-unknown, missing-applicableDate/legal-UNVERIFIED, missing-applicableDate/agency-unknown, missing-reviewer/legal-UNVERIFIED, missing-reviewer/agency-unknown |
| T15.unresolved-real-policy / regulatory-gate | missing-officialSource/activation-effect0, missing-officialSource/official-active0, missing-officialSource/activation-blocked, missing-officialSource/activation-blocked-error, missing-officialSource/fixture-not-official, missing-applicableDate/activation-effect0, missing-applicableDate/official-active0, missing-applicableDate/activation-blocked, missing-applicableDate/activation-blocked-error, missing-applicableDate/fixture-not-official, missing-reviewer/activation-effect0, missing-reviewer/official-active0, missing-reviewer/activation-blocked, missing-reviewer/activation-blocked-error, missing-reviewer/fixture-not-official |

## 실행과 제한

B2는 feaca0af9673620eff9a5ac0f08a657ce14e9ccd다. 실제 제품
adapter·DB·기관·host·model 실행은 NOT_RUN이다. schema 검증과 Cucumber file
selector RED는 준비 증거이며 runtime PASS가 아니다. `SupplyAssertionTest`는 선언한
assertion과 고정 관찰 입력의 mutant를 검사하며 제품 service나 효과를 흉내 내지 않는다. 정확한 실행
명령·exit·발견 수·fixture hash는 evidence/preparation/에 기록한다. 실모델·유료 호출·외부
제출·은행 이체를 실행하지 않는다.
