# 현재 authority와 control plane

과거 조회 시점으로 만료된 권한을 되살리거나 발행 정책을 덮어쓰면
업무 의미와 실제 실행 허용이 섞인다. S2 control은 두 시점을 분리하고
같은 transaction 안에서 현재 identity·policy를 다시 확인한다.

## 구현 범위

- `IdentityAuthorization`은 공유 `ExecutionClock`을 쓴다. 현재 membership,
  capability assignment와 grant의 교집합에 scope 차원 AND/ID OR를
  적용한다. 상위 delegator의 현재 권한도 다시 확인한다.
- `IdentityCommands`는 `createGrant`, `revokeGrant`, `assignCapability`,
  `revokeCapability`를 공통 handler에 등록한다. 인간의 grant는 자기
  행동·scope·종료 경계보다 넓어질 수 없다. capability 관리는 별도
  `ManagementAuthorities` manifest의 행동·scope 안으로 한정한다.
  payload role과 `stableRequestOwner`는 권한을 만들지 않는다.
- grant/capability 변경은 현재 변경과 종료 경계의
  `AuthorityInvalidations`를 같은 transaction에 남긴다.
- `PolicyCommands`는 draft→승인→발행·활성 포인터→신규 사용 종료를
  분리한다. 승인 hash, 근거, effective interval, 회귀 증거를 보존한다.
  발행 내용과 승인 행은 immutable이며 적용 pointer는 별도다.
- `PolicyCommandGuard`는 현재 COMMAND 정책의 명시 action rule을 찾는다.
  effect class 또는 승인 정책이 불명확하면 `HELD`로 차단한다.
  승인은 hash·scope·proposal revision·대상 revision·현재 policy hash·
  현재 인간 승인 capability·유효기간과 single-use 소비를 확인한다.
- `getGrant`, `getAccessContext`, `getPolicy`는 같은 read 인가를 쓴다.
  현재 access context는 만료·철회된 assignment/grant를 제외한다.

COMMAND 정책 content는 `rules` object에 실제 capability 이름을 key로
사용한다. 각 rule은 `effectClass`, nullable `approvalAction`, nullable
`decisionCapability`를 가진다. wildcard rule은 없다. 일반 RECORD,
예약·내부 분할·병합·이동은 별도 규칙이 없으면 새 승인을 만들지 않는다.
재고 조정/감소, 회수, 긴급 재배정 등 필요한 행동은 action별 지정된
MANAGER/QC/ADMIN decision capability를 명시한다. 역할명 자체는 권한이
아니다. source scope·물량·처분의 업무 판정은 해당 domain guard가 맡는다.

## fence와 trade-off

현재 정책 fence는 조직 행의 `FOR UPDATE`다. 그 뒤 actor와 상위 delegator
행을 ID 순서로 잠근다. 정책 변경도 동일 조직 fence, grant/capability
변경도 동일 actor fence를 쓴다. scope별 최적화보다 현재 구현의
직렬화 안전성을 우선했다. 조직 내 병렬 명령 처리량은 낮아질 수 있다.

V12는 V4 policy의 timestamp를 UTC 의미의 `TIMESTAMPTZ`로 변환한다.
현재 Task의 R3는 fresh fixture다. 운영 timestamp migration 인수나
기존 실제 자료의 timezone 판정으로 확대하지 않는다.

## 검증과 남은 범위

`ControlPersistenceTest`는 실제 PostgreSQL에서 다음을 assertion한다.
현재 역할 WRITE/현재 grant READ 교집합, effect class 우회 차단,
caller의 역사 시점과 독립된 expiry, published policy 불변,
조직 간 active pointer FK 차단, 병렬 철회와 실행 fence의 직렬화다.

이 기록 시점의 `git diff --check`는 PASS다. Maven slot 조정 때문에
새 targeted JUnit 실행은 아직 `NOT_RUN`이다. 통합 실행 결과는 별도
후속 기록으로 남긴다. 전체 C3/V4/V7 경로, MCP·실모델·운영 R5 identity,
규제 인증, BTP는 이 구현으로 PASS가 되지 않는다.

policy draft는 `fixtureOnly=true`를 요구한다. synthetic source·회귀 증거는
개발 계약 검증용이며 공식 법규나 운영 승인으로 표시하지 않는다.
실제 운영 정책 근거와 승인자 mapping은 미확정이므로 활성화 gate를
유지한다. sweeper가 invalidation/boundary를 소비하는 실행 인수는
runtime integration에서 확인한다.
