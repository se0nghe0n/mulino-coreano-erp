# Runtime, 권한과 client 인수

검색어: `grant fence`, `UNKNOWN_EXTERNAL`, `nextValidityBoundary`,
`six packages`, `skill loading`, `V1`, `V5`, `V7`.

## Grants와 approval

검증한 issuer/subject/audience/organization에서 AuthContext를 만들고
서버의 grant로 actor/delegator/actions/target/work scope/유효기간/
revokedAt/revision을 확인한다. grant 발급자는 자기 권한 범위 안에서만
위임하며 Agent 자기 확장은 거부한다. identity 관리, 업무 ADMIN,
FDE 권한을 구별한다. 미설정 정책은 효과 없이 확인 대상으로 남긴다.

구매 MANAGER, QC 결정, 회수 ADMIN, 정의/정책 CONFIG_APPROVER는 계획
§7의 행동별 계약이다. 별도 승인 정책이 없는 예약·피킹·출고에
새 인간 승인을 임의 추가하지 않는다. 승인은 immutable hash/revision,
결정자·시점·유효기간·소비 정책에 연결하며 범위 변경은 재검토한다.
기존 효과 replay도 현재 결과 조회 인가를 검사한다.

동일 application 서비스에서 조직·grant·정책·실물 scope를 fence로
보호한다. worker service credential이나 queue system context를 원
사용자 위임으로 삼지 않는다. enqueue 뒤 철회, 인가 검사 직후 철회,
재시작의 직렬화 순서를 barrier로 검증한다. 철회가 먼저 확정되면
새 효과0이고, 앞서 확정한 효과는 보존한다. 차단된 업무에는 owner와
재인가/대조 행동이 남아야 한다. direct/nested/batch/projection,
MCP/worker/blob/운영 명령도 같은 경계를 검사한다.

V7의 새 효과0 primary는 `after`의 movements 원행에서
kind=DISPATCH·commandIdempotencyKey=new20을 고정해 `sumEquals 0 BOX`로
읽고, active segment 원행의 단위와 대조한다. 전후 DISPATCH 원행 불변도
별도로 검사한다(restart baseline은 prior-committed다).
`/data/data/` derivation은 고정 수량 primary가 아니다. source뿐 아니라
baseline·unitSource·baselineUnitSource에도 같은 제한을 적용한다.
상세는 [저장소 harness](../../ontology-scenario-testing/references/repository-harness.md)의
V7 절을 따른다. 새 assertion의 실제 제품 인수는 `NOT_RUN`이다.

**출고 이후 현재 DENIED:** 승인된 dispatch30이 이미 배분을 CONSUMED로
만든 뒤 회수 제한이 확정됐다. 실제 인도20, 운송중10 보고를 받는다.
현재 SELL 부적격을 이유로 사실을 삭제하지 않는다. RECORD 주체·원천·
Dispatch/CargoScope·동일 실물·수량·중복·evidence policy를 검증해 인도20을
보존하고 위반/회수 의무를 연결한다. 새 창고 출고·배분은0이다.
비준수 인도를 무조건 정상 목표 충족으로 세지 않는다. 허위20이나
prior Dispatch 없는 보고는 claim/inbox와 대조 책임으로 남기고 정상
이행 효과0이다. Future dispatch에 대한 DENIED는 계속 실행을 막는다.

## Durable worker와 외부 효과

command unique key는 `(organizationId, stableRequestOwner, capabilityId,
commandIdempotencyKey)`이며 canonical payload hash/version/result refs를
묶는다. stableRequestOwner는 token/RPC/Run ID가 아니다. 확정 효과와
COMMITTED 결과는 같은 transaction이며 rollback된 시도를 성공으로
돌려주지 않는다. transient rollback은 같은 key로 한정 재시도하고
REJECTED 내용 수정은 새 canonical 요청/key로 한다.

DB에는 Work·GoalVersion·Assessment·Obligation·ExecutionAttempt와
outbox를 각각 저장한다. scheduler는 WAITING nextCheck, due obligation,
미연결 intake, UNKNOWN_EXTERNAL, 만료 claim을 다시 찾는다. lease와
fencing token으로 stale commit을 막고 종료된 부모의 새 이상에도 intake
owner/nextAction/nextCheck를 보존한다. queue 전달·alert 성공은 의무
해소가 아니다. 정상 관측마다 새 Work를 만들지 않는다.

`nextValidityBoundary`와 due sweeper는 만료 정책/grant/LOT/처분 허용
범위를 재평가하고 배분 SUSPENDED·부족/재인가 의무를 원자적으로
연결한다. sweeper 지연에도 실제 실행 시 현재 조건을 검사한다.
claim 중복, 프로세스 재시작, 큐 비어 있음, retry 소진을 주입해 owner와
중복효과0을 관찰한다. test profile의 가상 시계/barrier를 우선 쓰고
1초 tick/5초 TTL/1초 heartbeat/30초 관찰을 운영 SLA로 선언하지 않는다.

자율 loop fixture의 scheduler·due-sweeper는 watcher group 전의 seed·
장애·조회·관찰·clock 전진 중에도 기동하지 않는다. api·worker는 group
밖에서 시작하고, group branch 0 첫 action은 수동 watcher, 나머지는
loop process start 하나씩이다. CaseRunner가 수동 watcher 요청에
`observeFrom`을 넣는다. group에서는 `data.observationBoundaryAt`과 같은
시각, group 밖 반복 watcher에서는 dispatch 직전 시각이다. case는 이 값을
직접 쓰지 않는다. extractor는 지속 제출 기록에서
`[observeFrom, observeFrom+observationWindowSeconds]` 안의 행만 읽고
앞선 sweep 행을 재사용하지 않는다. watcher command는 observeFrom 전에
시작하거나 창 끝 뒤에 끝날 수 없다. 실제 host adapter의 전달값 처리와
제품 인수는 Step 3 actual 소유, `NOT_RUN`이다.
host await 뒤 DB 관찰은 `RUNTIME_TASK_SNAPSHOT`을 사용한다. observer의
요청·보고·artifact 검증과 실제 adapter의 `NOT_RUN` 상태는 위 저장소
harness의 runtimeProfile·snapshot 절을 따른다.

외부 응답 유실은 UNKNOWN_EXTERNAL이다. 안정된 externalOperationId와
상대 멱등/상태 조회 또는 담당 대조로 확정한다. 확인된 성공은 다시
발행하지 않고 확인된 실패만 정책에 따른 재시도 후보다. 멱등/조회
수단 없는 쓰기를 자동 재발행하지 않는다. 로컬 취소·DB rollback은
이미 발생한 외부 효과 취소가 아니다. 운영 복구도 관리 command의
인가·dry-run diff·근거·감사를 통과한다.

## Runtime package 구현

파일 형식과 점진 loading은 공식
[Agent Skills 규격](https://agentskills.io/specification)을 따른다.
`name`/`description` frontmatter를 두고 꼭 필요한 references/scripts만
추가한다. `allowed-tools`는 실험적 client 기능이며 서버 인가가 아니다.
Skills over MCP는 선택 extension이다.

제품용 실제 디렉터리는 `agents/skills/<name>/`이고 발견 경로
`.agents/skills/<name>`와 `.claude/skills/<name>`는 각각 실제 디렉터리를
가리킨다. developer skill을 제품 package로 세지 않는다.

| Package | 필요한 행동 경계 |
|---|---|
| ontology-work-coordinator | 두 진입점 같은 ID/시점, slot 확인, 목표·의무·인간 책임, grant 내 위임 |
| ontology-procurement-transport | proposal/승인/전달/공급 수락/도착 구별, 잔여 의무 |
| ontology-import-qc | 정책·제출 증거·부분 수령 대조·QC 결정과 남은 제한 |
| ontology-sales-returns-recall | 행동 적격성·예약/출고/인도·반품/정정·ADMIN 회수·잔여 책임 |
| ontology-settlement | 송장 종류·차이/FX 근거·지급 참조; 실제 은행 이체 없음 |
| ontology-definition-authoring | 허용 typed 정의·영향/회귀·지정 승인·불변 발행 |

package manifest에는 name/version/hash, 지원 definition/capability/schema,
검증한 client 범위를 둔다. definition→handler/evaluator/schema 호환을
서버가 검사하고 v1 업무를 최신 의미로 몰래 바꾸지 않는다. 미지원
버전은 효과0·owner 있는 보류다. skill hash는 loading·올바른 해석의
증명이 아니다. 악성 문서 지시는 데이터로 취급한다.

## Actual client harness와 증거

Target client의 설치된 정확한 version과 실행 가능한 인터페이스부터
확인한다. synthetic JSON runner는 protocol harness로 표시하고 실제
Codex/Claude 등 host의 integration 성공으로 세지 않는다. host가 최신
wire/skill 기능을 지원하지 않으면 해당 통합은 NOT_RUN/미완료다.
호환 client나 얇은 adapter로 규격을 유지한다.

한 실제 client 인수 기록은 최소 다음을 연결한다.

- 격리 fixture·서버 commit·schema/definition/evaluator/skill manifest와
  실제 client/model/prompt/tool version, 정확한 command/config.
- skill 검색 노출, 본문/필요 reference loading의 관찰 artifact.
  host가 loading 증거를 제공하지 않으면 확인되지 않은 범위를 명시.
- 실제 tool wire transcript와 주체/scope, 서버 response, 효과/의무 조회.
  비밀은 redact하며 원 wire의 hash와 재현 가능한 safe artifact를 남김.
- 두 진입점, 동의어/다국어, 대상 충돌/모호성, QUERY→쓰기 오해,
  문서 속 악성 지시, 오래된 skill/정의, token 갱신 retry 결과.

T20/C3 결정적 schema fixture는 실모델 의미 성능을 증명하지 않는다.
§13 corpus는60건·각3회·최소10건의 이탈리아어/영어 또는 혼합 표현을
사용한다. ≥95% 구조화와 부당 실행/무권한/중복/허위 완료0은 제안
수용값이며 실행 전 R8에서 client/model·비용 상한·업무 수용치를
확정한다. 지연/token/비용/확인질문 비율을 별도 보고한다. 모델 비용
승인 부재는 이 실행을 대기시키며 다른 결정적 검증을 막지 않는다.
