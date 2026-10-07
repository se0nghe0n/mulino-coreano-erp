# S1 core·정의·신원·증거·읽기 통합 기록

S0의 실제 DB·인가·MCP 기준선을 새 업무 모델로 확장한다. 사용자
Step3는 ACTIVE이며 이 문서의 S1도 아직 ACTIVE다. 각 worker의 검사
성공만으로 S1을 닫지 않는다.

## 기준선과 소유권

공통 baseline은 `164414418b6c3ce2959979939388d59cf2068e2c`이며
로컬 tag는 `step3-s0-complete`다. Task branch는
`feat/ontology-implementation`이다. 실제 GPT-6.1 Sol medium writer가
같은 기준선의 독립 worktree에서 작업하며 coordinator만 통합한다.

| Subtask | branch / 절대 worktree | 소유 범위 |
|---|---|---|
| 공통 읽기·adapter | `step3/s1-integration` / `/Volumes/VideoStore/Developer/mulino-ontology-step3-s1-integration` | 공통 port·REST/OData/MCP·V7 업무 참조·통합 checks·공유 build |
| 신원·현재 권한 | `step3/s1-identity` / `/Volumes/VideoStore/Developer/mulino-ontology-step3-s1-identity` | V3·조직/actor·외부 신원 mapping·현재 grant·공통 인가 구현 |
| 정의·정책 | `step3/s1-definitions` / `/Volumes/VideoStore/Developer/mulino-ontology-step3-s1-definitions` | V4·불변 정의·typed validator·정의/정책 조회 |
| 품목·물량·관계 | `step3/s1-inventory` / `/Volumes/VideoStore/Developer/mulino-ontology-step3-s1-inventory` | V5·품목/LOT/active segment/계보/typed 관계·CQN 조회 |
| 사건·원문·증거 | `step3/s1-evidence` / `/Volumes/VideoStore/Developer/mulino-ontology-step3-s1-evidence` | V6·불변 관측/문서·blob adapter·정정 참조·시점 조회 |
| 실제 인수 adapter | `step3/s1-adapter` / `/Volumes/VideoStore/Developer/mulino-ontology-step3-s1-adapter` | opt-in 실제 HTTP/JDBC driver·격리 fixture·실행 관찰 |

`adapters/blob`는 evidence가 소유하고 다른 공통 adapter는 integration이
소유한다. `backend/pom.xml`과 전체 CDS import는 integration 단일
writer가 관리한다. 긴 Maven/verify 실행은 전체 동시2개로 제한하며
같은 worktree의 build를 겹쳐 실행하지 않는다.

## 통합 경계와 인수 기준

S1은 빈 DB Flyway, 실제 CQN 조회, 조직 FK와 접근 통제, 정의의
type/cardinality, 두 진입점의 동일한 세계 조회를 인수한다. 업무의
Goal/Assessment/Obligation 참조는 실제 DB fixture로 읽되 생명주기
명령은 S2에서 구현한다. 구매·실수령·판매 이행을 S1 조회 결과로
성공 처리하지 않는다. 미구현 적격량·예약량은 UNKNOWN으로 구분한다.

전체 T01·T02 등이 뒤 단계의 명령/원장을 포함하면 해당 전체 case는
NOT_RUN으로 남긴다. 별도 S1의 실제 HTTP+독립 JDBC 검사로 현재 구현한
범위를 증명한다. adapter는 미지원 source/control을 빈 배열이나0으로
채우지 않는다. query projection hash를 DB MVCC snapshot ID로 바꾸지
않으며 실제 관찰 출처·시점을 따로 기록한다.

도메인 Instant는 PostgreSQL `TIMESTAMPTZ`로 저장한다. 고정 CDS compiler의
`Timestamp` SQL 출력과 다른 이 부분은 대상 column을 명시한 compatibility
예외로 기록한다. 나머지 entity/column/type/폭/정밀도/키 계약을 비교하며
실제 CQN의 UTC·Asia/Seoul session 및 offset 입력 round-trip과 시간
predicate를 검증해야 한다. S0의 별도 spike schema는 변경하지 않는다.

## 현재 통합

| 산출물 | worker commit | Task commit |
|---|---|---|
| 공통 조회/인가 port | `b1a3b87` | `35bcb5c` |
| typed scope·decimal 전송 | `88a5287` | `c5ab0f8` |
| 복수 scope 인가 계약 | `e414963` | `5f903ea` |
| V3 조직·신원 schema | `f158f43` | `151db50` |
| V4 정의·정책 schema | `36e3834` | `6bc3d53` |
| 정의 조회·검증·부분 검사 | `5baf104` | `92c228c` |
| V5 물량 schema·읽기 | `03be319` | `e025b9b` |

definitions의 focused8 tests는 통과했다. 이 중 PostgreSQL 검사는 최소
조직 dependency로 V4를 검증했으며 V3–V7 전체 결합 증거는 아니다.
통합 build·전체 schema compatibility·실제 두 진입점 검사는 아직 대기다.
후속 commit과 실행 결과를 이 기록에 추가한다.

R3에 따라 실제 운영 자료 없이 격리 DB와 개발 fixture를 사용한다.
R5 실제 identity, R7 BTP, R8 추가 실모델 비용은 아직 미확정이며
로컬 개발 fixture나 native 구현 worker를 운영 인수로 표시하지 않는다.
