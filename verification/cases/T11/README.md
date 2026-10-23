# T11 독립 관찰 계약

현재 입력과 제품 경로는
[2026-10-24 수정 기록](../../../docs/execution/fix-wave-20261024-t11.md)을
따른다. 원본110 assertion ID와 fixture·grant·clock은 유지한다.
새 가역 입력 layer와5개 oracle 표현의 계약 교정, 실제 focused PG
검증을 추가했다. 전체 actual scenarios는 통합 뒤 Haiku 실행이 필요하다.

모든 scenario는 setup도 명시적 행동으로 실행한다. 아래 초기 관찰
설계의 미구현 표기는 작성 당시의 기록이다. 현재 실행 결과는 위 수정
기록에서 focused 검사와 전체 인수를 구별한다. 원행 source는 조직·caseId·품목 scope 안에서 완전하게 조회한다.
DB snapshot token은 직전 공개 조회가 발급한 snapshotRevision으로 고정한다.

각 fixture의 policy는 synthetic이며 법규나 운영 승인으로 해석하지 않는다.
정의/evaluator/정책·인증 actor와 scoped grant·LOT·물량·증거 hash·인간
owner와 supervisor·nextAction·nextCheck를 명시한다. 테스트하는 효과는
setup에 seed하지 않는다. 전송 요청의 IDs는 alias/result로 연결한다.

`rawRows`의 works/goalVersions/assessments/conditionResults/assessmentInputs,
obligations/assignments/handovers/workLinks, segments/movements/contributions,
activities/inbox/audit/outbox/executionAttempts/approvals/contracts/claims,
observationCoverage/supervisionIssues는 독립 observer의 read-only mapping
계약이다. 현재 구현된 범위는 실제 observer source와 실행 증거로 확인한다. API 결과와 함께
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

## 2026-10-23 parent90 입력 후속 검증

`author_parent_ranges.py`는 `4f01e111`의 source graph·누적 기간·assessment
입력 위에 적용하는 T11 전용 가역 layer다. 세 `parent90-*`의 원래
`receive90` 원문은 `rangeRootId=Q`, `startQuantity=0`, `quantity=90 BOX`다.
원래 serial label `001-030`과 `031-090`을 각각 `[0,30)`, `[30,90)`으로
표현한다. 원래 label·provenance·요청은 `parent-range-input-ledger.json`에
그대로 남긴다. Work link 방향과 canonical occurrence는 바꾸지 않는다.

link/close 직전에 공개 `getWork`로 현재 revision을 읽는다. `closeWork`의
지원 입력인 Work·reason을 유지하고, 서버가 직접 확인하는 최신 assessment
대신 보내던 오래된 `assessmentId`와 미지원 설명 slot은 ledger에 보존한다.
110개 assertion 원문, 다른 9개 subcase, 모든 fixture bytes와 business
권한·수량·시각은 보존한다. `scenario.feature`에는 조회 행동만 추가한다.

재생성은 `python3 verification/cases/T11/author_parent_ranges.py`, 정적
보존 검사는 `python3 -m unittest discover -s verification/cases/T11`로 한다.
`restore()`는 결과 전체를 검증하여 입력·권한·oracle의 미기록 변이를 거부한다.
기존 source/cumulative/assessment authoring의 fixed point도 검사한다.

결정적 실검증은 `python3 verification/actual/t11/run.py <새 evidence 경로>`다.
현재 tree에서 npm/CDS/backend를 빌드하고 전용 PostgreSQL·Spring HTTP를
시작하여 원래 세 흐름과 구간/권한 음성 입력을 실행한다. 모델이나
`./verify scenarios --actual`을 호출하지 않는다. SQL은 별도 read-only
REPEATABLE_READ transaction에서 원래 receipt·canonical·link·credit·판정을
읽는다. 생성한 server/container/key는 종료 때 정리한다.

현재 range 명령은 APPLIED지만 원래 child→parent 방향의 credit은 부모에게
귀속된다. 자식의 실제 판정은 UNVERIFIED이고 FULFILLED 종료는
HELD/ASSESSMENT_NOT_CURRENT다. `getAssessment`에는 원래 후속 입력이
가리키던 `obligationId`도 없다. 자식 원천을 만들거나 방향을 뒤집어 PASS로
바꾸지 않는다. 기존 oracle를 지원할 제품 계약/구현 검토와 부족10의 실제
assignment·인계 근거 결정은 coordinator에게 남긴다. 전체 Task/Step과
원래 scenario 인수는 OPEN이다. 실행별 세부 결과는 `.local/t11-parent-followup`
원로그와 인계 artifact에 보존한다.

2026-10-23 후속 승인으로 공용 `assessment_contracts.reverse`의 T11 분기에
새 layer의 strict inverse를 먼저 연결했다. feature도 T11 전용 역변환을
거친 뒤 기존 복원을 적용한다. T09/T12 분기와 126개 기존 binding ledger는
변경하지 않았고, oracle·입력·actor·feature 변조 거부를 유지한다.
제품의 남은 경계와 최소 변경안은 [도메인 후속 제안](domain-gap-proposal.md)에
읽기 전용 조사로 정리했다. 제품 구현·추가 fixture 입력은 적용하지 않았다.
