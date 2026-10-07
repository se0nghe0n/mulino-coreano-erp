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

이 기록 시점의 `git diff --check`는 PASS다. Maven slot 조정 후
targeted JUnit 실행은 11 tests PASS다. PostgreSQL/CQN tests 5개와
기존 identity unit tests 6개가 failures/errors/skips 0으로 통과했다.
자기 grant 철회와 더 엄격한 COMMAND policy 활성화를 실제
`ApplicationCommands`로 실행하고 다음 행동 차단을 확인했다.
검사 중 공유 srv의 미통합 모듈 imports 4개를 임시 제외한 뒤 복구했다.
전체 통합 srv compile과 domain 결합은 root gate에서 다시 확인한다. 전체 C3/V4/V7 경로, MCP·실모델·운영 R5 identity,
규제 인증, BTP는 이 구현으로 PASS가 되지 않는다.

policy draft는 `fixtureOnly=true`를 요구한다. synthetic source·회귀 증거는
개발 계약 검증용이며 공식 법규나 운영 승인으로 표시하지 않는다.
실제 운영 정책 근거와 승인자 mapping은 미확정이므로 활성화 gate를
유지한다. sweeper가 invalidation/boundary를 소비하는 실행 인수는
runtime integration에서 확인한다.

권한·정책을 변경하는 명령의 commit guard는 서버 handler의 명시
`mutatesAuthorization` 선언에 한정한다. pre-effect 인가와 같은
transaction의 fence를 유지하며 원 membership/grant/assignment/정책/
승인 만료 경계를 current clock으로 다시 검사한다. 자기 철회나 의도한
정책 강화 때문에 원 명령을 rollback하지 않는다. 일반 업무 command는
post-effect 현재 인가를 다시 확인한다. payload의 effect class로 이 경로를
선택하지 않는다.

감사 연결 후속 변경은 실행 전 실제 COMMAND policy ID/version/hash와
검증한 approval ID/hash·scope hash·proposal revision·decision capability·
approver를 transaction-local proof에 보존한다. membership/assignment/
grant ID·revision과 delegator chain은 같은 현재 scope 인가 평가에서
선택한 행을 사용한다. 정책 활성화와 자기 철회 후에도 이 원 근거를
감사에 남긴다. replay는 새 결정이나 효과를 만들지 않는다.

`AuthorityEvidenceTest`와 기존 gateway test의 감사 assertion은 이 후속
변경에서 추가했다. 이후 세 class의 13 tests가 모두 통과했다. 실제 두 WORK scope replay
반례와 감사 assertion을 포함한다. 아래 최종 denial audit assertion
변경은 별도 root 회귀가 필요하다.

Sol review P2의 재요청 결과 누설을 차단했다. 최초 실행이 W1/W2를
포함한 결과를 저장한 뒤 W2 grant만 철회하고 W1은 유지했다. 검색의
ID OR 권한은 여전히 true지만 같은 key의 전체 결과 replay는 거부되고
새 효과는 0이었다. replay guard는 모든 저장 효과 scope를 현재
identity chain fence 안에서 확인하며 업무 prerequisite나 승인 소비를
반복하지 않는다. 실제 gateway·PolicyCommandGuard·IdentityAuthorization·
PostgreSQL/CQN을 썼고 업무 효과 handler만 테스트 fixture다.

13 tests PASS 후 공유 `3144652`가 재요청 거부 감사를 별도로 저장하도록
변경됐다. test는 원 COMMITTED 기록/효과 1개를 유지하고 동일 command에
APPLIED와 REJECTED 감사 2개, 안전한 denial facts와 빈 effect refs를
요구하도록 맞췄다. 이 마지막 assertion 변경은 `NOT_RUN`이며 root의
combined native 회귀에서 확인한다.

control subject binding은 payload의 선언 ID를 실제 slot/DB target과
대조한다. grant 생성·capability 배정은 실제 recipient의 Human/Agent
ID에, 철회는 Grant/CapabilityAssignment 행과 해당 recipient에 연결한다.
정책 승인·활성화·종료는 실제 draft ID를 PolicyVersion noun으로 연결한다.
optional 선언을 생략한 기존 입력은 허용한다. 선언한 noun은 발행 정의와
행동에 있어야 하며 ID·cardinality·case가 다르면 공통 gateway가 거부한다.

createPolicyDraft의 ID는 서버가 만든다. 현재 handler에 typed item policy
scope가 없으므로 임의 TradeItem/조직 scope를 생성될 정책 subject로
간주하지 않는다. 이 command의 nonempty subject는 차단한다. T13 등
향후 품목별 정책 scope는 S3 typed 계약과 함께 구현해야 한다.

기존 실제 gateway test에 잘못된 Grant/PolicyVersion subject가 철회·승인
효과를 만들지 않고 올바른 PolicyVersion subject는 승인되는 assertion을
추가했다. 이 후속 변경은 아직 NOT_RUN이며 root의 combined 회귀에서
확인한다. 공통 dependency는 `3d44a08`과 `b89cd54`다.
