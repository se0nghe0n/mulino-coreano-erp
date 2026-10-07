# D24의 인가·감사·보존 계약

한 표면의 인가 성공은 검색·blob·batch·worker·관리 경로의 인수를
대신하지 않는다.13개 subcase가 각 경로, 원자적 감사와 보존을 검사한다.

검색/blob/worker는 READ 위임을, 관리 명령은 조직A 관리 권한으로
조직B 주체를 변경하는 시도를 사용한다. 독립 DB observer는 조직A/B
전체 fixture scope를 읽어 타 조직의 금지 효과도 검사한다. 이 observer의
검증 권한을 제품 요청자 권한으로 전달하지 않는다. 거부 감사는 허용하고
업무/원장/배분/외부 요청 변경은 금지한다.

batch는 허용된 단일 hold를 양성 control로 먼저 실행한다. 이후 조직A의
허용 hold와 조직B의 금지 dispatch를 한 atomic batch로 요청한다. 최초
control의 hold1은 유지되고 batch의 추가 hold/출고는 모두 rollback돼야
한다. worker의 SYSTEM_USER context가 원 인간의 READ 위임을 확대하지
않는다. blob 양성 control은 서버 인가 후 자기 문서의 단기 참조·만료·
실제 다운로드 hash를 검사한다.

audit-rollback-and-retry는 실행 가능한20 BOX allocation에서 실제 감사
저장 fault를 주입한다. domain·ledger·allocation·obligation/assignment·
outbox·멱등 결과의 실제 전후 row를 각각 비교한다. fault 해제 뒤 같은
key의 실제 dispatch는 한 번 적용돼야 한다. 원장60−20=40 BOX와 배분의
EXECUTABLE→CONSUMED를 audit before/after에 대조한다. actor/delegator,
work/target/action, request/effect/evidence, definition/policy/result를
검사하고 별도 승인 없는 dispatch의 approvalRefs는 빈 배열이다.

legal-hold·active-reference·R6-unconfirmed·authorized-delete·restore-deleted는
다섯 artifact 종류에 서로 다른 가상 기간과 명시 basis/effectiveDate/
reviewer/method를 둔다. 실제 정책으로 주장하지 않는다. legal hold와
진행 의무 참조는 삭제를 막는다. R6 미확정 scope는 삭제0이다. synthetic
승인 profile의 만료·무참조 문서만 tombstone과 blob 삭제 대조를 만든다.
`retentionSweep`는 fixture의 config maintenance identity로 실제 인증된
host 실행을 요구한다. payload의 actor 문자열만으로 권한을 만들지 않는다.

복원 사례는 삭제 전 실제 backup output을 읽는다. 삭제 후 sweep의
actual deletion history를 함께 입력하고 쓰기 개방 전에 적용한다.
actual restore artifact의 tombstone·deletion log·blob NOT_PRESENT와
DB/blob/definition/capability/evaluator/skill/config/deployment 구성을
독립 extractor로 검사한다. 이전 Git commit 복원을 삭제 이력 적용으로
주장하지 않는다.

sentinel-artifact-scan은 가상 marker를 validation 실패 입력에 넣는다.
dataInventory→inspectArtifacts→scanArtifacts로 queue/error/log/audit/
evidence/backupManifest의 실제 전체 파일을 연결한다. 이 isolated fixture는
표면별 전체 파일 하나씩을 명시하며 추가 파일을 누락한 inventory도 거부한다.
actual descriptor,
전체 bytesRead/hash와 모든 발견 위치는 공통 HostObservationValidator가
파일을 직접 읽어 검사한다. case는 발견0, 정확한 여섯 표면 inventory,
필요 감사 참조 보존과 별도 secret-manager 복원을 검사한다. literal
marker가 없다는 결과를 모든 종류의 비밀 부재로 확대하지 않는다.

실제 API/DB/blob/process/복원 adapter는 NOT_RUN이다. parser RED와
assertion mutant 검사는 제품 PASS가 아니다.
