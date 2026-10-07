# T09 독립 관찰 계약

모든 scenario는 setup도 명시적 행동으로 실행한다. 제품 adapter와 실제
API/DB/host/model은 아직 없어 NOT_RUN이다. 고정 selftest는 제품 fake가
아니다. 원행 source는 조직·caseId·품목 scope 안에서 완전하게 조회한다.
DB snapshot token은 직전 공개 조회가 발급한 snapshotRevision으로 고정한다.

각 fixture의 policy는 synthetic이며 법규나 운영 승인으로 해석하지 않는다.
정의/evaluator/정책·인증 actor와 scoped grant·LOT·물량·증거 hash·인간
owner와 supervisor·nextAction·nextCheck를 명시한다. 테스트하는 효과는
setup에 seed하지 않는다. 전송 요청의 IDs는 alias/result로 연결한다.

`rawRows`의 works/goalVersions/assessments/conditionResults/assessmentInputs,
obligations/assignments/handovers/workLinks, segments/movements/contributions,
activities/inbox/audit/outbox/executionAttempts/approvals/contracts/claims,
observationCoverage/supervisionIssues는 독립 observer의 read-only mapping
계약이다. 현 제품 schema의 존재를 주장하지 않는다. API 결과와 함께
원행 sourceQuery·parameters·mappingVersion·scopeComplete·snapshot artifact를
요구한다. count0도 완전한 scope의 실제 빈 원행 관찰이 필요하다.

상세 계산과 각 conjunction clause의 연결은 case.json의 assertion별
oracleExplanation과 oracleRef에 보존한다. 서버 의미 Boolean을 답안으로
요구하지 않는다. 허용된 denial/read audit는 금지된 업무 효과와 구별한다.

## 독립 손계산과 관찰 의미

누적100은 수령60+40이다. 현재40은 수령60−출고60+수령40이다.
구간 처음100에서100을 출고하면 끝0이다. EXISTS_IN은 처음100의
충족 상태를 인정하지만 끝 STATE_AT는0, THROUGHOUT은 출고 후0의
관측 때문에 미충족이다. 구간 전체100의 정상 continuity와 관측 공백의
UNVERIFIED는 별도 fixture다. LOT는 초안과 수령의 필수 단계를 구별한다.

fixture의 DocumentVersion.content는 UTF-8 원문이다. sha256는 해당
원문의 실제 bytes에서 계산했으며 토큰이나 credential을 담지 않는다.

## 작성 인수 증거

[checks/work-contracts.json](checks/work-contracts.json)은6개 case의
17 oracle·69 observation을509개 assertion에 연결한다. 검증 baseline은
6add3a5이며51개 fixture와 Gherkin·원문·schema hash를 보존한다.

- schema6개는 exit0이다. 실제 AssertionEngine을 사용하는 자체13개를
  포함한 harness143개가 실패·오류·skip0으로 통과했다.
- 실제 Gherkin file selector는 기대·발견·시작·실패·미구현 실패가
  각각51이고 scenario skip0이며 exit1이다. 첫 실패 뒤의 Gherkin
  step skip은 제품 assertion 전수 PASS로 해석하지 않는다.
- 제품 scenarios profile은 exit2/NOT_RUN이다. 원행·host·API·유료 모델과
  실제 제품의 행동을 실행했다고 주장하지 않는다. 이 증거는 Step2
  작성 인수이며 combined registry/prepare와 review는 통합 단계에 남는다.

초기 빈 where 형식 오류와 공통 alias 가용성 순서 오류는 최종 검증에서
수정된 상태로 재실행했으며 RED로 세지 않았다. 완전한 명령·관찰·exit와
원행 미실행 결과는 checks/의 로그 및 보고서에 기록했다.
