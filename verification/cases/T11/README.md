# T11 독립 관찰 계약

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

자식 기여30+60=90이고 부모 목표100까지 부족10이다. 자식 종료와
부족10의 수락된 이전 뒤에도 부모90은100이 아니다. shared 실제60은
두 부모에30+30으로 배분하며60을 각각 복제한120을 허용하지 않는다.
초안 취소·timeout·미지원 evaluator·pending correction·stale revision과
출하 뒤 취소는 다른 조건이며 외부 사실과 남은 책임을 보존한다.

fixture의 DocumentVersion.content는 UTF-8 원문이다. sha256는 해당
원문의 실제 bytes에서 계산했으며 토큰이나 credential을 담지 않는다.
