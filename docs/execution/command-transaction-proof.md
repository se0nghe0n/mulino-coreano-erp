# 명령 응답에 실제 transaction 계보를 보존한다

기준은 `4d5479158e6b3808787240629b5562d6803289ea`이다. T17 원문
`replacement-command-effects-16`과
`released-replacement-allocation-21`은 명령 응답의 transactionId와
실제 allocation tuple의 transaction 계보를 대조한다. 원문 case,
assertion/oracle/fixture/grant/clock은 수정하지 않는다.

`ApplicationCommands`는 기존 현재 인가·정책·revision·claim commit 검사를
통과한 적용 결과에 서버 소유 transactionId를 넣는다. `CommandRepository`는
활성 Spring transaction의 JdbcTemplate connection에서
`pg_current_xact_id()::xid::text`를 읽는다. 기존 command fence와 도메인
쓰기·CommandRecords 완료가 참여하는 같은 거래다. 입력/handler의 ID,
랜덤 UUID, hash나 wall clock으로 대체하지 않는다.

결과는 최초 CommandRecords.resultJson에 함께 저장한다. 기존 terminal
immutable trigger가 저장 결과 변경을 막는다. replay는 기존 scope의
현재 권한을 다시 확인한 뒤 원래 JSON을 반환하며 재시도 거래의 XID로
덮어쓰지 않는다. 과거 저장 결과에 필드가 없으면 그대로 없으며 임의
backfill하지 않는다. NOT_APPLIED 거부 경로에는 필드를 넣지 않는다.
ACCEPTED_PENDING_EXTERNAL은 관찰/책임 같은 실제 내부 효과의 COMMITTED
결과이므로 같은 규칙을 쓴다. 외부 물리 효과가 완료됐다는 뜻은 아니다.

## 의미와 한계

transactionId는 현재 PostgreSQL database의 top-level 32-bit XID 문자열이다.
업무 ID, 전역/영구 식별자, 시각, 순서 보증, XID epoch가 아니다. 다른
DB/복원본/복제본과 비교하거나 wraparound/freeze 이후의 오래된 tuple을
같은 거래라고 추정하지 않는다. epoch를 만들어 붙이지 않는다.

일반 명령의 실제 allocation/CommandRecords xmin과 일치함을 PG에서
검증한다. savepoint/subtransaction에서 작성한 tuple xmin은 top-level
XID와 다를 수 있으므로 모든 tuple의 xmin이 항상 같다는 계약은 아니다.
현재 observer의 동일 DB/snapshot·첫 10억 XID 미만·유일한 COMMITTED
command 조건을 완화하지 않는다. response XID만으로 observer의 증명을
대체하지 않는다.

한 changeset의 여러 명령은 같은 transactionId와 서로 다른 commandId를
가진다. 따라서 XID는 단일 command를 증명하지 않는다. 현재 observer는
이 경우 유일성 부족으로 fail-closed한다. caller-owned changeset 안의
APPLIED 결과는 outer commit 전까지 tentative이며 외부에 성공으로
발행하면 안 된다. outer rollback 시 결과·효과·감사 모두 사라진다.
이 필드만으로 commit을 주장하지 않는다. 기존 CAP changeset/transport
commit 계약을 변경하지 않는다.

## 집중 검증과 산출물

새 `CommandTransactionPostgresTest`는 자기 PostgreSQL/key/blob만 소유한다.
static nested fixture authoring을 구성해 사용하며 enclosing test의 host를
초기화하거나 107개 테스트를 상속하지 않는다. 실제 identity/policy/source
검증과 reserve/replace/release pipeline을 사용한다. 기존 synthetic
fixture authoring을 재사용하며 원래 scenario grant는 바꾸지 않는다.

reserve의 원 xmin, replacement의 이전/새 allocation 두 tuple과 command
xmin, release의 새 XID, creation command 유지, 동일 key replay의 원래
결과·tuple 불변, terminal JSON 불변, 권한 철회·다른 조직 거부,
효과 후 예외 rollback, 두 명령 changeset와 outer rollback을 확인한다.
일반 요청 실패에는 성공 결과가 반환되지 않으며 REJECTED 저장 JSON에
transactionId가 없음을 확인한다.

로그/JUnit/SQL row·xmin/명령·exit·버전·해시·exact own 자원 정리는
`.local/command-transaction-proof`에 남긴다. full backend/harness/native
actual은 실행하지 않는다. Task/Step는 OPEN이며 primary가 통합한다.

응답 schema는 새 필드와 성공 outcome 제한만 공개한다. 소유 범위 밖
`agents/runtime-package-manifest.json`의 이 schema hash는 primary가
통합 시 동기화해야 한다. 변경 전·후 hash를 local 산출물에 남긴다.

최신 test별 결과는 Spring/PG 6 PASS, 0 FAIL/ERROR/SKIP이다. 마지막
6개 실행의 5 PASS/1 FAIL은 changeset helper의 내부 값과 wire 값 비교
차이였고, 같은 TransportValues 정규화로 교정한 해당 1개 재실행이
PASS이다. 제품 XID 계산/인가/수량 규칙을 바꾸어 통과시키지 않았다.
원래 transactionId 누락 RED 1건, 내부/wire 표현 비교 실패 4건과 첫
테스트 compile 실패도 원로그로 보존한다. schema 통제 7개와 실제 저장된
응답 216건 검증은 모두 PASS이다. 이 수는 PG test 개수와 구별한다.
