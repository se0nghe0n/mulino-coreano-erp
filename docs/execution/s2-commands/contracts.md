# S2 공통 명령 거래 계약

각 channel에서 권한과 멱등 결과가 달라지는 문제를 막기 위해 REST,
CAP action, MCP tool을 하나의 `ApplicationCommands`로 연결한다.
공개 actor·role·clock·runtime claim override는 받지 않는다.

- `CommandHandler.prepare`는 typed slots와 효과 종류를 검증하고,
  조직에 속한 scope·fence·기존 revision을 반환한다. 쓰기를 하지 않는다.
- `CommandRequests`는 구조와 provenance를 검사하고 재귀적으로 정렬한
  JSON의 SHA256를 계산한다. 실행 key·대화 ID·승인 ID·기존 hash와
  proposalRevision은 의미 hash에서 제외한다. expectedRevision은 포함한다.
- definitionVersion label 또는 UUID는 조직 내 PUBLISHED 자료로 조회한다.
  저장 content hash·metadata와 실제 handler의 capability semantic version,
  evaluator 및 input/output schema contract를 검사한다.
- 조직과 서버 stableRequestOwner·capability·key의 멱등 fence, handler의
  scope fence, 정책·actor fence를 취득한다. scope fence는 정렬한
  `organizationId + ':' + fenceKey`의 PostgreSQL advisory transaction lock이다.
- 잠금 뒤 prepare를 다시 수행한다. 범위·authority actor·효과 종류가
  바뀌면 conflict다. 현재 grant와 expected revision·필요 승인·claim을
  검사한다. 같은 key의 다른 canonical hash는 효과0 conflict다.
- 같은 key의 완료 결과 재조회는 현재 scope 인가를 요구한다. 이미 끝난
  효과를 새로 실행하지 않으므로 실행용 정책·승인 소비를 재수행하지 않는다.
  현재 scope 권한을 잃으면 과거 결과를 노출하지 않는다.
- 효과·감사·승인 소비·결과를 같은 Spring transaction에 저장한다.
  `CommandExecution.commandId()`로 handler가 outbox의 원 명령을 연결한다.
  실제 outbox 저장과 lease 구현은 runtime module 소유다.
- 일반 명령은 효과 뒤 현재 인가와 정책을 재검사한다. 설치된 control
  handler만 `mutatesAuthorization`을 선언할 수 있다. control guard는 자신의
  의도한 정책/권한 변경을 동시 철회로 혼동하지 않도록 commit proof를
  검증한다. 다른 handler는 이 예외를 쓸 수 없다.
- 감사 저장 실패는 효과와 멱등 성공을 rollback한다. 예상된 업무 거부는
  효과 transaction rollback 뒤 별도 transaction에 안전한 거부 감사와
  REJECTED 결과를 남긴다. 이 거부 key를 다른 내용으로 수정하지 않는다.
- PostgreSQL lock/serialization/deadlock 오류는 같은 요청과 key로 최대
  세 attempt만 수행한다. 새 revision이나 승인으로 바꾸지 않는다.
- `retryOriginal`은 저장 actor·stable owner와 현재 인가를 확인한 뒤
  저장된 canonical 요청과 key를 그대로 실행한다. UNKNOWN_EXTERNAL은
  대조가 필요하다. 거부 기록에는 원문 intent를 저장하지 않는다.

공개 endpoint는 `/api/ontology/commands/{capability}`,
`/api/ontology/records/{capability}`, `/api/ontology/commands/validate`다.
`/api/evidence/uploads`도 `attachEvidence` RECORD envelope를 같은
pipeline으로 보낸다. CAP은 `command`·`validateCommand` action을 쓰며,
MCP는 실제 등록된 handler capability만 tools/list에 게시한다.

이 worker의 PG 검사는 실제 CQN·Spring transaction·ledger·감사·정의 hash를
검증한다. identity와 policy 판단의 test double은 실제 권한 인수를
대신하지 않는다. control/runtime 통합, native HTTP/OData/MCP 명령 인수,
전체 T41, 실제 model·운영·BTP 인수는 결합 검사에서 따로 판정한다.

subjectRefs는 정확한 noun 이름과 실제 효과 대상을 선언한다. handler의
`subjectBindings`는 검증된 slot·DB 대상에서 noun별 ID와 최소·최대 개수를
계산한다. 일반 인가 scope에 ID가 있다는 이유로 subject로 허용하지 않는다.
정의의 noun·action과 binding을 fence 전후에 확인한다. 빈 선언은 handler가
최소 개수 0을 명시한 경우에 허용하며, 기본 handler는 빈 선언만 허용한다.
검증된 binding은 immutable 감사 사실에 남고, canonical intent의 subject도
같은 hash에 묶인다. 완료된 요청의 replay는 소비된 업무 조건을 재실행하지
않고 저장된 모든 실제 효과 scope에 현재 인가를 확인한다.

완료된 명령의 권한 철회 또는 다른 hash replay도 별도 거부 감사를 남긴다.
기존 성공 record·효과·결과는 바꾸지 않는다. CommandRecords의 완료·거부
상태는 DB trigger로 update·delete를 막고 감사 FK는 같은 조직을 강제한다.
S0 reserve 기술 검증 경로는 명시한 `platform-spike` profile에서만 활성화한다.
일반 profile의 generic CAP projection도 별도 role 요건으로 접근을 막는다.
